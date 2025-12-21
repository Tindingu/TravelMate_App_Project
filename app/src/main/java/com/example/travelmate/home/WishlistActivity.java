package com.example.travelmate.home;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
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

import com.example.travelmate.R;
import com.example.travelmate.chat.ChatListActivity;
import com.example.travelmate.notifications.NotificationsActivity;
import com.example.travelmate.trips.MyTripsActivity;
import com.example.travelmate.trips.ScheduleItemModel;
import com.example.travelmate.trips.TripAlarmReceiver;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.travelmate.trips.TripModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WishlistActivity extends AppCompatActivity {

    // ================= UI =================
    RecyclerView rvWishlist;
    LinearLayout navHome, navBookmark, navCalendar, navNotification,navChat;
    LinearLayout layoutEmpty;
    TextView tvItemCount;

    // ================= DATA =================
    PlaceAdapter adapter;
    ArrayList<PlaceModel> wishlist = new ArrayList<>();
    List<TripModel> myTrips = new ArrayList<>();

    // ================= FIREBASE =================
    FirebaseFirestore db;
    String uid;

    interface OnCheckCallback {
        void onResult(@Nullable String conflictName);
    }

    // ================= LIFECYCLE =================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        rvWishlist = findViewById(R.id.rvWishlist);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvItemCount = findViewById(R.id.tvItemCount);

        rvWishlist.setLayoutManager(new LinearLayoutManager(this));
        navHome = findViewById(R.id.navHome);
        navBookmark = findViewById(R.id.navBookmark);
        navChat = findViewById(R.id.navChat);
        navCalendar = findViewById(R.id.navCalendar);
        navNotification = findViewById(R.id.navNotification);
        db = FirebaseFirestore.getInstance();
        adapter = new PlaceAdapter(
                wishlist,
                place -> openPlaceDetail(place),
                place -> removeFromWishlist(place),
                place -> {},
                place -> showAddToTripDialog(place)
        );
        rvWishlist.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadData(); // ✅ GỌI SAU KHI ADAPTER SẴN SÀNG
        }

