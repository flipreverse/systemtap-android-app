package com.systemtap.android.net.protocol;

import android.util.Log;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.systemtap.android.net.protocol.SystemTapMessage.MessageType;
import com.systemtap.android.net.protocol.SystemTapMessage.ModulePayload;
import com.systemtap.android.net.protocol.SystemTapMessage.SystemTapMessageObject;

public class StopModule extends AbstractMessage {

	private static final String TAG = StopModule.class.getSimpleName();
	private String mName;
	
	public StopModule(String pName) {
		super(MessageType.STOP_MODULE);
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
	
	public static StopModule fromSystemTapMessageObject(SystemTapMessageObject pSystemTapMessageObject) {
		StopModule stopModule = null;
		
		ByteString payload = pSystemTapMessageObject.getPayload();
		try {
			ModulePayload modulePayload = ModulePayload.parseFrom(payload);
			String name = modulePayload.getName();
			stopModule = new StopModule(name);
		} catch (InvalidProtocolBufferException e) {
			Log.e(TAG,"Can't parse payload: " + e + " -- " + e.getMessage());
		}
		return stopModule;
	}
}
