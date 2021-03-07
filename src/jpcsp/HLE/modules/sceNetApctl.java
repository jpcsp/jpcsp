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
package jpcsp.HLE.modules;

import static jpcsp.HLE.HLEModuleManager.HLESyscallNid;
import static jpcsp.HLE.modules.SysMemUserForUser.USER_PARTITION_ID;
import static jpcsp.HLE.modules.sceNetAdhocctl.IBSS_NAME_LENGTH;
import static jpcsp.HLE.modules.sceNetAdhocctl.fillNextPointersInLinkedList;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.hardware.Wlan;
import jpcsp.settings.Settings;
import jpcsp.util.HLEUtilities;
import jpcsp.Emulator;
import jpcsp.Processor;

public class sceNetApctl extends HLEModule {
    public static Logger log = Modules.getLogger("sceNetApctl");

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
	public static final int PSP_NET_APCTL_INFO_UNKNOWN19     = 19;
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

	public static final int PSP_NET_APCTL_INFO_SECURITY_TYPE_NONE        = 0;
	public static final int PSP_NET_APCTL_INFO_SECURITY_TYPE_WEP         = 1;
	public static final int PSP_NET_APCTL_INFO_SECURITY_TYPE_WPA_TKIP    = 2;
	public static final int PSP_NET_APCTL_INFO_SECURITY_TYPE_UNSUPPORTED = 3;
	public static final int PSP_NET_APCTL_INFO_SECURITY_TYPE_WPA_AES     = 4;

	public static final int PSP_NET_APCTL_DESC_IBSS = 0;
	public static final int PSP_NET_APCTL_DESC_SSID_NAME = 1;
	public static final int PSP_NET_APCTL_DESC_SSID_NAME_LENGTH = 2;
	public static final int PSP_NET_APCTL_DESC_SIGNAL_STRENGTH = 4;
	public static final int PSP_NET_APCTL_DESC_SECURITY = 5;

	public static final int SSID_NAME_LENGTH = 32;

	private static final String dummyPrimaryDNS = "1.2.3.4";
	private static final String dummySecondaryDNS = "1.2.3.5";
	private static final String dummySubnetMask = "255.255.255.0";
	private static final int dummySubnetMaskInt = 0xFFFFFF00;

    protected static final String uidPurpose = "sceNetApctl";
    protected static final String uidHandlerPurpose = "sceNetApctlHandler";
    protected int state = PSP_NET_APCTL_STATE_DISCONNECTED;
    protected int connectionIndex = 0;
	private static String localHostIP;
    private HashMap<Integer, ApctlHandler> apctlHandlers = new HashMap<Integer, ApctlHandler>();
    protected static final int stateTransitionDelay = 100000; // 100ms
    protected SceKernelThreadInfo sceNetApctlThread;
    protected boolean sceNetApctlThreadTerminate;
    private boolean doScan;
    private volatile long scanStartMillis;
    private static final int SCAN_DURATION_MILLIS = 700;
    private int NET_APCTL_LOOP_ADDRESS;

	@Override
	public void start() {
		NET_APCTL_LOOP_ADDRESS = HLEUtilities.getInstance().installLoopHandler(this, "hleNetApctlThread");
	}

