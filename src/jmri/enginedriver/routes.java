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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class routes extends Activity  implements OnGestureListener {

	private threaded_application mainapp;  // hold pointer to mainapp

	private SharedPreferences prefs;

	private ArrayList<HashMap<String, String>> routesFullList;
	private ArrayList<HashMap<String, String> > routes_list;
	private SimpleAdapter routes_list_adapter;
	private ArrayList<String> locationList;
	private ArrayAdapter<String> locationListAdapter;
	private static String location = null;
	private Spinner locationSpinner;

	private GestureDetector myGesture ;
	private Menu RMenu;

	public class route_item implements AdapterView.OnItemClickListener	  {

		//When a route  is clicked, extract systemname and send command to toggle it
		public void onItemClick(AdapterView<?> parent, View v, int position, long id)	    {
			ViewGroup vg = (ViewGroup)v; //convert to viewgroup for clicked row
			ViewGroup rl = (ViewGroup) vg.getChildAt(0);  //get relativelayout
			TextView snv = (TextView) rl.getChildAt(1); // get systemname text from 2nd box
			String systemname = (String) snv.getText();
			mainapp.sendMsg(mainapp.comm_msg_handler, message_type.ROUTE, '2'+systemname);	// toggle
		};
	}	  

	public void refresh_route_view() {

		boolean hidesystemroutes = prefs.getBoolean("hide_system_route_names_preference", 
				getResources().getBoolean(R.bool.prefHideSystemRouteNamesDefaultValue));

		//specify logic for sort comparison (by username)
		Comparator<HashMap<String, String>> route_comparator = new Comparator<HashMap<String, String>>() {
			@Override
			public int compare(HashMap<String, String> arg0, HashMap<String, String> arg1) {
				return arg0.get("rt_user_name").compareTo(arg1.get("rt_user_name"));	//*** was compareToIgnoreCase
			}
		};

		//clear and rebuild
		routesFullList.clear();
		locationList.clear();
		if (mainapp.rt_state_names != null) {  //not allowed
			if (mainapp.rt_user_names != null) { //none defined
				int pos = 0;
				String del = prefs.getString("DelimiterPreference", getApplicationContext().getResources().getString(R.string.prefDelimiterDefaultValue));
				for (String username : mainapp.rt_user_names) {
					if (!username.equals(""))  {  //skip routes without usernames
						//get values from global array
						String systemname = mainapp.rt_system_names[pos];
						String currentstate = mainapp.rt_states[pos];
						String currentstatedesc = mainapp.rt_state_names.get(currentstate);
						if (currentstatedesc == null) {
							currentstatedesc = "   ???";
						}

						//put values into temp hashmap
						HashMap<String, String> hm=new HashMap<String, String>();
						hm.put("rt_user_name", username);
						hm.put("rt_system_name_hidden", systemname);
						if (!hidesystemroutes) {  //check prefs for show or not show this
							hm.put("rt_system_name", systemname);
						}
						hm.put("rt_current_state_desc", currentstatedesc);
						routesFullList.add(hm);

						//if location is new, add to list
						if(del.length() > 0) {
							int delim = username.indexOf(del);
							if(delim >= 0) {
								String loc = username.substring(0, delim);
								if(!locationList.contains(loc))
									locationList.add(loc);
							}
						}
						//
					}
					pos++;
				}
				//				routes_list_adapter.notifyDataSetChanged();
			}
		}
		updateRouteEntry();

		//sort lists by username
		Collections.sort(routesFullList, route_comparator);
		Collections.sort(locationList);
		locationList.add(0,getString(R.string.location_all));	// this entry goes at the top of the list
		locationListAdapter.notifyDataSetChanged();
		if(!locationList.contains(location))
			location = getString(R.string.location_all);
		locationSpinner.setSelection(locationListAdapter.getPosition(location));

		filterRouteView();
	}

	private void filterRouteView() {
		final String loc = location + prefs.getString("DelimiterPreference", getApplicationContext().getResources().getString(R.string.prefDelimiterDefaultValue));
		final boolean useAllLocations = getString(R.string.location_all).equals(location);
		routes_list.clear();
		for(HashMap<String, String> hm : routesFullList) {
			String userName = hm.get("rt_user_name");
			if(useAllLocations || userName.startsWith(loc)) {
				@SuppressWarnings("unchecked")
				HashMap<String, String> hmFilt = (HashMap<String, String>) hm.clone(); 
				if(!useAllLocations)
					hmFilt.put("rt_user_name", userName.substring(loc.length()));
				routes_list.add(hmFilt);
			}
		}
		routes_list_adapter.notifyDataSetChanged();  //update the list
	}

	private int updateRouteEntry() {
		Button butSet = (Button) findViewById(R.id.route_toggle);
		EditText rte = (EditText) findViewById(R.id.route_entry);
		TextView rtePrefix =(TextView)findViewById(R.id.route_prefix);
		String route = rte.getText().toString().trim();
		int txtLen = route.length();
		if (mainapp.rt_state_names != null) {
			rte.setEnabled(true);
			butSet.setText(getString(R.string.set));
			// don't allow Set button if nothing entered
			if(txtLen > 0) {
				butSet.setEnabled(true);
				if(Character.isDigit(route.charAt(0))) // show default route prefix if numeric entry
					rtePrefix.setEnabled(true);
				else
					rtePrefix.setEnabled(false);
			}
			else {
				butSet.setEnabled(false);
				rtePrefix.setEnabled(false);
			}
		} 
		else {
			rte.setEnabled(false);
			butSet.setEnabled(false);
			if(!rte.getText().toString().equals(getString(R.string.disabled)))
				rte.setText(getString(R.string.disabled));
			rtePrefix.setEnabled(false);
		}

		if(RMenu != null)
		{
			mainapp.displayEStop(RMenu);
		}

		return txtLen;
	}

	//Handle messages from the communication thread back to this thread (responses from withrottle)
	@SuppressLint("HandlerLeak")
	class routes_handler extends Handler {

		public void handleMessage(Message msg) {
			switch(msg.what) {
			case message_type.RESPONSE: {
				String response_str = msg.obj.toString();

				if (response_str.length() >= 3) {
					String com1 = response_str.substring(0,3);
					//refresh routes if any have changed state or if route list changed
					if ("PRA".equals(com1) || "PRL".equals(com1)) {
						refresh_route_view();
					}
					//update power icon
					if ("PPA".equals(com1)) {
						mainapp.setPowerStateButton(RMenu);
					}
				}
			}
			break;
			case message_type.LOCATION_DELIMITER:
				refresh_route_view();
				break;
			case message_type.WIT_CON_RETRY:
				refresh_route_view(); 
				break;
			case message_type.DISCONNECT:
			case message_type.SHUTDOWN:
				disconnect();
				break;
			};
		}
	}

	public class button_listener implements View.OnClickListener  {
		char whichCommand; //command to send for button instance 'C'lose, 'T'hrow or '2' for toggle

		public button_listener(char new_command) {
			whichCommand = new_command;
		}

		public void onClick(View v) {
			EditText entryv=(EditText)findViewById(R.id.route_entry);
			String entrytext = new String(entryv.getText().toString().trim());
			if (entrytext.length() > 0 ) {
				//if text starts with a digit then use default prefix
				//otherwise send the text as is
				if(Character.isDigit(entrytext.charAt(0)))
					entrytext = getString(R.string.routes_default_prefix) + entrytext;
				mainapp.sendMsg(mainapp.comm_msg_handler, message_type.ROUTE, whichCommand+entrytext);
			}
		};
	}

	@Override
	public boolean onTouchEvent(MotionEvent event){
		return myGesture.onTouchEvent(event);
	}


	public void setTitleToIncludeThrotName()
	{
		String defaultName = getApplicationContext().getResources().getString(R.string.prefThrottleNameDefaultValue);
		setTitle(getApplicationContext().getResources().getString(R.string.app_name_routes) + "    |    Throttle Name: " + 
				prefs.getString("throttle_name_preference", defaultName));
	}

	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState)  {
		super.onCreate(savedInstanceState);

		mainapp=(threaded_application)getApplication();
		prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
		if(mainapp.isForcingFinish()) {		// expedite
			return;
		}

//		setTitleToIncludeThrotName();

		setContentView(R.layout.routes);
		//put pointer to this activity's handler in main app's shared variable
		mainapp.routes_msg_handler=new routes_handler();
		myGesture = new GestureDetector(this);

		routesFullList=new ArrayList<HashMap<String, String> >();
		//Set up a list adapter to allow adding the list of recent connections to the UI.
		routes_list=new ArrayList<HashMap<String, String> >();
		routes_list_adapter=new SimpleAdapter(this, routes_list, R.layout.routes_item, 
				new String[] {"rt_user_name", "rt_system_name_hidden", "rt_system_name", "rt_current_state_desc"},
				new int[] {R.id.rt_user_name, R.id.rt_system_name_hidden, R.id.rt_system_name, R.id.rt_current_state_desc});
		ListView routes_lv=(ListView)findViewById(R.id.routes_list);
		routes_lv.setAdapter(routes_list_adapter);
		routes_lv.setOnItemClickListener(new route_item());

		OnTouchListener gestureListener = new ListView.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (myGesture.onTouchEvent(event)) {
					return true;
				}
				return false;
			}
		};
		routes_lv.setOnTouchListener(gestureListener);

		EditText rte = (EditText)findViewById(R.id.route_entry);
		rte.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				updateRouteEntry();
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});
		rte.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if((actionId & EditorInfo.IME_MASK_ACTION) != 0) {
					InputMethodManager imm = 
						(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					return true;
				}
				else
					return false;
			}
		});

		//Set the button callbacks, storing the command to pass for each
		Button b=(Button)findViewById(R.id.route_toggle);
		button_listener click_listener=new button_listener('2');
		b.setOnClickListener(click_listener);

		((EditText) findViewById(R.id.route_entry)).setRawInputType(InputType.TYPE_CLASS_NUMBER);

		locationList = new ArrayList<String>();
		locationSpinner = (Spinner) findViewById(R.id.routes_location);
		locationListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, locationList);
		locationListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		locationSpinner.setAdapter(locationListAdapter);

		locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				location = (String)parent.getSelectedItem();
				filterRouteView();
			}
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		//update route list
		refresh_route_view();
	};

	@Override
	public void onResume() {
		super.onResume();
		if(mainapp.isForcingFinish()) {		//expedite
			this.finish();
			return;
		}
	
		if(!mainapp.setActivityOrientation(this))  //set screen orientation based on prefs
		{
			Intent in=new Intent().setClass(this, web_activity.class);		// if autoWeb and landscape, switch to Web activity
			startActivity(in);
			this.finish();
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			return;
		}
		mainapp.cancelRunningNotify();
		
//		setTitleToIncludeThrotName();
		if(RMenu != null)
		{
			mainapp.displayEStop(RMenu);
			mainapp.displayPowerStateMenuButton(RMenu);
		}
		updateRouteEntry();	// enable/disable button
		// suppress popup keyboard until EditText is touched
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	/** Called when the activity is finished. */
	@Override
	public void onDestroy() {
		Log.d("Engine_Driver","routes.onDestroy()");
		mainapp.routes_msg_handler = null;
		super.onDestroy();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if(this.isFinishing()) {		//if finishing, expedite it and don't invoke setContentIntentNotification
			return;
		}
		mainapp.setContentIntentNotification(this.getIntent());
//***		this.finish(); //don't keep on return stack
	}

	//Always go to throttle activity if back button pressed
	@Override
	public boolean onKeyDown(int key, KeyEvent event) {
		if(key==KeyEvent.KEYCODE_BACK) {
			this.finish();  //end this activity
			connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
			return true;
		}
		return(super.onKeyDown(key, event));
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if((Math.abs(e2.getX() - e1.getX()) > threaded_application.min_fling_distance) && 
				(Math.abs(velocityX) > threaded_application.min_fling_velocity)) {
			// left to right swipe goes to throttle
			if(e2.getX() > e1.getX()) {
				this.finish();
				connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
			}
			// right to left swipe goes to turnouts
			else {
				Intent in=new Intent().setClass(this, turnouts.class);
				startActivity(in);
				this.finish();
				connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
			}
			return true;
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}
	@Override
	public void onShowPress(MotionEvent e) {
	}
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.routes_menu, menu);
		RMenu = menu;
		mainapp.displayEStop(RMenu);
		mainapp.displayPowerStateMenuButton(menu);
		mainapp.setPowerStateButton(menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		Intent in;
		switch (item.getItemId()) {
		case R.id.throttle_mnu:
			this.finish();
			connection_activity.overridePendingTransition(this, R.anim.push_right_in, R.anim.push_right_out);
			break;
		case R.id.turnouts_mnu:
			in = new Intent().setClass(this, turnouts.class);
			startActivity(in);
			this.finish();
			connection_activity.overridePendingTransition(this, R.anim.push_left_in, R.anim.push_left_out);
			break;
		case R.id.web_mnu:
			in=new Intent().setClass(this, web_activity.class);
			startActivity(in);
			this.finish();
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.exit_mnu:
			mainapp.checkExit(this);
			break;
		case R.id.power_control_mnu:
			in=new Intent().setClass(this, power_control.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.preferences_mnu:
			in=new Intent().setClass(this, preferences.class);
			startActivityForResult(in, 0);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.about_mnu:
			in=new Intent().setClass(this, about_page.class);
			startActivity(in);
			connection_activity.overridePendingTransition(this, R.anim.fade_in, R.anim.fade_out);
			break;
		case R.id.EmerStop:
			mainapp.sendEStopMsg();
			break;
		case R.id.power_layout_button:
			mainapp.powerStateMenuButton();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	//handle return from menu items
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//since we always do the same action no need to distinguish between requests
	}

	private void disconnect() {
		this.finish();
	}

};