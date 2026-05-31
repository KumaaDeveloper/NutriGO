package com.example.pedulimakanan;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class RegisterActivity extends Activity {

    private static final String CONNECTOR_URL = "http://172.104.183.200/pedulimakanan/connector.php";

    private EditText etNamaRegister, etEmailRegister, etPhoneRegister, etAlamatRegister,
            etPasswordRegister, etConfirmRegister;
    private boolean passwordVisible = false;
    private boolean confirmVisible  = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ImageButton btnBackRegister       = findViewById(R.id.btnBackRegister);
        ImageButton btnEyeRegister        = findViewById(R.id.btnEyeRegister);
        ImageButton btnEyeConfirmRegister = findViewById(R.id.btnEyeConfirmRegister);

        etNamaRegister     = findViewById(R.id.etNamaRegister);
        etEmailRegister    = findViewById(R.id.etEmailRegister);
        etPhoneRegister    = findViewById(R.id.etPhoneRegister);
        etAlamatRegister   = findViewById(R.id.etAlamatRegister);
        etPasswordRegister = findViewById(R.id.etPasswordRegister);
        etConfirmRegister  = findViewById(R.id.etConfirmRegister);
        Button btnDaftar   = findViewById(R.id.btnDaftar);

        btnEyeRegister.setImageResource(R.drawable.ic_eye_close);
        btnEyeConfirmRegister.setImageResource(R.drawable.ic_eye_close);

        btnBackRegister.setOnClickListener(v -> finish());

        btnEyeRegister.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            togglePassword(etPasswordRegister, btnEyeRegister, passwordVisible);
        });

        btnEyeConfirmRegister.setOnClickListener(v -> {
            confirmVisible = !confirmVisible;
            togglePassword(etConfirmRegister, btnEyeConfirmRegister, confirmVisible);
        });

        btnDaftar.setOnClickListener(v -> {
            String nama     = etNamaRegister.getText().toString().trim();
            String email    = etEmailRegister.getText().toString().trim();
            String noHp     = etPhoneRegister.getText().toString().trim();
            String alamat   = etAlamatRegister.getText().toString().trim();
            String password = etPasswordRegister.getText().toString().trim();
            String confirm  = etConfirmRegister.getText().toString().trim();

            if (nama.isEmpty()) { etNamaRegister.setError(getString(R.string.nama_harus_diisi)); return; }
            if (email.isEmpty()) { etEmailRegister.setError(getString(R.string.email_harus_diisi)); return; }
            if (noHp.isEmpty()) { etPhoneRegister.setError(getString(R.string.no_hp_harus_diisi)); return; }
            if (alamat.isEmpty()) { etAlamatRegister.setError(getString(R.string.alamat_harus_diisi)); return; }
            if (password.isEmpty()) { etPasswordRegister.setError(getString(R.string.password_harus_diisi)); return; }

            if (!isPasswordValid(password)) {
                showDialog(getString(R.string.register_gagal),
                        "Password harus minimal 8 karakter, memiliki 1 huruf besar, dan 1 angka");
                return;
            }

            if (confirm.isEmpty()) { etConfirmRegister.setError(getString(R.string.konfirmasi_password_harus_diisi)); return; }

            if (!password.equals(confirm)) {
                showDialog(getString(R.string.register_gagal), getString(R.string.password_tidak_sama));
                return;
            }

            new RegisterTask().execute(nama, email, noHp, alamat, password);
        });
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
                .setPositiveButton(getString(R.string.tutup), (d, w) -> d.dismiss())
                .show();
    }

    private class RegisterTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... data) {
            HttpURLConnection conn = null;
            try {
                String nama = data[0];
                String email = data[1];
                String noHp = data[2];
                String alamat = data[3];
                String password = data[4];

                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("register", "UTF-8") + "&" +
                                URLEncoder.encode("nama", "UTF-8") + "=" + URLEncoder.encode(nama, "UTF-8") + "&" +
                                URLEncoder.encode("email", "UTF-8") + "=" + URLEncoder.encode(email, "UTF-8") + "&" +
                                URLEncoder.encode("no_hp", "UTF-8") + "=" + URLEncoder.encode(noHp, "UTF-8") + "&" +
                                URLEncoder.encode("alamat", "UTF-8") + "=" + URLEncoder.encode(alamat, "UTF-8") + "&" +
                                URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8");

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
                String message = jsonObject.getString("message");

                if (success) {
                    new AlertDialog.Builder(RegisterActivity.this)
                            .setTitle(getString(R.string.berhasil))
                            .setMessage("Akun berhasil dibuat! Silakan login.")
                            .setPositiveButton(getString(R.string.tutup), (dialog, which) -> {
                                dialog.dismiss();
                                finish();
                            })
                            .show();
                } else {
                    showDialog(getString(R.string.register_gagal), message);
                }
            } catch (Exception e) {
                showDialog(getString(R.string.register_gagal), "Response server tidak valid");
            }
        }
    }
}