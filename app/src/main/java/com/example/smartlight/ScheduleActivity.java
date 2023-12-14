package com.example.smartlight;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ArrayAdapter;

public class ScheduleActivity extends AppCompatActivity {

    private Button editButton;
    private Button removeButton;
    private Button resetButton;
    private ListView scheduleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen2);

        editButton = findViewById(R.id.editButton);
        removeButton = findViewById(R.id.removeButton);
        resetButton = findViewById(R.id.resetButton);
        scheduleList = findViewById(R.id.scheduledItemsList);

        Button scheduleButton = findViewById(R.id.scheduleButton);
        scheduleButton.setBackgroundColor(getResources().getColor(R.color.dark_purple));


        ArrayAdapter<TimeInterval> adapter = new TimeIntervalAdapter(this, MainActivity.userPreferences);
        scheduleList.setAdapter(adapter);






        Button homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(ScheduleActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });


        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }
}
