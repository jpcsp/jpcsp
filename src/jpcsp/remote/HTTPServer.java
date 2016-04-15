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

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import jpcsp.Emulator;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class HTTPServer {
	private static Logger log = Logger.getLogger("http");
	private static HTTPServer instance;
	private static final int port = 30005;
	private static final String method = "method";
	private static final String path = "path";
	private static final String version = "version";
	private static final String eol = "\r\n";
	private static final String boundary = "--boundarybetweensingleimages";
	private HTTPServerThread serverThread;
	private Robot captureRobot;

	public static HTTPServer getInstance() {
		if (instance == null) {
			instance = new HTTPServer();
		}
		return instance;
	}

	private class HTTPServerThread extends Thread {
		private boolean exit;
		private ServerSocket serverSocket;

		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(port);
				serverSocket.setSoTimeout(1);
			} catch (IOException e) {
				log.error(String.format("Server socket at port %d not available: %s", port, e));
				exit();
			}

			while (!exit) {
				try {
					Socket socket = serverSocket.accept();
					socket.setSoTimeout(1);
					HTTPSocketHandlerThread handlerThread = new HTTPSocketHandlerThread(socket);
					handlerThread.setName(String.format("HTTP Handler %d", socket.getPort()));
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

			serverThread = null;
		}

		public void exit() {
			exit = true;
		}
	}

	private class HTTPSocketHandlerThread extends Thread {
		Socket socket;

		public HTTPSocketHandlerThread(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			process(socket);
		}
	}

	private HTTPServer() {
		serverThread = new HTTPServerThread();
		serverThread.setDaemon(true);
		serverThread.setName("HTTP Server");
		serverThread.start();

		try {
			captureRobot = new Robot();
			captureRobot.setAutoDelay(0);
		} catch (AWTException e) {
			log.error("Create captureRobot", e);
		}
	}

	private void process(Socket socket) {
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
					if (log.isDebugEnabled()) {
						log.debug(String.format("Received request: '%s', headers: %s", request, requestHeaders));
					}
					process(requestHeaders, os);
					os.flush();
					break;
				}
			} catch (SocketTimeoutException e) {
				// Ignore timeout
			} catch (IOException e) {
				if (log.isDebugEnabled()) {
					log.debug("Receive socket", e);
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
		String[] lines = request.split(eol);

		for (int i = 0; i < lines.length; i++) {
			if (i == 0) {
				// Parse e.g. "GET / HTTP/1.1"
				String[] words = lines[i].split(" ");
				if (words.length >= 1) {
					headers.put(method, words[0]);
				}
				if (words.length >= 2) {
					headers.put(path, words[1]);
				}
				if (words.length >= 3) {
					headers.put(version, words[2]);
				}
			} else {
				// Parse e.g. "Host: localhost:30005"
				String[] words = lines[i].split(": *", 2);
				if (words.length >= 2) {
					headers.put(words[0].toLowerCase(), words[1]);
				}
			}
		}

		return headers;
	}

	private void process(HashMap<String, String> request, OutputStream os) throws IOException {
		if ("GET".equals(request.get(method))) {
			if ("/".equals(request.get(path))) {
				sendHTMLResponse(os, "<h1>Welcome to Jpcsp!</h1>You can stream the <a href=\"video\">Video</a>, or display a single <a href=\"screenshot\">Screenshot</a>.");
			} else if ("/hello".equals(request.get(path))) {
				sendHTMLResponse(os, "Hello from Jpcsp!");
			} else if ("/screenshot".equals(request.get(path))) {
				sendHTMLResponse(os, "<img src=\"screen.png\" /></br><a href=\"screenshot\">Refresh</a>");
			} else if ("/screen.png".equals(request.get(path))) {
				sendScreenImage(os, "png");
			} else if ("/screen.jpg".equals(request.get(path))) {
				sendScreenImage(os, "jpg");
			} else if ("/video".equals(request.get(path))) {
//				sendHTMLResponse(os, "<img src=\"screen.mjpg\" /><audio controls><source src=\"audio.l16\" type=\"audio/l16; rate=44100; channels=2\"></audio>");
				sendHTMLResponse(os, "<img src=\"screen.mjpg\" /><audio controls><source src=\"audio.l16\" type=\"audio/mp3\"></audio>");
			} else if ("/screen.mjpg".equals(request.get(path))) {
				sendScreenVideo(os);
			} else if ("/audio.l16".equals(request.get(path))) {
				sendAudioL16(os);
			} else {
				sendError(os, 404, "Not Found");
			}
		} else {
			sendError(os, 405, "Method Now Allowed");
		}
	}

	private void sendHTMLResponse(OutputStream os, String html) throws IOException {
		sendHTTPResponseCode(os, 200, "OK");
		sendResponseHeader(os, "Content-Type", "text/html");
		sendEndOfHeaders(os);
		os.write("<html>".getBytes());
		os.write(html.getBytes());
		os.write("</html>".getBytes());
	}

	private void sendResponseLine(OutputStream os, String line) throws IOException {
		os.write(line.getBytes());
		os.write(eol.getBytes());
	}

	private void sendHTTPResponseCode(OutputStream os, int code, String msg) throws IOException {
		sendResponseLine(os, String.format("HTTP/1.1 %d %s", code, msg));
	}

	private void sendResponseHeader(OutputStream os, String name, String value) throws IOException {
		sendResponseLine(os, String.format("%s: %s", name, value));
	}

	private void sendResponseHeader(OutputStream os, String name, int value) throws IOException {
		sendResponseHeader(os, name, String.valueOf(value));
	}

	private void sendEndOfHeaders(OutputStream os) throws IOException {
		sendResponseLine(os, "");
	}

	private void sendError(OutputStream os, int code, String msg) throws IOException {
		sendHTTPResponseCode(os, code, msg);
		sendEndOfHeaders(os);
	}

	private void sendScreenImage(OutputStream os, String fileFormat) throws IOException {
        String fileName = String.format("%s%cscreen.%s", Settings.getInstance().readString("emu.tmppath"), File.separatorChar, fileFormat);
		File file = new File(fileName);
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
	        sendHTTPResponseCode(os, 200, "OK");
	        sendResponseHeader(os, "Cache-Control", "no-cache");
	        sendResponseHeader(os, "Cache-Control", "private");
	        sendResponseHeader(os, "Content-Type", String.format("image/%s", fileFormat));
	        sendResponseHeader(os, "Content-Length", length);
	        sendEndOfHeaders(os);
	        byte[] buffer = new byte[length];
	        InputStream is = new FileInputStream(file);
	        length = is.read(buffer);
	        is.close();
	        os.write(buffer, 0, length);
    	} else {
			sendError(os, 404, "Not Found");
    	}
	}

	private void sendScreenVideo(OutputStream os) throws IOException {
		String fileFormat = "jpg";
        String fileName = String.format("%s%cscreen.%s", Settings.getInstance().readString("emu.tmppath"), File.separatorChar, fileFormat);
		File file = new File(fileName);
		Rectangle rect = Emulator.getMainGUI().getCaptureRectangle();
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Capturing screen from %s to %s", rect, fileName));
    	}

        sendHTTPResponseCode(os, 200, "OK");
        sendResponseHeader(os, "Cache-Control", "no-cache");
        sendResponseHeader(os, "Cache-Control", "private");
        sendResponseHeader(os, "Content-Type", String.format("multipart/x-mixed-replace; boundary=%s", boundary));
        sendEndOfHeaders(os);

        while (true) {
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

	    	// Utilities.sleep(33, 0);
    	}
	}

	private void sendAudioL16(OutputStream os) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("sendAudioL16"));
		}
        sendHTTPResponseCode(os, 200, "OK");
        //sendResponseHeader(os, "Content-Type", "audio/l16; rate=44100; channels=2");
        sendResponseHeader(os, "Content-Type", "audio/wav");
        sendEndOfHeaders(os);

        byte[] buffer = new byte[10240];
        InputStream is = null;
        try {
        	is = new FileInputStream("sample.wav");
	        while (true) {
	        	//os.write(Modules.sceAudioModule.audioData);
	        	is.read(buffer);
	        	os.write(buffer);
	        }
        } finally {
        	if (is != null) {
        		is.close();
        	}
        }
	}
}
