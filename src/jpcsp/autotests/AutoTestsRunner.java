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
package jpcsp.autotests;

import java.awt.DisplayMode;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.GUI.IMainGUI;
import jpcsp.HLE.Modules;
import jpcsp.HLE.VFS.emulator.EmulatorVirtualFileSystem;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.hardware.Screen;
import jpcsp.log.LoggingOutputStream;

public class AutoTestsRunner {
	Emulator emulator;
	private static final String rootDirectory = "../pspautotests";
	private static final Logger log = Logger.getLogger("pspautotests");
	private static final int FAIL_TIMEOUT = 10; // in seconds

	class DummyGUI implements IMainGUI {
		@Override public void setMainTitle(String title) { }
		@Override public void RefreshButtons() { }
		@Override public void setLocation() { }
		@Override public DisplayMode getDisplayMode() { return new DisplayMode(480, 272, 32, 60); }
		@Override public void endWindowDialog() { }
		@Override public boolean isFullScreen() { return false; }
		@Override public boolean isVisible() { return false; }
		@Override public void pack() { }
		@Override public void setFullScreenDisplaySize() { }
		@Override public void startWindowDialog(Window window) { }
		@Override public void startBackgroundWindowDialog(Window window) { }
		@Override public Rectangle getCaptureRectangle() { return null; }
		@Override public void onUmdChange() { }
		@Override public void setDisplayMinimumSize(int width, int height) { }
		@Override public void setDisplaySize(int width, int height) { }
	}
	
	public AutoTestsRunner() {
		emulator = new Emulator(new DummyGUI());
        emulator.setFirmwareVersion(630);
	}
	
	public void run() {
        DOMConfigurator.configure("LogSettings.xml");
        System.setOut(new PrintStream(new LoggingOutputStream(Logger.getLogger("emu"), Level.INFO)));
        Screen.setHasScreen(false);
        IoFileMgrForUser.IoOperation.iodevctl.setDelayMillis(0);
        Modules.sceDisplayModule.setCalledFromCommandLine();

		try {
			runImpl();
		} catch (Throwable o) {
			o.printStackTrace();
		}
		
		System.exit(0);
	}

	protected void runImpl() throws Throwable {
		runTestFolder(rootDirectory + "/tests");
//		runTest(rootDirectory + "/tests/gpu/reflection/reflection");
	}

	protected void runTestFolder(String folderPath) throws Throwable {
		for (File file : new File(folderPath).listFiles()) {
			if (file.getName().charAt(0) == '.') {
				continue;
			}
			if (file.isDirectory()) {
				runTestFolder(file.getPath());
			} else if (file.isFile()) {
				String name = file.getPath();
				if (name.substring(name.length() - 9).equals(".expected")) {
					runTest(name.substring(0, name.length() - 9));
				}
			}
		}
	}

	protected boolean isWindows() {
		return (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0);
	}

	protected void runTest(String baseFileName) throws Throwable {
		new File(EmulatorVirtualFileSystem.getScreenshotFileName()).delete();

		boolean timeout = false;
		try {
			runFile(baseFileName + ".prx");
		} catch (TimeoutException toe) {
			timeout = true;
		}
		checkOutput(baseFileName, baseFileName + ".expected", timeout);
	}
	
	protected void checkOutput(String baseFileName, String fileName, boolean timeout) throws IOException {
		String actualOutput = AutoTestsOutput.getOutput().trim();
		String expectedOutput = readFileAsString(fileName).trim();
		if (actualOutput.equals(expectedOutput)) {
			log.info(String.format("%s: OK", baseFileName));
		} else {
			if (timeout) {
				log.error(String.format("%s: FAIL, TIMEOUT", baseFileName));
			} else {
				log.error(String.format("%s: FAIL", baseFileName));
			}
			diff(expectedOutput, actualOutput);
		}

		File screenshotExpected = new File(fileName + ".bmp");
		if (screenshotExpected.canRead()) {
			File screenshotResult = new File(EmulatorVirtualFileSystem.getScreenshotFileName());
			if (screenshotResult.canRead()) {
				File savedScreenshotResult = new File(baseFileName + ".result.bmp");
				savedScreenshotResult.delete();
				if (screenshotResult.renameTo(savedScreenshotResult)) {
					log.info(String.format("%s: saved screenshot under '%s'", baseFileName, savedScreenshotResult));
				} else {
					log.error(String.format("%s: cannot save screenshot from '%s' to '%s'", baseFileName, screenshotResult, savedScreenshotResult));
				}
			} else {
				log.error(String.format("%s: FAIL, no result screenshot found", baseFileName));
			}
		}
	}

