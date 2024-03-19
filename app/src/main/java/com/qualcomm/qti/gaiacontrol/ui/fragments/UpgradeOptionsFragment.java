/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.gaiacontrol.ui.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.qualcomm.qti.gaiacontrol.Consts;
import com.qualcomm.qti.gaiacontrol.R;
import com.qualcomm.qti.gaiacontrol.rwcp.RWCP;
import com.qualcomm.qti.gaiacontrol.services.GAIAGATTBLEService;
import com.qualcomm.qti.gaiacontrol.ui.ParameterView;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * <p>This fragment displays an user interface to manage options of an update: it gives the possibility to change
 * some RWCP, MTU and logs parameters. It also displays the file the user has selected.</p>
 */
public class UpgradeOptionsFragment extends Fragment implements ParameterView.ParameterViewListener {

    // ====== FIELDS ====================================================================

    /**
     * For debug mode, the tag to display for logs.
     */
    private static final String TAG = "UpgradeOptionsFragment";
    /**
     * The layout which has all the file information.
     */
    private View mLayoutFileInformation;
    /**
     * The button to start the upgrade action.
     */
    private View mButtonActionUpgrade;
    /**
     * The text view which displays the sub-message for picking a file. If there is a file this message explains how
     * to change it, if there is no selected file, this message explains how to pick one.
     */
    private TextView mTextViewActionPickFileMessage;
    /**
     * The text view to display when the selected file has been modified for the last time.
     */
    private TextView mTextViewFileLastModification;
    /**
     * The text view to display the selected file name.
     */
    private TextView mTextViewFileName;
    /**
     * The text view to display the selected file path.
     */
    private TextView mTextViewFilePath;
    /**
     * The text view to display the selected file size.
     */
    private TextView mTextViewFileSize;
    /**
     * All the views which represents some configurable parameters for the upgrade.
     */
    private ParameterView mRWCPParameter, mMtuParameter, mInitialWindowParameter, mMaximumWindowParameter;
    /**
     * To know if the user has already change the MTU size to know if the Warning dialog needs to be displayed.
     */
    private boolean hadUserChangedMtu = false;
    /**
     * The listener to trigger events from this fragment.
     */
    private UpgradeOptionsFragmentListener mListener;


    // ====== STATIC METHODS ====================================================================

    /**
     * The factory method to create a new instance of this fragment using the provided parameters.
     *
     * @return A new instance of fragment UpgradeVMFragment.
     */
    public static UpgradeOptionsFragment newInstance() {
        return new UpgradeOptionsFragment();
    }


    // ====== CONSTRUCTORS ====================================================================

    /**
     * Empty constructor - required.
     */
    public UpgradeOptionsFragment() {
    }


    // ====== FRAGMENT METHODS ====================================================================

