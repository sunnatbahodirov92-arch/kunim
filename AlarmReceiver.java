package uz.kunim.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        if (in == null) return;
        String action = in.getAction();
        if (AlarmStore.ACTION_STOP.equals(action)) {
            c.stopService(new Intent(c, AlarmService.class));
            return;
        }
        String id = in.getStringExtra("id");
        if (id == null) return;
        // Ilova ochiq: signalni ilovaning o'zi chaladi
        if (AlarmStore.foreground) return;
        AlarmStore.addRung(c, id, 0);
        Intent s = new Intent(c, AlarmService.class);
        s.putExtra("id", id);
        s.putExtra("time", in.getStringExtra("time"));
        s.putExtra("title", in.getStringExtra("title"));
        try {
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(s);
            else c.startService(s);
        } catch (Exception ignored) {}
    }
}
