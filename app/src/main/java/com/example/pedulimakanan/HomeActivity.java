package com.example.pedulimakanan;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = new DatabaseHelper(this);

        // Greeting
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String namaUser = prefs.getString("nama", "");
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        if (!namaUser.isEmpty()) {
            tvGreeting.setText("Halo, " + namaUser + "!\nMau Makan Apa Hari Ini?");
        }

        // Search
        etSearchHome = findViewById(R.id.etSearchHome);
        etSearchHome.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterStores(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // RecyclerViews
        rvPopularStores = findViewById(R.id.rvPopularStores);
        rvBudgetStores  = findViewById(R.id.rvBudgetStores);

        popularAdapter = new RestaurantAdapter(this, new ArrayList<>(), this::openStore);
        budgetAdapter  = new RestaurantAdapter(this, new ArrayList<>(), this::openStore);

        rvPopularStores.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPopularStores.setAdapter(popularAdapter);

        rvBudgetStores.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvBudgetStores.setAdapter(budgetAdapter);

        // Bottom nav
        setupBottomNav(prefs);

        // Load stores from SQLite
        loadStores();
    }

    private void loadStores() {
        allStores.clear();
        Cursor c = db.getAllRestoran();
        while (c.moveToNext()) {
            RestoranModel r = new RestoranModel();
            r.id         = c.getInt(c.getColumnIndexOrThrow("id"));
            r.namaResto  = c.getString(c.getColumnIndexOrThrow("nama_resto"));
            r.alamatResto = c.getString(c.getColumnIndexOrThrow("alamat_resto"));
            r.kategori   = c.getString(c.getColumnIndexOrThrow("kategori"));
            r.rating     = c.getFloat(c.getColumnIndexOrThrow("rating"));
            r.gambarUrl  = c.getString(c.getColumnIndexOrThrow("gambar_url"));
            allStores.add(r);
        }
        c.close();
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

    private void setupBottomNav(SharedPreferences prefs) {
        int userId = prefs.getInt("user_id", -1);

        LinearLayout layoutNavHome      = findViewById(R.id.layoutNavHome);
        LinearLayout layoutNavFavorit   = findViewById(R.id.layoutNavFavorit);
        LinearLayout layoutNavCart      = findViewById(R.id.layoutNavCart);
        LinearLayout layoutNavTransaksi = findViewById(R.id.layoutNavTransaksi);
        LinearLayout layoutNavProfile   = findViewById(R.id.layoutNavProfile);

        // Home — already here
        layoutNavHome.setOnClickListener(v -> { /* already on home */ });

        layoutNavFavorit.setOnClickListener(v ->
                Toast.makeText(this, "Favorit (coming soon)", Toast.LENGTH_SHORT).show());

        layoutNavCart.setOnClickListener(v ->
                Toast.makeText(this, "Keranjang (coming soon)", Toast.LENGTH_SHORT).show());

        layoutNavTransaksi.setOnClickListener(v ->
                Toast.makeText(this, "Transaksi (coming soon)", Toast.LENGTH_SHORT).show());

        layoutNavProfile.setOnClickListener(v ->
                Toast.makeText(this, "Profil (coming soon)", Toast.LENGTH_SHORT).show());
    }
}