/*Copyright (C) 2018 M. Steve Todd
  mstevetodd@gmail.com

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

// Main java file.
/* TODO: see changelog-and-todo-list.txt for complete list of project to-do's */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import dcc_ex.ex_toolbox.type.Consist;
import dcc_ex.ex_toolbox.type.message_type;
import dcc_ex.ex_toolbox.import_export.ImportExport;
import dcc_ex.ex_toolbox.util.LocaleHelper;
import dcc_ex.ex_toolbox.util.PermissionsHelper;
import dcc_ex.ex_toolbox.comms.comm_handler;
import dcc_ex.ex_toolbox.comms.comm_thread;
import jmri.jmrit.roster.RosterEntry;

//The application will start up a thread that will handle network communication in order to ensure that the UI is never blocked.
//This thread will only act upon messages sent to it. The network communication needs to persist across activities, so that is why
@SuppressLint("NewApi")
public class threaded_application extends Application {
    public static String INTRO_VERSION = "10";  // set this to a different string to force the intro to run on next startup.

    private threaded_application mainapp = this;
    public comm_thread commThread;
    public volatile String host_ip = null; //The IP address of the WiThrottle server.
    public volatile String logged_host_ip = null;
    public volatile int port = 0; //The TCP port that the WiThrottle server is running on
//    public Double withrottle_version = 0.0; //version of withrottle server
    public volatile int web_server_port = 0; //default port for jmri web server
    private String serverType = ""; //should be set by server in initial command strings
    private String serverDescription = ""; //may be set by server in initial command strings
    public volatile boolean doFinish = false;  // when true, tells any Activities that are being created/resumed to finish()
    //shared variables returned from the withrottle server, stored here for easy access by other activities
    public volatile Consist[] consists;
    public LinkedHashMap<Integer, String>[] function_labels;  //function#s and labels from roster for throttles
    public LinkedHashMap<Integer, String> function_labels_default;  //function#s and labels from local settings
    LinkedHashMap<Integer, String> function_labels_default_for_roster;  //function#s and labels from local settings for roster entries with no function labels
    LinkedHashMap<Integer, String> function_consist_locos; // used for the 'special' consists function label string matching
    public LinkedHashMap<Integer, String> function_consist_latching; // used for the 'special' consists function label string matching

    public boolean[][] function_states = {null, null, null, null, null, null};  //current function states for throttles
    public String[] to_system_names;
    public String[] to_user_names;
    public String[] to_states;
    public HashMap<String, String> to_state_names;
    public String[] rt_system_names;
    public String[] rt_user_names;
    public String[] rt_states;
    public HashMap<String, String> rt_state_names; //if not set, routes are not allowed
    public Map<String, String> roster_entries;  //roster sent by WiThrottle
    public Map<String, String> consist_entries;
//    public static DownloadRosterTask dlRosterTask = null;
//    private static DownloadMetaTask dlMetadataTask = null;
    HashMap<String, RosterEntry> roster;  //roster entries retrieved from /roster/?format=xml (null if not retrieved)
//    public static HashMap<String, String> jmriMetadata = null;  //metadata values (such as JMRIVERSION) retrieved from web server (null if not retrieved)
    public String power_state;

    public int getFastClockFormat() {
        return fastClockFormat;
    }

    public int fastClockFormat = 0; //0=no display, 1=12hr, 2=24hr
    public Long fastClockSeconds = 0L;
    public int androidVersion = 0;
    public String appVersion = "";
    //minimum Android version for some features
    public final int minImmersiveModeVersion = android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
    public final int minThemeVersion = android.os.Build.VERSION_CODES.HONEYCOMB;
    public final int minScreenDimNewMethodVersion = Build.VERSION_CODES.KITKAT;
    public final int minActivatedButtonsVersion = Build.VERSION_CODES.ICE_CREAM_SANDWICH;

    //all heartbeat values are in milliseconds
    public static final int DEFAULT_OUTBOUND_HEARTBEAT_INTERVAL = 10000; //interval for outbound heartbeat when WiT heartbeat is disabled
    public static final int MIN_OUTBOUND_HEARTBEAT_INTERVAL = 1000;   //minimum allowed interval for outbound heartbeat generator
    public static final int MAX_OUTBOUND_HEARTBEAT_INTERVAL = 30000;  //maximum allowed interval for outbound heartbeat generator
//    static final double HEARTBEAT_RESPONSE_FACTOR = 0.9;   //adjustment for inbound and outbound timers
    public static final int MIN_INBOUND_HEARTBEAT_INTERVAL = 1000;   //minimum allowed interval for (enabled) inbound heartbeat generator
    public static final int MAX_INBOUND_HEARTBEAT_INTERVAL = 60000;  //maximum allowed interval for inbound heartbeat generator
    public int heartbeatInterval = 0;                       //WiT heartbeat interval setting (milliseconds)
    public int prefHeartbeatResponseFactor = 90;   //adjustment for inbound and outbound timers as a percent

    public static int WiThrottle_Msg_Interval = 100;   //minimum desired interval (ms) between messages sent to
    //  WiThrottle server, can be chgd for specific servers
    //   do not exceed 200, unless slider delay is also changed

    public String deviceId = "";

    public String client_address; //address string of the client address
    public Inet4Address client_address_inet4; //inet4 value of the client address
    public String client_ssid = "UNKNOWN";    //string of the connected SSID
    public String client_type = "UNKNOWN"; //network type, usually WIFI or MOBILE

    public HashMap<String, String> knownDCCEXserverIps = new HashMap<>();
    public boolean isDCCEX = true;  // is a DCC-EX EX-CommandStation
    public String DCCEXversion = "";
    public double DCCEXversionValue = 0.0;
    public static final double DCCEX_MIN_VERSION_FOR_TRACK_MANAGER = 04.002007;
    public static final double DCCEX_MIN_VERSION_FOR_CURRENTS = 04.002019;
    public int DCCEXlistsRequested = -1;  // -1=not requested  0=requested  1,2,3= no. of lists received

    public boolean DCCEXscreenIsOpen = false;

    public int [] DCCEXtrackType = {1, 2, 0, 0, 0, 0, 0, 0};
    public int [] DCCEXtrackPower = {-1, -1, -1, -1, -1, -1, -1, -1};
    public boolean [] DCCEXtrackAvailable = {false, false, false, false, false, false, false, false};
    public String [] DCCEXtrackId = {"", "", "", "", "", "", "", ""};
    public final static int DCCEX_MAX_TRACKS = 8;

    public String rosterStringDCCEX = ""; // used to process the roster list
    public int [] rosterIDsDCCEX;  // used to process the roster list
    public String [] rosterLocoNamesDCCEX;  // used to process the roster list
    public String [] rosterLocoFunctionsDCCEX;  // used to process the roster list
    public boolean [] rosterDetailsReceivedDCCEX;  // used to process the roster list

    public final static int DCCEX_MAX_SENSORS = 10;
    public int sensorDCCEXcount = 0;
    public int [] sensorIDsDCCEX;  // used to process the sensor list
    public int [] sensorVpinsDCCEX;  // used to process the sensor list
    public int [] sensorPullupsDCCEX;  // used to process the sensor list

