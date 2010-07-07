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

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Resource;
import jpcsp.Settings;
import jpcsp.GUI.CancelButton;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.util.Utilities;
import static jpcsp.HLE.modules.sceDisplay.PSP_DISPLAY_PIXEL_FORMAT_4444;
import static jpcsp.HLE.modules.sceDisplay.PSP_DISPLAY_PIXEL_FORMAT_5551;
import static jpcsp.HLE.modules.sceDisplay.PSP_DISPLAY_PIXEL_FORMAT_565;
import static jpcsp.HLE.modules.sceDisplay.PSP_DISPLAY_PIXEL_FORMAT_8888;

public class ImageViewer extends JFrame {
	private static final long serialVersionUID = 8837780642045065242L;

	private int startAddress;
	private int bufferWidth;
	private int imageWidth;
	private int imageHeight;
	private boolean imageSwizzle;
	private int pixelFormat = PSP_DISPLAY_PIXEL_FORMAT_8888;
	private MemoryImage memoryImage;
	private Memory mem;
	private static final String windowNameForSettings = "imageviewer";
	private JTextField addressField;
	private JTextField widthField;
	private JTextField heightField;
	private JTextField bufferWidthField;
	private JComboBox pixelFormatField;
	private JCheckBox swizzleField;

	public ImageViewer() {
		init();
	}

	private void init() {
		mem = Memory.getInstance();

		initComponents();
	}

	private void initComponents() {
		setTitle(Resource.get("imageviewer"));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowDeactivated(java.awt.event.WindowEvent evt) {
                formWindowDeactivated(evt);
            }
        });
    	setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    	addressField = new JTextField(String.format("0x%X", MemoryMap.START_VRAM));
    	addressField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                onKeyPressed(evt);
            }
    	});

    	widthField = new JTextField("16");
    	widthField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                onKeyPressed(evt);
            }
    	});

    	heightField = new JTextField("16");
    	heightField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                onKeyPressed(evt);
            }
    	});

    	bufferWidthField = new JTextField("16");
    	bufferWidthField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                onKeyPressed(evt);
            }
    	});

    	pixelFormatField = new JComboBox(new String[] { "565", "5551", "4444", "8888" });
    	pixelFormatField.setSelectedIndex(PSP_DISPLAY_PIXEL_FORMAT_8888);

    	swizzleField = new JCheckBox(Resource.get("swizzle"));

    	JButton goToButton = new JButton(Resource.get("gotoaddress"));
		goToButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				goToAddress();
			}
		});

		CancelButton cancel = new CancelButton(this);

		memoryImage = new MemoryImage();
		memoryImage.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		JLabel labelWidth = new JLabel("W:");
		JLabel labelHeight = new JLabel("H:");
		JLabel labelBufferWidth = new JLabel("BW:");

		layout.setHorizontalGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
						.addComponent(addressField, GroupLayout.PREFERRED_SIZE, 65, GroupLayout.PREFERRED_SIZE)
						.addComponent(labelWidth)
						.addComponent(widthField, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
						.addComponent(labelHeight)
						.addComponent(heightField, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
						.addComponent(labelBufferWidth)
						.addComponent(bufferWidthField, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
						.addComponent(pixelFormatField)
						.addComponent(swizzleField)
						.addComponent(goToButton)
						.addComponent(cancel))
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
						.addComponent(goToButton)
						.addComponent(cancel))
				.addComponent(memoryImage));

        setLocation(Settings.getInstance().readWindowPos(windowNameForSettings));
        setSize(Settings.getInstance().readWindowSize(windowNameForSettings, 600, 300));

        refreshImage();
	}

    public int getStartAddress() {
		return startAddress;
	}

	public void setStartAddress(int startAddress) {
		this.startAddress = startAddress;
	}

	public int getBufferWidth() {
		return bufferWidth;
	}

	public void setBufferWidth(int bufferWidth) {
		this.bufferWidth = bufferWidth;
	}

	public int getPixelFormat() {
		return pixelFormat;
	}

	public void setPixelFormat(int pixelFormat) {
		this.pixelFormat = pixelFormat;
	}

	public int getImageWidth() {
		return imageWidth;
	}

	public void setImageWidth(int imageWidth) {
		this.imageWidth = imageWidth;
	}

	public int getImageHeight() {
		return imageHeight;
	}

	public void setImageHeight(int imageHeight) {
		this.imageHeight = imageHeight;
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

		repaint();
	}

	private void onKeyPressed(KeyEvent evt) {
		if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
			goToAddress();
		}
	}

	private class MemoryImage extends JPanel {
		private static final long serialVersionUID = 1372183323503668615L;

		public MemoryImage() {
		}

		@Override
		public void paintComponent(Graphics g) {
			if (mem.isAddressGood(startAddress)) {
				Insets insets = getInsets();
				int minWidth = Math.min(imageWidth, bufferWidth);
				for (int y = 0; y < imageHeight; y++) {
					for (int x = 0; x < minWidth; x++) {
						g.setColor(getColor(x, y));
						drawPixel(g, x + insets.left, y + insets.top);
					}
				}
			}
		}

		private Color getColor(int x, int y) {
			int r;
			int g;
			int b;
			int a;

			int bytesPerPixel = sceDisplay.getPixelFormatBytes(pixelFormat);
			int pixel;
			if (imageSwizzle) {
				int swizzleStep = 16 / bytesPerPixel; // Number of pixels in 16 bytes
				pixel = (x % swizzleStep) +
				        (x / swizzleStep) * swizzleStep * 8 +
				        (y % 8) * swizzleStep +
				        (y / 8) * 8 * bufferWidth;
			} else {
				pixel = y * bufferWidth + x;
			}
			int address = startAddress + pixel * bytesPerPixel;
			int value = (bytesPerPixel == 2 ? mem.read16(address) : mem.read32(address));

            // GU_PSM_8888 : 0xAABBGGRR
            // GU_PSM_4444 : 0xABGR
            // GU_PSM_5551 : ABBBBBGGGGGRRRRR
            // GU_PSM_5650 : BBBBBGGGGGGRRRRR
			switch (pixelFormat) {
				case PSP_DISPLAY_PIXEL_FORMAT_8888:
					r = (value >>  0) & 0xFF;
					g = (value >>  8) & 0xFF;
					b = (value >> 16) & 0xFF;
					a = (value >> 24) & 0xFF;
					break;
				case PSP_DISPLAY_PIXEL_FORMAT_4444:
					r = ((value >>  0) & 0x0F) << 4;
					g = ((value >>  4) & 0x0F) << 4;
					b = ((value >>  8) & 0x0F) << 4;
					a = ((value >> 12) & 0x0F) << 4;
					break;
				case PSP_DISPLAY_PIXEL_FORMAT_5551:
					r = ((value >>  0) & 0x1F) << 3;
					g = ((value >>  5) & 0x1F) << 3;
					b = ((value >> 10) & 0x1F) << 3;
					a = ((value >> 15) & 0x01) << 7;
					break;
				case PSP_DISPLAY_PIXEL_FORMAT_565:
					r = ((value >>  0) & 0x1F) << 3;
					g = ((value >>  5) & 0x3F) << 2;
					b = ((value >> 11) & 0x1F) << 3;
					a = 0xFF;
					break;
				default:
					r = 0;
					g = 0;
					b = 0;
					a = 0;
			}

			return new Color(r, g, b, a);
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
