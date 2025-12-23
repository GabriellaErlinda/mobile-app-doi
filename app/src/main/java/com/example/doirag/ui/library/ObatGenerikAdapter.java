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

public class ObatGenerikAdapter extends ListAdapter<ObatGenerikItem, ObatGenerikAdapter.VH> {

    public interface OnDrugClick {
        void onClick(ObatGenerikItem item);
    }
    private final OnDrugClick listener;

    public ObatGenerikAdapter(OnDrugClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ObatGenerikItem> DIFF =
            new DiffUtil.ItemCallback<ObatGenerikItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull ObatGenerikItem oldItem, @NonNull ObatGenerikItem newItem) {
                    return Objects.equals(oldItem.id, newItem.id);
                }
                @Override
                public boolean areContentsTheSame(@NonNull ObatGenerikItem oldItem, @NonNull ObatGenerikItem newItem) {
                    // Cek beberapa field penting untuk update UI
                    return Objects.equals(oldItem.nama_generik, newItem.nama_generik);
                }
            };

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drug, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ObatGenerikItem item = getItem(pos);
        h.bind(item);
        h.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView textName, textMeta;
        VH(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textMeta = itemView.findViewById(R.id.textMeta);
        }

        void bind(ObatGenerikItem d) {
            textName.setText(d.nama_generik);

            // Tampilkan daftar merek sebagai subtitle
            if (d.nama_obat_dan_produsen != null && !d.nama_obat_dan_produsen.isEmpty()) {
                textMeta.setText(d.nama_obat_dan_produsen);
            } else {
                textMeta.setText("Bahan Aktif");
            }
        }
    }
}