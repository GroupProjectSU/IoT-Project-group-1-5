package com.example.smartlight;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;

public class EditActivity extends AppCompatActivity {

    private Button confirmButton, cancelButton;
    private TimePicker timePicker1, timePicker2;
    private EditText valueInput;
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

        TimeInterval interval = (TimeInterval) getIntent().getSerializableExtra("selectedInterval");
        int selectedPosition = getIntent().getIntExtra("selectedPosition", -1);

        // Populate time pickers and value input
        populateFields(interval);

        confirmButton.setOnClickListener(v -> finishEditing(true, interval, selectedPosition));
        cancelButton.setOnClickListener(v -> finishEditing(false, null, -1));
    }

    private void populateFields(TimeInterval interval) {
        if (interval != null) {
            timePicker1.setCurrentHour(interval.getStartHour());
            timePicker1.setCurrentMinute(interval.getStartMinute());
            timePicker2.setCurrentHour(interval.getEndHour());
            timePicker2.setCurrentMinute(interval.getEndMinute());
            valueInput.setText(String.valueOf(interval.getValue()));
        }
    }

    private void finishEditing(boolean isConfirmed, TimeInterval interval, int position) {
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
                value = 0; //For now the default will be 0, maybe change to an alert later.
            }
            interval.setValue(value);

            Intent returnIntent = new Intent();
            returnIntent.putExtra("updatedInterval", interval);
            returnIntent.putExtra("updatedPosition", position);
            setResult(RESULT_OK, returnIntent);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }}