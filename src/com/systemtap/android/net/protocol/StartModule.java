package com.systemtap.android.net.protocol;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.systemtap.android.logging.Eventlog;
import com.systemtap.android.net.protocol.SystemTapMessage.MessageType;
import com.systemtap.android.net.protocol.SystemTapMessage.ModulePayload;
import com.systemtap.android.net.protocol.SystemTapMessage.SystemTapMessageObject;

public class StartModule extends AbstractMessage {
	
	private static final String TAG = StartModule.class.getSimpleName();
	private String mName;
	
	public StartModule(String pName) {
		super(MessageType.START_MODULE);
		mName = pName;
	}
	
	public String getName() {
		return mName;
	}
	
	@Override
	protected ByteString generatePayload() {
		ByteString payload = ModulePayload.newBuilder()
								.setName(mName)
								.build()
								.toByteString();
		
		return payload;
	}
	
	public static StartModule fromSystemTapMessageObject(SystemTapMessageObject pSystemTapMessageObject) {
		StartModule startModule = null;
		
		ByteString payload = pSystemTapMessageObject.getPayload();
		try {
			ModulePayload modulePayload = ModulePayload.parseFrom(payload);
			String name = modulePayload.getName();
			startModule = new StartModule(name);
		} catch (InvalidProtocolBufferException e) {
			Eventlog.e(TAG,"Can't parse payload: " + e + " -- " + e.getMessage());
		}
		return startModule;
	}
}
