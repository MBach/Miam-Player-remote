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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Message;
import android.preference.PreferenceManager;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.SharedPreferencesKeys;
import org.miamplayer.miamplayerremote.backend.MiamPlayer;
import org.miamplayer.miamplayerremote.backend.MiamPlayerService;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.MsgType;

public class MiamPlayerBroadcastReceiver extends BroadcastReceiver {

    public static final String CONNECT = "org.miamplayer.miamplayerremote.connect";

    public static final String DISCONNECT = "org.miamplayer.miamplayerremote.disconnect";

    public static final String PLAYPAUSE = "org.miamplayer.miamplayerremote.playpause";

    public static final String PLAY = "org.miamplayer.miamplayerremote.play";

    public static final String PAUSE = "org.miamplayer.miamplayerremote.pause";

    public static final String NEXT = "org.miamplayer.miamplayerremote.next";

    @Override
    public void onReceive(Context context, Intent intent) {
        Message msg = Message.obtain();

        // Check which key was pressed
        switch (intent.getAction()) {
            case CONNECT:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                String ip = prefs.getString(SharedPreferencesKeys.SP_KEY_IP, "");
                int port = Integer.valueOf(
                        prefs.getString(SharedPreferencesKeys.SP_KEY_PORT,
                                String.valueOf(MiamPlayer.DefaultPort)));
                int auth = prefs.getInt(SharedPreferencesKeys.SP_LAST_AUTH_CODE, 0);

                // Start the background service
                Intent serviceIntent = new Intent(context, MiamPlayerService.class);
                serviceIntent.putExtra(MiamPlayerService.SERVICE_ID,
                        MiamPlayerService.SERVICE_START);
                serviceIntent.putExtra(MiamPlayerService.EXTRA_STRING_IP, ip);
                serviceIntent.putExtra(MiamPlayerService.EXTRA_INT_PORT, port);
                serviceIntent.putExtra(MiamPlayerService.EXTRA_INT_AUTH, auth);
                context.startService(serviceIntent);
                break;
            case DISCONNECT:
                msg.obj = MiamPlayerMessage.getMessage(MsgType.DISCONNECT);
                break;
            case PLAYPAUSE:
                msg.obj = MiamPlayerMessage.getMessage(MsgType.PLAYPAUSE);
                break;
            case PLAY:
                msg.obj = MiamPlayerMessage.getMessage(MsgType.PLAY);
                break;
            case PAUSE:
                msg.obj = MiamPlayerMessage.getMessage(MsgType.PAUSE);
                break;
            case NEXT:
                msg.obj = MiamPlayerMessage.getMessage(MsgType.NEXT);
                break;
        }

        // Now send the message
        if (msg != null && msg.obj != null
                && App.MiamPlayerConnection != null) {
            App.MiamPlayerConnection.mHandler.sendMessage(msg);
        }
    }
}
