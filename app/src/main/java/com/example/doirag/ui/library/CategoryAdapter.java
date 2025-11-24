package com.example.doirag.ui.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doirag.R;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

    private final List<CategoryItem> items;
    private final OnCategoryClick listener;

    // Interface diperbarui untuk mengirim 2 String
    public interface OnCategoryClick {
        void onClick(String filterValue, String pageTitle);
    }

    public CategoryAdapter(List<CategoryItem> items, OnCategoryClick listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CategoryItem item = items.get(position);

        // Tampilkan nama pendek di kartu
        holder.tvName.setText(item.displayName);
        holder.imgIcon.setImageResource(item.iconResId);

        holder.itemView.setOnClickListener(v -> {
            // Saat diklik, kirim dbValue (untuk filter database)
            // dan displayName (untuk judul header halaman selanjutnya)
            listener.onClick(item.dbValue, item.displayName);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView imgIcon;

        VH(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            imgIcon = itemView.findViewById(R.id.imgIcon);
        }
    }
}