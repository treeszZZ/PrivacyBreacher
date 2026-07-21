package io.nandandesai.privacybreacher;

import android.app.TimePickerDialog;
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
    private SharedPreferences sharedPreferences;
    private int currentHour, currentMinute;

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
        sharedPreferences = getContext().getSharedPreferences("SleepTrackerPrefs", Context.MODE_PRIVATE);

        loadThreshold();

        tvSleepInterval.setOnClickListener(v -> showTimePicker());
    }

    private void loadThreshold() {
        int hour = sharedPreferences.getInt("threshold_hour", 22);
        int minute = sharedPreferences.getInt("threshold_minute", 0);
        String time = String.format("%02d:%02d", hour, minute);
        tvSleepInterval.setText(time);
        currentHour = hour;
        currentMinute = minute;
    }

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, minute) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("threshold_hour", hourOfDay);
                    editor.putInt("threshold_minute", minute);
                    editor.apply();
                    String time = String.format("%02d:%02d", hourOfDay, minute);
                    tvSleepInterval.setText(time);
                    Toast.makeText(getContext(), "睡眠区间已更新为 " + time, Toast.LENGTH_SHORT).show();
                },
                currentHour, currentMinute, true
        );
        dialog.show();
    }
}
