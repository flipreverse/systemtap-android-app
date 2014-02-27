package com.systemtap.android.net.protocol;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.systemtap.android.logging.Eventlog;
import com.systemtap.android.net.protocol.SystemTapMessage.MessageType;
import com.systemtap.android.net.protocol.SystemTapMessage.SendModulePayload;
import com.systemtap.android.net.protocol.SystemTapMessage.SystemTapMessageObject;

public class SendModule extends AbstractMessage {

	private static final String TAG = SendModule.class.getSimpleName();
	private String mName;
	private byte[] mData;
	
	public SendModule(String pName, byte[] pData) {
		super(MessageType.SEND_MODULE);
		mName = pName;
		mData = pData;
	}
	
	public String getName() {
		return mName;
	}
	
	public byte[] getData() {
		return mData;
	}
	
	@Override
	protected ByteString generatePayload() {
		ByteString payload = SendModulePayload.newBuilder()
								.setName(mName)
								.setData(ByteString.copyFrom(mData))
								.build()
								.toByteString();
		
		return payload;
	}
	
	public static SendModule fromSystemTapMessageObject(SystemTapMessageObject pSystemTapMessageObject) {
		SendModule sendModule = null;
		
		ByteString payload = pSystemTapMessageObject.getPayload();
		try {
			SendModulePayload sendModulePayload = SendModulePayload.parseFrom(payload);
			byte[] data = sendModulePayload.getData().toByteArray();
			String name = sendModulePayload.getName();
			sendModule = new SendModule(name,data);
		} catch (InvalidProtocolBufferException e) {
			Eventlog.e(TAG,"Can't parse payload: " + e + " -- " + e.getMessage());
		}
		return sendModule;
	}
}
