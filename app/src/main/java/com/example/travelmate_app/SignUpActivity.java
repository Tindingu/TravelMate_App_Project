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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnSignUp;
    private ImageView ivBackSignUp;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Ánh xạ view
        etEmail = findViewById(R.id.etSignUpEmail);
        etPassword = findViewById(R.id.etSignUpPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        ivBackSignUp = findViewById(R.id.ivBackSignUp);

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Khi người dùng nhấn nút "Sign Up"
        btnSignUp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Kiểm tra dữ liệu
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tạo tài khoản mới trên Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();

                            if (user != null) {
                                // Lưu vào Firestore
                                Map<String, Object> userInfo = new HashMap<>();
                                userInfo.put("email", user.getEmail());
                                userInfo.put("uid", user.getUid());
                                userInfo.put("createdAt", System.currentTimeMillis());

                                firestore.collection("users")
                                        .document(user.getUid())
                                        .set(userInfo)
                                        .addOnSuccessListener(aVoid ->
                                                Toast.makeText(this, "✅ User saved to Firestore", Toast.LENGTH_SHORT).show()
                                        )
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "❌ Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                        );
                            }

                            Toast.makeText(this, "🎉 Sign up successful!", Toast.LENGTH_SHORT).show();
                            Intent   intent = new Intent(this, WelcomeActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "❌ Sign up failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Nút quay lại
        ivBackSignUp.setOnClickListener(v -> finish());
    }
}
