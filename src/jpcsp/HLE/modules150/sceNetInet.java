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

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.savarese.rocksaw.net.RawSocket;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure;
import jpcsp.HLE.kernel.types.pspNetSockAddrInternet;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.sceNetApctl;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.network.RawChannel;
import jpcsp.network.RawSelector;
import jpcsp.settings.AbstractStringSettingsListener;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;
import jpcsp.Emulator;
import jpcsp.Memory;

@HLELogging
public class sceNetInet extends HLEModule {
    public static Logger log = Modules.getLogger("sceNetInet");

    public static final int AF_INET = 2; // Address familiy internet

    public static final int SOCK_STREAM = 1; // Stream socket
    public static final int SOCK_DGRAM = 2; // Datagram socket
    public static final int SOCK_RAW = 3; // Raw socket
    public static final int SOCK_STREAM_UNKNOWN_10 = 10; // Looks like a SOCK_STREAM, but specifics unknown
    private static final String[] socketTypeNames = new String[] {
    	"Unknown0", "SOCK_STREAM", "SOCK_DGRAM", "SOCK_RAW", "Unknown4", "Unknown5", "Unknown6", "Unknown7", "Unknown8", "Unknown9", "SOCK_STREAM_UNKNOWN_10"
    };

    public static final int SOL_SOCKET = 0xFFFF; // Socket level
    public static final int INADDR_ANY = 0x00000000; // wildcard/any IP address
    public static final int INADDR_BROADCAST = 0xFFFFFFFF; // Broadcast address

    public static final int EAGAIN = SceKernelErrors.ERROR_ERRNO_RESOURCE_UNAVAILABLE & 0x0000FFFF;
    public static final int EWOULDBLOCK = EAGAIN; // EWOULDBLOCK == EAGAIN
    public static final int EINPROGRESS = SceKernelErrors.ERROR_ERRNO_IN_PROGRESS & 0x0000FFFF;
    public static final int ENOTCONN = SceKernelErrors.ERROR_ERRNO_NOT_CONNECTED & 0x0000FFFF;
    public static final int ECLOSED = SceKernelErrors.ERROR_ERRNO_CLOSED & 0x0000FFFF;
    public static final int EIO = SceKernelErrors.ERROR_ERRNO_IO_ERROR & 0x0000FFFF;
    public static final int EISCONN = SceKernelErrors.ERROR_ERRNO_IS_ALREADY_CONNECTED & 0x0000FFFF;
    public static final int EALREADY = SceKernelErrors.ERROR_ERRNO_ALREADY & 0x0000FFFF;
    public static final int EADDRNOTAVAIL = SceKernelErrors.ERROR_ERRNO_ADDRESS_NOT_AVAILABLE & 0x0000FFFF;

    // Types of socket shutdown ("how" parameter)
    public static final int SHUT_RD = 0; // Disallow further receives
    public static final int SHUT_WR = 1; // Disallow further sends
    public static final int SHUT_RDWR = 2; // Disallow further sends/receives

    // Socket options
    public static final int SO_DEBUG        = 0x0001; // turn on debugging info recording
    public static final int SO_ACCEPTCONN   = 0x0002; // socket has had listen()
    public static final int SO_REUSEADDR    = 0x0004; // allow local address reuse
    public static final int SO_KEEPALIVE    = 0x0008; // keep connections alive
    public static final int SO_DONTROUTE    = 0x0010; // just use interface addresses
    public static final int SO_BROADCAST    = 0x0020; // permit sending of broadcast msgs
    public static final int SO_USELOOPBACK  = 0x0040; // bypass hardware when possible
    public static final int SO_LINGER       = 0x0080; // linger on close if data present
    public static final int SO_OOBINLINE    = 0x0100; // leave received OOB data in line
    public static final int SO_REUSEPORT    = 0x0200; // allow local address & port reuse
    public static final int SO_TIMESTAMP    = 0x0400; // timestamp received dgram traffic
    public static final int SO_ONESBCAST    = 0x0800; // allow broadcast to 255.255.255.255
    public static final int SO_SNDBUF       = 0x1001; // send buffer size
    public static final int SO_RCVBUF       = 0x1002; // receive buffer size
    public static final int SO_SNDLOWAT     = 0x1003; // send low-water mark
    public static final int SO_RCVLOWAT     = 0x1004; // receive low-water mark
    public static final int SO_SNDTIMEO     = 0x1005; // send timeout
    public static final int SO_RCVTIMEO     = 0x1006; // receive timeout
    public static final int SO_ERROR        = 0x1007; // get error status and clear
    public static final int SO_TYPE         = 0x1008; // get socket type
    public static final int SO_OVERFLOWED   = 0x1009; // datagrams: return packets dropped
    public static final int SO_NONBLOCK     = 0x1009; // non-blocking I/O

    // Bitmasks for sceNetInetPoll()
    public static final int POLLIN     = 0x0001;
    public static final int POLLPRI    = 0x0002;
    public static final int POLLOUT    = 0x0004;
    public static final int POLLERR    = 0x0008;
    public static final int POLLHUP    = 0x0010;
    public static final int POLLNVAL   = 0x0020;
    public static final int POLLRDNORM = 0x0040;
    public static final int POLLRDBAND = 0x0080;
    public static final int POLLWRBAND = 0x0100;

    // Infinite timeout for scenetInetPoll()
    public static final int POLL_INFTIM = -1;

    // Polling period (micro seconds) for blocking operations
    protected static final int BLOCKED_OPERATION_POLLING_MICROS = 10000;

    protected static final int readSelectionKeyOperations = SelectionKey.OP_READ | SelectionKey.OP_ACCEPT;
    protected static final int writeSelectionKeyOperations = SelectionKey.OP_WRITE;

    // MSG flag options for sceNetInetRecvfrom and sceNetInetSendto (from <sys/socket.h>)
    public static final int MSG_OOB = 0x1;       // Requests out-of-band data.
    public static final int MSG_PEEK = 0x2;      // Peeks at an incoming message.
    public static final int MSG_DONTROUTE = 0x4; // Sends data without routing tables.
    public static final int MSG_EOR = 0x8;       // Terminates a record.
    public static final int MSG_TRUNC = 0x10;    // Truncates data before receiving it.
    public static final int MSG_CTRUNC = 0x20;   // Truncates control data before receiving it.
    public static final int MSG_WAITALL = 0x40;  // Waits until all data can be returned (blocking).
    public static final int MSG_DONTWAIT = 0x80; // Doesn't wait until all data can be returned (non-blocking).
    public static final int MSG_BCAST = 0x100;   // Message received by link-level broadcast.
    public static final int MSG_MCAST = 0x200;   // Message received by link-level multicast.

    private static InetAddress[] broadcastAddresses;

    @Override
	public String getName() {
    	return "sceNetInet";
	}

    private static class BroadcastAddressSettingsListener extends AbstractStringSettingsListener {
		@Override
		protected void settingsValueChanged(String value) {
			// Force a new evaluation of broadcasrAddresses in getBroadcastInetSocketAddress()
			broadcastAddresses = null;
		}
    }

    public static InetSocketAddress[] getBroadcastInetSocketAddress(int port) throws UnknownHostException {
    	if (broadcastAddresses == null) {
        	String broadcastAddressNames = Settings.getInstance().readString("network.broadcastAddress");
        	if (broadcastAddressNames != null && broadcastAddressNames.length() > 0) {
        		String [] addressNames = broadcastAddressNames.split(" *[,;] *");
        		ArrayList<InetAddress> addresses = new ArrayList<InetAddress>();
        		for (int i = 0; i < addressNames.length; i++) {
	        		try {
	        			InetAddress address = InetAddress.getByName(addressNames[i]);
	        			addresses.add(address);
	        		} catch (Exception e) {
	        			log.error(String.format("Error resolving the broadcast address '%s' from the Settings file", addressNames[i]), e);
	        		}
        		}

        		if (addresses.size() > 0) {
        			broadcastAddresses = addresses.toArray(new InetAddress[addresses.size()]);
        		}
        	}

        	if (broadcastAddresses == null) {
        		// When SO_ONESBCAST is not enabled, map the broadcast address
        		// to the broadcast address from the network of the local IP address.
        		// E.g.
        		//  - localHostIP: A.B.C.D
        		//  - subnetMask: 255.255.255.0
        		// -> localBroadcastIP: A.B.C.255
        		InetAddress localInetAddress = InetAddress.getByName(sceNetApctl.getLocalHostIP());
        		int localAddress = bytesToInternetAddress(localInetAddress.getAddress());
        		int subnetMask = Integer.reverseBytes(sceNetApctl.getSubnetMaskInt());
        		int localBroadcastAddressInt = localAddress & subnetMask;
        		localBroadcastAddressInt |= INADDR_BROADCAST & ~subnetMask;

        		broadcastAddresses = new InetAddress[1];
        		broadcastAddresses[0] = InetAddress.getByAddress(internetAddressToBytes(localBroadcastAddressInt));
        	}

        	if (log.isDebugEnabled()) {
        		for (int i = 0; i < broadcastAddresses.length; i++) {
        			log.debug(String.format("Using the following broadcast address#%d: %s", i + 1, broadcastAddresses[i].getHostAddress()));
        		}
        	}
    	}

    	InetSocketAddress[] socketAddresses = new InetSocketAddress[broadcastAddresses.length];
    	for (int i = 0; i < socketAddresses.length; i++) {
    		socketAddresses[i] = new InetSocketAddress(broadcastAddresses[i], port);
    	}

    	return socketAddresses;
    }

    protected static abstract class BlockingState implements IAction {
		public pspInetSocket inetSocket;
		public int threadId;
		public boolean threadBlocked;
		public long timeout; // microseconds
		public long start; // Clock.microTime
		private boolean insideExecute;

		public BlockingState(pspInetSocket inetSocket, long timeout) {
			this.inetSocket = inetSocket;
			threadId = Modules.ThreadManForUserModule.getCurrentThreadID();
			threadBlocked = false;
			start = Emulator.getClock().microTime();
			this.timeout = timeout;
		}

		public boolean isTimeout() {
			long now = Emulator.getClock().microTime();
			return now >= start + timeout;
		}

		@Override
		public void execute() {
			// Avoid executing the blocking state while already processing it.
			// E.g. when the thread is unblocked by executeBlockingState(),
			// this action is called again
			if (!insideExecute) {
				insideExecute = true;
				executeBlockingState();
				insideExecute = false;
			}
		}

		protected abstract void executeBlockingState();
	}

	protected static class BlockingAcceptState extends BlockingState {
		public pspNetSockAddrInternet acceptAddr;

		public BlockingAcceptState(pspInetSocket inetSocket, pspNetSockAddrInternet acceptAddr) {
			super(inetSocket, pspInetSocket.NO_TIMEOUT);
			this.acceptAddr = acceptAddr;
		}

		@Override
		protected void executeBlockingState() {
			inetSocket.blockedAccept(this);
		}
	}

	protected static class BlockingPollState extends BlockingState {
		public Selector selector;
		public pspInetPollFd[] pollFds;

		public BlockingPollState(Selector selector, pspInetPollFd[] pollFds, long timeout) {
			super(null, timeout);
			this.selector = selector;
			this.pollFds = pollFds;
		}

		@Override
		protected void executeBlockingState() {
			Modules.sceNetInetModule.blockedPoll(this);
		}
	}

	protected static class BlockingSelectState extends BlockingState {
		public Selector selector;
		public RawSelector rawSelector;
		public int numberSockets;
		public TPointer readSocketsAddr;
		public TPointer writeSocketsAddr;
		public TPointer outOfBandSocketsAddr;
		public int count;

		public BlockingSelectState(Selector selector, RawSelector rawSelector, int numberSockets, TPointer readSocketsAddr, TPointer writeSocketsAddr, TPointer outOfBandSocketsAddr, long timeout, int count) {
			super(null, timeout);
			this.selector = selector;
			this.rawSelector = rawSelector;
			this.numberSockets = numberSockets;
			this.readSocketsAddr = readSocketsAddr;
			this.writeSocketsAddr = writeSocketsAddr;
			this.outOfBandSocketsAddr = outOfBandSocketsAddr;
			this.count = count;
		}

		@Override
		protected void executeBlockingState() {
			Modules.sceNetInetModule.blockedSelect(this);
		}
	}

	protected static class BlockingReceiveState extends BlockingState {
		public int buffer;
		public int bufferLength;
		public int flags;
		public int receivedLength;

		public BlockingReceiveState(pspInetSocket inetSocket, int buffer, int bufferLength, int flags, int receivedLength) {
			super(inetSocket, inetSocket.getReceiveTimeout());
			this.buffer = buffer;
			this.bufferLength = bufferLength;
			this.flags = flags;
			this.receivedLength = receivedLength;
		}

		@Override
		protected void executeBlockingState() {
			inetSocket.blockedRecv(this);
		}
	}

	protected static class BlockingReceiveFromState extends BlockingState {
		public int buffer;
		public int bufferLength;
		public int flags;
		public pspNetSockAddrInternet fromAddr;
		public int receivedLength;

