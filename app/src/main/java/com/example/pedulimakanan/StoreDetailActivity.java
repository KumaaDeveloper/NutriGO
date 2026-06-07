package com.example.pedulimakanan;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import java.util.LinkedHashMap;
import java.util.Map;

public class StoreDetailActivity extends Activity {

    private static final String CONNECTOR_URL = "http://172.104.183.200/pedulimakanan/connector.php";
    private static final String PREF_NAME = "login_session";
    private static final int BIAYA_ONGKIR = 6000;
    private static final int REQ_PROMO = 2001;

    private int restoranId;
    private String restoranNama;
    private int userId;

    private int appliedDiscount = 0;
    private boolean appliedFreeOngkir = false;
    private String appliedPromoText = "";

    private final Map<Integer, CartItem> cart = new LinkedHashMap<>();

    private LinearLayout llMenuContainer;
    private TextView tvTotal;
    private TextView tvHarga;
    private TextView tvOngkir;
    private TextView tvPromoAmount;
    private TextView tvPromoRow;
    private TextView tvPromoStatus;
    private LinearLayout llPromoRow;
    private Button btnCekPromo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_detail);

        restoranId = getIntent().getIntExtra("restoran_id", -1);
        restoranNama = getIntent().getStringExtra("restoran_nama");

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        TextView tvTitle = findViewById(R.id.tvStoreTitle);
        tvTitle.setText(restoranNama);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvHarga = findViewById(R.id.tvHarga);
        tvOngkir = findViewById(R.id.tvOngkir);
        tvPromoAmount = findViewById(R.id.tvPromoAmount);
        tvPromoRow = findViewById(R.id.tvPromoRow);
        tvPromoStatus = findViewById(R.id.tvPromoStatus);
        llPromoRow = findViewById(R.id.llPromoRow);
        tvTotal = findViewById(R.id.tvTotal);
        llMenuContainer = findViewById(R.id.llMenuContainer);
        btnCekPromo = findViewById(R.id.btnCekPromo);

        btnCekPromo.setOnClickListener(v -> openPromoActivity());
        findViewById(R.id.btnBayarLangsung).setOnClickListener(v -> placeOrderDirectly());
        findViewById(R.id.btnTambahKeranjang).setOnClickListener(v -> addToCartDatabase());

        new LoadMenuTask().execute();
        updateSummary();
    }

    private void openPromoActivity() {
        if (cart.isEmpty()) {
            Toast.makeText(this, "Pilih menu terlebih dahulu untuk cek promo", Toast.LENGTH_SHORT).show();
            return;
        }

        int subtotal = getSubtotal();

        Intent intent = new Intent(StoreDetailActivity.this, PromoActivity.class);
        intent.putExtra("restoran_nama", restoranNama);
        intent.putExtra("subtotal", subtotal);
        intent.putExtra("ongkir", BIAYA_ONGKIR);
        intent.putExtra("applied_discount", appliedDiscount);
        intent.putExtra("applied_free_ongkir", appliedFreeOngkir);
        intent.putExtra("applied_promo_text", appliedPromoText);

        startActivityForResult(intent, REQ_PROMO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PROMO && resultCode == RESULT_OK && data != null) {
            appliedDiscount = data.getIntExtra("discount", 0);
            appliedFreeOngkir = data.getBooleanExtra("free_ongkir", false);
            appliedPromoText = data.getStringExtra("promo_text");

            if (appliedPromoText == null) {
                appliedPromoText = "";
            }

            updateSummary();
        }
    }

    private int getSubtotal() {
        int subtotal = 0;

        for (CartItem item : cart.values()) {
            subtotal += item.harga * item.jumlah;
        }

        return subtotal;
    }

    private void updateSummary() {
        int subtotal = getSubtotal();
        int ongkirFinal = appliedFreeOngkir ? 0 : BIAYA_ONGKIR;
        int discountFinal = Math.min(appliedDiscount, subtotal);
        int total = Math.max(0, subtotal + ongkirFinal - discountFinal);

        tvHarga.setText("Rp " + formatRupiah(subtotal));

        if (appliedFreeOngkir) {
            tvOngkir.setText("Gratis");
        } else {
            tvOngkir.setText("Rp " + formatRupiah(BIAYA_ONGKIR));
        }

        if (discountFinal > 0 || appliedFreeOngkir) {
            llPromoRow.setVisibility(View.VISIBLE);

            if (appliedPromoText.trim().isEmpty()) {
                tvPromoRow.setText("Promo");
            } else {
                tvPromoRow.setText("Promo (" + appliedPromoText + ")");
            }

            if (discountFinal > 0 && appliedFreeOngkir) {
                tvPromoAmount.setText("- Rp " + formatRupiah(discountFinal) + " + Gratis Ongkir");
            } else if (discountFinal > 0) {
                tvPromoAmount.setText("- Rp " + formatRupiah(discountFinal));
            } else {
                tvPromoAmount.setText("Gratis Ongkir");
            }

            tvPromoStatus.setText(appliedPromoText.trim().isEmpty() ? "Promo digunakan" : appliedPromoText);
        } else {
            llPromoRow.setVisibility(View.GONE);
            tvPromoStatus.setText("Belum ada promo digunakan");
        }

        tvTotal.setText("Rp " + formatRupiah(total));
        updatePromoButtonState();
    }

    private void updatePromoButtonState() {
        if (btnCekPromo == null) {
            return;
        }

        boolean adaMenu = !cart.isEmpty();
        btnCekPromo.setEnabled(adaMenu);

        if (adaMenu) {
            btnCekPromo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#246E9B")));
            btnCekPromo.setTextColor(Color.WHITE);
        } else {
            btnCekPromo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            btnCekPromo.setTextColor(Color.WHITE);
        }
    }

    private void resetPromoIfCartEmpty() {
        if (cart.isEmpty()) {
            appliedDiscount = 0;
            appliedFreeOngkir = false;
            appliedPromoText = "";
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
                obj.put("menu_id", item.menuId);
                obj.put("nama_item", item.nama);
                obj.put("harga", item.harga);
                obj.put("jumlah", item.jumlah);
                cartArray.put(obj);

                subtotal += item.harga * item.jumlah;
            }

            int ongkirFinal = appliedFreeOngkir ? 0 : BIAYA_ONGKIR;
            int discountFinal = Math.min(appliedDiscount, subtotal);
            int total = Math.max(0, subtotal + ongkirFinal - discountFinal);

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

            new AddToCartTask().execute(cartArray.toString());

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private int getMenuImage(String nama) {
        if (nama == null) return R.drawable.img_placeholder_food;

        switch (nama.toLowerCase().trim()) {
            case "tuna san": return R.drawable.img_tuna_san;
            case "hail caesar": return R.drawable.img_hail_caesar;
            case "mini bowl": return R.drawable.img_mini_bowl;
            case "protein mix": return R.drawable.img_protein_mix;
            case "brown rice bowl": return R.drawable.img_brown_rice_bowl;
            case "quinoa power": return R.drawable.img_quinoa_power;
            case "grain classic": return R.drawable.img_grain_classic;
            case "green detox": return R.drawable.img_green_detox;
            case "chicken bowl": return R.drawable.img_chicken_bowl;
            case "wrap veggie": return R.drawable.img_wrap_veggie;
            case "fruit bowl": return R.drawable.img_fruit_bowl;
            case "vegan burger": return R.drawable.img_vegan_burger;
            case "smoothie bowl": return R.drawable.img_smoothie_bowl;
            case "tropical blast": return R.drawable.img_tropical_blast;
            case "green power": return R.drawable.img_green_power;
            case "berry bliss": return R.drawable.img_berry_bliss;
            case "peanut butter boo": return R.drawable.img_peanut_butter_boo;
            case "detox cleanse": return R.drawable.img_detox_clease;
            case "granola bar": return R.drawable.img_granola_bar;
            case "edamame cup": return R.drawable.img_edamame_cup;
            case "mixed nuts": return R.drawable.img_mixed_nuts;
            case "rice cake": return R.drawable.img_rice_cake;
            default: return R.drawable.img_placeholder_food;
        }
    }

    private String formatRupiah(int amount) {
        return String.format("%,d", amount).replace(',', '.');
    }

    static class CartItem {
        int menuId;
        int harga;
        int jumlah;
        String nama;

        CartItem(int menuId, String nama, int harga, int jumlah) {
            this.menuId = menuId;
            this.nama = nama;
            this.harga = harga;
            this.jumlah = jumlah;
        }
    }

    private class LoadMenuTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            HttpURLConnection conn = null;

            try {
                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" +
                                URLEncoder.encode("get_menu_by_restoran", "UTF-8") + "&" +
                                URLEncoder.encode("restoran_id", "UTF-8") + "=" +
                                URLEncoder.encode(String.valueOf(restoranId), "UTF-8");

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
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response == null) {
                return;
            }

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
                            if (!cart.containsKey(menuId)) {
                                return;
                            }

                            CartItem item = cart.get(menuId);
                            item.jumlah--;

                            if (item.jumlah <= 0) {
                                cart.remove(menuId);
                                tvQty.setText("0");
                                resetPromoIfCartEmpty();
                            } else {
                                tvQty.setText(String.valueOf(item.jumlah));
                            }

                            updateSummary();
                        });

                        llMenuContainer.addView(row);
                    }
                }

            } catch (Exception ignored) {
            }
        }
    }

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

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" +
                                URLEncoder.encode("buat_transaksi_server", "UTF-8") + "&" +
                                URLEncoder.encode("user_id", "UTF-8") + "=" +
                                URLEncoder.encode(String.valueOf(userId), "UTF-8") + "&" +
                                URLEncoder.encode("restoran_id", "UTF-8") + "=" +
                                URLEncoder.encode(String.valueOf(restoranId), "UTF-8") + "&" +
                                URLEncoder.encode("total_harga", "UTF-8") + "=" +
                                URLEncoder.encode(totalHarga, "UTF-8") + "&" +
                                URLEncoder.encode("items", "UTF-8") + "=" +
                                URLEncoder.encode(cartItemsJson, "UTF-8");

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
                if (conn != null) {
                    conn.disconnect();
                }
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
                    Toast.makeText(
                            StoreDetailActivity.this,
                            jsonObject.optString("message", "Gagal memproses pesanan"),
                            Toast.LENGTH_LONG
                    ).show();
                }

            } catch (Exception e) {
                Toast.makeText(StoreDetailActivity.this, "Response tidak valid", Toast.LENGTH_SHORT).show();
            }
        }
    }

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

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" +
                                URLEncoder.encode("tambah_ke_keranjang_server", "UTF-8") + "&" +
                                URLEncoder.encode("user_id", "UTF-8") + "=" +
                                URLEncoder.encode(String.valueOf(userId), "UTF-8") + "&" +
                                URLEncoder.encode("restoran_id", "UTF-8") + "=" +
                                URLEncoder.encode(String.valueOf(restoranId), "UTF-8") + "&" +
                                URLEncoder.encode("items", "UTF-8") + "=" +
                                URLEncoder.encode(itemsJson, "UTF-8");

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
                if (conn != null) {
                    conn.disconnect();
                }
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