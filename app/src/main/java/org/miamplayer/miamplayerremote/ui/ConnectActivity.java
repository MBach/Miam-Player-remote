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

package org.miamplayer.miamplayerremote.ui;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.miamplayer.miamplayerremote.App;
import org.miamplayer.miamplayerremote.R;
import org.miamplayer.miamplayerremote.SharedPreferencesKeys;
import org.miamplayer.miamplayerremote.backend.MiamPlayer;
import org.miamplayer.miamplayerremote.backend.MiamPlayerService;
import org.miamplayer.miamplayerremote.backend.downloader.DownloadManager;
import org.miamplayer.miamplayerremote.backend.mdns.MiamPlayerMDnsDiscovery;
import org.miamplayer.miamplayerremote.backend.mediasession.MiamPlayerMediaSessionNotification;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerMessage;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.MsgType;
import org.miamplayer.miamplayerremote.backend.pb.MiamPlayerRemoteProtocolBuffer.ReasonDisconnect;
import org.miamplayer.miamplayerremote.ui.adapter.CustomMiamPlayersAdapter;
import org.miamplayer.miamplayerremote.ui.dialogs.CrashReportDialog;
import org.miamplayer.miamplayerremote.ui.settings.MiamPlayerSettings;
import org.miamplayer.miamplayerremote.utils.Utilities;

/**
 * The connect dialog
 */
public class ConnectActivity extends AppCompatActivity {

    private final int ANIMATION_DURATION = 2000;

    private final int ID_PLAYER_DIALOG = 1;

    private final int ID_SETTINGS = 2;

    private final int ID_PERMISSION_REQUEST = 3;

    public final static int RESULT_DISCONNECT = 1;

    public final static int RESULT_QUIT = 2;

    private Button mBtnConnect;

    private ImageButton mBtnMiamPlayer;

    private AutoCompleteTextView mEtIp;

    MaterialDialog mPdConnect;

    private SharedPreferences mSharedPref;

    private ConnectActivityHandler mHandler = new ConnectActivityHandler(this);

    private int mAuthCode = 0;

    private MiamPlayerMDnsDiscovery mMiamPlayerMDns;

    private AlphaAnimation mAlphaDown;

    private AlphaAnimation mAlphaUp;

    private boolean mAnimationCancel;

    private Intent mServiceIntent;

    private boolean doAutoConnect = true;

