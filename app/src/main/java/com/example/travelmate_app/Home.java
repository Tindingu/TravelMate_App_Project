package com.example.travelmate_app;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class Home extends AppCompatActivity {

    LinearLayout navHome, navBookmark, navCalendar, navNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Ánh xạ các view từ layout
        navHome = findViewById(R.id.navHome);
        navBookmark = findViewById(R.id.navBookmark);
        navCalendar = findViewById(R.id.navCalendar);
        navNotification = findViewById(R.id.navNotification);

        // Gán sự kiện click cho từng item
        navHome.setOnClickListener(this::onNavItemClicked);
        navBookmark.setOnClickListener(this::onNavItemClicked);
        navCalendar.setOnClickListener(this::onNavItemClicked);
        navNotification.setOnClickListener(this::onNavItemClicked);

        // Mặc định chọn Home khi mở Activity
        setActive(navHome);
    }

    private void onNavItemClicked(View view) {
        // Reset lại tất cả item về trạng thái bình thường
        resetNavState();

        // Đặt trạng thái được chọn cho icon tương ứng
        setActive((LinearLayout) view);
    }

    private void resetNavState() {
        navHome.setBackground(null);
        navBookmark.setBackground(null);
        navCalendar.setBackground(null);
        navNotification.setBackground(null);
    }

    private void setActive(LinearLayout selected) {
        // Gắn background cho item được chọn
        selected.setBackgroundResource(R.drawable.nav_item_selected_bg);
    }
}
