package io.nandandesai.privacybreacher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class PrivacyBreacherService extends Service {

    private static final String TAG = "PrivacyBreacherService";
    private static final String PREF_NAME = "SleepTrackerPrefs";
    private static final int FALLBACK_WINDOW_HOURS = 4;  // 定义兜底窗口常量

    private EventReceiver eventReceiver;
    private Handler handler;
    private Runnable checkRunnable;
    private DataBaseHelper dbHelper;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DataBaseHelper(this);
        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, getNotification(this));

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        eventReceiver = new EventReceiver();
        registerReceiver(eventReceiver, intentFilter);

        startPeriodicCheck();

        return START_STICKY;
    }

    private void startPeriodicCheck() {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                performCheck();
                handler.postDelayed(this, 10 * 60 * 1000);
            }
        };
        handler.postDelayed(checkRunnable, 60 * 1000);
    }

    private void performCheck() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long pendingTimestamp = prefs.getLong("pending_timestamp", -1);

        if (pendingTimestamp == -1) {
            return;
        }

        int thresholdHour = prefs.getInt("threshold_hour", 22);
        int thresholdMinute = prefs.getInt("threshold_minute", 0);

        long now = System.currentTimeMillis();
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTimeInMillis(now);
        int currentHour = nowCal.get(Calendar.HOUR_OF_DAY);
        int currentMinute = nowCal.get(Calendar.MINUTE);

        boolean isPastThreshold = (currentHour > thresholdHour) ||
                (currentHour == thresholdHour && currentMinute >= thresholdMinute + 5);

        if (isPastThreshold) {
            long diff = now - pendingTimestamp;
            if (diff <= FALLBACK_WINDOW_HOURS * 3600 * 1000) {
                String sleepDate = DataBaseHelper.formatDate(pendingTimestamp);
                dbHelper.insertEvent("SCREEN_OFF", pendingTimestamp, sleepDate);
            }
            prefs.edit().remove("pending_timestamp").apply();
        }
    }

    @Override
    public void onDestroy() {
        if (eventReceiver != null) {
            unregisterReceiver(eventReceiver);
        }
        if (handler != null && checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    private Notification getNotification(Context context) {
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "sleep_tracker_channel";
        NotificationChannel channel = new NotificationChannel(channelId, "Sleep Tracker", NotificationManager.IMPORTANCE_LOW);
        manager.createNotificationChannel(channel);

        return new NotificationCompat.Builder(context, channelId)
                .setContentTitle("睡眠追踪中")
                .setContentText("正在记录您的锁屏时间")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
}