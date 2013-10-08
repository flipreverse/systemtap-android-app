package edu.udo.cs.ess.systemtap.service;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Timer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import edu.udo.cs.ess.systemtap.net.ControlDaemonStarter;
import edu.udo.cs.ess.systemtap.net.protocol.SystemTapMessage.ModuleStatus;



public class SystemTapHandler extends Handler
{
	private static final String TAG = SystemTapHandler.class.getSimpleName();
	
	public static final int START_MODULE = 0x0;
	public static final int STOP_MODULE = 0x2;
	public static final int DELETE_MODULE = 0x4;
	public static final int RELOAD_PREFERENCES = 0x8;
	public static final int DELETE_LOG_FILE = 0x10;
	public static final int DELETE_OUTPUT_FILE = 0x20;
	
	public static final String MODULENAME_ID = "modulename";
	public static final String FILENAME_ID = "filename";

	private ModuleManagement mModuleManagement;
	private SystemTapService mSystemTapService;
	private ControlDaemonStarter mControlDaemonStarter;
	private Timer mTimer;
	private SystemTapTimerTask mSystemtTapTimerTask;
	private WakeLock mWakeLock;
	private int mNoRunning;
	private String mLogPath;
	private String mOutputPath;
	private String mRunPath;
	private String mModulesPath;

	public SystemTapHandler(Looper pLooper,SystemTapService pSystemTapService,ModuleManagement pModuleManagement, String pLogPath, String pOutputPath, String pModulesPath, String pRunPath)
	{
		super(pLooper);
		mSystemTapService = pSystemTapService;
		mModuleManagement = pModuleManagement;
		PowerManager powerManager = (PowerManager)mSystemTapService.getSystemService(Context.POWER_SERVICE);
		mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SystemTap WakeLock");
		mTimer = new Timer("SystemTapTimer",true);
		// Start the WIFI/connection listener, which will bring up the control daemon.
		mControlDaemonStarter = new ControlDaemonStarter(mSystemTapService);
		mSystemtTapTimerTask = null;
		mNoRunning = 0;
		mLogPath = pLogPath;
		mOutputPath = pOutputPath;
		mModulesPath = pModulesPath;
		mRunPath = pRunPath;
	}
	
	@Override
	public void handleMessage(Message pMsg)
	{
		String modulename = null;
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mSystemTapService);

