package com.example.doirag.ui.library;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doirag.R;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class ObatSediaanListFragment extends Fragment {

    private RecyclerView recycler;
    private FastScroller fastScroller;
    private ObatSediaanAdapter adapter;
    private LibraryViewModel viewModel;
    private LinearLayoutManager layoutManager;

    // Data dari Arguments
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
        // GUNAKAN LAYOUT BARU
        View v = inflater.inflate(R.layout.fragment_sediaan_list_custom, container, false);

        // 1. Setup Header UI
        setupHeader(v);

        // 2. Setup RecyclerView
        recycler = v.findViewById(R.id.recycler);
        fastScroller = v.findViewById(R.id.fastScroller);
        layoutManager = new LinearLayoutManager(requireContext());
        recycler.setLayoutManager(layoutManager);

        adapter = new ObatSediaanAdapter(item -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("drug_item", item);
            Navigation.findNavController(requireView()).navigate(R.id.nav_drug_detail, bundle);
        });
        recycler.setAdapter(adapter);

        // 3. Connect ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);

        // Reset search query saat masuk halaman ini agar bersih
        viewModel.setSearchQuery("");
        // Set Filter Kategori (Ini yang memperbaiki layar kosong)
        viewModel.setCategoryFilter(categoryFilter);

        viewModel.getFilteredSediaanDrugs().observe(getViewLifecycleOwner(), sediaanItems -> {
            adapter.submitList(sediaanItems);
        });

        setupFastScroller();

        return v;
    }

    private void setupHeader(View v) {
        TextView tvTitle = v.findViewById(R.id.tvPageTitle);
        ImageButton btnBack = v.findViewById(R.id.btnBack);
        EditText inputSearch = v.findViewById(R.id.inputSearch);
        ChipGroup chipGroupSort = v.findViewById(R.id.chipGroupSort);

        // Set Title
        tvTitle.setText(pageTitle != null ? pageTitle : "Daftar Obat");

        // Back Button Logic
        btnBack.setOnClickListener(view -> Navigation.findNavController(view).navigateUp());

        // Search Logic
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setSearchQuery(s.toString());
            }
        });

        // Sort Logic
        chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipSortAZ) {
                viewModel.setSortOrder(true);
            } else if (id == R.id.chipSortZA) {
                viewModel.setSortOrder(false);
            }
        });
    }

    private void setupFastScroller() {
        fastScroller.setListener(section -> {
            List<ObatSediaanItem> currentList = adapter.getCurrentList();
            if (currentList == null || currentList.isEmpty()) return;

            for (int i = 0; i < currentList.size(); i++) {
                String name = currentList.get(i).drug_name;
                if (name != null && !name.isEmpty()) {
                    String firstChar = name.substring(0, 1).toUpperCase();
                    if (section.equals("#")) {
                        if (Character.isDigit(firstChar.charAt(0))) { scrollTo(i); break; }
                    } else {
                        if (firstChar.compareTo(section) >= 0) { scrollTo(i); break; }
                    }
                }
            }
        });
    }

    private void scrollTo(int position) {
        layoutManager.scrollToPositionWithOffset(position, 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Jangan reset categoryFilter disini jika Anda ingin search tetap bekerja di context ini,
        // Tapi reset search query boleh.
        viewModel.setSearchQuery("");
    }
}