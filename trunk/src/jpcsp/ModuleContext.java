package jpcsp;

import java.util.List;
import java.util.LinkedList;
import jpcsp.Loader;
import jpcsp.format.DeferredStub;
import jpcsp.format.PSPModuleInfo;

public class ModuleContext {

    // See Loader class for valid formats
    public int fileFormat;
    public String pspfilename; // boot path

    // The space consumed by the program image
    public int loadAddressLow, loadAddressHigh;
    public int baseAddress; // should in theory be the same as loadAddressLow
    public int entryAddress; // this will be the relocated entry address

    // address/size pairs
    // used by the debugger/instruction counter
    public int[] textsection;
    public int[] initsection;
    public int[] finisection;
    public int[] stubtextsection;

    // .rodata.sceModuleInfo
    // module info currently isn't relocated, so you might have to do gp = baseAddress + gp
    public PSPModuleInfo moduleInfo;

    public List<DeferredStub> unresolvedImports;
    public int importFixupAttempts;

    public ModuleContext() {
        fileFormat = Loader.FORMAT_UNKNOWN;
        pspfilename = "";

        loadAddressLow = 0;
        loadAddressHigh = 0;
        baseAddress = 0;
        entryAddress = 0;

        textsection = new int[2];
        initsection = new int[2];
        finisection = new int[2];
        stubtextsection = new int[2];

        moduleInfo = new PSPModuleInfo();

        unresolvedImports = new LinkedList<DeferredStub>();
        importFixupAttempts = 0;
    }
}
