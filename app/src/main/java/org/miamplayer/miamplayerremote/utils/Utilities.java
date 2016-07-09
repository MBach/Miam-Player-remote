package org.miamplayer.miamplayerremote.utils;

import com.afollestad.materialdialogs.MaterialDialog;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Locale;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.R;
import org.miamplayer.miamplayerremote.backend.mediasession.MiamPlayerMediaSessionNotification;
import org.miamplayer.miamplayerremote.ui.MainActivity;

public class Utilities {

    @SuppressLint("DefaultLocale")
    public static String PrettyTime(int seconds) {
        // last.fm sometimes gets the track length wrong, so you end up with
        // negative times.
        seconds = Math.abs(seconds);

        int hours = seconds / (60 * 60);
        int minutes = (seconds / 60) % 60;
        seconds %= 60;

        String ret;
        if (hours > 0) {
            ret = String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            ret = String.format("%d:%02d", minutes, seconds);
        }

        return ret;
    }

    /**
     * Show a simple designed message dialog
     *
     * @param context In which context will the dialog be displayed?
     * @param title   The resource id of the message dialog title.
     * @param message The resource id of the message
     * @return The Dialog object
     */
    public static Dialog ShowMessageDialog(Context context, int title, int message) {
        return ShowMessageDialog(context, context.getString(title), context.getString(message),
                false);
    }

    /**
     * Show a simple designed message dialog
     *
     * @param context In which context will the dialog be displayed?
     * @param title   The resource id of the message dialog title.
     * @param message The resource id of the message
     * @return The Dialog object
     */
    public static Dialog ShowHtmlMessageDialog(Context context, int title, int message) {
        return ShowMessageDialog(context, context.getString(title), context.getString(message),
                true);
    }

    /**
     * Show a simple designed message dialog
     *
     * @param context In which context will the dialog be displayed?
     * @param title   The message string dialog title.
     * @param message The string of the message
     * @return The Dialog object
     */
    public static Dialog ShowMessageDialog(Context context, String title, String message,
            boolean hasHtml) {

        String content;
        if (hasHtml) {
            content = Html.fromHtml(message).toString();
        } else {
            content = message;
        }

        return new MaterialDialog.Builder(context)
                .title(title)
                .content(content)
                .negativeText(R.string.dialog_close)
                .show();
    }

    public static byte[] ToIPByteArray(int addr) {
        return new byte[]{(byte) addr, (byte) (addr >>> 8), (byte) (addr >>> 16),
                (byte) (addr >>> 24)};
    }

    public static InetAddress ToInetAddress(int addr) {
        try {
            return InetAddress.getByAddress(ToIPByteArray(addr));
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * Get the free space on the external storage device (like sd card)
     *
     * @return The free space in byte
     */
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static double getFreeSpaceExternal() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return (double) stat.getAvailableBlocks() * (double) stat.getBlockSize();
        } else {
            return (double) stat.getAvailableBlocksLong() * (double) stat.getBlockSizeLong();
        }
    }

    /**
     * Get the free space on the internal storage device
     *
     * @return The free space in byte
     */
    public static double getFreeSpaceInternal() {
        StatFs stat = new StatFs(App.getApp().getFilesDir().getPath());
        return (double) stat.getAvailableBlocksLong() * (double) stat.getBlockSizeLong();
    }

    /**
     * Is the device connceted to a wifi network?
     *
     * @return true if connected to a wifi network
     */
    @SuppressWarnings("deprecation")
    public static boolean onWifi() {
        ConnectivityManager connManager = (ConnectivityManager) App.getApp()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean onWifi = false;

        Network[] networks = connManager.getAllNetworks();
        NetworkInfo networkInfo;
        for (Network mNetwork : networks) {
            networkInfo = connManager.getNetworkInfo(mNetwork);
            if (networkInfo.getState().equals(NetworkInfo.State.CONNECTED) &&
                    networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                onWifi = true;
                break;
            }
        }

        return onWifi;
    }

    /**
     * Convert bytes in a human readable format.
     *
     * @param bytes The byte count
     * @param iec   false for KB, false for KiB
     * @return The human readable file size
     */
    public static String humanReadableBytes(long bytes, boolean iec) {
        // Are we using xB or xiB?
        int byteUnit = iec ? 1024 : 1000;
        float newBytes = bytes;
        int exp = 0;

        // Calculate the file size in the best readable way
        while (newBytes > byteUnit) {
            newBytes = newBytes / byteUnit;
            exp++;
        }

        // What prefix do we have to use?
        String prefix = "";
        if (exp > 0) {
            prefix = (iec ? " KMGTPE" : " kMGTPE").charAt(exp) + ((iec) ? "i" : "");
        }

        // Return a human readable String
        return String.format(Locale.US, "%.2f %sB", newBytes, prefix);
    }

    /**
     * Checks if MiamPlayer Remote is connected to an instance
     * of MiamPlayer.
     *
     * @return true if connected, false otherwise
     */
    public static boolean isRemoteConnected() {
        return App.MiamPlayerConnection != null && App.MiamPlayerConnection.isConnected();
    }

    /**
     * This method removes all illegal characters in a file name.
     *
     * @param str The string containing a file or folder name
     * @return A string that is a vaild file name
     */
    public static String removeInvalidFileCharacters(String str) {
        String illegal = "[\\\\~#%&*{}/:<>?|\\\"-]";
        return str.replaceAll(illegal, "");
    }

    public static PendingIntent getMiamPlayerRemotePendingIntent(Context context) {
        // Set the result intent
        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        resultIntent.putExtra(MiamPlayerMediaSessionNotification.EXTRA_NOTIFICATION_ID, -1);

        // Create a TaskStack, so the app navigates correctly backwards
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        return stackBuilder.getPendingIntent(9912, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Nullable
    public static Inet4Address getIp4Address() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address
                            && !address.isLoopbackAddress()) {
                        return (Inet4Address) address;
                    }
                }
            }
        } catch (SocketException e) {
            return null;
        }

        return null;
    }
}
