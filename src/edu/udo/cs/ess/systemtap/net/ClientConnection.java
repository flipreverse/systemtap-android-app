package edu.udo.cs.ess.systemtap.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.LinkedList;

import com.google.protobuf.InvalidProtocolBufferException;

import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.net.protocol.AbstractMessage;
import edu.udo.cs.ess.systemtap.net.protocol.Ack;
import edu.udo.cs.ess.systemtap.net.protocol.DeleteModule;
import edu.udo.cs.ess.systemtap.net.protocol.ModuleList;
import edu.udo.cs.ess.systemtap.net.protocol.SendModule;
import edu.udo.cs.ess.systemtap.net.protocol.StartModule;
import edu.udo.cs.ess.systemtap.net.protocol.StopModule;
import edu.udo.cs.ess.systemtap.net.protocol.SystemTapMessage.MessageType;
import edu.udo.cs.ess.systemtap.net.protocol.SystemTapMessage.ModuleInfo;
import edu.udo.cs.ess.systemtap.net.protocol.SystemTapMessage.SystemTapMessageObject;
import edu.udo.cs.ess.systemtap.service.Module;
import edu.udo.cs.ess.systemtap.service.SystemTapService;

public class ClientConnection implements Runnable {

	private static final String TAG = ClientConnection.class.getSimpleName();
	private Socket mSocket;
	private Thread mClientThread;
	private boolean mRunning;
	private SystemTapService mSystemTapService;
	
