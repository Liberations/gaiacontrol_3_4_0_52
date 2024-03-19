package com.qualcomm.qti.gaiacontrol.activities;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.qualcomm.qti.gaiacontrol.Consts;
import com.qualcomm.qti.gaiacontrol.R;
import com.qualcomm.qti.gaiacontrol.Utils;
import com.qualcomm.qti.gaiacontrol.gaia.MainGaiaManager;
import com.qualcomm.qti.gaiacontrol.services.BluetoothService;
import com.qualcomm.qti.gaiacontrol.services.GAIAGATTBLEService;
import com.qualcomm.qti.libraries.gaia.GAIA;
import com.qualcomm.qti.libraries.vmupgrade.UpgradeError;
import com.qualcomm.qti.libraries.vmupgrade.UpgradeManager;
import com.qualcomm.qti.libraries.vmupgrade.codes.ResumePoints;
import com.qualcomm.qti.libraries.vmupgrade.codes.ReturnCodes;

import java.io.File;

public class MyUpgradeActivity extends ServiceActivity {
    private final static String START_TIME_KEY = "START_TIME";
    private final String TAG = "wenTest";
    private File mFile;
    /**
     * To know the state of the RWCP mode: enabled or disabled.
     */
    private boolean mIsRWCPEnabled = false;
    /**
     * The start time of the upgrade. If there is no upgrade going in, this field has no meaning.
     */
    private long mStartTime = 0;


