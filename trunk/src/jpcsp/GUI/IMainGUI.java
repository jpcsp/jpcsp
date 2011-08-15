package jpcsp.GUI;

import java.awt.DisplayMode;
import java.awt.Window;

public interface IMainGUI {
	void setMainTitle(String title);
	void RefreshButtons();
	void setLocation();
	public DisplayMode getDisplayMode();
	void endWindowDialog();
	boolean isFullScreen();
	boolean isVisible();
	void pack();
	void setFullScreenDisplaySize();
	void startWindowDialog(Window window);
}