	public ClientConnection(Socket pSocket, SystemTapService pSystemTapService) {
		mSocket = pSocket;
		mClientThread = new Thread(this);
		mRunning = false;
		mSystemTapService = pSystemTapService;
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
			mSocket = null;
			mClientThread.join();
		} catch (IOException e) {
			Eventlog.e(TAG,"stop(): Can't close socket: " + e + " -- " + e.getMessage());
		} catch (InterruptedException e) {
			Eventlog.e(TAG,"stop(): Can't wait for client thread: " + e + " -- " + e.getMessage());
		}
	}
	
	public void run() {
		SystemTapMessageObject stapMsg = null;
		byte[] buffer;
		int msgObjectSize = 0, readErrors = 0;
		DataInputStream in = null;
		mRunning = true;

		try {
			in = new DataInputStream(mSocket.getInputStream());
		} catch (IOException e) {
			Eventlog.e(TAG,"run(): Can't retrieve inputstream: " + e + " -- " + e.getMessage());
		}
		while (mRunning) {
			try {
				// First, read the message object size
				msgObjectSize = in.readInt();
				if (msgObjectSize == 0) {
					continue;
				}
				// Allocate a buffer being large enough to read the message object at once from stream
				buffer = new byte[msgObjectSize];
				// Read message object at once
				in.readFully(buffer);
				stapMsg = SystemTapMessageObject.parseFrom(buffer);
				this.handleMessage(stapMsg);
			} catch (InvalidProtocolBufferException e) {
				Eventlog.e(TAG,"run(): Can't parse SystemTapMessageObject: " + e + " -- " + e.getMessage());
			} catch (IOException e) {
				if (mRunning) {
					// Avoid an infinity loop
					readErrors++;
					Eventlog.e(TAG,"run(): Error reading from stream: " + e + " -- " + e.getMessage());
					if (readErrors >= 2) {
						mRunning = false;
						Eventlog.e(TAG,"run(): Tried to read " + readErrors + " times from inputstream. Terminating.");
					}
				}
			}
			buffer = null;
		}
		Eventlog.d(TAG,"run(): ClientConnection terminated.");
		try {
			mSocket.close();
		} catch (IOException e) {
			Eventlog.e(TAG,"run(): Can't close socket: " + e + " -- " + e.getMessage());
		}
		mSocket = null;
	}
	
	private boolean sendMessage(AbstractMessage pAMsg) {

		SystemTapMessageObject pStapMsg = pAMsg.toSystemTapMessageObject();
		if (pStapMsg == null) {
			Eventlog.e(TAG,"sendMessage(): Can't create SystemTapMessageObject from AbstractMessage");
			return false;
		}
		// Serialize the message to a byte array
		byte[] data = pStapMsg.toByteArray();
		try {
			DataOutputStream out = new DataOutputStream(mSocket.getOutputStream());
			out.writeInt(data.length);
			out.write(data);
			return true;
		} catch (IOException e) {
			Eventlog.e(TAG,"sendMessage(): Error writing SystemTapMessageObject to stream: " + e + " -- " + e.getMessage());
		}
		return false;
	}
	
	private void handleMessage(SystemTapMessageObject pMsg) {
		
		switch (pMsg.getType()) {
			case ACK:
				Eventlog.d(TAG,"handleMessage(): Weird! I should never receive an ACK.");
				break;
			case LIST_MODULES:
				Collection<Module> modules = mSystemTapService.getModules();
				LinkedList<ModuleInfo> moduleinfos = new LinkedList<ModuleInfo>();
				for (Module module : modules) {
					ModuleInfo info = ModuleInfo.newBuilder()
										.setName(module.getName())
										.setStatus(module.getStatus())
										.build();
					moduleinfos.add(info);
				}
				ModuleList moduleList = new ModuleList(moduleinfos);
				if (this.sendMessage(moduleList)) {
					Eventlog.d(TAG,"Sent a module list to " + this.getRemoteAddress());
				} else {
					Eventlog.e(TAG,"Can't send a module list to " + this.getRemoteAddress());
				}
				break;
			case MODULE_LIST:
				Eventlog.d(TAG,"handleMessage(): Weird! I should never receive a MODULE_LIST.");
				break;
			case SEND_MODULE:
				SendModule sendModule = SendModule.fromSystemTapMessageObject(pMsg);
				if (sendModule != null) {
					Eventlog.d(TAG,"Got SendModule: " + sendModule);
					if (mSystemTapService.addModule(sendModule.getName(),sendModule.getData())) {
						if (!this.sendMessage(new Ack(MessageType.SEND_MODULE))) {
							Eventlog.e(TAG,"handleMessage(): Can't send Ack(SEND_MODULE).");
						}
					}
				} else {
					Eventlog.e(TAG,"handleMessage(): Can't generate SendModule from SystemTapMessageObject.");
				}
				break;
			case DELETE_MODULE:
				DeleteModule deleteModule = DeleteModule.fromSystemTapMessageObject(pMsg);
				if (deleteModule != null) {
					Eventlog.d(TAG,"Got SendModule: " + deleteModule);
					mSystemTapService.deleteModule(deleteModule.getName());
					if (!this.sendMessage(new Ack(MessageType.DELETE_MODULE))) {
						Eventlog.e(TAG,"handleMessage(): Can't send Ack(DELETE_MODULE).");
					}
				} else {
					Eventlog.e(TAG,"handleMessage(): Can't generate DeleteModule from SystemTapMessageObject.");
				}
				break;
			case START_MODULE:
				StartModule startModule = StartModule.fromSystemTapMessageObject(pMsg);
				if (startModule != null) {
					Eventlog.d(TAG,"Got SendModule: " + startModule);
					mSystemTapService.startModule(startModule.getName());
					if (!this.sendMessage(new Ack(MessageType.START_MODULE))) {
						Eventlog.e(TAG,"handleMessage(): Can't send Ack(START_MODULE).");
					}
				} else {
					Eventlog.e(TAG,"handleMessage(): Can't generate StartModule from SystemTapMessageObject.");
				}
				break;
			case STOP_MODULE:
				StopModule stopModule = StopModule.fromSystemTapMessageObject(pMsg);
				if (stopModule != null) {
					Eventlog.d(TAG,"Got SendModule: " + stopModule);
					mSystemTapService.stopModule(stopModule.getName());
					if (!this.sendMessage(new Ack(MessageType.STOP_MODULE))) {
						Eventlog.e(TAG,"handleMessage(): Can't send Ack(STOP_MODULE).");
					}
				} else {
					Eventlog.e(TAG,"handleMessage(): Can't generate StopModule from SystemTapMessageObject.");
				}
				break;
			default:
				Eventlog.e(TAG,"handleMessage(): Unknown message type: " + pMsg.getType());
				break;
		}
	}
}