    @Override // Activity
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPref = getSharedPreferences(Consts.PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(Consts.TRANSPORT_KEY, BluetoothService.Transport.BR_EDR);
        editor.putString(Consts.BLUETOOTH_ADDRESS_KEY, "00:02:5C:22:22:22");
        editor.apply();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // to keep screen on during update


        String path = getCacheDir().getAbsolutePath() + "/HHZ_M2_OTA_V12_20230308_DEMO.bin";
        mFile = new File(path);
        Log.d(TAG, "onCreate: " + path);
        findViewById(R.id.tvCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abortUpgrade();
            }
        });

    }


    public void abortUpgrade() {
        if (mService != null) {
            mService.abortUpgrade();
        }
    }

    private void startUpgrade(File file) {
        if (file != null) {
            mStartTime = 0;
            mService.startUpgrade(file);
            Log.d(TAG, "startUpgrade: 继续升级");
        } else {
            Log.d(TAG, "startUpgrade: 文件为空");
        }
    }

    @Override // Activity
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (mStartTime != 0) {
            savedInstanceState.putLong(START_TIME_KEY, mStartTime);
        }
    }

    @Override // Activity
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state saved with savedInstanceState
        if (mStartTime == 0 && savedInstanceState.containsKey(START_TIME_KEY)) {
            mStartTime = savedInstanceState.getLong(START_TIME_KEY);
        }
    }

    @Override // FragmentActivity
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mService != null) {
            mService.enableUpgrade(true);
            restoreUpgradeDialog();
        }
    }

    private void restoreUpgradeDialog() {
        if (mService != null && mService.isUpgrading()) {
            if (mService.getConnectionState() == BluetoothService.State.CONNECTED) {
                Log.d(TAG, "restoreUpgradeDialog: 上次传输步骤" + mService.getResumePoint());
            }
        }
    }

    @Override // Activity
    protected void onPause() {
        super.onPause();
        if (mService != null && !mService.isUpgrading()) {
            mService.enableUpgrade(false);
        }
    }

    @Override
    protected void handleMessageFromService(Message msg) {
        String handleMessage = "Handle a message from BLE service: " + msg.what;

        switch (msg.what) {
            case BluetoothService.Messages.CONNECTION_STATE_HAS_CHANGED:
                @BluetoothService.State int connectionState = (int) msg.obj;
                onConnectionStateChanged(connectionState);
                String stateLabel = connectionState == BluetoothService.State.CONNECTED ? "CONNECTED"
                        : connectionState == BluetoothService.State.CONNECTING ? "CONNECTING"
                        : connectionState == BluetoothService.State.DISCONNECTING ? "DISCONNECTING"
                        : connectionState == BluetoothService.State.DISCONNECTED ? "DISCONNECTED"
                        : "UNKNOWN";
                if (mService == null || !mService.isUpgrading()) {
                    displayLongToast(getString(R.string.toast_device_information) + stateLabel);
                }
                if (DEBUG)
                    Log.d(TAG, handleMessage + "CONNECTION_STATE_HAS_CHANGED: " + stateLabel);
                break;

            case BluetoothService.Messages.DEVICE_BOND_STATE_HAS_CHANGED:
                int bondState = (int) msg.obj;
                String bondStateLabel = bondState == BluetoothDevice.BOND_BONDED ? "BONDED"
                        : bondState == BluetoothDevice.BOND_BONDING ? "BONDING"
                        : "BOND NONE";
                if (mService == null || !mService.isUpgrading()) {
                    displayLongToast(getString(R.string.toast_device_information) + bondStateLabel);
                }
                if (DEBUG)
                    Log.d(TAG, handleMessage + "DEVICE_BOND_STATE_HAS_CHANGED: " + bondStateLabel);
                break;

            case BluetoothService.Messages.GATT_SUPPORT:
                if (DEBUG) Log.d(TAG, handleMessage + "GATT_SUPPORT");

                break;
            case BluetoothService.Messages.GAIA_READY:
                if (DEBUG) Log.d(TAG, handleMessage + "GAIA_READY");
                startUpgrade(mFile);
                break;

            case BluetoothService.Messages.GATT_READY:
                if (DEBUG) Log.d(TAG, handleMessage + "GATT_READY");
                break;

            case BluetoothService.Messages.UPGRADE_MESSAGE:
                @BluetoothService.UpgradeMessage int upgradeMessage = msg.arg1;
                Object content = msg.obj;
                onReceiveUpgradeMessage(upgradeMessage, content);
                break;

            case BluetoothService.Messages.GATT_MESSAGE:
                @GAIAGATTBLEService.GattMessage int gattMessage = msg.arg1;
                Object data = msg.obj;
                onReceiveGattMessage(gattMessage, data);
                if (DEBUG) Log.d(TAG, handleMessage + "GATT_MESSAGE");
                break;

            default:
                if (DEBUG)
                    Log.d(TAG, handleMessage + "UNKNOWN MESSAGE: " + msg.what);
                break;
        }
    }

    private void onReceiveUpgradeMessage(int message, Object content) {
        StringBuilder handleMessage = new StringBuilder("Handle a message from BLE service: UPGRADE_MESSAGE, ");
        switch (message) {
            case BluetoothService.UpgradeMessage.UPGRADE_FINISHED:
                Log.d(TAG, "onReceiveUpgradeMessage: 升级完成");
                mStartTime = 0;
                handleMessage.append("UPGRADE_FINISHED");
                break;

            case BluetoothService.UpgradeMessage.UPGRADE_REQUEST_CONFIRMATION:
                @UpgradeManager.ConfirmationType int confirmation = (int) content;
                askForConfirmation(confirmation);
                handleMessage.append("UPGRADE_REQUEST_CONFIRMATION");
                break;

            case BluetoothService.UpgradeMessage.UPGRADE_STEP_HAS_CHANGED:
                @ResumePoints.Enum int step = (int) content;
                if (step == ResumePoints.Enum.DATA_TRANSFER
                        && mStartTime == 0 /* step does not change due to a reconnection */) {
                    mStartTime = SystemClock.elapsedRealtime();
                }
                Log.d(TAG, "onReceiveUpgradeMessage: step" + step);
                handleMessage.append("UPGRADE_STEP_HAS_CHANGED");
                break;

            case BluetoothService.UpgradeMessage.UPGRADE_ERROR:
                UpgradeError error = (UpgradeError) content;
                mStartTime = 0;
                manageError(error);
                handleMessage.append("UPGRADE_ERROR");
                break;

            case BluetoothService.UpgradeMessage.UPGRADE_UPLOAD_PROGRESS:
                double percentage = (double) content;
                Log.d(TAG, "onReceiveUpgradeMessage: percentage" + percentage);
                handleMessage.append("UPGRADE_UPLOAD_PROGRESS");
                break;
        }

        if (DEBUG && message != BluetoothService.UpgradeMessage.UPGRADE_UPLOAD_PROGRESS) {
            // The upgrade upload messages are not displayed to avoid too many logs.
            Log.d(TAG, handleMessage.toString());
        }
    }

    /**
     * <p>This method is called when this activity receives a
     * {@link GAIAGATTBLEService.GattMessage GattMessage} from the Service.</p>
     * <p>This method will act depending on the type of GATT message which had been broadcast to this activity.</p>
     *
     * @param gattMessage The GATT Message type.
     * @param data        Any complementary information provided with the GATT Message.
     */
    @SuppressLint("SwitchIntDef")
    private void onReceiveGattMessage(@GAIAGATTBLEService.GattMessage int gattMessage, Object data) {
        switch (gattMessage) {

            case GAIAGATTBLEService.GattMessage.RWCP_SUPPORTED:
                boolean rwcpSupported = (boolean) data;
                if (!rwcpSupported) {
                    Toast.makeText(this, R.string.toast_rwcp_not_supported, Toast.LENGTH_SHORT).show();
                }
                break;

            case GAIAGATTBLEService.GattMessage.RWCP_ENABLED:
                mIsRWCPEnabled = (boolean) data;
                int textRWCPEnabled = mIsRWCPEnabled ? R.string.toast_rwcp_enabled : R.string.toast_rwcp_disabled;
                Toast.makeText(this, textRWCPEnabled, Toast.LENGTH_SHORT).show();
                break;

            case GAIAGATTBLEService.GattMessage.TRANSFER_FAILED:
                // The transport layer has failed to transmit bytes to the device using RWCP
                Log.d(TAG, "onReceiveGattMessage: " + getString(R.string.dialog_upgrade_transfer_failed));
                break;

            case GAIAGATTBLEService.GattMessage.MTU_SUPPORTED:
                boolean mtuSupported = (boolean) data;
                if (!mtuSupported) {
                    Toast.makeText(this, R.string.toast_mtu_not_supported, Toast.LENGTH_SHORT).show();
                }
                break;

            case GAIAGATTBLEService.GattMessage.MTU_UPDATED:
                int mtu = (int) data;
                Toast.makeText(this, getString(R.string.toast_mtu_updated) + " " + mtu, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * <p>This method allows the Upgrade process to ask the user for any confirmation to carry on the upgrade process.</p>
     *
     * @param confirmation The type of confirmation which has to be requested from the user.
     */
    private void askForConfirmation(@UpgradeManager.ConfirmationType final int confirmation) {
        switch (confirmation) {
            case UpgradeManager.ConfirmationType.COMMIT:
                displayConfirmationDialog(confirmation, R.string.alert_upgrade_commit_title,
                        R.string.alert_upgrade_commit_message);
                break;
            case UpgradeManager.ConfirmationType.IN_PROGRESS:
                // no obligation to ask for confirmation as the commit confirmation should happen next
                mService.sendConfirmation(confirmation, true);
                break;
            case UpgradeManager.ConfirmationType.TRANSFER_COMPLETE:
                displayConfirmationDialog(confirmation, R.string.alert_upgrade_transfer_complete_title,
                        R.string.alert_upgrade_transfer_complete_message);
                break;
            case UpgradeManager.ConfirmationType.BATTERY_LOW_ON_DEVICE:
                displayConfirmationDialog(confirmation, R.string.alert_upgrade_low_battery_title,
                        R.string.alert_upgrade_low_battery_message);
                break;
            case UpgradeManager.ConfirmationType.WARNING_FILE_IS_DIFFERENT:
                displayConfirmationDialog(confirmation, R.string.alert_upgrade_sync_id_different_title,
                        R.string.alert_upgrade_sync_id_different_message);
                break;
        }
    }

    private void displayConfirmationDialog(int confirmation, int alert_upgrade_commit_title, int alert_upgrade_commit_message) {
        Log.d(TAG, "displayConfirmationDialog: " + alert_upgrade_commit_title + "\n" + alert_upgrade_commit_message);
        mService.sendConfirmation(confirmation, true);
    }

    /**
     * <p>When an error occurs during the upgrade, this method allows display of error information to the user
     * depending on the error type contained on the {@link UpgradeError UpgradeError} parameter.</p>
     *
     * @param error The information related to the error which occurred during the upgrade process.
     */
    private void manageError(UpgradeError error) {
        switch (error.getError()) {
            case UpgradeError.ErrorTypes.AN_UPGRADE_IS_ALREADY_PROCESSING:
                // nothing should happen as there is already an upgrade processing.
                // in case it's not already displayed, we display the Upgrade dialog
                Log.d(TAG, "manageError: 正在升级");
                break;

            case UpgradeError.ErrorTypes.ERROR_BOARD_NOT_READY:
                // display error message + "please try again later"
                Log.d(TAG, "manageError: 蓝牙未绑定");
                break;

            case UpgradeError.ErrorTypes.EXCEPTION:
                // display that an error has occurred?
                Log.d(TAG, "manageError: 未知异常" + error.getString());
                break;

            case UpgradeError.ErrorTypes.NO_FILE:
                Log.d(TAG, "manageError: 升级文件不存在");
                break;

            case UpgradeError.ErrorTypes.RECEIVED_ERROR_FROM_BOARD:
                Log.d(TAG, "manageError: 升级失败" + ReturnCodes.getReturnCodesMessage(error.getReturnCode()) + Utils.getIntToHexadecimal(error.getReturnCode()));
                break;

            case UpgradeError.ErrorTypes.WRONG_DATA_PARAMETER:
                Log.d(TAG, "manageError: 数据传输错误");
                break;
        }
    }

    private void onConnectionStateChanged(int connectionState) {
        Log.d(TAG, "onConnectionStateChanged: " + connectionState);
    }

    @Override
    protected void onServiceConnected() {
        mService.enableUpgrade(true);
        mService.enableDebugLogs(Consts.DEBUG);
        @GAIA.Transport int transport = getTransport() == BluetoothService.Transport.BR_EDR ?
                GAIA.Transport.BR_EDR : GAIA.Transport.BLE;
        if (mService.getTransport() == BluetoothService.Transport.BLE) {
            ((GAIAGATTBLEService) mService).getRWCPStatus();
        }
        restoreUpgradeDialog();
    }


    @Override
    protected void onServiceDisconnected() {
        mService.enableUpgrade(false);
        mService.enableDebugLogs(Consts.DEBUG);
    }


}
