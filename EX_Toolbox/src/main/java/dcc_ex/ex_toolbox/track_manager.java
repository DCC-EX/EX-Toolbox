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

public class track_manager extends AppCompatActivity implements GestureOverlayView.OnGestureListener {

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
    private String DCCEXsendCommandValue = "";
    private EditText etDCCEXsendCommandValue;

    private LinearLayout DCCEXwriteInfoLayout;
    private TextView DCCEXwriteInfoLabel;
    private String DCCEXinfoStr = "";

    private TextView DCCEXresponsesLabel;
    private TextView DCCEXsendsLabel;
    private ScrollView DCCEXresponsesScrollView;
    private ScrollView DCCEXsendsScrollView;

    private int dccCmdIndex = 0;
    String[] dccExCommonCommandsEntryValuesArray;
    String[] dccExCommonCommandsEntriesArray; // display version
    int[] dccExCommonCommandsHasParametersArray; // display version

    private boolean DCCEXhideSends = false;

    Button sendCommandButton;
    Button previousCommandButton;
    Button nextCommandButton;
    Button writeTracksButton;
    //    Button hideSendsButton;
    Button clearCommandsButton;

    private LinearLayout[] dexcDCCEXtracklayout = {null, null, null, null, null, null, null, null};
    private LinearLayout dexcDCCEXtrackLinearLayout;
    Spinner dccExCommonCommandsSpinner;

    private int[] dccExTrackTypeIndex = {1, 2, 1, 1, 1, 1, 1, 1};
    private Spinner[] dccExTrackTypeSpinner = {null, null, null, null, null, null, null, null};
    private EditText[] dccExTrackTypeIdEditText = {null, null, null, null, null, null, null, null};
    private LinearLayout[] dccExTrackTypeLayout = {null, null, null, null, null, null, null, null};

    String[] dccExTrackTypeEntryValuesArray;
    String[] dccExTrackTypeEntriesArray; // display version

    static final int WHICH_ADDRESS = 0;
    static final int WHICH_CV = 1;
    static final int WHICH_CV_VALUE = 2;
    static final int WHICH_COMMAND = 3;

    static final int TRACK_TYPE_OFF_INDEX = 0;
    static final int TRACK_TYPE_DCC_MAIN_INDEX = 1;
    static final int TRACK_TYPE_DCC_PROG_INDEX = 2;
    static final int TRACK_TYPE_DC_INDEX = 3;
    static final int TRACK_TYPE_DCX_INDEX = 4;

