package io.nandandesai.privacybreacher;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DataBaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sleep_tracker.db";
    private static final int DATABASE_VERSION = 1;

    // 表名
    public static final String TABLE_EVENTS = "events";

    // 列名
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_EVENT_TYPE = "event_type";      // "SCREEN_OFF"
    public static final String COLUMN_TIMESTAMP = "timestamp";        // 原始时间戳（毫秒）
    public static final String COLUMN_SLEEP_DATE = "sleep_date";      // 用户归属的日期（yyyy-MM-dd）
    public static final String COLUMN_IS_CONFIRMED = "is_confirmed";  // 0 或 1

    // 建表语句
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
        // 简单处理：升级时删除表重建
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        onCreate(db);
    }

    // ===================== 增删改查方法 =====================

    // 插入一条事件记录
    public long insertEvent(String eventType, long timestamp, String sleepDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_TYPE, eventType);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_SLEEP_DATE, sleepDate);
        values.put(COLUMN_IS_CONFIRMED, 0);  // 默认未确认
        long id = db.insert(TABLE_EVENTS, null, values);
        db.close();
        return id;
    }

    // 查询所有未确认的记录，按日期倒序、时间倒序排列
    public Cursor getUnconfirmedEvents() {
        SQLiteDatabase db = this.getReadableDatabase();
        String orderBy = COLUMN_SLEEP_DATE + " DESC, " + COLUMN_TIMESTAMP + " DESC";
        return db.query(TABLE_EVENTS, null, COLUMN_IS_CONFIRMED + "=?", new String[]{"0"}, null, null, orderBy);
    }

    // 将一条记录标记为已确认
    public int confirmEvent(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_CONFIRMED, 1);
        int rows = db.update(TABLE_EVENTS, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    // 更新记录的 sleep_date（用于用户修改归属日期）
    public int updateSleepDate(long id, String newDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SLEEP_DATE, newDate);
        int rows = db.update(TABLE_EVENTS, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    // 删除某条记录（可选，暂不实现，保留）
    public void deleteEvent(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EVENTS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // 工具：将时间戳格式化为 "yyyy-MM-dd"
    public static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // 工具：将时间戳格式化为 "HH:mm"（用于显示时间）
    public static String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}