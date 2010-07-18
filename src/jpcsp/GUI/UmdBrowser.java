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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jpcsp.Emulator;
import jpcsp.MainGUI;
import jpcsp.Resource;
import jpcsp.Settings;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.format.PSF;

/**
 * @author Orphis, gid15
 *
 */
public class UmdBrowser extends JDialog {
	private static final String windowNameForSettings = "umdbrowser";

	private final class MemStickTableColumnModel extends DefaultTableColumnModel {
		private static final long serialVersionUID = -6321946514015824875L;

		private final class CellRenderer extends DefaultTableCellRenderer {
			private static final long serialVersionUID = 6767267483048658105L;

			@Override
			public Component getTableCellRendererComponent(JTable table,
                                Object obj, boolean isSelected, boolean hasFocus,
                                int row, int column)
            {
				if (obj instanceof Icon) {
					setIcon((Icon) obj);
					return this;
				} else if (obj instanceof String) {
                	JTextArea textArea = new JTextArea((String) obj);
                	textArea.setFont(getFont());
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

		public MemStickTableColumnModel() {
			setColumnMargin(0);
			CellRenderer cellRenderer = new CellRenderer();
			TableColumn tableColumn = new TableColumn(0, 144, cellRenderer, null);
			tableColumn.setHeaderValue(Resource.get("icon"));
			tableColumn.setMaxWidth(154);
			tableColumn.setMinWidth(144);
			TableColumn tableColumn2 = new TableColumn(1, 100, cellRenderer, null);
			tableColumn2.setHeaderValue(Resource.get("title"));
			addColumn(tableColumn);
			addColumn(tableColumn2);
		}
	}

	private final class MemStickTableModel extends AbstractTableModel {
		private static final long serialVersionUID = -1675488447176776560L;
		private UmdInfoLoader umdInfoLoader;

		public MemStickTableModel(File path) {
			if(!path.isDirectory()) {
				Emulator.log.error(path + Resource.get("nodirectory"));
				return;
			}
			programs = path.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					String lower = file.getName().toLowerCase();
					if (lower.endsWith(".cso") || lower.endsWith(".iso"))
						return true;
					if (file.isDirectory()) {
						File eboot[] = file.listFiles(new FileFilter() {
							@Override
							public boolean accept(File arg0) {
								return arg0.getName().equalsIgnoreCase("eboot.pbp");
							}
						});
						return eboot.length != 0;
					}
					return false;
				}
			});

			// The UMD informations are loaded asynchronously 
			// to provide a faster loading time for the UmdBrowser.
			// Prepare the containers for the information and
			// start the async loader thread as a daemon running at low priority.
			icons = new ImageIcon[programs.length];
			psfs = new PSF[programs.length];
			umdInfoLoaded = new boolean[programs.length];

			for (int i = 0; i < programs.length; ++i) {
				umdInfoLoaded[i] = false;
			}
			// load the first row: its size is used to compute the table size
			loadUmdInfo(0);

			umdInfoLoader = new UmdInfoLoader();
			umdInfoLoader.setName("Umd Browser - Umd Info Loader");
			umdInfoLoader.setPriority(Thread.MIN_PRIORITY);
			umdInfoLoader.setDaemon(true);
			umdInfoLoader.start();
		}

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public int getRowCount() {
			if (programs == null) {
				return 0;
			}

			return programs.length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			try {
				// The UMD info is loaded asynchronously.
				// Wait for the information to be loaded.
				while (!umdInfoLoaded[rowIndex]) {
					sleep(1);
				}

				switch (columnIndex) {
				case 0:
					return icons[rowIndex];
				case 1:
					String title;
					if (psfs[rowIndex] == null || (title = psfs[rowIndex].getString("TITLE")) == null) {
						// No PSF TITLE, get the parent directory name
						title =  programs[rowIndex].getParentFile().getName();
					}

					String discid;
					if (psfs[rowIndex] == null || (discid = psfs[rowIndex].getString("DISC_ID")) == null) {
						discid =  "No ID";
					}

					String firmware;
					if (psfs[rowIndex] == null || (firmware = psfs[rowIndex].getString("PSP_SYSTEM_VER")) == null) {
						firmware =  "Not found";
					}

					String prgPath = programs[rowIndex].getCanonicalPath();
					File cwd = new File(".");
					if (prgPath.startsWith(cwd.getCanonicalPath())) {
						prgPath = prgPath.substring(cwd.getCanonicalPath().length() + 1);
					}

					String text = String.format("%s\n%s\n%s\n%s", title, discid, firmware, prgPath);
					return text;
				}
			} catch (IOException e) {
				Emulator.log.error(e);
			}
			return null;
		}
	}

