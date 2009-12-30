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

package jpcsp.HLE.modules150;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JOptionPane;

import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.SceUtilityMsgDialogParams;
import jpcsp.HLE.kernel.types.SceUtilityOskParams;
import jpcsp.HLE.kernel.types.SceUtilitySavedataParam;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.Modules;
import jpcsp.HLE.pspiofilemgr;
import jpcsp.hardware.MemoryStick;
import jpcsp.util.Utilities;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;

import jpcsp.Allegrex.CpuState; // New-Style Processor
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;

public class sceUtility implements HLEModule {
	@Override
	public String getName() { return "sceUtility"; }

	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.addFunction(sceUtilityGameSharingInitStartFunction, 0xC492F751);
			mm.addFunction(sceUtilityGameSharingShutdownStartFunction, 0xEFC6F80F);
			mm.addFunction(sceUtilityGameSharingUpdateFunction, 0x7853182D);
			mm.addFunction(sceUtilityGameSharingGetStatusFunction, 0x946963F3);
			mm.addFunction(sceNetplayDialogInitStartFunction, 0x3AD50AE7);
			mm.addFunction(sceNetplayDialogShutdownStartFunction, 0xBC6B6296);
			mm.addFunction(sceNetplayDialogUpdateFunction, 0x417BED54);
			mm.addFunction(sceNetplayDialogGetStatusFunction, 0xB6CEE597);
			mm.addFunction(sceUtilityNetconfInitStartFunction, 0x4DB1E739);
			mm.addFunction(sceUtilityNetconfShutdownStartFunction, 0xF88155F6);
			mm.addFunction(sceUtilityNetconfUpdateFunction, 0x91E70E35);
			mm.addFunction(sceUtilityNetconfGetStatusFunction, 0x6332AA39);
			mm.addFunction(sceUtilitySavedataInitStartFunction, 0x50C4CD57);
			mm.addFunction(sceUtilitySavedataShutdownStartFunction, 0x9790B33C);
			mm.addFunction(sceUtilitySavedataUpdateFunction, 0xD4B95FFB);
			mm.addFunction(sceUtilitySavedataGetStatusFunction, 0x8874DBE0);
			mm.addFunction(sceUtility_2995D020Function, 0x2995D020);
			mm.addFunction(sceUtility_B62A4061Function, 0xB62A4061);
			mm.addFunction(sceUtility_ED0FAD38Function, 0xED0FAD38);
			mm.addFunction(sceUtility_88BC7406Function, 0x88BC7406);
			mm.addFunction(sceUtilityMsgDialogInitStartFunction, 0x2AD8E239);
			mm.addFunction(sceUtilityMsgDialogShutdownStartFunction, 0x67AF3428);
			mm.addFunction(sceUtilityMsgDialogUpdateFunction, 0x95FC253B);
			mm.addFunction(sceUtilityMsgDialogGetStatusFunction, 0x9A1C91D7);
			mm.addFunction(sceUtilityOskInitStartFunction, 0xF6269B82);
			mm.addFunction(sceUtilityOskShutdownStartFunction, 0x3DFAEBA9);
			mm.addFunction(sceUtilityOskUpdateFunction, 0x4B85C861);
			mm.addFunction(sceUtilityOskGetStatusFunction, 0xF3F76017);
			mm.addFunction(sceUtilitySetSystemParamIntFunction, 0x45C18506);
			mm.addFunction(sceUtilitySetSystemParamStringFunction, 0x41E30674);
			mm.addFunction(sceUtilityGetSystemParamIntFunction, 0xA5DA2406);
			mm.addFunction(sceUtilityGetSystemParamStringFunction, 0x34B78343);
			mm.addFunction(sceUtilityCheckNetParamFunction, 0x5EEE6548);
			mm.addFunction(sceUtilityGetNetParamFunction, 0x434D4B3A);

            gamesharing_status = PSP_UTILITY_ERROR_NOT_INITED;
            netplaydialog_status = PSP_UTILITY_ERROR_NOT_INITED;
            netconf_status = PSP_UTILITY_ERROR_NOT_INITED;

            savedata_status = PSP_UTILITY_ERROR_NOT_INITED;
            savedata_mode = -1;

            msgdialog_status = PSP_UTILITY_ERROR_NOT_INITED;
            msgdialog_params = null;

