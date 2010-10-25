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
package jpcsp.graphics;

import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_CANCEL_DONE;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_DONE;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_DRAWING;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_END_REACHED;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_STALL_REACHED;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE0;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE1;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE2;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE3;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE4;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE5;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE6;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE7;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_PROJECTION;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_TEXGEN;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_VIEW;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_WORLD;
import static jpcsp.graphics.GeCommands.*;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.State;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.HLE.kernel.types.pspGeContext;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.graphics.GeContext.EnableDisableFlag;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.RE.buffer.IREBufferManager;
import jpcsp.graphics.capture.CaptureManager;
import jpcsp.graphics.textures.Texture;
import jpcsp.graphics.textures.TextureCache;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Utilities;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

//
// Ideas for Optimization:
// - compile GE lists (or part of it) into OpenGL display list (glNewList/glCallList).
//   For example, immutable subroutines called using CALL could be compiled into a display list.
//   A first run of the game using a profiler option could be used to detect which parts
//   are immutable. This information could be stored in a file for subsequent runs and
//   used as hints for the next runs.
// - Unswizzle textures in shader (is this possible?)
//
public class VideoEngine {

    public static final int NUM_LIGHTS = 4;
    public static final int SIZEOF_FLOAT = IRenderingEngine.sizeOfType[IRenderingEngine.RE_FLOAT];
    public final static String[] psm_names = new String[]{
        "PSM_5650",
        "PSM_5551",
        "PSM_4444",
        "PSM_8888",
        "PSM_4BIT_INDEXED",
        "PSM_8BIT_INDEXED",
        "PSM_16BIT_INDEXED",
        "PSM_32BIT_INDEXED",
        "PSM_DXT1",
        "PSM_DXT3",
        "PSM_DXT5"
    };
    public final static String[] logical_ops_names = new String[]{
        "LOP_CLEAR",
        "LOP_AND",
        "LOP_REVERSE_AND",
        "LOP_COPY",
        "LOP_INVERTED_AND",
        "LOP_NO_OPERATION",
        "LOP_EXLUSIVE_OR",
        "LOP_OR",
        "LOP_NEGATED_OR",
        "LOP_EQUIVALENCE",
        "LOP_INVERTED",
        "LOP_REVERSE_OR",
        "LOP_INVERTED_COPY",
        "LOP_INVERTED_OR",
        "LOP_NEGATED_AND",
        "LOP_SET"
    };
    private static final int[] textureByteAlignmentMapping = {2, 2, 2, 4};
    private static VideoEngine instance;
    private sceDisplay display;
    private IRenderingEngine re;
    private GeContext context;
    private IREBufferManager bufferManager;
    public static Logger log = Logger.getLogger("ge");
    public static final boolean useTextureCache = true;
    private boolean useVertexCache = false;
    private static GeCommands helper;
    private VertexInfo vinfo = new VertexInfo();
    private VertexInfoReader vertexInfoReader = new VertexInfoReader();
    private static final char SPACE = ' ';
    private DurationStatistics statistics;
    private DurationStatistics vertexStatistics = new DurationStatistics("Vertex");
    private DurationStatistics waitSignalStatistics = new DurationStatistics("Wait for GE Signal completion");
    private DurationStatistics waitStallStatistics = new DurationStatistics("Wait on stall");
    private DurationStatistics textureCacheLookupStatistics = new DurationStatistics("Lookup in TextureCache");
    private DurationStatistics vertexCacheLookupStatistics = new DurationStatistics("Lookup in VertexCache");
    private DurationStatistics[] commandStatistics;
    private int errorCount;
    private static final int maxErrorCount = 5; // Abort list processing when detecting more errors
    private boolean isLogTraceEnabled;
    private boolean isLogDebugEnabled;
    private boolean isLogInfoEnabled;
    private boolean isLogWarnEnabled;
    private int primCount;
    private boolean viewportChanged;
    public MatrixUpload projectionMatrixUpload;
    public MatrixUpload modelMatrixUpload;
    public MatrixUpload viewMatrixUpload;
    public MatrixUpload textureMatrixUpload;
    private int boneMatrixIndex;
    private int boneMatrixLinearUpdatedMatrix; // number of updated matrix
    private static final float[] blackColor = new float[]{0, 0, 0, 0};
    private boolean lightingChanged;
    private boolean materialChanged;
    private boolean textureChanged;
    private int[] patch_prim_types = { PRIM_TRIANGLE_STRIPS, PRIM_LINES_STRIPS, PRIM_POINT };
    private boolean clutIsDirty;
    private boolean usingTRXKICK;
    private int maxSpriteHeight;
    private boolean blendChanged;
    private boolean depthChanged;
    private boolean scissorChanged;
    // opengl needed information/buffers
    private int textureId = -1;
    private int[] tmp_texture_buffer32 = new int[1024 * 1024];
    private short[] tmp_texture_buffer16 = new short[1024 * 1024];
    private int[] clut_buffer32 = new int[4096];
    private short[] clut_buffer16 = new short[4096];
    private boolean listHasEnded;
    private PspGeList currentList; // The currently executing list
    private static final int drawBufferSize = 2 * 1024 * 1024 * SIZEOF_FLOAT;
    private int bufferId;
    private int nativeBufferId;
    float[][] bboxVertices;
    private ConcurrentLinkedQueue<PspGeList> drawListQueue;
    private boolean somethingDisplayed;
    private boolean geBufChanged;
    private IAction hleAction;
    private HashMap<Integer, Integer> currentCMDValues;
    private boolean bboxWarningDisplayed = false;
    private Set<Integer> videoTextures;

    public static class MatrixUpload {
        private final float[] matrix;
        private boolean changed;
        private int[] matrixIndex;
        private int index;
        private int maxIndex;

        public MatrixUpload(float[] matrix, int matrixWidth, int matrixHeight) {
            changed = true;
            this.matrix = matrix;

            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 4; x++) {
                    matrix[y * 4 + x] = (x == y ? 1 : 0);
                }
            }

