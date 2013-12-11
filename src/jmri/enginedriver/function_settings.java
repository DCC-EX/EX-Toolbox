/* Copyright (C) 2013 M. Steve Todd mstevetodd@enginedriver.rrclubs.org

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

package jmri.enginedriver;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import java.util.ArrayList;
import java.io.*;

import jmri.enginedriver.logviewer.ui.LogViewerActivity;

import android.text.method.TextKeyListener;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class function_settings extends Activity{

	private threaded_application mainapp;
	private boolean orientationChange = false;

	//set up label, dcc function, toggle setting for each button
	private static boolean settingsCurrent = false;
	private static ArrayList<String> aLbl = new ArrayList<String>();
	private static ArrayList<Integer> aFnc = new ArrayList<Integer>();
	private Menu FMenu;


	public void setTitleToIncludeThrotName()
	{
		SharedPreferences prefs  = getSharedPreferences("jmri.enginedriver_preferences", 0);
		String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
		setTitle(getApplicationContext().getResources().getString(R.string.app_name_functions) + "    |    Throttle Name: " + 
				prefs.getString("throttle_name_preference", defaultName));
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainapp=(threaded_application)getApplication();  //save pointer to main app

		//setTitleToIncludeThrotName();

		setContentView(R.layout.function_settings);
		orientationChange = false;

		if(savedInstanceState == null) {	//if not an orientation change then init settings array
			initSettings();
			settingsCurrent = true;
		}
		move_settings_to_view();			//copy settings array to view

		// suppress popup keyboard until EditText is touched
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		Button b=(Button)findViewById(R.id.fb_copy_labels_from_roster);
		if (mainapp.function_labels_T == null || mainapp.function_labels_T.size()==0) {
			b.setEnabled(false);  //disable button if no roster
		} 
		else { 
			//Set the button callback.
			button_listener click_listener=new button_listener();
			b.setOnClickListener(click_listener);
			b.setEnabled(true);
		}

		if(!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			//warn user that saving Default Function Settings requires SD Card
			TextView v=(TextView)findViewById(R.id.fs_heading);
			v.setText(getString(R.string.fs_edit_notice));
		}

	}

	@Override
	public void onResume() {
		super.onResume();
		mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
		mainapp.cancelRunningNotify();
		if(FMenu != null)
		{
			mainapp.displayEStop(FMenu);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle saveState) {		//orientation change
		move_view_to_settings();		//update settings array so onCreate can use it to initialize
		orientationChange = true;
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if(this.isFinishing()) {		//if finishing, expedite it and don't invoke setContentIntentNotification
			return;
		}
		mainapp.setContentIntentNotification(this.getIntent());
	}

	@Override
	public void onDestroy() {
		Log.d("Engine_Driver","function_settings.onDestroy() called");
		if(!orientationChange)
		{
			aLbl.clear();
			aFnc.clear();
		}
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.function_settings_menu, menu);
		FMenu = menu;
		mainapp.displayEStop(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		Intent in;
		switch (item.getItemId()) {
		case R.id.throttle_mnu:
			this.finish();
			connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
			break;
		case R.id.turnouts_mnu:
			this.finish();
			in=new Intent().setClass(this, turnouts.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
			break;
		case R.id.routes_mnu:
			in = new Intent().setClass(this, routes.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
			break;
		case R.id.web_mnu:
			this.finish();
			in=new Intent().setClass(this, web_activity.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.preferences_mnu:
			this.finish();
			in=new Intent().setClass(this, preferences.class);
			startActivityForResult(in, 0);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.power_control_mnu:
			this.finish();
			in=new Intent().setClass(this, power_control.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.about_mnu:
			this.finish();
			in=new Intent().setClass(this, about_page.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.logviewer_menu:
			this.finish();
			Intent logviewer=new Intent().setClass(this, LogViewerActivity.class);
			startActivity(logviewer);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.EmerStop:
			mainapp.sendEStopMsg();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	//build the arrays from the function_settings file
	//function_labels_default was loaded from settings file by TA
	//(and updated by saveSettings() when required) so just copy it
	void initSettings() {
		aLbl.clear();
		aFnc.clear();
		//read settings into local arrays
		for (Integer f : mainapp.function_labels_default.keySet()) {
			aFnc.add(f);
			aLbl.add(mainapp.function_labels_default.get(f));
		}
	}

	//take data from arrays and update the editing view
	void move_settings_to_view() {
		ViewGroup t = (ViewGroup) findViewById(R.id.label_func_table); //table
		//loop thru input rows, skipping first (headings)
		int ndx = 0;
		for(int i = 1; i < t.getChildCount(); i++) {
			ViewGroup r = (ViewGroup)t.getChildAt(i);
			//move to next non-blank array entry if it exists
			while(ndx < aFnc.size() && aLbl.get(ndx).length() == 0)
				ndx++;
			if(ndx < aFnc.size()) {
				((EditText)r.getChildAt(0)).setText(aLbl.get(ndx));
				((EditText)r.getChildAt(1)).setText(aFnc.get(ndx).toString());
				ndx++;
			}
			else {
				//			
				// work around for known EditText bug - see http://code.google.com/p/android/issues/detail?id=17508
				//			((EditText)r.getChildAt(0)).setText("");
				//			((EditText)r.getChildAt(1)).setText("");
				TextKeyListener.clear(((EditText)r.getChildAt(0)).getText());
				TextKeyListener.clear(((EditText)r.getChildAt(1)).getText());
			}
		}
	}

	//Save the valid function labels in the settings array
	void move_view_to_settings() {
		ViewGroup t = (ViewGroup) findViewById(R.id.label_func_table); //table
		ViewGroup r;  //row
		//loop thru each row, Skipping the first one (the headings)  format is "label:function#"
		int ndx = 0;
		for(int i = 1; i < t.getChildCount(); i++) {
			r = (ViewGroup)t.getChildAt(i);
			//get the 2 inputs from each row
			String label = ((EditText)r.getChildAt(0)).getText().toString();
			label = label.replace("\n", " ");  //remove newlines
			label = label.replace(":", " ");   //   and colons, as they confuse the save format
			String sfunc = ((EditText)r.getChildAt(1)).getText().toString();
			if(label.length() > 0 && sfunc.length() > 0) {
				//verify function is valid number between 0 and 28
				int func;
				try {
					func = Integer.parseInt(sfunc);
					if(func >= 0 && func <= 28) {
						if(aFnc.size() <= ndx) {
							aLbl.add(label);
							aFnc.add(func);
							settingsCurrent = false;
						}
						else if(!label.equals(aLbl.get(ndx)) || func != aFnc.get(ndx)) {
							aLbl.set(ndx, label);
							aFnc.set(ndx, func);
							settingsCurrent = false;
						}
						ndx++;
					}
				} 
				catch (Exception e) {
				}
			}
		}

		while(aFnc.size() > ndx) {			//if array remains then trim it
			aFnc.remove(ndx);
			aLbl.remove(ndx);
			settingsCurrent = false;
		}
	}

	//replace arrays using data from roster entry (called by button)
	void move_roster_to_settings() {
		int ndx = 0;
		for (Integer func : mainapp.function_labels_T.keySet()) {
			String label = mainapp.function_labels_T.get(func);
			if(label.length() > 0 && func >= 0 && func <= 28) {
				if(aFnc.size() <= ndx) {
					aLbl.add(label);
					aFnc.add(func);
					settingsCurrent = false;
				}
				else if(!label.equals(aLbl.get(ndx)) || func != aFnc.get(ndx)) {
					aLbl.set(ndx, label);
					aFnc.set(ndx, func);
					settingsCurrent = false;
				}
				ndx++;
			}
		}

		while(aFnc.size() > ndx) {			//if array remains then trim it
			aFnc.remove(ndx);
			aLbl.remove(ndx);
			settingsCurrent = false;
		}
	}

	public class button_listener implements View.OnClickListener
	{
		public void onClick(View v)
		{
			move_roster_to_settings();
			move_settings_to_view();
		};
	}

	//Handle pressing of the back button to save settings
	@Override
	public boolean onKeyDown(int key, KeyEvent event)
	{
		if(key==KeyEvent.KEYCODE_BACK) {
			move_view_to_settings();		//sync settings array to view
			if(!settingsCurrent)			//if settings array is not current
				saveSettings();			//save function labels to file
			this.finish();  //end this activity
			/*          connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			 */
		}
		return(super.onKeyDown(key, event));
	};

	//save function and labels to file
	void saveSettings() {
		//SD Card required to save settings
		if(!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
			return;
		//Save the valid function labels to the settings.txt file.
		File sdcard_path=Environment.getExternalStorageDirectory();
		File settings_file=new File(sdcard_path, "engine_driver/function_settings.txt");
		PrintWriter settings_output;
		String errMsg = "";
		try {
			settings_output=new PrintWriter(settings_file);
			mainapp.function_labels_default.clear();
			for(int i = 0; i < aFnc.size(); i++) {
				String label = aLbl.get(i);
				if(label.length() > 0) {
					Integer fnc = aFnc.get(i);
					settings_output.format("%s:%s\n", label, fnc);
					mainapp.function_labels_default.put(fnc, label);
				}
			}
			settings_output.flush();
			settings_output.close();
		}
		catch(IOException except) {
			errMsg = except.getMessage();
			Log.e("settings_activity", "Error creating a PrintWriter, IOException: "+errMsg);
		}
		if(errMsg.length() != 0)
			Toast.makeText(getApplicationContext(), "Save Settings Failed." +errMsg, Toast.LENGTH_LONG).show();
		else
			Toast.makeText(getApplicationContext(), "Settings Saved.", Toast.LENGTH_SHORT).show();
	}
}
