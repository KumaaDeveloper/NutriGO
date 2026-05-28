package com.example.pedulimakanan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.Toast;

public class HomeActivity extends Activity {

    private ImageButton navHome;
    private ImageButton navFavorite;
    private ImageButton navCart;
    private ImageButton navOrder;
    private ImageButton navProfile;

    private int userId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        View homeRoot = findViewById(R.id.homeRoot);
        homeRoot.requestFocus();
        hideKeyboard(homeRoot);

        userId = getIntent().getIntExtra("user_id", -1);

        navHome = findViewById(R.id.navHome);
        navFavorite = findViewById(R.id.navFavorite);
        navCart = findViewById(R.id.navCart);
        navOrder = findViewById(R.id.navOrder);
        navProfile = findViewById(R.id.navProfile);

        setActiveNav();

        navHome.setOnClickListener(v ->
                Toast.makeText(HomeActivity.this, "Kamu sudah di Home", Toast.LENGTH_SHORT).show()
        );

        navFavorite.setOnClickListener(v ->
                Toast.makeText(HomeActivity.this, "Favorit", Toast.LENGTH_SHORT).show()
        );

        navCart.setOnClickListener(v ->
                Toast.makeText(HomeActivity.this, "Keranjang", Toast.LENGTH_SHORT).show()
        );

        navOrder.setOnClickListener(v ->
                Toast.makeText(HomeActivity.this, "Pesanan", Toast.LENGTH_SHORT).show()
        );

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
            finish();
        });
    }

    private void setActiveNav() {
        navHome.setBackgroundResource(R.drawable.bg_nav_active);

        navFavorite.setBackgroundColor(Color.TRANSPARENT);
        navCart.setBackgroundColor(Color.TRANSPARENT);
        navOrder.setBackgroundColor(Color.TRANSPARENT);
        navProfile.setBackgroundColor(Color.TRANSPARENT);

        navHome.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        navFavorite.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        navCart.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        navOrder.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        navProfile.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}