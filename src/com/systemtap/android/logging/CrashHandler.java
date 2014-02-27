package com.systemtap.android.logging;

import java.lang.Thread.UncaughtExceptionHandler;


public class CrashHandler implements UncaughtExceptionHandler
{
	private static final String TAG = Eventlog.TAG + ".CrashHandler";
	
	private boolean mCrashing;
	private UncaughtExceptionHandler mOldUncaughtExceptionHandler;
	private UncaughtExceptionHandler mPlatformUncaughtExceptionHandler;
	
	public CrashHandler(UncaughtExceptionHandler pUncaughtExceptionHandler)
	{
		this.mOldUncaughtExceptionHandler = pUncaughtExceptionHandler;
		this.mPlatformUncaughtExceptionHandler = null;
		this.mCrashing = false;
	}
	
	public void setPlatformUncaughtExceptionHandler(UncaughtExceptionHandler pUncaughtExceptionHandler)
	{
		this.mPlatformUncaughtExceptionHandler = pUncaughtExceptionHandler;
	}
	
	public void uncaughtException(Thread pThread, Throwable pThrowable)
	{
		/* Avoid infinity loops if crash handling crashes as well */
		if (this.mCrashing)
		{
			return;
		}

		this.mCrashing = true;
		Eventlog.e(TAG,"Uncaught exception in " + pThread.getThreadGroup().getName() + ", Error:" +  pThrowable.toString());
		for(StackTraceElement elem: pThrowable.getStackTrace())
		{
			Eventlog.e(TAG,"\t at " + elem.toString());
		}
		Throwable cause = pThrowable.getCause();
		if (cause != null)
		{
			Eventlog.e(TAG, "Caused by:" + cause.toString());

			for(StackTraceElement elem: cause.getStackTrace())
			{
				Eventlog.e(TAG,"\t at " + elem.toString());
			}
		}
		Eventlog.shutdown();
		
		if (this.mPlatformUncaughtExceptionHandler != null)
		{
			this.mPlatformUncaughtExceptionHandler.uncaughtException(pThread, pThrowable);
		}
		else
		{
			/* Just execute the default handler if no platformspecific one is set */
			if (this.mOldUncaughtExceptionHandler != null)
			{
				this.mOldUncaughtExceptionHandler.uncaughtException(pThread, pThrowable);
			}
			else
			{
				/* No handler present */
				System.exit(1);
			}
		}
		/* reset internal state variable -- just do it, if the called handler does *not* temrinate the application */
		this.mCrashing = false;
	}

}
