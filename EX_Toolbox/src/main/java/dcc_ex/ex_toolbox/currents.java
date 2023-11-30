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

    private LinearLayout DCCEXwriteInfoLayout;
    private TextView DCCEXwriteInfoLabel;
    private String DCCEXinfoStr = "";

    private TextView DCCEXresponsesLabel;
    private TextView DCCEXsendsLabel;
    private ScrollView DCCEXresponsesScrollView;
    private ScrollView DCCEXsendsScrollView;

    private boolean DCCEXhideSends = false;

    Button startCurrentsButton;
    Button stopCurrentsButton;
    Button clearCommandsButton;

    private LinearLayout[] dccExCurrentLayouts = {null, null, null, null, null,  null, null, null, null, null};
    private TextView[] dccExCurrentsTextView = {null, null, null, null, null,  null, null, null, null, null};
    private TextView[] dccExCurrentsHighestTextView = {null, null, null, null, null,  null, null, null, null, null};
    private TextView[] dccExCurrentsMaxTextView = {null, null, null, null, null,  null, null, null, null, null};
    private ProgressBar[] dccExCurrentsMaxProgressView = {null, null, null, null, null,  null, null, null, null, null};

    //**************************************

    private Toolbar toolbar;

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
        // Log.d("Engine_Driver", "gestureMove action " + event.getAction());
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
                // Log.d("Engine_Driver", "gestureVelocity vel " + velocityX);
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
        // Log.d("Engine_Driver", "gestureEnd action " + event.getAction() + " inProgress? " + gestureInProgress);
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
                    setCurrentsFromResponses();
                    refreshDCCEXcurrentsView();
                    break;

                case message_type.RECEIVED_CURRENTS:
                    setCurrentsFromResponses();
                    refreshDCCEXcurrentsView();
                    break;

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
                    getApplicationContext().getResources().getString(R.string.app_name_currents_short),
                    mainapp.getFastClockTime());
        else
            mainapp.setToolbarTitle(toolbar,
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
        Log.d("Engine_Driver", "web_activity.onCreate()");

        mainapp = (threaded_application) this.getApplication();
        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        mainapp.applyTheme(this);

        super.onCreate(savedInstanceState);

        if (mainapp.isForcingFinish()) {        // expedite
            return;
        }

        setContentView(R.layout.currents);

        //put pointer to this activity's handler in main app's shared variable
        mainapp.currents_msg_handler = new currents_handler();

        mainapp.loadBackgroundImage(findViewById(R.id.currentsBackgroundImgView));

        DCCEXwriteInfoLayout = findViewById(R.id.dexc_DCCEXwriteInfoLayout);
        DCCEXwriteInfoLabel = findViewById(R.id.dexc_DCCEXwriteInfoLabel);
        DCCEXwriteInfoLabel.setText("");

        DCCEXresponsesLabel = findViewById(R.id.dexc_DCCEXresponsesLabel);
        DCCEXresponsesLabel.setText("");
        DCCEXsendsLabel = findViewById(R.id.dexc_DCCEXsendsLabel);
        DCCEXsendsLabel.setText("");

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.START_CURRENTS_TIMER);
//        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_CURRENTS);
//        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_CURRENTS_MAX);

        for (int i=0; i<mainapp.DCCEX_MAX_TRACKS; i++) {
            switch (i) {
                default:
                case 0:
                    dccExCurrentLayouts[i] = findViewById(R.id.dexc_DCCEXcurrent0layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.dexc_current_0_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.dexc_current_highest_0_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.dexc_current_max_0_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.dexc_current_max_0_progress_bar);
                    break;
                case 1:
                    dccExCurrentLayouts[i] = findViewById(R.id.dexc_DCCEXcurrent1layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.dexc_current_1_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.dexc_current_highest_1_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.dexc_current_max_1_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.dexc_current_max_1_progress_bar);
                    break;
                case 2:
                    dccExCurrentLayouts[i] = findViewById(R.id.dexc_DCCEXcurrent2layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.dexc_current_2_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.dexc_current_highest_2_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.dexc_current_max_2_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.dexc_current_max_2_progress_bar);
                    break;
                case 3:
                    dccExCurrentLayouts[i] = findViewById(R.id.dexc_DCCEXcurrent3layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.dexc_current_3_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.dexc_current_highest_3_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.dexc_current_max_3_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.dexc_current_max_3_progress_bar);
                    break;
                case 4:
                    dccExCurrentLayouts[i] = findViewById(R.id.dexc_DCCEXcurrent4layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.dexc_current_4_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.dexc_current_highest_4_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.dexc_current_max_4_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.dexc_current_max_4_progress_bar);
                    break;
                case 5:
                    dccExCurrentLayouts[i] = findViewById(R.id.dexc_DCCEXcurrent5layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.dexc_current_5_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.dexc_current_highest_5_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.dexc_current_max_5_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.dexc_current_max_5_progress_bar);
                    break;
                case 6:
                    dccExCurrentLayouts[i] = findViewById(R.id.dexc_DCCEXcurrent6layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.dexc_current_6_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.dexc_current_highest_6_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.dexc_current_max_6_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.dexc_current_max_6_progress_bar);
                    break;
                case 7:
                    dccExCurrentLayouts[i] = findViewById(R.id.dexc_DCCEXcurrent7layout);
                    dccExCurrentsTextView[i] = findViewById(R.id.dexc_current_7_value);
                    dccExCurrentsHighestTextView[i] = findViewById(R.id.dexc_current_highest_7_value);
                    dccExCurrentsMaxTextView[i] = findViewById(R.id.dexc_current_max_7_value);
                    dccExCurrentsMaxProgressView[i] = findViewById(R.id.dexc_current_max_7_progress_bar);
                    break;
            }


            startCurrentsButton = findViewById(R.id.dexc_DCCEXstartCurrentsButton);
            start_currents_button_listener startCurrentsClickListener = new start_currents_button_listener();
            startCurrentsButton.setOnClickListener(startCurrentsClickListener);

            stopCurrentsButton = findViewById(R.id.dexc_DCCEXstopCurrentsButton);
            stop_currents_button_listener stopCurrentsClickListener = new stop_currents_button_listener();
            stopCurrentsButton.setOnClickListener(stopCurrentsClickListener);

            DCCEXresponsesScrollView = findViewById(R.id.dexc_DCCEXresponsesScrollView);
            DCCEXsendsScrollView = findViewById(R.id.dexc_DCCEXsendsScrollView);

            clearCommandsButton = findViewById(R.id.dexc_DCCEXclearCommandsButton);
            clear_commands_button_listener clearCommandsClickListener = new clear_commands_button_listener();
            clearCommandsButton.setOnClickListener(clearCommandsClickListener);
        }

        resetCurrentTextFields();
        refreshDCCEXcurrentsView();

