package com.anthonykeane.speedzone;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by Keanea on 25/06/13.
 */
class PrefsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //addPreferencesFromResource(R.xml.preferences_headers);
        addPreferencesFromResource(R.xml.preferences);
    }
}