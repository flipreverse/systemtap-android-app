package edu.udo.cs.ess.systemtap.service;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;

import android.os.FileObserver;
import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.Config;

public class ModuleManagement extends Observable
{
	private static final String TAG = ModuleManagement.class.getSimpleName();
	private FileObserver mModuleDir;
	private ConcurrentHashMap<String, Module> mModules;
	
	public ModuleManagement()
	{
		mModuleDir = new FileObserver(Config.MODULES_ABSOLUTE_PATH,FileObserver.MOVED_FROM|FileObserver.MOVED_TO|FileObserver.CREATE|FileObserver.DELETE)
		{
			@Override
			public void onEvent(int event, String path)
			{
				Eventlog.d(ModuleManagement.TAG, "Something changed in " + Config.MODULES_ABSOLUTE_PATH + "(" + path + "), event=" + event);
				ModuleManagement.this.updateModules();
			}
		};
		mModuleDir.startWatching();
		mModules = new ConcurrentHashMap<String, Module>();
	}
	
	public synchronized Collection<Module> getModules()
	{
		return mModules.values();
	}
	
	public synchronized Module getModule(String pName)
	{
		return mModules.get(pName);
	}
	
	public synchronized Module createModule(String pName)
	{
		Module module = mModules.get(pName);
		if (module == null)
		{
			module = new Module(pName);
			mModules.put(pName, module);
		}
		else
		{
			Eventlog.e(TAG, "Try to insert existing module");
		}
		return module;
	}
	
	public synchronized void updateModuleStatus(String pName, Module.Status pStatus)
	{
		Module module = mModules.get(pName);
		if (module != null)
		{
			module.setStatus(pStatus);
			this.setChanged();
			this.notifyObservers();
			this.clearChanged();
		}
		else
		{
			Eventlog.e(TAG, "Try to update non-existing module: " + pName);
		}
	}
	
	public synchronized void updateModules()
	{
		Eventlog.d(TAG, "Updating module list...");
		File moduleDir = new File(Config.MODULES_ABSOLUTE_PATH);
		File moduleFiles[] = moduleDir.listFiles(new FilenameFilter()
		{
			@Override
			public boolean accept(File dir, String filename)
			{
				return filename.endsWith(Config.MODULE_EXT);
			}
		});
		
		Eventlog.d(TAG, "Got file list from " + Config.MODULES_ABSOLUTE_PATH + ", testing for new modules");
		for(File moduleFile: moduleFiles)
		{
			String filename = moduleFile.getName();
			String modulename = filename.substring(0, filename.indexOf(Config.MODULE_EXT));
			Eventlog.d(TAG, "Does " + modulename + " exist in database?");
			if (mModules.get(modulename) == null)
			{
				Eventlog.d(TAG,"No, create a new module (" + modulename + ")");
				this.createModule(modulename);
			}
			else
			{
				//TODO: do a md5 check of file content
				Eventlog.d(TAG,"Yes, doing nothing");
			}
		}
		
		Eventlog.d(TAG,"Checking if all modules in database exist");
		for(Module module:mModules.values())
		{
			File moduleFile = new File(Config.MODULES_ABSOLUTE_PATH + File.separator + module.getName() + Config.MODULE_EXT);
			Eventlog.d(TAG, "Does module " + module.getName() + " exist in module directory?");
			if (!moduleFile.exists())
			{
				if (module.getStatus() == Module.Status.RUNNING)
				{
					Eventlog.d(TAG, "No, it does not exist, but it will not be deleted because it is currently running.");
				}
				else
				{
					Eventlog.d(TAG, "No, it does not exist. Delete it from database.");
					mModules.remove(module.getName());
				}
			}
			else
			{
				//TODO: do a md5 check of file content
				Eventlog.d(TAG, "Yes, it does.");
			}
		}
	}
}
