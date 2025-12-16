package com.example.testproject1;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testproject1.models.ChatGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView rvChatList;
    private ChatGroupAdapter adapter;
    private List<ChatGroup> allGroups;
    private List<ChatGroup> filteredGroups;
    private EditText etSearchChat;
    private TextView tabAll, tabArchived;
    private ImageView btnAddFriend, btnCreateGroup;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private boolean showArchived = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        // Initialize views
        initViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Load chat groups
        loadChatGroups();

        // Setup listeners
        setupListeners();
    }

    private void initViews() {
        rvChatList = findViewById(R.id.rvChatList);
        etSearchChat = findViewById(R.id.etSearchChat);
        tabAll = findViewById(R.id.tabAll);
        tabArchived = findViewById(R.id.tabArchived);
        btnAddFriend = findViewById(R.id.btnAddFriend);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);

        allGroups = new ArrayList<>();
        filteredGroups = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new ChatGroupAdapter(this, filteredGroups);
        rvChatList.setLayoutManager(new LinearLayoutManager(this));
        rvChatList.setAdapter(adapter);
    }

    private void loadChatGroups() {
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Query groups where current user is a member
        db.collection("chat_groups")
                .whereArrayContains("memberIds", userId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải tin nhắn: " + error.getMessage(), 
                                     Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        allGroups.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            ChatGroup group = doc.toObject(ChatGroup.class);
                            group.setId(doc.getId());
                            allGroups.add(group);
                        }
                        filterGroups();
                    }
                });
    }

    private void filterGroups() {
        filteredGroups.clear();
        String searchQuery = etSearchChat.getText().toString().toLowerCase().trim();

        for (ChatGroup group : allGroups) {
            // Filter by archived status
            if (showArchived != group.isArchived()) {
                continue;
            }

            // Filter by search query
            if (!searchQuery.isEmpty()) {
                if (!group.getName().toLowerCase().contains(searchQuery)) {
                    continue;
                }
            }

            filteredGroups.add(group);
        }

        adapter.updateGroups(filteredGroups);
    }

    private void setupListeners() {
        // Search functionality
        etSearchChat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterGroups();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Tab switching
        tabAll.setOnClickListener(v -> {
            showArchived = false;
            updateTabStyles();
            filterGroups();
        });

        tabArchived.setOnClickListener(v -> {
            showArchived = true;
            updateTabStyles();
            filterGroups();
        });

        // Action buttons
        btnAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(this, FindFriendsActivity.class);
            startActivity(intent);
        });

        btnCreateGroup.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateGroupActivity.class);
            startActivity(intent);
        });
    }

    private void updateTabStyles() {
        if (showArchived) {
            // Archived tab is selected
            tabArchived.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            tabArchived.setTypeface(null, android.graphics.Typeface.BOLD);
            tabArchived.setBackgroundResource(R.drawable.bg_tab_selected);

            tabAll.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tabAll.setTypeface(null, android.graphics.Typeface.NORMAL);
            tabAll.setBackground(null);
        } else {
            // All tab is selected
            tabAll.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            tabAll.setTypeface(null, android.graphics.Typeface.BOLD);
            tabAll.setBackgroundResource(R.drawable.bg_tab_selected);

            tabArchived.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tabArchived.setTypeface(null, android.graphics.Typeface.NORMAL);
            tabArchived.setBackground(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload groups when returning to this activity
        if (currentUser != null) {
            loadChatGroups();
        }
    }
}
