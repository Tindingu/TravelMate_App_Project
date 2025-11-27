package com.example.testproject1;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class S3Uploader {

    public interface UploadCallback {
        void onUploaded(String finalUrl);
        void onError(String err);
    }

    public static void uploadImage(File file, String uploadUrl, String finalUrl, UploadCallback cb) {

        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(
                file,
                MediaType.parse("image/jpeg")
        );

        Request request = new Request.Builder()
                .url(uploadUrl)
                .put(body)
                .addHeader("Content-Type", "image/jpeg")
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                cb.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    cb.onUploaded(finalUrl);
                } else {
                    cb.onError("Upload FAILED, code=" + response.code());
                }
            }
        });
    }
}
