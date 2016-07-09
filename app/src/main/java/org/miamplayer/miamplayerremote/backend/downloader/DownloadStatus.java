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

package org.miamplayer.miamplayerremote.backend.downloader;

import org.miamplayer.miamplayerremote.backend.player.MySong;

public class DownloadStatus {

    public enum DownloaderState {IDLE, TRANSCODING, DOWNLOADING, FINISHED}

    private int mId;

    private DownloaderState mState;

    private double mProgress;

    private MySong mSong = new MySong();

    private int mTotalFiles;

    private int mCurrentFileIndex;

    private int mTranscodingTotal;

    private int mTranscodingFinished;

    public DownloadStatus(int id) {
        mId = id;
    }

    public int getId() {
        return mId;
    }

    public DownloaderState getState() {
        return mState;
    }

    public DownloadStatus setState(DownloaderState state) {
        mState = state;
        return this;
    }

    public double getProgress() {
        return mProgress;
    }

    public DownloadStatus setProgress(double progress) {
        mProgress = progress;
        return this;
    }

    public MySong getSong() {
        return mSong;
    }

    public DownloadStatus setSong(MySong song) {
        mSong = song;
        return this;
    }

    public int getTotalFiles() {
        return mTotalFiles;
    }

    public DownloadStatus setTotalFiles(int totalFiles) {
        mTotalFiles = totalFiles;
        return this;
    }

    public int getCurrentFileIndex() {
        return mCurrentFileIndex;
    }

    public DownloadStatus setCurrentFileIndex(int currentFileIndex) {
        mCurrentFileIndex = currentFileIndex;
        return this;
    }

    public int getTranscodingTotal() {
        return mTranscodingTotal;
    }

    public DownloadStatus setTranscodingTotal(int transcodingTotal) {
        mTranscodingTotal = transcodingTotal;
        return this;
    }

    public int getTranscodingFinished() {
        return mTranscodingFinished;
    }

    public DownloadStatus setTranscodingFinished(int transcodingFinished) {
        mTranscodingFinished = transcodingFinished;
        return this;
    }
}
