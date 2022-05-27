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

import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.THREAD_CALLBACK_USER_DEFINED;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceNpAuthRequestParameter;
import jpcsp.HLE.kernel.types.SceNpTicket;
import jpcsp.HLE.kernel.types.SceNpTicket.TicketParam;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceNpAuth extends HLEModule {
    public static Logger log = Modules.getLogger("sceNpAuth");
    public static boolean useDummyTicket = false;

    public final static int STATUS_ACCOUNT_SUSPENDED = 0x80;
    public final static int STATUS_ACCOUNT_CHAT_RESTRICTED = 0x100;
    public final static int STATUS_ACCOUNT_PARENTAL_CONTROL_ENABLED = 0x200;

    private boolean initialized;
    private int npMemSize;     // Memory allocated by the NP utility.
    private int npMaxMemSize;  // Maximum memory used by the NP utility.
    private int npFreeMemSize; // Free memory available to use by the NP utility.
    private SceKernelCallbackInfo npAuthCreateTicketCallback;
    private String serviceId;
    private byte[] ticketBytes = new byte[10000];
    private int ticketBytesLength;

	@Override
	public void start() {
		initialized = false;
		super.start();
	}

    protected void checkInitialized() {
    	if (!initialized) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_NPAUTH_NOT_INIT);
    	}
    }

    public static void addTicketParam(SceNpTicket ticket, int type, String value, int length) {
    	byte[] stringBytes = value.getBytes(Charset.forName("ASCII"));
    	byte[] bytes = new byte[length];
    	System.arraycopy(stringBytes, 0, bytes, 0, Math.min(length, stringBytes.length));
    	ticket.parameters.add(new TicketParam(type, bytes));
    }

    public static void addTicketParam(SceNpTicket ticket, String value, int length) {
    	addTicketParam(ticket, TicketParam.PARAM_TYPE_STRING_ASCII, value, length);
    }

    public static void addTicketParam(SceNpTicket ticket, int value) {
    	byte bytes[] = new byte[4];
    	Utilities.writeUnaligned32(bytes, 0, Utilities.endianSwap32(value));
    	ticket.parameters.add(new TicketParam(TicketParam.PARAM_TYPE_INT, bytes));
    }

    public static void addTicketDateParam(SceNpTicket ticket, long time) {
    	byte bytes[] = new byte[8];
    	Utilities.writeUnaligned32(bytes, 0, Utilities.endianSwap32((int) (time >> 32)));
    	Utilities.writeUnaligned32(bytes, 4, Utilities.endianSwap32((int) time));
    	ticket.parameters.add(new TicketParam(TicketParam.PARAM_TYPE_DATE, bytes));
    }

    public static void addTicketLongParam(SceNpTicket ticket, long value) {
    	byte bytes[] = new byte[8];
    	Utilities.writeUnaligned32(bytes, 0, Utilities.endianSwap32((int) (value >> 32)));
    	Utilities.writeUnaligned32(bytes, 4, Utilities.endianSwap32((int) value));
    	ticket.parameters.add(new TicketParam(TicketParam.PARAM_TYPE_LONG, bytes));
    }

    public static void addTicketParam(SceNpTicket ticket) {
    	ticket.parameters.add(new TicketParam(TicketParam.PARAM_TYPE_NULL, new byte[0]));
    }

    private static String encodeURLParam(String value) {
    	try {
			return URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return value;
		}
    }

    private static void addURLParam(StringBuilder params, String name, String value) {
    	if (params.length() > 0) {
    		params.append("&");
    	}
    	params.append(name);
    	params.append("=");
    	params.append(encodeURLParam(value));
    }

    private static void addURLParam(StringBuilder params, String name, int addr, int length) {
    	StringBuilder value = new StringBuilder();
    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, length, 1);
    	for (int i = 0; i < length; i++) {
    		int c = memoryReader.readNext();
    		value.append((char) c);
    	}

    	addURLParam(params, name, value.toString());
    }

    /**
     * Initialization.
     * 
     * @param poolSize
     * @param stackSize
     * @param threadPriority
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xA1DE86F8, version = 150, checkInsideInterrupt = true)
    public int sceNpAuthInit(int poolSize, int stackSize, int threadPriority) {
        npMemSize = poolSize;
        npMaxMemSize = poolSize / 2;    // Dummy
        npFreeMemSize = poolSize - 16;  // Dummy.

        initialized = true;

        return 0;
    }

    /**
     * Termination.
     * 
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x4EC1F667, version = 150, checkInsideInterrupt = true)
    public int sceNpAuthTerm() {
    	initialized = false;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF4531ADC, version = 150, checkInsideInterrupt = true)
    public int sceNpAuthGetMemoryStat(TPointer32 memStatAddr) {
    	checkInitialized();

    	memStatAddr.setValue(0, npMemSize);
        memStatAddr.setValue(4, npMaxMemSize);
        memStatAddr.setValue(8, npFreeMemSize);

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCD86A656, version = 150)
    public int sceNpAuthCreateStartRequest(TPointer paramAddr) {
    	SceNpAuthRequestParameter param = new SceNpAuthRequestParameter();
    	param.read(paramAddr);
    	if (log.isInfoEnabled()) {
    		log.info(String.format("sceNpAuthCreateStartRequest param: %s", param));
    	}

    	serviceId = param.serviceId;

    	if (!useDummyTicket) {
			String loginId = JOptionPane.showInputDialog("Enter your PSN Sign-In ID (Email Address)");
			if (loginId != null) {
    			String password = JOptionPane.showInputDialog("Enter your PSN Password");
    			if (password != null) {
	    			StringBuilder params = new StringBuilder();
	    			addURLParam(params, "serviceid", serviceId);
	    			addURLParam(params, "loginid", loginId);
	    			addURLParam(params, "password", password);
	    			if (param.cookie != 0) {
	    				addURLParam(params, "cookie", param.cookie, param.cookieSize);
	    			}
	    			if (param.entitlementIdAddr != 0) {
	    				addURLParam(params, "entitlementid", param.entitlementId);
	    				addURLParam(params, "consumedcount", Integer.toString(param.consumedCount));
	    			}

	    			HttpURLConnection connection = null;
    				ticketBytesLength = 0;
	    			try {
		    			connection = (HttpURLConnection) new URL("https://auth.np.ac.playstation.net/nav/auth").openConnection();
		    			connection.setRequestProperty("X-I-5-Version", "2.1");
		    			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		    			connection.setRequestProperty("X-Platform-Version", "PSP 06.60");
		    			connection.setRequestProperty("Content-Length", Integer.toString(params.length()));
		    			connection.setRequestProperty("User-Agent", "Lediatio Lunto Ritna");
		    			connection.setRequestMethod("POST");
		    			connection.setDoOutput(true);
		    			OutputStream os = connection.getOutputStream();
		    			os.write(params.toString().getBytes());
		    			os.close();
		    			connection.connect();
		    			int responseCode = connection.getResponseCode();
		    			if (log.isDebugEnabled()) {
		    				log.debug(String.format("Response code: %d", responseCode));
		    				for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
		    					log.debug(String.format("%s: %s", entry.getKey(), entry.getValue()));
		    				}
		    			}

		    			if (responseCode == 200) {
		    				InputStream in = connection.getInputStream();
		    				while (true) {
		    					int length = in.read(ticketBytes, ticketBytesLength, ticketBytes.length - ticketBytesLength);
		    					if (length < 0) {
		    						break;
		    					}
		    					ticketBytesLength += length;
		    				}
		    				in.close();

		    				if (log.isDebugEnabled()) {
		    					log.debug(String.format("Received ticket: %s", Utilities.getMemoryDump(ticketBytes, 0, ticketBytesLength)));
		    				}
		    			}
		    		} catch (MalformedURLException e) {
		    			log.error(e);
		    		} catch (IOException e) {
		    			log.error(e);
		    		} finally {
	    				if (connection != null) {
	    					connection.disconnect();
	    				}
		    		}
    			}
			}
    	}

    	if (param.ticketCallback != 0) {
    		int ticketLength = ticketBytesLength > 0 ? ticketBytesLength : 248;
    		npAuthCreateTicketCallback = Modules.ThreadManForUserModule.hleKernelCreateCallback("sceNpAuthCreateStartRequest", param.ticketCallback, param.callbackArgument);
    		if (Modules.ThreadManForUserModule.hleKernelRegisterCallback(THREAD_CALLBACK_USER_DEFINED, npAuthCreateTicketCallback.getUid())) {
    			Modules.ThreadManForUserModule.hleKernelNotifyCallback(THREAD_CALLBACK_USER_DEFINED, npAuthCreateTicketCallback.getUid(), ticketLength);
    		}
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3F1C1F70, version = 150)
    public int sceNpAuthGetTicket(int id, TPointer buffer, int length) {
    	int result;

    	if (useDummyTicket) {
    		SceNpTicket ticket = new SceNpTicket();
    		ticket.version = 0x00000121;
    		ticket.size = 0xF0;
    		addTicketParam(ticket, "XXXXXXXXXXXXXXXXXXXX", 20);
    		addTicketParam(ticket, 0);
    		long now = System.currentTimeMillis();
    		addTicketDateParam(ticket, now);
    		addTicketDateParam(ticket, now + 10 * 60 * 1000); // now + 10 minutes
    		addTicketLongParam(ticket, 0L);
    		addTicketParam(ticket, TicketParam.PARAM_TYPE_STRING, "DummyOnlineID", 32);
    		addTicketParam(ticket, "gb", 4);
    		addTicketParam(ticket, TicketParam.PARAM_TYPE_STRING, "XX", 4);
    		addTicketParam(ticket, serviceId, 24);
			int status = 0;
			if (Modules.sceNpModule.parentalControl == sceNp.PARENTAL_CONTROL_ENABLED) {
				status |= STATUS_ACCOUNT_PARENTAL_CONTROL_ENABLED;
			}
			status |= (Modules.sceNpModule.getUserAge() & 0x7F) << 24;
    		addTicketParam(ticket, status);
    		addTicketParam(ticket);
    		addTicketParam(ticket);
    		ticket.unknownBytes = new byte[72];
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceNpAuthGetTicket returning dummy ticket: %s", ticket));
    		}
    		ticket.write(buffer);
    		result = ticket.sizeof();
    	} else if (ticketBytesLength > 0) {
        	result = Math.min(ticketBytesLength, length);
        	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(buffer.getAddress(), result, 1);
    		for (int i = 0; i < result; i++) {
    			memoryWriter.writeNext(ticketBytes[i] & 0xFF);
    		}
    		memoryWriter.flush();

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceNpAuthGetTicket returning real ticket: %s", Utilities.getMemoryDump(buffer.getAddress(), result)));
    		}
    	} else {
        	buffer.clear(length);

        	result = length;

        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceNpAuthGetTicket returning empty ticket"));
        	}
    	}

    	return result;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6900F084, version = 150)
    public int sceNpAuthGetEntitlementById(TPointer ticketBuffer, int ticketLength, int unknown1, int unknown2) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x72BB0467, version = 150)
    public int sceNpAuthDestroyRequest(int id) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD99455DD, version = 150)
    public int sceNpAuthAbortRequest(int id) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5A3CB57A, version = 150)
    public int sceNpAuthGetTicketParam(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer ticketBuffer, int ticketLength, int paramNumber, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=256, usage=Usage.out) TPointer buffer) {
    	// This clear is always done, even when an error is returned
    	buffer.clear(256);

    	if (paramNumber < 0 || paramNumber >= SceNpTicket.NUMBER_PARAMETERS) {
    		return SceKernelErrors.ERROR_NP_MANAGER_INVALID_ARGUMENT;
    	}

    	if (ticketBuffer.getValue32() == 0) {
    		// This is an empty ticket, do no analyze it
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceNpAuthGetTicketParam returning empty param from empty ticket"));
    		}
    	} else {
	    	SceNpTicket ticket = new SceNpTicket();
	    	ticket.read(ticketBuffer);
	    	if (log.isDebugEnabled()) {
	    		log.debug(String.format("sceNpAuthGetTicketParam ticket: %s", ticket));
	    	}

	    	TicketParam ticketParam = ticket.parameters.get(paramNumber);
	    	ticketParam.writeForPSP(buffer);
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x75FB0AE3, version = 150)
    public int sceNpAuthGetEntitlementIdList() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x61BB18B3, version = 150)
    public int sceNpAuth_61BB18B3() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB714FBDD, version = 150)
    public int sceNpAuth_B714FBDD() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCE85B3B8, version = 150)
    public int sceNpAuth_CE85B3B8() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDAD65284, version = 150)
    public int sceNpAuth_DAD65284() {
    	return 0;
    }
}
