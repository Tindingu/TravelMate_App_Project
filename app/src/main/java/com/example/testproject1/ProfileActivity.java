//package com.example.testproject1;
//
//import android.app.AlertDialog;
//import android.content.Intent;
//import android.graphics.Color;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.ProgressBar;
//import android.widget.SeekBar;
//import android.widget.TextView;
//
//import androidx.activity.EdgeToEdge;
//import androidx.activity.result.ActivityResultLauncher;
//import androidx.activity.result.contract.ActivityResultContracts;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.github.mikephil.charting.charts.BarChart;
//import com.github.mikephil.charting.components.XAxis;
//import com.github.mikephil.charting.data.BarData;
//import com.github.mikephil.charting.data.BarDataSet;
//import com.github.mikephil.charting.data.BarEntry;
//import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
//import com.github.mikephil.charting.model.GradientColor;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.firestore.FirebaseFirestore;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.Locale;
//
//public class ProfileActivity extends AppCompatActivity {
//
//    // 🔹 View
//    private TextView tvProfileName, tvJoinDate, tvMemberType, layoutProfile;
//    private ImageView ivAvatar, ivBack;
//    private Button btnLogout;
//    private BarChart barChart;
//
//    // 🔹 Firebase
//    private FirebaseAuth auth;
//    private FirebaseFirestore db;
//    private FirebaseUser user;
//    private TextView  tvPoint;
//
//    // Activity Result Launcher for ProfileUpdateActivity
//    private ActivityResultLauncher<Intent> profileUpdateActivityLauncher;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_profile);
//
//        setupWindowInsets();
//        initFirebase();
//        initViews();
//        checkLoginStatus();
//        initProfileUpdateActivityLauncher();
//        loadUserData();
//        setupLogoutButton();
//        setupBackButton();
//        loadUserStats();
//        setupProfileButton();
//        setupUserChart();
//    }
//
//    // ============================================================
//    // 🔹 1. Cấu hình khoảng cách giao diện với thanh trạng thái
//    private void setupWindowInsets() {
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//    }
//
//    // ============================================================
//    // 🔹 2. Khởi tạo Firebase
//    private void initFirebase() {
//        auth = FirebaseAuth.getInstance();
//        db = FirebaseFirestore.getInstance();
//        user = auth.getCurrentUser();
//    }
//
//    // ============================================================
//    // 🔹 3. Ánh xạ view
//    private void initViews() {
//        tvProfileName = findViewById(R.id.tvProfileName);
//        tvJoinDate = findViewById(R.id.tvJoinDate);
//        tvMemberType = findViewById(R.id.tvMemberType);
//        layoutProfile = findViewById(R.id.layoutProfile);
//        ivAvatar = findViewById(R.id.ivAvatar);
//        ivBack = findViewById(R.id.ivBack);
//        btnLogout = findViewById(R.id.btnLogout);
//        barChart = findViewById(R.id.barChartProfileStats);
//        tvPoint=findViewById(R.id.tvPoints);
//    }
//
//    // ============================================================
//    // 🔹 4. Kiểm tra người dùng đăng nhập
//    private void checkLoginStatus() {
//        if (user == null) {
//            Intent intent = new Intent(this, WelcomeActivity.class);
//            startActivity(intent);
//            finish();
//        }
//    }
//
//    // ============================================================
//    // 🔹 4. Khởi tạo Activity Result Launcher cho ProfileUpdate
//    private void initProfileUpdateActivityLauncher() {
//        profileUpdateActivityLauncher = registerForActivityResult(
//            new ActivityResultContracts.StartActivityForResult(),
//            result -> {
//                // Khi quay lại từ ProfileUpdateActivity, load lại user data và avatar
//                // để cập nhật những thay đổi có thể có (bao gồm avatar mới)
//                loadUserData();
//                // Set result OK để HomeActivity cũng cập nhật
//                setResult(RESULT_OK);
//            }
//        );
//    }
//
//    // ============================================================
//    // 🔹 5. Lấy dữ liệu người dùng từ Firestore
//    private void loadUserData() {
//        if (user == null) return;
//
//        db.collection("users").document(user.getUid())
//                .get()
//                .addOnSuccessListener(document -> {
//                    if (document.exists()) {
//                        // 🔸 Hiển thị tên
//                        String name = document.getString("name");
//                        tvProfileName.setText(name != null ? name : "User");
//
//                        // 🔸 Load avatar từ photoUrl
//                        String photoUrl = document.getString("photoUrl");
//                        if (photoUrl != null && !photoUrl.isEmpty()) {
//                            com.bumptech.glide.Glide.with(this)
//                                    .load(photoUrl)
//                                    .circleCrop()
//                                    .placeholder(R.drawable.sample_avatar)
//                                    .error(R.drawable.sample_avatar)
//                                    .into(ivAvatar);
//                        } else {
//                            // Load default avatar
//                            com.bumptech.glide.Glide.with(this)
//                                    .load(R.drawable.sample_avatar)
//                                    .circleCrop()
//                                    .into(ivAvatar);
//                        }
//
//                        // 🔸 Hiển thị ngày tham gia
//                        if (user.getMetadata() != null) {
//                            long creationTime = user.getMetadata().getCreationTimestamp();
//                            Date date = new Date(creationTime);
//                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
//                            tvJoinDate.setText("Tham gia ngày " + sdf.format(date));
//                        } else {
//                            tvJoinDate.setText("Tham gia gần đây");
//                        }
//
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    android.util.Log.e("PROFILE_FIRESTORE", "Lỗi đọc dữ liệu Firestore", e);
//                    tvJoinDate.setText("Không thể tải thời gian tham gia");
//                });
//    }
//
//    // ============================================================
//    // 🔹 6. Nút logout với xác nhận
//    private void setupLogoutButton() {
//        btnLogout.setOnClickListener(v -> new AlertDialog.Builder(this)
//                .setTitle("Xác nhận đăng xuất")
//                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
//                .setPositiveButton("Đăng xuất", (dialog, which) -> {
//                    auth.signOut();
//                    Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    finish();
//                })
//                .setNegativeButton("Hủy", null)
//                .show());
//    }
//
//    // ============================================================
//    // 🔹 7. Nút quay lại
//    private void setupBackButton() {
//        ivBack.setOnClickListener(v -> finish());
//    }
//
//    // ============================================================
//    // 🔹 8. Nút Hồ sơ - chuyển sang ProfileUpdate
//    private void setupProfileButton() {
//        layoutProfile.setOnClickListener(v -> {
//            Intent intent = new Intent(ProfileActivity.this, ProfileUpdateActivity.class);
//            profileUpdateActivityLauncher.launch(intent);
//        });
//    }
//
//    // ============================================================
//    // 🔹 9. Biểu đồ thống kê người dùng
//    private void setupUserChart() {
//    // 🔹 8. Biểu đồ thống kê người dùng
//    private void setupUserChart(int totalLikes,int totalDislikes,int totalComments,int totalPlaces) {
//        ArrayList<BarEntry> entries = new ArrayList<>();
//        entries.add(new BarEntry(0, totalLikes)); // Likes
//        entries.add(new BarEntry(1, totalDislikes));  // Dislikes
//        entries.add(new BarEntry(2, totalComments));  // Comments
//        entries.add(new BarEntry(3, totalPlaces));  // totalPlaces
//
//        BarDataSet dataSet = new BarDataSet(entries, "Thống kê người dùng");
//
//        ArrayList<GradientColor> gradientColors = new ArrayList<>();
//        gradientColors.add(new GradientColor(Color.parseColor("#FFA726"), Color.parseColor("#FB8C00")));
//        gradientColors.add(new GradientColor(Color.parseColor("#FFA726"), Color.parseColor("#FB8C00")));
//        gradientColors.add(new GradientColor(Color.parseColor("#FFA726"), Color.parseColor("#FB8C00")));
//        gradientColors.add(new GradientColor(Color.parseColor("#FFA726"), Color.parseColor("#FB8C00")));
//        dataSet.setGradientColors(gradientColors);
//        dataSet.setValueTextSize(14f);
//        dataSet.setValueTextColor(Color.parseColor("#333333"));
//
//        BarData barData = new BarData(dataSet);
//        barData.setBarWidth(0.6f);
//        barChart.setData(barData);
//
//        String[] labels = {"Likes", "Dislikes", "Comments", "TotalPlaces"};
//        XAxis xAxis = barChart.getXAxis();
//        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
//        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
//        xAxis.setTextSize(12f);
//        xAxis.setTextColor(Color.parseColor("#555555"));
//        xAxis.setGranularity(1f);
//        xAxis.setLabelCount(labels.length);
//        xAxis.setDrawGridLines(false);
//        xAxis.setLabelRotationAngle(-10f);
//
//        barChart.getAxisLeft().setTextColor(Color.parseColor("#777777"));
//        barChart.getAxisLeft().setGridColor(Color.parseColor("#E0E0E0"));
//        barChart.getAxisRight().setEnabled(false);
//
//        barChart.getDescription().setEnabled(false);
//        barChart.getLegend().setEnabled(false);
//
//        barChart.setDrawGridBackground(false);
//        barChart.setDrawBorders(false);
//        barChart.setDrawValueAboveBar(true);
//        barChart.setExtraOffsets(5, 10, 5, 10);
//
//        barChart.animateY(1200, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);
//        barChart.invalidate();
//    }
//    private void loadUserStats() {
//        String uid = auth.getUid();
//        if (uid == null) {
//            Log.e("USER_STATS", "UID NULL");
//            return;
//        }
//
//        Log.d("USER_STATS", "Bắt đầu load thống kê cho UID: " + uid);
//
//        db.collectionGroup("comments")
//                .get()
//                .addOnSuccessListener(snap -> {
//
//                    Log.d("USER_STATS", "Tổng số documents tìm thấy: " + snap.size());
//
//                    int totalLikes = 0;
//                    int totalDislikes = 0;
//                    int totalComments = 0;
//                    ArrayList<String> places = new ArrayList<>();
//
//                    for (var doc : snap.getDocuments()) {
//
//                        Log.d("USER_STATS", "-----------------------------------");
//                        Log.d("USER_STATS", "COMMENT DOC ID = " + doc.getId());
//
//                        CommentModel c = doc.toObject(CommentModel.class);
//                        if (c == null) {
//                            Log.e("USER_STATS", "CommentModel NULL → SKIP");
//                            continue;
//                        }
//
//                        String commentUid = doc.getString("uid");
//                        Log.d("USER_STATS", "UID trong comment = " + commentUid);
//
//                        // Chỉ tính comment của user này
//                        if (!uid.equals(commentUid)) {
//                            Log.d("USER_STATS", "UID không trùng, bỏ qua");
//                            continue;
//                        }
//
//                        Log.d("USER_STATS", "UID khớp → tính thống kê");
//
//                        totalComments++;
//
//                        Long likeCount = doc.getLong("likeCount");
//                        Long dislikeCount = doc.getLong("dislikeCount");
//
//                        Log.d("USER_STATS", "likeCount = " + likeCount);
//                        Log.d("USER_STATS", "dislikeCount = " + dislikeCount);
//
//                        totalLikes += (likeCount != null ? likeCount : 0);
//                        totalDislikes += (dislikeCount != null ? dislikeCount : 0);
//
//                        // Lấy ID của Place chứa comment
//                        String placeId = doc.getReference().getParent().getParent().getId();
//                        Log.d("USER_STATS", "Place chứa comment: " + placeId);
//
//                        if (!places.contains(placeId)) {
//                            places.add(placeId);
//                            Log.d("USER_STATS", "➕ Thêm Place vào danh sách");
//                        }
//                    }
//
//                    int totalPlaces = places.size();
//                    int totalPoints = (totalLikes * 5)
//                            + (totalComments * 2)
//                            - (totalDislikes * 3)
//                            + (totalPlaces * 1);
//
//                    updateUserTier(totalPoints);
//
//                    Log.d("USER_STATS", "============ KẾT QUẢ ============");
//                    Log.d("USER_STATS", "totalLikes     = " + totalLikes);
//                    Log.d("USER_STATS", "totalDislikes  = " + totalDislikes);
//                    Log.d("USER_STATS", "totalComments  = " + totalComments);
//                    Log.d("USER_STATS", "totalPlaces    = " + totalPlaces);
//                    Log.d("USER_STATS", "=================================");
//
//                    setupUserChart(totalLikes, totalDislikes, totalComments, totalPlaces);
//                })
//                .addOnFailureListener(e -> {
//                    Log.e("USER_STATS", "LỖI FIRESTORE: " + e.getMessage());
//                });
//    }
//    private void updateUserTier(int totalPoints) {
//
//        TextView tvMemberType = findViewById(R.id.tvMemberType);
//        TextView tvPoint = findViewById(R.id.tvPoints);
//        SeekBar progressTier = findViewById(R.id.seekLevelProgress);
//        TextView tvNextTier = findViewById(R.id.tvNextTier);
//        TextView tvProgressPercent = findViewById(R.id.tvProgressPercent);
//
//        // ----- Cập nhật điểm hiển thị -----
//        tvPoint.setText(String.valueOf(totalPoints));
//
//        // ----- Tính % tiến trình (max 300 là Gold) -----
//        int capped = Math.min(totalPoints, 300);
//        int percent = (int) ((capped / 300f) * 100);
//
//        progressTier.setProgress(percent);
//        tvProgressPercent.setText(percent + "%");
//
//        // ----- Logic phân cấp -----
//        String nextTierText = "";
//
//        if (totalPoints >= 300) {
//            // GOLD
//            tvMemberType.setText("Member Gold");
//            tvMemberType.setBackgroundResource(R.drawable.button_rounded_orange);
//            progressTier.setProgressTintList(getColorStateList(R.color.gold));
//
//            nextTierText = "Bạn đã đạt cấp cao nhất ";
//
//        } else if (totalPoints >= 20) {
//            // SILVER
//            tvMemberType.setText("Member Silver");
//            tvMemberType.setBackgroundResource(R.drawable.button_rounded_silver);
//            progressTier.setProgressTintList(getColorStateList(R.color.silver));
//
//            int needed = 300 - totalPoints;
//            nextTierText = "Còn " + needed + " điểm nữa để lên Gold";
//
//        } else {
//            // BRONZE
//            tvMemberType.setText("Member Bronze");
//            tvMemberType.setBackgroundResource(R.drawable.button_rounded_bronze);
//            progressTier.setProgressTintList(getColorStateList(R.color.bronze));
//
//            int needed = 20 - totalPoints;
//            nextTierText = "Còn " + needed + " điểm nữa để lên Silver";
//        }
//
//        tvNextTier.setText(nextTierText);
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        // Reload user data when activity resumes (in case user came back from ProfileUpdateActivity with avatar changes)
//        loadUserData();
//    }
//}
//
//}
//
//
//
//
//

