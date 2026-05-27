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

        int    transaksiId  = getIntent().getIntExtra("transaksi_id", -1);
        String restoranNama = getIntent().getStringExtra("restoran_nama");
        int    totalHarga   = getIntent().getIntExtra("total_harga", 0);
        String tanggal      = getIntent().getStringExtra("tanggal");

        findViewById(R.id.btnBackDetail).setOnClickListener(v -> finish());

        // Header info
        TextView tvNama    = findViewById(R.id.tvDetailNamaResto);
        TextView tvTanggal = findViewById(R.id.tvDetailTanggal);
        TextView tvOrderId = findViewById(R.id.tvDetailOrderId);
        TextView tvStatus  = findViewById(R.id.tvDetailStatus);
        ImageView imgResto = findViewById(R.id.imgDetailResto);

        tvNama.setText(restoranNama);
        tvTanggal.setText(formatTanggal(tanggal));
        tvOrderId.setText("Order ID: #BCSHA" + transaksiId);
        tvStatus.setText("Paid");
        imgResto.setImageResource(getRestoImage(restoranNama));

        // Item list
        LinearLayout llItems = findViewById(R.id.llDetailItems);
        Cursor c = db.getDetailTransaksi(transaksiId);
        int subtotal = 0;
        int itemCount = 0;
        while (c.moveToNext()) {
            String nama  = c.getString(c.getColumnIndexOrThrow("nama_item"));
            int    harga = c.getInt(c.getColumnIndexOrThrow("harga_saat_ini"));
            int    qty   = c.getInt(c.getColumnIndexOrThrow("jumlah"));
            subtotal += harga * qty;
            itemCount += qty;

            View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_detail_row, llItems, false);
            TextView tvItemNama  = row.findViewById(R.id.tvDetailItemNama);
            TextView tvItemHarga = row.findViewById(R.id.tvDetailItemHarga);

            String label = qty > 1 ? nama + " x" + qty : nama;
            tvItemNama.setText(label);
            tvItemHarga.setText("Rp " + formatRupiah(harga * qty));
            llItems.addView(row);
        }
        c.close();

        // Item count badge
        TextView tvItemCount = findViewById(R.id.tvDetailItemCount);
        tvItemCount.setText(itemCount + " Item");

        // Summary
        int discount = subtotal + BIAYA_ONGKIR - totalHarga;
        if (discount < 0) discount = 0;

        TextView tvOngkir  = findViewById(R.id.tvDetailOngkir);
        TextView tvPromo   = findViewById(R.id.tvDetailPromo);
        TextView tvTotal   = findViewById(R.id.tvDetailTotal);
        LinearLayout llPromoRow = findViewById(R.id.llDetailPromoRow);

        tvOngkir.setText("Rp " + formatRupiah(BIAYA_ONGKIR));
        tvTotal.setText("Rp " + formatRupiah(totalHarga));

        if (discount > 0) {
            llPromoRow.setVisibility(View.VISIBLE);
            tvPromo.setText("- Rp " + formatRupiah(discount));
        } else {
            llPromoRow.setVisibility(View.GONE);
        }

        // Pesan Ulang
        findViewById(R.id.btnPesanUlang).setOnClickListener(v -> finish());
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
            String[] bulan = {"","Jan","Feb","Mar","Apr","Mei","Jun",
                              "Jul","Agu","Sep","Okt","Nov","Des"};
            int m = Integer.parseInt(parts[1]);
            return parts[2] + " " + bulan[m] + " " + parts[0];
        } catch (Exception e) { return raw; }
    }
}