		public BlockingReceiveFromState(pspInetSocket inetSocket, int buffer, int bufferLength, int flags, pspNetSockAddrInternet fromAddr, int receivedLength) {
			super(inetSocket, inetSocket.getReceiveTimeout());
			this.buffer = buffer;
			this.bufferLength = bufferLength;
			this.flags = flags;
			this.fromAddr = fromAddr;
			this.receivedLength = receivedLength;
		}

		@Override
		protected void executeBlockingState() {
			inetSocket.blockedRecvfrom(this);
		}
	}

	protected static class BlockingSendState extends BlockingState {
		public int buffer;
		public int bufferLength;
		public int flags;
		public int sentLength;

		public BlockingSendState(pspInetSocket inetSocket, int buffer, int bufferLength, int flags, int sentLength) {
			super(inetSocket, inetSocket.getSendTimeout());
			this.buffer = buffer;
			this.bufferLength = bufferLength;
			this.flags = flags;
			this.sentLength = sentLength;
		}

		@Override
		protected void executeBlockingState() {
			inetSocket.blockedSend(this);
		}
	}

	protected static class BlockingSendToState extends BlockingState {
		public int buffer;
		public int bufferLength;
		public int flags;
		public pspNetSockAddrInternet toAddr;
		public int sentLength;

		public BlockingSendToState(pspInetSocket inetSocket, int buffer, int bufferLength, int flags, pspNetSockAddrInternet toAddr, int sentLength) {
			super(inetSocket, inetSocket.getSendTimeout());
			this.buffer = buffer;
			this.bufferLength = bufferLength;
			this.flags = flags;
			this.toAddr = toAddr;
			this.sentLength = sentLength;
		}

		@Override
		protected void executeBlockingState() {
			inetSocket.blockedSendto(this);
		}
	}

	protected abstract class pspInetSocket {
		public static final long NO_TIMEOUT = Integer.MAX_VALUE * 1000000L;
		public static final int NO_TIMEOUT_INT = Integer.MAX_VALUE;
		private int uid;
		protected boolean blocking = true;
		protected boolean broadcast;
		protected boolean onesBroadcast;
		protected int receiveLowWaterMark = 1;
		protected int sendLowWaterMark = 2048;
		protected int receiveTimeout = NO_TIMEOUT_INT;
		protected int sendTimeout = NO_TIMEOUT_INT;
		protected int receiveBufferSize = 0x4000;
		protected int sendBufferSize = 0x4000;
		protected int error;
		protected boolean reuseAddress;
		protected boolean keepAlive;
		protected boolean lingerEnabled;
		protected int linger;
		protected boolean tcpNoDelay;

		public pspInetSocket(int uid) {
			this.uid = uid;
		}

		public int getUid() {
			return uid;
		}

		public abstract int connect(pspNetSockAddrInternet addr);
		public abstract int bind(pspNetSockAddrInternet addr);
		public abstract int recv(int buffer, int bufferLength, int flags, BlockingReceiveState blockingState);
		public abstract int send(int buffer, int bufferLength, int flags, BlockingSendState blockingState);
		public abstract int recvfrom(int buffer, int bufferLength, int flags, pspNetSockAddrInternet fromAddr, BlockingReceiveFromState blockingState);
		public abstract int sendto(int buffer, int bufferLength, int flags, pspNetSockAddrInternet toAddr, BlockingSendToState blockingState);
		public abstract int close();
		public abstract SelectableChannel getSelectableChannel();
		public abstract boolean isValid();
		public abstract int getSockname(pspNetSockAddrInternet sockAddrInternet);
		public abstract int getPeername(pspNetSockAddrInternet sockAddrInternet);
		public abstract int shutdown(int how);
		public abstract int listen(int backlog);
		public abstract int accept(pspNetSockAddrInternet sockAddrInternet, BlockingAcceptState blockingState);
		public abstract boolean finishConnect();

		public int setBlocking(boolean blocking) {
			this.blocking = blocking;

			return 0;
		}

		public boolean isBlocking() {
			return blocking;
		}

		public boolean isBlocking(int flags) {
			// Flag MSG_DONTWAIT set: the IO operation is non-blocking
			if ((flags & MSG_DONTWAIT) != 0) {
				return false;
			}

			return isBlocking();
		}

		protected SocketAddress getSocketAddress(int address, int port) throws UnknownHostException {
			SocketAddress socketAddress;
			if (address == INADDR_ANY) {
				socketAddress = new InetSocketAddress(port);
			} else if (address == INADDR_BROADCAST && !isOnesBroadcast()) {
				socketAddress = getBroadcastInetSocketAddress(port)[0];
			} else {
				socketAddress = new InetSocketAddress(InetAddress.getByAddress(internetAddressToBytes(address)), port);
			}

			return socketAddress;
		}

		protected SocketAddress[] getMultiSocketAddress(int address, int port) throws UnknownHostException {
			SocketAddress[] socketAddress;
			if (address == INADDR_ANY) {
				socketAddress = new SocketAddress[1];
				socketAddress[0] = new InetSocketAddress(port);
			} else if (address == INADDR_BROADCAST && !isOnesBroadcast()) {
				socketAddress = getBroadcastInetSocketAddress(port);
			} else {
				socketAddress = new SocketAddress[1];
				socketAddress[0] = new InetSocketAddress(InetAddress.getByAddress(internetAddressToBytes(address)), port);
			}

			return socketAddress;
		}

		protected SocketAddress getSocketAddress(pspNetSockAddrInternet addr) throws UnknownHostException {
			return getSocketAddress(addr.sin_addr, addr.sin_port);
		}

		protected SocketAddress[] getMultiSocketAddress(pspNetSockAddrInternet addr) throws UnknownHostException {
			return getMultiSocketAddress(addr.sin_addr, addr.sin_port);
		}

		protected InetAddress getInetAddress(int address) throws UnknownHostException {
			InetAddress inetAddress;
			inetAddress = InetAddress.getByAddress(internetAddressToBytes(address));

			return inetAddress;
		}

		protected InetAddress getInetAddress(pspNetSockAddrInternet addr) throws UnknownHostException {
			return getInetAddress(addr.sin_addr);
		}

		protected void copySocketAttributes(pspInetSocket from) {
			setBlocking(from.isBlocking());
			setBroadcast(from.isBroadcast());
			setOnesBroadcast(from.isOnesBroadcast());
			setReceiveLowWaterMark(from.getReceiveLowWaterMark());
			setSendLowWaterMark(from.getSendLowWaterMark());
			setReceiveTimeout(from.getReceiveTimeout());
			setSendTimeout(from.getSendTimeout());
			setReceiveBufferSize(from.getReceiveBufferSize());
			setSendBufferSize(from.getSendBufferSize());
			setReuseAddress(from.isReuseAddress());
			setKeepAlive(from.isKeepAlive());
			setLinger(from.isLingerEnabled(), from.getLinger());
			setTcpNoDelay(from.isTcpNoDelay());
		}

		public boolean isBroadcast() {
			return broadcast;
		}

		public int setBroadcast(boolean broadcast) {
			this.broadcast = broadcast;

			return 0;
		}

		public int getReceiveLowWaterMark() {
			return receiveLowWaterMark;
		}

		public void setReceiveLowWaterMark(int receiveLowWaterMark) {
			this.receiveLowWaterMark = receiveLowWaterMark;
		}

		public int getSendLowWaterMark() {
			return sendLowWaterMark;
		}

		public void setSendLowWaterMark(int sendLowWaterMark) {
			this.sendLowWaterMark = sendLowWaterMark;
		}

		protected byte[] getByteArray(int address, int length) {
			byte[] bytes = new byte[length];
			IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 1);
			for (int i = 0; i < length; i++) {
				bytes[i] = (byte) memoryReader.readNext();
			}

			return bytes;
		}

		protected ByteBuffer getByteBuffer(int address, int length) {
			return ByteBuffer.wrap(getByteArray(address, length));
		}

		@Override
		public String toString() {
			return String.format("pspInetSocket[uid=%d]", uid);
		}

		public boolean isOnesBroadcast() {
			return onesBroadcast;
		}

		public void setOnesBroadcast(boolean onesBroadcast) {
			this.onesBroadcast = onesBroadcast;
		}

		public int getReceiveTimeout() {
			return receiveTimeout;
		}

		public void setReceiveTimeout(int receiveTimeout) {
			this.receiveTimeout = receiveTimeout;
		}

		public int getSendTimeout() {
			return sendTimeout;
		}

		public void setSendTimeout(int sendTimeout) {
			this.sendTimeout = sendTimeout;
		}

		public int getErrorAndClear() {
			int value = error;
			// clear error and errno
			clearError();

			return value;
		}

		protected void setSocketError(int error) {
			this.error = error;
		}

		protected void setSocketError(Exception e) {
			if (e instanceof NotYetConnectedException) {
				setSocketError(ENOTCONN);
			} else if (e instanceof ClosedChannelException) {
				setSocketError(ECLOSED);
			} else if (e instanceof AsynchronousCloseException) {
				setSocketError(ECLOSED);
			} else if (e instanceof ClosedByInterruptException) {
				setSocketError(ECLOSED);
			} else if (e instanceof BindException) {
				setSocketError(EADDRNOTAVAIL);
			} else if (e instanceof IOException) {
				setSocketError(EIO);
			} else {
				setSocketError(-1); // Unknown error
			}
		}

		protected void setError(IOException e) {
			setSocketError(e);
			setErrno(error);
		}

		protected void setError(int error) {
			setSocketError(error);
			setErrno(this.error);
		}

		protected void clearError() {
			error = 0;
			setErrno(0);
		}

		public int getReceiveBufferSize() {
			return receiveBufferSize;
		}

		public int setReceiveBufferSize(int receiveBufferSize) {
			this.receiveBufferSize = receiveBufferSize;

			return 0;
		}

		public int getSendBufferSize() {
			return sendBufferSize;
		}

		public int setSendBufferSize(int sendBufferSize) {
			this.sendBufferSize = sendBufferSize;

			return 0;
		}

		public boolean isReuseAddress() {
			return reuseAddress;
		}

		public int setReuseAddress(boolean reuseAddress) {
			this.reuseAddress = reuseAddress;
			return 0;
		}

		public boolean isKeepAlive() {
			return keepAlive;
		}

		public int setKeepAlive(boolean keepAlive) {
			this.keepAlive = keepAlive;
			return 0;
		}

		public boolean isLingerEnabled() {
			return lingerEnabled;
		}

		public int getLinger() {
			return linger;
		}

		public int setLinger(boolean enabled, int linger) {
			this.lingerEnabled = enabled;
			this.linger = linger;
			return 0;
		}

		public boolean isTcpNoDelay() {
			return tcpNoDelay;
		}

		public int setTcpNoDelay(boolean tcpNoDelay) {
			this.tcpNoDelay = tcpNoDelay;
			return 0;
		}

