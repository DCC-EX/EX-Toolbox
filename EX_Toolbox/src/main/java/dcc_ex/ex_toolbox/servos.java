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
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.gesture.GestureOverlayView;
import android.graphics.Typeface;
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

import java.util.ArrayList;

import dcc_ex.ex_toolbox.logviewer.ui.LogViewerActivity;
import dcc_ex.ex_toolbox.type.message_type;
import dcc_ex.ex_toolbox.util.LocaleHelper;

public class servos extends AppCompatActivity implements GestureOverlayView.OnGestureListener {

    private threaded_application mainapp;  // hold pointer to mainapp
    private SharedPreferences prefs;

    private Menu tMenu;
    private static boolean savedMenuSelected;

    protected GestureOverlayView ov;
    // these are used for gesture tracking
    private float gestureStartX = 0;
    private float gestureStartY = 0;
    protected boolean gestureInProgress = false; // gesture is in progress
    private long gestureLastCheckTime; // time in milliseconds that velocity was last checked
    private static final long gestureCheckRate = 200; // rate in milliseconds to check velocity
    private VelocityTracker mVelocityTracker;

    protected Handler repeatUpdateHandler = new Handler();
    static int REP_DELAY = 5;

    //**************************************

    int activeServoButtonNo = 1;

    static final String SERVO_VPIN_DEFAULT = "100";
    static final String SERVO_THROWN_POSITION_DEFAULT = "409";
    static final String SERVO_MID_POSITION_DEFAULT = "307";
    static final String SERVO_CLOSED_POSITION_DEFAULT = "205";

    private String dccexServoVpin = SERVO_VPIN_DEFAULT;
    private Integer dccexServoVpinValue = Integer.parseInt(SERVO_VPIN_DEFAULT);
    private EditText etDccexServoVpinValue;

    private String dccexServoThrownPosition = SERVO_THROWN_POSITION_DEFAULT;
    private Integer dccexServoThrownPositionValue = Integer.parseInt(SERVO_THROWN_POSITION_DEFAULT);
    private EditText etDccexServoThrownPositionValue;
    private boolean autoIncrementThrownPosition = false;

    private String dccexServoMidPosition = SERVO_MID_POSITION_DEFAULT;
    private Integer dccexServoMidPositionValue = Integer.parseInt(SERVO_MID_POSITION_DEFAULT);
    private EditText etDccexServoMidPositionValue;
    private boolean autoIncrementMidPosition = false;

    private String dccexServoClosedPosition = SERVO_CLOSED_POSITION_DEFAULT;
    private Integer dccexServoClosedPositionValue = Integer.parseInt(SERVO_CLOSED_POSITION_DEFAULT);
    private EditText etDccexServoClosedPositionValue;

    private TextView etDccexServoExRailInstruction;

    private boolean [] autoIncrement = {false, false, false, false};
    private boolean [] autoDecrement = {false, false, false, false};

    Spinner dccExServoProfilesSpinner;
    Integer dccExServoProfilesIndex = 0;
    Integer dccExServoProfile = 0;

    String[] dccExServoProfilesEntryValuesArray;
    String[] dccExServoProfilesEntriesArray; // display version

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

//    ArrayList<String> DccexResponsesListHtml = new ArrayList<>();
//    ArrayList<String> dccexSendsListHtml = new ArrayList<>();

    private int dccCmdIndex = 0;
    String[] dccExCommonCommandsEntryValuesArray;
    String[] dccExCommonCommandsEntriesArray; // display version
    int[] dccExCommonCommandsHasParametersArray; // display version
    String[] dccExCommonCommandsAdditionalInfoArray;

    Button dccExServoThrowButton;
    Button dccExServoMidButton;
    Button dccExServoCloseButton;

    Button dccExServoPositionIncrementButton;
    Button dccExServoPositionDecrementButton;

    Button dccExServoPositionSwapButton;
    Button dccExServoResetButton;

    Button sendCommandButton;
    Button previousCommandButton;
    Button nextCommandButton;
    Button clearCommandsButton;

    Spinner dccExCommonCommandsSpinner;

//    private int dccexPreviousCommandIndex = -1;
//    ArrayList<String> dccexPreviousCommandList = new ArrayList<>();

