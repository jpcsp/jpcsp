package jpcsp.log;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import jpcsp.Settings;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class LogWindow extends JFrame {

	/**
	 *
	 */
	private static final long serialVersionUID = 9105338140096798954L;
	private JTextPane textPane;

	public LogWindow() {
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle("Logger");

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowDeactivated(WindowEvent e) {
				if (Settings.get_instance().readBoolOptions("guisettings/saveWindowPos")) {
					Settings.get_instance().writeWindowPos("logwindow", getLocation());

					/* save window size */
					String[] windowSize = new String[2];
					windowSize[0] = Integer.toString(getWidth());
					windowSize[1] = Integer.toString(getHeight());
					Settings.get_instance().writeWindowSize("logwindow", windowSize);
				}
			}});

		textPane = new JTextPane();
		JScrollPane scrollPane = new JScrollPane(textPane);

		TextPaneAppender textPaneAppender = (TextPaneAppender)Logger.getRootLogger().getAppender("JpcspAppender");
		if (textPaneAppender == null) {
			System.err.println("There is a problem with LogSettings.xml");
			System.exit(1);
		}
		textPaneAppender.setTextPane(textPane);

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
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File f = m_fileChooser.getSelectedFile();

					try {
						if (f.exists()) {
							int res = JOptionPane.showConfirmDialog(
											LogWindow.this,
											"File already Exists! Do you want to override?",
											"Already Exists Message",
											JOptionPane.YES_NO_OPTION,
											JOptionPane.QUESTION_MESSAGE);

							if (res != 0)
								return;
						}

						BufferedWriter out = null;
						try {
							out = new BufferedWriter(new FileWriter(f));
							out.write(textPane.getText());
						} catch (IOException ex) {
							throw ex;
						} finally {
							if (out != null) {
								try {
									out.close();
								} catch (IOException ex) {
									ex.printStackTrace();
								}
							}
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		});

		GroupLayout layout = new GroupLayout(getRootPane());
		layout.setAutoCreateGaps(true);
		//layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
				.addComponent(scrollPane)
				.addGroup(layout.createSequentialGroup()
						.addComponent(saveButton)
						.addComponent(clearButton)));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(scrollPane)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(saveButton)
						.addComponent(clearButton)));

		setSize(Settings.get_instance().readWindowSize("logwindow")[0], Settings.get_instance().readWindowSize("logwindow")[1]);
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
