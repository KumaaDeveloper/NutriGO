package com.example.pedulimakanan;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
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
import android.content.Intent;

public class TransactionDetailActivity extends Activity {

    private static final String CONNECTOR_URL = "http://139.162.46.52/pedulimakanan/connector.php";
    private static final int DEFAULT_ONGKIR = 6000;

    private LinearLayout llItems;
    private LinearLayout llRestoRatingContainer;
    private LinearLayout llDetailPesananContent;

    private TextView tvItemCountBadge;
    private TextView tvDetailOngkir;
    private TextView tvDetailTotal;
    private TextView tvDetailPromo;
    private TextView tvDetailPromoCode;
    private TextView tvToggleDetailPesanan;

    private LinearLayout llPromoRow;
    private LinearLayout llPromoCodeRow;

    private int totalHargaIntent = 0;
    private int transaksiId = -1;
    private int promoDiskonIntent = 0;
    private String promoKodeIntent = "";
    private boolean detailPesananExpanded = true;

    private final int COLOR_STAR_ACTIVE = Color.parseColor("#FFC107");
    private final int COLOR_STAR_INACTIVE = Color.parseColor("#BDBDBD");
    private final int GREEN = Color.parseColor("#4CAF50");
    private final int GREY = Color.parseColor("#9E9E9E");
    private final int BLACK = Color.parseColor("#000000");
    private final int WHITE = Color.parseColor("#FFFFFF");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        transaksiId = getIntent().getIntExtra("transaksi_id", -1);
        String restoranNama = getIntent().getStringExtra("restoran_nama");
        totalHargaIntent = getIntent().getIntExtra("total_harga", 0);
        String tanggal = getIntent().getStringExtra("tanggal");
        promoKodeIntent = getIntent().getStringExtra("promo_kode");
        promoDiskonIntent = getIntent().getIntExtra("promo_diskon", 0);

        if (promoKodeIntent == null) {
            promoKodeIntent = "";
        }

        ArrayList<String> restoListIntent = getIntent().getStringArrayListExtra("restoran_nama_list");

        findViewById(R.id.btnBackDetail).setOnClickListener(v -> finish());

        TextView tvNamaResto = findViewById(R.id.tvDetailNamaResto);
        tvNamaResto.setText(formatRestoNamesForDisplay(restoListIntent, restoranNama));
        tvNamaResto.setSingleLine(false);
        tvNamaResto.setMaxLines(8);

        ((TextView) findViewById(R.id.tvDetailTanggal)).setText(formatTanggal(tanggal));
        ((TextView) findViewById(R.id.tvDetailOrderId)).setText("Order ID: #BCSHA" + transaksiId);
        ((TextView) findViewById(R.id.tvDetailStatus)).setText("Paid");

        String firstResto = getFirstRestoName(restoListIntent, restoranNama);
        ((ImageView) findViewById(R.id.imgDetailResto)).setImageResource(getRestoImage(firstResto));

        llItems = findViewById(R.id.llDetailItems);
        llRestoRatingContainer = findViewById(R.id.llRestoRatingContainer);
        llDetailPesananContent = findViewById(R.id.llDetailPesananContent);

        tvItemCountBadge = findViewById(R.id.tvDetailItemCount);
        tvDetailOngkir = findViewById(R.id.tvDetailOngkir);
        tvDetailTotal = findViewById(R.id.tvDetailTotal);
        tvDetailPromo = findViewById(R.id.tvDetailPromo);
        tvDetailPromoCode = findViewById(R.id.tvDetailPromoCode);
        tvToggleDetailPesanan = findViewById(R.id.tvToggleDetailPesanan);

        llPromoRow = findViewById(R.id.llDetailPromoRow);
        llPromoCodeRow = findViewById(R.id.llDetailPromoCodeRow);

