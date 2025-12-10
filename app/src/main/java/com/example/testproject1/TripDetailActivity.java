package com.example.testproject1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog; // Import Dialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
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

        // Ánh xạ View
        tvTitle = findViewById(R.id.tvTitle);
        rvSchedule = findViewById(R.id.rvSchedule);
        ivBack = findViewById(R.id.ivBack);

        // Lấy dữ liệu chuyến đi được truyền sang
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
                // 1. Xem chi tiết
                item -> {
                    Intent i = new Intent(this, PlaceDetailActivity.class);
                    i.putExtra("id", item.getPlaceId());
                    i.putExtra("name", item.getPlaceName());
                    i.putExtra("lat", item.getLat());
                    i.putExtra("lon", item.getLon());
                    i.putExtra("address", item.getPlaceAddress());
                    startActivity(i);
                },
                // 2. Hành động (Xóa & Sửa)
                new ScheduleAdapter.OnActionListener() {
                    @Override
                    public void onDelete(ScheduleItemModel item, int position) {
                        showDeleteConfirmDialog(item, position);
                    }

                    @Override
                    public void onEdit(ScheduleItemModel item, int position) {
                        showEditDialog(item, position); // MỚI: Gọi hàm sửa
                    }
                }
        );

        rvSchedule.setLayoutManager(new LinearLayoutManager(this));
        rvSchedule.setAdapter(adapter);
    }
    private void showEditDialog(ScheduleItemModel item, int position) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_to_trip, null);
        builder.setView(view);
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // Ánh xạ
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        if (tvTitle != null) tvTitle.setText("Sửa thông tin");

        TextView tvLabel = view.findViewById(R.id.tvLabelTrip);
        if (tvLabel != null) tvLabel.setVisibility(View.GONE);

        View spinnerContainer = view.findViewById(R.id.spTrip);
        spinnerContainer.setVisibility(View.GONE); // Ẩn spinner

        EditText etDate = view.findViewById(R.id.etVisitDate);
        EditText etTime = view.findViewById(R.id.etVisitTime);
        EditText etNote = view.findViewById(R.id.etNote);
        android.widget.Button btnConfirm = view.findViewById(R.id.btnConfirmAdd);

        // 1. Điền dữ liệu cũ vào
        etDate.setText(item.getVisitDate());
        etTime.setText(item.getVisitTime());
        etNote.setText(item.getNote());
        btnConfirm.setText("Cập nhật");

        // 2. Xử lý chọn NGÀY (DatePicker) -> ĐÂY LÀ PHẦN BẠN BỊ THIẾU
        etDate.setOnClickListener(v -> {
            java.util.Calendar c = java.util.Calendar.getInstance();
            // Tách ngày cũ ra để hiển thị đúng trên lịch (nếu có)
            int y = c.get(java.util.Calendar.YEAR);
            int m = c.get(java.util.Calendar.MONTH);
            int d = c.get(java.util.Calendar.DAY_OF_MONTH);

            new android.app.DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                String dateStr = dayOfMonth + "/" + (month + 1) + "/" + year;
                etDate.setText(dateStr);
            }, y, m, d).show();
        });

        // 3. Xử lý chọn GIỜ (TimePicker) -> ĐÂY LÀ PHẦN BẠN BỊ THIẾU
        etTime.setOnClickListener(v -> {
            java.util.Calendar c = java.util.Calendar.getInstance();
            int h = c.get(java.util.Calendar.HOUR_OF_DAY);
            int mi = c.get(java.util.Calendar.MINUTE);

            new android.app.TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                String timeStr = String.format("%02d:%02d", hourOfDay, minute);
                etTime.setText(timeStr);
            }, h, mi, true).show();
        });

        // 4. Bấm Lưu
        btnConfirm.setOnClickListener(v -> {
            String newDate = etDate.getText().toString();
            String newTime = etTime.getText().toString();
            String newNote = etNote.getText().toString();

            if (newDate.isEmpty()) {
                Toast.makeText(this, "Ngày không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }

            updateScheduleItem(item, position, newDate, newTime, newNote, dialog);
        });

        dialog.show();
    }

    private void updateScheduleItem(ScheduleItemModel item, int position, String date, String time, String note, AlertDialog dialog) {
        // Cập nhật Firebase
        db.collection("schedule").document(item.getItemId())
                .update(
                        "visitDate", date,
                        "visitTime", time,
                        "note", note
                )
                .addOnSuccessListener(aVoid -> {
                    // Cập nhật thành công -> Cập nhật list local và Refresh Adapter
                    item.setVisitDate(date);
                    item.setVisitTime(time);
                    item.setNote(note);

                    adapter.notifyItemChanged(position);
                    dialog.dismiss();
                    Toast.makeText(this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    // ⭐ HÀM MỚI: Load dữ liệu và gán ID
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
                        // QUAN TRỌNG: Lưu ID của document để tí nữa còn xóa được
                        item.setItemId(doc.getId());
                        list.add(item);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ⭐ HÀM MỚI: Hiện hộp thoại xác nhận xóa
    private void showDeleteConfirmDialog(ScheduleItemModel item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa địa điểm")
                .setMessage("Bạn có chắc muốn xóa '" + item.getPlaceName() + "' khỏi lịch trình?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteScheduleItem(item, position))
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ⭐ HÀM MỚI: Xóa khỏi Firebase
    private void deleteScheduleItem(ScheduleItemModel item, int position) {
        if (item.getItemId() == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("schedule").document(item.getItemId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Xóa thành công trên Server thì xóa trên giao diện
                    list.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Đã xóa thành công", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}