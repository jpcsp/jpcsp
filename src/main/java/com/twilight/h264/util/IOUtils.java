package com.twilight.h264.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {
	
	public static byte[] toByteArray(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		byte[] buffer = new byte[4096];
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		} // while

		return output.toByteArray();
	}

	public static void closeQuietly(InputStream  input) {
		if( input == null ) {
			return;
		}
		try {
			input.close();
		} catch( IOException  ioe ) {
		}
	}

	public static void closeQuietly(OutputStream  output) {
		if( output == null ) {
			return;
		}
		try {
			output.close();
		} catch( IOException  ioe ) {
		}
	}
	
}
