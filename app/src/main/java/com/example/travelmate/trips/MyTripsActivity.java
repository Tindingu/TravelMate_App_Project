package com.example.travelmate.trips;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelmate.R;
import com.example.travelmate.chat.ChatListActivity;
import com.example.travelmate.home.HomeActivity;
import com.example.travelmate.home.WishlistActivity;
import com.example.travelmate.notifications.NotificationsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyTripsActivity extends AppCompatActivity {
    LinearLayout navHome, navBookmark, navChat, navCalendar, navNotification;

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
            Toast.makeText(this, "Bạn cần đăng nhập", Toast.LENGTH_SHORT).show();
            // Có thể finish() ở đây nếu muốn bắt buộc login
            return;
        }

        rvMyTrips = findViewById(R.id.rvMyTrips);
        tripList = new ArrayList<>();

        // Setup RecyclerView
        adapter = new TripAdapter(this, tripList, trip -> {
            // KHI BẤM VÀO 1 CHUYẾN ĐI -> MỞ MÀN HÌNH CHI TIẾT
            Intent intent = new Intent(MyTripsActivity.this, TripDetailActivity.class);
            intent.putExtra("trip_data", trip); // Class TripModel phải implements Serializable
            startActivity(intent);
        });
        rvMyTrips.setLayoutManager(new LinearLayoutManager(this));
        rvMyTrips.setAdapter(adapter);

        // Load dữ liệu
        loadUserTrips();

        // Nút tạo chuyến đi mới (FAB hoặc Button trong layout activity_my_trips)
        findViewById(R.id.btnCreateTrip).setOnClickListener(v -> showCreateTripDialog());

        setupBottomNav();
    }

    private void setupBottomNav() {
        navHome = findViewById(R.id.navHome);
        navBookmark = findViewById(R.id.navBookmark);
        navChat = findViewById(R.id.navChat);
        navCalendar = findViewById(R.id.navCalendar);
        navNotification = findViewById(R.id.navNotification);


        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(MyTripsActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        navBookmark.setOnClickListener(v -> {
            Intent intent = new Intent(MyTripsActivity.this, WishlistActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });


        navChat.setOnClickListener(v -> {
            Intent intent = new Intent(MyTripsActivity.this, ChatListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navNotification.setOnClickListener(v ->
                {
                    Intent intent = new Intent(MyTripsActivity.this, NotificationsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                }
        );
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

                        View emptyState = findViewById(R.id.layoutEmptyState);
                        if (tripList.isEmpty()) {
                            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                            rvMyTrips.setVisibility(View.GONE);
                        } else {
                            if (emptyState != null) emptyState.setVisibility(View.GONE);
                            rvMyTrips.setVisibility(View.VISIBLE);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    // ============================================================
    // ⭐ LOGIC TẠO CHUYẾN ĐI MỚI
    // ============================================================
    private void showCreateTripDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Inflate layout mới bạn đã sửa (dialog_create_trip.xml)
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_trip, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        // Làm nền dialog trong suốt để bo góc đẹp hơn
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Ánh xạ View từ dialog_create_trip.xml
        EditText etName = view.findViewById(R.id.etTripName);
        EditText etStart = view.findViewById(R.id.etStartDate);
        EditText etEnd = view.findViewById(R.id.etEndDate);
        Button btnCreate = view.findViewById(R.id.btnCreateTrip);

        // 1. Sự kiện chọn Ngày Bắt Đầu
        etStart.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                etStart.setText(date);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 2. Sự kiện chọn Ngày Kết Thúc
        etEnd.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                etEnd.setText(date);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 3. Sự kiện nút Tạo
        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String start = etStart.getText().toString().trim();
            String end = etEnd.getText().toString().trim();

            // Validate dữ liệu
            if (name.isEmpty() || start.isEmpty() || end.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra ngày kết thúc >= ngày bắt đầu
            if (convertDateToMillis(end) < convertDateToMillis(start)) {
                Toast.makeText(this, "Ngày kết thúc phải sau ngày bắt đầu!", Toast.LENGTH_SHORT).show();
                return;
            }

            createNewTrip(name, start, end, dialog);
        });

        dialog.show();
    }

    private void createNewTrip(String name, String start, String end, AlertDialog dialog) {
        String tripId = db.collection("trips").document().getId();

        // Constructor TripModel(id, name, start, end, userId)
        TripModel newTrip = new TripModel(tripId, name, start, end, currentUserId);

        db.collection("trips").document(tripId).set(newTrip)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Tạo chuyến đi thành công!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadUserTrips();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private long convertDateToMillis(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}