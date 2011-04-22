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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import jpcsp.Memory;
import jpcsp.MemoryMap;

public class CheatsGUI extends javax.swing.JFrame implements KeyListener {

    private static final long serialVersionUID = 6791588139795694296L;

    private static class CheatsThread extends Thread {

        public CheatsThread() {
        }

        @Override
        public void run() {
            Memory mem = Memory.getInstance();
            CheatsGUI cheats = CheatsGUI.getInstance();

            while (cheats.isON()) {
                if (cheats.getCodeType().equals("CWCheat")) {
                    String[] codes = cheats.getCodesList();
                    int comm = 0;
                    int arg = 0;

                    for (int i = 0; i < codes.length; i++) {
                        comm = (int)Long.parseLong(codes[i].split(" ")[0].substring(2), 16);
                        arg = (int)Long.parseLong(codes[i].split(" ")[1].substring(2), 16);

                        if ((comm >> 28) == 0) { // 8-bit write.
                            int addr = MemoryMap.START_USERSPACE + comm;
                            if (Memory.isAddressGood(addr)) {
                                mem.write8(addr, (byte) arg);
                            }
                        } else if ((comm >> 28) == 0x1) { // 16-bit write.
                            int addr = MemoryMap.START_USERSPACE + (comm - 0x10000000);
                            if (Memory.isAddressGood(addr)) {
                                mem.write16(addr, (short) arg);
                            }
                        } else if ((comm >> 28) == 0x2) { // 32-bit write.
                            int addr = MemoryMap.START_USERSPACE + (comm - 0x20000000);
                            if (Memory.isAddressGood(addr)) {
                                mem.write32(addr, arg);
                            }
                        } else if ((comm >> 8) == 0x301000) { // 8-bit increment.
                            int addr = MemoryMap.START_USERSPACE + arg;
                            int data = (comm & 0xFF);
                            if (Memory.isAddressGood(addr)) {
                                byte tmp = (byte) (mem.read8(addr) + data);
                                mem.write8(addr, tmp);
                            }
                        } else if ((comm >> 8) == 0x302000) { // 8-bit decrement.
                            int addr = MemoryMap.START_USERSPACE + arg;
                            int data = (comm & 0xFF);
                            if (Memory.isAddressGood(addr)) {
                                byte tmp = (byte) (mem.read8(addr) - data);
                                mem.write8(addr, tmp);
                            }
                        } else if ((comm >> 16) == 0x3030) { // 16-bit increment.
                            int addr = MemoryMap.START_USERSPACE + arg;
                            int data = (comm & 0xFFFF);
                            if (Memory.isAddressGood(addr)) {
                                short tmp = (short) (mem.read16(addr) + data);
                                mem.write16(addr, tmp);
                            }
                        } else if ((comm >> 16) == 0x3040) { // 16-bit decrement.
                            int addr = MemoryMap.START_USERSPACE + arg;
                            int data = (comm & 0xFFFF);
                            if (Memory.isAddressGood(addr)) {
                                short tmp = (short) (mem.read16(addr) - data);
                                mem.write16(addr, tmp);
                            }
                        } else if (comm == 0x30500000) { // 32-bit increment.
                            int addr = MemoryMap.START_USERSPACE + arg;
                            int data = (int)Long.parseLong(codes[i + 1].split(" ")[0].substring(2), 16);
                            if (Memory.isAddressGood(addr)) {
                                int tmp = (mem.read32(addr) + data);
                                mem.write32(addr, tmp);
                            }
                        } else if (comm == 0x30600000) { // 32-bit decrement.
                            int addr = MemoryMap.START_USERSPACE + arg;
                            int data = (int)Long.parseLong(codes[i + 1].split(" ")[0].substring(2), 16);
                            if (Memory.isAddressGood(addr)) {
                                int tmp = (mem.read32(addr) - data);
                                mem.write32(addr, tmp);
                            }
                        } else if ((comm >> 28) == 0x7) { // Boolean commands.
                            int addr = MemoryMap.START_USERSPACE + (comm - 0x70000000);
                            if ((arg >> 16) == 0x0) { // 8-bit OR.
                                if (Memory.isAddressGood(addr)) {
                                    byte val1 = (byte) (arg & 0xFF);
                                    byte val2 = (byte) mem.read8(addr);
                                    mem.write8(addr, (byte) (val1 | val2));
                                }
                            } else if ((arg >> 16) == 0x2) { // 8-bit AND.
                                if (Memory.isAddressGood(addr)) {
                                    byte val1 = (byte) (arg & 0xFF);
                                    byte val2 = (byte) mem.read8(addr);
                                    mem.write8(addr, (byte) (val1 & val2));
                                }
                            } else if ((arg >> 16) == 0x4) { // 8-bit XOR.
                                if (Memory.isAddressGood(addr)) {
                                    byte val1 = (byte) (arg & 0xFF);
                                    byte val2 = (byte) mem.read8(addr);
                                    mem.write8(addr, (byte) (val1 ^ val2));
                                }
                            } else if ((arg >> 16) == 0x1) { // 16-bit OR.
                                if (Memory.isAddressGood(addr)) {
                                    short val1 = (short) (arg & 0xFFFF);
                                    short val2 = (short) mem.read16(addr);
                                    mem.write16(addr, (short) (val1 | val2));
                                }
                            } else if ((arg >> 16) == 0x3) { // 16-bit AND.
                                if (Memory.isAddressGood(addr)) {
                                    short val1 = (short) (arg & 0xFFFF);
                                    short val2 = (short) mem.read16(addr);
                                    mem.write16(addr, (short) (val1 & val2));
                                }
                            } else if ((arg >> 16) == 0x5) { // 16-bit XOR.
                                if (Memory.isAddressGood(addr)) {
                                    short val1 = (short) (arg & 0xFFFF);
                                    short val2 = (short) mem.read16(addr);
                                    mem.write16(addr, (short) (val1 ^ val2));
                                }
                            }
                        } else if ((comm >> 28) == 0x5) { // Memcpy command.
                            int srcAddr = MemoryMap.START_USERSPACE + (comm - 0x50000000);
                            int destAddr = (int)Long.parseLong(codes[i + 1].split(" ")[0].substring(2), 16);
                            if (Memory.isAddressGood(srcAddr) && Memory.isAddressGood(destAddr)) {
                                mem.memcpy(destAddr, srcAddr, arg);
                            }
                        } else if ((comm >> 28) == 0x4) { // 32-bit patch code.
                            int addr = MemoryMap.START_USERSPACE + (comm - 0x40000000);
                            int data = (int)Long.parseLong(codes[i + 1].split(" ")[0].substring(2), 16);
                            int dataAdd = (int)Long.parseLong(codes[i + 1].split(" ")[1].substring(2), 16);

                            int maxAddr = (arg >> 16) & 0xFFFF;
                            int stepAddr = (arg & 0xFFFF) * 4;
                            for (int a = 0; a < maxAddr; a++) {
                                if (Memory.isAddressGood(addr)) {
                                    mem.write32(addr, data);
                                }
                                addr += stepAddr;
                                data += dataAdd;
                            }
                        } else if ((comm >> 28) == 0x8) { // 8-bit and 16-bit patch code.
                            int addr = MemoryMap.START_USERSPACE + (comm - 0x40000000);
                            int data = (int)Long.parseLong(codes[i + 1].split(" ")[0].substring(2), 16);
                            int dataAdd = (int)Long.parseLong(codes[i + 1].split(" ")[1].substring(2), 16);

                            int maxAddr = (arg >> 16) & 0xFFFF;
                            int stepAddr = (arg & 0xFFFF) * 4;
                            for (int a = 0; a < maxAddr; a++) {
                                if (Memory.isAddressGood(addr)) {
                                    if ((data >> 16) == 0x1000) {
                                        mem.write16(addr, (short) (data & 0xFFFF));
                                    } else {
                                        mem.write8(addr, (byte) (data & 0xFF));
                                    }
                                }
                                addr += stepAddr;
                                data += dataAdd;
                            }
                        }
                    }
                }
            }
        }
    }