    public int [] [] currentsDCCEX = { {0, 0, 0, 0,  0, 0, 0, 0}, {0, 0, 0, 0,  0, 0, 0, 0} };  // used to process the currents list
    public int [] currentsHighestDCCEX = {0, 0, 0, 0,  0, 0, 0, 0};  // used to process the currents list
    public int [] currentsMaxDCCEX = {0, 0, 0, 0,  0, 0, 0, 0};  // used to process the currents list

    public static int LATEST_VALUE = 0;
    public static int PREVIOUS_VALUE = 1;

    public int DCCEXpreviousCommandIndex = -1;
    public ArrayList<String> DCCEXpreviousCommandList = new ArrayList<>();

    public boolean [] [] throttleFunctionIsLatchingDCCEX = {null, null, null, null, null, null};
    public String [][] throttleLocoReleaseListDCCEX = {null, null, null, null, null, null};  // used to process the list of locos to release on a throttle

    public boolean turnoutsBeingProcessedDCCEX = false;
    public String turnoutStringDCCEX = ""; // used to process the turnout list
    public int [] turnoutIDsDCCEX;  // used to process the turnout list
    public String [] turnoutNamesDCCEX;  // used to process the turnout list
    public String [] turnoutStatesDCCEX;  // used to process the turnout list
    public boolean [] turnoutDetailsReceivedDCCEX;  // used to process the turnout list

    public boolean routesBeingProcessedDCCEX = false;
    public String routeStringDCCEX = ""; // used to process the route list
    public int [] routeIDsDCCEX;  // used to process the route list
    public String [] routeNamesDCCEX;  // used to process the route list
    public String [] routeTypesDCCEX;  // used to process the route list
    public String [] routeStatesDCCEX;  // used to process the route list
    public boolean [] routeDetailsReceivedDCCEX;  // used to process the route list

    public ArrayList<String> DCCEXresponsesListHtml = new ArrayList<>();
    public ArrayList<String> DCCEXsendsListHtml = new ArrayList<>();

    //For communication to the comm_thread.
    public comm_handler comm_msg_handler = null;
    //For communication to each of the activities (set and unset by the activity)
    public volatile Handler connection_msg_handler;
    public volatile Handler power_control_msg_handler;

    public volatile Handler dcc_ex_msg_handler;
    public volatile Handler cv_programmer_msg_handler;
    public volatile Handler servos_msg_handler;
    public volatile Handler track_manager_msg_handler;
    public volatile Handler sensors_msg_handler;
    public volatile Handler currents_msg_handler;
    public volatile Handler locos_msg_handler;

    public volatile Handler reconnect_status_msg_handler;
    public volatile Handler preferences_msg_handler;
    public volatile Handler settings_msg_handler;
    public volatile Handler logviewer_msg_handler;
    public volatile Handler about_page_msg_handler;

    //these constants are used for onFling
    public static final int SWIPE_MIN_DISTANCE = 120;
    public static final int SWIPE_MAX_OFF_PATH = 250;
    public static final int SWIPE_THRESHOLD_VELOCITY = 200;
    public static int min_fling_distance;           // pixel width needed for fling
    public static int min_fling_velocity;           // velocity needed for fling

    private static final int ED_NOTIFICATION_ID = 416;  //no significance to 416, just shouldn't be 0

    private SharedPreferences prefs;

    @NonNull
    public String connectedHostName = "";
    @NonNull
    public String connectedHostip = "";
    public int getConnectedPort() {
        return connectedPort;
    }
    public int connectedPort = 0;
    public String connectedSsid = "";

    public String languageCountry = "en";

    public boolean appIsFinishing = false;
    public boolean introIsRunning = false;

    private boolean exitConfirmed = false;
    private ApplicationLifecycleHandler lifecycleHandler;
    public static Context context;

    public static final int FORCED_RESTART_REASON_NONE = 0;
    public static final int FORCED_RESTART_REASON_RESET = 1;
//    public static final int FORCED_RESTART_REASON_IMPORT = 2;
//    public static final int FORCED_RESTART_REASON_IMPORT_SERVER_MANUAL = 3;
    public static final int FORCED_RESTART_REASON_THEME = 4;
//    public static final int FORCED_RESTART_REASON_THROTTLE_PAGE = 5;
    public static final int FORCED_RESTART_REASON_LOCALE = 6;
    public static final int FORCED_RESTART_REASON_IMPORT_SERVER_AUTO = 7;
//    public static final int FORCED_RESTART_REASON_AUTO_IMPORT = 8; // for local server files
    public static final int FORCED_RESTART_REASON_BACKGROUND = 9;
//    public static final int FORCED_RESTART_REASON_THROTTLE_SWITCH = 10;
    public static final int FORCED_RESTART_REASON_FORCE_WIFI = 11;
//    public static final int FORCED_RESTART_REASON_IMMERSIVE_MODE = 12;
//    public static final int FORCED_RESTART_REASON_DEAD_ZONE = 13;
    public static final int FORCED_RESTART_REASON_SHAKE_THRESHOLD = 14;

    public int actionBarIconCountThrottle = 0;
    public int actionBarIconCountRoutes = 0;
    public int actionBarIconCountTurnouts = 0;

    public Resources.Theme theme;

    public boolean webServerNameHasBeenChecked = false;

    public boolean haveForcedWiFiConnection = false;
    public boolean prefAllowMobileData = false;

    public boolean prefShowTimeOnLogEntry = false;
    public boolean prefFeedbackOnDisconnect = true;

    public String prefHapticFeedback = "None";
    //    public int prefHapticFeedbackSteps = 10;
    public int prefHapticFeedbackDuration = 250;
    public boolean prefHapticFeedbackButtons = false;

    /// swipe right sequence
    public static final int SCREEN_SWIPE_INDEX_CV_PROGRAMMER = 0;
    public static final int SCREEN_SWIPE_INDEX_LOCOS = 1;
    public static final int SCREEN_SWIPE_INDEX_SERVOS = 2;
    public static final int SCREEN_SWIPE_INDEX_SENSORS = 3;
    public static final int SCREEN_SWIPE_INDEX_CURRENTS = 4;
    public static final int SCREEN_SWIPE_INDEX_TRACK_MANGER = 5;
    public static final int SCREEN_SWIPE_INDEX_TURNTABLE = 6;
    public static final int SCREEN_SWIPE_INDEX_DIAG = 7;

    public boolean prefBackgroundImage = false;
    public String prefBackgroundImageFileName = "";
    public String prefBackgroundImagePosition = "FIT_CENTER";

    public boolean prefThrottleViewImmersiveModeHideToolbar = true;
    public boolean prefActionBarShowServerDescription = false;

    public static final String HAPTIC_FEEDBACK_NONE = "None";
    public static final String HAPTIC_FEEDBACK_SLIDER = "Slider";
    public static final String HAPTIC_FEEDBACK_SLIDER_SCALED = "Scaled";

