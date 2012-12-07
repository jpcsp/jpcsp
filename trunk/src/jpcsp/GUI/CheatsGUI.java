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

import static jpcsp.util.Utilities.parseHexLong;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.State;
import jpcsp.util.Utilities;

public class CheatsGUI extends javax.swing.JFrame implements KeyListener {
    private static final long serialVersionUID = 6791588139795694296L;
    private static final int cheatsThreadSleepMillis = 5;

    private static class CheatsThread extends Thread {
    	private String[] codes;
    	private int currentCode;
    	private final CheatsGUI cheats;
    	private volatile boolean exit;

        public CheatsThread(CheatsGUI cheats) {
        	this.cheats = cheats;
        }

        public void exit() {
        	exit = true;
        }

        private String getNextCode() {
        	String code;

        	while (true) {
        		if (currentCode >= codes.length) {
        			code = null;
        			break;
        		}

        		code = codes[currentCode++].trim();

            	if (code.startsWith("_L")) {
            		code = code.substring(2).trim();
            		break;
            	} else if (code.startsWith("0")) {
            		break;
            	}
        	}

        	return code;
        }

        private void skipCodes(int count) {
        	for (int i = 0; i < count; i++) {
        		if (getNextCode() == null) {
        			break;
        		}
        	}
        }

        private void skipAllCodes() {
        	currentCode = codes.length;
        }

        private static int getAddress(int value) {
        	// The User space base address has to be added to given value
        	return (value + MemoryMap.START_USERSPACE) & Memory.addressMask;
        }

