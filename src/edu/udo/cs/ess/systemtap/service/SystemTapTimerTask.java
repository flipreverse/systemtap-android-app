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
		File pidFile = null;
		
		Eventlog.d(TAG, "Check if modules (" + mModuleManagement.getModules().size() + ") status is still up to date");
		for(Module module:mModuleManagement.getModules())
		{
			Eventlog.d(TAG,"Current module: " + module.getName() + ",  status=" + module.getStatus());
			switch (module.getStatus())
			{
				case RUNNING:
					pidFile = new File(Config.STAP_RUN_ABSOLUTE_PATH + File.separator + module.getName() + Config.PID_EXT);
					if (pidFile.exists())
					{
						try
						{
							if (this.isPidFilePidValid(pidFile))
							{
								Eventlog.e(TAG,"module (" + module.getName() + ") is running and pid file exists. all fine. :-)");
							}
							else
							{
								Eventlog.e(TAG,"module (" + module.getName() + ") is running, but stap is not running. Updating status....");
								mModuleManagement.updateModuleStatus(module.getName(), Module.Status.CRASHED);
								Eventlog.e(TAG,"module (" + module.getName() + ") is crashed. Removing pid file: " + pidFile.delete());
							}
						}
						catch (IOException e)
						{
							Eventlog.e(TAG,"Error reading pid file of module " + module.getName() + ". Try again later.");
							Eventlog.printStackTrace(TAG, e);
						}
					}
					else
					{
						Eventlog.e(TAG,"module (" + module.getName() + ") is running, but stap (no pid file) is not running. Updating status....");
						mModuleManagement.updateModuleStatus(module.getName(), Module.Status.CRASHED);
					}
					break;
					
				case STOPPED:
					pidFile = new File(Config.STAP_RUN_ABSOLUTE_PATH + File.separator + module.getName() + Config.PID_EXT);
					if (pidFile.exists())
					{
						try
						{
							if (this.isPidFilePidValid(pidFile))
							{
								Eventlog.e(TAG,"module (" + module.getName() + ") is stopped, but stap is running. Updating status....");
								mModuleManagement.updateModuleStatus(module.getName(), Module.Status.RUNNING);
							}
							else
							{
								Eventlog.d(TAG,"module (" + module.getName() + ") is stopped, but pidfile exsits. Deleting it: " + pidFile.delete());
							}
						}
						catch (IOException e)
						{
							Eventlog.e(TAG,"Error reading pid file of module " + module.getName() + ". Try again later.");
							Eventlog.printStackTrace(TAG, e);
						}
					}
					else
					{
						Eventlog.d(TAG,"module (" + module.getName() + ") is stopped and no pid file exists. all fine. :-)");
					}
					break;
					
				case CRASHED:
					break;
			}
			pidFile = null;
		}
	}

	private boolean isPidFilePidValid(File pPidFile) throws IOException
	{
		int pid = -1;
		DataInputStream in = new DataInputStream(new FileInputStream(pPidFile));
		pid = Integer.valueOf(in.readLine());
		in.close();
		in = null;
		List<Integer> pids = Util.getProcessIDs(mContext,Config.STAP_IO_NAME);

		if (pids == null)
		{
			return false;
		}
		
		return pids.contains(pid);
	}
}
