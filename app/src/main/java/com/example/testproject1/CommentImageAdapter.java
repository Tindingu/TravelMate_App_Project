package com.example.testproject1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class CommentImageAdapter extends RecyclerView.Adapter<CommentImageAdapter.ImgVH> {

    ArrayList<String> images = new ArrayList<>();

    public void setImages(ArrayList<String> list) {
        images.clear();
        images.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public ImgVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment_image, parent, false);
        return new ImgVH(v);
    }

    @Override
    public void onBindViewHolder(ImgVH h, int pos) {
        Glide.with(h.itemView.getContext()).load(images.get(pos)).into(h.img);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    class ImgVH extends RecyclerView.ViewHolder {
        ImageView img;
        public ImgVH(View v) {
            super(v);
            img = v.findViewById(R.id.imgCmt);
        }
    }
}
