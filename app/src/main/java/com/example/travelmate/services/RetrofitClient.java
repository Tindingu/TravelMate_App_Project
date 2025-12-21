package com.example.travelmate.services;

import com.example.travelmate.route.OSRMApi;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://router.project-osrm.org/";

    private static Retrofit retrofit;

    public static OSRMApi getApi() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(OSRMApi.class);
    }
}
