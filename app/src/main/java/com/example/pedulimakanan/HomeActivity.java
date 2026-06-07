package com.example.pedulimakanan;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class HomeActivity extends Activity {

    private static final String CONNECTOR_URL = "http://172.104.183.200/pedulimakanan/connector.php";
    private static final String PREF_NAME = "login_session";

    private int userId;

    private RecyclerView rvStores;
    private RestaurantAdapter storeAdapter;
    private List<RestoranModel> allStores = new ArrayList<>();

    private String activeCategory = "Semua";
    private String searchQuery = "";

    private TextView chipSemua;
    private TextView chipMakanan;
    private TextView chipMinuman;
    private TextView chipSnack;

    private View topupSheet;
    private View dimOverlay;
    private TextView tvSheetSaldo;
    private EditText etCustomAmount;
    private TextView tvSaldo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        String namaUser = prefs.getString("nama", "");

        if (userId == -1) {
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
            return;
        }

        TextView tvGreeting = findViewById(R.id.tvGreeting);

        if (!namaUser.isEmpty()) {
            tvGreeting.setText("Halo, " + namaUser + "!\nMau Makan Apa Hari Ini?");
        }

        tvSaldo = findViewById(R.id.tvSaldo);

        findViewById(R.id.btnTambahSaldo).setOnClickListener(v -> showTopupSheet());

        EditText etSearch = findViewById(R.id.etSearchHome);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        chipSemua = findViewById(R.id.chipSemua);
        chipMakanan = findViewById(R.id.chipMakanan);
        chipMinuman = findViewById(R.id.chipMinuman);
        chipSnack = findViewById(R.id.chipSnack);

        chipSemua.setOnClickListener(v -> setCategory("Semua"));
        chipMakanan.setOnClickListener(v -> setCategory("Makanan"));
        chipMinuman.setOnClickListener(v -> setCategory("Minuman"));
        chipSnack.setOnClickListener(v -> setCategory("Snack"));

        updateChipStyles();

        rvStores = findViewById(R.id.rvStores);
        rvStores.setLayoutManager(new GridLayoutManager(this, 2));
        rvStores.setNestedScrollingEnabled(false);
        rvStores.setHasFixedSize(false);
        rvStores.setOverScrollMode(View.OVER_SCROLL_NEVER);

        storeAdapter = new RestaurantAdapter(
                HomeActivity.this,
                new ArrayList<RestoranModel>(),
                this::openStore
        );

        rvStores.setAdapter(storeAdapter);

        setupBottomNav();
        setupTopupSheet();

        new LoadHomeDataTask().execute();
    }

    @Override
    protected void onResume() {
        super.onResume();

        new LoadHomeDataTask().execute();

        if (storeAdapter != null) {
            storeAdapter.notifyDataSetChanged();
        }
    }

    private void setCategory(String category) {
        activeCategory = category;
        updateChipStyles();
        applyFilter();
    }

    private void updateChipStyles() {
        setChipActive(chipSemua, activeCategory.equals("Semua"));
        setChipActive(chipMakanan, activeCategory.equals("Makanan"));
        setChipActive(chipMinuman, activeCategory.equals("Minuman"));
        setChipActive(chipSnack, activeCategory.equals("Snack"));
    }

    private void setChipActive(TextView chip, boolean active) {
        if (active) {
            chip.setBackgroundResource(R.drawable.bg_chip_active);
            chip.setTextColor(0xFFFFFFFF);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_inactive);
            chip.setTextColor(0xFF246E9B);
        }
    }

    private void applyFilter() {
        List<RestoranModel> filtered = new ArrayList<>();

        for (RestoranModel r : allStores) {
            boolean matchCategory = activeCategory.equals("Semua")
                    || r.tipeMenu.equalsIgnoreCase(activeCategory);

            boolean matchSearch = r.namaResto.toLowerCase()
                    .contains(searchQuery.toLowerCase());

            if (matchCategory && matchSearch) {
                filtered.add(r);
            }
        }

        if (storeAdapter != null) {
            storeAdapter.updateData(filtered);
        }

        TextView tvEmpty = findViewById(R.id.tvEmptyStores);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openStore(RestoranModel store) {
        Intent intent = new Intent(HomeActivity.this, StoreDetailActivity.class);
        intent.putExtra("restoran_id", store.id);
        intent.putExtra("restoran_nama", store.namaResto);
        intent.putExtra("restoran_rating", store.rating);
        intent.putExtra("restoran_alamat", store.alamatResto);
        startActivity(intent);
    }

    private void setupTopupSheet() {
        topupSheet = findViewById(R.id.topupSheet);
        dimOverlay = findViewById(R.id.topupDimOverlay);
        tvSheetSaldo = findViewById(R.id.tvSheetSaldo);
        etCustomAmount = findViewById(R.id.etTopupCustom);

        dimOverlay.setOnClickListener(v -> hideTopupSheet());
        findViewById(R.id.btnTopupClose).setOnClickListener(v -> hideTopupSheet());

        int[] presets = {
                10000,
                25000,
                50000,
                100000,
                200000,
                500000
        };

        int[] btnIds = {
                R.id.btnTopup10,
                R.id.btnTopup25,
                R.id.btnTopup50,
                R.id.btnTopup100,
                R.id.btnTopup200,
                R.id.btnTopup500
        };

        for (int i = 0; i < btnIds.length; i++) {
            final int amount = presets[i];
            findViewById(btnIds[i]).setOnClickListener(v -> executeTopupServer(amount));
        }

        findViewById(R.id.btnTopupCustomConfirm).setOnClickListener(v -> {
            String val = etCustomAmount.getText().toString().trim();

            if (val.isEmpty()) {
                Toast.makeText(HomeActivity.this, "Masukkan nominal", Toast.LENGTH_SHORT).show();
                return;
            }

            int amount;

            try {
                amount = Integer.parseInt(val);
            } catch (Exception e) {
                Toast.makeText(HomeActivity.this, "Nominal tidak valid", Toast.LENGTH_SHORT).show();
                return;
            }

            if (amount < 1000) {
                Toast.makeText(HomeActivity.this, "Minimal top up Rp 1.000", Toast.LENGTH_SHORT).show();
                return;
            }

            executeTopupServer(amount);
        });
    }

    private void showTopupSheet() {
        dimOverlay.setVisibility(View.VISIBLE);
        topupSheet.setVisibility(View.VISIBLE);

        topupSheet.setTranslationY(
                topupSheet.getHeight() > 0 ? topupSheet.getHeight() : 1200f
        );

        ObjectAnimator.ofFloat(topupSheet, "translationY", 0f)
                .setDuration(300)
                .start();
    }

    private void hideTopupSheet() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(topupSheet, "translationY", 1200f);
        anim.setDuration(250);

        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                topupSheet.setVisibility(View.GONE);
                dimOverlay.setVisibility(View.GONE);
            }
        });

        anim.start();
    }

    private void executeTopupServer(int amount) {
        new TopupTask().execute(String.valueOf(amount));
    }

    private void setupBottomNav() {
        findViewById(R.id.layoutNavHome).setOnClickListener(v -> {
            // Sudah berada di halaman Home
        });

        findViewById(R.id.layoutNavFavorit).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, FavoriteActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.layoutNavCart).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CartActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.layoutNavTransaksi).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TransactionActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.layoutNavProfile).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });
    }

    private String fmt(int n) {
        return String.format("%,d", n).replace(',', '.');
    }

    private class LoadHomeDataTask extends AsyncTask<Void, Void, String> {

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
                                URLEncoder.encode("get_home_data", "UTF-8") + "&" +
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
                return;
            }

            try {
                JSONObject jsonObject = new JSONObject(response);

                if (jsonObject.optBoolean("success", false)) {
                    int saldo = jsonObject.optInt("saldo", 0);

                    tvSaldo.setText("Rp " + fmt(saldo));
                    tvSheetSaldo.setText("Saldo saat ini: Rp " + fmt(saldo));

                    allStores.clear();

                    JSONArray storesArray = jsonObject.getJSONArray("restoran");

                    for (int i = 0; i < storesArray.length(); i++) {
                        JSONObject obj = storesArray.getJSONObject(i);

                        RestoranModel r = new RestoranModel();
                        r.id = obj.getInt("id");
                        r.namaResto = obj.getString("nama_resto");
                        r.alamatResto = obj.optString("alamat_resto", "");
                        r.kategori = obj.optString("kategori", "");
                        r.rating = (float) obj.optDouble("rating", 0.0);
                        r.terjual = obj.optInt("terjual", 0);
                        r.gambarUrl = obj.optString("gambar_url", "");
                        r.tipeMenu = obj.optString("tipe_menu", "Makanan");

                        allStores.add(r);
                    }

                    applyFilter();
                }

            } catch (Exception ignored) {
            }
        }
    }

    private class TopupTask extends AsyncTask<String, Void, String> {

        private int addedAmount = 0;

        @Override
        protected String doInBackground(String... params) {
            addedAmount = Integer.parseInt(params[0]);
            HttpURLConnection conn = null;

            try {
                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" +
                                URLEncoder.encode("topup_saldo", "UTF-8") + "&" +
                                URLEncoder.encode("user_id", "UTF-8") + "=" +
                                URLEncoder.encode(String.valueOf(userId), "UTF-8") + "&" +
                                URLEncoder.encode("amount", "UTF-8") + "=" +
                                URLEncoder.encode(String.valueOf(addedAmount), "UTF-8");

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
                Toast.makeText(HomeActivity.this, "Koneksi server gagal", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject jsonObject = new JSONObject(response);

                if (jsonObject.optBoolean("success", false)) {
                    Toast.makeText(
                            HomeActivity.this,
                            "Top Up Rp " + fmt(addedAmount) + " berhasil!",
                            Toast.LENGTH_SHORT
                    ).show();

                    hideTopupSheet();
                    new LoadHomeDataTask().execute();

                } else {
                    Toast.makeText(
                            HomeActivity.this,
                            jsonObject.optString("message", "Top up gagal"),
                            Toast.LENGTH_SHORT
                    ).show();
                }

            } catch (Exception e) {
                Toast.makeText(HomeActivity.this, "Gagal memproses data server", Toast.LENGTH_SHORT).show();
            }
        }
    }
}