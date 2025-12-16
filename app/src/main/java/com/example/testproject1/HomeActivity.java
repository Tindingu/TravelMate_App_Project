
package com.example.testproject1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    // =========================
    // UI
    // =========================
    private LinearLayout navHome, navBookmark, navCalendar, navNotification;
    private TextView tvHello;
    private ImageView ivProfile, ivSearchBtn;
    private EditText etSearch;

    // RecyclerView
    private RecyclerView rvPlaces;
    private PlaceAdapter placeAdapter;
    private ArrayList<PlaceModel> placeList;
    private final Set<String> wishlistIds = new HashSet<>();

    // Firebase + Map + Network
    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private GoogleMap mMap;
    private RequestQueue queue;
    private GeminiService gpt;

    // GPS
    private FusedLocationProviderClient fusedLocationClient;

    // Filter Panel
    private LinearLayout filterPanel;
    private View dimBackground;
    private ImageView ivMenu;
    private boolean isFilterOpen = false;

    private CheckBox cbRestaurant, cbCafe, cbHotel, cbEntertainment, cbBar;
    private SeekBar seekDistance, seekRating, seekSoLuong;
    private TextView tvDistanceValue, tvRatingValue, tvSoLuong;
    private Button btnApplyFilter, btnResetFilter;

    private final Set<String> selectedTypes = new HashSet<>();
    private int selectedDistance = 1000; // meters
    private int selectedRating = 3;
    private int selectedSoLuong = 15;

    private LinearLayout layoutResultInfo;
    private TextView tvResultInfo;

    // TODO: set API key
    private static final String API_KEY_GEMINI = "AIzaSyBVp_j43nfQEHK2RPaVsnIT2ogIENGnhOQ";

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
        setupFilterLogic();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.homeMap);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        listenToWishlist();

        ivSearchBtn.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                Toast.makeText(this, "AI đang tìm kiếm...", Toast.LENGTH_SHORT).show();
                runAI(query);
            }
        });

        ivMenu.setOnClickListener(v -> toggleFilter());
        dimBackground.setOnClickListener(v -> toggleFilter());
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
        ivSearchBtn = findViewById(R.id.ivSearch);
        etSearch = findViewById(R.id.etSearch);

        rvPlaces = findViewById(R.id.rvPlaces);

        filterPanel = findViewById(R.id.filterPanel);
        dimBackground = findViewById(R.id.dimBackground);
        ivMenu = findViewById(R.id.ivFilter);

        cbRestaurant = findViewById(R.id.cbRestaurant);
        cbCafe = findViewById(R.id.cbCafe);
        cbHotel = findViewById(R.id.cbHotel);
        cbEntertainment = findViewById(R.id.cbEntertainment);
        cbBar = findViewById(R.id.cbBar);

        seekDistance = findViewById(R.id.seekDistance);
        tvDistanceValue = findViewById(R.id.tvDistanceValue);

        seekSoLuong = findViewById(R.id.seekSoLuong);
        tvSoLuong = findViewById(R.id.tvSoLuong);

        seekRating = findViewById(R.id.seekRating);
        tvRatingValue = findViewById(R.id.tvRatingValue);

        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        btnResetFilter = findViewById(R.id.btnResetFilter);

        layoutResultInfo = findViewById(R.id.layoutResultInfo);
        tvResultInfo = findViewById(R.id.tvResultInfo);
    }

    // ============================================================
    // ⭐ WISHLIST REALTIME
    // ============================================================
    private void listenToWishlist() {
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .collection("wishlist")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    wishlistIds.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        wishlistIds.add(doc.getId());
                    }

                    if (placeList != null) {
                        for (PlaceModel p : placeList) {
                            String docId = p.getName().replaceAll("[^a-zA-Z0-9]", "_");
                            p.setFavorite(wishlistIds.contains(docId));
                        }
                        if (placeAdapter != null) placeAdapter.notifyDataSetChanged();
                    }
                });
    }

    // ============================================================
    // ⭐ SETUP LIST
    // ============================================================
    private void setupRecyclerView() {
        placeList = new ArrayList<>();

        rvPlaces.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        new PagerSnapHelper().attachToRecyclerView(rvPlaces);

        placeAdapter = new PlaceAdapter(
                placeList,

                // 1) CLICK ITEM → ZOOM MAP + DETAIL
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

                // 2) CLICK TIM → WISHLIST
                place -> {
                    if (place.isFavorite()) addToWishlist(place);
                    else removeFromWishlist(place);
                },

                // 3) CLICK "Đường đi >"
                place -> {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(
                                this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                101
                        );
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
        if (mMap == null) return;

        String coords = startLon + "," + startLat + ";" + endLon + "," + endLat;

        OSRMApi api = RetrofitClient.getApi();
        Call<OSRMResponse> call = api.getRoute(coords, "full", "polyline");

        call.enqueue(new Callback<OSRMResponse>() {
            @Override
            public void onResponse(@NonNull Call<OSRMResponse> call, @NonNull Response<OSRMResponse> response) {
                if (!response.isSuccessful() || response.body() == null
                        || response.body().getRoutes() == null
                        || response.body().getRoutes().isEmpty()) {
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
            public void onFailure(@NonNull Call<OSRMResponse> call, @NonNull Throwable t) {
                Log.e("OSRM_FAIL", String.valueOf(t.getMessage()));
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
                    } catch (Exception e) {
                        Log.e("AI_PARSE", String.valueOf(e.getMessage()));
                    }
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
        String url = "https://nominatim.openstreetmap.org/search?format=json&q="
                + locationName.replace(" ", "+");

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.length() == 0) {
                            Toast.makeText(this, "Không tìm thấy vùng: " + locationName, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        double lat = response.getJSONObject(0).getDouble("lat");
                        double lon = response.getJSONObject(0).getDouble("lon");

                        if (mMap != null) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 14f));
                        searchOSM_AI(category, lat, lon, radius);

                    } catch (Exception e) {
                        Log.e("NOMI_ERR", String.valueOf(e.getMessage()));
                    }
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

    private List<String[]> mapCategoryToOSM(String category) {
        // NOTE: giữ behavior của bạn: category sẽ được đưa vào selectedTypes
        List<String[]> tags = new ArrayList<>();
        String c = category == null ? "" : category.toLowerCase();

        selectedTypes.add(c);

        for (String a : selectedTypes) {
            switch (a) {
                case "hotel":
                case "resort":
                case "homestay":
                    tags.add(new String[]{"tourism", "hotel"});
                    break;

                case "restaurant":
                case "food":
                    tags.add(new String[]{"amenity", "restaurant"});
                    break;

                case "cafe":
                case "coffee":
                    tags.add(new String[]{"amenity", "cafe"});
                    tags.add(new String[]{"shop", "coffee"});
                    break;

                case "bar":
                case "pub":
                    tags.add(new String[]{"amenity", "bar"});
                    tags.add(new String[]{"amenity", "pub"});
                    break;

                case "fast_food":
                    tags.add(new String[]{"amenity", "fast_food"});
                    break;

                case "park":
                case "garden":
                    tags.add(new String[]{"leisure", "park"});
                    tags.add(new String[]{"leisure", "garden"});
                    break;

                case "supermarket":
                    tags.add(new String[]{"shop", "supermarket"});
                    break;

                default:
                    if (!a.isEmpty()) tags.add(new String[]{"amenity", a});
                    break;
            }
        }
        return tags;
    }

    private String buildOverpassQuery(List<String[]> tags, double lat, double lon, int radius) {
        StringBuilder sb = new StringBuilder();
        sb.append("https://overpass-api.de/api/interpreter?data=[out:json];(");

        for (String[] t : tags) {
            sb.append("node[\"")
                    .append(t[0]).append("\"=\"")
                    .append(t[1]).append("\"](around:")
                    .append(radius).append(",")
                    .append(lat).append(",")
                    .append(lon).append(");");
        }

        sb.append(");out 50;");
        return sb.toString();
    }

    private void searchOSM_AI(String category, double lat, double lon, int radius) {
        if (mMap == null) return;

        List<String[]> tagss = mapCategoryToOSM(category);
        String url = buildOverpassQuery(tagss, lat, lon, radius);

        Log.d("OVERPASS_URL", url);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                res -> {
                    try {
                        mMap.clear();
                        placeList.clear();

                        JSONArray arr = res.getJSONArray("elements");
                        int limit = Math.min(arr.length(), selectedSoLuong);

                        if (limit == 0) {
                            Toast.makeText(this, "Không tìm thấy địa điểm nào.", Toast.LENGTH_SHORT).show();
                            rvPlaces.setVisibility(View.GONE);
                            return;
                        }

                        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

                        for (int i = 0; i < limit; i++) {
                            JSONObject o = arr.getJSONObject(i);
                            if (!o.has("lat") || !o.has("lon")) continue;

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
                            } catch (Exception ignored) {}

                            // 1) Tạo place trước
                            PlaceModel place = new PlaceModel(name, address, 0.0, la, lo);

                            String favId = name.replaceAll("[^a-zA-Z0-9]", "_");
                            place.setFavorite(wishlistIds.contains(favId));

                            placeList.add(place);

                            mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(la, lo))
                                    .title(name));

                            // 2) Load rating sau (async) + update đúng item
                            String placesID = la + "_" + lo;
                            ensurePlaceExists(placesID, name, address, la, lo);
                            if (user != null) {
                                db.collection("places")
                                        .document(placesID)
                                        .get()
                                        .addOnSuccessListener(doc -> {
                                            double rating = 0.0;
                                            if (doc.exists() && doc.getDouble("ratingAvg") != null) {
                                                rating = doc.getDouble("ratingAvg");
                                            }

                                            place.setRating(rating);

                                            int realIndex = placeList.indexOf(place);
                                            if (realIndex != -1 && placeAdapter != null) {
                                                placeAdapter.notifyItemChanged(realIndex);
                                            }
                                        });
                            }
                        }

                        rvPlaces.setVisibility(View.VISIBLE);
                        if (placeAdapter != null) placeAdapter.notifyDataSetChanged();

                        showResultInfoAutoHide(placeList.size(), selectedSoLuong);

                    } catch (Exception e) {
                        Log.e("OSM_ERR", String.valueOf(e.getMessage()));
                    }
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

        db.collection("users")
                .document(user.getUid())
                .collection("wishlist")
                .document(docId)
                .set(data);
    }

    private void removeFromWishlist(PlaceModel place) {
        if (user == null) return;

        String docId = place.getName().replaceAll("[^a-zA-Z0-9]", "_");

        db.collection("users")
                .document(user.getUid())
                .collection("wishlist")
                .document(docId)
                .delete();
    }

    // ============================================================
    // ⭐ USER INFO (Home chỉ hiển thị hello + avatar)
    // ============================================================
    private void setupUserProfile() {
        if (user != null) {
            db.collection("users")
                    .document(user.getUid())
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null || snapshot == null || !snapshot.exists()) return;

                        String name = snapshot.getString("name");
                        String avatarUrl = snapshot.getString("photoUrl");

                        tvHello.setText("Hello " + name + "!");

                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.avttest)
                                    .circleCrop()
                                    .into(ivProfile);
                        }
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
    public void onMapReady(@NonNull GoogleMap gm) {
        mMap = gm;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        getCurrentLocation();

        // (optional) test: click map to draw route from fixed point
        // Bạn có thể comment nếu không cần
        mMap.setOnMapClickListener(point -> {
            double startLat = 10.8450;
            double startLon = 106.7963;
            requestRoute(startLat, startLon, point.latitude, point.longitude);
        });
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    101
            );
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && mMap != null) {
                        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
                        mMap.addMarker(new MarkerOptions().position(pos).title("Bạn đang ở đây ⭐"));
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);

        if (code == 101 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
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
        navCalendar.setOnClickListener(this::onNavClick);
        navNotification.setOnClickListener(this::onNavClick);
    }

    private void onNavClick(View v) {
        resetNav();
        setActive((LinearLayout) v);

        if (v.getId() == R.id.navBookmark) {
            startActivity(new Intent(this, WishlistActivity.class));
            overridePendingTransition(0, 0);
        }

        // Nếu bạn có Calendar/Notification activity thì thêm tương tự ở đây.
    }

    private void resetNav() {
        navHome.setBackground(null);
        navBookmark.setBackground(null);
        navCalendar.setBackground(null);
        navNotification.setBackground(null);
    }

    private void setActive(LinearLayout l) {
        l.setBackgroundResource(R.drawable.nav_item_selected_bg);
    }

    // ============================================================
    // ⭐ FILTER UI
    // ============================================================
    private void toggleFilter() {
        if (!isFilterOpen) {
            filterPanel.animate().translationX(0).setDuration(250).start();
            dimBackground.setVisibility(View.VISIBLE);
            dimBackground.animate().alpha(1f).setDuration(250);
            isFilterOpen = true;
        } else {
            filterPanel.animate().translationX(-filterPanel.getWidth()).setDuration(250).start();
            dimBackground.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(() -> dimBackground.setVisibility(View.GONE));
            isFilterOpen = false;
        }
    }

    private void updateSelectedTypesFromCheckbox() {
        selectedTypes.clear();
        if (cbRestaurant.isChecked()) selectedTypes.add("restaurant");
        if (cbCafe.isChecked()) selectedTypes.add("cafe");
        if (cbHotel.isChecked()) selectedTypes.add("hotel");
        if (cbEntertainment.isChecked()) selectedTypes.add("entertainment");
        if (cbBar.isChecked()) selectedTypes.add("bar");
    }

    private void setupFilterLogic() {
        View.OnClickListener typeListener = v -> updateSelectedTypesFromCheckbox();

        cbRestaurant.setOnClickListener(typeListener);
        cbCafe.setOnClickListener(typeListener);
        cbHotel.setOnClickListener(typeListener);
        cbEntertainment.setOnClickListener(typeListener);
        cbBar.setOnClickListener(typeListener);

        seekDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // giữ cách tính của bạn nhưng tránh = 0 quá dễ
                selectedDistance = Math.max(100, (progress / 100) * 100);
                tvDistanceValue.setText(String.format(Locale.getDefault(), "%.1f km", selectedDistance / 1000.0));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekRating.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedRating = progress;
                tvRatingValue.setText(progress + " sao");
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekSoLuong.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedSoLuong = Math.max(1, progress);
                tvSoLuong.setText(selectedSoLuong + " địa điểm");
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnApplyFilter.setOnClickListener(v -> {
            applyFilters();
            toggleFilter();
        });

        btnResetFilter.setOnClickListener(v -> resetFilters());
    }

    private void applyFilters() {
        Log.d("FILTER", "Types = " + selectedTypes);
        Log.d("FILTER", "Distance = " + selectedDistance + "m");
        Log.d("FILTER", "Rating >= " + selectedRating);

        Toast.makeText(this, "Đã áp dụng bộ lọc!", Toast.LENGTH_SHORT).show();

        // NOTE: hiện tại filter chỉ log. Nếu muốn lọc thật:
        // - gọi lại searchOSM_AI với radius = selectedDistance
        // - hoặc lọc placeList theo rating >= selectedRating
    }

    private void resetFilters() {
        cbRestaurant.setChecked(false);
        cbCafe.setChecked(false);
        cbHotel.setChecked(false);
        cbEntertainment.setChecked(false);
        cbBar.setChecked(false);

        selectedTypes.clear();

        seekDistance.setProgress(1000);
        tvDistanceValue.setText("1.0 km");
        selectedDistance = 1000;

        seekRating.setProgress(3);
        tvRatingValue.setText("3 sao");
        selectedRating = 3;

        // nếu bạn muốn reset cả số lượng:
        // seekSoLuong.setProgress(15);
        // selectedSoLuong = 15;
        // tvSoLuong.setText("15 địa điểm");
    }

    private void showResultInfoAutoHide(int found, int expected) {
        layoutResultInfo.setVisibility(View.VISIBLE);
        layoutResultInfo.setAlpha(0f);

        tvResultInfo.setText("Đã tìm thấy " + found + "/" + expected + " địa điểm");

        if (found < expected) {
            layoutResultInfo.setBackgroundResource(R.drawable.bg_result_info_warning);
        } else {
            layoutResultInfo.setBackgroundResource(R.drawable.bg_result_info);
        }

        layoutResultInfo.animate().alpha(1f).setDuration(300).start();

        layoutResultInfo.postDelayed(() -> {
            layoutResultInfo.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> layoutResultInfo.setVisibility(View.GONE))
                    .start();
        }, 5000);
    }
    private void ensurePlaceExists(String placeId,
                                   String name,
                                   String address,
                                   double lat,
                                   double lon) {

        db.collection("places")
                .document(placeId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", placeId);
                        data.put("name", name);
                        data.put("address", address);
                        data.put("lat", lat);
                        data.put("lon", lon);
                        data.put("ratingAvg", 0.0);
                        data.put("createdAt",
                                com.google.firebase.firestore.FieldValue.serverTimestamp());

                        db.collection("places")
                                .document(placeId)
                                .set(data);
                    }
                });
    }

}
