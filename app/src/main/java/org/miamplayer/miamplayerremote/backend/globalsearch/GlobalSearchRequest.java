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

package org.miamplayer.miamplayerremote.backend.globalsearch;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;

import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer;

public class GlobalSearchRequest {
    private int mId;

    private GlobalSearchDatabaseHelper mGlobalSearchDatabaseHelper;

    private MiamPlayerRemoteProtocolBuffer.GlobalSearchStatus mStatus;

    private HashMap<String, MiamPlayerRemoteProtocolBuffer.SongMetadata> mUrlMetadata = new HashMap<>();

    public GlobalSearchRequest(int id, GlobalSearchDatabaseHelper db) {
        mId = id;
        mGlobalSearchDatabaseHelper = db;
        mStatus = MiamPlayerRemoteProtocolBuffer.GlobalSearchStatus.GlobalSearchStarted;
    }

    public int getId() {
        return mId;
    }

    public MiamPlayerRemoteProtocolBuffer.GlobalSearchStatus getStatus() {
        return mStatus;
    }

    public void setStatus(
            MiamPlayerRemoteProtocolBuffer.GlobalSearchStatus status) {
        mStatus = status;
    }

    public void addSearchResults(
            MiamPlayerRemoteProtocolBuffer.ResponseGlobalSearch searchResult) {

        SQLiteDatabase db = mGlobalSearchDatabaseHelper.getWritableDatabase();
        db.beginTransaction();

        for (MiamPlayerRemoteProtocolBuffer.SongMetadata song : searchResult.getSongMetadataList()) {
            ContentValues contentValues = new ContentValues();

            contentValues.put("global_search_id", getId());
            contentValues.put("search_query", searchResult.getQuery());
            contentValues.put("search_provider", searchResult.getSearchProvider());

            contentValues.put("title", song.getTitle());
            contentValues.put("album", song.getAlbum());
            contentValues.put("artist", song.getArtist());
            contentValues.put("albumartist", song.getAlbumartist());
            contentValues.put("track", song.getTrack());
            contentValues.put("disc", song.getDisc());
            contentValues.put("pretty_year", song.getPrettyYear());
            contentValues.put("genre", song.getGenre());
            contentValues.put("pretty_length", song.getPrettyLength());
            try {
                contentValues.put("year", Integer.parseInt(song.getPrettyYear()));
            } catch (NumberFormatException e) {
                contentValues.put("year", -1);
            }
            contentValues.put("filename", song.getUrl()); // filename is url
            contentValues.put("is_local", song.getIsLocal() ? 1 : 0);
            contentValues.put("filesize", song.getFileSize());
            contentValues.put("rating", song.getRating());

            db.insert(GlobalSearchDatabaseHelper.TABLE_NAME, null, contentValues);

            mUrlMetadata.put(song.getUrl(), song);
        }

        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    public MiamPlayerRemoteProtocolBuffer.SongMetadata getSongFromUrl(String url) {
        return mUrlMetadata.get(url);
    }
}
