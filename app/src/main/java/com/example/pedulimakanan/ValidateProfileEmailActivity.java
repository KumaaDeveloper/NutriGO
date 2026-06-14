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

public class ValidateProfileEmailActivity extends Activity {

    private static final String CONNECTOR_URL = "http://139.162.46.52/pedulimakanan/connector.php";

    private int userId = -1;
    private String oldEmail = "";
    private String newEmail = "";

    private TextView tvValidateDesc;
    private TextView tvValidateStatus;
    private ProgressBar progressValidate;
    private Button btnValidateEmail;
    private Button btnBackProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        allowScreenRecord();

        setContentView(R.layout.activity_validate_profile_email);

        userId = getIntent().getIntExtra("user_id", -1);
        oldEmail = getIntent().getStringExtra("old_email");
        newEmail = getIntent().getStringExtra("new_email");

        if (oldEmail == null) oldEmail = "";
        if (newEmail == null) newEmail = "";

        tvValidateDesc = findViewById(R.id.tvValidateDesc);
        tvValidateStatus = findViewById(R.id.tvValidateStatus);
        progressValidate = findViewById(R.id.progressValidate);
        btnValidateEmail = findViewById(R.id.btnValidateEmail);
        btnBackProfile = findViewById(R.id.btnBackProfile);

        tvValidateDesc.setText("Email baru yang akan divalidasi:\n" + newEmail);

        progressValidate.setVisibility(View.GONE);
        tvValidateStatus.setVisibility(View.GONE);

        btnValidateEmail.setOnClickListener(v -> {
            if (userId <= 0) {
                showFailedState("User tidak valid");
                return;
            }

            if (newEmail.trim().isEmpty()) {
                showFailedState("Email baru kosong");
                return;
            }

            new ValidateProfileEmailTask().execute(String.valueOf(userId), newEmail);
        });

        btnBackProfile.setOnClickListener(v -> backToProfile());
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
            btnBackProfile.setEnabled(false);
        } else {
            progressValidate.setVisibility(View.GONE);
            btnValidateEmail.setEnabled(true);
            btnBackProfile.setEnabled(true);
        }
    }

    private void showSuccessState(String message) {
        setLoading(false);

        tvValidateStatus.setVisibility(View.VISIBLE);
        tvValidateStatus.setText(message);

        btnValidateEmail.setText("Email Valid");
        btnValidateEmail.setEnabled(false);
        btnBackProfile.setText("Kembali ke Profile");

        new AlertDialog.Builder(ValidateProfileEmailActivity.this)
                .setTitle("Berhasil")
                .setMessage("Email berhasil divalidasi dan diubah.")
                .setPositiveButton("Kembali", (dialog, which) -> {
                    dialog.dismiss();
                    backToProfile();
                })
                .show();
    }

    private void showFailedState(String message) {
        setLoading(false);

        tvValidateStatus.setVisibility(View.VISIBLE);
        tvValidateStatus.setText(message);

        btnValidateEmail.setText("Coba Validasi Lagi");
        btnBackProfile.setText("Kembali ke Profile");

        new AlertDialog.Builder(ValidateProfileEmailActivity.this)
                .setTitle("Validasi Email Gagal")
                .setMessage(message)
                .setPositiveButton("Tutup", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void backToProfile() {
        Intent intent = new Intent(ValidateProfileEmailActivity.this, ProfileActivity.class);
        intent.putExtra("user_id", userId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private class ValidateProfileEmailTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            setLoading(true);
        }

        @Override
        protected String doInBackground(String... data) {
            HttpURLConnection conn = null;

            try {
                String userIdValue = data[0];
                String emailValue = data[1];

                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" +
                                URLEncoder.encode("validate_update_email", "UTF-8") + "&" +
                                URLEncoder.encode("user_id", "UTF-8") + "=" +
                                URLEncoder.encode(userIdValue, "UTF-8") + "&" +
                                URLEncoder.encode("email", "UTF-8") + "=" +
                                URLEncoder.encode(emailValue, "UTF-8");

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