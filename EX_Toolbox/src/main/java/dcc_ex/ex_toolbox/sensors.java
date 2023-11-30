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
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import dcc_ex.ex_toolbox.logviewer.ui.LogViewerActivity;
import dcc_ex.ex_toolbox.type.message_type;
import dcc_ex.ex_toolbox.util.LocaleHelper;

public class sensors extends AppCompatActivity implements GestureOverlayView.OnGestureListener {

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

    Button readSensorsButton;
    Button clearCommandsButton;

//    private LinearLayout[] dccExSensorLayouts = {null, null, null, null, null,  null, null, null, null, null};
//    private TextView[] dccExSensorStatusTextView = {null, null, null, null, null,  null, null, null, null, null};
//    private TextView[] dccExSensorIdTextView = {null, null, null, null, null,  null, null, null, null, null};
//    private TextView[] dccExSensorVpinsTextView = {null, null, null, null, null,  null, null, null, null, null};
//    private TextView[] dccExSensorPullupsTextView = {null, null, null, null, null,  null, null, null, null, null};
//
//    private int[] DCCEXsensorStatus= {-1, -1, -1, -1, -1,  -1, -1, -1, -1, -1};
//    private int[] DCCEXsensorIds = {0, 0, 0, 0, 0,  0, 0, 0, 0, 0};
//    private int[] DCCEXsensorVpins = {0, 0, 0, 0, 0,  0, 0, 0, 0, 0};
//    private int[] DCCEXsensorPullups = {1, 1, 1, 1, 1,  1, 1, 1, 1, 1};

    static final String SENSOR_STATUS_UNKNOWN = "-1";
    static final String SENSOR_STATUS_INACTIVE = "0";
    static final String SENSOR_STATUS_ACTIVE = "1";

