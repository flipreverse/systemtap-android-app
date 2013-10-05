package edu.udo.cs.ess.systemtap.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.service.SystemTapService;

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
			Eventlog.e(TAG,"stop(): Can't close socket: " + e + " -- " + e.getMessage());
		} catch (InterruptedException e) {
			Eventlog.e(TAG,"stop(): Can't wait for server thread: " + e + " -- " + e.getMessage());
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

		Eventlog.d(TAG,"run(): Start listening on port " + mServerSocket.getLocalPort());
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
					Eventlog.e(TAG,"run(): Can't accept incoming connection: " + e.getMessage());
				}
			}
		}
		Eventlog.d(TAG,"run(): Finished accepting incoming connections.");
	}
}
