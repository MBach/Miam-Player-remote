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

import com.afollestad.materialdialogs.MaterialDialog;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.R;
import org.miamplayer.miamplayerremote.SharedPreferencesKeys;
import org.miamplayer.miamplayerremote.backend.MiamPlayerLibraryDownloader;
import org.miamplayer.miamplayerremote.backend.database.SongSelectItem;
import org.miamplayer.miamplayerremote.backend.downloader.DownloadManager;
import org.miamplayer.miamplayerremote.backend.globalsearch.elements.DownloaderResult;
import org.miamplayer.miamplayerremote.backend.globalsearch.elements.DownloaderResult.DownloadResult;
import org.miamplayer.miamplayerremote.backend.library.LibraryDatabaseHelper;
import org.miamplayer.miamplayerremote.backend.library.LibraryQuery;
import org.miamplayer.miamplayerremote.backend.listener.OnLibraryDownloadListener;
import org.miamplayer.miamplayerremote.backend.listener.OnSongSelectFinishedListener;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessageFactory;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.MsgType;
import org.miamplayer.miamplayerremote.backend.player.MySong;
import org.miamplayer.miamplayerremote.ui.adapter.DynamicSongQueryAdapter;
import org.miamplayer.miamplayerremote.ui.interfaces.BackPressHandleable;
import org.miamplayer.miamplayerremote.ui.interfaces.RemoteDataReceiver;
import org.miamplayer.miamplayerremote.utils.Utilities;

