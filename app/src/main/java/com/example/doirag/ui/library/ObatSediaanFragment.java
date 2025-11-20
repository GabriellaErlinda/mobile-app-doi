package com.example.doirag.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doirag.R;

// Ganti nama class menjadi ObatSediaanFragment
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
            // TODO: Handle klik obat sediaan
        });
        recycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);

        // Meng-observe data sediaan
        viewModel.getFilteredSediaanDrugs().observe(getViewLifecycleOwner(), sediaanItems -> {
            adapter.submitList(sediaanItems);
        });

        return v;
    }
}