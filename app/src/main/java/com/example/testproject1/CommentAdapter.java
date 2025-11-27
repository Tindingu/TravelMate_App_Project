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

import java.util.ArrayList;
import java.util.Date;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    ArrayList<CommentModel> list;

    public CommentAdapter(ArrayList<CommentModel> list) {
        this.list = list;
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

        // ========================
        // Map đúng field CommentModel
        // ========================
        h.tvUser.setText(c.getUser());
        h.tvMessage.setText(c.getMessage());
        h.ratingBar.setRating(c.getRating());

        String time = DateFormat.format("dd/MM/yyyy • HH:mm",
                new Date(c.getTimestamp())).toString();
        h.tvTime.setText(time);

        // ========================
        // Load MULTI IMAGE LIST
        // ========================
        if (c.getImageUrls() != null && !c.getImageUrls().isEmpty()) {
            h.rvImages.setVisibility(View.VISIBLE);
            h.imgAdapter.setImages(c.getImageUrls());
        } else {
            h.rvImages.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {

        TextView tvUser, tvMessage, tvTime;
        RatingBar ratingBar;
        RecyclerView rvImages;
        CommentImageAdapter imgAdapter;

        public CommentViewHolder(View v) {
            super(v);

            tvUser = v.findViewById(R.id.tvUser);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvTime = v.findViewById(R.id.tvTime);
            ratingBar = v.findViewById(R.id.ratingUser);

            rvImages = v.findViewById(R.id.rvCommentImages);
            rvImages.setLayoutManager(
                    new LinearLayoutManager(v.getContext(), LinearLayoutManager.HORIZONTAL, false)
            );

            imgAdapter = new CommentImageAdapter();
            rvImages.setAdapter(imgAdapter);
        }
    }
}
