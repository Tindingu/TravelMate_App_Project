package com.example.testproject1;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.model.GradientColor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    // 🔹 View
    private TextView tvProfileName, tvJoinDate, tvMemberType, layoutProfile;
    private ImageView ivAvatar, ivBack;
    private Button btnLogout;
    private BarChart barChart;

    // 🔹 Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    // Activity Result Launcher for ProfileUpdateActivity
    private ActivityResultLauncher<Intent> profileUpdateActivityLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        setupWindowInsets();
        initFirebase();
        initViews();
        checkLoginStatus();
        initProfileUpdateActivityLauncher();
        loadUserData();
        setupLogoutButton();
        setupBackButton();
        setupProfileButton();
        setupUserChart();
    }

    // ============================================================
    // 🔹 1. Cấu hình khoảng cách giao diện với thanh trạng thái
    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // ============================================================
    // 🔹 2. Khởi tạo Firebase
    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();
    }

    // ============================================================
    // 🔹 3. Ánh xạ view
    private void initViews() {
        tvProfileName = findViewById(R.id.tvProfileName);
        tvJoinDate = findViewById(R.id.tvJoinDate);
        tvMemberType = findViewById(R.id.tvMemberType);
        layoutProfile = findViewById(R.id.layoutProfile);
        ivAvatar = findViewById(R.id.ivAvatar);
        ivBack = findViewById(R.id.ivBack);
        btnLogout = findViewById(R.id.btnLogout);
        barChart = findViewById(R.id.barChartProfileStats);
    }

    // ============================================================
    // 🔹 4. Kiểm tra người dùng đăng nhập
    private void checkLoginStatus() {
        if (user == null) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        }
    }

    // ============================================================
    // 🔹 4. Khởi tạo Activity Result Launcher cho ProfileUpdate
    private void initProfileUpdateActivityLauncher() {
        profileUpdateActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Khi quay lại từ ProfileUpdateActivity, load lại user data và avatar
                // để cập nhật những thay đổi có thể có (bao gồm avatar mới)
                loadUserData();
                // Set result OK để HomeActivity cũng cập nhật
                setResult(RESULT_OK);
            }
        );
    }

    // ============================================================
    // 🔹 5. Lấy dữ liệu người dùng từ Firestore
    private void loadUserData() {
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // 🔸 Hiển thị tên
                        String name = document.getString("name");
                        tvProfileName.setText(name != null ? name : "User");

                        // 🔸 Load avatar từ photoUrl
                        String photoUrl = document.getString("photoUrl");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            com.bumptech.glide.Glide.with(this)
                                    .load(photoUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.sample_avatar)
                                    .error(R.drawable.sample_avatar)
                                    .into(ivAvatar);
                        } else {
                            // Load default avatar
                            com.bumptech.glide.Glide.with(this)
                                    .load(R.drawable.sample_avatar)
                                    .circleCrop()
                                    .into(ivAvatar);
                        }

                        // 🔸 Hiển thị ngày tham gia
                        if (user.getMetadata() != null) {
                            long creationTime = user.getMetadata().getCreationTimestamp();
                            Date date = new Date(creationTime);
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                            tvJoinDate.setText("Tham gia ngày " + sdf.format(date));
                        } else {
                            tvJoinDate.setText("Tham gia gần đây");
                        }

                        // 🔸 Hiển thị loại thành viên
                        tvMemberType.setText("Member Gold");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PROFILE_FIRESTORE", "Lỗi đọc dữ liệu Firestore", e);
                    tvJoinDate.setText("Không thể tải thời gian tham gia");
                });
    }

    // ============================================================
    // 🔹 6. Nút logout với xác nhận
    private void setupLogoutButton() {
        btnLogout.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Xác nhận đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    auth.signOut();
                    Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show());
    }

    // ============================================================
    // 🔹 7. Nút quay lại
    private void setupBackButton() {
        ivBack.setOnClickListener(v -> finish());
    }

    // ============================================================
    // 🔹 8. Nút Hồ sơ - chuyển sang ProfileUpdate
    private void setupProfileButton() {
        layoutProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ProfileUpdateActivity.class);
            profileUpdateActivityLauncher.launch(intent);
        });
    }

    // ============================================================
    // 🔹 9. Biểu đồ thống kê người dùng
    private void setupUserChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 120)); // Likes
        entries.add(new BarEntry(1, 45));  // Comments
        entries.add(new BarEntry(2, 60));  // Favorites
        entries.add(new BarEntry(3, 20));  // Trips

        BarDataSet dataSet = new BarDataSet(entries, "Thống kê người dùng");

        ArrayList<GradientColor> gradientColors = new ArrayList<>();
        gradientColors.add(new GradientColor(Color.parseColor("#FFA726"), Color.parseColor("#FB8C00")));
        gradientColors.add(new GradientColor(Color.parseColor("#FFA726"), Color.parseColor("#FB8C00")));
        gradientColors.add(new GradientColor(Color.parseColor("#FFA726"), Color.parseColor("#FB8C00")));
        gradientColors.add(new GradientColor(Color.parseColor("#FFA726"), Color.parseColor("#FB8C00")));
        dataSet.setGradientColors(gradientColors);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.parseColor("#333333"));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        barChart.setData(barData);

        String[] labels = {"Likes", "Comments", "Favorites", "Trips"};
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.parseColor("#555555"));
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(labels.length);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-10f);

        barChart.getAxisLeft().setTextColor(Color.parseColor("#777777"));
        barChart.getAxisLeft().setGridColor(Color.parseColor("#E0E0E0"));
        barChart.getAxisRight().setEnabled(false);

        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);

        barChart.setDrawGridBackground(false);
        barChart.setDrawBorders(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setExtraOffsets(5, 10, 5, 10);

        barChart.animateY(1200, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);
        barChart.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload user data when activity resumes (in case user came back from ProfileUpdateActivity with avatar changes)
        loadUserData();
    }
}
