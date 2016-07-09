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

package org.miamplayer.miamplayerremote.backend.receivers;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.SharedPreferencesKeys;
import org.miamplayer.miamplayerremote.backend.MiamPlayer;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessageFactory;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer;

public class MiamPlayerPhoneStateCheck extends BroadcastReceiver {

    private static String lastPhoneState = TelephonyManager.EXTRA_STATE_IDLE;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (App.getApp() == null
                || App.MiamPlayerConnection == null
                || App.MiamPlayer == null
                || !App.MiamPlayerConnection.isConnected()) {
            return;
        }

        if (!intent.getAction().equals("android.intent.action.PHONE_STATE")) {
            return;
        }

        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Check if we need to change the volume
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getApp());
        String volumeString = prefs
                .getString(SharedPreferencesKeys.SP_CALL_VOLUME,
                        MiamPlayer.DefaultCallVolume);
        int volume = Integer.parseInt(volumeString);

        // Get the pebble settings
        if (prefs.getBoolean(SharedPreferencesKeys.SP_LOWER_VOLUME, true)) {
            // Get the current state of the telephone
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

            // On Lollipop, the state is broadcasted twice. Only process new states.
            if (lastPhoneState.equals(state))
                return;

            Message msg = Message.obtain();

            LastMiamPlayerState lastMiamPlayerState = new LastMiamPlayerState();
            lastMiamPlayerState.load();

            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)
                    || state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {

                // Only lower the volume once. When receiving a call, the state is RINGING. On pickup
                // OFFHOOK is broadcasted. So we only need to take action when we previously had the
                // IDLE state.
                if (lastPhoneState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    lastMiamPlayerState.volume = App.MiamPlayer.getVolume();
                    lastMiamPlayerState.state = App.MiamPlayer.getState();
                    lastMiamPlayerState.save();

                    if (volume >= 0) {
                        msg.obj = MiamPlayerMessageFactory
                                .buildVolumeMessage(Integer.parseInt(volumeString));
                    } else {
                        msg.obj = MiamPlayerMessage.getMessage(
                                MiamPlayerRemoteProtocolBuffer.MsgType.PAUSE);
                    }
                }
            } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                if (volume >= 0) {
                    msg.obj = MiamPlayerMessageFactory.buildVolumeMessage(lastMiamPlayerState.volume);
                } else {
                    if (lastMiamPlayerState.state.equals(MiamPlayer.State.PLAY)) {
                        msg.obj = MiamPlayerMessage.getMessage(
                                MiamPlayerRemoteProtocolBuffer.MsgType.PLAY);
                    }
                }
            }

            // Now send the message
            if (msg != null && msg.obj != null
                    && App.MiamPlayerConnection != null) {
                App.MiamPlayerConnection.mHandler.sendMessage(msg);
            }

            lastPhoneState = state;
        }
    }

    private class LastMiamPlayerState {
        private final static String KEY_LAST_VOLUME = "phone_last_volume";
        private final static String KEY_LAST_STATE  = "phone_last_state";

        public int volume;
        public MiamPlayer.State state;

        public void load() {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getApp());
            volume = prefs.getInt(KEY_LAST_VOLUME, App.MiamPlayer.getVolume());
            state = MiamPlayer.State.values()[prefs.getInt(KEY_LAST_STATE, App.MiamPlayer.getState().ordinal())];
        }

        public void save() {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getApp());
            SharedPreferences.Editor editor = prefs.edit();

            editor.putInt(KEY_LAST_VOLUME, App.MiamPlayer.getVolume());
            editor.putInt(KEY_LAST_STATE, App.MiamPlayer.getState().ordinal());

            editor.apply();
        }
    }

}
