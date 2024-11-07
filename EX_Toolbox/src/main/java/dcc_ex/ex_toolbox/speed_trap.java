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

import static dcc_ex.ex_toolbox.threaded_application.context;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import dcc_ex.ex_toolbox.logviewer.ui.LogViewerActivity;
import dcc_ex.ex_toolbox.type.message_type;
import dcc_ex.ex_toolbox.util.LocaleHelper;

public class speed_trap extends AppCompatActivity implements GestureOverlayView.OnGestureListener {

    private threaded_application mainapp;  // hold pointer to mainapp
    private SharedPreferences prefs;

    private Menu tMenu;
//    private static boolean savedMenuSelected;

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
    private String startPinText = "22";
    private String endPinText = "24";
    private Integer startPin = 22;
    private Integer endPin = 24;
    private EditText etDccexStartPin;
    private EditText etDccexEndPin;

    private String distanceText = "10";
    private Double distance = 10.0;
    private EditText etDccexDistance;

    private String delayText = "10";
    private long delay = 10;
    private long delayMilliseconds = 10000;
    private EditText etDccexDelay;

//    private LinearLayout dccexWriteInfoLayout;
    private TextView dccexWriteInfoLabel;
    private String dccexInfoStr = "";

    private TextView dccexResponsesLabel;
    private TextView dccexSendsLabel;
//    private ScrollView dccexResponsesScrollView;
//    private ScrollView dccexSendsScrollView;

    private TextView tvScaleSpeed;
//    double scaleSpeed;
    String scaleSpeedText = "";

    private int scalesIndex = 0;
//    String[] scalesEntryValuesArray;
//    String[] scalesEntriesArray; // display version
    String[] scaleRatiosArray;
    private double scaleRatio = 1.0;

    private int unitsIndex = 0;
//    String[] unitsEntryValuesArray;
//    String[] unitsEntriesArray; // display version
    String[] unitsRatiosArray;
    private double unitsRatio = 1.0;

    Spinner scalesSpinner;
    Spinner unitsSpinner;

    Button startButton;
    Button swapButton;
    Button clearCommandsButton;


    private static final int DISABLED = 0;
    private static final int ENABLED = 1;
    private static final int RUN_STARTED = 2;
    private static final int RUN_FINISHED = 3;

    private long startTime;
    private long endTime;
    private double runTime = 0;

    int runState = DISABLED;
    private MyCountDownTimer waitTimer;

    //**************************************

    private LinearLayout screenNameLine;
    private Toolbar toolbar;
    private LinearLayout statusLine;
//    private int toolbarHeight;

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

//        toolbarHeight = toolbar.getHeight() + statusLine.getHeight() + screenNameLine.getHeight();

        gestureInProgress = true;
        gestureLastCheckTime = event.getEventTime();
        mVelocityTracker.clear();

        // start the gesture timeout timer
        if (mainapp.speed_trap_msg_handler != null)
            mainapp.speed_trap_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
    }

    public void gestureMove(MotionEvent event) {
        // Log.d("EX_Toolbox", "gestureMove action " + event.getAction());
        if ( (mainapp != null) && (mainapp.speed_trap_msg_handler != null) && (gestureInProgress) ) {
            // stop the gesture timeout timer
            mainapp.speed_trap_msg_handler.removeCallbacks(gestureStopped);

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
                mainapp.speed_trap_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
            }
        }
    }

    private void gestureEnd(MotionEvent event) {
        // Log.d("EX_Toolbox", "gestureEnd action " + event.getAction() + " inProgress? " + gestureInProgress);
        if ( (mainapp != null) && (mainapp.speed_trap_msg_handler != null) && (gestureInProgress) ) {
            mainapp.speed_trap_msg_handler.removeCallbacks(gestureStopped);

            float deltaX = (event.getX() - gestureStartX);
            float absDeltaX =  Math.abs(deltaX);
            if (absDeltaX > threaded_application.min_fling_distance) { // only process left/right swipes
                // valid gesture. Change the event action to CANCEL so that it isn't processed by any control below the gesture overlay
                event.setAction(MotionEvent.ACTION_CANCEL);
                // process swipe in the direction with the largest change
                Intent nextScreenIntent = mainapp.getNextIntentInSwipeSequence(threaded_application.SCREEN_SWIPE_INDEX_SPEED_TRAP, deltaX);
                startACoreActivity(this, nextScreenIntent, true, deltaX);
            } else {
                // gesture was not long enough
                gestureFailed(event);
            }
        }
    }

    private void gestureCancel(MotionEvent event) {
        if (mainapp.speed_trap_msg_handler != null)
            mainapp.speed_trap_msg_handler.removeCallbacks(gestureStopped);
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
    class SpeedTrapMessageHandler extends Handler {

        public SpeedTrapMessageHandler(Looper looper) {
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

                case message_type.RECEIVED_SENSOR: {
                    String s = msg.obj.toString();
                    if (s.length() > 0) {
                        String[] sArgs = s.split("(\\|)");
                        updateScaleSpeed(sArgs[0], sArgs[1]);
                    }
                    break;
                }
                case message_type.DCCEX_COMMAND_ECHO:  // informational response
                case message_type.DCCEX_RESPONSE:
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
                    getApplicationContext().getResources().getString(R.string.app_name_speed_trap),
                    mainapp.getFastClockTime());
        else
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_speed_trap),
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

        setContentView(R.layout.speed_trap);

        //put pointer to this activity's handler in main app's shared variable
        mainapp.speed_trap_msg_handler = new SpeedTrapMessageHandler(Looper.getMainLooper());

        mainapp.getCommonPreferences();
        mainapp.loadBackgroundImage(findViewById(R.id.speed_trapBackgroundImgView));


        // *****************************


        dccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexSpeedTrapRunWaitingToStart);
