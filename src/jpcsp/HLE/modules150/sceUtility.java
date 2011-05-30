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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import jpcsp.Controller;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Resource;
import jpcsp.Settings;
import jpcsp.State;
import jpcsp.Allegrex.CpuState;
import jpcsp.GUI.CancelButton;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SystemTimeManager;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.HLE.kernel.types.SceUtilityGamedataInstallParams;
import jpcsp.HLE.kernel.types.SceUtilityGameSharingParams;
import jpcsp.HLE.kernel.types.SceUtilityHtmlViewerParams;
import jpcsp.HLE.kernel.types.SceUtilityMsgDialogParams;
import jpcsp.HLE.kernel.types.SceUtilityNetconfParams;
import jpcsp.HLE.kernel.types.SceUtilityNpSigninParams;
import jpcsp.HLE.kernel.types.SceUtilityOskParams;
import jpcsp.HLE.kernel.types.SceUtilitySavedataParam;
import jpcsp.HLE.kernel.types.SceUtilityScreenshotParams;
import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure;
import jpcsp.HLE.kernel.types.SceUtilityOskParams.SceUtilityOskData;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.HLE.modules.sceCtrl;
import jpcsp.HLE.modules.sceNetApctl;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.format.PSF;
import jpcsp.hardware.MemoryStick;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceUtility implements HLEModule, HLEStartModule {

    protected static Logger log = Modules.getLogger("sceUtility");

    @Override
    public String getName() {
        return "sceUtility";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {
            mm.addFunction(0xC492F751, sceUtilityGameSharingInitStartFunction);
            mm.addFunction(0xEFC6F80F, sceUtilityGameSharingShutdownStartFunction);
            mm.addFunction(0x7853182D, sceUtilityGameSharingUpdateFunction);
            mm.addFunction(0x946963F3, sceUtilityGameSharingGetStatusFunction);
            mm.addFunction(0x3AD50AE7, sceNetplayDialogInitStartFunction);
            mm.addFunction(0xBC6B6296, sceNetplayDialogShutdownStartFunction);
            mm.addFunction(0x417BED54, sceNetplayDialogUpdateFunction);
            mm.addFunction(0xB6CEE597, sceNetplayDialogGetStatusFunction);
            mm.addFunction(0x4DB1E739, sceUtilityNetconfInitStartFunction);
            mm.addFunction(0xF88155F6, sceUtilityNetconfShutdownStartFunction);
            mm.addFunction(0x91E70E35, sceUtilityNetconfUpdateFunction);
            mm.addFunction(0x6332AA39, sceUtilityNetconfGetStatusFunction);
            mm.addFunction(0x50C4CD57, sceUtilitySavedataInitStartFunction);
            mm.addFunction(0x9790B33C, sceUtilitySavedataShutdownStartFunction);
            mm.addFunction(0xD4B95FFB, sceUtilitySavedataUpdateFunction);
            mm.addFunction(0x8874DBE0, sceUtilitySavedataGetStatusFunction);
            mm.addFunction(0x2995D020, sceUtilitySavedataErrInitStartFunction);
            mm.addFunction(0xB62A4061, sceUtilitySavedataErrShutdownStartFunction);
            mm.addFunction(0xED0FAD38, sceUtilitySavedataErrUpdateFunction);
            mm.addFunction(0x88BC7406, sceUtilitySavedataErrGetStatusFunction);
            mm.addFunction(0x2AD8E239, sceUtilityMsgDialogInitStartFunction);
            mm.addFunction(0x67AF3428, sceUtilityMsgDialogShutdownStartFunction);
            mm.addFunction(0x95FC253B, sceUtilityMsgDialogUpdateFunction);
            mm.addFunction(0x9A1C91D7, sceUtilityMsgDialogGetStatusFunction);
            mm.addFunction(0xF6269B82, sceUtilityOskInitStartFunction);
            mm.addFunction(0x3DFAEBA9, sceUtilityOskShutdownStartFunction);
            mm.addFunction(0x4B85C861, sceUtilityOskUpdateFunction);
            mm.addFunction(0xF3F76017, sceUtilityOskGetStatusFunction);
            mm.addFunction(0x45C18506, sceUtilitySetSystemParamIntFunction);
            mm.addFunction(0x41E30674, sceUtilitySetSystemParamStringFunction);
            mm.addFunction(0xA5DA2406, sceUtilityGetSystemParamIntFunction);
            mm.addFunction(0x34B78343, sceUtilityGetSystemParamStringFunction);
            mm.addFunction(0x5EEE6548, sceUtilityCheckNetParamFunction);
            mm.addFunction(0x434D4B3A, sceUtilityGetNetParamFunction);
            mm.addFunction(0x4FED24D8, sceUtilityGetNetParamLatestIDFunction);
            mm.addFunction(0x16D02AF0, sceUtilityNpSigninInitStartFunction);
            mm.addFunction(0xE19C97D6, sceUtilityNpSigninShutdownStartFunction);
            mm.addFunction(0xF3FBC572, sceUtilityNpSigninUpdateFunction);
            mm.addFunction(0x86ABDB1B, sceUtilityNpSigninGetStatusFunction);
            mm.addFunction(0x42071A83, sceUtilityPS3ScanInitStartFunction);
            mm.addFunction(0xD17A0573, sceUtilityPS3ScanShutdownStartFunction);
            mm.addFunction(0xD852CDCE, sceUtilityPS3ScanUpdateFunction);
            mm.addFunction(0x89317C8F, sceUtilityPS3ScanGetStatusFunction);
            mm.addFunction(0x81c44706, sceUtilityRssReaderInitStartFunction);
            mm.addFunction(0xB0FB7FF5, sceUtilityRssReaderContStartFunction);
            mm.addFunction(0xE7B778D8, sceUtilityRssReaderShutdownStartFunction);
            mm.addFunction(0x6F56F9CF, sceUtilityRssReaderUpdateFunction);
            mm.addFunction(0x8326AB05, sceUtilityRssReaderGetStatusFunction);
            mm.addFunction(0x4B0A8FE5, sceUtilityRssSubscriberInitStartFunction);
            mm.addFunction(0x06A48659, sceUtilityRssSubscriberShutdownStartFunction);
            mm.addFunction(0xA084E056, sceUtilityRssSubscriberUpdateFunction);
            mm.addFunction(0x2B96173B, sceUtilityRssSubscriberGetStatusFunction);
            mm.addFunction(0x0251B134, sceUtilityScreenshotInitStartFunction);
            mm.addFunction(0x86A03A27, sceUtilityScreenshotContStartFunction);
            mm.addFunction(0xF9E0008C, sceUtilityScreenshotShutdownStartFunction);
            mm.addFunction(0xAB083EA9, sceUtilityScreenshotUpdateFunction);
            mm.addFunction(0xD81957B7, sceUtilityScreenshotGetStatusFunction);
            mm.addFunction(0xCDC3AA41, sceUtilityHtmlViewerInitStartFunction);
            mm.addFunction(0xF5CE1134, sceUtilityHtmlViewerShutdownStartFunction);
            mm.addFunction(0x05AFB9E4, sceUtilityHtmlViewerUpdateFunction);
            mm.addFunction(0xBDA7D894, sceUtilityHtmlViewerGetStatusFunction);
            mm.addFunction(0x24AC31EB, sceUtilityGamedataInstallInitStartFunction);
            mm.addFunction(0x32E32DCB, sceUtilityGamedataInstallShutdownStartFunction);
            mm.addFunction(0x4AECD179, sceUtilityGamedataInstallUpdateFunction);
            mm.addFunction(0xB57E95D9, sceUtilityGamedataInstallGetStatusFunction);
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
            mm.removeFunction(sceUtilitySavedataErrInitStartFunction);
            mm.removeFunction(sceUtilitySavedataErrShutdownStartFunction);
            mm.removeFunction(sceUtilitySavedataErrUpdateFunction);
            mm.removeFunction(sceUtilitySavedataErrGetStatusFunction);
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
            mm.removeFunction(sceUtilityGetNetParamLatestIDFunction);
            mm.removeFunction(sceUtilityNpSigninInitStartFunction);
            mm.removeFunction(sceUtilityNpSigninShutdownStartFunction);
            mm.removeFunction(sceUtilityNpSigninUpdateFunction);
            mm.removeFunction(sceUtilityNpSigninGetStatusFunction);
            mm.removeFunction(sceUtilityPS3ScanInitStartFunction);
            mm.removeFunction(sceUtilityPS3ScanShutdownStartFunction);
            mm.removeFunction(sceUtilityPS3ScanUpdateFunction);
            mm.removeFunction(sceUtilityPS3ScanGetStatusFunction);
            mm.removeFunction(sceUtilityRssReaderInitStartFunction);
            mm.removeFunction(sceUtilityRssReaderContStartFunction);
            mm.removeFunction(sceUtilityRssReaderShutdownStartFunction);
            mm.removeFunction(sceUtilityRssReaderUpdateFunction);
            mm.removeFunction(sceUtilityRssReaderGetStatusFunction);
            mm.removeFunction(sceUtilityRssSubscriberInitStartFunction);
            mm.removeFunction(sceUtilityRssSubscriberShutdownStartFunction);
            mm.removeFunction(sceUtilityRssSubscriberUpdateFunction);
            mm.removeFunction(sceUtilityRssSubscriberGetStatusFunction);
            mm.removeFunction(sceUtilityScreenshotInitStartFunction);
            mm.removeFunction(sceUtilityScreenshotContStartFunction);
            mm.removeFunction(sceUtilityScreenshotShutdownStartFunction);
            mm.removeFunction(sceUtilityScreenshotUpdateFunction);
            mm.removeFunction(sceUtilityScreenshotGetStatusFunction);
            mm.removeFunction(sceUtilityHtmlViewerInitStartFunction);
            mm.removeFunction(sceUtilityHtmlViewerShutdownStartFunction);
            mm.removeFunction(sceUtilityHtmlViewerUpdateFunction);
            mm.removeFunction(sceUtilityHtmlViewerGetStatusFunction);
            mm.removeFunction(sceUtilityGamedataInstallInitStartFunction);
            mm.removeFunction(sceUtilityGamedataInstallShutdownStartFunction);
            mm.removeFunction(sceUtilityGamedataInstallUpdateFunction);
            mm.removeFunction(sceUtilityGamedataInstallGetStatusFunction);
        }
    }

    @Override
    public void start() {
        gameSharingState = new GameSharingUtilityDialogState("sceUtilityGameSharing");
        netplayDialogState = new NotImplementedUtilityDialogState("sceNetplayDialog");
        netconfState = new NetconfUtilityDialogState("sceUtilityNetconf");
        savedataState = new SavedataUtilityDialogState("sceUtilitySavedata");
        msgDialogState = new MsgDialogUtilityDialogState("sceUtilityMsgDialog");
        oskState = new OskUtilityDialogState("sceUtilityOsk");
        npSigninState = new NpSigninUtilityDialogState("sceUtilityNpSignin");
        PS3ScanState = new NotImplementedUtilityDialogState("sceUtilityPS3Scan");
        rssReaderState = new NotImplementedUtilityDialogState("sceUtilityRssReader");
        rssSubscriberState = new NotImplementedUtilityDialogState("sceUtilityRssSubscriber");
        screenshotState = new ScreenshotUtilityDialogState("sceUtilityScreenshot");
        htmlViewerState = new HtmlViewerUtilityDialogState("sceUtilityHtmlViewer");
        savedataErrState = new NotImplementedUtilityDialogState("sceUtilitySavedataErr");
        gamedataInstallState = new GamedataInstallUtilityDialogState("sceUtilityGamedataInstall");

        systemParam_nickname = Settings.getInstance().readString("emu.sysparam.nickname");
        systemParam_adhocChannel = Settings.getInstance().readInt("emu.sysparam.adhocchannel", 0);
        systemParam_wlanPowersave = Settings.getInstance().readInt("emu.sysparam.wlanpowersave", 0);
        systemParam_dateFormat = Settings.getInstance().readInt("emu.sysparam.dateformat", PSP_SYSTEMPARAM_DATE_FORMAT_YYYYMMDD);
        systemParam_timeFormat = Settings.getInstance().readInt("emu.sysparam.timeformat", PSP_SYSTEMPARAM_TIME_FORMAT_24HR);
        systemParam_timeZone = Settings.getInstance().readInt("emu.sysparam.timezone", 0);
        systemParam_daylightSavingTime = Settings.getInstance().readInt("emu.sysparam.daylightsavings", 0);
        systemParam_language = Settings.getInstance().readInt("emu.impose.language", PSP_SYSTEMPARAM_LANGUAGE_ENGLISH);
        systemParam_buttonPreference = Settings.getInstance().readInt("emu.impose.button", PSP_SYSTEMPARAM_BUTTON_CROSS);
    }

    @Override
    public void stop() {
    }

    public static final int PSP_SYSTEMPARAM_ID_STRING_NICKNAME = 1;
    public static final int PSP_SYSTEMPARAM_ID_INT_ADHOC_CHANNEL = 2;
    public static final int PSP_SYSTEMPARAM_ID_INT_WLAN_POWERSAVE = 3;
    public static final int PSP_SYSTEMPARAM_ID_INT_DATE_FORMAT = 4;
    public static final int PSP_SYSTEMPARAM_ID_INT_TIME_FORMAT = 5;
    public static final int PSP_SYSTEMPARAM_ID_INT_TIMEZONE = 6;
    public static final int PSP_SYSTEMPARAM_ID_INT_DAYLIGHTSAVINGS = 7;
    public static final int PSP_SYSTEMPARAM_ID_INT_LANGUAGE = 8;
    public static final int PSP_SYSTEMPARAM_ID_INT_BUTTON_PREFERENCE = 9;

    public static final int PSP_SYSTEMPARAM_LANGUAGE_JAPANESE = 0;
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

    public final static int PSP_SYSTEMPARAM_BUTTON_CIRCLE = 0;
    public final static int PSP_SYSTEMPARAM_BUTTON_CROSS = 1;

    public static final int PSP_UTILITY_DIALOG_STATUS_NONE = 0;
    public static final int PSP_UTILITY_DIALOG_STATUS_INIT = 1;
    public static final int PSP_UTILITY_DIALOG_STATUS_VISIBLE = 2;
    public static final int PSP_UTILITY_DIALOG_STATUS_QUIT = 3;
    public static final int PSP_UTILITY_DIALOG_STATUS_FINISHED = 4;

    public static final int PSP_UTILITY_DIALOG_RESULT_OK = 0;
    public static final int PSP_UTILITY_DIALOG_RESULT_CANCELED = 1;
    public static final int PSP_UTILITY_DIALOG_RESULT_ABORTED = 2;

    public static final int PSP_NETPARAM_NAME         =  0; // string
    public static final int PSP_NETPARAM_SSID         =  1; // string
    public static final int PSP_NETPARAM_SECURE       =  2; // int
    public static final int PSP_NETPARAM_WEPKEY       =  3; // string
    public static final int PSP_NETPARAM_IS_STATIC_IP =  4; // int
    public static final int PSP_NETPARAM_IP           =  5; // string
    public static final int PSP_NETPARAM_NETMASK      =  6; // string
    public static final int PSP_NETPARAM_ROUTE        =  7; // string
    public static final int PSP_NETPARAM_MANUAL_DNS   =  8; // int
    public static final int PSP_NETPARAM_PRIMARYDNS   =  9; // string
    public static final int PSP_NETPARAM_SECONDARYDNS = 10; // string
    public static final int PSP_NETPARAM_PROXY_USER   = 11; // string
    public static final int PSP_NETPARAM_PROXY_PASS   = 12; // string
    public static final int PSP_NETPARAM_USE_PROXY    = 13; // int
    public static final int PSP_NETPARAM_PROXY_SERVER = 14; // string
    public static final int PSP_NETPARAM_PROXY_PORT   = 15; // int
    public static final int PSP_NETPARAM_UNKNOWN1     = 16; // int
    public static final int PSP_NETPARAM_UNKNOWN2     = 17; // int

    protected static final int maxLineLengthForDialog = 80;
    protected static final int[] fontHeightSavedataList = new int[]{12, 12, 12, 12, 12, 12, 9, 8, 7, 6};

    protected GameSharingUtilityDialogState gameSharingState;
    protected UtilityDialogState netplayDialogState;
    protected NetconfUtilityDialogState netconfState;
    protected SavedataUtilityDialogState savedataState;
    protected MsgDialogUtilityDialogState msgDialogState;
    protected OskUtilityDialogState oskState;
    protected UtilityDialogState npSigninState;
    protected UtilityDialogState PS3ScanState;
    protected UtilityDialogState rssReaderState;
    protected UtilityDialogState rssSubscriberState;
    protected ScreenshotUtilityDialogState screenshotState;
    protected HtmlViewerUtilityDialogState htmlViewerState;
    protected UtilityDialogState savedataErrState;
    protected GamedataInstallUtilityDialogState gamedataInstallState;

    protected String systemParam_nickname;
    protected int systemParam_adhocChannel;
    protected int systemParam_wlanPowersave;
    protected int systemParam_dateFormat;
    protected int systemParam_timeFormat;
    protected int systemParam_timeZone;
    protected int systemParam_daylightSavingTime;
    protected int systemParam_language;
    protected int systemParam_buttonPreference;

    private static final String dummyNetParamName = "NetConf #%d";
    private static final int numberNetConfigurations = 1;

    protected abstract static class UtilityDialogState {
        protected String name;
        protected pspAbstractMemoryMappedStructure params;
        protected int paramsAddr;
        protected int status;
        protected int result;
        protected UtilityDialog dialog;
        protected int drawSpeed;

        public UtilityDialogState(String name) {
            this.name = name;
            status = PSP_UTILITY_DIALOG_STATUS_NONE;
            result = PSP_UTILITY_DIALOG_RESULT_OK;
        }

        protected void openDialog(UtilityDialog dialog) {
    		status = PSP_UTILITY_DIALOG_STATUS_VISIBLE;
        	this.dialog = dialog;
        	dialog.setVisible(true);
        }

        protected boolean isDialogOpen() {
        	return dialog != null;
        }

        protected void updateDialog() {
        	int delayMicros = 1000000 / 60;
        	if (drawSpeed > 0) {
        		delayMicros /= drawSpeed;
        	}
        	Modules.ThreadManForUserModule.hleKernelDelayThread(delayMicros, false);
        }

        protected boolean isDialogActive() {
        	return isDialogOpen() && dialog.isVisible();
        }

        protected void finishDialog() {
        	if (dialog != null) {
	        	Settings.getInstance().writeWindowPos(name, dialog.getLocation());
	            Settings.getInstance().writeWindowSize(name, dialog.getSize());
	            dialog = null;
        	}
            status = PSP_UTILITY_DIALOG_STATUS_QUIT;
        }

        public void executeInitStart(Processor processor) {
            CpuState cpu = processor.cpu;
            Memory mem = Memory.getInstance();

            paramsAddr = cpu.gpr[4];
            if (!Memory.isAddressGood(paramsAddr)) {
                log.error(String.format("%sInitStart bad address 0x%08X", name, paramsAddr));
                cpu.gpr[2] = -1;
            } else {
                this.params = createParams();

                params.read(mem, paramsAddr);

                if (log.isInfoEnabled()) {
                    log.info(String.format("%sInitStart %s", name, params.toString()));
                }

                // Start with INIT
                status = PSP_UTILITY_DIALOG_STATUS_INIT;

                cpu.gpr[2] = 0;
            }
        }

        private boolean isReadyForVisible() {
        	// Wait for all the buttons to be released
        	if (State.controller.getButtons() != 0) {
        		return false;
        	}

        	return true;
        }

        public void executeGetStatus(Processor processor) {
            CpuState cpu = processor.cpu;

            if (log.isDebugEnabled()) {
                log.debug(name + "GetStatus status " + status);
            }

            cpu.gpr[2] = status;

            // after returning FINISHED once, return NONE on following calls
            if (status == PSP_UTILITY_DIALOG_STATUS_FINISHED) {
                status = PSP_UTILITY_DIALOG_STATUS_NONE;
            } else if (status == PSP_UTILITY_DIALOG_STATUS_INIT && isReadyForVisible()) {
                // Move from INIT to VISIBLE
                status = PSP_UTILITY_DIALOG_STATUS_VISIBLE;
            }
        }

        public void executeShutdownStart(Processor processor) {
            CpuState cpu = processor.cpu;

            if (log.isDebugEnabled()) {
                log.debug(name + "ShutdownStart");
            }

            status = PSP_UTILITY_DIALOG_STATUS_FINISHED;

            cpu.gpr[2] = 0;
        }

        public void executeUpdate(Processor processor) {
            CpuState cpu = processor.cpu;

            drawSpeed = cpu.gpr[4]; // FPS used for internal animation sync (1 = 60 FPS; 2 = 30 FPS; 3 = 15 FPS).
            if (log.isDebugEnabled()) {
                log.debug(name + "Update drawSpeed=" + drawSpeed);
            }

            cpu.gpr[2] = 0;

            if (status == PSP_UTILITY_DIALOG_STATUS_INIT && isReadyForVisible()) {
                // Move from INIT to VISIBLE
                status = PSP_UTILITY_DIALOG_STATUS_VISIBLE;
            } else if (status == PSP_UTILITY_DIALOG_STATUS_VISIBLE) {
                // Some games reach sceUtilitySavedataInitStart with empty params which only
                // get filled with a subsequent call to sceUtilitySavedataUpdate (eg.: To Love-Ru).
                // This is why we have to re-read the params here.
                params.read(Memory.getInstance(), paramsAddr);

                boolean keepVisible = executeUpdateVisible(processor);

                if (status == PSP_UTILITY_DIALOG_STATUS_VISIBLE && isDialogOpen()) {
                	dialog.checkController();
                }

                if (status == PSP_UTILITY_DIALOG_STATUS_VISIBLE && !isDialogOpen() && !keepVisible) {
                    // There was no dialog or it has completed
                    status = PSP_UTILITY_DIALOG_STATUS_QUIT;
                }
            }
        }

        public void abort() {
            status = PSP_UTILITY_DIALOG_STATUS_FINISHED;
            result = PSP_UTILITY_DIALOG_RESULT_ABORTED;
        }

        public void cancel() {
            status = PSP_UTILITY_DIALOG_STATUS_FINISHED;
            result = PSP_UTILITY_DIALOG_RESULT_CANCELED;
        }

        protected abstract boolean executeUpdateVisible(Processor processor);

        protected abstract pspAbstractMemoryMappedStructure createParams();
    }

    protected static class NotImplementedUtilityDialogState extends UtilityDialogState {
        public NotImplementedUtilityDialogState(String name) {
            super(name);
        }

        @Override
        public void executeInitStart(Processor processor) {
            CpuState cpu = processor.cpu;

            log.warn(String.format("Unimplemented: %sInitStart params=0x%08X", name, cpu.gpr[4]));

            cpu.gpr[2] = SceKernelErrors.ERROR_UTILITY_IS_UNKNOWN;
        }

        @Override
        public void executeShutdownStart(Processor processor) {
            CpuState cpu = processor.cpu;

            log.warn("Unimplemented: " + name + "ShutdownStart");

            cpu.gpr[2] = SceKernelErrors.ERROR_UTILITY_IS_UNKNOWN;
        }

        @Override
        public void executeGetStatus(Processor processor) {
            CpuState cpu = processor.cpu;

            log.warn("Unimplemented: " + name + "GetStatus");

            cpu.gpr[2] = SceKernelErrors.ERROR_UTILITY_IS_UNKNOWN;
        }

		@Override
		protected boolean executeUpdateVisible(Processor processor) {
            CpuState cpu = processor.cpu;

            log.warn("Unimplemented: " + name + "Update");

            cpu.gpr[2] = SceKernelErrors.ERROR_UTILITY_IS_UNKNOWN;

            return false;
		}

		@Override
		protected pspAbstractMemoryMappedStructure createParams() {
			return null;
		}
    }

    protected static class SavedataUtilityDialogState extends UtilityDialogState {
    	protected SceUtilitySavedataParam savedataParams;
        protected volatile String saveListSelection;

    	public SavedataUtilityDialogState(String name) {
			super(name);
		}

		@Override
		protected pspAbstractMemoryMappedStructure createParams() {
			savedataParams = new SceUtilitySavedataParam();
			return savedataParams;
		}

        @Override
		protected boolean executeUpdateVisible(Processor processor) {
	        Memory mem = Processor.memory;

	        switch (savedataParams.mode) {
	            case SceUtilitySavedataParam.MODE_AUTOLOAD:
	            case SceUtilitySavedataParam.MODE_LOAD: {
	                if (savedataParams.saveName == null || savedataParams.saveName.length() == 0) {
	                    if (savedataParams.saveNameList != null && savedataParams.saveNameList.length > 0) {
	                        savedataParams.saveName = savedataParams.saveNameList[0];
	                    } else {
	                        savedataParams.saveName = "-000";
	                    }
	                }

	                try {
	                    savedataParams.load(mem);
	                    savedataParams.base.result = 0;
	                    savedataParams.write(mem);
	                } catch (IOException e) {
	                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_LOAD_NO_DATA;
	                } catch (Exception e) {
	                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_LOAD_ACCESS_ERROR;
	                    log.error(e);
	                }
	                break;
	            }

	            case SceUtilitySavedataParam.MODE_LISTLOAD: {
	                if (!isDialogOpen()) {
	                    // Search for valid saves.
	                    ArrayList<String> validNames = new ArrayList<String>();

	                    for (int i = 0; i < savedataParams.saveNameList.length; i++) {
	                        savedataParams.saveName = savedataParams.saveNameList[i];

	                        if (savedataParams.isPresent()) {
	                            validNames.add(savedataParams.saveName);
	                        }
	                    }

	                    SavedataDialog savedataDialog = new SavedataDialog(savedataParams, this, validNames.toArray(new String[validNames.size()]));
	                    openDialog(savedataDialog);
	                } else if (!isDialogActive()) {
	                	if (dialog.buttonPressed != SceUtilityMsgDialogParams.PSP_UTILITY_BUTTON_PRESSED_OK) {
	                		// Dialog cancelled
	                		savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_LOAD_BAD_PARAMS;
	                	} else if (saveListSelection == null) {
		                    log.warn("Savedata MODE_LISTLOAD no save selected");
		                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_LOAD_BAD_PARAMS;
		                } else {
		                    savedataParams.saveName = saveListSelection;
		                    try {
		                        savedataParams.load(mem);
		                        savedataParams.base.result = 0;
		                        savedataParams.write(mem);
		                    } catch (IOException e) {
		                        savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_LOAD_NO_DATA;
		                    } catch (Exception e) {
		                        savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_LOAD_ACCESS_ERROR;
		                        log.error(e);
		                    }
		                }
		                finishDialog();
	                } else {
	                	updateDialog();
	                }
	                break;
	            }

	            case SceUtilitySavedataParam.MODE_AUTOSAVE:
	            case SceUtilitySavedataParam.MODE_SAVE: {
	                if (savedataParams.saveName == null || savedataParams.saveName.length() == 0) {
	                    if (savedataParams.saveNameList != null && savedataParams.saveNameList.length > 0) {
	                        savedataParams.saveName = savedataParams.saveNameList[0];
	                    } else {
	                        savedataParams.saveName = "-000";
	                    }
	                }

	                try {
	                    savedataParams.save(mem);
	                    savedataParams.base.result = 0;
	                } catch (IOException e) {
	                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_SAVE_ACCESS_ERROR;
	                } catch (Exception e) {
	                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_SAVE_ACCESS_ERROR;
                        log.error(e);
	                }
	                break;
	            }

	            case SceUtilitySavedataParam.MODE_LISTSAVE: {
	            	if (!isDialogOpen()) {
	            		SavedataDialog savedataDialog = new SavedataDialog(savedataParams, this, savedataParams.saveNameList);
	            		openDialog(savedataDialog);
	            	} else if (!isDialogActive()) {
	                	if (dialog.buttonPressed != SceUtilityMsgDialogParams.PSP_UTILITY_BUTTON_PRESSED_OK) {
	                		// Dialog cancelled
	                		savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_SAVE_BAD_PARAMS;
	                	} else if (saveListSelection == null) {
		                    log.warn("Savedata MODE_LISTSAVE no save selected");
		                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_SAVE_BAD_PARAMS;
		                } else {
		                    savedataParams.saveName = saveListSelection;
		                    try {
		                        savedataParams.save(mem);
		                        savedataParams.base.result = 0;
		                    } catch (IOException e) {
		                        savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_SAVE_ACCESS_ERROR;
		                    } catch (Exception e) {
		                        savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_SAVE_ACCESS_ERROR;
		                        log.error(e);
		                    }
		                }
		                finishDialog();
	            	} else {
	                	updateDialog();
	            	}
	                break;
	            }

	            case SceUtilitySavedataParam.MODE_DELETE: {
	                if (savedataParams.saveNameList != null) {
	                    for (int i = 0; i < savedataParams.saveNameList.length; i++) {
	                        String save = savedataParams.getBasePath(savedataParams.saveNameList[i]);
	                        if (Modules.IoFileMgrForUserModule.rmdir(save, true)) {
	                            log.debug("Savedata MODE_DELETE deleting " + save);
	                        }
	                    }
	                    savedataParams.base.result = 0;
	                } else {
		                if (!isDialogOpen()) {
		                    // Search for valid saves.
		                    String pattern = savedataParams.gameName + ".*";

		                    String[] entries = Modules.IoFileMgrForUserModule.listFiles(SceUtilitySavedataParam.savedataPath, pattern);
		                    ArrayList<String> validNames = new ArrayList<String>();
	                    	for (int i = 0; entries != null && i < entries.length; i++) {
	                    		String saveName = entries[i].substring(savedataParams.gameName.length());
		                        if (savedataParams.isPresent(savedataParams.gameName, saveName)) {
		                            validNames.add(saveName);
		                        }
	                    	}

		                    SavedataDialog savedataDialog = new SavedataDialog(savedataParams, this, validNames.toArray(new String[validNames.size()]));
		                    openDialog(savedataDialog);
		                } else if (!isDialogActive()) {
		                	if (dialog.buttonPressed != SceUtilityMsgDialogParams.PSP_UTILITY_BUTTON_PRESSED_OK) {
		                		// Dialog cancelled
		                		savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_DELETE_BAD_PARAMS;
		                	} else if (saveListSelection == null) {
			                    log.warn("Savedata MODE_DELETE no save selected");
			                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_DELETE_BAD_PARAMS;
			                } else {
			                	String dirName = savedataParams.getBasePath(saveListSelection);
			                	if (Modules.IoFileMgrForUserModule.rmdir(dirName, true)) {
		                            log.debug("Savedata MODE_DELETE deleting " + dirName);
			                        savedataParams.base.result = 0;
			                	} else {
			                        savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_DELETE_ACCESS_ERROR;
			                	}
			                }
			                finishDialog();
		                } else {
		                	updateDialog();
		                }
	                }
	                break;
	            }

	            case SceUtilitySavedataParam.MODE_SIZES: {
	                // "METAL SLUG XX" outputs the following on stdout after calling mode 8:
	                //
	                // ------ SIZES ------
	                // ---------- savedata result ----------
	                // result = 0x801103c7
	                //
	                // bind : un used(0x0).
	                //
	                // -- dir name --
	                // title id : ULUS10495
	                // user  id : METALSLUGXX
	                //
	                // ms free size
	                //   cluster size(byte) : 32768 byte
	                //   free cluster num   : 32768
	                //   free size(KB)      : 1048576 KB
	                //   free size(string)  : "1 GB"
	                //
	                // ms data size(titleId=ULUS10495, userId=METALSLUGXX)
	                //   cluster num        : 0
	                //   size (KB)          : 0 KB
	                //   size (string)      : "0 KB"
	                //   size (32KB)        : 0 KB
	                //   size (32KB string) : "0 KB"
	                //
	                // utility data size
	                //   cluster num        : 13
	                //   size (KB)          : 416 KB
	                //   size (string)      : "416 KB"
	                //   size (32KB)        : 416 KB
	                //   size (32KB string) : "416 KB"
	                // error: SCE_UTILITY_SAVEDATA_TYPE_SIZES return 801103c7
	                //
                    int baseResult = 0;
	                String gameName = savedataParams.gameName;
	                String saveName = savedataParams.saveName;

	                // MS free size.
                    // Gets the ammount of free space in the Memory Stick. If null,
                    // the size is ignored and no error is returned.
	                int buffer1Addr = savedataParams.msFreeAddr;
	                if (Memory.isAddressGood(buffer1Addr)) {
	                    String memoryStickFreeSpaceString = MemoryStick.getSizeKbString(MemoryStick.getFreeSizeKb());

	                    mem.write32(buffer1Addr + 0, MemoryStick.getSectorSize());
	                    mem.write32(buffer1Addr + 4, MemoryStick.getFreeSizeKb() / MemoryStick.getSectorSizeKb());
	                    mem.write32(buffer1Addr + 8, MemoryStick.getFreeSizeKb());
	                    Utilities.writeStringNZ(mem, buffer1Addr + 12, 8, memoryStickFreeSpaceString);

	                    log.debug("Memory Stick Free Space = " + memoryStickFreeSpaceString);
	                }

	                // MS data size.
                    // Gets the size of the data already saved in the Memory Stick.
                    // If null, the size is ignored and no error is returned.
	                int buffer2Addr = savedataParams.msDataAddr;
	                if (Memory.isAddressGood(buffer2Addr)) {
	                    gameName = Utilities.readStringNZ(mem, buffer2Addr, 13);
	                    saveName = Utilities.readStringNZ(mem, buffer2Addr + 16, 20);

                        if (savedataParams.isPresent(gameName, saveName)) {
                            int savedataSizeKb = savedataParams.getSizeKb(gameName, saveName);
                            int savedataSize32Kb = MemoryStick.getSize32Kb(savedataSizeKb);

                            mem.write32(buffer2Addr + 36, savedataSizeKb / MemoryStick.getSectorSizeKb()); // Number of sectors.
                            mem.write32(buffer2Addr + 40, savedataSizeKb); // Size in Kb.
                            Utilities.writeStringNZ(mem, buffer2Addr + 44, 8, MemoryStick.getSizeKbString(savedataSizeKb));
                            mem.write32(buffer2Addr + 52, savedataSize32Kb);
                            Utilities.writeStringNZ(mem, buffer2Addr + 56, 8, MemoryStick.getSizeKbString(savedataSize32Kb));

                            log.debug("Memory Stick Full Space = " +  MemoryStick.getSizeKbString(savedataSizeKb));
	                    } else {
                            log.warn("Memory Stick Full Space = no data found!");
	                        baseResult = SceKernelErrors.ERROR_SAVEDATA_SIZES_NO_DATA;
	                    }
	                }

	                // Utility data size.
                    // Gets the size of the data to be saved in the Memory Stick.
                    // If null, the size is ignored and no error is returned.
	                int buffer3Addr = savedataParams.utilityDataAddr;
	                if (Memory.isAddressGood(buffer3Addr)) {
	                    int memoryStickRequiredSpaceKb = 0;
	                    memoryStickRequiredSpaceKb += MemoryStick.getSectorSizeKb(); // Assume 1 sector for SFO-Params
                        // In overwrite mode, the dataSize params are ignored.
                        if (!savedataParams.overwrite) {
                            // The main binary dataSize depends on the fileName existance.
                            if (savedataParams.fileName != null) {
                                memoryStickRequiredSpaceKb += computeMemoryStickRequiredSpaceKb(savedataParams.dataSize);
                            }
                            memoryStickRequiredSpaceKb += computeMemoryStickRequiredSpaceKb(savedataParams.icon0FileData.size);
                            memoryStickRequiredSpaceKb += computeMemoryStickRequiredSpaceKb(savedataParams.icon1FileData.size);
                            memoryStickRequiredSpaceKb += computeMemoryStickRequiredSpaceKb(savedataParams.pic1FileData.size);
                            memoryStickRequiredSpaceKb += computeMemoryStickRequiredSpaceKb(savedataParams.snd0FileData.size);
                        }
	                    String memoryStickRequiredSpaceString = MemoryStick.getSizeKbString(memoryStickRequiredSpaceKb);
	                    int memoryStickRequiredSpace32Kb = MemoryStick.getSize32Kb(memoryStickRequiredSpaceKb);
	                    String memoryStickRequiredSpace32KbString = MemoryStick.getSizeKbString(memoryStickRequiredSpace32Kb);

	                    mem.write32(buffer3Addr + 0, memoryStickRequiredSpaceKb / MemoryStick.getSectorSizeKb());
	                    mem.write32(buffer3Addr + 4, memoryStickRequiredSpaceKb);
	                    Utilities.writeStringNZ(mem, buffer3Addr + 8, 8, memoryStickRequiredSpaceString);
	                    mem.write32(buffer3Addr + 16, memoryStickRequiredSpace32Kb);
	                    Utilities.writeStringNZ(mem, buffer3Addr + 20, 8, memoryStickRequiredSpace32KbString);

	                    log.debug("Memory Stick Required Space = " + memoryStickRequiredSpaceString);
	                }
                    savedataParams.base.result = baseResult;
	                break;
	            }

	            case SceUtilitySavedataParam.MODE_SINGLEDELETE: {
	            	String saveDir = savedataParams.getBasePath();
	                if (Modules.IoFileMgrForUserModule.rmdir(saveDir, true)) {
	                    savedataParams.base.result = 0;
	                } else {
	                    log.warn("Savedata MODE_SINGLEDELETE directory not found!");
	                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_DELETE_NO_DATA;
	                }
	                break;
	            }

	            case SceUtilitySavedataParam.MODE_LIST: {
	                int buffer4Addr = savedataParams.idListAddr;
	                if (Memory.isAddressGood(buffer4Addr)) {
	                    int maxEntries = mem.read32(buffer4Addr + 0);
	                    int entriesAddr = mem.read32(buffer4Addr + 8);
	                    String saveName = savedataParams.saveName;
	                    // PSP file name pattern:
	                    //   '?' matches one character
	                    //   '*' matches any character sequence
	                    // To convert to regular expressions:
	                    //   replace '?' with '.'
	                    //   replace '*' with '.*'
	                    String pattern = saveName.replace('?', '.');
	                    pattern = pattern.replace("*", ".*");
	                    pattern = savedataParams.gameName + pattern;

	                    String[] entries = Modules.IoFileMgrForUserModule.listFiles(SceUtilitySavedataParam.savedataPath, pattern);
	                    log.debug("Entries: " + entries);
	                    int numEntries = entries == null ? 0 : entries.length;
	                    numEntries = Math.min(numEntries, maxEntries);
	                    for (int i = 0; i < numEntries; i++) {
	                        String filePath = SceUtilitySavedataParam.savedataPath + "/" + entries[i];
	                        SceIoStat stat = Modules.IoFileMgrForUserModule.statFile(filePath);
	                        int entryAddr = entriesAddr + i * 72;
	                        if (stat != null) {
	                            mem.write32(entryAddr + 0, stat.mode);
	                            stat.ctime.write(mem, entryAddr + 4);
	                            stat.atime.write(mem, entryAddr + 20);
	                            stat.mtime.write(mem, entryAddr + 36);
	                        }
	                        String entryName = entries[i].substring(savedataParams.gameName.length());
	                        Utilities.writeStringNZ(mem, entryAddr + 52, 20, entryName);
	                    }
	                    mem.write32(buffer4Addr + 4, numEntries);
	                }
	                savedataParams.base.result = 0;
	                break;
	            }

	            case SceUtilitySavedataParam.MODE_FILES: {
	                int buffer5Addr = savedataParams.fileListAddr;
	                if (Memory.isAddressGood(buffer5Addr)) {
	                    int saveFileSecureEntriesAddr = mem.read32(buffer5Addr + 24);
	                    int saveFileEntriesAddr = mem.read32(buffer5Addr + 28);
	                    int systemEntriesAddr = mem.read32(buffer5Addr + 32);

	                    String path = savedataParams.getBasePath(savedataParams.saveName);
	                    String[] entries = Modules.IoFileMgrForUserModule.listFiles(path, null);

	                    int maxNumEntries = (entries == null) ? 0 : entries.length;
	                    int saveFileSecureNumEntries = 0;
	                    int saveFileNumEntries = 0;
	                    int systemFileNumEntries = 0;

	                    // List all files in the savedata (normal and/or encrypted).
	                    for (int i = 0; i < maxNumEntries; i++) {
	                        String filePath = path + "/" + entries[i];
	                        SceIoStat stat = Modules.IoFileMgrForUserModule.statFile(filePath);

	                        // System files.
	                        if (filePath.contains(".SFO") || filePath.contains("ICON") || filePath.contains("PIC") || filePath.contains("SND")) {
	                            if (Memory.isAddressGood(systemEntriesAddr)) {
	                                int entryAddr = systemEntriesAddr + systemFileNumEntries * 80;
	                                if (stat != null) {
	                                    mem.write32(entryAddr + 0, stat.mode);
	                                    mem.write64(entryAddr + 8, stat.size);
	                                    stat.ctime.write(mem, entryAddr + 16);
	                                    stat.atime.write(mem, entryAddr + 32);
	                                    stat.mtime.write(mem, entryAddr + 48);
	                                }
	                                String entryName = entries[i];
	                                Utilities.writeStringNZ(mem, entryAddr + 64, 16, entryName);
	                            }
	                            systemFileNumEntries++;
	                        } else { // Write to secure and normal.
	                            if (Memory.isAddressGood(saveFileSecureEntriesAddr)) {
	                                int entryAddr = saveFileSecureEntriesAddr + saveFileSecureNumEntries * 80;
	                                if (stat != null) {
	                                    mem.write32(entryAddr + 0, stat.mode);
	                                    mem.write64(entryAddr + 8, stat.size);
	                                    stat.ctime.write(mem, entryAddr + 16);
	                                    stat.atime.write(mem, entryAddr + 32);
	                                    stat.mtime.write(mem, entryAddr + 48);
	                                }
	                                String entryName = entries[i];
	                                Utilities.writeStringNZ(mem, entryAddr + 64, 16, entryName);
	                            }
	                            saveFileSecureNumEntries++;

	                            if (Memory.isAddressGood(saveFileEntriesAddr)) {
	                                int entryAddr = saveFileEntriesAddr + saveFileNumEntries * 80;
	                                if (stat != null) {
	                                    mem.write32(entryAddr + 0, stat.mode);
	                                    mem.write64(entryAddr + 8, stat.size);
	                                    stat.ctime.write(mem, entryAddr + 16);
	                                    stat.atime.write(mem, entryAddr + 32);
	                                    stat.mtime.write(mem, entryAddr + 48);
	                                }
	                                String entryName = entries[i];
	                                Utilities.writeStringNZ(mem, entryAddr + 64, 16, entryName);
	                            }
	                            saveFileNumEntries++;
	                        }
	                    }
	                    mem.write32(buffer5Addr + 12, saveFileSecureNumEntries);
	                    mem.write32(buffer5Addr + 16, saveFileNumEntries);
	                    mem.write32(buffer5Addr + 20, systemFileNumEntries);

	                    if(entries == null) {
	                        savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_RW_NO_DATA;
	                    } else {
	                        savedataParams.base.result = 0;
	                    }
	                }
	                break;
	            }

	            case SceUtilitySavedataParam.MODE_MAKEDATA:
	            case SceUtilitySavedataParam.MODE_MAKEDATASECURE: {
	                // Split saving version.
	                // Write system data files (encrypted or not).
	                if (savedataParams.saveName == null || savedataParams.saveName.length() == 0) {
	                    if (savedataParams.saveNameList != null && savedataParams.saveNameList.length > 0) {
	                        savedataParams.saveName = savedataParams.saveNameList[0];
	                    } else {
	                        savedataParams.saveName = "-000";
	                    }
	                }

	                try {
	                    savedataParams.save(mem);
	                    savedataParams.base.result = 0;
	                } catch (IOException e) {
	                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_RW_ACCESS_ERROR;
	                } catch (Exception e) {
	                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_RW_ACCESS_ERROR;
                        log.error(e);
	                }
	                break;
	            }

	            case SceUtilitySavedataParam.MODE_READ:
	            case SceUtilitySavedataParam.MODE_READSECURE: {
	                // Sub-types of mode LOAD.
	                // Read the contents of only one specified file (encrypted or not).
	                if (savedataParams.saveName == null || savedataParams.saveName.length() == 0) {
	                    if (savedataParams.saveNameList != null && savedataParams.saveNameList.length > 0) {
	                        savedataParams.saveName = savedataParams.saveNameList[0];
	                    } else {
	                        savedataParams.saveName = "-000";
	                    }
	                }

	                try {
	                    savedataParams.singleRead(mem);
	                    savedataParams.base.result = 0;
	                    savedataParams.write(mem);
	                } catch (IOException e) {
	                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_RW_NO_DATA;
	                } catch (Exception e) {
	                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_RW_ACCESS_ERROR;
                        log.error(e);
	                }
	                break;
	            }

	            case SceUtilitySavedataParam.MODE_WRITE:
	            case SceUtilitySavedataParam.MODE_WRITESECURE: {
	                // Sub-types of mode SAVE.
	                // Writes the contents of only one specified file (encrypted or not).
	                if (savedataParams.saveName == null || savedataParams.saveName.length() == 0) {
	                    if (savedataParams.saveNameList != null && savedataParams.saveNameList.length > 0) {
	                        savedataParams.saveName = savedataParams.saveNameList[0];
	                    } else {
	                        savedataParams.saveName = "-000";
	                    }
	                }

	                try {
	                    savedataParams.singleWrite(mem);
	                    savedataParams.base.result = 0;
	                } catch (IOException e) {
	                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_RW_ACCESS_ERROR;
	                } catch (Exception e) {
	                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_RW_ACCESS_ERROR;
                        log.error(e);
	                }
	                break;
	            }

	            case SceUtilitySavedataParam.MODE_DELETEDATA:
	                // Sub-type of mode DELETE.
	                // Deletes the contents of only one specified file.
	                if (savedataParams.fileName != null) {
	                    String save = "ms0/PSP/SAVEDATA/" + State.discId + savedataParams.saveName + "/" + savedataParams.fileName;
	                    File f = new File(save);

	                    if (f != null) {
	                        log.debug("Savedata MODE_DELETEDATA deleting " + save);
	                        f = new File(save);
	                        f.delete();
	                    }
	                    savedataParams.base.result = 0;
	                } else {
	                    log.warn("Savedata MODE_DELETEDATA no data found!");
	                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_RW_NO_DATA;
	                }
	                break;

	            case SceUtilitySavedataParam.MODE_GETSIZE:
	                int buffer6Addr = savedataParams.sizeAddr;
                    boolean isPresent = savedataParams.isPresent();

	                if (Memory.isAddressGood(buffer6Addr)) {
	                    int saveFileSecureNumEntries = mem.read32(buffer6Addr + 0);
	                    int saveFileNumEntries = mem.read32(buffer6Addr + 4);
	                    int saveFileSecureEntriesAddr = mem.read32(buffer6Addr + 8);
	                    int saveFileEntriesAddr = mem.read32(buffer6Addr + 12);

	                    int totalSizeKb = 0;

	                    for (int i = 0; i < saveFileSecureNumEntries; i++) {
	                        int entryAddr = saveFileSecureEntriesAddr + i * 24;
	                        long size = mem.read64(entryAddr);
	                        String fileName = Utilities.readStringNZ(entryAddr + 8, 16);
	                        int sizeKb = Utilities.getSizeKb(size);
	                        if (log.isDebugEnabled()) {
	                        	log.debug(String.format("   Secure File '%s', size %d (%d KB)", fileName, size, sizeKb));
	                        }

	                        totalSizeKb += sizeKb;
	                    }
	                    for (int i = 0; i < saveFileNumEntries; i++) {
	                        int entryAddr = saveFileEntriesAddr + i * 24;
	                        long size = mem.read64(entryAddr);
	                        String fileName = Utilities.readStringNZ(entryAddr + 8, 16);
	                        int sizeKb = Utilities.getSizeKb(size);
	                        if (log.isDebugEnabled()) {
	                        	log.debug(String.format("   File '%s', size %d (%d KB)", fileName, size, sizeKb));
	                        }

	                        totalSizeKb += sizeKb;
	                    }

	                    // Free MS size.
	                    int freeSizeKb = MemoryStick.getFreeSizeKb();
	                    String memoryStickFreeSpaceString = MemoryStick.getSizeKbString(freeSizeKb);
	                    mem.write32(buffer6Addr + 16, MemoryStick.getSectorSize());
	                    mem.write32(buffer6Addr + 20, freeSizeKb / MemoryStick.getSectorSizeKb());
	                    mem.write32(buffer6Addr + 24, freeSizeKb);
	                    Utilities.writeStringNZ(mem, buffer6Addr + 28, 8, memoryStickFreeSpaceString);

	                    // If there's not enough size, we have to write how much size we need.
	                    // With enough size, our needed size is always 0.
	                    if (totalSizeKb > freeSizeKb) {
		                    int neededSizeKb = totalSizeKb - freeSizeKb;

		                    // Additional size needed to write savedata.
		                    mem.write32(buffer6Addr + 36, neededSizeKb);
		                    Utilities.writeStringNZ(mem, buffer6Addr + 40, 8, MemoryStick.getSizeKbString(neededSizeKb));

		                    if (isPresent) {
		                    	// Additional size needed to overwrite savedata.
		                    	mem.write32(buffer6Addr + 48, neededSizeKb);
		                    	Utilities.writeStringNZ(mem, buffer6Addr + 52, 8, MemoryStick.getSizeKbString(neededSizeKb));
		                    }
	                    } else {
		                    mem.write32(buffer6Addr + 36, 0);
		                    if (isPresent) {
		                    	mem.write32(buffer6Addr + 48, 0);
		                    }
	                    }
	                }

	                // MODE_GETSIZE also checks if a MemoryStick is inserted and if there're no previous data.
	                if (MemoryStick.getStateMs() != MemoryStick.PSP_MEMORYSTICK_STATE_DRIVER_READY) {
	                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_RW_NO_MEMSTICK;
	                } else if (!isPresent) {
	                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_RW_NO_DATA;
	                } else {
	                    savedataParams.base.result = 0;
	                }
	                break;

	            default:
	                log.warn("Savedata - Unsupported mode " + savedataParams.mode);
	                savedataParams.base.result = -1;
	                break;
	        }

	        savedataParams.base.writeResult(mem);
	        if (log.isDebugEnabled()) {
	            log.debug(String.format("hleUtilitySavedataDisplay result: 0x%08X", savedataParams.base.result));
	        }

	        return false;
		}
    }

    protected static class MsgDialogUtilityDialogState extends UtilityDialogState {
		protected SceUtilityMsgDialogParams msgDialogParams;
		protected MsgDialog msgDialog;

    	public MsgDialogUtilityDialogState(String name) {
			super(name);
		}

		@Override
		protected boolean executeUpdateVisible(Processor processor) {
	        Memory mem = Processor.memory;

	        if (!isDialogOpen()) {
	        	msgDialog = new MsgDialog(msgDialogParams, this);
	        	openDialog(msgDialog);
	        } else if (!isDialogActive()) {
	        	msgDialogParams.buttonPressed = msgDialog.buttonPressed;
	        	if (log.isDebugEnabled()) {
	        		log.debug(String.format("sceUtilityMsgDialog returning buttonPressed=%d", msgDialogParams.buttonPressed));
	        	}
	            msgDialogParams.base.result = 0;
	            msgDialogParams.write(mem);
	        	finishDialog();
	        } else {
	        	msgDialog.checkController();
	        	updateDialog();
	        }

	        return false;
		}

		@Override
		protected pspAbstractMemoryMappedStructure createParams() {
			msgDialogParams = new SceUtilityMsgDialogParams();
			return msgDialogParams;
		}
    }

    protected static class OskUtilityDialogState extends UtilityDialogState {
		protected SceUtilityOskParams oskParams;
		protected OskDialog oskDialog;

    	public OskUtilityDialogState(String name) {
			super(name);
		}

		@Override
		protected boolean executeUpdateVisible(Processor processor) {
	        Memory mem = Processor.memory;

	        if (!isDialogOpen()) {
	        	oskDialog = new OskDialog(oskParams, this);
	        	openDialog(oskDialog);
	        } else if (!isDialogActive()) {
	        	if (oskDialog.buttonPressed == SceUtilityMsgDialogParams.PSP_UTILITY_BUTTON_PRESSED_OK) {
	                oskParams.oskData.result = SceUtilityOskData.PSP_UTILITY_OSK_DATA_CHANGED;
	        		oskParams.oskData.outText = oskDialog.textField.getText();
	                log.info("hleUtilityOskDisplay returning '" + oskParams.oskData.outText + "'");
	        	} else {
	                oskParams.oskData.result = SceUtilityOskData.PSP_UTILITY_OSK_DATA_CANCELED;
	        		oskParams.oskData.outText = oskDialog.textField.getText();
	                log.info("hleUtilityOskDisplay cancelled");
	        	}
	        	oskParams.base.result = 0;
	        	oskParams.write(mem);
	            finishDialog();
	        } else {
	        	oskDialog.checkController();
	        	updateDialog();
	        }

	        return false;
		}

		@Override
		protected pspAbstractMemoryMappedStructure createParams() {
			oskParams = new SceUtilityOskParams();
			return oskParams;
		}
    }

    protected static class GameSharingUtilityDialogState extends UtilityDialogState {
		protected SceUtilityGameSharingParams gameSharingParams;

    	public GameSharingUtilityDialogState(String name) {
			super(name);
		}

		@Override
		protected boolean executeUpdateVisible(Processor processor) {
			// TODO to be implemented
			return false;
		}

		@Override
		protected pspAbstractMemoryMappedStructure createParams() {
			gameSharingParams = new SceUtilityGameSharingParams();
			return gameSharingParams;
		}
    }

    protected static class NetconfUtilityDialogState extends UtilityDialogState {
		protected SceUtilityNetconfParams netconfParams;

    	public NetconfUtilityDialogState(String name) {
			super(name);
		}

		@Override
		protected boolean executeUpdateVisible(Processor processor) {
			// TODO to be implemented
			boolean keepVisible = false;

			if (netconfParams.netAction == SceUtilityNetconfParams.PSP_UTILITY_NETCONF_CONNECT_APNET ||
			    netconfParams.netAction == SceUtilityNetconfParams.PSP_UTILITY_NETCONF_CONNECT_APNET_LASTUSED) {
				int state = Modules.sceNetApctl.hleNetApctlGetState();

				// The Netconf dialog stays visible until the network reaches
				// the state PSP_NET_APCTL_STATE_GOT_IP.
				if (state == sceNetApctl.PSP_NET_APCTL_STATE_GOT_IP) {
					keepVisible = false;
				} else {
					keepVisible = true;
					if (state == sceNetApctl.PSP_NET_APCTL_STATE_DISCONNECTED) {
						// When connecting with infrastructure, simulate a connection
						// using the first network configuration entry.
						Modules.sceNetApctl.hleNetApctlConnect(1);
					}
				}
			}

			return keepVisible;
		}

		@Override
		protected pspAbstractMemoryMappedStructure createParams() {
			netconfParams = new SceUtilityNetconfParams();
			return netconfParams;
		}
    }

    protected static class ScreenshotUtilityDialogState extends UtilityDialogState {
		protected SceUtilityScreenshotParams screenshotParams;

    	public ScreenshotUtilityDialogState(String name) {
			super(name);
		}

		@Override
		protected boolean executeUpdateVisible(Processor processor) {
			// TODO to be implemented
			return false;
		}

        protected void executeContStart(Processor processor) {
			// Continuous mode which takes several screenshots
            // on regular intervals set by an internal counter.

            // To execute the cont mode, the screenshot utility must
            // be initialized with sceUtilityScreenshotInitStart and the startupType
            // parameter has to be PSP_UTILITY_SCREENSHOT_TYPE_CONT_AUTO, otherwise, an
            // error is returned.

            CpuState cpu = processor.cpu;
            Memory mem = Memory.getInstance();

            paramsAddr = cpu.gpr[4];
            if (!Memory.isAddressGood(paramsAddr)) {
                log.error(String.format("%sContStart bad address 0x%08X", name, paramsAddr));
                cpu.gpr[2] = -1;
            } else {
                this.params = createParams();
                params.read(mem, paramsAddr);
                if (log.isInfoEnabled()) {
                    log.info(String.format("%sContStart %s", name, params.toString()));
                }
                if (screenshotParams.isContModeOn()) {
                    // Start with INIT
                    status = PSP_UTILITY_DIALOG_STATUS_INIT;
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = SceKernelErrors.ERROR_SCREENSHOT_CONT_MODE_NOT_INIT;
                }
            }
		}

		@Override
		protected pspAbstractMemoryMappedStructure createParams() {
			screenshotParams = new SceUtilityScreenshotParams();
			return screenshotParams;
		}
    }

    protected static class GamedataInstallUtilityDialogState extends UtilityDialogState {
    	protected SceUtilityGamedataInstallParams gamedataInstallParams;

    	public GamedataInstallUtilityDialogState(String name) {
            super(name);
        }

		@Override
		protected pspAbstractMemoryMappedStructure createParams() {
			gamedataInstallParams = new SceUtilityGamedataInstallParams();
			return gamedataInstallParams;
		}

		@Override
		protected boolean executeUpdateVisible(Processor processor) {
			return false;
		}
    }

    protected static class NpSigninUtilityDialogState extends UtilityDialogState {
    	protected SceUtilityNpSigninParams npSigninParams;

    	public NpSigninUtilityDialogState(String name) {
            super(name);
        }

		@Override
		protected pspAbstractMemoryMappedStructure createParams() {
			npSigninParams = new SceUtilityNpSigninParams();
			return npSigninParams;
		}

		@Override
		protected boolean executeUpdateVisible(Processor processor) {
			return false;
		}
    }

    protected static class HtmlViewerUtilityDialogState extends UtilityDialogState {
		protected SceUtilityHtmlViewerParams htmlViewerParams;

    	public HtmlViewerUtilityDialogState(String name) {
			super(name);
		}

		@Override
		protected boolean executeUpdateVisible(Processor processor) {
			// TODO to be implemented
			return false;
		}

		@Override
		protected pspAbstractMemoryMappedStructure createParams() {
			htmlViewerParams = new SceUtilityHtmlViewerParams();
			return htmlViewerParams;
		}
    }

    protected static abstract class UtilityDialog extends JComponent {
		private static final long serialVersionUID = -993546461292372048L;
		protected JDialog dialog;
		protected int buttonPressed;
		protected JPanel messagePane;
		protected JPanel buttonPane;
		protected ActionListener closeActionListener;
		protected static final String actionCommandOK = "OK";
		protected static final String actionCommandYES = "YES";
		protected static final String actionCommandNO = "NO";
		protected static final String actionCommandESC = "ESC";
		protected UtilityDialogState utilityDialogState;
		protected String confirmButtonActionCommand = actionCommandOK;
		protected String cancelButtonActionCommand = actionCommandESC;
		protected long pressedTimestamp;
		protected static final int repeatDelay = 200000;
		protected boolean downPressedButton;
		protected boolean downPressedAnalog;
		protected boolean upPressedButton;
		protected boolean upPressedAnalog;

		protected void createDialog(final UtilityDialogState utilityDialogState, String message) {
			this.utilityDialogState = utilityDialogState;

			String title = String.format("Message from %s", State.title);
	        dialog = new JDialog((Frame) null, title, false);

	        dialog.setSize(Settings.getInstance().readWindowSize(utilityDialogState.name, 200, 100));
	        dialog.setLocation(Settings.getInstance().readWindowPos(utilityDialogState.name));
	        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

			messagePane = new JPanel();
    		messagePane.setBorder(new EmptyBorder(5, 10, 5, 10));
    		messagePane.setLayout(new BoxLayout(messagePane, BoxLayout.Y_AXIS));

    		if (message != null) {
	    		message = formatMessageForDialog(message);
	    		// Split the message according to the new lines
				while (message.length() > 0) {
					int newLinePosition = message.indexOf("\n");
					JLabel label = new JLabel();
					label.setHorizontalAlignment(JLabel.CENTER);
					label.setAlignmentX(CENTER_ALIGNMENT);
					if (newLinePosition < 0) {
						label.setText(message);
						message = "";
					} else {
						String messagePart = message.substring(0, newLinePosition);
						label.setText(messagePart);
						message = message.substring(newLinePosition + 1);
					}
					messagePane.add(label);
				}
    		}

    		if (JDialog.isDefaultLookAndFeelDecorated()) {
    			if (UIManager.getLookAndFeel().getSupportsWindowDecorations()) {
    				dialog.setUndecorated(true);
    				getRootPane().setWindowDecorationStyle(JRootPane.INFORMATION_DIALOG);
    			}
    		}

    		buttonPane = new JPanel();
    		buttonPane.setBorder(new EmptyBorder(5, 10, 5, 10));
    		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));

    		closeActionListener = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					processActionCommand(event.getActionCommand());
					dialog.dispose();
				}
    		};
		}

		protected void processActionCommand(String actionCommand) {
			if (actionCommandYES.equals(actionCommand)) {
				buttonPressed = SceUtilityMsgDialogParams.PSP_UTILITY_BUTTON_PRESSED_YES;
			} else if (actionCommandNO.equals(actionCommand)) {
				buttonPressed = SceUtilityMsgDialogParams.PSP_UTILITY_BUTTON_PRESSED_NO;
			} else if (actionCommandOK.equals(actionCommand)) {
				buttonPressed = SceUtilityMsgDialogParams.PSP_UTILITY_BUTTON_PRESSED_OK;
			} else if (actionCommandESC.equals(actionCommand)) {
				buttonPressed = SceUtilityMsgDialogParams.PSP_UTILITY_BUTTON_PRESSED_ESC;
			} else {
				buttonPressed = SceUtilityMsgDialogParams.PSP_UTILITY_BUTTON_PRESSED_INVALID;
			}
		}

		protected void endDialog() {
	        Container contentPane = dialog.getContentPane();
    		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
    		contentPane.add(messagePane);
    		contentPane.add(buttonPane);

			dialog.pack();
		}

		protected void setDefaultButton(JButton button) {
			dialog.getRootPane().setDefaultButton(button);
		}

		@Override
		public void setVisible(boolean flag) {
			dialog.setVisible(flag);
		}

		@Override
		public boolean isVisible() {
			return dialog.isVisible();
		}

		@Override
		public Point getLocation() {
			return dialog.getLocation();
		}

		@Override
		public Dimension getSize() {
			return dialog.getSize();
		}

        protected boolean isButtonPressed(int button) {
        	Controller controller = State.controller;
        	if ((controller.getButtons() & button) == button) {
        		return true;
        	}

        	return false;
        }

        protected boolean isConfirmButtonPressed() {
        	return isButtonPressed(Modules.sceUtilityModule.systemParam_buttonPreference == PSP_SYSTEMPARAM_BUTTON_CIRCLE ? sceCtrl.PSP_CTRL_CIRCLE : sceCtrl.PSP_CTRL_CROSS);
        }

        protected boolean isCancelButtonPressed() {
        	return isButtonPressed(Modules.sceUtilityModule.systemParam_buttonPreference == PSP_SYSTEMPARAM_BUTTON_CIRCLE ? sceCtrl.PSP_CTRL_CROSS : sceCtrl.PSP_CTRL_CIRCLE);
        }

        private int getControllerLy() {
        	return State.controller.getLy() & 0xFF;
        }

        private int getControllerAnalogCenter() {
        	return Controller.analogCenter & 0xFF;
        }

        private void checkRepeat() {
        	if (pressedTimestamp != 0 && SystemTimeManager.getSystemTime() - pressedTimestamp > repeatDelay) {
        		upPressedAnalog = false;
        		upPressedButton = false;
        		downPressedAnalog = false;
        		downPressedButton = false;
        		pressedTimestamp = 0;
        	}
        }

        protected boolean isUpPressed() {
        	checkRepeat();
        	if (upPressedButton || upPressedAnalog) {
            	if (!isButtonPressed(sceCtrl.PSP_CTRL_UP)) {
            		upPressedButton = false;
            	}

            	if (getControllerLy() >= getControllerAnalogCenter()) {
            		upPressedAnalog = false;
            	}

            	return false;
        	}

        	if (isButtonPressed(sceCtrl.PSP_CTRL_UP)) {
        		upPressedButton = true;
        		pressedTimestamp = SystemTimeManager.getSystemTime();
        		return true;
        	}

        	if (getControllerLy() < getControllerAnalogCenter()) {
        		upPressedAnalog = true;
        		pressedTimestamp = SystemTimeManager.getSystemTime();
        		return true;
        	}

        	return false;
        }

        protected boolean isDownPressed() {
        	checkRepeat();
        	if (downPressedButton || downPressedAnalog) {
            	if (!isButtonPressed(sceCtrl.PSP_CTRL_DOWN)) {
            		downPressedButton = false;
            	}

            	if (getControllerLy() <= getControllerAnalogCenter()) {
            		downPressedAnalog = false;
            	}

            	return false;
        	}

        	if (isButtonPressed(sceCtrl.PSP_CTRL_DOWN)) {
        		downPressedButton = true;
        		pressedTimestamp = SystemTimeManager.getSystemTime();
        		return true;
        	}

        	if (getControllerLy() > getControllerAnalogCenter()) {
        		downPressedAnalog = true;
        		pressedTimestamp = SystemTimeManager.getSystemTime();
        		return true;
        	}

        	return false;
        }

        public void checkController() {
			if (isConfirmButtonPressed()) {
				processActionCommand(confirmButtonActionCommand);
				dialog.dispose();
			} else if (isCancelButtonPressed()) {
				processActionCommand(cancelButtonActionCommand);
				dialog.dispose();
			}
		}
    }

    protected static class SavedataDialog extends UtilityDialog {
		private static final long serialVersionUID = 3753863112417187248L;
		private final JTable table;
		private SavedataUtilityDialogState savedataDialogState;
		private SceUtilitySavedataParam savedataParams;
		private final String[] saveNames;

		public SavedataDialog(final SceUtilitySavedataParam savedataParams, final SavedataUtilityDialogState savedataDialogState, final String[] saveNames) {
			this.savedataDialogState = savedataDialogState;
			this.savedataParams = savedataParams;
			this.saveNames = saveNames;

			createDialog(savedataDialogState, null);

			dialog.setTitle("Savedata List");
            dialog.setSize(Settings.getInstance().readWindowSize(savedataDialogState.name, 400, 401));

            final JButton selectButton = new JButton("Select");
            selectButton.setActionCommand(actionCommandOK);
            setDefaultButton(selectButton);

            final JButton cancelButton = new CancelButton(dialog);
            cancelButton.setActionCommand(actionCommandESC);
            cancelButton.addActionListener(closeActionListener);

            SavedataListTableModel savedataListTableModel = new SavedataListTableModel(saveNames, savedataParams);
            SavedataListTableColumnModel savedataListTableColumnModel = new SavedataListTableColumnModel();
            savedataListTableColumnModel.setFontHeight(savedataListTableModel.getFontHeight());

            table = new JTable(savedataListTableModel, savedataListTableColumnModel);
            table.setRowHeight(80);
            table.setRowSelectionAllowed(true);
            table.setColumnSelectionAllowed(false);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent event) {
                    selectButton.setEnabled(!((ListSelectionModel) event.getSource()).isSelectionEmpty());
                }
            });
            table.setFont(new Font("SansSerif", Font.PLAIN, fontHeightSavedataList[0]));
            JScrollPane listScroll = new JScrollPane(table);

            GroupLayout layout = new GroupLayout(dialog.getRootPane());
            layout.setAutoCreateGaps(true);
            layout.setAutoCreateContainerGaps(true);

            layout.setHorizontalGroup(layout.createParallelGroup(
                    GroupLayout.Alignment.TRAILING).addComponent(listScroll).addGroup(
                    layout.createSequentialGroup().addComponent(selectButton).addComponent(cancelButton)));

            layout.setVerticalGroup(layout.createSequentialGroup().addComponent(
                    listScroll).addGroup(
                    layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(selectButton).addComponent(cancelButton)));

            dialog.getRootPane().setLayout(layout);

            selectButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    if (updateSelection()) {
    					processActionCommand(event.getActionCommand());
                        dialog.dispose();
                    }
                }
            });

            // Define the selected row according to the focus field
            int selectedRow = 0;
            switch (savedataParams.focus) {
            	case SceUtilitySavedataParam.FOCUS_FIRSTLIST: {
            		selectedRow = 0;
            		break;
            	}
            	case SceUtilitySavedataParam.FOCUS_LASTLIST: {
            		selectedRow = table.getRowCount() - 1;
            		break;
            	}
            	case SceUtilitySavedataParam.FOCUS_LATEST: {
            		long latestTimestamp = Long.MIN_VALUE;
            		for (int i = 0; i < saveNames.length; i++) {
            			long timestamp = getTimestamp(saveNames[i]);
            			if (timestamp > latestTimestamp) {
            				latestTimestamp = timestamp;
            				selectedRow = i;
            			}
            		}
            		break;
            	}
            	case SceUtilitySavedataParam.FOCUS_OLDEST: {
            		long oldestTimestamp = Long.MAX_VALUE;
            		for (int i = 0; i < saveNames.length; i++) {
            			long timestamp = getTimestamp(saveNames[i]);
            			if (timestamp < oldestTimestamp) {
            				oldestTimestamp = timestamp;
            				selectedRow = i;
            			}
            		}
            		break;
            	}
            	case SceUtilitySavedataParam.FOCUS_FIRSTEMPTY: {
            		for (int i = 0; i < saveNames.length; i++) {
            			if (isEmpty(saveNames[i])) {
            				selectedRow = i;
            				break;
            			}
            		}
            		break;
            	}
            	case SceUtilitySavedataParam.FOCUS_LASTEMPTY: {
            		for (int i = saveNames.length - 1; i >= 0; i--) {
            			if (isEmpty(saveNames[i])) {
            				selectedRow = i;
            				break;
            			}
            		}
            		break;
            	}
            }

            if (selectedRow >= 0 && selectedRow < table.getRowCount()) {
				table.changeSelection(selectedRow, table.getSelectedColumn(), false, false);
            }

            endDialog();
		}

		private boolean isEmpty(String saveName) {
            return !savedataParams.isPresent(savedataParams.gameName, saveName);
		}

		private long getTimestamp(String saveName) {
			return savedataParams.getTimestamp(savedataParams.gameName, saveName);
		}

		private boolean updateSelection() {
			int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < saveNames.length) {
                savedataDialogState.saveListSelection = saveNames[selectedRow];
                return true;
            }

            return false;
		}

		@Override
		public void checkController() {
			int selectedRow = table.getSelectedRow();
			if (isDownPressed()) {
				// One row down
				selectedRow++;
			} else if (isUpPressed()) {
				// One row up
				selectedRow--;
			} else if (isButtonPressed(sceCtrl.PSP_CTRL_LTRIGGER)) {
				// First row
				selectedRow = 0;
			} else if (isButtonPressed(sceCtrl.PSP_CTRL_RTRIGGER)) {
				// Last row
				selectedRow = table.getRowCount() - 1;
			}

			// Update selected row if changed
			if (selectedRow != table.getSelectedRow()) {
				selectedRow = Math.min(selectedRow, table.getRowCount() - 1);
				selectedRow = Math.max(selectedRow, 0);
				table.changeSelection(selectedRow, table.getSelectedColumn(), false, false);
			}

			if (updateSelection() || isCancelButtonPressed()) {
				super.checkController();
			}
		}
    }

    protected static class MsgDialog extends UtilityDialog {
		private static final long serialVersionUID = 3823899730551154698L;
		protected SceUtilityMsgDialogParams msgDialogParams;

		public MsgDialog(final SceUtilityMsgDialogParams msgDialogParams, MsgDialogUtilityDialogState msgDialogState) {
			this.msgDialogParams = msgDialogParams;
			createDialog(msgDialogState, msgDialogParams.message);

    		if (msgDialogParams.isOptionYesNo()) {
    			JButton yesButton = new JButton("Yes");
    			JButton noButton = new JButton("No");
    			yesButton.addActionListener(closeActionListener);
    			yesButton.setActionCommand(actionCommandYES);
    			noButton.addActionListener(closeActionListener);
    			noButton.setActionCommand(actionCommandNO);
    			confirmButtonActionCommand = actionCommandYES;
    			cancelButtonActionCommand = actionCommandNO;
    			buttonPane.add(yesButton);
    			buttonPane.add(noButton);
    			if (msgDialogParams.isOptionYesNoDefaultYes()) {
    				setDefaultButton(yesButton);
    			} else if (msgDialogParams.isOptionYesNoDefaultNo()) {
    				setDefaultButton(noButton);
    			}
    		} else if (msgDialogParams.isOptionOk()) {
    			JButton okButton = new JButton("Ok");
    			okButton.addActionListener(closeActionListener);
    			okButton.setActionCommand(actionCommandOK);
    			buttonPane.add(okButton);
    			setDefaultButton(okButton);
    		} else if (msgDialogParams.mode == SceUtilityMsgDialogParams.PSP_UTILITY_MSGDIALOG_MODE_TEXT) {
    			JButton okButton = new JButton("Ok");
    			okButton.addActionListener(closeActionListener);
    			okButton.setActionCommand(actionCommandOK);
    			buttonPane.add(okButton);
    			setDefaultButton(okButton);
    		} else if (msgDialogParams.mode == SceUtilityMsgDialogParams.PSP_UTILITY_MSGDIALOG_MODE_ERROR) {
    			String errorMessage = String.format("Error 0x%08X", msgDialogParams.errorValue);
    			JLabel errorMessageLabel = new JLabel(errorMessage);
    			messagePane.add(errorMessageLabel);

    			JButton okButton = new JButton("Ok");
    			okButton.addActionListener(closeActionListener);
    			okButton.setActionCommand(actionCommandOK);
    			buttonPane.add(okButton);
    			setDefaultButton(okButton);
    		}

    		endDialog();
    	}
    }

    protected static class OskDialog extends UtilityDialog {
		private static final long serialVersionUID = 1155047781007677923L;
		protected JTextField textField;

		public OskDialog(final SceUtilityOskParams oskParams, OskUtilityDialogState oskState) {
			createDialog(oskState, oskParams.oskData.desc);

			textField = new JTextField(oskParams.oskData.inText);
			messagePane.add(textField);

			JButton okButton = new JButton("Ok");
			okButton.addActionListener(closeActionListener);
			okButton.setActionCommand(actionCommandOK);
			buttonPane.add(okButton);
			setDefaultButton(okButton);

	        endDialog();
    	}
    }

    protected static String getNetParamName(int id) {
    	if (id == 0) {
    		return "";
    	}
    	return String.format(dummyNetParamName, id);
    }

    protected static String formatMessageForDialog(String message) {
        StringBuilder formattedMessage = new StringBuilder();

        for (int i = 0; i < message.length();) {
            String rest = message.substring(i);
            int newLineIndex = rest.indexOf("\n");
            if (newLineIndex >= 0 && newLineIndex < maxLineLengthForDialog) {
            	formattedMessage.append(rest.substring(0, newLineIndex + 1));
            	i += newLineIndex + 1;
            } else if (rest.length() > maxLineLengthForDialog) {
                int lastSpace = rest.lastIndexOf(' ', maxLineLengthForDialog);
                rest = rest.substring(0, (lastSpace >= 0 ? lastSpace : maxLineLengthForDialog));
                formattedMessage.append(rest);
                i += rest.length() + 1;
                formattedMessage.append("\n");
            } else {
                formattedMessage.append(rest);
                i += rest.length();
            }
        }

        return formattedMessage.toString();
    }

    protected static class SavedataListTableColumnModel extends DefaultTableColumnModel {
        private static final long serialVersionUID = -2460343777558549264L;
        private int fontHeight = 12;

        private final class CellRenderer extends DefaultTableCellRenderer {

            private static final long serialVersionUID = 6230063075762638253L;

            @Override
            public Component getTableCellRendererComponent(JTable table,
                    Object obj, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                if (obj instanceof Icon) {
                    setIcon((Icon) obj);
                } else if (obj instanceof String) {
                    JTextArea textArea = new JTextArea((String) obj);
                    textArea.setFont(new Font("SansSerif", Font.PLAIN, fontHeight));
                    if (isSelected) {
                        textArea.setForeground(table.getSelectionForeground());
                        textArea.setBackground(table.getSelectionBackground());
                    } else {
                        textArea.setForeground(table.getForeground());
                        textArea.setBackground(table.getBackground());
                    }
                    return textArea;
                } else {
                    setIcon(null);
                }
                return super.getTableCellRendererComponent(table, obj, isSelected, hasFocus, row, column);
            }
        }

        public SavedataListTableColumnModel() {
            setColumnMargin(0);
            CellRenderer cellRenderer = new CellRenderer();
            TableColumn tableColumn = new TableColumn(0, 144, cellRenderer, null);
            tableColumn.setHeaderValue(Resource.get("icon"));
            tableColumn.setMaxWidth(144);
            tableColumn.setMinWidth(144);
            TableColumn tableColumn2 = new TableColumn(1, 100, cellRenderer, null);
            tableColumn2.setHeaderValue(Resource.get("title"));
            addColumn(tableColumn);
            addColumn(tableColumn2);
        }

        public void setFontHeight(int fontHeight) {
            this.fontHeight = fontHeight;
        }
    }

    /**
     * Count how many times a string "find" occurs in a string "s".
     * @param s    the string where to count
     * @param find count how many times this string occurs in string "s"
     * @return     the number of times "find" occurs in "s"
     */
    private static int count(String s, String find) {
        int count = 0;
        int i = 0;
        while ((i = s.indexOf(find, i)) >= 0) {
            count++;
            i = i + find.length();
        }
        return count;
    }

    private static int computeMemoryStickRequiredSpaceKb(int sizeByte) {
        int sizeKb = Utilities.getSizeKb(sizeByte);
        int sectorSizeKb = MemoryStick.getSectorSizeKb();
        int numberSectors = (sizeKb + sectorSizeKb - 1) / sectorSizeKb;

        return numberSectors * sectorSizeKb;
    }

    protected static class SavedataListTableModel extends AbstractTableModel {
        private static final long serialVersionUID = -8867168909834783380L;
        private int numberRows;
        private ImageIcon[] icons;
        private String[] descriptions;
        private int fontHeight = 12;

        public SavedataListTableModel(String[] saveNames, SceUtilitySavedataParam savedataParams) {
            numberRows = saveNames == null ? 0 : saveNames.length;
            icons = new ImageIcon[numberRows];
            descriptions = new String[numberRows];

            for (int i = 0; i < numberRows; i++) {
                if (saveNames[i] != null) {
                    // Get icon0 file
                    String iconFileName = savedataParams.getFileName(saveNames[i], SceUtilitySavedataParam.icon0FileName);
                    SeekableDataInput iconDataInput = Modules.IoFileMgrForUserModule.getFile(iconFileName, IoFileMgrForUser.PSP_O_RDONLY);
                    if (iconDataInput != null) {
                        try {
                            int length = (int) iconDataInput.length();
                            byte[] iconBuffer = new byte[length];
                            iconDataInput.readFully(iconBuffer);
                            iconDataInput.close();
                            icons[i] = new ImageIcon(iconBuffer);
                        } catch (IOException e) {
                        }
                    }

                    // Get values (title, detail...) from SFO file
                    String sfoFileName = savedataParams.getFileName(saveNames[i], SceUtilitySavedataParam.paramSfoFileName);
                    SeekableDataInput sfoDataInput = Modules.IoFileMgrForUserModule.getFile(sfoFileName, IoFileMgrForUser.PSP_O_RDONLY);
                    if (sfoDataInput != null) {
                        try {
                            int length = (int) sfoDataInput.length();
                            byte[] sfoBuffer = new byte[length];
                            sfoDataInput.readFully(sfoBuffer);
                            sfoDataInput.close();

                            PSF psf = new PSF();
                            psf.read(ByteBuffer.wrap(sfoBuffer));
                            String title = psf.getString("TITLE");
                            String detail = psf.getString("SAVEDATA_DETAIL");
                            String savedataTitle = psf.getString("SAVEDATA_TITLE");

                            // Get Modification time of SFO file
                            SceIoStat sfoStat = Modules.IoFileMgrForUserModule.statFile(sfoFileName);
                            Calendar cal = Calendar.getInstance();
                            ScePspDateTime pspTime = sfoStat.mtime;
                            cal.set(pspTime.year, pspTime.month, pspTime.day, pspTime.hour, pspTime.minute, pspTime.second);

                            descriptions[i] = String.format("%1$s\n%4$tF %4$tR\n%2$s\n%3$s", title, savedataTitle, detail, cal);
                            int numberLines = 1 + count(descriptions[i], "\n");
                            if (numberLines < fontHeightSavedataList.length) {
                                setFontHeight(fontHeightSavedataList[numberLines]);
                            } else {
                                setFontHeight(fontHeightSavedataList[fontHeightSavedataList.length - 1]);
                            }
                        } catch (IOException e) {
                        }
                    }
                }

                // default icon
                if (icons[i] == null) {
                    icons[i] = new ImageIcon(getClass().getResource("/jpcsp/images/icon0.png"));
                }

                // default description
                if (descriptions[i] == null) {
                    descriptions[i] = "Not present";
                }

                // Rescale over sized icons
                if (icons[i] != null) {
                    Image image = icons[i].getImage();
                    if (image.getWidth(null) > 144 || image.getHeight(null) > 80) {
                        image = image.getScaledInstance(144, 80, Image.SCALE_SMOOTH);
                        icons[i].setImage(image);
                    }
                }
            }
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return numberRows;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return icons[rowIndex];
            }
            return descriptions[rowIndex];
        }

        public int getFontHeight() {
            return fontHeight;
        }

        public void setFontHeight(int fontHeight) {
            this.fontHeight = fontHeight;
        }
    }

    public void sceUtilityGameSharingInitStart(Processor processor) {
        gameSharingState.executeInitStart(processor);
    }

    public void sceUtilityGameSharingShutdownStart(Processor processor) {
        gameSharingState.executeShutdownStart(processor);
    }

    public void sceUtilityGameSharingUpdate(Processor processor) {
    	gameSharingState.executeUpdate(processor);
    }

    public void sceUtilityGameSharingGetStatus(Processor processor) {
        gameSharingState.executeGetStatus(processor);
    }

    public void sceNetplayDialogInitStart(Processor processor) {
        netplayDialogState.executeInitStart(processor);
    }

    public void sceNetplayDialogShutdownStart(Processor processor) {
        netplayDialogState.executeShutdownStart(processor);
    }

    public void sceNetplayDialogUpdate(Processor processor) {
    	netplayDialogState.executeUpdate(processor);
    }

    public void sceNetplayDialogGetStatus(Processor processor) {
        netplayDialogState.executeGetStatus(processor);
    }

    public void sceUtilityNetconfInitStart(Processor processor) {
        netconfState.executeInitStart(processor);
    }

    public void sceUtilityNetconfShutdownStart(Processor processor) {
        netconfState.executeShutdownStart(processor);
    }

    public void sceUtilityNetconfUpdate(Processor processor) {
    	netconfState.executeUpdate(processor);
    }

    public void sceUtilityNetconfGetStatus(Processor processor) {
        netconfState.executeGetStatus(processor);
    }

    public void sceUtilitySavedataInitStart(Processor processor) {
        savedataState.executeInitStart(processor);
    }

    public void sceUtilitySavedataShutdownStart(Processor processor) {
        savedataState.executeShutdownStart(processor);
    }

    public void sceUtilitySavedataUpdate(Processor processor) {
    	savedataState.executeUpdate(processor);
    }

    public void sceUtilitySavedataGetStatus(Processor processor) {
        savedataState.executeGetStatus(processor);
    }

    public void sceUtilitySavedataErrInitStart(Processor processor) {
        savedataErrState.executeInitStart(processor);
    }

    public void sceUtilitySavedataErrShutdownStart(Processor processor) {
        savedataErrState.executeShutdownStart(processor);
    }

    public void sceUtilitySavedataErrUpdate(Processor processor) {
    	savedataErrState.executeUpdate(processor);
    }

    public void sceUtilitySavedataErrGetStatus(Processor processor) {
        savedataErrState.executeGetStatus(processor);
    }

    public void sceUtilityMsgDialogInitStart(Processor processor) {
        msgDialogState.executeInitStart(processor);
    }

    public void sceUtilityMsgDialogShutdownStart(Processor processor) {
        msgDialogState.executeShutdownStart(processor);
    }

    public void sceUtilityMsgDialogUpdate(Processor processor) {
    	msgDialogState.executeUpdate(processor);
    }

    public void sceUtilityMsgDialogGetStatus(Processor processor) {
        msgDialogState.executeGetStatus(processor);
    }

    public void sceUtilityOskInitStart(Processor processor) {
        oskState.executeInitStart(processor);
    }

    public void sceUtilityOskShutdownStart(Processor processor) {
        oskState.executeShutdownStart(processor);
    }

    public void sceUtilityOskUpdate(Processor processor) {
    	oskState.executeUpdate(processor);
    }

    public void sceUtilityOskGetStatus(Processor processor) {
        oskState.executeGetStatus(processor);
    }

    public void sceUtilityNpSigninInitStart(Processor processor) {
        npSigninState.executeInitStart(processor);
    }

    public void sceUtilityNpSigninShutdownStart(Processor processor) {
        npSigninState.executeShutdownStart(processor);
    }

    public void sceUtilityNpSigninUpdate(Processor processor) {
    	npSigninState.executeUpdate(processor);
    }

    public void sceUtilityNpSigninGetStatus(Processor processor) {
        npSigninState.executeGetStatus(processor);
    }

    public void sceUtilityPS3ScanInitStart(Processor processor) {
        PS3ScanState.executeInitStart(processor);
    }

    public void sceUtilityPS3ScanShutdownStart(Processor processor) {
        PS3ScanState.executeShutdownStart(processor);
    }

    public void sceUtilityPS3ScanUpdate(Processor processor) {
    	PS3ScanState.executeUpdate(processor);
    }

    public void sceUtilityPS3ScanGetStatus(Processor processor) {
        PS3ScanState.executeGetStatus(processor);
    }

    public void sceUtilityRssReaderInitStart(Processor processor) {
        rssReaderState.executeInitStart(processor);
    }

    public void sceUtilityRssReaderContStart(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented: sceUtilityRssReaderContStart");

        cpu.gpr[2] = SceKernelErrors.ERROR_UTILITY_IS_UNKNOWN;
    }

    public void sceUtilityRssReaderShutdownStart(Processor processor) {
        rssReaderState.executeShutdownStart(processor);
    }

    public void sceUtilityRssReaderUpdate(Processor processor) {
    	rssReaderState.executeUpdate(processor);
    }

    public void sceUtilityRssReaderGetStatus(Processor processor) {
        rssReaderState.executeGetStatus(processor);
    }

    public void sceUtilityRssSubscriberInitStart(Processor processor) {
        rssSubscriberState.executeInitStart(processor);
    }

    public void sceUtilityRssSubscriberShutdownStart(Processor processor) {
        rssSubscriberState.executeShutdownStart(processor);
    }

    public void sceUtilityRssSubscriberUpdate(Processor processor) {
    	rssSubscriberState.executeUpdate(processor);
    }

    public void sceUtilityRssSubscriberGetStatus(Processor processor) {
        rssSubscriberState.executeGetStatus(processor);
    }

    public void sceUtilityScreenshotInitStart(Processor processor) {
        screenshotState.executeInitStart(processor);
    }

    public void sceUtilityScreenshotContStart(Processor processor) {
        screenshotState.executeContStart(processor);
    }

    public void sceUtilityScreenshotShutdownStart(Processor processor) {
        screenshotState.executeShutdownStart(processor);
    }

    public void sceUtilityScreenshotUpdate(Processor processor) {
    	screenshotState.executeUpdate(processor);
    }

    public void sceUtilityScreenshotGetStatus(Processor processor) {
        screenshotState.executeGetStatus(processor);
    }

    public void sceUtilityHtmlViewerInitStart(Processor processor) {
        htmlViewerState.executeInitStart(processor);
    }

    public void sceUtilityHtmlViewerShutdownStart(Processor processor) {
        htmlViewerState.executeShutdownStart(processor);
    }

    public void sceUtilityHtmlViewerUpdate(Processor processor) {
    	htmlViewerState.executeUpdate(processor);
    }

    public void sceUtilityHtmlViewerGetStatus(Processor processor) {
        htmlViewerState.executeGetStatus(processor);
    }

    public void sceUtilityGamedataInstallInitStart(Processor processor) {
        gamedataInstallState.executeInitStart(processor);
    }

    public void sceUtilityGamedataInstallShutdownStart(Processor processor) {
        gamedataInstallState.executeShutdownStart(processor);
    }

    public void sceUtilityGamedataInstallUpdate(Processor processor) {
    	gamedataInstallState.executeUpdate(processor);
    }

    public void sceUtilityGamedataInstallGetStatus(Processor processor) {
        gamedataInstallState.executeGetStatus(processor);
    }

    public void sceUtilitySetSystemParamInt(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int id = cpu.gpr[4];
        int value_addr = cpu.gpr[5];

        if (!Memory.isAddressGood(value_addr)) {
            log.warn("sceUtilitySetSystemParamInt(id=" + id + ",value=0x" + Integer.toHexString(value_addr) + ") bad address");
            cpu.gpr[2] = -1;
        } else {
            log.debug("sceUtilitySetSystemParamInt(id=" + id + ",value=0x" + Integer.toHexString(value_addr) + ")");

            cpu.gpr[2] = 0;
            switch (id) {
                case PSP_SYSTEMPARAM_ID_INT_ADHOC_CHANNEL:
                    systemParam_adhocChannel = mem.read32(value_addr);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_WLAN_POWERSAVE:
                    systemParam_wlanPowersave = mem.read32(value_addr);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_DATE_FORMAT:
                    systemParam_dateFormat = mem.read32(value_addr);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_TIME_FORMAT:
                    systemParam_timeFormat = mem.read32(value_addr);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_TIMEZONE:
                    systemParam_timeZone = mem.read32(value_addr);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_DAYLIGHTSAVINGS:
                    systemParam_daylightSavingTime = mem.read32(value_addr);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_LANGUAGE:
                    systemParam_language = mem.read32(value_addr);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_BUTTON_PREFERENCE:
                    systemParam_buttonPreference = mem.read32(value_addr);
                    break;

                default:
                    log.warn("UNIMPLEMENTED: sceUtilitySetSystemParamInt(id=" + id + ",value=0x" + Integer.toHexString(value_addr) + ") unhandled id");
                    cpu.gpr[2] = -1;
                    break;
            }
        }
    }

    public void sceUtilitySetSystemParamString(Processor processor) {
        CpuState cpu = processor.cpu;

        int id = cpu.gpr[4];
        int str_addr = cpu.gpr[5];

        if (!Memory.isAddressGood(str_addr)) {
            log.warn("sceUtilitySetSystemParamString(id=" + id + ",str=0x" + Integer.toHexString(str_addr) + ") bad address");
            cpu.gpr[2] = -1;
        } else {
            log.debug("sceUtilitySetSystemParamString(id=" + id + ",str=0x" + Integer.toHexString(str_addr) + ")");

            cpu.gpr[2] = 0;
            switch (id) {
                case PSP_SYSTEMPARAM_ID_STRING_NICKNAME:
                    systemParam_nickname = Utilities.readStringZ(str_addr);
                    break;

                default:
                    log.warn("UNIMPLEMENTED: sceUtilitySetSystemParamString(id=" + id + ",str=0x" + Integer.toHexString(str_addr) + ") unhandled id");
                    cpu.gpr[2] = -1;
                    break;
            }
        }
    }

    public void sceUtilityGetSystemParamInt(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int id = cpu.gpr[4];
        int value_addr = cpu.gpr[5];

        if (!Memory.isAddressGood(value_addr)) {
            log.warn("sceUtilityGetSystemParamInt(id=" + id + ",value=0x" + Integer.toHexString(value_addr) + ") bad address");
            cpu.gpr[2] = -1;
        } else {
            log.debug("sceUtilityGetSystemParamInt(id=" + id + ",value=0x" + Integer.toHexString(value_addr) + ")");

            cpu.gpr[2] = 0;
            switch (id) {
                case PSP_SYSTEMPARAM_ID_INT_ADHOC_CHANNEL:
                    mem.write32(value_addr, systemParam_adhocChannel);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_WLAN_POWERSAVE:
                    mem.write32(value_addr, systemParam_wlanPowersave);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_DATE_FORMAT:
                    mem.write32(value_addr, systemParam_dateFormat);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_TIME_FORMAT:
                    mem.write32(value_addr, systemParam_timeFormat);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_TIMEZONE:
                    mem.write32(value_addr, systemParam_timeZone);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_DAYLIGHTSAVINGS:
                    mem.write32(value_addr, systemParam_daylightSavingTime);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_LANGUAGE:
                    mem.write32(value_addr, systemParam_language);
                    break;

                case PSP_SYSTEMPARAM_ID_INT_BUTTON_PREFERENCE:
                    mem.write32(value_addr, systemParam_buttonPreference);
                    break;

                default:
                    log.warn("UNIMPLEMENTED: sceUtilityGetSystemParamInt(id=" + id + ",value=0x" + Integer.toHexString(value_addr) + ") unhandled id");
                    cpu.gpr[2] = -1;
                    break;
            }
        }
    }

    public void sceUtilityGetSystemParamString(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int id = cpu.gpr[4];
        int str_addr = cpu.gpr[5];
        int len = cpu.gpr[6];

        if (!Memory.isAddressGood(str_addr)) {
            log.warn("sceUtilityGetSystemParamString(id=" + id + ",str=0x" + Integer.toHexString(str_addr) + ",len=" + len + ") bad address");
            cpu.gpr[2] = -1;
        } else {
            log.debug("sceUtilityGetSystemParamString(id=" + id + ",str=0x" + Integer.toHexString(str_addr) + ",len=" + len + ")");

            cpu.gpr[2] = 0;
            switch (id) {
                case PSP_SYSTEMPARAM_ID_STRING_NICKNAME:
                    Utilities.writeStringNZ(mem, str_addr, len, systemParam_nickname);
                    break;

                default:
                    log.warn("UNIMPLEMENTED:sceUtilityGetSystemParamString(id=" + id + ",str=0x" + Integer.toHexString(str_addr) + ",len=" + len + ") unhandled id");
                    cpu.gpr[2] = -1;
                    break;
            }
        }
    }

    /**
     * Check existance of a Net Configuration
     *
     * @param id - id of net Configuration (1 to n)
     * @return 0 on success,
     */
    public void sceUtilityCheckNetParam(Processor processor) {
        CpuState cpu = processor.cpu;

        int id = cpu.gpr[4];

        boolean available = (id >= 0 && id <= numberNetConfigurations);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceUtilityCheckNetParam(id=%d) available %b", id, available));
        }

        cpu.gpr[2] = available ? 0 : SceKernelErrors.ERROR_NETPARAM_BAD_NETCONF;
    }

    /**
     * Get Net Configuration Parameter
     *
     * @param conf - Net Configuration number (1 to n)
     * (0 returns valid but seems to be a copy of the last config requested)
     * @param param - which parameter to get
     * @param data - parameter data
     * @return 0 on success,
     */
    public void sceUtilityGetNetParam(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int id = cpu.gpr[4];
        int param = cpu.gpr[5];
        int data = cpu.gpr[6];

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceUtilityGetNetParam(id=%d, param=%d, data=0x%08X)", id, param, data));
        }

        if (id < 0 || id > numberNetConfigurations) {
        	log.warn(String.format("sceUtilityGetNetParam invalid id=%d", id));
        	cpu.gpr[2] = SceKernelErrors.ERROR_NETPARAM_BAD_NETCONF;
        } else if (!Memory.isAddressGood(data)) {
        	log.warn(String.format("sceUtilityGetNetParam invalid data address 0x%08X", data));
        	cpu.gpr[2] = -1;
        } else {
	        cpu.gpr[2] = 0;
	        switch (param) {
		        case PSP_NETPARAM_NAME: {
		        	Utilities.writeStringZ(mem, data, getNetParamName(id));
		        	break;
		        }
		        case PSP_NETPARAM_SSID: {
		        	Utilities.writeStringZ(mem, data, sceNetApctl.getSSID());
		        	break;
		        }
		        case PSP_NETPARAM_SECURE: {
		        	mem.write32(data, 1);
		        	break;
		        }
		        case PSP_NETPARAM_WEPKEY: {
		        	Utilities.writeStringZ(mem, data, "XXXXXXXXXXXXXXXXX");
		        	break;
		        }
		        case PSP_NETPARAM_IS_STATIC_IP: {
		        	mem.write32(data, 0);
		        	break;
		        }
		        case PSP_NETPARAM_IP: {
		        	Utilities.writeStringZ(mem, data, sceNetApctl.getLocalHostIP());
		        	break;
		        }
		        case PSP_NETPARAM_NETMASK: {
		        	Utilities.writeStringZ(mem, data, sceNetApctl.getSubnetMask());
		        	break;
		        }
		        case PSP_NETPARAM_ROUTE: {
		        	Utilities.writeStringZ(mem, data, sceNetApctl.getGateway());
		        	break;
		        }
		        case PSP_NETPARAM_MANUAL_DNS: {
		        	mem.write32(data, 0);
		        	break;
		        }
		        case PSP_NETPARAM_PRIMARYDNS: {
		        	Utilities.writeStringZ(mem, data, sceNetApctl.getPrimaryDNS());
		        	break;
		        }
		        case PSP_NETPARAM_SECONDARYDNS: {
		        	Utilities.writeStringZ(mem, data, sceNetApctl.getSecondaryDNS());
		        	break;
		        }
		        default: {
		        	log.warn(String.format("sceUtilityGetNetParam invalid data address 0x%08X", data));
		        	cpu.gpr[2] = SceKernelErrors.ERROR_NETPARAM_BAD_PARAM;
		        }
	        }
        }
    }

    /**
     * Get Current Net Configuration ID
     *
     * @param idAddr - Address to store the current net ID
     * @return 0 on success,
     */
    public void sceUtilityGetNetParamLatestID(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int idAddr = cpu.gpr[4];

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceUtilityGetNetParamLatestID: idAddr=0x%08X", idAddr));
        }

        if (Memory.isAddressGood(idAddr)) {
        	// TODO Check if this function returns the number of net configurations
        	// or the ID the latest selected net configuration
            mem.write32(idAddr, numberNetConfigurations);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_NETPARAM_BAD_PARAM;
        }
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
    public final HLEModuleFunction sceUtilitySavedataErrInitStartFunction = new HLEModuleFunction("sceUtility", "sceUtilitySavedataErrInitStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilitySavedataErrInitStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilitySavedataErrInitStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilitySavedataErrShutdownStartFunction = new HLEModuleFunction("sceUtility", "sceUtilitySavedataErrShutdownStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilitySavedataErrShutdownStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilitySavedataErrShutdownStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilitySavedataErrUpdateFunction = new HLEModuleFunction("sceUtility", "sceUtilitySavedataErrUpdate") {

        @Override
        public final void execute(Processor processor) {
            sceUtilitySavedataErrUpdate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilitySavedataErrUpdate(processor);";
        }
    };
    public final HLEModuleFunction sceUtilitySavedataErrGetStatusFunction = new HLEModuleFunction("sceUtility", "sceUtilitySavedataErrGetStatus") {

        @Override
        public final void execute(Processor processor) {
            sceUtilitySavedataErrGetStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilitySavedataErrGetStatus(processor);";
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
    public final HLEModuleFunction sceUtilityNpSigninInitStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityNpSigninInitStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityNpSigninInitStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityNpSigninInitStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityNpSigninShutdownStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityNpSigninShutdownStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityNpSigninShutdownStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityNpSigninShutdownStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityNpSigninUpdateFunction = new HLEModuleFunction("sceUtility", "sceUtilityNpSigninUpdate") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityNpSigninUpdate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityNpSigninUpdate(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityNpSigninGetStatusFunction = new HLEModuleFunction("sceUtility", "sceUtilityNpSigninGetStatus") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityNpSigninGetStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityNpSigninGetStatus(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityPS3ScanInitStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityPS3ScanInitStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityPS3ScanInitStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityPS3ScanInitStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityPS3ScanShutdownStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityPS3ScanShutdownStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityPS3ScanShutdownStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityPS3ScanShutdownStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityPS3ScanUpdateFunction = new HLEModuleFunction("sceUtility", "sceUtilityPS3ScanUpdate") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityPS3ScanUpdate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityPS3ScanUpdate(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityPS3ScanGetStatusFunction = new HLEModuleFunction("sceUtility", "sceUtilityPS3ScanGetStatus") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityPS3ScanGetStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityPS3ScanGetStatus(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityRssReaderInitStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityRssReaderInitStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityRssReaderInitStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityRssReaderInitStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityRssReaderContStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityRssReaderContStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityRssReaderContStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityRssReaderContStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityRssReaderShutdownStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityRssReaderShutdownStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityRssReaderShutdownStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityRssReaderShutdownStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityRssReaderUpdateFunction = new HLEModuleFunction("sceUtility", "sceUtilityRssReaderUpdate") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityRssReaderUpdate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityRssReaderUpdate(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityRssReaderGetStatusFunction = new HLEModuleFunction("sceUtility", "sceUtilityRssReaderGetStatus") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityRssReaderGetStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityRssReaderGetStatus(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityRssSubscriberInitStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityRssSubscriberInitStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityRssSubscriberInitStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityRssSubscriberInitStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityRssSubscriberShutdownStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityRssSubscriberShutdownStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityRssSubscriberShutdownStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityRssSubscriberShutdownStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityRssSubscriberUpdateFunction = new HLEModuleFunction("sceUtility", "sceUtilityRssSubscriberUpdate") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityRssSubscriberUpdate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityRssSubscriberUpdate(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityRssSubscriberGetStatusFunction = new HLEModuleFunction("sceUtility", "sceUtilityRssSubscriberGetStatus") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityRssSubscriberGetStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityRssSubscriberGetStatus(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityScreenshotInitStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityScreenshotInitStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityScreenshotInitStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityScreenshotInitStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityScreenshotContStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityScreenshotContStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityScreenshotContStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityScreenshotContStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityScreenshotShutdownStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityScreenshotShutdownStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityScreenshotShutdownStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityScreenshotShutdownStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityScreenshotUpdateFunction = new HLEModuleFunction("sceUtility", "sceUtilityScreenshotUpdate") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityScreenshotUpdate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityScreenshotUpdate(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityScreenshotGetStatusFunction = new HLEModuleFunction("sceUtility", "sceUtilityScreenshotGetStatus") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityScreenshotGetStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityScreenshotGetStatus(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityHtmlViewerInitStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityHtmlViewerInitStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityHtmlViewerInitStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityHtmlViewerInitStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityHtmlViewerShutdownStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityHtmlViewerShutdownStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityHtmlViewerShutdownStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityHtmlViewerShutdownStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityHtmlViewerUpdateFunction = new HLEModuleFunction("sceUtility", "sceUtilityHtmlViewerUpdate") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityHtmlViewerUpdate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityHtmlViewerUpdate(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityHtmlViewerGetStatusFunction = new HLEModuleFunction("sceUtility", "sceUtilityHtmlViewerGetStatus") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityHtmlViewerGetStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityHtmlViewerGetStatus(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityGamedataInstallInitStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityGamedataInstallInitStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityGamedataInstallInitStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityGamedataInstallInitStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityGamedataInstallShutdownStartFunction = new HLEModuleFunction("sceUtility", "sceUtilityGamedataInstallShutdownStart") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityGamedataInstallShutdownStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityGamedataInstallShutdownStart(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityGamedataInstallUpdateFunction = new HLEModuleFunction("sceUtility", "sceUtilityGamedataInstallUpdate") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityGamedataInstallUpdate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityGamedataInstallUpdate(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityGamedataInstallGetStatusFunction = new HLEModuleFunction("sceUtility", "sceUtilityGamedataInstallGetStatus") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityGamedataInstallGetStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityGamedataInstallGetStatus(processor);";
        }
    };
    public final HLEModuleFunction sceUtilityGetNetParamLatestIDFunction = new HLEModuleFunction("sceUtility", "sceUtilityGetNetParamLatestID") {

        @Override
        public final void execute(Processor processor) {
            sceUtilityGetNetParamLatestID(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceUtilityModule.sceUtilityGetNetParamLatestID(processor);";
        }
    };
}