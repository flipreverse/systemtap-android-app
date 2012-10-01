package edu.udo.cs.ess.systemtap.service;

public class Module
{
	public enum Status
	{
		STOPPED, RUNNING, CRASHED;
	};
	
	private String mName;
	private Status mStatus;
	
	public Module(String pName)
	{
		mName = pName;
		mStatus = Status.STOPPED;
	}
	
	public String getName() { return mName; }
	
	public void setStatus(Status pStatus) { mStatus = pStatus; }
	
	public Status getStatus() { return mStatus; }
}
