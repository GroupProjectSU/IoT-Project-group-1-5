package com.example.smartlight;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TimePicker;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Switch toggleButton;
    private SharedPreferences preferences; //this is a way to store data in key value pairs - used for the switch
    private static final String PREFERENCES_NAME = "com.example.smartlight.preferences";
    private static final String SWITCH_STATE_KEY = "switch_state";
    private Button homeButton;
    private Button addButton;
    private Button scheduleButton;
    private TimePicker timePicker1;
    private TimePicker timePicker2;
    private EditText valueInput;

    public static ArrayList<TimeInterval> userPreferences = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen1);

        //connect java-buttons to xml-buttons
        toggleButton = findViewById(R.id.toggleButton);
        addButton = findViewById(R.id.addButton);
        timePicker1 = findViewById(R.id.timePicker1);
        timePicker2 = findViewById(R.id.timePicker2);
        homeButton = findViewById(R.id.homeButton);
        scheduleButton = findViewById(R.id.scheduleButton);
        valueInput = findViewById(R.id.valueInput);



        // Initialize SharedPreferences
        preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);

        //Restore the saved state of the switch
        boolean switchState = preferences.getBoolean(SWITCH_STATE_KEY, false); //Default is false
        toggleButton.setChecked(switchState);


        toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //Save the switch state
            preferences.edit().putBoolean(SWITCH_STATE_KEY, isChecked).apply();


            if(isChecked) {

            } else {

            }
        });

        //this makes the timepickers in 24 hour format instead of AM/PM format
        timePicker1.setIs24HourView(true);
        timePicker2.setIs24HourView(true);


        //This makes the home button darker, indicating it's the home screen.
        homeButton.setBackgroundColor(getResources().getColor(R.color.dark_purple));



        //add button listener
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //ADD functionality
                int startHour = timePicker1.getCurrentHour();
                int startMinute = timePicker1.getCurrentMinute();
                int endHour = timePicker2.getCurrentHour();
                int endMinute = timePicker2.getCurrentMinute();

                double value;
                try { //If the user hasn't put a number...
                    value = Double.parseDouble(valueInput.getText().toString());
                } catch (NumberFormatException e) {
                    value = 0; //set a default value for now, fix later
                }

                //Create a new TimeInterval object
                TimeInterval interval = new TimeInterval(startHour, startMinute, endHour, endMinute, value);

                //Add to the users collection
                userPreferences.add(interval);
            }
        });


        //listener for Schedule button to navigate to ScheduleActivity/screen2
        scheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //navigate to ScheduleActivity
                Intent intent = new Intent(MainActivity.this, ScheduleActivity.class);
                intent.putExtra("userPreferences", userPreferences); // Pass the userpreference list to schedule activity
                startActivity(intent);
            }
        });

    }
}
