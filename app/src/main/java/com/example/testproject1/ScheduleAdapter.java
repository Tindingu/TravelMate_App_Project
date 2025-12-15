package com.example.testproject1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private List<ScheduleItemModel> list;
    private OnItemClickListener listener;
    private OnActionListener actionListener;

    // Interface xem chi tiết
    public interface OnItemClickListener {
        void onClick(ScheduleItemModel item);
    }

    // Interface gộp chung cho các hành động (Xóa, Sửa)
    public interface OnActionListener {
        void onDelete(ScheduleItemModel item, int position);
        void onEdit(ScheduleItemModel item, int position);
    }

    public ScheduleAdapter(List<ScheduleItemModel> list, OnItemClickListener listener, OnActionListener actionListener) {
        this.list = list;
        this.listener = listener;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleItemModel item = list.get(position);

        // ⭐ SỬA ĐOẠN NÀY: Hiển thị cả Giờ Bắt Đầu và Kết Thúc
        String timeDisplay = item.getVisitTime(); // Giờ bắt đầu

        // Nếu có giờ kết thúc thì nối thêm vào
        if (item.getEndTime() != null && !item.getEndTime().isEmpty()) {
            // Dùng \n để xuống dòng cho đẹp trong cột hẹp
            timeDisplay = timeDisplay + " - " + item.getEndTime();
        }

        holder.tvTime.setText(timeDisplay);
        // ----------------------------------------------------

        holder.tvDate.setText(item.getVisitDate());
        holder.tvName.setText(item.getPlaceName());
        holder.tvAddress.setText(item.getPlaceAddress());

        if (item.getNote() != null && !item.getNote().isEmpty()) {
            holder.tvNote.setVisibility(View.VISIBLE);
            holder.tvNote.setText(item.getNote());
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        // 1. Click item -> Xem chi tiết
        holder.itemView.setOnClickListener(v -> listener.onClick(item));

        // 2. Click Xóa
        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDelete(item, position);
            }
        });

        // 3. Click Sửa
        holder.btnEdit.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onEdit(item, position);
            }
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvDate, tvName, tvNote, tvAddress;
        ImageView btnDelete;
        ImageView btnEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvName = itemView.findViewById(R.id.tvPlaceName);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}