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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Resource;
import jpcsp.Settings;
import jpcsp.GUI.CancelButton;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules150.sceDisplay.BufferInfo;
import jpcsp.graphics.GeCommands;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.ImageReader;
import jpcsp.util.Utilities;

public class ImageViewer extends JFrame {
	private static final long serialVersionUID = 8837780642045065242L;

	private static final String windowNameForSettings = "imageviewer";
	private static final Color imageBorderColor = Color.RED;
	private static final int imageBorderSize = 2;

	private int startAddress = MemoryMap.START_VRAM;
	private int bufferWidth = 512;
	private int imageWidth = 480;
	private int imageHeight = 272;
	private boolean imageSwizzle = false;
	private boolean useAlpha = false;
	private int pixelFormat = GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
	private int clutAddress = 0;
	private int clutNumberBlocks = 0;
	private int clutFormat = GeCommands.CMODE_FORMAT_32BIT_ABGR8888;
	private int clutStart = 0;
	private int clutShift = 0;
	private int clutMask = 0xFF;

	private MemoryImage memoryImage;
	private JTextField addressField;
	private JTextField widthField;
	private JTextField heightField;
	private JTextField bufferWidthField;
	private JComboBox pixelFormatField;
	private JTextField clutAddressField;
	private JTextField clutNumberBlocksField;
	private JComboBox clutFormatField;
	private JCheckBox swizzleField;
	private JCheckBox useAlphaField;

	public ImageViewer() {
		init();
	}

	private void init() {
		initComponents();
	}

