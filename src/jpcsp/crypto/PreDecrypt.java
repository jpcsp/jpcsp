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
		private byte[] input;
		private byte[] output;
		private int cmd;

		public PreDecryptInfo(byte[] input, byte[] output, int cmd) {
			this.input = input;
			this.output = output;
			this.cmd = cmd;
		}

		public boolean decrypt(byte[] out, int outOffset, int outSize, byte[] in, int inOffset, int inSize, int cmd) {
			if (this.cmd != cmd) {
				return false;
			}
			if (input.length != inSize || output.length != outSize) {
				return false;
			}

			for (int i = 0; i < inSize; i++) {
				if (input[i] != in[inOffset + i]) {
					return false;
				}
			}

			if (out != null && outSize > 0) {
				System.arraycopy(output, 0, out, outOffset, outSize);
			}

			return true;
		}

		private static boolean equals(byte[] a, byte[] b) {
			if (a == null) {
				return b == null;
			}
			if (b == null) {
				return false;
			}
			if (a.length != b.length) {
				return false;
			}

			for (int i = 0; i < a.length; i++) {
				if (a[i] != b[i]) {
					return false;
				}
			}

			return true;
		}

		public boolean equals(PreDecryptInfo info) {
			if (cmd != info.cmd) {
				return false;
			}
			if (!equals(input, info.input)) {
				return false;
			}
			if (!equals(output, info.output)) {
				return false;
			}

			return true;
		}

		private static void toString(StringBuilder s, byte[] bytes, String name) {
			s.append(String.format(", %s=[", name));
			if (bytes != null) {
				for (int i = 0; i < bytes.length; i++) {
					if (i > 0) {
						s.append(", ");
					}
					s.append(String.format("0x%02X", bytes[i]));
				}
			}
			s.append("]");
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append(String.format("cmd=0x%X", cmd));
			toString(s, input, "Input");
			toString(s, output, "Output");

			return s.toString();
		}
	}

	public static boolean preDecrypt(byte[] out, int outOffset, int outSize, byte[] in, int inOffset, int inSize, int cmd) {
		for (PreDecryptInfo preDecrypt : preDecrypts) {
			if (preDecrypt.decrypt(out, outOffset, outSize, in, inOffset, inSize, cmd)) {
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
		for (int i = 0; i < preDecrypts.length; i++) {
			if (info.equals(preDecrypts[i])) {
				log.warn(String.format("PreDecrypt.xml: duplicate entry %s", info));
				return;
			}
		}

		PreDecryptInfo[] newPreDecrypts = new PreDecryptInfo[preDecrypts.length + 1];
		System.arraycopy(preDecrypts, 0, newPreDecrypts, 0, preDecrypts.length);
		newPreDecrypts[preDecrypts.length] = info;

		preDecrypts = newPreDecrypts;
	}
}
