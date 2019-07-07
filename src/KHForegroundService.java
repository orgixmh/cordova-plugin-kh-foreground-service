package com.nks.khforeground;

import android.content.Intent;
import android.content.Context;
import android.app.Service;
import android.os.IBinder;
import android.os.Bundle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;


public class KHForegroundService extends Service {

    Context mContext = this;
    Thread thread = null;
    JSONArray blePool = new JSONArray();

    bleScanner blePlugin = new bleScanner();
    Notifications notifications = new Notifications();
    private boolean looperStateEmu = false; //for emulation only
    private boolean looperState = false;
    private final static String TAG = KHForegroundService.class.getSimpleName();
    private final long timeInterval = 60000;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals("start")) {
            // Start the service
            startPluginForegroundService(intent.getExtras());

            if (thread==null) {
                initLooper();
            }
            startLooper();
        } else {
            // Stop the service
            stopLooper();
            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    private void startPluginForegroundService(Bundle extras) {
        Context context = getApplicationContext();
        try {
            JSONObject foregroundNotification = new JSONObject();
            foregroundNotification.put("NAME", " ");
            foregroundNotification.put("TYPE", notifications.FOREGROUND_NOTIFICATION);
            foregroundNotification.put("FOREGROUND_MESSAGE", extras.getString("foregroundMessage"));
            foregroundNotification.put("UNLOCK_MESSAGE", extras.getString("unlockMessage"));

            notifications.init(mContext, foregroundNotification);
            startForeground(1, notifications.foregroundNotification);
        }catch (JSONException e){
            System.out.println("SERVICE FAILED TO START, NOTIFICATION ERROR");
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
                   }

            } catch (JSONException e) {
                //error
            }
        }
        System.out.println("OLD POOL->\n"+blePool.toString());
        System.out.println("NEW POOL->\n"+newPool.toString());
        blePool = newPool;
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
                    blePoolItem.put("SCANNED", 3);
                    blePool.put(n,blePoolItem);
                    returnCode =  true;
                }

            } catch (JSONException e) {
                //error
            }


        }
        return returnCode;
    }
    private void addToBlePool(JSONObject scannedDevice){

        try {
            scannedDevice.put("SCANNED", 3);
            if (!deviceExist(scannedDevice)){
                scannedDevice.put("MAC", scannedDevice.getString("id"));
                scannedDevice.put("NAME", scannedDevice.getString("name"));
                scannedDevice.remove("id");
                scannedDevice.remove("name");
                blePool.put(scannedDevice);
                sync();
            }

        } catch (JSONException e) {
            System.out.println(e);
        }
    }
    private void initLooper(){
        blePlugin.timeInterval=timeInterval;
        Runnable runnable = new Runnable() {

            public void run() {
                while (looperState) {
                    sync();
                    try {
                        blePlugin.execute("SCAN", mContext, mListener);

                    } catch (Exception e) {
                        System.out.println(TAG+" -> Error while executing scan on bleScanner:" +e);
                    }

                    try {
                        Thread.sleep(timeInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread = new Thread(runnable);
        thread.start();

        Runnable runnable1 = new Runnable() {

            public void run() {
                while (looperStateEmu) {
                    Random rand = new Random();
                    int randomNum = rand.nextInt(8);
                    int i=0;
                    while (i<randomNum) {
                        addToBlePool(fakeDevice());
                        i++;
                    }
                    System.out.println("=> "+randomNum+" DEVICES ADDED!");
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        Thread thread1 = new Thread(runnable1);
        thread1.start();
    }
    public JSONObject fakeDevice(){
        Random rand = new Random();
        int randomNum = rand.nextInt(10000);
        randomNum++;

        try {
            JSONObject scannedDevice = new JSONObject();
            scannedDevice.put("NAME", "DEV_"+randomNum);
            scannedDevice.put("id", "00:"+randomNum);
            return scannedDevice;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
    private void stopLooper(){
        looperState = false;
    }

    private void startLooper(){
        looperState = true;
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
}
