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

import java.util.LinkedList;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.backend.listener.OnGlobalSearchResponseListener;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer;

public class GlobalSearchManager {

    private int mCurrentId = -1;

    private GlobalSearchRequest mRequest;

    private GlobalSearchDatabaseHelper mGlobalSearchDatabaseHelper = new GlobalSearchDatabaseHelper(
            App.getApp());

    private LinkedList<OnGlobalSearchResponseListener> mListeners = new LinkedList<>();

    private GlobalSearchProviderIconStore mGlobalSearchProviderIconStore
            = new GlobalSearchProviderIconStore();

    private static GlobalSearchManager mInstance;

    public static GlobalSearchManager getInstance() {
        if (mInstance == null) {
            mInstance = new GlobalSearchManager();
        }
        return mInstance;
    }

    private GlobalSearchManager() {
        reset();
    }

    public void reset() {
        mRequest = null;
        mCurrentId = -1;

        mGlobalSearchDatabaseHelper.deleteAll();
    }

    public void parseMiamPlayerMessage(MiamPlayerMessage miamPlayerMessage) {
        if (miamPlayerMessage.isErrorMessage())
            return;


        switch (miamPlayerMessage.getMessageType()) {
            case GLOBAL_SEARCH_RESULT:
                parseGlobalSearchResult(miamPlayerMessage.getMessage().getResponseGlobalSearch());
                break;
            case GLOBAL_SEARCH_STATUS:
                parseGlobalSearchStatus(
                        miamPlayerMessage.getMessage().getResponseGlobalSearchStatus());
                break;
        }

    }

    private void parseGlobalSearchResult(
            MiamPlayerRemoteProtocolBuffer.ResponseGlobalSearch responseGlobalSearch) {
        int id = responseGlobalSearch.getId();

        // Parse only results for the current request
        if (id != mCurrentId) {
            return;
        }

        mRequest.addSearchResults(responseGlobalSearch);

        // Add the icon to the store
        getGlobalSearchProviderIconStore().insertProvider(responseGlobalSearch.getSearchProvider(),
                responseGlobalSearch.getSearchProviderIcon());

        fireOnResultsReceived(id);
    }

    private void parseGlobalSearchStatus(
            MiamPlayerRemoteProtocolBuffer.ResponseGlobalSearchStatus responseGlobalSearchStatus) {
        int id = responseGlobalSearchStatus.getId();

        switch (responseGlobalSearchStatus.getStatus()) {
            case GlobalSearchStarted:
                mRequest = new GlobalSearchRequest(id, mGlobalSearchDatabaseHelper);
                mCurrentId = id;
                break;
            case GlobalSearchFinished:
                if (id == mCurrentId) {
                    mRequest.setStatus(responseGlobalSearchStatus.getStatus());
                }
                break;
        }

        fireOnStatusChanged(id, responseGlobalSearchStatus.getStatus());
    }

    public void addOnGlobalSearchResponseListerner(OnGlobalSearchResponseListener l) {
        mListeners.add(l);
    }

    public void removeOnGlobalSearchResponseListerner(OnGlobalSearchResponseListener l) {
        mListeners.remove(l);
    }

    private void fireOnStatusChanged(int id,
                                     MiamPlayerRemoteProtocolBuffer.GlobalSearchStatus status) {
        for (OnGlobalSearchResponseListener l : mListeners) {
            l.onStatusChanged(id, status);
        }
    }

    private void fireOnResultsReceived(int id) {
        for (OnGlobalSearchResponseListener l : mListeners) {
            l.onResultsReceived(id);
        }
    }

    public GlobalSearchProviderIconStore getGlobalSearchProviderIconStore() {
        return mGlobalSearchProviderIconStore;
    }

    public GlobalSearchRequest getRequest() {
        return mRequest;
    }
}