    // This event fires first, before creation of fragment or any views
    // The onAttach method is called when the Fragment instance is associated with an Activity.
    // This does not mean the Activity is fully initialized.
    @Override // Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof UpgradeOptionsFragmentListener) {
            this.mListener = (UpgradeOptionsFragmentListener) context;
            // listener attached the views can be set up with values
            initValues();
        }
    }

    @Override // Fragment
    public void onResume() {
        super.onResume();
        // the views are initialised the views can be set up with values
        initValues();
    }

    @Override // Fragment
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upgrade_options, container, false);
        init(view);
        return view;
    }


    // ====== PARAMETER VIEW METHODS ====================================================================

    @Override // ParameterView.ParameterViewListener
    public void onTextEdited(int id, int value) {
        switch (id) {
            case R.id.parameter_mtu_size:
                onMtuChangedByUser(value);
                return;
            case R.id.parameter_initial_window:
                onInitialWindowChangedByUser(value);
                return;
            case R.id.parameter_maximum_window:
                onMaximumWindowChangedByUser(value);
//                return; // not necessary
        }
    }

    @Override // ParameterView.ParameterViewListener
    public void onChecked(int id, boolean checked) {
        switch (id) {
            case R.id.parameter_rwcp:
                onRWCPChecked(checked);
                return;
            case R.id.parameter_debug_logs:
                onLogsChecked(checked);
                break;
        }
    }


    // ====== PUBLIC METHODS ====================================================================

    /**
     * <p>Called when this activity receives a
     * {@link GAIAGATTBLEService.GattMessage#RWCP_SUPPORTED RWCP_SUPPORTED} message from
     * the attached service.</p>
     * <p>It manages the corresponding view components depending on the given support value.</p>
     *
     * @param supported
     *          True if RWCP is supported, false otherwise.
     */
    public void onRWCPSupported(boolean supported) {
        mRWCPParameter.setSupported(supported);
    }

    /**
     * <p>Called when this activity receives a
     * {@link GAIAGATTBLEService.GattMessage#RWCP_ENABLED RWCP_ENABLED} message from
     * the attached service.</p>
     * <p>It manages the corresponding view components depending on the given support value.</p>
     *
     * @param enabled
     *          True if RWCP had been enabled, false otherwise.
     * @param fileSelected
     *          True if a file is selected, false otherwise.
     */
    public void onRWCPEnabled(boolean enabled, boolean fileSelected) {
        mRWCPParameter.showProgress(false);
        mButtonActionUpgrade.setEnabled(fileSelected);
        mRWCPParameter.setChecked(enabled);
        mInitialWindowParameter.setEnabled(enabled);
        mMaximumWindowParameter.setEnabled(enabled);
    }

    /**
     * <p>Called when this activity receives a
     * {@link GAIAGATTBLEService.GattMessage#MTU_SUPPORTED MTU_SUPPORTED} message from
     * the attached service.</p>
     * <p>It manages the corresponding view components depending on the given support value.</p>
     *
     * @param supported
     *          True if the maximum MTU size is supported, false otherwise.
     * @param fileSelected
     *          True if a file is selected, false otherwise.
     */
    public void onMtuSupported(boolean supported, boolean fileSelected) {
        mMtuParameter.setSupported(supported);
        if (!supported) {
            mMtuParameter.showProgress(false);
            mButtonActionUpgrade.setEnabled(fileSelected);
        }
    }

    /**
     * <p>Called when this activity receives a
     * {@link GAIAGATTBLEService.GattMessage#MTU_UPDATED MTU_UPDATED} message from
     * the attached service.</p>
     * <p>It manages the corresponding view components depending on the given value.</p>
     *
     * @param size
     *          The size which is configured between the Android device and the connected device.
     * @param fileSelected
     *          True if a file is selected, false otherwise.
     */
    public void onMtuUpdated(int size, boolean fileSelected) {
        mMtuParameter.showProgress(false);
        mButtonActionUpgrade.setEnabled(fileSelected);
        mMtuParameter.setValue(size);
    }


    // ====== PRIVATE METHODS ====================================================================

    /**
     * This method allows initialisation of components.
     *
     * @param view
     *            The inflated view for this fragment.
     */
    private void init(View view) {
        // get UI components
        Button buttonFilePicker = view.findViewById(R.id.bt_action_pick_file);
        mLayoutFileInformation = view.findViewById(R.id.layout_file_information);
        mButtonActionUpgrade = view.findViewById(R.id.bt_action_upgrade);
        mTextViewActionPickFileMessage = view.findViewById(R.id.tv_action_pick_file_message);
        mTextViewFileLastModification = view.findViewById(R.id.tv_file_last_modification);
        mTextViewFileName = view.findViewById(R.id.tv_file_name);
        mTextViewFilePath = view.findViewById(R.id.tv_file_path);
        mTextViewFileSize = view.findViewById(R.id.tv_file_size);
        mMtuParameter = view.findViewById(R.id.parameter_mtu_size);
        mRWCPParameter = view.findViewById(R.id.parameter_rwcp);
        mInitialWindowParameter = view.findViewById(R.id.parameter_initial_window);
        mMaximumWindowParameter = view.findViewById(R.id.parameter_maximum_window);
        ParameterView logsParameter = view.findViewById(R.id.parameter_debug_logs);

        // setting up components
        buttonFilePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.pickFile();
            }
        });
        mButtonActionUpgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.startUpgrade();
            }
        });
        mRWCPParameter.setListener(this);
        mMtuParameter.setListener(this);
        mInitialWindowParameter.setEnabled(false);
        mInitialWindowParameter.setListener(this);
        mMaximumWindowParameter.setEnabled(false);
        mMaximumWindowParameter.setListener(this);
        logsParameter.setChecked(Consts.DEBUG);
        logsParameter.setListener(this);

