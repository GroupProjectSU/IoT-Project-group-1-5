package com.example.smartlight;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import java.util.Collections;

import com.google.gson.Gson;
import android.app.AlertDialog;

import info.mqtt.android.service.MqttAndroidClient;

public class ScheduleActivity extends AppCompatActivity {

    private Button editButton, removeButton, resetButton, scheduleButton;
    private ListView scheduleList;
    private ArrayAdapter<TimeInterval> adapter;
    private ArrayList<TimeInterval> userPreferences;
    private int selectedPosition = -1;
    private SharedPreferences preferences;
    private MqttAndroidClient client;

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
        prepareButtonListeners();
        clearSelection();

        client = MainActivity.getMqttClient();
    }

    private void initializeSharedPreferences() {
        preferences = getSharedPreferences("com.example.smartlight.preferences", MODE_PRIVATE);
    }

    private void setupListViewAdapter() {
        adapter = new TimeIntervalAdapter(this, userPreferences);
        scheduleList.setAdapter(adapter);
    }

    private void setupListViewClickListener() {
        scheduleList.setOnItemClickListener((parent, view, position, id) -> {
            if (selectedPosition == position) {
                view.setBackgroundColor(Color.TRANSPARENT);
                selectedPosition = -1;
            } else {
                selectedPosition = position;
                for (int i = 0; i < parent.getChildCount(); i++) {
                    parent.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                }
                view.setBackgroundColor(Color.LTGRAY);
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void prepareButtonListeners() {
        removeButton.setOnClickListener(this::handleRemoveButtonClick);
        resetButton.setOnClickListener(this::handleResetButtonClick);
        editButton.setOnClickListener(this::handleEditButtonClick);
        Button homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(this::navigateBackToMainActivity);
    }

    private void handleRemoveButtonClick(View view) {
        if (selectedPosition >= 0 && !userPreferences.isEmpty()) {
            userPreferences.remove(selectedPosition);
            clearSelection();
            saveAndUpdatePreferences();
        }
    }

    private void handleResetButtonClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Preferences");
        builder.setMessage("Are you sure that you want to reset your preferences?");
        builder.setPositiveButton("YES", (dialog, which) -> {
            userPreferences.clear();
            adapter.notifyDataSetChanged();
            saveAndUpdatePreferences();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void handleEditButtonClick(View view) {
        if (selectedPosition >= 0 && !userPreferences.isEmpty()) {
            TimeInterval selectedInterval = userPreferences.get(selectedPosition);
            Intent intent = new Intent(ScheduleActivity.this, EditActivity.class);
            intent.putExtra("selectedInterval", selectedInterval);
            intent.putExtra("selectedPosition", selectedPosition);
            intent.putExtra("userPreferences", userPreferences);
            clearSelection();
            startActivityForResult(intent, 2);
        }
    }

    private void saveAndUpdatePreferences() {
        MainActivity.updateAndPublishPreferences(this, userPreferences, client, preferences);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2 && resultCode == RESULT_OK) {
            ArrayList<TimeInterval> updatedPreferences = (ArrayList<TimeInterval>) data.getSerializableExtra("userPreferences");
            if (updatedPreferences != null) {
                userPreferences.clear();
                userPreferences.addAll(updatedPreferences);
                Collections.sort(userPreferences, MainActivity::compareTimeIntervals);
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void clearSelection() {
        selectedPosition = -1;
        adapter.notifyDataSetChanged();
        for (int i = 0; i < scheduleList.getChildCount(); i++) {
            scheduleList.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void navigateBackToMainActivity(View view) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("userPreferences", userPreferences);
        setResult(RESULT_OK, returnIntent);
        finish();
    }
}
