package com.nks.khforeground;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;

import com.nks.kh.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Notifications {

    private static String NOTIFICATION_BODY_MESSAGE;
    private static String NOTIFICATION_FOREGROUND_MESSAGE;
    private Context mContext;
    private JSONArray notsPool = new JSONArray();
    public Notification foregroundNotification = new Notification();

    public static final int FOREGROUND_NOTIFICATION = 0;
    public static final int DEVICE_NOTIFICATION = 1;
    public static final int ERROR_NOTIFICATION = 9;

    NotificationChannel channel;
    NotificationManager manager;


    public Notification init(Context mContext, JSONObject notificationData) {

        try{
            this.NOTIFICATION_BODY_MESSAGE          = notificationData.getString("UNLOCK_MESSAGE");
            this.NOTIFICATION_FOREGROUND_MESSAGE    = notificationData.getString("FOREGROUND_MESSAGE");
        }
        catch (JSONException e){
            e.printStackTrace();
            this.NOTIFICATION_BODY_MESSAGE = "Body test";
            this.NOTIFICATION_FOREGROUND_MESSAGE = "Service test";
        }
        //Save context for later usage
        this.mContext = mContext;

        // Delete notification channel if it already exists
        manager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);


        // Get notification channel importance
        int importance = NotificationManager.IMPORTANCE_HIGH;


        // Create notification channel
        if (Build.VERSION.SDK_INT >= 26) {
            manager.deleteNotificationChannel("foreground.service.channel");
            channel = new NotificationChannel("foreground.service.channel", "KH Background Services", importance);
            channel.setDescription("Enables background connectivity with KH accessories.");
            mContext.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }


        //Create foreground notification
        Notification serviceNotification = createNotification(notificationData);

        //Save foreground notification data for later use
        this.foregroundNotification = serviceNotification;

        //Returning notification back to the service controller
        return serviceNotification;
    }



    private void cleanNotificationIDs(int startID){

        System.out.println("Notifications from id "+ startID + " removed");
        while(startID < 10){
            //if (notificationExist(startID)){
                removeNotification(startID);
            //}else{
            //    break;
            //}
            startID++;
        }

    }

    public void sendNotifications(JSONArray scanPool){
        System.out.println("ScanPool Length:"+scanPool.length());
        if (scanPool.length()>=1) {

            int f = 1;
            for (int n=0; n < scanPool.length(); n++) {
                try {
                    JSONObject scanObject = scanPool.getJSONObject(n);
                    f = n+1;
                    scanObject.put("ID", f);
                    scanObject.put("TYPE", DEVICE_NOTIFICATION);
                    Notification deviceNotification = createNotification(scanObject);
                    if (deviceNotification!= null) {
                        manager.notify(f, deviceNotification);
                    }else{
                        System.out.println("---> WARING NULL NOTIFICATION" );
                    }
                    scanPool.put(n, scanObject);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            //Clean stucked nots
            cleanNotificationIDs(f+1);
        }else{
            manager.notify(1 , foregroundNotification);
            cleanNotificationIDs(2);
        }

    }


    public void removeNotification(int notificationID){
        manager.cancel(notificationID);


    }


    private boolean notificationExist(int notificationID){

        boolean isExist = false;
        if (Build.VERSION.SDK_INT >= 26) {
            StatusBarNotification[] notifications = manager.getActiveNotifications();
            for (StatusBarNotification notification : notifications) {

                if (notification.getId() == notificationID) {
                    isExist = true;
                } else {
                    isExist = false;
                }
            }
        }

        return isExist;
    }


    public Notification createNotification(JSONObject notificationData){
        Notification notification = null;
        try{

            //Initialize notification needed variables
            int notType = notificationData.getInt("TYPE");
            int notificationID = 1;
            String notMac;
            String notName;
            String notBody;

            // Make intend
            Intent notificationIntent = new Intent(mContext, NotificationsListener.class);
            int pendingIndentAction = PendingIntent.FLAG_UPDATE_CURRENT;

            if (notType==FOREGROUND_NOTIFICATION){

                notBody = notificationData.getString("NAME");
                notName = NOTIFICATION_FOREGROUND_MESSAGE;
                notificationID = 1;

                notificationIntent.putExtra("type", FOREGROUND_NOTIFICATION);

                pendingIndentAction = PendingIntent.FLAG_UPDATE_CURRENT;


            }else{

                notBody = NOTIFICATION_BODY_MESSAGE;
                notName = notificationData.getString("NAME");
                notificationID = notificationData.getInt("ID");
                notMac = notificationData.getString("MAC");

                //Notifcation Extras
                notificationIntent.putExtra("unlockDeviceMac", notMac);
                notificationIntent.putExtra("unlockDeviceName", notName);
                notificationIntent.putExtra("type", DEVICE_NOTIFICATION);


            }

            notificationData.put("ID",notificationID );

            //Creating pending intend
            PendingIntent pendingIntent = PendingIntent.getService(mContext, notificationID, notificationIntent, pendingIndentAction);

            // Get notification icon
            int notificationIcon = mContext.getResources().getIdentifier((String) "icon", "drawable", mContext.getPackageName());


            if (Build.VERSION.SDK_INT >= 26) {
                // Make notification (Android >=8)
                notification = new Notification.Builder(mContext, "foreground.service.channel")
                        .setContentTitle(notName)
                        .setContentText(notBody)
                        .setOngoing(true)
                        .setContentIntent(pendingIntent)
                        .setSmallIcon(R.drawable.kh_notification)
                        .build();
            }else{

                NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.kh_notification)
                        .setContentTitle(notName)
                        .setOngoing(true)
                        .setContentIntent(pendingIntent)
                        .setContentText(notBody)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                notification = builder.build();
            }
            notification.flags = Notification.FLAG_ONLY_ALERT_ONCE;

            return notification;
        }
        catch (JSONException e){
            e.printStackTrace();
            return null;
        }

    }
}