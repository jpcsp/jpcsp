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
package jpcsp.network;

import static jpcsp.Allegrex.compiler.RuntimeContext.setLog4jMDC;
import static jpcsp.scheduler.Scheduler.getNow;
import static jpcsp.util.Utilities.getArray;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.network.protocols.EtherFrame;
import jpcsp.util.Utilities;

/**
 * Proxy Wlan Adapter ensuring that all the methods are asynchronous and are executed without delay.
 * 
 * @author gid15
 *
 */
public class AsyncWlanAdapter extends BaseWlanAdapter {
	private IWlanAdapter wlanAdapter;
	// For Wlan packets receiver
	private final Object wlanReceiverLock = new Object();
    private final List<byte[]> receivedWlanPackets = new LinkedList<byte[]>();
	private WlanPacketsReceiverThread wlanPacketsReceiverThread;
	private static final int maxReceivedWlanPacketsSize = 0; // 0 means no max limit
	// For GameMode packets receiver
    private final Object gameModeReceiverLock = new Object();
    private final List<byte[]> receivedGameModePackets = new LinkedList<byte[]>();
    private final List<pspNetMacAddress> receivedGameModeMacAddresses = new LinkedList<pspNetMacAddress>();
	private GameModePacketsReceiverThread gameModePacketsReceiverThread;
	// For Wlan packets sender
	private final Object wlanSenderLock = new Object();
	private final List<byte[]> sentWlanPackets = new LinkedList<byte[]>();
	private WlanPacketsSenderThread wlanPacketsSenderThread;
	// For GameMode packets sender
	private final Object gameModeSenderLock = new Object();
	private final List<byte[]> sentGameModePackets = new LinkedList<byte[]>();
	private final List<pspNetMacAddress> sentGameModeMacAddresses = new LinkedList<pspNetMacAddress>();
	private GameModePacketsSenderThread gameModePacketsSenderThread;
	// For AccessPoint packets sender
	private final Object accessPointSenderLock = new Object();
	private final List<byte[]> sentAccessPointPackets = new LinkedList<byte[]>();
	private final List<EtherFrame> sentAccessPointEtherFrames = new LinkedList<EtherFrame>();
	private AccessPointPacketsSenderThread accessPointPacketsSenderThread;

	private abstract class BaseThread extends Thread {
    	protected volatile boolean exit;

		public void exit() {
			exit = true;
		}
	}

	private abstract class BaseReceiverThread extends BaseThread {
    	protected final byte[] buffer = new byte[10000];
    	private static final int delay = 10000; // Microseconds

    	void delay(long start) {
    		int duration = (int) (getNow() - start);
    		if (duration < delay) {
				Utilities.sleep(delay - duration);
    		}
    	}
	}

	private abstract class BaseSenderThread extends BaseThread {
    	protected final Semaphore sync = new Semaphore(0);
    	private static final int delay = 1000; // Milliseconds

        protected boolean waitForSync() {
        	while (true) {
    	    	try {
    	    		int availablePermits = sync.drainPermits();
    	    		if (availablePermits > 0) {
    	    			break;
    	    		}

        			if (sync.tryAcquire(delay, TimeUnit.MILLISECONDS)) {
        				break;
        			}
    				return false;
    			} catch (InterruptedException e) {
    				// Ignore exception and retry again
    				if (log.isDebugEnabled()) {
    					log.debug(String.format("BaseSenderThread waitForSync %s", e));
    				}
    			}
        	}

        	return true;
        }

        public void sync() {
        	// Notify the thread that a new packet is available
        	sync.release();
    	}

		@Override
		public void exit() {
			// Notify the thread that we want to exit
			sync();

			super.exit();
		}
	}

	private class WlanPacketsReceiverThread extends BaseReceiverThread {
		@Override
		public void run() {
			setLog4jMDC();
			while (!exit) {
				try {
					long start = getNow();
					int length = wlanAdapter.receiveWlanPacket(buffer, 0, buffer.length);
					if (length < 0) {
						delay(start);
					} else {
						byte[] receivedWlanPacket = getArray(buffer, length);
						int size;
						boolean added;
						synchronized (wlanReceiverLock) {
							size = receivedWlanPackets.size();
							if (maxReceivedWlanPacketsSize > 0 && size >= maxReceivedWlanPacketsSize) {
								added = false;
							} else {
								receivedWlanPackets.add(receivedWlanPacket);
								added = true;
							}
						}

						if (!added) {
							if (log.isDebugEnabled()) {
								log.debug(String.format("Dropped packet, receivedWlanPackets size=%d", size));
							}
						} else if (size > 100) {
							if ((size % 10) == 0) {
								log.error(String.format("Extremely slow processing of network traffic (%d packets waiting in receive queue)", size));
							}
						} else if (size > 10) {
							if ((size % 10) == 0) {
								log.warn(String.format("Slow processing of network traffic (%d packets waiting in receive queue)", size));
							}
						} else {
							if (log.isTraceEnabled()) {
								log.trace(String.format("receivedWlanPackets size=%d", size));
							}
						}
					}
				} catch (IOException e) {
					log.error("receiveWlanPacket", e);
				}
			}
		}
    }