//        mTextViewFileSize.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                onRWCPChecked(true);
//            }
//        },4000);


    }

    /**
     * <p>To initialise the values of the different view components in order to match the ones used by the
     * application.</p>
     * <p>To initialise the value a listener needs to be attached and the views created.</p>
     */
    private void initValues() {
        if (mListener != null && mTextViewFileName != null) {
            // listener to get values is attached and views had been initialised
            setFileInformation(mListener.getFile());
            //onRWCPEnabled(mListener.isRWCPEnabled(), mListener.getFile() != null);
            mMtuParameter.setValue(mListener.getMtuSize());
            mInitialWindowParameter.setValue(mListener.getRWCPInitialWindow());
            mMaximumWindowParameter.setValue(mListener.getRWCPMaximumWindow());
        }
    }

    /**
     * <p>To display the file information of the given file on the UI.</p>
     * <p>This method shows the file description view and updates it with: the file name, the last modified date,
     * the canonical path and the size in KB.</p>
     * <p>If the file is null, the file description view is hidden.</p>
     * <p>This method also the sub message from the picking option depending of a file is selected or not.</p>
     *
     * @param file
     *          The file object to display information from.
     */
    private void setFileInformation(File file) {
        if (file != null) {
            mButtonActionUpgrade.setEnabled(true);
            mLayoutFileInformation.setVisibility(View.VISIBLE);
            mTextViewActionPickFileMessage.setText(R.string.message_pick_file_change);
            mTextViewFileName.setText(file.getName());
            String date = DateFormat.format(Consts.DATE_FORMAT, new Date(file.lastModified())).toString();
            mTextViewFileLastModification.setText(date);
            try {
                mTextViewFilePath.setText(file.getCanonicalPath());
            } catch (IOException e) {
                Log.e(TAG, "Get Canonical path error: " + e.getMessage());
            }
            long size = file.length() / 1024; // file size in bytes changed to kb
            String sizeText = size + Consts.UNIT_FILE_SIZE;
            mTextViewFileSize.setText(sizeText);
        }
        else {
            mButtonActionUpgrade.setEnabled(false);
            mLayoutFileInformation.setVisibility(View.GONE);
            mTextViewActionPickFileMessage.setText(R.string.message_pick_file_default);
        }
    }

    /**
     * <p>Called when the user checks the RWCP switch in order to enable or disable the RWCP mode.</p>
     * <p>This method calls {@link GAIAGATTBLEService#enableRWCP(boolean) enableRWCP} and hide or show components
     * while the request is executed.</p>
     *
     * @param checked True if the user has enabled the RWCP mode, false otherwise.
     */
    private void onRWCPChecked(boolean checked) {
        if (mListener.enableRWCP(checked)) {
            mRWCPParameter.showProgress(true);
            mButtonActionUpgrade.setEnabled(false);
        }
        else {
            // RWCP is not supported
            mRWCPParameter.showProgress(false);
            mRWCPParameter.setEnabled(false);
            mRWCPParameter.setChecked(false);
        }
    }

    /**
     * <p>Called when the user types a new value for the MTU size.</p>
     * <p>This method displays a warning if the given size is not within the expected bounds:
     * {@link Consts.MTU#MINIMUM MINIMUM} and {@link Consts.MTU#MAXIMUM MAXIMUM}.</p>
     * <p>This method shows a warning dialog if the user has not been informed yet of the possible long delay when
     * changing the MTU size. Otherwise it calls {@link #changeMTU(int) changeMTU} to request the MTu size to be
     * changed.</p>
     *
     * @param size
     *          The MTU sets up by the user.
     */
    private void onMtuChangedByUser(int size) {
        if (size < Consts.MTU.MINIMUM || size > Consts.MTU.MAXIMUM) {
            mMtuParameter.displayError(getString(R.string.error_size_out_of_range, Consts.MTU.MINIMUM, Consts.MTU.MAXIMUM));
            mMtuParameter.setValue(mListener.getMtuSize());
            return;
        }

        if (!hadUserChangedMtu) {
            showMtuDialog(size);
        }
        else {
            changeMTU(size);
        }
    }

    /**
     * <p>Called when the user types a new value for the initial window size.</p>
     * <p>This method displays a warning if the given size is not within the expected bounds:
     * 1 and the maximum as given by
     * {@link UpgradeOptionsFragmentListener#getRWCPMaximumWindow() getRWCPMaximumWindow}.</p>
     *
     * @param size
     *          The size sets up by the user.
     */
    private void onInitialWindowChangedByUser(int size) {
        int maximum = mListener.getRWCPMaximumWindow();
        if (size <= 0 || size > maximum) {
            mInitialWindowParameter.displayError(getString(R.string.error_size_out_of_range, 1, maximum));
            mInitialWindowParameter.setValue(mListener.getRWCPInitialWindow());
            return;
        }

        if (!mListener.setRWCPInitialWindow(size)) {
            mInitialWindowParameter.displayError(getString(R.string.error_size_not_modifiable));
            mInitialWindowParameter.setValue(mListener.getRWCPInitialWindow());
        }
    }

    /**
     * <p>Called when the user types a new value for the maximum window size.</p>
     * <p>This method displays a warning if the given size is not within the expected bounds:
     * 1 and {@link RWCP#WINDOW_MAX WINDOW_MAX}.</p>
     *
     * @param size
     *          The size sets up by the user.
     */
    private void onMaximumWindowChangedByUser(int size) {
        if (size <= 0 || size > RWCP.WINDOW_MAX) {
            mMaximumWindowParameter.displayError(getString(R.string.error_size_out_of_range, 1, RWCP.WINDOW_MAX));
            mMaximumWindowParameter.setValue(mListener.getRWCPMaximumWindow());
            return;
        }

        if (!mListener.setRWCPMaximumWindow(size)) {
            mMaximumWindowParameter.displayError(getString(R.string.error_size_not_modifiable));
            mMaximumWindowParameter.setValue(mListener.getRWCPMaximumWindow());
        }
    }

    /**
     * <p>Called when the user validates the set up of MTU size.</p>
     * <p>This method calls {@link GAIAGATTBLEService#setMtuSize(int) setMtuSize} and hide or show
     * components while the request is executed.</p>
     *
     * @param size
     *          The MTU size sets up by the user.
     */
    private void changeMTU(int size) {
        hadUserChangedMtu = true;
        if (mListener.setMTUSize(size)) {
            mMtuParameter.showProgress(true);
            mButtonActionUpgrade.setEnabled(false);
        }
        else {
            mMtuParameter.showProgress(false);
            mMtuParameter.setEnabled(false);
            mMtuParameter.setValue(mListener.getMtuSize());
        }

    }

    /**
     * <p>Called when the user does not validate the change of the MTU size.</p>
     * <p>This method manages the properties if related components.</p>
     */
    private void onMtuDialogCancelled() {
        mMtuParameter.setChecked(false);
        mMtuParameter.showProgress(false);
        mButtonActionUpgrade.setEnabled(mListener.getFile() != null);
    }

    /**
     * <p>Called when the user checks the Logs switch in order to enable or disable the Debug logs.</p>
     * <p>This method sends the request to this class listener.</p>
     *
     * @param checked True if the user has enabled the logs, false otherwise.
     */
    private void onLogsChecked(boolean checked) {
        mListener.enableDebugLogs(checked);
    }

    /**
     * <p>To display a dialog to inform the user that changing the MTU size might take a long time if it
     * is not available on the devices.</p>
     * <p>This dialog lets the user confirm or cancel the activation.</p>
     *
     * @param size
     *          The MTU size sets up by the user.
     */
    private void showMtuDialog(final int size) {
        //noinspection ConstantConditions
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.alert_mtu_title)
                .setMessage(R.string.alert_mtu_message);
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                onMtuDialogCancelled();
            }
        });
        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                onMtuDialogCancelled();
            }
        });
        builder.setPositiveButton(R.string.button_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                changeMTU(size);
            }
        });

        builder.create().show();
    }


    // ====== INNER INTERFACES ====================================================================

    /**
     * The listener triggered by events from this fragment.
     */
    public interface UpgradeOptionsFragmentListener {
        /**
         * <p>Called when the user checks the RWCP switch to enable or disable the RWCP mode. This method must enable
         * or disable the RWCP mode in the application and from the device.</p>
         *
         * @param enabled
         *          True when the user chooses to enable the RWCP mode, false otherwise.
         *
         * @return True if it was possible to initiate the request, false if it seems unsupported by the device.
         */
        boolean enableRWCP(boolean enabled);

        /**
         * <p>Called when the user sets up the MTU size. This method must request the MTU size to be changed.</p>
         * <p>When the operation is done, {@link #onMtuUpdated(int, boolean) onMtuUpdated} or
         * {@link #onMtuSupported(boolean, boolean) onMtuSupported} should be called to update the display.</p>
         *
         * @param size
         *          The MTU size sets up by the user.
         *
         * @return True if it was possible to initiate the request, false if it seems unsupported by the device.
         */
        boolean setMTUSize(int size);

        /**
         * <p>To get the current selected file.</p>
         *
         * @return The current selected file as known by the application. Null if there is no file selected.
         */
        File getFile();

        /**
         * <p>This method is called when the user taps on a button in order to pick a file.</p>
         */
        void pickFile();

        /**
         * This method allows to start the upgrade process as asked by the user.
         */
        void startUpgrade();

        /**
         * <p>To get the current status of the RWCP mode: enabled or disabled.</p>
         */
        boolean isRWCPEnabled();

        /**
         * <p>To enable the display of the debug logs in the Android log system.</p>
         *
         * @param enable
         *          True to enable the display of the logs, false otherwise.
         */
        void enableDebugLogs(boolean enable);

        /**
         * <p>To get the current MTU size as configured between the Android device and the connected device.</p>
         *
         * @return The configured MTU size.
         */
        int getMtuSize();

        /**
         * <p>To get the current initial window size as configured for the RWCP client.</p>
         *
         * @return The configured size.
         */
        int getRWCPInitialWindow();

        /**
         * <p>Called when the user sets up the initial window size. This method must set up the initial window size
         * for RWCP to the given size.</p>
         *
         * @param size
         *          The size sets up by the user.
         *
         * @return True if it was possible to change the size, false otherwise.
         */
        boolean setRWCPInitialWindow(int size);

        /**
         * <p>To get the current maximum window size as configured for the RWCP client.</p>
         *
         * @return The configured size.
         */
        int getRWCPMaximumWindow();

        /**
         * <p>Called when the user sets up the maximum window size. This method must set up the maximum window size
         * for RWCP to the given size.</p>
         *
         * @param size
         *          The size sets up by the user.
         *
         * @return True if it was possible to change the size, false otherwise.
         */
        boolean setRWCPMaximumWindow(int size);
    }
}
