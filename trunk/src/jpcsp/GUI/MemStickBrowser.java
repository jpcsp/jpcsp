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
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import jpcsp.Emulator;
import jpcsp.MainGUI;
import jpcsp.WindowPropSaver;
import jpcsp.format.PBP;
import jpcsp.format.PSF;
import jpcsp.util.Constants;

public class MemStickBrowser extends javax.swing.JDialog {

    private static final long serialVersionUID = 7788144302296106541L;
    private MainGUI main;
    private File path;

    public MemStickBrowser(MainGUI main, File path) {
        super(main, true);

        this.main = main;
        this.path = path;

        initComponents();

        // restrict icon column width manually
        tblPrograms.getColumnModel().getColumn(0).setMinWidth(Constants.ICON0_WIDTH);
        tblPrograms.getColumnModel().getColumn(0).setMaxWidth(Constants.ICON0_WIDTH);

        // set custom renderers
        tblPrograms.setDefaultRenderer(Icon.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                setText(""); // NOI18N
                setIcon((Icon) value);
                return this;
            }
        });
        tblPrograms.setDefaultRenderer(File.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                try {
                    String prgPath = ((File) value).getCanonicalPath();
                    File cwd = new File(".");
                    if (prgPath.startsWith(cwd.getCanonicalPath())) {
                        prgPath = prgPath.substring(cwd.getCanonicalPath().length() + 1);
                    }
                    setText(prgPath);
                } catch (IOException ioe) {
                    setText(ioe.getLocalizedMessage());
                    ioe.printStackTrace();
                }
                if (isSelected) {
                    setForeground(table.getSelectionForeground());
                    setBackground(table.getSelectionBackground());
                } else {
                    setForeground(table.getForeground());
                    setBackground(table.getBackground());
                }
                return this;
            }
        });

        // enable 'load' button on valid selection
        tblPrograms.getSelectionModel()
                .addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                btnLoad.setEnabled(!((ListSelectionModel) e.getSource()).isSelectionEmpty());
            }
        });

        refreshFiles();

        WindowPropSaver.loadWindowProperties(this);
    }

    private final class MemStickTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -1675488447176776560L;
        private ImageIcon[] icons;
        private File[] programs;
        private PBP[] pbps;
        private PSF[] psfs;
        private File path;

        public MemStickTableModel(File path) {
            if (!path.isDirectory()) {
                Emulator.log.error("'" + path + "' is not a directory");
                this.path = null;
            } else {
                this.path = path;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return Icon.class;
                case 1:
                    return String.class;
                case 2:
                    return File.class;
                default:
                    throw new IndexOutOfBoundsException("column index out of range");
            }
        }

        @Override
        public String getColumnName(int column) {
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
            switch (column) {
                case 0:
                    return bundle.getString("MemStickBrowser.column.icon.text");
                case 1:
                    return bundle.getString("MemStickBrowser.column.title.text");
                case 2:
                    return bundle.getString("MemStickBrowser.column.path.text");
                default:
                    throw new IndexOutOfBoundsException("column index out of range");
            }
        }

        public void refresh() {
            // nothing to refresh on invalid path
            if (path == null) {
                return;
            }

            programs = path.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    String lower = file.getName().toLowerCase();
                    if (lower.endsWith(".pbp")) {
                        return true;
                    }
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
                    if (programs[i].isDirectory()) {
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
                        if (metadir.exists()) {
                            eboot = metadir.listFiles(new FileFilter() {
                                @Override
                                public boolean accept(File arg0) {
                                    return arg0.getName().equalsIgnoreCase("eboot.pbp");
                                }
                            });
                            if (eboot.length > 0) {
                                metapbp = eboot[0];
                            }
                        }

                        // kxploit%
                        metadir = new File(programs[i].getParentFile().getParentFile().getPath()
                                + File.separatorChar + programs[i].getParentFile().getName() + "%");
                        if (metadir.exists()) {
                            eboot = metadir.listFiles(new FileFilter() {
                                @Override
                                public boolean accept(File arg0) {
                                    return arg0.getName().equalsIgnoreCase("eboot.pbp");
                                }
                            });
                            if (eboot.length > 0) {
                                metapbp = eboot[0];
                            }
                        }

                        // Load unpacked icon
                        File[] icon0file = programs[i].getParentFile().listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File arg0) {
                                return arg0.getName().equalsIgnoreCase("icon0.png");
                            }
                        });
                        if (icon0file.length > 0) {
                            icons[i] = new ImageIcon(icon0file[0].getPath());
                        }

                        // Load unpacked PSF
                        File[] psffile = programs[i].getParentFile().listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File arg0) {
                                return arg0.getName().equalsIgnoreCase("param.sfo");
                            }
                        });
                        if (psffile.length > 0) {
                            RandomAccessFile raf = new RandomAccessFile(psffile[0], "r");
                            FileChannel roChannel = raf.getChannel();
                            ByteBuffer readbuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) roChannel.size());
                            psfs[i] = new PSF();
                            psfs[i].read(readbuffer);
                            raf.close();
                        }
                    }
                    if (programs[i].getName().toLowerCase().endsWith(".pbp")) {
                        // Load packed icon
                        RandomAccessFile raf = new RandomAccessFile(metapbp, "r");
                        FileChannel roChannel = raf.getChannel();
                        // Limit the size of the data read from the file to 100Kb.
                        // Some PBP files for demos can be very large (over 200GB)
                        // and raise an OutOfMemory exception.
                        int size = Math.min((int) roChannel.size(), 100 * 1024);
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
                    if (icons[i] == null) {
                        icons[i] = new ImageIcon(getClass().getResource("/jpcsp/images/icon0.png"));
                    }

                    // Rescale over sized icons
                    if (icons[i] != null) {
                        Image image = icons[i].getImage();
                        if (image.getWidth(null) > Constants.ICON0_WIDTH || image.getHeight(null) > Constants.ICON0_HEIGHT) {
                            image = image.getScaledInstance(Constants.ICON0_WIDTH, Constants.ICON0_HEIGHT, Image.SCALE_SMOOTH);
                            icons[i].setImage(image);
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            fireTableDataChanged();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public int getRowCount() {
            return (programs != null) ? programs.length : 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return icons[rowIndex];
                case 1:
                    String title;
                    if (psfs[rowIndex] == null || (title = psfs[rowIndex].getPrintableString("TITLE")) == null) {
                        // No PSF TITLE, get the parent directory name
                        title = programs[rowIndex].getParentFile().getName();
                    }
                    return title;
                case 2:
                    return programs[rowIndex];
                default:
                    return null;
            }
        }
    }

    final public void refreshFiles() {
        ((MemStickTableModel) tblPrograms.getModel()).refresh();
    }

    private void loadSelectedFile() {
        main.loadFile((File) tblPrograms.getValueAt(tblPrograms.getSelectedRow(), 2));
        dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        tblPrograms = new javax.swing.JTable();
        btnLoad = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        btnCancel = new jpcsp.GUI.CancelButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
        setTitle(bundle.getString("MemStickBrowser.title")); // NOI18N
        setMinimumSize(new java.awt.Dimension(600, 350));

        tblPrograms.setModel(new MemStickTableModel(path));
        tblPrograms.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_NEXT_COLUMN);
        tblPrograms.setRowHeight(80);
        tblPrograms.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tblPrograms.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblProgramsMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tblPrograms);

        btnLoad.setText(bundle.getString("LoadButton.text")); // NOI18N
        btnLoad.setEnabled(false);
        btnLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoadActionPerformed(evt);
            }
        });

        btnRefresh.setText(bundle.getString("MemStickBrowser.btnRefresh.text")); // NOI18N
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        btnCancel.setText(bundle.getString("CancelButton.text")); // NOI18N
        btnCancel.setParent(this);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnRefresh)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnLoad)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnLoad)
                    .addComponent(btnRefresh)
                    .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoadActionPerformed
        loadSelectedFile();
    }//GEN-LAST:event_btnLoadActionPerformed

    private void tblProgramsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblProgramsMouseClicked
        // load selected file on double click
        if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1) {
            loadSelectedFile();
        }
    }//GEN-LAST:event_tblProgramsMouseClicked

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        refreshFiles();
    }//GEN-LAST:event_btnRefreshActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private jpcsp.GUI.CancelButton btnCancel;
    private javax.swing.JButton btnLoad;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblPrograms;
    // End of variables declaration//GEN-END:variables
}
