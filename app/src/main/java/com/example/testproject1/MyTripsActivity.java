package com.example.testproject1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MyTripsActivity extends AppCompatActivity {

    private RecyclerView rvMyTrips;
    private TripAdapter adapter;
    private List<TripModel> tripList;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trips);

        // Init Firestore & Auth
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            // Xử lý nếu chưa login (vd: finish() hoặc yêu cầu login)
            Toast.makeText(this, "Bạn cần đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        rvMyTrips = findViewById(R.id.rvMyTrips);
        tripList = new ArrayList<>();

        // Setup RecyclerView
        adapter = new TripAdapter(this, tripList, trip -> {
            // KHI BẤM VÀO 1 CHUYẾN ĐI -> MỞ MÀN HÌNH CHI TIẾT
            Intent intent = new Intent(MyTripsActivity.this, TripDetailActivity.class);
            intent.putExtra("trip_data", trip); // Truyền dữ liệu chuyến đi sang
            startActivity(intent);
        });
        rvMyTrips.setLayoutManager(new LinearLayoutManager(this));
        rvMyTrips.setAdapter(adapter);

        // Load dữ liệu
        loadUserTrips();

        // Nút tạo chuyến đi mới
        findViewById(R.id.btnCreateTrip).setOnClickListener(v -> showCreateTripDialog());
        setupBottomNav();
    }
    private void setupBottomNav() {
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navBookmark = findViewById(R.id.navBookmark);
        LinearLayout navCalendar = findViewById(R.id.navCalendar);
        LinearLayout navNotification = findViewById(R.id.navNotification);

        // 1. Nút Home: Quay về HomeActivity
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(MyTripsActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish(); // Đóng màn hình hiện tại
        });

        // 2. Nút Bookmark: Chuyển sang Wishlist
        navBookmark.setOnClickListener(v -> {
            Intent intent = new Intent(MyTripsActivity.this, WishlistActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish(); // Đóng màn hình hiện tại để tránh chồng activities
        });

        // 3. Nút Calendar: Đang ở đây rồi nên không làm gì cả (hoặc reload)
        navCalendar.setOnClickListener(v -> {
            // Do nothing
        });

        // 4. Notification
        navNotification.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        });
    }
    private void loadUserTrips() {
        db.collection("trips")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        tripList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            TripModel trip = document.toObject(TripModel.class);
                            tripList.add(trip);
                        }
                        if (tripList.isEmpty()) {
                            // Nếu không có chuyến đi: Hiện hình minh họa, Ẩn danh sách
                            findViewById(R.id.layoutEmptyState).setVisibility(View.VISIBLE);
                            findViewById(R.id.rvMyTrips).setVisibility(View.GONE);
                        } else {
                            // Có dữ liệu: Ẩn hình minh họa, Hiện danh sách
                            findViewById(R.id.layoutEmptyState).setVisibility(View.GONE);
                            findViewById(R.id.rvMyTrips).setVisibility(View.VISIBLE);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void showCreateTripDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tạo chuyến đi mới");

        // Layout đơn giản cho dialog (hoặc bạn có thể tạo layout xml riêng)
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_create_trip, null, false);
        // ⚠️ Lưu ý: Bạn cần tạo layout dialog_create_trip.xml (xem bên dưới)

        final EditText inputName = viewInflated.findViewById(R.id.etTripName);
        final EditText inputStart = viewInflated.findViewById(R.id.etStartDate);
        final EditText inputEnd = viewInflated.findViewById(R.id.etEndDate);

        builder.setView(viewInflated);

        builder.setPositiveButton("Tạo", (dialog, which) -> {
            String name = inputName.getText().toString();
            String start = inputStart.getText().toString();
            String end = inputEnd.getText().toString();

            if (!name.isEmpty()) {
                createNewTrip(name, start, end);
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createNewTrip(String name, String start, String end) {
        String tripId = db.collection("trips").document().getId();
        TripModel newTrip = new TripModel(tripId, name, start, end, currentUserId);

        db.collection("trips").document(tripId).set(newTrip)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã tạo chuyến đi!", Toast.LENGTH_SHORT).show();
                    loadUserTrips(); // Refresh list
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}