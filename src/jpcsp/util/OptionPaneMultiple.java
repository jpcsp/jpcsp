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
package jpcsp.util;

/**
 *
 * @author nickblame
 */
import java.awt.Component;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class OptionPaneMultiple {
    JTextField startaddr;
    JTextField endaddr;
    JTextField filename;
    boolean completed=false;
    public OptionPaneMultiple(Component parentComponent, String start, String end) {
        startaddr = new JTextField(5);
        endaddr = new JTextField(5);
        filename = new JTextField(5);
        filename.setText("dump.txt");
        startaddr.setText(start);
        endaddr.setText(end);
        Object[] msg = {"Start address:", startaddr, "End address:", endaddr, "File name:", filename};

        Object[] options = {"Ok", "Cancel"};
        JOptionPane op = new JOptionPane(
                msg,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                options);

        JDialog dialog = op.createDialog(parentComponent, "dump code...");
        dialog.setVisible(true);
        Object selectedValue = op.getValue();


        //int result = JOptionPane.OK_OPTION;

        //try {
        //    result = ((Integer) op.getValue()).intValue();
        //} catch (Exception uninitializedValue) {
       // }
        //System.out.println(selectedValue);
        if (selectedValue == null) {
            //System.out.println("dump code window Closed");

        } else if (selectedValue.equals("Ok")) {
            //System.out.println("dump code returns " + startaddr.getText() + " : " + endaddr.getText() + " : " + filename.getText());
            completed=true;

        } else {
            //System.out.println("dump code window Canceled");
        }

    }
    public String[] getInput(){
        String[] i={startaddr.getText(),endaddr.getText(),filename.getText()};
        return i;
    }
    public boolean completed(){

        return completed;
    }

}
