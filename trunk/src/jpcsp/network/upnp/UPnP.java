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
package jpcsp.network.upnp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jpcsp.State;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class UPnP {
	public static Logger log = Logger.getLogger("upnp");
	protected IGD igd;
	public  static final int discoveryTimeoutMillis = 2000;
	public  static final int discoveryPort = 1900;
	public  static final String multicastIp = "239.255.255.250";
	private static final String[] deviceList = new String[] {
			"urn:schemas-upnp-org:device:InternetGatewayDevice:1",
			"urn:schemas-upnp-org:service:WANIPConnection:1",
			"urn:schemas-upnp-org:service:WANPPPConnection:1",
			"upnp:rootdevice"
	};

	protected static class Device {
		public String descURL;
		public String st;
	}

	public void discover() {
		try {
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(discoveryTimeoutMillis);
			socket.setReuseAddress(true);
			byte[] response = new byte[1536];
			DatagramPacket responsePacket = new DatagramPacket(response, response.length);
			List<Device> devices = new LinkedList<Device>();
			for (String device : deviceList) {
				if (responsePacket.getPort() == -1) {
					String discoveryRequest = String.format("M-SEARCH * HTTP/1.1\r\nHOST: %s:%d\r\nST: %s\r\nMAN: \"ssdp:discover\"\r\nMX: %d\r\n\r\n", multicastIp, discoveryPort, device, discoveryTimeoutMillis / 1000);
					DatagramPacket packet = new DatagramPacket(discoveryRequest.getBytes(), discoveryRequest.length(), new InetSocketAddress(multicastIp, discoveryPort));
					socket.send(packet);
				}
				try {
					socket.receive(responsePacket);
					if (responsePacket.getLength() > 0) {
						String reply = new String(responsePacket.getData(), responsePacket.getOffset(), responsePacket.getLength());
						log.debug(String.format("Discovery %s: %s", device, reply));
						Pattern p = Pattern.compile("^location: *(\\S+)$.*^st: *(\\S+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
						Matcher m = p.matcher(reply);
						if (m.find()) {
							String location = m.group(1);
							String st = m.group(2);
							log.debug(String.format("Location: '%s', st: '%s'", location, st));
							Device newDevice = new Device();
							newDevice.descURL = location;
							newDevice.st = st;
							devices.add(newDevice);
						} else {
							log.error(String.format("Could not parse discovery response for %s: %s", device, reply));
						}
					}
					responsePacket = new DatagramPacket(response, response.length);
				} catch (SocketTimeoutException e) {
					log.info(String.format("Timeout while discovering %s", device));
				}
			}
			socket.close();

			igd = new IGD();
			Set<String> processedUrls = new HashSet<String>();
			for (Device device : devices) {
				if (!processedUrls.contains(device.descURL)) {
					igd.discover(device.descURL);
					processedUrls.add(device.descURL);
					if (igd.isValid() && igd.isConnected(this)) {
						log.info(String.format("IGD connected with external IP: %s", igd.getExternalIPAddress(this)));
						break;
					}
				}
			}
		} catch (IOException e) {
			log.error("discover", e);
		}
	}

	public IGD getIGD() {
		return igd;
	}

	protected HashMap<String, String> executeSimpleUPnPcommand(String controlUrl, String serviceType, String action, HashMap<String, String> arguments) {
		HashMap<String, String> result = null;

		StringBuilder body = new StringBuilder();

		body.append(String.format("<?xml version=\"1.0\"?>\r\n"));
		body.append(String.format("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\r\n"));
		body.append(String.format("<s:Body>\r\n"));
		body.append(String.format("  <u:%s xmlns:u=\"%s\">\r\n", action, serviceType));
		if (arguments != null) {
			for (String name : arguments.keySet()) {
				String value = arguments.get(name);
				if (value == null) {
					body.append(String.format("    <%s />\r\n", name));
				} else {
					body.append(String.format("    <%s>%s</%s>\r\n", name, value, name));
				}
			}
		}
		body.append(String.format("  </u:%s>\r\n", action));
		body.append(String.format("</s:Body>\r\n"));
		body.append(String.format("</s:Envelope>\r\n"));

		if (log.isTraceEnabled()) {
			log.trace(String.format("Sending UPnP command: %s", body.toString()));
		}

		try {
			URL url = new URL(controlUrl);
			URLConnection connection = url.openConnection();
			if (connection instanceof HttpURLConnection) {
				HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
				httpURLConnection.setRequestMethod("POST");
			}
			connection.setRequestProperty("SOAPAction", String.format("%s#%s", serviceType, action));
			connection.setRequestProperty("Content-Type", "text/xml");
			connection.setDoOutput(true);
			OutputStream output = connection.getOutputStream();
			output.write(body.toString().getBytes());
			output.flush();
			output.close();

			connection.connect();

			InputStream response = connection.getInputStream();
			StringBuilder content = new StringBuilder();
			byte[] buffer = new byte[1024];
			int n;
			do {
				n = response.read(buffer);
				if (n > 0) {
					content.append(new String(buffer, 0, n));
				}
			} while (n >= 0);
			response.close();

			if (log.isDebugEnabled()) {
				log.debug(String.format("UPnP command serviceType %s, action %s, result: %s", serviceType, action, content.toString()));
			}

			result = parseSimpleCommandResponse(content.toString());

			if (log.isDebugEnabled()) {
				String errorCode = result.get("errorCode");
				if (errorCode != null) {
					log.debug(String.format("UPnP command %s: errorCode = %s", action, errorCode));
				}
			}
		} catch (MalformedURLException e) {
			log.error("executeUPnPcommand", e);
		} catch (IOException e) {
			log.error("executeUPnPcommand", e);
		}

		return result;
	}

	protected HashMap<String, String> parseSimpleCommandResponse(String content) {
		HashMap<String, String> result = null;

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setIgnoringElementContentWhitespace(true);
		documentBuilderFactory.setIgnoringComments(true);
		documentBuilderFactory.setCoalescing(true);

		try {
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document response = documentBuilder.parse(new ByteArrayInputStream(content.getBytes()));
			result = new HashMap<String, String>();
			parseElement(response.getDocumentElement(), result, null);
		} catch (ParserConfigurationException e) {
			log.error("Discovery", e);
		} catch (SAXException e) {
			log.error("Discovery", e);
		} catch (MalformedURLException e) {
			log.error("Discovery", e);
		} catch (IOException e) {
			log.error("Discovery", e);
		}

		return result;
	}

	protected void parseElement(Element element, HashMap<String, String> result, String name) {
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node instanceof Element) {
				parseElement((Element) node, result, node.getNodeName());
			} else if (name != null && node.getTextContent() != null) {
				String value = node.getTextContent();
				if (result.containsKey(name)) {
					value = result.get(name) + value;
				}
				result.put(name, value);
			}
		}
	}

	public String getStatusInfo(String controlUrl, String serviceType) {
		HashMap<String, String> result = executeSimpleUPnPcommand(controlUrl, serviceType, "GetStatusInfo", null);

		return result.get("NewConnectionStatus");
	}

	public String getExternalIPAddress(String controlUrl, String serviceType) {
		HashMap<String, String> result = executeSimpleUPnPcommand(controlUrl, serviceType, "GetExternalIPAddress", null);

		return result.get("NewExternalIPAddress");
	}

	public void addPortMapping(String controlUrl, String serviceType, String remoteHost, int externalPort, String protocol, int internalPort, String internalClient, String description, int leaseDuration) {
		HashMap<String, String> arguments = new HashMap<String, String>();

		arguments.put("NewRemoteHost", remoteHost);
		arguments.put("NewExternalPort", Integer.toString(externalPort));
		arguments.put("NewProtocol", getProtocol(protocol));
		arguments.put("NewInternalPort", Integer.toString(internalPort));
		arguments.put("NewInternalClient", internalClient);
		arguments.put("NewEnabled", "1");
		arguments.put("NewPortMappingDescription", description != null ? description : String.format("Jpcsp-%s", State.discId));
		arguments.put("NewLeaseDuration", Integer.toString(leaseDuration));

		HashMap<String, String> result = executeSimpleUPnPcommand(controlUrl, serviceType, "AddPortMapping", arguments);

		if (log.isDebugEnabled() && result != null) {
			log.debug(String.format("addPortMapping errorCode=%s", result.get("errorCode")));
		}
	}

	public void deletePortMapping(String controlUrl, String serviceType, String remoteHost, int externalPort, String protocol) {
		HashMap<String, String> arguments = new HashMap<String, String>();

		arguments.put("NewRemoteHost", remoteHost);
		arguments.put("NewExternalPort", Integer.toString(externalPort));
		arguments.put("NewProtocol", getProtocol(protocol));

		HashMap<String, String> result = executeSimpleUPnPcommand(controlUrl, serviceType, "DeletePortMapping", arguments);

		if (log.isDebugEnabled() && result != null) {
			log.debug(String.format("deletePortMapping errorCode=%s", result.get("errorCode")));
		}
	}

	protected String getProtocol(String protocol) {
		if (protocol != null) {
			protocol = protocol.toUpperCase();
			if (!protocol.equals("TCP") && !protocol.equals("UDP")) {
				// Unknown protocol
				protocol = null;
			}
		}

		return protocol;
	}
}
