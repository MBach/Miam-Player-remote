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

package org.miamplayer.miamplayerremote.ui;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

import org.miamplayer.miamplayerremote.R;
import org.miamplayer.miamplayerremote.backend.globalsearch.elements.ServiceFound;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;

/**
 * This class is used to handle the messages sent from the
 * connection thread
 */
public class ConnectActivityHandler extends Handler {

    WeakReference<ConnectActivity> mDialog;

    ConnectActivityHandler(ConnectActivity connectActivity) {
        mDialog = new WeakReference<>(connectActivity);
    }

    @Override
    public void handleMessage(Message msg) {
        ConnectActivity cd = mDialog.get();
        if (cd != null) {
            if (msg.obj instanceof MiamPlayerMessage) {
                MiamPlayerMessage miamPlayerMessage = (MiamPlayerMessage) msg.obj;

                if (miamPlayerMessage.isErrorMessage()) {
                    // We have got an error
                    switch (miamPlayerMessage.getErrorMessage()) {
                        case NO_CONNECTION:
                            cd.mPdConnect.dismiss();
                            cd.noConnection();
                            break;
                        case OLD_PROTO:
                            cd.mPdConnect.dismiss();
                            cd.oldProtoVersion();
                            break;
                        default:
                            cd.mPdConnect.dismiss();
                            cd.noConnection();
                            break;
                    }
                    cd.disconnected(miamPlayerMessage);
                } else {
                    // Okay, normal message
                    switch (miamPlayerMessage.getMessageType()) {
                        case INFO:
                            cd.mPdConnect
                                    .setContent(cd.getString(R.string.connectdialog_download_data));
                            break;
                        case FIRST_DATA_SENT_COMPLETE:
                            cd.mPdConnect.dismiss();
                            cd.showPlayerDialog();
                            break;
                        case DISCONNECT:
                            cd.mPdConnect.dismiss();
                            cd.disconnected(miamPlayerMessage);
                            break;
                        default:
                            break;
                    }
                }
            } else if (msg.obj instanceof ServiceFound) {
                cd.serviceFound();
            }
        }
    }
}
