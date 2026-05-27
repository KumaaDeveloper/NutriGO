package com.example.pedulimakanan;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class StoreDetailActivity extends Activity {

    private static final int BIAYA_ONGKIR = 6000;

    // Fix 1: correct generic type — Map<String, Integer> not Map<String, HashMap>
    private static final Map<String, Integer> PROMO_MAP = new HashMap<>();
    static {
        PROMO_MAP.put("HEMAT10",  10000);
        PROMO_MAP.put("DISKON19", 19000);
        PROMO_MAP.put("MURAH5",   5000);
    }

    private DatabaseHelper db;
    private int restoranId;
    private String restoranNama;
    private int userId;
    private int appliedDiscount = 0;

    private final Map<Integer, CartItem> cart = new LinkedHashMap<>();

    private LinearLayout llMenuContainer;
    private TextView tvTotal, tvHarga, tvOngkir, tvPromoAmount;
    private LinearLayout llPromoRow;
    private EditText etPromo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_detail);

        db = new DatabaseHelper(this);
        restoranId   = getIntent().getIntExtra("restoran_id", -1);
        restoranNama = getIntent().getStringExtra("restoran_nama");

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        TextView tvTitle = findViewById(R.id.tvStoreTitle);
        tvTitle.setText(restoranNama);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvHarga       = findViewById(R.id.tvHarga);
        tvOngkir      = findViewById(R.id.tvOngkir);
        tvPromoAmount = findViewById(R.id.tvPromoAmount);
        llPromoRow    = findViewById(R.id.llPromoRow);
        tvTotal       = findViewById(R.id.tvTotal);
        etPromo       = findViewById(R.id.etPromo);
        llMenuContainer = findViewById(R.id.llMenuContainer);

        tvOngkir.setText("Rp " + formatRupiah(BIAYA_ONGKIR));

        findViewById(R.id.btnAddPromo).setOnClickListener(v -> applyPromo());
        findViewById(R.id.btnPesan).setOnClickListener(v -> placeOrder());

        loadMenu();
        updateSummary();
    }

    private void loadMenu() {
        llMenuContainer.removeAllViews();
        Cursor c = db.getMenuByRestoran(restoranId);
        while (c.moveToNext()) {
            int    menuId = c.getInt(c.getColumnIndexOrThrow("id"));
            String nama   = c.getString(c.getColumnIndexOrThrow("nama_item"));
            int    harga  = c.getInt(c.getColumnIndexOrThrow("harga"));
            String desc   = c.getString(c.getColumnIndexOrThrow("deskripsi"));

            // Fix 2: item_menu_row layout must exist in res/layout/
            View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_menu_row, llMenuContainer, false);

            TextView    tvNama   = row.findViewById(R.id.tvMenuNama);
            TextView    tvHarga  = row.findViewById(R.id.tvMenuHarga);
            TextView    tvDesc   = row.findViewById(R.id.tvMenuDesc);
            ImageView   imgMenu  = row.findViewById(R.id.imgMenu);
            TextView    tvQty    = row.findViewById(R.id.tvQty);
            ImageButton btnPlus  = row.findViewById(R.id.btnPlus);
            ImageButton btnMinus = row.findViewById(R.id.btnMinus);

            tvNama.setText(nama);
            tvHarga.setText("Rp " + formatRupiah(harga));
            tvDesc.setText(desc);

            // Fix 3: always fall back to placeholder — no missing drawable crash
            imgMenu.setImageResource(getMenuImage(nama));

            int currentQty = cart.containsKey(menuId) ? cart.get(menuId).jumlah : 0;
            tvQty.setText(String.valueOf(currentQty));

            btnPlus.setOnClickListener(v -> {
                CartItem item = cart.containsKey(menuId)
                        ? cart.get(menuId)
                        : new CartItem(menuId, nama, harga, 0);
                item.jumlah++;
                cart.put(menuId, item);
                tvQty.setText(String.valueOf(item.jumlah));
                updateSummary();
            });

            btnMinus.setOnClickListener(v -> {
                if (!cart.containsKey(menuId)) return;
                CartItem item = cart.get(menuId);
                item.jumlah--;
                if (item.jumlah <= 0) {
                    cart.remove(menuId);
                    tvQty.setText("0");
                } else {
                    tvQty.setText(String.valueOf(item.jumlah));
                }
                updateSummary();
            });

            llMenuContainer.addView(row);
        }
        c.close();
    }

    private void updateSummary() {
        int subtotal = 0;
        for (CartItem item : cart.values()) subtotal += item.harga * item.jumlah;
        int total = Math.max(0, subtotal + BIAYA_ONGKIR - appliedDiscount);

        tvHarga.setText("Rp " + formatRupiah(subtotal));
        tvPromoAmount.setText("- Rp " + formatRupiah(appliedDiscount));
        llPromoRow.setVisibility(appliedDiscount > 0 ? View.VISIBLE : View.GONE);
        tvTotal.setText("Rp " + formatRupiah(total));
    }

    private void applyPromo() {
        String code = etPromo.getText().toString().trim().toUpperCase();
        if (code.isEmpty()) {
            Toast.makeText(this, "Masukkan kode promo", Toast.LENGTH_SHORT).show();
            return;
        }
        if (PROMO_MAP.containsKey(code)) {
            appliedDiscount = PROMO_MAP.get(code);
            Toast.makeText(this, "Promo berhasil diterapkan!", Toast.LENGTH_SHORT).show();
            updateSummary();
        } else {
            Toast.makeText(this, "Kode promo tidak valid", Toast.LENGTH_SHORT).show();
        }
    }

    private void placeOrder() {
        if (cart.isEmpty()) {
            Toast.makeText(this, "Keranjang masih kosong", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userId == -1) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONArray cartArray = new JSONArray();
            int subtotal = 0;
            for (CartItem item : cart.values()) {
                JSONObject obj = new JSONObject();
                obj.put("menu_id",   item.menuId);
                obj.put("nama_item", item.nama);
                obj.put("harga",     item.harga);
                obj.put("jumlah",    item.jumlah);
                cartArray.put(obj);
                subtotal += item.harga * item.jumlah;
            }
            int total = Math.max(0, subtotal + BIAYA_ONGKIR - appliedDiscount);
            long transaksiId = db.buatTransaksi(userId, restoranId, cartArray, total);
            if (transaksiId != -1) {
                Toast.makeText(this, "Pesanan berhasil!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, TransactionActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Gagal membuat pesanan", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Returns a drawable for the menu item.
     * If you haven't added a specific image yet, it falls back to the placeholder
     * so the app never crashes from a missing drawable.
     */
    private int getMenuImage(String nama) {
        if (nama == null) return R.drawable.img_placeholder_food;
        switch (nama.toLowerCase().trim()) {
            case "tuna san":        return R.drawable.img_tuna_san;
            case "hail caesar":     return R.drawable.img_hail_caesar;
            case "mini bowl":       return R.drawable.img_mini_bowl;
            case "protein mix":     return R.drawable.img_protein_mix;
            case "brown rice bowl": return R.drawable.img_brown_rice_bowl;
            case "quinoa power":    return R.drawable.img_quinoa_power;
            case "grain classic":   return R.drawable.img_grain_classic;
            case "green detox":     return R.drawable.img_green_detox;
            case "chicken bowl":    return R.drawable.img_chicken_bowl;
            case "wrap veggie":     return R.drawable.img_wrap_veggie;
            case "fruit bowl":      return R.drawable.img_fruit_bowl;
            case "vegan burger":    return R.drawable.img_vegan_burger;
            case "smoothie bowl":   return R.drawable.img_smoothie_bowl;
            default:                return R.drawable.img_placeholder_food;
        }
    }

    private String formatRupiah(int amount) {
        return String.format("%,d", amount).replace(',', '.');
    }

    static class CartItem {
        int menuId, harga, jumlah;
        String nama;
        CartItem(int menuId, String nama, int harga, int jumlah) {
            this.menuId = menuId; this.nama = nama;
            this.harga  = harga;  this.jumlah = jumlah;
        }
    }
}