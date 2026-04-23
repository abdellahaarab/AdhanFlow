package com.aarab.prayertimessersheng.worker;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.aarab.prayertimessersheng.alarm.AlarmScheduler;
import com.aarab.prayertimessersheng.data.PrayerTime;
import com.aarab.prayertimessersheng.repository.PrayerRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * WorkManager worker — runs on network constraint, syncs current month,
 * then reschedules today's Adhan alarms.
 */
public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx   = getApplicationContext();
        SharedPreferences prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String city   = prefs.getString("selected_city", "Ifrane");

        PrayerRepository repo = new PrayerRepository(ctx);
        Calendar cal  = Calendar.getInstance();
        int year  = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;

        // Block until async callback completes (max 30 s)
        CountDownLatch latch   = new CountDownLatch(1);
        boolean[]      success = {false};
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());

        repo.syncMonth(city, year, month, new PrayerRepository.SyncCallback() {
            @Override
            public void onSuccess(List<PrayerTime> times) {
                // Reschedule alarms for today
                for (PrayerTime pt : times) {
                    if (pt.date.equals(today)) {
                        AlarmScheduler.scheduleAll(ctx, pt);
                        break;
                    }
                }
                success[0] = true;
                latch.countDown();
            }
            @Override
            public void onError(String msg) { latch.countDown(); }
        });

        try { latch.await(30, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        return success[0] ? Result.success() : Result.retry();
    }
}
