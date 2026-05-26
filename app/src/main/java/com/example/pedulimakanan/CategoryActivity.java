package com.example.pedulimakanan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class CategoryActivity extends Activity {

    private LinearLayout containerVerticalCategory;
    private TextView tvCategoryTitle;
    private String namaKategoriAktif = "";

    private static class KelompokResto {
        String namaResto;
        double rating;
        String gambarUrl;

        KelompokResto(String namaResto, double rating, String gambarUrl) {
            this.namaResto = namaResto;
            this.rating = rating;
            this.gambarUrl = gambarUrl;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        containerVerticalCategory = findViewById(R.id.containerVerticalCategory);
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);

        // Membaca jenis kategori yang dilempar dari Home Screen
        Intent intentData = getIntent();
        if (intentData != null && intentData.hasExtra("KATEGORI_MENU")) {
            namaKategoriAktif = intentData.getStringExtra("KATEGORI_MENU");
            tvCategoryTitle.setText(namaKategoriAktif);
        }

        // Tombol Back Universal
        ImageButton btnBack = findViewById(R.id.btnBackToHome);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            });
        }

        setupNavbar();
        loadRestoranBerdasarkanKategori();
    }

    private void loadRestoranBerdasarkanKategori() {
        containerVerticalCategory.removeAllViews();
        List<KelompokResto> listResto = new ArrayList<>();

        // Logika pengelompokan data tiruan / database lokal berdasarkan judul kategori
        if (namaKategoriAktif.equalsIgnoreCase("Makanan Sehat Populer")) {
            listResto.add(new KelompokResto("Saladstop", 4.8, "saladstop"));
            listResto.add(new KelompokResto("Burgreens Fresh", 4.7, "saladstop")); // Menggunakan fallback aset gambar yang ada
        } else {
            // Jika kategori yang diklik adalah Makanan Sehat Murah
            listResto.add(new KelompokResto("Supergrain", 4.3, "supergrain"));
            listResto.add(new KelompokResto("Berrywell Healthy", 4.5, "saladstop"));
        }

        for (KelompokResto resto : listResto) {
            tampilkanRestoKeUI(resto);
        }
    }

    private void tampilkanRestoKeUI(KelompokResto resto) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.item_restaurant_vertical, containerVerticalCategory, false);

        TextView tvName = cardView.findViewById(R.id.tvRestaurantNameVertical);
        TextView tvRating = cardView.findViewById(R.id.tvRestaurantRatingVertical);
        ImageView imgResto = cardView.findViewById(R.id.imgRestaurantVertical);

        tvName.setText(resto.namaResto);
        tvRating.setText(String.valueOf(resto.rating));

        int resId = 0;
        if (resto.gambarUrl != null && !resto.gambarUrl.isEmpty()) {
            resId = getResources().getIdentifier(resto.gambarUrl, "drawable", getPackageName());
        }

        if (resId != 0) {
            imgResto.setImageResource(resId);
        } else {
            imgResto.setImageResource(R.drawable.logo);
        }

        containerVerticalCategory.addView(cardView);
    }

    private void setupNavbar() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
        findViewById(R.id.navFavorite).setOnClickListener(v -> {
            startActivity(new Intent(this, FavoriteActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }
}