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
package jpcsp.Debugger;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import jpcsp.Emulator;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules150.sceDisplay;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.WindowPropSaver;
import jpcsp.graphics.GeCommands;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.ImageReader;

public class ImageViewer extends javax.swing.JFrame {
    
    private static final long serialVersionUID = 8837780642045065242L;
    private int startAddress = MemoryMap.START_VRAM;
    private int bufferWidth = 512;
    private int imageWidth = 480;
    private int imageHeight = 272;
    private boolean imageSwizzle = false;
    private boolean useAlpha = false;
    private int backgroundColor = 0;
    private int pixelFormat = GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
    private int clutAddress = 0;
    private int clutNumberBlocks = 32;
    private int clutFormat = GeCommands.CMODE_FORMAT_32BIT_ABGR8888;
    private int clutStart = 0;
    private int clutShift = 0;
    private int clutMask = 0xFF;
    private static final Color[] backgroundColors = new Color[]{
        Color.WHITE,
        Color.BLACK,
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.GRAY
    };
    
    public ImageViewer() {
        // memoryImage construction overriden for MemoryImage
        initComponents();
        copyValuesToFields();
        
        WindowPropSaver.loadWindowProperties(this);
    }
    
    public void refreshImage() {
        goToAddress();
    }
    
    private void valuesUpdated() {
        memoryImage.setSize(memoryImage.getPreferredSize());
        repaint();
    }
    
    private void goToAddress() {
        try {
            startAddress = Integer.decode(addressField.getText());
            imageWidth = Integer.decode(widthField.getText());
            imageHeight = Integer.decode(heightField.getText());
            bufferWidth = Integer.decode(bufferWidthField.getText());
            clutAddress = Integer.decode(clutAddressField.getText());
            clutNumberBlocks = Integer.decode(clutNumberBlocksField.getText());
        } catch (NumberFormatException nfe) {
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp");
            JOptionPane.showMessageDialog(this, bundle.getString("ImageViewer.strInvalidNumber.text") + " " + nfe.getLocalizedMessage());
            return;
        }
        
        pixelFormat = pixelFormatField.getSelectedIndex();
        imageSwizzle = swizzleField.isSelected();
        useAlpha = useAlphaField.isSelected();
        backgroundColor = backgroundColorField.getSelectedIndex();
        clutFormat = clutFormatField.getSelectedIndex();

        // clean UI strings before updating
        copyValuesToFields();
        valuesUpdated();
    }
    
    private void copyValuesToFields() {
        addressField.setText(String.format("0x%08X", startAddress));
        widthField.setText(String.format("%d", imageWidth));
        heightField.setText(String.format("%d", imageHeight));
        bufferWidthField.setText(String.format("%d", bufferWidth));
        pixelFormatField.setSelectedIndex(pixelFormat);
        swizzleField.setSelected(imageSwizzle);
        useAlphaField.setSelected(useAlpha);
        backgroundColorField.setSelectedIndex(backgroundColor);
        clutAddressField.setText(String.format("0x%08X", clutAddress));
        clutNumberBlocksField.setText(String.format("%d", clutNumberBlocks));
        clutFormatField.setSelectedIndex(clutFormat);
    }
    
    private void goToBufferInfo(sceDisplay.BufferInfo bufferInfo) {
        startAddress = bufferInfo.topAddr;
        imageWidth = bufferInfo.width;
        imageHeight = bufferInfo.height;
        bufferWidth = bufferInfo.bufferWidth;
        pixelFormat = bufferInfo.pixelFormat;
        imageSwizzle = false;
        useAlpha = false;
        
        copyValuesToFields();
        valuesUpdated();
    }
    
    @Override
    public void dispose() {
        Emulator.getMainGUI().endWindowDialog();
        super.dispose();
    }
    
    private class MemoryImage extends JPanel {
        
        private static final long serialVersionUID = 1372183323503668615L;
        
