package com.example.testproject1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // Nhớ import ImageView
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

    private ArrayList<PlaceModel> list;
    private OnItemClickListener itemListener;
    private OnFavoriteClickListener favListener; // ⭐ Listener mới cho nút Tim

    // Interface 1: Xử lý khi bấm vào cả cái thẻ (để Zoom Map)
    public interface OnItemClickListener {
        void onItemClick(PlaceModel place);
    }

    // Interface 2: Xử lý khi bấm vào trái tim (Add/Remove Wishlist)
    public interface OnFavoriteClickListener {
        void onFavoriteClick(PlaceModel place);
    }

    // Constructor cập nhật: Nhận thêm favListener
    public PlaceAdapter(ArrayList<PlaceModel> list, OnItemClickListener itemListener, OnFavoriteClickListener favListener) {
        this.list = list;
        this.itemListener = itemListener;
        this.favListener = favListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout item_place.xml
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaceModel place = list.get(position);

        // Gán dữ liệu text
        holder.tvName.setText(place.getName());
        holder.tvAddress.setText(place.getAddress());
        holder.tvRating.setText(String.format("%.1f ⭐", place.getRating()));

        // ⭐ LOGIC HIỂN THỊ TRÁI TIM
        // Kiểm tra trạng thái isFavorite trong Model để hiện tim Đỏ hay Rỗng
        if (place.isFavorite()) {
            holder.btnFavorite.setImageResource(R.drawable.ic_heart_filled); // Tim đỏ
        } else {
            holder.btnFavorite.setImageResource(R.drawable.ic_heart_outline); // Tim rỗng
        }

        // ⭐ SỰ KIỆN BẤM NÚT TIM
        holder.btnFavorite.setOnClickListener(v -> {
            // 1. Đổi trạng thái trong Model (True <-> False)
            boolean newState = !place.isFavorite();
            place.setFavorite(newState);

            // 2. Cập nhật giao diện ngay lập tức cho mượt
            if (newState) {
                holder.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
            } else {
                holder.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
            }

            // 3. Gọi về HomeActivity để lưu vào Firestore
            favListener.onFavoriteClick(place);
        });

        // Sự kiện bấm vào item (Zoom map) - Giữ nguyên
        holder.itemView.setOnClickListener(v -> itemListener.onItemClick(place));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ViewHolder cập nhật
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvRating;
        ImageView btnFavorite; // ⭐ Thêm biến nút tim

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPlaceName);
            tvAddress = itemView.findViewById(R.id.tvPlaceAddress);
            tvRating = itemView.findViewById(R.id.tvPlaceRating);

            // ⭐ Ánh xạ nút tim từ layout item_place.xml
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}