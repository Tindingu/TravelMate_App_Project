package com.example.testproject1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateGroupActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView btnCreate, tvFriendsTitle;
    private EditText etGroupName;
    private RecyclerView rvFriends;
    private LinearLayout layoutSelectedMembers, layoutEmpty;
    private TextView tvSelectedCount, btnFindFriends;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private UserSelectionAdapter adapter;
    private List<Map<String, Object>> allFriends;
    private List<String> selectedUserIds;
    private ListenerRegistration friendListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        loadFriends();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnCreate = findViewById(R.id.btnCreate);
        etGroupName = findViewById(R.id.etGroupName);
        tvFriendsTitle = findViewById(R.id.tvFriendsTitle);
        rvFriends = findViewById(R.id.rvFriends);
        layoutSelectedMembers = findViewById(R.id.layoutSelectedMembers);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnFindFriends = findViewById(R.id.btnFindFriends);

        allFriends = new ArrayList<>();
        selectedUserIds = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new UserSelectionAdapter(this, allFriends, selectedUserIds);
        adapter.setOnSelectionChangedListener(count -> updateSelectedCount());
        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        rvFriends.setAdapter(adapter);
    }

    private void loadFriends() {
        String userId = currentUser.getUid();

        if (friendListener != null) {
            friendListener.remove(); // tránh listen trùng
        }

        friendListener = db.collection("users")
                .document(userId)
                .addSnapshotListener((snapshot, e) -> {

                    if (e != null || snapshot == null || !snapshot.exists()) {
                        showEmptyState();
                        return;
                    }

                    List<String> friendIds = (List<String>) snapshot.get("friendIds");

                    allFriends.clear();
                    selectedUserIds.clear();
                    adapter.notifyDataSetChanged();

                    if (friendIds == null || friendIds.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    tvFriendsTitle.setText("Chọn thành viên từ bạn bè (" + friendIds.size() + ")");
                    loadFriendsInfo(friendIds);
                });
    }

    private void loadFriendsInfo(List<String> friendIds) {
        final int total = friendIds.size();
        final int[] loaded = {0};

        for (String friendId : friendIds) {
            db.collection("users").document(friendId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        loaded[0]++;

                        if (doc.exists()) {
                            Map<String, Object> friend = new HashMap<>();
                            friend.put("userId", doc.getId());
                            friend.put("name", doc.getString("name"));
                            friend.put("email", doc.getString("email"));
                            friend.put("avatar", doc.getString("avatarUrl"));
                            allFriends.add(friend);
                        }

                        // ✅ CHỈ notify khi load xong hết
                        if (loaded[0] == total) {
                            adapter.notifyDataSetChanged();
                            hideEmptyState();
                        }
                    });
        }
    }


    private void showEmptyState() {
        layoutEmpty.setVisibility(View.VISIBLE);
        rvFriends.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        layoutEmpty.setVisibility(View.GONE);
        rvFriends.setVisibility(View.VISIBLE);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnCreate.setOnClickListener(v -> createGroup());

        btnFindFriends.setOnClickListener(v -> {
            startActivity(new Intent(this, FindFriendsActivity.class));
        });
    }

    private void updateSelectedCount() {
        int count = selectedUserIds.size();
        if (count > 0) {
            layoutSelectedMembers.setVisibility(View.VISIBLE);
            tvSelectedCount.setText(count + " thành viên");
        } else {
            layoutSelectedMembers.setVisibility(View.GONE);
        }
    }

    private void createGroup() {
        String groupName = etGroupName.getText().toString().trim();

        if (groupName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên nhóm", Toast.LENGTH_SHORT).show();
            etGroupName.requestFocus();
            return;
        }

        // Tạo danh sách thành viên (bao gồm cả người tạo)
        List<String> memberIds = new ArrayList<>(selectedUserIds);
        memberIds.add(currentUser.getUid());

        // Tạo danh sách admin (người tạo là admin mặc định)
        List<String> adminIds = new ArrayList<>();
        adminIds.add(currentUser.getUid());

        // Tạo ChatGroup object
        Map<String, Object> groupData = new HashMap<>();
        groupData.put("name", groupName);
        groupData.put("createdBy", currentUser.getUid());
        groupData.put("memberIds", memberIds);
        groupData.put("adminIds", adminIds);
        groupData.put("createdAt", Timestamp.now());
        groupData.put("isGroup", true);
        groupData.put("isArchived", false);
        groupData.put("isMuted", false);
        groupData.put("isPinned", false);

        // Lưu vào Firestore
        db.collection("chat_groups")
                .add(groupData)
                .addOnSuccessListener(documentReference -> {
                    String groupId = documentReference.getId();

                    Toast.makeText(this, "Đã tạo nhóm thành công", Toast.LENGTH_SHORT).show();

                    // Gửi system message
                    sendSystemMessage(groupId, groupName);

                    // Mở chat của nhóm vừa tạo
                    Intent intent = new Intent(this, GroupChatActivity.class);
                    intent.putExtra("groupId", groupId);
                    intent.putExtra("groupName", groupName);
                    startActivity(intent);

                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tạo nhóm: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendSystemMessage(String groupId, String groupName) {
        Map<String, Object> message = new HashMap<>();
        message.put("groupId", groupId);
        message.put("senderId", "system");
        message.put("senderName", "Hệ thống");
        message.put("senderAvatar", "");
        message.put("content", "👥 " + currentUser.getDisplayName() + " đã tạo nhóm \"" + groupName + "\"");
        message.put("type", "system");
        message.put("timestamp", Timestamp.now());

        db.collection("chat_groups")
                .document(groupId)
                .collection("messages")
                .add(message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload friends khi quay lại (có thể đã thêm bạn mới)
//        loadFriends();
    }
}

