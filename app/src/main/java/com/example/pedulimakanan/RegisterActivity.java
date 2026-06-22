package com.example.pedulimakanan;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class RegisterActivity extends Activity {

    private static final String CONNECTOR_URL = "http://139.162.46.52/pedulimakanan/connector.php";

    private static final String LAST_REGISTER_PREF = "last_register_data";
    private static final String KEY_LAST_NAMA = "last_nama";
    private static final String KEY_LAST_PASSWORD = "last_password";
    private static final String KEY_HAS_LAST_REGISTER = "has_last_register";
    private static final String KEY_LAST_REGISTER_TIME = "last_register_time";

    private static final int MAX_USERNAME_LENGTH = 20;

    private EditText etNamaRegister;
    private EditText etEmailRegister;
    private EditText etPhoneRegister;
    private AutoCompleteTextView etAlamatRegister;
    private EditText etPasswordRegister;
    private EditText etConfirmRegister;

    private boolean passwordVisible = false;
    private boolean confirmVisible = false;

    private final Handler addressHandler = new Handler(Looper.getMainLooper());
    private Runnable addressRunnable;

    private boolean isSelectingAddress = false;
    private boolean isAddressValid = false;
    private String selectedValidAddress = "";
    private int addressRequestCode = 0;

    private AddressSuggestionAdapter addressAdapter;
    private final ArrayList<String> addressSuggestions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        allowScreenRecord();

        setContentView(R.layout.activity_register);

        ImageButton btnBackRegister = findViewById(R.id.btnBackRegister);
        ImageButton btnEyeRegister = findViewById(R.id.btnEyeRegister);
        ImageButton btnEyeConfirmRegister = findViewById(R.id.btnEyeConfirmRegister);

        etNamaRegister = findViewById(R.id.etNamaRegister);
        etEmailRegister = findViewById(R.id.etEmailRegister);
        etPhoneRegister = findViewById(R.id.etPhoneRegister);
        etAlamatRegister = findViewById(R.id.etAlamatRegister);
        etPasswordRegister = findViewById(R.id.etPasswordRegister);
        etConfirmRegister = findViewById(R.id.etConfirmRegister);

        Button btnDaftar = findViewById(R.id.btnDaftar);

        etNamaRegister.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_USERNAME_LENGTH)});
        etNamaRegister.setInputType(InputType.TYPE_CLASS_TEXT);
        etNamaRegister.setHint("Username");

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

        setupUsernameWatcher();
        setupAddressAutocomplete();

        btnDaftar.setOnClickListener(v -> {
            String nama = etNamaRegister.getText().toString().trim();
            String email = etEmailRegister.getText().toString().trim();
            String noHp = etPhoneRegister.getText().toString().trim();
            String alamat = etAlamatRegister.getText().toString().trim();
            String password = etPasswordRegister.getText().toString().trim();
            String confirm = etConfirmRegister.getText().toString().trim();

            if (!isUsernameValid(nama)) {
                return;
            }

            if (email.isEmpty()) {
                etEmailRegister.setError(getString(R.string.email_harus_diisi));
                return;
            }

            if (noHp.isEmpty()) {
                etPhoneRegister.setError(getString(R.string.no_hp_harus_diisi));
                return;
            }

            if (alamat.isEmpty()) {
                etAlamatRegister.setError(getString(R.string.alamat_harus_diisi));
                return;
            }

            if (!isAddressValid || selectedValidAddress.isEmpty() || !alamat.equals(selectedValidAddress)) {
                etAlamatRegister.setError("Alamat tidak valid");
                Toast.makeText(
                        RegisterActivity.this,
                        "Alamat tidak valid. Pilih alamat dari rekomendasi Geoapify.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            if (password.isEmpty()) {
                etPasswordRegister.setError(getString(R.string.password_harus_diisi));
                return;
            }

            if (!isPasswordValid(password)) {
                showDialog(
                        getString(R.string.register_gagal),
                        "Password harus minimal 8 karakter, memiliki 1 huruf besar, dan 1 angka"
                );
                return;
            }

            if (confirm.isEmpty()) {
                etConfirmRegister.setError(getString(R.string.konfirmasi_password_harus_diisi));
                return;
            }

            if (!password.equals(confirm)) {
                showDialog(getString(R.string.register_gagal), getString(R.string.password_tidak_sama));
                return;
            }

            new RegisterTask().execute(nama, email, noHp, alamat, password);
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

    private void setupUsernameWatcher() {
        etNamaRegister.addTextChangedListener(new TextWatcher() {
            private boolean editing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (editing) {
                    return;
                }

                String oldText = s.toString();
                String cleanText = oldText.replaceAll("[^A-Za-z]", "");

                if (!oldText.equals(cleanText)) {
                    editing = true;
                    etNamaRegister.setText(cleanText);
                    etNamaRegister.setSelection(cleanText.length());
                    editing = false;

                    etNamaRegister.setError("Username hanya boleh huruf, tanpa spasi, angka, dan karakter unik");
                } else {
                    etNamaRegister.setError(null);
                }
            }
        });
    }

    private boolean isUsernameValid(String username) {
        if (username == null || username.trim().isEmpty()) {
            etNamaRegister.setError("Username harus diisi");
            return false;
        }

        if (username.length() > MAX_USERNAME_LENGTH) {
            etNamaRegister.setError("Username maksimal 20 huruf");
            return false;
        }

        if (username.contains(" ")) {
            etNamaRegister.setError("Username tidak boleh memakai spasi");
            return false;
        }

        if (!username.matches("^[A-Za-z]+$")) {
            etNamaRegister.setError("Username hanya boleh huruf, tanpa angka dan karakter unik");
            return false;
        }

        return true;
    }

    private void setupAddressAutocomplete() {
        addressAdapter = new AddressSuggestionAdapter();

        etAlamatRegister.setAdapter(addressAdapter);
        etAlamatRegister.setThreshold(3);
        etAlamatRegister.setDropDownHeight(dpToPx(240));
        etAlamatRegister.setDropDownVerticalOffset(dpToPx(4));

        etAlamatRegister.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && addressSuggestions.size() > 0) {
                etAlamatRegister.showDropDown();
            }
        });

        etAlamatRegister.setOnClickListener(v -> {
            if (addressSuggestions.size() > 0) {
                etAlamatRegister.showDropDown();
            }
        });

        etAlamatRegister.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= addressSuggestions.size()) {
                return;
            }

            if (addressRunnable != null) {
                addressHandler.removeCallbacks(addressRunnable);
            }

            addressRequestCode++;

            String selectedAddress = addressSuggestions.get(position);

            isSelectingAddress = true;

            selectedValidAddress = selectedAddress;
            isAddressValid = true;

            etAlamatRegister.setText(selectedAddress, false);
            etAlamatRegister.setSelection(selectedAddress.length());
            etAlamatRegister.setError(null);
            etAlamatRegister.dismissDropDown();

            isSelectingAddress = false;
        });

        etAlamatRegister.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isSelectingAddress) {
                    return;
                }

                String keyword = s.toString().trim();

                if (!keyword.equals(selectedValidAddress)) {
                    isAddressValid = false;
                    selectedValidAddress = "";
                    etAlamatRegister.setError(null);
                }

                if (addressRunnable != null) {
                    addressHandler.removeCallbacks(addressRunnable);
                }

                addressRequestCode++;

                if (keyword.length() < 3) {
                    addressSuggestions.clear();
                    addressAdapter.notifyDataSetChanged();
                    etAlamatRegister.dismissDropDown();
                    return;
                }

                int currentRequestCode = addressRequestCode;

                addressRunnable = () -> new AddressAutocompleteTask(keyword, currentRequestCode).execute();
                addressHandler.postDelayed(addressRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private class AddressSuggestionAdapter extends BaseAdapter implements Filterable {

        @Override
        public int getCount() {
            return addressSuggestions.size();
        }

        @Override
        public Object getItem(int position) {
            return addressSuggestions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = new TextView(RegisterActivity.this);

            tv.setText(addressSuggestions.get(position));
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(13);
            tv.setBackgroundColor(Color.WHITE);
            tv.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));
            tv.setMinHeight(dpToPx(54));
            tv.setSingleLine(false);
            tv.setMaxLines(3);

            return tv;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = addressSuggestions;
                    results.count = addressSuggestions.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    notifyDataSetChanged();
                }
            };
        }
    }

    private class AddressAutocompleteTask extends AsyncTask<Void, Void, ArrayList<String>> {

        private final String requestedInput;
        private final int requestCode;
        private String errorMessage = "";

        AddressAutocompleteTask(String requestedInput, int requestCode) {
            this.requestedInput = requestedInput;
            this.requestCode = requestCode;
        }

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            ArrayList<String> suggestions = new ArrayList<>();
            HttpURLConnection conn = null;

            try {
                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" +
                                URLEncoder.encode("address_autocomplete", "UTF-8") + "&" +
                                URLEncoder.encode("input", "UTF-8") + "=" +
                                URLEncoder.encode(requestedInput, "UTF-8");

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

                String response = result.toString();

                if (response.trim().isEmpty()) {
                    errorMessage = "Response server kosong";
                    return suggestions;
                }

                JSONObject jsonObject = new JSONObject(response);

                if (jsonObject.optBoolean("success", false)) {
                    JSONArray arr = jsonObject.optJSONArray("suggestions");

                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            suggestions.add(arr.getString(i));
                        }
                    }
                } else {
                    errorMessage = jsonObject.optString("message", "Gagal mengambil rekomendasi alamat");
                }

            } catch (Exception e) {
                errorMessage = e.getMessage() == null ? "Gagal mengambil alamat" : e.getMessage();

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            return suggestions;
        }

        @Override
        protected void onPostExecute(ArrayList<String> suggestions) {
            String currentInput = etAlamatRegister.getText().toString().trim();

            if (requestCode != addressRequestCode) {
                return;
            }

            if (!currentInput.equals(requestedInput)) {
                return;
            }

            if (currentInput.equals(selectedValidAddress) && isAddressValid) {
                return;
            }

            addressSuggestions.clear();
            addressSuggestions.addAll(suggestions);
            addressAdapter.notifyDataSetChanged();

            if (!suggestions.isEmpty() && etAlamatRegister.hasFocus()) {
                etAlamatRegister.postDelayed(() -> {
                    if (requestCode == addressRequestCode && etAlamatRegister.hasFocus()) {
                        etAlamatRegister.showDropDown();
                    }
                }, 150);
            } else {
                etAlamatRegister.dismissDropDown();

                if (!errorMessage.equals("")) {
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                } else if (currentInput.length() >= 3) {
                    etAlamatRegister.setError("Alamat tidak ditemukan");
                    Toast.makeText(
                            RegisterActivity.this,
                            "Alamat tidak ditemukan di Geoapify",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
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

    private void saveLastRegisterData(String nama, String password) {
        SharedPreferences prefs = getSharedPreferences(LAST_REGISTER_PREF, MODE_PRIVATE);

        prefs.edit()
                .putBoolean(KEY_HAS_LAST_REGISTER, true)
                .putString(KEY_LAST_NAMA, nama)
                .putString(KEY_LAST_PASSWORD, password)
                .putLong(KEY_LAST_REGISTER_TIME, System.currentTimeMillis())
                .apply();
    }

    private void showDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.tutup), (d, w) -> d.dismiss())
                .show();
    }

    private class RegisterTask extends AsyncTask<String, Void, String> {

        private String savedNama = "";
        private String savedPassword = "";

        @Override
        protected String doInBackground(String... data) {
            HttpURLConnection conn = null;

            try {
                String nama = data[0];
                String email = data[1];
                String noHp = data[2];
                String alamat = data[3];
                String password = data[4];

                savedNama = nama;
                savedPassword = password;

                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                String postData =
                        URLEncoder.encode("action", "UTF-8") + "=" +
                                URLEncoder.encode("register_pending", "UTF-8") + "&" +
                                URLEncoder.encode("nama", "UTF-8") + "=" +
                                URLEncoder.encode(nama, "UTF-8") + "&" +
                                URLEncoder.encode("email", "UTF-8") + "=" +
                                URLEncoder.encode(email, "UTF-8") + "&" +
                                URLEncoder.encode("no_hp", "UTF-8") + "=" +
                                URLEncoder.encode(noHp, "UTF-8") + "&" +
                                URLEncoder.encode("alamat", "UTF-8") + "=" +
                                URLEncoder.encode(alamat, "UTF-8") + "&" +
                                URLEncoder.encode("password", "UTF-8") + "=" +
                                URLEncoder.encode(password, "UTF-8");

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
                    saveLastRegisterData(savedNama, savedPassword);

                    int pendingId = jsonObject.optInt("pending_id", 0);
                    String email = jsonObject.optString("email", "");
                    String noHp = jsonObject.optString("no_hp", "");
                    String nextStep = jsonObject.optString("next_step", "email");

                    Intent intent;

                    if (nextStep.equals("phone")) {
                        intent = new Intent(RegisterActivity.this, ValidatePhoneActivity.class);
                    } else {
                        intent = new Intent(RegisterActivity.this, ValidateEmailActivity.class);
                    }

                    intent.putExtra("pending_id", pendingId);
                    intent.putExtra("email", email);
                    intent.putExtra("no_hp", noHp);
                    startActivity(intent);

                } else {
                    showDialog(getString(R.string.register_gagal), message);
                }

            } catch (Exception e) {
                showDialog(
                        getString(R.string.register_gagal),
                        "Response server tidak valid:\n" + response
                );
            }
        }
    }
}