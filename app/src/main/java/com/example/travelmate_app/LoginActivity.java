package com.example.travelmate_app;

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

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ImageView ivBackLogin;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Ánh xạ view
        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        ivBackLogin = findViewById(R.id.ivBackLogin);

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance();

        // Sự kiện khi nhấn nút "Login"
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi Firebase để đăng nhập
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            Toast.makeText(this, "✅ Login successful: " + user.getEmail(), Toast.LENGTH_SHORT).show();

                            // Chuyển sang trang Welcome hoặc MainActivity
                            Intent intent = new Intent(this, MainActivity.class);
                            startActivity(intent);   // 🟢 mở MainActivity
                            finish();                // 🔵 đóng LoginActivity để không quay lại bằng nút Back

                        } else {
                            Toast.makeText(this, "❌ Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Nút quay lại
        ivBackLogin.setOnClickListener(v -> finish());
    }
}
