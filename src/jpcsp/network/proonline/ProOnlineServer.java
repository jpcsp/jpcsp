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
package jpcsp.network.proonline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;

import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.network.proonline.PacketFactory.SceNetAdhocctlConnectPacketS2C;
import jpcsp.network.proonline.PacketFactory.SceNetAdhocctlDisconnectPacketS2C;
import jpcsp.network.proonline.PacketFactory.SceNetAdhocctlPacketBaseC2S;
import jpcsp.network.proonline.PacketFactory.SceNetAdhocctlPacketBaseS2C;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

/*
 * Ported from ProOnline aemu server
 * https://code.google.com/p/aemu/source/browse/#hg%2Fpspnet_adhocctl_server
 */
public class ProOnlineServer {
	protected static Logger log = ProOnlineNetworkAdapter.log;
	private static ProOnlineServer instance;
	private ProOnlineServerThread serverThread;
	private static final int port = 27312;
	private ServerSocket serverSocket;
	private List<User> users;
	private PacketFactory packetFactory;
	private User currentUser;
	private List<Game> games;

	public static ProOnlineServer getInstance() {
		if (instance == null) {
			instance = new ProOnlineServer();
		}

		return instance;
	}

	private ProOnlineServer() {
	}

	private static class User {
		public Socket socket;
		public long lastReceiveTimestamp;
		public byte[] buffer = new byte[1000];
		public int bufferLength = 0;
		public pspNetMacAddress mac;
		public String nickName;
		public Game game;
		public Group group;
		public int ip;
		public String ipString;

		public boolean isTimeout() {
			boolean isTimeout = System.currentTimeMillis() - lastReceiveTimestamp > 15000;
			if (isTimeout) {
				log.debug(String.format("User timed out now=%d, lastReceiveTimestamp=%d", System.currentTimeMillis(), lastReceiveTimestamp));
			}
			return isTimeout;
		}

		@Override
		public String toString() {
			return String.format("%s (MAC: %s - IP: %s)", nickName, mac, ipString);
		}
	}

	private static class Game {
		public String name;
		public int playerCount;
		public List<Group> groups;

		public Game(String name) {
			this.name = name;
			groups = new LinkedList<Group>();
		}
	}

	private static class Group {
		public String name;
		public Game game;
		public List<User> players;

		public Group(String name, Game game) {
			this.name = name;
			this.game = game;
			players = new LinkedList<User>();
			if (game != null) {
				game.groups.add(this);
			}
		}
	}

	private class ProOnlineServerThread extends Thread {
		private boolean exit;

		@Override
		public void run() {
			log.debug(String.format("Starting ProOnlineServerThread"));
			while (!exit) {
				try {
					Socket socket = serverSocket.accept();
					socket.setSoTimeout(1);
					loginUserStream(socket);
				} catch (SocketTimeoutException e) {
					// Ignore timeout
				} catch (IOException e) {
					log.debug("Accept server socket", e);
				}

				for (User user : users) {
					int length = 0;
					try {
						InputStream is = user.socket.getInputStream();
						length = is.read(user.buffer, user.bufferLength, user.buffer.length - user.bufferLength);
					} catch (SocketTimeoutException e) {
						// Ignore timeout
					} catch (IOException e) {
						log.debug("Receive user socket", e);
					}

					if (length > 0) {
						user.bufferLength += length;
						user.lastReceiveTimestamp = System.currentTimeMillis();
						processUserStream(user);
					} else if (length < 0 || user.isTimeout()) {
						logoutUser(user);
					}
				}

				Utilities.sleep(10);
			}
		}

		public void exit() {
			exit = true;
		}
	}

	public void start() {
		packetFactory = new PacketFactory();

		try {
			serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(1);
		} catch (IOException e) {
			log.error(String.format("Server sockert at port %d not available: %s", port, e));
			return;
		}

		users = new LinkedList<ProOnlineServer.User>();
		games = new LinkedList<ProOnlineServer.Game>();

		serverThread = new ProOnlineServerThread();
		serverThread.setName("ProOnline Server Thread");
		serverThread.setDaemon(true);
		serverThread.start();
	}

