package com.example.testproject1;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

public class GeminiService {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private final String apiKey;

    public GeminiService(String apiKey) {
        this.apiKey = apiKey;
    }

    public interface GeminiCallback {
        void onSuccess(JSONObject data);
        void onError(String error);
    }

    public void analyzeQuery(String query, GeminiCallback callback) {

        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("parts", new JSONArray().put(
                    new JSONObject().put(
                            "text",
                            "Hãy phân tích câu sau và trả về JSON dạng:\n" +
                                    "{ \"category\":\"cafe\", \"location\":\"Hồ Hoàn Kiếm Hà Nội\", \"radius\":2500 }\n" +
                                    "Câu: " + query +
                                    "\nCHỈ TRẢ JSON. KHÔNG GIẢI THÍCH."
                    )
            ));

            JSONObject bodyJson = new JSONObject();
            bodyJson.put("contents", new JSONArray().put(userMsg));

            RequestBody body = RequestBody.create(
                    bodyJson.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(GEMINI_URL + apiKey)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        String raw = response.body().string();

                        android.util.Log.d("GEMINI_RAW", raw);

                        JSONObject json = new JSONObject(raw);

                        String text = json
                                .getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        // remove markdown ```json ```
                        String clean = text
                                .replace("```json", "")
                                .replace("```", "")
                                .trim();

                        JSONObject result = new JSONObject(clean);

                        callback.onSuccess(result);

                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }
}
