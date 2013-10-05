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
import edu.udo.cs.ess.systemtap.R;
import edu.udo.cs.ess.systemtap.service.SystemTapService;

public class ControlDaemonStarter extends BroadcastReceiver {

	private static final String TAG = ControlDaemonStarter.class.getSimpleName();
	private ControlDaemon mControlDaemon;
	private Object mLock;
	private IntentFilter mIntentFilter;	
	
	public ControlDaemonStarter() {
		mLock = new Object();
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
		this.controlDaemon(pContext,false);
	}

	public void reloadSettings(Context pContext) {
		this.controlDaemon(pContext,true);
	}

	private void controlDaemon(Context pContext, boolean pReload) {
		synchronized(mLock) {
			ConnectivityManager connManager = (ConnectivityManager) pContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			SystemTapService stapService = (SystemTapService)pContext;

			// Got wifi connection?
			if (netInfo.isConnected()) {
				// Daemon already running?
				if (mControlDaemon == null || pReload) {
					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(pContext);
					try {
						if (pReload) {
							if (mControlDaemon != null) {
								mControlDaemon.stop();
							}
							mControlDaemon = null;
							Eventlog.d(TAG,"Port changed. Restarting ControlDaemon...");
						} else {
							Eventlog.d(TAG,"ControlDaemon is not running. Wifi changed. Starting daemon...");
						}
						// Parse the port from preferences
						int port = Integer.valueOf(settings.getString(pContext.getString(R.string.pref_daemon_port), pContext.getString(R.string.default_daemon_port)));
						// Init and start control daemon
						mControlDaemon = new ControlDaemon(port,stapService);
						mControlDaemon.start();
					} catch (IOException e) {
						Eventlog.e(TAG,"Can't init and start ControlDaemon: " + e + " -- " + e.getMessage());
					} catch (NumberFormatException e) {
						Eventlog.e(TAG,"Can't parse number of parallel running modules: " + e + " -- " + e.getMessage());
					}
				}
			} else {
				// No wifi connection, but daemon is running. Stop it.
				if (mControlDaemon != null) {
					Eventlog.d(TAG,"ControlDaemon is running. Wifi changed. Stopping daemon...");
					mControlDaemon.stop();
					mControlDaemon = null;
				}
			}
		}
	}
}