        findViewById(R.id.detailPesananPanel).setOnClickListener(v -> toggleDetailPesanan());
        findViewById(R.id.layoutHeaderDetailPesanan).setOnClickListener(v -> toggleDetailPesanan());
        findViewById(R.id.llDetailPesananContent).setOnClickListener(v -> toggleDetailPesanan());
        findViewById(R.id.btnPesanUlang).setOnClickListener(v -> {
            Intent intent = new Intent(TransactionDetailActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        updateDetailPesananToggle();

        new LoadTransactionDetailTask().execute(String.valueOf(transaksiId));
    }

    private void toggleDetailPesanan() {
        detailPesananExpanded = !detailPesananExpanded;
        updateDetailPesananToggle();
    }

    private void updateDetailPesananToggle() {
        if (llDetailPesananContent == null || tvToggleDetailPesanan == null) {
            return;
        }

        if (detailPesananExpanded) {
            llDetailPesananContent.setVisibility(View.VISIBLE);
            tvToggleDetailPesanan.setText("−");
        } else {
            llDetailPesananContent.setVisibility(View.GONE);
            tvToggleDetailPesanan.setText("+");
        }
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

    private String getFirstRestoName(ArrayList<String> list, String fallback) {
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }

        if (fallback == null) {
            return "";
        }

        String[] parts = fallback.split("\\s*\\+\\s*");
        return parts.length > 0 ? parts[0].trim() : fallback;
    }

    private String formatRestoNamesForDisplay(ArrayList<String> names, String fallback) {
        ArrayList<String> clean = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();

        if (names != null) {
            for (String n : names) {
                if (n != null && !n.trim().isEmpty()) {
                    unique.add(normalizeRestoName(n));
                }
            }
        }

        if (unique.isEmpty() && fallback != null) {
            String[] parts = fallback.split("\\s*\\+\\s*");

            for (String p : parts) {
                if (!p.trim().isEmpty()) {
                    unique.add(normalizeRestoName(p.trim()));
                }
            }
        }

        clean.addAll(unique);

        if (clean.isEmpty()) {
            return "Restoran";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < clean.size(); i++) {
            if (i > 0) {
                if (i % 4 == 0) {
                    sb.append("\n");
                } else {
                    sb.append(" + ");
                }
            }

            sb.append(clean.get(i));
        }

        return sb.toString();
    }

    private String formatPromoTextForDisplay(String rawPromoText) {
        if (rawPromoText == null || rawPromoText.trim().isEmpty()) {
            return "-";
        }

        String[] codes = rawPromoText.split("\\s*\\+\\s*");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < codes.length; i++) {
            if (i > 0) {
                if (i % 3 == 0) {
                    sb.append("\n");
                } else {
                    sb.append(" + ");
                }
            }

            sb.append(codes[i].trim());
        }

        return sb.toString();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable makeRounded(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private ArrayList<RestoRatingModel> parseRestoRatingModels(JSONObject jsonObject, JSONArray itemsArray) {
        ArrayList<RestoRatingModel> result = new ArrayList<>();
        Set<String> usedNames = new LinkedHashSet<>();

        try {
            JSONArray restoArray = jsonObject.optJSONArray("restoran");

            if (restoArray != null) {
                for (int i = 0; i < restoArray.length(); i++) {
                    JSONObject r = restoArray.getJSONObject(i);

                    int restoranId = r.optInt("restoran_id", r.optInt("id", 0));
                    String namaResto = normalizeRestoName(r.optString("nama_resto", r.optString("nama", "")));
                    int rating = r.optInt("rating_pembeli", 0);

                    if (!namaResto.isEmpty() && !usedNames.contains(namaResto)) {
                        usedNames.add(namaResto);

                        RestoRatingModel model = new RestoRatingModel();
                        model.restoranId = restoranId;
                        model.namaResto = namaResto;
                        model.savedRating = rating;
                        model.selectedRating = rating;
                        result.add(model);
                    }
                }
            }

            if (result.isEmpty() && itemsArray != null) {
                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject item = itemsArray.getJSONObject(i);

                    int restoranId = item.optInt("restoran_id", 0);
                    String namaResto = normalizeRestoName(item.optString("nama_resto", ""));

                    if (!namaResto.isEmpty() && !usedNames.contains(namaResto)) {
                        usedNames.add(namaResto);

                        RestoRatingModel model = new RestoRatingModel();
                        model.restoranId = restoranId;
                        model.namaResto = namaResto;
                        model.savedRating = item.optInt("rating_pembeli", 0);
                        model.selectedRating = model.savedRating;
                        result.add(model);
                    }
                }
            }

            if (result.isEmpty()) {
                ArrayList<String> intentList = getIntent().getStringArrayListExtra("restoran_nama_list");
                String fallback = getIntent().getStringExtra("restoran_nama");

                Set<String> names = new LinkedHashSet<>();

                if (intentList != null) {
                    for (String n : intentList) {
                        if (n != null && !n.trim().isEmpty()) {
                            names.add(normalizeRestoName(n));
                        }
                    }
                }

                if (names.isEmpty() && fallback != null) {
                    String[] parts = fallback.split("\\s*\\+\\s*");

                    for (String p : parts) {
                        if (!p.trim().isEmpty()) {
                            names.add(normalizeRestoName(p.trim()));
                        }
                    }
                }

                int oldRating = jsonObject.optInt("rating_pembeli", 0);

                for (String n : names) {
                    RestoRatingModel model = new RestoRatingModel();
                    model.restoranId = 0;
                    model.namaResto = n;
                    model.savedRating = oldRating;
                    model.selectedRating = oldRating;
                    result.add(model);
                }
            }

        } catch (Exception ignored) {
        }

        return result;
    }

    private void renderRatingBoxes(ArrayList<RestoRatingModel> models) {
        llRestoRatingContainer.removeAllViews();

        if (models == null || models.isEmpty()) {
            return;
        }

        TextView title = new TextView(this);
        title.setText("Rating Restoran");
        title.setTextColor(BLACK);
        title.setTextSize(15);
        title.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleLp.setMargins(0, dp(14), 0, dp(8));
        title.setLayoutParams(titleLp);

        llRestoRatingContainer.addView(title);

        for (RestoRatingModel model : models) {
            llRestoRatingContainer.addView(createRatingBox(model));
        }
    }

    private View createRatingBox(RestoRatingModel model) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.setBackground(makeRounded(WHITE, dp(16), Color.parseColor("#EEEEEE"), 1));

        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        boxLp.setMargins(0, 0, 0, dp(10));
        box.setLayoutParams(boxLp);

        TextView tvResto = new TextView(this);
        tvResto.setText(model.namaResto);
        tvResto.setTextColor(BLACK);
        tvResto.setTextSize(14);
        tvResto.setTypeface(null, Typeface.BOLD);
        box.addView(tvResto);

        TextView tvInfo = new TextView(this);
        tvInfo.setText(model.savedRating > 0
                ? "Rating Anda: " + model.savedRating + " dari 5"
                : "Pilih bintang untuk memberi rating");
        tvInfo.setTextColor(Color.parseColor("#666666"));
        tvInfo.setTextSize(12);

        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoLp.setMargins(0, dp(6), 0, 0);
        tvInfo.setLayoutParams(infoLp);

        box.addView(tvInfo);

        LinearLayout starsRow = new LinearLayout(this);
        starsRow.setOrientation(LinearLayout.HORIZONTAL);
        starsRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams starsLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        starsLp.setMargins(0, dp(12), 0, dp(12));
        starsRow.setLayoutParams(starsLp);

        model.stars.clear();

        for (int i = 1; i <= 5; i++) {
            ImageView star = new ImageView(this);
            star.setImageResource(R.drawable.ic_star);

            LinearLayout.LayoutParams starLp = new LinearLayout.LayoutParams(dp(32), dp(32));
            starLp.setMargins(0, 0, dp(8), 0);
            star.setLayoutParams(starLp);

            int ratingValue = i;

            star.setOnClickListener(v -> {
                if (model.savedRating > 0) {
                    Toast.makeText(this, "Rating untuk " + model.namaResto + " sudah tersimpan", Toast.LENGTH_SHORT).show();
                    return;
                }

                model.selectedRating = ratingValue;
                tvInfo.setText("Anda memberi rating " + model.selectedRating + " dari 5");
                updateStarsForModel(model);
                updateRatingButton(model);
            });

            model.stars.add(star);
            starsRow.addView(star);
        }

        box.addView(starsRow);

        Button btnSubmit = new Button(this);
        btnSubmit.setText("Konfirmasi Rating");
        btnSubmit.setAllCaps(false);
        btnSubmit.setTextColor(WHITE);
        btnSubmit.setTextSize(13);
        btnSubmit.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42)
        );
        btnSubmit.setLayoutParams(btnLp);

