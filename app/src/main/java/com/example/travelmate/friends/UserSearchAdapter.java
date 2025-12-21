package com.example.travelmate.friends;

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
import com.example.travelmate.common.User;

import java.util.ArrayList;
import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {

    public interface OnAddFriendClickListener {
        void onAddFriendClick(User user, int position);
    }

    private Context context;
    private List<User> users;
    private List<String> friendIds; // Danh sách ID bạn bè hiện tại
    private List<String> pendingRequestIds; // Danh sách ID đã gửi lời mời
    private OnAddFriendClickListener listener;

    public UserSearchAdapter(Context context, List<User> users, List<String> friendIds,
                             List<String> pendingRequestIds) {
        this.context = context;
        this.users = users != null ? users : new ArrayList<>();
        this.friendIds = friendIds != null ? friendIds : new ArrayList<>();
        this.pendingRequestIds = pendingRequestIds != null ? pendingRequestIds : new ArrayList<>();
    }

    public void setOnAddFriendClickListener(OnAddFriendClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_user_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);

        holder.tvName.setText(user.getName() != null ? user.getName() : "Unknown");
        holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        // Load avatar
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(context)
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.avttest)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.avttest);
        }

        // Xác định trạng thái
        String userId = user.getId();

        if (friendIds.contains(userId)) {
            // Đã là bạn bè
            holder.btnAddFriend.setVisibility(View.GONE);
            holder.tvPending.setVisibility(View.GONE);
            holder.tvFriend.setVisibility(View.VISIBLE);
        } else if (pendingRequestIds.contains(userId)) {
            // Đã gửi lời mời
            holder.btnAddFriend.setVisibility(View.GONE);
            holder.tvPending.setVisibility(View.VISIBLE);
            holder.tvFriend.setVisibility(View.GONE);
        } else {
            // Chưa kết bạn
            holder.btnAddFriend.setVisibility(View.VISIBLE);
            holder.tvPending.setVisibility(View.GONE);
            holder.tvFriend.setVisibility(View.GONE);
        }

        // Click listener cho nút kết bạn
        holder.btnAddFriend.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddFriendClick(user, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateUsers(List<User> newUsers) {
        this.users = newUsers != null ? newUsers : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void updateFriendIds(List<String> friendIds) {
        this.friendIds = friendIds != null ? friendIds : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void updatePendingRequestIds(List<String> pendingRequestIds) {
        this.pendingRequestIds = pendingRequestIds != null ? pendingRequestIds : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addPendingRequest(String userId) {
        if (!pendingRequestIds.contains(userId)) {
            pendingRequestIds.add(userId);
            // Tìm vị trí user và cập nhật
            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).getId().equals(userId)) {
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvEmail;
        TextView btnAddFriend, tvPending, tvFriend;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            btnAddFriend = itemView.findViewById(R.id.btnAddFriend);
            tvPending = itemView.findViewById(R.id.tvPending);
            tvFriend = itemView.findViewById(R.id.tvFriend);
        }
    }
}

