package com.twilight.h264.player;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JPanel;

public class PlayerFrame extends JPanel {
	public Image lastFrame;
	
	public void paint(Graphics g) {
		Image img = lastFrame;
		if(img != null) {
			g.drawImage(lastFrame, 0, 0, img.getWidth(this), img.getHeight(this), this);
		} // if
	}

}
