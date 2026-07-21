package io.nandandesai.privacybreacher;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private TextView tvSleepInterval;
    private TextView tvColorThreshold;
    private SharedPreferences sharedPreferences;
    private int sleepHour, sleepMinute;
    private int colorHour, colorMinute;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvSleepInterval = view.findViewById(R.id.tvSleepInterval);
        tvColorThreshold = view.findViewById(R.id.tvColorThreshold);
        sharedPreferences = requireContext().getSharedPreferences("SleepTrackerPrefs", Context.MODE_PRIVATE);

        loadAllSettings();

        tvSleepInterval.setOnClickListener(v -> showTimePicker("sleep"));
        tvColorThreshold.setOnClickListener(v -> showTimePicker("color"));

        view.findViewById(R.id.cvExport).setOnClickListener(v -> exportData());
    }

    private void loadAllSettings() {
        sleepHour = sharedPreferences.getInt("threshold_hour", 22);
        sleepMinute = sharedPreferences.getInt("threshold_minute", 0);
        tvSleepInterval.setText(String.format("%02d:%02d", sleepHour, sleepMinute));

        colorHour = sharedPreferences.getInt("color_threshold_hour", 22);
        colorMinute = sharedPreferences.getInt("color_threshold_minute", 0);
        tvColorThreshold.setText(String.format("%02d:%02d", colorHour, colorMinute));
    }

    private void showTimePicker(String type) {
        int hour, minute;
        String key;
        if ("sleep".equals(type)) {
            hour = sleepHour;
            minute = sleepMinute;
            key = "睡眠区间";
        } else {
            hour = colorHour;
            minute = colorMinute;
            key = "标色阈值";
        }

        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minuteOfHour) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    if ("sleep".equals(type)) {
                        editor.putInt("threshold_hour", hourOfDay);
                        editor.putInt("threshold_minute", minuteOfHour);
                        sleepHour = hourOfDay;
                        sleepMinute = minuteOfHour;
                        tvSleepInterval.setText(String.format("%02d:%02d", hourOfDay, minuteOfHour));
                    } else {
                        editor.putInt("color_threshold_hour", hourOfDay);
                        editor.putInt("color_threshold_minute", minuteOfHour);
                        colorHour = hourOfDay;
                        colorMinute = minuteOfHour;
                        tvColorThreshold.setText(String.format("%02d:%02d", hourOfDay, minuteOfHour));
                    }
                    editor.apply();
                    Toast.makeText(getContext(), key + "已更新为 " + String.format("%02d:%02d", hourOfDay, minuteOfHour), Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        ((MainActivity) getActivity()).refreshHome();
                    }
                },
                hour, minute, true
        );
        dialog.show();
    }

    private void exportData() {
        DataBaseHelper dbHelper = new DataBaseHelper(getContext());
        Cursor cursor = dbHelper.getAllConfirmedEvents("DESC");
        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(getContext(), "暂无已确认数据可导出", Toast.LENGTH_SHORT).show();
            if (cursor != null) cursor.close();
            dbHelper.close();
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
            String fileName = "好梦_数据_" + sdf.format(new Date()) + ".csv";
            File file = new File(requireContext().getExternalFilesDir(null), fileName);

            FileWriter writer = new FileWriter(file);
            writer.append("日期,入睡时间,时间戳\n");
            while (cursor.moveToNext()) {
                String date = cursor.getString(cursor.getColumnIndex(DataBaseHelper.COLUMN_SLEEP_DATE));
                long timestamp = cursor.getLong(cursor.getColumnIndex(DataBaseHelper.COLUMN_TIMESTAMP));
                String time = DataBaseHelper.formatTime(timestamp);
                writer.append(date).append(",").append(time).append(",").append(String.valueOf(timestamp)).append("\n");
            }
            writer.flush();
            writer.close();
            cursor.close();
            dbHelper.close();

            Toast.makeText(getContext(), "导出成功", Toast.LENGTH_SHORT).show();

            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(android.content.Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(requireContext(),
                            requireContext().getPackageName() + ".fileprovider", file));
            shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(android.content.Intent.createChooser(shareIntent, "分享数据"));

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "导出失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }
}
