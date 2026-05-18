package com.example.pedulimakanan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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

public class LoginActivity extends Activity {

    private static final String CONNECTOR_URL = "http://10.0.2.2/pedulimakanan/connector.php";

    private EditText etNamaLogin, etPasswordLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etNamaLogin = findViewById(R.id.etNamaLogin);
        etPasswordLogin = findViewById(R.id.etPasswordLogin);

        TextView tvLupaSandi = findViewById(R.id.tvLupaSandi);
        TextView tvDaftarSekarang = findViewById(R.id.tvDaftarSekarang);
        Button btnMasuk = findViewById(R.id.btnMasuk);

        tvLupaSandi.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        tvDaftarSekarang.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btnMasuk.setOnClickListener(v -> {
            String nama = etNamaLogin.getText().toString().trim();
            String password = etPasswordLogin.getText().toString().trim();

            if (nama.isEmpty()) {
                etNamaLogin.setError(getString(R.string.nama_harus_diisi));
                return;
            }

            if (password.isEmpty()) {
                etPasswordLogin.setError(getString(R.string.password_harus_diisi));
                return;
            }

            new LoginTask().execute(nama, password);
        });
    }

    private class LoginTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... data) {
            try {
                String nama = data[0];
                String password = data[1];

                URL url = new URL(CONNECTOR_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("login", "UTF-8") + "&" +
                                URLEncoder.encode("nama", "UTF-8") + "=" + URLEncoder.encode(nama, "UTF-8") + "&" +
                                URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8");

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
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    showDialogMessage(getString(R.string.login_gagal), message);
                }

            } catch (Exception e) {
                showDialogMessage(getString(R.string.login_gagal), "Response server tidak valid");
            }
        }
    }

    private void showDialogMessage(String title, String message) {
        new AlertDialog.Builder(LoginActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.tutup), (dialog, which) -> dialog.dismiss())
                .show();
    }
}