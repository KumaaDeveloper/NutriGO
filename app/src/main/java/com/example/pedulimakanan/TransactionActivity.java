package com.example.pedulimakanan;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

public class TransactionActivity extends Activity {

    private DatabaseHelper db;
    private int userId;
    private LinearLayout llTransaksiContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        db = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        llTransaksiContainer = findViewById(R.id.llTransaksiContainer);

        View btnBack = findViewById(R.id.btnBackTransaction);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupBottomNavigation();
        loadTransactions();
    }

    private void setupBottomNavigation() {
        findViewById(R.id.layoutNavHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.layoutNavFavorit).setOnClickListener(v ->
                startActivity(new Intent(this, FavoriteActivity.class)));

        findViewById(R.id.layoutNavCart).setOnClickListener(v ->
                startActivity(new Intent(this, CartActivity.class)));

        findViewById(R.id.layoutNavProfile).setOnClickListener(v ->
                Toast.makeText(this, "Profil (coming soon)", Toast.LENGTH_SHORT).show());
    }

    private void loadTransactions() {
        llTransaksiContainer.removeAllViews();
        if (userId == -1) return;

        Cursor c = db.getTransaksiByUser(userId);
        try {
            if (c != null && c.moveToFirst()) {
                do {
                    int transaksiId = c.getInt(c.getColumnIndexOrThrow("id"));
                    String namaResto = c.getString(c.getColumnIndexOrThrow("nama_resto"));
                    int total = c.getInt(c.getColumnIndexOrThrow("total_harga"));
                    String tanggal = c.getString(c.getColumnIndexOrThrow("tanggal"));
                    String status = c.getString(c.getColumnIndexOrThrow("status_pesanan"));

                    View row = LayoutInflater.from(this).inflate(R.layout.item_transaction_row, llTransaksiContainer, false);

                    ((TextView) row.findViewById(R.id.tvTransNamaResto)).setText(namaResto);
                    ((TextView) row.findViewById(R.id.tvTransTotal)).setText("Rp " + formatRupiah(total));
                    ((TextView) row.findViewById(R.id.tvTransTanggal)).setText(formatTanggal(tanggal));
                    ((TextView) row.findViewById(R.id.tvTransStatus)).setText(status);
                    ((ImageView) row.findViewById(R.id.imgTransResto)).setImageResource(getRestoImage(namaResto));

                    row.findViewById(R.id.btnTransDetail).setOnClickListener(v -> {
                        Intent intent = new Intent(this, TransactionDetailActivity.class);
                        intent.putExtra("transaksi_id", transaksiId);
                        intent.putExtra("restoran_nama", namaResto);
                        intent.putExtra("total_harga", total);
                        intent.putExtra("tanggal", tanggal);
                        startActivity(intent);
                    });

                    llTransaksiContainer.addView(row);
                } while (c.moveToNext());
            } else {
                TextView empty = new TextView(this);
                empty.setText("Belum ada transaksi.");
                empty.setPadding(32, 32, 32, 32);
                llTransaksiContainer.addView(empty);
            }
        } finally {
            if (c != null) c.close();
        }
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

    private String formatRupiah(int amount) {
        return String.format("%,d", amount).replace(',', '.');
    }

    private String formatTanggal(String raw) {
        try {
            String[] parts = raw.split(" ")[0].split("-");
            String[] bulan = {"","Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des"};
            return parts[2] + " " + bulan[Integer.parseInt(parts[1])] + " " + parts[0];
        } catch (Exception e) { return raw; }
    }
}