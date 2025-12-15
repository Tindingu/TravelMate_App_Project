package com.example.testproject1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

    private ArrayList<PlaceModel> list;

    private OnItemClickListener itemListener;
    private OnFavoriteClickListener favListener;
    private OnDirectionClickListener directionListener;
    private OnAddToTripClickListener addTripListener;

    // ----------------------------
    // INTERFACES
    // ----------------------------
    public interface OnItemClickListener {
        void onItemClick(PlaceModel place);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(PlaceModel place);
    }

    public interface OnDirectionClickListener {
        void onDirectionClick(PlaceModel place);
    }
    public interface OnAddToTripClickListener {
        void onAddToTrip(PlaceModel place);
    }

    public PlaceAdapter(ArrayList<PlaceModel> list,
                        OnItemClickListener itemListener,
                        OnFavoriteClickListener favListener,
                        OnDirectionClickListener directionListener,
                        OnAddToTripClickListener addTripListener) {

        this.list = list;
        this.itemListener = itemListener;
        this.favListener = favListener;
        this.directionListener = directionListener;
        this.addTripListener = addTripListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaceModel place = list.get(position);

        holder.tvName.setText(place.getName());
        holder.tvAddress.setText(place.getAddress());
        holder.tvRating.setText(String.format("%.1f ⭐", place.getRating()));

        // 1. TIM (Favorite)
        holder.btnFavorite.setImageResource(
                place.isFavorite() ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline
        );
        holder.btnFavorite.setOnClickListener(v -> {
            boolean newState = !place.isFavorite();
            place.setFavorite(newState);
            holder.btnFavorite.setImageResource(
                    newState ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline
            );
            favListener.onFavoriteClick(place);
        });

        // 2. CLICK ITEM (Detail)
        holder.itemView.setOnClickListener(v -> itemListener.onItemClick(place));

        // 3. ĐƯỜNG ĐI (Direction)
        holder.tvDirection.setOnClickListener(v -> directionListener.onDirectionClick(place));

        // 4. ⭐ MỚI: CLICK NÚT LỊCH (Add to Trip)

        if (addTripListener != null) {
            holder.btnAddToTrip.setVisibility(View.VISIBLE);
            holder.btnAddToTrip.setOnClickListener(v -> addTripListener.onAddToTrip(place));
        } else {
            holder.btnAddToTrip.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ----------------------------
    // VIEW HOLDER
    // ----------------------------
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvAddress, tvRating, tvDirection;
        ImageView btnFavorite;
        ImageView btnAddToTrip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvPlaceName);
            tvAddress = itemView.findViewById(R.id.tvPlaceAddress);
            tvRating = itemView.findViewById(R.id.tvPlaceRating);
            tvDirection = itemView.findViewById(R.id.btnDirection);

            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnAddToTrip = itemView.findViewById(R.id.btnAddToTrip);
        }
    }
}
