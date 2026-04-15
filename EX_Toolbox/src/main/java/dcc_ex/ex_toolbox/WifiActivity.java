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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import dcc_ex.ex_toolbox.logviewer.ui.LogViewerActivity;
import dcc_ex.ex_toolbox.type.message_type;
import dcc_ex.ex_toolbox.util.LocaleHelper;

public class WifiActivity extends AppCompatActivity implements GestureOverlayView.OnGestureListener {

    private threaded_application mainapp;  // hold pointer to mainapp
    private SharedPreferences prefs;

    private Menu tMenu;
    private static boolean savedMenuSelected;

    EditText dccexWifiSsidValueEditText;
    EditText dccexWifiPasswordValueEditText;
    EditText dccexWifiHostnameValueEditText;

    String dccexWifiSsidValue = "";
    String dccexWifiPasswordValue = "";
    String dccexWifiHostnameValue = "";

    boolean validSsid = false;
    boolean validPassword = false;
    boolean validHostname = false;

    TextView dccexWifiCurrentSettingsTextView;

    Button setStationButton;
    Button setHostnameButton;
//    Button setHostnameAndStationButton;
    Button resetButton;

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

    private LinearLayout dccexWriteInfoLayout;
    private TextView dccexWriteInfoLabel;
    private String dccexInfoStr = "";

    private TextView dccexResponsesLabel;
    private TextView dccexSendsLabel;
    private ScrollView dccexResponsesScrollView;
    private ScrollView dccexSendsScrollView;

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
        gestureStartX = event.getX();
        gestureStartY = event.getY();
//        Log.d("EX_Toolbox", "gestureStart x=" + gestureStartX + " y=" + gestureStartY);

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
    class wifi_handler extends Handler {

        public wifi_handler(Looper looper) {
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
                            mainapp.setPowerStateActionViewButton(tMenu, findViewById(R.id.powerLayoutButton));
                        }
                    }
                    break;
                }

                case message_type.DCCEX_RECEIVED_WIFI_DETAILS:
                    showResult();
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
                    getApplicationContext().getResources().getString(R.string.app_name_wifi_short),
                    mainapp.getFastClockTime());
        else
            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_wifi),
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

        setContentView(R.layout.wifi);

        //put pointer to this activity's handler in main app's shared variable
        mainapp.wifi_msg_handler = new wifi_handler(Looper.getMainLooper());

        mainapp.loadBackgroundImage(findViewById(R.id.wifiBackgroundImgView));

        dccexWriteInfoLayout = findViewById(R.id.ex_DccexWriteInfoLayout);
        dccexWriteInfoLabel = findViewById(R.id.ex_DccexWriteInfoLabel);
        dccexWriteInfoLabel.setText("");

        dccexResponsesLabel = findViewById(R.id.ex_DccexResponsesLabel);
        dccexResponsesLabel.setText("");
        dccexSendsLabel = findViewById(R.id.ex_DccexSendsLabel);
        dccexSendsLabel.setText("");

        dccexResponsesScrollView = findViewById(R.id.ex_DccexResponsesScrollView);
        dccexSendsScrollView = findViewById(R.id.ex_DccexSendsScrollView);

        clearCommandsButton = findViewById(R.id.ex_dccexClearCommandsButton);
        ClearCommandsButtonListener clearCommandsClickListener = new ClearCommandsButtonListener();
        clearCommandsButton.setOnClickListener(clearCommandsClickListener);

        mainapp.getCommonPreferences();

        dccexWifiSsidValueEditText = findViewById(R.id.dccexWifiSsidValue);
        dccexWifiSsidValueEditText.setText("");
        dccexWifiSsidValueEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                    dccexWifiSsidValue = dccexWifiSsidValueEditText.getText().toString();

                validSsid = true;
                if (dccexWifiSsidValue.contains(" ")) validSsid = false;
                if (dccexWifiSsidValue.isEmpty()) validSsid = false;

                refreshDccexWifiView();
                showResult();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        dccexWifiPasswordValueEditText = findViewById(R.id.dccexWifiPasswordValue);
        dccexWifiPasswordValueEditText.setText("");
        dccexWifiPasswordValueEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                dccexWifiPasswordValue = dccexWifiPasswordValueEditText.getText().toString();

                validPassword = true;
                if (dccexWifiPasswordValue.isEmpty()) validPassword = false;
                if (dccexWifiPasswordValue.length()<8) validPassword = false;

                refreshDccexWifiView();
                showResult();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        dccexWifiHostnameValueEditText = findViewById(R.id.dccexWifiHostnameValue);
        dccexWifiHostnameValueEditText.setText("");
        dccexWifiHostnameValueEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                dccexWifiHostnameValue = dccexWifiHostnameValueEditText.getText().toString();

                validHostname = true;
                if (dccexWifiHostnameValue.contains(" ")) validHostname = false;
                if (dccexWifiHostnameValue.isEmpty()) validHostname = false;

                refreshDccexWifiView();
                showResult();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        dccexWifiCurrentSettingsTextView = findViewById(R.id.dccexWifiCurrentSettings);

        setStationButton = findViewById(R.id.dccexWifiSetStationButton);
        SetStationButtonListener setStationButtonListener = new SetStationButtonListener();
        setStationButton.setOnClickListener(setStationButtonListener);

        setHostnameButton = findViewById(R.id.dccexWifiSetHostnameButton);
        SetHostnameButtonListener setHostnameButtonListener = new SetHostnameButtonListener();
        setHostnameButton.setOnClickListener(setHostnameButtonListener);

