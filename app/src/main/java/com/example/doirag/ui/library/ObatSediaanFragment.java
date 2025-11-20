package com.example.doirag.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation; // IMPORT ADDED
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doirag.R;

public class ObatSediaanFragment extends Fragment {

    private RecyclerView recycler;
    private ObatSediaanAdapter adapter;
    private LibraryViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list_simple, container, false);

        recycler = v.findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

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

        return v;
    }
}