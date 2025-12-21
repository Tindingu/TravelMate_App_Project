package com.example.travelmate.profile;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.travelmate.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ProfilePublicActivity extends AppCompatActivity {

    private ImageView ivAvatar, ivBack;
    private TextView tvProfileName, tvJoinDate, tvMemberType, tvPoints;
    private BarChart barChart;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_profile);

        userId = getIntent().getStringExtra("uid");
        if (userId == null || userId.isEmpty()) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        ivAvatar = findViewById(R.id.ivAvatar);
        ivBack = findViewById(R.id.ivBack);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvJoinDate = findViewById(R.id.tvJoinDate);
        tvMemberType = findViewById(R.id.tvMemberType);
        tvPoints = findViewById(R.id.tvPoints);
        barChart = findViewById(R.id.barChartProfileStats);

        ivBack.setOnClickListener(v -> finish());

        loadUserInfo();
        loadUserStats();
    }

    // =========================
    // LOAD USER BASIC INFO
    // =========================
    private void loadUserInfo() {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    // NAME
                    String name = doc.getString("name");
                    tvProfileName.setText(
                            name != null && !name.isEmpty() ? name : "User"
                    );

                    // AVATAR
                    String avatarUrl = doc.getString("photoUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(this)
                                .load(avatarUrl)
                                .placeholder(R.drawable.avttest)
                                .circleCrop()
                                .into(ivAvatar);
                    } else {
                        ivAvatar.setImageResource(R.drawable.avttest);
                    }

                    // CREATED AT (SAFE)
                    Object createdAtObj = doc.get("createdAt");
                    if (createdAtObj instanceof com.google.firebase.Timestamp) {
                        Date d = ((com.google.firebase.Timestamp) createdAtObj).toDate();
                        tvJoinDate.setText(
                                "Joined on " + DateFormat.format("dd/MM/yyyy", d)
                        );
                    } else if (createdAtObj instanceof Long) {
                        Date d = new Date((Long) createdAtObj);
                        tvJoinDate.setText(
                                "Joined on " + DateFormat.format("dd/MM/yyyy", d)
                        );
                    } else {
                        tvJoinDate.setText("Joined");
                    }
                });
    }

    // =========================
    // LOAD USER STATS (COMMENTS)
    // =========================
    private void loadUserStats() {
        db.collectionGroup("comments")
                .get()
                .addOnSuccessListener(snap -> {

                    int likes = 0;
                    int dislikes = 0;
                    int comments = 0;
                    ArrayList<String> places = new ArrayList<>();

                    for (DocumentSnapshot doc : snap.getDocuments()) {

                        String uid = doc.getString("uid");
                        if (uid == null || !uid.equals(userId)) continue;

                        comments++;

                        Long likeCount = doc.getLong("likeCount");
                        Long dislikeCount = doc.getLong("dislikeCount");

                        likes += likeCount != null ? likeCount : 0;
                        dislikes += dislikeCount != null ? dislikeCount : 0;

                        if (doc.getReference().getParent() != null &&
                                doc.getReference().getParent().getParent() != null) {

                            String placeId =
                                    doc.getReference().getParent().getParent().getId();

                            if (!places.contains(placeId)) {
                                places.add(placeId);
                            }
                        }
                    }

                    int totalPlaces = places.size();
                    int points = likes * 5 + comments * 2 - dislikes * 3 + totalPlaces;

                    tvPoints.setText(String.valueOf(points));
                    updateMemberType(points);

                    if (comments > 0 || likes > 0 || dislikes > 0 || totalPlaces > 0) {
                        drawChart(likes, dislikes, comments, totalPlaces);
                    } else {
                        barChart.setVisibility(View.GONE);
                    }
                });
    }

    // =========================
    // MEMBER TYPE
    // =========================
    private void updateMemberType(int points) {
        if (points >= 300) {
            tvMemberType.setText("Member Gold");
            tvMemberType.setBackgroundResource(R.drawable.button_rounded_gold);
        } else if (points >= 100) {
            tvMemberType.setText("Member Silver");
            tvMemberType.setBackgroundResource(R.drawable.button_rounded_silver);
        } else {
            tvMemberType.setText("Member Bronze");
            tvMemberType.setBackgroundResource(R.drawable.button_rounded_bronze);
        }
    }

    // =========================
    // DRAW CHART
    // =========================
    private void drawChart(int likes, int dislikes, int comments, int places) {

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, likes));
        entries.add(new BarEntry(1, dislikes));
        entries.add(new BarEntry(2, comments));
        entries.add(new BarEntry(3, places));

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        barChart.setData(data);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(
                new String[]{"Likes", "Dislikes", "Comments", "Places"}
        ));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(800);
        barChart.invalidate();
    }
}
