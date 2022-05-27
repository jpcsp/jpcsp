package com.twilight.h264.util;

import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream extends InputStream {
	protected volatile InputStream inputStream;
	private int count;
	private long byteCount;
	
	public CountingInputStream(InputStream is) {
		inputStream = is;
	}
	
	public long getByteCount() {
		return byteCount;
	}
	
	public int getCount() {
		return count;
	}

	public long resetByteCount() {
		byteCount = 0;
		return byteCount;
	}

	public int resetCount() {
		count = 0;
		return count;
	}
	
	public long skip(long length) throws IOException {
		long ret = inputStream.skip(length);
		byteCount += length;
		count += length;
		return ret;
	}

	////
	public int read() throws IOException {
		// TODO Auto-generated method stub
		int ret = inputStream.read();
		count++;
		byteCount++;
		return ret;
	}
	
	public int read(byte b[]) throws IOException  {
		int cnt =  inputStream.read(b, 0, b.length);
		count += cnt;
		byteCount += cnt;
		return cnt;
	}
 
     public int read(byte b[], int off, int len) throws IOException  {
    	int cnt = inputStream.read(b, off, len);
 		count += cnt;
		byteCount += cnt;
		return cnt;
     }
  
     public int available() throws IOException  {
    	 return inputStream.available();
     }
 
     public void close() throws IOException  {
    	 inputStream.close();
     }
 
     public synchronized void mark(int readlimit) {
    	 inputStream.mark(readlimit);
     }
 
     public synchronized void reset() throws IOException  {
    	 inputStream.reset();
     }
 
     public boolean markSupported() {
    	 return inputStream.markSupported();
     }

}
