package com.example.doirag.ui.library;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LibraryViewModel extends AndroidViewModel {

    // --- HTTP Client ---
    private final OkHttpClient httpClient;
    private final Gson gson;

    // --- Config Supabase ---
    private final String SUPABASE_BASE_URL = "https://htkwoucfxjjthcoifaiq.supabase.co";
    private final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imh0a3dvdWNmeGpqdGhjb2lmYWlxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI4NTMwMDAsImV4cCI6MjA3ODQyOTAwMH0.6Gk-JfyIFO1vcIieeKmaMPYRjNJuSwQwaVuevItipE4";

    // --- Nama Tabel (Optional reference) ---
    private final String TABEL_GENERIK = "obat_generik";
    private final String TABEL_SEDIAAN = "obat_sediaan";

    // --- State (Input) ---
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Integer> currentTab = new MutableLiveData<>(1);

    // --- LiveData (Output) ---
    private final MutableLiveData<List<ObatGenerikItem>> filteredGenerikDrugs = new MutableLiveData<>();
    private final MutableLiveData<List<ObatSediaanItem>> filteredSediaanDrugs = new MutableLiveData<>();

    // NEW: LiveData for Dynamic Categories
    private final MutableLiveData<List<String>> categoryList = new MutableLiveData<>();

    private static final String TAG = "LibraryViewModel";

    public LibraryViewModel(@NonNull Application application) {
        super(application);
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();

        // Ambil data obat pertama kali
        fetchData();

        // Ambil daftar kategori untuk filter
        fetchCategories();
    }

    // --- FUNGSI KONTROL ---

    public void setCurrentTab(int position) {
        if (!Objects.equals(currentTab.getValue(), position)) {
            currentTab.setValue(position);
            fetchData();
        }
    }

    public void setSearchQuery(String query) {
        // Jangan panggil jika query sama untuk mencegah spam request
        if (!Objects.equals(query, searchQuery.getValue())) {
            searchQuery.setValue(query);
            fetchData();
        }
    }

    public LiveData<List<String>> getCategoryList() {
        return categoryList;
    }

    // --- FUNGSI "ROUTER" PENGAMBIL DATA ---

    private void fetchData() {
        String query = searchQuery.getValue();
        Integer tab = currentTab.getValue();

        if (tab == null) tab = 1; // Default ke Sediaan jika null

        if (tab == 0) {
            // Tab 0: Obat Generik
            // Saat ini masih placeholder, nanti Anda bisa buat RPC "search_obat_generik"
            // dan panggil method fetchGenerikDrugs(query) di sini.
            Log.d(TAG, "Placeholder: Mengambil data Generik query: " + query);
            fetchGenerikDrugsPlaceholder(query);

        } else if (tab == 1) {
            // Tab 1: Obat Sediaan
            fetchSediaanDrugs(query);
        }
    }

    // --- PENGAMBIL DATA: KATEGORI (Untuk Filter Chip) ---

    public void fetchCategories() {
        CompletableFuture.runAsync(() -> {
            try {
                // Memanggil RPC Function: get_unique_categories
                HttpUrl url = HttpUrl.parse(SUPABASE_BASE_URL + "/rest/v1/rpc/get_unique_categories")
                        .newBuilder()
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonString = response.body().string();

                        // Parse: [{"category": "Analgesik"}, {"category": "Antibiotik"}]
                        Type listType = new TypeToken<List<CategoryWrapper>>(){}.getType();
                        List<CategoryWrapper> rawList = gson.fromJson(jsonString, listType);

                        List<String> strings = new ArrayList<>();
                        if (rawList != null) {
                            for (CategoryWrapper item : rawList) {
                                if (item.category != null && !item.category.isEmpty()) {
                                    strings.add(item.category);
                                }
                            }
                        }

                        // Update LiveData
                        categoryList.postValue(strings);
                    } else {
                        Log.e(TAG, "Gagal ambil kategori: " + response.code());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching categories", e);
            }
        });
    }

    // --- PENGAMBIL DATA: OBAT SEDIAAN ---

    private void fetchSediaanDrugs(String query) {
        Log.d(TAG, "Mengambil data 'Sediaan' dengan RPC... Query: \"" + query + "\"");
        CompletableFuture.runAsync(() -> {
            try {
                HttpUrl url = HttpUrl.parse(SUPABASE_BASE_URL + "/rest/v1/rpc/search_obat_sediaan")
                        .newBuilder()
                        .build();

                Map<String, String> bodyMap = Collections.singletonMap("search_term", query);
                String jsonBody = gson.toJson(bodyMap);

                RequestBody body = RequestBody.create(
                        jsonBody,
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonString = response.body().string();
                        Type listType = new TypeToken<List<ObatSediaanItem>>(){}.getType();
                        List<ObatSediaanItem> hasil = gson.fromJson(jsonString, listType);

                        Log.d(TAG, "Berhasil! Ditemukan " + (hasil != null ? hasil.size() : 0) + " item sediaan.");
                        filteredSediaanDrugs.postValue(hasil != null ? hasil : Collections.emptyList());
                    } else {
                        Log.e(TAG, "Request Sediaan Gagal: " + response.code());
                        filteredSediaanDrugs.postValue(Collections.emptyList());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error Sediaan: " + e.getMessage());
                filteredSediaanDrugs.postValue(Collections.emptyList());
            }
        });
    }

    // --- PENGAMBIL DATA: OBAT GENERIK (Placeholder) ---

    private void fetchGenerikDrugsPlaceholder(String query) {
        // TODO: Implementasi Fetch Generik serupa dengan Sediaan
        // 1. Buat RPC search_obat_generik di Supabase
        // 2. Copy paste logic dari fetchSediaanDrugs dan sesuaikan Type token ke List<ObatGenerikItem>

        // Saat ini return list kosong agar UI tidak error
        filteredGenerikDrugs.postValue(Collections.emptyList());
    }

    public LiveData<List<ObatGenerikItem>> getFilteredGenerikDrugs() {
        return filteredGenerikDrugs;
    }

    public LiveData<List<ObatSediaanItem>> getFilteredSediaanDrugs() {
        return filteredSediaanDrugs;
    }

    // --- Helper Class for Category Parsing ---
    private static class CategoryWrapper {
        @SerializedName("category_main") // Sesuaikan dengan nama kolom return function SQL Anda
        String category;
    }
}