            maxIndex = matrixWidth * matrixHeight;
            matrixIndex = new int[maxIndex];
            for (int i = 0; i < maxIndex; i++) {
            	matrixIndex[i] = (i % matrixWidth) + (i / matrixWidth) * 4;
            }
        }

        public void startUpload(int startIndex) {
        	index = startIndex;
        }

        public final boolean uploadValue(float value) {
            if (index >= maxIndex) {
            	if(VideoEngine.getInstance().isLogDebugEnabled) {
					VideoEngine.log(String.format("Ignored Matrix upload value (idx=%08X)", index));
				}
            } else {
	            int i = matrixIndex[index];
	            if (matrix[i] != value) {
	                matrix[i] = value;
	                changed = true;
	            }
            }
            index++;

            return index >= maxIndex;
        }

        public boolean isChanged() {
            return changed;
        }

        public void setChanged(boolean changed) {
            this.changed = changed;
        }
    }

    private static void log(String msg) {
        log.debug(msg);
    }

    public static VideoEngine getInstance() {
        if (instance == null) {
            helper = new GeCommands();
            instance = new VideoEngine();
        }
        return instance;
    }

    private VideoEngine() {
        context = new GeContext();
        modelMatrixUpload = new MatrixUpload(context.model_uploaded_matrix, 3, 4);
        viewMatrixUpload = new MatrixUpload(context.view_uploaded_matrix, 3, 4);
        textureMatrixUpload = new MatrixUpload(context.texture_uploaded_matrix, 3, 4);
        projectionMatrixUpload = new MatrixUpload(context.proj_uploaded_matrix, 4, 4);
        boneMatrixLinearUpdatedMatrix = 8;

        statistics = new DurationStatistics("VideoEngine Statistics");
        commandStatistics = new DurationStatistics[256];
        for (int i = 0; i < commandStatistics.length; i++) {
            commandStatistics[i] = new DurationStatistics(String.format("%-11s", helper.getCommandString(i)));
        }

        drawListQueue = new ConcurrentLinkedQueue<PspGeList>();

        bboxVertices = new float[8][3];
        for (int i = 0; i < 8; i++) {
            bboxVertices[i] = new float[3];
        }

        currentCMDValues = new HashMap<Integer, Integer>();
        videoTextures = new HashSet<Integer>();
    }

    /** Called from pspge module */
    public void pushDrawList(PspGeList list) {
        drawListQueue.add(list);
    }

    /** Called from pspge module */
    public void pushDrawListHead(PspGeList list) {
        // The ConcurrentLinkedQueue type doesn't allow adding
        // objects directly at the head of the queue.

        // This function creates a new array using the given list as it's head
        // and constructs a new ConcurrentLinkedQueue based on it.
        // The actual drawListQueue is then replaced by this new one.
        int arraySize = drawListQueue.size();

        if (arraySize > 0) {
            PspGeList[] array = drawListQueue.toArray(new PspGeList[arraySize]);

            ConcurrentLinkedQueue<PspGeList> newQueue = new ConcurrentLinkedQueue<PspGeList>();
            PspGeList[] newArray = new PspGeList[arraySize + 1];

            newArray[0] = list;
            for (int i = 0; i < arraySize; i++) {
                newArray[i + 1] = array[i];
                newQueue.add(newArray[i]);
            }

            drawListQueue = newQueue;
        } else {    // If the queue is empty.
            drawListQueue.add(list);
        }
    }

    public int numberDrawLists() {
    	return drawListQueue.size();
    }

    public boolean hasDrawLists() {
        return !drawListQueue.isEmpty();
    }

    public boolean hasDrawList(int listAddr) {
        if (currentList != null && currentList.list_addr == listAddr) {
            return true;
        }

        for (PspGeList list : drawListQueue) {
            if (list != null && list.list_addr == listAddr) {
                return true;
            }
        }

        return false;
    }

    public PspGeList getFirstDrawList() {
    	PspGeList firstList = currentList;
    	if (firstList == null) {
    		firstList = drawListQueue.peek();
    	}

    	return firstList;
    }

    public PspGeList getLastDrawList() {
        PspGeList lastList = null;
        for (PspGeList list : drawListQueue) {
            if (list != null) {
                lastList = list;
            }
        }

        if (lastList == null) {
            lastList = currentList;
        }

        return lastList;
    }

    public void start() {
        display = Modules.sceDisplayModule;
        re = display.getRenderingEngine();
        re.setGeContext(context);
        context.setRenderingEngine(re);
        bufferManager = re.getBufferManager();

        if (!re.getBufferManager().useVBO()) {
            // VertexCache is relying on VBO
            useVertexCache = false;
        }

        bufferId = bufferManager.genBuffer(IRenderingEngine.RE_FLOAT, drawBufferSize / SIZEOF_FLOAT, IRenderingEngine.RE_STREAM_DRAW);
        nativeBufferId = bufferManager.genBuffer(IRenderingEngine.RE_BYTE, drawBufferSize, IRenderingEngine.RE_STREAM_DRAW);
    }

    public IRenderingEngine getRenderingEngine() {
    	return re;
    }

    public GeContext getContext() {
    	return context;
    }

    public static void exit() {
        if (instance != null) {
            log.info(instance.statistics.toString());
            Arrays.sort(instance.commandStatistics);
            int numberCommands = 20;
            log.info(numberCommands + " most time intensive Video commands:");
            for (int i = 0; i < numberCommands; i++) {
                VideoEngine.log.info("    " + instance.commandStatistics[i].toString());
            }
            log.info(instance.vertexStatistics);
            log.info(instance.waitSignalStatistics);
            log.info(instance.waitStallStatistics);
            log.info(instance.textureCacheLookupStatistics);
            log.info(instance.vertexCacheLookupStatistics);
        }
    }

    public static DurationStatistics getStatistics() {
        if (instance == null) {
            return null;
        }

        return instance.statistics;
    }

    /** call from GL thread
     * @return true if an update was made
     */
    public boolean update() {
        int listCount = drawListQueue.size();
        PspGeList list = drawListQueue.poll();
        if (list == null) {
            return false;
        }

        startUpdate();

        if (State.captureGeNextFrame) {
            CaptureManager.startCapture("capture.bin", list);
        }

        if (State.replayGeNextFrame) {
            // Load the replay list into drawListQueue
            CaptureManager.startReplay("capture.bin");

            // Hijack the current list with the replay list
            // TODO this is assuming there is only 1 list in drawListQueue at this point, only the last list is the replay list
            PspGeList replayList = drawListQueue.poll();
            replayList.id = list.id;
            replayList.blockedThreadIds.clear();
            replayList.blockedThreadIds.addAll(list.blockedThreadIds);
            list = replayList;
        }

        // Draw only as many lists as currently available in the drawListQueue.
        // Some game add automatically a new list to the queue when the current
        // list is finishing.
        do {
            executeList(list);
            listCount--;
            if (listCount <= 0) {
                break;
            }
            list = drawListQueue.poll();
        } while (list != null);

        if (State.captureGeNextFrame) {
            // Can't end capture until we get a sceDisplaySetFrameBuf after the list has executed
            CaptureManager.markListExecuted();
        }

        if (State.replayGeNextFrame) {
            CaptureManager.endReplay();
            State.replayGeNextFrame = false;
        }

        endUpdate();

        return true;
    }

    private void logLevelUpdated() {
        isLogTraceEnabled = log.isTraceEnabled();
        isLogDebugEnabled = log.isDebugEnabled();
        isLogInfoEnabled = log.isInfoEnabled();
        isLogWarnEnabled = log.isEnabledFor(Level.WARN);
    }

    public void setLogLevel(Level level) {
        log.setLevel(level);
        logLevelUpdated();
    }

    /**
     * The memory used by GE has been updated or changed.
     * Update the caches so that they see these changes.
     */
    private void memoryForGEUpdated() {
        if (useTextureCache) {
            TextureCache.getInstance().resetTextureAlreadyHashed();
        }
        if (useVertexCache) {
            VertexCache.getInstance().resetVertexAlreadyHashed();
        }
    }

    public void hleSetFrameBuf(int topAddr, int bufferWidth, int pixelFormat) {
    	if (context.fbp != topAddr || context.fbw != bufferWidth || context.psm != pixelFormat) {
    		context.fbp = topAddr;
    		context.fbw = bufferWidth;
    		context.psm = pixelFormat;
    		geBufChanged = true;
    	}
    }

    private void startUpdate() {
        statistics.start();

        logLevelUpdated();
        memoryForGEUpdated();
        somethingDisplayed = false;
        textureChanged = true;
        projectionMatrixUpload.setChanged(true);
        modelMatrixUpload.setChanged(true);
        viewMatrixUpload.setChanged(true);
        textureMatrixUpload.setChanged(true);
        clutIsDirty = true;
        lightingChanged = true;
        blendChanged = true;
        viewportChanged = true;
        depthChanged = true;
        materialChanged = true;
        scissorChanged = true;
        errorCount = 0;
        usingTRXKICK = false;
        maxSpriteHeight = 0;
        primCount = 0;
    }

    private void endUpdate() {
    	if (useVertexCache) {
            if (primCount > VertexCache.cacheMaxSize) {
                log.warn(String.format("VertexCache size (%d) too small to execute %d PRIM commands", VertexCache.cacheMaxSize, primCount));
            }
        }

        statistics.end();
    }

    public void error(String message) {
        errorCount++;
        log.error(message);
        if (errorCount >= maxErrorCount) {
            if (tryToFallback()) {
                log.error("Aborting current list processing due to too many errors");
            }
        }
    }

    private boolean tryToFallback() {
        boolean abort = false;

        if (!currentList.isStackEmpty()) {
            // When have some CALLs on the stack, try to return from the last CALL
            int oldPc = currentList.pc;
            currentList.ret();
            int newPc = currentList.pc;
            if (isLogDebugEnabled) {
                log(String.format("tryToFallback old PC: 0x%08X, new PC: 0x%08X", oldPc, newPc));
            }
        } else {
            // Finish this list
            currentList.finishList();
            listHasEnded = true;
            abort = true;
        }

        return abort;
    }

    private void checkCurrentListPc() {
        Memory mem = Memory.getInstance();
        while (!mem.isAddressGood(currentList.pc)) {
            if (!mem.isIgnoreInvalidMemoryAccess()) {
                error("Reading GE list from invalid address 0x" + Integer.toHexString(currentList.pc));
                break;
            }
			// Ignoring memory read errors.
			// Try to fall back and continue the list processing.
			log.warn("Reading GE list from invalid address 0x" + Integer.toHexString(currentList.pc));
			if (tryToFallback()) {
			    break;
			}
        }
    }

    private void executeHleAction() {
        if (hleAction != null) {
            hleAction.execute();
            hleAction = null;
        }
    }

    // call from GL thread
    // There is an issue here with Emulator.pause
    // - We want to stop on errors
    // - But user may also press pause button
    //   - Either continue drawing to the end of the list (bad if the list contains an infinite loop)
    //   - Or we want to be able to restart drawing when the user presses the run button
    private void executeList(PspGeList list) {
        currentList = list;
        listHasEnded = false;
        currentList.status = PSP_GE_LIST_DRAWING;

        if (isLogDebugEnabled) {
            log("executeList " + list);
        }

        executeHleAction();

        IMemoryReader memoryReader = MemoryReader.getMemoryReader(currentList.pc, 4);
        int memoryReaderPc = currentList.pc;
        int waitForSyncCount = 0;
        while (!listHasEnded && (!Emulator.pause || State.captureGeNextFrame)) {
            if (currentList.isPaused() || currentList.isEnded()) {
				waitSignalStatistics.start();
				if (isLogDebugEnabled) {
				    log.debug(String.format("FINISH / SIGNAL / END reached, waiting for Sync"));
				}
				currentList.status = PSP_GE_LIST_END_REACHED;
				if (!currentList.waitForSync(10)) {
				    if (isLogDebugEnabled) {
				        log.debug("Wait for sync while END reached");
				    }
				    waitForSyncCount++;

				    // Waiting maximum 100 * 10ms (= 1 second) on an END command.
				    // After this timeout, abort the list.
				    if (waitForSyncCount > 100) {
				        error(String.format("Waiting too long on an END command, aborting the list %s", currentList));
				    }
				} else {
				    waitForSyncCount = 0;
				}

				executeHleAction();
				if (currentList.isRestarted()) {
					currentList.clearRestart();
					currentList.clearPaused();
				}
				if (!currentList.isPaused()) {
	                if (currentList.isFinished()) {
	                    listHasEnded = true;
	                    break;
	                }
				    currentList.status = PSP_GE_LIST_DRAWING;
				}
				waitSignalStatistics.end();
            } else if (currentList.isStallReached()) {
                waitStallStatistics.start();
                if (isLogDebugEnabled) {
                    log.debug(String.format("Stall address 0x%08X reached, waiting for Sync", currentList.pc));
                }
                currentList.status = PSP_GE_LIST_STALL_REACHED;
                if (!currentList.waitForSync(10)) {
                    if (isLogDebugEnabled) {
                        log.debug("Wait for sync while stall reached");
                    }
                    waitForSyncCount++;

                    // Waiting maximum 100 * 10ms (= 1 second) on a stall address.
                    // After this timeout, abort the list.
                    //
                    // When the stall address is at the very beginning of the list
                    // (i.e. the list has just been enqueued, but the stall has not yet been updated),
                    // allow waiting for a longer time (the CPU might be busy
                    // compiling a huge CodeBlock on the first call).
                    // This avoids aborting the first list enqueued.
                    int maxStallCount = (currentList.pc != currentList.list_addr ? 100 : 400);

                    if (waitForSyncCount > maxStallCount) {
                        error(String.format("Waiting too long on stall address 0x%08X, aborting the list %s", currentList.pc, currentList));
                    }
                } else {
                    waitForSyncCount = 0;
                }
                executeHleAction();
                if (!currentList.isStallReached()) {
                    currentList.status = PSP_GE_LIST_DRAWING;
                }
                waitStallStatistics.end();
            } else {
                if (currentList.pc != memoryReaderPc) {
                    // The currentList.pc is no longer reading in sequence
                    // and has jumped to a next location, get a new memory reader.
                    checkCurrentListPc();
                    if (listHasEnded || Emulator.pause) {
                        break;
                    }
                    memoryReader = MemoryReader.getMemoryReader(currentList.pc, 4);
                }
                int ins = memoryReader.readNext();
                currentList.pc += 4;
                memoryReaderPc = currentList.pc;

                executeCommand(ins);
            }
        }

        if (Emulator.pause && !listHasEnded) {
            VideoEngine.log.info("Emulator paused - cancelling current list id=" + currentList.id);
            currentList.status = PSP_GE_LIST_CANCEL_DONE;
        }

        // let DONE take priority over STALL_REACHED
        if (listHasEnded) {
            currentList.status = PSP_GE_LIST_END_REACHED;

            // Tested on PSP:
            // A list is only DONE after a combination of FINISH + END.
            if (currentList.isEnded()) {
                currentList.status = PSP_GE_LIST_DONE;
            }
        }

        if (list.isDone()) {
        	Modules.sceGe_userModule.hleGeListSyncDone(list);
        }

        executeHleAction();

        currentList = null;
    }

    public PspGeList getCurrentList() {
        return currentList;
    }

    public float[] getMatrix(int mtxtype) {
        float resmtx[] = new float[4 * 4];
        switch (mtxtype) {
            case PSP_GE_MATRIX_BONE0:
            case PSP_GE_MATRIX_BONE1:
            case PSP_GE_MATRIX_BONE2:
            case PSP_GE_MATRIX_BONE3:
            case PSP_GE_MATRIX_BONE4:
            case PSP_GE_MATRIX_BONE5:
            case PSP_GE_MATRIX_BONE6:
            case PSP_GE_MATRIX_BONE7:
                resmtx = context.bone_uploaded_matrix[mtxtype - PSP_GE_MATRIX_BONE0];
                break;
            case PSP_GE_MATRIX_WORLD:
                resmtx = context.model_uploaded_matrix;
                break;
            case PSP_GE_MATRIX_VIEW:
                resmtx = context.view_uploaded_matrix;
                break;
            case PSP_GE_MATRIX_PROJECTION:
                resmtx = context.proj_uploaded_matrix;
                break;
            case PSP_GE_MATRIX_TEXGEN:
                resmtx = context.texture_uploaded_matrix;
                break;
        }

        return resmtx;
    }

    public int getCommandValue(int cmd) {
        return currentCMDValues.get(cmd);
    }

    public String commandToString(int cmd) {
        return GeCommands.getInstance().getCommandString(cmd);
    }

    public static int command(int instruction) {
        return (instruction >>> 24);
    }

    private static int intArgument(int instruction) {
        return (instruction & 0x00FFFFFF);
    }

    private static float floatArgument(int normalArgument) {
        return Float.intBitsToFloat(normalArgument << 8);
    }

    private int getStencilOp(int pspOP) {
    	if (pspOP > SOP_DECREMENT_STENCIL_VALUE) {
            log.warn("UNKNOWN stencil op " + pspOP);
    	}

        return SOP_KEEP_STENCIL_VALUE;
    }

    private int getBlendFix(float[] fix_color) {
        if (fix_color[0] == 0 && fix_color[1] == 0 && fix_color[2] == 0) {
            return IRenderingEngine.GU_FIX_BLACK;
        } else if (fix_color[0] == 1 && fix_color[1] == 1 && fix_color[2] == 1) {
            return IRenderingEngine.GU_FIX_WHITE;
        } else {
            return IRenderingEngine.GU_FIX_BLEND_COLOR;
        }
    }

    private int getColorInt(float[] color) {
    	return (((int) (color[0] * 255))      ) |
    	       (((int) (color[1] * 255)) <<  8) |
    	       (((int) (color[2] * 255)) << 16) |
    	       (((int) (color[3] * 255)) << 24);
    }

    private float[] getBlendColor(int gl_blend_src, int gl_blend_dst) {
        float[] blend_color = null;
        if (gl_blend_src == IRenderingEngine.GU_FIX_BLEND_COLOR) {
            blend_color = context.sfix_color;
            if (gl_blend_dst == IRenderingEngine.GU_FIX_BLEND_COLOR) {
                if (context.sfix_color[0] != context.dfix_color[0]
                        || context.sfix_color[1] != context.dfix_color[1]
                        || context.sfix_color[2] != context.dfix_color[2]
                        || context.sfix_color[3] != context.dfix_color[3]) {
                    log.warn(String.format("UNSUPPORTED: Both different SFIX (%08X) and DFIX (%08X) are not supported (blend equation=%d)", getColorInt(context.sfix_color), getColorInt(context.dfix_color), context.blendEquation));
                }
            }
        } else if (gl_blend_dst == IRenderingEngine.GU_FIX_BLEND_COLOR) {
            blend_color = context.dfix_color;
        }

        return blend_color;
    }

    // hack partially based on pspplayer
    private void setBlendFunc() {
    	int reBlendSrc = context.blend_src;
    	if (context.blend_src < 0 || context.blend_src > 10) {
            error("Unhandled alpha blend src used " + context.blend_src);
            reBlendSrc = 0;
    	} else if (context.blend_src == 10) { // GU_FIX
    		reBlendSrc = getBlendFix(context.sfix_color);
    	}

    	int reBlendDst = context.blend_dst;
    	if (context.blend_dst < 0 || context.blend_dst > 10) {
            error("Unhandled alpha blend dst used " + context.blend_dst);
            reBlendDst = 0;
    	} else if (context.blend_dst == 10) { // GU_FIX
        	if (reBlendSrc == IRenderingEngine.GU_FIX_BLEND_COLOR
        	        && context.sfix_color[0] + context.dfix_color[0] == 1
        	        && context.sfix_color[1] + context.dfix_color[1] == 1
        	        && context.sfix_color[2] + context.dfix_color[2] == 1) {
        		reBlendDst = IRenderingEngine.GU_FIX_BLEND_ONE_MINUS_COLOR;
        	} else {
        		reBlendDst = getBlendFix(context.dfix_color);
        	}
    	}

        float[] blend_color = getBlendColor(reBlendSrc, reBlendDst);
        if (blend_color != null) {
        	re.setBlendColor(blend_color);
        }

        re.setBlendFunc(reBlendSrc, reBlendDst);
    }

    private int getClutAddr(int level, int clutNumEntries, int clutEntrySize) {
        return context.tex_clut_addr + context.tex_clut_start * clutEntrySize;
    }

    private void readClut() {
        if (!clutIsDirty) {
            return;
        }

        // Texture using clut?
        if (context.texture_storage >= TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED && context.texture_storage <= TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED) {
            if (context.tex_clut_mode == CMODE_FORMAT_32BIT_ABGR8888) {
                readClut32(0);
            } else {
                readClut16(0);
            }
        }
    }

    private short[] readClut16(int level) {
        int clutNumEntries = context.tex_clut_num_blocks * 16;

        // Update the clut_buffer only if some clut parameters have been changed
        // since last update.
        if (clutIsDirty) {
            IMemoryReader memoryReader = MemoryReader.getMemoryReader(getClutAddr(level, clutNumEntries, 2), (clutNumEntries - context.tex_clut_start) * 2, 2);
            for (int i = context.tex_clut_start; i < clutNumEntries; i++) {
                clut_buffer16[i] = (short) memoryReader.readNext();
            }
            clutIsDirty = false;
        }

        if (State.captureGeNextFrame) {
            log.info("Capture readClut16");
            CaptureManager.captureRAM(context.tex_clut_addr, clutNumEntries * 2);
        }

        return clut_buffer16;
    }

    private int[] readClut32(int level) {
        int clutNumEntries = context.tex_clut_num_blocks * 8;

        // Update the clut_buffer only if some clut parameters have been changed
        // since last update.
        if (clutIsDirty) {
            IMemoryReader memoryReader = MemoryReader.getMemoryReader(getClutAddr(level, clutNumEntries, 4), (clutNumEntries - context.tex_clut_start) * 4, 4);
            for (int i = context.tex_clut_start; i < clutNumEntries; i++) {
                clut_buffer32[i] = memoryReader.readNext();
            }
            clutIsDirty = false;
        }

        if (State.captureGeNextFrame) {
            log.info("Capture readClut32");
            CaptureManager.captureRAM(context.tex_clut_addr, clutNumEntries * 4);
        }

        return clut_buffer32;
    }

    private int getClutIndex(int index) {
        return ((context.tex_clut_start + index) >> context.tex_clut_shift) & context.tex_clut_mask;
    }

    // UnSwizzling based on pspplayer
    private Buffer unswizzleTextureFromMemory(int texaddr, int bytesPerPixel, int level) {
        int rowWidth = (bytesPerPixel > 0) ? (context.texture_buffer_width[level] * bytesPerPixel) : (context.texture_buffer_width[level] / 2);
        int pitch = rowWidth / 4;
        int bxc = rowWidth / 16;
        int byc = Math.max((context.texture_height[level] + 7) / 8, 1);

        int ydest = 0;

        IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, 4);
        for (int by = 0; by < byc; by++) {
            if (rowWidth >= 16) {
                int xdest = ydest;
                for (int bx = 0; bx < bxc; bx++) {
                    int dest = xdest;
                    for (int n = 0; n < 8; n++) {
                        tmp_texture_buffer32[dest] = memoryReader.readNext();
                        tmp_texture_buffer32[dest + 1] = memoryReader.readNext();
                        tmp_texture_buffer32[dest + 2] = memoryReader.readNext();
                        tmp_texture_buffer32[dest + 3] = memoryReader.readNext();

                        dest += pitch;
                    }
                    xdest += 4;
                }
                ydest += (rowWidth * 8) / 4;
            } else if (rowWidth == 8) {
            	for (int n = 0; n < 8; n++, ydest += 2) {
                    tmp_texture_buffer32[ydest] = memoryReader.readNext();
                    tmp_texture_buffer32[ydest + 1] = memoryReader.readNext();
                    memoryReader.skip(2);
            	}
            } else if (rowWidth == 4) {
            	for (int n = 0; n < 8; n++, ydest++) {
                    tmp_texture_buffer32[ydest] = memoryReader.readNext();
                    memoryReader.skip(3);
            	}
            } else if (rowWidth == 2) {
            	for (int n = 0; n < 4; n++, ydest++) {
            		int n1 = memoryReader.readNext() & 0xFFFF;
            		memoryReader.skip(3);
            		int n2 = memoryReader.readNext() & 0xFFFF;
                    memoryReader.skip(3);
                    tmp_texture_buffer32[ydest] = n1 | (n2 << 16);
            	}
            } else if (rowWidth == 1) {
            	for (int n = 0; n < 2; n++, ydest++) {
            		int n1 = memoryReader.readNext() & 0xFF;
            		memoryReader.skip(3);
            		int n2 = memoryReader.readNext() & 0xFF;
                    memoryReader.skip(3);
            		int n3 = memoryReader.readNext() & 0xFF;
                    memoryReader.skip(3);
            		int n4 = memoryReader.readNext() & 0xFF;
                    memoryReader.skip(3);
                    tmp_texture_buffer32[ydest] = n1 | (n2 << 8) | (n3 << 16) | (n4 << 24);
            	}
            }
        }

        if (State.captureGeNextFrame) {
            log.info("Capture unswizzleTextureFromMemory");
            CaptureManager.captureRAM(texaddr, rowWidth * context.texture_height[level]);
        }

        return IntBuffer.wrap(tmp_texture_buffer32);
    }

    private String getArgumentLog(int normalArgument) {
        if (normalArgument == 0) {
            return "(0)"; // a very common case...
        }

        return String.format("(hex=%08X,int=%d,float=%f)", normalArgument, normalArgument, floatArgument(normalArgument));
    }

    public void executeCommand(int instruction) {
        int normalArgument = intArgument(instruction);
        // Compute floatArgument only on demand, most commands do not use it.
        //float floatArgument = floatArgument(instruction);

        int command = command(instruction);
        currentCMDValues.put(command, normalArgument);
        if (isLogInfoEnabled) {
            commandStatistics[command].start();
        }
        switch (command) {
            case NOP:
                if (isLogDebugEnabled) {
                    log(helper.getCommandString(NOP));
                }

                // Check if we are not reading from an invalid memory region.
                // Abort the list if this is the case.
                // This is only done in the NOP command to not impact performance.
                checkCurrentListPc();
                break;

            case VADDR:
                vinfo.ptr_vertex = currentList.getAddress(normalArgument);
                if (isLogDebugEnabled) {
                    log(helper.getCommandString(VADDR) + " " + String.format("%08x", vinfo.ptr_vertex));
                }
                break;

            case IADDR:
                vinfo.ptr_index = currentList.getAddress(normalArgument);
                if (isLogDebugEnabled) {
                    log(helper.getCommandString(IADDR) + " " + String.format("%08x", vinfo.ptr_index));
                }
                break;

            case PRIM:
                executeCommandPRIM(normalArgument);
                break;

            case BEZIER:
                int ucount = normalArgument & 0xFF;
                int vcount = (normalArgument >> 8) & 0xFF;
                if (isLogDebugEnabled) {
                    log(helper.getCommandString(BEZIER) + " ucount=" + ucount + ", vcount=" + vcount);
                }

                updateGeBuf();
                loadTexture();

                drawBezier(ucount, vcount);
                break;

            case SPLINE: {
                // Number of control points.
                int sp_ucount = normalArgument & 0xFF;
                int sp_vcount = (normalArgument >> 8) & 0xFF;
                // Knot types.
                int sp_utype = (normalArgument >> 16) & 0x3;
                int sp_vtype = (normalArgument >> 18) & 0x3;

                if (isLogDebugEnabled) {
                    log(helper.getCommandString(SPLINE) + " sp_ucount=" + sp_ucount + ", sp_vcount=" + sp_vcount +
                            " sp_utype=" + sp_utype + ", sp_vtype=" + sp_vtype);
                }

                updateGeBuf();
                loadTexture();

                drawSpline(sp_ucount, sp_vcount, sp_utype, sp_vtype);
                break;
            }

            case BBOX:
                executeCommandBBOX(normalArgument);
                break;

            case JUMP: {
                int oldPc = currentList.pc;
                currentList.jump(normalArgument);
                int newPc = currentList.pc;
                if (isLogDebugEnabled) {
                    log(String.format("%s old PC: 0x%08X, new PC: 0x%08X", helper.getCommandString(JUMP), oldPc, newPc));
                }
                break;
            }

            case BJUMP:
                executeCommandBJUMP(normalArgument);
                break;

            case CALL: {
                int oldPc = currentList.pc;
                currentList.call(normalArgument);
                int newPc = currentList.pc;
                if (isLogDebugEnabled) {
                    log(String.format("%s old PC: 0x%08X, new PC: 0x%08X", helper.getCommandString(CALL), oldPc, newPc));
                }
                break;
            }

            case RET: {
                int oldPc = currentList.pc;
                currentList.ret();
                int newPc = currentList.pc;
                if (isLogDebugEnabled) {
                    log(String.format("%s old PC: 0x%08X, new PC: 0x%08X", helper.getCommandString(RET), oldPc, newPc));
                }
                break;
            }

            case END:
                // Try to end the current list.
                // The list only ends (isEnded() == true) if FINISH was called previously.
                // In SIGNAL + END cases, isEnded() still remains false.
                currentList.endList();
                currentList.pauseList();
                if (isLogDebugEnabled) {
                    log(helper.getCommandString(END) + " pc=0x" + Integer.toHexString(currentList.pc));
                }
                updateGeBuf();
                break;

            case SIGNAL:
                int behavior = (normalArgument >> 16) & 0xFF;
                int signal = normalArgument & 0xFFFF;
                if (behavior < 1 || behavior > 3) {
                    if (isLogWarnEnabled) {
                        log(helper.getCommandString(SIGNAL) + " (behavior=" + behavior + ",signal=0x" + Integer.toHexString(signal) + ") unknown behavior");
                    }
                } else if (isLogDebugEnabled) {
                    log(helper.getCommandString(SIGNAL) + " (behavior=" + behavior + ",signal=0x" + Integer.toHexString(signal) + ")");
                }
                currentList.clearRestart();
                currentList.pushSignalCallback(currentList.id, behavior, signal);
                break;

            case FINISH:
                if (isLogDebugEnabled) {
                    log(helper.getCommandString(FINISH) + " " + getArgumentLog(normalArgument));
                }
                currentList.clearRestart();
                currentList.finishList();
                currentList.pushFinishCallback(currentList.id, normalArgument);
                break;

            case BASE:
            	context.base = (normalArgument << 8) & 0xff000000;
                // Bits of (normalArgument & 0x0000FFFF) are ignored
                // (tested: "Ape Escape On the Loose")
                if (isLogDebugEnabled) {
                    log(helper.getCommandString(BASE) + " " + String.format("%08x", context.base));
                }
                break;

            case VTYPE: {
                int old_transform_mode = context.transform_mode;
                boolean old_vertex_hasColor = vinfo.color != 0;
                vinfo.processType(normalArgument);
                context.transform_mode = (normalArgument >> 23) & 0x1;
                boolean vertex_hasColor = vinfo.color != 0;

                //Switching from 2D to 3D or 3D to 2D?
                if (old_transform_mode != context.transform_mode) {
                    projectionMatrixUpload.setChanged(true);
                    modelMatrixUpload.setChanged(true);
                    viewMatrixUpload.setChanged(true);
                    textureMatrixUpload.setChanged(true);
                    viewportChanged = true;
                    depthChanged = true;
                    materialChanged = true;
                    // Switching from 2D to 3D?
                    if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD) {
                        lightingChanged = true;
                    }
                } else if (old_vertex_hasColor != vertex_hasColor) {
                    // Materials have to be reloaded when the vertex color presence is changing
                    materialChanged = true;
                }

                if (isLogDebugEnabled) {
                    log(helper.getCommandString(VTYPE) + " " + vinfo.toString());
                }
                break;
            }

            case OFFSET_ADDR:
            	context.baseOffset = normalArgument << 8;
                if (isLogDebugEnabled) {
                    log(String.format("%s 0x%08X", helper.getCommandString(OFFSET_ADDR), context.baseOffset));
                }
                break;

            case ORIGIN_ADDR:
            	context.baseOffset = currentList.pc - 4;
                if (normalArgument != 0) {
                    log.warn(String.format("%s unknown argument 0x%08X", helper.getCommandString(ORIGIN_ADDR), normalArgument));
                } else if (isLogDebugEnabled) {
                    log(String.format("%s 0x%08X originAddr=0x%08X", helper.getCommandString(ORIGIN_ADDR), normalArgument, context.baseOffset));
                }
                break;

            case REGION1:
            	context.region_x1 = normalArgument & 0x3ff;
            	context.region_y1 = (normalArgument >> 10) & 0x3ff;
            	scissorChanged = true;
                break;

            case REGION2:
            	context.region_x2 = normalArgument & 0x3ff;
            	context.region_y2 = (normalArgument >> 10) & 0x3ff;
            	context.region_width = (context.region_x2 + 1) - context.region_x1;
            	context.region_height = (context.region_y2 + 1) - context.region_y1;
                if (isLogDebugEnabled) {
                    log("drawRegion(" + context.region_x1 + "," + context.region_y1 + "," + context.region_width + "," + context.region_height + ")");
                }
            	scissorChanged = true;
                break;

            /*
             * Lighting enable/disable
             */
            case LTE: {
            	context.lightingFlag.setEnabled(normalArgument);
                if (context.lightingFlag.isEnabled()) {
                    lightingChanged = true;
                    materialChanged = true;
                }
                break;
            }

            /*
             * Individual lights enable/disable
             */
            case LTE0:
            case LTE1:
            case LTE2:
            case LTE3: {
                int lnum = command - LTE0;
                EnableDisableFlag lightFlag = context.lightFlags[lnum];
                lightFlag.setEnabled(normalArgument);
                if (lightFlag.isEnabled()) {
                    lightingChanged = true;
                }
                break;
            }

            case CPE:
            	context.clipPlanesFlag.setEnabled(normalArgument);
                break;

            case BCE:
            	context.cullFaceFlag.setEnabled(normalArgument);
                break;

            case TME:
            	context.textureFlag.setEnabled(normalArgument);
                break;

            case FGE:
            	context.fogFlag.setEnabled(normalArgument);
                if (context.fogFlag.isEnabled()) {
                	re.setFogHint();
                }
                break;

            case DTE:
            	context.ditherFlag.setEnabled(normalArgument);
                break;

            case ABE:
            	context.blendFlag.setEnabled(normalArgument);
                break;

            case ATE:
            	context.alphaTestFlag.setEnabled(normalArgument);
                break;

            case ZTE:
            	context.depthTestFlag.setEnabled(normalArgument);
                if (context.depthTestFlag.isEnabled()) {
                    // OpenGL requires the Depth parameters to be reloaded
                    depthChanged = true;
                }
                break;

            case STE:
            	context.stencilTestFlag.setEnabled(normalArgument);
                break;

            case AAE:
            	context.lineSmoothFlag.setEnabled(normalArgument);
                if (context.lineSmoothFlag.isEnabled()) {
                	re.setLineSmoothHint();
                }
                break;

            case PCE: {
            	context.patchCullFaceFlag.setEnabled(normalArgument);
                break;
            }

            case CTE: {
            	context.colorTestFlag.setEnabled(normalArgument);
                break;
            }

            case LOE:
            	context.colorLogicOpFlag.setEnabled(normalArgument);
                break;

            /*
             * Skinning
             */
            case BOFS: {
                boneMatrixIndex = normalArgument;
                if (isLogDebugEnabled) {
                    log("bone matrix offset", normalArgument);
                }
                break;
            }

            case BONE:
            	executeCommandBONE(normalArgument);
                break;

            /*
             * Morphing
             */
            case MW0:
            case MW1:
            case MW2:
            case MW3:
            case MW4:
            case MW5:
            case MW6:
            case MW7: {
                int index = command - MW0;
                float floatArgument = floatArgument(normalArgument);
                context.morph_weight[index] = floatArgument;
                re.setMorphWeight(index, floatArgument);
                if (isLogDebugEnabled) {
                    log("morph weight " + index, floatArgument);
                }
                break;
            }

            case PSUB:
            	context.patch_div_s = normalArgument & 0xFF;
            	context.patch_div_t = (normalArgument >> 8) & 0xFF;
                re.setPatchDiv(context.patch_div_s, context.patch_div_t);
                if (isLogDebugEnabled) {
                    log(helper.getCommandString(PSUB) + " patch_div_s=" + context.patch_div_s + ", patch_div_t=" + context.patch_div_t);
                }
                break;

            case PPRIM: {
            	context.patch_prim = (normalArgument & 0x3);
                // Primitive type to use in patch division:
                // 0 - Triangle.
                // 1 - Line.
                // 2 - Point.
                re.setPatchPrim(context.patch_prim);
                if (isLogDebugEnabled) {
                    log(helper.getCommandString(PPRIM) + " patch_prim=" + context.patch_prim);
                }
                break;
            }

            case PFACE: {
                // 0 - Clockwise oriented patch / 1 - Counter clockwise oriented patch.
            	context.patchFaceFlag.setEnabled(normalArgument);
                break;
            }

            case MMS:
                modelMatrixUpload.startUpload(normalArgument);
                if (isLogDebugEnabled) {
                    log("sceGumMatrixMode GU_MODEL " + normalArgument);
                }
                break;

            case MODEL:
                if (modelMatrixUpload.uploadValue(floatArgument(normalArgument))) {
                    log("glLoadMatrixf", context.model_uploaded_matrix);
                }
                break;

            case VMS:
                viewMatrixUpload.startUpload(normalArgument);
                if (isLogDebugEnabled) {
                    log("sceGumMatrixMode GU_VIEW " + normalArgument);
                }
                break;

            case VIEW:
                if (viewMatrixUpload.uploadValue(floatArgument(normalArgument))) {
                    log("glLoadMatrixf", context.view_uploaded_matrix);
                }
                break;

            case PMS:
                projectionMatrixUpload.startUpload(normalArgument);
                if (isLogDebugEnabled) {
                    log("sceGumMatrixMode GU_PROJECTION " + normalArgument);
                }
                break;

            case PROJ:
                if (projectionMatrixUpload.uploadValue(floatArgument(normalArgument))) {
                    log("glLoadMatrixf", context.proj_uploaded_matrix);
                }
                break;

            case TMS:
                textureMatrixUpload.startUpload(normalArgument);
                if (isLogDebugEnabled) {
                    log("sceGumMatrixMode GU_TEXTURE " + normalArgument);
                }
                break;

            case TMATRIX:
                if (textureMatrixUpload.uploadValue(floatArgument(normalArgument))) {
                    log("glLoadMatrixf", context.texture_uploaded_matrix);
                }
                break;

            case XSCALE: {
                int old_viewport_width = context.viewport_width;
                context.viewport_width = (int) floatArgument(normalArgument);
                if (old_viewport_width != context.viewport_width) {
                    viewportChanged = true;
                }
                break;
            }

            case YSCALE: {
                int old_viewport_height = context.viewport_height;
                context.viewport_height = (int) floatArgument(normalArgument);
                if (old_viewport_height != context.viewport_height) {
                    viewportChanged = true;
                }
                break;
            }

            case ZSCALE: {
                float old_zscale = context.zscale;
                float floatArgument = floatArgument(normalArgument);
                context.zscale = floatArgument / 65535.f;
                if (old_zscale != context.zscale) {
                    depthChanged = true;
                }

                if (isLogDebugEnabled) {
                    log(helper.getCommandString(ZSCALE) + " " + floatArgument);
                }
                break;
            }

            case XPOS: {
                int old_viewport_cx = context.viewport_cx;
                context.viewport_cx = (int) floatArgument(normalArgument);
                if (old_viewport_cx != context.viewport_cx) {
                    viewportChanged = true;
                }
                break;
            }

            case YPOS: {
                int old_viewport_cy = context.viewport_cy;
                context.viewport_cy = (int) floatArgument(normalArgument);
                if (old_viewport_cy != context.viewport_cy) {
                    viewportChanged = true;
                }

                // Log only on the last called command (always XSCALE -> YSCALE -> XPOS -> YPOS).
                if (isLogDebugEnabled) {
                    log.debug("sceGuViewport(cx=" + context.viewport_cx + ", cy=" + context.viewport_cy + ", w=" + context.viewport_width + " h=" + context.viewport_height + ")");
                }
                break;
            }

            case ZPOS: {
                float old_zpos = context.zpos;
                float floatArgument = floatArgument(normalArgument);
                context.zpos = floatArgument / 65535.f;
                if (old_zpos != context.zpos) {
                    depthChanged = true;
                }

                if (isLogDebugEnabled) {
                    log(helper.getCommandString(ZPOS), floatArgument);
                }
                break;
            }

            /*
             * Texture transformations
             */
            case USCALE: {
                float old_tex_scale_x = context.tex_scale_x;
                context.tex_scale_x = floatArgument(normalArgument);

                if (old_tex_scale_x != context.tex_scale_x) {
                    textureMatrixUpload.setChanged(true);
                }
                break;
            }

            case VSCALE: {
                float old_tex_scale_y = context.tex_scale_y;
                context.tex_scale_y = floatArgument(normalArgument);

                if (old_tex_scale_y != context.tex_scale_y) {
                    textureMatrixUpload.setChanged(true);
                }

                if (isLogDebugEnabled) {
                    log("sceGuTexScale(u=" + context.tex_scale_x + ", v=" + context.tex_scale_y + ")");
                }
                break;
            }

            case UOFFSET: {
                float old_tex_translate_x = context.tex_translate_x;
                context.tex_translate_x = floatArgument(normalArgument);

                if (old_tex_translate_x != context.tex_translate_x) {
                    textureMatrixUpload.setChanged(true);
                }
                break;
            }

            case VOFFSET: {
                float old_tex_translate_y = context.tex_translate_y;
                context.tex_translate_y = floatArgument(normalArgument);

                if (old_tex_translate_y != context.tex_translate_y) {
                    textureMatrixUpload.setChanged(true);
                }

                if (isLogDebugEnabled) {
                    log("sceGuTexOffset(u=" + context.tex_translate_x + ", v=" + context.tex_translate_y + ")");
                }
                break;
            }

            case OFFSETX: {
                int old_offset_x = context.offset_x;
                context.offset_x = normalArgument >> 4;
                if (old_offset_x != context.offset_x) {
                    viewportChanged = true;
                }
                break;
            }

            case OFFSETY: {
                int old_offset_y = context.offset_y;
                context.offset_y = normalArgument >> 4;
                if (old_offset_y != context.offset_y) {
                    viewportChanged = true;
                }

                if(isLogDebugEnabled) {
                    log.debug("sceGuOffset(x=" + context.offset_x + ",y=" + context.offset_y + ")");
                }

                break;
            }

            case SHADE: {
                re.setShadeModel(normalArgument & 1);
                if (isLogDebugEnabled) {
                    log("sceGuShadeModel(" + ((normalArgument != 0) ? "smooth" : "flat") + ")");
                }
                break;
            }

            case RNORM: {
                // This seems to be taked into account when calculating the lighting
                // for the current normal.
            	context.faceNormalReverseFlag.setEnabled(normalArgument);
                break;
            }

            /*
             * Material setup
             */
            case CMAT: {
                int old_mat_flags = context.mat_flags;
                context.mat_flags = normalArgument & 7;
                if (old_mat_flags != context.mat_flags) {
                    materialChanged = true;
                }

                if (isLogDebugEnabled) {
                    log("sceGuColorMaterial " + context.mat_flags);
                }
                break;
            }

            case EMC:
            	context.mat_emissive[0] = ((normalArgument) & 255) / 255.f;
            	context.mat_emissive[1] = ((normalArgument >> 8) & 255) / 255.f;
            	context.mat_emissive[2] = ((normalArgument >> 16) & 255) / 255.f;
            	context.mat_emissive[3] = 1.f;
                materialChanged = true;
                re.setMaterialEmissiveColor(context.mat_emissive);
                if (isLogDebugEnabled) {
                    log("material emission " + String.format("r=%.1f g=%.1f b=%.1f (%08X)",
                    		context.mat_emissive[0], context.mat_emissive[1], context.mat_emissive[2], normalArgument));
                }
                break;

            case AMC:
                context.mat_ambient[0] = ((normalArgument) & 255) / 255.f;
                context.mat_ambient[1] = ((normalArgument >> 8) & 255) / 255.f;
                context.mat_ambient[2] = ((normalArgument >> 16) & 255) / 255.f;
                materialChanged = true;
                if (isLogDebugEnabled) {
                    log(String.format("material ambient r=%.1f g=%.1f b=%.1f (%08X)",
                            context.mat_ambient[0], context.mat_ambient[1], context.mat_ambient[2], normalArgument));
                }
                break;

            case DMC:
                context.mat_diffuse[0] = ((normalArgument) & 255) / 255.f;
                context.mat_diffuse[1] = ((normalArgument >> 8) & 255) / 255.f;
                context.mat_diffuse[2] = ((normalArgument >> 16) & 255) / 255.f;
                context.mat_diffuse[3] = 1.f;
                materialChanged = true;
                if (isLogDebugEnabled) {
                    log("material diffuse " + String.format("r=%.1f g=%.1f b=%.1f (%08X)",
                            context.mat_diffuse[0], context.mat_diffuse[1], context.mat_diffuse[2], normalArgument));
                }
                break;

            case SMC:
                context.mat_specular[0] = ((normalArgument) & 255) / 255.f;
                context.mat_specular[1] = ((normalArgument >> 8) & 255) / 255.f;
                context.mat_specular[2] = ((normalArgument >> 16) & 255) / 255.f;
                context.mat_specular[3] = 1.f;
                materialChanged = true;
                if (isLogDebugEnabled) {
                    log("material specular " + String.format("r=%.1f g=%.1f b=%.1f (%08X)",
                            context.mat_specular[0], context.mat_specular[1], context.mat_specular[2], normalArgument));
                }
                break;

            case AMA: {
                context.mat_ambient[3] = ((normalArgument) & 255) / 255.f;
                materialChanged = true;
                if (isLogDebugEnabled) {
                    log(String.format("material ambient a=%.1f (%02X)",
                            context.mat_ambient[3], normalArgument & 255));
                }
                break;
            }

            case SPOW: {
                float floatArgument = floatArgument(normalArgument);
                re.setMaterialShininess(floatArgument);
                if (isLogDebugEnabled) {
                    log("material shininess " + floatArgument);
                }
                break;
            }

            case ALC:
            	context.ambient_light[0] = ((normalArgument) & 255) / 255.f;
            	context.ambient_light[1] = ((normalArgument >> 8) & 255) / 255.f;
            	context.ambient_light[2] = ((normalArgument >> 16) & 255) / 255.f;
                re.setLightModelAmbientColor(context.ambient_light);
                if (isLogDebugEnabled) {
                    log.debug(String.format("ambient light r=%.1f g=%.1f b=%.1f (%06X)",
                    		context.ambient_light[0], context.ambient_light[1], context.ambient_light[2], normalArgument));
                }
                break;

            case ALA:
            	context.ambient_light[3] = ((normalArgument) & 255) / 255.f;
                re.setLightModelAmbientColor(context.ambient_light);
                break;

            case LMODE: {
                re.setLightMode(normalArgument & 1);
                if (isLogDebugEnabled) {
                    log.debug("sceGuLightMode(" + (((normalArgument & 1) != 0) ? "GU_SEPARATE_SPECULAR_COLOR" : "GU_SINGLE_COLOR") + ")");
                }
                // Check if other values than 0 and 1 are set
                if ((normalArgument & ~1) != 0) {
                    log.warn(String.format("Unknown light mode sceGuLightMode(%06X)", normalArgument));
                }
                break;
            }

            /*
             * Light types
             */
            case LT0:
            case LT1:
            case LT2:
            case LT3: {
                int lnum = command - LT0;
                int old_light_type = context.light_type[lnum];
                int old_light_kind = context.light_kind[lnum];
                context.light_type[lnum] = (normalArgument >> 8) & 3;
                context.light_kind[lnum] = normalArgument & 3;

                if (old_light_type != context.light_type[lnum] || old_light_kind != context.light_kind[lnum]) {
                    lightingChanged = true;
                }

                switch (context.light_type[lnum]) {
                    case LIGHT_DIRECTIONAL:
                    	context.light_pos[lnum][3] = 0.f;
                        break;
                    case LIGHT_POINT:
                    	re.setLightSpotCutoff(lnum, 180);
                    	context.light_pos[lnum][3] = 1.f;
                        break;
                    case LIGHT_SPOT:
                    	context.light_pos[lnum][3] = 1.f;
                        break;
                    default:
                        error("Unknown light type : " + normalArgument);
                }
                re.setLightType(lnum, context.light_type[lnum], context.light_kind[lnum]);

                if (isLogDebugEnabled) {
                    log.debug("Light " + lnum + " type " + (normalArgument >> 8) + " kind " + (normalArgument & 3));
                }
                break;
            }

            /*
             *  Light attributes
             */

            // Position
            case LXP0:
            case LXP1:
            case LXP2:
            case LXP3:
            case LYP0:
            case LYP1:
            case LYP2:
            case LYP3:
            case LZP0:
            case LZP1:
            case LZP2:
            case LZP3: {
                int lnum = (command - LXP0) / 3;
                int component = (command - LXP0) % 3;
                float old_light_pos = context.light_pos[lnum][component];
                context.light_pos[lnum][component] = floatArgument(normalArgument);

                if (old_light_pos != context.light_pos[lnum][component]) {
                    lightingChanged = true;
                }
                break;
            }

            case LXD0:
            case LXD1:
            case LXD2:
            case LXD3:
            case LYD0:
            case LYD1:
            case LYD2:
            case LYD3:
            case LZD0:
            case LZD1:
            case LZD2:
            case LZD3: {
                int lnum = (command - LXD0) / 3;
                int component = (command - LXD0) % 3;
                float old_light_dir = context.light_dir[lnum][component];

                // OpenGL requires a normal in the opposite direction as the PSP
                context.light_dir[lnum][component] = -floatArgument(normalArgument);

                if (old_light_dir != context.light_dir[lnum][component]) {
                    lightingChanged = true;
                }
                // OpenGL parameter for light direction is set in initRendering
                // because it depends on the model/view matrix
                break;
            }

            // Light Attenuation

            // Constant
            case LCA0:
            case LCA1:
            case LCA2:
            case LCA3: {
                int lnum = (command - LCA0) / 3;
                re.setLightConstantAttenuation(lnum, floatArgument(normalArgument));
                break;
            }

            // Linear
            case LLA0:
            case LLA1:
            case LLA2:
            case LLA3: {
                int lnum = (command - LLA0) / 3;
                re.setLightLinearAttenuation(lnum, floatArgument(normalArgument));
                break;
            }

            // Quadratic
            case LQA0:
            case LQA1:
            case LQA2:
            case LQA3: {
                int lnum = (command - LQA0) / 3;
                re.setLightQuadraticAttenuation(lnum, floatArgument(normalArgument));
                break;
            }

            /*
             * Spot light exponent
             */
            case SLE0:
            case SLE1:
            case SLE2:
            case SLE3: {
                int lnum = command - SLE0;
                float old_spotLightExponent = context.spotLightExponent[lnum];
                context.spotLightExponent[lnum] = floatArgument(normalArgument);

                if (old_spotLightExponent != context.spotLightExponent[lnum]) {
                    lightingChanged = true;
                }

                if (isLogDebugEnabled) {
                    VideoEngine.log.debug("sceGuLightSpot(" + lnum + ",X," + context.spotLightExponent[lnum] + ",X)");
                }
                break;
            }

            /*
             * Spot light cutoff angle
             */
            case SLF0:
            case SLF1:
            case SLF2:
            case SLF3: {
                int lnum = command - SLF0;
                float old_spotLightCutoff = context.spotLightCutoff[lnum];

                // PSP Cutoff is cosine of angle, OpenGL expects degrees
                float floatArgument = floatArgument(normalArgument);
                float degreeCutoff = (float) Math.toDegrees(Math.acos(floatArgument));
                if ((degreeCutoff >= 0 && degreeCutoff <= 90) || degreeCutoff == 180) {
                	context.spotLightCutoff[lnum] = degreeCutoff;

                    if (old_spotLightCutoff != context.spotLightCutoff[lnum]) {
                        lightingChanged = true;
                    }

                    if (isLogDebugEnabled) {
                        log.debug("sceGuLightSpot(" + lnum + ",X,X," + floatArgument + "=" + degreeCutoff + ")");
                    }
                } else {
                    log.warn("sceGuLightSpot(" + lnum + ",X,X," + floatArgument + ") invalid argument value");
                }
                break;
            }

            // Color

            // Ambient
            case ALC0:
            case ALC1:
            case ALC2:
            case ALC3: {
                int lnum = (command - ALC0) / 3;
                context.lightAmbientColor[lnum][0] = ((normalArgument) & 255) / 255.f;
                context.lightAmbientColor[lnum][1] = ((normalArgument >> 8) & 255) / 255.f;
                context.lightAmbientColor[lnum][2] = ((normalArgument >> 16) & 255) / 255.f;
                context.lightAmbientColor[lnum][3] = 1.f;
                re.setLightAmbientColor(lnum, context.lightAmbientColor[lnum]);
                log("sceGuLightColor (GU_LIGHT0, GU_AMBIENT)");
                break;
            }

            // Diffuse
            case DLC0:
            case DLC1:
            case DLC2:
            case DLC3: {
                int lnum = (command - DLC0) / 3;
                context.lightDiffuseColor[lnum][0] = ((normalArgument) & 255) / 255.f;
                context.lightDiffuseColor[lnum][1] = ((normalArgument >> 8) & 255) / 255.f;
                context.lightDiffuseColor[lnum][2] = ((normalArgument >> 16) & 255) / 255.f;
                context.lightDiffuseColor[lnum][3] = 1.f;
                re.setLightDiffuseColor(lnum, context.lightDiffuseColor[lnum]);
                log("sceGuLightColor (GU_LIGHT0, GU_DIFFUSE)");
                break;
            }

            // Specular
            case SLC0:
            case SLC1:
            case SLC2:
            case SLC3: {
                int lnum = (command - SLC0) / 3;
                float old_lightSpecularColor0 = context.lightSpecularColor[lnum][0];
                float old_lightSpecularColor1 = context.lightSpecularColor[lnum][1];
                float old_lightSpecularColor2 = context.lightSpecularColor[lnum][2];
                context.lightSpecularColor[lnum][0] = ((normalArgument) & 255) / 255.f;
                context.lightSpecularColor[lnum][1] = ((normalArgument >> 8) & 255) / 255.f;
                context.lightSpecularColor[lnum][2] = ((normalArgument >> 16) & 255) / 255.f;
                context.lightSpecularColor[lnum][3] = 1.f;

                if (old_lightSpecularColor0 != context.lightSpecularColor[lnum][0] || old_lightSpecularColor1 != context.lightSpecularColor[lnum][1] || old_lightSpecularColor2 != context.lightSpecularColor[lnum][2]) {
                    lightingChanged = true;
                }
                re.setLightSpecularColor(lnum, context.lightDiffuseColor[lnum]);
                log("sceGuLightColor (GU_LIGHT0, GU_SPECULAR)");
                break;
            }

            case FFACE: {
            	context.frontFaceCw = normalArgument != 0;
                re.setFrontFace(context.frontFaceCw);
                if (isLogDebugEnabled) {
                    log(helper.getCommandString(FFACE) + " " + ((normalArgument != 0) ? "clockwise" : "counter-clockwise"));
                }
                break;
            }

            case FBP:
                // FBP can be called before or after FBW
            	context.fbp = (context.fbp & 0xff000000) | normalArgument;
                if (isLogDebugEnabled) {
                    log(helper.getCommandString(FBP) + " fbp=" + Integer.toHexString(context.fbp) + ", fbw=" + context.fbw);
                }
                geBufChanged = true;
                break;

            case FBW:
            	context.fbp = (context.fbp & 0x00ffffff) | ((normalArgument << 8) & 0xff000000);
            	context.fbw = normalArgument & 0xffff;
                if (isLogDebugEnabled) {
                    log(helper.getCommandString(FBW) + " fbp=" + Integer.toHexString(context.fbp) + ", fbw=" + context.fbw);
                }
                geBufChanged = true;
                break;

            case ZBP:
            	context.zbp = (context.zbp & 0xff000000) | normalArgument;
                if (isLogDebugEnabled) {
                    log("zbp=" + Integer.toHexString(context.zbp) + ", zbw=" +context. zbw);
                }
                break;

            case ZBW:
            	context.zbp = (context.zbp & 0x00ffffff) | ((normalArgument << 8) & 0xff000000);
                context.zbw = normalArgument & 0xffff;
                if (isLogDebugEnabled) {
                    log("zbp=" + Integer.toHexString(context.zbp) + ", zbw=" + context.zbw);
                }
                break;

            case TBP0:
            case TBP1:
            case TBP2:
            case TBP3:
            case TBP4:
            case TBP5:
            case TBP6:
            case TBP7: {
                int level = command - TBP0;
                int old_texture_base_pointer = context.texture_base_pointer[level];
                context.texture_base_pointer[level] = (context.texture_base_pointer[level] & 0xff000000) | normalArgument;

                if (old_texture_base_pointer != context.texture_base_pointer[level]) {
                    textureChanged = true;
                }

                if (isLogDebugEnabled) {
                    log("sceGuTexImage(level=" + level + ", X, X, X, lo(pointer=0x" + Integer.toHexString(context.texture_base_pointer[level]) + "))");
                }
                break;
            }

            case TBW0:
            case TBW1:
            case TBW2:
            case TBW3:
            case TBW4:
            case TBW5:
            case TBW6:
            case TBW7: {
                int level = command - TBW0;
                int old_texture_base_pointer = context.texture_base_pointer[level];
                int old_texture_buffer_width = context.texture_buffer_width[level];
                context.texture_base_pointer[level] = (context.texture_base_pointer[level] & 0x00ffffff) | ((normalArgument << 8) & 0xff000000);
                context.texture_buffer_width[level] = normalArgument & 0xffff;

                if (old_texture_base_pointer != context.texture_base_pointer[level] || old_texture_buffer_width != context.texture_buffer_width[level]) {
                    textureChanged = true;
                }

                if (isLogDebugEnabled) {
                    log("sceGuTexImage(level=" + level + ", X, X, texBufferWidth=" + context.texture_buffer_width[level] + ", hi(pointer=0x" + Integer.toHexString(context.texture_base_pointer[level]) + "))");
                }
                break;
            }

            case CBP: {
                int old_tex_clut_addr = context.tex_clut_addr;
                context.tex_clut_addr = (context.tex_clut_addr & 0xff000000) | normalArgument;

                clutIsDirty = true;
                if (old_tex_clut_addr != context.tex_clut_addr) {
                    textureChanged = true;
                }

                if (isLogDebugEnabled) {
                    log("sceGuClutLoad(X, lo(cbp=0x" + Integer.toHexString(context.tex_clut_addr) + "))");
                }
                break;
            }

            case CBPH: {
                int old_tex_clut_addr = context.tex_clut_addr;
                context.tex_clut_addr = (context.tex_clut_addr & 0x00ffffff) | ((normalArgument << 8) & 0x0f000000);

                clutIsDirty = true;
                if (old_tex_clut_addr != context.tex_clut_addr) {
                    textureChanged = true;
                }

                if (isLogDebugEnabled) {
                    log("sceGuClutLoad(X, hi(cbp=0x" + Integer.toHexString(context.tex_clut_addr) + "))");
                }
                break;
            }

            case TRXSBP:
                context.textureTx_sourceAddress = (context.textureTx_sourceAddress & 0xFF000000) | normalArgument;
                break;

            case TRXSBW:
                context.textureTx_sourceAddress = (context.textureTx_sourceAddress & 0x00FFFFFF) | ((normalArgument << 8) & 0xFF000000);
                context.textureTx_sourceLineWidth = normalArgument & 0x0000FFFF;

                // TODO Check when sx and sy are reset to 0. Here or after TRXKICK?
                context.textureTx_sx = 0;
                context.textureTx_sy = 0;
                break;

            case TRXDBP:
                context.textureTx_destinationAddress = (context.textureTx_destinationAddress & 0xFF000000) | normalArgument;
                break;

            case TRXDBW:
                context.textureTx_destinationAddress = (context.textureTx_destinationAddress & 0x00FFFFFF) | ((normalArgument << 8) & 0xFF000000);
                context.textureTx_destinationLineWidth = normalArgument & 0x0000FFFF;

                // TODO Check when dx and dy are reset to 0. Here or after TRXKICK?
                context.textureTx_dx = 0;
                context.textureTx_dy = 0;
                break;

            case TSIZE0:
            case TSIZE1:
            case TSIZE2:
            case TSIZE3:
            case TSIZE4:
            case TSIZE5:
            case TSIZE6:
            case TSIZE7: {
                int level = command - TSIZE0;
                int old_texture_height = context.texture_height[level];
                int old_texture_width = context.texture_width[level];
                // Astonishia Story is using normalArgument = 0x1804
                // -> use texture_height = 1 << 0x08 (and not 1 << 0x18)
                //        texture_width  = 1 << 0x04
                // The maximum texture size is 512x512: the exponent value must be [0..9]
                int height_exp2 = Math.min((normalArgument >> 8) & 0x0F, 9);
                int width_exp2 = Math.min((normalArgument) & 0x0F, 9);
                context.texture_height[level] = 1 << height_exp2;
                context.texture_width[level] = 1 << width_exp2;

                if (old_texture_height != context.texture_height[level] || old_texture_width != context.texture_width[level]) {
                    if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD && level == 0) {
                        textureMatrixUpload.setChanged(true);
                    }
                    textureChanged = true;
                }

                if (isLogDebugEnabled) {
                    log("sceGuTexImage(level=" + level + ", width=" + context.texture_width[level] + ", height=" + context.texture_height[level] + ", X, X)");
                }
                break;
            }

            case TMAP:
                int old_tex_map_mode = context.tex_map_mode;
                context.tex_map_mode = normalArgument & 3;
                context.tex_proj_map_mode = (normalArgument >> 8) & 3;

                if (old_tex_map_mode != context.tex_map_mode) {
                    textureMatrixUpload.setChanged(true);
                }

                if (isLogDebugEnabled) {
                    log("sceGuTexMapMode(mode=" + context.tex_map_mode + ", X, X)");
                    log("sceGuTexProjMapMode(mode=" + context.tex_proj_map_mode + ")");
                }
                break;

            case TEXTURE_ENV_MAP_MATRIX: {
            	context.tex_shade_u = (normalArgument >> 0) & 0x3;
            	context.tex_shade_v = (normalArgument >> 8) & 0x3;

                for (int i = 0; i < 3; i++) {
                	context.tex_envmap_matrix[i + 0] = context.light_pos[context.tex_shade_u][i];
                	context.tex_envmap_matrix[i + 4] = context.light_pos[context.tex_shade_v][i];
                }

                textureMatrixUpload.setChanged(true);
                if (isLogDebugEnabled) {
                    log("sceGuTexMapMode(X, " + context.tex_shade_u + ", " + context.tex_shade_v + ")");
                }
                break;
            }

            case TMODE: {
                int old_texture_num_mip_maps = context.texture_num_mip_maps;
                boolean old_mipmapShareClut = context.mipmapShareClut;
                boolean old_texture_swizzle = context.texture_swizzle;
                context.texture_num_mip_maps = (normalArgument >> 16) & 0x7;
                // This parameter has only a meaning when
                //  texture_storage == GU_PSM_T4 and texture_num_mip_maps > 0
                // when parameter==0: all the mipmaps share the same clut entries (normal behavior)
                // when parameter==1: each mipmap has its own clut table, 16 entries each, stored sequentially
                context.mipmapShareClut = ((normalArgument >> 8) & 0x1) == 0;
                context.texture_swizzle = ((normalArgument) & 0x1) != 0;

                if (old_texture_num_mip_maps != context.texture_num_mip_maps || old_mipmapShareClut != context.mipmapShareClut || old_texture_swizzle != context.texture_swizzle) {
                    textureChanged = true;
                }

                if (isLogDebugEnabled) {
                    log("sceGuTexMode(X, mipmaps=" + context.texture_num_mip_maps + ", mipmapShareClut=" + context.mipmapShareClut + ", swizzle=" + context.texture_swizzle + ")");
                }
                break;
            }

            case TPSM: {
                int old_texture_storage = context.texture_storage;
                context.texture_storage = normalArgument & 0xF; // Lower four bits.

                if (old_texture_storage != context.texture_storage) {
                    textureChanged = true;
                }

                if (isLogDebugEnabled) {
                    log("sceGuTexMode(tpsm=" + context.texture_storage + "(" + getPsmName(context.texture_storage) + "), X, X, X)");
                }
                break;
            }

            case CLOAD: {
                int old_tex_clut_num_blocks = context.tex_clut_num_blocks;
                context.tex_clut_num_blocks = normalArgument & 0x3F;

                clutIsDirty = true;
                if (old_tex_clut_num_blocks != context.tex_clut_num_blocks) {
                    textureChanged = true;
                }

                // Some games use the following sequence:
                // - sceGuClutLoad(num_blocks=32, X)
                // - sceGuClutLoad(num_blocks=1, X)
                // - tflush
                // - prim ... (texture data is referencing the clut entries from 32 blocks)
                //
                readClut();

                if (isLogDebugEnabled) {
                    log("sceGuClutLoad(num_blocks=" + context.tex_clut_num_blocks + ", X)");
                }
                break;
            }

            case CMODE: {
                int old_tex_clut_mode = context.tex_clut_mode;
                int old_tex_clut_shift = context.tex_clut_shift;
                int old_tex_clut_mask = context.tex_clut_mask;
                int old_tex_clut_start = context.tex_clut_start;
                context.tex_clut_mode = normalArgument & 0x03;
                context.tex_clut_shift = (normalArgument >> 2) & 0x1F;
                context.tex_clut_mask = (normalArgument >> 8) & 0xFF;
                context.tex_clut_start = (normalArgument >> 16) & 0x1F;

                clutIsDirty = true;
                if (old_tex_clut_mode != context.tex_clut_mode || old_tex_clut_shift != context.tex_clut_shift || old_tex_clut_mask != context.tex_clut_mask || old_tex_clut_start != context.tex_clut_start) {
                    textureChanged = true;
                }

                if (isLogDebugEnabled) {
                    log("sceGuClutMode(cpsm=" + context.tex_clut_mode + "(" + getPsmName(context.tex_clut_mode) + "), shift=" + context.tex_clut_shift + ", mask=0x" + Integer.toHexString(context.tex_clut_mask) + ", start=" + context.tex_clut_start + ")");
                }
                break;
            }

            case TFLT: {
                int old_tex_mag_filter = context.tex_mag_filter;
                int old_tex_min_filter = context.tex_min_filter;

                context.tex_min_filter = normalArgument & 0x7;
                context.tex_mag_filter = (normalArgument >> 8) & 0x1;

                if (isLogDebugEnabled) {
                    log("sceGuTexFilter(min=" + context.tex_min_filter + ", mag=" + context.tex_mag_filter + ") (mm#" + context.texture_num_mip_maps + ")");
                }

                if (context.tex_min_filter == TFLT_UNKNOW1 || context.tex_min_filter == TFLT_UNKNOW2) {
                    log.warn("Unknown minimizing filter " + (normalArgument & 0xFF));
                    context.tex_min_filter = TFLT_NEAREST;
                }

                if (old_tex_mag_filter != context.tex_mag_filter || old_tex_min_filter != context.tex_min_filter) {
                    textureChanged = true;
                }
                break;
            }

            case TWRAP: {
            	context.tex_wrap_s = normalArgument & 0xFF;
            	context.tex_wrap_t = (normalArgument >> 8) & 0xFF;

                if (context.tex_wrap_s > TWRAP_WRAP_MODE_CLAMP) {
                    log.warn(helper.getCommandString(TWRAP) + " unknown wrap mode " + context.tex_wrap_s);
                    context.tex_wrap_s = TWRAP_WRAP_MODE_REPEAT;
                }
                if (context.tex_wrap_t > TWRAP_WRAP_MODE_CLAMP) {
                    log.warn(helper.getCommandString(TWRAP) + " unknown wrap mode " + context.tex_wrap_t);
                    context.tex_wrap_t = TWRAP_WRAP_MODE_REPEAT;
                }
                break;
            }

            case TBIAS: {
            	context.tex_mipmap_mode = normalArgument & 0x3;
            	context.tex_mipmap_bias_int = (int) (byte) (normalArgument >> 16); // Signed 8-bit integer
            	context.tex_mipmap_bias = context.tex_mipmap_bias_int / 16.0f;
                if (isLogDebugEnabled) {
                    log.debug("sceGuTexLevelMode(mode=" + context.tex_mipmap_mode + ", bias=" + context.tex_mipmap_bias + ")");
                }
                break;
            }

            case TFUNC:
                executeCommandTFUNC(normalArgument);
                break;

            case TEC: {
            	context.tex_env_color[0] = ((normalArgument) & 255) / 255.f;
            	context.tex_env_color[1] = ((normalArgument >> 8) & 255) / 255.f;
            	context.tex_env_color[2] = ((normalArgument >> 16) & 255) / 255.f;
            	context.tex_env_color[3] = 1.f;
                re.setTextureEnvColor(context.tex_env_color);

                if (isLogDebugEnabled) {
                    log(String.format("sceGuTexEnvColor %08X (no alpha)", normalArgument));
                }
                break;
            }

            case TFLUSH: {
                // Do not load the texture right now, clut parameters can still be
                // defined after the TFLUSH and before the PRIM command.
                // Delay the texture loading until the PRIM command.
                if (isLogDebugEnabled) {
                    log("tflush (deferring to prim)");
                }
                break;
            }

            case TSYNC: {
                // Probably synchronizing the GE when a drawing result
            	// is used as a texture. Currently ignored.
                if (isLogDebugEnabled) {
                    log(helper.getCommandString(TSYNC) + " waiting for drawing.");
                }
                break;
            }

            case FFAR:
            	context.fog_far = floatArgument(normalArgument);
                break;

            case FDIST:
            	context.fog_dist = floatArgument(normalArgument);
                if ((context.fog_far != 0.0f) && (context.fog_dist != 0.0f)) {
                    float end = context.fog_far;
                    float start = end - (1 / context.fog_dist);
                    re.setFogDist(start, end);
                }
                break;

            case FCOL:
            	context.fog_color[0] = ((normalArgument) & 255) / 255.f;
                context.fog_color[1] = ((normalArgument >> 8) & 255) / 255.f;
                context.fog_color[2] = ((normalArgument >> 16) & 255) / 255.f;
                context.fog_color[3] = 1.f;
                re.setFogColor(context.fog_color);

                if (isLogDebugEnabled) {
                    log(String.format("sceGuFog(X, X, color=%08X) (no alpha)", normalArgument));
                }
                break;

            case TSLOPE: {
            	context.tslope_level = floatArgument(normalArgument);
                if (isLogDebugEnabled) {
                    log(helper.getCommandString(TSLOPE) + " tslope_level=" + context.tslope_level);
                }
                break;
            }

            case PSM:
            	context.psm = normalArgument;
                if (isLogDebugEnabled) {
                    log("psm=" + normalArgument + "(" + getPsmName(normalArgument) + ")");
                }
                geBufChanged = true;
                break;

            case CLEAR:
                executeCommandCLEAR(normalArgument);
                break;

            case SCISSOR1:
            	context.scissor_x1 = normalArgument & 0x3ff;
            	context.scissor_y1 = (normalArgument >> 10) & 0x3ff;
            	scissorChanged = true;
                break;

            case SCISSOR2:
            	context.scissor_x2 = normalArgument & 0x3ff;
            	context.scissor_y2 = (normalArgument >> 10) & 0x3ff;
            	context.scissor_width = 1 + context.scissor_x2 - context.scissor_x1;
            	context.scissor_height = 1 + context.scissor_y2 - context.scissor_y1;
                if (isLogDebugEnabled) {
                    log("sceGuScissor(" + context.scissor_x1 + "," + context.scissor_y1 + "," + context.scissor_width + "," + context.scissor_height + ")");
                }
            	scissorChanged = true;
                break;

            case NEARZ: {
                float old_nearZ = context.nearZ;
                context.nearZ = (normalArgument & 0xFFFF) / (float) 0xFFFF;
                if (old_nearZ != context.nearZ) {
                    depthChanged = true;
                }
                break;
            }

            case FARZ: {
                float old_farZ = context.farZ;
                context.farZ = (normalArgument & 0xFFFF) / (float) 0xFFFF;
                if (old_farZ != context.farZ) {
                    // OpenGL requires the Depth parameters to be reloaded
                    depthChanged = true;
                }

                if (depthChanged) {
                    re.setDepthRange(context.zpos, context.zscale, context.nearZ, context.farZ);
                }

                if (isLogDebugEnabled) {
                    log.debug("sceGuDepthRange(" + context.nearZ + ", " + context.farZ + ")");
                }
                break;
            }

            case CTST: {
            	context.colorTestFunc = normalArgument & 3;
                re.setColorTestFunc(context.colorTestFunc);
                break;
            }

            case CREF: {
            	context.colorTestRef[0] = (normalArgument) & 0xFF;
            	context.colorTestRef[1] = (normalArgument >> 8) & 0xFF;
            	context.colorTestRef[2] = (normalArgument >> 16) & 0xFF;
                re.setColorTestReference(context.colorTestRef);
                break;
            }

            case CMSK: {
            	context.colorTestMsk[0] = (normalArgument) & 0xFF;
                context.colorTestMsk[1] = (normalArgument >> 8) & 0xFF;
                context.colorTestMsk[2] = (normalArgument >> 16) & 0xFF;
                re.setColorTestMask(context.colorTestMsk);
                break;
            }

            case ATST: {
                int func = normalArgument & 0xFF;
                if (func > ATST_PASS_PIXEL_IF_GREATER_OR_EQUAL) {
                    log.warn("sceGuAlphaFunc unhandled func " + func);
                    func = ATST_ALWAYS_PASS_PIXEL;
                }
                int ref = (normalArgument >> 8) & 0xFF;
                re.setAlphaFunc(func, ref);

                if (isLogDebugEnabled) {
                	log("sceGuAlphaFunc(" + func + "," + ref + ")");
                }
                break;
            }

            case STST: {
            	context.stencilFunc = normalArgument & 0xFF;
            	if (context.stencilFunc > STST_FUNCTION_PASS_TEST_IF_GREATER_OR_EQUAL) {
            		log.warn("Unknown stencil function " + context.stencilFunc);
            		context.stencilFunc = STST_FUNCTION_ALWAYS_PASS_STENCIL_TEST;
            	}
                context.stencilRef = (normalArgument >> 8) & 0xff;
                context.stencilMask = (normalArgument >> 16) & 0xff;
                re.setStencilFunc(context.stencilFunc, context.stencilRef, context.stencilMask);

                if (isLogDebugEnabled) {
                	log("sceGuStencilFunc(func=" + (normalArgument & 0xFF) + ", ref=" + context.stencilRef + ", mask=" + context.stencilMask + ")");
                }
                break;
            }

            case SOP: {
                context.stencilOpFail = getStencilOp(normalArgument & 0xFF);
                context.stencilOpZFail = getStencilOp((normalArgument >> 8) & 0xFF);
                context.stencilOpZPass = getStencilOp((normalArgument >> 16) & 0xFF);
                re.setStencilOp(context.stencilOpFail, context.stencilOpZFail, context.stencilOpZPass);

                if (isLogDebugEnabled) {
                	log("sceGuStencilOp(fail=" + (normalArgument & 0xFF) + ", zfail=" + ((normalArgument >> 8) & 0xFF) + ", zpass=" + ((normalArgument >> 16) & 0xFF) + ")");
                }
                break;
            }

            case ZTST: {
                int oldDepthFunc = context.depthFunc;

                context.depthFunc = normalArgument & 0xFF;
                if (context.depthFunc > ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER_OR_EQUAL) {
                	error(String.format("%s unknown depth function %d", commandToString(ZTST), context.depthFunc));
                	context.depthFunc = ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS;
                }

                if (oldDepthFunc != context.depthFunc) {
                    depthChanged = true;
                }

                if (isLogDebugEnabled) {
                    log("sceGuDepthFunc(" + normalArgument + ")");
                }
                break;
            }

            case ALPHA: {
                int old_blend_src = context.blend_src;
                int old_blend_dst = context.blend_dst;
                context.blend_src = normalArgument & 0xF;
                context.blend_dst = (normalArgument >> 4) & 0xF;
                context.blendEquation = (normalArgument >> 8) & 0xF;
                if (context.blendEquation > ALPHA_SOURCE_BLEND_OPERATION_ABSOLUTE_VALUE) {
                    log.warn("Unhandled blend operation " + context.blendEquation);
                    context.blendEquation = ALPHA_SOURCE_BLEND_OPERATION_ADD;
                }

                re.setBlendEquation(context.blendEquation);

                if (old_blend_src != context.blend_src || old_blend_dst != context.blend_dst) {
                    blendChanged = true;
                }

                if (isLogDebugEnabled) {
                    log("sceGuBlendFunc(op=" + context.blendEquation + ", src=" + context.blend_src + ", dst=" + context.blend_dst + ")");
                }
                break;
            }

            case SFIX: {
                float old_sfix_color0 = context.sfix_color[0];
                float old_sfix_color1 = context.sfix_color[1];
                float old_sfix_color2 = context.sfix_color[2];
                context.sfix_color[0] = ((normalArgument) & 255) / 255.f;
                context.sfix_color[1] = ((normalArgument >> 8) & 255) / 255.f;
                context.sfix_color[2] = ((normalArgument >> 16) & 255) / 255.f;
                context.sfix_color[3] = 1.f;

                if (old_sfix_color0 != context.sfix_color[0] || old_sfix_color1 != context.sfix_color[1] || old_sfix_color2 != context.sfix_color[2]) {
                    blendChanged = true;
                }

                if (isLogDebugEnabled) {
                    log(String.format("%s : 0x%08X", helper.getCommandString(command), normalArgument));
                }
                break;
            }

            case DFIX: {
                float old_dfix_color0 = context.dfix_color[0];
                float old_dfix_color1 = context.dfix_color[1];
                float old_dfix_color2 = context.dfix_color[2];
                context.dfix_color[0] = ((normalArgument) & 255) / 255.f;
                context.dfix_color[1] = ((normalArgument >> 8) & 255) / 255.f;
                context.dfix_color[2] = ((normalArgument >> 16) & 255) / 255.f;
                context.dfix_color[3] = 1.f;

                if (old_dfix_color0 != context.dfix_color[0] || old_dfix_color1 != context.dfix_color[1] || old_dfix_color2 != context.dfix_color[2]) {
                    blendChanged = true;
                }

                if (isLogDebugEnabled) {
                    log(String.format("%s : 0x%08X", helper.getCommandString(command), normalArgument));
                }
                break;
            }

            case DTH0:
            	context.dither_matrix[0] = (normalArgument) & 0xF;
                context.dither_matrix[1] = (normalArgument >> 4) & 0xF;
                context.dither_matrix[2] = (normalArgument >> 8) & 0xF;
                context.dither_matrix[3] = (normalArgument >> 12) & 0xF;
                break;

            case DTH1:
            	context.dither_matrix[4] = (normalArgument) & 0xF;
                context.dither_matrix[5] = (normalArgument >> 4) & 0xF;
                context.dither_matrix[6] = (normalArgument >> 8) & 0xF;
                context.dither_matrix[7] = (normalArgument >> 12) & 0xF;
                break;

            case DTH2:
            	context.dither_matrix[8] = (normalArgument) & 0xF;
                context.dither_matrix[9] = (normalArgument >> 4) & 0xF;
                context.dither_matrix[10] = (normalArgument >> 8) & 0xF;
                context.dither_matrix[11] = (normalArgument >> 12) & 0xF;
                break;

            case DTH3:
            	context.dither_matrix[12] = (normalArgument) & 0xF;
                context.dither_matrix[13] = (normalArgument >> 4) & 0xF;
                context.dither_matrix[14] = (normalArgument >> 8) & 0xF;
                context.dither_matrix[15] = (normalArgument >> 12) & 0xF;

                // The dither matrix's values can vary between -8 and 7.
                // The most significant bit acts as sign bit.
                // Translate and log only at the last command.

                for (int i = 0; i < 16; i++) {
                    if (context.dither_matrix[i] > 7) {
                    	context.dither_matrix[i] |= 0xFFFFFFF0;
                    }
                }

                if (isLogDebugEnabled) {
                    log.debug("DTH0:" + "  " + context.dither_matrix[0] + "  " + context.dither_matrix[1] + "  " + context.dither_matrix[2] + "  " + context.dither_matrix[3]);
                    log.debug("DTH1:" + "  " + context.dither_matrix[4] + "  " + context.dither_matrix[5] + "  " + context.dither_matrix[6] + "  " + context.dither_matrix[7]);
                    log.debug("DTH2:" + "  " + context.dither_matrix[8] + "  " + context.dither_matrix[9] + "  " + context.dither_matrix[10] + "  " + context.dither_matrix[11]);
                    log.debug("DTH3:" + "  " + context.dither_matrix[12] + "  " + context.dither_matrix[13] + "  " + context.dither_matrix[14] + "  " + context.dither_matrix[15]);
                }
                break;

            case LOP: {
            	re.setLogicOp(normalArgument & 0xF);
            	if (isLogDebugEnabled) {
            		log.debug("sceGuLogicalOp( LogicOp = " + normalArgument + "(" + getLOpName(normalArgument) + ")");
            	}
                break;
            }

            case ZMSK: {
                // NOTE: PSP depth mask as 1 is meant to avoid depth writes,
                //       with OpenGL it's the opposite
            	context.depthMask = (normalArgument == 0);
            	re.setDepthMask(context.depthMask);
            	if (context.depthMask) {
                    // OpenGL requires the Depth parameters to be reloaded
                    depthChanged = true;
            	}

                if (isLogDebugEnabled) {
                    log("sceGuDepthMask(" + (normalArgument != 0 ? "disableWrites" : "enableWrites") + ")");
                }
                break;
            }

            case PMSKC: {
                context.colorMask[0] = normalArgument & 0xFF;
                context.colorMask[1] = (normalArgument >> 8) & 0xFF;
                context.colorMask[2] = (normalArgument >> 16) & 0xFF;
            	re.setColorMask(context.colorMask[0], context.colorMask[1], context.colorMask[2], context.colorMask[3]);

            	if (isLogDebugEnabled) {
                    log(String.format("%s color mask=0x%06X", helper.getCommandString(PMSKC), normalArgument));
                }
                break;
            }

            case PMSKA: {
                context.colorMask[3] = normalArgument & 0xFF;
            	re.setColorMask(context.colorMask[0], context.colorMask[1], context.colorMask[2], context.colorMask[3]);

                if (isLogDebugEnabled) {
                    log(String.format("%s alpha mask=0x%02X", helper.getCommandString(PMSKA), normalArgument));
                }
                break;
            }

            case TRXKICK:
                executeCommandTRXKICK(normalArgument);
                break;

            case TRXPOS:
                context.textureTx_sx = normalArgument & 0x1FF;
                context.textureTx_sy = (normalArgument >> 10) & 0x1FF;
                break;

            case TRXDPOS:
                context.textureTx_dx = normalArgument & 0x1FF;
                context.textureTx_dy = (normalArgument >> 10) & 0x1FF;
                break;

            case TRXSIZE:
                context.textureTx_width = (normalArgument & 0x3FF) + 1;
                context.textureTx_height = ((normalArgument >> 10) & 0x1FF) + 1;
                break;

            case VSCX:
                int coordX = normalArgument & 0xFFFF;
                log.warn("Unimplemented VSCX: coordX=" + coordX);
                break;

            case VSCY:
                int coordY = normalArgument & 0xFFFF;
                log.warn("Unimplemented VSCY: coordY=" + coordY);
                break;

            case VSCZ:
                int coordZ = normalArgument & 0xFFFF;
                log.warn("Unimplemented VSCZ: coordZ=" + coordZ);
                break;

            case VTCS:
                float coordS = floatArgument(normalArgument);
                log.warn("Unimplemented VTCS: coordS=" + coordS);
                break;

            case VTCT:
                float coordT = floatArgument(normalArgument);
                log.warn("Unimplemented VTCT: coordT=" + coordT);
                break;

            case VTCQ:
                float coordQ = floatArgument(normalArgument);
                log.warn("Unimplemented VTCQ: coordQ=" + coordQ);
                break;

            case VCV:
                int colorR = normalArgument & 0xFF;
                int colorG = (normalArgument >> 8) & 0xFF;
                int colorB = (normalArgument >> 16) & 0xFF;
                log.warn("Unimplemented VCV: colorR=" + colorR + ", colorG=" + colorG + ", colorB=" + colorB);
                break;

            case VAP:
                int alpha = normalArgument & 0xFF;
                int prim_type = (normalArgument >> 8) & 0x7;
                log.warn("Unimplemented VAP: alpha=" + alpha + ", prim_type=" + prim_type);
                break;

            case VFC:
                int fog = normalArgument & 0xFF;
                log.warn("Unimplemented VFC: fog=" + fog);
                break;

            case VSCV:
                int colorR2 = normalArgument & 0xFF;
                int colorG2 = (normalArgument >> 8) & 0xFF;
                int colorB2 = (normalArgument >> 16) & 0xFF;
                log.warn("Unimplemented VSCV: colorR2=" + colorR2 + ", colorG2=" + colorG2 + ", colorB2=" + colorB2);
                break;

            case DUMMY: {
                // This command always appears before a BOFS command and seems to have
                // no special meaning.
                // The command also appears sometimes after a PRIM command.

                // Confirmed on PSP to be a dummy command and can be safely ignored.
                // This commands' normalArgument may not be always 0, as it's totally
                // discarded on the PSP.
                if (isLogDebugEnabled) {
                    log.debug("Ignored DUMMY video command.");
                }
                break;
            }

            default:
                if (isLogWarnEnabled) {
                    log.warn("Unknown/unimplemented video command [" + helper.getCommandString(command(instruction)) + "]" + getArgumentLog(normalArgument));
                }
        }
        if (isLogInfoEnabled) {
            commandStatistics[command].end();
        }
    }

    private void executeCommandCLEAR(int normalArgument) {
        if ((normalArgument & 1) == 0) {
            re.endClearMode();
            if (isLogDebugEnabled) {
            	log("clear mode end");
            }
        } else {
            // TODO Add more disabling in clear mode, we also need to reflect the change to the internal GE registers
            boolean color = (normalArgument & 0x100) != 0;
            boolean alpha = (normalArgument & 0x200) != 0;
            boolean depth = (normalArgument & 0x400) != 0;

            updateGeBuf();
            re.startClearMode(color, alpha, depth);
            if (isLogDebugEnabled) {
                log("clear mode : " + (normalArgument >> 8));
            }
        }

        blendChanged = true;
        lightingChanged = true;
        projectionMatrixUpload.setChanged(true);
        modelMatrixUpload.setChanged(true);
        viewMatrixUpload.setChanged(true);
        textureMatrixUpload.setChanged(true);
        viewportChanged = true;
        depthChanged = true;
        materialChanged = true;
    }

    private void executeCommandTFUNC(int normalArgument) {
    	context.textureFunc = normalArgument & 0x7;
    	if (context.textureFunc >= TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_UNKNOW1) {
            VideoEngine.log.warn("Unimplemented tfunc mode " + context.textureFunc);
            context.textureFunc = TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_MODULATE;
    	}

    	context.textureAlphaUsed = ((normalArgument >> 8) & 0x1) != TFUNC_FRAGMENT_DOUBLE_TEXTURE_COLOR_ALPHA_IS_IGNORED;
    	context.textureColorDoubled = ((normalArgument >> 16) & 0x1) != TFUNC_FRAGMENT_DOUBLE_ENABLE_COLOR_UNTOUCHED;

        re.setTextureFunc(context.textureFunc, context.textureAlphaUsed, context.textureColorDoubled);

        if (isLogDebugEnabled) {
            log(String.format("sceGuTexFunc mode %06X", normalArgument)
                    + (((normalArgument & 0x10000) != 0) ? " SCALE" : "")
                    + (((normalArgument & 0x100) != 0) ? " ALPHA" : ""));
        }
    }

    private void executeCommandPRIM(int normalArgument) {
        int numberOfVertex = normalArgument & 0xFFFF;
        int type = ((normalArgument >> 16) & 0x7);

        Memory mem = Memory.getInstance();
        if (!mem.isAddressGood(vinfo.ptr_vertex)) {
            // Abort here to avoid a lot of useless memory read errors...
            error(helper.getCommandString(PRIM) + " Invalid vertex address 0x" + Integer.toHexString(vinfo.ptr_vertex));
            return;
        }

        if (type > PRIM_SPRITES) {
            error(helper.getCommandString(PRIM) + " Type unhandled " + type);
            return;
        }

        updateGeBuf();
        somethingDisplayed = true;
        primCount++;

        loadTexture();

        // Logging
        if (isLogDebugEnabled) {
            switch (type) {
                case PRIM_POINT:
                    log("prim point " + numberOfVertex + "x");
                    break;
                case PRIM_LINE:
                    log("prim line " + (numberOfVertex / 2) + "x");
                    break;
                case PRIM_LINES_STRIPS:
                    log("prim lines_strips " + (numberOfVertex - 1) + "x");
                    break;
                case PRIM_TRIANGLE:
                    log("prim triangle " + (numberOfVertex / 3) + "x");
                    break;
                case PRIM_TRIANGLE_STRIPS:
                    log("prim triangle_strips " + (numberOfVertex - 2) + "x");
                    break;
                case PRIM_TRIANGLE_FANS:
                    log("prim triangle_fans " + (numberOfVertex - 2) + "x");
                    break;
                case PRIM_SPRITES:
                    log("prim sprites " + (numberOfVertex / 2) + "x");
                    break;
                default:
                    VideoEngine.log.warn("prim unhandled " + type);
                    break;
            }
        }

        boolean useVertexColor = initRendering();

        int nTexCoord = 2;
        int nColor = 4;
        int nVertex = 3;

        boolean useTexture = false;
        boolean useTextureFromNormal = false;
        boolean useTextureFromNormalizedNormal = false;
        boolean useTextureFromPosition = false;
        switch (context.tex_map_mode) {
            case TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV:
            	if (vinfo.texture != 0) {
            	    useTexture = true;
            	}
                break;

            case TMAP_TEXTURE_MAP_MODE_TEXTURE_MATRIX: {
                switch (context.tex_proj_map_mode) {
                    case TMAP_TEXTURE_PROJECTION_MODE_POSITION:
                        if (vinfo.position != 0) {
                            useTexture = true;
                            useTextureFromPosition = true;
                	        nTexCoord = nVertex;
                        }
                        break;
                    case TMAP_TEXTURE_PROJECTION_MODE_TEXTURE_COORDINATES:
                        if (vinfo.texture != 0) {
                            useTexture = true;
                        }
                        break;
                    case TMAP_TEXTURE_PROJECTION_MODE_NORMAL:
                        if (vinfo.normal != 0) {
                            useTexture = true;
                            useTextureFromNormal = true;
                            nTexCoord = 3;
                        }
                        break;
                    case TMAP_TEXTURE_PROJECTION_MODE_NORMALIZED_NORMAL:
                        if (vinfo.normal != 0) {
                            useTexture = true;
                            useTextureFromNormalizedNormal = true;
                            nTexCoord = 3;
                        }
                        break;
                }
                break;
            }

            case TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP:
                break;

            default:
                log("Unhandled texture matrix mode " + context.tex_map_mode);
                break;
        }

        vertexStatistics.start();

        vinfo.setMorphWeights(context.morph_weight);
        vinfo.setDirty();

        int numberOfWeightsForBuffer;
        boolean mustComputeWeights;
        if (vinfo.weight != 0) {
        	numberOfWeightsForBuffer = re.setBones(vinfo.skinningWeightCount, context.boneMatrixLinear);
        	mustComputeWeights = (numberOfWeightsForBuffer == 0);
        } else {
        	numberOfWeightsForBuffer = re.setBones(0, null);
        	mustComputeWeights = false;
        }

        // Do not use optimized VertexInfo reading when
        // - using Vertex Cache
        // - the Vertex are indexed
        // - the PRIM_SPRITE primitive is used where it is not supported natively
        // - the normals have to be normalized for the texture mapping
        // - the weights have to be computed and are not supported natively
        // - the vertex address is invalid
        // - tracing is enabled because it doesn't produce any trace information
        if (!useVertexCache &&
            vinfo.index == 0 &&
            (type != PRIM_SPRITES || re.canNativeSpritesPrimitive()) &&
            !useTextureFromNormalizedNormal &&
            !mustComputeWeights &&
            mem.isAddressGood(vinfo.ptr_vertex) &&
            !isLogTraceEnabled) {
        	//
            // Optimized VertexInfo reading:
            // - do not copy the info already available in the OpenGL format
            //   (native format), load it into nativeBuffer (a direct buffer
            //   is required by OpenGL).
            // - try to keep the info in "int" format when possible, convert
            //   to "float" only when necessary
            // The best case is no reading and no conversion at all when all the
            // vertex info are available in a format usable by OpenGL.
            //
            Buffer buffer = vertexInfoReader.read(vinfo, vinfo.ptr_vertex, numberOfVertex, re.canAllNativeVertexInfo());

            enableClientState(useVertexColor, useTexture);

            int stride = vertexInfoReader.getStride();
            if (buffer != null) {
            	bufferManager.setBufferData(bufferId, stride * numberOfVertex, buffer, IRenderingEngine.RE_STREAM_DRAW);
            }

            if (vertexInfoReader.hasNative()) {
                // Copy the VertexInfo from Memory to the nativeBuffer
                // (a direct buffer is required by glXXXPointer())
            	int size = vinfo.vertexSize * numberOfVertex;
            	Buffer vertexData = mem.getBuffer(vinfo.ptr_vertex, size);
            	bufferManager.setBufferData(nativeBufferId, size, vertexData, IRenderingEngine.RE_STREAM_DRAW);
            }

            if (vinfo.texture != 0 || useTexture) {
                boolean textureNative;
                int textureOffset;
                int textureType;
                if (useTextureFromNormal) {
                    textureNative = vertexInfoReader.isNormalNative();
                    textureOffset = vertexInfoReader.getNormalOffset();
                    textureType = vertexInfoReader.getNormalType();
                    nTexCoord = vertexInfoReader.getNormalNumberValues();
                } else if (useTextureFromPosition) {
                    textureNative = vertexInfoReader.isPositionNative();
                    textureOffset = vertexInfoReader.getPositionOffset();
                    textureType = vertexInfoReader.getPositionType();
                    nTexCoord = vertexInfoReader.getPositionNumberValues();
                } else {
                    textureNative = vertexInfoReader.isTextureNative();
                    textureOffset = vertexInfoReader.getTextureOffset();
                    textureType = vertexInfoReader.getTextureType();
                    nTexCoord = vertexInfoReader.getTextureNumberValues();
                }
                setTexCoordPointer(useTexture, nTexCoord, textureType, stride, textureOffset, textureNative, true);
            }
            nVertex = vertexInfoReader.getPositionNumberValues();
            nColor = vertexInfoReader.getColorNumberValues();
            int nWeight = vertexInfoReader.getWeightNumberValues();

            re.setVertexInfo(vinfo, re.canAllNativeVertexInfo(), useVertexColor);
            setColorPointer(useVertexColor, nColor, vertexInfoReader.getColorType(), stride, vertexInfoReader.getColorOffset(), vertexInfoReader.isColorNative(), true);
            setNormalPointer(vertexInfoReader.getNormalType(), stride, vertexInfoReader.getNormalOffset(), vertexInfoReader.isNormalNative(), true);
            setWeightPointer(nWeight, vertexInfoReader.getWeightType(), stride, vertexInfoReader.getWeightOffset(), vertexInfoReader.isWeightNative(), true);
            setVertexPointer(nVertex, vertexInfoReader.getPositionType(), stride, vertexInfoReader.getPositionOffset(), vertexInfoReader.isPositionNative(), true);

            re.drawArrays(type, 0, numberOfVertex);

        } else {
            // Non-optimized VertexInfo reading
            VertexInfo cachedVertexInfo = null;
            if (useVertexCache) {
                vertexCacheLookupStatistics.start();
                cachedVertexInfo = VertexCache.getInstance().getVertex(vinfo, numberOfVertex, context.bone_uploaded_matrix, numberOfWeightsForBuffer);
                vertexCacheLookupStatistics.end();
            }

            ByteBuffer byteBuffer = bufferManager.getBuffer(bufferId);
            FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
            floatBuffer.clear();

            re.setVertexInfo(vinfo, false, useVertexColor);

            switch (type) {
                case PRIM_POINT:
                case PRIM_LINE:
                case PRIM_LINES_STRIPS:
                case PRIM_TRIANGLE:
                case PRIM_TRIANGLE_STRIPS:
                case PRIM_TRIANGLE_FANS:
                	float[] normalizedNormal = new float[3];
                    if (cachedVertexInfo == null) {
                        for (int i = 0; i < numberOfVertex; i++) {
                            int addr = vinfo.getAddress(mem, i);

                            VertexState v = vinfo.readVertex(mem, addr);

                            // Do skinning first as it modifies v.p and v.n
                            if (mustComputeWeights && vinfo.position != 0) {
                                doSkinning(vinfo, v);
                            }

                            if (useTextureFromNormal) {
                                floatBuffer.put(v.n, 0, 3);
                            } else if (useTextureFromNormalizedNormal) {
                            	float normalLength = (float) Math.sqrt(v.n[0] * v.n[0] + v.n[1] * v.n[1] + v.n[2] * v.n[2]);
                            	normalizedNormal[0] = v.n[0] / normalLength;
                            	normalizedNormal[1] = v.n[1] / normalLength;
                            	normalizedNormal[2] = v.n[2] / normalLength;
                                floatBuffer.put(normalizedNormal, 0, 3);
                            } else if (useTextureFromPosition) {
                                floatBuffer.put(v.p, 0, 3);
                            } else if (useTexture) {
                                floatBuffer.put(v.t);
                            }
                            if (useVertexColor) {
                                floatBuffer.put(v.c);
                            }
                            if (vinfo.normal != 0) {
                                floatBuffer.put(v.n);
                            }
                            if (vinfo.position != 0) {
                                floatBuffer.put(v.p);
                            }
                            if (numberOfWeightsForBuffer > 0) {
                                floatBuffer.put(v.boneWeights, 0, numberOfWeightsForBuffer);
                            }

                            if (isLogTraceEnabled) {
                                if (vinfo.texture != 0 && vinfo.position != 0) {
                                    log.trace("  vertex#" + i + " (" + ((int) v.t[0]) + "," + ((int) v.t[1]) + ") at (" + ((int) v.p[0]) + "," + ((int) v.p[1]) + "," + ((int) v.p[2]) + ")");
                                }
                            }
                        }

                        if (useVertexCache) {
                            cachedVertexInfo = new VertexInfo(vinfo);
                            VertexCache.getInstance().addVertex(re, cachedVertexInfo, numberOfVertex, context.bone_uploaded_matrix, numberOfWeightsForBuffer);
                            int size = floatBuffer.position();
                            floatBuffer.rewind();
                            cachedVertexInfo.loadVertex(re, floatBuffer, size);
                        } else {
                            bufferManager.setBufferData(bufferId, floatBuffer.position() * SIZEOF_FLOAT, byteBuffer, IRenderingEngine.RE_STREAM_DRAW);
                        }
                    } else {
                        if (isLogDebugEnabled) {
                            log.debug("Reusing cached Vertex Data");
                        }
                        cachedVertexInfo.bindVertex(re);
                    }
                    setDataPointers(nVertex, useVertexColor, nColor, useTexture, nTexCoord, vinfo.normal != 0, numberOfWeightsForBuffer, cachedVertexInfo == null);
                    re.drawArrays(type, 0, numberOfVertex);
                    maxSpriteHeight = Integer.MAX_VALUE;
                    break;

                case PRIM_SPRITES:
                	re.disableFlag(IRenderingEngine.GU_CULL_FACE);

                	float[] mvpMatrix = null;
                	if (!vinfo.transform2D) {
                		mvpMatrix = new float[4 * 4];
                		// pre-Compute the MVP (Model-View-Projection) matrix
                		matrixMult(mvpMatrix, context.model_uploaded_matrix, context.view_uploaded_matrix);
                		matrixMult(mvpMatrix, mvpMatrix, context.proj_uploaded_matrix);
                	}

                	if (cachedVertexInfo == null) {
                        for (int i = 0; i < numberOfVertex; i += 2) {
                            int addr1 = vinfo.getAddress(mem, i);
                            int addr2 = vinfo.getAddress(mem, i + 1);
                            VertexState v1 = vinfo.readVertex(mem, addr1);
                            VertexState v2 = vinfo.readVertex(mem, addr2);

                            v1.p[2] = v2.p[2];

                            if (v2.p[1] > maxSpriteHeight) {
                                maxSpriteHeight = (int) v2.p[1];
                            }

                            //
                            // Texture flip tested using the GElist application:
                            // - it depends on the X and Y coordinates:
                            //   GU_TRANSFORM_3D:
                            //     X1 < X2 && Y1 < Y2 :     flipped
                            //     X1 > X2 && Y1 > Y2 :     flipped
                            //     X1 < X2 && Y1 > Y2 : not flipped
                            //     X1 > X2 && Y1 < Y2 : not flipped
                            //   GU_TRANSFORM_2D: opposite results because
                            //                    the Y-Axis is upside-down in 2D
                            //     X1 < X2 && Y1 < Y2 : not flipped
                            //     X1 > X2 && Y1 > Y2 : not flipped
                            //     X1 < X2 && Y1 > Y2 :     flipped
                            //     X1 > X2 && Y1 < Y2 :     flipped
                            // - the tests for GU_TRANSFORM_3D are based on the coordinates
                            //   after the MVP (Model-View-Projection) transformation
                            // - texture coordinates are irrelevant
                            //
                            float x1, y1, x2, y2;
                            if (mvpMatrix == null) {
                            	x1 =  v1.p[0];
                            	y1 = -v1.p[1]; // Y-Axis is upside-down in 2D
                            	x2 =  v2.p[0];
                            	y2 = -v2.p[1]; // Y-Axis is upside-down in 2D
                            } else {
                            	// Apply the MVP transformation to both position coordinates
                            	float[] mvpPosition = new float[2];
                            	vectorMult(mvpPosition, mvpMatrix, v1.p);
                            	x1 = mvpPosition[0];
                            	y1 = mvpPosition[1];
                            	vectorMult(mvpPosition, mvpMatrix, v2.p);
                            	x2 = mvpPosition[0];
                            	y2 = mvpPosition[1];
                            }
                            boolean flippedTexture = (y1 < y2 && x1 < x2) ||
                                                     (y1 > y2 && x1 > x2);

                            if (isLogDebugEnabled) {
                            	log(String.format("  sprite (%.0f,%.0f)-(%.0f,%.0f) at (%.0f,%.0f,%.0f)-(%.0f,%.0f,%.0f)%s", v1.t[0], v1.t[1], v2.t[0], v2.t[1], v1.p[0], v1.p[1], v1.p[2], v2.p[0], v2.p[1], v2.p[2], flippedTexture ? " flipped" : ""));
                            }

                            // V1
                            if (vinfo.texture != 0) {
                                floatBuffer.put(v1.t);
                            }
                            if (useVertexColor) {
                                floatBuffer.put(v2.c);
                            }
                            if (vinfo.normal != 0) {
                                floatBuffer.put(v2.n);
                            }
                            if (vinfo.position != 0) {
                                floatBuffer.put(v1.p);
                            }

                            if (vinfo.texture != 0) {
                                if (flippedTexture) {
                                    floatBuffer.put(v2.t[0]).put(v1.t[1]);
                                } else {
                                    floatBuffer.put(v1.t[0]).put(v2.t[1]);
                                }
                            }
                            if (useVertexColor) {
                                floatBuffer.put(v2.c);
                            }
                            if (vinfo.normal != 0) {
                                floatBuffer.put(v2.n);
                            }
                            if (vinfo.position != 0) {
                                floatBuffer.put(v1.p[0]).put(v2.p[1]).put(v2.p[2]);
                            }

                            // V2
                            if (vinfo.texture != 0) {
                                floatBuffer.put(v2.t);
                            }
                            if (useVertexColor) {
                                floatBuffer.put(v2.c);
                            }
                            if (vinfo.normal != 0) {
                                floatBuffer.put(v2.n);
                            }
                            if (vinfo.position != 0) {
                                floatBuffer.put(v2.p);
                            }

                            if (vinfo.texture != 0) {
                                if (flippedTexture) {
                                    floatBuffer.put(v1.t[0]).put(v2.t[1]);
                                } else {
                                    floatBuffer.put(v2.t[0]).put(v1.t[1]);
                                }
                            }
                            if (useVertexColor) {
                                floatBuffer.put(v2.c);
                            }
                            if (vinfo.normal != 0) {
                                floatBuffer.put(v2.n);
                            }
                            if (vinfo.position != 0) {
                                floatBuffer.put(v2.p[0]).put(v1.p[1]).put(v2.p[2]);
                            }
                        }
                        if (useVertexCache) {
                            cachedVertexInfo = new VertexInfo(vinfo);
                            VertexCache.getInstance().addVertex(re, cachedVertexInfo, numberOfVertex, context.bone_uploaded_matrix, numberOfWeightsForBuffer);
                            int size = floatBuffer.position();
                            floatBuffer.rewind();
                            cachedVertexInfo.loadVertex(re, floatBuffer, size);
                        } else {
                            bufferManager.setBufferData(bufferId, floatBuffer.position() * SIZEOF_FLOAT, byteBuffer, IRenderingEngine.RE_STREAM_DRAW);
                        }
                    } else {
                        if (isLogDebugEnabled) {
                            log.debug("Reusing cached Vertex Data");
                        }
                        cachedVertexInfo.bindVertex(re);
                    }
                    setDataPointers(nVertex, useVertexColor, nColor, useTexture, nTexCoord, vinfo.normal != 0, 0, cachedVertexInfo == null);
                    re.drawArrays(IRenderingEngine.RE_QUADS, 0, numberOfVertex * 2);
                    context.cullFaceFlag.updateEnabled();
                    break;
            }
        }

        vertexStatistics.end();

        // Don't capture the ram if the vertex list is embedded in the display list. TODO handle stall_addr == 0 better
        // TODO may need to move inside the loop if indices are used, or find the largest index so we can calculate the size of the vertex list
        if (State.captureGeNextFrame) {
    		if (!isVertexBufferEmbedded()) {
	            log.info("Capture PRIM");
	            CaptureManager.captureRAM(vinfo.ptr_vertex, vinfo.vertexSize * numberOfVertex);
    		}
            display.captureGeImage();
            textureChanged = true;
        }

        endRendering(useVertexColor, useTexture, numberOfVertex);

    }

    private void executeCommandTRXKICK(int normalArgument) {
    	context.textureTx_pixelSize = normalArgument & 0x1;

    	context.textureTx_sourceAddress &= Memory.addressMask;
    	context.textureTx_destinationAddress &= Memory.addressMask;

        if (isLogDebugEnabled) {
            log(helper.getCommandString(TRXKICK) + " from 0x" + Integer.toHexString(context.textureTx_sourceAddress) + "(" + context.textureTx_sx + "," + context.textureTx_sy + ") to 0x" + Integer.toHexString(context.textureTx_destinationAddress) + "(" + context.textureTx_dx + "," + context.textureTx_dy + "), width=" + context.textureTx_width + ", height=" + context.textureTx_height + ", pixelSize=" + context.textureTx_pixelSize);
        }

        usingTRXKICK = true;
        updateGeBuf();

        int pixelFormatGe = context.psm;
        int bpp = (context.textureTx_pixelSize == TRXKICK_16BIT_TEXEL_SIZE) ? 2 : 4;
        int bppGe = sceDisplay.getPixelFormatBytes(pixelFormatGe);

        memoryForGEUpdated();

        if (!display.isGeAddress(context.textureTx_destinationAddress) || bpp != bppGe) {
            if (isLogDebugEnabled) {
                if (bpp != bppGe) {
                    log(helper.getCommandString(TRXKICK) + " BPP not compatible with GE");
                } else {
                    log(helper.getCommandString(TRXKICK) + " not in Ge Address space");
                }
            }
            int width = context.textureTx_width;
            int height = context.textureTx_height;

            int srcAddress = context.textureTx_sourceAddress + (context.textureTx_sy * context.textureTx_sourceLineWidth + context.textureTx_sx) * bpp;
            int dstAddress = context.textureTx_destinationAddress + (context.textureTx_dy * context.textureTx_destinationLineWidth + context.textureTx_dx) * bpp;
            Memory memory = Memory.getInstance();
            if (context.textureTx_sourceLineWidth == width && context.textureTx_destinationLineWidth == width) {
                // All the lines are adjacent in memory,
                // copy them all in a single memcpy operation.
                int copyLength = height * width * bpp;
                if (isLogDebugEnabled) {
                    log(String.format("%s memcpy(0x%08X-0x%08X, 0x%08X, 0x%X)", helper.getCommandString(TRXKICK), dstAddress, dstAddress + copyLength, srcAddress, copyLength));
                }
                memory.memcpy(dstAddress, srcAddress, copyLength);
            } else {
                // The lines are not adjacent in memory: copy line by line.
                int copyLength = width * bpp;
                int srcLineLength = context.textureTx_sourceLineWidth * bpp;
                int dstLineLength = context.textureTx_destinationLineWidth * bpp;
                for (int y = 0; y < height; y++) {
                    if (isLogDebugEnabled) {
                        log(String.format("%s memcpy(0x%08X-0x%08X, 0x%08X, 0x%X)", helper.getCommandString(TRXKICK), dstAddress, dstAddress + copyLength, srcAddress, copyLength));
                    }
                    memory.memcpy(dstAddress, srcAddress, copyLength);
                    srcAddress += srcLineLength;
                    dstAddress += dstLineLength;
                }
            }

            if (State.captureGeNextFrame) {
                log.warn("TRXKICK outside of Ge Address space not supported in capture yet");
            }
        } else {
            int width = context.textureTx_width;
            int height = context.textureTx_height;
            int dx = context.textureTx_dx;
            int dy = context.textureTx_dy;
            int lineWidth = context.textureTx_sourceLineWidth;

            int geAddr = display.getTopAddrGe();
            dy += (context.textureTx_destinationAddress - geAddr) / (display.getBufferWidthGe() * bpp);
            dx += ((context.textureTx_destinationAddress - geAddr) % (display.getBufferWidthGe() * bpp)) / bpp;

            if (isLogDebugEnabled) {
                log(helper.getCommandString(TRXKICK) + " in Ge Address space: dx=" + dx + ", dy=" + dy + ", width=" + width + ", height=" + height + ", lineWidth=" + lineWidth + ", bpp=" + bpp);
            }

            int texture = re.genTexture();
            re.bindTexture(texture);

            re.startDirectRendering(true, false, true, true, false, 480, 272);
            re.setPixelStore(lineWidth, bpp);

            Buffer buffer = Memory.getInstance().getBuffer(context.textureTx_sourceAddress, lineWidth * height * bpp);

            if (State.captureGeNextFrame) {
                log.info("Capture TRXKICK");
                CaptureManager.captureRAM(context.textureTx_sourceAddress, lineWidth * height * bpp);
            }

            //
            // glTexImage2D only supports
            //		width = (1 << n)	for some integer n
            //		height = (1 << m)	for some integer m
            //
            // This the reason why we are also using glTexSubImage2D.
            //
            int bufferHeight = Utilities.makePow2(height);
            re.setTexImage(0,pixelFormatGe, lineWidth, bufferHeight, pixelFormatGe, pixelFormatGe, null);
            re.setTexSubImage(0, context.textureTx_sx, context.textureTx_sy, width, height, pixelFormatGe, pixelFormatGe, buffer);

            re.beginDraw(PRIM_SPRITES);
            re.drawColor(1.0f, 1.0f, 1.0f, 1.0f);

            float texCoordX = width / (float) lineWidth;
            float texCoordY = height / (float) bufferHeight;

            re.drawTexCoord(0.0f, 0.0f);
            re.drawVertex(dx, dy);

            re.drawTexCoord(texCoordX, 0.0f);
            re.drawVertex(dx + width, dy);

            re.drawTexCoord(texCoordX, texCoordY);
            re.drawVertex(dx + width, dy + height);

            re.drawTexCoord(0.0f, texCoordY);
            re.drawVertex(dx, dy + height);

            re.endDraw();

            re.endDirectRendering();
            re.deleteTexture(texture);
        }
    }

    private void executeCommandBBOX(int normalArgument) {
        Memory mem = Memory.getInstance();
        int numberOfVertexBoundingBox = normalArgument & 0xFF;

        if (!mem.isAddressGood(vinfo.ptr_vertex)) {
            // Abort here to avoid a lot of useless memory read errors...
            error(String.format("%s Invalid vertex address 0x%08X", helper.getCommandString(BBOX), vinfo.ptr_vertex));
            return;
        } else if (vinfo.position == 0) {
            log.warn(helper.getCommandString(BBOX) + " no positions for vertex!");
            return;
        } else if (!re.hasBoundingBox()) {
        	if (!bboxWarningDisplayed) {
        		log.warn("Not supported by your OpenGL version (but can be ignored): " + helper.getCommandString(BBOX) + " numberOfVertex=" + numberOfVertexBoundingBox);
        		bboxWarningDisplayed = true;
        	}
            return;
        } else if ((numberOfVertexBoundingBox % 8) != 0) {
            // How to interpret non-multiple of 8?
            log.warn(helper.getCommandString(BBOX) + " unsupported numberOfVertex=" + numberOfVertexBoundingBox);
        } else if (isLogDebugEnabled) {
            log.debug(helper.getCommandString(BBOX) + " numberOfVertex=" + numberOfVertexBoundingBox);
        }

        boolean useVertexColor = initRendering();

        re.setVertexInfo(vinfo, false, useVertexColor);
        re.beginBoundingBox(numberOfVertexBoundingBox);
        for (int i = 0; i < numberOfVertexBoundingBox; i++) {
            int addr = vinfo.getAddress(mem, i);

            VertexState v = vinfo.readVertex(mem, addr);
            if (isLogDebugEnabled) {
                log.debug(String.format("%s (%f,%f,%f)", helper.getCommandString(BBOX), v.p[0], v.p[1], v.p[2]));
            }

            int vertexIndex = i % 8;
            bboxVertices[vertexIndex][0] = v.p[0];
            bboxVertices[vertexIndex][1] = v.p[1];
            bboxVertices[vertexIndex][2] = v.p[2];

            if (vertexIndex == 7) {
            	re.drawBoundingBox(bboxVertices);
            }
        }
        re.endBoundingBox();

        endRendering(useVertexColor, false, numberOfVertexBoundingBox);
    }

    private void executeCommandBJUMP(int normalArgument) {
        boolean takeConditionalJump = false;

        if (re.hasBoundingBox()) {
        	takeConditionalJump = !re.isBoundingBoxVisible();
        }

        if (takeConditionalJump) {
            int oldPc = currentList.pc;
            currentList.jump(normalArgument);
            int newPc = currentList.pc;
            if (isLogDebugEnabled) {
                log(String.format("%s old PC: 0x%08X, new PC: 0x%08X", helper.getCommandString(BJUMP), oldPc, newPc));
            }
        } else {
            if (isLogDebugEnabled) {
                log(String.format("%s not taking Conditional Jump", helper.getCommandString(BJUMP)));
            }
        }
    }

    private void executeCommandBONE(int normalArgument) {
        // Multiple BONE matrix can be loaded in sequence
        // without having to issue a BOFS for each matrix.
        int matrixIndex = boneMatrixIndex / 12;
        int elementIndex = boneMatrixIndex % 12;
        if (matrixIndex >= 8) {
        	if(isLogDebugEnabled)
        		log("Ignoring BONE matrix element: boneMatrixIndex=" + boneMatrixIndex);
        } else {
            float floatArgument = floatArgument(normalArgument);
            context.bone_uploaded_matrix[matrixIndex][elementIndex] = floatArgument;
        	context.boneMatrixLinear[(boneMatrixIndex / 3) * 4 + (boneMatrixIndex % 3)] = floatArgument;
            if (matrixIndex >= boneMatrixLinearUpdatedMatrix) {
                boneMatrixLinearUpdatedMatrix = matrixIndex + 1;
            }

            boneMatrixIndex++;

            if (isLogDebugEnabled && (boneMatrixIndex % 12) == 0) {
                for (int x = 0; x < 3; x++) {
                    log.debug(String.format("bone matrix %d %.2f %.2f %.2f %.2f",
                            matrixIndex,
                            context.bone_uploaded_matrix[matrixIndex][x + 0],
                            context.bone_uploaded_matrix[matrixIndex][x + 3],
                            context.bone_uploaded_matrix[matrixIndex][x + 6],
                            context.bone_uploaded_matrix[matrixIndex][x + 9]));
                }
            }
        }
    }

    private void enableClientState(boolean useVertexColor, boolean useTexture) {
        if (vinfo.texture != 0 || useTexture) {
            re.enableClientState(IRenderingEngine.RE_TEXTURE);
        } else {
        	re.disableClientState(IRenderingEngine.RE_TEXTURE);
        }
        if (useVertexColor) {
            re.enableClientState(IRenderingEngine.RE_COLOR);
        } else {
            re.disableClientState(IRenderingEngine.RE_COLOR);
        }
        if (vinfo.normal != 0) {
        	re.enableClientState(IRenderingEngine.RE_NORMAL);
        } else {
        	re.disableClientState(IRenderingEngine.RE_NORMAL);
        }
        re.enableClientState(IRenderingEngine.RE_VERTEX);
    }

    private void setTexCoordPointer(boolean useTexture, int nTexCoord, int type, int stride, int offset, boolean isNative, boolean useBufferManager) {
        if (useTexture) {
        	if (!useBufferManager) {
        		re.setTexCoordPointer(nTexCoord, type, stride, offset);
        	} else if (isNative) {
            	bufferManager.setTexCoordPointer(nativeBufferId, nTexCoord, type, vinfo.vertexSize, offset);
            } else {
            	bufferManager.setTexCoordPointer(bufferId, nTexCoord, type, stride, offset);
            }
        }
    }

    private void setColorPointer(boolean useVertexColor, int nColor, int type, int stride, int offset, boolean isNative, boolean useBufferManager) {
        if (useVertexColor) {
        	if (!useBufferManager) {
        		re.setColorPointer(nColor, type, stride, offset);
        	} else if (isNative) {
                bufferManager.setColorPointer(nativeBufferId, nColor, type, vinfo.vertexSize, offset);
            } else {
            	bufferManager.setColorPointer(bufferId, nColor, type, stride, offset);
            }
        }
    }

    private void setVertexPointer(int nVertex, int type, int stride, int offset, boolean isNative, boolean useBufferManager) {
    	if (!useBufferManager) {
    		re.setVertexPointer(nVertex, type, stride, offset);
    	} else if (isNative) {
            bufferManager.setVertexPointer(nativeBufferId, nVertex, type, vinfo.vertexSize, offset);
        } else {
        	bufferManager.setVertexPointer(bufferId, nVertex, type, stride, offset);
        }
    }

    private void setNormalPointer(int type, int stride, int offset, boolean isNative, boolean useBufferManager) {
        if (vinfo.normal != 0) {
        	if (!useBufferManager) {
        		re.setNormalPointer(type, stride, offset);
        	} else if (isNative) {
                bufferManager.setNormalPointer(nativeBufferId, type, vinfo.vertexSize, offset);
            } else {
            	bufferManager.setNormalPointer(bufferId, type, stride, offset);
            }
        }
    }

    private void setWeightPointer(int numberOfWeightsForBuffer, int type, int stride, int offset, boolean isNative, boolean useBufferManager) {
    	if (numberOfWeightsForBuffer > 0) {
    		if (!useBufferManager) {
    			re.setWeightPointer(numberOfWeightsForBuffer, type, stride, offset);
    		} else if (isNative) {
    			re.setWeightPointer(numberOfWeightsForBuffer, type, vinfo.vertexSize, offset);
    		} else {
    			re.setWeightPointer(numberOfWeightsForBuffer, type, stride, offset);
    		}
    	}
    }

    private void setDataPointers(int nVertex, boolean useVertexColor, int nColor, boolean useTexture, int nTexCoord, boolean useNormal, int numberOfWeightsForBuffer, boolean useBufferManager) {
        int stride = 0, cpos = 0, npos = 0, vpos = 0, wpos = 0;

        if (vinfo.texture != 0 || useTexture) {
            stride += SIZEOF_FLOAT * nTexCoord;
            cpos = npos = vpos = stride;
        }
        if (useVertexColor) {
            stride += SIZEOF_FLOAT * 4;
            npos = vpos = stride;
        }
        if (useNormal) {
            stride += SIZEOF_FLOAT * 3;
            vpos = stride;
        }
        stride += SIZEOF_FLOAT * 3;
        if (numberOfWeightsForBuffer > 0) {
            wpos = stride;
            stride += SIZEOF_FLOAT * numberOfWeightsForBuffer;
        }

        enableClientState(useVertexColor, useTexture);
        setTexCoordPointer(useTexture, nTexCoord, IRenderingEngine.RE_FLOAT, stride, 0, false, useBufferManager);
        setColorPointer(useVertexColor, nColor, IRenderingEngine.RE_FLOAT, stride, cpos, false, useBufferManager);
        setNormalPointer(IRenderingEngine.RE_FLOAT, stride, npos, false, useBufferManager);
        setWeightPointer(numberOfWeightsForBuffer, IRenderingEngine.RE_FLOAT, stride, wpos, false, useBufferManager);
        setVertexPointer(nVertex, IRenderingEngine.RE_FLOAT, stride, vpos, false, useBufferManager);
    }

    public void doPositionSkinning(VertexInfo vinfo, float[] boneWeights, float[] position) {
        float x = 0, y = 0, z = 0;
        for (int i = 0; i < vinfo.skinningWeightCount; i++) {
            if (boneWeights[i] != 0) {
                x += (position[0] * context.bone_uploaded_matrix[i][0]
                        + position[1] * context.bone_uploaded_matrix[i][3]
                        + position[2] * context.bone_uploaded_matrix[i][6]
                        + context.bone_uploaded_matrix[i][9]) * boneWeights[i];

                y += (position[0] * context.bone_uploaded_matrix[i][1]
                        + position[1] * context.bone_uploaded_matrix[i][4]
                        + position[2] * context.bone_uploaded_matrix[i][7]
                        + context.bone_uploaded_matrix[i][10]) * boneWeights[i];

                z += (position[0] * context.bone_uploaded_matrix[i][2]
                        + position[1] * context.bone_uploaded_matrix[i][5]
                        + position[2] * context.bone_uploaded_matrix[i][8]
                        + context.bone_uploaded_matrix[i][11]) * boneWeights[i];
            }
        }

        position[0] = x;
        position[1] = y;
        position[2] = z;
    }

    public void doNormalSkinning(VertexInfo vinfo, float[] boneWeights, float[] normal) {
        float nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < vinfo.skinningWeightCount; i++) {
            if (boneWeights[i] != 0) {
                // Normals shouldn't be translated :)
                nx += (normal[0] * context.bone_uploaded_matrix[i][0]
                        + normal[1] * context.bone_uploaded_matrix[i][3]
                        + normal[2] * context.bone_uploaded_matrix[i][6]) * boneWeights[i];

                ny += (normal[0] * context.bone_uploaded_matrix[i][1]
                        + normal[1] * context.bone_uploaded_matrix[i][4]
                        + normal[2] * context.bone_uploaded_matrix[i][7]) * boneWeights[i];

                nz += (normal[0] * context.bone_uploaded_matrix[i][2]
                        + normal[1] * context.bone_uploaded_matrix[i][5]
                        + normal[2] * context.bone_uploaded_matrix[i][8]) * boneWeights[i];
            }
        }

        /*
        // TODO: I doubt psp hardware normalizes normals after skinning,
        // but if it does, this should be uncommented :)
        float length = nx*nx + ny*ny + nz*nz;

        if (length > 0.f) {
        length = 1.f / (float)Math.sqrt(length);

        nx *= length;
        ny *= length;
        nz *= length;
        }
         */

        normal[0] = nx;
        normal[1] = ny;
        normal[2] = nz;
    }

    private void doSkinning(VertexInfo vinfo, VertexState v) {
        float x = 0, y = 0, z = 0;
        float nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < vinfo.skinningWeightCount; ++i) {
            if (v.boneWeights[i] != 0.f) {

                x += (v.p[0] * context.bone_uploaded_matrix[i][0]
                        + v.p[1] * context.bone_uploaded_matrix[i][3]
                        + v.p[2] * context.bone_uploaded_matrix[i][6]
                        + context.bone_uploaded_matrix[i][9]) * v.boneWeights[i];

                y += (v.p[0] * context.bone_uploaded_matrix[i][1]
                        + v.p[1] * context.bone_uploaded_matrix[i][4]
                        + v.p[2] * context.bone_uploaded_matrix[i][7]
                        + context.bone_uploaded_matrix[i][10]) * v.boneWeights[i];

                z += (v.p[0] * context.bone_uploaded_matrix[i][2]
                        + v.p[1] * context.bone_uploaded_matrix[i][5]
                        + v.p[2] * context.bone_uploaded_matrix[i][8]
                        + context.bone_uploaded_matrix[i][11]) * v.boneWeights[i];

                // Normals shouldn't be translated :)
                nx += (v.n[0] * context.bone_uploaded_matrix[i][0]
                        + v.n[1] * context.bone_uploaded_matrix[i][3]
                        + v.n[2] * context.bone_uploaded_matrix[i][6]) * v.boneWeights[i];

                ny += (v.n[0] * context.bone_uploaded_matrix[i][1]
                        + v.n[1] * context.bone_uploaded_matrix[i][4]
                        + v.n[2] * context.bone_uploaded_matrix[i][7]) * v.boneWeights[i];

                nz += (v.n[0] * context.bone_uploaded_matrix[i][2]
                        + v.n[1] * context.bone_uploaded_matrix[i][5]
                        + v.n[2] * context.bone_uploaded_matrix[i][8]) * v.boneWeights[i];
            }
        }

        v.p[0] = x;
        v.p[1] = y;
        v.p[2] = z;

        /*
        // TODO: I doubt psp hardware normalizes normals after skinning,
        // but if it does, this should be uncommented :)
        float length = nx*nx + ny*ny + nz*nz;

        if (length > 0.f) {
        length = 1.f / (float)Math.sqrt(length);

        nx *= length;
        ny *= length;
        nz *= length;
        }
         */

        v.n[0] = nx;
        v.n[1] = ny;
        v.n[2] = nz;
    }

    private void log(String commandString, float floatArgument) {
        if (isLogDebugEnabled) {
            log(commandString + SPACE + floatArgument);
        }
    }

    private void log(String commandString, int value) {
        if (isLogDebugEnabled) {
            log(commandString + SPACE + value);
        }
    }

    private void log(String commandString, float[] matrix) {
        if (isLogDebugEnabled) {
            for (int y = 0; y < 4; y++) {
                log(commandString + SPACE + String.format("%.1f %.1f %.1f %.1f", matrix[0 + y * 4], matrix[1 + y * 4], matrix[2 + y * 4], matrix[3 + y * 4]));
            }
        }
    }

    private boolean canCacheTexture(int tex_addr) {
    	if (!useTextureCache) {
    		return false;
    	}

        // Some games are storing compressed textures in VRAM (e.g. Skate Park City).
        // Force only a reload of textures that can be generated by the GE buffer,
        // i.e. when texture_storage is one of
        // BGR5650=0, ABGR5551=1, ABGR4444=2 or ABGR8888=3.
    	if (isVRAM(tex_addr) && context.texture_storage <= TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
        	return false;
        }

    	if (!videoTextures.isEmpty() && videoTextures.contains(tex_addr)) {
    		return false;
    	}

    	return true;
    }

    private void loadTexture() {
        // No need to reload or check the texture cache if no texture parameter
        // has been changed since last call loadTexture()
        if (!textureChanged) {
            return;
        }

        // HACK: avoid texture uploads of null pointers
        // This can come from Sony's GE init code (pspsdk GE init is ok)
        if (context.texture_base_pointer[0] == 0) {
            return;
        }

        // Texture not used when disabled (automatically disabled in clear mode).
        if (!context.textureFlag.isEnabled()) {
            return;
        }

        Texture texture;
        int tex_addr = context.texture_base_pointer[0] & Memory.addressMask;
        if (!canCacheTexture(tex_addr)) {
            texture = null;

            // Generate a texture id if we don't have one
            if (textureId == -1) {
            	textureId = re.genTexture();
            }

            re.bindTexture(textureId);
        } else {
            TextureCache textureCache = TextureCache.getInstance();
            boolean textureRequiresClut = context.texture_storage >= TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED && context.texture_storage <= TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED;

            textureCacheLookupStatistics.start();
            // Check if the texture is in the cache
            if (textureRequiresClut) {
            	texture = textureCache.getTexture(context.texture_base_pointer[0], context.texture_buffer_width[0], context.texture_width[0], context.texture_height[0], context.texture_storage, context.tex_clut_addr, context.tex_clut_mode, context.tex_clut_start, context.tex_clut_shift, context.tex_clut_mask, context.tex_clut_num_blocks, context.texture_num_mip_maps, context.mipmapShareClut);
            } else {
            	texture = textureCache.getTexture(context.texture_base_pointer[0], context.texture_buffer_width[0], context.texture_width[0], context.texture_height[0], context.texture_storage, 0, 0, 0, 0, 0, 0, context.texture_num_mip_maps, false);
            }
            textureCacheLookupStatistics.end();

            // Create the texture if not yet in the cache
            if (texture == null) {
            	if (textureRequiresClut) {
            		texture = new Texture(textureCache, context.texture_base_pointer[0], context.texture_buffer_width[0], context.texture_width[0], context.texture_height[0], context.texture_storage, context.tex_clut_addr, context.tex_clut_mode, context.tex_clut_start, context.tex_clut_shift, context.tex_clut_mask, context.tex_clut_num_blocks, context.texture_num_mip_maps, context.mipmapShareClut);
            	} else {
            		texture = new Texture(textureCache, context.texture_base_pointer[0], context.texture_buffer_width[0], context.texture_width[0], context.texture_height[0], context.texture_storage, 0, 0, 0, 0, 0, 0, context.texture_num_mip_maps, false);
            	}
                textureCache.addTexture(re, texture);
            }

            texture.bindTexture(re);
        }

        // Load the texture if not yet loaded
        if (texture == null || !texture.isLoaded() || State.captureGeNextFrame || State.replayGeNextFrame) {
            if (isLogDebugEnabled) {
                log(helper.getCommandString(TFLUSH)
                        + " " + String.format("0x%08X", context.texture_base_pointer[0])
                        + ", buffer_width=" + context.texture_buffer_width[0]
                        + " (" + context.texture_width[0] + "," + context.texture_height[0] + ")");

                log(helper.getCommandString(TFLUSH)
                        + " texture_storage=0x" + Integer.toHexString(context.texture_storage)
                        + "(" + getPsmName(context.texture_storage)
                        + "), tex_clut_mode=0x" + Integer.toHexString(context.tex_clut_mode)
                        + ", tex_clut_addr=" + String.format("0x%08X", context.tex_clut_addr)
                        + ", texture_swizzle=" + context.texture_swizzle);
            }

            Buffer final_buffer = null;
            int texclut = context.tex_clut_addr;
            int texaddr;

            int textureByteAlignment = 4;   // 32 bits
            boolean compressedTexture = false;

            int numberMipmaps = context.texture_num_mip_maps;
            Memory mem = Memory.getInstance();

            for (int level = 0; level <= numberMipmaps; level++) {
                // Extract texture information with the minor conversion possible
                // TODO: Get rid of information copying, and implement all the available formats
                texaddr = context.texture_base_pointer[level];
                texaddr &= Memory.addressMask;
                if (!mem.isAddressGood(texaddr)) {
                	error(String.format("Invalid texture address 0x%08X for texture level %d", texaddr, level));
                	break;
                }
                compressedTexture = false;
                int compressedTextureSize = 0;
                int buffer_storage = context.texture_storage;

                switch (context.texture_storage) {
                    case TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED: {
                        if (texclut == 0) {
                            return;
                        }

                        buffer_storage = context.tex_clut_mode;
                        switch (context.tex_clut_mode) {
                            case CMODE_FORMAT_16BIT_BGR5650:
                            case CMODE_FORMAT_16BIT_ABGR5551:
                            case CMODE_FORMAT_16BIT_ABGR4444: {
                                textureByteAlignment = 2;  // 16 bits
                                short[] clut = readClut16(level);
                                int clutSharingOffset = context.mipmapShareClut ? 0 : level * 16;

                                if (!context.texture_swizzle) {
                                    int length = context.texture_buffer_width[level] * context.texture_height[level];
                                    IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length / 2, 1);
                                    for (int i = 0; i < length; i += 2) {

                                        int index = memoryReader.readNext();

                                        tmp_texture_buffer16[i] = clut[getClutIndex(index & 0xF) + clutSharingOffset];
                                        tmp_texture_buffer16[i + 1] = clut[getClutIndex((index >> 4) & 0xF) + clutSharingOffset];
                                    }
                                    final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);

                                    if (State.captureGeNextFrame) {
                                        log.info("Capture loadTexture clut 4/16 unswizzled");
                                        CaptureManager.captureRAM(texaddr, length / 2);
                                    }
                                } else {
                                    unswizzleTextureFromMemory(texaddr, 0, level);
                                    int pixels = context.texture_buffer_width[level] * context.texture_height[level];
                                    for (int i = 0, j = 0; i < pixels; i += 8, j++) {
                                        int n = tmp_texture_buffer32[j];
                                        int index = n & 0xF;
                                        tmp_texture_buffer16[i + 0] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 4) & 0xF;
                                        tmp_texture_buffer16[i + 1] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 8) & 0xF;
                                        tmp_texture_buffer16[i + 2] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 12) & 0xF;
                                        tmp_texture_buffer16[i + 3] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 16) & 0xF;
                                        tmp_texture_buffer16[i + 4] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 20) & 0xF;
                                        tmp_texture_buffer16[i + 5] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 24) & 0xF;
                                        tmp_texture_buffer16[i + 6] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 28) & 0xF;
                                        tmp_texture_buffer16[i + 7] = clut[getClutIndex(index) + clutSharingOffset];
                                    }
                                    final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);
                                    break;
                                }

                                break;
                            }

                            case CMODE_FORMAT_32BIT_ABGR8888: {
                                int[] clut = readClut32(level);
                                int clutSharingOffset = context.mipmapShareClut ? 0 : level * 16;

                                if (!context.texture_swizzle) {
                                    int length = context.texture_buffer_width[level] * context.texture_height[level];
                                    IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length / 2, 1);
                                    for (int i = 0; i < length; i += 2) {

                                        int index = memoryReader.readNext();

                                        tmp_texture_buffer32[i + 1] = clut[getClutIndex((index >> 4) & 0xF) + clutSharingOffset];
                                        tmp_texture_buffer32[i] = clut[getClutIndex(index & 0xF) + clutSharingOffset];
                                    }
                                    final_buffer = IntBuffer.wrap(tmp_texture_buffer32);

                                    if (State.captureGeNextFrame) {
                                        log.info("Capture loadTexture clut 4/32 unswizzled");
                                        CaptureManager.captureRAM(texaddr, length / 2);
                                    }
                                } else {
                                    unswizzleTextureFromMemory(texaddr, 0, level);
                                    int pixels = context.texture_buffer_width[level] * context.texture_height[level];
                                    for (int i = pixels - 8, j = (pixels / 8) - 1; i >= 0; i -= 8, j--) {
                                        int n = tmp_texture_buffer32[j];
                                        int index = n & 0xF;
                                        tmp_texture_buffer32[i + 0] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 4) & 0xF;
                                        tmp_texture_buffer32[i + 1] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 8) & 0xF;
                                        tmp_texture_buffer32[i + 2] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 12) & 0xF;
                                        tmp_texture_buffer32[i + 3] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 16) & 0xF;
                                        tmp_texture_buffer32[i + 4] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 20) & 0xF;
                                        tmp_texture_buffer32[i + 5] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 24) & 0xF;
                                        tmp_texture_buffer32[i + 6] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 28) & 0xF;
                                        tmp_texture_buffer32[i + 7] = clut[getClutIndex(index) + clutSharingOffset];
                                    }
                                    final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
                                }

                                break;
                            }

                            default: {
                                error("Unhandled clut4 texture mode " + context.tex_clut_mode);
                                return;
                            }
                        }

                        break;
                    }
                    case TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED: {
                        final_buffer = readIndexedTexture(level, texaddr, texclut, 1);
                        buffer_storage = context.tex_clut_mode;
                        textureByteAlignment = textureByteAlignmentMapping[context.tex_clut_mode];
                        break;
                    }
                    case TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED: {
                        final_buffer = readIndexedTexture(level, texaddr, texclut, 2);
                        buffer_storage = context.tex_clut_mode;
                        textureByteAlignment = textureByteAlignmentMapping[context.tex_clut_mode];
                        break;
                    }
                    case TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED: {
                        final_buffer = readIndexedTexture(level, texaddr, texclut, 4);
                        buffer_storage = context.tex_clut_mode;
                        textureByteAlignment = textureByteAlignmentMapping[context.tex_clut_mode];
                        break;
                    }
                    case TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
                    case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
                    case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444: {
                        textureByteAlignment = 2;  // 16 bits

                        if (!context.texture_swizzle) {
                            int length = Math.max(context.texture_buffer_width[level], context.texture_width[level]) * context.texture_height[level];
                            final_buffer = Memory.getInstance().getBuffer(texaddr, length * 2);
                            if (final_buffer == null) {
                                IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length * 2, 2);
                                for (int i = 0; i < length; i++) {
                                    int pixel = memoryReader.readNext();
                                    tmp_texture_buffer16[i] = (short) pixel;
                                }

                                final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);
                            }

                            if (State.captureGeNextFrame) {
                                log.info("Capture loadTexture 16 unswizzled");
                                CaptureManager.captureRAM(texaddr, length * 2);
                            }
                        } else {
                            final_buffer = unswizzleTextureFromMemory(texaddr, 2, level);
                        }

                        break;
                    }

                    case TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888: {
                        final_buffer = getTexture32BitBuffer(texaddr, level);
                        break;
                    }

                    case TPSM_PIXEL_STORAGE_MODE_DXT1: {
                        if (isLogDebugEnabled) {
                            log.debug("Loading texture TPSM_PIXEL_STORAGE_MODE_DXT1 " + Integer.toHexString(texaddr));
                        }
                        compressedTexture = true;
                        compressedTextureSize = getCompressedTextureSize(level, 8);
                        IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, compressedTextureSize, 4);
                        // PSP DXT1 hardware format reverses the colors and the per-pixel
                        // bits, and encodes the color in RGB 565 format
                        int i = 0;
                        for (int y = 0; y < context.texture_height[level]; y += 4) {
                            for (int x = 0; x < context.texture_buffer_width[level]; x += 4, i += 2) {
                                tmp_texture_buffer32[i + 1] = memoryReader.readNext();
                                tmp_texture_buffer32[i + 0] = memoryReader.readNext();
                            }
                            for (int x = context.texture_buffer_width[level]; x < context.texture_width[level]; x += 4, i += 2) {
                                tmp_texture_buffer32[i + 0] = 0;
                                tmp_texture_buffer32[i + 1] = 0;
                            }
                        }
                        final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
                        break;
                    }

                    case TPSM_PIXEL_STORAGE_MODE_DXT3: {
                        if (isLogDebugEnabled) {
                            log.debug("Loading texture TPSM_PIXEL_STORAGE_MODE_DXT3 " + Integer.toHexString(texaddr));
                        }
                        compressedTexture = true;
                        compressedTextureSize = getCompressedTextureSize(level, 4);
                        IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, compressedTextureSize, 4);
                        // PSP DXT3 format reverses the alpha and color parts of each block,
                        // and reverses the color and per-pixel terms in the color part.
                        int i = 0;
                        for (int y = 0; y < context.texture_height[level]; y += 4) {
                            for (int x = 0; x < context.texture_buffer_width[level]; x += 4, i += 4) {
                                // Color
                                tmp_texture_buffer32[i + 3] = memoryReader.readNext();
                                tmp_texture_buffer32[i + 2] = memoryReader.readNext();
                                // Alpha
                                tmp_texture_buffer32[i + 0] = memoryReader.readNext();
                                tmp_texture_buffer32[i + 1] = memoryReader.readNext();
                            }
                            for (int x = context.texture_buffer_width[level]; x < context.texture_width[level]; x += 4, i += 4) {
                                tmp_texture_buffer32[i + 0] = 0;
                                tmp_texture_buffer32[i + 1] = 0;
                                tmp_texture_buffer32[i + 2] = 0;
                                tmp_texture_buffer32[i + 3] = 0;
                            }
                        }
                        final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
                        break;
                    }

                    case TPSM_PIXEL_STORAGE_MODE_DXT5: {
                        if (isLogDebugEnabled) {
                            log.debug("Loading texture TPSM_PIXEL_STORAGE_MODE_DXT5 " + Integer.toHexString(texaddr));
                        }
                        compressedTexture = true;
                        compressedTextureSize = getCompressedTextureSize(level, 4);
                        IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, compressedTextureSize, 2);
                        // PSP DXT5 format reverses the alpha and color parts of each block,
                        // and reverses the color and per-pixel terms in the color part. In
                        // the alpha part, the 2 reference alpha values are swapped with the
                        // alpha interpolation values.
                        int i = 0;
                        for (int y = 0; y < context.texture_height[level]; y += 4) {
                            for (int x = 0; x < context.texture_buffer_width[level]; x += 4, i += 8) {
                                // Color
                                tmp_texture_buffer16[i + 6] = (short) memoryReader.readNext();
                                tmp_texture_buffer16[i + 7] = (short) memoryReader.readNext();
                                tmp_texture_buffer16[i + 4] = (short) memoryReader.readNext();
                                tmp_texture_buffer16[i + 5] = (short) memoryReader.readNext();
                                // Alpha
                                tmp_texture_buffer16[i + 1] = (short) memoryReader.readNext();
                                tmp_texture_buffer16[i + 2] = (short) memoryReader.readNext();
                                tmp_texture_buffer16[i + 3] = (short) memoryReader.readNext();
                                tmp_texture_buffer16[i + 0] = (short) memoryReader.readNext();
                            }
                            for (int x = context.texture_buffer_width[level]; x < context.texture_width[level]; x += 4, i += 8) {
                                tmp_texture_buffer16[i + 0] = 0;
                                tmp_texture_buffer16[i + 1] = 0;
                                tmp_texture_buffer16[i + 2] = 0;
                                tmp_texture_buffer16[i + 3] = 0;
                                tmp_texture_buffer16[i + 4] = 0;
                                tmp_texture_buffer16[i + 5] = 0;
                                tmp_texture_buffer16[i + 6] = 0;
                                tmp_texture_buffer16[i + 7] = 0;
                            }
                        }
                        final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);
                        break;
                    }

                    default: {
                        error("Unhandled texture storage " + context.texture_storage);
                        return;
                    }
                }

                // Upload texture to openGL.
                re.setTextureMipmapMagFilter(context.tex_mag_filter);
                re.setTextureMipmapMinFilter(context.tex_min_filter);

                re.setPixelStore(context.texture_buffer_width[level], textureByteAlignment);

                if (compressedTexture) {
                    re.setCompressedTexImage(
                            level,
                            buffer_storage,
                            context.texture_width[level], context.texture_height[level],
                            compressedTextureSize,
                            final_buffer);
                } else {
                    re.setTexImage(
                            level,
                            buffer_storage,
                            context.texture_width[level], context.texture_height[level],
                            buffer_storage,
                            buffer_storage,
                            final_buffer);
                }

                if (State.captureGeNextFrame) {
                    if (isVRAM(tex_addr)) {
                        CaptureManager.captureImage(texaddr, level, final_buffer, context.texture_width[level], context.texture_height[level], context.texture_buffer_width[level], buffer_storage, compressedTexture, compressedTextureSize, false);
                    } else if (!CaptureManager.isImageCaptured(texaddr)) {
                        CaptureManager.captureImage(texaddr, level, final_buffer, context.texture_width[level], context.texture_height[level], context.texture_buffer_width[level], buffer_storage, compressedTexture, compressedTextureSize, true);
                    }
                }

                if (texture != null) {
                    texture.setIsLoaded();
                    if (isLogDebugEnabled) {
                        log(helper.getCommandString(TFLUSH) + " Loaded texture " + texture.getGlId());
                    }
                }
            }

            checkTextureMinFilter(compressedTexture, numberMipmaps);
        } else {
            boolean compressedTexture = (context.texture_storage >= TPSM_PIXEL_STORAGE_MODE_DXT1 && context.texture_storage <= TPSM_PIXEL_STORAGE_MODE_DXT5);
            re.setTextureMipmapMagFilter(context.tex_mag_filter);
            re.setTextureMipmapMinFilter(context.tex_min_filter);
            checkTextureMinFilter(compressedTexture, context.texture_num_mip_maps);

            if (isLogDebugEnabled) {
                log(helper.getCommandString(TFLUSH) + " Reusing cached texture " + texture.getGlId());
            }
        }

        textureChanged = false;
    }

    private void checkTextureMinFilter(boolean compressedTexture, int numberMipmaps) {
        // OpenGL/Hardware cannot interpolate between compressed textures;
        // this restriction has been checked on NVIDIA GeForce 8500 GT and 9800 GT
        if (compressedTexture) {
            int new_tex_min_filter;
            if (context.tex_min_filter == TFLT_NEAREST || context.tex_min_filter == TFLT_NEAREST_MIPMAP_LINEAR || context.tex_min_filter == TFLT_NEAREST_MIPMAP_NEAREST) {
                new_tex_min_filter = TFLT_NEAREST;
            } else {
                new_tex_min_filter = TFLT_LINEAR;
            }

            if (new_tex_min_filter != context.tex_min_filter) {
	            re.setTextureMipmapMinFilter(new_tex_min_filter);
	            if (isLogDebugEnabled) {
	                log("Overwriting texture min filter, no mipmap was generated but filter was set to use mipmap");
	            }
            }
        }
    }

    private Buffer readIndexedTexture(int level, int texaddr, int texclut, int bytesPerIndex) {
        Buffer buffer = null;

        int length = context.texture_buffer_width[level] * context.texture_height[level];
        switch (context.tex_clut_mode) {
            case CMODE_FORMAT_16BIT_BGR5650:
            case CMODE_FORMAT_16BIT_ABGR5551:
            case CMODE_FORMAT_16BIT_ABGR4444: {
                if (texclut == 0) {
                    return null;
                }

                short[] clut = readClut16(level);

                if (!context.texture_swizzle) {
                    IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length * bytesPerIndex, bytesPerIndex);
                    for (int i = 0; i < length; i++) {
                        int index = memoryReader.readNext();
                        tmp_texture_buffer16[i] = clut[getClutIndex(index)];
                    }
                    buffer = ShortBuffer.wrap(tmp_texture_buffer16);

                    if (State.captureGeNextFrame) {
                        log.info("Capture loadTexture clut 8/16 unswizzled");
                        CaptureManager.captureRAM(texaddr, length * bytesPerIndex);
                    }
                } else {
                    unswizzleTextureFromMemory(texaddr, bytesPerIndex, level);
                    switch (bytesPerIndex) {
                        case 1: {
                            for (int i = 0, j = 0; i < length; i += 4, j++) {
                                int n = tmp_texture_buffer32[j];
                                int index = n & 0xFF;
                                tmp_texture_buffer16[i + 0] = clut[getClutIndex(index)];
                                index = (n >> 8) & 0xFF;
                                tmp_texture_buffer16[i + 1] = clut[getClutIndex(index)];
                                index = (n >> 16) & 0xFF;
                                tmp_texture_buffer16[i + 2] = clut[getClutIndex(index)];
                                index = (n >> 24) & 0xFF;
                                tmp_texture_buffer16[i + 3] = clut[getClutIndex(index)];
                            }
                            break;
                        }
                        case 2: {
                            for (int i = 0, j = 0; i < length; i += 2, j++) {
                                int n = tmp_texture_buffer32[j];
                                tmp_texture_buffer16[i + 0] = clut[getClutIndex(n & 0xFFFF)];
                                tmp_texture_buffer16[i + 1] = clut[getClutIndex(n >>> 16)];
                            }
                            break;
                        }
                        case 4: {
                            for (int i = 0; i < length; i++) {
                                int n = tmp_texture_buffer32[i];
                                tmp_texture_buffer16[i] = clut[getClutIndex(n)];
                            }
                            break;
                        }
                    }
                    buffer = ShortBuffer.wrap(tmp_texture_buffer16);
                }

                break;
            }

            case CMODE_FORMAT_32BIT_ABGR8888: {
                if (texclut == 0) {
                    return null;
                }

                int[] clut = readClut32(level);

                if (!context.texture_swizzle) {
                    IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length * bytesPerIndex, bytesPerIndex);
                    for (int i = 0; i < length; i++) {
                        int index = memoryReader.readNext();
                        tmp_texture_buffer32[i] = clut[getClutIndex(index)];
                    }
                    buffer = IntBuffer.wrap(tmp_texture_buffer32);

                    if (State.captureGeNextFrame) {
                        log.info("Capture loadTexture clut 8/32 unswizzled");
                        CaptureManager.captureRAM(texaddr, length * bytesPerIndex);
                    }
                } else {
                    unswizzleTextureFromMemory(texaddr, bytesPerIndex, level);
                    switch (bytesPerIndex) {
                        case 1: {
                            for (int i = length - 4, j = (length / 4) - 1; i >= 0; i -= 4, j--) {
                                int n = tmp_texture_buffer32[j];
                                int index = n & 0xFF;
                                tmp_texture_buffer32[i + 0] = clut[getClutIndex(index)];
                                index = (n >> 8) & 0xFF;
                                tmp_texture_buffer32[i + 1] = clut[getClutIndex(index)];
                                index = (n >> 16) & 0xFF;
                                tmp_texture_buffer32[i + 2] = clut[getClutIndex(index)];
                                index = (n >> 24) & 0xFF;
                                tmp_texture_buffer32[i + 3] = clut[getClutIndex(index)];
                            }
                            break;
                        }
                        case 2: {
                            for (int i = length - 2, j = (length / 2) - 1; i >= 0; i -= 2, j--) {
                                int n = tmp_texture_buffer32[j];
                                tmp_texture_buffer32[i + 0] = clut[getClutIndex(n & 0xFFFF)];
                                tmp_texture_buffer32[i + 1] = clut[getClutIndex(n >>> 16)];
                            }
                            break;
                        }
                        case 4: {
                            for (int i = 0; i < length; i++) {
                                int n = tmp_texture_buffer32[i];
                                tmp_texture_buffer32[i] = clut[getClutIndex(n)];
                            }
                            break;
                        }
                    }
                    buffer = IntBuffer.wrap(tmp_texture_buffer32);
                }

                break;
            }

            default: {
                error("Unhandled clut8 texture mode " + context.tex_clut_mode);
                break;
            }
        }

        return buffer;
    }

    private void setScissor() {
        if (context.scissor_x1 >= 0 && context.scissor_y1 >= 0 && context.scissor_width <= context.region_width && context.scissor_height <= context.region_height) {
        	int scissorX = context.scissor_x1;
        	int scissorY = context.scissor_y1;
        	int scissorWidth = context.scissor_width;
        	int scissorHeight = context.scissor_height;
        	if (scissorHeight < 272) {
        		scissorY += 272 - scissorHeight;
        	}
            re.setScissor(scissorX, scissorY, scissorWidth, scissorHeight);
        	context.scissorTestFlag.setEnabled(true);
        } else {
        	context.scissorTestFlag.setEnabled(false);
        }
    }

    private boolean initRendering() {
        /*
         * Defer transformations until primitive rendering
         */

    	/*
    	 * Set Scissor
    	 */
    	if (scissorChanged) {
    		setScissor();
    		scissorChanged = false;
    	}

    	/*
         * Apply Blending
         */
        if (blendChanged) {
            setBlendFunc();
            blendChanged = false;
        }

        /*
         * Apply projection matrix
         */
        if (projectionMatrixUpload.isChanged()) {
            if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD) {
            	if (context.viewport_height <= 0 && context.viewport_width >= 0) {
            		re.setProjectionMatrix(context.proj_uploaded_matrix);
            	} else {
            		float[] flippedMatrix = new float[16];
            		System.arraycopy(context.proj_uploaded_matrix, 0, flippedMatrix, 0, flippedMatrix.length);
            		if (context.viewport_height > 0) {
                		// Flip upside-down
            			flippedMatrix[5] = -flippedMatrix[5];
            		}
            		if (context.viewport_width < 0) {
            			// Flip right-to-left
            			flippedMatrix[0] = -flippedMatrix[0];
            		}
            		re.setProjectionMatrix(flippedMatrix);
            	}
            } else {
            	re.setProjectionMatrix(null);
            }
            projectionMatrixUpload.setChanged(false);

            // The viewport has to be reloaded when the projection matrix has changed
            viewportChanged = true;
        }

        /*
         * Apply viewport
         */
        boolean loadOrtho2D = false;
        if (viewportChanged) {
            if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD) {
                re.setViewport(0, 0, 480, 272);
                // Load the ortho for 2D after the depth settings
                loadOrtho2D = true;
            } else {
                if (context.viewport_cx == 0 && context.viewport_cy == 0 && context.viewport_height == 0 && context.viewport_width == 0) {
                	context.viewport_cx = 2048;
                	context.viewport_cy = 2048;
                	context.viewport_width = 480;
                	context.viewport_height = 272;
                }

                int halfHeight = Math.abs(context.viewport_height);
                int haltWidth = Math.abs(context.viewport_width);
                int viewportX = context.viewport_cx - haltWidth - context.offset_x;
                int viewportY = context.viewport_cy - halfHeight - context.offset_y;
                int viewportWidth = 2 * haltWidth;
                int viewportHeight = 2 * halfHeight;

                // Align the viewport to the top of the window
                if (viewportHeight < 272) {
                    viewportY += 272 - viewportHeight;
            	}

                if (viewportHeight <= 272) {
                	re.setViewport(viewportX, viewportY, viewportWidth, viewportHeight);
                }
            }
            viewportChanged = false;
        }

        /*
         * Apply depth handling
         */
        if (depthChanged) {
            if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD) {
            	re.setDepthFunc(context.depthFunc);
                re.setDepthRange(context.zpos, context.zscale, context.zpos - context.zscale, context.zpos + context.zscale);
            } else {
            	re.setDepthFunc(context.depthFunc);
                re.setDepthRange(0.5f, 0.5f, 0, 1);
            }
            depthChanged = false;
        }

        /*
         * Load the 2D ortho (only after the depth settings
         */
        if (loadOrtho2D) {
            re.setProjectionMatrix(getOrthoMatrix(0, 480, 272, 0, 0, -0xFFFF));
        }

        /*
         * 2D mode handling
         */
        if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD) {
            // 2D mode shouldn't be affected by the lighting and fog
        	re.disableFlag(IRenderingEngine.GU_LIGHTING);
        	re.disableFlag(IRenderingEngine.GU_FOG);

            // TODO I don't know why, but the GL_MODELVIEW matrix has to be reloaded
            // each time in 2D mode... Otherwise textures are not displayed.
            modelMatrixUpload.setChanged(true);
        } else {
        	context.lightingFlag.update();
        	context.fogFlag.update();
        }

        /*
         * Model-View matrix has to reloaded when
         * - model matrix changed
         * - view matrix changed
         * - lighting has to be reloaded
         */
        boolean loadLightingSettings = (viewMatrixUpload.isChanged() || lightingChanged) && context.lightingFlag.isEnabled() && context.transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD;
        boolean modelViewMatrixChanged = modelMatrixUpload.isChanged() || viewMatrixUpload.isChanged() || loadLightingSettings;

        /*
         * Apply view matrix
         */
        if (modelViewMatrixChanged) {
            if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD) {
            	re.setViewMatrix(context.view_uploaded_matrix);
            } else {
            	re.setViewMatrix(null);
            }
            viewMatrixUpload.setChanged(false);
        }

        /*
         *  Setup lights on when view transformation is set up
         */
        if (loadLightingSettings || context.tex_map_mode == TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP) {
            for (int i = 0; i < NUM_LIGHTS; i++) {
                if (context.lightFlags[i].isEnabled() || (context.tex_map_mode == TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP && (context.tex_shade_u == i || context.tex_shade_v == i))) {
                	re.setLightPosition(i, context.light_pos[i]);
                	re.setLightDirection(i, context.light_dir[i]);

                    if (context.light_type[i] == LIGHT_SPOT) {
                    	re.setLightSpotExponent(i, context.spotLightExponent[i]);
                    	re.setLightSpotCutoff(i, context.spotLightCutoff[i]);
                    } else {
                        // uniform light distribution
                    	re.setLightSpotExponent(i, 0);
                    	re.setLightSpotCutoff(i, 180);
                    }

                    // Light kind:
                    //  LIGHT_DIFFUSE_SPECULAR: use ambient, diffuse and specular colors
                    //  all other light kinds: use ambient and diffuse colors (not specular)
                    if (context.light_kind[i] != LIGHT_AMBIENT_DIFFUSE) {
                    	re.setLightSpecularColor(i, context.lightSpecularColor[i]);
                    } else {
                    	re.setLightSpecularColor(i, blackColor);
                    }
                }
            }

            lightingChanged = false;
        }

        if (modelViewMatrixChanged) {
            // Apply model matrix
            if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD) {
            	re.setModelMatrix(context.model_uploaded_matrix);
            } else {
            	re.setModelMatrix(null);
            }
            modelMatrixUpload.setChanged(false);
            re.endModelViewMatrixUpdate();
        }

        /*
         * Apply texture transforms
         */
        if (textureMatrixUpload.isChanged()) {
            if (context.transform_mode != VTYPE_TRANSFORM_PIPELINE_TRANS_COORD) {
            	re.setTextureMapMode(TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV, TMAP_TEXTURE_PROJECTION_MODE_TEXTURE_COORDINATES);

            	float[] textureMatrix = new float[] {
            			1.f / context.texture_width[0], 0, 0, 0,
                		0, 1.f / context.texture_height[0], 0, 0,
                		0, 0, 1, 0,
                		0, 0, 0, 1
                	};
            	re.setTextureMatrix(textureMatrix);
            } else {
            	re.setTextureMapMode(context.tex_map_mode, context.tex_proj_map_mode);
                switch (context.tex_map_mode) {
                    case TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV: {
                        re.disableFlag(IRenderingEngine.RE_TEXTURE_GEN_S);
                        re.disableFlag(IRenderingEngine.RE_TEXTURE_GEN_T);

                        float[] textureMatrix = new float[] {
                        		context.tex_scale_x, 0, 0, 0,
                        		0, context.tex_scale_y, 0, 0,
                        		0, 0, 1, 0,
                        		context.tex_translate_x, context.tex_translate_y, 0, 1
                        	};
                    	re.setTextureMatrix(textureMatrix);
                        break;
                    }

                    case TMAP_TEXTURE_MAP_MODE_TEXTURE_MATRIX: {
                        re.disableFlag(IRenderingEngine.RE_TEXTURE_GEN_S);
                        re.disableFlag(IRenderingEngine.RE_TEXTURE_GEN_T);
                    	re.setTextureMatrix(context.texture_uploaded_matrix);
                        break;
                    }

                    case TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP: {
                    	re.setTextureEnvironmentMapping(context.tex_shade_u, context.tex_shade_v);
                        re.enableFlag(IRenderingEngine.RE_TEXTURE_GEN_S);
                        re.enableFlag(IRenderingEngine.RE_TEXTURE_GEN_T);
                    	re.setTextureMatrix(context.tex_envmap_matrix);
                        break;
                    }

                    default:
                        log("Unhandled texture matrix mode " + context.tex_map_mode);
                }
            }

            textureMatrixUpload.setChanged(false);
        }

        boolean useVertexColor = false;
        if (!context.lightingFlag.isEnabled() || context.transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD) {
        	re.disableFlag(IRenderingEngine.RE_COLOR_MATERIAL);
            if (vinfo.color != 0) {
                useVertexColor = true;
            } else {
                if (materialChanged) {
                	re.setVertexColor(context.mat_ambient);
                    materialChanged = false;
                }
            }
        } else if (vinfo.color != 0 && context.mat_flags != 0) {
            useVertexColor = true;
            if (materialChanged) {
            	boolean ambient = (context.mat_flags & 1) != 0;
            	boolean diffuse = (context.mat_flags & 2) != 0;
            	boolean specular = (context.mat_flags & 4) != 0;
                if (!ambient) {
                	re.setMaterialAmbientColor(context.mat_ambient);
                }
                if (!diffuse) {
                	re.setMaterialDiffuseColor(context.mat_diffuse);
                }
                if (!specular) {
                	re.setMaterialSpecularColor(context.mat_specular);
                }
                re.setColorMaterial(ambient, diffuse, specular);
            	re.enableFlag(IRenderingEngine.RE_COLOR_MATERIAL);
                materialChanged = false;
            }
        } else {
        	re.disableFlag(IRenderingEngine.RE_COLOR_MATERIAL);
            if (materialChanged) {
            	re.setMaterialAmbientColor(context.mat_ambient);
            	re.setMaterialDiffuseColor(context.mat_diffuse);
            	re.setMaterialSpecularColor(context.mat_specular);
            	re.setColorMaterial(false, false, false);
                materialChanged = false;
            }
        }

        re.setTextureWrapMode(context.tex_wrap_s, context.tex_wrap_t);

        int mipmapBaseLevel = 0;
        int mipmapMaxLevel = context.texture_num_mip_maps;
        if (context.tex_mipmap_mode == TBIAS_MODE_CONST) {
            // TBIAS_MODE_CONST uses the tex_mipmap_bias_int level supplied by TBIAS.
            mipmapBaseLevel = context.tex_mipmap_bias_int;
            mipmapMaxLevel = context.tex_mipmap_bias_int;
            if (isLogDebugEnabled) {
                log.debug("TBIAS_MODE_CONST " + context.tex_mipmap_bias_int);
            }
        } else if (context.tex_mipmap_mode == TBIAS_MODE_AUTO) {
        	// TODO implement TBIAS_MODE_AUTO. The following is not correct
            // TBIAS_MODE_AUTO performs a comparison between the texture's weight and height at level 0.
            // int maxValue = Math.max(context.texture_width[0], context.texture_height[0]);
        	//
            // if(maxValue <= 1) {
            //     mipmapBaseLevel = 0;
            // } else {
            //     mipmapBaseLevel = (int) ((Math.log((Math.abs(maxValue) / Math.abs(context.zpos))) / Math.log(2)) + context.tex_mipmap_bias);
            // }
            // mipmapMaxLevel = mipmapBaseLevel;
            // if (isLogDebugEnabled) {
            //     log.debug("TBIAS_MODE_AUTO " + context.tex_mipmap_bias + ", param=" + maxValue);
            // }
        } else if (context.tex_mipmap_mode == TBIAS_MODE_SLOPE) {
            // TBIAS_MODE_SLOPE uses the tslope_level level supplied by TSLOPE.
            mipmapBaseLevel = (int) ((Math.log(Math.abs(context.tslope_level) / Math.abs(context.zpos)) / Math.log(2)) + context.tex_mipmap_bias);
            mipmapMaxLevel = mipmapBaseLevel;
            if (isLogDebugEnabled) {
                log.debug("TBIAS_MODE_SLOPE " + context.tex_mipmap_bias + ", slope=" + context.tslope_level);
            }
        }

        // Clamp to [0..texture_num_mip_maps]
        mipmapBaseLevel = Math.max(0, Math.min(mipmapBaseLevel, context.texture_num_mip_maps));
        // Clamp to [mipmapBaseLevel..texture_num_mip_maps]
        mipmapMaxLevel = Math.max(mipmapBaseLevel, Math.min(mipmapMaxLevel, context.texture_num_mip_maps));
        if (isLogDebugEnabled) {
            log.debug("Texture Mipmap base=" + mipmapBaseLevel + ", max=" + mipmapMaxLevel + ", textureNumMipmaps=" + context.texture_num_mip_maps);
        }
        re.setTextureMipmapMinLevel(mipmapBaseLevel);
        re.setTextureMipmapMaxLevel(mipmapMaxLevel);

        return useVertexColor;
    }

    private void endRendering(boolean useVertexColor, boolean useTexture, int numberOfVertex) {
        Memory mem = Memory.getInstance();

        // VADDR/IADDR are updated after vertex rendering
        // (IADDR when indexed and VADDR when not).
        // Some games rely on this and don't reload VADDR/IADDR between 2 PRIM/BBOX calls.
        if (vinfo.index == 0) {
            vinfo.ptr_vertex = vinfo.getAddress(mem, numberOfVertex);
        } else {
            vinfo.ptr_index += numberOfVertex * vinfo.index;
        }

        switch (context.tex_map_mode) {
            case TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP: {
                re.disableFlag(IRenderingEngine.RE_TEXTURE_GEN_S);
                re.disableFlag(IRenderingEngine.RE_TEXTURE_GEN_T);
                break;
            }
        }
    }

    public static float[] getOrthoMatrix(float left, float right, float bottom, float top, float near, float far) {
    	float dx = right - left;
    	float dy = top - bottom;
    	float dz = far - near;
    	float[] orthoMatrix = {
        		2.f / dx, 0, 0, 0,
        		0, 2.f / dy, 0, 0,
        		0, 0, -2.f / dz, 0,
        		-(right + left) / dx, -(top + bottom) / dy, -(far + near) / dz, 1
        };

    	return orthoMatrix;
    }

    float spline_n(int i, int j, float u, int[] knot) {
    	if(j == 0) {
    		if(knot[i] <= u && u < knot[i + 1])
    			return 1;
    		return 0;
    	}
    	float res = 0;
    	if(knot[i + j] - knot[i] != 0)
    	    res += (u - knot[i]) / (knot[i + j] - knot[i]) * spline_n(i, j - 1, u, knot);
    	if(knot[i + j + 1] - knot[i + 1] != 0)
    		res += (knot[i + j + 1] - u) / (knot[i + j + 1] - knot[i + 1]) * spline_n(i + 1, j - 1, u, knot);
    	return res;
    }

    int[] spline_knot(int n, int type) {
    	int[] knot = new int[n + 5];
    	for(int i = 0; i < n - 1; i++) {
    		knot[i + 3] = i;
        }

    	if((type & 1) == 0) {
    		knot[0] = -3;
    		knot[1] = -2;
    		knot[2] = -1;
    	}
    	if((type & 2) == 0) {
    		knot[n + 2] = n - 1;
    		knot[n + 3] = n;
    		knot[n + 4] = n + 1;
    	} else {
    		knot[n + 2] = n - 2;
    		knot[n + 3] = n - 2;
    		knot[n + 4] = n - 2;
    	}

    	return knot;
    }

    private void drawSpline(int ucount, int vcount, int utype, int vtype) {
        if (ucount < 4 || vcount < 4) {
            log.warn("Unsupported spline parameters uc=" + ucount + " vc=" + vcount);
            return;
        }

        boolean useVertexColor = initRendering();
        boolean useTexture = vinfo.texture != 0 || context.textureFlag.isEnabled();
        boolean useNormal = context.lightingFlag.isEnabled();

        // Generate control points.
        VertexState[][] ctrlpoints = getControlPoints(ucount, vcount);

        // GE capture.
        if (State.captureGeNextFrame && !isVertexBufferEmbedded()) {
            log.info("Capture drawSpline");
            CaptureManager.captureRAM(vinfo.ptr_vertex, vinfo.vertexSize * ucount * vcount);
        }

        // Generate patch VertexState.
        VertexState[][] patch = new VertexState[context.patch_div_s + 1][context.patch_div_t + 1];

        // Calculate knot arrays.
        int n = ucount - 1;
        int m = vcount - 1;
        int[] knot_u = spline_knot(n, utype);
        int[] knot_v = spline_knot(m, vtype);

        // The spline grows to a limit defined by n - 2 for u and m - 2 for v.
        // This limit is open, so we need to get a very close approximation of it.
        float limit = 2.000001f;

        // Process spline vertexes with Cox-deBoor's algorithm.
        for(int j = 0; j <= context.patch_div_t; j++) {
        	float v = (float)j * (float)(m - limit) / (float)context.patch_div_t;

        	for(int i = 0; i <= context.patch_div_s; i++) {
        		float u = (float)i * (float)(n - limit) / (float)context.patch_div_s;

        		patch[i][j] = new VertexState();
        		VertexState p = patch[i][j];

        		for(int ii = 0; ii <= n; ii++) {
        			for(int jj = 0; jj <= m; jj++) {
        				float f = spline_n(ii, 3, u, knot_u) * spline_n(jj, 3, v, knot_v);
        				if(f != 0) {
        					pointMultAdd(p, ctrlpoints[ii][jj], f, useVertexColor, useTexture, useNormal);
        				}
        			}
        		}
        		if(useTexture && vinfo.texture == 0) {
        			p.t[0] = u;
        			p.t[1] = v;
        		}
        	}
        }

        drawCurvedSurface(patch, ucount, vcount, useVertexColor, useTexture, useNormal);
    }

	private void pointMultAdd(VertexState dest, VertexState src, float f, boolean useVertexColor, boolean useTexture, boolean useNormal) {
		dest.p[0] += f * src.p[0];
		dest.p[1] += f * src.p[1];
		dest.p[2] += f * src.p[2];
		if(useTexture) {
			dest.t[0] += f * src.t[0];
			dest.t[1] += f * src.t[1];
		}
		if(useVertexColor) {
			dest.c[0] += f * src.c[0];
			dest.c[1] += f * src.c[1];
			dest.c[2] += f * src.c[2];
		}
		if(useNormal) {
			dest.n[0] += f * src.n[0];
			dest.n[1] += f * src.n[1];
			dest.n[2] += f * src.n[2];
		}
	}

    private void drawBezier(int ucount, int vcount) {
        if ((ucount - 1) % 3 != 0 && (vcount - 1) % 3 != 0) {
            log.warn("Unsupported bezier parameters ucount=" + ucount + " vcount=" + vcount);
            return;
        }

        boolean useVertexColor = initRendering();
        boolean useTexture = vinfo.texture != 0 || context.textureFlag.isEnabled();
        boolean useNormal = context.lightingFlag.isEnabled();

        VertexState[][] anchors = getControlPoints(ucount, vcount);

        // Don't capture the ram if the vertex list is embedded in the display list. TODO handle stall_addr == 0 better
        // TODO may need to move inside the loop if indices are used, or find the largest index so we can calculate the size of the vertex list
        if (State.captureGeNextFrame && !isVertexBufferEmbedded()) {
            log.info("Capture drawBezier");
            CaptureManager.captureRAM(vinfo.ptr_vertex, vinfo.vertexSize * ucount * vcount);
        }

        // Generate patch VertexState.
        VertexState[][] patch = new VertexState[context.patch_div_s + 1][context.patch_div_t + 1];

        // Number of patches in the U and V directions
        int upcount = ucount / 3;
        int vpcount = vcount / 3;

        float[][] ucoeff = new float[context.patch_div_s + 1][];

        for(int j = 0; j <= context.patch_div_t; j++) {
        	float vglobal = (float)j * vpcount / (float)context.patch_div_t;

        	int vpatch = (int)vglobal; // Patch number
        	float v = vglobal - vpatch;
        	if(j == context.patch_div_t) {
    			vpatch--;
    			v = 1.f;
    		}
        	float[] vcoeff = BernsteinCoeff(v);

        	for(int i = 0; i <= context.patch_div_s; i++) {
        		float uglobal = (float)i * upcount / (float)context.patch_div_s;
        		int upatch = (int)uglobal;
        		float u = uglobal - upatch;
        		if(i == context.patch_div_s) {
        			upatch--;
        			u = 1.f;
        		}
        		ucoeff[i] = BernsteinCoeff(u);

        		patch[i][j] = new VertexState();
        		VertexState p = patch[i][j];

        		for(int ii = 0; ii < 4; ++ii) {
        			for(int jj = 0; jj < 4; ++jj) {
        				pointMultAdd(p,
        						anchors[3 * upatch + ii][3 * vpatch + jj],
        						ucoeff[i][ii] * vcoeff[jj],
        						useVertexColor, useTexture, useNormal);
        			}
        		}

        		if(useTexture && vinfo.texture == 0) {
        			p.t[0] = uglobal;
        			p.t[1] = vglobal;
        		}
        	}
        }

        drawCurvedSurface(patch, ucount, vcount, useVertexColor, useTexture, useNormal);
    }

	private void drawCurvedSurface(VertexState[][] patch, int ucount, int vcount,
			boolean useVertexColor, boolean useTexture, boolean useNormal) {
		// TODO: Compute the normals
		setDataPointers(3, useVertexColor, 4, useTexture, 2, useNormal, 0, true);

		re.setVertexInfo(vinfo, false, useVertexColor);

		ByteBuffer drawByteBuffer = bufferManager.getBuffer(bufferId);
		FloatBuffer drawFloatBuffer = drawByteBuffer.asFloatBuffer();
        for(int j = 0; j <= context.patch_div_t - 1; j++) {
        	drawFloatBuffer.clear();

        	for(int i = 0; i <= context.patch_div_s; i++) {
        		VertexState v1 = patch[i][j];
                VertexState v2 = patch[i][j + 1];

        		if(useTexture)     drawFloatBuffer.put(v1.t);
        		if(useVertexColor) drawFloatBuffer.put(v1.c);
        		if(useNormal)      drawFloatBuffer.put(v1.n);
        		drawFloatBuffer.put(v1.p);

        		if(useTexture)     drawFloatBuffer.put(v2.t);
        		if(useVertexColor) drawFloatBuffer.put(v2.c);
        		if(useNormal)      drawFloatBuffer.put(v2.n);
        		drawFloatBuffer.put(v2.p);
        	}

        	bufferManager.setBufferData(bufferId, drawFloatBuffer.position() * SIZEOF_FLOAT, drawByteBuffer.rewind(), IRenderingEngine.RE_STREAM_DRAW);
            re.drawArrays(patch_prim_types[context.patch_prim], 0, (context.patch_div_s + 1) * 2);
        }

        endRendering(useVertexColor, useTexture, ucount * vcount);
	}

	private VertexState[][] getControlPoints(int ucount, int vcount) {
		VertexState[][] controlPoints = new VertexState[ucount][vcount];

		Memory mem = Memory.getInstance();
        for (int u = 0; u < ucount; u++) {
            for (int v = 0; v < vcount; v++) {
                int addr = vinfo.getAddress(mem, v * ucount + u);
                VertexState vs = vinfo.readVertex(mem, addr);
                if (isLogDebugEnabled) {
                	log(String.format("control point #%d,%d p(%f,%f,%f) t(%f,%f), c(%f,%f,%f)",
                			u, v,
                			vs.p[0], vs.p[1], vs.p[2],
                			vs.t[0], vs.t[1],
                			vs.c[0], vs.c[1], vs.c[2]));
                }
                controlPoints[u][v] = vs;
            }
        }
        return controlPoints;
	}

    private float[] BernsteinCoeff(float u) {
        float uPow2 = u * u;
        float uPow3 = uPow2 * u;
        float u1 = 1 - u;
        float u1Pow2 = u1 * u1;
        float u1Pow3 = u1Pow2 * u1;
        return new float[] {u1Pow3, 3 * u * u1Pow2, 3 * uPow2 * u1, uPow3 };
    }

    private Buffer getTexture32BitBuffer(int texaddr, int level) {
        Buffer final_buffer = null;

        if (!context.texture_swizzle) {
            // texture_width might be larger than texture_buffer_width
            int bufferlen = Math.max(context.texture_buffer_width[level], context.texture_width[level]) * context.texture_height[level] * 4;
            final_buffer = Memory.getInstance().getBuffer(texaddr, bufferlen);
            if (final_buffer == null) {
                int length = context.texture_buffer_width[level] * context.texture_height[level];
                IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length * 4, 4);
                for (int i = 0; i < length; i++) {
                    tmp_texture_buffer32[i] = memoryReader.readNext();
                }
                final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
            }

            if (State.captureGeNextFrame) {
                log.info("Capture getTexture32BitBuffer unswizzled");
                CaptureManager.captureRAM(texaddr, bufferlen);
            }
        } else {
            final_buffer = unswizzleTextureFromMemory(texaddr, 4, level);
        }

        return final_buffer;
    }

    public final static String getPsmName(final int psm) {
        return (psm >= 0 && psm < psm_names.length)
                ? psm_names[psm % psm_names.length]
                : "PSM_UNKNOWN" + psm;
    }

    public final static String getLOpName(final int ops) {
        return (ops >= 0 && ops < logical_ops_names.length)
                ? logical_ops_names[ops % logical_ops_names.length]
                : "UNKNOWN_LOP" + ops;
    }

    private int getCompressedTextureSize(int level, int compressionRatio) {
        return getCompressedTextureSize(context.texture_width[level], context.texture_height[level], compressionRatio);
    }

    public static int getCompressedTextureSize(int width, int height, int compressionRatio) {
        int compressedTextureWidth = ((width + 3) / 4) * 4;
        int compressedTextureHeight = ((height + 3) / 4) * 4;
        int compressedTextureSize = compressedTextureWidth * compressedTextureHeight * 4 / compressionRatio;

        return compressedTextureSize;
    }

    private void updateGeBuf() {
        if (geBufChanged) {
            display.hleDisplaySetGeBuf(context.fbp, context.fbw, context.psm, somethingDisplayed);
            geBufChanged = false;

            textureChanged = true;
            maxSpriteHeight = 0;
            projectionMatrixUpload.setChanged(true);
            modelMatrixUpload.setChanged(true);
            viewMatrixUpload.setChanged(true);
            textureMatrixUpload.setChanged(true);
            viewportChanged = true;
            depthChanged = true;
            materialChanged = true;
        }
    }
    // For capture/replay

    public int getFBP() {
        return context.fbp;
    }

    public int getFBW() {
        return context.fbw;
    }

    public int getZBP() {
        return context.zbp;
    }

    public int getZBW() {
        return context.zbw;
    }

    public int getPSM() {
        return context.psm;
    }

    private boolean isVertexBufferEmbedded() {
        // stall_addr may be 0
        return (vinfo.ptr_vertex >= currentList.list_addr && vinfo.ptr_vertex < currentList.getStallAddr());
    }

    private boolean isVRAM(int addr) {
        addr &= Memory.addressMask;

        return addr >= MemoryMap.START_VRAM && addr <= MemoryMap.END_VRAM;
    }

    private void hlePerformAction(IAction action, Semaphore sync) {
        hleAction = action;

        while (true) {
            try {
                sync.acquire();
                break;
            } catch (InterruptedException e) {
                // Retry again..
            }
        }
    }

    public void hleSaveContext(pspGeContext pspContext) {
        // If we are rendering, we have to wait for a consistent state
        // before saving the context: let the display thread perform
        // the save when appropriate.
        if (hasDrawLists() || currentList != null) {
            Semaphore sync = new Semaphore(0);
            hlePerformAction(new SaveContextAction(pspContext, sync), sync);
        } else {
            saveContext(pspContext);
        }
    }

    public void hleRestoreContext(pspGeContext pspContext) {
        // If we are rendering, we have to wait for a consistent state
        // before restoring the context: let the display thread perform
        // the restore when appropriate.
        if (hasDrawLists() || currentList != null) {
            Semaphore sync = new Semaphore(0);
            hlePerformAction(new RestoreContextAction(pspContext, sync), sync);
        } else {
            restoreContext(pspContext);
        }
    }

    private void saveContext(pspGeContext pspContext) {
        pspContext.base = context.base;
        pspContext.baseOffset = context.baseOffset;

        pspContext.fbp = context.fbp;
        pspContext.fbw = context.fbw;
        pspContext.zbp = context.zbp;
        pspContext.zbw = context.zbw;
        pspContext.psm = context.psm;

        pspContext.flags = 0;
        for (EnableDisableFlag flag : context.flags) {
            pspContext.flags = flag.save(pspContext.flags);
        }

        pspContext.region_x1 = context.region_x1;
        pspContext.region_y1 = context.region_y1;
        pspContext.region_x2 = context.region_x2;
        pspContext.region_y2 = context.region_y2;
        pspContext.region_width = context.region_width;
        pspContext.region_height = context.region_height;
        pspContext.scissor_x1 = context.scissor_x1;
        pspContext.scissor_y1 = context.scissor_y1;
        pspContext.scissor_x2 = context.scissor_x2;
        pspContext.scissor_y2 = context.scissor_y2;
        pspContext.scissor_width = context.scissor_width;
        pspContext.scissor_height = context.scissor_height;
        pspContext.offset_x = context.offset_x;
        pspContext.offset_y = context.offset_y;
        pspContext.viewport_width = context.viewport_width;
        pspContext.viewport_height = context.viewport_height;
        pspContext.viewport_cx = context.viewport_cx;
        pspContext.viewport_cy = context.viewport_cy;

        System.arraycopy(context.proj_uploaded_matrix, 0, pspContext.proj_uploaded_matrix, 0, context.proj_uploaded_matrix.length);
        System.arraycopy(context.texture_uploaded_matrix, 0, pspContext.texture_uploaded_matrix, 0, context.texture_uploaded_matrix.length);
        System.arraycopy(context.model_uploaded_matrix, 0, pspContext.model_uploaded_matrix, 0, context.model_uploaded_matrix.length);
        System.arraycopy(context.view_uploaded_matrix, 0, pspContext.view_uploaded_matrix, 0, context.view_uploaded_matrix.length);
        System.arraycopy(context.morph_weight, 0, pspContext.morph_weight, 0, context.morph_weight.length);
        System.arraycopy(context.tex_envmap_matrix, 0, pspContext.tex_envmap_matrix, 0, context.tex_envmap_matrix.length);
        if (pspGeContext.fullVersion) {
            for (int i = 0; i < context.bone_uploaded_matrix.length; i++) {
                System.arraycopy(context.bone_uploaded_matrix[i], 0, pspContext.bone_uploaded_matrix[i], 0, context.bone_uploaded_matrix[i].length);
            }
        }
        for (int i = 0; i < context.light_pos.length; i++) {
            System.arraycopy(context.light_pos[i], 0, pspContext.light_pos[i], 0, context.light_pos[i].length);
            System.arraycopy(context.light_dir[i], 0, pspContext.light_dir[i], 0, context.light_dir[i].length);
        }

        System.arraycopy(context.light_enabled, 0, pspContext.light_enabled, 0, context.light_enabled.length);
        System.arraycopy(context.light_type, 0, pspContext.light_type, 0, context.light_type.length);
        System.arraycopy(context.light_kind, 0, pspContext.light_kind, 0, context.light_kind.length);
        System.arraycopy(context.spotLightExponent, 0, pspContext.spotLightExponent, 0, context.spotLightExponent.length);
        System.arraycopy(context.spotLightCutoff, 0, pspContext.spotLightCutoff, 0, context.spotLightCutoff.length);

        System.arraycopy(context.fog_color, 0, pspContext.fog_color, 0, context.fog_color.length);
        pspContext.fog_far = context.fog_far;
        pspContext.fog_dist = context.fog_dist;

        pspContext.nearZ = context.nearZ;
        pspContext.farZ = context.farZ;
        pspContext.zscale = context.zscale;
        pspContext.zpos = context.zpos;

        pspContext.mat_flags = context.mat_flags;
        System.arraycopy(context.mat_ambient, 0, pspContext.mat_ambient, 0, context.mat_ambient.length);
        System.arraycopy(context.mat_diffuse, 0, pspContext.mat_diffuse, 0, context.mat_diffuse.length);
        System.arraycopy(context.mat_specular, 0, pspContext.mat_specular, 0, context.mat_specular.length);
        System.arraycopy(context.mat_emissive, 0, pspContext.mat_emissive, 0, context.mat_emissive.length);

        System.arraycopy(context.ambient_light, 0, pspContext.ambient_light, 0, context.ambient_light.length);

        pspContext.texture_storage = context.texture_storage;
        pspContext.texture_num_mip_maps = context.texture_num_mip_maps;
        pspContext.texture_swizzle = context.texture_swizzle;

        System.arraycopy(context.texture_base_pointer, 0, pspContext.texture_base_pointer, 0, context.texture_base_pointer.length);
        System.arraycopy(context.texture_width, 0, pspContext.texture_width, 0, context.texture_width.length);
        System.arraycopy(context.texture_height, 0, pspContext.texture_height, 0, context.texture_height.length);
        System.arraycopy(context.texture_buffer_width, 0, pspContext.texture_buffer_width, 0, context.texture_buffer_width.length);
        pspContext.tex_min_filter = context.tex_min_filter;
        pspContext.tex_mag_filter = context.tex_mag_filter;

        pspContext.tex_translate_x = context.tex_translate_x;
        pspContext.tex_translate_y = context.tex_translate_y;
        pspContext.tex_scale_x = context.tex_scale_x;
        pspContext.tex_scale_y = context.tex_scale_y;
        System.arraycopy(context.tex_env_color, 0, pspContext.tex_env_color, 0, context.tex_env_color.length);
        pspContext.tex_enable = context.textureFlag.isEnabledInt();

        pspContext.tex_clut_addr = context.tex_clut_addr;
        pspContext.tex_clut_num_blocks = context.tex_clut_num_blocks;
        pspContext.tex_clut_mode = context.tex_clut_mode;
        pspContext.tex_clut_shift = context.tex_clut_shift;
        pspContext.tex_clut_mask = context.tex_clut_mask;
        pspContext.tex_clut_start = context.tex_clut_start;
        pspContext.tex_wrap_s = context.tex_wrap_s;
        pspContext.tex_wrap_t = context.tex_wrap_t;
        pspContext.patch_div_s = context.patch_div_s;
        pspContext.patch_div_t = context.patch_div_t;

        pspContext.transform_mode = context.transform_mode;

        pspContext.textureTx_sourceAddress = context.textureTx_sourceAddress;
        pspContext.textureTx_sourceLineWidth = context.textureTx_sourceLineWidth;
        pspContext.textureTx_destinationAddress = context.textureTx_destinationAddress;
        pspContext.textureTx_destinationLineWidth = context.textureTx_destinationLineWidth;
        pspContext.textureTx_width = context.textureTx_width;
        pspContext.textureTx_height = context.textureTx_height;
        pspContext.textureTx_sx = context.textureTx_sx;
        pspContext.textureTx_sy = context.textureTx_sy;
        pspContext.textureTx_dx = context.textureTx_dx;
        pspContext.textureTx_dy = context.textureTx_dy;
        pspContext.textureTx_pixelSize = context.textureTx_pixelSize;

        System.arraycopy(context.dfix_color, 0, pspContext.dfix_color, 0, context.dfix_color.length);
        System.arraycopy(context.sfix_color, 0, pspContext.sfix_color, 0, context.sfix_color.length);
        pspContext.blend_src = context.blend_src;
        pspContext.blend_dst = context.blend_dst;

        pspContext.depthFunc = context.depthFunc;

        pspContext.tex_map_mode = context.tex_map_mode;
        pspContext.tex_proj_map_mode = context.tex_proj_map_mode;

        System.arraycopy(context.colorMask, 0, pspContext.glColorMask, 0, context.colorMask.length);

        pspContext.copyGLToContext(display.getGL());
    }

    private void restoreContext(pspGeContext pspContext) {
    	context.base = pspContext.base;
    	context.baseOffset = pspContext.baseOffset;

    	context.fbp = pspContext.fbp;
    	context.fbw = pspContext.fbw;
    	context.zbp = pspContext.zbp;
    	context.zbw = pspContext.zbw;
    	context.psm = pspContext.psm;

        for (EnableDisableFlag flag : context.flags) {
            flag.restore(pspContext.flags);
        }

        context.region_x1 = pspContext.region_x1;
        context.region_y1 = pspContext.region_y1;
        context.region_x2 = pspContext.region_x2;
        context.region_y2 = pspContext.region_y2;
        context.region_width = pspContext.region_width;
        context.region_height = pspContext.region_height;
        context.scissor_x1 = pspContext.scissor_x1;
        context.scissor_y1 = pspContext.scissor_y1;
        context.scissor_x2 = pspContext.scissor_x2;
        context.scissor_y2 = pspContext.scissor_y2;
        context.scissor_width = pspContext.scissor_width;
        context.scissor_height = pspContext.scissor_height;
        context.offset_x = pspContext.offset_x;
        context.offset_y = pspContext.offset_y;
        context.viewport_width = pspContext.viewport_width;
        context.viewport_height = pspContext.viewport_height;
        context.viewport_cx = pspContext.viewport_cx;
        context.viewport_cy = pspContext.viewport_cy;

        System.arraycopy(pspContext.proj_uploaded_matrix, 0, context.proj_uploaded_matrix, 0, context.proj_uploaded_matrix.length);
        System.arraycopy(pspContext.texture_uploaded_matrix, 0, context.texture_uploaded_matrix, 0, context.texture_uploaded_matrix.length);
        System.arraycopy(pspContext.model_uploaded_matrix, 0, context.model_uploaded_matrix, 0, context.model_uploaded_matrix.length);
        System.arraycopy(pspContext.view_uploaded_matrix, 0, context.view_uploaded_matrix, 0, context.view_uploaded_matrix.length);
        System.arraycopy(pspContext.morph_weight, 0, context.morph_weight, 0, context.morph_weight.length);
        System.arraycopy(pspContext.tex_envmap_matrix, 0, context.tex_envmap_matrix, 0, context.tex_envmap_matrix.length);
        if (pspGeContext.fullVersion) {
            for (int i = 0; i < context.bone_uploaded_matrix.length; i++) {
                System.arraycopy(pspContext.bone_uploaded_matrix[i], 0, context.bone_uploaded_matrix[i], 0, context.bone_uploaded_matrix[i].length);
            }
        }
        for (int i = 0; i < context.light_pos.length; i++) {
            System.arraycopy(pspContext.light_pos[i], 0, context.light_pos[i], 0, context.light_pos[i].length);
            System.arraycopy(pspContext.light_dir[i], 0, context.light_dir[i], 0, context.light_dir[i].length);
        }

        System.arraycopy(pspContext.light_enabled, 0, context.light_enabled, 0, context.light_enabled.length);
        System.arraycopy(pspContext.light_type, 0, context.light_type, 0, context.light_type.length);
        System.arraycopy(pspContext.light_kind, 0, context.light_kind, 0, context.light_kind.length);
        System.arraycopy(pspContext.spotLightExponent, 0, context.spotLightExponent, 0, context.spotLightExponent.length);
        System.arraycopy(pspContext.spotLightCutoff, 0, context.spotLightCutoff, 0,context. spotLightCutoff.length);

        System.arraycopy(pspContext.fog_color, 0, context.fog_color, 0, context.fog_color.length);
        context.fog_far = pspContext.fog_far;
        context.fog_dist = pspContext.fog_dist;

        context.nearZ = pspContext.nearZ;
        context.farZ = pspContext.farZ;
        context.zscale = pspContext.zscale;
        context.zpos = pspContext.zpos;

        context.mat_flags = pspContext.mat_flags;
        System.arraycopy(pspContext.mat_ambient, 0, context.mat_ambient, 0, context.mat_ambient.length);
        System.arraycopy(pspContext.mat_diffuse, 0, context.mat_diffuse, 0, context.mat_diffuse.length);
        System.arraycopy(pspContext.mat_specular, 0, context.mat_specular, 0, context.mat_specular.length);
        System.arraycopy(pspContext.mat_emissive, 0, context.mat_emissive, 0, context.mat_emissive.length);

        System.arraycopy(pspContext.ambient_light, 0, context.ambient_light, 0, context.ambient_light.length);

        context.texture_storage = pspContext.texture_storage;
        context.texture_num_mip_maps = pspContext.texture_num_mip_maps;
        context.texture_swizzle = pspContext.texture_swizzle;

        System.arraycopy(pspContext.texture_base_pointer, 0, context.texture_base_pointer, 0, context.texture_base_pointer.length);
        System.arraycopy(pspContext.texture_width, 0, context.texture_width, 0, context.texture_width.length);
        System.arraycopy(pspContext.texture_height, 0, context.texture_height, 0, context.texture_height.length);
        System.arraycopy(pspContext.texture_buffer_width, 0, context.texture_buffer_width, 0, context.texture_buffer_width.length);
        context.tex_min_filter = pspContext.tex_min_filter;
        context.tex_mag_filter = pspContext.tex_mag_filter;

        context.tex_translate_x = pspContext.tex_translate_x;
        context.tex_translate_y = pspContext.tex_translate_y;
        context.tex_scale_x = pspContext.tex_scale_x;
        context.tex_scale_y = pspContext.tex_scale_y;
        System.arraycopy(pspContext.tex_env_color, 0, context.tex_env_color, 0, context.tex_env_color.length);
        context.textureFlag.setEnabled(pspContext.tex_enable);

        context.tex_clut_addr = pspContext.tex_clut_addr;
        context.tex_clut_num_blocks = pspContext.tex_clut_num_blocks;
        context.tex_clut_mode = pspContext.tex_clut_mode;
        context.tex_clut_shift = pspContext.tex_clut_shift;
        context.tex_clut_mask = pspContext.tex_clut_mask;
        context.tex_clut_start = pspContext.tex_clut_start;
        context.tex_wrap_s = pspContext.tex_wrap_s;
        context.tex_wrap_t = pspContext.tex_wrap_t;
        context.patch_div_s = pspContext.patch_div_s;
        context.patch_div_t = pspContext.patch_div_t;

        context.transform_mode = pspContext.transform_mode;

        context.textureTx_sourceAddress = pspContext.textureTx_sourceAddress;
        context.textureTx_sourceLineWidth = pspContext.textureTx_sourceLineWidth;
        context.textureTx_destinationAddress = pspContext.textureTx_destinationAddress;
        context.textureTx_destinationLineWidth = pspContext.textureTx_destinationLineWidth;
        context.textureTx_width = pspContext.textureTx_width;
        context.textureTx_height = pspContext.textureTx_height;
        context.textureTx_sx = pspContext.textureTx_sx;
        context.textureTx_sy = pspContext.textureTx_sy;
        context.textureTx_dx = pspContext.textureTx_dx;
        context.textureTx_dy = pspContext.textureTx_dy;
        context.textureTx_pixelSize = pspContext.textureTx_pixelSize;

        System.arraycopy(pspContext.dfix_color, 0, context.dfix_color, 0, context.dfix_color.length);
        System.arraycopy(pspContext.sfix_color, 0, context.sfix_color, 0, context.sfix_color.length);
        context.blend_src = pspContext.blend_src;
        context.blend_dst = pspContext.blend_dst;

        context.depthFunc = pspContext.depthFunc;

        context.tex_map_mode = pspContext.tex_map_mode;
        context.tex_proj_map_mode = pspContext.tex_proj_map_mode;

        System.arraycopy(pspContext.glColorMask, 0, context.colorMask, 0, context.colorMask.length);

        pspContext.copyContextToGL(display.getGL());

        projectionMatrixUpload.setChanged(true);
        modelMatrixUpload.setChanged(true);
        viewMatrixUpload.setChanged(true);
        textureMatrixUpload.setChanged(true);
        lightingChanged = true;
        blendChanged = true;
        textureChanged = true;
        geBufChanged = true;
        viewportChanged = true;
        depthChanged = true;
        materialChanged = true;
    }

    public boolean isUsingTRXKICK() {
        return usingTRXKICK;
    }

    public int getMaxSpriteHeight() {
        return maxSpriteHeight;
    }

    public void setUseVertexCache(boolean useVertexCache) {
        // VertexCache is relying on VBO
    	if (bufferManager != null && !bufferManager.useVBO()) {
    		useVertexCache = false;
    	}

        this.useVertexCache = useVertexCache;
        if (useVertexCache) {
            VideoEngine.log.info("Using Vertex Cache");
        }
    }

    public int getBase() {
        return context.base;
    }

    public void setBase(int base) {
    	context.base = base;
    }

    public int getBaseOffset() {
        return context.baseOffset;
    }

    public void setBaseOffset(int baseOffset) {
    	context.baseOffset = baseOffset;
    }

    public void addVideoTexture(int address) {
    	videoTextures.add(address);
    }

    public void resetVideoTextures() {
    	videoTextures.clear();
    }

    protected void matrixMult(float[] result, float[] m1, float[] m2) {
    	// If the result has to be stored into one of the input matrix,
    	// store the result in a temp array first.
    	float[] origResult = null;
    	if (result == m1 || result == m2) {
    		origResult = result;
    		result = new float[4 * 4];
    	}

    	for (int i = 0; i < 4; i++) {
    		for (int j = 0; j < 4; j++) {
    			float s = 0;
    			for (int k = 0; k < 4; k++) {
    				s += m1[k * 4 + j] * m2[i * 4 + k];
    			}
    			result[i * 4 + j] = s;
    		}
    	}

    	if (origResult != null) {
    		System.arraycopy(result, 0, origResult, 0, result.length);
    	}
    }

    protected void vectorMult(float[] result, float[] m, float[] v) {
    	for (int i = 0; i < result.length; i++) {
    		float s = 0;
    		for (int j = 0; j < v.length; j++) {
    			s += v[j] * m[j * 4 + i];
    		}
    		result[i] = s;
    	}
    }

    private class SaveContextAction implements IAction {

        private pspGeContext context;
        private Semaphore sync;

        public SaveContextAction(pspGeContext context, Semaphore sync) {
            this.context = context;
            this.sync = sync;
        }

        @Override
        public void execute() {
            saveContext(context);
            sync.release();
        }
    }

    private class RestoreContextAction implements IAction {

        private pspGeContext context;
        private Semaphore sync;

        public RestoreContextAction(pspGeContext context, Semaphore sync) {
            this.context = context;
            this.sync = sync;
        }

        @Override
        public void execute() {
            restoreContext(context);
            sync.release();
        }
    }
}