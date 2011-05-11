/*
This file is part of jpcsp.

Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.HLE.modules150;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.util.Utilities;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Settings;

import jpcsp.Allegrex.CpuState;

public class sceNetApctl implements HLEModule {
    protected static Logger log = Modules.getLogger("sceNetApctl");

    public static final int PSP_NET_APCTL_STATE_DISCONNECTED = 0;
	public static final int PSP_NET_APCTL_STATE_SCANNING     = 1;
	public static final int PSP_NET_APCTL_STATE_JOINING      = 2;
	public static final int PSP_NET_APCTL_STATE_GETTING_IP   = 3;
	public static final int PSP_NET_APCTL_STATE_GOT_IP       = 4;
	public static final int PSP_NET_APCTL_STATE_EAP_AUTH     = 5;
	public static final int PSP_NET_APCTL_STATE_KEY_EXCHANGE = 6;

	public static final int PSP_NET_APCTL_EVENT_CONNECT_REQUEST    = 0;
	public static final int PSP_NET_APCTL_EVENT_SCAN_REQUEST       = 1;
	public static final int PSP_NET_APCTL_EVENT_SCAN_COMPLETE      = 2;
	public static final int PSP_NET_APCTL_EVENT_ESTABLISHED        = 3;
	public static final int PSP_NET_APCTL_EVENT_GET_IP             = 4;
	public static final int PSP_NET_APCTL_EVENT_DISCONNECT_REQUEST = 5;
	public static final int PSP_NET_APCTL_EVENT_ERROR              = 6;
	public static final int PSP_NET_APCTL_EVENT_INFO               = 7;
	public static final int PSP_NET_APCTL_EVENT_EAP_AUTH           = 8;
	public static final int PSP_NET_APCTL_EVENT_KEY_EXCHANGE       = 9;
	public static final int PSP_NET_APCTL_EVENT_RECONNECT          = 10;

	public static final int PSP_NET_APCTL_INFO_PROFILE_NAME  = 0;
	public static final int PSP_NET_APCTL_INFO_BSSID         = 1;
	public static final int PSP_NET_APCTL_INFO_SSID          = 2;
	public static final int PSP_NET_APCTL_INFO_SSID_LENGTH   = 3;
	public static final int PSP_NET_APCTL_INFO_SECURITY_TYPE = 4;
	public static final int PSP_NET_APCTL_INFO_STRENGTH      = 5;
	public static final int PSP_NET_APCTL_INFO_CHANNEL       = 6;
	public static final int PSP_NET_APCTL_INFO_POWER_SAVE    = 7;
	public static final int PSP_NET_APCTL_INFO_IP            = 8;
	public static final int PSP_NET_APCTL_INFO_SUBNETMASK    = 9;
	public static final int PSP_NET_APCTL_INFO_GATEWAY       = 10;
	public static final int PSP_NET_APCTL_INFO_PRIMDNS       = 11;
	public static final int PSP_NET_APCTL_INFO_SECDNS        = 12;
	public static final int PSP_NET_APCTL_INFO_USE_PROXY     = 13;
	public static final int PSP_NET_APCTL_INFO_PROXY_URL     = 14;
	public static final int PSP_NET_APCTL_INFO_PROXY_PORT    = 15;
	public static final int PSP_NET_APCTL_INFO_8021_EAP_TYPE = 16;
	public static final int PSP_NET_APCTL_INFO_START_BROWSER = 17;
	public static final int PSP_NET_APCTL_INFO_WIFISP        = 18;
	private static final String[] apctlInfoNames = new String[] {
		"PROFILE_NAME",
		"BSSID",
		"SSID",
		"SSID_LENGTH",
		"SECURITY_TYPE",
		"STRENGTH",
		"CHANNEL",
		"POWER_SAVE",
		"IP",
		"SUBNETMASK",
		"GATEWAY",
		"PRIMDNS",
		"SECDNS",
		"USE_PROXY",
		"PROXY_URL",
		"PROXY_PORT",
		"8021_EAP_TYPE",
		"START_BROWSER",
		"WIFISP"
	};

	public static final int PSP_NET_APCTL_INFO_SECURITY_TYPE_NONE = 0;
	public static final int PSP_NET_APCTL_INFO_SECURITY_TYPE_WEP  = 1;
	public static final int PSP_NET_APCTL_INFO_SECURITY_TYPE_WPA  = 2;

	private static final String dummyPrimaryDNS = "1.2.3.4";
	private static final String dummySecondaryDNS = "1.2.3.5";
	private static final String dummyGateway = "1.2.3.0";
	private static final String dummySubnetMask = "255.255.255.0";
	private static final int dummySubnetMaskInt = 0xFFFFFF00;

	@Override
	public String getName() {
		return "sceNetApctl";
	}
	
	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 150) {
			mm.addFunction(sceNetApctlInitFunction, 0xE2F91F9B);
			mm.addFunction(sceNetApctlTermFunction, 0xB3EDD0EC);
			mm.addFunction(sceNetApctlGetInfoFunction, 0x2BEFDF23);
			mm.addFunction(sceNetApctlAddHandlerFunction, 0x8ABADD51);
			mm.addFunction(sceNetApctlDelHandlerFunction, 0x5963991B);
			mm.addFunction(sceNetApctlConnectFunction, 0xCFB957C6);
			mm.addFunction(sceNetApctlDisconnectFunction, 0x24FE91A1);
			mm.addFunction(sceNetApctlGetStateFunction, 0x5DEAC81B);
		}
	}
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 150) {
			mm.removeFunction(sceNetApctlInitFunction);
			mm.removeFunction(sceNetApctlTermFunction);
			mm.removeFunction(sceNetApctlGetInfoFunction);
			mm.removeFunction(sceNetApctlAddHandlerFunction);
			mm.removeFunction(sceNetApctlDelHandlerFunction);
			mm.removeFunction(sceNetApctlConnectFunction);
			mm.removeFunction(sceNetApctlDisconnectFunction);
			mm.removeFunction(sceNetApctlGetStateFunction);
		}
	}

    protected static final String uidPurpose = "sceNetApctl";
    protected int state = PSP_NET_APCTL_STATE_DISCONNECTED;
	private static String localHostIP;
    private HashMap<Integer, ApctlHandler> apctlHandlers = new HashMap<Integer, ApctlHandler>();
    protected static final int stateTransitionDelay = 100000; // 100ms
    protected SceKernelThreadInfo sceNetApctlThread;
    protected boolean sceNetApctlThreadTerminate;

    protected class ApctlHandler {
    	private int id;
        private int addr;
        private int pArg;

        private ApctlHandler(int id, int addr, int pArg) {
        	this.id = id;
        	this.addr = addr;
        	this.pArg = pArg;
        }

        protected void triggerHandler(int oldState, int newState, int event, int error) {
            SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
            if (thread != null) {
                Modules.ThreadManForUserModule.executeCallback(thread, addr, null, true, oldState, newState, event, error, pArg);
            }
        }

		@Override
		public String toString() {
			return String.format("ApctlHandler[id=%d, addr=0x%08X, pArg=0x%08X]", id, addr, pArg);
		}
    }

    protected void notifyHandler(int oldState, int newState, int event, int error) {
        for (ApctlHandler handler : apctlHandlers.values()) {
            handler.triggerHandler(oldState, newState, event, error);
        }
    }

    protected void changeState(int newState) {
    	int oldState = state;
    	int error = 0;
    	int event;

    	if (newState == oldState) {
    		// State not changed, no notification
    		return;
    	}

    	// The event is set to match tests done on a real PSP
    	switch (newState) {
	    	case PSP_NET_APCTL_STATE_JOINING:
	    		event = PSP_NET_APCTL_EVENT_CONNECT_REQUEST;
	    		break;
	    	case PSP_NET_APCTL_STATE_GETTING_IP:
	    		event = PSP_NET_APCTL_EVENT_ESTABLISHED;
	    		break;
	    	case PSP_NET_APCTL_STATE_GOT_IP:
	    		event = PSP_NET_APCTL_EVENT_GET_IP;
	    		break;
	    	case PSP_NET_APCTL_STATE_DISCONNECTED:
	    		event = PSP_NET_APCTL_EVENT_DISCONNECT_REQUEST;
	    		break;
    		default:
    			event = PSP_NET_APCTL_EVENT_CONNECT_REQUEST;
    			break;
    	}

    	// Set the new state before calling the handler, in case
    	// the handler is calling back sceNetApctl (e.g. sceNetApctlDisconnect()).
    	state = newState;

    	notifyHandler(oldState, newState, event, error);

    	if (newState == PSP_NET_APCTL_STATE_JOINING) {
    		triggerNetApctlThread();
    	}
    }

    protected static String getApctlInfoName(int code) {
    	if (code < 0 || code >= apctlInfoNames.length) {
    		return String.format("Unknown Info %d", code);
    	}

    	return apctlInfoNames[code];
    }

    public static String getSSID() {
		String ssid = null;
		try {
			ssid = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getDisplayName();
		} catch (SocketException e) {
			log.error(e);
		} catch (UnknownHostException e) {
			log.error(e);
		}

		return ssid;
	}

    public static String getPrimaryDNS() {
    	String ip = getLocalHostIP();
    	if (ip != null) {
    		// Try to guess the primary DNS by replacing the last part of our
    		// IP address with 1.
    		// e.g.: ip=A.B.C.D -> primaryDNS=A.B.C.1
    		int lastDot = ip.lastIndexOf(".");
    		if (lastDot >= 0) {
    			String primaryDNS = ip.substring(0, lastDot) + ".1";
    			return primaryDNS;
    		}
    	}
    	
		return dummyPrimaryDNS;
	}

    public static String getSecondaryDNS() {
		return dummySecondaryDNS;
	}

    public static String getGateway() {
		return dummyGateway;
	}

    public static String getSubnetMask() {
		return dummySubnetMask;
	}

	public static int getSubnetMaskInt() {
		return dummySubnetMaskInt;
	}

	/**
	 * Returns the best IP address for the local host.
	 * The best IP address is defined as follows:
	 * - in all the IPv4 address of the local host, one IP address not being
	 *   a gateway address (not X.X.X.1).
	 *   E.g. such gateway addresses are defined for VMware bridges.
	 * - if no such address is found, take the IP address returned by
	 *     InetAddress.getLocalHost().getHostAddress()
	 * - if everything fails, take a dummy address "192.168.1.1"
	 *
	 * @return the best IP address for the local host
	 */
	public static String getLocalHostIP() {
		if (localHostIP == null) {
			localHostIP = "192.168.1.1";
			try {
				localHostIP = InetAddress.getLocalHost().getHostAddress();
				InetAddress localHostAddress = InetAddress.getLocalHost();
				InetAddress[] allLocalIPs = InetAddress.getAllByName(localHostAddress.getHostName());
				for (int i = 0; allLocalIPs != null && i < allLocalIPs.length; i++) {
					byte[] bytes = allLocalIPs[i].getAddress();
					if (bytes != null && bytes.length == 4 && bytes[3] != 1) {
						localHostIP = allLocalIPs[i].getHostAddress();
					}
				}
			} catch (UnknownHostException e) {
				log.error(e);
			}
		}

		return localHostIP;
	}

	public void hleNetApctlConnect(int index) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("hleNetApctlConnect index=%d", index));
		}

		changeState(PSP_NET_APCTL_STATE_JOINING);
	}

	public int hleNetApctlGetState() {
		return state;
	}

	protected void triggerNetApctlThread() {
		if (sceNetApctlThread != null) {
			Modules.ThreadManForUserModule.hleKernelWakeupThread(sceNetApctlThread);
		}
	}

	public void hleNetApctlThread(Processor processor) {
		if (sceNetApctlThreadTerminate) {
			processor.cpu.gpr[2] = 0; // Exit status
			Modules.ThreadManForUserModule.hleKernelExitDeleteThread();
			sceNetApctlThread = null;
		} else {
			boolean stateTransitionCompleted = true;

			// Make a transition to the next state
	    	switch (state) {
		    	case PSP_NET_APCTL_STATE_JOINING:
		    		changeState(PSP_NET_APCTL_STATE_GETTING_IP);
		    		stateTransitionCompleted = false;
		    		break;
		    	case PSP_NET_APCTL_STATE_GETTING_IP:
		    		changeState(PSP_NET_APCTL_STATE_GOT_IP);
		    		break;
			}

	    	if (stateTransitionCompleted) {
	    		// Wait for a new state reset... wakeup is done by triggerNetApctlThread()
	    		Modules.ThreadManForUserModule.hleKernelSleepThread(false);
	    	} else {
	    		// Wait a little bit before moving to the next state...
	    		Modules.ThreadManForUserModule.hleKernelDelayThread(stateTransitionDelay, false);
	    	}
		}
	}

	/**
	 * Init the apctl.
	 *
	 * @param stackSize - The stack size of the internal thread.
	 *
	 * @param initPriority - The priority of the internal thread.
	 *
	 * @return < 0 on error.
	 */
	public void sceNetApctlInit(Processor processor) {
		CpuState cpu = processor.cpu;

		int stackSize = cpu.gpr[4];
		int initPriority = cpu.gpr[5];

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetApctlInit stackSize=%d, initPriority=%d", stackSize, initPriority));
		}

		if (sceNetApctlThread == null) {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
			sceNetApctlThread = threadMan.hleKernelCreateThread("SceNetApctl",
					ThreadManForUser.NET_APCTL_LOOP_ADDRESS, initPriority, stackSize,
					threadMan.getCurrentThread().attr, 0);
			sceNetApctlThreadTerminate = false;
			threadMan.hleKernelStartThread(sceNetApctlThread, 0, 0, sceNetApctlThread.gpReg_addr);
		}

		cpu.gpr[2] = 0;
	}

	/**
	 * Terminate the apctl.
	 *
	 * @return < 0 on error.
	 */
	public void sceNetApctlTerm(Processor processor) {
		CpuState cpu = processor.cpu;

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetApctlTerm"));
		}

		changeState(PSP_NET_APCTL_STATE_DISCONNECTED);

		sceNetApctlThreadTerminate = true;
		triggerNetApctlThread();

		cpu.gpr[2] = 0;
	}

	/**
	 * Get the apctl information.
	 *
	 * @param code - One of the PSP_NET_APCTL_INFO_* defines.
	 *
	 * @param pInfo - Pointer to a ::SceNetApctlInfo.
	 *
	 * @return < 0 on error.
	 */
	//		union SceNetApctlInfo
	//		{
	//		        char name[64];                  /* Name of the config used */
	//		        unsigned char bssid[6];         /* MAC address of the access point */
	//		        unsigned char ssid[32];         /* ssid */                     
	//		        unsigned int ssidLength;        /* ssid string length*/
	//		        unsigned int securityType;      /* 0 for none, 1 for WEP, 2 for WPA) */
	//		        unsigned char strength;         /* Signal strength in % */
	//		        unsigned char channel;          /* Channel */
	//		        unsigned char powerSave;        /* 1 on, 0 off */
	//		        char ip[16];                    /* PSP's ip */
	//		        char subNetMask[16];            /* Subnet mask */
	//		        char gateway[16];               /* Gateway */
	//		        char primaryDns[16];            /* Primary DNS */
	//		        char secondaryDns[16];          /* Secondary DNS */
	//		        unsigned int useProxy;          /* 1 for proxy, 0 for no proxy */
	//		        char proxyUrl[128];             /* Proxy url */
	//		        unsigned short proxyPort;       /* Proxy port */
	//		        unsigned int eapType;           /* 0 is none, 1 is EAP-MD5 */
	//		        unsigned int startBrowser;      /* Should browser be started */
	//		        unsigned int wifisp;            /* 1 if connection is for Wifi service providers (WISP) */
	//		
	//		};
	public void sceNetApctlGetInfo(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

		int code = cpu.gpr[4];
		int pInfo = cpu.gpr[5];

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetApctlGetInfo code=%d(%s), pInfo=0x%08X", code, getApctlInfoName(code), pInfo));
		}

		if (!Memory.isAddressGood(pInfo)) {
			log.warn(String.format("sceNetApctlGetInfo invalid address pInfo=0x%08X", pInfo));
			cpu.gpr[2] = -1;
		} else {
			cpu.gpr[2] = 0;
			switch (code) {
				case PSP_NET_APCTL_INFO_IP: {
					String ip = getLocalHostIP();
					Utilities.writeStringNZ(mem, pInfo, 16, ip);
					if (log.isDebugEnabled()) {
						log.debug(String.format("sceNetApctlGetInfo returning IP address '%s'", ip));
					}
					break;
				}
				case PSP_NET_APCTL_INFO_SSID: {
					String ssid = getSSID();
					if (ssid == null) {
						cpu.gpr[2] = -1;
					} else {
						Utilities.writeStringNZ(mem, pInfo, 32, ssid);
						if (log.isDebugEnabled()) {
							log.debug(String.format("sceNetApctlGetInfo returning SSID '%s'", ssid));
						}
					}
					break;
				}
				case PSP_NET_APCTL_INFO_SSID_LENGTH: {
					String ssid = getSSID();
					if (ssid == null) {
						cpu.gpr[2] = -1;
					} else {
						mem.write32(pInfo, Math.min(ssid.length(), 32));
					}
					break;
				}
				case PSP_NET_APCTL_INFO_PRIMDNS: {
					Utilities.writeStringNZ(mem, pInfo, 16, getPrimaryDNS());
					break;
				}
				case PSP_NET_APCTL_INFO_SECDNS: {
					Utilities.writeStringNZ(mem, pInfo, 16, getSecondaryDNS());
					break;
				}
				case PSP_NET_APCTL_INFO_GATEWAY: {
					Utilities.writeStringNZ(mem, pInfo, 16, getGateway());
					break;
				}
				case PSP_NET_APCTL_INFO_SUBNETMASK: {
					Utilities.writeStringNZ(mem, pInfo, 16, getSubnetMask());
					break;
				}
				case PSP_NET_APCTL_INFO_CHANNEL: {
					int channel = Settings.getInstance().readInt("emu.sysparam.adhocchannel", 0);
					mem.write8(pInfo, (byte) channel);
					break;
				}
				case PSP_NET_APCTL_INFO_STRENGTH: {
					int signalStrength = 100;
					mem.write8(pInfo, (byte) signalStrength);
				}
				default: {
					cpu.gpr[2] = -1;
					log.warn(String.format("sceNetApctlGetInfo unimplemented code=%d(%s)", code, getApctlInfoName(code)));
					break;
				}
			}
		}
	}

	/**
	 * Add an apctl event handler.
	 *
	 * @param handler - Pointer to the event handler function.
	 *                  typedef void (*sceNetApctlHandler)(int oldState, int newState, int event, int error, void *pArg)
	 *
	 * @param pArg - Value to be passed to the pArg parameter of the handler function.
	 *
	 * @return A handler id or < 0 on error.
	 */
	public void sceNetApctlAddHandler(Processor processor) {
		CpuState cpu = processor.cpu;

		int handler = cpu.gpr[4];
		int pArg = cpu.gpr[5];

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetApctlAddHandler handler=0x%08X, pArg=0x%08X", handler, pArg));
		}

    	int uid = SceUidManager.getNewUid(uidPurpose);
    	ApctlHandler apctlHandler = new ApctlHandler(uid, handler, pArg);
    	apctlHandlers.put(uid, apctlHandler);

    	cpu.gpr[2] = uid;
	}

	/**
	 * Delete an apctl event handler.
	 *
	 * @param handlerid - A handler as created returned from sceNetApctlAddHandler.
	 *
	 * @return < 0 on error.
	 */
	public void sceNetApctlDelHandler(Processor processor) {
		CpuState cpu = processor.cpu;

		int handlerId = cpu.gpr[4];

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetApctlDelHandler handlerId=%d", handlerId));
		}

		if (!apctlHandlers.containsKey(handlerId)) {
			log.warn(String.format("sceNetApctlDelHandler unknown handlerId=%d", handlerId));
			cpu.gpr[2] = -1;
		} else {
	        SceUidManager.releaseUid(handlerId, uidPurpose);
			apctlHandlers.remove(handlerId);

			cpu.gpr[2] = 0;
		}
	}

	/**
	 * Connect to an access point.
	 *
	 * @param connIndex - The index of the connection.
	 *
	 * @return < 0 on error.
	 */
	public void sceNetApctlConnect(Processor processor) {
		CpuState cpu = processor.cpu;

		int connIndex = cpu.gpr[4];

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetApctlConnect connIndex=%d", connIndex));
		}

		hleNetApctlConnect(connIndex);

		cpu.gpr[2] = 0;
	}

	/**
	 * Disconnect from an access point.
	 *
	 * @return < 0 on error.
	 */
	public void sceNetApctlDisconnect(Processor processor) {
		CpuState cpu = processor.cpu;

		if (log.isDebugEnabled()) {
			log.debug("sceNetApctlDisconnect");
		}

		changeState(PSP_NET_APCTL_STATE_DISCONNECTED);

		cpu.gpr[2] = 0;
	}

	/**
	 * Get the state of the access point connection.
	 *
	 * @param pState - Pointer to receive the current state (one of the PSP_NET_APCTL_STATE_* defines).
	 *
	 * @return < 0 on error.
	 */
	public void sceNetApctlGetState(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

		int pState = cpu.gpr[4];

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetApctlGetState pState=0x%08X, state=%d", pState, state));
		}

		if (!Memory.isAddressGood(pState)) {
			log.warn(String.format("sceNetApctlGetState invalid address pState=0x%08X", pState));
			cpu.gpr[2] = -1;
		} else {
			mem.write32(pState, state);
			cpu.gpr[2] = 0;
		}
	}

	public final HLEModuleFunction sceNetApctlInitFunction = new HLEModuleFunction("sceNetApctl", "sceNetApctlInit") {
		@Override
		public final void execute(Processor processor) {
			sceNetApctlInit(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceNetApctlModule.sceNetApctlInit(processor);";
		}
	};

	public final HLEModuleFunction sceNetApctlTermFunction = new HLEModuleFunction("sceNetApctl", "sceNetApctlTerm") {
		@Override
		public final void execute(Processor processor) {
			sceNetApctlTerm(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceNetApctlModule.sceNetApctlTerm(processor);";
		}
	};

	public final HLEModuleFunction sceNetApctlGetInfoFunction = new HLEModuleFunction("sceNetApctl", "sceNetApctlGetInfo") {
		@Override
		public final void execute(Processor processor) {
			sceNetApctlGetInfo(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceNetApctlModule.sceNetApctlGetInfo(processor);";
		}
	};

	public final HLEModuleFunction sceNetApctlAddHandlerFunction = new HLEModuleFunction("sceNetApctl", "sceNetApctlAddHandler") {
		@Override
		public final void execute(Processor processor) {
			sceNetApctlAddHandler(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceNetApctlModule.sceNetApctlAddHandler(processor);";
		}
	};

	public final HLEModuleFunction sceNetApctlDelHandlerFunction = new HLEModuleFunction("sceNetApctl", "sceNetApctlDelHandler") {
		@Override
		public final void execute(Processor processor) {
			sceNetApctlDelHandler(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceNetApctlModule.sceNetApctlDelHandler(processor);";
		}
	};

	public final HLEModuleFunction sceNetApctlConnectFunction = new HLEModuleFunction("sceNetApctl", "sceNetApctlConnect") {
		@Override
		public final void execute(Processor processor) {
			sceNetApctlConnect(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceNetApctlModule.sceNetApctlConnect(processor);";
		}
	};

	public final HLEModuleFunction sceNetApctlDisconnectFunction = new HLEModuleFunction("sceNetApctl", "sceNetApctlDisconnect") {
		@Override
		public final void execute(Processor processor) {
			sceNetApctlDisconnect(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceNetApctlModule.sceNetApctlDisconnect(processor);";
		}
	};

	public final HLEModuleFunction sceNetApctlGetStateFunction = new HLEModuleFunction("sceNetApctl", "sceNetApctlGetState") {
		@Override
		public final void execute(Processor processor) {
			sceNetApctlGetState(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceNetApctlModule.sceNetApctlGetState(processor);";
		}
	};
};
