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
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;
    private TextInputEditText searchInput;
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
                    if (pos == 0) tab.setText("Daftar Obat Generik");
                    else tab.setText("Daftar Sediaan Obat");
                }
        ).attach();

        // 3. Setup Search Bar
        searchInput = (TextInputEditText) binding.searchLayout.getEditText();
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

        // 4. Setup Tab Listener (Show/Hide Filter button based on Tab)
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.setCurrentTab(tab.getPosition());
                if (searchInput != null) {
                    if (tab.getPosition() == 0) {
                        binding.searchLayout.setHint("Cari nama obat generik…");
                        binding.btnFilter.setVisibility(View.GONE);
                    } else {
                        binding.searchLayout.setHint("Cari sediaan/route…");
                        binding.btnFilter.setVisibility(View.VISIBLE);
                    }
                    searchInput.setText("");
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 5. Setup Filter & Sort UI
        binding.filterScrollView.setVisibility(View.VISIBLE);

        // Default state check button filter
        if (binding.tabLayout.getSelectedTabPosition() == 0) {
            binding.btnFilter.setVisibility(View.GONE);
        }

        // SORT BUTTON CLICK LISTENER
        binding.btnSort.setOnClickListener(v -> showSortMenu());

        // FILTER BUTTON
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

        // Add options: ID 1 for Ascending, ID 2 for Descending
        popup.getMenu().add(0, 1, 0, "A→Z");
        popup.getMenu().add(0, 2, 0, "Z→A");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                // User chose A-Z
                viewModel.setSortOrder(true);
                binding.btnSort.setText("A-Z");
            } else {
                // User chose Z-A
                viewModel.setSortOrder(false);
                binding.btnSort.setText("Z-A");
            }
            return true;
        });
        popup.show();
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());

        // Build view programmatically
        android.widget.ScrollView scrollView = new android.widget.ScrollView(requireContext());
        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(40, 40, 40, 40);

        android.widget.TextView title = new android.widget.TextView(requireContext());
        title.setText("Pilih Kategori");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0,0,0,24);
        container.addView(title);

        ChipGroup chipGroup = new ChipGroup(requireContext());
        chipGroup.setSingleSelection(true);

        // Add "All" option
        Chip allChip = new Chip(requireContext());
        allChip.setText("Semua Kategori");
        allChip.setCheckable(true);
        // If active filter is null, check this chip
        if (viewModel.getActiveCategoryFilter() == null) allChip.setChecked(true);
        allChip.setOnClickListener(v -> {
            viewModel.setCategoryFilter(null);
            bottomSheetDialog.dismiss();
        });
        chipGroup.addView(allChip);

        // Add Categories from ViewModel
        List<String> categories = viewModel.getCategoryList().getValue();
        if (categories != null) {
            for (String cat : categories) {
                Chip chip = new Chip(requireContext());
                chip.setText(cat);
                chip.setCheckable(true);

                // Check if this category is currently active
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
            Chip chip = new Chip(requireContext());
            chip.setText(currentFilter);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> viewModel.setCategoryFilter(null));
            binding.activeFiltersChipGroup.addView(chip);

            // Highlight filter button
            binding.btnFilter.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.primary, null)));
        } else {
            // Reset filter button
            binding.btnFilter.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFCBD5E1));
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