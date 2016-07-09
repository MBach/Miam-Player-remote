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

import android.appwidget.AppWidgetManager;

public class WidgetIntent {

    public static String ACTION_APPWIDGET_UPDATE = AppWidgetManager.ACTION_APPWIDGET_UPDATE;

    public static String EXTRA_APPWIDGET_IDS = AppWidgetManager.EXTRA_APPWIDGET_IDS;

    public static String EXTRA_MIAMPLAYER_ACTION = "org.miamplayer.miamplayerremote.widget.extra.action";

    public static String EXTRA_MIAMPLAYER_CONNECTION_STATE
            = "org.miamplayer.miamplayerremote.widget.extra.connnect.state";

    public enum MiamPlayerAction {DEFAULT, CONNECTION_STATUS, STATE_CHANGE}

}
