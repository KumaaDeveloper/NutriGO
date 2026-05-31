package com.example.pedulimakanan;

import android.app.Activity;
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

public class TransactionDetailActivity extends Activity {

    private static final String CONNECTOR_URL = "http://172.104.183.200/pedulimakanan/connector.php";
    private static final int BIAYA_ONGKIR = 6000;

    private LinearLayout llItems;
    private TextView tvItemCountBadge;
    private TextView tvDetailOngkir, tvDetailTotal, tvDetailPromo;
    private LinearLayout llPromoRow;

    private int totalHargaIntent = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        int transaksiId = getIntent().getIntExtra("transaksi_id", -1);
        String restoranNama = getIntent().getStringExtra("restoran_nama");
        totalHargaIntent = getIntent().getIntExtra("total_harga", 0);
        String tanggal = getIntent().getStringExtra("tanggal");

        findViewById(R.id.btnBackDetail).setOnClickListener(v -> finish());

        // Header info
        ((TextView) findViewById(R.id.tvDetailNamaResto)).setText(restoranNama);
        ((TextView) findViewById(R.id.tvDetailTanggal)).setText(formatTanggal(tanggal));
        ((TextView) findViewById(R.id.tvDetailOrderId)).setText("Order ID: #BCSHA" + transaksiId);
        ((TextView) findViewById(R.id.tvDetailStatus)).setText("Paid");
        ((ImageView) findViewById(R.id.imgDetailResto)).setImageResource(getRestoImage(restoranNama));

        llItems = findViewById(R.id.llDetailItems);
        tvItemCountBadge = findViewById(R.id.tvDetailItemCount);
        tvDetailOngkir = findViewById(R.id.tvDetailOngkir);
        tvDetailTotal = findViewById(R.id.tvDetailTotal);
        tvDetailPromo = findViewById(R.id.tvDetailPromo);
        llPromoRow = findViewById(R.id.llDetailPromoRow);

        findViewById(R.id.btnPesanUlang).setOnClickListener(v -> finish());

        new LoadTransactionDetailTask().execute(String.valueOf(transaksiId));
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

    // ── ASYNCTASK: Mengambil List Menu Nota Pembelian dari Server API ──
    private class LoadTransactionDetailTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("get_detail_transaksi_server", "UTF-8") + "&" +
                        URLEncoder.encode("transaksi_id", "UTF-8") + "=" + URLEncoder.encode(params[0], "UTF-8");

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
                    llItems.removeAllViews();
                    JSONArray itemsArray = jsonObject.getJSONArray("detail");

                    int subtotal = 0;
                    int itemCount = 0;

                    for (int i = 0; i < itemsArray.length(); i++) {
                        JSONObject obj = itemsArray.getJSONObject(i);
                        String nama = obj.getString("nama_item");
                        int harga = obj.getInt("harga_saat_ini");
                        int qty = obj.getInt("jumlah");

                        subtotal += (harga * qty);
                        itemCount += qty;

                        View row = LayoutInflater.from(TransactionDetailActivity.this).inflate(R.layout.item_detail_row, llItems, false);
                        ((TextView) row.findViewById(R.id.tvDetailItemNama)).setText(qty > 1 ? nama + " x" + qty : nama);
                        ((TextView) row.findViewById(R.id.tvDetailItemHarga)).setText("Rp " + formatRupiah(harga * qty));
                        llItems.addView(row);
                    }

                    tvItemCountBadge.setText(itemCount + " Item");
                    tvDetailOriginalPriceCalculation(subtotal);
                }
            } catch (Exception ignored) {}
        }
    }

    private void tvDetailOriginalPriceCalculation(int subtotal) {
        int discount = (subtotal + BIAYA_ONGKIR) - totalHargaIntent;
        if (discount < 0) discount = 0;

        tvDetailOngkir.setText("Rp " + formatRupiah(BIAYA_ONGKIR));
        tvDetailTotal.setText("Rp " + formatRupiah(totalHargaIntent));

        if (discount > 0) {
            llPromoRow.setVisibility(View.VISIBLE);
            tvDetailPromo.setText("- Rp " + formatRupiah(discount));
        } else {
            llPromoRow.setVisibility(View.GONE);
        }
    }
}