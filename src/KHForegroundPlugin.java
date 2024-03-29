package com.nks.khforeground;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.Build;

public class KHForegroundPlugin extends CordovaPlugin {

    private static boolean isServiceRunningInForeground(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }

            }
        }
        return false;
    }

    @Override
    public boolean execute (final String action, final JSONArray args, final CallbackContext command) throws JSONException {

        Activity activity = cordova.getActivity();
        Intent intent = new Intent(activity, KHForegroundService.class);

        switch(action)
        {

            //START KH FOREGROUND SERVICE /////////////////////////////////////////////////
            case "start":
                // Tell the service we want to start it
                intent.setAction("START");

                // Pass the notification title/text/icon to the service
                intent.putExtra("foregroundMessage", args.getString(0))
                      .putExtra("foregroundDevices", args.getString(2))
                      .putExtra("unlockMessage", args.getString(1));


                // Finally start the service
                if (isServiceRunningInForeground(activity.getApplicationContext(),KHForegroundService.class)) {

                }else {
                    if (Build.VERSION.SDK_INT >= 26) {
                        activity.getApplicationContext().startForegroundService(intent);
                    } else {
                        activity.getApplicationContext().startService(intent);
                    }
                }
                // Return success to cordova
                command.success();
                break;

            //STOP KH FOREGROUND SERVICE //////////////////////////////////////////////////
            case "stop":
                // Tell the service we want to stop it

                intent.setAction("STOP");

                // Stop the service
                if (isServiceRunningInForeground(activity.getApplicationContext(),KHForegroundService.class)) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        activity.getApplicationContext().startForegroundService(intent);
                    } else {
                        activity.getApplicationContext().startService(intent);
                    }
                }


                // Return success to cordova
                command.success();
                break;

            //CHECK PENDING UNLOCKS ///////////////////////////////////////////////////////
            case "checkUnlock":
                //Assuming that kh resuming so we need first to stop service


                intent.setAction("STOP");

                if (isServiceRunningInForeground(activity.getApplicationContext(),KHForegroundService.class)) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        activity.getApplicationContext().startForegroundService(intent);
                    } else {
                        activity.getApplicationContext().startService(intent);
                    }
                }

                //A little messy way to share data between classes, processes and threads --> NEED FIX
                SharedObject sharedObject = SharedObject.getInstance();

                //Checking for pending unlock
                if (sharedObject.deviceName!=null){

                    //Creating cordova plugin response container
                    JSONObject pluginReturn = new JSONObject();
                    try {
                        //Filling response container with unlock keys
                        pluginReturn.put("deviceName", sharedObject.deviceName);
                        pluginReturn.put("deviceMac", sharedObject.deviceMac);

                        //Cleaning sharedObject
                        sharedObject.deviceName = null;
                        sharedObject.deviceMac = null;
                        //Finally returning data to js via cordova
                        command.success(pluginReturn);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        command.error("ForegroundService => WARNING, can't set sharedObject variable to JSON object. ERROR:"+e.toString());
                    }
                }else{
                    command.error("ForegroundService => No pending unlock has be found.");
                }
                break;
            default:
                command.error("ForegroundService => WARNING, no action specified");
        }
        return true;
    }
}