	public static void diff(String x, String y) {
		diff(x.split("\\n"), y.split("\\n"));
	}

    public static void diff(String[] x, String[] y) {
        // number of lines of each file
        int M = x.length;
        int N = y.length;

        // opt[i][j] = length of LCS of x[i..M] and y[j..N]
        int[][] opt = new int[M+1][N+1];

        // compute length of LCS and all subproblems via dynamic programming
        for (int i = M-1; i >= 0; i--) {
            for (int j = N-1; j >= 0; j--) {
                if (x[i].equals(y[j]))
                    opt[i][j] = opt[i+1][j+1] + 1;
                else 
                    opt[i][j] = Math.max(opt[i+1][j], opt[i][j+1]);
            }
        }

        // recover LCS itself and print out non-matching lines to standard output
        int i = 0, j = 0;
        while (i < M && j < N) {
            if (x[i].equals(y[j])) {
                log.debug("  " + x[i]);
                i++;
                j++;
            } else if (opt[i+1][j] >= opt[i][j+1]) {
            	log.info("- " + x[i++]);
            } else {
            	log.info("+ " + y[j++]);
            }
        }

        // dump out one remainder of one string if the other is exhausted
        while (i < M || j < N) {
            if (i == M) {
            	log.info("+ " + y[j++]);
            } else if (j == N) {
            	log.info("- " + x[i++]);
            }
        }
    }
    
    protected void reset() {
		AutoTestsOutput.clearOutput();

		Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_PAUSE);
		Emulator.getInstance().initNewPsp(false);
    }
	
	protected void runFile(String fileName) throws Throwable {
		File file = new File(fileName);

		reset();

		try {
	        RandomAccessFile raf = new RandomAccessFile(file, "r");
	        try {
		        FileChannel roChannel = raf.getChannel();
		        try {
			        ByteBuffer readbuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) roChannel.size());
			        {
			        	//module =
			        	emulator.load(file.getPath(), readbuffer);
			        }
		        } finally {
		        	roChannel.close();
		        }
	        } finally {
	        	raf.close();
	        }
		} catch (FileNotFoundException fileNotFoundException) {
		}

		RuntimeContext.setIsHomebrew(true);

		UmdIsoReader umdIsoReader = new UmdIsoReader(rootDirectory + "/input/cube.cso");
        Modules.IoFileMgrForUserModule.setIsoReader(umdIsoReader);
        jpcsp.HLE.Modules.sceUmdUserModule.setIsoReader(umdIsoReader);
        Modules.IoFileMgrForUserModule.setfilepath(file.getParent());

		log.debug(String.format("Running: %s...", fileName));
        {
            RuntimeContext.setIsHomebrew(false);

            HLEModuleManager.getInstance().startModules(false);
            Modules.sceDisplayModule.setUseSoftwareRenderer(true);
            {
				emulator.RunEmu();

				long startTime = System.currentTimeMillis(); 
				while (!Emulator.pause) {
					Modules.sceDisplayModule.step();
					if (System.currentTimeMillis() - startTime > FAIL_TIMEOUT * 1000) {
						throw(new TimeoutException());
					}
					Thread.sleep(1);
				}
            }
			HLEModuleManager.getInstance().stopModules();
        }
	}
	
	private static String readFileAsString(String filePath) throws java.io.IOException{
	    byte[] buffer = new byte[(int) new File(filePath).length()];
	    BufferedInputStream f = null;
	    try {
	        f = new BufferedInputStream(new FileInputStream(filePath));
	        f.read(buffer);
	    } finally {
	        if (f != null) try { f.close(); } catch (IOException ignored) { }
	    }
	    return new String(buffer);
	}
}
