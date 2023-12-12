api_token = "c065424eafcbd07ffdc28594fe30f2d7519bb72276c2de9699201a719d710ef3"  # LIFX API token used as authorization to access an accounts LIFX bulbs

import requests  # makes it possible to send data over the internet
import json      # makes it possible to handle data in JSON format 


def set_lifx_brightness(token, selector, brightness, powerValue): # token parameter to access an accounts LIFX bulbs, selector parameter to specify which bulb to control, brightness paramater to specify the desired brightnesslevel
    # The method constructs a request to send data over to the LIFX system. 
   
    #this url variable tells the script where to send the request with a placeholder "{selector}" for specifying which bulbs to control
    url = f"https://api.lifx.com/v1/lights/{selector}/state"
    
    #Both papyload and headers are used as what data to send in the request to the LIFX system
    payload = {
        "power": powerValue,  # ...
        "brightness": brightness # instruction to sets the brightnesslevel to the "brightness" variable betwen 0-1
    }
    headers = {
        "Authorization": f"Bearer {token}", #sends the API token to gain access
        "Content-Type": "application/json"  #this tells the LIFX API that the data that is sent is ssent in JSON format
    }

    response = requests.put(url, json=payload, headers=headers) #request.put sends the messeage to the LIFX API and update their server 
    return response.json()  # When the message is sent, the LIFX API will send back a respons, this method returns/takes and convert it to a json format. 
  

brightness_level = 0.2  # Set brightness to 20%


#if brightness_level == 0:
 #   powerValue = "off"
#else:
 #   powerValue = "on"



# Calling the function to set brightness
responseData = set_lifx_brightness(api_token, "all", brightness_level, powerValue) #this executes the set_lifx_brightness to constructs and send a request over to the LIFX system with desiered .   

#print response from the LIFX API
print(json.dumps(responseData, indent=4, sort_keys=True)) #json.dump converts objects to strings (responseData), indent=4 for easier reading, sort_keys=True to sort the respons data
