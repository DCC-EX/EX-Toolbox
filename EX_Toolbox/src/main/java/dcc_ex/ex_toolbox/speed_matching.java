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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import dcc_ex.ex_toolbox.logviewer.ui.LogViewerActivity;
import dcc_ex.ex_toolbox.type.message_type;
import dcc_ex.ex_toolbox.util.LocaleHelper;

public class speed_matching extends AppCompatActivity implements GestureOverlayView.OnGestureListener {

    private threaded_application mainapp;  // hold pointer to mainapp
//    private SharedPreferences prefs;

    private Menu tMenu;
//    private static boolean savedMenuSelected;

    protected GestureOverlayView ov;
    // these are used for gesture tracking
    private float gestureStartX = 0;
    private float gestureStartY = 0;
    protected boolean gestureInProgress = false; // gesture is in progress
    private long gestureLastCheckTime; // time in milliseconds that velocity was last checked
    private static final long gestureCheckRate = 200; // rate in milliseconds to check velocity
    private VelocityTracker mVelocityTracker;

    //**************************************

//    private LinearLayout DccexWriteInfoLayout;
    private TextView DccexWriteInfoLabel;
    private String DccexInfoStr = "";

    private TextView DccexResponsesLabel;
    private TextView DccexSendsLabel;
//    private ScrollView DccexResponsesScrollView;
//    private ScrollView DccexSendsScrollView;

    Button clearCommandsButton;

//    static final int WHICH_ADDRESS = 0;
//    static final int WHICH_CV = 1;
//    static final int WHICH_CV_VALUE = 2;
//    static final int WHICH_COMMAND = 3;

    Button[] writeButtons = {null, null, null, null, null, null};  // LOW, MID, HIGH, Acceleration, Deceleration, Kick Start
    Button[] writeMinusButtons = {null, null, null, null, null, null};
    Button[] writePlusButtons = {null, null, null, null, null, null};

    Button[] setSpeedButtons = {null, null, null, null};  // LOW, MID, HIGH, STOP
    Button setDirectionButton = null;
    TextView tvSetDirection;
    Button setStep1Button;
    Button setStep5Button;
    Button setStep10Button;

    EditText[] valsEditText = {null, null, null, null, null, null};  // LOW, MID, HIGH, Acceleration, Deceleration, Kick Start
    int [] vals = {0, 0, 0, 0, 0, 0};  // LOW, MID, HIGH, Acceleration, Deceleration, Kick Start

    EditText stepValueEditText;
    int stepValue;

    EditText locoAddrMasterEditText;
    EditText locoAddrSecondEditText;
    int locoAddrMaster = 0;
    int locoAddrSecond = 0;
    int locoDirection = 1;

    static int FORWARD = 1;
    static int REVERSE = 0;

    static int LOW = 0;
    static int MID = 1;
    static int HIGH = 2;
    static int STOP = 3;
    static int ACCEL = 3;  //note: same as Stop. NOt used in the same arrays as stop
    static int DECEL = 4;
    static int KICK = 5;
    int[] speedCVs = {2, 6, 5, 3, 4, 65};  // LOW, MID, HIGH, Acceleration, Deceleration, Kick Start
    int[] speeds = {5, 63, 126, 0};  // LOW, MID, HIGH, STOP

    float vn = 4; // DCC-EC Version number

