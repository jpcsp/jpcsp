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
package jpcsp.crypto;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jpcsp.util.Utilities;

/**
 * List of values that can't be decrypted on Jpcsp due to missing keys.
 * They have been decrypted on a real PSP and we just reuse the result from the PSP here.
 */
public class PreDecrypt {
	public static Logger log = CryptoEngine.log;
	private static PreDecryptInfo preDecrypts[];

	private static class PreDecryptInfo {
		private byte[] preIn;
		private byte[] preOut;
		private int preCmd;

		public PreDecryptInfo(byte[] preIn, byte[] preOut, int preCmd) {
			this.preIn = preIn;
			this.preOut = preOut;
			this.preCmd = preCmd;
		}

		public boolean decrypt(byte[] out, int outSize, byte[] in, int inSize, int cmd) {
			if (preCmd != cmd) {
				return false;
			}
			if (preIn.length != inSize || preOut.length != outSize) {
				return false;
			}

			for (int i = 0; i < inSize; i++) {
				if (preIn[i] != in[i]) {
					return false;
				}
			}

			System.arraycopy(preOut, 0, out, 0, outSize);

			return true;
		}
	}

	public static boolean preDecrypt(byte[] out, int outSize, byte[] in, int inSize, int cmd) {
		for (PreDecryptInfo preDecrypt : preDecrypts) {
			if (preDecrypt.decrypt(out, outSize, in, inSize, cmd)) {
				return true;
			}
		}

		return false;
	}

	public static void init() {
    	DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setIgnoringElementContentWhitespace(true);
		documentBuilderFactory.setIgnoringComments(true);
		documentBuilderFactory.setCoalescing(true);
		try {
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(PreDecrypt.class.getResourceAsStream("PreDecrypt.xml"));
			Element configuration = document.getDocumentElement();
			load(configuration);
		} catch (ParserConfigurationException e) {
			log.error(e);
		} catch (SAXException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}
	}

	private static String getContent(Node node) {
		if (node.hasChildNodes()) {
			return getContent(node.getChildNodes());
		}

		return node.getNodeValue();
	}

	private static String getContent(NodeList nodeList) {
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

	private static void load(Element configuration) {
		preDecrypts = new PreDecryptInfo[0];
		NodeList infos = configuration.getElementsByTagName("PreDecryptInfo");
		int n = infos.getLength();
		for (int i = 0; i < n; i++) {
			Element info = (Element) infos.item(i);
			loadInfo(info);
		}
	}

	private static byte[] parseBytes(String s) {
		byte[] bytes = null;

		for (int i = 0; i < s.length(); ) {
			i = s.indexOf("0x", i);
			if (i < 0) {
				break;
			}
			i += 2;

			int value = Integer.parseInt(s.substring(i, i + 2), 16);
			bytes = Utilities.add(bytes, (byte) value);
		}

		return bytes;
	}

	private static void loadInfo(Element info) {
		int cmd = Integer.parseInt(info.getAttribute("cmd"));
		String input = getContent(info.getElementsByTagName("Input"));
		String output = getContent(info.getElementsByTagName("Output"));

		addInfo(parseBytes(input), parseBytes(output), cmd);
	}

	private static void addInfo(byte[] input, byte[] output, int cmd) {
		PreDecryptInfo info = new PreDecryptInfo(input, output, cmd);
		PreDecryptInfo[] newPreDecrypts = new PreDecryptInfo[preDecrypts.length + 1];
		System.arraycopy(preDecrypts, 0, newPreDecrypts, 0, preDecrypts.length);
		newPreDecrypts[preDecrypts.length] = info;

		preDecrypts = newPreDecrypts;
	}
}