    static final String[] TRACK_TYPES = {"OFF", "MAIN", "PROG", "DC", "DCX"};
    static final boolean[] TRACK_TYPES_NEED_ID = {false, false, false, true, true};

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
        if (mainapp.track_manager_msg_handler != null)
            mainapp.track_manager_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
    }

    public void gestureMove(MotionEvent event) {
        // Log.d("Engine_Driver", "gestureMove action " + event.getAction());
        if ( (mainapp != null) && (mainapp.track_manager_msg_handler != null) && (gestureInProgress) ) {
            // stop the gesture timeout timer
            mainapp.track_manager_msg_handler.removeCallbacks(gestureStopped);

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
                mainapp.track_manager_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
            }
        }
    }

    private void gestureEnd(MotionEvent event) {
        // Log.d("Engine_Driver", "gestureEnd action " + event.getAction() + " inProgress? " + gestureInProgress);
        if ( (mainapp != null) && (mainapp.track_manager_msg_handler != null) && (gestureInProgress) ) {
            mainapp.track_manager_msg_handler.removeCallbacks(gestureStopped);

            float deltaX = (event.getX() - gestureStartX);
            float absDeltaX =  Math.abs(deltaX);
            if (absDeltaX > threaded_application.min_fling_distance) { // only process left/right swipes
                // valid gesture. Change the event action to CANCEL so that it isn't processed by any control below the gesture overlay
                event.setAction(MotionEvent.ACTION_CANCEL);
                // process swipe in the direction with the largest change
                Intent nextScreenIntent = mainapp.getNextIntentInSwipeSequence(threaded_application.SCREEN_SWIPE_INDEX_TRACK_MANGER, deltaX);
                startACoreActivity(this, nextScreenIntent, true, deltaX);
            } else {
                // gesture was not long enough
                gestureFailed(event);
            }
        }
    }

    private void gestureCancel(MotionEvent event) {
        if (mainapp.track_manager_msg_handler != null)
            mainapp.track_manager_msg_handler.removeCallbacks(gestureStopped);
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
    class track_manager_handler extends Handler {

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
                    refreshDCCEXtracksView();
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
                    getApplicationContext().getResources().getString(R.string.app_name_track_manager_short),
                    mainapp.getFastClockTime());
        else
            mainapp.setToolbarTitle(toolbar,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_track_manager),
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

        setContentView(R.layout.track_manager);

        //put pointer to this activity's handler in main app's shared variable
        mainapp.track_manager_msg_handler = new track_manager_handler();

//        // enable remote debugging of all webviews
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
//                WebView.setWebContentsDebuggingEnabled(true);
//            }
//        }
//
//        // open all links inside the current view (don't start external web browser)
//        WebViewClient EDWebClient = new WebViewClient() {
//            private int loadRetryCnt = 0;
//            private String currentUrl = null;
//
//            @Override
//            public void onPageFinished(WebView view, String url) {
//                super.onPageFinished(view, url);
//            }
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                return handleLoadingErrorRetries();
//            }
//
//            // above form of shouldOverrideUrlloading is deprecated so support the new form if available
//            @TargetApi(Build.VERSION_CODES.N)
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
//                return handleLoadingErrorRetries();
//            }
//
//            // stop page from continually reloading when loading errors occur
//            // (this can happen if the initial web page pref is set to a non-existant url)
//            private boolean handleLoadingErrorRetries() {
//                if (++loadRetryCnt >= 3) {   // if same page is reloading (due to errors)
//                    loadRetryCnt = 0;        // reset count for next url load
//                    return true;                // don't load the page
//                }
//                return false;                   // load in webView
//            }
//        };

        //Set the buttons
//        closeButton = findViewById(R.id.webview_button_close);
//        web_activity.close_button_listener close_click_listener = new web_activity.close_button_listener();
//        closeButton.setOnClickListener(close_click_listener);

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

        dccExCommonCommandsEntryValuesArray = this.getResources().getStringArray(R.array.dccExCommonCommandsEntryValues);
//        final List<String> dccCommonCommandsValuesList = new ArrayList<>(Arrays.asList(dccExCommonCommandsEntryValuesArray));
        dccExCommonCommandsEntriesArray = this.getResources().getStringArray(R.array.dccExCommonCommandsEntries); // display version
//        final List<String> dccCommonCommandsEntriesList = new ArrayList<>(Arrays.asList(dccExCommonCommandsEntriesArray));
        dccExCommonCommandsHasParametersArray = this.getResources().getIntArray(R.array.dccExCommonCommandsHasParameters);

        dccCmdIndex=0;
        dccExCommonCommandsSpinner = findViewById(R.id.dexc_common_commands_list);
        ArrayAdapter<?> spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExCommonCommandsEntries, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dccExCommonCommandsSpinner.setAdapter(spinner_adapter);
        dccExCommonCommandsSpinner.setOnItemSelectedListener(new command_spinner_listener());
        dccExCommonCommandsSpinner.setSelection(dccCmdIndex);

        dccExTrackTypeEntryValuesArray = this.getResources().getStringArray(R.array.dccExTrackTypeEntryValues);
//        final List<String> dccTrackTypeValuesList = new ArrayList<>(Arrays.asList(dccExTrackTypeEntryValuesArray));
        dccExTrackTypeEntriesArray = this.getResources().getStringArray(R.array.dccExTrackTypeEntries); // display version
//        final List<String> dccTrackTypeEntriesList = new ArrayList<>(Arrays.asList(dccExTrackTypeEntriesArray));

        for (int i=0; i<mainapp.DCCEX_MAX_TRACKS; i++) {
            switch (i) {
                default:
                case 0:
                    dccExTrackTypeLayout[0] = findViewById(R.id.dexc_DCCEXtrack0layout);
                    dccExTrackTypeSpinner[0] = findViewById(R.id.dexc_track_type_0_list);
                    dccExTrackTypeIdEditText[0] = findViewById(R.id.dexc_track_0_value);
                    break;
                case 1:
                    dccExTrackTypeLayout[1] = findViewById(R.id.dexc_DCCEXtrack1layout);
                    dccExTrackTypeSpinner[1] = findViewById(R.id.dexc_track_type_1_list);
                    dccExTrackTypeIdEditText[1] = findViewById(R.id.dexc_track_1_value);
                    break;
                case 2:
                    dccExTrackTypeLayout[2] = findViewById(R.id.dexc_DCCEXtrack2layout);
                    dccExTrackTypeSpinner[2] = findViewById(R.id.dexc_track_type_2_list);
                    dccExTrackTypeIdEditText[2] = findViewById(R.id.dexc_track_2_value);
                    break;
                case 3:
                    dccExTrackTypeLayout[3] = findViewById(R.id.dexc_DCCEXtrack3layout);
                    dccExTrackTypeSpinner[3] = findViewById(R.id.dexc_track_type_3_list);
                    dccExTrackTypeIdEditText[3] = findViewById(R.id.dexc_track_3_value);
                    break;
                case 4:
                    dccExTrackTypeLayout[4] = findViewById(R.id.dexc_DCCEXtrack4layout);
                    dccExTrackTypeSpinner[4] = findViewById(R.id.dexc_track_type_4_list);
                    dccExTrackTypeIdEditText[4] = findViewById(R.id.dexc_track_4_value);
                    break;
                case 5:
                    dccExTrackTypeLayout[5] = findViewById(R.id.dexc_DCCEXtrack5layout);
                    dccExTrackTypeSpinner[5] = findViewById(R.id.dexc_track_type_5_list);
                    dccExTrackTypeIdEditText[5] = findViewById(R.id.dexc_track_5_value);
                    break;
                case 6:
                    dccExTrackTypeLayout[6] = findViewById(R.id.dexc_DCCEXtrack6layout);
                    dccExTrackTypeSpinner[6] = findViewById(R.id.dexc_track_type_6_list);
                    dccExTrackTypeIdEditText[6] = findViewById(R.id.dexc_track_6_value);
                    break;
                case 7:
                    dccExTrackTypeLayout[7] = findViewById(R.id.dexc_DCCEXtrack7layout);
                    dccExTrackTypeSpinner[7] = findViewById(R.id.dexc_track_type_7_list);
                    dccExTrackTypeIdEditText[7] = findViewById(R.id.dexc_track_7_value);
                    break;
            }
            ArrayAdapter<?> track_type_spinner_adapter = ArrayAdapter.createFromResource(this, R.array.dccExTrackTypeEntries, android.R.layout.simple_spinner_item);
            track_type_spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dccExTrackTypeSpinner[i].setAdapter(track_type_spinner_adapter);
            dccExTrackTypeSpinner[i].setOnItemSelectedListener(new track_type_spinner_listener(dccExTrackTypeSpinner[i], i));
            dccExTrackTypeSpinner[i].setSelection(dccExTrackTypeIndex[i]);

            writeTracksButton = findViewById(R.id.dexc_DCCEXwriteTracksButton);
            write_tracks_button_listener writeTracksClickListener = new write_tracks_button_listener();
            writeTracksButton.setOnClickListener(writeTracksClickListener);

            DCCEXresponsesScrollView = findViewById(R.id.dexc_DCCEXresponsesScrollView);
            DCCEXsendsScrollView = findViewById(R.id.dexc_DCCEXsendsScrollView);

            clearCommandsButton = findViewById(R.id.dexc_DCCEXclearCommandsButton);
            clear_commands_button_listener clearCommandsClickListener = new clear_commands_button_listener();
            clearCommandsButton.setOnClickListener(clearCommandsClickListener);
        }

        refreshDCCEXtracksView();

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");

        mainapp.getCommonPreferences();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

    } // end onCreate

    @Override
    public void onResume() {
        Log.d("EX_Toolbox", "track_manager.onResume() called");
        mainapp.applyTheme(this);

        super.onResume();

        mainapp.getCommonPreferences();

        setActivityTitle();
        mainapp.DCCEXscreenIsOpen = true;
        refreshDCCEXview();
        refreshDCCEXtracksView();

        if (mainapp.isForcingFinish()) {    //expedite
            this.finish();
            return;
        }

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.TIME_CHANGED);    // request time update
        CookieSyncManager.getInstance().startSync();

        // enable swipe/fling detection if enabled in Prefs
        ov = findViewById(R.id.track_manager_overlay);
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
        if (mainapp.track_manager_msg_handler == null)
            mainapp.track_manager_msg_handler = new track_manager_handler();
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
        Log.d("EX_Toolbox", "track_manager.onDestroy() called");

        if (mainapp.track_manager_msg_handler !=null) {
            mainapp.track_manager_msg_handler.removeCallbacksAndMessages(null);
            mainapp.track_manager_msg_handler = null;
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
            if (mainapp.track_manager_msg_handler!=null) {
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
        inflater.inflate(R.menu.track_manager_menu, menu);
        tMenu = menu;

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
            case R.id.servos_mnu:
                navigateAway(true, null);
                in = new Intent().setClass(this, servos.class);
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
        }
    }

    @SuppressLint("ApplySharedPref")
    public void forceRestartApp(int forcedRestartReason) {
        Log.d("EX-Toolbox", "track_manager.forceRestartApp() ");
        Message msg = Message.obtain();
        msg.what = message_type.RESTART_APP;
        msg.arg1 = forcedRestartReason;
        mainapp.comm_msg_handler.sendMessage(msg);
    }

