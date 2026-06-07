package com.example.pedulimakanan;

import android.app.Activity;
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

public class TransactionDetailActivity extends Activity {

    private static final String CONNECTOR_URL = "http://172.104.183.200/pedulimakanan/connector.php";
    private static final int BIAYA_ONGKIR = 6000;

    private LinearLayout llItems;
    private TextView tvItemCountBadge;
    private TextView tvDetailOngkir;
    private TextView tvDetailTotal;
    private TextView tvDetailPromo;
    private LinearLayout llPromoRow;

    private Button btnBeriRating;
    private LinearLayout llRatingBox;
    private LinearLayout llRatingStars;
    private TextView tvRatingInfo;
    private Button btnKonfirmasiRating;

    private ImageView star1;
    private ImageView star2;
    private ImageView star3;
    private ImageView star4;
    private ImageView star5;

    private int totalHargaIntent = 0;
    private int transaksiId = -1;
    private int selectedRating = 0;
    private int savedRating = 0;

    private final int COLOR_STAR_ACTIVE = Color.parseColor("#FFC107");
    private final int COLOR_STAR_INACTIVE = Color.parseColor("#BDBDBD");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        transaksiId = getIntent().getIntExtra("transaksi_id", -1);
        String restoranNama = getIntent().getStringExtra("restoran_nama");
        totalHargaIntent = getIntent().getIntExtra("total_harga", 0);
        String tanggal = getIntent().getStringExtra("tanggal");

        findViewById(R.id.btnBackDetail).setOnClickListener(v -> finish());

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

        btnBeriRating = findViewById(R.id.btnBeriRating);
        llRatingBox = findViewById(R.id.llRatingBox);
        llRatingStars = findViewById(R.id.llRatingStars);
        tvRatingInfo = findViewById(R.id.tvRatingInfo);
        btnKonfirmasiRating = findViewById(R.id.btnKonfirmasiRating);

        star1 = findViewById(R.id.starRating1);
        star2 = findViewById(R.id.starRating2);
        star3 = findViewById(R.id.starRating3);
        star4 = findViewById(R.id.starRating4);
        star5 = findViewById(R.id.starRating5);

        setupRatingUi();

        findViewById(R.id.btnPesanUlang).setOnClickListener(v -> finish());

