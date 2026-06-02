package com.example.pedulimakanan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
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

public class ValidatePhoneActivity extends Activity {

    private static final String CONNECTOR_URL = "http://172.104.183.200/pedulimakanan/connector.php";

    private int pendingId = 0;
    private String email = "";
    private String noHp = "";

    private TextView tvValidatePhoneDesc;
    private TextView tvValidatePhoneStatus;
    private ProgressBar progressValidatePhone;
    private Button btnValidatePhone;
    private Button btnBackRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        allowScreenRecord();

        setContentView(R.layout.activity_validate_phone);

        pendingId = getIntent().getIntExtra("pending_id", 0);
        email = getIntent().getStringExtra("email");
        noHp = getIntent().getStringExtra("no_hp");

        if (email == null) {
            email = "";
        }

        if (noHp == null) {
            noHp = "";
        }

        tvValidatePhoneDesc = findViewById(R.id.tvValidatePhoneDesc);
        tvValidatePhoneStatus = findViewById(R.id.tvValidatePhoneStatus);
        progressValidatePhone = findViewById(R.id.progressValidatePhone);
        btnValidatePhone = findViewById(R.id.btnValidatePhone);
        btnBackRegister = findViewById(R.id.btnBackRegister);

        tvValidatePhoneDesc.setText("Nomor HP yang akan divalidasi:\n" + formatPhoneDisplay(noHp));

        progressValidatePhone.setVisibility(View.GONE);
        tvValidatePhoneStatus.setVisibility(View.GONE);

        btnValidatePhone.setOnClickListener(v -> {
            if (pendingId <= 0) {
                showFailedState("Pending ID tidak valid");
                return;
            }

            new ValidatePhoneTask().execute(String.valueOf(pendingId));
        });

        btnBackRegister.setOnClickListener(v -> backToRegister());
    }

    @Override
    protected void onResume() {
        super.onResume();
        allowScreenRecord();
    }

    private void allowScreenRecord() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    private void setLoading(boolean loading) {
        if (loading) {
            progressValidatePhone.setVisibility(View.VISIBLE);
            tvValidatePhoneStatus.setVisibility(View.VISIBLE);
            tvValidatePhoneStatus.setText("Sedang memvalidasi nomor HP...");
            btnValidatePhone.setEnabled(false);
            btnBackRegister.setEnabled(false);
        } else {
            progressValidatePhone.setVisibility(View.GONE);
            btnValidatePhone.setEnabled(true);
            btnBackRegister.setEnabled(true);
        }
    }

    private void showSuccessState(String message) {
        setLoading(false);

        tvValidatePhoneStatus.setVisibility(View.VISIBLE);
        tvValidatePhoneStatus.setText(message);

        btnValidatePhone.setText("Nomor Valid");
        btnValidatePhone.setEnabled(false);
        btnBackRegister.setText("Ke Login");

        new AlertDialog.Builder(ValidatePhoneActivity.this)
                .setTitle("Berhasil")
                .setMessage("Semua data sudah valid, silahkan login.")
                .setPositiveButton("Login", (dialog, which) -> {
                    dialog.dismiss();

                    Intent intent = new Intent(ValidatePhoneActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private void showFailedState(String message) {
        setLoading(false);

        tvValidatePhoneStatus.setVisibility(View.VISIBLE);
        tvValidatePhoneStatus.setText(message);

        btnValidatePhone.setText("Coba Validasi Lagi");
        btnBackRegister.setText("Kembali ke Register");

        new AlertDialog.Builder(ValidatePhoneActivity.this)
                .setTitle("Validasi Nomor Gagal")
                .setMessage(message)
                .setPositiveButton("Tutup", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void backToRegister() {
        Intent intent = new Intent(ValidatePhoneActivity.this, RegisterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private String formatPhoneDisplay(String phone) {
        if (phone == null) {
            return "+62";
        }

        String clean = phone.trim();
        clean = clean.replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        clean = clean.replaceAll("[^0-9+]", "");

        if (clean.startsWith("+62")) {
            return "+62 " + clean.substring(3);
        }

        if (clean.startsWith("62")) {
            return "+62 " + clean.substring(2);
        }

        if (clean.startsWith("0")) {
            return "+62 " + clean.substring(1);
        }

        return "+62 " + clean;
    }

    private class ValidatePhoneTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            setLoading(true);
        }

        @Override
        protected String doInBackground(String... data) {
            HttpURLConnection conn = null;

            try {
                String pendingIdValue = data[0];

                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" +
                                URLEncoder.encode("validate_pending_phone", "UTF-8") + "&" +
                                URLEncoder.encode("pending_id", "UTF-8") + "=" +
                                URLEncoder.encode(pendingIdValue, "UTF-8");

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

                boolean success = jsonObject.optBoolean("success", false);
                String message = jsonObject.optString("message", "Validasi gagal");

                if (success) {
                    showSuccessState(message);
                } else {
                    showFailedState(message);
                }

            } catch (Exception e) {
                showFailedState("Response server tidak valid:\n" + response);
            }
        }
    }
}