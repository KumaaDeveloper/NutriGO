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
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class TransactionActivity extends Activity {

    private static final String CONNECTOR_URL = "http://139.162.46.52/pedulimakanan/connector.php";
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
            case "nutrisnack":
                return R.drawable.img_nutri_snack;
            case "smoothiebar":
                return R.drawable.img_smoothie_bar;
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
            String[] bulan = {"", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des"};
            return parts[2] + " " + bulan[Integer.parseInt(parts[1])] + " " + parts[0];
        } catch (Exception e) {
            return raw;
        }
    }

    private String normalizeRestoName(String name) {
        if (name == null) {
            return "";
        }

        String n = name.toLowerCase(Locale.US).trim().replace(" ", "");

        if (n.equals("saladstop") || n.equals("saladtop")) return "Saladstop";
        if (n.equals("burgreen")) return "Burgreen";
        if (n.equals("supergrain")) return "Supergrain";
        if (n.equals("greenbowl")) return "GreenBowl";
        if (n.equals("freshbox")) return "FreshBox";
        if (n.equals("nutrisnack")) return "NutriSnack";
        if (n.equals("smoothiebar")) return "SmoothieBar";

        return name.trim();
    }

    private ArrayList<String> getRestoNamesFromTransaction(JSONObject obj) {
        ArrayList<String> result = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();

        try {
            JSONArray list = obj.optJSONArray("nama_resto_list");

            if (list != null) {
                for (int i = 0; i < list.length(); i++) {
                    String name = list.optString(i, "").trim();

                    if (!name.isEmpty() && !name.equalsIgnoreCase("null")) {
                        unique.add(normalizeRestoName(name));
                    }
                }
            }

            JSONArray restoranArray = obj.optJSONArray("restoran");

            if (restoranArray != null) {
                for (int i = 0; i < restoranArray.length(); i++) {
                    JSONObject r = restoranArray.getJSONObject(i);
                    String name = r.optString("nama_resto", "").trim();

                    if (!name.isEmpty() && !name.equalsIgnoreCase("null")) {
                        unique.add(normalizeRestoName(name));
                    }
                }
            }

            String raw = obj.optString("nama_resto", "").trim();

            if (!raw.isEmpty() && !raw.equalsIgnoreCase("null")) {
                String[] parts = raw.split("\\s*\\+\\s*");

                for (String p : parts) {
                    if (!p.trim().isEmpty()) {
                        unique.add(normalizeRestoName(p.trim()));
                    }
                }
            }

        } catch (Exception ignored) {
        }

        result.addAll(unique);
        return result;
    }

    private String joinRestoNamesForDisplay(ArrayList<String> names) {
        if (names == null || names.isEmpty()) {
            return "Restoran";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                if (i % 4 == 0) {
                    sb.append("\n");
                } else {
                    sb.append(" + ");
                }
            }

            sb.append(names.get(i));
        }

        return sb.toString();
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
                        int total = obj.getInt("total_harga");
                        String tanggal = obj.getString("tanggal");
                        String status = obj.optString("status_pesanan", "Selesai");
                        String promoKode = obj.optString("promo_kode", "");
                        int promoDiskon = obj.optInt("promo_diskon", 0);

                        ArrayList<String> restoNames = getRestoNamesFromTransaction(obj);
                        String namaRestoDisplay = joinRestoNamesForDisplay(restoNames);
                        String firstResto = restoNames.isEmpty() ? obj.optString("nama_resto", "") : restoNames.get(0);

                        View row = LayoutInflater.from(TransactionActivity.this)
                                .inflate(R.layout.item_transaction_row, llTransaksiContainer, false);

                        TextView tvNamaResto = row.findViewById(R.id.tvTransNamaResto);
                        TextView tvTotal = row.findViewById(R.id.tvTransTotal);
                        TextView tvTanggal = row.findViewById(R.id.tvTransTanggal);
                        TextView tvStatus = row.findViewById(R.id.tvTransStatus);
                        ImageView imgResto = row.findViewById(R.id.imgTransResto);

                        tvNamaResto.setText(namaRestoDisplay);
                        tvNamaResto.setSingleLine(false);
                        tvNamaResto.setMaxLines(8);

                        tvTotal.setText("Rp " + formatRupiah(total));
                        tvTanggal.setText(formatTanggal(tanggal));
                        tvStatus.setText(status);
                        imgResto.setImageResource(getRestoImage(firstResto));

                        row.findViewById(R.id.btnTransDetail).setOnClickListener(v -> {
                            Intent intent = new Intent(TransactionActivity.this, TransactionDetailActivity.class);
                            intent.putExtra("transaksi_id", transaksiId);
                            intent.putExtra("restoran_nama", namaRestoDisplay);
                            intent.putStringArrayListExtra("restoran_nama_list", restoNames);
                            intent.putExtra("total_harga", total);
                            intent.putExtra("tanggal", tanggal);
                            intent.putExtra("promo_kode", promoKode);
                            intent.putExtra("promo_diskon", promoDiskon);
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