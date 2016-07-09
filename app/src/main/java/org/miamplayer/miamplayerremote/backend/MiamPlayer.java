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

import org.miamplayer.miamplayerremote.backend.player.MySong;
import org.miamplayer.miamplayerremote.backend.player.PlaylistManager;

/**
 * This Class stores the attributes of MiamPlayer, like the version,
 * the current player state, current song, etc.
 */
public class MiamPlayer {

    public enum State {PLAY, PAUSE, STOP}

    public enum RepeatMode {OFF, TRACK, ALBUM, PLAYLIST}

    public enum ShuffleMode {OFF, ALL, INSIDE_ALBUM, ALBUMS}

    public final static int DefaultPort = 5500; // Change also strings.xml!

    public final static String DefaultCallVolume = "20";

    public final static String DefaultVolumeInc = "10";

    private String mHostname;

    private String mVersion;

    private MySong mCurrentSong;

    private int mVolume;

    private State mState;

    private RepeatMode mRepeatMode = RepeatMode.OFF;

    private ShuffleMode mShuffleMode = ShuffleMode.OFF;

    private int mSongPosition;

    private PlaylistManager mPlaylistManager = new PlaylistManager();

    public int getSongPosition() {
        return mSongPosition;
    }

    public void setSongPosition(int songPosition) {
        this.mSongPosition = songPosition;
    }

    public MiamPlayer() {
        mVersion = "";
        mCurrentSong = null;
        mVolume = 100;
        mState = State.STOP;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    public MySong getCurrentSong() {
        return mCurrentSong;
    }

    public void setCurrentSong(MySong currentSong) {
        this.mCurrentSong = currentSong;
    }

    public int getVolume() {
        return mVolume;
    }

    public void setVolume(int volume) {
        this.mVolume = volume;
    }

    public State getState() {
        return mState;
    }

    public void setState(State state) {
        this.mState = state;
    }

    public RepeatMode getRepeatMode() {
        return mRepeatMode;
    }

    public void setRepeatMode(RepeatMode mRepeatMode) {
        this.mRepeatMode = mRepeatMode;
    }

    public void nextRepeatMode() {
        switch (mRepeatMode) {
            case OFF:
                mRepeatMode = RepeatMode.TRACK;
                break;
            case TRACK:
                mRepeatMode = RepeatMode.ALBUM;
                break;
            case ALBUM:
                mRepeatMode = RepeatMode.PLAYLIST;
                break;
            case PLAYLIST:
                mRepeatMode = RepeatMode.OFF;
                break;
        }
    }

    public ShuffleMode getShuffleMode() {
        return mShuffleMode;
    }

    public void setShuffleMode(ShuffleMode mShuffleMode) {
        this.mShuffleMode = mShuffleMode;
    }

    public void nextShuffleMode() {
        switch (mShuffleMode) {
            case OFF:
                mShuffleMode = ShuffleMode.ALL;
                break;
            case ALL:
                mShuffleMode = ShuffleMode.INSIDE_ALBUM;
                break;
            case INSIDE_ALBUM:
                mShuffleMode = ShuffleMode.ALBUMS;
                break;
            case ALBUMS:
                mShuffleMode = ShuffleMode.OFF;
                break;
        }
    }

    public PlaylistManager getPlaylistManager() {
        return mPlaylistManager;
    }

    public String getHostname() {
        return mHostname;
    }

    public void setHostname(String hostname) {
        mHostname = hostname;
    }
}
