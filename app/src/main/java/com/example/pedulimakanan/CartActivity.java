package com.example.pedulimakanan;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CartActivity extends Activity {

    private DatabaseHelper db;
    private int userId;
    private LinearLayout llCartContainer;
    private TextView tvCartTotal;
    private TextView tvSaldoInfo; // [UPDATE]: Tambahan variabel untuk info saldo
    private List<Integer> selectedCartIds = new ArrayList<>();
    private int totalHargaTerpilih = 0;
    private int restoranIdAktif = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart); // HARUS DI SINI DULU

        db = new DatabaseHelper(this);
        userId = getSharedPreferences("user_session", MODE_PRIVATE).getInt("user_id", -1);

        // Sekarang baru panggil findViewById
        llCartContainer = findViewById(R.id.llCartContainer);
        tvCartTotal     = findViewById(R.id.tvCartTotal);
        tvSaldoInfo     = findViewById(R.id.tvSaldoInfo); // Ini sudah benar

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnCheckout = findViewById(R.id.btnCheckoutCart);
        if (btnCheckout != null) btnCheckout.setOnClickListener(v -> prosesCheckout());

        loadKeranjangData();
    }

    // [UPDATE]: Method untuk update saldo saat activity aktif kembali
    @Override
    protected void onResume() {
        super.onResume();
        tampilkanSaldo();
    }

    private void tampilkanSaldo() {
        if (tvSaldoInfo != null) {
            int saldo = db.getSaldo(userId);
            tvSaldoInfo.setText("Saldo Anda: Rp " + formatRupiah(saldo));
        }
    }

    private void loadKeranjangData() {
        if (llCartContainer == null) return;
        llCartContainer.removeAllViews();
        selectedCartIds.clear();
        totalHargaTerpilih = 0;
        updateTotalUI();

        Cursor c = db.getKeranjangByUser(userId);
        try {
            while (c != null && c.moveToNext()) {
                int cartId = c.getInt(c.getColumnIndexOrThrow("id"));
                String nama = c.getString(c.getColumnIndexOrThrow("nama_item"));
                int harga  = c.getInt(c.getColumnIndexOrThrow("harga"));
                int jumlah = c.getInt(c.getColumnIndexOrThrow("jumlah"));
                int subtotalItem = harga * jumlah;
                restoranIdAktif = c.getInt(c.getColumnIndexOrThrow("restoran_id"));

                View row = LayoutInflater.from(this).inflate(R.layout.item_cart_row, llCartContainer, false);
                ((TextView) row.findViewById(R.id.tvCartItemNama)).setText(nama);
                ((TextView) row.findViewById(R.id.tvCartItemHarga)).setText("Harga: Rp " + formatRupiah(harga));
                ((TextView) row.findViewById(R.id.tvCartItemQty)).setText("x" + jumlah);
                ((TextView) row.findViewById(R.id.tvCartItemSubtotal)).setText("Total: Rp " + formatRupiah(subtotalItem));

                CheckBox cb = row.findViewById(R.id.cbCartItem);
                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) selectedCartIds.add(cartId);
                    else selectedCartIds.remove(Integer.valueOf(cartId));
                    calculateTotal();
                });

                row.findViewById(R.id.btnHapusItem).setOnClickListener(v -> {
                    db.hapusItemKeranjang(cartId);
                    loadKeranjangData();
                });
                llCartContainer.addView(row);
            }
        } finally {
            if (c != null) c.close();
        }
    }

    private void calculateTotal() {
        totalHargaTerpilih = 0;
        Cursor c = db.getKeranjangByUser(userId);
        try {
            while (c != null && c.moveToNext()) {
                int id = c.getInt(c.getColumnIndexOrThrow("id"));
                if (selectedCartIds.contains(id)) {
                    totalHargaTerpilih += (c.getInt(c.getColumnIndexOrThrow("harga")) * c.getInt(c.getColumnIndexOrThrow("jumlah")));
                }
            }
        } finally {
            if (c != null) c.close();
        }
        updateTotalUI();
    }

    private void updateTotalUI() {
        tvCartTotal.setText("Rp " + formatRupiah(totalHargaTerpilih));
    }

    private void prosesCheckout() {
        if (selectedCartIds.isEmpty()) {
            Toast.makeText(this, "Pilih item yang ingin dibayar!", Toast.LENGTH_SHORT).show();
            return;
        }

        int saldoUser = db.getSaldo(userId);
        if (saldoUser < totalHargaTerpilih) {
            Toast.makeText(this, "Saldo tidak cukup! Sisa saldo: Rp " + formatRupiah(saldoUser), Toast.LENGTH_LONG).show();
            return;
        }

        SQLiteDatabase writeDb = db.getWritableDatabase();
        writeDb.beginTransaction();
        try {
            db.updateSaldo(userId, saldoUser - totalHargaTerpilih);

            ContentValues cv = new ContentValues();
            cv.put("user_id", userId);
            cv.put("restoran_id", restoranIdAktif);
            cv.put("total_harga", totalHargaTerpilih);
            cv.put("status_pesanan", "Selesai");
            cv.put("tanggal", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

            long id = writeDb.insert(DatabaseHelper.TABLE_TRANSAKSI, null, cv);
            if (id != -1) {
                for (int cartId : selectedCartIds) {
                    db.hapusItemKeranjang(cartId);
                }
                writeDb.setTransactionSuccessful();
                Toast.makeText(this, "Pembayaran Berhasil!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, TransactionActivity.class));
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            writeDb.endTransaction();
        }
    }

    private String formatRupiah(int amount) {
        return String.format("%,d", amount).replace(',', '.');
    }
}