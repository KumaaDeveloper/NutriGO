package com.example.pedulimakanan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "db_pedulimakanan.db";
    private static final int DB_VERSION = 6;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_RESTORAN = "restoran";
    public static final String TABLE_MENU = "menu";
    public static final String TABLE_TRANSAKSI = "transaksi";
    public static final String TABLE_DETAIL_TRANSAKSI = "detail_transaksi";
    public static final String TABLE_FAVORIT = "favorit";
    public static final String TABLE_PENDING_USERS = "pending_users";
    public static final String TABLE_KERANJANG = "keranjang";

    public static final String TIPE_MAKANAN = "Makanan";
    public static final String TIPE_MINUMAN = "Minuman";
    public static final String TIPE_SNACK = "Snack";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nama TEXT NOT NULL," +
                "email TEXT UNIQUE NOT NULL," +
                "no_hp TEXT," +
                "password TEXT NOT NULL," +
                "kode_verifikasi TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "alamat_default TEXT," +
                "saldo INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_PENDING_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nama TEXT, email TEXT, no_hp TEXT, password TEXT," +
                "kode_verifikasi TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE " + TABLE_RESTORAN + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nama_resto TEXT NOT NULL," +
                "alamat_resto TEXT," +
                "kategori TEXT," +
                "tipe_menu TEXT DEFAULT 'Makanan'," +
                "rating REAL DEFAULT 0," +
                "gambar_url TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_MENU + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "restoran_id INTEGER NOT NULL," +
                "nama_item TEXT NOT NULL," +
                "harga INTEGER NOT NULL," +
                "deskripsi TEXT," +
                "gambar_url TEXT," +
                "is_tersedia INTEGER DEFAULT 1," +
                "FOREIGN KEY(restoran_id) REFERENCES restoran(id))");

        db.execSQL("CREATE TABLE " + TABLE_TRANSAKSI + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "restoran_id INTEGER NOT NULL," +
                "total_harga INTEGER," +
                "status_pesanan TEXT DEFAULT 'Selesai'," +
                "tanggal TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY(user_id) REFERENCES users(id)," +
                "FOREIGN KEY(restoran_id) REFERENCES restoran(id))");

        db.execSQL("CREATE TABLE " + TABLE_DETAIL_TRANSAKSI + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "transaksi_id INTEGER NOT NULL," +
                "menu_id INTEGER NOT NULL," +
                "jumlah INTEGER NOT NULL," +
                "harga_saat_ini INTEGER NOT NULL," +
                "FOREIGN KEY(transaksi_id) REFERENCES transaksi(id)," +
                "FOREIGN KEY(menu_id) REFERENCES menu(id))");

        db.execSQL("CREATE TABLE " + TABLE_FAVORIT + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "restoran_id INTEGER NOT NULL," +
                "FOREIGN KEY(user_id) REFERENCES users(id)," +
                "FOREIGN KEY(restoran_id) REFERENCES restoran(id))");

        db.execSQL("CREATE TABLE " + TABLE_KERANJANG + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "restoran_id INTEGER NOT NULL," +
                "menu_id INTEGER NOT NULL," +
                "jumlah INTEGER NOT NULL," +
                "FOREIGN KEY(user_id) REFERENCES users(id)," +
                "FOREIGN KEY(restoran_id) REFERENCES restoran(id)," +
                "FOREIGN KEY(menu_id) REFERENCES menu(id))");

        seedData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_KERANJANG);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DETAIL_TRANSAKSI);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSAKSI);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORIT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MENU);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESTORAN);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENDING_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    private void seedData(SQLiteDatabase db) {
        insertResto(db, "Saladstop", "Jl. Sudirman No. 1", "Salad", TIPE_MAKANAN, 4.8f, "saladstop");
        insertResto(db, "Supergrain", "Jl. Thamrin No. 5", "Grain Bowl", TIPE_MAKANAN, 4.3f, "supergrain");
        insertResto(db, "Burgreen", "Jl. Kemang No. 10", "Vegan", TIPE_MAKANAN, 4.5f, "burgreen");
        insertResto(db, "GreenBowl", "Jl. Kuningan No. 7", "Bowls", TIPE_MAKANAN, 3.8f, "greenbowl");

        insertResto(db, "SmoothieBar", "Jl. Sudirman No. 50", "Smoothie", TIPE_MINUMAN, 4.6f, "img_placeholder_food");

        insertResto(db, "FreshBox", "Jl. Senayan No. 2", "Healthy", TIPE_SNACK, 3.5f, "freshbox");
        insertResto(db, "NutriSnack", "Jl. Blok M No. 8", "Snack", TIPE_SNACK, 4.0f, "img_placeholder_food");

        insertMenu(db, 1, "Tuna San", 85000, "Salad tuna segar dengan alpukat, tomat, saus madu", "", 1);
        insertMenu(db, 1, "Hail Caesar", 80000, "Caesar klasik dengan ayam panggang dan parmesan", "", 1);
        insertMenu(db, 1, "Mini Bowl", 68000, "Pilihan sayuran segar dengan protein pilihanmu", "", 1);
        insertMenu(db, 1, "Protein Mix", 95000, "Campuran protein tinggi: telur, tuna, edamame", "", 1);

        insertMenu(db, 2, "Brown Rice Bowl", 72000, "Nasi merah dengan sayuran dan saus kacang", "", 1);
        insertMenu(db, 2, "Quinoa Power", 88000, "Quinoa dengan avocado, edamame, dan dressing lemon", "", 1);
        insertMenu(db, 2, "Grain Classic", 65000, "Mix biji-bijian dengan topping ayam dan sayuran", "", 1);

        insertMenu(db, 3, "Vegan Burger", 75000, "Burger vegan dengan patty jamur dan saus tomat", "", 1);
        insertMenu(db, 3, "Smoothie Bowl", 65000, "Acai smoothie bowl dengan granola dan buah", "", 1);

        insertMenu(db, 4, "Green Detox", 58000, "Campuran sayuran hijau dengan dressing jahe", "", 1);
        insertMenu(db, 4, "Chicken Bowl", 62000, "Ayam panggang dengan brokoli dan wortel", "", 1);

        insertMenu(db, 5, "Tropical Blast", 28000, "Mangga, nanas, dan jeruk dengan coconut water", "", 1);
        insertMenu(db, 5, "Green Power", 30000, "Bayam, apel hijau, jahe, dan lemon segar", "", 1);
        insertMenu(db, 5, "Berry Bliss", 32000, "Stroberi, blueberry, raspberry dengan susu almond", "", 1);
        insertMenu(db, 5, "Peanut Butter Boo", 35000, "Pisang, selai kacang, oat, dan susu sapi", "", 1);
        insertMenu(db, 5, "Detox Cleanse", 27000, "Timun, seledri, lemon, dan madu murni", "", 1);

        insertMenu(db, 6, "Wrap Veggie", 55000, "Wrap dengan sayuran segar dan hummus", "", 1);
        insertMenu(db, 6, "Fruit Bowl", 48000, "Campuran buah segar musiman", "", 1);

        insertMenu(db, 7, "Granola Bar", 18000, "Granola oat dengan madu dan kacang", "", 1);
        insertMenu(db, 7, "Edamame Cup", 15000, "Edamame rebus dengan taburan garam himalaya", "", 1);
        insertMenu(db, 7, "Mixed Nuts", 22000, "Campuran kacang panggang tanpa garam", "", 1);
        insertMenu(db, 7, "Rice Cake", 12000, "Kue beras renyah rasa original", "", 1);
    }

    private void insertResto(SQLiteDatabase db, String nama, String alamat,
                             String kategori, String tipeMenu, float rating, String gambar) {
        ContentValues cv = new ContentValues();
        cv.put("nama_resto", nama);
        cv.put("alamat_resto", alamat);
        cv.put("kategori", kategori);
        cv.put("tipe_menu", tipeMenu);
        cv.put("rating", rating);
        cv.put("gambar_url", gambar);
        db.insert(TABLE_RESTORAN, null, cv);
    }

    private void insertMenu(SQLiteDatabase db, int restoranId, String nama,
                            int harga, String deskripsi, String gambar, int tersedia) {
        ContentValues cv = new ContentValues();
        cv.put("restoran_id", restoranId);
        cv.put("nama_item", nama);
        cv.put("harga", harga);
        cv.put("deskripsi", deskripsi);
        cv.put("gambar_url", gambar);
        cv.put("is_tersedia", tersedia);
        db.insert(TABLE_MENU, null, cv);
    }

    public int login(String nama, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id FROM " + TABLE_USERS + " WHERE nama=? AND password=?",
                new String[]{nama, password}
        );

        int userId = -1;

        if (c.moveToFirst()) {
            userId = c.getInt(0);
        }

        c.close();
        return userId;
    }

    public boolean register(String nama, String email, String noHp, String alamat, String password) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT id FROM " + TABLE_USERS + " WHERE email=?",
                new String[]{email}
        );

        boolean exists = c.moveToFirst();
        c.close();

        if (exists) {
            return false;
        }

        ContentValues cv = new ContentValues();
        cv.put("nama", nama);
        cv.put("email", email);
        cv.put("no_hp", noHp);
        cv.put("alamat_default", alamat);
        cv.put("password", password);

        return getWritableDatabase().insert(TABLE_USERS, null, cv) != -1;
    }

    public String requestResetCode(String identifier, String method) {
        SQLiteDatabase db = getReadableDatabase();
        String column = method.equals("email") ? "email" : "no_hp";

        Cursor c = db.rawQuery(
                "SELECT id FROM " + TABLE_USERS + " WHERE " + column + "=?",
                new String[]{identifier}
        );

        if (!c.moveToFirst()) {
            c.close();
            return null;
        }

        int userId = c.getInt(0);
        c.close();

        String code = String.valueOf((int) (Math.random() * 900000) + 100000);

        ContentValues cv = new ContentValues();
        cv.put("kode_verifikasi", code);

        getWritableDatabase().update(
                TABLE_USERS,
                cv,
                "id=?",
                new String[]{String.valueOf(userId)}
        );

        return code;
    }

    public boolean resetPassword(String identifier, String method, String kode, String passwordBaru) {
        SQLiteDatabase db = getReadableDatabase();
        String column = method.equals("email") ? "email" : "no_hp";

        Cursor c = db.rawQuery(
                "SELECT id, kode_verifikasi FROM " + TABLE_USERS + " WHERE " + column + "=?",
                new String[]{identifier}
        );

        if (!c.moveToFirst()) {
            c.close();
            return false;
        }

        int userId = c.getInt(0);
        String storedCode = c.getString(1);
        c.close();

        if (!kode.equals(storedCode)) {
            return false;
        }

        ContentValues cv = new ContentValues();
        cv.put("password", passwordBaru);
        cv.put("kode_verifikasi", "");

        getWritableDatabase().update(
                TABLE_USERS,
                cv,
                "id=?",
                new String[]{String.valueOf(userId)}
        );

        return true;
    }

    public Cursor getAllRestoran() {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_RESTORAN + " ORDER BY rating DESC",
                null
        );
    }

    public Cursor getRestoranByTipe(String tipeMenu) {
        if (tipeMenu == null || tipeMenu.isEmpty()) {
            return getAllRestoran();
        }

        return getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_RESTORAN + " WHERE tipe_menu=? ORDER BY rating DESC",
                new String[]{tipeMenu}
        );
    }

    public Cursor getMenuByRestoran(int restoranId) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_MENU + " WHERE restoran_id=? AND is_tersedia=1",
                new String[]{String.valueOf(restoranId)}
        );
    }

    public long buatTransaksi(int userId, int restoranId, JSONArray cartItems, int totalHarga) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        try {
            ContentValues cv = new ContentValues();
            cv.put("user_id", userId);
            cv.put("restoran_id", restoranId);
            cv.put("total_harga", totalHarga);
            cv.put("status_pesanan", "Selesai");

            long transaksiId = db.insert(TABLE_TRANSAKSI, null, cv);

            if (transaksiId == -1) {
                return -1;
            }

            for (int i = 0; i < cartItems.length(); i++) {
                JSONObject item = cartItems.getJSONObject(i);

                ContentValues dcv = new ContentValues();
                dcv.put("transaksi_id", transaksiId);
                dcv.put("menu_id", item.getInt("menu_id"));
                dcv.put("jumlah", item.getInt("jumlah"));
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

    public Cursor getTransaksiByUser(int userId) {
        return getReadableDatabase().rawQuery(
                "SELECT t.*, r.nama_resto FROM " + TABLE_TRANSAKSI + " t " +
                        "JOIN " + TABLE_RESTORAN + " r ON t.restoran_id = r.id " +
                        "WHERE t.user_id=? ORDER BY t.tanggal DESC",
                new String[]{String.valueOf(userId)}
        );
    }

    public Cursor getDetailTransaksi(int transaksiId) {
        return getReadableDatabase().rawQuery(
                "SELECT dt.*, m.nama_item FROM " + TABLE_DETAIL_TRANSAKSI + " dt " +
                        "JOIN " + TABLE_MENU + " m ON dt.menu_id = m.id " +
                        "WHERE dt.transaksi_id=?",
                new String[]{String.valueOf(transaksiId)}
        );
    }

    public boolean isFavorit(int userId, int restoranId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id FROM " + TABLE_FAVORIT + " WHERE user_id=? AND restoran_id=?",
                new String[]{String.valueOf(userId), String.valueOf(restoranId)}
        );

        boolean result = c.moveToFirst();
        c.close();

        return result;
    }

    public void toggleFavorit(int userId, int restoranId) {
        if (isFavorit(userId, restoranId)) {
            getWritableDatabase().delete(
                    TABLE_FAVORIT,
                    "user_id=? AND restoran_id=?",
                    new String[]{String.valueOf(userId), String.valueOf(restoranId)}
            );
        } else {
            ContentValues cv = new ContentValues();
            cv.put("user_id", userId);
            cv.put("restoran_id", restoranId);

            getWritableDatabase().insert(TABLE_FAVORIT, null, cv);
        }
    }

    public Cursor getFavoritByUser(int userId) {
        return getReadableDatabase().rawQuery(
                "SELECT r.* FROM " + TABLE_RESTORAN + " r " +
                        "JOIN " + TABLE_FAVORIT + " f ON r.id = f.restoran_id " +
                        "WHERE f.user_id=?",
                new String[]{String.valueOf(userId)}
        );
    }

    public boolean tambahKeKeranjang(int userId, int restoranId, int menuId, int jumlah) {
        SQLiteDatabase db = getWritableDatabase();

        Cursor c = db.rawQuery(
                "SELECT id, jumlah FROM " + TABLE_KERANJANG +
                        " WHERE user_id=? AND restoran_id=? AND menu_id=?",
                new String[]{
                        String.valueOf(userId),
                        String.valueOf(restoranId),
                        String.valueOf(menuId)
                }
        );

        ContentValues cv = new ContentValues();
        boolean success;

        if (c.moveToFirst()) {
            int idLama = c.getInt(0);
            int jumlahLama = c.getInt(1);

            cv.put("jumlah", jumlahLama + jumlah);

            success = db.update(
                    TABLE_KERANJANG,
                    cv,
                    "id=?",
                    new String[]{String.valueOf(idLama)}
            ) > 0;
        } else {
            cv.put("user_id", userId);
            cv.put("restoran_id", restoranId);
            cv.put("menu_id", menuId);
            cv.put("jumlah", jumlah);

            success = db.insert(TABLE_KERANJANG, null, cv) != -1;
        }

        c.close();
        return success;
    }

    public Cursor getKeranjangByUser(int userId) {
        return getReadableDatabase().rawQuery(
                "SELECT k.id, k.restoran_id, k.menu_id, k.jumlah, " +
                        "m.nama_item AS nama_item, m.harga AS harga, r.nama_resto AS nama_resto " +
                        "FROM " + TABLE_KERANJANG + " k " +
                        "JOIN " + TABLE_MENU + " m ON k.menu_id = m.id " +
                        "JOIN " + TABLE_RESTORAN + " r ON k.restoran_id = r.id " +
                        "WHERE k.user_id = ?",
                new String[]{String.valueOf(userId)}
        );
    }

    public void hapusItemKeranjang(int keranjangId) {
        getWritableDatabase().delete(
                TABLE_KERANJANG,
                "id=?",
                new String[]{String.valueOf(keranjangId)}
        );
    }

    public void bersihkanKeranjang(int userId) {
        getWritableDatabase().delete(
                TABLE_KERANJANG,
                "user_id=?",
                new String[]{String.valueOf(userId)}
        );
    }

    public int getSaldo(int userId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT saldo FROM " + TABLE_USERS + " WHERE id=?",
                new String[]{String.valueOf(userId)}
        );

        int saldo = 0;

        if (c.moveToFirst()) {
            saldo = c.getInt(0);
        }

        c.close();
        return saldo;
    }

    public boolean updateSaldo(int userId, int jumlahBaru) {
        ContentValues cv = new ContentValues();
        cv.put("saldo", jumlahBaru);

        return getWritableDatabase().update(
                TABLE_USERS,
                cv,
                "id=?",
                new String[]{String.valueOf(userId)}
        ) > 0;
    }
}