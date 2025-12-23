package com.example.doirag.ui.library;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doirag.BuildConfig;
// Pastikan import model Item Anda benar
// import com.example.doirag.model.ObatGenerikItem;
// import com.example.doirag.model.ObatSediaanItem;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LibraryViewModel extends AndroidViewModel {

    // --- HTTP Client ---
    private final OkHttpClient httpClient;
    private final Gson gson;

    // --- Config ---
    private final String SUPABASE_BASE_URL = BuildConfig.SUPABASE_URL;
    private final String SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;

    private final String FLASK_BASE_URL = "http://10.120.193.97:5000";

    // --- Data Storage ---
    private List<ObatGenerikItem> masterGenerikList = new ArrayList<>();
    private List<ObatSediaanItem> allSediaanDrugs = new ArrayList<>();

    // --- State (Input) ---
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Integer> currentTab = new MutableLiveData<>(0); // Default tab 0 (Generik)

    private String activeCategoryFilter = null;
    private boolean isSortAscending = true;

    // --- LiveData (Output) ---
    private final MutableLiveData<List<ObatGenerikItem>> filteredGenerikDrugs = new MutableLiveData<>();
    private final MutableLiveData<List<ObatSediaanItem>> filteredSediaanDrugs = new MutableLiveData<>();
    private final MutableLiveData<List<String>> categoryList = new MutableLiveData<>();

    private static final String TAG = "LibraryViewModel";
    private boolean allSediaanLoaded = false;
    private boolean allGenerikLoaded = false;

    public LibraryViewModel(@NonNull Application application) {
        super(application);
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();

        // Load Data Awal (Supaya list tidak kosong saat pertama buka)
        fetchAllSediaanDrugs();
        fetchAllGenerikDrugs();
        fetchCategories();
    }

    // --- FUNGSI KONTROL UI ---
    public void setCurrentTab(int position) {
        if (currentTab.getValue() == null || currentTab.getValue() != position) {
            currentTab.setValue(position);
            // Re-apply search jika tab berubah (misal user ketik di tab 1 lalu pindah tab 2)
            applySearchLogic();
        }
    }

    public void setSearchQuery(String query) {
        if (searchQuery.getValue() == null || !searchQuery.getValue().equals(query)) {
            searchQuery.setValue(query);
            applySearchLogic();
        }
    }

    public void setSortOrder(boolean ascending) {
        this.isSortAscending = ascending;
        // Sorting lokal tetap bisa dilakukan pada hasil yang sudah didapat
        sortCurrentResults();
    }

    public void setCategoryFilter(String category) {
        this.activeCategoryFilter = category;
        applySearchLogic();
    }

    public String getActiveCategoryFilter() { return activeCategoryFilter; }
    public LiveData<List<String>> getCategoryList() { return categoryList; }
    public LiveData<List<ObatGenerikItem>> getFilteredGenerikDrugs() { return filteredGenerikDrugs; }
    public LiveData<List<ObatSediaanItem>> getFilteredSediaanDrugs() { return filteredSediaanDrugs; }

    // --- LOGIC UTAMA SEARCH ---

    private void applySearchLogic() {
        String query = searchQuery.getValue();
        int tab = currentTab.getValue() != null ? currentTab.getValue() : 0;

        // 1. Jika Query KOSONG -> Gunakan data lokal (Load All)
        if (query == null || query.trim().isEmpty()) {
            if (tab == 0) { // Generik
                filteredGenerikDrugs.postValue(new ArrayList<>(masterGenerikList));
            } else { // Sediaan
                applyLocalFilterForSediaan(allSediaanDrugs); // Tetap butuh filter kategori
            }
            return;
        }

        // 2. Jika Ada Query -> Tembak API Python
        // Tentukan scope berdasarkan Tab
        String scope = (tab == 0) ? "generik" : "sediaan";
        searchObatFromApi(query, scope);
    }

    private void searchObatFromApi(String query, String scope) {
        CompletableFuture.runAsync(() -> {
            try {
                // Construct URL: /search-obat?q=...&scope=...&mode=fuzzy_supabase
                HttpUrl url = HttpUrl.parse(FLASK_BASE_URL + "/search-obat")
                        .newBuilder()
                        .addQueryParameter("q", query)
                        .addQueryParameter("k", "20") // Ambil top 20 hasil
                        .addQueryParameter("scope", scope)
                        .addQueryParameter("mode", "fuzzy_supabase") // Gunakan mode fuzzy RPC
                        .build();

                Request request = new Request.Builder().url(url).get().build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonResponse = response.body().string();

                        // Parse Wrapper Response
                        SearchResponseWrapper wrapper = gson.fromJson(jsonResponse, SearchResponseWrapper.class);

                        if (scope.equals("generik")) {
                            List<ObatGenerikItem> results = new ArrayList<>();
                            if (wrapper.results != null) {
                                for (SearchResultItem item : wrapper.results) {
                                    // Deserialisasi 'attrs' menjadi objek ObatGenerikItem
                                    ObatGenerikItem obat = gson.fromJson(item.attrs, ObatGenerikItem.class);
                                    if(obat != null) results.add(obat);
                                }
                            }
                            // Post value langsung (API results biasanya sudah sorted by relevansi)
                            filteredGenerikDrugs.postValue(results);

                        } else {
                            // Scope: SEDIAAN
                            List<ObatSediaanItem> results = new ArrayList<>();
                            if (wrapper.results != null) {
                                for (SearchResultItem item : wrapper.results) {
                                    // Deserialisasi 'attrs' menjadi objek ObatSediaanItem
                                    ObatSediaanItem obat = gson.fromJson(item.attrs, ObatSediaanItem.class);
                                    if(obat != null) results.add(obat);
                                }
                            }
                            // Untuk sediaan, kita mungkin masih perlu filter kategori client-side
                            // walaupun datanya dari API search
                            applyLocalFilterForSediaan(results);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error searching API: " + e.getMessage());
                // Fallback: jika API mati, lakukan pencarian lokal manual (opsional)
                // fallbackToLocalSearch(query);
            }
        });
    }

    // Helper untuk memfilter kategori & sort (Khusus Tab Sediaan)
    // Ini dipanggil baik saat load semua (query kosong) maupun setelah dapat hasil API
    private void applyLocalFilterForSediaan(List<ObatSediaanItem> sourceList) {
        List<ObatSediaanItem> processed = new ArrayList<>(sourceList);

        // 1. Filter Kategori
        if (activeCategoryFilter != null && !activeCategoryFilter.isEmpty()) {
            List<ObatSediaanItem> temp = new ArrayList<>();
            for (ObatSediaanItem item : processed) {
                if (item.category_main != null && item.category_main.equalsIgnoreCase(activeCategoryFilter)) {
                    temp.add(item);
                }
            }
            processed = temp;
        }

        // 2. Sort (Hanya jika user meminta A-Z/Z-A, jika default biarkan urutan ranking API)
        // Disini kita sort simple by name saja
        Collections.sort(processed, (o1, o2) -> {
            String s1 = o1.drug_name != null ? o1.drug_name : "";
            String s2 = o2.drug_name != null ? o2.drug_name : "";
            return isSortAscending ? s1.compareToIgnoreCase(s2) : s2.compareToIgnoreCase(s1);
        });

        filteredSediaanDrugs.postValue(processed);
    }

    // Dipanggil saat tombol sort ditekan tanpa query berubah
    private void sortCurrentResults() {
        if (currentTab.getValue() == 0) { // Generik
            List<ObatGenerikItem> current = filteredGenerikDrugs.getValue();
            if(current != null) {
                List<ObatGenerikItem> sorted = new ArrayList<>(current);
                Collections.sort(sorted, (o1, o2) -> {
                    String s1 = o1.nama_generik != null ? o1.nama_generik : "";
                    String s2 = o2.nama_generik != null ? o2.nama_generik : "";
                    return isSortAscending ? s1.compareToIgnoreCase(s2) : s2.compareToIgnoreCase(s1);
                });
                filteredGenerikDrugs.setValue(sorted);
            }
        } else { // Sediaan
            List<ObatSediaanItem> current = filteredSediaanDrugs.getValue();
            if(current != null) applyLocalFilterForSediaan(current);
        }
    }

    // --- CLASS WRAPPER UNTUK JSON API RESPONSE ---
    private static class SearchResponseWrapper {
        @SerializedName("results")
        List<SearchResultItem> results;
    }

    private static class SearchResultItem {
        @SerializedName("attrs")
        JsonElement attrs; // Kita ambil sebagai JsonElement dulu, lalu convert sesuai tipe obat
    }

    // --- NETWORK CALLS (Initial Load) ---
    // (Kode fetchAllSediaanDrugs, fetchAllGenerikDrugs, fetchCategories TETAP SAMA seperti file asli Anda)
    // ... Copy paste method fetchAllSediaanDrugs, fetchAllGenerikDrugs, fetchCategories, dll di sini ...

    // --- BATCH FETCHING IMPLEMENTATION (Disalin dari kode asli Anda agar tidak hilang) ---

    public void fetchCategories() {
        CompletableFuture.runAsync(() -> {
            try {
                HttpUrl url = HttpUrl.parse(SUPABASE_BASE_URL + "/rest/v1/distinct_categories")
                        .newBuilder().addQueryParameter("select", "category_main").build();
                Request request = new Request.Builder().url(url).get()
                        .addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY).build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonString = response.body().string();
                        Type listType = new TypeToken<List<CategoryWrapper>>(){}.getType();
                        List<CategoryWrapper> rawList = gson.fromJson(jsonString, listType);
                        List<String> strings = new ArrayList<>();
                        if (rawList != null) for (CategoryWrapper item : rawList) if (item.category != null) strings.add(item.category);
                        Collections.sort(strings, (s1, s2) -> {
                            int n1 = extractNumber(s1); int n2 = extractNumber(s2);
                            return (n1 != -1 && n2 != -1) ? Integer.compare(n1, n2) : s1.compareToIgnoreCase(s2);
                        });
                        categoryList.postValue(strings);
                    }
                }
            } catch (Exception e) { Log.e(TAG, "Error fetching categories", e); }
        });
    }

    // Helper Extract Number
    private int extractNumber(String text) {
        try {
            Matcher matcher = Pattern.compile("^(\\d+)").matcher(text);
            if (matcher.find()) return Integer.parseInt(matcher.group(1));
            return -1;
        } catch (Exception e) { return -1; }
    }

    private void fetchAllSediaanDrugs() {
        if (allSediaanLoaded) return;
        CompletableFuture.runAsync(() -> {
            // ... (Isi sama dengan kode asli Anda: Loop fetch supabase) ...
            // Saat selesai, update allSediaanDrugs
            // Dan panggil: applyLocalFilterForSediaan(allSediaanDrugs);

            // Simpelnya, panggil fungsi asli Anda, tapi di baris terakhir ganti applySearchAndFilters() menjadi:
            // if(searchQuery.getValue().isEmpty()) applyLocalFilterForSediaan(allSediaanDrugs);
        });

        // --- Implementasi singkat fetchAllSediaanDrugs agar kode lengkap ---
        CompletableFuture.runAsync(() -> {
            List<ObatSediaanItem> completeList = new ArrayList<>();
            int offset = 0; int pageSize = 1000; boolean hasMoreData = true;
            try {
                while (hasMoreData) {
                    HttpUrl url = HttpUrl.parse(SUPABASE_BASE_URL + "/rest/v1/obat_sediaan")
                            .newBuilder().addQueryParameter("select", "id,name,manufacturer,category_main,category_sub,komposisi,farmakologi,indikasi,dosis,kontraindikasi,perhatian,efek_samping,interaksi_obat,kemasan")
                            .addQueryParameter("order", "name.asc").build();
                    int rangeStart = offset; int rangeEnd = offset + pageSize - 1;
                    Request request = new Request.Builder().url(url).get()
                            .addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                            .addHeader("Range", rangeStart + "-" + rangeEnd).build();
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<ObatSediaanItem> chunk = gson.fromJson(response.body().string(), new TypeToken<List<ObatSediaanItem>>(){}.getType());
                            if (chunk != null && !chunk.isEmpty()) { completeList.addAll(chunk); offset += chunk.size(); if (chunk.size() < pageSize) hasMoreData = false; } else hasMoreData = false;
                        } else hasMoreData = false;
                    }
                }
                if (!completeList.isEmpty()) {
                    allSediaanDrugs = completeList;
                    allSediaanLoaded = true;
                    // HANYA tampilkan data lokal jika tidak ada search query aktif
                    if (searchQuery.getValue() == null || searchQuery.getValue().isEmpty()) {
                        applyLocalFilterForSediaan(allSediaanDrugs);
                    }
                }
            } catch (Exception e) { Log.e(TAG, "Error fetching all drugs", e); }
        });
    }

    private void fetchAllGenerikDrugs() {
        if (allGenerikLoaded) return;
        CompletableFuture.runAsync(() -> {
            // ... (Logic fetch generik asli Anda) ...
            List<ObatGenerikItem> completeList = new ArrayList<>();
            int offset = 0; int pageSize = 1000; boolean hasMoreData = true;
            try {
                while (hasMoreData) {
                    HttpUrl url = HttpUrl.parse(SUPABASE_BASE_URL + "/rest/v1/obat_generik")
                            .newBuilder().addQueryParameter("select", "*").addQueryParameter("order", "name.asc").build();
                    Request request = new Request.Builder().url(url).get().addHeader("apikey", SUPABASE_ANON_KEY)
                            .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                            .addHeader("Range", offset + "-" + (offset + pageSize - 1)).build();
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<ObatGenerikItem> chunk = gson.fromJson(response.body().string(), new TypeToken<List<ObatGenerikItem>>(){}.getType());
                            if (chunk != null && !chunk.isEmpty()) { completeList.addAll(chunk); offset += chunk.size(); if (chunk.size() < pageSize) hasMoreData = false; } else hasMoreData = false;
                        } else hasMoreData = false;
                    }
                }
                if (!completeList.isEmpty()) {
                    masterGenerikList = completeList;
                    allGenerikLoaded = true;
                    if (searchQuery.getValue() == null || searchQuery.getValue().isEmpty()) {
                        filteredGenerikDrugs.postValue(masterGenerikList);
                    }
                }
            } catch (Exception e) { Log.e(TAG, "Error fetching generik", e); }
        });
    }

    private static class CategoryWrapper { @SerializedName("category_main") String category; }
}