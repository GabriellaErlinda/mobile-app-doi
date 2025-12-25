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
        // Fers, kita inflate layout-nya pake ViewBinding biar rapi
        binding = FragmentLibraryBinding.inflate(inflater, container, false);

        // Connect ke ViewModel-nya biar datanya sinkron se-activity
        viewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);

        // 1. Setup ViewPager buat swipe-swipe antar tab
        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                // Kalo tab 0 isinya Generik, sisanya Sediaan
                if (position == 0) {
                    return new ObatGenerikFragment();
                } else {
                    return new ObatSediaanFragment();
                }
            }
            @Override
            public int getItemCount() {
                return 2; // Cuma ada 2 tab doang
            }
        };
        binding.viewPager.setAdapter(adapter);

        // 2. Setup TabLayout biar nyambung sama ViewPager
        new com.google.android.material.tabs.TabLayoutMediator(
                binding.tabLayout, binding.viewPager,
                (tab, pos) -> {
                    if (pos == 0) tab.setText("Daftar Bahan Aktif");
                    else tab.setText("Daftar Obat");
                }
        ).attach();

        // 3. Setup Search Bar logic
        searchInput = binding.searchInput;
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    // Tiap kali user ngetik, search query di VM langsung ke trigger update
                    viewModel.setSearchQuery(s.toString());
                }
            });
        }

        // Double check buat pastiin logic search-nya beneran ke trigger pas kosong
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString();
                viewModel.setSearchQuery(query);

                // Cek kalo query empty, reset logic harus jalan
                if (query.isEmpty()) {
                }
            }
        });

        // 4. Setup Tab Listener buat kontrol visibilitas UI header
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.setCurrentTab(tab.getPosition());

                if (tab.getPosition() == 0) {
                    // Di Tab Generik: Search & Sort harus ada
                    binding.searchContainer.setVisibility(View.VISIBLE);
                    binding.btnFilter.setVisibility(View.GONE);
                } else {
                    // Di Tab Sediaan: Kita hide dulu biar fokus ke grid kategori etc
                    binding.searchContainer.setVisibility(View.GONE);
                    binding.filterScrollView.setVisibility(View.GONE);
                }

                // Tiap ganti tab, search input kita clear biar gak rancu
                if (searchInput != null) searchInput.setText("");
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Button Sort buat munculin menu A-Z etc
        binding.btnSort.setOnClickListener(v -> showSortMenu());

        // Button Filter logic-nya ada di bottom sheet
        binding.btnFilter.setOnClickListener(v -> showFilterBottomSheet());

        // Observe data biar Chip Filter aktif di atas ke-update otomatis
        viewModel.getFilteredSediaanDrugs().observe(getViewLifecycleOwner(), list -> {
            updateActiveFilterChip();
        });

        // Listener buat gesture tarik ke bawah buat refresh page
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.refreshData();
        });

        // Pantau loading state dari VM buat matiin/nyalain animasi refresh-nya
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.swipeRefreshLayout.setRefreshing(loading);
        });

        // Cek kalo ada arguments masuk (misal dari Home)
        handleArguments();

        return binding.getRoot();
    }

    // --- Sort Menu Logic ---
    private void showSortMenu() {
        // Bikin menu popup pas tombol sort diklik
        PopupMenu popup = new PopupMenu(requireContext(), binding.btnSort);
        popup.getMenu().add(0, 1, 0, "A→Z");
        popup.getMenu().add(0, 2, 0, "Z→A");

        popup.setOnMenuItemClickListener(item -> {
            // Pas diklik, order-nya di VM langsung ke trigger berubah
            viewModel.setSortOrder(item.getItemId() == 1);
            return true;
        });
        popup.show();
    }

    private void showFilterBottomSheet() {
        // Munculin modal dari bawah buat pilih kategori obat
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

        // Chip 'Semua' buat reset filter
        Chip allChip = new Chip(requireContext());
        allChip.setText(R.string.semua_kategori);
        allChip.setCheckable(true);
        if (viewModel.getActiveCategoryFilter() == null) allChip.setChecked(true);
        allChip.setOnClickListener(v -> {
            viewModel.setCategoryFilter(null);
            bottomSheetDialog.dismiss();
        });
        chipGroup.addView(allChip);

        // Ambil list kategori yang ada di VM
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
                    // Pas kategori dipilih, filter di VM langsung ke trigger
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
        // Update UI chip yang ada di bawah tab bar
        binding.activeFiltersChipGroup.removeAllViews();
        String currentFilter = viewModel.getActiveCategoryFilter();

        // filter ada, tampilin chip-nya
        if (currentFilter != null) {
            binding.filterScrollView.setVisibility(View.VISIBLE);

            Chip chip = new Chip(requireContext());
            chip.setText(currentFilter);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> viewModel.setCategoryFilter(null));
            binding.activeFiltersChipGroup.addView(chip);
        } else {
            // Kalo gak ada filter, scrollview-nya di-hide aja biar gak nyampah
            if (binding.tabLayout.getSelectedTabPosition() == 0) {
                binding.filterScrollView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Memory management: clear binding biar gak leak
        binding = null;
    }

    private void handleArguments() {
        // Logic buat nangkep kiriman data dari fragment lain (misal search query fers)
        if (getArguments() == null || searchInput == null) return;

        String query = getArguments().getString("searchQuery");
        if (query != null && !query.isEmpty()) {
            searchInput.setText(query);
        }

        // Cek kalo butuh auto-focus ke search bar pas masuk
        boolean focus = getArguments().getBoolean("focusSearch");
        if (focus) {
            searchInput.post(() -> {
                searchInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
            });
        }
        getArguments().clear(); // Udah dipake, clear biar ga ke trigger lagi pas rotate etc
    }
}