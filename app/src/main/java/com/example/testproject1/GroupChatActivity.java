    package com.example.testproject1;

    import android.Manifest;
    import android.app.AlarmManager;
    import android.app.AlertDialog;
    import android.app.PendingIntent;
    import android.content.Context;
    import android.content.Intent;
    import android.content.pm.PackageManager;
    import android.location.Address;
    import android.location.Geocoder;
    import android.net.Uri;
    import android.os.Bundle;
    import android.text.Editable;
    import android.text.TextWatcher;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.widget.EditText;
    import android.widget.ImageView;
    import android.widget.LinearLayout;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.contract.ActivityResultContracts;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.app.ActivityCompat;
    import androidx.core.content.ContextCompat;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.example.testproject1.models.ChatGroup;
    import com.example.testproject1.models.Message;
    import com.google.android.gms.location.FusedLocationProviderClient;
    import com.google.android.gms.location.LocationServices;
    import com.google.firebase.Timestamp;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.auth.FirebaseUser;
    import com.google.firebase.firestore.DocumentSnapshot;
    import com.google.firebase.firestore.FieldValue;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.firebase.firestore.Query;
    import com.google.firebase.firestore.QueryDocumentSnapshot;
    import com.google.firebase.storage.FirebaseStorage;
    import com.google.firebase.storage.StorageReference;

    import java.io.IOException;
    import java.util.ArrayList;
    import java.util.Calendar;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Locale;
    import java.util.Map;
    import java.util.UUID;

    public class GroupChatActivity extends AppCompatActivity {

        private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

        private TextView tvGroupName, tvMemberCount;
        private RecyclerView rvMessages;
        private EditText etMessage;
        private ImageView btnBack, btnMenu, btnSend, btnLike;
        private ImageView btnImage, btnAttach, btnLocation, btnTask;
        private LinearLayout layoutReplyPreview;
        private TextView tvReplyToName, tvReplyToContent;
        private ImageView btnCancelReply;
    private ImageView btnAddTrip;
        private MessageAdapter adapter;
        private List<Message> messages;
        private String groupId;
        private String groupName;
        private Message replyToMessage;
        private boolean isPrivateChat = false;

        private FirebaseFirestore db;
        private FirebaseAuth auth;
        private FirebaseUser currentUser;
        private FirebaseStorage storage;
        private FusedLocationProviderClient fusedLocationClient;

        // Activity Result Launcher for image picker
        private ActivityResultLauncher<String> imagePickerLauncher;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_group_chat);

            // Get group info from intent
            groupId = getIntent().getStringExtra("groupId");
            groupName = getIntent().getStringExtra("groupName");
            isPrivateChat = getIntent().getBooleanExtra("isPrivateChat", false);

            // Initialize Firebase
            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();
            currentUser = auth.getCurrentUser();
            storage = FirebaseStorage.getInstance();

            // Initialize Location Client
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            // Initialize Image Picker
            setupImagePicker();

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

        private void setupImagePicker() {
            imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadAndSendImage(uri);
                    }
                }
            );
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
            btnAddTrip=findViewById(R.id.btnAddTrip);
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
            adapter.setOnMessageClickListener(message -> {

                if (!"OPEN_TRIP".equals(message.getAction())
                        || message.getActionId() == null
                        || currentUser == null) return;

                String tripId = message.getActionId();

                // 🔍 1. Kiểm tra quyền admin của user trong group
                db.collection("chat_groups")
                        .document(groupId)
                        .get()
                        .addOnSuccessListener(groupDoc -> {

                            if (!groupDoc.exists()) return;

                            List<String> adminIds = (List<String>) groupDoc.get("adminIds");
                            boolean isAdmin = adminIds != null
                                    && adminIds.contains(currentUser.getUid());

                            // 👤 2. Nếu KHÔNG phải admin → mới lưu trip
                            if (!isAdmin) {
                                saveTripForCurrentUser(tripId);
                            }

                            // 🔓 3. Luôn mở chi tiết trip
                            FirebaseFirestore.getInstance()
                                    .collection("trips")
                                    .document(tripId)
                                    .get()
                                    .addOnSuccessListener(doc -> {

                                        if (!doc.exists()) return;

                                        TripModel trip = doc.toObject(TripModel.class);
                                        if (trip == null) return;

                                        // 🔥 Fix tripId null
                                        trip.setTripId(doc.getId());

                                        Intent intent = new Intent(
                                                GroupChatActivity.this,
                                                TripDetailActivity.class
                                        );
                                        intent.putExtra("trip_data", trip);
                                        startActivity(intent);
                                    });
                        });
            });





        }
        private void saveTripForCurrentUser(String sourceTripId) {

            if (currentUser == null) return;

            String currentUserId = currentUser.getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 1️⃣ Check user đã có trip này chưa (theo sourceTripId)
            db.collection("trips")
                    .whereEqualTo("userId", currentUserId)
                    .whereEqualTo("sourceTripId", sourceTripId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(checkSnap -> {

                        if (!checkSnap.isEmpty()) {
                            // ❌ Đã lưu rồi
                            Toast.makeText(
                                    this,
                                    "Bạn đã lưu lịch trình này rồi",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        // 2️⃣ Lấy trip gốc
                        db.collection("trips")
                                .document(sourceTripId)
                                .get()
                                .addOnSuccessListener(sourceTripDoc -> {

                                    if (!sourceTripDoc.exists()) return;

                                    TripModel sourceTrip = sourceTripDoc.toObject(TripModel.class);
                                    if (sourceTrip == null) return;

                                    // 3️⃣ Tạo trip mới cho user
                                    String newTripId = db.collection("trips").document().getId();

                                    TripModel newTrip = new TripModel(
                                            newTripId,
                                            sourceTrip.getName(),
                                            sourceTrip.getStartDate(),
                                            sourceTrip.getEndDate(),
                                            currentUserId
                                    );

                                    // 🔥 đánh dấu trip được lưu từ group
                                    newTrip.setSourceTripId(sourceTripId);

                                    db.collection("trips")
                                            .document(newTripId)
                                            .set(newTrip)
                                            .addOnSuccessListener(a -> {

                                                // 4️⃣ Clone schedule
    //                                            cloneSchedule(sourceTripId, newTripId);
                                                cloneSchedule(
                                                        sourceTripId,
                                                        newTripId,
                                                        sourceTrip.getName(),
                                                        currentUser.getUid()
                                                );

                                                Toast.makeText(
                                                        this,
                                                        "Đã lưu lịch trình về của bạn",
                                                        Toast.LENGTH_SHORT
                                                ).show();
                                            });
                                });
                    });
        }
    //    private void cloneSchedule(String oldTripId, String newTripId) {
    //
    //        FirebaseFirestore db = FirebaseFirestore.getInstance();
    //
    //        db.collection("schedule")
    //                .whereEqualTo("tripId", oldTripId)
    //                .get()
    //                .addOnSuccessListener(snapshot -> {
    //
    //                    for (DocumentSnapshot doc : snapshot) {
    //
    //                        ScheduleItemModel item = doc.toObject(ScheduleItemModel.class);
    //                        if (item == null) continue;
    //
    //                        String newItemId = db.collection("schedule").document().getId();
    //
    //                        item.setItemId(newItemId);
    //                        item.setTripId(newTripId);
    //
    //                        db.collection("schedule")
    //                                .document(newItemId)
    //                                .set(item);
    //                    }
    //                });
    //    }
    private void cloneSchedule(
            String oldTripId,
            String newTripId,
            String tripName,
            String userId
    ) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("schedule")
                .whereEqualTo("tripId", oldTripId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    for (DocumentSnapshot doc : snapshot) {

                        ScheduleItemModel item = doc.toObject(ScheduleItemModel.class);
                        if (item == null) continue;

                        String newItemId = db.collection("schedule").document().getId();

                        // clone data
                        item.setItemId(newItemId);
                        item.setTripId(newTripId);

                        db.collection("schedule")
                                .document(newItemId)
                                .set(item)
                                .addOnSuccessListener(a -> {

                                    // =========================
                                    // 🔥 SET ALARM CHO USER MỚI
                                    // =========================
                                    long millis = convertToMillis(
                                            item.getVisitDate(),
                                            item.getVisitTime()
                                    );
                                    if (millis <= System.currentTimeMillis()) return;

                                    scheduleTripNotification(
                                            millis,
                                            item.getPlaceName(),   // placeName
                                            newTripId,             // tripId
                                            tripName,              // tripName
                                            userId,                // userId
                                            null,                  // groupId (user lưu cá nhân)
                                            item.getPlaceName(),
                                            item.getPlaceAddress(),
                                            item.getVisitDate(),
                                            item.getVisitTime(),
                                            item.getEndTime()
                                    );
                                });
                    }
                });
    }
        private void scheduleTripNotification(
                long startTimeMillis,
                String placeName,
                String tripId,
                String tripName,
                String userId,
                String groupId,
                String namePlace,
                String addressPlace,
                String date,
                String start,
                String end
        ) {
            try {
                if (startTimeMillis <= System.currentTimeMillis()) {
                    Log.e("DEBUG_NOTIFY", "⏰ Time in past, skip alarm");
                    return;
                }

                Intent intent = new Intent(this, TripAlarmReceiver.class);
                intent.putExtra("placeName", placeName);
                intent.putExtra("tripId", tripId);
                intent.putExtra("tripName", tripName);
                intent.putExtra("userId", userId);
                intent.putExtra("groupId", groupId);
                intent.putExtra("namePlace", namePlace);
                intent.putExtra("addressPlace", addressPlace);
                intent.putExtra("date", date);
                intent.putExtra("start", start);
                intent.putExtra("end", end);

                int requestCode = (tripId + "_" + startTimeMillis).hashCode();

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                AlarmManager alarmManager =
                        (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                if (alarmManager != null) {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            startTimeMillis,
                            pendingIntent
                    );
                }

                Log.d("DEBUG_NOTIFY", "✅ Alarm set for " + namePlace);

            } catch (Exception e) {
                Log.e("DEBUG_NOTIFY", "❌ scheduleTripNotification crash", e);
            }
        }

        private long convertToMillis(String date, String time) {
            String[] d = date.split("/");
            String[] t = time.split(":");
            Calendar c = Calendar.getInstance();
            c.set(
                    Integer.parseInt(d[2]),
                    Integer.parseInt(d[1]) - 1,
                    Integer.parseInt(d[0]),
                    Integer.parseInt(t[0]),
                    Integer.parseInt(t[1]),
                    0
            );
            return c.getTimeInMillis();
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
                            // Kiểm tra nếu là chat riêng
                            Boolean isGroup = value.getBoolean("isGroup");
                            isPrivateChat = isGroup != null && !isGroup;

                            if (isPrivateChat) {
                                // Chat riêng - hiển thị tên của người kia
                                String user1Id = value.getString("user1Id");
                                String user2Id = value.getString("user2Id");
                                String user1Name = value.getString("user1Name");
                                String user2Name = value.getString("user2Name");

                                // Hiển thị tên người còn lại
                                if (currentUser != null && currentUser.getUid().equals(user1Id)) {
                                    tvGroupName.setText(user2Name);
                                } else {
                                    tvGroupName.setText(user1Name);
                                }
                                tvMemberCount.setVisibility(View.GONE);
                            } else {
                                // Chat nhóm
                                tvGroupName.setText(group.getName());
                                int memberCount = group.getMemberIds() != null ? group.getMemberIds().size() : 0;
                                tvMemberCount.setText(memberCount + " thành viên");
                                tvMemberCount.setVisibility(View.VISIBLE);
                            }
                        }
                    });
        }

        private void setupListeners() {
            btnBack.setOnClickListener(v -> finish());

            btnMenu.setOnClickListener(v -> {
                Intent intent;
                if (isPrivateChat) {
                    intent = new Intent(this, PrivateChatDashboardActivity.class);
                } else {
                    intent = new Intent(this, GroupDashboardActivity.class);
                }
                intent.putExtra("groupId", groupId);
                intent.putExtra("groupName", groupName);
                intent.putExtra("isPrivateChat", isPrivateChat);
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

            // Toolbar buttons - Image picker
            btnImage.setOnClickListener(v -> openImagePicker());

            // Toolbar buttons - File attachment
            btnAttach.setOnClickListener(v ->
                Toast.makeText(this, "Tính năng đính kèm đang phát triển", Toast.LENGTH_SHORT).show());

            // Toolbar buttons - Send location
            btnLocation.setOnClickListener(v -> sendCurrentLocation());

            // Toolbar buttons - Create task
            btnTask.setOnClickListener(v -> showCreateTaskDialog());
            btnAddTrip.setOnClickListener(v -> {

                if (currentUser == null) return;

                db.collection("chat_groups")
                        .document(groupId)
                        .get()
                        .addOnSuccessListener(doc -> {

                            if (!doc.exists()) return;

                            List<String> adminIds = (List<String>) doc.get("adminIds");

                            if (adminIds != null && adminIds.contains(currentUser.getUid())) {
                                // ✅ Là admin → cho add trip
                                showSelectTripDialog();
                            } else {
                                // ❌ Không phải admin
                                Toast.makeText(
                                        this,
                                        "Chỉ admin mới có quyền gắn chuyến đi cho nhóm",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Không kiểm tra được quyền admin", Toast.LENGTH_SHORT).show()
                        );
            });


        }
    //    private void showSelectTripDialog() {
    //        if (currentUser == null) return;
    //
    //        List<String> tripNames = new ArrayList<>();
    //        List<DocumentSnapshot> tripDocs = new ArrayList<>();
    //
    //        AlertDialog.Builder builder = new AlertDialog.Builder(this);
    //        builder.setTitle("Chọn chuyến đi cho nhóm");
    //
    //        db.collection("trips")
    //                .whereEqualTo("userId", currentUser.getUid())
    //                .get()
    //                .addOnSuccessListener(snapshot -> {
    //
    //                    if (snapshot.isEmpty()) {
    //                        Toast.makeText(this, "Bạn chưa có chuyến đi nào", Toast.LENGTH_SHORT).show();
    //                        return;
    //                    }
    //
    //                    for (DocumentSnapshot doc : snapshot) {
    //                        tripDocs.add(doc);
    //                        tripNames.add(doc.getString("name"));
    //                    }
    //
    //                    builder.setItems(
    //                            tripNames.toArray(new String[0]),
    //                            (dialog, which) -> attachGroupToTrip(tripDocs.get(which))
    //                    );
    //
    //                    builder.show();
    //                });
    //    }
    private void showSelectTripDialog() {
        if (currentUser == null) return;

        // 1️⃣ Lấy group hiện tại
        db.collection("chat_groups")
                .document(groupId)
                .get()
                .addOnSuccessListener(groupDoc -> {

                    if (!groupDoc.exists()) return;

                    String existingTripId = groupDoc.getString("tripId");

                    // ===============================
                    // CASE 1: GROUP ĐÃ CÓ TRIP → HỎI HỦY
                    // ===============================
                    if (existingTripId != null && !existingTripId.isEmpty()) {

                        new AlertDialog.Builder(this)
                                .setTitle("Hủy chuyến đi")
                                .setMessage("Nhóm đang có một chuyến đi. Bạn có muốn hủy chuyến đi hiện tại không?")
                                .setPositiveButton("Hủy chuyến đi", (dialog, which) -> {
                                    removeTripFromGroup(existingTripId);
                                })
                                .setNegativeButton("Không", null)
                                .show();

                        return;
                    }

                    // ===============================
                    // CASE 2: GROUP CHƯA CÓ TRIP → CHỌN TRIP
                    // ===============================
                    List<String> tripNames = new ArrayList<>();
                    List<DocumentSnapshot> tripDocs = new ArrayList<>();

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Chọn chuyến đi cho nhóm");

                    db.collection("trips")
                            .whereEqualTo("userId", currentUser.getUid())
                            .get()
                            .addOnSuccessListener(snapshot -> {

                                if (snapshot.isEmpty()) {
                                    Toast.makeText(
                                            this,
                                            "Bạn chưa có chuyến đi nào",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    return;
                                }

                                for (DocumentSnapshot doc : snapshot) {
                                    tripDocs.add(doc);
                                    tripNames.add(doc.getString("name"));
                                }

                                builder.setItems(
                                        tripNames.toArray(new String[0]),
                                        (dialog, which) ->
                                                attachGroupToTrip(tripDocs.get(which))
                                );

                                builder.show();
                            });
                });
    }
        private void removeTripFromGroup(String tripId) {
            Map<String, Object> groupUpdate = new HashMap<>();
            groupUpdate.put("tripId", FieldValue.delete());

            db.collection("chat_groups")
                    .document(groupId)
                    .update(groupUpdate)
                    .addOnSuccessListener(a -> {

                        // optional: update flag trong trip
                        db.collection("trips").document(tripId)
                                .update("isGroupTrip", false);

                        sendSystemMessage("❌ Admin đã hủy chuyến đi của nhóm");
                        Toast.makeText(this, "Đã hủy chuyến đi của nhóm", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }


        private void attachGroupToTrip(DocumentSnapshot tripDoc) {
            if (currentUser == null) return;

            String tripId = tripDoc.getId();
            String tripName = tripDoc.getString("name");

            // 1) Check group hiện tại đã có trip chưa
            db.collection("chat_groups")
                    .document(groupId)
                    .get()
                    .addOnSuccessListener(groupDoc -> {
                        if (!groupDoc.exists()) return;

                        String existingTripId = groupDoc.getString("tripId");
                        if (existingTripId != null && !existingTripId.isEmpty()) {
                            Toast.makeText(this, "Nhóm đã có chuyến đi. Hãy hủy chuyến đi cũ trước.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 2) Check trip này đã bị group khác dùng chưa (do trip không còn groupId)
                        db.collection("chat_groups")
                                .whereEqualTo("tripId", tripId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(snapshot -> {
                                    if (!snapshot.isEmpty()) {
                                        Toast.makeText(this, "Trip này đã được gắn cho một nhóm khác", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    // 3) OK -> gắn trip cho group
                                    Map<String, Object> groupUpdate = new HashMap<>();
                                    groupUpdate.put("tripId", tripId);

                                    db.collection("chat_groups")
                                            .document(groupId)
                                            .update(groupUpdate)
                                            .addOnSuccessListener(a -> {

                                                // optional: update trip flag cho UI
                                                db.collection("trips").document(tripId)
                                                        .update("isGroupTrip", true);

                                                sendTripSystemMessage(
                                                        "🗺️ Nhóm đã được gắn vào chuyến đi \"" + tripName + "\"\n"
                                                                + "             👉 Xem chi tiết | 💾 Lưu",
                                                        tripId
                                                );
                                                Toast.makeText(this, "Đã gắn nhóm vào chuyến đi " + tripName, Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                            );
                                });
                    });
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
            // Check if current user is the sender
            boolean isOwnMessage = message.getSenderId().equals(currentUser.getUid());

            String[] options;
            if (isOwnMessage) {
                options = new String[]{"Trả lời", "Thả cảm xúc", "Chỉnh sửa", "Xóa tin nhắn"};
            } else {
                options = new String[]{"Trả lời", "Thả cảm xúc"};
            }

            new AlertDialog.Builder(this)
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0: // Reply
                                replyToMessage = message;
                                showReplyPreview();
                                break;
                            case 1: // Reaction
                                showReactionOptions(message);
                                break;
                            case 2: // Edit (only for own messages)
                                if (isOwnMessage) {
                                    showEditMessageDialog(message);
                                }
                                break;
                            case 3: // Delete (only for own messages)
                                if (isOwnMessage) {
                                    showDeleteConfirmDialog(message);
                                }
                                break;
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

        // ========== IMAGE UPLOAD FEATURE ==========

        private void openImagePicker() {
            imagePickerLauncher.launch("image/*");
        }

        private void uploadAndSendImage(Uri imageUri) {
            if (currentUser == null) {
                Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading
            Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

            String fileName = "chat_images/" + groupId + "/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference ref = storage.getReference().child(fileName);

            ref.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            sendImageMessage(uri.toString());
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

        private void sendImageMessage(String imageUrl) {
            Message message = new Message(
                    groupId,
                    currentUser.getUid(),
                    currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User",
                    currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "",
                    "📷 Hình ảnh",
                    "image"
            );
            message.setImageUrl(imageUrl);

            db.collection("chat_groups")
                    .document(groupId)
                    .collection("messages")
                    .add(message)
                    .addOnSuccessListener(documentReference -> {
                        updateGroupLastMessage(message);
                        Toast.makeText(this, "Đã gửi ảnh", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi gửi ảnh", Toast.LENGTH_SHORT).show());
        }

        // ========== LOCATION FEATURE ==========

        private void sendCurrentLocation() {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }

            Toast.makeText(this, "Đang lấy vị trí...", Toast.LENGTH_SHORT).show();

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            double lat = location.getLatitude();
                            double lng = location.getLongitude();
                            String address = getAddressFromCoordinates(lat, lng);
                            sendLocationMessage(lat, lng, address);
                        } else {
                            Toast.makeText(this, "Không thể lấy vị trí", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi lấy vị trí: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

        private String getAddressFromCoordinates(double lat, double lng) {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    return address.getAddressLine(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "Vị trí: " + lat + ", " + lng;
        }

        private void sendLocationMessage(double lat, double lng, String locationName) {
            if (currentUser == null) return;

            Message message = new Message(
                    groupId,
                    currentUser.getUid(),
                    currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User",
                    currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "",
                    "📍 " + locationName,
                    "location"
            );
            message.setLatitude(lat);
            message.setLongitude(lng);
            message.setLocationName(locationName);

            db.collection("chat_groups")
                    .document(groupId)
                    .collection("messages")
                    .add(message)
                    .addOnSuccessListener(documentReference -> {
                        updateGroupLastMessage(message);
                        Toast.makeText(this, "Đã gửi vị trí", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi gửi vị trí", Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendCurrentLocation();
                } else {
                    Toast.makeText(this, "Cần quyền vị trí để gửi", Toast.LENGTH_SHORT).show();
                }
            }
        }

        // ========== EDIT MESSAGE FEATURE ==========

        private void showEditMessageDialog(Message message) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Chỉnh sửa tin nhắn");

            final EditText input = new EditText(this);
            input.setText(message.getContent());
            input.setSelection(message.getContent().length());
            builder.setView(input);

            builder.setPositiveButton("Lưu", (dialog, which) -> {
                String newContent = input.getText().toString().trim();
                if (!newContent.isEmpty() && !newContent.equals(message.getContent())) {
                    editMessage(message, newContent);
                }
            });

            builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
            builder.show();
        }

        private void editMessage(Message message, String newContent) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("content", newContent);
            updates.put("isEdited", true);
            updates.put("editedAt", Timestamp.now());
            if (message.getOriginalContent() == null) {
                updates.put("originalContent", message.getContent());
            }

            db.collection("chat_groups")
                    .document(groupId)
                    .collection("messages")
                    .document(message.getId())
                    .update(updates)
                    .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Đã chỉnh sửa tin nhắn", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi chỉnh sửa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        // ========== DELETE MESSAGE FEATURE ==========

        private void showDeleteConfirmDialog(Message message) {
            new AlertDialog.Builder(this)
                    .setTitle("Xóa tin nhắn")
                    .setMessage("Bạn có chắc muốn xóa tin nhắn này?")
                    .setPositiveButton("Xóa", (dialog, which) -> deleteMessage(message))
                    .setNegativeButton("Hủy", null)
                    .show();
        }

        private void deleteMessage(Message message) {
            // Soft delete - update isDeleted flag
            Map<String, Object> updates = new HashMap<>();
            updates.put("isDeleted", true);
            updates.put("content", "Tin nhắn đã bị xóa");

            db.collection("chat_groups")
                    .document(groupId)
                    .collection("messages")
                    .document(message.getId())
                    .update(updates)
                    .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Đã xóa tin nhắn", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        // ========== TASK FEATURE ==========

        private void showCreateTaskDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_task, null);
            builder.setView(dialogView);

            EditText etTaskTitle = dialogView.findViewById(R.id.etTaskTitle);
            EditText etTaskDescription = dialogView.findViewById(R.id.etTaskDescription);

            builder.setTitle("Tạo công việc mới");
            builder.setPositiveButton("Tạo", (dialog, which) -> {
                String title = etTaskTitle.getText().toString().trim();
                String description = etTaskDescription.getText().toString().trim();
                if (!title.isEmpty()) {
                    createTask(title, description);
                } else {
                    Toast.makeText(this, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Hủy", null);
            builder.show();
        }

        private void createTask(String title, String description) {
            if (currentUser == null) return;

            Map<String, Object> task = new HashMap<>();
            task.put("title", title);
            task.put("description", description);
            task.put("createdBy", currentUser.getUid());
            task.put("createdByName", currentUser.getDisplayName());
            task.put("createdAt", Timestamp.now());
            task.put("status", "pending");
            task.put("assignedTo", new ArrayList<String>());

            db.collection("chat_groups")
                    .document(groupId)
                    .collection("tasks")
                    .add(task)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Đã tạo công việc", Toast.LENGTH_SHORT).show();
                        // Send system message about new task
                        sendSystemMessage("📋 " + currentUser.getDisplayName() + " đã tạo công việc: " + title);
                    })
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tạo công việc: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        private void sendSystemMessage(String content) {
            Message message = new Message(
                    groupId,
                    "system",
                    "Hệ thống",
                    "",
                    content,
                    "system"
            );

            db.collection("chat_groups")
                    .document(groupId)
                    .collection("messages")
                    .add(message);
        }
        private void sendTripSystemMessage(String content, String tripId) {
            Message message = new Message(
                    groupId,
                    "system",
                    "Hệ thống",
                    "",
                    content,
                    "system"
            );
            message.setAction("OPEN_TRIP");
            message.setActionId(tripId); // ✅ ĐÚNG là tripId

            db.collection("chat_groups")
                    .document(groupId)
                    .collection("messages")
                    .add(message);
        }

    }
