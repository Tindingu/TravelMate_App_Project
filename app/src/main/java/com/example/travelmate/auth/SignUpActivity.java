package com.example.travelmate.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.travelmate.R;
import com.example.travelmate.common.LoadingDialog;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etName;
    private Button btnSignUp;
    private ImageView ivBackSignUp;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initViews();
        initFirebase();
        setupListeners();

        loadingDialog = new LoadingDialog(this);
    }

    private void initViews() {
        etEmail = findViewById(R.id.etSignUpEmail);
        etPassword = findViewById(R.id.etSignUpPassword);
        etName = findViewById(R.id.etSignUpName);
        btnSignUp = findViewById(R.id.btnSignUp);
        ivBackSignUp = findViewById(R.id.ivBackSignUp);
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void setupListeners() {
        btnSignUp.setOnClickListener(v -> signUpUser());
        ivBackSignUp.setOnClickListener(v -> finish());
    }

    private void signUpUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();

        if (!validateInputs(name, email, password)) return;

        loadingDialog.showDialog("Đang tạo tài khoản...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            updateUserProfile(user, name);
                            saveUserToFirestore(user, name, email);
                        } else {
                            loadingDialog.dismissDialog();
                            Toast.makeText(this, "Lỗi: không lấy được thông tin người dùng", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        loadingDialog.dismissDialog();
                        Toast.makeText(this, "❌ Đăng ký thất bại: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInputs(String name, String email, String password) {
        if (TextUtils.isEmpty(name)) {
            etName.setError("Vui lòng nhập tên");
            etName.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải từ 6 ký tự trở lên");
            etPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void updateUserProfile(FirebaseUser user, String name) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        user.updateProfile(profileUpdates);
    }

    private void saveUserToFirestore(FirebaseUser user, String name, String email) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("uid", user.getUid());
        userInfo.put("name", name);
        userInfo.put("email", email);
        userInfo.put("createdAt", System.currentTimeMillis());

        firestore.collection("users")
                .document(user.getUid())
                .set(userInfo)
                .addOnSuccessListener(aVoid -> {
                    loadingDialog.dismissDialog();
                    Toast.makeText(this, "🎉 Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    navigateToWelcome();
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismissDialog();
                    Toast.makeText(this, "❌ Lưu thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToWelcome() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
