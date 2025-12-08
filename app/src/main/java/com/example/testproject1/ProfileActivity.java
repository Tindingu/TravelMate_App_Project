package com.example.testproject1;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
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
    private TextView tvProfileName, tvJoinDate, tvMemberType;
    private ImageView ivAvatar, ivBack;
    private Button btnLogout;
    private BarChart barChart;

    // 🔹 Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private TextView  tvPoint;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        setupWindowInsets();
        initFirebase();
        initViews();
        checkLoginStatus();
        loadUserData();
        setupLogoutButton();
        setupBackButton();
        loadUserStats();
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
        ivAvatar = findViewById(R.id.ivAvatar);
        ivBack = findViewById(R.id.ivBack);
        btnLogout = findViewById(R.id.btnLogout);
        barChart = findViewById(R.id.barChartProfileStats);
        tvPoint=findViewById(R.id.tvPoints);
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

                        // 🔸 Hiển thị ngày tham gia
                        if (user.getMetadata() != null) {
                            long creationTime = user.getMetadata().getCreationTimestamp();
                            Date date = new Date(creationTime);
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                            tvJoinDate.setText("Tham gia ngày " + sdf.format(date));
                        } else {
                            tvJoinDate.setText("Tham gia gần đây");
                        }

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
    // 🔹 8. Biểu đồ thống kê người dùng
    private void setupUserChart(int totalLikes,int totalDislikes,int totalComments,int totalPlaces) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, totalLikes)); // Likes
        entries.add(new BarEntry(1, totalDislikes));  // Dislikes
        entries.add(new BarEntry(2, totalComments));  // Comments
        entries.add(new BarEntry(3, totalPlaces));  // totalPlaces

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

        String[] labels = {"Likes", "Dislikes", "Comments", "TotalPlaces"};
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
    private void loadUserStats() {
        String uid = auth.getUid();
        if (uid == null) {
            Log.e("USER_STATS", "UID NULL");
            return;
        }

        Log.d("USER_STATS", "Bắt đầu load thống kê cho UID: " + uid);

        db.collectionGroup("comments")
                .get()
                .addOnSuccessListener(snap -> {

                    Log.d("USER_STATS", "Tổng số documents tìm thấy: " + snap.size());

                    int totalLikes = 0;
                    int totalDislikes = 0;
                    int totalComments = 0;
                    ArrayList<String> places = new ArrayList<>();

                    for (var doc : snap.getDocuments()) {

                        Log.d("USER_STATS", "-----------------------------------");
                        Log.d("USER_STATS", "COMMENT DOC ID = " + doc.getId());

                        CommentModel c = doc.toObject(CommentModel.class);
                        if (c == null) {
                            Log.e("USER_STATS", "CommentModel NULL → SKIP");
                            continue;
                        }

                        String commentUid = doc.getString("uid");
                        Log.d("USER_STATS", "UID trong comment = " + commentUid);

                        // Chỉ tính comment của user này
                        if (!uid.equals(commentUid)) {
                            Log.d("USER_STATS", "UID không trùng, bỏ qua");
                            continue;
                        }

                        Log.d("USER_STATS", "UID khớp → tính thống kê");

                        totalComments++;

                        Long likeCount = doc.getLong("likeCount");
                        Long dislikeCount = doc.getLong("dislikeCount");

                        Log.d("USER_STATS", "likeCount = " + likeCount);
                        Log.d("USER_STATS", "dislikeCount = " + dislikeCount);

                        totalLikes += (likeCount != null ? likeCount : 0);
                        totalDislikes += (dislikeCount != null ? dislikeCount : 0);

                        // Lấy ID của Place chứa comment
                        String placeId = doc.getReference().getParent().getParent().getId();
                        Log.d("USER_STATS", "Place chứa comment: " + placeId);

                        if (!places.contains(placeId)) {
                            places.add(placeId);
                            Log.d("USER_STATS", "➕ Thêm Place vào danh sách");
                        }
                    }

                    int totalPlaces = places.size();
                    int totalPoints = (totalLikes * 5)
                            + (totalComments * 2)
                            - (totalDislikes * 3)
                            + (totalPlaces * 1);

                    updateUserTier(totalPoints);

                    Log.d("USER_STATS", "============ KẾT QUẢ ============");
                    Log.d("USER_STATS", "totalLikes     = " + totalLikes);
                    Log.d("USER_STATS", "totalDislikes  = " + totalDislikes);
                    Log.d("USER_STATS", "totalComments  = " + totalComments);
                    Log.d("USER_STATS", "totalPlaces    = " + totalPlaces);
                    Log.d("USER_STATS", "=================================");

                    setupUserChart(totalLikes, totalDislikes, totalComments, totalPlaces);
                })
                .addOnFailureListener(e -> {
                    Log.e("USER_STATS", "LỖI FIRESTORE: " + e.getMessage());
                });
    }
    private void updateUserTier(int totalPoints) {

        TextView tvMemberType = findViewById(R.id.tvMemberType);
        TextView tvPoint = findViewById(R.id.tvPoints);
        SeekBar progressTier = findViewById(R.id.seekLevelProgress);
        TextView tvNextTier = findViewById(R.id.tvNextTier);
        TextView tvProgressPercent = findViewById(R.id.tvProgressPercent);

        // ----- Cập nhật điểm hiển thị -----
        tvPoint.setText(String.valueOf(totalPoints));

        // ----- Tính % tiến trình (max 300 là Gold) -----
        int capped = Math.min(totalPoints, 300);
        int percent = (int) ((capped / 300f) * 100);

        progressTier.setProgress(percent);
        tvProgressPercent.setText(percent + "%");

        // ----- Logic phân cấp -----
        String nextTierText = "";

        if (totalPoints >= 300) {
            // GOLD
            tvMemberType.setText("Member Gold");
            tvMemberType.setBackgroundResource(R.drawable.button_rounded_orange);
            progressTier.setProgressTintList(getColorStateList(R.color.gold));

            nextTierText = "Bạn đã đạt cấp cao nhất ";

        } else if (totalPoints >= 20) {
            // SILVER
            tvMemberType.setText("Member Silver");
            tvMemberType.setBackgroundResource(R.drawable.button_rounded_silver);
            progressTier.setProgressTintList(getColorStateList(R.color.silver));

            int needed = 300 - totalPoints;
            nextTierText = "Còn " + needed + " điểm nữa để lên Gold";

        } else {
            // BRONZE
            tvMemberType.setText("Member Bronze");
            tvMemberType.setBackgroundResource(R.drawable.button_rounded_bronze);
            progressTier.setProgressTintList(getColorStateList(R.color.bronze));

            int needed = 20 - totalPoints;
            nextTierText = "Còn " + needed + " điểm nữa để lên Silver";
        }

        tvNextTier.setText(nextTierText);
    }

}





