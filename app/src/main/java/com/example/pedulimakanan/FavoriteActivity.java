package com.example.pedulimakanan;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class FavoriteActivity extends Activity {

    private static final String PREF_NAME = "login_session";

    private DatabaseHelper db;
    private int userId;
    private LinearLayout llFavoriteContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        db = new DatabaseHelper(this);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        findViewById(R.id.btnBackFavorite).setOnClickListener(v -> finish());

        llFavoriteContainer = findViewById(R.id.llFavoriteContainer);

        setupBottomNav();
        loadFavorites();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void setupBottomNav() {
        LinearLayout layoutNavHome      = findViewById(R.id.layoutNavHome);
        LinearLayout layoutNavFavorit   = findViewById(R.id.layoutNavFavorit);
        LinearLayout layoutNavCart      = findViewById(R.id.layoutNavCart);
        LinearLayout layoutNavTransaksi = findViewById(R.id.layoutNavTransaksi);
        LinearLayout layoutNavProfile   = findViewById(R.id.layoutNavProfile);

        layoutNavHome.setOnClickListener(v -> {
            Intent intent = new Intent(FavoriteActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        layoutNavFavorit.setOnClickListener(v -> {
            // Sudah berada di halaman favorit
        });

        layoutNavCart.setOnClickListener(v -> {
            Intent intent = new Intent(FavoriteActivity.this, CartActivity.class);
            startActivity(intent);
            finish();
        });

        layoutNavTransaksi.setOnClickListener(v -> {
            Intent intent = new Intent(FavoriteActivity.this, TransactionActivity.class);
            startActivity(intent);
            finish();
        });

        layoutNavProfile.setOnClickListener(v -> {
            Intent intent = new Intent(FavoriteActivity.this, ProfileActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
            finish();
        });
    }

    private void loadFavorites() {
        llFavoriteContainer.removeAllViews();

        if (userId == -1) {
            showEmptyFavoriteBox();
            return;
        }

        Cursor c = db.getFavoritByUser(userId);

        if (c == null || !c.moveToFirst()) {
            showEmptyFavoriteBox();

            if (c != null) {
                c.close();
            }

            return;
        }

        LinearLayout currentRow = null;
        int itemCount = 0;

        do {
            int restoId = c.getInt(c.getColumnIndexOrThrow("id"));
            String namaResto = c.getString(c.getColumnIndexOrThrow("nama_resto"));
            String kategori = c.getString(c.getColumnIndexOrThrow("kategori"));

            if (itemCount % 2 == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);

                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

                rowParams.setMargins(0, 0, 0, dpToPx(12));
                currentRow.setLayoutParams(rowParams);

                llFavoriteContainer.addView(currentRow);
            }

            View card = LayoutInflater.from(this)
                    .inflate(R.layout.item_restaurant_card, currentRow, false);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
            );

            if (itemCount % 2 == 0) {
                cardParams.setMargins(0, 0, dpToPx(6), 0);
            } else {
                cardParams.setMargins(dpToPx(6), 0, 0, 0);
            }

            card.setLayoutParams(cardParams);

            TextView tvNama = card.findViewById(R.id.tvNamaResto);
            TextView tvKat = card.findViewById(R.id.tvKategori);
            ImageView imgResto = card.findViewById(R.id.imgResto);

            tvNama.setText(namaResto);
            tvKat.setText(kategori);
            imgResto.setImageResource(getRestoImage(namaResto));

            ImageView imgBtnFavorite = card.findViewById(R.id.imgBtnFavorite);

            if (imgBtnFavorite != null) {
                imgBtnFavorite.setImageResource(R.drawable.ic_favorite);
                imgBtnFavorite.setColorFilter(android.graphics.Color.parseColor("#FF4A4A"));

                imgBtnFavorite.setOnClickListener(v -> {
                    db.toggleFavorit(userId, restoId);

                    Toast.makeText(
                            FavoriteActivity.this,
                            namaResto + " dihapus dari favorit",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadFavorites();
                });
            }

            card.setOnClickListener(v -> {
                Intent intent = new Intent(FavoriteActivity.this, StoreDetailActivity.class);
                intent.putExtra("restoran_id", restoId);
                intent.putExtra("restoran_nama", namaResto);
                startActivity(intent);
            });

            if (currentRow != null) {
                currentRow.addView(card);
            }

            itemCount++;

        } while (c.moveToNext());

        c.close();

        if (itemCount % 2 != 0) {
            View emptySpace = new View(this);

            LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
            );

            emptyParams.setMargins(dpToPx(6), 0, 0, 0);
            emptySpace.setLayoutParams(emptyParams);

            LinearLayout lastRow = (LinearLayout) llFavoriteContainer.getChildAt(
                    llFavoriteContainer.getChildCount() - 1
            );

            lastRow.addView(emptySpace);
        }
    }

    private void showEmptyFavoriteBox() {
        View emptyView = LayoutInflater.from(this)
                .inflate(R.layout.item_empty_favorite, llFavoriteContainer, false);

        Button btnCariMakanan = emptyView.findViewById(R.id.btnCariMakanan);

        btnCariMakanan.setOnClickListener(v -> {
            Intent intent = new Intent(FavoriteActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        llFavoriteContainer.addView(emptyView);
    }

    private int getRestoImage(String nama) {
        if (nama == null) {
            return R.drawable.img_placeholder_food;
        }

        switch (nama.toLowerCase().trim()) {
            case "saladstop":
                return R.drawable.img_saladstop;

            case "burgreen":
                return R.drawable.img_burgreen;

            case "supergrain":
                return R.drawable.img_supergrain;

            case "greenbowl":
                return R.drawable.img_greenbowl;

            case "freshbox":
                return R.drawable.img_freshbox;

            default:
                return R.drawable.img_placeholder_food;
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}