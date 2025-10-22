//package com.example.testproject1; // Thay bằng package của bạn
//
//import android.content.Intent; // Cần import Intent
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button; // Cần import Button
//import android.widget.ImageView;
//import androidx.appcompat.app.AppCompatActivity;
//
//public class SignUpActivity extends AppCompatActivity {
//
//    Button btnSignUp; // Khai báo nút Sign Up
//    ImageView ivBackSignUp; // Khai báo nút back (từ lần trước)
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_signup);
//
//        // Ánh xạ các view từ layout XML
//        btnSignUp = findViewById(R.id.btnSignUp);
//        ivBackSignUp = findViewById(R.id.ivBackSignUp);
//
//        // === GÁN SỰ KIỆN CLICK CHO NÚT "SIGN UP" ===
//        btnSignUp.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Đây là nơi bạn xử lý logic đăng ký (lấy text, kiểm tra, lưu database...)
//                // Sau khi xử lý xong, chúng ta chuyển sang trang Login
//
//                // 1. Tạo một Intent để mở LoginActivity
//                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
//
//                // 2. Bắt đầu Activity mới (chuyển trang)
//                startActivity(intent);
//
//                // 3. (Khuyên dùng) Đóng Activity hiện tại
//                // Điều này ngăn người dùng nhấn "Back" từ trang Login
//                // và quay lại trang Sign Up một lần nữa.
//                finish();
//            }
//        });
//
//        // Sự kiện click cho nút back (giữ nguyên từ yêu cầu trước)
//        ivBackSignUp.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Đóng trang Sign Up và quay lại trang Welcome
//                finish();
//            }
//        });
//    }
//}
package com.example.testproject1; // Thay bằng package của bạn

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignUpActivity extends AppCompatActivity {

    EditText etSignUpName, etSignUpEmail, etSignUpPassword;
    Button btnSignUp;
    ImageView ivBackSignUp;

    // Khai báo đối tượng FirebaseAuth
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ các view
        etSignUpName = findViewById(R.id.etSignUpName);
        etSignUpEmail = findViewById(R.id.etSignUpEmail);
        etSignUpPassword = findViewById(R.id.etSignUpPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        ivBackSignUp = findViewById(R.id.ivBackSignUp);

        // Sự kiện click cho nút "SIGN IN" (Sign Up)
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Gọi hàm đăng ký
                registerUser();
            }
        });

        // Sự kiện click cho nút back
        ivBackSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void registerUser() {
        String name = etSignUpName.getText().toString().trim();
        String email = etSignUpEmail.getText().toString().trim();
        String password = etSignUpPassword.getText().toString().trim();

        // --- Kiểm tra đầu vào ---
        if (TextUtils.isEmpty(name)) {
            etSignUpName.setError("Vui lòng nhập tên của bạn");
            etSignUpName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etSignUpEmail.setError("Vui lòng nhập email");
            etSignUpEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etSignUpPassword.setError("Vui lòng nhập mật khẩu");
            etSignUpPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etSignUpPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etSignUpPassword.requestFocus();
            return;
        }
        // --- Kết thúc kiểm tra ---

        // Hiển thị thông báo (ví dụ: ProgressBar) - Tùy chọn

        // Tạo người dùng mới với Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Đăng ký thành công
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Cập nhật tên hiển thị cho user
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            if (user != null) {
                                user.updateProfile(profileUpdates);
                            }

                            Toast.makeText(SignUpActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                            // Chuyển sang trang Login (như yêu cầu của bạn)
                            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                            finish(); // Đóng activity này

                        } else {
                            // Đăng ký thất bại
                            Toast.makeText(SignUpActivity.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}