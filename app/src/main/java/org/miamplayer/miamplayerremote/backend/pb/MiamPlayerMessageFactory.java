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

package org.miamplayer.miamplayerremote.backend.pb;

import java.util.LinkedList;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.DownloadItem;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.Message;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.MsgType;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.Repeat;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.RequestChangeSong;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.RequestConnect;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.RequestDownloadSongs;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.RequestInsertUrls;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.RequestPlaylistSongs;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.RequestRateSong;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.RequestRemoveSongs;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.RequestSetTrackPosition;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.RequestSetVolume;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.ResponseSongOffer;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.Shuffle;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.ShuffleMode;
import org.miamplayer.miamplayerremote.backend.player.MySong;

/**
 * Creates the protocol buffer messages
 */
public class MiamPlayerMessageFactory {

    private MiamPlayerMessageFactory() {
    }

    /**
     * Create a song offer response
     *
     * @return ResponseSongOffer Builder for protocol buffer message
     */
    public static MiamPlayerMessage buildSongOfferResponse(boolean accepted) {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.SONG_OFFER_RESPONSE);
        ResponseSongOffer.Builder offer = msg.getResponseSongOfferBuilder();
        offer.setAccepted(accepted);
        return new MiamPlayerMessage(msg);
    }

    public static MiamPlayerMessage buildDownloadSongsMessage(DownloadItem downloadItem) {
        return buildDownloadSongsMessage(downloadItem, -1, null);
    }

    public static MiamPlayerMessage buildDownloadSongsMessage(DownloadItem downloadItem, int playlistId) {
        return buildDownloadSongsMessage(downloadItem, playlistId, null);
    }

    public static MiamPlayerMessage buildDownloadSongsMessage(DownloadItem downloadItem, LinkedList<String> urls) {
        return buildDownloadSongsMessage(downloadItem, -1, urls);
    }

    /**
     * Create a download song message
     *
     * @return The built request
     */
    public static MiamPlayerMessage buildDownloadSongsMessage(DownloadItem downloadItem, int playlistId, LinkedList<String> urls) {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.DOWNLOAD_SONGS);
        RequestDownloadSongs.Builder request = msg.getRequestDownloadSongsBuilder();

        request.setPlaylistId(playlistId);
        request.setDownloadItem(downloadItem);
        if (urls != null && !urls.isEmpty())
            request.addAllUrls(urls);

        return new MiamPlayerMessage(msg);
    }

    /**
     * Create the volume specific message
     *
     * @return the Volume message part
     */
    public static MiamPlayerMessage buildVolumeMessage(int volume) {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.SET_VOLUME);

        RequestSetVolume.Builder requestSetVolume = msg.getRequestSetVolumeBuilder();
        requestSetVolume.setVolume(volume);

        return new MiamPlayerMessage(msg);
    }

    /**
     * Create the connect specific message
     *
     * @return the connect message part
     */
    public static MiamPlayerMessage buildConnectMessage(String ip, int port, int authCode,
                                                        boolean getPlaylistSongs, boolean isDownloader) {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.CONNECT);

        RequestConnect.Builder requestConnect = msg.getRequestConnectBuilder();

        requestConnect.setAuthCode(authCode);
        requestConnect.setSendPlaylistSongs(getPlaylistSongs);
        requestConnect.setDownloader(isDownloader);

        MiamPlayerMessage miamPlayerMessage = new MiamPlayerMessage(msg);
        miamPlayerMessage.setIp(ip);
        miamPlayerMessage.setPort(port);

        return miamPlayerMessage;
    }

    /**
     * Build shuffle Message
     *
     * @return The created element
     */
    public static MiamPlayerMessage buildShuffle() {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.SHUFFLE);

        Shuffle.Builder shuffle = msg.getShuffleBuilder();

        switch (App.MiamPlayer.getShuffleMode()) {
            case OFF:
                shuffle.setShuffleMode(ShuffleMode.Shuffle_Off);
                break;
            case ALL:
                shuffle.setShuffleMode(ShuffleMode.Shuffle_All);
                break;
            case INSIDE_ALBUM:
                shuffle.setShuffleMode(ShuffleMode.Shuffle_InsideAlbum);
                break;
            case ALBUMS:
                shuffle.setShuffleMode(ShuffleMode.Shuffle_Albums);
                break;
        }
        return new MiamPlayerMessage(msg);
    }

    /**
     * Build Repeat Message
     *
     * @return The created element
     */
    public static MiamPlayerMessage buildRepeat() {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.REPEAT);

        Repeat.Builder repeat = msg.getRepeatBuilder();

        switch (App.MiamPlayer.getRepeatMode()) {
            case OFF:
                repeat.setRepeatMode(MiamPlayerRemoteProtocolBuffer.RepeatMode.Repeat_Off);
                break;
            case TRACK:
                repeat.setRepeatMode(MiamPlayerRemoteProtocolBuffer.RepeatMode.Repeat_Track);
                break;
            case ALBUM:
                repeat.setRepeatMode(MiamPlayerRemoteProtocolBuffer.RepeatMode.Repeat_Album);
                break;
            case PLAYLIST:
                repeat.setRepeatMode(MiamPlayerRemoteProtocolBuffer.RepeatMode.Repeat_Playlist);
                break;
        }
        return new MiamPlayerMessage(msg);
    }

    /**
     * Request all Songs in current playlist
     *
     * @return The Builder for the Message
     */
    public static MiamPlayerMessage buildRequestPlaylistSongs(int playlistId) {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.REQUEST_PLAYLIST_SONGS);

        RequestPlaylistSongs.Builder requestPlaylistSongs = msg.getRequestPlaylistSongsBuilder();

        requestPlaylistSongs.setId(playlistId);

        return new MiamPlayerMessage(msg);
    }

    /**
     * Request all Songs in current playlist
     *
     * @return The Builder for the Message
     */
    public static MiamPlayerMessage buildRequestChangeSong(int songIndex, int playlistId) {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.CHANGE_SONG);

        RequestChangeSong.Builder request = msg.getRequestChangeSongBuilder();

        request.setSongIndex(songIndex);
        request.setPlaylistId(playlistId);

        return new MiamPlayerMessage(msg);
    }

    /**
     * Request to set the track position
     *
     * @return The MiamPlayer message
     */
    public static MiamPlayerMessage buildTrackPosition(int position) {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.SET_TRACK_POSITION);

        RequestSetTrackPosition.Builder request = msg.getRequestSetTrackPositionBuilder();
        request.setPosition(position);

        return new MiamPlayerMessage(msg);
    }

    /**
     * Rate the current track
     *
     * @param rating the rating from 0 to 1. Multiply five times for star count
     * @return the MiamPlayer Message
     */
    public static MiamPlayerMessage buildRateTrack(float rating) {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.RATE_SONG);

        RequestRateSong.Builder request = msg.getRequestRateSongBuilder();
        request.setRating(rating);

        return new MiamPlayerMessage(msg);
    }

    /**
     * Inserts a song into given playlist
     *
     * @param playistId The id of the playlist
     * @param urls      The urls to the items
     * @return the MiamPlayer Message
     */
    public static MiamPlayerMessage buildInsertUrl(int playistId, LinkedList<String> urls) {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.INSERT_URLS);

        RequestInsertUrls.Builder insertUrls = msg.getRequestInsertUrlsBuilder();
        insertUrls.setPlaylistId(playistId);
        for (String url : urls) {
            insertUrls.addUrls(url);
        }

        return new MiamPlayerMessage(msg);
    }

    public static MiamPlayerMessage buildInsertSongs(int playistId,
                                                     LinkedList<MiamPlayerRemoteProtocolBuffer.SongMetadata> songs) {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.INSERT_URLS);

        RequestInsertUrls.Builder insertSongs = msg.getRequestInsertUrlsBuilder();
        insertSongs.setPlaylistId(playistId);
        insertSongs.addAllSongs(songs);

        return new MiamPlayerMessage(msg);
    }

    public static MiamPlayerMessage buildRemoveSongFromPlaylist(int playlistId, MySong song) {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.REMOVE_SONGS);

        RequestRemoveSongs.Builder removeItems = msg.getRequestRemoveSongsBuilder();
        removeItems.setPlaylistId(playlistId);
        removeItems.addSongs(song.getIndex());

        return new MiamPlayerMessage(msg);
    }

    public static MiamPlayerMessage buildRemoveMultipleSongsFromPlaylist(int playlistId,
                                                                         LinkedList<MySong> songs) {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.REMOVE_SONGS);

        RequestRemoveSongs.Builder removeItems = msg.getRequestRemoveSongsBuilder();
        removeItems.setPlaylistId(playlistId);

        for (MySong s : songs) {
            removeItems.addSongs(s.getIndex());
        }

        return new MiamPlayerMessage(msg);
    }

    public static MiamPlayerMessage buildClosePlaylist(int playlistId) {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.CLOSE_PLAYLIST);

        MiamPlayerRemoteProtocolBuffer.RequestClosePlaylist.Builder requestClosePlaylist = msg
                .getRequestClosePlaylistBuilder();
        requestClosePlaylist.setPlaylistId(playlistId);

        return new MiamPlayerMessage(msg);
    }

    public static MiamPlayerMessage buildGlobalSearch(String query) {
        Message.Builder msg = MiamPlayerMessage.getMessageBuilder(MsgType.GLOBAL_SEARCH);
        MiamPlayerRemoteProtocolBuffer.RequestGlobalSearch.Builder requestGlobalSearch = msg.
                getRequestGlobalSearchBuilder();

        requestGlobalSearch.setQuery(query);

        return new MiamPlayerMessage(msg);
    }
}
