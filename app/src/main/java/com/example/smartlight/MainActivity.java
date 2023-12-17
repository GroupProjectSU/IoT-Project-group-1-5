package com.example.smartlight;

import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TimePicker;
import android.app.AlertDialog;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collections;


public class MainActivity extends AppCompatActivity {

    private Switch toggleButton;
    private SharedPreferences preferences; //this object is used to save and retrieve data as key-value pairs
    private static final String PREFERENCES_NAME = "com.example.smartlight.preferences";  //it works as a key for the shared preference file name for where the preference will be saved
    private static final String SWITCH_STATE_KEY = "switch_state"; //this is the key to save and retrieve data
    private Button homeButton, addButton, scheduleButton;
    private TimePicker timePicker1, timePicker2;
    private EditText valueInput;
    private ArrayList<TimeInterval> userPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen1);

        initializeViews();
        initializeSharedPreferences();
        loadUserPreferences();
        setupButtonListeners();
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

        setToggleButtonListener();

        homeButton.setBackgroundColor(getResources().getColor(R.color.dark_purple));
    }

    private void initializeSharedPreferences() {
        preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE); //the get-method is used to declare the sharedpreference object as a file that you can store and retrieve key-value pairs, with a mode (private/public)
        boolean switchState = preferences.getBoolean(SWITCH_STATE_KEY, false);  //this retrieves the boolean value of the key "SWITCH_STATE_KEY" in the preferences file, the false argument is just a default value that the get method will return if the "SWITCH_STATE_KEY" has no value-pair. Then puts that value in a variable
        toggleButton.setChecked(switchState); //this then sets the switch state according to the retrieved boolean value.
    }


    private void setToggleButtonListener() {
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //Save the switchs state
                SharedPreferences.Editor editor = preferences.edit(); //SharedPreferences.Editor object allows you to modify the values in SharedPreferences (in this case the preferences file)
                editor.putBoolean(SWITCH_STATE_KEY, isChecked); //puts the new value of the key "SWITCH_STATE_KEY" to the boolean of the switch that represents it's state
                editor.apply(); //applies the changes to the file
            }
        });
    }


    private void setupButtonListeners() {
        addButton.setOnClickListener(this::handleAddButtonClick);
        scheduleButton.setOnClickListener(this::handleScheduleButtonClick);

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
            //parse the input as a double then round it to the nearest int
            double doubleValue = Double.parseDouble(valueInput.getText().toString());
            value = (int) Math.round(doubleValue);
        } catch (NumberFormatException e) {
            value = 0; //puts the value as 0 if the input is not a valid number
        }

        TimeInterval newInterval = new TimeInterval(startHour, startMinute, endHour, endMinute, value);

        if (checkForConflictingTimeInterval(newInterval)) {
            showConflictResolutionDialog(newInterval);
        } else {
            userPreferences.add(newInterval);
            saveUserPreferences(); //Save the updated list to SharedPreferences
        }
    }

    private void saveUserPreferences() {
        //Sort and save the list
        Collections.sort(userPreferences, (i1, i2) -> compareTimeIntervals(i1, i2)); //sorts the list according to the compareTimeIntervals method as a comparator
        SharedPreferences.Editor editor = preferences.edit(); //SharedPreferences.Editor object allows you to modify the values in SharedPreferences (in this case the preferences file)
        Gson gson = new Gson(); //gson is a library that can convert java objects to a JSON formatted string and vice versa. This was needed since lists was too complex to be stored in the sharedpreferences
        String json = gson.toJson(userPreferences); //converts the userpreference list into JSON format
        editor.putString("userPreferences", json); //puts the json string (userpreference list) in the key "userPreferences"
        editor.apply(); //applies the changes to the file
    }

    private void loadUserPreferences() {
        Gson gson = new Gson(); //gson is a library that can convert java objects to a JSON formatted string and vice versa. This was needed since lists was too complex to be stored in the sharedpreferences
        String json = preferences.getString("userPreferences", null); //retrieves the json string that is stored in the "userPreferences" key, if there is no value, then null will be retrieved
        Type type = new TypeToken<ArrayList<TimeInterval>>() {}.getType(); //defines which type of data that the gson will convert the json string to, which in this case is ArrayList<TimeInterval>
        userPreferences = gson.fromJson(json, type); //the gson converts the retrieved json-string to the defiened type (ArrayList<TimeInterval>)

        if (userPreferences == null) {
            userPreferences = new ArrayList<>();
        } else {
            //sort the list after loading according to the compareTimeIntervals method as a comparator
            Collections.sort(userPreferences, (i1, i2) -> compareTimeIntervals(i1, i2));
        }
    }

    private int compareTimeIntervals(TimeInterval i1, TimeInterval i2) {
        //compare by end time
        int endComparison = Integer.compare(i1.getEndHour() * 60 + i1.getEndMinute(),
                i2.getEndHour() * 60 + i2.getEndMinute());
        return endComparison;
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveUserPreferences(); //Override the onPause method to save the list of intervals when the application pauses
    }


    private void showValueInputErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Input Error");
        builder.setMessage("Please enter a value to proceed.");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss()); //which represent the dialog interface
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void handleScheduleButtonClick(View view) {
        Intent intent = new Intent(MainActivity.this, ScheduleActivity.class); //navigate from MainActivity to ScheduleActivity
        intent.putExtra("userPreferences", userPreferences); //sends data alongside the intent, in this case the userpreference-list
        startActivityForResult(intent, 1); //startActivityForResult is used to start an activity and receive a result back from it (so that main activity can recieve changes regarding list data), the 1 is used as a code that can identify the request.
    }

    @Override //this method handles the result returned from the ScheduleActivity via the startActivityForResult-method
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) { //if the result from activity is 1 (i.e. coming from Schedule activivty) and the operations in the activity was succeeded
            //update userPreferences from the returned data
            userPreferences = (ArrayList<TimeInterval>) data.getSerializableExtra("userPreferences"); //userPreferences is updated with data received from ScheduleActivity
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
        //convert times to minutes since midnight
        int start1 = interval1.getStartHour() * 60 + interval1.getStartMinute();
        int end1 = interval1.getEndHour() * 60 + interval1.getEndMinute();
        int start2 = interval2.getStartHour() * 60 + interval2.getStartMinute();
        int end2 = interval2.getEndHour() * 60 + interval2.getEndMinute();

        //handle intervals that cross midnight
        if (end1 <= start1) end1 += 24 * 60;
        if (end2 <= start2) end2 += 24 * 60;

        //check if interval1 is within one day and interval2 spans across two days
        if (end2 > 24 * 60 && start1 < end1) {
            if (start1 < (end2 - 24 * 60)) return true;
            if (end1 > start2 + 24 * 60) return true;
        }

        //check if interval2 is within one day and interval1 spans across two days
        if (end1 > 24 * 60 && start2 < end2) {
            if (start2 < (end1 - 24 * 60)) return true;
            if (end2 > start1 + 24 * 60) return true;
        }

        //standard time overlap (check for intervals within the same day)
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