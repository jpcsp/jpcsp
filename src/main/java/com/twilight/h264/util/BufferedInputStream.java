package com.twilight.h264.util;

import java.io.IOException;
import java.io.InputStream;

public class BufferedInputStream extends FilterInputStream{
	protected volatile byte[] buf;
	protected int count;
	protected int markLimit;
	protected int markPos = -1;
	protected int pos;

	public BufferedInputStream(InputStream is) {
		super(is);
		buf = new byte[8912];
	}

	public BufferedInputStream(InputStream is, int size) {
		super(is);
		buf = new byte[size];
	}
	
	public synchronized int available() throws IOException {
		InputStream local = inputStream;
		return count - pos + local.available();
	}

	public void close() throws IOException {
		InputStream local = inputStream;
		buf = null;
		inputStream = null;
		if(local != null)
			local.close();
	}
	
	private int fillBuf(InputStream localIn, byte[] localBuf) throws IOException {
		if(markPos == -1 || (pos - markPos >= markLimit)) {
			int result = localIn.read(localBuf);
			if(result > 0) {
				markPos = -1;
				pos = 0;
				count = result == -1?0:result;
			} // if
			return result;
		} // if
		if(markPos == 0 && markLimit > localBuf.length) {
			int newLength = localBuf.length * 2;
			if(newLength > markLimit)
				newLength = markLimit;
			byte[] newBuf = new byte[newLength];
			System.arraycopy(localBuf, 0, newBuf, 0, localBuf.length);
			localBuf = buf = newBuf;
		} else if(markPos > 0) {
			System.arraycopy(localBuf, markPos, localBuf, 0, localBuf.length - markPos);
		} // if
		pos -= markPos;
		count = markPos = 0;
		int bytesRead = localIn.read(localBuf,pos,localBuf.length - pos);
		count = bytesRead <= 0? pos: pos + bytesRead;
		return bytesRead;
	}
	
	public synchronized void mark(int readLimit) {
		markLimit = readLimit;
		markPos = pos;
	}
	
	public boolean markSupported() { return true; }
	
	public synchronized int read() throws IOException {
		byte[] localBuf = buf;
		InputStream localIn = inputStream;
		if(pos >= count && fillBuf(localIn, localBuf) == -1) {
			return -1;
		} // if
		if(localBuf != buf) {
			localBuf = buf;
			if(localBuf == null) throw new IOException();
		}
		if(count - pos > 0)
			return localBuf[pos++] & 0xff;
		return -1;
	}
	
	public synchronized int read(byte[] buffer,int offset,int length) throws IOException {
		byte[] localBuf = buf;
		if(localBuf==null) throw new IOException();
		if(length == 0) return 0;
		InputStream localIn = inputStream;
		if(localIn==null) throw new IOException();
		int required;
		if(pos < count) {
			 int copyLength = count - pos >= length?length: count - pos;
			 System.arraycopy(localBuf, pos, buffer, offset, copyLength);
			 pos += copyLength;
			 if(copyLength == length || localIn.available() == 0)
				 return copyLength;
			 offset += copyLength;
			 required = length - copyLength;
		} else {
			required = length;
		} // if

		while(true) {
			int read;
			if(markPos == -1 && required > localBuf.length) {
				read = localIn.read(buffer, offset, required);
				if(read == -1)
					return required == length?-1:length - required;
			} else {
				if(fillBuf(localIn, localBuf) == -1)
					return required == length?-1:length - required;
				if(localBuf != buf) {
					localBuf = buf;
					if(localBuf == null)
						throw new IOException();
				} // if
				read = count - pos >= required? required: count - pos;
				System.arraycopy(localBuf, pos, buffer, offset, read);
				pos += read;
			} // if
			required -= read;
			if(required == 0)
				return length;
			if(localIn.available() == 0)
				return length - required;
			offset += read;
		} // while
	}
	
	public synchronized void reset() throws IOException {
		if(buf == null) throw new IOException();
		if(-1 == markPos)
			throw new IOException();
		pos = markPos;
	}
	
	public synchronized long skip(long amount) throws IOException {
		byte[] localBuf = buf;
		InputStream localIn = inputStream;
		if(localBuf == null)
			throw new IOException();
		if(amount < 1) return 0;
		if(localIn == null)
			throw new IOException();
		if(count - pos >= amount) {
			pos += amount;
			return amount;
		} // if
		long read = count - pos;
		pos = count;
		if(markPos != -1) {
			if(amount <= markLimit) {
				if(fillBuf(localIn, localBuf) == -1)
					return read;
				if(count - pos >= amount - read) {
					pos += amount - read;
					return amount;
				} // if
				read += (count - pos);
				pos = count;
				return read;
			} // if
		} // if
		return read + localIn.skip(amount - read);
	}
	
}
