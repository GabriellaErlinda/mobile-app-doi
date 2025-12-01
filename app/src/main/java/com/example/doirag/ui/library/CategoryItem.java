package com.example.doirag.ui.library;

public class CategoryItem {
    String displayName; // Nama pendek untuk di Card (ex: "PERNAFASAN")
    String dbValue;     // Nama persis di Database (ex: "1. Sistem Saluran Pernafasan")
    int iconResId;

    public CategoryItem(String displayName, String dbValue, int iconResId) {
        this.displayName = displayName;
        this.dbValue = dbValue;
        this.iconResId = iconResId;
    }
}