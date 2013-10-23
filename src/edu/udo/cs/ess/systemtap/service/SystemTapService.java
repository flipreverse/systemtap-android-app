package edu.udo.cs.ess.systemtap.service;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Observer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.Config;
import edu.udo.cs.ess.systemtap.R;
import edu.udo.cs.ess.systemtap.SystemTapActivity;
import edu.udo.cs.ess.systemtap.net.protocol.SystemTapMessage.ModuleStatus;

public class SystemTapService extends Service
{
	public static final String TAG = SystemTapService.class.getSimpleName();
	private static final int NOTIFICATION_ID = 0x4711;
	public static final String RELOAD_PREFERENCES = "reloadpreferences";
	
	private SystemTapHandler mSystemTapHandler;
	private SystemTapBinder mSystemTapBinder;
	private ModuleManagement mModuleManagement;
	private boolean mInitFailed;
	private boolean mInit;
	
	public SystemTapService()
	{
		super();
		mInitFailed = false;
		mInit = false;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if (intent == null || intent.getAction() == null) {
			return START_STICKY;
		}

		Message msg = mSystemTapHandler.obtainMessage();
		if (intent.getAction().equalsIgnoreCase(SystemTapService.RELOAD_PREFERENCES)) {
			msg.what = SystemTapHandler.RELOAD_PREFERENCES;			
		}
		mSystemTapHandler.sendMessage(msg);

	    return START_STICKY;
	}
	
	@Override
	public void onCreate()
	{		
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
		{
			BroadcastReceiver externalStorageReceiver = new BroadcastReceiver()
			{
				@Override
				public void onReceive(Context pContext, Intent pIntet)
				{
					pContext.unregisterReceiver(this);
					SystemTapService.this.initialize();
					if (mInitFailed)
					{
					    Toast.makeText(SystemTapService.this, SystemTapService.this.getText(R.string.stap_service_start_failed), Toast.LENGTH_SHORT).show();
					}
				}
			};
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
			intentFilter.addDataScheme("file");
			this.registerReceiver(externalStorageReceiver, intentFilter);
		}
		else
		{
			this.initialize();
			if (mInitFailed)
			{
			    Toast.makeText(this, this.getText(R.string.stap_service_start_failed), Toast.LENGTH_SHORT).show();
			}
		}
		
		super.onCreate();
	}
	
	@Override
	public void onDestroy()
	{
	    Toast.makeText(this, this.getText(R.string.stap_service_terminated), Toast.LENGTH_SHORT).show();

		if (!mInit || mInitFailed)
		{
			Eventlog.e(TAG, "public method on SystemTapService called, but service is not initialized!");
			return;
		}

		mModuleManagement.save();
	    this.stopAllModules();
	    mSystemTapHandler.onDestory();
	    
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0) { return mSystemTapBinder; }
	
