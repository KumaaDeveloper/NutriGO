package com.example.pedulimakanan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity {

    private RecyclerView rvPopularStores, rvBudgetStores;
    private EditText etSearchHome;
    private DatabaseHelper db;

    private List<RestoranModel> allStores = new ArrayList<>();
    private RestaurantAdapter popularAdapter, budgetAdapter;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = new DatabaseHelper(this);

        // 1. Cek Session Login
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        String namaUser = prefs.getString("nama", "");

        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // 2. Set Greeting
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        if (!namaUser.isEmpty()) {
            tvGreeting.setText("Halo, " + namaUser + "!\nMau Makan Apa Hari Ini?");
        }

        // 3. Setup Tombol Topup & History
        ImageView btnTambahSaldo = findViewById(R.id.btnTambahSaldo);
        btnTambahSaldo.setOnClickListener(v -> showTopupDialog());

        findViewById(R.id.btnHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, TransactionActivity.class));
        });

        // 4. Setup Search
        etSearchHome = findViewById(R.id.etSearchHome);
        etSearchHome.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { filterStores(s.toString()); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // 5. Initialize RecyclerViews
        rvPopularStores = findViewById(R.id.rvPopularStores);
        rvBudgetStores  = findViewById(R.id.rvBudgetStores);

        popularAdapter = new RestaurantAdapter(this, new ArrayList<>(), this::openStore);
        budgetAdapter  = new RestaurantAdapter(this, new ArrayList<>(), this::openStore);

        rvPopularStores.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPopularStores.setAdapter(popularAdapter);
        rvBudgetStores.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvBudgetStores.setAdapter(budgetAdapter);

        setupBottomNav();
        loadStores();
        updateSaldoUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSaldoUI();
    }

    private void updateSaldoUI() {
        int saldo = db.getSaldo(userId);
        TextView tvSaldo = findViewById(R.id.tvSaldo);
        tvSaldo.setText("Rp. " + String.format("%,d", saldo).replace(',', '.'));
    }

    private void showTopupDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("Top Up Saldo")
                .setMessage("Masukkan nominal saldo:")
                .setView(input)
                .setPositiveButton("Top Up", (dialog, which) -> {
                    String val = input.getText().toString();
                    if (!val.isEmpty()) {
                        int nominal = Integer.parseInt(val);
                        if (nominal <= 0) {
                            Toast.makeText(this, "Nominal tidak valid!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int saldoLama = db.getSaldo(userId);
                        db.updateSaldo(userId, saldoLama + nominal);
                        updateSaldoUI();
                        Toast.makeText(this, "Top Up Berhasil!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void loadStores() {
        allStores.clear();
        Cursor c = db.getAllRestoran();
        try {
            while (c != null && c.moveToNext()) {
                RestoranModel r = new RestoranModel();
                r.id          = c.getInt(c.getColumnIndexOrThrow("id"));
                r.namaResto   = c.getString(c.getColumnIndexOrThrow("nama_resto"));
                r.alamatResto = c.getString(c.getColumnIndexOrThrow("alamat_resto"));
                r.kategori    = c.getString(c.getColumnIndexOrThrow("kategori"));
                r.rating      = c.getFloat(c.getColumnIndexOrThrow("rating"));
                r.gambarUrl   = c.getString(c.getColumnIndexOrThrow("gambar_url"));
                allStores.add(r);
            }
        } finally {
            if (c != null) c.close();
        }
        filterStores("");
    }

    private void filterStores(String query) {
        List<RestoranModel> popular = new ArrayList<>();
        List<RestoranModel> budget  = new ArrayList<>();
        for (RestoranModel r : allStores) {
            if (r.namaResto.toLowerCase().contains(query.toLowerCase())) {
                if (r.rating >= 4.0f) popular.add(r);
                else                  budget.add(r);
            }
        }
        popularAdapter.updateData(popular);
        budgetAdapter.updateData(budget);
    }

    private void openStore(RestoranModel store) {
        Intent intent = new Intent(this, StoreDetailActivity.class);
        intent.putExtra("restoran_id",    store.id);
        intent.putExtra("restoran_nama",  store.namaResto);
        intent.putExtra("restoran_rating", store.rating);
        intent.putExtra("restoran_alamat", store.alamatResto);
        startActivity(intent);
    }

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
}