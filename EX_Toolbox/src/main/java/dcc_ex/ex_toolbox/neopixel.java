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
import android.graphics.Color;
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
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import dcc_ex.ex_toolbox.logviewer.ui.LogViewerActivity;
import dcc_ex.ex_toolbox.type.message_type;
import dcc_ex.ex_toolbox.util.HorizontalSeekBar;
import dcc_ex.ex_toolbox.util.LocaleHelper;

public class neopixel extends AppCompatActivity implements GestureOverlayView.OnGestureListener {

    private threaded_application mainapp;  // hold pointer to mainapp
    private SharedPreferences prefs;

    private Menu tMenu;
    private static boolean savedMenuSelected;

    int vpinValue = 0;
    TextView vpinValueField;
    int vpinCountValue = 0;
    TextView vpinCountValueField;
    TextView sampleLabel;

    HorizontalSeekBar redSeekbar;
    HorizontalSeekBar greenSeekbar;
    HorizontalSeekBar blueSeekbar;

    int red = -1;
    int green = -1;
    int blue = -1;

    int lastRedSeekbarSliderPositionSent = -1;
    int lastGreenSeekbarSliderPositionSent = -1;
    int lastBlueSeekbarSliderPositionSent = -1;

    protected Handler colorSliderRptHandler  = new Handler();
    boolean isRepeatRunning = false;

    public interface color_type {
        int RED = 0;
        int GREEN = 1;
        int BLUE = 2;
    }

    int autoIncrementDecrement = 0;

    int slidersStartX=0;
    int slidersStartY=0;
    int slidersEndX=0;
    int slidersEndY=0;

    Button onButton;
    Button offButton;
    Button[] plusMinusButtons = {null,null,null,null,null,null};

    protected GestureOverlayView ov;
    // these are used for gesture tracking
    private float gestureStartX = 0;
    private float gestureStartY = 0;
    protected boolean gestureInProgress = false; // gesture is in progress
    private long gestureLastCheckTime; // time in milliseconds that velocity was last checked
    private static final long gestureCheckRate = 200; // rate in milliseconds to check velocity
    private VelocityTracker mVelocityTracker;
    boolean foundSliderOverlayArea = false;

    //**************************************

    private LinearLayout DccexWriteInfoLayout;
    private TextView DccexWriteInfoLabel;
    private String DccexInfoStr = "";

    private TextView DccexResponsesLabel;
    private TextView DccexSendsLabel;
    private ScrollView DccexResponsesScrollView;
    private ScrollView DccexSendsScrollView;

    Button clearCommandsButton;

    //**************************************

    private LinearLayout screenNameLine;
    private Toolbar toolbar;
    private LinearLayout statusLine;

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
        getSliderOverlayArea();
        gestureStartX = event.getX();
        gestureStartY = event.getY();
//        Log.d("EX_Toolbox", "gestureStart x=" + gestureStartX + " y=" + gestureStartY);

        if ( (gestureStartX>=slidersStartX) && (gestureStartY >= slidersStartY)
                && (gestureStartX <= slidersEndX) && (gestureStartY <= slidersEndY) ) {
            gestureInProgress = false;
            return;
        }

        gestureInProgress = true;
        gestureLastCheckTime = event.getEventTime();
        mVelocityTracker.clear();