	private void initComponents() {
		setTitle(Resource.get("imageviewer"));
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
			public void windowDeactivated(java.awt.event.WindowEvent evt) {
                formWindowDeactivated(evt);
            }
        });
    	setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    	addressField = new JTextField();
    	addressField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                onKeyPressed(evt);
            }
    	});

    	widthField = new JTextField();
    	widthField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                onKeyPressed(evt);
            }
    	});

    	heightField = new JTextField();
    	heightField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                onKeyPressed(evt);
            }
    	});

    	bufferWidthField = new JTextField();
    	bufferWidthField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                onKeyPressed(evt);
            }
    	});

    	pixelFormatField = new JComboBox(new String[] { "565", "5551", "4444", "8888", "Indexed 4", "Indexed 8", "Indexed 16", "Indexed 32", "DXT1", "DXT3", "DXT5" });

    	swizzleField = new JCheckBox(Resource.get("swizzle"));

    	useAlphaField = new JCheckBox(Resource.get("usealpha"));

    	JLabel clutLabel = new JLabel(Resource.get("clut"));
    	clutAddressField = new JTextField();

    	clutNumberBlocksField = new JTextField();

    	clutFormatField = new JComboBox(new String[] { "565", "5551", "4444", "8888" });

    	JButton goToAddress = new JButton(Resource.get("gotoaddress"));
		goToAddress.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				goToAddress();
			}
		});

		JButton goToGeButton = new JButton(Resource.get("gotoge"));
		goToGeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				goToGe();
			}
		});

		JButton goToFbButton = new JButton(Resource.get("gotofb"));
		goToFbButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				goToFb();
			}
		});

		CancelButton cancel = new CancelButton(this);

		memoryImage = new MemoryImage();
		if (imageBorderSize > 0) {
			memoryImage.setBorder(BorderFactory.createLineBorder(imageBorderColor, imageBorderSize));
		}

		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		JLabel labelWidth = new JLabel(Resource.get("imagewidth"));
		JLabel labelHeight = new JLabel(Resource.get("imageheight"));
		JLabel labelBufferWidth = new JLabel(Resource.get("bufferwidth"));

		layout.setHorizontalGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
						.addComponent(addressField, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE)
						.addComponent(labelWidth)
						.addComponent(widthField, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
						.addComponent(labelHeight)
						.addComponent(heightField, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
						.addComponent(labelBufferWidth)
						.addComponent(bufferWidthField, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
						.addComponent(pixelFormatField, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(swizzleField)
						.addComponent(useAlphaField)
						)
				.addGroup(layout.createSequentialGroup()
						.addComponent(clutLabel)
						.addComponent(clutAddressField, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE)
						.addComponent(clutNumberBlocksField, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
						.addComponent(clutFormatField, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
						)
				.addGroup(layout.createSequentialGroup()
						.addComponent(goToAddress)
						.addComponent(goToGeButton)
						.addComponent(goToFbButton)
						.addComponent(cancel)
						)
				.addComponent(memoryImage));
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup()
						.addComponent(addressField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(labelWidth)
						.addComponent(widthField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(labelHeight)
						.addComponent(heightField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(labelBufferWidth)
						.addComponent(bufferWidthField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(pixelFormatField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(swizzleField)
						.addComponent(useAlphaField)
						)
				.addGroup(layout.createParallelGroup()
						.addComponent(clutLabel)
						.addComponent(clutAddressField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(clutNumberBlocksField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(clutFormatField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						)
				.addGroup(layout.createParallelGroup()
						.addComponent(goToAddress)
						.addComponent(goToGeButton)
						.addComponent(goToFbButton)
						.addComponent(cancel)
						)
				.addComponent(memoryImage));

        setLocation(Settings.getInstance().readWindowPos(windowNameForSettings));
        setSize(Settings.getInstance().readWindowSize(windowNameForSettings, Math.max(imageWidth + 30, 440), imageHeight + 110));

        copyValuesToFields();
	}

	public void refreshImage() {
		goToAddress();
	}

	private void formWindowDeactivated(WindowEvent evt) {
	    //Called when the window is closed
	    if (Settings.getInstance().readBool("gui.saveWindowPos")) {
            Settings.getInstance().writeWindowPos(windowNameForSettings, getLocation());
	    }
	}

	private void valuesUpdated() {
		memoryImage.setSize(memoryImage.getPreferredSize());
		repaint();
	}

	private void goToAddress() {
		try {
			startAddress = Utilities.parseAddress(addressField.getText());
		} catch (NumberFormatException e) {
	        JOptionPane.showMessageDialog(this, Resource.get("numbernotcorrect"));
	        return;
		}

		try {
			imageWidth = (int) Utilities.parseLong(widthField.getText());
		} catch (NumberFormatException e) {
	        JOptionPane.showMessageDialog(this, Resource.get("numbernotcorrect"));
	        return;
		}

		try {
			imageHeight = (int) Utilities.parseLong(heightField.getText());
		} catch (NumberFormatException e) {
	        JOptionPane.showMessageDialog(this, Resource.get("numbernotcorrect"));
	        return;
		}

		try {
			bufferWidth = (int) Utilities.parseLong(bufferWidthField.getText());
		} catch (NumberFormatException e) {
	        JOptionPane.showMessageDialog(this, Resource.get("numbernotcorrect"));
	        return;
		}

		pixelFormat = pixelFormatField.getSelectedIndex();
		imageSwizzle = swizzleField.isSelected();
		useAlpha = useAlphaField.isSelected();

		try {
			clutAddress = (int) Utilities.parseAddress(clutAddressField.getText());
		} catch (NumberFormatException e) {
	        JOptionPane.showMessageDialog(this, Resource.get("numbernotcorrect"));
	        return;
		}

		try {
			clutNumberBlocks = (int) Utilities.parseLong(clutNumberBlocksField.getText());
		} catch (NumberFormatException e) {
	        JOptionPane.showMessageDialog(this, Resource.get("numbernotcorrect"));
	        return;
		}

		clutFormat = clutFormatField.getSelectedIndex();

		valuesUpdated();
	}

	private void copyValuesToFields() {
		addressField.setText(String.format("0x%X", startAddress));
		widthField.setText(String.format("%d", imageWidth));
		heightField.setText(String.format("%d", imageHeight));
		bufferWidthField.setText(String.format("%d", bufferWidth));
		pixelFormatField.setSelectedIndex(pixelFormat);
		swizzleField.setSelected(imageSwizzle);
		useAlphaField.setSelected(useAlpha);
		clutAddressField.setText(String.format("0x%X", clutAddress));
		clutNumberBlocksField.setText(String.format("%d", clutNumberBlocks));
		clutFormatField.setSelectedIndex(clutFormat);
	}

	private void goToBufferInfo(BufferInfo bufferInfo) {
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

	private void goToGe() {
		goToBufferInfo(Modules.sceDisplayModule.getBufferInfoGe());
	}

	private void goToFb() {
		goToBufferInfo(Modules.sceDisplayModule.getBufferInfoFb());
	}

	private void onKeyPressed(KeyEvent evt) {
		if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
			goToAddress();
		}
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
				IMemoryReader imageReader = ImageReader.getImageReader(startAddress, imageWidth, imageHeight, bufferWidth, pixelFormat, imageSwizzle, clutAddress, clutFormat, clutNumberBlocks, clutStart, clutShift, clutMask);

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
}