        public MemoryImage() {
        }
        
        @Override
        public void paintComponent(Graphics g) {
            if (Memory.isAddressGood(startAddress)) {
                Insets insets = getInsets();
                int minWidth = Math.min(imageWidth, bufferWidth);
                
                g.setColor(backgroundColors[backgroundColor]);
                g.fillRect(insets.left, insets.top, minWidth, imageHeight);
                
                IMemoryReader imageReader = ImageReader.getImageReader(startAddress, imageWidth, imageHeight, bufferWidth, pixelFormat, imageSwizzle, clutAddress, clutFormat, clutNumberBlocks, clutStart, clutShift, clutMask, null, null);
                
                for (int y = 0; y < imageHeight; y++) {
                    for (int x = 0; x < minWidth; x++) {
                        int colorABGR = imageReader.readNext();
                        int colorARGB = ImageReader.colorABGRtoARGB(colorABGR);
                        g.setColor(new Color(colorARGB, useAlpha));
                        
                        drawPixel(g, x + insets.left, y + insets.top);
                    }
                }
            }
        }
        
        private void drawPixel(Graphics g, int x, int y) {
            g.drawLine(x, y, x, y);
        }
        
        @Override
        public Dimension getPreferredSize() {
            Insets insets = getInsets();
            return new Dimension(imageWidth + insets.left + insets.right, imageHeight + insets.top + insets.bottom);
        }
        
        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        addressField = new javax.swing.JTextField();
        lblWidth = new javax.swing.JLabel();
        widthField = new javax.swing.JTextField();
        lblHeight = new javax.swing.JLabel();
        heightField = new javax.swing.JTextField();
        lblBufferWidth = new javax.swing.JLabel();
        bufferWidthField = new javax.swing.JTextField();
        lblAddress = new javax.swing.JLabel();
        lblPixelFormat = new javax.swing.JLabel();
        pixelFormatField = new javax.swing.JComboBox();
        swizzleField = new javax.swing.JCheckBox();
        lblCLUT = new javax.swing.JLabel();
        clutAddressField = new javax.swing.JTextField();
        lblCLUTNumberBlocks = new javax.swing.JLabel();
        clutNumberBlocksField = new javax.swing.JTextField();
        lblCLUTFormat = new javax.swing.JLabel();
        clutFormatField = new javax.swing.JComboBox();
        lblBackgroundColor = new javax.swing.JLabel();
        backgroundColorField = new javax.swing.JComboBox();
        btnGoToAddress = new javax.swing.JButton();
        btnGoToGE = new javax.swing.JButton();
        btnGoToFB = new javax.swing.JButton();
        useAlphaField = new javax.swing.JCheckBox();
        memoryImage = new MemoryImage();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
        setTitle(bundle.getString("ImageViewer.title")); // NOI18N
        setMinimumSize(new java.awt.Dimension(532, 500));

