/*
 * Copyright 2012 Alexander Lochmann
 *
 * This file is part of SystemTap4Android.
 *
 * SystemTap4Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SystemTap4Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SystemTap4Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.systemtap.android.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
	 * Execute the command {@link pCmd} on a shell as root.
	 * @param pCmd
	 * @param detached run the command asynchronously
	 * @return An instance of CmdStatus containing the output of stdout and stderr if any. returnCode is -1 if anything unusual happened
	 */
	public static CmdStatus runCmdAsRoot(String pCmd, boolean detached)
	{
		Runtime runtime;
		Process process;
        CmdStatus ret = new CmdStatus();

		String cmd = pCmd;
		if (detached) {
			cmd += " &";
		}
		Log.i(TAG, "Try to run \"" + cmd + "\" as root");
		try
		{
			runtime = Runtime.getRuntime();
			process = runtime.exec(new String[]{"su", "-c", "sh"});

			BufferedWriter suInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
			BufferedReader suOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader suErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			suInput.write(cmd);
			suInput.newLine();
			suInput.flush();
			try {
				/* Wait a few milliseconds until the command has been executed. */
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				/* We don't care :-) */
			}
			while (suOutput.ready()) {
				String line;
				line = suOutput.readLine();
				ret.stdOut.addLast(line);
				Log.d(TAG,"stdout: " + line);
			}
			while (suErr.ready()) {
				String line;
				line = suErr.readLine();
				ret.stdErr.addLast(line);
				Log.d(TAG,"stderr: " + line);
			}
			// Get the exitcode of cmd
            suInput.write("echo $?");
            suInput.newLine();
            suInput.flush();
            try {
                ret.returnCode = Integer.valueOf(suOutput.readLine());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Cannot parse exitcode", e);
            }

			// Terminate our shell
			suInput.write("exit");
			suInput.newLine();
			suInput.flush();
			if (process.waitFor() != 0)
			{
				Log.i(TAG, "SU shell didn't terminate successfully.");
			}
		}
		catch (Exception e)
		{
			Log.e(TAG,"Can't execute \"" + cmd + "\" as root: " + e.getMessage());
		}
		
		return ret;
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

	public static boolean copyFileFromRAW(Context pContext, int pRAWID, String pFilename) {
		return Util.copyFileFromRAW(pContext,pRAWID,pFilename,null);
	}

	/**
	 * Copy the file included in the apk-file (identified by {@link pRAWID}) to the apps private data dir as file {@link pFilename}
	 * @param pContext the applicatins context get access to the raw resource
	 * @param pRAWID
	 * @param pFilename the filename the raw resource should be copied to
	 * @return true if all went fine, false otherwise
	 */
	public static boolean copyFileFromRAW(Context pContext, int pRAWID, String pFilename, String pDir)
	{
		InputStream is = null;
		OutputStream os = null;
		File file = null;
		byte[] buffer = new byte[1024];
		int count  = 0;
		
		try {
			if (pDir != null) {
				File dir = new File(pContext.getFilesDir().getParent() + File.separator + pDir);
				if (dir.mkdirs()) {
					Log.d(TAG, "copyFileFromRAW(): Created directories: '" + dir.getAbsolutePath() + "'.");
				}
                file = new File(pContext.getFilesDir().getParent() + File.separator + pDir + File.separator + pFilename);
			} else {
                file = new File(pContext.getFilesDir().getParent() + File.separator + pFilename);
            }
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
		LinkedList<Integer> list = new LinkedList<Integer>();
		
		try
		{
			StringTokenizer tokenizer = null;

			CmdStatus ret = runCmdAsRoot(pContext.getFilesDir().getParent() + File.separator + Config.BUSYBOX_NAME + " pidof " + pCmd, false);
			if (ret.returnCode == 0) {

				for (String line : ret.stdOut) {
					tokenizer = new StringTokenizer(line, " ");
					while (tokenizer.hasMoreTokens()) {
						list.add(Integer.valueOf(tokenizer.nextToken()));
					}
				}
				Log.d(TAG, "Got " + list.size() + " pids from pidof");

				return list;
			} else {
				return null;
			}
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

		if (!pPidFile.exists()) {
		    Log.e(TAG,"pid file (" + pPidFile + ") does not exist.");
			return false;
		}
		BufferedReader in = new BufferedReader(new FileReader(pPidFile));
		try {
			pid = Integer.valueOf(in.readLine());
		} catch (NumberFormatException e) {
			Log.e(TAG, "Cannot read from pid file (" + pPidFile + ")", e);
		} finally {
			in.close();
			in = null;
		}

		List<Integer> pids = Util.getProcessIDs(pContext,Config.STAP_IO_NAME);

		if (pids == null)
		{
			Log.e(TAG,"PID list is empty");
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