//        setHostnameAndStationButton = findViewById(R.id.dccexWifiSetHostnameAndStationButton);
//        SetHostnameAndStationButtonListener setHostnameAndStationButtonListener = new SetHostnameAndStationButtonListener();
//        setHostnameAndStationButton.setOnClickListener(setHostnameAndStationButtonListener);

        resetButton = findViewById(R.id.dccexWifiResetButton);
        ResetButtonListener resetButtonListener = new ResetButtonListener();
        resetButton.setOnClickListener(resetButtonListener);

        refreshDccexWifiView();

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                mainapp.checkExit(WifiActivity.this);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

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
        Log.d("EX_Toolbox", "neopixel.onResume() called");
        mainapp.applyTheme(this);

        super.onResume();

        mainapp.getCommonPreferences();

        setActivityTitle();
        mainapp.activeScreen = mainapp.ACTIVE_SCREEN_WIFI;
        mainapp.dccexScreenIsOpen = true;

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_WIFI_DETAILS,"");

        validSsid = false;
        validPassword = false;
        validHostname = false;

        refreshDccexView();
        refreshDccexWifiView();

        if (mainapp.isForcingFinish()) {    //expedite
            this.finish();
            return;
        }

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.TIME_CHANGED);    // request time update
        CookieSyncManager.getInstance().startSync();

        // enable swipe/fling detection if enabled in Prefs
        ov = findViewById(R.id.wifiOverlay);
        ov.addOnGestureListener(this);
        ov.setEventsInterceptionEnabled(true);
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
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
        if (mainapp.wifi_msg_handler == null)
            mainapp.wifi_msg_handler = new wifi_handler(Looper.getMainLooper());
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

        if (mainapp.wifi_msg_handler !=null) {
            mainapp.wifi_msg_handler.removeCallbacksAndMessages(null);
            mainapp.wifi_msg_handler = null;
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
            if (mainapp.wifi_msg_handler!=null) {
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
        inflater.inflate(R.menu.wifi_menu, menu);
        tMenu = menu;

        mainapp.setTrackmanagerMenuOption(menu);

        mainapp.displayToolbarMenuButtons(menu);
        mainapp.displayPowerStateMenuButton(menu);
        mainapp.setPowerMenuOption(menu);
        mainapp.setPowerStateActionViewButton(menu, findViewById(R.id.powerLayoutButton));
        mainapp.setPowerMenuOption(menu);
        mainapp.reformatMenu(menu);

        adjustToolbarSize(menu);

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
            in = new Intent().setClass(this, WifiActivity.class);
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
        } else if (item.getItemId() == R.id.powerLayoutButton) {
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

    public class SetStationButtonListener implements View.OnClickListener {

        public SetStationButtonListener() {
        }

        public void onClick(View v) {
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SEND_WIFI_STATION, dccexWifiSsidValue + " " + dccexWifiPasswordValue);

            mainapp.buttonVibration();
            mainapp.hideSoftKeyboard(v);

        }
    }

    public class SetHostnameButtonListener implements View.OnClickListener {

        public SetHostnameButtonListener() {
        }

        public void onClick(View v) {
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SEND_WIFI_HOSTNAME, dccexWifiHostnameValue);

            mainapp.buttonVibration();
            mainapp.hideSoftKeyboard(v);

        }
    }