//        rvWishlist.setAdapter(adapter);
        setupBottomNav();

        // Android 13+ notification permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (uid != null) {
            loadData(); // 🔥 gắn lại listener mỗi lần quay lại
        }
    }


    // ================= PLACE DETAIL =================
    private void openPlaceDetail(PlaceModel place) {
        Intent intent = new Intent(this, PlaceDetailActivity.class);
        intent.putExtra("id", place.getId());
        intent.putExtra("name", place.getName());
        intent.putExtra("address", place.getAddress());
        intent.putExtra("rating", place.getRating());
        intent.putExtra("lat", place.getLat());
        intent.putExtra("lon", place.getLon());
        startActivity(intent);
    }

    // ================= ADD TO TRIP =================
    private void showAddToTripDialog(PlaceModel selectedPlace) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(LayoutInflater.from(this).inflate(R.layout.dialog_add_to_trip, null))
                .create();
        dialog.show();

        View view = dialog.getWindow().getDecorView();
        Spinner spTrip = view.findViewById(R.id.spTrip);
        EditText etDate = view.findViewById(R.id.etVisitDate);
        EditText etStartTime = view.findViewById(R.id.etStartTime);
        EditText etEndTime = view.findViewById(R.id.etEndTime);
        EditText etNote = view.findViewById(R.id.etNote);
        Button btnConfirm = view.findViewById(R.id.btnConfirmAdd);


        List<String> tripNames = new ArrayList<>();
        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tripNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTrip.setAdapter(spinnerAdapter);

        db.collection("trips")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(qs -> {
                    myTrips.clear();
                    tripNames.clear();
                    for (DocumentSnapshot doc : qs) {
                        TripModel trip = doc.toObject(TripModel.class);
                        if (trip == null) continue;

                        // 🔥 FIX CRASH
                        trip.setTripId(doc.getId());

                        myTrips.add(trip);
                        tripNames.add(trip.getName());
                    }
                    spinnerAdapter.notifyDataSetChanged();

                    if (myTrips.isEmpty()) {
                        Toast.makeText(this, "Bạn chưa có chuyến đi nào!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });

        etDate.setOnClickListener(v -> showDatePicker(etDate));
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));

        btnConfirm.setOnClickListener(v -> {
            int pos = spTrip.getSelectedItemPosition();
            if (pos < 0) return;

            TripModel trip = myTrips.get(pos);
            if (trip.getTripId() == null) {
                Toast.makeText(this, "Lỗi tripId null", Toast.LENGTH_SHORT).show();
                return;
            }

            String date = etDate.getText().toString();
            String sTime = etStartTime.getText().toString();
            String eTime = etEndTime.getText().toString();
            String note = etNote.getText().toString();

            Log.d("DEBUG_TRIP", "TripId=" + trip.getTripId());

            if (date.isEmpty() || sTime.isEmpty() || eTime.isEmpty()) {
                Toast.makeText(this, "Nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (convertTimeToMinutes(eTime) <= convertTimeToMinutes(sTime)) {
                Toast.makeText(this, "Giờ kết thúc phải sau giờ bắt đầu", Toast.LENGTH_SHORT).show();
                return;
            }

            checkDuplicateTime(trip.getTripId(), date, sTime, eTime, conflict -> {
                if (conflict != null) {
                    Toast.makeText(this, "Trùng giờ với: " + conflict, Toast.LENGTH_LONG).show();
                } else {
                    saveToSchedule(
                            trip.getTripId(),
                            trip,                // 👈 truyền trip vào
                            selectedPlace,
                            date,
                            sTime,
                            eTime,
                            note,
                            dialog
                    );

                }
            });
        });
    }

    // ================= CHECK DUPLICATE =================
    private void checkDuplicateTime(String tripId, String date, String newStart, String newEnd, OnCheckCallback cb) {
        int newS = convertTimeToMinutes(newStart);
        int newE = convertTimeToMinutes(newEnd);

        db.collection("schedule")
                .whereEqualTo("tripId", tripId)
                .whereEqualTo("visitDate", date)
                .get()
                .addOnSuccessListener(qs -> {
                    for (DocumentSnapshot doc : qs) {
                        int oldS = convertTimeToMinutes(doc.getString("visitTime"));
                        int oldE = convertTimeToMinutes(doc.getString("endTime"));
                        if (newS < oldE && newE > oldS) {
                            ScheduleItemModel item = doc.toObject(ScheduleItemModel.class);
                            cb.onResult(item != null ? item.getPlaceName() : "địa điểm khác");
                            return;
                        }
                    }
                    cb.onResult(null);
                });
    }

    // ================= SAVE & NOTIFY =================
//    private void saveToSchedule(String tripId, PlaceModel place, String date,
//                                String start, String end, String note, AlertDialog dialog) {
//
//        String itemId = db.collection("schedule").document().getId();
//        ScheduleItemModel item =
//                new ScheduleItemModel(itemId, tripId, place, date, start, end);
//        item.setNote(note);
//
//        db.collection("schedule").document(itemId).set(item)
//                .addOnSuccessListener(a -> {
//                    long millis = convertToMillis(date, start);
//                    Log.d("DEBUG_ALARM", "StartMillis=" + millis);
//                    scheduleTripNotification(millis, place.getName());
//                    Toast.makeText(this, "Đã thêm vào lịch trình!", Toast.LENGTH_SHORT).show();
//                    dialog.dismiss();
//                });
//    }
    private void saveToSchedule(
            String tripId,
            TripModel trip,          // 👈 thêm trip vào param
            PlaceModel place,
            String date,
            String start,
            String end,
            String note,
            AlertDialog dialog
    ) {

        String itemId = db.collection("schedule").document().getId();

        ScheduleItemModel item =
                new ScheduleItemModel(itemId, tripId, place, date, start, end);
        item.setNote(note);

        db.collection("schedule")
                .document(itemId)
                .set(item)
                .addOnSuccessListener(a -> {

                    long millis = convertToMillis(date, start);
                    Log.d("DEBUG_ALARM", "StartMillis=" + millis);

                    String tripName = trip.getName(); // ✅ ĐÚNG
                    String userId   = FirebaseAuth.getInstance().getCurrentUser().getUid();
//                    String groupId  = null;           // ✅ wishlist = không group

                    db.collection("chat_groups")
                            .whereEqualTo("tripId", tripId)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(snapshot -> {

                                String groupId = null;
                                if (!snapshot.isEmpty()) {
                                    groupId = snapshot.getDocuments().get(0).getId();
                                }

                                scheduleTripNotification(
                                        millis,
                                        place.getName(),
                                        tripId,
                                        tripName,
                                        userId,
                                        groupId,
                                         place.getName(),
                                         place.getAddress(),
                                         date,
                                         start,
                                         end
                                        // ✅ CÓ groupId THẬT
                                );
                            });


                    Toast.makeText(this, "Đã thêm vào lịch trình!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
    }




    private void scheduleTripNotification(
            long startTimeMillis,
            String placeName,
            String tripId,
            String tripName,
            String userId,
            String groupId,
             String  namePlace,
            String addressPlace ,
            String date,
           String start,
            String end
    ) {
        try {

            Log.d("DEBUG_NOTIFY", "Schedule for: " + placeName);
            Log.d("DEBUG_NOTIFY", "Start millis = " + startTimeMillis);
            Log.d("DEBUG_NOTIFY", "Now millis   = " + System.currentTimeMillis());

            if (startTimeMillis <= System.currentTimeMillis()) {
                Log.e("DEBUG_NOTIFY", "Start time is in the past!");
                return;
            }

            Intent intent = new Intent(this, TripAlarmReceiver.class);

            // ✅ CODE CŨ
            intent.putExtra("placeName", placeName);

            // 🔥 BỔ SUNG – QUAN TRỌNG
            intent.putExtra("tripId", tripId);
            intent.putExtra("tripName", tripName);
            intent.putExtra("userId", userId);
            intent.putExtra("groupId", groupId); // null cũng OK
            intent.putExtra("namePlace",namePlace);
            intent.putExtra("addessPlace",addressPlace);
            intent.putExtra("date",date);
            intent.putExtra("start",start);
            intent.putExtra("end",end);
            // ⚠️ requestCode KHÔNG được để 0 (sẽ bị đè alarm)
            int requestCode = (tripId + "_" + startTimeMillis).hashCode();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager =
                    (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            if (alarmManager != null) {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        startTimeMillis,
                        pendingIntent
                );
            }

            Log.d("DEBUG_NOTIFY", "Alarm scheduled OK");

        } catch (Exception e) {
            Log.e("DEBUG_NOTIFY", "CRASH in scheduleTripNotification", e);
        }
    }



    // ================= UTIL =================
    private void showDatePicker(EditText et) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this,
                (v, y, m, d) -> et.setText(d + "/" + (m + 1) + "/" + y),
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showTimePicker(EditText et) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this,
                (v, h, m) -> et.setText(String.format("%02d:%02d", h, m)),
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                true
        ).show();
    }

    private int convertTimeToMinutes(String t) {
        if (t == null) return 0;
        String[] p = t.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    private long convertToMillis(String date, String time) {
        String[] d = date.split("/");
        String[] t = time.split(":");
        Calendar c = Calendar.getInstance();
        c.set(
                Integer.parseInt(d[2]),
                Integer.parseInt(d[1]) - 1,
                Integer.parseInt(d[0]),
                Integer.parseInt(t[0]),
                Integer.parseInt(t[1]),
                0
        );
        return c.getTimeInMillis();
    }

    // ================= DATA =================
    private void loadData() {
        db.collection("users")
                .document(uid)
                .collection("wishlist")
                .addSnapshotListener(this, (qs, e) -> {
                    if (e != null || qs == null) return;

                    wishlist.clear();

                    for (DocumentSnapshot doc : qs) {
                        PlaceModel p = doc.toObject(PlaceModel.class);
                        if (p != null) {
                            p.setId(doc.getString("id")); // ← placeId THẬT

                            p.setFavorite(true);
                            wishlist.add(p);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    checkEmptyState();
                });
    }



    private void removeFromWishlist(PlaceModel place) {
        db.collection("users").document(uid)
                .collection("wishlist")
                .document(place.getId())
                .delete()
                .addOnSuccessListener(v -> {
                    wishlist.remove(place);
                    adapter.notifyDataSetChanged();
                    checkEmptyState();
                });
    }

    private void checkEmptyState() {
        tvItemCount.setText(wishlist.size() + " địa điểm");
        layoutEmpty.setVisibility(wishlist.isEmpty() ? View.VISIBLE : View.GONE);
        rvWishlist.setVisibility(wishlist.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ================= NAV =================
    private void setupBottomNav() {
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(WishlistActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(WishlistActivity.this, MyTripsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navNotification.setOnClickListener(v ->
                {
                    Intent intent = new Intent(WishlistActivity.this, NotificationsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                }
        );
        navChat.setOnClickListener(v ->
                {
                    Intent intent = new Intent(WishlistActivity.this, ChatListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                }
        );

    }

}
