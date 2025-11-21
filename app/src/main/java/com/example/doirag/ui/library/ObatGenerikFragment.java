package com.example.doirag.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doirag.R;

import java.util.List;

public class ObatGenerikFragment extends Fragment {

    private RecyclerView recycler;
    private FastScroller fastScroller;
    private ObatGenerikAdapter adapter;
    private LibraryViewModel viewModel;
    private LinearLayoutManager layoutManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list_simple, container, false);

        recycler = v.findViewById(R.id.recycler);
        fastScroller = v.findViewById(R.id.fastScroller);

        layoutManager = new LinearLayoutManager(requireContext());
        recycler.setLayoutManager(layoutManager);

        // UPDATE LISTENER DISINI
        adapter = new ObatGenerikAdapter(item -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("drug_item", item);

            // Navigasi ke Detail Generik
            Navigation.findNavController(requireView())
                    .navigate(R.id.nav_drug_detail_generik, bundle);
        });

        recycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);

        viewModel.getFilteredGenerikDrugs().observe(getViewLifecycleOwner(), generikItems -> {
            adapter.submitList(generikItems);
        });

        fastScroller.setListener(section -> {
            List<ObatGenerikItem> currentList = adapter.getCurrentList();
            if (currentList == null || currentList.isEmpty()) return;

            for (int i = 0; i < currentList.size(); i++) {
                String name = currentList.get(i).nama_generik;
                if (name != null && !name.isEmpty()) {
                    String firstChar = name.substring(0, 1).toUpperCase();

                    if (section.equals("#")) {
                        if (Character.isDigit(firstChar.charAt(0))) {
                            scrollTo(i);
                            break;
                        }
                    } else {
                        if (firstChar.compareTo(section) >= 0) {
                            scrollTo(i);
                            break;
                        }
                    }
                }
            }
        });

        return v;
    }

    private void scrollTo(int position) {
        layoutManager.scrollToPositionWithOffset(position, 0);
    }
}