    private ArrayList<HashMap<String, String>> sensors_list;
    private SimpleAdapter sensors_list_adapter;

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
        if (mainapp.sensors_msg_handler != null)
            mainapp.sensors_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
    }

    public void gestureMove(MotionEvent event) {
        // Log.d("Engine_Driver", "gestureMove action " + event.getAction());
        if ( (mainapp != null) && (mainapp.sensors_msg_handler != null) && (gestureInProgress) ) {
            // stop the gesture timeout timer
            mainapp.sensors_msg_handler.removeCallbacks(gestureStopped);

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
                mainapp.sensors_msg_handler.postDelayed(gestureStopped, gestureCheckRate);
            }
        }
    }

    private void gestureEnd(MotionEvent event) {
        // Log.d("Engine_Driver", "gestureEnd action " + event.getAction() + " inProgress? " + gestureInProgress);
        if ( (mainapp != null) && (mainapp.sensors_msg_handler != null) && (gestureInProgress) ) {
            mainapp.sensors_msg_handler.removeCallbacks(gestureStopped);

            float deltaX = (event.getX() - gestureStartX);
            float absDeltaX =  Math.abs(deltaX);
            if (absDeltaX > threaded_application.min_fling_distance) { // only process left/right swipes
                // valid gesture. Change the event action to CANCEL so that it isn't processed by any control below the gesture overlay
                event.setAction(MotionEvent.ACTION_CANCEL);
                // process swipe in the direction with the largest change
                Intent nextScreenIntent = mainapp.getNextIntentInSwipeSequence(threaded_application.SCREEN_SWIPE_INDEX_SENSORS, deltaX);
                startACoreActivity(this, nextScreenIntent, true, deltaX);
            } else {
                // gesture was not long enough
                gestureFailed(event);
            }
        }
    }

    private void gestureCancel(MotionEvent event) {
        if (mainapp.sensors_msg_handler != null)
            mainapp.sensors_msg_handler.removeCallbacks(gestureStopped);
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
    class sensors_handler extends Handler {

        public void handleMessage(Message msg) {
            switch (msg.what) {

                case message_type.RESPONSE: {    //handle messages from server
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
                case message_type.RECEIVED_SENSOR:
                    String s = msg.obj.toString();
                    if (s.length() > 0) {
                        String[] sArgs = s.split("(\\|)");
                        updateASensorListItem(sArgs[0], sArgs[1]);
                        sensors_list_adapter.notifyDataSetChanged();

                        refreshDCCEXsensorsView();
                    }
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
                    getApplicationContext().getResources().getString(R.string.app_name_sensors_short),
                    mainapp.getFastClockTime());
        else
            mainapp.setToolbarTitle(toolbar,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_sensors),
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

        setContentView(R.layout.sensors);

        mainapp.loadBackgroundImage(findViewById(R.id.sensorsBackgroundImgView));

        //put pointer to this activity's handler in main app's shared variable
        mainapp.sensors_msg_handler = new sensors_handler();

        DCCEXwriteInfoLayout = findViewById(R.id.dexc_DCCEXwriteInfoLayout);
        DCCEXwriteInfoLabel = findViewById(R.id.dexc_DCCEXwriteInfoLabel);
        DCCEXwriteInfoLabel.setText("");

        DCCEXresponsesLabel = findViewById(R.id.dexc_DCCEXresponsesLabel);
        DCCEXresponsesLabel.setText("");
        DCCEXsendsLabel = findViewById(R.id.dexc_DCCEXsendsLabel);
        DCCEXsendsLabel.setText("");

        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_ALL_SENSOR_DETAILS);

        //Set up a list adapter to allow adding discovered sensors to the UI.
        sensors_list = new ArrayList<>();
        sensors_list_adapter = new SimpleAdapter(this, sensors_list, R.layout.sensors_list_item,
                new String[]{"sensorId", "vpin", "pullup", "status"},
                new int[]{R.id.sensor_id_value, R.id.sensor_vpin_value, R.id.sensor_pullup_value, R.id.sensor_status});
        ListView locos_list = findViewById(R.id.sensors_list);
        locos_list.setAdapter(sensors_list_adapter);

        readSensorsButton = findViewById(R.id.dexc_DCCEXreadSensorsButton);
        read_sensors_button_listener readSensorsClickListener = new read_sensors_button_listener();
        readSensorsButton.setOnClickListener(readSensorsClickListener);


        DCCEXresponsesScrollView = findViewById(R.id.dexc_DCCEXresponsesScrollView);
        DCCEXsendsScrollView = findViewById(R.id.dexc_DCCEXsendsScrollView);

        clearCommandsButton = findViewById(R.id.dexc_DCCEXclearCommandsButton);
        clear_commands_button_listener clearCommandsClickListener = new clear_commands_button_listener();
        clearCommandsButton.setOnClickListener(clearCommandsClickListener);

//        resetSensorTextFields();
        refreshDCCEXsensorsView();

//        mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_SENSOR, "");
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
        Log.d("EX_Toolbox", "sensors.onResume() called");
        mainapp.applyTheme(this);

        super.onResume();

        mainapp.getCommonPreferences();

        setActivityTitle();
        mainapp.DCCEXscreenIsOpen = true;
        refreshDCCEXview();
        refreshDCCEXsensorsView();

        if (mainapp.isForcingFinish()) {    //expedite
            this.finish();
            return;
        }

        setIdsFromResponses();

            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.TIME_CHANGED);    // request time update
        CookieSyncManager.getInstance().startSync();

        // enable swipe/fling detection if enabled in Prefs
        ov = findViewById(R.id.sensors_overlay);
        ov.addOnGestureListener(this);
        ov.setEventsInterceptionEnabled(true);
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    @Override
    public void onPause() {
        Log.d("EX_Toolbox", "sensors.onPause() called");
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("EX_Toolbox", "sensors.onStart() called");
        // put pointer to this activity's handler in main app's shared variable
        if (mainapp.sensors_msg_handler == null)
            mainapp.sensors_msg_handler = new sensors_handler();
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
        Log.d("EX_Toolbox", "sensors.onDestroy() called");

        if (mainapp.sensors_msg_handler !=null) {
            mainapp.sensors_msg_handler.removeCallbacksAndMessages(null);
            mainapp.sensors_msg_handler = null;
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
            if (mainapp.sensors_msg_handler!=null) {
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
        inflater.inflate(R.menu.sensors_menu, menu);
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
            case R.id.locos_mnu:
                navigateAway(true, null);
                in = new Intent().setClass(this, locos.class);
                startACoreActivity(this, in, false, 0);
                return true;
            case R.id.servos_mnu:
                navigateAway(true, null);
                in = new Intent().setClass(this, servos.class);
                startACoreActivity(this, in, false, 0);
                return true;
            case R.id.currents_mnu:_mnu:
                navigateAway(true, null);
                in = new Intent().setClass(this, currents.class);
                startACoreActivity(this, in, false, 0);
                return true;
            case R.id.track_manager_mnu:_mnu:
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
        Log.d("EX-Toolbox", "sensors.forceRestartApp() ");
        Message msg = Message.obtain();
        msg.what = message_type.RESTART_APP;
        msg.arg1 = forcedRestartReason;
        mainapp.comm_msg_handler.sendMessage(msg);
    }

//**************************************************************************************

    @SuppressLint("DefaultLocale")
    void setIdsFromResponses() {
        for (int i=0; i<mainapp.sensorDCCEXcount; i++) {
            updateSensorsList(String.format("%d",mainapp.sensorIDsDCCEX[i]),
                String.format("%d",mainapp.sensorVpinsDCCEX[i]),
                String.format("%d",mainapp.sensorPullupsDCCEX[i]),
                SENSOR_STATUS_UNKNOWN);
        }
        sensors_list_adapter.notifyDataSetChanged();
//        showHideButtons();
    }

    void updateSensorsList(String id, String vpin, String pullup, String status) {
        HashMap<String, String> hm = new HashMap<String, String>();
        hm.put("sensorId", id);
        hm.put("vpin", vpin);
        hm.put("pullup", pullup);
        switch (status) {
            case SENSOR_STATUS_ACTIVE:
                hm.put("status", getApplicationContext().getResources().getString(R.string.DCCEXsensorActive));
                break;
            case SENSOR_STATUS_INACTIVE:
                hm.put("status", getApplicationContext().getResources().getString(R.string.DCCEXsensorInactive));
                break;
            default:
            case SENSOR_STATUS_UNKNOWN:
                hm.put("status", getApplicationContext().getResources().getString(R.string.DCCEXsensorUnknown));
                break;
        }
        String foundSensorId = hm.get("sensorId");
        boolean entryExists = false;

        //stop if new address is already in the list
        HashMap<String, String> tm;
        for (int index = 0; index < sensors_list.size(); index++) {
            tm = sensors_list.get(index);
            if (tm.get("sensorId").equals(foundSensorId)) {
                entryExists = true;
                sensors_list.set(index, hm);
                break;
            }
        }
        if (!entryExists) {                // if new loco, add to discovered list on screen
            sensors_list.add(hm);
        }
    }

    void updateASensorListItem(String id, String status) {
        HashMap<String, String> tm;
        for (int index = 0; index < sensors_list.size(); index++) {
            tm = sensors_list.get(index);
            if (tm.get("sensorId").equals(id)) {
                updateSensorsList(id, tm.get("vpin"), tm.get("pullup"), status);
                break;
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

    public class read_sensors_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            for (int i=0; i<mainapp.sensorDCCEXcount; i++) {
                if (mainapp.sensorIDsDCCEX[i]!=0) {
                    mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_SENSOR,
                            mainapp.sensorIDsDCCEX[i] + " " + Integer.toString(mainapp.sensorVpinsDCCEX[i]) + " " + Integer.toString(mainapp.sensorPullupsDCCEX[i]));
                }
            }
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

//    private void resetSensorTextFields() {
//        for (int i = 0; i < threaded_application.DCCEX_MAX_SENSORS; i++) {
//            DCCEXsensorIds[i] = 0;
//            DCCEXsensorVpins[i] = 0;
//            DCCEXsensorPullups[i] = 0;
//
//            dccExSensorIdTextView[i].setText(Integer.toString(DCCEXsensorIds[i]));
//            dccExSensorVpinsTextView[i].setText(Integer.toString(DCCEXsensorVpins[i]));
//            dccExSensorPullupsTextView[i].setText(Integer.toString(DCCEXsensorPullups[i]));
//        }
//    }
//
//
//    private void showHideButtons() {
//        for (int i = 0; i < threaded_application.DCCEX_MAX_SENSORS; i++) {
//            if (DCCEXsensorIds[i] != 0) {
//                dccExSensorLayouts[i].setVisibility(View.VISIBLE);
//            } else {
//                dccExSensorLayouts[i].setVisibility(View.GONE);
//            }
//        }
//    }

    public void refreshDCCEXview() {
        DCCEXwriteInfoLabel.setText(DCCEXinfoStr);
        refreshDCCEXcommandsView();
//        showHideButtons();

    }

    public void refreshDCCEXcommandsView() {
        DCCEXresponsesLabel.setText(Html.fromHtml(mainapp.DCCEXresponsesStr));
        DCCEXsendsLabel.setText(Html.fromHtml(mainapp.DCCEXsendsStr));
    }

    public void refreshDCCEXsensorsView() {
//        for (int i = 0; i < threaded_application.DCCEX_MAX_SENSORS; i++) {
//        }
//        showHideButtons();

    }

}
