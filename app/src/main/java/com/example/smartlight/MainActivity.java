package com.example.smartlight;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.app.AlertDialog;

import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;


public class MainActivity extends AppCompatActivity {

    //SharedPreferences variables
    private SharedPreferences preferencesToSave; //this object is used to save and retrieve data as key-value pairs
    private static final String preferenceName = "com.example.smartlight.preferences";  //it works as a key for the shared preference file name for where the preference will be saved
    private static final String switchStateKey = "switch_state"; //this is the key to save and retrieve data for the switch state

    //UI variables
    private Switch toggleButton;
    private Button homeButton, addButton, scheduleButton;
    private TimePicker timePicker1, timePicker2;
    private EditText valueInput;
    private ArrayList<TimeInterval> userPreferences;
    private TextView luxView, lampStatusLabel;

    //MQTT-variables
    private static final String TAG = "MainActivity";  //this constant is used to categorise the log messages in a tag, so that we can filter the log messages
    private static final String SERVER_URI = "tcp://test.mosquitto.org:1883"; //this tells the client where to connect to the broker, for publishing and recieving messages.
    private static final String topic = "iot/sensor"; //the topic where the lux sensors readings are being published
    private static MqttAndroidClient client; //the MqttAndroidClient allows the application to connect and communicate with to the broker

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen1);

        connectComponents();
        prepareSharedPreferences();
        loadUserPreferences();
        connect();
    }


    private void connectComponents() {
        toggleButton = findViewById(R.id.toggleButton);
        addButton = findViewById(R.id.addButton);
        timePicker1 = findViewById(R.id.timePicker1);
        timePicker2 = findViewById(R.id.timePicker2);
        homeButton = findViewById(R.id.homeButton);
        scheduleButton = findViewById(R.id.scheduleButton);
        valueInput = findViewById(R.id.valueInput);
        luxView = findViewById(R.id.lightIntensity);
        lampStatusLabel = findViewById(R.id.switchStatusLabel);

        timePicker1.setIs24HourView(true);
        timePicker2.setIs24HourView(true);
        homeButton.setBackgroundColor(getResources().getColor(R.color.dark_purple));

        setToggleButtonListener();

        addButton.setOnClickListener(this::handleAddButtonClick);
        scheduleButton.setOnClickListener(this::handleScheduleButtonClick);
    }

    private void prepareSharedPreferences() {
        preferencesToSave = getSharedPreferences(preferenceName, MODE_PRIVATE); //the get-method is used to declare the sharedpreference object as a file that you can store and retrieve key-value pairs, with a mode (private/public)
        boolean switchState = preferencesToSave.getBoolean(switchStateKey, false);  //this retrieves the boolean value of the key "switchStateKey" in the preferences file, the false argument is just a default value that the get method will return if the "switchStateKey" has no value-pair. Then puts that value in a variable
        toggleButton.setChecked(switchState); //this then sets the switch state according to the retrieved boolean value.
    }


    private void connect() {
        String clientId = MqttClient.generateClientId();  //this creates a unique ID for the client to use for the broker. so that the broker can manage the clients session.
        client = new MqttAndroidClient(this.getApplicationContext(), SERVER_URI, clientId, Ack.AUTO_ACK); //this creates a client with information regarding, the applications context (recourses, classes...), the servers URI (where to connect to the broker), the uniquely generated client ID, and Ack.AUTO_ACK which automatically acknowledge received messages.

        client.connect(null, new IMqttActionListener() {  //this method tries to establish a connecttion with the broker, with a listener as a parameter that triggers callbacks regarding the connection
            @Override
            public void onSuccess(IMqttToken asyncActionToken) { //this callback method will run if the connection to the broker was successful
                Log.d(TAG, "successfully connected to the MQTT broker");
                subscribeToTopic(); //with a successful connection to the broker, a subscription to a topic can be initiated.
                System.out.println("subscribed to topic: " + topic);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {//this callback method will run if the connection to the broker was unsuccessful
                Log.e(TAG, "connection to the MQTT Broker failed");
                System.out.println("subscription to the topic: " + topic + "failed");
            }
        });

        client.setCallback(new MqttCallbackExtended() { //handles actions/events regarding the connection to the mqtt broker.
            @Override
            public void connectComplete(boolean reconnect, String serverURI) { //when a connection is done (either initially or a reconnection) then the (re)subscription is also done
                if (reconnect) {
                    Log.d(TAG, "successfully reconnected to : " + serverURI);
                    subscribeToTopic();
                }
            }

            @Override
            public void connectionLost(Throwable cause) { //when the connection to the broker is lost, the write a messegae to the log.
                Log.d(TAG, "the connection to the MQTT Broker is lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) { //this method gets triggered when a message is sent from the server, containing the topic in which the message was sent to, and the message

                String luxValue = new String(message.getPayload()); //this extracts the message and puts it in a string variable

                Log.d(TAG, "message received from the topic " + topic + ": " + luxValue);
                System.out.println("message received in topic " + topic + ": " + luxValue);
                System.out.println(message);
                luxView.setText("Current light intensity: " + luxValue);//shows the lux value in the application
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) { //when a message is published by the client to the broker, this method gets triggered
            }
        });
    }

    public static MqttAndroidClient getMqttClient() {   //this gives the other classes access to the mqtt client so that they can use it to publish to the broker
        return client;
    }

    private void subscribeToTopic() { //this method makes the client subscribe to a topic, so that the application can recieve the lux values from the lux sensor
        client.subscribe(topic, 1, null, new IMqttActionListener() { //this method attempts to subscribe to a specific topic (iot/sensor), with information regarding quality of service (1 as in atleast one try for the broker to deliver the message), and it also includes a listener as a parameter that triggers callbacks regarding the subscription attempt.
            @Override
            public void onSuccess(IMqttToken asyncActionToken) { //this callback method will run if the subscription to the broker was successful
                System.out.println("successfully subscribed to the topic: " + topic);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) { //this callback method will run if the subscription to the broker was unsuccessful
                System.out.println("subscription to the topic: " + topic + " failed");
            }
        });
    }

    private void setToggleButtonListener() {
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //Save the state of the switch
                lampStatusLabel.setText(isChecked ? "Lamp is ON" : "Lamp is OFF");
                SharedPreferences.Editor editor = preferencesToSave.edit(); //SharedPreferences.Editor object allows you to modify the values in SharedPreferences (in this case the preferences file)
                editor.putBoolean(switchStateKey, isChecked); //puts the new value of the key "switchStateKey" to the boolean of the switch that represents it's state
                editor.apply(); //applies the changes to the file

                //Publish to a topic in the broker the switches state
                if (client != null && client.isConnected()) {
                    String publishTopic = "iot/switch";
                    String message = Boolean.toString(isChecked);
                    client.publish(publishTopic, message.getBytes(), 1, true);
                    Log.d(TAG, "The switches state is checked: " + message + " + and it's state has been published to the topic: " + publishTopic);
                } else {
                    Log.d(TAG, "The switches state has not been published, check if the MQTT client is connected");
                }

            }
        });
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
            //put the lux value in variable and round it up to an int
            double doubleValue = Double.parseDouble(valueInput.getText().toString());
            value = (int) Math.round(doubleValue);
        } catch (NumberFormatException e) {
            value = 0; //if there were any errors, the value would be 0
        }

        TimeInterval newInterval = new TimeInterval(startHour, startMinute, endHour, endMinute, value);

        ArrayList<TimeInterval> tempPreferences = new ArrayList<>(userPreferences);

        //before adding a new user preference, we must see if he has any other preference that conflicts with the one he wants to create
        if (OverlappingTimeintervallChecker.checkForConflictingPreferences(tempPreferences, newInterval)) {
            showConflictResolutionDialog(newInterval);
        } else {
            userPreferences.add(newInterval);
            updateAndPublishPreferences(this, userPreferences, client, preferencesToSave);
        }
    }


    private static void saveUserPreferences(ArrayList<TimeInterval> userPrefs, SharedPreferences sharedPreferences) {
        Collections.sort(userPrefs, (i1, i2) -> compareTimeIntervals(i1, i2));
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(userPrefs);
        editor.putString("userPreferences", json);
        editor.apply();
    }


    public static void updateAndPublishPreferences(Context context, ArrayList<TimeInterval> userPrefs, MqttAndroidClient mqttClient, SharedPreferences sharedPreferences) {
        //save
        saveUserPreferences(userPrefs, sharedPreferences);

        //publish
        if (mqttClient != null && mqttClient.isConnected()) {
            publishUserPreferences(mqttClient, userPrefs, "iot/prefLux");
        } else {
            Log.e(TAG, "MQTT Client is not connected. Attempting to reconnect...");
            // Attempt to reconnect and then publish
            mqttClient.connect(null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Reconnected to MQTT Broker. Attempting to publish again.");
                    publishUserPreferences(mqttClient, userPrefs, "iot/prefLux");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Failed to reconnect to MQTT Broker: " + exception.getMessage());
                }
            });
        }
    }

    public static void publishUserPreferences(MqttAndroidClient client, ArrayList<TimeInterval> userPreferences, String topic) {
        if (client != null && client.isConnected()) {
            Gson gson = new Gson();
            String json = gson.toJson(userPreferences);
            client.publish(topic, json.getBytes(), 1, true);
            Log.d(TAG, "An updated user preference list has been published to the topic: " + topic);

        } else {
            Log.d(TAG, "The user preference list failed to publish, check if the client is connected");
        }
    }

    private void loadUserPreferences() {
        Gson gson = new Gson(); //gson is a library that can convert java objects to a JSON formatted string and vice versa. This was needed since lists was too complex to be stored in the sharedpreferences
        String json = preferencesToSave.getString("userPreferences", null); //retrieves the json string that is stored in the "userPreferences" key, if there is no value, then null will be retrieved
        Type type = new TypeToken<ArrayList<TimeInterval>>() {
        }.getType(); //defines which type of data that the gson will convert the json string to, which in this case is ArrayList<TimeInterval>
        userPreferences = gson.fromJson(json, type); //the gson converts the retrieved json-string to the defiened type (ArrayList<TimeInterval>)

        if (userPreferences == null) {
            userPreferences = new ArrayList<>();
        } else {
            //sort the list after loading according to the compareTimeIntervals method as a comparator
            Collections.sort(userPreferences, (i1, i2) -> compareTimeIntervals(i1, i2));
        }
    }

    public static int compareTimeIntervals(TimeInterval i1, TimeInterval i2) {
        //compare by end time
        int endComparison = Integer.compare(i1.getEndHour() * 60 + i1.getEndMinute(), i2.getEndHour() * 60 + i2.getEndMinute());
        return endComparison;
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        Intent intent = new Intent(MainActivity.this, ScheduleActivity.class);
        intent.putExtra("userPreferences", userPreferences);
        startActivityForResult(intent, 1);
    }


    @Override
    //this method handles the result returned from the ScheduleActivity via the startActivityForResult-method
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            ArrayList<TimeInterval> updatedPreferences = (ArrayList<TimeInterval>) data.getSerializableExtra("userPreferences");
            if (updatedPreferences != null) {
                userPreferences.clear();
                userPreferences.addAll(updatedPreferences);
            }
        }
    }

    private void showConflictResolutionDialog(TimeInterval newInterval) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Time Interval Conflict");
        builder.setMessage("There is already a time interval that conflicts with the one you want to create! Do you want to remove the conflicting interval and proceed?");

        builder.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                OverlappingTimeintervallChecker.removeConflictingIntervals(newInterval, userPreferences);
                dialog.dismiss();
                updateAndPublishPreferences(MainActivity.this, userPreferences, client, preferencesToSave);
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

}