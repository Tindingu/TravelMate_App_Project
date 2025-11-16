//    package com.example.testproject1;
//
//    import android.content.Intent;
//    import android.os.Bundle;
//    import android.view.View;
//    import android.widget.ImageView;
//    import android.widget.LinearLayout;
//    import android.widget.TextView;
//
//    import androidx.appcompat.app.AppCompatActivity;
//
//    import com.example.testproject1.ProfileActivity;
//    import com.google.firebase.auth.FirebaseAuth;
//    import com.google.firebase.auth.FirebaseUser;
//
//    public class HomeActivity extends AppCompatActivity {
//
//        LinearLayout navHome, navBookmark, navCalendar, navNotification;
//        private FirebaseAuth auth;
//        private FirebaseUser user;
//        TextView name;
//        private ImageView btnavt;
//        @Override
//        protected void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//
//            setContentView(R.layout.activity_home);
//
//            // Ánh xạ các view từ layout
//            navHome = findViewById(R.id.navHome);
//            navBookmark = findViewById(R.id.navBookmark);
//            navCalendar = findViewById(R.id.navCalendar);
//            navNotification = findViewById(R.id.navNotification);
//            btnavt=findViewById(R.id.ivProfile);
//
//            // Gán sự kiện click cho từng item
//            navHome.setOnClickListener(this::onNavItemClicked);
//            navBookmark.setOnClickListener(this::onNavItemClicked);
//            navCalendar.setOnClickListener(this::onNavItemClicked);
//            navNotification.setOnClickListener(this::onNavItemClicked);
//            // ánh xạ name
//            name=findViewById(R.id.tvHello);
//            // Mặc định chọn Home khi mở Activity
//            setActive(navHome);
//            // Khởi tạo Firebase
//            auth = FirebaseAuth.getInstance();
//            user = auth.getCurrentUser();
//            if(user!=null){
//                name.setText("Hello "+ user.getDisplayName());
//            }else {
//                name.setText("Hello Guest!");
//            }
//            this.btnavt.setOnClickListener(e->{
//                Intent intent = new Intent(this, ProfileActivity.class);
//                startActivity(intent);   // 🟢 mở MainActivity
//            });
//        }
//
//        private void onNavItemClicked(View view) {
//            // Reset lại tất cả item về trạng thái bình thường
//            resetNavState();
//
//            // Đặt trạng thái được chọn cho icon tương ứng
//            setActive((LinearLayout) view);
//        }
//
//        private void resetNavState() {
//            navHome.setBackground(null);
//            navBookmark.setBackground(null);
//            navCalendar.setBackground(null);
//            navNotification.setBackground(null);
//        }
//
//        private void setActive(LinearLayout selected) {
//            // Gắn background cho item được chọn
//            selected.setBackgroundResource(R.drawable.nav_item_selected_bg);
//        }
//
//    }
package com.example.testproject1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    LinearLayout navHome, navBookmark, navCalendar, navNotification;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    TextView name;
    private ImageView btnavt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Ánh xạ view
        navHome = findViewById(R.id.navHome);
        navBookmark = findViewById(R.id.navBookmark);
        navCalendar = findViewById(R.id.navCalendar);
        navNotification = findViewById(R.id.navNotification);
        btnavt = findViewById(R.id.ivProfile);
        name = findViewById(R.id.tvHello);

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        // Mặc định chọn Home khi mở Activity
        setActive(navHome);

        // Gán sự kiện click
        navHome.setOnClickListener(this::onNavItemClicked);
        navBookmark.setOnClickListener(this::onNavItemClicked);
        navCalendar.setOnClickListener(this::onNavItemClicked);
        navNotification.setOnClickListener(this::onNavItemClicked);

        // 🔹 Lấy tên người dùng từ Firestore giống ProfileActivity
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String userName = document.getString("name");
                            if (userName != null && !userName.isEmpty()) {
                                name.setText("Hello " + userName + "!");
                            } else {
                                name.setText("Hello User!");
                            }
                        } else {
                            name.setText("Hello User!");
                        }
                    })
                    .addOnFailureListener(e -> {
                        name.setText("Hello User!");
                    });
        } else {
            name.setText("Hello Guest!");
        }

        // 🔹 Khi nhấn avatar → mở ProfileActivity
        btnavt.setOnClickListener(e -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    private void onNavItemClicked(View view) {
        resetNavState();
        setActive((LinearLayout) view);
    }

    private void resetNavState() {
        navHome.setBackground(null);
        navBookmark.setBackground(null);
        navCalendar.setBackground(null);
        navNotification.setBackground(null);
    }

    private void setActive(LinearLayout selected) {
        selected.setBackgroundResource(R.drawable.nav_item_selected_bg);
    }
}
