package jpcsp.log;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import jpcsp.Settings;
import jpcsp.util.Utilities;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class LogWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private String[] loglevels = {"ALL","TRACE","DEBUG","INFO","WARN","ERROR","FATAL","OFF" };
	private JTextPane textPane;

	public LogWindow() {
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle("Logger");

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowDeactivated(WindowEvent e) {
				if (Settings.getInstance().readBool("gui.saveWindowPos")) {
					Settings.getInstance().writeWindowPos("logwindow", getLocation());

					/* save window size */
					Settings.getInstance().writeWindowSize("logwindow",
							new int[] { getWidth(), getHeight()});
				}
			}});

		textPane = new JTextPane();
		JScrollPane scrollPane = new JScrollPane(textPane);

		TextPaneAppender textPaneAppender = (TextPaneAppender)Logger.getRootLogger().getAppender("JpcspAppender");
		if (textPaneAppender != null) {
			textPaneAppender.setTextPane(textPane);
		}

		JButton clearButton = new JButton("Clear");
		clearButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				clearScreenMessages();
			}});
		JButton saveButton = new JButton("Save to file...");
		saveButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JFileChooser m_fileChooser = new JFileChooser();
                        m_fileChooser.setSelectedFile(new File("logoutput.txt"));
                        m_fileChooser.setDialogTitle("Save logging output");
                        m_fileChooser.setCurrentDirectory(new java.io.File("."));
                        int returnVal = m_fileChooser.showSaveDialog(LogWindow.this);
                        if (returnVal != JFileChooser.APPROVE_OPTION) {
                            return;
                        }
                        File f = m_fileChooser.getSelectedFile();
                        BufferedWriter out = null;
                        try {
                            if (f.exists()) {
                                int res = JOptionPane.showConfirmDialog(
                                        LogWindow.this,
                                        "File '" + f.getName() + "' already Exists! Do you want to override?",
                                        "Save Log Output",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.WARNING_MESSAGE);

                                if (res != JOptionPane.YES_OPTION) {
                                    return;
                                }
                            }

                            out = new BufferedWriter(new FileWriter(f));
                            out.write(textPane.getText());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        } finally {
                            Utilities.close(out);
                        }
                    }
		});
        JLabel loglevellabel = new JLabel("Choose Log Level");
        final JComboBox loglevelcombo = new JComboBox(loglevels);
        final Logger rootLogger = Logger.getRootLogger();
        Level getlevelfromconfig = rootLogger.getLevel();

        if(getlevelfromconfig.equals(Level.ALL))   loglevelcombo.setSelectedIndex(0);
        if(getlevelfromconfig.equals(Level.TRACE)) loglevelcombo.setSelectedIndex(1);
        if(getlevelfromconfig.equals(Level.DEBUG)) loglevelcombo.setSelectedIndex(2);
        if(getlevelfromconfig.equals(Level.INFO))  loglevelcombo.setSelectedIndex(3);
        if(getlevelfromconfig.equals(Level.WARN))  loglevelcombo.setSelectedIndex(4);
        if(getlevelfromconfig.equals(Level.ERROR)) loglevelcombo.setSelectedIndex(5);
        if(getlevelfromconfig.equals(Level.FATAL)) loglevelcombo.setSelectedIndex(6);
        if(getlevelfromconfig.equals(Level.OFF))   loglevelcombo.setSelectedIndex(7);

        loglevelcombo.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent itemEvent){
               if (itemEvent.getStateChange() == ItemEvent.SELECTED)
               {
                   if(itemEvent.getItem().equals("ALL"))   rootLogger.setLevel(Level.ALL);
                   if(itemEvent.getItem().equals("TRACE")) rootLogger.setLevel(Level.TRACE);
                   if(itemEvent.getItem().equals("DEBUG")) rootLogger.setLevel(Level.DEBUG);
                   if(itemEvent.getItem().equals("WARN"))  rootLogger.setLevel(Level.WARN);
                   if(itemEvent.getItem().equals("INFO"))  rootLogger.setLevel(Level.INFO);
                   if(itemEvent.getItem().equals("ERROR")) rootLogger.setLevel(Level.ERROR);
                   if(itemEvent.getItem().equals("FATAL")) rootLogger.setLevel(Level.FATAL);
                   if(itemEvent.getItem().equals("OFF"))   rootLogger.setLevel(Level.OFF);
               }}});


         loglevelcombo.addMouseWheelListener(new java.awt.event.MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent wheelEvent) {
                switch(wheelEvent.getWheelRotation()){
                    case 1:
                        if(loglevelcombo.getSelectedIndex() != 7)
                                loglevelcombo.setSelectedIndex(loglevelcombo.getSelectedIndex()+1);

                        break;

                    case -1:
                        if(loglevelcombo.getSelectedIndex() != 0)
                                loglevelcombo.setSelectedIndex(loglevelcombo.getSelectedIndex()-1);

                        break;

                    default: break;
                }
            }
        });


		GroupLayout layout = new GroupLayout(getRootPane());
		layout.setAutoCreateGaps(true);
		//layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
				.addComponent(scrollPane)
				.addGroup(layout.createSequentialGroup()
                        .addComponent(loglevellabel)
                        .addComponent(loglevelcombo)
						.addComponent(saveButton)
						.addComponent(clearButton)));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(scrollPane)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(loglevellabel)
                        .addComponent(loglevelcombo)
						.addComponent(saveButton)
						.addComponent(clearButton)));

		int[] size = Settings.getInstance().readWindowSize("logwindow");
		setSize(size[0], size[1]);
		if (getHeight() <= 200 || getWidth() <= 200)
			setSize(500, 300);
		getRootPane().setLayout(layout);
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
		System.setProperty("log4j.properties", "LogSettings.xml");
		DOMConfigurator.configure("LogSettings.xml");

		System.setOut(new PrintStream(new LoggingOutputStream(Logger.getLogger("sysout"), Level.INFO)));
		new LogWindow().setVisible(true);
	}

	public void clearScreenMessages() {
		synchronized (textPane) {
			textPane.setText("");
		}
	}

}
