package io.nandandesai.privacybreacher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EventReceiver extends BroadcastReceiver {

    private static final String TAG = "EventReceiver";
    private static final String PREF_NAME = "SleepTrackerPrefs";
    private static final String KEY_THRESHOLD_HOUR = "threshold_hour";
    private static final String KEY_THRESHOLD_MINUTE = "threshold_minute";
    private static final int DEFAULT_THRESHOLD_HOUR = 22; // 默认 22:00

    // 兜底窗口：阈值后多少小时内若无记录，则取阈值前的最后一次锁屏
    private static final int FALLBACK_WINDOW_HOURS = 4;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        // 只处理屏幕关闭事件
        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            Log.i(TAG, "屏幕关闭事件触发");

            // 获取用户设定的阈值时间
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            int thresholdHour = prefs.getInt(KEY_THRESHOLD_HOUR, DEFAULT_THRESHOLD_HOUR);
            int thresholdMinute = prefs.getInt(KEY_THRESHOLD_MINUTE, 0);

            long now = System.currentTimeMillis();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(now);
            int currentHour = cal.get(Calendar.HOUR_OF_DAY);
            int currentMinute = cal.get(Calendar.MINUTE);

            // 判断当前时间是否 >= 阈值时间
            boolean isAfterThreshold = (currentHour > thresholdHour) ||
                    (currentHour == thresholdHour && currentMinute >= thresholdMinute);

            if (isAfterThreshold) {
                // 情况1：在阈值时间之后，直接记录
                String sleepDate = DataBaseHelper.formatDate(now);
                DataBaseHelper dbHelper = new DataBaseHelper(context);
                long id = dbHelper.insertEvent("SCREEN_OFF", now, sleepDate);
                Log.i(TAG, "记录锁屏事件，ID=" + id + "，时间=" + new Date(now));
                dbHelper.close();
            } else {
                // 情况2：当前时间在阈值之前，暂不记录，但设置一个“待处理”标记
                // 我们用 SharedPreferences 暂存这个时间，等到阈值时间后检查
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong("pending_timestamp", now);
                editor.apply();
                Log.i(TAG, "暂存锁屏事件（阈值前），时间=" + new Date(now));
            }

            // 此外，我们还需要一个定时检查逻辑：在阈值时间后几分钟检查是否已记录
            // 由于 BroadcastReceiver 不能长期运行，我们利用 Service 来调度这个检查
            // 我们会在 PrivacyBreacherService 中处理这部分
        }
    }
}