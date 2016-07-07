/*
 * Copyright 2015 Alexander Lochmann
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

package com.systemtap.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.systemtap.android.R;

public class BootReceiver extends BroadcastReceiver {

	private static final String TAG = SystemTapService.TAG + "." + BootReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context pContext, Intent pIntent) {
        Log.d(TAG,"Received " + pIntent.getAction());
		if (pIntent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(pContext);
            if (settings.getBoolean(pContext.getString(R.string.pref_autostart), false)){
                Log.d(TAG,"Received ACTION_BOOT_COMPLETED. Starting SystemTapService...");
				Intent intent = new Intent(pContext,SystemTapService.class);
				pContext.startService(intent);
            }
        } else {
            Log.e(TAG,"Received unknown event:" + pIntent.getAction());
        }
    }

}
