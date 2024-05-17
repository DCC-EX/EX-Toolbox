/*Copyright (C) 2017 M. Steve Todd mstevetodd@gmail.com
  
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package dcc_ex.ex_toolbox;

import static android.text.InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
import static dcc_ex.ex_toolbox.threaded_application.context;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.gesture.GestureOverlayView;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieSyncManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dcc_ex.ex_toolbox.logviewer.ui.LogViewerActivity;
import dcc_ex.ex_toolbox.type.message_type;
import dcc_ex.ex_toolbox.util.LocaleHelper;

public class cv_programmer extends AppCompatActivity implements android.gesture.GestureOverlayView.OnGestureListener {

    private threaded_application mainapp;  // hold pointer to mainapp
    private SharedPreferences prefs;

    private Menu tMenu;
    private static boolean savedMenuSelected;

    private boolean isRestarting = false;

    protected GestureOverlayView ov;
    // these are used for gesture tracking
    private float gestureStartX = 0;
    private float gestureStartY = 0;
    protected boolean gestureInProgress = false; // gesture is in progress
    private long gestureLastCheckTime; // time in milliseconds that velocity was last checked
    private static final long gestureCheckRate = 200; // rate in milliseconds to check velocity
    private VelocityTracker mVelocityTracker;

    //**************************************
    private String DccexCv = "";
    private String DccexCvValue = "";
    private EditText etDccexCv;
    private EditText etDccexCvValue;

    private String dccexAddress = "";
    private EditText etDccexWriteAddressValue;

    private String DccexSendCommandValue = "";
    private EditText etDccexSendCommandValue;

    private LinearLayout DccexWriteInfoLayout;
    private TextView DccexWriteInfoLabel;
    private String DccexInfoStr = "";

    private TextView DccexResponsesLabel;
    private TextView DccexSendsLabel;
//    private String dccexResponsesStr = "";
//    private String dccexSendsStr = "";
    private ScrollView DccexResponsesScrollView;
    private ScrollView DccexSendsScrollView;

    private Spinner dcc_action_type_spinner;

//    ArrayList<String> DccexResponsesListHtml = new ArrayList<>();
//    ArrayList<String> dccexSendsListHtml = new ArrayList<>();

    private int dccCvsIndex = 0;
    String[] dccCvsEntryValuesArray;
    String[] dccCvsEntriesArray; // display version

    private int dccCmdIndex = 0;
    String[] dccExCommonCommandsEntryValuesArray;
    String[] dccExCommonCommandsEntriesArray; // display version
    int[] dccExCommonCommandsHasParametersArray; // display version
    String[] dccExCommonCommandsAdditionalInfoArray;

    private int dccExActionTypeIndex = 0;
    String[] dccExActionTypeEntryValuesArray;
    String[] dccExActionTypeEntriesArray; // display version

    private static final int PROGRAMMING_TRACK = 0;
    private static final int PROGRAMMING_ON_MAIN = 1;
    private static final int TRACK_MANAGER = 2;

    Button readAddressButton;
    Button writeAddressButton;
    Button readCvButton;
    Button writeCvButton;
    Button sendCommandButton;
    Button previousCommandButton;
    Button nextCommandButton;
    Button writeTracksButton;
    //    Button hideSendsButton;
    Button clearCommandsButton;

    private LinearLayout dexcProgrammingCommonCvsLayout;
    private LinearLayout dexcProgrammingAddressLayout;
    private LinearLayout dexcProgrammingCvLayout;
    private LinearLayout[] dexcDccexTracklayout = {null, null, null, null, null, null, null, null};
    private LinearLayout dexcDccexTrackLinearLayout;
    Spinner dccExCommonCvsSpinner;
    Spinner dccExCommonCommandsSpinner;

    private int[] dccExTrackTypeIndex = {1, 2, 1, 1, 1, 1, 1, 1};
    private Spinner[] dccExTrackTypeSpinner = {null, null, null, null, null, null, null, null};
    private EditText[] dccExTrackTypeIdEditText = {null, null, null, null, null, null, null, null};
    private LinearLayout[] dccExTrackTypeLayout = {null, null, null, null, null, null, null, null};

    String[] dccExTrackTypeEntryValuesArray;
    String[] dccExTrackTypeEntriesArray; // display version

//    private int dccexPreviousCommandIndex = -1;
//    ArrayList<String> dccexPreviousCommandList = new ArrayList<>();

    static final int WHICH_ADDRESS = 0;
    static final int WHICH_CV = 1;
    static final int WHICH_CV_VALUE = 2;
    static final int WHICH_COMMAND = 3;

    static final int TRACK_TYPE_OFF_NONE_INDEX = 0;
    static final int TRACK_TYPE_DCC_MAIN_INDEX = 1;
    static final int TRACK_TYPE_DCC_PROG_INDEX = 2;
    static final int TRACK_TYPE_DC_INDEX = 3;
    static final int TRACK_TYPE_DCX_INDEX = 4;

    static final String[] TRACK_TYPES = {"NONE", "MAIN", "PROG", "DC", "DCX", "AUTO", "EXT", "PROG"};
    static final boolean[] TRACK_TYPES_NEED_ID = {false, false, false, true, true, false, false, false};
//    static final boolean[] TRACK_TYPES_SELECTABLE = {true, true, true, true, true, true, false, false};


    String cv29SpeedSteps;
    String cv29AnalogueMode;
    String cv29Direction;
    String cv29AddressSize;
    String cv29SpeedTable;

    //**************************************


    private LinearLayout screenNameLine;
    private Toolbar toolbar;
    private LinearLayout statusLine;
    private int toolbarHeight;

    @Override
    public void onGesture(GestureOverlayView arg0, MotionEvent event) {
        gestureMove(event);
    }

    @Override
    public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {
        gestureCancel(event);
    }

    // determine if the action was long enough to be a swipe
    @Override
    public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
        gestureEnd(event);
    }

    @Override
    public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
        gestureStart(event);
    }

    private void gestureStart(MotionEvent event) {
        gestureStartX = event.getX();
        gestureStartY = event.getY();
//        Log.d("EX_Toolbox", "gestureStart x=" + gestureStartX + " y=" + gestureStartY);

        toolbarHeight = toolbar.getHeight() + statusLine.getHeight() + screenNameLine.getHeight();

        gestureInProgress = true;
        gestureLastCheckTime = event.getEventTime();
        mVelocityTracker.clear();

        // start the gesture timeout timer
        if (mainapp.cv_programmer_msg_handler != null)
            mainapp.cv_programmer_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
    }

    public void gestureMove(MotionEvent event) {
        // Log.d("EX_Toolbox", "gestureMove action " + event.getAction());
        if ( (mainapp != null) && (mainapp.cv_programmer_msg_handler != null) && (gestureInProgress) ) {
            // stop the gesture timeout timer
            mainapp.cv_programmer_msg_handler.removeCallbacks(gestureStopped);

            mVelocityTracker.addMovement(event);
            if ((event.getEventTime() - gestureLastCheckTime) > gestureCheckRate) {
                // monitor velocity and fail gesture if it is too low
                gestureLastCheckTime = event.getEventTime();
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000);
                int velocityX = (int) velocityTracker.getXVelocity();
                int velocityY = (int) velocityTracker.getYVelocity();
                // Log.d("EX_Toolbox", "gestureVelocity vel " + velocityX);
                if ((Math.abs(velocityX) < threaded_application.min_fling_velocity) && (Math.abs(velocityY) < threaded_application.min_fling_velocity)) {
                    gestureFailed(event);
                }
            }
            if (gestureInProgress) {
                // restart the gesture timeout timer
                mainapp.cv_programmer_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
            }
        }
    }

    private void gestureEnd(MotionEvent event) {
        // Log.d("EX_Toolbox", "gestureEnd action " + event.getAction() + " inProgress? " + gestureInProgress);
        if ( (mainapp != null) && (mainapp.cv_programmer_msg_handler != null) && (gestureInProgress) ) {
            mainapp.cv_programmer_msg_handler.removeCallbacks(gestureStopped);

            float deltaX = (event.getX() - gestureStartX);
            float absDeltaX =  Math.abs(deltaX);
            if (absDeltaX > threaded_application.min_fling_distance) { // only process left/right swipes
                // valid gesture. Change the event action to CANCEL so that it isn't processed by any control below the gesture overlay
                event.setAction(MotionEvent.ACTION_CANCEL);
                // process swipe in the direction with the largest change
                Intent nextScreenIntent = mainapp.getNextIntentInSwipeSequence(threaded_application.SCREEN_SWIPE_INDEX_CV_PROGRAMMER, deltaX);
                startACoreActivity(this, nextScreenIntent, true, deltaX);
            } else {
                // gesture was not long enough
                gestureFailed(event);
            }
        }
    }

    private void gestureCancel(MotionEvent event) {
        if (mainapp.cv_programmer_msg_handler != null)
            mainapp.cv_programmer_msg_handler.removeCallbacks(gestureStopped);
        gestureInProgress = false;
    }

    void gestureFailed(MotionEvent event) {
        // end the gesture
        gestureInProgress = false;
    }

    //
    // GestureStopped runs when more than gestureCheckRate milliseconds
    // elapse between onGesture events (i.e. press without movement).
    //
    @SuppressLint("Recycle")
    private Runnable gestureStopped = new Runnable() {
        @Override
        public void run() {
            if (gestureInProgress) {
                // end the gesture
                gestureInProgress = false;
            }
        }
    };


    @SuppressLint("HandlerLeak")
    class cv_programmer_handler extends Handler {

        public cv_programmer_handler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {

                case message_type.RESPONSE: {    //handle messages from WiThrottle server
                    String s = msg.obj.toString();
                    String response_str = s.substring(0, Math.min(s.length(), 2));

                    if (s.length() >= 3) {
                        String com1 = s.substring(0, 3);
                        //update power icon
                        if ("PPA".equals(com1)) {
                            mainapp.setPowerStateButton(tMenu);
                        }
                    }
                    break;
                }
                case message_type.RECEIVED_TRACKS:
                    refreshDccexTracksView();
                    break;

                case message_type.RECEIVED_DECODER_ADDRESS:
                    String response_str = msg.obj.toString();
                    if ( (response_str.length() > 0) && !(response_str.charAt(0)=='-') ) {  //refresh address
                        dccexAddress = response_str;
                        DccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexSucceeded);
                    } else {
                        DccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexFailed);
                    }
                    refreshDccexView();
                    break;
                case message_type.RECEIVED_CV:
                    String cvResponseStr = msg.obj.toString();
                    if (cvResponseStr.length() > 0) {
                        String[] cvArgs = cvResponseStr.split("(\\|)");
                        if ( (cvArgs[0].equals(DccexCv)) && !(cvArgs[1].charAt(0)=='-') ) { // response matches what we got back
                            DccexCvValue = cvArgs[1];
                            DccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexSucceeded);
                            checkCv29(DccexCv, DccexCvValue);
                        } else {
                            resetTextField(WHICH_CV_VALUE);
                            DccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexFailed);
                        }
                        refreshDccexView();
                    }
                    break;
                case message_type.DCCEX_COMMAND_ECHO:  // informational response
                case message_type.DCCEX_RESPONSE:
//                    refreshDccexView();
                    refreshDccexCommandsView();
                    break;

                case message_type.WIT_CON_RETRY:
                    witRetry(msg.obj.toString());
                    break;
                case message_type.WIT_CON_RECONNECT:
                    break;
                case message_type.TIME_CHANGED:
                    setActivityTitle();
                    break;
                case message_type.REQUEST_REFRESH_MENU:
                    mainapp.displayToolbarMenuButtons(tMenu);
                    break;
                case message_type.RESTART_APP:
                    startNewCvProgrammerActivity();
                    break;
                case message_type.RELAUNCH_APP:
                case message_type.DISCONNECT:
                case message_type.SHUTDOWN:
                    disconnect();
                    break;
            }
        }
    }

    //	set the title, optionally adding the current time.
    private void setActivityTitle() {
        if (mainapp.getFastClockFormat() > 0)
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    "",
                    getApplicationContext().getResources().getString(R.string.app_name_cv_programmer_short),
                    mainapp.getFastClockTime());
        else
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_cv_programmer),
                    "");
    }

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("EX_Toolbox", "web_activity.onCreate()");

        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("dcc_ex.ex_toolbox_preferences", 0);
        mainapp.applyTheme(this);

        super.onCreate(savedInstanceState);

        if (mainapp.isForcingFinish()) {        // expedite
            return;
        }

        setContentView(R.layout.cv_programmer);

        //put pointer to this activity's handler in main app's shared variable
        mainapp.cv_programmer_msg_handler = new cv_programmer_handler(Looper.getMainLooper());

        mainapp.getCommonPreferences();
        mainapp.loadBackgroundImage(findViewById(R.id.cv_programmerBackgroundImgView));

        readAddressButton = findViewById(R.id.ex_DccexReadAddressButton);
        read_address_button_listener read_address_click_listener = new read_address_button_listener();
        readAddressButton.setOnClickListener(read_address_click_listener);

        writeAddressButton = findViewById(R.id.ex_DccexWriteAddressButton);
        write_address_button_listener write_address_click_listener = new write_address_button_listener();
        writeAddressButton.setOnClickListener(write_address_click_listener);

        etDccexWriteAddressValue = findViewById(R.id.ex_DccexWriteAddressValue);
        etDccexWriteAddressValue.setText("");
        etDccexWriteAddressValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_ADDRESS); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        readCvButton = findViewById(R.id.ex_DccexReadCvButton);
        read_cv_button_listener readCvClickListener = new read_cv_button_listener();
        readCvButton.setOnClickListener(readCvClickListener);

        writeCvButton = findViewById(R.id.ex_DccexWriteCvButton);
        write_cv_button_listener writeCvClickListener = new write_cv_button_listener();
        writeCvButton.setOnClickListener(writeCvClickListener);

        etDccexCv = findViewById(R.id.ex_DccexCv);
        etDccexCv.setText("");
        etDccexCv.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_CV); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        etDccexCvValue = findViewById(R.id.ex_DccexCvValue);
        etDccexCvValue.setText("");
        etDccexCvValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_CV_VALUE); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        sendCommandButton = findViewById(R.id.ex_DccexSendCommandButton);
        send_command_button_listener sendCommandClickListener = new send_command_button_listener();
        sendCommandButton.setOnClickListener(sendCommandClickListener);

        etDccexSendCommandValue = findViewById(R.id.ex_DccexSendCommandValue);
        etDccexSendCommandValue.setInputType(TYPE_TEXT_FLAG_AUTO_CORRECT);
        etDccexSendCommandValue.setText("");
        etDccexSendCommandValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_COMMAND); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        DccexWriteInfoLayout = findViewById(R.id.ex_DccexWriteInfoLayout);
        DccexWriteInfoLabel = findViewById(R.id.ex_DccexWriteInfoLabel);
        DccexWriteInfoLabel.setText("");

        previousCommandButton = findViewById(R.id.ex_DccexPreviousCommandButton);
        previous_command_button_listener previousCommandClickListener = new previous_command_button_listener();
        previousCommandButton.setOnClickListener(previousCommandClickListener);

        nextCommandButton = findViewById(R.id.ex_DccexNextCommandButton);
        next_command_button_listener nextCommandClickListener = new next_command_button_listener();
        nextCommandButton.setOnClickListener(nextCommandClickListener);

        DccexResponsesLabel = findViewById(R.id.ex_DccexResponsesLabel);
        DccexResponsesLabel.setText("");
        DccexSendsLabel = findViewById(R.id.ex_DccexSendsLabel);
        DccexSendsLabel.setText("");

        dccCvsEntryValuesArray = this.getResources().getStringArray(R.array.dccCvsEntryValues);
//        final List<String> dccCvsValuesList = new ArrayList<>(Arrays.asList(dccCvsEntryValuesArray));
        dccCvsEntriesArray = this.getResources().getStringArray(R.array.dccCvsEntries); // display version
//        final List<String> dccCvsEntriesList = new ArrayList<>(Arrays.asList(dccCvsEntriesArray));

        dccCvsIndex=0;
        dccExCommonCvsSpinner = findViewById(R.id.ex_dcc_cv_list);
        ArrayAdapter<?> spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccCvsEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccExCommonCvsSpinner.setAdapter(spinner_adapter);
        dccExCommonCvsSpinner.setOnItemSelectedListener(new spinner_listener());
        dccExCommonCvsSpinner.setSelection(dccCvsIndex);

        dccExCommonCommandsEntryValuesArray = this.getResources().getStringArray(R.array.dccExCommonCommandsEntryValues);
//        final List<String> dccCommonCommandsValuesList = new ArrayList<>(Arrays.asList(dccExCommonCommandsEntryValuesArray));
        dccExCommonCommandsEntriesArray = this.getResources().getStringArray(R.array.dccExCommonCommandsEntries); // display version
//        final List<String> dccCommonCommandsEntriesList = new ArrayList<>(Arrays.asList(dccExCommonCommandsEntriesArray));
        dccExCommonCommandsHasParametersArray = this.getResources().getIntArray(R.array.dccExCommonCommandsHasParameters);
        dccExCommonCommandsAdditionalInfoArray = this.getResources().getStringArray(R.array.dccExCommonCommandsAdditionalInfo);

        dccCmdIndex=0;
        dccExCommonCommandsSpinner = findViewById(R.id.ex_common_commands_list);
        spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExCommonCommandsEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccExCommonCommandsSpinner.setAdapter(spinner_adapter);
        dccExCommonCommandsSpinner.setOnItemSelectedListener(new command_spinner_listener());
        dccExCommonCommandsSpinner.setSelection(dccCmdIndex);

        if (mainapp.DccexVersionValue <= threaded_application.DCCEX_MIN_VERSION_FOR_TRACK_MANAGER) {  /// need to remove the track manager option
            dccExActionTypeEntryValuesArray = new String [2];
            dccExActionTypeEntriesArray = new String [2];
            for (int i=0; i<2; i++) {
                dccExActionTypeEntryValuesArray[i] = this.getResources().getStringArray(R.array.dccExActionTypeEntryValues)[i];
                dccExActionTypeEntriesArray[i] = this.getResources().getStringArray(R.array.dccExActionTypeEntries)[i];
            }
        } else {
            dccExActionTypeEntryValuesArray = this.getResources().getStringArray(R.array.dccExActionTypeEntryValues);
            dccExActionTypeEntriesArray = this.getResources().getStringArray(R.array.dccExActionTypeEntries); // display version
        }
        final List<String> dccActionTypeValuesList = new ArrayList<>(Arrays.asList(dccExActionTypeEntryValuesArray));
        final List<String> dccActionTypeEntriesList = new ArrayList<>(Arrays.asList(dccExActionTypeEntriesArray));

        dccExActionTypeIndex = PROGRAMMING_TRACK;
        dcc_action_type_spinner = findViewById(R.id.ex_action_type_list);
//        ArrayAdapter<?> action_type_spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExActionTypeEntries, android.R.layout.simple_spinner_item);
        ArrayAdapter<?> action_type_spinner_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dccExActionTypeEntriesArray);
        action_type_spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dcc_action_type_spinner.setAdapter(action_type_spinner_adapter);
        dcc_action_type_spinner.setOnItemSelectedListener(new action_type_spinner_listener());
        dcc_action_type_spinner.setSelection(dccExActionTypeIndex);

        LinearLayout cv_programmer_button = findViewById(R.id.dccex_cv_programmer_prog_track_layout);
        dccex_navigation_button_listener navigation_button_listener = new dccex_navigation_button_listener(0);
        cv_programmer_button.setOnClickListener(navigation_button_listener);

        cv_programmer_button = findViewById(R.id.dccex_cv_programmer_pom_layout);
        navigation_button_listener = new dccex_navigation_button_listener(1);
        cv_programmer_button.setOnClickListener(navigation_button_listener);

        dexcProgrammingCommonCvsLayout = findViewById(R.id.ex_programmingCommonCvsLayout);
        dexcProgrammingAddressLayout = findViewById(R.id.ex_programmingAddressLayout);
        dexcProgrammingCvLayout = findViewById(R.id.ex_programmingCvLayout);
        dexcDccexTrackLinearLayout = findViewById(R.id.ex_DccexTrackLinearLayout);

        dccExTrackTypeEntryValuesArray = this.getResources().getStringArray(R.array.dccExTrackTypeEntryValues);
//        final List<String> dccTrackTypeValuesList = new ArrayList<>(Arrays.asList(dccExTrackTypeEntryValuesArray));
        dccExTrackTypeEntriesArray = this.getResources().getStringArray(R.array.dccExTrackTypeEntries); // display version
        float vn = 4;
        try {
            vn = Float.valueOf(mainapp.DccexVersion);
        } catch (Exception ignored) { } // invalid version
        if (vn <= 04.002068) {  // need to change the NONE to OFF in track manager
            dccExTrackTypeEntriesArray[0] = "OFF";
        }//        final List<String> dccTrackTypeEntriesList = new ArrayList<>(Arrays.asList(dccExTrackTypeEntriesArray));

        for (int i = 0; i< threaded_application.DCCEX_MAX_TRACKS; i++) {
            switch (i) {
                default:
                case 0:
                    dccExTrackTypeLayout[0] = findViewById(R.id.ex_DccexTrack0layout);
                    dccExTrackTypeSpinner[0] = findViewById(R.id.ex_track_type_0_list);
                    dccExTrackTypeIdEditText[0] = findViewById(R.id.ex_track_0_value);
                    break;
                case 1:
                    dccExTrackTypeLayout[1] = findViewById(R.id.ex_DccexTrack1layout);
                    dccExTrackTypeSpinner[1] = findViewById(R.id.ex_track_type_1_list);
                    dccExTrackTypeIdEditText[1] = findViewById(R.id.ex_track_1_value);
                    break;
                case 2:
                    dccExTrackTypeLayout[2] = findViewById(R.id.ex_DccexTrack2layout);
                    dccExTrackTypeSpinner[2] = findViewById(R.id.ex_track_type_2_list);
                    dccExTrackTypeIdEditText[2] = findViewById(R.id.ex_track_2_value);
                    break;
                case 3:
                    dccExTrackTypeLayout[3] = findViewById(R.id.ex_DccexTrack3layout);
                    dccExTrackTypeSpinner[3] = findViewById(R.id.ex_track_type_3_list);
                    dccExTrackTypeIdEditText[3] = findViewById(R.id.ex_track_3_value);
                    break;
                case 4:
                    dccExTrackTypeLayout[4] = findViewById(R.id.ex_DccexTrack4layout);
                    dccExTrackTypeSpinner[4] = findViewById(R.id.ex_track_type_4_list);
                    dccExTrackTypeIdEditText[4] = findViewById(R.id.ex_track_4_value);
                    break;
                case 5:
                    dccExTrackTypeLayout[5] = findViewById(R.id.ex_DccexTrack5layout);
                    dccExTrackTypeSpinner[5] = findViewById(R.id.ex_track_type_5_list);
                    dccExTrackTypeIdEditText[5] = findViewById(R.id.ex_track_5_value);
                    break;
                case 6:
                    dccExTrackTypeLayout[6] = findViewById(R.id.ex_DccexTrack6layout);
                    dccExTrackTypeSpinner[6] = findViewById(R.id.ex_track_type_6_list);
                    dccExTrackTypeIdEditText[6] = findViewById(R.id.ex_track_6_value);
                    break;
                case 7:
                    dccExTrackTypeLayout[7] = findViewById(R.id.ex_DccexTrack7layout);
                    dccExTrackTypeSpinner[7] = findViewById(R.id.ex_track_type_7_list);
                    dccExTrackTypeIdEditText[7] = findViewById(R.id.ex_track_7_value);
                    break;
            }
            ArrayAdapter<?> track_type_spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExTrackTypeEntries, android.R.layout.simple_spinner_item);
            track_type_spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dccExTrackTypeSpinner[i].setAdapter(track_type_spinner_adapter);
            dccExTrackTypeSpinner[i].setOnItemSelectedListener(new track_type_spinner_listener(dccExTrackTypeSpinner[i], i));
            dccExTrackTypeSpinner[i].setSelection(dccExTrackTypeIndex[i]);

            writeTracksButton = findViewById(R.id.ex_DccexWriteTracksButton);
            write_tracks_button_listener writeTracksClickListener = new write_tracks_button_listener();
            writeTracksButton.setOnClickListener(writeTracksClickListener);

            DccexResponsesScrollView = findViewById(R.id.ex_DccexResponsesScrollView);
            DccexSendsScrollView = findViewById(R.id.ex_DccexSendsScrollView);

            clearCommandsButton = findViewById(R.id.ex_dccexClearCommandsButton);
            clear_commands_button_listener clearCommandsClickListener = new clear_commands_button_listener();
            clearCommandsButton.setOnClickListener(clearCommandsClickListener);

//            hideSendsButton = findViewById(R.id.ex_dccexHideSendsButton);
//            hide_sends_button_listener hideSendsClickListener = new hide_sends_button_listener();
//            hideSendsButton.setOnClickListener(hideSendsClickListener);

        }
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");

        screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        statusLine = (LinearLayout) findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

    } // end onCreate

    @Override
    public void onResume() {
        Log.d("EX_Toolbox", "cv_programmer.onResume() called");
        mainapp.applyTheme(this);
        super.onResume();

        mainapp.getCommonPreferences();

        setActivityTitle();
        mainapp.dccexScreenIsOpen = true;
        mainapp.activeScreen = mainapp.ACTIVE_SCREEN_CV_PROGRAMMER;
        refreshDccexView();
        refreshDccexTracksView();

        if (mainapp.isForcingFinish()) {    //expedite
            this.finish();
            return;
        }

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.TIME_CHANGED);    // request time update
        CookieSyncManager.getInstance().startSync();

        // enable swipe/fling detection if enabled in Prefs
        ov = findViewById(R.id.cv_programmer_overlay);
        ov.addOnGestureListener(this);
        ov.setEventsInterceptionEnabled(true);
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    @Override
    public void onPause() {
        Log.d("EX_Toolbox", "cv_programmer.onPause() called");
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("EX_Toolbox", "cv_programmer.onStart() called");
        // put pointer to this activity's handler in main app's shared variable
        if (mainapp.cv_programmer_msg_handler == null)
            mainapp.cv_programmer_msg_handler = new cv_programmer_handler(Looper.getMainLooper());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (!mainapp.setActivityOrientation(this)) {   //set screen orientation based on prefs
            Intent in = mainapp.getCvProgrammerIntent();
            in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
            startActivity(in);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("EX_Toolbox", "cv_programmer.onDestroy() called");

        mainapp.hideSoftKeyboard(this.getCurrentFocus());
        mainapp.dccexScreenIsOpen = false;

        if (!isRestarting) {
            removeHandlers();
        }
        else {
            isRestarting = false;
        }

        if (mainapp.cv_programmer_msg_handler !=null) {
            mainapp.cv_programmer_msg_handler.removeCallbacksAndMessages(null);
            mainapp.cv_programmer_msg_handler = null;
        } else {
            Log.d("EX_Toolbox", "onDestroy: mainapp.web_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    }

    private void removeHandlers() {
        if (mainapp.cv_programmer_msg_handler != null) {
            mainapp.cv_programmer_msg_handler.removeCallbacks(gestureStopped);
            mainapp.cv_programmer_msg_handler.removeCallbacksAndMessages(null);
            mainapp.cv_programmer_msg_handler = null;
        } else {
            Log.d("EX_Toolbox", "onDestroy: mainapp.throttle_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    }



    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    protected void onRestoreInstanceState(@NonNull Bundle state) {
        super.onRestoreInstanceState(state);
    }

    //Handle pressing of the back button to end this activity
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK) {
            if (mainapp.cv_programmer_msg_handler!=null) {
                mainapp.checkExit(this);
            } else { // something has gone wrong and the activity did not shut down properly so force it
                disconnect();
            }
            return (true); // stop processing this key
        }
        mainapp.exitDoubleBackButtonInitiated = 0;
        return (super.onKeyDown(key, event));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.cv_programmer_menu, menu);
        tMenu = menu;

        mainapp.setTrackmanagerMenuOption(menu);
        mainapp.setCurrentsMenuOption(menu);

        mainapp.displayToolbarMenuButtons(menu);
        mainapp.displayPowerStateMenuButton(menu);
        mainapp.setPowerMenuOption(menu);
        mainapp.setPowerStateButton(menu);

        mainapp.setPowerMenuOption(menu);

        return  super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        Intent in;
        if ( (item.getItemId() == R.id.cv_programmer_mnu) || (item.getItemId() == R.id.toolbar_button_cv_programmer) ) {
            navigateAway(true, null);
            in = new Intent().setClass(this, cv_programmer.class);
            startACoreActivity(this, in, false, 0);
            return true;
        } else if ( (item.getItemId() == R.id.speed_matching_mnu) || (item.getItemId() == R.id.toolbar_button_speed_matching) ) {
            navigateAway(true, null);
            in = new Intent().setClass(this, speed_matching.class);
            startACoreActivity(this, in, false, 0);
            return true;
        } else if ( (item.getItemId() == R.id.servos_mnu) || (item.getItemId() == R.id.toolbar_button_servos) ) {
            navigateAway(true, null);
            in = new Intent().setClass(this, servos.class);
            startACoreActivity(this, in, false, 0);
            return true;
        } else if ( (item.getItemId() == R.id.track_manager_mnu) || (item.getItemId() == R.id.toolbar_button_track_manager) ) {
            navigateAway(true, null);
            in = new Intent().setClass(this, track_manager.class);
            startACoreActivity(this, in, false, 0);
            return true;
        } else if ( (item.getItemId() == R.id.currents_mnu) || (item.getItemId() == R.id.toolbar_button_currents) ) {
            navigateAway(true, null);
            in = new Intent().setClass(this, currents.class);
            startACoreActivity(this, in, false, 0);
            return true;
        } else if ( (item.getItemId() == R.id.sensors_mnu) || (item.getItemId() == R.id.toolbar_button_sensors) ) {
            navigateAway(true, null);
            in = new Intent().setClass(this, sensors.class);
            startACoreActivity(this, in, false, 0);
            return true;
        } else if ( (item.getItemId() == R.id.locos_mnu) || (item.getItemId() == R.id.toolbar_button_locos) ) {
            navigateAway(true, null);
            in = new Intent().setClass(this, locos.class);
            startACoreActivity(this, in, false, 0);
            return true;
        } else if ( (item.getItemId() == R.id.roster_mnu) || (item.getItemId() == R.id.toolbar_button_roster) ) {
            navigateAway(true, null);
            in = new Intent().setClass(this, roster.class);
            startACoreActivity(this, in, false, 0);
            return true;

        } else if (item.getItemId() == R.id.exit_mnu) {
            mainapp.checkAskExit(this);
            return true;
        } else if (item.getItemId() == R.id.power_control_mnu) {
            navigateAway(false, power_control.class);
            return true;
        } else if (item.getItemId() == R.id.settings_mnu) {
            in = new Intent().setClass(this, SettingsActivity.class);
            startActivityForResult(in, 0);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (item.getItemId() == R.id.logviewer_menu) {
            navigateAway(false, LogViewerActivity.class);
            return true;
        } else if (item.getItemId() == R.id.about_mnu) {
            navigateAway(false, about_page.class);
            return true;
        } else if (item.getItemId() == R.id.power_layout_button) {
            if (!mainapp.isPowerControlAllowed()) {
                mainapp.powerControlNotAllowedDialog(tMenu);
            } else {
                mainapp.powerStateMenuButton();
            }
            mainapp.buttonVibration();
            return true;
        } else {
                return super.onOptionsItemSelected(item);
        }
    }

    //handle return from menu items
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    // helper methods to handle navigating away from this activity
    private void navigateAway() {
        this.finish();
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private void navigateAway(boolean returningToOtherActivity, Class activityClass) {
        Intent in;
        if (activityClass != null ) {
            in = new Intent().setClass(this, activityClass);
        } else {  // if null assume we want the throttle activity
            in = mainapp.getCvProgrammerIntent();
        }
        if (returningToOtherActivity) {                 // if not returning
            startACoreActivity(this, in, false, 0);
        } else {
            startActivityForResult(in, 0);
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        }
    }

    private void disconnect() {
        this.finish();
    }

    // helper app to initialize statics (in case GC has not run since app last shutdown)
    // call before instantiating any instances of class
    public static void initStatics() {
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    // common startActivity()
    // used for swipes for the main activities only - Throttle, Turnouts, Routs, Web
    void startACoreActivity(Activity activity, Intent in, boolean swipe, float deltaX) {
        if (activity != null && in != null) {
            in.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            ActivityOptions options;
            if (deltaX>0) {
                options = ActivityOptions.makeCustomAnimation(context, R.anim.push_right_in, R.anim.push_right_out);
            } else {
                options = ActivityOptions.makeCustomAnimation(context, R.anim.push_left_in, R.anim.push_left_out);
            }
            startActivity(in, options.toBundle());
//            overridePendingTransition(mainapp.getFadeIn(swipe, deltaX), mainapp.getFadeOut(swipe, deltaX));
        }
    }

    @SuppressLint("ApplySharedPref")
    public void forceRestartApp(int forcedRestartReason) {
        Log.d("EX-Toolbox", "cv_programmer.forceRestartApp() ");
        Message msg = Message.obtain();
        msg.what = message_type.RESTART_APP;
        msg.arg1 = forcedRestartReason;
        mainapp.comm_msg_handler.sendMessage(msg);
    }

    private void startNewCvProgrammerActivity() {
        // remove old handlers since the new Intent will have its own
        isRestarting = true;        // tell OnDestroy to skip removing handlers since it will run after the new Intent is created
        removeHandlers();

        //end current Intent then start the new Intent
        Intent newCvProgrammer = new Intent().setClass(this, cv_programmer.class);
        this.finish();
        startActivity(newCvProgrammer);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }


//**************************************************************************************

    public class read_address_button_listener implements View.OnClickListener {

        public void onClick(View v) {
            DccexInfoStr = "";
            resetTextField(WHICH_ADDRESS);
            mainapp.buttonVibration();
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_DECODER_ADDRESS, "*", -1);
            refreshDccexView();
            mainapp.hideSoftKeyboard(v);

        }
    }

    public class write_address_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DccexInfoStr = "";
            String addrStr = etDccexWriteAddressValue.getText().toString();
            try {
                Integer addr = Integer.decode(addrStr);
                if ((addr > 2) && (addr <= 10239)) {
                    dccexAddress = addr.toString();
                    mainapp.buttonVibration();
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_DECODER_ADDRESS, "", addr);
                } else {
                    resetTextField(WHICH_ADDRESS);
                }
            } catch (Exception e) {
                resetTextField(WHICH_ADDRESS);
            }
            refreshDccexView();
            mainapp.hideSoftKeyboard(v);

        }
    }

    public class read_cv_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DccexInfoStr = "";
            resetTextField(WHICH_CV_VALUE);
            String cvStr = etDccexCv.getText().toString();
            try {
                int cv = Integer.decode(cvStr);
                if (cv > 0) {
                    DccexCv = Integer.toString(cv);
                    mainapp.buttonVibration();
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_CV, "", cv);
                    refreshDccexView();
                }
            } catch (Exception e) {
                resetTextField(WHICH_CV);
            }
            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class write_cv_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DccexInfoStr = "";
            String cvStr = etDccexCv.getText().toString();
            String cvValueStr = etDccexCvValue.getText().toString();
            String addrStr = etDccexWriteAddressValue.getText().toString();
            if (dccExActionTypeIndex == PROGRAMMING_TRACK) {
                try {
                    Integer cv = Integer.decode(cvStr);
                    int cvValue = Integer.decode(cvValueStr);
                    if (cv > 0) {
                        DccexCv = cv.toString();
                        DccexCvValue = Integer.toString(cvValue);
                        mainapp.buttonVibration();
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_CV, cvValueStr, cv);
                    } else {
                        resetTextField(WHICH_ADDRESS);
                    }
                } catch (Exception e) {
                    resetTextField(WHICH_ADDRESS);
                }
            } else {
                try {
                    Integer cv = Integer.decode(cvStr);
                    int cvValue = Integer.decode(cvValueStr);
                    Integer addr = Integer.decode(addrStr);
                    if ((addr > 2) && (addr <= 9999) && (cv > 0) && (cvValue > 0)) {
                        dccexAddress = addr.toString();
                        DccexCv = cv.toString();
                        DccexCvValue = Integer.toString(cvValue);
                        mainapp.buttonVibration();
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_POM_CV, DccexCv + " " + DccexCvValue, addr);
                    } else {
                        resetTextField(WHICH_ADDRESS);
                    }
                } catch (Exception e) {
                    resetTextField(WHICH_ADDRESS);
                }
            }
            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class send_command_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DccexInfoStr = "";
            String cmdStr = etDccexSendCommandValue.getText().toString();
            if ((cmdStr.length() > 0) && (cmdStr.charAt(0) != '<')) {
                mainapp.buttonVibration();
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DCCEX_SEND_COMMAND, "<" + cmdStr + ">");

                if ((cmdStr.charAt(0) == '=') && (cmdStr.length() > 1)) // we don't get a response from a tracks command, so request an update
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");

                if ((mainapp.dccexPreviousCommandList.size() == 0) || !(mainapp.dccexPreviousCommandList.get(mainapp.dccexPreviousCommandList.size() - 1).equals(cmdStr))) {
                    mainapp.dccexPreviousCommandList.add(cmdStr);
                    if (mainapp.dccexPreviousCommandList.size() > 20) {
                        mainapp.dccexPreviousCommandList.remove(0);
                    }
                }
                mainapp.dccexPreviousCommandIndex = mainapp.dccexPreviousCommandList.size();
            }
            resetTextField(WHICH_COMMAND);
            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class previous_command_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DccexInfoStr = "";
            String cmdStr = etDccexSendCommandValue.getText().toString();
            if (mainapp.dccexPreviousCommandIndex > 0) {
                DccexSendCommandValue = mainapp.dccexPreviousCommandList.get(mainapp.dccexPreviousCommandIndex - 1);
                mainapp.dccexPreviousCommandIndex--;
            } else {
                DccexSendCommandValue = mainapp.dccexPreviousCommandList.get(mainapp.dccexPreviousCommandList.size() - 1);
                mainapp.dccexPreviousCommandIndex = mainapp.dccexPreviousCommandList.size() - 1;
            }
            etDccexSendCommandValue.setText(DccexSendCommandValue);

            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class next_command_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DccexInfoStr = "";
            String cmdStr = etDccexSendCommandValue.getText().toString();
            if (mainapp.dccexPreviousCommandIndex < mainapp.dccexPreviousCommandList.size() - 1) {
                DccexSendCommandValue = mainapp.dccexPreviousCommandList.get(mainapp.dccexPreviousCommandIndex + 1);
                mainapp.dccexPreviousCommandIndex++;
            } else {
                DccexSendCommandValue = mainapp.dccexPreviousCommandList.get(0);
                mainapp.dccexPreviousCommandIndex = 0;
            }
            etDccexSendCommandValue.setText(DccexSendCommandValue);

            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class write_tracks_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            Integer typeIndex;
            String type;
            Integer id;
            char trackLetter;

            for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
                if (mainapp.dccexTrackAvailable[i]) {
                    trackLetter = (char) ('A' + i);
                    typeIndex = dccExTrackTypeSpinner[i].getSelectedItemPosition();
                    type = TRACK_TYPES[typeIndex];
                    mainapp.dccexTrackType[i] = typeIndex;

                    if (!TRACK_TYPES_NEED_ID[typeIndex]) {
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK, trackLetter + " " + type, 0);
                    } else {
                        try {
                            id = Integer.parseInt(dccExTrackTypeIdEditText[i].getText().toString());
                            mainapp.dccexTrackId[i] = id.toString();
                            if (mainapp.dccexTrackType[i] != TRACK_TYPE_OFF_NONE_INDEX) {
                                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK, trackLetter + " " + type, id);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class clear_commands_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.DccexResponsesListHtml.clear();
            mainapp.dccexSendsListHtml.clear();
            mainapp.dccexResponsesStr = "";
            mainapp.dccexSendsStr = "";
            refreshDccexView();
        }
    }

    private void witRetry(String s) {
        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
        startActivity(in);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private void resetTextField(int which) {
        switch (which) {
            case WHICH_ADDRESS:
                dccexAddress = "";
                etDccexWriteAddressValue.setText("");
                break;
            case WHICH_CV:
                DccexCv = "";
                etDccexCv.setText("");
                break;
            case WHICH_CV_VALUE:
                DccexCvValue = "";
                etDccexCvValue.setText("");
                break;
            case WHICH_COMMAND:
                DccexSendCommandValue = "";
                etDccexSendCommandValue.setText("");
        }
    }

    private void readTextField(int which) {
        switch (which) {
            case WHICH_ADDRESS:
                dccexAddress = etDccexWriteAddressValue.getText().toString();
                break;
            case WHICH_CV:
                DccexCv = etDccexCv.getText().toString();
                break;
            case WHICH_CV_VALUE:
                DccexCvValue = etDccexCvValue.getText().toString();
                break;
            case WHICH_COMMAND:
                DccexSendCommandValue = etDccexSendCommandValue.getText().toString();
        }
    }

    private void showHideButtons() {
        if (dccExActionTypeIndex != TRACK_MANAGER) {
            dexcProgrammingCommonCvsLayout.setVisibility(View.VISIBLE);
            dccExCommonCvsSpinner.setVisibility(View.VISIBLE);

            dexcProgrammingAddressLayout.setVisibility(View.VISIBLE);
            dexcProgrammingCvLayout.setVisibility(View.VISIBLE);
            dexcDccexTrackLinearLayout.setVisibility(View.GONE);
            DccexWriteInfoLayout.setVisibility(View.VISIBLE);

            sendCommandButton.setEnabled(false);
            writeAddressButton.setEnabled(dccexAddress.length() != 0);
            readCvButton.setEnabled(DccexCv.length() != 0);
            if (dccExActionTypeIndex == PROGRAMMING_TRACK) {
                writeCvButton.setEnabled(((DccexCv.length() != 0) && (DccexCvValue.length() != 0)));
            } else {
                writeCvButton.setEnabled(((DccexCv.length() != 0) && (DccexCvValue.length() != 0) && (dccexAddress.length() != 0)));
            }
        } else {
            dexcProgrammingCommonCvsLayout.setVisibility(View.GONE);
            dccExCommonCvsSpinner.setVisibility(View.GONE);

            dexcProgrammingAddressLayout.setVisibility(View.GONE);
            dexcProgrammingCvLayout.setVisibility(View.GONE);
            DccexWriteInfoLayout.setVisibility(View.GONE);
            dexcDccexTrackLinearLayout.setVisibility(View.VISIBLE);

            for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
                dccExTrackTypeIdEditText[i].setVisibility(TRACK_TYPES_NEED_ID[dccExTrackTypeIndex[i]] ? View.VISIBLE : View.GONE);
            }
        }
        sendCommandButton.setEnabled((DccexSendCommandValue.length() != 0) && (DccexSendCommandValue.charAt(0) != '<'));
        previousCommandButton.setEnabled((mainapp.dccexPreviousCommandIndex >= 0));
        nextCommandButton.setEnabled((mainapp.dccexPreviousCommandIndex >= 0));
    }

    public void refreshDccexView() {
        try {
            etDccexWriteAddressValue.setText(dccexAddress);
            DccexWriteInfoLabel.setText(DccexInfoStr);
            etDccexCv.setText(DccexCv);
            etDccexCvValue.setText(DccexCvValue);
    //        etDccexSendCommandValue.setText(DccexSendCommandValue);

            if (dccExActionTypeIndex == PROGRAMMING_TRACK) {
                readAddressButton.setVisibility(View.VISIBLE);
                writeAddressButton.setVisibility(View.VISIBLE);
                readCvButton.setVisibility(View.VISIBLE);
            } else {
                readAddressButton.setVisibility(View.GONE);
                writeAddressButton.setVisibility(View.GONE);
                readCvButton.setVisibility(View.GONE);
            }

            refreshDccexCommandsView();

        } catch (Exception e) {
            Log.e("EX_Toolbox", "refreshDccexView: object not available on resume, yet");
        }

        showHideButtons();
    }

    public void refreshDccexCommandsView() {
        DccexResponsesLabel.setText(Html.fromHtml(mainapp.dccexResponsesStr));
        DccexSendsLabel.setText(Html.fromHtml(mainapp.dccexSendsStr));
    }

    public void refreshDccexTracksView() {

        for (int i = 0; i< threaded_application.DCCEX_MAX_TRACKS; i++) {
            dccExTrackTypeSpinner[i].setSelection(mainapp.dccexTrackType[i]);
            dccExTrackTypeIdEditText[i].setText(mainapp.dccexTrackId[i]);
            dccExTrackTypeLayout[i].setVisibility(mainapp.dccexTrackAvailable[i] ? View.VISIBLE : View.GONE);
        }
        showHideButtons();

    }

    public class spinner_listener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mainapp.exitDoubleBackButtonInitiated = 0;

            dccCvsIndex = dccExCommonCvsSpinner.getSelectedItemPosition();
            if (dccCvsIndex > 0) {
                DccexCv = dccCvsEntryValuesArray[dccCvsIndex];
                resetTextField(WHICH_CV_VALUE);
                etDccexCvValue.requestFocus();
            }
            dccCvsIndex = 0;
            dccExCommonCvsSpinner.setSelection(dccCvsIndex);
            DccexInfoStr = "";

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the soft keyboard to close
            }

            refreshDccexView();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    public class command_spinner_listener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mainapp.exitDoubleBackButtonInitiated = 0;

            dccCmdIndex = dccExCommonCommandsSpinner.getSelectedItemPosition();
            if (dccCmdIndex > 0) {
                DccexSendCommandValue = dccExCommonCommandsEntryValuesArray[dccCmdIndex];
                if (dccExCommonCommandsHasParametersArray[dccCmdIndex] >0)
                    DccexSendCommandValue = DccexSendCommandValue + " ";
                etDccexSendCommandValue.setText(DccexSendCommandValue);
                etDccexSendCommandValue.requestFocus();
                etDccexSendCommandValue.setSelection(DccexSendCommandValue.length());
            }
//            DccexInfoStr = "";
            if (dccCmdIndex != 0) {
                DccexInfoStr = dccExCommonCommandsAdditionalInfoArray[dccCmdIndex];
            }
            dccCmdIndex = 0;
            dccExCommonCommandsSpinner.setSelection(dccCmdIndex);

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the soft keyboard to close
            }

            refreshDccexView();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    public class action_type_spinner_listener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mainapp.exitDoubleBackButtonInitiated = 0;

            Spinner spinner = findViewById(R.id.ex_action_type_list);
            dccExActionTypeIndex = spinner.getSelectedItemPosition();
            resetTextField(WHICH_CV);
            resetTextField(WHICH_CV_VALUE);
            DccexInfoStr = "";

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the soft keyboard to close
            }

            dccCvsIndex = 0;
            dccExCommonCvsSpinner.setSelection(dccCvsIndex);

            refreshDccexView();
            refreshDccexTracksView();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    public class track_type_spinner_listener implements AdapterView.OnItemSelectedListener {
        Spinner mySpinner;
        int myIndex;

        track_type_spinner_listener(Spinner spinner, int index) {
            mySpinner = spinner;
            myIndex = index;
        }

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mainapp.exitDoubleBackButtonInitiated = 0;

            dccExTrackTypeIndex[myIndex] = mySpinner.getSelectedItemPosition();
            if (dccExTrackTypeIndex[myIndex] == TRACK_TYPE_DCC_PROG_INDEX) {
                for (int i=0; i<8; i++) {
                    if ( (dccExTrackTypeIndex[i] == TRACK_TYPE_DCC_PROG_INDEX) && (myIndex != i) ) { // only one prog allowed
                        dccExTrackTypeSpinner[i].setSelection(TRACK_TYPE_OFF_NONE_INDEX);
                    }
                }
            }
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the soft keyboard to close
            }

            refreshDccexView();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    void checkCv29(String cv, String cvValueStr) {
        if ( (cv.equals("29")) && (mainapp.activeScreen==mainapp.ACTIVE_SCREEN_CV_PROGRAMMER) ) {
            try {
                String rslt = "";
                int cvValue = Integer.parseInt(cvValueStr);
                if (mainapp.bitExtracted(cvValue,1,1)==0) {
                    cv29Direction = getApplicationContext().getResources().getString(R.string.cv29DirectionForward);
                } else {
                    cv29Direction = getApplicationContext().getResources().getString(R.string.cv29DirectionReverse);
                }
                rslt = rslt + cv29Direction + "<br />";

                if (mainapp.bitExtracted(cvValue,1,2)==0) {
                    cv29SpeedSteps = getApplicationContext().getResources().getString(R.string.cv29SpeedSteps14);
                } else {
                    cv29SpeedSteps = getApplicationContext().getResources().getString(R.string.cv29SpeedSteps28);
                }
                rslt = rslt + cv29SpeedSteps + "<br />";

                if (mainapp.bitExtracted(cvValue,1,3)==0) {
                    cv29AnalogueMode = getApplicationContext().getResources().getString(R.string.cv29AnalogueConversionOff);
                } else {
                    cv29AnalogueMode = getApplicationContext().getResources().getString(R.string.cv29AnalogueConversionOn);
                }
                rslt = rslt + cv29AnalogueMode + "<br />";

                // bit 4 is Railcom

                if (mainapp.bitExtracted(cvValue,1,5)==0) {
                    cv29SpeedTable = getApplicationContext().getResources().getString(R.string.cv29SpeedTableNo);
                } else {
                    cv29SpeedTable = getApplicationContext().getResources().getString(R.string.cv29SpeedTableYes);
                }
                rslt = rslt + cv29SpeedTable + "<br />";

                if (mainapp.bitExtracted(cvValue,1,6)==0) {
                    cv29AddressSize = getApplicationContext().getResources().getString(R.string.cv29AddressSize2bit);
                } else {
                    cv29AddressSize = getApplicationContext().getResources().getString(R.string.cv29AddressSize4bit);
                }
                rslt = rslt +  cv29AddressSize;

                mainapp.dccexResponsesStr = "<p>" + rslt + "</p>" + mainapp.dccexResponsesStr;

                mainapp.dccexResponsesStr = "<p>"
                        + String.format(getApplicationContext().getResources().getString(R.string.cv29SpeedToggleDirection),
                                        mainapp.toggleBit(cvValue,1) )
                        + "</p>" + mainapp.dccexResponsesStr;

                mainapp.dccexResponsesStr = "<p>"
                        + String.format(getApplicationContext().getResources().getString(R.string.cv29SpeedTableToggle),
                        mainapp.toggleBit(cvValue, 5))
                        + "</p>" + mainapp.dccexResponsesStr;

            } catch (Exception e) {
                Log.e("EX_Toolbox", "Error processing cv29: " + e.getMessage());
            }
        }
    }

    public class dccex_navigation_button_listener implements View.OnClickListener {
        int myIndex;

        dccex_navigation_button_listener(int index) {
            myIndex = index;
        }

        public void onClick(View v) {
            mainapp.buttonVibration();
            dcc_action_type_spinner.setSelection(myIndex);
            mainapp.hideSoftKeyboard(v);
        }
    }

}
