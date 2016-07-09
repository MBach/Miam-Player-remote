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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import javax.jmdns.ServiceInfo;

import org.miamplayer.miamplayerremote.R;

/**
 * Class is used for displaying the song data
 */
public class CustomMiamPlayersAdapter extends ArrayAdapter<ServiceInfo> {

    private Context mContext;

    private List<ServiceInfo> mData;

    public CustomMiamPlayersAdapter(Context context, int resource,
                                    List<ServiceInfo> data) {
        super(context, resource, data);
        mContext = context;
        mData = data;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MiamPlayerViewHolder miamPlayerViewHolder;

        if (convertView == null) {
            convertView = ((Activity) mContext).getLayoutInflater()
                    .inflate(R.layout.item_miamplayer, parent, false);

            miamPlayerViewHolder = new MiamPlayerViewHolder();
            miamPlayerViewHolder.textViewHost = (TextView) convertView
                    .findViewById(R.id.tvClItemHost);
            miamPlayerViewHolder.textViewIp = (TextView) convertView.findViewById(R.id.tvClIp);

            convertView.setTag(miamPlayerViewHolder);
        } else {
            miamPlayerViewHolder = (MiamPlayerViewHolder) convertView.getTag();
        }

        miamPlayerViewHolder.textViewHost.setText(mData.get(position).getName());
        miamPlayerViewHolder.textViewIp.setText(
                mData.get(position).getInet4Addresses()[0].toString().split("/")[1] + ":" + mData
                        .get(position).getPort());

        return convertView;
    }

    private class MiamPlayerViewHolder {

        public TextView textViewHost;

        public TextView textViewIp;
    }

}