package com.aarab.prayertimessersheng.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.aarab.prayertimessersheng.alarm.AlarmScheduler;
import com.aarab.prayertimessersheng.data.AppDatabase;
import com.aarab.prayertimessersheng.data.PrayerTime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

/** Re-schedules prayer alarms after device reboots. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String city  = prefs.getString("selected_city", "Ifrane");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        Executors.newSingleThreadExecutor().execute(() -> {
            PrayerTime pt = AppDatabase.getInstance(context)
                    .prayerTimeDao().getByDate(city, today);
            if (pt != null) {
                AlarmScheduler.scheduleAll(context, pt);
            }
        });
    }
}