    public int[] lastKnownSpeedDCCEX = {0,0,0,0,0,0};
    public int[] lastKnownDirDCCEX = {0,0,0,0,0,0};

    public String DCCEXresponsesStr = "";
    public String DCCEXsendsStr = "";

    public ImportExport importExport = new ImportExport();

//    public boolean prefActionBarShowDccExButton = false;

    /**
     * Display OnGoing Notification that indicates EngineDriver is Running.
     * Should only be called when ED is going into the background.
     * Currently call this from each activity onPause, passing the current intent
     * to return to when reopening.
     */
    private void addNotification(Intent notificationIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "ed_channel_HIGH";// The id of the channel.
            String CHANNEL_ID_QUIET = "ed_channel_HIGH_quiet";// The id of the channel without sound.
            String channelId;
            CharSequence name = this.getString(R.string.notification_title);// The user-visible name of the channel.
            NotificationChannel mChannel;

            boolean prefFeedbackWhenGoingToBackground = prefs.getBoolean("prefFeedbackWhenGoingToBackground", getResources().getBoolean(R.bool.prefFeedbackWhenGoingToBackgroundDefaultValue));
            if (prefFeedbackWhenGoingToBackground) {
                channelId = CHANNEL_ID;
                mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
            } else {
                channelId = CHANNEL_ID_QUIET;
                mChannel = new NotificationChannel(CHANNEL_ID_QUIET, name, NotificationManager.IMPORTANCE_DEFAULT);
                mChannel.setSound(null, null);
            }

            PendingIntent contentIntent = PendingIntent.getActivity(this, ED_NOTIFICATION_ID, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Notification notification =
                    new Notification.Builder(this)
                            .setSmallIcon(R.drawable.icon_notification)
                            .setContentTitle(this.getString(R.string.notification_title))
                            .setContentText(this.getString(R.string.notification_text))
                            .setContentIntent(contentIntent)
                            .setOngoing(true)
                            .setChannelId(channelId)
                            .build();

            // Add as notification
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(mChannel);
            manager.notify(ED_NOTIFICATION_ID, notification);
        } else {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.icon)
                            .setContentTitle(this.getString(R.string.notification_title))
                            .setContentText(this.getString(R.string.notification_text))
                            .setOngoing(true);

            PendingIntent contentIntent = PendingIntent.getActivity(this, ED_NOTIFICATION_ID, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.setContentIntent(contentIntent);

            // Add as notification
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(ED_NOTIFICATION_ID, builder.build());
            safeToast(threaded_application.context.getResources().getString(R.string.notification_title), Toast.LENGTH_LONG);
        }
    }

    // Remove notification
    private void removeNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(ED_NOTIFICATION_ID);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("EX_Toolbox", "t_a.onCreate()");
        try {
            appVersion = "v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        androidVersion = android.os.Build.VERSION.SDK_INT;

        Log.i("EX_Toolbox", "Engine Driver:" + appVersion + ", SDK:" + androidVersion);

        context = getApplicationContext();

        prefs = getSharedPreferences("dcc_ex.ex_toolbox_preferences", 0);

        commThread = new comm_thread(mainapp, prefs);

        lifecycleHandler = new ApplicationLifecycleHandler();
        registerActivityLifecycleCallbacks(lifecycleHandler);
        registerComponentCallbacks(lifecycleHandler);

        haveForcedWiFiConnection = false;

        mainapp.sensorDCCEXcount =0;
        mainapp.sensorIDsDCCEX = new int[100];
        mainapp.sensorVpinsDCCEX = new int[100];
        mainapp.sensorPullupsDCCEX = new int[100];

        //setup some legacy stuff from ED
        function_states[0] = new boolean[32];

        try {
            Map<String, ?> ddd = prefs.getAll();
            String dwr = prefs.getString("TypeThrottle", "false");
        } catch (Exception ex) {
            String dwr = ex.toString();
        }

//        dlMetadataTask = new DownloadMetaTask();
//        dlRosterTask = new DownloadRosterTask();

        CookieSyncManager.createInstance(this);     //create this here so onPause/onResume for webViews can control it

        prefShowTimeOnLogEntry = prefs.getBoolean("prefShowTimeOnLogEntry",
                getResources().getBoolean(R.bool.prefShowTimeOnLogEntryDefaultValue));
        prefFeedbackOnDisconnect = prefs.getBoolean("prefFeedbackOnDisconnect",
                getResources().getBoolean(R.bool.prefFeedbackOnDisconnectDefaultValue));


    } // end onCreate


    public class ApplicationLifecycleHandler implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
        private boolean isInBackground = true;
        private Activity runningActivity = null;

        @Override
        public void onActivityCreated(@NonNull Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            if (isInBackground) {                           // if coming out of background
                isInBackground = false;
                exitConfirmed = false;
                removeNotification();
            }
            runningActivity = activity;                 // save most recently resumed activity
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            if (isInBackground && activity == runningActivity) {
                removeNotification();           // destroyed in background so remove notification
            }
        }

        @Override
        public void onConfigurationChanged(@NonNull Configuration configuration) {
        }

        @Override
        public void onLowMemory() {
        }

        @Override
        public void onTrimMemory(int level) {
            if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {   // if in background
                if (!isInBackground) {                              // if just went into bkg
                    isInBackground = true;
                    if (!exitConfirmed) {                       // if user did not just confirm exit
                        addNotification(runningActivity.getIntent());
//                    } else {                                    // user confirmed exit
                    }
                }
                if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) { // time to kill app
                    if (!exitConfirmed) {       // if TA is running in bkg
                        // disconnect and shutdown
                        sendMsg(comm_msg_handler, message_type.DISCONNECT, "", 1);
                        exitConfirmed = true;
                    }
                }
            }
        }
    }

    public boolean isForcingFinish() {
        return doFinish;
    }

    public void cancelForcingFinish() {
        doFinish = false;
    }

