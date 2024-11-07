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

Derived from the samples for AppIntro at https://github.com/paolorotolo/AppIntro

*/

package dcc_ex.ex_toolbox.intro;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.appintro.AppIntro2;
import com.github.appintro.AppIntroFragment;
import com.github.appintro.model.SliderPage;

import dcc_ex.ex_toolbox.R;
import dcc_ex.ex_toolbox.threaded_application;
import dcc_ex.ex_toolbox.util.PermissionsHelper;

public class intro_activity extends AppIntro2 {
    private boolean introComplete = false;
    private SharedPreferences prefs;
    private String prefTheme  = "";
    private String originalPrefTheme  = "";

    private threaded_application mainapp;    //pointer back to application

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("EX_Toolbox", "intro_activity.onCreate()");

        mainapp = (threaded_application) this.getApplication();

        mainapp.introIsRunning = true;

        prefs = getSharedPreferences("dcc_ex.ex_toolbox_preferences", 0);
        originalPrefTheme = prefs.getString("prefTheme", getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));

        // Note here that we DO NOT use setContentView();

        SliderPage sliderPage0 = new SliderPage();
        sliderPage0.setTitle(getApplicationContext().getResources().getString(R.string.introWelcomeTitle));
        sliderPage0.setDescription(getApplicationContext().getResources().getString(R.string.introWelcomeSummary));
        sliderPage0.setImageDrawable(R.drawable.intro_welcome);
//        sliderPage0.setBgColor(getResources().getColor(R.color.intro_background));
        sliderPage0.setBackgroundColor(getResources().getColor(R.color.intro_background));
        addSlide(AppIntroFragment.newInstance(sliderPage0));

        int slideNumber = 1;  // how many preceding slides

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.POST_NOTIFICATIONS)) {
                SliderPage sliderPage = new SliderPage();
                sliderPage.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
                sliderPage.setDescription(getApplicationContext().getResources().getString(R.string.permissionsPOST_NOTIFICATIONS));
                sliderPage.setImageDrawable(R.drawable.icon_xl);
//        sliderPage0.setBgColor(getResources().getColor(R.color.intro_background));
                sliderPage0.setBackgroundColor(getResources().getColor(R.color.intro_background));
                addSlide(AppIntroFragment.newInstance(sliderPage));
                slideNumber = slideNumber + 1;
                askForPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, slideNumber);
            }
        }

//<!-- needed for API 33 -->
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
//<!-- needed for API 33 -->
            if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_IMAGES)) {
                SliderPage sliderPage = new SliderPage();
                sliderPage.setTitle(getApplicationContext().getResources().getString(R.string.permissionsReadImages));
                sliderPage.setDescription(getApplicationContext().getResources().getString(R.string.permissionsReadImages));
                sliderPage.setImageDrawable(R.drawable.icon_xl);
//        sliderPage0.setBgColor(getResources().getColor(R.color.intro_background));
                sliderPage0.setBackgroundColor(getResources().getColor(R.color.intro_background));
                addSlide(AppIntroFragment.newInstance(sliderPage));
                slideNumber = slideNumber + 1;
                askForPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, slideNumber);
            }
//<!-- needed for API 33 -->
        } else if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_MEDIA_IMAGES)) {
                SliderPage sliderPage = new SliderPage();
                sliderPage.setTitle(getApplicationContext().getResources().getString(R.string.permissionsREAD_MEDIA_IMAGES));
                sliderPage.setDescription(getApplicationContext().getResources().getString(R.string.permissionsREAD_MEDIA_IMAGES));
                sliderPage.setImageDrawable(R.drawable.icon_xl);
//        sliderPage0.setBgColor(getResources().getColor(R.color.intro_background));
                sliderPage0.setBackgroundColor(getResources().getColor(R.color.intro_background));
                addSlide(AppIntroFragment.newInstance(sliderPage));
                slideNumber = slideNumber + 1;
                askForPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, slideNumber);
            }
        } else { // needed for API 34
            if ( (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_MEDIA_IMAGES))
                    && (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_MEDIA_VISUAL_USER_SELECTED)) ) {

                SliderPage sliderPage = new SliderPage();
                sliderPage.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
                sliderPage.setDescription(getApplicationContext().getResources().getString(R.string.permissionsREAD_MEDIA_VISUAL_USER_SELECTED));
                sliderPage.setImageDrawable(R.drawable.icon_vector);
//        sliderPage0.setBgColor(getResources().getColor(R.color.intro_background));
                sliderPage0.setBackgroundColor(getResources().getColor(R.color.intro_background));
                addSlide(AppIntroFragment.newInstance(sliderPage));
                slideNumber = slideNumber + 1;
                askForPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED}, slideNumber);
            }
        }
