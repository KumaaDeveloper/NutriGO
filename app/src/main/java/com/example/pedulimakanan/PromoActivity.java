package com.example.pedulimakanan;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;
import java.util.Locale;

public class PromoActivity extends Activity {

    private String restoranNama;
    private int subtotal;
    private int ongkir;

    private EditText etKodePromo;
    private LinearLayout llFoundPromo;
    private LinearLayout llPromoList;
    private TextView tvSummaryPromo;
    private TextView tvSummaryDiskon;
    private TextView tvSummaryOngkir;
    private TextView tvSummaryTotal;
    private Button btnTerapkanPromo;

    private Promo selectedRegularPromo = null;
    private Promo selectedPrivatePromo = null;
    private Promo foundPromo = null;

    private final int GREEN = Color.parseColor("#4CAF50");
    private final int GREY = Color.parseColor("#9E9E9E");
    private final int BLACK = Color.parseColor("#000000");
    private final int WHITE = Color.parseColor("#FFFFFF");
    private final int BLUE = Color.parseColor("#246E9B");
    private final int RED = Color.parseColor("#F44336");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promo);

        restoranNama = getIntent().getStringExtra("restoran_nama");
        subtotal = getIntent().getIntExtra("subtotal", 0);
        ongkir = getIntent().getIntExtra("ongkir", 6000);

        if (restoranNama == null) {
            restoranNama = "";
        }

        findViewById(R.id.btnBackPromo).setOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.tvPromoTitle)).setText("Promo");
        ((TextView) findViewById(R.id.tvPromoSubtitle)).setText(restoranNama);
        ((TextView) findViewById(R.id.tvPromoSubtotal)).setText("Subtotal: Rp " + formatRupiah(subtotal));

        etKodePromo = findViewById(R.id.etKodePromo);
        llFoundPromo = findViewById(R.id.llFoundPromo);
        llPromoList = findViewById(R.id.llPromoList);
        tvSummaryPromo = findViewById(R.id.tvSummaryPromo);
        tvSummaryDiskon = findViewById(R.id.tvSummaryDiskon);
        tvSummaryOngkir = findViewById(R.id.tvSummaryOngkir);
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal);
        btnTerapkanPromo = findViewById(R.id.btnTerapkanPromo);

        findViewById(R.id.btnCekKodePromo).setOnClickListener(v -> checkPromoCode());
        btnTerapkanPromo.setOnClickListener(v -> applySelectedPromo());

        renderPromos();
        updateSummary();
    }

    private void checkPromoCode() {
        String code = etKodePromo.getText().toString().trim().toUpperCase(Locale.US);

        if (code.isEmpty()) {
            Toast.makeText(this, "Masukkan kode promo", Toast.LENGTH_SHORT).show();
            return;
        }

        Promo promo = findPromoByCode(code);

        if (promo == null) {
            foundPromo = null;
            Toast.makeText(this, "Kode promo tidak ditemukan", Toast.LENGTH_SHORT).show();
        } else {
            foundPromo = promo;
            Toast.makeText(this, "Kode promo ditemukan", Toast.LENGTH_SHORT).show();
        }

        renderPromos();
        updateSummary();
    }

    private Promo findPromoByCode(String code) {
        for (Promo p : getPrivatePromos()) {
            if (p.code.equalsIgnoreCase(code)) {
                return p;
            }
        }

        for (Promo p : getRestaurantPromos(restoranNama)) {
            if (p.code.equalsIgnoreCase(code)) {
                return p;
            }
        }

        return null;
    }

    private void renderPromos() {
        llFoundPromo.removeAllViews();
        llPromoList.removeAllViews();

        if (foundPromo != null) {
            llFoundPromo.addView(createSmallTitle("Promo ditemukan"));
            llFoundPromo.addView(createPromoBox(foundPromo));
            llFoundPromo.setVisibility(View.VISIBLE);
        } else {
            llFoundPromo.setVisibility(View.GONE);
        }

        llPromoList.addView(createSmallTitle("Promo untuk " + restoranNama));

        ArrayList<Promo> promos = getRestaurantPromos(restoranNama);

        if (promos.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada promo untuk restoran ini.");
            empty.setTextColor(Color.parseColor("#777777"));
            empty.setTextSize(13);
            empty.setPadding(0, dp(10), 0, dp(10));
            llPromoList.addView(empty);
        } else {
            for (Promo promo : promos) {
                llPromoList.addView(createPromoBox(promo));
            }
        }
    }

    private TextView createSmallTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(BLACK);
        tv.setTextSize(14);
        tv.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, dp(12), 0, dp(8));
        tv.setLayoutParams(lp);

        return tv;
    }

    private View createPromoBox(Promo promo) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.setBackground(makeRounded(WHITE, dp(18), Color.parseColor("#EEEEEE"), 1));

        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        boxLp.setMargins(0, 0, 0, dp(12));
        box.setLayoutParams(boxLp);

        LinearLayout leftCol = new LinearLayout(this);
        leftCol.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        leftCol.setLayoutParams(leftLp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(promo.title.toUpperCase(Locale.US));
        tvTitle.setTextColor(BLACK);
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setMaxLines(2);
        leftCol.addView(tvTitle);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(getRequirementText(promo));
        tvStatus.setTextColor(isEligible(promo) ? GREEN : RED);
        tvStatus.setTextSize(12);
        tvStatus.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusLp.setMargins(0, dp(5), 0, 0);
        tvStatus.setLayoutParams(statusLp);
        leftCol.addView(tvStatus);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(promo.description);
        tvDesc.setTextColor(Color.parseColor("#666666"));
        tvDesc.setTextSize(12);
        tvDesc.setMaxLines(2);

        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descLp.setMargins(0, dp(6), 0, 0);
        tvDesc.setLayoutParams(descLp);
        leftCol.addView(tvDesc);

        box.addView(leftCol);

        TextView btnUse = new TextView(this);
        btnUse.setText(getButtonText(promo));
        btnUse.setGravity(Gravity.CENTER);
        btnUse.setTextSize(12);
        btnUse.setTypeface(null, Typeface.BOLD);

        boolean eligible = isEligible(promo);
        boolean selected = isSelected(promo);

        if (selected) {
            btnUse.setTextColor(WHITE);
            btnUse.setBackground(makeRounded(GREEN, dp(26), GREEN, 1));
        } else if (eligible) {
            btnUse.setTextColor(WHITE);
            btnUse.setBackground(makeRounded(BLUE, dp(26), BLUE, 1));
        } else {
            btnUse.setTextColor(WHITE);
            btnUse.setBackground(makeRounded(GREY, dp(26), GREY, 1));
        }

        btnUse.setEnabled(eligible);

        LinearLayout.LayoutParams useLp = new LinearLayout.LayoutParams(dp(86), dp(36));
        useLp.setMargins(dp(10), 0, 0, 0);
        btnUse.setLayoutParams(useLp);

        btnUse.setOnClickListener(v -> {
            if (!isEligible(promo)) {
                Toast.makeText(this, "Promo belum memenuhi syarat", Toast.LENGTH_SHORT).show();
                return;
            }

            if (promo.isPrivate) {
                if (selectedPrivatePromo != null && selectedPrivatePromo.code.equalsIgnoreCase(promo.code)) {
                    selectedPrivatePromo = null;
                } else {
                    selectedPrivatePromo = promo;
                }
            } else {
                if (selectedRegularPromo != null && selectedRegularPromo.code.equalsIgnoreCase(promo.code)) {
                    selectedRegularPromo = null;
                } else {
                    selectedRegularPromo = promo;
                }
            }

            renderPromos();
            updateSummary();
        });

        box.addView(btnUse);

        return box;
    }

    private boolean isSelected(Promo promo) {
        if (promo.isPrivate) {
            return selectedPrivatePromo != null && selectedPrivatePromo.code.equalsIgnoreCase(promo.code);
        }

        return selectedRegularPromo != null && selectedRegularPromo.code.equalsIgnoreCase(promo.code);
    }

    private String getButtonText(Promo promo) {
        return isSelected(promo) ? "Dipilih" : "Gunakan";
    }

    private String getRequirementText(Promo promo) {
        if (subtotal <= 0) {
            return "Pilih menu terlebih dahulu";
        }

        if (promo.minSubtotal > 0) {
            if (subtotal >= promo.minSubtotal) {
                return "Syarat terpenuhi";
            }

            return "Min. belanja Rp " + formatRupiah(promo.minSubtotal);
        }

        return "Bisa digunakan";
    }

    private boolean isEligible(Promo promo) {
        if (subtotal <= 0) {
            return false;
        }

        return subtotal >= promo.minSubtotal;
    }

    private void updateSummary() {
        int discount = 0;
        boolean freeOngkir = false;
        String promoText = "";

        if (selectedRegularPromo != null) {
            discount += calculateDiscount(selectedRegularPromo);
            freeOngkir = freeOngkir || selectedRegularPromo.freeOngkir;
            promoText = selectedRegularPromo.code;
        }

        if (selectedPrivatePromo != null) {
            discount += calculateDiscount(selectedPrivatePromo);
            freeOngkir = freeOngkir || selectedPrivatePromo.freeOngkir;

            if (promoText.trim().isEmpty()) {
                promoText = selectedPrivatePromo.code;
            } else {
                promoText = promoText + " + " + selectedPrivatePromo.code;
            }
        }

        if (discount > subtotal) {
            discount = subtotal;
        }

        int ongkirFinal = freeOngkir ? 0 : ongkir;
        int total = Math.max(0, subtotal + ongkirFinal - discount);

        if (promoText.trim().isEmpty()) {
            tvSummaryPromo.setText("-");
            tvSummaryDiskon.setText("Rp 0");
            tvSummaryOngkir.setText("Rp " + formatRupiah(ongkir));
            tvSummaryTotal.setText("Rp " + formatRupiah(subtotal + ongkir));

            btnTerapkanPromo.setEnabled(false);
            btnTerapkanPromo.setBackgroundTintList(ColorStateList.valueOf(GREY));
        } else {
            tvSummaryPromo.setText(promoText);
            tvSummaryDiskon.setText("- Rp " + formatRupiah(discount));

            if (freeOngkir) {
                tvSummaryOngkir.setText("Gratis");
                tvSummaryOngkir.setTextColor(GREEN);
            } else {
                tvSummaryOngkir.setText("Rp " + formatRupiah(ongkir));
                tvSummaryOngkir.setTextColor(Color.parseColor("#333333"));
            }

            tvSummaryTotal.setText("Rp " + formatRupiah(total));

            btnTerapkanPromo.setEnabled(true);
            btnTerapkanPromo.setBackgroundTintList(ColorStateList.valueOf(GREEN));
        }
    }

    private void applySelectedPromo() {
        int discount = 0;
        boolean freeOngkir = false;
        String promoText = "";

        if (selectedRegularPromo != null) {
            discount += calculateDiscount(selectedRegularPromo);
            freeOngkir = freeOngkir || selectedRegularPromo.freeOngkir;
            promoText = selectedRegularPromo.code;
        }

        if (selectedPrivatePromo != null) {
            discount += calculateDiscount(selectedPrivatePromo);
            freeOngkir = freeOngkir || selectedPrivatePromo.freeOngkir;

            if (promoText.trim().isEmpty()) {
                promoText = selectedPrivatePromo.code;
            } else {
                promoText = promoText + " + " + selectedPrivatePromo.code;
            }
        }

        if (discount > subtotal) {
            discount = subtotal;
        }

        Intent data = new Intent();
        data.putExtra("discount", discount);
        data.putExtra("free_ongkir", freeOngkir);
        data.putExtra("promo_text", promoText);

        setResult(RESULT_OK, data);
        finish();
    }

    private int calculateDiscount(Promo promo) {
        if (!isEligible(promo)) {
            return 0;
        }

        if (promo.freeOngkir) {
            return 0;
        }

        if (promo.type == Promo.TYPE_FIXED) {
            return promo.amount;
        }

        if (promo.type == Promo.TYPE_PERCENT) {
            int raw = (int) Math.floor(subtotal * (promo.percent / 100.0));
            return Math.min(raw, promo.maxDiscount);
        }

        return 0;
    }

    private ArrayList<Promo> getPrivatePromos() {
        ArrayList<Promo> list = new ArrayList<>();

        list.add(new Promo("HEMAT10", "Diskon 10.000 Khusus Kamu", "Potongan langsung Rp10.000", Promo.TYPE_FIXED, 10000, 0, 0, 0, false, true));
        list.add(new Promo("ONGKIRGRATIS", "Gratis Ongkir Khusus Kamu", "Biaya ongkir menjadi gratis", Promo.TYPE_FIXED, 0, 0, 0, 0, true, true));
        list.add(new Promo("NUTRIGOBARU", "Diskon 20% Khusus Pengguna Baru", "Diskon 20% maksimal Rp20.000", Promo.TYPE_PERCENT, 0, 20, 20000, 0, false, true));

        return list;
    }

    private ArrayList<Promo> getRestaurantPromos(String resto) {
        ArrayList<Promo> list = new ArrayList<>();
        String r = resto == null ? "" : resto.toLowerCase(Locale.US).trim();

        if (r.equals("saladstop")) {
            list.add(fixed("FRESH5K", "Diskon 5.000 Khusus Saladstop", "Potongan Rp5.000", 5000, 0));
            list.add(fixed("SALAD10K", "Diskon 10.000 Khusus Saladstop", "Potongan Rp10.000 minimal belanja Rp80.000", 10000, 80000));
            list.add(percent("HEALTHY12", "Diskon 12% Khusus Saladstop", "Diskon 12% maksimal Rp15.000", 12, 15000, 0));
        } else if (r.equals("supergrain")) {
            list.add(fixed("GRAIN5K", "Diskon 5.000 Khusus Supergrain", "Potongan Rp5.000", 5000, 0));
            list.add(fixed("SUPER8K", "Diskon 8.000 Khusus Supergrain", "Potongan Rp8.000 minimal belanja Rp70.000", 8000, 70000));
            list.add(percent("LUNCH10", "Diskon 10% Khusus Supergrain", "Diskon 10% maksimal Rp12.000", 10, 12000, 0));
        } else if (r.equals("burgreen")) {
            list.add(fixed("VEG5K", "Diskon 5.000 Khusus Burgreen", "Potongan Rp5.000", 5000, 0));
            list.add(fixed("GREEN7K", "Diskon 7.000 Khusus Burgreen", "Potongan Rp7.000", 7000, 0));
            list.add(percent("BURGREEN10", "Diskon 10% Khusus Burgreen", "Diskon 10% maksimal Rp10.000", 10, 10000, 0));
            list.add(fixed("HEMAT12K", "Diskon 12.000 Khusus Burgreen", "Potongan Rp12.000 minimal belanja Rp120.000", 12000, 120000));
        } else if (r.equals("greenbowl")) {
            list.add(fixed("BOWL5K", "Diskon 5.000 Khusus GreenBowl", "Potongan Rp5.000", 5000, 0));
            list.add(fixed("GREEN8K", "Diskon 8.000 Khusus GreenBowl", "Potongan Rp8.000 minimal belanja Rp75.000", 8000, 75000));
            list.add(percent("FRESH10", "Diskon 10% Khusus GreenBowl", "Diskon 10% maksimal Rp10.000", 10, 10000, 0));
        } else if (r.equals("smoothiebar")) {
            list.add(fixed("SMOOTH5K", "Diskon 5.000 Khusus SmoothieBar", "Potongan Rp5.000", 5000, 0));
            list.add(fixed("BOOST8K", "Diskon 8.000 Khusus SmoothieBar", "Potongan Rp8.000", 8000, 0));
            list.add(percent("DRINK10", "Diskon 10% Khusus SmoothieBar", "Diskon 10% maksimal Rp10.000", 10, 10000, 0));
            list.add(fixed("SHAKE12K", "Diskon 12.000 Khusus SmoothieBar", "Potongan Rp12.000 minimal belanja Rp100.000", 12000, 100000));
        } else if (r.equals("freshbox")) {
            list.add(fixed("BOX5K", "Diskon 5.000 Khusus FreshBox", "Potongan Rp5.000", 5000, 0));
            list.add(fixed("FRESH7K", "Diskon 7.000 Khusus FreshBox", "Potongan Rp7.000", 7000, 0));
            list.add(fixed("MEAL10K", "Diskon 10.000 Khusus FreshBox", "Potongan Rp10.000 minimal belanja Rp90.000", 10000, 90000));
        } else if (r.equals("nutrisnack")) {
            list.add(fixed("NUTRI5K", "Diskon 5.000 Khusus NutriSnack", "Potongan Rp5.000", 5000, 0));
            list.add(fixed("SNACK7K", "Diskon 7.000 Khusus NutriSnack", "Potongan Rp7.000", 7000, 0));
            list.add(percent("NUTRI10", "Diskon 10% Khusus NutriSnack", "Diskon 10% maksimal Rp10.000", 10, 10000, 0));
        }

        return list;
    }

    private Promo fixed(String code, String title, String desc, int amount, int minSubtotal) {
        return new Promo(code, title, desc, Promo.TYPE_FIXED, amount, 0, 0, minSubtotal, false, false);
    }

    private Promo percent(String code, String title, String desc, int percent, int maxDiscount, int minSubtotal) {
        return new Promo(code, title, desc, Promo.TYPE_PERCENT, 0, percent, maxDiscount, minSubtotal, false, false);
    }

    private GradientDrawable makeRounded(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private String formatRupiah(int amount) {
        return String.format("%,d", amount).replace(',', '.');
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    static class Promo {
        static final int TYPE_FIXED = 1;
        static final int TYPE_PERCENT = 2;

        String code;
        String title;
        String description;
        int type;
        int amount;
        int percent;
        int maxDiscount;
        int minSubtotal;
        boolean freeOngkir;
        boolean isPrivate;

        Promo(String code, String title, String description, int type,
              int amount, int percent, int maxDiscount, int minSubtotal,
              boolean freeOngkir, boolean isPrivate) {
            this.code = code;
            this.title = title;
            this.description = description;
            this.type = type;
            this.amount = amount;
            this.percent = percent;
            this.maxDiscount = maxDiscount;
            this.minSubtotal = minSubtotal;
            this.freeOngkir = freeOngkir;
            this.isPrivate = isPrivate;
        }
    }
}