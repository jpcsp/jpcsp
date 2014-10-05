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
package jpcsp.test;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.modules.sceAudiocodec;
import jpcsp.media.codec.CodecFactory;
import jpcsp.media.codec.ICodec;
import jpcsp.util.Utilities;

import com.twilight.h264.decoder.AVFrame;
import com.twilight.h264.decoder.AVPacket;
import com.twilight.h264.decoder.H264Context;
import com.twilight.h264.decoder.H264Decoder;
import com.twilight.h264.decoder.MpegEncContext;
import com.twilight.h264.player.PlayerFrame;
import com.twilight.h264.util.FrameUtils;

public class PSMFPlayer implements Runnable {
	private static Logger log = Logger.getLogger("PSMFPlayer");

	private PlayerFrame displayPanel;
	private String fileName;
	private int[] buffer = null;
	private int[] videoData = new int[0x10000];
	private int[] audioData = new int[0x10000];
	private int videoDataOffset;
	private int audioDataOffset;
	private int audioFrameLength;
    private final int frameHeader[] = new int[8];
    private int frameHeaderLength;
	private InputStream is;

	public static void main(String[] args) {
		new PSMFPlayer(args);
	}

	public PSMFPlayer(String[] args) {
		if(args.length<1) {
			System.out.println("Usage: java jpcsp.PSMFPlayer <.pmf file>\n");
			return;
		}

		JFrame frame = new JFrame("Player");
		displayPanel = new PlayerFrame();

		frame.getContentPane().add(displayPanel, BorderLayout.CENTER);

		// Finish setting up the frame, and show it.
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		displayPanel.setVisible(true);
		// Standard PSP screen dimensions
		displayPanel.setPreferredSize(new Dimension(480, 272));
		frame.pack();
		frame.setVisible(true);
		
		fileName = args[0];

		new Thread(this).start();
	}
	
	@Override
	public void run() {
		System.out.println("Playing "+ fileName);
		playFile(fileName);		
	}

