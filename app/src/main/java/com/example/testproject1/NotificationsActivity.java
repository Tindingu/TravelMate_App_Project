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

import com.example.testproject1.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView btnMarkAllRead;
    private RecyclerView rvNotifications;
    private LinearLayout layoutEmpty;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private NotificationAdapter adapter;
    private List<Notification> notifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        loadNotifications();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        notifications = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this, notifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        adapter.setOnNotificationClickListener((notification, position) -> {
            // Đánh dấu đã đọc
            markAsRead(notification.getId());

            // Xử lý theo loại thông báo
            String type = notification.getType();
            if ("friend_accepted".equals(type)) {
                // Có thể mở profile của bạn mới
                Toast.makeText(this, "Bạn và " + notification.getFriendName() +
                        " đã là bạn bè!", Toast.LENGTH_SHORT).show();
            } else if ("group_invite".equals(type)) {
                // Mở nhóm chat
                // Intent intent = new Intent(this, GroupChatActivity.class);
                // intent.putExtra("groupId", notification.getGroupId());
                // startActivity(intent);
            }
        });
    }

    private void loadNotifications() {
        db.collection("notifications")
                .whereEqualTo("recipientId", currentUser.getUid())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    notifications.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Notification notification = doc.toObject(Notification.class);
                            notification.setId(doc.getId());
                            notifications.add(notification);
                        }
                    }

                    updateUI();
                });
    }

    private void updateUI() {
        if (notifications.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            adapter.updateNotifications(notifications);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
    }

    private void markAsRead(String notificationId) {
        db.collection("notifications")
                .document(notificationId)
                .update("isRead", true);
    }

    private void markAllAsRead() {
        WriteBatch batch = db.batch();

        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                batch.update(db.collection("notifications").document(notification.getId()),
                        "isRead", true);
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã đọc tất cả thông báo", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

