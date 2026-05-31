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
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends Activity {

    private static final String CONNECTOR_URL = "http://172.104.183.200/pedulimakanan/connector.php";
    private static final String PREF_NAME = "login_session";

    private int userId;
    private LinearLayout llCartContainer;
    private TextView tvCartTotal;
    private TextView tvSaldoInfo;

    private final List<Integer> selectedCartIds = new ArrayList<>();
    private int totalHargaTerpilih = 0;
    private int restoranIdAktif = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        userId = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getInt("user_id", -1);

        llCartContainer = findViewById(R.id.llCartContainer);
        tvCartTotal     = findViewById(R.id.tvCartTotal);
        tvSaldoInfo     = findViewById(R.id.tvSaldoInfo);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnCheckout = findViewById(R.id.btnCheckoutCart);
        if (btnCheckout != null) btnCheckout.setOnClickListener(v -> prosesCheckout());

        new LoadCartDataTask().execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new LoadCartDataTask().execute();
    }

    private void applyLocalCalculation(JSONArray cartArray) {
        totalHargaTerpilih = 0;
        try {
            for (int i = 0; i < cartArray.length(); i++) {
                JSONObject obj = cartArray.getJSONObject(i);
                int cartId = obj.getInt("id");
                if (selectedCartIds.contains(cartId)) {
                    totalHargaTerpilih += (obj.getInt("harga") * obj.getInt("jumlah"));
                }
            }
        } catch (Exception ignored) {}
        tvCartTotal.setText("Rp " + formatRupiah(totalHargaTerpilih));
    }

    private void prosesCheckout() {
        if (selectedCartIds.isEmpty()) {
            Toast.makeText(this, "Pilih item yang ingin dibayar!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ubah list ID terpilih menjadi JSONArray string
        JSONArray selectedArray = new JSONArray();
        for (int id : selectedCartIds) {
            selectedArray.put(id);
        }

        new CheckoutCartTask().execute(selectedArray.toString(), String.valueOf(totalHargaTerpilih));
    }

    private String formatRupiah(int amount) {
        return String.format("%,d", amount).replace(',', '.');
    }

    // ── ASYNCTASK: Membaca List Item Keranjang & Saldo dari Cloud Server ──
    private class LoadCartDataTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("get_keranjang_server", "UTF-8") + "&" +
                        URLEncoder.encode("user_id", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(userId), "UTF-8");

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
                    int saldo = jsonObject.optInt("saldo", 0);
                    tvSaldoInfo.setText("Saldo Anda: Rp " + formatRupiah(saldo));

                    llCartContainer.removeAllViews();
                    JSONArray cartArray = jsonObject.getJSONArray("keranjang");

                    if (cartArray.length() == 0) {
                        tvCartTotal.setText("Rp 0");
                        return;
                    }

                    for (int i = 0; i < cartArray.length(); i++) {
                        JSONObject obj = cartArray.getJSONObject(i);
                        int cartId = obj.getInt("id");
                        String nama = obj.getString("nama_item");
                        int harga = obj.getInt("harga");
                        int jumlah = obj.getInt("jumlah");
                        int subtotalItem = harga * jumlah;
                        restoranIdAktif = obj.getInt("restoran_id");

                        View row = LayoutInflater.from(CartActivity.this).inflate(R.layout.item_cart_row, llCartContainer, false);
                        ((TextView) row.findViewById(R.id.tvCartItemNama)).setText(nama);
                        ((TextView) row.findViewById(R.id.tvCartItemHarga)).setText("Harga: Rp " + formatRupiah(harga));
                        ((TextView) row.findViewById(R.id.tvCartItemQty)).setText("x" + jumlah);
                        ((TextView) row.findViewById(R.id.tvCartItemSubtotal)).setText("Total: Rp " + formatRupiah(subtotalItem));

                        CheckBox cb = row.findViewById(R.id.cbCartItem);
                        cb.setChecked(selectedCartIds.contains(cartId));

                        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if (isChecked) {
                                if (!selectedCartIds.contains(cartId)) selectedCartIds.add(cartId);
                            } else {
                                selectedCartIds.remove(Integer.valueOf(cartId));
                            }
                            applyLocalCalculation(cartArray);
                        });

                        row.findViewById(R.id.btnHapusItem).setOnClickListener(v ->
                                new DeleteCartItemTask().execute(String.valueOf(cartId))
                        );
                        llCartContainer.addView(row);
                    }
                    applyLocalCalculation(cartArray);
                }
            } catch (Exception ignored) {}
        }
    }

    // ── ASYNCTASK: Menghapus Item Keranjang dari Server ──
    private class DeleteCartItemTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("hapus_item_keranjang_server", "UTF-8") + "&" +
                        URLEncoder.encode("cart_id", "UTF-8") + "=" + URLEncoder.encode(params[0], "UTF-8");

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(postData);
                writer.flush();
                writer.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = reader.readLine();
                reader.close();
                return new JSONObject(response).optBoolean("success", false);
            } catch (Exception e) {
                return false;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                new LoadCartDataTask().execute();
            }
        }
    }

    // ── ASYNCTASK: Memproses Checkout & Validasi Potong Saldo ke Server ──
    private class CheckoutCartTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String cartIdsJson = params[0];
            String totalHarga = params[1];
            HttpURLConnection conn = null;
            try {
                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("checkout_keranjang_server", "UTF-8") + "&" +
                        URLEncoder.encode("user_id", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(userId), "UTF-8") + "&" +
                        URLEncoder.encode("restoran_id", "UTF-8") + "=" + URLEncoder.encode(String.valueOf(restoranIdAktif), "UTF-8") + "&" +
                        URLEncoder.encode("total_harga", "UTF-8") + "=" + URLEncoder.encode(totalHarga, "UTF-8") + "&" +
                        URLEncoder.encode("cart_ids", "UTF-8") + "=" + URLEncoder.encode(cartIdsJson, "UTF-8");

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
                Toast.makeText(CartActivity.this, "Koneksi checkout gagal", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.optBoolean("success", false)) {
                    Toast.makeText(CartActivity.this, "Pembayaran Berhasil!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(CartActivity.this, TransactionActivity.class));
                    finish();
                } else {
                    Toast.makeText(CartActivity.this, jsonObject.optString("message", "Gagal checkout"), Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(CartActivity.this, "Response server error", Toast.LENGTH_SHORT).show();
            }
        }
    }
}