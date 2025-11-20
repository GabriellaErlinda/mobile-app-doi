package com.example.doirag.ui.library;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class ObatGenerikItem {

    @SerializedName("id")
    public String id;

    @SerializedName("nama_generik")
    public String nama_generik;

    @SerializedName("deskripsi_singkat")
    public String deskripsi_singkat;

    @SerializedName("indikasi")
    public String indikasi;

    @SerializedName("kontraindikasi")
    public String kontraindikasi;

    @SerializedName("efek_samping")
    public String efek_samping;

    @SerializedName("dosis")
    public String dosis;

    @SerializedName("perhatian")
    public String perhatian;

    public ObatGenerikItem() {
        // Constructor default
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObatGenerikItem that = (ObatGenerikItem) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(nama_generik, that.nama_generik);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nama_generik);
    }
}