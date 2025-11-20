package com.example.doirag.ui.rag;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ChatSpacingDecoration extends RecyclerView.ItemDecoration {
    private final int space;
    public ChatSpacingDecoration(int spacePx) { this.space = spacePx; }
    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        outRect.top = space / 2;
        outRect.bottom = space / 2;
    }
}
