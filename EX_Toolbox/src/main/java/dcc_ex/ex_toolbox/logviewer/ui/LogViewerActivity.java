package dcc_ex.ex_toolbox.logviewer.ui;

import static dcc_ex.ex_toolbox.threaded_application.context;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
//import android.support.annotation.NonNull;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dcc_ex.ex_toolbox.R;
import dcc_ex.ex_toolbox.type.message_type;
import dcc_ex.ex_toolbox.threaded_application;
import dcc_ex.ex_toolbox.util.PermissionsHelper;
import dcc_ex.ex_toolbox.util.PermissionsHelper.RequestCodes;

//import dcc_ex.ex_toolbox.logviewer.R;

/** @noinspection CallToPrintStackTrace*/
public class LogViewerActivity extends AppCompatActivity implements PermissionsHelper.PermissionsHelperGrantedCallback {
    private ArrayAdapter adaptor = null;
    private LogReaderTask logReaderTask = null;
    private threaded_application mainapp;  // hold pointer to mainapp

    private Button saveButton;
    private Button stopSaveButton;
    private Button shareButton;
    private TextView saveInfoTV;

//    private static final String EX_TOOLBOX_DIR = "Android\\data\\dcc_ex.ex_toolbox\\files";

    private Menu AMenu;
    private LinearLayout screenNameLine;
    private Toolbar toolbar;
    private LinearLayout statusLine;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();
        if (mainapp.isForcingFinish()) {        // expedite
            return;
        }

        mainapp.applyTheme(this);
        setContentView(R.layout.log_main);

        final ListView listView = findViewById(android.R.id.list);

        ArrayList<String> logArray = new ArrayList<>();
        adaptor = new LogStringAdaptor(this, R.layout.logitem, logArray);

        listView.setAdapter(adaptor);

        listView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
                TextView logItem = (TextView) view;
                String logItemText = logItem.getText().toString();

                final AlertDialog.Builder builder = new AlertDialog.Builder(LogViewerActivity.this);
                String text = ((TextView) view).getText().toString();
                builder.setMessage(text);
                builder.show();
                mainapp.buttonVibration();
            }
        } );

        //Set the buttons
        Button closeButton = findViewById(R.id.logviewer_button_close);
        close_button_listener close_click_listener = new close_button_listener();
        closeButton.setOnClickListener(close_click_listener);

//        Button resetButton = findViewById(R.id.logviewer_button_reset);
//        reset_button_listener reset_click_listener = new reset_button_listener();
//        resetButton.setOnClickListener(reset_click_listener);

        saveButton = findViewById(R.id.logviewer_button_save);
        SaveButtonListener saveClickListener = new SaveButtonListener();
        saveButton.setOnClickListener(saveClickListener);

        stopSaveButton = findViewById(R.id.logviewer_button_stop_save);
        StopSaveButtonListener stopSaveClickListener = new StopSaveButtonListener();
        stopSaveButton.setOnClickListener(stopSaveClickListener);

        saveInfoTV = findViewById(R.id.logviewer_info);

        shareButton = findViewById(R.id.logviewer_button_share);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainapp.buttonVibration(); // If you want vibration
                showLogFileSelectionDialog();
            }
        });

        showHideSaveButton();

        logReaderTask = new LogReaderTask();

        logReaderTask.execute();

        //put pointer to this activity's handler in main app's shared variable
        mainapp.logviewer_msg_handler = new logviewer_handler(Looper.getMainLooper());

        screenNameLine = findViewById(R.id.screen_name_line);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        statusLine = findViewById(R.id.status_line);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            toolbar.showOverflowMenu();

            SharedPreferences prefs = getSharedPreferences("dcc_ex.ex_toolbox_preferences", 0);

            mainapp.setToolbarTitle(toolbar, statusLine, screenNameLine,
                    getApplicationContext().getResources().getString(R.string.app_name) + " | ",
                    getApplicationContext().getResources().getString(R.string.app_name_log_viewer),
                    "");
        }

        Log.d("EX_Toolbox", mainapp.getAboutInfo());

    } // end onCreate

    @Override
    public void onResume() {
        super.onResume();
        if (mainapp.isForcingFinish()) {        //expedite
            this.finish();
        }

//        if (AMenu != null) {
//            mainapp.displayPowerStateMenuButton(AMenu);
//            mainapp.setPowerStateButton(AMenu);
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logviewer_menu, menu);
        AMenu = menu;
        mainapp.displayPowerStateMenuButton(menu);
        mainapp.setPowerStateButton(menu);
        mainapp.reformatMenu(menu);

        return  super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        if (item.getItemId() == R.id.power_layout_button) {
            if (!mainapp.isPowerControlAllowed()) {
                mainapp.powerControlNotAllowedDialog(AMenu);
            } else {
                mainapp.powerStateMenuButton();
            }
            mainapp.buttonVibration();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onDestroy() {
        Log.d("EX_Toolbox", "log_viewer.onDestroy() called");

        if (logReaderTask != null ) {
            logReaderTask.stopTask();
        }
        super.onDestroy();
    }

    public class close_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.buttonVibration();
            finish();
        }
    }

