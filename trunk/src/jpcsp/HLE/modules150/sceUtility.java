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
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Resource;
import jpcsp.Settings;
import jpcsp.State;
import jpcsp.Allegrex.CpuState;
import jpcsp.GUI.CancelButton;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.HLE.kernel.types.SceUtilityGameSharingParams;
import jpcsp.HLE.kernel.types.SceUtilityMsgDialogParams;
import jpcsp.HLE.kernel.types.SceUtilityNetconfParams;
import jpcsp.HLE.kernel.types.SceUtilityOskParams;
import jpcsp.HLE.kernel.types.SceUtilitySavedataParam;
import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.format.PSF;
import jpcsp.graphics.VideoEngine;
import jpcsp.hardware.MemoryStick;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceUtility implements HLEModule, HLEStartModule {
    protected static Logger log = Modules.getLogger("sceUtility");

	@Override
	public String getName() { return "sceUtility"; }

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
			mm.addFunction(0x2995D020, sceUtility_2995D020Function);
			mm.addFunction(0xB62A4061, sceUtility_B62A4061Function);
			mm.addFunction(0xED0FAD38, sceUtility_ED0FAD38Function);
			mm.addFunction(0x88BC7406, sceUtility_88BC7406Function);
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

	@Override
    public void start() {
		gameSharingState    = new UtilityDialogState("sceUtilityGameSharing");
        netplayDialogState  = new NotImplementedUtilityDialogState("sceNetplayDialog");
        netconfState        = new UtilityDialogState("sceUtilityNetconf");
		savedataState       = new UtilityDialogState("sceUtilitySavedata");
		msgDialogState      = new UtilityDialogState("sceUtilityMsgDialog");
		oskState            = new UtilityDialogState("sceUtilityOsk");
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

    public static final int PSP_UTILITY_DIALOG_STATUS_NONE = 0;
    public static final int PSP_UTILITY_DIALOG_STATUS_INIT = 1;
    public static final int PSP_UTILITY_DIALOG_STATUS_VISIBLE = 2;
    public static final int PSP_UTILITY_DIALOG_STATUS_QUIT = 3;
    public static final int PSP_UTILITY_DIALOG_STATUS_FINISHED = 4;

    public static final int PSP_UTILITY_DIALOG_RESULT_OK = 0;
    public static final int PSP_UTILITY_DIALOG_RESULT_CANCELED = 1;
    public static final int PSP_UTILITY_DIALOG_RESULT_ABORTED = 2;

    protected static final int maxLineLengthForDialog = 80;
    protected static final int[] fontHeightSavedataList = new int[] { 12, 12, 12, 12, 12, 12, 9, 8, 7, 6 };
    private static final String windowNameForSettings = "savedata";

    protected UtilityDialogState gameSharingState;
    protected SceUtilityGameSharingParams gameSharingParams;
    protected UtilityDialogState netplayDialogState;
    protected UtilityDialogState netconfState;
    protected SceUtilityNetconfParams netconfParams;
    protected UtilityDialogState savedataState;
    protected SceUtilitySavedataParam savedataParams;
    protected UtilityDialogState msgDialogState;
    protected SceUtilityMsgDialogParams msgDialogParams;
    protected UtilityDialogState oskState;
    protected SceUtilityOskParams oskParams;

    // TODO expose via settings GUI
    protected String systemParam_nickname = "JPCSP";
    protected int systemParam_adhocChannel = 0;
    protected int systemParam_wlanPowersave = 0;
    protected int systemParam_dateFormat = PSP_SYSTEMPARAM_DATE_FORMAT_YYYYMMDD;
    protected int systemParam_timeFormat = PSP_SYSTEMPARAM_TIME_FORMAT_24HR;
    protected int systemParam_timeZone = 0;
    protected int systemParam_daylightSavingTime = 0;
    protected int systemParam_language = PSP_SYSTEMPARAM_LANGUAGE_ENGLISH;
    protected int systemParam_buttonPreference = 0;

    // Save list vars.
    protected Object saveListSelection;
    protected boolean saveListSelected;

    protected static class UtilityDialogState {
    	protected String name;
    	private pspAbstractMemoryMappedStructure params;
    	private int paramsAddr;
    	private int status;
        private int result;
    	private boolean displayLocked;

    	public UtilityDialogState(String name) {
    		this.name = name;
    		status = PSP_UTILITY_DIALOG_STATUS_NONE;
            result = PSP_UTILITY_DIALOG_RESULT_OK;
    		displayLocked = false;
    	}

    	public void executeInitStart(Processor processor, pspAbstractMemoryMappedStructure params) {
    		CpuState cpu = processor.cpu;
    		Memory mem = Memory.getInstance();

    		paramsAddr = cpu.gpr[4];
    		if (!mem.isAddressGood(paramsAddr)) {
    			log.error(name + "InitStart bad address " + String.format("0x%08X", paramsAddr));
    			cpu.gpr[2] = -1;
    		} else {
    			this.params = params;

    			params.read(mem, paramsAddr);

	    		if (log.isInfoEnabled()) {
					log.info("PARTIAL:" + name + "InitStart " + params.toString());
				}

	            // Start with INIT
	    		status = PSP_UTILITY_DIALOG_STATUS_INIT;

	    		cpu.gpr[2] = 0;
    		}
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
            } else if (status == PSP_UTILITY_DIALOG_STATUS_INIT) {
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

    	public boolean tryUpdate(Processor processor) {
    		CpuState cpu = processor.cpu;

            int drawSpeed = cpu.gpr[4]; // FPS used for internal animation sync (1 = 60 FPS; 2 = 30 FPS; 3 = 15 FPS).
            if (log.isDebugEnabled()) {
                log.debug(name + "Update drawSpeed=" + drawSpeed);
            }

            boolean canDisplay = false;

            if (status == PSP_UTILITY_DIALOG_STATUS_INIT) {
            	// Move from INIT to VISIBLE
            	status = PSP_UTILITY_DIALOG_STATUS_VISIBLE;
            } else if (status == PSP_UTILITY_DIALOG_STATUS_VISIBLE) {
	            // A call to the GUI (JOptionPane) is only possible when the VideoEngine is not
	            // busy waiting on a sync: call JOptionPane only when the display is not locked.
            	while (true) {
		            canDisplay = Modules.sceDisplayModule.tryLockDisplay();
		            if (canDisplay) {
		            	displayLocked = true;
		            	break;
		            } else if (VideoEngine.getInstance().getCurrentList() == null) {
		            	// Check if the VideoEngine is not processing a list: in that case,
		            	// this could mean the display will be soon available for locking
		            	// (e.g. list processing is done, but still copying the graphics
		            	//  to PSP memory in sceDisplay.display()).
		            	// Wait a little bit and try again to lock the display.
		            	if (log.isDebugEnabled()) {
		            		log.debug(name + "Update : could not lock the display but VideoEngine not displayed, waiting a while...");
		            	}

		            	try {
		                    Thread.sleep(1);
		                } catch (InterruptedException e) {
		                	// Ignore exception
		                }
		            } else {
		            	if (log.isDebugEnabled()) {
		            		log.debug(name + "Update : could not lock the display");
		            	}
		            	break;
		            }
            	}
            }

            if (canDisplay) {
                // Some games reach sceUtilitySavedataInitStart with empty params which only
                // get filled with a subsquent call to sceUtilitySavedataUpdate (eg.: To Love-Ru).
                // This is why we have to re-read the params here.
            	params.read(Memory.getInstance(), paramsAddr);
            }

            cpu.gpr[2] = 0;

            return canDisplay;
    	}

    	public void endUpdate(Processor processor) {
    		if (displayLocked) {
    			Modules.sceDisplayModule.unlockDisplay();
    			displayLocked = false;
    		}

    		if (status == PSP_UTILITY_DIALOG_STATUS_VISIBLE) {
    			// Dialog has completed
    			status = PSP_UTILITY_DIALOG_STATUS_QUIT;
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
    }

    protected static class NotImplementedUtilityDialogState extends UtilityDialogState {
		public NotImplementedUtilityDialogState(String name) {
			super(name);
		}

		@Override
    	public void executeInitStart(Processor processor, pspAbstractMemoryMappedStructure params) {
			CpuState cpu = processor.cpu;

			log.warn("Unimplemented: " + name + "InitStart");

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
		public boolean tryUpdate(Processor processor) {
			CpuState cpu = processor.cpu;

			log.warn("Unimplemented: " + name + "Update");

			cpu.gpr[2] = SceKernelErrors.ERROR_UTILITY_IS_UNKNOWN;

			return false;
		}

		@Override
		public void endUpdate(Processor processor) {
			// Do nothing
		}
    }

    protected String formatMessageForDialog(String message) {
		StringBuilder formattedMessage = new StringBuilder();

		for (int i = 0; i < message.length(); ) {
			String rest = message.substring(i);
			if (rest.length() > maxLineLengthForDialog) {
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

    protected final class SavedataListTableColumnModel extends DefaultTableColumnModel {
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
        while((i = s.indexOf(find, i)) >= 0){
            count++;
            i = i + find.length();
        }
    	return count;
    }

    protected final class SavedataListTableModel extends AbstractTableModel {
		private static final long serialVersionUID = -8867168909834783380L;
		private int numberRows;
		private ImageIcon[] icons;
		private String[] descriptions;
		private int fontHeight = 12;

		public SavedataListTableModel(String[] saveNames) {
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

    protected void showSavedataList(final String[] saveNames) {
        final JDialog mainDisplay = new JDialog();
        mainDisplay.setTitle("Savedata List");
        mainDisplay.setSize(Settings.getInstance().readWindowSize(windowNameForSettings, 400, 401));
        mainDisplay.setLocation(Settings.getInstance().readWindowPos(windowNameForSettings));
        mainDisplay.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        final JButton selectButton = new JButton("Select");
        mainDisplay.getRootPane().setDefaultButton(selectButton);

        final JButton cancelButton = new CancelButton(mainDisplay);

        SavedataListTableModel savedataListTableModel = new SavedataListTableModel(saveNames);
        SavedataListTableColumnModel savedataListTableColumnModel = new SavedataListTableColumnModel();
        savedataListTableColumnModel.setFontHeight(savedataListTableModel.getFontHeight());
        final JTable table = new JTable(savedataListTableModel, savedataListTableColumnModel);
        table.setRowHeight(80);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
            	selectButton.setEnabled(!((ListSelectionModel)e.getSource()).isSelectionEmpty());
            }});
        table.setFont(new Font("SansSerif", Font.PLAIN, fontHeightSavedataList[0]));
        JScrollPane listScroll = new JScrollPane(table);

        GroupLayout layout = new GroupLayout(mainDisplay.getRootPane());
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(layout.createParallelGroup(
                GroupLayout.Alignment.TRAILING).addComponent(listScroll)
                .addGroup(
                layout.createSequentialGroup().addComponent(selectButton)
                        .addComponent(cancelButton)));

        layout.setVerticalGroup(layout.createSequentialGroup().addComponent(
        		listScroll).addGroup(
                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(selectButton).addComponent(cancelButton)));

		mainDisplay.getRootPane().setLayout(layout);
        mainDisplay.setVisible(true);

        saveListSelected = false;

        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if(table.getSelectedRow() != -1) {
                    saveListSelection = saveNames[table.getSelectedRow()];
                    mainDisplay.dispose();
                    saveListSelected = true;
                }
            }
        });

        // Wait for user selection.
        while(!saveListSelected) {
            if(!mainDisplay.isVisible())
                break;
        }

        Settings.getInstance().writeWindowPos(windowNameForSettings, mainDisplay.getLocation());
        Settings.getInstance().writeWindowSize(windowNameForSettings, mainDisplay.getSize());
    }

	public void sceUtilityGameSharingInitStart(Processor processor) {
        gameSharingParams = new SceUtilityGameSharingParams();
		gameSharingState.executeInitStart(processor, gameSharingParams);
	}

	public void sceUtilityGameSharingShutdownStart(Processor processor) {
		gameSharingState.executeShutdownStart(processor);
	}

	public void sceUtilityGameSharingUpdate(Processor processor) {
		if (gameSharingState.tryUpdate(processor)) {
			gameSharingState.endUpdate(processor);
		}
	}

	public void sceUtilityGameSharingGetStatus(Processor processor) {
		gameSharingState.executeGetStatus(processor);
	}

	public void sceNetplayDialogInitStart(Processor processor) {
		netplayDialogState.executeInitStart(processor, null);
	}

	public void sceNetplayDialogShutdownStart(Processor processor) {
		netplayDialogState.executeShutdownStart(processor);
	}

	public void sceNetplayDialogUpdate(Processor processor) {
		if (netplayDialogState.tryUpdate(processor)) {
			netplayDialogState.endUpdate(processor);
		}
	}

	public void sceNetplayDialogGetStatus(Processor processor) {
		netplayDialogState.executeGetStatus(processor);
	}

    public void sceUtilityNetconfInitStart(Processor processor) {
        netconfParams = new SceUtilityNetconfParams();
		netconfState.executeInitStart(processor, netconfParams);
	}

	public void sceUtilityNetconfShutdownStart(Processor processor) {
		netconfState.executeShutdownStart(processor);
	}

	public void sceUtilityNetconfUpdate(Processor processor) {
		if (netconfState.tryUpdate(processor)) {
			netconfState.endUpdate(processor);
		}
	}

	public void sceUtilityNetconfGetStatus(Processor processor) {
		netconfState.executeGetStatus(processor);
	}

	private int computeMemoryStickRequiredSpaceKb(int sizeByte) {
	    int sizeKb = (sizeByte + 1023) / 1024;
	    int sectorSizeKb = MemoryStick.getSectorSizeKb();
	    int numberSectors = (sizeKb + sectorSizeKb - 1) / sectorSizeKb;

	    return numberSectors * sectorSizeKb;
	}

    private boolean deleteSavedataDir(String saveName) {
        File saveDir = new File(saveName);
        if(saveDir.exists()) {
            File[] subFiles = saveDir.listFiles();
            for(int i = 0; i < subFiles.length; i++) {
                subFiles[i].delete();
            }
        }
        return (saveDir.delete());
    }

    private void hleUtilitySavedataDisplay() {
        Memory mem = Processor.memory;

        switch (savedataParams.mode) {
            case SceUtilitySavedataParam.MODE_AUTOLOAD:
            case SceUtilitySavedataParam.MODE_LOAD:
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
                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_LOAD_NO_DATA;
                    e.printStackTrace();
                }
                break;

            case SceUtilitySavedataParam.MODE_LISTLOAD:
                //Search for valid saves.
            	ArrayList<String> validNames = new ArrayList<String>();

                for(int i = 0; i < savedataParams.saveNameList.length; i++) {
                    savedataParams.saveName = savedataParams.saveNameList[i];

                    if(savedataParams.isPresent()) {
                        validNames.add(savedataParams.saveName);
                    }
                }

                showSavedataList(validNames.toArray(new String[validNames.size()]));
                if (saveListSelection == null) {
                    log.warn("Savedata MODE_LISTLOAD no save selected");
                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_LOAD_NO_DATA;
                } else {
                    savedataParams.saveName = saveListSelection.toString();
                    try {
                        savedataParams.load(mem);
                        savedataParams.base.result = 0;
                        savedataParams.write(mem);
                    } catch (IOException e) {
                        savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_LOAD_NO_DATA;
                    } catch (Exception e) {
                        savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_LOAD_NO_DATA;
                        e.printStackTrace();
                    }
                }
                break;

            case SceUtilitySavedataParam.MODE_AUTOSAVE:
            case SceUtilitySavedataParam.MODE_SAVE:
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
                    e.printStackTrace();
                }
                break;

            case SceUtilitySavedataParam.MODE_LISTSAVE:
                showSavedataList(savedataParams.saveNameList);
                if (saveListSelection == null) {
                    log.warn("Savedata MODE_LISTSAVE no save selected");
                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_SAVE_NO_MEMSTICK;
                }

                else {
                    savedataParams.saveName = saveListSelection.toString();
                    try {
                        savedataParams.save(mem);
                        savedataParams.base.result = 0;
                    } catch (IOException e) {
                        savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_SAVE_ACCESS_ERROR;
                    } catch (Exception e) {
                        savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_SAVE_ACCESS_ERROR;
                        e.printStackTrace();
                    }
                }
                break;

            case SceUtilitySavedataParam.MODE_DELETE:
                if(savedataParams.saveNameList != null) {
                    for(int i = 0; i < savedataParams.saveNameList.length; i++) {
                        String save = "ms0/PSP/SAVEDATA/" + (State.discId) +
                                (savedataParams.saveNameList[i]);
                        if(deleteSavedataDir(save)) {
                            log.debug("Savedata MODE_DELETE deleting " + save);
                        }
                    }
                    savedataParams.base.result = 0;
                } else {
                    log.warn("Savedata MODE_DELETE no saves found!");
                   savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_LOAD_NO_DATA;
                }
                break;

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
                log.warn("PARTIAL:Savedata mode 8 (SCE_UTILITY_SAVEDATA_TYPE_SIZES)");
                String gameName = savedataParams.gameName;
                String saveName = savedataParams.saveName;

                // ms free size
                int buffer1Addr = savedataParams.msFreeAddr;
                if (mem.isAddressGood(buffer1Addr)) {
                    String memoryStickFreeSpaceString = MemoryStick.getSizeKbString(MemoryStick.getFreeSizeKb());

                    mem.write32(buffer1Addr +  0, MemoryStick.getSectorSize());
                    mem.write32(buffer1Addr +  4, MemoryStick.getFreeSizeKb() / MemoryStick.getSectorSizeKb());
                    mem.write32(buffer1Addr +  8, MemoryStick.getFreeSizeKb());
                    Utilities.writeStringNZ(mem, buffer1Addr +  12, 8, memoryStickFreeSpaceString);

                    log.debug("Memory Stick Free Space = " + memoryStickFreeSpaceString);
                }

                // ms data size
                int buffer2Addr = savedataParams.msDataAddr;
                if (mem.isAddressGood(buffer2Addr)) {
                    gameName = Utilities.readStringNZ(mem, buffer2Addr, 13);
                    saveName = Utilities.readStringNZ(mem, buffer2Addr + 16, 20);
                    int savedataSizeKb = savedataParams.getSize(gameName, saveName);
                    int savedataSize32Kb = MemoryStick.getSize32Kb(savedataSizeKb);

                    mem.write32(buffer2Addr + 36, savedataSizeKb / MemoryStick.getSectorSizeKb()); // Number of sectors
                    mem.write32(buffer2Addr + 40, savedataSizeKb); // Size in Kb
                    Utilities.writeStringNZ(mem, buffer2Addr + 44, 8, MemoryStick.getSizeKbString(savedataSizeKb));
                    mem.write32(buffer2Addr + 52, savedataSize32Kb);
                    Utilities.writeStringNZ(mem, buffer2Addr + 56, 8, MemoryStick.getSizeKbString(savedataSize32Kb));
                }

                // utility data size
                int buffer3Addr = savedataParams.utilityDataAddr;
                if (mem.isAddressGood(buffer3Addr)) {
                    int memoryStickRequiredSpaceKb = 0;
                    memoryStickRequiredSpaceKb += MemoryStick.getSectorSizeKb(); // Assume 1 sector for SFO-Params
                    memoryStickRequiredSpaceKb += computeMemoryStickRequiredSpaceKb(savedataParams.dataSize);
                    memoryStickRequiredSpaceKb += computeMemoryStickRequiredSpaceKb(savedataParams.icon0FileData.size);
                    memoryStickRequiredSpaceKb += computeMemoryStickRequiredSpaceKb(savedataParams.icon1FileData.size);
                    memoryStickRequiredSpaceKb += computeMemoryStickRequiredSpaceKb(savedataParams.pic1FileData.size);
                    memoryStickRequiredSpaceKb += computeMemoryStickRequiredSpaceKb(savedataParams.snd0FileData.size);
                    String memoryStickRequiredSpaceString = MemoryStick.getSizeKbString(memoryStickRequiredSpaceKb);
                    int memoryStickRequiredSpace32Kb = MemoryStick.getSize32Kb(memoryStickRequiredSpaceKb);
                    String memoryStickRequiredSpace32KbString = MemoryStick.getSizeKbString(memoryStickRequiredSpace32Kb);

                    mem.write32(buffer3Addr +  0, memoryStickRequiredSpaceKb / MemoryStick.getSectorSizeKb());
                    mem.write32(buffer3Addr +  4, memoryStickRequiredSpaceKb);
                    Utilities.writeStringNZ(mem, buffer3Addr +  8, 8, memoryStickRequiredSpaceString);
                    mem.write32(buffer3Addr + 16, memoryStickRequiredSpace32Kb);
                    Utilities.writeStringNZ(mem, buffer3Addr + 20, 8, memoryStickRequiredSpace32KbString);

                    log.debug("Memory Stick Required Space = " + memoryStickRequiredSpaceString);
                }

            	if (savedataParams.isPresent(gameName, saveName)) {
                    savedataParams.base.result = 0;
            	} else {
                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_SIZES_NO_DATA;
            	}
                break;
            }

            case SceUtilitySavedataParam.MODE_LIST: {
                log.debug("Savedata mode 11");
                int buffer4Addr = savedataParams.idListAddr;
                if (mem.isAddressGood(buffer4Addr)) {
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
                if (mem.isAddressGood(buffer5Addr)) {
                    int saveFileSecureEntriesAddr = mem.read32(buffer5Addr + 24);
                    int saveFileEntriesAddr = mem.read32(buffer5Addr + 28);
                    int systemEntriesAddr = mem.read32(buffer5Addr + 32);

                    String path = savedataParams.getBasePath(savedataParams.saveName);
                	String[] entries = Modules.IoFileMgrForUserModule.listFiles(path, null);

                	int maxNumEntries = entries == null ? 0 : entries.length;
                    int saveFileSecureNumEntries = 0;
                    int saveFileNumEntries = 0;
                    int systemFileNumEntries = 0;

                    // List all files in the savedata (normal and/or encrypted).
                	for (int i = 0; i < maxNumEntries; i++) {
                        String filePath = path + "/" + entries[i];
                        SceIoStat stat = Modules.IoFileMgrForUserModule.statFile(filePath);

                        // System files.
                        if(filePath.contains(".SFO") || filePath.contains("ICON")
                                || filePath.contains("PIC") || filePath.contains("SND")) {
                            if(mem.isAddressGood(systemEntriesAddr)) {
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
                            if(mem.isAddressGood(saveFileSecureEntriesAddr)) {
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

                            if(mem.isAddressGood(saveFileEntriesAddr)) {
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
                }
        		savedataParams.base.result = 0;
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
                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_RW_FILE_NOT_FOUND;
                } catch (Exception e) {
                    savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_RW_NO_DATA;
                    e.printStackTrace();
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
                    e.printStackTrace();
                }
                break;
            }

            case SceUtilitySavedataParam.MODE_DELETEDATA:
                // Sub-type of mode DELETE.
                // Deletes the contents of only one specified file.
                if(savedataParams.fileName != null) {
                    String save = "ms0/PSP/SAVEDATA/" + State.discId + savedataParams.saveName + "/" + savedataParams.fileName;
                    File f = new File(save);

                    if(f != null) {
                        log.debug("Savedata MODE_DELETEDATA deleting " + save);
                        f = new File(save);
                        f.delete();
                    }
                    savedataParams.base.result = 0;
                } else {
                    log.warn("Savedata MODE_DELETEDATA no data found!");
                   savedataParams.base.result = SceKernelErrors.ERROR_SAVEDATA_LOAD_NO_DATA;
                }
                break;

            case SceUtilitySavedataParam.MODE_GETSIZE:
                int buffer6Addr = savedataParams.sizeAddr;
                if (mem.isAddressGood(buffer6Addr)) {
                    int saveFileSecureNumEntries = mem.read32(buffer6Addr + 0);
                    int saveFileNumEntries = mem.read32(buffer6Addr + 4);
                    int saveFileSecureEntriesAddr = mem.read32(buffer6Addr + 8);
                    int saveFileEntriesAddr = mem.read32(buffer6Addr + 12);

                    int totalSize = 0;
                    int neededSize = 0;
                    int freeSize = MemoryStick.getFreeSizeKb();

                    for(int i = 0; i < saveFileSecureNumEntries; i++) {
                        int entryAddr = saveFileSecureEntriesAddr + i * 24;
                        long size = mem.read64(entryAddr);
                        String fileName = Utilities.readStringNZ(entryAddr + 8, 16);

                        totalSize += size;
                    }
                    for(int i = 0; i < saveFileNumEntries; i++) {
                        int entryAddr = saveFileEntriesAddr + i * 24;
                        long size = mem.read64(entryAddr);
                        String fileName = Utilities.readStringNZ(entryAddr + 8, 16);

                        totalSize += size;
                    }

                    // If there's not enough size, we have to write how much size we need.
                    // With enough size, our needed size is always 0.
                    if(totalSize > freeSize) {
                        neededSize = totalSize;
                    }

                    // Free MS size.
                	String memoryStickFreeSpaceString = MemoryStick.getSizeKbString(freeSize);
                    mem.write32(buffer6Addr +  16, MemoryStick.getSectorSize());
                    mem.write32(buffer6Addr +  20, freeSize / MemoryStick.getSectorSizeKb());
                    mem.write32(buffer6Addr +  24, freeSize);
                    Utilities.writeStringNZ(mem, buffer6Addr +  28, 8, memoryStickFreeSpaceString);

                    // Size needed to write savedata.
                    mem.write32(buffer6Addr +  36, neededSize);
                    Utilities.writeStringNZ(mem, buffer6Addr +  40, 8, neededSize + " KB");

                    // Size needed to overwrite savedata.
                    mem.write32(buffer6Addr +  48, neededSize);
                    Utilities.writeStringNZ(mem, buffer6Addr +  52, 8, neededSize + " KB");

                }
        		savedataParams.base.result = 0;
                break;

            default:
                log.warn("Savedata - Unsupported mode " + savedataParams.mode);
                savedataParams.base.result = -1;
                break;
        }

        savedataParams.base.writeResult(mem);
        if (log.isDebugEnabled()) {
            log.debug("hleUtilitySavedataDisplay savedResult:0x" + Integer.toHexString(savedataParams.base.result));
        }
    }

	public void sceUtilitySavedataInitStart(Processor processor) {
        savedataParams = new SceUtilitySavedataParam();
        savedataState.executeInitStart(processor, savedataParams);
    }

    public void sceUtilitySavedataShutdownStart(Processor processor) {
    	savedataState.executeShutdownStart(processor);
    }

    public void sceUtilitySavedataUpdate(Processor processor) {
    	if (savedataState.tryUpdate(processor)) {
    		hleUtilitySavedataDisplay();

    		savedataState.endUpdate(processor);
    	}
	}

    public void sceUtilitySavedataGetStatus(Processor processor) {
    	savedataState.executeGetStatus(processor);
    }

	public void sceUtility_2995D020(Processor processor) {
		CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceUtility_2995D020 [0x2995D020]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceUtility_B62A4061(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceUtility_B62A4061 [0xB62A4061]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceUtility_ED0FAD38(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceUtility_ED0FAD38 [0xED0FAD38]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void sceUtility_88BC7406(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceUtility_88BC7406 [0x88BC7406]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

    protected void hleUtilityMsgDialogDisplay() {
        Memory mem = Processor.memory;

        String title = String.format("Message from %s", State.title);
        if (msgDialogParams.isOptionYesNo()) {
            int result = JOptionPane.showConfirmDialog(null, formatMessageForDialog(msgDialogParams.message), null, JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                msgDialogParams.buttonPressed = 1;
            } else if (result == JOptionPane.NO_OPTION) {
                msgDialogParams.buttonPressed = 2;
            } else if (result == JOptionPane.CANCEL_OPTION) {
                msgDialogParams.buttonPressed = 3;
            }
        } else if (msgDialogParams.mode == SceUtilityMsgDialogParams.PSP_UTILITY_MSGDIALOG_MODE_TEXT) {
            JOptionPane.showMessageDialog(null, formatMessageForDialog(msgDialogParams.message), title, JOptionPane.INFORMATION_MESSAGE);
        }
        msgDialogParams.base.result = 0;
        msgDialogParams.write(mem);
    }

    public void sceUtilityMsgDialogInitStart(Processor processor) {
        msgDialogParams = new SceUtilityMsgDialogParams();
        msgDialogState.executeInitStart(processor, msgDialogParams);
    }

    public void sceUtilityMsgDialogShutdownStart(Processor processor) {
    	msgDialogState.executeShutdownStart(processor);
    }

	public void sceUtilityMsgDialogUpdate(Processor processor) {
		if (msgDialogState.tryUpdate(processor)) {
			hleUtilityMsgDialogDisplay();
			msgDialogState.endUpdate(processor);
		}
	}

    public void sceUtilityMsgDialogGetStatus(Processor processor) {
    	msgDialogState.executeGetStatus(processor);
    }

    protected void hleUtilityOskDisplay() {
        Memory mem = Processor.memory;

        oskParams.oskData.outText = JOptionPane.showInputDialog(oskParams.oskData.desc, oskParams.oskData.inText);
        oskParams.base.result = 0;
        oskParams.oskData.result = SceUtilityOskParams.PSP_UTILITY_OSK_STATE_INITIALIZED;
        oskParams.write(mem);
        log.info("hleUtilityOskDisplay returning '" + oskParams.oskData.outText + "'");
    }

	public void sceUtilityOskInitStart(Processor processor) {
        oskParams = new SceUtilityOskParams();
        oskState.executeInitStart(processor, oskParams);
	}

	public void sceUtilityOskShutdownStart(Processor processor) {
		oskState.executeShutdownStart(processor);
	}

	public void sceUtilityOskUpdate(Processor processor) {
		if (oskState.tryUpdate(processor)) {
			hleUtilityOskDisplay();
			oskState.endUpdate(processor);
		}
	}

	public void sceUtilityOskGetStatus(Processor processor) {
		oskState.executeGetStatus(processor);
	}

	public void sceUtilitySetSystemParamInt(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

        int id = cpu.gpr[4];
        int value_addr = cpu.gpr[5];

        if (!mem.isAddressGood(value_addr)) {
            log.warn("sceUtilitySetSystemParamInt(id=" + id + ",value=0x" + Integer.toHexString(value_addr) + ") bad address");
            cpu.gpr[2] = -1;
        } else {
            log.debug("sceUtilitySetSystemParamInt(id=" + id + ",value=0x" + Integer.toHexString(value_addr) + ")");

            cpu.gpr[2] = 0;
            switch(id) {
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
		Memory mem = Processor.memory;

        int id = cpu.gpr[4];
        int str_addr = cpu.gpr[5];

        if (!mem.isAddressGood(str_addr)) {
            log.warn("sceUtilitySetSystemParamString(id=" + id + ",str=0x" + Integer.toHexString(str_addr) + ") bad address");
            cpu.gpr[2] = -1;
        } else {
            log.debug("sceUtilitySetSystemParamString(id=" + id + ",str=0x" + Integer.toHexString(str_addr) + ")");

            cpu.gpr[2] = 0;
            switch(id) {
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

        if (!mem.isAddressGood(value_addr)) {
            log.warn("sceUtilityGetSystemParamInt(id=" + id + ",value=0x" + Integer.toHexString(value_addr) + ") bad address");
            cpu.gpr[2] = -1;
        } else {
            log.debug("PARTIAL:sceUtilityGetSystemParamInt(id=" + id + ",value=0x" + Integer.toHexString(value_addr) + ")");

            cpu.gpr[2] = 0;
            switch(id) {
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

        if (!mem.isAddressGood(str_addr)) {
            log.warn("sceUtilityGetSystemParamString(id=" + id + ",str=0x" + Integer.toHexString(str_addr) + ",len=" + len + ") bad address");
            cpu.gpr[2] = -1;
        } else {
            log.debug("PARTIAL:sceUtilityGetSystemParamString(id=" + id + ",str=0x" + Integer.toHexString(str_addr) + ",len=" + len + ")");

            cpu.gpr[2] = 0;
            switch(id) {
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

	public void sceUtilityCheckNetParam(Processor processor) {
		CpuState cpu = processor.cpu;

		int id = cpu.gpr[4];

		log.warn("IGNORING: sceUtilityCheckNetParam(id=" + id + ")");

		cpu.gpr[2] = 0;
	}

	public void sceUtilityGetNetParam(Processor processor) {
		CpuState cpu = processor.cpu;

        int id = cpu.gpr[4];
        int param = cpu.gpr[5];
        int net_addr = cpu.gpr[6];

		log.warn("IGNORING: sceUtilityGetNetParam(id=" + id + ", param=" + param + ", net_addr="
                + Integer.toHexString(net_addr)+ ")");

		cpu.gpr[2] = 0;
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
}