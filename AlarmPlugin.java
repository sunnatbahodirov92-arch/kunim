package uz.kunim.app;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;

/** JS <-> native: budilniklarni AlarmManager'ga qo'yadi. */
@CapacitorPlugin(name = "KunimAlarm")
public class AlarmPlugin extends Plugin {

    @PluginMethod
    public void schedule(PluginCall call) {
        JSArray arr = call.getArray("alarms", new JSArray());
        AlarmStore.scheduleAll(getContext(), arr);
        call.resolve();
    }

    @PluginMethod
    public void config(PluginCall call) {
        SharedPreferences.Editor e = AlarmStore.prefs(getContext()).edit();
        Integer sn = call.getInt("snooze");
        if (sn != null) e.putInt("snooze", sn);
        Integer vol = call.getInt("vol");
        if (vol != null) e.putInt("vol", vol);
        String[] keys = {"stopL", "openL", "snoozeL"};
        for (String k : keys) {
            String v = call.getString(k);
            if (v != null) e.putString(k, v);
        }
        e.apply();
        call.resolve();
    }

    @PluginMethod
    public void canExact(PluginCall call) {
        boolean ok = true;
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            ok = am != null && am.canScheduleExactAlarms();
        }
        JSObject r = new JSObject();
        r.put("value", ok);
        call.resolve(r);
    }

    @PluginMethod
    public void openExactSettings(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 31) {
            Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:" + getContext().getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { getContext().startActivity(i); } catch (Exception ignored) {}
        }
        call.resolve();
    }

    @PluginMethod
    public void canFullScreen(PluginCall call) {
        boolean ok = true;
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager nm = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            ok = nm != null && nm.canUseFullScreenIntent();
        }
        JSObject r = new JSObject();
        r.put("value", ok);
        call.resolve(r);
    }

    @PluginMethod
    public void openFullScreenSettings(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 34) {
            Intent i = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                Uri.parse("package:" + getContext().getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { getContext().startActivity(i); } catch (Exception ignored) {}
        }
        call.resolve();
    }

    /** Yopiq ilovada native chalgan budilniklar: [{id, until}] */
    @PluginMethod
    public void popRung(PluginCall call) {
        JSONArray a = AlarmStore.popRung(getContext());
        JSArray items = new JSArray();
        for (int i = 0; i < a.length(); i++) items.put(a.opt(i));
        JSObject r = new JSObject();
        r.put("items", items);
        call.resolve(r);
    }

    /** Budilnik oynasidagi "Ochish" bosilgan bo'lsa shu vazifa id'si. */
    @PluginMethod
    public void popLaunch(PluginCall call) {
        String id = AlarmStore.popLaunch(getContext());
        JSObject r = new JSObject();
        if (id != null) r.put("id", id);
        call.resolve(r);
    }
}