	@Override
	public void stop() {
		sceNetApctlThread = null;
		sceNetApctlThreadTerminate = false;
		doScan = false;
		apctlHandlers.clear();
		super.stop();
	}

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
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("Triggering hanlder 0x%08X, oldState=%d, newState=%d, event=%d, error=0x%08X", addr, oldState, newState, event, error));
            	}
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
	    		if (oldState == PSP_NET_APCTL_STATE_SCANNING) {
	    			event = PSP_NET_APCTL_EVENT_SCAN_COMPLETE;
	    		} else {
	    			event = PSP_NET_APCTL_EVENT_DISCONNECT_REQUEST;
	    		}
	    		break;
	    	case PSP_NET_APCTL_STATE_SCANNING:
	    		event = PSP_NET_APCTL_EVENT_SCAN_REQUEST;
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
			NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
			if (networkInterface != null) {
				ssid = networkInterface.getDisplayName();
			}
			if (ssid == null) {
				ssid = "Jpcsp";
			}
		} catch (SocketException e) {
			log.error(e);
		} catch (UnknownHostException e) {
			log.error(e);
		}

		return ssid;
	}

    public static boolean hasPrimaryDNS() {
    	String primaryDNS = Settings.getInstance().readString("network.primaryDNS");
    	return primaryDNS != null && primaryDNS.length() > 0;
    }

    public static String getPrimaryDNS() {
    	// If a primary DNS is defined in the Settings, use it
    	String primaryDNS = Settings.getInstance().readString("network.primaryDNS");
    	if (primaryDNS != null && primaryDNS.length() > 0) {
    		return primaryDNS;
    	}

    	String ip = getLocalHostIP();
    	if (ip != null) {
    		// Try to guess the primary DNS by replacing the last part of our
    		// IP address with 1.
    		// e.g.: ip=A.B.C.D -> primaryDNS=A.B.C.1
    		int lastDot = ip.lastIndexOf(".");
    		if (lastDot >= 0) {
    			primaryDNS = ip.substring(0, lastDot) + ".1";
    			return primaryDNS;
    		}
    	}
    	
		return dummyPrimaryDNS;
	}

    public static String getSecondaryDNS() {
		return dummySecondaryDNS;
	}

    public static String getGateway() {
    	String gateway = getLocalHostIP();

    	// Replace last component of the local IP with "1".
    	// E.g. "192.168.1.10" -> "192.168.1.1"
    	int lastDot = gateway.lastIndexOf('.');
    	if (lastDot >= 0) {
    		gateway = gateway.substring(0, lastDot + 1) + "1";
    	}

    	return gateway;
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
					if (log.isDebugEnabled()) {
						log.debug(String.format("IP address of local host: %s", allLocalIPs[i].getHostAddress()));
					}
					byte[] bytes = allLocalIPs[i].getAddress();
					if (bytes != null && bytes.length == 4 && bytes[3] != 1) {
						localHostIP = allLocalIPs[i].getHostAddress();
					}
				}
				if (log.isDebugEnabled()) {
					log.debug(String.format("Using IP address of local host: %s, Subnet Mask %s", localHostIP, getSubnetMask()));
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
		connectionIndex = index;

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

    @HLEFunction(nid = HLESyscallNid, version = 150)
	public void hleNetApctlThread(Processor processor) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("hleNetApctlThread state=%d", state));
		}

		if (sceNetApctlThreadTerminate) {
			processor.cpu._v0 = 0; // Exit status
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
		    	case PSP_NET_APCTL_STATE_DISCONNECTED:
		    		if (doScan) {
		        		scanStartMillis = Emulator.getClock().milliTime();
		    			changeState(PSP_NET_APCTL_STATE_SCANNING);
		                doScan = false;
		                stateTransitionCompleted = false;
		    		}
		    		break;
		    	case PSP_NET_APCTL_STATE_SCANNING:
		    		// End of SCAN?
		    		long now = Emulator.getClock().milliTime();
		    		if (now - scanStartMillis > SCAN_DURATION_MILLIS) {
		    			// Return to DISCONNECTED state and trigger SCAN event
		    			changeState(PSP_NET_APCTL_STATE_DISCONNECTED);
		    		} else {
		    			stateTransitionCompleted = false;
		    		}
			}

			if (stateTransitionCompleted) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("hleNetApctlThread sleeping with state=%d", state));
				}

	    		// Wait for a new state reset... wakeup is done by triggerNetApctlThread()
	    		Modules.ThreadManForUserModule.hleKernelSleepThread(false);
	    	} else {
				if (log.isDebugEnabled()) {
					log.debug(String.format("hleNetApctlThread waiting for %d us with state=%d", stateTransitionDelay, state));
				}

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
	@HLEFunction(nid = 0xE2F91F9B, version = 150)
	public int sceNetApctlInit(int stackSize, int initPriority) {
		if (sceNetApctlThread == null) {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
			sceNetApctlThread = threadMan.hleKernelCreateThread("SceNetApctl", NET_APCTL_LOOP_ADDRESS, initPriority, stackSize, threadMan.getCurrentThread().attr, 0, USER_PARTITION_ID);
			sceNetApctlThreadTerminate = false;
			threadMan.hleKernelStartThread(sceNetApctlThread, 0, TPointer.NULL, sceNetApctlThread.gpReg_addr);
		}

		return 0;
	}

	/**
	 * Terminate the apctl.
	 *
	 * @return < 0 on error.
	 */
	@HLEFunction(nid = 0xB3EDD0EC, version = 150)
	public int sceNetApctlTerm() {
		changeState(PSP_NET_APCTL_STATE_DISCONNECTED);

		sceNetApctlThreadTerminate = true;
		triggerNetApctlThread();

		return 0;
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
	@HLEFunction(nid = 0x2BEFDF23, version = 150)
	public int sceNetApctlGetInfo(int code, TPointer pInfo) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetApctlGetInfo code=0x%X(%s)", code, getApctlInfoName(code)));
		}

		switch (code) {
			case PSP_NET_APCTL_INFO_PROFILE_NAME: {
				String name = sceUtility.getNetParamName(connectionIndex);
				pInfo.setStringNZ(128, name);
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetApctlGetInfo returning Profile name '%s'", name));
				}
				break;
			}
			case PSP_NET_APCTL_INFO_IP: {
				String ip = getLocalHostIP();
				pInfo.setStringNZ(16, ip);
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetApctlGetInfo returning IP address '%s'", ip));
				}
				break;
			}
			case PSP_NET_APCTL_INFO_SSID: {
				String ssid = getSSID();
				if (ssid == null) {
					return -1;
				}
				pInfo.setStringNZ(SSID_NAME_LENGTH, ssid);
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetApctlGetInfo returning SSID '%s'", ssid));
				}
				break;
			}
			case PSP_NET_APCTL_INFO_SSID_LENGTH: {
				String ssid = getSSID();
				if (ssid == null) {
					return -1;
				}
				pInfo.setValue32(Math.min(ssid.length(), SSID_NAME_LENGTH));
				break;
			}
			case PSP_NET_APCTL_INFO_PRIMDNS: {
				String primaryDNS = getPrimaryDNS();
				pInfo.setStringNZ(16, primaryDNS);
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetApctlGetInfo returning Primary DNS '%s'", primaryDNS));
				}
				break;
			}
			case PSP_NET_APCTL_INFO_SECDNS: {
				pInfo.setStringNZ(16, getSecondaryDNS());
				break;
			}
			case PSP_NET_APCTL_INFO_GATEWAY: {
				pInfo.setStringNZ(16, getGateway());
				break;
			}
			case PSP_NET_APCTL_INFO_SUBNETMASK: {
				pInfo.setStringNZ(16, getSubnetMask());
				break;
			}
			case PSP_NET_APCTL_INFO_CHANNEL: {
				int channel = Settings.getInstance().readInt("emu.sysparam.adhocchannel", 0);
				pInfo.setValue8((byte) channel);
				break;
			}
			case PSP_NET_APCTL_INFO_STRENGTH: {
				pInfo.setValue8((byte) Wlan.getSignalStrenth());
				break;
			}
			case PSP_NET_APCTL_INFO_USE_PROXY: {
				pInfo.setValue32(false); // Don't use proxy
				break;
			}
			case PSP_NET_APCTL_INFO_START_BROWSER: {
				// Is it needed to start the browser to login/authenticate
				// this connection?
				pInfo.setValue32(false); // Do not start the browser
				break;
			}
			case PSP_NET_APCTL_INFO_UNKNOWN19: {
				// The PSP is returning value 1 (tested with JpcspTrace)
				pInfo.setValue32(1);
				break;
			}
			default: {
				log.warn(String.format("sceNetApctlGetInfo unimplemented code=0x%X(%s)", code, getApctlInfoName(code)));
				return -1;
			}
		}

		return 0;
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
	@HLEFunction(nid = 0x8ABADD51, version = 150)
	public int sceNetApctlAddHandler(TPointer handler, int handlerArg) {
    	int uid = SceUidManager.getNewUid(uidPurpose);
    	ApctlHandler apctlHandler = new ApctlHandler(uid, handler.getAddress(), handlerArg);
    	apctlHandlers.put(uid, apctlHandler);

    	return uid;
	}

	/**
	 * Delete an apctl event handler.
	 *
	 * @param handlerid - A handler as created returned from sceNetApctlAddHandler.
	 *
	 * @return < 0 on error.
	 */
	@HLEFunction(nid = 0x5963991B, version = 150)
	public int sceNetApctlDelHandler(int handlerId) {
		if (!apctlHandlers.containsKey(handlerId)) {
			log.warn(String.format("sceNetApctlDelHandler unknown handlerId=0x%X", handlerId));
			return -1;
		}
        SceUidManager.releaseUid(handlerId, uidPurpose);
		apctlHandlers.remove(handlerId);

		return 0;
	}

	/**
	 * Connect to an access point.
	 *
	 * @param connIndex - The index of the connection.
	 *
	 * @return < 0 on error.
	 */
	@HLEFunction(nid = 0xCFB957C6, version = 150)
	public int sceNetApctlConnect(int connIndex) {
		hleNetApctlConnect(connIndex);

		return 0;
	}

	/**
	 * Disconnect from an access point.
	 *
	 * @return < 0 on error.
	 */
	@HLEFunction(nid = 0x24FE91A1, version = 150)
	public int sceNetApctlDisconnect() {
		changeState(PSP_NET_APCTL_STATE_DISCONNECTED);

		return 0;
	}

	/**
	 * Get the state of the access point connection.
	 *
	 * @param pState - Pointer to receive the current state (one of the PSP_NET_APCTL_STATE_* defines).
	 *
	 * @return < 0 on error.
	 */
	@HLEFunction(nid = 0x5DEAC81B, version = 150)
	public int sceNetApctlGetState(@BufferInfo(usage=Usage.out) TPointer32 stateAddr) {
		stateAddr.setValue(state);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x2935C45B, version = 150)
	public int sceNetApctlGetBSSDescEntry2() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xA3E77E13, version = 150)
	public int sceNetApctlScanSSID2() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xF25A5006, version = 150)
	public int sceNetApctlGetBSSDescIDList2() {
		return 0;
	}

	@HLEFunction(nid = 0xE9B2E5E6, version = 150)
	public int sceNetApctlScanUser() {
		doScan = true;
		triggerNetApctlThread();

    	return 0;
	}

	@HLEFunction(nid = 0x6BDDCB8C, version = 150)
	public int sceNetApctlGetBSSDescIDListUser(TPointer32 sizeAddr, @CanBeNull TPointer buf) {
		final int userInfoSize = 8;
		int entries = 1;
		int size = sizeAddr.getValue();
		// Return size required
		sizeAddr.setValue(entries * userInfoSize);

		if (buf.isNotNull()) {
        	int offset = 0;
        	for (int i = 0; i < entries; i++) {
        		// Check if enough space available to write the next structure
        		if (offset + userInfoSize > size) {
        			break;
        		}

        		if (log.isDebugEnabled()) {
        			log.debug(String.format("sceNetApctlGetBSSDescIDListUser returning %d at 0x%08X", i, buf.getAddress() + offset));
        		}

        		/** Pointer to next Network structure in list: will be written later */
        		offset += 4;

        		/** Entry ID */
        		buf.setValue32(offset, i);
        		offset += 4;
        	}

        	fillNextPointersInLinkedList(buf, offset, userInfoSize);
		}

		return 0;
	}

	@HLEFunction(nid = 0x04776994, version = 150)
	public int sceNetApctlGetBSSDescEntryUser(int entryId, int infoId, TPointer result) {
		switch (infoId) {
			case PSP_NET_APCTL_DESC_IBSS: // IBSS, 6 bytes
				byte[] ibss = Modules.sceNetAdhocctlModule.hleNetAdhocctlGetIBSS();
				result.setArray(ibss, IBSS_NAME_LENGTH);
				break;
			case PSP_NET_APCTL_DESC_SSID_NAME:
				// Return 32 bytes
				String ssid = getSSID();
				result.setStringNZ(SSID_NAME_LENGTH, ssid);
				break;
			case PSP_NET_APCTL_DESC_SSID_NAME_LENGTH:
				// Return one 32-bit value
				int length = Math.min(getSSID().length(), SSID_NAME_LENGTH);
				result.setValue32(length);
				break;
			case PSP_NET_APCTL_DESC_SIGNAL_STRENGTH:
				// Return 1 byte
				result.setValue8((byte) Wlan.getSignalStrenth());
				break;
			case PSP_NET_APCTL_DESC_SECURITY:
				// Return one 32-bit value
				result.setValue32(PSP_NET_APCTL_INFO_SECURITY_TYPE_WPA_AES);
				break;
			default:
				log.warn(String.format("sceNetApctlGetBSSDescEntryUser unknown id %d", infoId));
				return -1;
		}

		return 0;
	}

	@HLEFunction(nid = 0x7CFAB990, version = 150)
	public int sceNetApctlAddInternalHandler(TPointer handler, int handlerArg) {
		// This seems to be a 2nd kind of handler
		return sceNetApctlAddHandler(handler, handlerArg);
	}

	@HLEFunction(nid = 0xE11BAFAB, version = 150)
	public int sceNetApctlDelInternalHandler(int handlerId) {
		// This seems to be a 2nd kind of handler
		return sceNetApctlDelHandler(handlerId);
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xA7BB73DF, version = 150)
	public int sceNetApctl_A7BB73DF(TPointer handler, int handlerArg) {
		// This seems to be a 3rd kind of handler
		return sceNetApctlAddHandler(handler, handlerArg);
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x6F5D2981, version = 150)
	public int sceNetApctl_6F5D2981(int handlerId) {
		// This seems to be a 3rd kind of handler
		return sceNetApctlDelHandler(handlerId);
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x69745F0A, version = 150)
	public int sceNetApctl_lib2_69745F0A(int handlerId) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x4C19731F, version = 150)
	public int sceNetApctl_lib2_4C19731F(int code, TPointer pInfo) {
		return sceNetApctlGetInfo(code, pInfo);
	}

	@HLEFunction(nid = 0xB3CF6849, version = 150)
	public int sceNetApctlScan() {
		return sceNetApctlScanUser();
	}

	@HLEFunction(nid = 0x0C7FFA5C, version = 150)
	public int sceNetApctlGetBSSDescIDList(TPointer32 sizeAddr, @CanBeNull TPointer buf) {
		return sceNetApctlGetBSSDescIDListUser(sizeAddr, buf);
	}

	@HLEFunction(nid = 0x96BEB231, version = 150)
	public int sceNetApctlGetBSSDescEntry(int entryId, int infoId, TPointer result) {
		return sceNetApctlGetBSSDescEntryUser(entryId, infoId, result);
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xC20A144C, version = 150)
	public int sceNetApctl_lib2_C20A144C(int connIndex, pspNetMacAddress ps3MacAddress) {
		return sceNetApctlConnect(connIndex);
	}
}