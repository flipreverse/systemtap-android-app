package edu.udo.cs.ess.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import android.util.Log;

/**
 * @author alex
 *
 */
public class Eventlog
{
	public static final String TAG = Eventlog.class.getSimpleName();
	private static final int WRITEBACK_THRESHOLD = 150;
	private static final String SUFFIX = "_applog.txt";

	private static LinkedList<Event> mEvents = null;
	
	private static CrashHandler mCrashHandler = null;
	private static UncaughtExceptionHandler mDefaultUncaughtExceptionHandler = null;
	
	private static boolean mRunning = true;
	private static Thread mWritebackThread = null;
	private static Semaphore mWritebackLock = null;
	private static File mOutputFile = null;
	private static PrintWriter mOutputStream = null;
	
	private static int mFileLogLevel = Event.VERBOSE;
	private static int mOutLogLevel = Event.VERBOSE;
	private static long mStartTime = -1;
	
	public synchronized static void initialize(String pPath) throws IOException
	{
		if (pPath == null)
		{
			throw new IllegalArgumentException("Neither logger nor path have to be null");
		}
		
		if (!isInitialized())
		{
			mStartTime = System.currentTimeMillis();
			
			File temp = new File(pPath);
			if (!temp.exists())
			{
				temp.mkdirs();
			}
			temp = null;
			mOutputFile = new File(pPath + File.separator + Eventlog.generateTempOutputName());
			if (!mOutputFile.createNewFile())
			{
				String errMsg = "Could not create the desired outputfile (" +  mOutputFile.getAbsolutePath() + "), isFile=" + mOutputFile.isFile() + ", canWrite=" + mOutputFile.canWrite();
				mOutputFile = null;
				throw new IOException(errMsg);
			}

			mWritebackLock = new Semaphore(0);
			mEvents = new LinkedList<Event>();
			mOutputStream = new PrintWriter(new FileOutputStream(mOutputFile));
			mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
			mCrashHandler = new CrashHandler(mDefaultUncaughtExceptionHandler);
			Thread.setDefaultUncaughtExceptionHandler(mCrashHandler);
			
			mRunning = true;
			mWritebackThread = new Thread(new Runnable()
			{
				public void run()
				{
					Eventlog.doWriteback();
				}
			});
			mWritebackThread.start();
		}
		else
		{
			Log.e(TAG, "Eventlog is already initialized");
		}
	}
	
	public synchronized static void shutdown()
	{
		if (!isInitialized())
		{
			return;
		}

		Thread.setDefaultUncaughtExceptionHandler(mDefaultUncaughtExceptionHandler);
		mCrashHandler = null;
		
		/* Stop the runaway loop und wake up the writeback thread */
		mRunning = false;
		mWritebackLock.release();
		/* Wait until the writeback thread has finished */
		try
		{
			mWritebackThread.join();
		}
		catch (InterruptedException e)
		{
			Log.e(TAG, "Cannot wait for writeback thread to finish: " + e.getMessage());
		}
		
		mWritebackThread = null;
		mWritebackLock = null;
		try
		{
			mOutputStream.flush();
		}
		catch (Exception e)
		{
			Log.e(TAG, "Cannot flush the mOutputStream: " + e.getMessage());
		}
		try
		{
			mOutputStream.close();
		}
		catch (Exception e)
		{
			Log.e(TAG, "Cannot close the mOutputStream: " + e.getMessage());
		}
		mOutputStream = null;
		/* Rename the logfile -- the filename now contains start and end time  */
		if (!mOutputFile.renameTo(new File(mOutputFile.getParent() + File.separator + Eventlog.generateOutputName())))
		{
			Log.e(TAG, "Cannot rename outputfile");
		}
		mOutputFile = null;
		mEvents = null;	
	}
	
	public synchronized static void v(String pTag, String pMsg)
	{
		Eventlog.doLog(Event.VERBOSE, pTag, pMsg);
	}
	
	public synchronized static void d(String pTag, String pMsg)
	{
		Eventlog.doLog(Event.DEBUG, pTag, pMsg);
	}
	
	public synchronized static void i(String pTag, String pMsg)
	{
		Eventlog.doLog(Event.INFO, pTag, pMsg);
	}
	
	public synchronized static void w(String pTag, String pMsg)
	{
		Eventlog.doLog(Event.WARNING, pTag, pMsg);
	}

	public synchronized static void e(String pTag, String pMsg)
	{
		Eventlog.doLog(Event.ERROR, pTag, pMsg);
	}

