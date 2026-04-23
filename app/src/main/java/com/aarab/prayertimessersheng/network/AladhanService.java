package com.aarab.prayertimessersheng.network;

import com.aarab.prayertimessersheng.network.models.AladhanMonthlyResponse;
import com.aarab.prayertimessersheng.network.models.AladhanResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AladhanService {

    /** Today's timings (kept for backwards compat) */
    @GET("v1/timingsByCity")
    Call<AladhanResponse> getTimingsByCity(
            @Query("city") String city,
            @Query("country") String country,
            @Query("method") int method
    );

    /**
     * Full month calendar.
     * method 21 = Morocco, school 0 = Shafi / 1 = Hanafi
     */
    @GET("v1/calendarByCity/{year}/{month}")
    Call<AladhanMonthlyResponse> getMonthlyCalendar(
            @Path("year")  int year,
            @Path("month") int month,
            @Query("city")    String city,
            @Query("country") String country,
            @Query("method")  int method,
            @Query("school")  int school
    );
}
