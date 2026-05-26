package com.example.pedulimakanan;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity {

    // Sesuaikan IP dan file PHP milikmu
    private static final String CONNECTOR_URL = "http://172.104.183.200/pedulimakanan/connector.php?action=get_populer_data";

    private LinearLayout containerPopuler;
    private TextView tvSaldo;
    private EditText etSearch;

    // Model data yang menampung String gambarUrl dari database
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
        setContentView(R.layout.activity_home);

        containerPopuler = findViewById(R.id.containerPopuler);
        tvSaldo = findViewById(R.id.tvSaldo);
        etSearch = findViewById(R.id.etSearch);

        // Saldo default mockup
        tvSaldo.setText("Rp. 10.000");

        // Cari baris ini di HomeActivity.java lama kamu, lalu timpa dengan ini:
        findViewById(R.id.navFavorite).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, FavoriteActivity.class));
            overridePendingTransition(0, 0); // Menghilangkan animasi kedip saat pindah menu
        });

        findViewById(R.id.navCart).setOnClickListener(v ->
                Toast.makeText(this, "Keranjang Belanja", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.navOrders).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, TransactionActivity.class));
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
            overridePendingTransition(0, 0);
        });

        TextView tvJudulPopuler = findViewById(R.id.tvLabelPopuler); // Pastikan ID ini ditambahkan di XML judul populer kamu
        if (tvJudulPopuler != null) {
            tvJudulPopuler.setOnClickListener(v -> {
                Intent keKategori = new Intent(HomeActivity.this, CategoryActivity.class);
                keKategori.putExtra("KATEGORI_MENU", "Makanan Sehat Populer");
                startActivity(keKategori);
            });
        }

        TextView tvJudulMurah = findViewById(R.id.tvLabelMurah); // Pastikan ID ini ditambahkan di XML judul murah kamu
        if (tvJudulMurah != null) {
            tvJudulMurah.setOnClickListener(v -> {
                Intent keKategori = new Intent(HomeActivity.this, CategoryActivity.class);
                keKategori.putExtra("KATEGORI_MENU", "Makanan Sehat Murah");
                startActivity(keKategori);
            });
        }

        // Jalankan sinkronisasi data dari Database
        new FetchPopulerDataTask().execute();
    }

    // AsyncTask untuk mengambil data dari backend PHP
    private class FetchPopulerDataTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(CONNECTOR_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                conn.disconnect();
                return response.toString();

            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String jsonResult) {
            if (jsonResult == null) {
                // Jika koneksi server offline/gagal, load data lokal agar tidak kosong
                populateLocalFallback();
                return;
            }

            try {
                JSONObject jsonObject = new JSONObject(jsonResult);
                JSONArray populerArray = jsonObject.getJSONArray("makanan_populer");

                containerPopuler.removeAllViews();

                for (int i = 0; i < populerArray.length(); i++) {
                    JSONObject data = populerArray.getJSONObject(i);

                    // Ambil field dari database sesuai struktur tabel restoran
                    String nama = data.getString("nama_resto");
                    double rating = data.getDouble("rating");
                    String gambarUrl = data.getString("gambar_url"); // Berisi teks misal: "saladstop"

                    KelompokResto resto = new KelompokResto(nama, rating, gambarUrl);
                    tampilkanCardKeUI(resto);
                }

            } catch (Exception e) {
                populateLocalFallback();
            }
        }
    }

    private void tampilkanCardKeUI(KelompokResto resto) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.item_restaurant, containerPopuler, false);

        TextView tvName = cardView.findViewById(R.id.tvRestaurantName);
        TextView tvRating = cardView.findViewById(R.id.tvRestaurantRating);
        ImageView imgResto = cardView.findViewById(R.id.imgRestaurant);

        tvName.setText(resto.namaResto);
        tvRating.setText(String.valueOf(resto.rating));

        // Mencari file gambar di drawable secara dinamis berdasarkan String database
        int resId = 0;
        if (resto.gambarUrl != null && !resto.gambarUrl.isEmpty()) {
            resId = getResources().getIdentifier(
                    resto.gambarUrl,
                    "drawable",
                    getPackageName()
            );
        }

        if (resId != 0) {
            imgResto.setImageResource(resId);
        } else {
            imgResto.setImageResource(R.drawable.logo); // Gambar cadangan jika file tidak ditemukan
        }

        containerPopuler.addView(cardView);
    }

    // Fungsi cadangan jika server mati saat kamu presentasi/coding offline
    private void populateLocalFallback() {
        containerPopuler.removeAllViews();

        // Kita panggil dua kali secara manual untuk tes layout menyamping
        tampilkanCardKeUI(new KelompokResto("Saladstop", 4.8, "saladstop"));
        tampilkanCardKeUI(new KelompokResto("Supergrain", 4.3, "supergrain"));
    }
}