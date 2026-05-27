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

        findViewById(R.id.btnBackTransaction).setOnClickListener(v -> finish());
        llTransaksiContainer = findViewById(R.id.llTransaksiContainer);

        loadTransactions();
    }

    private void loadTransactions() {
        llTransaksiContainer.removeAllViews();
        if (userId == -1) return;

        Cursor c = db.getTransaksiByUser(userId);
        if (!c.moveToFirst()) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada transaksi.");
            empty.setPadding(32, 32, 32, 32);
            llTransaksiContainer.addView(empty);
            c.close();
            return;
        }
        do {
            int    transaksiId = c.getInt(c.getColumnIndexOrThrow("id"));
            String namaResto   = c.getString(c.getColumnIndexOrThrow("nama_resto"));
            int    total       = c.getInt(c.getColumnIndexOrThrow("total_harga"));
            String tanggal     = c.getString(c.getColumnIndexOrThrow("tanggal"));
            String status      = c.getString(c.getColumnIndexOrThrow("status_pesanan"));

            View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_transaction_row, llTransaksiContainer, false);

            TextView tvNama    = row.findViewById(R.id.tvTransNamaResto);
            TextView tvTotal   = row.findViewById(R.id.tvTransTotal);
            TextView tvTanggal = row.findViewById(R.id.tvTransTanggal);
            TextView tvStatus  = row.findViewById(R.id.tvTransStatus);
            ImageView imgResto = row.findViewById(R.id.imgTransResto);
            Button btnDetail   = row.findViewById(R.id.btnTransDetail);

            tvNama.setText(namaResto);
            tvTotal.setText("Rp" + formatRupiah(total));
            tvTanggal.setText(formatTanggal(tanggal));
            tvStatus.setText(status);
            imgResto.setImageResource(getRestoImage(namaResto));

            btnDetail.setOnClickListener(v -> {
                Intent intent = new Intent(this, TransactionDetailActivity.class);
                intent.putExtra("transaksi_id", transaksiId);
                intent.putExtra("restoran_nama", namaResto);
                intent.putExtra("total_harga", total);
                intent.putExtra("tanggal", tanggal);
                startActivity(intent);
            });

            llTransaksiContainer.addView(row);
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

    private String formatRupiah(int amount) {
        return String.format("%,d", amount).replace(',', '.');
    }

    private String formatTanggal(String raw) {
        // raw is "2026-05-12 09:30:00", show as "12 Mei 2026"
        try {
            String[] parts = raw.split(" ")[0].split("-");
            String[] bulan = {"","Jan","Feb","Mar","Apr","Mei","Jun",
                              "Jul","Agu","Sep","Okt","Nov","Des"};
            int m = Integer.parseInt(parts[1]);
            return parts[2] + " " + bulan[m] + " " + parts[0];
        } catch (Exception e) { return raw; }
    }
}
