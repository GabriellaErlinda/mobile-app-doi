package com.example.doirag.ui.rag;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatMessage {

    // Tipe untuk menentukan siapa yg kirim
    public enum Type {
        USER,
        ASSISTANT,
        LOADING // Tipe untuk "..." saat AI sedang berpikir
    }

    private final String message;
    private final long timestamp;
    private final Type type;

    public ChatMessage(String message, Type type) {
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    // Getter
    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Type getType() {
        return type;
    }

    public String getFormattedTime() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "";
        }
    }
}