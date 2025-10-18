package com.example.testproject1;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView; // Import ImageView
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    ImageView ivBackLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ivBackLogin = findViewById(R.id.ivBackLogin);

        ivBackLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Quay trở lại màn hình trước đó
                finish();
            }
        });
    }
}