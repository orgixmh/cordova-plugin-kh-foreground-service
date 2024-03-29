package com.nks.khforeground;

import android.content.Intent;
import android.content.Context;
import android.app.Service;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;



public class KHForegroundService extends Service {

    Context mContext = this;
    Thread thread = null;
    JSONArray blePool = new JSONArray();

    bleScanner blePlugin = new bleScanner();
    Notifications notifications = new Notifications();
    JSONArray devicesFilterArray = new JSONArray();

    private boolean looperState = false;
    private boolean looperInit = false;

    private final static String TAG = KHForegroundService.class.getSimpleName();
    private final long timeInterval = 8000;
    private final int SCANNED_EXPIRATION = 1; // How many scans needed



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            switch (action) {
                case "START":
                    Log.i(TAG, " --> START KH Service");
                    startPluginForegroundService(intent.getExtras());
                    break;
                case "STOP":
                    Log.i(TAG, " --> STOP KH Service");
                    stopForegroundService();
                    break;
            }
        }
        return START_STICKY;


    }
    private void stopForegroundService() {
        blePlugin.execute("STOP", mContext, mListener);
        stopLooper();


        // Stop foreground service and remove the notification.
        stopForeground(true);

        // Stop the foreground service.
        stopSelf();
        if (looperInit) {
            notifications.cleanNotificationIDs(1);
        }
    }

    private void startPluginForegroundService(Bundle extras) {
        Context context = getApplicationContext();
        try {
            String devicesFilter = extras.getString("foregroundDevices");

            devicesFilterArray = new JSONArray(devicesFilter);

            JSONObject foregroundNotification = new JSONObject();
            foregroundNotification.put("NAME", " ");
            foregroundNotification.put("TYPE", notifications.FOREGROUND_NOTIFICATION);
            foregroundNotification.put("FOREGROUND_MESSAGE", extras.getString("foregroundMessage"));
            foregroundNotification.put("UNLOCK_MESSAGE", extras.getString("unlockMessage"));

            notifications.init(mContext, foregroundNotification);
            startForeground(1, notifications.foregroundNotification);
            blePool = new JSONArray();
            if (!looperInit) {
                initLooper();
                looperInit = true;
            }
            startLooper();

        }catch (JSONException e){
            Log.e(TAG,"SERVICE FAILED TO START, NOTIFICATION ERROR");
            
        }
    }

    private void sync(){
        JSONArray newPool = new JSONArray();


        for (int n = 0; n < blePool.length(); n++) {
            try {
                JSONObject scannedDevice = blePool.getJSONObject(n);
                scannedDevice.put("SCANNED", scannedDevice.getInt("SCANNED")-1);
                if (scannedDevice.getInt("SCANNED")>0){
                    newPool.put(scannedDevice);
                    Log.d(TAG,"     Device #"+n+"  | MAC: "+scannedDevice.getString("MAC")+ " | NAME: "+scannedDevice.getString("NAME") + " | SCANNED: "+scannedDevice.getString("SCANNED")+" |"+ " RSSI: "+scannedDevice.getString("RSSI")+" |");
                }
            } catch (JSONException e) {
                //error
            }
        }

        blePool = sortJsonArray(newPool, "RSSI");
        notifications.sendNotifications(blePool);

    }
    private boolean deviceExist(JSONObject scannedDevice){

        boolean returnCode = false;
        for (int n = 0; n < blePool.length(); n++) {
            try {
                JSONObject blePoolItem = blePool.getJSONObject(n);
                String deviceMAC= scannedDevice.getString("id");
                String scannerMAC = blePoolItem.getString("MAC");
                if (deviceMAC.equals(scannerMAC)){
                    //UPDATING BLE DATABASE
                    blePoolItem.put("SCANNED", SCANNED_EXPIRATION + 1);
                    blePoolItem.put("RSSI", scannedDevice.getString("rssi"));
                    blePool.put(n,blePoolItem);
                    returnCode =  true;
                }

            } catch (JSONException e) {
                //error
            }


        }
        return returnCode;
    }

    private boolean isDeviceStored(JSONObject device){

        try {
            String deviceMac = device.getString("id");
            for (int n = 0; n < devicesFilterArray.length(); n++) {
                JSONObject storedDevice = devicesFilterArray.getJSONObject(n);
                String storedDeviceMac = storedDevice.getString("bssid");
                if (deviceMac.equals(storedDeviceMac)) {
                    Log.d(TAG,"Device " + device.getString("id") + " FOUND in filter list.");
                    return true;
                }

            }
            Log.d(TAG,"Device " + device.getString("id") + " NOT EXIST in filter list.");
        } catch (JSONException e) {
            Log.e(TAG,"Filter search FAILED with error: "+e);
            return false;
        }

        return false;
    }

    private void addToBlePool(JSONObject scannedDevice){

        try {

            scannedDevice.put("SCANNED", SCANNED_EXPIRATION + 1);

            if (!deviceExist(scannedDevice) && isDeviceStored(scannedDevice)){
                if (scannedDevice.has("name")) {
                    scannedDevice.put("MAC", scannedDevice.getString("id"));
                    scannedDevice.put("NAME", scannedDevice.getString("name"));
                    scannedDevice.put("RSSI", scannedDevice.getString("rssi"));
                    scannedDevice.remove("id");
                    scannedDevice.remove("name");
                    scannedDevice.remove("rssi");
                    scannedDevice.remove("advertising"); //Removing advertising data
                    blePool.put(scannedDevice);
                    notifications.sendNotifications(blePool);
                }
            }else{
                Log.d(TAG,"Device " + scannedDevice.getString("id") + " ignored.");
            }

        } catch (JSONException e) {
            Log.e(TAG,"ERROR PARSING SCANNED DEVICE:" + e);
        }
    }
    private void initLooper(){
        blePlugin.timeInterval=timeInterval;
        Runnable runnable = new Runnable() {

            public void run() {

                while (looperState) {

                    try {
                        blePlugin.execute("SCAN", mContext, mListener);

                    } catch (Exception e) {
                        Log.e(TAG, " -> Error while executing scan on bleScanner:" +e);
                    }

                    try {
                        Thread.sleep(timeInterval);
                        if (looperState) { //Possibility of asynchronus  run
                            blePlugin.execute("STOP", mContext, mListener);
                            sync();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        blePlugin.execute("STOP", mContext, mListener);
                    }
                }
            }
        };
        thread = new Thread(runnable);
        thread.start();
    }

    private void stopLooper(){
        looperState = false;
        Log.i(TAG," --> STOPPING LOOPER.");
    }

    private void startLooper(){
        looperState = true;
        Log.i(TAG," --> STARTING LOOPER.");
    }


    private CallbacksHelper mListener = new CallbacksHelper() {
        @Override
        public void onCallback(JSONObject scanResults) {
            addToBlePool(scanResults);
        }
    };



    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static JSONArray sortJsonArray(JSONArray array, String sortKey) {
        try {
            List<JSONObject> jsons = new ArrayList<JSONObject>();
            for (int i = 0; i < array.length(); i++) {
                jsons.add(array.getJSONObject(i));
            }
            Collections.sort(jsons, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject lhs, JSONObject rhs) {
                    try {
                        String lid = lhs.getString(sortKey);
                        String rid = rhs.getString(sortKey);
                        return lid.compareTo(rid);
                    }catch (JSONException e){
                        Log.e(TAG,"ERROR _2_ JSON SORT");
                        return 0;
                    }
                }
            });
            return new JSONArray(jsons);
        } catch (JSONException e) {
            Log.e(TAG,"ERROR _1_ JSON SORT");
            return new JSONArray();
        }
    }
}
