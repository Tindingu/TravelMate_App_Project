package com.example.testproject1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private Context context;
    private List<TripModel> tripList;
    private OnTripClickListener listener;

    // Interface để xử lý sự kiện click vào item
    public interface OnTripClickListener {
        void onTripClick(TripModel trip);
    }

    public TripAdapter(Context context, List<TripModel> tripList, OnTripClickListener listener) {
        this.context = context;
        this.tripList = tripList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        TripModel trip = tripList.get(position);
        holder.tvTripName.setText(trip.getName());

        // Hiển thị ngày tháng
        String dateRange = trip.getStartDate() + " -> " + trip.getEndDate();
        holder.tvDateRange.setText(dateRange);

        View backgroundLayout = holder.itemView.findViewById(R.id.layoutTripBackground);
        if (position % 2 == 0) {
            backgroundLayout.setBackgroundResource(R.drawable.gradient_blue);
        } else {
            backgroundLayout.setBackgroundResource(R.drawable.gradient_orange);
        }
        // Bắt sự kiện click
        holder.itemView.setOnClickListener(v -> listener.onTripClick(trip));
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvTripName, tvDateRange;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTripName = itemView.findViewById(R.id.tvTripName);
            tvDateRange = itemView.findViewById(R.id.tvDateRange);
        }
    }
}