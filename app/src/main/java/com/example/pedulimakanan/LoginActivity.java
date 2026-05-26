package com.example.pedulimakanan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends Activity {

    private EditText etNamaLogin, etPasswordLogin;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new DatabaseHelper(this);

        etNamaLogin    = findViewById(R.id.etNamaLogin);
        etPasswordLogin = findViewById(R.id.etPasswordLogin);
        TextView tvLupaSandi     = findViewById(R.id.tvLupaSandi);
        TextView tvDaftarSekarang = findViewById(R.id.tvDaftarSekarang);
        Button   btnMasuk        = findViewById(R.id.btnMasuk);

        tvLupaSandi.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        tvDaftarSekarang.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        btnMasuk.setOnClickListener(v -> {
            String nama     = etNamaLogin.getText().toString().trim();
            String password = etPasswordLogin.getText().toString().trim();

            if (nama.isEmpty()) {
                etNamaLogin.setError(getString(R.string.nama_harus_diisi));
                return;
            }
            if (password.isEmpty()) {
                etPasswordLogin.setError(getString(R.string.password_harus_diisi));
                return;
            }

            int userId = db.login(nama, password);
            if (userId != -1) {
                // Save session
                SharedPreferences prefs =
                        getSharedPreferences("user_session", MODE_PRIVATE);
                prefs.edit()
                        .putInt("user_id", userId)
                        .putString("nama", nama)
                        .putBoolean("logged_in", true)
                        .apply();

                startActivity(new Intent(this, HomeActivity.class));
                finish();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.login_gagal))
                        .setMessage("Nama atau password salah.")
                        .setPositiveButton(getString(R.string.tutup),
                                (d, w) -> d.dismiss())
                        .show();
            }
        });
    }
}