//    public class DownloadRosterTask extends DownloadDataTask {
//        @SuppressWarnings("unchecked")
//        @Override
//        void runMethod(Download dl) throws IOException {
//            String rosterUrl = createUrl("roster/?format=xml");
//            HashMap<String, RosterEntry> rosterTemp;
//            if (rosterUrl == null || rosterUrl.equals("") || dl.cancel)
//                return;
//            Log.d("EX_Toolbox", "t_a: Background loading roster from " + rosterUrl);
//            int rosterSize;
//            try {
//                RosterLoader rl = new RosterLoader(rosterUrl);
//                if (dl.cancel)
//                    return;
//                rosterTemp = rl.parse();
//                if (rosterTemp == null) {
//                    Log.w("EX_Toolbox", "t_a: Roster parse failed.");
//                    return;
//                }
//                rosterSize = rosterTemp.size();     //throws exception if still null
//                if (!dl.cancel)
//                    roster = (HashMap<String, RosterEntry>) rosterTemp.clone();
//            } catch (Exception e) {
//                throw new IOException();
//            }
//            Log.d("EX_Toolbox", "t_a: Loaded " + rosterSize + " entries from /roster/?format=xml.");
//        }
//    }

    abstract class DownloadDataTask {
        private Download dl = null;

        abstract void runMethod(Download dl) throws IOException;

        public class Download extends Thread {
            public volatile boolean cancel = false;

            @Override
            public void run() {
                try {
                    runMethod(this);
                    if (!cancel) {
                        Log.d("EX_Toolbox", "t_a: sendMsg - message - ROSTER_UPDATE");
                        sendMsg(comm_msg_handler, message_type.ROSTER_UPDATE);      //send message to alert other activities
                    }
                } catch (Throwable t) {
                    Log.d("EX_Toolbox", "t_a: Data fetch failed: " + t.getMessage());
                }

                // background load of Data completed
                finally {
                    if (cancel) {
                        Log.d("EX_Toolbox", "t_a: Data fetch cancelled");
                    }
                }
                Log.d("EX_Toolbox", "t_a: Data fetch ended");
            }

            Download() {
                super("DownloadData");
            }
        }

        public void get() {
            if (dl != null) {
                dl.cancel = true;   // try to stop any update that is in progress on old download thread
            }
            dl = new Download();    // create new download thread
            dl.start();             // start an update
        }

        public void stop() {
            if (dl != null) {
                dl.cancel = true;
            }
        }
    }

    // get the roster name from address string 123(L).  Return input string if not found in roster or in consist
    public String getRosterNameFromAddress(String addr_str, boolean returnBlankIfNotFound) {
        boolean prefRosterRecentLocoNames = true;

        if (prefRosterRecentLocoNames) {
            if ((roster_entries != null) && (roster_entries.size() > 0)) {
                for (String rosterName : roster_entries.keySet()) {  // loop thru roster entries,
                    if (roster_entries.get(rosterName).equals(addr_str)) { //looking for value = input parm
                        return rosterName;  //if found, return the roster name (key)
                    }
                }
            }
            if (getConsistNameFromAddress(addr_str) != null) { //check for a JMRI consist for this address
                return getConsistNameFromAddress(addr_str);
            }
        }
        if (returnBlankIfNotFound) return "";

        return addr_str; //return input if not found
    }

    public String getConsistNameFromAddress(String addr_str) {
        if (addr_str.charAt(0) == 'S' || addr_str.charAt(0) == 'L') { //convert from S123 to 123(S) formatting if needed
            addr_str = cvtToAddrP(addr_str);
        }
        if ((consist_entries != null) && (consist_entries.size() > 0)) {
            return consist_entries.get(addr_str);  //consists are keyed by address "123(L)"
        }
        return null;
    }

