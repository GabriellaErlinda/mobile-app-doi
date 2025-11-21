package com.example.doirag.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.doirag.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject; // Import JsonObject

public class DetailObatGenerikFragment extends Fragment {

    private ObatGenerikItem drugItem;

    // Views (Text)
    private TextView tvDrugName, tvBrands;
    private TextView tvDosis, tvDosisAwal, tvDosisPemeliharaan;
    private TextView tvKontraindikasi, tvEfekSamping, tvPerhatian;
    private TextView tvInteraksi, tvIndikasi, tvFarmakologi, tvExtras;

    // Views (Section Layouts)
    private LinearLayout sectionDosis, sectionDosisAwal, sectionDosisPemeliharaan;
    private LinearLayout sectionKontraindikasi, sectionEfekSamping, sectionPerhatian;
    private LinearLayout sectionInteraksi, sectionIndikasi, sectionFarmakologi, sectionExtras;

    // Cards
    private MaterialCardView cardBrands, cardDosis, cardSafety, cardOthers;

    private MaterialToolbar toolbar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            drugItem = (ObatGenerikItem) getArguments().getSerializable("drug_item");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail_obat_generik, container, false);
        initializeViews(view);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        if (drugItem != null) {
            populateData();
        }
        return view;
    }

    private void initializeViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        tvDrugName = view.findViewById(R.id.tvDrugName);
        tvBrands = view.findViewById(R.id.tvBrands);

        // Text Views
        tvDosis = view.findViewById(R.id.tvDosis);
        tvDosisAwal = view.findViewById(R.id.tvDosisAwal);
        tvDosisPemeliharaan = view.findViewById(R.id.tvDosisPemeliharaan);
        tvKontraindikasi = view.findViewById(R.id.tvKontraindikasi);
        tvEfekSamping = view.findViewById(R.id.tvEfekSamping);
        tvPerhatian = view.findViewById(R.id.tvPerhatian);
        tvInteraksi = view.findViewById(R.id.tvInteraksi);
        tvIndikasi = view.findViewById(R.id.tvIndikasi);
        tvFarmakologi = view.findViewById(R.id.tvFarmakologi);
        tvExtras = view.findViewById(R.id.tvExtras);

        // Section Layouts
        sectionDosis = view.findViewById(R.id.sectionDosis);
        sectionDosisAwal = view.findViewById(R.id.sectionDosisAwal);
        sectionDosisPemeliharaan = view.findViewById(R.id.sectionDosisPemeliharaan);
        sectionKontraindikasi = view.findViewById(R.id.sectionKontraindikasi);
        sectionEfekSamping = view.findViewById(R.id.sectionEfekSamping);
        sectionPerhatian = view.findViewById(R.id.sectionPerhatian);
        sectionInteraksi = view.findViewById(R.id.sectionInteraksi);
        sectionIndikasi = view.findViewById(R.id.sectionIndikasi);
        sectionFarmakologi = view.findViewById(R.id.sectionFarmakologi);
        sectionExtras = view.findViewById(R.id.sectionExtras);

        // Cards
        cardBrands = view.findViewById(R.id.cardBrands);
        cardDosis = view.findViewById(R.id.cardDosis);
        cardSafety = view.findViewById(R.id.cardSafety);
        cardOthers = view.findViewById(R.id.cardOthers);
    }

    private void populateData() {
        tvDrugName.setText(drugItem.nama_generik);

        // --- Brands ---
        if (hasContent(drugItem.nama_obat_dan_produsen)) {
            cardBrands.setVisibility(View.VISIBLE);
            tvBrands.setText(drugItem.nama_obat_dan_produsen);
        } else {
            cardBrands.setVisibility(View.GONE);
        }

        // --- Dosis Sections ---
        boolean hasDosis = bindSection(sectionDosis, tvDosis, drugItem.dosis);
        boolean hasDosisAwal = bindSection(sectionDosisAwal, tvDosisAwal, drugItem.dosis_awal);
        boolean hasDosisPeme = bindSection(sectionDosisPemeliharaan, tvDosisPemeliharaan, drugItem.dosis_pemeliharaan);

        if (!hasDosis && !hasDosisAwal && !hasDosisPeme) {
            cardDosis.setVisibility(View.GONE);
        }

        // --- Safety Sections ---
        boolean hasKontra = bindSection(sectionKontraindikasi, tvKontraindikasi, drugItem.kontraindikasi);
        boolean hasEfek = bindSection(sectionEfekSamping, tvEfekSamping, drugItem.efek_samping);
        boolean hasPerhatian = bindSection(sectionPerhatian, tvPerhatian, drugItem.perhatian);

        if (!hasKontra && !hasEfek && !hasPerhatian) {
            cardSafety.setVisibility(View.GONE);
        }

        // --- Other Sections ---
        boolean hasInteraksi = bindSection(sectionInteraksi, tvInteraksi, drugItem.interaksi_obat);
        boolean hasIndikasi = bindSection(sectionIndikasi, tvIndikasi, drugItem.indikasi);
        boolean hasFarmakologi = bindSection(sectionFarmakologi, tvFarmakologi, drugItem.farmakologi);

        // --- Handle Extras (SAFE VERSION) ---
        boolean hasExtras = false;

        // 1. Cek apakah extras tidak null DAN merupakan JsonObject
        if (drugItem.extras != null && drugItem.extras.isJsonObject()) {
            JsonObject extrasObj = drugItem.extras.getAsJsonObject();

            // 2. Cek apakah object tersebut memiliki keys
            if (extrasObj.size() > 0) {
                StringBuilder sb = new StringBuilder();

                for (String key : extrasObj.keySet()) {
                    sb.append("• ").append(key).append(":\n");

                    JsonElement valueElement = extrasObj.get(key);
                    String valueStr = "";

                    if (valueElement.isJsonPrimitive()) {
                        valueStr = valueElement.getAsString();
                    } else {
                        valueStr = valueElement.toString();
                    }

                    sb.append(valueStr).append("\n\n");
                }
                String extrasText = sb.toString().trim();
                hasExtras = bindSection(sectionExtras, tvExtras, extrasText);
            }
        }

        if (!hasExtras) {
            sectionExtras.setVisibility(View.GONE);
        }

        if (!hasInteraksi && !hasIndikasi && !hasFarmakologi && !hasExtras) {
            cardOthers.setVisibility(View.GONE);
        }
    }

    private boolean bindSection(View sectionLayout, TextView textView, String content) {
        if (hasContent(content)) {
            sectionLayout.setVisibility(View.VISIBLE);
            textView.setText(content);
            return true;
        } else {
            sectionLayout.setVisibility(View.GONE);
            return false;
        }
    }

    private boolean hasContent(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        return !trimmed.isEmpty() && !trimmed.equals("-");
    }
}