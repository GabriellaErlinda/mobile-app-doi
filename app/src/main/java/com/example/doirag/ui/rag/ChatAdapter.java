package com.example.doirag.ui.rag;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doirag.R;

public class ChatAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {

    // Definisikan tipe view
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_ASSISTANT = 2;
    private static final int VIEW_TYPE_LOADING = 3;

    public ChatAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else if (viewType == VIEW_TYPE_ASSISTANT) {
            View view = inflater.inflate(R.layout.item_chat_assistant, parent, false);
            return new AssistantViewHolder(view);
        } else {
            // Untuk Tipe LOADING, kita pakai ulang layout assistant
            View view = inflater.inflate(R.layout.item_chat_assistant, parent, false);
            return new AssistantViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = getItem(position);
        int viewType = holder.getItemViewType();

        if (viewType == VIEW_TYPE_USER) {
            ((UserViewHolder) holder).bind(message);
        } else if (viewType == VIEW_TYPE_ASSISTANT) {
            ((AssistantViewHolder) holder).bind(message);
        } else if (viewType == VIEW_TYPE_LOADING) {
            // Tampilan loading
            ((AssistantViewHolder) holder).bindLoading();
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage.Type type = getItem(position).getType();
        if (type == ChatMessage.Type.USER) {
            return VIEW_TYPE_USER;
        } else if (type == ChatMessage.Type.ASSISTANT) {
            return VIEW_TYPE_ASSISTANT;
        } else {
            return VIEW_TYPE_LOADING;
        }
    }

    // --- ViewHolders ---

    // ViewHolder untuk item_chat_user.xml
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
        }

        void bind(ChatMessage message) {
            textMessage.setText(message.getMessage());
            textTime.setText(message.getFormattedTime());
        }
    }

    // ViewHolder untuk item_chat_assistant.xml
    static class AssistantViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime;
        // Kita tidak pakai citations dari XML, tapi Anda bisa menambahkannya nanti

        AssistantViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
        }

        void bind(ChatMessage message) {
            textMessage.setText(message.getMessage());
            textTime.setText(message.getFormattedTime());
        }

        // Fungsi khusus untuk "Loading..."
        void bindLoading() {
            textMessage.setText("...");
            textTime.setText("");
        }
    }

    // --- DiffUtil Callback ---
    private static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ChatMessage>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                    return oldItem.getTimestamp() == newItem.getTimestamp();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                    return oldItem.getMessage().equals(newItem.getMessage()) &&
                            oldItem.getType() == newItem.getType();
                }
            };
}