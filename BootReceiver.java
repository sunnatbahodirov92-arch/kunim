package uz.kunim.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        try { AlarmStore.restore(c); } catch (Exception ignored) {}
    }
}