//    public class SetHostnameAndStationButtonListener implements View.OnClickListener {
//
//        public SetHostnameAndStationButtonListener() {
//        }
//
//        public void onClick(View v) {
//            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.SEND_WIFI_HOSTNAME, dccexWifiHostnameValue);
//            mainapp.sendMsgDelay(mainapp.comm_msg_handler, 10000, message_type.SEND_WIFI_STATION, dccexWifiSsidValue + " " + dccexWifiPasswordValue);
//
//            mainapp.buttonVibration();
//            mainapp.hideSoftKeyboard(v);
//
//        }
//    }

    public class ResetButtonListener implements View.OnClickListener {

        public ResetButtonListener() {
        }

        public void onClick(View v) {
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_RESET_WIFI,"");

            mainapp.buttonVibration();
            mainapp.hideSoftKeyboard(v);

        }
    }

    public class ClearCommandsButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.exitDoubleBackButtonInitiated = 0;
            mainapp.dccexResponsesListHtml.clear();
            mainapp.dccexSendsListHtml.clear();
            mainapp.dccexResponsesStr = "";
            mainapp.dccexSendsStr = "";
            refreshDccexView();
        }
    }

    private void showResult() {
        String currentWifi = getApplicationContext().getResources().getString(R.string.dccexWifiCurrentSettings) + "\n"
                + getApplicationContext().getResources().getString(R.string.dccexWifiAccessPoint) + "\n"
                + " - " + getApplicationContext().getResources().getString(R.string.dccexWifiSsidValueLabel) + ":" + mainapp.wifiAccessPointSsid + " "
                + getApplicationContext().getResources().getString(R.string.dccexWifiPasswordValueLabel) + ":" + mainapp.wifiAccessPointPassword + "\n"
                + getApplicationContext().getResources().getString(R.string.dccexWifiStation) + "\n"
                + " - " + getApplicationContext().getResources().getString(R.string.dccexWifiSsidValueLabel) + ":" + mainapp.wifiStationSsid + " "
                + getApplicationContext().getResources().getString(R.string.dccexWifiPasswordValueLabel) + ":" + mainapp.wifiStationPassword + "\n"
                + getApplicationContext().getResources().getString(R.string.dccexWifiHostnameValueLabel) + ":" + mainapp.wifiHostname
                ;

        dccexWifiCurrentSettingsTextView.setText(currentWifi);
    }

    public void refreshDccexView() {
        dccexWriteInfoLabel.setText(dccexInfoStr);
        refreshDccexCommandsView();
        showResult();
    }

    public void refreshDccexCommandsView() {
        dccexResponsesLabel.setText(Html.fromHtml(mainapp.dccexResponsesStr));
        dccexSendsLabel.setText(Html.fromHtml(mainapp.dccexSendsStr));
    }

    public void refreshDccexWifiView() {
        boolean enabled = true;
//        if (vpinValue>0) { enabled = true;}

        setStationButton.setEnabled(validSsid && validPassword);
        setHostnameButton.setEnabled(validHostname);
//        setHostnameAndStationButton.setEnabled(validHostname && validSsid && validPassword);
        resetButton.setEnabled(enabled);

    }

    void adjustToolbarSize(Menu menu) {
        int newHeightAndWidth = mainapp.adjustToolbarSize(toolbar);

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            View itemChooser = item.getActionView();

            if (itemChooser != null) {
                itemChooser.getLayoutParams().height = newHeightAndWidth;
                itemChooser.getLayoutParams().width = (int) ( (float) newHeightAndWidth * 1.3 );

                itemChooser.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onOptionsItemSelected(item);
                    }
                });
            }
        }
    }
}
