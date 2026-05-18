package com.example.pedulimakanan;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
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

    private static final String CONNECTOR_URL = "http://10.0.2.2/pedulimakanan/connector.php";

    private EditText etEmailForgot, etKodeVerifikasi;
    private EditText etPasswordBaru, etConfirmForgot;
    private TextView tvLabelEmailForgot;

    private boolean passwordBaruVisible = false;
    private boolean confirmVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        ImageButton btnBackForgot = findViewById(R.id.btnBackForgot);
        ImageButton btnEyePasswordBaru = findViewById(R.id.btnEyePasswordBaru);
        ImageButton btnEyeConfirmForgot = findViewById(R.id.btnEyeConfirmForgot);

        RadioButton rbEmail = findViewById(R.id.rbEmail);
        RadioButton rbNomorHp = findViewById(R.id.rbNomorHp);

        tvLabelEmailForgot = findViewById(R.id.tvLabelEmailForgot);

        etEmailForgot = findViewById(R.id.etEmailForgot);
        etKodeVerifikasi = findViewById(R.id.etKodeVerifikasi);
        etPasswordBaru = findViewById(R.id.etPasswordBaru);
        etConfirmForgot = findViewById(R.id.etConfirmForgot);

        Button btnAmbilKode = findViewById(R.id.btnAmbilKode);
        Button btnSelesai = findViewById(R.id.btnSelesai);

        btnBackForgot.setOnClickListener(v -> finish());

        rbEmail.setOnClickListener(v -> {
            tvLabelEmailForgot.setText(getString(R.string.masukkan_email));
            etEmailForgot.setText("");
            etEmailForgot.setHint(getString(R.string.email));
            etEmailForgot.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        });

        rbNomorHp.setOnClickListener(v -> {
            showDialogMessage("Info", "Reset password saat ini memakai email.");
            rbEmail.setChecked(true);
            rbNomorHp.setChecked(false);
        });

        btnAmbilKode.setOnClickListener(v -> {
            String email = etEmailForgot.getText().toString().trim();

            if (email.isEmpty()) {
                etEmailForgot.setError(getString(R.string.email_harus_diisi));
                return;
            }

            new RequestCodeTask().execute(email);
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
            String email = etEmailForgot.getText().toString().trim();
            String kode = etKodeVerifikasi.getText().toString().trim();
            String passwordBaru = etPasswordBaru.getText().toString().trim();
            String confirm = etConfirmForgot.getText().toString().trim();

            if (email.isEmpty()) {
                etEmailForgot.setError(getString(R.string.email_harus_diisi));
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
                showDialogMessage(
                        getString(R.string.reset_password_gagal),
                        getString(R.string.password_tidak_sama)
                );
                return;
            }

            new ResetPasswordTask().execute(email, kode, passwordBaru, confirm);
        });
    }

    private class RequestCodeTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... data) {
            try {
                String email = data[0];

                URL url = new URL(CONNECTOR_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("request_code", "UTF-8") + "&" +
                                URLEncoder.encode("email", "UTF-8") + "=" + URLEncoder.encode(email, "UTF-8");

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(postData);
                writer.flush();
                writer.close();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                conn.disconnect();

                return result.toString();

            } catch (Exception e) {
                return "{\"success\":false,\"message\":\"Koneksi gagal: " + e.getMessage() + "\"}";
            }
        }

        @Override
        protected void onPostExecute(String response) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                boolean success = jsonObject.getBoolean("success");
                String message = jsonObject.getString("message");

                if (success) {
                    String kode = jsonObject.getString("kode");

                    new AlertDialog.Builder(ForgotPasswordActivity.this)
                            .setTitle(getString(R.string.berhasil))
                            .setMessage(message + "\n\nKode kamu: " + kode)
                            .setPositiveButton(getString(R.string.tutup), (dialog, which) -> dialog.dismiss())
                            .show();
                } else {
                    showDialogMessage(getString(R.string.reset_password_gagal), message);
                }

            } catch (Exception e) {
                showDialogMessage(getString(R.string.reset_password_gagal), "Response server tidak valid");
            }
        }
    }

    private class ResetPasswordTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... data) {
            try {
                String email = data[0];
                String kode = data[1];
                String passwordBaru = data[2];
                String confirm = data[3];

                URL url = new URL(CONNECTOR_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("reset_password", "UTF-8") + "&" +
                                URLEncoder.encode("email", "UTF-8") + "=" + URLEncoder.encode(email, "UTF-8") + "&" +
                                URLEncoder.encode("kode", "UTF-8") + "=" + URLEncoder.encode(kode, "UTF-8") + "&" +
                                URLEncoder.encode("password_baru", "UTF-8") + "=" + URLEncoder.encode(passwordBaru, "UTF-8") + "&" +
                                URLEncoder.encode("konfirmasi", "UTF-8") + "=" + URLEncoder.encode(confirm, "UTF-8");

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(postData);
                writer.flush();
                writer.close();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                conn.disconnect();

                return result.toString();

            } catch (Exception e) {
                return "{\"success\":false,\"message\":\"Koneksi gagal: " + e.getMessage() + "\"}";
            }
        }

        @Override
        protected void onPostExecute(String response) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                boolean success = jsonObject.getBoolean("success");
                String message = jsonObject.getString("message");

                if (success) {
                    new AlertDialog.Builder(ForgotPasswordActivity.this)
                            .setTitle(getString(R.string.berhasil))
                            .setMessage(message)
                            .setPositiveButton(getString(R.string.tutup), (dialog, which) -> {
                                dialog.dismiss();
                                finish();
                            })
                            .show();
                } else {
                    showDialogMessage(getString(R.string.reset_password_gagal), message);
                }

            } catch (Exception e) {
                showDialogMessage(getString(R.string.reset_password_gagal), "Response server tidak valid");
            }
        }
    }

    private void togglePassword(EditText editText, boolean visible) {
        if (visible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        editText.setSelection(editText.getText().length());
    }

    private void showDialogMessage(String title, String message) {
        new AlertDialog.Builder(ForgotPasswordActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.tutup), (dialog, which) -> dialog.dismiss())
                .show();
    }
}