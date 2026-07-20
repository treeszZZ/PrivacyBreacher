package io.nandandesai.privacybreacher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ConfirmAdapter.OnConfirmListener {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private ConfirmAdapter adapter;
    private DataBaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.confirmRecyclerView);
        emptyText = findViewById(R.id.emptyText);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ConfirmAdapter(this, this);
        recyclerView.setAdapter(adapter);

        dbHelper = new DataBaseHelper(this);

        // 启动服务（如果未运行）
        if (!isMyServiceRunning(PrivacyBreacherService.class)) {
            Intent serviceIntent = new Intent(this, PrivacyBreacherService.class);
            startService(serviceIntent);
        }

        // 加载数据
        loadUnconfirmedRecords();
    }

    private void loadUnconfirmedRecords() {
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
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.setRecords(records);
        }
    }

    @Override
    public void onConfirm(long id) {
        // 标记为已确认
        int rows = dbHelper.confirmEvent(id);
        if (rows > 0) {
            // 刷新列表
            loadUnconfirmedRecords();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}