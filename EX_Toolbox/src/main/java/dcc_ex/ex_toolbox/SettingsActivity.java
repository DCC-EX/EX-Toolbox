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

The SettingActivity is a replacement for the original preferences activity that
was rewritten to support AppCompat.V7
 */

package dcc_ex.ex_toolbox;

import static dcc_ex.ex_toolbox.threaded_application.context;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    public static final int RESULT_LOAD_IMG = 1;
    private int result;                     // set to RESULT_FIRST_USER when something is edited

    private String deviceId = "";

    public SharedPreferences prefs;
    public threaded_application mainapp;  // hold pointer to mainapp

    private Toolbar toolbar;
    private Menu SAMenu;

    private String exportedPreferencesFileName = "exported_preferences.ed";
    private boolean overwiteFile = false;

    private static final String IMPORT_PREFIX = "Import- "; // these two have to be the same length
    private static final String EXPORT_PREFIX = "Export- ";

    private static final String IMPORT_EXPORT_OPTION_NONE = "None";
    private static final String IMPORT_EXPORT_OPTION_EXPORT = "Export";
    private static final String IMPORT_EXPORT_OPTION_IMPORT = "Import";
    private static final String IMPORT_EXPORT_OPTION_RESET = "Reset";
    private static final String IMPORT_EXPORT_OPTION_IMPORT_URL = "URL";

    private static String AUTO_IMPORT_EXPORT_OPTION_CONNECT_AND_DISCONNECT = "Connect Disconnect";

    private static final String EXTERNAL_URL_PREFERENCES_IMPORT = "external_url_preferences_import.ed";
    private static final String EX_TOOLBOX_DIR = "Android/data/dcc_ex.ex_toolbox/files";
    private static final String SERVER_EX_TOOLBOX_DIR = "prefs/ex_toolbox";

    private String[] prefHostImportExportEntriesFound = {"None"};
    private String[] prefHostImportExportEntryValuesFound = {"None"};

    private ProgressDialog pDialog;
    public static final int PROGRESS_BAR_TYPE = 0;

    private static final String PREF_IMPORT_ALL_FULL = "Yes";
    private static final String PREF_IMPORT_ALL_PARTIAL = "No";
    private static final String PREF_IMPORT_ALL_RESET = "-";

    private boolean forceRestartAppOnPreferencesClose = false;
    private int forceRestartAppOnPreferencesCloseReason = 0;
    private boolean forceReLaunchAppOnPreferencesClose = false;

    private boolean isInSubScreen = false;

    private String prefThrottleScreenType = "Default";
    private String prefThrottleScreenTypeOriginal = "Default";
    protected boolean prefBackgroundImage = false;
    boolean prefThrottleSwitchButtonDisplay = false;
    protected boolean prefHideSlider = false;

    private String prefConsistFollowRuleStyle = "original";
    private static final String CONSIST_FUNCTION_RULE_STYLE_ORIGINAL = "original";
    private static final String CONSIST_FUNCTION_RULE_STYLE_COMPLEX = "complex";
    private static final String CONSIST_FUNCTION_RULE_STYLE_SPECIAL_EXACT = "specialExact";
    private static final String CONSIST_FUNCTION_RULE_STYLE_SPECIAL_PARTIAL = "specialPartial";

    private boolean ignoreThisThrottleNumChange = false;

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("EX_Toolbox", "Settings: onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment;
        if (savedInstanceState == null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragment = new SettingsFragment().newInstance("Advanced Setting");
            fragmentTransaction.add(R.id.settings_preferences_frame, fragment);
            fragmentTransaction.commit();
        }

        mainapp = (threaded_application) this.getApplication();
        mainapp.applyTheme(this,true);


        prefs = getSharedPreferences("dcc_ex.ex_toolbox_preferences", 0);

        deviceId = Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        //put pointer to this activity's message handler in main app's shared variable (If needed)
        mainapp.settings_msg_handler = new SettingsActivity.settings_handler();

        //put pointer to this activity's message handler in main app's shared variable (If needed)
        mainapp.preferences_msg_handler = new SettingsActivity.settings_handler();

        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            toolbar.showOverflowMenu();
            mainapp.setToolbarTitle(toolbar,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_preferences),
                    "");
            Log.d("EX_Toolbox", "Settings: Set toolbar");
        }

    } // end onCreate

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat,
                                           PreferenceScreen preferenceScreen) {
        Log.d("EX_Toolbox", "callback called to attach the preference sub screen");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        SettingsSubScreenFragment fragment = SettingsSubScreenFragment.newInstance("Advanced Settings Subscreen");
        Bundle args = new Bundle();
        //Defining the sub screen as new root for the  subscreen
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
        fragment.setArguments(args);
        ft.replace(R.id.settings_preferences_frame, fragment, preferenceScreen.getKey());
//        ft.addToBackStack(null);
        ft.addToBackStack(preferenceScreen.getKey());
        ft.commit();

        return true;
    }

    @Override
    protected void onResume() {
        Log.d("EX_Toolbox", "Settings: onResume()");
        super.onResume();

        Log.d("EX_Toolbox", "settings.onResume() called");
        try {
            dismissDialog(PROGRESS_BAR_TYPE);
        } catch (Exception e) {
            Log.d("EX_Toolbox", "settings.onResume() no dialog to kill");
        }

        if (mainapp.isForcingFinish()) {     //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs

        if (SAMenu != null) {
            mainapp.displayPowerStateMenuButton(SAMenu);
            mainapp.setPowerStateButton(SAMenu);
        }

//        mainapp.applyTheme(this,true);

        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            toolbar.showOverflowMenu();
            mainapp.setToolbarTitle(toolbar,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_preferences),
                    "");
            Log.d("EX_Toolbox", "Settings: Set toolbar");
        }
    }

