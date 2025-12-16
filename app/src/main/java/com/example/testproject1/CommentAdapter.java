package com.example.testproject1;

import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final ArrayList<CommentModel> list;
    private final String placeId;

    // =========================
    // USER CACHE (STATIC – dùng chung)
    // =========================
    private static final Map<String, UserCache> userCache = new HashMap<>();

    static class UserCache {
        String name;
        String avatarUrl;
    }

    public CommentAdapter(ArrayList<CommentModel> list, String placeId) {
        this.list = list;
        this.placeId = placeId;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder h, int pos) {
        CommentModel c = list.get(pos);
        String commentUid = c.getUid();
        String currentUid = FirebaseAuth.getInstance().getUid();

        if (commentUid == null) return;

// 👉 USER ĐANG ĐĂNG NHẬP → REALTIME
        if (commentUid.equals(currentUid)) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(commentUid)
                    .addSnapshotListener((snap, e) -> {
                        if (snap == null || !snap.exists()) return;

                        UserCache u = new UserCache();
                        u.name = snap.getString("name");
                        u.avatarUrl = snap.getString("photoUrl");

                        userCache.put(commentUid, u);
                        bindUser(h, u);
                    });
        }
// 👉 USER KHÁC → LOAD 1 LẦN + CACHE
        else {
            UserCache cached = userCache.get(commentUid);
            if (cached != null) {
                bindUser(h, cached);
            } else {
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(commentUid)
                        .get()
                        .addOnSuccessListener(snap -> {
                            if (!snap.exists()) return;

                            UserCache u = new UserCache();
                            u.name = snap.getString("name");
                            u.avatarUrl = snap.getString("photoUrl");

                            userCache.put(commentUid, u);
                            bindUser(h, u);
                        });
            }
        }


        // =========================
        // CONTENT
        // =========================
        h.tvMessage.setText(c.getMessage());
        h.ratingBar.setRating(c.getRating());

        String time = DateFormat.format(
                "dd/MM/yyyy • HH:mm",
                new Date(c.getTimestamp())
        ).toString();
        h.tvTime.setText(time);

        // =========================
        // MULTI IMAGE
        // =========================
        if (c.getImageUrls() != null && !c.getImageUrls().isEmpty()) {
            h.rvImages.setVisibility(View.VISIBLE);
            h.imgAdapter.setImages(c.getImageUrls());
        } else {
            h.rvImages.setVisibility(View.GONE);
        }

        // =========================
        // LIKE / DISLIKE STATE
        // =========================
        boolean isLiked = c.getLikedBy().contains(currentUid);
        boolean isDisliked = c.getDislikedBy().contains(currentUid);

        h.icLike.setImageResource(isLiked ? R.drawable.liked : R.drawable.like);
        h.icDislike.setImageResource(isDisliked ? R.drawable.disliked : R.drawable.dislike);

        h.tvLikeCount.setText(String.valueOf(c.getLikeCount()));
        h.tvDislikeCount.setText(String.valueOf(c.getDislikeCount()));

        // =========================
        // CLICK LIKE
        // =========================
        h.btnLike.setOnClickListener(v -> {
            if (isLiked) {
                c.getLikedBy().remove(currentUid);
                c.setLikeCount(c.getLikeCount() - 1);
            } else {
                c.getLikedBy().add(currentUid);
                c.setLikeCount(c.getLikeCount() + 1);

                if (isDisliked) {
                    c.getDislikedBy().remove(currentUid);
                    c.setDislikeCount(c.getDislikeCount() - 1);
                }
            }
            updateComment(c, h.getAdapterPosition());
        });

        // =========================
        // CLICK DISLIKE
        // =========================
        h.btnDislike.setOnClickListener(v -> {
            if (isDisliked) {
                c.getDislikedBy().remove(currentUid);
                c.setDislikeCount(c.getDislikeCount() - 1);
            } else {
                c.getDislikedBy().add(currentUid);
                c.setDislikeCount(c.getDislikeCount() + 1);

                if (isLiked) {
                    c.getLikedBy().remove(currentUid);
                    c.setLikeCount(c.getLikeCount() - 1);
                }
            }
            updateComment(c, h.getAdapterPosition());
        });
        h.ivAvatar.setOnClickListener(v -> {
            if (c.getUid() == null) return;

            Intent intent = new Intent(
                    h.itemView.getContext(),
                    ProfilePublicActivity.class
            );
            intent.putExtra("uid", c.getUid());
            h.itemView.getContext().startActivity(intent);
        });

        h.tvUser.setOnClickListener(v -> {
            if (c.getUid() == null) return;

            Intent intent = new Intent(
                    h.itemView.getContext(),
                    ProfilePublicActivity.class
            );
            intent.putExtra("uid", c.getUid());
            h.itemView.getContext().startActivity(intent);
        });

    }

    // =========================
    // BIND USER UI
    // =========================
    private void bindUser(CommentViewHolder h, UserCache u) {
        h.tvUser.setText(u.name != null ? u.name : "Người dùng");

        if (u.avatarUrl != null && !u.avatarUrl.isEmpty()) {
            Glide.with(h.itemView.getContext())
                    .load(u.avatarUrl)
                    .placeholder(R.drawable.avttest)
                    .circleCrop()
                    .into(h.ivAvatar);
        } else {
            h.ivAvatar.setImageResource(R.drawable.avttest);
        }
    }

    // =========================
    // UPDATE FIRESTORE
    // =========================
    private void updateComment(CommentModel c, int position) {
        FirebaseFirestore.getInstance()
                .collection("places")
                .document(placeId)
                .collection("comments")
                .document(c.getId())
                .update(
                        "likedBy", c.getLikedBy(),
                        "dislikedBy", c.getDislikedBy(),
                        "likeCount", c.getLikeCount(),
                        "dislikeCount", c.getDislikeCount()
                )
                .addOnSuccessListener(a -> notifyItemChanged(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // =========================
    // VIEW HOLDER
    // =========================
    static class CommentViewHolder extends RecyclerView.ViewHolder {

        TextView tvUser, tvMessage, tvTime, tvLikeCount, tvDislikeCount;
        RatingBar ratingBar;
        ImageView icLike, icDislike, ivAvatar;
        RecyclerView rvImages;
        CommentImageAdapter imgAdapter;
        View btnLike, btnDislike;

        public CommentViewHolder(@NonNull View v) {
            super(v);

            tvUser = v.findViewById(R.id.tvUser);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvTime = v.findViewById(R.id.tvTime);

            ratingBar = v.findViewById(R.id.ratingUser);

            icLike = v.findViewById(R.id.icLike);
            icDislike = v.findViewById(R.id.icDislike);

            tvLikeCount = v.findViewById(R.id.tvLikeCount);
            tvDislikeCount = v.findViewById(R.id.tvDislikeCount);

            btnLike = v.findViewById(R.id.btnLike);
            btnDislike = v.findViewById(R.id.btnDislike);

            ivAvatar = v.findViewById(R.id.imgAvatar);

            rvImages = v.findViewById(R.id.rvCommentImages);
            rvImages.setLayoutManager(
                    new LinearLayoutManager(
                            v.getContext(),
                            LinearLayoutManager.HORIZONTAL,
                            false
                    )
            );

            imgAdapter = new CommentImageAdapter();
            rvImages.setAdapter(imgAdapter);
        }
    }
}
