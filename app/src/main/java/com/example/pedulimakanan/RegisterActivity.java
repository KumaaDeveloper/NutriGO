package com.example.pedulimakanan;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

public class RegisterActivity extends Activity {

    private EditText etNamaRegister, etEmailRegister, etPhoneRegister,
            etAlamatRegister, etPasswordRegister, etConfirmRegister; // ── Tambah etAlamatRegister
    private boolean passwordVisible = false;
    private boolean confirmVisible  = false;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = new DatabaseHelper(this);

        ImageButton btnBackRegister      = findViewById(R.id.btnBackRegister);
        ImageButton btnEyeRegister       = findViewById(R.id.btnEyeRegister);
        ImageButton btnEyeConfirmRegister = findViewById(R.id.btnEyeConfirmRegister);
        etNamaRegister    = findViewById(R.id.etNamaRegister);
        etEmailRegister   = findViewById(R.id.etEmailRegister);
        etPhoneRegister   = findViewById(R.id.etPhoneRegister);
        etAlamatRegister  = findViewById(R.id.etAlamatRegister); // ── Inisialisasi ID Alamat
        etPasswordRegister = findViewById(R.id.etPasswordRegister);
        etConfirmRegister  = findViewById(R.id.etConfirmRegister);
        Button btnDaftar  = findViewById(R.id.btnDaftar);

        btnEyeRegister.setImageResource(R.drawable.ic_eye_close);
        btnEyeConfirmRegister.setImageResource(R.drawable.ic_eye_close);

        btnBackRegister.setOnClickListener(v -> finish());

        btnEyeRegister.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            togglePassword(etPasswordRegister, btnEyeRegister, passwordVisible);
        });

        btnEyeConfirmRegister.setOnClickListener(v -> {
            confirmVisible = !confirmVisible;
            togglePassword(etConfirmRegister, btnEyeConfirmRegister, confirmVisible);
        });

        btnDaftar.setOnClickListener(v -> {
            String nama     = etNamaRegister.getText().toString().trim();
            String email    = etEmailRegister.getText().toString().trim();
            String noHp     = etPhoneRegister.getText().toString().trim();
            String alamat   = etAlamatRegister.getText().toString().trim(); // ── Ambil teks alamat
            String password = etPasswordRegister.getText().toString().trim();
            String confirm  = etConfirmRegister.getText().toString().trim();

            if (nama.isEmpty())    { etNamaRegister.setError(getString(R.string.nama_harus_diisi)); return; }
            if (email.isEmpty())   { etEmailRegister.setError(getString(R.string.email_harus_diisi)); return; }
            if (noHp.isEmpty())    { etPhoneRegister.setError(getString(R.string.no_hp_harus_diisi)); return; }
            if (alamat.isEmpty())  { etAlamatRegister.setError(getString(R.string.alamat_harus_diisi)); return; } // ── Validasi alamat
            if (password.isEmpty()){ etPasswordRegister.setError(getString(R.string.password_harus_diisi)); return; }
            if (confirm.isEmpty()) { etConfirmRegister.setError(getString(R.string.konfirmasi_password_harus_diisi)); return; }

            if (!password.equals(confirm)) {
                showDialog(getString(R.string.register_gagal), getString(R.string.password_tidak_sama));
                return;
            }

            // Panggil fungsi register dengan parameter alamat tambahan
            boolean success = db.register(nama, email, noHp, password, alamat);
            if (success) {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.berhasil))
                        .setMessage("Akun berhasil dibuat! Silakan login.")
                        .setPositiveButton(getString(R.string.tutup), (d, w) -> {
                            d.dismiss();
                            finish();
                        }).show();
            } else {
                showDialog(getString(R.string.register_gagal),
                        "Email sudah terdaftar. Gunakan email lain.");
            }
        });
    }

    private void togglePassword(EditText et, ImageButton btn, boolean visible) {
        if (visible) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btn.setImageResource(R.drawable.ic_eye);
        } else {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btn.setImageResource(R.drawable.ic_eye_close);
        }
        et.setSelection(et.getText().length());
    }

    private void showDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.tutup), (d, w) -> d.dismiss())
                .show();
    }
}