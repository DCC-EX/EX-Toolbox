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

import dcc_ex.ex_toolbox.logviewer.ui.LogViewerActivity;

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

    private String DCCEXservoVpin = SERVO_VPIN_DEFAULT;
    private Integer DCCEXservoVpinValue = Integer.parseInt(SERVO_VPIN_DEFAULT);
    private EditText etDCCEXservoVpinValue;

    private String DCCEXservoThrownPosition = SERVO_THROWN_POSITION_DEFAULT;
    private Integer DCCEXservoThrownPositionValue = Integer.parseInt(SERVO_THROWN_POSITION_DEFAULT);
    private EditText etDCCEXservoThrownPositionValue;
    private boolean autoIncrementThrownPosition = false;

    private String DCCEXservoMidPosition = SERVO_MID_POSITION_DEFAULT;
    private Integer DCCEXservoMidPositionValue = Integer.parseInt(SERVO_MID_POSITION_DEFAULT);
    private EditText etDCCEXservoMidPositionValue;
    private boolean autoIncrementMidPosition = false;

    private String DCCEXservoClosedPosition = SERVO_CLOSED_POSITION_DEFAULT;
    private Integer DCCEXservoClosedPositionValue = Integer.parseInt(SERVO_CLOSED_POSITION_DEFAULT);
    private EditText etDCCEXservoClosedPositionValue;

    private boolean [] autoIncrement = {false, false, false, false};
    private boolean [] autoDecrement = {false, false, false, false};

    Spinner dccExServoProfilesSpinner;
    Integer dccExServoProfilesIndex = 0;
    Integer dccExServoProfile = 0;

    String[] dccExServoProfilesEntryValuesArray;
    String[] dccExServoProfilesEntriesArray; // display version

    private String DCCEXsendCommandValue = "";
    private EditText etDCCEXsendCommandValue;

    private LinearLayout DCCEXwriteInfoLayout;
    private TextView DCCEXwriteInfoLabel;
    private String DCCEXinfoStr = "";

    private TextView DCCEXresponsesLabel;
    private TextView DCCEXsendsLabel;
//    private String DCCEXresponsesStr = "";
//    private String DCCEXsendsStr = "";
    private ScrollView DCCEXresponsesScrollView;
    private ScrollView DCCEXsendsScrollView;

//    ArrayList<String> DCCEXresponsesListHtml = new ArrayList<>();
//    ArrayList<String> DCCEXsendsListHtml = new ArrayList<>();

    private int dccCmdIndex = 0;
    String[] dccExCommonCommandsEntryValuesArray;
    String[] dccExCommonCommandsEntriesArray; // display version
    int[] dccExCommonCommandsHasParametersArray; // display version

    private boolean DCCEXhideSends = false;

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

//    private int DCCEXpreviousCommandIndex = -1;
//    ArrayList<String> DCCEXpreviousCommandList = new ArrayList<>();

    static final int WHICH_VPIN = 0;
    static final int WHICH_THROWN_POSITION = 1;
    static final int WHICH_MID_POSITION = 2;
    static final int WHICH_CLOSED_POSITION = 3;
    static final int WHICH_COMMAND = 4;

    static final int DELTA_INCREMENT = 1;
    static final int DELTA_DECREMENT = -1;
    static final int DELTA_ZERO = 0;

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
        // Log.d("Engine_Driver", "gestureMove action " + event.getAction());
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
                // Log.d("Engine_Driver", "gestureVelocity vel " + velocityX);
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
        // Log.d("Engine_Driver", "gestureEnd action " + event.getAction() + " inProgress? " + gestureInProgress);
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
//                    refreshDCCEXview();
                    refreshDCCEXcommandsView();
                    break;
                case message_type.DCCEX_RESPONSE:  // informational response