    static final int WHICH_VPIN = 0;
    static final int WHICH_THROWN_POSITION = 1;
    static final int WHICH_MID_POSITION = 2;
    static final int WHICH_CLOSED_POSITION = 3;
    static final int WHICH_COMMAND = 4;

    static final int DELTA_INCREMENT = 1;
    static final int DELTA_DECREMENT = -1;
    static final int DELTA_ZERO = 0;

    Spinner dss_servosList;
    ArrayAdapter<String> servoSpinnerAdapter;
    public ArrayList<String> servoList;

    //**************************************


    private Toolbar toolbar;
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

        gestureInProgress = true;
        gestureLastCheckTime = event.getEventTime();
        mVelocityTracker.clear();

        // start the gesture timeout timer
        if (mainapp.servos_msg_handler != null)
            mainapp.servos_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
    }

    public void gestureMove(MotionEvent event) {
        // Log.d("EX_Toolbox", "gestureMove action " + event.getAction());
        if ( (mainapp != null) && (mainapp.servos_msg_handler != null) && (gestureInProgress) ) {
            // stop the gesture timeout timer
            mainapp.servos_msg_handler.removeCallbacks(gestureStopped);

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
                mainapp.servos_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
            }
        }
    }

    private void gestureEnd(MotionEvent event) {
        // Log.d("EX_Toolbox", "gestureEnd action " + event.getAction() + " inProgress? " + gestureInProgress);
        if ( (mainapp != null) && (mainapp.servos_msg_handler != null) && (gestureInProgress) ) {
            mainapp.servos_msg_handler.removeCallbacks(gestureStopped);

            float deltaX = (event.getX() - gestureStartX);
            float absDeltaX =  Math.abs(deltaX);
            if (absDeltaX > threaded_application.min_fling_distance) { // only process left/right swipes
                // valid gesture. Change the event action to CANCEL so that it isn't processed by any control below the gesture overlay
                event.setAction(MotionEvent.ACTION_CANCEL);
                // process swipe in the direction with the largest change
                Intent nextScreenIntent = mainapp.getNextIntentInSwipeSequence(threaded_application.SCREEN_SWIPE_INDEX_SERVOS, deltaX);
                startACoreActivity(this, nextScreenIntent, true, deltaX);
            } else {
                // gesture was not long enough
                gestureFailed(event);
            }
        }
    }

    private void gestureCancel(MotionEvent event) {
        if (mainapp.servos_msg_handler != null)
            mainapp.servos_msg_handler.removeCallbacks(gestureStopped);
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
    class servos_handler extends Handler {

        public servos_handler(Looper looper) {
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
                case message_type.RESTART_APP:
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
            mainapp.setToolbarTitle(toolbar,
                    "",
                    getApplicationContext().getResources().getString(R.string.app_name_servos_short),
                    mainapp.getFastClockTime());
        else
            mainapp.setToolbarTitle(toolbar,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_servos),
                    "");
    }

    private void witRetry(String s) {
        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
        startActivity(in);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * Called when the activity is first created.
     */
    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
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

        setContentView(R.layout.servos);

        mainapp.loadBackgroundImage(findViewById(R.id.servosBackgroundImgView));

        //put pointer to this activity's handler in main app's shared variable
        mainapp.servos_msg_handler = new servos_handler(Looper.getMainLooper());

        dccExServoThrowButton = findViewById(R.id.ex_DccexServoThrowButton);
        servo_button_listener servo_throw_click_listener = new servo_button_listener(WHICH_THROWN_POSITION);
        dccExServoThrowButton.setOnClickListener(servo_throw_click_listener);

        dccExServoMidButton = findViewById(R.id.ex_DccexServoMidButton);
        servo_button_listener servo_mid_click_listener = new servo_button_listener(WHICH_MID_POSITION);
        dccExServoMidButton.setOnClickListener(servo_mid_click_listener);

        dccExServoCloseButton = findViewById(R.id.ex_DccexServoCloseButton);
        servo_button_listener servo_close_click_listener = new servo_button_listener(WHICH_CLOSED_POSITION);
        dccExServoCloseButton.setOnClickListener(servo_close_click_listener);

        dccExServoPositionIncrementButton = findViewById(R.id.ex_DccexServoPositionIncrementButton);
        servo_position_increment_button_listener servo_position_increment_click_listener = new servo_position_increment_button_listener();
        dccExServoPositionIncrementButton.setOnClickListener(servo_position_increment_click_listener);
        dccExServoPositionIncrementButton.setOnLongClickListener(servo_position_increment_click_listener);
        dccExServoPositionIncrementButton.setOnTouchListener(servo_position_increment_click_listener);

        dccExServoPositionDecrementButton = findViewById(R.id.ex_DccexServoPositionDecrementButton);
        servo_position_decrement_button_listener servo_position_decrement_click_listener = new servo_position_decrement_button_listener();
        dccExServoPositionDecrementButton.setOnClickListener(servo_position_decrement_click_listener);
        dccExServoPositionDecrementButton.setOnLongClickListener(servo_position_decrement_click_listener);
        dccExServoPositionDecrementButton.setOnTouchListener(servo_position_decrement_click_listener);

        dccExServoPositionSwapButton = findViewById(R.id.ex_DccexServoPositionSwapButton);
        servo_position_swap_button_listener servo_position_swap_click_listener = new servo_position_swap_button_listener();
        dccExServoPositionSwapButton.setOnClickListener(servo_position_swap_click_listener);

        dccExServoResetButton = findViewById(R.id.ex_DccexServoResetButton);
        servo_reset_button_listener servo_reset_click_listener = new servo_reset_button_listener();
        dccExServoResetButton.setOnClickListener(servo_reset_click_listener);

        //-----------------------------------------

        etDccexServoVpinValue = findViewById(R.id.ex_DccexServoVpinValue);
        etDccexServoVpinValue.setText(SERVO_VPIN_DEFAULT);
        etDccexServoVpinValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_VPIN); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        etDccexServoClosedPositionValue = findViewById(R.id.ex_DccexServoClosedPositionValue);
        etDccexServoClosedPositionValue.setText(SERVO_CLOSED_POSITION_DEFAULT);
        etDccexServoClosedPositionValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_CLOSED_POSITION); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        etDccexServoMidPositionValue = findViewById(R.id.ex_DccexServoMidPositionValue);
        etDccexServoMidPositionValue.setText(SERVO_MID_POSITION_DEFAULT);
        etDccexServoMidPositionValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_MID_POSITION); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        etDccexServoThrownPositionValue = findViewById(R.id.ex_DccexServoThrownPositionValue);
        etDccexServoThrownPositionValue.setText(SERVO_THROWN_POSITION_DEFAULT);
        etDccexServoThrownPositionValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_THROWN_POSITION); showHideButtons();  }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        etDccexServoExRailInstruction = findViewById(R.id.ex_DccexServoExRailInstruction);
        etDccexServoExRailInstruction.setText("");

        //-----------------------------------------

        dccExServoProfilesEntryValuesArray = this.getResources().getStringArray(R.array.dccExServoProfilesEntryValues);
        dccExServoProfilesEntriesArray = this.getResources().getStringArray(R.array.dccExServoProfilesEntries);

        dccCmdIndex=0;
        dccExServoProfilesSpinner = findViewById(R.id.ex_servoProfilesList);
        ArrayAdapter<?> spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExServoProfilesEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccExServoProfilesSpinner.setAdapter(spinner_adapter);
        dccExServoProfilesSpinner.setOnItemSelectedListener(new servo_profiles_spinner_listener());
        dccExServoProfilesSpinner.setSelection(3);

        //-----------------------------------------

        dccExCommonCommandsEntryValuesArray = this.getResources().getStringArray(R.array.dccExCommonCommandsEntryValues);
        dccExCommonCommandsEntriesArray = this.getResources().getStringArray(R.array.dccExCommonCommandsEntries); // display version
        dccExCommonCommandsHasParametersArray = this.getResources().getIntArray(R.array.dccExCommonCommandsHasParameters);
        dccExCommonCommandsAdditionalInfoArray = this.getResources().getStringArray(R.array.dccExCommonCommandsAdditionalInfo);

        dccCmdIndex=0;
        dccExCommonCommandsSpinner = findViewById(R.id.ex_common_commands_list);
        spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExCommonCommandsEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccExCommonCommandsSpinner.setAdapter(spinner_adapter);
        dccExCommonCommandsSpinner.setOnItemSelectedListener(new command_spinner_listener());
        dccExCommonCommandsSpinner.setSelection(dccCmdIndex);

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

        clearCommandsButton = findViewById(R.id.ex_dccexClearCommandsButton);
        clear_commands_button_listener clearCommandsClickListener = new clear_commands_button_listener();
        clearCommandsButton.setOnClickListener(clearCommandsClickListener);



        // Set the options for the saved servos
        mainapp.importExport.initialiseServoList();
        mainapp.importExport.readServoListFromFile();

        servoList = new ArrayList<>(mainapp.importExport.servoList);
        dss_servosList = findViewById(R.id.ex_ServosList);
        servoSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, servoList);
        servoSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dss_servosList.setAdapter(servoSpinnerAdapter);
        dss_servosList.setOnItemSelectedListener(new servoSpinnerListener());
        reloadServosSpinner();

        //-----------------------------------------

        mainapp.getCommonPreferences();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

    } // end onCreate

    @Override
    public void onResume() {
        Log.d("EX_Toolbox", "servos.onResume() called");
        mainapp.applyTheme(this);

        super.onResume();

        mainapp.getCommonPreferences();

        setActivityTitle();
        mainapp.activeScreen = mainapp.ACTIVE_SCREEN_SERVOS;
        mainapp.dccexScreenIsOpen = true;
        refreshDccexView();
        setActivateServoButtons(activeServoButtonNo);

        if (mainapp.isForcingFinish()) {    //expedite
            this.finish();
            return;
        }

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.TIME_CHANGED);    // request time update
        CookieSyncManager.getInstance().startSync();

        // enable swipe/fling detection if enabled in Prefs
        ov = findViewById(R.id.servos_overlay);
        ov.addOnGestureListener(this);
        ov.setEventsInterceptionEnabled(true);
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }

        autoIncrement[WHICH_CLOSED_POSITION] = false;
        autoIncrement[WHICH_MID_POSITION] = false;
        autoIncrement[WHICH_THROWN_POSITION] = false;
    }

    @Override
    public void onPause() {
        Log.d("EX_Toolbox", "servos.onPause() called");
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("EX_Toolbox", "servos.onStart() called");
        // put pointer to this activity's handler in main app's shared variable
        if (mainapp.servos_msg_handler == null)
            mainapp.servos_msg_handler = new servos_handler(Looper.getMainLooper());
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
        Log.d("EX_Toolbox", "servos.onDestroy() called");

        if (mainapp.servos_msg_handler !=null) {
            mainapp.servos_msg_handler.removeCallbacksAndMessages(null);
            mainapp.servos_msg_handler = null;
        } else {
            Log.d("EX_Toolbox", "onDestroy: mainapp.web_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    protected void onRestoreInstanceState(@NonNull Bundle state) {
        super.onRestoreInstanceState(state);
    }

//    public class close_button_listener implements View.OnClickListener {
//        public void onClick(View v) {
//            navigateAway();
//            mainapp.buttonVibration();
//        }
//    }

    //Handle pressing of the back button to end this activity
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK) {
            if (mainapp.servos_msg_handler!=null) {
                mainapp.checkExit(this);
            } else { // something has gone wrong and the activity did not shut down properly so force it
                disconnect();
            }
            return (true); // stop processing this key
        }
        return (super.onKeyDown(key, event));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.servos_menu, menu);
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
            mainapp.checkExit(this);
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
        } else {  // if null assume we want the CV Programmer activity
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
        Log.d("EX-Toolbox", "servos.forceRestartApp() ");
        Message msg = Message.obtain();
        msg.what = message_type.RESTART_APP;
        msg.arg1 = forcedRestartReason;
        mainapp.comm_msg_handler.sendMessage(msg);
    }


