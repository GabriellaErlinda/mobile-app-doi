package com.example.doirag.ui.library;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.doirag.databinding.FragmentLibraryBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

public class LibraryFragment extends Fragment { // Hapus "implements Searchable" (jika ada)

    private FragmentLibraryBinding binding;
    private TextInputEditText searchInput;

    // 1. Tambahkan referensi ke ViewModel
    private LibraryViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);

        // 2. Dapatkan ViewModel yang di-scope ke Activity
        // Ini memastikan semua fragment berbagi ViewModel yang sama
        viewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);

        // 3. Setup ViewPager (Kode Anda di sini sudah benar)
        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    // Panggil nama fragment baru
                    return new ObatGenerikFragment();
                } else {
                    // Panggil nama fragment baru
                    return new ObatSediaanFragment();
                }
            }
            @Override
            public int getItemCount() {
                return 2; // dua tab
            }
        };
        binding.viewPager.setAdapter(adapter);

        // 4. Setup TabLayoutMediator (Kode Anda di sini sudah benar)
        new com.google.android.material.tabs.TabLayoutMediator(
                binding.tabLayout, binding.viewPager,
                (tab, pos) -> {
                    if (pos == 0) {
                        tab.setText("Daftar Obat Generik");
                    } else {
                        tab.setText("Daftar Sediaan Obat");
                    }
                }
        ).attach();

        // 5. Setup Search Bar (INI PERBAIKANNYA)
        searchInput = (TextInputEditText) binding.searchLayout.getEditText();

        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    // LANGSUNG KIRIM KE VIEWMODEL
                    // ViewModel sudah tahu tab mana yang aktif
                    viewModel.setSearchQuery(s.toString());
                }
            });
        }

        // 6. Setup Tab Listener (INI PERBAIKANNYA)
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                // BERI TAHU VIEWMODEL TAB BERUBAH
                viewModel.setCurrentTab(tab.getPosition());

                // Logika UI (sudah benar)
                if (searchInput != null) {
                    if (tab.getPosition() == 0) {
                        binding.searchLayout.setHint("Cari nama obat generik…");
                    } else
                        binding.searchLayout.setHint("Cari sediaan/route…");
                    searchInput.setText(""); // Clear search on tab change
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 7. Handle arguments (Kode Anda di sini sudah benar)
        handleArguments();

        return binding.getRoot();
    }

    // Tambahkan ini untuk mencegah memory leak
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Kode handleArguments Anda sudah benar, tidak perlu diubah
    private void handleArguments() {
        if (getArguments() == null || searchInput == null) return;

        // Check for a search query
        String query = getArguments().getString("searchQuery");
        if (query != null && !query.isEmpty()) {
            searchInput.setText(query);
            // The TextWatcher will automatically trigger the search
        }

        // Check if we need to request focus
        boolean focus = getArguments().getBoolean("focusSearch");
        if (focus) {
            // Use post to ensure the view is ready
            searchInput.post(() -> {
                searchInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
            });
        }

        // Clear arguments so they don't re-trigger on config change
        getArguments().clear();
    }
}
