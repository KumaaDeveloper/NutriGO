package com.example.pedulimakanan;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

public class ForgotPasswordActivity extends Activity {

    private static final String PHONE_PREFIX = "+62";

    private EditText etEmailForgot, etKodeVerifikasi, etPasswordBaru, etConfirmForgot;
    private TextView tvLabelEmailForgot;
    private RadioButton rbEmail, rbNomorHp;

    private boolean passwordBaruVisible = false;
    private boolean confirmVisible      = false;
    private boolean isUpdatingPhoneText = false;
    private String  selectedMethod      = "email";

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        db = new DatabaseHelper(this);

        ImageButton btnBackForgot      = findViewById(R.id.btnBackForgot);
        ImageButton btnEyePasswordBaru = findViewById(R.id.btnEyePasswordBaru);
        ImageButton btnEyeConfirmForgot = findViewById(R.id.btnEyeConfirmForgot);

        rbEmail            = findViewById(R.id.rbEmail);
        rbNomorHp          = findViewById(R.id.rbNomorHp);
        tvLabelEmailForgot = findViewById(R.id.tvLabelEmailForgot);
        etEmailForgot      = findViewById(R.id.etEmailForgot);
        etKodeVerifikasi   = findViewById(R.id.etKodeVerifikasi);
        etPasswordBaru     = findViewById(R.id.etPasswordBaru);
        etConfirmForgot    = findViewById(R.id.etConfirmForgot);
        Button btnAmbilKode = findViewById(R.id.btnAmbilKode);
        Button btnSelesai   = findViewById(R.id.btnSelesai);

        rbEmail.setChecked(true);
        btnBackForgot.setOnClickListener(v -> finish());

        // ── Radio buttons ─────────────────────────────────────────────
        rbEmail.setOnClickListener(v -> {
            selectedMethod = "email";
            rbNomorHp.setChecked(false);
            tvLabelEmailForgot.setText(getString(R.string.masukkan_email));
            etEmailForgot.setText("");
            etEmailForgot.setHint(getString(R.string.email));
            etEmailForgot.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            etEmailForgot.setFilters(new InputFilter[]{});
        });

        rbNomorHp.setOnClickListener(v -> {
            selectedMethod = "phone";
            rbEmail.setChecked(false);
            tvLabelEmailForgot.setText("Masukkan Nomor HP");
            etEmailForgot.setHint("Nomor HP");
            etEmailForgot.setInputType(InputType.TYPE_CLASS_PHONE);
            etEmailForgot.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
            etEmailForgot.setText(PHONE_PREFIX);
            etEmailForgot.setSelection(etEmailForgot.getText().length());
        });

        // ── Phone prefix guard ────────────────────────────────────────
        etEmailForgot.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable editable) {
                if (!selectedMethod.equals("phone") || isUpdatingPhoneText) return;
                String text = editable.toString();
                if (!text.startsWith(PHONE_PREFIX)) {
                    isUpdatingPhoneText = true;
                    String digits = text.replace("+62", "").replaceAll("[^0-9]", "");
                    etEmailForgot.setText(PHONE_PREFIX + digits);
                    etEmailForgot.setSelection(etEmailForgot.getText().length());
                    isUpdatingPhoneText = false;
                }
            }
        });

        // ── Ambil Kode ────────────────────────────────────────────────
        btnAmbilKode.setOnClickListener(v -> {
            String identifier = etEmailForgot.getText().toString().trim();
            if (!validateIdentifier(identifier)) return;

            String code = db.requestResetCode(identifier, selectedMethod);
            if (code != null) {
                // In a real app you'd send this via SMS/email.
                // Since we're local, we show it directly (dev mode).
                new AlertDialog.Builder(this)
                        .setTitle("Kode Verifikasi")
                        .setMessage("Kode kamu: " + code +
                                "\n\n(Pada aplikasi produksi kode ini dikirim via SMS/email)")
                        .setPositiveButton(getString(R.string.tutup), (d, w) -> d.dismiss())
                        .show();
            } else {
                showDialog(getString(R.string.reset_password_gagal),
                        selectedMethod.equals("email")
                                ? "Email tidak ditemukan."
                                : "Nomor HP tidak ditemukan.");
            }
        });

        // ── Toggle password visibility ────────────────────────────────
        btnEyePasswordBaru.setOnClickListener(v -> {
            passwordBaruVisible = !passwordBaruVisible;
            togglePassword(etPasswordBaru, passwordBaruVisible);
        });
        btnEyeConfirmForgot.setOnClickListener(v -> {
            confirmVisible = !confirmVisible;
            togglePassword(etConfirmForgot, confirmVisible);
        });

        // ── Selesai / reset ───────────────────────────────────────────
        btnSelesai.setOnClickListener(v -> {
            String identifier   = etEmailForgot.getText().toString().trim();
            String kode         = etKodeVerifikasi.getText().toString().trim();
            String passwordBaru = etPasswordBaru.getText().toString().trim();
            String confirm      = etConfirmForgot.getText().toString().trim();

            if (!validateIdentifier(identifier)) return;
            if (kode.isEmpty())         { etKodeVerifikasi.setError(getString(R.string.kode_verifikasi_harus_diisi)); return; }
            if (passwordBaru.isEmpty()) { etPasswordBaru.setError(getString(R.string.password_baru_harus_diisi)); return; }
            if (confirm.isEmpty())      { etConfirmForgot.setError(getString(R.string.konfirmasi_password_harus_diisi)); return; }
            if (!passwordBaru.equals(confirm)) {
                showDialog(getString(R.string.reset_password_gagal), getString(R.string.password_tidak_sama));
                return;
            }

            boolean success = db.resetPassword(identifier, selectedMethod, kode, passwordBaru);
            if (success) {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.berhasil))
                        .setMessage("Password berhasil diubah! Silakan login.")
                        .setPositiveButton(getString(R.string.tutup), (d, w) -> {
                            d.dismiss();
                            finish();
                        }).show();
            } else {
                showDialog(getString(R.string.reset_password_gagal),
                        "Kode verifikasi salah atau sudah kadaluarsa.");
            }
        });
    }

    private boolean validateIdentifier(String identifier) {
        if (selectedMethod.equals("email")) {
            if (identifier.isEmpty()) {
                etEmailForgot.setError(getString(R.string.email_harus_diisi));
                return false;
            }
        } else {
            if (identifier.equals(PHONE_PREFIX) || identifier.length() <= PHONE_PREFIX.length()) {
                etEmailForgot.setError("Nomor HP harus diisi");
                return false;
            }
        }
        return true;
    }

    private void togglePassword(EditText et, boolean visible) {
        et.setInputType(visible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
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