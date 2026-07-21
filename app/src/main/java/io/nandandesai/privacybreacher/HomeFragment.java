package io.nandandesai.privacybreacher;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment implements ConfirmAdapter.OnConfirmListener {

    private RecyclerView rvRecords;
    private TextView tvStatus;
    private TextView tvEmpty;
    private TextView tvThreshold;
    private TextView tvMonthYear;
    private TextView tvDayDetail;
    private GridLayout calendarGrid;
    private ImageButton btnPrevMonth, btnNextMonth;

    private DataBaseHelper dbHelper;
    private ConfirmAdapter adapter;
    private SharedPreferences prefs;

    // 日历状态
    private Calendar currentCalendar = Calendar.getInstance();
    private Map<String, Integer> sleepDataMap = new HashMap<>(); // key: "yyyy-MM-dd", value: hour (入睡时间的小时数)
    private int colorThresholdHour = 22;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化视图
        rvRecords = view.findViewById(R.id.rvRecords);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvThreshold = view.findViewById(R.id.tvThreshold);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        tvDayDetail = view.findViewById(R.id.tvDayDetail);
        calendarGrid = view.findViewById(R.id.calendarGrid);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);

        if (getContext() != null) {
            dbHelper = new DataBaseHelper(getContext());
            prefs = getContext().getSharedPreferences("SleepTrackerPrefs", Context.MODE_PRIVATE);
        }

        // 初始化列表
        rvRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ConfirmAdapter(getContext(), this);
        rvRecords.setAdapter(adapter);

        // 加载数据
        loadThresholdDisplay();
        loadUnconfirmedRecords();

        // 加载标色阈值
        colorThresholdHour = prefs.getInt("color_threshold_hour", 22);

        // 加载日历
        loadCalendar();

        // 翻页事件
        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            loadCalendar();
        });
        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            loadCalendar();
        });
    }

    private void loadThresholdDisplay() {
        if (prefs == null) return;
        int hour = prefs.getInt("threshold_hour", 22);
        int minute = prefs.getInt("threshold_minute", 0);
        String time = String.format("%02d:%02d", hour, minute);
        tvThreshold.setText("睡眠区间：" + time);
    }

    private void loadUnconfirmedRecords() {
        if (dbHelper == null) return;
        Cursor cursor = dbHelper.getUnconfirmedEvents();
        java.util.List<ConfirmRecord> records = new java.util.ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndex(DataBaseHelper.COLUMN_ID));
                long timestamp = cursor.getLong(cursor.getColumnIndex(DataBaseHelper.COLUMN_TIMESTAMP));
                String date = cursor.getString(cursor.getColumnIndex(DataBaseHelper.COLUMN_SLEEP_DATE));
                String time = DataBaseHelper.formatTime(timestamp);
                records.add(new ConfirmRecord(id, date, time, timestamp));
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (records.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvRecords.setVisibility(View.GONE);
            tvStatus.setText("暂无记录，今晚锁屏后自动记录");
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvRecords.setVisibility(View.VISIBLE);
            adapter.setRecords(records);
            tvStatus.setText("今日已记录 " + records.size() + " 条待确认记录");
        }
    }

    // ==================== 日历逻辑 ====================

    private void loadCalendar() {
        if (dbHelper == null || prefs == null) return;

        // 读取标色阈值
        colorThresholdHour = prefs.getInt("color_threshold_hour", 22);

        // 获取当月已确认记录
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String yearMonth = sdf.format(currentCalendar.getTime()).substring(0, 7);
        loadSleepDataForMonth(yearMonth);

        // 更新月份标题
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy年M月", Locale.getDefault());
        tvMonthYear.setText(monthFormat.format(currentCalendar.getTime()));

        // 渲染日历网格
        renderCalendar();
    }

    private void loadSleepDataForMonth(String yearMonth) {
        sleepDataMap.clear();
        if (dbHelper == null) return;

        // 查询该月所有已确认的记录
        Cursor cursor = dbHelper.getConfirmedEventsForMonth(yearMonth);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String date = cursor.getString(cursor.getColumnIndex(DataBaseHelper.COLUMN_SLEEP_DATE));
                long timestamp = cursor.getLong(cursor.getColumnIndex(DataBaseHelper.COLUMN_TIMESTAMP));
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(timestamp);
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                sleepDataMap.put(date, hour);
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    private void renderCalendar() {
        calendarGrid.removeAllViews();

        // 获取当前月份的第一天
        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=周日
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 填充空白
        for (int i = 0; i < firstDayOfWeek; i++) {
            View emptyView = new View(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 44;
            params.columnSpec = GridLayout.spec(i, 1f);
            emptyView.setLayoutParams(params);
            calendarGrid.addView(emptyView);
        }

        // 填充日期方块
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            String dateStr = sdf.format(cal.getTime());

            TextView dayView = new TextView(getContext());
            dayView.setText(String.valueOf(day));
            dayView.setGravity(android.view.Gravity.CENTER);
            dayView.setTextSize(12);
            dayView.setPadding(4, 8, 4, 8);

            // 设置背景和颜色
            int bgColor;
            int textColor;
            if (sleepDataMap.containsKey(dateStr)) {
                int sleepHour = sleepDataMap.get(dateStr);
                if (sleepHour <= colorThresholdHour) {
                    bgColor = getResources().getColor(android.R.color.holo_blue_dark);
                    textColor = getResources().getColor(android.R.color.white);
                } else {
                    bgColor = getResources().getColor(android.R.color.white);
                    textColor = getResources().getColor(android.R.color.black);
                }
                // 存储入睡时间用于点击详情
                dayView.setTag(R.id.tag_sleep_hour, sleepHour);
                dayView.setTag(R.id.tag_has_record, true);
            } else {
                bgColor = getResources().getColor(android.R.color.darker_gray);
                textColor = getResources().getColor(android.R.color.darker_gray);
                dayView.setTag(R.id.tag_has_record, false);
            }

            dayView.setBackgroundColor(bgColor);
            dayView.setTextColor(textColor);

            // 添加点击事件查看详情
            dayView.setOnClickListener(v -> showDayDetail(dateStr));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 44;
            params.columnSpec = GridLayout.spec((firstDayOfWeek + day - 1) % 7, 1f);
            params.setMargins(2, 2, 2, 2);
            dayView.setLayoutParams(params);
            calendarGrid.addView(dayView);
        }
    }

    private void showDayDetail(String date) {
        if (sleepDataMap.containsKey(date)) {
            int hour = sleepDataMap.get(date);
            tvDayDetail.setText(date + "  入睡时间：" + String.format("%02d:00", hour));
        } else {
            tvDayDetail.setText(date + "  当天无记录");
        }
    }

    // ==================== 外部调用 ====================

    public void refreshData() {
        loadThresholdDisplay();
        loadUnconfirmedRecords();
        loadCalendar();
    }

    @Override
    public void onConfirm(long id) {
        if (dbHelper != null && dbHelper.confirmEvent(id) > 0) {
            loadUnconfirmedRecords();
            loadCalendar();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
