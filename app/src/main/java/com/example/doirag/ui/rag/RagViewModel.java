package com.example.doirag.ui.rag;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap; // Diganti dari Collections karena butuh put 2 data
import java.util.List;
import java.util.Map;
import java.util.UUID; // Import untuk Session ID unik
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RagViewModel extends AndroidViewModel {

    private static final String TAG = "RagViewModel";

    private static final String FLASK_BACKEND_URL = "http://10.13.215.97:5000/tanya-obat";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String currentSessionId;

    private final MutableLiveData<List<ChatMessage>> chatMessages = new MutableLiveData<>();
    private final List<ChatMessage> messageList = new ArrayList<>();

    public RagViewModel(@NonNull Application application) {
        super(application);

        this.currentSessionId = UUID.randomUUID().toString();
        Log.d(TAG, "Session ID initialized: " + currentSessionId);

        // Setup Timeout (LLM butuh waktu lama, pertahankan timeout tinggi)
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS) // Backend Senopati melakukan RAG + LLM, butuh waktu
                .retryOnConnectionFailure(true)
                .build();

        this.gson = new Gson();

        // Pesan sambutan
        messageList.add(new ChatMessage(
                "Halo! Saya Medibot asisten apoteker Anda. Ada yang bisa saya bantu terkait informasi obat?",
                ChatMessage.Type.ASSISTANT
        ));
        chatMessages.setValue(new ArrayList<>(messageList));
    }

    public LiveData<List<ChatMessage>> getChatMessages() {
        return chatMessages;
    }

    public void sendMessage(String userQuery) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return;
        }

        // 1. Tambah pesan user ke UI
        ChatMessage userMessage = new ChatMessage(userQuery, ChatMessage.Type.USER);
        messageList.add(userMessage);

        // 2. Tambah indikator loading
        ChatMessage loadingMessage = new ChatMessage("...", ChatMessage.Type.LOADING);
        messageList.add(loadingMessage);

        chatMessages.setValue(new ArrayList<>(messageList));

        // 3. Request ke Backend Senopati
        CompletableFuture.runAsync(() -> {
            try {
                // Buat Payload JSON: {"pertanyaan": "...", "session_id": "..."}
                Map<String, String> bodyMap = new HashMap<>();
                bodyMap.put("pertanyaan", userQuery);
                bodyMap.put("session_id", currentSessionId);

                String jsonBody = gson.toJson(bodyMap);

                RequestBody body = RequestBody.create(
                        jsonBody,
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(FLASK_BACKEND_URL)
                        .post(body)
                        .build();

                Log.d(TAG, "Mengirim ke Senopati: " + jsonBody);

                try (Response response = httpClient.newCall(request).execute()) {
                    String responseJson = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Respon Senopati: " + responseJson);

                    if (response.isSuccessful()) {
                        FlaskResponse flaskResponse = gson.fromJson(responseJson, FlaskResponse.class);

                        if (flaskResponse != null && flaskResponse.jawaban != null) {
                            // Sukses: Tampilkan jawaban AI
                            updateLastMessage(new ChatMessage(
                                    flaskResponse.jawaban,
                                    ChatMessage.Type.ASSISTANT
                            ));
                        } else {
                            updateLastMessage(new ChatMessage(
                                    "Maaf, respon server kosong.",
                                    ChatMessage.Type.ASSISTANT
                            ));
                        }
                    } else {
                        updateLastMessage(new ChatMessage(
                                "Gagal terhubung ke Senopati (Error " + response.code() + ")",
                                ChatMessage.Type.ASSISTANT
                        ));
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error network", e);
                updateLastMessage(new ChatMessage(
                        "Terjadi kesalahan koneksi: " + e.getMessage(),
                        ChatMessage.Type.ASSISTANT
                ));
            }
        });
    }

    private void updateLastMessage(ChatMessage newMessage) {
        if (messageList.isEmpty()) return;

        // Hapus bubble loading "..."
        messageList.remove(messageList.size() - 1);
        // Masukkan jawaban asli
        messageList.add(newMessage);

        // Update UI (postValue aman untuk background thread)
        chatMessages.postValue(new ArrayList<>(messageList));
    }

    // Class model untuk memparsing JSON balasan dari app-senopati2.py
    private static class FlaskResponse {
        @SerializedName("jawaban")
        String jawaban;

        @SerializedName("session_id") // Backend sekarang mengembalikan session_id juga
        String sessionId;
    }
}