        @Override
        public void run() {
            Memory mem = Memory.getInstance();

            while (!exit) {
            	// Sleep a little bit to not use the CPU at 100%
            	Utilities.sleep(cheatsThreadSleepMillis, 0);

                codes = cheats.getCodesList();

                currentCode = 0;
                while (true) {
                	String code = getNextCode();
                	if (code == null) {
                		break;
                	}

                	String[] parts = code.split(" ");
                	if (parts == null || parts.length < 2) {
                		continue;
                	}

                	int value;
                	int comm = (int) parseHexLong(parts[0].trim());
                    int arg = (int) parseHexLong(parts[1].trim());
                    int addr = getAddress(comm & 0x0FFFFFFF);

                    switch (comm >>> 28) {
                    	case 0: // 8-bit write.
                    		if (Memory.isAddressGood(addr)) {
                    			mem.write8(addr, (byte) arg);
                    		}
                    		break;
                    	case 0x1: // 16-bit write.
                    		if (Memory.isAddressGood(addr)) {
                    			mem.write16(addr, (short) arg);
                    		}
                    		break;
                    	case 0x2: // 32-bit write.
                    		if (Memory.isAddressGood(addr)) {
                    			mem.write32(addr, arg);
                    		}
                    		break;
                    	case 0x3: // Increment/Decrement
                    		addr = getAddress(arg);
                    		value = 0;
                    		int increment = 0;
                    		// Read value from memory
                    		switch ((comm >> 20) & 0xF) {
                    			case 1:
                    			case 2: // 8-bit
                    				value = mem.read8(addr);
                    				increment = comm & 0xFF;
                    				break;
                    			case 3:
                    			case 4: // 16-bit
                    				value = mem.read16(addr);
                    				increment = comm & 0xFFFF;
                    				break;
                    			case 5:
                    			case 6: // 32-bit
                    				value = mem.read32(addr);
                                    code = getNextCode();
                                    parts = code.split(" ");
                                    if (parts != null && parts.length >= 1) {
                                    	increment = (int) parseHexLong(parts[0].trim());
                                    }
                    				break;
                    		}
                    		// Increment/Decrement value
                    		switch ((comm >> 20) & 0xF) {
                    			case 1:
                    			case 3:
                    			case 5: // Increment
                    				value += increment;
                    				break;
                    			case 2:
                    			case 4:
                    			case 6: // Decrement
                    				value -= increment;
                    				break;
                    		}
                    		// Write value back to memory
                    		switch ((comm >> 20) & 0xF) {
                    			case 1:
                    			case 2: // 8-bit
                    				mem.write8(addr, (byte) value);
                    				break;
                    			case 3:
                    			case 4: // 16-bit
                    				mem.write16(addr, (short) value);
                    				break;
                    			case 5:
                    			case 6: // 32-bit
                    				mem.write32(addr, value);
                    				break;
                    		}
                    		break;
                    	case 0x4: // 32-bit patch code.
                    		code = getNextCode();
                            parts = code.split(" ");
                            if (parts != null && parts.length >= 1) {
                            	int data = (int) parseHexLong(parts[0].trim());
                            	int dataAdd = (int) parseHexLong(parts[1].trim());

                            	int maxAddr = (arg >> 16) & 0xFFFF;
                                int stepAddr = (arg & 0xFFFF) * 4;
                                for (int a = 0; a < maxAddr; a++) {
                                    if (Memory.isAddressGood(addr)) {
                                        mem.write32(addr, data);
                                    }
                                    addr += stepAddr;
                                    data += dataAdd;
                                }
                            }
                            break;
                    	case 0x5: // Memcpy command.
                    		code = getNextCode();
                            parts = code.split(" ");
                            if (parts != null && parts.length >= 1) {
                            	int destAddr = (int) parseHexLong(parts[0].trim());
                                if (Memory.isAddressGood(addr) && Memory.isAddressGood(destAddr)) {
                                    mem.memcpy(destAddr, addr, arg);
                                }
                            }
                            break;
                    	case 0x6: // Pointer commands
                    		code = getNextCode();
                            parts = code.split(" ");
                            if (parts != null && parts.length >= 2) {
                            	int arg2 = (int) parseHexLong(parts[0].trim());
                            	int offset = (int) parseHexLong(parts[1].trim());
                            	int baseOffset = (arg2 >>> 20) * 4;
                            	int base = mem.read32(addr + baseOffset);
                            	int count = arg2 & 0xFFFF;
                            	int type = (arg2 >> 16) & 0xF;
                            	for (int i = 1; i < count; i++) {
                            		if (i + 1 < count) {
                            			code = getNextCode();
        	                            parts = code.split(" ");
        	                            if (parts != null && parts.length >= 2) {
        	                            	int arg3 = (int) parseHexLong(parts[0].trim());
        	                            	int arg4 = (int) parseHexLong(parts[1].trim());
        	                            	int comm3 = arg3 >>> 28;
        	                            	switch (comm3) {
        	                            		case 0x1: // type copy byte
        	                            			int srcAddr = mem.read32(addr) + offset;
        	                            			int dstAddr = mem.read32(addr + baseOffset) + (arg3 & 0x0FFFFFFF);
        	                            			mem.memcpy(dstAddr, srcAddr, arg);
        	                            			type = -1; // Done
        	                            			break;
        	                            		case 0x2:
        	                            		case 0x3: // type pointer walk
        	                            			int walkOffset = arg3 & 0x0FFFFFFF;
        	                            			if (comm3 == 0x3) {
        	                            				walkOffset = -walkOffset;
        	                            			}
        	                            			base = mem.read32(base + walkOffset);
        	                            			int comm4 = arg4 >>> 28;
        	                            			switch (comm4) {
        	                            				case 0x2:
        	                            				case 0x3: // type pointer walk
        	                            					walkOffset = arg4 & 0x0FFFFFFF;
        	                            					if (comm4 == 0x3) {
        	                            						walkOffset = -walkOffset;
        	                            					}
        	                            					base = mem.read32(base + walkOffset);
        	                            					break;
        	                            			}
        	                            			break;
        	                            		case 0x9: // type multi address write
        	                            			base += arg3 & 0x0FFFFFFF;
        	                            			arg += arg4; // CHECKME Not sure about this?
        	                            			break;
        	                            	}
        	                            }
                            		}
                            	}

                            	switch (type) {
                        			case 0: // 8-bit write
                        				mem.write8(base + offset, (byte) arg);
                        				break;
                        			case 1: // 16-bit write
                        				mem.write16(base + offset, (short) arg);
                        				break;
                        			case 2: // 32-bit write
                        				mem.write32(base + offset, arg);
                        				break;
                        			case 3: // 8-bit inverse write
                        				mem.write8(base - offset, (byte) arg);
                        				break;
                        			case 4: // 16-bit inverse write
                        				mem.write16(base - offset, (short) arg);
                        				break;
                        			case 5: // 32-bit inverse write
                        				mem.write32(base - offset, arg);
                        				break;
                        			case -1: // Operation already performed, nothing to do
                        				break;
                        		}
                            }
                    		break;
                    	case 0x7: // Boolean commands.
                            switch (arg >> 16) {
	                            case 0x0000: // 8-bit OR.
	                                if (Memory.isAddressGood(addr)) {
	                                    byte val1 = (byte) (arg & 0xFF);
	                                    byte val2 = (byte) mem.read8(addr);
	                                    mem.write8(addr, (byte) (val1 | val2));
	                                }
	                                break;
	                            case 0x0002: // 8-bit AND.
	                                if (Memory.isAddressGood(addr)) {
	                                    byte val1 = (byte) (arg & 0xFF);
	                                    byte val2 = (byte) mem.read8(addr);
	                                    mem.write8(addr, (byte) (val1 & val2));
	                                }
	                                break;
	                            case 0x0004: // 8-bit XOR.
	                                if (Memory.isAddressGood(addr)) {
	                                    byte val1 = (byte) (arg & 0xFF);
	                                    byte val2 = (byte) mem.read8(addr);
	                                    mem.write8(addr, (byte) (val1 ^ val2));
	                                }
	                                break;
	                            case 0x0001: // 16-bit OR.
	                                if (Memory.isAddressGood(addr)) {
	                                    short val1 = (short) (arg & 0xFFFF);
	                                    short val2 = (short) mem.read16(addr);
	                                    mem.write16(addr, (short) (val1 | val2));
	                                }
	                                break;
	                            case 0x0003: // 16-bit AND.
	                                if (Memory.isAddressGood(addr)) {
	                                    short val1 = (short) (arg & 0xFFFF);
	                                    short val2 = (short) mem.read16(addr);
	                                    mem.write16(addr, (short) (val1 & val2));
	                                }
	                                break;
	                            case 0x0005: // 16-bit XOR.
	                                if (Memory.isAddressGood(addr)) {
	                                    short val1 = (short) (arg & 0xFFFF);
	                                    short val2 = (short) mem.read16(addr);
	                                    mem.write16(addr, (short) (val1 ^ val2));
	                                }
	                                break;
                            }
                            break;
                    	case 0x8: // 8-bit and 16-bit patch code.
                    		code = getNextCode();
                            parts = code.split(" ");
                            if (parts != null && parts.length >= 1) {
                            	int data = (int) parseHexLong(parts[0].trim());
                            	int dataAdd = (int) parseHexLong(parts[1].trim());

                            	boolean is8Bit = (data >> 16) == 0x0000;
                            	int maxAddr = (arg >> 16) & 0xFFFF;
                                int stepAddr = (arg & 0xFFFF) * (is8Bit ? 1 : 2);
                                for (int a = 0; a < maxAddr; a++) {
                                    if (Memory.isAddressGood(addr)) {
                                    	if (is8Bit) {
                                    		mem.write8(addr, (byte) (data & 0xFF));
                                    	} else {
                                    		mem.write16(addr, (short) (data & 0xFFFF));
                                    	}
                                    }
                                    addr += stepAddr;
                                    data += dataAdd;
                                }
                            }
                            break;
                    	case 0xB: // Time command
                    		// CHECKME Not sure what to do for this code?
                    		break;
                    	case 0xC: // Code stopper
                    		if (Memory.isAddressGood(addr)) {
                    			value = mem.read32(addr);
                    			if (value != arg) {
                    				skipAllCodes();
                    			}
                    		}
                    		break;
                    	case 0xD: // Test commands & Jocker codes
                    		switch (arg >>> 28) {
                    			case 0:
                    			case 2: // Test commands, single skip
		                        	boolean is8Bit = (arg >> 28) == 0x2;
		                        	if (Memory.isAddressGood(addr)) {
		                        		int memoryValue = is8Bit ? mem.read8(addr) : mem.read16(addr);
		                        		int testValue = arg & (is8Bit ? 0xFF : 0xFFFF);
		                        		boolean executeNextLine = false;
		                        		switch ((arg >> 20) & 0xF) {
		                        			case 0x0: // Equal
		                        				executeNextLine = memoryValue == testValue;
		                        				break;
		                        			case 0x1: // Not Equal
		                        				executeNextLine = memoryValue != testValue;
		                        				break;
		                        			case 0x2: // Less Than
		                        				executeNextLine = memoryValue < testValue;
		                        				break;
		                        			case 0x3: // Greater Than
		                        				executeNextLine = memoryValue > testValue;
		                        				break;
		                        		}
		                        		if (!executeNextLine) {
		                        			skipCodes(1);
		                        		}
		                        	}
		                        	break;
                    			case 4:
                    			case 5:
                    			case 6:
                    			case 7: // Address Test commands
		                        	int addr1 = addr;
		                        	int addr2 = getAddress(arg & 0x0FFFFFFF);
		                        	if (Memory.isAddressGood(addr1) && Memory.isAddressGood(addr2)) {
			                            code = getNextCode();
			                            parts = code.split(" ");
			                            if (parts != null && parts.length >= 1) {
			                            	int skip = (int) parseHexLong(parts[0].trim());
			                            	int type = (int) parseHexLong(parts[1].trim());
			                            	int value1 = 0;
			                            	int value2 = 0;
			                            	switch (type & 0xF) {
			                            		case 0: // 8 bit
			                            			value1 = mem.read8(addr1);
			                            			value2 = mem.read8(addr2);
			                            			break;
			                            		case 1: // 16 bit
			                            			value1 = mem.read16(addr1);
			                            			value2 = mem.read16(addr2);
			                            			break;
			                            		case 2: // 32 bit
			                            			value1 = mem.read32(addr1);
			                            			value2 = mem.read32(addr2);
			                            			break;
			                            	}
			                            	boolean executeNextLines = false;
			                            	switch (arg >>> 28) {
			                            		case 4: // Equal
			                            			executeNextLines = value1 == value2;
			                            			break;
			                            		case 5: // Not Equal
			                            			executeNextLines = value1 != value2;
			                            			break;
			                            		case 6: // Less Than
			                            			executeNextLines = value1 < value2;
			                            			break;
			                            		case 7: // Greater Than
			                            			executeNextLines = value1 > value2;
			                            			break;
			                            	}
			                            	if (!executeNextLines) {
			                            		skipCodes(skip);
			                            	}
			                            }
		                        	}
		                        	break;
                    			case 1: // Joker code
                    			case 3: // Inverse Joker code
                    				int testButtons = arg & 0x0FFFFFFF;
                    				int buttons = jpcsp.State.controller.getButtons();
                    				boolean executeNextLines = false;
                    				if ((arg >>> 28) == 1) {
                    					executeNextLines = testButtons == buttons;
                    				} else {
                    					executeNextLines = testButtons != buttons;
                    				}
                    				if (!executeNextLines) {
                        				int skip = (comm & 0xFF) + 1;
                    					skipCodes(skip);
                    				}
                    				break;
                    		}
                    		break;
                    	case 0xE: // Test commands, multiple skip
                        	boolean is8Bit = (comm >> 24) == 0x1;
                        	addr = getAddress(arg & 0x0FFFFFFF);
                        	if (Memory.isAddressGood(addr)) {
                        		int memoryValue = is8Bit ? mem.read8(addr) : mem.read16(addr);
                        		int testValue = comm & (is8Bit ? 0xFF : 0xFFFF);
                        		boolean executeNextLines = false;
                        		switch (arg >>> 28) {
                        			case 0x0: // Equal
                        				executeNextLines = memoryValue == testValue;
                        				break;
                        			case 0x1: // Not Equal
                        				executeNextLines = memoryValue != testValue;
                        				break;
                        			case 0x2: // Less Than
                        				executeNextLines = memoryValue < testValue;
                        				break;
                        			case 0x3: // Greater Than
                        				executeNextLines = memoryValue > testValue;
                        				break;
                        		}
                        		if (!executeNextLines) {
                        			int skip = (comm >> 16) & (is8Bit ? 0xFF : 0xFFF);
                        			skipCodes(skip);
                        		}
                        	}
                        	break;
                    }
                }
            }

            // Exiting...
            cheats.onCheatsThreadEnded();
        }
    }

