package jpcsp.GUI;

import java.awt.DisplayMode;
import java.awt.Rectangle;
import java.awt.Window;

public interface IMainGUI {
	void setMainTitle(String title);
	void RefreshButtons();
	void setLocation();
	public DisplayMode getDisplayMode();
	boolean isFullScreen();
	boolean isVisible();
	void pack();
	void setFullScreenDisplaySize();

	/**
     * Display a new window in front of the main window.
     * If the main window is the full screen window, disable the full screen mode
     * so that the new window can be displayed (no other window can be displayed
     * in front of a full screen window).
     * 
     * @param window     the window to be displayed
     */
	void startWindowDialog(Window window);

	/**
     * Display a new window but keep the focus on the main window.
     * If the main window is the full screen window, disable the full screen mode
     * so that the new window can be displayed (no other window can be displayed
     * in front of a full screen window).
     * 
     * @param window     the window to be displayed
     */
	void startBackgroundWindowDialog(Window window);

	/**
     * Restore the full screen window if required.
     */
	void endWindowDialog();

	Rectangle getCaptureRectangle();
	void onUmdChange();

	public void setDisplayMinimumSize(int width, int height);
	public void setDisplaySize(int width, int height);
}
