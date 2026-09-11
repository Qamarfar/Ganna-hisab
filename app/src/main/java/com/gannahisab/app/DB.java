package com.gannahisab.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DB extends SQLiteOpenHelper {

    private static final String DB_NAME = "ganna.db";
    private static final int DB_VERSION = 3;

    public DB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Master list: mill names (saved once, reused every time)
        db.execSQL("CREATE TABLE IF NOT EXISTS mills(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE)");

        // Master list: driver name + phone number (saved once, reused every time)
        db.execSQL("CREATE TABLE IF NOT EXISTS drivers(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, phone TEXT)");

        // Every load/trip becomes a NEW row here - nothing ever gets overwritten
        db.execSQL("CREATE TABLE IF NOT EXISTS trips(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date TEXT, truck TEXT, driver TEXT, driverPhone TEXT, maund REAL, mill TEXT," +
                "driverCost REAL, otherCost REAL, saleRate REAL, saleTotal REAL, status TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS expenses(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date TEXT, title TEXT, amount REAL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS receipts(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date TEXT, mill TEXT, amount REAL, note TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS payments(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date TEXT, person TEXT, amount REAL, note TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS purchases(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date TEXT, seller TEXT, maund REAL, rate REAL, total REAL, paid REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            db.execSQL("ALTER TABLE trips ADD COLUMN saleRate REAL DEFAULT 0");
            db.execSQL("ALTER TABLE trips ADD COLUMN saleTotal REAL DEFAULT 0");
        }
        if (oldV < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS mills(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS drivers(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, phone TEXT)");
            db.execSQL("ALTER TABLE trips ADD COLUMN driverPhone TEXT DEFAULT ''");
        }
    }

    // ---------- Mill master list ----------

    /** Saves a mill name if it's new. Existing mills are left untouched. */
    public void addMillIfNotExists(String name) {
        if (name == null || name.trim().isEmpty()) return;
        ContentValues cv = new ContentValues();
        cv.put("name", name.trim());
        getWritableDatabase().insertWithOnConflict("mills", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    /** Returns every saved mill name, for the dropdown/autocomplete. */
    public java.util.List<String> getMillNames() {
        java.util.List<String> list = new java.util.ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT name FROM mills ORDER BY name ASC", null);
        while (c.moveToNext()) list.add(c.getString(0));
        c.close();
        return list;
    }

    // ---------- Driver master list ----------

    /** Saves a driver's name + phone. If the name already exists, its phone number is updated. */
    public void addOrUpdateDriver(String name, String phone) {
        if (name == null || name.trim().isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name.trim());
        cv.put("phone", phone == null ? "" : phone.trim());
        long result = db.insertWithOnConflict("drivers", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (result == -1) {
            // name already existed -> update its phone number instead
            db.execSQL("UPDATE drivers SET phone=? WHERE name=?", new Object[]{phone == null ? "" : phone.trim(), name.trim()});
        }
    }

    /** Returns every saved driver name, for the dropdown/autocomplete. */
    public java.util.List<String> getDriverNames() {
        java.util.List<String> list = new java.util.ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT name FROM drivers ORDER BY name ASC", null);
        while (c.moveToNext()) list.add(c.getString(0));
        c.close();
        return list;
    }

    /** Looks up a driver's saved phone number by name (empty string if not found). */
    public String getDriverPhone(String name) {
        if (name == null || name.trim().isEmpty()) return "";
        Cursor c = getReadableDatabase().rawQuery("SELECT phone FROM drivers WHERE name=?", new String[]{name.trim()});
        String phone = "";
        if (c.moveToFirst()) phone = c.getString(0);
        c.close();
        return phone == null ? "" : phone;
    }

    // ---------- Trips (loads) ----------

    /** Adds a brand-new trip/load row. Every previous row stays exactly as it was. */
    public void insertTrip(String date, String truck, String driver, String driverPhone,
                            double maund, String mill, double driverCost, double otherCost,
                            double saleRate, double saleTotal, String status) {
        ContentValues cv = new ContentValues();
        cv.put("date", date);
        cv.put("truck", truck);
        cv.put("driver", driver);
        cv.put("driverPhone", driverPhone);
        cv.put("maund", maund);
        cv.put("mill", mill);
        cv.put("driverCost", driverCost);
        cv.put("otherCost", otherCost);
        cv.put("saleRate", saleRate);
        cv.put("saleTotal", saleTotal);
        cv.put("status", status);
        getWritableDatabase().insert("trips", null, cv);
    }

    /** Every trip ever saved, newest first. Nothing is ever filtered out or deleted automatically. */
    public Cursor getAllTrips() {
        return getReadableDatabase().rawQuery(
                "SELECT date,mill,driver,driverPhone,truck,maund,driverCost,otherCost,saleRate,saleTotal,status " +
                        "FROM trips ORDER BY id DESC", null);
    }

    /** Every trip for one specific mill, newest first - the mill's full running record. */
    public Cursor getTripsForMill(String millName) {
        return getReadableDatabase().rawQuery(
                "SELECT date,mill,driver,driverPhone,truck,maund,driverCost,otherCost,saleRate,saleTotal,status " +
                        "FROM trips WHERE mill=? ORDER BY id DESC", new String[]{millName});
    }
}
