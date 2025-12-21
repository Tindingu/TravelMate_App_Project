package com.example.travelmate.trips;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.travelmate.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TripAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "trip_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("DEBUG_RECEIVER", "🔥 TripAlarmReceiver TRIGGERED");

        String placeName = intent.getStringExtra("placeName");
        if (placeName == null) placeName = "địa điểm đã lên lịch";

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // ✅ TẠO CHANNEL (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nhắc lịch trình",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(channel);
        }

        // ✅ BUILD NOTIFICATION
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // icon chắc chắn
                .setContentTitle("⏰ Đến giờ rồi!")
                .setContentText("Bắt đầu chuyến đi tại: " + placeName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL) // 🔥 sound + rung + lights
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .build();

        // ✅ SHOW NOTIFICATION
        manager.notify((int) System.currentTimeMillis(), notification);
        // 🔹 BỔ SUNG – KHÔNG ẢNH HƯỞNG CODE CŨ

        // =========================
// 🔹 BỔ SUNG – DEBUG LOG
// =========================
        String tripId = intent.getStringExtra("tripId");
        String tripName = intent.getStringExtra("tripName");
        String userId = intent.getStringExtra("userId");
        String groupId = intent.getStringExtra("groupId"); // có thể null
        String namePlace= intent.getStringExtra("namePlace");
        String addressPlace= intent.getStringExtra("addressPlace");
        String date= intent.getStringExtra("date");
        String start=intent.getStringExtra("start");
        String end=intent.getStringExtra("end");

        Log.d("DEBUG_ALARM", "👉 tripId = " + tripId);
        Log.d("DEBUG_ALARM", "👉 tripName = " + tripName);
        Log.d("DEBUG_ALARM", "👉 userId = " + userId);
        Log.d("DEBUG_ALARM", "👉 groupId = " + groupId);
        Log.d("DEBUG_ALARM", "👉 namePlace = " + namePlace);
        Log.d("DEBUG_ALARM", "👉 addessPlace = " + addressPlace);
        Log.d("DEBUG_ALARM", "👉 date = " + date);
        Log.d("DEBUG_ALARM", "👉 start = " + start);
        Log.d("DEBUG_ALARM", "👉 end = " + end);



// 1️⃣ LƯU THÔNG BÁO TRONG APP
        if (userId != null && tripId != null) {
            Log.d("DEBUG_ALARM", "✅ VÀO saveInAppNotification()");
            saveInAppNotification(tripId, tripName, userId,namePlace);
        } else {
            Log.e("DEBUG_ALARM", "❌ KHÔNG vào saveInAppNotification() "
                    + "(userId hoặc tripId null)");
        }

// 2️⃣ GỬI THÔNG BÁO VÀO GROUP (NẾU CÓ)
        // 2️⃣ GỬI THÔNG BÁO VÀO GROUP (NẾU CÓ)
        if (groupId != null && !groupId.isEmpty()) {

            // ✅ CASE 1: Intent đã có groupId
            Log.d("DEBUG_ALARM", "✅ groupId từ Intent → gửi vào group chat");
            sendGroupSystemMessage(
                    groupId,
                    tripId,
                    tripName,
                    namePlace,
                    addressPlace,
                    date,
                    start,
                    end
            );

        } else {

            // ✅ CASE 2: Intent chưa có groupId → query trip
            Log.d("DEBUG_ALARM", "ℹ️ groupId null → query chat_groups để lấy groupId theo tripId");

            FirebaseFirestore.getInstance()
                    .collection("chat_groups")
                    .whereEqualTo("tripId", tripId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(qs -> {
                        if (qs.isEmpty()) {
                            Log.d("DEBUG_ALARM", "ℹ️ Không có group nào gắn tripId này → không gửi group");
                            return;
                        }

                        String groupIdFromGroup = qs.getDocuments().get(0).getId();
                        Log.d("DEBUG_ALARM", "✅ Lấy groupId từ chat_groups → gửi group");
                        Log.d("DEBUG_ALARM", "👉 groupIdFromGroup = " + groupIdFromGroup);

                        sendGroupSystemMessage(
                                groupIdFromGroup,
                                tripId,
                                tripName,
                                namePlace,
                                addressPlace,
                                date,
                                start,
                                end
                        );
                    })
                    .addOnFailureListener(e ->
                            Log.e("DEBUG_ALARM", "❌ Lỗi query chat_groups", e)
                    );

        }




    }
    // 📥 LƯU THÔNG BÁO ĐỂ HIỆN TRONG NotificationsActivity
    // =====================================================
    private void saveInAppNotification(String tripId, String tripName, String userId,String namePlace) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> noti = new HashMap<>();
        noti.put("recipientId", userId);
        noti.put("title", "⏰ Nhắc lịch trình");
        noti.put("message", "Đến giờ tới địa điểm "+namePlace+" của chuyến đi " + tripName);
        noti.put("type", "trip_reminder");
        noti.put("tripId", tripId);
        noti.put("isRead", false);
        noti.put("createdAt", Timestamp.now()); // ⚠️ KHÔNG dùng serverTimestamp

        db.collection("notifications").add(noti);
    }

    // =====================================================
    // 💬 GỬI SYSTEM MESSAGE VÀO GROUP CHAT
    // =====================================================
    private void sendGroupSystemMessage(
            String groupId,
            String tripId,
            String tripName,
            String namePlace,
            String addressPlace,
            String date,
            String start,
            String end
    ) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String content =
                "⏰ **Thông báo lịch trình**\n" +
                        "📍 Địa điểm: " + namePlace + "\n" +
                        "🕒 Thời gian: " + start + " - " + end + "\n" +
                        "📅 Ngày: " + date + "\n" +
                        "🗺️ Chuyến đi: " + tripName;

        Map<String, Object> message = new HashMap<>();
        message.put("groupId", groupId);
        message.put("senderId", "system");
        message.put("senderName", "Hệ thống");
        message.put("content", content);
        message.put("type", "system");
        message.put("timestamp", Timestamp.now());

        // 👇 giữ nguyên logic click
        message.put("action", "OPEN_TRIP");
        message.put("actionId", tripId);

        db.collection("chat_groups")
                .document(groupId)
                .collection("messages")
                .add(message);
    }

}
