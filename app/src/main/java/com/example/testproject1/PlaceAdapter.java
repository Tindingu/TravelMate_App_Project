package com.example.testproject1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

    private ArrayList<PlaceModel> list;
    // Interface để xử lý khi bấm vào item
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(PlaceModel place);
    }

    public PlaceAdapter(ArrayList<PlaceModel> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaceModel place = list.get(position);
        holder.tvName.setText(place.getName());
        holder.tvAddress.setText(place.getAddress());

        // Giả lập hiển thị rating
        holder.tvRating.setText(String.format("%.1f ⭐ (Random)", place.getRating()));

        holder.itemView.setOnClickListener(v -> listener.onItemClick(place));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPlaceName);
            tvAddress = itemView.findViewById(R.id.tvPlaceAddress);
            tvRating = itemView.findViewById(R.id.tvPlaceRating);
        }
    }
}