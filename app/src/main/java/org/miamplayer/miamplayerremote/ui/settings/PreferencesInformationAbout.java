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

import com.afollestad.materialdialogs.MaterialDialog;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.miamplayer.miamplayerremote.R;

public class PreferencesInformationAbout extends PreferenceFragment {

    private Preference mAboutDialogPreference;

    private Preference mVersion;

    private Preference mMiamPlayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_about);

        mAboutDialogPreference = getPreferenceScreen()
                .findPreference("pref_key_about");
        mVersion = getPreferenceScreen().findPreference("pref_version");
        mMiamPlayer = getPreferenceScreen().findPreference("pref_miamplayer_website");

        // Get the Version
        try {
            mVersion.setTitle(getString(R.string.pref_version_title) +
                    " " +
                    getActivity().getPackageManager()
                            .getPackageInfo(getActivity().getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            mVersion.setTitle(getString(R.string.app_name));
        }

        mVersion.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://github.com/miamplayer-player/Android-Remote"));

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), R.string.app_not_available,
                            Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });
        mMiamPlayer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://www.miamplayer-player.org/"));

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), R.string.app_not_available,
                            Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });
        mAboutDialogPreference.setOnPreferenceClickListener(opclAbout);
    }

    /**
     * Create a new about dialog
     */
    private Preference.OnPreferenceClickListener
            opclAbout = new Preference.OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.pref_about_title)
                    .negativeText(R.string.dialog_close)
                    .customView(R.layout.dialog_about, false)
                    .cancelable(true)
                    .show();

            View view = dialog.getCustomView();

            // Fill the people working on this project
            TextView tvAuthors = (TextView) view.findViewById(R.id.tvAuthors);
            TextView tvSupporters = (TextView) view.findViewById(R.id.tvSupporters);
            TextView tvOthers = (TextView) view.findViewById(R.id.tvOthers);

            // Authors
            tvAuthors.setText("Andreas Muttscheller\n");

            // Supporters
            tvSupporters.setText("David Sansome (MiamPlayer-Dev)\n" +
                    "John Maguire (MiamPlayer-Dev)\n" +
                    "Arnaud Bienner (MiamPlayer-Dev)");

            // Others
            tvOthers.setText(Html.fromHtml(
                    "Thanks to all the <a href=\"https://github.com/miamplayer-player/Android-Remote/graphs/contributors\">contributors</a> and <a href=\"https://www.transifex.com/projects/p/miamplayer-remote/\">translators</a>!"));
            tvOthers.setMovementMethod(LinkMovementMethod.getInstance());

            return true;
        }
    };
}
