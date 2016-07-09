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

import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import org.miamplayer.miamplayerremote.R;
import org.miamplayer.miamplayerremote.SharedPreferencesKeys;
import org.miamplayer.miamplayerremote.ui.adapter.PreferenceHeaderAdapter;
import org.miamplayer.miamplayerremote.ui.adapter.PreferenceHeaderAdapter.PreferenceHeader;
import org.miamplayer.miamplayerremote.utils.Utilities;

/**
 * The settings screen of MiamPlayer Remote
 */
public class MiamPlayerSettings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Keep screen on if user has requested this in preferences
        if (PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(SharedPreferencesKeys.SP_KEEP_SCREEN_ON, true)
                && Utilities.isRemoteConnected()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        getFragmentManager().beginTransaction()
                .add(R.id.content_frame, new PreferenceListFragment())
                .addToBackStack("List")
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() <= 1) {
            this.finish();
        } else {
            getFragmentManager().popBackStack();
            invalidateOptionsMenu();
            getSupportActionBar().setTitle(R.string.app_name);
        }
    }

    public static class PreferenceListFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_preference_list,
                    container, false);

            final ArrayList<PreferenceHeader> items = new ArrayList<>();
            items.add(new PreferenceHeader(getString(R.string.pref_cat_behavior)));
            items.add(new PreferenceHeader(getString(R.string.pref_cat_player), R.drawable.nav_player, new PreferencesBehaviorPlayer()));
            items.add(new PreferenceHeader(getString(R.string.pref_cat_library), R.drawable.nav_library, new PreferencesBehaviorLibrary()));
            items.add(new PreferenceHeader(getString(R.string.pref_cat_downloads), R.drawable.nav_downloads, new PreferencesBehaviorDownloads()));
            items.add(new PreferenceHeader(getString(R.string.pref_cat_advanced), R.drawable.nav_more_horiz, new PreferencesBehaviorAdvanced()));

            items.add(new PreferenceHeader(getString(R.string.pref_cat_network)));
            items.add(new PreferenceHeader(getString(R.string.pref_cat_connection), R.drawable.nav_antenna, new PreferencesConnection()));

            items.add(new PreferenceHeader(getString(R.string.pref_cat_info)));
            items.add(new PreferenceHeader(getString(R.string.pref_cat_about), R.drawable.nav_info, new PreferencesInformationAbout()));
            items.add(new PreferenceHeader(getString(R.string.pref_cat_license), R.drawable.nav_license, new PreferencesInformationLicenses()));

            PreferenceHeaderAdapter adapter = new PreferenceHeaderAdapter(getActivity(), items);

            ListView listView = (ListView) view.findViewById(android.R.id.list);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Fragment fragment = items.get(position).fragment;
                    getActivity().getFragmentManager().beginTransaction()
                            .replace(R.id.content_frame, fragment)
                            .addToBackStack("Setting")
                            .commit();

                    ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(items.get(position).title);
                }
            });

            return view;
        }
    }
}