        addressField.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        addressField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        addressField.setText("0x00000000"); // NOI18N
        addressField.setToolTipText(bundle.getString("ImageViewer.addressField.toolTipText")); // NOI18N
        addressField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                ImageViewer.this.keyPressed(evt);
            }
        });

        lblWidth.setText(bundle.getString("ImageViewer.lblWidth.text")); // NOI18N

        widthField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        widthField.setText("480"); // NOI18N
        widthField.setToolTipText(bundle.getString("ImageViewer.widthField.toolTipText")); // NOI18N
        widthField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                ImageViewer.this.keyPressed(evt);
            }
        });

        lblHeight.setText(bundle.getString("ImageViewer.lblHeight.text")); // NOI18N

        heightField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        heightField.setText("272"); // NOI18N
        heightField.setToolTipText(bundle.getString("ImageViewer.heightField.toolTipText")); // NOI18N
        heightField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                ImageViewer.this.keyPressed(evt);
            }
        });

        lblBufferWidth.setText(bundle.getString("ImageViewer.lblBufferWidth.text")); // NOI18N

        bufferWidthField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        bufferWidthField.setText("512"); // NOI18N
        bufferWidthField.setToolTipText(bundle.getString("ImageViewer.bufferWidthField.toolTipText")); // NOI18N
        bufferWidthField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                ImageViewer.this.keyPressed(evt);
            }
        });

        lblAddress.setText(bundle.getString("ImageViewer.lblAddress.text")); // NOI18N

        lblPixelFormat.setText(bundle.getString("ImageViewer.lblPixelFormat.text")); // NOI18N

        pixelFormatField.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "565", "5551", "4444", "8888", "Indexed 4", "Indexed 8", "Indexed 16", "Indexed 32", "DXT1", "DXT3", "DXT5" }));
        pixelFormatField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeImageActionPerformed(evt);
            }
        });

        swizzleField.setText(bundle.getString("ImageViewer.swizzleField.text")); // NOI18N
        swizzleField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeImageActionPerformed(evt);
            }
        });

        lblCLUT.setText(bundle.getString("ImageViewer.lblCLUT.text")); // NOI18N

        clutAddressField.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        clutAddressField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        clutAddressField.setText("0x00000000"); // NOI18N
        clutAddressField.setToolTipText(bundle.getString("ImageViewer.clutAddressField.toolTipText")); // NOI18N
        clutAddressField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                ImageViewer.this.keyPressed(evt);
            }
        });

        lblCLUTNumberBlocks.setText(bundle.getString("ImageViewer.lblCLUTNumberBlocks.text")); // NOI18N

        clutNumberBlocksField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        clutNumberBlocksField.setText("32"); // NOI18N
        clutNumberBlocksField.setToolTipText(bundle.getString("ImageViewer.clutNumberBlocksField.toolTipText")); // NOI18N
        clutNumberBlocksField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                ImageViewer.this.keyPressed(evt);
            }
        });

        lblCLUTFormat.setText(bundle.getString("ImageViewer.lblCLUTFormat.text")); // NOI18N

        clutFormatField.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "565", "5551", "4444", "8888" }));
        clutFormatField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeImageActionPerformed(evt);
            }
        });

        lblBackgroundColor.setText(bundle.getString("ImageViewer.lblBackgroundColor.text")); // NOI18N

        backgroundColorField.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "White", "Black", "Red", "Green", "Blue", "Gray" }));
        backgroundColorField.setSelectedItem("Black");
        backgroundColorField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeImageActionPerformed(evt);
            }
        });

        btnGoToAddress.setText(bundle.getString("ImageViewer.btnGoToAddress.text")); // NOI18N
        btnGoToAddress.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGoToAddressActionPerformed(evt);
            }
        });

        btnGoToGE.setText(bundle.getString("ImageViewer.btnGoToGE.text")); // NOI18N
        btnGoToGE.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGoToGEActionPerformed(evt);
            }
        });

        btnGoToFB.setText(bundle.getString("ImageViewer.btnGoToFB.text")); // NOI18N
        btnGoToFB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGoToFBActionPerformed(evt);
            }
        });

        useAlphaField.setText(bundle.getString("ImageViewer.useAlphaField.text")); // NOI18N
        useAlphaField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeImageActionPerformed(evt);
            }
        });

        memoryImage.setBackground(new java.awt.Color(0, 0, 0));
        memoryImage.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 0, 0), 10));

        javax.swing.GroupLayout memoryImageLayout = new javax.swing.GroupLayout(memoryImage);
        memoryImage.setLayout(memoryImageLayout);
        memoryImageLayout.setHorizontalGroup(
            memoryImageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 480, Short.MAX_VALUE)
        );
        memoryImageLayout.setVerticalGroup(
            memoryImageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 272, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lblPixelFormat)
                                            .addComponent(lblAddress)
                                            .addComponent(lblCLUT))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                            .addComponent(addressField, javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(clutAddressField)
                                            .addComponent(pixelFormatField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(lblBackgroundColor)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(backgroundColorField, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(26, 26, 26)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addGroup(layout.createSequentialGroup()
                                                        .addComponent(swizzleField)
                                                        .addGap(18, 18, 18))
                                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                        .addComponent(widthField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addGap(35, 35, 35)))
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(useAlphaField)
                                                    .addGroup(layout.createSequentialGroup()
                                                        .addGap(10, 10, 10)
                                                        .addComponent(heightField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(lblBufferWidth)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(bufferWidthField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                            .addGroup(layout.createSequentialGroup()
                                                .addComponent(lblCLUTNumberBlocks)
                                                .addGap(15, 15, 15)
                                                .addComponent(clutNumberBlocksField, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblCLUTFormat)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(clutFormatField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                    .addGroup(layout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(lblWidth)
                                        .addGap(74, 74, 74)
                                        .addComponent(lblHeight))))
                            .addComponent(memoryImage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 8, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnGoToAddress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnGoToGE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnGoToFB, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addressField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblWidth)
                    .addComponent(widthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblHeight)
                    .addComponent(heightField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblBufferWidth)
                    .addComponent(bufferWidthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblAddress))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPixelFormat)
                    .addComponent(pixelFormatField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(swizzleField)
                    .addComponent(useAlphaField))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(clutAddressField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblCLUTNumberBlocks)
                    .addComponent(clutNumberBlocksField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblCLUTFormat)
                    .addComponent(clutFormatField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblCLUT))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblBackgroundColor)
                    .addComponent(backgroundColorField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGoToAddress)
                    .addComponent(btnGoToGE)
                    .addComponent(btnGoToFB))
                .addGap(18, 18, 18)
                .addComponent(memoryImage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnGoToAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGoToAddressActionPerformed
        goToAddress();
    }//GEN-LAST:event_btnGoToAddressActionPerformed
    
    private void btnGoToGEActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGoToGEActionPerformed
        goToBufferInfo(Modules.sceDisplayModule.getBufferInfoGe());
    }//GEN-LAST:event_btnGoToGEActionPerformed
    
    private void btnGoToFBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGoToFBActionPerformed
        goToBufferInfo(Modules.sceDisplayModule.getBufferInfoFb());
    }//GEN-LAST:event_btnGoToFBActionPerformed
    
    private void keyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_keyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            refreshImage();
        }
    }//GEN-LAST:event_keyPressed
    
    private void changeImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeImageActionPerformed
        refreshImage();
    }//GEN-LAST:event_changeImageActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField addressField;
    private javax.swing.JComboBox backgroundColorField;
    private javax.swing.JButton btnGoToAddress;
    private javax.swing.JButton btnGoToFB;
    private javax.swing.JButton btnGoToGE;
    private javax.swing.JTextField bufferWidthField;
    private javax.swing.JTextField clutAddressField;
    private javax.swing.JComboBox clutFormatField;
    private javax.swing.JTextField clutNumberBlocksField;
    private javax.swing.JTextField heightField;
    private javax.swing.JLabel lblAddress;
    private javax.swing.JLabel lblBackgroundColor;
    private javax.swing.JLabel lblBufferWidth;
    private javax.swing.JLabel lblCLUT;
    private javax.swing.JLabel lblCLUTFormat;
    private javax.swing.JLabel lblCLUTNumberBlocks;
    private javax.swing.JLabel lblHeight;
    private javax.swing.JLabel lblPixelFormat;
    private javax.swing.JLabel lblWidth;
    private javax.swing.JPanel memoryImage;
    private javax.swing.JComboBox pixelFormatField;
    private javax.swing.JCheckBox swizzleField;
    private javax.swing.JCheckBox useAlphaField;
    private javax.swing.JTextField widthField;
    // End of variables declaration//GEN-END:variables
}
