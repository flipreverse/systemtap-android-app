package com.systemtap.android.service;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;

import android.os.FileObserver;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.systemtap.android.Config;
import com.systemtap.android.logging.Eventlog;
import com.systemtap.android.net.protocol.SystemTapMessage.ModuleStatus;

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
	
	public synchronized LinkedList<Module> getModules()
	{
		return new LinkedList<Module>(mModules.values());
	}

	public synchronized LinkedList<Module> getRunningModules() {
		LinkedList<Module> ret = new LinkedList<Module>();
	
		for (Module module : this.getModules()) {
			if (module.getStatus() == ModuleStatus.RUNNING) {
				ret.add(module);
			}
		}
		return ret;
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
			this.setChanged();
			this.notifyObservers(mModules.values());
			this.clearChanged();
		}
		else
		{
			Eventlog.e(TAG, "Try to insert existing module");
		}
		return module;
	}
	
	public synchronized void updateModuleStatus(String pName, ModuleStatus pStatus)
	{
		Module module = mModules.get(pName);
		if (module != null)
		{
			module.setStatus(pStatus);
			this.setChanged();
			this.notifyObservers(mModules.values());
			this.clearChanged();
		}
		else
		{
			Eventlog.e(TAG, "Try to update non-existing module: " + pName);
		}
	}
	
	public void save() {
		for (Module module : mModules.values()) {
			
			File moduleConfFile = new File(Config.MODULES_ABSOLUTE_PATH + File.separator + module.getName()  + Config.MODULE_CONF_FILE_EXT);
			try {
				JsonWriter writer = new JsonWriter(new FileWriter(moduleConfFile));
				writer.beginObject();
				writer.name(Config.MODULE_CONF_FILE_ENTRY_STATUS).value(module.getStatus().name());
				writer.endObject();
				writer.close();
			} catch (IOException e) {
				Eventlog.e(TAG,"save(): Can't save modules meta information: " + moduleConfFile.getAbsolutePath());
			}
		}
	}
	
	public void load() {
		this.updateModules(true);
	}
	
	public synchronized void updateModules(){
		this.updateModules(false);
	}
	
	private synchronized void updateModules(boolean pLoadConf) 
	{
		boolean changed = false;
		
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
			Module module = mModules.get(modulename);
			if (module == null)
			{
				Eventlog.d(TAG,"No, create a new module (" + modulename + ")");
				module = this.createModule(modulename);
				changed = true;
			}
			else
			{
				//TODO: do a md5 check of file content
				Eventlog.d(TAG,"Yes, doing nothing");
			}
			if (pLoadConf) {
				Eventlog.d(TAG,"Reading module conf file...");
				File moduleConfFile = new File(Config.MODULES_ABSOLUTE_PATH + File.separator + module.getName() + Config.MODULE_CONF_FILE_EXT);
				try {
					JsonReader reader = new JsonReader(new FileReader(moduleConfFile));
					reader.beginObject();
					while (reader.hasNext()) {
						String entryName = reader.nextName(), statusText = "";
						if (entryName.equalsIgnoreCase(Config.MODULE_CONF_FILE_ENTRY_STATUS)) {
							statusText = reader.nextString();
							if (statusText.equalsIgnoreCase(ModuleStatus.STOPPED.name())) {
								module.setStatus(ModuleStatus.STOPPED);
							} else if (statusText.equalsIgnoreCase(ModuleStatus.RUNNING.name())) {
								module.setStatus(ModuleStatus.RUNNING);
							} else if (statusText.equalsIgnoreCase(ModuleStatus.CRASHED.name())) {
								module.setStatus(ModuleStatus.CRASHED);
							}
						}
					}
					reader.endObject();
					reader.close();
				} catch (IOException e) {
					Eventlog.e(TAG,"updateModules(): Can't parse module config: " + moduleConfFile.getAbsolutePath());
				}
			}
		}
		
		Eventlog.d(TAG,"Checking if all modules in database exist");
		for(Module module:mModules.values())
		{
			File moduleFile = new File(Config.MODULES_ABSOLUTE_PATH + File.separator + module.getName() + Config.MODULE_EXT);
			Eventlog.d(TAG, "Does module " + module.getName() + " exist in module directory?");
			if (!moduleFile.exists())
			{
				if (module.getStatus() == ModuleStatus.RUNNING)
				{
					Eventlog.d(TAG, "No, it does not exist, but it will not be deleted because it is currently running.");
				}
				else
				{
					Eventlog.d(TAG, "No, it does not exist. Delete it from database.");
					mModules.remove(module.getName());
					changed = true;
				}
			}
			else
			{
				//TODO: do a md5 check of file content
				Eventlog.d(TAG, "Yes, it does.");
			}
		}
		
		if (changed)
		{
			this.setChanged();
			this.notifyObservers(mModules.values());
			this.clearChanged();
		}
	}
}
