package com.example.doirag.ui.rag;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.example.doirag.R;
import com.example.doirag.databinding.FragmentRagBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;


public class RagFragment extends Fragment {

    private FragmentRagBinding binding;
    private RagViewModel viewModel;
    private ChatAdapter adapter;
    private RecyclerView recyclerChat;
    private TextInputEditText inputMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRagBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 1. Dapatkan ViewModel
        viewModel = new ViewModelProvider(this).get(RagViewModel.class);

        // 2. Setup RecyclerView
        recyclerChat = binding.recyclerChat;
        adapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true); // Pesan baru muncul dari bawah
        recyclerChat.setLayoutManager(layoutManager);
        recyclerChat.setAdapter(adapter);
        recyclerChat.addItemDecoration(new ChatSpacingDecoration(12)); // Hapus jika tidak mau

        // 3. Setup Input dan Tombol Kirim
        inputMessage = binding.inputMessage;

        // Kirim saat tombol 'Enter/Send' di keyboard ditekan
        inputMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // Kirim saat tombol 'Send' (MaterialButton) ditekan
        binding.buttonSend.setOnClickListener(v -> sendMessage());

        // 4. Observe LiveData dari ViewModel
        viewModel.getChatMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.submitList(messages);

            // Auto-scroll ke pesan terbaru
            recyclerChat.post(() -> {
                recyclerChat.smoothScrollToPosition(adapter.getItemCount() - 1);
            });
        });

        // 5. Setup Chip Prompts
        setupPromptChips();

        // Listen for keyboard (IME) insets and add padding to the bottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            // Apply padding to the bottom equal to the keyboard height
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeInsets.bottom);
            return windowInsets;
        });

        return root;
    }

    // Helper untuk mengirim pesan
    private void sendMessage() {
        String query = inputMessage.getText().toString();
        if (query.trim().isEmpty()) return;

        // Kirim pesan ke ViewModel
        viewModel.sendMessage(query);

        // Kosongkan input
        inputMessage.setText("");
    }

    private void setupPromptChips() {
        String[] prompts = new String[]{
                "Indikasi & kontraindikasi",
                "Dosis dewasa & anak",
                "Interaksi obat",
                "Efek samping umum"
        };
        binding.chipGroupPrompts.removeAllViews();
        for (String p : prompts) {
            Chip chip = new Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Assist_Elevated);
            chip.setText(p);
            chip.setOnClickListener(v -> {
                binding.inputMessage.setText(p + ": ");
                binding.inputMessage.setSelection(binding.inputMessage.getText().length());
            });
            binding.chipGroupPrompts.addView(chip);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}