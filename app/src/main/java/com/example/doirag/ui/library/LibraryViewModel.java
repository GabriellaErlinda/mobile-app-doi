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

    // --- Config Supabase ---
    private final String SUPABASE_BASE_URL = "https://htkwoucfxjjthcoifaiq.supabase.co";
    private final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imh0a3dvdWNmeGpqdGhjb2lmYWlxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI4NTMwMDAsImV4cCI6MjA3ODQyOTAwMH0.6Gk-JfyIFO1vcIieeKmaMPYRjNJuSwQwaVuevItipE4";

    // --- Data Storage ---
    private List<ObatGenerikItem> masterGenerikList = new ArrayList<>();
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

        // Load Data on initialization
        fetchAllSediaanDrugs();
        fetchCategories();
    }

    // --- FUNGSI KONTROL UI ---

    public void setCurrentTab(int position) {
        if (currentTab.getValue() == null || currentTab.getValue() != position) {
            currentTab.setValue(position);
        }
    }

    public void setSearchQuery(String query) {
        if (searchQuery.getValue() == null || !searchQuery.getValue().equals(query)) {
            searchQuery.setValue(query);
            applySearchAndFilters();
        }
    }

    public void setSortOrder(boolean ascending) {
        this.isSortAscending = ascending;
        applySearchAndFilters();
        processGenerikList();
    }

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

    // --- LOGIC FILTERING & SORTING ---

    private void applySearchAndFilters() {
        if (allSediaanDrugs.isEmpty()) return;

        String query = searchQuery.getValue();
        List<ObatSediaanItem> processed = new ArrayList<>();

        // 1. Apply Search
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

        // 3. Sort
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
                // Get data from view (the SQL order is helpful but we will refine it in Java)
                HttpUrl url = HttpUrl.parse(SUPABASE_BASE_URL + "/rest/v1/distinct_categories")
                        .newBuilder()
                        .addQueryParameter("select", "category_main")
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

                            // --- CUSTOM NATURAL SORTING (1, 2, ... 10) ---
                            Collections.sort(strings, (s1, s2) -> {
                                int n1 = extractNumber(s1);
                                int n2 = extractNumber(s2);

                                if (n1 != -1 && n2 != -1) {
                                    // Both have numbers, compare numbers numerically
                                    return Integer.compare(n1, n2);
                                } else {
                                    // Fallback to standard string comparison if one doesn't have a number
                                    return s1.compareToIgnoreCase(s2);
                                }
                            });
                        }
                        categoryList.postValue(strings);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching categories", e);
            }
        });
    }

    // Helper to extract the leading number from strings like "1. Sistem Saluran..."
    private int extractNumber(String text) {
        try {
            if (text == null || text.isEmpty()) return -1;

            // Regex to find digits at the start of the string
            Matcher matcher = Pattern.compile("^(\\d+)").matcher(text);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
            return -1; // No number found
        } catch (Exception e) {
            return -1;
        }
    }

    // Batch Fetching Implementation
    private void fetchAllSediaanDrugs() {
        if (allDrugsLoaded) return;

        CompletableFuture.runAsync(() -> {
            List<ObatSediaanItem> completeList = new ArrayList<>();
            int offset = 0;
            int pageSize = 1000;
            boolean hasMoreData = true;

            try {
                while (hasMoreData) {
                    HttpUrl url = HttpUrl.parse(SUPABASE_BASE_URL + "/rest/v1/obat_sediaan")
                            .newBuilder()
                            .addQueryParameter("select", "id,name,manufacturer,category_main,category_sub,komposisi,farmakologi,indikasi,dosis,kontraindikasi,perhatian,efek_samping,interaksi_obat,kemasan")
                            .addQueryParameter("order", "name.asc")
                            .build();

                    int rangeStart = offset;
                    int rangeEnd = offset + pageSize - 1;

                    Request request = new Request.Builder()
                            .url(url)
                            .get()
                            .addHeader("apikey", SUPABASE_ANON_KEY)
                            .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                            .addHeader("Range", rangeStart + "-" + rangeEnd)
                            .build();

                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String jsonString = response.body().string();
                            Type listType = new TypeToken<List<ObatSediaanItem>>(){}.getType();
                            List<ObatSediaanItem> chunk = gson.fromJson(jsonString, listType);

                            if (chunk != null && !chunk.isEmpty()) {
                                completeList.addAll(chunk);
                                offset += chunk.size();
                                if (chunk.size() < pageSize) {
                                    hasMoreData = false;
                                }
                            } else {
                                hasMoreData = false;
                            }
                        } else {
                            hasMoreData = false;
                        }
                    }
                }

                if (!completeList.isEmpty()) {
                    allSediaanDrugs = completeList;
                    allDrugsLoaded = true;
                    applySearchAndFilters();
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching all drugs", e);
            }
        });
    }

    private static class CategoryWrapper {
        @SerializedName("category_main")
        String category;
    }
}