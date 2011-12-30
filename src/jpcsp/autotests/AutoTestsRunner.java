package jpcsp.autotests;

import java.awt.DisplayMode;
import java.awt.Window;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.GUI.IMainGUI;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.filesystems.umdiso.UmdIsoReader;

public class AutoTestsRunner {
	Emulator emulator;
	
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
	}
	
	public AutoTestsRunner() {
		emulator = new Emulator(new DummyGUI());
        emulator.setFirmwareVersion(630);
	}
	
	public void run() {
		Logger.getRootLogger().addAppender(new WriterAppender(new SimpleLayout(), System.out));
		//Logger.getRootLogger().setLevel(Level.WARN);
		//Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getRootLogger().setLevel(Level.ERROR);

		try {
			runImpl();
		} catch (Throwable o) {
			o.printStackTrace();
		}
		
		System.exit(0);
	}
	
	protected void runImpl() throws Throwable {
		runTestFolder("pspautotests/tests");
		//runTest("pspautotests/tests/rtc/rtc");
	}
	
	protected void runTestFolder(String folderPath) throws Throwable {
		for (File file : new File(folderPath).listFiles()) {
			if (file.getName().charAt(0) == '.') continue;
			//System.out.println(file);
			if (file.isDirectory()) {
				runTestFolder(file.getAbsolutePath());
			} else if (file.isFile()) {
				String name = file.getAbsolutePath();
				if (name.substring(name.length() - 9).equals(".expected")) {
					runTest(name.substring(0, name.length() - 9));
				}
			}
		}
	}
	
	protected boolean isWindows() {
		return (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0);
	}
	
	protected void buildFile(String fileName) throws IOException, InterruptedException {
		String command;
		if (isWindows()) {
			command = "pspautotests/tests/build.bat";
		} else {
			command = "pspautotests/tests/build.sh";
		}
		Runtime.getRuntime().exec(new String[] { command, fileName }).waitFor();
	}
	
	protected void runTest(String baseFileName) throws Throwable {
		try {
			buildFile(baseFileName + ".elf");
			runFile(baseFileName + ".elf");
			checkOutput(baseFileName + ".expected");
		} catch (TimeoutException toe) {
			System.out.println("FAIL:TIMEOUT");
		}
	}
	
	protected void checkOutput(String fileName) throws IOException {
		String actualOutput = AutoTestsOutput.getOutput().trim();
		String expectedOutput = readFileAsString(fileName).trim();
		if (actualOutput.equals(expectedOutput)) {
			System.out.println("OK");
		} else {
			System.out.println("FAIL");
			diff(expectedOutput, actualOutput);
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
                System.out.println("  " + x[i]);
                i++;
                j++;
            }
            else if (opt[i+1][j] >= opt[i][j+1]) System.out.println("- " + x[i++]);
            else                                 System.out.println("+ " + y[j++]);
        }

        // dump out one remainder of one string if the other is exhausted
        while(i < M || j < N) {
            if      (i == M) System.out.println("- " + y[j++]);
            else if (j == N) System.out.println("+ " + x[i++]);
        }
    }
    
    protected void reset() {
		AutoTestsOutput.clearOutput();
		
		Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_PAUSE);
		RuntimeContext.reset();
		HLEModuleManager.getInstance().stopModules();
    }
	
	protected void runFile(String fileName) throws Throwable {
		File file = new File(fileName);
		
		reset();
		sceDisplay.ignoreLWJGLError = true;

		//SceModule module;
		
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
		
		UmdIsoReader umdIsoReader = new UmdIsoReader("pspautotests/input/cube.cso");
        Modules.IoFileMgrForUserModule.setIsoReader(umdIsoReader);
        jpcsp.HLE.Modules.sceUmdUserModule.setIsoReader(umdIsoReader);
        Modules.IoFileMgrForUserModule.setfilepath(file.getPath());
        
        //System.out.printf("Started\n");
		System.out.print(String.format("Running: %s...", fileName));
        {
            RuntimeContext.setIsHomebrew(false);
            //Modules.SysMemUserForUserModule.setMemory64MB(true);

            HLEModuleManager.getInstance().startModules();
            {
				emulator.RunEmu();

				long startTime = System.currentTimeMillis(); 
				while (!Emulator.pause) {
					if (System.currentTimeMillis() - startTime > 5 * 1000) {
						throw(new TimeoutException());
					}
					Thread.sleep(1);
				}
            }
			HLEModuleManager.getInstance().stopModules();

        }
        
        //reset();
        // System.out.printf("Ended\n");
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
