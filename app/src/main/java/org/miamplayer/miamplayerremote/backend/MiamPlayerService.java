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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.R;
import org.miamplayer.miamplayerremote.SharedPreferencesKeys;
import org.miamplayer.miamplayerremote.backend.downloader.DownloadManager;
import org.miamplayer.miamplayerremote.backend.globalsearch.GlobalSearchManager;
import org.miamplayer.miamplayerremote.backend.listener.PlayerConnectionListener;
import org.miamplayer.miamplayerremote.backend.mediasession.MiamPlayerMediaSessionNotification;
import org.miamplayer.miamplayerremote.backend.mediasession.MediaSessionController;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessageFactory;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.MsgType;
import org.miamplayer.miamplayerremote.utils.Utilities;

public class MiamPlayerService extends Service {

    public final static String SERVICE_ID = "org.miamplayer.miamplayerremote.service.id";

    public final static int SERVICE_START = 1;

    public final static int SERVICE_DISCONNECTED = 2;

    public final static String EXTRA_STRING_IP = "EXTRA_IP";

    public final static String EXTRA_INT_PORT = "EXTRA_PORT";

    public final static String EXTRA_INT_AUTH = "EXTRA_AUTH";

    private final String TAG = getClass().getSimpleName();

    private NotificationManager mNotificationManager;

    private Thread mPlayerThread;

    private boolean mUseWakeLock = false;

    private PowerManager.WakeLock mWakeLock;

    private Handler mUiHandler;

    private MiamPlayerServiceBinder mMiamPlayerServiceBinder = new MiamPlayerServiceBinder();

    public class MiamPlayerServiceBinder extends Binder {

        public MiamPlayerService getMiamPlayerService() {
            return MiamPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Get a Wakelock Object
        PowerManager pm = (PowerManager) getSystemService(
                Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MiamPlayer");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mUseWakeLock = prefs.getBoolean(SharedPreferencesKeys.SP_WAKE_LOCK, false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMiamPlayerServiceBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(SERVICE_ID)) {
            handleServiceAction(intent);
        }

        return START_STICKY;
    }

    /**
     * Handle the requests to the service
     *
     * @param intent The action to perform
     */
    public void handleServiceAction(final Intent intent) {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int action = intent.getIntExtra(SERVICE_ID, 0);
        switch (action) {
            case SERVICE_START:
                // Create a new instance
                if (App.MiamPlayerConnection == null) {
                    App.MiamPlayerConnection = new MiamPlayerPlayerConnection();
                    App.MiamPlayerConnection.setUiHandler(mUiHandler);
                    MediaSessionController mediaSessionController = new MediaSessionController(this,
                            App.MiamPlayerConnection);
                    mediaSessionController.registerMediaSession();

                    GlobalSearchManager.getInstance().reset();

                    App.MiamPlayerConnection.addPlayerConnectionListener(
                            new PlayerConnectionListener() {
                                @Override
                                public void onConnectionStatusChanged(
                                        MiamPlayerPlayerConnection.ConnectionStatus status) {
                                    switch (status) {
                                        case IDLE:
                                            sendConnectMessageIfPossible(intent);
                                            break;
                                        case CONNECTING:
                                            break;
                                        case NO_CONNECTION:
                                            sendDisconnectServiceMessage();
                                            break;
                                        case CONNECTED:
                                            if (mUseWakeLock) {
                                                mWakeLock.acquire();
                                            }
                                            break;
                                        case LOST_CONNECTION:
                                            showKeepAliveDisconnectNotification();
                                            break;
                                        case DISCONNECTED:
                                            sendDisconnectServiceMessage();

                                            if (mUseWakeLock) {
                                                mWakeLock.release();
                                            }
                                            break;
                                    }
                                }

                                @Override
                                public void onMiamPlayerMessageReceived(
                                        MiamPlayerMessage miamPlayerMessage) {
                                    GlobalSearchManager.getInstance().parseMiamPlayerMessage(
                                            miamPlayerMessage);
                                }
                            });

                    mPlayerThread = new Thread(App.MiamPlayerConnection);
                    mPlayerThread.start();
                } else {
                    sendConnectMessageIfPossible(intent);
                }
                break;
            case SERVICE_DISCONNECTED:
                intteruptThread();
                App.MiamPlayerConnection = null;
                stopSelf();
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        if (App.MiamPlayerConnection != null
                && App.MiamPlayerConnection.isConnected()) {
            // Move the request to the message
            Message msg = Message.obtain();
            msg.obj = MiamPlayerMessage.getMessage(MsgType.DISCONNECT);

            // Send the request to the thread
            App.MiamPlayerConnection.mHandler.sendMessage(msg);
        }
        intteruptThread();
        App.MiamPlayerConnection = null;
    }

    public void setUiHandler(Handler uiHandler) {
        mUiHandler = uiHandler;
    }

    private void sendDisconnectServiceMessage() {
        Intent mServiceIntent = new Intent(
                MiamPlayerService.this,
                MiamPlayerService.class);
        mServiceIntent
                .putExtra(SERVICE_ID, SERVICE_DISCONNECTED);
        startService(mServiceIntent);
    }

    private void intteruptThread() {
        if (mPlayerThread != null) {
            mPlayerThread.interrupt();
        }

        if (App.MiamPlayerConnection != null
                && App.MiamPlayerConnection.mHandler != null
                && mPlayerThread.isAlive()) {
            App.MiamPlayerConnection.mHandler.post(new Runnable() {

                @Override
                public void run() {
                    Looper.myLooper().quit();
                }

            });
        }

        DownloadManager.getInstance().shutdown();
    }

    /**
     * Create a notification that shows, that we got a keep alive timeout
     */
    @SuppressLint("InlinedApi")
    private void showKeepAliveDisconnectNotification() {
         Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_disconnect_keep_alive))
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(Utilities.getMiamPlayerRemotePendingIntent(this))
                .build();
        mNotificationManager
                .notify(MiamPlayerMediaSessionNotification.NOTIFIFCATION_ID, notification);
    }

    private void sendConnectMessageIfPossible(Intent intent) {
        if (intent.hasExtra(EXTRA_STRING_IP)) {
            final String ip = intent.getStringExtra(EXTRA_STRING_IP);
            final int port = intent.getIntExtra(EXTRA_INT_PORT, 0);
            final int auth = intent.getIntExtra(EXTRA_INT_AUTH, 0);

            Message msg = Message.obtain();
            msg.obj = MiamPlayerMessageFactory
                    .buildConnectMessage(ip, port, auth, true, false);
            App.MiamPlayerConnection.mHandler.sendMessage(msg);
        }
    }
}
