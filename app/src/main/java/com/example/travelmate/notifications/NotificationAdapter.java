package com.example.travelmate.notifications;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travelmate.R;
import com.example.travelmate.notifications.Notification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification, int position);
    }

    private Context context;
    private List<Notification> notifications;
    private OnNotificationClickListener listener;

    public NotificationAdapter(Context context, List<Notification> notifications) {
        this.context = context;
        this.notifications = notifications != null ? notifications : new ArrayList<>();
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getMessage());

        // Format time
        if (notification.getCreatedAt() != null) {
            holder.tvTime.setText(formatTimestamp(notification.getCreatedAt().toDate()));
        }

        // Icon theo loại thông báo
        String type = notification.getType();
        if ("friend_accepted".equals(type)) {
            holder.ivIcon.setImageResource(R.drawable.ic_add_friend);

            // Load avatar nếu có
            if (notification.getFriendAvatar() != null && !notification.getFriendAvatar().isEmpty()) {
                Glide.with(context)
                        .load(notification.getFriendAvatar())
                        .placeholder(R.drawable.ic_add_friend)
                        .circleCrop()
                        .into(holder.ivIcon);
            }
        } else if ("group_invite".equals(type)) {
            holder.ivIcon.setImageResource(R.drawable.ic_add_group);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_notification);
        }

        // Unread indicator
        if (notification.isRead()) {
            holder.viewUnread.setVisibility(View.GONE);
            holder.itemView.setAlpha(0.7f);
        } else {
            holder.viewUnread.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(1.0f);
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void updateNotifications(List<Notification> newNotifications) {
        this.notifications = newNotifications != null ? newNotifications : new ArrayList<>();
        notifyDataSetChanged();
    }

    private String formatTimestamp(Date date) {
        long diff = System.currentTimeMillis() - date.getTime();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (minutes < 1) {
            return "Vừa xong";
        } else if (minutes < 60) {
            return minutes + " phút trước";
        } else if (hours < 24) {
            return hours + " giờ trước";
        } else if (days < 7) {
            return days + " ngày trước";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(date);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvMessage, tvTime;
        View viewUnread;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            viewUnread = itemView.findViewById(R.id.viewUnread);
        }
    }
}

