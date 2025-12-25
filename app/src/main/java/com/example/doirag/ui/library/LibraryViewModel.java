package com.example.doirag.ui.library;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doirag.BuildConfig;
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
    private final String FLASK_BASE_URL = "http://10.169.3.97:5000";

    // --- Data Storage ---
    // List ini buat simpen data master biar gak perlu bolak-balik fetch ke server
    private List<ObatGenerikItem> masterGenerikList = new ArrayList<>();
    private List<ObatSediaanItem> allSediaanDrugs = new ArrayList<>();

    // --- State (Input) ---
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Integer> currentTab = new MutableLiveData<>(0);
    private String activeCategoryFilter = null;
    private boolean isSortAscending = true;

    // --- LiveData (Output) ---
    private final MutableLiveData<List<ObatGenerikItem>> filteredGenerikDrugs = new MutableLiveData<>();
    private final MutableLiveData<List<ObatSediaanItem>> filteredSediaanDrugs = new MutableLiveData<>();
    private final MutableLiveData<List<String>> categoryList = new MutableLiveData<>();

    // _isLoading ini buat kontrol loading state di UI biar user tau ada proses
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);

    private static final String TAG = "LibraryViewModel";
    private boolean allSediaanLoaded = false;
    private boolean allGenerikLoaded = false;

    public LibraryViewModel(@NonNull Application application) {
        super(application);
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();

        // Fers, kita load data awal pas ViewModel ini dibikin biar gak kosong melompong
        fetchSediaanData(false);
        fetchGenerikData(false);
        fetchCategories();
    }

    // --- UI CONTROLS ---

    // Function buat dipantau Fragment biar animasinya jalan pas loading
    public LiveData<Boolean> isLoading() { return _isLoading; }

    public void refreshData() {
        // Set loading jadi true biar animasinya muncul
        _isLoading.setValue(true);

        // Force refresh dengan bypass check 'Loaded' biar data beneran di-update
        CompletableFuture<Void> fetchGenerik = fetchGenerikData(true);
        CompletableFuture<Void> fetchSediaan = fetchSediaanData(true);

        // Pas kedua proses beres, baru matiin loading state-nya
        CompletableFuture.allOf(fetchGenerik, fetchSediaan)
                .thenRun(() -> _isLoading.postValue(false));
    }

    public void setCurrentTab(int position) {
        if (currentTab.getValue() == null || currentTab.getValue() != position) {
            currentTab.setValue(position);
            // Tiap pindah tab, logic search harus ke trigger ulang biar datanya sinkron
            applySearchLogic();
        }
    }

    public void setSearchQuery(String query) {
        if (searchQuery.getValue() == null || !searchQuery.getValue().equals(query)) {
            searchQuery.setValue(query);
            // Search logic ke trigger tiap kali user ngetik sesuatu di EditText
            applySearchLogic();
        }
    }

    public void setSortOrder(boolean ascending) {
        this.isSortAscending = ascending;
        // Sorting lokal biar cepet tanpa hit API lagi
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

    // --- CORE LOGIC ---

    private void applySearchLogic() {
        String query = searchQuery.getValue();
        int tab = currentTab.getValue() != null ? currentTab.getValue() : 0;

        // Pas query kosong (user hapus ketikan), balikin ke data awal yang udah ada di RAM
        if (query == null || query.trim().isEmpty()) {
            if (tab == 0) filteredGenerikDrugs.postValue(new ArrayList<>(masterGenerikList));
            else applyLocalFilterForSediaan(allSediaanDrugs);
            return;
        }

        // Kalo ada query-nya, panggil backend Flask buat dapetin hasil search
        searchObatFromApi(query);
    }

    private void searchObatFromApi(String query) {
        CompletableFuture.runAsync(() -> {
            try {
                int tab = currentTab.getValue() != null ? currentTab.getValue() : 0;
                String type = (tab == 0) ? "generik" : "sediaan";

                // Setup URL dengan query parameter q (query) dan k (limit 20 hasil)
                HttpUrl url = HttpUrl.parse(FLASK_BASE_URL + "/search-obat")
                        .newBuilder()
                        .addQueryParameter("q", query)
                        .addQueryParameter("type", type)
                        .addQueryParameter("k", "20")
                        .build();

                Request request = new Request.Builder().url(url).get().build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonResponse = response.body().string();
                        SearchResponseWrapper wrapper = gson.fromJson(jsonResponse, SearchResponseWrapper.class);
                        if (wrapper == null || wrapper.results == null) return;

                        // Parse datanya sesuai type yang lagi aktif (Generik vs Sediaan)
                        if (type.equals("generik")) {
                            List<ObatGenerikItem> results = new ArrayList<>();
                            for (SearchResultItem item : wrapper.results) {
                                ObatGenerikItem obat = gson.fromJson(item.attrs, ObatGenerikItem.class);
                                if (obat != null) results.add(obat);
                            }
                            filteredGenerikDrugs.postValue(results);
                        } else {
                            List<ObatSediaanItem> results = new ArrayList<>();
                            for (SearchResultItem item : wrapper.results) {
                                ObatSediaanItem obat = gson.fromJson(item.attrs, ObatSediaanItem.class);
                                if (obat != null) results.add(obat);
                            }
                            // Setelah dapet hasil search, tetep filter kategori lokal biar match
                            applyLocalFilterForSediaan(results);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error searching API", e);
            }
        });
    }

    private void applyLocalFilterForSediaan(List<ObatSediaanItem> sourceList) {
        List<ObatSediaanItem> processed = new ArrayList<>(sourceList);

        // Filter data berdasarkan kategori kalo kategorinya ada yang dipilih
        if (activeCategoryFilter != null && !activeCategoryFilter.isEmpty()) {
            List<ObatSediaanItem> temp = new ArrayList<>();
            for (ObatSediaanItem item : processed) {
                if (item.category_main != null && item.category_main.equalsIgnoreCase(activeCategoryFilter)) {
                    temp.add(item);
                }
            }
            processed = temp;
        }

        // Terakhir, sort hasilnya A-Z atau Z-A
        Collections.sort(processed, (o1, o2) -> {
            String s1 = o1.drug_name != null ? o1.drug_name : "";
            String s2 = o2.drug_name != null ? o2.drug_name : "";
            return isSortAscending ? s1.compareToIgnoreCase(s2) : s2.compareToIgnoreCase(s1);
        });

        filteredSediaanDrugs.postValue(processed);
    }

    private void sortCurrentResults() {
        // Logic ini ke trigger pas user mencet tombol sort doang tanpa ngetik apa-apa
        if (currentTab.getValue() == 0) {
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
        } else {
            List<ObatSediaanItem> current = filteredSediaanDrugs.getValue();
            if(current != null) applyLocalFilterForSediaan(current);
        }
    }

    // --- REFACTORED DATA FETCHING ---

    private CompletableFuture<Void> fetchSediaanData(boolean forceRefresh) {
        // Cek dulu datanya udah ada apa belum, kalo udah ada ngapain fetch lagi (kecuali force refresh)
        if (allSediaanLoaded && !forceRefresh) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            List<ObatSediaanItem> completeList = new ArrayList<>();
            int offset = 0; int pageSize = 1000; boolean hasMoreData = true;
            try {
                // Fetch bertahap per 1000 item biar Supabase-nya gak overload
                while (hasMoreData) {
                    HttpUrl url = HttpUrl.parse(SUPABASE_BASE_URL + "/rest/v1/obat_sediaan")
                            .newBuilder().addQueryParameter("select", "id,name,manufacturer,category_main,category_sub,komposisi,farmakologi,indikasi,dosis,kontraindikasi,perhatian,efek_samping,interaksi_obat,kemasan")
                            .addQueryParameter("order", "name.asc").build();
                    Request request = new Request.Builder().url(url).get()
                            .addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                            .addHeader("Range", offset + "-" + (offset + pageSize - 1)).build();
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<ObatSediaanItem> chunk = gson.fromJson(response.body().string(), new TypeToken<List<ObatSediaanItem>>(){}.getType());
                            if (chunk != null && !chunk.isEmpty()) {
                                completeList.addAll(chunk);
                                offset += chunk.size();
                                if (chunk.size() < pageSize) hasMoreData = false;
                            } else hasMoreData = false;
                        } else hasMoreData = false;
                    }
                }
                if (!completeList.isEmpty()) {
                    allSediaanDrugs = completeList;
                    allSediaanLoaded = true;
                    if (searchQuery.getValue() == null || searchQuery.getValue().isEmpty()) {
                        applyLocalFilterForSediaan(allSediaanDrugs);
                    }
                }
            } catch (Exception e) { Log.e(TAG, "Error fetching sediaan", e); }
        });
    }

    private CompletableFuture<Void> fetchGenerikData(boolean forceRefresh) {
        // Sama kayak sediaan, cek dulu datanya ada apa belum
        if (allGenerikLoaded && !forceRefresh) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            List<ObatGenerikItem> completeList = new ArrayList<>();
            int offset = 0; int pageSize = 1000; boolean hasMoreData = true;
            try {
                while (hasMoreData) {
                    HttpUrl url = HttpUrl.parse(SUPABASE_BASE_URL + "/rest/v1/obat_generik")
                            .newBuilder().addQueryParameter("select", "*").addQueryParameter("order", "name.asc").build();
                    Request request = new Request.Builder().url(url).get()
                            .addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                            .addHeader("Range", offset + "-" + (offset + pageSize - 1)).build();
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<ObatGenerikItem> chunk = gson.fromJson(response.body().string(), new TypeToken<List<ObatGenerikItem>>(){}.getType());
                            if (chunk != null && !chunk.isEmpty()) {
                                completeList.addAll(chunk);
                                offset += chunk.size();
                                if (chunk.size() < pageSize) hasMoreData = false;
                            } else hasMoreData = false;
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

    public void fetchCategories() {
        // Ambil daftar kategori yang ada di DB biar bisa dipake filter
        CompletableFuture.runAsync(() -> {
            try {
                HttpUrl url = HttpUrl.parse(SUPABASE_BASE_URL + "/rest/v1/distinct_categories")
                        .newBuilder().addQueryParameter("select", "category_main").build();
                Request request = new Request.Builder().url(url).get()
                        .addHeader("apikey", SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY).build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonString = response.body().string();
                        List<CategoryWrapper> rawList = gson.fromJson(jsonString, new TypeToken<List<CategoryWrapper>>(){}.getType());
                        List<String> strings = new ArrayList<>();
                        if (rawList != null) for (CategoryWrapper item : rawList) if (item.category != null) strings.add(item.category);
                        // Sortir kategorinya berdasarkan angka fers, sisanya A-Z etc
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

    private int extractNumber(String text) {
        // Helper buat nyari angka di awal string kategori (misal "1. Sistem Saluran Pernafasan")
        try {
            Matcher matcher = Pattern.compile("^(\\d+)").matcher(text);
            if (matcher.find()) return Integer.parseInt(matcher.group(1));
            return -1;
        } catch (Exception e) { return -1; }
    }

    private static class SearchResponseWrapper {
        @SerializedName("results") List<SearchResultItem> results;
    }

    private static class SearchResultItem {
        @SerializedName("attrs") JsonElement attrs;
    }

    private static class CategoryWrapper {
        @SerializedName("category_main") String category;
    }
}