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
package jpcsp;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jpcsp.HLE.pspiofilemgr;
import jpcsp.format.PBP;
import jpcsp.format.PSF;
import jpcsp.util.MetaInformation;

/**
 * @author Orphis
 * 
 */
public class MemStickBrowser extends JDialog {

	private final class MemStickTableColumnModel extends DefaultTableColumnModel {
		private static final long serialVersionUID = -6321946514015824875L;

		private final class CellRenderer extends DefaultTableCellRenderer {
			private static final long serialVersionUID = 6767267483048658105L;

			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object obj, boolean isSelected, boolean hasFocus,
					int row, int column) {
				if(obj instanceof Icon) {
					setText("");
					setIcon((Icon) obj);
					return this;
				}
				setIcon(null);
				return super.getTableCellRendererComponent(table, obj, isSelected, hasFocus, row, column);
			}
		}

		public MemStickTableColumnModel() {
			setColumnMargin(0);
			CellRenderer cellRenderer = new CellRenderer();
			TableColumn tableColumn = new TableColumn(0, 144, cellRenderer, null);
			tableColumn.setHeaderValue("Icon");
			tableColumn.setMaxWidth(154);
			tableColumn.setMinWidth(144);
			TableColumn tableColumn2 = new TableColumn(1, 100, cellRenderer, null);
			tableColumn2.setHeaderValue("Title");
			TableColumn tableColumn3 = new TableColumn(2, 200, cellRenderer, null);
			tableColumn3.setHeaderValue("Path");
			addColumn(tableColumn);			
			addColumn(tableColumn2);			
			addColumn(tableColumn3);
		}
	}

	private final class MemStickTableModel extends AbstractTableModel {
		private static final long serialVersionUID = -1675488447176776560L;
		
		

		public MemStickTableModel(File path) {
			if(!path.isDirectory()) {
				System.out.println(path + " isn't a directory");
				return;
			}
			programs = path.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					if (file.getName().toLowerCase().endsWith(".pbp"))
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
			
			icons = new ImageIcon[programs.length];
			pbps = new PBP[programs.length];
			psfs = new PSF[programs.length];
			
			for (int i = 0; i < programs.length; ++i) {
				try {
					if(programs[i].isDirectory()) {
						File eboot[] = programs[i].listFiles(new FileFilter() {
							@Override
							public boolean accept(File arg0) {
								return arg0.getName().equalsIgnoreCase("eboot.pbp");
							}
						});
						programs[i] = eboot[0];
					}
					FileChannel roChannel = new RandomAccessFile(programs[i], "r").getChannel();
					ByteBuffer readbuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int)roChannel.size());
					pbps[i] = new PBP(readbuffer);
					psfs[i] = pbps[i].readPSF(readbuffer);
					if(pbps[i].getSizeIcon0() > 0) {
						byte[] icon0 = new byte[pbps[i].getSizeIcon0()];
						readbuffer.position((int) pbps[i].getOffsetIcon0());
						readbuffer.get(icon0);
						icons[i] = new ImageIcon(icon0);
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
	            
				
			}
		}

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public int getRowCount() {
			return programs.length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			try {
				switch (columnIndex) {
				case 0:
					return icons[rowIndex];
				case 1:
					String title;
					if(psfs[rowIndex] == null || (title = psfs[rowIndex].getString("TITLE")) == null) {
						// No PSF TITLE, get the parent directory name
						title =  programs[rowIndex].getParentFile().getName();
					}
					return title;
				case 2:
					return programs[rowIndex].getCanonicalPath();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 7788144302296106541L;
	private JButton loadButton;
	private JTable table;
	private File[] programs;
	private ImageIcon[] icons;
	private PBP[] pbps;
	private PSF[] psfs;

	/**
	 * @param arg0
	 */
	public MemStickBrowser(final Emulator emu, Frame arg0) {
		super(arg0);
		
		setModal(true);
		
		setTitle("Memory Stick Browser");
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		table = new JTable(new MemStickTableModel(new File("ms0/PSP/GAME")), new MemStickTableColumnModel());
		table.setFillsViewportHeight(true);
		table.setRowHeight(80);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
		table.setTableHeader(new JTableHeader(table.getColumnModel()));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(false);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				loadButton.setEnabled(!((ListSelectionModel)e.getSource()).isSelectionEmpty());
			}});

		for (int c = 0; c < table.getColumnCount() - 1; c++) {
			DefaultTableColumnModel colModel = (DefaultTableColumnModel) table
					.getColumnModel();
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
			for (int r = 0; r < table.getRowCount(); r++) {
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

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MemStickBrowser.this.setVisible(false);
				MemStickBrowser.this.dispose();
			}
		});
		loadButton = new JButton("Load");
		loadButton.setEnabled(false);
		loadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					File selectedPBP = programs[table.getSelectedRow()];
					FileChannel roChannel = new RandomAccessFile(selectedPBP, "r").getChannel();
					ByteBuffer readbuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) roChannel.size());
					emu.load(readbuffer);
					pspiofilemgr.get_instance().setfilepath(selectedPBP.getParentFile().getCanonicalPath());
					((Frame)getParent()).setTitle(MetaInformation.FULL_NAME + " - "
							+ table.getModel().getValueAt(table.getSelectedRow(), 1));
					setVisible(false);
					dispose();
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		});

		layout.setHorizontalGroup(layout.createParallelGroup(
				GroupLayout.Alignment.TRAILING).addComponent(scrollPane)
				.addGroup(
						layout.createSequentialGroup().addComponent(loadButton)
								.addComponent(cancelButton)));

		layout.setVerticalGroup(layout.createSequentialGroup().addComponent(
				scrollPane).addGroup(
				layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(loadButton).addComponent(cancelButton)));

		getRootPane().setLayout(layout);
		setSize(600, 400);
	}

	public void refreshFiles() {
		table.setModel(new MemStickTableModel(new File("ms0/PSP/GAME")));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		} 
		MemStickBrowser msb = new MemStickBrowser(null, null);
		msb.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		msb.setVisible(true);
	}


}
