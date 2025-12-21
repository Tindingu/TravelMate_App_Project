package com.example.travelmate.trips;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travelmate.chat.ChatGroup;
import com.example.travelmate.chat.ChatListActivity;
import com.example.travelmate.chat.CreateGroupActivity;
import com.example.travelmate.common.User;
import com.example.travelmate.R;
import com.example.travelmate.chat.GroupMemberAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class GroupDashboardActivity extends AppCompatActivity {

    private ImageView btnBack, ivGroupAvatar, btnEditGroupName;
    private TextView tvGroupName, btnLeaveGroup, btnDeleteGroup;
    private LinearLayout btnSearchMessages, btnMuteNotifications, btnPinConversation, btnAddMembers;
    private RecyclerView rvMembers, rvBulletins, rvMedia, rvFilesLinks;
    private TextView tabFiles, tabLinks;

    private String groupId;
    private String groupName;
    private ChatGroup currentGroup;
    private boolean isCurrentUserAdmin = false;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    private GroupMemberAdapter memberAdapter;
    private List<User> membersList;

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
        btnDeleteGroup = findViewById(R.id.btnDeleteGroup);

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

        // Initialize members list
        membersList = new ArrayList<>();

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

                        // Kiểm tra quyền admin
                        List<String> adminIds = currentGroup.getAdminIds();
                        if (adminIds != null && currentUser != null) {
                            isCurrentUserAdmin = adminIds.contains(currentUser.getUid());
                        }

                        updateUI();
                        loadMembers();
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

        // Hiển thị nút xóa nhóm nếu là admin
        if (isCurrentUserAdmin) {
            btnDeleteGroup.setVisibility(View.VISIBLE);
        } else {
            btnDeleteGroup.setVisibility(View.GONE);
        }
    }

    private void loadMembers() {
        if (currentGroup == null || currentGroup.getMemberIds() == null) return;

        membersList.clear();
        List<String> memberIds = currentGroup.getMemberIds();

        for (String memberId : memberIds) {
            db.collection("users").document(memberId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                user.setId(doc.getId());
                                membersList.add(user);

                                // Cập nhật adapter
                                if (memberAdapter == null) {
                                    List<String> adminIds = currentGroup.getAdminIds() != null ?
                                            currentGroup.getAdminIds() : new ArrayList<>();
                                    memberAdapter = new GroupMemberAdapter(
                                            this, membersList, adminIds,
                                            currentUser.getUid(), isCurrentUserAdmin);
                                    memberAdapter.setOnRemoveMemberClickListener(this::showRemoveMemberDialog);
                                    rvMembers.setAdapter(memberAdapter);
                                } else {
                                    memberAdapter.updateMembers(membersList);
                                }
                            }
                        }
                    });
        }
    }

    private void showRemoveMemberDialog(User member, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa thành viên")
                .setMessage("Bạn có chắc muốn xóa " + member.getName() + " khỏi nhóm?")
                .setPositiveButton("Xóa", (dialog, which) -> removeMember(member, position))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void removeMember(User member, int position) {
        if (currentGroup == null || member == null) return;

        // Xóa member khỏi memberIds
        db.collection("chat_groups")
                .document(groupId)
                .update("memberIds", FieldValue.arrayRemove(member.getId()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa " + member.getName() + " khỏi nhóm",
                            Toast.LENGTH_SHORT).show();
                    memberAdapter.removeMember(position);

                    // Gửi system message
                    sendSystemMessage(currentUser.getDisplayName() + " đã xóa " +
                            member.getName() + " khỏi nhóm");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendSystemMessage(String content) {
        java.util.Map<String, Object> message = new java.util.HashMap<>();
        message.put("groupId", groupId);
        message.put("senderId", "system");
        message.put("senderName", "Hệ thống");
        message.put("senderAvatar", "");
        message.put("content", "👥 " + content);
        message.put("type", "system");
        message.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("chat_groups")
                .document(groupId)
                .collection("messages")
                .add(message);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEditGroupName.setOnClickListener(v -> showEditGroupNameDialog());

        btnSearchMessages.setOnClickListener(v -> 
            Toast.makeText(this, "Tính năng tìm kiếm tin nhắn đang phát triển", 
                         Toast.LENGTH_SHORT).show());

        btnMuteNotifications.setOnClickListener(v -> toggleMuteNotifications());

        btnPinConversation.setOnClickListener(v -> togglePinConversation());

        btnAddMembers.setOnClickListener(v -> openAddMembersActivity());

        btnLeaveGroup.setOnClickListener(v -> showLeaveGroupDialog());

        btnDeleteGroup.setOnClickListener(v -> showDeleteGroupDialog());

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

    private void showDeleteGroupDialog() {
        if (!isCurrentUserAdmin) {
            Toast.makeText(this, "Chỉ admin mới có thể xóa nhóm", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xóa nhóm")
                .setMessage("Bạn có chắc muốn xóa nhóm này? Tất cả tin nhắn sẽ bị xóa vĩnh viễn.")
                .setPositiveButton("Xóa", (dialog, which) -> deleteGroup())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteGroup() {
        if (currentGroup == null) return;

        // Xóa tất cả messages trong subcollection
        db.collection("chat_groups")
                .document(groupId)
                .collection("messages")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }

                    // Xóa tất cả tasks trong subcollection
                    db.collection("chat_groups")
                            .document(groupId)
                            .collection("tasks")
                            .get()
                            .addOnSuccessListener(taskSnapshot -> {
                                for (DocumentSnapshot doc : taskSnapshot.getDocuments()) {
                                    doc.getReference().delete();
                                }

                                // Cuối cùng xóa group document
                                db.collection("chat_groups")
                                        .document(groupId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Đã xóa nhóm", Toast.LENGTH_SHORT).show();
                                            // Quay về ChatListActivity
                                            Intent intent = new Intent(this, ChatListActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            startActivity(intent);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Lỗi xóa nhóm: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            });
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

    private void openAddMembersActivity() {
        // Mở CreateGroupActivity để tạo nhóm mới hoặc thêm thành viên
        Intent intent = new Intent(this, CreateGroupActivity.class);
        startActivity(intent);
    }
}
