package com.systemtap.android.net.protocol;

import java.util.LinkedList;

import android.util.Log;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.systemtap.android.net.protocol.SystemTapMessage.MessageType;
import com.systemtap.android.net.protocol.SystemTapMessage.ModuleInfo;
import com.systemtap.android.net.protocol.SystemTapMessage.ModuleListPayload;
import com.systemtap.android.net.protocol.SystemTapMessage.SystemTapMessageObject;

public class ModuleList extends AbstractMessage {

	private static final String TAG = ModuleList.class.getSimpleName();
	private LinkedList<ModuleInfo> mModuleInfos;
	
	public ModuleList(LinkedList<ModuleInfo> pModuleInfos) {
		super(MessageType.MODULE_LIST);
		mModuleInfos = pModuleInfos;
	}
	
	@Override
	protected ByteString generatePayload() {
		ByteString payload = null;
		
		payload = ModuleListPayload.newBuilder()
					.addAllModules(mModuleInfos)
					.build()
					.toByteString();
		return payload;
	}
	
	public static ModuleList fromSystemTapMessage(SystemTapMessageObject pSystemTapMessageObject) {
		ModuleList moduleList = null;
		
		ByteString payload = pSystemTapMessageObject.getPayload();
		try {
			ModuleListPayload moduleListPayload = ModuleListPayload.parseFrom(payload);
			LinkedList<ModuleInfo> moduleInfos = new LinkedList<ModuleInfo>(moduleListPayload.getModulesList());
			moduleList = new ModuleList(moduleInfos);
		} catch (InvalidProtocolBufferException e) {
			Log.e(TAG,"Can't parse payload: " + e + " -- " + e.getMessage());
		}
		return moduleList;
	}
}
