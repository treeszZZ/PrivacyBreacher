package io.nandandesai.privacybreacher;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
    }

    private void loadAllSettings() {
        // 睡眠区间
        sleepHour = sharedPreferences.getInt("threshold_hour", 22);
        sleepMinute = sharedPreferences.getInt("threshold_minute", 0);
        tvSleepInterval.setText(String.format("%02d:%02d", sleepHour, sleepMinute));

        // 标色阈值
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

                    // 通知 MainActivity 刷新首页
                    if (getActivity() != null) {
                        ((MainActivity) getActivity()).refreshHome();
                    }
                },
                hour, minute, true
        );
        dialog.show();
    }
}
