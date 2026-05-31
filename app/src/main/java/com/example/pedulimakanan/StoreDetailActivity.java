package com.example.pedulimakanan;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

public class StoreDetailActivity extends Activity {

    private static final String CONNECTOR_URL = "http://172.104.183.200/pedulimakanan/connector.php";
    private static final String PREF_NAME = "login_session";
    private static final int BIAYA_ONGKIR = 6000;

    private static final Map<String, Integer> PROMO_MAP = new HashMap<>();
    static {
        PROMO_MAP.put("HEMAT10",  10000);
        PROMO_MAP.put("DISKON19", 19000);
        PROMO_MAP.put("MURAH5",   5000);
    }

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

        restoranId   = getIntent().getIntExtra("restoran_id", -1);
        restoranNama = getIntent().getStringExtra("restoran_nama");

        // Menggunakan session login terpusat dari main branch
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
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

        // Inisialisasi tombol pembayaran dan keranjang belanja
        findViewById(R.id.btnBayarLangsung).setOnClickListener(v -> placeOrderDirectly());
        findViewById(R.id.btnTambahKeranjang).setOnClickListener(v -> addToCartDatabase());

        // Pemuatan data menu dari API server remote
        new LoadMenuTask().execute();
        updateSummary();
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

    private void placeOrderDirectly() {
        if (cart.isEmpty()) {
            Toast.makeText(this, "Pilih menu terlebih dahulu", Toast.LENGTH_SHORT).show();
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

            // Eksekusi checkout pembayaran langsung ke server API remote
            new DirectOrderTask().execute(cartArray.toString(), String.valueOf(total));

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addToCartDatabase() {
        if (cart.isEmpty()) {
            Toast.makeText(this, "Pilih menu terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userId == -1) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONArray cartArray = new JSONArray();
            for (CartItem item : cart.values()) {
                JSONObject obj = new JSONObject();
                obj.put("menu_id", item.menuId);
                obj.put("jumlah", item.jumlah);
                cartArray.put(obj);
            }

            // Kirim kumpulan data menu sekaligus ke API keranjang server
            new AddToCartTask().execute(cartArray.toString());

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

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
            case "tropical blast":  return R.drawable.img_tropical_blast;
            case "green power":     return R.drawable.img_green_power;
            case "berry bliss":     return R.drawable.img_berry_bliss;
            case "peanut butter boo": return R.drawable.img_peanut_butter_boo;
            case "detox cleanse":   return R.drawable.img_detox_clease;
            case "granola bar":     return R.drawable.img_granola_bar;
            case "edamame cup":     return R.drawable.img_edamame_cup;
            case "mixed nuts":      return R.drawable.img_mixed_nuts;
            case "rice cake":       return R.drawable.img_rice_cake;
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

    // ── ASYNCTASK 1: Mengambil Data Daftar Menu dari Server API ──
    private class LoadMenuTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("get_menu_by_restoran", "UTF-8") + "&" +
                        URLEncoder.encode("restoran_id", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(restoranId), "UTF-8");

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(postData);
                writer.flush();
                writer.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                return result.toString();
            } catch (Exception e) {
                return null;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response == null) return;
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.optBoolean("success", false)) {
                    llMenuContainer.removeAllViews();
                    JSONArray menuArray = jsonObject.getJSONArray("menu");

                    for (int i = 0; i < menuArray.length(); i++) {
                        JSONObject obj = menuArray.getJSONObject(i);
                        int menuId = obj.getInt("id");
                        String nama = obj.getString("nama_item");
                        int harga = obj.getInt("harga");
                        String desc = obj.optString("deskripsi", "");

                        View row = LayoutInflater.from(StoreDetailActivity.this)
                                .inflate(R.layout.item_menu_row, llMenuContainer, false);

                        TextView tvNama = row.findViewById(R.id.tvMenuNama);
                        TextView tvHarga = row.findViewById(R.id.tvMenuHarga);
                        TextView tvDesc = row.findViewById(R.id.tvMenuDesc);
                        ImageView imgMenu = row.findViewById(R.id.imgMenu);
                        TextView tvQty = row.findViewById(R.id.tvQty);
                        ImageButton btnPlus = row.findViewById(R.id.btnPlus);
                        ImageButton btnMinus = row.findViewById(R.id.btnMinus);

                        tvNama.setText(nama);
                        tvHarga.setText("Rp " + formatRupiah(harga));
                        tvDesc.setText(desc);
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
                }
            } catch (Exception ignored) {}
        }
    }

    // ── ASYNCTASK 2: Kirim Transaksi Bayar Langsung ke Server (Potong Saldo MySQL) ──
    private class DirectOrderTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String cartItemsJson = params[0];
            String totalHarga = params[1];
            HttpURLConnection conn = null;
            try {
                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("buat_transaksi_server", "UTF-8") + "&" +
                        URLEncoder.encode("user_id", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(userId), "UTF-8") + "&" +
                        URLEncoder.encode("restoran_id", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(restoranId), "UTF-8") + "&" +
                        URLEncoder.encode("total_harga", "UTF-8") + "=" + URLEncoder.encode(totalHarga, "UTF-8") + "&" +
                        URLEncoder.encode("items", "UTF-8") + "=" + URLEncoder.encode(cartItemsJson, "UTF-8");

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(postData);
                writer.flush();
                writer.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                return result.toString();
            } catch (Exception e) {
                return null;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response == null) {
                Toast.makeText(StoreDetailActivity.this, "Koneksi server gagal", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.optBoolean("success", false)) {
                    Toast.makeText(StoreDetailActivity.this, "Pembayaran Berhasil! Saldo terpotong.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(StoreDetailActivity.this, TransactionActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    // Berteriak jika saldo di server tidak cukup atau ada error database
                    Toast.makeText(StoreDetailActivity.this, jsonObject.optString("message", "Gagal memproses pesanan"), Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(StoreDetailActivity.this, "Response tidak valid", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ── ASYNCTASK 3: Memasukkan Data Belanjaan ke Keranjang Server ──
    private class AddToCartTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String itemsJson = params[0];
            HttpURLConnection conn = null;
            try {
                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("tambah_ke_keranjang_server", "UTF-8") + "&" +
                        URLEncoder.encode("user_id", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(userId), "UTF-8") + "&" +
                        URLEncoder.encode("restoran_id", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(restoranId), "UTF-8") + "&" +
                        URLEncoder.encode("items", "UTF-8") + "=" + URLEncoder.encode(itemsJson, "UTF-8");

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(postData);
                writer.flush();
                writer.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                return result.toString();
            } catch (Exception e) {
                return null;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response == null) {
                Toast.makeText(StoreDetailActivity.this, "Gagal terhubung ke keranjang belanja", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.optBoolean("success", false)) {
                    Toast.makeText(StoreDetailActivity.this, "Berhasil ditambahkan ke keranjang belanja!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(StoreDetailActivity.this, "Gagal memasukkan menu ke keranjang", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(StoreDetailActivity.this, "Error parsing data keranjang", Toast.LENGTH_SHORT).show();
            }
        }
    }
}