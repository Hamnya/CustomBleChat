package com.hamnya.homeblechat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.hamnya.homeblechat.bluetooth.BleManager;
import com.hamnya.homeblechat.utils.Logs;

import java.util.ArrayList;
import java.util.Set;

/*
* 권한 필요해 Need ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission to get scan results
* */
public class DeviceListActivity extends Activity {

    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    // Constants
    public static final long SCAN_PERIOD = 3*1000;	// Stops scanning after a pre-defined scan period.

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private ActivityHandler mActivityHandler;
    private BluetoothAdapter mBtAdapter;
    private BleManager mBleManager;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private ArrayList<BluetoothDevice> mDevices = new ArrayList<BluetoothDevice>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.activity_device_list);
        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);
        mActivityHandler = new ActivityHandler();

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
       mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.adapter_device_name);


        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);


        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // Get BLE Manager
        mBleManager = BleManager.getInstance(getApplicationContext(), null);
        mBleManager.setScanCallback(mLeScanCallback);

        doDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        if(D) Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // Empty cache
        mDevices.clear();

        // If we're already discovering, stop it
        if (mBleManager.getState() == BleManager.STATE_SCANNING) {
            mBleManager.scanLeDevice(false);
        }

        // Request discover from BluetoothAdapter
        mBleManager.scanLeDevice(true);

        // Stops scanning after a pre-defined scan period.
        mActivityHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopDiscovery();
            }
        }, SCAN_PERIOD);
    }

    /**
     * Stop device discover
     */
    private void stopDiscovery() {
        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(false);
        setTitle(R.string.bt_title);
        mBleManager.scanLeDevice(false);
        finish();
    }

    /**
     * Check if it's already cached
     */
    private boolean checkDuplicated(BluetoothDevice device) {
        for(BluetoothDevice dvc : mDevices) {
            if(device.getAddress().equalsIgnoreCase(dvc.getAddress())) {
                return true;
            }
        }
        return false;
    }



    /**
     * BLE scan callback
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Logs.d("# Scan device rssi is " + rssi);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                                if(!checkDuplicated(device)) {

                                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                                    mNewDevicesArrayAdapter.notifyDataSetChanged();
                                    mDevices.add(device);

                                    if(device.getName() != null && mDevices != null){
                                        if(device.getName().equalsIgnoreCase("CASO")) {
                                            goConn(device.getName(), device.getAddress());

                                        }
                                    }

                                }
                            }
                        }
                    });
                }
            };

    private void goConn(String deviceName, String deviceAddr){
        mBtAdapter.cancelDiscovery();
        // Get the device MAC address, which is the last 17 chars in the View
        String address = deviceAddr;
        Log.d(TAG, "User selected device : " + address);

        // Create the result Intent and include the MAC address
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

        // Set result and finish this Activity
        setResult(Activity.RESULT_OK, intent);
        finish();

    }

    public class ActivityHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {}

            super.handleMessage(msg);
        }
    }
}
