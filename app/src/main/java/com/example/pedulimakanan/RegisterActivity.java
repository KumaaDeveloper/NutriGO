package com.example.pedulimakanan;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class RegisterActivity extends Activity {

    ImageButton btnBackRegister;
    ImageButton btnEyeRegister;
    ImageButton btnEyeConfirmRegister;

    EditText etNamaRegister;
    EditText etEmailRegister;
    EditText etPhoneRegister;
    EditText etPasswordRegister;
    EditText etConfirmRegister;

    Button btnDaftar;

    boolean passwordVisible = false;
    boolean confirmVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        btnBackRegister = findViewById(R.id.btnBackRegister);
        btnEyeRegister = findViewById(R.id.btnEyeRegister);
        btnEyeConfirmRegister = findViewById(R.id.btnEyeConfirmRegister);

        etNamaRegister = findViewById(R.id.etNamaRegister);
        etEmailRegister = findViewById(R.id.etEmailRegister);
        etPhoneRegister = findViewById(R.id.etPhoneRegister);
        etPasswordRegister = findViewById(R.id.etPasswordRegister);
        etConfirmRegister = findViewById(R.id.etConfirmRegister);

        btnDaftar = findViewById(R.id.btnDaftar);

        btnBackRegister.setOnClickListener(v -> finish());

        btnEyeRegister.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            togglePassword(etPasswordRegister, passwordVisible);
        });

        btnEyeConfirmRegister.setOnClickListener(v -> {
            confirmVisible = !confirmVisible;
            togglePassword(etConfirmRegister, confirmVisible);
        });

        btnDaftar.setOnClickListener(v -> {
            String nama = etNamaRegister.getText().toString().trim();
            String email = etEmailRegister.getText().toString().trim();
            String noHp = etPhoneRegister.getText().toString().trim();
            String password = etPasswordRegister.getText().toString().trim();
            String confirm = etConfirmRegister.getText().toString().trim();

            if (nama.isEmpty()) {
                etNamaRegister.setError("Nama harus diisi");
                return;
            }

            if (email.isEmpty()) {
                etEmailRegister.setError("Email harus diisi");
                return;
            }

            if (noHp.isEmpty()) {
                etPhoneRegister.setError("No HP harus diisi");
                return;
            }

            if (password.isEmpty()) {
                etPasswordRegister.setError("Password harus diisi");
                return;
            }

            if (confirm.isEmpty()) {
                etConfirmRegister.setError("Konfirmasi password harus diisi");
                return;
            }

            if (!password.equals(confirm)) {
                etConfirmRegister.setError("Password tidak sama");
                return;
            }

            Toast.makeText(RegisterActivity.this, "Daftar berhasil", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void togglePassword(EditText editText, boolean visible) {
        if (visible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        editText.setSelection(editText.getText().length());
    }
}