package com.systemtap.android;

import java.io.File;

import android.os.Environment;

public class Config
{
	
	private static final String MAIN_DIR = "systemtap";	
	private static final String MODULES_DIR = "modules";	
	private static final String STAP_OUTPUT_DIR = "stap_output";	
	private static final String STAP_LOG_DIR = "stap_log";	
	private static final String STAP_RUN_DIR = "stap_run";	
	private static final String LOG_DIR = "log";
	
	public static final String MEDIA_PATH = "/sdcard/"/*Environment.getExternalStorageDirectory().getAbsolutePath()*/;
	public static final String MAIN_PATH = MAIN_DIR;
	public static final String MAIN_ABSOLUTE_PATH = MEDIA_PATH + File.separator + MAIN_DIR;
	
	public static final String MODULES_PATH = MAIN_DIR + File.separator + MODULES_DIR;
	public static final String MODULES_ABSOLUTE_PATH = MEDIA_PATH + File.separator + MAIN_DIR + File.separator + MODULES_DIR;
	
	public static final String STAP_OUTPUT_PATH = MAIN_DIR + File.separator + STAP_OUTPUT_DIR;
	public static final String STAP_OUTPUT_ABSOLUTE_PATH = MEDIA_PATH + File.separator + MAIN_DIR + File.separator + STAP_OUTPUT_DIR;
	
	public static final String STAP_LOG_PATH = MAIN_DIR + File.separator + STAP_LOG_DIR;
	public static final String STAP_LOG_ABSOLUTE_PATH = MEDIA_PATH + File.separator + MAIN_DIR + File.separator + STAP_LOG_DIR;
	
	public static final String STAP_RUN_PATH = MAIN_DIR + File.separator + STAP_RUN_DIR;
	public static final String STAP_RUN_ABSOLUTE_PATH = MEDIA_PATH + File.separator + MAIN_DIR + File.separator + STAP_RUN_DIR;
	
	public static final String LOG_PATH = MAIN_DIR + File.separator + LOG_DIR;
	public static final String LOG_ABSOLUTE_PATH = MEDIA_PATH + File.separator + MAIN_DIR + File.separator + LOG_DIR;
	
	public static final String MODULE_EXT = ".ko";
	
	public static final String PID_EXT = ".pid";
	
	public static final String STAP_CONFIG = "stap.config";
	
	public static final String STAP_RUN_NAME = "staprun";
	
	public static final String STAP_IO_NAME = "stapio";

	public static final String STAP_MERGE_NAME = "stapmerge";

	public static final String STAP_SH_NAME = "stapsh";

	public static final String STAP_SCRIPT_NAME = "start_stap.sh";
	
	public static final String KILL_SCRIPT_NAME = "kill.sh";
	
	public static final String KILL_CONFIG = "kill.config";
	
	public static final String BUSYBOX_NAME = "busybox";
	
	public static final String MODULE_CONF_FILE_EXT = ".txt";
	
	public static final String MODULE_CONF_FILE_ENTRY_STATUS = "status";
	
	public static final int TIMER_TASK_PERIOD = 5 * 60 * 1000;
}
