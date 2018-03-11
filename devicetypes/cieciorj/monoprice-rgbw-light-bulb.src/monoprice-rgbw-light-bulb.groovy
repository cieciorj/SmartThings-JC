/**
 *  Monoprice RGBW Light Bulb
 *
 *  Copyright 2018 JOSEPH CIECIOR
 *
 *		Variation of 'RGBW Light' by SmartThingsCommunity:
 *			https://github.com/SmartThingsCommunity/SmartThingsPublic/tree/master/devicetypes/smartthings/rgbw-light.src
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 * 	Version:
 *    	- 1.0: Initial Release (3/10/2018)
 *
 * 	Known Issues:
 *   	- Factory reset not working
 *
 * 	Device Documentation:
 *		- https://downloads.monoprice.com/files/manuals/27482_Manual_170720.pdf
**/
metadata {
	definition (name: "Monoprice RGBW Light Bulb", namespace: "cieciorj", author: "Joseph Ciecior", ocfDeviceType: "oic.d.light") {
		capability "Actuator"
		capability "Color Control"
		capability "Color Temperature"
		capability "Configuration"
        capability "Switch"
		capability "Switch Level"
        capability "Refresh"
        capability "Health Check"

		command "colorTempPreset3000K"
        command "colorTempPreset5000K"
        command "colorTempPreset10000K"

		fingerprint mfr:"0208", prod:"0101", model:"0004"
	}


	simulator {

	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
                }
            tileAttribute ("device.color", key: "SECONDARY_CONTROL") {
            	attributeState "color", label: '${currentValue}'
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }
		}
        valueTile("rgbLabel", "device.color", width: 4, height: 1) {state "default", label: "RGB Selector"}
        valueTile("colorTempLabel", "device.colorTemperature", width: 2, height: 1) {state "default", label: "Color Temp"}
        controlTile("rgbSelector", "device.color", "color", height: 4, width: 4, inactiveLabel: false) {
            state "color", action: "color control.setColor"
		}
        controlTile("colorTempSelector", "device.colorTemperature", "slider", width: 2, height: 4, decoration: "flat",
        		inactiveLabel: false, range:"(3000..10000)") {
            state "colorTemp", action: "setColorTemperature", label: "Color Temp"
        }
        //ColorTemp Presets
       	standardTile("colorTempPreset3000KButton", "device.colorTemperature", width: 2, height: 1, decoration: "flat") {
       		state"colorTemp", action: "colorTempPreset3000K", label: "3,000K"
        }
        standardTile("colorTempPreset5000KButton", "device.colorTemperature", width: 2, height: 1, decoration: "flat") {
       		state"colorTemp", action: "colorTempPreset5000K", label: "5,000K"
        }
        standardTile("colorTempPreset10000KButton", "device.colorTemperature", width: 2, height: 1, decoration: "flat") {
       		state"colorTemp", action: "colorTempPreset10000K", label: "10,000K"
        }
	}

    preferences {
        input (
            name: "statusMemory",
            title: "STATUS MEMORY\n\n" +
            	   "The RGB Smart Bulb can be set to remember and restore its on/off status after a power outage.",
            type: "enum",
            defaultValue: "Remember Last State (Default)",
            options: ["Remember Last State (Default)", "Always Turn On", "Always Remain Off"],
            required: true
        )
        input (
            name: "loadStatusChange",
            title: "LOAD STATUS CHANGE NOTIFICATION\n\n" +
            	   "The RGB Smart Bulb can send notifications to an associated device (Group Lifeline) whenever the power load chanes.",
            type: "enum",
            defaultValue: "Send BASIC REPORT when power load changes (Default)",
            options: ["Notifications are disabled", "Send BASIC REPORT when power load changes (Default)"],
            required: true
        )
        input (
            name: "factoryReset",
            title: "RESET TO FACTORY DEFAULTS\n\n" +
            	   "Note: Resetting the RGB Smart Bulb will exclude it from the Z-Wave network",
            type: "bool",
            required: false
        )
    }
}

def ping() {
    log.debug "Health Check Ping -- Calling refresh()"
    refresh()
}

def refresh() {
	log.debug "refresh()"
	zwave.switchMultilevelV2.switchMultilevelGet().format()
}


