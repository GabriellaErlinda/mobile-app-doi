package com.example.doirag.ui.library;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class FastScroller extends View {

    private final String[] sections = new String[]{"#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    private Paint paint;
    private int width, height, sectionHeight;
    private FastScrollListener listener;

    public interface FastScrollListener {
        void onSectionClicked(String section);
    }

    public void setListener(FastScrollListener listener) {
        this.listener = listener;
    }

    public FastScroller(Context context) {
        super(context);
        init();
    }

    public FastScroller(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.parseColor("#64748B")); // Slate 500 color
        paint.setTextSize(30f);
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
        if (sections.length > 0) {
            sectionHeight = height / sections.length;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (sectionHeight == 0) return;

        for (int i = 0; i < sections.length; i++) {
            float xPos = width / 2f;
            // Center text vertically in the section
            float yPos = (sectionHeight * i) + (sectionHeight / 1.5f);
            canvas.drawText(sections[i], xPos, yPos, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            if (sectionHeight == 0) return false;

            float y = event.getY();
            int currentSectionIndex = (int) (y / sectionHeight);

            if (currentSectionIndex >= 0 && currentSectionIndex < sections.length) {
                String section = sections[currentSectionIndex];
                if (listener != null) {
                    listener.onSectionClicked(section);
                }
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
}