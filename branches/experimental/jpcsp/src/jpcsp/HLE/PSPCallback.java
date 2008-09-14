package jpcsp.HLE;

public class PSPCallback {
	
	protected int uid = 0;
	protected String name;
	protected int callback_addr;
	protected int callback_arg_addr;
	protected int notifyCount;
	protected int notifyArg;
	
	public PSPCallback(String name, int parentThreadUid, int callback_addr, int callback_arg_addr) {
		this.name = name;
		uid = SceUIDMan.get_instance().getNewUid("ThreadMan-callback");
		
        notifyCount = 0; // ?
        notifyArg = 0; // ?
        
		this.callback_addr = callback_addr;
		this.callback_arg_addr = callback_arg_addr;
	}

}
