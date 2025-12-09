package com.example.testproject1;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testproject1.models.ChatGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class GroupDashboardActivity extends AppCompatActivity {

    private ImageView btnBack, ivGroupAvatar, btnEditGroupName;
    private TextView tvGroupName, btnLeaveGroup;
    private LinearLayout btnSearchMessages, btnMuteNotifications, btnPinConversation, btnAddMembers;
    private RecyclerView rvMembers, rvBulletins, rvMedia, rvFilesLinks;
    private TextView tabFiles, tabLinks;

    private String groupId;
    private String groupName;
    private ChatGroup currentGroup;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_dashboard);

        // Get group info from intent
        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        // Initialize views
        initViews();

        // Load group data
        loadGroupData();

        // Setup listeners
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        ivGroupAvatar = findViewById(R.id.ivGroupAvatar);
        btnEditGroupName = findViewById(R.id.btnEditGroupName);
        tvGroupName = findViewById(R.id.tvGroupName);
        btnLeaveGroup = findViewById(R.id.btnLeaveGroup);

        btnSearchMessages = findViewById(R.id.btnSearchMessages);
        btnMuteNotifications = findViewById(R.id.btnMuteNotifications);
        btnPinConversation = findViewById(R.id.btnPinConversation);
        btnAddMembers = findViewById(R.id.btnAddMembers);

        rvMembers = findViewById(R.id.rvMembers);
        rvBulletins = findViewById(R.id.rvBulletins);
        rvMedia = findViewById(R.id.rvMedia);
        rvFilesLinks = findViewById(R.id.rvFilesLinks);

        tabFiles = findViewById(R.id.tabFiles);
        tabLinks = findViewById(R.id.tabLinks);

        // Set initial group name
        tvGroupName.setText(groupName);

        // Setup RecyclerViews
        setupRecyclerViews();
    }

    private void setupRecyclerViews() {
        // Members RecyclerView
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        // TODO: Set members adapter when implemented

        // Bulletins RecyclerView
        rvBulletins.setLayoutManager(new LinearLayoutManager(this));
        // TODO: Set bulletins adapter when implemented

        // Media RecyclerView (Grid layout)
        rvMedia.setLayoutManager(new GridLayoutManager(this, 3));
        // TODO: Set media adapter when implemented

        // Files & Links RecyclerView
        rvFilesLinks.setLayoutManager(new LinearLayoutManager(this));
        // TODO: Set files/links adapter when implemented
    }

    private void loadGroupData() {
        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy nhóm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("chat_groups")
                .document(groupId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !value.exists()) {
                        Toast.makeText(this, "Lỗi tải thông tin nhóm", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    currentGroup = value.toObject(ChatGroup.class);
                    if (currentGroup != null) {
                        currentGroup.setId(groupId);
                        updateUI();
                    }
                });
    }

    private void updateUI() {
        if (currentGroup == null) return;

        tvGroupName.setText(currentGroup.getName());

        // Load group avatar
        if (currentGroup.getAvatarUrl() != null && !currentGroup.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentGroup.getAvatarUrl())
                    .placeholder(R.drawable.avttest)
                    .into(ivGroupAvatar);
        } else {
            ivGroupAvatar.setImageResource(R.drawable.avttest);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEditGroupName.setOnClickListener(v -> showEditGroupNameDialog());

        btnSearchMessages.setOnClickListener(v -> 
            Toast.makeText(this, "Tính năng tìm kiếm tin nhắn đang phát triển", 
                         Toast.LENGTH_SHORT).show());

        btnMuteNotifications.setOnClickListener(v -> toggleMuteNotifications());

        btnPinConversation.setOnClickListener(v -> togglePinConversation());

        btnAddMembers.setOnClickListener(v -> 
            Toast.makeText(this, "Tính năng thêm thành viên đang phát triển", 
                         Toast.LENGTH_SHORT).show());

        btnLeaveGroup.setOnClickListener(v -> showLeaveGroupDialog());

        // Tab switching for Files/Links
        tabFiles.setOnClickListener(v -> {
            tabFiles.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            tabFiles.setTypeface(null, android.graphics.Typeface.BOLD);
            tabLinks.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tabLinks.setTypeface(null, android.graphics.Typeface.NORMAL);
            // TODO: Load files
        });

        tabLinks.setOnClickListener(v -> {
            tabLinks.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            tabLinks.setTypeface(null, android.graphics.Typeface.BOLD);
            tabFiles.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tabFiles.setTypeface(null, android.graphics.Typeface.NORMAL);
            // TODO: Load links
        });
    }

    private void showEditGroupNameDialog() {
        if (currentGroup == null) return;

        android.widget.EditText input = new android.widget.EditText(this);
        input.setText(currentGroup.getName());

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa tên nhóm")
                .setView(input)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        updateGroupName(newName);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateGroupName(String newName) {
        db.collection("chat_groups")
                .document(groupId)
                .update("name", newName)
                .addOnSuccessListener(aVoid -> 
                    Toast.makeText(this, "Đã cập nhật tên nhóm", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Lỗi cập nhật tên nhóm", Toast.LENGTH_SHORT).show());
    }

    private void toggleMuteNotifications() {
        if (currentGroup == null) return;

        boolean newMutedState = !currentGroup.isMuted();
        
        db.collection("chat_groups")
                .document(groupId)
                .update("muted", newMutedState)
                .addOnSuccessListener(aVoid -> {
                    String message = newMutedState ? "Đã tắt thông báo" : "Đã bật thông báo";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Lỗi cập nhật cài đặt", Toast.LENGTH_SHORT).show());
    }

    private void togglePinConversation() {
        if (currentGroup == null) return;

        boolean newPinnedState = !currentGroup.isPinned();
        
        db.collection("chat_groups")
                .document(groupId)
                .update("pinned", newPinnedState)
                .addOnSuccessListener(aVoid -> {
                    String message = newPinnedState ? "Đã ghim hội thoại" : "Đã bỏ ghim hội thoại";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Lỗi cập nhật cài đặt", Toast.LENGTH_SHORT).show());
    }

    private void showLeaveGroupDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Rời nhóm")
                .setMessage("Bạn có chắc muốn rời khỏi nhóm này?")
                .setPositiveButton("Rời", (dialog, which) -> leaveGroup())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void leaveGroup() {
        if (currentUser == null || currentGroup == null) return;

        String userId = currentUser.getUid();

        // Remove user from memberIds
        if (currentGroup.getMemberIds() != null) {
            currentGroup.getMemberIds().remove(userId);

            db.collection("chat_groups")
                    .document(groupId)
                    .update("memberIds", currentGroup.getMemberIds())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã rời nhóm", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> 
                        Toast.makeText(this, "Lỗi rời nhóm", Toast.LENGTH_SHORT).show());
        }
    }
}
