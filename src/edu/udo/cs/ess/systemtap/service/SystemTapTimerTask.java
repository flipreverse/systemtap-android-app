package edu.udo.cs.ess.systemtap.service;

import java.util.TimerTask;

import android.content.Context;
import edu.udo.cs.ess.logging.Eventlog;

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
		Module.Status moduleNewStatus;
		
		Eventlog.d(TAG, "Check if modules (" + mModuleManagement.getModules().size() + ") status is still up to date");
		for(Module module:mModuleManagement.getModules())
		{
			Eventlog.d(TAG,"Current module: " + module.getName() + ",  status=" + module.getStatus());
			moduleNewStatus = Util.checkModuleStatus(mContext, module.getName(), module.getStatus());
			if (module.getStatus() != moduleNewStatus)
			{
				Eventlog.d(TAG,"modules (" + module.getName() + ") status has changed. Updating database...");
				// Since we just get here in case the module status has changed, it is just necessary to check moduleNewStatus.
				if (moduleNewStatus == Module.Status.STOPPED || moduleNewStatus == Module.Status.CRASHED) {
					mSystemTapHandler.decrementRunningModules();
				} else if (moduleNewStatus == Module.Status.RUNNING) {
					mSystemTapHandler.incrementRunningModules();
				}
				mModuleManagement.updateModuleStatus(module.getName(), moduleNewStatus);
			}
		}
	}
}
