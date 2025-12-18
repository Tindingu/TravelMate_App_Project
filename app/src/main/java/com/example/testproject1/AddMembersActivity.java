package com.example.testproject1;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddMembersActivity extends AppCompatActivity {

    private ImageView btnBack, btnConfirm;
    private EditText etSearch;
    private RecyclerView rvUsers;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String groupId;
    private List<String> currentMemberIds;
    private List<Map<String, Object>> allUsers;
    private List<Map<String, Object>> filteredUsers;
    private List<String> selectedUserIds;
    private UserSelectionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_members);

        groupId = getIntent().getStringExtra("groupId");
        currentMemberIds = getIntent().getStringArrayListExtra("currentMemberIds");
        if (currentMemberIds == null) currentMemberIds = new ArrayList<>();

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews();
        setupListeners();
        loadUsers();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnConfirm = findViewById(R.id.btnConfirm);
        etSearch = findViewById(R.id.etSearch);
        rvUsers = findViewById(R.id.rvUsers);

        allUsers = new ArrayList<>();
        filteredUsers = new ArrayList<>();
        selectedUserIds = new ArrayList<>();

        adapter = new UserSelectionAdapter(this, filteredUsers, selectedUserIds);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnConfirm.setOnClickListener(v -> addSelectedMembers());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUsers() {
        // Đầu tiên load danh sách bạn bè
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> friendIds = (List<String>) doc.get("friendIds");
                        if (friendIds != null && !friendIds.isEmpty()) {
                            loadFriendsInfo(friendIds);
                        } else {
                            // Nếu không có bạn bè, load tất cả users
                            loadAllUsers();
                        }
                    } else {
                        loadAllUsers();
                    }
                })
                .addOnFailureListener(e -> loadAllUsers());
    }

    private void loadFriendsInfo(List<String> friendIds) {
        allUsers.clear();

        for (String friendId : friendIds) {
            // Bỏ qua nếu đã là thành viên nhóm
            if (currentMemberIds.contains(friendId)) continue;

            db.collection("users").document(friendId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Map<String, Object> user = new HashMap<>();
                            user.put("userId", doc.getId());
                            user.put("name", doc.getString("name"));
                            user.put("email", doc.getString("email"));
                            user.put("avatar", doc.getString("avatarUrl"));
                            allUsers.add(user);

                            filteredUsers.clear();
                            filteredUsers.addAll(allUsers);
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    private void loadAllUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allUsers.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String userId = doc.getId();
                        // Exclude current members and current user
                        if (!currentMemberIds.contains(userId) &&
                            !userId.equals(currentUser.getUid())) {
                            Map<String, Object> user = new HashMap<>();
                            user.put("userId", userId);
                            user.put("name", doc.getString("name"));
                            user.put("email", doc.getString("email"));
                            user.put("avatar", doc.getString("avatarUrl"));
                            allUsers.add(user);
                        }
                    }
                    filteredUsers.addAll(allUsers);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Lỗi tải danh sách: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show());
    }

    private void filterUsers(String query) {
        filteredUsers.clear();
        if (query.isEmpty()) {
            filteredUsers.addAll(allUsers);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Map<String, Object> user : allUsers) {
                String name = (String) user.get("name");
                String email = (String) user.get("email");
                if ((name != null && name.toLowerCase().contains(lowerQuery)) ||
                    (email != null && email.toLowerCase().contains(lowerQuery))) {
                    filteredUsers.add(user);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void addSelectedMembers() {
        if (selectedUserIds.isEmpty()) {
            Toast.makeText(this, "Chọn ít nhất một thành viên", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update memberIds array in Firestore
        db.collection("chat_groups")
                .document(groupId)
                .update("memberIds", FieldValue.arrayUnion(selectedUserIds.toArray()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã thêm " + selectedUserIds.size() + " thành viên",
                            Toast.LENGTH_SHORT).show();

                    // Send system message
                    sendSystemMessage();

                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void sendSystemMessage() {
        StringBuilder names = new StringBuilder();
        for (String userId : selectedUserIds) {
            for (Map<String, Object> user : allUsers) {
                if (userId.equals(user.get("userId"))) {
                    if (names.length() > 0) names.append(", ");
                    names.append(user.get("name"));
                    break;
                }
            }
        }

        Map<String, Object> message = new HashMap<>();
        message.put("groupId", groupId);
        message.put("senderId", "system");
        message.put("senderName", "Hệ thống");
        message.put("senderAvatar", "");
        message.put("content", "👥 " + currentUser.getDisplayName() + " đã thêm " + names + " vào nhóm");
        message.put("type", "system");
        message.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("chat_groups")
                .document(groupId)
                .collection("messages")
                .add(message);
    }
}

