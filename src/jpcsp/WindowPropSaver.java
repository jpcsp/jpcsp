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
package jpcsp;

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import jpcsp.settings.Settings;

/**
 * This class implements the automatic storing and loading of window positions
 * for Frames and Dialogs.
 *
 * @note Loading the window position only on the WINDOW_OPENED event might lead
 * to flicker. See the comment on loadWindowProperties() for details.
 *
 * @author tempura
 */
public class WindowPropSaver implements AWTEventListener {

    @Override
    public void eventDispatched(AWTEvent awte) {
        Window window = (Window) ((WindowEvent) awte).getComponent();
        switch (awte.getID()) {
            case WindowEvent.WINDOW_DEACTIVATED:
                onSavePosition(window);
                break;
            case WindowEvent.WINDOW_OPENED:
                // this is only a fall-back for windows which do not call
                // loadWindowProperties() on GUI construction time
                // failing to do so will lead to a short flicker, as the window
                // is placed at the default position and moved afterwards after
                // signaling the OPENED event
                onLoadPosition(window);
                break;
        }
    }

    /**
     * Put a call to this function into the Frame's contructor in order to
     * ensure a flicker-free placing of the Frame.
     *
     * @note Call this function as last line of the constructor.
     *
     * @param window The window to place and resize.
     */
    public static void loadWindowProperties(Window window) {
        onLoadPosition(window);
    }

    /**
     * Only use this for MainGUI - as this is the only window which can be
     * closed without being deactivated first.
     *
     * @param window The MainGUI window.
     */
    public static void saveWindowProperties(MainGUI mainGUI) {
        onSavePosition(mainGUI);
    }

    /**
     * This will set the window position to either the stored position or to the
     * screen center if not position was found.
     *
     * @note It uses the class name as identifier in the settings.
     *
     * @param frame The frame to initialise.
     * @param identifierForConfig The identifier used in the settings file.
     */
    private static void onLoadPosition(Window window) {
        if (!isWindowFrameOrDialog(window)) {
            return;
        }

        String identifierForConfig = window.getClass().getSimpleName();

        // do not load positions for standard dialogs (like file open)
        if (identifierForConfig.equals("JDialog")) {
            return;
        }

        // MainGUI needs special handling due to being able to go fullscreen
        if (identifierForConfig.equals("MainGUI") && Emulator.getMainGUI().isFullScreen()) {
            return;
        }

        if (Settings.getInstance().readBool("gui.saveWindowPos")
                && Settings.getInstance().readWindowPos(identifierForConfig) != null) {

            Emulator.log.debug("loading window position of '" + identifierForConfig + "'");

            // LogWindow needs special handling if it shall be attached to the MainGUI
            if (!(identifierForConfig.equals("LogWindow") && Settings.getInstance().readBool("gui.snapLogwindow"))) {
                window.setLocation(Settings.getInstance().readWindowPos(identifierForConfig));
            }

            // read the size only if the frame is resizeable
            if (isWindowResizeable(window)
                    && Settings.getInstance().readWindowSize(identifierForConfig) != null) {
                window.setSize(Settings.getInstance().readWindowSize(identifierForConfig));
            }
        } else {
            // show the frame simply centered
            window.setLocationRelativeTo(null);
        }
    }

    /**
     * Store the current position of the window.
     *
     * @note It uses the class name as identifier in the settings.
     *
     * @param frame The frame to initialise.
     * @param identifierForConfig The identifier used in the settings file.
     */
    private static void onSavePosition(Window window) {
        if (!isWindowFrameOrDialog(window)) {
            return;
        }

        if (Settings.getInstance().readBool("gui.saveWindowPos")) {
            String identifierForConfig = window.getClass().getSimpleName();

            // do not save positions for standard dialogs (like file open)
            if (identifierForConfig.equals("JDialog")) {
                return;
            }

            // MainGUI needs special handling due to being able to go fullscreen
            if (identifierForConfig.equals("MainGUI") && Emulator.getMainGUI().isFullScreen()) {
                return;
            }

            Emulator.log.debug("saving window position of '" + identifierForConfig + "'");

            Settings.getInstance().writeWindowPos(identifierForConfig, window.getLocation());

            // write the window size only if the window is resizeable
            if (isWindowResizeable(window)) {
                Settings.getInstance().writeWindowSize(identifierForConfig, window.getSize());
            }
        }
    }

    /**
     * Check if the given window object is resizeable.
     *
     * @note Only done for Frames and Dialogs.
     *
     * @param window The window instance to check.
     * @return true if the window is resizable, false otherwise.
     */
    private static boolean isWindowResizeable(Window window) {
        return (window instanceof Frame && ((Frame) window).isResizable())
                || (window instanceof Dialog && ((Dialog) window).isResizable());
    }

    /**
     * Check if the given window is an instance of Frame or Dialog.
     *
     * @param window The window instance to check.
     * @return true if the window is an instance of Frame or Dialog, else false.
     */
    private static boolean isWindowFrameOrDialog(Window window) {
        return (window instanceof Frame) || (window instanceof Dialog);
    }
}
