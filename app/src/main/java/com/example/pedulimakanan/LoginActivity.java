package com.example.pedulimakanan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

    private static final String CONNECTOR_URL = "http://172.104.183.200/pedulimakanan/connector.php";

    private static final String PREF_NAME = "login_session";
    private static final String KEY_IS_LOGIN = "is_login";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAMA = "nama";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NO_HP = "no_hp";

    private static final String LAST_LOGIN_PREF = "last_login_data";
    private static final String KEY_HAS_LAST_LOGIN = "has_last_login";
    private static final String KEY_LAST_LOGIN_NAMA = "last_login_nama";
    private static final String KEY_LAST_LOGIN_PASSWORD = "last_login_password";

    private EditText etNamaLogin;
    private EditText etPasswordLogin;

    private boolean passwordVisible = false;
    private boolean lastLoginDialogShowing = false;

    private String pendingLoginNama = "";
    private String pendingLoginPassword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isUserAlreadyLogin()) {
            goToHomeFromSavedSession();
            return;
        }

        setContentView(R.layout.activity_login);

        etNamaLogin = findViewById(R.id.etNamaLogin);
        etPasswordLogin = findViewById(R.id.etPasswordLogin);

        ImageButton btnEyeLogin = findViewById(R.id.btnEyeLogin);
        TextView tvLupaSandi = findViewById(R.id.tvLupaSandi);
        TextView tvDaftarSekarang = findViewById(R.id.tvDaftarSekarang);
        Button btnMasuk = findViewById(R.id.btnMasuk);

        btnEyeLogin.setImageResource(R.drawable.ic_eye_close);
        btnEyeLogin.setContentDescription(getString(R.string.tampilkan_password));

        btnEyeLogin.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            togglePassword(etPasswordLogin, btnEyeLogin, passwordVisible);
        });

        etNamaLogin.setOnClickListener(v -> showLastLoginDialog());

        etNamaLogin.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showLastLoginDialog();
            }
        });

        tvLupaSandi.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        tvDaftarSekarang.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );

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

            pendingLoginNama = nama;
            pendingLoginPassword = password;

            new LoginTask().execute(nama, password);
        });
    }

    private boolean isUserAlreadyLogin() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return sharedPreferences.getBoolean(KEY_IS_LOGIN, false);
    }

    private void goToHomeFromSavedSession() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        int userId = sharedPreferences.getInt(KEY_USER_ID, -1);
        String nama = sharedPreferences.getString(KEY_NAMA, "");
        String email = sharedPreferences.getString(KEY_EMAIL, "");
        String noHp = sharedPreferences.getString(KEY_NO_HP, "");

        if (userId == -1) {
            clearLoginSession();
            setContentView(R.layout.activity_login);
            return;
        }

        goToHome(userId, nama, email, noHp);
    }

    private void saveLoginSession(int userId, String nama, String email, String noHp) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        sharedPreferences.edit()
                .putBoolean(KEY_IS_LOGIN, true)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_NAMA, nama)
                .putString(KEY_EMAIL, email)
                .putString(KEY_NO_HP, noHp)
                .apply();
    }

    private void clearLoginSession() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
    }

    private void saveLastLoginData(String nama, String password) {
        SharedPreferences prefs = getSharedPreferences(LAST_LOGIN_PREF, MODE_PRIVATE);

        prefs.edit()
                .putBoolean(KEY_HAS_LAST_LOGIN, true)
                .putString(KEY_LAST_LOGIN_NAMA, nama)
                .putString(KEY_LAST_LOGIN_PASSWORD, password)
                .apply();
    }

    private void showLastLoginDialog() {
        if (lastLoginDialogShowing) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences(LAST_LOGIN_PREF, MODE_PRIVATE);

        boolean hasLastLogin = prefs.getBoolean(KEY_HAS_LAST_LOGIN, false);
        String lastNama = prefs.getString(KEY_LAST_LOGIN_NAMA, "");
        String lastPassword = prefs.getString(KEY_LAST_LOGIN_PASSWORD, "");

        if (!hasLastLogin || lastNama.trim().isEmpty() || lastPassword.trim().isEmpty()) {
            return;
        }

        if (!etNamaLogin.getText().toString().trim().isEmpty()
                || !etPasswordLogin.getText().toString().trim().isEmpty()) {
            return;
        }

        lastLoginDialogShowing = true;

        new AlertDialog.Builder(LoginActivity.this)
                .setTitle("Akun terakhir login")
                .setMessage("Lanjut menggunakan akun: " + lastNama + "?")
                .setNegativeButton("Tidak", (dialog, which) -> {
                    dialog.dismiss();
                    lastLoginDialogShowing = false;
                })
                .setPositiveButton("Lanjut", (dialog, which) -> {
                    dialog.dismiss();

                    etNamaLogin.setText(lastNama);
                    etPasswordLogin.setText(lastPassword);
                    etPasswordLogin.setSelection(etPasswordLogin.getText().length());

                    lastLoginDialogShowing = false;
                })
                .setOnCancelListener(dialog -> lastLoginDialogShowing = false)
                .show();
    }

    private void showSaveLoginConfirmation(int userId, String nama, String email, String noHp) {
        new AlertDialog.Builder(LoginActivity.this)
                .setTitle("Simpan Data Login")
                .setMessage("Apakah kamu ingin menyimpan data login di device ini?")
                .setNegativeButton("Tidak", (dialog, which) -> {
                    dialog.dismiss();
                    goToHome(userId, nama, email, noHp);
                })
                .setPositiveButton("Simpan", (dialog, which) -> {
                    dialog.dismiss();
                    saveLoginSession(userId, nama, email, noHp);
                    goToHome(userId, nama, email, noHp);
                })
                .show();
    }

    private void goToHome(int userId, String nama, String email, String noHp) {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("nama", nama);
        intent.putExtra("email", email);
        intent.putExtra("no_hp", noHp);
        startActivity(intent);
        finish();
    }

    private void togglePassword(EditText editText, ImageButton imageButton, boolean visible) {
        if (visible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            imageButton.setImageResource(R.drawable.ic_eye);
            imageButton.setContentDescription(getString(R.string.sembunyikan_password));
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            imageButton.setImageResource(R.drawable.ic_eye_close);
            imageButton.setContentDescription(getString(R.string.tampilkan_password));
        }

        editText.setSelection(editText.getText().length());
    }

    private void showDialogMessage(String title, String message) {
        new AlertDialog.Builder(LoginActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.tutup), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private class LoginTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... data) {
            HttpURLConnection conn = null;

            try {
                String nama = data[0];
                String password = data[1];

                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

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
                    int userId = jsonObject.getInt("id");
                    String nama = jsonObject.optString("nama", "");
                    String email = jsonObject.optString("email", "");
                    String noHp = jsonObject.optString("no_hp", "");

                    saveLastLoginData(pendingLoginNama, pendingLoginPassword);

                    showSaveLoginConfirmation(userId, nama, email, noHp);

                } else {
                    showDialogMessage(getString(R.string.login_gagal), message);
                }

            } catch (Exception e) {
                showDialogMessage(getString(R.string.login_gagal), "Response server tidak valid:\n" + response);
            }
        }
    }
}