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

package org.miamplayer.miamplayerremote.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.R;
import org.miamplayer.miamplayerremote.SharedPreferencesKeys;
import org.miamplayer.miamplayerremote.backend.MiamPlayer;
import org.miamplayer.miamplayerremote.backend.MiamPlayerPlayerConnection;
import org.miamplayer.miamplayerremote.backend.player.MySong;
import org.miamplayer.miamplayerremote.backend.receivers.MiamPlayerBroadcastReceiver;
import org.miamplayer.miamplayerremote.utils.Utilities;

public class MiamPlayerWidgetProvider extends AppWidgetProvider {

    private WidgetIntent.MiamPlayerAction mCurrentMiamPlayerAction;

    private MiamPlayerPlayerConnection.ConnectionStatus mCurrentConnectionStatus;

    @Override
    public void onReceive(Context context, Intent intent) {
        mCurrentMiamPlayerAction = WidgetIntent.MiamPlayerAction.DEFAULT;
        mCurrentConnectionStatus = MiamPlayerPlayerConnection.ConnectionStatus.IDLE;

        String action = intent.getAction();
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                int idAction = extras.getInt(WidgetIntent.EXTRA_MIAMPLAYER_ACTION);
                int idState = extras.getInt(WidgetIntent.EXTRA_MIAMPLAYER_CONNECTION_STATE);

                mCurrentMiamPlayerAction = WidgetIntent.MiamPlayerAction.values()[idAction];
                mCurrentConnectionStatus = MiamPlayerPlayerConnection.ConnectionStatus
                        .values()[idState];
            }
        }

        // Call this last. In AppWidgetProvider it calls onUpdate and other methods for the Widget
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Perform this loop procedure for each App Widget that belongs to this provider
        for(int appWidgetId : appWidgetIds) {
            // Get the layout for the App Widget and update fields
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_miamplayer);

            switch (mCurrentMiamPlayerAction) {
                case DEFAULT:
                case CONNECTION_STATUS:
                    updateViewsOnConnectionStatusChange(context, views);
                    break;
                case STATE_CHANGE:
                    updateViewsOnStateChange(context, views);
                    break;
            }

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private void updateViewsOnConnectionStatusChange(Context context, RemoteViews views) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean canConnect = prefs.contains(SharedPreferencesKeys.SP_KEY_IP);

        views.setBoolean(R.id.widget_btn_play_pause, "setEnabled", false);
        views.setBoolean(R.id.widget_btn_next, "setEnabled", false);

        switch (mCurrentConnectionStatus) {
            case IDLE:
            case DISCONNECTED:
                // Reset play button
                views.setImageViewResource(R.id.widget_btn_play_pause,
                        R.drawable.ab_media_play);

                if (canConnect) {
                    // Textviews
                    views.setTextViewText(R.id.widget_subtitle, prefs.getString(
                            SharedPreferencesKeys.SP_KEY_IP, ""));
                    views.setTextViewText(R.id.widget_title,
                            context.getString(R.string.widget_connect_to));

                    // Start an intent to connect to Clemetine
                    Intent intentConnect = new Intent(context, MiamPlayerBroadcastReceiver.class);
                    intentConnect.setAction(MiamPlayerBroadcastReceiver.CONNECT);
                    views.setOnClickPendingIntent(R.id.widget_layout, PendingIntent
                            .getBroadcast(context, 0, intentConnect, PendingIntent.FLAG_ONE_SHOT));
                } else {
                    // Textviews
                    views.setTextViewText(R.id.widget_subtitle,
                            context.getString(R.string.widget_open_miamplayer));
                    views.setTextViewText(R.id.widget_title,
                            context.getString(R.string.widget_not_connected));

                    // Start MiamPlayer Remote
                    views.setOnClickPendingIntent(R.id.widget_layout,
                            Utilities.getMiamPlayerRemotePendingIntent(context));
                }
                break;
            case CONNECTING:
                views.setTextViewText(R.id.widget_subtitle, "");
                views.setTextViewText(R.id.widget_title,
                        context.getString(R.string.connectdialog_connecting));
                break;
            case NO_CONNECTION:
                views.setTextViewText(R.id.widget_subtitle,
                        context.getString(R.string.widget_open_miamplayer));
                views.setTextViewText(R.id.widget_title,
                        context.getString(R.string.widget_couldnt_connect));
                // Start MiamPlayer Remote
                views.setOnClickPendingIntent(R.id.widget_layout,
                        Utilities.getMiamPlayerRemotePendingIntent(context));
                break;
            case CONNECTED:
                views.setBoolean(R.id.widget_btn_play_pause, "setEnabled", true);
                views.setBoolean(R.id.widget_btn_next, "setEnabled", true);
                break;
        }
    }

    private void updateViewsOnStateChange(Context context, RemoteViews views) {
        MySong currentSong = App.MiamPlayer.getCurrentSong();

        // Textviews
        if (currentSong == null) {
            views.setTextViewText(R.id.widget_subtitle, "");
            views.setTextViewText(R.id.widget_title,
                    context.getString(R.string.player_nosong));
        } else {
            views.setTextViewText(R.id.widget_title, currentSong.getTitle());
            views.setTextViewText(R.id.widget_subtitle,
                    currentSong.getArtist() + " / " + currentSong.getAlbum());
        }

        // Play or pause?
        Intent intentPlayPause = new Intent(context, MiamPlayerBroadcastReceiver.class);

        if (App.MiamPlayer.getState() == MiamPlayer.State.PLAY) {
            views.setImageViewResource(R.id.widget_btn_play_pause,
                    R.drawable.ab_media_pause);
            intentPlayPause.setAction(MiamPlayerBroadcastReceiver.PAUSE);
        } else {
            views.setImageViewResource(R.id.widget_btn_play_pause,
                    R.drawable.ab_media_play);
            intentPlayPause.setAction(MiamPlayerBroadcastReceiver.PLAY);
        }
        views.setOnClickPendingIntent(R.id.widget_btn_play_pause,
                PendingIntent
                        .getBroadcast(context, 0, intentPlayPause,
                                PendingIntent.FLAG_ONE_SHOT));

        // Next track
        Intent intentNext = new Intent(context, MiamPlayerBroadcastReceiver.class);
        intentNext.setAction(MiamPlayerBroadcastReceiver.NEXT);

        views.setOnClickPendingIntent(R.id.widget_btn_next,
                PendingIntent
                        .getBroadcast(context, 0, intentNext, PendingIntent.FLAG_ONE_SHOT));

        // When connected, user can start the app by touching anywhere
        views.setOnClickPendingIntent(R.id.widget_layout,
                Utilities.getMiamPlayerRemotePendingIntent(context));
    }
}
