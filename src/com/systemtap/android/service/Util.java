package com.systemtap.android.service;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.systemtap.android.Config;
import com.systemtap.android.net.SystemTapMessage.ModuleStatus;

public class Util
{
	private static final String TAG = Util.class.getSimpleName();
	
	/**
	 * Execute the command {@link pCmd} on a shell 
	 * @param pCmd
	 * @return 0 if all went fine, -1 otherwise 
	 */
	public static int runCmd(String pCmd)
	{
		Runtime runtime;
		Process process;
		int retval = -1;
		
		try
		{
			runtime = Runtime.getRuntime();
			process = runtime.exec(new String[]{"sh","-c",pCmd});
			retval = process.waitFor();
		}
		catch (Exception e)
		{
			Log.e(TAG, "Can't execute cmd \"" + pCmd + "\":" + e.getMessage());
		}
		
		return retval;
	}
	
	/**
	 * Execute the command {@link pCmd} on a shell as root sending each string in {@link pInput} as a separated line stdin of the new process.
	 * @param pCmd
	 * @param pInput
	 * @return 0 if all went fine, -1 otherwise
	 */
	public static int runCmdAsRoot(String pCmd, List<String> pInput)
	{
		Runtime runtime;
		Process process;
		int retval = -1;
		
		Log.i(TAG, "Try to run \"" + pCmd + "\" as root");
		try
		{
			runtime = Runtime.getRuntime();
			process = runtime.exec(new String[]{"su","-c",pCmd});
			if (pInput != null && pInput.size() > 0)
			{
				PrintWriter out = new PrintWriter(process.getOutputStream(),true);
				Iterator<String> iter = pInput.iterator(); 
				while (iter.hasNext())
				{
					String next = iter.next();
					out.write(next + "\n");
				}
				out.flush();
				out.close();
			}
			retval = process.waitFor();
			if (retval != 0)
			{
				DataInputStream in = new DataInputStream(process.getErrorStream());
				String line = null;
				Log.e(TAG, "Error while running \"" + pCmd + "\" as root:");
				while ((line = in.readLine()) != null)
				{
					Log.e(TAG,line);
				}				
				in.close();
			}
			else
			{
				Log.i(TAG, "\"" + pCmd + "\" terminated successfully.");
			}
		}
		catch (Exception e)
		{
			Log.e(TAG,"Can't execute \"" + pCmd + "\" as root: " + e.getMessage());
		}
		
		return retval;
	}
	
