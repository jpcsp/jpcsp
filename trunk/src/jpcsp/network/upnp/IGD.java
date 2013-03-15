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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class IGD {
	protected static Logger log = UPnP.log;
	String descriptionUrl;
	String baseUrl;
	String presentationUrl;
	IGDdataService cif;
	IGDdataService first;
	IGDdataService second;
	IGDdataService ipV6FC;

	protected static class IGDdataService {
		String serviceType;
		String controlUrl;
		String eventSubUrl;
		String scpdUrl;

		@Override
		public String toString() {
			return String.format("serviceType=%s[controlUrl=%s, eventSubUrl=%s, scpdUrl=%s]", serviceType, controlUrl, eventSubUrl, scpdUrl);
		}
	}

	public IGD() {
	}

	public void discover(String descriptionUrl) {
    	DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setIgnoringElementContentWhitespace(true);
		documentBuilderFactory.setIgnoringComments(true);
		documentBuilderFactory.setCoalescing(true);

		try {
			URL url = new URL(descriptionUrl);
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document description = documentBuilder.parse(url.openStream());
			parseIGDdata(description);
			this.descriptionUrl = descriptionUrl;
		} catch (ParserConfigurationException e) {
			log.error("Discovery", e);
		} catch (SAXException e) {
			log.error("Discovery", e);
		} catch (MalformedURLException e) {
			log.error("Discovery", e);
		} catch (IOException e) {
			log.error("Discovery", e);
		}
	}

	public boolean isValid() {
		return first != null && first.serviceType != null;
	}

	public boolean isConnected(UPnP upnp) {
		return "Connected".equals(upnp.getStatusInfo(buildUrl(first.controlUrl), first.serviceType));
	}

	public String getExternalIPAddress(UPnP upnp) {
		return upnp.getExternalIPAddress(buildUrl(first.controlUrl), first.serviceType);
	}

	public void addPortMapping(UPnP upnp, String remoteHost, int externalPort, String protocol, int internalPort, String internalClient, String description, int leaseDuration) {
		if (first != null) {
			upnp.addPortMapping(buildUrl(first.controlUrl), first.serviceType, remoteHost, externalPort, protocol, internalPort, internalClient, description, leaseDuration);
		}
	}

	public void deletePortMapping(UPnP upnp, String remoteHost, int externalPort, String protocol) {
		if (first != null) {
			upnp.deletePortMapping(buildUrl(first.controlUrl), first.serviceType, remoteHost, externalPort, protocol);
		}
	}

	private void parseIGDdata(Document description) {
		baseUrl = null;
		presentationUrl = null;
		cif = null;
		first = null;
		second = null;
		ipV6FC = null;
		parseElement(description.getDocumentElement());

		log.info(String.format("IGD data: %s", toString()));
	}

	private void parseElement(Element element) {
		if ("service".equals(element.getNodeName())) {
			parseService(element);
		} else if ("URLBase".equals(element.getNodeName())) {
			baseUrl = getContent(element);
		} else if ("presentationURL".equals(element.getNodeName())) {
			presentationUrl = getContent(element);
		} else {
			NodeList children = element.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node node = children.item(i);
				if (node instanceof Element) {
					parseElement((Element) node);
				}
			}
		}
	}

	private String getContent(Node node) {
		if (node.hasChildNodes()) {
			return getContent(node.getChildNodes());
		}

		return node.getNodeValue();
	}

	private String getContent(NodeList nodeList) {
		if (nodeList == null || nodeList.getLength() <= 0) {
			return null;
		}

		StringBuilder content = new StringBuilder();
		int n = nodeList.getLength();
		for (int i = 0; i < n; i++) {
			Node node = nodeList.item(i);
			content.append(getContent(node));
		}

		return content.toString();
	}

	private String getNodeValue(Element element, String nodeName) {
		return getContent(element.getElementsByTagName(nodeName));
	}

	private void parseService(Element element) {
		String serviceType = getNodeValue(element, "serviceType");
		IGDdataService dataService = null;
		if ("urn:schemas-upnp-org:service:WANCommonInterfaceConfig:1".equals(serviceType)) {
			cif = new IGDdataService();
			dataService = cif;
		} else if ("urn:schemas-upnp-org:service:WANIPv6FirewallControl:1".equals(serviceType)) {
			ipV6FC = new IGDdataService();
			dataService = ipV6FC;
		} else if ("urn:schemas-upnp-org:service:WANIPConnection:1".equals(serviceType) ||
		           "urn:schemas-upnp-org:service:WANPPPConnection:1".equals(serviceType)) {
			if (first == null) {
				first = new IGDdataService();
				dataService = first;
			} else if (second == null) {
				second = new IGDdataService();
				dataService = second;
			}
		}

		if (dataService != null) {
			dataService.serviceType = serviceType;
			dataService.controlUrl = getNodeValue(element, "controlURL");
			dataService.eventSubUrl = getNodeValue(element, "eventSubURL");
			dataService.scpdUrl = getNodeValue(element, "SCPDURL");
		}
	}

	protected String buildUrl(String url) {
		if (url.matches("^https?://.*")) {
			return url;
		}

		StringBuilder completeUrl = new StringBuilder();
		if (baseUrl != null && baseUrl.length() > 0) {
			completeUrl.append(baseUrl);
		} else {
			completeUrl.append(descriptionUrl);
		}

		int firstColon = completeUrl.indexOf(":");
		if (firstColon >= 0) {
			int firstSep = completeUrl.indexOf("/", firstColon + 3);
			if (firstSep >= 0) {
				completeUrl.setLength(firstSep);
			}
		}

		if (!url.startsWith("/")) {
			completeUrl.append("/");
		}
		completeUrl.append(url);

		return completeUrl.toString();
	}

	@Override
	public String toString() {
		return String.format("urlBase=%s, presentationUrl=%s, CIF: %s, first: %s, second: %s, IPv6FC: %s", baseUrl, presentationUrl, cif, first, second, ipV6FC);
	}
}
