package com.example.travelmate.profile;

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

import com.example.travelmate.R;
import com.example.travelmate.auth.WelcomeActivity;
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
    private String currentDisplayName;

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
        loadCurrentDisplayName();
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
    // 🔹 5. Load display name hiện tại từ intent
    private void loadCurrentDisplayName() {
        Intent intent = getIntent();
        currentDisplayName = intent.getStringExtra("current_username");

        if (currentDisplayName != null) {
            etUsername.setText(currentDisplayName);
            etUsername.setSelection(currentDisplayName.length()); // Đặt con trỏ ở cuối
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
            updateDisplayName();
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
    // 🔹 7. Cập nhật display name
    private void updateDisplayName() {
        String newDisplayName = etUsername.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(newDisplayName)) {
            Toast.makeText(this, "Tên hiển thị không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newDisplayName.length() < 3) {
            Toast.makeText(this, "Tên hiển thị phải có ít nhất 3 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newDisplayName.length() > 50) {
            Toast.makeText(this, "Tên hiển thị không được quá 50 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        // Nếu display name không thay đổi
        if (newDisplayName.equals(currentDisplayName)) {
            Toast.makeText(this, "Tên hiển thị không thay đổi", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // Disable button để tránh click nhiều lần
        btnDone.setEnabled(false);
        btnDone.setText("Đang cập nhật...");

        // Cập nhật lên Firestore
        updateDisplayNameInFirestore(newDisplayName);
    }

    // ============================================================
    // 🔹 8. Cập nhật display name lên Firestore
    private void updateDisplayNameInFirestore(String newDisplayName) {
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
                        // Document đã tồn tại, chỉ update name (displayName)
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("name", newDisplayName);

                        db.collection("users").document(userId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Tên hiển thị đã được cập nhật thành công", Toast.LENGTH_SHORT).show();
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("username", newDisplayName);
                                    setResult(RESULT_OK, resultIntent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("CHANGE_USERNAME", "Lỗi cập nhật tên hiển thị", e);
                                    Toast.makeText(this, "Lỗi cập nhật tên hiển thị: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    enableDoneButton();
                                });
                    } else {
                        // Document chưa tồn tại, tạo mới với đủ fields
                        Map<String, Object> newUserData = new HashMap<>();
                        newUserData.put("email", userEmail);
                        newUserData.put("name", newDisplayName);
                        newUserData.put("photoUrl", ""); // Để trống, sẽ được cập nhật sau

                        db.collection("users").document(userId)
                                .set(newUserData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Document tạo mới và tên hiển thị đã được thiết lập", Toast.LENGTH_SHORT).show();
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("username", newDisplayName);
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
