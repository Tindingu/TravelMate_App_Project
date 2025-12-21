package com.example.travelmate.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.travelmate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailVerifyActivity extends AppCompatActivity {

    private static final String TAG = "EmailVerifyActivity";
    private TextView tvResendCode, tvWaitingTitle, tvWaitingSubtitle;
    private ImageView ivBack;
    private FirebaseAuth auth;
    private FirebaseUser user;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable checkEmailVerifiedRunnable;
    private final long CHECK_INTERVAL_MS = 3000; // kiểm tra mỗi 3s

    // Biến để xử lý password reset flow
    private boolean isPasswordReset = false;
    private boolean isFromChangePassword = false;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_emailverify);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        // Nhận thông tin từ Intent
        Intent intent = getIntent();
        isPasswordReset = intent.getBooleanExtra("isPasswordReset", false);
        isFromChangePassword = intent.getBooleanExtra("fromChangePassword", false);
        userEmail = intent.getStringExtra("email");

        initViews();
        updateUIForPasswordReset();
        setupClickListeners();
        startMonitoring();
    }

    private void initViews() {
        tvResendCode = findViewById(R.id.tvResendCode);
        tvWaitingTitle = findViewById(R.id.tvWaitingTitle);
        tvWaitingSubtitle = findViewById(R.id.tvWaitingSubtitle);
        ivBack = findViewById(R.id.ivBackEmailVerify);
    }

    private void updateUIForPasswordReset() {
        if (isPasswordReset) {
            tvWaitingTitle.setText("Waiting for password reset...");
            tvWaitingSubtitle.setText("Please check your email and click the reset link. This screen will redirect automatically once you complete the password reset.");
            tvResendCode.setText("Resend Reset Email");
        }
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        tvResendCode.setOnClickListener(v -> {
            if (isPasswordReset) {
                // Gửi lại email reset password
                if (userEmail != null) {
                    auth.sendPasswordResetEmail(userEmail)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to send email: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            } else {
                // Gửi email verification thông thường
                if (user != null) {
                    user.sendEmailVerification()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to send email: " + e.getMessage(), Toast.LENGTH_LONG).show());
                } else {
                    Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void startMonitoring() {
        if (isPasswordReset) {
            // Đối với password reset, sử dụng logic khác để detect khi user đã reset password
            checkPasswordResetCompletion();
        } else {
            // Email verification thông thường
            checkEmailVerification();
        }
    }

    private void checkEmailVerification() {
        checkEmailVerifiedRunnable = new Runnable() {
            @Override
            public void run() {
                if (user == null) return;
                user.reload().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser refreshed = auth.getCurrentUser();
                        if (refreshed != null && refreshed.isEmailVerified()) {
                            Log.d(TAG, "Email verified -> finishing activity");
                            Toast.makeText(EmailVerifyActivity.this, "Email verified", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    handler.postDelayed(this, CHECK_INTERVAL_MS);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to reload user", e);
                    handler.postDelayed(this, CHECK_INTERVAL_MS);
                });
            }
        };
        handler.postDelayed(checkEmailVerifiedRunnable, CHECK_INTERVAL_MS);
    }

    private void checkPasswordResetCompletion() {
        // Đối với password reset, chúng ta không thể trực tiếp detect khi user click link
        // Thay vào đó, chúng ta sẽ check xem user có thể sign in lại không
        // Hoặc sử dụng một cách khác để detect

        // Tạm thời, chúng ta sẽ hiển thị một nút để user bấm khi họ đã hoàn thành reset
        // Hoặc chúng ta có thể sử dụng Firebase Dynamic Links để handle deep link

        // Một cách đơn giản hơn là sau 10 giây, tự động chuyển sang NewPasswordActivity
        // (giả sử user đã click link)
        handler.postDelayed(() -> {
            Log.d(TAG, "Auto-navigating to NewPasswordActivity after delay");
            navigateToNewPassword();
        }, 10000); // 10 giây
    }

    private void navigateToNewPassword() {
        Intent intent = new Intent(this, NewPasswordActivity.class);
        intent.putExtra("fromChangePassword", isFromChangePassword);
        intent.putExtra("email", userEmail);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (checkEmailVerifiedRunnable != null) {
            handler.removeCallbacks(checkEmailVerifiedRunnable);
        }
    }
}
