package com.aarab.prayertimessersheng;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.aarab.prayertimessersheng.data.PrayerTime;
import com.aarab.prayertimessersheng.databinding.ActivityMainBinding;
import com.aarab.prayertimessersheng.viewmodel.PrayerViewModel;
import com.aarab.prayertimessersheng.worker.SyncWorker;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding b;
    private PrayerViewModel viewModel;

    // ── Convenience static helpers (used by AdhanReceiver) ───────────────────
    public static boolean isAdhanEnabled(Context ctx) {
        return ctx.getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("adhan_enabled", true);
    }
    public static int getAdhanVolume(Context ctx) {
        return ctx.getSharedPreferences("settings", MODE_PRIVATE)
                .getInt("adhan_volume", 100);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        viewModel = new ViewModelProvider(this).get(PrayerViewModel.class);

        observeViewModel();
        setupClicks();
        scheduleBackgroundSync();

        viewModel.loadToday();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh city label and times when returning from Settings
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String cityAr = prefs.getString("selected_city_ar", "إفران");
        b.tvCity.setText(cityAr);
        viewModel.loadToday();
    }

    // ── ViewModel observers ───────────────────────────────────────────────────

    private void observeViewModel() {

        viewModel.loading.observe(this, isLoading ->
                b.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE));

        viewModel.status.observe(this, msg -> b.tvStatus.setText(msg));

        viewModel.today.observe(this, this::bindPrayerTimes);

        viewModel.nextName.observe(this, name -> b.tvNextPrayerName.setText(name));

        viewModel.nextTime.observe(this, time -> b.tvNextPrayerTime.setText(time));

        viewModel.countdownMs.observe(this, ms -> {
            long h = ms / 3_600_000;
            long m = (ms % 3_600_000) / 60_000;
            long s = (ms % 60_000) / 1_000;
            b.tvCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));
        });
    }

    // ── Bind one PrayerTime to the 6 card text views ──────────────────────────

    private void bindPrayerTimes(PrayerTime pt) {
        if (pt == null) return;

        // Hijri date header
        if (pt.hijriDay != null && pt.hijriMonthAr != null) {
            String hijri = pt.hijriDay + " " + pt.hijriMonthAr + " " + pt.hijriYear + " هـ";
            b.tvHijriDate.setText(hijri);
        }

        // City (Arabic display name)
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        b.tvCity.setText(prefs.getString("selected_city_ar", "إفران"));

        // Times
        b.tvFajrTime.setText(safe(pt.fajr));
        b.tvSunriseTime.setText(safe(pt.sunrise));
        b.tvDhuhrTime.setText(safe(pt.dhuhr));
        b.tvAsrTime.setText(safe(pt.asr));
        b.tvMaghribTime.setText(safe(pt.maghrib));
        b.tvIshaTime.setText(safe(pt.isha));

        // Fade-in animation for cards
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(500);
        b.cardFajr.startAnimation(fadeIn);
        b.cardDhuhr.startAnimation(fadeIn);
        b.cardAsr.startAnimation(fadeIn);
        b.cardMaghrib.startAnimation(fadeIn);
        b.cardIsha.startAnimation(fadeIn);

        // Highlight the next prayer card
        highlightNextPrayer(pt);
    }

    /**
     * Compares current time against each prayer and sets the matching card
     * to the gold 'active' background.
     */
    private void highlightNextPrayer(PrayerTime pt) {
        Drawable normal = ContextCompat.getDrawable(this, R.drawable.card_prayer);
        Drawable active = ContextCompat.getDrawable(this, R.drawable.card_active);

        View[] cards = {b.cardFajr, b.cardSunrise, b.cardDhuhr,
                b.cardAsr, b.cardMaghrib, b.cardIsha};
        String[] times = {safe(pt.fajr), safe(pt.sunrise), safe(pt.dhuhr),
                safe(pt.asr), safe(pt.maghrib), safe(pt.isha)};

        // Reset all
        for (View card : cards) card.setBackground(normal);

        long now = System.currentTimeMillis();
        for (int i = 0; i < times.length; i++) {
            long pms = parseToMillis(times[i]);
            if (pms > now) {
                cards[i].setBackground(active);
                break;
            }
        }
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setupClicks() {
        b.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        b.btnQibla.setOnClickListener(v ->
                startActivity(new Intent(this, QiblaActivity.class)));

        b.tvCity.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    // ── WorkManager periodic sync ─────────────────────────────────────────────

    private void scheduleBackgroundSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "prayer_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                work);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String safe(String t) { return (t != null && !t.isEmpty()) ? t : "--:--"; }

    private long parseToMillis(String timeStr) {
        try {
            String[] p = timeStr.split(":");
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
            cal.set(java.util.Calendar.MINUTE,      Integer.parseInt(p[1]));
            cal.set(java.util.Calendar.SECOND,      0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) { return 0; }
    }
}
