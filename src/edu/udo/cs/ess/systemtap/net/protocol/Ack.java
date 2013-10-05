package edu.udo.cs.ess.systemtap.net.protocol;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.net.protocol.SystemTapMessage.AckPayload;
import edu.udo.cs.ess.systemtap.net.protocol.SystemTapMessage.MessageType;
import edu.udo.cs.ess.systemtap.net.protocol.SystemTapMessage.SystemTapMessageObject;

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