//CONFIGURE(): Gets the configuration for all parameters and saves value to 'state.configVal[parameterNumber]'
def configure() {
	log.debug "configure()"

    def cmd = []

	cmd << zwave.configurationV2.configurationGet(parameterNumber: 21).format()
    cmd << zwave.configurationV2.configurationGet(parameterNumber: 24).format()
    cmd << zwave.configurationV2.configurationGet(parameterNumber: 255).format()

    delayBetween(cmd, 500)
}

//UPDATED(): Triggers when there is a change to configuration
def updated() {
	//Eliminate double-update
	if (state.lastUpdated && now() <= state.lastUpdated + 3000) return
    state.lastUpdated = now()

    log.debug "updated()"

    def cmd = []
    def statusMemoryVal = []
    def loadStatusChangeVal = []

    statusMemoryVal = [convertOptionstoInt(statusMemoryOptions, settings.statusMemory)]
    loadStatusChangeVal = [convertOptionstoInt(loadStatusChangeOptions, settings.loadStatusChange)]

    if (settings.statusMemory && statusMemoryVal != state.configVal21) {
        log.debug "New statusMemory Configuration: statusMemoryVal (${statusMemoryVal}) != state.configVal21 (${state.configVal21})"
        cmd << zwave.configurationV2.configurationSet(configurationValue: statusMemoryVal, parameterNumber: 21, size: 1)
        cmd << zwave.configurationV2.configurationGet(parameterNumber: 21)
        state.configVal21 = settings.statusMemory
    }

    if (settings.loadStatusChange && loadStatusChangeVal != state.configVal24) {
        log.debug "New loadStatusChangeVal Configuration: loadStatusChangeVal (${loadStatusChangeVal}) != state.configVal24 (${state.configVal24})"
        cmd << zwave.configurationV2.configurationSet(configurationValue: loadStatusChangeVal, parameterNumber: 24, size: 1)
        cmd << zwave.configurationV2.configurationGet(parameterNumber: 24)
        state.configVal24 = settings.loadStatusChangeVal
    }
    //THIS IS NOT WORKING -- Nothing is returning for parm 255
    if (settings.factoryReset == true) {
        log.debug "Conduct Factory Reset"
        cmd << zwave.configurationV2.configurationSet(scaledConfigurationValue: 1431655765, parameterNumber: 255, size: 4)
        cmd << zwave.configurationV2.configurationGet(parameterNumber: 255)
        settings.factoryReset = false
    }

	sendHubCommand(cmd.collect{ new physicalgraph.device.HubAction(it.format()) }, 500)
	log.debug "Set Check-in Interval for Health Check -- Every 30 Minutes (1800 Seconds)"
	sendEvent(name: "checkInterval", value: 1800, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
}

def convertOptionstoInt(options, settingName) {
    options?.find { "${settingName}" == it.name }?.value
}

def convertInttoOptions(options, settingValue) {
	options?.find { settingValue == it.value }?.name
}

def getStatusMemoryOptions() {
	[
    	[name: "Remember Last State (Default)", value: 0],
        [name: "Always Turn On", value: 1],
        [name: "Always Remain Off", value: 2]
    ]
}

def getLoadStatusChangeOptions() {
	[
    	[name: "Notifications are disabled", value: 0],
        [name: "Send BASIC REPORT when power load changes (Default)", value: 1]
    ]
}

def parse(description) {
	def result = null
    def cmd = zwave.parse(description, [0x20: 1, 0x26: 2, 0x70: 1, 0x33: 1])

    log.debug "Command: ${cmd}"

    result = zwaveEvent(cmd)

	log.debug("'$description' parsed to $result")

    result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv2.SwitchMultilevelReport cmd) {
	dimmerEvents(cmd)
}


//Stores the config value in a state variable
def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
    state."configVal${cmd.parameterNumber}" = cmd.configurationValue
}

private dimmerEvents(physicalgraph.zwave.Command cmd) {
    def value = (cmd.value ? "on" : "off")
    // Always turns on light when level is set.  May experiment with only sending explicit values as to set a level without turning on light.
	def result = [createEvent(name: "switch", value: value, descriptionText: "$device.displayName was turned $value")]
	if (cmd.value) {
		result << createEvent(name: "level", value: cmd.value, unit: "%")
	}

	result
}

