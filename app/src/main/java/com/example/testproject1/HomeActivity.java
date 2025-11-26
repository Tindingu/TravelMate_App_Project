package com.example.testproject1;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot; // Import mới
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet; // Import mới
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set; // Import mới

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    // UI Components
    LinearLayout navHome, navBookmark, navCalendar, navNotification;
    TextView tvHello;
    ImageView ivProfile, ivSearchBtn;
    EditText etSearch;

    // RecyclerView & Data
    RecyclerView rvPlaces;
    PlaceAdapter placeAdapter;
    ArrayList<PlaceModel> placeList;
    Set<String> wishlistIds = new HashSet<>(); // ⭐ Cache danh sách ID yêu thích

    // Firebase & Map
    FirebaseAuth auth;
    FirebaseUser user;
    FirebaseFirestore db;
    GoogleMap mMap;
    RequestQueue queue;
    GeminiService gpt;

    private static final String API_KEY_GEMINI = "AIzaSyDMXgF8hZRMrW18nfh03MBlYegJmpaZXng";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. Khởi tạo
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();
        queue = Volley.newRequestQueue(this);
        gpt = new GeminiService(API_KEY_GEMINI);

        // 2. Setup UI & Data
        initViews();
        setupUserProfile();
        setupRecyclerView();
        setupBottomNav();

        // 3. Setup Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.homeMap);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // 4. ⭐ Lắng nghe Wishlist từ Firestore (Realtime Update)
        listenToWishlist();

        // 5. Search Event
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
        navCalendar = findViewById(R.id.navCalendar);
        navNotification = findViewById(R.id.navNotification);
        tvHello = findViewById(R.id.tvHello);
        ivProfile = findViewById(R.id.ivProfile);
        etSearch = findViewById(R.id.etSearch);
        ivSearchBtn = findViewById(R.id.ivSearch);
        rvPlaces = findViewById(R.id.rvPlaces);
    }

    // ============================================================
    // ⭐ LOGIC 1: LẮNG NGHE WISHLIST (ĐỂ ĐỔI MÀU TIM)
    // ============================================================
    private void listenToWishlist() {
        if (user == null) return;

        // Lắng nghe realtime collection 'wishlist'
        db.collection("users").document(user.getUid()).collection("wishlist")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        wishlistIds.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            // Lưu ID (tên quán đã chuẩn hóa) vào cache
                            wishlistIds.add(doc.getId());
                        }
                        // Cập nhật lại giao diện nếu đang hiển thị
                        if (placeList != null && !placeList.isEmpty()) {
                            for (PlaceModel p : placeList) {
                                String docId = p.getName().replaceAll("[^a-zA-Z0-9]", "_");
                                p.setFavorite(wishlistIds.contains(docId));
                            }
                            placeAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    // ============================================================
    // ⭐ LOGIC 2: SETUP LIST NGANG & CLICK EVENT
    // ============================================================
    private void setupRecyclerView() {
        placeList = new ArrayList<>();
        rvPlaces.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Hiệu ứng Snap (Căn giữa item khi vuốt)
        new PagerSnapHelper().attachToRecyclerView(rvPlaces);

        placeAdapter = new PlaceAdapter(placeList,
                // Click thẻ -> Zoom Map
                place -> {
                    if (mMap != null) {
                        LatLng loc = new LatLng(place.getLat(), place.getLon());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 17f));
                        Marker m = mMap.addMarker(new MarkerOptions().position(loc).title(place.getName()));
                        if (m != null) m.showInfoWindow();
                    }
                },
                // Click Tim -> Thêm/Xóa Wishlist
                place -> {
                    if (place.isFavorite()) addToWishlist(place);
                    else removeFromWishlist(place);
                }
        );
        rvPlaces.setAdapter(placeAdapter);
    }

    // ============================================================
    // ⭐ LOGIC 3: TÌM KIẾM (AI -> NOMINATIM -> OSM + GEOCODER)
    // ============================================================
    private void runAI(String text) {
        gpt.analyzeQuery(text, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                // Chuyển về luồng UI để tránh lỗi
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
                runOnUiThread(() -> Toast.makeText(HomeActivity.this, "Lỗi AI: " + error, Toast.LENGTH_SHORT).show());
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
                error -> {
                    if (error.networkResponse != null && error.networkResponse.statusCode == 403) {
                        Toast.makeText(this, "Lỗi bản đồ (403): Bị chặn do quá tải.", Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            // Thêm User-Agent để tránh lỗi 403
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("User-Agent", "Mozilla/5.0 (Android) TravelMate/1.0");
                return h;
            }
        };
        queue.add(req);
    }

    private void searchOSM_AI(String category, double lat, double lon, int radius) {
        String url = "https://overpass-api.de/api/interpreter?data=[out:json];"
                + "node[\"amenity\"=\"" + category + "\"](around:" + radius + "," + lat + "," + lon + ");out 50;";

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

                        Geocoder geocoder = new Geocoder(HomeActivity.this, Locale.getDefault());

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

                            // Lấy địa chỉ THẬT
                            String address = "Đang cập nhật...";
                            try {
                                List<Address> addrs = geocoder.getFromLocation(la, lo, 1);
                                if (addrs != null && !addrs.isEmpty()) {
                                    address = addrs.get(0).getAddressLine(0)
                                            .replace(", Vietnam", "").replace(", Việt Nam", "");
                                }
                            } catch (IOException e) {
                                if (o.has("tags") && o.getJSONObject("tags").has("addr:street"))
                                    address = o.getJSONObject("tags").getString("addr:street");
                            }

                            double rating = 3.5 + (Math.random() * 1.5);
                            PlaceModel place = new PlaceModel(name, address, rating, la, lo);

                            // ⭐ Kiểm tra trạng thái Tim Đỏ
                            String docId = name.replaceAll("[^a-zA-Z0-9]", "_");
                            if (wishlistIds.contains(docId)) {
                                place.setFavorite(true);
                            }

                            placeList.add(place);
                            mMap.addMarker(new MarkerOptions().position(new LatLng(la, lo)).title(name));
                        }

                        placeAdapter.notifyDataSetChanged();
                        rvPlaces.setVisibility(View.VISIBLE); // Hiện list

                    } catch (Exception e) { Log.e("OSM_ERR", e.getMessage()); }
                },
                error -> Toast.makeText(this, "Lỗi tìm kiếm", Toast.LENGTH_SHORT).show()
        );
        queue.add(req);
    }

    // ... (Wishlist Add/Remove & Nav giữ nguyên) ...
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
                .set(data)
                .addOnSuccessListener(v -> Toast.makeText(this, "Đã lưu!", Toast.LENGTH_SHORT).show());
    }

    private void removeFromWishlist(PlaceModel place) {
        if (user == null) return;
        String docId = place.getName().replaceAll("[^a-zA-Z0-9]", "_");
        db.collection("users").document(user.getUid())
                .collection("wishlist").document(docId)
                .delete()
                .addOnSuccessListener(v -> Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show());
    }

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
        ivProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override public void onMapReady(GoogleMap gm) { mMap = gm; mMap.getUiSettings().setZoomControlsEnabled(false); }

    private void setupBottomNav() {
        setActive(navHome);
        navHome.setOnClickListener(this::onNavClick);
        navBookmark.setOnClickListener(this::onNavClick);
        navCalendar.setOnClickListener(this::onNavClick);
        navNotification.setOnClickListener(this::onNavClick);
    }
    private void onNavClick(View v) {
        resetNav();
        setActive((LinearLayout) v);
        if (v.getId() == R.id.navBookmark){
            startActivity(new Intent(this, WishlistActivity.class));
            overridePendingTransition(0, 0);
        }

    }
    private void resetNav() { navHome.setBackground(null); navBookmark.setBackground(null); navCalendar.setBackground(null); navNotification.setBackground(null); }
    private void setActive(LinearLayout l) { l.setBackgroundResource(R.drawable.nav_item_selected_bg); }
}