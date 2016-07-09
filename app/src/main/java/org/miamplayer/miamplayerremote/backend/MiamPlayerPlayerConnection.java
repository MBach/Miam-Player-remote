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

import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.Date;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.backend.listener.PlayerConnectionListener;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage.ErrorMessage;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessageFactory;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.Message.Builder;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.MsgType;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.ReasonDisconnect;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.ResponseDisconnect;

/**
 * This Thread-Class is used to communicate with MiamPlayer
 */
public class MiamPlayerPlayerConnection extends MiamPlayerSimpleConnection
        implements Runnable {

    public MiamPlayerConnectionHandler mHandler;

    public final static int PROCESS_PROTOC = 874456;

    public enum ConnectionStatus {IDLE, CONNECTING, NO_CONNECTION, CONNECTED, LOST_CONNECTION, DISCONNECTED}

    private final long KEEP_ALIVE_TIMEOUT = 25000; // 25 Second timeout

    private final int MAX_RECONNECTS = 5;

    private Handler mUiHandler;

    private int mLeftReconnects;

    private long mLastKeepAlive;

    private ArrayList<PlayerConnectionListener> mListeners
            = new ArrayList<>();

    private MiamPlayerMessage mRequestConnect;

    private long mStartTx;

    private long mStartRx;

    private long mStartTime;

    private Thread mIncomingThread;

    /**
     * Add a new listener for closed connections
     *
     * @param listener The listener object
     */
    public void addPlayerConnectionListener(PlayerConnectionListener listener) {
        mListeners.add(listener);
    }

    public void run() {
        // Start the thread
        Looper.prepare();
        mHandler = new MiamPlayerConnectionHandler(this);

        fireOnConnectionStatusChanged(ConnectionStatus.IDLE);

        Looper.loop();
    }

    /**
     * Try to connect to MiamPlayer
     *
     * @param message The Request Object. Stores the ip to connect to.
     */
    @Override
    public boolean createConnection(MiamPlayerMessage message) {
        fireOnConnectionStatusChanged(ConnectionStatus.CONNECTING);

        // Reset the connected flag
        mLastKeepAlive = 0;

        // Now try to connect and set the input and output streams
        boolean connected = super.createConnection(message);

        // Check if MiamPlayer dropped the connection.
        // Is possible when we connect from a public ip and miamplayer rejects it
        if (connected && !mSocket.isClosed()) {
            // Now we are connected

            // We can now reconnect MAX_RECONNECTS times when
            // we get a keep alive timeout
            mLeftReconnects = MAX_RECONNECTS;

            // Set the current time to last keep alive
            setLastKeepAlive(System.currentTimeMillis());

            // Until we get a new connection request from ui,
            // don't request the first data a second time
            mRequestConnect = MiamPlayerMessageFactory
                    .buildConnectMessage(message.getIp(), message.getPort(),
                            message.getMessage().getRequestConnect().getAuthCode(),
                            false,
                            message.getMessage().getRequestConnect().getDownloader());

            // Save started transmitted bytes
            int uid = App.getApp().getApplicationInfo().uid;
            mStartTx = TrafficStats.getUidTxBytes(uid);
            mStartRx = TrafficStats.getUidRxBytes(uid);

            mStartTime = new Date().getTime();

            // Create a new thread for reading data from MiamPlayer.
            // This is done blocking, so we receive the data directly instead of
            // waiting for the handler and still be able to send commands directly.
            mIncomingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isConnected() && !mIncomingThread.isInterrupted()) {
                        checkKeepAlive();

                        MiamPlayerMessage m = getProtoc(3000);
                        if (!m.isErrorMessage() || m.getErrorMessage() != ErrorMessage.TIMEOUT) {
                            Message msg = Message.obtain();
                            msg.obj = m;
                            msg.arg1 = PROCESS_PROTOC;
                            mHandler.sendMessage(msg);
                        }
                    }
                }
            });
            mIncomingThread.start();

            // Get hostname
            App.MiamPlayer.setHostname(mSocket.getInetAddress().getHostName());

            fireOnConnectionStatusChanged(ConnectionStatus.CONNECTED);

        } else {
            sendUiMessage(new MiamPlayerMessage(ErrorMessage.NO_CONNECTION));
            fireOnConnectionStatusChanged(ConnectionStatus.NO_CONNECTION);
        }

        return connected;
    }

    /**
     * Process the received protocol buffer
     *
     * @param miamPlayerMessage The Message received from MiamPlayer
     */
    protected void processProtocolBuffer(MiamPlayerMessage miamPlayerMessage) {
        fireOnMiamPlayerMessageReceived(miamPlayerMessage);

        // Close the connection if we have an old proto verion
        if (miamPlayerMessage.isErrorMessage()) {
            closeConnection(miamPlayerMessage);
        } else if (miamPlayerMessage.getMessageType() == MsgType.DISCONNECT) {
            closeConnection(miamPlayerMessage);
        } else {
            sendUiMessage(miamPlayerMessage);
        }
    }

    /**
     * Send a message to the ui thread
     *
     * @param obj The Message containing data
     */
    private void sendUiMessage(Object obj) {
        Message msg = Message.obtain();
        msg.obj = obj;
        // Send the Messages
        if (mUiHandler != null) {
            mUiHandler.sendMessage(msg);
        }
    }

    /**
     * Send a request to miamplayer
     *
     * @param message The request as a RequestToThread object
     * @return true if data was sent, false if not
     */
    @Override
    public boolean sendRequest(MiamPlayerMessage message) {
        // Send the request to MiamPlayer
        boolean ret = super.sendRequest(message);

        // If we lost connection, try to reconnect
        if (!ret) {
            //
            if (mRequestConnect != null) {
                ret = super.createConnection(mRequestConnect);
            }
            if (!ret) {
                // Failed. Close connection
                Builder builder = MiamPlayerMessage.getMessageBuilder(MsgType.DISCONNECT);
                ResponseDisconnect.Builder disc = builder.getResponseDisconnectBuilder();
                disc.setReasonDisconnect(ReasonDisconnect.Server_Shutdown);
                builder.setResponseDisconnect(disc);
                closeConnection(new MiamPlayerMessage(builder));
            }
        }

        return ret;
    }

    /**
     * Disconnect from MiamPlayer
     *
     * @param message The RequestDisconnect Object
     */
    @Override
    public void disconnect(MiamPlayerMessage message) {
        if (isConnected()) {
            // Set the Connected flag to false, so the loop in
            // checkForData() is interrupted
            super.disconnect(message);

            // and close the connection
            closeConnection(message);
        }
    }

    /**
     * Close the socket and the streams
     */
    private void closeConnection(MiamPlayerMessage miamPlayerMessage) {
        // Disconnect socket
        closeSocket();

        sendUiMessage(miamPlayerMessage);

        try {
            mIncomingThread.interrupt();
            mIncomingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Fire the listener
        if (miamPlayerMessage.isErrorMessage() &&
                (miamPlayerMessage.getErrorMessage() == ErrorMessage.IO_EXCEPTION ||
                        miamPlayerMessage.getErrorMessage() == ErrorMessage.KEEP_ALIVE_TIMEOUT)) {
            fireOnConnectionStatusChanged(ConnectionStatus.LOST_CONNECTION);
        }

        fireOnConnectionStatusChanged(ConnectionStatus.DISCONNECTED);

        // Close thread
        Looper.myLooper().quit();
    }

    /**
     * Check the keep alive timeout.
     * If we reached the timeout, we can assume, that we lost the connection
     */
    private void checkKeepAlive() {
        if (mLastKeepAlive > 0
                && (System.currentTimeMillis() - mLastKeepAlive) > KEEP_ALIVE_TIMEOUT) {
            // Check if we shall reconnect
            while (mLeftReconnects > 0) {
                closeSocket();
                if (super.createConnection(mRequestConnect)) {
                    mLeftReconnects = MAX_RECONNECTS;
                    break;
                }

                mLeftReconnects--;
            }

            // We tried, but the server isn't there anymore
            if (mLeftReconnects == 0) {
                Message msg = Message.obtain();
                msg.obj = new MiamPlayerMessage(ErrorMessage.KEEP_ALIVE_TIMEOUT);
                msg.arg1 = PROCESS_PROTOC;
                mHandler.sendMessage(msg);
            }
        }
    }

    /**
     * Fire the event to all listeners
     *
     * @param status The current connection status
     */
    private void fireOnConnectionStatusChanged(ConnectionStatus status) {
        for (PlayerConnectionListener listener : mListeners) {
            listener.onConnectionStatusChanged(status);
        }
    }

    /**
     * Fire the event to all listeners
     */
    private void fireOnMiamPlayerMessageReceived(MiamPlayerMessage msg) {
        for (PlayerConnectionListener listener : mListeners) {
            listener.onMiamPlayerMessageReceived(msg);
        }
    }

    /**
     * Set the ui Handler, to which the thread should talk to
     *
     * @param playerHandler The Handler
     */
    public void setUiHandler(Handler playerHandler) {
        this.mUiHandler = playerHandler;
    }

    /**
     * Set the last keep alive timestamp
     *
     * @param lastKeepAlive The time
     */
    public void setLastKeepAlive(long lastKeepAlive) {
        this.mLastKeepAlive = lastKeepAlive;
    }

    public long getStartTx() {
        return mStartTx;
    }

    public long getStartRx() {
        return mStartRx;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public MiamPlayerMessage getRequestConnect() {
        return mRequestConnect;
    }
}
