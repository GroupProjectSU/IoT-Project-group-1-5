# MQTT
import time
import datetime
import paho.mqtt.client as mqtt
import paho.mqtt.publish as publish

# sensor
import board
import busio
import adafruit_tsl2591 	# lux sensor

# to round the lux value
from decimal import Decimal


# Initialize I2C bus and sensor.
i2c = busio.I2C(board.SCL, board.SDA)
lux_sensor = adafruit_tsl2591.TSL2591(i2c)	


# MQTT broker and topic to publish to
mqtt_broker = "test.mosquitto.org"	
topic_pub = "iot/sensor"	



# Things to doin diffrent stages of mqtt
def on_connect(client, userdata, flags, rc):
	if rc==0:
		print("Connected to broker. Code: "+str(rc))
	else:
		print("Connection failed. Code: " + str(rc))
		
def on_publish(client, userdata, mid):
    print("Published: " + str(mid))
	
def on_disconnect(client, userdata, rc):
	if rc != 0:
		print ("Unexpected disonnection. Code: ", str(rc))
	else:
		print("Disconnected. Code: " + str(rc))
	
def on_log(client, userdata, level, buf):		# Message is in buf
    print("MQTT Log: " + str(buf))

	

# getting lux value
def get_lux():
	lux = lux_sensor.lux
	lux_value = round(Decimal(lux), 2) 	# Rounds the lux value to 2 decimals
	print('Total light: {0} lux'.format(lux_value))
	return lux_value
	
	
client = mqtt.Client()	

# Set callbacks
client.on_connect = on_connect
client.on_disconnect = on_disconnect
client.on_publish = on_publish
client.on_log = on_log

# Connect to MQTT 
print("Attempting to connect to broker " + mqtt_broker)
client.connect(mqtt_broker)	# Broker address (mqtt_broker), port (default = 1883) and keepalive (default = 60s)
client.loop_start()


# Loop to publish lux
while True:
	lux_to_send = get_lux()	
	client.publish(topic_pub, str(lux_to_send))
	time.sleep(2.0)	# delay
