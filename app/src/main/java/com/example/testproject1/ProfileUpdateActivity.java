package com.example.testproject1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileUpdateActivity extends AppCompatActivity {

    // 🔹 Views
    private TextView tvDisplayName, tvUsernameDisplay, tvUsername, tvPasswordMask;
    private Button btnChangeAvatar, btnChangeUsername, btnChangePassword;
    private ImageView ivBack, ivMenu;

    // 🔹 Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    // 🔹 Activity Result Launchers
    private ActivityResultLauncher<Intent> changeUsernameResultLauncher;
    private ActivityResultLauncher<Intent> changePasswordResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profileupdate);

        setupWindowInsets();
        initFirebase();
        initViews();
        checkLoginStatus();
        loadUserData();
        setupClickListeners();
        initActivityResultLaunchers();
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
    // 🔹 3. Ánh xạ views
    private void initViews() {
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvUsernameDisplay = findViewById(R.id.tvUsernameDisplay);
        tvUsername = findViewById(R.id.tvUsername);
        tvPasswordMask = findViewById(R.id.tvPasswordMask);

        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        btnChangeUsername = findViewById(R.id.btnChangeUsername);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        ivBack = findViewById(R.id.ivBack);
        ivMenu = findViewById(R.id.ivMenu);
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
    // 🔹 5. Khởi tạo Activity Result Launchers
    private void initActivityResultLaunchers() {
        // Launcher cho change username
        changeUsernameResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String newUsername = result.getData().getStringExtra("username");
                    if (newUsername != null) {
                        // Cập nhật UI với username mới
                        tvUsername.setText(newUsername);
                        tvUsernameDisplay.setText(newUsername);
                        Toast.makeText(this, "Username đã được cập nhật", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );

        // Launcher cho change password
        changePasswordResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, "Mật khẩu đã được cập nhật thành công", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    // ============================================================
    // 🔹 6. Lấy dữ liệu người dùng từ Firestore
    private void loadUserData() {
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Hiển thị tên
                        String displayName = document.getString("displayName");
                        if (displayName == null) displayName = document.getString("name");
                        tvDisplayName.setText(displayName != null ? displayName : "User");

                        // Hiển thị username
                        String username = document.getString("username");
                        if (username == null) username = document.getString("name");
                        tvUsername.setText(username != null ? username : "username");
                        tvUsernameDisplay.setText(username != null ? username : "username");

                        // Hiển thị password mask
                        tvPasswordMask.setText("••••••••");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PROFILE_UPDATE", "Lỗi đọc dữ liệu Firestore", e);
                });
    }

    // ============================================================
    // 🔹 7. Thiết lập các click listeners
    private void setupClickListeners() {
        // Back button
        ivBack.setOnClickListener(v -> finish());

        // Menu button (tạm thời không có logic)
        ivMenu.setOnClickListener(v -> {
            Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show();
        });

        // Change Avatar button
        btnChangeAvatar.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng đổi avatar đang phát triển", Toast.LENGTH_SHORT).show();
        });

        // Change Username button
        btnChangeUsername.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileUpdateActivity.this, ChangeUsernameActivity.class);
            // Truyền username hiện tại
            intent.putExtra("current_username", tvUsername.getText().toString());
            changeUsernameResultLauncher.launch(intent);
        });

        // Change Password button
        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileUpdateActivity.this, ChangePasswordActivity.class);
            changePasswordResultLauncher.launch(intent);
        });
    }
}
