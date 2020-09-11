/**
 * Copyright 2018 Ricoh Company, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.theta360.pluginapplication.task;

import android.os.AsyncTask;
import android.util.Log;

import com.theta360.pluginapplication.network.HttpConnector;

import org.json.JSONException;
import org.json.JSONObject;


public class PrepareBluetoothTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "PrepareBluetoothTask";

    private Callback mCallback;
    String setBluetoothPower = "";
    String setBluetoothRole = "";
    String orgBluetoothPower = "";
    String orgBluetoothRole = "";
    boolean initFlag = false;

    public PrepareBluetoothTask(Callback callback, String inBluetoothPower, String inbluetoothRole, boolean inInitFlag) {
        this.mCallback = callback;
        setBluetoothPower = inBluetoothPower;
        setBluetoothRole = inbluetoothRole;
        initFlag = inInitFlag;
    }

    @Override
    synchronized protected String doInBackground(Void... params) {
        HttpConnector camera = new HttpConnector("127.0.0.1:8080");
        String strResult = "";

        //現在の設定を取得
        String strJsonGetBluetoothSetting = "{\"name\": \"camera.getOptions\", \"parameters\": { \"optionNames\": [\"_bluetoothPower\", \"_bluetoothRole\"] } }";
        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonGetBluetoothSetting);
        try {
            JSONObject output = new JSONObject(strResult);
            JSONObject results = output.getJSONObject("results");
            JSONObject options = results.getJSONObject("options");
            orgBluetoothPower = options.getString("_bluetoothPower");
            orgBluetoothRole = options.getString("_bluetoothRole");
            Log.d(TAG,"[BT]: orgBluetoothPower=" + orgBluetoothPower + ", orgBluetoothRole=" + orgBluetoothRole +
                    " -> Power=" + setBluetoothPower + ", Role=" + setBluetoothRole + ", initFlag=" + String.valueOf(initFlag));

            if (initFlag) {
                String setOptions;
                if ( orgBluetoothRole.equals("Central") || orgBluetoothRole.equals("Central_Peripheral") ) {
                    setOptions = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{ \"_bluetoothRole\":\"Peripheral\" } } }";
                    strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, setOptions);
                    Log.d(TAG,"[BT]: set _bluetoothRole=Peripheral :result =" + strResult );
                }
                if (orgBluetoothPower.equals("OFF")) {
                    setOptions = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{ \"_bluetoothPower\":\"ON\" } } }";
                    strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, setOptions);
                    Log.d(TAG,"[BT]: set _bluetoothPower=ON :result =" + strResult );
                }
            } else {
                String setOptions;
                if ( setBluetoothRole.equals("Central") || setBluetoothRole.equals("Central_Peripheral") ) {
                    setOptions = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{ \"_bluetoothRole\":\"" + setBluetoothRole + "\" } } }";
                    strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, setOptions);
                    Log.d(TAG,"[BT]: set _bluetoothRole=" + setBluetoothRole + " :result =" + strResult );
                }
                if (setBluetoothPower.equals("OFF")) {
                    setOptions = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{ \"_bluetoothPower\":\"OFF\" } } }";
                    strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, setOptions);
                    Log.d(TAG,"[BT]: set _bluetoothPower=" + setBluetoothPower + " :result =" + strResult );
                }
            }

            strResult = "OK";
        } catch (JSONException e1) {
            e1.printStackTrace();
            strResult = "NG";
        }

        return strResult;
    }

    @Override
    protected void onPostExecute(String result) {
        mCallback.onPrepareBluetooth(orgBluetoothPower, orgBluetoothRole, initFlag);
    }

    public interface Callback {
        void onPrepareBluetooth(String saveBluetoothPower, String saveBluetoothRole, boolean initBulettooth);
    }
}

