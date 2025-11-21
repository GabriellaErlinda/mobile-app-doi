package com.example.doirag.ui.library;

import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonElement; // Ubah ke JsonElement
import java.io.Serializable;
import java.util.Objects;

public class ObatGenerikItem implements Serializable {

    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String nama_generik;

    @SerializedName("nama_obat_dan_produsen")
    public String nama_obat_dan_produsen;

    @SerializedName("komposisi")
    public String komposisi;

    @SerializedName("farmakologi")
    public String farmakologi;

    @SerializedName("indikasi")
    public String indikasi;

    @SerializedName("dosis")
    public String dosis;

    @SerializedName("dosis_awal")
    public String dosis_awal;

    @SerializedName("dosis_pemeliharaan")
    public String dosis_pemeliharaan;

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

    // UBAH KE JsonElement: Ini adalah tipe universal (bisa Object, Array, Primitive, atau Null)
    // Ini akan mencegah crash "Expected JsonObject but was..."
    @SerializedName("extras")
    public JsonElement extras;

    public ObatGenerikItem() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObatGenerikItem that = (ObatGenerikItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}