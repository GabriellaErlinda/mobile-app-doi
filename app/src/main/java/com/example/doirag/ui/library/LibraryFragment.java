package com.example.doirag.ui.library;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.doirag.R;
import com.example.doirag.databinding.FragmentLibraryBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import android.widget.EditText;

import java.util.List;

public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;
    private EditText searchInput;
    private LibraryViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);

        // 1. Setup ViewPager
        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return new ObatGenerikFragment();
                } else {
                    return new ObatSediaanFragment();
                }
            }
            @Override
            public int getItemCount() {
                return 2;
            }
        };
        binding.viewPager.setAdapter(adapter);

        // 2. Setup TabLayout
        new com.google.android.material.tabs.TabLayoutMediator(
                binding.tabLayout, binding.viewPager,
                (tab, pos) -> {
                    if (pos == 0) tab.setText("Daftar Bahan Aktif");
                    else tab.setText("Daftar Obat");
                }
        ).attach();

        // 3. Setup Search Bar
        searchInput = binding.searchInput;
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    viewModel.setSearchQuery(s.toString());
                }
            });
        }

        // 4. Setup Tab Listener (Kontrol Visibilitas Header)
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.setCurrentTab(tab.getPosition());

                if (tab.getPosition() == 0) {
                    // TAB OBAT GENERIK: Tampilkan Search & Sort
                    binding.searchContainer.setVisibility(View.VISIBLE);
                    // Tombol Filter tetap GONE untuk generik sesuai request sebelumnya
                    binding.btnFilter.setVisibility(View.GONE);
                } else {
                    // TAB OBAT SEDIAAN (GRID): Sembunyikan SEMUA (Search, Filter, Sort)
                    binding.searchContainer.setVisibility(View.GONE);
                    // Sembunyikan juga chip filter aktif jika ada
                    binding.filterScrollView.setVisibility(View.GONE);
                }

                if (searchInput != null) searchInput.setText("");
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // SORT BUTTON CLICK LISTENER
        binding.btnSort.setOnClickListener(v -> showSortMenu());

        // FILTER BUTTON (Hanya logika, tapi tombolnya di-hide di layout untuk sekarang)
        binding.btnFilter.setOnClickListener(v -> showFilterBottomSheet());

        // Observe Active Filter to update Chip UI on the bar
        viewModel.getFilteredSediaanDrugs().observe(getViewLifecycleOwner(), list -> {
            updateActiveFilterChip();
        });

        handleArguments();

        return binding.getRoot();
    }

    // --- Sort Menu Logic ---
    private void showSortMenu() {
        PopupMenu popup = new PopupMenu(requireContext(), binding.btnSort);
        popup.getMenu().add(0, 1, 0, "A→Z");
        popup.getMenu().add(0, 2, 0, "Z→A");

        popup.setOnMenuItemClickListener(item -> {
            viewModel.setSortOrder(item.getItemId() == 1);
            return true;
        });
        popup.show();
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());

        android.widget.ScrollView scrollView = new android.widget.ScrollView(requireContext());
        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(40, 40, 40, 40);

        android.widget.TextView title = new android.widget.TextView(requireContext());
        title.setText(R.string.pilih_kategori);
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0,0,0,24);
        container.addView(title);

        ChipGroup chipGroup = new ChipGroup(requireContext());
        chipGroup.setSingleSelection(true);

        Chip allChip = new Chip(requireContext());
        allChip.setText(R.string.semua_kategori);
        allChip.setCheckable(true);
        if (viewModel.getActiveCategoryFilter() == null) allChip.setChecked(true);
        allChip.setOnClickListener(v -> {
            viewModel.setCategoryFilter(null);
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

    private void updateActiveFilterChip() {
        binding.activeFiltersChipGroup.removeAllViews();
        String currentFilter = viewModel.getActiveCategoryFilter();

        if (currentFilter != null) {
            // Hanya tampilkan chip filter jika kita TIDAK di Tab Sediaan (Grid)
            // Tapi karena Tab Sediaan menyembunyikan parent viewnya (filterScrollView),
            // ini aman.
            binding.filterScrollView.setVisibility(View.VISIBLE);

            Chip chip = new Chip(requireContext());
            chip.setText(currentFilter);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> viewModel.setCategoryFilter(null));
            binding.activeFiltersChipGroup.addView(chip);
        } else {
            // Jika tidak ada filter aktif dan kita di tab generik (pos 0), hide scrollview
            if (binding.tabLayout.getSelectedTabPosition() == 0) {
                binding.filterScrollView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void handleArguments() {
        if (getArguments() == null || searchInput == null) return;

        String query = getArguments().getString("searchQuery");
        if (query != null && !query.isEmpty()) {
            searchInput.setText(query);
        }

        boolean focus = getArguments().getBoolean("focusSearch");
        if (focus) {
            searchInput.post(() -> {
                searchInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
            });
        }
        getArguments().clear();
    }
}