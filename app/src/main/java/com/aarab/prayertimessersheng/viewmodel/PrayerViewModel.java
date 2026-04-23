package com.aarab.prayertimessersheng.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.aarab.prayertimessersheng.data.PrayerTime;
import com.aarab.prayertimessersheng.repository.PrayerRepository;

import java.util.Calendar;
import java.util.List;

public class PrayerViewModel extends AndroidViewModel {

    private final PrayerRepository repository;
    private CountDownTimer countdownTimer;

    // ── Exposed LiveData ─────────────────────────────────────────────────────
    public final MutableLiveData<PrayerTime>       today         = new MutableLiveData<>();
    public final MutableLiveData<List<PrayerTime>> month         = new MutableLiveData<>();
    public final MutableLiveData<String>           status        = new MutableLiveData<>();
    public final MutableLiveData<Boolean>          loading       = new MutableLiveData<>(false);
    public final MutableLiveData<String>           nextName      = new MutableLiveData<>();
    public final MutableLiveData<String>           nextTime      = new MutableLiveData<>();
    public final MutableLiveData<Long>             countdownMs   = new MutableLiveData<>();

    public PrayerViewModel(@NonNull Application app) {
        super(app);
        repository = new PrayerRepository(app);
    }

    // ── Public commands ──────────────────────────────────────────────────────

    public void loadToday() {
        String city = city();
        loading.postValue(true);
        repository.getTodayPrayerTimes(city, new PrayerRepository.SyncCallback() {
            @Override public void onSuccess(List<PrayerTime> times) {
                loading.postValue(false);
                if (!times.isEmpty()) {
                    PrayerTime pt = times.get(0);
                    today.postValue(pt);
                    status.postValue("✓ آخر تحديث: " + pt.date);
                    
                    // Schedule alarms whenever we load today's times successfully
                    com.aarab.prayertimessersheng.alarm.AlarmScheduler.scheduleAll(getApplication(), pt);
                    
                    computeNextPrayer(pt);
                }
            }
            @Override public void onError(String msg) {
                loading.postValue(false);
                status.postValue(msg);
            }
        });
    }

    public void loadMonth(int year, int month) {
        repository.getMonthPrayerTimes(city(), year, month, new PrayerRepository.SyncCallback() {
            @Override public void onSuccess(List<PrayerTime> times) {
                PrayerViewModel.this.month.postValue(times);
            }
            @Override public void onError(String msg) { status.postValue(msg); }
        });
    }

    // ── Countdown ────────────────────────────────────────────────────────────

    private void computeNextPrayer(PrayerTime pt) {
        String[] arabicNames = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};
        String[] times       = {pt.fajr, pt.sunrise, pt.dhuhr, pt.asr, pt.maghrib, pt.isha};
        long now = System.currentTimeMillis();

        for (int i = 0; i < times.length; i++) {
            long pms = toMillis(times[i]);
            if (pms > now) {
                nextName.postValue(arabicNames[i]);
                nextTime.postValue(times[i] != null ? times[i] : "--:--");
                startCountdown(pms - now);
                return;
            }
        }
        // All prayers passed → show tomorrow's Fajr
        nextName.postValue("فجر الغد");
        nextTime.postValue(pt.fajr != null ? pt.fajr : "--:--");
    }

    private void startCountdown(long msUntil) {

        if (countdownTimer != null) {
            countdownTimer.cancel();
        }

        if (msUntil <= 0) return;

        new Handler(Looper.getMainLooper()).post(() -> {

            countdownTimer = new CountDownTimer(msUntil, 1000) {

                @Override
                public void onTick(long ms) {
                    countdownMs.setValue(ms); // UI thread → use setValue
                }

                @Override
                public void onFinish() {
                    computeReload();
                }

            };

            countdownTimer.start();
        });
    }

    private void computeReload() {
        loadToday(); // or smarter logic later
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private long toMillis(String timeStr) {
        if (timeStr == null || timeStr.equals("--:--")) return 0;
        try {
            String[] p = timeStr.split(":");
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
            cal.set(Calendar.MINUTE,      Integer.parseInt(p[1]));
            cal.set(Calendar.SECOND,      0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) { return 0; }
    }

    private String city() {
        SharedPreferences prefs =
                getApplication().getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getString("selected_city", "Ifrane");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (countdownTimer != null) countdownTimer.cancel();
    }
}
