package com.example.travelmate.chat;

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

public class GroupMemberAdapter extends RecyclerView.Adapter<GroupMemberAdapter.ViewHolder> {

    public interface OnRemoveMemberClickListener {
        void onRemoveMemberClick(User member, int position);
    }

    private Context context;
    private List<User> members;
    private List<String> adminIds;
    private String currentUserId;
    private boolean isCurrentUserAdmin;
    private OnRemoveMemberClickListener listener;

    public GroupMemberAdapter(Context context, List<User> members, List<String> adminIds,
                              String currentUserId, boolean isCurrentUserAdmin) {
        this.context = context;
        this.members = members != null ? members : new ArrayList<>();
        this.adminIds = adminIds != null ? adminIds : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.isCurrentUserAdmin = isCurrentUserAdmin;
    }

    public void setOnRemoveMemberClickListener(OnRemoveMemberClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_group_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User member = members.get(position);

        holder.tvName.setText(member.getName() != null ? member.getName() : "Unknown");
        holder.tvEmail.setText(member.getEmail() != null ? member.getEmail() : "");

        // Load avatar
        if (member.getAvatarUrl() != null && !member.getAvatarUrl().isEmpty()) {
            Glide.with(context)
                    .load(member.getAvatarUrl())
                    .placeholder(R.drawable.avttest)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.avttest);
        }

        // Hiển thị badge Admin nếu là admin
        if (adminIds.contains(member.getId())) {
            holder.tvAdminBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvAdminBadge.setVisibility(View.GONE);
        }

        // Hiển thị nút xóa nếu:
        // 1. Current user là admin
        // 2. Member không phải là chính mình
        // 3. Member không phải là admin (hoặc current user là creator)
        boolean canRemove = isCurrentUserAdmin &&
                !member.getId().equals(currentUserId) &&
                !adminIds.contains(member.getId());

        if (canRemove) {
            holder.btnRemoveMember.setVisibility(View.VISIBLE);
            holder.btnRemoveMember.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveMemberClick(member, position);
                }
            });
        } else {
            holder.btnRemoveMember.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public void updateMembers(List<User> newMembers) {
        this.members = newMembers != null ? newMembers : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void updateAdminIds(List<String> adminIds) {
        this.adminIds = adminIds != null ? adminIds : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeMember(int position) {
        if (position >= 0 && position < members.size()) {
            members.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, btnRemoveMember;
        TextView tvName, tvEmail, tvAdminBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvAdminBadge = itemView.findViewById(R.id.tvAdminBadge);
            btnRemoveMember = itemView.findViewById(R.id.btnRemoveMember);
        }
    }
}