	/**
	 * Creates the direcotry specified by {@link pDirecotry}.
	 * It creates all parent dirs (behaves like "mkdir -p").
	 * @param pDirecotry
	 */
	public static boolean createDirecotryOnExternalStorage(String pDirectory)
	{
		File root = null, file = null;
		
		root = Environment.getExternalStorageDirectory();
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
		{
			file = new File(root + File.separator + pDirectory);
			if (!file.exists())
			{
				return file.mkdirs();
			}
			else
			{
				return true;
			}
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Copy the file included in the apk-file (identified by {@link pRAWID}) to the apps private data dir as file {@link pFilename}
	 * @param pContext the applicatins context get access to the raw resource
	 * @param pRAWID
	 * @param pFilename the filename the raw resource should be copied to
	 * @return true if all went fine, false otherwise
	 */
	public static boolean copyFileFromRAW(Context pContext, int pRAWID, String pFilename)
	{
		InputStream is = null;
		OutputStream os = null;
		File file = null;
		byte[] buffer = new byte[1024];
		int count  = 0;
		
		try
		{
			file = new File(pContext.getFilesDir().getParent() + File.separator + pFilename);
			is = pContext.getApplicationContext().getResources().openRawResource(pRAWID);
			if (file.exists()) {
				byte md5Old[], md5New[];
				try {
					md5Old = Util.mkMD5Hash(file.getAbsolutePath());
				} catch(Exception e) {
					Log.e(TAG,"copyFileFromRAW(): Can't create md5 hash of existing file (id = " + pRAWID + "): " + e + " --- " + e.getMessage());
					return false;
				}
				try {
					md5New = Util.mkMD5Hash(is);
					is.close();
					is = pContext.getApplicationContext().getResources().openRawResource(pRAWID);
				} catch(Exception e) {
					Log.e(TAG,"copyFileFromRAW(): Can't create md5 hash of raw file (" + file.getAbsolutePath() + "): " + e + " --- " + e.getMessage());
					return false;
				}
				if (Arrays.equals(md5Old, md5New)) {
					Log.d(TAG, "copyFileFromRAW(): Existing file (" + file.getAbsolutePath() + ") and raw file are equal. Doing nothing!");
					return true;
				} else {
					Log.d(TAG, "copyFileFromRAW(): Existing file (" + file.getAbsolutePath() + ") and raw file are not equal. Start copying...");
				}
			}
			
			os = new FileOutputStream(file);
			while ((count = is.read(buffer)) > 0)
			{
				os.write(buffer, 0, count);
			}
			
			is.close();
			os.close();
			if (!file.setExecutable(true, false))
			{
				Log.e(TAG, "copyFileFromRAW(): Could not make file executeable: " + pFilename);
				return false;
			}
			return true;
		}
		catch (Exception e)
		{
			Log.e(TAG,"Can't copy raw resource to file \"" + pFilename + "\": " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * Get all pids of {@link pCmd}.
	 * @param pContext 
	 * @param pCmd
	 * @return if no process with name {@link pCmd} is running, an empty array will be returned. otherwise the array contains all process ids of this command. if an error occurs, null will be returned.
	 */
	public static List<Integer> getProcessIDs(Context pContext, String pCmd)
	{
		Runtime runtime = null;
		Process process = null;
		
		LinkedList<Integer> list = new LinkedList<Integer>();
		
		try
		{
			runtime = Runtime.getRuntime();
			process = runtime.exec(new String[]{pContext.getFilesDir().getParent() + File.separator + Config.BUSYBOX_NAME,"pidof",pCmd});
			DataInputStream in = new DataInputStream(process.getInputStream());
			
			if (process.waitFor() != 0)
			{
				return null;
			}

			Log.d(TAG,"Parsing pidof output...");
			String line = null;
			StringTokenizer tokenizer = null;
			
			while ((line = in.readLine()) != null)
			{
				tokenizer = new StringTokenizer(line, " ");
				while (tokenizer.hasMoreTokens())
				{
					list.add(Integer.valueOf(tokenizer.nextToken()));
				}
			}
			in.close();
			Log.d(TAG,"Got " + list.size() + " pids from pidof");

			return list;
		}
		catch(Exception e)
		{
			Log.e(TAG,"Can't retrieve process ids of \"" + pCmd + "\": " + e.getMessage());
		}		
		return null;
	}
	
	/**
	 * Reads {@link pPidFile}s content, which acutally should be a process id.   It checks wether it belongs to a running process.
	 * @param pContext the activities context
	 * @param pPidFile 
	 * @return true if the pid belongs to a running process
	 * @throws IOException
	 */
	public static boolean isPidFilePidValid(Context pContext, File pPidFile) throws IOException
	{
		int pid = -1;
		DataInputStream in = new DataInputStream(new FileInputStream(pPidFile));
		pid = Integer.valueOf(in.readLine());
		in.close();
		in = null;
		List<Integer> pids = Util.getProcessIDs(pContext,Config.STAP_IO_NAME);

		if (pids == null)
		{
			return false;
		}
		
		return pids.contains(pid);
	}
	
	/**
	 * First check if pid file exists. Second, read its content and query system wether the pid belongs a running process.
	 * @param pContext the activities context
	 * @param pModulename the modulename which status the caller wants to know
	 * @param pStatus the expected status
	 * @return Returns new module state
	 */
	public static ModuleStatus checkModuleStatus(Context pContext, String pModulename, ModuleStatus pStatus)
	{
		File pidFile = new File(Config.STAP_RUN_ABSOLUTE_PATH + File.separator + pModulename + Config.PID_EXT);
		if (pidFile.exists())
		{
			try
			{
				if (Util.isPidFilePidValid(pContext,pidFile))
				{
					switch (pStatus)
					{
						case RUNNING:
							Log.d(TAG,"module (" + pModulename + ") status is running and pid file exists. all fine. :-)");
							return ModuleStatus.RUNNING;
							
						case STOPPED:
							Log.e(TAG,"module (" + pModulename + ") status is stopped, but stap is running. Updating status...");
							return ModuleStatus.RUNNING;
							
						case CRASHED:
							Log.e(TAG,"module (" + pModulename + ") status is crasehd, but stap is running. Updating status...");
							return ModuleStatus.RUNNING;
					}
				}
				else
				{
					switch (pStatus)
					{
						case RUNNING:
							Log.e(TAG,"module (" + pModulename + ") status is running, but stap is not running. Updating status....");
							Log.e(TAG,"module (" + pModulename + ") status is crashed. Removing pid file: " + pidFile.delete());
							return ModuleStatus.CRASHED;
							
						case STOPPED:
							Log.d(TAG,"module (" + pModulename + ") status is stopped, but pidfile exsits. Deleting it: " + pidFile.delete());
							return ModuleStatus.STOPPED;
							
						case CRASHED:
							Log.d(TAG,"module (" + pModulename + ") status is crashed, but pidfile exsits. Deleting it: " + pidFile.delete());
							return ModuleStatus.CRASHED;
					}
				}
			}
			catch (IOException e)
			{
				Log.e(TAG,"Error reading pid file of module " + pModulename + ". Try again later.");
				return ModuleStatus.CRASHED;
			}
		}
		else
		{
			switch (pStatus)
			{
				case RUNNING:
					Log.e(TAG,"module (" + pModulename + ") status is running, but stap (no pid file) is not running. Updating status.... ");
					return ModuleStatus.CRASHED;
					
				case STOPPED:
					Log.d(TAG,"module (" + pModulename + ") status is stopped and no pid file exists. all fine. :-)");
					return ModuleStatus.STOPPED;
					
				case CRASHED:
					Log.d(TAG,"module (" + pModulename + ") status is crashed and no pid file exists. all fine. :-)");
					return ModuleStatus.CRASHED;
			}
		}
		/* Although i hate this kind of comments: this should never happen */
		Log.e(TAG,"checkModuleStatus() reached end of function. Module: " + pModulename + ", Status: " + pStatus);
		return ModuleStatus.STOPPED;
	}
	
	public static byte[] mkMD5Hash(String pFilename) throws NoSuchAlgorithmException, IOException {
		byte ret[];
		InputStream in = new FileInputStream(pFilename);
		
		ret = Util.mkMD5Hash(in);
		in.close();
		
		return ret;
	}
	
	public static byte[] mkMD5Hash(InputStream pIn) throws NoSuchAlgorithmException, IOException {
		MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
		int bytesRead = 0;
		byte buffer[] = new byte[512];
		do {
			bytesRead = pIn.read(buffer,0,buffer.length);
			if (bytesRead > 0) {
				digest.update(buffer, 0, bytesRead);
			}
		} while (bytesRead > 0);
		
	    return digest.digest();
	}
}
