//package com.example.testproject1;
//
//import android.text.format.DateFormat;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.RatingBar;
//import android.widget.TextView;
//
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import java.util.ArrayList;
//import java.util.Date;
//
//public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
//
//    ArrayList<CommentModel> list;
//
//    public CommentAdapter(ArrayList<CommentModel> list) {
//        this.list = list;
//    }
//
//    @Override
//    public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        View v = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_comment, parent, false);
//        return new CommentViewHolder(v);
//    }
//
//    @Override
//    public void onBindViewHolder(CommentViewHolder h, int pos) {
//        CommentModel c = list.get(pos);
//
//        // ========================
//        // Map đúng field CommentModel
//        // ========================
//        h.tvUser.setText(c.getUser());
//        h.tvMessage.setText(c.getMessage());
//        h.ratingBar.setRating(c.getRating());
//
//        String time = DateFormat.format("dd/MM/yyyy • HH:mm",
//                new Date(c.getTimestamp())).toString();
//        h.tvTime.setText(time);
//
//        // ========================
//        // Load MULTI IMAGE LIST
//        // ========================
//        if (c.getImageUrls() != null && !c.getImageUrls().isEmpty()) {
//            h.rvImages.setVisibility(View.VISIBLE);
//            h.imgAdapter.setImages(c.getImageUrls());
//        } else {
//            h.rvImages.setVisibility(View.GONE);
//        }
//    }
//
//    @Override
//    public int getItemCount() {
//        return list.size();
//    }
//
//    class CommentViewHolder extends RecyclerView.ViewHolder {
//
//        TextView tvUser, tvMessage, tvTime;
//        RatingBar ratingBar;
//        RecyclerView rvImages;
//        CommentImageAdapter imgAdapter;
//
//        public CommentViewHolder(View v) {
//            super(v);
//
//            tvUser = v.findViewById(R.id.tvUser);
//            tvMessage = v.findViewById(R.id.tvMessage);
//            tvTime = v.findViewById(R.id.tvTime);
//            ratingBar = v.findViewById(R.id.ratingUser);
//
//            rvImages = v.findViewById(R.id.rvCommentImages);
//            rvImages.setLayoutManager(
//                    new LinearLayoutManager(v.getContext(), LinearLayoutManager.HORIZONTAL, false)
//            );
//
//            imgAdapter = new CommentImageAdapter();
//            rvImages.setAdapter(imgAdapter);
//        }
//    }
//}
package com.example.testproject1;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private ArrayList<CommentModel> list;
    private String placeId; // cần truyền từ PlaceDetailActivity

    public CommentAdapter(ArrayList<CommentModel> list, String placeId) {
        this.list = list;
        this.placeId = placeId;
    }

    @Override
    public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(CommentViewHolder h, int pos) {
        CommentModel c = list.get(pos);
        String uid = FirebaseAuth.getInstance().getUid();

        // ========================
        // SET TEXT + RATING
        // ========================
        h.tvUser.setText(c.getUser());
        h.tvMessage.setText(c.getMessage());
        h.ratingBar.setRating(c.getRating());

        String time = DateFormat.format("dd/MM/yyyy • HH:mm",
                new Date(c.getTimestamp())).toString();
        h.tvTime.setText(time);

        // ========================
        // MULTI IMAGE
        // ========================
        if (c.getImageUrls() != null && !c.getImageUrls().isEmpty()) {
            h.rvImages.setVisibility(View.VISIBLE);
            h.imgAdapter.setImages(c.getImageUrls());
        } else {
            h.rvImages.setVisibility(View.GONE);
        }

        // ========================
        // LIKE / DISLIKE HIỂN THỊ
        // ========================
        boolean isLiked = c.getLikedBy().contains(uid);
        boolean isDisliked = c.getDislikedBy().contains(uid);

        h.icLike.setImageResource(isLiked ? R.drawable.liked : R.drawable.like);
        h.icDislike.setImageResource(isDisliked ? R.drawable.disliked : R.drawable.dislike);

        h.tvLikeCount.setText(String.valueOf(c.getLikeCount()));
        h.tvDislikeCount.setText(String.valueOf(c.getDislikeCount()));

        // ========================
        // CLICK LIKE
        // ========================
        h.btnLike.setOnClickListener(v -> {
            if (isLiked) {
                // Bỏ Like
                c.getLikedBy().remove(uid);
                c.setLikeCount(c.getLikeCount() - 1);
            } else {
                // Like
                c.getLikedBy().add(uid);
                c.setLikeCount(c.getLikeCount() + 1);

                // Nếu đang dislike → bỏ
                if (isDisliked) {
                    c.getDislikedBy().remove(uid);
                    c.setDislikeCount(c.getDislikeCount() - 1);
                }
            }

            updateComment(c);
        });

        // ========================
        // CLICK DISLIKE
        // ========================
        h.btnDislike.setOnClickListener(v -> {
            if (isDisliked) {
                c.getDislikedBy().remove(uid);
                c.setDislikeCount(c.getDislikeCount() - 1);
            } else {
                c.getDislikedBy().add(uid);
                c.setDislikeCount(c.getDislikeCount() + 1);

                if (isLiked) {
                    c.getLikedBy().remove(uid);
                    c.setLikeCount(c.getLikeCount() - 1);
                }
            }

            updateComment(c);
        });
    }

    // ========================
    // FIRESTORE UPDATE
    // ========================
    private void updateComment(CommentModel c) {
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
                .addOnSuccessListener(a -> notifyDataSetChanged());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    // ========================
    // VIEW HOLDER
    // ========================
    class CommentViewHolder extends RecyclerView.ViewHolder {

        TextView tvUser, tvMessage, tvTime, tvLikeCount, tvDislikeCount;
        RatingBar ratingBar;
        ImageView icLike, icDislike;
        RecyclerView rvImages;
        CommentImageAdapter imgAdapter;
        View btnLike, btnDislike;

        public CommentViewHolder(View v) {
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

            rvImages = v.findViewById(R.id.rvCommentImages);
            rvImages.setLayoutManager(
                    new LinearLayoutManager(v.getContext(), LinearLayoutManager.HORIZONTAL, false)
            );

            imgAdapter = new CommentImageAdapter();
            rvImages.setAdapter(imgAdapter);
        }
    }
}
