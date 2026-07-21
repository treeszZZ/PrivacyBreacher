package io.nandandesai.privacybreacher;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataBaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sleep_tracker.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_EVENTS = "events";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_EVENT_TYPE = "event_type";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_SLEEP_DATE = "sleep_date";
    public static final String COLUMN_IS_CONFIRMED = "is_confirmed";

    private static final String CREATE_TABLE_EVENTS =
            "CREATE TABLE " + TABLE_EVENTS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_EVENT_TYPE + " TEXT NOT NULL, " +
                    COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                    COLUMN_SLEEP_DATE + " TEXT NOT NULL, " +
                    COLUMN_IS_CONFIRMED + " INTEGER DEFAULT 0" +
                    ");";

    public DataBaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_EVENTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        onCreate(db);
    }

    public long insertEvent(String eventType, long timestamp, String sleepDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_TYPE, eventType);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_SLEEP_DATE, sleepDate);
        values.put(COLUMN_IS_CONFIRMED, 0);
        long id = db.insert(TABLE_EVENTS, null, values);
        db.close();
        return id;
    }

    public Cursor getUnconfirmedEvents() {
        SQLiteDatabase db = this.getReadableDatabase();
        String orderBy = COLUMN_SLEEP_DATE + " DESC, " + COLUMN_TIMESTAMP + " DESC";
        return db.query(TABLE_EVENTS, null, COLUMN_IS_CONFIRMED + "=?", new String[]{"0"}, null, null, orderBy);
    }

    public int confirmEvent(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_CONFIRMED, 1);
        int rows = db.update(TABLE_EVENTS, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    // 新增方法：查询某月所有已确认的记录
    public Cursor getConfirmedEventsForMonth(String yearMonth) {
        SQLiteDatabase db = this.getReadableDatabase();
        // yearMonth 格式: "2025-01"
        String query = "SELECT " + COLUMN_SLEEP_DATE + ", " + COLUMN_TIMESTAMP +
                " FROM " + TABLE_EVENTS +
                " WHERE " + COLUMN_IS_CONFIRMED + " = 1" +
                " AND " + COLUMN_SLEEP_DATE + " LIKE ?";
        return db.rawQuery(query, new String[]{yearMonth + "%"});
    }

    public static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public void close() {
        try {
            super.close();
        } catch (Exception e) {
            // ignore
        }
    }
}
