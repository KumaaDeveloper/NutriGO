package com.example.pedulimakanan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ProfileActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Pasang aksi klik untuk setiap menu profil sesuai mockup kamu
        findViewById(R.id.btnChangePhoto).setOnClickListener(v -> Toast.makeText(this, "Ubah Foto Profil", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnChangeEmail).setOnClickListener(v -> Toast.makeText(this, "Ubah Email", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnProfileName).setOnClickListener(v -> Toast.makeText(this, "Ubah Nama", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnUsername).setOnClickListener(v -> Toast.makeText(this, "Ubah Username", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnPassword).setOnClickListener(v -> Toast.makeText(this, "Ubah Password", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnKeluar).setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        setupNavbar();
    }

    private void setupNavbar() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
        findViewById(R.id.navFavorite).setOnClickListener(v -> {
            startActivity(new Intent(this, FavoriteActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
        findViewById(R.id.navOrders).setOnClickListener(v -> {
            startActivity(new Intent(this, TransactionActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }
}