public class LibraryFragment extends Fragment implements BackPressHandleable, RemoteDataReceiver,
        SwipeRefreshLayout.OnRefreshListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private ActionBar mActionBar;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private SwipeRefreshLayout mEmptyLibrary;

    private ListView mList;

    private LinkedList<DynamicSongQueryAdapter> mAdapters = new LinkedList<>();

    private TextView mLibraryEmptyText;

    private MaterialDialog mProgressDialog;

    private String mLastFilter = "";

    private MiamPlayerLibraryDownloader mMiamPlayerLibraryDownloader;

    private int mLibraryQueriesDone;

    private int mLibraryLevels;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the actionbar
        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if we are still connected
        if (App.MiamPlayerConnection == null || App.MiamPlayer == null
                || !App.MiamPlayerConnection.isConnected()) {
            return;
        }

        setActionBarTitle();

        if (mMiamPlayerLibraryDownloader != null) {
            createDownloadProgressDialog();
            mMiamPlayerLibraryDownloader.addOnLibraryDownloadListener(mOnLibraryDownloadListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mMiamPlayerLibraryDownloader != null) {
            mMiamPlayerLibraryDownloader
                    .removeOnLibraryDownloadListener(mOnLibraryDownloadListener);
            mProgressDialog.dismiss();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container,
                false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.library_refresh_layout);
        mEmptyLibrary = (SwipeRefreshLayout) view.findViewById(R.id.library_refresh_empty_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mEmptyLibrary.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.orange);
        mEmptyLibrary.setColorSchemeResources(R.color.orange);

        mList = (ListView) view.findViewById(R.id.library);

        mLibraryEmptyText = (TextView) mEmptyLibrary.findViewById(R.id.library_empty_txt);

        mList.setOnItemClickListener(oiclLibraryClick);
        mList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mList.setMultiChoiceModeListener(new MultiChoiceModeListener() {
            @Override
            public boolean onActionItemClicked(ActionMode mode,
                    android.view.MenuItem item) {
                SparseBooleanArray checkedPositions = mList.getCheckedItemPositions();
                final LinkedList<SongSelectItem> selectedItems = new LinkedList<>();
                final LinkedList<String> urls = new LinkedList<>();
                mLibraryQueriesDone = 0;

                for (int i = 0; i < checkedPositions.size(); ++i) {
                    int position = checkedPositions.keyAt(i);
                    if (checkedPositions.valueAt(i)) {
                        selectedItems.add(mAdapters.getLast().getItem(position));
                    }
                }

                for (SongSelectItem libraryItem : selectedItems) {
                    OnSongSelectFinishedListener listener;

                    switch (item.getItemId()) {
                        case R.id.library_context_add:

                            listener = new OnSongSelectFinishedListener() {
                                @Override
                                public void OnSongSelectFinished(LinkedList<SongSelectItem> l) {
                                    addSongsToPlaylist(l);
                                }
                            };

                            break;
                        case R.id.library_context_download:
                            listener = new OnSongSelectFinishedListener() {
                                @Override
                                public void OnSongSelectFinished(LinkedList<SongSelectItem> l) {
                                    for (SongSelectItem libItem : l) {
                                        urls.add(libItem.getUrl());
                                    }
                                    mLibraryQueriesDone++;

                                    // Have we got all queries?
                                    if (mLibraryQueriesDone == selectedItems.size() && !urls
                                            .isEmpty()) {
                                        DownloadManager.getInstance()
                                                .addJob(MiamPlayerMessageFactory
                                                        .buildDownloadSongsMessage(
                                                                MiamPlayerRemoteProtocolBuffer.DownloadItem.Urls,
                                                                urls));
                                    }

                                }
                            };

                            break;
                        default:
                            return false;
                    }
                    queryLibraryItems(libraryItem, listener);
                }
                mode.finish();
                return true;
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode,
                    android.view.Menu menu) {
                android.view.MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.library_context_menu, menu);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getActivity().getWindow()
                            .setStatusBarColor(
                                    ContextCompat.getColor(getActivity(), R.color.grey_cab_status));
                }

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode,
                    android.view.Menu menu) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getActivity().getWindow()
                            .setStatusBarColor(ContextCompat.getColor(getActivity(),
                                    R.color.actionbar_dark));
                }
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode,
                    int position, long id, boolean checked) {
            }
        });

        createRootAdapter();

        showList();

        mActionBar.setTitle("");
        mActionBar.setSubtitle("/");

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        mAdapters.clear();
    }

    private OnLibraryDownloadListener mOnLibraryDownloadListener = new OnLibraryDownloadListener() {

        @Override
        public void OnLibraryDownloadFinished(DownloaderResult result) {
            mProgressDialog.dismiss();
            mMiamPlayerLibraryDownloader = null;
            mSwipeRefreshLayout.setRefreshing(false);
            mEmptyLibrary.setRefreshing(false);

            if (result.getResult() == DownloadResult.SUCCESSFUL) {
                LibraryQuery libraryQuery = new LibraryQuery(getActivity());
                libraryQuery.openDatabase();
                libraryQuery.setLevel(0);
                DynamicSongQueryAdapter a = new DynamicSongQueryAdapter(getActivity(), libraryQuery);
                mAdapters.add(a);
                showList();
            } else {
                Utilities.ShowMessageDialog(getActivity(), R.string.library_download_error,
                        result.getMessageStringId());
            }
        }

        @Override
        public void OnProgressUpdate(long progress, int total) {
            mProgressDialog.setProgress((int) progress);
            mProgressDialog.setMaxProgress(total);
        }

        @Override
        public void OnOptimizeLibrary() {
            mProgressDialog.dismiss();

            mProgressDialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.library_please_wait)
                    .content(R.string.library_optimize)
                    .cancelable(false)
                    .progress(true, -1)
                    .show();
        }
    };

    private void createDownloadProgressDialog() {
        mProgressDialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.library_please_wait)
                .content(R.string.library_download)
                .cancelable(false)
                .progress(false, 0)
                .show();
    }

    private void createRootAdapter() {
        // Check if we have the correct MiamPlayer library. Otherwise delete it
        LibraryDatabaseHelper libraryDatabaseHelper = new LibraryDatabaseHelper();
        libraryDatabaseHelper.removeDatabaseIfFromOtherMiamPlayer();

        LibraryQuery libraryQuery = new LibraryQuery(getActivity());
        mLibraryLevels = libraryQuery.getMaxLevels();

        // Create the adapter
        if (mMiamPlayerLibraryDownloader == null && libraryDatabaseHelper.databaseExists()) {
            libraryQuery.openDatabase();
            libraryQuery.setLevel(0);
            DynamicSongQueryAdapter a = new DynamicSongQueryAdapter(getActivity(), libraryQuery);
            mAdapters.add(a);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();

        inflater.inflate(R.menu.library_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Create a listener for search change
        SearchView searchView = (SearchView) menu.findItem(
                R.id.library_menu_search).getActionView();

        final SearchView.OnQueryTextListener queryTextListener
                = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                // Set the filter text as the fragments might not yet
                // created. Only the left and right fragment from the
                // currently active is created (onCreate() called).
                // Therefore the other adapters are not yet created,
                // onCreate filters for this string given in setFilterText()
                if (!mAdapters.isEmpty()) {
                    mAdapters.getLast().getFilter().filter(newText);
                    mLastFilter = newText;

                    mLibraryEmptyText.setText(R.string.library_no_search_results);
                }
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                // Do something
                if (!mAdapters.isEmpty()) {
                    mAdapters.getLast().getFilter().filter(query);
                    mLastFilter = query;
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

    private void queryLibraryItems(SongSelectItem libraryItem,
            OnSongSelectFinishedListener onSongSelectFinishedListener) {

        if (libraryItem.getLevel() == mLibraryLevels-1) {
            LinkedList<SongSelectItem> result = new LinkedList<>();
            result.add(libraryItem);
            onSongSelectFinishedListener.OnSongSelectFinished(result);
        } else {
            LibraryQuery libraryQuery = new LibraryQuery(getActivity());
            libraryQuery.openDatabase();
            libraryQuery.setLevel(mLibraryLevels-1);
            libraryQuery.setSelection(libraryItem.getSelection());
            libraryQuery.addOnLibrarySelectFinishedListener(onSongSelectFinishedListener);
            libraryQuery.selectDataAsync();
        }
    }

    private void setActionBarTitle() {
        MySong currentSong = App.MiamPlayer.getCurrentSong();
        if (currentSong == null) {
            mActionBar.setTitle(getString(R.string.player_nosong));
        } else {
            mActionBar.setTitle(currentSong.getArtist() + " / " + currentSong.getTitle());
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mList.setFastScrollEnabled(true);
        mList.setTextFilterEnabled(true);
        mList.setSelector(new ColorDrawable(ContextCompat.getColor(getActivity(), android.R.color.transparent)));
        mList.setOnItemClickListener(oiclLibraryClick);
        mList.setDivider(null);
        mList.setDividerHeight(0);

        mList.setEmptyView(mEmptyLibrary);
    }

    @Override
    public void MessageFromMiamPlayer(MiamPlayerMessage miamPlayerMessage) {
        switch (miamPlayerMessage.getMessageType()) {
            case CURRENT_METAINFO:
                setActionBarTitle();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onBackPressed() {
        // When we have only one item left, just use the normal back behavior
        if (mAdapters.size() <= 1) {
            return false;
        }

        // Remove the last element and show the new list
        mAdapters.removeLast();
        showList();

        return true;
    }

    /**
     * Show the last element in the list of adapters
     */
    private void showList() {
        if (mAdapters.isEmpty() || mAdapters.getLast().isEmpty()) {
            mList.setEmptyView(mEmptyLibrary);
        } else {
            DynamicSongQueryAdapter adapter = mAdapters.getLast();
            adapter.getFilter().filter(mLastFilter);
            mList.setAdapter(adapter);
            if (adapter.isEmpty()) {
                mActionBar.setSubtitle("/ ");
            } else {
                SongSelectItem item = adapter.getItem(0);

                StringBuilder sb = new StringBuilder();
                sb.append("/ ");
                for (int i=0;i<mAdapters.size()-1;i++) {
                    sb.append(item.getSelection()[i]);
                    if (i<mAdapters.size()-2)
                        sb.append(" / ");
                }
                mActionBar.setSubtitle(sb.toString());
            }
        }
    }

    private void addSongsToPlaylist(LinkedList<SongSelectItem> l) {
        Message msg = Message.obtain();
        LinkedList<String> urls = new LinkedList<>();
        for (SongSelectItem item : l) {
            urls.add(item.getUrl());
        }

        msg.obj = MiamPlayerMessageFactory.buildInsertUrl(
                App.MiamPlayer.getPlaylistManager().getActivePlaylistId(), urls);

        App.MiamPlayerConnection.mHandler.sendMessage(msg);

        String text = getActivity().getResources().getQuantityString(R.plurals.songs_added,
                urls.size(), urls.size());
        Toast.makeText(getActivity(),
                text,
                Toast.LENGTH_SHORT).show();
    }

    private OnItemClickListener oiclLibraryClick = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            SongSelectItem item = mAdapters.getLast().getItem(position);

            if (item.getLevel() == mLibraryLevels-1) {
                Message msg = Message.obtain();
                LinkedList<String> urls = new LinkedList<>();
                urls.add(item.getUrl());
                msg.obj = MiamPlayerMessageFactory.buildInsertUrl(
                        App.MiamPlayer.getPlaylistManager().getActivePlaylistId(), urls);
                App.MiamPlayerConnection.mHandler.sendMessage(msg);

                String text = getActivity().getResources().getQuantityString(R.plurals.songs_added,
                        1, 1);
                Toast.makeText(getActivity(),
                        text,
                        Toast.LENGTH_SHORT).show();
            } else {
                LibraryQuery libraryQuery = new LibraryQuery(getActivity());
                libraryQuery.openDatabase();
                libraryQuery.setLevel(mAdapters.size());
                libraryQuery.setSelection(item.getSelection());
                mAdapters.add(new DynamicSongQueryAdapter(getActivity(), libraryQuery));
                showList();
            }
        }
    };

    @Override
    public void onRefresh() {
        mAdapters.clear();
        showList();

        mMiamPlayerLibraryDownloader = new MiamPlayerLibraryDownloader(getActivity());
        mMiamPlayerLibraryDownloader.addOnLibraryDownloadListener(
                mOnLibraryDownloadListener);
        mMiamPlayerLibraryDownloader.startDownload(MiamPlayerMessage
                .getMessage(MsgType.GET_LIBRARY));

        createDownloadProgressDialog();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SharedPreferencesKeys.SP_LIBRARY_GROUPING)
                || key.equals(SharedPreferencesKeys.SP_LIBRARY_SORTING)) {
            mAdapters.clear();

            createRootAdapter();
            showList();
        }
    }
}
