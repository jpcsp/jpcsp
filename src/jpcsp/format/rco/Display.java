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
package jpcsp.format.rco;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;

import org.apache.log4j.Logger;

import jpcsp.format.RCO;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXNativeObject;
import jpcsp.format.rco.vsmx.objects.BaseNativeObject;
import jpcsp.format.rco.vsmx.objects.MoviePlayer;

public class Display extends JComponent {
	private static final long serialVersionUID = 5488196052725313236L;
	private static final Logger log = RCO.log;
	private List<IDisplay> objects;
	private IDisplay focus;
	private float moviePlayerWidth = (float) MoviePlayer.DEFAULT_WIDTH;
	private float moviePlayerHeight = (float) MoviePlayer.DEFAULT_HEIGHT;

	public Display() {
		objects = new LinkedList<IDisplay>();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (objects.size() > 0 && log.isTraceEnabled()) {
			log.trace(String.format("Starting to paint Display with %d objects, focus=%s", objects.size(), focus));
			log.trace(String.format("display %dx%d", getWidth(), getHeight()));
		}

		for (IDisplay object : objects) {
			paint(g, object);
		}
	}

	private void paint(Graphics g, IDisplay object) {
		int cx = object.getX();
		int cy = object.getY();
		int width = object.getWidth();
		int height = object.getHeight();
		int x = cx - width / 2;
		int y = cy + height / 2;

		BufferedImage image = object.getAnimImage();

		float displayWidth = getWidth();
		float displayHeight = getHeight();
		x = Math.round(x / (moviePlayerWidth / 2f) * (displayWidth / 2f) + (displayWidth / 2f));
		y = Math.round(-y / (moviePlayerHeight / 2f) * (displayHeight / 2f) + (displayHeight / 2f));
		width = Math.round(width / (moviePlayerWidth / displayWidth));
		height = Math.round(height / (moviePlayerWidth / displayWidth));

		if (log.isTraceEnabled()) {
			log.trace(String.format("paint at (%d,%d) %dx%d - image=%dx%d, object=%s", x, y, width, height, image == null ? 0 : image.getWidth(), image == null ? 0 : image.getHeight(), object));
		}

		if (image != null) {
			g.drawImage(image, x, y, width, height, null);
		} else {
			g.setColor(Color.BLACK);
			g.drawRect(x, y, width, height);
		}

		if (focus == object) {
			g.setColor(Color.RED);
			g.drawRect(x, y, width, height);
		}
	}

	public void add(IDisplay object) {
		objects.add(object);
	}

	public void add(BaseNativeObject object) {
		if (object instanceof IDisplay) {
			add((IDisplay) object);
		}
	}

	public void add(VSMXBaseObject object) {
		if (object instanceof VSMXNativeObject) {
			add(((VSMXNativeObject) object).getObject());
		}
	}

	public void remove(IDisplay object) {
		objects.remove(object);
	}

	public void remove(BaseNativeObject object) {
		if (object instanceof IDisplay) {
			remove((IDisplay) object);
		}
	}

	public void remove(VSMXBaseObject object) {
		if (object instanceof VSMXNativeObject) {
			remove(((VSMXNativeObject) object).getObject());
		}
	}

	public void setFocus(IDisplay focus) {
		if (this.focus != focus) {
			this.focus = focus;
			if (log.isDebugEnabled()) {
				log.debug(String.format("Display.setFocus %s", focus));
			}
			repaint();
		}
	}

	public void setFocus(BaseNativeObject focus) {
		if (focus instanceof IDisplay) {
			setFocus((IDisplay) focus);
		}
	}

	public void setFocus(VSMXBaseObject focus) {
		if (focus instanceof VSMXNativeObject) {
			setFocus(((VSMXNativeObject) focus).getObject());
		}
	}
}