	public synchronized static void printStackTrace(String pTag, Throwable pCause)
	{
		Eventlog.e(pTag,pCause.toString());
		for (StackTraceElement elem : pCause.getStackTrace())
		{
			Eventlog.e(pTag, "at " + elem.toString());
		}
	}
	
	
	/**
	 * Set an platform-specific exception handler. The Eventlog assumes that if set the exception handler will terminate the application.
	 * If not the Eventlog has to be reinitialized!
	 * @param pUncaughtExceptionHandler
	 * @see Eventlog#initialize(Logger, String)
	 */
	public synchronized static void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler pUncaughtExceptionHandler) { 	mCrashHandler.setPlatformUncaughtExceptionHandler(pUncaughtExceptionHandler); }
	
	/**
	 * Get the current log level for messages written to the output file
	 * @return a value between Event.ERROR and Event.DEBUG
	 * @see Event#ERROR
	 * @see Event#WARNING
	 * @see Event#INFO
	 * @see Event#DEBUG
	 * @see Event#VERBOSE
	 */
	public synchronized static int getFileLogLevel() { return mFileLogLevel; }

	/**
	 * Get the current log level for messages written to standard out
	 * @return a value between Event.ERROR and Event.DEBUG
	 * @see Event#ERROR
	 * @see Event#WARNING
	 * @see Event#INFO
	 * @see Event#DEBUG
	 * @see Event#VERBOSE
	 */
	public synchronized static int getOutLogLevel() { return mOutLogLevel; }
	
	/**
	 * Set the log level for messages written to the output file
	 * @param pLevel the new log level
	 * @see Event#ERROR
	 * @see Event#WARNING
	 * @see Event#INFO
	 * @see Event#DEBUG
	 * @see Event#VERBOSE
	 */
	public synchronized static void setFileLogLevel(int pLevel) { mFileLogLevel = pLevel; }

	/**
	 * Set the log level for messages written to standard out
	 * @param pLevel the new log level
	 * @see Event#ERROR
	 * @see Event#WARNING
	 * @see Event#INFO
	 * @see Event#DEBUG
	 * @see Event#VERBOSE
	 */
	public synchronized static void setOutLogLevel(int pLevel) { mOutLogLevel = pLevel; }
	
	/**
	 * Creates a new list containing all events having a level greater or equal to pLevel
	 * @param pLevel the desired level
	 * @return 
	 */
	public synchronized static LinkedList<Event> getEvents(int pLevel)
	{
		LinkedList<Event> temp = new LinkedList<Event>();
		Event cur = null;
		
		if (mEvents == null)
		{
			return temp;
		}
		
		synchronized(mEvents)
		{
			ListIterator<Event> iter = mEvents.listIterator();
			while (iter.hasNext())
			{
				cur = iter.next();
				if (cur.getLevel() >= pLevel)
				{
					temp.addLast(cur);
				}
			}
		}
		return temp;
	}

	
	private static void doLog(int pLevel,String pTag, String pMsg)
	{
		/* Do nothing, if the Eventlog isn't initialized */
		if (!isInitialized())
		{
			return;
		}

		if (pLevel >= mFileLogLevel)
		{
			synchronized(mEvents)
			{
				mEvents.addLast(new Event(pLevel,System.currentTimeMillis(),pTag,pMsg));
			}
		}
		if (pLevel >= mOutLogLevel)
		{
			switch (pLevel)
			{
				case Event.ERROR:
					Log.e(pTag, pMsg);break;
				case Event.WARNING:
					Log.w(pTag, pMsg);break;
				case Event.INFO:
					Log.i(pTag, pMsg);break;
				case Event.DEBUG:
					Log.d(pTag, pMsg);break;
				case Event.VERBOSE:
					Log.v(pTag, pMsg);break;
				default:
				{
					Log.e(TAG,"doLog(): unknown level");
				}
			}
		}
		checkOverflow();
	}
	
	private static boolean isInitialized()
	{
		return mEvents != null;
	}
	
	private static void checkOverflow()
	{
		synchronized (mEvents)
		{
			/* the event list contains more than WRITEBACK_THRESHOLD events. Notify the writeback thread to begin with its job. */
			if (mEvents.size() >= Eventlog.WRITEBACK_THRESHOLD)
			{
				Log.d(TAG, "number of events exceeded limit (" + mEvents.size() + ") --> triggered writeback");
				mWritebackLock.release();
			}
		}
	}
	
	/**
	 * Generates the final name of the output file.
	 * @return a filename
	 */
	private static String generateOutputName()
	{
		return mStartTime + "_" + System.currentTimeMillis() + SUFFIX;
	}

	/**
	 * Generates a temporary name for the output file.
	 * @return a filename
	 */
	private static String generateTempOutputName()
	{
		return mStartTime + SUFFIX;
	}
	
	/**
	 * Wait until a thread signals that thenumber of events exceeded the threshold
	 */
	private static void doWriteback()
	{
		while (mRunning)
		{
			try
			{
				/* Wait until there are at least WRITEBACK_THRESHOLD entries in the event list */
				mWritebackLock.acquire();
			}
			catch (InterruptedException e)
			{
				Log.e(TAG, "writeback thread got interrupted: " + e.getMessage());
				continue;
			}
			Log.d(TAG, "Woke up...");
			Eventlog.writeEvents();		
		}
		/* Writeback events that were entered between a call to shutdown() and the  */
		Eventlog.writeEvents();
	}
	
	/**
	 * Acquire the lock for the event list, iterate through all elements, writes them to the file and clear the list.
	 */
	private static void writeEvents()
	{
		Event cur = null;
		/* We need exclusive access to the event list */
		synchronized(mEvents)
		{
			ListIterator<Event> iter = mEvents.listIterator();
			while (iter.hasNext())
			{
				cur = iter.next();
				try
				{
					mOutputStream.println(cur.toString());
				}
				catch (Exception e)
				{
					Log.e(TAG, "error while writing to outputfile: " + e.getMessage());
				}
			}
			mOutputStream.flush();
			mEvents.clear();
		}
	}
}
