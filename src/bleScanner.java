// (c) 2019 Nikas Konstantinos
// KeylessHome Android native Scanner

package com.nks.khforeground;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import com.megster.cordova.ble.central.*;
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
    private static final String TAG = "bleScanner";
    private CallbacksHelper mListener;
    private BluetoothAdapter bluetoothAdapter;

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
            System.out.println("BT_PLUGIN: BT ERROR");
            return false;
        }


        boolean validAction = true;

        if (action.equals("SCAN")) {
            scan();
        } else if (action.equals(STOP_SCAN)) {
            bluetoothAdapter.stopLeScan(this);
        }
        return true;
    }

    private void scan() {

        if (bluetoothAdapter.isDiscovering()) {
            System.out.println("Tried to start scan while already running.");
            return;
        }

        // clear non-connected cached peripherals
        for(Iterator<Map.Entry<String, Peripheral>> iterator = peripherals.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Peripheral> entry = iterator.next();
            Peripheral device = entry.getValue();
            boolean connecting = device.isConnecting();
            if (connecting){
                System.out.println("Not removing connecting device: " + device.getDevice().getAddress());
            }
            if(!entry.getValue().isConnected() && !connecting) {
                iterator.remove();
            }
        }
        UUID khuuid = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
        UUID[] uuids = new UUID[1];
        uuids[0]=khuuid;
        bluetoothAdapter.startLeScan(uuids, this);
        //bluetoothAdapter.startLeScan(this);

        if (timeInterval > 0) {
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            System.out.println("Stopping Scan");
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
                System.out.println(peripheral.asJSONObject());
            }else{
                System.out.println("bleScanner -> mListener is null, callBack attach faild.");
            }

        } else {
            Peripheral peripheral = peripherals.get(address);
            peripheral.update(rssi, scanRecord);

        }
    }


}
