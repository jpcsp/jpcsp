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

import static jpcsp.filesystems.umdiso.UmdIsoFile.sectorLength;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import jpcsp.Controller.keyCode;
import jpcsp.Emulator;
import jpcsp.MainGUI;
import jpcsp.State;
import jpcsp.HLE.Modules;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class HTTPServer {
	private static Logger log = Logger.getLogger("http");
	private static HTTPServer instance;
	private static final int port = 80;
	private static final String method = "method";
	private static final String path = "path";
	private static final String parameters = "parameters";
	private static final String version = "version";
	private static final String eol = "\r\n";
	private static final String boundary = "--boundarybetweensingleimages";
	private static final String isoDirectory = "/iso/";
	private static final String iconDirectory = "/icon/";
	private HTTPServerThread serverThread;
	private Robot captureRobot;
	private UmdIsoReader previousUmdIsoReader;
	private String previousIsoFilename;
	private HashMap<Integer, keyCode> keyMapping;
	private int runMapping = -1;
	private int pauseMapping = -1;
	private int resetMapping = -1;
	private static final int MAX_COMPRESSED_COUNT = 0x7F;

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
		keyMapping = new HashMap<Integer, keyCode>();

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
				// Parse e.g. "GET / HTTP/1.1" into 3 words: "GET", "/" and "HTTP/1.1"
				String[] words = lines[i].split(" ");
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
			} else {
				// Parse e.g. "Host: localhost:30005" into 2 words: "Host" and "localhost:30005"
				String[] words = lines[i].split(": *", 2);
				if (words.length >= 2) {
					headers.put(words[0].toLowerCase(), words[1]);
				}
			}
		}

		return headers;
	}

	private void process(HashMap<String, String> request, OutputStream os) throws IOException {
		try {
			String pathValue = request.get(path);
			if ("GET".equals(request.get(method))) {
				if ("/".equals(pathValue)) {
					sendHTMLResponseFile(os, "html/index.html");
				} else if (pathValue.endsWith(".html")) {
					sendHTMLResponseFile(os, "html" + pathValue);
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
				} else if ("/widgetlist.xml".equals(pathValue)) {
					sendResponseFile(os, "html/widgetlist.xml", "text/xml");
				} else if (pathValue.startsWith("/Widget/")) {
					sendResponseFile(os, "html" + pathValue, "application/octet-stream");
				} else {
					sendErrorNotFound(os);
				}
			} else if ("HEAD".equals(request.get(method))) {
				if (pathValue.startsWith(isoDirectory)) {
					sendIso(request, os, pathValue, false);
				} else {
					sendErrorNotFound(os);
				}
			} else {
				sendError(os, 405, "Method Not Allowed");
			}
		} catch (SocketException e) {
			// Ignore exception (e.g. Connection reset by peer)
		}
	}

	private void sendResource(OutputStream os, String name) throws IOException {
		InputStream input = getClass().getResourceAsStream(name);

		if (input != null) {
			byte[] buffer = new byte[1000];
			while (true) {
				int length = input.read(buffer);
				if (length < 0) {
					break;
				}
				os.write(buffer, 0, length);
			}
			input.close();
		}
	}

	private void sendResponseFile(OutputStream os, String fileName, String contentType) throws IOException {
		sendHTTPResponseCode(os, 200, "OK");
		if (contentType != null) {
			sendResponseHeader(os, "Content-Type", contentType);
		}
		sendEndOfHeaders(os);
		sendResource(os, fileName);
	}

	private void sendHTMLResponseFile(OutputStream os, String fileName) throws IOException {
		sendResponseFile(os, fileName, "text/html");
	}

	private void sendResponseLine(OutputStream os, String line) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Response: %s", line));
		}
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

	private void sendResponseHeader(OutputStream os, String name, long value) throws IOException {
		sendResponseHeader(os, name, String.valueOf(value));
	}

	private void sendEndOfHeaders(OutputStream os) throws IOException {
		sendResponseLine(os, "");
	}

	private void sendError(OutputStream os, int code, String msg) throws IOException {
		sendHTTPResponseCode(os, code, msg);
		sendEndOfHeaders(os);
	}

	private void sendErrorNotFound(OutputStream os) throws IOException {
		sendError(os, 404, "Not Found");
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
			sendErrorNotFound(os);
    	}
	}

	private void sendVideoMJPG(OutputStream os) throws IOException {
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
    	}
	}

	private void sendAudioWAV(OutputStream os) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("sendAudioWAV"));
		}
        sendHTTPResponseCode(os, 200, "OK");
        sendResponseHeader(os, "Content-Type", "audio/wav");
        sendEndOfHeaders(os);

        int channels = 2;
        int sampleRate = 44100;

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
        sendHTTPResponseCode(os, 200, "OK");
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
        return captureRobot.createScreenCapture(rect);
	}

	private void sendVideoRAW(OutputStream os) throws IOException {
		Rectangle rect = Emulator.getMainGUI().getCaptureRectangle();
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Capturing RAW screen from %s", rect));
    	}

        sendHTTPResponseCode(os, 200, "OK");
        sendResponseHeader(os, "Cache-Control", "no-cache");
        sendResponseHeader(os, "Cache-Control", "private");
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

	private void sendVideoCompressedRAW(OutputStream os) throws IOException {
		Rectangle rect = Emulator.getMainGUI().getCaptureRectangle();
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Capturing compressed RAW screen from %s", rect));
    	}

        sendHTTPResponseCode(os, 200, "OK");
        sendResponseHeader(os, "Cache-Control", "no-cache");
        sendResponseHeader(os, "Cache-Control", "private");
        sendResponseHeader(os, "Content-Type", "video/compressed-raw");
        sendEndOfHeaders(os);

        int[] image = new int[rect.width * rect.height];
        int[] previousImage = new int[rect.width * rect.height];
        byte[] buffer = new byte[rect.width * rect.height * 4 + 4];

        while (true) {
            BufferedImage img = getScreenImage(rect);

	        int width = img.getWidth();
	        int height = img.getHeight();
	        int i = 0;
	        for (int y = 0; y < height; y++) {
	        	for (int x = 0; x < width; x++, i++) {
	        		int color = img.getRGB(x, y);
	        		image[i] = color & 0x00FFFFFF;
	        	}
	        }

	        // The first 4 bytes of the buffer will contain the length of the compressed image
	        int compressedLength = compressImage(width, height, image, previousImage, buffer, 4);
	        // Store the length of the compressed image
	        buffer[0] = (byte) ((compressedLength >>  0) & 0xFF);
	        buffer[1] = (byte) ((compressedLength >>  8) & 0xFF);
	        buffer[2] = (byte) ((compressedLength >> 16) & 0xFF);
	        buffer[3] = (byte) ((compressedLength >> 24) & 0xFF);

	        os.write(buffer, 0, compressedLength);
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sendVideoCompressedRAW sent %dx%d image (%d bytes, compression rate %.1f%%)", width, height, compressedLength, 100f * compressedLength / (image.length * 3)));
        	}
	        os.flush();

	        // Swap previous and current image buffers
	        int[] swapImage = image;
	        image = previousImage;
	        previousImage = swapImage;
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

								sendHTTPResponseCode(os, 206, "Partial Content");
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
					sendHTTPResponseCode(os, 200, "OK");

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
				File[] umdPaths = MainGUI.getUmdPaths();
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

		sendHTTPResponseCode(os, 200, "OK");
		sendEndOfHeaders(os);
	}

	private void processKeyMapping(Map<String, String> event) {
		for (String key : event.keySet()) {
			if ("run".equals(key)) {
				runMapping = Integer.parseInt(event.get(key));
			} else if ("pause".equals(key)) {
				pauseMapping = Integer.parseInt(event.get(key));
			} else if ("reset".equals(key)) {
				resetMapping = Integer.parseInt(event.get(key));
			} else if (!"type".equals(key)) {
				try {
					keyCode code = keyCode.valueOf(key);
					keyMapping.put(Integer.parseInt(event.get(key)), code);
				} catch (IllegalArgumentException e) {
					// Ignore exception
				}
			}
		}
	}

	private void sendIcon(OutputStream os, String pathValue) throws IOException {
		sendHTTPResponseCode(os, 200, "OK");
		sendResponseHeader(os, "Content-Type", "image/png");
		sendEndOfHeaders(os);

		sendResource(os, "/jpcsp/icons/" + pathValue.substring(iconDirectory.length()));
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
}
