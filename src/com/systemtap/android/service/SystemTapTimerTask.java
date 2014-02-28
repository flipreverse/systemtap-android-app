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
