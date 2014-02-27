package com.systemtap.android.net.protocol;

import com.google.protobuf.ByteString;
import com.systemtap.android.net.protocol.SystemTapMessage.MessageType;
import com.systemtap.android.net.protocol.SystemTapMessage.SystemTapMessageObject;
/**
 * Every protobuf message is wrapped into a java class.
 * Each type in SystemTapMessage.MessageType is represented by its own class.
 * Common code among all concrete message classes is combined in this class.
 * @author alex
 *
 */
public abstract class AbstractMessage {

	private MessageType mType;
	
	public AbstractMessage(MessageType pType) {
		mType = pType;
	}
	
	public MessageType getType() {
		return mType;
	}
	/**
	 * Some message types need additional information. It will be stored as a bytestring.
	 * Hence, each subclass needing additional information has to override this method.
	 * @return
	 */
	protected ByteString generatePayload() {
		return ByteString.EMPTY;
	}
	
	public SystemTapMessageObject toSystemTapMessageObject() {
		SystemTapMessageObject systemTapMessageObject = null;

		systemTapMessageObject = SystemTapMessageObject.newBuilder()
							.setType(mType)
							.setPayload(this.generatePayload())
							.build();
		return systemTapMessageObject;
	}
}
