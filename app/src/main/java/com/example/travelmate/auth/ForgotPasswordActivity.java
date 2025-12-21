package com.example.travelmate.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.travelmate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.example.travelmate.common.LoadingDialog;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnVerify;
    private ImageView ivBack;
    private FirebaseAuth auth;
    private LoadingDialog loadingDialog;
    private boolean isFromChangePassword = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgotpassword);

        auth = FirebaseAuth.getInstance();
        loadingDialog = new LoadingDialog(this);

        // Kiểm tra xem có được gọi từ ChangePasswordActivity không
        Intent intent = getIntent();
        isFromChangePassword = intent.getBooleanExtra("fromChangePassword", false);

        initViews();
        setupListeners();
    }

    // ============================================================
    private void initViews() {
        etEmail = findViewById(R.id.etForgotPasswordEmail);
        btnVerify = findViewById(R.id.btnVerify);
        ivBack = findViewById(R.id.ivBackForgotPassword);
    }

    private void setupListeners() {
        btnVerify.setOnClickListener(v -> handlePasswordReset());
        ivBack.setOnClickListener(v -> finish());
    }

    // ============================================================
    private void handlePasswordReset() {
        String email = etEmail.getText().toString().trim();

        if (!validateEmail(email)) return;

        btnVerify.setEnabled(false);
        loadingDialog.showDialog("Đang gửi email khôi phục...");

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    loadingDialog.dismissDialog();
                    btnVerify.setEnabled(true);

                    if (task.isSuccessful()) {
                        showSuccessDialog(email);
                    } else {
                        handleResetEmailFailed(task.getException());
                    }
                });
    }

    // ============================================================
    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return false;
        }
        return true;
    }

    // ============================================================
    private void showSuccessDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("📩 Email Đã Gửi Thành Công")
                .setMessage("Liên kết đặt lại mật khẩu đã được gửi tới:\n\n" + email +
                        "\n\nHãy kiểm tra hộp thư và click vào link để đặt lại mật khẩu." +
                        "\n\n💡 Nếu không thấy, hãy kiểm tra mục Spam/Thư rác.")
                .setNegativeButton("Gửi lại", (dialog, which) -> sendPasswordResetEmail(email))
                .setCancelable(false);

        // Chuyển sang EmailVerifyActivity để chờ xác thực
        builder.setPositiveButton("OK", (dialog, which) -> {
            Intent intent = new Intent(ForgotPasswordActivity.this, EmailVerifyActivity.class);
            intent.putExtra("email", email);
            intent.putExtra("isPasswordReset", true);
            intent.putExtra("fromChangePassword", isFromChangePassword);
            startActivity(intent);
            finish();
        });

        builder.show();
    }

    private void sendPasswordResetEmail(String email) {
        loadingDialog.showDialog("Đang gửi lại email...");
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    loadingDialog.dismissDialog();
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "✅ Đã gửi lại email!", Toast.LENGTH_SHORT).show();
                    } else {
                        handleResetEmailFailed(task.getException());
                    }
                });
    }

    private void handleResetEmailFailed(Exception exception) {
        String message = "Gửi email thất bại!";
        if (exception != null && exception.getMessage() != null) {
            String msg = exception.getMessage().toLowerCase();
            if (msg.contains("network")) {
                message = "Lỗi mạng. Vui lòng kiểm tra kết nối Internet.";
            } else if (msg.contains("too-many-requests")) {
                message = "Bạn thao tác quá nhanh, vui lòng thử lại sau.";
            }
        }
        Toast.makeText(this, "❌ " + message, Toast.LENGTH_LONG).show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
