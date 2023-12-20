package com.example.smartlight;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;

public class MqttService extends Service {
    private static final String TAG = "MqttService";
    private static final String SERVER_URI = "tcp://test.mosquitto.org:1883";
    private static final String TOPIC = "iot/sensor";
    public static final String LUX_VALUE_INTENT = "com.example.smartlight.LUX_VALUE_INTENT";
    public static final String LUX_VALUE_EXTRA = "luxValue";

    private MqttAndroidClient client;

    @Override
    public void onCreate() {
        super.onCreate();
        connectToMqttBroker();
        System.out.println("Testing if service works");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void connectToMqttBroker() {
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), SERVER_URI, clientId, Ack.AUTO_ACK);

        client.connect(null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d(TAG, "Connected to MQTT Broker!");
                subscribeToTopic();
                System.out.println("Subscribed successfully to topic: " + TOPIC);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e(TAG, "Failed to connect to MQTT Broker!");
                System.out.println("Failed to subscribe to topic: " + TOPIC);
            }
        });

        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    Log.d(TAG, "Reconnected to : " + serverURI);
                    subscribeToTopic();
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "Connection to MQTT Broker lost!");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String luxValue = new String(message.getPayload());
                Log.d(TAG, "Message received: " + luxValue);
                System.out.println("Message received on topic " + topic + ": " + luxValue);
                System.out.println(message);
                broadcastLuxValue(luxValue);
                Intent intent = new Intent("com.example.smartlight.LUX_UPDATE");
                intent.putExtra("lux", luxValue);
                sendBroadcast(intent);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
    }

    private void subscribeToTopic() {
        client.subscribe(TOPIC, 1, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                System.out.println("Subscription successful to topic: " + TOPIC);
            }
            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                System.out.println("Failed to subscribe to topic: " + TOPIC);
            }
        });
    }

    private void broadcastLuxValue(String luxValue) {
        Intent intent = new Intent(LUX_VALUE_INTENT);
        intent.putExtra(LUX_VALUE_EXTRA, luxValue);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
