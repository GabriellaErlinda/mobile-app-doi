package com.example.doirag.ui.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doirag.R;

import java.util.Objects;

public class ObatSediaanAdapter extends ListAdapter<ObatSediaanItem, ObatSediaanAdapter.VH> {

    public interface OnDrugClick {
        void onClick(ObatSediaanItem item); // Diubah ke ObatSediaanItem
    }

    private final OnDrugClick listener;

    public ObatSediaanAdapter(OnDrugClick listener) { // Constructor diubah
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ObatSediaanItem> DIFF =
            new DiffUtil.ItemCallback<ObatSediaanItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull ObatSediaanItem oldItem, @NonNull ObatSediaanItem newItem) {
                    return Objects.equals(oldItem.id, newItem.id);
                }
                @Override
                public boolean areContentsTheSame(@NonNull ObatSediaanItem oldItem, @NonNull ObatSediaanItem newItem) {
                    return oldItem.equals(newItem);
                }
            };

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drug, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ObatSediaanItem item = getItem(pos); // Diubah ke ObatSediaanItem
        h.bind(item);
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(item);
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView textName, textMeta;
        VH(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textMeta = itemView.findViewById(R.id.textMeta);
        }

        void bind(ObatSediaanItem d) { // Diubah ke ObatSediaanItem
            textName.setText(d.drug_name);
            String meta = "";
            if (d.manufacturer != null && !d.manufacturer.isEmpty()) meta += d.manufacturer;
            /*if (d.category_main != null && !d.category_main.isEmpty()) {
                if (!meta.isEmpty()) meta += " • ";
                meta += d.category_main;
            }*/
            textMeta.setText(meta);
        }
    }
}