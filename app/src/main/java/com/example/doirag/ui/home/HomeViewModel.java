package com.example.doirag.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText = new MutableLiveData<>();

    public HomeViewModel() {
        mText.setValue("Selamat datang");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
