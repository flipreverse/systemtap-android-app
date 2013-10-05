package edu.udo.cs.ess.systemtap.net.protocol;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.net.protocol.SystemTapMessage.MessageType;
import edu.udo.cs.ess.systemtap.net.protocol.SystemTapMessage.ModulePayload;
import edu.udo.cs.ess.systemtap.net.protocol.SystemTapMessage.SystemTapMessageObject;

public class DeleteModule  extends AbstractMessage {

	private static final String TAG = StopModule.class.getSimpleName();
	private String mName;
	
	public DeleteModule(String pName) {
		super(MessageType.DELETE_MODULE);
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
	
	public static DeleteModule fromSystemTapMessageObject(SystemTapMessageObject pSystemTapMessageObject) {
		DeleteModule deleteModule = null;
		
		ByteString payload = pSystemTapMessageObject.getPayload();
		try {
			ModulePayload modulePayload = ModulePayload.parseFrom(payload);
			String name = modulePayload.getName();
			deleteModule = new DeleteModule(name);
		} catch (InvalidProtocolBufferException e) {
			Eventlog.e(TAG,"Can't parse payload: " + e + " -- " + e.getMessage());
		}
		return deleteModule;
	}
}
