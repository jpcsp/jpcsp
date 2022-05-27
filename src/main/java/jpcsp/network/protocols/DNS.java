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
package jpcsp.network.protocols;

import java.io.EOFException;

import jpcsp.util.Utilities;

public class DNS {
	// DNS packet format, see http://www.tcpipguide.com/free/t_DNSMessageHeaderandQuestionSectionFormat.htm
    public static final int DNS_RESOURCE_RECORD_CLASS_IN = 1; // "Internet"
    public static final int DNS_RESOURCE_RECORD_TYPE_A = 1; // Address (IPv4) record
    public static final int DNS_RESPONSE_CODE_NO_ERROR = 0;
    public static final int DNS_RESPONSE_CODE_NAME_ERROR = 3;
	public int identifier;
	public boolean isResponseFlag;
	public int opcode;
	public boolean authoritativeAnswer;
	public boolean truncationFlag;
	public boolean recursionDesired;
	public boolean recursionAvailable;
	public int zero;
	public int responseCode;
	public int questionCount;
	public int answerRecordCount;
	public int authorityRecordCount;
	public int additionalRecordCount;
	public DNSRecord questions[];
	public DNSAnswerRecord answerRecords[];
	public DNSAnswerRecord authorityRecords[];
	public DNSAnswerRecord additionalRecords[];

	public static class DNSRecord {
		// See format http://www.zytrax.com/books/dns/ch15/
		public String recordName;
		// List of types: https://en.wikipedia.org/wiki/List_of_DNS_record_types
		public int recordType;
		// Only record class: DNS_RESOURCE_RECORD_CLASS_IN
		public int recordClass;

		public void read(NetPacket packet) throws EOFException {
			recordName = packet.readDnsNameNotation();
			recordType = packet.read16();
			recordClass = packet.read16();
		}

		public NetPacket write(NetPacket packet) throws EOFException {
			packet.writeDnsNameNotation(recordName);
			packet.write16(recordType);
			packet.write16(recordClass);

			return packet;
		}

		public int sizeOf() {
			if (recordName == null || recordName.length() == 0) {
				return 5;
			}
			return recordName.length() + 6;
		}

		@Override
		public String toString() {
			return String.format("recordName='%s', recordType=0x%X, recordClass=0x%X", recordName, recordType, recordClass);
		}
	}

	public static class DNSAnswerRecord extends DNSRecord {
		// See format http://www.zytrax.com/books/dns/ch15/
		public int ttl;
		public int dataLength;
		public byte[] data;

		@Override
		public void read(NetPacket packet) throws EOFException {
			super.read(packet);
			ttl = packet.read32();
			dataLength = packet.read16();
			data = packet.readBytes(dataLength);
		}

		@Override
		public NetPacket write(NetPacket packet) throws EOFException {
			super.write(packet);
			packet.write32(ttl);
			packet.write16(dataLength);
			packet.writeBytes(data, 0, dataLength);

			return packet;
		}

		@Override
		public int sizeOf() {
			return super.sizeOf() + 6 + dataLength;
		}

		@Override
		public String toString() {
			return String.format("%s, ttl=0x%X, dataLength=0x%X, data=%s", super.toString(), ttl, dataLength, Utilities.getMemoryDump(data, 0, dataLength));
		}
	}

	public DNS() {
	}

	public DNS(DNS dns) {
		identifier = dns.identifier;
		isResponseFlag = dns.isResponseFlag;
		opcode = dns.opcode;
		authoritativeAnswer = dns.authoritativeAnswer;
		truncationFlag = dns.truncationFlag;
		recursionDesired = dns.recursionDesired;
		recursionAvailable = dns.recursionAvailable;
		zero = dns.zero;
		responseCode = dns.responseCode;
		questionCount = dns.questionCount;
		answerRecordCount = dns.answerRecordCount;
		authorityRecordCount = dns.authorityRecordCount;
		additionalRecordCount = dns.additionalRecordCount;
		questions = dns.questions;
		answerRecords = dns.answerRecords;
		authorityRecords = dns.authorityRecords;
		additionalRecords = dns.additionalRecords;
	}

