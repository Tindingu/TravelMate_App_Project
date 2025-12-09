package com.example.testproject1;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testproject1.models.ChatGroup;
import com.example.testproject1.models.Message;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupChatActivity extends AppCompatActivity {

    private TextView tvGroupName, tvMemberCount;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageView btnBack, btnMenu, btnSend, btnLike;
    private ImageView btnImage, btnAttach, btnLocation, btnTask;
    private LinearLayout layoutReplyPreview;
    private TextView tvReplyToName, tvReplyToContent;
    private ImageView btnCancelReply;

    private MessageAdapter adapter;
    private List<Message> messages;
    private String groupId;
    private String groupName;
    private Message replyToMessage;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        // Get group info from intent
        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        // Initialize views
        initViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Load messages
        loadMessages();

        // Load group info
        loadGroupInfo();

        // Setup listeners
        setupListeners();
    }

    private void initViews() {
        tvGroupName = findViewById(R.id.tvGroupName);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnBack = findViewById(R.id.btnBack);
        btnMenu = findViewById(R.id.btnMenu);
        btnSend = findViewById(R.id.btnSend);
        btnLike = findViewById(R.id.btnLike);
        btnImage = findViewById(R.id.btnImage);
        btnAttach = findViewById(R.id.btnAttach);
        btnLocation = findViewById(R.id.btnLocation);
        btnTask = findViewById(R.id.btnTask);
        layoutReplyPreview = findViewById(R.id.layoutReplyPreview);
        tvReplyToName = findViewById(R.id.tvReplyToName);
        tvReplyToContent = findViewById(R.id.tvReplyToContent);
        btnCancelReply = findViewById(R.id.btnCancelReply);

        messages = new ArrayList<>();
        tvGroupName.setText(groupName);
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(this, messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        // Set long click listener for reply/reaction
        adapter.setOnMessageLongClickListener(message -> showMessageOptions(message));
    }

    private void loadMessages() {
        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy nhóm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("chat_groups")
                .document(groupId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải tin nhắn: " + error.getMessage(), 
                                     Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        messages.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Message message = doc.toObject(Message.class);
                            message.setId(doc.getId());
                            messages.add(message);
                        }
                        adapter.updateMessages(messages);
                        rvMessages.scrollToPosition(messages.size() - 1);
                        
                        // Mark messages as read
                        markMessagesAsRead();
                    }
                });
    }

    private void loadGroupInfo() {
        db.collection("chat_groups")
                .document(groupId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !value.exists()) {
                        return;
                    }

                    ChatGroup group = value.toObject(ChatGroup.class);
                    if (group != null) {
                        tvGroupName.setText(group.getName());
                        int memberCount = group.getMemberIds() != null ? group.getMemberIds().size() : 0;
                        tvMemberCount.setText(memberCount + " thành viên");
                    }
                });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnMenu.setOnClickListener(v -> {
            Intent intent = new Intent(this, GroupDashboardActivity.class);
            intent.putExtra("groupId", groupId);
            intent.putExtra("groupName", groupName);
            startActivity(intent);
        });

        // Toggle send/like button based on input
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    btnSend.setVisibility(View.VISIBLE);
                    btnLike.setVisibility(View.GONE);
                } else {
                    btnSend.setVisibility(View.GONE);
                    btnLike.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSend.setOnClickListener(v -> sendMessage());

        btnLike.setOnClickListener(v -> sendLike());

        btnCancelReply.setOnClickListener(v -> cancelReply());

        // Toolbar buttons
        btnImage.setOnClickListener(v -> 
            Toast.makeText(this, "Tính năng gửi ảnh đang phát triển", Toast.LENGTH_SHORT).show());
        
        btnAttach.setOnClickListener(v -> 
            Toast.makeText(this, "Tính năng đính kèm đang phát triển", Toast.LENGTH_SHORT).show());
        
        btnLocation.setOnClickListener(v -> 
            Toast.makeText(this, "Tính năng gửi vị trí đang phát triển", Toast.LENGTH_SHORT).show());
        
        btnTask.setOnClickListener(v -> 
            Toast.makeText(this, "Tính năng tạo task đang phát triển", Toast.LENGTH_SHORT).show());
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        Message message = new Message(
                groupId,
                currentUser.getUid(),
                currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User",
                currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "",
                content,
                "text"
        );

        // Add reply information if replying
        if (replyToMessage != null) {
            message.setReplyToId(replyToMessage.getId());
            message.setReplyToContent(replyToMessage.getContent());
            message.setReplyToSenderName(replyToMessage.getSenderName());
        }

        // Send to Firestore
        db.collection("chat_groups")
                .document(groupId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    // Update group's last message
                    updateGroupLastMessage(message);
                    etMessage.setText("");
                    cancelReply();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Lỗi gửi tin nhắn", Toast.LENGTH_SHORT).show());
    }

    private void sendLike() {
        if (currentUser == null) {
            return;
        }

        Message message = new Message(
                groupId,
                currentUser.getUid(),
                currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User",
                currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "",
                "👍",
                "text"
        );

        db.collection("chat_groups")
                .document(groupId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> updateGroupLastMessage(message));
    }

    private void updateGroupLastMessage(Message message) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessageContent", message.getContent());
        updates.put("lastMessageSenderId", message.getSenderId());
        updates.put("lastMessageSenderName", message.getSenderName());
        updates.put("lastMessageTime", Timestamp.now());

        db.collection("chat_groups")
                .document(groupId)
                .update(updates);
    }

    private void showMessageOptions(Message message) {
        String[] options = {"Trả lời", "Thả cảm xúc"};
        
        new AlertDialog.Builder(this)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Reply to message
                        replyToMessage = message;
                        showReplyPreview();
                    } else if (which == 1) {
                        // Show reaction options
                        showReactionOptions(message);
                    }
                })
                .show();
    }

    private void showReplyPreview() {
        if (replyToMessage != null) {
            layoutReplyPreview.setVisibility(View.VISIBLE);
            tvReplyToName.setText(replyToMessage.getSenderName());
            tvReplyToContent.setText(replyToMessage.getContent());
            etMessage.requestFocus();
        }
    }

    private void cancelReply() {
        replyToMessage = null;
        layoutReplyPreview.setVisibility(View.GONE);
    }

    private void showReactionOptions(Message message) {
        String[] emojis = {"❤️", "👍", "😂", "😮", "😢", "🙏"};
        
        new AlertDialog.Builder(this)
                .setItems(emojis, (dialog, which) -> {
                    if (currentUser != null) {
                        addReaction(message, emojis[which]);
                    }
                })
                .show();
    }

    private void addReaction(Message message, String emoji) {
        String messageId = message.getId();
        String userId = currentUser.getUid();

        db.collection("chat_groups")
                .document(groupId)
                .collection("messages")
                .document(messageId)
                .update("reactions." + userId, emoji)
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Lỗi thêm cảm xúc", Toast.LENGTH_SHORT).show());
    }

    private void markMessagesAsRead() {
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        
        // Reset unread count for this user
        db.collection("chat_groups")
                .document(groupId)
                .update("unreadCount." + userId, 0);
    }
}