package com.example.testproject1;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.animation.Easing;
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

    // =============================
    // UI
    // =============================
    private TextView tvProfileName, tvJoinDate, tvMemberType, tvPoints;
    private TextView tvNextTier, tvProgressPercent;
    private ImageView ivAvatar, ivBack;
    private Button btnLogout;
    private SeekBar seekLevelProgress;
    private BarChart barChart;

    // =============================
    // Firebase
    // =============================
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    private ActivityResultLauncher<Intent> profileUpdateLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        setupWindowInsets();
        initFirebase();
        initViews();
        checkLoginStatus();
        initProfileUpdateLauncher();
        setupBackButton();
        setupLogoutButton();

        loadUserData();
        loadUserStats();
    }

    // =============================
    // Insets
    // =============================
    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content),
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });
    }

    // =============================
    // Firebase
    // =============================
    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();
    }

    // =============================
    // Views
    // =============================
    private void initViews() {
        tvProfileName = findViewById(R.id.tvProfileName);
        tvJoinDate = findViewById(R.id.tvJoinDate);
        tvMemberType = findViewById(R.id.tvMemberType);
        tvPoints = findViewById(R.id.tvPoints);
        tvNextTier = findViewById(R.id.tvNextTier);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);

        ivAvatar = findViewById(R.id.ivAvatar);
        ivBack = findViewById(R.id.ivBack);
        btnLogout = findViewById(R.id.btnLogout);

        seekLevelProgress = findViewById(R.id.seekLevelProgress);
        barChart = findViewById(R.id.barChartProfileStats);

        findViewById(R.id.layoutProfile).setOnClickListener(v -> openProfileUpdate());
    }

    // =============================
    // Login check
    // =============================
    private void checkLoginStatus() {
        if (user == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        }
    }

    // =============================
    // Profile Update
    // =============================
    private void initProfileUpdateLauncher() {
        profileUpdateLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> loadUserData());
    }

    private void openProfileUpdate() {
        profileUpdateLauncher.launch(
                new Intent(this, ProfileUpdateActivity.class)
        );
    }

    // =============================
    // User Info
    // =============================
    private void loadUserData() {
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    tvProfileName.setText(doc.getString("name"));

                    String photoUrl = doc.getString("photoUrl");
                    com.bumptech.glide.Glide.with(this)
                            .load(photoUrl != null ? photoUrl : R.drawable.sample_avatar)
                            .circleCrop()
                            .into(ivAvatar);

                    if (user.getMetadata() != null) {
                        long time = user.getMetadata().getCreationTimestamp();
                        SimpleDateFormat sdf =
                                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        tvJoinDate.setText("Tham gia ngày " + sdf.format(new Date(time)));
                    }
                });
    }

    // =============================
    // Logout
    // =============================
    private void setupLogoutButton() {
        btnLogout.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Đăng xuất")
                        .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                        .setPositiveButton("Đăng xuất", (d, w) -> {
                            auth.signOut();
                            Intent i = new Intent(this, WelcomeActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(i);
                            finish();
                        })
                        .setNegativeButton("Hủy", null)
                        .show()
        );
    }

    private void setupBackButton() {
        ivBack.setOnClickListener(v -> finish());
    }

    // =============================
    // USER STATS + CHART
    // =============================
    private void loadUserStats() {
        if (user == null) return;

        db.collectionGroup("comments")
                .get()
                .addOnSuccessListener(snap -> {

                    int likes = 0, dislikes = 0, comments = 0;
                    ArrayList<String> places = new ArrayList<>();

                    for (var doc : snap.getDocuments()) {
                        if (!user.getUid().equals(doc.getString("uid"))) continue;

                        comments++;
                        likes += doc.getLong("likeCount") != null ? doc.getLong("likeCount") : 0;
                        dislikes += doc.getLong("dislikeCount") != null ? doc.getLong("dislikeCount") : 0;

                        String placeId = doc.getReference().getParent().getParent().getId();
                        if (!places.contains(placeId)) places.add(placeId);
                    }

                    int totalPlaces = places.size();
                    int points = likes * 5 + comments * 2 - dislikes * 3 + totalPlaces;

                    updateTier(points);
                    setupUserChart(likes, dislikes, comments, totalPlaces);
                });
    }

    private void setupUserChart(int likes, int dislikes, int comments, int places) {

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, likes));
        entries.add(new BarEntry(1, dislikes));
        entries.add(new BarEntry(2, comments));
        entries.add(new BarEntry(3, places));

        BarDataSet set = new BarDataSet(entries, "Thống kê");
        set.setValueTextSize(14f);

        ArrayList<GradientColor> colors = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            colors.add(new GradientColor(Color.parseColor("#FFA726"), Color.parseColor("#FB8C00")));
        }
        set.setGradientColors(colors);

        BarData data = new BarData(set);
        data.setBarWidth(0.6f);
        barChart.setData(data);

        XAxis x = barChart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(
                new String[]{"Likes", "Dislikes", "Comments", "Places"}
        ));
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);
        x.setDrawGridLines(false);

        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.animateY(1000, Easing.EaseInOutQuad);
        barChart.invalidate();
    }

    // =============================
    // TIER
    // =============================
    private void updateTier(int points) {
        tvPoints.setText(String.valueOf(points));

        int percent = Math.min(100, (int) ((points / 300f) * 100));
        seekLevelProgress.setProgress(percent);
        tvProgressPercent.setText(percent + "%");

        if (points >= 300) {
            tvMemberType.setText("Member Gold");
        } else if (points >= 20) {
            tvMemberType.setText("Member Silver");
        } else {
            tvMemberType.setText("Member Bronze");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }
}