	private int read8(InputStream is) {
		try {
			return is.read();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return -1;
	}

	private int read16(InputStream is) {
		return (read8(is) << 8) | read8(is);
	}

	private int read32(InputStream is) {
		return (read8(is) << 24) | (read8(is) << 16) | (read8(is) << 8) | read8(is);
	}

	private void skip(InputStream is, int n) {
		if (n > 0) {
			try {
				is.skip(n);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private int skipPesHeader(InputStream is, int startCode) {
		int pesLength = 0;
		int c = read8(is);
		pesLength++;
		while (c == 0xFF) {
			c = read8(is);
			pesLength++;
		}

		if ((c & 0xC0) == 0x40) {
			skip(is, 1);
			c = read8(is);
			pesLength += 2;
		}

		if ((c & 0xE0) == 0x20) {
			skip(is, 4);
			pesLength += 4;
			if ((c & 0x10) != 0) {
				skip(is, 5);
				pesLength += 5;
			}
		} else if ((c & 0xC0) == 0x80) {
			skip(is, 1);
			int headerLength = read8(is);
			pesLength += 2;
			skip(is, headerLength);
			pesLength += headerLength;
		}

		if (startCode == 0x1BD) { // PRIVATE_STREAM_1
			int channel = read8(is);
			pesLength++;
			if (channel >= 0x80 && channel <= 0xCF) {
				skip(is, 3);
				pesLength += 3;
				if (channel >= 0xB0 && channel <= 0xBF) {
					skip(is, 1);
					pesLength++;
				}
			} else {
				skip(is, 3);
				pesLength += 3;
			}
		}

		return pesLength;
	}

	private void addVideoData(InputStream is, int length) {
		if (videoDataOffset + length > videoData.length) {
			// Extend the inputBuffer
			int[] newInputBuffer = new int[videoDataOffset + length];
			System.arraycopy(videoData, 0, newInputBuffer, 0, videoDataOffset);
			videoData = newInputBuffer;
		}

		for (int i = 0; i < length; i++) {
			videoData[videoDataOffset++] = read8(is);
		}
	}

	private void addAudioData(InputStream is, int length) {
		if (audioDataOffset + length > audioData.length) {
			// Extend the inputBuffer
			int[] newInputBuffer = new int[audioDataOffset + length];
			System.arraycopy(audioData, 0, newInputBuffer, 0, audioDataOffset);
			audioData = newInputBuffer;
		}

		while (length > 0) {
    		int currentFrameLength = audioFrameLength == 0 ? 0 : audioDataOffset % audioFrameLength;
    		if (currentFrameLength == 0) {
    			// 8 bytes header:
    			// - byte 0: 0x0F
    			// - byte 1: 0xD0
    			// - byte 2: 0x28
    			// - byte 3: (frameLength - 8) / 8
    			// - bytes 4-7: 0x00
    			while (frameHeaderLength < frameHeader.length && length > 0) {
    				frameHeader[frameHeaderLength++] = read8(is);
    				length--;
    			}
    			if (frameHeaderLength < frameHeader.length) {
    				// Frame header not yet complete
    				break;
    			}
    			if (length == 0) {
    				// Frame header is complete but no data is following the header.
    				// Retry when some data is available
    				break;
    			}

    			int frameHeader23 = (frameHeader[2] << 8) | frameHeader[3];
    			audioFrameLength = ((frameHeader23 & 0x3FF) << 3) + 8;
    			if (frameHeader[0] != 0x0F || frameHeader[1] != 0xD0) {
					log.warn(String.format("Audio frame length 0x%X with incorrect header (header: %02X %02X %02X %02X %02X %02X %02X %02X)", audioFrameLength, frameHeader[0], frameHeader[1], frameHeader[2], frameHeader[3], frameHeader[4], frameHeader[5], frameHeader[6], frameHeader[7]));
    			}

    			frameHeaderLength = 0;
    		}

    		int lengthToNextFrame = audioFrameLength - currentFrameLength;
    		int readLength = Utilities.min(length, lengthToNextFrame);
    		for (int i = 0; i < readLength; i++) {
    			audioData[audioDataOffset++] = read8(is);
    		}
    		length -= readLength;
		}
	}

	private void readPsmfHeader(File f) {
		try {
			is = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		if (read32(is) != 0x50534D46) { // PSMF
			return;
		}

		skip(is, 4);
		int mpegOffset = read32(is);
		skip(is, mpegOffset - 12);
	}

	private boolean readPsmfPacket(int videoChannel, int audioChannel) {
		while (true) {
			int startCode = read32(is);
			if (startCode == -1) {
				// End of file
				return false;
			}
			int codeLength;
			switch (startCode) {
				case 0x1BA: // PACK_START_CODE
					skip(is, 10);
					break;
				case 0x1BB: // SYSTEM_HEADER_START_CODE
					skip(is, 14);
					break;
				case 0x1BE: // PADDING_STREAM
				case 0x1BF: // PRIVATE_STREAM_2
					codeLength = read16(is);
					skip(is, codeLength);
					break;
				case 0x1BD: { // PRIVATE_STREAM_1
					// Audio stream
					codeLength = read16(is);
					int pesLength = skipPesHeader(is, startCode);
					codeLength -= pesLength;
					addAudioData(is, codeLength);
					break;
				}
				case 0x1E0: case 0x1E1: case 0x1E2: case 0x1E3: // Video streams
				case 0x1E4: case 0x1E5: case 0x1E6: case 0x1E7:
				case 0x1E8: case 0x1E9: case 0x1EA: case 0x1EB:
				case 0x1EC: case 0x1ED: case 0x1EE: case 0x1EF:
					codeLength = read16(is);
					if (videoChannel < 0 || startCode - 0x1E0 == videoChannel) {
						int pesLength = skipPesHeader(is, startCode);
						codeLength -= pesLength;
						addVideoData(is, codeLength);
						return true;
					}
					skip(is, codeLength);
					break;
			}
		}
	}

	private void consumeVideoData(int length) {
		if (length >= videoDataOffset) {
			videoDataOffset = 0;
		} else {
			System.arraycopy(videoData, length, videoData, 0, videoDataOffset - length);
			videoDataOffset -= length;
		}
	}

	private void consumeAudioData(int length) {
		if (length >= audioDataOffset) {
			audioDataOffset = 0;
		} else {
			System.arraycopy(audioData, length, audioData, 0, audioDataOffset - length);
			audioDataOffset -= length;
		}
	}

	private int findFrameEnd() {
		for (int i = 5; i < videoDataOffset; i++) {
			if (videoData[i - 4] == 0x00 &&
			    videoData[i - 3] == 0x00 &&
			    videoData[i - 2] == 0x00 &&
			    videoData[i - 1] == 0x01) {
				int naluType = videoData[i] & 0x1F;
				if (naluType == H264Context.NAL_AUD) {
					return i - 4;
				}
			}
		}

		return -1;
	}

	public boolean playFile(String filename) {
        DOMConfigurator.configure("LogSettings.xml");

        H264Decoder codec;
	    MpegEncContext c= null;
	    int[] got_picture = new int[1];
	    File f = new File(filename);
	    AVFrame picture;
	    AVPacket avpkt = new AVPacket();

	    avpkt.av_init_packet();

	    System.out.println("Video decoding\n");

	    /* find the mpeg1 video decoder */
	    codec = new H264Decoder();
	    if (codec == null) {
	    	System.out.println("codec not found\n");
	        System.exit(1);
	    }

	    c = MpegEncContext.avcodec_alloc_context();
	    picture = AVFrame.avcodec_alloc_frame();

	    if ((codec.capabilities & H264Decoder.CODEC_CAP_TRUNCATED) != 0) {
	        c.flags |= MpegEncContext.CODEC_FLAG_TRUNCATED; /* we do not send complete frames */
	    }

	    /* For some codecs, such as msmpeg4 and mpeg4, width and height
	       MUST be initialized there because this information is not
	       available in the bitstream. */

	    /* open it */
	    if (c.avcodec_open(codec) < 0) {
	    	System.out.println("could not open codec\n");
	        System.exit(1);
	    }

	    try {
	    	readPsmfHeader(f);
	    	int videoChannel = 0;
	    	int audioChannel = 0;
	    	int audioChannels = 2;
		    int frame = 0;

			Memory mem = Memory.getInstance();
			int audioInputAddr = MemoryMap.START_USERSPACE + 0x10000;
			int audioOutputAddr = MemoryMap.START_USERSPACE;
		    ICodec audioCodec = CodecFactory.getCodec(sceAudiocodec.PSP_CODEC_AT3PLUS);
		    boolean audioCodecInit = false;
		    byte[] audioOutputData = null;
	        AudioFormat audioFormat = new AudioFormat(44100,
	                16,
	                audioChannels,
	                true,
	                false);
	        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine mLine = (SourceDataLine) AudioSystem.getLine(info);
            mLine.open(audioFormat);
            log.info(String.format("Audio line buffer size %d", mLine.getBufferSize()));
            mLine.start();

            long startTime = System.currentTimeMillis();

	    	while (true) {
			    int frameSize = -1;
			    do {
			    	if (!readPsmfPacket(videoChannel, audioChannel)) {
			    		if (videoDataOffset <= 0) {
			    			// Enf of file reached
			    			break;
			    		}
			    		frameSize = findFrameEnd();
			    		if (frameSize < 0) {
				    		// Process pending last frame
			    			frameSize = videoDataOffset;
			    		}
			    	} else {
			    		frameSize = findFrameEnd();
			    	}
			    } while (frameSize <= 0);

			    if (frameSize <= 0) {
			    	break;
			    }
			    avpkt.data_base = videoData;
		        avpkt.size = frameSize;
		        avpkt.data_offset = 0;

		        while (avpkt.size > 0) {
		            int len = c.avcodec_decode_video2(picture, got_picture, avpkt);
		            if (len < 0) {
		                log.error("Error while decoding frame "+ frame);
		                // Discard current packet and proceed to next packet
		                break;
		            } // if
		            if (got_picture[0] != 0) {
		            	picture = c.priv_data.displayPicture;

		            	int imageWidth = picture.imageWidthWOEdge;
		            	int imageHeight = picture.imageHeightWOEdge;
						int bufferSize = imageWidth * imageHeight;
						if (buffer == null || bufferSize != buffer.length) {
							buffer = new int[bufferSize];
						}
						FrameUtils.YUV2RGB_WOEdge(picture, buffer);
						displayPanel.lastFrame = displayPanel.createImage(new MemoryImageSource(imageWidth
								, imageHeight, buffer, 0, imageWidth));
						displayPanel.invalidate();
						displayPanel.updateUI();

						frame++;

					    long now = System.currentTimeMillis();
					    long currentDuration = now - startTime;
					    long videoDuration = frame * 100000L / 3003L;
					    if (currentDuration < videoDuration) {
					    	Thread.sleep(videoDuration - currentDuration);
					    }

					    now = System.currentTimeMillis();
					    log.info(String.format("FPS %f", 1000f * frame / (now - startTime)));
		            }
		            avpkt.size -= len;
		            avpkt.data_offset += len;
		        }

		        consumeVideoData(frameSize);

		        if (audioOutputData != null) {
		        	if (mLine.available() >= audioOutputData.length) {
		        		mLine.write(audioOutputData, 0, audioOutputData.length);
		        		audioOutputData = null;
		        	}
		        } else if (audioFrameLength > 0) {
		        	if (!audioCodecInit) {
		        		audioCodec.init(audioFrameLength, audioChannels, audioChannels, 0);
		        		audioCodecInit = true;
		        	}
		        	while (audioDataOffset >= audioFrameLength) {
		        		for (int i = 0; i < audioFrameLength; i++) {
		        			mem.write8(audioInputAddr + i, (byte) audioData[i]);
		        		}
		        		int result = audioCodec.decode(audioInputAddr, audioFrameLength, audioOutputAddr);
		        		if (result < 0) {
		        			log.error(String.format("Audio decode error 0x%08X", result));
		        			break;
		        		} else if (result == 0) {
		        			break;
		        		} else if (result > 0) {
		        			consumeAudioData(audioFrameLength);

		        			audioOutputData = new byte[audioCodec.getNumberOfSamples() * 2 * audioChannels];
		        			for (int i = 0; i < audioOutputData.length; i++) {
		        				audioOutputData[i] = (byte) mem.read8(audioOutputAddr + i);
		        			}

		        			if (mLine.available() < audioOutputData.length) {
				        		break;
				        	}
			        		mLine.write(audioOutputData, 0, audioOutputData.length);
			        		audioOutputData = null;
		        		}
		        	}
		        }
			} // while

	    	mLine.drain();
	    	mLine.close();
	    } catch(Exception e) {
	    	e.printStackTrace();
	    } finally {
	    	try { is.close(); } catch(Exception ee) {}
	    } // try

	    c.avcodec_close();
	    c = null;
	    picture = null;
	    System.out.println("Stop playing video.");

	    return true;
	}
}
