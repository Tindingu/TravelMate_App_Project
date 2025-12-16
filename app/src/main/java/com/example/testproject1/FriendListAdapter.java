package com.example.testproject1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testproject1.models.User;

import java.util.ArrayList;
import java.util.List;

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.ViewHolder> {

    public interface OnFriendActionListener {
        void onStartChat(User friend, int position);
        void onAddToGroup(User friend, int position);
        void onRemoveFriend(User friend, int position);
    }

    private Context context;
    private List<User> friends;
    private OnFriendActionListener listener;

    public FriendListAdapter(Context context, List<User> friends) {
        this.context = context;
        this.friends = friends != null ? friends : new ArrayList<>();
    }

    public void setOnFriendActionListener(OnFriendActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User friend = friends.get(position);

        holder.tvName.setText(friend.getName() != null ? friend.getName() : "Unknown");
        holder.tvEmail.setText(friend.getEmail() != null ? friend.getEmail() : "");

        // Load avatar
        if (friend.getAvatarUrl() != null && !friend.getAvatarUrl().isEmpty()) {
            Glide.with(context)
                    .load(friend.getAvatarUrl())
                    .placeholder(R.drawable.avttest)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.avttest);
        }

        // Menu 3 chấm
        holder.btnMenu.setOnClickListener(v -> {
            showPopupMenu(v, friend, position);
        });
    }

    private void showPopupMenu(View anchor, User friend, int position) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_friend_options, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_start_chat) {
                if (listener != null) {
                    listener.onStartChat(friend, position);
                }
                return true;
            } else if (itemId == R.id.action_add_to_group) {
                if (listener != null) {
                    listener.onAddToGroup(friend, position);
                }
                return true;
            } else if (itemId == R.id.action_remove_friend) {
                if (listener != null) {
                    listener.onRemoveFriend(friend, position);
                }
                return true;
            }
            return false;
        });

        popup.show();
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    public void updateFriends(List<User> newFriends) {
        this.friends = newFriends != null ? newFriends : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeFriend(int position) {
        if (position >= 0 && position < friends.size()) {
            friends.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, btnMenu;
        TextView tvName, tvEmail;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }
    }
}

