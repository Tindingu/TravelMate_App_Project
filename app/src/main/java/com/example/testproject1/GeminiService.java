package com.example.testproject1;

import android.util.Log;

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
            // Prompt mạnh
            String strictPrompt =
                    "Hãy phân tích câu sau và TRẢ VỀ JSON THUẦN (chỉ JSON, không markdown, không giải thích).\n" +
                            "JSON phải có dạng:\n" +
                            "{ \"category\": \"cafe\", \"location\": \"Hồ Hoàn Kiếm Hà Nội\", \"radius\": 2500 }\n" +
                            "YÊU CẦU:\n" +
                            "- Luôn luôn trả JSON HỢP LỆ.\n" +
                            "-  phải đúng chuẩn tag của OpenStreetMap.\n" +
                            "- category: phải là loại địa điểm (cafe, restaurant, bar…)\n" +
                            "- location: tên khu vực\n" +
                            "- radius: số mét (int)\n\n" +
                            "Câu của người dùng: " + query;

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("parts", new JSONArray().put(new JSONObject().put("text", strictPrompt)));

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
                        Log.d("GEMINI_RAW", raw);

                        JSONObject json = new JSONObject(raw);

                        String text = json
                                .getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        // Tách JSON bằng regex (trích mọi phần nằm trong {...})
                        String extractedJson = text.replaceAll("(?s).*?(\\{.*?\\}).*", "$1");

                        // Fix lỗi JSON cơ bản
                        extractedJson = extractedJson.trim()
                                .replace("“", "\"")
                                .replace("”", "\"")
                                .replace("‘", "\"")
                                .replace("’", "\"");

                        JSONObject result = new JSONObject(extractedJson);

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
