package com.example.testproject1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
//import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    // UI
    LinearLayout navHome, navBookmark, navChat, navCalendar, navNotification;
    TextView tvHello;
    ImageView ivProfile, ivSearchBtn;
    EditText etSearch;

    // RecyclerView
    RecyclerView rvPlaces;
    PlaceAdapter placeAdapter;
    ArrayList<PlaceModel> placeList;
    Set<String> wishlistIds = new HashSet<>();

    // Firebase + Map
    FirebaseAuth auth;
    FirebaseUser user;
    FirebaseFirestore db;
    GoogleMap mMap;
    RequestQueue queue;
    GeminiService gpt;

    // GPS
    FusedLocationProviderClient fusedLocationClient;

    private static final String API_KEY_GEMINI = "AIzaSyDJ4HUU4OxC4EAKfuo7Zp-FEphSeceyWCY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();
        queue = Volley.newRequestQueue(this);
        gpt = new GeminiService(API_KEY_GEMINI);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupUserProfile();
        setupRecyclerView();
        setupBottomNav();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.homeMap);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        listenToWishlist();

        ivSearchBtn.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                Toast.makeText(this, "AI đang tìm kiếm...", Toast.LENGTH_SHORT).show();
                runAI(query);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetNav();
        setActive(navHome);
    }

    private void initViews() {
        navHome = findViewById(R.id.navHome);
        navBookmark = findViewById(R.id.navBookmark);
        navChat = findViewById(R.id.navChat);
        navCalendar = findViewById(R.id.navCalendar);
        navNotification = findViewById(R.id.navNotification);

        tvHello = findViewById(R.id.tvHello);
        ivProfile = findViewById(R.id.ivProfile);
        ivSearchBtn = findViewById(R.id.ivSearch);
        etSearch = findViewById(R.id.etSearch);

        rvPlaces = findViewById(R.id.rvPlaces);
    }

    // ============================================================
    // ⭐ WISHLIST REALTIME
    // ============================================================
    private void listenToWishlist() {
        if (user == null) return;

        db.collection("users").document(user.getUid()).collection("wishlist")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    wishlistIds.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        wishlistIds.add(doc.getId());
                    }

                    if (placeList != null) {
                        for (PlaceModel p : placeList) {
                            String docId = p.getName().replaceAll("[^a-zA-Z0-9]", "_");
                            p.setFavorite(wishlistIds.contains(docId));
                        }
                        placeAdapter.notifyDataSetChanged();
                    }
                });
    }

    // ============================================================
    // ⭐ SETUP LIST
    // ============================================================
    private void setupRecyclerView() {

        placeList = new ArrayList<>();
        rvPlaces.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Hiệu ứng Snap căn giữa
        new PagerSnapHelper().attachToRecyclerView(rvPlaces);

        // TẠO ADAPTER ĐẦY ĐỦ 3 CALLBACK
        placeAdapter = new PlaceAdapter(
                placeList,

                // ============================
                // 1. CLICK ITEM → ZOOM MAP + DETAIL
                // ============================
                place -> {
                    if (mMap != null) {
                        LatLng loc = new LatLng(place.getLat(), place.getLon());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 17f));

                        Marker m = mMap.addMarker(new MarkerOptions().position(loc).title(place.getName()));
                        if (m != null) m.showInfoWindow();
                    }

                    Intent intent = new Intent(HomeActivity.this, PlaceDetailActivity.class);
                    intent.putExtra("id", place.getId());
                    intent.putExtra("name", place.getName());
                    intent.putExtra("address", place.getAddress());
                    intent.putExtra("rating", place.getRating());
                    intent.putExtra("lat", place.getLat());
                    intent.putExtra("lon", place.getLon());
                    startActivity(intent);
                },

                // ============================
                // 2. CLICK TIM → WISHLIST
                // ============================
                place -> {
                    if (place.isFavorite()) addToWishlist(place);
                    else removeFromWishlist(place);
                },

                // ============================
                // 3. CLICK "Đường đi >"
                // ============================
                place -> {
                    // Lấy vị trí hiện tại
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(location -> {
                                if (location == null) {
                                    Toast.makeText(this, "Không lấy được vị trí hiện tại!", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                double startLat = location.getLatitude();
                                double startLon = location.getLongitude();

                                double endLat = place.getLat();
                                double endLon = place.getLon();

                                Log.d("DIRECTION", "Đi từ: " + startLat + "," + startLon);
                                Log.d("DIRECTION", "Đến: " + endLat + "," + endLon);

                                requestRoute(startLat, startLon, endLat, endLon);
                            });
                }
        );

        rvPlaces.setAdapter(placeAdapter);
    }

    private void requestRoute(double startLat, double startLon, double endLat, double endLon) {

        String coords = startLon + "," + startLat + ";" + endLon + "," + endLat;

        OSRMApi api = RetrofitClient.getApi();

        Call<OSRMResponse> call = api.getRoute(
                coords,
                "full",
                "polyline"
        );

        call.enqueue(new Callback<OSRMResponse>() {
            @Override
            public void onResponse(Call<OSRMResponse> call, Response<OSRMResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e("OSRM", "Dữ liệu OSRM không hợp lệ");
                    return;
                }

                try {
                    String encoded = response.body().getRoutes().get(0).getGeometry();
                    List<LatLng> points = OSRMPolylineDecoder.decode(encoded);

                    mMap.addPolyline(new PolylineOptions()
                            .addAll(points)
                            .width(12)
                            .color(Color.BLUE));

                    Log.d("OSRM", "Vẽ đường thành công!");

                } catch (Exception e) {
                    Log.e("OSRM_ERR", e.toString());
                }
                Log.d("OSRM_RAW", new Gson().toJson(response.body()));

            }

            @Override
            public void onFailure(Call<OSRMResponse> call, Throwable t) {
                Log.e("OSRM_FAIL", t.getMessage());
            }
        });
    }



    // ============================================================
    // ⭐ AI → NOMINATIM → OSM SEARCH
    // ============================================================
    private void runAI(String text) {
        gpt.analyzeQuery(text, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                runOnUiThread(() -> {
                    try {
                        String category = json.getString("category");
                        String location = json.getString("location");
                        int radius = json.getInt("radius");
                        getLocationFromAI(location, category, radius);
                    } catch (Exception e) { Log.e("AI_PARSE", e.getMessage()); }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(HomeActivity.this, "Lỗi AI: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void getLocationFromAI(String locationName, String category, int radius) {
        String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + locationName.replace(" ", "+");

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.length() == 0) {
                            Toast.makeText(this, "Không tìm thấy vùng: " + locationName, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        double lat = response.getJSONObject(0).getDouble("lat");
                        double lon = response.getJSONObject(0).getDouble("lon");

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 14f));
                        searchOSM_AI(category, lat, lon, radius);
                    } catch (Exception e) { Log.e("NOMI_ERR", e.getMessage()); }
                },
                error -> Toast.makeText(this, "Lỗi map (403 hoặc quá tải)", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("User-Agent", "Mozilla/5.0 (Android) TravelMate/1.0");
                return h;
            }
        };

        queue.add(req);
    }
    private String[] mapCategoryToOSM(String category) {
        switch (category.toLowerCase()) {
            case "hotel":
                return new String[]{"tourism", "hotel"};
            case "restaurant":
                return new String[]{"amenity", "restaurant"};
            case "cafe":
                return new String[]{"amenity", "cafe"};
            case "bar":
                return new String[]{"amenity", "bar"};
            case "fast_food":
                return new String[]{"amenity", "fast_food"};
        }
        return new String[]{"amenity", category}; // fallback
    }

    private void searchOSM_AI(String category, double lat, double lon, int radius) {
        String[] loc= mapCategoryToOSM(category);

        String url = "https://overpass-api.de/api/interpreter?data=[out:json];"
                + "node[\"" + loc[0] + "\"=\"" + loc[1] + "\"](around:"
                + radius + "," + lat + "," + lon + ");out 50;";


        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    try {
                        mMap.clear();
                        placeList.clear();

                        JSONArray arr = res.getJSONArray("elements");
                        int limit = Math.min(arr.length(), 15);

                        if (limit == 0) {
                            Toast.makeText(this, "Không tìm thấy địa điểm nào.", Toast.LENGTH_SHORT).show();
                            rvPlaces.setVisibility(View.GONE);
                            return;
                        }

                        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

                        for (int i = 0; i < limit; i++) {
                            JSONObject o = arr.getJSONObject(i);
                            if (!o.has("lat")) continue;

                            double la = o.getDouble("lat");
                            double lo = o.getDouble("lon");

                            String name = "Địa điểm";
                            if (o.has("tags")) {
                                JSONObject tags = o.getJSONObject("tags");
                                if (tags.has("name")) name = tags.getString("name");
                                else if (tags.has("brand")) name = tags.getString("brand");
                            }

                            String address = "Đang cập nhật...";
                            try {
                                List<Address> addrs = geocoder.getFromLocation(la, lo, 1);
                                if (addrs != null && !addrs.isEmpty()) {
                                    address = addrs.get(0).getAddressLine(0);
                                }
                            } catch (IOException ignored) {}

                            double rating = 3.5 + (Math.random() * 1.5);

                            PlaceModel place = new PlaceModel(name, address, rating, la, lo);

                            String docId = name.replaceAll("[^a-zA-Z0-9]", "_");
                            place.setFavorite(wishlistIds.contains(docId));

                            placeList.add(place);
                            mMap.addMarker(new MarkerOptions().position(new LatLng(la, lo)).title(name));
                        }

                        rvPlaces.setVisibility(View.VISIBLE);
                        placeAdapter.notifyDataSetChanged();

                    } catch (Exception e) { Log.e("OSM_ERR", e.getMessage()); }
                },
                error -> Toast.makeText(this, "Lỗi tìm kiếm", Toast.LENGTH_SHORT).show()
        );

        queue.add(req);
    }

    // ============================================================
    // ⭐ WISHLIST
    // ============================================================
    private void addToWishlist(PlaceModel place) {
        if (user == null) return;
        String docId = place.getName().replaceAll("[^a-zA-Z0-9]", "_");

        Map<String, Object> data = new HashMap<>();
        data.put("name", place.getName());
        data.put("address", place.getAddress());
        data.put("rating", place.getRating());
        data.put("lat", place.getLat());
        data.put("lon", place.getLon());
        data.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(user.getUid())
                .collection("wishlist").document(docId)
                .set(data);
    }

    private void removeFromWishlist(PlaceModel place) {
        if (user == null) return;
        String docId = place.getName().replaceAll("[^a-zA-Z0-9]", "_");

        db.collection("users").document(user.getUid())
                .collection("wishlist").document(docId)
                .delete();
    }

    // ============================================================
    // ⭐ USER INFO
    // ============================================================
    private void setupUserProfile() {
        if (user != null) {
            db.collection("users").document(user.getUid()).get().addOnSuccessListener(s -> {
                if (s.exists()) {
                    tvHello.setText("Hello " + s.getString("name") + "!");
                    String url = s.getString("profileImageUrl");
                    if (url != null) Glide.with(this).load(url).placeholder(R.drawable.avttest).into(ivProfile);
                } else tvHello.setText("Hello " + user.getDisplayName());
            });
        }

        ivProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );
    }

    // ============================================================
    // ⭐ GOOGLE MAP READY → AUTO ZOOM GPS
    // ============================================================
    @Override
    public void onMapReady(GoogleMap gm) {
        mMap = gm;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        getCurrentLocation();
//        Test
        mMap.setOnMapClickListener(point -> {
            double startLat = 10.8450;
            double startLon = 106.7963;

            double endLat = point.latitude;
            double endLon = point.longitude;

            Log.d("TEST_OSRM", "Start: " + startLat + "," + startLon);
            Log.d("TEST_OSRM", "End: " + endLat + "," + endLon);

            requestRoute(startLat, startLon, endLat, endLon);
        });

    }

    // ============================================================
    // ⭐ LẤY GPS HIỆN TẠI
    // ============================================================
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && mMap != null) {

                        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));

                        mMap.addMarker(new MarkerOptions()
                                .position(pos)
                                .title("Bạn đang ở đây ⭐"));
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);

        if (code == 101 && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }

    // ============================================================
    // ⭐ NAV
    // ============================================================
    private void setupBottomNav() {
        setActive(navHome);

        navHome.setOnClickListener(this::onNavClick);
        navBookmark.setOnClickListener(this::onNavClick);
        navChat.setOnClickListener(this::onNavClick);
        navCalendar.setOnClickListener(this::onNavClick);
        navNotification.setOnClickListener(this::onNavClick);
    }

    private void onNavClick(View v) {
        resetNav();
        setActive((LinearLayout) v);

        if (v.getId() == R.id.navBookmark) {
            startActivity(new Intent(this, WishlistActivity.class));
            overridePendingTransition(0, 0);
        } else if (v.getId() == R.id.navChat) {
            startActivity(new Intent(this, ChatListActivity.class));
            overridePendingTransition(0, 0);
        }
    }

    private void resetNav() {
        navHome.setBackground(null);
        navBookmark.setBackground(null);
        navChat.setBackground(null);
        navCalendar.setBackground(null);
        navNotification.setBackground(null);
    }

    private void setActive(LinearLayout l) {
        l.setBackgroundResource(R.drawable.nav_item_selected_bg);
    }

}
