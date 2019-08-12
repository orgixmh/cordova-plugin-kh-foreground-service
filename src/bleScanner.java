// (c) 2019 Nikas Konstantinos
// KeylessHome Android native Scanner

package com.nks.khforeground;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;

import com.megster.cordova.ble.central.*;

import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static android.content.Context.BLUETOOTH_SERVICE;


public class bleScanner implements BluetoothAdapter.LeScanCallback {
    public  long timeInterval = 7000;
    private Context context;

    private static final String STOP_SCAN = "stopScan";
    private CallbacksHelper mListener;
    private BluetoothAdapter bluetoothAdapter;
    private final static String TAG = bleScanner.class.getSimpleName();

    // key is the MAC Address
    private Map<String, Peripheral> peripherals = new LinkedHashMap<String, Peripheral>();


        public boolean execute(String action, Context context, CallbacksHelper mListener) {
        this.mListener = mListener;


        if (bluetoothAdapter == null) {
            this.context = context;
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (bluetoothAdapter == null) {
            LOG.d(TAG,"BT_PLUGIN: BT ERROR");
            return false;
        }


        boolean validAction = true;

        if (action.equals("SCAN")) {
            scan();
        } else  {
            bluetoothAdapter.stopLeScan(this);
        }
        return true;
    }

    private void scan() {

        if (bluetoothAdapter.isDiscovering()) {
            LOG.d(TAG,"Tried to start scan while already running.");
            return;
        }

        // clear non-connected cached peripherals
        for(Iterator<Map.Entry<String, Peripheral>> iterator = peripherals.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Peripheral> entry = iterator.next();
            Peripheral device = entry.getValue();
            boolean connecting = device.isConnecting();
            if (connecting){
                LOG.d(TAG,"Not removing connecting device: " + device.getDevice().getAddress());
            }
            if(!entry.getValue().isConnected() && !connecting) {
                iterator.remove();
            }
        }
        UUID khuuid = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
        UUID[] uuids = new UUID[1];
        uuids[0]=khuuid;
        if (Build.VERSION.SDK_INT >= 26) {
            bluetoothAdapter.startLeScan(uuids, this);
        }else{
            bluetoothAdapter.startLeScan(this);
        }


        if (timeInterval < 0) {
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            LOG.d(TAG,"Stopping Scan");
                            bleScanner.this.bluetoothAdapter.stopLeScan(bleScanner.this);
                        }
                    },
                    timeInterval - 1000
            );

        }

        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);

    }


    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

        String address = device.getAddress();
        boolean alreadyReported = peripherals.containsKey(address) && !peripherals.get(address).isUnscanned();
        if (!alreadyReported) {

            Peripheral peripheral = new Peripheral(device, rssi, scanRecord);
            peripherals.put(device.getAddress(), peripheral);

            if (mListener != null) {
                mListener.onCallback(peripheral.asJSONObject());
            }else{
                LOG.d(TAG,"bleScanner -> mListener is null, callBack attach faild.");
            }

        } else {
            Peripheral peripheral = peripherals.get(address);
            peripheral.update(rssi, scanRecord);

        }
    }


}