            osk_status = PSP_UTILITY_DIALOG_QUIT;
		}
	}

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.removeFunction(sceUtilityGameSharingInitStartFunction);
			mm.removeFunction(sceUtilityGameSharingShutdownStartFunction);
			mm.removeFunction(sceUtilityGameSharingUpdateFunction);
			mm.removeFunction(sceUtilityGameSharingGetStatusFunction);
			mm.removeFunction(sceNetplayDialogInitStartFunction);
			mm.removeFunction(sceNetplayDialogShutdownStartFunction);
			mm.removeFunction(sceNetplayDialogUpdateFunction);
			mm.removeFunction(sceNetplayDialogGetStatusFunction);
			mm.removeFunction(sceUtilityNetconfInitStartFunction);
			mm.removeFunction(sceUtilityNetconfShutdownStartFunction);
			mm.removeFunction(sceUtilityNetconfUpdateFunction);
			mm.removeFunction(sceUtilityNetconfGetStatusFunction);
			mm.removeFunction(sceUtilitySavedataInitStartFunction);
			mm.removeFunction(sceUtilitySavedataShutdownStartFunction);
			mm.removeFunction(sceUtilitySavedataUpdateFunction);
			mm.removeFunction(sceUtilitySavedataGetStatusFunction);
			mm.removeFunction(sceUtility_2995D020Function);
			mm.removeFunction(sceUtility_B62A4061Function);
			mm.removeFunction(sceUtility_ED0FAD38Function);
			mm.removeFunction(sceUtility_88BC7406Function);
			mm.removeFunction(sceUtilityMsgDialogInitStartFunction);
			mm.removeFunction(sceUtilityMsgDialogShutdownStartFunction);
			mm.removeFunction(sceUtilityMsgDialogUpdateFunction);
			mm.removeFunction(sceUtilityMsgDialogGetStatusFunction);
			mm.removeFunction(sceUtilityOskInitStartFunction);
			mm.removeFunction(sceUtilityOskShutdownStartFunction);
			mm.removeFunction(sceUtilityOskUpdateFunction);
			mm.removeFunction(sceUtilityOskGetStatusFunction);
			mm.removeFunction(sceUtilitySetSystemParamIntFunction);
			mm.removeFunction(sceUtilitySetSystemParamStringFunction);
			mm.removeFunction(sceUtilityGetSystemParamIntFunction);
			mm.removeFunction(sceUtilityGetSystemParamStringFunction);
			mm.removeFunction(sceUtilityCheckNetParamFunction);
			mm.removeFunction(sceUtilityGetNetParamFunction);

		}
	}

    public static final int PSP_SYSTEMPARAM_ID_STRING_NICKNAME = 1;
    public static final int PSP_SYSTEMPARAM_ID_INT_ADHOC_CHANNEL = 2;
    public static final int PSP_SYSTEMPARAM_ID_INT_WLAN_POWERSAVE = 3;
    public static final int PSP_SYSTEMPARAM_ID_INT_DATE_FORMAT = 4;
    public static final int PSP_SYSTEMPARAM_ID_INT_TIME_FORMAT = 5;
    public static final int PSP_SYSTEMPARAM_ID_INT_TIMEZONE = 6;
    public static final int PSP_SYSTEMPARAM_ID_INT_DAYLIGHTSAVINGS = 7;
    public static final int PSP_SYSTEMPARAM_ID_INT_LANGUAGE = 8;
    public static final int PSP_SYSTEMPARAM_ID_INT_UNKNOWN = 9;

    public static final int PSP_SYSTEMPARAM_LANGUAGE_ENGLISH = 1;
    public static final int PSP_SYSTEMPARAM_LANGUAGE_FRENCH = 2;
    public static final int PSP_SYSTEMPARAM_LANGUAGE_SPANISH = 3;
    public static final int PSP_SYSTEMPARAM_LANGUAGE_GERMAN = 4;
    public static final int PSP_SYSTEMPARAM_LANGUAGE_ITALIAN = 5;
    public static final int PSP_SYSTEMPARAM_LANGUAGE_DUTCH = 6;
    public static final int PSP_SYSTEMPARAM_LANGUAGE_PORTUGUESE = 7;
    public static final int PSP_SYSTEMPARAM_LANGUAGE_RUSSIAN = 8;
    public static final int PSP_SYSTEMPARAM_LANGUAGE_KOREAN = 9;
    public static final int PSP_SYSTEMPARAM_LANGUAGE_CHINESE_TRADITIONAL = 10;
    public static final int PSP_SYSTEMPARAM_LANGUAGE_CHINESE_SIMPLIFIED = 11;

    public static final int PSP_SYSTEMPARAM_DATE_FORMAT_YYYYMMDD = 0;
    public static final int PSP_SYSTEMPARAM_DATE_FORMAT_MMDDYYYY = 1;
    public static final int PSP_SYSTEMPARAM_DATE_FORMAT_DDMMYYYY = 2;

    public static final int PSP_SYSTEMPARAM_TIME_FORMAT_24HR = 0;
    public static final int PSP_SYSTEMPARAM_TIME_FORMAT_12HR = 1;

    public static final int PSP_UTILITY_ERROR_NOT_INITED = 0x80110005; // might not be correct name

    public static final int PSP_NETPARAM_ERROR_BAD_NETCONF = 0x80110601;
    public static final int PSP_NETPARAM_ERROR_BAD_PARAM = 0x80110604;

    public static final int PSP_UTILITY_DIALOG_NONE = 0;
    public static final int PSP_UTILITY_DIALOG_INIT = 1;
    public static final int PSP_UTILITY_DIALOG_VISIBLE = 2;
    public static final int PSP_UTILITY_DIALOG_QUIT = 3;
    public static final int PSP_UTILITY_DIALOG_FINISHED = 4;


    protected int gamesharing_status;
    protected int netplaydialog_status;
    protected int netconf_status;

    protected int savedata_status;
    protected int savedata_mode; //hacky should be done better

    protected int msgdialog_status;
    protected SceUtilityMsgDialogParams msgdialog_params;

    protected int osk_status;

    // TODO expose via settings GUI
    protected String systemParam_nickname = "JPCSP";
    protected int systemParam_dateFormat = PSP_SYSTEMPARAM_DATE_FORMAT_YYYYMMDD;
    protected int systemParam_timeFormat = PSP_SYSTEMPARAM_TIME_FORMAT_24HR;
    protected int systemParam_timeZone = 0; // TODO probably minutes west or east of UTC
    protected int systemParam_language = PSP_SYSTEMPARAM_LANGUAGE_ENGLISH;
    protected int systemParam_buttonPreference = 0;

	public void sceUtilityGameSharingInitStart(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtilityGameSharingInitStart [0xC492F751]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceUtilityGameSharingShutdownStart(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtilityGameSharingShutdownStart [0xEFC6F80F]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceUtilityGameSharingUpdate(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtilityGameSharingUpdate [0x7853182D]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceUtilityGameSharingGetStatus(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtilityGameSharingGetStatus [0x946963F3]");

		cpu.gpr[2] = gamesharing_status;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceNetplayDialogInitStart(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceNetplayDialogInitStart [0x3AD50AE7]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceNetplayDialogShutdownStart(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceNetplayDialogShutdownStart [0xBC6B6296]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceNetplayDialogUpdate(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceNetplayDialogUpdate [0x417BED54]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceNetplayDialogGetStatus(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceNetplayDialogGetStatus [0xB6CEE597]");

		cpu.gpr[2] = netplaydialog_status;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceUtilityNetconfInitStart(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtilityNetconfInitStart [0x4DB1E739]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceUtilityNetconfShutdownStart(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtilityNetconfShutdownStart [0xF88155F6]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceUtilityNetconfUpdate(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtilityNetconfUpdate [0x91E70E35]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceUtilityNetconfGetStatus(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtilityNetconfGetStatus [0x6332AA39]");

		cpu.gpr[2] = netconf_status;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	private int computeMemoryStickRequiredSpaceKb(int sizeByte) {
	    int sizeKb = (sizeByte + 1023) / 1024;
	    int sectorSizeKb = MemoryStick.getSectorSizeKb();
	    int numberSectors = (sizeKb + sectorSizeKb - 1) / sectorSizeKb;

	    return numberSectors * sectorSizeKb;
	}

	public void sceUtilitySavedataInitStart(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        Memory mem = Processor.memory;

        int savedataParamAddr = cpu.gpr[4];

        SceUtilitySavedataParam sceUtilitySavedataParam = new SceUtilitySavedataParam();
        sceUtilitySavedataParam.read(mem, savedataParamAddr);

        Modules.log.debug("PARTIAL:sceUtilitySavedataInitStart savedataParamAddr=0x" + Integer.toHexString(savedataParamAddr) +
            ", gameName=" + sceUtilitySavedataParam.gameName +
            ", saveName=" + sceUtilitySavedataParam.saveName +
            ", fileName=" + sceUtilitySavedataParam.fileName +
            ", mode=" + sceUtilitySavedataParam.mode);

        savedata_mode = sceUtilitySavedataParam.mode;

        // HACK let's start on quit, then the app should call shutdown and then we change status to finished
        savedata_status = PSP_UTILITY_DIALOG_QUIT;

        // TODO HACK to enable game loading in ToE. To be removed when dialog for MODE_LISTLOAD and MODE_LISTSAVE is implemented.
        if (savedata_mode == SceUtilitySavedataParam.MODE_LISTLOAD) {
            Modules.log.warn("sceUtilitySavedataInitStart MODE_LISTLOAD converted to MODE_AUTOLOAD");
            savedata_mode = SceUtilitySavedataParam.MODE_AUTOLOAD;
        } else if (savedata_mode == SceUtilitySavedataParam.MODE_LISTSAVE) {
            Modules.log.warn("sceUtilitySavedataInitStart MODE_LISTSAVE converted to MODE_AUTOSAVE");
            savedata_mode = SceUtilitySavedataParam.MODE_AUTOSAVE;
        }

        switch (savedata_mode) {
            case SceUtilitySavedataParam.MODE_AUTOLOAD:
            case SceUtilitySavedataParam.MODE_LOAD:
                if (sceUtilitySavedataParam.saveName == null || sceUtilitySavedataParam.saveName.length() == 0) {
                    if (sceUtilitySavedataParam.saveNameList != null && sceUtilitySavedataParam.saveNameList.length > 0) {
                        sceUtilitySavedataParam.saveName = sceUtilitySavedataParam.saveNameList[0];
                    } else {
                        sceUtilitySavedataParam.saveName = "-000";
                    }
                }

                try {
                    sceUtilitySavedataParam.load(mem, pspiofilemgr.getInstance());
                    sceUtilitySavedataParam.base.result = 0;
                    sceUtilitySavedataParam.write(mem);
                } catch (IOException e) {
                    sceUtilitySavedataParam.base.result = ERROR_SAVEDATA_LOAD_NO_DATA;
                } catch (Exception e) {
                    sceUtilitySavedataParam.base.result = ERROR_SAVEDATA_LOAD_NO_DATA;
                    e.printStackTrace();
                }
                break;

            case SceUtilitySavedataParam.MODE_LISTLOAD:
                // TODO Implement dialog to display list of available SAVEDATA files
                Modules.log.warn("sceUtilitySavedataInitStart MODE_LISTLOAD => result=ERROR_SAVEDATA_LOAD_NO_DATA");
                sceUtilitySavedataParam.base.result = ERROR_SAVEDATA_LOAD_NO_DATA;
                break;

            case SceUtilitySavedataParam.MODE_AUTOSAVE:
            case SceUtilitySavedataParam.MODE_SAVE:
                try {
                    if (sceUtilitySavedataParam.saveName == null || sceUtilitySavedataParam.saveName.length() == 0) {
                        if (sceUtilitySavedataParam.saveNameList != null && sceUtilitySavedataParam.saveNameList.length > 0) {
                            sceUtilitySavedataParam.saveName = sceUtilitySavedataParam.saveNameList[0];
                        } else {
                            sceUtilitySavedataParam.saveName = "-000";
                        }
                    }

                    sceUtilitySavedataParam.save(mem, pspiofilemgr.getInstance());
                    sceUtilitySavedataParam.base.result = 0;
                } catch (IOException e) {
                    sceUtilitySavedataParam.base.result = ERROR_SAVEDATA_SAVE_ACCESS_ERROR;
                } catch (Exception e) {
                    sceUtilitySavedataParam.base.result = ERROR_SAVEDATA_SAVE_ACCESS_ERROR;
                    e.printStackTrace();
                }
                break;

            case SceUtilitySavedataParam.MODE_LISTSAVE:
                // TODO Implement dialog to display list of available SAVEDATA files
                Modules.log.warn("sceUtilitySavedataInitStart MODE_LISTSAVE => result=ERROR_SAVEDATA_SAVE_NO_MS");
                sceUtilitySavedataParam.base.result = ERROR_SAVEDATA_SAVE_NO_MEMSTICK;
                break;

            case SceUtilitySavedataParam.MODE_TRYSAVE: {
                Modules.log.warn("PARTIAL:sceUtilitySavedataInitStart mode 8");
                if (sceUtilitySavedataParam.isPresent(pspiofilemgr.getInstance())) {
                    sceUtilitySavedataParam.base.result = 0;
                } else {
                    sceUtilitySavedataParam.base.result = ERROR_SAVEDATA_MODE8_NO_DATA;
                }
                int buffer1Addr = sceUtilitySavedataParam.buffer1Addr;
                if (mem.isAddressGood(buffer1Addr)) {
                    String memoryStickFreeSpaceString = MemoryStick.getSizeKbString(MemoryStick.getFreeSizeKb());

                    mem.write32(buffer1Addr +  0, MemoryStick.getSectorSize());
                    mem.write32(buffer1Addr +  4, MemoryStick.getFreeSizeKb() / MemoryStick.getSectorSizeKb());
                    mem.write32(buffer1Addr +  8, MemoryStick.getFreeSizeKb());
                    Utilities.writeStringNZ(mem, buffer1Addr +  12, 8, memoryStickFreeSpaceString);

                    Modules.log.debug("Memory Stick Free Space = " + memoryStickFreeSpaceString);
                }
                int buffer3Addr = sceUtilitySavedataParam.buffer3Addr;
                if (mem.isAddressGood(buffer3Addr)) {
                    int memoryStickRequiredSpaceKb = 0;
                    memoryStickRequiredSpaceKb += MemoryStick.getSectorSizeKb(); // Assume 1 sector for SFO-Params
                    memoryStickRequiredSpaceKb += computeMemoryStickRequiredSpaceKb(sceUtilitySavedataParam.dataSize);
                    memoryStickRequiredSpaceKb += computeMemoryStickRequiredSpaceKb(sceUtilitySavedataParam.icon0FileData.size);
                    memoryStickRequiredSpaceKb += computeMemoryStickRequiredSpaceKb(sceUtilitySavedataParam.icon1FileData.size);
                    memoryStickRequiredSpaceKb += computeMemoryStickRequiredSpaceKb(sceUtilitySavedataParam.pic1FileData.size);
                    memoryStickRequiredSpaceKb += computeMemoryStickRequiredSpaceKb(sceUtilitySavedataParam.snd0FileData.size);
                    String memoryStickRequiredSpaceString = MemoryStick.getSizeKbString(memoryStickRequiredSpaceKb);

                    mem.write32(buffer3Addr +  0, memoryStickRequiredSpaceKb / MemoryStick.getSectorSizeKb());
                    mem.write32(buffer3Addr +  4, memoryStickRequiredSpaceKb);
                    Utilities.writeStringNZ(mem, buffer3Addr +  8, 8, memoryStickRequiredSpaceString);
                    mem.write32(buffer3Addr + 16, memoryStickRequiredSpaceKb);
                    Utilities.writeStringNZ(mem, buffer3Addr + 20, 8, memoryStickRequiredSpaceString);

                    Modules.log.debug("Memory Stick Required Space = " + memoryStickRequiredSpaceString);
                }
                break;
            }

            case SceUtilitySavedataParam.MODE_LIST: {
                Modules.log.debug("sceUtilitySavedataInitStart mode 11");
                int buffer4Addr = sceUtilitySavedataParam.buffer4Addr;
                if (mem.isAddressGood(buffer4Addr)) {
                	int maxEntries = mem.read32(buffer4Addr + 0);
                	int entriesAddr = mem.read32(buffer4Addr + 8);
                	String saveName = sceUtilitySavedataParam.saveName;
                	// PSP file name pattern:
                	//   '?' matches one character
                	//   '*' matches any character sequence
                	// To convert to regular expressions:
                	//   replace '?' with '.'
                	//   replace '*' with '.*'
                	String pattern = saveName.replace('?', '.');
                	pattern = pattern.replace("*", ".*");
                	pattern = sceUtilitySavedataParam.gameName + pattern;

                	pspiofilemgr fileManager = pspiofilemgr.getInstance();
                	String[] entries = fileManager.listFiles(SceUtilitySavedataParam.savedataPath, pattern);
                	Modules.log.debug("Entries: " + entries);
                	int numEntries = entries == null ? 0 : entries.length;
                	numEntries = Math.min(numEntries, maxEntries);
                	for (int i = 0; i < numEntries; i++) {
                		String filePath = SceUtilitySavedataParam.savedataPath + "/" + entries[i];
                		SceIoStat stat = fileManager.statFile(filePath);
                		int entryAddr = entriesAddr + i * 72;
                		if (stat != null) {
                			mem.write32(entryAddr + 0, stat.mode);
                			stat.ctime.write(mem, entryAddr + 4);
                			stat.atime.write(mem, entryAddr + 20);
                			stat.mtime.write(mem, entryAddr + 36);
                		}
                		String entryName = entries[i].substring(sceUtilitySavedataParam.gameName.length());
                		Utilities.writeStringNZ(mem, entryAddr + 52, 20, entryName);
                	}
                	mem.write32(buffer4Addr + 4, numEntries);
                }
        		sceUtilitySavedataParam.base.result = 0;
                break;
            }

            case SceUtilitySavedataParam.MODE_TEST: {
            	boolean isPresent = false;

            	try {
                    isPresent = sceUtilitySavedataParam.test(mem, pspiofilemgr.getInstance());
                } catch (FileNotFoundException e) {
                	isPresent = false;
                } catch (Exception e) {
                }

                if (isPresent) {
                	sceUtilitySavedataParam.base.result = ERROR_SAVEDATA_MODE15_SAVEDATA_PRESENT;
                } else {
                    sceUtilitySavedataParam.base.result = ERROR_SAVEDATA_MODE15_SAVEDATA_NOT_PRESENT;
                }
                break;
            }

            default:
                Modules.log.warn("sceUtilitySavedataInitStart - Unsupported mode " + savedata_mode);
                sceUtilitySavedataParam.base.result = -1;
                break;
        }

        sceUtilitySavedataParam.base.writeResult(mem, savedataParamAddr);
        Modules.log.debug("sceUtilitySavedataInitStart savedResult:0x" + Integer.toHexString(sceUtilitySavedataParam.base.result));

        cpu.gpr[2] = 0;
        Modules.log.debug("sceUtilitySavedataInitStart ret:0x" + Integer.toHexString(cpu.gpr[2]));
    }

    public void sceUtilitySavedataShutdownStart(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor

        Modules.log.warn("PARTIAL:sceUtilitySavedataShutdownStart");

        savedata_status = PSP_UTILITY_DIALOG_FINISHED;

        cpu.gpr[2] = 0; // return code required
    }

	public void sceUtilitySavedataUpdate(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtilitySavedataUpdate [0xD4B95FFB]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

    public void sceUtilitySavedataGetStatus(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor

        Modules.log.warn("PARTIAL:sceUtilitySavedataGetStatus return:0x" + Integer.toHexString(savedata_status));

        cpu.gpr[2] = savedata_status;

        // after returning FINISHED once, return NONE on following calls
        if (savedata_status == PSP_UTILITY_DIALOG_FINISHED)
            savedata_status = PSP_UTILITY_DIALOG_NONE;
    }

	public void sceUtility_2995D020(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtility_2995D020 [0x2995D020]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceUtility_B62A4061(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtility_B62A4061 [0xB62A4061]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceUtility_ED0FAD38(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtility_ED0FAD38 [0xED0FAD38]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceUtility_88BC7406(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtility_88BC7406 [0x88BC7406]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

    public void sceUtilityMsgDialogInitStart(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int params_addr = cpu.gpr[4];
        msgdialog_params = new SceUtilityMsgDialogParams();
        if (mem.isAddressGood(params_addr)) {
            msgdialog_params.read(mem, params_addr);

            Modules.log.warn("PARTIAL:sceUtilityMsgDialogInitStart message='" + msgdialog_params.message + "'");
            Modules.log.debug(msgdialog_params.toString());

            if (msgdialog_params.options == SceUtilityMsgDialogParams.PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_NO ||
            	msgdialog_params.options == SceUtilityMsgDialogParams.PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_YES) {
                int result = JOptionPane.showConfirmDialog(null, msgdialog_params.message, null, JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                	msgdialog_params.buttonPressed = 1;
                } else if (result == JOptionPane.NO_OPTION) {
                	msgdialog_params.buttonPressed = 2;
                } else if (result == JOptionPane.CANCEL_OPTION) {
                	msgdialog_params.buttonPressed = 3;
                }
            } else if (msgdialog_params.mode == SceUtilityMsgDialogParams.PSP_UTILITY_MSGDIALOG_MODE_TEXT) {
            	JOptionPane.showMessageDialog(null, msgdialog_params.message);
            }
            msgdialog_params.base.result = 0;
            msgdialog_params.write(mem);

            // HACK let's start on quit, then the app should call shutdown and then we change status to finished
            msgdialog_status = PSP_UTILITY_DIALOG_QUIT;

            cpu.gpr[2] = 0;
        } else {
            Modules.log.error("sceUtilityMsgDialogInitStart bad address " + String.format("0x%08X", msgdialog_params));
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_READ);
            cpu.gpr[2] = -1;
        }
    }

    public void sceUtilityMsgDialogShutdownStart(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("PARTIAL:sceUtilityMsgDialogShutdownStart");

        msgdialog_status = PSP_UTILITY_DIALOG_FINISHED;

        cpu.gpr[2] = 0;
    }

	public void sceUtilityMsgDialogUpdate(Processor processor) {
		CpuState cpu = processor.cpu;

		int unk1 = cpu.gpr[4];

        Modules.log.warn("UNIMPLEMENTED:sceUtilityMsgDialogUpdate(unk1=" + unk1 + ")");

        cpu.gpr[2] = 0;
	}

    public void sceUtilityMsgDialogGetStatus(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor

        int returnStatus = msgdialog_status;

        Modules.log.warn("PARTIAL:sceUtilityMsgDialogGetStatus return:" + Integer.toHexString(returnStatus));

        if (msgdialog_status == PSP_UTILITY_DIALOG_INIT) {
            msgdialog_status = PSP_UTILITY_DIALOG_VISIBLE;
        }

        // after returning FINISHED once, return NONE on following calls
        else if (msgdialog_status == PSP_UTILITY_DIALOG_FINISHED) {
            msgdialog_status = PSP_UTILITY_DIALOG_NONE;
        }

        cpu.gpr[2] = returnStatus;
    }

	public void sceUtilityOskInitStart(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

        int oskParamAddr = cpu.gpr[4];

        SceUtilityOskParams sceUtilityOskParams = new SceUtilityOskParams();
        sceUtilityOskParams.read(mem, oskParamAddr);

        Modules.log.warn("PARTIAL:sceUtilityOskInitStart oskParamAddr=0x" + Integer.toHexString(oskParamAddr) +
            ", desc=" + sceUtilityOskParams.oskData.desc +
            ", inText=" + sceUtilityOskParams.oskData.inText +
            ", outText=" + sceUtilityOskParams.oskData.outText);

        osk_status = PSP_UTILITY_DIALOG_QUIT;

        sceUtilityOskParams.oskData.outText = JOptionPane.showInputDialog(sceUtilityOskParams.oskData.desc, sceUtilityOskParams.oskData.inText);
        sceUtilityOskParams.base.result = 0;
        sceUtilityOskParams.oskData.result = 2;	// Unknown value, but required by "SEGA Rally"
        sceUtilityOskParams.write(mem, oskParamAddr);
        Modules.log.info("sceUtilityOskInitStart returning '" + sceUtilityOskParams.oskData.outText + "'");

        cpu.gpr[2] = 0;
	}

	public void sceUtilityOskShutdownStart(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("PARTIAL:sceUtilityOskShutdownStart");

        osk_status = PSP_UTILITY_DIALOG_FINISHED;

		cpu.gpr[2] = osk_status;
	}

	public void sceUtilityOskUpdate(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtilityOskUpdate [0x4B85C861]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceUtilityOskGetStatus(Processor processor) {
		CpuState cpu = processor.cpu;

        Modules.log.warn("PARTIAL:sceUtilityOskGetStatus return:0x" + Integer.toHexString(osk_status));

		cpu.gpr[2] = osk_status;
        if (osk_status == PSP_UTILITY_DIALOG_FINISHED)
            osk_status = PSP_UTILITY_DIALOG_NONE;
	}

	public void sceUtilitySetSystemParamInt(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtilitySetSystemParamInt [0x45C18506]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceUtilitySetSystemParamString(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtilitySetSystemParamString [0x41E30674]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public void sceUtilityGetSystemParamInt(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

        int id = cpu.gpr[4];
        int value_addr = cpu.gpr[5];

        if (!mem.isAddressGood(value_addr)) {
            Modules.log.warn("sceUtilityGetSystemParamInt(id=" + id + ",value=0x" + Integer.toHexString(value_addr) + ") bad address");
            cpu.gpr[2] = -1;
        } else {
            Modules.log.debug("PARTIAL:sceUtilityGetSystemParamInt(id=" + id + ",value=0x" + Integer.toHexString(value_addr) + ")");

            cpu.gpr[2] = 0;
            switch(id) {
                case PSP_SYSTEMPARAM_ID_INT_DATE_FORMAT:
                    mem.write32(value_addr, systemParam_dateFormat);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_TIME_FORMAT:
                    mem.write32(value_addr, systemParam_timeFormat);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_TIMEZONE:
                    mem.write32(value_addr, systemParam_timeZone);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_LANGUAGE:
                    mem.write32(value_addr, systemParam_language);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_UNKNOWN:
                    mem.write32(value_addr, systemParam_buttonPreference);
                    break;

                default:
                    Modules.log.warn("UNIMPLEMENTED:sceUtilityGetSystemParamInt(id=" + id + ",value=0x" + Integer.toHexString(value_addr) + ") unhandled id");
                    cpu.gpr[2] = -1;
                    break;
            }
        }
	}

	public void sceUtilityGetSystemParamString(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

        int id = cpu.gpr[4];
        int str_addr = cpu.gpr[5];
        int len = cpu.gpr[6];

        if (!mem.isAddressGood(str_addr)) {
            Modules.log.warn("sceUtilityGetSystemParamString(id=" + id + ",str=0x" + Integer.toHexString(str_addr) + ",len=" + len + ") bad address");
            cpu.gpr[2] = -1;
        } else {
            Modules.log.debug("PARTIAL:sceUtilityGetSystemParamString(id=" + id + ",str=0x" + Integer.toHexString(str_addr) + ",len=" + len + ")");

            cpu.gpr[2] = 0;
            switch(id) {
                case PSP_SYSTEMPARAM_ID_STRING_NICKNAME:
                    Utilities.writeStringNZ(mem, str_addr, len, systemParam_nickname);
                    break;

                default:
                    Modules.log.warn("UNIMPLEMENTED:sceUtilityGetSystemParamString(id=" + id + ",str=0x" + Integer.toHexString(str_addr) + ",len=" + len + ") unhandled id");
                    cpu.gpr[2] = -1;
                    break;
            }
        }
	}

	public void sceUtilityCheckNetParam(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor

		int id = cpu.gpr[4];

		Modules.log.warn("UNIMPLEMENTED:sceUtilityCheckNetParam(id=" + id + ")");

		cpu.gpr[2] = PSP_NETPARAM_ERROR_BAD_PARAM;
	}

	public void sceUtilityGetNetParam(Processor processor) {
		CpuState cpu = processor.cpu; // New-Style Processor
		// Processor cpu = processor; // Old-Style Processor
		Memory mem = Processor.memory;

		/* put your own code here instead */

		// int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
		// float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

		Modules.log.warn("Unimplemented NID function sceUtilityGetNetParam [0x434D4B3A]");

		cpu.gpr[2] = 0xDEADC0DE;

		// cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
	}

	public final HLEModuleFunction sceUtilityGameSharingInitStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityGameSharingInitStart") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityGameSharingInitStart(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityGameSharingInitStart(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityGameSharingShutdownStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityGameSharingShutdownStart") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityGameSharingShutdownStart(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityGameSharingShutdownStart(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityGameSharingUpdateFunction = new HLEModuleFunction("sceUtility", "sceUtilityGameSharingUpdate") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityGameSharingUpdate(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityGameSharingUpdate(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityGameSharingGetStatusFunction = new HLEModuleFunction("sceUtility", "sceUtilityGameSharingGetStatus") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityGameSharingGetStatus(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityGameSharingGetStatus(processor);";
		}
	};

	public final HLEModuleFunction sceNetplayDialogInitStartFunction = new HLEModuleFunction("sceUtility", "sceNetplayDialogInitStart") {
		@Override
		public final void execute(Processor processor) {
			sceNetplayDialogInitStart(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceNetplayDialogInitStart(processor);";
		}
	};

	public final HLEModuleFunction sceNetplayDialogShutdownStartFunction = new HLEModuleFunction("sceUtility", "sceNetplayDialogShutdownStart") {
		@Override
		public final void execute(Processor processor) {
			sceNetplayDialogShutdownStart(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceNetplayDialogShutdownStart(processor);";
		}
	};

	public final HLEModuleFunction sceNetplayDialogUpdateFunction = new HLEModuleFunction("sceUtility", "sceNetplayDialogUpdate") {
		@Override
		public final void execute(Processor processor) {
			sceNetplayDialogUpdate(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceNetplayDialogUpdate(processor);";
		}
	};

	public final HLEModuleFunction sceNetplayDialogGetStatusFunction = new HLEModuleFunction("sceUtility", "sceNetplayDialogGetStatus") {
		@Override
		public final void execute(Processor processor) {
			sceNetplayDialogGetStatus(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceNetplayDialogGetStatus(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityNetconfInitStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityNetconfInitStart") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityNetconfInitStart(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityNetconfInitStart(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityNetconfShutdownStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityNetconfShutdownStart") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityNetconfShutdownStart(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityNetconfShutdownStart(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityNetconfUpdateFunction = new HLEModuleFunction("sceUtility", "sceUtilityNetconfUpdate") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityNetconfUpdate(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityNetconfUpdate(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityNetconfGetStatusFunction = new HLEModuleFunction("sceUtility", "sceUtilityNetconfGetStatus") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityNetconfGetStatus(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityNetconfGetStatus(processor);";
		}
	};

	public final HLEModuleFunction sceUtilitySavedataInitStartFunction = new HLEModuleFunction("sceUtility", "sceUtilitySavedataInitStart") {
		@Override
		public final void execute(Processor processor) {
			sceUtilitySavedataInitStart(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilitySavedataInitStart(processor);";
		}
	};

	public final HLEModuleFunction sceUtilitySavedataShutdownStartFunction = new HLEModuleFunction("sceUtility", "sceUtilitySavedataShutdownStart") {
		@Override
		public final void execute(Processor processor) {
			sceUtilitySavedataShutdownStart(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilitySavedataShutdownStart(processor);";
		}
	};

	public final HLEModuleFunction sceUtilitySavedataUpdateFunction = new HLEModuleFunction("sceUtility", "sceUtilitySavedataUpdate") {
		@Override
		public final void execute(Processor processor) {
			sceUtilitySavedataUpdate(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilitySavedataUpdate(processor);";
		}
	};

	public final HLEModuleFunction sceUtilitySavedataGetStatusFunction = new HLEModuleFunction("sceUtility", "sceUtilitySavedataGetStatus") {
		@Override
		public final void execute(Processor processor) {
			sceUtilitySavedataGetStatus(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilitySavedataGetStatus(processor);";
		}
	};

	public final HLEModuleFunction sceUtility_2995D020Function = new HLEModuleFunction("sceUtility", "sceUtility_2995D020") {
		@Override
		public final void execute(Processor processor) {
			sceUtility_2995D020(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtility_2995D020(processor);";
		}
	};

	public final HLEModuleFunction sceUtility_B62A4061Function = new HLEModuleFunction("sceUtility", "sceUtility_B62A4061") {
		@Override
		public final void execute(Processor processor) {
			sceUtility_B62A4061(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtility_B62A4061(processor);";
		}
	};

	public final HLEModuleFunction sceUtility_ED0FAD38Function = new HLEModuleFunction("sceUtility", "sceUtility_ED0FAD38") {
		@Override
		public final void execute(Processor processor) {
			sceUtility_ED0FAD38(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtility_ED0FAD38(processor);";
		}
	};

	public final HLEModuleFunction sceUtility_88BC7406Function = new HLEModuleFunction("sceUtility", "sceUtility_88BC7406") {
		@Override
		public final void execute(Processor processor) {
			sceUtility_88BC7406(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtility_88BC7406(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityMsgDialogInitStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityMsgDialogInitStart") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityMsgDialogInitStart(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityMsgDialogInitStart(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityMsgDialogShutdownStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityMsgDialogShutdownStart") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityMsgDialogShutdownStart(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityMsgDialogShutdownStart(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityMsgDialogUpdateFunction = new HLEModuleFunction("sceUtility", "sceUtilityMsgDialogUpdate") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityMsgDialogUpdate(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityMsgDialogUpdate(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityMsgDialogGetStatusFunction = new HLEModuleFunction("sceUtility", "sceUtilityMsgDialogGetStatus") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityMsgDialogGetStatus(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityMsgDialogGetStatus(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityOskInitStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityOskInitStart") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityOskInitStart(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityOskInitStart(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityOskShutdownStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityOskShutdownStart") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityOskShutdownStart(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityOskShutdownStart(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityOskUpdateFunction = new HLEModuleFunction("sceUtility", "sceUtilityOskUpdate") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityOskUpdate(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityOskUpdate(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityOskGetStatusFunction = new HLEModuleFunction("sceUtility", "sceUtilityOskGetStatus") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityOskGetStatus(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityOskGetStatus(processor);";
		}
	};

	public final HLEModuleFunction sceUtilitySetSystemParamIntFunction = new HLEModuleFunction("sceUtility", "sceUtilitySetSystemParamInt") {
		@Override
		public final void execute(Processor processor) {
			sceUtilitySetSystemParamInt(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilitySetSystemParamInt(processor);";
		}
	};

	public final HLEModuleFunction sceUtilitySetSystemParamStringFunction = new HLEModuleFunction("sceUtility", "sceUtilitySetSystemParamString") {
		@Override
		public final void execute(Processor processor) {
			sceUtilitySetSystemParamString(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilitySetSystemParamString(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityGetSystemParamIntFunction = new HLEModuleFunction("sceUtility", "sceUtilityGetSystemParamInt") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityGetSystemParamInt(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityGetSystemParamInt(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityGetSystemParamStringFunction = new HLEModuleFunction("sceUtility", "sceUtilityGetSystemParamString") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityGetSystemParamString(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityGetSystemParamString(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityCheckNetParamFunction = new HLEModuleFunction("sceUtility", "sceUtilityCheckNetParam") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityCheckNetParam(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityCheckNetParam(processor);";
		}
	};

	public final HLEModuleFunction sceUtilityGetNetParamFunction = new HLEModuleFunction("sceUtility", "sceUtilityGetNetParam") {
		@Override
		public final void execute(Processor processor) {
			sceUtilityGetNetParam(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityGetNetParam(processor);";
		}
	};

};
