package com.example.pedulimakanan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.imageview.ShapeableImageView;
import com.yalantis.ucrop.UCrop;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

@SuppressWarnings("deprecation")
public class ProfileActivity extends Activity {

    private static final String CONNECTOR_URL = "http://139.162.46.52/pedulimakanan/connector.php";
    private static final String PREF_NAME = "login_session";

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int UCROP_REQUEST_CODE = UCrop.REQUEST_CROP;
    private static final long MAX_IMAGE_SIZE_BYTES = 12 * 1024 * 1024;

    private static final int MAX_DISPLAY_NAME_LENGTH = 20;
    private static final int MAX_USERNAME_LENGTH = 20;

    private ShapeableImageView imgProfile;
    private TextView tvProfileName;

    private Button btnChangeEmail;
    private Button btnChangePhone;
    private Button btnChangeAddress;
    private Button btnChangeUsername;
    private Button btnChangePassword;
    private Button btnKeluar;

    private View layoutNavHome;
    private View layoutNavFavorit;
    private View layoutNavCart;
    private View layoutNavTransaksi;
    private View layoutNavProfile;

    private AlertDialog loadingDialog;

    private int userId = -1;

    private String currentEmail = "";
    private String currentPhone = "";
    private String currentProfileName = "";
    private String currentUsername = "";
    private String currentAddress = "";
    private String currentProfilePicture = "";

    private boolean firstLoadDone = false;

    private final Handler addressHandler = new Handler(Looper.getMainLooper());
    private Runnable addressRunnable;
    private boolean isSelectingAddress = false;
    private boolean isAddressValid = false;
    private String selectedValidAddress = "";
    private final ArrayList<String> addressSuggestions = new ArrayList<>();
    private AddressSuggestionAdapter addressAdapter;
    private AutoCompleteTextView activeAddressInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        userId = getIntent().getIntExtra("user_id", prefs.getInt("user_id", -1));

        imgProfile = findViewById(R.id.imgProfile);
        tvProfileName = findViewById(R.id.tvProfileName);

        btnChangeEmail = findViewById(R.id.btnChangeEmail);
        btnChangePhone = findViewById(R.id.btnChangePhone);
        btnChangeAddress = findViewById(R.id.btnChangeAddress);
        btnChangeUsername = findViewById(R.id.btnChangeUsername);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnKeluar = findViewById(R.id.btnKeluar);

        layoutNavHome = findViewById(R.id.layoutNavHome);
        layoutNavFavorit = findViewById(R.id.layoutNavFavorit);
        layoutNavCart = findViewById(R.id.layoutNavCart);
        layoutNavTransaksi = findViewById(R.id.layoutNavTransaksi);
        layoutNavProfile = findViewById(R.id.layoutNavProfile);

        setActiveNav();
        setupBottomNav();

        if (userId == -1) {
            showDialogMessage("Error", "User tidak ditemukan. Pastikan data login tersimpan dengan benar.");
            return;
        }

        new ProfileTask("get_profile").execute();

        imgProfile.setOnClickListener(v -> showProfilePictureMenu());

        tvProfileName.setClickable(true);
        tvProfileName.setFocusable(true);
        tvProfileName.setOnClickListener(v -> showChangeProfileNameDialog());

