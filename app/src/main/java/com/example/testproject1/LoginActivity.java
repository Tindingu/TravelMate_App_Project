//package com.example.testproject1;
//
//import android.os.Bundle;
//import android.view.View;
//import android.widget.ImageView; // Import ImageView
//import androidx.appcompat.app.AppCompatActivity;
//
//public class LoginActivity extends AppCompatActivity {
//
//    ImageView ivBackLogin;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_login);
//
//        ivBackLogin = findViewById(R.id.ivBackLogin);
//
//        ivBackLogin.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Quay trở lại màn hình trước đó
//                finish();
//            }
//        });
//    }
//}
package com.example.testproject1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.testproject1.ForgotPasswordActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    EditText etLoginEmail, etLoginPassword;
    Button btnLogin;
    ImageView ivBackLogin;

    // Khai báo FirebaseAuth
    private FirebaseAuth mAuth;
    private TextView tvForgotPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ view
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        ivBackLogin = findViewById(R.id.ivBackLogin);
        tvForgotPassword=findViewById(R.id.tvForgotPassword);
        // Sự kiện click nút Login
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Gọi hàm đăng nhập
                loginUser();
            }
        });

        // Sự kiện click nút back
        ivBackLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        // --- Kiểm tra đầu vào ---
        if (TextUtils.isEmpty(email)) {
            etLoginEmail.setError("Vui lòng nhập email");
            etLoginEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etLoginPassword.setError("Vui lòng nhập mật khẩu");
            etLoginPassword.requestFocus();
            return;
        }
        // --- Kết thúc kiểm tra ---

        // Đăng nhập với Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Đăng nhập thành công (giữ nguyên)
                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();

                        } else {
                            // === PHẦN XỬ LÝ LỖI ===
                            try {
                                throw task.getException();
                            }
                            catch (FirebaseAuthInvalidCredentialsException e) {
                                etLoginPassword.setError("Mật khẩu không đúng, vui lòng thử lại.");
                                etLoginPassword.requestFocus();
                            }

                            catch (Exception e) {
                                Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        // Kiểm tra xem người dùng đã đăng nhập từ trước chưa
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if(currentUser != null){
//            // Nếu đã đăng nhập, chuyển thẳng vào Home
//            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            startActivity(intent);
//            finish();
//        }
//    }

}