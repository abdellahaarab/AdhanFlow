package com.aarab.prayertimessersheng.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Singleton Retrofit client for AlAdhan API. */
public class RetrofitClient {

    private static final String BASE_URL = "https://api.aladhan.com/";
    private static AladhanService service;

    public static AladhanService getService() {
        if (service == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            service = retrofit.create(AladhanService.class);
        }
        return service;
    }
}
