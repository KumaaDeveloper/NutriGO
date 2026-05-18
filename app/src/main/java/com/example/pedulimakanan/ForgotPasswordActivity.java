package com.example.pedulimakanan;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class ForgotPasswordActivity extends Activity {

    private boolean passwordBaruVisible = false;
    private boolean confirmVisible = false;

    private EditText etPasswordBaru;
    private EditText etConfirmForgot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        ImageButton btnBackForgot = findViewById(R.id.btnBackForgot);

        RadioButton rbEmail = findViewById(R.id.rbEmail);
        RadioButton rbNomorHp = findViewById(R.id.rbNomorHp);

        TextView tvLabelEmailForgot = findViewById(R.id.tvLabelEmailForgot);

        EditText etEmailForgot = findViewById(R.id.etEmailForgot);
        EditText etKodeVerifikasi = findViewById(R.id.etKodeVerifikasi);

        etPasswordBaru = findViewById(R.id.etPasswordBaru);
        etConfirmForgot = findViewById(R.id.etConfirmForgot);

        ImageButton btnEyePasswordBaru = findViewById(R.id.btnEyePasswordBaru);
        ImageButton btnEyeConfirmForgot = findViewById(R.id.btnEyeConfirmForgot);

        Button btnSelesai = findViewById(R.id.btnSelesai);

        btnBackForgot.setOnClickListener(v -> finish());

        rbEmail.setOnClickListener(v -> {
            tvLabelEmailForgot.setText(getString(R.string.masukkan_email));
            etEmailForgot.setText("");
            etEmailForgot.setHint(getString(R.string.email));
            etEmailForgot.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        });

        rbNomorHp.setOnClickListener(v -> {
            tvLabelEmailForgot.setText(getString(R.string.masukkan_nomor_hp));
            etEmailForgot.setText("");
            etEmailForgot.setHint(getString(R.string.nomor_hp));
            etEmailForgot.setInputType(InputType.TYPE_CLASS_PHONE);
        });

        btnEyePasswordBaru.setOnClickListener(v -> {
            passwordBaruVisible = !passwordBaruVisible;
            togglePassword(etPasswordBaru, passwordBaruVisible);
        });

        btnEyeConfirmForgot.setOnClickListener(v -> {
            confirmVisible = !confirmVisible;
            togglePassword(etConfirmForgot, confirmVisible);
        });

        btnSelesai.setOnClickListener(v -> {
            String emailAtauHp = etEmailForgot.getText().toString().trim();
            String kode = etKodeVerifikasi.getText().toString().trim();
            String passwordBaru = etPasswordBaru.getText().toString().trim();
            String confirm = etConfirmForgot.getText().toString().trim();

            if (emailAtauHp.isEmpty()) {
                etEmailForgot.setError(getString(R.string.data_harus_diisi));
                return;
            }

            if (kode.isEmpty()) {
                etKodeVerifikasi.setError(getString(R.string.kode_verifikasi_harus_diisi));
                return;
            }

            if (passwordBaru.isEmpty()) {
                etPasswordBaru.setError(getString(R.string.password_baru_harus_diisi));
                return;
            }

            if (confirm.isEmpty()) {
                etConfirmForgot.setError(getString(R.string.konfirmasi_password_harus_diisi));
                return;
            }

            if (!passwordBaru.equals(confirm)) {
                etConfirmForgot.setError(getString(R.string.password_tidak_sama));
                return;
            }

            Toast.makeText(
                    ForgotPasswordActivity.this,
                    getString(R.string.password_berhasil_diubah),
                    Toast.LENGTH_SHORT
            ).show();

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