//    @Override
//    protected void onStart() {
//        Log.d("EX_Toolbox", "Settings: onStart()");
//        super.onStart();
//    }

    @Override
    protected void onDestroy() {
        Log.d("EX_Toolbox", "settings.onDestroy() called");
        super.onDestroy();
        if (mainapp.settings_msg_handler !=null) {
            mainapp.settings_msg_handler.removeCallbacksAndMessages(null);
            mainapp.settings_msg_handler = null;
        } else {
            Log.d("EX_Toolbox", "Preferences: onDestroy: mainapp.preferences_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
        if (forceRestartAppOnPreferencesClose) {
            forceRestartApp(forceRestartAppOnPreferencesCloseReason);
        }
        if (forceReLaunchAppOnPreferencesClose) {
            forceReLaunchApp(forceRestartAppOnPreferencesCloseReason);
        }
    }

    @SuppressLint("ApplySharedPref")
    public void forceRestartApp(int forcedRestartReason) {
        Log.d("EX_Toolbox", "Settings: forceRestartApp() - forcedRestartReason: " + forcedRestartReason);

        finish();
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        Message msg = Message.obtain();
        msg.what = message_type.RESTART_APP;
        msg.arg1 = forcedRestartReason;
        mainapp.comm_msg_handler.sendMessage(msg);
    }

    @SuppressLint("ApplySharedPref")
    public void forceReLaunchApp(int forcedRestartReason) {
        Log.d("EX_Toolbox", "Settings: forceRelaunchApp() ");

        finish();
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
        Message msg = Message.obtain();
        msg.what = message_type.RELAUNCH_APP;
        msg.arg1 = forcedRestartReason;
        mainapp.comm_msg_handler.sendMessage(msg);

    }


    public void reload() {
        // restart the activity so all the preferences show correctly based on what was imported / hidden
        Log.d("EX_Toolbox", "Settings: Forcing activity to recreate");
        recreate();
    }

    private void resetPreferencesDialog() {
        Log.d("EX_Toolbox", "Settings: Resetting preferences");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            //@Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        resetPreferences();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        prefs.edit().putString("prefImportExport", IMPORT_EXPORT_OPTION_NONE).commit();  //reset the preference
                        reload();
                        break;
                }
            }
        };

        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setTitle(getApplicationContext().getResources().getString(R.string.dialogConfirmResetPreferencesTitle))
                .setMessage(getApplicationContext().getResources().getString(R.string.dialogResetPreferencesQuestion))
                .setPositiveButton(R.string.yes, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener);
        ab.show();
    }

    @SuppressLint("ApplySharedPref")
    private void resetPreferences() {
        SharedPreferences.Editor prefEdit = prefs.edit();
        prefEdit.clear();
        prefEdit.commit();
        Log.d("EX_Toolbox", "Settings: Reset succeeded");
        delete_settings_file("function_settings.txt");
        delete_settings_file("connections_list.txt");
        delete_settings_file("recent_engine_list.txt");
        delete_auto_import_settings_files();

        reload();

        forceRestartApp(mainapp.FORCED_RESTART_REASON_RESET);
    }

    private void delete_auto_import_settings_files() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            File sdcard_path = Environment.getExternalStorageDirectory();
