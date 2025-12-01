package com.example.doirag.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doirag.R;
import java.util.ArrayList;
import java.util.List;

public class ObatSediaanFragment extends Fragment {

    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list_simple, container, false);

        recyclerView = v.findViewById(R.id.recycler);

        if (v.findViewById(R.id.fastScroller) != null)
            v.findViewById(R.id.fastScroller).setVisibility(View.GONE);
        if (v.findViewById(R.id.filterContainer) != null)
            v.findViewById(R.id.filterContainer).setVisibility(View.GONE);


        int paddingPx = (int) (12 * getResources().getDisplayMetrics().density);
        recyclerView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        if (recyclerView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams();
            params.setMargins(0, 0, 0, 0); // Reset semua margin layout
            recyclerView.setLayoutParams(params);
        }

        // Grid 2 Kolom
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        // Ambil Data Kategori sesuai Database
        List<CategoryItem> categories = getCategories();

        // Setup Adapter
        CategoryAdapter adapter = new CategoryAdapter(categories, (filterValue, pageTitle) -> {
            Bundle args = new Bundle();
            // Masukkan data untuk dikirim ke ObatSediaanListFragment
            args.putString("category_filter", filterValue); // String persis database
            args.putString("page_title", pageTitle);       // Judul pendek (cth: "PERNAFASAN")

            try {
                // Navigasi ke List Fragment
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main)
                        .navigate(R.id.nav_sediaan_list, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        recyclerView.setAdapter(adapter);

        return v;
    }

    private List<CategoryItem> getCategories() {
        List<CategoryItem> list = new ArrayList<>();
        list.add(new CategoryItem("SEMUA KATEGORI", null, R.drawable.ic_all_category));
        list.add(new CategoryItem("PERNAFASAN", "1. Sistem Saluran Pernafasan", R.drawable.ic_pernapasan));
        list.add(new CategoryItem("KARDIOVASKULER", "2. Sistem Kardiovaskuler", R.drawable.ic_kardiovaskular));
        list.add(new CategoryItem("PENCERNAAN", "3. Sistem Saluran Cerna", R.drawable.ic_pencernaan));
        list.add(new CategoryItem("SARAF & OTOT", "4. Sistem Saraf dan Otot", R.drawable.ic_saraf_otot));
        list.add(new CategoryItem("KEMIH & KELAMIN", "5. Kemih dan Kelamin", R.drawable.ic_kemih));
        list.add(new CategoryItem("METABOLISME", "6. Sistem Metabolisme", R.drawable.ic_metabolisme));
        list.add(new CategoryItem("IMUNOLOGI & VAKSIN", "7. Sistem Imunologi, Vaksin dan Imunosera", R.drawable.ic_imun));
        list.add(new CategoryItem("ANTIBIOTIKA", "8. Anti Biotika dll", R.drawable.ic_antibiotika));
        list.add(new CategoryItem("HORMON", "9. HORMON", R.drawable.ic_hormon));
        list.add(new CategoryItem("MATA", "10. MATA", R.drawable.ic_mata));
        list.add(new CategoryItem("TELINGA", "11. TELINGA", R.drawable.ic_telinga));
        list.add(new CategoryItem("MULUT & TENGGOROKAN", "12. OBAT-OBAT MULUT dan TENGGOROKAN", R.drawable.ic_mulut));
        list.add(new CategoryItem("KULIT", "13. KULIT", R.drawable.ic_kulit));
        list.add(new CategoryItem("VITAMIN & SUPLEMEN", "14. VITAMIN - SUPLEMEN", R.drawable.ic_vitamin));
        list.add(new CategoryItem("NUTRISI", "15. NUTRISI", R.drawable.ic_nutrisi));

        return list;
    }
}