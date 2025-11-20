package com.example.doirag.ui.library;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class ObatSediaanItem {

    @SerializedName("id")
    public String id;

    @SerializedName("name") // Ganti "drug_name" di Supabase Anda menjadi "name" agar cocok
    public String drug_name; // atau ubah @SerializedName("drug_name")

    @SerializedName("manufacturer")
    public String manufacturer;

    @SerializedName("category_main")
    public String category_main;

    @SerializedName("category_sub")
    public String category_sub;

    @SerializedName("komposisi")
    public String komposisi;

    @SerializedName("farmakologi")
    public String farmakologi;

    @SerializedName("indikasi")
    public String indikasi;

    @SerializedName("dosis")
    public String dosis;

    @SerializedName("kontraindikasi")
    public String kontraindikasi;

    @SerializedName("perhatian")
    public String perhatian;

    @SerializedName("efek_samping")
    public String efek_samping;

    @SerializedName("interaksi_obat")
    public String interaksi_obat;

    @SerializedName("kemasan")
    public String kemasan;


    // Constructor default diperlukan GSON
    public ObatSediaanItem() {
    }

    // Override equals() dan hashCode() agar DiffUtil bekerja lebih baik
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObatSediaanItem that = (ObatSediaanItem) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(drug_name, that.drug_name) &&
                Objects.equals(manufacturer, that.manufacturer) &&
                Objects.equals(category_main, that.category_main);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, drug_name, manufacturer, category_main);
    }
}