    private class GameModePacketsReceiverThread extends BaseReceiverThread {
    	private final pspNetMacAddress macAddress = new pspNetMacAddress();

		@Override
		public void run() {
			setLog4jMDC();
			while (!exit) {
				try {
					long start = getNow();
					int length = wlanAdapter.receiveGameModePacket(macAddress, buffer, 0, buffer.length);
					if (length < 0) {
						delay(start);
					} else {
						byte[] receivedGameModePacket = getArray(buffer, length);
						synchronized (gameModeReceiverLock) {
							receivedGameModePackets.add(receivedGameModePacket);
							receivedGameModeMacAddresses.add(new pspNetMacAddress(macAddress.macAddress));
						}
					}
				} catch (IOException e) {
					log.error("receiveGameModePacket", e);
				}
			}
		}
    }

    private class WlanPacketsSenderThread extends BaseSenderThread {
		@Override
		public void run() {
			setLog4jMDC();
			while (!exit) {
				if (waitForSync()) {
					while (!exit) {
						byte[] sentWlanPacket = null;
						synchronized (wlanSenderLock) {
							if (!sentWlanPackets.isEmpty()) {
								sentWlanPacket = sentWlanPackets.remove(0);
							}
						}

						if (sentWlanPacket == null) {
							break;
						}

						try {
							wlanAdapter.sendWlanPacket(sentWlanPacket, 0, sentWlanPacket.length);
						} catch (IOException e) {
							log.error("sendWlanPacket", e);
						}
					}
				}
			}
		}
    }

    private class GameModePacketsSenderThread extends BaseSenderThread {
		@Override
		public void run() {
			setLog4jMDC();
			while (!exit) {
				if (waitForSync()) {
					while (!exit) {
						byte[] sentGameModePacket = null;
						pspNetMacAddress sentGameModeMacAddress = null;
						synchronized (gameModeSenderLock) {
							if (!sentGameModePackets.isEmpty()) {
								sentGameModePacket = sentGameModePackets.remove(0);
								sentGameModeMacAddress = sentGameModeMacAddresses.remove(0);
							}
						}

						if (sentGameModePacket == null) {
							break;
						}

						try {
							wlanAdapter.sendGameModePacket(sentGameModeMacAddress, sentGameModePacket, 0, sentGameModePacket.length);
						} catch (IOException e) {
							log.error("sendGameModePacket", e);
						}
					}
				}
			}
		}
    }

    private class AccessPointPacketsSenderThread extends BaseSenderThread {
		@Override
		public void run() {
			setLog4jMDC();
			while (!exit) {
				if (waitForSync()) {
					while (!exit) {
						byte[] sentAccessPointPacket = null;
						EtherFrame sentAccessPointEtherFrame = null;
						synchronized (accessPointSenderLock) {
							if (!sentAccessPointPackets.isEmpty()) {
								sentAccessPointPacket = sentAccessPointPackets.remove(0);
								sentAccessPointEtherFrame = sentAccessPointEtherFrames.remove(0);
							}
						}

						if (sentAccessPointPacket == null) {
							break;
						}

						try {
							wlanAdapter.sendAccessPointPacket(sentAccessPointPacket, 0, sentAccessPointPacket.length, sentAccessPointEtherFrame);
						} catch (IOException e) {
							log.error("sendAccessPointPacket", e);
						}
					}
				}
			}
		}
    }

    public AsyncWlanAdapter(IWlanAdapter wlanAdapter) {
		this.wlanAdapter = wlanAdapter;
	}

	@Override
	public void start() throws IOException {
		// Start the Wlan Adapter before we start our asynchronous threads
		wlanAdapter.start();

		if (wlanPacketsReceiverThread == null) {
			wlanPacketsReceiverThread = new WlanPacketsReceiverThread();
			wlanPacketsReceiverThread.setName("AsyncWlanAdapter Wlan Packets Receiver Thread");
			wlanPacketsReceiverThread.setDaemon(true);
			wlanPacketsReceiverThread.start();
		}

		if (gameModePacketsReceiverThread == null) {
			gameModePacketsReceiverThread = new GameModePacketsReceiverThread();
			gameModePacketsReceiverThread.setName("AsyncWlanAdapter GameMode Packets Receiver Thread");
			gameModePacketsReceiverThread.setDaemon(true);
			gameModePacketsReceiverThread.start();
		}

		if (wlanPacketsSenderThread == null) {
			wlanPacketsSenderThread = new WlanPacketsSenderThread();
			wlanPacketsSenderThread.setName("AsyncWlanAdapter Wlan Packets Sender Thread");
			wlanPacketsSenderThread.setDaemon(true);
			wlanPacketsSenderThread.start();
		}

		if (gameModePacketsSenderThread == null) {
			gameModePacketsSenderThread = new GameModePacketsSenderThread();
			gameModePacketsSenderThread.setName("AsyncWlanAdapter GameMode Packets Sender Thread");
			gameModePacketsSenderThread.setDaemon(true);
			gameModePacketsSenderThread.start();
		}

		if (accessPointPacketsSenderThread == null) {
			accessPointPacketsSenderThread = new AccessPointPacketsSenderThread();
			accessPointPacketsSenderThread.setName("AsyncWlanAdapter AccessPoint Packets Sender Thread");
			accessPointPacketsSenderThread.setDaemon(true);
			accessPointPacketsSenderThread.start();
		}
	}

