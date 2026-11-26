package uz.kunim.app;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Qulf ekranida ham chiqadigan budilnik oynasi. */
public class AlarmActivity extends Activity {
    private final Handler h = new Handler(Looper.getMainLooper());
    private String id = "", time = "", title = "";

    private final Runnable autoClose = new Runnable() {
        @Override public void run() { finish(); }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildUi(getIntent());
    }

    @Override
    protected void onNewIntent(Intent in) {
        super.onNewIntent(in);
        setIntent(in);
        buildUi(in);
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(sp);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button button(String label, boolean primary, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(18);
        b.setTextColor(primary ? 0xFF0E7C86 : Color.WHITE);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(16));
        if (primary) g.setColor(Color.WHITE);
        else { g.setColor(0x22FFFFFF); g.setStroke(dp(1), 0x66FFFFFF); }
        b.setBackground(g);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        lp.topMargin = dp(12);
        b.setLayoutParams(lp);
        return b;
    }

    private void buildUi(Intent in) {
        String a = in.getStringExtra("id"), t = in.getStringExtra("time"), n = in.getStringExtra("title");
        id = a == null ? "" : a;
        time = t == null ? "" : t;
        title = n == null ? "" : n;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(0xFF0E7C86);
        root.setPadding(dp(28), dp(48), dp(28), dp(36));

        root.addView(text("\u23F0", 56, false));
        root.addView(text(time, 64, true));
        TextView tt = text(title, 24, false);
        tt.setPadding(0, dp(8), 0, dp(40));
        root.addView(tt);

        android.content.SharedPreferences p = AlarmStore.prefs(this);
        root.addView(button(p.getString("openL", "Open"), true, new View.OnClickListener() {
            @Override public void onClick(View v) { openApp(); }
        }));
        root.addView(button(p.getString("snoozeL", "Snooze"), false, new View.OnClickListener() {
            @Override public void onClick(View v) {
                AlarmStore.snooze(AlarmActivity.this, id, time, title);
                stopRinging();
                finish();
            }
        }));
        root.addView(button(p.getString("stopL", "Stop"), false, new View.OnClickListener() {
            @Override public void onClick(View v) {
                stopRinging();
                finish();
            }
        }));
        setContentView(root);
        h.removeCallbacks(autoClose);
        h.postDelayed(autoClose, AlarmService.RING_MS + 2000);
    }

    private void stopRinging() {
        stopService(new Intent(this, AlarmService.class));
    }

    private void openApp() {
        AlarmStore.setLaunch(this, id);
        stopRinging();
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if (km != null) km.requestDismissKeyguard(this, null);
            }
        } catch (Exception ignored) {}
        Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacks(autoClose);
        super.onDestroy();
    }
}
