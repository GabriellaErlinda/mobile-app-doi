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

import com.example.doirag.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

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

        android.widget.ScrollView scrollView = new android.widget.ScrollView(requireContext());
        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(40, 40, 40, 40);

        TextView title = new TextView(requireContext());
        title.setText("Pilih Kategori");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0,0,0,24);
        container.addView(title);

        ChipGroup chipGroup = new ChipGroup(requireContext());
        chipGroup.setSingleSelection(true);

        Chip allChip = new Chip(requireContext());
        allChip.setText("Semua Kategori");
        allChip.setCheckable(true);
        // Cek status saat ini
        if (viewModel.getActiveCategoryFilter() == null) allChip.setChecked(true);

        allChip.setOnClickListener(v -> {
            viewModel.setCategoryFilter(null);
            updateActiveChipUI(null);
            bottomSheetDialog.dismiss();
        });
        chipGroup.addView(allChip);

        List<String> categories = viewModel.getCategoryList().getValue();
        if (categories != null) {
            for (String cat : categories) {
                Chip chip = new Chip(requireContext());
                chip.setText(cat);
                chip.setCheckable(true);
                if (cat.equals(viewModel.getActiveCategoryFilter())) {
                    chip.setChecked(true);
                }
                chip.setOnClickListener(v -> {
                    viewModel.setCategoryFilter(cat);
                    updateActiveChipUI(cat);
                    bottomSheetDialog.dismiss();
                });
                chipGroup.addView(chip);
            }
        }

        container.addView(chipGroup);
        scrollView.addView(container);
        bottomSheetDialog.setContentView(scrollView);
        bottomSheetDialog.show();
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