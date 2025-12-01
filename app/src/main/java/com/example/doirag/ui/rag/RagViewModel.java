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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RagViewModel extends AndroidViewModel {

    private static final String TAG = "RagViewModel";
    // Sesuaikan endpoint Flask
    private static final String FLASK_BACKEND_URL = "http://192.168.2.247:5000/tanya-obat";

    private final OkHttpClient httpClient;
    private final Gson gson;

    private final MutableLiveData<List<ChatMessage>> chatMessages = new MutableLiveData<>();
    private final List<ChatMessage> messageList = new ArrayList<>();

    public RagViewModel(@NonNull Application application) {
        super(application);

        // Pakai timeout yang lebih besar supaya tidak cepat timeout saat RAG + Gemini
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)   // waktu sambung ke server
                .writeTimeout(30, TimeUnit.SECONDS)     // waktu kirim body
                .readTimeout(90, TimeUnit.SECONDS)      // waktu tunggu jawaban server
                // .callTimeout(0, TimeUnit.SECONDS)    // 0 = no global timeout (opsional)
                .retryOnConnectionFailure(true)
                .build();

        this.gson = new Gson();

        // Pesan sambutan awal dari asisten
        messageList.add(new ChatMessage(
                "Halo! Ada yang bisa saya bantu terkait informasi obat?",
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

        // 1. Tambahkan pesan user
        ChatMessage userMessage = new ChatMessage(userQuery, ChatMessage.Type.USER);
        messageList.add(userMessage);

        // 2. Tambahkan pesan "loading"
        ChatMessage loadingMessage = new ChatMessage("...", ChatMessage.Type.LOADING);
        messageList.add(loadingMessage);

        // 3. Update UI (dipanggil dari main thread)
        chatMessages.setValue(new ArrayList<>(messageList));

        // 4. Panggilan jaringan di background thread
        CompletableFuture.runAsync(() -> {
            try {
                // Body JSON yang dikirim ke Flask
                Map<String, String> bodyMap = Collections.singletonMap("pertanyaan", userQuery);
                String jsonBody = gson.toJson(bodyMap);

                RequestBody body = RequestBody.create(
                        jsonBody,
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(FLASK_BACKEND_URL)
                        .post(body)
                        .build();

                Log.d(TAG, "Mengirim request ke: " + FLASK_BACKEND_URL);
                Log.d(TAG, "Request body: " + jsonBody);

                // try-with-resources agar response tertutup otomatis
                try (Response response = httpClient.newCall(request).execute()) {
                    int code = response.code();
                    String responseJson = response.body() != null ? response.body().string() : "";

                    Log.d(TAG, "HTTP code: " + code);
                    Log.d(TAG, "Response JSON: " + responseJson);

                    if (response.isSuccessful()) {
                        // Parse JSON dari Flask, misalnya: { "jawaban": "..." }
                        FlaskResponse flaskResponse = gson.fromJson(responseJson, FlaskResponse.class);

                        if (flaskResponse == null || flaskResponse.jawaban == null) {
                            Log.e(TAG, "FlaskResponse null / field 'jawaban' null");
                            updateLastMessage(new ChatMessage(
                                    "Maaf, format balasan server tidak sesuai.\n" + responseJson,
                                    ChatMessage.Type.ASSISTANT
                            ));
                        } else {
                            // Ganti "loading" dengan jawaban AI
                            updateLastMessage(new ChatMessage(
                                    flaskResponse.jawaban,
                                    ChatMessage.Type.ASSISTANT
                            ));
                        }
                    } else {
                        Log.e(TAG, "Request gagal: " + code + " " + response.message());
                        updateLastMessage(new ChatMessage(
                                "Maaf, terjadi kesalahan pada server (" + code + ").",
                                ChatMessage.Type.ASSISTANT
                        ));
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error saat memanggil backend", e);
                updateLastMessage(new ChatMessage(
                        "Maaf, terjadi error: " + e.getClass().getSimpleName() + " - " + e.getMessage(),
                        ChatMessage.Type.ASSISTANT
                ));
            }
        });
    }


    private void updateLastMessage(ChatMessage newMessage) {
        if (messageList.isEmpty()) return;

        // Hapus pesan "loading" terakhir
        messageList.remove(messageList.size() - 1);
        // Tambahkan pesan baru (jawaban AI / error)
        messageList.add(newMessage);

        // postValue karena dipanggil dari background thread
        chatMessages.postValue(new ArrayList<>(messageList));
    }

    private static class FlaskResponse {
        @SerializedName("jawaban")
        String jawaban;
    }
}
