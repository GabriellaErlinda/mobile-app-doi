package com.example.doirag.ui.library;

public class CategoryItem {
    public String displayName; // Nama pendek untuk di Card (ex: "PERNAFASAN")
    public String dbValue;     // Nama persis di Database (ex: "1. Sistem Saluran Pernafasan")
    public int iconResId;

    public CategoryItem(String displayName, String dbValue, int iconResId) {
        this.displayName = displayName;
        this.dbValue = dbValue;
        this.iconResId = iconResId;
    }
}