//**************************************************************************************

    void showHideButtons() {
        boolean enable = true;
        if ( (dccexServoVpin.length()==0) || (dccexServoThrownPosition.length()==0) || (dccexServoMidPosition.length()==0) || (dccexServoClosedPosition.length()==0) ) {
            enable = false;
        }
        dccExServoCloseButton.setEnabled(enable);
        dccExServoMidButton.setEnabled(enable);
        dccExServoThrowButton.setEnabled(enable);
        dccExServoPositionDecrementButton.setEnabled(enable);
        dccExServoPositionIncrementButton.setEnabled(enable);

        sendCommandButton.setEnabled((DccexSendCommandValue.length() != 0) && (DccexSendCommandValue.charAt(0) != '<'));
        previousCommandButton.setEnabled((mainapp.dccexPreviousCommandIndex >= 0));
        nextCommandButton.setEnabled((mainapp.dccexPreviousCommandIndex >= 0));
    }


    int getIntFromString(String str) {
        int result = 0;
        try {
            result = Integer.parseInt(str);
        } catch (Exception ignored) {}
        return result;
    }

//    int getMidPosition() {
//        int result;
//        if (dccexServoThrownPositionValue > dccexServoClosedPositionValue) {
//            result = (dccexServoThrownPositionValue - dccexServoClosedPositionValue)/2 + dccexServoClosedPositionValue;
//        } else if(dccexServoThrownPositionValue < dccexServoClosedPositionValue) {
//            result = (dccexServoClosedPositionValue - dccexServoThrownPositionValue)/2 + dccexServoThrownPositionValue;
//        } else { // =
//            result = dccexServoThrownPositionValue;
//        }
//        return result;
//    }

    void setActivateServoButton(Button btn, boolean active) {
        if (active) {
            btn.setHovered(true);
            btn.setSelected(true);
            btn.setActivated(true);
            btn.setTypeface(null, Typeface.ITALIC + Typeface.BOLD);
        } else {
            btn.setHovered(false);
            btn.setSelected(false);
            btn.setActivated(false);
            btn.setTypeface(null, Typeface.NORMAL);
        }
    }

    void setActivateServoButtons(int buttonNo) {
        activeServoButtonNo = buttonNo;
        switch (buttonNo) {
            default:
            case WHICH_CLOSED_POSITION:
                setActivateServoButton(dccExServoCloseButton, true);
                setActivateServoButton(dccExServoMidButton, false);
                setActivateServoButton(dccExServoThrowButton, false);
                break;
            case WHICH_MID_POSITION:
                setActivateServoButton(dccExServoCloseButton, false);
                setActivateServoButton(dccExServoMidButton, true);
                setActivateServoButton(dccExServoThrowButton, false);                break;
            case WHICH_THROWN_POSITION:
                setActivateServoButton(dccExServoCloseButton, false);
                setActivateServoButton(dccExServoMidButton, false);
                setActivateServoButton(dccExServoThrowButton, true);
                break;
        }
    }

    private void setTextSelection(int buttonNo) {
        switch (buttonNo) {
            default:
            case WHICH_CLOSED_POSITION:
                etDccexServoClosedPositionValue.requestFocus();
                etDccexServoClosedPositionValue.setSelection(dccexServoClosedPosition.length());
//                    msgTxt = String.format("%s %d %d", dccexServoVpin, dccexServoClosedPositionValue, dccExServoProfile);
                break;
            case WHICH_MID_POSITION:
                etDccexServoMidPositionValue.requestFocus();
                etDccexServoMidPositionValue.setSelection(dccexServoMidPosition.length());
//                    msgTxt = String.format("%s %d %d", dccexServoVpin, dccexServoMidPositionValue, dccExServoProfile);
                break;
            case WHICH_THROWN_POSITION:
                etDccexServoThrownPositionValue.requestFocus();
                etDccexServoThrownPositionValue.setSelection(dccexServoThrownPosition.length());
//                    msgTxt = String.format("%s %d %d", dccexServoVpin, dccexServoThrownPositionValue, dccExServoProfile);
                break;
        }
    }

    private class servo_button_listener implements View.OnClickListener {
        int buttonNo;

        protected servo_button_listener(int bNo) {
            buttonNo = bNo;
        }

        public void onClick(View v) {
            String msgTxt;
            setActivateServoButtons(buttonNo);
            DccexInfoStr = "";
            mainapp.buttonVibration();
            sendServoPosition(buttonNo, DELTA_ZERO);
            refreshDccexView();
            mainapp.hideSoftKeyboard(v);

            setTextSelection(buttonNo);

//            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.MOVE_SERVO, msgTxt, 0);
        }
    }

    @SuppressLint("DefaultLocale")
    void sendServoPosition(int which, int delta) {
        String msgTxt;

        switch (which) {
            case WHICH_THROWN_POSITION:
                dccexServoThrownPositionValue = dccexServoThrownPositionValue + delta;
                dccexServoThrownPosition = Integer.toString(dccexServoThrownPositionValue);
                etDccexServoThrownPositionValue.setText(dccexServoThrownPosition);
                setExRailInstruction();
                msgTxt = String.format("%s %d %d", dccexServoVpin, dccexServoThrownPositionValue, dccExServoProfile);
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.MOVE_SERVO, msgTxt, 0);
//                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.MOVE_SERVO, dccexServoVpin, dccexServoThrownPositionValue);
                break;
            case WHICH_MID_POSITION:
                dccexServoMidPositionValue = dccexServoMidPositionValue + delta;
                dccexServoMidPosition = Integer.toString(dccexServoMidPositionValue);
                etDccexServoMidPositionValue.setText(dccexServoMidPosition);
                setExRailInstruction();
                msgTxt = String.format("%s %d %d", dccexServoVpin, dccexServoMidPositionValue, dccExServoProfile);
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.MOVE_SERVO, msgTxt, 0);
//              mainapp.sendMsg(mainapp.comm_msg_handler, message_type.MOVE_SERVO, dccexServoVpin, dccexServoMidPositionValue);
                break;
            case WHICH_CLOSED_POSITION:
                dccexServoClosedPositionValue = dccexServoClosedPositionValue + delta;
                dccexServoClosedPosition = Integer.toString(dccexServoClosedPositionValue);
                etDccexServoClosedPositionValue.setText(dccexServoClosedPosition);
                setExRailInstruction();
                msgTxt = String.format("%s %d %d", dccexServoVpin, dccexServoClosedPositionValue, dccExServoProfile);
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.MOVE_SERVO, msgTxt, 0);
//              mainapp.sendMsg(mainapp.comm_msg_handler, message_type.MOVE_SERVO, dccexServoVpin, dccexServoClosedPositionValue);
                break;
        }
        setTextSelection(which);
        reloadServosSpinner();
    }

    private void reloadServosSpinner() {
//        mainapp.importExport.convertServoListToArray();
        mainapp.importExport.updateServoList(dccexServoVpinValue, dccexServoClosedPositionValue,dccexServoThrownPositionValue,dccexServoThrownPositionValue, dccExServoProfile);
        mainapp.importExport.writeServoListToFile(prefs);
//        servoSpinnerAdapter.clear();
        for (int i = servoSpinnerAdapter.getCount()-1; i >= 0; i--) {
            servoSpinnerAdapter.remove(servoSpinnerAdapter.getItem(i));
        }
        servoSpinnerAdapter.addAll(mainapp.importExport.servoList);
        servoSpinnerAdapter.notifyDataSetChanged();
        dss_servosList.setSelection(0);
    }

    private class servo_position_increment_button_listener implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {
        public void onClick(View v) {
            mainapp.buttonVibration();
            sendServoPosition(activeServoButtonNo, DELTA_INCREMENT);
            mainapp.hideSoftKeyboard(v);
        }

        @Override
        public boolean onLongClick(View v) {
            autoIncrement[activeServoButtonNo] = true;
            repeatUpdateHandler.post(new RptUpdater(activeServoButtonNo, 0) );
            return false;
        }

        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouch(View v, MotionEvent event) {
            if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                    && autoIncrement[activeServoButtonNo]) {
                autoIncrement[activeServoButtonNo] = false;
            }
            return false;
        }
    }

    private class servo_position_decrement_button_listener implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {
        public void onClick(View v) {
            mainapp.buttonVibration();
            sendServoPosition(activeServoButtonNo, DELTA_DECREMENT);
            mainapp.hideSoftKeyboard(v);
        }

        @Override
        public boolean onLongClick(View v) {
            autoDecrement[activeServoButtonNo] = true;
            repeatUpdateHandler.post(new RptUpdater(activeServoButtonNo, 0) );
            return false;
        }

        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouch(View v, MotionEvent event) {
            if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                    && autoDecrement[activeServoButtonNo]) {
                autoDecrement[activeServoButtonNo] = false;
            }
            return false;
        }
    }

    private class servo_position_swap_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.buttonVibration();

            String tempStr = dccexServoThrownPosition;
            int tempInt = dccexServoThrownPositionValue;
            dccexServoThrownPositionValue = dccexServoClosedPositionValue;
            dccexServoThrownPosition = dccexServoClosedPosition;
            dccexServoClosedPositionValue = tempInt;
            dccexServoClosedPosition = tempStr;

            etDccexServoClosedPositionValue.setText(dccexServoClosedPosition);
            etDccexServoThrownPositionValue.setText(dccexServoThrownPosition);

            setExRailInstruction();

            mainapp.hideSoftKeyboard(v);
        }
    }

    private void resetServo() {
        dccexServoVpin = SERVO_VPIN_DEFAULT;
        dccexServoVpinValue = Integer.parseInt(SERVO_VPIN_DEFAULT);
        dccexServoThrownPosition = SERVO_THROWN_POSITION_DEFAULT;
        dccexServoThrownPositionValue = Integer.parseInt(SERVO_THROWN_POSITION_DEFAULT);
        dccexServoMidPosition = SERVO_MID_POSITION_DEFAULT;
        dccexServoMidPositionValue = Integer.parseInt(SERVO_MID_POSITION_DEFAULT);
        dccexServoClosedPosition = SERVO_CLOSED_POSITION_DEFAULT;
        dccexServoClosedPositionValue = Integer.parseInt(SERVO_CLOSED_POSITION_DEFAULT);

        etDccexServoVpinValue.setText(dccexServoVpin);
        etDccexServoClosedPositionValue.setText(dccexServoClosedPosition);
        etDccexServoMidPositionValue.setText(dccexServoMidPosition);
        etDccexServoThrownPositionValue.setText(dccexServoThrownPosition);

        setExRailInstruction();
    }

    private class servo_reset_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.buttonVibration();
            resetServo();
            mainapp.hideSoftKeyboard(v);
        }
    }

        // ------------------------------------


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

    public class clear_commands_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.DccexResponsesListHtml.clear();
            mainapp.dccexSendsListHtml.clear();
            mainapp.dccexResponsesStr = "";
            mainapp.dccexSendsStr = "";
            refreshDccexView();
        }
    }



    //********************************************************************

    private void resetTextField(int which) {
        switch (which) {
            case WHICH_VPIN:
                dccexServoVpin = SERVO_VPIN_DEFAULT;
                dccexServoVpinValue = Integer.parseInt(SERVO_VPIN_DEFAULT);
                etDccexServoVpinValue.setText(SERVO_VPIN_DEFAULT);
                setExRailInstruction();
                break;
            case WHICH_THROWN_POSITION:
                dccexServoThrownPosition = SERVO_THROWN_POSITION_DEFAULT;
                dccexServoThrownPositionValue = Integer.parseInt(SERVO_THROWN_POSITION_DEFAULT);
                etDccexServoThrownPositionValue.setText(SERVO_THROWN_POSITION_DEFAULT);
                setExRailInstruction();
                break;
            case WHICH_MID_POSITION:
                dccexServoMidPosition = SERVO_MID_POSITION_DEFAULT;
                dccexServoMidPositionValue = Integer.parseInt(SERVO_MID_POSITION_DEFAULT);
                etDccexServoMidPositionValue.setText(SERVO_MID_POSITION_DEFAULT);
                setExRailInstruction();
                break;
            case WHICH_CLOSED_POSITION:
                dccexServoClosedPosition = SERVO_CLOSED_POSITION_DEFAULT;
                dccexServoClosedPositionValue = Integer.parseInt(SERVO_CLOSED_POSITION_DEFAULT);
                etDccexServoClosedPositionValue.setText(SERVO_CLOSED_POSITION_DEFAULT);
                setExRailInstruction();
                break;
            case WHICH_COMMAND:
                DccexSendCommandValue = "";
                etDccexSendCommandValue.setText("");
        }
    }

    private void setExRailInstruction() {
        etDccexServoExRailInstruction.setText(String.format( getApplicationContext().getString(R.string.dccexServoExRailInstruction),
                dccexServoVpinValue,dccexServoThrownPositionValue,dccexServoClosedPositionValue,dccExServoProfilesEntriesArray[dccExServoProfile]));

    }

    private void readTextField(int which) {
        switch (which) {
            case WHICH_VPIN:
                dccexServoVpin = etDccexServoVpinValue.getText().toString();
                dccexServoVpinValue = getIntFromString(dccexServoVpin);
                break;
            case WHICH_THROWN_POSITION:
                dccexServoThrownPosition = etDccexServoThrownPositionValue.getText().toString();
                dccexServoThrownPositionValue = getIntFromString(dccexServoThrownPosition);
                break;
            case WHICH_MID_POSITION:
                dccexServoMidPosition = etDccexServoMidPositionValue.getText().toString();
                dccexServoMidPositionValue = getIntFromString(dccexServoMidPosition);
                break;
            case WHICH_CLOSED_POSITION:
                dccexServoClosedPosition = etDccexServoClosedPositionValue.getText().toString();
                dccexServoClosedPositionValue = getIntFromString(dccexServoClosedPosition);
                break;
            case WHICH_COMMAND:
                DccexSendCommandValue = etDccexSendCommandValue.getText().toString();
                break;
        }
        setActivateServoButtons(which);
    }


    //********************************************************************

    public void refreshDccexView() {
        DccexWriteInfoLabel.setText(DccexInfoStr);
        refreshDccexCommandsView();
        showHideButtons();
    }

    public void refreshDccexCommandsView() {
        DccexResponsesLabel.setText(Html.fromHtml(mainapp.dccexResponsesStr));
        DccexSendsLabel.setText(Html.fromHtml(mainapp.dccexSendsStr));
    }


    //********************************************************************

    public class servo_profiles_spinner_listener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            dccExServoProfilesIndex = dccExServoProfilesSpinner.getSelectedItemPosition();
            dccExServoProfile = Integer.parseInt(dccExServoProfilesEntryValuesArray[dccExServoProfilesIndex]);

            setExRailInstruction();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    public class command_spinner_listener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

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
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close
            }

            refreshDccexView();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    // For speed slider speed buttons.
    protected class RptUpdater implements Runnable {
        int which;
        int repeatDelay;

        protected RptUpdater(int Which, int rptDelay) {
            which = Which;
            repeatDelay = rptDelay;

            if (repeatDelay!=0) {
                REP_DELAY = repeatDelay;
            } else {
                try {
                    REP_DELAY = Integer.parseInt(prefs.getString("speed_arrows_throttle_repeat_delay", "100"));
                } catch (NumberFormatException ex) {
                    REP_DELAY = 100;
                }
            }
        }

        @Override
        public void run() {
//            Log.d("EX_Toolbox", "RptUpdater: onProgressChanged - mAutoIncrement: " + mAutoIncrement + " mAutoDecrement: " + mAutoDecrement);
            if (autoIncrement[which]) {
                sendServoPosition(which, DELTA_INCREMENT);
                repeatUpdateHandler.postDelayed(new RptUpdater(which,REP_DELAY), REP_DELAY);
            } else if (autoDecrement[which]) {
                sendServoPosition(which, DELTA_DECREMENT);
                repeatUpdateHandler.postDelayed(new RptUpdater(which,REP_DELAY), REP_DELAY);
            }
        }
    }

    public class servoSpinnerListener implements AdapterView.OnItemSelectedListener {
        @SuppressLint("DefaultLocale")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Spinner spinner = findViewById(R.id.ex_ServosList);
            int index = spinner.getSelectedItemPosition();
            if (index>0) {
                dccexServoVpin = String.format("%d", mainapp.importExport.servoVpinList.get(index));
                dccexServoVpinValue = mainapp.importExport.servoVpinList.get(index);
                etDccexServoVpinValue.setText(dccexServoVpin);

                dccexServoThrownPosition = String.format("%d", mainapp.importExport.servoThrownPositionList.get(index));
                dccexServoThrownPositionValue = mainapp.importExport.servoThrownPositionList.get(index);
                etDccexServoThrownPositionValue.setText(dccexServoThrownPosition);

                dccexServoMidPosition = String.format("%d", mainapp.importExport.servoThrownPositionList.get(index));
                dccexServoMidPositionValue = mainapp.importExport.servoThrownPositionList.get(index);
                etDccexServoMidPositionValue.setText(dccexServoMidPosition);

                dccexServoClosedPosition = String.format("%d", mainapp.importExport.servoClosedPositionList.get(index));
                dccexServoClosedPositionValue = mainapp.importExport.servoClosedPositionList.get(index);
                etDccexServoClosedPositionValue.setText(dccexServoClosedPosition);

                dccExServoProfile = mainapp.importExport.servoProfileList.get(index);
                dccExServoProfilesSpinner.setSelection(dccExServoProfile);

                refreshDccexView();
                dss_servosList.setSelection(0);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }


}
