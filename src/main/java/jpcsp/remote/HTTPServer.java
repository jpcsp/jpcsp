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
package jpcsp.remote;

import static jpcsp.Allegrex.compiler.RuntimeContext.setLog4jMDC;
import static jpcsp.HLE.modules.sceNpAuth.STATUS_ACCOUNT_PARENTAL_CONTROL_ENABLED;
import static jpcsp.HLE.modules.sceNpAuth.addTicketDateParam;
import static jpcsp.HLE.modules.sceNpAuth.addTicketLongParam;
import static jpcsp.HLE.modules.sceNpAuth.addTicketParam;
import static jpcsp.filesystems.umdiso.UmdIsoFile.sectorLength;
import static jpcsp.hardware.Wlan.getLocalInetAddress;
import static jpcsp.util.Utilities.getDefaultPortForProtocol;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import jpcsp.Controller.keyCode;
import jpcsp.Emulator;
import jpcsp.MainGUI;
import jpcsp.State;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceNpTicket;
import jpcsp.HLE.kernel.types.SceNpTicket.TicketParam;
import jpcsp.HLE.modules.sceNp;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.format.Elf32Header;
import jpcsp.remote.HTTPConfiguration.HttpServerConfiguration;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class HTTPServer {
	private static Logger log = Logger.getLogger("http");
	private static HTTPServer instance;
	private static final HTTPServerDescriptor[] serverDescriptors = new HTTPServerDescriptor[] {
		new HTTPServerDescriptor(0, 80, false),
		new HTTPServerDescriptor(1, 443, true)
	};
	public static boolean processProxyRequestLocally = false;
	public static final String method = "method";
	public static final String path = "path";
	public static final String host = "host";
	public static final String parameters = "parameters";
	public static final String version = "version";
	public static final String data = "data";
	public static final String contentLength = "content-length";
	private static final String eol = "\r\n";
	private static final String boundary = "--boundarybetweensingleimages";
	private static final String isoDirectory = "/iso/";
	private static final String iconDirectory = "/icon/";
	private static final String rootDirectory = "root";
	private static final String widgetDirectory = "Widget";
	private static final String widgetPath = rootDirectory + "/" + widgetDirectory;
	private static final String indexFile = "index.html";
	private static final String naclDirectory = "nacl";
	private static final String widgetlistFile = "/widgetlist.xml";
	private HTTPServerThread[] serverThreads;
	private Robot captureRobot;
	private UmdIsoReader previousUmdIsoReader;
	private String previousIsoFilename;
	private HashMap<Integer, keyCode> keyMapping;
	private int runMapping = -1;
	private int pauseMapping = -1;
	private int resetMapping = -1;
	private static final int MAX_COMPRESSED_COUNT = 0x7F;
	private DisplayAction displayAction;
	private int displayActionUsageCount = 0;
	private BufferedImage currentDisplayImage;
	private boolean currentDisplayImageHasAlpha = false;
	private Proxy proxy;
	private int proxyPort;
	private int proxyAddress;
	private SceNpTicket ticket;
	private Map<String, IProcessHTTPRequest> processors;

	public static HTTPServer getInstance() {
		if (instance == null) {
			Utilities.disableSslCertificateChecks();
			instance = new HTTPServer();
		}
		return instance;
	}

	private static class HTTPServerDescriptor {
		private int index;
		private int port;
		private boolean ssl;

		public HTTPServerDescriptor(int index, int port, boolean ssl) {
			this.index = index;
			this.port = port;
			this.ssl = ssl;
		}

		public int getIndex() {
			return index;
		}

		public int getPort() {
			return port;
		}

		public boolean isSsl() {
			return ssl;
		}
	}

	private class HTTPServerThread extends Thread {
		private boolean exit;
		private ServerSocket serverSocket;
		private HTTPServerDescriptor descriptor;

		public HTTPServerThread(HTTPServerDescriptor descriptor) {
			this.descriptor = descriptor;
		}

		@Override
		public void run() {
			setLog4jMDC();
			try {
				if (descriptor.isSsl()) {
					SSLServerSocketFactory factory = getSSLServerSocketFactory();
					if (factory != null) {
						serverSocket = factory.createServerSocket(descriptor.getPort());
					}
				} else {
					serverSocket = new ServerSocket(descriptor.getPort(), 50, getLocalInetAddress());
				}
				if (serverSocket != null) {
					serverSocket.setSoTimeout(1);
				}
			} catch (IOException e) {
				log.error(String.format("Server socket at port %d not available: %s", descriptor.getPort(), e));
			} catch (KeyStoreException e) {
				log.error(String.format("SSL Server socket at port %d not available: %s", descriptor.getPort(), e));
			} catch (NoSuchAlgorithmException e) {
				log.error(String.format("SSL Server socket at port %d not available: %s", descriptor.getPort(), e));
			} catch (CertificateException e) {
				log.error(String.format("SSL Server socket at port %d not available: %s", descriptor.getPort(), e));
			} catch (UnrecoverableKeyException e) {
				log.error(String.format("SSL Server socket at port %d not available: %s", descriptor.getPort(), e));
			} catch (KeyManagementException e) {
				log.error(String.format("SSL Server socket at port %d not available: %s", descriptor.getPort(), e));
			}

			if (serverSocket == null) {
				exit();
			}

			while (!exit) {
				try {
					Socket socket = serverSocket.accept();
					socket.setSoTimeout(1);
					HTTPSocketHandlerThread handlerThread = new HTTPSocketHandlerThread(descriptor, socket);
					handlerThread.setName(String.format("HTTP Handler %d/%d", descriptor.getPort(), socket.getPort()));
					handlerThread.setDaemon(true);
					handlerThread.start();
				} catch (SocketTimeoutException e) {
					// Ignore timeout
				} catch (IOException e) {
					log.debug("Accept server socket", e);
				}

				Utilities.sleep(10);
			}

			if (serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					// Ignore exception
				}
			}

			serverThreads[descriptor.getIndex()] = null;
		}

		private SSLServerSocketFactory getSSLServerSocketFactory() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, KeyManagementException {
			String jksFileName = "jpcsp.jks";
			if (!new File(jksFileName).canRead()) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("getSSLServerSocketFactory cannot read the file '%s'", jksFileName));
				}
				return null;
			}

			char[] password = "changeit".toCharArray();
			FileInputStream keyStoreInputStream = new FileInputStream(jksFileName);

			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(keyStoreInputStream, password);

			String defaultAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(defaultAlgorithm);
			keyManagerFactory.init(keyStore, password);

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

			return sslContext.getServerSocketFactory();
		}

		public void exit() {
			exit = true;
		}
	}

	private class HTTPSocketHandlerThread extends Thread {
		private HTTPServerDescriptor descriptor;
		private Socket socket;

		public HTTPSocketHandlerThread(HTTPServerDescriptor descriptor, Socket socket) {
			this.descriptor = descriptor;
			this.socket = socket;
		}

		@Override
		public void run() {
			setLog4jMDC();
			process(descriptor, socket);
		}
	}

	private class DisplayAction implements IAction {
		@Override
		public void execute() {
			currentDisplayImage = Modules.sceDisplayModule.getCurrentDisplayAsBufferedImage(false);
			currentDisplayImageHasAlpha = false;
		}
	}

	private HTTPServer() {
		keyMapping = new HashMap<Integer, keyCode>();
		processors = new HashMap<String, IProcessHTTPRequest>();

		serverThreads = new HTTPServerThread[serverDescriptors.length];
		for (HTTPServerDescriptor descriptor : serverDescriptors) {
			if (descriptor.getIndex() == 0) {
				String addressName = "localhost";
				proxyPort = descriptor.getPort();

				InetSocketAddress socketAddress = new InetSocketAddress(addressName, proxyPort);
				proxy = new Proxy(Proxy.Type.HTTP, socketAddress);

				byte addrBytes[] = socketAddress.getAddress().getAddress();
				proxyAddress = (addrBytes[0] & 0xFF) | ((addrBytes[1] & 0xFF) << 8) | ((addrBytes[2] & 0xFF) << 16) | ((addrBytes[3] & 0xFF) << 24);
			}
			HTTPServerThread serverThread = new HTTPServerThread(descriptor);
			serverThreads[descriptor.getIndex()] = serverThread;
			serverThread.setDaemon(true);
			serverThread.setName("HTTP Server");
			serverThread.start();
		}

		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				if (log.isDebugEnabled()) {
					log.debug(String.format("getPasswordAuthentication called for scheme='%s', prompt='%s'", getRequestingScheme(), getRequestingPrompt()));
				}

				if ("digest".equals(getRequestingScheme())) {
					return new PasswordAuthentication("c7y-basic01", "A9QTbosh0W0D^{7467l-n_>2Y%JG^v>o".toCharArray());
				} else if ("c7y-basic".equals(getRequestingPrompt())) {
					// This is the PSP authentication, but it seems to no longer be accepted...
					char[] pwd = new char[] {
						(char) 0x35, (char) 0x03, (char) 0x0f, (char) 0x19, (char) 0x40, (char) 0x16, (char) 0x49, (char) 0x04,
						(char) 0x1c, (char) 0x35, (char) 0x03, (char) 0x1e, (char) 0x21, (char) 0x48, (char) 0x2d, (char) 0x4e,
						(char) 0x07, (char) 0x1c, (char) 0x5a, (char) 0x36, (char) 0x0e, (char) 0x3f, (char) 0x0c, (char) 0x18,
						(char) 0x49, (char) 0x15, (char) 0x4e, (char) 0x21, (char) 0x14, (char) 0x36, (char) 0x1d, (char) 0x16
					};
					return new PasswordAuthentication("c7y-basic02", pwd);
				} else if ("c7y-ranking".equals(getRequestingPrompt())) {
					// This is the PSP authentication, but it seems to no longer be accepted...
					char[] pwd = new char[] {
						(char) 0x21, (char) 0x2D, (char) 0x18, (char) 0x1B, (char) 0x1D, (char) 0x0E, (char) 0x2A, (char) 0x23,
						(char) 0x04, (char) 0x4C, (char) 0x4B, (char) 0x19, (char) 0x4F, (char) 0x25, (char) 0x26, (char) 0x3F,
						(char) 0x4B, (char) 0x4D, (char) 0x4C, (char) 0x44, (char) 0x58, (char) 0x3C, (char) 0x31, (char) 0x4C,
						(char) 0x15, (char) 0x4C, (char) 0x5C, (char) 0x41, (char) 0x32, (char) 0x38, (char) 0x1E, (char) 0x08
					};
					return new PasswordAuthentication("c7y-ranking01", pwd);
				}

				return super.getPasswordAuthentication();
			}
		});

		try {
			captureRobot = new Robot();
			captureRobot.setAutoDelay(0);
		} catch (AWTException e) {
			log.error("Create captureRobot", e);
		}
	}

	public void register(String path, IProcessHTTPRequest processor) {
		processors.put(path, processor);
	}

	private static String decodePath(String path) {
		StringBuilder decoded = new StringBuilder();

		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (c == '+') {
				decoded.append(' ');
			} else if (c == '%') {
				int hex = Integer.parseInt(path.substring(i + 1, i + 3), 16);
				i += 2;
				decoded.append((char) hex);
			} else {
				decoded.append(c);
			}
		}

		return decoded.toString();
	}

	private void process(HTTPServerDescriptor descriptor, Socket socket) {
		InputStream is = null;
		try {
			is = socket.getInputStream();
		} catch (IOException e) {
			log.error("process InputStream", e);
		}
		OutputStream os = null;
		try {
			os = socket.getOutputStream();
		} catch (IOException e) {
			log.error("process OutputStream", e);
		}

		byte[] buffer = new byte[10000];
		int bufferLength = 0;
		while (is != null && os != null) {
			try {
				int length = is.read(buffer, bufferLength, buffer.length - bufferLength);
				if (length < 0) {
					break;
				}
				if (length > 0) {
					bufferLength += length;
					String request = new String(buffer, 0, bufferLength);
					HashMap<String, String> requestHeaders = parseRequest(request);
					if (requestHeaders != null) {
						if (log.isDebugEnabled()) {
							log.debug(String.format("Received request: '%s', headers: %s", request, requestHeaders));
						}
						boolean keepAlive = process(descriptor, requestHeaders, os);
						os.flush();

						if (keepAlive) {
							bufferLength = 0;
						} else {
							break;
						}
					}
				}
			} catch (SocketTimeoutException e) {
				// Ignore timeout
			} catch (IOException e) {
				// Do not log the exception when the remote client has closed the connection
				if (!(e.getCause() instanceof EOFException)) {
					if (log.isDebugEnabled()) {
						log.debug("Receive socket", e);
					}
				}
				break;
			}
		}

		try {
			socket.close();
		} catch (IOException e) {
			// Ignore exception
		}
	}

	private HashMap<String, String> parseRequest(String request) {
		HashMap<String, String> headers = new HashMap<String, String>();
		String[] lines = request.split(eol, -1); // Do not loose trailing empty strings
		boolean header = true;

		if (log.isTraceEnabled()) {
			log.trace(String.format("parseRequest '%s'", request));
		}

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (i == 0) {
				// Parse e.g. "GET / HTTP/1.1" into 3 words: "GET", "/" and "HTTP/1.1"
				String[] words = line.split(" ");
				if (words.length >= 1) {
					headers.put(method, words[0]);
				}
				if (words.length >= 2) {
					String completePath = words[1];
					int parametersIndex = completePath.indexOf("?");
					if (parametersIndex >= 0) {
						headers.put(path, decodePath(completePath.substring(0, parametersIndex)));
						headers.put(parameters, completePath.substring(parametersIndex + 1));
					} else {
						headers.put(path, decodePath(completePath));
					}
				}
				if (words.length >= 3) {
					headers.put(version, words[2]);
				}
			} else if (header) {
				if (line.length() == 0) {
					// End of header
					header = false;
				} else {
					// Parse e.g. "Host: localhost:30005" into 2 words: "Host" and "localhost:30005"
					String[] words = line.split(": *", 2);
					if (words.length >= 2) {
						headers.put(words[0].toLowerCase(), words[1]);
					}
				}
			} else {
				String previousData = headers.get(data);
				if (previousData != null) {
					headers.put(data, previousData + eol + line);
				} else {
					headers.put(data, line);
				}
			}
		}

		if (header) {
			return null;
		}

		if (headers.get(contentLength) != null) {
			int headerContentLength = Integer.parseInt(headers.get(contentLength));
			if (headerContentLength > 0) {
				String additionalData = headers.get(data);
				if (additionalData == null || additionalData.length() < headerContentLength) {
					if (log.isTraceEnabled()) {
						log.trace(String.format("parseRequest content-length=%d, data length=%d", headerContentLength, additionalData.length()));
					}
					return null;
				}
			}
		}

		return headers;
	}

	private boolean doProxy(HttpServerConfiguration httpServerConfiguration, HTTPServerDescriptor descriptor, HashMap<String, String> request, OutputStream os, String pathValue) throws IOException {
		int forcedPort = 0;
		if (httpServerConfiguration.serverPort != descriptor.port) {
			forcedPort = httpServerConfiguration.serverPort;
		}

		boolean keepAlive = doProxy(descriptor, request, os, pathValue, forcedPort);
		if (!httpServerConfiguration.doKeepAlive) {
			keepAlive = false;
		}

		return keepAlive;
	}

	private boolean doProxy(HTTPServerDescriptor descriptor, HashMap<String, String> request, OutputStream os, String pathValue, int forcedPort) throws IOException {
		boolean keepAlive = false;

		String remoteUrl = getUrl(descriptor, request, pathValue, forcedPort);

		if (log.isDebugEnabled()) {
			log.debug(String.format("doProxy connecting to '%s'", remoteUrl));
		}

		HttpURLConnection connection = (HttpURLConnection) new URL(remoteUrl).openConnection();
		for (String key : request.keySet()) {
			if (!data.equals(key) && !method.equals(key) && !version.equals(key) && !path.equals(key) && !parameters.equals(key)) {
				connection.setRequestProperty(key, request.get(key));
			}
		}

		// Do not follow HTTP redirects
		connection.setInstanceFollowRedirects(false);

		connection.setRequestMethod(request.get(method));
		String additionalData = request.get(data);
		if (additionalData != null) {
			if ("/nav/auth".equals(pathValue) && additionalData.contains("&consoleid=")) {
				// Remove the "consoleid" parameter as it is recognized as invalid.
				// The dummy value returned by sceOpenPSIDGetPSID is not valid.
				additionalData = additionalData.replaceAll("\\&consoleid=[0-9a-fA-F]*", "");
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("doProxy additional data: '%s'", additionalData));
			}

			connection.setDoOutput(true);
			OutputStream dataStream = connection.getOutputStream();
			dataStream.write(additionalData.getBytes());
			dataStream.close();
		}
		connection.connect();

		int dataLength = connection.getContentLength();

		byte[] buffer = new byte[100000];
		int length = 0;
		boolean endOfInputReached = false;
		InputStream in = null;
		try {
			in = connection.getInputStream();
			while (length < buffer.length) {
				int l = in.read(buffer, length, buffer.length - length);
				if (l < 0) {
					endOfInputReached = true;
					break;
				}
				length += l;
			}
		} catch (IOException e) {
			log.debug("doProxy", e);
		}

		String bufferString = new String(buffer, 0, length);
		boolean bufferPatched = false;
		if (bufferString.contains("https://legaldoc.dl.playstation.net")) {
			bufferString = bufferString.replace("https://legaldoc.dl.playstation.net", "http://legaldoc.dl.playstation.net");
			bufferPatched = true;
		}

		if (bufferPatched) {
			buffer = bufferString.getBytes();
			length = buffer.length;

			// Also update the "Content-Length" header if it was specified
			if (dataLength >= 0) {
				dataLength = length;
			}
		}

		sendHTTPResponseCode(os, connection.getResponseCode(), connection.getResponseMessage());

		// Only send a "Content-Length" header if the remote server did send it
		if (dataLength >= 0) {
			sendResponseHeader(os, contentLength, dataLength);
		}

		for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
			String key = entry.getKey();
			if (key != null && !"transfer-encoding".equals(key.toLowerCase())) {
				for (String value : entry.getValue()) {
					// Ignore "Set-Cookie" with an empty value
					if ("Set-Cookie".equalsIgnoreCase(key) && value.length() == 0) {
						continue;
					}

					// If we changed "https" into "http", remove the information that the cookie can
					// only be sent over https, otherwise, it will be lost.
					if (forcedPort == 443 && "Set-Cookie".equalsIgnoreCase(key)) {
						value = value.replace("; Secure", "");
					}

					// If we changed "https" into "http", keep redirecting to the
					// http address instead of https.
					if (forcedPort == 443 && "Location".equalsIgnoreCase(key)) {
						if (value.startsWith("https:")) {
							value = value.replaceFirst("https:", "http:");
						}
					}

					sendResponseHeader(os, key, value);

					if ("connection".equalsIgnoreCase(key) && "keep-alive".equalsIgnoreCase(value)) {
						keepAlive = true;
					}
					if ("content-type".equalsIgnoreCase(key) && "application/x-i-5-ticket".equalsIgnoreCase(value) && length > 0) {
						ticket = new SceNpTicket();
						ticket.read(buffer, 0, length);
					}
				}
			}
		}
		sendEndOfHeaders(os);

		if (log.isDebugEnabled()) {
			log.debug(String.format("doProxy%s:\n%s", (bufferPatched ? " (response patched)" : ""), Utilities.getMemoryDump(buffer, 0, length)));
		}

		os.write(buffer, 0, length);

		if (in != null) {
			while (!endOfInputReached) {
				length = 0;
				try {
					while (length < buffer.length) {
						int l = in.read(buffer, length, buffer.length - length);
						if (l < 0) {
							endOfInputReached = true;
							break;
						}
						length += l;
					}
				} catch (IOException e) {
					log.debug("doProxy", e);
				}
				os.write(buffer, 0, length);
			}
			in.close();
		}

		return keepAlive;
	}

	private HttpServerConfiguration getHttpServerConfiguration(String serverName, String pathValue) {
		if (serverName != null) {
			for (HttpServerConfiguration httpServerConfiguration : HTTPConfiguration.doProxyServers) {
				if (httpServerConfiguration.serverName.equals(serverName)) {
					boolean found = true;
					if (httpServerConfiguration.fakedPaths != null) {
						for (String fakedPath : httpServerConfiguration.fakedPaths) {
							if (fakedPath.equals(pathValue)) {
								found = false;
								break;
							}
						}
					}

					if (found) {
						return httpServerConfiguration;
					}
				}
			}
		}

		return null;
	}

	private boolean process(OutputStream os, String path, HashMap<String, String> request) throws IOException {
		for (String key : processors.keySet()) {
			if (path.startsWith(key)) {
				IProcessHTTPRequest processor = processors.get(key);
				if (processor.processRequest(this, os, path, request)) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean process(HTTPServerDescriptor descriptor, HashMap<String, String> request, OutputStream os) throws IOException {
		boolean keepAlive = false;
		try {
			String pathValue = request.get(path);
			String baseUrl = getBaseUrl(descriptor, request, 0);
			if (pathValue.startsWith(baseUrl)) {
				pathValue = pathValue.substring(baseUrl.length() - 1);
			}
			String methodValue = request.get(method);

			HttpServerConfiguration httpServerConfiguration = getHttpServerConfiguration(request.get(host), pathValue);

			if (httpServerConfiguration != null) {
				keepAlive = doProxy(httpServerConfiguration, descriptor, request, os, pathValue);
//			} else if ("auth.np.ac.playstation.net".equals(request.get(host)) && "/nav/auth".equals(pathValue)) {
//				sendNpNavAuth(request.get(data), os);
//			} else if ("getprof.gb.np.community.playstation.net".equals(request.get(host)) && "/basic_view/sec/get_self_profile".equals(pathValue)) {
//				sendNpGetSelfProfile(request.get(data), os);
			} else if ("commerce.np.ac.playstation.net".equals(request.get(host)) && "/cap.m".equals(pathValue)) {
				sendCapM(request.get(data), os);
			} else if ("commerce.np.ac.playstation.net".equals(request.get(host)) && "/kdp.m".equals(pathValue)) {
				sendKdpM(request.get(data), os);
			} else if ("video.dl.playstation.net".equals(request.get(host)) && pathValue.matches("/cdn/video/[A-Z][A-Z]/g")) {
				sendVideoStore(os);
			} else if ("GET".equals(methodValue)) {
				if (process(os, pathValue, request)) {
					// Already processed
				} else if ("/".equals(pathValue)) {
					sendResponseFile(os, rootDirectory + "/" + indexFile);
				} else if ("/screen.png".equals(pathValue)) {
					sendScreenImage(os, "png");
				} else if ("/screen.jpg".equals(pathValue)) {
					sendScreenImage(os, "jpg");
				} else if ("/screen.mjpg".equals(pathValue)) {
					sendVideoMJPG(os);
				} else if ("/screen.raw".equals(pathValue)) {
					sendVideoRAW(os);
				} else if ("/screen.craw".equals(pathValue)) {
					sendVideoCompressedRAW(os);
				} else if ("/audio.wav".equals(pathValue)) {
					sendAudioWAV(os);
				} else if ("/audio.raw".equals(pathValue)) {
					sendAudioRAW(os);
				} else if ("/controls".equals(pathValue)) {
					processControls(os, request.get(parameters));
				} else if (pathValue.startsWith(iconDirectory)) {
					sendIcon(os, pathValue);
				} else if (pathValue.startsWith(isoDirectory)) {
					sendIso(request, os, pathValue, true);
				} else if (widgetlistFile.equals(pathValue)) {
					sendWidgetlist(descriptor, request, os, pathValue);
				} else if (pathValue.startsWith("/" + widgetDirectory + "/")) {
					sendWidget(os, request.get(parameters), rootDirectory + pathValue);
				} else if (pathValue.startsWith("/" + naclDirectory + "/")) {
					sendNaClResponse(os, pathValue.substring(6));
				} else if (pathValue.endsWith(".html")) {
					sendResponseFile(os, rootDirectory + pathValue);
				} else if (pathValue.endsWith(".txt")) {
					sendResponseFile(os, rootDirectory + pathValue);
				} else if (pathValue.endsWith(".xml")) {
					sendResponseFile(os, rootDirectory + pathValue);
				} else {
					sendErrorNotFound(os);
				}
			} else if ("HEAD".equals(methodValue)) {
				if (pathValue.startsWith(isoDirectory)) {
					sendIso(request, os, pathValue, false);
				} else {
					sendErrorNotFound(os);
				}
			} else if ("POST".equals(methodValue)) {
				if (process(os, pathValue, request)) {
					// Already processed
				} else {
					sendErrorMethodNotAllowed(os);
				}
			} else {
				sendErrorMethodNotAllowed(os);
			}
		} catch (SocketException e) {
			// Ignore exception (e.g. Connection reset by peer)
			keepAlive = false;
		}

		return keepAlive;
	}

	private static String guessMimeType(String fileName) {
		if (fileName != null) {
			if (fileName.endsWith(".js")) {
				return "application/javascript";
			} else if (fileName.endsWith(".html")) {
				return "text/html";
			} else if (fileName.endsWith(".css")) {
				return "text/css";
			} else if (fileName.endsWith(".png")) {
				return "image/png";
			} else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
				return "image/jpeg";
			} else if (fileName.endsWith(".xml")) {
				return "text/xml";
			} else if (fileName.endsWith(".zip")) {
				return "application/zip";
			}
		}

		return "application/octet-stream";
	}

	private void sendRedirect(OutputStream os, String redirect) throws IOException {
		sendHTTPResponseCode(os, 302);
		sendResponseHeader(os, "Location", redirect);
		sendEndOfHeaders(os);
	}

	private void sendResponseFile(OutputStream os, String fileName) throws IOException {
		sendResponseFile(os, getClass().getResourceAsStream(fileName), guessMimeType(fileName));
	}

	public void sendResponse(OutputStream os, String response) throws IOException {
		byte[] buffer = response.getBytes();
		sendResponse(os, buffer, buffer.length, null);
	}

	private void sendResponse(OutputStream os, byte[] buffer, int bufferLength, String contentType) throws IOException {
		sendOK(os);
		if (contentType != null) {
			sendResponseHeader(os, "Content-Type", contentType);
		}
		if (bufferLength > 0) {
			sendResponseHeader(os, "Content-Length", bufferLength);
		}
		sendEndOfHeaders(os);

		if (bufferLength > 0) {
			os.write(buffer, 0, bufferLength);
		}
	}

	private void sendResponseFile(OutputStream os, InputStream is, String contentType) throws IOException {
		byte[] buffer = new byte[1000];
		int contentLength = 0;
		if (is != null) {
			while (true) {
				if (buffer.length - contentLength < 1000) {
					buffer = Utilities.extendArray(buffer, 1000);
				}
				int length = is.read(buffer, contentLength, buffer.length - contentLength);
				if (length < 0) {
					break;
				}
				contentLength += length;
			}
			is.close();
		}

		sendResponse(os, buffer, contentLength, contentType);
	}

	private void sendResponseLine(OutputStream os, String line) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Response: %s", line));
		}
		os.write(line.getBytes());
		os.write(eol.getBytes());
	}

	private static String guessHTTPResponseCodeMsg(int code) {
		switch (code) {
			case 200: return "OK";
			case 206: return "Partial Content";
			case 302: return "Found";
			case 404: return "Not Found";
			case 405: return "Method Not Allowed";
		}

		return "";
	}

	private void sendHTTPResponseCode(OutputStream os, int code, String msg) throws IOException {
		sendResponseLine(os, String.format("HTTP/1.1 %d %s", code, msg));
	}

	private void sendHTTPResponseCode(OutputStream os, int code) throws IOException {
		sendHTTPResponseCode(os, code, guessHTTPResponseCodeMsg(code));
	}

	private void sendOK(OutputStream os) throws IOException {
		sendHTTPResponseCode(os, 200);
	}

	private void sendResponseHeader(OutputStream os, String name, String value) throws IOException {
		sendResponseLine(os, String.format("%s: %s", name, value));
	}

	private void sendResponseHeader(OutputStream os, String name, int value) throws IOException {
		sendResponseHeader(os, name, String.valueOf(value));
	}

	private void sendResponseHeader(OutputStream os, String name, long value) throws IOException {
		sendResponseHeader(os, name, String.valueOf(value));
	}

	private void sendNoCache(OutputStream os) throws IOException {
        sendResponseHeader(os, "Cache-Control", "no-cache");
        sendResponseHeader(os, "Cache-Control", "private");
	}

	private void sendEndOfHeaders(OutputStream os) throws IOException {
		sendResponseLine(os, "");
	}

	private void sendError(OutputStream os, int code) throws IOException {
		sendHTTPResponseCode(os, code);
		sendEndOfHeaders(os);
	}

	private void sendErrorNotFound(OutputStream os) throws IOException {
		sendError(os, 404);
	}

	private void sendErrorMethodNotAllowed(OutputStream os) throws IOException {
		sendError(os, 405);
	}

	private void sendScreenImage(OutputStream os, String fileFormat) throws IOException {
        String fileName = String.format("%s%cscreen.%s", Settings.getInstance().readString("emu.tmppath"), File.separatorChar, fileFormat);
		File file = new File(fileName);
		file.deleteOnExit();
		Rectangle rect = Emulator.getMainGUI().getCaptureRectangle();
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Capturing screen from %s to %s", rect, fileName));
    	}

        BufferedImage img = captureRobot.createScreenCapture(rect);
    	try {
    		file.delete();
            ImageIO.write(img, fileFormat, file);
            img.flush();
        } catch (IOException e) {
            log.error("Error saving screenshot", e);
        }

    	if (file.canRead()) {
	        int length = (int) file.length();
	        sendOK(os);
	        sendNoCache(os);
	        sendResponseHeader(os, "Content-Type", String.format("image/%s", fileFormat));
	        sendResponseHeader(os, "Content-Length", length);
	        sendEndOfHeaders(os);
	        byte[] buffer = new byte[length];
	        InputStream is = new FileInputStream(file);
	        length = is.read(buffer);
	        is.close();
	        file.delete();
	        os.write(buffer, 0, length);
    	} else {
			sendErrorNotFound(os);
    	}
	}

	private void sendVideoMJPG(OutputStream os) throws IOException {
		String fileFormat = "jpg";
        String fileName = String.format("%s%cscreen.%s", Settings.getInstance().readString("emu.tmppath"), File.separatorChar, fileFormat);
		File file = new File(fileName);
		file.deleteOnExit();
		Rectangle rect = Emulator.getMainGUI().getCaptureRectangle();
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Capturing screen from %s to %s", rect, fileName));
    	}

    	startDisplayAction();

    	try {
	    	sendOK(os);
	    	sendNoCache(os);
	        sendResponseHeader(os, "Content-Type", String.format("multipart/x-mixed-replace; boundary=%s", boundary));
	        sendEndOfHeaders(os);

	        while (true) {
		        BufferedImage img = getScreenImage(rect);
		    	try {
		    		file.delete();
		            ImageIO.write(img, fileFormat, file);
		            img.flush();
		        } catch (IOException e) {
		            log.error("Error saving screenshot", e);
		        }

		    	if (file.canRead()) {
			        int length = (int) file.length();
			        if (log.isDebugEnabled()) {
			        	log.debug(String.format("Sending video image length=%d", length));
			        }
			        byte[] buffer = new byte[length];
			        InputStream is = new FileInputStream(file);
			        length = is.read(buffer);
			        is.close();

			        sendResponseLine(os, boundary);
			        sendResponseHeader(os, "Content-Type", "image/jpeg");
			        sendResponseHeader(os, "Content-Length", length);
			        sendEndOfHeaders(os);
			        os.write(buffer, 0, length);
			        sendEndOfHeaders(os);
			        os.flush();
		    	} else {
		    		if (log.isDebugEnabled()) {
		    			log.debug(String.format("Cannot read capture file %s", file));
		    		}
		    		break;
		    	}
	    	}
    	} finally {
    		stopDisplayAction();
    		file.delete();
    	}
	}

	private void sendAudioWAV(OutputStream os) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("sendAudioWAV"));
		}
		sendOK(os);
        sendResponseHeader(os, "Content-Type", "audio/wav");
        sendEndOfHeaders(os);

        int channels = 2;
        int sampleRate = 44100;
        byte[] silenceBuffer = new byte[1024 * channels * 2];

        byte[] header = new byte[100];
        int n = 0;
        // "RIFF"
        header[n++] = 'R';
        header[n++] = 'I';
        header[n++] = 'F';
        header[n++] = 'F';
        // Total file size
        header[n++] = 0;
        header[n++] = 0;
        header[n++] = 0;
        header[n++] = 0x7F;
        // "WAVE"
        header[n++] = 'W';
        header[n++] = 'A';
        header[n++] = 'V';
        header[n++] = 'E';
        // "fmt " tag
        header[n++] = 'f';
        header[n++] = 'm';
        header[n++] = 't';
        header[n++] = ' ';
        // length of "fmt " tag
        header[n++] = 16;
        header[n++] = 0;
        header[n++] = 0;
        header[n++] = 0;
        // format tag (1 == PCM)
        header[n++] = 1;
        header[n++] = 0;
        // channels
        header[n++] = (byte) channels;
        header[n++] = 0;
        // sample rate
        header[n++] = (byte) ((sampleRate     ) & 0xFF);
        header[n++] = (byte) ((sampleRate >> 8) & 0xFF);
        header[n++] = 0;
        header[n++] = 0;
        // bytes per second
        int bytesPerSecond = 2 * channels * sampleRate;
        header[n++] = (byte) ((bytesPerSecond      ) & 0xFF);
        header[n++] = (byte) ((bytesPerSecond >>  8) & 0xFF);
        header[n++] = (byte) ((bytesPerSecond >> 16) & 0xFF);
        header[n++] = (byte) ((bytesPerSecond >> 24) & 0xFF);
        // block align
        header[n++] = (byte) (2 * channels);
        header[n++] = 0;
        // bits per sample
        header[n++] = 16;
        header[n++] = 0;
        os.write(header, 0, n);

        byte[] dataHeader = new byte[8];
        dataHeader[0] = 'd';
        dataHeader[1] = 'a';
        dataHeader[2] = 't';
        dataHeader[3] = 'a';

        long start = System.currentTimeMillis();
        while (true) {
        	long now = System.currentTimeMillis();
        	while (now < start) {
        		Utilities.sleep(1, 0);
        		now = System.currentTimeMillis();
        	}
        	byte[] buffer = Modules.sceAudioModule.audioData;
        	if (buffer == null) {
        		buffer = silenceBuffer;
        	}
        	int length = buffer.length;
        	dataHeader[4] = (byte) ((length      ) & 0xFF);
        	dataHeader[5] = (byte) ((length >>  8) & 0xFF);
        	dataHeader[6] = (byte) ((length >> 16) & 0xFF);
        	dataHeader[7] = (byte) ((length >> 24) & 0xFF);
        	os.write(dataHeader);
        	os.write(buffer, 0, length);
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sendAudioWAV sent %d bytes", length));
        	}
        	start += 1000 * length / (2 * channels * sampleRate);
        }
	}

	private void sendAudioRAW(OutputStream os) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("sendAudioRAW"));
		}
		sendOK(os);
        sendResponseHeader(os, "Content-Type", "audio/raw");
        sendEndOfHeaders(os);

        int channels = 2;
        int sampleRate = 44100;
        byte[] silenceBuffer = new byte[1024 * channels * 2];

        long start = System.currentTimeMillis();
        while (true) {
        	long now = System.currentTimeMillis();
        	while (now < start) {
        		Utilities.sleep(1, 0);
        		now = System.currentTimeMillis();
        	}
        	byte[] buffer = Modules.sceAudioModule.audioData;
        	if (buffer == null) {
        		buffer = silenceBuffer;
        	}
        	int length = buffer.length;
        	os.write(buffer, 0, length);
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sendAudioRAW sent %d bytes", length));
        	}
        	start += 1000 * length / (2 * channels * sampleRate);
        }
	}

	private BufferedImage getScreenImage(Rectangle rect) {
		if (currentDisplayImage != null) {
			return currentDisplayImage;
		}

		currentDisplayImageHasAlpha = true;
        return captureRobot.createScreenCapture(rect);
	}

	private void sendVideoRAW(OutputStream os) throws IOException {
		Rectangle rect = Emulator.getMainGUI().getCaptureRectangle();
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Capturing RAW screen from %s", rect));
    	}

    	startDisplayAction();

    	try {
	    	sendOK(os);
	    	sendNoCache(os);
	        sendResponseHeader(os, "Content-Type", "video/raw");
	        sendEndOfHeaders(os);

	        byte[] pixels = new byte[rect.width * rect.height * 3];
	        while (true) {
	            BufferedImage img = getScreenImage(rect);

		        int i = 0;
		        for (int y = 0; y < img.getHeight(); y++) {
		        	for (int x = 0; x < img.getWidth(); x++, i+= 3) {
		        		int color = img.getRGB(x, y);
		        		pixels[i + 0] = (byte) ((color >> 16) & 0xFF);
		        		pixels[i + 1] = (byte) ((color >>  8) & 0xFF);
		        		pixels[i + 2] = (byte) ((color >>  0) & 0xFF);
		        	}
		        }
		        os.write(pixels);
	        	if (log.isDebugEnabled()) {
	        		log.debug(String.format("sendVideoRAW sent %dx%d image (%d bytes)", rect.width, rect.height, pixels.length));
	        	}
		        os.flush();
	    	}
    	} finally {
    		stopDisplayAction();
    	}
	}

	private int storeCompressedPixel(int color, byte[] buffer, int compressedLength, boolean rle, int count) {
		if (!rle) {
			count |= 0x80;
		}

		buffer[compressedLength++] = (byte) count;
		buffer[compressedLength++] = (byte) ((color >> 16) & 0xFF);
		buffer[compressedLength++] = (byte) ((color >>  8) & 0xFF);
		buffer[compressedLength++] = (byte) ((color >>  0) & 0xFF);

		return compressedLength;
	}

	private int compressImage(int width, int height, int[] image, int[] previousImage, byte[] buffer, int compressedLength) {
		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; ) {
				int color = image[i];
				int previousColor = previousImage[i];
				i++;
				x++;

				// RLE?
				if (x < width && color == image[i]) {
					if (color == previousColor) {
						// Both methods apply: RLE and matching previous video.
						// Choose the one matching the most pixels.
						boolean rleFailed = false;
						boolean previousFailed = false;
						int count;
						for (count = 0; x < width && count < MAX_COMPRESSED_COUNT; count++) {
							boolean rleMatch = !rleFailed && image[i] == color;
							boolean previousMatch = !previousFailed && image[i] == previousImage[i];

							if (rleMatch) {
								if (previousMatch) {
									// OK, both still matching
								} else {
									// Continue RLE, previous image no longer matching
									previousFailed = true;
								}
							} else {
								if (previousMatch) {
									// Continue testing previous image, RLE no longer matching
									rleFailed = true;
								} else {
									// Both tests failed, abort
									break;
								}
							}
							i++;
							x++;
						}

						// If none failed, prefer RLE encoding (because faster decoding)
						if (!rleFailed) {
							compressedLength = storeCompressedPixel(color, buffer, compressedLength, true, count);
						} else {
							// Encode to match the previous image
							if (x < width) {
								color = image[i++];
								x++;
							} else if (count > 0) {
								// Past screen width, take previous pixel
								color = image[i - 1];
								count--;
							}
							compressedLength = storeCompressedPixel(color, buffer, compressedLength, false, count);
						}
					} else {
						// Only RLE, not matching previous image
						i++;
						x++;
						int count;
						for (count = 1; x < width; count++) {
							if (color != image[i] || count >= MAX_COMPRESSED_COUNT) {
								break;
							}
							i++;
							x++;
						}
						compressedLength = storeCompressedPixel(color, buffer, compressedLength, true, count);
					}
				} else if (x < width && color == previousColor) {
					// No RLE, only matching previous image
					int count;
					for (count = 0; x < width; count++) {
						color = image[i];
						previousColor = previousImage[i];
						i++;
						x++;
						if (color != previousColor || count >= MAX_COMPRESSED_COUNT || x >= width) {
							break;
						}
					}
					compressedLength = storeCompressedPixel(color, buffer, compressedLength, false, count);
				} else {
					// No RLE, not matching previous image
					compressedLength = storeCompressedPixel(color, buffer, compressedLength, true, 0);
				}
			}
		}

		return compressedLength;
	}

	private static void write32(byte[] buffer, int offset, int value) {
        buffer[offset + 0] = (byte) ((value >>  0) & 0xFF);
        buffer[offset + 1] = (byte) ((value >>  8) & 0xFF);
        buffer[offset + 2] = (byte) ((value >> 16) & 0xFF);
        buffer[offset + 3] = (byte) ((value >> 24) & 0xFF);
	}

	private void sendVideoCompressedRAW(OutputStream os) throws IOException {
		Rectangle rect = Emulator.getMainGUI().getCaptureRectangle();
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Capturing compressed RAW screen from %s", rect));
    	}

    	startDisplayAction();

    	try {
	    	sendOK(os);
	    	sendNoCache(os);
	        sendResponseHeader(os, "Content-Type", "video/compressed-raw");
	        sendEndOfHeaders(os);

	        int[] image = new int[0];
	        int[] previousImage = null;
	        byte[] buffer = null;

	        while (true) {
	            BufferedImage img = getScreenImage(rect);

	            if (img != null) {
			        int width = img.getWidth();
			        int height = img.getHeight();
			        int imageSize = width * height;

			        // Is the image now larger?
			        if (image.length < imageSize) {
			        	// Resize the buffers
			        	image = new int[imageSize];
			        	previousImage = new int[imageSize];
			        	buffer = new byte[imageSize * 4 + 12];
			        }

			        img.getRGB(0, 0, width, height, image, 0, width);

	            	if (currentDisplayImageHasAlpha) {
	            		for (int i = 0; i < imageSize; i++) {
	            			image[i] &= 0x00FFFFFF;
	            		}
	            	}

			        // The first 12 bytes of the buffer will contain
	            	// - the length of the compressed image (including the 12 bytes header)
	            	// - the image width in pixels
	            	// - the image height in pixels
			        int compressedLength = compressImage(width, height, image, previousImage, buffer, 12);
			        // Store the length of the compressed image and its size
			        write32(buffer, 0, compressedLength);
	            	write32(buffer, 4, width);
	            	write32(buffer, 8, height);

			        os.write(buffer, 0, compressedLength);
		        	if (log.isDebugEnabled()) {
		        		log.debug(String.format("sendVideoCompressedRAW sent %dx%d image (%d bytes, compression rate %.1f%%)", width, height, compressedLength, 100f * compressedLength / (image.length * 3)));
		        	}
			        os.flush();

			        // Swap previous and current image buffers
			        int[] swapImage = image;
			        image = previousImage;
			        previousImage = swapImage;
	            } else {
	            	Utilities.sleep(10, 0);
	            }
	        }
    	} finally {
    		stopDisplayAction();
    	}
	}

	private void sendIso(HashMap<String, String> request, OutputStream os, String pathValue, boolean sendContent) throws IOException {
		String isoFileName = pathValue.substring(isoDirectory.length());
		if (log.isDebugEnabled()) {
			log.debug(String.format("sendIso '%s'", isoFileName));
		}

		boolean contentSent = false;
		try {
			UmdIsoReader iso = getIso(isoFileName);
			if (iso != null) {
				if (sendContent) {
					String range = request.get("range");
					if (range != null) {
						if (range.startsWith("bytes=")) {
							String rangeValues = range.substring(6);
							String[] ranges = rangeValues.split("-");
							if (ranges != null && ranges.length == 2) {
								long from = Long.parseLong(ranges[0]);
								long to = Long.parseLong(ranges[1]);
								if (log.isDebugEnabled()) {
									log.debug(String.format("sendIso bytes from=0x%X, to=0x%X, length=0x%X", from, to, to - from + 1));
								}

								sendHTTPResponseCode(os, 206);
								sendResponseHeader(os, "Content-Range", String.format("bytes %d-%d", from, to));
								sendEndOfHeaders(os);
								sendIsoContent(os, iso, from, to);
								contentSent = true;
							} else {
								log.warn(String.format("sendIso: unsupported range format '%s'", range));
							}
						} else {
							log.warn(String.format("sendIso: unsupported range format '%s'", range));
						}
					}
				} else {
					sendOK(os);

					long isoLength = iso.getNumSectors() * (long) sectorLength;
					if (log.isDebugEnabled()) {
						log.debug(String.format("sendIso returning content-length=0x%X", isoLength));
					}
					sendResponseHeader(os, "Content-Length", isoLength);
					sendResponseHeader(os, "Accept-Ranges", "bytes");
					sendEndOfHeaders(os);
					contentSent = true;
				}
			}
		} catch (IOException e) {
			contentSent = false;
		}

		if (!contentSent) {
			sendErrorNotFound(os);
		}
	}

	private UmdIsoReader getIso(String isoFileName) throws FileNotFoundException, IOException {
		UmdIsoReader iso = null;
		if (isoFileName.equals(previousIsoFilename)) {
			iso = previousUmdIsoReader;
			if (log.isDebugEnabled()) {
				log.debug(String.format("Reusing previous UmdIsoReader for '%s'", isoFileName));
			}
		} else {
			if ("umdbuffer.iso".equals(isoFileName)) {
				iso = new UmdIsoReader((String) null, true);
			} else {
				File[] umdPaths = MainGUI.getUmdPaths(false);
				for (int i = 0; i < umdPaths.length; i++) {
					File isoPath = new File(String.format("%s%s%s", umdPaths[i], File.separator, isoFileName));
					if (isoPath.exists()) {
						iso = new UmdIsoReader(isoPath.getPath(), false);
						break;
					}
				}
			}

			if (previousUmdIsoReader != null) {
				previousUmdIsoReader.close();
			}
			previousIsoFilename = isoFileName;
			previousUmdIsoReader = iso;
		}

		return iso;
	}

	private void sendIsoContent(OutputStream os, UmdIsoReader iso, long from, long to) throws IOException {
		int startSector = (int) (from / sectorLength);
		int endSector = (int) ((to + sectorLength) / sectorLength);
		int numberSectors = endSector - startSector;
		byte[] buffer = new byte[numberSectors * UmdIsoFile.sectorLength];
		iso.readSectors(startSector, numberSectors, buffer, 0);

		int startSectorOffset = (int) (from - startSector * (long) sectorLength);
		int length = (int) (to - from + 1);
		os.write(buffer, startSectorOffset, length);
	}

	private static Map<String, String> parseParameters(String parameters) {
		Map<String, String> result = new HashMap<String, String>();
		String[] nvpairs = parameters.split("&");
		for (String nvpair : nvpairs) {
			String[] nv = nvpair.split("=", 2);
			if (nv != null && nv.length >= 2) {
				String name = nv[0];
				String value = decodePath(nv[1]);
				result.put(name, value);
			}
		}

		return result;
	}

	private void processControls(OutputStream os, String parameters) throws IOException {
		if (parameters != null) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("processControls %s", parameters));
			}

			Map<String, String> event = parseParameters(parameters);

			String type = event.get("type");
			if ("keyup".equals(type)) {
				int code = Integer.parseInt(event.get("keyCode"));
				if (code == runMapping) {
					Emulator.getMainGUI().run();
				} else if (code == pauseMapping) {
					Emulator.getMainGUI().pause();
				} else if (code == resetMapping) {
					Emulator.getMainGUI().reset();
				} else if (keyMapping.containsKey(code)) {
					State.controller.keyReleased(keyMapping.get(code));
				} else {
					State.controller.keyReleased(code);
				}
			} else if ("keydown".equals(type)) {
				int code = Integer.parseInt(event.get("keyCode"));
				if (keyMapping.containsKey(code)) {
					State.controller.keyPressed(keyMapping.get(code));
				} else {
					State.controller.keyPressed(code);
				}
			} else if ("run".equals(type)) {
				Emulator.getMainGUI().run();
			} else if ("pause".equals(type)) {
				Emulator.getMainGUI().pause();
			} else if ("reset".equals(type)) {
				Emulator.getMainGUI().reset();
			} else if ("mapping".equals(type)) {
				processKeyMapping(event);
			} else {
				log.warn(String.format("processControls unknown type '%s'", type));
			}
		}

		sendOK(os);
		sendEndOfHeaders(os);
	}

	private void processKeyMapping(Map<String, String> event) {
		for (String key : event.keySet()) {
			String value = event.get(key);
			if (value.length() == 0) {
				// Silently ignore empty values
			} else if ("run".equals(key)) {
				runMapping = Integer.parseInt(value);
			} else if ("pause".equals(key)) {
				pauseMapping = Integer.parseInt(value);
			} else if ("reset".equals(key)) {
				resetMapping = Integer.parseInt(value);
			} else if (!"type".equals(key)) {
				try {
					keyCode code = keyCode.valueOf(key);
					keyMapping.put(Integer.parseInt(value), code);
				} catch (IllegalArgumentException e) {
					// Ignore exception
				}
			}
		}
	}

	private void sendIcon(OutputStream os, String pathValue) throws IOException {
		sendResponseFile(os, "/jpcsp/icons/" + pathValue.substring(iconDirectory.length()));
	}

	private String readInputStream(InputStream is) throws IOException {
		byte[] buffer = new byte[100000];
		int length = is.read(buffer);
		return new String(buffer, 0, length);
	}

	private String readResource(String name) throws IOException {
		InputStream is = getClass().getResourceAsStream(name);
		return readInputStream(is);
	}

	private String extractTemplateRepeat(String template) {
		int repeat = template.indexOf("$REPEAT");
		int end = template.indexOf("$END");
		if (repeat < 0 || end < 0 || end < repeat) {
			return "";
		}

		return template.substring(repeat + 7, end);
	}

	private String replaceTemplate(String template, String name, String value) {
		return template.replace(name, value);
	}

	private String replaceTemplateRepeat(String template, String value) {
		int repeat = template.indexOf("$REPEAT");
		int end = template.indexOf("$END");
		if (repeat < 0 || end < 0 || end < repeat) {
			return template;
		}

		return template.substring(0, repeat) + value + template.substring(end + 4);
	}

	private String[] getWidgetList() throws IOException {
		List<String> list = new LinkedList<String>();
		BufferedReader dir = null;
		try {
			dir = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(widgetPath)));
			while (true) {
				String entry = dir.readLine();
				if (entry == null) {
					break;
				}
				list.add(entry);
			}
		} finally {
			if (dir != null) {
				dir.close();
			}
		}

		return list.toArray(new String[list.size()]);
	}

	private InputStream getFileFromZip(String zipFileName, String fileName) throws IOException {
		if (File.separatorChar != '/') {
			fileName = fileName.replace('/', File.separatorChar);
		}

		InputStream zipInput = getClass().getResourceAsStream(zipFileName);
		if (zipInput != null) {
			ZipInputStream zipContent = new ZipInputStream(zipInput);
			while (true) {
				ZipEntry entry = zipContent.getNextEntry();
				if (entry == null) {
					break;
				}
				if (fileName.equalsIgnoreCase(entry.getName())) {
					return zipContent;
				}
			}
		}

		return null;
	}

	private void sendNaClResponse(OutputStream os, String pathValue) throws IOException {
		int sepIndex = pathValue.indexOf("/");
		if (sepIndex < 0) {
			if (pathValue.isEmpty() || indexFile.equals(pathValue)) {
				String template = readResource(rootDirectory + "/" + naclDirectory + "/" + indexFile);
				String repeat = extractTemplateRepeat(template);
				StringBuilder lines = new StringBuilder();
				for (String widget : getWidgetList()) {
					lines.append(replaceTemplate(repeat, "$NAME", widget.replace(".zip", "")));
				}
				String html = replaceTemplateRepeat(template, lines.toString());
				if (log.isDebugEnabled()) {
					log.debug(String.format("sendNaClResponse returning:\n%s", html));
				}
				sendResponseFile(os, new ByteArrayInputStream(html.getBytes()), guessMimeType(indexFile));
			} else {
				sendRedirect(os, pathValue + "/" + indexFile);
			}
			return;
		}
		String zipFileName = pathValue.substring(0, sepIndex);
		String resourceFileName = pathValue.substring(sepIndex + 1);

		if (resourceFileName.startsWith("$MANAGER_WIDGET/")) {
			// Sending dummy Widget.js and TVKeyValue.js
			sendResponseFile(os, rootDirectory + "/" + resourceFileName.substring(1));
		} else {
			zipFileName = String.format("%s/%s.zip", widgetPath, zipFileName);
			InputStream zipContent = getFileFromZip(zipFileName, resourceFileName);

			if (zipContent != null) {
				sendResponseFile(os, zipContent, guessMimeType(resourceFileName));
			} else {
				sendError(os, 404);
			}
		}
	}

	private void startDisplayAction() {
		displayActionUsageCount++;

		if (displayAction == null) {
			displayAction = new DisplayAction();
			Modules.sceDisplayModule.addDisplayAction(displayAction);
		}
	}

	private void stopDisplayAction() {
		displayActionUsageCount--;

		if (displayAction != null && displayActionUsageCount <= 0) {
			Modules.sceDisplayModule.removeDisplayAction(displayAction);
			displayAction = null;
		}
	}

	private static String getBaseUrl(HTTPServerDescriptor descriptor, HashMap<String, String> request, int forcedPort) {
		String hostName = request.get(host);
		int port = forcedPort > 0 ? forcedPort : descriptor.getPort();
		String protocol = request.get("x-forwarded-proto");
		if (protocol == null) {
			if (forcedPort > 0) {
				protocol = forcedPort == 443 ? "https" : "http";
			} else {
				protocol = descriptor.isSsl() ? "https" : "http";
			}
		}

		StringBuilder baseUrl = new StringBuilder();
		baseUrl.append(protocol);
		baseUrl.append("://");
		baseUrl.append(hostName);

		// Add the port if this is not the default one
		if (port != getDefaultPortForProtocol(protocol)) {
			baseUrl.append(":");
			baseUrl.append(port);
		}
		baseUrl.append("/");

		return baseUrl.toString();
	}

	private static String getUrl(HTTPServerDescriptor descriptor, HashMap<String, String> request, String pathValue, int forcedPort) {
		if (pathValue.startsWith("https://") || pathValue.startsWith("http://")) {
			int endOfPath = pathValue.indexOf("/", 8);
			if (endOfPath >= 0) {
				pathValue = pathValue.substring(endOfPath);
			} else {
				pathValue = "";
			}
		}

		String baseUrl = getBaseUrl(descriptor, request, forcedPort);

		String query = "";
		if (request.containsKey(parameters)) {
			query = "?" + request.get(parameters);
		}

		if (pathValue == null) {
			return baseUrl + query;
		}

		if (pathValue.startsWith("/")) {
			pathValue = pathValue.substring(1);
		}

		return baseUrl + pathValue + query;
	}

	private static String getArchitecture(HashMap<String, String> request) {
		String architecture = null;

		String userAgent = request.get("user-agent");
		if (userAgent != null && userAgent.indexOf("SmartTV") > 0) {
			// Samsung Smart TV is using the ARM architecture
			architecture = Integer.toString(Elf32Header.E_MACHINE_ARM);
		}

		return architecture;
	}

	/*
	 * Send the widgetlist.xml as expected by a Samsung Smart TV.
	 *
	 * The XML response is build dynamically, based on the packages available
	 * under the Widget directory.
	 */
	private void sendWidgetlist(HTTPServerDescriptor descriptor, HashMap<String, String> request, OutputStream os, String pathValue) throws IOException {
		String template = readResource(rootDirectory + widgetlistFile + ".template");
		String repeat = extractTemplateRepeat(template);

		String architecture = getArchitecture(request);
		String architectureParam = "";
		if (architecture != null) {
			architectureParam = "?architecture=" + architecture;
		}

		StringBuilder list = new StringBuilder();
		Pattern pattern = Pattern.compile("<widgetname(\\s+itemtype=\"string\")?>(.*)</widgetname>", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		for (String widget : getWidgetList()) {
			String zipFileName = String.format("%s/%s", widgetPath, widget);
			InputStream configXml = getFileFromZip(zipFileName, "config.xml");
			if (configXml != null) {
				String xml = readInputStream(configXml);
				Matcher matcher = pattern.matcher(xml);
				if (matcher.find()) {
					String widgetName = matcher.group(2);
					String downloadUrl = String.format("%s%s/%s%s", getBaseUrl(descriptor, request, 0), widgetDirectory, widget, architectureParam);
					String entry = replaceTemplate(repeat, "$WIDGETNAME", widgetName);
					entry = replaceTemplate(entry, "$DOWNLOADURL", downloadUrl);
					list.append(entry);
				}
			}
		}
		String xml = replaceTemplateRepeat(template, list.toString());

		sendResponseFile(os, new ByteArrayInputStream(xml.getBytes()), guessMimeType(pathValue));
	}

	private boolean isMatchingELFArchitecture(byte[] header, int length, int machineArchitecture) throws IOException {
		ByteBuffer byteBuffer = ByteBuffer.wrap(header, 0, length);
		Elf32Header elfHeader = new Elf32Header(byteBuffer);

		return elfHeader.isValid() && elfHeader.getE_machine() == machineArchitecture;
	}

	private void sendWidget(OutputStream os, String parameters, String pathValue) throws IOException {
		String architecture = null;
		if (parameters != null) {
			Map<String, String> map = parseParameters(parameters);
			architecture = map.get("architecture");
		}

		if (architecture == null) {
			sendResponseFile(os, pathValue);
		} else {
			// Filter the Widget zip file to only include the ELF files
			// matching the given architecture.
			// The Samsung Smart TV is rejecting the installation of a Widget
			// containing code for another architecture.
			int machineArchitecture = Integer.parseInt(architecture);
			ZipInputStream zin = new ZipInputStream(getClass().getResourceAsStream(pathValue));
			ByteArrayOutputStream out = new ByteArrayOutputStream(1000000);
			ZipOutputStream zout = new ZipOutputStream(out);
			byte[] buffer = new byte[100000];
			byte[] header = new byte[0x40];

			while (true) {
				ZipEntry entry = zin.getNextEntry();
				if (entry == null) {
					break;
				}

				int length = 0;
				boolean doCopy = true;
				if (entry.getName().endsWith(".nexe")) {
					length = zin.read(header);
					if (!isMatchingELFArchitecture(header, length, machineArchitecture)) {
						if (log.isDebugEnabled()) {
							log.debug(String.format("Skipping the Widget entry '%s' because it is not matching the architecture 0x%X", entry.getName(), machineArchitecture));
						}
						doCopy = false;
					}
				}

				if (doCopy) {
					zout.putNextEntry(entry);
					zout.write(header, 0, length);
					while (true) {
						length = zin.read(buffer);
						if (length <= 0) {
							break;
						}
						zout.write(buffer, 0, length);
					}
				}
			}
			zin.close();
			zout.close();

			sendResponseFile(os, new ByteArrayInputStream(out.toByteArray()), guessMimeType(pathValue));
		}
	}

	public Proxy getProxy() {
		return proxy;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public int getProxyAddress() {
		return proxyAddress;
	}

	public void sendNpNavAuth(String data, OutputStream os) throws IOException {
		Map<String, String> parameters = parseParameters(data);

		SceNpTicket ticket = new SceNpTicket();
		ticket.version = 0x00000121;
		ticket.size = 0xF0;
		ticket.unknown = 0x3000;
		ticket.sizeParams = 0xA4;
		addTicketParam(ticket, "XXXXXXXXXXXXXXXXXXXX", 20);
		addTicketParam(ticket, 0);
		long now = System.currentTimeMillis();
		addTicketDateParam(ticket, now);
		addTicketDateParam(ticket, now + 10 * 60 * 1000); // now + 10 minutes
		addTicketLongParam(ticket, 0L); // Used by DRM
		addTicketParam(ticket, TicketParam.PARAM_TYPE_STRING, "DummyOnlineID", 32);
		addTicketParam(ticket, "gb", 4);
		addTicketParam(ticket, TicketParam.PARAM_TYPE_STRING, "XX", 4);
		addTicketParam(ticket, parameters.get("serviceid"), 24);
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
			log.debug(String.format("sendNpNavAuth returning dummy ticket: %s", ticket));
		}
		byte[] response = ticket.toByteArray();

		sendOK(os);
		sendResponseHeader(os, "X-I-5-Status", "OK");
		sendResponseHeader(os, "X-I-5-Version", "2.1");
		sendResponseHeader(os, "Content-Length", response.length);
		sendResponseHeader(os, "Content-Type", "application/x-i-5-ticket");
		sendEndOfHeaders(os);
		os.write(response);
	}

	public void sendNpGetSelfProfile(String data, OutputStream os) throws IOException {
		String xml = "<profile result=\"00\">";
		xml += "<jid>DummyOnlineID@a8.gb.np.playstation.net</jid>";
		xml += "<onlinename upd=\"0\">DummyOnlineID</onlinename>";
		xml += "<country>gb</country>";
		xml += "<language1>1</language1>";
		xml += "<language2 />";
		xml += "<language3 />";
		xml += "<aboutme />";
		xml += "<avatarurl id=\"0\">http://static-resource.np.community.playstation.net/avatar_s/default/DefaultAvatar_s.png</avatarurl>";
		xml += "<ptlp>0</ptlp>";
		xml += "</profile>";
		byte[] response = xml.getBytes();

		sendOK(os);
		sendResponseHeader(os, "Content-Length", response.length);
		sendResponseHeader(os, "Content-Type", "text/xml;charset=UTF-8");
		sendEndOfHeaders(os);
		os.write(response);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Response: %s", xml));
		}
	}

	public void sendCapM(String data, OutputStream os) throws IOException {
		int responseLength = 4240;
		byte[] response = new byte[responseLength];

		if (ticket != null) {
			TicketParam ticketParam = ticket.parameters.get(4);
			for (int i = 0; i < 8; i++) {
				response[i + 80] = ticketParam.getBytesValue()[7 - i];
			}
		}

		sendOK(os);
		sendResponseHeader(os, "X-I-5-DRM-Version", "1.0");
		sendResponseHeader(os, "X-I-5-DRM-Status", "OK; max_console=1; current_console=0");
		sendResponseHeader(os, "Content-Length", responseLength);
		sendResponseHeader(os, "Content-Type", "application/x-i-5-drm");
		sendEndOfHeaders(os);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Response:%s", Utilities.getMemoryDump(response, 0, responseLength)));
		}

		os.write(response, 0, responseLength);
	}

	public void sendKdpM(String data, OutputStream os) throws IOException {
		Map<String, String> parameters = parseParameters(data);
		String productId = parameters.get("productid");

		int responseLength = 4240;
		byte[] response = new byte[responseLength];

		if (productId != null) {
			ByteBuffer buffer = ByteBuffer.wrap(response);
			buffer.position(16);
			Utilities.writeStringZ(buffer, productId);
		}

		if (ticket != null) {
			TicketParam ticketParam = ticket.parameters.get(4);
			for (int i = 0; i < 8; i++) {
				response[i + 80] = ticketParam.getBytesValue()[7 - i];
			}
		}

		sendOK(os);
		sendResponseHeader(os, "X-I-5-DRM-Version", "1.0");
		sendResponseHeader(os, "X-I-5-DRM-Status", "OK");
		sendResponseHeader(os, "Content-Length", responseLength);
		sendResponseHeader(os, "Content-Type", "application/x-i-5-drm");
		sendEndOfHeaders(os);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Response:%s", Utilities.getMemoryDump(response, 0, responseLength)));
		}

		os.write(response, 0, responseLength);
	}

	public void sendVideoStore(OutputStream os) throws IOException {
		byte[] response = new byte[1];
		response[0] = (byte) '3';
		int responseLength = response.length;

		sendOK(os);
		sendResponseHeader(os, "Content-Length", responseLength);
		sendEndOfHeaders(os);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Response:%s", Utilities.getMemoryDump(response, 0, responseLength)));
		}

		os.write(response, 0, responseLength);
	}
}
