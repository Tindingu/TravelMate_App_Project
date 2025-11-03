package com.example.travelmate_app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.model.GradientColor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


import android.graphics.Color;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvJoinDate, tvMemberType;
    private ImageView ivAvatar, ivBack;
    private Button btnLogout;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile); // ✅ Sử dụng đúng layout hồ sơ

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 🔹 Ánh xạ view
        tvProfileName = findViewById(R.id.tvProfileName);
        tvJoinDate = findViewById(R.id.tvJoinDate);
        tvMemberType = findViewById(R.id.tvMemberType);
        ivAvatar = findViewById(R.id.ivAvatar);
        ivBack = findViewById(R.id.ivBack);
        btnLogout = findViewById(R.id.btnLogout);

//         🔹 Khởi tạo Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔹 Kiểm tra user đăng nhập
        FirebaseUser user = auth.getCurrentUser();
        android.util.Log.d("DEBUG_PROFILE", "User = " + (user == null ? "null" : user.getEmail()));

        if (user == null) {
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // 🔹 Lấy dữ liệu người dùng từ Firestore
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        tvProfileName.setText(name != null ? name : "User");
                    }
                });

        // 🔹 Nút Back
        ivBack.setOnClickListener(v -> finish());

//         🔹 Logout có xác nhận
        btnLogout.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    auth.signOut();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show());
        // 🔹 Biểu đồ thống kê
        // 🔹 Biểu đồ thống kê
        BarChart barChart = findViewById(R.id.barChartProfileStats);

// 🔸 Dữ liệu thống kê mẫu: Likes, Comments, Favorites, Trips
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 120)); // Lượt like
        entries.add(new BarEntry(1, 45));  // Lượt comment
        entries.add(new BarEntry(2, 60));  // Lượt yêu thích
        entries.add(new BarEntry(3, 20));  // Số chuyến đi

// 🔹 Tạo dataset với màu gradient
        BarDataSet dataSet = new BarDataSet(entries, "Thống kê người dùng");

        ArrayList<GradientColor> gradientColors = new ArrayList<>();
// Gradient từ xanh dương → xanh ngọc (hiện đại)
        gradientColors.add(new GradientColor(Color.parseColor("#FFA726"), Color.parseColor("#FB8C00"))); // Likes
        gradientColors.add(new GradientColor(Color.parseColor("#FFA726"), Color.parseColor("#FB8C00"))); // Comments
        gradientColors.add(new GradientColor(Color.parseColor("#FFA726"), Color.parseColor("#FB8C00"))); // Favorites
        gradientColors.add(new GradientColor(Color.parseColor("#FFA726"), Color.parseColor("#FB8C00"))); // Trips
        dataSet.setGradientColors(gradientColors);

// 🔹 Tùy chỉnh chữ và hiệu ứng
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.parseColor("#333333"));
        dataSet.setBarShadowColor(Color.TRANSPARENT);
        dataSet.setHighLightAlpha(0);

// 🔹 Tạo dữ liệu cho biểu đồ
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        barChart.setData(barData);

// 🔹 Cấu hình trục X
        String[] labels = {"Likes", "Comments", "Favorites", "Trips"};
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.parseColor("#555555"));
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(labels.length);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-10f); // nghiêng nhẹ cho cân đối

// 🔹 Cấu hình trục Y
        barChart.getAxisLeft().setTextColor(Color.parseColor("#777777"));
        barChart.getAxisLeft().setGridColor(Color.parseColor("#E0E0E0"));
        barChart.getAxisRight().setEnabled(false);

// 🔹 Tắt mô tả và legend
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);

// 🔹 Làm đẹp bố cục
        barChart.setDrawGridBackground(false);
        barChart.setDrawBorders(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setExtraOffsets(5, 10, 5, 10);

// 🔹 Hiệu ứng animation
        barChart.animateY(1200, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);
        barChart.invalidate();


    }
}
