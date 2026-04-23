package com.aarab.prayertimessersheng.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PrayerTimeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PrayerTime> times);

    /** Get one specific day — used for home screen. */
    @Query("SELECT * FROM prayer_times WHERE city = :city AND date = :date LIMIT 1")
    PrayerTime getByDate(String city, String date);

    /**
     * Get all days for a year-month.
     * yearMonth format: "2024-04"
     */
    @Query("SELECT * FROM prayer_times WHERE city = :city AND substr(date,1,7) = :yearMonth ORDER BY date ASC")
    List<PrayerTime> getByYearMonth(String city, String yearMonth);

    @Query("SELECT COUNT(*) FROM prayer_times WHERE city = :city AND substr(date,1,7) = :yearMonth")
    int countByYearMonth(String city, String yearMonth);

    @Query("DELETE FROM prayer_times WHERE city = :city AND substr(date,1,7) = :yearMonth")
    void deleteByYearMonth(String city, String yearMonth);
}
