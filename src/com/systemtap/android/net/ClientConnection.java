package com.systemtap.android.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.LinkedList;

import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.systemtap.android.net.SystemTapMessage.Ack;
import com.systemtap.android.net.SystemTapMessage.MessageType;
import com.systemtap.android.net.SystemTapMessage.ModuleInfo;
import com.systemtap.android.net.SystemTapMessage.ModuleList;
import com.systemtap.android.net.SystemTapMessage.ModuleStatus;
import com.systemtap.android.net.SystemTapMessage.SendModule;
import com.systemtap.android.service.Module;
import com.systemtap.android.service.SystemTapService;

public class ClientConnection implements Runnable {

	private static final String TAG = ClientConnection.class.getSimpleName();
	private Socket mSocket;
	private Thread mClientThread;;
	private ControlDaemon mControlDaemon;
	private boolean mRunning;
	private SystemTapService mSystemTapService;
	
	public ClientConnection(Socket pSocket, SystemTapService pSystemTapService, ControlDaemon pControlDaemon) {
		mSocket = pSocket;
		mClientThread = new Thread(this);
		mRunning = false;
		mSystemTapService = pSystemTapService;
		mControlDaemon = pControlDaemon;
	}
	
	public SocketAddress getRemoteAddress() {
		return mSocket.getRemoteSocketAddress();
	}
	
	public void start() {
		mClientThread.start();
	}
	
	public void stop() {
		if (!mRunning) {
			return;
		}
		mRunning = false;
		try {
			/* Closing the socket will interrupt a blocking read{Int,Fully}.
			 * In addition, mRunning is already false. The while loop in run() will terminate.
			 */
			mSocket.close();
			mClientThread.join();
		} catch (IOException e) {
			Log.e(TAG,"stop(): Can't close socket: " + e + " -- " + e.getMessage());
		} catch (InterruptedException e) {
			Log.e(TAG,"stop(): Can't wait for client thread: " + e + " -- " + e.getMessage());
		}
	}
	
	public void run() {
		byte[] buffer;
		int msgObjectSize = 0, readErrors = 0, msgType = 0;
		DataInputStream in = null;
		mRunning = true;

		try {
			in = new DataInputStream(mSocket.getInputStream());
		} catch (IOException e) {
			Log.e(TAG,"run(): Can't retrieve inputstream: " + e + " -- " + e.getMessage());
		}
		while (mRunning) {
			try {
				// First, read the message type
				msgType = in.readInt();
				// Second, read the message object size
				msgObjectSize = in.readInt();
				if (msgObjectSize != 0) {
					// Allocate a buffer being large enough to read the message object at once from stream
					buffer = new byte[msgObjectSize];
					// Read message object at once
					in.readFully(buffer);
				} else {
					buffer = new byte[1];
				}
				this.handleMessage(msgType, buffer);
			} catch (InvalidProtocolBufferException e) {
				Log.e(TAG,"run(): Can't parse SystemTapMessageObject: " + e + " -- " + e.getMessage());
			} catch (IOException e) {
				if (mRunning) {
					// Avoid an infinity loop
					readErrors++;
					Log.e(TAG,"run(): Error reading from stream: " + e + " -- " + e.getMessage());
					if (readErrors >= 2) {
						mRunning = false;
						Log.e(TAG,"run(): Tried to read " + readErrors + " times from inputstream. Terminating.");
					}
				}
			}
			buffer = null;
		}
		Log.d(TAG,"run(): ClientConnection terminated.");
		try {
			mSocket.close();
		} catch (IOException e) {
			Log.e(TAG,"run(): Can't close socket: " + e + " -- " + e.getMessage());
		}
		mSocket = null;
		mControlDaemon.onClientDisconnected(this);
	}
	
