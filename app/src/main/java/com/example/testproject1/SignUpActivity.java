package com.example.testproject1; // Thay bằng package của bạn

import android.content.Intent; // Cần import Intent
import android.os.Bundle;
import android.view.View;
import android.widget.Button; // Cần import Button
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    Button btnSignUp; // Khai báo nút Sign Up
    ImageView ivBackSignUp; // Khai báo nút back (từ lần trước)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Ánh xạ các view từ layout XML
        btnSignUp = findViewById(R.id.btnSignUp);
        ivBackSignUp = findViewById(R.id.ivBackSignUp);

        // === GÁN SỰ KIỆN CLICK CHO NÚT "SIGN UP" ===
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Đây là nơi bạn xử lý logic đăng ký (lấy text, kiểm tra, lưu database...)
                // Sau khi xử lý xong, chúng ta chuyển sang trang Login

                // 1. Tạo một Intent để mở LoginActivity
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);

                // 2. Bắt đầu Activity mới (chuyển trang)
                startActivity(intent);

                // 3. (Khuyên dùng) Đóng Activity hiện tại
                // Điều này ngăn người dùng nhấn "Back" từ trang Login
                // và quay lại trang Sign Up một lần nữa.
                finish();
            }
        });

        // Sự kiện click cho nút back (giữ nguyên từ yêu cầu trước)
        ivBackSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Đóng trang Sign Up và quay lại trang Welcome
                finish();
            }
        });
    }
}