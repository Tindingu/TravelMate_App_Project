package com.example.travelmate.services;

import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PreSignService {

    public interface Callback {
        void onSuccess(String uploadUrl, String finalUrl);
        void onError(String err);
    }

    public static void getPreSignedUrl(String backendUrl, String filename, Callback cb) {

        OkHttpClient client = new OkHttpClient();

        String fullUrl = backendUrl + "?filename=" + filename;

        Request request = new Request.Builder()
                .url(fullUrl)
                .get()
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                cb.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);

                    String uploadUrl = json.getString("uploadUrl");
                    String finalUrl = json.getString("finalUrl");

                    cb.onSuccess(uploadUrl, finalUrl);

                } catch (Exception e) {
                    cb.onError(e.getMessage());
                }
            }
        });
    }
}
