package com.example.travelmate.profile;

import android.content.Intent;
import android.net.Uri;
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

import com.bumptech.glide.Glide;
import com.example.travelmate.R;
import com.example.travelmate.auth.WelcomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileUpdateActivity extends AppCompatActivity {

    // 🔹 Views
    private TextView tvDisplayName, tvUsernameDisplay, tvUsername, tvPasswordMask;
    private Button btnChangeAvatar, btnChangeUsername, btnChangePassword;
    private ImageView ivBack, ivMenu, ivProfileAvatar;

    // 🔹 Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    // 🔹 Activity Result Launchers
    private ActivityResultLauncher<Intent> changeUsernameResultLauncher;
    private ActivityResultLauncher<Intent> changePasswordResultLauncher;
    private ActivityResultLauncher<Intent> changeAvatarResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profileupdate);

        setupWindowInsets();
        initFirebase();
        initViews();
        checkLoginStatus();
        initActivityResultLaunchers();
        loadUserData();
        setupClickListeners();
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
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
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
        // Launcher cho change avatar
        changeAvatarResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String avatarUri = result.getData().getStringExtra("avatar_uri");
                        String photoUrl = result.getData().getStringExtra("photo_url");

                        if (avatarUri != null) {
                            // Update the UI avatar ImageView with returned uri
                            try {
                                Glide.with(this)
                                        .load(Uri.parse(avatarUri))
                                        .circleCrop()
                                        .into(ivProfileAvatar);
                            } catch (Exception e) {
                                android.util.Log.e("PROFILE_UPDATE", "Failed to set avatar image", e);
                            }
                            Toast.makeText(this, "Avatar đã được cập nhật", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Launcher cho change username (thực tế là change display name)
        changeUsernameResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String newDisplayName = result.getData().getStringExtra("username");
                    if (newDisplayName != null) {
                        // Cập nhật UI với tên hiển thị mới
                        tvUsername.setText(newDisplayName);
                        tvUsernameDisplay.setText(newDisplayName);
                        tvDisplayName.setText(newDisplayName);
                        Toast.makeText(this, "Tên hiển thị đã được cập nhật", Toast.LENGTH_SHORT).show();
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
                        // Hiển thị tên (đọc từ field "name")
                        String displayName = document.getString("name");
                        if (displayName == null) displayName = document.getString("displayName"); // fallback cũ
                        tvDisplayName.setText(displayName != null ? displayName : "User");

                        // Hiển thị username (cũng dùng field "name")
                        tvUsername.setText(displayName != null ? displayName : "username");
                        tvUsernameDisplay.setText(displayName != null ? displayName : "username");

                        // Hiển thị password mask
                        tvPasswordMask.setText("••••••••");

                        // Load avatar từ photoUrl
                        String photoUrl = document.getString("photoUrl");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.sample_avatar)
                                    .error(R.drawable.sample_avatar)
                                    .into(ivProfileAvatar);
                        } else {
                            // Load default avatar
                            Glide.with(this)
                                    .load(R.drawable.sample_avatar)
                                    .circleCrop()
                                    .into(ivProfileAvatar);
                        }
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
        ivMenu.setOnClickListener(v -> Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show());

        // Also allow tapping the avatar image itself to edit
        ivProfileAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileUpdateActivity.this, ChangeAvatar.class);
            changeAvatarResultLauncher.launch(intent);
        });

        // Change Avatar button
        btnChangeAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileUpdateActivity.this, ChangeAvatar.class);
            changeAvatarResultLauncher.launch(intent);
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
