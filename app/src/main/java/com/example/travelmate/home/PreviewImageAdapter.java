package com.example.travelmate.home;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.travelmate.R;

import java.util.ArrayList;

public class PreviewImageAdapter extends RecyclerView.Adapter<PreviewImageAdapter.Holder> {

    ArrayList<Uri> images;
    OnRemoveClick listener;

    public interface OnRemoveClick {
        void onRemove(int position);
    }

    public PreviewImageAdapter(ArrayList<Uri> images, OnRemoveClick listener) {
        this.images = images;
        this.listener = listener;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_preview_image, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder h, int pos) {
        Uri uri = images.get(pos);
        h.img.setImageURI(uri);

        h.btnRemove.setOnClickListener(v -> listener.onRemove(pos));
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    class Holder extends RecyclerView.ViewHolder {
        ImageView img, btnRemove;

        Holder(View v) {
            super(v);
            img = v.findViewById(R.id.imgPreviewItem);
            btnRemove = v.findViewById(R.id.btnRemovePreview);
        }
    }
}
