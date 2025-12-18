package com.example.testproject1;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PrivateChatDashboardActivity extends AppCompatActivity {

    private ImageView btnBack, ivUserAvatar;
    private TextView tvUserName, tvUserEmail;
    private LinearLayout btnSearchMessages, btnMuteNotifications, btnCreateGroup;
    private RecyclerView rvMedia;
    private TextView btnDeleteChat;

    private String groupId;
    private String groupName;
    private String otherUserId;
    private String otherUserName;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_chat_dashboard);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");

        if (currentUser == null || groupId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy cuộc trò chuyện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadChatInfo();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        ivUserAvatar = findViewById(R.id.ivUserAvatar);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        btnSearchMessages = findViewById(R.id.btnSearchMessages);
        btnMuteNotifications = findViewById(R.id.btnMuteNotifications);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);
        rvMedia = findViewById(R.id.rvMedia);
        btnDeleteChat = findViewById(R.id.btnDeleteChat);

        tvUserName.setText(groupName);
    }

    private void loadChatInfo() {
        db.collection("chat_groups").document(groupId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String user1Id = doc.getString("user1Id");
                    String user2Id = doc.getString("user2Id");
                    String user1Name = doc.getString("user1Name");
                    String user2Name = doc.getString("user2Name");
                    String user1Avatar = doc.getString("user1Avatar");
                    String user2Avatar = doc.getString("user2Avatar");

                    // Xác định người còn lại
                    if (currentUser.getUid().equals(user1Id)) {
                        otherUserId = user2Id;
                        otherUserName = user2Name;
                        loadAvatar(user2Avatar);
                    } else {
                        otherUserId = user1Id;
                        otherUserName = user1Name;
                        loadAvatar(user1Avatar);
                    }

                    tvUserName.setText(otherUserName);

                    // Load email từ users collection
                    if (otherUserId != null) {
                        loadOtherUserInfo();
                    }
                });
    }

    private void loadAvatar(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.avttest)
                    .circleCrop()
                    .into(ivUserAvatar);
        }
    }

    private void loadOtherUserInfo() {
        db.collection("users").document(otherUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String email = doc.getString("email");
                        if (email != null) {
                            tvUserEmail.setText(email);
                        }
                    }
                });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSearchMessages.setOnClickListener(v ->
            Toast.makeText(this, "Tính năng tìm kiếm đang phát triển", Toast.LENGTH_SHORT).show());

        btnMuteNotifications.setOnClickListener(v -> toggleMuteNotifications());

        // Nút tạo nhóm - mở CreateGroupActivity
        btnCreateGroup.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateGroupActivity.class);
            startActivity(intent);
        });

        btnDeleteChat.setOnClickListener(v -> showDeleteChatDialog());
    }

    private void toggleMuteNotifications() {
        db.collection("chat_groups").document(groupId)
                .get()
                .addOnSuccessListener(doc -> {
                    Boolean isMuted = doc.getBoolean("isMuted");
                    boolean newMutedState = isMuted == null || !isMuted;

                    db.collection("chat_groups").document(groupId)
                            .update("isMuted", newMutedState)
                            .addOnSuccessListener(aVoid -> {
                                String message = newMutedState ? "Đã tắt thông báo" : "Đã bật thông báo";
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void showDeleteChatDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa cuộc trò chuyện")
                .setMessage("Bạn có chắc muốn xóa cuộc trò chuyện này? Tất cả tin nhắn sẽ bị xóa.")
                .setPositiveButton("Xóa", (dialog, which) -> deleteChat())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteChat() {
        // Xóa tất cả messages trong subcollection
        db.collection("chat_groups")
                .document(groupId)
                .collection("messages")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }

                    // Xóa chat document
                    db.collection("chat_groups")
                            .document(groupId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đã xóa cuộc trò chuyện", Toast.LENGTH_SHORT).show();

                                // Quay về ChatListActivity
                                Intent intent = new Intent(this, ChatListActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e ->
                                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
    }
}

