/* This file is part of the Android MiamPlayer Remote.
 * Copyright (C) 2013, Andreas Muttscheller <asfa194@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.miamplayer.miamplayerremote.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.miamplayer.miamplayerremote.R;
import org.miamplayer.miamplayerremote.SharedPreferencesKeys;
import org.miamplayer.miamplayerremote.backend.MiamPlayer;

public class PreferencesConnection extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private EditTextPreference mPortPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_connection);

        // Read the port and fill in the summary
        mPortPreference = (EditTextPreference) getPreferenceScreen()
                .findPreference(SharedPreferencesKeys.SP_KEY_PORT);
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());

        String port = sharedPreferences
                .getString(SharedPreferencesKeys.SP_KEY_PORT,
                        String.valueOf(MiamPlayer.DefaultPort));
        mPortPreference.setSummary(getString(R.string.pref_port_summary) + " " + port);

        mPortPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                EditTextPreference editTextPref = (EditTextPreference) preference;
                editTextPref.getEditText().setSelection(editTextPref.getText().length());
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SharedPreferencesKeys.SP_KEY_PORT)) {
            String port = sharedPreferences
                    .getString(SharedPreferencesKeys.SP_KEY_PORT,
                            String.valueOf(MiamPlayer.DefaultPort));
            int intPort = 0;
            try {
                intPort = Integer.parseInt(port);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            // Check if the port is in a valid range
            if (intPort < 1024 || intPort > 65535) {
                port = String.valueOf(MiamPlayer.DefaultPort);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(SharedPreferencesKeys.SP_KEY_PORT, port);
                editor.apply();

                // Tell the user that he specified an illegal port
                Toast.makeText(getActivity(), getString(R.string.pref_port_error),
                        Toast.LENGTH_LONG).show();
            }
            // Set the summary
            mPortPreference.setSummary(getString(R.string.pref_port_summary) + " " + port);
            mPortPreference.setText(port);
        }
    }
}
