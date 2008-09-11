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

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;


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

import jpcsp.MainGUI;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.format.PBP;
import jpcsp.format.PSF;
import jpcsp.util.MetaInformation;

/**
 * @author Orphis
 * 
 */
public class UmdBrowser extends JDialog {

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
                        TableColumn tableColumn3 = new TableColumn(2, 150, cellRenderer, null);
			tableColumn3.setHeaderValue("DiscID");
                        TableColumn tableColumn4 = new TableColumn(3, 100, cellRenderer, null);
			tableColumn4.setHeaderValue("Firmware");
			TableColumn tableColumn5 = new TableColumn(4, 200, cellRenderer, null);
			tableColumn5.setHeaderValue("Path");
			addColumn(tableColumn);			
			addColumn(tableColumn2);			
			addColumn(tableColumn3);
                        addColumn(tableColumn4);
                        addColumn(tableColumn5);
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

				    UmdIsoReader iso = new UmdIsoReader(programs[i].getPath());
			            UmdIsoFile paramSfo = iso.getFile("PSP_GAME/param.sfo");
			            
			            psfs[i] = new PSF(0);
			            byte[] sfo = new byte[(int)paramSfo.length()];
			            paramSfo.read(sfo);
			            ByteBuffer buf = ByteBuffer.wrap(sfo);
			            psfs[i].read(buf);
			            
			            UmdIsoFile icon0umd = iso.getFile("PSP_GAME/ICON0.PNG");
			            byte[] icon0 = new byte[(int) icon0umd.length()];
			            icon0umd.read(icon0);
			            icons[i] = new ImageIcon(icon0);
					
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
                                    	String discid;
					if(psfs[rowIndex] == null || (discid = psfs[rowIndex].getString("DISC_ID")) == null) {
						discid =  "No ID";
					}
					return discid;
                                case 3:
                                    	String firmware;
					if(psfs[rowIndex] == null || (firmware = psfs[rowIndex].getString("PSP_SYSTEM_VER")) == null) {
						
						firmware =  "Not found";
					}
					return firmware;
				case 4:
					String prgPath = programs[rowIndex].getCanonicalPath();
					File cwd = new File(".");
					if(prgPath.startsWith(cwd.getCanonicalPath()))
						prgPath = prgPath.substring(cwd.getCanonicalPath().length() + 1);
					return prgPath;
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
	private File path;
	/**
	 * @param arg0
	 */
	public UmdBrowser(MainGUI arg0, File path) {
		super(arg0);
		
		this.path = path;
		setModal(true);
		
		setTitle("Umd Iso/Cso Browser");
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
			public void valueChanged(ListSelectionEvent e) {
				loadButton.setEnabled(!((ListSelectionModel)e.getSource()).isSelectionEmpty());
			}});
		table.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(arg0.getClickCount() == 2 && arg0.getButton() == MouseEvent.BUTTON1)
					loadSelectedfile();				
			}
		});

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
				UmdBrowser.this.setVisible(false);
				UmdBrowser.this.dispose();
			}
		});
		loadButton = new JButton("Load");
		loadButton.setEnabled(false);
		loadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadSelectedfile();
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
		setSize(700, 400);
	}

	public void refreshFiles() {
		table.setModel(new MemStickTableModel(path));
	}
	
	private void loadSelectedfile() {
		File selectedFile = programs[table.getSelectedRow()];
		String lower = selectedFile.getName().toLowerCase();
			((MainGUI) getParent()).loadUMD(selectedFile);

		((Frame) getParent()).setTitle(MetaInformation.FULL_NAME + " - "
				+ table.getModel().getValueAt(table.getSelectedRow(), 1));
		setVisible(false);
		dispose();
	}

}
