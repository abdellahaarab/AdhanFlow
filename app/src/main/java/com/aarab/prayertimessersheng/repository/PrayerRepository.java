package com.aarab.prayertimessersheng.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.aarab.prayertimessersheng.data.AppDatabase;
import com.aarab.prayertimessersheng.data.PrayerTime;
import com.aarab.prayertimessersheng.data.PrayerTimeDao;
import com.aarab.prayertimessersheng.network.AladhanService;
import com.aarab.prayertimessersheng.network.RetrofitClient;
import com.aarab.prayertimessersheng.network.models.AladhanMonthlyResponse;
import com.aarab.prayertimessersheng.network.models.DayData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Single source of truth.
 * Strategy: Room first → if missing, fetch from AlAdhan API → save to Room.
 */
public class PrayerRepository {

    public interface SyncCallback {
        void onSuccess(List<PrayerTime> times);
        void onError(String message);
    }

    private final PrayerTimeDao dao;
    private final AladhanService service;
    private final Context context;

    public PrayerRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dao     = AppDatabase.getInstance(context).prayerTimeDao();
        this.service = RetrofitClient.getService();
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /** Load today's prayer times — Room first, API fallback. */
    public void getTodayPrayerTimes(String city, SyncCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String today   = todayString();
            PrayerTime cached = dao.getByDate(city, today);
            if (cached != null) {
                List<PrayerTime> result = new ArrayList<>();
                result.add(cached);
                callback.onSuccess(result);
                return;
            }
            // Not cached — fetch the whole month
            Calendar cal = Calendar.getInstance();
            syncMonth(city, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, new SyncCallback() {
                @Override public void onSuccess(List<PrayerTime> times) {
                    for (PrayerTime pt : times) {
                        if (pt.date.equals(today)) {
                            List<PrayerTime> r = new ArrayList<>();
                            r.add(pt);
                            callback.onSuccess(r);
                            return;
                        }
                    }
                    callback.onError("لا توجد بيانات لليوم");
                }
                @Override public void onError(String message) { callback.onError(message); }
            });
        });
    }

    /** Load or sync a full month. */
    public void getMonthPrayerTimes(String city, int year, int month, SyncCallback callback) {
        String ym = yearMonth(year, month);
        Executors.newSingleThreadExecutor().execute(() -> {
            int count = dao.countByYearMonth(city, ym);
            if (count >= 28) {                      // have enough days cached
                callback.onSuccess(dao.getByYearMonth(city, ym));
            } else {
                syncMonth(city, year, month, callback);
            }
        });
    }

    /** Force-fetch from API and persist to Room. */
    public void syncMonth(String city, int year, int month, SyncCallback callback) {
        SharedPreferences prefs  = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        int method = prefs.getInt("calculation_method", 21); // 21 = Morocco
        int school = prefs.getInt("madhhab", 0);             // 0 = Shafi

        service.getMonthlyCalendar(year, month, city, "Morocco", method, school)
                .enqueue(new Callback<AladhanMonthlyResponse>() {

                    @Override
                    public void onResponse(Call<AladhanMonthlyResponse> call,
                                           Response<AladhanMonthlyResponse> response) {
                        if (!response.isSuccessful()
                                || response.body() == null
                                || response.body().data == null) {
                            fallbackToCache(city, year, month, callback,
                                    "خطأ في الخادم: " + response.code());
                            return;
                        }

                        List<PrayerTime> list = mapToPrayerTimes(city, response.body().data);

                        Executors.newSingleThreadExecutor().execute(() -> {
                            dao.insertAll(list);
                            callback.onSuccess(list);
                        });
                    }

                    @Override
                    public void onFailure(Call<AladhanMonthlyResponse> call, Throwable t) {
                        fallbackToCache(city, year, month, callback, "لا يوجد اتصال بالإنترنت");
                    }
                });
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void fallbackToCache(String city, int year, int month,
                                 SyncCallback callback, String errorMsg) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<PrayerTime> cached = dao.getByYearMonth(city, yearMonth(year, month));
            if (cached != null && !cached.isEmpty()) {
                callback.onSuccess(cached);
            } else {
                callback.onError(errorMsg);
            }
        });
    }

    private List<PrayerTime> mapToPrayerTimes(String city, List<DayData> days) {
        List<PrayerTime> result = new ArrayList<>();
        for (DayData day : days) {
            PrayerTime pt = new PrayerTime();
            pt.city = city;

            // Date: API gives "DD-MM-YYYY" → convert to "yyyy-MM-dd"
            if (day.date != null && day.date.gregorian != null) {
                String raw = day.date.gregorian.date; // "01-04-2024"
                String[] p = raw.split("-");
                if (p.length == 3) {
                    pt.date = p[2] + "-" + p[1] + "-" + p[0]; // "2024-04-01"
                }
            }

            if (day.timings != null) {
                pt.fajr    = clean(day.timings.Fajr);
                pt.sunrise = clean(day.timings.Sunrise);
                pt.dhuhr   = clean(day.timings.Dhuhr);
                pt.asr     = clean(day.timings.Asr);
                pt.maghrib = clean(day.timings.Maghrib);
                pt.isha    = clean(day.timings.Isha);
            }

            if (day.date != null && day.date.hijri != null) {
                pt.hijriDay  = day.date.hijri.day;
                pt.hijriYear = day.date.hijri.year;
                if (day.date.hijri.month != null) {
                    pt.hijriMonth   = day.date.hijri.month.en;
                    pt.hijriMonthAr = day.date.hijri.month.ar;
                }
            }

            if (pt.date != null && !pt.date.isEmpty()) {
                result.add(pt);
            }
        }
        return result;
    }

    /** Strip timezone suffix like " (+01)" from AlAdhan times. */
    private String clean(String raw) {
        if (raw == null) return "--:--";
        int idx = raw.indexOf(' ');
        return idx > 0 ? raw.substring(0, idx) : raw;
    }

    private String todayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private String yearMonth(int year, int month) {
        return String.format(Locale.US, "%04d-%02d", year, month);
    }
}
