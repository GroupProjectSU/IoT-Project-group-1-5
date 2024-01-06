package com.example.smartlight;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;

import java.util.ArrayList;
import info.mqtt.android.service.MqttAndroidClient;

public class EditActivity extends AppCompatActivity {
    private Button confirmButton, cancelButton;
    private TimePicker timePicker1, timePicker2;
    private EditText valueInput;
    private ArrayList<TimeInterval> userPreferences; // Added to store all user preferences
    private int selectedPosition;
    private MqttAndroidClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen3);

        confirmButton = findViewById(R.id.confirmButton);
        cancelButton = findViewById(R.id.cancelButton);
        timePicker1 = findViewById(R.id.timePicker1);
        timePicker2 = findViewById(R.id.timePicker2);
        valueInput = findViewById(R.id.valueInput);

        timePicker1.setIs24HourView(true);
        timePicker2.setIs24HourView(true);

        userPreferences = (ArrayList<TimeInterval>) getIntent().getSerializableExtra("userPreferences");
        TimeInterval interval = (TimeInterval) getIntent().getSerializableExtra("selectedInterval");
        selectedPosition = getIntent().getIntExtra("selectedPosition", -1);

        confirmButton.setOnClickListener(v -> finishEditing(true, interval));
        cancelButton.setOnClickListener(v -> finishEditing(false, null));

        client = MainActivity.getMqttClient();
    }


    private void finishEditing(boolean isConfirmed, TimeInterval interval) {
        if (isConfirmed && interval != null) {
            interval.setStartHour(timePicker1.getCurrentHour());
            interval.setStartMinute(timePicker1.getCurrentMinute());
            interval.setEndHour(timePicker2.getCurrentHour());
            interval.setEndMinute(timePicker2.getCurrentMinute());

            String valueText = valueInput.getText().toString();
            int value;
            try {
                double doubleValue = Double.parseDouble(valueText);
                value = (int) Math.round(doubleValue);
            } catch (NumberFormatException e) {
                value = 0;
            }
            interval.setValue(value);

            // Create a copy of userPreferences excluding the interval being edited
            ArrayList<TimeInterval> tempPreferences = new ArrayList<>(userPreferences);
            if (selectedPosition >= 0 && selectedPosition < userPreferences.size()) {
                tempPreferences.remove(selectedPosition);
            }

            if (OverlappingTimeintervallChecker.checkForConflictingPreferences(tempPreferences, interval)) {
                showConflictResolutionDialog(interval);
            } else {
                userPreferences.set(selectedPosition, interval); // Update the interval
                updateAndReturn();
            }

        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void showConflictResolutionDialog(TimeInterval interval) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Time Interval Conflict");
        builder.setMessage("This time interval conflicts with existing intervals. Do you want to proceed and remove conflicting intervals?");

        builder.setPositiveButton("Proceed", (dialog, which) -> {
            OverlappingTimeintervallChecker.removeConflictingIntervals(interval, userPreferences);
            updateAndReturn(); // Call the updated method
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void updateAndReturn() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("userPreferences", userPreferences); // Return the updated list
        setResult(RESULT_OK, returnIntent);

        // Save preferences and publish to MQTT
        if (client != null && client.isConnected()) {
            MainActivity.updateAndPublishPreferences(this, userPreferences, client, getSharedPreferences("com.example.smartlight.preferences", MODE_PRIVATE));
        }
        finish();
    }

}