	@Override
	public void stop() throws IOException {
		if (wlanPacketsReceiverThread != null) {
			wlanPacketsReceiverThread.exit();
			wlanPacketsReceiverThread = null;
		}

		if (gameModePacketsReceiverThread != null) {
			gameModePacketsReceiverThread.exit();
			gameModePacketsReceiverThread = null;
		}

		if (wlanPacketsSenderThread != null) {
			wlanPacketsSenderThread.exit();
			wlanPacketsSenderThread = null;
		}

		if (gameModePacketsSenderThread != null) {
			gameModePacketsSenderThread.exit();
			gameModePacketsSenderThread = null;
		}

		if (accessPointPacketsSenderThread != null) {
			accessPointPacketsSenderThread.exit();
			accessPointPacketsSenderThread = null;
		}

		wlanAdapter.stop();
	}

	@Override
	public void sendWlanPacket(byte[] buffer, int offset, int length) {
		if (wlanPacketsSenderThread == null) {
			return;
		}

		byte[] sentWlanPacket = getArray(buffer, offset, length);

		synchronized (wlanSenderLock) {
			sentWlanPackets.add(sentWlanPacket);
		}

		wlanPacketsSenderThread.sync();
	}

	@Override
	public void sendAccessPointPacket(byte[] buffer, int offset, int length, EtherFrame etherFrame) {
		if (accessPointPacketsSenderThread == null) {
			return;
		}

		byte[] sentAccessPointPacket = getArray(buffer, offset, length);
		EtherFrame sentAccessPointEtherFrame = new EtherFrame(etherFrame);

		synchronized (accessPointSenderLock) {
			sentAccessPointPackets.add(sentAccessPointPacket);
			sentAccessPointEtherFrames.add(sentAccessPointEtherFrame);
		}

		accessPointPacketsSenderThread.sync();
	}

	@Override
	public void sendGameModePacket(pspNetMacAddress macAddress, byte[] buffer, int offset, int length) {
		if (gameModePacketsSenderThread == null) {
			return;
		}

		byte[] sentGameModePacket = getArray(buffer, offset, length);
		pspNetMacAddress sentGameModeMacAddress = new pspNetMacAddress(macAddress.macAddress);

		synchronized (gameModeSenderLock) {
			sentGameModePackets.add(sentGameModePacket);
			sentGameModeMacAddresses.add(sentGameModeMacAddress);
		}

		gameModePacketsSenderThread.sync();
	}

	@Override
	public int receiveWlanPacket(byte[] buffer, int offset, int length) {
		byte[] receivedWlanPacket;

		synchronized (wlanReceiverLock) {
			if (receivedWlanPackets.isEmpty()) {
				return -1;
			}
			receivedWlanPacket = receivedWlanPackets.remove(0);
		}

		int receivedLength = Math.min(receivedWlanPacket.length, length);
		System.arraycopy(receivedWlanPacket, 0, buffer, offset, receivedLength);

		return receivedLength;
	}

	@Override
	public int receiveGameModePacket(pspNetMacAddress macAddress, byte[] buffer, int offset, int length) {
		byte[] receivedGameModePacket;
		pspNetMacAddress receivedGameModeMacAddress;

		synchronized (gameModeReceiverLock) {
			if (receivedGameModePackets.isEmpty()) {
				return -1;
			}
			receivedGameModePacket = receivedGameModePackets.remove(0);
			receivedGameModeMacAddress = receivedGameModeMacAddresses.remove(0);
		}

		int receivedLength = Math.min(receivedGameModePacket.length, length);
		System.arraycopy(receivedGameModePacket, 0, buffer, offset, receivedLength);
		macAddress.setMacAddress(receivedGameModeMacAddress.macAddress);

		return receivedLength;
	}

	@Override
	public void wlanScan(String ssid, int[] channels) throws IOException {
		wlanAdapter.wlanScan(ssid, channels);
	}

	@Override
	public void sendChatMessage(String message) {
		wlanAdapter.sendChatMessage(message);
	}
}
