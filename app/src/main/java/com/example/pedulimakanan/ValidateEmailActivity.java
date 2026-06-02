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

public class ValidateEmailActivity extends Activity {

    private static final String CONNECTOR_URL = "http://172.104.183.200/pedulimakanan/connector.php";

    private int pendingId = 0;
    private String email = "";
    private String noHp = "";

    private TextView tvValidateDesc;
    private TextView tvValidateStatus;
    private ProgressBar progressValidate;
    private Button btnValidateEmail;
    private Button btnBackRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        allowScreenRecord();

        setContentView(R.layout.activity_validate_email);

        pendingId = getIntent().getIntExtra("pending_id", 0);
        email = getIntent().getStringExtra("email");
        noHp = getIntent().getStringExtra("no_hp");

        if (email == null) {
            email = "";
        }

        if (noHp == null) {
            noHp = "";
        }

        tvValidateDesc = findViewById(R.id.tvValidateDesc);
        tvValidateStatus = findViewById(R.id.tvValidateStatus);
        progressValidate = findViewById(R.id.progressValidate);
        btnValidateEmail = findViewById(R.id.btnValidateEmail);
        btnBackRegister = findViewById(R.id.btnBackRegister);

        tvValidateDesc.setText("Email yang akan divalidasi:\n" + email);

        progressValidate.setVisibility(View.GONE);
        tvValidateStatus.setVisibility(View.GONE);

        btnValidateEmail.setOnClickListener(v -> {
            if (pendingId <= 0) {
                showFailedState("Pending ID tidak valid");
                return;
            }

            new ValidateEmailTask().execute(String.valueOf(pendingId));
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
            progressValidate.setVisibility(View.VISIBLE);
            tvValidateStatus.setVisibility(View.VISIBLE);
            tvValidateStatus.setText("Sedang memvalidasi email...");
            btnValidateEmail.setEnabled(false);
            btnBackRegister.setEnabled(false);
        } else {
            progressValidate.setVisibility(View.GONE);
            btnValidateEmail.setEnabled(true);
            btnBackRegister.setEnabled(true);
        }
    }

    private void goToPhoneValidation() {
        Intent intent = new Intent(ValidateEmailActivity.this, ValidatePhoneActivity.class);
        intent.putExtra("pending_id", pendingId);
        intent.putExtra("email", email);
        intent.putExtra("no_hp", noHp);
        startActivity(intent);
        finish();
    }

    private void showSuccessState(String message) {
        setLoading(false);

        tvValidateStatus.setVisibility(View.VISIBLE);
        tvValidateStatus.setText(message);

        btnValidateEmail.setText("Email Valid");
        btnValidateEmail.setEnabled(false);
        btnBackRegister.setText("Lanjut Validasi Nomor");

        goToPhoneValidation();
    }

    private void showFailedState(String message) {
        setLoading(false);

        tvValidateStatus.setVisibility(View.VISIBLE);
        tvValidateStatus.setText(message);

        btnValidateEmail.setText("Coba Validasi Lagi");
        btnBackRegister.setText("Kembali ke Register");

        new AlertDialog.Builder(ValidateEmailActivity.this)
                .setTitle("Validasi Gagal")
                .setMessage(message)
                .setPositiveButton("Tutup", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void backToRegister() {
        Intent intent = new Intent(ValidateEmailActivity.this, RegisterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private class ValidateEmailTask extends AsyncTask<String, Void, String> {

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
                                URLEncoder.encode("validate_pending_email", "UTF-8") + "&" +
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
                    noHp = jsonObject.optString("no_hp", noHp);
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