	public void exit() {
		if (serverThread != null) {
			serverThread.exit();
			serverThread = null;
		}

		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				log.debug("Closing server socket", e);
			}
			serverSocket = null;
		}
	}

	private static int convertIp(byte[] bytes) {
		int ip = 0;

		for (int i = 0; i < bytes.length; i++) {
			ip |= bytes[i] << (i * 8);
		}

		return ip;
	}

	private void sendToUser(User user, SceNetAdhocctlPacketBaseS2C packet) throws IOException {
		OutputStream os = user.socket.getOutputStream();
		os.write(packet.getBytes());
		os.flush();
	}

	private void loginUserStream(Socket socket) throws IOException {
		String ip = socket.getInetAddress().getHostAddress();

		// Check for duplicated user
//		for (User user : users) {
//			if (user.ipString.equals(ip)) {
//				// Duplicate user (same IP & same port)
//				log.debug(String.format("Duplicate user IP: %s", ip));
//				socket.close();
//				return;
//			}
//		}

		User user = new User();
		user.ip = convertIp(socket.getInetAddress().getAddress());
		user.ipString = ip;
		user.socket = socket;
		user.lastReceiveTimestamp = System.currentTimeMillis();
		users.add(user);

		log.info(String.format("New Connection from %s", user.ipString));
	}

	private void logoutUser(User user) {
		if (user.group != null) {
			disconnectUser(user);
		}

		try {
			user.socket.close();
		} catch (IOException e) {
			// Ignore exception
		}

		if (user.game != null) {
			log.info(String.format("%s stopped playing %s.", user, user.game.name));

			user.game.playerCount--;

			// Empty game
			if (user.game.playerCount <= 0) {
				games.remove(user.game);
			}

			user.game = null;
		} else {
			log.info(String.format("Dropped Connection %s.", user));
		}

		users.remove(user);
	}

	private void processUserStream(User user) {
		if (user.bufferLength <= 0) {
			return;
		}

		int consumed = 0;
		SceNetAdhocctlPacketBaseC2S packet = packetFactory.createPacketC2S(null, this, user.buffer, user.bufferLength);
		if (packet == null) {
			// Skip the unknown code
			consumed = 1;
		} else if (user.bufferLength >= packet.getLength()) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Incoming client packet %s", packet));
			}

			currentUser = user;
			packet.process();
			currentUser = null;

			consumed = packet.getLength();
		}

		if (consumed >= user.bufferLength) {
			user.bufferLength = 0;
		} else {
			// Removed consumed bytes from the buffer
			user.bufferLength -= consumed;
			System.arraycopy(user.buffer, consumed, user.buffer, 0, user.bufferLength);
		}
	}

	public void processLogin(pspNetMacAddress mac, String nickName, String gameName) {
		if (gameName.matches("[A-Z0-9]{9}")) {
			currentUser.game = null;
			for (Game game : games) {
				if (game.name.equals(gameName)) {
					currentUser.game = game;
					break;
				}
			}

			if (currentUser.game == null) {
				currentUser.game = new Game(gameName);
				games.add(currentUser.game);
			}

			currentUser.game.playerCount++;
			currentUser.mac = mac;
			currentUser.nickName = nickName;

			log.info(String.format("%s started playing %s.", currentUser, currentUser.game.name));
		} else {
			log.info(String.format("Invalid login for game '%s'", gameName));
		}
	}

	private void disconnectUser(User user) {
		if (user.group != null) {
			Group group = user.group;
			group.players.remove(user);

			for (User groupUser : group.players) {
				SceNetAdhocctlDisconnectPacketS2C packet = new SceNetAdhocctlDisconnectPacketS2C(user.ip);
				try {
					sendToUser(groupUser, packet);
				} catch (IOException e) {
					log.debug("disconnectUser", e);
				}
			}

			log.info(String.format("%s left %s group %s.", user, user.game.name, group.name));

			user.group = null;

			// Empty group
			if (group.players.isEmpty()) {
				group.game.groups.remove(group);
			}
		} else {
			log.info(String.format("%s attempted to leave %s without joining one first.", user, user.game.name));
			logoutUser(user);
		}
	}

	public void processDisconnect() {
		disconnectUser(currentUser);
	}

	public void processScan() {
		// User is disconnected
		if (currentUser.group == null) {
			// Iterate game groups
			for (Group group : currentUser.game.groups) {
				pspNetMacAddress mac = new pspNetMacAddress();
				if (!group.players.isEmpty()) {
					// Founder of the group is the first player
					mac = group.players.get(0).mac;
				}
				try {
					sendToUser(currentUser, new PacketFactory.SceNetAdhocctlScanPacketS2C(group.name, mac));
				} catch (IOException e) {
					log.debug("processScan", e);
				}
			}
		} else {
			log.info(String.format("%s attempted to scan for %s groups without disconnecting from %s first.", currentUser, currentUser.game.name, currentUser.group.name));
			logoutUser(currentUser);
		}
	}

	private void spreadMessage(User fromUser, String message) {
		// Global notice
		if (fromUser == null) {
			// Iterate players
			for (User user : users) {
				// User has access to chat
				if (user.group != null) {
					try {
						sendToUser(user, new PacketFactory.SceNetAdhocctlChatPacketS2C(message, ""));
					} catch (IOException e) {
						log.debug("spreadMessage global notice", e);
					}
				}
			}
		} else if (fromUser.group != null) {
			// User is connected
			int messageCount = 0;
			for (User user : fromUser.group.players) {
				// Skip self
				if (user != fromUser) {
					try {
						sendToUser(user, new PacketFactory.SceNetAdhocctlChatPacketS2C(message, fromUser.nickName));
						messageCount++;
					} catch (IOException e) {
						log.debug("spreadMessage", e);
					}
				}
			}

			if (messageCount > 0) {
				log.info(String.format("%s sent '%s' to %d players in %s group %s", fromUser, message, messageCount, fromUser.game.name, fromUser.group.name));
			}
		} else {
			// User is disconnected
			log.info(String.format("%s attempted to send a text message without joining a %s group first", fromUser, fromUser.game.name));
		}
	}

	public void processChat(String message) {
		spreadMessage(currentUser, message);
	}

	public void processConnect(String groupName) {
		if (groupName.matches("[A-Za-z0-9]*")) {
			// User is disconnected
			if (currentUser.group == null) {
				for (Group group : currentUser.game.groups) {
					if (group.name.equals(groupName)) {
						currentUser.group = group;
						break;
					}
				}

				// New group
				if (currentUser.group == null) {
					currentUser.group = new Group(groupName, currentUser.game);
				}

				for (User user : currentUser.group.players) {
					SceNetAdhocctlConnectPacketS2C packet = new SceNetAdhocctlConnectPacketS2C(currentUser.nickName, currentUser.mac, currentUser.ip);
					try {
						sendToUser(user, packet);
					} catch (IOException e) {
						log.debug("processConnect", e);
					}

					packet = new SceNetAdhocctlConnectPacketS2C(user.nickName, user.mac, user.ip);
					try {
						sendToUser(currentUser, packet);
					} catch (IOException e) {
						log.debug("processConnect", e);
					}
				}

				currentUser.group.players.add(currentUser);

				try {
					sendToUser(currentUser, new PacketFactory.SceNetAdhocctlConnectBSSIDPacketS2C(currentUser.group.players.get(0).mac));
				} catch (IOException e) {
					log.debug("processConnect", e);
				}
				log.info(String.format("%s joined %s group '%s'.", currentUser, currentUser.game == null ? "" : currentUser.game.name, currentUser.group.name));
			} else {
				// Already connected to another group
				log.info(String.format("%s attempted to join %s group '%s' without disconnecting from %s first.", currentUser, currentUser.game == null ? "" : currentUser.game.name, groupName, currentUser.group.name));
				logoutUser(currentUser);
			}
		} else {
			log.info(String.format("%s attempted to join invalid %s group '%s'.", currentUser, currentUser.game == null ? "" : currentUser.game.name, groupName));
			logoutUser(currentUser);
		}
	}
}