//**************************************************************************************

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

    public class write_tracks_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            Integer typeIndex;
            String type;
            Integer id;
            char trackLetter;

            for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
                if (mainapp.DCCEXtrackAvailable[i]) {
                    trackLetter = (char) ('A' + i);
                    typeIndex = dccExTrackTypeSpinner[i].getSelectedItemPosition();
                    type = TRACK_TYPES[typeIndex];
                    mainapp.DCCEXtrackType[i] = typeIndex;

                    if (!TRACK_TYPES_NEED_ID[typeIndex]) {
                        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK, trackLetter + " " + type, 0);
                    } else {
                        try {
                            id = Integer.parseInt(dccExTrackTypeIdEditText[i].getText().toString());
                            mainapp.DCCEXtrackId[i] = id.toString();
                            if (mainapp.DCCEXtrackType[i] != TRACK_TYPE_OFF_INDEX) {
                                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK, trackLetter + " " + type, id);
                            }
                        } catch (Exception e) {
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
            mainapp.DCCEXresponsesListHtml.clear();
            mainapp.DCCEXsendsListHtml.clear();
            mainapp.DCCEXresponsesStr = "";
            mainapp.DCCEXsendsStr = "";
            refreshDCCEXview();
        }
    }

    private void resetTextField(int which) {
        switch (which) {
            case WHICH_ADDRESS:
                break;
            case WHICH_CV:
                break;
            case WHICH_CV_VALUE:
                break;
            case WHICH_COMMAND:
                DCCEXsendCommandValue = "";
                etDCCEXsendCommandValue.setText("");
        }
    }

    private void readTextField(int which) {
        switch (which) {
            case WHICH_ADDRESS:
                break;
            case WHICH_CV:
                break;
            case WHICH_CV_VALUE:
                break;
            case WHICH_COMMAND:
                DCCEXsendCommandValue = etDCCEXsendCommandValue.getText().toString();
        }
    }

    private void showHideButtons() {
//        DCCEXwriteInfoLayout.setVisibility(View.GONE);
//        dexcDCCEXtrackLinearLayout.setVisibility(View.VISIBLE);

        for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
            dccExTrackTypeIdEditText[i].setVisibility(TRACK_TYPES_NEED_ID[dccExTrackTypeIndex[i]] ? View.VISIBLE : View.GONE);
        }
        sendCommandButton.setEnabled((DCCEXsendCommandValue.length() != 0) && (DCCEXsendCommandValue.charAt(0) != '<'));
        previousCommandButton.setEnabled((mainapp.DCCEXpreviousCommandIndex >= 0));
        nextCommandButton.setEnabled((mainapp.DCCEXpreviousCommandIndex >= 0));
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

    public void refreshDCCEXtracksView() {

        for (int i = 0; i< threaded_application.DCCEX_MAX_TRACKS; i++) {
            dccExTrackTypeSpinner[i].setSelection(mainapp.DCCEXtrackType[i]);
            dccExTrackTypeIdEditText[i].setText(mainapp.DCCEXtrackId[i]);
            dccExTrackTypeLayout[i].setVisibility(mainapp.DCCEXtrackAvailable[i] ? View.VISIBLE : View.GONE);
        }
        showHideButtons();

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

            dccExTrackTypeIndex[myIndex] = mySpinner.getSelectedItemPosition();
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

}
