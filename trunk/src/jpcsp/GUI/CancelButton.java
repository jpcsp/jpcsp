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

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.KeyStroke;

import jpcsp.Resource;

public class CancelButton extends JButton {
	private static final long serialVersionUID = 7544005354633954062L;
	final public Window display;
	private static final String escapeCommand = "Escape pressed";

	public CancelButton(Window display) {
		this.display = display;
		init();
	}

	private void init() {
		setText(Resource.get("cancel"));

		// Display the display when the button is clicked
		addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                display.dispose();
            }
        });

		// Click on the button when the ESC key is pressed
		registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (escapeCommand.equals(event.getActionCommand())) {
					doClick();
				}
			}
		}, escapeCommand, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JButton.WHEN_IN_FOCUSED_WINDOW);
	}
}
