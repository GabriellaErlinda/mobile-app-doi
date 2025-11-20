package com.example.doirag;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navView = findViewById(R.id.nav_view);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(navView, navController);

            // === KEYBOARD DETECTION LOGIC ===
            // This listener detects layout changes (like the keyboard popping up)
            View rootView = findViewById(R.id.container);
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                Rect r = new Rect();
                // 1. Get the visible height of the app window
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();

                // 2. Calculate the difference (height - visible part)
                // If the difference is big (> 15% of screen), the keyboard is likely OPEN
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    // Keyboard is OPEN
                    // Check if we are on the Chat/Rag page
                    if (navController.getCurrentDestination() != null &&
                            navController.getCurrentDestination().getId() == R.id.nav_rag) {
                        // Hide the navbar so the input field sits on top of the keyboard
                        navView.setVisibility(View.GONE);
                    }
                } else {
                    // Keyboard is CLOSED
                    // Bring the navbar back
                    navView.setVisibility(View.VISIBLE);
                }
            });
        }
    }
}