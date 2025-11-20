package com.example.doirag.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.doirag.R;
import com.example.doirag.databinding.FragmentDetailObatSediaanBinding;
import com.google.android.material.chip.Chip;

public class DetailObatSediaanFragment extends Fragment {

    private FragmentDetailObatSediaanBinding binding;
    private ObatSediaanItem drugItem;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the object passed from the list
        if (getArguments() != null) {
            drugItem = (ObatSediaanItem) getArguments().getSerializable("drug_item");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDetailObatSediaanBinding.inflate(inflater, container, false);

        // Setup Back Button on Toolbar
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        if (drugItem != null) {
            populateData();
        }

        return binding.getRoot();
    }

    private void populateData() {
        // Header
        binding.tvDrugName.setText(drugItem.drug_name);
        binding.tvManufacturer.setText(drugItem.manufacturer != null ? drugItem.manufacturer : "-");

        // Add Category Chips dynamically
        if (drugItem.category_main != null && !drugItem.category_main.isEmpty())
            addChip(drugItem.category_main);
        if (drugItem.category_sub != null && !drugItem.category_sub.isEmpty())
            addChip(drugItem.category_sub);

        // Medical Info
        binding.tvIndikasi.setText(formatText(drugItem.indikasi));
        binding.tvKomposisi.setText(formatText(drugItem.komposisi));
        binding.tvDosis.setText(formatText(drugItem.dosis));

        // Safety Info
        binding.tvKontraindikasi.setText(formatText(drugItem.kontraindikasi));
        binding.tvEfekSamping.setText(formatText(drugItem.efek_samping));
        binding.tvPerhatian.setText(formatText(drugItem.perhatian));

        // Other Info
        binding.tvInteraksi.setText(formatText(drugItem.interaksi_obat));
        binding.tvFarmakologi.setText(formatText(drugItem.farmakologi));
        binding.tvKemasan.setText(formatText(drugItem.kemasan));
    }

    private String formatText(String text) {
        return (text == null || text.trim().isEmpty()) ? "-" : text;
    }

    private void addChip(String text) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setClickable(false);
        chip.setEnsureMinTouchTargetSize(false);
        // Style the chip slightly
        chip.setChipBackgroundColorResource(com.google.android.material.R.color.m3_sys_color_dynamic_light_surface_container_high);
        binding.chipGroupCategories.addView(chip);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}