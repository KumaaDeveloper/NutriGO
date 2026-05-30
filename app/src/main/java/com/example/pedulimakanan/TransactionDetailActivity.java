package com.example.pedulimakanan;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

public class TransactionDetailActivity extends Activity {

    private static final int BIAYA_ONGKIR = 6000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        DatabaseHelper db = new DatabaseHelper(this);

        // Ambil Data dari Intent
        int transaksiId = getIntent().getIntExtra("transaksi_id", -1);
        String restoranNama = getIntent().getStringExtra("restoran_nama");
        int totalHarga = getIntent().getIntExtra("total_harga", 0);
        String tanggal = getIntent().getStringExtra("tanggal");

        findViewById(R.id.btnBackDetail).setOnClickListener(v -> finish());

        // Header info
        ((TextView) findViewById(R.id.tvDetailNamaResto)).setText(restoranNama);
        ((TextView) findViewById(R.id.tvDetailTanggal)).setText(formatTanggal(tanggal));
        ((TextView) findViewById(R.id.tvDetailOrderId)).setText("Order ID: #BCSHA" + transaksiId);
        ((TextView) findViewById(R.id.tvDetailStatus)).setText("Paid");
        ((ImageView) findViewById(R.id.imgDetailResto)).setImageResource(getRestoImage(restoranNama));

        // Item list
        LinearLayout llItems = findViewById(R.id.llDetailItems);
        Cursor c = db.getDetailTransaksi(transaksiId);
        int subtotal = 0;
        int itemCount = 0;

        try {
            while (c != null && c.moveToNext()) {
                String nama = c.getString(c.getColumnIndexOrThrow("nama_item"));
                int harga = c.getInt(c.getColumnIndexOrThrow("harga_saat_ini"));
                int qty = c.getInt(c.getColumnIndexOrThrow("jumlah"));

                subtotal += (harga * qty);
                itemCount += qty;

                View row = LayoutInflater.from(this).inflate(R.layout.item_detail_row, llItems, false);
                ((TextView) row.findViewById(R.id.tvDetailItemNama)).setText(qty > 1 ? nama + " x" + qty : nama);
                ((TextView) row.findViewById(R.id.tvDetailItemHarga)).setText("Rp " + formatRupiah(harga * qty));
                llItems.addView(row);
            }
        } finally {
            if (c != null) c.close();
        }

        // Item count badge
        ((TextView) findViewById(R.id.tvDetailItemCount)).setText(itemCount + " Item");

        // Summary
        int discount = (subtotal + BIAYA_ONGKIR) - totalHarga;
        if (discount < 0) discount = 0;

        ((TextView) findViewById(R.id.tvDetailOngkir)).setText("Rp " + formatRupiah(BIAYA_ONGKIR));
        ((TextView) findViewById(R.id.tvDetailTotal)).setText("Rp " + formatRupiah(totalHarga));

        LinearLayout llPromoRow = findViewById(R.id.llDetailPromoRow);
        if (discount > 0) {
            llPromoRow.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvDetailPromo)).setText("- Rp " + formatRupiah(discount));
        } else {
            llPromoRow.setVisibility(View.GONE);
        }

        // Pesan Ulang
        findViewById(R.id.btnPesanUlang).setOnClickListener(v -> finish());
    }

    private int getRestoImage(String nama) {
        if (nama == null) return R.drawable.img_placeholder_food;
        switch (nama.toLowerCase().trim()) {
            case "saladstop": return R.drawable.img_saladstop;
            case "burgreen": return R.drawable.img_burgreen;
            case "supergrain": return R.drawable.img_supergrain;
            case "greenbowl": return R.drawable.img_greenbowl;
            case "freshbox": return R.drawable.img_freshbox;
            case "nutrisnack": return R.drawable.img_nutri_snack;
            case "smoothiebar": return R.drawable.img_smoothie_bar;
            default: return R.drawable.img_placeholder_food;
        }
    }

    private String formatRupiah(int amount) {
        return String.format("%,d", amount).replace(',', '.');
    }

    private String formatTanggal(String raw) {
        try {
            String[] parts = raw.split(" ")[0].split("-");
            String[] bulan = {"", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des"};
            int m = Integer.parseInt(parts[1]);
            return parts[2] + " " + bulan[m] + " " + parts[0];
        } catch (Exception e) { return raw; }
    }
}