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
import android.os.Bundle;
import android.os.Message;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.backend.MiamPlayerService;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.MsgType;
import org.miamplayer.miamplayerremote.ui.TaskerSettings;
import org.miamplayer.miamplayerremote.utils.bundle.PluginBundleManager;

public class TaskerFireReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (com.twofortyfouram.locale.api.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
            final Bundle bundle = intent.getBundleExtra(
                    com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);

            Message msg = Message.obtain();

            switch (bundle.getInt(PluginBundleManager.BUNDLE_EXTRA_INT_TYPE)) {
                case TaskerSettings.ACTION_CONNECT:
                    String ip = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_IP);
                    int port = bundle.getInt(PluginBundleManager.BUNDLE_EXTRA_INT_PORT);
                    int auth = bundle.getInt(PluginBundleManager.BUNDLE_EXTRA_INT_AUTH);

                    // Start the background service
                    Intent serviceIntent = new Intent(context, MiamPlayerService.class);
                    serviceIntent.putExtra(MiamPlayerService.SERVICE_ID,
                            MiamPlayerService.SERVICE_START);
                    serviceIntent.putExtra(MiamPlayerService.EXTRA_STRING_IP, ip);
                    serviceIntent.putExtra(MiamPlayerService.EXTRA_INT_PORT, port);
                    serviceIntent.putExtra(MiamPlayerService.EXTRA_INT_AUTH, auth);
                    context.startService(serviceIntent);
                    break;
                case TaskerSettings.ACTION_DISCONNECT:
                    msg.obj = MiamPlayerMessage.getMessage(MsgType.DISCONNECT);
                    break;
                case TaskerSettings.ACTION_PLAY:
                    msg.obj = MiamPlayerMessage.getMessage(MsgType.PLAY);
                    break;
                case TaskerSettings.ACTION_PAUSE:
                    msg.obj = MiamPlayerMessage.getMessage(MsgType.PAUSE);
                    break;
                case TaskerSettings.ACTION_PLAYPAUSE:
                    msg.obj = MiamPlayerMessage.getMessage(MsgType.PLAYPAUSE);
                    break;
                case TaskerSettings.ACTION_NEXT:
                    msg.obj = MiamPlayerMessage.getMessage(MsgType.NEXT);
                    break;
                case TaskerSettings.ACTION_STOP:
                    msg.obj = MiamPlayerMessage.getMessage(MsgType.STOP);
                    break;
            }

            // Now send the message
            if (msg != null && msg.obj != null
                    && App.MiamPlayerConnection != null) {
                App.MiamPlayerConnection.mHandler.sendMessage(msg);
            }
        }
    }
}
