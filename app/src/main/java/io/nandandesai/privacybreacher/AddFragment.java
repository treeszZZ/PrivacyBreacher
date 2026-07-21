package io.nandandesai.privacybreacher;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;

public class AddFragment extends Fragment {

    private Button btnDate, btnTime, btnSave;
    private int selectedYear, selectedMonth, selectedDay;
    private int selectedHour, selectedMinute;
    private boolean isDateSelected = false;
    private boolean isTimeSelected = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnDate = view.findViewById(R.id.btnDate);
        btnTime = view.findViewById(R.id.btnTime);
        btnSave = view.findViewById(R.id.btnSave);

        // 默认选中当前日期和时间
        Calendar cal = Calendar.getInstance();
        selectedYear = cal.get(Calendar.YEAR);
        selectedMonth = cal.get(Calendar.MONTH);
        selectedDay = cal.get(Calendar.DAY_OF_MONTH);
        selectedHour = cal.get(Calendar.HOUR_OF_DAY);
        selectedMinute = cal.get(Calendar.MINUTE);
        isDateSelected = true;
        isTimeSelected = true;
        updateDateButton();
        updateTimeButton();

        btnDate.setOnClickListener(v -> showDatePicker());
        btnTime.setOnClickListener(v -> showTimePicker());
        btnSave.setOnClickListener(v -> saveRecord());
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = dayOfMonth;
                    isDateSelected = true;
                    updateDateButton();
                },
                selectedYear, selectedMonth, selectedDay
        );
        dialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;
                    isTimeSelected = true;
                    updateTimeButton();
                },
                selectedHour, selectedMinute, true
        );
        dialog.show();
    }

    private void updateDateButton() {
        String date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
        btnDate.setText(date);
    }

    private void updateTimeButton() {
        String time = String.format("%02d:%02d", selectedHour, selectedMinute);
        btnTime.setText(time);
    }

    private void saveRecord() {
        if (!isDateSelected || !isTimeSelected) {
            Toast.makeText(getContext(), "请完整选择日期和时间", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute);
        long timestamp = cal.getTimeInMillis();

        String sleepDate = DataBaseHelper.formatDate(timestamp);
        DataBaseHelper dbHelper = new DataBaseHelper(getContext());
        long id = dbHelper.insertEvent("SCREEN_OFF", timestamp, sleepDate);
        dbHelper.close();

        if (id > 0) {
            Toast.makeText(getContext(), "记录已添加", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                ((MainActivity) getActivity()).switchToHome();
            }
        } else {
            Toast.makeText(getContext(), "添加失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }
}
