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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import dcc_ex.ex_toolbox.logviewer.ui.LogViewerActivity;
import dcc_ex.ex_toolbox.type.message_type;
import dcc_ex.ex_toolbox.util.LocaleHelper;

public class currents extends AppCompatActivity implements GestureOverlayView.OnGestureListener {

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

    //**************************************

    private LinearLayout DccexWriteInfoLayout;
    private TextView DccexWriteInfoLabel;
    private String DccexInfoStr = "";

    private TextView DccexResponsesLabel;
    private TextView DccexSendsLabel;
    private ScrollView DccexResponsesScrollView;
    private ScrollView DccexSendsScrollView;

    Button startCurrentsButton;
    Button stopCurrentsButton;
    Button clearCommandsButton;

    private LinearLayout[] dccExCurrentLayouts = {null, null, null, null, null,  null, null, null, null, null};
    private TextView[] dccExCurrentsTextView = {null, null, null, null, null,  null, null, null, null, null};
    private TextView[] dccExCurrentsHighestTextView = {null, null, null, null, null,  null, null, null, null, null};
    private TextView[] dccExCurrentsMaxTextView = {null, null, null, null, null,  null, null, null, null, null};
    private ProgressBar[] dccExCurrentsMaxProgressView = {null, null, null, null, null,  null, null, null, null, null};

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
        gestureStartX = event.getX();
        gestureStartY = event.getY();
//        Log.d("EX_Toolbox", "gestureStart x=" + gestureStartX + " y=" + gestureStartY);

        gestureInProgress = true;
        gestureLastCheckTime = event.getEventTime();
        mVelocityTracker.clear();