    private JButton jButtonClear;
    private JRadioButton jRadioONOFF;
    private JScrollPane jScrollPane1;
    private JTextArea jTextArea1;
    private JButton jButtonImportFromCheatDB;
    private boolean cheatsOn = false;
    private CheatsThread cheatsThread = null;

    public CheatsGUI() {
        initComponents();
        setTitle("Cheats - CWCheat");
    }

    @Override
    public void keyTyped(KeyEvent arg0) {
    }

    @Override
    public void keyReleased(KeyEvent arg0) {
    }

    @Override
    public void keyPressed(KeyEvent arg0) {
    }

    private void initComponents() {
        jButtonClear = new JButton();
        jScrollPane1 = new JScrollPane();
        jTextArea1 = new JTextArea();
        jRadioONOFF = new JRadioButton();
        jButtonImportFromCheatDB = new JButton();

        setTitle("Cheats");
        setResizable(true);

        jButtonClear.setText("Clear");
        jButtonClear.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearActionPerformed(evt);
            }
        });

        jTextArea1.setColumns(30);
        jTextArea1.setEditable(true);
        jTextArea1.setFont(new java.awt.Font("Monospaced", 1, 14));
        jTextArea1.setRows(20);

        jScrollPane1.setViewportView(jTextArea1);
        jTextArea1.getAccessibleContext().setAccessibleName("cheatPane");

        jRadioONOFF.setText("On/Off");
        jRadioONOFF.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioONOFFActionPerformed(evt);
            }
        });

        jButtonImportFromCheatDB.setText("Import from cheat.db");
        jButtonImportFromCheatDB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				jButtonImportFromCheatDBActionPerformed(evt);
			}
		});

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup().addContainerGap()
                		.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                				.addGroup(layout.createSequentialGroup()
                						.addComponent(jRadioONOFF)
                						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 63, Short.MAX_VALUE)
                						.addComponent(jButtonImportFromCheatDB)
                						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 63, Short.MAX_VALUE)
                						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                						.addComponent(jButtonClear, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE))
        						.addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 314, Short.MAX_VALUE))
						.addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                		.addContainerGap()
                		.addComponent(jScrollPane1)
                		.addGap(6)
                		.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                				.addComponent(jButtonClear)
                				.addComponent(jRadioONOFF)
                				.addComponent(jButtonImportFromCheatDB))
						.addContainerGap()));
        pack();
    }

    public String[] getCodesList() {
        String text = jTextArea1.getText();
        String[] codes = text.split("\n");

        return codes;
    }

    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {
        jTextArea1.setText("");
    }

    private void jRadioONOFFActionPerformed(java.awt.event.ActionEvent evt) {
        cheatsOn = !cheatsOn;
        jTextArea1.setEditable(!cheatsOn);

        if (cheatsOn && cheatsThread == null && !jTextArea1.getText().equals("")) {
            jTextArea1.setEditable(false);
            cheatsThread = new CheatsThread(this);
            cheatsThread.setPriority(Thread.MIN_PRIORITY);
            cheatsThread.setName("HLECheatThread");
            cheatsThread.setDaemon(true);
            cheatsThread.start();
        } else if (!cheatsOn && cheatsThread != null) {
        	cheatsThread.exit();
        }
    }

    private void addCheatLine(String line) {
    	String cheatCodes = jTextArea1.getText();
    	if (cheatCodes == null || cheatCodes.length() <= 0) {
    		cheatCodes = line;
    	} else {
    		cheatCodes += "\n" + line;
    	}
    	jTextArea1.setText(cheatCodes);
    }

    private void jButtonImportFromCheatDBActionPerformed(ActionEvent evt) {
    	File cheatDBFile = new File("cheat.db");
    	if (cheatDBFile.canRead()) {
    		try {
				BufferedReader reader = new BufferedReader(new FileReader(cheatDBFile));
				boolean insideApplicationid = false;
				while (reader.ready()) {
					String line = reader.readLine();
					if (line == null) {
						// end of file
						break;
					}
					line = line.trim();
					if (line.startsWith("_S ")) {
						String applicationId = line.substring(2).trim().replace("-", "");
						insideApplicationid = applicationId.equalsIgnoreCase(State.discId);
					}
					if (insideApplicationid) {
						// Add the line to the cheat codes
						addCheatLine(line);
					}
				}
				reader.close();
			} catch (IOException e) {
				Emulator.log.error("Import from cheat.db", e);
			}
    	}
    }

    public void onCheatsThreadEnded() {
    	cheatsThread = null;
    }

    @Override
	public void dispose() {
    	if (cheatsThread != null) {
    		cheatsThread.exit();
    	}

    	Emulator.getMainGUI().endWindowDialog();
		super.dispose();
	}
}
