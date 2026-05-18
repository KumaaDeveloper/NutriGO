package com.example.pedulimakanan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {

    TextView tvLupaSandi, tvDaftarSekarang;
    Button btnMasuk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tvLupaSandi = findViewById(R.id.tvLupaSandi);
        tvDaftarSekarang = findViewById(R.id.tvDaftarSekarang);
        btnMasuk = findViewById(R.id.btnMasuk);

        tvLupaSandi.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        tvDaftarSekarang.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btnMasuk.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Tombol Masuk ditekan", Toast.LENGTH_SHORT).show();
        });
    }
}