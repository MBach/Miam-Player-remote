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

package org.miamplayer.miamplayerremote.ui.settings;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.miamplayer.miamplayerremote.R;

public class DefaultDirChooser {

    private Context mContext;

    private LinkedList<DirectorySelectedListener> dirListenerList
            = new LinkedList<>();

    public DefaultDirChooser(Context context) {
        mContext = context;
    }

    public void showAvailableDirectories() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);

        dialog.setTitle(R.string.file_dialog_set_dir);
        final List<String> directories = getDirectories();
        dialog.setItems(directories.toArray(new CharSequence[directories.size()]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fireDirectorySelectedEvent(directories.get(which));
                    }
                });

        dialog.show();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private LinkedList<String> getDirectories() {
        LinkedList<String> directories = new LinkedList<>();

        File[] defaultDirs = ContextCompat.getExternalFilesDirs(mContext, Environment.DIRECTORY_MUSIC);
        for (File f : defaultDirs) {
            if (f != null)
                directories.add(f.toString());
        }

        String publicMusicDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString();
        if (canWriteToExternalStorage(publicMusicDir)) {
            directories.add(publicMusicDir);
        }

        if (canWriteToExternalStorage(
                Environment.getExternalStorageDirectory().getAbsolutePath())) {
            directories.add(mContext.getString(R.string.file_dialog_custom_paths_available));
        }
        return directories;
    }

    private boolean canWriteToExternalStorage(String path) {
        // Check the external store state
        File checkFile = new File(path, "/MiamPlayerTestFile.CheckIfWritable");
        try {
            if (checkFile.createNewFile()) {
                checkFile.delete();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void addDirectoryListener(DirectorySelectedListener listener) {
        dirListenerList.add(listener);
    }

    private void fireDirectorySelectedEvent(final String directory) {
        for (DirectorySelectedListener l : dirListenerList) {
            l.directorySelected(directory);
        }
    }

    public interface DirectorySelectedListener {

        void directorySelected(String dir);
    }
}
