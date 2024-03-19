/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.gaiacontrol.ui;

import android.content.Context;
import android.content.res.TypedArray;

import androidx.annotation.AttrRes;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.qualcomm.qti.gaiacontrol.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>This view component describes the view of a parameter which is composed of a title, a subtitle and a control.</p>
 * <p>To set up the right control, a ParameterView requires a {@link Type Type}.</p>
 * <p>The title, the subtitle and the type must be defined when creating the view through the attributes set, see
 * {@link R.styleable#ParameterView ParameterView}.</p>
 */
public class ParameterView extends FrameLayout {

    // ====== PRIVATE FIELDS ========================================================================

    /**
     * The tag to display for logs.
     */
    private static final String TAG = "ParameterView";
    /**
     * The switch the user can check and uncheck when type is {@link Type#SWITCH SWITCH}.
     */
    private Switch mSwitch;
    /**
     * The edit text the user can edit the text of when type is {@link Type#EDIT_TEXT EDIT_TEXT}.
     */
    private EditText mEditText;
    /**
     * The text views to display the parameter information.
     */
    private TextView mTitle, mSubTitle;
    /**
     * The undeterminate progress bar to show while validating the value typed by the user.
     */
    private ProgressBar mProgressBar;
    /**
     * To differentiate if the call to
     * {@link CompoundButton.OnCheckedChangeListener#onCheckedChanged(CompoundButton, boolean) onCheckedChanged} had
     * been made by the application or the user.
     */
    private boolean isProgrammaticallyChecked = false;
    /**
     * The listener which would like to be notified of user events within this view.
     */
    private ParameterViewListener mListener;
    /**
     * The type of parameter to display the corresponding control.
     */
    private @Type int mType;


    // ====== ENUMS ========================================================================

    /**
     * All types a ParameterView component can be in order to display the corresponding control.
     */
    @IntDef({ Type.SWITCH, Type.EDIT_TEXT })
    @Retention(RetentionPolicy.SOURCE)
    @interface Type {
        /**
         * This type displays a switch control as a parameter.
         */
        int SWITCH = 0;
        /**
         * This type displays an edit text control as a parameter.
         */
        int EDIT_TEXT = 1;
    }

    // ====== CONSTRUCTORS ========================================================================

    /* All mandatory constructors when implementing a View */

    public ParameterView(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public ParameterView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public ParameterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("unused")
    public ParameterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }


    // ====== PUBLIC METHODS ========================================================================

    @Override // FrameLayout
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mEditText.setEnabled(enabled);
        mSwitch.setEnabled(enabled);
        mTitle.setEnabled(enabled);
        mSubTitle.setEnabled(enabled);
    }

    /**
     * <p>To set up the listener which would like to be notified of events from this view.</p>
     *
     * @param listener
     *          The listener to notify when the user interacts with this view.
     */
    public void setListener(ParameterViewListener listener) {
        mListener = listener;
    }

    /**
     * <p>To set up the value displayed in the edit text if this ParameterView type is
     * {@link Type#EDIT_TEXT EDIT_TEXT}.</p>
     *
     * @param value
     *          The integer value to display.
     */
    public void setValue(int value) {
        if (mType != Type.EDIT_TEXT) {
            Log.w(TAG, "setValue() rejected: ParameterView is not an EDIT_TEXT");
            return;
        }
        mEditText.setEnabled(true);
        String text = "" + value;
        mEditText.setText(text);
    }

    /**
     * <p>To check or uncheck the switch of this ParameterView if its type is {@link Type#SWITCH SWITCH}.</p>
     *
     * @param checked
     *          True to check the switch, false to uncheck it.
     */
    public void setChecked(boolean checked) {
        if (mType != Type.SWITCH) {
            Log.w(TAG, "setValue() rejected: ParameterView is not a SWITCH");
            return;
        }
        isProgrammaticallyChecked = true;
        mSwitch.setEnabled(true);
        mSwitch.setChecked(checked);
        isProgrammaticallyChecked = false;
    }

    /**
     * <p>To hide the control of this ParameterView in order to display an undeterminate progress bar instead.</p>
     *
     * @param show
     *          True to show the progress bar, false to display the control.
     */
    public void showProgress(boolean show) {
        mProgressBar.setVisibility(show ? VISIBLE : INVISIBLE);
        switch (mType) {
            case Type.EDIT_TEXT:
                mEditText.setVisibility(show ? INVISIBLE : VISIBLE);
                break;
            case Type.SWITCH:
                mSwitch.setVisibility(show ? INVISIBLE : VISIBLE);
                break;
        }
    }

    /**
     * <p>To enable and disable the whole view depending on the support of the parameter it represents.</p>
     * <p>This view does not enable the controls.</p>
     *
     * @param supported
     *          True to display this parameter as supported, false otherwise.
     */
    public void setSupported(boolean supported) {
        setEnabled(supported);
        if (supported) {
            mSwitch.setEnabled(false);
            mEditText.setEnabled(false);
        }
    }

    /**
     * <p>To display an error when the user has entered a wrong value.</p>
     *
     * @param error
     *          The text to display.
     */
    public void displayError(String error) {
        if (mType != Type.EDIT_TEXT) {
            Log.w(TAG, "displayError(): not implemented for types other than EDIT_TEXT.");
            return;
        }
        mEditText.setError(error);
    }


    // ====== PRIVATE METHODS ========================================================================

    /**
     * <p>Inflate the layout used for the {@link ParameterView} and initialises all the view components.</p>
     *
     * @param context
     *          Context fo the application, used to inflate the layout.
     * @param attrs
     *          The attributes set up within the declaration of {@link ParameterView} in an xml file.
     * @param defStyleAttr
     *          An attribute in the current theme that contains a reference to a style resource that supplies defaults
     *          values for the TypedArray. Can be 0 to not look for defaults.
     * @param defStyleRes
     *          A resource identifier of a style resource that supplies default values for the TypedArray, used only
     *          if defStyleAttr is 0 or can not be found in the theme. Can be 0 to not look for defaults.
     */
    private void init(Context context, AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        String title = "", subtitle = "";
        @Type int type = Type.SWITCH;

        // get the attributes defined in the XML file
        if (attrs != null) {
            TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ParameterView, defStyleAttr,
                                                                         defStyleRes);
            type = getAttributeType(array);
            title = getAttributeTitle(array);
            subtitle = getAttributeSubTitle(array);
            array.recycle();
        }
        else {
            Log.w(TAG, "init: ParameterView component MUST be defined with a set of attributes.");
        }

        // inflate the layout
        inflate(getContext(), R.layout.layout_parameter, this);

        mSwitch = findViewById(R.id.parameter_switch);
        mEditText = findViewById(R.id.parameter_edit_text);
        mTitle = findViewById(R.id.parameter_title);
        mSubTitle = findViewById(R.id.parameter_subtitle);
        mProgressBar = findViewById(R.id.parameter_progress_bar);

        // init the views
        initComponents(title, subtitle, type);
    }

    /**
     * <p>To set up the type of this parameter view, this shouldn't be called after the initialisation process
     * happened.</p>
     * <p>This method adjusts the components visible on the view depending on the given type.</p>
     *
     * @param type
     *          The type set up for this ParameterView.
     */
    private void setType(@Type int type) {
        mType = type;
        switch (type) {
            case Type.EDIT_TEXT:
                mSwitch.setVisibility(INVISIBLE);
                mEditText.setVisibility(VISIBLE);
                break;

            case Type.SWITCH:
                mSwitch.setVisibility(VISIBLE);
                mEditText.setVisibility(INVISIBLE);
                break;
        }
    }

    /**
     * <p>To set up the title of this parameter view, this shouldn't be changed after the initialisation process
     * happened.</p>
     *
     * @param title
     *          The title to display in the view.
     */
    private void setTitle(String title) {
        mTitle.setText(title);
        mTitle.setVisibility(VISIBLE);
    }

    /**
     * <p>To set up the subtitle of this parameter view, this shouldn't be changed after the initialisation process
     * happened.</p>
     *
     * @param subtitle
     *          The subtitle to display in the view.
     */
    private void setSubtitle(String subtitle) {
        if (subtitle == null || subtitle.length() == 0) {
            mSubTitle.setVisibility(GONE);
        }
        else {
            mSubTitle.setVisibility(VISIBLE);
            mSubTitle.setText(subtitle);
        }
    }

    /**
     * <p>To retrieve the type from an attribute set.</p>
     *
     * @param array
     *          The array extracted from an attribute set.
     *
     * @return The type described for the ParameterView.
     */
    private @Type int getAttributeType(TypedArray array) {
        if (array != null && array.hasValue(R.styleable.ParameterView_type)) {
            return array.getInteger(R.styleable.ParameterView_type, Type.SWITCH);
        }

        Log.w(TAG, "getAttributeType: ParameterView component MUST be defined with a type.");
        return Type.SWITCH;
    }

    /**
     * <p>To retrieve the title from an attribute set.</p>
     *
     * @param array
     *          The array extracted from an attribute set.
     *
     * @return The title given in the ParameterView description.
     */
    private String getAttributeTitle(TypedArray array) {
        if (array != null && array.hasValue(R.styleable.ParameterView_type)) {
            return array.getString(R.styleable.ParameterView_title);
        }

        Log.w(TAG, "getAttributeTitle: ParameterView component MUST be defined with a title.");
        return "";
    }

    /**
     * <p>To retrieve the subtitle from an attribute set.</p>
     *
     * @param array
     *          The array extracted from an attribute set.
     *
     * @return The subtitle given in the ParameterView description.
     */
    private String getAttributeSubTitle(TypedArray array) {
        if (array != null && array.hasValue(R.styleable.ParameterView_type)) {
            return array.getString(R.styleable.ParameterView_subtitle);
        }

        Log.w(TAG, "getSubtitle: ParameterView component MUST be defined with a subtitle.");
        return "";
    }

    /**
     * <p>To initialise the values and states of all the view components.</p>
     *
     * @param title
     *          The title of the ParameterView
     * @param subtitle
     *          The subtitle of the ParameterView
     * @param type
     *          The type of the ParameterView
     */
    private void initComponents(String title, String subtitle, @Type int type) {
        setTitle(title);
        setSubtitle(subtitle);
        setType(type);

        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (isProgrammaticallyChecked) {
                    // no need to dispatch the event, setChecked had been called
                    return;
                }
                mListener.onChecked(getId(), checked);
            }
        });

        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    // hide keyboard
                    InputMethodManager inputManager =
                            (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (inputManager != null) {
                        inputManager.hideSoftInputFromWindow(mEditText.getWindowToken(),
                                                             InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                    // dispatch user input
                    mListener.onTextEdited(getId(), Integer.parseInt(mEditText.getText().toString()));
                    return true;
                }
                return false;
            }
        });
    }


    // ====== INNER INTERFACES ========================================================================

    /**
     * <p>The listener which describes the methods to implement in order to receive events when the user interacts
     * with a ParameterView./p>
     */
    public interface ParameterViewListener {

        /**
         * <p>Called when the user has validated the edition of the Edit Text if this is an {@link Type#EDIT_TEXT}
         * view.</p>
         *
         * @param id
         *          The id of this parameter view.
         * @param value
         *          The value typed by the user in the edit text.
         */
        void onTextEdited(int id, int value);

        /**
         * <p>Called when the user has checked the switch if this is a {@link Type#SWITCH} view.</p>
         *
         * @param id
         *          The id of this parameter view.
         * @param checked
         *          True if the user has checked the switch, false if they unchecked it.
         */
        void onChecked(int id, boolean checked);
    }
}
