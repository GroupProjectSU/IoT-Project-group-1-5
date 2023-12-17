package com.example.smartlight;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import android.widget.Button;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import com.google.gson.Gson;
import android.app.AlertDialog;

public class ScheduleActivity extends AppCompatActivity {

    private Button editButton, removeButton, resetButton, scheduleButton;
    private ListView scheduleList;
    private ArrayAdapter<TimeInterval> adapter;
    private ArrayList<TimeInterval> userPreferences;
    private int selectedPosition = -1;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen2);


        editButton = findViewById(R.id.editButton);
        removeButton = findViewById(R.id.removeButton);
        resetButton = findViewById(R.id.resetButton);
        scheduleList = findViewById(R.id.scheduledItemsList);
        scheduleButton = findViewById(R.id.scheduleButton);

        scheduleButton.setBackgroundColor(getResources().getColor(R.color.dark_purple));

        initializeSharedPreferences();

        userPreferences = (ArrayList<TimeInterval>) getIntent().getSerializableExtra("userPreferences");


        setupListViewAdapter();
        setupListViewClickListener();
        setupButtonListeners();
    }

    private void initializeSharedPreferences() {
        preferences = getSharedPreferences("com.example.smartlight.preferences", MODE_PRIVATE);
    }



    private void setupListViewAdapter() {
        adapter = new TimeIntervalAdapter(this, userPreferences);
        scheduleList.setAdapter(adapter);
    }

    private void setupListViewClickListener() {
        scheduleList.setOnItemClickListener((parent, view, position, id) -> selectedPosition = position);
    }

    private void setupButtonListeners() {
        removeButton.setOnClickListener(this::handleRemoveButtonClick);
        resetButton.setOnClickListener(this::handleResetButtonClick);
        editButton.setOnClickListener(this::handleEditButtonClick);
        Button homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(this::navigateBackToMainActivity);
    }

    private void handleRemoveButtonClick(View view) {
        if (selectedPosition >= 0 && !userPreferences.isEmpty()) {
            userPreferences.remove(selectedPosition);
            adapter.notifyDataSetChanged();
            selectedPosition = -1;
            saveUserPreferences(); //Save changes
        }
    }

    private void handleResetButtonClick(View view) {
        //create an AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Preferences");
        builder.setMessage("Are you sure that you want to reset your preferences?");

        //set the Positive button ("YES") and its click listener
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //user clicked "YES" so  the preferences gets cleared
                userPreferences.clear();
                adapter.notifyDataSetChanged();
                saveUserPreferences(); //save changes
            }
        });

        //set the Negative button ("Cancel") and its click listener
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //user clicked "Cancel", so dismiss the dialog and do nothing
                dialog.dismiss();
            }
        });

        //create and show the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void handleEditButtonClick(View view) {
        if (selectedPosition >= 0 && !userPreferences.isEmpty()) {
            TimeInterval selectedInterval = userPreferences.get(selectedPosition);
            Intent intent = new Intent(ScheduleActivity.this, EditActivity.class);
            intent.putExtra("selectedInterval", selectedInterval);
            intent.putExtra("selectedPosition", selectedPosition);
            startActivityForResult(intent, 2);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2 && resultCode == RESULT_OK) {
            TimeInterval updatedInterval = (TimeInterval) data.getSerializableExtra("updatedInterval");
            int updatedPosition = data.getIntExtra("updatedPosition", -1);
            if (updatedPosition >= 0) {
                userPreferences.set(updatedPosition, updatedInterval);
                adapter.notifyDataSetChanged();
                saveUserPreferences(); //Save changes
            }
        }
    }

    private void saveUserPreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(userPreferences);
        editor.putString("userPreferences", json);
        editor.apply();
    }

    private void navigateBackToMainActivity(View view) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("userPreferences", userPreferences);
        setResult(RESULT_OK, returnIntent);
        finish();
    }
}