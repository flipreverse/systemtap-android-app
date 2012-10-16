package edu.udo.cs.ess.systemtap.service;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;
import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.Config;
import edu.udo.cs.ess.systemtap.R;



public class SystemTapHandler extends Handler
{
	private static final String TAG = SystemTapHandler.class.getSimpleName();
	
	public static final int START_MODULE = 0x0;
	public static final int STOP_MODULE = 0x2;
	
	public static final String MODULENAME_ID = "modulename";

	private ModuleManagement mModuleManagement;
	private SystemTapService mSystemTapService;

	public SystemTapHandler(Looper pLooper,SystemTapService pSystemTapService,ModuleManagement pModuleManagetment)
	{
		super(pLooper);
		mSystemTapService = pSystemTapService;
		mModuleManagement = pModuleManagetment;
	}
	
	@Override
	public void handleMessage(Message pMsg)
	{
		String modulename = null;
		LinkedList<String> list = null;
		switch (pMsg.what)
		{
		case START_MODULE:
			modulename = pMsg.getData().getString(MODULENAME_ID);
			File moduleFile = new File(Config.MODULES_ABSOLUTE_PATH + File.separator + modulename + Config.MODULE_EXT);
			if (moduleFile.exists())
			{
				Module module = mModuleManagement.getModule(modulename);
				if (module == null)
				{
					module = mModuleManagement.createModule(modulename);
				}
				DateFormat format = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss");
				Date date = new Date();
				String outputFilename = modulename + "_" + format.format(date);
				list = new LinkedList<String>();
				list.add("modulename=" + modulename);
				list.add("moduledir=" + Config.MODULES_ABSOLUTE_PATH);
				list.add("outputname=" + outputFilename);
				list.add("outputdir=" + Config.STAP_OUTPUT_ABSOLUTE_PATH);
				list.add("logdir=" + Config.STAP_LOG_ABSOLUTE_PATH);
				list.add("rundir=" + Config.STAP_RUN_ABSOLUTE_PATH);
				list.add("stapdir=" + mSystemTapService.getFilesDir().getParent());
				list.add(":q!");
				
				if (Util.runCmdAsRoot(mSystemTapService.getFilesDir().getParent() + File.separator + Config.STAP_SCRIPT_NAME,list) != 0)
				{
					Eventlog.e(TAG,"Could not start stap");
					Toast.makeText(mSystemTapService, mSystemTapService.getText(R.string.stap_service_start_failed) + ":" + modulename, Toast.LENGTH_SHORT).show();
					mModuleManagement.updateModuleStatus(module.getName(), Module.Status.CRASHED);
				}
				else
				{
					Module.Status moduleStatus = Util.checkModuleStatus(mSystemTapService, module.getName(), true);
					mModuleManagement.updateModuleStatus(module.getName(), moduleStatus);
				}
			}
			else
			{
				Toast.makeText(mSystemTapService, mSystemTapService.getText(R.string.stap_service_start_fail_nomodule) + ":" + modulename, Toast.LENGTH_SHORT).show();
				Eventlog.e(TAG, "START_MODULE: No such module " + modulename);
			}
			break;
			
		case STOP_MODULE:
			modulename = pMsg.getData().getString(MODULENAME_ID);
			File pidFile = new File(Config.STAP_RUN_ABSOLUTE_PATH + File.separator + modulename + Config.PID_EXT);
			if (pidFile.exists())
			{
				int pid = -1;
				try
				{
					DataInputStream in = new DataInputStream(new FileInputStream(pidFile));
					pid = Integer.valueOf(in.readLine());
					in.close();
				}
				catch (IOException e)
				{
					Eventlog.printStackTrace(TAG, e);
					break;
				}
				list = new LinkedList<String>();
				list.add("pid=" + pid);
				list.add("busyboxdir=" + mSystemTapService.getFilesDir().getParent());
				list.add(":q!");
				if (Util.runCmdAsRoot(mSystemTapService.getFilesDir().getParent() + File.separator + Config.KILL_SCRIPT_NAME,list) != 0)
				{
					Toast.makeText(mSystemTapService, mSystemTapService.getText(R.string.stap_service_stop_fail) + ":" + modulename, Toast.LENGTH_SHORT).show();
					Eventlog.e(TAG, "Could not run kill script (" + modulename + ")");
				}
			}
			else
			{
				Toast.makeText(mSystemTapService, mSystemTapService.getText(R.string.stap_service_stop_fail_nomdoule), Toast.LENGTH_SHORT).show();
				Eventlog.e(TAG, "Could not stop module - selected module (" + modulename + ") is not running");
			}
			break;
		}
	}
}
