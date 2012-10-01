package edu.udo.cs.ess.systemtap.service;

import android.os.Binder;


public class SystemTapBinder extends Binder
{
	private SystemTapService mSystemTapService;
	
	public SystemTapBinder(SystemTapService pSystemTapService)
	{
		mSystemTapService = pSystemTapService;
	}
	
	public SystemTapService getService() { return mSystemTapService; }
}
