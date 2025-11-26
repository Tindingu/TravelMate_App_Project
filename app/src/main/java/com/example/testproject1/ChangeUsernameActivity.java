package com.example.testproject1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ChangeUsernameActivity extends AppCompatActivity {

    // 🔹 Views
    private EditText etUsername;
    private Button btnCancel, btnDone;
    private Toolbar toolbar; // added

    // 🔹 Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    // 🔹 Data
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_changeusername);

        setupWindowInsets();
        initFirebase();
        initViews();
        setupToolbar(); // added
        checkLoginStatus();
        loadCurrentUsername();
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
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();
    }

    // ============================================================
    // 🔹 3. Ánh xạ views
    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        btnCancel = findViewById(R.id.btnCancel);
        btnDone = findViewById(R.id.btnDone);
        toolbar = findViewById(R.id.toolbar); // added
    }

    // ============================================================
    // 🔹 3.1 Thiết lập toolbar
    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                // keep the toolbar title from XML visible (do not disable show title)
            }

            // Handle navigation click
            toolbar.setNavigationOnClickListener(v -> {
                setResult(RESULT_CANCELED);
                finish();
            });
        }
    }

    // ============================================================
    // 🔹 4. Kiểm tra người dùng đăng nhập
    private void checkLoginStatus() {
        if (user == null) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        }
    }

    // ============================================================
    // 🔹 5. Load username hiện tại từ intent
    private void loadCurrentUsername() {
        Intent intent = getIntent();
        currentUsername = intent.getStringExtra("current_username");

        if (currentUsername != null) {
            etUsername.setText(currentUsername);
            etUsername.setSelection(currentUsername.length()); // Đặt con trỏ ở cuối
        }
    }

    // ============================================================
    // 🔹 6. Thiết lập click listeners
    private void setupClickListeners() {
        // Cancel button
        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Done button
        btnDone.setOnClickListener(v -> {
            updateUsername();
        });

        // Handle back gesture
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    // ============================================================
    // 🔹 7. Cập nhật username
    private void updateUsername() {
        String newUsername = etUsername.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(newUsername)) {
            Toast.makeText(this, "Username không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newUsername.length() < 3) {
            Toast.makeText(this, "Username phải có ít nhất 3 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newUsername.length() > 30) {
            Toast.makeText(this, "Username không được quá 30 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra ký tự hợp lệ (chỉ cho phép chữ cái, số, dấu gạch dưới và dấu chấm)
        if (!newUsername.matches("^[a-zA-Z0-9._]+$")) {
            Toast.makeText(this, "Username chỉ được chứa chữ cái, số, dấu gạch dưới và dấu chấm", Toast.LENGTH_LONG).show();
            return;
        }

        // Nếu username không thay đổi
        if (newUsername.equals(currentUsername)) {
            Toast.makeText(this, "Username không thay đổi", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // Disable button để tránh click nhiều lần
        btnDone.setEnabled(false);
        btnDone.setText("Đang cập nhật...");

        // Cập nhật lên Firestore
        updateUsernameInFirestore(newUsername);
    }

    // ============================================================
    // 🔹 8. Cập nhật username lên Firestore
    private void updateUsernameInFirestore(String newUsername) {
        if (user == null) {
            enableDoneButton();
            return;
        }

        String userId = user.getUid();
        String userEmail = user.getEmail() != null ? user.getEmail() : "";

        // Kiểm tra document có tồn tại không
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Document đã tồn tại, chỉ update username
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("username", newUsername);

                        db.collection("users").document(userId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Username đã được cập nhật thành công", Toast.LENGTH_SHORT).show();
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("username", newUsername);
                                    setResult(RESULT_OK, resultIntent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("CHANGE_USERNAME", "Lỗi cập nhật username", e);
                                    Toast.makeText(this, "Lỗi cập nhật username: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    enableDoneButton();
                                });
                    } else {
                        // Document chưa tồn tại, tạo mới với đủ 3 fields
                        Map<String, Object> newUserData = new HashMap<>();
                        newUserData.put("email", userEmail);
                        newUserData.put("phone", ""); // Để trống, sẽ được cập nhật sau
                        newUserData.put("username", newUsername);

                        db.collection("users").document(userId)
                                .set(newUserData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Document tạo mới và username đã được thiết lập", Toast.LENGTH_SHORT).show();
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("username", newUsername);
                                    setResult(RESULT_OK, resultIntent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("CHANGE_USERNAME", "Lỗi tạo document mới", e);
                                    Toast.makeText(this, "Lỗi tạo document: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    enableDoneButton();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CHANGE_USERNAME", "Lỗi kiểm tra document", e);
                    Toast.makeText(this, "Lỗi kiểm tra tài liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    enableDoneButton();
                });
    }

    // ============================================================
    // 🔹 9. Enable lại Done button
    private void enableDoneButton() {
        btnDone.setEnabled(true);
        btnDone.setText("Done");
    }
}
