package io.nandandesai.privacybreacher;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements ConfirmAdapter.OnConfirmListener {

    private RecyclerView rvRecords;
    private TextView tvStatus;
    private TextView tvEmpty;
    private TextView tvThreshold;
    private DataBaseHelper dbHelper;
    private ConfirmAdapter adapter;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvRecords = view.findViewById(R.id.rvRecords);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvThreshold = view.findViewById(R.id.tvThreshold);

        if (getContext() != null) {
            dbHelper = new DataBaseHelper(getContext());
            sharedPreferences = getContext().getSharedPreferences("SleepTrackerPrefs", Context.MODE_PRIVATE);
        }

        rvRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ConfirmAdapter(getContext(), this);
        rvRecords.setAdapter(adapter);

        loadData();
    }

    // 供 MainActivity 调用（设置页面修改阈值后刷新）
    public void refreshData() {
        loadData();
    }

    private void loadData() {
        updateThresholdDisplay();
        loadUnconfirmedRecords();
    }

    private void updateThresholdDisplay() {
        if (sharedPreferences == null) return;
        int hour = sharedPreferences.getInt("threshold_hour", 22);
        int minute = sharedPreferences.getInt("threshold_minute", 0);
        String time = String.format("%02d:%02d", hour, minute);
        tvThreshold.setText("睡眠区间：" + time);
    }

    private void loadUnconfirmedRecords() {
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

    @Override
    public void onConfirm(long id) {
        if (dbHelper != null && dbHelper.confirmEvent(id) > 0) {
            loadUnconfirmedRecords();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