		protected void storeBytes(int address, int length, byte[] bytes) {
			if (length > 0) {
				IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, length, 1);
				for (int i = 0; i < length; i++) {
					memoryWriter.writeNext(bytes[i]);
				}
				memoryWriter.flush();
			}
		}

		public Selector getSelector(Selector selector, RawSelector rawSelector) {
			return selector;
		}

		public int recv(int buffer, int bufferLength, int flags) {
			if ((flags & ~MSG_DONTWAIT) != 0) {
				log.warn(String.format("sceNetInetRecv unsupported flag 0x%X on socket", flags));
			}
			return recv(buffer, bufferLength, flags, null);
		}

		public void blockedRecv(BlockingReceiveState blockingState) {
			if (blockingState.isTimeout()) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetInetRecv socket=%d returning %d (timeout)", getUid(), blockingState.receivedLength));
				}
				setErrno(EAGAIN);
				unblockThread(blockingState, blockingState.receivedLength);
			} else {
				int length = recv(blockingState.buffer + blockingState.receivedLength, blockingState.bufferLength - blockingState.receivedLength, blockingState.flags, blockingState);
				if (length >= 0) {
					unblockThread(blockingState, blockingState.receivedLength);
				}
			}
		}

		public int send(int buffer, int bufferLength, int flags) {
			if ((flags & ~MSG_DONTWAIT) != 0) {
				log.warn(String.format("sceNetInetSend unsupported flag 0x%X on socket", flags));
			}
			return send(buffer, bufferLength, flags, null);
		}

		public void blockedSend(BlockingSendState blockingState) {
			if (blockingState.isTimeout()) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetInetSend socket=%d returning %d (timeout)", getUid(), blockingState.sentLength));
				}
				setErrno(EAGAIN);
				unblockThread(blockingState, blockingState.sentLength);
			} else {
				int length = send(blockingState.buffer + blockingState.sentLength, blockingState.bufferLength - blockingState.sentLength, blockingState.flags, blockingState);
				if (length > 0) {
					unblockThread(blockingState, blockingState.sentLength);
				}
			}
		}

		public int sendto(int buffer, int bufferLength, int flags, pspNetSockAddrInternet toAddr) {
			if ((flags & ~MSG_DONTWAIT) != 0) {
				log.warn(String.format("sceNetInetSendto unsupported flag 0x%X on socket", flags));
			}
			return sendto(buffer, bufferLength, flags, toAddr, null);
		}

		public void blockedSendto(BlockingSendToState blockingState) {
			if (blockingState.isTimeout()) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetInetSendto socket=%d returning %d (timeout)", getUid(), blockingState.sentLength));
				}
				setErrno(EAGAIN);
				unblockThread(blockingState, blockingState.sentLength);
			} else {
				int length = sendto(blockingState.buffer + blockingState.sentLength, blockingState.bufferLength - blockingState.sentLength, blockingState.flags, blockingState.toAddr, blockingState);
				if (length > 0) {
					unblockThread(blockingState, blockingState.sentLength);
				}
			}
		}

		public int recvfrom(int buffer, int bufferLength, int flags, pspNetSockAddrInternet fromAddr) {
			if ((flags & ~MSG_DONTWAIT) != 0) {
				log.warn(String.format("sceNetInetRecvfrom unsupported flag 0x%X on socket", flags));
			}
			return recvfrom(buffer, bufferLength, flags, fromAddr, null);
		}

		public void blockedRecvfrom(BlockingReceiveFromState blockingState) {
			if (blockingState.isTimeout()) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetInetRecvfrom socket=%d returning %d (timeout)", getUid(), blockingState.receivedLength));
				}
				setErrno(EAGAIN);
				unblockThread(blockingState, blockingState.receivedLength);
			} else {
				int length = recvfrom(blockingState.buffer + blockingState.receivedLength, blockingState.bufferLength - blockingState.receivedLength, blockingState.flags, blockingState.fromAddr, blockingState);
				if (length >= 0) {
					unblockThread(blockingState, blockingState.receivedLength);
				}
			}
		}

		public int accept(pspNetSockAddrInternet sockAddrInternet) {
			return accept(sockAddrInternet, null);
		}

		public void blockedAccept(BlockingAcceptState blockingState) {
			int socketUid = accept(blockingState.acceptAddr, blockingState);
			if (socketUid >= 0) {
				unblockThread(blockingState, socketUid);
			}
		}
	}

	protected class pspInetStreamSocket extends pspInetSocket {
		private SocketChannel socketChannel;
		private ServerSocketChannel serverSocketChannel;
		private boolean isServerSocket;
		private SocketAddress pendingBindAddress;
		private int backlog;

		public pspInetStreamSocket(int uid) {
			super(uid);
		}

		private void configureSocketChannel() throws IOException {
			// We have to use non-blocking sockets at Java level
			// to allow further PSP thread scheduling while the PSP is
			// waiting for a blocking operation.
			socketChannel.configureBlocking(false);

			socketChannel.socket().setReceiveBufferSize(receiveBufferSize);
			socketChannel.socket().setSendBufferSize(sendBufferSize);
			socketChannel.socket().setKeepAlive(keepAlive);
			socketChannel.socket().setReuseAddress(reuseAddress);
			socketChannel.socket().setSoLinger(lingerEnabled, linger);
			socketChannel.socket().setTcpNoDelay(tcpNoDelay);

			// Connect has no timeout
			socketChannel.socket().setSoTimeout(0);
		}

		private void openChannel() throws IOException {
			if (isServerSocket) {
				if (serverSocketChannel == null) {
					serverSocketChannel = ServerSocketChannel.open();

					// We have to use non-blocking sockets at Java level
					// to allow further PSP thread scheduling while the PSP is
					// waiting for a blocking operation.
					serverSocketChannel.configureBlocking(false);

					if (socketChannel != null) {
						// If the socket was already bound, remember the bind address.
						// It will be rebound in bindChannel().
						if (socketChannel.socket().getLocalSocketAddress() != null) {
							pendingBindAddress = socketChannel.socket().getLocalSocketAddress();
						}
						socketChannel.close();
						socketChannel = null;
					}
				}
			} else {
				if (socketChannel == null) {
					socketChannel = SocketChannel.open();
					configureSocketChannel();
				}
			}
		}

		private void bindChannel() throws IOException {
			if (pendingBindAddress != null) {
				if (isServerSocket) {
					serverSocketChannel.socket().bind(pendingBindAddress, backlog);
				} else {
					socketChannel.socket().bind(pendingBindAddress);
				}
				pendingBindAddress = null;
			}
		}

		@Override
		public boolean finishConnect() {
			if (!isBlocking() && socketChannel.isConnectionPending()) {
				// Try to finish the connection
				try {
					return socketChannel.finishConnect();
				} catch (IOException e) {
					log.error(e);
					setSocketError(e);
					return false;
				}
			}

			return true;
		}

		@Override
		public int connect(pspNetSockAddrInternet addr) {
			if (isServerSocket) {
				log.error(String.format("connect not supported on server socket stream addr=%s, %s", addr.toString(), toString()));
				return -1;
			}

			try {
				openChannel();
				bindChannel();
				// On non-blocking, the connect might still be in progress
				if (!finishConnect()) {
					// Connect already in progress
					setErrno(EALREADY);
					return -1;
				}
				if (socketChannel.isConnected()) {
					// Already connected
					setErrno(EISCONN);
					return -1;
				}

				boolean connected = socketChannel.connect(getSocketAddress(addr));

				if (isBlocking()) {
					// blocking mode: wait for the connection to complete
					while (!socketChannel.finishConnect()) {
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							// Ignore exception
						}
					}
				} else if (!connected) {
					// non-blocking mode: return EINPROGRESS
					setErrno(EINPROGRESS);
					return -1;
				}
			} catch (IOException e) {
				log.error(e);
				setError(e);
				return -1;
			}

			clearError();
			return 0;
		}

		@Override
		public int bind(pspNetSockAddrInternet addr) {
			try {
				openChannel();
				if (isServerSocket) {
					pendingBindAddress = getSocketAddress(addr);
				} else {
					pendingBindAddress = null;
					socketChannel.socket().bind(getSocketAddress(addr));
				}
			} catch (IOException e) {
				log.error(e);
				setError(e);
				return -1;
			}

			clearError();
			return 0;
		}

		@Override
		public int recv(int buffer, int bufferLength, int flags, BlockingReceiveState blockingState) {
			try {
				// On non-blocking, the connect might still be in progress
				if (!finishConnect()) {
					setErrno(EAGAIN);
					return -1;
				}

				byte[] bytes = new byte[bufferLength];
				int length = socketChannel.read(ByteBuffer.wrap(bytes));
				storeBytes(buffer, length, bytes);

				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetInetRecv socket=%d received %d bytes", getUid(), length));
					if (log.isTraceEnabled()) {
						log.trace(String.format("Received data: %s", Utilities.getMemoryDump(buffer, length)));
					}
				}

				// end of stream
				if (length < 0) {
					clearError();
					return 0;
				}

				// Nothing received on a non-blocking stream, return EAGAIN in errno
				if (length == 0 && !isBlocking(flags)) {
					setErrno(EAGAIN);
					return -1;
				}

				if (blockingState != null) {
					blockingState.receivedLength += length;
				}

				// With a blocking stream, at least the low water mark has to be read
				if (isBlocking(flags)) {
					if (blockingState == null) {
						blockingState = new BlockingReceiveState(this, buffer, bufferLength, flags, length);
					}

					// If we have not yet read as much as the low water mark,
					// block the thread and retry later.
					if (blockingState.receivedLength < getReceiveLowWaterMark() && length < bufferLength) {
						blockThread(blockingState);
						return -1;
					}
				}

				clearError();
				return length;
			} catch (IOException e) {
				log.error(e);
				setError(e);
				return -1;
			}
		}

		@Override
		public int send(int buffer, int bufferLength, int flags, BlockingSendState blockingState) {
			try {
				// On non-blocking, the connect might still be in progress
				if (!finishConnect()) {
					setError(ENOTCONN);
					return -1;
				}

				ByteBuffer byteBuffer = getByteBuffer(buffer, bufferLength);
				int length = socketChannel.write(byteBuffer);
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetInetSend socket=%d successfully sent %d bytes", getUid(), length));
				}

				// Nothing sent on a non-blocking stream, return EAGAIN in errno
				if (length == 0 && !isBlocking(flags)) {
					setErrno(EAGAIN);
					return -1;
				}

				if (blockingState != null) {
					blockingState.sentLength += length;
				}

				// With a blocking stream, we have to send all the bytes
				if (isBlocking(flags)) {
					if (blockingState == null) {
						blockingState = new BlockingSendState(this, buffer, bufferLength, flags, length);
					}

					// If we have not yet sent all the bytes, block the thread
					// and retry later
					if (length < bufferLength) {
						blockThread(blockingState);
						return -1;
					}
				}

				clearError();
				return length;
			} catch (IOException e) {
				log.error(e);
				setError(e);
				return -1;
			}
		}

		@Override
		public int close() {
			if (socketChannel != null) {
				try {
					socketChannel.close();
					socketChannel = null;
				} catch (IOException e) {
					log.error(e);
					setError(e);
					return -1;
				}
			}

			if (serverSocketChannel != null) {
				try {
					serverSocketChannel.close();
					serverSocketChannel = null;
				} catch (IOException e) {
					log.error(e);
					setError(e);
					return -1;
				}
			}

			clearError();
			return 0;
		}

		@Override
		public int recvfrom(int buffer, int bufferLength, int flags, pspNetSockAddrInternet fromAddr, BlockingReceiveFromState blockingState) {
			log.warn("sceNetInetRecvfrom not supported on stream socket");
			setError(-1);
			return -1;
		}

		@Override
		public int sendto(int buffer, int bufferLength, int flags, pspNetSockAddrInternet toAddr, BlockingSendToState blockingState) {
			log.warn("sceNetInetSendto not supported on stream socket");
			setError(-1);
			return -1;
		}

		@Override
		public SelectableChannel getSelectableChannel() {
			if (isServerSocket) {
				return serverSocketChannel;
			}
			return socketChannel;
		}

		@Override
		public boolean isValid() {
			if (isServerSocket) {
				return serverSocketChannel != null;
			}

			if (socketChannel == null) {
				return false;
			}

			if (socketChannel.isConnectionPending()) {
				// Finish the connection otherwise, the channel will never
				// be readable/writable
				try {
					socketChannel.finishConnect();
				} catch (IOException e) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("%s: %s", toString(), e.toString()));
					}
					return false;
				}
			} else if (!socketChannel.isConnected()) {
				return false;
			}

			return !socketChannel.socket().isClosed();
		}

		@Override
		public int setReceiveBufferSize(int receiveBufferSize) {
			super.setReceiveBufferSize(receiveBufferSize);
			if (socketChannel != null) {
				try {
					socketChannel.socket().setReceiveBufferSize(receiveBufferSize);
				} catch (SocketException e) {
					setError(e);
					return -1;
				}
			}

			return 0;
		}

		@Override
		public int setSendBufferSize(int sendBufferSize) {
			super.setSendBufferSize(sendBufferSize);
			if (socketChannel != null) {
				try {
					socketChannel.socket().setSendBufferSize(sendBufferSize);
				} catch (SocketException e) {
					setError(e);
					return -1;
				}
			}

			return 0;
		}

		@Override
		public int getPeername(pspNetSockAddrInternet sockAddrInternet) {
			if (socketChannel == null) {
				return -1;
			}

			InetAddress inetAddress = socketChannel.socket().getInetAddress();
			sockAddrInternet.readFromInetAddress(inetAddress);

			return 0;
		}

		@Override
		public int getSockname(pspNetSockAddrInternet sockAddrInternet) {
			if (socketChannel == null) {
				return -1;
			}

			InetAddress inetAddress = socketChannel.socket().getLocalAddress();
			sockAddrInternet.readFromInetAddress(inetAddress);

			return 0;
		}

		@Override
		public int setKeepAlive(boolean keepAlive) {
			super.setKeepAlive(keepAlive);
			if (socketChannel != null) {
				try {
					socketChannel.socket().setKeepAlive(keepAlive);
				} catch (SocketException e) {
					log.error(e);
					setError(e);
					return -1;
				}
			}

			return 0;
		}

		@Override
		public int setLinger(boolean enabled, int linger) {
			super.setLinger(enabled, linger);
			if (socketChannel != null) {
				try {
					socketChannel.socket().setSoLinger(enabled, linger);
				} catch (SocketException e) {
					log.error(e);
					setError(e);
					return -1;
				}
			}

			return 0;
		}

		@Override
		public int setReuseAddress(boolean reuseAddress) {
			super.setReuseAddress(reuseAddress);
			if (socketChannel != null) {
				try {
					socketChannel.socket().setReuseAddress(reuseAddress);
				} catch (SocketException e) {
					log.error(e);
					setError(e);
					return -1;
				}
			}

			return 0;
		}

		@Override
		public int setTcpNoDelay(boolean tcpNoDelay) {
			super.setTcpNoDelay(tcpNoDelay);
			if (socketChannel != null) {
				try {
					socketChannel.socket().setTcpNoDelay(tcpNoDelay);
				} catch (SocketException e) {
					log.error(e);
					setError(e);
					return -1;
				}
			}

			return 0;
		}

		@Override
		public int shutdown(int how) {
			if (socketChannel != null) {
				try {
					switch (how) {
						case SHUT_RD:
							socketChannel.socket().shutdownInput();
							break;
						case SHUT_WR:
							socketChannel.socket().shutdownOutput();
							break;
						case SHUT_RDWR:
							socketChannel.socket().shutdownInput();
							socketChannel.socket().shutdownOutput();
							break;
					}
				} catch (IOException e) {
					log.error(e);
					setError(e);
					return -1;
				}
			}

			return 0;
		}

		@Override
		public int listen(int backlog) {
			isServerSocket = true;
			this.backlog = backlog;
			try {
				openChannel();
				bindChannel();
			} catch (IOException e) {
				log.error(e);
				setError(e);
				return -1;
			}

			return 0;
		}

		@Override
		public int accept(pspNetSockAddrInternet sockAddrInternet) {
			if (!isServerSocket) {
				log.error(String.format("sceNetInetAccept on non-server socket stream not allowed addr=%s, %s", sockAddrInternet.toString(), toString()));
				return -1;
			}
			return super.accept(sockAddrInternet);
		}

		@Override
		public int accept(pspNetSockAddrInternet sockAddrInternet, BlockingAcceptState blockingState) {
			SocketChannel socketChannel;
			try {
				openChannel();
				bindChannel();
				socketChannel = serverSocketChannel.accept();
			} catch (IOException e) {
				log.error(e);
				setError(e);
				return -1;
			}

			if (socketChannel == null) {
				if (isBlocking()) {
					if (blockingState == null) {
						blockingState = new BlockingAcceptState(this, sockAddrInternet);
					}
					blockThread(blockingState);
					return -1;
				}

				setErrno(EWOULDBLOCK);
				return -1;
			}

			pspInetStreamSocket inetSocket = (pspInetStreamSocket) createSocket(SOCK_STREAM, 0);
			inetSocket.socketChannel = socketChannel;
			inetSocket.copySocketAttributes(this);
			try {
				inetSocket.configureSocketChannel();
			} catch (IOException e) {
				log.error(e);
			}
			sockAddrInternet.readFromInetSocketAddress((InetSocketAddress) socketChannel.socket().getRemoteSocketAddress());
			sockAddrInternet.write(Memory.getInstance());

			if (log.isDebugEnabled()) {
				log.debug(String.format("sceNetInetAccept accepted connection from %s on socket %s", sockAddrInternet.toString(), inetSocket.toString()));
			} else if (log.isInfoEnabled()) {
				log.info(String.format("sceNetInetAccept accepted connection from %s", sockAddrInternet.toString()));
			}

			return inetSocket.getUid();
		}
	}

	protected class pspInetDatagramSocket extends pspInetSocket {
		private DatagramChannel datagramChannel;

		public pspInetDatagramSocket(int uid) {
			super(uid);

			// Datagrams have different default buffer sizes
			receiveBufferSize = 41600;
			sendBufferSize = 9216;
		}

		private void openChannel() throws IOException {
			if (datagramChannel == null) {
				datagramChannel = DatagramChannel.open();
				// We have to use non-blocking sockets at Java level
				// to allow further PSP thread scheduling while the PSP is
				// waiting for a blocking operation.
				datagramChannel.configureBlocking(false);

				datagramChannel.socket().setReceiveBufferSize(receiveBufferSize);
				datagramChannel.socket().setSendBufferSize(sendBufferSize);
			}
		}

		@Override
		public int connect(pspNetSockAddrInternet addr) {
			try {
				openChannel();
				datagramChannel.connect(getSocketAddress(addr));
			} catch (IOException e) {
				log.error(e);
				setError(e);
				return -1;
			}

			clearError();
			return 0;
		}

		@Override
		public int bind(pspNetSockAddrInternet addr) {
			try {
				openChannel();
				datagramChannel.socket().bind(getSocketAddress(addr));
			} catch (IOException e) {
				log.error(e);
				setError(e);
				return -1;
			}

			clearError();
			return 0;
		}

		@Override
		public int close() {
			if (datagramChannel != null) {
				try {
					datagramChannel.close();
					datagramChannel = null;
				} catch (IOException e) {
					log.error(e);
					setError(e);
					return -1;
				}
			}

			clearError();
			return 0;
		}

		@Override
		public int recv(int buffer, int bufferLength, int flags, BlockingReceiveState blockingState) {
			try {
				byte[] bytes = new byte[bufferLength];
				ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
				SocketAddress socketAddress = datagramChannel.receive(byteBuffer);
				int length = byteBuffer.position();
				storeBytes(buffer, length, bytes);

				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetInetRecv socket=%d received %d bytes from %s", getUid(), length, socketAddress));
					if (log.isTraceEnabled()) {
						log.trace(String.format("Received data: %s", Utilities.getMemoryDump(buffer, length)));
					}
				}

				if (length < 0) {
					// end of stream
					clearError();
					return 0;
				}

				// Nothing received on a non-blocking stream, return EAGAIN in errno
				if (length == 0 && !isBlocking(flags)) {
					setErrno(EAGAIN);
					return -1;
				}

				if (blockingState != null) {
					blockingState.receivedLength += length;
				}

				// With a blocking stream, at least the low water mark has to be read
				if (isBlocking(flags)) {
					if (blockingState == null) {
						blockingState = new BlockingReceiveState(this, buffer, bufferLength, flags, length);
					}

					// If we have not yet read as much as the low water mark,
					// block the thread and retry later.
					if (blockingState.receivedLength < getReceiveLowWaterMark() && length < bufferLength) {
						blockThread(blockingState);
						return -1;
					}
				}

				clearError();
				return length;
			} catch (IOException e) {
				log.error(e);
				setError(e);
				return -1;
			}
		}

		@Override
		public int send(int buffer, int bufferLength, int flags, BlockingSendState blockingState) {
			log.warn("sceNetInetSend not supported on datagram socket");
			setError(-1);
			return -1;
		}

		@Override
		public int recvfrom(int buffer, int bufferLength, int flags, pspNetSockAddrInternet fromAddr, BlockingReceiveFromState blockingState) {
			try {
				byte[] bytes = new byte[bufferLength];
				ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
				SocketAddress socketAddress = datagramChannel.receive(byteBuffer);
				int length = byteBuffer.position();
				storeBytes(buffer, length, bytes);

				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetInetRecvfrom socket=%d received %d bytes from %s", getUid(), length, socketAddress));
					if (log.isTraceEnabled()) {
						log.trace(String.format("Received data: %s", Utilities.getMemoryDump(buffer, length)));
					}
				}

				if (socketAddress == null) {
					// Nothing received on a non-blocking datagram, return EAGAIN in errno
					if (!isBlocking(flags)) {
						setErrno(EAGAIN);
						return -1;
					}

					// Nothing received on a blocking datagram, block the thread
					if (blockingState == null) {
						blockingState = new BlockingReceiveFromState(this, buffer, bufferLength, flags, fromAddr, length);
					}
					blockThread(blockingState);
					return -1;
				}

				if (socketAddress instanceof InetSocketAddress) {
					InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
					fromAddr.readFromInetSocketAddress(inetSocketAddress);
					fromAddr.write(Memory.getInstance());
				}

				clearError();
				return length;
			} catch (IOException e) {
				log.error(e);
				setError(e);
				return -1;
			}
		}

		@Override
		public int sendto(int buffer, int bufferLength, int flags, pspNetSockAddrInternet toAddr, BlockingSendToState blockingState) {
			try {
				openChannel();
				ByteBuffer byteBuffer = getByteBuffer(buffer, bufferLength);
				SocketAddress socketAddress = getSocketAddress(toAddr);
				int length = datagramChannel.send(byteBuffer, socketAddress);
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetInetSendto socket=%d successfully sent %d bytes", getUid(), length));
				}

				// Nothing sent on a non-blocking stream, return EAGAIN in errno
				if (length == 0 && !isBlocking(flags)) {
					setErrno(EAGAIN);
					return -1;
				}

				if (blockingState != null) {
					blockingState.sentLength += length;
				}

				// With a blocking stream, we have to send all the bytes
				if (isBlocking(flags)) {
					if (blockingState == null) {
						blockingState = new BlockingSendToState(this, buffer, bufferLength, flags, toAddr, length);
					}

					// If we have not yet sent all the bytes, block the thread
					// and retry later
					if (length < bufferLength) {
						blockThread(blockingState);
						return -1;
					}
				}

				clearError();
				return length;
			} catch (IOException e) {
				log.error(e);
				setError(e);
				return -1;
			}
		}

		@Override
		public int setBroadcast(boolean broadcast) {
			super.setBroadcast(broadcast);
			try {
				openChannel();
				datagramChannel.socket().setBroadcast(broadcast);
			} catch (IOException e) {
				log.error(e);
				setError(e);
				return -1;
			}

			clearError();
			return 0;
		}

		@Override
		public SelectableChannel getSelectableChannel() {
			return datagramChannel;
		}

		@Override
		public boolean isValid() {
			return datagramChannel != null && !datagramChannel.socket().isClosed();
		}

		@Override
		public int setReceiveBufferSize(int receiveBufferSize) {
			super.setReceiveBufferSize(receiveBufferSize);
			if (datagramChannel != null) {
				try {
					datagramChannel.socket().setReceiveBufferSize(receiveBufferSize);
				} catch (SocketException e) {
					setError(e);
					return -1;
				}
			}

			return 0;
		}

		@Override
		public int setSendBufferSize(int sendBufferSize) {
			super.setSendBufferSize(sendBufferSize);
			if (datagramChannel != null) {
				try {
					datagramChannel.socket().setSendBufferSize(sendBufferSize);
				} catch (SocketException e) {
					setError(e);
					return -1;
				}
			}

			return 0;
		}

		@Override
		public int getPeername(pspNetSockAddrInternet sockAddrInternet) {
			if (datagramChannel == null) {
				return -1;
			}

			InetAddress inetAddress = datagramChannel.socket().getInetAddress();
			sockAddrInternet.readFromInetAddress(inetAddress);

			return 0;
		}

		@Override
		public int getSockname(pspNetSockAddrInternet sockAddrInternet) {
			if (datagramChannel == null) {
				return -1;
			}

			InetAddress inetAddress = datagramChannel.socket().getLocalAddress();
			sockAddrInternet.readFromInetAddress(inetAddress);

			return 0;
		}

		@Override
		public int setReuseAddress(boolean reuseAddress) {
			super.setReuseAddress(reuseAddress);
			if (datagramChannel != null) {
				try {
					datagramChannel.socket().setReuseAddress(reuseAddress);
				} catch (SocketException e) {
					log.error(e);
					setError(e);
					return -1;
				}
			}

			return 0;
		}

		@Override
		public int shutdown(int how) {
			log.error(String.format("Shutdown not supported on datagram socket: how=%d, %s", how, toString()));
			return -1;
		}

		@Override
		public boolean finishConnect() {
			// Nothing to do for datagrams
			return true;
		}

		@Override
		public int listen(int backlog) {
			log.error(String.format("Listen not supported on datagram socket: backlog=%d, %s", backlog, toString()));
			return -1;
		}

		@Override
		public int accept(pspNetSockAddrInternet sockAddrInternet, BlockingAcceptState blockingState) {
			log.error(String.format("Accept not supported on datagram socket: sockAddrInternet=%s, %s", sockAddrInternet.toString(), toString()));
			return -1;
		}
	}

	protected class pspInetRawSocket extends pspInetSocket {
		private RawChannel rawChannel;
		private int protocol;
		private boolean isAvailable;

		public pspInetRawSocket(int uid, int protocol) {
			super(uid);
			this.protocol = protocol;
			isAvailable = true;
		}

		protected boolean openChannel() throws IllegalStateException {
			if (!isAvailable) {
				return false;
			}

			if (rawChannel == null) {
				try {
					rawChannel = new RawChannel();
				} catch (UnsatisfiedLinkError e) {
					log.error(String.format("The rocksaw library is not available on your system (%s). This library is required to implement RAW sockets. Disabling this feature.", e.toString()));
					isAvailable = false;
					return false;
				}

				try {
					rawChannel.socket().open(RawSocket.PF_INET, protocol);

					// Use non-blocking IO's
					rawChannel.configureBlocking(false);
				} catch (IOException e) {
					log.error(String.format("You need to start Jpcsp with administator right to be able to open RAW sockets (%s). Disabling this feature.", e.toString()));
					isAvailable = false;
					return false;
				}
			}

			return rawChannel.socket().isOpen();
		}

		@Override
		public int bind(pspNetSockAddrInternet addr) {
			if (!openChannel()) {
				return -1;
			}

			try {
				rawChannel.socket().bind(getInetAddress(addr));
			} catch (IllegalStateException e) {
				log.error(e);
				return -1;
			} catch (UnknownHostException e) {
				log.error(e);
				setError(e);
				return -1;
			} catch (IOException e) {
				log.error(e);
				setError(e);
				return -1;
			}

			return 0;
		}

		@Override
		public int close() {
			if (rawChannel != null) {
				try {
					rawChannel.close();
				} catch (IOException e) {
					log.error(e);
					setError(e);
					return -1;
				} finally {
					rawChannel = null;
				}
			}

			return 0;
		}

		@Override
		public int connect(pspNetSockAddrInternet addr) {
			log.error(String.format("sceNetInetConnect is not supported on Raw sockets: %s", toString()));
			return -1;
		}

		@Override
		public boolean finishConnect() {
			return openChannel();
		}

		@Override
		public int getPeername(pspNetSockAddrInternet sockAddrInternet) {
			log.error(String.format("sceNetInetGetpeername is not supported on Raw sockets: %s", toString()));
			return -1;
		}

		@Override
		public SelectableChannel getSelectableChannel() {
			if (!openChannel()) {
				return null;
			}
			return rawChannel;
		}

		@Override
		public int getSockname(pspNetSockAddrInternet sockAddrInternet) {
			log.error(String.format("sceNetInetGetsockname is not supported on Raw sockets: %s", toString()));
			return -1;
		}

		@Override
		public boolean isValid() {
			return openChannel();
		}

		@Override
		public int listen(int backlog) {
			log.error(String.format("sceNetInetListen is not supported on Raw sockets: %s", toString()));
			return -1;
		}

		@Override
		public int recv(int buffer, int bufferLength, int flags, BlockingReceiveState blockingState) {
			log.error(String.format("sceNetInetRecv is not supported on Raw sockets: %s", toString()));
			return -1;
		}

		@Override
		public int recvfrom(int buffer, int bufferLength, int flags, pspNetSockAddrInternet fromAddr, BlockingReceiveFromState blockingState) {
			try {
				if (!openChannel()) {
					return -1;
				}

				// Nothing available for read?
				if (!rawChannel.socket().isSelectedForRead()) {
					if (!isBlocking(flags)) {
						// Nothing received on a non-blocking stream, return EAGAIN in errno
						setErrno(EAGAIN);
						return -1;
					}

					if (blockingState == null) {
						blockingState = new BlockingReceiveFromState(this, buffer, bufferLength, flags, fromAddr, 0);
					}

					// Block the thread and retry later.
					blockThread(blockingState);
					return -1;
				}

				byte[] bytes = new byte[bufferLength];
				byte[] address = new byte[4];
				int length = rawChannel.socket().read(bytes, address);
				storeBytes(buffer, length, bytes);

				if (blockingState != null) {
					blockingState.receivedLength += length;
				}

				fromAddr.sin_family = AF_INET;
				fromAddr.sin_addr = bytesToInternetAddress(address);
				fromAddr.write(Memory.getInstance());

				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetInetRecvfrom socket=%d received %d bytes from %s", getUid(), length, fromAddr));
					if (log.isTraceEnabled()) {
						log.trace(String.format("Received data: %s", Utilities.getMemoryDump(buffer, length)));
					}
				}

				return length;
			} catch (InterruptedIOException e) {
				log.error(e);
				setError(e);
				return -1;
			} catch (IOException e) {
				log.error(e);
				setError(e);
				return -1;
			}
		}

		@Override
		public int send(int buffer, int bufferLength, int flags, BlockingSendState blockignState) {
			log.error(String.format("sceNetInetSend is not supported on Raw sockets: %s", toString()));
			return -1;
		}

		@Override
		public int sendto(int buffer, int bufferLength, int flags, pspNetSockAddrInternet toAddr, BlockingSendToState blockingState) {
			try {
				if (!openChannel()) {
					return -1;
				}

				// Ready for write?
				if (!rawChannel.socket().isSelectedForWrite()) {
					if (!isBlocking(flags)) {
						// Nothing sent on a non-blocking stream, return EAGAIN in errno
						setErrno(EAGAIN);
						return -1;
					}

					// With a blocking stream, we have to send all the bytes
					if (blockingState == null) {
						blockingState = new BlockingSendToState(this, buffer, bufferLength, flags, toAddr, 0);
					}

					// Block the thread and retry later
					blockThread(blockingState);
					return -1;
				}

				InetAddress inetAddress = getInetAddress(toAddr);
				byte[] data = getByteArray(buffer, bufferLength);
				int length = rawChannel.socket().write(inetAddress, data);
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetInetSendto socket=%d successfully sent %d bytes", getUid(), length));
				}

				if (blockingState != null) {
					blockingState.sentLength += length;
				}

				return length;
			} catch (IllegalStateException e) {
				log.error(e);
				return -1;
			} catch (IOException e) {
				log.error(e);
				setError(e);
				return -1;
			}
		}

		@Override
		public int shutdown(int how) {
			log.warn(String.format("sceNetInetShutdown is not supported on Raw sockets: %s", toString()));
			return 0;
		}

		@Override
		public Selector getSelector(Selector selector, RawSelector rawSelector) {
			return rawSelector;
		}

		@Override
		public int accept(pspNetSockAddrInternet sockAddrInternet, BlockingAcceptState blockingState) {
			log.error(String.format("sceNetInetAccept is not supported on Raw sockets: %s", toString()));
			return -1;
		}
	}

	protected static class pspInetPollFd extends pspAbstractMemoryMappedStructure {
		public int fd;
		public int events;
		public int revents;

		@Override
		protected void read() {
			fd = read32();
			events = read16();
			revents = read16();
		}

		@Override
		protected void write() {
			write32(fd);
			write16((short) events);
			write16((short) revents);
		}

		@Override
		public int sizeof() {
			return 8;
		}

		@Override
		public String toString() {
			return String.format("PollFd[fd=%d, events=0x%04X(%s), revents=0x%04X(%s)]", fd, events, getPollEventName(events), revents, getPollEventName(revents));
		}
	}

	protected HashMap<Integer, pspInetSocket> sockets;
	protected static final String idPurpose = "sceNetInet-socket";

	@Override
	public void start() {
		setSettingsListener("network.broadcastAddress", new BroadcastAddressSettingsListener());

		sockets = new HashMap<Integer, pspInetSocket>();

        super.start();
	}

	@Override
	public void stop() {
		// Close all the open sockets
		for (pspInetSocket inetSocket : sockets.values()) {
			inetSocket.close();
		}
		sockets.clear();

        super.stop();
	}

	/**
	 * Set the errno to an error value.
	 * Each thread has its own errno.
	 *
	 * @param errno
	 */
	public static void setErrno(int errno) {
		Modules.ThreadManForUserModule.getCurrentThread().errno = errno;
	}

	/**
	 * Return the current value of the errno.
	 * Each thread has its own errno.
	 *
	 * @return the errno of the current thread
	 */
	public static int getErrno() {
		return Modules.ThreadManForUserModule.getCurrentThread().errno;
	}

	protected int createSocketId() {
		// A socket ID has to be a number [1..255],
		// because sceNetInetSelect can handle only 256 bits.
		// 0 is considered by LuaPLayer as an invalid value as well.
		return SceUidManager.getNewId(idPurpose, 1, 255);
	}

	protected pspInetSocket createSocket(int type, int protocol) {
		int uid = createSocketId();
    	pspInetSocket inetSocket = null;
    	if (type == SOCK_STREAM) {
    		inetSocket = new pspInetStreamSocket(uid);
    	} else if (type == SOCK_DGRAM) {
    		inetSocket = new pspInetDatagramSocket(uid);
    	} else if (type == SOCK_RAW) {
    		inetSocket = new pspInetRawSocket(uid, protocol);
    	} else if (type == SOCK_STREAM_UNKNOWN_10) {
    		inetSocket = new pspInetStreamSocket(uid);
    	}
    	sockets.put(uid, inetSocket);

    	return inetSocket;
	}

	protected void releaseSocketId(int id) {
		SceUidManager.releaseId(id, idPurpose);
	}

	protected int readSocketList(Selector selector, RawSelector rawSelector, TPointer address, int n, int selectorOperation, String comment) {
		int closedSocketsCount = 0;

		if (address.isNotNull()) {
			LinkedList<Integer> closedChannels = new LinkedList<Integer>();
			int length = (n + 7) / 8;
			if (selectorOperation != 0) {
				IMemoryReader memoryReader = MemoryReader.getMemoryReader(address.getAddress(), length, 4);
				int value = 0;
				for (int socket = 0; socket < n; socket++) {
					if ((socket % 32) == 0) {
						value = memoryReader.readNext();
					}
					int bit = (value & 1);
					value = value >>> 1;
					if (bit != 0) {
						pspInetSocket inetSocket = sockets.get(socket);
						if (inetSocket != null) {
							SelectableChannel selectableChannel = inetSocket.getSelectableChannel();
							if (selectableChannel != null) {
								int registeredOperation = selectorOperation & selectableChannel.validOps();
								if (registeredOperation != 0) {
									Selector socketSelector = inetSocket.getSelector(selector, rawSelector);

									// A channel may be registered at most once with any particular selector
									if (selectableChannel.isRegistered()) {
										// If the channel is already registered,
										// add the new operation to the active registration
										SelectionKey selectionKey = selectableChannel.keyFor(socketSelector);
										selectionKey.interestOps(selectionKey.interestOps() | registeredOperation);
									} else {
										try {
											selectableChannel.register(socketSelector, registeredOperation, new Integer(socket));
										} catch (ClosedChannelException e) {
											closedChannels.add(socket);
											if (log.isDebugEnabled()) {
												log.debug(String.format("%s: %s", inetSocket.toString(), e.toString()));
											}
										}
									}
								}
							}
						}
					}
				}
			}

			// Clear the socket list so that we just have to set the bits for
			// the sockets that are ready.
			address.clear(length);

			// and set the bit for all the closed channels
			for (Integer socket : closedChannels) {
				setSelectBit(address, socket);
				closedSocketsCount++;
			}
		}

		return closedSocketsCount;
	}

	protected String dumpSelectBits(TPointer addr, int n) {
		if (addr.isNull() || n <= 0) {
			return "";
		}

		StringBuilder dump = new StringBuilder();
		for (int socket = 0; socket < n; socket++) {
			int bit = 1 << (socket % 8);
			int value = addr.getValue8(socket / 8);
			if ((value & bit) != 0) {
				if (dump.length() > 0) {
					dump.append(", ");
				}
				dump.append(String.format("%d", socket));
			}
		}

		return dump.toString();
	}

	protected void setSelectBit(TPointer addr, int socket) {
		if (addr.isNotNull()) {
			int offset = socket / 8;
			int value = 1 << (socket % 8);
			addr.setValue8(offset, (byte) (addr.getValue8(offset) | value));
		}
	}

	protected void blockThread(BlockingState blockingState) {
		if (!blockingState.threadBlocked) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Blocking the current thread %s", Modules.ThreadManForUserModule.getCurrentThread().toString()));
			}
			Modules.ThreadManForUserModule.hleBlockCurrentThread(SceKernelThreadInfo.JPCSP_WAIT_NET, blockingState);
			blockingState.threadBlocked = true;
		}
		long schedule = Emulator.getClock().microTime() + BLOCKED_OPERATION_POLLING_MICROS;
		Emulator.getScheduler().addAction(schedule, blockingState);
	}

	protected void unblockThread(BlockingState blockingState, int returnValue) {
		SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getThreadById(blockingState.threadId);
		if (thread != null) {
			thread.cpuContext._v0 = returnValue;
		}
		if (blockingState.threadBlocked) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Unblocking the thread %s", thread.toString()));
			}
			Modules.ThreadManForUserModule.hleUnblockThread(blockingState.threadId);
			blockingState.threadBlocked = false;
		}
	}

	protected int checkInvalidSelectedSockets(BlockingSelectState blockingState) {
		int countInvalidSocket = 0;

		// Check for valid sockets.
		// When a socket is no longer valid (e.g. connect failed),
		// return the select bit for this socket so that the application
		// has a chance to see the failed connection.
		for (SelectionKey selectionKey : blockingState.selector.keys()) {
			if (selectionKey.isValid()) {
				int socket = (Integer) selectionKey.attachment();
				pspInetSocket inetSocket = sockets.get(socket);
				if (inetSocket == null || !inetSocket.isValid()) {
					countInvalidSocket++;

					int interestOps;
					try {
						interestOps = selectionKey.interestOps();
					} catch (CancelledKeyException e) {
						// The key has been cancelled, set the selection bit for all operations
						interestOps = SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT | SelectionKey.OP_ACCEPT;
					}

					if ((interestOps & readSelectionKeyOperations) != 0) {
						setSelectBit(blockingState.readSocketsAddr, socket);
					}
					if ((interestOps & writeSelectionKeyOperations) != 0) {
						setSelectBit(blockingState.writeSocketsAddr, socket);
					}
					// Out-of-band data is not implemented (not supported by Java?)
				}
			}
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("checkInvalidSelectedSockets returns %d", countInvalidSocket));
		}

		return countInvalidSocket;
	}

	protected void setPollResult(pspInetPollFd[] pollFds, int socket, int revents) {
		for (int i = 0; i < pollFds.length; i++) {
			if (pollFds[i].fd == socket) {
				pollFds[i].revents |= revents;
				break;
			}
		}
	}

	protected void blockedPoll(BlockingPollState blockingState) {
		try {
			// Try to finish all the pending connections
			for (int i = 0; i < blockingState.pollFds.length; i++) {
				pspInetSocket inetSocket = sockets.get(blockingState.pollFds[i].fd);
				if (inetSocket != null) {
					inetSocket.finishConnect();
				}
			}

			// We do not want to block here on the selector, call selectNow
			int count = blockingState.selector.selectNow();

			boolean threadBlocked;
			if (count <= 0) {
				// Check for timeout
				if (blockingState.isTimeout()) {
					// Timeout, unblock the thread and return 0
					if (log.isDebugEnabled()) {
						log.debug(String.format("sceNetInetPoll returns %d sockets (timeout)", count));
					}
					threadBlocked = false;
				} else {
					// No timeout, keep blocking the thread
					threadBlocked = true;
				}
			} else {
				// Some sockets are ready, unblock the thread and return the count
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetInetPoll returns %d sockets", count));
				}

				for (Iterator<SelectionKey> it = blockingState.selector.selectedKeys().iterator(); it.hasNext(); ) {
					// Retrieve the next key and remove it from the set
					SelectionKey selectionKey = it.next();
					it.remove();

					if (selectionKey.isReadable()) {
						int socket = (Integer) selectionKey.attachment();
						setPollResult(blockingState.pollFds, socket, POLLIN | POLLRDNORM);
					}
					if (selectionKey.isWritable()) {
						int socket = (Integer) selectionKey.attachment();
						setPollResult(blockingState.pollFds, socket, POLLOUT);
					}
				}

				threadBlocked = false;
			}

			if (threadBlocked) {
				blockThread(blockingState);
			} else {
				// We do no longer need the selector, close it
				blockingState.selector.close();

				// Write back the updated revents fields
				Memory mem = Memory.getInstance();
				for (int i = 0; i < blockingState.pollFds.length; i++) {
					blockingState.pollFds[i].write(mem);
					if (log.isDebugEnabled()) {
						log.debug(String.format("sceNetInetPoll returning pollFd[%d]=%s", i, blockingState.pollFds[i].toString()));
					}
				}

				// sceNetInetPoll can now return the count, unblock the thread
				unblockThread(blockingState, count);
			}
		} catch (IOException e) {
			log.error(e);
		}
	}

	protected void processSelectedKey(Selector selector, BlockingSelectState blockingState) {
		for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
			// Retrieve the next key and remove it from the set
			SelectionKey selectionKey = it.next();
			it.remove();

			if ((selectionKey.readyOps() & readSelectionKeyOperations) != 0) {
				int socket = (Integer) selectionKey.attachment();
				setSelectBit(blockingState.readSocketsAddr, socket);
			}
			if ((selectionKey.readyOps() & writeSelectionKeyOperations) != 0) {
				int socket = (Integer) selectionKey.attachment();
				setSelectBit(blockingState.writeSocketsAddr, socket);
			}
		}
	}

	protected void blockedSelect(BlockingSelectState blockingState) {
		try {
			// Try to finish all the pending connections
			for (Iterator<SelectionKey> it = blockingState.selector.keys().iterator(); it.hasNext(); ) {
				SelectionKey selectionKey = it.next();
				Integer socket = (Integer) selectionKey.attachment();
				pspInetSocket inetSocket = sockets.get(socket);
				if (inetSocket != null) {
					inetSocket.finishConnect();
				}
			}

			// Start with the count of closed channels
			// (detected when registering the selector)
			int count = blockingState.count;
			// We do not want to block here on the selector, call selectNow
			count += blockingState.selector.selectNow();
			count += blockingState.rawSelector.selectNow();
			// add any socket becoming invalid (e.g. connect failed)
			count += checkInvalidSelectedSockets(blockingState);

			boolean threadBlocked;
			if (count <= 0) {
				// Check for timeout
				if (blockingState.isTimeout()) {
					// Timeout, unblock the thread and return 0
					if (log.isDebugEnabled()) {
						log.debug(String.format("sceNetInetSelect returns %d sockets (timeout)", count));
					}
					threadBlocked = false;
				} else {
					// No timeout, keep blocking the thread
					threadBlocked = true;
				}
			} else {
				// Some sockets are ready, unblock the thread and return the count
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetInetSelect returns %d sockets", count));
				}

				processSelectedKey(blockingState.selector, blockingState);
				processSelectedKey(blockingState.rawSelector, blockingState);

				threadBlocked = false;
			}

			if (threadBlocked) {
				blockThread(blockingState);
			} else {
				// We do no longer need the selectors, close them
				blockingState.selector.close();
				blockingState.rawSelector.close();

				if (log.isDebugEnabled() && count > 0) {
					if (blockingState.readSocketsAddr.isNotNull()) {
						log.debug(String.format("sceNetInetSelect returning Read Sockets       : %s", dumpSelectBits(blockingState.readSocketsAddr, blockingState.numberSockets)));
					}
					if (blockingState.writeSocketsAddr.isNotNull()) {
						log.debug(String.format("sceNetInetSelect returning Write Sockets      : %s", dumpSelectBits(blockingState.writeSocketsAddr, blockingState.numberSockets)));
					}
					if (blockingState.outOfBandSocketsAddr.isNotNull()) {
						log.debug(String.format("sceNetInetSelect returning Out-of-band Sockets: %s", dumpSelectBits(blockingState.outOfBandSocketsAddr, blockingState.numberSockets)));
					}
				}

				// sceNetInetSelect can now return the count, unblock the thread
				unblockThread(blockingState, count);
			}
		} catch (IOException e) {
			log.error(e);
		}
	}

	public static String internetAddressToString(int address) {
		int n4 = (address >> 24) & 0xFF;
		int n3 = (address >> 16) & 0xFF;
		int n2 = (address >>  8) & 0xFF;
		int n1 = (address      ) & 0xFF;

		return String.format("%d.%d.%d.%d", n1, n2, n3, n4);
	}

	public static int bytesToInternetAddress(byte[] bytes) {
		if (bytes == null) {
			return 0;
		}

		int inetAddress = 0;
		for (int i = bytes.length - 1; i >= 0; i--) {
			inetAddress = (inetAddress << 8) | (bytes[i] & 0xFF);
		}

		return inetAddress;
	}

	public static byte[] internetAddressToBytes(int address) {
		byte[] bytes = new byte[4];

		int n4 = (address >> 24) & 0xFF;
		int n3 = (address >> 16) & 0xFF;
		int n2 = (address >>  8) & 0xFF;
		int n1 = (address      ) & 0xFF;
		bytes[3] = (byte) n4;
		bytes[2] = (byte) n3;
		bytes[1] = (byte) n2;
		bytes[0] = (byte) n1;

		return bytes;
	}

	protected static String getSocketTypeNameString(int type) {
		if (type < 0 || type >= socketTypeNames.length) {
			return String.format("Unknown type %d", type);
		}

		return socketTypeNames[type];
	}

	protected static String getPollEventName(int event) {
		StringBuilder name = new StringBuilder();

		if ((event & POLLIN) != 0) {
			name.append("|POLLIN");
		}
		if ((event & POLLPRI) != 0) {
			name.append("|POLLPRI");
		}
		if ((event & POLLOUT) != 0) {
			name.append("|POLLOUT");
		}
		if ((event & POLLERR) != 0) {
			name.append("|POLLERR");
		}
		if ((event & POLLHUP) != 0) {
			name.append("|POLLHUP");
		}
		if ((event & POLLNVAL) != 0) {
			name.append("|POLLNVAL");
		}
		if ((event & POLLRDNORM) != 0) {
			name.append("|POLLRDNORM");
		}
		if ((event & POLLRDBAND) != 0) {
			name.append("|POLLRDBAND");
		}
		if ((event & POLLWRBAND) != 0) {
			name.append("|POLLWRBAND");
		}

		if (name.length() > 0 && name.charAt(0) == '|') {
			name.deleteCharAt(0);
		}

		return name.toString();
	}

	protected static String getOptionNameString(int optionName) {
		switch (optionName) {
			case SO_DEBUG: return "SO_DEBUG";
			case SO_ACCEPTCONN: return "SO_ACCEPTCONN";
			case SO_REUSEADDR: return "SO_REUSEADDR";
			case SO_KEEPALIVE: return "SO_KEEPALIVE";
			case SO_DONTROUTE: return "SO_DONTROUTE";
			case SO_BROADCAST: return "SO_BROADCAST";
			case SO_USELOOPBACK: return "SO_USELOOPBACK";
			case SO_LINGER: return "SO_LINGER";
			case SO_OOBINLINE: return "SO_OOBINLINE";
			case SO_REUSEPORT: return "SO_REUSEPORT";
			case SO_TIMESTAMP: return "SO_TIMESTAMP";
			case SO_ONESBCAST: return "SO_ONESBCAST";
			case SO_SNDBUF: return "SO_SNDBUF";
			case SO_RCVBUF: return "SO_RCVBUF";
			case SO_SNDLOWAT: return "SO_SNDLOWAT";
			case SO_RCVLOWAT: return "SO_RCVLOWAT";
			case SO_SNDTIMEO: return "SO_SNDTIMEO";
			case SO_RCVTIMEO: return "SO_RCVTIMEO";
			case SO_ERROR: return "SO_ERROR";
			case SO_TYPE: return "SO_TYPE";
			case SO_NONBLOCK: return "SO_NONBLOCK";
			default: return String.format("Unknown option 0x%X", optionName);
		}
	}

	public int checkSocket(int socket) {
		if (!sockets.containsKey(socket)) {
			log.warn(String.format("checkSocket invalid socket=0x%X", socket));
			throw new SceKernelErrorException(-1); // Unknown error code
		}

		return socket;
	}

	public int checkAddressLength(int addressLength) {
		if (addressLength < 16) {
			log.warn(String.format("checkAddressLength invalid addressLength=%d", addressLength));
			throw new SceKernelErrorException(-1); // Unknown error code
		}

		return addressLength;
	}

	// int sceNetInetInit(void);
	@HLEFunction(nid = 0x17943399, version = 150)
	public int sceNetInetInit() {
		return 0;
	}

	// int sceNetInetTerm(void);
	@HLEFunction(nid = 0xA9ED66B9, version = 150)
	public int sceNetInetTerm() {
		return 0;
	}

	// int sceNetInetAccept(int s, struct sockaddr *addr, socklen_t *addrlen);
	@HLEFunction(nid = 0xDB094E1B, version = 150)
	public int sceNetInetAccept(@CheckArgument("checkSocket") int socket, TPointer address, TPointer32 addressLengthAddr) {
		pspInetSocket inetSocket = sockets.get(socket);
		pspNetSockAddrInternet sockAddrInternet = new pspNetSockAddrInternet();

		int addressLength = addressLengthAddr.getValue();
		// addressLength is unsigned int
		if (addressLength < 0) {
			addressLength = Integer.MAX_VALUE;
		}
		sockAddrInternet.setMaxSize(addressLength);
		sockAddrInternet.write(address);
		if (sockAddrInternet.sizeof() < addressLength) {
			addressLengthAddr.setValue(sockAddrInternet.sizeof());
		}

		return inetSocket.accept(sockAddrInternet);
	}

	// int sceNetInetBind(int socket, const struct sockaddr *address, socklen_t address_len);
	@HLEFunction(nid = 0x1A33F9AE, version = 150)
	public int sceNetInetBind(@CheckArgument("checkSocket") int socket, pspNetSockAddrInternet sockAddrInternet, @CheckArgument("checkAddressLength") int addressLength) {
		if (sockAddrInternet.sin_family != AF_INET) {
			log.warn(String.format("sceNetInetBind invalid socket address family=%d", sockAddrInternet.sin_family));
			return -1;
		}

		pspInetSocket inetSocket = sockets.get(socket);
		int result = inetSocket.bind(sockAddrInternet);

		if (result == 0) {
			if (log.isInfoEnabled()) {
				log.info(String.format("sceNetInetBind binding to %s", sockAddrInternet.toString()));
			}
		} else {
			log.warn(String.format("sceNetInetBind failed binding to %s", sockAddrInternet.toString()));
		}

		return result;
	}

	// int sceNetInetClose(int s);
	@HLEFunction(nid = 0x8D7284EA, version = 150)
	public int sceNetInetClose(@CheckArgument("checkSocket") int socket) {
		pspInetSocket inetSocket = sockets.get(socket);
		int result = inetSocket.close();
		releaseSocketId(socket);
        sockets.remove(socket);

        return result;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x805502DD, version = 150)
	public int sceNetInetCloseWithRST() {
		return 0;
	}

	// int sceNetInetConnect(int socket, const struct sockaddr *serv_addr, socklen_t addrlen);
	@HLEFunction(nid = 0x410B34AA, version = 150)
	public int sceNetInetConnect(@CheckArgument("checkSocket") int socket, pspNetSockAddrInternet sockAddrInternet, @CheckArgument("checkAddressLength") int addressLength) {
		if (sockAddrInternet.sin_family != AF_INET) {
			log.warn(String.format("sceNetInetConnect invalid socket address family=%d", sockAddrInternet.sin_family));
			return -1;
		}

		pspInetSocket inetSocket = sockets.get(socket);
		int result = inetSocket.connect(sockAddrInternet);

		if (result == 0) {
			if (log.isInfoEnabled()) {
				log.info(String.format("sceNetInetConnect connected to %s", sockAddrInternet.toString()));
			}
		} else {
			if (getErrno() == EINPROGRESS) {
				if (log.isInfoEnabled()) {
					log.info(String.format("sceNetInetConnect connecting to %s", sockAddrInternet.toString()));
				}
			} else {
				log.warn(String.format("sceNetInetConnect failed connecting to %s (errno=%d)", sockAddrInternet.toString(), getErrno()));
			}
		}

		return result;
	}

	@HLEFunction(nid = 0xE247B6D6, version = 150)
	public int sceNetInetGetpeername(@CheckArgument("checkSocket") int socket, TPointer address, TPointer32 addressLengthAddr) {
		pspNetSockAddrInternet sockAddrInternet = new pspNetSockAddrInternet();
		pspInetSocket inetSocket = sockets.get(socket);
		int result = inetSocket.getSockname(sockAddrInternet);

		sockAddrInternet.setMaxSize(addressLengthAddr.getValue());
		sockAddrInternet.write(address);
		addressLengthAddr.setValue(sockAddrInternet.sizeof());

		return result;
	}

	@HLEFunction(nid = 0x162E6FD5, version = 150)
	public int sceNetInetGetsockname(@CheckArgument("checkSocket") int socket, TPointer address, TPointer32 addressLengthAddr) {
		pspNetSockAddrInternet sockAddrInternet = new pspNetSockAddrInternet();
		pspInetSocket inetSocket = sockets.get(socket);
		int result = inetSocket.getSockname(sockAddrInternet);

		sockAddrInternet.setMaxSize(addressLengthAddr.getValue());
		sockAddrInternet.write(address);
		addressLengthAddr.setValue(sockAddrInternet.sizeof());

		return result;
	}

	@HLEFunction(nid = 0x4A114C7C, version = 150)
	public int sceNetInetGetsockopt(@CheckArgument("checkSocket") int socket, int level, int optionName, TPointer optionValue, @CanBeNull TPointer32 optionLengthAddr) {
		int optionLength = optionLengthAddr.isNull() ? 0 : optionLengthAddr.getValue();

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetInetGetsockopt optionName=%s, optionLength=%d", getOptionNameString(optionName), optionLength));
		}

		pspInetSocket inetSocket = sockets.get(socket);

		if (optionName == SO_ERROR && optionLength >= 4) {
			optionValue.setValue32(inetSocket.getErrorAndClear());
			if (log.isDebugEnabled()) {
				log.debug(String.format("sceNetInetGetsockopt SO_ERROR returning %d", optionValue.getValue32()));
			}
			optionLengthAddr.setValue(4);
		} else if (optionName == SO_NONBLOCK && optionLength >= 4) {
			optionValue.setValue32(inetSocket.isBlocking());
			optionLengthAddr.setValue(4);
		} else if (optionName == SO_BROADCAST && optionLength >= 4) {
			optionValue.setValue32(inetSocket.isBroadcast());
			optionLengthAddr.setValue(4);
		} else if (optionName == SO_RCVLOWAT && optionLength >= 4) {
			optionValue.setValue32(inetSocket.getReceiveLowWaterMark());
			optionLengthAddr.setValue(4);
		} else if (optionName == SO_SNDLOWAT && optionLength >= 4) {
			optionValue.setValue32(inetSocket.getSendLowWaterMark());
			optionLengthAddr.setValue(4);
		} else if (optionName == SO_RCVTIMEO && optionLength >= 4) {
			int timeout = inetSocket.getReceiveTimeout();
			// Returning 0 for "no timeout" value
			optionValue.setValue32(timeout == pspInetSocket.NO_TIMEOUT_INT ? 0 : timeout);
			optionLengthAddr.setValue(4);
		} else if (optionName == SO_SNDTIMEO && optionLength >= 4) {
			int timeout = inetSocket.getSendTimeout();
			// Returning 0 for "no timeout" value
			optionValue.setValue32(timeout == pspInetSocket.NO_TIMEOUT_INT ? 0 : timeout);
			optionLengthAddr.setValue(4);
		} else if (optionName == SO_RCVBUF && optionLength >= 4) {
			optionValue.setValue32(inetSocket.getReceiveBufferSize());
			optionLengthAddr.setValue(4);
		} else if (optionName == SO_SNDBUF && optionLength >= 4) {
			optionValue.setValue32(inetSocket.getSendBufferSize());
			optionLengthAddr.setValue(4);
		} else if (optionName == SO_KEEPALIVE && optionLength >= 4) {
			optionValue.setValue32(inetSocket.isKeepAlive());
			optionLengthAddr.setValue(4);
		} else if (optionName == SO_LINGER && optionLength >= 8) {
			optionValue.setValue32(0, inetSocket.isLingerEnabled());
			optionValue.setValue32(4, inetSocket.getLinger());
			optionLengthAddr.setValue(8);
		} else if (optionName == SO_REUSEADDR && optionLength >= 4) {
			optionValue.setValue32(inetSocket.isReuseAddress());
			optionLengthAddr.setValue(4);
		} else {
			log.warn(String.format("Unimplemented sceNetInetGetsockopt socket=0x%X, level=0x%X, optionName=0x%X, optionValue=%s, optionLength=%s(0x%X)", socket, level, optionName, optionValue, optionLengthAddr, optionLength));
			return -1;
		}

		return 0;
	}

	// int sceNetInetListen(int s, int backlog);
	@HLEFunction(nid = 0xD10A1A7A, version = 150)
	public int sceNetInetListen(@CheckArgument("checkSocket") int socket, int backlog) {
		pspInetSocket inetSocket = sockets.get(socket);
		return inetSocket.listen(backlog);
	}

	/*
	 * sceNetInetPoll seems to work in a similar way to the BSD socket poll() function:
	 *
	 * int poll(struct pollfd *fds, nfds_t nfds, int timeout);
	 * fds      Points to an array of pollfd structures, which are defined as:
	 *            struct pollfd {
	 *                int fd;
	 *                short events;
	 *                short revents;
	 *            }
	 *          The fd member is an open file descriptor. If fd is -1, the
	 *          pollfd structure is considered unused, and revents will be
	 *          cleared.
	 *
	 *          The events and revents members are bitmasks of conditions to
	 *          monitor and conditions found, respectively.
	 *
	 * nfds     An unsigned integer specifying the number of pollfd structures
	 *          in the array.
	 *
	 * timeout  Maximum interval to wait for the poll to complete, in milliseconds.
	 *          If this value is 0, poll() will return immediately.
	 *          If this value is INFTIM (-1), poll() will block indefinitely until
	 *          a condition is found.
	 *
	 * The calling process sets the events bitmask and poll() sets the revents
	 * bitmask. Each call to poll() resets the revents bitmask for accuracy. The
	 * condition flags in the bitmasks are defined as:
	 *
	 * POLLIN      Data other than high-priority data may be read without blocking.
	 * POLLRDNORM  Normal data may be read without blocking.
	 * POLLRDBAND  Priority data may be read without blocking.
	 * POLLPRI     High-priority data may be read without blocking.
	 * POLLOUT     Normal data may be written without blocking.
	 * POLLWRBAND  Priority data may be written.
	 * POLLERR     An error has occurred on the device or socket. This flag is
	 *             only valid in the revents bitmask; it is ignored in the
	 *             events member.
	 * POLLHUP     The device or socket has been disconnected. This event and
	 *             POLLOUT are mutually-exclusive; a descriptor can never be
	 *             writable if a hangup has occurred. However, this event and
	 *             POLLIN, POLLRDNORM, POLLRDBAND, or POLLPRI are not mutually-
	 *             exclusive. This flag is only valid in the revents bitmask; it
	 *             is ignored in the events member.
	 * POLLNVAL    The corresponding file descriptor is invalid. This flag is
	 *             only valid in the revents bitmask; it is ignored in the
	 *             events member.
	 *
	 * Bitmask Values:
	 *   POLLIN     0x0001
	 *   POLLRDNORM 0x0040
	 *   POLLRDBAND 0x0080
	 *   POLLPRI    0x0002
	 *   POLLOUT    0x0004
	 *   POLLWRBAND 0x0100
	 *   POLLERR    0x0008
	 *   POLLHUP    0x0010
	 *   POLLNVAL   0x0020
	 *
	 * Return values:
	 *             Upon error, poll() returns -1 and sets the global variable errno
	 *             to indicate the error. If the timeout interval was reached before
	 *             any events occurred, poll() returns 0. Otherwise, poll() returns
	 *             the number of file descriptors for which revents is non-zero.
	 */
	@HLEFunction(nid = 0xFAABB1DD, version = 150)
	public int sceNetInetPoll(TPointer fds, int nfds, int timeout) {
		int result = 0;
		long timeoutUsec;
		if (timeout == POLL_INFTIM) {
			timeoutUsec = pspInetSocket.NO_TIMEOUT;
		} else {
			timeoutUsec = timeout * 1000L;
		}

		try {
			Selector selector = Selector.open();

			pspInetPollFd[] pollFds = new pspInetPollFd[nfds];
			for (int i = 0; i < nfds; i++) {
				pspInetPollFd pollFd = new pspInetPollFd();
				pollFd.read(fds, i * pollFd.sizeof());
				pollFds[i] = pollFd;

				if (pollFd.fd == -1) {
					pollFd.revents = 0;
				} else {
					pspInetSocket inetSocket = sockets.get(pollFd.fd);
					if (inetSocket == null) {
						pollFd.revents = POLLNVAL;
					} else {
						SelectableChannel selectableChannel = inetSocket.getSelectableChannel();
						if (selectableChannel == null) {
							pollFd.revents = POLLHUP;
						} else {
							int registeredOperations = 0;
							if ((pollFd.events & (POLLIN | POLLRDNORM | POLLRDBAND | POLLPRI)) != 0) {
								registeredOperations |= SelectionKey.OP_READ;
							}
							if ((pollFd.events & (POLLOUT | POLLWRBAND)) != 0) {
								registeredOperations |= SelectionKey.OP_WRITE;
							}
							registeredOperations &= selectableChannel.validOps();
							if (selectableChannel.isRegistered()) {
								log.warn(String.format("sceNetInetPoll channel already registered pollFd[%d]=%s", i, pollFd));
							} else {
								try {
									selectableChannel.register(selector, registeredOperations, new Integer(pollFd.fd));
									pollFd.revents = 0;
								} catch (ClosedChannelException e) {
									pollFd.revents = POLLHUP;
								}
							}
						}
					}
				}

				if (log.isDebugEnabled()) {
					log.debug(String.format("sceNetInetPoll pollFd[%d]=%s", i, pollFd));
				}
			}

			BlockingPollState blockingState = new BlockingPollState(selector, pollFds, timeoutUsec);

			SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
			thread.cpuContext._v0 = 0; // This will be overwritten by the execution of the blockingState
			setErrno(0);

			// Check if there are ready operations, otherwise, block the thread
			blockingState.execute();
			result = thread.cpuContext._v0;
		} catch (IOException e) {
			log.error("sceNetInetPoll", e);
			setErrno(-1);
			return -1;
		}

		return result;
	}

	// size_t  sceNetInetRecv(int s, void *buf, size_t len, int flags);
	@HLEFunction(nid = 0xCDA85C99, version = 150)
	public int sceNetInetRecv(@CheckArgument("checkSocket") int socket, TPointer buffer, int bufferLength, int flags) {
		pspInetSocket inetSocket = sockets.get(socket);
		return inetSocket.recv(buffer.getAddress(), bufferLength, flags);
	}

	// size_t  sceNetInetRecvfrom(int socket, void *buffer, size_t bufferLength, int flags, struct sockaddr *from, socklen_t *fromlen);
	@HLEFunction(nid = 0xC91142E4, version = 150)
	public int sceNetInetRecvfrom(@CheckArgument("checkSocket") int socket, TPointer buffer, int bufferLength, int flags, TPointer from, TPointer32 fromLengthAddr) {
		pspInetSocket inetSocket = sockets.get(socket);
		pspNetSockAddrInternet fromAddrInternet = new pspNetSockAddrInternet();

		int fromLength = fromLengthAddr.getValue();
		fromAddrInternet.setMaxSize(fromLength);
		fromAddrInternet.write(from);
		if (fromAddrInternet.sizeof() < fromLength) {
			fromLengthAddr.setValue(fromAddrInternet.sizeof());
		}

		return inetSocket.recvfrom(buffer.getAddress(), bufferLength, flags, fromAddrInternet);
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xEECE61D2, version = 150)
	public int sceNetInetRecvmsg() {
		return 0;
	}

	@HLEFunction(nid = 0x5BE8D595, version = 150)
	public int sceNetInetSelect(int numberSockets, @CanBeNull TPointer readSocketsAddr, @CanBeNull TPointer writeSocketsAddr, @CanBeNull TPointer outOfBandSocketsAddr, @CanBeNull TPointer32 timeoutAddr) {
		int result = 0;
		numberSockets = Math.min(numberSockets, 256);

		long timeoutUsec;
		if (timeoutAddr.isNotNull()) {
			// timeoutAddr points to the following structure:
			// - offset 0: int seconds
			// - offset 4: int microseconds
			timeoutUsec = timeoutAddr.getValue(0) * 1000000L;
			timeoutUsec += timeoutAddr.getValue(4);
		} else {
			// Take a very large value
			timeoutUsec = pspInetSocket.NO_TIMEOUT;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetInetSelect timeout=%d us", timeoutUsec));
			if (readSocketsAddr.isNotNull()) {
				log.debug(String.format("sceNetInetSelect Read Sockets       : %s", dumpSelectBits(readSocketsAddr, numberSockets)));
			}
			if (writeSocketsAddr.isNotNull()) {
				log.debug(String.format("sceNetInetSelect Write Sockets      : %s", dumpSelectBits(writeSocketsAddr, numberSockets)));
			}
			if (outOfBandSocketsAddr.isNotNull()) {
				log.debug(String.format("sceNetInetSelect Out-of-band Sockets: %s", dumpSelectBits(outOfBandSocketsAddr, numberSockets)));
			}
		}

		try {
			Selector selector = Selector.open();
			RawSelector rawSelector = RawSelector.open();

			int count = 0;

			// Read the socket list for the read operation and register them with the selector
			count += readSocketList(selector, rawSelector, readSocketsAddr, numberSockets, readSelectionKeyOperations, "readSockets");

			// Read the socket list for the write operation and register them with the selector
			count += readSocketList(selector, rawSelector, writeSocketsAddr, numberSockets, writeSelectionKeyOperations, "writeSockets");

			// Read the socket list for the out-of-band data and register them with the selector.
			// Out-of-band data is currently not implemented as I don't see any
			// support in Java for this rarely used feature.
			count += readSocketList(selector, rawSelector, outOfBandSocketsAddr, numberSockets, 0, "outOfBandSockets");

			BlockingSelectState blockingState = new BlockingSelectState(selector, rawSelector, numberSockets, readSocketsAddr, writeSocketsAddr, outOfBandSocketsAddr, timeoutUsec, count);

			setErrno(0);
			SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
			thread.cpuContext._v0 = 0; // This will be overwritten by the execution of the blockingState

			// Check if there are ready operations, otherwise, block the thread
			blockingState.execute();
			result = thread.cpuContext._v0;
		} catch (IOException e) {
			log.error(e);
			setErrno(-1);
			return -1;
		}

		return result;
	}

	// size_t sceNetInetSend(int socket, const void *buffer, size_t bufferLength, int flags);
	@HLEFunction(nid = 0x7AA671BC, version = 150)
	public int sceNetInetSend(@CheckArgument("checkSocket") int socket, TPointer buffer, int bufferLength, int flags) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("Send data: %s", Utilities.getMemoryDump(buffer.getAddress(), bufferLength)));
		}

		pspInetSocket inetSocket = sockets.get(socket);
		return inetSocket.send(buffer.getAddress(), bufferLength, flags);
	}

	// size_t sceNetInetSendto(int socket, const void *buffer, size_t bufferLength, int flags, const struct sockaddr *to, socklen_t tolen);
	@HLEFunction(nid = 0x05038FC7, version = 150)
	public int sceNetInetSendto(@CheckArgument("checkSocket") int socket, TPointer buffer, int bufferLength, int flags, pspNetSockAddrInternet toSockAddress, @CheckArgument("checkAddressLength") int toLength) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("Sendto data: %s", Utilities.getMemoryDump(buffer.getAddress(), bufferLength)));
		}

		if (toSockAddress.sin_family != AF_INET) {
			log.warn(String.format("sceNetInetSendto invalid socket address familiy sin_family=%d", toSockAddress.sin_family));
			return -1;
		}
		pspInetSocket inetSocket = sockets.get(socket);
		return inetSocket.sendto(buffer.getAddress(), bufferLength, flags, toSockAddress);
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x774E36F4, version = 150)
	public int sceNetInetSendmsg() {
		return 0;
	}

	// int sceNetInetSetsockopt(int socket, int level, int optname, const void *optval, socklen_t optlen);
	@HLEFunction(nid = 0x2FE71FE7, version = 150)
	public int sceNetInetSetsockopt(@CheckArgument("checkSocket") int socket, int level, int optionName, @CanBeNull TPointer optionValueAddr, int optionLength) {
		int result = 0;
		int optionValue = 0;
		if (optionValueAddr.isNotNull() && optionLength >= 4) {
			optionValue = optionValueAddr.getValue32();
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetInetSetsockopt optionName=%s", getOptionNameString(optionName)));
			if (log.isTraceEnabled()) {
				log.trace(String.format("Option value: %s", Utilities.getMemoryDump(optionValueAddr.getAddress(), optionLength)));
			}
		}

		pspInetSocket inetSocket = sockets.get(socket);

		if (level == SOL_SOCKET) {
			if (optionName == SO_NONBLOCK && optionLength == 4) {
				result = inetSocket.setBlocking(optionValue == 0);
			} else if (optionName == SO_BROADCAST && optionLength == 4) {
				result = inetSocket.setBroadcast(optionValue != 0);
			} else if (optionName == SO_RCVLOWAT && optionLength == 4) {
				inetSocket.setReceiveLowWaterMark(optionValue);
				result = 0;
			} else if (optionName == SO_SNDLOWAT && optionLength == 4) {
				inetSocket.setSendLowWaterMark(optionValue);
				result = 0;
			} else if (optionName == SO_RCVTIMEO && optionLength == 4) {
				// 0 means "no timeout"
				inetSocket.setReceiveTimeout(optionValue == 0 ? pspInetSocket.NO_TIMEOUT_INT : optionValue);
				result = 0;
			} else if (optionName == SO_SNDTIMEO && optionLength == 4) {
				// 0 means "no timeout"
				inetSocket.setSendTimeout(optionValue == 0 ? pspInetSocket.NO_TIMEOUT_INT : optionValue);
				result = 0;
			} else if (optionName == SO_RCVBUF && optionLength == 4) {
				result = inetSocket.setReceiveBufferSize(optionValue);
			} else if (optionName == SO_SNDBUF && optionLength == 4) {
				result = inetSocket.setSendBufferSize(optionValue);
			} else if (optionName == SO_KEEPALIVE && optionLength == 4) {
				result = inetSocket.setKeepAlive(optionValue != 0);
			} else if (optionName == SO_LINGER && optionLength == 8) {
				result = inetSocket.setLinger(optionValue != 0, optionValueAddr.getValue32(4));
			} else if (optionName == SO_REUSEADDR && optionLength == 4) {
				result = inetSocket.setReuseAddress(optionValue != 0);
			} else {
				log.warn(String.format("Unimplemented sceNetInetSetsockopt optionName=%s", getOptionNameString(optionName)));
				result = 0;
			}
		} else {
			log.warn(String.format("Unimplemented sceNetInetSetsockopt unknown level=0x%X, optionName=%s", level, getOptionNameString(optionName)));
			result = 0;
		}

		return result;
	}

	// int sceNetInetShutdown(int s, int how);
	@HLEFunction(nid = 0x4CFE4E56, version = 150)
	public int sceNetInetShutdown(@CheckArgument("checkSocket") int socket, int how) {
		if (how < SHUT_RD || how > SHUT_RDWR) {
			log.warn(String.format("sceNetInetShutdown invalid how=%d", how));
			return -1;
		}

		pspInetSocket inetSocket = sockets.get(socket);
		return inetSocket.shutdown(how);
	}

	// int sceNetInetSocket(int domain, int type, int protocol);
	@HLEFunction(nid = 0x8B7B220F, version = 150)
	public int sceNetInetSocket(int domain, int type, int protocol) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetInetSocket domain=0x%X, type=0x%X(%s), protocol=0x%X", domain, type, getSocketTypeNameString(type), protocol));
		}

		if (domain != AF_INET) {
			log.warn(String.format("sceNetInetSocket unsupported domain=0x%X, type=0x%X(%s), protocol=0x%X", domain, type, getSocketTypeNameString(type), protocol));
			return -1;
		}
		if (type != SOCK_DGRAM && type != SOCK_STREAM && type != SOCK_RAW && type != SOCK_STREAM_UNKNOWN_10) {
			log.warn(String.format("sceNetInetSocket unsupported type=0x%X(%s), domain=0x%X, protocol=0x%X", type, getSocketTypeNameString(type), domain, protocol));
			return -1;
		}

		pspInetSocket inetSocket = createSocket(type, protocol);
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetInetSocket created socket=0x%X", inetSocket.getUid()));
		}

		return inetSocket.getUid();
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x80A21ABD, version = 150)
	public int sceNetInetSocketAbort() {
		return 0;
	}

	@HLEFunction(nid = 0xFBABE411, version = 150)
	public int sceNetInetGetErrno() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetInetGetErrno returning 0x%08X(%1$d)", getErrno()));
		}

		return getErrno();
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xB3888AD4, version = 150)
	public int sceNetInetGetTcpcbstat() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x39B0C7D3, version = 150)
	public int sceNetInetGetUdpcbstat() {
		return 0;
	}

	@HLEFunction(nid = 0xB75D5B0A, version = 150)
	public int sceNetInetInetAddr(PspString name) {
		byte[] inetAddressBytes;
		try {
			inetAddressBytes = InetAddress.getByName(name.getString()).getAddress();
		} catch (UnknownHostException e) {
			log.error("sceNetInetInetAddr", e);
			return -1;
		}

		int inetAddress = bytesToInternetAddress(inetAddressBytes);
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetInetInetAddr %s returning 0x%08X", name, inetAddress));
		}

		return inetAddress;
	}

	@HLEFunction(nid = 0x1BDF5D13, version = 150)
	public int sceNetInetInetAton(PspString hostname, TPointer32 addr) {
		int result;
		try {
			InetAddress inetAddress = InetAddress.getByName(hostname.getString());
			int resolvedAddress = bytesToInternetAddress(inetAddress.getAddress());
			addr.setValue(resolvedAddress);
			if (log.isDebugEnabled()) {
				log.debug(String.format("sceNetInetInetAton returning address 0x%08X('%s')", resolvedAddress, sceNetInet.internetAddressToString(resolvedAddress)));
			} else if (log.isInfoEnabled()) {
				log.info(String.format("sceNetInetInetAton resolved '%s' into '%s'", hostname.getString(), sceNetInet.internetAddressToString(resolvedAddress)));
			}
			result = 1;
		} catch (UnknownHostException e) {
			log.error("sceNetInetInetAton", e);
			result = 0;
		}

		return result;
	}

	@HLEFunction(nid = 0xD0792666, version = 150)
	public int sceNetInetInetNtop(int family, TPointer32 srcAddr, TPointer buffer, int bufferLength) {
		if (family != AF_INET) {
			log.warn(String.format("sceNetInetInetNtop unsupported family 0x%X", family));
			return 0;
		}

		int addr = srcAddr.getValue();
		String ip = internetAddressToString(addr);
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetInetInetNtop returning %s for 0x%08X", ip, addr));
		}
		buffer.setStringNZ(bufferLength, ip);

		return buffer.getAddress();
	}

	@HLEFunction(nid = 0xE30B8C19, version = 150)
	public int sceNetInetInetPton(int family, PspString src, TPointer32 buffer) {
		int result;

		if (family != AF_INET) {
			log.warn(String.format("sceNetInetInetPton unsupported family 0x%X", family));
			return -1;
		}

		try {
			byte[] inetAddressBytes = InetAddress.getByName(src.getString()).getAddress();
			int inetAddress = bytesToInternetAddress(inetAddressBytes);
			buffer.setValue(inetAddress);
			if (log.isDebugEnabled()) {
				log.debug(String.format("sceNetInetInetPton returning 0x%08X for '%s'", inetAddress, src.getString()));
			}
			result = 1;
		} catch (UnknownHostException e) {
			log.warn(String.format("sceNetInetInetPton returned error '%s' for '%s'", e.toString(), src.getString()));
			result = 0;
		}

		return result;
	}

	@HLEFunction(nid = 0x8CA3A97E, version = 150)
	public int sceNetInetGetPspError() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetInetGetPspError returning 0x%08X(%1$d)", getErrno()));
		}

		return getErrno();
	}
}