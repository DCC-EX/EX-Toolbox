/*Copyright (C) 2013 M. Steve Todd mstevetodd@enginedriver.rrclubs.org

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

import java.util.Random;
import jmri.enginedriver.logviewer.ui.LogViewerActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private threaded_application mainapp;  // hold pointer to mainapp
	private Menu PRMenu;

	public void setTitleToIncludeThrotName()
	{
//		SharedPreferences prefs  = getSharedPreferences("jmri.enginedriver_preferences", 0);
//		String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
//		setTitle(getApplicationContext().getResources().getString(R.string.app_name_preferences) + "    |    Throttle Name: " + 
//				prefs.getString("throttle_name_preference", defaultName));
	}

	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainapp=(threaded_application)getApplication();
		addPreferencesFromResource(R.xml.preferences);
		setTitleToIncludeThrotName();
		if(mainapp.power_state == null)
		{
			getPreferenceScreen().findPreference("show_layout_power_button_preference").setSelectable(false);
			getPreferenceScreen().findPreference("show_layout_power_button_preference").setEnabled(false);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		mainapp.setActivityOrientation(this);  //set screen orientation based on prefs
		mainapp.cancelRunningNotify();
		// Set up a listener whenever a key changes            
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		setTitleToIncludeThrotName();
		if(PRMenu != null)
		{
			mainapp.displayEStop(PRMenu);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		Log.d("Engine_Driver","preferences.onPause() called");
		super.onPause();

		// Unregister the listener            
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		mainapp.setContentIntentNotification(this.getIntent());
	}

	@Override
	protected void onDestroy() {
		Log.d("Engine_Driver","preferences.onDestroy() called");
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.preferences_menu, menu);
		PRMenu = menu;
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

	@SuppressWarnings("deprecation")
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		threaded_application mainapp=(threaded_application)this.getApplication();
		if (key.equals("throttle_name_preference"))  {
			String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
			String currentValue = sharedPreferences.getString(key, defaultName).trim();
			//if new name is blank or the default name, make it unique
			if (currentValue.equals("") || currentValue.equals(defaultName)) {
				String deviceId = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
				if (deviceId != null && deviceId.length() >=4) {
					deviceId = deviceId.substring(deviceId.length() - 4);
				} else {
					Random rand = new Random();
					deviceId = String.valueOf(rand.nextInt(9999));  //use random string
				}
				String uniqueDefaultName = defaultName + " " + deviceId;
				sharedPreferences.edit().putString(key, uniqueDefaultName).commit();  //save new name to prefs
			}
			setTitleToIncludeThrotName();
		}
		else if(key.equals("WebViewLocation")) {
			mainapp.alert_activities(message_type.WEBVIEW_LOC,""); 
		}
		else if(key.equals("ThrottleOrientation")) {
			//if mode was fixed (Port or Land) won't get callback so need explicit call here 
			mainapp.setActivityOrientation(this);
		}
		else if(key.equals("InitialWebPage")) {
			mainapp.alert_activities(message_type.INITIAL_WEBPAGE,""); 
		}
		else if(key.equals("InitialThrotWebPage")) {
			mainapp.alert_activities(message_type.INITIAL_WEBPAGE,""); 
		}
		else if(key.equals("DelimiterPreference")) {
			mainapp.alert_activities(message_type.LOCATION_DELIMITER,""); 
		}
	}
	//Handle pressing of the back button to end this activity
	@Override
	public boolean onKeyDown(int key, KeyEvent event) {
		if(key==KeyEvent.KEYCODE_BACK)
		{
			this.finish();  //end this activity
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
		}
		return(super.onKeyDown(key, event));
	};
}
