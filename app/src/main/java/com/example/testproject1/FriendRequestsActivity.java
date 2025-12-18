package com.example.testproject1;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testproject1.models.FriendRequest;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FriendRequestsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tabReceived, tabSent;
    private RecyclerView rvRequests;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyMessage;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private FriendRequestAdapter adapter;
    private List<FriendRequest> receivedRequests;
    private List<FriendRequest> sentRequests;
    private boolean isReceivedTab = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_requests);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadReceivedRequests();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tabReceived = findViewById(R.id.tabReceived);
        tabSent = findViewById(R.id.tabSent);
        rvRequests = findViewById(R.id.rvRequests);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        receivedRequests = new ArrayList<>();
        sentRequests = new ArrayList<>();

        adapter = new FriendRequestAdapter(this, receivedRequests, true);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        rvRequests.setAdapter(adapter);

        adapter.setOnRequestActionListener(new FriendRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(FriendRequest request, int position) {
                acceptFriendRequest(request, position);
            }

            @Override
            public void onReject(FriendRequest request, int position) {
                rejectFriendRequest(request, position);
            }

            @Override
            public void onCancel(FriendRequest request, int position) {
                cancelFriendRequest(request, position);
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        tabReceived.setOnClickListener(v -> {
            if (!isReceivedTab) {
                isReceivedTab = true;
                updateTabStyles();
                showReceivedRequests();
            }
        });

        tabSent.setOnClickListener(v -> {
            if (isReceivedTab) {
                isReceivedTab = false;
                updateTabStyles();
                loadSentRequests();
            }
        });
    }

    private void updateTabStyles() {
        if (isReceivedTab) {
            tabReceived.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            tabReceived.setTypeface(null, android.graphics.Typeface.BOLD);
            tabReceived.setBackgroundResource(R.drawable.bg_tab_selected);

            tabSent.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tabSent.setTypeface(null, android.graphics.Typeface.NORMAL);
            tabSent.setBackground(null);
        } else {
            tabSent.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            tabSent.setTypeface(null, android.graphics.Typeface.BOLD);
            tabSent.setBackgroundResource(R.drawable.bg_tab_selected);

            tabReceived.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tabReceived.setTypeface(null, android.graphics.Typeface.NORMAL);
            tabReceived.setBackground(null);
        }
    }

    private void loadReceivedRequests() {
        db.collection("friend_requests")
                .whereEqualTo("receiverId", currentUser.getUid())
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    receivedRequests.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            FriendRequest request = doc.toObject(FriendRequest.class);
                            request.setId(doc.getId());
                            receivedRequests.add(request);
                        }
                    }

                    if (isReceivedTab) {
                        showReceivedRequests();
                    }
                });
    }

    private void loadSentRequests() {
        db.collection("friend_requests")
                .whereEqualTo("senderId", currentUser.getUid())
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    sentRequests.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        FriendRequest request = doc.toObject(FriendRequest.class);
                        request.setId(doc.getId());
                        sentRequests.add(request);
                    }
                    showSentRequests();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                });
    }

    private void showReceivedRequests() {
        adapter = new FriendRequestAdapter(this, receivedRequests, true);
        adapter.setOnRequestActionListener(new FriendRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(FriendRequest request, int position) {
                acceptFriendRequest(request, position);
            }

            @Override
            public void onReject(FriendRequest request, int position) {
                rejectFriendRequest(request, position);
            }

            @Override
            public void onCancel(FriendRequest request, int position) {
                cancelFriendRequest(request, position);
            }
        });
        rvRequests.setAdapter(adapter);

        if (receivedRequests.isEmpty()) {
            showEmptyState("Không có lời mời kết bạn nào");
        } else {
            hideEmptyState();
        }
    }

    private void showSentRequests() {
        adapter = new FriendRequestAdapter(this, sentRequests, false);
        adapter.setOnRequestActionListener(new FriendRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(FriendRequest request, int position) {}

            @Override
            public void onReject(FriendRequest request, int position) {}

            @Override
            public void onCancel(FriendRequest request, int position) {
                cancelFriendRequest(request, position);
            }
        });
        rvRequests.setAdapter(adapter);

        if (sentRequests.isEmpty()) {
            showEmptyState("Bạn chưa gửi lời mời kết bạn nào");
        } else {
            hideEmptyState();
        }
    }

    private void showEmptyState(String message) {
        tvEmptyMessage.setText(message);
        layoutEmpty.setVisibility(View.VISIBLE);
        rvRequests.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        layoutEmpty.setVisibility(View.GONE);
        rvRequests.setVisibility(View.VISIBLE);
    }

    private void acceptFriendRequest(FriendRequest request, int position) {
        String senderId = request.getSenderId();
        String receiverId = request.getReceiverId();

        // 1. Cập nhật status của friend request
        db.collection("friend_requests").document(request.getId())
                .update("status", "accepted", "updatedAt", Timestamp.now())
                .addOnSuccessListener(aVoid -> {
                    // 2. Thêm friendId cho cả 2 user
                    db.collection("users").document(senderId)
                            .update("friendIds", FieldValue.arrayUnion(receiverId));

                    db.collection("users").document(receiverId)
                            .update("friendIds", FieldValue.arrayUnion(senderId));

                    Toast.makeText(this, "Đã chấp nhận lời mời kết bạn",
                            Toast.LENGTH_SHORT).show();

                    // 3. Xóa khỏi list
                    adapter.removeItem(position);
                    if (receivedRequests.isEmpty()) {
                        showEmptyState("Không có lời mời kết bạn nào");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void rejectFriendRequest(FriendRequest request, int position) {
        db.collection("friend_requests").document(request.getId())
                .update("status", "rejected", "updatedAt", Timestamp.now())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã từ chối lời mời kết bạn",
                            Toast.LENGTH_SHORT).show();
                    adapter.removeItem(position);
                    if (receivedRequests.isEmpty()) {
                        showEmptyState("Không có lời mời kết bạn nào");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void cancelFriendRequest(FriendRequest request, int position) {
        db.collection("friend_requests").document(request.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã hủy lời mời kết bạn",
                            Toast.LENGTH_SHORT).show();
                    adapter.removeItem(position);
                    if (sentRequests.isEmpty()) {
                        showEmptyState("Bạn chưa gửi lời mời kết bạn nào");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}

