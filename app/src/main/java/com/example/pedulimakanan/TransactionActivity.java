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

public class TransactionActivity extends Activity {

    private static final String CONNECTOR_URL = "http://172.104.183.200/pedulimakanan/connector.php";
    private static final String PREF_NAME = "login_session";

    private int userId;
    private LinearLayout llTransaksiContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        llTransaksiContainer = findViewById(R.id.llTransaksiContainer);

        View btnBack = findViewById(R.id.btnBackTransaction);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        setupBottomNavigation();
        new LoadTransactionsTask().execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new LoadTransactionsTask().execute();
    }

    private void setupBottomNavigation() {
        LinearLayout layoutNavHome = findViewById(R.id.layoutNavHome);
        LinearLayout layoutNavFavorit = findViewById(R.id.layoutNavFavorit);
        LinearLayout layoutNavCart = findViewById(R.id.layoutNavCart);
        LinearLayout layoutNavTransaksi = findViewById(R.id.layoutNavTransaksi);
        LinearLayout layoutNavProfile = findViewById(R.id.layoutNavProfile);

        layoutNavHome.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        layoutNavFavorit.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionActivity.this, FavoriteActivity.class);
            startActivity(intent);
            finish();
        });

        layoutNavCart.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionActivity.this, CartActivity.class);
            startActivity(intent);
            finish();
        });

        layoutNavTransaksi.setOnClickListener(v -> {
            // Sudah berada di halaman transaksi
        });

        layoutNavProfile.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionActivity.this, ProfileActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
            finish();
        });
    }

    private int getRestoImage(String nama) {
        if (nama == null) {
            return R.drawable.img_placeholder_food;
        }

        switch (nama.toLowerCase().trim()) {
            case "saladstop":
                return R.drawable.img_saladstop;

            case "burgreen":
                return R.drawable.img_burgreen;

            case "supergrain":
                return R.drawable.img_supergrain;

            case "greenbowl":
                return R.drawable.img_greenbowl;

            case "freshbox":
                return R.drawable.img_freshbox;

            default:
                return R.drawable.img_placeholder_food;
        }
    }

    private String formatRupiah(int amount) {
        return String.format("%,d", amount).replace(',', '.');
    }

    private String formatTanggal(String raw) {
        try {
            String[] parts = raw.split(" ")[0].split("-");
            String[] bulan = {
                    "",
                    "Jan",
                    "Feb",
                    "Mar",
                    "Apr",
                    "Mei",
                    "Jun",
                    "Jul",
                    "Agu",
                    "Sep",
                    "Okt",
                    "Nov",
                    "Des"
            };

            return parts[2] + " " + bulan[Integer.parseInt(parts[1])] + " " + parts[0];

        } catch (Exception e) {
            return raw;
        }
    }

    private void showEmptyTransactionBox() {
        llTransaksiContainer.removeAllViews();

        View emptyView = LayoutInflater.from(this)
                .inflate(R.layout.item_empty_transaction, llTransaksiContainer, false);

        Button btnMulaiBelanja = emptyView.findViewById(R.id.btnMulaiBelanjaTransaction);

        btnMulaiBelanja.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        llTransaksiContainer.addView(emptyView);
    }

    private class LoadTransactionsTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            if (userId == -1) {
                return null;
            }

            HttpURLConnection conn = null;

            try {
                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" +
                                URLEncoder.encode("get_transaksi_server", "UTF-8") + "&" +
                                URLEncoder.encode("user_id", "UTF-8") + "=" +
                                URLEncoder.encode(String.valueOf(userId), "UTF-8");

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
                showEmptyTransactionBox();
                return;
            }

            try {
                JSONObject jsonObject = new JSONObject(response);

                if (jsonObject.optBoolean("success", false)) {
                    llTransaksiContainer.removeAllViews();

                    JSONArray transArray = jsonObject.getJSONArray("transaksi");

                    if (transArray.length() == 0) {
                        showEmptyTransactionBox();
                        return;
                    }

                    for (int i = 0; i < transArray.length(); i++) {
                        JSONObject obj = transArray.getJSONObject(i);

                        int transaksiId = obj.getInt("id");
                        String namaResto = obj.getString("nama_resto");
                        int total = obj.getInt("total_harga");
                        String tanggal = obj.getString("tanggal");
                        String status = obj.optString("status_pesanan", "Selesai");

                        View row = LayoutInflater.from(TransactionActivity.this)
                                .inflate(R.layout.item_transaction_row, llTransaksiContainer, false);

                        TextView tvNamaResto = row.findViewById(R.id.tvTransNamaResto);
                        TextView tvTotal = row.findViewById(R.id.tvTransTotal);
                        TextView tvTanggal = row.findViewById(R.id.tvTransTanggal);
                        TextView tvStatus = row.findViewById(R.id.tvTransStatus);
                        ImageView imgResto = row.findViewById(R.id.imgTransResto);

                        tvNamaResto.setText(namaResto);
                        tvTotal.setText("Rp " + formatRupiah(total));
                        tvTanggal.setText(formatTanggal(tanggal));
                        tvStatus.setText(status);
                        imgResto.setImageResource(getRestoImage(namaResto));

                        row.findViewById(R.id.btnTransDetail).setOnClickListener(v -> {
                            Intent intent = new Intent(TransactionActivity.this, TransactionDetailActivity.class);
                            intent.putExtra("transaksi_id", transaksiId);
                            intent.putExtra("restoran_nama", namaResto);
                            intent.putExtra("total_harga", total);
                            intent.putExtra("tanggal", tanggal);
                            startActivity(intent);
                        });

                        llTransaksiContainer.addView(row);
                    }

                } else {
                    showEmptyTransactionBox();
                }

            } catch (Exception e) {
                showEmptyTransactionBox();
            }
        }
    }
}