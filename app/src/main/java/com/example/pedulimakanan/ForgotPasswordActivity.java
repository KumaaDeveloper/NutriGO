package com.example.pedulimakanan;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class ForgotPasswordActivity extends Activity {

    private static final String CONNECTOR_URL = "http://139.162.46.52/pedulimakanan/connector.php";
    private static final String PHONE_PREFIX = "+62";

    private EditText etEmailForgot;
    private EditText etKodeVerifikasi;
    private EditText etPasswordBaru;
    private EditText etConfirmForgot;

    private TextView tvLabelEmailForgot;
    private RadioButton rbEmail;
    private RadioButton rbNomorHp;

    private boolean passwordBaruVisible = false;
    private boolean confirmVisible = false;
    private boolean isUpdatingPhoneText = false;
    private String selectedMethod = "email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        allowScreenRecord();

        setContentView(R.layout.activity_forgot_password);

        ImageButton btnBackForgot = findViewById(R.id.btnBackForgot);
        ImageButton btnEyePasswordBaru = findViewById(R.id.btnEyePasswordBaru);
        ImageButton btnEyeConfirmForgot = findViewById(R.id.btnEyeConfirmForgot);

        rbEmail = findViewById(R.id.rbEmail);
        rbNomorHp = findViewById(R.id.rbNomorHp);
        tvLabelEmailForgot = findViewById(R.id.tvLabelEmailForgot);

        etEmailForgot = findViewById(R.id.etEmailForgot);
        etKodeVerifikasi = findViewById(R.id.etKodeVerifikasi);
        etPasswordBaru = findViewById(R.id.etPasswordBaru);
        etConfirmForgot = findViewById(R.id.etConfirmForgot);

        Button btnAmbilKode = findViewById(R.id.btnAmbilKode);
        Button btnSelesai = findViewById(R.id.btnSelesai);

        rbEmail.setChecked(true);

        btnBackForgot.setOnClickListener(v -> finish());

        rbEmail.setOnClickListener(v -> {
            selectedMethod = "email";
            rbEmail.setChecked(true);
            rbNomorHp.setChecked(false);

            tvLabelEmailForgot.setText(getString(R.string.masukkan_email));

            etEmailForgot.setText("");
            etEmailForgot.setHint(getString(R.string.email));
            etEmailForgot.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            etEmailForgot.setFilters(new InputFilter[]{});
        });

        rbNomorHp.setOnClickListener(v -> {
            selectedMethod = "phone";
            rbEmail.setChecked(false);
            rbNomorHp.setChecked(true);

            tvLabelEmailForgot.setText("Masukkan Nomor HP");

            etEmailForgot.setHint("Nomor HP");
            etEmailForgot.setInputType(InputType.TYPE_CLASS_PHONE);
            etEmailForgot.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
            etEmailForgot.setText(PHONE_PREFIX);
            etEmailForgot.setSelection(etEmailForgot.getText().length());
        });

        etEmailForgot.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!selectedMethod.equals("phone") || isUpdatingPhoneText) {
                    return;
                }

                String text = editable.toString();

                if (!text.startsWith(PHONE_PREFIX)) {
                    isUpdatingPhoneText = true;

                    String digits = text
                            .replace("+62", "")
                            .replaceAll("[^0-9]", "");

                    etEmailForgot.setText(PHONE_PREFIX + digits);
                    etEmailForgot.setSelection(etEmailForgot.getText().length());

                    isUpdatingPhoneText = false;
                }
            }
        });

        btnAmbilKode.setOnClickListener(v -> {
            String identifier = etEmailForgot.getText().toString().trim();

            if (!validateIdentifier(identifier)) {
                return;
            }

            new RequestCodeTask().execute(identifier, selectedMethod);
        });

        btnEyePasswordBaru.setOnClickListener(v -> {
            passwordBaruVisible = !passwordBaruVisible;
            togglePassword(etPasswordBaru, btnEyePasswordBaru, passwordBaruVisible);
        });

        btnEyeConfirmForgot.setOnClickListener(v -> {
            confirmVisible = !confirmVisible;
            togglePassword(etConfirmForgot, btnEyeConfirmForgot, confirmVisible);
        });

        btnSelesai.setOnClickListener(v -> {
            String identifier = etEmailForgot.getText().toString().trim();
            String kode = etKodeVerifikasi.getText().toString().trim();
            String passwordBaru = etPasswordBaru.getText().toString().trim();
            String confirm = etConfirmForgot.getText().toString().trim();

            if (!validateIdentifier(identifier)) {
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

            if (!isPasswordValid(passwordBaru)) {
                showDialog(
                        getString(R.string.reset_password_gagal),
                        "Password harus minimal 8 karakter, memiliki 1 huruf besar, dan 1 angka"
                );
                return;
            }

            if (confirm.isEmpty()) {
                etConfirmForgot.setError(getString(R.string.konfirmasi_password_harus_diisi));
                return;
            }

            if (!passwordBaru.equals(confirm)) {
                showDialog(getString(R.string.reset_password_gagal), getString(R.string.password_tidak_sama));
                return;
            }

            new ResetPasswordTask().execute(identifier, selectedMethod, kode, passwordBaru);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        allowScreenRecord();
    }

    private void allowScreenRecord() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    private boolean validateIdentifier(String identifier) {
        if (selectedMethod.equals("email")) {
            if (identifier.isEmpty()) {
                etEmailForgot.setError(getString(R.string.email_harus_diisi));
                return false;
            }

            if (!identifier.contains("@") || !identifier.contains(".")) {
                etEmailForgot.setError("Format email tidak valid");
                return false;
            }

        } else {
            if (identifier.equals(PHONE_PREFIX) || identifier.length() <= PHONE_PREFIX.length()) {
                etEmailForgot.setError("Nomor HP harus diisi");
                return false;
            }

            if (identifier.length() < 10) {
                etEmailForgot.setError("Nomor HP terlalu pendek");
                return false;
            }
        }

        return true;
    }

    private boolean isPasswordValid(String password) {
        return password.matches("^(?=.*[A-Z])(?=.*\\d).{8,}$");
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
                .setPositiveButton(getString(R.string.tutup), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private class RequestCodeTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... data) {
            HttpURLConnection conn = null;

            try {
                String identifier = data[0];
                String method = data[1];

                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" +
                                URLEncoder.encode("request_code", "UTF-8") + "&" +
                                URLEncoder.encode("identifier", "UTF-8") + "=" +
                                URLEncoder.encode(identifier, "UTF-8") + "&" +
                                URLEncoder.encode("method", "UTF-8") + "=" +
                                URLEncoder.encode(method, "UTF-8");

                OutputStream os = conn.getOutputStream();

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(postData);
                writer.flush();
                writer.close();

                os.close();

                BufferedReader reader;

                if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();

                return result.toString();

            } catch (Exception e) {
                return "{\"success\":false,\"message\":\"Koneksi gagal\"}";
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String response) {
            try {
                JSONObject jsonObject = new JSONObject(response);

                boolean success = jsonObject.getBoolean("success");
                String message = jsonObject.optString("message", "");

                if (success) {
                    String code = jsonObject.optString("code", "");

                    new AlertDialog.Builder(ForgotPasswordActivity.this)
                            .setTitle("Kode Verifikasi")
                            .setMessage("Kode kamu: " + code + "\n\n(Pada aplikasi produksi kode ini dikirim via SMS/email)")
                            .setPositiveButton(getString(R.string.tutup), (dialog, which) -> dialog.dismiss())
                            .show();

                } else {
                    showDialog(getString(R.string.reset_password_gagal), message);
                }

            } catch (Exception e) {
                showDialog(getString(R.string.reset_password_gagal), "Server response tidak valid:\n" + response);
            }
        }
    }

    private class ResetPasswordTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... data) {
            HttpURLConnection conn = null;

            try {
                String identifier = data[0];
                String method = data[1];
                String code = data[2];
                String newPassword = data[3];

                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" +
                                URLEncoder.encode("reset_password", "UTF-8") + "&" +
                                URLEncoder.encode("identifier", "UTF-8") + "=" +
                                URLEncoder.encode(identifier, "UTF-8") + "&" +
                                URLEncoder.encode("method", "UTF-8") + "=" +
                                URLEncoder.encode(method, "UTF-8") + "&" +
                                URLEncoder.encode("code", "UTF-8") + "=" +
                                URLEncoder.encode(code, "UTF-8") + "&" +
                                URLEncoder.encode("password", "UTF-8") + "=" +
                                URLEncoder.encode(newPassword, "UTF-8");

                OutputStream os = conn.getOutputStream();

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(postData);
                writer.flush();
                writer.close();

                os.close();

                BufferedReader reader;

                if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();

                return result.toString();

            } catch (Exception e) {
                return "{\"success\":false,\"message\":\"Koneksi gagal\"}";
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String response) {
            try {
                JSONObject jsonObject = new JSONObject(response);

                boolean success = jsonObject.getBoolean("success");
                String message = jsonObject.optString("message", "");

                if (success) {
                    new AlertDialog.Builder(ForgotPasswordActivity.this)
                            .setTitle(getString(R.string.berhasil))
                            .setMessage("Password berhasil diubah! Silakan login.")
                            .setPositiveButton(getString(R.string.tutup), (dialog, which) -> {
                                dialog.dismiss();
                                finish();
                            })
                            .show();

                } else {
                    showDialog(getString(R.string.reset_password_gagal), message);
                }

            } catch (Exception e) {
                showDialog(getString(R.string.reset_password_gagal), "Server response tidak valid:\n" + response);
            }
        }
    }
}