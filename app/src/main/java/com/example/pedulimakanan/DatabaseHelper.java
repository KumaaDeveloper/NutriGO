package com.example.pedulimakanan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "db_pedulimakanan.db";
    private static final int    DB_VERSION = 2;

    // ── Table names ──────────────────────────────────────────────────
    public static final String TABLE_USERS           = "users";
    public static final String TABLE_RESTORAN        = "restoran";
    public static final String TABLE_MENU            = "menu";
    public static final String TABLE_TRANSAKSI       = "transaksi";
    public static final String TABLE_DETAIL_TRANSAKSI= "detail_transaksi";
    public static final String TABLE_FAVORIT         = "favorit";
    public static final String TABLE_PENDING_USERS   = "pending_users";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ── onCreate: create all tables + seed sample data ────────────────
    @Override
    public void onCreate(SQLiteDatabase db) {
        // USERS
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nama TEXT NOT NULL," +
                "email TEXT UNIQUE NOT NULL," +
                "no_hp TEXT," +
                "password TEXT NOT NULL," +
                "kode_verifikasi TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "alamat_default TEXT" +
                ")");

        // PENDING_USERS (holds registrations awaiting verification if needed)
        db.execSQL("CREATE TABLE " + TABLE_PENDING_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nama TEXT," +
                "email TEXT," +
                "no_hp TEXT," +
                "password TEXT," +
                "kode_verifikasi TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

        // RESTORAN
        db.execSQL("CREATE TABLE " + TABLE_RESTORAN + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nama_resto TEXT NOT NULL," +
                "alamat_resto TEXT," +
                "kategori TEXT," +
                "rating REAL DEFAULT 0," +
                "gambar_url TEXT" +
                ")");

        // MENU
        db.execSQL("CREATE TABLE " + TABLE_MENU + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "restoran_id INTEGER NOT NULL," +
                "nama_item TEXT NOT NULL," +
                "harga INTEGER NOT NULL," +
                "deskripsi TEXT," +
                "gambar_url TEXT," +
                "is_tersedia INTEGER DEFAULT 1," +
                "FOREIGN KEY(restoran_id) REFERENCES restoran(id)" +
                ")");

        // TRANSAKSI
        db.execSQL("CREATE TABLE " + TABLE_TRANSAKSI + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "restoran_id INTEGER NOT NULL," +
                "total_harga INTEGER," +
                "status_pesanan TEXT DEFAULT 'Selesai'," +
                "tanggal TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY(user_id) REFERENCES users(id)," +
                "FOREIGN KEY(restoran_id) REFERENCES restoran(id)" +
                ")");

        // DETAIL_TRANSAKSI
        db.execSQL("CREATE TABLE " + TABLE_DETAIL_TRANSAKSI + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "transaksi_id INTEGER NOT NULL," +
                "menu_id INTEGER NOT NULL," +
                "jumlah INTEGER NOT NULL," +
                "harga_saat_ini INTEGER NOT NULL," +
                "FOREIGN KEY(transaksi_id) REFERENCES transaksi(id)," +
                "FOREIGN KEY(menu_id) REFERENCES menu(id)" +
                ")");

        // FAVORIT
        db.execSQL("CREATE TABLE " + TABLE_FAVORIT + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "restoran_id INTEGER NOT NULL," +
                "FOREIGN KEY(user_id) REFERENCES users(id)," +
                "FOREIGN KEY(restoran_id) REFERENCES restoran(id)" +
                ")");

        seedData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DETAIL_TRANSAKSI);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSAKSI);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORIT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MENU);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESTORAN);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENDING_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // ── Seed sample restaurants & menus ──────────────────────────────
    private void seedData(SQLiteDatabase db) {
        // ---- Restaurants (id assigned in order: 1, 2, 3, 4) ----
        insertResto(db, "Saladstop",  "Jl. Sudirman No. 1",  "Salad",      4.8f, "saladstop");
        insertResto(db, "Supergrain", "Jl. Thamrin No. 5",   "Grain Bowl", 4.3f, "supergrain");
        insertResto(db, "GreenBowl",  "Jl. Kuningan No. 7",  "Bowls",      3.8f, "greenbowl");
        insertResto(db, "FreshBox",   "Jl. Senayan No. 2",   "Healthy",    3.5f, "freshbox");
        insertResto(db, "Burgreen",   "Jl. Kemang No. 10",   "Vegan",      4.5f, "burgreen");

        // ---- Menu for Saladstop (id=1) ----
        insertMenu(db, 1, "Tuna San",    85000, "Salad tuna segar dengan alpukat, tomat, saus madu", "", 1);
        insertMenu(db, 1, "Hail Caesar", 80000, "Caesar klasik dengan ayam panggang dan parmesan",   "", 1);
        insertMenu(db, 1, "Mini Bowl",   68000, "Pilihan sayuran segar dengan protein pilihanmu",    "", 1);
        insertMenu(db, 1, "Protein Mix", 95000, "Campuran protein tinggi: telur, tuna, edamame",     "", 1);

        // ---- Menu for Supergrain (id=2) ----
        insertMenu(db, 2, "Brown Rice Bowl", 72000, "Nasi merah dengan sayuran dan saus kacang",          "", 1);
        insertMenu(db, 2, "Quinoa Power",    88000, "Quinoa dengan avocado, edamame, dan dressing lemon", "", 1);
        insertMenu(db, 2, "Grain Classic",   65000, "Mix biji-bijian dengan topping ayam dan sayuran",    "", 1);

        // ---- Menu for GreenBowl (id=3) ----
        insertMenu(db, 3, "Green Detox",  58000, "Campuran sayuran hijau dengan dressing jahe", "", 1);
        insertMenu(db, 3, "Chicken Bowl", 62000, "Ayam panggang dengan brokoli dan wortel",     "", 1);

        // ---- Menu for FreshBox (id=4) ----
        insertMenu(db, 4, "Wrap Veggie", 55000, "Wrap dengan sayuran segar dan hummus", "", 1);
        insertMenu(db, 4, "Fruit Bowl",  48000, "Campuran buah segar musiman",          "", 1);

        // ---- Menu for Burgreen (id=5) ----
        insertMenu(db, 5, "Vegan Burger",  75000, "Burger vegan dengan patty jamur dan saus tomat", "", 1);
        insertMenu(db, 5, "Smoothie Bowl", 65000, "Acai smoothie bowl dengan granola dan buah",     "", 1);
    }

    private void insertResto(SQLiteDatabase db, String nama, String alamat,
                             String kategori, float rating, String gambar) {
        ContentValues cv = new ContentValues();
        cv.put("nama_resto",   nama);
        cv.put("alamat_resto", alamat);
        cv.put("kategori",     kategori);
        cv.put("rating",       rating);
        cv.put("gambar_url",   gambar);
        db.insert(TABLE_RESTORAN, null, cv);
    }

    private void insertMenu(SQLiteDatabase db, int restoranId, String nama,
                            int harga, String deskripsi, String gambar, int tersedia) {
        ContentValues cv = new ContentValues();
        cv.put("restoran_id",  restoranId);
        cv.put("nama_item",    nama);
        cv.put("harga",        harga);
        cv.put("deskripsi",    deskripsi);
        cv.put("gambar_url",   gambar);
        cv.put("is_tersedia",  tersedia);
        db.insert(TABLE_MENU, null, cv);
    }

    // ════════════════════════════════════════════════════════════════
    //  AUTH
    // ════════════════════════════════════════════════════════════════

    /** Returns user id on success, -1 if wrong credentials */
    public int login(String nama, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id FROM " + TABLE_USERS +
                        " WHERE nama=? AND password=?",
                new String[]{nama, password});
        int userId = -1;
        if (c.moveToFirst()) userId = c.getInt(0);
        c.close();
        return userId;
    }

    /** Returns true if registration succeeded */
    public boolean register(String nama, String email, String noHp, String alamat, String password) {
        // Check duplicate email
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id FROM " + TABLE_USERS + " WHERE email=?",
                new String[]{email});
        boolean exists = c.moveToFirst();
        c.close();
        if (exists) return false;

        ContentValues cv = new ContentValues();
        cv.put("nama",           nama);
        cv.put("email",          email);
        cv.put("no_hp",          noHp);
        cv.put("alamat_default", alamat);
        cv.put("password",       password);

        long row = getWritableDatabase().insert(TABLE_USERS, null, cv);
        return row != -1;
    }

    /** Returns the new password-reset code, or null if user not found */
    public String requestResetCode(String identifier, String method) {
        SQLiteDatabase db  = getReadableDatabase();
        String column      = method.equals("email") ? "email" : "no_hp";
        Cursor c = db.rawQuery(
                "SELECT id FROM " + TABLE_USERS + " WHERE " + column + "=?",
                new String[]{identifier});
        if (!c.moveToFirst()) { c.close(); return null; }
        int userId = c.getInt(0);
        c.close();

        String code = String.valueOf((int)(Math.random() * 900000) + 100000);
        ContentValues cv = new ContentValues();
        cv.put("kode_verifikasi", code);
        getWritableDatabase().update(TABLE_USERS, cv, "id=?",
                new String[]{String.valueOf(userId)});
        return code;
    }

    /** Returns true if reset succeeded */
    public boolean resetPassword(String identifier, String method,
                                 String kode, String passwordBaru) {
        SQLiteDatabase db = getReadableDatabase();
        String column     = method.equals("email") ? "email" : "no_hp";
        Cursor c = db.rawQuery(
                "SELECT id, kode_verifikasi FROM " + TABLE_USERS +
                        " WHERE " + column + "=?",
                new String[]{identifier});
        if (!c.moveToFirst()) { c.close(); return false; }
        int    userId       = c.getInt(0);
        String storedCode   = c.getString(1);
        c.close();

        if (!kode.equals(storedCode)) return false;

        ContentValues cv = new ContentValues();
        cv.put("password",         passwordBaru);
        cv.put("kode_verifikasi",  "");
        getWritableDatabase().update(TABLE_USERS, cv, "id=?",
                new String[]{String.valueOf(userId)});
        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  RESTORAN
    // ════════════════════════════════════════════════════════════════

    public Cursor getAllRestoran() {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_RESTORAN + " ORDER BY rating DESC", null);
    }

    // ════════════════════════════════════════════════════════════════
    //  MENU
    // ════════════════════════════════════════════════════════════════

    public Cursor getMenuByRestoran(int restoranId) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_MENU +
                        " WHERE restoran_id=? AND is_tersedia=1",
                new String[]{String.valueOf(restoranId)});
    }

    // ════════════════════════════════════════════════════════════════
    //  TRANSAKSI
    // ════════════════════════════════════════════════════════════════

    /**
     * Creates a full order from a cart.
     * @param userId      logged-in user
     * @param restoranId  store being ordered from
     * @param cartItems   JSONArray of {menu_id, nama_item, harga, jumlah}
     * @param totalHarga  pre-calculated total
     * @return new transaksi id, or -1 on failure
     */
    public long buatTransaksi(int userId, int restoranId,
                              JSONArray cartItems, int totalHarga) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Insert header
            ContentValues cv = new ContentValues();
            cv.put("user_id",       userId);
            cv.put("restoran_id",   restoranId);
            cv.put("total_harga",   totalHarga);
            cv.put("status_pesanan","Selesai");
            long transaksiId = db.insert(TABLE_TRANSAKSI, null, cv);
            if (transaksiId == -1) return -1;

            // Insert detail rows
            for (int i = 0; i < cartItems.length(); i++) {
                JSONObject item = cartItems.getJSONObject(i);
                ContentValues dcv = new ContentValues();
                dcv.put("transaksi_id",   transaksiId);
                dcv.put("menu_id",        item.getInt("menu_id"));
                dcv.put("jumlah",         item.getInt("jumlah"));
                dcv.put("harga_saat_ini", item.getInt("harga"));
                db.insert(TABLE_DETAIL_TRANSAKSI, null, dcv);
            }

            db.setTransactionSuccessful();
            return transaksiId;
        } catch (Exception e) {
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    /** Returns all transactions for a user, newest first */
    public Cursor getTransaksiByUser(int userId) {
        return getReadableDatabase().rawQuery(
                "SELECT t.*, r.nama_resto FROM " + TABLE_TRANSAKSI + " t " +
                        "JOIN " + TABLE_RESTORAN + " r ON t.restoran_id = r.id " +
                        "WHERE t.user_id=? ORDER BY t.tanggal DESC",
                new String[]{String.valueOf(userId)});
    }

    /** Returns detail rows for one transaction */
    public Cursor getDetailTransaksi(int transaksiId) {
        return getReadableDatabase().rawQuery(
                "SELECT dt.*, m.nama_item FROM " + TABLE_DETAIL_TRANSAKSI + " dt " +
                        "JOIN " + TABLE_MENU + " m ON dt.menu_id = m.id " +
                        "WHERE dt.transaksi_id=?",
                new String[]{String.valueOf(transaksiId)});
    }

    // ════════════════════════════════════════════════════════════════
    //  FAVORIT
    // ════════════════════════════════════════════════════════════════

    public boolean isFavorit(int userId, int restoranId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id FROM " + TABLE_FAVORIT +
                        " WHERE user_id=? AND restoran_id=?",
                new String[]{String.valueOf(userId), String.valueOf(restoranId)});
        boolean result = c.moveToFirst();
        c.close();
        return result;
    }

    public void toggleFavorit(int userId, int restoranId) {
        if (isFavorit(userId, restoranId)) {
            getWritableDatabase().delete(TABLE_FAVORIT,
                    "user_id=? AND restoran_id=?",
                    new String[]{String.valueOf(userId), String.valueOf(restoranId)});
        } else {
            ContentValues cv = new ContentValues();
            cv.put("user_id",    userId);
            cv.put("restoran_id", restoranId);
            getWritableDatabase().insert(TABLE_FAVORIT, null, cv);
        }
    }

    public Cursor getFavoritByUser(int userId) {
        return getReadableDatabase().rawQuery(
                "SELECT r.* FROM " + TABLE_RESTORAN + " r " +
                        "JOIN " + TABLE_FAVORIT + " f ON r.id = f.restoran_id " +
                        "WHERE f.user_id=?",
                new String[]{String.valueOf(userId)});
    }
}