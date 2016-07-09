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

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import java.lang.reflect.Field;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.R;
import org.miamplayer.miamplayerremote.backend.MiamPlayer;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.MsgType;
import org.miamplayer.miamplayerremote.ui.adapter.PlayerPageAdapter;
import org.miamplayer.miamplayerremote.ui.fragments.playerpages.ConnectionFragment;
import org.miamplayer.miamplayerremote.ui.fragments.playerpages.PlayerPageFragment;
import org.miamplayer.miamplayerremote.ui.fragments.playerpages.SongDetailFragment;
import org.miamplayer.miamplayerremote.ui.interfaces.BackPressHandleable;
import org.miamplayer.miamplayerremote.ui.interfaces.RemoteDataReceiver;
import org.miamplayer.miamplayerremote.ui.widgets.SlidingTabLayout;

public class PlayerFragment extends Fragment implements BackPressHandleable, RemoteDataReceiver {

    private ImageButton mBtnNext;

    private ImageButton mBtnPrev;

    private ImageButton mBtnPlayPause;

    private ActionBar mActionBar;

    private SlidingTabLayout mTabs;

    private PlayerPageFragment mPlayerPageFragment;

    private SongDetailFragment mSongDetailFragment;

    private ConnectionFragment mConnectionFragment;

    private ViewPager myPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Get the actionbar
        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        mActionBar.setTitle(R.string.player_playlist);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player,
                container, false);

        mPlayerPageFragment = new PlayerPageFragment();

        mSongDetailFragment = new SongDetailFragment();

        mConnectionFragment = new ConnectionFragment();

        PlayerPageAdapter playerPageAdapter;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            playerPageAdapter = new PlayerPageAdapter(getActivity(), getChildFragmentManager());
        } else {
            playerPageAdapter = new PlayerPageAdapter(getActivity(), getFragmentManager());
        }
        playerPageAdapter.addFragment(mPlayerPageFragment);
        playerPageAdapter.addFragment(mSongDetailFragment);
        playerPageAdapter.addFragment(mConnectionFragment);
        myPager = (ViewPager) view.findViewById(R.id.player_pager);
        myPager.setAdapter(playerPageAdapter);
        myPager.setCurrentItem(0);

        // Get the Views
        mBtnNext = (ImageButton) view.findViewById(R.id.btnNext);
        mBtnPrev = (ImageButton) view.findViewById(R.id.btnPrev);
        mBtnPlayPause = (ImageButton) view.findViewById(R.id.btnPlaypause);

        // Set the onclicklistener for the buttons
        mBtnNext.setOnClickListener(oclControl);
        mBtnPrev.setOnClickListener(oclControl);
        mBtnPlayPause.setOnClickListener(oclControl);
        mBtnPlayPause.setOnLongClickListener(olclControl);

        // Initialize interface
        stateChanged();
        metadataChanged();

        mTabs = (SlidingTabLayout) getActivity().findViewById(R.id.tabs);

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        myPager.setCurrentItem(0);

        mTabs.setDistributeEvenly(true);
        mTabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return ContextCompat.getColor(getActivity(), R.color.actionbar_dark);
            }
        });
        mTabs.setTextViewColor(ContextCompat.getColor(getActivity(), R.color.white));
        mTabs.setViewPager(myPager);
        mTabs.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        mTabs.setVisibility(View.GONE);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);

        } catch (NoSuchFieldException ignored) {
        } catch (IllegalAccessException ignored) {
        }
    }

    @Override
    public void MessageFromMiamPlayer(MiamPlayerMessage miamPlayerMessage) {
        switch (miamPlayerMessage.getMessageType()) {
            case PLAY:
            case PAUSE:
            case STOP:
                stateChanged();
                break;
            case CURRENT_METAINFO:
                metadataChanged();
            default:
                break;
        }

        if (mPlayerPageFragment.isAdded()) {
            mPlayerPageFragment.MessageFromMiamPlayer(miamPlayerMessage);
        }
        if (mSongDetailFragment.isAdded()) {
            mSongDetailFragment.MessageFromMiamPlayer(miamPlayerMessage);
        }
        if (mConnectionFragment.isAdded()) {
            mConnectionFragment.MessageFromMiamPlayer(miamPlayerMessage);
        }
    }

    private void metadataChanged() {
        // ActionBar shows the current playlist
        if (App.MiamPlayer.getPlaylistManager().getActivePlaylist() != null) {
            mActionBar.setSubtitle(
                    App.MiamPlayer.getPlaylistManager().getActivePlaylist().getName());
        }
    }

    private void stateChanged() {
        // display play / pause image
        if (App.MiamPlayer.getState() == MiamPlayer.State.PLAY) {
            mBtnPlayPause.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_media_pause));
        } else {
            mBtnPlayPause.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_media_play));
        }
    }

    private OnClickListener oclControl = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Message msg = Message.obtain();

            switch (v.getId()) {
                case R.id.btnNext:
                    msg.obj = MiamPlayerMessage.getMessage(MsgType.NEXT);
                    break;
                case R.id.btnPrev:
                    msg.obj = MiamPlayerMessage.getMessage(MsgType.PREVIOUS);
                    break;
                case R.id.btnPlaypause:
                    msg.obj = MiamPlayerMessage.getMessage(MsgType.PLAYPAUSE);
                    break;
                default:
                    break;
            }
            // Send the request to the thread
            if (msg.obj != null) {
                App.MiamPlayerConnection.mHandler.sendMessage(msg);
            }
        }
    };

    private OnLongClickListener olclControl = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            boolean ret = false;
            Message msg = Message.obtain();

            switch (v.getId()) {
                case R.id.btnPlaypause:
                    Toast.makeText(getActivity(), R.string.player_stop_after_current,
                            Toast.LENGTH_SHORT).show();
                    msg.obj = MiamPlayerMessage.getMessage(MsgType.STOP_AFTER);
                    ret = true;
                    break;
                default:
                    break;
            }

            App.MiamPlayerConnection.mHandler.sendMessage(msg);
            return ret;
        }
    };

    @Override
    public boolean onBackPressed() {
        return false;
    }
}
