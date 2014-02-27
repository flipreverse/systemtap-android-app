package com.systemtap.android.service;

import com.systemtap.android.net.protocol.SystemTapMessage.ModuleStatus;

public class Module
{	
	private String mName;
	private ModuleStatus mStatus;
	
	public Module(String pName)
	{
		mName = pName;
		mStatus = ModuleStatus.STOPPED;
	}
	
	public String getName() { return mName; }
	
	public void setStatus(ModuleStatus pStatus) { mStatus = pStatus; }
	
	public ModuleStatus getStatus() { return mStatus; }
}
