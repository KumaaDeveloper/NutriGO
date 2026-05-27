package com.example.pedulimakanan;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

public class FavoriteActivity extends Activity {

    private DatabaseHelper db;
    private int userId;
    private LinearLayout llFavoriteContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        db = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        findViewById(R.id.btnBackFavorite).setOnClickListener(v -> finish());
        llFavoriteContainer = findViewById(R.id.llFavoriteContainer);

        // Muat data restoran yang difavoritkan
        loadFavorites();

        // ── INTEGRASI LOGIKA BOTTOM NAVBAR MANUAL ──
        LinearLayout layoutNavHome      = findViewById(R.id.layoutNavHome);
        LinearLayout layoutNavFavorit   = findViewById(R.id.layoutNavFavorit);
        LinearLayout layoutNavCart      = findViewById(R.id.layoutNavCart);
        LinearLayout layoutNavTransaksi = findViewById(R.id.layoutNavTransaksi);
        LinearLayout layoutNavProfile   = findViewById(R.id.layoutNavProfile);

        layoutNavHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Bersihkan stack agar kembali ke home utama
            startActivity(intent);
            finish();
        });

        layoutNavFavorit.setOnClickListener(v -> {
            // Sudah berada di halaman favorit, tidak perlu aksi tambahan
        });

        layoutNavCart.setOnClickListener(v ->
                Toast.makeText(this, "Keranjang (coming soon)", Toast.LENGTH_SHORT).show());

        layoutNavTransaksi.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransactionActivity.class);
            startActivity(intent);
        });

        layoutNavProfile.setOnClickListener(v ->
                Toast.makeText(this, "Profil (coming soon)", Toast.LENGTH_SHORT).show());
    }

    private void loadFavorites() {
        llFavoriteContainer.removeAllViews();
        if (userId == -1) return;

        // Mengambil data dari Query milik DatabaseHelper
        Cursor c = db.getFavoritByUser(userId);

        if (!c.moveToFirst()) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada restoran favorit.");
            empty.setPadding(32, 32, 32, 32);
            empty.setGravity(android.view.Gravity.CENTER);
            llFavoriteContainer.addView(empty);
            c.close();
            return;
        }

        do {
            int    restoId   = c.getInt(c.getColumnIndexOrThrow("id"));
            String namaResto = c.getString(c.getColumnIndexOrThrow("nama_resto"));
            String kategori  = c.getString(c.getColumnIndexOrThrow("kategori"));

            // Menggunakan layout item_restaurant_card milikmu agar visualnya seragam
            View card = LayoutInflater.from(this)
                    .inflate(R.layout.item_restaurant_card, llFavoriteContainer, false);

            TextView tvNama    = card.findViewById(R.id.tvNamaResto);
            TextView tvKat     = card.findViewById(R.id.tvKategori);
            ImageView imgResto = card.findViewById(R.id.imgResto);

            tvNama.setText(namaResto);
            tvKat.setText(kategori);
            imgResto.setImageResource(getRestoImage(namaResto));

            // Sembunyikan atau matikan fungsi klik tombol hati kecil di dalam halaman Favorit
            // agar user tidak bingung (atau biarkan jika ingin bisa langsung unfavorite dari sini)
            ImageView imgBtnFavorite = card.findViewById(R.id.imgBtnFavorite);
            if (imgBtnFavorite != null) {
                // Di halaman favorit, kita kunci agar tampil merah penuh secara konstan
                imgBtnFavorite.setImageResource(R.drawable.ic_favorite);
                imgBtnFavorite.setColorFilter(android.graphics.Color.parseColor("#FF4A4A"));

                // Opsional: jika ingin klik hati langsung menghapus item dari list favorit secara real-time:
                imgBtnFavorite.setOnClickListener(v -> {
                    db.toggleFavorit(userId, restoId);
                    Toast.makeText(this, namaResto + " dihapus dari favorit", Toast.LENGTH_SHORT).show();
                    loadFavorites(); // Refresh list secara instan
                });
            }

            // Jika kartu favorit diklik, langsung arahkan ke Detail Toko
            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, StoreDetailActivity.class);
                intent.putExtra("restoran_id", restoId);
                intent.putExtra("restoran_nama", namaResto);
                startActivity(intent);
            });

            llFavoriteContainer.addView(card);
        } while (c.moveToNext());
        c.close();
    }

    private int getRestoImage(String nama) {
        if (nama == null) return R.drawable.img_placeholder_food;
        switch (nama.toLowerCase().trim()) {
            case "saladstop":  return R.drawable.img_saladstop;
            case "burgreen":   return R.drawable.img_burgreen;
            case "supergrain": return R.drawable.img_supergrain;
            case "greenbowl":  return R.drawable.img_greenbowl;
            case "freshbox":   return R.drawable.img_freshbox;
            default:           return R.drawable.img_placeholder_food;
        }
    }
}