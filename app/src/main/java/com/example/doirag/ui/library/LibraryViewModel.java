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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

    public LibraryViewModel(@NonNull Application application) {
        super(application);
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();

        fetchData();
        fetchCategories();
    }

    // --- FUNGSI KONTROL ---

    public void setCurrentTab(int position) {
        if (!Objects.equals(currentTab.getValue(), position)) {
            currentTab.setValue(position);
            // Re-fetch or Re-process is handled implicitly if needed,
            // but usually lists are independent.
        }
    }

    public void setSearchQuery(String query) {
        if (!Objects.equals(query, searchQuery.getValue())) {
            searchQuery.setValue(query);
            fetchData();
        }
    }

    // NEW: Apply Sort
    public void setSortOrder(boolean ascending) {
        this.isSortAscending = ascending;
        processSediaanList();
        processGenerikList();
    }

    // NEW: Apply Filter
    public void setCategoryFilter(String category) {
        this.activeCategoryFilter = category;
        processSediaanList();
        // Generik usually doesn't have the same category structure, so we might only filter Sediaan
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

    // --- INTERNAL PROCESSING (Sort & Filter) ---

    private void processSediaanList() {
        if (masterSediaanList == null) return;

        List<ObatSediaanItem> processed = new ArrayList<>();

        // 1. Filter by Category
        if (activeCategoryFilter != null && !activeCategoryFilter.isEmpty()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                processed = masterSediaanList.stream()
                        .filter(item -> item.category_main != null &&
                                item.category_main.equalsIgnoreCase(activeCategoryFilter))
                        .collect(Collectors.toList());
            } else {
                // Fallback for older Android
                for (ObatSediaanItem item : masterSediaanList) {
                    if (item.category_main != null &&
                            item.category_main.equalsIgnoreCase(activeCategoryFilter)) {
                        processed.add(item);
                    }
                }
            }
        } else {
            processed.addAll(masterSediaanList);
        }

        // 2. Sort (Alphabetical)
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

    private void fetchData() {
        String query = searchQuery.getValue();
        // Always fetch both or manage based on tab.
        // For simplicity, we fetch relevant data.

        // Reset master lists slightly to avoid stale data confusion during loading if needed,
        // but here we keep them to prevent flickering.

        fetchSediaanDrugs(query);
        fetchGenerikDrugsPlaceholder(query);
    }

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

    private void fetchSediaanDrugs(String query) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpUrl url = HttpUrl.parse(SUPABASE_BASE_URL + "/rest/v1/rpc/search_obat_sediaan")
                        .newBuilder()
                        .build();

                Map<String, String> bodyMap = Collections.singletonMap("search_term", query);
                String jsonBody = gson.toJson(bodyMap);
                RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

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

                        // Update Master List
                        masterSediaanList = (hasil != null) ? hasil : new ArrayList<>();

                        // Apply local filters/sort
                        processSediaanList();
                    } else {
                        masterSediaanList = new ArrayList<>();
                        processSediaanList();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error Sediaan: " + e.getMessage());
                masterSediaanList = new ArrayList<>();
                processSediaanList();
            }
        });
    }

    private void fetchGenerikDrugsPlaceholder(String query) {
        // Placeholder logic
        masterGenerikList = new ArrayList<>();
        processGenerikList();
    }

    private static class CategoryWrapper {
        @SerializedName("category_main")
        String category;
    }
}