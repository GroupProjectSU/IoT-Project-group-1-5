package com.example.smartlight;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.util.ArrayList;

import info.mqtt.android.service.MqttAndroidClient;

public class mqttpublisher {
    public static void publishUserPreferences(Context context, MqttAndroidClient client, ArrayList<TimeInterval> userPreferences) {
        if (client != null && client.isConnected()) {
            Gson gson = new Gson();
            String json = gson.toJson(userPreferences);
            String publishTopic = "iot/prefLux";

                client.publish(publishTopic, json.getBytes(), 1, false);
                Log.d("MqttPublisher", "Published user preferences to MQTT topic: " + publishTopic);

        } else {
            Log.d("MqttPublisher", "Cannot publish, MQTT client is not connected");
        }
    }
}
