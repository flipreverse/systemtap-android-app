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

package com.systemtap.android.service;

import java.util.TimerTask;

import android.content.Context;
import android.util.Log;

import com.systemtap.android.net.SystemTapMessage.ModuleStatus;

public class SystemTapTimerTask extends TimerTask
{
	private static final String TAG = SystemTapTimerTask.class.getSimpleName();
	
	private ModuleManagement mModuleManagement;
	private Context mContext;
	private SystemTapHandler mSystemTapHandler;
	
	public SystemTapTimerTask(ModuleManagement pModuleManagement,Context pContext, SystemTapHandler pSystemTapHandler)
	{
		mModuleManagement = pModuleManagement;
		mContext = pContext;
		mSystemTapHandler = pSystemTapHandler;
	}
	
	@Override
	public void run()
	{
		ModuleStatus moduleNewStatus;
		
		Log.d(TAG, "Check if modules (" + mModuleManagement.getModules().size() + ") status is still up to date");
		for(Module module:mModuleManagement.getModules())
		{
			Log.d(TAG,"Current module: " + module.getName() + ",  status=" + module.getStatus());
			moduleNewStatus = Util.checkModuleStatus(mContext, module.getName(), module.getStatus());
			if (module.getStatus() != moduleNewStatus)
			{
				Log.d(TAG,"modules (" + module.getName() + ") status has changed. Updating database...");
				// Since we just get here in case the module status has changed, it is just necessary to check moduleNewStatus.
				if (moduleNewStatus == ModuleStatus.STOPPED || moduleNewStatus == ModuleStatus.CRASHED) {
					mSystemTapHandler.decrementRunningModules();
				} else if (moduleNewStatus == ModuleStatus.RUNNING) {
					mSystemTapHandler.incrementRunningModules();
				}
				mModuleManagement.updateModuleStatus(module.getName(), moduleNewStatus);
			}
		}
	}
}
