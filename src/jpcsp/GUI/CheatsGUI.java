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
import java.awt.Point;
import javax.swing.JOptionPane;

import jpcsp.HLE.ThreadMan;

public class CheatsGUI extends javax.swing.JFrame implements KeyListener {
    private javax.swing.JButton jButtonInsert;
    private javax.swing.JButton jButtonRemove;
    private javax.swing.JRadioButton jRadioONOFF;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;

    private String code = "";
    private String codeType = "";
    private static CheatsGUI instance;

    public CheatsGUI(String type) {
        initComponents();
        setTitle("Cheats - " + type);
        codeType = type;
        instance = this;
    }

    @Override
    public void keyTyped(KeyEvent arg0) { }

    @Override
    public void keyReleased(KeyEvent arg0) { }

    @Override
    public void keyPressed(KeyEvent arg0) {
    }

    private void initComponents() {
        jButtonInsert = new javax.swing.JButton();
        jButtonRemove = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jRadioONOFF = new javax.swing.JRadioButton();

        setTitle("Cheats");
        setResizable(false);

        jButtonInsert.setText("Insert");
        jButtonInsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonInsertActionPerformed(evt);
            }
        });

        jButtonRemove.setText("Remove");
        jButtonRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRemoveActionPerformed(evt);
            }
        });

        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setFont(new java.awt.Font("Monospaced", 1, 14));
        jTextArea1.setRows(5);
        jTextArea1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextArea1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jTextArea1);
        jTextArea1.getAccessibleContext().setAccessibleName("cheatPane");

        jRadioONOFF.setText("On/Off");
        jRadioONOFF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioONOFFActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jRadioONOFF)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 63, Short.MAX_VALUE)
                        .addComponent(jButtonInsert, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButtonRemove, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 314, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 229, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButtonRemove)
                            .addComponent(jButtonInsert)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jRadioONOFF)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }

    public static CheatsGUI getInstance() {
        return instance;
    }

    public String getCodeType() {
        return codeType;
    }

    public boolean checkCWCheatFormat(String text) {
        return(text.charAt(0) == '0' && text.charAt(1) == 'x' &&
                text.charAt(11) == '0' && text.charAt(12) == 'x' &&
                text.length() == 21);
    }

    public String[] getCheats() {
        String text = jTextArea1.getText();
        String[] codes = text.split("\n");

        return codes;
    }

    private void jButtonRemoveActionPerformed(java.awt.event.ActionEvent evt) {
        if(jTextArea1.getSelectedText() != null) {
            if(!jTextArea1.getSelectedText().equals(""))
                jTextArea1.replaceRange(null, jTextArea1.getSelectionStart(), jTextArea1.getSelectionEnd());
        }
    }

    private void jButtonInsertActionPerformed(java.awt.event.ActionEvent evt) {
        code = JOptionPane.showInputDialog(this, "Input your code: ");
        if(code != null) {
            if(checkCWCheatFormat(code)) {
                if(jTextArea1.getText().equals(""))
                    jTextArea1.append(code);
                else
                    jTextArea1.append("\n" + code);
            }
            else
                JOptionPane.showMessageDialog(this, "Invalid input! CWCheat format is: '0x12345678 0x12345678'", "Input error", JOptionPane.ERROR_MESSAGE, null);
        }
    }

    private void jRadioONOFFActionPerformed(java.awt.event.ActionEvent evt) {
        if(!jTextArea1.getText().equals("")) {
            ThreadMan.getInstance().try_hook_cheat_thread();
        }
    }

    private void jTextArea1MouseClicked(java.awt.event.MouseEvent evt) {
        try {
            Point pos = jTextArea1.getMousePosition();
            int posY = (int)pos.getY();
            int lineTop = 0;
            int lineBottom = 18;
            int line = 0;

            for(int i = 0; i < jTextArea1.getLineCount(); i++) {
                if(posY > lineTop && posY <= lineBottom) {
                    line = i;
                    break;
                }
                lineTop += 18;
                lineBottom += 18;
            }
            jTextArea1.select(jTextArea1.getLineStartOffset(line), jTextArea1.getLineEndOffset(line));
        } catch (Exception e) {
            // Nothing to do.
        }
    }
}