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

public class ObatSediaanFragment extends Fragment {

    private RecyclerView recycler;
    private FastScroller fastScroller;
    private ObatSediaanAdapter adapter;
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

        adapter = new ObatSediaanAdapter(item -> {
            // Create bundle with the item
            Bundle bundle = new Bundle();
            bundle.putSerializable("drug_item", item);

            // Navigate to Detail Page
            Navigation.findNavController(requireView())
                    .navigate(R.id.nav_drug_detail, bundle);
        });

        recycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);

        viewModel.getFilteredSediaanDrugs().observe(getViewLifecycleOwner(), sediaanItems -> {
            adapter.submitList(sediaanItems);
        });

        // Setup Fast Scroller Logic
        fastScroller.setListener(section -> {
            List<ObatSediaanItem> currentList = adapter.getCurrentList();
            if (currentList == null || currentList.isEmpty()) return;

            for (int i = 0; i < currentList.size(); i++) {
                String name = currentList.get(i).drug_name;
                if (name != null && !name.isEmpty()) {
                    String firstChar = name.substring(0, 1).toUpperCase();

                    if (section.equals("#")) {
                        // Check if it starts with a digit
                        if (Character.isDigit(firstChar.charAt(0))) {
                            scrollTo(i);
                            break;
                        }
                    } else {
                        // Check if the first character is >= the selected section
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