        // start the gesture timeout timer
        if (mainapp.neopixel_msg_handler != null)
            mainapp.neopixel_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
    }

    public void gestureMove(MotionEvent event) {
        // Log.d(""EX_Toolbox", "gestureMove action " + event.getAction());
        if ( (mainapp != null) && (mainapp.neopixel_msg_handler != null) && (gestureInProgress) ) {
            // stop the gesture timeout timer
            mainapp.neopixel_msg_handler.removeCallbacks(gestureStopped);

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
                mainapp.neopixel_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
            }
        }
    }

    private void gestureEnd(MotionEvent event) {
        // Log.d(""EX_Toolbox", "gestureEnd action " + event.getAction() + " inProgress? " + gestureInProgress);
        if ( (mainapp != null) && (mainapp.neopixel_msg_handler != null) && (gestureInProgress) ) {
            mainapp.neopixel_msg_handler.removeCallbacks(gestureStopped);

            float deltaX = (event.getX() - gestureStartX);
            float absDeltaX =  Math.abs(deltaX);
            if (absDeltaX > threaded_application.min_fling_distance) { // only process left/right swipes
                // valid gesture. Change the event action to CANCEL so that it isn't processed by any control below the gesture overlay
                event.setAction(MotionEvent.ACTION_CANCEL);
                // process swipe in the direction with the largest change
                Intent nextScreenIntent = mainapp.getNextIntentInSwipeSequence(threaded_application.SCREEN_SWIPE_INDEX_NEOPIXEL, deltaX);
                startACoreActivity(this, nextScreenIntent, true, deltaX);
            } else {
                // gesture was not long enough
                gestureFailed(event);
            }
        }
    }

    private void gestureCancel(MotionEvent event) {
        if (mainapp.neopixel_msg_handler != null)
            mainapp.neopixel_msg_handler.removeCallbacks(gestureStopped);
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
    class neopixel_handler extends Handler {

        public neopixel_handler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            String s;
            switch (msg.what) {

                case message_type.RESPONSE: {    //handle messages from server
                    s = msg.obj.toString();
//                    String response_str = s.substring(0, Math.min(s.length(), 2));

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
                case message_type.DCCEX_RESPONSE:  // informational response
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
                    getApplicationContext().getResources().getString(R.string.app_name_neopixel_short),
                    mainapp.getFastClockTime());
        else
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_neopixel),
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
        prefs = getSharedPreferences("dcc_ex.ex_toolbox_preferences", 0);
        mainapp.applyTheme(this);

        super.onCreate(savedInstanceState);

        if (mainapp.isForcingFinish()) {        // expedite
            return;
        }

        setContentView(R.layout.neopixel);

        //put pointer to this activity's handler in main app's shared variable
        mainapp.neopixel_msg_handler = new neopixel_handler(Looper.getMainLooper());

        mainapp.loadBackgroundImage(findViewById(R.id.neopixelBackgroundImgView));

        DccexWriteInfoLayout = findViewById(R.id.ex_DccexWriteInfoLayout);
        DccexWriteInfoLabel = findViewById(R.id.ex_DccexWriteInfoLabel);
        DccexWriteInfoLabel.setText("");

        DccexResponsesLabel = findViewById(R.id.ex_DccexResponsesLabel);
        DccexResponsesLabel.setText("");
        DccexSendsLabel = findViewById(R.id.ex_DccexSendsLabel);
        DccexSendsLabel.setText("");

        DccexResponsesScrollView = findViewById(R.id.ex_DccexResponsesScrollView);
        DccexSendsScrollView = findViewById(R.id.ex_DccexSendsScrollView);

        clearCommandsButton = findViewById(R.id.ex_dccexClearCommandsButton);
        clear_commands_button_listener clearCommandsClickListener = new clear_commands_button_listener();
        clearCommandsButton.setOnClickListener(clearCommandsClickListener);

        mainapp.getCommonPreferences();

        vpinValueField = findViewById(R.id.ex_DccexNeopixelVpinValue);
        vpinValueField.setText("");
        vpinValueField.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                vpinValue = getIntFromString(vpinValueField.getText().toString());
                refreshDccexNeopixelView();
                showResult();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        vpinCountValueField = findViewById(R.id.ex_DccexNeopixelVpinCountValue);
        vpinCountValueField.setText("");
        vpinCountValueField.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                vpinCountValue = getIntFromString(vpinCountValueField.getText().toString());
                refreshDccexNeopixelView();
                showResult();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        sampleLabel = findViewById(R.id.ex_DccexNeopixelSampleLabel);

        ColorSliderListener seekbarListener = new ColorSliderListener(color_type.RED);
        redSeekbar = findViewById(R.id.redSeekBar);
        redSeekbar.setOnSeekBarChangeListener(seekbarListener);
        redSeekbar.setOnTouchListener(seekbarListener);

        seekbarListener = new ColorSliderListener(color_type.GREEN);
        greenSeekbar = findViewById(R.id.greenSeekBar);
        greenSeekbar.setOnSeekBarChangeListener(seekbarListener);
        greenSeekbar.setOnTouchListener(seekbarListener);

        seekbarListener = new ColorSliderListener(color_type.BLUE);
        blueSeekbar = findViewById(R.id.blueSeekBar);
        blueSeekbar.setOnSeekBarChangeListener(seekbarListener);
        blueSeekbar.setOnTouchListener(seekbarListener);


        plusMinusButtons[0] = findViewById(R.id.ex_RedMinusButton);
        ChangeButtonListener changeButtonListener = new ChangeButtonListener(color_type.RED, -1);
        plusMinusButtons[0].setOnClickListener(changeButtonListener);

        plusMinusButtons[1] = findViewById(R.id.ex_RedPlusButton);
        changeButtonListener = new ChangeButtonListener(color_type.RED, 1);
        plusMinusButtons[1].setOnClickListener(changeButtonListener);

        plusMinusButtons[2] = findViewById(R.id.ex_GreenMinusButton);
        changeButtonListener = new ChangeButtonListener(color_type.GREEN, -1);
        plusMinusButtons[2].setOnClickListener(changeButtonListener);

        plusMinusButtons[3] = findViewById(R.id.ex_GreenPlusButton);
        changeButtonListener = new ChangeButtonListener(color_type.GREEN, 1);
        plusMinusButtons[3].setOnClickListener(changeButtonListener);

        plusMinusButtons[4] = findViewById(R.id.ex_BlueMinusButton);
        changeButtonListener = new ChangeButtonListener(color_type.BLUE, -1);
        plusMinusButtons[4].setOnClickListener(changeButtonListener);

        plusMinusButtons[5] = findViewById(R.id.ex_BluePlusButton);
        changeButtonListener = new ChangeButtonListener(color_type.BLUE, 1);
        plusMinusButtons[5].setOnClickListener(changeButtonListener);

        onButton = findViewById(R.id.ex_DccexNeopixelOnButton);
        OnOffButtonListener onOffButtonListener = new OnOffButtonListener(1);
        onButton.setOnClickListener(onOffButtonListener);

        offButton = findViewById(R.id.ex_DccexNeopixelOffButton);
        onOffButtonListener = new OnOffButtonListener(0);
        offButton.setOnClickListener(onOffButtonListener);

//        resetNeopixelTextFields();
        refreshDccexNeopixelView();

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
        Log.d("EX_Toolbox", "neopixel.onResume() called");
        mainapp.applyTheme(this);

        super.onResume();

        mainapp.getCommonPreferences();

        setActivityTitle();
        mainapp.activeScreen = mainapp.ACTIVE_SCREEN_NEOPIXEL;
        mainapp.dccexScreenIsOpen = true;

        refreshDccexView();
        refreshDccexNeopixelView();

        if (mainapp.isForcingFinish()) {    //expedite
            this.finish();
            return;
        }

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.TIME_CHANGED);    // request time update
        CookieSyncManager.getInstance().startSync();

        // enable swipe/fling detection if enabled in Prefs
        ov = findViewById(R.id.neopixel_overlay);
        ov.addOnGestureListener(this);
        ov.setEventsInterceptionEnabled(true);
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }

        getSliderOverlayArea();
    }

    void getSliderOverlayArea() {
        if ( (!foundSliderOverlayArea) && (ov!=null) )  {
            // find the slider areas so that we can ignore gestures
            View sliderArea = findViewById(R.id.ex_neoPixelSliderArea);

            int[] location = new int[2];
            ov.getLocationOnScreen(location);
            int ovx = location[0];
            int ovy = location[1];

            location = new int[2];
            if (sliderArea != null) {
                sliderArea.getLocationOnScreen(location);
                int x = location[0];
                int y = location[1];

                if ( (x>0) || (y>0) ) {  // may not have finished drawing yet
                    slidersStartX = x - ovx;
                    slidersStartY = y - ovy;
                    slidersEndX = x + sliderArea.getWidth() - ovx;
                    slidersEndY = y + sliderArea.getHeight() - ovy;

                    foundSliderOverlayArea = true;
                }
            }
        }
    }

    @Override
    public void onPause() {
        Log.d("EX_Toolbox", "neopixel.onPause() called");
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
//        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.STOP_CURRENTS_TIMER);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("EX_Toolbox", "neopixel.onStart() called");
        // put pointer to this activity's handler in main app's shared variable
        if (mainapp.neopixel_msg_handler == null)
            mainapp.neopixel_msg_handler = new neopixel_handler(Looper.getMainLooper());
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
        Log.d("EX_Toolbox", "neopixel.onDestroy() called");

        if (mainapp.neopixel_msg_handler !=null) {
            mainapp.neopixel_msg_handler.removeCallbacksAndMessages(null);
            mainapp.neopixel_msg_handler = null;
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
            if (mainapp.neopixel_msg_handler!=null) {
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
        inflater.inflate(R.menu.neopixel_menu, menu);
        tMenu = menu;

        mainapp.setTrackmanagerMenuOption(menu);

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
            in = new Intent().setClass(this, neopixel.class);
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
        Log.d("EX-Toolbox", "neopixel.forceRestartApp() ");
        Message msg = Message.obtain();
        msg.what = message_type.RESTART_APP;
        msg.arg1 = forcedRestartReason;
        mainapp.comm_msg_handler.sendMessage(msg);
    }

//**************************************************************************************

    public class OnOffButtonListener implements View.OnClickListener {
        int onOff;

        public OnOffButtonListener(int thisOnOff) {
            onOff = thisOnOff;
        }

        public void onClick(View v) {
            mainapp.buttonVibration();
            mainapp.hideSoftKeyboard(v);

            if (vpinValue>0) {
                String cmd = "";
                if (vpinCountValue <= 0) {
                    cmd = String.format("%d", vpinValue);
                } else {
                    cmd = String.format("%d %d", vpinValue, vpinCountValue);
                }
                if (onOff == 0) {
                    cmd = "-" + cmd;
                }
                if (onOff == 1) { // on
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SEND_NEOPIXEL_ON_OFF, cmd);
                } else { // off
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SEND_NEOPIXEL_ON_OFF, cmd);
                }
            }
        }
    }


    public class ChangeButtonListener implements View.OnClickListener {
        int color;
        int adjust;

        public ChangeButtonListener(int thisColor, int thisAdjust) {
            color = thisColor;
            adjust = thisAdjust;
        }

        public void onClick(View v) {
            mainapp.buttonVibration();
            mainapp.hideSoftKeyboard(v);

            int pos = 0;
            switch (color) {
                case color_type.RED:
                    pos = redSeekbar.getProgress();
                    redSeekbar.setProgress(pos + adjust);
                    break;
                case color_type.GREEN:
                    pos = greenSeekbar.getProgress();
                    greenSeekbar.setProgress(pos + adjust);
                    break;
                case color_type.BLUE:
                    pos = blueSeekbar.getProgress();
                    blueSeekbar.setProgress(pos + adjust);
                    break;
            }
        }
    }

    //Listeners for the sliders
    protected class ColorSliderListener implements SeekBar.OnSeekBarChangeListener, View.OnTouchListener {
        int whichColor;
        boolean dragInProgress = false;

        protected ColorSliderListener(int new_whichColor) {
            whichColor = new_whichColor; // store values for this listener
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // consume event if gesture is in progress, otherwise pass it to the SeekBar onProgressChanged()
            mainapp.hideSoftKeyboard(v);
            return (gestureInProgress);
        }

        @Override
        public void onProgressChanged(SeekBar sbBrake, int newSliderPosition, boolean fromUser) {
            Log.d("EX_Toolbox","ColorSliderListener(): onProgressChanged(): whichColor: " + whichColor);
            getSliderPositions();
            showResult();
            if (!dragInProgress) {
                sendResult();
            } else if ((fromUser) && (!isRepeatRunning)) {
                colorSliderRptHandler.postDelayed(new ColorSliderRptUpdater(500), 500);
            }
            mainapp.buttonVibration();
        }

        @Override
        public void onStartTrackingTouch(SeekBar sbBrake) {
            gestureInProgress = false;
            dragInProgress = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar sbBrake) {
            gestureInProgress = false;
            dragInProgress = false;
            showResult();
            sendResult();
        }
    }

    // For Semi Realistic Air update repeater
    protected class ColorSliderRptUpdater implements Runnable {
        int delayMillis;

        protected ColorSliderRptUpdater(int myRepeatDelay) {
            delayMillis = myRepeatDelay;
        }

        @Override
        public void run() {
            Log.d("EX_Toolbox","ColorSliderRptUpdater: run()");
            if (mainapp.appIsFinishing) { return; }


            Log.d("EX_Toolbox","ColorSliderRptUpdater(): run(): " + String.format("lR: %d  lG: %d  lB: %d", lastRedSeekbarSliderPositionSent, lastGreenSeekbarSliderPositionSent, lastBlueSeekbarSliderPositionSent));
            Log.d("EX_Toolbox","ColorSliderRptUpdater(): run(): " + String.format("R: %d  G: %d  B: %d", red, green, blue));

            if ( (lastRedSeekbarSliderPositionSent != red)
                    || (lastGreenSeekbarSliderPositionSent != green)
                    || (lastBlueSeekbarSliderPositionSent != blue) ) {
                sendResult();
                isRepeatRunning = true;
                colorSliderRptHandler.postDelayed(new ColorSliderRptUpdater(delayMillis), delayMillis);
            } else {
                isRepeatRunning = false;
            }
        }
    }

    int getIntFromString(String str) {
        int result = 0;
        try {
            result = Integer.parseInt(str);
        } catch (Exception ignored) {}
        return result;
    }

    public class clear_commands_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            mainapp.DccexResponsesListHtml.clear();
            mainapp.dccexSendsListHtml.clear();
            mainapp.dccexResponsesStr = "";
            mainapp.dccexSendsStr = "";
            refreshDccexView();
        }
    }

