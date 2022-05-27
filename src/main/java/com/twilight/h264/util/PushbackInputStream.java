package com.twilight.h264.util;

import java.io.IOException;
import java.io.InputStream;

public class PushbackInputStream extends FilterInputStream {

	protected byte[] buf;
	protected int pos;

	public PushbackInputStream(InputStream is) {
		super(is);
		buf = (is == null)?null:new byte[1];
		pos = 1;
	}
	
	public PushbackInputStream(InputStream is, int size) {
		super(is);
		buf = (is == null)?null:new byte[size];
		pos = size;
	}
	
	public int available() throws IOException {
		if(buf == null)
			throw new IOException();
		return buf.length - pos + inputStream.available();		
	}
	
	public void close() throws IOException {
		if(inputStream != null) {
			inputStream.close();
			inputStream = null;
			buf = null;
		} // if	
	}
	
	public boolean markSupported() { return false; }
	
	public int read() throws IOException {
		if(buf==null)
			throw new IOException();
		if(pos < buf.length)
			return (buf[pos++] & 0xff);
		return inputStream.read();
	}
	
	public int read(byte[] buffer, int offset, int len) throws IOException {
		if(buf==null)
			throw new IOException();
		int copiedBytes = 0;
		int copyLength = 0;
		int newOffset = offset;
		if(pos < buf.length) {
			copyLength = (buf.length - pos >= len)?len:buf.length - pos;
			System.arraycopy(buf, pos, buffer, newOffset, copyLength);
			newOffset += copyLength;
			copiedBytes += copyLength;
			pos += copyLength;
		} // if
		if(copyLength == len) {
			return len;
		} // if
		int inCopied = inputStream.read(buffer,newOffset,len - copiedBytes);
		if(inCopied > 0)
			return inCopied + copiedBytes;
		if(copiedBytes == 0)
			return inCopied;
		return copiedBytes;
	}
	
	public long skip(long count) throws IOException {
		if(inputStream==null)
			throw new IOException();
		if(count <= 0) return 0;
		int numSkipped = 0;
		if(pos < buf.length) {
			numSkipped += (count < buf.length - pos)?count:buf.length - pos;
			pos += numSkipped;
		} // if
		if(numSkipped < count)
			numSkipped += inputStream.skip(count - numSkipped);
		return numSkipped;	
	}
	
	public void unread(byte[] buffer) throws IOException {
		unread(buffer,0,buffer.length);
	}
	
	public void unread(byte[] buffer, int offset, int length) throws IOException {
		if(length > pos)
			throw new IOException();
		System.arraycopy(buffer, offset, buf, pos - length, length);
		pos =  pos - length;
	}
	
	public void unread(int oneByte) throws IOException {
		if(buf == null)
			throw new IOException();
		buf[--pos] = (byte)oneByte;
	}
	
	public void mark(int limit) { return; } // Not Support.
	public void reset() throws IOException { // Not Support.
		throw new IOException();
	}
	
		
}
