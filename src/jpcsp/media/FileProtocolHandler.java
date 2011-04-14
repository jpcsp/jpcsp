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
package jpcsp.media;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.log4j.Logger;

import com.xuggle.xuggler.io.IURLProtocolHandler;

public class FileProtocolHandler implements IURLProtocolHandler {
	private Logger log = MediaEngine.log;
	private File file;
	private RandomAccessFile stream;

	public FileProtocolHandler(String fileName) {
		file = new File(fileName);
	}

	@Override
	public boolean isStreamed(String url, int flags) {
		return false;
	}

	@Override
	public int close() {
		try {
			stream.close();
			stream = null;
		} catch (IOException e) {
			log.error(e);
		}
		return 0;
	}

	@Override
	public int open(String url, int flags) {
		if (stream != null) {
			close();
		}

		if (file == null) {
			file = new File(url);
		}

		final String mode;
		switch (flags) {
			case URL_RDONLY_MODE:
				mode = "r";
				break;
			case URL_RDWR:
				mode = "rw";
				break;
			case URL_WRONLY_MODE:
				mode = "w";
				break;
			default:
				log.error(String.format("Unknown open flag %d", flags));
				return -1;
		}

		try {
			stream = new RandomAccessFile(file, mode);
		} catch (FileNotFoundException e) {
			log.error(e);
			return -1;
		}

		return 0;
	}

	@Override
	public int read(byte[] buf, int size) {
		try {
			return stream.read(buf, 0, size);
		} catch (IOException e) {
			log.error(e);
			return -1;
		}
	}

	@Override
	public long seek(long offset, int whence) {
		try {
			final long seek;
			switch (whence) {
				case SEEK_SET:
					seek = offset;
					break;
				case SEEK_CUR:
					seek = stream.getFilePointer() + offset;
					break;
				case SEEK_END:
					seek = stream.length() + offset;
					break;
				case SEEK_SIZE:
					return stream.length();
				default:
					log.error(String.format("Unknown seek whence %d", whence));
					return -1;
			}
			stream.seek(seek);
			return seek;
		} catch (IOException e) {
			log.error(e);
			return -1;
		}
	}

	@Override
	public int write(byte[] buf, int size) {
		try {
			stream.write(buf, 0, size);
			return size;
		} catch (IOException e) {
			log.error(e);
			return -1;
		}
	}
}
