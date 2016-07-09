/* T
his file is part of the Android MiamPlayer Remote.
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

package org.miamplayer.miamplayerremote.ui.fragments.playerpages;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.R;
import org.miamplayer.miamplayerremote.SharedPreferencesKeys;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessageFactory;
import org.miamplayer.miamplayerremote.ui.interfaces.BackPressHandleable;
import org.miamplayer.miamplayerremote.ui.interfaces.NameableTitle;
import org.miamplayer.miamplayerremote.ui.interfaces.RemoteDataReceiver;
import org.miamplayer.miamplayerremote.utils.Utilities;

public class ConnectionFragment extends Fragment
        implements BackPressHandleable, RemoteDataReceiver, NameableTitle {

    private TextView tv_ip;

    private TextView tv_version;

    private TextView tv_time;

    private TextView tv_traffic;

    private SeekBar sb_volume;

    private SharedPreferences mSharedPref;

    private Timer mUpdateTimer;

    private boolean mUserChangesVolume;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the shared preferences
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        setHasOptionsMenu(true);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player_connection,
                container, false);

        tv_ip = (TextView) view.findViewById(R.id.cn_ip);
        tv_time = (TextView) view.findViewById(R.id.cn_time);
        tv_version = (TextView) view.findViewById(R.id.cn_version);
        tv_traffic = (TextView) view.findViewById(R.id.cn_traffic);

        updateData();

        tv_ip.setText(mSharedPref.getString(SharedPreferencesKeys.SP_KEY_IP, "") + ":" + mSharedPref
                .getString(SharedPreferencesKeys.SP_KEY_PORT, ""));
        tv_version.setText(App.MiamPlayer.getVersion());

        sb_volume = (SeekBar) view.findViewById(R.id.cn_volume);
        sb_volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Message msg = Message.obtain();
                    msg.obj = MiamPlayerMessageFactory.buildVolumeMessage(progress);
                    App.MiamPlayerConnection.mHandler.sendMessage(msg);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mUserChangesVolume = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mUserChangesVolume = false;
            }
        });

        sb_volume.setProgress(App.MiamPlayer.getVolume());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mUpdateTimer = new Timer();
        mUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateData();
                        }
                    });
                }
            }
        }, 500, 500);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mUpdateTimer != null) {
            mUpdateTimer.cancel();
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateData() {
        if (App.MiamPlayerConnection == null) {
            return;
        }
        long diff = new Date().getTime() - App.MiamPlayerConnection.getStartTime();
        String dateFormat = String.format(Locale.US, "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(diff),
                TimeUnit.MILLISECONDS.toMinutes(diff) % 60,
                TimeUnit.MILLISECONDS.toSeconds(diff) % 60
        );
        tv_time.setText(dateFormat);

        int uid = getActivity().getApplicationInfo().uid;

        if (TrafficStats.getUidRxBytes(uid) == TrafficStats.UNSUPPORTED) {
            tv_traffic.setText(R.string.connection_traffic_unsupported);
        } else {
            String tx = Utilities.humanReadableBytes(
                    TrafficStats.getUidTxBytes(uid) - App.MiamPlayerConnection.getStartTx(), true);
            String rx = Utilities.humanReadableBytes(
                    TrafficStats.getUidRxBytes(uid) - App.MiamPlayerConnection.getStartRx(), true);

            long total = TrafficStats.getUidTxBytes(uid) - App.MiamPlayerConnection.getStartTx() +
                    TrafficStats.getUidRxBytes(uid) - App.MiamPlayerConnection.getStartRx();
            long a = total / TimeUnit.MILLISECONDS.toSeconds(diff);
            String perSecond = Utilities.humanReadableBytes(a, true);

            tv_traffic.setText(tx + " / " + rx + " (" + perSecond + "/s)");
        }
    }

    @Override
    public void MessageFromMiamPlayer(MiamPlayerMessage miamPlayerMessage) {
        switch (miamPlayerMessage.getMessageType()) {
            case SET_VOLUME:
                if (!mUserChangesVolume) {
                    sb_volume.setProgress(App.MiamPlayer.getVolume());
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public int getTitleId() {
        return R.string.fragment_title_connection;
    }
}
