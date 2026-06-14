package com.example.pedulimakanan;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

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
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint("SetTextI18n")
@SuppressWarnings("deprecation")
public class CartActivity extends Activity {

    private static final String CONNECTOR_URL = "http://139.162.46.52/pedulimakanan/connector.php";
    private static final String PREF_NAME = "login_session";
    private static final int REQUEST_PROMO_CART = 1001;
    private static final int BIAYA_ONGKIR = 6000;

    private int userId;
    private int saldoUser = 0;

    private LinearLayout llCartContainer;
    private TextView tvCartSubtotal;
    private TextView tvCartPromoText;
    private TextView tvCartPromoDiscount;
    private TextView tvCartTotal;
    private TextView tvSaldoInfo;
    private TextView tvSaldoWarning;
    private Button btnCheckoutCart;
    private Button btnPromoCart;

    private final List<Integer> selectedCartIds = new ArrayList<>();

    private int totalHargaTerpilih = 0;
    private int totalBayarFinal = 0;
    private int restoranIdAktif = -1;

    private int promoDiscount = 0;
    private boolean promoFreeOngkir = false;
    private String promoText = "";

    private JSONArray latestCartArray = new JSONArray();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Locale localeId = new Locale("id", "ID");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        llCartContainer = findViewById(R.id.llCartContainer);
        tvCartSubtotal = findViewById(R.id.tvCartSubtotal);
        tvCartPromoText = findViewById(R.id.tvCartPromoText);
        tvCartPromoDiscount = findViewById(R.id.tvCartPromoDiscount);
        tvCartTotal = findViewById(R.id.tvCartTotal);
        tvSaldoInfo = findViewById(R.id.tvSaldoInfo);
        tvSaldoWarning = findViewById(R.id.tvSaldoWarning);
        btnCheckoutCart = findViewById(R.id.btnCheckoutCart);
        btnPromoCart = findViewById(R.id.btnPromoCart);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnPromoCart != null) {
            btnPromoCart.setOnClickListener(v -> openPromoActivity());
        }

        if (btnCheckoutCart != null) {
            btnCheckoutCart.setOnClickListener(v -> prosesCheckout());
        }

        setupBottomNav();
        updatePaymentSummary();
        updateCheckoutButtonState();
        loadCartData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCartData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }

    private void setupBottomNav() {
        LinearLayout layoutNavHome = findViewById(R.id.layoutNavHome);
        LinearLayout layoutNavFavorit = findViewById(R.id.layoutNavFavorit);
        LinearLayout layoutNavCart = findViewById(R.id.layoutNavCart);
        LinearLayout layoutNavTransaksi = findViewById(R.id.layoutNavTransaksi);
        LinearLayout layoutNavProfile = findViewById(R.id.layoutNavProfile);

        layoutNavHome.setOnClickListener(v -> {
            Intent intent = new Intent(CartActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        layoutNavFavorit.setOnClickListener(v -> {
            Intent intent = new Intent(CartActivity.this, FavoriteActivity.class);
            startActivity(intent);
            finish();
        });

        layoutNavCart.setOnClickListener(v -> {
        });

        layoutNavTransaksi.setOnClickListener(v -> {
            Intent intent = new Intent(CartActivity.this, TransactionActivity.class);
            startActivity(intent);
            finish();
        });

        layoutNavProfile.setOnClickListener(v -> {
            Intent intent = new Intent(CartActivity.this, ProfileActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
            finish();
        });
    }

    private void clearAppliedPromo() {
        promoDiscount = 0;
        promoFreeOngkir = false;
        promoText = "";
    }

    private void applyLocalCalculation(JSONArray cartArray) {
        totalHargaTerpilih = 0;
        restoranIdAktif = -1;

        try {
            for (int i = 0; i < cartArray.length(); i++) {
                JSONObject obj = cartArray.getJSONObject(i);
                int cartId = obj.getInt("id");

                if (selectedCartIds.contains(cartId)) {
                    int harga = obj.getInt("harga");
                    int jumlah = obj.getInt("jumlah");

                    totalHargaTerpilih += harga * jumlah;

                    if (restoranIdAktif == -1) {
                        restoranIdAktif = obj.optInt("restoran_id", -1);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (selectedCartIds.isEmpty()) {
            clearAppliedPromo();
        }

        updatePaymentSummary();
        updateCheckoutButtonState();
    }

    private void updatePaymentSummary() {
        int usedDiscount = Math.min(promoDiscount, totalHargaTerpilih);
        int ongkirAsli = selectedCartIds.isEmpty() ? 0 : BIAYA_ONGKIR;
        int ongkirFinal = promoFreeOngkir ? 0 : ongkirAsli;

        totalBayarFinal = Math.max(0, totalHargaTerpilih + ongkirFinal - usedDiscount);

        if (tvCartSubtotal != null) {
            tvCartSubtotal.setText("Rp " + formatRupiah(totalHargaTerpilih));
        }

        if (promoText == null || promoText.trim().isEmpty()) {
            if (tvCartPromoText != null) {
                tvCartPromoText.setText("-");
            }

            if (tvCartPromoDiscount != null) {
                tvCartPromoDiscount.setText("- Rp 0");
            }
        } else {
            if (tvCartPromoText != null) {
                tvCartPromoText.setText(formatPromoTextForDisplay(promoText));
                tvCartPromoText.setSingleLine(false);
                tvCartPromoText.setMaxLines(10);
                tvCartPromoText.setGravity(android.view.Gravity.END);
            }

            if (tvCartPromoDiscount != null) {
                int totalPotongan = usedDiscount;

                if (promoFreeOngkir) {
                    totalPotongan += ongkirAsli;
                }

                tvCartPromoDiscount.setText("- Rp " + formatRupiah(totalPotongan));
            }
        }

        if (tvCartTotal != null) {
            tvCartTotal.setText("Rp " + formatRupiah(totalBayarFinal));
        }
    }

    private void updateCheckoutButtonState() {
        if (btnCheckoutCart == null || tvSaldoWarning == null) {
            return;
        }

        boolean adaItemDipilih = !selectedCartIds.isEmpty();
        boolean saldoCukup = saldoUser >= totalBayarFinal;

        if (btnPromoCart != null) {
            if (adaItemDipilih) {
                btnPromoCart.setEnabled(true);
                btnPromoCart.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#246E9B")));
                btnPromoCart.setText("Pilih Promo");
            } else {
                btnPromoCart.setEnabled(false);
                btnPromoCart.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                btnPromoCart.setText("Pilih Promo");
            }
        }

        if (!adaItemDipilih) {
            tvSaldoWarning.setVisibility(View.GONE);
            btnCheckoutCart.setEnabled(false);
            btnCheckoutCart.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            btnCheckoutCart.setText("Proses Checkout");
            return;
        }

        if (!saldoCukup) {
            tvSaldoWarning.setVisibility(View.VISIBLE);
            tvSaldoWarning.setText("Saldo Anda kurang. Silakan top up saldo terlebih dahulu.");
            btnCheckoutCart.setEnabled(false);
            btnCheckoutCart.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            btnCheckoutCart.setText("Saldo Tidak Cukup");
            return;
        }

        tvSaldoWarning.setVisibility(View.GONE);
        btnCheckoutCart.setEnabled(true);
        btnCheckoutCart.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        btnCheckoutCart.setText("Proses Checkout");
    }

    private void openPromoActivity() {
        if (selectedCartIds.isEmpty()) {
            return;
        }

        ArrayList<String> restoranNames = getSelectedRestaurantNames();

        if (restoranNames.isEmpty()) {
            Toast.makeText(this, "Restoran belum terbaca", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(CartActivity.this, PromoActivity.class);
        intent.putExtra("subtotal", totalHargaTerpilih);
        intent.putExtra("ongkir", BIAYA_ONGKIR);
        intent.putExtra("restoran_nama", restoranNames.get(0));
        intent.putStringArrayListExtra("restoran_nama_list", restoranNames);

        startActivityForResult(intent, REQUEST_PROMO_CART);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PROMO_CART && resultCode == RESULT_OK && data != null) {
            promoDiscount = data.getIntExtra("promo_diskon", data.getIntExtra("discount", 0));
            promoFreeOngkir = data.getBooleanExtra("free_ongkir", false);

            promoText = data.getStringExtra("promo_kode");

            if (promoText == null || promoText.trim().isEmpty()) {
                promoText = data.getStringExtra("promo_text");
            }

            if (promoText == null) {
                promoText = "";
            }

            if (promoDiscount > totalHargaTerpilih) {
                promoDiscount = totalHargaTerpilih;
            }

            Toast.makeText(
                    this,
                    "Diterima Cart: " + promoText + " | Diskon: " + promoDiscount,
                    Toast.LENGTH_LONG
            ).show();

            updatePaymentSummary();
            updateCheckoutButtonState();
        }
    }

    private ArrayList<String> getSelectedRestaurantNames() {
        Set<String> names = new LinkedHashSet<>();

        try {
            for (int i = 0; i < latestCartArray.length(); i++) {
                JSONObject obj = latestCartArray.getJSONObject(i);
                int cartId = obj.getInt("id");

                if (selectedCartIds.contains(cartId)) {
                    String restoName = obj.optString("restoran_nama", "").trim();

                    if (restoName.isEmpty() || restoName.equalsIgnoreCase("null")) {
                        restoName = obj.optString("nama_resto", "").trim();
                    }

                    if (restoName.isEmpty() || restoName.equalsIgnoreCase("null")) {
                        restoName = getRestaurantNameByMenu(obj.optString("nama_item", ""));
                    }

                    if (!restoName.trim().isEmpty()) {
                        names.add(restoName.trim());
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return new ArrayList<>(names);
    }

    private String getRestaurantNameByMenu(String nama) {
        if (nama == null) {
            return "";
        }

        String n = nama.toLowerCase(Locale.US).trim();

        switch (n) {
            case "tuna san":
            case "hail caesar":
            case "mini bowl":
            case "protein mix":
                return "Saladstop";
            case "brown rice bowl":
            case "quinoa power":
            case "grain classic":
            case "green detox":
                return "Supergrain";
            case "chicken bowl":
            case "wrap veggie":
            case "fruit bowl":
            case "vegan burger":
                return "Burgreen";
            case "tropical blast":
            case "green power":
            case "berry bliss":
            case "peanut butter boo":
            case "detox cleanse":
                return "SmoothieBar";
            case "granola bar":
            case "edamame cup":
            case "mixed nuts":
            case "rice cake":
                return "NutriSnack";
            default:
                return "";
        }
    }

    private void prosesCheckout() {
        if (selectedCartIds.isEmpty()) {
            Toast.makeText(this, "Pilih item yang ingin dibayar!", Toast.LENGTH_SHORT).show();
            updateCheckoutButtonState();
            return;
        }

        updatePaymentSummary();

        if (saldoUser < totalBayarFinal) {
            Toast.makeText(this, "Saldo Anda kurang untuk melakukan checkout", Toast.LENGTH_SHORT).show();
            updateCheckoutButtonState();
            return;
        }

        JSONArray selectedArray = new JSONArray();

        for (int id : selectedCartIds) {
            selectedArray.put(id);
        }

        int ongkirAsli = selectedCartIds.isEmpty() ? 0 : BIAYA_ONGKIR;
        int usedDiscount = Math.min(promoDiscount, totalHargaTerpilih);
        int totalPotonganPromo = usedDiscount;

        if (promoFreeOngkir) {
            totalPotonganPromo += ongkirAsli;
        }

        checkoutCart(
                selectedArray.toString(),
                String.valueOf(totalBayarFinal),
                String.valueOf(totalPotonganPromo),
                promoText == null ? "" : promoText,
                String.valueOf(ongkirAsli)
        );
    }

    private String formatRupiah(int amount) {
        return NumberFormat.getInstance(localeId).format(amount);
    }

    private String formatPromoTextForDisplay(String rawPromoText) {
        if (rawPromoText == null || rawPromoText.trim().isEmpty()) {
            return "-";
        }

        String[] codes = rawPromoText.split("\\s*\\+\\s*");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < codes.length; i++) {
            if (i > 0) {
                if (i % 3 == 0) {
                    sb.append("\n");
                } else {
                    sb.append(" + ");
                }
            }

            sb.append(codes[i].trim());
        }

        return sb.toString();
    }

    private int getMenuImage(String nama) {
        if (nama == null) {
            return R.drawable.img_placeholder_food;
        }

        switch (nama.toLowerCase(Locale.US).trim()) {
            case "tuna san":
                return R.drawable.img_tuna_san;
            case "hail caesar":
                return R.drawable.img_hail_caesar;
            case "mini bowl":
                return R.drawable.img_mini_bowl;
            case "protein mix":
                return R.drawable.img_protein_mix;
            case "brown rice bowl":
                return R.drawable.img_brown_rice_bowl;
            case "quinoa power":
                return R.drawable.img_quinoa_power;
            case "grain classic":
                return R.drawable.img_grain_classic;
            case "green detox":
                return R.drawable.img_green_detox;
            case "chicken bowl":
                return R.drawable.img_chicken_bowl;
            case "wrap veggie":
                return R.drawable.img_wrap_veggie;
            case "fruit bowl":
                return R.drawable.img_fruit_bowl;
            case "vegan burger":
                return R.drawable.img_vegan_burger;
            case "smoothie bowl":
                return R.drawable.img_smoothie_bowl;
            case "tropical blast":
                return R.drawable.img_tropical_blast;
            case "green power":
                return R.drawable.img_green_power;
            case "berry bliss":
                return R.drawable.img_berry_bliss;
            case "peanut butter boo":
                return R.drawable.img_peanut_butter_boo;
            case "detox cleanse":
                return R.drawable.img_detox_clease;
            case "granola bar":
                return R.drawable.img_granola_bar;
            case "edamame cup":
                return R.drawable.img_edamame_cup;
            case "mixed nuts":
                return R.drawable.img_mixed_nuts;
            case "rice cake":
                return R.drawable.img_rice_cake;
            default:
                return R.drawable.img_placeholder_food;
        }
    }

    private void showEmptyCartBox() {
        llCartContainer.removeAllViews();
        selectedCartIds.clear();
        clearAppliedPromo();

        totalHargaTerpilih = 0;
        totalBayarFinal = 0;
        restoranIdAktif = -1;
        latestCartArray = new JSONArray();

        updatePaymentSummary();
        updateCheckoutButtonState();

        View emptyView = LayoutInflater.from(this)
                .inflate(R.layout.item_empty_cart, llCartContainer, false);

        Button btnCariMakanan = emptyView.findViewById(R.id.btnCariMakananCart);

        btnCariMakanan.setOnClickListener(v -> {
            Intent intent = new Intent(CartActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        llCartContainer.addView(emptyView);
    }

    private void loadCartData() {
        executorService.execute(() -> {
            String postData =
                    enc("action") + "=" + enc("get_keranjang_server") + "&" +
                            enc("user_id") + "=" + enc(String.valueOf(userId));

            String response = requestServer(postData);

            mainHandler.post(() -> handleLoadCartResponse(response));
        });
    }

    private void handleLoadCartResponse(String response) {
        if (response == null) {
            showEmptyCartBox();
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(response);

            if (jsonObject.optBoolean("success", false)) {
                saldoUser = jsonObject.optInt("saldo", 0);
                tvSaldoInfo.setText("Saldo Anda: Rp " + formatRupiah(saldoUser));

                llCartContainer.removeAllViews();

                JSONArray cartArray = jsonObject.getJSONArray("keranjang");
                latestCartArray = cartArray;

                if (cartArray.length() == 0) {
                    showEmptyCartBox();
                    return;
                }

                sanitizeSelectedIds(cartArray);

                for (int i = 0; i < cartArray.length(); i++) {
                    JSONObject obj = cartArray.getJSONObject(i);

                    int cartId = obj.getInt("id");
                    String nama = obj.getString("nama_item");
                    int harga = obj.getInt("harga");
                    int jumlah = obj.getInt("jumlah");
                    int subtotalItem = harga * jumlah;

                    View row = LayoutInflater.from(CartActivity.this)
                            .inflate(R.layout.item_cart_row, llCartContainer, false);

                    ImageView imgCartItem = row.findViewById(R.id.imgCartItem);
                    TextView tvNama = row.findViewById(R.id.tvCartItemNama);
                    TextView tvHarga = row.findViewById(R.id.tvCartItemHarga);
                    TextView tvQty = row.findViewById(R.id.tvCartItemQty);
                    TextView tvSubtotal = row.findViewById(R.id.tvCartItemSubtotal);
                    CheckBox cb = row.findViewById(R.id.cbCartItem);

                    imgCartItem.setImageResource(getMenuImage(nama));
                    tvNama.setText(nama);
                    tvHarga.setText("Harga: Rp " + formatRupiah(harga));
                    tvQty.setText("x" + jumlah);
                    tvSubtotal.setText("Total: Rp " + formatRupiah(subtotalItem));

                    cb.setChecked(selectedCartIds.contains(cartId));

                    cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        clearAppliedPromo();

                        if (isChecked) {
                            if (!selectedCartIds.contains(cartId)) {
                                selectedCartIds.add(cartId);
                            }
                        } else {
                            selectedCartIds.remove(Integer.valueOf(cartId));
                        }

                        applyLocalCalculation(cartArray);
                    });

                    row.findViewById(R.id.btnHapusItem).setOnClickListener(v -> {
                        selectedCartIds.remove(Integer.valueOf(cartId));
                        clearAppliedPromo();
                        deleteCartItem(String.valueOf(cartId));
                    });

                    llCartContainer.addView(row);
                }

                applyLocalCalculation(cartArray);

            } else {
                showEmptyCartBox();
            }

        } catch (Exception e) {
            showEmptyCartBox();
        }
    }

    private void sanitizeSelectedIds(JSONArray cartArray) {
        Set<Integer> validIds = new LinkedHashSet<>();

        try {
            for (int i = 0; i < cartArray.length(); i++) {
                validIds.add(cartArray.getJSONObject(i).getInt("id"));
            }

            for (int i = selectedCartIds.size() - 1; i >= 0; i--) {
                if (!validIds.contains(selectedCartIds.get(i))) {
                    selectedCartIds.remove(i);
                    clearAppliedPromo();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void deleteCartItem(String cartId) {
        executorService.execute(() -> {
            String postData =
                    enc("action") + "=" + enc("hapus_item_keranjang_server") + "&" +
                            enc("cart_id") + "=" + enc(cartId);

            String response = requestServer(postData);
            boolean success = false;

            try {
                if (response != null) {
                    success = new JSONObject(response).optBoolean("success", false);
                }
            } catch (Exception ignored) {
            }

            boolean finalSuccess = success;

            mainHandler.post(() -> {
                if (finalSuccess) {
                    Toast.makeText(CartActivity.this, "Item dihapus dari keranjang", Toast.LENGTH_SHORT).show();
                    loadCartData();
                } else {
                    Toast.makeText(CartActivity.this, "Gagal menghapus item", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void checkoutCart(String cartIdsJson, String totalHarga, String promoDiskon, String promoKode, String ongkir) {
        executorService.execute(() -> {
            String postData =
                    enc("action") + "=" + enc("checkout_keranjang_server") + "&" +
                            enc("user_id") + "=" + enc(String.valueOf(userId)) + "&" +
                            enc("restoran_id") + "=" + enc(String.valueOf(restoranIdAktif)) + "&" +
                            enc("total_harga") + "=" + enc(totalHarga) + "&" +
                            enc("promo_diskon") + "=" + enc(promoDiskon) + "&" +
                            enc("discount") + "=" + enc(promoDiskon) + "&" +
                            enc("promo_kode") + "=" + enc(promoKode) + "&" +
                            enc("promo_text") + "=" + enc(promoKode) + "&" +
                            enc("ongkir") + "=" + enc(ongkir) + "&" +
                            enc("cart_ids") + "=" + enc(cartIdsJson);

            String response = requestServer(postData);

            mainHandler.post(() -> handleCheckoutResponse(response));
        });
    }

    private void handleCheckoutResponse(String response) {
        if (response == null) {
            Toast.makeText(CartActivity.this, "Koneksi checkout gagal", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(response);

            if (jsonObject.optBoolean("success", false)) {
                Toast.makeText(CartActivity.this, "Pembayaran Berhasil!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(CartActivity.this, TransactionActivity.class);
                startActivity(intent);
                finish();

            } else {
                Toast.makeText(
                        CartActivity.this,
                        jsonObject.optString("message", "Gagal checkout"),
                        Toast.LENGTH_LONG
                ).show();

                loadCartData();
            }

        } catch (Exception e) {
            Toast.makeText(CartActivity.this, "Response server error", Toast.LENGTH_SHORT).show();
        }
    }

    private String requestServer(String postData) {
        HttpURLConnection conn = null;

        try {
            URL url = new URL(CONNECTOR_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
            writer.write(postData);
            writer.flush();
            writer.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            reader.close();
            return result.toString();

        } catch (Exception e) {
            return null;

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String enc(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return "";
        }
    }
}