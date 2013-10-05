package edu.udo.cs.ess.systemtap.service;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Timer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.widget.Toast;
import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.Config;
import edu.udo.cs.ess.systemtap.R;
import edu.udo.cs.ess.systemtap.net.ControlDaemon;
import edu.udo.cs.ess.systemtap.net.protocol.SystemTapMessage.ModuleStatus;



public class SystemTapHandler extends Handler
{
	private static final String TAG = SystemTapHandler.class.getSimpleName();
	
	public static final int START_MODULE = 0x0;
	public static final int STOP_MODULE = 0x2;
	public static final int DELETE_MODULE = 0x4;
	
	public static final String MODULENAME_ID = "modulename";

	private ModuleManagement mModuleManagement;
	private SystemTapService mSystemTapService;
	private Timer mTimer;
	private SystemTapTimerTask mSystemtTapTimerTask;
	private WakeLock mWakeLock;
	private int mNoRunning;

	public SystemTapHandler(Looper pLooper,SystemTapService pSystemTapService,ModuleManagement pModuleManagetment)
	{
		super(pLooper);
		mSystemTapService = pSystemTapService;
		mModuleManagement = pModuleManagetment;
		PowerManager powerManager = (PowerManager)mSystemTapService.getSystemService(Context.POWER_SERVICE);
		mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SystemTap WakeLock");
		mTimer = new Timer("SystemTapTimer",true);
		mSystemtTapTimerTask = null;
		mNoRunning = 0;
	}
	
	@Override
	public void handleMessage(Message pMsg)
	{
		String modulename = null;
		LinkedList<String> list = null;
		File moduleFile = null;
		switch (pMsg.what)
		{
		case START_MODULE:
			int noRunningModules = -1;
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mSystemTapService);
			try {
				// Parse the number of parallel running modules from preferences
				noRunningModules = Integer.valueOf(settings.getString(mSystemTapService.getString(R.string.pref_running_modules), mSystemTapService.getString(R.string.default_running_modules)));
			} catch (NumberFormatException e) {
				Eventlog.e(TAG,"Can't parse number of parallel running modules: " + e + " -- " + e.getMessage());
				break;
			}

			if (mModuleManagement.getRunningModules().size() >= noRunningModules) {
				Eventlog.e(TAG,"Reached maximum number of parallel running modules.");
				Toast.makeText(mSystemTapService,mSystemTapService.getString(R.string.stap_service_start_fail_maxmodules), Toast.LENGTH_SHORT).show();
				break;
			}

			modulename = pMsg.getData().getString(MODULENAME_ID);
			moduleFile = new File(Config.MODULES_ABSOLUTE_PATH + File.separator + modulename + Config.MODULE_EXT);
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
					mModuleManagement.updateModuleStatus(module.getName(), ModuleStatus.CRASHED);
				}
				else
				{
					try
					{
						/* Wait a few milliseconds until stapio is *really* started. Otherwise the following status update will fail! */
						Thread.sleep(900);
					}
					catch (InterruptedException e)
					{
						/* We don't care :-) */
					}
					ModuleStatus moduleStatus = Util.checkModuleStatus(mSystemTapService, module.getName(), ModuleStatus.RUNNING);
					mModuleManagement.updateModuleStatus(module.getName(), moduleStatus);
					if (moduleStatus == ModuleStatus.RUNNING) {
						this.incrementRunningModules();
					}
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
				else
				{
					try
					{
						/* Wait a few milliseconds until stapio is *really* started. Otherwise the following status update will fail! */
						Thread.sleep(700);
					}
					catch (InterruptedException e)
					{
						/* We don't care :-) */
					}
					ModuleStatus moduleStatus = Util.checkModuleStatus(mSystemTapService, modulename, ModuleStatus.STOPPED);
					mModuleManagement.updateModuleStatus(modulename, moduleStatus);
					if (moduleStatus == ModuleStatus.STOPPED) {
						this.decrementRunningModules();
					}
				}
			}
			else
			{
				Toast.makeText(mSystemTapService, mSystemTapService.getText(R.string.stap_service_stop_fail_nomdoule), Toast.LENGTH_SHORT).show();
				Eventlog.e(TAG, "Could not stop module - selected module (" + modulename + ") is not running");
			}
			break;

		case DELETE_MODULE:
			modulename = pMsg.getData().getString(MODULENAME_ID);
			moduleFile = new File(Config.MODULES_ABSOLUTE_PATH + File.separator + modulename + Config.MODULE_EXT);
			if (moduleFile.exists())
			{
				Module module = mModuleManagement.getModule(modulename);
				if (module == null)
				{
					Eventlog.e(TAG,"Asked for a module deletion, but no such module: " + modulename);
					break;
				}
				if (module.getStatus() != ModuleStatus.RUNNING) {
					if (!moduleFile.delete()) {
						Eventlog.e(TAG,"Can't delete module file: " + moduleFile.getAbsolutePath());
					} else {
						Eventlog.d(TAG,"Deleted module file: " + moduleFile.getAbsolutePath());
					}
				} else {
					Eventlog.e(TAG,"Can't delete module file. It is currently running!");
				}
			}
			else
			{
				Toast.makeText(mSystemTapService, mSystemTapService.getText(R.string.stap_service_start_fail_nomodule) + ":" + modulename, Toast.LENGTH_SHORT).show();
				Eventlog.e(TAG, "START_MODULE: No such module " + modulename);
			}
			break;
		}
	}
	
	public void decrementRunningModules() {
		synchronized (mTimer) {
			mNoRunning--;
			if (mNoRunning == 0) {
					mSystemtTapTimerTask.cancel();
					mSystemtTapTimerTask = null;
					mTimer.purge();
					Eventlog.d(TAG,"Last running module stopped. Canceled timer.");
		            if (mWakeLock.isHeld()){
		            	mWakeLock.release();
		            	Eventlog.d(TAG,"Released wake lock");
		            }
			}
		}
	}
	
	public void incrementRunningModules() {
		synchronized (mTimer) {
			if (mNoRunning == 0) {
					Eventlog.d(TAG,"First module started. Starting timer task.");
					mSystemtTapTimerTask = new SystemTapTimerTask(mModuleManagement,mSystemTapService,this);
					mTimer.schedule(mSystemtTapTimerTask, 10 * 1000, Config.TIMER_TASK_PERIOD);
					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mSystemTapService);
		            if (settings.getBoolean(mSystemTapService.getString(R.string.pref_wakelock), false)){
		            	Eventlog.d(TAG,"Acquire wake lock");
		            	mWakeLock.acquire();
		            }
			}
			mNoRunning++;
		}
	}
}
