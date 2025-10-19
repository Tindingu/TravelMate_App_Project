//package com.example.travelmate_app;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.example.travelmate_app.R;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class RegisterActivityBackend extends AppCompatActivity {
//
//    private EditText etEmail, etPassword;
//    private Button btnSignUp;
//    private ImageView ivBackSignUp;
//
//    private FirebaseAuth auth;
//    private FirebaseFirestore firestore;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_signup);
//
//        // Ánh xạ view
//        etEmail = findViewById(R.id.etSignUpEmail);
//        etPassword = findViewById(R.id.etSignUpPassword);
//        btnSignUp = findViewById(R.id.btnSignUp);
//        ivBackSignUp = findViewById(R.id.ivBackSignUp);
//
//        // Khởi tạo Firebase
//        auth = FirebaseAuth.getInstance();
//        firestore = FirebaseFirestore.getInstance();
//
//        // Khi người dùng nhấn nút "Sign Up"
//        btnSignUp.setOnClickListener(v -> {
//            String email = etEmail.getText().toString().trim();
//            String password = etPassword.getText().toString().trim();
//
//            // Kiểm tra dữ liệu
//            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
//                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // Tạo tài khoản mới
//            auth.createUserWithEmailAndPassword(email, password)
//                    .addOnCompleteListener(task -> {
//                        if (task.isSuccessful()) {
//                            // Lấy user hiện tại
//                            FirebaseUser user = auth.getCurrentUser();
//
//                            // Lưu thêm thông tin vào Firestore (tùy chọn)
//                            if (user != null) {
//                                Map<String, Object> userInfo = new HashMap<>();
//                                userInfo.put("email", user.getEmail());
//                                userInfo.put("uid", user.getUid());
//                                userInfo.put("createdAt", System.currentTimeMillis());
//
//                                firestore.collection("users")
//                                        .document(user.getUid())
//                                        .set(userInfo)
//                                        .addOnSuccessListener(aVoid ->
//                                                Toast.makeText(this, "User saved to Firestore", Toast.LENGTH_SHORT).show()
//                                        )
//                                        .addOnFailureListener(e ->
//                                                Toast.makeText(this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_SHORT).show()
//                                        );
//                            }
//
//                            // Hiển thị thông báo & chuyển trang
//                            Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show();
//                            startActivity(new Intent(this, LoginActivity.class));
//                            finish();
//                        } else {
//                            // Lỗi đăng ký
//                            Toast.makeText(this, "Sign up failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
//                        }
//                    });
//        });
//
//        // Nút quay lại
//        ivBackSignUp.setOnClickListener(v -> finish());
//    }
//}
