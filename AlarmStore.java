package uz.kunim.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

/** Budilnik ro'yxati, "chalingan" belgilari va AlarmManager bilan ishlash. */
final class AlarmStore {
    /** Ilova ochiq bo'lsa JS o'zi chaladi, native chalmaydi (ikki marta chalinmasligi uchun). */
    static volatile boolean foreground = false;

    static final String ACTION_ALARM = "uz.kunim.app.ALARM";
    static final String ACTION_STOP = "uz.kunim.app.STOP";
    private static final String PREFS = "kunim_alarm";

    private AlarmStore() {}

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static int piFlags() {
        return PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
    }

    // ---------- ro'yxat ----------
    static JSONArray list(Context c) {
        try { return new JSONArray(prefs(c).getString("list", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    static void saveList(Context c, JSONArray a) {
        prefs(c).edit().putString("list", a.toString()).apply();
    }

    // ---------- AlarmManager ----------
    static PendingIntent alarmPi(Context c, String id, long at, String time, String title) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.setAction(ACTION_ALARM);
        i.setData(Uri.parse("kunim://alarm/" + Uri.encode(id)));
        i.putExtra("id", id);
        i.putExtra("at", at);
        i.putExtra("time", time);
        i.putExtra("title", title);
        return PendingIntent.getBroadcast(c, 0, i, piFlags());
    }

    static PendingIntent showIntent(Context c) {
        Intent i = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        if (i == null) i = new Intent();
        return PendingIntent.getActivity(c, 1, i, piFlags());
    }

    static void set(Context c, String id, long at, String time, String title) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = alarmPi(c, id, at, time, title);
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, showIntent(c)), pi);
        } catch (SecurityException e) {
            try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    static void cancelAll(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        JSONArray old = list(c);
        for (int i = 0; i < old.length(); i++) {
            JSONObject o = old.optJSONObject(i);
            if (o == null || am == null) continue;
            PendingIntent pi = alarmPi(c, o.optString("id"), o.optLong("at"), o.optString("time"), o.optString("title"));
            am.cancel(pi);
            pi.cancel();
        }
        saveList(c, new JSONArray());
    }

    /** JS yuborgan to'liq ro'yxat bilan eskisini almashtiradi. */
    static synchronized void scheduleAll(Context c, JSONArray alarms) {
        cancelAll(c);
        long now = System.currentTimeMillis();
        JSONArray keep = new JSONArray();
        for (int i = 0; i < alarms.length(); i++) {
            JSONObject o = alarms.optJSONObject(i);
            if (o == null) continue;
            long at = o.optLong("at");
            String id = o.optString("id");
            if (at <= now || id.length() == 0) continue;
            set(c, id, at, o.optString("time"), o.optString("title"));
            keep.put(o);
        }
        saveList(c, keep);
    }

    /** Qayta yoqilgandan / yangilangandan keyin saqlangan budilniklarni tiklaydi. */
    static synchronized void restore(Context c) {
        long now = System.currentTimeMillis();
        JSONArray l = list(c);
        for (int i = 0; i < l.length(); i++) {
            JSONObject o = l.optJSONObject(i);
            if (o == null) continue;
            long at = o.optLong("at");
            if (at > now) set(c, o.optString("id"), at, o.optString("time"), o.optString("title"));
        }
    }

    // ---------- "keyinroq" ----------
    static synchronized long snooze(Context c, String id, String time, String title) {
        int min = prefs(c).getInt("snooze", 5);
        long at = System.currentTimeMillis() + min * 60000L;
        set(c, id, at, time, title);
        try {
            JSONArray l = list(c), n = new JSONArray();
            for (int i = 0; i < l.length(); i++) {
                JSONObject o = l.optJSONObject(i);
                if (o != null && !id.equals(o.optString("id"))) n.put(o);
            }
            JSONObject o = new JSONObject();
            o.put("id", id); o.put("at", at); o.put("time", time); o.put("title", title);
            n.put(o);
            saveList(c, n);
        } catch (Exception ignored) {}
        addRung(c, id, at);
        return at;
    }

    // ---------- JS'ga xabar: native chalgan budilniklar ----------
    static synchronized void addRung(Context c, String id, long until) {
        try {
            JSONArray a = new JSONArray(prefs(c).getString("rung", "[]")), n = new JSONArray();
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o != null && !id.equals(o.optString("id"))) n.put(o);
            }
            JSONObject o = new JSONObject();
            o.put("id", id); o.put("until", until);
            n.put(o);
            prefs(c).edit().putString("rung", n.toString()).apply();
        } catch (Exception ignored) {}
    }

    static synchronized JSONArray popRung(Context c) {
        JSONArray a;
        try { a = new JSONArray(prefs(c).getString("rung", "[]")); }
        catch (Exception e) { a = new JSONArray(); }
        prefs(c).edit().remove("rung").apply();
        return a;
    }

    static void setLaunch(Context c, String id) { prefs(c).edit().putString("launch", id).apply(); }

    static synchronized String popLaunch(Context c) {
        String id = prefs(c).getString("launch", null);
        prefs(c).edit().remove("launch").apply();
        return id;
    }
}
