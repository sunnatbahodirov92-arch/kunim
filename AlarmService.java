package uz.kunim.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** Budilnik chalganda: 60 soniya takrorlanuvchi signal + tebranish + to'liq ekran bildirishnoma. */
public class AlarmService extends Service {
    static final String CH = "kunim_alarm_v2";
    static final String CH_MISSED = "kunim_missed_v2";
    static final int NID = 4242;
    static final long RING_MS = 60000;

    private MediaPlayer player;
    private Vibrator vib;
    private PowerManager.WakeLock wl;
    private final Handler h = new Handler(Looper.getMainLooper());
    private String id = "", time = "", title = "";

    private final Runnable timeout = new Runnable() {
        @Override public void run() {
            postMissed();
            stopSelf();
        }
    };

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent in, int flags, int startId) {
        if (in == null) { stopSelf(); return START_NOT_STICKY; }
        String a = in.getStringExtra("id"), t = in.getStringExtra("time"), n = in.getStringExtra("title");
        id = a == null ? "" : a;
        time = t == null ? "" : t;
        title = n == null ? "" : n;
        createChannels();
        Notification nt = buildNotification();
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(NID, nt, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            else startForeground(NID, nt);
        } catch (Exception e) {
            stopSelf();
            return START_NOT_STICKY;
        }
        acquireWake();
        startSound();
        startVibration();
        h.removeCallbacks(timeout);
        h.postDelayed(timeout, RING_MS);
        return START_NOT_STICKY;
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel c = new NotificationChannel(CH, "KUNIM budilnik", NotificationManager.IMPORTANCE_HIGH);
        c.setSound(null, null);
        c.enableVibration(false);
        c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(c);
        NotificationChannel m = new NotificationChannel(CH_MISSED, "KUNIM eslatma", NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(m);
    }

    private Notification.Builder builder(String channel) {
        return Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, channel) : new Notification.Builder(this);
    }

    private Notification buildNotification() {
        Intent ai = new Intent(this, AlarmActivity.class);
        ai.putExtra("id", id);
        ai.putExtra("time", time);
        ai.putExtra("title", title);
        ai.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(this, 7, ai, AlarmStore.piFlags());

        Intent st = new Intent(this, AlarmReceiver.class);
        st.setAction(AlarmStore.ACTION_STOP);
        PendingIntent stop = PendingIntent.getBroadcast(this, 8, st, AlarmStore.piFlags());
        String stopL = AlarmStore.prefs(this).getString("stopL", "Stop");

        Notification.Builder b = builder(CH)
            .setSmallIcon(R.drawable.ic_stat_kunim)
            .setContentTitle("\u23F0 " + time)
            .setContentText(title)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setColor(0xFF0E7C86)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .addAction(R.drawable.ic_stat_kunim, stopL, stop);
        if (Build.VERSION.SDK_INT < 26) {
            b.setPriority(Notification.PRIORITY_MAX);
            b.setSound(null);
        }
        return b.build();
    }

    private void postMissed() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            Notification n = builder(CH_MISSED)
                .setSmallIcon(R.drawable.ic_stat_kunim)
                .setContentTitle("\u23F0 " + time)
                .setContentText(title)
                .setColor(0xFF0E7C86)
                .setAutoCancel(true)
                .setContentIntent(AlarmStore.showIntent(this))
                .build();
            nm.notify(5000 + (id.hashCode() & 0xFFF), n);
        } catch (Exception ignored) {}
    }

    private void acquireWake() {
        try {
            if (wl != null && wl.isHeld()) wl.release();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm == null) return;
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kunim:alarm");
            wl.acquire(RING_MS + 10000);
        } catch (Exception ignored) {}
    }

    private void startSound() {
        stopSound();
        try {
            AssetFileDescriptor fd = getResources().openRawResourceFd(R.raw.alarm);
            MediaPlayer p = new MediaPlayer();
            p.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
            p.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            fd.close();
            float v = Math.max(0.1f, Math.min(1f, AlarmStore.prefs(this).getInt("vol", 70) / 100f));
            p.setVolume(v, v);
            p.setLooping(true);
            p.prepare();
            p.start();
            player = p;
        } catch (Exception e) {
            stopSound();
        }
    }

    private void stopSound() {
        try {
            if (player != null) {
                try { player.stop(); } catch (Exception ignored) {}
                player.release();
            }
        } catch (Exception ignored) {}
        player = null;
    }

    private void startVibration() {
        try {
            vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vib == null || !vib.hasVibrator()) return;
            long[] p = {0, 500, 300, 500, 1000};
            if (Build.VERSION.SDK_INT >= 26) {
                vib.vibrate(VibrationEffect.createWaveform(p, 0),
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
            } else {
                vib.vibrate(p, 0);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        h.removeCallbacks(timeout);
        stopSound();
        try { if (vib != null) vib.cancel(); } catch (Exception ignored) {}
        try { if (wl != null && wl.isHeld()) wl.release(); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
