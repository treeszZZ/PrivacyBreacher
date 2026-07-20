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
import androidx.lifecycle.MutableLiveData;

import java.util.Calendar;
import java.util.Date;

public class PrivacyBreacherService extends Service {

    private static final String TAG = "PrivacyBreacherService";
    private static final String PREF_NAME = "SleepTrackerPrefs";

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
        // 启动前台服务
        startForeground(1, getNotification(this));

        // 注册广播接收器
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        // 只监听屏幕关闭，不需要其他事件
        eventReceiver = new EventReceiver();
        registerReceiver(eventReceiver, intentFilter);

        // 启动定时检查任务（每10分钟检查一次是否有待处理记录）
        startPeriodicCheck();

        return START_STICKY;
    }

    private void startPeriodicCheck() {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                performCheck();
                handler.postDelayed(this, 10 * 60 * 1000); // 10分钟间隔
            }
        };
        // 首次延迟1分钟执行
        handler.postDelayed(checkRunnable, 60 * 1000);
    }

    private void performCheck() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long pendingTimestamp = prefs.getLong("pending_timestamp", -1);

        if (pendingTimestamp == -1) {
            // 没有待处理的记录
            return;
        }

        // 获取阈值时间
        int thresholdHour = prefs.getInt("threshold_hour", 22);
        int thresholdMinute = prefs.getInt("threshold_minute", 0);

        // 获取当前时间
        long now = System.currentTimeMillis();
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTimeInMillis(now);
        int currentHour = nowCal.get(Calendar.HOUR_OF_DAY);
        int currentMinute = nowCal.get(Calendar.MINUTE);

        // 判断当前时间是否已经过了阈值时间（且至少过了5分钟，给用户缓冲）
        boolean isPastThreshold = (currentHour > thresholdHour) ||
                (currentHour == thresholdHour && currentMinute >= thresholdMinute + 5);

        if (isPastThreshold) {
            // 检查从pending时间到当前时间之间是否有新记录（即是否有阈值后的锁屏事件）
            // 由于我们每次锁屏都会记录（如果是阈值后），因此如果pending存在，说明阈值后没有新锁屏
            // 那么我们就使用pending时间作为候选睡眠时间

            // 但是还要判断pending时间与阈值时间的间隔是否在FALLBACK_WINDOW_HOURS以内
            long diff = now - pendingTimestamp;
            if (diff <= FALLBACK_WINDOW_HOURS * 3600 * 1000) {
                // 在兜底窗口内，将其作为候选记录
                String sleepDate = DataBaseHelper.formatDate(pendingTimestamp);
                long id = dbHelper.insertEvent("SCREEN_OFF", pendingTimestamp, sleepDate);
                Log.i(TAG, "兜底记录，ID=" + id + "，时间=" + new Date(pendingTimestamp));
            } else {
                Log.i(TAG, "待处理时间太早，忽略（超过兜底窗口）");
            }

            // 清除pending标记
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