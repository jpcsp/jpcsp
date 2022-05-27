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

import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isJavaIdentifierStart;
import static java.lang.Character.isWhitespace;
import static java.lang.Integer.parseInt;
import static jpcsp.util.Constants.charset;
import static jpcsp.util.Utilities.add;
import static libkirk.KirkEngine.KIRK_CMD_DECRYPT_SIGN;
import static libkirk.KirkEngine.KIRK_CMD_ENCRYPT_SIGN;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jpcsp.HLE.Modules;
import jpcsp.util.Utilities;

/**
 * List of values that can't be decrypted on Jpcsp due to missing keys.
 * They have been decrypted on a real PSP and we just reuse the result from the PSP here.
 */
public class PreDecrypt {
	public static Logger log = CryptoEngine.log;
	private static PreDecryptInfo preDecrypts[];
	private static int preDecryptIndex;

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
			if (input == null && inSize != 0) {
				return false;
			}
			if (output == null && outSize != 0) {
				return false;
			}
			if (input != null && output != null) {
				if (input.length != inSize || output.length != outSize) {
					return false;
				}
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
			s.append(String.format("\t<%s>", name));
			if (bytes != null) {
				for (int i = 0; i < bytes.length; i++) {
					if (i > 0) {
						s.append(", ");
					}
					if ((i % 16) == 0) {
						s.append("\n\t\t");
					}
					s.append(String.format("0x%02X", bytes[i]));
				}
			}
			s.append(String.format("\n\t</%s>\n", name));
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append(String.format("<PreDecryptInfo cmd=\"%d\">\n", cmd));
			toString(s, input, "Input");
			toString(s, output, "Output");
			s.append("</PreDecryptInfo>");

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
			Document document = documentBuilder.parse(PreDecrypt.class.getResourceAsStream("/PreDecrypt.xml"));
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

	private static int skipWhitespaces(String s, int i) {
		while (i < s.length() && isWhitespace(s.charAt(i))) {
			i++;
		}

		return i;
	}

	private static byte[] parseBytes(String s) {
		byte[] bytes = null;

		int length = s.length();
		for (int i = 0; i < length; ) {
			i = skipWhitespaces(s, i);

			// The value can be
			// - 0xNN: representing one byte in hexadecimal notation
			// - Java name: name of a Java static field, whose value will be converted to a byte array.
			if (s.startsWith("0x", i)) {
				i += 2;

				int value = parseInt(s.substring(i, i + 2), 16);
				i += 2;

				bytes = add(bytes, (byte) value);
			} else if (i < length && isJavaIdentifierStart(s.charAt(i))) {
				int startJavaName = i;
				i++;
				while (i < length) {
					char c = s.charAt(i);
					if (isJavaIdentifierPart(c)) {
						i++;
					} else if (c == '.') {
						i++;
						if (i < length) {
							c = s.charAt(i);
							if (isJavaIdentifierStart(c)) {
								i++;
							} else {
								break;
							}
						}
					} else {
						break;
					}
				}
				int endJavaName = i;
				String javaName = s.substring(startJavaName, endJavaName);

				int fieldNameIndex = javaName.lastIndexOf('.');
				if (fieldNameIndex > 0) {
					String className = javaName.substring(0, fieldNameIndex);
					String fieldName = javaName.substring(fieldNameIndex + 1);

					try {
						Class<?> classObject = Modules.class.getClassLoader().loadClass(className);
						Field fieldObject = classObject.getField(fieldName);
						// Retrieve the value of the static field
						Object fieldValue = fieldObject.get(null);
						// Convert the field value to a byte array
						byte[] fieldValueBytes;
						if (fieldValue instanceof byte[]) {
							fieldValueBytes = (byte[]) fieldValue;
						} else {
							fieldValueBytes = fieldValue.toString().getBytes(charset);
						}

						bytes = add(bytes, fieldValueBytes);
					} catch (ClassNotFoundException e) {
						log.error(e);
					} catch (NoSuchFieldException e) {
						log.error(e);
					} catch (SecurityException e) {
						log.error(e);
					} catch (IllegalArgumentException e) {
						log.error(e);
					} catch (IllegalAccessException e) {
						log.error(e);
					}
				}
			} else {
				break;
			}

			i = skipWhitespaces(s, i);

			if (i < length && s.charAt(i) == ',') {
				i++;
			}
		}

		return bytes;
	}

	private static void loadInfo(Element info) {
		int cmd = Integer.parseInt(info.getAttribute("cmd"));
		String input = getContent(info.getElementsByTagName("Input"));
		String output = getContent(info.getElementsByTagName("Output"));

		addInfo(parseBytes(input), parseBytes(output), cmd);
	}

	private static boolean isUseless(byte[] input, byte[] output, int cmd) {
		if (cmd == KIRK_CMD_ENCRYPT_SIGN || cmd == KIRK_CMD_DECRYPT_SIGN) {
			// Kirk commands 2 and 3 are unimplemented, do try to call them
			return false;
		}

		ByteBuffer inBuffer = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN);
		byte[] kirkOutput = new byte[output.length];
		ByteBuffer outBuffer = ByteBuffer.wrap(kirkOutput).order(ByteOrder.LITTLE_ENDIAN);

    	int inSizeAligned = Utilities.alignUp(input.length, 15);
    	CryptoEngine crypto = new CryptoEngine();
    	int result = crypto.getKIRKEngine().hleUtilsBufferCopyWithRange(outBuffer, output.length, inBuffer, inSizeAligned, input.length, cmd);

    	if (result != 0) {
    		// The KIRK engine cannot process the command
    		return false;
    	}

    	// The KIRK engine can process the command, verify that we have the same output
    	return Arrays.equals(kirkOutput, output);
	}

	private static void addInfo(byte[] input, byte[] output, int cmd) {
		preDecryptIndex++;

		PreDecryptInfo info = new PreDecryptInfo(input, output, cmd);
		for (int i = 0; i < preDecrypts.length; i++) {
			if (info.equals(preDecrypts[i])) {
				log.warn(String.format("PreDecrypt.xml: duplicate entry #%d:\n%s", preDecryptIndex, info));
				return;
			}
		}

		if (isUseless(input, output, cmd)) {
			log.warn(String.format("PreDecrypt.xml: useless entry #%d:\n%s", preDecryptIndex, info));
			return;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("PreDecrypt.xml: adding entry #%d:\n%s", preDecryptIndex, info));
		}

		PreDecryptInfo[] newPreDecrypts = new PreDecryptInfo[preDecrypts.length + 1];
		System.arraycopy(preDecrypts, 0, newPreDecrypts, 0, preDecrypts.length);
		newPreDecrypts[preDecrypts.length] = info;

		preDecrypts = newPreDecrypts;
	}
}
