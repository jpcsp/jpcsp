/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp;

import java.awt.Component;
import javax.swing.JOptionPane;
import jpcsp.info.MetaInformation;

/**
 *
 * @author Leandro
 */
public class JpcspDialogManager {

    public static void showInformation(Component compo, String message) {
        JOptionPane.showMessageDialog(compo, message, MetaInformation.FULL_NAME, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showWarning(Component compo, String message) {
        JOptionPane.showMessageDialog(compo, message, MetaInformation.FULL_NAME, JOptionPane.WARNING_MESSAGE);
    }

    public static void showError(Component compo, String message) {
        JOptionPane.showMessageDialog(compo, message, MetaInformation.FULL_NAME, JOptionPane.ERROR_MESSAGE);
    }
}
