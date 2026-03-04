package com.example.drivetestapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.drivetestapp.models.NetworkMeasurement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "DriveTestDB";
    private static final int DB_VERSION = 4;

    public DatabaseHelper(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE logs (timestamp LONG PRIMARY KEY, rat TEXT, op TEXT, mcc TEXT, mnc TEXT, " +
                "lat REAL, lon REAL, cid INTEGER, lac INTEGER, tac INTEGER, pci INTEGER, psc INTEGER, " +
                "bsic INTEGER, rsrp INTEGER, rsrq INTEGER, snr INTEGER, rssi INTEGER, rscp INTEGER, " +
                "ecNo INTEGER, rxLev INTEGER, rxQual INTEGER, arfcn INTEGER, earfcn INTEGER, uarfcn INTEGER, " +
                "synced INTEGER DEFAULT 0)"); // Synced 0 = No, 1 = Yes
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS logs");
        onCreate(db);
    }

    public void saveAllData(NetworkMeasurement m) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("timestamp", m.timestamp); v.put("rat", m.ratType); v.put("op", m.operatorName);
        v.put("mcc", m.mcc); v.put("mnc", m.mnc); v.put("lat", m.latitude); v.put("lon", m.longitude);
        v.put("cid", m.cellId); v.put("lac", m.lac); v.put("tac", m.tac); v.put("pci", m.pci);
        v.put("psc", m.psc); v.put("bsic", m.bsic); v.put("rsrp", m.rsrp); v.put("rsrq", m.rsrq);
        v.put("snr", m.snr); v.put("rssi", m.rssi); v.put("rscp", m.rscp); v.put("ecNo", m.ecNo);
        v.put("rxLev", m.rxLev); v.put("rxQual", m.rxQual); v.put("arfcn", m.arfcn);
        v.put("earfcn", m.earfcn); v.put("uarfcn", m.uarfcn);
        // Do not put "synced" here, let it default to 0 for new records
        db.insertWithOnConflict("logs", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Missing method fix: Gets records that haven't been uploaded yet.
     */
    public List<NetworkMeasurement> getUnsyncedData(int limit) {
        List<NetworkMeasurement> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM logs WHERE synced = 0 ORDER BY timestamp ASC LIMIT ?",
                new String[]{String.valueOf(limit)});

        if (c.moveToFirst()) {
            do {
                NetworkMeasurement m = new NetworkMeasurement();
                m.timestamp = c.getLong(0); m.ratType = c.getString(1); m.operatorName = c.getString(2);
                m.mcc = c.getString(3); m.mnc = c.getString(4); m.latitude = c.getDouble(5);
                m.longitude = c.getDouble(6); m.cellId = c.getInt(7); m.lac = c.getInt(8);
                m.tac = c.getInt(9); m.pci = c.getInt(10); m.psc = c.getInt(11);
                m.bsic = c.getInt(12); m.rsrp = c.getInt(13); m.rsrq = c.getInt(14);
                m.snr = c.getInt(15); m.rssi = c.getInt(16); m.rscp = c.getInt(17);
                m.ecNo = c.getInt(18); m.rxLev = c.getInt(19); m.rxQual = c.getInt(20);
                m.arfcn = c.getInt(21); m.earfcn = c.getInt(22); m.uarfcn = c.getInt(23);
                list.add(m);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    /**
     * Missing method fix: Marks a record as successfully sent to Firebase.
     */
    public void markAsSynced(long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("synced", 1);
        db.update("logs", v, "timestamp = ?", new String[]{String.valueOf(timestamp)});
    }

    public List<NetworkMeasurement> getAllLocalLogs() {
        List<NetworkMeasurement> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM logs ORDER BY timestamp DESC", null);
        if (c.moveToFirst()) {
            do {
                NetworkMeasurement m = new NetworkMeasurement();
                m.timestamp = c.getLong(0); m.ratType = c.getString(1); m.operatorName = c.getString(2);
                m.mcc = c.getString(3); m.mnc = c.getString(4); m.latitude = c.getDouble(5);
                m.longitude = c.getDouble(6); m.cellId = c.getInt(7); m.lac = c.getInt(8);
                m.tac = c.getInt(9); m.pci = c.getInt(10); m.psc = c.getInt(11);
                m.bsic = c.getInt(12); m.rsrp = c.getInt(13); m.rsrq = c.getInt(14);
                m.snr = c.getInt(15); m.rssi = c.getInt(16); m.rscp = c.getInt(17);
                m.ecNo = c.getInt(18); m.rxLev = c.getInt(19); m.rxQual = c.getInt(20);
                m.arfcn = c.getInt(21); m.earfcn = c.getInt(22); m.uarfcn = c.getInt(23);
                list.add(m);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public void deleteAllLogs() { getWritableDatabase().delete("logs", null, null); }
}