		switch (pMsg.what)
		{
			case START_MODULE:
				modulename = pMsg.getData().getString(MODULENAME_ID);
				this.startModule(modulename);
				break;
				
			case STOP_MODULE:
				modulename = pMsg.getData().getString(MODULENAME_ID);
				this.stopModule(modulename);
				break;
	
			case DELETE_MODULE:
				modulename = pMsg.getData().getString(MODULENAME_ID);
				this.deleteModule(modulename);
				break;
	
			case RELOAD_PREFERENCES:
				Eventlog.d(TAG,"Reloading preferences...");
				Eventlog.d(TAG,"Restarting ControlDaemon");
				mControlDaemonStarter.reloadSettings();
				Eventlog.d(TAG,"Resetting wake lock");
				synchronized (mTimer) {
					if (mNoRunning > 0) {
						if (settings.getBoolean(mSystemTapService.getString(R.string.pref_wakelock), false)) {
							if (!mWakeLock.isHeld()) {
								Eventlog.d(TAG,"User wants a wake lock. Acquiring wake lock.");
								mWakeLock.acquire();
							}
						} else {
							if (mWakeLock.isHeld()) {
								Eventlog.d(TAG,"User doesn't want a wake lock. Releasing wake lock.");
								mWakeLock.release();
							}
						}
					}
				}
				Eventlog.d(TAG,"Check if number of parallel running modules still match preference");
				LinkedList<Module> modules = mModuleManagement.getRunningModules();
				int noRunningModules = -1;
				if ((noRunningModules = this.getPrefRunningModules()) == -1) {
					break;
				}
				/*
				 * This is a quite ugly strategy to determine which modules should be killed!! :-/
				 * Maybe this should be commented out.
				 */
				if (modules.size() > noRunningModules) {
					int noToKill = modules.size() - noRunningModules;
					Eventlog.d(TAG,"User reduced number of parallel running modules by " + noToKill + ". Killing the first " + noToKill + " running modules.");
					for (int i = 0; i < noToKill; i++) {
						Module module = modules.get(i);
						this.stopModule(module.getName());
						Eventlog.d(TAG,"Killed module \"" + module.getName() + "\"");
					}
				}
				break;

			case DELETE_OUTPUT_FILE:
			case DELETE_LOG_FILE:
				Bundle data = pMsg.getData();
				String filename = data.getString(SystemTapHandler.FILENAME_ID), moduleName = data.getString(SystemTapHandler.MODULENAME_ID);
				File files[] = null;

				if (filename == null) {
					if (pMsg.what == DELETE_OUTPUT_FILE) {
						files = this.getOutputFiles(moduleName);
					} else {
						files = this.getLogFiles(moduleName);
					}
				} else {
					if (pMsg.what == DELETE_OUTPUT_FILE) {
						files = new File[]{ new File(mOutputPath + File.separator + filename) };
					} else {
						files = new File[]{ new File(mLogPath + File.separator + filename) };
					}
				}
				this.deleteFiles(files);
				break;

			default:
				Eventlog.d(TAG,"handleMessage(): what=" + pMsg.what);
		}
	}
	
	public void onDestory() {
		mControlDaemonStarter.stop();
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
		            if (settings.getBoolean(mSystemTapService.getString(R.string.pref_wakelock), false)) {
		            	Eventlog.d(TAG,"Acquire wake lock");
		            	mWakeLock.acquire();
		            }
			}
			mNoRunning++;
		}
	}
	
	public File[] getOutputFiles(final String pModuleName) {
		return this.getFiles(pModuleName, mOutputPath);
	}
	
	public File[] getLogFiles(final String pModuleName) {
		return this.getFiles(pModuleName, mLogPath);
	}
	
	private File[] getFiles(final String pModuleName, String pPath) {
		File outputDir = new File(pPath);
		File outputFiles[] = outputDir.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				if (pathname.isFile()) {
					return pathname.getName().startsWith(pModuleName);
				}
				return false;
			}
		});
		return outputFiles;
	}
	
	private void deleteFiles(File pFiles[]) {
		for (File curFile : pFiles) {
			boolean ret = curFile.delete();
			Eventlog.d(TAG,"Deleting " + curFile.getAbsolutePath() + " .... Result: " + ret);
		}
	}
	
	private void deleteModule(String pModulename) {
		File moduleFile = null;

		moduleFile = new File(mModulesPath + File.separator + pModulename + Config.MODULE_EXT);
		if (moduleFile.exists()) {
			Module module = mModuleManagement.getModule(pModulename);
			if (module == null) {
				Eventlog.e(TAG,"Asked for a module deletion, but no such module: " + pModulename);
				return;
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
		} else {
			Toast.makeText(mSystemTapService, mSystemTapService.getText(R.string.stap_service_start_fail_nomodule) + ":" + pModulename, Toast.LENGTH_SHORT).show();
			Eventlog.e(TAG, "START_MODULE: No such module " + pModulename);
		}
	}
	
	private void startModule(String pModulename) {
		LinkedList<String> list = null;
		File moduleFile = null;

		int noRunningModules = -1;
		if ((noRunningModules = this.getPrefRunningModules()) == -1)  {
			return;
		}

		if (mModuleManagement.getRunningModules().size() >= noRunningModules) {
			Eventlog.e(TAG,"Reached maximum number of parallel running modules.");
			Toast.makeText(mSystemTapService,mSystemTapService.getString(R.string.stap_service_start_fail_maxmodules), Toast.LENGTH_SHORT).show();
			return;
		}

		moduleFile = new File(mModulesPath + File.separator + pModulename + Config.MODULE_EXT);
		if (moduleFile.exists()) {
			Module module = mModuleManagement.getModule(pModulename);
			if (module == null) {
				module = mModuleManagement.createModule(pModulename);
			}
			DateFormat format = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss");
			Date date = new Date();
			String outputFilename = pModulename + "_" + format.format(date);
			list = new LinkedList<String>();
			list.add("modulename=" + pModulename);
			list.add("moduledir=" + mModulesPath);
			list.add("outputname=" + outputFilename);
			list.add("outputdir=" + mOutputPath);
			list.add("logdir=" + mLogPath);
			list.add("rundir=" + mRunPath);
			list.add("stapdir=" + mSystemTapService.getFilesDir().getParent());
			list.add(":q!");
			
			if (Util.runCmdAsRoot(mSystemTapService.getFilesDir().getParent() + File.separator + Config.STAP_SCRIPT_NAME,list) != 0) {
				Eventlog.e(TAG,"Could not start stap");
				Toast.makeText(mSystemTapService, mSystemTapService.getText(R.string.stap_service_start_failed) + ":" + pModulename, Toast.LENGTH_SHORT).show();
				mModuleManagement.updateModuleStatus(module.getName(), ModuleStatus.CRASHED);
			} else {
				try {
					/* Wait a few milliseconds until stapio is *really* started. Otherwise the following status update will fail! */
					Thread.sleep(900);
				} catch (InterruptedException e) {
					/* We don't care :-) */
				}
				ModuleStatus moduleStatus = Util.checkModuleStatus(mSystemTapService, module.getName(), ModuleStatus.RUNNING);
				mModuleManagement.updateModuleStatus(module.getName(), moduleStatus);
				if (moduleStatus == ModuleStatus.RUNNING) {
					this.incrementRunningModules();
				}
			}
		} else {
			Toast.makeText(mSystemTapService, mSystemTapService.getText(R.string.stap_service_start_fail_nomodule) + ":" + pModulename, Toast.LENGTH_SHORT).show();
			Eventlog.e(TAG, "START_MODULE: No such module " + pModulename);
		}
	}
	
	private void stopModule(String pModulename) {
		LinkedList<String> list = null;

		File pidFile = new File(mRunPath + File.separator + pModulename + Config.PID_EXT);
		if (pidFile.exists()) {
			int pid = -1;
			try {
				DataInputStream in = new DataInputStream(new FileInputStream(pidFile));
				pid = Integer.valueOf(in.readLine());
				in.close();
			} catch (IOException e) {
				Eventlog.printStackTrace(TAG, e);
				return;
			}
			list = new LinkedList<String>();
			list.add("pid=" + pid);
			list.add("busyboxdir=" + mSystemTapService.getFilesDir().getParent());
			list.add(":q!");
			if (Util.runCmdAsRoot(mSystemTapService.getFilesDir().getParent() + File.separator + Config.KILL_SCRIPT_NAME,list) != 0) {
				Toast.makeText(mSystemTapService, mSystemTapService.getText(R.string.stap_service_stop_fail) + ":" + pModulename, Toast.LENGTH_SHORT).show();
				Eventlog.e(TAG, "Could not run kill script (" + pModulename + ")");
			} else {
				try {
					/* Wait a few milliseconds until stapio is *really* started. Otherwise the following status update will fail! */
					Thread.sleep(700);
				} catch (InterruptedException e) {
					/* We don't care :-) */
				}
				ModuleStatus moduleStatus = Util.checkModuleStatus(mSystemTapService, pModulename, ModuleStatus.STOPPED);
				mModuleManagement.updateModuleStatus(pModulename, moduleStatus);
				if (moduleStatus == ModuleStatus.STOPPED) {
					this.decrementRunningModules();
				}
			}
		} else {
			Toast.makeText(mSystemTapService, mSystemTapService.getText(R.string.stap_service_stop_fail_nomdoule), Toast.LENGTH_SHORT).show();
			Eventlog.e(TAG, "Could not stop module - selected module (" + pModulename + ") is not running");
		}
	}
	
	private int getPrefRunningModules() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mSystemTapService);

		int noRunningModules = -1;
		try {
			// Parse the number of parallel running modules from preferences
			noRunningModules = Integer.valueOf(settings.getString(mSystemTapService.getString(R.string.pref_running_modules), mSystemTapService.getString(R.string.default_running_modules)));
		} catch (NumberFormatException e) {
			Eventlog.e(TAG,"Can't parse number of parallel running modules: " + e + " -- " + e.getMessage());
			return -1;
		}
		return noRunningModules;
	}
}
