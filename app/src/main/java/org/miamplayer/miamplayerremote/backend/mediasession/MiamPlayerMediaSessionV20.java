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

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.backend.MiamPlayer;
import org.miamplayer.miamplayerremote.backend.player.MySong;
import org.miamplayer.miamplayerremote.backend.receivers.MiamPlayerMediaButtonEventReceiver;

@SuppressWarnings("deprecation")
public class MiamPlayerMediaSessionV20 extends MiamPlayerMediaSession {

    private AudioManager mAudioManager;

    private ComponentName mMiamPlayerMediaButtonEventReceiver;

    private android.media.RemoteControlClient mRcClient;

    public MiamPlayerMediaSessionV20(Context context) {
        super(context);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        mMiamPlayerMediaButtonEventReceiver = new ComponentName(mContext.getPackageName(),
                MiamPlayerMediaButtonEventReceiver.class.getName());
    }

    @Override
    public void registerSession() {
        mAudioManager.registerMediaButtonEventReceiver(mMiamPlayerMediaButtonEventReceiver);

        // Create the intent
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mMiamPlayerMediaButtonEventReceiver);
        PendingIntent mediaPendingIntent = PendingIntent
                .getBroadcast(mContext,
                        0,
                        mediaButtonIntent,
                        0);

        // Create the client
        mRcClient = new android.media.RemoteControlClient(mediaPendingIntent);
        if (App.MiamPlayer.getState() == MiamPlayer.State.PLAY) {
            mRcClient.setPlaybackState(android.media.RemoteControlClient.PLAYSTATE_PLAYING);
        } else {
            mRcClient.setPlaybackState(android.media.RemoteControlClient.PLAYSTATE_PAUSED);
        }
        mRcClient.setTransportControlFlags(android.media.RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                android.media.RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                android.media.RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                android.media.RemoteControlClient.FLAG_KEY_MEDIA_PAUSE);
        mAudioManager.registerRemoteControlClient(mRcClient);
    }

    @Override
    public void unregisterSession() {
        // Disconnect EventReceiver and RemoteControlClient
        mAudioManager.unregisterMediaButtonEventReceiver(mMiamPlayerMediaButtonEventReceiver);

        if (mRcClient != null) {
            mAudioManager.unregisterRemoteControlClient(mRcClient);
        }
    }

    @Override
    public void updateSession() {
        // Update playstate
        if (App.MiamPlayer.getState() == MiamPlayer.State.PLAY) {
            mRcClient.setPlaybackState(android.media.RemoteControlClient.PLAYSTATE_PLAYING);
        } else {
            mRcClient.setPlaybackState(android.media.RemoteControlClient.PLAYSTATE_PAUSED);
        }

        // Change the data
        MySong song = App.MiamPlayer.getCurrentSong();
        if (song != null && song.getArt() != null) {
            android.media.RemoteControlClient.MetadataEditor editor = mRcClient.editMetadata(false);
            editor.putBitmap(android.media.RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, song.getArt());

            // The RemoteControlClients displays the following info:
            // METADATA_KEY_TITLE (white) - METADATA_KEY_ALBUMARTIST (grey) - METADATA_KEY_ALBUM (grey)
            //
            // So I put the metadata not in the "correct" fields to display artist, track and album
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, song.getAlbum());
            editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, song.getArtist());
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, song.getTitle());
            editor.apply();
        }
    }
}