//<!-- needed for API 34 -->


        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.ACCESS_FINE_LOCATION )) {
                SliderPage sliderPage = new SliderPage();
                sliderPage.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
                sliderPage.setDescription(getApplicationContext().getResources().getString(R.string.permissionsACCESS_FINE_LOCATION));
                sliderPage.setImageDrawable(R.drawable.icon_xl);
//        sliderPage0.setBgColor(getResources().getColor(R.color.intro_background));
                sliderPage0.setBackgroundColor(getResources().getColor(R.color.intro_background));
                addSlide(AppIntroFragment.newInstance(sliderPage));
                slideNumber = slideNumber + 1;
                askForPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, slideNumber);
            }
        } else {
            if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.NEARBY_WIFI_DEVICES)) {
                SliderPage sliderPage = new SliderPage();
                sliderPage.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
                sliderPage.setDescription(getApplicationContext().getResources().getString(R.string.permissionsNEARBY_WIFI_DEVICES));
                sliderPage.setImageDrawable(R.drawable.icon_xl);
//        sliderPage0.setBgColor(getResources().getColor(R.color.intro_background));
                sliderPage0.setBackgroundColor(getResources().getColor(R.color.intro_background));
                addSlide(AppIntroFragment.newInstance(sliderPage));
                slideNumber = slideNumber + 1;
                askForPermissions(new String[]{Manifest.permission.NEARBY_WIFI_DEVICES}, slideNumber);
            }
        }

//        if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.WRITE_SETTINGS)) {
//            if (android.os.Build.VERSION.SDK_INT >= 23) {
//                Fragment fragment3 = new intro_write_settings();
//                addSlide(fragment3);
//            }
//        }

        Fragment fragment0 = new intro_theme();
        addSlide(fragment0);

        Fragment fragment99 = new intro_finish();
        addSlide(fragment99);

        // OPTIONAL METHODS
        // Override bar/separator color.
        setBarColor(getResources().getColor(R.color.intro_buttonbar_background));

        // Hide Skip/Done button.
//        showSkipButton(false);
//        setProgressButtonEnabled(true);

        // Turn vibration on and set intensity.
        // NOTE: you will probably need to ask VIBRATE permission in Manifest.
        setVibrate(false);
        //setVibrateIntensity(30);
        setWizardMode(true);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        introComplete = true;
        SharedPreferences prefsNoBackup = mainapp.getSharedPreferences("dcc_ex.ex_toolbox_preferences_no_backup", 0);
        prefsNoBackup.edit().putString("prefRunIntro", threaded_application.INTRO_VERSION).commit();

        prefTheme = prefs.getString("prefTheme", getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));

        if (!prefTheme.equals(originalPrefTheme))  {

            // the theme has changed so need to restart the app.
            Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
//            Runtime.getRuntime().exit(0); // really force the kill

        }
        mainapp.introIsRunning = false;
        this.finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }

    @Override
    public void onResume() {
        super.onResume();
//        if (this.isFinishing()) {        //if finishing, expedite it
//            return;
//        }
    }


    @Override
    public void onDestroy() {
        Log.d("EX_Toolbox", "intro_activity.onDestroy() called");

        mainapp.introIsRunning = false;
        if (!introComplete) {
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.introbackButtonPress), Toast.LENGTH_LONG).show();
        }
        super.onDestroy();
    }
}

