package io.nandandesai.privacybreacher;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment implements ConfirmAdapter.OnConfirmListener {

    private RecyclerView rvPending;
    private TextView tvPendingCount, tvPendingEmpty, tvStatus, tvThreshold;
    private ConfirmAdapter pendingAdapter;
    private DataBaseHelper dbHelper;
    private SharedPreferences prefs;

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ConfirmedPagerAdapter pagerAdapter;

    private String currentOrder = "DESC";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvPending = view.findViewById(R.id.rvPending);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvPendingEmpty = view.findViewById(R.id.tvPendingEmpty);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvThreshold = view.findViewById(R.id.tvThreshold);
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);

        if (getContext() != null) {
            dbHelper = new DataBaseHelper(getContext());
            prefs = getContext().getSharedPreferences("SleepTrackerPrefs", Context.MODE_PRIVATE);
        }

        pendingAdapter = new ConfirmAdapter(getContext(), this);
        rvPending.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPending.setAdapter(pendingAdapter);

        pagerAdapter = new ConfirmedPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if (position == 0) tab.setText("列表");
                    else tab.setText("日历");
                }
        ).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // 安全调用：检查 Fragment 是否已附加
                ConfirmedListFragment listFragment = pagerAdapter.getListFragment();
                if (listFragment != null && listFragment.isAdded()) {
                    if (position == 0) {
                        listFragment.showSortButton(true);
                    } else {
                        listFragment.showSortButton(false);
                    }
                }
            }
        });

        loadAllData();
    }

    private void loadAllData() {
        loadThresholdDisplay();
        loadPendingRecords();
        updatePendingCount();
        // 安全刷新：检查适配器中的 Fragment 是否已附加
        ConfirmedListFragment listFragment = pagerAdapter.getListFragment();
        if (listFragment != null && listFragment.isAdded()) {
            listFragment.refreshList();
        }
        ConfirmedCalendarFragment calendarFragment = pagerAdapter.getCalendarFragment();
        if (calendarFragment != null && calendarFragment.isAdded()) {
            calendarFragment.refreshCalendar();
        }
    }

    private void loadThresholdDisplay() {
        if (prefs == null) return;
        int hour = prefs.getInt("threshold_hour", 22);
        int minute = prefs.getInt("threshold_minute", 0);
        String time = String.format("%02d:%02d", hour, minute);
        tvThreshold.setText("睡眠区间：" + time);
    }

    private void loadPendingRecords() {
        if (dbHelper == null) return;
        Cursor cursor = dbHelper.getUnconfirmedEvents();
        List<ConfirmRecord> records = new ArrayList<>();
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
            tvPendingEmpty.setVisibility(View.VISIBLE);
            rvPending.setVisibility(View.GONE);
            tvStatus.setText("暂无记录，今晚锁屏后自动记录");
        } else {
            tvPendingEmpty.setVisibility(View.GONE);
            rvPending.setVisibility(View.VISIBLE);
            pendingAdapter.setRecords(records);
            tvStatus.setText("今日已记录 " + records.size() + " 条待确认记录");
        }
        updatePendingCount();
    }

    private void updatePendingCount() {
        if (pendingAdapter.getItemCount() > 0) {
            tvPendingCount.setText(pendingAdapter.getItemCount() + " 条待确认");
        } else {
            tvPendingCount.setText("0 条待确认");
        }
    }

    @Override
    public void onConfirm(long id) {
        if (dbHelper != null && dbHelper.confirmEvent(id) > 0) {
            loadAllData();
        }
    }

    public void onWithdraw(long id) {
        if (dbHelper == null) return;
        ContentValues values = new ContentValues();
        values.put(DataBaseHelper.COLUMN_IS_CONFIRMED, 0);
        int rows = dbHelper.getWritableDatabase().update(DataBaseHelper.TABLE_EVENTS,
                values, DataBaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        if (rows > 0) {
            loadAllData();
            Toast.makeText(getContext(), "已撤回该记录", Toast.LENGTH_SHORT).show();
        }
    }

    public void refreshData() {
        loadAllData();
    }

    public String getCurrentOrder() {
        return currentOrder;
    }

    public void toggleSortOrder() {
        if ("DESC".equals(currentOrder)) {
            currentOrder = "ASC";
        } else {
            currentOrder = "DESC";
        }
        ConfirmedListFragment listFragment = pagerAdapter.getListFragment();
        if (listFragment != null && listFragment.isAdded()) {
            listFragment.updateSortButtonText(currentOrder);
            listFragment.refreshList();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 从其他页面返回时刷新数据
        if (isAdded() && getContext() != null) {
            loadAllData();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // ===== ViewPager Adapter =====
    private class ConfirmedPagerAdapter extends FragmentStateAdapter {
        private ConfirmedListFragment listFragment;
        private ConfirmedCalendarFragment calendarFragment;

        public ConfirmedPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
            listFragment = new ConfirmedListFragment();
            calendarFragment = new ConfirmedCalendarFragment();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) return listFragment;
            else return calendarFragment;
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        public ConfirmedListFragment getListFragment() {
            return listFragment;
        }

        public ConfirmedCalendarFragment getCalendarFragment() {
            return calendarFragment;
        }
    }

    // ===== 已确认列表 Fragment =====
    public static class ConfirmedListFragment extends Fragment {
        private RecyclerView rvConfirmed;
        private TextView tvSortButton;
        private TextView tvEmpty;
        private ConfirmedAdapter adapter;
        private HomeFragment parent;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_confirmed_list, container, false);
            rvConfirmed = view.findViewById(R.id.rvConfirmed);
            tvSortButton = view.findViewById(R.id.tvSortButton);
            tvEmpty = view.findViewById(R.id.tvConfirmedEmpty);

            if (getParentFragment() instanceof HomeFragment) {
                parent = (HomeFragment) getParentFragment();
            }

            rvConfirmed.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new ConfirmedAdapter(getContext(), parent);
            rvConfirmed.setAdapter(adapter);

            if (parent != null) {
                tvSortButton.setOnClickListener(v -> parent.toggleSortOrder());
                updateSortButtonText(parent.getCurrentOrder());
            }

            refreshList();
            return view;
        }

        public void updateSortButtonText(String order) {
            if ("DESC".equals(order)) {
                tvSortButton.setText("倒序");
            } else {
                tvSortButton.setText("正序");
            }
        }

        public void showSortButton(boolean show) {
            if (isAdded()) {
                tvSortButton.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }

        public void refreshList() {
            if (!isAdded() || getContext() == null) {
                return;
            }
            if (parent == null || parent.dbHelper == null) {
                if (adapter != null) {
                    adapter.setRecords(new ArrayList<>());
                    tvEmpty.setVisibility(View.VISIBLE);
                }
                return;
            }
            DataBaseHelper helper = parent.dbHelper;
            String order = parent.getCurrentOrder();
            Cursor cursor = helper.getAllConfirmedEvents(order);
            List<ConfirmedRecord> records = new ArrayList<>();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndex(DataBaseHelper.COLUMN_ID));
                    long timestamp = cursor.getLong(cursor.getColumnIndex(DataBaseHelper.COLUMN_TIMESTAMP));
                    String date = cursor.getString(cursor.getColumnIndex(DataBaseHelper.COLUMN_SLEEP_DATE));
                    String time = DataBaseHelper.formatTime(timestamp);
                    records.add(new ConfirmedRecord(id, date, time, timestamp));
                } while (cursor.moveToNext());
                cursor.close();
            }
            adapter.setRecords(records);
            if (records.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvConfirmed.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvConfirmed.setVisibility(View.VISIBLE);
            }
        }
    }

    // ===== 已确认日历 Fragment =====
    public static class ConfirmedCalendarFragment extends Fragment {
        private GridLayout calendarGrid;
        private TextView tvMonthYear;
        private ImageButton btnPrev, btnNext;
        private HomeFragment parent;
        private Calendar currentCalendar = Calendar.getInstance();
        private Map<String, Integer> sleepDataMap = new HashMap<>();
        private int colorThresholdHour = 22;
        private boolean isAttached = false;

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            if (getParentFragment() instanceof HomeFragment) {
                parent = (HomeFragment) getParentFragment();
            }
            isAttached = true;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_calendar, container, false);
            calendarGrid = view.findViewById(R.id.calendarGrid);
            tvMonthYear = view.findViewById(R.id.tvMonthYear);
            btnPrev = view.findViewById(R.id.btnPrevMonth);
            btnNext = view.findViewById(R.id.btnNextMonth);

            if (parent != null && parent.prefs != null) {
                colorThresholdHour = parent.prefs.getInt("color_threshold_hour", 22);
            }

            btnPrev.setOnClickListener(v -> {
                currentCalendar.add(Calendar.MONTH, -1);
                refreshCalendar();
            });
            btnNext.setOnClickListener(v -> {
                currentCalendar.add(Calendar.MONTH, 1);
                refreshCalendar();
            });

            refreshCalendar();
            return view;
        }

        public void refreshCalendar() {
            if (!isAdded() || getContext() == null) {
                return;
            }
            if (parent == null || parent.dbHelper == null) {
                return;
            }

            DataBaseHelper helper = parent.dbHelper;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String yearMonth = sdf.format(currentCalendar.getTime()).substring(0, 7);
            sleepDataMap.clear();

            Cursor cursor = helper.getConfirmedEventsForMonth(yearMonth);
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

            SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy年M月", Locale.getDefault());
            tvMonthYear.setText(monthFormat.format(currentCalendar.getTime()));

            renderCalendar();
        }

        private void renderCalendar() {
            calendarGrid.removeAllViews();

            Calendar cal = (Calendar) currentCalendar.clone();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

            for (int i = 0; i < firstDayOfWeek; i++) {
                View empty = new View(getContext());
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 56;
                params.columnSpec = GridLayout.spec(i, 1f);
                empty.setLayoutParams(params);
                calendarGrid.addView(empty);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            for (int day = 1; day <= daysInMonth; day++) {
                cal.set(Calendar.DAY_OF_MONTH, day);
                String dateStr = sdf.format(cal.getTime());

                TextView dayView = new TextView(getContext());
                dayView.setText(String.valueOf(day));
                dayView.setGravity(Gravity.CENTER);
                dayView.setTextSize(14);
                dayView.setPadding(2, 2, 2, 2);

                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(6);

                int textColor;
                if (sleepDataMap.containsKey(dateStr)) {
                    int sleepHour = sleepDataMap.get(dateStr);
                    if (sleepHour <= colorThresholdHour) {
                        drawable.setColor(getResources().getColor(R.color.calendar_blue));
                        textColor = getResources().getColor(android.R.color.white);
                    } else {
                        drawable.setColor(getResources().getColor(android.R.color.white));
                        drawable.setStroke(1, getResources().getColor(R.color.calendar_white_border));
                        textColor = getResources().getColor(android.R.color.black);
                    }
                    dayView.setTag(R.id.tag_has_record, true);
                    dayView.setTag(R.id.tag_sleep_hour, sleepHour);
                    dayView.setTag(R.id.tag_sleep_date, dateStr);
                } else {
                    drawable.setColor(getResources().getColor(R.color.calendar_gray));
                    textColor = getResources().getColor(android.R.color.darker_gray);
                    dayView.setTag(R.id.tag_has_record, false);
                }

                dayView.setBackground(drawable);
                dayView.setTextColor(textColor);

                dayView.setOnClickListener(v -> showDetail(dateStr));

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 56;
                params.columnSpec = GridLayout.spec((firstDayOfWeek + day - 1) % 7, 1f);
                params.setMargins(2, 2, 2, 2);
                dayView.setLayoutParams(params);
                calendarGrid.addView(dayView);
            }
        }

        private void showDetail(String date) {
            if (!sleepDataMap.containsKey(date)) {
                Toast.makeText(getContext(), date + " 当天无记录", Toast.LENGTH_SHORT).show();
                return;
            }

            int hour = sleepDataMap.get(date);
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_sleep_detail, null);
            TextView tvDate = dialogView.findViewById(R.id.tvDetailDate);
            TextView tvTime = dialogView.findViewById(R.id.tvDetailTime);
            Button btnWithdraw = dialogView.findViewById(R.id.btnDetailWithdraw);
            Button btnClose = dialogView.findViewById(R.id.btnDetailClose);

            tvDate.setText(date);
            tvTime.setText("入睡时间：" + String.format("%02d:00", hour));

            AlertDialog dialog = builder.create();
            dialog.setView(dialogView);
            dialog.setCancelable(false);

            btnWithdraw.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("撤回确认")
                        .setMessage("确认撤回该记录？")
                        .setPositiveButton("确认", (d, which) -> {
                            if (parent != null && parent.dbHelper != null) {
                                DataBaseHelper helper = parent.dbHelper;
                                Cursor c = helper.getConfirmedEventsForMonth(date.substring(0, 7));
                                long id = -1;
                                if (c != null) {
                                    while (c.moveToNext()) {
                                        String dStr = c.getString(c.getColumnIndex(DataBaseHelper.COLUMN_SLEEP_DATE));
                                        if (dStr.equals(date)) {
                                            id = c.getLong(c.getColumnIndex(DataBaseHelper.COLUMN_ID));
                                            break;
                                        }
                                    }
                                    c.close();
                                }
                                if (id != -1) {
                                    parent.onWithdraw(id);
                                    dialog.dismiss();
                                    refreshCalendar();
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });

            btnClose.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }
    }
}
