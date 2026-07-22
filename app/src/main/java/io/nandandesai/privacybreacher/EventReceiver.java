package io.nandandesai.privacybreacher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;

public class EventReceiver extends BroadcastReceiver {

    private static final String TAG = "EventReceiver";
    private static final String PREF_NAME = "SleepTrackerPrefs";
    private static final int DEFAULT_THRESHOLD_HOUR = 22;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        // 只处理屏幕关闭事件
        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            Log.i(TAG, "屏幕关闭事件触发");

            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            int thresholdHour = prefs.getInt("threshold_hour", DEFAULT_THRESHOLD_HOUR);
            int thresholdMinute = prefs.getInt("threshold_minute", 0);

            long now = System.currentTimeMillis();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(now);
            int currentHour = cal.get(Calendar.HOUR_OF_DAY);
            int currentMinute = cal.get(Calendar.MINUTE);

            // 判断当前时间是否 >= 阈值时间
            boolean isAfterThreshold = (currentHour > thresholdHour) ||
                    (currentHour == thresholdHour && currentMinute >= thresholdMinute);

            if (isAfterThreshold) {
                // 直接记录到数据库
                String sleepDate = DataBaseHelper.formatDate(now);
                DataBaseHelper dbHelper = new DataBaseHelper(context);
                long id = dbHelper.insertEvent("SCREEN_OFF", now, sleepDate);
                dbHelper.close();
                Log.i(TAG, "已记录锁屏事件，ID=" + id + "，时间=" + DataBaseHelper.formatTime(now));
            } else {
                // 在阈值前锁屏，暂存到 SharedPreferences，等待检查
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong("pending_timestamp", now);
                editor.apply();
                Log.i(TAG, "阈值前锁屏，已暂存");
            }
        }
    }
}
