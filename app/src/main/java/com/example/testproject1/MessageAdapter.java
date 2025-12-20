package com.example.testproject1;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testproject1.models.Message;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_SYSTEM = 3;

    private Context context;
    private List<Message> messages;
    private String currentUserId;
    private OnMessageLongClickListener longClickListener;

    public interface OnMessageLongClickListener {
        void onMessageLongClick(Message message);
    }

    public MessageAdapter(Context context, List<Message> messages) {
        this.context = context;
        this.messages = messages != null ? messages : new ArrayList<>();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                             FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }
    public interface OnMessageClickListener {
        void onMessageClick(Message message);
    }
    private OnMessageClickListener clickListener;

    public void setOnMessageClickListener(OnMessageClickListener listener) {
        this.clickListener = listener;
    }


    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if ("system".equals(message.getType())) {
            return VIEW_TYPE_SYSTEM;
        } else if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_RECEIVED) {
            View view = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_system, parent, false);
            return new SystemMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        if (holder instanceof SentMessageViewHolder) {
            bindSentMessage((SentMessageViewHolder) holder, message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            bindReceivedMessage((ReceivedMessageViewHolder) holder, message);
        } else if (holder instanceof SystemMessageViewHolder) {
            bindSystemMessage((SystemMessageViewHolder) holder, message);
        }
    }

    private void bindSentMessage(SentMessageViewHolder holder, Message message) {
        // Handle deleted message
        if (message.isDeleted()) {
            holder.tvMessage.setText("Tin nhắn đã bị xóa");
            holder.tvMessage.setAlpha(0.5f);
            holder.layoutReply.setVisibility(View.GONE);
            holder.layoutReactions.setVisibility(View.GONE);
            if (holder.ivMessageImage != null) holder.ivMessageImage.setVisibility(View.GONE);
            holder.tvTime.setText(formatTime(message.getTimestamp().toDate()));
            return;
        }

        holder.tvMessage.setAlpha(1.0f);

        // Handle different message types
        String type = message.getType();
        if ("image".equals(type) && message.getImageUrl() != null) {
            holder.tvMessage.setVisibility(View.GONE);
            if (holder.ivMessageImage != null) {
                holder.ivMessageImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.ic_camera)
                        .into(holder.ivMessageImage);

                // Click to view full image
                holder.ivMessageImage.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(android.net.Uri.parse(message.getImageUrl()), "image/*");
                    context.startActivity(intent);
                });
            }
        } else if ("location".equals(type)) {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.tvMessage.setText("📍 " + (message.getLocationName() != null ? message.getLocationName() : "Vị trí đã chia sẻ"));
            if (holder.ivMessageImage != null) holder.ivMessageImage.setVisibility(View.GONE);

            // Click to open in Maps
            holder.itemView.setOnClickListener(v -> {
                String uri = "geo:" + message.getLatitude() + "," + message.getLongitude() + "?q=" + message.getLatitude() + "," + message.getLongitude();
                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
                context.startActivity(intent);
            });
        } else {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.tvMessage.setText(message.getContent());
            if (holder.ivMessageImage != null) holder.ivMessageImage.setVisibility(View.GONE);
        }

        // Show edited indicator
        String timeText = formatTime(message.getTimestamp().toDate());
        if (message.isEdited()) {
            timeText += " (đã chỉnh sửa)";
        }
        holder.tvTime.setText(timeText);

        // Show reply indicator if replying to another message
        if (message.getReplyToId() != null && message.getReplyToContent() != null) {
            holder.layoutReply.setVisibility(View.VISIBLE);
            holder.tvReplyToName.setText(message.getReplyToSenderName());
            holder.tvReplyToContent.setText(message.getReplyToContent());
        } else {
            holder.layoutReply.setVisibility(View.GONE);
        }

        // Show reactions if any
        if (message.getReactions() != null && !message.getReactions().isEmpty()) {
            holder.layoutReactions.setVisibility(View.VISIBLE);
            holder.tvReactions.setText(formatReactions(message.getReactions()));
        } else {
            holder.layoutReactions.setVisibility(View.GONE);
        }

        // Long click for reply/reaction options
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onMessageLongClick(message);
            }
            return true;
        });
    }

    private void bindReceivedMessage(ReceivedMessageViewHolder holder, Message message) {
        holder.tvSenderName.setText(message.getSenderName());

        // Handle deleted message
        if (message.isDeleted()) {
            holder.tvMessage.setText("Tin nhắn đã bị xóa");
            holder.tvMessage.setAlpha(0.5f);
            holder.layoutReply.setVisibility(View.GONE);
            holder.layoutReactions.setVisibility(View.GONE);
            if (holder.ivMessageImage != null) holder.ivMessageImage.setVisibility(View.GONE);
            holder.tvTime.setText(formatTime(message.getTimestamp().toDate()));
            loadSenderAvatar(holder, message);
            return;
        }

        holder.tvMessage.setAlpha(1.0f);

        // Handle different message types
        String type = message.getType();
        if ("image".equals(type) && message.getImageUrl() != null) {
            holder.tvMessage.setVisibility(View.GONE);
            if (holder.ivMessageImage != null) {
                holder.ivMessageImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.ic_camera)
                        .into(holder.ivMessageImage);

                // Click to view full image
                holder.ivMessageImage.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(android.net.Uri.parse(message.getImageUrl()), "image/*");
                    context.startActivity(intent);
                });
            }
        } else if ("location".equals(type)) {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.tvMessage.setText("📍 " + (message.getLocationName() != null ? message.getLocationName() : "Vị trí đã chia sẻ"));
            if (holder.ivMessageImage != null) holder.ivMessageImage.setVisibility(View.GONE);

            // Click to open in Maps
            holder.itemView.setOnClickListener(v -> {
                String uri = "geo:" + message.getLatitude() + "," + message.getLongitude() + "?q=" + message.getLatitude() + "," + message.getLongitude();
                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
                context.startActivity(intent);
            });
        } else {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.tvMessage.setText(message.getContent());
            if (holder.ivMessageImage != null) holder.ivMessageImage.setVisibility(View.GONE);
        }

        // Show edited indicator
        String timeText = formatTime(message.getTimestamp().toDate());
        if (message.isEdited()) {
            timeText += " (đã chỉnh sửa)";
        }
        holder.tvTime.setText(timeText);

        // Load sender avatar
        loadSenderAvatar(holder, message);

        // Show reply indicator if replying to another message
        if (message.getReplyToId() != null && message.getReplyToContent() != null) {
            holder.layoutReply.setVisibility(View.VISIBLE);
            holder.tvReplyToName.setText(message.getReplyToSenderName());
            holder.tvReplyToContent.setText(message.getReplyToContent());
        } else {
            holder.layoutReply.setVisibility(View.GONE);
        }

        // Show reactions if any
        if (message.getReactions() != null && !message.getReactions().isEmpty()) {
            holder.layoutReactions.setVisibility(View.VISIBLE);
            holder.tvReactions.setText(formatReactions(message.getReactions()));
        } else {
            holder.layoutReactions.setVisibility(View.GONE);
        }

        // Long click for reply/reaction options
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onMessageLongClick(message);
            }
            return true;
        });
    }

    private void loadSenderAvatar(ReceivedMessageViewHolder holder, Message message) {
        if (message.getSenderAvatar() != null && !message.getSenderAvatar().isEmpty()) {
            Glide.with(context)
                    .load(message.getSenderAvatar())
                    .placeholder(R.drawable.avttest)
                    .into(holder.ivSenderAvatar);
        } else {
            holder.ivSenderAvatar.setImageResource(R.drawable.avttest);
        }
    }

    private void bindSystemMessage(SystemMessageViewHolder holder, Message message) {
        holder.tvSystemMessage.setText(message.getContent());

        // Mặc định system message không click
        holder.itemView.setOnClickListener(null);
        holder.tvSystemMessage.setAlpha(0.7f);

        // Nếu là system message có action OPEN_TRIP → cho click
        if ("OPEN_TRIP".equals(message.getAction()) && message.getActionId() != null) {

            holder.tvSystemMessage.setAlpha(1f);
            holder.tvSystemMessage.setTextColor(
                    context.getResources().getColor(R.color.blue_500, null)
            );

            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onMessageClick(message);
                }
            });
        }
    }


    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<Message> newMessages) {
        this.messages = newMessages != null ? newMessages : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    private String formatTime(java.util.Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    private String formatReactions(java.util.Map<String, String> reactions) {
        java.util.Map<String, Integer> emojiCount = new java.util.HashMap<>();
        for (String emoji : reactions.values()) {
            emojiCount.put(emoji, emojiCount.getOrDefault(emoji, 0) + 1);
        }

        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, Integer> entry : emojiCount.entrySet()) {
            if (sb.length() > 0) sb.append("  ");
            sb.append(entry.getKey()).append(" ").append(entry.getValue());
        }
        return sb.toString();
    }

    // ViewHolder for sent messages
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvReplyToName, tvReplyToContent, tvReactions;
        LinearLayout layoutReply, layoutReactions;
        ImageView ivMessageImage;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvReplyToName = itemView.findViewById(R.id.tvReplyToName);
            tvReplyToContent = itemView.findViewById(R.id.tvReplyToContent);
            tvReactions = itemView.findViewById(R.id.tvReactions);
            layoutReply = itemView.findViewById(R.id.layoutReply);
            layoutReactions = itemView.findViewById(R.id.layoutReactions);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
        }
    }

    // ViewHolder for received messages
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSenderAvatar, ivMessageImage;
        TextView tvSenderName, tvMessage, tvTime, tvReplyToName, tvReplyToContent, tvReactions;
        LinearLayout layoutReply, layoutReactions;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSenderAvatar = itemView.findViewById(R.id.ivSenderAvatar);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvReplyToName = itemView.findViewById(R.id.tvReplyToName);
            tvReplyToContent = itemView.findViewById(R.id.tvReplyToContent);
            tvReactions = itemView.findViewById(R.id.tvReactions);
            layoutReply = itemView.findViewById(R.id.layoutReply);
            layoutReactions = itemView.findViewById(R.id.layoutReactions);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
        }
    }

    // ViewHolder for system messages
    static class SystemMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvSystemMessage;

        SystemMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSystemMessage = itemView.findViewById(R.id.tvSystemMessage);
        }
    }
}
