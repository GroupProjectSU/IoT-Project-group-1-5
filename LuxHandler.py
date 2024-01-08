from TimeInterval import TimeInterval
import paho.mqtt.client as mqtt

from datetime import datetime, timedelta, time
import time as time_m #renamed so that it doesn't get confused with time from datetime

#LIFX lamp
import requests
import json

# sensor
import board
import busio
import adafruit_tsl2591 	# lux sensor


# The recieved data from the broker
timeIntervals = []
switchState = True

# Initialize I2C bus and lux sensor.
i2c = busio.I2C(board.SCL, board.SDA)
lux_sensor = adafruit_tsl2591.TSL2591(i2c)	


# MQTT broker and topic to subscribe to
mqtt_broker = "test.mosquitto.org"
topic_sub = "iot/prefLux"
topic_sub2 = "iot/switch"

#Lamp
LAMP_ID = "id:d073d53b9e28"
api_token = ""
brightness_level = 1 
is_adjusting = False


def set_lamp_brightness(token, selector, brightness):
    global brightness_level
    powerValue = "on" if brightness > 0.0 and switchState else "off"
    url = f"https://api.lifx.com/v1/lights/{selector}/state"
    payload = {
        "power": powerValue, 
        "brightness": brightness
    }
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    requests.put(url, json=payload, headers=headers)
    brightness_level = brightness

   
def increment_brightness(token, selector, increment):
    new_brightness = max(min(brightness_level + increment, 1.0), 0.0)
    set_lamp_brightness(token, selector, new_brightness)
    

def decrement_brightness(token, selector, decrement):
    new_brightness = max(min(brightness_level - decrement, 1.0), 0.0)
    set_lamp_brightness(token, selector, new_brightness)
   

def get_lux():
	return lux_sensor.lux


def setPrefLux():
    global is_adjusting
    now = datetime.now()
    current_time = now.time()

    # Default values in case there is no current time interval
    if (time(00, 00) <= current_time < time(8, 00)):
        default_value = 0
    elif (time(21, 00) <= current_time <= time(23, 59)):
        default_value = 10
    elif (time(8, 00) <= current_time <= time(8, 10)):
        default_value = 20
    else:
        default_value = 50
    
    # Find current time interval
    current_interval = None
    for interval in timeIntervals:
        start_time = datetime.strptime(f"{interval.start_hour:02d}:{interval.start_minute:02d}", "%H:%M").time()
        end_time = datetime.strptime(f"{interval.end_hour:02d}:{interval.end_minute:02d}", "%H:%M").time()

        if start_time <= current_time < end_time or start_time >= end_time and (start_time <= current_time or current_time < end_time):
            current_interval = interval
            break
        
    # Use the current time interval's value, if not available, use the default value
    target_lux = current_interval.get_value() if current_interval else default_value
   
    if target_lux == 0:
        set_lamp_brightness(api_token, LAMP_ID, 0)
    
    # Adjust the lamp brightness until the lux that the sensor receives is within the desired range.
    tolerance = 0.4
    is_adjusting = True

    while not (target_lux - tolerance <= get_lux() <= target_lux + tolerance):
        if not is_adjusting:
            break
        elif brightness_level == 1 and get_lux() < target_lux or brightness_level == 0 and get_lux() > target_lux:
            break

        if get_lux() < target_lux:
            increment_brightness(api_token, LAMP_ID, 0.02) 
        elif get_lux() > target_lux:
            decrement_brightness(api_token, LAMP_ID, 0.02) 

        # pause, before rechecking
        time_m.sleep(0.3)

    is_adjusting = False



def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Connected to MQTT Broker")
        client.subscribe(topic_sub) 
        client.subscribe(topic_sub2)  
        
    else:
        print(f"Connection failed. rc: {rc}")



def on_message(client, userdata, msg):
    global timeIntervals, switchState, is_adjusting 
    topic = msg.topic
    payload = msg.payload.decode()

    if topic == "iot/prefLux":
        # Deserialize JSON payload --> list of dictionarys
        intervals_data = json.loads(payload)
        timeIntervals.clear()
        # Recreate TimeInterval objects and add them to timeIntervals
        for interval_dict in intervals_data:
            interval = TimeInterval(start_hour=interval_dict['startHour'],
                                    start_minute=interval_dict['startMinute'],
                                    end_hour=interval_dict['endHour'],
                                    end_minute=interval_dict['endMinute'],
                                    value=interval_dict['value'])
            timeIntervals.append(interval)
        str_representation = [str(obj) for obj in timeIntervals] #Makes use of __str__ method in TimeInterval, to print specifik info on obj.
        print(f"Time intervals received and updated: {str_representation}")
        is_adjusting = False
        
    elif topic == "iot/switch":
        # Update switchState 
        switchState = payload.lower() == 'true'
        set_lamp_brightness(api_token, LAMP_ID, brightness_level)
        print(f"Switch state updated to: {switchState}")



client = mqtt.Client()

# Set callbacks
client.on_connect = on_connect
client.on_message = on_message
client.connect(mqtt_broker) # Broker address (mqtt_broker), port (default = 1883) and keepalive (default = 60s)
client.loop_start()



try:
    set_lamp_brightness(api_token, LAMP_ID, brightness_level)
    while True:
        setPrefLux()
        time_m.sleep(5)
except KeyboardInterrupt:
    client.disconnect()
    print("Disconnected from broker")
