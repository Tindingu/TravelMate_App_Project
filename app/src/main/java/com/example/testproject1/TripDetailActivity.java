package com.example.testproject1;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TripDetailActivity extends AppCompatActivity {

    RecyclerView rvSchedule;
    ScheduleAdapter adapter;
    List<ScheduleItemModel> list = new ArrayList<>();
    FirebaseFirestore db;
    TextView tvTitle;
    ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        db = FirebaseFirestore.getInstance();

        tvTitle = findViewById(R.id.tvTitle);
        rvSchedule = findViewById(R.id.rvSchedule);
        ivBack = findViewById(R.id.ivBack);

        TripModel trip = (TripModel) getIntent().getSerializableExtra("trip_data");

        if (trip != null) {
            tvTitle.setText(trip.getName());
            loadData(trip.getTripId());
        } else {
            tvTitle.setText("Chi tiết lịch trình");
        }

        setupRecycler();

        ivBack.setOnClickListener(v -> finish());
    }

    private void setupRecycler() {
        adapter = new ScheduleAdapter(list,
                item -> {
                    Intent i = new Intent(this, PlaceDetailActivity.class);
                    i.putExtra("id", item.getPlaceId());
                    i.putExtra("name", item.getPlaceName());
                    i.putExtra("lat", item.getLat());
                    i.putExtra("lon", item.getLon());
                    i.putExtra("address", item.getPlaceAddress());
                    startActivity(i);
                },
                new ScheduleAdapter.OnActionListener() {
                    @Override
                    public void onDelete(ScheduleItemModel item, int position) {
                        showDeleteConfirmDialog(item, position);
                    }

                    @Override
                    public void onEdit(ScheduleItemModel item, int position) {
                        showEditDialog(item, position);
                    }
                }
        );

        rvSchedule.setLayoutManager(new LinearLayoutManager(this));
        rvSchedule.setAdapter(adapter);
    }

    // ============================================================
    // LOGIC SỬA (EDIT)
    // ============================================================
    private void showEditDialog(ScheduleItemModel item, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_to_trip, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        // Ẩn các phần không cần thiết
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        if (tvTitle != null) tvTitle.setText("Sửa thông tin");

        TextView tvLabel = view.findViewById(R.id.tvLabelTrip);
        if (tvLabel != null) tvLabel.setVisibility(View.GONE);

        View spinnerContainer = view.findViewById(R.id.layoutTripSpinner);
        if (spinnerContainer != null) spinnerContainer.setVisibility(View.GONE);

        EditText etDate = view.findViewById(R.id.etVisitDate);
        EditText etStartTime = view.findViewById(R.id.etStartTime);
        EditText etEndTime = view.findViewById(R.id.etEndTime);
        EditText etNote = view.findViewById(R.id.etNote);
        Button btnConfirm = view.findViewById(R.id.btnConfirmAdd);

        // 1. Điền dữ liệu cũ
        etDate.setText(item.getVisitDate());
        etStartTime.setText(item.getVisitTime());
        etEndTime.setText(item.getEndTime());
        etNote.setText(item.getNote());
        btnConfirm.setText("Cập nhật");

        // 2. Chọn Ngày/Giờ
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                String dateStr = dayOfMonth + "/" + (month + 1) + "/" + year;
                etDate.setText(dateStr);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));

        // 3. Bấm Lưu -> Gọi hàm checkConflictAndUpdate
        btnConfirm.setOnClickListener(v -> {
            String newDate = etDate.getText().toString();
            String newStart = etStartTime.getText().toString();
            String newEnd = etEndTime.getText().toString();
            String newNote = etNote.getText().toString();

            if (newDate.isEmpty() || newStart.isEmpty() || newEnd.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (convertTimeToMinutes(newEnd) <= convertTimeToMinutes(newStart)) {
                Toast.makeText(this, "Giờ kết thúc phải sau giờ bắt đầu!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi hàm kiểm tra và cập nhật (tương tự như code bạn muốn)
            checkConflictAndUpdate(item, position, newDate, newStart, newEnd, newNote, dialog);
        });

        dialog.show();
    }

    // ⭐ HÀM BẠN MUỐN: Kiểm tra trùng -> Toast tên -> Cập nhật
    private void checkConflictAndUpdate(ScheduleItemModel currentItem, int position, String newDate, String newStart, String newEnd, String newNote, AlertDialog dialog) {
        db.collection("schedule")
                .whereEqualTo("tripId", currentItem.getTripId())
                .whereEqualTo("visitDate", newDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean isConflict = false;
                    int newS = convertTimeToMinutes(newStart);
                    int newE = convertTimeToMinutes(newEnd);

                    for (DocumentSnapshot doc : querySnapshot) {
                        // QUAN TRỌNG: Bỏ qua chính item đang sửa (so sánh ID)
                        if (doc.getId().equals(currentItem.getItemId())) {
                            continue;
                        }

                        String existStart = doc.getString("visitTime");
                        String existEnd = doc.getString("endTime");

                        if (existStart == null || existEnd == null) continue;

                        int oldS = convertTimeToMinutes(existStart);
                        int oldE = convertTimeToMinutes(existEnd);

                        // Logic trùng giờ
                        if (newS < oldE && newE > oldS) {
                            isConflict = true;

                            // Lấy tên địa điểm bị trùng
                            // Vì dữ liệu Place nằm trong object 'place', ta phải lấy object ra trước
                            ScheduleItemModel existItem = doc.toObject(ScheduleItemModel.class);
                            String conflictName = "Địa điểm khác";
                            if (existItem != null && existItem.getPlaceName() != null) {
                                conflictName = existItem.getPlaceName();
                            }

                            // Hiển thị thông báo
                            Toast.makeText(this, "Bị trùng giờ với: " + conflictName, Toast.LENGTH_LONG).show();
                            break; // Dừng vòng lặp
                        }
                    }

                    // Nếu không trùng thì mới cho Update
                    if (!isConflict) {
                        performUpdate(currentItem, position, newDate, newStart, newEnd, newNote, dialog);
                    }
                });
    }

    // Hàm thực hiện Update xuống Firebase (Tách ra cho gọn)
    private void performUpdate(ScheduleItemModel item, int position, String date, String start, String end, String note, AlertDialog dialog) {
        db.collection("schedule").document(item.getItemId())
                .update(
                        "visitDate", date,
                        "visitTime", start,
                        "endTime", end,
                        "note", note
                )
                .addOnSuccessListener(aVoid -> {
                    // Cập nhật Local List
                    item.setVisitDate(date);
                    item.setVisitTime(start);
                    item.setEndTime(end);
                    item.setNote(note);

                    adapter.notifyItemChanged(position);
                    dialog.dismiss();
                    Toast.makeText(this, "Đã cập nhật thành công", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ============================================================
    // CÁC HÀM TIỆN ÍCH
    // ============================================================
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

    private void loadData(String tripId) {
        db.collection("schedule")
                .whereEqualTo("tripId", tripId)
                .orderBy("visitDate", Query.Direction.ASCENDING)
                .orderBy("visitTime", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    list.clear();
                    for (DocumentSnapshot doc : snap) {
                        ScheduleItemModel item = doc.toObject(ScheduleItemModel.class);
                        item.setItemId(doc.getId());
                        list.add(item);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showDeleteConfirmDialog(ScheduleItemModel item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa địa điểm")
                .setMessage("Bạn có chắc muốn xóa '" + item.getPlaceName() + "' khỏi lịch trình?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteScheduleItem(item, position))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteScheduleItem(ScheduleItemModel item, int position) {
        if (item.getItemId() == null) return;
        db.collection("schedule").document(item.getItemId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    list.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Đã xóa thành công", Toast.LENGTH_SHORT).show();
                });
    }
}