	private DNSRecord[] readRecords(NetPacket packet, int count) throws EOFException {
		DNSRecord records[] = new DNSRecord[count];
		for (int i = 0; i < count; i++) {
			records[i] = new DNSRecord();
			records[i].read(packet);
		}

		return records;
	}

	private DNSAnswerRecord[] readAnswerRecords(NetPacket packet, int count) throws EOFException {
		DNSAnswerRecord records[] = new DNSAnswerRecord[count];
		for (int i = 0; i < count; i++) {
			records[i] = new DNSAnswerRecord();
			records[i].read(packet);
		}

		return records;
	}

	public void read(NetPacket packet) throws EOFException {
		identifier = packet.read16();
		isResponseFlag = packet.readBoolean();
		opcode = packet.readBits(4);
		authoritativeAnswer = packet.readBoolean();
		truncationFlag = packet.readBoolean();
		recursionDesired = packet.readBoolean();
		recursionAvailable = packet.readBoolean();
		zero = packet.readBits(3);
		responseCode = packet.readBits(4);
		questionCount = packet.read16();
		answerRecordCount = packet.read16();
		authorityRecordCount = packet.read16();
		additionalRecordCount = packet.read16();
		questions = readRecords(packet, questionCount);
		answerRecords = readAnswerRecords(packet, answerRecordCount);
		authorityRecords = readAnswerRecords(packet, authorityRecordCount);
		additionalRecords = readAnswerRecords(packet, additionalRecordCount);
	}

	private void writeRecords(NetPacket packet, int recordsCount, DNSRecord records[]) throws EOFException {
		for (int i = 0; i < recordsCount; i++) {
			records[i].write(packet);
		}
	}

	public NetPacket write(NetPacket packet) throws EOFException {
		packet.write16(identifier);
		packet.writeBoolean(isResponseFlag);
		packet.writeBits(opcode, 4);
		packet.writeBoolean(authoritativeAnswer);
		packet.writeBoolean(truncationFlag);
		packet.writeBoolean(recursionDesired);
		packet.writeBoolean(recursionAvailable);
		packet.writeBits(zero, 3);
		packet.writeBits(responseCode, 4);
		packet.write16(questionCount);
		packet.write16(answerRecordCount);
		packet.write16(authorityRecordCount);
		packet.write16(additionalRecordCount);
		writeRecords(packet, questionCount, questions);
		writeRecords(packet, answerRecordCount, answerRecords);
		writeRecords(packet, authorityRecordCount, authorityRecords);
		writeRecords(packet, additionalRecordCount, additionalRecords);

		return packet;
	}

	public NetPacket write() throws EOFException {
		return write(new NetPacket(sizeOf()));
	}

	private int sizeOf(int recordsCount, DNSRecord records[]) {
		int size = 0;
		for (int i = 0; i < recordsCount; i++) {
			size += records[i].sizeOf();
		}

		return size;
	}

	public int sizeOf() {
		int size = 12;
		size += sizeOf(questionCount, questions);
		size += sizeOf(answerRecordCount, answerRecords);
		size += sizeOf(authorityRecordCount, authorityRecords);
		size += sizeOf(additionalRecordCount, additionalRecords);

		return size;
	}

	private void toString(StringBuilder s, String prefix, int recordsCount, DNSRecord records[]) {
		for (int i = 0; i < recordsCount; i++) {
			s.append(String.format(", %s#%d[%s]", prefix, i, records[i]));
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();

		s.append(String.format("identifier=0x%04X, isResponseFlag=%b, opcode=0x%X, authoritativeAnswer=%b, truncationFlag=%b, recursionDesired=%b, recursionAvailable=%b, zero=0x%X, responseCode=0x%X, questionCount=%d, answerRecordCount=%d, authorityRecordCount=%d, additionalRecordCount=%d", identifier, isResponseFlag, opcode, authoritativeAnswer, truncationFlag, recursionDesired, recursionAvailable, zero, responseCode, questionCount, answerRecordCount, authorityRecordCount, additionalRecordCount));
		toString(s, "question", questionCount, questions);
		toString(s, "answerRecord", answerRecordCount, answerRecords);
		toString(s, "authorityRecord", authorityRecordCount, authorityRecords);
		toString(s, "additionalRecord", additionalRecordCount, additionalRecords);

		return s.toString();
	}
}
