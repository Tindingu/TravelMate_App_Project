package com.example.testproject1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    LinearLayout navHome, navBookmark, navCalendar, navNotification;
    FirebaseAuth auth;
    FirebaseUser user;
    FirebaseFirestore db;

    TextView name;
    ImageView btnavt;

    // ⭐ ADD SEARCH VIEW
    EditText etSearch;
    ImageView btnSearch;

    GoogleMap mMap;
    RequestQueue queue;

    GeminiService gpt;
    private static final String API_KEY_GEMINI = "AIzaSyD_mE9VKKR0vC4m9PzMo5VaeD9aifpTvUM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        navHome = findViewById(R.id.navHome);
        navBookmark = findViewById(R.id.navBookmark);
        navCalendar = findViewById(R.id.navCalendar);
        navNotification = findViewById(R.id.navNotification);
        btnavt = findViewById(R.id.ivProfile);
        name = findViewById(R.id.tvHello);

        // ⭐ Search box
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.ivSearch);

        queue = Volley.newRequestQueue(this);
        gpt = new GeminiService(API_KEY_GEMINI);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(d -> name.setText("Hello " + d.getString("name") + "!"));
        } else {
            name.setText("Hello Guest!");
        }

        btnavt.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        setActive(navHome);
        navHome.setOnClickListener(this::onNavClick);
        navBookmark.setOnClickListener(this::onNavClick);
        navCalendar.setOnClickListener(this::onNavClick);
        navNotification.setOnClickListener(this::onNavClick);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.homeMap);

        if (mapFragment != null) mapFragment.getMapAsync(this);

        // ⭐ LẤY QUERY TỪ THANH SEARCH
        btnSearch.setOnClickListener(v -> {
            String q = etSearch.getText().toString().trim();
            if (!q.isEmpty()) runAI(q);
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // ⭐ KHÔNG GỌI AI Ở ĐÂY NỮA
        // Map load lên trống → chờ user search
    }

    // ============================================================
    // ⭐ GỌI AI PHÂN TÍCH CÂU TIẾNG VIỆT
    // ============================================================
    private void runAI(String text) {

        gpt.analyzeQuery(text, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(JSONObject json) {

                try {
                    String category = json.getString("category");
                    String location = json.getString("location");
                    int radius = json.getInt("radius");

                    Log.d("AI_DATA", json.toString());

                    getLocationFromAI(location, category, radius);

                } catch (Exception e) {
                    Log.e("AI_PARSE_ERR", e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("AI_ERROR", error);
            }
        });
    }

    // ============================================================
    // ⭐ B1 – LẤY LAT/LON TỪ LOCATION
    // ============================================================
    private void getLocationFromAI(String locationName, String category, int radius) {

        String url = "https://nominatim.openstreetmap.org/search?format=json&q="
                + locationName.replace(" ", "+");

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.length() == 0) return;

                        double lat = response.getJSONObject(0).getDouble("lat");
                        double lon = response.getJSONObject(0).getDouble("lon");

                        LatLng center = new LatLng(lat, lon);

                        // ⭐ ZOOM VÀO LOCATION
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 15f));

                        searchOSM_AI(category, lat, lon, radius);

                    } catch (Exception e) {
                        Log.e("NOMI_ERR", e.getMessage());
                    }
                },
                error -> Log.e("NOMI_FAIL", error.toString())
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "Mozilla/5.0 (Android)");
                return headers;
            }
        };

        queue.add(req);
    }

    // ============================================================
    // ⭐ B2 – TÌM OSM & HIỂN THỊ 15 QUÁN
    // ============================================================
    private void searchOSM_AI(String category, double lat, double lon, int radius) {

        String url =
                "https://overpass-api.de/api/interpreter?data=[out:json];"
                        + "node[\"amenity\"=\"" + category + "\"](around:" + radius + ","
                        + lat + "," + lon + ");out 50;";

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                res -> {
                    try {
                        mMap.clear();

                        JSONArray arr = res.getJSONArray("elements");

                        int limit = Math.min(arr.length(), 15);

                        for (int i = 0; i < limit; i++) {

                            JSONObject o = arr.getJSONObject(i);
                            if (!o.has("lat")) continue;

                            double la = o.getDouble("lat");
                            double lo = o.getDouble("lon");

                            String placeName = "Địa điểm";
                            if (o.has("tags") && o.getJSONObject("tags").has("name"))
                                placeName = o.getJSONObject("tags").getString("name");

                            mMap.addMarker(
                                    new MarkerOptions()
                                            .position(new LatLng(la, lo))
                                            .title(placeName)
                            );
                        }

                    } catch (Exception e) {
                        Log.e("OSM_PARSE", e.getMessage());
                    }
                },
                error -> Log.e("OSM_ERR", error.toString())
        );

        queue.add(req);
    }

    // ============================================================
    // Bottom Nav
    // ============================================================
    private void onNavClick(View v) {
        resetNav();
        setActive((LinearLayout) v);
    }

    private void resetNav() {
        navHome.setBackground(null);
        navBookmark.setBackground(null);
        navCalendar.setBackground(null);
        navNotification.setBackground(null);
    }

    private void setActive(LinearLayout layout) {
        layout.setBackgroundResource(R.drawable.nav_item_selected_bg);
    }
}