    private javax.swing.JButton jButtonClear;
    private javax.swing.JRadioButton jRadioONOFF;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    private String codeType = "";
    private boolean toggle = false;
    private static CheatsGUI instance;
    private Thread cheatsThread = null;

    public CheatsGUI(String type) {
        initComponents();
        setTitle("Cheats - " + type);
        codeType = type;
        instance = this;
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
        jButtonClear = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jRadioONOFF = new javax.swing.JRadioButton();

        setTitle("Cheats");
        setResizable(false);

        jButtonClear.setText("Clear");
        jButtonClear.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearActionPerformed(evt);
            }
        });

        jTextArea1.setColumns(20);
        jTextArea1.setEditable(true);
        jTextArea1.setFont(new java.awt.Font("Monospaced", 1, 14));
        jTextArea1.setRows(5);

        jScrollPane1.setViewportView(jTextArea1);
        jTextArea1.getAccessibleContext().setAccessibleName("cheatPane");

        jRadioONOFF.setText("On/Off");
        jRadioONOFF.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioONOFFActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(jRadioONOFF).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 63, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jButtonClear, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 314, Short.MAX_VALUE)).addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 229, javax.swing.GroupLayout.PREFERRED_SIZE).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addGap(18, 18, 18).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jButtonClear))).addGroup(layout.createSequentialGroup().addGap(6, 6, 6).addComponent(jRadioONOFF))).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        pack();
    }

    public static CheatsGUI getInstance() {
        return instance;
    }

    public String getCodeType() {
        return codeType;
    }

    public boolean isON() {
        return toggle;
    }

    public boolean checkCWCheatFormat(String text) {
        return (text.charAt(0) == '0' && text.charAt(1) == 'x' &&
                text.charAt(11) == '0' && text.charAt(12) == 'x' &&
                text.length() == 21);
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
        if (cheatsThread == null && !jTextArea1.getText().equals("")) {
            jTextArea1.setEditable(false);
            cheatsThread = new CheatsThread();
            cheatsThread.setPriority(Thread.MIN_PRIORITY);
            cheatsThread.setName("HLECheatThread");
            cheatsThread.start();
        }
        if (!toggle) {
            jTextArea1.setEditable(false);
            cheatsThread.run();
            toggle = true;
        } else {
            jTextArea1.setEditable(true);
            Thread.yield();
            toggle = false;
        }
    }
}