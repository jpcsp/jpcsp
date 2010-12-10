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
package jpcsp.GUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import jpcsp.Emulator;
import jpcsp.Settings;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.HLE.Modules;

import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class UmdVideoPlayer extends JFrame implements KeyListener {
	private UmdIsoReader iso;
	private UmdIsoFile isoFile;
    private HashMap<Integer, MpsStreamInfo> mpsStreamMap;
    private int currentStreamIndex;
    private int screenWidth;
    private int screenHeigth;
	private String fileName;
	private IContainer container;
	private IVideoResampler resampler;
    private int videoStreamId;
    private IStreamCoder videoCoder;
    private int audioStreamId;
    private IStreamCoder audioCoder;
    private IPacket packet;
    private long firstTimestampInStream;
    private long systemClockStartTime;
    private IConverter converter;
    private BufferedImage image;
    private boolean done;
    private boolean endOfVideo;
    private boolean threadExit;
    private JLabel display;
    private MpsDisplayThread displayThread;
    private MpsByteChannel byteChannel;
    private SourceDataLine mLine;

    protected class MpsStreamInfo {
        private String streamName;
        private int streamWidth;
        private int streamHeigth;

        public MpsStreamInfo(String name, int width, int heigth) {
            streamName = name;
            streamWidth = width;
            streamHeigth = heigth;
        }

        public String getName() {
            return streamName;
        }

        public int getWidth() {
            return streamWidth;
        }

        public int getHeigth() {
            return streamHeigth;
        }
    }

    public UmdVideoPlayer(UmdIsoReader iso) {
        super("UMD Video Player");
        screenWidth = 480;
        screenHeigth = 272;
        this.iso = iso;
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        super.addKeyListener(this);
        super.setResizable(false);
        super.setPreferredSize(new Dimension(screenWidth, screenHeigth));
        display = new JLabel();
        super.getContentPane().add(display, BorderLayout.CENTER);
        super.pack();
        super.setVisible(true);
        init();
	}

    @Override
    public void keyPressed(KeyEvent keyCode) {
        if(keyCode.getKeyCode() == KeyEvent.VK_RIGHT) {
            goToNextMpsStream();
        } else if ((keyCode.getKeyCode() == KeyEvent.VK_LEFT) && (currentStreamIndex > 0)) {
            goToPreviousMpsStream();
        }
    }

    @Override
    public void keyReleased(KeyEvent keyCode) {
    }

    @Override
    public void keyTyped(KeyEvent keyCode) {
    }

	private void init() {
		image = null;
		done = false;
		threadExit = false;
		isoFile = null;
        mpsStreamMap = new HashMap<Integer, MpsStreamInfo>();
        currentStreamIndex = 0;
        addStreamFromPlaylistFile(currentStreamIndex);
        Modules.log.info("Setting aspect ratio to 16:9.");
        if (mpsStreamMap.containsKey(currentStreamIndex)) {
            MpsStreamInfo info = mpsStreamMap.get(currentStreamIndex);
            fileName = "UMD_VIDEO/STREAM/" + info.getName() + ".MPS";
            Modules.log.info("Loading stream:" + fileName);
            try {
                isoFile = iso.getFile(fileName);
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
                Emulator.log.error(e);
            }
        }
	}

    public void addStreamFromPlaylistFile(int index) {
        try {
            UmdIsoFile file = iso.getFile("UMD_VIDEO/PLAYLIST.UMD");
            if (file.readInt() != 0x56444D55) { // UMDV
                Modules.log.warn("Accessing invalid PLAYLIST.UMD file!");
            }
            int umdvVersion = file.readInt();
            int globalDataOffset = (file.readUnsignedByte() << 24) | (file.readUnsignedByte() << 16)
                    | (file.readUnsignedByte() << 8) | file.readUnsignedByte(); // Endian swap.
            int currentOffset = (globalDataOffset + 0x310) + (index * 0x360);   // Each stream has 0x360 bytes in the playlist.
            file.seek(currentOffset);  // Skip the global data.
            file.seek(currentOffset + 9);
            int streamHeigth = (int) (file.readByte() * 0x10); // Find the stream's original heigth.
            file.seek(currentOffset + 17);
            int streamWidth = (int) (file.readByte() * 0x10); // Find the stream's original width.
            file.seek(currentOffset + 20);
            byte[] stringBuf = new byte[5];  // Name tags only have 5 digits.
            file.read(stringBuf, 0, 5);
            String streamName = new String(stringBuf);   // Find the stream's name.

            MpsStreamInfo info = new MpsStreamInfo(streamName, streamWidth, streamHeigth);
            mpsStreamMap.put(index, info);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goToNextMpsStream() {
        closeVideo();
        closeAudio();
        addStreamFromPlaylistFile(++currentStreamIndex);
        if (mpsStreamMap.containsKey(currentStreamIndex)) {
            MpsStreamInfo info = mpsStreamMap.get(currentStreamIndex);
            fileName = "UMD_VIDEO/STREAM/" + info.getName() + ".MPS";
            Modules.log.info("Loading stream:" + fileName);
            try {
                isoFile = iso.getFile(fileName);
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
			Emulator.log.error(e);
            }
        }
        if(isoFile != null) {
            startVideo();
        }
    }

    private void goToPreviousMpsStream() {
        closeVideo();
        closeAudio();
        addStreamFromPlaylistFile(--currentStreamIndex);
        if (mpsStreamMap.containsKey(currentStreamIndex)) {
            MpsStreamInfo info = mpsStreamMap.get(currentStreamIndex);
            fileName = "UMD_VIDEO/STREAM/" + info.getName() + ".MPS";
            Modules.log.info("Loading stream:" + fileName);
            try {
                isoFile = iso.getFile(fileName);
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
			Emulator.log.error(e);
            }
        }
        if(isoFile != null) {
            startVideo();
        }
    }

	private Image getImage() {
		return image;
	}

	public boolean initVideo() {
		if (isoFile == null) {
			return false;
		}
		if (!startVideo()) {
			return false;
		}
		displayThread = new MpsDisplayThread();
		displayThread.setDaemon(true);
		displayThread.setName("UMD Video Player Thread");
	    displayThread.start();
	    return true;
	}

	private boolean startVideo() {
		endOfVideo = false;

		try {
			container = IContainer.make();
		} catch (Throwable e) {
			Emulator.log.error(e);
			return false;
		}

		try {
			isoFile.seek(0);
		} catch (IOException e) {
			Emulator.log.error(e);
			return false;
		}
		byteChannel = new MpsByteChannel(isoFile);

		if (container.open(byteChannel, null) < 0) {
	    	Emulator.log.error("could not open file: " + fileName);
	        return false;
	    }

	    int numStreams = container.getNumStreams();
	    videoStreamId = -1;
	    videoCoder = null;
	    audioStreamId = -1;
	    audioCoder = null;
	    boolean audioMuted = Settings.getInstance().readBool("emu.mutesound");
	    for (int i = 0; i < numStreams; i++) {
	    	IStream stream = container.getStream(i);
	    	IStreamCoder coder = stream.getStreamCoder();

	    	if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO)
	    	{
	    		videoStreamId = i;
	    		videoCoder = coder;
	    	} else if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO && !audioMuted) {
	    		audioStreamId = i;
	    		audioCoder = coder;
	    	}
	    }

	    if (videoCoder != null && videoCoder.open() < 0) {
	    	Emulator.log.error("could not open video decoder for container: " + fileName);
	    	return false;
	    }
	    if (audioCoder != null && audioCoder.open() < 0) {
	    	Emulator.log.info("AT3+ audio format is not yet supported by Jpcsp (file=" + fileName + ")");
	    	return false;
	    }

	    resampler = null;
	    if (videoCoder != null) {
	    	converter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, IPixelFormat.Type.BGR24, videoCoder.getWidth(), videoCoder.getHeight());
	    	if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
		    	resampler = IVideoResampler.make(screenWidth,
		    			screenHeigth, IPixelFormat.Type.BGR24,
		    			videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
		    	if (resampler == null) {
		    		Emulator.log.error("could not create color space resampler for: " + fileName);
		    		return false;
		    	}
	    	}
	    }
	    if (audioCoder != null) {
	    	openAudio(audioCoder);
	    }
	    packet = IPacket.make();
	    firstTimestampInStream = Global.NO_PTS;
	    systemClockStartTime = 0;
	    return true;
	}

	private void closeVideo() {
		if (container != null) {
			container.close();
			container = null;
		}
		if (videoCoder != null) {
			videoCoder.close();
			videoCoder = null;
		}
		if (resampler != null) {
			resampler.delete();
			resampler = null;
		}
		if (converter != null) {
			converter.delete();
			converter = null;
		}
		if (packet != null) {
			packet.delete();
			packet = null;
		}
	}

	private void stopDisplayThread() {
		while (displayThread != null && !threadExit) {
			done = true;
			sleep(1);
		}
		displayThread = null;
	}

	public void stopVideo() {
		stopDisplayThread();
		closeVideo();
		closeAudio();
		if (isoFile != null) {
			try {
				isoFile.close();
			} catch (IOException e) {
				// Ignore Exception
			}
		}
	}

	public void stepVideo() {
		if (container.readNextPacket(packet) >= 0) {
			if (packet.getStreamIndex() == videoStreamId && videoCoder != null) {
		        IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
		            videoCoder.getWidth(), videoCoder.getHeight());
		        int offset = 0;
		        while (offset < packet.getSize()) {
		            int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
		            if (bytesDecoded < 0) {
		        	    throw new RuntimeException("got error decoding video in: " + fileName);
		            }
		            offset += bytesDecoded;
		            if (picture.isComplete()) {
		        	    IVideoPicture newPic = picture;
		                if (resampler != null) {
		            	    newPic = IVideoPicture.make(resampler.getOutputPixelFormat(),
		            			    screenWidth, screenHeigth);
		            	    if (resampler.resample(newPic, picture) < 0) {
		            		    throw new RuntimeException("could not resample video from: " + fileName);
		            	    }
		                }
		                if (newPic.getPixelType() != IPixelFormat.Type.BGR24) {
		            	    throw new RuntimeException("could not decode video BGR 24 bit data in: " + fileName);
		                }
		                if (firstTimestampInStream == Global.NO_PTS) {
		            	    firstTimestampInStream = picture.getTimeStamp();
		            	    systemClockStartTime = System.currentTimeMillis();
		                } else {
		            	    long systemClockCurrentTime = System.currentTimeMillis();
		            	    long millisecondsClockTimeSinceStartofVideo = systemClockCurrentTime - systemClockStartTime;
		            	    long millisecondsStreamTimeSinceStartOfVideo = (picture.getTimeStamp() - firstTimestampInStream)/1000;
		            	    final long millisecondsTolerance = 50;
		            	    final long millisecondsToSleep =  (millisecondsStreamTimeSinceStartOfVideo - (millisecondsClockTimeSinceStartofVideo + millisecondsTolerance));
		            	    sleep(millisecondsToSleep);
		                }
                        if((converter != null) && (newPic != null)) {
                            image = converter.toImage(newPic);
                        }
		            }
		        }
		    } else if (packet.getStreamIndex() == audioStreamId && audioCoder != null) {
		        IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());
		        int offset = 0;
		        while(offset < packet.getSize()) {
		            int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);
		            if (bytesDecoded < 0) {
		                throw new RuntimeException("got error decoding audio in: " + fileName);
		            }
		            offset += bytesDecoded;
		            if (samples.isComplete())
		            {
		                playAudio(samples);
		            }
		        }
		    }
		} else {
			endOfVideo = true;
		}
	}

	private void openAudio(IStreamCoder aAudioCoder) {
	    AudioFormat audioFormat = new AudioFormat(aAudioCoder.getSampleRate(),
	        (int)IAudioSamples.findSampleBitDepth(aAudioCoder.getSampleFormat()),
	        aAudioCoder.getChannels(),
	        true,
	        false);
	    DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
	    try {
	        mLine = (SourceDataLine) AudioSystem.getLine(info);
	        mLine.open(audioFormat);
	        mLine.start();
        } catch (IllegalArgumentException iae) {
            // Some streams use a NULL PCM format.
            // Ignore the audio handling in these cases, for now.
            audioCoder = null;
	    } catch (LineUnavailableException e) {
	        throw new RuntimeException("could not open audio line");
	    }
	}

	private void playAudio(IAudioSamples aSamples) {
	    byte[] rawBytes = aSamples.getData().getByteArray(0, aSamples.getSize());
	    mLine.write(rawBytes, 0, aSamples.getSize());
	}

	private void closeAudio() {
	    if (mLine != null) {
	      mLine.drain();
	      mLine.close();
	      mLine = null;
	    }
	}

	private void sleep(long millis) {
	    if (millis > 0) {
		    try {
			  Thread.sleep(millis);
		    } catch (InterruptedException e) {
		    	// Ignore exception
		    }
	    }
	}

	private class MpsDisplayThread extends Thread {
		@Override
		public void run() {
			while (!done) {
				while (!endOfVideo && !done) {
					stepVideo();
					if (display != null && image != null) {
						display.setIcon(new ImageIcon(getImage()));
					}
				}
                goToNextMpsStream();
			}
			threadExit = true;
		}
	}

	private static class MpsByteChannel implements ReadableByteChannel {
		private UmdIsoFile file;
		private byte[] buffer;
        private int bufOffset;

		public MpsByteChannel(UmdIsoFile file) {
            this.file = file;
		}

		@Override
		public int read(ByteBuffer dst) throws IOException {
			int available = dst.remaining();
			if (buffer == null || buffer.length < available) {
				buffer = new byte[available];
			}

			int length = file.read(buffer, bufOffset, available);
			if (length > 0) {
				dst.put(buffer, bufOffset, length);
			}

			return length;
		}

		@Override
		public void close() throws IOException {
			file.close();
			file = null;
		}

		@Override
		public boolean isOpen() {
			return file != null;
		}
	}
}