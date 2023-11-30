/* Copyright (C) 2017 M. Steve Todd mstevetodd@gmail.com

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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import dcc_ex.ex_toolbox.type.message_type;
import dcc_ex.ex_toolbox.util.LocaleHelper;

public class power_control extends AppCompatActivity {

    private threaded_application mainapp;  // hold pointer to mainapp
    private Drawable power_on_drawable;  //hold background graphics for power button
    private Drawable power_off_drawable;
    private Drawable power_unknown_drawable;
    private Menu PMenu;

    static final String[] TRACK_TYPES = {"OFF", "MAIN", "PROG", "DC", "DCX"};
    private Button dccExTrackPowerButton[] = {null, null, null, null, null, null, null, null};
    private LinearLayout[] dccExTrackTypeLayout = {null, null, null, null, null, null, null, null};
    private TextView[] dccExTrackType = {null, null, null, null, null, null, null, null};

    float vn = 4; // DCC-EC Version number

    private Toolbar toolbar;

    //Handle messages from the communication thread back to this thread (responses from withrottle)
    @SuppressLint("HandlerLeak")
    class power_control_handler extends Handler {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case message_type.RESPONSE:
                    String response_str = msg.obj.toString();
                    if (response_str.length() >= 3 && response_str.substring(0, 3).equals("PPA")) {  //refresh power state
                        refresh_power_control_view();
                    }
                    if ( (response_str.length() == 5) && ("PXX".equals(response_str.substring(0, 3))) ) {  // individual track power response
                        refreshDCCEXtracksView();
                    }
                    break;
                case message_type.WIT_CON_RETRY:
                    witRetry(msg.obj.toString());
                    break;
                case message_type.WIT_CON_RECONNECT:
                    refresh_power_control_view();
                    break;
                case message_type.RESTART_APP:
                case message_type.RELAUNCH_APP:
                case message_type.DISCONNECT:
                case message_type.SHUTDOWN:
                    disconnect();
                    break;
                case message_type.RECEIVED_TRACKS:
                    refreshDCCEXtracksView();
                    break;
            }
        }
    }

    private void witRetry(String s) {
        Intent in = new Intent().setClass(this, reconnect_status.class);
        in.putExtra("status", s);
        startActivity(in);
        connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
    }

    public class button_listener implements View.OnClickListener {

        public void onClick(View v) {
            int newState = 1;
            if (mainapp.power_state.equals("1")) { //toggle to opposite value 0=off, 1=on
                newState = 0;
            }
            mainapp.sendMsg(mainapp.comm_msg_handler, message_type.POWER_CONTROL, "", newState);
            mainapp.buttonVibration();
        }
    }

    public class SetTrackPowerButtonListener implements View.OnClickListener {
        int myTrack;
        char myTrackLetter;

        public SetTrackPowerButtonListener(int track) {
            myTrack = track;
            myTrackLetter = (char) ('A' + track);
        }

        public void onClick(View v) {
            if (mainapp.DCCEXtrackPower[myTrack] == 0 ) {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK_POWER, ""+myTrackLetter, 1);
            } else {
                mainapp.sendMsg(mainapp.comm_msg_handler, message_type.WRITE_TRACK_POWER, ""+myTrackLetter, 0);
            }
        }
    }

    void setPowerbutton(Button btn, int powerState) {
        TypedValue outValue = new TypedValue();
        if (powerState == 1) {
            mainapp.theme.resolveAttribute(R.attr.ed_power_green_button, outValue, true);
        } else if (powerState == 0) {
            mainapp.theme.resolveAttribute(R.attr.ed_power_red_button, outValue, true);
        } else {
            mainapp.theme.resolveAttribute(R.attr.ed_power_yellow_button, outValue, true);
        }
        Drawable img = getResources().getDrawable(outValue.resourceId);
        btn.setBackground(img);
    }

    public void refreshDCCEXtracksView() {
        for (int i = 0; i < threaded_application.DCCEX_MAX_TRACKS; i++) {
            if (vn >= 05.002005) {  /// need to remove the track power options
                dccExTrackTypeLayout[i].setVisibility(mainapp.DCCEXtrackAvailable[i] ? View.VISIBLE : View.GONE);
                dccExTrackType[i].setText(TRACK_TYPES[mainapp.DCCEXtrackType[i]]);
                setPowerbutton(dccExTrackPowerButton[i],mainapp.DCCEXtrackPower[i]);
            } else {
                dccExTrackTypeLayout[i].setVisibility(View.GONE);
            }
        }
    }

    //Set the button text based on current power state  TODO: improve code 
    public void refresh_power_control_view() {
        Button b = findViewById(R.id.power_control_button);
        Drawable currentImage = power_unknown_drawable;
        if (!mainapp.isPowerControlAllowed()) {
            b.setEnabled(false);
            TextView tv = findViewById(R.id.power_control_text);
            tv.setText(getString(R.string.power_control_not_allowed));
        } else {
            b.setEnabled(true);
            switch (mainapp.power_state) {
                case "1":
                    currentImage = power_on_drawable;
                    break;
                case "2":
                    currentImage = power_unknown_drawable;
                    break;
                default:
                    currentImage = power_off_drawable;
                    break;
            }
        }

        b.setBackgroundDrawable(currentImage);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainapp = (threaded_application) getApplication();
        if (mainapp.isForcingFinish()) {     // expedite
            return;
        }

        mainapp.applyTheme(this);

        setContentView(R.layout.power_control);

        //put pointer to this activity's handler in main app's shared variable (If needed)
        mainapp.power_control_msg_handler = new power_control_handler();

        // request this as early as possible
        if (mainapp.isDCCEX) mainapp.sendMsg(mainapp.comm_msg_handler, message_type.REQUEST_TRACKS, "");

        try {
            vn = Float.valueOf(mainapp.DCCEXversion);
        } catch (Exception e) { } // invalid version

        power_on_drawable = getResources().getDrawable(R.drawable.power_green);
        power_off_drawable = getResources().getDrawable(R.drawable.power_red);
        power_unknown_drawable = getResources().getDrawable(R.drawable.power_yellow);


        //Set the button callbacks, storing the command to pass for each
        Button b = findViewById(R.id.power_control_button);
        button_listener click_listener = new button_listener();
        b.setOnClickListener(click_listener);

        Button closeButton = findViewById(R.id.power_button_close);
        close_button_listener close_click_listener = new close_button_listener();
        closeButton.setOnClickListener(close_click_listener);


        for (int i=0; i < mainapp.DCCEX_MAX_TRACKS; i++) {
            switch (i) {
                default:
                case 0:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DCCEXtrackPower0layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton0);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType0);
                    break;
                case 1:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DCCEXtrackPower1layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton1);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType1);
                    break;
                case 2:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DCCEXtrackPower2layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton2);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType2);
                    break;
                case 3:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DCCEXtrackPower3layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton3);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType3);
                    break;
                case 4:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DCCEXtrackPower4layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton4);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType4);
                    break;
                case 5:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DCCEXtrackPower5layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton5);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType5);
                    break;
                case 6:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DCCEXtrackPower6layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton6);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType6);
                    break;
                case 7:
                    dccExTrackTypeLayout[i] = findViewById(R.id.DCCEXtrackPower7layout);
                    dccExTrackPowerButton[i] = findViewById(R.id.DCCEXpowerControlButton7);
                    dccExTrackType[i] = findViewById(R.id.DCCEXpowerControlTrackType7);
                    break;
            }
            SetTrackPowerButtonListener buttonListener = new SetTrackPowerButtonListener(i);
            dccExTrackPowerButton[i].setOnClickListener(buttonListener);
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            mainapp.setToolbarTitle(toolbar,
                    getApplicationContext().getResources().getString(R.string.app_name),
                    getApplicationContext().getResources().getString(R.string.app_name_power_control),
                    "");
        }

    } // end onCreate

    @Override
    public void onResume() {
        super.onResume();
        if (mainapp.isForcingFinish()) { //expedite
            this.finish();
            return;
        }
        mainapp.setActivityOrientation(this);  //set screen orientation based on prefs

        //update power state
        refresh_power_control_view();
        refreshDCCEXtracksView();
    }

    /**
     * Called when the activity is finished.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mainapp.power_control_msg_handler !=null) {
            mainapp.power_control_msg_handler.removeCallbacksAndMessages(null);
            mainapp.power_control_msg_handler = null;
        } else {
            Log.d("EX_Toolbox", "onDestroy: mainapp.power_control_msg_handler is null. Unable to removeCallbacksAndMessages");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.power_menu, menu);
        PMenu = menu;

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        //noinspection SwitchStatementWithTooFewBranches
        switch (item.getItemId()) {
            case R.id.power_layout_button:
                if (!mainapp.isPowerControlAllowed()) {
                    mainapp.powerControlNotAllowedDialog(PMenu);
                } else {
                    mainapp.powerStateMenuButton();
                }
                mainapp.buttonVibration();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //Handle pressing of the back button to end this activity
    @Override
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_BACK) {
            this.finish();  //end this activity
            connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        return (super.onKeyDown(key, event));
    }

    private void disconnect() {
        this.finish();
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    public class close_button_listener implements View.OnClickListener {
        public void onClick(View v) {
            mainapp.buttonVibration();
            finish();
        }
    }
}
