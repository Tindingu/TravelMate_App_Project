package com.example.travelmate.friends;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travelmate.R;

import java.util.List;
import java.util.Map;

public class UserSelectionAdapter extends RecyclerView.Adapter<UserSelectionAdapter.ViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private Context context;
    private List<Map<String, Object>> users;
    private List<String> selectedUserIds;
    private OnSelectionChangedListener selectionListener;

    public UserSelectionAdapter(Context context, List<Map<String, Object>> users,
                                 List<String> selectedUserIds) {
        this.context = context;
        this.users = users;
        this.selectedUserIds = selectedUserIds;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_user_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> user = users.get(position);

        String userId = (String) user.get("userId");
        String name = (String) user.get("name");
        String email = (String) user.get("email");
        String avatar = (String) user.get("avatar");

        holder.tvName.setText(name != null ? name : "Unknown");
        holder.tvEmail.setText(email != null ? email : "");

        // Load avatar
        if (avatar != null && !avatar.isEmpty()) {
            Glide.with(context)
                    .load(avatar)
                    .placeholder(R.drawable.avttest)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.avttest);
        }

        // Check state
        holder.cbSelect.setChecked(selectedUserIds.contains(userId));

        // Toggle selection
        holder.itemView.setOnClickListener(v -> {
            if (selectedUserIds.contains(userId)) {
                selectedUserIds.remove(userId);
                holder.cbSelect.setChecked(false);
            } else {
                selectedUserIds.add(userId);
                holder.cbSelect.setChecked(true);
            }
            notifySelectionChanged();
        });

        holder.cbSelect.setOnClickListener(v -> {
            if (holder.cbSelect.isChecked()) {
                if (!selectedUserIds.contains(userId)) {
                    selectedUserIds.add(userId);
                }
            } else {
                selectedUserIds.remove(userId);
            }
            notifySelectionChanged();
        });
    }

    private void notifySelectionChanged() {
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedUserIds.size());
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvEmail;
        CheckBox cbSelect;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            cbSelect = itemView.findViewById(R.id.cbSelect);
        }
    }
}

