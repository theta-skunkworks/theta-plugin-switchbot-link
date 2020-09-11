/**
 * Copyright 2018 Ricoh Company, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theta360.switchbotlink;


import android.util.Log;

public class SwitchBot  {
    private static final String TAG = "SwitchBot";

    public static final int DEVICE_TYPE_BOT = 0x48;
    public static final int DEVICE_TYPE_TH_ADD = 0x74;
    public static final int DEVICE_TYPE_TH_NOR = 0x54;

    public static final byte MASK_B6toB0 = (byte)0x7F;
    public static final byte MASK_B7toB6 = (byte)0xC0;
    public static final byte MASK_B5toB4 = (byte)0x30;
    public static final byte MASK_B3toB0 = (byte)0x0F;

    public static final byte MASK_B7 = (byte)0x80;
    public static final byte MASK_B3 = (byte)0x08;
    public static final byte MASK_B2 = (byte)0x04;
    public static final byte MASK_B1 = (byte)0x02;
    public static final byte MASK_B0 = (byte)0x01;

    //-- Advertise Info --
    String macAddress ;
    int rssi;
    long timestampNanos ;

    //-- mScanRecord  --
    byte[] serviceData;        // parce後データだけあればいいとはおもうが念のため保持

    //-- Service data after parse --
    int deviceType ;           // SwitchBot Bot (WoHand) 0x48
                                 // SwitchBot MeterTH (WoSensorTH) : 0x74 Add Mode
                                 // SwitchBot MeterTH (WoSensorTH) : 0x74 Normal Mode
    boolean groupD;
    boolean groupC;
    boolean groupB;
    boolean groupA;
    int bat;                      // 0-100 [%]

    int temperatureAlertStatus; // 0: no alert,
                                    // 1: low temp alert (temperature lower than the low limit)
                                    // 2: high temp alert (temperature higher than the high limit)
                                    // 3: temp alert (temperature higher than than the low limit and lower than the high limit)
    int humidityAlertStatus;    // 0: no alert,
                                   // 1: low humidity alert (humidity lower than the low limit)
                                   // 2: high humidity alert (humidity higher than the high limit)
                                   // 3: temp humidity (humidity higher than than the low limit and lower than the high limit)
    boolean temperatureScale;   //false:Celsius scale (°C), true:Fahrenheit scale (°F)
    double temperature;          // ± 127.9 [°C]
    int humidity;                 // 0-99 [%]


    public SwitchBot(String inMac, int inRssi, long inTimestampNanos,  byte[] inServiceData){
        macAddress = inMac;
        rssi = inRssi;
        timestampNanos = inTimestampNanos;
        serviceData = inServiceData;

        deviceType = (int)(inServiceData[0]&MASK_B6toB0);

        groupD = ( (inServiceData[1]&MASK_B3) == MASK_B3 ) ? true : false ;
        groupC = ( (inServiceData[1]&MASK_B2) == MASK_B2 ) ? true : false ;
        groupB = ( (inServiceData[1]&MASK_B1) == MASK_B1 ) ? true : false ;
        groupA = ( (inServiceData[1]&MASK_B0) == MASK_B0 ) ? true : false ;

        bat = (int)( inServiceData[2]&MASK_B6toB0 );

        if ( (deviceType == DEVICE_TYPE_TH_ADD) || (deviceType == DEVICE_TYPE_TH_NOR) ) {
            temperatureAlertStatus = (int) ( (inServiceData[3]&MASK_B7toB6) >> 6 );
            humidityAlertStatus = (int) ( (inServiceData[3]&MASK_B5toB4) >> 4 );

            temperature = (double)( inServiceData[4]&MASK_B6toB0 ) + ((double)(inServiceData[3]&MASK_B3toB0))/10.0 ;
            double signOfTemp = ( (inServiceData[4]&MASK_B7) == MASK_B7 ) ? 1.0 : -1.0 ;
            temperature = signOfTemp * temperature ;

            temperatureScale = ((inServiceData[5]&MASK_B7) == MASK_B7) ? true : false ;
            humidity = (int)(inServiceData[5]&MASK_B6toB0);
        }

        //debug
        String formatStrByte0to2 = String.format("deviceType=0x%02X, Group D=%b,C=%b,B=%b,A=%b, Bat=%d, ", deviceType, groupD, groupC, groupB, groupA, bat );
        String formatStrByte3to5 = String.format("TempAlart=%d, HumiAlart=%d, temperature=%.1f, tempScale=%b, humidity=%d", temperatureAlertStatus, humidityAlertStatus, temperature, temperatureScale, humidity);
        Log.d(TAG, "MAC=" + macAddress + ", rssi=" + String.valueOf(rssi) + ", parsed:" + formatStrByte0to2 + formatStrByte3to5);

        return;
    }



}
