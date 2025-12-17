package com.example.testproject1;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WishlistActivity extends AppCompatActivity {

    // UI
    RecyclerView rvWishlist;
    LinearLayout navHome, navBookmark, navCalendar, navNotification;
    LinearLayout layoutEmpty;
    TextView tvItemCount;

    // Data
    PlaceAdapter adapter;
    ArrayList<PlaceModel> wishlist;
    List<TripModel> myTrips = new ArrayList<>();

    // Firebase
    FirebaseFirestore db;
    String uid;

    // Interface Check Trùng
    interface OnCheckCallback {
        void onResult(@Nullable String conflictName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        // Init Views
        rvWishlist = findViewById(R.id.rvWishlist);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvItemCount = findViewById(R.id.tvItemCount);

        rvWishlist.setLayoutManager(new LinearLayoutManager(this));
        wishlist = new ArrayList<>();

        // Init Firebase
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadData();
        }

        // CẬP NHẬT ADAPTER
        adapter = new PlaceAdapter(
                wishlist,
                place -> {
                    Intent intent = new Intent(this, PlaceDetailActivity.class);
                    intent.putExtra("id", place.getId());
                    intent.putExtra("name", place.getName());
                    intent.putExtra("address", place.getAddress());
                    intent.putExtra("rating", place.getRating());
                    intent.putExtra("lat", place.getLat());
                    intent.putExtra("lon", place.getLon());
                    startActivity(intent);
                },
                place -> removeFromWishlist(place),
                place -> { },
                place -> showAddToTripDialog(place)
        );

        rvWishlist.setAdapter(adapter);
        setupBottomNav();
    }

    // ============================================================
    // ⭐ LOGIC HIỆN DIALOG (CÓ CHECK TRÙNG GIỜ)
    // ============================================================
    private void showAddToTripDialog(PlaceModel selectedPlace) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_to_trip, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        Spinner spTrip = view.findViewById(R.id.spTrip);
        EditText etDate = view.findViewById(R.id.etVisitDate);
        EditText etStartTime = view.findViewById(R.id.etStartTime);
        EditText etEndTime = view.findViewById(R.id.etEndTime);
        EditText etNote = view.findViewById(R.id.etNote);
        Button btnConfirm = view.findViewById(R.id.btnConfirmAdd);

        // Load Trip
        List<String> tripNames = new ArrayList<>();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tripNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTrip.setAdapter(spinnerAdapter);

        if (uid != null) {
            db.collection("trips").whereEqualTo("userId", uid).get()
                    .addOnSuccessListener(querySnapshot -> {
                        myTrips.clear();
                        tripNames.clear();
                        for (DocumentSnapshot doc : querySnapshot) {
                            TripModel trip = doc.toObject(TripModel.class);
                            myTrips.add(trip);
                            tripNames.add(trip.getName());
                        }
                        spinnerAdapter.notifyDataSetChanged();

                        if (myTrips.isEmpty()) {
                            Toast.makeText(this, "Bạn chưa có chuyến đi nào!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    });
        }

        // Chọn Ngày
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                etDate.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Chọn Giờ
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));

        // Lưu
        btnConfirm.setOnClickListener(v -> {
            int pos = spTrip.getSelectedItemPosition();
            if (pos < 0) return;

            TripModel trip = myTrips.get(pos);
            String date = etDate.getText().toString();
            String sTime = etStartTime.getText().toString();
            String eTime = etEndTime.getText().toString();
            String note = etNote.getText().toString();

            if (date.isEmpty() || sTime.isEmpty() || eTime.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (convertTimeToMinutes(eTime) <= convertTimeToMinutes(sTime)) {
                Toast.makeText(this, "Giờ kết thúc phải sau giờ bắt đầu!", Toast.LENGTH_SHORT).show();
                return;
            }

            // ⭐ Gọi hàm kiểm tra trùng
            checkDuplicateTime(trip.getTripId(), date, sTime, eTime, conflictName -> {
                if (conflictName != null) {
                    Toast.makeText(this, "Bị trùng giờ với: " + conflictName, Toast.LENGTH_LONG).show();
                } else {
                    saveToSchedule(trip.getTripId(), selectedPlace, date, sTime, eTime, note, dialog);
                }
            });
        });

        dialog.show();
    }

    // ⭐ Hàm kiểm tra trùng giờ
    private void checkDuplicateTime(String tripId, String date, String newStart, String newEnd, OnCheckCallback callback) {
        int newS = convertTimeToMinutes(newStart);
        int newE = convertTimeToMinutes(newEnd);

        db.collection("schedule")
                .whereEqualTo("tripId", tripId)
                .whereEqualTo("visitDate", date)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String conflictName = null;

                    for (DocumentSnapshot doc : querySnapshot) {
                        String existStart = doc.getString("visitTime");
                        String existEnd = doc.getString("endTime");

                        if (existStart == null || existEnd == null) continue;

                        int oldS = convertTimeToMinutes(existStart);
                        int oldE = convertTimeToMinutes(existEnd);

                        // Logic: StartMới < EndCũ && StartCũ < EndMới
                        if (newS < oldE && newE > oldS) {
                            // Lấy tên địa điểm bị trùng
                            ScheduleItemModel item = doc.toObject(ScheduleItemModel.class);
                            if (item != null && item.getPlaceName() != null) {
                                conflictName = item.getPlaceName();
                            } else {
                                conflictName = "Một địa điểm khác";
                            }
                            break;
                        }
                    }
                    callback.onResult(conflictName);
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    private void saveToSchedule(String tripId, PlaceModel place, String date, String start, String end, String note, AlertDialog dialog) {
        String itemId = db.collection("schedule").document().getId();

        ScheduleItemModel item = new ScheduleItemModel(itemId, tripId, place, date, start, end);
        item.setNote(note);

        db.collection("schedule").document(itemId).set(item)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Đã thêm vào lịch trình!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showTimePicker(EditText et) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            et.setText(String.format("%02d:%02d", hourOfDay, minute));
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private int convertTimeToMinutes(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    private void loadData() {
        if (uid == null) return;
        db.collection("users").document(uid).collection("wishlist")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    wishlist.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        String address = doc.getString("address");
                        double rating = doc.getDouble("rating") != null ? doc.getDouble("rating") : 0.0;
                        double lat = doc.getDouble("lat") != null ? doc.getDouble("lat") : 0.0;
                        double lon = doc.getDouble("lon") != null ? doc.getDouble("lon") : 0.0;

                        PlaceModel p = new PlaceModel(name, address, rating, lat, lon);
                        p.setFavorite(true);
                        p.setId(doc.getId());
                        wishlist.add(p);
                    }
                    adapter.notifyDataSetChanged();
                    checkEmptyState();
                });
    }

    private void removeFromWishlist(PlaceModel place) {
        String docId = place.getId();
        if (docId == null) docId = place.getName().replaceAll("[^a-zA-Z0-9]", "_");

        db.collection("users").document(uid).collection("wishlist").document(docId)
                .delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Đã xóa khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show();
                    wishlist.remove(place);
                    adapter.notifyDataSetChanged();
                    checkEmptyState();
                });
    }

    private void checkEmptyState() {
        if (wishlist.isEmpty()) {
            rvWishlist.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvItemCount.setText("0 địa điểm");
        } else {
            rvWishlist.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            tvItemCount.setText(wishlist.size() + " địa điểm đã lưu");
        }
    }

    private void setupBottomNav() {
        navHome = findViewById(R.id.navHome);
        navBookmark = findViewById(R.id.navBookmark);
        navCalendar = findViewById(R.id.navCalendar);
        navNotification = findViewById(R.id.navNotification);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(WishlistActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        navCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(WishlistActivity.this, MyTripsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });
        navNotification.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        });
    }
}