        btnChangeEmail.setOnClickListener(v -> showChangeEmailDialog());
        btnChangePhone.setOnClickListener(v -> showChangePhoneDialog());
        btnChangeAddress.setOnClickListener(v -> showChangeAddressDialog());
        btnChangeUsername.setOnClickListener(v -> showChangeUsernameDialog());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnKeluar.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void setupBottomNav() {
        layoutNavHome.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        layoutNavFavorit.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, FavoriteActivity.class);
            startActivity(intent);
            finish();
        });

        layoutNavCart.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, CartActivity.class);
            startActivity(intent);
            finish();
        });

        layoutNavTransaksi.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, TransactionActivity.class);
            startActivity(intent);
            finish();
        });

        layoutNavProfile.setOnClickListener(v -> {
            // Sudah di halaman Profile
        });
    }

    private void setActiveNav() {
        layoutNavHome.setBackgroundColor(Color.TRANSPARENT);
        layoutNavFavorit.setBackgroundColor(Color.TRANSPARENT);
        layoutNavCart.setBackgroundColor(Color.TRANSPARENT);
        layoutNavTransaksi.setBackgroundColor(Color.TRANSPARENT);
        layoutNavProfile.setBackgroundColor(Color.TRANSPARENT);

        layoutNavHome.setPadding(0, 0, 0, 0);
        layoutNavFavorit.setPadding(0, 0, 0, 0);
        layoutNavCart.setPadding(0, 0, 0, 0);
        layoutNavTransaksi.setPadding(0, 0, 0, 0);
        layoutNavProfile.setPadding(0, 0, 0, 0);
    }

    private void showProfilePictureMenu() {
        String[] menus = {"Upload Foto", "Reset Foto"};

        new AlertDialog.Builder(ProfileActivity.this)
                .setTitle("Foto Profile")
                .setItems(menus, (dialog, which) -> {
                    if (which == 0) {
                        openImagePicker();
                    } else if (which == 1) {
                        showResetProfilePictureConfirmation();
                    }
                })
                .show();
    }

    private void showResetProfilePictureConfirmation() {
        new AlertDialog.Builder(ProfileActivity.this)
                .setTitle("Reset Foto Profile")
                .setMessage("Apakah kamu yakin ingin menghapus foto profile?")
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Reset", (dialog, which) -> {
                    dialog.dismiss();
                    new ProfileTask("reset_profile_picture").execute();
                })
                .show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(intent, "Pilih Foto Profile"), PICK_IMAGE_REQUEST);
    }

    private Uri copyImageToCache(Uri sourceUri) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = getContentResolver().openInputStream(sourceUri);

            if (inputStream == null) {
                return null;
            }

            File cacheFile = new File(
                    getCacheDir(),
                    "source_profile_" + System.currentTimeMillis() + ".jpg"
            );

            outputStream = new FileOutputStream(cacheFile);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();

            return FileProvider.getUriForFile(
                    ProfileActivity.this,
                    getPackageName() + ".fileprovider",
                    cacheFile
            );

        } catch (Exception e) {
            showDialogMessage("Gagal", "Gagal membaca gambar: " + e.getMessage());
            return null;

        } finally {
            try {
                if (outputStream != null) outputStream.close();
            } catch (Exception ignored) {
            }

            try {
                if (inputStream != null) inputStream.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void startCrop(Uri sourceUri) {
        Uri cachedSourceUri = copyImageToCache(sourceUri);

        if (cachedSourceUri == null) {
            showDialogMessage("Gagal", "Gagal membaca gambar");
            return;
        }

        try {
            String destinationFileName = "cropped_profile_" + System.currentTimeMillis() + ".jpg";
            File destinationFile = new File(getCacheDir(), destinationFileName);

            Uri destinationUri = FileProvider.getUriForFile(
                    ProfileActivity.this,
                    getPackageName() + ".fileprovider",
                    destinationFile
            );

            grantUriPermission(
                    getPackageName(),
                    cachedSourceUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            grantUriPermission(
                    getPackageName(),
                    destinationUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            UCrop.Options options = new UCrop.Options();
            options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
            options.setCompressionQuality(90);
            options.setCircleDimmedLayer(true);
            options.setShowCropGrid(false);
            options.setShowCropFrame(false);
            options.setFreeStyleCropEnabled(false);
            options.setHideBottomControls(false);
            options.setToolbarTitle("Atur Foto Profile");

            UCrop.of(cachedSourceUri, destinationUri)
                    .withAspectRatio(1, 1)
                    .withMaxResultSize(800, 800)
                    .withOptions(options)
                    .start(ProfileActivity.this);

        } catch (Exception e) {
            showDialogMessage("Gagal", "Gagal membuka crop gambar: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            try {
                getContentResolver().takePersistableUriPermission(
                        imageUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (Exception ignored) {
            }

            String mimeType = getContentResolver().getType(imageUri);

            if (mimeType == null ||
                    (!mimeType.equals("image/png") &&
                            !mimeType.equals("image/jpg") &&
                            !mimeType.equals("image/jpeg") &&
                            !mimeType.equals("image/gif"))) {

                showDialogMessage("Gagal", "Format gambar harus PNG, JPG, JPEG, atau GIF");
                return;
            }

            long fileSize = getFileSizeFromUri(imageUri);

            if (fileSize > MAX_IMAGE_SIZE_BYTES) {
                showDialogMessage("Gagal", "Ukuran gambar maksimal 12 MB");
                return;
            }

            if (mimeType.equals("image/gif")) {
                loadLocalGif(imageUri);
                new UploadProfilePictureTask().execute(imageUri);
            } else {
                startCrop(imageUri);
            }
        }

        if (requestCode == UCROP_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri croppedUri = UCrop.getOutput(data);

            if (croppedUri != null) {
                long croppedSize = getFileSizeFromUri(croppedUri);

                if (croppedSize > MAX_IMAGE_SIZE_BYTES) {
                    showDialogMessage("Gagal", "Ukuran gambar maksimal 12 MB");
                    return;
                }

                try {
                    imgProfile.setImageURI(croppedUri);
                } catch (Exception ignored) {
                }

                new UploadProfilePictureTask().execute(croppedUri);
            }
        }

        if (resultCode == UCrop.RESULT_ERROR && data != null) {
            Throwable cropError = UCrop.getError(data);

            if (cropError != null && cropError.getMessage() != null) {
                showDialogMessage("Gagal", cropError.getMessage());
            } else {
                showDialogMessage("Gagal", "Crop gambar gagal");
            }
        }
    }

    private void loadLocalGif(Uri imageUri) {
        try {
            Glide.with(ProfileActivity.this).clear(imgProfile);

            Glide.with(ProfileActivity.this)
                    .asGif()
                    .load(imageUri)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .override(160, 160)
                    .placeholder(R.drawable.ic_profile_big)
                    .error(R.drawable.ic_profile_big)
                    .into(imgProfile);

        } catch (Exception e) {
            imgProfile.setImageResource(R.drawable.ic_profile_big);
        }
    }

    private void loadProfilePicture(String profilePicture) {
        if (profilePicture == null || profilePicture.trim().equals("") || profilePicture.equals("null")) {
            try {
                Glide.with(ProfileActivity.this).clear(imgProfile);
            } catch (Exception ignored) {
            }

            imgProfile.setImageResource(R.drawable.ic_profile_big);
            return;
        }

        String lowerUrl = profilePicture.toLowerCase();

        try {
            Glide.with(ProfileActivity.this).clear(imgProfile);

            if (lowerUrl.endsWith(".gif")) {
                Glide.with(ProfileActivity.this)
                        .asGif()
                        .load(profilePicture)
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .override(160, 160)
                        .placeholder(R.drawable.ic_profile_big)
                        .error(R.drawable.ic_profile_big)
                        .into(imgProfile);
            } else {
                Glide.with(ProfileActivity.this)
                        .load(profilePicture)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .override(256, 256)
                        .placeholder(R.drawable.ic_profile_big)
                        .error(R.drawable.ic_profile_big)
                        .into(imgProfile);
            }

        } catch (Exception e) {
            imgProfile.setImageResource(R.drawable.ic_profile_big);
        }
    }

    private long getFileSizeFromUri(Uri uri) {
        long size = -1;

        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);

            if (cursor != null) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

                if (sizeIndex >= 0 && cursor.moveToFirst()) {
                    size = cursor.getLong(sizeIndex);
                }

                cursor.close();
            }

            if (size <= 0) {
                InputStream inputStream = getContentResolver().openInputStream(uri);

                if (inputStream != null) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long total = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        total += bytesRead;
                    }

                    inputStream.close();
                    size = total;
                }
            }

        } catch (Exception e) {
            size = -1;
        }

        return size;
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(ProfileActivity.this)
                .setTitle("Konfirmasi Keluar")
                .setMessage("Apakah kamu yakin ingin keluar dari akun?")
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Keluar", (dialog, which) -> {
                    dialog.dismiss();

                    getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                            .edit()
                            .clear()
                            .apply();

                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private void showChangeEmailDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_single_input, null);

        TextView tvOldValue = view.findViewById(R.id.tvOldValue);
        EditText etNewValue = view.findViewById(R.id.etNewValue);

        tvOldValue.setText("Email sebelumnya: " + currentEmail);
        etNewValue.setHint("Masukkan email baru");
        etNewValue.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change Email")
                .setView(view)
                .setPositiveButton("Lanjut", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnSave.setOnClickListener(v -> {
                String newEmail = etNewValue.getText().toString().trim();

                if (newEmail.isEmpty()) {
                    etNewValue.setError("Email baru harus diisi");
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                    etNewValue.setError("Format email tidak valid");
                    return;
                }

                if (newEmail.equalsIgnoreCase(currentEmail)) {
                    etNewValue.setError("Email baru sama dengan email sebelumnya");
                    return;
                }

                dialog.dismiss();

                Intent intent = new Intent(ProfileActivity.this, ValidateProfileEmailActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("old_email", currentEmail);
                intent.putExtra("new_email", newEmail);
                startActivity(intent);
            });
        });

        dialog.show();
    }

    private void showChangePhoneDialog() {
        LinearLayout root = new LinearLayout(ProfileActivity.this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dpToPx(20), dpToPx(8), dpToPx(20), 0);

        TextView tvOldValue = new TextView(ProfileActivity.this);
        tvOldValue.setTextColor(Color.BLACK);
        tvOldValue.setTextSize(14);
        tvOldValue.setText("No telp sebelumnya: " + formatPhoneDisplay(currentPhone));

        LinearLayout phoneContainer = new LinearLayout(ProfileActivity.this);
        phoneContainer.setOrientation(LinearLayout.HORIZONTAL);
        phoneContainer.setGravity(Gravity.CENTER_VERTICAL);
        phoneContainer.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4));
        phoneContainer.setBackgroundResource(R.drawable.bg_input);

        LinearLayout.LayoutParams phoneContainerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(52)
        );
        phoneContainerParams.setMargins(0, dpToPx(12), 0, 0);
        phoneContainer.setLayoutParams(phoneContainerParams);

        TextView tvPrefix = new TextView(ProfileActivity.this);
        tvPrefix.setText("+62");
        tvPrefix.setTextColor(Color.BLACK);
        tvPrefix.setTextSize(15);
        tvPrefix.setGravity(Gravity.CENTER_VERTICAL);

        EditText etPhone = new EditText(ProfileActivity.this);
        etPhone.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        ));
        etPhone.setBackgroundColor(Color.TRANSPARENT);
        etPhone.setHint("81221222123");
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        etPhone.setTextColor(Color.BLACK);
        etPhone.setTextSize(15);
        etPhone.setSingleLine(true);
        etPhone.setPadding(dpToPx(8), 0, 0, 0);

        phoneContainer.addView(tvPrefix);
        phoneContainer.addView(etPhone);

        root.addView(tvOldValue);
        root.addView(phoneContainer);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change No Telp")
                .setView(root)
                .setPositiveButton("Lanjut", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnSave.setOnClickListener(v -> {
                String newPhone = etPhone.getText().toString().trim();
                newPhone = normalizePhoneForServer(newPhone);

                if (newPhone.isEmpty()) {
                    etPhone.setError("No telp baru harus diisi");
                    return;
                }

                if (newPhone.length() < 8) {
                    etPhone.setError("No telp terlalu pendek");
                    return;
                }

                if (newPhone.equals(normalizePhoneForServer(currentPhone))) {
                    etPhone.setError("No telp baru sama dengan sebelumnya");
                    return;
                }

                dialog.dismiss();

                Intent intent = new Intent(ProfileActivity.this, ValidateProfilePhoneActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("old_phone", currentPhone);
                intent.putExtra("new_phone", newPhone);
                startActivity(intent);
            });
        });

        dialog.show();
    }

    private void showChangeAddressDialog() {
        addressSuggestions.clear();
        selectedValidAddress = "";
        isAddressValid = false;
        isSelectingAddress = false;

        LinearLayout layout = new LinearLayout(ProfileActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(8), dpToPx(20), 0);

        TextView tvOldValue = new TextView(ProfileActivity.this);
        tvOldValue.setTextColor(Color.BLACK);
        tvOldValue.setTextSize(14);

        String oldAddress = currentAddress;

        if (oldAddress == null || oldAddress.trim().equals("") || oldAddress.equals("null")) {
            oldAddress = "Belum ada alamat";
        }

        tvOldValue.setText("Alamat sebelumnya:\n" + oldAddress);

        AutoCompleteTextView etNewAddress = new AutoCompleteTextView(ProfileActivity.this);
        etNewAddress.setHint("Masukkan alamat baru");
        etNewAddress.setTextColor(Color.BLACK);
        etNewAddress.setTextSize(14);
        etNewAddress.setSingleLine(false);
        etNewAddress.setMinLines(2);
        etNewAddress.setMaxLines(4);
        etNewAddress.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etNewAddress.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        etNewAddress.setThreshold(3);
        etNewAddress.setDropDownHeight(dpToPx(220));
        etNewAddress.setDropDownVerticalOffset(dpToPx(4));

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        inputParams.setMargins(0, dpToPx(12), 0, 0);
        etNewAddress.setLayoutParams(inputParams);

        activeAddressInput = etNewAddress;
        addressAdapter = new AddressSuggestionAdapter();
        etNewAddress.setAdapter(addressAdapter);

        etNewAddress.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= addressSuggestions.size()) return;

            isSelectingAddress = true;

            String selectedAddress = addressSuggestions.get(position);

            selectedValidAddress = selectedAddress;
            isAddressValid = true;

            etNewAddress.setText(selectedAddress);
            etNewAddress.setSelection(selectedAddress.length());
            etNewAddress.setError(null);
            etNewAddress.dismissDropDown();

            isSelectingAddress = false;
        });

        etNewAddress.setOnClickListener(v -> {
            if (!addressSuggestions.isEmpty()) {
                etNewAddress.showDropDown();
            }
        });

        etNewAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !addressSuggestions.isEmpty()) {
                etNewAddress.showDropDown();
            }
        });

        etNewAddress.addTextChangedListener(new TextWatcher() {
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
                }

                if (addressRunnable != null) {
                    addressHandler.removeCallbacks(addressRunnable);
                }

                if (keyword.length() < 3) {
                    addressSuggestions.clear();
                    addressAdapter.notifyDataSetChanged();
                    etNewAddress.dismissDropDown();

                    isAddressValid = false;
                    selectedValidAddress = "";
                    return;
                }

                addressRunnable = () -> new AddressAutocompleteTask(etNewAddress).execute(keyword);
                addressHandler.postDelayed(addressRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        layout.addView(tvOldValue);
        layout.addView(etNewAddress);

        AlertDialog dialog = new AlertDialog.Builder(ProfileActivity.this)
                .setTitle("Change Address")
                .setView(layout)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnSave.setOnClickListener(v -> {
                String newAddress = etNewAddress.getText().toString().trim();

                if (newAddress.isEmpty()) {
                    etNewAddress.setError("Alamat baru harus diisi");
                    return;
                }

                if (!isAddressValid || !newAddress.equals(selectedValidAddress)) {
                    etNewAddress.setError("Alamat tidak valid");
                    Toast.makeText(
                            ProfileActivity.this,
                            "Alamat tidak valid. Pilih alamat dari rekomendasi.",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                dialog.dismiss();
                new ProfileTask("update_address").execute(newAddress);
            });
        });

        dialog.setOnDismissListener(d -> {
            if (addressRunnable != null) {
                addressHandler.removeCallbacks(addressRunnable);
            }

            addressSuggestions.clear();
            selectedValidAddress = "";
            isAddressValid = false;
            isSelectingAddress = false;
            activeAddressInput = null;
        });

        dialog.show();
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
            TextView tv = new TextView(ProfileActivity.this);

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

    private class AddressAutocompleteTask extends AsyncTask<String, Void, ArrayList<String>> {

        private String errorMessage = "";
        private final AutoCompleteTextView targetInput;

        AddressAutocompleteTask(AutoCompleteTextView targetInput) {
            this.targetInput = targetInput;
        }

        @Override
        protected ArrayList<String> doInBackground(String... params) {
            ArrayList<String> suggestions = new ArrayList<>();
            HttpURLConnection conn = null;

            try {
                String input = params[0];

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
                                URLEncoder.encode(input, "UTF-8");

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
                errorMessage = e.getMessage();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            return suggestions;
        }

        @Override
        protected void onPostExecute(ArrayList<String> suggestions) {
            if (targetInput != activeAddressInput) {
                return;
            }

            addressSuggestions.clear();
            addressSuggestions.addAll(suggestions);

            if (addressAdapter != null) {
                addressAdapter.notifyDataSetChanged();
            }

            String currentInput = targetInput.getText().toString().trim();

            boolean exactMatch = false;

            for (String item : suggestions) {
                if (item.equalsIgnoreCase(currentInput)) {
                    exactMatch = true;
                    selectedValidAddress = item;
                    isAddressValid = true;
                    break;
                }
            }

            if (!exactMatch && !currentInput.equals(selectedValidAddress)) {
                isAddressValid = false;
            }

            if (!suggestions.isEmpty() && targetInput.hasFocus()) {
                targetInput.postDelayed(() -> {
                    if (targetInput == activeAddressInput) {
                        targetInput.requestFocus();
                        targetInput.showDropDown();
                    }
                }, 200);
            } else {
                targetInput.dismissDropDown();

                isAddressValid = false;
                selectedValidAddress = "";

                if (!errorMessage.equals("")) {
                    Toast.makeText(ProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                } else {
                    targetInput.setError("Alamat tidak ditemukan");
                    Toast.makeText(
                            ProfileActivity.this,
                            "Alamat tidak ditemukan",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        }
    }

    private String normalizePhoneForServer(String phone) {
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
        String clean = normalizePhoneForServer(phone);

        if (clean.isEmpty()) {
            return "+62";
        }

        return "+62 " + clean;
    }

    private boolean isDisplayNameValid(String name, EditText input) {
        if (name == null || name.trim().isEmpty()) {
            input.setError("Display name harus diisi");
            return false;
        }

        if (name.length() > MAX_DISPLAY_NAME_LENGTH) {
            input.setError("Display name maksimal 20 karakter termasuk spasi");
            return false;
        }

        return true;
    }

    private boolean isUsernameValid(String username, EditText input) {
        if (username == null || username.trim().isEmpty()) {
            input.setError("Username baru harus diisi");
            return false;
        }

        if (username.length() > MAX_USERNAME_LENGTH) {
            input.setError("Username maksimal 20 huruf");
            return false;
        }

        if (username.contains(" ")) {
            input.setError("Username tidak boleh memakai spasi");
            return false;
        }

        if (!username.matches("^[A-Za-z]+$")) {
            input.setError("Username hanya boleh huruf, tanpa angka dan karakter unik");
            return false;
        }

        return true;
    }
    private void showChangeProfileNameDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_single_input, null);

        TextView tvOldValue = view.findViewById(R.id.tvOldValue);
        EditText etNewValue = view.findViewById(R.id.etNewValue);

        tvOldValue.setText("Display name sebelumnya: " + currentProfileName);
        etNewValue.setHint("Maksimal 12 huruf");
        etNewValue.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change Display Name")
                .setView(view)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnSave.setOnClickListener(v -> {
                String newName = etNewValue.getText().toString().trim();

                if (!isDisplayNameValid(newName, etNewValue)) {
                    return;
                }

                if (newName.equals(currentProfileName)) {
                    etNewValue.setError("Display name baru sama dengan sebelumnya");
                    return;
                }

                dialog.dismiss();
                new ProfileTask("update_profile_name").execute(newName);
            });
        });

        dialog.show();
    }

    private void showChangeUsernameDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_single_input, null);

        TextView tvOldValue = view.findViewById(R.id.tvOldValue);
        EditText etNewValue = view.findViewById(R.id.etNewValue);

        tvOldValue.setText("Username sebelumnya: " + currentUsername);
        etNewValue.setHint("Huruf saja, maksimal 20");
        etNewValue.setInputType(InputType.TYPE_CLASS_TEXT);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Username")
                .setView(view)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnSave.setOnClickListener(v -> {
                String newUsername = etNewValue.getText().toString().trim();

                if (!isUsernameValid(newUsername, etNewValue)) {
                    return;
                }

                if (newUsername.equalsIgnoreCase(currentUsername)) {
                    etNewValue.setError("Username baru sama dengan sebelumnya");
                    return;
                }

                dialog.dismiss();
                new ProfileTask("update_username").execute(newUsername);
            });
        });

        dialog.show();
    }

    private void showChangePasswordDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);

        EditText etOldPassword = view.findViewById(R.id.etOldPassword);
        EditText etNewPassword = view.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        TextView tvForgotPassword = view.findViewById(R.id.tvForgotPassword);

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Password")
                .setView(view)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnSave.setOnClickListener(v -> {
                String oldPassword = etOldPassword.getText().toString().trim();
                String newPassword = etNewPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                if (oldPassword.isEmpty()) {
                    etOldPassword.setError("Password sebelumnya harus diisi");
                    return;
                }

                if (newPassword.isEmpty()) {
                    etNewPassword.setError("Password baru harus diisi");
                    return;
                }

                if (!isPasswordValid(newPassword)) {
                    showDialogMessage(
                            "Password tidak valid",
                            "Password harus minimal 8 karakter, memiliki 1 huruf besar, dan 1 angka"
                    );
                    return;
                }

                if (confirmPassword.isEmpty()) {
                    etConfirmPassword.setError("Konfirmasi password harus diisi");
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    showDialogMessage("Gagal", "Password baru dan konfirmasi tidak sama");
                    return;
                }

                dialog.dismiss();
                new ProfileTask("update_password").execute(oldPassword, newPassword, confirmPassword);
            });
        });

        dialog.show();
    }

    private boolean isPasswordValid(String password) {
        return password.matches("^(?=.*[A-Z])(?=.*\\d).{8,}$");
    }

    private class ProfileTask extends AsyncTask<String, Void, String> {

        private String action;
        private String submittedValue = "";

        ProfileTask(String action) {
            this.action = action;
        }

        @Override
        protected String doInBackground(String... data) {
            if (data.length > 0) {
                submittedValue = data[0];
            }

            HttpURLConnection conn = null;

            try {
                URL url = new URL(CONNECTOR_URL);
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                StringBuilder postData = new StringBuilder();

                postData.append(URLEncoder.encode("action", "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(action, "UTF-8"))
                        .append("&")
                        .append(URLEncoder.encode("user_id", "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(String.valueOf(userId), "UTF-8"));

                if (action.equals("update_email")) {
                    postData.append("&email=")
                            .append(URLEncoder.encode(data[0], "UTF-8"));

                } else if (action.equals("update_phone")) {
                    postData.append("&no_hp=")
                            .append(URLEncoder.encode(data[0], "UTF-8"));

                } else if (action.equals("update_profile_name")) {
                    postData.append("&profile_name=")
                            .append(URLEncoder.encode(data[0], "UTF-8"));

                } else if (action.equals("update_address")) {
                    postData.append("&alamat=")
                            .append(URLEncoder.encode(data[0], "UTF-8"));

                } else if (action.equals("update_username")) {
                    postData.append("&nama=")
                            .append(URLEncoder.encode(data[0], "UTF-8"));

                } else if (action.equals("update_password")) {
                    postData.append("&old_password=")
                            .append(URLEncoder.encode(data[0], "UTF-8"))
                            .append("&new_password=")
                            .append(URLEncoder.encode(data[1], "UTF-8"))
                            .append("&confirm_password=")
                            .append(URLEncoder.encode(data[2], "UTF-8"));
                }

                OutputStream os = conn.getOutputStream();

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(postData.toString());
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
                    if (action.equals("get_profile")) {
                        currentUsername = jsonObject.optString("nama", "");
                        currentEmail = jsonObject.optString("email", "");
                        currentPhone = jsonObject.optString("no_hp", "");
                        currentProfileName = jsonObject.optString("profile_name", "");
                        currentAddress = jsonObject.optString("alamat", "");

                        if (currentProfileName.equals("") || currentProfileName.equals("null")) {
                            currentProfileName = currentUsername;
                        }

                        tvProfileName.setText(currentProfileName);

                        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                        editor.putString("nama", currentUsername);
                        editor.putString("profile_name", currentProfileName);
                        editor.putString("alamat", currentAddress);
                        editor.apply();

                        currentProfilePicture = jsonObject.optString("profile_picture", "");
                        loadProfilePicture(currentProfilePicture);
                        firstLoadDone = true;

                    } else if (action.equals("reset_profile_picture")) {
                        showDialogMessage("Berhasil", message);
                        loadProfilePicture("");
                        new ProfileTask("get_profile").execute();

                    } else {
                        if (action.equals("update_profile_name")) {
                            currentProfileName = submittedValue;
                            tvProfileName.setText(currentProfileName);

                            SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                            editor.putString("profile_name", currentProfileName);
                            editor.apply();

                        } else if (action.equals("update_username")) {
                            currentUsername = submittedValue;

                            SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                            editor.putString("nama", currentUsername);
                            editor.apply();

                        } else if (action.equals("update_address")) {
                            currentAddress = submittedValue;

                            SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                            editor.putString("alamat", currentAddress);
                            editor.apply();
                        }

                        showDialogMessage("Berhasil", message);
                        new ProfileTask("get_profile").execute();
                    }

                } else {
                    showDialogMessage("Gagal", message);

                    if (!action.equals("get_profile")) {
                        new ProfileTask("get_profile").execute();
                    }
                }

            } catch (Exception e) {
                showDialogMessage("Gagal", "Response server tidak valid:\n" + limitText(response));
            }
        }
    }

    private class UploadProfilePictureTask extends AsyncTask<Uri, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            setUploadUiEnabled(false);
            showLoadingDialog("Mengupload foto profile...\nMohon tunggu sampai selesai");
        }

        @Override
        protected String doInBackground(Uri... uris) {
            HttpURLConnection conn = null;

            try {
                Uri imageUri = uris[0];

                String boundary = "----PeduliMakananBoundary" + System.currentTimeMillis();
                URL url = new URL(CONNECTOR_URL);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                conn.setConnectTimeout(60000);
                conn.setReadTimeout(60000);

                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                writeFormField(dos, boundary, "action", "upload_profile_picture");
                writeFormField(dos, boundary, "user_id", String.valueOf(userId));

                String fileName = getFileNameFromUri(imageUri);
                String mimeType = getContentResolver().getType(imageUri);

                if (mimeType == null) {
                    mimeType = "image/jpeg";
                }

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"profile_picture\"; filename=\"" + fileName + "\"\r\n");
                dos.writeBytes("Content-Type: " + mimeType + "\r\n\r\n");

                InputStream inputStream = getContentResolver().openInputStream(imageUri);

                if (inputStream == null) {
                    return "{\"success\":false,\"message\":\"Gagal membaca gambar\"}";
                }

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }

                inputStream.close();

                dos.writeBytes("\r\n");
                dos.writeBytes("--" + boundary + "--\r\n");
                dos.flush();
                dos.close();

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
                return "{\"success\":false,\"message\":\"Upload gagal: " + e.getMessage() + "\"}";
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String response) {
            hideLoadingDialog();
            setUploadUiEnabled(true);

            try {
                JSONObject jsonObject = new JSONObject(response);

                boolean success = jsonObject.getBoolean("success");
                String message = jsonObject.optString("message", "");

                if (success) {
                    showDialogMessage("Berhasil", message);
                } else {
                    showDialogMessage("Gagal", message);
                }

                new ProfileTask("get_profile").execute();

            } catch (Exception e) {
                showDialogMessage("Gagal", "Response server tidak valid:\n" + limitText(response));
                new ProfileTask("get_profile").execute();
            }
        }

        @Override
        protected void onCancelled() {
            hideLoadingDialog();
            setUploadUiEnabled(true);
            super.onCancelled();
        }
    }

    private void setUploadUiEnabled(boolean enabled) {
        imgProfile.setEnabled(enabled);
        tvProfileName.setEnabled(enabled);
        btnChangeEmail.setEnabled(enabled);
        btnChangePhone.setEnabled(enabled);
        btnChangeAddress.setEnabled(enabled);
        btnChangeUsername.setEnabled(enabled);
        btnChangePassword.setEnabled(enabled);
        btnKeluar.setEnabled(enabled);

        layoutNavHome.setEnabled(enabled);
        layoutNavFavorit.setEnabled(enabled);
        layoutNavCart.setEnabled(enabled);
        layoutNavTransaksi.setEnabled(enabled);
        layoutNavProfile.setEnabled(enabled);
    }

    private void showLoadingDialog(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            return;
        }

        LinearLayout layout = new LinearLayout(ProfileActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dpToPx(28), dpToPx(24), dpToPx(28), dpToPx(24));

        ProgressBar progressBar = new ProgressBar(ProfileActivity.this);
        progressBar.setIndeterminate(true);

        TextView tvMessage = new TextView(ProfileActivity.this);
        tvMessage.setText(message);
        tvMessage.setTextSize(15);
        tvMessage.setTextColor(Color.BLACK);
        tvMessage.setGravity(Gravity.CENTER);
        tvMessage.setPadding(0, dpToPx(14), 0, 0);

        layout.addView(progressBar);
        layout.addView(tvMessage);

        loadingDialog = new AlertDialog.Builder(ProfileActivity.this)
                .setView(layout)
                .create();

        loadingDialog.setCancelable(false);
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void writeFormField(DataOutputStream dos, String boundary, String name, String value) throws Exception {
        dos.writeBytes("--" + boundary + "\r\n");
        dos.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        dos.writeBytes(value + "\r\n");
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "profile_picture.jpg";

        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

            if (nameIndex >= 0 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex);
            }

            cursor.close();
        }

        return fileName;
    }

    private String limitText(String text) {
        if (text == null || text.trim().equals("")) {
            return "Response kosong dari server";
        }

        if (text.length() > 300) {
            return text.substring(0, 300);
        }

        return text;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (userId != -1 && firstLoadDone) {
            new ProfileTask("get_profile").execute();
        }
    }

    @Override
    protected void onDestroy() {
        hideLoadingDialog();

        try {
            Glide.with(ProfileActivity.this).clear(imgProfile);
        } catch (Exception ignored) {
        }

        super.onDestroy();
    }

    private void showDialogMessage(String title, String message) {
        new AlertDialog.Builder(ProfileActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Tutup", (dialog, which) -> dialog.dismiss())
                .show();
    }
}