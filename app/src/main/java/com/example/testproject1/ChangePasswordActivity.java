package com.example.testproject1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    // 🔹 Views
    private EditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private Button btnChangePassword;
    private TextView tvForgotPassword;
    private ImageView ivBack;

    // 🔹 Firebase
    private FirebaseAuth auth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_changepassword_profile);

        setupWindowInsets();
        initFirebase();
        initViews();
        checkLoginStatus();
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
        user = auth.getCurrentUser();
    }

    // ============================================================
    // 🔹 3. Ánh xạ views
    private void initViews() {
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        ivBack = findViewById(R.id.ivBack);
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
    // 🔹 5. Thiết lập click listeners
    private void setupClickListeners() {
        // Back button
        ivBack.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Forgot password
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(ChangePasswordActivity.this, ForgotPasswordActivity.class);
            intent.putExtra("fromChangePassword", true);
            startActivity(intent);
        });

        // Change password button
        btnChangePassword.setOnClickListener(v -> {
            changePassword();
        });

        // Handle back gesture
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    // ============================================================
    // 🔹 6. Đổi mật khẩu
    private void changePassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmNewPassword.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu hiện tại", Toast.LENGTH_SHORT).show();
            etCurrentPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu mới", Toast.LENGTH_SHORT).show();
            etNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "Mật khẩu mới phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            etNewPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Vui lòng nhập lại mật khẩu mới", Toast.LENGTH_SHORT).show();
            etConfirmNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu mới không khớp", Toast.LENGTH_SHORT).show();
            etConfirmNewPassword.requestFocus();
            return;
        }

        if (currentPassword.equals(newPassword)) {
            Toast.makeText(this, "Mật khẩu mới phải khác mật khẩu hiện tại", Toast.LENGTH_SHORT).show();
            etNewPassword.requestFocus();
            return;
        }

        // Disable button để tránh click nhiều lần
        btnChangePassword.setEnabled(false);
        btnChangePassword.setText("Đang đổi mật khẩu...");

        // Thực hiện đổi mật khẩu
        updatePassword(currentPassword, newPassword);
    }

    // ============================================================
    // 🔹 7. Cập nhật mật khẩu trên Firebase Auth
    private void updatePassword(String currentPassword, String newPassword) {
        if (user == null || user.getEmail() == null) {
            enableChangePasswordButton();
            Toast.makeText(this, "Không thể xác thực người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo credential để xác thực lại người dùng
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        // Xác thực lại người dùng trước khi đổi mật khẩu
        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Xác thực thành công, tiến hành đổi mật khẩu
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid1 -> {
                                // Đổi mật khẩu thành công
                                Toast.makeText(ChangePasswordActivity.this,
                                    "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();

                                // Trả kết quả về ProfileUpdateActivity
                                setResult(RESULT_OK);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // Đổi mật khẩu thất bại
                                android.util.Log.e("CHANGE_PASSWORD", "Lỗi đổi mật khẩu", e);
                                Toast.makeText(ChangePasswordActivity.this,
                                    "Lỗi đổi mật khẩu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                enableChangePasswordButton();
                            });
                })
                .addOnFailureListener(e -> {
                    // Xác thực thất bại
                    android.util.Log.e("CHANGE_PASSWORD", "Lỗi xác thực mật khẩu hiện tại", e);
                    Toast.makeText(ChangePasswordActivity.this,
                        "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show();
                    etCurrentPassword.requestFocus();
                    enableChangePasswordButton();
                });
    }

    // ============================================================
    // 🔹 8. Enable lại Change Password button
    private void enableChangePasswordButton() {
        btnChangePassword.setEnabled(true);
        btnChangePassword.setText("Đổi mật khẩu");
    }

    // ============================================================
    // 🔹 9. Clear sensitive data khi activity bị destroy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (etCurrentPassword != null) {
            etCurrentPassword.setText("");
        }
        if (etNewPassword != null) {
            etNewPassword.setText("");
        }
        if (etConfirmNewPassword != null) {
            etConfirmNewPassword.setText("");
        }
    }
}
