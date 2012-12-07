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
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.WindowConstants;
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
import jpcsp.format.PBP;
import jpcsp.format.PSF;
import jpcsp.settings.Settings;

/**
 * @author Orphis
 *
 */
public class MemStickBrowser extends JDialog {

    private static final class MemStickTableColumnModel extends DefaultTableColumnModel {
        private static final long serialVersionUID = -6321946514015824875L;

        private static final class CellRenderer extends DefaultTableCellRenderer {
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
            tableColumn.setHeaderValue(Resource.get("icon"));
            tableColumn.setMaxWidth(144);
            tableColumn.setMinWidth(144);
            TableColumn tableColumn2 = new TableColumn(1, 100, cellRenderer, null);
            tableColumn2.setHeaderValue(Resource.get("title"));
            TableColumn tableColumn3 = new TableColumn(2, 200, cellRenderer, null);
            tableColumn3.setHeaderValue(Resource.get("path"));
            addColumn(tableColumn);
            addColumn(tableColumn2);
            addColumn(tableColumn3);
        }
    }

    private final class MemStickTableModel extends AbstractTableModel {
        private static final long serialVersionUID = -1675488447176776560L;



        public MemStickTableModel(File path) {
            if(!path.isDirectory()) {
                Emulator.log.error(path + Resource.get("nodirectory"));
                return;
            }
            programs = path.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    String lower = file.getName().toLowerCase();
                    if (lower.endsWith(".pbp"))
                        return true;
                    if (file.isDirectory()
                            && !file.getName().startsWith("%")
                            && !file.getName().endsWith("%")) {
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
                    File metapbp = programs[i];
                    if(programs[i].isDirectory()) {
                        File eboot[] = programs[i].listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File arg0) {
                                return arg0.getName().equalsIgnoreCase("eboot.pbp");
                            }
                        });

                        metapbp = programs[i] = eboot[0];

                        // %__SCE__kxploit
                        File metadir = new File(programs[i].getParentFile().getParentFile().getPath()
                                + File.separatorChar + "%" + programs[i].getParentFile().getName());
                        if(metadir.exists()) {
                            eboot = metadir.listFiles(new FileFilter() {
                                @Override
                                public boolean accept(File arg0) {
                                    return arg0.getName().equalsIgnoreCase("eboot.pbp");
                                }
                            });
                            if(eboot.length > 0)
                                metapbp = eboot[0];
                        }

                        // kxploit%
                        metadir = new File(programs[i].getParentFile().getParentFile().getPath()
                                + File.separatorChar + programs[i].getParentFile().getName() + "%");
                        if(metadir.exists()) {
                            eboot = metadir.listFiles(new FileFilter() {
                                @Override
                                public boolean accept(File arg0) {
                                    return arg0.getName().equalsIgnoreCase("eboot.pbp");
                                }
                            });
                            if(eboot.length > 0)
                                metapbp = eboot[0];
                        }

                        // Load unpacked icon
                        File[] icon0file = programs[i].getParentFile().listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File arg0) {
                                return arg0.getName().equalsIgnoreCase("icon0.png");
                            }
                        });
                        if(icon0file.length > 0) {
                            icons[i] = new ImageIcon(icon0file[0].getPath());
                        }

                        // Load unpacked PSF
                        File[] psffile = programs[i].getParentFile().listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File arg0) {
                                return arg0.getName().equalsIgnoreCase("param.sfo");
                            }
                        });
                        if(psffile.length > 0) {
                        	RandomAccessFile raf = new RandomAccessFile(psffile[0], "r");
                            FileChannel roChannel = raf.getChannel();
                            ByteBuffer readbuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) roChannel.size());
                            psfs[i] = new PSF();
                            psfs[i].read(readbuffer);
                            raf.close();
                        }
                    }
                    if(programs[i].getName().toLowerCase().endsWith(".pbp")) {
                        // Load packed icon
                    	RandomAccessFile raf = new RandomAccessFile(metapbp, "r");
                        FileChannel roChannel = raf.getChannel();
                        // Limit the size of the data read from the file to 100Kb.
                        // Some PBP files for demos can be very large (over 200GB)
                        // and raise an OutOfMemory exception.
                        int size = Math.min((int)roChannel.size(), 100 * 1024);
                        ByteBuffer readbuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
                        pbps[i] = new PBP(readbuffer);
                        PSF psf = pbps[i].readPSF(readbuffer);
                        if (psf != null) {
                            psfs[i] = psf;
                        }
                        if (pbps[i].getSizeIcon0() > 0) {
                            byte[] icon0 = new byte[pbps[i].getSizeIcon0()];
                            readbuffer.position((int) pbps[i].getOffsetIcon0());
                            readbuffer.get(icon0);
                            icons[i] = new ImageIcon(icon0);
                        }
                        raf.close();
                    }

                    // default icon
                    if (icons[i] == null)
                        icons[i] = new ImageIcon(getClass().getResource("/jpcsp/images/icon0.png"));

                    // Rescale over sized icons
                    if (icons[i] != null) {
                        Image image = icons[i].getImage();
                        if (image.getWidth(null) > 144 || image.getHeight(null) > 80) {
                            image = image.getScaledInstance(144, 80, Image.SCALE_SMOOTH);
                            icons[i].setImage(image);
                        }
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
                    if(psfs[rowIndex] == null || (title = psfs[rowIndex].getPrintableString("TITLE")) == null) {
                        // No PSF TITLE, get the parent directory name
                        title =  programs[rowIndex].getParentFile().getName();
                    }
                    return title;
                case 2:
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
    public MemStickBrowser(MainGUI arg0, File path) {
        super(arg0);

        this.path = path;
        setModal(true);

        setTitle(Resource.get("memstick"));
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

        table.setFont(Settings.getInstance().getFont());

        DefaultTableColumnModel colModel = (DefaultTableColumnModel)table.getColumnModel();
        for (int c = 0; c < table.getColumnCount() - 1; c++) {
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

        JButton cancelButton = new CancelButton(this);

        loadButton = new JButton(Resource.get("load"));
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
        setSize(600, 400);
    }

    public void refreshFiles() {
        table.setModel(new MemStickTableModel(path));
    }

    private void loadSelectedfile() {
        File selectedFile = programs[table.getSelectedRow()];
            ((MainGUI) getParent()).loadFile(selectedFile);

        setVisible(false);
        dispose();
    }
}
