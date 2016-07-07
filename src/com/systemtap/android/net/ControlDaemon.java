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

package com.systemtap.android.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

import android.util.Log;

import com.systemtap.android.service.SystemTapService;

public class ControlDaemon implements Runnable {

	private static final String TAG = ControlDaemon.class.getSimpleName();
	private boolean mRunning;
	private ServerSocket mServerSocket;
	private Thread mServerThread;
	private SystemTapService mSystemTapService;
	private LinkedList<ClientConnection> mConnections;
	
	public ControlDaemon(int pPort, SystemTapService pSystemTapService) throws IOException {
		mRunning = false;
		mServerSocket = new ServerSocket(pPort);
		mServerThread = new Thread(this);
		mSystemTapService = pSystemTapService;
		mConnections = new LinkedList<ClientConnection>();
	}
	
	public void start() {
		mServerThread.start();
	}
	
	public void stop() {
		mRunning = false;
		try {
			mServerSocket.close();
			mServerThread.join();
		} catch (IOException e) {
			Log.e(TAG,"stop(): Can't close socket: " + e + " -- " + e.getMessage());
		} catch (InterruptedException e) {
			Log.e(TAG,"stop(): Can't wait for server thread: " + e + " -- " + e.getMessage());
		}

		LinkedList<ClientConnection> connections = null;
		synchronized(mConnections) {
			connections = new LinkedList<ClientConnection>(mConnections);
		}
		for (ClientConnection conn : connections) {
			conn.stop();
		}
	}
	
	public void onClientDisconnected(ClientConnection pClientConnection) {
		synchronized(mConnections) {
			mConnections.remove(pClientConnection);
		}
	}

	public void run() {
		ClientConnection clientConnection = null;
		Socket clientSocket = null;
		mRunning = true;

		Log.d(TAG,"run(): Start listening on port " + mServerSocket.getLocalPort());
		while (mRunning) {
			try {
				clientSocket = mServerSocket.accept();
				// Delegate all send/receive and protocol staff to a dedicated thread
				clientConnection = new ClientConnection(clientSocket,mSystemTapService,this);
				// Remember all active connections
				mConnections.add(clientConnection);
				// Start receiving and protocol handling
				clientConnection.start();
			} catch (IOException e) {
				if (mRunning) {
					Log.e(TAG,"run(): Can't accept incoming connection: " + e.getMessage());
				}
			}
		}
		Log.d(TAG,"run(): Finished accepting incoming connections.");
	}
}
