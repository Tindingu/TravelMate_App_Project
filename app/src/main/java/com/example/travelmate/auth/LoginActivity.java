package com.example.travelmate.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.travelmate.home.HomeActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.example.travelmate.R;
import com.example.travelmate.common.LoadingDialog;

public class LoginActivity extends AppCompatActivity {

    private EditText etLoginEmail, etLoginPassword;
    private Button btnLogin;
    private ImageView ivBackLogin;
    private TextView tvForgotPassword;
    private FirebaseAuth mAuth;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initFirebase();
        initViews();
        setupListeners();

        loadingDialog = new LoadingDialog(this);
    }

    // ============================================================
    // 1️⃣ Khởi tạo Firebase
    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }

    // ============================================================
    // 2️⃣ Ánh xạ View
    private void initViews() {
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        ivBackLogin = findViewById(R.id.ivBackLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
    }

    // ============================================================
    // 3️⃣ Gán sự kiện
    private void setupListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        ivBackLogin.setOnClickListener(v -> finish());
        tvForgotPassword.setOnClickListener(v -> openForgotPassword());
    }

    // ============================================================
    // 4️⃣ Mở trang Quên mật khẩu
    private void openForgotPassword() {
        startActivity(new Intent(this, ForgotPasswordActivity.class));
    }

    // ============================================================
    // 5️⃣ Hàm đăng nhập
    private void loginUser() {
        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (!validateInputs(email, password)) return;

        // 🔹 Hiển thị loading khi bắt đầu đăng nhập
        loadingDialog.showDialog("Đang đăng nhập...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // 🔹 Luôn tắt loading khi Firebase trả kết quả
                        loadingDialog.dismissDialog();

                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            openHomePage();
                        } else {
                            handleLoginError(task.getException());
                        }
                    }
                });
    }

    // ============================================================
    // 6️⃣ Kiểm tra dữ liệu nhập
    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            etLoginEmail.setError("Vui lòng nhập email");
            etLoginEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            etLoginPassword.setError("Vui lòng nhập mật khẩu");
            etLoginPassword.requestFocus();
            return false;
        }
        return true;
    }

    // ============================================================
    // 7️⃣ Xử lý lỗi đăng nhập
    private void handleLoginError(Exception e) {
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            etLoginPassword.setError("Mật khẩu không đúng, vui lòng thử lại.");
            etLoginPassword.requestFocus();
        } else {
            Toast.makeText(this, "Đăng nhập thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ============================================================
    // 8️⃣ Chuyển sang Home (không delay)
    private void openHomePage() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
