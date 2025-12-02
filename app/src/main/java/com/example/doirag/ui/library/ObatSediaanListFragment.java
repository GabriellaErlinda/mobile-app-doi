package com.example.doirag.ui.library;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.doirag.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ObatSediaanListFragment extends Fragment {

    private RecyclerView recycler;
    private ObatSediaanAdapter adapter;
    private LibraryViewModel viewModel;
    private LinearLayoutManager layoutManager;

    private String categoryFilter;
    private String pageTitle;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryFilter = getArguments().getString("category_filter");
            pageTitle = getArguments().getString("page_title");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sediaan_list_custom, container, false);

        // 1. Setup RecyclerView
        recycler = v.findViewById(R.id.recycler);
        layoutManager = new LinearLayoutManager(requireContext());
        recycler.setLayoutManager(layoutManager);

        adapter = new ObatSediaanAdapter(item -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("drug_item", item);
            Navigation.findNavController(requireView()).navigate(R.id.nav_drug_detail, bundle);
        });
        recycler.setAdapter(adapter);

        // 2. Connect ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);

        // Reset query & Set filter awal dari navigasi
        viewModel.setSearchQuery("");
        viewModel.setCategoryFilter(categoryFilter);

        viewModel.getFilteredSediaanDrugs().observe(getViewLifecycleOwner(), sediaanItems -> {
            adapter.submitList(sediaanItems);
        });

        // Observe jika filter berubah (misal dari tombol Filter di "Semua Kategori")
        // untuk mengupdate UI chip atau title jika perlu
        viewModel.getCategoryList().observe(getViewLifecycleOwner(), categories -> {
            // Triggered when categories loaded, useful for filter dialog
        });

        // 3. Setup Header & Buttons
        setupHeader(v);

        return v;
    }

    private void setupHeader(View v) {
        TextView tvTitle = v.findViewById(R.id.tvPageTitle);
        ImageButton btnBack = v.findViewById(R.id.btnBack);
        EditText inputSearch = v.findViewById(R.id.inputSearch);
        MaterialButton btnFilter = v.findViewById(R.id.btnFilterList);
        MaterialButton btnSort = v.findViewById(R.id.btnSortList);
        ChipGroup chipGroupActive = v.findViewById(R.id.chipGroupActiveFilter);

        // Set Title
        tvTitle.setText(pageTitle != null ? pageTitle : "Daftar Obat");

        // Back Button
        btnBack.setOnClickListener(view -> Navigation.findNavController(view).navigateUp());

        // Search
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setSearchQuery(s.toString());
            }
        });

        // Sort Button Logic (Popup Menu)
        btnSort.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(requireContext(), btnSort);
            popup.getMenu().add(0, 1, 0, "A → Z");
            popup.getMenu().add(0, 2, 0, "Z → A");
            popup.setOnMenuItemClickListener(item -> {
                viewModel.setSortOrder(item.getItemId() == 1);
                return true;
            });
            popup.show();
        });

        // Filter Button Logic
        btnFilter.setOnClickListener(view -> showFilterBottomSheet());

        // === LOGIC TAMPILAN KHUSUS "SEMUA KATEGORI" ===
        // Jika categoryFilter == null, berarti kita di "Semua Kategori"
        if (categoryFilter == null) {
            btnFilter.setVisibility(View.VISIBLE); // Tampilkan tombol filter

            // Tampilkan chip jika user memilih filter lewat tombol
            chipGroupActive.setVisibility(View.VISIBLE);
            // Kita perlu observe active filter dari VM untuk update chip
            // (Hack: menggunakan observer dummy di sini atau update manual saat dialog close)
            // Agar reaktif, mari tambahkan logic update chip di sini saat filter berubah
            // Namun karena method ini dipanggil sekali, kita butuh observer.
            // Observer filter ada di bawah.
        } else {
            // Jika kategori spesifik, sembunyikan tombol filter (user sudah di dalam kategori)
            btnFilter.setVisibility(View.GONE);
            chipGroupActive.setVisibility(View.GONE);
        }

        // Setup Observer untuk Chip Filter di halaman "Semua Kategori"
        if (categoryFilter == null) {
            // Kita pakai addOnLayoutChangeListener atau sejenisnya, tapi
            // lebih baik pakai observer global di onCreateView jika memungkinkan.
            // Untuk simplisitas, kita update chip manual saat dialog filter ditutup.
        }
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());

        // 1. Setup Layout Utama BottomSheet
        android.widget.LinearLayout mainContainer = new android.widget.LinearLayout(requireContext());
        mainContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        mainContainer.setPadding(0, 40, 0, 40); // Padding container

        // 2. Judul
        TextView title = new TextView(requireContext());
        title.setText("Pilih Kategori");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(40, 0, 40, 24); // Padding kiri kanan judul
        title.setTextColor(getResources().getColor(R.color.text_primary, null));
        mainContainer.addView(title);

        // 3. Setup RecyclerView untuk Grid 2 Kolom
        RecyclerView gridRecycler = new RecyclerView(requireContext());
        gridRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        gridRecycler.setPadding(24, 0, 24, 0); // Padding grid agar tidak mepet layar
        gridRecycler.setClipToPadding(false);

        // 4. Siapkan Data (Mapping Nama Pendek -> Value Database)
        List<FilterOption> options = getFilterOptions();

        // 5. Setup Adapter
        FilterAdapter adapter = new FilterAdapter(options, viewModel.getActiveCategoryFilter(), item -> {
            viewModel.setCategoryFilter(item.dbValue);
            updateActiveChipUI(item.label); // Tampilkan nama pendek di chip UI
            bottomSheetDialog.dismiss();
        });

        gridRecycler.setAdapter(adapter);
        mainContainer.addView(gridRecycler);

        // 6. Tampilkan
        bottomSheetDialog.setContentView(mainContainer);
        bottomSheetDialog.show();
    }

    // --- HELPER METHODS UNTUK DATA FILTER ---

    private List<FilterOption> getFilterOptions() {
        List<FilterOption> list = new ArrayList<>();
        // Format: (Label Tampil, Value Database)
        list.add(new FilterOption("Semua Kategori", null));
        list.add(new FilterOption("Pernafasan", "1. Sistem Saluran Pernafasan"));
        list.add(new FilterOption("Kardiovaskuler", "2. Sistem Kardiovaskuler"));
        list.add(new FilterOption("Pencernaan", "3. Sistem Saluran Cerna"));
        list.add(new FilterOption("Saraf & Otot", "4. Sistem Saraf dan Otot"));
        list.add(new FilterOption("Kemih & Kelamin", "5. Kemih dan Kelamin"));
        list.add(new FilterOption("Metabolisme", "6. Sistem Metabolisme"));
        list.add(new FilterOption("Imun & Vaksin", "7. Sistem Imunologi, Vaksin dan Imunosera"));
        list.add(new FilterOption("Antibiotika", "8. Anti Biotika dll"));
        list.add(new FilterOption("Hormon", "9. HORMON"));
        list.add(new FilterOption("Mata", "10. MATA"));
        list.add(new FilterOption("Telinga", "11. TELINGA"));
        list.add(new FilterOption("Mulut & Tenggorokan", "12. OBAT-OBAT MULUT dan TENGGOROKAN"));
        list.add(new FilterOption("Kulit", "13. KULIT"));
        list.add(new FilterOption("Vitamin & Suplemen", "14. VITAMIN - SUPLEMEN"));
        list.add(new FilterOption("Nutrisi", "15. NUTRISI"));
        return list;
    }

    // --- INNER CLASSES (ADAPTER & MODEL) ---

    private static class FilterOption {
        String label;
        String dbValue;
        FilterOption(String l, String v) { label = l; dbValue = v; }
    }

    interface OnItemClick {
        void onClick(FilterOption item);
    }

    private class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.VH> {
        private final List<FilterOption> items;
        private final String currentActiveValue;
        private final OnItemClick listener;

        FilterAdapter(List<FilterOption> items, String currentActiveValue, OnItemClick listener) {
            this.items = items;
            this.currentActiveValue = currentActiveValue;
            this.listener = listener;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_filter_grid, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            FilterOption item = items.get(position);
            holder.tvName.setText(item.label);

            boolean isActive;
            if (item.dbValue == null) {
                isActive = (currentActiveValue == null);
            } else {
                isActive = item.dbValue.equals(currentActiveValue);
            }

            // Ganti warna jika aktif
            if (isActive) {
                holder.card.setCardBackgroundColor(getResources().getColor(R.color.primary, null));
                holder.tvName.setTextColor(getResources().getColor(R.color.white, null));
                holder.card.setStrokeColor(getResources().getColor(R.color.primary, null));
            } else {
                holder.card.setCardBackgroundColor(getResources().getColor(R.color.surface, null));
                holder.tvName.setTextColor(getResources().getColor(R.color.text_primary, null));
                holder.card.setStrokeColor(getResources().getColor(R.color.light_grey, null));
            }

            holder.itemView.setOnClickListener(v -> listener.onClick(item));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName;
            MaterialCardView card;
            VH(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvFilterName);
                card = itemView.findViewById(R.id.cardFilterItem);
            }
        }
    }

    private void updateActiveChipUI(String filterName) {
        View v = getView();
        if (v == null) return;

        ChipGroup chipGroup = v.findViewById(R.id.chipGroupActiveFilter);
        chipGroup.removeAllViews();

        if (filterName != null) {
            Chip chip = new Chip(requireContext());
            chip.setText(filterName);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(view -> {
                viewModel.setCategoryFilter(null);
                chipGroup.removeView(chip);
            });
            chipGroup.addView(chip);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Reset search saat keluar halaman agar tidak bocor ke halaman lain
        viewModel.setSearchQuery("");
    }
}