        new LoadTransactionDetailTask().execute(String.valueOf(transaksiId));
    }

    private void setupRatingUi() {
        llRatingBox.setVisibility(View.GONE);
        selectedRating = 0;
        savedRating = 0;

        updateStars(0, true);
        updateConfirmButton();

        btnBeriRating.setOnClickListener(v -> {
            if (savedRating > 0) {
                Toast.makeText(this, "Rating sudah diberikan dan tidak bisa diubah", Toast.LENGTH_SHORT).show();
                return;
            }

            llRatingBox.setVisibility(
                    llRatingBox.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
            );
        });

        star1.setOnClickListener(v -> chooseRating(1));
        star2.setOnClickListener(v -> chooseRating(2));
        star3.setOnClickListener(v -> chooseRating(3));
        star4.setOnClickListener(v -> chooseRating(4));
        star5.setOnClickListener(v -> chooseRating(5));

        btnKonfirmasiRating.setOnClickListener(v -> {
            if (savedRating > 0) {
                Toast.makeText(this, "Rating sudah diberikan", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedRating < 1 || selectedRating > 5) {
                Toast.makeText(this, "Pilih rating terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }

            new SubmitRatingTask().execute(
                    String.valueOf(transaksiId),
                    String.valueOf(selectedRating)
            );
        });
    }

    private void chooseRating(int rating) {
        if (savedRating > 0) {
            Toast.makeText(this, "Rating sudah tersimpan dan tidak bisa diubah", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedRating = rating;
        updateStars(selectedRating, true);
        tvRatingInfo.setText("Anda memberi rating " + selectedRating + " dari 5");
        updateConfirmButton();
    }

    private void updateStars(int rating, boolean clickable) {
        ImageView[] stars = {star1, star2, star3, star4, star5};

        for (int i = 0; i < stars.length; i++) {
            if (i < rating) {
                stars[i].setColorFilter(COLOR_STAR_ACTIVE);
            } else {
                stars[i].setColorFilter(COLOR_STAR_INACTIVE);
            }

            stars[i].setEnabled(clickable);
            stars[i].setAlpha(clickable ? 1.0f : 0.8f);
        }
    }

    private void updateConfirmButton() {
        if (savedRating > 0) {
            btnKonfirmasiRating.setEnabled(false);
            btnKonfirmasiRating.setText("Rating Sudah Tersimpan");
            btnKonfirmasiRating.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            return;
        }

        if (selectedRating <= 0) {
            btnKonfirmasiRating.setEnabled(false);
            btnKonfirmasiRating.setText("Konfirmasi Rating");
            btnKonfirmasiRating.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
        } else {
            btnKonfirmasiRating.setEnabled(true);
            btnKonfirmasiRating.setText("Konfirmasi Rating");
            btnKonfirmasiRating.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }
    }

    private void applySavedRating(int rating) {
        savedRating = rating;
        selectedRating = rating;

        if (savedRating > 0) {
            llRatingBox.setVisibility(View.VISIBLE);
            updateStars(savedRating, false);
            tvRatingInfo.setText("Rating Anda: " + savedRating + " dari 5");
            btnBeriRating.setText("Rating Sudah Diberikan");
            btnBeriRating.setEnabled(false);
            btnBeriRating.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
        } else {
            updateStars(0, true);
            tvRatingInfo.setText("Pilih bintang untuk memberi rating");
            btnBeriRating.setText("Beri Rating");
            btnBeriRating.setEnabled(true);
            btnBeriRating.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFC107")));
        }

        updateConfirmButton();
    }

    private int getRestoImage(String nama) {
        if (nama == null) return R.drawable.img_placeholder_food;

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
            int m = Integer.parseInt(parts[1]);
            return parts[2] + " " + bulan[m] + " " + parts[0];
        } catch (Exception e) {
            return raw;
        }
    }

    private class LoadTransactionDetailTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection conn = null;

            try {
                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" +
                                URLEncoder.encode("get_detail_transaksi_server", "UTF-8") + "&" +
                                URLEncoder.encode("transaksi_id", "UTF-8") + "=" +
                                URLEncoder.encode(params[0], "UTF-8");

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
                Toast.makeText(TransactionDetailActivity.this, "Gagal memuat detail transaksi", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject jsonObject = new JSONObject(response);

                if (jsonObject.optBoolean("success", false)) {
                    llItems.removeAllViews();

                    int ratingPembeli = jsonObject.optInt("rating_pembeli", 0);
                    applySavedRating(ratingPembeli);

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

                        View row = LayoutInflater.from(TransactionDetailActivity.this)
                                .inflate(R.layout.item_detail_row, llItems, false);

                        ((TextView) row.findViewById(R.id.tvDetailItemNama))
                                .setText(qty > 1 ? nama + " x" + qty : nama);

                        ((TextView) row.findViewById(R.id.tvDetailItemHarga))
                                .setText("Rp " + formatRupiah(harga * qty));

                        llItems.addView(row);
                    }

                    tvItemCountBadge.setText(itemCount + " Item");
                    tvDetailOriginalPriceCalculation(subtotal);
                } else {
                    Toast.makeText(
                            TransactionDetailActivity.this,
                            jsonObject.optString("message", "Gagal memuat detail transaksi"),
                            Toast.LENGTH_SHORT
                    ).show();
                }

            } catch (Exception e) {
                Toast.makeText(TransactionDetailActivity.this, "Response server error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class SubmitRatingTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection conn = null;

            try {
                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" +
                                URLEncoder.encode("submit_rating_transaksi", "UTF-8") + "&" +
                                URLEncoder.encode("transaksi_id", "UTF-8") + "=" +
                                URLEncoder.encode(params[0], "UTF-8") + "&" +
                                URLEncoder.encode("rating", "UTF-8") + "=" +
                                URLEncoder.encode(params[1], "UTF-8");

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
                Toast.makeText(TransactionDetailActivity.this, "Koneksi rating gagal", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject jsonObject = new JSONObject(response);

                if (jsonObject.optBoolean("success", false)) {
                    Toast.makeText(TransactionDetailActivity.this, "Rating berhasil disimpan", Toast.LENGTH_SHORT).show();
                    applySavedRating(selectedRating);
                } else {
                    Toast.makeText(
                            TransactionDetailActivity.this,
                            jsonObject.optString("message", "Gagal menyimpan rating"),
                            Toast.LENGTH_LONG
                    ).show();

                    new LoadTransactionDetailTask().execute(String.valueOf(transaksiId));
                }

            } catch (Exception e) {
                Toast.makeText(TransactionDetailActivity.this, "Response server error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void tvDetailOriginalPriceCalculation(int subtotal) {
        int discount = (subtotal + BIAYA_ONGKIR) - totalHargaIntent;

        if (discount < 0) {
            discount = 0;
        }

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