        // start the gesture timeout timer
        if (mainapp.currents_msg_handler != null)
            mainapp.currents_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
    }

    public void gestureMove(MotionEvent event) {
        // Log.d(""EX_Toolbox", "gestureMove action " + event.getAction());
        if ( (mainapp != null) && (mainapp.currents_msg_handler != null) && (gestureInProgress) ) {
            // stop the gesture timeout timer
            mainapp.currents_msg_handler.removeCallbacks(gestureStopped);

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
                mainapp.currents_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
            }
        }
    }

    private void gestureEnd(MotionEvent event) {
        // Log.d(""EX_Toolbox", "gestureEnd action " + event.getAction() + " inProgress? " + gestureInProgress);
        if ( (mainapp != null) && (mainapp.currents_msg_handler != null) && (gestureInProgress) ) {
            mainapp.currents_msg_handler.removeCallbacks(gestureStopped);

            float deltaX = (event.getX() - gestureStartX);
            float absDeltaX =  Math.abs(deltaX);
            if (absDeltaX > threaded_application.min_fling_distance) { // only process left/right swipes
                // valid gesture. Change the event action to CANCEL so that it isn't processed by any control below the gesture overlay
                event.setAction(MotionEvent.ACTION_CANCEL);
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.STOP_CURRENTS_TIMER);
                // process swipe in the direction with the largest change
                Intent nextScreenIntent = mainapp.getNextIntentInSwipeSequence(threaded_application.SCREEN_SWIPE_INDEX_CURRENTS, deltaX);
                startACoreActivity(this, nextScreenIntent, true, deltaX);
            } else {
                // gesture was not long enough
                gestureFailed(event);
            }
        }
    }

    private void gestureCancel(MotionEvent event) {
        if (mainapp.currents_msg_handler != null)
            mainapp.currents_msg_handler.removeCallbacks(gestureStopped);
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
    class currents_handler extends Handler {

        public currents_handler(Looper looper) {
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

                case message_type.RECEIVED_CURRENTS_MAX:
                case message_type.RECEIVED_CURRENTS:
                    setCurrentsFromResponses();
                    refreshDccexCurrentsView();
                    break;

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
                    getApplicationContext().getResources().getString(R.string.app_name_currents_short),
                    mainapp.getFastClockTime());
        else
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_currents),
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

        setContentView(R.layout.currents);

        //put pointer to this activity's handler in main app's shared variable
        mainapp.currents_msg_handler = new currents_handler(Looper.getMainLooper());

        mainapp.loadBackgroundImage(findViewById(R.id.currentsBackgroundImgView));

        DccexWriteInfoLayout = findViewById(R.id.ex_DccexWriteInfoLayout);
        DccexWriteInfoLabel = findViewById(R.id.ex_DccexWriteInfoLabel);
        DccexWriteInfoLabel.setText("");

        DccexResponsesLabel = findViewById(R.id.ex_DccexResponsesLabel);
        DccexResponsesLabel.setText("");
        DccexSendsLabel = findViewById(R.id.ex_DccexSendsLabel);
        DccexSendsLabel.setText("");

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.START_CURRENTS_TIMER);
//        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_CURRENTS);
//        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_CURRENTS_MAX);

        for (int i = 0; i< threaded_application.DCCEX_MAX_TRACKS; i++) {
            switch (i) {
                default:
                case 0:
                    dccExCurrentLayouts[i] = findViewById(R.id.ex_DccexCurrent0layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.ex_current_0_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.ex_current_highest_0_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.ex_current_max_0_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.ex_current_max_0_progress_bar);
                    break;
                case 1:
                    dccExCurrentLayouts[i] = findViewById(R.id.ex_DccexCurrent1layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.ex_current_1_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.ex_current_highest_1_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.ex_current_max_1_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.ex_current_max_1_progress_bar);
                    break;
                case 2:
                    dccExCurrentLayouts[i] = findViewById(R.id.ex_DccexCurrent2layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.ex_current_2_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.ex_current_highest_2_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.ex_current_max_2_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.ex_current_max_2_progress_bar);
                    break;
                case 3:
                    dccExCurrentLayouts[i] = findViewById(R.id.ex_DccexCurrent3layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.ex_current_3_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.ex_current_highest_3_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.ex_current_max_3_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.ex_current_max_3_progress_bar);
                    break;
                case 4:
                    dccExCurrentLayouts[i] = findViewById(R.id.ex_DccexCurrent4layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.ex_current_4_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.ex_current_highest_4_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.ex_current_max_4_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.ex_current_max_4_progress_bar);
                    break;
                case 5:
                    dccExCurrentLayouts[i] = findViewById(R.id.ex_DccexCurrent5layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.ex_current_5_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.ex_current_highest_5_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.ex_current_max_5_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.ex_current_max_5_progress_bar);
                    break;
                case 6:
                    dccExCurrentLayouts[i] = findViewById(R.id.ex_DccexCurrent6layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.ex_current_6_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.ex_current_highest_6_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.ex_current_max_6_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.ex_current_max_6_progress_bar);
                    break;
                case 7:
                    dccExCurrentLayouts[i] = findViewById(R.id.ex_DccexCurrent7layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.ex_current_7_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.ex_current_highest_7_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.ex_current_max_7_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.ex_current_max_7_progress_bar);
                    break;
            }

        }

        startCurrentsButton = findViewById(R.id.ex_DccexStartCurrentsButton);
        start_currents_button_listener startCurrentsClickListener = new start_currents_button_listener();
        startCurrentsButton.setOnClickListener(startCurrentsClickListener);

        stopCurrentsButton = findViewById(R.id.ex_DccexStopCurrentsButton);
        stop_currents_button_listener stopCurrentsClickListener = new stop_currents_button_listener();
        stopCurrentsButton.setOnClickListener(stopCurrentsClickListener);

        DccexResponsesScrollView = findViewById(R.id.ex_DccexResponsesScrollView);
        DccexSendsScrollView = findViewById(R.id.ex_DccexSendsScrollView);

        clearCommandsButton = findViewById(R.id.ex_dccexClearCommandsButton);
        ClearCommandsButtonListener clearCommandsClickListener = new ClearCommandsButtonListener();
        clearCommandsButton.setOnClickListener(clearCommandsClickListener);

        resetCurrentTextFields();
        refreshDccexCurrentsView();

//        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_CURRENTS, "");
//
        mainapp.getCommonPreferences();

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
        Log.d("EX_Toolbox", "currents.onResume() called");
        mainapp.applyTheme(this);

        super.onResume();

        mainapp.getCommonPreferences();

        setActivityTitle();
        mainapp.activeScreen = mainapp.ACTIVE_SCREEN_CURRENTS;
        mainapp.dccexScreenIsOpen = true;

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.START_CURRENTS_TIMER);
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_CURRENTS);
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_CURRENTS_MAX);

        refreshDccexView();
        refreshDccexCurrentsView();

        if (mainapp.isForcingFinish()) {    //expedite
            this.finish();
            return;
        }

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.TIME_CHANGED);    // request time update
        CookieSyncManager.getInstance().startSync();

        // enable swipe/fling detection if enabled in Prefs
        ov = findViewById(R.id.currents_overlay);
        ov.addOnGestureListener(this);
        ov.setEventsInterceptionEnabled(true);
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    @Override
    public void onPause() {
        Log.d("EX_Toolbox", "currents.onPause() called");
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.STOP_CURRENTS_TIMER);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("EX_Toolbox", "currents.onStart() called");
        // put pointer to this activity's handler in main app's shared variable
        if (mainapp.currents_msg_handler == null)
            mainapp.currents_msg_handler = new currents_handler(Looper.getMainLooper());
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
        Log.d("EX_Toolbox", "currents.onDestroy() called");

        if (mainapp.currents_msg_handler !=null) {
            mainapp.currents_msg_handler.removeCallbacksAndMessages(null);
            mainapp.currents_msg_handler = null;
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
            if (mainapp.currents_msg_handler!=null) {
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
        inflater.inflate(R.menu.currents_menu, menu);
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
        }
    }

    @SuppressLint("ApplySharedPref")
    public void forceRestartApp(int forcedRestartReason) {
        Log.d("EX-Toolbox", "currents.forceRestartApp() ");
        Message msg = Message.obtain();
        msg.what = message_type.RESTART_APP;
        msg.arg1 = forcedRestartReason;
        mainapp.comm_msg_handler.sendMessage(msg);
    }

