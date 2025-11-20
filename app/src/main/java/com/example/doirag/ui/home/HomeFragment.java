package com.example.doirag.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.doirag.R;
import com.example.doirag.databinding.FragmentHomeBinding;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private NavController navController; // ADDED: NavController reference

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // ADDED: Get NavController instance
        navController = NavHostFragment.findNavController(this);

        // ======= Kategori Populer (CHANGED) =======
        String[] categories = new String[]{
                "Analgesik", "Antibiotik", "Antihistamin",
                "Vitamin", "Antipiretik", "Antasida"
        };

        for (String cat : categories) {
            Chip chip = new Chip(requireContext(), null,
                    com.google.android.material.R.style.Widget_Material3_Chip_Assist_Elevated);
            chip.setText(cat);
            chip.setCheckable(false);
            chip.setOnClickListener(v -> {
                // ADDED: Navigate to library and pass category as search query
                Bundle args = new Bundle();
                args.putString("searchQuery", cat);
                args.putBoolean("focusSearch", false);
                navController.navigate(R.id.nav_library, args);
            });
            binding.chipGroupCategories.addView(chip);
        }

        // ======= Riwayat Pencarian Dummy (CHANGED) =======
        String[] history = new String[]{
                "Paracetamol", "Amoksisilin", "CTM"
        };
        for (String item : history) {
            MaterialCardView card = new MaterialCardView(requireContext());
            // ... (card styling is the same) ...
            card.setRadius(12);
            card.setStrokeWidth(1);
            card.setStrokeColor(MaterialColors.getColor(card,
                    com.google.android.material.R.attr.colorOutline));
            card.setClickable(true);
            card.setUseCompatPadding(true);

            TextView tv = new TextView(requireContext());
            tv.setText("🔍 " + item);
            tv.setPadding(16, 12, 16, 12);
            tv.setTextSize(14);
            card.addView(tv);

            // ADDED: Click listener to navigate with history item as query
            card.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("searchQuery", item);
                args.putBoolean("focusSearch", false);
                navController.navigate(R.id.nav_library, args);
            });

            binding.layoutHistory.addView(card);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) card.getLayoutParams();
            lp.setMargins(0, 0, 0, 8);
            card.setLayoutParams(lp);
        }

        // ======= Navigasi (CHANGED) =======

        // ADDED: Listener for the fake search bar
        binding.inputSearch.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("focusSearch", true); // Tell library to open keyboard
            navController.navigate(R.id.nav_library, args);
        });

        binding.cardScan.setOnClickListener(v -> {
            // "Telusuri Obat" now navigates to the library
            // This is cleaner than the search bar click, which requests focus
            navController.navigate(R.id.nav_library);
        });

        binding.cardChat.setOnClickListener(v -> {
            navController.navigate(R.id.nav_rag);
        });

        // ======= Observasi teks header =======
        viewModel.getText().observe(getViewLifecycleOwner(), text -> {
            binding.homeTitle.setText("Halo, " + text);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}