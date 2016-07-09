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

package org.miamplayer.miamplayerremote.backend;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;

/**
 * This class receives the handler messages from the ui thread
 */
public class MiamPlayerConnectionHandler extends Handler {

    WeakReference<MiamPlayerPlayerConnection> mMiamPlayerConnection;

    public MiamPlayerConnectionHandler(MiamPlayerPlayerConnection c) {
        mMiamPlayerConnection = new WeakReference<>(c);
    }

    @Override
    public void handleMessage(Message msg) {
        MiamPlayerPlayerConnection myMiamPlayerConnection = mMiamPlayerConnection.get();

        if (msg.arg1 == MiamPlayerPlayerConnection.PROCESS_PROTOC) {
            myMiamPlayerConnection.processProtocolBuffer((MiamPlayerMessage) msg.obj);
        } else {
            // Act on the message
            MiamPlayerMessage message = (MiamPlayerMessage) msg.obj;
            if (message.isErrorMessage()) {
                myMiamPlayerConnection.disconnect(message);
            } else {
                switch (message.getMessageType()) {
                    case CONNECT:
                        if (!myMiamPlayerConnection.isConnected()) {
                            myMiamPlayerConnection.createConnection(message);
                        }
                        break;
                    case DISCONNECT:
                        myMiamPlayerConnection.disconnect(message);
                        break;
                    default:
                        myMiamPlayerConnection.sendRequest(message);
                        break;
                }
            }
        }
    }
}