//            File dir = new File(sdcard_path, DCC_EX_DIR); // in case the folder does not already exist
            File dir = new File(context.getExternalFilesDir(null), EX_TOOLBOX_DIR);
            File[] edFiles = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File folder, String name) {
                    return name.toLowerCase().startsWith("auto_");
                }
            });
            if (edFiles != null && edFiles.length > 0){
                for (int i=0; i<edFiles.length; i++) {
                    delete_settings_file(edFiles[i].getName());
                }
            }
        }
    }

    private void delete_settings_file(String file_name) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            File sdcard_path = Environment.getExternalStorageDirectory();
//            File settings_file = new File(sdcard_path, "ex_toolbox/" + file_name);
            File settings_file = new File(context.getExternalFilesDir(null), file_name);
            if (settings_file.exists()) {
                if (settings_file.delete()) {
                    Log.d("EX_Toolbox", "Settings: " + file_name + " deleted");
                } else {
                    Log.e("EX_Toolbox", "Settings: " + file_name + " NOT deleted");
                }
            }
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    //Handle messages from the communication thread back to the UI thread.
    // currently only for the download from a URL
    @SuppressLint("HandlerLeak")
    private class settings_handler extends Handler {

        @SuppressLint("ApplySharedPref")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RESPONSE:                       // see if loco added to or removed from any throttle
                    String response_str = msg.obj.toString();
                    if (response_str.length() >= 3) {
                        char com1 = response_str.charAt(0);
                        char com2 = response_str.charAt(2);

                        String comA = response_str.substring(0, 3);
                        //update power icon
                        if ("PPA".equals(comA)) {
                            mainapp.setPowerStateButton(SAMenu);
                        }
                    }
                    break;

            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) { // dialog for the progress bar
        //noinspection SwitchStatementWithTooFewBranches
        switch (id) {
            case PROGRESS_BAR_TYPE: // we set this to 0
                pDialog = new ProgressDialog(this);
                pDialog.setMessage("Downloading file. Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(true);
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }

    protected void loadImagefromGallery() {
        // Create intent to Open Image applications like Gallery, Google Photos
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }

    //Handle pressing of the back button to end this activity
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if ((key == KeyEvent.KEYCODE_BACK) && (!isInSubScreen) ) {
            setResult(result);
            finish();  //end this activity
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        isInSubScreen = false;
        return (super.onKeyDown(key, event));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("EX_Toolbox", "Settings: onCreateOptionsMenu()");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        SAMenu = menu;
        mainapp.displayPowerStateMenuButton(menu);
        mainapp.setPowerStateButton(menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        //noinspection SwitchStatementWithTooFewBranches
        switch (item.getItemId()) {
            case R.id.power_layout_button:
                if (!mainapp.isPowerControlAllowed()) {
                    mainapp.powerControlNotAllowedDialog(SAMenu);
                } else {
                    mainapp.powerStateMenuButton();
                }
                mainapp.buttonVibration();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("EX_Toolbox", "Settings: onActivityResult()");
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK && null != data) {
                // Get the Image from data
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.MediaColumns.DATA };

                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String imgpath = cursor.getString(columnIndex);
                cursor.close();

                SharedPreferences.Editor edit=prefs.edit();
                edit.putString("prefBackgroundImageFileName",imgpath);
                edit.commit();

                forceRestartAppOnPreferencesClose = true;
                forceRestartAppOnPreferencesCloseReason = mainapp.FORCED_RESTART_REASON_BACKGROUND;
            }
            else {
                Toast.makeText(this, R.string.prefBackgroundImageFileNameNoImageSelected, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("EX_Toolbox", "Settings: Loading background image Failed: " + e.getMessage());
        }

    }

    @SuppressLint("ApplySharedPref")
    protected boolean limitIntPrefValue(PreferenceScreen prefScreen, SharedPreferences sharedPreferences, String key, int minVal, int maxVal, String defaultVal) {
        Log.d("EX_Toolbox", "Settings: limitIntPrefValue()");
        boolean isValid = true;
        EditTextPreference prefText = (EditTextPreference) prefScreen.findPreference(key);
        try {
            int newVal = Integer.parseInt(sharedPreferences.getString(key, defaultVal).trim());
            if (newVal > maxVal) {
                sharedPreferences.edit().putString(key, Integer.toString(maxVal)).commit();
                prefText.setText(Integer.toString(maxVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Integer.toString(minVal), Integer.toString(maxVal), Integer.toString(maxVal)), Toast.LENGTH_LONG).show();
            } else if (newVal < minVal) {
                sharedPreferences.edit().putString(key, Integer.toString(minVal)).commit();
                prefText.setText(Integer.toString(minVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Integer.toString(minVal), Integer.toString(maxVal), Integer.toString(minVal)), Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            sharedPreferences.edit().putString(key, defaultVal).commit();
            prefText.setText(defaultVal);
            isValid = false;
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesNotNumeric, Integer.toString(minVal), Integer.toString(maxVal), defaultVal), Toast.LENGTH_LONG).show();
        }
        return isValid;
    }

    @SuppressLint("ApplySharedPref")
    protected boolean limitFloatPrefValue(PreferenceScreen prefScreen, SharedPreferences sharedPreferences, String key, Float minVal, Float maxVal, String defaultVal) {
        Log.d("EX_Toolbox", "Settings: limitFloatPrefValue()");
        boolean isValid = true;
        EditTextPreference prefText = (EditTextPreference) prefScreen.findPreference(key);
        try {
            Float newVal = Float.parseFloat(sharedPreferences.getString(key, defaultVal).trim());
            if (newVal > maxVal) {
                sharedPreferences.edit().putString(key, Float.toString(maxVal)).commit();
                prefText.setText(Float.toString(maxVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Float.toString(minVal), Float.toString(maxVal), Float.toString(maxVal)), Toast.LENGTH_LONG).show();
            } else if (newVal < minVal) {
                sharedPreferences.edit().putString(key, Float.toString(minVal)).commit();
                prefText.setText(Float.toString(minVal));
                isValid = false;
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesOutsideLimits, Float.toString(minVal), Float.toString(maxVal), Float.toString(minVal)), Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            sharedPreferences.edit().putString(key, defaultVal).commit();
            prefText.setText(defaultVal);
            isValid = false;
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toastPreferencesNotNumeric, Float.toString(minVal), Float.toString(maxVal), defaultVal), Toast.LENGTH_LONG).show();
        }
        return isValid;
    }

    private void enableDisablePreference(PreferenceScreen prefScreen, String key, boolean enable) {
        Log.d("EX_Toolbox", "Settings: enableDisablePreference(): key: " + key);
        Preference p = prefScreen.findPreference(key);
        if (p != null) {
            p.setSelectable(enable);
            p.setEnabled(enable);
        } else {
            Log.w("EX_Toolbox", "Preference key '" + key + "' not found, not set to " + enable);
        }
    }

    private void showHideBackgroundImagePreferences(PreferenceScreen prefScreen) {
        boolean enable = true;
        if (prefBackgroundImage) {
            enable = false;
        }
        enableDisablePreference(prefScreen, "prefBackgroundImageFileNameImagePicker", !enable);
        enableDisablePreference(prefScreen, "prefBackgroundImagePosition", !enable);
    }

    public void loadSharedPreferences(){
        prefs = getSharedPreferences("dcc_ex.ex_toolbox_preferences", 0);
    }

    private void getConnectionsList() {
        String host_name;
        String host_name_filename;
        String errMsg;

        try {
//                File sdcard_path = Environment.getExternalStorageDirectory();
//                File connections_list_file = new File(sdcard_path, "ex_toolbox/connections_list.txt");
            File connections_list_file = new File(context.getExternalFilesDir(null), "connections_list.txt");

            if (connections_list_file.exists()) {
                BufferedReader list_reader = new BufferedReader(new FileReader(connections_list_file));
                while (list_reader.ready()) {
                    String line = list_reader.readLine();
                    List<String> parts = Arrays.asList(line.split(":", 3)); //split record from file, max of 3 parts
                    if (parts.size() > 1) {  //skip if not split
                        host_name = parts.get(0);
                        host_name_filename = host_name.replaceAll("[^A-Za-z0-9_]", "_") + ".ed";
                        if ((!host_name.equals("")) && (!isAlreadyInArray(prefHostImportExportEntriesFound, IMPORT_PREFIX + host_name_filename))) {
                            prefHostImportExportEntriesFound = add(prefHostImportExportEntriesFound, IMPORT_PREFIX + host_name_filename);
                            prefHostImportExportEntriesFound = add(prefHostImportExportEntriesFound, EXPORT_PREFIX + host_name_filename);
                            prefHostImportExportEntryValuesFound = add(prefHostImportExportEntryValuesFound, IMPORT_PREFIX + host_name_filename);
                            prefHostImportExportEntryValuesFound = add(prefHostImportExportEntryValuesFound, EXPORT_PREFIX + host_name_filename);
                        }
                    }
                }
                list_reader.close();
            } else {
                Log.d("settingActivity", "getConnectionsList: Recent connections not found");
            }
        } catch (IOException except) {
            errMsg = except.getMessage();
            Log.e("EX_Toolbox", "Settings: Error reading recent connections list: " + errMsg);
            Toast.makeText(getApplicationContext(), R.string.prefImportExportErrorReadingList + " " + errMsg, Toast.LENGTH_SHORT).show();
        }

    }

    private static String[] add(String[] stringArray, String newValue) {
        String[] tempArray = new String[stringArray.length + 1];
        System.arraycopy(stringArray, 0, tempArray, 0, stringArray.length);
        tempArray[stringArray.length] = newValue;
        return tempArray;
    }

    public static boolean isAlreadyInArray(String[] arr, String targetValue) {
        for (String s : arr) {
            if (s.equals(targetValue))
                return true;
        }
        return false;
    }


    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -


    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

        private threaded_application mainapp;  // hold pointer to mainapp
        private SharedPreferences prefs;

//        private int result;                     // set to RESULT_FIRST_USER when something is edited

//        private String[] prefHostImportExportEntriesFound = {"None"};
//        private String[] prefHostImportExportEntryValuesFound = {"None"};

        private String prefThemeOriginal = "Default";

        private static final String PREF_IMPORT_ALL_FULL = "Yes";
        private static final String PREF_IMPORT_ALL_PARTIAL = "No";
        private static final String PREF_IMPORT_ALL_RESET = "-";

        public String[] advancedPreferences;

        public static final int RESULT_LOAD_IMG = 1;

        protected String defaultName;
        SettingsActivity parentActivity;

        private static final String TAG = SettingsFragment.class.getName();
        public static final String PAGE_ID = "page_id";
        public static final String FRAGMENT_TAG = "my_preference_fragment";

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        public static SettingsFragment newInstance(String pageId) {
            SettingsFragment f = new SettingsFragment();
            Bundle args = new Bundle();
            args.putString(PAGE_ID, pageId);
            f.setArguments(args);
            return (f);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Log.d("EX_Toolbox", "Settings: SettingsFragment onCreatePreferences()");
                setPreferencesFromResource(R.xml.preferences, rootKey);

            Activity a = getActivity();
//            if(a instanceof SettingsActivity) {
                parentActivity = (SettingsActivity) a;
//            }

            setPreferencesUI();
        }

        @Override
        public void onResume() {
            Log.d("EX_Toolbox", "Settings: SettingsFragment onResume()");
            super.onResume();

            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);

            setPreferencesUI();

        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            super.onPreferenceTreeClick(preference);
            return false;
        }

        @Override
        public void onPause() {
            super.onPause();

            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @SuppressLint("ApplySharedPref")
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d("EX_Toolbox", "Settings: onSharedPreferenceChanged(): key: " + key);
            boolean prefForcedRestart = sharedPreferences.getBoolean("prefForcedRestart", false);

            if (!prefForcedRestart) {  // don't do anything if the preference have been loaded and we are about to reload the app.
                switch (key) {
                    case "prefScreenBrightnessDim":
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 1, 100, "5");
                        break;
                    case "prefConnectTimeoutMs":
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 100, 99999, getResources().getString(R.string.prefConnectTimeoutMsDefaultValue));
                        break;
                    case "prefSocketTimeoutMs":
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 100, 9999, getResources().getString(R.string.prefSocketTimeoutMsDefaultValue));
                        break;
                    case "prefHeartbeatResponseFactor":
                        parentActivity.limitIntPrefValue(getPreferenceScreen(), sharedPreferences, key, 50, 90, getResources().getString(R.string.prefHeartbeatResponseFactorDefaultValue));
                        break;
                    case "ThrottleOrientation":
                        //if mode was fixed (Port or Land) won't get callback so need explicit call here
                        parentActivity.mainapp.setActivityOrientation(parentActivity);
                        break;
                    case "ClockDisplayTypePreference":
                        parentActivity.mainapp.sendMsg(parentActivity.mainapp.comm_msg_handler, message_type.CLOCK_DISPLAY_CHANGED);
                        break;
                    case "prefLocale":
                        sharedPreferences.edit().putString("prefLeftDirectionButtons", "").commit();
                        sharedPreferences.edit().putString("prefRightDirectionButtons", "").commit();
                        sharedPreferences.edit().putString("prefLeftDirectionButtonsShort", "").commit();
                        sharedPreferences.edit().putString("prefRightDirectionButtonsShort", "").commit();
                        parentActivity.forceReLaunchAppOnPreferencesClose = true;
                        parentActivity.forceRestartApp(mainapp.FORCED_RESTART_REASON_LOCALE);
                        break;

                    case "prefTheme":
                        String prefTheme = sharedPreferences.getString("prefTheme", parentActivity.getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));
                        if (!prefTheme.equals(prefThemeOriginal)) {
                            parentActivity.forceRestartApp(mainapp.FORCED_RESTART_REASON_THEME);
                        }
                        break;

                    case "prefShowAdvancedPreferences":
                        parentActivity.reload();
                        break;

                    case "prefAllowMobileData":
                        parentActivity.mainapp.haveForcedWiFiConnection = false;
                        parentActivity.forceRestartAppOnPreferencesCloseReason = mainapp.FORCED_RESTART_REASON_FORCE_WIFI;
                        parentActivity.forceReLaunchAppOnPreferencesClose = true;
                        break;

                    case "prefFeedbackOnDisconnect":
                        mainapp.prefFeedbackOnDisconnect = sharedPreferences.getBoolean("prefFeedbackOnDisconnect",
                                getResources().getBoolean(R.bool.prefFeedbackOnDisconnectDefaultValue));
                        break;

                    case "prefHapticFeedbackButtons":
                        mainapp.prefHapticFeedbackButtons = prefs.getBoolean("prefHapticFeedbackButtons", getResources().getBoolean(R.bool.prefHapticFeedbackButtonsDefaultValue));
                        break;

                }
            }
        }

        void setPreferencesUI() {
            Log.d("EX_Toolbox", "Settings: setPreferencesUI()");
            prefs = parentActivity.prefs;
//            defaultName = parentActivity.getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
            defaultName = "EX-Toolbox";

            mainapp = parentActivity.mainapp;
            if (mainapp != null) {
//                mainapp.applyTheme(parentActivity, true);

                if (!mainapp.isPowerControlAllowed()) {
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "show_layout_power_button_preference", false);
                }
                if (mainapp.androidVersion < mainapp.minImmersiveModeVersion) {
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefThrottleViewImmersiveMode", false);
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefThrottleViewImmersiveModeHideToolbar", false);
                }

                if (mainapp.connectedHostName.equals("")) { // option is only available when there is no current connection
                    parentActivity.getConnectionsList();
                    ListPreference preference = (ListPreference) findPreference("prefHostImportExport");
                    if (preference != null) {
                        preference.setEntries(parentActivity.prefHostImportExportEntriesFound);
                        preference.setEntryValues(parentActivity.prefHostImportExportEntryValuesFound);
                    }
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefAllowMobileData", true);
                } else {
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefAllowMobileData", false);
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefHostImportExport", false);
                }

                if (mainapp.androidVersion < mainapp.minActivatedButtonsVersion) {
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefSelectedLocoIndicator", false);
                }

                if ((mainapp.connectedHostip == null) || (mainapp.web_server_port == 0)) {
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefImportServerManual", false);
                }
            }


            if (prefs != null) {
                prefThemeOriginal = prefs.getString("prefTheme",
                        parentActivity.getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));
                if (mainapp.androidVersion < mainapp.minThemeVersion) {
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefTheme", false);
                }

                parentActivity.result = RESULT_OK;

                parentActivity.deviceId = Settings.System.getString(parentActivity.getContentResolver(), Settings.Secure.ANDROID_ID);
                prefs.edit().putString("prefAndroidId", parentActivity.deviceId).commit();

                // - - - - - - - - - - - -

                prefs.edit().putBoolean("prefForcedRestart", false).commit();
                prefs.edit().putInt("prefForcedRestartReason", mainapp.FORCED_RESTART_REASON_NONE).commit();
                prefs.edit().putString("prefPreferencesImportAll", PREF_IMPORT_ALL_RESET).commit();

                advancedPreferences = getResources().getStringArray(R.array.advancedPreferences);
                hideAdvancedPreferences();
            }

        }

        @SuppressLint("ApplySharedPref")
        static void putObject(SharedPreferences sharedPreferences, final String key, final Object val) {
            if (val instanceof Boolean)
                sharedPreferences.edit().putBoolean(key, (Boolean) val).commit();
            else if (val instanceof Float)
                sharedPreferences.edit().putFloat(key, (Float) val).commit();
            else if (val instanceof Integer)
                sharedPreferences.edit().putInt(key, (Integer) val).commit();
            else if (val instanceof Long)
                sharedPreferences.edit().putLong(key, (Long) val).commit();
            else if (val instanceof String)
                sharedPreferences.edit().putString(key, ((String) val)).commit();
        }

        public void removePreference(Preference preference) {
            try {
                PreferenceGroup parent = getParent(getPreferenceScreen(), preference);
                if (parent != null)
                    parent.removePreference(preference);
                else //Doesn't have a parent
                    getPreferenceScreen().removePreference(preference);
            } catch (Exception except) {
                Log.d("EX_Toolbox", "Settings: removePreference: failed: " + preference);
                return;
            }
        }

        private void hideAdvancedPreferences() {
            if (!prefs.getBoolean("prefShowAdvancedPreferences", parentActivity.getApplicationContext().getResources().getBoolean(R.bool.prefShowAdvancedPreferencesDefaultValue) ) ) {
                for (String advancedPreference1 : advancedPreferences) {
// //                Log.d("EX_Toolbox", "Settings: hideAdvancedPreferences(): " + advancedPreference1);
                    Preference advancedPreference = (Preference) findPreference(advancedPreference1);
                    if (advancedPreference != null) {
                        removePreference(advancedPreference);
                    } else {
                        Log.d("EX_Toolbox", "Settings: '" + advancedPreference1 + "' not found.");
                    }
                }
            }
        }

        private PreferenceGroup getParent(PreferenceGroup groupToSearchIn, Preference preference) {
            for (int i = 0; i < groupToSearchIn.getPreferenceCount(); ++i) {
                Preference child = groupToSearchIn.getPreference(i);

                if (child == preference)
                    return groupToSearchIn;

                if (child instanceof PreferenceGroup) {
                    PreferenceGroup childGroup = (PreferenceGroup)child;
                    PreferenceGroup result = getParent(childGroup, preference);
                    if (result != null)
                        return result;
                }
            }

            return null;
        }

    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    public static class SettingsSubScreenFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        private static final String TAG = SettingsSubScreenFragment.class.getName();
        public static final String PAGE_ID = "page_id";
        SettingsActivity parentActivity;
        public String[] advancedSubPreferences;

        public static SettingsSubScreenFragment newInstance(String pageId) {
            SettingsSubScreenFragment f = new SettingsSubScreenFragment();
            Bundle args = new Bundle();
            args.putString(PAGE_ID, pageId);
            f.setArguments(args);

            return (f);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            // rootKey is the name of preference sub screen key name , here--customPrefKey
            setPreferencesFromResource(R.xml.preferences, rootKey);
            Log.d(TAG, "onCreatePreferences of the sub screen " + rootKey);

            Activity a = getActivity();
            parentActivity = (SettingsActivity) a;
            if (parentActivity.prefs==null) {
                parentActivity.loadSharedPreferences();
            }

            parentActivity.isInSubScreen = true;

            parentActivity.prefBackgroundImage = parentActivity.prefs.getBoolean("prefBackgroundImage", false);
            parentActivity.showHideBackgroundImagePreferences(getPreferenceScreen());

            // option is only available when there is no current connection

            if (parentActivity.mainapp != null) {
                if (parentActivity.mainapp.connectedHostName.equals("")) {
                    parentActivity.getConnectionsList();
                    ListPreference preference = (ListPreference) findPreference("prefHostImportExport");
                    if (preference != null) {
                        preference.setEntries(parentActivity.prefHostImportExportEntriesFound);
                        preference.setEntryValues(parentActivity.prefHostImportExportEntryValuesFound);
                    }
                } else {
                    parentActivity.enableDisablePreference(getPreferenceScreen(), "prefHostImportExport", false);
                }
            }

            advancedSubPreferences = getResources().getStringArray(R.array.advancedSubPreferences);
            hideAdvancedSubPreferences();
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            // Set the default white background in the view so as to avoid transparency
//            view.setBackgroundColor(
//                    ContextCompat.getColor(getContext(), R.color.background_material_light));

        }

        @Override
        public void onResume() {
            Log.d("EX_Toolbox", "Settings: SettingsFragment onResume()");
            super.onResume();

            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            super.onPause();

            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference.getKey().equals("prefBackgroundImageFileNameImagePicker")) {
                parentActivity.loadImagefromGallery();
                return true;
            } else {
                super.onPreferenceTreeClick(preference);
            }
            return false;
        }

        private PreferenceGroup getParent(PreferenceGroup groupToSearchIn, Preference preference) {
            for (int i = 0; i < groupToSearchIn.getPreferenceCount(); ++i) {
                Preference child = groupToSearchIn.getPreference(i);

                if (child == preference)
                    return groupToSearchIn;

                if (child instanceof PreferenceGroup) {
                    PreferenceGroup childGroup = (PreferenceGroup)child;
                    PreferenceGroup result = getParent(childGroup, preference);
                    if (result != null)
                        return result;
                }
            }
            return null;
        }

        public void removeSubPreference(Preference preference) {
            try {
                PreferenceGroup parent = getParent(getPreferenceScreen(), preference);
                if (parent != null)
                    parent.removePreference(preference);
                else //Doesn't have a parent
                    getPreferenceScreen().removePreference(preference);
            } catch (Exception except) {
                Log.d("EX_Toolbox", "Settings: removeSubPreference: failed: " + preference);
                return;
            }
        }

        private void hideAdvancedSubPreferences() {
            if (!parentActivity.prefs.getBoolean("prefShowAdvancedPreferences", parentActivity.getApplicationContext().getResources().getBoolean(R.bool.prefShowAdvancedPreferencesDefaultValue) ) ) {
                for (String advancedSubPreference1 : advancedSubPreferences) {
// //                Log.d("EX_Toolbox", "Settings: hideAdvancedPreferences(): " + advancedPreference1);
                    Preference advancedSubPreference = (Preference) findPreference(advancedSubPreference1);
                    if (advancedSubPreference != null) {
                        removeSubPreference(advancedSubPreference);
                    } else {
                        Log.d("EX_Toolbox", "Settings: '" + advancedSubPreference1 + "' not found.");
                    }
                }
            }
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String key) {
            boolean prefForcedRestart = sharedPreferences.getBoolean("prefForcedRestart", false);

            if ((key != null) && !prefForcedRestart) {  // don't do anything if the preference have been loaded and we are about to reload the app.
                switch (key) {
                    case "prefImportExport":
                            parentActivity.exportedPreferencesFileName = "exported_preferences.ed";
                            String currentValue = sharedPreferences.getString(key, "");
                            if (currentValue.equals(IMPORT_EXPORT_OPTION_RESET)) {
                                parentActivity.resetPreferencesDialog();
                            }
                        break;
                    case "prefBackgroundImage":
                        parentActivity.prefBackgroundImage = sharedPreferences.getBoolean("prefBackgroundImage", false);
                        parentActivity.showHideBackgroundImagePreferences(getPreferenceScreen());
                    case "prefBackgroundImageFileName":
                    case "prefBackgroundImagePosition":
                        parentActivity.forceRestartAppOnPreferencesClose = true;
                        parentActivity.forceRestartAppOnPreferencesCloseReason = parentActivity.mainapp.FORCED_RESTART_REASON_BACKGROUND;
                        break;

                    case "prefAccelerometerShakeThreshold":
                        parentActivity.limitFloatPrefValue(getPreferenceScreen(), sharedPreferences, key, 1.2F, 3.0F, "2.0"); // limit check new value
                        parentActivity.forceRestartAppOnPreferencesCloseReason = parentActivity.mainapp.FORCED_RESTART_REASON_SHAKE_THRESHOLD;
                        parentActivity.forceRestartAppOnPreferencesClose = true;
                        break;

                    case "prefShowTimeOnLogEntry":
                        parentActivity.mainapp.prefShowTimeOnLogEntry = sharedPreferences.getBoolean("prefShowTimeOnLogEntry",
                                getResources().getBoolean(R.bool.prefShowTimeOnLogEntryDefaultValue));
                        break;

                }
            }
        }
    }
}