//                    refreshDCCEXview();
                    refreshDCCEXcommandsView();
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
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("Engine_Driver", "web_activity.onCreate()");

        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        mainapp.applyTheme(this);

        super.onCreate(savedInstanceState);

        if (mainapp.isForcingFinish()) {        // expedite
            return;
        }

        setContentView(R.layout.servos);

        //put pointer to this activity's handler in main app's shared variable
        mainapp.servos_msg_handler = new servos_handler();

        dccExServoThrowButton = findViewById(R.id.dexc_DCCEXservoThrowButton);
        servo_button_listener servo_throw_click_listener = new servo_button_listener(WHICH_THROWN_POSITION);
        dccExServoThrowButton.setOnClickListener(servo_throw_click_listener);

        dccExServoMidButton = findViewById(R.id.dexc_DCCEXservoMidButton);
        servo_button_listener servo_mid_click_listener = new servo_button_listener(WHICH_MID_POSITION);
        dccExServoMidButton.setOnClickListener(servo_mid_click_listener);

        dccExServoCloseButton = findViewById(R.id.dexc_DCCEXservoCloseButton);
        servo_button_listener servo_close_click_listener = new servo_button_listener(WHICH_CLOSED_POSITION);
        dccExServoCloseButton.setOnClickListener(servo_close_click_listener);

        dccExServoPositionIncrementButton = findViewById(R.id.dexc_DCCEXservoPositionIncrementButton);
        servo_position_increment_button_listener servo_position_increment_click_listener = new servo_position_increment_button_listener();
        dccExServoPositionIncrementButton.setOnClickListener(servo_position_increment_click_listener);
        dccExServoPositionIncrementButton.setOnLongClickListener(servo_position_increment_click_listener);
        dccExServoPositionIncrementButton.setOnTouchListener(servo_position_increment_click_listener);

        dccExServoPositionDecrementButton = findViewById(R.id.dexc_DCCEXservoPositionDecrementButton);
        servo_position_decrement_button_listener servo_position_decrement_click_listener = new servo_position_decrement_button_listener();
        dccExServoPositionDecrementButton.setOnClickListener(servo_position_decrement_click_listener);
        dccExServoPositionDecrementButton.setOnLongClickListener(servo_position_decrement_click_listener);
        dccExServoPositionDecrementButton.setOnTouchListener(servo_position_decrement_click_listener);

        dccExServoPositionSwapButton = findViewById(R.id.dexc_DCCEXservoPositionSwapButton);
        servo_position_swap_button_listener servo_position_swap_click_listener = new servo_position_swap_button_listener();
        dccExServoPositionSwapButton.setOnClickListener(servo_position_swap_click_listener);

        dccExServoResetButton = findViewById(R.id.dexc_DCCEXservoResetButton);
        servo_reset_button_listener servo_reset_click_listener = new servo_reset_button_listener();
        dccExServoResetButton.setOnClickListener(servo_reset_click_listener);

        //-----------------------------------------

        etDCCEXservoVpinValue = findViewById(R.id.dexc_DCCEXservoVpinValue);
        etDCCEXservoVpinValue.setText(SERVO_VPIN_DEFAULT);
        etDCCEXservoVpinValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_VPIN); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        etDCCEXservoClosedPositionValue = findViewById(R.id.dexc_DCCEXservoClosedPositionValue);
        etDCCEXservoClosedPositionValue.setText(SERVO_CLOSED_POSITION_DEFAULT);
        etDCCEXservoClosedPositionValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_CLOSED_POSITION); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        etDCCEXservoMidPositionValue = findViewById(R.id.dexc_DCCEXservoMidPositionValue);
        etDCCEXservoMidPositionValue.setText(SERVO_MID_POSITION_DEFAULT);
        etDCCEXservoMidPositionValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_MID_POSITION); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        etDCCEXservoThrownPositionValue = findViewById(R.id.dexc_DCCEXservoThrownPositionValue);
        etDCCEXservoThrownPositionValue.setText(SERVO_THROWN_POSITION_DEFAULT);
        etDCCEXservoThrownPositionValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_THROWN_POSITION); showHideButtons();  }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        //-----------------------------------------

        dccExServoProfilesEntryValuesArray = this.getResources().getStringArray(R.array.dccExServoProfilesEntryValues);
        dccExServoProfilesEntriesArray = this.getResources().getStringArray(R.array.dccExServoProfilesEntries);

        dccCmdIndex=0;
        dccExServoProfilesSpinner = findViewById(R.id.dexc_servoProfilesList);
        ArrayAdapter<?> spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExServoProfilesEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccExServoProfilesSpinner.setAdapter(spinner_adapter);
        dccExServoProfilesSpinner.setOnItemSelectedListener(new servo_profiles_spinner_listener());
        dccExServoProfilesSpinner.setSelection(0);

        //-----------------------------------------

        dccExCommonCommandsEntryValuesArray = this.getResources().getStringArray(R.array.dccExCommonCommandsEntryValues);
        dccExCommonCommandsEntriesArray = this.getResources().getStringArray(R.array.dccExCommonCommandsEntries); // display version
        dccExCommonCommandsHasParametersArray = this.getResources().getIntArray(R.array.dccExCommonCommandsHasParameters);

        dccCmdIndex=0;
        dccExCommonCommandsSpinner = findViewById(R.id.dexc_common_commands_list);
        spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExCommonCommandsEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccExCommonCommandsSpinner.setAdapter(spinner_adapter);
        dccExCommonCommandsSpinner.setOnItemSelectedListener(new command_spinner_listener());
        dccExCommonCommandsSpinner.setSelection(dccCmdIndex);

        sendCommandButton = findViewById(R.id.dexc_DCCEXsendCommandButton);
        send_command_button_listener sendCommandClickListener = new send_command_button_listener();
        sendCommandButton.setOnClickListener(sendCommandClickListener);

        etDCCEXsendCommandValue = findViewById(R.id.dexc_DCCEXsendCommandValue);
        etDCCEXsendCommandValue.setInputType(TYPE_TEXT_FLAG_AUTO_CORRECT);
        etDCCEXsendCommandValue.setText("");
        etDCCEXsendCommandValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextField(WHICH_COMMAND); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        DCCEXwriteInfoLayout = findViewById(R.id.dexc_DCCEXwriteInfoLayout);
        DCCEXwriteInfoLabel = findViewById(R.id.dexc_DCCEXwriteInfoLabel);
        DCCEXwriteInfoLabel.setText("");

        previousCommandButton = findViewById(R.id.dexc_DCCEXpreviousCommandButton);
        previous_command_button_listener previousCommandClickListener = new previous_command_button_listener();
        previousCommandButton.setOnClickListener(previousCommandClickListener);

        nextCommandButton = findViewById(R.id.dexc_DCCEXnextCommandButton);
        next_command_button_listener nextCommandClickListener = new next_command_button_listener();
        nextCommandButton.setOnClickListener(nextCommandClickListener);

        DCCEXresponsesLabel = findViewById(R.id.dexc_DCCEXresponsesLabel);
        DCCEXresponsesLabel.setText("");
        DCCEXsendsLabel = findViewById(R.id.dexc_DCCEXsendsLabel);
        DCCEXsendsLabel.setText("");

        clearCommandsButton = findViewById(R.id.dexc_DCCEXclearCommandsButton);
        clear_commands_button_listener clearCommandsClickListener = new clear_commands_button_listener();
        clearCommandsButton.setOnClickListener(clearCommandsClickListener);


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
        mainapp.DCCEXscreenIsOpen = true;
        refreshDCCEXview();
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
            mainapp.servos_msg_handler = new servos_handler();
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
            Log.d("Engine_Driver", "onDestroy: mainapp.web_msg_handler is null. Unable to removeCallbacksAndMessages");
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
        switch (item.getItemId()) {

            case R.id.cv_programmer_mnu:
                navigateAway(true, null);
                in = new Intent().setClass(this, cv_programmer.class);
                startACoreActivity(this, in, false, 0);
                return true;
            case R.id.track_manager_mnu:
                navigateAway(true, null);
                in = new Intent().setClass(this, track_manager.class);
                startACoreActivity(this, in, false, 0);
                return true;
            case R.id.currents_mnu:
                navigateAway(true, null);
                in = new Intent().setClass(this, currents.class);
                startACoreActivity(this, in, false, 0);
                return true;
            case R.id.sensors_mnu:
                navigateAway(true, null);
                in = new Intent().setClass(this, sensors.class);
                startACoreActivity(this, in, false, 0);
                return true;

            case R.id.exit_mnu:
                mainapp.checkExit(this);
                return true;
            case R.id.power_control_mnu:
                navigateAway(false, power_control.class);
                return true;
/*            case R.id.preferences_mnu:
                navigateAway(false, SettingsActivity.class);
                return true;*/
            case R.id.settings_mnu:
                in = new Intent().setClass(this, SettingsActivity.class);
                startActivityForResult(in, 0);
                connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
                return true;
            case R.id.logviewer_menu:
                navigateAway(false, LogViewerActivity.class);
                return true;
            case R.id.about_mnu:
                navigateAway(false, about_page.class);
                return true;
            case R.id.power_layout_button:
                if (!mainapp.isPowerControlAllowed()) {
                    mainapp.powerControlNotAllowedDialog(tMenu);
                } else {
                    mainapp.powerStateMenuButton();
                }
                mainapp.buttonVibration();
                return true;
            default:
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
        if ( (DCCEXservoVpin.length()==0) || (DCCEXservoThrownPosition.length()==0) || (DCCEXservoMidPosition.length()==0) || (DCCEXservoClosedPosition.length()==0) ) {
            enable = false;
        }
        dccExServoCloseButton.setEnabled(enable);
        dccExServoMidButton.setEnabled(enable);
        dccExServoThrowButton.setEnabled(enable);
        dccExServoPositionDecrementButton.setEnabled(enable);
        dccExServoPositionIncrementButton.setEnabled(enable);

        sendCommandButton.setEnabled((DCCEXsendCommandValue.length() != 0) && (DCCEXsendCommandValue.charAt(0) != '<'));
        previousCommandButton.setEnabled((mainapp.DCCEXpreviousCommandIndex >= 0));
        nextCommandButton.setEnabled((mainapp.DCCEXpreviousCommandIndex >= 0));
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
//        if (DCCEXservoThrownPositionValue > DCCEXservoClosedPositionValue) {
//            result = (DCCEXservoThrownPositionValue - DCCEXservoClosedPositionValue)/2 + DCCEXservoClosedPositionValue;
//        } else if(DCCEXservoThrownPositionValue < DCCEXservoClosedPositionValue) {
//            result = (DCCEXservoClosedPositionValue - DCCEXservoThrownPositionValue)/2 + DCCEXservoThrownPositionValue;
//        } else { // =
//            result = DCCEXservoThrownPositionValue;
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
                etDCCEXservoClosedPositionValue.requestFocus();
                etDCCEXservoClosedPositionValue.setSelection(DCCEXservoClosedPosition.length());
//                    msgTxt = String.format("%s %d %d", DCCEXservoVpin, DCCEXservoClosedPositionValue, dccExServoProfile);
                break;
            case WHICH_MID_POSITION:
                etDCCEXservoMidPositionValue.requestFocus();
                etDCCEXservoMidPositionValue.setSelection(DCCEXservoMidPosition.length());
//                    msgTxt = String.format("%s %d %d", DCCEXservoVpin, DCCEXservoMidPositionValue, dccExServoProfile);
                break;
            case WHICH_THROWN_POSITION:
                etDCCEXservoThrownPositionValue.requestFocus();
                etDCCEXservoThrownPositionValue.setSelection(DCCEXservoThrownPosition.length());
//                    msgTxt = String.format("%s %d %d", DCCEXservoVpin, DCCEXservoThrownPositionValue, dccExServoProfile);
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
            DCCEXinfoStr = "";
            mainapp.buttonVibration();
            sendServoPosition(buttonNo, DELTA_ZERO);
            refreshDCCEXview();
            mainapp.hideSoftKeyboard(v);

            setTextSelection(buttonNo);

//            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.MOVE_SERVO, msgTxt, 0);
        }
    }

    void sendServoPosition(int which, int delta) {
        String msgTxt;

        switch (which) {
            case WHICH_THROWN_POSITION:
                DCCEXservoThrownPositionValue = DCCEXservoThrownPositionValue + delta;
                DCCEXservoThrownPosition = Integer.toString(DCCEXservoThrownPositionValue);
                etDCCEXservoThrownPositionValue.setText(DCCEXservoThrownPosition);
                msgTxt = String.format("%s %d %d", DCCEXservoVpin, DCCEXservoThrownPositionValue, dccExServoProfile);
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.MOVE_SERVO, msgTxt, 0);
//                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.MOVE_SERVO, DCCEXservoVpin, DCCEXservoThrownPositionValue);
                break;
            case WHICH_MID_POSITION:
                DCCEXservoMidPositionValue = DCCEXservoMidPositionValue + delta;
                DCCEXservoMidPosition = Integer.toString(DCCEXservoMidPositionValue);
                etDCCEXservoMidPositionValue.setText(DCCEXservoMidPosition);
                msgTxt = String.format("%s %d %d", DCCEXservoVpin, DCCEXservoMidPositionValue, dccExServoProfile);
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.MOVE_SERVO, msgTxt, 0);
//              mainapp.sendMsg(mainapp.comm_msg_handler, message_type.MOVE_SERVO, DCCEXservoVpin, DCCEXservoMidPositionValue);
                break;
            case WHICH_CLOSED_POSITION:
                DCCEXservoClosedPositionValue = DCCEXservoClosedPositionValue + delta;
                DCCEXservoClosedPosition = Integer.toString(DCCEXservoClosedPositionValue);
                etDCCEXservoClosedPositionValue.setText(DCCEXservoClosedPosition);
                msgTxt = String.format("%s %d %d", DCCEXservoVpin, DCCEXservoClosedPositionValue, dccExServoProfile);
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.MOVE_SERVO, msgTxt, 0);
//              mainapp.sendMsg(mainapp.comm_msg_handler, message_type.MOVE_SERVO, DCCEXservoVpin, DCCEXservoClosedPositionValue);
                break;
        }
        setTextSelection(which);
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

            String tempStr = DCCEXservoThrownPosition;
            int tempInt = DCCEXservoThrownPositionValue;
            DCCEXservoThrownPositionValue = DCCEXservoClosedPositionValue;
            DCCEXservoThrownPosition = DCCEXservoClosedPosition;
            DCCEXservoClosedPositionValue = tempInt;
            DCCEXservoClosedPosition = tempStr;

            etDCCEXservoClosedPositionValue.setText(DCCEXservoClosedPosition);
            etDCCEXservoThrownPositionValue.setText(DCCEXservoThrownPosition);

            mainapp.hideSoftKeyboard(v);
        }
    }

    private void resetServo() {
        DCCEXservoVpin = SERVO_VPIN_DEFAULT;
        DCCEXservoVpinValue = Integer.parseInt(SERVO_VPIN_DEFAULT);
        DCCEXservoThrownPosition = SERVO_THROWN_POSITION_DEFAULT;
        DCCEXservoThrownPositionValue = Integer.parseInt(SERVO_THROWN_POSITION_DEFAULT);
        DCCEXservoMidPosition = SERVO_MID_POSITION_DEFAULT;
        DCCEXservoMidPositionValue = Integer.parseInt(SERVO_MID_POSITION_DEFAULT);
        DCCEXservoClosedPosition = SERVO_CLOSED_POSITION_DEFAULT;
        DCCEXservoClosedPositionValue = Integer.parseInt(SERVO_CLOSED_POSITION_DEFAULT);

        etDCCEXservoVpinValue.setText(DCCEXservoVpin);
        etDCCEXservoClosedPositionValue.setText(DCCEXservoClosedPosition);
        etDCCEXservoMidPositionValue.setText(DCCEXservoMidPosition);
        etDCCEXservoThrownPositionValue.setText(DCCEXservoThrownPosition);
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
            DCCEXinfoStr = "";
            String cmdStr = etDCCEXsendCommandValue.getText().toString();
            if ((cmdStr.length() > 0) && (cmdStr.charAt(0) != '<')) {
                mainapp.buttonVibration();
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.DCCEX_SEND_COMMAND, "<" + cmdStr + ">");

                if ((cmdStr.charAt(0) == '=') && (cmdStr.length() > 1)) // we don't get a response from a tracks command, so request an update
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");

                if ((mainapp.DCCEXpreviousCommandList.size() <= 0) || !(mainapp.DCCEXpreviousCommandList.get(mainapp.DCCEXpreviousCommandList.size() - 1).equals(cmdStr))) {
                    mainapp.DCCEXpreviousCommandList.add(cmdStr);
                    if (mainapp.DCCEXpreviousCommandList.size() > 20) {
                        mainapp.DCCEXpreviousCommandList.remove(0);
                    }
                }
                mainapp.DCCEXpreviousCommandIndex = mainapp.DCCEXpreviousCommandList.size();
            }
            resetTextField(WHICH_COMMAND);
            refreshDCCEXview();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class previous_command_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DCCEXinfoStr = "";
            String cmdStr = etDCCEXsendCommandValue.getText().toString();
            if (mainapp.DCCEXpreviousCommandIndex > 0) {
                DCCEXsendCommandValue = mainapp.DCCEXpreviousCommandList.get(mainapp.DCCEXpreviousCommandIndex - 1);
                mainapp.DCCEXpreviousCommandIndex--;
            } else {
                DCCEXsendCommandValue = mainapp.DCCEXpreviousCommandList.get(mainapp.DCCEXpreviousCommandList.size() - 1);
                mainapp.DCCEXpreviousCommandIndex = mainapp.DCCEXpreviousCommandList.size() - 1;
            }
            etDCCEXsendCommandValue.setText(DCCEXsendCommandValue);

            refreshDCCEXview();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class next_command_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            DCCEXinfoStr = "";
            String cmdStr = etDCCEXsendCommandValue.getText().toString();
            if (mainapp.DCCEXpreviousCommandIndex < mainapp.DCCEXpreviousCommandList.size() - 1) {
                DCCEXsendCommandValue = mainapp.DCCEXpreviousCommandList.get(mainapp.DCCEXpreviousCommandIndex + 1);
                mainapp.DCCEXpreviousCommandIndex++;
            } else {
                DCCEXsendCommandValue = mainapp.DCCEXpreviousCommandList.get(0);
                mainapp.DCCEXpreviousCommandIndex = 0;
            }
            etDCCEXsendCommandValue.setText(DCCEXsendCommandValue);

            refreshDCCEXview();
            mainapp.hideSoftKeyboard(v);
        }
    }

    public class clear_commands_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.DCCEXresponsesListHtml.clear();
            mainapp.DCCEXsendsListHtml.clear();
            mainapp.DCCEXresponsesStr = "";
            mainapp.DCCEXsendsStr = "";
            refreshDCCEXview();
        }
    }



    //********************************************************************

    private void resetTextField(int which) {
        switch (which) {
            case WHICH_VPIN:
                DCCEXservoVpin = SERVO_VPIN_DEFAULT;
                DCCEXservoVpinValue = Integer.parseInt(SERVO_VPIN_DEFAULT);
                etDCCEXservoVpinValue.setText(SERVO_VPIN_DEFAULT);
                break;
            case WHICH_THROWN_POSITION:
                DCCEXservoThrownPosition = SERVO_THROWN_POSITION_DEFAULT;
                DCCEXservoThrownPositionValue = Integer.parseInt(SERVO_THROWN_POSITION_DEFAULT);
                etDCCEXservoThrownPositionValue.setText(SERVO_THROWN_POSITION_DEFAULT);
                break;
            case WHICH_MID_POSITION:
                DCCEXservoMidPosition = SERVO_MID_POSITION_DEFAULT;
                DCCEXservoMidPositionValue = Integer.parseInt(SERVO_MID_POSITION_DEFAULT);
                etDCCEXservoMidPositionValue.setText(SERVO_MID_POSITION_DEFAULT);
                break;
            case WHICH_CLOSED_POSITION:
                DCCEXservoClosedPosition = SERVO_CLOSED_POSITION_DEFAULT;
                DCCEXservoClosedPositionValue = Integer.parseInt(SERVO_CLOSED_POSITION_DEFAULT);
                etDCCEXservoClosedPositionValue.setText(SERVO_CLOSED_POSITION_DEFAULT);
                break;
            case WHICH_COMMAND:
                DCCEXsendCommandValue = "";
                etDCCEXsendCommandValue.setText("");
        }
    }

    private void readTextField(int which) {
        switch (which) {
            case WHICH_VPIN:
                DCCEXservoVpin = etDCCEXservoVpinValue.getText().toString();
                DCCEXservoVpinValue = getIntFromString(DCCEXservoVpin);
                break;
            case WHICH_THROWN_POSITION:
                DCCEXservoThrownPosition = etDCCEXservoThrownPositionValue.getText().toString();
                DCCEXservoThrownPositionValue = getIntFromString(DCCEXservoThrownPosition);
                break;
            case WHICH_MID_POSITION:
                DCCEXservoMidPosition = etDCCEXservoMidPositionValue.getText().toString();
                DCCEXservoMidPositionValue = getIntFromString(DCCEXservoMidPosition);
                break;
            case WHICH_CLOSED_POSITION:
                DCCEXservoClosedPosition = etDCCEXservoClosedPositionValue.getText().toString();
                DCCEXservoClosedPositionValue = getIntFromString(DCCEXservoClosedPosition);
                break;
            case WHICH_COMMAND:
                DCCEXsendCommandValue = etDCCEXsendCommandValue.getText().toString();
                break;
        }
        setActivateServoButtons(which);
    }


    //********************************************************************

    public void refreshDCCEXview() {
        DCCEXwriteInfoLabel.setText(DCCEXinfoStr);
        refreshDCCEXcommandsView();
        showHideButtons();
    }

    public void refreshDCCEXcommandsView() {
        DCCEXresponsesLabel.setText(Html.fromHtml(mainapp.DCCEXresponsesStr));
        DCCEXsendsLabel.setText(Html.fromHtml(mainapp.DCCEXsendsStr));
    }


    //********************************************************************

    public class servo_profiles_spinner_listener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            dccExServoProfilesIndex = dccExServoProfilesSpinner.getSelectedItemPosition();
            dccExServoProfile = Integer.parseInt(dccExServoProfilesEntryValuesArray[dccExServoProfilesIndex]);
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
                DCCEXsendCommandValue = dccExCommonCommandsEntryValuesArray[dccCmdIndex];
                if (dccExCommonCommandsHasParametersArray[dccCmdIndex] >0)
                    DCCEXsendCommandValue = DCCEXsendCommandValue + " ";
                etDCCEXsendCommandValue.setText(DCCEXsendCommandValue);
                etDCCEXsendCommandValue.requestFocus();
                etDCCEXsendCommandValue.setSelection(DCCEXsendCommandValue.length());
            }
            dccCmdIndex = 0;
            dccExCommonCommandsSpinner.setSelection(dccCmdIndex);
            DCCEXinfoStr = "";

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if ((imm != null) && (view != null)) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); // force the softkeyboard to close
            }

            refreshDCCEXview();
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




}
