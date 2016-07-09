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
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import org.miamplayer.miamplayerremote.R;
import org.miamplayer.miamplayerremote.SharedPreferencesKeys;
import org.miamplayer.miamplayerremote.backend.MiamPlayer;

public class PreferencesBehaviorPlayer extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private ListPreference mCallVolume;

    private ListPreference mVolumeInc;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_player);

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());

        mCallVolume = (ListPreference) getPreferenceScreen()
                .findPreference(SharedPreferencesKeys.SP_CALL_VOLUME);
        String currentCallVolume = sharedPreferences
                .getString(SharedPreferencesKeys.SP_CALL_VOLUME, "20");
        updateCallVolumeSummary(currentCallVolume);

        mVolumeInc = (ListPreference) getPreferenceScreen()
                .findPreference(SharedPreferencesKeys.SP_VOLUME_INC);
        String currentVolumeInc = sharedPreferences
                .getString(SharedPreferencesKeys.SP_VOLUME_INC, MiamPlayer.DefaultVolumeInc);
        mVolumeInc.setSummary(
                getString(R.string.pref_volume_inc_summary).replace("%s", currentVolumeInc));
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
        if (key.equals(SharedPreferencesKeys.SP_CALL_VOLUME)) {
            String currentCallVolume = sharedPreferences
                    .getString(SharedPreferencesKeys.SP_CALL_VOLUME, "20");
            updateCallVolumeSummary(currentCallVolume);
        } else if (key.equals(SharedPreferencesKeys.SP_VOLUME_INC)) {
            String currentVolumeInc = sharedPreferences
                    .getString(SharedPreferencesKeys.SP_VOLUME_INC,
                            MiamPlayer.DefaultVolumeInc);
            mVolumeInc.setSummary(
                    getString(R.string.pref_volume_inc_summary).replace("%s", currentVolumeInc));
        }
    }

    private void updateCallVolumeSummary(String currentCallVolume) {
        if (currentCallVolume.equals("-1")) {
            mCallVolume.setSummary(
                    getString(R.string.pref_call_volume_summary)
                            .replace("%s", getString(R.string.tasker_pause)));
        } else {
            mCallVolume.setSummary(
                    getString(R.string.pref_call_volume_summary)
                            .replace("%s", currentCallVolume));
        }
    }
}