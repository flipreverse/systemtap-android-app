package edu.udo.cs.ess.systemtap.service;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Observer;
import java.util.Timer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.Config;
import edu.udo.cs.ess.systemtap.R;
import edu.udo.cs.ess.systemtap.SystemtapActivity;

public class SystemTapService extends Service
{
	private static final String TAG = SystemTapService.class.getSimpleName();
	private static final int NOTIFICATION_ID = 0x4711;
	
	private SystemTapHandler mSystemTapHandler;
	private SystemTapBinder mSystemTapBinder;
	private ModuleManagement mModuleManagetment;
	private Timer mTimer;
	private SystemTapTimerTask mSystemtTapTimerTask;
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
		/*Message msg = service_handler.obtainMessage();
		msg.arg1 = startId;
		service_handler.sendMessage(msg);*/

	    return super.onStartCommand(intent, flags, startId);
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
		
	    mSystemtTapTimerTask.cancel();
	    mTimer.cancel();
	    mTimer.purge();
	    this.stopAllModules();
	    
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0) { return mSystemTapBinder; }
	
	/* BEGIN - PUBLIC INTERFACE */
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
	
	public Collection<Module> getModules()
	{
		if (!mInit || mInitFailed)
		{
			Eventlog.e(TAG, "public method on SystemTapService called, but service is not initialized!");
			return null;
		}
		
		return mModuleManagetment.getModules();
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
		
		File outputDir = new File(Config.STAP_OUTPUT_ABSOLUTE_PATH);
		File outputFiles[] = outputDir.listFiles(new FileFilter()
		{
			
			@Override
			public boolean accept(File pathname)
			{
				if (pathname.isFile())
				{
					return pathname.getName().startsWith(pModulename);
				}
				return false;
			}
		});	
		return outputFiles;
	}
	
	public void registerObserver(Observer pObserver)
	{
		if (!mInit || mInitFailed)
		{
			Eventlog.e(TAG, "public method on SystemTapService called, but service is not initialized!");
			return;
		}
		
		mModuleManagetment.addObserver(pObserver);
	}
	
	public void unregisterObserver(Observer pObserver)
	{
		if (!mInit || mInitFailed)
		{
			Eventlog.e(TAG, "public method on SystemTapService called, but service is not initialized!");
			return;
		}
		
		mModuleManagetment.deleteObserver(pObserver);
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
		
		mModuleManagetment = new ModuleManagement();
		mModuleManagetment.updateModules();
		mSystemTapHandler = new SystemTapHandler(thread.getLooper(),this,mModuleManagetment);
		mSystemTapBinder = new SystemTapBinder(this);
		mTimer = new Timer("SystemTapTimer",true);
		mSystemtTapTimerTask = new SystemTapTimerTask(mModuleManagetment,this);
		mTimer.schedule(mSystemtTapTimerTask, 10 * 1000, Config.TIMER_TASK_PERIOD);
		mInit = true;
		
		Eventlog.d(TAG,"Make sure that no stap process is already running at service startup");
		LinkedList<String> list = new LinkedList<String>();
		list.add("pid=-1");
		list.add("busyboxdir=" + this.getFilesDir().getParent());
		list.add(":q!");
		Util.runCmdAsRoot(this.getFilesDir().getParent() + File.separator + Config.KILL_SCRIPT_NAME, list);
		
		Notification notification = new Notification(R.drawable.ic_launcher, this.getText(R.string.stap_service_started),System.currentTimeMillis());
		Intent target_intent = new Intent(this,SystemtapActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, target_intent, 0);
        notification.setLatestEventInfo(this, this.getText(R.string.stap_service_running),this.getText(R.string.stap_service_detail_info), contentIntent);
        this.startForeground(SystemTapService.NOTIFICATION_ID, notification);
	}
	
	private void stopAllModules()
	{
		Eventlog.d(TAG, "Stopping all modules...");
		for(Module module:mModuleManagetment.getModules())
		{
			if (module.getStatus() == Module.Status.RUNNING)
			{
				Eventlog.d(TAG, "Stopping module: " + module.getName());
				this.stopModule(module.getName());
			}
		}		
	}
}
