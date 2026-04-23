package com.aarab.prayertimessersheng.network.models;

import com.google.gson.annotations.SerializedName;

public class Timings {
    @SerializedName("Fajr")    public String Fajr;
    @SerializedName("Sunrise") public String Sunrise;
    @SerializedName("Dhuhr")   public String Dhuhr;
    @SerializedName("Asr")     public String Asr;
    @SerializedName("Maghrib") public String Maghrib;
    @SerializedName("Isha")    public String Isha;
}
