package com.example.doirag.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doirag.R;
import com.example.doirag.databinding.FragmentHomeBinding;
import com.example.doirag.ui.library.CategoryItem;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        navController = NavHostFragment.findNavController(this);

        setupHomeCategories();
        // setupHistory();
        setupNavigation();
        setupObservers();

        return root;
    }

    private void setupHomeCategories() {
        // 1. List category di home
        List<CategoryItem> homeCats = new ArrayList<>();
        homeCats.add(new CategoryItem("Pernafasan", "1. Sistem Saluran Pernafasan", R.drawable.ic_pernapasan));
        homeCats.add(new CategoryItem("Kardio", "2. Sistem Kardiovaskuler", R.drawable.ic_kardiovaskular));
        homeCats.add(new CategoryItem("Pencernaan", "3. Sistem Saluran Cerna", R.drawable.ic_pencernaan));
        homeCats.add(new CategoryItem("Saraf & Otot", "4. Sistem Saraf dan Otot", R.drawable.ic_saraf_otot));
        homeCats.add(new CategoryItem("Kemih", "5. Kemih dan Kelamin", R.drawable.ic_kemih));
        homeCats.add(new CategoryItem("Metabolisme", "6. Sistem Metabolisme", R.drawable.ic_metabolisme));
        homeCats.add(new CategoryItem("Imun", "7. Sistem Imunologi, Vaksin dan Imunosera", R.drawable.ic_imun));
        homeCats.add(new CategoryItem("Antibiotik", "8. Anti Biotika dll", R.drawable.ic_antibiotika));
        homeCats.add(new CategoryItem("Hormon", "9. HORMON", R.drawable.ic_hormon));
        homeCats.add(new CategoryItem("Mata", "10. MATA", R.drawable.ic_mata));

        // 2. Setup RecyclerView Grid 5 Kolom
        binding.recyclerHomeCategories.setLayoutManager(new GridLayoutManager(requireContext(), 5));

        // 3. Simple Adapter (Inner Class)
        binding.recyclerHomeCategories.setAdapter(new RecyclerView.Adapter<HomeCatViewHolder>() {
            @NonNull
            @Override
            public HomeCatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_category, parent, false);
                return new HomeCatViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull HomeCatViewHolder holder, int position) {
                CategoryItem item = homeCats.get(position);
                holder.tvName.setText(item.displayName);
                holder.imgIcon.setImageResource(item.iconResId);

                holder.itemView.setOnClickListener(v -> {
                    // Navigasi ke Halaman List Obat Spesifik
                    Bundle args = new Bundle();
                    args.putString("category_filter", item.dbValue);
                    args.putString("page_title", item.displayName.toUpperCase());
                    navController.navigate(R.id.nav_sediaan_list, args);
                });
            }

            @Override
            public int getItemCount() { return homeCats.size(); }
        });
    }

    // ViewHolder Class
    static class HomeCatViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView imgIcon;
        HomeCatViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvHomeCatName);
            imgIcon = itemView.findViewById(R.id.imgHomeCat);
        }
    }

    private void setupNavigation() {
        binding.inputSearch.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("focusSearch", true);
            navController.navigate(R.id.nav_library, args);
        });
        binding.cardScan.setOnClickListener(v -> navController.navigate(R.id.nav_library));
        binding.cardChat.setOnClickListener(v -> navController.navigate(R.id.nav_rag));

        // "Telusuri Lebih Banyak"
        binding.tvSeeMore.setOnClickListener(v -> {
            // Navigasi ke Library Fragment -> Tab Sediaan (Index 1)
            Bundle args = new Bundle();
            args.putInt("target_tab", 2);
            navController.navigate(R.id.nav_library, args);
        });
    }

    /* // BAGIAN INI DI-COMMENT DENGAN BLOK AGAR LEBIH AMAN DAN TIDAK MENYISAKAN KURUNG KURAWAL
    private void setupHistory() {
        String[] history = new String[]{"Paracetamol", "Amoksisilin", "CTM"};
        for (String item : history) {
            MaterialCardView card = new MaterialCardView(requireContext());
            card.setRadius(12);
            card.setStrokeWidth(1);
            card.setStrokeColor(MaterialColors.getColor(card, com.google.android.material.R.attr.colorOutline));
            card.setClickable(true);
            card.setUseCompatPadding(true);
            TextView tv = new TextView(requireContext());
            tv.setText("🔍 " + item);
            tv.setPadding(16, 12, 16, 12);
            tv.setTextSize(14);
            card.addView(tv);
            card.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("searchQuery", item);
                navController.navigate(R.id.nav_library, args);
            });
            binding.layoutHistory.addView(card);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) card.getLayoutParams();
            lp.setMargins(0, 0, 0, 8);
            card.setLayoutParams(lp);
        }
    }
    */

    private void setupObservers() {
        viewModel.getText().observe(getViewLifecycleOwner(), text -> {
            binding.homeTitle.setText("Halo, " + text);
        });
    }

}