    private String cv29SpeedSteps;
    private String cv29AnalogueMode;
    private String cv29Direction;
    private String cv29AddressSize;
    private String cv29SpeedTable;

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
        if (mainapp.speed_matching_msg_handler != null)
            mainapp.speed_matching_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
    }

    public void gestureMove(MotionEvent event) {
        // Log.d("EX_Toolbox", "gestureMove action " + event.getAction());
        if ( (mainapp != null) && (mainapp.speed_matching_msg_handler != null) && (gestureInProgress) ) {
            // stop the gesture timeout timer
            mainapp.speed_matching_msg_handler.removeCallbacks(gestureStopped);

            mVelocityTracker.addMovement(event);
            if ((event.getEventTime() - gestureLastCheckTime) > gestureCheckRate) {
                // monitor velocity and fail gesture if it is too low
                gestureLastCheckTime = event.getEventTime();
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000);
                int velocityX = (int) velocityTracker.getXVelocity();
                int velocityY = (int) velocityTracker.getYVelocity();
                // Log.d(""EX_Toolbox", "gestureVelocity vel " + velocityX);
                if ((Math.abs(velocityX) < threaded_application.min_fling_velocity) && (Math.abs(velocityY) < threaded_application.min_fling_velocity)) {
                    gestureFailed(event);
                }
            }
            if (gestureInProgress) {
                // restart the gesture timeout timer
                mainapp.speed_matching_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
            }
        }
    }

    private void gestureEnd(MotionEvent event) {
        // Log.d(""EX_Toolbox", "gestureEnd action " + event.getAction() + " inProgress? " + gestureInProgress);
        if ( (mainapp != null) && (mainapp.speed_matching_msg_handler != null) && (gestureInProgress) ) {
            mainapp.speed_matching_msg_handler.removeCallbacks(gestureStopped);

            float deltaX = (event.getX() - gestureStartX);
            float absDeltaX =  Math.abs(deltaX);
            if (absDeltaX > threaded_application.min_fling_distance) { // only process left/right swipes
                // valid gesture. Change the event action to CANCEL so that it isn't processed by any control below the gesture overlay
                event.setAction(MotionEvent.ACTION_CANCEL);
                // process swipe in the direction with the largest change
                Intent nextScreenIntent = mainapp.getNextIntentInSwipeSequence(threaded_application.SCREEN_SWIPE_INDEX_SPEED_MATCHING, deltaX);
                startACoreActivity(this, nextScreenIntent, true, deltaX);
            } else {
                // gesture was not long enough
                gestureFailed(event);
            }
        }
    }

    private void gestureCancel(MotionEvent event) {
        if (mainapp.speed_matching_msg_handler != null)
            mainapp.speed_matching_msg_handler.removeCallbacks(gestureStopped);
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
    class speed_matching_handler extends Handler {

        public speed_matching_handler(Looper looper) {
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
                case message_type.RECEIVED_CV:
                    String cvResponseStr = msg.obj.toString();
                    if (cvResponseStr.length() > 0) {
                        String[] cvArgs = cvResponseStr.split("(\\|)");
                        int cv = Integer.decode(cvArgs[0]);
                        if ( !(cvArgs[1].charAt(0)=='-') ) {
                            if (cv==speedCVs[LOW]) {
                                vals[LOW] = Integer.decode(cvArgs[0]);
                                valsEditText[LOW].setText(cvArgs[1]);
                            } else if (cv==speedCVs[MID]) {
                                vals[MID] = Integer.decode(cvArgs[0]);
                                valsEditText[MID].setText(cvArgs[1]);
                            } else if (cv==speedCVs[HIGH]) {
                                vals[HIGH] = Integer.decode(cvArgs[0]);
                                valsEditText[HIGH].setText(cvArgs[1]);
                            } else if (cv==speedCVs[ACCEL]) {
                                vals[ACCEL] = Integer.decode(cvArgs[0]);
                                valsEditText[ACCEL].setText(cvArgs[1]);
                            } else if (cv==speedCVs[DECEL]) {
                                vals[DECEL] = Integer.decode(cvArgs[0]);
                                valsEditText[DECEL].setText(cvArgs[1]);
                            } else if (cv==speedCVs[KICK]) {
                                vals[KICK] = Integer.decode(cvArgs[0]);
                                valsEditText[KICK].setText(cvArgs[1]);
                            } else if (cv==29) {
                                checkCv29(cvArgs[0], cvArgs[1]);
                            } else {
                                break;
                            }
                            refreshDccexView();
                        }
                    }
                    break;
                case message_type.RECEIVED_DECODER_ADDRESS:
                    String response_str = msg.obj.toString();
                    if ( (response_str.length() > 0) && !(response_str.charAt(0)=='-') ) {  //refresh address
                        locoAddrSecond = Integer.decode(response_str);
                        locoAddrSecondEditText.setText(response_str);
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
                    getApplicationContext().getResources().getString(R.string.app_name_speed_matching_short),
                    mainapp.getFastClockTime());
        else
            mainapp.setToolbarTitle(toolbar,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_speed_matching),
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
        Log.d("EX_Toolbox", "web_activity.onCreate()");

        mainapp = (threaded_application) this.getApplication();
//        prefs = getSharedPreferences("dcc_ex.ex_toolbox_preferences", 0);
        mainapp.applyTheme(this);

        super.onCreate(savedInstanceState);

        if (mainapp.isForcingFinish()) {        // expedite
            return;
        }

        setContentView(R.layout.speed_matching);

        mainapp.loadBackgroundImage(findViewById(R.id.speedMatchingBackgroundImgView));

        //put pointer to this activity's handler in main app's shared variable
        mainapp.speed_matching_msg_handler = new speed_matching_handler(Looper.getMainLooper());

        //Set the buttons

//        DccexWriteInfoLayout = findViewById(R.id.ex_DccexWriteInfoLayout);
        DccexWriteInfoLabel = findViewById(R.id.ex_DccexWriteInfoLabel);
        DccexWriteInfoLabel.setText("");

        DccexResponsesLabel = findViewById(R.id.ex_DccexResponsesLabel);
        DccexResponsesLabel.setText("");
        DccexSendsLabel = findViewById(R.id.ex_DccexSendsLabel);
        DccexSendsLabel.setText("");

//        DccexResponsesScrollView = findViewById(R.id.ex_DccexResponsesScrollView);
//        DccexSendsScrollView = findViewById(R.id.ex_DccexSendsScrollView);

        clearCommandsButton = findViewById(R.id.ex_dccexClearCommandsButton);
        clear_commands_button_listener clearCommandsClickListener = new clear_commands_button_listener();
        clearCommandsButton.setOnClickListener(clearCommandsClickListener);

        // ********************************

        valsEditText[LOW] = findViewById(R.id.ex_DccexSpeedMatchingLowValue);
        valsEditText[MID] = findViewById(R.id.ex_DccexSpeedMatchingMidValue);
        valsEditText[HIGH] = findViewById(R.id.ex_DccexSpeedMatchingHighValue);

        locoAddrMasterEditText = findViewById(R.id.ex_DccexSpeedMatchingMasterAddrValue);
        locoAddrSecondEditText = findViewById(R.id.ex_DccexSpeedMatchingSecondAddrValue);

        stepValueEditText = findViewById(R.id.ex_DccexSpeedMatchingStepValue);

        valsEditText[ACCEL] = findViewById(R.id.ex_DccexSpeedMatchingAccelerationValue);
        valsEditText[DECEL] = findViewById(R.id.ex_DccexSpeedMatchingDecelerationValue);
        valsEditText[KICK] = findViewById(R.id.ex_DccexSpeedMatchingKickStartValue);

        // ********************************

        writeButtons[LOW] = findViewById(R.id.ex_DccexSpeedMatchingLowWriteButton);
        WriteButtonListener writeButtonListener = new WriteButtonListener(LOW, 0);
        writeButtons[LOW].setOnClickListener(writeButtonListener);

        writeButtons[MID] = findViewById(R.id.ex_DccexSpeedMatchingMidWriteButton);
        writeButtonListener = new WriteButtonListener(MID, 0);
        writeButtons[MID].setOnClickListener(writeButtonListener);

        writeButtons[HIGH] = findViewById(R.id.ex_DccexSpeedMatchingHighWriteButton);
        writeButtonListener = new WriteButtonListener(HIGH, 0);
        writeButtons[HIGH].setOnClickListener(writeButtonListener);

        writeButtons[ACCEL] = findViewById(R.id.ex_DccexSpeedMatchingAccelerationWriteButton);
        writeButtonListener = new WriteButtonListener(ACCEL, 0);
        writeButtons[ACCEL].setOnClickListener(writeButtonListener);

        writeButtons[DECEL] = findViewById(R.id.ex_DccexSpeedMatchingDecelerationWriteButton);
        writeButtonListener = new WriteButtonListener(DECEL, 0);
        writeButtons[DECEL].setOnClickListener(writeButtonListener);

        writeButtons[KICK] = findViewById(R.id.ex_DccexSpeedMatchingKickStartWriteButton);
        writeButtonListener = new WriteButtonListener(KICK, 0);
        writeButtons[KICK].setOnClickListener(writeButtonListener);
        // ********************************

        writeMinusButtons[LOW] = findViewById(R.id.ex_DccexSpeedMatchingLowWriteMinusButton);
        writeButtonListener = new WriteButtonListener(LOW, -1);
        writeMinusButtons[LOW].setOnClickListener(writeButtonListener);

        writeMinusButtons[MID] = findViewById(R.id.ex_DccexSpeedMatchingMidWriteMinusButton);
        writeButtonListener = new WriteButtonListener(MID, -1);
        writeMinusButtons[MID].setOnClickListener(writeButtonListener);

        writeMinusButtons[HIGH] = findViewById(R.id.ex_DccexSpeedMatchingHighWriteMinusButton);
        writeButtonListener = new WriteButtonListener(HIGH, -1);
        writeMinusButtons[HIGH].setOnClickListener(writeButtonListener);

        writeMinusButtons[ACCEL] = findViewById(R.id.ex_DccexSpeedMatchingAccelerationWriteMinusButton);
        writeButtonListener = new WriteButtonListener(ACCEL, -1);
        writeMinusButtons[ACCEL].setOnClickListener(writeButtonListener);

        writeMinusButtons[DECEL] = findViewById(R.id.ex_DccexSpeedMatchingDecelerationWriteMinusButton);
        writeButtonListener = new WriteButtonListener(DECEL, -1);
        writeMinusButtons[DECEL].setOnClickListener(writeButtonListener);

        writeMinusButtons[KICK] = findViewById(R.id.ex_DccexSpeedMatchingKickStartWriteMinusButton);
        writeButtonListener = new WriteButtonListener(KICK, -1);
        writeMinusButtons[KICK].setOnClickListener(writeButtonListener);

        // ********************************

        writePlusButtons[LOW] = findViewById(R.id.ex_DccexSpeedMatchingLowWritePlusButton);
        writeButtonListener = new WriteButtonListener(LOW, 1);
        writePlusButtons[LOW].setOnClickListener(writeButtonListener);

        writePlusButtons[MID] = findViewById(R.id.ex_DccexSpeedMatchingMidWritePlusButton);
        writeButtonListener = new WriteButtonListener(MID, 1);
        writePlusButtons[MID].setOnClickListener(writeButtonListener);

        writePlusButtons[HIGH] = findViewById(R.id.ex_DccexSpeedMatchingHighWritePlusButton);
        writeButtonListener = new WriteButtonListener(HIGH, 1);
        writePlusButtons[HIGH].setOnClickListener(writeButtonListener);

        writePlusButtons[ACCEL] = findViewById(R.id.ex_DccexSpeedMatchingAccelerationWritePlusButton);
        writeButtonListener = new WriteButtonListener(ACCEL, 1);
        writePlusButtons[ACCEL].setOnClickListener(writeButtonListener);

        writePlusButtons[DECEL] = findViewById(R.id.ex_DccexSpeedMatchingDecelerationWritePlusButton);
        writeButtonListener = new WriteButtonListener(DECEL, 1);
        writePlusButtons[DECEL].setOnClickListener(writeButtonListener);

        writePlusButtons[KICK] = findViewById(R.id.ex_DccexSpeedMatchingKickStartWritePlusButton);
        writeButtonListener = new WriteButtonListener(KICK, 1);
        writePlusButtons[KICK].setOnClickListener(writeButtonListener);

        // ********************************

        setSpeedButtons[LOW] = findViewById(R.id.ex_DccexSpeedMatchingLowSetSpeedButton);
        SetSpeedButtonListener setSpeedButtonListener = new SetSpeedButtonListener(LOW);
        setSpeedButtons[LOW].setOnClickListener(setSpeedButtonListener);

        setSpeedButtons[MID] = findViewById(R.id.ex_DccexSpeedMatchingMidSetSpeedButton);
        setSpeedButtonListener = new SetSpeedButtonListener(MID);
        setSpeedButtons[MID].setOnClickListener(setSpeedButtonListener);

        setSpeedButtons[HIGH] = findViewById(R.id.ex_DccexSpeedMatchingHighSetSpeedButton);
        setSpeedButtonListener = new SetSpeedButtonListener(HIGH);
        setSpeedButtons[HIGH].setOnClickListener(setSpeedButtonListener);

        setSpeedButtons[STOP] = findViewById(R.id.ex_DccexSpeedMatchingStopButton);
        setSpeedButtonListener = new SetSpeedButtonListener(STOP);
        setSpeedButtons[STOP].setOnClickListener(setSpeedButtonListener);

        setDirectionButton = findViewById(R.id.ex_DccexSpeedMatchingDirectionButton);
        SetDirectionButtonListener setDirectionButtonListener = new SetDirectionButtonListener();
        setDirectionButton.setOnClickListener(setDirectionButtonListener);
        tvSetDirection = findViewById(R.id.ex_DccexSpeedMatchingDirection);

        // ********************************

        setStep1Button = findViewById(R.id.ex_DccexSpeedMatchingStep1Button);
        SetStepButtonListener setStepButtonListener = new SetStepButtonListener(1);
        setStep1Button.setOnClickListener(setStepButtonListener);

        setStep5Button = findViewById(R.id.ex_DccexSpeedMatchingStep5Button);
        setStepButtonListener = new SetStepButtonListener(5);
        setStep5Button.setOnClickListener(setStepButtonListener);

        setStep10Button = findViewById(R.id.ex_DccexSpeedMatchingStep10Button);
        setStepButtonListener = new SetStepButtonListener(10);
        setStep10Button.setOnClickListener(setStepButtonListener);

        // ********************************

        Button readSecondLocoButton = findViewById(R.id.ex_DccexSpeedMatchingReadSecondButton);
        ReadSecondButtonListener buttonListener = new ReadSecondButtonListener();
        readSecondLocoButton.setOnClickListener(buttonListener);

        // ********************************

        mainapp.getCommonPreferences();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

    } // end onCreate

    @Override
    public void onResume() {
        Log.d("EX_Toolbox", "speed_matching.onResume() called");
        mainapp.applyTheme(this);

        super.onResume();

        mainapp.getCommonPreferences();

        setActivityTitle();
        mainapp.dccexScreenIsOpen = true;
        mainapp.activeScreen = mainapp.ACTIVE_SCREEN_SPEED_MATCHING;
        refreshDccexView();

        if (mainapp.isForcingFinish()) {    //expedite
            this.finish();
            return;
        }

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.TIME_CHANGED);    // request time update
        CookieSyncManager.getInstance().startSync();

        // enable swipe/fling detection if enabled in Prefs
        ov = findViewById(R.id.speed_matching_overlay);
        ov.addOnGestureListener(this);
        ov.setEventsInterceptionEnabled(true);
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    @Override
    public void onPause() {
        Log.d("EX_Toolbox", "track_manager.onPause() called");
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("EX_Toolbox", "track_manager.onStart() called");
        // put pointer to this activity's handler in main app's shared variable
        if (mainapp.speed_matching_msg_handler == null)
            mainapp.speed_matching_msg_handler = new speed_matching_handler(Looper.getMainLooper());
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
        Log.d("EX_Toolbox", "speed_matching.onDestroy() called");

        if (mainapp.speed_matching_msg_handler !=null) {
            mainapp.speed_matching_msg_handler.removeCallbacksAndMessages(null);
            mainapp.speed_matching_msg_handler = null;
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

    //Handle pressing of the back button to end this activity
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK) {
            if (mainapp.speed_matching_msg_handler!=null) {
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
        inflater.inflate(R.menu.speed_matching_menu, menu);
        tMenu = menu;

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
        }
    }

    @SuppressLint("ApplySharedPref")
    public void forceRestartApp(int forcedRestartReason) {
        Log.d("EX-Toolbox", "speed_matching.forceRestartApp() ");
        Message msg = Message.obtain();
        msg.what = message_type.RESTART_APP;
        msg.arg1 = forcedRestartReason;
        mainapp.comm_msg_handler.sendMessage(msg);
    }

//**************************************************************************************

    public class WriteButtonListener implements View.OnClickListener {
        int myWhich;
        int myAdjust;

        public WriteButtonListener(int which, int adjust) {
            myWhich = which;
            myAdjust = adjust;
        }

        public void onClick(View v) {
            mainapp.buttonVibration();
            getLocoAddresses();
            getStepValue();
            mainapp.hideSoftKeyboard(v);
            if (locoAddrSecond>0) {
                String cvValueStr = valsEditText[myWhich].getText().toString();
                try {
                    int cvVal = Integer.decode(cvValueStr);
                    if (myAdjust != 0) {
                        cvVal = cvVal + (stepValue * myAdjust);
                        if (cvVal<0) cvVal = 0;
                        else if (cvVal>255) cvVal = 255;
                        valsEditText[myWhich].setText(""+cvVal);
                        vals[myWhich] = cvVal;
                        }
                    if ( (cvVal >= 0) && (cvVal <= 255)) {
                        mainapp.buttonVibration();
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_POM_CV, speedCVs[myWhich] + " " + cvVal, locoAddrSecond);
                        refreshDccexView();
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public class SetSpeedButtonListener implements View.OnClickListener {
        int myWhich;

        public SetSpeedButtonListener(int which) {
            myWhich = which;
        }

        public void onClick(View v) {
            mainapp.buttonVibration();
            getLocoAddresses();
            mainapp.hideSoftKeyboard(v);

            if (locoAddrMaster>0) {
                mainapp.buttonVibration();
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SET_SPEED_DIRECT, "" + speeds[myWhich] + " " + locoDirection, locoAddrMaster);
            }
            if (locoAddrSecond>0) {
                mainapp.buttonVibration();
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SET_SPEED_DIRECT, "" + speeds[myWhich] + " " + locoDirection, locoAddrSecond);
            }
        }
    }

public class SetDirectionButtonListener implements View.OnClickListener {

        public void onClick(View v) {
            mainapp.buttonVibration();
            getLocoAddresses();
            mainapp.hideSoftKeyboard(v);

            if (locoDirection==FORWARD) {
                locoDirection=REVERSE;
                tvSetDirection.setText(getApplicationContext().getResources().getString(R.string.speedMatchDirectionReverse));
            } else {
                locoDirection=FORWARD;
                tvSetDirection.setText(getApplicationContext().getResources().getString(R.string.speedMatchDirectionForward));
            }
        }
    }

    public class SetStepButtonListener implements View.OnClickListener {
        int myStep;

        public SetStepButtonListener(int step) {
            myStep = step;
        }

        public void onClick(View v) {
            mainapp.buttonVibration();
            getLocoAddresses();
            mainapp.hideSoftKeyboard(v);

            stepValueEditText.setText(""+myStep);
            stepValue = myStep;
        }
    }



    public class ReadSecondButtonListener implements View.OnClickListener {

        public void onClick(View v) {
            mainapp.hideSoftKeyboard(v);
            mainapp.buttonVibration();
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_CV, "", speedCVs[0]);
            mainapp.sendMsgDelay(mainapp.comm_msg_handler, 3000L, message_type.REQUEST_CV, "", speedCVs[1], 0);
            mainapp.sendMsgDelay(mainapp.comm_msg_handler, 6000L, message_type.REQUEST_CV, "", speedCVs[2], 0);
            mainapp.sendMsgDelay(mainapp.comm_msg_handler, 9000L, message_type.REQUEST_CV, "", speedCVs[3], 0);
            mainapp.sendMsgDelay(mainapp.comm_msg_handler, 12000L, message_type.REQUEST_CV, "", speedCVs[4], 0);
            mainapp.sendMsgDelay(mainapp.comm_msg_handler, 15000L, message_type.REQUEST_CV, "", speedCVs[5], 0);
            mainapp.sendMsgDelay(mainapp.comm_msg_handler, 18000L, message_type.REQUEST_DECODER_ADDRESS, "", 0, 0);
            mainapp.sendMsgDelay(mainapp.comm_msg_handler, 21000L, message_type.REQUEST_CV, "", 29, 0);
            refreshDccexView();

        }
    }

    void getLocoAddresses() {
        locoAddrMaster = 0;
        locoAddrSecond = 0;
        String addrValueStr = locoAddrMasterEditText.getText().toString();
        try {
            locoAddrMaster = Integer.decode(addrValueStr);
        } catch (Exception ignored) {
        }
        addrValueStr = locoAddrSecondEditText.getText().toString();
        try {
            locoAddrSecond = Integer.decode(addrValueStr);
        } catch (Exception ignored) {
        }

    }

    void getStepValue() {
        stepValue = 1;
        String stepValueStr = stepValueEditText.getText().toString();
        try {
            stepValue = Integer.decode(stepValueStr);
        } catch (Exception ignored) {
        }
    }

    // *********************************

    public class clear_commands_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.DccexResponsesListHtml.clear();
            mainapp.dccexSendsListHtml.clear();
            mainapp.dccexResponsesStr = "";
            mainapp.dccexSendsStr = "";
            refreshDccexView();
        }
    }

    public void refreshDccexView() {
        DccexWriteInfoLabel.setText(DccexInfoStr);
        refreshDccexCommandsView();
    }

    public void refreshDccexCommandsView() {
        DccexResponsesLabel.setText(Html.fromHtml(mainapp.dccexResponsesStr));
        DccexSendsLabel.setText(Html.fromHtml(mainapp.dccexSendsStr));
    }

    void checkCv29(String cv, String cvValueStr) {
        if ( (cv.equals("29")) && (mainapp.activeScreen==mainapp.ACTIVE_SCREEN_SPEED_MATCHING) ) {
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

                if (mainapp.bitExtracted(cvValue,1,5)==1) {
                    mainapp.dccexResponsesStr = "<p>"
                            + String.format(getApplicationContext().getResources().getString(R.string.cv29SpeedTableDisable),
                            mainapp.toggleBit(cvValue, 5))
                            + "</p>" + mainapp.dccexResponsesStr;
                }

//                refreshDccexView();

            } catch (Exception e) {
                Log.e("EX_Toolbox", "Error processing cv29: " + e.getMessage());
            }
        }
    }
}
