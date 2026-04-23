package com.aarab.prayertimessersheng.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * Room entity — one row = one day of prayer times for one city.
 * Primary key is composite (city + date) to support multi-city storage.
 */
@Entity(tableName = "prayer_times", primaryKeys = {"city", "date"})
public class PrayerTime {

    @NonNull
    public String city = "";

    /** ISO date: yyyy-MM-dd */
    @NonNull
    public String date = "";

    public String fajr;
    public String sunrise;
    public String dhuhr;
    public String asr;
    public String maghrib;
    public String isha;

    // Hijri calendar fields
    public String hijriDay;
    public String hijriMonth;       // English month name
    public String hijriMonthAr;     // Arabic month name e.g. "رمضان"
    public String hijriYear;

    public PrayerTime() {}
}