//        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_CURRENTS, "");
//
        mainapp.getCommonPreferences();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
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

        mainapp.DCCEXscreenIsOpen = true;


        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.START_CURRENTS_TIMER);
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_CURRENTS);
        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_CURRENTS_MAX);

        refreshDCCEXview();
        refreshDCCEXcurrentsView();

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
            mainapp.currents_msg_handler = new currents_handler();
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
            Log.d("Engine_Driver", "onDestroy: mainapp.web_msg_handler is null. Unable to removeCallbacksAndMessages");
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
        return (super.onKeyDown(key, event));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.currents_menu, menu);
        tMenu = menu;

        mainapp.setTrackmanagerMenuOption(menu);

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
            case R.id.servos_mnu:
                navigateAway(true, null);
                in = new Intent().setClass(this, servos.class);
                startACoreActivity(this, in, false, 0);
                return true;
            case R.id.locos_mnu:
                navigateAway(true, null);
                in = new Intent().setClass(this, locos.class);
                startACoreActivity(this, in, false, 0);
                return true;
            case R.id.sensors_mnu:
                navigateAway(true, null);
                in = new Intent().setClass(this, sensors.class);
                startACoreActivity(this, in, false, 0);
                return true;
            case R.id.track_manager_mnu:
                navigateAway(true, null);
                in = new Intent().setClass(this, track_manager.class);
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
        double x = ((double) mainapp.currentsDCCEX[mainapp.LATEST_VALUE][which]) / ((double) mainapp.currentsMaxDCCEX[which]) * 100;
        return (int) x;
    }

    void setCurrentsFromResponses() {
        for (int i=0; i<mainapp.DCCEX_MAX_TRACKS; i++) {
            int latest = mainapp.currentsDCCEX[mainapp.LATEST_VALUE][i];
            if ( (latest==0) && (mainapp.currentsDCCEX[mainapp.PREVIOUS_VALUE][i]!=0) ) {
                latest = mainapp.currentsDCCEX[mainapp.PREVIOUS_VALUE][i];
            }
            dccExCurrentsTextView[i].setText(Integer.toString(latest));
            dccExCurrentsHighestTextView[i].setText(Integer.toString(mainapp.currentsHighestDCCEX[i]));
            dccExCurrentsMaxTextView[i].setText(Integer.toString(mainapp.currentsMaxDCCEX[i]));
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
            resetCurrentTextFields();
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.START_CURRENTS_TIMER);
        }
    }

    public class stop_currents_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.STOP_CURRENTS_TIMER);
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

    private void resetCurrentTextFields() {
        for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
            mainapp.currentsDCCEX[mainapp.LATEST_VALUE][i] = 0;
            mainapp.currentsDCCEX[mainapp.PREVIOUS_VALUE][i] = 0;
            mainapp.currentsHighestDCCEX[i] = 0;
            mainapp.currentsMaxDCCEX[i] = 0;

            dccExCurrentsTextView[i].setText(Integer.toString(mainapp.currentsDCCEX[mainapp.LATEST_VALUE][i]));
            dccExCurrentsHighestTextView[i].setText(Integer.toString(mainapp.currentsHighestDCCEX[i]));
            dccExCurrentsMaxTextView[i].setText(Integer.toString(mainapp.currentsMaxDCCEX[i]));
        }
    }


    private void showHideButtons() {
        for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
            if ( (mainapp.currentsDCCEX[mainapp.LATEST_VALUE][i] != 0) || (mainapp.currentsMaxDCCEX[i] != 0) ) {
                dccExCurrentLayouts[i].setVisibility(View.VISIBLE);
            } else {
                dccExCurrentLayouts[i].setVisibility(View.GONE);
            }
        }
    }

    public void refreshDCCEXview() {
        DCCEXwriteInfoLabel.setText(DCCEXinfoStr);
        refreshDCCEXcommandsView();
        showHideButtons();

    }

    public void refreshDCCEXcommandsView() {
        DCCEXresponsesLabel.setText(Html.fromHtml(mainapp.DCCEXresponsesStr));
        DCCEXsendsLabel.setText(Html.fromHtml(mainapp.DCCEXsendsStr));
    }

    public void refreshDCCEXcurrentsView() {
        showHideButtons();
    }

}
