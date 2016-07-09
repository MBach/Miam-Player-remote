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

package org.miamplayer.miamplayerremote.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import org.miamplayer.miamplayerremote.R;
import org.miamplayer.miamplayerremote.backend.downloader.MiamPlayerSongDownloader;
import org.miamplayer.miamplayerremote.backend.downloader.DownloadManager;
import org.miamplayer.miamplayerremote.utils.Utilities;

/**
 * Class is used for displaying the song data
 */
public class DownloaderAdapter extends ArrayAdapter<MiamPlayerSongDownloader> {

    private Context mContext;

    private DownloadManager mDownloadManager;

    public DownloaderAdapter(Context context, int resource,
            List<MiamPlayerSongDownloader> data) {
        super(context, resource, data);
        mContext = context;

        mDownloadManager = DownloadManager.getInstance();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MiamPlayerSongDownloader downloader = getItem(position);
        DownloadViewHolder downloadViewHolder;

        if (convertView == null) {
            convertView = ((Activity) mContext).getLayoutInflater()
                    .inflate(R.layout.item_download, parent, false);

            downloadViewHolder = new DownloadViewHolder();

            downloadViewHolder.title = (TextView) convertView.findViewById(R.id.tvDlTitle);
            downloadViewHolder.subtitle = (TextView) convertView.findViewById(R.id.tvDlSubtitle);
            downloadViewHolder.progress = (ProgressBar) convertView.findViewById(R.id.pbDlProgress);
            downloadViewHolder.cancel = (ImageButton) convertView.findViewById(R.id.ibCancelDl);
            downloadViewHolder.totalDownload = (TextView) convertView
                    .findViewById(R.id.tvDownloadSize);

            convertView.setTag(downloadViewHolder);
        } else {
            downloadViewHolder = (DownloadViewHolder) convertView.getTag();
        }

        convertView.setBackgroundResource(R.drawable.selector_white_orange_selected);

        downloadViewHolder.cancel.setOnClickListener(oclCancel);
        downloadViewHolder.cancel.setTag(downloader);

        downloadViewHolder.progress.setMax(100);
        downloadViewHolder.progress.setProgress((int) downloader.getDownloadStatus().getProgress());
        downloadViewHolder.title.setText(mDownloadManager.getTitleForItem(downloader));
        downloadViewHolder.subtitle.setText(mDownloadManager.getSubtitleForItem(
                downloader));

        StringBuilder sb = new StringBuilder();
        sb.append(Utilities.humanReadableBytes(downloader.getTotalDownloaded(), true));
        sb.append(" / ");
        sb.append(Utilities.humanReadableBytes(downloader.getTotalFileSize(), true));
        if (downloader.getStatus() == AsyncTask.Status.RUNNING) {
            sb.append(" (");
            sb.append(Utilities.humanReadableBytes(downloader.getDownloadSpeedPerSecond(), true));
            sb.append("/s)");
        }

        downloadViewHolder.totalDownload.setText(sb.toString());

        return convertView;
    }

    private OnClickListener oclCancel = new OnClickListener() {

        @Override
        public void onClick(View v) {
            MiamPlayerSongDownloader downloader = (MiamPlayerSongDownloader) v.getTag();

            if (downloader.getStatus() == AsyncTask.Status.RUNNING) {
                downloader.cancel(false);
                Toast.makeText(mContext, R.string.download_noti_canceled, Toast.LENGTH_SHORT)
                        .show();
            } else {
                mDownloadManager.removeDownloader(downloader.getId());
                remove(downloader);
            }

            notifyDataSetChanged();
        }
    };

    private class DownloadViewHolder {

        TextView title;

        TextView subtitle;

        TextView totalDownload;

        ProgressBar progress;

        ImageButton cancel;
    }
}
