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

package org.miamplayer.miamplayerremote.ui.fragments;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import android.app.Fragment;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.R;
import org.miamplayer.miamplayerremote.backend.downloader.DownloadManager;
import org.miamplayer.miamplayerremote.backend.listener.OnPlaylistReceivedListener;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessageFactory;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.DownloadItem;
import org.miamplayer.miamplayerremote.backend.player.MyPlaylist;
import org.miamplayer.miamplayerremote.backend.player.MySong;
import org.miamplayer.miamplayerremote.backend.player.PlaylistManager;
import org.miamplayer.miamplayerremote.ui.adapter.PlaylistSongAdapter;
import org.miamplayer.miamplayerremote.ui.interfaces.BackPressHandleable;
import org.miamplayer.miamplayerremote.ui.interfaces.RemoteDataReceiver;

public class PlaylistFragment extends Fragment implements BackPressHandleable, RemoteDataReceiver {

    private PlaylistSongAdapter mAdapter;

    private MaterialDialog mProgressDialog;

    private ActionBar mActionBar;

    private Spinner mPlaylistsSpinner;

    private ListView mList;

    private View mEmptyPlaylist;

    private PlaylistManager mPlaylistManager;

    private OnPlaylistReceivedListener mPlaylistListener;

    private LinkedList<MyPlaylist> mPlaylists = new LinkedList<>();

    private String mFilterText;

    private boolean mUpdateTrackPositionOnNewTrack = false;

    private int mSelectionOffset;

