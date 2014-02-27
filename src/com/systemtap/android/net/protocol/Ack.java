package com.systemtap.android.net.protocol;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.systemtap.android.logging.Eventlog;
import com.systemtap.android.net.protocol.SystemTapMessage.AckPayload;
import com.systemtap.android.net.protocol.SystemTapMessage.MessageType;
import com.systemtap.android.net.protocol.SystemTapMessage.SystemTapMessageObject;

public class Ack extends AbstractMessage {

	private static final String TAG = Ack.class.getSimpleName();
	private MessageType mAckedType;
	
	public Ack(MessageType pAckedType) {
		super(MessageType.ACK);
		mAckedType = pAckedType;
	}
	
	@Override
	protected ByteString generatePayload() {
		ByteString payload = AckPayload.newBuilder()
								.setAckedType(mAckedType)
								.build().toByteString();
		return payload;
	}
	
	public static Ack fromSystemTapMessage(SystemTapMessageObject pSystemTapMessageObject) {
		Ack ack = null;
		
		ByteString payload = pSystemTapMessageObject.getPayload();
		try {
			AckPayload ackPayload = AckPayload.parseFrom(payload);
			ack = new Ack(ackPayload.getAckedType());
		} catch (InvalidProtocolBufferException e) {
			Eventlog.e(TAG,"Can't parse payload: " + e.getMessage());
		}
		
		return ack;
	}
}
