package com.systemtap.android.logging;

public class Event
{
	public static final int VERBOSE = 0x1;
	public static final int DEBUG = 0x2;
	public static final int INFO = 0x4;
	public static final int WARNING = 0x8;
	public static final int ERROR = 0x10;

	public static final String DELIMITER = ";";
	public static final String STRING_QUOTE = "\"";
	
	private int mLevel;
	private long mTimestamp;
	private String mTag;
	private String mText;
	
	public Event(int pLevel, long pTimestamp, String pTag, String pText)
	{
		this.mLevel = pLevel;
		this.mTimestamp = pTimestamp;
		this.mTag = pTag;
		this.mText = pText;
	}
	
	public int getLevel() { return this.mLevel; }
	
	public long getTimestamp() { return this.mTimestamp; }
	
	public String getTag()  { return this.mTag; }
	
	public String getText() { return this.mText; }
	
	@Override
	public String toString()
	{	
		return Event.STRING_QUOTE + this.mTimestamp + Event.STRING_QUOTE + Event.DELIMITER + Event.STRING_QUOTE + Event.logLevelToString(this.mLevel) + Event.STRING_QUOTE + Event.DELIMITER + Event.STRING_QUOTE + this.mTag + Event.STRING_QUOTE + Event.DELIMITER + Event.STRING_QUOTE + this.mText + Event.STRING_QUOTE;
	}
	
	public static String logLevelToString(int pLevel)
	{
		String ret = "unknown";
		
		switch (pLevel)
		{
			case Event.VERBOSE:
				ret = "verbose";
				break;

			case Event.DEBUG:
				ret = "debug";
				break;

			case Event.INFO:
				ret = "info";
				break;

			case Event.WARNING:
				ret = "warning";
				break;

			case Event.ERROR:
				ret = "error";
				break;
		}
		
		return ret;
	}
}
