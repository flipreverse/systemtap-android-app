package com.systemtap.android.net.protocol;

import com.systemtap.android.net.protocol.SystemTapMessage.MessageType;
import com.systemtap.android.net.protocol.SystemTapMessage.SystemTapMessageObject;

public class ListModules extends AbstractMessage {

	public ListModules() {
		super(MessageType.LIST_MODULES);
	}
	
	public static ListModules fromSystemTapMessage(SystemTapMessageObject pSystemTapMessageObject) {
		ListModules listModules = null;
		
		if (pSystemTapMessageObject.getType() == MessageType.LIST_MODULES) {
			listModules = new ListModules();
		}
		
		return listModules;
	}
}
