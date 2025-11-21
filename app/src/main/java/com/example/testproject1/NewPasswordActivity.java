package com.example.testproject1;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class NewPasswordActivity extends AppCompatActivity {

    // 🔹 Views
    private EditText etNewPassword, etConfirmPassword;
    private Button btnResetPassword;
    private ImageView ivBack, ivToggleNewPassword, ivToggleConfirmPassword;

    // 🔹 Firebase
    private FirebaseAuth auth;

    // 🔹 Data
    private boolean isFromChangePassword = false;
    private String userEmail;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_newpassword);

        setupWindowInsets();
        initFirebase();
        getIntentData();
        initViews();
        setupClickListeners();
    }

    // ============================================================
    // 🔹 1. Cấu hình khoảng cách giao diện với thanh trạng thái
    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // ============================================================
    // 🔹 2. Khởi tạo Firebase
    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
    }

    // ============================================================
    // 🔹 3. Nhận dữ liệu từ Intent
    private void getIntentData() {
        Intent intent = getIntent();
        isFromChangePassword = intent.getBooleanExtra("fromChangePassword", false);
        userEmail = intent.getStringExtra("email");
    }

    // ============================================================
    // 🔹 4. Ánh xạ views
    private void initViews() {
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        ivBack = findViewById(R.id.ivBackNewPassword);
        ivToggleNewPassword = findViewById(R.id.ivToggleNewPassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
    }

    // ============================================================
    // 🔹 5. Thiết lập click listeners
    private void setupClickListeners() {
        // Back button
        ivBack.setOnClickListener(v -> finish());

        // Toggle password visibility
        ivToggleNewPassword.setOnClickListener(v -> togglePasswordVisibility(etNewPassword, ivToggleNewPassword, isNewPasswordVisible));
        ivToggleConfirmPassword.setOnClickListener(v -> togglePasswordVisibility(etConfirmPassword, ivToggleConfirmPassword, isConfirmPasswordVisible));

        // Reset password button
        btnResetPassword.setOnClickListener(v -> handlePasswordReset());

        // Handle back gesture
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    // ============================================================
    // 🔹 6. Toggle password visibility
    private void togglePasswordVisibility(EditText editText, ImageView toggleIcon, boolean isVisible) {
        if (isVisible) {
            // Hide password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleIcon.setImageResource(android.R.drawable.ic_menu_view); // Eye closed icon
        } else {
            // Show password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleIcon.setImageResource(android.R.drawable.ic_menu_view); // Eye open icon
        }
        editText.setSelection(editText.getText().length());

        // Update visibility state
        if (editText == etNewPassword) {
            isNewPasswordVisible = !isVisible;
        } else {
            isConfirmPasswordVisible = !isVisible;
        }
    }

    // ============================================================
    // 🔹 7. Xử lý reset mật khẩu
    private void handlePasswordReset() {
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(newPassword, confirmPassword)) {
            return;
        }

        // Disable button để tránh click nhiều lần
        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("UPDATING PASSWORD...");

        // Thực hiện cập nhật mật khẩu
        updatePassword(newPassword);
    }

    // ============================================================
    // 🔹 8. Validate inputs
    private boolean validateInputs(String newPassword, String confirmPassword) {
        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "Please enter new password", Toast.LENGTH_SHORT).show();
            etNewPassword.requestFocus();
            return false;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            etNewPassword.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show();
            etConfirmPassword.requestFocus();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    // ============================================================
    // 🔹 9. Cập nhật mật khẩu
    private void updatePassword(String newPassword) {
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                        navigateBack();
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("NEW_PASSWORD", "Error updating password", e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        enableResetButton();
                    });
        } else {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            enableResetButton();
        }
    }

    // ============================================================
    // 🔹 10. Enable lại Reset button
    private void enableResetButton() {
        btnResetPassword.setEnabled(true);
        btnResetPassword.setText("RESET PASSWORD");
    }

    // ============================================================
    // 🔹 11. Navigate back dựa trên nguồn gọi
    private void navigateBack() {
        if (isFromChangePassword) {
            // Quay về ProfileUpdateActivity
            Intent intent = new Intent(this, ProfileUpdateActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else {
            // Chuyển về LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    // ============================================================
    // 🔹 12. Clear sensitive data khi activity bị destroy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (etNewPassword != null) {
            etNewPassword.setText("");
        }
        if (etConfirmPassword != null) {
            etConfirmPassword.setText("");
        }
    }
}
