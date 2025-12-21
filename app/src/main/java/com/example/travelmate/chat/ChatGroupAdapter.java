package com.example.travelmate.chat;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travelmate.R;
import com.example.travelmate.chat.ChatGroup;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ChatGroupAdapter extends RecyclerView.Adapter<ChatGroupAdapter.ViewHolder> {

    private Context context;
    private List<ChatGroup> groups;
    private String currentUserId;

    public ChatGroupAdapter(Context context, List<ChatGroup> groups) {
        this.context = context;
        this.groups = groups != null ? groups : new ArrayList<>();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                             FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatGroup group = groups.get(position);

        // Set group name
        holder.tvGroupName.setText(group.getName());

        // Load group avatar
        if (group.getAvatarUrl() != null && !group.getAvatarUrl().isEmpty()) {
            Glide.with(context)
                    .load(group.getAvatarUrl())
                    .placeholder(R.drawable.avttest)
                    .into(holder.ivGroupAvatar);
        } else {
            holder.ivGroupAvatar.setImageResource(R.drawable.ic_group1);
        }

        // Format last message
        String lastMessage = "";
        if (group.getLastMessageContent() != null) {
            String senderName = group.getLastMessageSenderName();
            if (group.getLastMessageSenderId() != null && 
                group.getLastMessageSenderId().equals(currentUserId)) {
                senderName = "Bạn";
            }
            lastMessage = senderName + ": " + group.getLastMessageContent();
        }
        holder.tvLastMessage.setText(lastMessage);

        // Format timestamp
        if (group.getLastMessageTime() != null) {
            holder.tvTime.setText(formatTimestamp(group.getLastMessageTime().toDate()));
        } else {
            holder.tvTime.setText("");
        }

        // Show unread badge if there are unread messages
        if (group.getUnreadCount() != null && group.getUnreadCount().containsKey(currentUserId)) {
            int unreadCount = group.getUnreadCount().get(currentUserId);
            if (unreadCount > 0) {
                holder.tvUnreadBadge.setVisibility(View.VISIBLE);
                holder.tvUnreadBadge.setText(String.valueOf(Math.min(unreadCount, 99)));
            } else {
                holder.tvUnreadBadge.setVisibility(View.GONE);
            }
        } else {
            holder.tvUnreadBadge.setVisibility(View.GONE);
        }

        // Click listener to open chat
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, GroupChatActivity.class);
            intent.putExtra("groupId", group.getId());
            intent.putExtra("groupName", group.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    public void updateGroups(List<ChatGroup> newGroups) {
        this.groups = newGroups != null ? newGroups : new ArrayList<>();
        notifyDataSetChanged();
    }

    private String formatTimestamp(Date date) {
        long diff = System.currentTimeMillis() - date.getTime();
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (hours < 1) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + " phút";
        } else if (hours < 24) {
            return hours + " giờ";
        } else if (days < 7) {
            return days + " ngày";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(date);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGroupAvatar;
        TextView tvGroupName;
        TextView tvLastMessage;
        TextView tvTime;
        TextView tvUnreadBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGroupAvatar = itemView.findViewById(R.id.ivGroupAvatar);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
        }
    }
}
