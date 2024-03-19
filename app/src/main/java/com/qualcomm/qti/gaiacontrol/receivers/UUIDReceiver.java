/**************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.gaiacontrol.receivers;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.os.Parcelable;

/**
 * <p>This class allows reception of information from the system about discovery of UUID for a BluetoothDevice.</p>
 * <p>This receiver should be used with the following intent filter:
 * {@link BluetoothDevice#ACTION_UUID ACTION_UUID}.</p>
 */
public class UUIDReceiver extends BroadcastReceiver {
    /**
     * The listener to dispatch events from this receiver.
     */
    private final UUIDListener mListener;

    private final BluetoothDevice mDevice;

    /**
     * <p>The constructor of this class.</p>
     *
     * @param listener The listener to inform of broadcast events from this receiver.
     *
     * @param device the device this receiver should listen UUIDs for.
     */
    public UUIDReceiver(UUIDListener listener, BluetoothDevice device) {
        this.mListener = listener;
        this.mDevice = device;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(BluetoothDevice.ACTION_UUID)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Parcelable[] parcels = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                // note: EXTRA_UUID gives an array of Parcelable,
            if (device != null && mDevice!= null && mDevice.equals(device) && parcels != null) {
                ParcelUuid[] uuids = new ParcelUuid[parcels.length];
                for (int i=0; i<parcels.length; i++) {
                    uuids[i] = (ParcelUuid) parcels[i];
                }
                mListener.onUUIDFound(device, uuids);
            }
        }
    }

    /**
     * <p>The listener for the {@link UUIDReceiver UUIDReceiver} receiver.</p>
     */
    public interface UUIDListener {
        /**
         * <p>The method to dispatch a found UUID to a listener of this receiver.</p>
         *
         * @param parcels
         *          The uuid which had been found for the device set up when creating the UUIDReceiver.

         * @param device
         *              The device for which the UUID has been found.
         */
        void onUUIDFound(BluetoothDevice device, ParcelUuid[] parcels);
    }

}
