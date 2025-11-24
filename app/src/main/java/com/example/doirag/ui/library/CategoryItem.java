package com.example.doirag.ui.library;

public class CategoryItem {
    String displayName; // Nama pendek untuk di Card (misal: "PERNAFASAN")
    String dbValue;     // Nama persis di Database (misal: "1. Sistem Saluran Pernafasan")
    int iconResId;      // Ikon

    public CategoryItem(String displayName, String dbValue, int iconResId) {
        this.displayName = displayName;
        this.dbValue = dbValue;
        this.iconResId = iconResId;
    }
}