	private boolean sendMessage(int pMsgType, byte pBuffer[]) {
		try {
			DataOutputStream out = new DataOutputStream(mSocket.getOutputStream());
			out.writeInt(pMsgType);
			out.writeInt(pBuffer.length);
			out.write(pBuffer);
			return true;
		} catch (IOException e) {
			Log.e(TAG,"sendMessage(): Error writing SystemTapMessageObject to stream: " + e + " -- " + e.getMessage());
		}
		return false;
	}
	
	private boolean sendAck(int pMsgType) {
		byte data[] = Ack.newBuilder()
				  .setAckedType(pMsgType)
				  .build()
				  .toByteArray();
		return this.sendMessage(MessageType.ACK_VALUE,data);
	}
	
	private void handleMessage(int pMsgType, byte pBuffer[]) {
		byte data[];
		
		switch (pMsgType) {
			case MessageType.ACK_VALUE:
				Log.d(TAG,"handleMessage(): Weird! I should never receive an ACK.");
				break;
			case MessageType.LIST_MODULES_VALUE:
				Log.d(TAG,"Got LIST_MODULES");
				Collection<Module> modules = mSystemTapService.getModules();
				LinkedList<ModuleInfo> moduleinfos = new LinkedList<ModuleInfo>();
				for (Module module : modules) {
					ModuleInfo info = ModuleInfo.newBuilder()
										.setName(module.getName())
										.setStatus(module.getStatus())
										.build();
					moduleinfos.add(info);
				}
				data = ModuleList.newBuilder()
						  .addAllModules(moduleinfos)
						  .build()
						  .toByteArray();
				if (this.sendMessage(MessageType.MODULE_LIST_VALUE,data)) {
					Log.d(TAG,"Sent a module list to " + this.getRemoteAddress());
				} else {
					Log.e(TAG,"Can't send a module list to " + this.getRemoteAddress());
				}
				break;
			case MessageType.MODULE_LIST_VALUE:
				Log.d(TAG,"handleMessage(): Weird! I should never receive a MODULE_LIST.");
				break;
			case MessageType.SEND_MODULE_VALUE:
				SendModule sendModule = null;
				try {
					sendModule = SendModule.parseFrom(pBuffer);
				} catch (InvalidProtocolBufferException e) {
					Log.e(TAG,"Can't parse SEND_MODULE: " + e + " -- " + e.getMessage());
				}
				
				if (sendModule != null) {
					Log.d(TAG,"Got SEND_MODULE");
					if (mSystemTapService.addModule(sendModule.getName(),sendModule.getData().toByteArray())) {
						if (!this.sendAck(MessageType.SEND_MODULE_VALUE)) {
							Log.e(TAG,"handleMessage(): Can't send Ackf for SEND_MODULE.");
						}
					}
				}
				break;
			case MessageType.CONTROL_MODULE_VALUE:
				ModuleInfo moduleInfo = null;
				try {
					moduleInfo = ModuleInfo.parseFrom(pBuffer);
				} catch (InvalidProtocolBufferException e) {
					Log.e(TAG,"Can't parse CONTROL_MODULE: " + e + " -- " + e.getMessage());
				}
				
				if (moduleInfo != null) {
					Log.d(TAG,"Got CONTROL_MODULE");
					switch (moduleInfo.getStatus()) {
						case RUNNING:
								mSystemTapService.startModule(moduleInfo.getName());
							break;
	
						case STOPPED:
							mSystemTapService.stopModule(moduleInfo.getName());
						break;
	
						case DELETED:
							mSystemTapService.deleteModule(moduleInfo.getName());
						break;

						default:
							Log.e(TAG,"Got unknown status for CONTROL_MODULE: " + moduleInfo.getStatus());
						break;
					}
					if (!this.sendAck(MessageType.CONTROL_MODULE_VALUE)) {
						Log.e(TAG,"handleMessage(): Can't send Ackf for DELETE_MODULE.");
					}
				}
				break;
			default:
				Log.e(TAG,"handleMessage(): Unknown message type: " + pMsgType);
				break;
		}
	}
}
