package edu.udo.cs.ess.systemtap.service;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.TimerTask;

import android.content.Context;
import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.Config;

public class SystemTapTimerTask extends TimerTask
{
	private static final String TAG = SystemTapTimerTask.class.getSimpleName();
	
	private ModuleManagement mModuleManagement;
	private Context mContext;
	
	public SystemTapTimerTask(ModuleManagement pModuleManagement,Context pContext)
	{
		mModuleManagement = pModuleManagement;
		mContext = pContext;
	}
	
	@Override
	public void run()
	{
		Module.Status moduleStatus;
		
		Eventlog.d(TAG, "Check if modules (" + mModuleManagement.getModules().size() + ") status is still up to date");
		for(Module module:mModuleManagement.getModules())
		{
			Eventlog.d(TAG,"Current module: " + module.getName() + ",  status=" + module.getStatus());
			moduleStatus = module.getStatus();
			switch (module.getStatus())
			{
				case RUNNING:
					moduleStatus = Util.checkModuleStatus(mContext, module.getName(), true);
					break;
					
				case STOPPED:
					moduleStatus = Util.checkModuleStatus(mContext, module.getName(), false);
					break;
					
				case CRASHED:
					break;
			}
			if (module.getStatus() != moduleStatus)
			{
				Eventlog.d(TAG,"modules (" + module.getName() + ") status has changed. Updating database...");
				mModuleManagement.updateModuleStatus(module.getName(), moduleStatus);
			}
		}
	}
}
