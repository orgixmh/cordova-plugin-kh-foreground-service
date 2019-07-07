package com.nks.khforeground;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.nks.kh.MainActivity;

public class NotificationsListener extends IntentService {

    private static final String TAG = "NotificationsListener";

    public NotificationsListener() {
        super("DisplayNotification");
    }
    private void startCordova(){

        Intent dialogIntent = new Intent(getBaseContext(), MainActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplication().startActivity(dialogIntent);
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Notification click event handled, waking up cordova and pushing container to js");
        SharedObject sharedObject = SharedObject.getInstance();
        sharedObject.deviceMac = intent.getStringExtra("unlockDeviceMac");
        sharedObject.deviceName = intent.getStringExtra("unlockDeviceName");
        startCordova();
    }
}