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
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.shape.CornerFamily;

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
        if (text == null || text.isEmpty()) return;

        // clean regex
        String cleanText = text.replaceAll("^\\d+[a-zA-Z]*\\.\\s*", "# ");

        Chip chip = new Chip(requireContext());
        chip.setText(cleanText);
        chip.setClickable(false);
        chip.setCheckable(false);

        chip.setEllipsize(android.text.TextUtils.TruncateAt.END);

        chip.setChipBackgroundColorResource(R.color.muted_white);

        chip.setTextColor(getResources().getColor(R.color.dark_grey, null));
        chip.setChipStrokeColorResource(R.color.light_grey);
        chip.setChipStrokeWidth(3f);

        ShapeAppearanceModel shape = chip.getShapeAppearanceModel().toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, 100f) // radius chip
                .build();
        chip.setShapeAppearanceModel(shape);

        binding.chipGroupCategories.addView(chip);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}