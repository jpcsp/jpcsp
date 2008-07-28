/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.Debugger;

/**
 *
 * @author nickblame
 */
import javax.swing.*;

public class OptionPaneMultiple extends JFrame {
    JTextField startaddr;
    JTextField endaddr;
    JTextField filename;
    boolean completed=false;
    public OptionPaneMultiple() {
        startaddr = new JTextField(5);
        endaddr = new JTextField(5);
        filename = new JTextField(5);
        Object[] msg = {"Start address:", startaddr, "End address:", endaddr, "File name:", filename};

        Object[] options = {"Ok", "Cancel"};
        JOptionPane op = new JOptionPane(
                msg,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                options);

        JDialog dialog = op.createDialog(this, "dump code...");
        dialog.setVisible(true);
        Object selectedValue = op.getValue();


        //int result = JOptionPane.OK_OPTION;

        //try {
        //    result = ((Integer) op.getValue()).intValue();
        //} catch (Exception uninitializedValue) {
       // }
        //System.out.println(selectedValue);
        if (selectedValue == null) {
            System.out.println("dump code window Closed");
           
        } else if (selectedValue.equals("Ok")) {
            //System.out.println("dump code returns " + startaddr.getText() + " : " + endaddr.getText() + " : " + filename.getText());
            completed=true;
           
        } else {
            System.out.println("dump code window Canceled");
            
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
