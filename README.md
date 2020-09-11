# THETA Plug-in sample that works with multiple "SwitchBot MeterTH"

より詳しい日本語の説明は[こちら](https://qiita.com/KA-2/items/66222ca713c7ab82bd32)。<br>
[Click here](https://qiita.com/KA-2/items/66222ca713c7ab82bd32) for a more detailed explanation in Japanese.

## Overview

- Acquires the temperature and humidity advertised by multiple SwitchBot thermo-hygrometers and displays them in a list on the WebUI (dynamic update)
- The humidity history of one thermo-hygrometer selected from the list is displayed as a graph on the WebUI (dynamic update).
- Take a picture triggered by a sudden rise in humidity of one thermo-hygrometer selected from the list (judgment is in THETA. WebUI is for visualization and is not essential)


This plugin is based on [this plugin](https://github.com/theta-skunkworks/theta-plugin-extendedpreview) that displays the live preview on the WebUI, and adds BLE processing.


## Description of the command added to the base project


### Commands

List of extended commands

|No|Command Name|
|---|---|
|1|camera.getSwitchBotList|
|2|camera.setTrigger|
|3|camera.getHumidityList|

<details><summary><b>Extended command details (click to open !!!)</b></summary><div>

<br>

### camera.getSwitchBotList

#### Overview

Get a list of devices with the latest scan results.

#### Parameters

None.

#### Results

|Name|Type|Description|
|---|---|---|
|mac|String|MAC address|
|type|String|"bot" or "TH"|
|bat|Integer| 0 to 100 |
|rssi|Integer| Radio field strength numerical value |
|temperature|Number| -127.9 to 127.9 Celsius |
|humidity|Integer| 0 to 99 % |

<br>

### camera.setTrigger

#### Overview

Specify the device to be the shooting trigger by MAC address.

#### Parameters

|Name|Type|Description|
|---|---|---|
|mac|String|MAC address|

#### Results

None.

<br>

### camera.getHumidityList

#### Overview

Acquire the humidity log of the specified device.

#### Parameters

|Name|Type|Description|
|---|---|---|
|mac|String|MAC address|

#### Results

|Name|Type|Description|
|---|---|---|
|time|Integer Array|Time[sec] difference when using the latest data base point.|
|humidity|Integer Array|0 to 99 %|


</br>
</br>

</div></details>


## Development Environment

### Camera
* RICOH THETA V Firmware ver.3.40.1 and above
* RICOH THETA Z1 Firmware ver.1.50.1 and above

### SDK/Library
* RICOH THETA Plug-in SDK ver.2.1.0

### Development Software
* Android Studio ver.3.5.3
* gradle ver.5.1.1


## License

```
Copyright 2018 Ricoh Company, Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contact
![Contact](img/contact.png)

