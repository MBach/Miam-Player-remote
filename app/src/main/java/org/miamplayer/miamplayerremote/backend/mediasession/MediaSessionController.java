/* This file is part of the Android MiamPlayer Remote.
 * Copyright (C) 2014, Andreas Muttscheller <asfa194@gmail.com>
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

package org.miamplayer.miamplayerremote.backend.mediasession;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.backend.MiamPlayer;
import org.miamplayer.miamplayerremote.backend.MiamPlayerPlayerConnection;
import org.miamplayer.miamplayerremote.backend.listener.PlayerConnectionListener;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;
import org.miamplayer.miamplayerremote.backend.player.MySong;
import org.miamplayer.miamplayerremote.backend.receivers.MiamPlayerMediaButtonEventReceiver;
import org.miamplayer.miamplayerremote.widget.MiamPlayerWidgetProvider;
import org.miamplayer.miamplayerremote.widget.WidgetIntent;

public class MediaSessionController {

    private final String PLAYSTATE_CHANGED = "com.android.music.playstatechanged";

    private final String META_CHANGED = "com.android.music.metachanged";

    private Context mContext;

    private MiamPlayerPlayerConnection mMiamPlayerPlayerConnection;

    private MiamPlayerMediaSession mMiamPlayerMediaSession;

    private MiamPlayerMediaSessionNotification mMediaSessionNotification;

    private AudioManager mAudioManager;

    private BroadcastReceiver mMediaButtonBroadcastReceiver;

    public MediaSessionController(Context context,
            MiamPlayerPlayerConnection miamplayerPlayerConnection) {
        mContext = context;
        mMiamPlayerPlayerConnection = miamplayerPlayerConnection;

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        mMediaButtonBroadcastReceiver = new MiamPlayerMediaButtonEventReceiver();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mMiamPlayerMediaSession = new MiamPlayerMediaSessionV20(mContext);
        } else {
            mMiamPlayerMediaSession = new MiamPlayerMediaSessionV21(mContext);
        }
        mMediaSessionNotification = new MiamPlayerMediaSessionNotification(mContext);
    }

    public void registerMediaSession() {
        mMiamPlayerPlayerConnection.addPlayerConnectionListener(new PlayerConnectionListener() {
            @Override
            public void onConnectionStatusChanged(
                    MiamPlayerPlayerConnection.ConnectionStatus status) {
                switch (status) {
                    case IDLE:
                        break;
                    case CONNECTING:
                        break;
                    case NO_CONNECTION:
                        break;
                    case CONNECTED:
                        // Request AudioFocus, so the widget is shown on the lock-screen
                        mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                                AudioManager.STREAM_MUSIC,
                                AudioManager.AUDIOFOCUS_GAIN);

                        // Register MediaButtonReceiver
                        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
                        mContext.registerReceiver(mMediaButtonBroadcastReceiver, filter);

                        mMiamPlayerMediaSession.registerSession();
                        mMediaSessionNotification.registerSession();
                        mMediaSessionNotification.setMediaSessionCompat(
                                mMiamPlayerMediaSession.getMediaSession());
                        break;
                    case DISCONNECTED:
                        mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
                        mContext.unregisterReceiver(mMediaButtonBroadcastReceiver);

                        mMiamPlayerMediaSession.unregisterSession();
                        mMediaSessionNotification.unregisterSession();
                        break;
                }
                sendWidgetUpdateIntent(WidgetIntent.MiamPlayerAction.CONNECTION_STATUS, status);
            }

            @Override
            public void onMiamPlayerMessageReceived(MiamPlayerMessage miamPlayerMessage) {
                if (miamPlayerMessage.isErrorMessage()) {
                    return;
                }

                switch (miamPlayerMessage.getMessageType()) {
                    case CURRENT_METAINFO:
                        mMiamPlayerMediaSession.updateSession();
                        mMediaSessionNotification.updateSession();
                        sendMetachangedIntent(META_CHANGED);
                        sendWidgetUpdateIntent(WidgetIntent.MiamPlayerAction.STATE_CHANGE,
                                MiamPlayerPlayerConnection.ConnectionStatus.CONNECTED);
                        break;
                    case PLAY:
                    case PAUSE:
                    case STOP:
                        mMiamPlayerMediaSession.updateSession();
                        mMediaSessionNotification.updateSession();
                        sendMetachangedIntent(PLAYSTATE_CHANGED);
                        sendWidgetUpdateIntent(WidgetIntent.MiamPlayerAction.STATE_CHANGE,
                                MiamPlayerPlayerConnection.ConnectionStatus.CONNECTED);
                        break;
                    case FIRST_DATA_SENT_COMPLETE:
                        sendWidgetUpdateIntent(WidgetIntent.MiamPlayerAction.STATE_CHANGE,
                                MiamPlayerPlayerConnection.ConnectionStatus.CONNECTED);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void sendMetachangedIntent(String what) {
        MySong currentSong = App.MiamPlayer.getCurrentSong();
        Intent i = new Intent(what);
        i.putExtra("playing", App.MiamPlayer.getState() == MiamPlayer.State.PLAY);
        if (null != currentSong) {
            i.putExtra("id", Long.valueOf(currentSong.getId()));
            i.putExtra("artist", currentSong.getArtist());
            i.putExtra("album", currentSong.getAlbum());
            i.putExtra("track", currentSong.getTitle());
        }

        mContext.sendBroadcast(i);
    }

    private void sendWidgetUpdateIntent(WidgetIntent.MiamPlayerAction action,
            MiamPlayerPlayerConnection.ConnectionStatus connectionStatus) {
        // Get widget ids
        ComponentName widgetComponent = new ComponentName(mContext.getPackageName(),
                MiamPlayerWidgetProvider.class.getName());
        int[] widgetIds = AppWidgetManager.getInstance(mContext).getAppWidgetIds(widgetComponent);

        if (widgetIds.length > 0) {
            Intent intent = new Intent(mContext, MiamPlayerWidgetProvider.class);
            intent.setAction(WidgetIntent.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(WidgetIntent.EXTRA_APPWIDGET_IDS, widgetIds);
            intent.putExtra(WidgetIntent.EXTRA_MIAMPLAYER_ACTION, action.ordinal());
            intent.putExtra(WidgetIntent.EXTRA_MIAMPLAYER_CONNECTION_STATE,
                    connectionStatus.ordinal());

            mContext.sendBroadcast(intent);
        }
    }

    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener
            = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
        }
    };
}
