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

            // === 1. DESTINATION LISTENER ===
            // Handle general visibility based on which page we are on
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.nav_drug_detail) {
                    // Always hide navbar on Drug Detail page
                    navView.setVisibility(View.GONE);
                } else {
                    // Show navbar on Home, Library, and initially on Chat
                    navView.setVisibility(View.VISIBLE);
                }
            });

            // === 2. KEYBOARD LISTENER ===
            // Handle visibility dynamic changes (like typing in Chat)
            View rootView = findViewById(R.id.container);
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    // Keyboard is OPEN
                    // Only hide navbar if we are on the Chat/Rag page
                    if (navController.getCurrentDestination() != null &&
                            navController.getCurrentDestination().getId() == R.id.nav_rag) {
                        navView.setVisibility(View.GONE);
                    }
                } else {
                    // Keyboard is CLOSED
                    // Restore navbar ONLY if we are NOT on the Drug Detail page
                    // (Because the Detail page must always have it hidden)
                    if (navController.getCurrentDestination() != null &&
                            navController.getCurrentDestination().getId() != R.id.nav_drug_detail) {
                        navView.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }
}