//        dccexWriteInfoLayout = findViewById(R.id.ex_writeInfoLayout);
        dccexWriteInfoLabel = findViewById(R.id.ex_writeInfoLabel);
        dccexWriteInfoLabel.setText(dccexInfoStr);

        dccexResponsesLabel = findViewById(R.id.ex_responsesLabel);
        dccexResponsesLabel.setText("");
        dccexSendsLabel = findViewById(R.id.ex_DccexSendsLabel);
        dccexSendsLabel.setText("");

        // *****************************


        etDccexStartPin = findViewById(R.id.ex_startPin);
        startPinText = prefs.getString("prefSpeedTrapStartPin", "22");
        startPin = Integer.valueOf(startPinText);
        etDccexStartPin.setText(startPinText);
        etDccexStartPin.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextFields(); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        etDccexEndPin = findViewById(R.id.ex_endPin);
        endPinText = prefs.getString("prefSpeedTrapEndPin", "24");
        endPin = Integer.valueOf(endPinText);
        etDccexEndPin.setText(endPinText);
        etDccexEndPin.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextFields(); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        etDccexDistance = findViewById(R.id.ex_distance);
        distanceText = prefs.getString("prefSpeedTrapDistance", "10");
        distance = Double.valueOf(distanceText);
        etDccexDistance.setText(distanceText);
        etDccexDistance.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextFields(); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        etDccexDelay = findViewById(R.id.ex_delay);
        delayText = prefs.getString("prefSpeedTrapDelay", "10");
        delay = Long.valueOf(delayText);
        delayMilliseconds = delay * 1000;
        etDccexDelay.setText(delayText);
        etDccexDelay.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { readTextFields(); showHideButtons(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        scaleRatiosArray = this.getResources().getStringArray(R.array.scaleRatios);

        scalesIndex = Integer.valueOf(prefs.getString("prefSpeedTrapScalesIndex", "33"));
        scaleRatio = Double.parseDouble(scaleRatiosArray[scalesIndex]);

        scalesSpinner = findViewById(R.id.ex_scales_list);
        ArrayAdapter<?> scalesSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.scaleNames, android.R.layout.simple_spinner_item);
        scalesSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scalesSpinner.setAdapter(scalesSpinnerAdapter);
        scalesSpinner.setOnItemSelectedListener(new ScalesSpinnerListener());
        scalesSpinner.setSelection(scalesIndex);

        unitsRatiosArray = this.getResources().getStringArray(R.array.unitRatios);

        unitsIndex=0;
        unitsSpinner = findViewById(R.id.ex_units_list);
        ArrayAdapter<?> unitsSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.unitNames, android.R.layout.simple_spinner_item);
        unitsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitsSpinner.setAdapter(unitsSpinnerAdapter);
        unitsSpinner.setOnItemSelectedListener(new UnitsSpinnerListener());
        unitsSpinner.setSelection(unitsIndex);

        startButton = findViewById(R.id.ex_startButton);
        StartButtonListener startButtonListener = new StartButtonListener();
        startButton.setOnClickListener(startButtonListener);
        runState = DISABLED;

        swapButton = findViewById(R.id.ex_swapButton);
        SwapButtonListener swapButtonListener = new SwapButtonListener();
        swapButton.setOnClickListener(swapButtonListener);

        tvScaleSpeed = findViewById(R.id.ex_ScaleSpeed);

        clearCommandsButton = findViewById(R.id.ex_dccexClearCommandsButton);
        ClearCommandsButtonListener clearCommandsClickListener = new ClearCommandsButtonListener();
        clearCommandsButton.setOnClickListener(clearCommandsClickListener);

        // *****************************

        screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = findViewById(R.id.toolbar);
        statusLine = findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

    } // end onCreate

    @Override
    public void onResume() {
        Log.d("EX_Toolbox", "speed_trap.onResume() called");
        mainapp.applyTheme(this);
        super.onResume();

        mainapp.getCommonPreferences();

        setActivityTitle();
        mainapp.dccexScreenIsOpen = true;
        mainapp.activeScreen = mainapp.ACTIVE_SCREEN_SPEED_TRAP;
        refreshDccexView();

        if (mainapp.isForcingFinish()) {    //expedite
            this.finish();
            return;
        }

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.TIME_CHANGED);    // request time update
        CookieSyncManager.getInstance().startSync();

        // enable swipe/fling detection if enabled in Prefs
        ov = findViewById(R.id.speed_trap_overlay);
        ov.addOnGestureListener(this);
        ov.setEventsInterceptionEnabled(true);
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    @Override
    public void onPause() {
        Log.d("EX_Toolbox", "speed_trap.onPause() called");
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("EX_Toolbox", "speed_trap.onStart() called");
        // put pointer to this activity's handler in main app's shared variable
        if (mainapp.speed_trap_msg_handler == null)
            mainapp.speed_trap_msg_handler = new SpeedTrapMessageHandler(Looper.getMainLooper());
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
        Log.d("EX_Toolbox", "speed_trap.onDestroy() called");

        mainapp.hideSoftKeyboard(this.getCurrentFocus());
        mainapp.dccexScreenIsOpen = false;

        if (!isRestarting) {
            removeHandlers();
        }
        else {
            isRestarting = false;
        }

        if (mainapp.speed_trap_msg_handler !=null) {
            mainapp.speed_trap_msg_handler.removeCallbacksAndMessages(null);
            mainapp.speed_trap_msg_handler = null;
        } else {
            Log.d("EX_Toolbox", "onDestroy: mainapp.web_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    }

    private void removeHandlers() {
        if (mainapp.speed_trap_msg_handler != null) {
            mainapp.speed_trap_msg_handler.removeCallbacks(gestureStopped);
            mainapp.speed_trap_msg_handler.removeCallbacksAndMessages(null);
            mainapp.speed_trap_msg_handler = null;
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
            if (mainapp.speed_trap_msg_handler!=null) {
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
        inflater.inflate(R.menu.speed_trap_menu, menu);
        tMenu = menu;

        mainapp.setTrackmanagerMenuOption(menu);
        mainapp.setCurrentsMenuOption(menu);

        mainapp.displayToolbarMenuButtons(menu);
        mainapp.displayPowerStateMenuButton(menu);
        mainapp.setPowerMenuOption(menu);
        mainapp.setPowerStateButton(menu);
        mainapp.setPowerMenuOption(menu);
        mainapp.reformatMenu(menu);

        return  super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        Intent in;
        if ( (item.getItemId() == R.id.cv_programmer_mnu) || (item.getItemId() == R.id.toolbar_button_cv_programmer) ) {
            navigateAway(true, null);
            in = new Intent().setClass(this, speed_trap.class);
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
        } else if ( (item.getItemId() == R.id.speed_trap_mnu) || (item.getItemId() == R.id.toolbar_button_speed_trap) ) {
            navigateAway(true, null);
            in = new Intent().setClass(this, speed_trap.class);
            startACoreActivity(this, in, false, 0);
            return true;
        } else if ( (item.getItemId() == R.id.neopixel_mnu) || (item.getItemId() == R.id.toolbar_button_neopixel) ) {
            navigateAway(true, null);
            in = new Intent().setClass(this, neopixel.class);
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
        super.onActivityResult(requestCode, resultCode, data);
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
    // used for swipes for the main activities only
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
        Log.d("EX-Toolbox", "speed_trap.forceRestartApp() ");
        Message msg = Message.obtain();
        msg.what = message_type.RESTART_APP;
        msg.arg1 = forcedRestartReason;
        mainapp.comm_msg_handler.sendMessage(msg);
    }

    private void startNewSpeedTrapActivity() {
        // remove old handlers since the new Intent will have its own
        isRestarting = true;        // tell OnDestroy to skip removing handlers since it will run after the new Intent is created
        removeHandlers();

        //end current Intent then start the new Intent
        Intent newCvProgrammer = new Intent().setClass(this, speed_trap.class);
        this.finish();
        startActivity(newCvProgrammer);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }


//**************************************************************************************

    private void updateScaleSpeed(String id, String status) {
        Log.d("EX_Toolbox", "updateScaleSpeed id=" + id + " status=" + status);
        if ( (runState == DISABLED) || (status.equals("0")) ) return;

        int pinId = Integer.parseInt(id);
        if ( (pinId == startPin) && ( (runState == ENABLED) || ( runState == RUN_FINISHED)) ) {
            if ( runState == RUN_FINISHED) {
                if (System.currentTimeMillis() > endTime + delayMilliseconds ) {
                    runState = ENABLED;
                } else {
                    long wait = (int) ((endTime + delayMilliseconds - System.currentTimeMillis()) / 1000);
                    dccexInfoStr = String.format(getApplicationContext().getResources().getString(R.string.dccexSpeedTrapRunFinishedWait), wait);
                    refreshDccexView();
                }
            }
            if (runState == ENABLED) {
                startTime = System.currentTimeMillis();
                endTime = 0;
                runState = RUN_STARTED;
                if (waitTimer!=null) waitTimer.cancel();
                dccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexSpeedTrapRunStarted);
                refreshDccexView();
                Log.d("EX_Toolbox", "updateScaleSpeed startTime=" + startTime);
            }
        } else if ( (pinId == endPin) && (runState == RUN_STARTED) )  {
            endTime = System.currentTimeMillis();
            runTime = (endTime - startTime) / 1000.0;
            refreshScaleSpeed();
            runState = RUN_FINISHED;
            if (delayMilliseconds>0) {
                waitTimer = new MyCountDownTimer(delayMilliseconds, 1000);
                waitTimer.start();
                dccexInfoStr = String.format(getApplicationContext().getResources().getString(R.string.dccexSpeedTrapRunFinished),delay);
            } else {
                dccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexSpeedTrapRunWaitingToStart);
            }
            refreshDccexView();
            Log.d("EX_Toolbox", "updateScaleSpeed endTime=" + endTime + " runTime=" + runTime);
        }

        // otherwise ignore
    }

    private void refreshScaleSpeed() {
        if (runState != DISABLED) {
            double speedCmPerSec = distance / runTime;
            double speedKmPerHour = speedCmPerSec * 3600 / 100000;
            double scaleSpeedKph = speedKmPerHour / scaleRatio;
            double scaleSpeedMph = speedKmPerHour * 0.621371 / scaleRatio;
            scaleSpeedText = String.format("kph: %.2f  mph: %.2f    (%.3fs)", scaleSpeedKph, scaleSpeedMph, runTime);
            dccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexSpeedTrapRunFinished);
            refreshDccexView();
        }
    }

    public class ClearCommandsButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            dccexInfoStr = "";
            mainapp.DccexResponsesListHtml.clear();
            mainapp.dccexSendsListHtml.clear();
            mainapp.dccexResponsesStr = "";
            mainapp.dccexSendsStr = "";
            refreshDccexView();
        }
    }

    public class StartButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            runState = ENABLED;
            dccexInfoStr = "";
            resetTextFields();
            mainapp.buttonVibration();
            if (waitTimer!=null) waitTimer.cancel();
            dccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexSpeedTrapRunWaitingToStart);
            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    @SuppressLint("ApplySharedPref")
    public class SwapButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            int pin = startPin;
            String pinText = startPinText;

            startPinText = endPinText;
            startPin = endPin;
            etDccexStartPin.setText(startPinText);
            prefs.edit().putString("prefSpeedTrapStartPin", startPinText).commit();

            endPinText = pinText;
            endPin = pin;
            etDccexEndPin.setText(endPinText);
            prefs.edit().putString("prefSpeedTrapEndPin", endPinText).commit();

            mainapp.buttonVibration();
            refreshDccexView();
            mainapp.hideSoftKeyboard(v);
        }
    }

    private void witRetry(String s) {
        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
        startActivity(in);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    private void resetTextFields() {
        scaleSpeedText = "";
        tvScaleSpeed.setText("");
    }

    @SuppressLint("ApplySharedPref")
    private void readTextFields() {
        startPinText = etDccexStartPin.getText().toString();
        try {
            startPin = Integer.valueOf(startPinText);
            prefs.edit().putString("prefSpeedTrapStartPin", startPinText).commit();
        } catch (Exception e) {
            // ignore for now
        }
        endPinText = etDccexEndPin.getText().toString();
        try {
            endPin = Integer.valueOf(endPinText);
            prefs.edit().putString("prefSpeedTrapEndPin", endPinText).commit();
        } catch (Exception e) {
            // ignore for now
        }
        distanceText = etDccexDistance.getText().toString();
        try {
            distance = Double.valueOf(distanceText);
            prefs.edit().putString("prefSpeedTrapDistance", distanceText).commit();
        } catch (Exception e) {
            // ignore for now
        }
        delayText = etDccexDelay.getText().toString();
        try {
            delay = Long.valueOf(delayText);
            delayMilliseconds = delay * 1000;
            prefs.edit().putString("prefSpeedTrapDelay", delayText).commit();
        } catch (Exception e) {
            // ignore for now
        }
    }

    private void showHideButtons() {
    }

    public void refreshDccexView() {
        dccexWriteInfoLabel.setText(dccexInfoStr);
        try {
            etDccexStartPin.setText(startPinText);
            etDccexEndPin.setText(endPinText);
            etDccexDistance.setText(distanceText);
            etDccexDelay.setText(delayText);
            tvScaleSpeed.setText(scaleSpeedText);
            dccexWriteInfoLabel.setText(dccexInfoStr);

        } catch (Exception e) {
            Log.e("EX_Toolbox", "refreshDccexView: object not available on resume, yet");
        }

        showHideButtons();
    }


    public class ScalesSpinnerListener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mainapp.exitDoubleBackButtonInitiated = 0;

            scalesIndex = scalesSpinner.getSelectedItemPosition();
            try {
                scaleRatio = Double.parseDouble(scaleRatiosArray[scalesIndex]);
                prefs.edit().putString("prefSpeedTrapScalesIndex", String.valueOf(scalesIndex)).commit();
                refreshScaleSpeed();
            } catch (Exception e) {
                // do nothing for now
            }
            dccexInfoStr = "";

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

    public class UnitsSpinnerListener implements AdapterView.OnItemSelectedListener {

        @SuppressLint("ApplySharedPref")
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mainapp.exitDoubleBackButtonInitiated = 0;

            unitsIndex = unitsSpinner.getSelectedItemPosition();
            if (unitsIndex > 0) {
                try {
                    unitsRatio = Double.parseDouble(unitsRatiosArray[scalesIndex]);
                } catch (Exception e) {
                    // do nothing for now
                }
            }
            dccexInfoStr = "";

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

    public void refreshDccexCommandsView() {
        dccexResponsesLabel.setText(Html.fromHtml(mainapp.dccexResponsesStr));
        dccexSendsLabel.setText(Html.fromHtml(mainapp.dccexSendsStr));
    }

    private class MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
//            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.KIDS_TIMER_START, "", 0, 0);
        }

        @Override
        public void onFinish() {  // When timer is finished
            dccexInfoStr = getApplicationContext().getResources().getString(R.string.dccexSpeedTrapRunWaitingToStart);
            refreshDccexView();
        }

        @Override
        public void onTick(long millisUntilFinished) {   // millisUntilFinished    The amount of time until finished.
            long wait = (int) (millisUntilFinished / 1000);
            dccexInfoStr = String.format(getApplicationContext().getResources().getString(R.string.dccexSpeedTrapRunFinished), wait);
            refreshDccexView();
        }
    }
}