	/* BEGIN - PUBLIC INTERFACE */
	public boolean addModule(String pName, byte[] pData) {
		File moduleFile = new File(Config.MODULES_ABSOLUTE_PATH + File.separator + pName + Config.MODULE_EXT);
		if (moduleFile.exists()) {
			Eventlog.e(TAG,"addModule(): Module " + pName + " already present. Doing nothing.");
			return true;
		}
		/**
		 * The module management keeps track of the module directory for added or deleted files. It analyzes them, if it is a systemtap module.
		 * If not present in database, it will be added. Hence, to add a new module it's just necessary to copy it to the directory.
		 * The observer will do the rest. 
		 */
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(moduleFile));
			out.write(pData);
			out.close();
		} catch(IOException e) {
			Eventlog.e(TAG,"addModule(): Can't save module content to file (" + moduleFile.getAbsolutePath() + "): " + e.getMessage());
			return false;
		}
		return true;
	}

	public void startModule(String pModulename)
	{
		if (!mInit || mInitFailed)
		{
			Eventlog.e(TAG, "public method on SystemTapService called, but service is not initialized!");
			return;
		}
		
		Message msg = mSystemTapHandler.obtainMessage();
		msg.what = SystemTapHandler.START_MODULE;
		Bundle data = new Bundle();
		data.putString(SystemTapHandler.MODULENAME_ID, pModulename);
		msg.setData(data);
		mSystemTapHandler.sendMessage(msg);
	}

	public void stopModule(String pModulename)
	{
		if (!mInit || mInitFailed)
		{
			Eventlog.e(TAG, "public method on SystemTapService called, but service is not initialized!");
			return;
		}
		
		Message msg = mSystemTapHandler.obtainMessage();
		msg.what = SystemTapHandler.STOP_MODULE;
		Bundle data = new Bundle();
		data.putString(SystemTapHandler.MODULENAME_ID, pModulename);
		msg.setData(data);
		mSystemTapHandler.sendMessage(msg);
	}

	public void deleteModule(String pModulename)
	{
		if (!mInit || mInitFailed)
		{
			Eventlog.e(TAG, "public method on SystemTapService called, but service is not initialized!");
			return;
		}
		/**
		 * Forward the instruction to the service handler, which will call the module management to delete the module.
		 * Someone may argue that it is sufficient to delete the file from its directory. But further checks are required, e.g. is this module running.
		 * Moreover upcoming features may require additional checks or operations.
		 * Hence, it is necessary to implement module deletion in this asymmetrical way in comparison to adding a module.
		 */
		Message msg = mSystemTapHandler.obtainMessage();
		msg.what = SystemTapHandler.DELETE_MODULE;
		Bundle data = new Bundle();
		data.putString(SystemTapHandler.MODULENAME_ID, pModulename);
		msg.setData(data);
		mSystemTapHandler.sendMessage(msg);
	}
	
	public Module getModule(String pName)
	{
		if (!mInit || mInitFailed)
		{
			Eventlog.e(TAG, "public method on SystemTapService called, but service is not initialized!");
			return null;
		}
		
		return mModuleManagement.getModule(pName);
	}
	
	public Collection<Module> getModules()
	{
		if (!mInit || mInitFailed)
		{
			Eventlog.e(TAG, "public method on SystemTapService called, but service is not initialized!");
			return null;
		}
		
		return mModuleManagement.getModules();
	}
	
	public File[] getLogFiles(final String pModulename)
	{
		if (!mInit || mInitFailed)
		{
			Eventlog.e(TAG, "public method on SystemTapService called, but service is not initialized!");
			return null;
		}
		
		File logDir = new File(Config.STAP_LOG_ABSOLUTE_PATH);
		File logFiles[] = logDir.listFiles(new FileFilter()
		{
			
			@Override
			public boolean accept(File pathname)
			{
				if (pathname.isFile())
				{
					return pathname.getName().startsWith(pModulename) && pathname.getName().endsWith(".txt");
				}
				return false;
			}
		});	
		return logFiles;
	}
	
	public File[] getOutputFiles(final String pModulename)
	{
		if (!mInit || mInitFailed)
		{
			Eventlog.e(TAG, "public method on SystemTapService called, but service is not initialized!");
			return null;
		}	
		return mSystemTapHandler.getOutputFiles(pModulename);
	}
	
	public void deleteAllLogFiles(String pModulename) {
		Message msg = mSystemTapHandler.obtainMessage();
		msg.what = SystemTapHandler.DELETE_LOG_FILE;
		Bundle data = new Bundle();
		data.putString(SystemTapHandler.MODULENAME_ID, pModulename);
		msg.setData(data);
		mSystemTapHandler.sendMessage(msg);
	}
	
	public void deleteLogFile(String pModulename, String pFilename) {
		Message msg = mSystemTapHandler.obtainMessage();
		msg.what = SystemTapHandler.DELETE_LOG_FILE;
		Bundle data = new Bundle();
		data.putString(SystemTapHandler.MODULENAME_ID, pModulename);
		data.putString(SystemTapHandler.FILENAME_ID, pFilename);
		msg.setData(data);
		mSystemTapHandler.sendMessage(msg);
	}
	
	public void deleteAllOutputFiles(String pModulename) {
		Message msg = mSystemTapHandler.obtainMessage();
		msg.what = SystemTapHandler.DELETE_OUTPUT_FILE;
		Bundle data = new Bundle();
		data.putString(SystemTapHandler.MODULENAME_ID, pModulename);
		msg.setData(data);
		mSystemTapHandler.sendMessage(msg);
	}
	
	public void deleteOutputFile(String pModulename, String pFilename) {
		Eventlog.e(TAG,"delete file="+pFilename+",module="+pModulename);
		Message msg = mSystemTapHandler.obtainMessage();
		msg.what = SystemTapHandler.DELETE_OUTPUT_FILE;
		Bundle data = new Bundle();
		data.putString(SystemTapHandler.MODULENAME_ID, pModulename);
		data.putString(SystemTapHandler.FILENAME_ID, pFilename);
		msg.setData(data);
		mSystemTapHandler.sendMessage(msg);
	}
	
	public void registerObserver(Observer pObserver)
	{
		if (!mInit || mInitFailed)
		{
			Eventlog.e(TAG, "public method on SystemTapService called, but service is not initialized!");
			return;
		}
		
		mModuleManagement.addObserver(pObserver);
	}
	
	public void unregisterObserver(Observer pObserver)
	{
		if (!mInit || mInitFailed)
		{
			Eventlog.e(TAG, "public method on SystemTapService called, but service is not initialized!");
			return;
		}
		
		mModuleManagement.deleteObserver(pObserver);
	}
	/* END - PUBLIC INTERFACE */
	
	private void initialize()
	{
		
		/* First create all required directories on sdcard */
		if (!Util.createDirecotryOnExternalStorage(Config.MAIN_PATH))
		{
			mInitFailed = true;
			return;
		}
		if (!Util.createDirecotryOnExternalStorage(Config.MODULES_PATH))
		{
			mInitFailed = true;
			return;
		}
		if (!Util.createDirecotryOnExternalStorage(Config.STAP_LOG_PATH))
		{
			mInitFailed = true;
			return;
		}
		if (!Util.createDirecotryOnExternalStorage(Config.STAP_OUTPUT_PATH))
		{
			mInitFailed = true;
			return;
		}
		if (!Util.createDirecotryOnExternalStorage(Config.STAP_RUN_PATH))
		{
			mInitFailed = true;
			return;
		}

		try
		{
			Eventlog.initialize(Config.LOG_ABSOLUTE_PATH);
		}
		catch (IOException e)
		{
			Log.e(TAG,"The eventlog could not be started: " + e.getMessage());
			Log.e(TAG,Config.LOG_ABSOLUTE_PATH);
		    Toast.makeText(this, this.getText(R.string.stap_service_start_failed), Toast.LENGTH_SHORT).show();
		}

		/* Second extract all included scripts or binaries to our private data directory */
		if (!Util.copyFileFromRAW(this, R.raw.stapio, Config.STAP_IO_NAME))
		{
			mInitFailed = true;
			return;
		}
		if (!Util.copyFileFromRAW(this, R.raw.staprun, Config.STAP_RUN_NAME))
		{
			mInitFailed = true;
			return;
		}
		if (!Util.copyFileFromRAW(this, R.raw.stap_merge, Config.STAP_MERGE_NAME))
		{
			mInitFailed = true;
			return;
		}
		if (!Util.copyFileFromRAW(this, R.raw.stapsh, Config.STAP_SH_NAME))
		{
			mInitFailed = true;
			return;
		}
		if (!Util.copyFileFromRAW(this, R.raw.start_stap, Config.STAP_SCRIPT_NAME))
		{
			mInitFailed = true;
			return;
		}
		if (!Util.copyFileFromRAW(this, R.raw.busybox, Config.BUSYBOX_NAME))
		{
			mInitFailed = true;
			return;
		}
		if (!Util.copyFileFromRAW(this, R.raw.kill, Config.KILL_SCRIPT_NAME))
		{
			mInitFailed = true;
			return;
		}

		HandlerThread thread = new HandlerThread("Service Worker",android.os.Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		
		mModuleManagement = new ModuleManagement();
		mModuleManagement.load();
		mSystemTapHandler = new SystemTapHandler(thread.getLooper(),this,mModuleManagement,Config.STAP_LOG_ABSOLUTE_PATH,Config.STAP_OUTPUT_ABSOLUTE_PATH,Config.MODULES_ABSOLUTE_PATH,Config.STAP_RUN_ABSOLUTE_PATH);
		mSystemTapBinder = new SystemTapBinder(this);
		mInit = true;
		
		Eventlog.d(TAG,"Make sure that no stap process is already running at service startup");
		LinkedList<String> list = new LinkedList<String>();
		list.add("pid=-1");
		list.add("busyboxdir=" + this.getFilesDir().getParent());
		list.add(":q!");
		Util.runCmdAsRoot(this.getFilesDir().getParent() + File.separator + Config.KILL_SCRIPT_NAME, list);
		
		Notification notification = new Notification(R.drawable.ic_launcher, this.getText(R.string.stap_service_started),System.currentTimeMillis());
		Intent targetIntent = new Intent(this,SystemTapActivity.class);
		//targetIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		targetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, targetIntent, 0);
        notification.setLatestEventInfo(this, this.getText(R.string.stap_service_running),this.getText(R.string.stap_service_detail_info), contentIntent);
        this.startForeground(SystemTapService.NOTIFICATION_ID, notification);
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (settings.getBoolean(this.getString(R.string.pref_restore_modules), false)) {
        	this.restoreModules();
        } else {
			Eventlog.d(TAG,"No restore wanted, reset running modules to stopped.");
        	// No restore, set all running modules to stopped
    		for (Module module : mModuleManagement.getModules()) {
    			if (module.getStatus() == ModuleStatus.RUNNING) {
    				Eventlog.e(TAG,module.getName());
    				mModuleManagement.updateModuleStatus(module.getName(),ModuleStatus.STOPPED);
    			}
    		}
        }
	}
	
	private void stopAllModules()
	{
		Eventlog.d(TAG, "Stopping all modules...");
		for(Module module:mModuleManagement.getModules())
		{
			if (module.getStatus() == ModuleStatus.RUNNING)
			{
				Eventlog.d(TAG, "Stopping module: " + module.getName());
				this.stopModule(module.getName());
			}
		}		
	}
	
	private void restoreModules() {
		Eventlog.d(TAG,"Restoring modules...");
		for (Module module : mModuleManagement.getModules()) {
			if (module.getStatus() == ModuleStatus.RUNNING) {
				Eventlog.d(TAG,"Module " + module.getName() + " was running on last shutdown. Restarting...");
				this.startModule(module.getName());
			} else {
				Eventlog.d(TAG,"Module " + module.getName() + " was not running on last shutdown.");
			}
		}
	}
}
