/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.GUI;

import static jpcsp.HLE.modules150.sceAudiocodec.PSP_CODEC_AT3PLUS;
import static jpcsp.util.Utilities.endianSwap32;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.AbstractBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.MainGUI;
import jpcsp.Memory;
import jpcsp.WindowPropSaver;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.iso.UmdIsoVirtualFile;
import jpcsp.HLE.modules150.sceAtrac3plus.AtracFileInfo;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.format.PSF;
import jpcsp.format.psmf.PsmfAudioDemuxVirtualFile;
import jpcsp.settings.Settings;
import jpcsp.util.Constants;

/**
 * @author Orphis, gid15
 */
public class UmdBrowser extends javax.swing.JDialog {
    private static final long serialVersionUID = 7788144302296106541L;
    private static Logger log = Emulator.log;

    private final class MemStickTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -1675488447176776560L;
        private UmdInfoLoader umdInfoLoader;
        private String pathPrefix;

        public MemStickTableModel(File[] paths) {
            // Default values in case we return an error
            umdInfoLoaded = new boolean[0];

            // Collect all the programs for all the given paths
            List<File> programList = new ArrayList<File>();
            for (File path : paths) {
                if (!path.isDirectory()) {
                    log.error("'" + path + "' is not a directory.");
                    return;
                }

                try {
                    this.pathPrefix = path.getCanonicalPath();
                } catch (IOException e) {
                    this.pathPrefix = path.getPath();
                }

                File[] pathPrograms = path.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        String lower = file.getName().toLowerCase();
                        if (lower.endsWith(".cso") || lower.endsWith(".iso")) {
                            return true;
                        }
                        if (file.isDirectory()) {
                            File eboot[] = file.listFiles(new FileFilter() {
                                @Override
                                public boolean accept(File file) {
                                    return file.getName().equalsIgnoreCase("eboot.pbp");
                                }
                            });
                            if (eboot.length != 1) {
                            	return false;
                            }

                            // Basic sanity checks on EBOOT.PBP
                            DataInputStream is = null;
                            try {
								is = new DataInputStream(new FileInputStream(eboot[0]));
								byte[] header = new byte[0x24];
								int length = is.read(header);
								if (length != header.length) {
									return false;
								}
								// PBP header?
								if (header[0] != 0 || header[1] != 'P' || header[2] != 'B' || header[3] != 'P') {
									return false;
								}
								int psarDataOffset = endianSwap32(is.readInt());
								// Homebrews have a PSAR data offset equal to the file length
								if (psarDataOffset >= eboot[0].length()) {
									// Homebrew
									return false;
								}
							} catch (IOException e) {
								return false;
							} finally {
								if (is != null) {
									try {
										is.close();
									} catch (IOException e) {
										// Ignore exception
									}
								}
							}

                            // Valid EBOOT.PBP
                            return true;
                        }
                        return false;
                    }
                });
                
                programList.addAll(Arrays.asList(pathPrograms));
            }

            // Sort the programs based on their file name
            Collections.sort(programList, new Comparator<File>() {
                @Override
                public int compare(File file1, File file2) {
                    if (file1 == null) {
                        return (file2 == null ? 0 : 1);
                    } else if (file2 == null) {
                        return -1;
                    }
                    
                    String name1 = file1.getName().toLowerCase();
                    String name2 = file2.getName().toLowerCase();
                    if (name1.equals(name2)) {
                        return compare(file1.getParentFile(), file2.getParentFile());
                    }
                    return name1.compareTo(name2);
                }
            });
            
            programs = programList.toArray(new File[programList.size()]);

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
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return Icon.class;
                case 1:
                    return String.class;
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
                default:
                    throw new IndexOutOfBoundsException("column index out of range");
            }
        }
        
        @Override
        public int getColumnCount() {
            return 2;
        }
        
        @Override
        public int getRowCount() {
            return (programs != null) ? programs.length : 0;
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= umdInfoLoaded.length) {
                return null;
            }
            
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
                        String title = getTitle(rowIndex);
                        
                        String discid;
                        if (psfs[rowIndex] == null || (discid = psfs[rowIndex].getString("DISC_ID")) == null) {
                            discid = "No ID";
                        }
                        
                        String firmware;
                        if (psfs[rowIndex] == null || (firmware = psfs[rowIndex].getString("PSP_SYSTEM_VER")) == null) {
                            firmware = "Not found";
                        }
                        
                        String prgPath = programs[rowIndex].getCanonicalPath();
                        if (prgPath.startsWith(pathPrefix)) {
                            prgPath = prgPath.substring(pathPrefix.length() + 1);
                        } else {
                            String cwdPath = new File(".").getCanonicalPath();
                            if (prgPath.startsWith(cwdPath)) {
                                prgPath = prgPath.substring(cwdPath.length() + 1);
                            }
                        }
                        
                        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
                        String text = String.format(
                                "%s\n%s: %s\n%s: %s\n%s",
                                title,
                                bundle.getString("UmdBrowser.strDiscID.text"),
                                discid,
                                bundle.getString("UmdBrowser.strFirmware.text"),
                                firmware,
                                prgPath);
                        return text;
                }
            } catch (IOException e) {
                log.error(e);
            }
            return null;
        }
    }
    private File[] programs;
    private ImageIcon[] icons;
    private PSF[] psfs;
    private volatile boolean[] umdInfoLoaded;
    private UmdBrowserPmf umdBrowserPmf;
    private UmdBrowserSound umdBrowserSound;
    private int lastRowIndex = -1;
    private boolean isSwitchingUmd;
    private MainGUI gui;
    private File[] paths;

    /**
     * Creates new form UmdBrowser
     */
    public UmdBrowser(MainGUI gui, File[] paths) {
        super(gui);

        this.gui = gui;
        this.paths = paths;

        initPNG();

        initComponents();

        // set blinking border for ICON0
        icon0Label.setBorder(new PmfBorder());

        // restrict icon column width manually
        table.getColumnModel().getColumn(0).setMinWidth(Constants.ICON0_WIDTH);
        table.getColumnModel().getColumn(0).setMaxWidth(Constants.ICON0_WIDTH);

        // set custom renderers
        table.setDefaultRenderer(Icon.class, new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                setText(""); // NOI18N
                setIcon((Icon) value);
                return this;
            }
        });
        table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JTextArea textArea = new JTextArea((String) value);
                textArea.setFont(getFont());
                if (isSelected) {
                    textArea.setForeground(table.getSelectionForeground());
                    textArea.setBackground(table.getSelectionBackground());
                } else {
                    textArea.setForeground(table.getForeground());
                    textArea.setBackground(table.getBackground());
                }
                return textArea;
            }
        });

        // update icons on selection change
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                onSelectionChanged(event);
            }
        });

        WindowPropSaver.loadWindowProperties(this);
    }

    private void initPNG() {
        // Invoke
        //    sun.awt.image.PNGImageDecoder.setCheckCRC(false)
        // to avoid an exception "PNGImageDecoder$PNGException: crc corruption" when reading incorrect PNG files.
        // As this is a class in the "sun" package, be careful as this class could disappear in a later JDK version:
        // do not statically reference this class and invoke the method using reflection.
        try {
        	getClass().getClassLoader().loadClass("sun.awt.image.PNGImageDecoder").getMethod("setCheckCRC", boolean.class).invoke(null, false);
        } catch (Throwable e) {
        	log.info(e);
        }
    }

    private String getUmdBrowseCacheDirectory(String name) {
        // Return "tmp/UmdBrowserCache/<name>/"
        return String.format("%1$s%2$cUmdBrowserCache%2$c%3$s%2$c", Settings.getInstance().readString("emu.tmppath"), File.separatorChar, name);
    }

    private void writeUmdBrowseCacheFile(String cacheDirectory, String name, byte[] content) {
        try {
            OutputStream os = new FileOutputStream(cacheDirectory + name);
            os.write(content);
            os.close();
        } catch (FileNotFoundException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void loadUmdInfo(int rowIndex) {
        if (rowIndex >= umdInfoLoaded.length || umdInfoLoaded[rowIndex]) {
            return;
        }

        try {
        	boolean cacheEntry = true;
        	String entryName = programs[rowIndex].getName();
            if (programs[rowIndex].isDirectory()) {
                File eboot[] = programs[rowIndex].listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.getName().equalsIgnoreCase("eboot.pbp");
                    }
                });

                if (eboot.length > 0) {
                	programs[rowIndex] = eboot[0];
                } else {
                	cacheEntry = false;
                }
            }

            if (cacheEntry) {
                String cacheDirectory = getUmdBrowseCacheDirectory(entryName);
                File sfoFile = new File(cacheDirectory + "param.sfo");
                if (sfoFile.canRead()) {
                    // Read the param.sfo and ICON0.PNG from the UmdBrowserCache
                    byte[] sfo = new byte[(int) sfoFile.length()];
                    InputStream is = new FileInputStream(sfoFile);
                    is.read(sfo);
                    is.close();
                    psfs[rowIndex] = new PSF();
                    psfs[rowIndex].read(ByteBuffer.wrap(sfo));

                    File icon0File = new File(cacheDirectory + "ICON0.PNG");
                    if (icon0File.canRead()) {
                        icons[rowIndex] = new ImageIcon(icon0File.getPath());
                    } else {
                        icons[rowIndex] = new ImageIcon(getClass().getResource("/jpcsp/images/icon0.png"));
                    }
                } else {
                    // Read the param.sfo and ICON0.PNG from the ISO and
                    // store them in the UmdBrowserCache.

                    // Create the UmdBrowse Cache directories
                    new File(cacheDirectory).mkdirs();

                    UmdIsoReader iso = new UmdIsoReader(programs[rowIndex].getPath());

                    UmdIsoFile paramSfo = iso.getFile("PSP_GAME/param.sfo");
                    byte[] sfo = new byte[(int) paramSfo.length()];
                    paramSfo.read(sfo);
                    paramSfo.close();
                    writeUmdBrowseCacheFile(cacheDirectory, "param.sfo", sfo);
                    ByteBuffer buf = ByteBuffer.wrap(sfo);
                    psfs[rowIndex] = new PSF();
                    psfs[rowIndex].read(buf);

                    UmdIsoFile icon0umd = iso.getFile("PSP_GAME/ICON0.PNG");
                    byte[] icon0 = new byte[(int) icon0umd.length()];
                    icon0umd.read(icon0);
                    icon0umd.close();
                    writeUmdBrowseCacheFile(cacheDirectory, "ICON0.PNG", icon0);
                    icons[rowIndex] = new ImageIcon(icon0);
                }
            }
        } catch (FileNotFoundException e) {
            // Check if we're dealing with a UMD_VIDEO.
            try {
                UmdIsoReader iso = new UmdIsoReader(programs[rowIndex].getPath());

                UmdIsoFile paramSfo = iso.getFile("UMD_VIDEO/param.sfo");
                UmdIsoFile umdDataFile = iso.getFile("UMD_DATA.BIN");

                // Manually fetch the DISC ID from the UMD_DATA.BIN (video ISO files lack
                // this param in their param.sfo).
                byte[] umdDataId = new byte[10];
                umdDataFile.readFully(umdDataId, 0, 9);
                String umdDataIdString = new String(umdDataId);

                byte[] sfo = new byte[(int) paramSfo.length()];
                paramSfo.read(sfo);
                paramSfo.close();
                ByteBuffer buf = ByteBuffer.wrap(sfo);
                psfs[rowIndex] = new PSF();
                psfs[rowIndex].read(buf);
                psfs[rowIndex].put("DISC_ID", umdDataIdString);

                UmdIsoFile icon0umd = iso.getFile("UMD_VIDEO/ICON0.PNG");
                byte[] icon0 = new byte[(int) icon0umd.length()];
                icon0umd.read(icon0);
                icon0umd.close();
                icons[rowIndex] = new ImageIcon(icon0);
            } catch (FileNotFoundException ve) {
                // default icon
                icons[rowIndex] = new ImageIcon(getClass().getResource("/jpcsp/images/icon0.png"));
            } catch (IOException ve) {
                log.error(ve);
            }
        } catch (IOException e) {
            log.error(e);
        }

        umdInfoLoaded[rowIndex] = true;
    }
    
    private void onSelectionChanged(ListSelectionEvent event) {
        loadButton.setEnabled(!((ListSelectionModel) event.getSource()).isSelectionEmpty());

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
                log.error(e);
            }

            // Read PIC1.PNG
            try {
                UmdIsoFile pic1umd = iso.getFile("PSP_GAME/PIC1.PNG");
                byte[] pic1 = new byte[(int) pic1umd.length()];
                pic1umd.read(pic1);
                pic1umd.close();
                pic1Icon = new ImageIcon(pic1);
            } catch (FileNotFoundException e) {
                // Check if we're dealing with a UMD_VIDEO.
                try {
                    UmdIsoFile pic1umd = iso.getFile("UMD_VIDEO/PIC1.PNG");
                    byte[] pic1 = new byte[(int) pic1umd.length()];
                    pic1umd.read(pic1);
                    pic1umd.close();
                    pic1Icon = new ImageIcon(pic1);
                } catch (FileNotFoundException ve) {
                    // Generate an empty image
                    pic1Icon = new ImageIcon();
                    BufferedImage image = new BufferedImage(Constants.PSPSCREEN_WIDTH, Constants.PSPSCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                    pic1Icon.setImage(image);
                } catch (IOException ve) {
                    log.error(ve);
                }
            } catch (IOException e) {
                log.error(e);
            }

            icon0Icon = icons[rowIndex];

            if (lastRowIndex != rowIndex) {
                stopVideo();
                umdBrowserPmf = new UmdBrowserPmf(iso, "PSP_GAME/ICON1.PMF", icon0Label);
                if (iso.hasFile("PSP_GAME/SND0.AT3")) {
                	umdBrowserSound = new UmdBrowserSound(Memory.getInstance(), iso, "PSP_GAME/SND0.AT3");
                } else {
                	IVirtualFile pmf = new UmdIsoVirtualFile(iso.getFile("PSP_GAME/ICON1.PMF"));
                	IVirtualFile audio = new PsmfAudioDemuxVirtualFile(pmf, 0x800, 0);
                	AtracFileInfo atracFileInfo = new AtracFileInfo();
                	atracFileInfo.inputFileDataOffset = 0;
                	atracFileInfo.atracChannels = 2;
                	atracFileInfo.atracCodingMode = 0;
                	umdBrowserSound = new UmdBrowserSound(Memory.getInstance(), audio, PSP_CODEC_AT3PLUS, atracFileInfo);
                	audio.ioClose();
                	pmf.ioClose();
                }
            }

            lastRowIndex = rowIndex;
        } catch (FileNotFoundException e) {
            // Ignore exception
        } catch (IOException e) {
            log.error(e);
        }
        pic0Label.setIcon(pic0Icon);
        pic1Label.setIcon(pic1Icon);
        icon0Label.setIcon(icon0Icon);
    }

    private String getTitle(int rowIndex) {
        String title;
        if (psfs[rowIndex] == null || (title = psfs[rowIndex].getString("TITLE")) == null) {
            // No PSF TITLE, get the parent directory name
            title = programs[rowIndex].getParentFile().getName();
        }

        return title;
    }

    private void scrollTo(char c) {
        c = Character.toLowerCase(c);
        int scrollToRow = -1;
        for (int rowIndex = 0; rowIndex < programs.length; rowIndex++) {
            String title = getTitle(rowIndex);
            if (title != null && title.length() > 0) {
                char firstChar = Character.toLowerCase(title.charAt(0));
                if (firstChar == c) {
                    scrollToRow = rowIndex;
                    break;
                }
            }
        }

        if (scrollToRow >= 0) {
            table.scrollRectToVisible(table.getCellRect(scrollToRow, 0, true));
        }
    }

    private void stopVideo() {
        if (umdBrowserPmf != null) {
            umdBrowserPmf.stopVideo();
            umdBrowserPmf = null;
        }

        if (umdBrowserSound != null) {
            umdBrowserSound.stopSound();
            umdBrowserSound = null;
        }
    }

    private void loadSelectedfile() {
        stopVideo();

        File selectedFile = programs[table.getSelectedRow()];
        if (isSwitchingUmd()) {
            gui.switchUMD(selectedFile);
            setVisible(false);
            dispose();
        } else {
            gui.loadUMD(selectedFile);
            dispose();
            gui.loadAndRun();
        }
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

    public boolean isSwitchingUmd() {
        return isSwitchingUmd;
    }

    public void setSwitchingUmd(boolean isSwitchingUmd) {
        this.isSwitchingUmd = isSwitchingUmd;
        loadButton.setText(java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp").getString(isSwitchingUmd() ? "UmdBrowser.loadButtonSwitch.text" : "UmdBrowser.loadButton.text"));
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

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        loadButton = new javax.swing.JButton();
        cancelButton = new jpcsp.GUI.CancelButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        imagePanel = new javax.swing.JPanel();
        icon0Label = new javax.swing.JLabel();
        pic0Label = new javax.swing.JLabel();
        pic1Label = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
        setTitle(bundle.getString("UmdBrowser.title")); // NOI18N
        setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        loadButton.setText(bundle.getString("LoadButton.text")); // NOI18N
        loadButton.setEnabled(false);
        loadButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadButtonActionPerformed(evt);
            }
        });

        cancelButton.setText(bundle.getString("CancelButton.text")); // NOI18N
        cancelButton.setParent(this);

        table.setModel(new MemStickTableModel(paths));
        table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_NEXT_COLUMN);
        table.setRowHeight(Constants.ICON0_HEIGHT);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
			public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableMouseClicked(evt);
            }
        });
        table.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
			public void keyTyped(java.awt.event.KeyEvent evt) {
                tableKeyTyped(evt);
            }
            @Override
			public void keyPressed(java.awt.event.KeyEvent evt) {
                tableKeyPressed(evt);
            }
            @Override
			public void keyReleased(java.awt.event.KeyEvent evt) {
                tableKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(table);

        imagePanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        imagePanel.setPreferredSize(new java.awt.Dimension(480, 272));
        imagePanel.setLayout(new java.awt.GridBagLayout());

        icon0Label.setBackground(new java.awt.Color(255, 255, 255));
        icon0Label.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 22, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        imagePanel.add(icon0Label, gridBagConstraints);

        pic0Label.setBackground(new java.awt.Color(204, 204, 204));
        pic0Label.setMaximumSize(new java.awt.Dimension(310, 180));
        pic0Label.setMinimumSize(new java.awt.Dimension(310, 180));
        pic0Label.setPreferredSize(new java.awt.Dimension(310, 180));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        imagePanel.add(pic0Label, gridBagConstraints);

        pic1Label.setBackground(new java.awt.Color(153, 153, 153));
        pic1Label.setMaximumSize(new java.awt.Dimension(480, 272));
        pic1Label.setMinimumSize(new java.awt.Dimension(480, 272));
        pic1Label.setPreferredSize(new java.awt.Dimension(480, 272));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        imagePanel.add(pic1Label, gridBagConstraints);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(loadButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(imagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 375, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(imagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(loadButton)
                    .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void tableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseClicked
        if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1) {
            loadSelectedfile();
        }
    }//GEN-LAST:event_tableMouseClicked
    
    private void tableKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableKeyPressed
        // do nothing
    }//GEN-LAST:event_tableKeyPressed
    
    private void tableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableKeyReleased
        // do nothing
    }//GEN-LAST:event_tableKeyReleased
    
    private void tableKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableKeyTyped
        scrollTo(evt.getKeyChar());
    }//GEN-LAST:event_tableKeyTyped
    
    private void loadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadButtonActionPerformed
        loadSelectedfile();
    }//GEN-LAST:event_loadButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private jpcsp.GUI.CancelButton cancelButton;
    private javax.swing.JLabel icon0Label;
    private javax.swing.JPanel imagePanel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton loadButton;
    private javax.swing.JLabel pic0Label;
    private javax.swing.JLabel pic1Label;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
}
