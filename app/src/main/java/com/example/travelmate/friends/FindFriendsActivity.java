package com.example.travelmate.friends;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelmate.R;
import com.example.travelmate.chat.ChatGroup;
import com.example.travelmate.chat.GroupChatActivity;
import com.example.travelmate.friends.FriendRequest;
import com.example.travelmate.common.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FindFriendsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etSearch;

    // Search results
    private LinearLayout layoutSearchResults;
    private TextView tvSearchResultsTitle;
    private RecyclerView rvSearchResults;

    // Friend requests
    private LinearLayout layoutFriendRequests;
    private TextView tvFriendRequestsTitle;
    private ImageView ivExpandRequests;
    private RecyclerView rvFriendRequests;
    private LinearLayout layoutRequestsEmpty;

    // Friends list
    private LinearLayout layoutFriendsList;
    private TextView tvFriendsListTitle;
    private ImageView ivExpandFriends;
    private RecyclerView rvFriendsList;
    private LinearLayout layoutFriendsEmpty;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private UserSearchAdapter searchAdapter;
    private FriendRequestAdapter requestAdapter;
    private FriendListAdapter friendListAdapter;

    private List<User> searchResults;
    private List<FriendRequest> friendRequests;
    private List<User> friendsList;
    private List<String> friendIds;
    private List<String> pendingRequestIds;

    private boolean isRequestsExpanded = true;
    private boolean isFriendsExpanded = true;

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerViews();
        loadCurrentUserData();
        loadFriendRequests();
        loadFriendsList();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etSearch = findViewById(R.id.etSearch);

        // Search results
        layoutSearchResults = findViewById(R.id.layoutSearchResults);
        tvSearchResultsTitle = findViewById(R.id.tvSearchResultsTitle);
        rvSearchResults = findViewById(R.id.rvSearchResults);

        // Friend requests
        layoutFriendRequests = findViewById(R.id.layoutFriendRequests);
        tvFriendRequestsTitle = findViewById(R.id.tvFriendRequestsTitle);
        ivExpandRequests = findViewById(R.id.ivExpandRequests);
        rvFriendRequests = findViewById(R.id.rvFriendRequests);
        layoutRequestsEmpty = findViewById(R.id.layoutRequestsEmpty);

        // Friends list
        layoutFriendsList = findViewById(R.id.layoutFriendsList);
        tvFriendsListTitle = findViewById(R.id.tvFriendsListTitle);
        ivExpandFriends = findViewById(R.id.ivExpandFriends);
        rvFriendsList = findViewById(R.id.rvFriendsList);
        layoutFriendsEmpty = findViewById(R.id.layoutFriendsEmpty);

        searchResults = new ArrayList<>();
        friendRequests = new ArrayList<>();
        friendsList = new ArrayList<>();
        friendIds = new ArrayList<>();
        pendingRequestIds = new ArrayList<>();
    }

    private void setupRecyclerViews() {
        // Search adapter
        searchAdapter = new UserSearchAdapter(this, searchResults, friendIds, pendingRequestIds);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(searchAdapter);

        searchAdapter.setOnAddFriendClickListener((user, position) -> sendFriendRequest(user));

        // Friend requests adapter
        requestAdapter = new FriendRequestAdapter(this, friendRequests, true);
        rvFriendRequests.setLayoutManager(new LinearLayoutManager(this));
        rvFriendRequests.setAdapter(requestAdapter);

        requestAdapter.setOnRequestActionListener(new FriendRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(FriendRequest request, int position) {
                acceptFriendRequest(request, position);
            }

            @Override
            public void onReject(FriendRequest request, int position) {
                rejectFriendRequest(request, position);
            }

            @Override
            public void onCancel(FriendRequest request, int position) {}
        });

        // Friends list adapter
        friendListAdapter = new FriendListAdapter(this, friendsList);
        rvFriendsList.setLayoutManager(new LinearLayoutManager(this));
        rvFriendsList.setAdapter(friendListAdapter);

        friendListAdapter.setOnFriendActionListener(new FriendListAdapter.OnFriendActionListener() {
            @Override
            public void onStartChat(User friend, int position) {
                startPrivateChat(friend);
            }

            @Override
            public void onAddToGroup(User friend, int position) {
                showSelectGroupDialog(friend);
            }

            @Override
            public void onRemoveFriend(User friend, int position) {
                showRemoveFriendDialog(friend, position);
            }
        });
    }

    private void loadCurrentUserData() {
        String userId = currentUser.getUid();

        // Load danh sách bạn bè IDs
        db.collection("users").document(userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    User user = value.toObject(User.class);
                    if (user != null && user.getFriendIds() != null) {
                        friendIds.clear();
                        friendIds.addAll(user.getFriendIds());
                        searchAdapter.updateFriendIds(friendIds);

                        // Load thông tin chi tiết bạn bè
                        loadFriendsDetails();
                    }
                });

        // Load danh sách lời mời đang chờ (đã gửi đi)
        db.collection("friend_requests")
                .whereEqualTo("senderId", userId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    pendingRequestIds.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        String receiverId = doc.getString("receiverId");
                        if (receiverId != null) {
                            pendingRequestIds.add(receiverId);
                        }
                    }
                    searchAdapter.updatePendingRequestIds(pendingRequestIds);
                });
    }

    private void loadFriendsDetails() {
        friendsList.clear();

        if (friendIds.isEmpty()) {
            updateFriendsListUI();
            return;
        }

        for (String friendId : friendIds) {
            db.collection("users").document(friendId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            User friend = doc.toObject(User.class);
                            if (friend != null) {
                                friend.setId(doc.getId());

                                // Kiểm tra trùng lặp
                                boolean exists = false;
                                for (User f : friendsList) {
                                    if (f.getId().equals(friend.getId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    friendsList.add(friend);
                                }

                                updateFriendsListUI();
                            }
                        }
                    });
        }
    }

    private void loadFriendRequests() {
        db.collection("friend_requests")
                .whereEqualTo("receiverId", currentUser.getUid())
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    friendRequests.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            FriendRequest request = doc.toObject(FriendRequest.class);
                            request.setId(doc.getId());
                            friendRequests.add(request);
                        }
                    }

                    updateFriendRequestsUI();
                });
    }

    private void loadFriendsList() {
        // Đã được load trong loadCurrentUserData -> loadFriendsDetails
    }

    private void updateFriendRequestsUI() {
        int count = friendRequests.size();
        tvFriendRequestsTitle.setText("Lời mời kết bạn (" + count + ")");

        if (count == 0) {
            rvFriendRequests.setVisibility(View.GONE);
            layoutRequestsEmpty.setVisibility(isRequestsExpanded ? View.VISIBLE : View.GONE);
        } else {
            rvFriendRequests.setVisibility(isRequestsExpanded ? View.VISIBLE : View.GONE);
            layoutRequestsEmpty.setVisibility(View.GONE);
            requestAdapter.updateRequests(friendRequests);
        }
    }

    private void updateFriendsListUI() {
        int count = friendsList.size();
        tvFriendsListTitle.setText("Bạn bè (" + count + ")");

        if (count == 0) {
            rvFriendsList.setVisibility(View.GONE);
            layoutFriendsEmpty.setVisibility(isFriendsExpanded ? View.VISIBLE : View.GONE);
        } else {
            rvFriendsList.setVisibility(isFriendsExpanded ? View.VISIBLE : View.GONE);
            layoutFriendsEmpty.setVisibility(View.GONE);
            friendListAdapter.updateFriends(friendsList);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Toggle expand/collapse friend requests
        tvFriendRequestsTitle.setOnClickListener(v -> toggleFriendRequests());
        ivExpandRequests.setOnClickListener(v -> toggleFriendRequests());

        // Toggle expand/collapse friends list
        tvFriendsListTitle.setOnClickListener(v -> toggleFriendsList());
        ivExpandFriends.setOnClickListener(v -> toggleFriendsList());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {
                    String query = s.toString().trim();
                    if (query.length() >= 2) {
                        searchUsers(query);
                    } else {
                        hideSearchResults();
                    }
                };

                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void toggleFriendRequests() {
        isRequestsExpanded = !isRequestsExpanded;

        float fromDegree = isRequestsExpanded ? 180f : 0f;
        float toDegree = isRequestsExpanded ? 0f : 180f;
        RotateAnimation rotate = new RotateAnimation(fromDegree, toDegree,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(200);
        rotate.setFillAfter(true);
        ivExpandRequests.startAnimation(rotate);

        updateFriendRequestsUI();
    }

    private void toggleFriendsList() {
        isFriendsExpanded = !isFriendsExpanded;

        float fromDegree = isFriendsExpanded ? 180f : 0f;
        float toDegree = isFriendsExpanded ? 0f : 180f;
        RotateAnimation rotate = new RotateAnimation(fromDegree, toDegree,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(200);
        rotate.setFillAfter(true);
        ivExpandFriends.startAnimation(rotate);

        updateFriendsListUI();
    }

    private void searchUsers(String query) {
        String lowerQuery = query.toLowerCase();

        db.collection("users")
                .orderBy("name")
                .startAt(lowerQuery)
                .endAt(lowerQuery + "\uf8ff")
                .limit(20)
                .get()
                .addOnSuccessListener(nameResults -> {
                    List<User> foundUsers = new ArrayList<>();

                    for (DocumentSnapshot doc : nameResults.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setId(doc.getId());
                            if (!user.getId().equals(currentUser.getUid())) {
                                foundUsers.add(user);
                            }
                        }
                    }

                    db.collection("users")
                            .orderBy("email")
                            .startAt(lowerQuery)
                            .endAt(lowerQuery + "\uf8ff")
                            .limit(20)
                            .get()
                            .addOnSuccessListener(emailResults -> {
                                for (DocumentSnapshot doc : emailResults.getDocuments()) {
                                    User user = doc.toObject(User.class);
                                    if (user != null) {
                                        user.setId(doc.getId());
                                        if (!user.getId().equals(currentUser.getUid()) &&
                                            !containsUser(foundUsers, user.getId())) {
                                            foundUsers.add(user);
                                        }
                                    }
                                }
                                showSearchResults(foundUsers);
                            })
                            .addOnFailureListener(e -> showSearchResults(foundUsers));
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Lỗi tìm kiếm: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private boolean containsUser(List<User> users, String userId) {
        for (User user : users) {
            if (user.getId().equals(userId)) return true;
        }
        return false;
    }

    private void showSearchResults(List<User> users) {
        searchResults.clear();
        searchResults.addAll(users);
        searchAdapter.updateUsers(searchResults);

        if (users.isEmpty()) {
            tvSearchResultsTitle.setText("Không tìm thấy người dùng");
        } else {
            tvSearchResultsTitle.setText("Kết quả tìm kiếm (" + users.size() + ")");
        }

        layoutSearchResults.setVisibility(View.VISIBLE);
    }

    private void hideSearchResults() {
        searchResults.clear();
        searchAdapter.updateUsers(searchResults);
        layoutSearchResults.setVisibility(View.GONE);
    }

    private void sendFriendRequest(User targetUser) {
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String senderName = doc.getString("name");
                    String senderEmail = doc.getString("email");
                    String senderAvatar = doc.getString("avatarUrl");

                    if (senderName == null) senderName = currentUser.getDisplayName();
                    if (senderEmail == null) senderEmail = currentUser.getEmail();

                    FriendRequest request = new FriendRequest(
                            currentUser.getUid(), senderName, senderEmail, senderAvatar,
                            targetUser.getId(), targetUser.getName(),
                            targetUser.getEmail(), targetUser.getAvatarUrl()
                    );

                    db.collection("friend_requests")
                            .add(request)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "Đã gửi lời mời kết bạn", Toast.LENGTH_SHORT).show();
                                searchAdapter.addPendingRequest(targetUser.getId());
                            })
                            .addOnFailureListener(e ->
                                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
    }

    private void acceptFriendRequest(FriendRequest request, int position) {
        String senderId = request.getSenderId();
        String receiverId = request.getReceiverId();

        db.collection("friend_requests").document(request.getId())
                .update("status", "accepted", "updatedAt", Timestamp.now())
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(senderId)
                            .update("friendIds", FieldValue.arrayUnion(receiverId));

                    db.collection("users").document(receiverId)
                            .update("friendIds", FieldValue.arrayUnion(senderId));

                    createNewFriendNotification(senderId, receiverId, request.getReceiverName());

                    Toast.makeText(this, "Đã chấp nhận lời mời kết bạn", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void rejectFriendRequest(FriendRequest request, int position) {
        db.collection("friend_requests").document(request.getId())
                .update("status", "rejected", "updatedAt", Timestamp.now())
                .addOnSuccessListener(aVoid ->
                    Toast.makeText(this, "Đã từ chối lời mời", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createNewFriendNotification(String recipientId, String friendId, String friendName) {
        java.util.Map<String, Object> notification = new java.util.HashMap<>();
        notification.put("recipientId", recipientId);
        notification.put("type", "friend_accepted");
        notification.put("title", "Bạn mới");
        notification.put("message", friendName + " đã chấp nhận lời mời kết bạn của bạn");
        notification.put("friendId", friendId);
        notification.put("friendName", friendName);
        notification.put("isRead", false);
        notification.put("createdAt", Timestamp.now());

        db.collection("notifications").add(notification);
    }

    // ========== XỬ LÝ THÊM VÀO NHÓM ==========

    private void showSelectGroupDialog(User friend) {
        // Load danh sách nhóm của user
        db.collection("chat_groups")
                .whereArrayContains("memberIds", currentUser.getUid())
                .whereEqualTo("isGroup", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ChatGroup> groups = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ChatGroup group = doc.toObject(ChatGroup.class);
                        if (group != null) {
                            group.setId(doc.getId());
                            // Chỉ hiển thị nhóm mà bạn bè chưa tham gia
                            if (group.getMemberIds() == null ||
                                !group.getMemberIds().contains(friend.getId())) {
                                groups.add(group);
                            }
                        }
                    }

                    if (groups.isEmpty()) {
                        Toast.makeText(this, "Không có nhóm nào có thể thêm " + friend.getName(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    showGroupSelectionDialog(groups, friend);
                });
    }

    private void showGroupSelectionDialog(List<ChatGroup> groups, User friend) {
        String[] groupNames = new String[groups.size()];
        for (int i = 0; i < groups.size(); i++) {
            groupNames[i] = groups.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Thêm " + friend.getName() + " vào nhóm")
                .setItems(groupNames, (dialog, which) -> {
                    ChatGroup selectedGroup = groups.get(which);
                    addFriendToGroup(friend, selectedGroup);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void addFriendToGroup(User friend, ChatGroup group) {
        db.collection("chat_groups").document(group.getId())
                .update("memberIds", FieldValue.arrayUnion(friend.getId()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã thêm " + friend.getName() + " vào " + group.getName(),
                            Toast.LENGTH_SHORT).show();

                    // Gửi system message
                    sendGroupSystemMessage(group.getId(),
                            currentUser.getDisplayName() + " đã thêm " + friend.getName() + " vào nhóm");
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ========== XỬ LÝ CHAT RIÊNG 1-1 ==========

    private void startPrivateChat(User friend) {
        String currentUserId = currentUser.getUid();
        String friendId = friend.getId();

        // Tạo ID duy nhất cho cuộc trò chuyện 1-1 (sắp xếp theo thứ tự alphabet)
        String chatId = currentUserId.compareTo(friendId) < 0
                ? currentUserId + "_" + friendId
                : friendId + "_" + currentUserId;

        // Kiểm tra xem đã có cuộc trò chuyện chưa
        db.collection("chat_groups").document(chatId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Đã có cuộc trò chuyện, mở trực tiếp
                        openPrivateChat(chatId, friend.getName());
                    } else {
                        // Chưa có, tạo mới
                        createPrivateChat(chatId, friend);
                    }
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createPrivateChat(String chatId, User friend) {
        String currentUserId = currentUser.getUid();

        // Lấy thông tin user hiện tại
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    String currentUserName = doc.getString("name");
                    if (currentUserName == null) {
                        currentUserName = currentUser.getDisplayName();
                    }

                    // Tạo danh sách thành viên
                    java.util.List<String> memberIds = new java.util.ArrayList<>();
                    memberIds.add(currentUserId);
                    memberIds.add(friend.getId());

                    // Tạo chat group cho chat 1-1
                    java.util.Map<String, Object> chatData = new java.util.HashMap<>();
                    chatData.put("name", friend.getName()); // Tên hiển thị là tên bạn bè
                    chatData.put("createdBy", currentUserId);
                    chatData.put("memberIds", memberIds);
                    chatData.put("createdAt", Timestamp.now());
                    chatData.put("isGroup", false); // Đánh dấu là chat 1-1
                    chatData.put("isArchived", false);
                    chatData.put("isMuted", false);
                    chatData.put("isPinned", false);
                    // Lưu thông tin cả 2 user để hiển thị đúng tên
                    chatData.put("user1Id", currentUserId);
                    chatData.put("user1Name", currentUserName);
                    chatData.put("user2Id", friend.getId());
                    chatData.put("user2Name", friend.getName());
                    chatData.put("user1Avatar", doc.getString("avatarUrl"));
                    chatData.put("user2Avatar", friend.getAvatarUrl());

                    // Lưu với ID cố định
                    db.collection("chat_groups").document(chatId)
                            .set(chatData)
                            .addOnSuccessListener(aVoid -> {
                                openPrivateChat(chatId, friend.getName());
                            })
                            .addOnFailureListener(e ->
                                Toast.makeText(this, "Lỗi tạo cuộc trò chuyện: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                });
    }

    private void openPrivateChat(String chatId, String friendName) {
        Intent intent = new Intent(this, GroupChatActivity.class);
        intent.putExtra("groupId", chatId);
        intent.putExtra("groupName", friendName);
        intent.putExtra("isPrivateChat", true);
        startActivity(intent);
    }

    private void sendGroupSystemMessage(String groupId, String content) {
        java.util.Map<String, Object> message = new java.util.HashMap<>();
        message.put("groupId", groupId);
        message.put("senderId", "system");
        message.put("senderName", "Hệ thống");
        message.put("senderAvatar", "");
        message.put("content", "👥 " + content);
        message.put("type", "system");
        message.put("timestamp", Timestamp.now());

        db.collection("chat_groups").document(groupId)
                .collection("messages").add(message);
    }

    // ========== XỬ LÝ XÓA BẠN BÈ ==========

    private void showRemoveFriendDialog(User friend, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bạn bè")
                .setMessage("Bạn có chắc muốn xóa " + friend.getName() + " khỏi danh sách bạn bè?")
                .setPositiveButton("Xóa", (dialog, which) -> removeFriend(friend, position))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void removeFriend(User friend, int position) {
        String currentUserId = currentUser.getUid();
        String friendId = friend.getId();

        // Xóa khỏi friendIds của cả 2 user
        db.collection("users").document(currentUserId)
                .update("friendIds", FieldValue.arrayRemove(friendId))
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(friendId)
                            .update("friendIds", FieldValue.arrayRemove(currentUserId));

                    Toast.makeText(this, "Đã xóa " + friend.getName() + " khỏi danh sách bạn bè",
                            Toast.LENGTH_SHORT).show();

                    // Cập nhật UI
                    friendIds.remove(friendId);
                    friendListAdapter.removeFriend(position);
                    updateFriendsListUI();
                    searchAdapter.updateFriendIds(friendIds);
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}