    private Set<String> mKnownIps;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_connectdialog);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mKnownIps = mSharedPref
                .getStringSet(SharedPreferencesKeys.SP_KNOWN_IP, new LinkedHashSet<String>());

        initializeUi();

        // Check if we got a stack trace
        CrashReportDialog crashReportDialog = new CrashReportDialog(this);
        crashReportDialog.showDialogIfTraceExists();

        if (doAutoConnect && crashReportDialog.hasTrace()) {
            doAutoConnect = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check if we are currently connected, then open the player dialog
        if ((mPdConnect == null || !mPdConnect.isShowing())
                && App.MiamPlayerConnection != null
                && App.MiamPlayerConnection.isConnected()) {
            showPlayerDialog();
            return;
        }

        // mDNS Discovery
        mMiamPlayerMDns = new MiamPlayerMDnsDiscovery(mHandler);

        // Check if Autoconnect is enabled
        if (mSharedPref.getBoolean(SharedPreferencesKeys.SP_KEY_AC, false) && doAutoConnect) {
            // Post delayed, so the service has time to start
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    connect();
                }
            }, 250);

        } else {
            mMiamPlayerMDns.discoverServices();
        }
        doAutoConnect = true;

        // Remove still active notifications
        NotificationManager mNotificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(MiamPlayerMediaSessionNotification.NOTIFIFCATION_ID);
        mNotificationManager.cancel(DownloadManager.NOTIFICATION_ID_DOWNLOADS);
        mNotificationManager.cancel(DownloadManager.NOTIFICATION_ID_DOWNLOADS_FINISHED);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMiamPlayerMDns != null) {
            mMiamPlayerMDns.stopServiceDiscovery();
            mBtnMiamPlayer.clearAnimation();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_connectdialog);

        initializeUi();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent settingsIntent = new Intent(this, MiamPlayerSettings.class);
                startActivity(settingsIntent);
                doAutoConnect = false;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();

        MenuInflater inf = getMenuInflater();
        inf.inflate(R.menu.connectdialog_menu, menu);

        return true;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        // First time called? Show an info screen
        if (mSharedPref.getBoolean(SharedPreferencesKeys.SP_FIRST_CALL, true)) {
            mSharedPref.edit().putBoolean(SharedPreferencesKeys.SP_FIRST_CALL, false).apply();

            // Show the info screen
            showFirstTimeScreen();
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
            new MaterialDialog.Builder(this)
                    .title(R.string.permissions_required_title)
                    .content(R.string.permissions_required_text)
                    .negativeText(R.string.dialog_continue)
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            ActivityCompat.requestPermissions(ConnectActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.READ_PHONE_STATE},
                                    ID_PERMISSION_REQUEST);
                        }
                    })
                    .show();
        }
    }

    private void initializeUi() {
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        // Get the Layoutelements
        mBtnConnect = (Button) findViewById(R.id.btnConnect);
        mBtnConnect.setOnClickListener(oclConnect);
        mBtnConnect.requestFocus();

        mBtnMiamPlayer = (ImageButton) findViewById(R.id.btnMiamPlayerIcon);
        mBtnMiamPlayer.setOnClickListener(oclMiamPlayer);

        // Setup the animation for the MiamPlayer icon
        mAlphaDown = new AlphaAnimation(1.0f, 0.3f);
        mAlphaUp = new AlphaAnimation(0.3f, 1.0f);
        mAlphaDown.setDuration(ANIMATION_DURATION);
        mAlphaUp.setDuration(ANIMATION_DURATION);
        mAlphaDown.setFillAfter(true);
        mAlphaUp.setFillAfter(true);
        mAlphaUp.setAnimationListener(mAnimationListener);
        mAlphaDown.setAnimationListener(mAnimationListener);
        mAnimationCancel = false;

        // Ip and Autoconnect
        mEtIp = (AutoCompleteTextView) findViewById(R.id.etIp);
        mEtIp.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mEtIp.setThreshold(3);

        // Get old ip and auto-connect from shared preferences
        mEtIp.setText(mSharedPref.getString(SharedPreferencesKeys.SP_KEY_IP, ""));
        mEtIp.setSelection(mEtIp.length());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.select_dialog_item, mKnownIps.toArray(new String[0]));
        mEtIp.setAdapter(adapter);

        // Get the last auth code
        mAuthCode = mSharedPref.getInt(SharedPreferencesKeys.SP_LAST_AUTH_CODE, 0);
    }

    private OnClickListener oclConnect = new OnClickListener() {

        @Override
        public void onClick(View v) {
            // And connect
            connect();
        }
    };

    private OnClickListener oclMiamPlayer = new OnClickListener() {

        @Override
        public void onClick(View v) {
            // Only when we have Jelly Bean or higher
            if (!mMiamPlayerMDns.getServices().isEmpty()) {
                mAnimationCancel = true;
                final MaterialDialog.Builder builder = new MaterialDialog.Builder(
                        ConnectActivity.this);

                builder.title(R.string.connectdialog_services);
                CustomMiamPlayersAdapter adapter = new CustomMiamPlayersAdapter(
                        ConnectActivity.this,
                        R.layout.item_miamplayer, mMiamPlayerMDns.getServices());
                builder.adapter(adapter, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i,
                            CharSequence charSequence) {
                        materialDialog.dismiss();
                        // The 'which' argument contains the index position
                        // of the selected item
                        ServiceInfo service = mMiamPlayerMDns.getServices().get(i);
                        // Insert the host
                        String ip = service.getInet4Addresses()[0].toString().split("/")[1];
                        mEtIp.setText(ip);

                        // Update the port
                        SharedPreferences.Editor editor = mSharedPref.edit();
                        editor.putString(SharedPreferencesKeys.SP_KEY_PORT,
                                String.valueOf(service.getPort()));
                        editor.apply();
                        connect();
                    }
                });
                builder.negativeText(R.string.dialog_close);
                builder.show();
            }
        }
    };

    private OnCancelListener oclProgressDialog = new OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
            if (App.MiamPlayerConnection != null &&
                    App.MiamPlayerConnection.mHandler != null) {
                // Move the request to the message
                Message msg = Message.obtain();
                msg.obj = MiamPlayerMessage.getMessage(MsgType.DISCONNECT);

                // Send the request to the thread
                App.MiamPlayerConnection.mHandler.sendMessage(msg);
            }
        }

    };

    /**
     * Connect to miamplayer
     */
    private void connect() {
        // Do not connect if the activity has finished!
        if (this.isFinishing()) {
            return;
        }

        if (!mKnownIps.contains(mEtIp.getText().toString())) {
            mKnownIps.add(mEtIp.getText().toString());
        }

        final String ip = mEtIp.getText().toString();

        // Save the data
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putString(SharedPreferencesKeys.SP_KEY_IP, ip);
        editor.putInt(SharedPreferencesKeys.SP_LAST_AUTH_CODE, mAuthCode);
        editor.putStringSet(SharedPreferencesKeys.SP_KNOWN_IP, mKnownIps);

        editor.apply();

        // Create a progress dialog
        mPdConnect = new MaterialDialog.Builder(this)
                .cancelable(true)
                .cancelListener(oclProgressDialog)
                .content(R.string.connectdialog_connecting)
                .progress(true, -1)
                .show();

        // Start the service so it won't be stopped on unbindService
        Intent serviceIntent = new Intent(this, MiamPlayerService.class);
        startService(serviceIntent);

        bindService(serviceIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MiamPlayerService.MiamPlayerServiceBinder miamPlayerServiceBinder
                        = (MiamPlayerService.MiamPlayerServiceBinder) service;

                miamPlayerServiceBinder.getMiamPlayerService().setUiHandler(mHandler);

                Intent connectIntent = new Intent(ConnectActivity.this, MiamPlayerService.class);
                connectIntent.putExtra(MiamPlayerService.SERVICE_ID,
                        MiamPlayerService.SERVICE_START);
                connectIntent.putExtra(MiamPlayerService.EXTRA_STRING_IP, ip);
                connectIntent.putExtra(MiamPlayerService.EXTRA_INT_PORT, getPort());
                connectIntent.putExtra(MiamPlayerService.EXTRA_INT_AUTH, mAuthCode);

                miamPlayerServiceBinder.getMiamPlayerService().handleServiceAction(connectIntent);

                unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        }, Context.BIND_AUTO_CREATE);
    }

    private int getPort() {
        // Get the port to connect to
        int port;
        try {
            port = Integer.valueOf(
                    mSharedPref.getString(SharedPreferencesKeys.SP_KEY_PORT,
                            String.valueOf(MiamPlayer.DefaultPort)));
        } catch (NumberFormatException e) {
            port = MiamPlayer.DefaultPort;
        }

        return port;
    }

    /**
     * Show the user the dialog to enter the auth code
     */
    void showAuthCodePromt() {
        new MaterialDialog.Builder(this)
                .title(R.string.input_auth_code)
                .inputType(InputType.TYPE_CLASS_NUMBER)
                .input("", "", false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        try {
                            mAuthCode = Integer.parseInt(input.toString());
                            dialog.dismiss();
                            connect();
                        } catch (NumberFormatException e) {
                            Toast.makeText(ConnectActivity.this, R.string.invalid_code,
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                }).show();
    }

    /**
     * Show the user the first time called dialog
     */
    private void showFirstTimeScreen() {
        Utilities.ShowMessageDialog(this,
                getString(R.string.first_time_title),
                getString(R.string.first_time_text, getString(R.string.miamplayer_version)),
                true);
    }

    /**
     * We connected to miamplayer successfully. Now open other view
     */
    void showPlayerDialog() {
        if (mMiamPlayerMDns != null) {
            mMiamPlayerMDns.stopServiceDiscovery();
        }

        // Start the player dialog
        Intent playerDialog = new Intent(this, MainActivity.class);
        playerDialog.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(playerDialog, ID_PLAYER_DIALOG);
    }

    /**
     * We couldn't connect to miamplayer. Inform the user
     */
    void noConnection() {
        // Do not display dialog if the activity has finished!
        if (this.isFinishing()) {
            return;
        }

        // Check if we have not a local ip
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();

        // Get the current wifi state
        if (!Utilities.onWifi()) {
            Utilities.ShowMessageDialog(this, R.string.connectdialog_error, R.string.wifi_disabled);
        } else if (!Utilities.ToInetAddress(ip).isSiteLocalAddress()) {
            Utilities.ShowMessageDialog(this, R.string.connectdialog_error, R.string.no_private_ip);
        } else {
            Utilities.ShowMessageDialog(this,
                    getString(R.string.connectdialog_error),
                    getString(R.string.check_ip, getString(R.string.miamplayer_version)),
                    false);
        }
    }

    /**
     * We have an old Proto version. User has to update MiamPlayer
     */
    void oldProtoVersion() {
        String title = getString(R.string.error_versions);
        String message = getString(R.string.old_proto, getString(R.string.miamplayer_version));
        Utilities.ShowMessageDialog(this, title, message, false);
    }

    /**
     * MiamPlayer closed the connection
     *
     * @param miamPlayerMessage The object to work with
     */
    void disconnected(MiamPlayerMessage miamPlayerMessage) {
        // Restart the background service
        mServiceIntent = new Intent(this, MiamPlayerService.class);
        mServiceIntent.putExtra(MiamPlayerService.SERVICE_ID, MiamPlayerService.SERVICE_START);
        startService(mServiceIntent);

        if (!miamPlayerMessage.isErrorMessage()) {
            if (miamPlayerMessage.getMessage().getResponseDisconnect()
                    .getReasonDisconnect() == ReasonDisconnect.Wrong_Auth_Code) {
                showAuthCodePromt();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ID_PLAYER_DIALOG) {
            if (resultCode == Activity.RESULT_CANCELED || resultCode == RESULT_QUIT) {
                finish();
            } else {
                doAutoConnect = false;
            }
        } else if (requestCode == ID_SETTINGS) {
            doAutoConnect = false;
        }
    }

    /**
     * A service was found. Now show a toast and animate the icon
     */
    void serviceFound() {
        if (mMiamPlayerMDns.getServices().isEmpty()) {
            mBtnMiamPlayer.clearAnimation();
        } else {
            // Start the animation
            mBtnMiamPlayer.startAnimation(mAlphaDown);
        }
    }

    private AnimationListener mAnimationListener = new AnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            if (!mAnimationCancel) {
                if (animation.equals(mAlphaDown)) {
                    mBtnMiamPlayer.startAnimation(mAlphaUp);
                } else {
                    mBtnMiamPlayer.startAnimation(mAlphaDown);
                }
            } else {
                mBtnMiamPlayer.clearAnimation();
                mAnimationCancel = false;
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }

    };
}
