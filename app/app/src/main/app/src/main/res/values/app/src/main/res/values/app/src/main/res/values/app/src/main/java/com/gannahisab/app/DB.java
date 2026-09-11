package com.gannahisab.app;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;

public class DB extends SQLiteOpenHelper {
    public DB(Context c){ super(c,"ganna.db",null,2); }
    @Override public void onCreate(SQLiteDatabase d){
        d.execSQL("CREATE TABLE purchases(id INTEGER PRIMARY KEY AUTOINCREMENT,date TEXT,seller TEXT,maund REAL,rate REAL,total REAL,paid REAL)");
        d.execSQL("CREATE TABLE trips(id INTEGER PRIMARY KEY AUTOINCREMENT,date TEXT,truck TEXT,driver TEXT,maund REAL,mill TEXT,driverCost REAL,otherCost REAL,saleRate REAL,saleTotal REAL,status TEXT)");
        d.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY AUTOINCREMENT,date TEXT,title TEXT,amount REAL)");
        d.execSQL("CREATE TABLE payments(id INTEGER PRIMARY KEY AUTOINCREMENT,date TEXT,person TEXT,amount REAL,note TEXT)");
        d.execSQL("CREATE TABLE receipts(id INTEGER PRIMARY KEY AUTOINCREMENT,date TEXT,mill TEXT,amount REAL,note TEXT)");
    }
    @Override public void onUpgrade(SQLiteDatabase d,int oldV,int newV){
        if(oldV<2){
            try{d.execSQL("ALTER TABLE trips ADD COLUMN saleRate REAL DEFAULT 0");}catch(Exception ignored){}
            try{d.execSQL("ALTER TABLE trips ADD COLUMN saleTotal REAL DEFAULT 0");}catch(Exception ignored){}
            d.execSQL("CREATE TABLE IF NOT EXISTS receipts(id INTEGER PRIMARY KEY AUTOINCREMENT,date TEXT,mill TEXT,amount REAL,note TEXT)");
        }
    }
    public double sum(String table,String col){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM("+col+"),0) FROM "+table,null);c.moveToFirst();double x=c.getDouble(0);c.close();return x;}
}
