package com.talybin.aircat;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }

        /*
        // Remove icon space reservation
        @Override
        public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
            super.setPreferenceScreen(preferenceScreen);
            if (preferenceScreen != null) {
                int i = preferenceScreen.getPreferenceCount() - 1;
                for (; i >= 0; --i)
                    preferenceScreen.getPreference(i).setIconSpaceReserved(false);
            }
        }*/
    }
}