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
    private OnItemClickListener listener;      // Click item để xem chi tiết
    private OnActionListener actionListener;   // ⭐ MỚI: Xử lý cả Xóa và Sửa

    // Interface xem chi tiết (giữ nguyên)
    public interface OnItemClickListener {
        void onClick(ScheduleItemModel item);
    }

    // ⭐ MỚI: Interface gộp chung cho các hành động (Action)
    public interface OnActionListener {
        void onDelete(ScheduleItemModel item, int position);
        void onEdit(ScheduleItemModel item, int position); // Thêm hàm Edit
    }

    // ⭐ Cập nhật Constructor: Nh nhận OnActionListener thay vì OnDeleteClickListener
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

        holder.tvTime.setText(item.getVisitTime());
        holder.tvDate.setText(item.getVisitDate());
        holder.tvName.setText(item.getPlaceName());
        holder.tvAddress.setText(item.getPlaceAddress());

        if (item.getNote() != null && !item.getNote().isEmpty()) {
            holder.tvNote.setVisibility(View.VISIBLE);
            holder.tvNote.setText(item.getNote());
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        // 1. Click vào cả dòng -> Xem chi tiết
        holder.itemView.setOnClickListener(v -> listener.onClick(item));

        // 2. Click nút Xóa -> Gọi hàm onDelete
        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDelete(item, position);
            }
        });

        // 3. ⭐ MỚI: Click nút Sửa -> Gọi hàm onEdit
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
        ImageView btnEdit; // ⭐ MỚI: Khai báo nút sửa

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvName = itemView.findViewById(R.id.tvPlaceName);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            // ⭐ MỚI: Ánh xạ nút sửa (đảm bảo trong item_schedule.xml đã có id btnEdit)
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}