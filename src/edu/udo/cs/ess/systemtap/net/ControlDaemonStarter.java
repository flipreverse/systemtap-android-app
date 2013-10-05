package edu.udo.cs.ess.systemtap.net;

import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.Config;
import edu.udo.cs.ess.systemtap.R;
import edu.udo.cs.ess.systemtap.service.SystemTapService;

public class ControlDaemonStarter extends BroadcastReceiver {

	private static final String TAG = ControlDaemonStarter.class.getSimpleName();
	private ControlDaemon mControlDaemon;
	private IntentFilter mIntentFilter;	
	
	public ControlDaemonStarter() {
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
		mIntentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		mControlDaemon = null;
	}
	
	public IntentFilter getFilter() {
		return mIntentFilter;
	}

	@Override
	public void onReceive(Context pContext, Intent pIntent) {
		ConnectivityManager connManager = (ConnectivityManager) pContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		SystemTapService stapService = (SystemTapService)pContext;

		// Got wifi connection?
		if (netInfo.isConnected()) {
			// Daemon already running?
			if (mControlDaemon == null) {
				Eventlog.d(TAG,"ServerDaemon is not running. Wifi changed. Starting daemon...");
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(pContext);
				try {
					// Parse the port from preferences
					int port = Integer.valueOf(settings.getString(pContext.getString(R.string.pref_daemon_port), pContext.getString(R.string.default_daemon_port)));
					// Init and start control daemon
					mControlDaemon = new ControlDaemon(port,stapService);
					mControlDaemon.start();
				} catch (IOException e) {
					Eventlog.e(TAG,"Can't init and start ServerDaemon: " + e.getMessage());
				}
			}
		} else {
			// No wifi connection, but daemon is running. Stop it.
			if (mControlDaemon != null) {
				Eventlog.d(TAG,"ServerDaemon is running. Wifi changed. Stopping daemon...");
				mControlDaemon.stop();
				mControlDaemon = null;
			}
		}
	}

}
