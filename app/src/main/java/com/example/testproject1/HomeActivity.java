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
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import com.bumptech.glide.Glide; // Thư viện tải ảnh
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    // UI Components
    LinearLayout navHome, navBookmark, navCalendar, navNotification;
    TextView tvHello;
    ImageView ivProfile, ivSearchBtn;
    EditText etSearch;

    // RecyclerView cho danh sách địa điểm
    RecyclerView rvPlaces;
    PlaceAdapter placeAdapter;
    ArrayList<PlaceModel> placeList;

    // Firebase
    FirebaseAuth auth;
    FirebaseUser user;
    FirebaseFirestore db;

    // Map & Network
    GoogleMap mMap;
    RequestQueue queue;
    GeminiService gpt;

    // API Key (Lưu ý: Nên bảo mật key này trong thực tế)
    private static final String API_KEY_GEMINI = "AIzaSyAnO3FmEU7NIO9VqpJEimnKO6rZKq6hQRM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. Khởi tạo các dịch vụ
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();
        queue = Volley.newRequestQueue(this);
        gpt = new GeminiService(API_KEY_GEMINI);

        // 2. Ánh xạ View
        initViews();

        // 3. Setup User Profile (Header)
        setupUserProfile();

        // 4. Setup RecyclerView (List địa điểm)
        setupRecyclerView();

        // 5. Setup Bottom Navigation
        setupBottomNav();

        // 6. Setup Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.homeMap);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // 7. Setup Search (Nút tìm kiếm)
        ivSearchBtn.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                Toast.makeText(this, "Đang hỏi AI...", Toast.LENGTH_SHORT).show();
                runAI(query);
            }
        });
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

    private void setupUserProfile() {
        if (user != null) {
            // Lấy tên từ Firestore để chính xác nhất
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            tvHello.setText("Hello " + name + "!");

                            // Nếu có ảnh trong Firestore, load ảnh đó
                            String photoUrl = documentSnapshot.getString("profileImageUrl");
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(this).load(photoUrl).placeholder(R.drawable.avttest).into(ivProfile);
                            }
                        } else {
                            // Fallback nếu chưa có trong Firestore
                            tvHello.setText("Hello " + user.getDisplayName() + "!");
                        }
                    })
                    .addOnFailureListener(e -> tvHello.setText("Hello User!"));
        } else {
            tvHello.setText("Hello Guest!");
        }

        // Click avatar -> Mở ProfileActivity
        ivProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void setupRecyclerView() {
        placeList = new ArrayList<>();

        // Quan trọng: LayoutManager nằm ngang (Horizontal)
        rvPlaces.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        placeAdapter = new PlaceAdapter(placeList, place -> {

            // 🗺️ 1. Zoom tới vị trí (giữ nguyên)
            if (mMap != null) {
                LatLng loc = new LatLng(place.getLat(), place.getLon());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 17f));

                Marker marker = mMap.addMarker(new MarkerOptions().position(loc).title(place.getName()));
                if (marker != null) marker.showInfoWindow();
            }

            // 📌 2. MỞ TRANG CHI TIẾT QUÁN
            Intent intent = new Intent(HomeActivity.this, PlaceDetailActivity.class);
            intent.putExtra("id", place.getId());
            intent.putExtra("name", place.getName());
            intent.putExtra("address", place.getAddress());
            intent.putExtra("rating", place.getRating());
            intent.putExtra("lat", place.getLat());
            intent.putExtra("lon", place.getLon());
            startActivity(intent);
        });


        rvPlaces.setAdapter(placeAdapter);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false); // Tắt nút zoom mặc định cho đẹp
    }

    // ============================================================
    // 🧠 PHẦN 1: GỌI AI (GEMINI)
    // ============================================================
    private void runAI(String text) {
        // Gọi class GeminiService bạn đã có
        gpt.analyzeQuery(text, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                try {
                    String category = json.getString("category");
                    String location = json.getString("location");
                    int radius = json.getInt("radius");

                    Log.d("AI_RESULT", "Category: " + category + ", Loc: " + location);

                    // Có dữ liệu từ AI -> Gọi bước 2
                    getLocationFromAI(location, category, radius);

                } catch (Exception e) {
                    Log.e("AI_PARSE_ERR", e.getMessage());
                    Toast.makeText(HomeActivity.this, "Lỗi đọc dữ liệu AI", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Log.e("AI_ERROR", error);
                Toast.makeText(HomeActivity.this, "AI không phản hồi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ============================================================
    // 🗺️ PHẦN 2: TÌM TỌA ĐỘ VÙNG (NOMINATIM)
    // ============================================================
    private void getLocationFromAI(String locationName, String category, int radius) {
        String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + locationName.replace(" ", "+");

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.length() == 0) {
                            Toast.makeText(this, "Không tìm thấy địa điểm " + locationName, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Lấy kết quả đầu tiên
                        double lat = response.getJSONObject(0).getDouble("lat");
                        double lon = response.getJSONObject(0).getDouble("lon");

                        LatLng center = new LatLng(lat, lon);

                        // Di chuyển map tới vùng đó (VD: Hà Nội)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 14f));

                        // Gọi bước 3: Tìm các địa điểm cụ thể (quán cafe, nhà hàng...)
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
                headers.put("User-Agent", "TravelMateApp/1.0"); // Bắt buộc cho Nominatim
                return headers;
            }
        };
        queue.add(req);
    }

    // ============================================================
    // 📍 PHẦN 3: TÌM ĐỊA ĐIỂM CỤ THỂ & HIỂN THỊ LIST (OVERPASS API)
    // ============================================================
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

                        // Khởi tạo Geocoder
                        Geocoder geocoder = new Geocoder(HomeActivity.this, Locale.getDefault());

                        for (int i = 0; i < limit; i++) {
                            JSONObject o = arr.getJSONObject(i);
                            if (!o.has("lat")) continue;

                            double la = o.getDouble("lat");
                            double lo = o.getDouble("lon");

                            // 1. Lấy tên
                            String name = "Địa điểm";
                            if (o.has("tags")) {
                                JSONObject tags = o.getJSONObject("tags");
                                if (tags.has("name")) name = tags.getString("name");
                                else if (tags.has("brand")) name = tags.getString("brand");
                            }

                            // 2. Lấy địa chỉ THẬT (Geocoder)
                            String address = "Đang cập nhật...";
                            try {
                                List<Address> addresses = geocoder.getFromLocation(la, lo, 1);
                                if (addresses != null && !addresses.isEmpty()) {
                                    // Lấy dòng địa chỉ đầu tiên (thường là đầy đủ nhất)
                                    address = addresses.get(0).getAddressLine(0);

                                    // Tùy chọn: Nếu địa chỉ quá dài, cắt bớt chữ "Vietnam" ở cuối cho gọn
                                    address = address.replace(", Vietnam", "").replace(", Việt Nam", "");
                                }
                            } catch (IOException e) {
                                // Fallback: Nếu mất mạng, thử lấy từ OSM tags
                                if (o.has("tags")) {
                                    JSONObject tags = o.getJSONObject("tags");
                                    if(tags.has("addr:street")) address = tags.getString("addr:street");
                                }
                            }

                            double rating = 3.5 + (Math.random() * 1.5);

                            placeList.add(new PlaceModel(name, address, rating, la, lo));
                            mMap.addMarker(new MarkerOptions().position(new LatLng(la, lo)).title(name));
                        }

                        placeAdapter.notifyDataSetChanged();
                        rvPlaces.setVisibility(View.VISIBLE); // Hiện list ngang lên

                    } catch (Exception e) {
                        Log.e("OSM_PARSE", e.getMessage());
                    }
                },
                error -> Toast.makeText(HomeActivity.this, "Lỗi tìm kiếm!", Toast.LENGTH_SHORT).show()
        );
        queue.add(req);
    }
    // ============================================================
    // Bottom Navigation Logic
    // ============================================================
    private void setupBottomNav() {
        // Mặc định chọn Home
        setActive(navHome);

        navHome.setOnClickListener(this::onNavClick);
        navBookmark.setOnClickListener(this::onNavClick);
        navCalendar.setOnClickListener(this::onNavClick);
        navNotification.setOnClickListener(this::onNavClick);
    }

    private void onNavClick(View v) {
        resetNav();
        setActive((LinearLayout) v);

        if (v.getId() == R.id.navBookmark) {
            // startActivity(new Intent(this, BookmarkActivity.class));
        }
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
