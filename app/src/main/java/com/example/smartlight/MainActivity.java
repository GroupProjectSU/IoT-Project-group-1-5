package com.example.smartlight;

import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TimePicker;
import android.app.AlertDialog;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Switch toggleButton;
    private SharedPreferences preferences;
    private static final String PREFERENCES_NAME = "com.example.smartlight.preferences";
    private static final String SWITCH_STATE_KEY = "switch_state";
    private Button homeButton, addButton, scheduleButton;
    private TimePicker timePicker1, timePicker2;
    private EditText valueInput;
    private ArrayList<TimeInterval> userPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen1);

        // Initialize the views and SharedPreferences
        initializeViews();
        initializeSharedPreferences();
        setupButtonListeners();

        // Initialize userPreferences
        userPreferences = new ArrayList<>();
    }

    private void initializeViews() {
        toggleButton = findViewById(R.id.toggleButton);
        addButton = findViewById(R.id.addButton);
        timePicker1 = findViewById(R.id.timePicker1);
        timePicker2 = findViewById(R.id.timePicker2);
        homeButton = findViewById(R.id.homeButton);
        scheduleButton = findViewById(R.id.scheduleButton);
        valueInput = findViewById(R.id.valueInput);

        timePicker1.setIs24HourView(true);
        timePicker2.setIs24HourView(true);
    }

    private void initializeSharedPreferences() {
        preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        boolean switchState = preferences.getBoolean(SWITCH_STATE_KEY, false);
        toggleButton.setChecked(switchState);
    }

    private void setupButtonListeners() {
        addButton.setOnClickListener(this::handleAddButtonClick);
        scheduleButton.setOnClickListener(this::handleScheduleButtonClick);
        homeButton.setBackgroundColor(getResources().getColor(R.color.dark_purple));
    }

    private void handleAddButtonClick(View view) {
        int startHour = timePicker1.getCurrentHour();
        int startMinute = timePicker1.getCurrentMinute();
        int endHour = timePicker2.getCurrentHour();
        int endMinute = timePicker2.getCurrentMinute();

        if (valueInput.getText().toString().isEmpty()) {
            showValueInputErrorDialog();
            return;
        }

        int value;
        try {
            // Parse the input as a double, then round to the nearest integer
            double doubleValue = Double.parseDouble(valueInput.getText().toString());
            value = (int) Math.round(doubleValue);
        } catch (NumberFormatException e) {
            value = 0; // Default to 0 if the input is not a valid number
        }

        TimeInterval newInterval = new TimeInterval(startHour, startMinute, endHour, endMinute, value);

        if (checkForConflictingTimeInterval(newInterval)) {
            showConflictResolutionDialog(newInterval);
        } else {
            userPreferences.add(newInterval);
        }
    }

    private void showValueInputErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Input Error");
        builder.setMessage("Please enter a value to proceed.");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private double parseValueInput() {
        try {
            return Double.parseDouble(valueInput.getText().toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void handleScheduleButtonClick(View view) {
        Intent intent = new Intent(MainActivity.this, ScheduleActivity.class);
        intent.putExtra("userPreferences", userPreferences);
        startActivityForResult(intent, 1); // Use startActivityForResult
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Update userPreferences from the returned data
            userPreferences = (ArrayList<TimeInterval>) data.getSerializableExtra("userPreferences");
        }
    }

    private boolean checkForConflictingTimeInterval(TimeInterval newInterval) {
        for (TimeInterval existingInterval : userPreferences) {
            if (isTimeOverlap(existingInterval, newInterval)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTimeOverlap(TimeInterval interval1, TimeInterval interval2) {
        // Convert times to minutes since midnight
        int start1 = interval1.getStartHour() * 60 + interval1.getStartMinute();
        int end1 = interval1.getEndHour() * 60 + interval1.getEndMinute();
        int start2 = interval2.getStartHour() * 60 + interval2.getStartMinute();
        int end2 = interval2.getEndHour() * 60 + interval2.getEndMinute();

        // Handle intervals that cross midnight
        if (end1 <= start1) end1 += 24 * 60;
        if (end2 <= start2) end2 += 24 * 60;

        // Check if interval1 is within one day and interval2 spans across two days
        if (end2 > 24 * 60 && start1 < end1) {
            if (start1 < (end2 - 24 * 60)) return true;
            if (end1 > start2 + 24 * 60) return true;
        }

        // Check if interval2 is within one day and interval1 spans across two days
        if (end1 > 24 * 60 && start2 < end2) {
            if (start2 < (end1 - 24 * 60)) return true;
            if (end2 > start1 + 24 * 60) return true;
        }

        // Standard overlap check for intervals within the same day
        return start1 < end2 && start2 < end1;
    }

    private void showConflictResolutionDialog(TimeInterval newInterval) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Time Interval Conflict");
        builder.setMessage("There is already a time interval that overlaps with the one you want to create. Do you want to remove the overlapping interval and proceed?");

        builder.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeConflictingTimeInterval(newInterval);
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void removeConflictingTimeInterval(TimeInterval newInterval) {
        ArrayList<TimeInterval> intervalsToRemove = new ArrayList<>();

        for (TimeInterval interval : userPreferences) {
            if (isTimeOverlap(interval, newInterval)) {
                intervalsToRemove.add(interval);
            }
        }

        userPreferences.removeAll(intervalsToRemove);
        userPreferences.add(newInterval);
    }
}