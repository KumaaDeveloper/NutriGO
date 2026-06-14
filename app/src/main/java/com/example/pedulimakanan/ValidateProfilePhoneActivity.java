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

public class ValidateProfilePhoneActivity extends Activity {

    private static final String CONNECTOR_URL = "http://139.162.46.52/pedulimakanan/connector.php";

    private int userId = -1;
    private String oldPhone = "";
    private String newPhone = "";

    private TextView tvValidatePhoneDesc;
    private TextView tvValidatePhoneStatus;
    private ProgressBar progressValidatePhone;
    private Button btnValidatePhone;
    private Button btnBackProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        allowScreenRecord();

        setContentView(R.layout.activity_validate_profile_phone);

        userId = getIntent().getIntExtra("user_id", -1);
        oldPhone = getIntent().getStringExtra("old_phone");
        newPhone = getIntent().getStringExtra("new_phone");

        if (oldPhone == null) oldPhone = "";
        if (newPhone == null) newPhone = "";

        tvValidatePhoneDesc = findViewById(R.id.tvValidatePhoneDesc);
        tvValidatePhoneStatus = findViewById(R.id.tvValidatePhoneStatus);
        progressValidatePhone = findViewById(R.id.progressValidatePhone);
        btnValidatePhone = findViewById(R.id.btnValidatePhone);
        btnBackProfile = findViewById(R.id.btnBackProfile);

        tvValidatePhoneDesc.setText("Nomor HP baru yang akan divalidasi:\n" + formatPhoneDisplay(newPhone));

        progressValidatePhone.setVisibility(View.GONE);
        tvValidatePhoneStatus.setVisibility(View.GONE);

        btnValidatePhone.setOnClickListener(v -> {
            if (userId <= 0) {
                showFailedState("User tidak valid");
                return;
            }

            if (newPhone.trim().isEmpty()) {
                showFailedState("No telp baru kosong");
                return;
            }

            new ValidateProfilePhoneTask().execute(String.valueOf(userId), newPhone);
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
            progressValidatePhone.setVisibility(View.VISIBLE);
            tvValidatePhoneStatus.setVisibility(View.VISIBLE);
            tvValidatePhoneStatus.setText("Sedang memvalidasi nomor HP...");
            btnValidatePhone.setEnabled(false);
            btnBackProfile.setEnabled(false);
        } else {
            progressValidatePhone.setVisibility(View.GONE);
            btnValidatePhone.setEnabled(true);
            btnBackProfile.setEnabled(true);
        }
    }

    private void showSuccessState(String message) {
        setLoading(false);

        tvValidatePhoneStatus.setVisibility(View.VISIBLE);
        tvValidatePhoneStatus.setText(message);

        btnValidatePhone.setText("Nomor Valid");
        btnValidatePhone.setEnabled(false);
        btnBackProfile.setText("Kembali ke Profile");

        new AlertDialog.Builder(ValidateProfilePhoneActivity.this)
                .setTitle("Berhasil")
                .setMessage("Nomor HP berhasil divalidasi dan diubah.")
                .setPositiveButton("Kembali", (dialog, which) -> {
                    dialog.dismiss();
                    backToProfile();
                })
                .show();
    }

    private void showFailedState(String message) {
        setLoading(false);

        tvValidatePhoneStatus.setVisibility(View.VISIBLE);
        tvValidatePhoneStatus.setText(message);

        btnValidatePhone.setText("Coba Validasi Lagi");
        btnBackProfile.setText("Kembali ke Profile");

        new AlertDialog.Builder(ValidateProfilePhoneActivity.this)
                .setTitle("Validasi Nomor Gagal")
                .setMessage(message)
                .setPositiveButton("Tutup", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void backToProfile() {
        Intent intent = new Intent(ValidateProfilePhoneActivity.this, ProfileActivity.class);
        intent.putExtra("user_id", userId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }

        String clean = phone.trim();
        clean = clean.replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        clean = clean.replaceAll("[^0-9+]", "");

        if (clean.startsWith("+62")) {
            clean = clean.substring(3);
        } else if (clean.startsWith("62")) {
            clean = clean.substring(2);
        } else if (clean.startsWith("0")) {
            clean = clean.substring(1);
        }

        return clean;
    }

    private String formatPhoneDisplay(String phone) {
        String clean = normalizePhone(phone);

        if (clean.isEmpty()) {
            return "+62";
        }

        return "+62 " + clean;
    }

    private class ValidateProfilePhoneTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            setLoading(true);
        }

        @Override
        protected String doInBackground(String... data) {
            HttpURLConnection conn = null;

            try {
                String userIdValue = data[0];
                String phoneValue = data[1];

                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" +
                                URLEncoder.encode("validate_update_phone", "UTF-8") + "&" +
                                URLEncoder.encode("user_id", "UTF-8") + "=" +
                                URLEncoder.encode(userIdValue, "UTF-8") + "&" +
                                URLEncoder.encode("no_hp", "UTF-8") + "=" +
                                URLEncoder.encode(phoneValue, "UTF-8");

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