//SWITCH COMMANDS
def on() {
	log.debug "ON"
    zwave.basicV1.basicSet(value: 0xFF).format()
}

def off() {
	log.debug "OFF"
    zwave.basicV1.basicSet(value: 0x00).format()
}

def setLevel(level) {
	log.debug "setLevel"
	setLevel(level, 1)
}

def setLevel(level, duration) {
	if(level > 99) level = 99
    log.debug "Level: ${level}, Duration: ${duration}"
	zwave.switchMultilevelV2.switchMultilevelSet(value: level, dimmingDuration: duration).format()
}


// COLOR COMMANDS
def setSaturation(percent) {
	log.debug "setSaturation($percent)"
	setColor(saturation: percent)
}

def setHue(value) {
	log.debug "setHue($value)"
	setColor(hue: value)
}

def setColor(value) {
	def result = []
	log.debug "setColor: ${value}"
	if (value.hex) {
		def c = value.hex.findAll(/[0-9a-fA-F]{2}/).collect { Integer.parseInt(it, 16) }
		result << zwave.switchColorV3.switchColorSet(red:c[0], green:c[1], blue:c[2], warmWhite:0, coldWhite:0).format()
	} else {
		def hue = value.hue ?: device.currentValue("hue")
		def saturation = value.saturation ?: device.currentValue("saturation")
		if(hue == null) hue = 13
		if(saturation == null) saturation = 13
		def rgb = huesatToRGB(hue, saturation)
		result << zwave.switchColorV3.switchColorSet(red: rgb[0], green: rgb[1], blue: rgb[2], warmWhite:0, coldWhite:0)
	}

	if(value.hue) sendEvent(name: "hue", value: value.hue)
	if(value.hex) sendEvent(name: "color", value: value.hex)
	if(value.switch) sendEvent(name: "switch", value: value.switch)
	if(value.saturation) sendEvent(name: "saturation", value: value.saturation)

	result
}

def setColorTemperature(kelvin) {
    //Assuming that 100% warmWhite = 3000K; 100% coldWhite = 1000K and scales linearly.  Not actually verified.

    def warmWhiteVal
    def coldWhiteVal

	sendEvent(name:"color", value:"#ffffff")

	//Converts the range 3000K - 10,000K to range 0 - 255
	coldWhiteVal = Math.round((((kelvin - 3000) * 255) / 7000))
    warmWhiteVal = Math.round(255 - coldWhiteVal)

    log.debug "setColorTemperature(${kelvin}) [warmWhite: ${warmWhiteVal} coldWhite: ${coldWhiteVal}]"
    zwave.switchColorV3.switchColorSet(warmWhite:warmWhiteVal, coldWhite:coldWhiteVal).format()
}

def rgbToHSV(red, green, blue) {
	float r = red / 255f
	float g = green / 255f
	float b = blue / 255f
	float max = [r, g, b].max()
	float delta = max - [r, g, b].min()
	def hue = 13
	def saturation = 0
	if (max && delta) {
		saturation = 100 * delta / max
		if (r == max) {
			hue = ((g - b) / delta) * 100 / 6
		} else if (g == max) {
			hue = (2 + (b - r) / delta) * 100 / 6
		} else {
			hue = (4 + (r - g) / delta) * 100 / 6
		}
	}
	[hue: hue, saturation: saturation, value: max * 100]
}

def huesatToRGB(float hue, float sat) {
	while(hue >= 100) hue -= 100
	int h = (int)(hue / 100 * 6)
	float f = hue / 100 * 6 - h
	int p = Math.round(255 * (1 - (sat / 100)))
	int q = Math.round(255 * (1 - (sat / 100) * f))
	int t = Math.round(255 * (1 - (sat / 100) * (1 - f)))
	switch (h) {
		case 0: return [255, t, p]
		case 1: return [q, 255, p]
		case 2: return [p, 255, t]
		case 3: return [p, q, 255]
		case 4: return [t, p, 255]
		case 5: return [255, p, q]
	}
}

//Tile - Color Temperature Presets
def colorTempPreset3000K() {
	setColorTemperature(3000)
}

def colorTempPreset5000K() {
	setColorTemperature(5000)
}
def colorTempPreset10000K() {
	setColorTemperature(10000)
}