//**************************************************************************************

    int progress(int which) {
        double x = ((double) mainapp.currentsDccex[threaded_application.LATEST_VALUE][which]) / ((double) mainapp.currentsMaxDccex[which]) * 100;
        return (int) x;
    }

    @SuppressLint("SetTextI18n")
    void setCurrentsFromResponses() {
        for (int i = 0; i< threaded_application.DCCEX_MAX_TRACKS; i++) {
            int latest = mainapp.currentsDccex[threaded_application.LATEST_VALUE][i];
            if ( (latest==0) && (mainapp.currentsDccex[threaded_application.PREVIOUS_VALUE][i]!=0) ) {
                latest = mainapp.currentsDccex[threaded_application.PREVIOUS_VALUE][i];
            }
            dccExCurrentsTextView[i].setText(Integer.toString(latest));
            dccExCurrentsHighestTextView[i].setText(Integer.toString(mainapp.currentsHighestDccex[i]));
            dccExCurrentsMaxTextView[i].setText(Integer.toString(mainapp.currentsMaxDccex[i]));
            dccExCurrentsMaxProgressView[i].setProgress(progress(i));
        }
        showHideButtons();
    }

    int getIntFromString(String str) {
        int result = 0;
        try {
            result = Integer.parseInt(str);
        } catch (Exception ignored) {}
        return result;
    }

    public class start_currents_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            resetCurrentTextFields();
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.START_CURRENTS_TIMER);
        }
    }

    public class stop_currents_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.STOP_CURRENTS_TIMER);
        }
    }

    public class ClearCommandsButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            mainapp.DccexResponsesListHtml.clear();
            mainapp.dccexSendsListHtml.clear();
            mainapp.dccexResponsesStr = "";
            mainapp.dccexSendsStr = "";
            refreshDccexView();
        }
    }

    @SuppressLint("SetTextI18n")
    private void resetCurrentTextFields() {
        for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
            mainapp.currentsDccex[threaded_application.LATEST_VALUE][i] = 0;
            mainapp.currentsDccex[threaded_application.PREVIOUS_VALUE][i] = 0;
            mainapp.currentsHighestDccex[i] = 0;
            mainapp.currentsMaxDccex[i] = 0;

            dccExCurrentsTextView[i].setText(Integer.toString(mainapp.currentsDccex[threaded_application.LATEST_VALUE][i]));
            dccExCurrentsHighestTextView[i].setText(Integer.toString(mainapp.currentsHighestDccex[i]));
            dccExCurrentsMaxTextView[i].setText(Integer.toString(mainapp.currentsMaxDccex[i]));
        }
    }


    private void showHideButtons() {
        for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
            if ( (mainapp.currentsDccex[threaded_application.LATEST_VALUE][i] != 0) || (mainapp.currentsMaxDccex[i] != 0) ) {
                dccExCurrentLayouts[i].setVisibility(View.VISIBLE);
            } else {
                dccExCurrentLayouts[i].setVisibility(View.GONE);
            }
        }
    }

    public void refreshDccexView() {
        DccexWriteInfoLabel.setText(DccexInfoStr);
        refreshDccexCommandsView();
        showHideButtons();

    }

    public void refreshDccexCommandsView() {
        DccexResponsesLabel.setText(Html.fromHtml(mainapp.dccexResponsesStr));
        DccexSendsLabel.setText(Html.fromHtml(mainapp.dccexSendsStr));
    }

    public void refreshDccexCurrentsView() {
        showHideButtons();
    }

}
