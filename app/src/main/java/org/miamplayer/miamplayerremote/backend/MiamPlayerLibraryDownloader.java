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

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import org.miamplayer.miamplayerremote.SharedPreferencesKeys;
import org.miamplayer.miamplayerremote.backend.globalsearch.elements.DownloaderResult;
import org.miamplayer.miamplayerremote.backend.globalsearch.elements.DownloaderResult.DownloadResult;
import org.miamplayer.miamplayerremote.backend.library.LibraryDatabaseHelper;
import org.miamplayer.miamplayerremote.backend.listener.OnLibraryDownloadListener;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessageFactory;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.MsgType;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.ResponseLibraryChunk;
import org.miamplayer.miamplayerremote.utils.Utilities;

public class MiamPlayerLibraryDownloader extends
        AsyncTask<MiamPlayerMessage, Long, DownloaderResult> {

    private final String TAG = "MiamPlayerLibraryDownloader";

    private Context mContext;

    private SharedPreferences mSharedPref;

    private MiamPlayerSimpleConnection mClient = new MiamPlayerSimpleConnection();

    private LibraryDatabaseHelper mLibrary;

    private LinkedList<OnLibraryDownloadListener> listeners
            = new LinkedList<>();

    private int mTotalSize;

    public MiamPlayerLibraryDownloader(Context context) {
        mContext = context;
        mLibrary = new LibraryDatabaseHelper();
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    /**
     * Add a OnLibraryDownloadFinishedListener. It is emitted when the download
     * is finished and the library file is available
     *
     * @param l The listener object
     */
    public void addOnLibraryDownloadListener(
            OnLibraryDownloadListener l) {
        listeners.add(l);
    }

    public void removeOnLibraryDownloadListener(OnLibraryDownloadListener l) {
        listeners.remove(l);
    }

    public void startDownload(MiamPlayerMessage message) {
        this.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);
    }

    @Override
    protected DownloaderResult doInBackground(MiamPlayerMessage... params) {
        if (mSharedPref.getBoolean(SharedPreferencesKeys.SP_WIFI_ONLY, false)
                && !Utilities.onWifi()) {
            return new DownloaderResult(0, DownloaderResult.DownloadResult.ONLY_WIFI);
        }

        // First create a connection
        if (!connect()) {
            return new DownloaderResult(0, DownloaderResult.DownloadResult.CONNECTION_ERROR);
        }

        // Start the download
        return startDownloading(params[0]);
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
        fireOnProgressUpdateListener(progress[0]);

        if (progress[0] == mTotalSize) {
            fireOnOptimizeLibraryListener();
        }
    }

    @Override
    protected void onCancelled() {
        fireOnLibraryDownloadFinishedListener(new DownloaderResult(0,
                DownloaderResult.DownloadResult.CANCELLED));
    }

    @Override
    protected void onPostExecute(DownloaderResult result) {
        // Notify the listeners
        fireOnLibraryDownloadFinishedListener(result);
    }

    /**
     * Connect to MiamPlayer
     *
     * @return true if the connection was established, false if not
     */
    private boolean connect() {
        String ip = mSharedPref.getString(SharedPreferencesKeys.SP_KEY_IP, "");
        int port;
        try {
            port = Integer.valueOf(mSharedPref.getString(SharedPreferencesKeys.SP_KEY_PORT,
                    String.valueOf(MiamPlayer.DefaultPort)));
        } catch (NumberFormatException e) {
            port = MiamPlayer.DefaultPort;
        }
        int authCode = mSharedPref.getInt(SharedPreferencesKeys.SP_LAST_AUTH_CODE, 0);

        return mClient.createConnection(MiamPlayerMessageFactory
                .buildConnectMessage(ip, port, authCode, false, true));
    }

    /**
     * Start the Download
     */
    private DownloaderResult startDownloading(
            MiamPlayerMessage miamPlayerMessage) {
        boolean downloadFinished = false;
        DownloaderResult result = new DownloaderResult(0,
                DownloadResult.SUCCESSFUL);
        File f = null;
        FileOutputStream fo = null;

        // Now request the songs
        mClient.sendRequest(miamPlayerMessage);

        while (!downloadFinished) {
            // Check if the user canceled the process
            if (isCancelled()) {
                // Close the stream and delete the incomplete file
                try {
                    if (fo != null) {
                        fo.flush();
                        fo.close();
                    }
                    if (f != null) {
                        f.delete();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }

            // Get the raw protocol buffer
            MiamPlayerMessage message = mClient.getProtoc(0);

            if (message.isErrorMessage()) {
                result = new DownloaderResult(0, DownloadResult.CONNECTION_ERROR);
                break;
            }

            // Is the download forbidden?
            if (message.getMessageType() == MsgType.DISCONNECT) {
                result = new DownloaderResult(0, DownloadResult.FOBIDDEN);
                break;
            }

            // Ignore other elements!
            if (message.getMessageType() != MsgType.LIBRARY_CHUNK) {
                continue;
            }

            ResponseLibraryChunk chunk = message.getMessage()
                    .getResponseLibraryChunk();

            try {
                // Check if we need to create a new file
                if (f == null) {
                    // Check if we have enougth free space
                    // size times 2, because we optimise the table later and
                    // need space for that too!
                    if ((chunk.getSize() * 2) > Utilities.getFreeSpaceExternal()) {
                        result = new DownloaderResult(0,
                                DownloadResult.INSUFFIANT_SPACE);
                        break;
                    }
                    f = mLibrary.getLibraryDb();

                    // User wants to override files, so delete it here!
                    // The check was already done in processSongOffer()
                    if (f.exists()) {
                        f.delete();
                    }

                    f.createNewFile();
                    fo = new FileOutputStream(f);

                    mTotalSize = chunk.getSize();
                }

                // Write chunk to sdcard
                fo.write(chunk.getData().toByteArray());

                publishProgress(f.length());

                // Have we downloaded all chunks?
                if (chunk.getChunkCount() == chunk.getChunkNumber()) {
                    fo.flush();
                    fo.close();
                    f = null;
                    downloadFinished = true;
                }

            } catch (IOException e) {
                result = new DownloaderResult(0,
                        DownloaderResult.DownloadResult.NOT_MOUNTED);
                break;
            }
        }

        // Disconnect at the end
        mClient.disconnect(MiamPlayerMessage.getMessage(MsgType.DISCONNECT));

        // Optimize library table
        if (result.getResult() == DownloadResult.SUCCESSFUL &&
                mLibrary.getLibraryDb().exists()) {
            try {
                mLibrary.optimizeTable();
            } catch (SQLiteException e) {
                // Database is damaged, delete it
                mLibrary.getLibraryDb().delete();
                result = new DownloaderResult(0, DownloadResult.ERROR);
            }
        }

        return result;
    }

    /*
     * Fire the listeners
     */
    private void fireOnLibraryDownloadFinishedListener(DownloaderResult result) {
        for (OnLibraryDownloadListener l : listeners) {
            l.OnLibraryDownloadFinished(result);
        }
    }

    private void fireOnOptimizeLibraryListener() {
        for (OnLibraryDownloadListener l : listeners) {
            l.OnOptimizeLibrary();
        }
    }

    private void fireOnProgressUpdateListener(long progress) {
        for (OnLibraryDownloadListener l : listeners) {
            l.OnProgressUpdate(progress, mTotalSize);
        }
    }

}