	private static final long serialVersionUID = 7788144302296106541L;
	private JButton loadButton;
	private JLabel pic1Label;
	private JLabel pic0Label;
	private JLabel icon0Label;
	private JTable table;
	private File[] programs;
	private ImageIcon[] icons;
	private PSF[] psfs;
	private volatile boolean[] umdInfoLoaded;
	private MainGUI gui;
	private UmdBrowserPmf umdBrowserPmf;
	private UmdBrowserSound umdBrowserSound;
	private int lastRowIndex = -1;

	public UmdBrowser(MainGUI gui, File path) {
		super(gui);

		this.gui = gui;
		setModal(true);

		setTitle(Resource.get("umdIsoCsobrowser"));
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		table = new JTable(new MemStickTableModel(path), new MemStickTableColumnModel());
		table.setFillsViewportHeight(true);
		table.setRowHeight(80);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
		table.setTableHeader(new JTableHeader(table.getColumnModel()));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(false);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				onSelectionChanged(event);
			}});
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(arg0.getClickCount() == 2 && arg0.getButton() == MouseEvent.BUTTON1)
					loadSelectedfile();
			}
		});

		for (int c = 0; c < table.getColumnCount() - 1; c++) {
			DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
			TableColumn col = colModel.getColumn(c);
			int width = 0;

			// Get width of column header
			TableCellRenderer renderer = col.getHeaderRenderer();
			if (renderer == null) {
				renderer = table.getTableHeader().getDefaultRenderer();
			}
			Component comp = renderer.getTableCellRendererComponent(table, col
					.getHeaderValue(), false, false, 0, 0);
			width = comp.getPreferredSize().width;

			// Get maximum width of column data
			for (int r = 0; r < 1; r++) {
				renderer = table.getCellRenderer(r, c);
				comp = renderer.getTableCellRendererComponent(table, table
						.getValueAt(r, c), false, false, r, c);
				width = Math.max(width, comp.getPreferredSize().width);
			}

			width += 2 * colModel.getColumnMargin();
			col.setPreferredWidth(width);
		}

		JScrollPane scrollPane = new JScrollPane(table);

		GroupLayout layout = new GroupLayout(getRootPane());
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		JButton cancelButton = new CancelButton(this);

		loadButton = new JButton(Resource.get("load"));
		loadButton.setEnabled(false);
		loadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadSelectedfile();
			}
		});

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		JPanel imagePanel = new JPanel(new GridBagLayout());

		icon0Label = new JLabel();
		icon0Label.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 0));
		icon0Label.setBorder(new PmfBorder());
		constraints.anchor = GridBagConstraints.WEST;
		imagePanel.add(icon0Label, constraints);

		pic0Label = new JLabel();
		Dimension pic0Size = new Dimension(310, 180);
		pic0Label.setMinimumSize(pic0Size);
		pic0Label.setMaximumSize(pic0Size);
		constraints.anchor = GridBagConstraints.EAST;
		imagePanel.add(pic0Label, constraints);

		// Add the background image as the last component
		// so that it is displayed behind all the others components.
		pic1Label = new JLabel();
		Dimension pic1Size = new Dimension(480, 272);
		pic1Label.setMinimumSize(pic1Size);
		pic1Label.setMaximumSize(pic1Size);
		constraints.anchor = GridBagConstraints.CENTER;
		imagePanel.add(pic1Label, constraints);

		layout.setHorizontalGroup(layout.createParallelGroup(
				GroupLayout.Alignment.TRAILING).addGroup(
						layout.createSequentialGroup().addComponent(imagePanel).addComponent(scrollPane))
				.addGroup(
						layout.createSequentialGroup().addComponent(loadButton)
								.addComponent(cancelButton)));

		layout.setVerticalGroup(layout.createSequentialGroup().addGroup(
				layout.createParallelGroup(GroupLayout.Alignment.CENTER).addComponent(
						imagePanel).addComponent(scrollPane)).addGroup(
				layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(loadButton).addComponent(cancelButton)));

		getRootPane().setLayout(layout);
        setLocation(Settings.getInstance().readWindowPos(windowNameForSettings));
		setSize(Settings.getInstance().readWindowSize(windowNameForSettings, 1000, 350));
	}

	private void loadUmdInfo(int rowIndex) {
		if (umdInfoLoaded[rowIndex]) {
			return;
		}

		try {
            if (programs[rowIndex].isDirectory()) {
                    File eboot[] = programs[rowIndex].listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File arg0) {
                                    return arg0.getName().equalsIgnoreCase("eboot.pbp");
                            }
                    });
                    programs[rowIndex] = eboot[0];
            }

            if (!programs[rowIndex].isDirectory()) {
                UmdIsoReader iso = new UmdIsoReader(programs[rowIndex].getPath());

                UmdIsoFile paramSfo = iso.getFile("PSP_GAME/param.sfo");
                byte[] sfo = new byte[(int)paramSfo.length()];
                paramSfo.read(sfo);
                paramSfo.close();
                ByteBuffer buf = ByteBuffer.wrap(sfo);
                psfs[rowIndex] = new PSF();
                psfs[rowIndex].read(buf);

                UmdIsoFile icon0umd = iso.getFile("PSP_GAME/ICON0.PNG");
                byte[] icon0 = new byte[(int) icon0umd.length()];
                icon0umd.read(icon0);
                icon0umd.close();
                icons[rowIndex] = new ImageIcon(icon0);
            }
		} catch (FileNotFoundException e) {
            // default icon
            icons[rowIndex] = new ImageIcon(getClass().getResource("/jpcsp/images/icon0.png"));
		} catch (IOException e) {
			Emulator.log.error(e);
		}

		umdInfoLoaded[rowIndex] = true;
	}

	private void onSelectionChanged(ListSelectionEvent event) {
		loadButton.setEnabled(!((ListSelectionModel)event.getSource()).isSelectionEmpty());

		ImageIcon pic0Icon = null;
		ImageIcon pic1Icon = null;
		ImageIcon icon0Icon = null;
		try {
			int rowIndex = table.getSelectedRow();
			UmdIsoReader iso = new UmdIsoReader(programs[rowIndex].getPath());

			// Read PIC0.PNG
			try {
                UmdIsoFile pic0umd = iso.getFile("PSP_GAME/PIC0.PNG");
                byte[] pic0 = new byte[(int) pic0umd.length()];
                pic0umd.read(pic0);
                pic0umd.close();
                pic0Icon = new ImageIcon(pic0);
			} catch (FileNotFoundException e) {
				// Ignore exception
			} catch (IOException e) {
				Emulator.log.error(e);
			}

			// Read PIC1.PNG
			try {
                UmdIsoFile pic1umd = iso.getFile("PSP_GAME/PIC1.PNG");
                byte[] pic1 = new byte[(int) pic1umd.length()];
                pic1umd.read(pic1);
                pic1umd.close();
                pic1Icon = new ImageIcon(pic1);
			} catch (FileNotFoundException e) {
				// Generate an empty image
				pic1Icon = new ImageIcon();
				BufferedImage image = new BufferedImage(480, 272, BufferedImage.TYPE_INT_ARGB);
				pic1Icon.setImage(image);
			} catch (IOException e) {
				Emulator.log.error(e);
			}

			icon0Icon = icons[rowIndex];

			if (lastRowIndex != rowIndex) {
				stopVideo();
				umdBrowserPmf = new UmdBrowserPmf(iso, "PSP_GAME/ICON1.PMF", icon0Label);
				umdBrowserSound = new UmdBrowserSound(iso, "PSP_GAME/SND0.AT3");
			}

			lastRowIndex = rowIndex;
		} catch (FileNotFoundException e) {
			// Ignore exception
		} catch (IOException e) {
			Emulator.log.error(e);
		}
		pic0Label.setIcon(pic0Icon);
		pic1Label.setIcon(pic1Icon);
		icon0Label.setIcon(icon0Icon);
	}

	private void stopVideo() {
		if (umdBrowserPmf != null) {
			umdBrowserPmf.stopVideo();
			umdBrowserPmf = null;
		}

		if (umdBrowserSound != null) {
			umdBrowserSound.stopVideo();
			umdBrowserSound = null;
		}
	}

	private void loadSelectedfile() {
		stopVideo();

		Settings.getInstance().writeWindowPos(windowNameForSettings, getLocation());
		Settings.getInstance().writeWindowSize(windowNameForSettings, getSize());

		File selectedFile = programs[table.getSelectedRow()];
		gui.loadUMD(selectedFile);

		setVisible(false);
		dispose();
	}

	@Override
	public void dispose() {
		// Stop the PMF video and sound before closing the UMD Browser
		stopVideo();

		super.dispose();
	}

	private static void sleep(long millis) {
	    if (millis > 0) {
		    try {
			  Thread.sleep(millis);
		    } catch (InterruptedException e) {
		    	// Ignore exception
		    }
	    }
	}

	/**
	 * Load asynchronously all the UMD information (icon, PSF).
	 */
	private class UmdInfoLoader extends Thread {
		@Override
		public void run() {
			for (int i = 0; i < umdInfoLoaded.length; i++) {
				loadUmdInfo(i);
			}
		}
		
	}

	private class PmfBorder extends AbstractBorder {
		private static final long serialVersionUID = -700510222853542503L;
		private static final int leftSpace = 20;
		private static final int topSpace = 8;
		private static final int borderWidth = 8;
		private static final int millisPerBeat = 1500;

		@Override
		public Insets getBorderInsets(Component c, Insets insets) {
			insets.set(topSpace, leftSpace, borderWidth, borderWidth);

			return insets;
		}

		@Override
		public Insets getBorderInsets(Component c) {
			return getBorderInsets(c, new Insets(0, 0, 0, 0));
		}

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			if (icon0Label.getIcon() == null) {
				return;
			}

			long now = System.currentTimeMillis();
			float beat = (now % millisPerBeat) / (float) millisPerBeat;
			float noBeat = 0.5f;

			// Draw border lines
			for (int i = 0; i < borderWidth; i++) {
				int alpha = getAlpha(noBeat, i);
				setColor(g, beat, alpha);

				// Vertical line on the right side
				g.drawLine(x + width - borderWidth + i, y + topSpace, x + width - borderWidth + i, y + height - borderWidth);

				// Horizontal line at the bottom
				g.drawLine(x + leftSpace, y + height - borderWidth + i, x + width - borderWidth, y + height - borderWidth + i);

				alpha = getAlpha(beat, i);
				setColor(g, noBeat, alpha);

				// Vertical line on the left side
				g.drawLine(x + leftSpace - i, y + topSpace, x + leftSpace - i, y + height - borderWidth);

				// Horizontal line at the top
				g.drawLine(x + leftSpace, y + topSpace - i, x + width - borderWidth, y + topSpace - i);
			}

			// Top left corner
			drawCorner(g, beat, noBeat, x + leftSpace - borderWidth, y + topSpace - borderWidth, borderWidth, borderWidth);

			// Top right corner
			drawCorner(g, beat, noBeat, x + width - borderWidth, y + topSpace - borderWidth, 0, borderWidth);

			// Bottom left corner
			drawCorner(g, beat, noBeat, x + leftSpace - borderWidth, y + height - borderWidth, borderWidth, 0);

			// Bottom right corner
			drawCorner(g, noBeat, beat, x + width - borderWidth, y + height - borderWidth, 0, 0);
		}

		private void drawCorner(Graphics g, float alphaBeat, float colorBeat, int x, int y, int centerX, int centerY) {
			for (int ix = 1; ix < borderWidth; ix++) {
				for (int iy = 1; iy < borderWidth; iy++) {
					int alpha = getAlpha(alphaBeat, ix - centerX, iy - centerY);
					setColor(g, colorBeat, alpha);
					drawPoint(g, x + ix, y + iy);
				}
			}
		}

		private int getAlpha(float beat, int distanceX, int distanceY) {
			float distance = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);

			return getAlpha(beat, distance);
		}

		private int getAlpha(float beat, float distance) {
			final float maxDistance = borderWidth;

			int maxAlpha = 0xF0;
			if (beat < 0.5f) {
				// beat 0.0 -> 0.5: increase alpha from 0 to max
				maxAlpha = (int) (maxAlpha * beat * 2);
			} else {
				// beat 0.5 -> 1.0: decrease alpha from max to 0
				maxAlpha = (int) (maxAlpha * (1 - beat) * 2);
			}

			distance = Math.abs(distance);
			distance = Math.min(distance, maxDistance);

			return maxAlpha - (int) ((distance * maxAlpha) / maxDistance);
		}

		private void setColor(Graphics g, float beat, int alpha) {
			int color = 0xA0;

			if (beat < 0.5f) {
				// beat 0.0 -> 0.5: increase color from 0 to max
				color = (int) (color * beat * 2);
			} else {
				// beat 0.5 -> 1.0: decrease alpha from max to 0
				color = (int) (color * (1 - beat) * 2);
			}

			g.setColor(new Color(color, color, color, alpha));
		}

		private void drawPoint(Graphics g, int x, int y) {
			g.drawLine(x, y, x, y);
		}
	}
}
