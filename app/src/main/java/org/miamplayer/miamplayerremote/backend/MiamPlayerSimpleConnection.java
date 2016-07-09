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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage.ErrorMessage;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerPbParser;

public class MiamPlayerSimpleConnection {

    // Socket, input and output streams
    protected Socket mSocket;

    protected DataInputStream mIn;

    protected DataOutputStream mOut;

    // Protocol buffer data
    private MiamPlayerPbParser mMiamPlayerPbParser = new MiamPlayerPbParser();

    /**
     * Try to connect to MiamPlayer
     *
     * @param message The Request Object. Stores the ip to connect to.
     */
    public boolean createConnection(MiamPlayerMessage message) {
        SocketAddress socketAddress = new InetSocketAddress(message.getIp(), message.getPort());
        mSocket = new Socket();
        try {
            mSocket.connect(socketAddress, 3000);
            mIn = new DataInputStream(mSocket.getInputStream());
            mOut = new DataOutputStream(mSocket.getOutputStream());

            // Send the connect request to miamplayer
            sendRequest(message);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Send a request to miamplayer
     *
     * @param message The request as a RequestToThread object
     * @return true if data was sent, false if not
     */
    public boolean sendRequest(MiamPlayerMessage message) {
        // Create the protocolbuffer
        byte[] data = message.getMessage().toByteArray();
        try {
            mOut.writeInt(data.length);
            mOut.write(data);
            mOut.flush();
        } catch (Exception e) {
            // Try to reconnect
            closeSocket();
            return false;
        }
        return true;
    }

    /**
     * Get the raw protocol buffer message. This function blocks until data is
     * available!
     *
     * @return The parsed protocol buffer
     */
    public MiamPlayerMessage getProtoc(int timeout) {
        MiamPlayerMessage message;
        try {
            // Read the data and return it
            mSocket.setSoTimeout(timeout);
            int len = mIn.readInt();
            // Check length. If it is less zero or more than 50mb it's very likely we got invalid data
            if (len < 0 || len > 52428800) {
                throw new IOException("Invalid data length");
            }
            byte[] data = new byte[len];
            mIn.readFully(data, 0, len);
            message = mMiamPlayerPbParser.parse(data);
        } catch (SocketTimeoutException e) {
            message = new MiamPlayerMessage(ErrorMessage.TIMEOUT);
        } catch (IOException e) {
            message = new MiamPlayerMessage(ErrorMessage.IO_EXCEPTION);
        }

        return message;
    }

    /**
     * Check if the Socket is still connected
     *
     * @return true if a connection is established
     */
    public boolean isConnected() {
        return !(mSocket == null
                || mOut == null
                || !mSocket.isConnected()
                || mSocket.isClosed());
    }

    /**
     * Disconnect from MiamPlayer
     *
     * @param message The RequestDisconnect Object
     */
    public void disconnect(MiamPlayerMessage message) {
        if (isConnected()) {
            // Send the disconnect message to miamplayer
            byte[] data = message.getMessage().toByteArray();

            try {
                // Now send the data
                mOut.writeInt(data.length);
                mOut.write(data);
                mOut.flush();

                closeSocket();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Close the socket and the in and out streams
     */
    protected void closeSocket() {
        try {
            mOut.close();
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
