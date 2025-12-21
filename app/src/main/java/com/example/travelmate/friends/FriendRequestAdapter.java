package com.example.travelmate.friends;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travelmate.R;
import com.example.travelmate.friends.FriendRequest;

import java.util.ArrayList;
import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(FriendRequest request, int position);
        void onReject(FriendRequest request, int position);
        void onCancel(FriendRequest request, int position);
    }

    private Context context;
    private List<FriendRequest> requests;
    private boolean isReceivedTab; // true = tab Đã nhận, false = tab Đã gửi
    private OnRequestActionListener listener;

    public FriendRequestAdapter(Context context, List<FriendRequest> requests, boolean isReceivedTab) {
        this.context = context;
        this.requests = requests != null ? requests : new ArrayList<>();
        this.isReceivedTab = isReceivedTab;
    }

    public void setOnRequestActionListener(OnRequestActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_friend_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendRequest request = requests.get(position);

        if (isReceivedTab) {
            // Tab Đã nhận - hiển thị thông tin người gửi
            holder.tvName.setText(request.getSenderName() != null ?
                    request.getSenderName() : "Unknown");
            holder.tvEmail.setText(request.getSenderEmail() != null ?
                    request.getSenderEmail() : "");

            // Load avatar người gửi
            if (request.getSenderAvatar() != null && !request.getSenderAvatar().isEmpty()) {
                Glide.with(context)
                        .load(request.getSenderAvatar())
                        .placeholder(R.drawable.avttest)
                        .circleCrop()
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.avttest);
            }

            // Hiển thị nút Accept/Reject
            holder.layoutReceivedActions.setVisibility(View.VISIBLE);
            holder.btnCancel.setVisibility(View.GONE);
        } else {
            // Tab Đã gửi - hiển thị thông tin người nhận
            holder.tvName.setText(request.getReceiverName() != null ?
                    request.getReceiverName() : "Unknown");
            holder.tvEmail.setText(request.getReceiverEmail() != null ?
                    request.getReceiverEmail() : "");

            // Load avatar người nhận
            if (request.getReceiverAvatar() != null && !request.getReceiverAvatar().isEmpty()) {
                Glide.with(context)
                        .load(request.getReceiverAvatar())
                        .placeholder(R.drawable.avttest)
                        .circleCrop()
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.avttest);
            }

            // Hiển thị nút Cancel
            holder.layoutReceivedActions.setVisibility(View.GONE);
            holder.btnCancel.setVisibility(View.VISIBLE);
        }

        // Click listeners
        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccept(request, position);
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReject(request, position);
            }
        });

        holder.btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancel(request, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public void updateRequests(List<FriendRequest> newRequests) {
        this.requests = newRequests != null ? newRequests : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < requests.size()) {
            requests.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvEmail;
        LinearLayout layoutReceivedActions;
        TextView btnAccept, btnReject, btnCancel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            layoutReceivedActions = itemView.findViewById(R.id.layoutReceivedActions);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}

