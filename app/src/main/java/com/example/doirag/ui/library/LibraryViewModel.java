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

    // --- Data Storage (Master Lists for Local Filtering) ---
    private List<ObatGenerikItem> masterGenerikList = new ArrayList<>();
    private List<ObatSediaanItem> masterSediaanList = new ArrayList<>();
    private List<ObatSediaanItem> allSediaanDrugs = new ArrayList<>(); // Store ALL drugs

    // --- State (Input) ---
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Integer> currentTab = new MutableLiveData<>(1);

    // Filtering & Sorting State
    private String activeCategoryFilter = null; // null means "All"
    private boolean isSortAscending = true; // A-Z default

    // --- LiveData (Output) ---
    private final MutableLiveData<List<ObatGenerikItem>> filteredGenerikDrugs = new MutableLiveData<>();
    private final MutableLiveData<List<ObatSediaanItem>> filteredSediaanDrugs = new MutableLiveData<>();
    private final MutableLiveData<List<String>> categoryList = new MutableLiveData<>();

    private static final String TAG = "LibraryViewModel";
    private boolean allDrugsLoaded = false;

    public LibraryViewModel(@NonNull Application application) {
        super(application);
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();

        // Load ALL drugs on initialization
        fetchAllSediaanDrugs();
        fetchCategories();
    }

    // --- FUNGSI KONTROL ---

    public void setCurrentTab(int position) {
        if (!Objects.equals(currentTab.getValue(), position)) {
            currentTab.setValue(position);
        }
    }

    public void setSearchQuery(String query) {
        if (!Objects.equals(query, searchQuery.getValue())) {
            searchQuery.setValue(query);
            applySearchAndFilters();
        }
    }

    // NEW: Apply Sort
    public void setSortOrder(boolean ascending) {
        this.isSortAscending = ascending;
        applySearchAndFilters();
        processGenerikList();
    }

    // NEW: Apply Filter
    public void setCategoryFilter(String category) {
        this.activeCategoryFilter = category;
        applySearchAndFilters();
    }

    public String getActiveCategoryFilter() {
        return activeCategoryFilter;
    }

    public LiveData<List<String>> getCategoryList() {
        return categoryList;
    }

    public LiveData<List<ObatGenerikItem>> getFilteredGenerikDrugs() {
        return filteredGenerikDrugs;
    }

    public LiveData<List<ObatSediaanItem>> getFilteredSediaanDrugs() {
        return filteredSediaanDrugs;
    }

    // --- INTERNAL PROCESSING (Search, Sort & Filter) ---

    private void applySearchAndFilters() {
        if (allSediaanDrugs.isEmpty()) return;

        String query = searchQuery.getValue();
        List<ObatSediaanItem> processed = new ArrayList<>();

        // 1. Apply Search Filter
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase().trim();
            for (ObatSediaanItem item : allSediaanDrugs) {
                if ((item.drug_name != null && item.drug_name.toLowerCase().contains(lowerQuery)) ||
                        (item.manufacturer != null && item.manufacturer.toLowerCase().contains(lowerQuery)) ||
                        (item.category_main != null && item.category_main.toLowerCase().contains(lowerQuery)) ||
                        (item.category_sub != null && item.category_sub.toLowerCase().contains(lowerQuery))) {
                    processed.add(item);
                }
            }
        } else {
            processed.addAll(allSediaanDrugs);
        }

        // 2. Apply Category Filter
        if (activeCategoryFilter != null && !activeCategoryFilter.isEmpty()) {
            List<ObatSediaanItem> categoryFiltered = new ArrayList<>();
            for (ObatSediaanItem item : processed) {
                if (item.category_main != null &&
                        item.category_main.equalsIgnoreCase(activeCategoryFilter)) {
                    categoryFiltered.add(item);
                }
            }
            processed = categoryFiltered;
        }

        // 3. Sort (Alphabetical)
        Collections.sort(processed, (o1, o2) -> {
            String s1 = o1.drug_name != null ? o1.drug_name : "";
            String s2 = o2.drug_name != null ? o2.drug_name : "";
            if (isSortAscending) {
                return s1.compareToIgnoreCase(s2);
            } else {
                return s2.compareToIgnoreCase(s1);
            }
        });

        filteredSediaanDrugs.postValue(processed);
    }

    private void processGenerikList() {
        if (masterGenerikList == null) return;

        List<ObatGenerikItem> processed = new ArrayList<>(masterGenerikList);

        // 1. Sort (Alphabetical)
        Collections.sort(processed, (o1, o2) -> {
            String s1 = o1.nama_generik != null ? o1.nama_generik : "";
            String s2 = o2.nama_generik != null ? o2.nama_generik : "";
            if (isSortAscending) {
                return s1.compareToIgnoreCase(s2);
            } else {
                return s2.compareToIgnoreCase(s1);
            }
        });

        filteredGenerikDrugs.postValue(processed);
    }

    // --- NETWORK CALLS ---

    public void fetchCategories() {
        CompletableFuture.runAsync(() -> {
            try {
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
                        Type listType = new TypeToken<List<CategoryWrapper>>(){}.getType();
                        List<CategoryWrapper> rawList = gson.fromJson(jsonString, listType);

                        List<String> strings = new ArrayList<>();
                        if (rawList != null) {
                            for (CategoryWrapper item : rawList) {
                                if (item.category != null && !item.category.isEmpty()) {
                                    strings.add(item.category);
                                }
                            }
                            Collections.sort(strings);
                        }
                        categoryList.postValue(strings);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching categories", e);
            }
        });
    }

    // NEW: Fetch ALL drugs from database
    private void fetchAllSediaanDrugs() {
        if (allDrugsLoaded) return;

        CompletableFuture.runAsync(() -> {
            try {
                // Use direct table query instead of search RPC to get ALL records
                HttpUrl url = HttpUrl.parse(SUPABASE_BASE_URL + "/rest/v1/obat_sediaan")
                        .newBuilder()
                        .addQueryParameter("select", "id,name,manufacturer,category_main,category_sub,komposisi,farmakologi,indikasi,dosis,kontraindikasi,perhatian,efek_samping,interaksi_obat,kemasan")
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .addHeader("Range", "0-9999") // Request up to 10000 records
                        .build();

                Log.d(TAG, "Fetching all drugs from: " + url);

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonString = response.body().string();
                        Type listType = new TypeToken<List<ObatSediaanItem>>(){}.getType();
                        List<ObatSediaanItem> hasil = gson.fromJson(jsonString, listType);

                        if (hasil != null && !hasil.isEmpty()) {
                            allSediaanDrugs = hasil;
                            allDrugsLoaded = true;
                            Log.d(TAG, "Successfully loaded " + hasil.size() + " drugs");

                            // Apply initial filters
                            applySearchAndFilters();
                        } else {
                            Log.w(TAG, "No drugs returned from database");
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch drugs: " + response.code() + " " + response.message());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching all drugs: " + e.getMessage(), e);
                allSediaanDrugs = new ArrayList<>();
                applySearchAndFilters();
            }
        });
    }

    private void fetchGenerikDrugsPlaceholder(String query) {
        // Placeholder logic - implement similarly to fetchAllSediaanDrugs if needed
        masterGenerikList = new ArrayList<>();
        processGenerikList();
    }

    private static class CategoryWrapper {
        @SerializedName("category_main")
        String category;
    }
}