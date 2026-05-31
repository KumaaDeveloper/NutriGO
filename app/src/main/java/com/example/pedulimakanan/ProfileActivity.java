package com.example.pedulimakanan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.imageview.ShapeableImageView;
import com.yalantis.ucrop.UCrop;

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

@SuppressWarnings("deprecation")
public class ProfileActivity extends Activity {

    private static final String CONNECTOR_URL = "http://172.104.183.200/pedulimakanan/connector.php";

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int UCROP_REQUEST_CODE = UCrop.REQUEST_CROP;
    private static final long MAX_IMAGE_SIZE_BYTES = 12 * 1024 * 1024;

    private ShapeableImageView imgProfile;
    private TextView tvProfileName;

    private Button btnChangeEmail;
    private Button btnChangePhone;
    private Button btnChangeProfileName;
    private Button btnChangeUsername;
    private Button btnChangePassword;
    private Button btnKeluar;

    private ImageButton navHome;
    private ImageButton navFavorite;
    private ImageButton navCart;
    private ImageButton navOrder;
    private ImageButton navProfile;

    private AlertDialog loadingDialog;

    private int userId = -1;

    private String currentEmail = "";
    private String currentPhone = "";
    private String currentProfileName = "";
    private String currentUsername = "";
    private String currentProfilePicture = "";

    private boolean firstLoadDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userId = getIntent().getIntExtra("user_id", -1);

        imgProfile = findViewById(R.id.imgProfile);
        tvProfileName = findViewById(R.id.tvProfileName);

        btnChangeEmail = findViewById(R.id.btnChangeEmail);
        btnChangePhone = findViewById(R.id.btnChangePhone);
        btnChangeProfileName = findViewById(R.id.btnChangeProfileName);
        btnChangeUsername = findViewById(R.id.btnChangeUsername);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnKeluar = findViewById(R.id.btnKeluar);

        navHome = findViewById(R.id.navHome);
        navFavorite = findViewById(R.id.navFavorite);
        navCart = findViewById(R.id.navCart);
        navOrder = findViewById(R.id.navOrder);
        navProfile = findViewById(R.id.navProfile);

        setActiveNav();

        if (userId == -1) {
            showDialogMessage("Error", "User tidak ditemukan. Pastikan LoginActivity mengirim user_id ke HomeActivity dan ProfileActivity.");
            return;
        }

        new ProfileTask("get_profile").execute();

        imgProfile.setOnClickListener(v -> showProfilePictureMenu());

        btnChangeEmail.setOnClickListener(v -> showChangeEmailDialog());
        btnChangePhone.setOnClickListener(v -> showChangePhoneDialog());
        btnChangeProfileName.setOnClickListener(v -> showChangeProfileNameDialog());
        btnChangeUsername.setOnClickListener(v -> showChangeUsernameDialog());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnKeluar.setOnClickListener(v -> showLogoutConfirmation());

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
            finish();
        });

        navFavorite.setOnClickListener(v -> showDialogMessage("Info", "Halaman favorit belum dibuat"));
        navCart.setOnClickListener(v -> showDialogMessage("Info", "Halaman keranjang belum dibuat"));
        navOrder.setOnClickListener(v -> showDialogMessage("Info", "Halaman pesanan belum dibuat"));
        navProfile.setOnClickListener(v -> {
        });
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
        try {
            InputStream inputStream = getContentResolver().openInputStream(sourceUri);

            if (inputStream == null) {
                return null;
            }

            File cacheFile = new File(getCacheDir(), "source_profile_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(cacheFile);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            return Uri.fromFile(cacheFile);

        } catch (Exception e) {
            return null;
        }
    }

    private void startCrop(Uri sourceUri) {
        Uri cachedSourceUri = copyImageToCache(sourceUri);

        if (cachedSourceUri == null) {
            showDialogMessage("Gagal", "Gagal membaca gambar");
            return;
        }

        String destinationFileName = "cropped_profile_" + System.currentTimeMillis() + ".jpg";
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), destinationFileName));

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

                imgProfile.setImageURI(croppedUri);
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

    private void setActiveNav() {
        navHome.setBackgroundColor(Color.TRANSPARENT);
        navFavorite.setBackgroundColor(Color.TRANSPARENT);
        navCart.setBackgroundColor(Color.TRANSPARENT);
        navOrder.setBackgroundColor(Color.TRANSPARENT);
        navProfile.setBackgroundResource(R.drawable.bg_nav_active);

        navHome.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        navFavorite.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        navCart.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        navOrder.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        navProfile.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
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

                    getSharedPreferences("login_session", MODE_PRIVATE)
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
        etNewValue.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change Email")
                .setView(view)
                .setPositiveButton("Simpan", null)
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

                dialog.dismiss();
                new ProfileTask("update_email").execute(newEmail);
            });
        });

        dialog.show();
    }

    private void showChangePhoneDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_single_input, null);

        TextView tvOldValue = view.findViewById(R.id.tvOldValue);
        EditText etNewValue = view.findViewById(R.id.etNewValue);

        tvOldValue.setText("No telp sebelumnya: " + currentPhone);
        etNewValue.setHint("Masukkan no telp baru");
        etNewValue.setInputType(InputType.TYPE_CLASS_PHONE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change No Telp")
                .setView(view)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnSave.setOnClickListener(v -> {
                String newPhone = etNewValue.getText().toString().trim();

                if (newPhone.isEmpty()) {
                    etNewValue.setError("No telp baru harus diisi");
                    return;
                }

                dialog.dismiss();
                new ProfileTask("update_phone").execute(newPhone);
            });
        });

        dialog.show();
    }

    private void showChangeProfileNameDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_single_input, null);

        TextView tvOldValue = view.findViewById(R.id.tvOldValue);
        EditText etNewValue = view.findViewById(R.id.etNewValue);

        tvOldValue.setText("Profile name sebelumnya: " + currentProfileName);
        etNewValue.setHint("Masukkan profile name baru");
        etNewValue.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Profile Name")
                .setView(view)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnSave.setOnClickListener(v -> {
                String newName = etNewValue.getText().toString().trim();

                if (newName.isEmpty()) {
                    etNewValue.setError("Profile name harus diisi");
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
        etNewValue.setHint("Masukkan username baru");
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

                if (newUsername.isEmpty()) {
                    etNewValue.setError("Username baru harus diisi");
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

        ProfileTask(String action) {
            this.action = action;
        }

        @Override
        protected String doInBackground(String... data) {
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

                        if (currentProfileName.equals("") || currentProfileName.equals("null")) {
                            currentProfileName = currentUsername;
                        }

                        tvProfileName.setText(currentProfileName);

                        currentProfilePicture = jsonObject.optString("profile_picture", "");
                        loadProfilePicture(currentProfilePicture);
                        firstLoadDone = true;

                    } else if (action.equals("reset_profile_picture")) {
                        showDialogMessage("Berhasil", message);
                        loadProfilePicture("");
                        new ProfileTask("get_profile").execute();

                    } else {
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
        btnChangeEmail.setEnabled(enabled);
        btnChangePhone.setEnabled(enabled);
        btnChangeProfileName.setEnabled(enabled);
        btnChangeUsername.setEnabled(enabled);
        btnChangePassword.setEnabled(enabled);
        btnKeluar.setEnabled(enabled);

        navHome.setEnabled(enabled);
        navFavorite.setEnabled(enabled);
        navCart.setEnabled(enabled);
        navOrder.setEnabled(enabled);
        navProfile.setEnabled(enabled);
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