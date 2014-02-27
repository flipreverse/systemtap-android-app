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
