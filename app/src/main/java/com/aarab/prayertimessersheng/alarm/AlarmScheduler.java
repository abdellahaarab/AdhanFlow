package com.aarab.prayertimessersheng.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.aarab.prayertimessersheng.data.PrayerTime;
import com.aarab.prayertimessersheng.receiver.AdhanReceiver;

import java.util.Calendar;

/** Schedules / cancels exact Adhan alarms via AlarmManager. */
public class AlarmScheduler {

    private static final String[] KEYS = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
    private static final String[] AR   = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};

    public static void scheduleAll(Context ctx, PrayerTime pt) {
        String[] times = {pt.fajr, pt.dhuhr, pt.asr, pt.maghrib, pt.isha};
        for (int i = 0; i < KEYS.length; i++) {
            schedule(ctx, KEYS[i], AR[i], times[i]);
        }
    }

    public static void cancelAll(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        for (String key : KEYS) {
            PendingIntent pi = buildPendingIntent(ctx, key, "", PendingIntent.FLAG_NO_CREATE);
            if (pi != null && am != null) am.cancel(pi);
        }
    }

    // ─── private ─────────────────────────────────────────────────────────────

    private static void schedule(Context ctx, String key, String nameAr, String timeStr) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = buildPendingIntent(ctx, key, nameAr,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 1. If time is invalid or adhan is disabled for this prayer -> Cancel any existing alarm
        SharedPreferences prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("adhan_" + key + "_enabled", true) 
                       && prefs.getBoolean("adhan_enabled", true);

        if (timeStr == null || timeStr.equals("--:--") || !enabled) {
            am.cancel(pi);
            return;
        }

        // 2. Parse time
        String[] parts = timeStr.split(":");
        if (parts.length < 2) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
        cal.set(Calendar.MINUTE,      Integer.parseInt(parts[1]));
        cal.set(Calendar.SECOND,      0);
        cal.set(Calendar.MILLISECOND, 0);

        // If time already passed today → schedule for tomorrow
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // 3. Set exact alarm (requires SCHEDULE_EXACT_ALARM on Android 12+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) {
                // If we can't schedule exact, fallback to inexact or notify user
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                return;
            }
        }
        
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
    }

    private static PendingIntent buildPendingIntent(Context ctx, String key,
                                                    String nameAr, int flags) {
        Intent intent = new Intent(ctx, AdhanReceiver.class);
        intent.putExtra("prayer_name", nameAr);
        intent.putExtra("prayer_key",  key);
        return PendingIntent.getBroadcast(ctx, key.hashCode(), intent, flags);
    }
}
