package com.example.pedulimakanan;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity {

    private DatabaseHelper db;
    private int userId;

    // Store list
    private RecyclerView rvStores;
    private RestaurantAdapter storeAdapter;
    private List<RestoranModel> allStores = new ArrayList<>();
    private String activeCategory = "Semua"; // "Semua" | "Makanan" | "Minuman" | "Snack"
    private String searchQuery = "";

    // Category chip views
    private TextView chipSemua, chipMakanan, chipMinuman, chipSnack;

    // Top-up sheet
    private View topupSheet, dimOverlay;
    private TextView tvSheetSaldo;
    private EditText etCustomAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = new DatabaseHelper(this);

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        String namaUser = prefs.getString("nama", "");

        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Greeting
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        if (!namaUser.isEmpty()) {
            tvGreeting.setText("Halo, " + namaUser + "!\nMau Makan Apa Hari Ini?");
        }

        // Top Up button
        findViewById(R.id.btnTambahSaldo).setOnClickListener(v -> showTopupSheet());

        // Search
        EditText etSearch = findViewById(R.id.etSearchHome);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                searchQuery = s.toString();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Category chips
        chipSemua   = findViewById(R.id.chipSemua);
        chipMakanan = findViewById(R.id.chipMakanan);
        chipMinuman = findViewById(R.id.chipMinuman);
        chipSnack   = findViewById(R.id.chipSnack);

        chipSemua.setOnClickListener(v   -> setCategory("Semua"));
        chipMakanan.setOnClickListener(v -> setCategory(DatabaseHelper.TIPE_MAKANAN));
        chipMinuman.setOnClickListener(v -> setCategory(DatabaseHelper.TIPE_MINUMAN));
        chipSnack.setOnClickListener(v   -> setCategory(DatabaseHelper.TIPE_SNACK));
        updateChipStyles();

        // RecyclerView — 2 columns grid
        rvStores = findViewById(R.id.rvStores);
        rvStores.setLayoutManager(new GridLayoutManager(this, 2));
        storeAdapter = new RestaurantAdapter(this, new ArrayList<>(), this::openStore);
        rvStores.setAdapter(storeAdapter);

        setupBottomNav();
        loadAllStores();
        updateSaldoUI();
        setupTopupSheet();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSaldoUI();
    }

    // ── Category filter ───────────────────────────────────────────────
    private void setCategory(String category) {
        activeCategory = category;
        updateChipStyles();
        applyFilter();
    }

    private void updateChipStyles() {
        // Active chip: dark blue bg + white text; inactive: light bg + dark text
        setChipActive(chipSemua,   activeCategory.equals("Semua"));
        setChipActive(chipMakanan, activeCategory.equals(DatabaseHelper.TIPE_MAKANAN));
        setChipActive(chipMinuman, activeCategory.equals(DatabaseHelper.TIPE_MINUMAN));
        setChipActive(chipSnack,   activeCategory.equals(DatabaseHelper.TIPE_SNACK));
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

    private void loadAllStores() {
        allStores.clear();
        Cursor c = db.getAllRestoran();
        try {
            while (c != null && c.moveToNext()) {
                allStores.add(cursorToModel(c));
            }
        } finally { if (c != null) c.close(); }
        applyFilter();
    }

    private void applyFilter() {
        List<RestoranModel> filtered = new ArrayList<>();
        for (RestoranModel r : allStores) {
            boolean matchCategory = activeCategory.equals("Semua") || r.tipeMenu.equals(activeCategory);
            boolean matchSearch   = r.namaResto.toLowerCase().contains(searchQuery.toLowerCase());
            if (matchCategory && matchSearch) filtered.add(r);
        }
        storeAdapter.updateData(filtered);

        // Show empty state if needed
        TextView tvEmpty = findViewById(R.id.tvEmptyStores);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private RestoranModel cursorToModel(Cursor c) {
        RestoranModel r = new RestoranModel();
        r.id        = c.getInt(c.getColumnIndexOrThrow("id"));
        r.namaResto = c.getString(c.getColumnIndexOrThrow("nama_resto"));
        r.alamatResto = c.getString(c.getColumnIndexOrThrow("alamat_resto"));
        r.kategori  = c.getString(c.getColumnIndexOrThrow("kategori"));
        r.rating    = c.getFloat(c.getColumnIndexOrThrow("rating"));
        r.gambarUrl = c.getString(c.getColumnIndexOrThrow("gambar_url"));
        int tipeIdx = c.getColumnIndex("tipe_menu");
        r.tipeMenu  = tipeIdx >= 0 ? c.getString(tipeIdx) : DatabaseHelper.TIPE_MAKANAN;
        return r;
    }

    private void openStore(RestoranModel store) {
        Intent intent = new Intent(this, StoreDetailActivity.class);
        intent.putExtra("restoran_id",     store.id);
        intent.putExtra("restoran_nama",   store.namaResto);
        intent.putExtra("restoran_rating", store.rating);
        intent.putExtra("restoran_alamat", store.alamatResto);
        startActivity(intent);
    }

    // ── Saldo ─────────────────────────────────────────────────────────
    private void updateSaldoUI() {
        int saldo = db.getSaldo(userId);
        ((TextView) findViewById(R.id.tvSaldo)).setText("Rp " + fmt(saldo));
        if (tvSheetSaldo != null)
            tvSheetSaldo.setText("Saldo saat ini: Rp " + fmt(saldo));
    }

    // ── Top-up sheet ──────────────────────────────────────────────────
    private void setupTopupSheet() {
        topupSheet     = findViewById(R.id.topupSheet);
        dimOverlay     = findViewById(R.id.topupDimOverlay);
        tvSheetSaldo   = findViewById(R.id.tvSheetSaldo);
        etCustomAmount = findViewById(R.id.etTopupCustom);

        dimOverlay.setOnClickListener(v -> hideTopupSheet());
        findViewById(R.id.btnTopupClose).setOnClickListener(v -> hideTopupSheet());

        int[] presets = {10000, 25000, 50000, 100000, 200000, 500000};
        int[] btnIds  = {R.id.btnTopup10, R.id.btnTopup25, R.id.btnTopup50,
                         R.id.btnTopup100, R.id.btnTopup200, R.id.btnTopup500};
        for (int i = 0; i < btnIds.length; i++) {
            final int amount = presets[i];
            findViewById(btnIds[i]).setOnClickListener(v -> doTopup(amount));
        }
        findViewById(R.id.btnTopupCustomConfirm).setOnClickListener(v -> {
            String val = etCustomAmount.getText().toString().trim();
            if (val.isEmpty()) { Toast.makeText(this, "Masukkan nominal", Toast.LENGTH_SHORT).show(); return; }
            int amount = Integer.parseInt(val);
            if (amount < 1000) { Toast.makeText(this, "Minimal top up Rp 1.000", Toast.LENGTH_SHORT).show(); return; }
            doTopup(amount);
        });
    }

    private void showTopupSheet() {
        tvSheetSaldo.setText("Saldo saat ini: Rp " + fmt(db.getSaldo(userId)));
        etCustomAmount.setText("");
        dimOverlay.setVisibility(View.VISIBLE);
        topupSheet.setVisibility(View.VISIBLE);
        topupSheet.setTranslationY(topupSheet.getHeight() > 0 ? topupSheet.getHeight() : 1200f);
        ObjectAnimator.ofFloat(topupSheet, "translationY", 0f).setDuration(300).start();
    }

    private void hideTopupSheet() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(topupSheet, "translationY", 1200f);
        anim.setDuration(250);
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                topupSheet.setVisibility(View.GONE);
                dimOverlay.setVisibility(View.GONE);
            }
        });
        anim.start();
    }

    private void doTopup(int amount) {
        db.updateSaldo(userId, db.getSaldo(userId) + amount);
        updateSaldoUI();
        Toast.makeText(this, "Top Up Rp " + fmt(amount) + " berhasil!", Toast.LENGTH_SHORT).show();
        hideTopupSheet();
    }

    // ── Bottom nav ────────────────────────────────────────────────────
    private void setupBottomNav() {
        findViewById(R.id.layoutNavHome).setOnClickListener(v -> {});
        findViewById(R.id.layoutNavFavorit).setOnClickListener(v ->
                startActivity(new Intent(this, FavoriteActivity.class)));
        findViewById(R.id.layoutNavCart).setOnClickListener(v ->
                startActivity(new Intent(this, CartActivity.class)));
        findViewById(R.id.layoutNavTransaksi).setOnClickListener(v ->
                startActivity(new Intent(this, TransactionActivity.class)));
        findViewById(R.id.layoutNavProfile).setOnClickListener(v ->
                Toast.makeText(this, "Profil (coming soon)", Toast.LENGTH_SHORT).show());
    }

    private String fmt(int n) { return String.format("%,d", n).replace(',', '.'); }
}