//    public class reset_button_listener implements View.OnClickListener {
//        public void onClick(View v) {
//
//        }
//    }

    @SuppressLint("HandlerLeak")
    class logviewer_handler extends Handler {

        public logviewer_handler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RESPONSE: {    //handle messages from WiThrottle server
                    String s = msg.obj.toString();
                    if (s.length() >= 3) {
                        String com1 = s.substring(0, 3);
                        //update power icon
                        if ("PPA".equals(com1)) {
                            mainapp.setPowerStateButton(AMenu);
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    public class SaveButtonListener  implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.buttonVibration();
            saveLogFile();
        }
    }

    public class StopSaveButtonListener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.buttonVibration();
            stopSaveLogFile();
        }
    }

    private void saveLogFile() {
        File logFile = new File(context.getExternalFilesDir(null), "logcat" + System.currentTimeMillis() + ".txt");

        try {
            Runtime.getRuntime().exec("logcat -c");
            mainapp.logcatProcess = Runtime.getRuntime().exec("logcat -f " + logFile);
            threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastSaveLogFile, logFile.toString()), Toast.LENGTH_LONG);
            mainapp.logSaveFilename = logFile.toString();
            showHideSaveButton();
            Log.d("EX_Toolbox", "Logging started to: " + logFile.toString());
            Log.d("EX_Toolbox", mainapp.getAboutInfo());
        } catch ( IOException e ) {
            e.printStackTrace();
        }

    }

    private void stopSaveLogFile() {
        if (mainapp.logcatProcess != null) {
            mainapp.logcatProcess.destroy(); // Sends SIGTERM to the process
            mainapp.logcatProcess = null;
            Log.d("EX_Toolbox", "Logcat file recording stopped.");
            threaded_application.safeToast("Logcat recording stopped.", Toast.LENGTH_SHORT);
            // You might want to update UI or mainapp.logSaveFilename here if needed
            mainapp.logSaveFilename = ""; // Or indicate it's no longer active
            showHideSaveButton();
        }
    }

    void showHideSaveButton() {
        if (mainapp.logSaveFilename.length()>0) {
            saveButton.setVisibility(View.GONE);
            stopSaveButton.setVisibility(View.VISIBLE);
            shareButton.setVisibility(View.GONE);
            saveInfoTV.setText(String.format(getApplicationContext().getResources().getString(R.string.infoSaveLogFile), mainapp.logSaveFilename) );
            saveInfoTV.setVisibility(View.GONE);
        } else {
            saveButton.setVisibility(View.VISIBLE);
            stopSaveButton.setVisibility(View.GONE);
            shareButton.setVisibility((checkHasLogFiles()) ? View.VISIBLE : View.GONE);
            saveInfoTV.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SwitchIntDef")
    public void navigateToHandler(@PermissionsHelper.RequestCodes int requestCode) {
        Log.d("EX_Toolbox", "LogViewerActivity: navigateToHandler:" + requestCode);
        if (!PermissionsHelper.getInstance().isPermissionGranted(LogViewerActivity.this, requestCode)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PermissionsHelper.getInstance().requestNecessaryPermissions(LogViewerActivity.this, requestCode);
            }
        } else {
            // Go to the correct handler based on the request code.
            // Only need to consider relevant request codes initiated by this Activity
            //noinspection SwitchStatementWithTooFewBranches
            switch (requestCode) {
//                case PermissionsHelper.STORE_LOG_FILES:
//                    Log.d("EX_Toolbox", "Preferences: Got permission for STORE_LOG_FILES - navigate to saveSharedPreferencesToFileImpl()");
//                    saveLogFileImpl();
//                    break;
                default:
                    // do nothing
                    Log.d("EX_Toolbox", "Preferences: Unrecognised permissions request code: " + requestCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(@RequestCodes int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!PermissionsHelper.getInstance().processRequestPermissionsResult(LogViewerActivity.this, requestCode, permissions, grantResults)) {
            Log.d("EX_Toolbox", "Unrecognised request - send up to super class");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private class LogStringAdaptor extends ArrayAdapter<String> {
        private List<String> objects;

        public LogStringAdaptor(Context context, int textviewid, List<String> objects) {
            super(context, textviewid, objects);

            this.objects = objects;
        }

        @Override
        public int getCount() {
            return ((null != objects) ? objects.size() : 0);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public String getItem(int position) {
            return ((null != objects) ? objects.get(position) : null);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;

            if (null == view) {
                LayoutInflater vi = (LayoutInflater) LogViewerActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = vi.inflate(R.layout.logitem, null);
            }

            String data = objects.get(position);

            if (null != data) {
                TextView textview = view.findViewById(R.id.txtLogString);
                String msg = data;
                int msgStart = data.indexOf("EX_Toolbox: "); //post-marshmallow format
                if (msgStart > 0) {
                    msg = data.substring(msgStart + 12);
                    if (mainapp.prefShowTimeOnLogEntry) {
                        int tmStart = data.indexOf(" "); //post-marshmallow format
                        String tm = data.substring(tmStart + 1,tmStart+12);
                        msg = tm + " " + msg;
                    }
                } else {
                    msgStart = data.indexOf("): "); //pre-marshmallow format
                    if (msgStart > 0) {
                        msg = data.substring(msgStart + 3);
                    }
                }
                if (!msg.substring(0,6).equals("<span>")) {
                    msg = msg.replaceAll("&", "&amp;");
                    msg = msg.replaceAll("<", "&lt;");
                    msg = msg.replaceAll(">", "&gt;");
                    if (msg.indexOf("About: ") < 0) {
                        if (mainapp.getSelectedTheme() == R.style.app_theme_colorful) {
                            msg = "<span style=\"color: #404040\">" + msg;
                        } else {
                            msg = "<span style=\"color: #CCCCCC\">" + msg;
                        }
                    } else {
                        msg = "<br/><span>" + msg;
                    }
                    if (msg.indexOf("--&gt;") > 0) {
                        msg = msg.replace("--&gt;", "</span><br/><b>--&gt;") + "</b>";
                    } else if (msg.indexOf("&lt;--") > 0) {
                        msg = msg.replace("&lt;--", "</span><br/><b>&lt;--") + "</b>";
                    } else {
                        msg = msg + "</span>";
                    }
                }
                textview.setText(Html.fromHtml(msg));

                return view;
            }
            return null;

        }
    }

    private class LogReaderTask extends AsyncTask<Void, String, Void> {
        private final String[] LOGCAT_CMD = new String[]{"logcat", "EX_Toolbox:D", "*:S"};
        //		private final int BUFFER_SIZE = 1024;

        private boolean isRunning = true;
        private Process logprocess = null;
        private BufferedReader reader = null;
        private String line = "";
        //		private String lastLine = "";

        @Override
        protected Void doInBackground(Void... params) {
            try {
                logprocess = Runtime.getRuntime().exec(LOGCAT_CMD);
            } catch (IOException e) {
                e.printStackTrace();

                isRunning = false;
            }

            try {
                //				reader = new BufferedReader(new InputStreamReader(
                //						logprocess.getInputStream()),BUFFER_SIZE);
                reader = new BufferedReader(new InputStreamReader(
                        logprocess.getInputStream()));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();

                isRunning = false;
            }

            line = "";
            //			lastLine = new String;

            try {
                while (isRunning) {
                    line = reader.readLine();
                    publishProgress(line);
                }
            } catch (IOException e) {
                e.printStackTrace();

                isRunning = false;
            }
            finally {
                if(reader != null) {
                    try {
                        reader.close();
                    }
                    catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //			if ((values[0] != null) && !values[0].equals(lastLine)) {
            if ((values[0] != null)) {
                adaptor.add(values[0]);
                adaptor.notifyDataSetChanged();
            }
            //			lastLine = values[0];
        }

        public void stopTask() {
            isRunning = false;
            if (logprocess != null) logprocess.destroy();
        }
    }

//    @SuppressLint("DefaultLocale")
//    private void logAboutInfo() {
//        String s = "";
//        // device info
//        s += "About: " + String.format("OS:%s, SDK:%s ", android.os.Build.VERSION.RELEASE, Build.VERSION.SDK_INT);
//        if (mainapp.client_address_inet4 != null) {
//            s += ", " + String.format("IP:%s", mainapp.client_address_inet4.toString().replaceAll("/", ""));
//            s += String.format(" SSID:%s Net:%s", mainapp.client_ssid, mainapp.client_type);
//        }
//
//        // EX-Toolbox version info
//        s += ", EX-ToolBox: " + mainapp.appVersion;
//        if (mainapp.getHostIp() != null) {
//            // WiT info
////            if (mainapp.getWithrottleVersion() != 0.0) {
//                s += ", WiThrottle:v" + mainapp.getDccexVersion();
//                s +=  String.format(", Heartbeat:%dms", mainapp.heartbeatInterval);
////            }
//            s += String.format(", Host:%s", mainapp.getHostIp() );
//            s += String.format(", Port:%s", mainapp.connectedPort);
//            //show server type and description if set
//            String sServer;
//            if (mainapp.getServerDescription().contains(mainapp.getServerType())) {
//                sServer = mainapp.getServerDescription();
//            } else {
//                sServer = mainapp.getServerType() + " " + mainapp.getServerDescription();
//            }
//            if (!sServer.isEmpty()) {
//                s += ", Server:" + sServer;
////            } else {
////                // otherwise show JMRI version info from web if populated
////                HashMap<String, String> JmriMetadata = threaded_application.jmriMetadata;
////                if (JmriMetadata != null && JmriMetadata.size() > 0) {
////                    s += ", JMRI v" + JmriMetadata.get("JMRIVERCANON") + " build:" + JmriMetadata.get("JMRIVERSION");
////                    if (JmriMetadata.get("activeProfile") != null) {
////                        s += ", Active Profile:" + JmriMetadata.get("activeProfile");
////                    }
////                }
//            }
//        }
//        Log.d("EX_Toolbox", s);
//    }


public boolean checkHasLogFiles() {
    mainapp.fileNamesList = new ArrayList<>();
    mainapp.namesList = new ArrayList<>();

    try {
        File dir = new File(context.getExternalFilesDir(null).getPath());
        File[] filesList = dir.listFiles();
        if (filesList != null) {
            for (File file : filesList) {
                String lowercaseFileName = file.getName().toLowerCase();
                if ( (lowercaseFileName.startsWith("logcat")) && (lowercaseFileName.endsWith(".txt")) ) {
                    return true; // got one, so just exi
                }
            }
        }
    } catch (Exception e) {
        Log.d("EX_Toolbox", "LogViewerActivity: getLogFileList(): Error trying to find log files");
    }

    return false;
}

    public ArrayList<File> getLogFilesForDialog() {
        ArrayList<File> logFiles = new ArrayList<>();
        try {
            File dir = new File(context.getExternalFilesDir(null).getPath());
            if (dir.exists() && dir.isDirectory()) {
                File[] filesList = dir.listFiles();
                if (filesList != null) {
                    for (File file : filesList) {
                        String lowercaseFileName = file.getName().toLowerCase();
                        if (lowercaseFileName.startsWith("logcat") && lowercaseFileName.endsWith(".txt")) {
                            logFiles.add(file);
                            Log.d("EX_Toolbox", "LogViewerActivity: getLogFilesForDialog(): Found: " + file.getName());
                        }
                    }
                    // Optional: Sort the files, e.g., by name or date
                    Collections.sort(logFiles, (file1, file2) -> file2.getName().compareTo(file1.getName())); // Sort descending by name (newest first if using timestamp)
                }
            }
        } catch (Exception e) {
            Log.e("EX_Toolbox", "LogViewerActivity: getLogFilesForDialog(): Error trying to find log files", e);
            threaded_application.safeToast("Error accessing log files.", Toast.LENGTH_SHORT);
        }
        return logFiles;
    }

    private void showLogFileSelectionDialog() {
        ArrayList<File> logFiles = getLogFilesForDialog();

        if (logFiles.isEmpty()) return;

        // Extract just the file names for display in the dialog
        ArrayList<String> fileNames = new ArrayList<>();
        for (File file : logFiles) {
            fileNames.add(file.getName());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.file_list_dialog, null);
        builder.setView(dialogView);

        ListView dialogListView = dialogView.findViewById(R.id.file_dialog_listview);
        Button cancelButton = dialogView.findViewById(R.id.file_dialog_button_cancel);

        // --- Setup ListView ---
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this,
                R.layout.file_list_item, // This layout MUST define the font
                R.id.file_list_item_text,    // The ID of the TextView within logfile_list_item.xml
                fileNames);
        dialogListView.setAdapter(listAdapter);

        final AlertDialog dialog = builder.create(); // Create before setting item click listener for ListView

        dialogListView.setOnItemClickListener((parent, view, position, id) -> {
            File selectedFile = logFiles.get(position);
//            threaded_application.safeToast("Selected: " + selectedFile.getName(), Toast.LENGTH_SHORT);
            shareFile(selectedFile,selectedFile.getName());
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void shareFile(File file, String fileName) {
        Uri fileUri = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".fileprovider",
                file
        );
        shareFile(fileUri, fileName);
    }

    private void shareFile(Uri fileUri, String fileName) {
        Intent chooserIntent = ShareCompat.IntentBuilder.from(this)
                .setType("text/plain")
                .setStream(fileUri)
                .setChooserTitle(getApplicationContext().getResources().getString(R.string.shareFile, fileName))
                .createChooserIntent();

        try {
            startActivity(chooserIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            threaded_application.safeToast(getApplicationContext().getResources().getString(R.string.toastNoAppToShare), Toast.LENGTH_SHORT);
        }
    }
}