    public PlaylistFragment() {
        mFilterText = "";
        mSelectionOffset = 3;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the actionbar
        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        setHasOptionsMenu(true);

        mPlaylistManager = App.MiamPlayer.getPlaylistManager();
        mPlaylistListener = new OnPlaylistReceivedListener() {
            @Override
            public void onPlaylistSongsReceived(final MyPlaylist p) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mProgressDialog != null) {
                            mProgressDialog.incrementProgress(1);
                            mProgressDialog.setContent(p.getName());
                        }

                        updateSongList();
                    }
                });
            }

            @Override
            public void onPlaylistReceived(final MyPlaylist p) {
            }

            @Override
            public void onAllRequestedPlaylistSongsReceived() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mProgressDialog != null && mProgressDialog.isShowing()) {
                            mPlaylists = mPlaylistManager.getAllPlaylists();

                            mProgressDialog.dismiss();
                            getActivity().invalidateOptionsMenu();

                            mPlaylistsSpinner.setSelection(
                                    mPlaylists.indexOf(mPlaylistManager.getActivePlaylist()));
                        }
                    }
                });
            }

            @Override
            public void onAllPlaylistsReceived() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPlaylists = mPlaylistManager.getAllPlaylists();
                        updatePlaylistSpinner();
                        RequestPlaylistSongs();
                    }
                });
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if we are still connected
        if (App.MiamPlayerConnection == null
                || App.MiamPlayer == null
                || !App.MiamPlayerConnection.isConnected()) {
            return;
        }

        mPlaylistManager.addOnPlaylistReceivedListener(mPlaylistListener);
        mPlaylists = mPlaylistManager.getAllPlaylists();

        mPlaylistsSpinner.setVisibility(View.VISIBLE);

        updatePlaylistSpinner();

        RequestPlaylistSongs();

        // Get the position of the current track if we have one
        if (App.MiamPlayer.getCurrentSong() != null) {
            updateViewPosition();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mPlaylistsSpinner.setVisibility(View.GONE);

        mPlaylistManager.removeOnPlaylistReceivedListener(mPlaylistListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist,
                container, false);

        mPlaylists = mPlaylistManager.getAllPlaylists();

        mList = (ListView) view.findViewById(R.id.songs);
        mEmptyPlaylist = view.findViewById(R.id.playlist_empty);

        // Add Spinner to toolbar
        mPlaylistsSpinner = (Spinner) getActivity().findViewById(R.id.toolbar_spinner);

        mPlaylistsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSongList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        updatePlaylistSpinner();

        // Create the adapter
        mAdapter = new PlaylistSongAdapter(getActivity(), R.layout.item_playlist,
                getSelectedPlaylistSongs());

        mList.setOnItemClickListener(oiclSong);
        mList.setAdapter(mAdapter);

        mList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mList.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public boolean onActionItemClicked(ActionMode mode,
                    android.view.MenuItem item) {
                SparseBooleanArray checkedPositions = mList.getCheckedItemPositions();
                LinkedList<MySong> selectedSongs = new LinkedList<>();

                for (int i = 0; i < checkedPositions.size(); ++i) {
                    int position = checkedPositions.keyAt(i);
                    if (checkedPositions.valueAt(i)) {
                        selectedSongs.add(getSelectedPlaylistSongs().get(position));
                    }
                }

                if (!selectedSongs.isEmpty()) {
                    switch (item.getItemId()) {
                        case R.id.playlist_context_play:
                            playSong(selectedSongs.get(0));

                            mode.finish();
                            return true;
                        case R.id.playlist_context_download:
                            LinkedList<String> urls = new LinkedList<>();
                            for (MySong s : selectedSongs) {
                                urls.add(s.getUrl());
                            }
                            if (!urls.isEmpty()) {
                                DownloadManager.getInstance().addJob(MiamPlayerMessageFactory
                                        .buildDownloadSongsMessage(DownloadItem.Urls,
                                                urls));
                            }
                            mode.finish();
                            return true;
                        case R.id.playlist_context_remove:
                            Message msg = Message.obtain();
                            msg.obj = MiamPlayerMessageFactory
                                    .buildRemoveMultipleSongsFromPlaylist(getPlaylistId(),
                                            selectedSongs);
                            App.MiamPlayerConnection.mHandler.sendMessage(msg);
                            mode.finish();
                            return true;
                        default:
                            return false;
                    }
                }
                return false;
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode,
                    android.view.Menu menu) {
                android.view.MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.playlist_context_menu, menu);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(
                            getActivity(), R.color.grey_cab_status));

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode,
                    android.view.Menu menu) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(
                            getActivity(), R.color.actionbar_dark));
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode,
                    int position, long id, boolean checked) {
            }
        });

        // Filter the results
        mAdapter.getFilter().filter(mFilterText);

        mActionBar.setTitle("");
        mActionBar.setSubtitle("");

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mList.setFastScrollEnabled(true);
        mList.setTextFilterEnabled(true);
        mList.setSelector(new ColorDrawable(ContextCompat.getColor(getActivity(), android.R.color.transparent)));
        mList.setDivider(null);
        mList.setDividerHeight(0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.download_playlist:
                DownloadManager.getInstance().addJob(MiamPlayerMessageFactory
                        .buildDownloadSongsMessage(DownloadItem.APlaylist, getPlaylistId()));
                return true;
            case R.id.clear_playlist:
                new MaterialDialog.Builder(getActivity())
                        .title(R.string.playlist_clear)
                        .content(R.string.playlist_clear_content)
                        .positiveText(R.string.playlist_clear_confirm)
                        .negativeText(R.string.dialog_cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                mPlaylistManager.clearPlaylist(getPlaylistId());
                                updateSongList();
                            }
                        })
                        .show();
                return true;
            case R.id.close_playlist:
                Message msg = Message.obtain();
                msg.obj = MiamPlayerMessageFactory.buildClosePlaylist(getPlaylistId());
                App.MiamPlayerConnection.mHandler.sendMessage(msg);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.playlist_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        android.view.MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.playlist_context_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Create a listener for search change
        SearchView searchView = (SearchView) menu.findItem(R.id.playlist_menu_search)
                .getActionView();

        final SearchView.OnQueryTextListener queryTextListener
                = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                // Set the filter text as the fragments might not yet
                // created. Only the left and right fragment from the
                // currently active is created (onCreate() called).
                // Therefore the other adapters are not yet created,
                // onCreate filters for this string given in setFilterText()
                setFilterText(newText);
                if (getAdapter() != null) {
                    getAdapter().getFilter().filter(newText);
                }
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                // Do something
                setFilterText(query);
                if (getAdapter() != null) {
                    getAdapter().getFilter().filter(query);
                }

                return true;
            }
        };
        searchView.setOnQueryTextListener(queryTextListener);
        searchView.setQueryHint(getString(R.string.playlist_search_hint));

        EditText searchText = (EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchText.setHintTextColor(ContextCompat.getColor(getActivity(),
                R.color.searchview_edittext_hint));

        super.onPrepareOptionsMenu(menu);
    }

    /**
     * Update the underlying data. It reloads the current playlist songs from the MiamPlayer
     * object.
     */
    public void updateSongList() {
        // Check if we should update the current view position
        mAdapter = new PlaylistSongAdapter(getActivity(), R.layout.item_playlist,
                getSelectedPlaylistSongs());
        mList.setAdapter(mAdapter);

        // We have to post notifyDataSetChanged() here, so fast scroll is set correctly.
        // Without it, the fast scroll cannot get any child views as the adapter is not yet fully
        // attached to the view and getChildCount() returns 0. Therefore, fast scroll won't
        // be enabled.
        // notifyDataSetChanged() forces the listview to recheck the fast scroll preconditions.
        mList.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });

        updateViewPosition();

        if (mPlaylists.isEmpty()) {
            mList.setEmptyView(mEmptyPlaylist);
        }

    }

    /**
     * Set the text to filter
     *
     * @param filterText String, which results are filtered by
     */
    public void setFilterText(String filterText) {
        mFilterText = filterText;
    }

    /**
     * Get the song adapter
     *
     * @return The CustomSongAdapter
     */
    public PlaylistSongAdapter getAdapter() {
        return mAdapter;
    }

    private OnItemClickListener oiclSong = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            playSong(mAdapter.getItem(position));
        }
    };

    private void playSong(MySong song) {
        Message msg = Message.obtain();
        msg.obj = MiamPlayerMessageFactory.buildRequestChangeSong(song.getIndex(), getPlaylistId());
        App.MiamPlayerConnection.mHandler.sendMessage(msg);

        mPlaylistManager.setActivePlaylist(getPlaylistId());
    }


    /**
     * Set the selection to the currently played item
     */
    private void updateViewPosition() {
        if (App.MiamPlayer.getCurrentSong() != null
                && mPlaylistManager.getActivePlaylistId() == getPlaylistId()) {
            int pos = App.MiamPlayer.getCurrentSong().getIndex();
            mList.setSelection(pos - mSelectionOffset);
        }
    }

    public boolean isUpdateTrackPositionOnNewTrack() {
        return mUpdateTrackPositionOnNewTrack;
    }

    public void setUpdateTrackPositionOnNewTrack(
            boolean updateTrackPositionOnNewTrack, int offset) {
        this.mUpdateTrackPositionOnNewTrack = updateTrackPositionOnNewTrack;
        mSelectionOffset = offset;
    }

    @Override
    public void MessageFromMiamPlayer(MiamPlayerMessage miamPlayerMessage) {
        switch (miamPlayerMessage.getMessageType()) {
            case CURRENT_METAINFO:
                updateSongList();
                break;
            default:
                break;
        }
    }

    /**
     * Sends a request to MiamPlayer to send all songs in all active playlists.
     */
    public void RequestPlaylistSongs() {
        // If a progress is showing, do not show again!
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            return;
        }

        // Open it directly only when we got all playlists
        int requests = mPlaylistManager.requestAllPlaylistSongs();
        if (requests > 0) {
            // Start a Progressbar
            mProgressDialog = new MaterialDialog.Builder(getActivity())
                    .progress(false, requests, true)
                    .title(R.string.player_download_playlists)
                    .content(R.string.playlist_loading)
                    .show();
        } else {
            mPlaylistsSpinner.setSelection(
                    mPlaylists.indexOf(mPlaylistManager.getActivePlaylist()));
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    private void updatePlaylistSpinner() {
        List<CharSequence> arrayList = new ArrayList<>();
        for (int i = 0; i < mPlaylists.size(); i++) {
            arrayList.add(mPlaylists.get(i).getName());
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, arrayList);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPlaylistsSpinner.setAdapter(adapter);
    }

    private int getPlaylistId() {
        return mPlaylists.get(getSelectedPlaylistPosition()).getId();
    }

    private LinkedList<MySong> getSelectedPlaylistSongs() {
        return mPlaylists.get(getSelectedPlaylistPosition()).getPlaylistSongs();
    }

    private int getSelectedPlaylistPosition() {
        int pos = mPlaylistsSpinner.getSelectedItemPosition();
        if (pos == Spinner.INVALID_POSITION || pos >= mPlaylists.size()) {
            pos = mPlaylists.indexOf(mPlaylistManager.getActivePlaylist());
            mPlaylistsSpinner.setSelection(pos);
        }
        return pos;
    }
}
