package com.laic.ashman.app;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;

/**
 * Created by duduba on 14-5-23.
 */
public class SettingActivity extends PreferenceActivity {
    private CheckBoxPreference mCheckLocalMap;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

    }

//    private void initPreferences() {
//        mCheckLocalMap = (CheckBoxPreference)findPreference(Consts.CHECKOUT_KEY);
//    }
}