        model.btnSubmit = btnSubmit;

        btnSubmit.setOnClickListener(v -> {
            if (model.savedRating > 0) {
                Toast.makeText(this, "Rating sudah diberikan", Toast.LENGTH_SHORT).show();
                return;
            }

            if (model.selectedRating < 1 || model.selectedRating > 5) {
                Toast.makeText(this, "Pilih rating terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }

            new SubmitRatingTask(model).execute(
                    String.valueOf(transaksiId),
                    String.valueOf(model.restoranId),
                    String.valueOf(model.selectedRating)
            );
        });

        box.addView(btnSubmit);

        updateStarsForModel(model);
        updateRatingButton(model);

        return box;
    }

    private void updateStarsForModel(RestoRatingModel model) {
        for (int i = 0; i < model.stars.size(); i++) {
            ImageView star = model.stars.get(i);

            if (i < model.selectedRating) {
                star.setColorFilter(COLOR_STAR_ACTIVE);
            } else {
                star.setColorFilter(COLOR_STAR_INACTIVE);
            }

            boolean clickable = model.savedRating <= 0;
            star.setEnabled(clickable);
            star.setAlpha(clickable ? 1.0f : 0.75f);
        }
    }

    private void updateRatingButton(RestoRatingModel model) {
        if (model.btnSubmit == null) {
            return;
        }

        if (model.savedRating > 0) {
            model.btnSubmit.setEnabled(false);
            model.btnSubmit.setText("Rating Sudah Tersimpan");
            model.btnSubmit.setBackgroundTintList(ColorStateList.valueOf(GREY));
            return;
        }

        if (model.selectedRating > 0) {
            model.btnSubmit.setEnabled(true);
            model.btnSubmit.setText("Konfirmasi Rating");
            model.btnSubmit.setBackgroundTintList(ColorStateList.valueOf(GREEN));
        } else {
            model.btnSubmit.setEnabled(false);
            model.btnSubmit.setText("Konfirmasi Rating");
            model.btnSubmit.setBackgroundTintList(ColorStateList.valueOf(GREY));
        }
    }

    private void updateDetailPaymentSummary(int subtotal, int ongkir, int promoDiskon, String promoKode) {
        if (ongkir <= 0) {
            ongkir = DEFAULT_ONGKIR;
        }

        if (promoDiskon < 0) {
            promoDiskon = 0;
        }

        if (promoDiskon > subtotal + ongkir) {
            promoDiskon = subtotal + ongkir;
        }

        tvDetailOngkir.setText("Rp " + formatRupiah(ongkir));
        tvDetailTotal.setText("Rp " + formatRupiah(totalHargaIntent));

        llPromoCodeRow.setVisibility(View.VISIBLE);
        llPromoRow.setVisibility(View.VISIBLE);

        if (promoKode != null && !promoKode.trim().isEmpty()) {
            tvDetailPromoCode.setText(formatPromoTextForDisplay(promoKode));
            tvDetailPromoCode.setTextColor(Color.parseColor("#246E9B"));
            tvDetailPromoCode.setSingleLine(false);
            tvDetailPromoCode.setMaxLines(10);
            tvDetailPromoCode.setGravity(Gravity.END);
        } else {
            tvDetailPromoCode.setText("-");
            tvDetailPromoCode.setTextColor(Color.parseColor("#555555"));
            tvDetailPromoCode.setGravity(Gravity.END);
        }

        if (promoDiskon > 0) {
            tvDetailPromo.setText("- Rp " + formatRupiah(promoDiskon));
            tvDetailPromo.setTextColor(GREEN);
        } else {
            tvDetailPromo.setText("Rp 0");
            tvDetailPromo.setTextColor(Color.parseColor("#555555"));
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

                    if (jsonObject.optInt("total_harga", 0) > 0) {
                        totalHargaIntent = jsonObject.optInt("total_harga", totalHargaIntent);
                    }

                    JSONArray itemsArray = jsonObject.getJSONArray("detail");

                    int subtotal = 0;
                    int itemCount = 0;

                    for (int i = 0; i < itemsArray.length(); i++) {
                        JSONObject obj = itemsArray.getJSONObject(i);

                        String nama = obj.getString("nama_item");
                        int harga = obj.getInt("harga_saat_ini");
                        int qty = obj.getInt("jumlah");
                        String namaResto = normalizeRestoName(obj.optString("nama_resto", ""));

                        subtotal += harga * qty;
                        itemCount += qty;

                        View row = LayoutInflater.from(TransactionDetailActivity.this)
                                .inflate(R.layout.item_detail_row, llItems, false);

                        TextView tvNamaItem = row.findViewById(R.id.tvDetailItemNama);
                        TextView tvHargaItem = row.findViewById(R.id.tvDetailItemHarga);

                        if (!namaResto.isEmpty()) {
                            tvNamaItem.setText(qty > 1 ? nama + " x" + qty + "\n" + namaResto : nama + "\n" + namaResto);
                        } else {
                            tvNamaItem.setText(qty > 1 ? nama + " x" + qty : nama);
                        }

                        tvHargaItem.setText("Rp " + formatRupiah(harga * qty));

                        llItems.addView(row);
                    }

                    tvItemCountBadge.setText(itemCount + " Item");

                    int ongkir = jsonObject.optInt("ongkir", DEFAULT_ONGKIR);

                    if (ongkir <= 0) {
                        ongkir = DEFAULT_ONGKIR;
                    }

                    int promoDiskon = jsonObject.optInt("promo_diskon", promoDiskonIntent);
                    String promoKode = jsonObject.optString("promo_kode", promoKodeIntent);

                    if ((promoKode == null || promoKode.trim().isEmpty()) && promoKodeIntent != null) {
                        promoKode = promoKodeIntent;
                    }

                    updateDetailPaymentSummary(subtotal, ongkir, promoDiskon, promoKode);

                    ArrayList<RestoRatingModel> models = parseRestoRatingModels(jsonObject, itemsArray);
                    renderRatingBoxes(models);

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

        private final RestoRatingModel model;

        SubmitRatingTask(RestoRatingModel model) {
            this.model = model;
        }

        @Override
        protected String doInBackground(String... params) {
            String transaksiIdParam = params[0];
            String restoranIdParam = params[1];
            String ratingParam = params[2];

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
                                URLEncoder.encode(transaksiIdParam, "UTF-8") + "&" +
                                URLEncoder.encode("restoran_id", "UTF-8") + "=" +
                                URLEncoder.encode(restoranIdParam, "UTF-8") + "&" +
                                URLEncoder.encode("rating", "UTF-8") + "=" +
                                URLEncoder.encode(ratingParam, "UTF-8");

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
                    Toast.makeText(TransactionDetailActivity.this, "Rating " + model.namaResto + " berhasil disimpan", Toast.LENGTH_SHORT).show();

                    model.savedRating = model.selectedRating;
                    updateStarsForModel(model);
                    updateRatingButton(model);

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

    static class RestoRatingModel {
        int restoranId = 0;
        String namaResto = "";
        int selectedRating = 0;
        int savedRating = 0;
        ArrayList<ImageView> stars = new ArrayList<>();
        Button btnSubmit;
    }
}