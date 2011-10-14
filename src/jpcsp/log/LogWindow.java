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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jpcsp.log;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintStream;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import jpcsp.settings.Settings;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class LogWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	private static String confFile = "LogSettings.xml";
	private String[] loglevels = {"ALL","TRACE","DEBUG","INFO","WARN","ERROR","FATAL","OFF" };
	private JTextPane textPane;

	public LogWindow() {
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle("Logger");

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowDeactivated(WindowEvent e) {
				if (Settings.getInstance().readBool("gui.saveWindowPos")) {
                    // Save window's size and position.
					Settings.getInstance().writeWindowPos("logwindow", getLocation());
					Settings.getInstance().writeWindowSize("logwindow", getSize());
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

		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
				.addComponent(scrollPane)
				.addGroup(layout.createSequentialGroup()
                        .addComponent(loglevellabel)
                        .addComponent(loglevelcombo)
						.addComponent(clearButton)));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(scrollPane)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(loglevellabel)
                        .addComponent(loglevelcombo)
						.addComponent(clearButton)));

		setSize(Settings.getInstance().readWindowSize("logwindow", 500, 300));
		getRootPane().setLayout(layout);
	}


    public static void setConfXMLFile(String path) {
        confFile = path;
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
		System.setProperty("log4j.properties", confFile);
		DOMConfigurator.configure(confFile);

		System.setOut(new PrintStream(new LoggingOutputStream(Logger.getLogger("sysout"), Level.INFO)));
		new LogWindow().setVisible(true);
	}

	public void clearScreenMessages() {
		synchronized (textPane) {
			textPane.setText("");
		}
	}
}