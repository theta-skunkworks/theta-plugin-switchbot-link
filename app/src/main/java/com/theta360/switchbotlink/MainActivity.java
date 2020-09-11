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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.util.Log;
import android.os.Bundle;
import android.view.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;
import com.theta360.pluginapplication.task.TakePictureTask;
import com.theta360.pluginapplication.task.TakePictureTask.Callback;
import com.theta360.pluginapplication.task.GetLiveViewTask;
import com.theta360.pluginapplication.task.MjisTimeOutTask;
import com.theta360.pluginapplication.task.PrepareBluetoothTask;
import com.theta360.pluginapplication.view.MJpegInputStream;
import com.theta360.pluginapplication.oled.Oled;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends PluginActivity {
    private static final String TAG = "SwitchbotLink";

    //Button Resorce
    private boolean onKeyDownModeButton = false;
    private boolean onKeyLongPressWlan = false;
    private boolean onKeyLongPressFn = false;

    //Preview Resorce
    private int previewFormatNo;
    GetLiveViewTask mGetLiveViewTask;
    private byte[]		latestLvFrame;

    //Preview Timeout Resorce
    private static final long FRAME_READ_TIMEOUT_MSEC  = 1000;
    MjisTimeOutTask mTimeOutTask;
    MJpegInputStream mjis;

    //WebServer Resorce
    private Context context;
    private WebServer webServer;

    //OLED Dislay Resorce
    Oled oledDisplay = null;
    private boolean mFinished;

    private static final int DISP_MODE_BIN = 0;
    private static final int DISP_MODE_EDGE = 1;
    private static final int DISP_MODE_MOTION = 2;
    int dispMode = DISP_MODE_BIN;

    private TakePictureTask.Callback mTakePictureTaskCallback = new Callback() {
        @Override
        public void onTakePicture(String fileUrl) {
            startPreview(mGetLiveViewTaskCallback, previewFormatNo);
        }
    };

    private String restoreBluetoothPoewr;
    private String restoreBluetoothRole;
    private PrepareBluetoothTask.Callback mPrepareBluetoothTaskCallback = new PrepareBluetoothTask.Callback() {
        @Override
        public void onPrepareBluetooth( String saveBluetoothPower, String saveBluetoothRole, boolean inInitFlag) {
            restoreBluetoothPoewr = saveBluetoothPower;
            restoreBluetoothRole = saveBluetoothRole;

            if (inInitFlag) {
                int initWaitMs = 0;
                if ( !saveBluetoothRole.equals("Peripheral") ) {
                    //Wait for the remote control scanner to finish
                    initWaitMs = 10000;
                } else {
                    if (saveBluetoothPower.equals("OFF")) {
                        //Wait for the _bluetoothPower turn ON
                        initWaitMs = 500;
                    }
                }
                if (initWaitMs!=0) {
                    Log.d(TAG,"Init wait " + String.valueOf(initWaitMs) + "msec");
                    try {
                        Thread.sleep(initWaitMs);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                //BLE Init
                Log.d(TAG,"#### BLE Init START ####");
                final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                bluetoothAdapter = bluetoothManager.getAdapter();
                mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();


                Log.d(TAG,"#### Set BLE Scan PARAM ####");
                ScanFilter scanFilter = new ScanFilter.Builder()
                        .setServiceUuid( ParcelUuid.fromString("cba20d00-224d-11e6-9fb8-0002a5d5c51b") )
                        .build();
                ArrayList scanFilterList = new ArrayList();
                scanFilterList.add(scanFilter);

                ScanSettings scanSettings = new ScanSettings.Builder()
                        //.setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();  //鈍くなるので監視には不向き
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(); //たくさんとれるが負荷高い。

                Log.d(TAG,"#### BLE Scan START ####");
                mBluetoothLeScanner.startScan(scanFilterList, scanSettings, mScanCallback);
            }
        }
    };

    // スキャン結果の格納場所を確保
    Map<String, ArrayList<SwitchBot>> scanLog = new HashMap<String, ArrayList<SwitchBot>>();
    private static final int SCANLOG_LIMIT_NUM = 60 ;

    //BLE Resorce
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeScanner mBluetoothLeScanner = null;

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG,"BLE:onScanResult() Callback");
            Log.d(TAG,"BLE:result = " + result.toString());

            byte[] bServiceData = result.getScanRecord().getServiceData( ParcelUuid.fromString("00000d00-0000-1000-8000-00805f9b34fb") ).clone();
            String hexString ="";
            for(int i=0; i< bServiceData.length; i++) {
                hexString += String.format("%02X", bServiceData[i]);
            }
            Log.d(TAG, "MAC:" + result.getDevice().toString() + ", RSSI:" + result.getRssi() + ", TimestampNano:" + result.getTimestampNanos() + ", ServiceData:0x" + hexString );

            //スキャン結果をログに保存 (すでにログがあれば追加、なければ新規)
            SwitchBot tempMember = new SwitchBot(result.getDevice().toString(), result.getRssi(), result.getTimestampNanos(), bServiceData);
            shootingJudgmentOnHumidity(tempMember.macAddress, tempMember.humidity);
            enterScanLog(tempMember);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG,"BLE:onBatchScanResults() Callback");
            for ( int i=0; i<results.size(); i++ ){
                Log.d(TAG,"BLE:result[" + String.valueOf(i) + "] = " + results.get(i).toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(TAG,"BLE:onScanFailed() Callback : errorCode=" + String.valueOf(errorCode));
        }
    };

    private void enterScanLog(SwitchBot inSwitchBotData) {
        ArrayList<SwitchBot> tempList = new ArrayList<SwitchBot>();
        if ( scanLog.containsKey(inSwitchBotData.macAddress) ) {
            Log.d(TAG, "From the 2nd Entry");

            //Limit the number of scan logs
            if ( scanLog.get(inSwitchBotData.macAddress).size() >= SCANLOG_LIMIT_NUM ) {
                scanLog.get(inSwitchBotData.macAddress).remove(0);
            }
            scanLog.get(inSwitchBotData.macAddress).add(inSwitchBotData);
        } else {
            Log.d(TAG, "1st Entry");

            scanLog.put(inSwitchBotData.macAddress, tempList);
            scanLog.get(inSwitchBotData.macAddress).add(inSwitchBotData);
        }
    }

    private String triggerMacAddr = "";
    private int HUMIDITY_THRESH = 5 ;
    private int HUMIDITY_JUDE_NUM = 1 ;
    private void shootingJudgmentOnHumidity (String inMacAddr, int newHumidity) {

        if ( scanLog.containsKey(inMacAddr) && (inMacAddr.equals(triggerMacAddr)) ) {

            if (scanLog.get(inMacAddr).size() > HUMIDITY_JUDE_NUM) {

                int averageHumidity=0;
                for (int i=0; i<HUMIDITY_JUDE_NUM; i++) {
                    averageHumidity += scanLog.get(inMacAddr).get( (scanLog.get(inMacAddr).size()-1)-i ).humidity;
                }
                averageHumidity = averageHumidity/HUMIDITY_JUDE_NUM;
                Log.d(TAG, "MAC:" + inMacAddr +", averageHumidity=" + String.valueOf(averageHumidity) + ", curHumidity=" + String.valueOf(newHumidity));

                if ( (newHumidity - averageHumidity) >= HUMIDITY_THRESH  ) {
                    Log.d(TAG, "shooting!");
                    stopPreview();
                    new TakePictureTask(mTakePictureTaskCallback).execute();
                }

            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set enable to close by pluginlibrary, If you set false, please call close() after finishing your end processing.
        setAutoClose(true);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //init OLED
        oledDisplay = new Oled(getApplicationContext());
        oledDisplay.brightness(100);
        oledDisplay.clear(oledDisplay.black);
        oledDisplay.draw();

        // Bluetooth周りを整え初期化する
        new PrepareBluetoothTask( mPrepareBluetoothTaskCallback, "ON", "Peripheral" , true).execute();

        // Set a callback when a button operation event is acquired.
        setKeyCallback(new KeyCallback() {
            @Override
            public void onKeyDown(int keyCode, KeyEvent event) {
                switch (keyCode) {
                    case KeyReceiver.KEYCODE_CAMERA :
                        stopPreview();
                        new TakePictureTask(mTakePictureTaskCallback).execute();
                        break;
                    case KeyReceiver.KEYCODE_MEDIA_RECORD :
                        // Disable onKeyUp of startup operation.
                        onKeyDownModeButton = true;
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onKeyUp(int keyCode, KeyEvent event) {

                switch (keyCode) {
                    case KeyReceiver.KEYCODE_WLAN_ON_OFF :
                        if (onKeyLongPressWlan) {
                            onKeyLongPressWlan=false;
                        } else {

                            dispMode++;
                            if ( dispMode > DISP_MODE_MOTION ) {
                                dispMode= DISP_MODE_BIN;
                            }

                        }

                        break;
                    case KeyReceiver.KEYCODE_MEDIA_RECORD :
                        if (onKeyDownModeButton) {
                            if (mGetLiveViewTask!=null) {
                                stopPreview();
                            } else {
                                startPreview(mGetLiveViewTaskCallback, previewFormatNo);
                            }
                            onKeyDownModeButton = false;
                        }
                        break;
                    case KeyEvent.KEYCODE_FUNCTION :
                        if (onKeyLongPressFn) {
                            onKeyLongPressFn=false;
                        } else {

                            //NOP : KEYCODE_FUNCTION

                        }

                        break;
                    default:
                        break;
                }

            }

            @Override
            public void onKeyLongPress(int keyCode, KeyEvent event) {
                switch (keyCode) {
                    case KeyReceiver.KEYCODE_WLAN_ON_OFF:
                        onKeyLongPressWlan=true;

                        //NOP : KEYCODE_WLAN_ON_OFF

                        break;
                    case KeyEvent.KEYCODE_FUNCTION :
                        onKeyLongPressFn=true;

                        //NOP : KEYCODE_FUNCTION

                        break;
                    default:
                        break;
                }

            }
        });

        this.context = getApplicationContext();
        this.webServer = new WebServer(this.context, mWebServerCallback);
        try {
            this.webServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG,"**** onResume() START ****");

        if (isApConnected()) {

        }

        //trigger MAC address 設定復帰
        restoreSetting();

        //Start LivePreview
        previewFormatNo = GetLiveViewTask.FORMAT_NO_640_30FPS;
        startPreview(mGetLiveViewTaskCallback, previewFormatNo);

        //Start OLED thread
        mFinished = false;
        drawOledThread();

        Log.d(TAG,"**** onResume() END ****");
    }

    @Override
    protected void onPause() {
        // Do end processing
        //close();

        //trigger MAC address 設定保存
        saveSetting();

        // Bluetooth周りをプラグイン起動前の状態に戻す（初期化なし→フラグをfalseで呼ぶ）
        new PrepareBluetoothTask( mPrepareBluetoothTaskCallback, restoreBluetoothPoewr, restoreBluetoothRole , false).execute();

        //debug : scan log dump
        for ( String key : scanLog.keySet() ) {
            Log.d(TAG, "--- MAC:" + key + " ---");
            for (int i=0; i<scanLog.get(key).size(); i++) {
                if ( (scanLog.get(key).get(i).deviceType == SwitchBot.DEVICE_TYPE_TH_NOR) ||
                        (scanLog.get(key).get(i).deviceType == SwitchBot.DEVICE_TYPE_TH_ADD) ) {
                    Log.d(TAG, " [" + String.valueOf(i) + "]:" +
                            "devType: TH, " +
                            "time:" + String.valueOf( scanLog.get(key).get(i).timestampNanos ) + " nsec, " +
                            "bat:" + String.valueOf( scanLog.get(key).get(i).bat ) + " %, " +
                            "RSSI:" + String.valueOf( scanLog.get(key).get(i).rssi ) + " db, " +
                            "temperature:" + String.valueOf( scanLog.get(key).get(i).temperature ) + " C, " +
                            "humidity:" + String.valueOf( scanLog.get(key).get(i).humidity ) + " %, "
                    );
                } else if (scanLog.get(key).get(i).deviceType == SwitchBot.DEVICE_TYPE_BOT) {
                    Log.d(TAG, " [" + String.valueOf(i) + "]:" +
                            "devType: bot, " +
                            "time:" + String.valueOf( scanLog.get(key).get(i).timestampNanos ) + " nsec, " +
                            "bat:" + String.valueOf( scanLog.get(key).get(i).bat ) + " %, " +
                            "RSSI:" + String.valueOf( scanLog.get(key).get(i).rssi ) + " db, "
                    );
                } else {
                    Log.d(TAG, " [" + String.valueOf(i) + "]:" +
                            "devType: undefined, " +
                            "bat:" + String.valueOf( scanLog.get(key).get(i).bat ) + " %, " +
                            "RSSI:" + String.valueOf( scanLog.get(key).get(i).rssi ) + " db, "
                    );
                }
            }
        }

        Log.d(TAG,"#### BLE Scan STOP ####");
        mBluetoothLeScanner.stopScan(mScanCallback);

        //Stop Web server
        this.webServer.stop();

        //Stop LivePreview
        stopPreview();

        //Stop OLED thread
        mFinished = true;

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.webServer != null) {
            this.webServer.stop();
        }
    }

    private void startPreview(GetLiveViewTask.Callback callback, int formatNo){
        if (mGetLiveViewTask!=null) {
            stopPreview();

            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mGetLiveViewTask = new GetLiveViewTask(callback, formatNo);
        mGetLiveViewTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void stopPreview(){
        //At the intended stop, timeout monitoring also stops.
        if (mTimeOutTask!=null) {
            mTimeOutTask.cancel(false);
            mTimeOutTask=null;
        }

        if (mGetLiveViewTask!=null) {
            mGetLiveViewTask.cancel(false);
            mGetLiveViewTask = null;
        }
    }


    /**
     * GetLiveViewTask Callback.
     */
    private GetLiveViewTask.Callback mGetLiveViewTaskCallback = new GetLiveViewTask.Callback() {

        @Override
        public void onGetResorce(MJpegInputStream inMjis) {
            mjis = inMjis;
        }

        @Override
        public void onLivePreviewFrame(byte[] previewByteArray) {
            latestLvFrame = previewByteArray;

            //Update timeout monitor
            if (mTimeOutTask!=null) {
                mTimeOutTask.cancel(false);
                mTimeOutTask=null;
            }
            mTimeOutTask = new MjisTimeOutTask(mMjisTimeOutTaskCallback, FRAME_READ_TIMEOUT_MSEC);
            mTimeOutTask.execute();
        }

        @Override
        public void onCancelled(Boolean inTimeoutOccurred) {
            mGetLiveViewTask = null;
            latestLvFrame = null;

            if (inTimeoutOccurred) {
                startPreview(mGetLiveViewTaskCallback, previewFormatNo);
            }
        }

    };


    /**
     * MjisTimeOutTask Callback.
     */
    private MjisTimeOutTask.Callback mMjisTimeOutTaskCallback = new MjisTimeOutTask.Callback() {
        @Override
        public void onTimeoutExec(){
            if (mjis!=null) {
                try {
                    // Force an IOException to `mjis.readMJpegFrame()' in GetLiveViewTask()
                    mjis.close();
                } catch (IOException e) {
                    Log.d(TAG, "[timeout] mjis.close() IOException");
                    e.printStackTrace();
                }
                mjis=null;
            }
        }
    };

    /**
     * WebServer Callback.
     */
    private WebServer.Callback mWebServerCallback = new WebServer.Callback() {
        @Override
        public void execStartPreview(int format) {
            previewFormatNo = format;
            startPreview(mGetLiveViewTaskCallback, format);
        }

        @Override
        public void execStopPreview() {
            stopPreview();
        }

        @Override
        public boolean execGetPreviewStat() {
            if (mGetLiveViewTask==null) {
                return false;
            } else {
                return true;
            }
        }

        @Override
        public byte[] getLatestFrame() {
            return latestLvFrame;
        }

        @Override
        public String getSwitchBotList() {
            JSONObject resultObj = new JSONObject();
            JSONArray jsonArray = new JSONArray();

            for ( String key : scanLog.keySet() ) {
                JSONObject obj = new JSONObject();

                int lastPos = scanLog.get(key).size()-1;
                try {
                    obj.put("mac", key);

                    if ( (scanLog.get(key).get(lastPos).deviceType == SwitchBot.DEVICE_TYPE_TH_NOR) ||
                            (scanLog.get(key).get(lastPos).deviceType == SwitchBot.DEVICE_TYPE_TH_ADD) ) {
                        obj.put("type", "TH");
                        obj.put("bat", scanLog.get(key).get(lastPos).bat);
                        obj.put("rssi", scanLog.get(key).get(lastPos).rssi);
                        obj.put("temperature", scanLog.get(key).get(lastPos).temperature);
                        obj.put("humidity", scanLog.get(key).get(lastPos).humidity);
                        if ( key.equals(triggerMacAddr) ) {
                            obj.put("trigger", "true");
                        } else {
                            obj.put("trigger", "false");
                        }

                    } else if (scanLog.get(key).get(lastPos).deviceType == SwitchBot.DEVICE_TYPE_BOT) {
                        obj.put("type", "bot");
                        obj.put("bat", scanLog.get(key).get(lastPos).bat);
                        obj.put("rssi", scanLog.get(key).get(lastPos).rssi);
                        obj.put("temperature", "N/A");
                        obj.put("humidity", "N/A");
                        obj.put("trigger", "false");
                    } else {
                        obj.put("type", "undefined");
                        obj.put("bat", scanLog.get(key).get(lastPos).bat);
                        obj.put("rssi", scanLog.get(key).get(lastPos).rssi);
                        obj.put("temperature", "N/A");
                        obj.put("humidity", "N/A");
                        obj.put("trigger", "false");
                    }

                    jsonArray.put(obj);

                } catch (JSONException e) {
                    Log.d(TAG, "JSONException: obj.put()");
                    e.printStackTrace();
                }
            }

            try {
                resultObj.put("entries", jsonArray);
            } catch (JSONException e) {
                Log.d(TAG, "JSONException: resultObj.put()");
                e.printStackTrace();
            }

            //Log.d(TAG, "getSwitchBotList():" + resultObj.toString() );
            return resultObj.toString();
        }

        @Override
        public void setTriggerMac(String setMac) {
            triggerMacAddr=setMac;
        }

        @Override
        public String getHumidityList(String setMac) {
            JSONObject resultObj = new JSONObject();
            JSONArray jsonArrayTime = new JSONArray();
            JSONArray jsonArrayHumidity = new JSONArray();

            int logSize = scanLog.get(setMac).size();
            long lastTimeNano = scanLog.get(setMac).get(logSize-1).timestampNanos;
            for (int i=0; i<logSize; i++) {
                long diffTimeSec = (scanLog.get(setMac).get(i).timestampNanos - lastTimeNano)/1000000000;
                int humidity = scanLog.get(setMac).get(i).humidity;
                jsonArrayTime.put(diffTimeSec);
                jsonArrayHumidity.put(humidity);
            }

            try {
                resultObj.put("time", jsonArrayTime);
                resultObj.put("humidity", jsonArrayHumidity);
            } catch (JSONException e) {
                Log.d(TAG, "JSONException: resultObj.put()");
                e.printStackTrace();
            }

            //Log.d(TAG, "getHumidityList():" + resultObj.toString() );
            return resultObj.toString();
        }
    };

    //==============================================================
    // OLED Thread
    //==============================================================
    public void drawOledThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int outFps=0;
                long startTime = System.currentTimeMillis();
                Bitmap beforeBmp = null;

                while (mFinished == false) {

                    byte[] jpegFrame = latestLvFrame;
                    if ( jpegFrame != null ) {

                        //JPEG -> Bitmap
                        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegFrame, 0, jpegFrame.length);
                        //Resize Bitmap : width=128pix, height=64pix.
                        Bitmap smallBmp = Bitmap.createScaledBitmap(bitmap, 128,  64, true);

                        Bitmap resultBmp=null;
                        switch (dispMode) {
                            case DISP_MODE_EDGE :
                                resultBmp= edgeImage(smallBmp, 25);
                                break;
                            case DISP_MODE_MOTION :
                                if (beforeBmp!=null){
                                    resultBmp = motionImage(smallBmp, beforeBmp, 30);
                                } else {
                                    resultBmp = smallBmp;
                                }
                                beforeBmp =smallBmp;
                                break;
                            default:
                                resultBmp=smallBmp;
                                try {
                                    Thread.sleep(22);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                        }
                        //[Output to OLED]
                        // After creating the 128x24 image by cutting the top and bottom 20pix each, it is output.)
                        //-- The code in the comments below is slow. --
                        //oledDisplay.setBitmap(0,0, Oled.OLED_WIDTH, Oled.OLED_HEIGHT, 0, 20, 128, resultBmp);
                        //oledDisplay.draw();
                        //-- The following code is fast. That's because it doesn't process the brightness threshold. --
                        Bitmap cutBmp = Bitmap.createBitmap(resultBmp, 0, 20, 128, 24);
                        Intent imageIntent = new Intent("com.theta360.plugin.ACTION_OLED_IMAGE_SHOW");
                        imageIntent.putExtra("bitmap", cutBmp);
                        sendBroadcast(imageIntent);


                        outFps++;
                    } else {
                        try {
                            Thread.sleep(33);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    long curTime = System.currentTimeMillis();
                    long diffTime = curTime - startTime;
                    if (diffTime >= 1000 ) {
                        Log.d(TAG, "[OLED]" + String.valueOf(outFps) + "[fps]" );
                        startTime = curTime;
                        outFps =0;
                    }

                }
            }
        }).start();
    }

    private Bitmap motionImage(Bitmap inBitmap1, Bitmap inBitmap2, int threshold){
        int black = 0xFF000000 ;
        int white = 0xFFFFFFFF ;

        int imgWidth1 = inBitmap1.getWidth();
        int imgHeight1 = inBitmap1.getHeight();
        int imgWidth2 = inBitmap2.getWidth();
        int imgHeight2 = inBitmap2.getHeight();

        Bitmap moveBmp = Bitmap.createBitmap(imgWidth1, imgHeight1, Bitmap.Config.ARGB_8888 );

        if ( (imgWidth1==imgWidth2) && (imgHeight1==imgHeight2) ) {
            for (int width=0; width<imgWidth1; width++) {
                for (int height=0; height<imgHeight1; height++) {
                    int Y1 = getY(inBitmap1.getPixel(width, height ));
                    int Y2 = getY(inBitmap2.getPixel(width, height ));

                    int absDiffY = Math.abs( Y1 - Y2 );
                    if ( absDiffY >= threshold ) {
                        moveBmp.setPixel(width, height, white);
                    } else {
                        moveBmp.setPixel(width, height, black);
                    }
                }
            }

        } else {
            Log.d(TAG, "input size error!" );
        }

        return moveBmp;
    }

    private Bitmap edgeImage(Bitmap inBitmap, int threshold){
        int black = 0xFF000000 ;
        int white = 0xFFFFFFFF ;

        int imgWidth = inBitmap.getWidth();
        int imgHeight = inBitmap.getHeight();

        Bitmap diffBmp = Bitmap.createBitmap(imgWidth, imgHeight, Bitmap.Config.ARGB_8888 );

        for (int width=0; width<imgWidth; width++) {
            for (int height=0; height<imgHeight; height++) {

                int Y1 = getY(inBitmap.getPixel(width, height ));
                int diffWidth;
                if ( width == (imgWidth-1) ) {
                    diffWidth=0;
                } else {
                    diffWidth=width+1;
                }
                int Y2 = getY(inBitmap.getPixel(diffWidth, height ));

                int absDiffY = Math.abs( Y1 - Y2 );
                if ( absDiffY >= threshold ) {
                    diffBmp.setPixel(width, height, white);
                } else {
                    diffBmp.setPixel(width, height, black);
                }

            }
        }

        return diffBmp;
    }

    private int getY(int inBmpColor){
        // int color = (A & 0xff) << 24 | (B & 0xff) << 16 | (G & 0xff) << 8 | (R & 0xff);
        //Y =  0.299 x R + 0.587  x G + 0.114  x B
        double dY = 0.299*(inBmpColor&0x000000FF) + 0.587*((inBmpColor&0x0000FF00)>>8) + 0.114*((inBmpColor&0x00FF0000)>>16);
        int Y = (int)(dY+0.5);
        return Y;
    }

    //==============================================================
    // 設定保存・復帰
    //==============================================================
    private static final String SAVE_TRIGGER_MAC  = "triggerMac";
    SharedPreferences sharedPreferences;
    void restoreSetting() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        triggerMacAddr = sharedPreferences.getString(SAVE_TRIGGER_MAC, "");
    }
    void saveSetting() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SAVE_TRIGGER_MAC, triggerMacAddr);
        editor.commit();
    }
}

