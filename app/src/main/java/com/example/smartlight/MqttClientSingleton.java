/*package com.example.smartlight;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;
import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttClient;

public class MqttClientSingleton {
    private static final String TAG = "MqttClientSingleton"; // Tag for logging
    private static MqttAndroidClient mqttClient = null;

    public static synchronized MqttAndroidClient getInstance(Context context) {
        if (mqttClient == null) {
            String clientId = MqttClient.generateClientId();
            mqttClient = new MqttAndroidClient(context.getApplicationContext(), "tcp://test.mosquitto.org:1883", clientId, Ack.AUTO_ACK);

            // Connect with MQTT broker
            mqttClient.connect(null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Successfully connected to the MQTT broker");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Connection to the MQTT broker failed: " + exception.toString());

                }
            });
        }
        return mqttClient;
    }
}
*/