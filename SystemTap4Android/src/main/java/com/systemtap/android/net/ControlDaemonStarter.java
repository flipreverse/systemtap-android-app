/*
 * Copyright 2012 Alexander Lochmann
 *
 * This file is part of SystemTap4Android.
 *
 * SystemTap4Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SystemTap4Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SystemTap4Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.systemtap.android.net;

import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.systemtap.android.R;
import com.systemtap.android.service.SystemTapService;

public class ControlDaemonStarter extends BroadcastReceiver {

	private static final String TAG = ControlDaemonStarter.class.getSimpleName();
	private ControlDaemon mControlDaemon;
	private SystemTapService mSystemTapService;
	private IntentFilter mIntentFilter;	
	
	public ControlDaemonStarter(SystemTapService pSystemTapService) {
		mSystemTapService = pSystemTapService;
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
		mIntentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		mSystemTapService.registerReceiver(this,mIntentFilter);
		mControlDaemon = null;
	}

	@Override
	public void onReceive(Context pContext, Intent pIntent) {
		this.controlDaemon(pContext,false);
	}

	public void reloadSettings() {
		this.controlDaemon(mSystemTapService,true);
	}
	
	public synchronized void stop() {
		mSystemTapService.unregisterReceiver(this);
		if (mControlDaemon != null) {
			mControlDaemon.stop();
		}
	}

	private synchronized void controlDaemon(Context pContext, boolean pReload) {
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
						Log.d(TAG,"Port changed. Restarting ControlDaemon...");
					} else {
						Log.d(TAG,"ControlDaemon is not running. Wifi changed. Starting daemon...");
					}
					// Parse the port from preferences
					int port = Integer.valueOf(settings.getString(pContext.getString(R.string.pref_daemon_port), pContext.getString(R.string.default_daemon_port)));
					// Init and start control daemon
					mControlDaemon = new ControlDaemon(port,stapService);
					mControlDaemon.start();
				} catch (IOException e) {
					Log.e(TAG,"Can't init and start ControlDaemon: " + e + " -- " + e.getMessage());
				} catch (NumberFormatException e) {
					Log.e(TAG,"Can't parse number of parallel running modules: " + e + " -- " + e.getMessage());
				}
			}
		} else {
			// No wifi connection, but daemon is running. Stop it.
			if (mControlDaemon != null) {
				Log.d(TAG,"ControlDaemon is running. Wifi changed. Stopping daemon...");
				mControlDaemon.stop();
				mControlDaemon = null;
			}
		}
	}
}