//    private void resetNeopixelTextFields() {
//    }

    private void showResult() {
        Log.d("EX_Toolbox","showResult(): " + String.format("R: %d  G: %d  B: %d", red, green, blue));
        if (vpinValue>0) {
            getSliderPositions();
            if (vpinCountValue <= 0) {
                DccexInfoStr = String.format("vPin: %d  R: %d  G: %d  B: %d", vpinValue, red, green, blue);
            } else {
                DccexInfoStr = String.format("vPin: %d  R: %d  G: %d  B: %d  Count: %d", vpinValue, red, green, blue, vpinCountValue);
            }
            DccexWriteInfoLabel.setText(DccexInfoStr);
            sampleLabel.setBackgroundColor(Color.argb(255, red, green, blue));
        }
    }

    private void sendResult() {
        Log.d("EX_Toolbox","sendResult(): " + String.format("R: %d  G: %d  B: %d", red, green, blue));

        if (vpinValue>0) {
            if ( (lastRedSeekbarSliderPositionSent != red)
                || (lastGreenSeekbarSliderPositionSent != green)
                || (lastBlueSeekbarSliderPositionSent != blue) ) {

                String cmd = "";
                if (vpinCountValue <= 0) {
                    cmd = String.format("%d %d %d %d", vpinValue, red, green, blue);
                } else {
                    cmd = String.format("%d %d %d %d %d", vpinValue, red, green, blue, vpinCountValue);
                }
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SEND_NEOPIXEL, cmd);

                lastRedSeekbarSliderPositionSent = red;
                lastGreenSeekbarSliderPositionSent = green;
                lastBlueSeekbarSliderPositionSent = blue;
            }
        }
    }

    public void refreshDccexView() {
        DccexWriteInfoLabel.setText(DccexInfoStr);
        refreshDccexCommandsView();
        showResult();
    }

    public void refreshDccexCommandsView() {
        DccexResponsesLabel.setText(Html.fromHtml(mainapp.dccexResponsesStr));
        DccexSendsLabel.setText(Html.fromHtml(mainapp.dccexSendsStr));
    }

    public void refreshDccexNeopixelView() {
        boolean enabled = false;
        if (vpinValue>0) { enabled = true;}

        redSeekbar.setEnabled(enabled);
        greenSeekbar.setEnabled(enabled);
        blueSeekbar.setEnabled(enabled);

        onButton.setEnabled(enabled);
        offButton.setEnabled(enabled);

        for (int i=0; i<6; i++) {
            if (plusMinusButtons[i] != null)
                plusMinusButtons[i].setEnabled(enabled);
        }

        vpinCountValueField.setEnabled(enabled);
        sampleLabel.setVisibility( (enabled) ? View.VISIBLE : View.GONE);
    }

    void getSliderPositions() {
        red = redSeekbar.getProgress();
        green = greenSeekbar.getProgress();
        blue = blueSeekbar.getProgress();

        Log.d("EX_Toolbox","getSliderPositions(): " + String.format("R: %d  G: %d  B: %d", red, green, blue));
    }
}