//    public int getWhichThrottleFromAddress(String addr_str, int startAt) {
////        if (addr_str.charAt(0) == 'S' || addr_str.charAt(0) == 'L') { //convert from S123 to 123(S) formatting if needed
////            addr_str = cvtToAddrP(addr_str);
////        }
//        // assumes "S123" "L333" type address, not "123(S)"
//        Consist con = null;
//        int whichThrottle = -1;
//        boolean found = false;
//        if (consists.length>=startAt) {
//            for (int i=startAt; i<consists.length; i++) {
//                con = mainapp.consists[i];
//                for (Consist.ConLoco l : con.getLocos()) {
//                    if (l.getAddress().equals(addr_str)) {
//                        found = true;
//                        whichThrottle = i;
//                        break;
//                    }
//                }
//                if (found) break;
//            }
//        }
//        return whichThrottle;
//    }


    //convert a string of form L123 to 123(L)
    public String cvtToAddrP(String addr_str) {
        return addr_str.substring(1) + "(" + addr_str.substring(0, 1) + ")";
    }

    //convert a string of form 123(L) to L123
    public String cvtToLAddr(String addr_str) {
        String[] sa = splitByString(addr_str, "(");  //split on the "("
        if (sa.length == 2) {
            return sa[1].substring(0, 1) + sa[0]; //move length to front and return format L123
        } else {
            return addr_str; //just return input if format not as expected
        }
    }

    public String getServerType() {
        return this.serverType;
    }

    /* handle server-specific settings here */
    public void setServerType(String serverType) {
        this.serverType = serverType;
        if (serverType.equals("MRC")) {
            web_server_port = 80; //hardcode web port for MRC
        } else if (serverType.equals("Digitrax")) {
            WiThrottle_Msg_Interval = 200; //increase the interval for LnWi
        } else if ( (serverType.equals("DCC-EX")) && (isDCCEX) ) {
            WiThrottle_Msg_Interval = 100; //increase the interval for DCC-EX
        }
    }

    public String getServerDescription() {
        return this.serverDescription;
    }

    public void setServerDescription(String serverDescription) {
        this.serverDescription = serverDescription;
    }

    //reinitialize statics in activities as required to be ready for next launch
    public static void reinitStatics() {
//        throttle.initStatics();
//        throttle_full.initStatics();
    }

    //initialize shared variables
    public void initShared() {
//        withrottle_version = 0.0;
        web_server_port = 0;
        host_ip = null;
        setServerType("");
        setServerDescription("");
//        jmriMetadata = null;
        power_state = null;
        to_states = null;
        to_system_names = null;
        to_user_names = null;
        to_state_names = null;
        rt_states = null;
        rt_system_names = null;
        rt_user_names = null;
        rt_state_names = null;

        DCCEXversion = "";
        DCCEXversionValue = 0.0;
        DCCEXlistsRequested = -1;
        DCCEXscreenIsOpen = false;

        rosterStringDCCEX = "";
        turnoutStringDCCEX = "";
        routeStringDCCEX = "";

        doFinish = false;
    }

    //
    // utilities
    //

    /**
     * ------ copied from jmri util code -------------------
     * Split a string into an array of Strings, at a particular
     * divider.  This is similar to the new String.split method,
     * except that this does not provide regular expression
     * handling; the divider string is just a string.
     *
     * @param input   String to split
     * @param divider Where to divide the input; this does not appear in output
     */
    static public String[] splitByString(String input, String divider) {

        //bail on empty input string, return input as single element
        if (input == null || input.length() == 0) return new String[]{input};

        int size = 0;
        String temp = input;

        // count entries
        while (temp.length() > 0) {
            size++;
            int index = temp.indexOf(divider);
            if (index < 0) break;    // break not found
            temp = temp.substring(index + divider.length());
            if (temp.length() == 0) {  // found at end
                size++;
                break;
            }
        }

        String[] result = new String[size];

        // find entries
        temp = input;
        size = 0;
        while (temp.length() > 0) {
            int index = temp.indexOf(divider);
            if (index < 0) break;    // done with all but last
            result[size] = temp.substring(0, index);
            temp = temp.substring(index + divider.length());
            size++;
        }
        result[size] = temp;

        return result;
    }

    public void powerStateMenuButton() {
        int newState = 1;
        if ("1".equals(power_state)) {          //toggle to opposite value 0=off, 1=on
            newState = 0;
        }
        sendMsg(comm_msg_handler, message_type.POWER_CONTROL, "", newState);
    }

    public void displayPowerStateMenuButton(Menu menu) {
        if (prefs.getBoolean("show_layout_power_button_preference", false) && (power_state != null)) {
            actionBarIconCountThrottle++;
            actionBarIconCountRoutes++;
            actionBarIconCountTurnouts++;
            menu.findItem(R.id.power_layout_button).setVisible(true);
        } else {
            menu.findItem(R.id.power_layout_button).setVisible(false);
        }
    }

    public void powerControlNotAllowedDialog(Menu pMenu) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setTitle(getApplicationContext().getResources().getString(R.string.powerWillNotWorkTitle));
        b.setMessage(getApplicationContext().getResources().getString(R.string.powerWillNotWork));
        b.setCancelable(true);
        b.setNegativeButton("OK", null);
        AlertDialog alert = b.create();
        alert.show();
        displayPowerStateMenuButton(pMenu);
    }

    /**
     * for menu passed in, hide or show the power menu option
     *
     * @param menu - menu object that will be adjusted
     */
    public void setPowerMenuOption(Menu menu) {
        if (menu != null) {
            MenuItem item = menu.findItem(R.id.power_control_mnu);
            if (item != null) {
                item.setVisible(isPowerControlAllowed());
            }
        }
    }

    public void setTrackmanagerMenuOption(Menu menu) {
        if (menu != null) {
            MenuItem item = menu.findItem(R.id.track_manager_mnu);
            if (item != null) {
                item.setVisible(mainapp.DCCEXversionValue > mainapp.DCCEX_MIN_VERSION_FOR_TRACK_MANAGER);
            }
        }
    }

    public void setCurrentsMenuOption(Menu menu) {
        if (menu != null) {
            MenuItem item = menu.findItem(R.id.currents_mnu);
            if (item != null) {
                item.setVisible(mainapp.DCCEXversionValue > mainapp.DCCEX_MIN_VERSION_FOR_CURRENTS);
            }
        }
    }

    public void setMenuItemById(Menu menu, int id, boolean show) {
        if (menu != null) {
            MenuItem item = menu.findItem(id);
            if (item != null) {
                item.setVisible(show);
            }
        }
    }

    /**
     * Is Power Control allowed for this connection?
     * this hides/shows menu options and activities
     *
     * @return true or false
     */
    public boolean isPowerControlAllowed() {
        return (power_state != null);
    }

    public void setPowerStateButton(Menu menu) {
        if (menu != null) {
            TypedValue outValue = new TypedValue();
            if ((power_state == null) || (power_state.equals("2"))) {
                theme.resolveAttribute(R.attr.ed_power_yellow_button, outValue, true);
                menu.findItem(R.id.power_layout_button).setIcon(outValue.resourceId);
                menu.findItem(R.id.power_layout_button).setTitle("Layout Power is UnKnown");
            } else if (power_state.equals("1")) {
                theme.resolveAttribute(R.attr.ed_power_green_button, outValue, true);
                menu.findItem(R.id.power_layout_button).setIcon(outValue.resourceId);
                menu.findItem(R.id.power_layout_button).setTitle("Layout Power is ON");
            } else {
                theme.resolveAttribute(R.attr.ed_power_red_button, outValue, true);
                menu.findItem(R.id.power_layout_button).setIcon(outValue.resourceId);
                menu.findItem(R.id.power_layout_button).setTitle("Layout Power is Off");
            }
        }
    }

    // forward a message to all running activities
    public void alert_activities(int msgType, String msgBody) {
        try {
            sendMsg(connection_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }

        try {
            sendMsg(cv_programmer_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }

        try {
            sendMsg(servos_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }

        try {
            sendMsg(sensors_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }

        try {
            sendMsg(currents_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }

        try {
            sendMsg(track_manager_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }

        try {
            sendMsg(locos_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }

        try {
            sendMsg(settings_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(logviewer_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(about_page_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(power_control_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }

        // *********************************

        try {
            sendMsg(dcc_ex_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
        try {
            sendMsg(reconnect_status_msg_handler, msgType, msgBody);
        } catch (Exception ignored) {
        }
    }

    public boolean sendMsg(Handler h, int msgType) {
        return sendMsgDelay(h, 0, msgType, "", 0, 0);
    }

    public boolean sendMsg(Handler h, int msgType, String msgBody) {
        return sendMsgDelay(h, 0, msgType, msgBody, 0, 0);
    }

    public boolean sendMsg(Handler h, int msgType, String msgBody, int msgArg1) {
        return sendMsgDelay(h, 0, msgType, msgBody, msgArg1, 0);
    }

    public boolean sendMsg(Handler h, int msgType, String msgBody, int msgArg1, int msgArg2) {
        return sendMsgDelay(h, 0, msgType, msgBody, msgArg1, msgArg2);
    }

    public boolean sendMsgDelay(Handler h, long delayMs, int msgType, int msgArg1) {
        return sendMsgDelay(h, delayMs, msgType, "", msgArg1, 0);
    }

    public boolean sendMsgDelay(Handler h, long delayMs, int msgType) {
        return sendMsgDelay(h, delayMs, msgType, "", 0, 0);
    }

    public boolean sendMsgDelay(Handler h, long delayMs, int msgType, String msgBody) {
        return sendMsgDelay(h, delayMs, msgType, msgBody, 0, 0);
    }

    public boolean sendMsgDelay(Handler h, long delayMs, int msgType, String msgBody, int msgArg1, int msgArg2) {
        boolean sent = false;
        if (h != null) {
            Message msg = Message.obtain();
            msg.what = msgType;
            msg.obj = msgBody;
            msg.arg1 = msgArg1;
            msg.arg2 = msgArg2;
            try {                           // handler access is not locked and might have been removed by activity
                sent = h.sendMessageDelayed(msg, delayMs);
            } catch (Exception e) {
                try {                       // exception could be that handler is gone so use another try/catch here
                    h.removeCallbacksAndMessages(null);
                } catch (Exception ignored) {
                }
            }
            if (!sent)
                msg.recycle();
        }
        return sent;
    }

    //
    // methods for use by Activities
    //

    // build a full url
    // returns: full url    if web_server_port is valid
    //          empty string   otherwise
    public String createUrl(String defaultUrl) {
        String url = "";
        int port = web_server_port;
        if (getServerType().equals("MRC")) {  //special case ignore any url passed-in if connected to MRC, as it does not forward
            defaultUrl = "";
            Log.d("EX_Toolbox", "t_a: ignoring web url for MRC");
        }
        if (port > 0) {
            if (defaultUrl.startsWith("http")) { //if url starts with http, use it as is
                url = defaultUrl;
            } else { //, else prepend servername and port and slash if needed           
                url = "http://" + host_ip + ":" + port + (defaultUrl.startsWith("/") ? "" : "/") + defaultUrl;
            }
        }
        return url;
    }

    // build a full uri
    // returns: full uri    if webServerPort is valid
    //          null        otherwise
    public String createUri() {
        String uri = "";
        int port = web_server_port;
        if (port > 0) {
            uri = "ws://" + host_ip + ":" + port + "/json/";
        }
        return uri;
    }


    public int getSelectedTheme() {
        return getSelectedTheme(false);
    }

    public int getSelectedTheme(boolean isPreferences) {
        String prefTheme = getCurrentTheme();
        if (!isPreferences) {  // not a preferences activity
            switch (prefTheme) {
                case "Black":
                    return R.style.app_theme_black;
                case "Outline":
                    return R.style.app_theme_outline;
                case "Ultra":
                    return R.style.app_theme_ultra;
                case "Colorful":
                    return R.style.app_theme_colorful;
                default:
                    return R.style.app_theme;
            }
        } else {
            switch (prefTheme) {
                case "Colorful":
//                    return R.style.app_theme_colorful_preferences;
                case "Black":
                case "Outline":
                case "Ultra":
                    return R.style.app_theme_black_preferences;
                default:
                    return R.style.app_theme_preferences;
            }
        }
    }

    /**
     * Applies the chosen theme from preferences to the specified activity
     *
     * @param activity the activity to set the theme for
     */
    public void applyTheme(Activity activity) {
        applyTheme(activity, false);
    }

    public void applyTheme(Activity activity, boolean isPreferences) {
        int selectedTheme = getSelectedTheme(isPreferences);
        activity.setTheme(selectedTheme);
        Log.d("EX_Toolbox", "t_a: applyTheme: " + selectedTheme);
        theme = activity.getTheme();

    }


    /**
     * Retrieve the currently configure theme from preferences
     *
     * @return a String representation of the selected theme
     */
    public String getCurrentTheme() {
        return prefs.getString("prefTheme", threaded_application.context.getResources().getString(R.string.prefThemeDefaultValue));
    }

    /**
     * Return fastClockSeconds as formatted time string
     *
     * @return a String representation of the time
     */
    public String getFastClockTime() {
        String f = "";
        if (fastClockFormat == 2) {
            f = "HH:mm"; // display in 24 hr format
        } else if (fastClockFormat == 1) {
            f = "h:mm a"; // display in 12 hr format
        }
        SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = new java.util.Date((fastClockSeconds * 1000L));
        return sdf.format(date);
    }

    /**
     * Set activity screen orientation based on prefs, check to avoid sending change when already there.
     * checks "auto Web on landscape" preference and returns false if orientation requires activity switch
     * Uses web orientation pref if called from web_activity, uses throttle orientation pref otherwise
     *
     * @param activity calling activity
     * @return true if the new orientation is ok for this activity.
     * false if "Auto Web on Landscape" is enabled and new orientation requires activity switch
     */
    @SuppressLint("SourceLockedOrientationActivity")
    public boolean setActivityOrientation(Activity activity) {
//        boolean isWeb = (activity.getLocalClassName().equals("web_activity"));
//        String to = prefs.getString("ThrottleOrientation",
//                threaded_application.context.getResources().getString(R.string.prefThrottleOrientationDefaultValue));
//        try {
//            int co = activity.getRequestedOrientation();
//            if (to.equals("Landscape") && (co != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE))
//                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//            else if ((to.equals("Auto-Rotate")) || (to.equals("Auto-Web")) && (co != ActivityInfo.SCREEN_ORIENTATION_SENSOR))
//                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
//            else if (to.equals("Portrait") && (co != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT))
//                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        } catch (Exception e) {
//            Log.e("EX_Toolbox", "t_a: setActivityOrientation: Unable to change Orientation: " + e.getMessage());
//        }

        return true;
    }

    public void checkExit(final Activity activity) {
        checkExit(activity, false);
    }

        // prompt for Exit
    // must be called on the UI thread
    public void checkExit(final Activity activity, boolean forceFastDisconnect) {
        final AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setTitle(R.string.exit_title);
        b.setMessage(R.string.exit_text);
        b.setCancelable(true);
        b.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                exitConfirmed = true;
                if (!forceFastDisconnect) {
                    sendMsg(comm_msg_handler, message_type.DISCONNECT, "");  //trigger disconnect / shutdown sequence
                } else {
                    sendMsg(comm_msg_handler, message_type.DISCONNECT, "", 1);  //trigger fast disconnect / shutdown sequence
                }
                buttonVibration();
            }
        });
        b.setNegativeButton(R.string.no, null);
        AlertDialog alert = b.create();
        alert.show();

        // find positiveButton and negativeButton
        Button positiveButton = alert.findViewById(android.R.id.button1);
        Button negativeButton = alert.findViewById(android.R.id.button2);
        // then get their parent ViewGroup
        ViewGroup buttonPanelContainer = (ViewGroup) positiveButton.getParent();
        int positiveButtonIndex = buttonPanelContainer.indexOfChild(positiveButton);
        int negativeButtonIndex = buttonPanelContainer.indexOfChild(negativeButton);
        if (positiveButtonIndex < negativeButtonIndex) {  // force 'No' 'Yes' order
            // prepare exchange their index in ViewGroup
            buttonPanelContainer.removeView(positiveButton);
            buttonPanelContainer.removeView(negativeButton);
            buttonPanelContainer.addView(negativeButton, positiveButtonIndex);
            buttonPanelContainer.addView(positiveButton, negativeButtonIndex);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        languageCountry = Locale.getDefault().getLanguage();
        if (!Locale.getDefault().getCountry().equals("")) {
            languageCountry = languageCountry + "_" + Locale.getDefault().getCountry();
        }
        super.attachBaseContext(LocaleHelper.onAttach(base, languageCountry));
    }

/*    public void displayMenuSeparator(Menu menu, Activity activity, int actionBarIconCount) {
        MenuItem mi = menu.findItem(R.id.separator);
        if (mi == null) return;

        if ((activity.getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT)
                && (actionBarIconCount > 2)) {
            mi.setVisible(true);
        } else if ((activity.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE)
                && (actionBarIconCount > 3)) {
            mi.setVisible(true);
        } else {
            mi.setVisible(false);
        }
    }*/

    public int Numeralise(String value) {
        switch (value) {
            case "One":
                return 1;
            case "Two":
                return 2;
            case "Three":
                return 3;
            case "Four":
                return 4;
            case "Five":
                return 5;
            case "Six":
                return 6;
        }
        return 1; // default to 1 in case of problems
    }

    //
    // map '0'-'9' to 0-9
    // map WiT deprecated throttle name characters T,S,G to throttle values
    //
    public int throttleCharToInt(char cWhichThrottle) {
        int val;
        if (Character.isDigit(cWhichThrottle)) {  // throttle number
            val = Character.getNumericValue((cWhichThrottle));
            if (val < 0) val = 0;
//            if (val >= maxThrottles) val = maxThrottles - 1;
            if (val >= 1) val = 0;
        } else switch (cWhichThrottle) {          // WiT protocol deprecated throttle letter codes
            case 'T':
                val = 0;
                break;
            case 'S':
                val = 1;
                break;
            case 'G':
                val = 2;
                break;
            default:
                val = 0;
                Log.d("debug", "TA.throttleCharToInt: no match for argument " + cWhichThrottle);
                break;
        }
//        if (val > maxThrottlesCurrentScreen)
        if (val > 1)
            Log.d("debug", "TA.throttleCharToInt: argument exceeds max number of throttles for current screen " + cWhichThrottle);
        return val;
    }

    public String throttleIntToString(int whichThrottle) {
        return Integer.toString(whichThrottle);
    }

    public void vibrate(int duration) {
        //we need vibrate permissions, otherwise do nothing
        PermissionsHelper phi = PermissionsHelper.getInstance();
        if (phi.isPermissionGranted(threaded_application.context, PermissionsHelper.VIBRATE)) {
            try {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //deprecated in API 26
                    v.vibrate(duration);
                }
            } catch (Exception ignored) {
            }
        }

    }

    public void vibrate(long[] pattern) {
        //we need vibrate permissions, otherwise do nothing
        PermissionsHelper phi = PermissionsHelper.getInstance();
        if (phi.isPermissionGranted(threaded_application.this, PermissionsHelper.VIBRATE)) {
            try {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(pattern, -1));
                } else {
                    //deprecated in API 26
                    v.vibrate(pattern, -1);
                }
            } catch (Exception ignored) {
            }
        }

    }

    //post process loco or Consist names to reduce the size of the address length strings
    public String locoAndConsistNamesCleanupHtml(String name) {
        return name.replaceAll("[(]S[)]", "<small><small>(S)</small></small>")
                .replaceAll("[(]L[)]", "<small><small>(L)</small></small>")
                .replaceAll("[+]", "<small>+</small>");
    }

    //display msg using Toast() safely by ensuring Toast() is called from the UI Thread
    public static void safeToast(final String msg_txt) {
        safeToast(msg_txt, Toast.LENGTH_SHORT);
    }

    public static void safeToast(final String msg_txt, final int length) {
        Log.d("EX_Toolbox", "t_a.show_toast_message: " + msg_txt);
        //need to do Toast() on the main thread so create a handler
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(threaded_application.context, msg_txt, length).show();
            }
        });
    }

    public Intent getCvProgrammerIntent() {
        Intent cv_programer;
        appIsFinishing = false;
        cv_programer = new Intent().setClass(this, cv_programmer.class);
        return cv_programer;
    }

    public Intent getNextIntentInSwipeSequence(int currentScreen, float deltaX) {

        int nextScreen;
        if (deltaX <= 0.0) {
            nextScreen = currentScreen + 1;
            if ( (nextScreen == SCREEN_SWIPE_INDEX_CURRENTS) && (mainapp.DCCEXversionValue <= mainapp.DCCEX_MIN_VERSION_FOR_CURRENTS) ) {
                nextScreen++;
            }
            if ( (nextScreen == SCREEN_SWIPE_INDEX_TRACK_MANGER) && (mainapp.DCCEXversionValue <= mainapp.DCCEX_MIN_VERSION_FOR_TRACK_MANAGER) ) {
                nextScreen++;
            }
            if (nextScreen > SCREEN_SWIPE_INDEX_TRACK_MANGER) {
                nextScreen = SCREEN_SWIPE_INDEX_CV_PROGRAMMER;
            }
        } else {
            nextScreen = currentScreen - 1;
            if (nextScreen < SCREEN_SWIPE_INDEX_CV_PROGRAMMER) {
                nextScreen = SCREEN_SWIPE_INDEX_TRACK_MANGER;
            }
            if ( (nextScreen == SCREEN_SWIPE_INDEX_TRACK_MANGER) && (mainapp.DCCEXversionValue <= mainapp.DCCEX_MIN_VERSION_FOR_TRACK_MANAGER) ) {
                nextScreen--;
            }
            if ( (nextScreen == SCREEN_SWIPE_INDEX_CURRENTS) && (mainapp.DCCEXversionValue <= mainapp.DCCEX_MIN_VERSION_FOR_CURRENTS) ) {
                nextScreen--;
            }
        }

        Intent nextIntent;
        switch (nextScreen) {
            case SCREEN_SWIPE_INDEX_CV_PROGRAMMER:
            default:
                nextIntent = new Intent().setClass(this, cv_programmer.class);
                break;
            case SCREEN_SWIPE_INDEX_LOCOS:
                nextIntent = new Intent().setClass(this, locos.class);
                break;
            case SCREEN_SWIPE_INDEX_SERVOS:
                nextIntent = new Intent().setClass(this, servos.class);
                break;
            case SCREEN_SWIPE_INDEX_SENSORS:
                nextIntent = new Intent().setClass(this, sensors.class);
                break;
            case SCREEN_SWIPE_INDEX_CURRENTS:
                nextIntent = new Intent().setClass(this, currents.class);
                break;
            case SCREEN_SWIPE_INDEX_TRACK_MANGER:
                nextIntent = new Intent().setClass(this, track_manager.class);
                break;
        }
        return nextIntent;
    }

    /***
     * show appropriate messages on a restart that was forced by prefs
     *
     * @param prefForcedRestartReason the reason that the restart occurred
     * @return true if the activity should immediately launch Preferences
     */
    public boolean prefsForcedRestart(int prefForcedRestartReason) {
        switch (prefForcedRestartReason) {
            case FORCED_RESTART_REASON_RESET: {
                Toast.makeText(context, context.getResources().getString(R.string.toastPreferencesResetSucceeded), Toast.LENGTH_LONG).show();
                break;
            }
            case FORCED_RESTART_REASON_THEME: {
                Toast.makeText(context, context.getResources().getString(R.string.toastPreferencesThemeChangeSucceeded), Toast.LENGTH_LONG).show();
                break;
            }
            case FORCED_RESTART_REASON_BACKGROUND: {
                Toast.makeText(context, context.getResources().getString(R.string.toastPreferencesBackgroundChangeSucceeded), Toast.LENGTH_LONG).show();
                break;
            }
            case FORCED_RESTART_REASON_LOCALE: {
                Toast.makeText(context, context.getResources().getString(R.string.toastPreferencesLocaleChangeSucceeded), Toast.LENGTH_LONG).show();
                break;
            }
            case FORCED_RESTART_REASON_IMPORT_SERVER_AUTO: {
                Toast.makeText(context,
                        context.getResources().getString(R.string.toastPreferencesImportServerAutoSucceeded, prefs.getString("prefPreferencesImportFileName", "")),
                        Toast.LENGTH_LONG).show();
                break;
            }
            case FORCED_RESTART_REASON_FORCE_WIFI: {
                Toast.makeText(context, context.getResources().getString(R.string.toastPreferencesChangedForceWiFi),
                        Toast.LENGTH_LONG).show();
                break;
            }
        }

        // include in this list if the Settings Activity should NOT be launched
        return ((prefForcedRestartReason != FORCED_RESTART_REASON_BACKGROUND)
                && (prefForcedRestartReason != FORCED_RESTART_REASON_RESET)
                && (prefForcedRestartReason != FORCED_RESTART_REASON_FORCE_WIFI)
                && (prefForcedRestartReason != FORCED_RESTART_REASON_SHAKE_THRESHOLD));
    }

    /* only DCC-EX supports the "Request Loco ID" feature at this time */
    public boolean supportsIDnGo() {
        return serverType.equals("DCC-EX");
    }

    public boolean supportsRoster() {
        //true if roster entries exist
        if ((roster_entries != null) && (roster_entries.size() > 0)) return true;
        //always show roster panel for these entries
        return (serverType.equals("JMRI") || serverType.equals("MRC") || serverType.equals("DCC-EX"));
    }

    public void buttonVibration() {
        if (prefHapticFeedbackButtons) {
            vibrate(prefHapticFeedbackDuration * 2);
        }
    }

    public String getHostIp() {
        return host_ip;
    }

//    public Double getWithrottleVersion() {
//        return withrottle_version;
//    }

    public String getDCCEXVersion() {
        return DCCEXversion;
    }

    public double getDCCEXVersionValue() {
        return DCCEXversionValue;
    }

    static public int getIntPrefValue(SharedPreferences sharedPreferences, String key, String defaultVal) {
        int newVal;
        try {
            newVal = Integer.parseInt(sharedPreferences.getString(key, defaultVal).trim());
        } catch (NumberFormatException e) {
            try {
                newVal = Integer.parseInt(defaultVal);
            } catch (NumberFormatException ex) {
                newVal = 0;
            }
        }
        return newVal;
    }

    public void setToolbarTitle(Toolbar toolbar, String title, String iconTitle, String clockText) {
        if (toolbar != null) {
            toolbar.setTitle("");
            TextView tvTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
            tvTitle.setText(title);

            TextView tvIconTitle = (TextView) toolbar.findViewById(R.id.toolbar_icon_title);
            tvIconTitle.setText(iconTitle);

            TextView tvIconHelp = (TextView) toolbar.findViewById(R.id.toolbar_icon_help);
            tvIconHelp.setText("");

            TextView tvToolbarServerDesc;
            int screenLayout = context.getResources().getConfiguration().screenLayout;
            screenLayout &= Configuration.SCREENLAYOUT_SIZE_MASK;
            if (screenLayout >= Configuration.SCREENLAYOUT_SIZE_XLARGE) {
                tvToolbarServerDesc = (TextView) toolbar.findViewById(R.id.toolbar_server_desc_x_large);
            } else {
                tvToolbarServerDesc = (TextView) toolbar.findViewById(R.id.toolbar_server_desc);
            }
            if (prefActionBarShowServerDescription) {
                tvToolbarServerDesc.setText(getServerDescription());
                tvToolbarServerDesc.setVisibility(View.VISIBLE);
            } else {
                tvToolbarServerDesc.setVisibility(View.GONE);
            }

            TextView mClock = (TextView) toolbar.findViewById(R.id.toolbar_clock);
            mClock.setText(clockText);

        }
    }

//    public class app_icon_button_listener implements View.OnClickListener {
//        public void onClick(View v) {
//            // tba
//            buttonVibration();
//        }
//    }

    public void hideSoftKeyboard(View view) {
        // Check if no view has focus:
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void hideSoftKeyboardAfterDialog() {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    // get the consist for the specified throttle
    private static final Consist emptyConsist = new Consist();

    public Consist getConsist(int whichThrottle) {
        if (consists == null || whichThrottle >= consists.length || consists[whichThrottle] == null)
            return emptyConsist;
        return consists[whichThrottle];
    }

    public String fixFilename(String fName) {
        String rslt = "";
        if (fName!=null) {
            rslt = fName.replaceAll("[\\\\/:\"*?<>|]", "_").trim();
        }
        return rslt;
    }

    public int getFadeIn(boolean swipe, float deltaX) {
        int fadeIn = R.anim.fade_in;
        if (swipe) {
            if (deltaX > 0.0) {
                    fadeIn = R.anim.push_right_in;
            } else {
                    fadeIn = R.anim.push_left_in;
            }
        }
        return fadeIn;
    }

    public int getFadeOut(boolean swipe, float deltaX) {
        int fadeOut = R.anim.fade_out;
        if (swipe) {
            if (deltaX > 0.0) {
                    fadeOut = R.anim.push_right_out;
            } else {
                    fadeOut = R.anim.push_left_out;
            }
        }
        return fadeOut;
    }

    // Function to extract k bits from p position and returns the extracted value as integer
    // from: https://www.geeksforgeeks.org/extract-k-bits-given-position-number/
    public int bitExtracted(int number, int k, int p)
    {
        return (((1 << k) - 1) & (number >> (p - 1)));
    }

    public int toggleBit(int n, int k) {
        return (n ^ (1 << (k - 1)));
    }

//    // for DCC-EX we need to temp store the list of locos so we can remove them individually
//    public void storeThrottleLocosForReleaseDCCEX(int whichThrottle) {
//        if (isDCCEX) {
//            Consist con = mainapp.consists[whichThrottle];
//            throttleLocoReleaseListDCCEX[whichThrottle] = new String [con.size()];
//            int i=0;
//            for (Consist.ConLoco l : con.getLocos()) {
//                throttleLocoReleaseListDCCEX[whichThrottle][i] = l.getAddress();
//                i++;
//            }
//        }
//    }

    void getCommonPreferences() {
        prefActionBarShowServerDescription = prefs.getBoolean("prefActionBarShowServerDescription",
                getResources().getBoolean(R.bool.prefActionBarShowServerDescriptionDefaultValue));

        prefBackgroundImage = prefs.getBoolean("prefBackgroundImage", getResources().getBoolean(R.bool.prefBackgroundImageDefaultValue));
        prefBackgroundImageFileName = prefs.getString("prefBackgroundImageFileName", getResources().getString(R.string.prefBackgroundImageFileNameDefaultValue));
        prefBackgroundImagePosition = prefs.getString("prefBackgroundImagePosition", getResources().getString(R.string.prefBackgroundImagePositionDefaultValue));
    }


    public void loadBackgroundImage(ImageView myImage) {
        if (prefBackgroundImage) {
            if ( ( (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                && (PermissionsHelper.getInstance().isPermissionGranted(this, PermissionsHelper.READ_IMAGES)) )
            || ( (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                && (PermissionsHelper.getInstance().isPermissionGranted(this, PermissionsHelper.READ_MEDIA_IMAGES)) ) ) {
                        loadBackgroundImageImpl(myImage);
                        myImage.invalidate();
            }
        }
    }

    public void loadBackgroundImageImpl(ImageView myImage) {
//        ImageView myImage = findViewById(R.id.backgroundImgView);
        try {
            File image_file = new File(prefBackgroundImageFileName);
            myImage.setImageBitmap(BitmapFactory.decodeFile(image_file.getPath()));
            switch (prefBackgroundImagePosition){
                case "FIT_CENTER":
                    myImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    break;
                case "CENTER_CROP":
                    myImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    break;
                case "CENTER":
                    myImage.setScaleType(ImageView.ScaleType.CENTER);
                    break;
                case "FIT_XY":
                    myImage.setScaleType(ImageView.ScaleType.FIT_XY);
                    break;
                case "CENTER_INSIDE":
                    myImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    break;
            }
        } catch (Exception e) {
            Log.d("ex_toolbox", "Throttle: failed loading background image");
        }
    }

}
