/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.gaiacontrol.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.material.tabs.TabLayout;
import com.qualcomm.qti.gaiacontrol.BuildConfig;
import com.qualcomm.qti.gaiacontrol.Consts;
import com.qualcomm.qti.gaiacontrol.R;
import com.qualcomm.qti.gaiacontrol.receivers.BREDRDiscoveryReceiver;
import com.qualcomm.qti.gaiacontrol.services.BluetoothService;
import com.qualcomm.qti.gaiacontrol.ui.adapters.DevicesListAdapter;
import com.qualcomm.qti.gaiacontrol.ui.adapters.DevicesListTabsAdapter;
import com.qualcomm.qti.gaiacontrol.ui.fragments.DevicesListFragment;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * This activity controls the scan of available BLE devices to connect with one of them. This activity also shows the
 * bonded devices to let the user choose the exact device he would like to use.
 * This activity is the start activity of the application and will initiate the connection with the device before
 * starting the next activity.
 */
public class DeviceDiscoveryActivity extends BluetoothActivity implements
        DevicesListFragment.DevicesListFragmentListener, BREDRDiscoveryReceiver.BREDRDiscoveryListener {

    /**
     * For debug mode, the tag to display for logs.
     */
    private final static String TAG = "DeviceDiscoveryActivity";
    /**
     * The {@link androidx.core.view.PagerAdapter} that will provide fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every loaded fragment in memory. If this becomes too
     * memory intensive, it may be best to switch to a {@link androidx.core.app.FragmentStatePagerAdapter}.
     */
    private DevicesListTabsAdapter mTabsAdapter;
    /**
     * The button the user uses to connect with a selected device over BLE.
     */
    private Button mBtConnectBLE;
    /**
     * The button the user uses to connect with a selected device over BR/EDR.
     */
    private Button mBtConnectBREDR;
    /**
     * The handler to use to postpone some actions.
     */
    private final Handler mHandler = new Handler();
    /**
     * To know if the scan is running.
     */
    private boolean mIsScanning = false;
    /**
     * The callback called when a device has been scanned by the LE scanner.
     */
    private final LeScanCallback mLeScanCallback = new LeScanCallback();
    /**
     * The adapter which should be informed about scanned devices.
     */
    private DevicesListAdapter mDevicesAdapter;
    /**
     * The runnable to trigger to stop the scan once the scanning time is finished.
     */
    private final Runnable mStopScanRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };
    /**
     * The broadcast receiver in order to get devices which had been discovered during scanning using
     * {@link BluetoothAdapter#startDiscovery() startDiscovery()}.
     */
    private final BREDRDiscoveryReceiver mDiscoveryReceiver = new BREDRDiscoveryReceiver(this);
    /**
     * The listener to be informed when the user changes the tab selection.
     */
    private final ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageSelected(int newPosition) {
            mTabsAdapter.onPageSelected(newPosition);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        public void onPageScrollStateChanged(int arg0) {
        }
    };


    // ------ OVERRIDE METHODS ----------------------------------------------------------------------------------------

    @SuppressWarnings("EmptyMethod") // does not need to be implemented at the moment
    @Override // ModelActivity
    public void onBluetoothEnabled() {
        super.onBluetoothEnabled();
        // start scan?
    }

    @Override // DevicesListFragmentListener
    public void startScan(DevicesListAdapter adapter) {
        mDevicesAdapter = adapter;
        scanDevices(true);
    }

    @Override // DevicesListFragmentListener
    public void onItemSelected(boolean selected) {
        enableButtons(selected);
    }

    @Override // DevicesListFragmentListener
    public void getBondedDevices(DevicesListAdapter adapter) {
        Set<BluetoothDevice> listDevices;

        if (mBtAdapter != null && mBtAdapter.isEnabled()) {
            listDevices = mBtAdapter.getBondedDevices();
        } else {
            listDevices = Collections.emptySet();
        }

        ArrayList<BluetoothDevice> listBLEDevices = new ArrayList<>();

        for (BluetoothDevice device : listDevices) {
            if (device.getType() == BluetoothDevice.DEVICE_TYPE_DUAL
                    || device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC
                    || device.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                listBLEDevices.add(device);
            }
        }
        adapter.setListDevices(listBLEDevices);
        mTabsAdapter.onScanFinished(DevicesListTabsAdapter.BONDED_LIST_TYPE);
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        if (mDevicesAdapter != null && device != null
                && device.getName() != null && device.getName().length() > 0) {
            mDevicesAdapter.add(device, 0);
        }
    }


    // ------ ACTIVITY METHODS ----------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        init();
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, 10086);
        // keep information
       /* SharedPreferences sharedPref = getSharedPreferences(Consts.PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(Consts.TRANSPORT_KEY, 0);
        editor.putString(Consts.BLUETOOTH_ADDRESS_KEY, "00:02:5C:22:22:22");
        editor.apply();
        startMainActivity();*/
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        int a2dp = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.A2DP);
        int headset = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
        int health = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEALTH);
        Log.d(TAG, headset+"onCreate: mymac"+a2dp+"health"+health+"device");

        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice("00:02:5c:22:22:11".toUpperCase());
            Log.d(TAG, "onCreate: mymac"+device.getAddress());
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    protected void onResumeFragments() {
        registerReceiver();
        super.onResumeFragments();
        enableButtons(mTabsAdapter.hasSelection());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBtAdapter != null && mBtAdapter.isEnabled()) {
            scanDevices(false);
        }
        unregisterReceiver();
    }


    // ------ PRIVATE METHODS -----------------------------------------------------------------------------------------

    /**
     *
     */
    private void enableButtons(boolean enabled) {
        if (!enabled || !mTabsAdapter.hasSelection()) {
            mBtConnectBLE.setEnabled(false);
            mBtConnectBREDR.setEnabled(false);
        } else {
            int type = mTabsAdapter.getSelectedDevice().getType();
            mBtConnectBREDR.setEnabled(type == BluetoothDevice.DEVICE_TYPE_CLASSIC
                    || type == BluetoothDevice.DEVICE_TYPE_DUAL);
            mBtConnectBLE.setEnabled(type == BluetoothDevice.DEVICE_TYPE_LE
                    || type == BluetoothDevice.DEVICE_TYPE_DUAL);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        scanDevices(true);
    }

    /**
     * <p>To start or stop the scan of available devices.</p>
     * <p>Do not use this method directly, prefer the
     * {@link DeviceDiscoveryActivity#startScan startScan} and {@link DeviceDiscoveryActivity#stopScan() stopScan} methods.</p>
     *
     * @param scan True to start the scan, false to stop it.
     */
    private void scanDevices(boolean scan) {
        assert mBtAdapter != null;

        if (scan && !mIsScanning) {
            mIsScanning = true;
            mHandler.postDelayed(mStopScanRunnable, Consts.SCANNING_TIME);
            //noinspection deprecation,UnusedAssignment
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "scanDevices: 无权限");
                return;
            }
            boolean isScanning = mBtAdapter.startLeScan(mLeScanCallback);
            //noinspection UnusedAssignment
            boolean isDiscovering = mBtAdapter.startDiscovery();
            if (DEBUG)
                Log.i(TAG, "Start scan of LE devices: " + isScanning + " - start discovery of BR/EDR devices: " +
                        isDiscovering);
        } else if (mIsScanning) {
            mIsScanning = false;
            mHandler.removeCallbacks(mStopScanRunnable);
            //noinspection deprecation
            mBtAdapter.stopLeScan(mLeScanCallback);
            //noinspection UnusedAssignment
            boolean isDiscovering = mBtAdapter.cancelDiscovery();
            if (DEBUG)
                Log.i(TAG, "Stop scan of LE devices - stop discovery of BR/EDR devices: " + isDiscovering);
        }
    }

    /**
     * <p>The method to call to stop the scan of available devices. This method will then call the
     * {@link DeviceDiscoveryActivity#scanDevices(boolean) scanDevices} method with "false" as the argument.</p>
     */
    private void stopScan() {
        mTabsAdapter.onScanFinished(DevicesListTabsAdapter.SCANNED_LIST_TYPE);
        scanDevices(false);
    }

    /**
     * To start the MainActivity.
     */
    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        this.startActivity(intent);
    }

    /**
     * <p>This method is used to initialize all view components which will be used in this activity.</p>
     */
    private void init() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME);
        Log.i(TAG, "Application version is " + BuildConfig.VERSION_NAME);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mTabsAdapter = new DevicesListTabsAdapter(getSupportFragmentManager(), this);
        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = findViewById(R.id.view_pager);
        mViewPager.setAdapter(mTabsAdapter);
        mViewPager.addOnPageChangeListener(pageChangeListener);

        // set up buttons
        mBtConnectBLE = findViewById(R.id.bt_connect_ble);
        mBtConnectBLE.setEnabled(false);
        mBtConnectBLE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConnectButtonClicked(BluetoothService.Transport.BLE);
            }
        });

        mBtConnectBREDR = findViewById(R.id.bt_connect_br_edr);
        mBtConnectBREDR.setEnabled(false);
        mBtConnectBREDR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConnectButtonClicked(BluetoothService.Transport.BR_EDR);
            }
        });

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    public static void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.setAccessible(true);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e("unpairDevice", e.toString());
        }
    }

    @SuppressLint("MissingPermission")
    public static void pairDevice(BluetoothDevice device) {
        device.createBond();
    }
    private void onConnectButtonClicked(@BluetoothService.Transport int transport) {
        stopScan();
        BluetoothDevice device = mTabsAdapter.getSelectedDevice();
        //unpairDevice(device);

        // keep information
        SharedPreferences sharedPref = getSharedPreferences(Consts.PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(Consts.TRANSPORT_KEY, transport);
        editor.putString(Consts.BLUETOOTH_ADDRESS_KEY, device.getAddress());
        editor.apply();

        Log.d(TAG, "onConnectButtonClicked: " + device.getAddress());
        Log.d(TAG, "onConnectButtonClicked: " + transport);
        startMainActivity();
    }

    /**
     * <p>To register the bond state receiver to be aware of any bond state change.</p>
     */
    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mDiscoveryReceiver, filter);
    }

    /**
     * <p>To unregister the bond state receiver when the application is stopped or we don't need it anymore.</p>
     */
    private void unregisterReceiver() {
        unregisterReceiver(mDiscoveryReceiver);
    }


    // ------ INNER CLASS ---------------------------------------------------------------------------------------------

    /**
     * Callback for scan results.
     */
    private class LeScanCallback implements BluetoothAdapter.LeScanCallback {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            if (mDevicesAdapter != null && device != null
                    && device.getName() != null && device.getName().length() > 0) {
                mDevicesAdapter.add(device, rssi);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        List<BluetoothDevice> connectedDevices = new ArrayList<>();

        for (BluetoothDevice device : bondedDevices) {
            Log.d(TAG, "onCreate: "+device.getAddress());
            int deviceBondState = device.getBondState();
            if (deviceBondState == BluetoothDevice.BOND_BONDED) {
                try {
                    Method isConnectedMethod = BluetoothDevice.class.getMethod("isConnected", (Class[]) null);
                    boolean isConnected = (boolean) isConnectedMethod.invoke(device, (Object[]) null);
                    if (isConnected) {
                        connectedDevices.add(device);
                    }
                    Log.d(TAG, "onCreate: "+device.getAddress()+" "+isConnected);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
