package com.example.smartlight;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;

import java.util.Calendar;
import java.util.TimeZone;

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
    private ArrayList<TimeInterval> userPreferences;
    private ArrayList<TimeInterval> defaultPreferences = new ArrayList<>();
    private int currentHour, currentMinute;



    @Override
    public void onCreate() {
        super.onCreate();
        connectToMqttBroker();
        System.out.println("Testing if service works");
        createDefaultPreferences();
        updateSwedishTime();
    }


    private void updateSwedishTime() { //OBS!: Call this method everytime before using the currentHour and currentMinute variables!!!
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Stockholm"));
        currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        currentMinute = calendar.get(Calendar.MINUTE);
    }

    private void createDefaultPreferences(){
        //06:00-09:00 - 50 lux
        defaultPreferences.add(new TimeInterval(6, 0, 9, 0, 50));

        //(09:00 to 17:00) - 150 lux
        defaultPreferences.add(new TimeInterval(9, 0, 17, 0, 150));

        //(17:00 to 20:00) - 100 lux
        defaultPreferences.add(new TimeInterval(17, 0, 20, 0, 100));

        //(20:00 to 22:00) - 75 lux
        defaultPreferences.add(new TimeInterval(20, 0, 22, 0, 75));

        //(22:00 to 06:00) - 50 lux
        defaultPreferences.add(new TimeInterval(22, 0, 6, 0, 50));
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("userPreferences")) {
            String jsonPreferences = intent.getStringExtra("userPreferences");
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<TimeInterval>>() {}.getType();
            userPreferences = gson.fromJson(jsonPreferences, type);
        }
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
                updateSwedishTime();
                String luxValue = new String(message.getPayload());
                int currentLux = Integer.parseInt(luxValue);
                int desiredLux = getDesiredLux(currentHour, currentMinute);
                adjustLampBrightness(currentLux, desiredLux);

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

    private int getDesiredLux(int hour, int minute) {
        TimeInterval currentTimeInterval = null;

        for (TimeInterval pref : userPreferences) {
            if (((pref.getStartHour() <= hour) && (pref.getStartMinute() <= minute)) && ((pref.getEndHour() >= hour) && (pref.getEndMinute() >= minute))) {
                currentTimeInterval = pref;
                break;
            }
        }

        if (currentTimeInterval == null) {
            for (TimeInterval defaultPref : defaultPreferences) {
                if (((defaultPref.getStartHour() <= hour) && (defaultPref.getStartMinute() <= minute)) && ((defaultPref.getEndHour() >= hour) && (defaultPref.getEndMinute() >= minute))) {
                    currentTimeInterval = defaultPref;
                    break;
                }
            }
        }

        return currentTimeInterval.getValue();
    }

    private void adjustLampBrightness(int currentLux, int desiredLux) {
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
