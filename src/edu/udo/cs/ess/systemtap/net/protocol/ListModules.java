package edu.udo.cs.ess.systemtap.net.protocol;

import edu.udo.cs.ess.systemtap.net.protocol.SystemTapMessage.MessageType;
import edu.udo.cs.ess.systemtap.net.protocol.SystemTapMessage.SystemTapMessageObject;

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
