package com.example.testproject1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.testproject1.LoginActivity;
import com.example.testproject1.R;
import com.google.firebase.auth.FirebaseAuth;

/**
 * ForgotPasswordActivity - Xử lý toàn bộ quy trình reset mật khẩu với Firebase
 *
 * Quy trình:
 * 1. User nhập email
 * 2. Validate email
 * 3. Gửi email reset password qua Firebase Authentication
 * 4. Hiển thị thông báo thành công
 * 5. User kiểm tra email và click link trong email
 * 6. Firebase tự động xử lý việc reset password
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnVerify;
    private ImageView ivBack;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.testproject1.R.layout.activity_forgotpassword);

        // Khởi tạo Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Ánh xạ view từ layout
        initViews();

        // Thiết lập các sự kiện
        setupListeners();
        ivBack.setOnClickListener(v -> finish());
    }

    /**
     * Ánh xạ các view từ layout
     */
    private void initViews() {
        etEmail = findViewById(R.id.etForgotPasswordEmail);
        btnVerify = findViewById(R.id.btnVerify);
        ivBack = findViewById(R.id.ivBackForgotPassword);
    }

    /**
     * Thiết lập các sự kiện click
     */
    private void setupListeners() {
        // Sự kiện khi nhấn nút VERIFY
        btnVerify.setOnClickListener(v -> handlePasswordReset());

        // Nút quay lại
        ivBack.setOnClickListener(v -> finish());
    }

    /**
     * Xử lý quy trình reset mật khẩu
     */
    private void handlePasswordReset() {
        String email = etEmail.getText().toString().trim();

        // Validate email trước khi gửi
        if (!validateEmail(email)) {
            return;
        }

        // Disable button để tránh spam click
        btnVerify.setEnabled(false);

        // Gửi email reset password
        sendPasswordResetEmail(email);
    }

    /**
     * Validate email address
     *
     * @param email Email cần validate
     * @return true nếu email hợp lệ, false nếu không
     */
    private boolean validateEmail(String email) {
        // Kiểm tra email có rỗng không
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }

        // Kiểm tra định dạng email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Gửi email reset password qua Firebase Authentication
     *
     * @param email Email của user cần reset password
     */
    private void sendPasswordResetEmail(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    // Enable lại button sau khi hoàn tất
                    btnVerify.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Gửi email thành công
                        handleResetEmailSent(email);
                    } else {
                        // Gửi email thất bại
                        handleResetEmailFailed(task.getException());
                    }
                });
    }

    /**
     * Xử lý khi gửi email reset thành công
     *
     * @param email Email đã gửi
     */
    private void handleResetEmailSent(String email) {
        // Hiển thị dialog thông báo thành công
        showSuccessDialog(email);
    }

    /**
     * Xử lý khi gửi email reset thất bại
     *
     * @param exception Exception từ Firebase
     */
    private void handleResetEmailFailed(Exception exception) {
        String errorMessage = "Failed to send reset email";

        if (exception != null) {
            String exceptionMessage = exception.getMessage();

            // Xử lý các lỗi phổ biến
            if (exceptionMessage != null) {
                if (exceptionMessage.contains("no user record")) {
                    errorMessage = "No account found with this email address";
                } else if (exceptionMessage.contains("network")) {
                    errorMessage = "Network error. Please check your connection";
                } else if (exceptionMessage.contains("too-many-requests")) {
                    errorMessage = "Too many requests. Please try again later";
                } else {
                    errorMessage = exceptionMessage;
                }
            }
        }

        // Hiển thị lỗi
        Toast.makeText(this, "❌ " + errorMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Hiển thị dialog thông báo gửi email thành công
     *
     * @param email Email đã gửi
     */
    private void showSuccessDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("Email Sent Successfully")
                .setMessage("A password reset link has been sent to:\n\n" + email +
                           "\n\nPlease check your email and follow the instructions to reset your password." +
                           "\n\nNote: Check your spam folder if you don't see the email.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Quay về màn hình Login sau khi user click OK
                    navigateToLogin();
                })
                .setNegativeButton("Resend", (dialog, which) -> {
                    // Gửi lại email nếu user muốn
                    sendPasswordResetEmail(email);
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Chuyển về màn hình Login
     */
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Xử lý khi user nhấn nút Back của hệ thống
     */

}

