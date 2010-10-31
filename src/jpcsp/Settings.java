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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jpcsp.Controller.keyCode;
import jpcsp.GUI.RecentElement;
import jpcsp.util.Utilities;

/**
 *
 * @author spip2001
 */
public class Settings {
	private final static String SETTINGS_FILE_NAME = "Settings.properties";
	private final static String DEFAULT_SETTINGS_FILE_NAME = "/jpcsp/DefaultSettings.properties";
	private static Settings instance = null;
	private Properties defaultSettings;
	private SortedProperties loadedSettings;

	public static Settings getInstance() {
		if (instance == null) instance = new Settings();
		return instance;
	}

	public void NullSettings() {
		instance = null;
	}

	private Settings() {
		defaultSettings = new Properties();
        InputStream defaultSettingsStream = null, loadedSettingsStream = null;
		try {
			defaultSettingsStream = getClass().getResourceAsStream(DEFAULT_SETTINGS_FILE_NAME);
			defaultSettings.load(defaultSettingsStream);
			loadedSettings = new SortedProperties(defaultSettings);
			File settingsFile = new File(SETTINGS_FILE_NAME);
			settingsFile.createNewFile();
            loadedSettingsStream = new BufferedInputStream(new FileInputStream(settingsFile));
			loadedSettings.load(loadedSettingsStream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			Utilities.close(defaultSettingsStream, loadedSettingsStream);
		}
	}

	/**
	 * Write settings in file
	 *
	 * @param doc
	 *        Settings as XML document
	 */
	private void writeSettings() {
                BufferedOutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(SETTINGS_FILE_NAME));
			loadedSettings.store(out, null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			Utilities.close(out);
        }
	}

	public Point readWindowPos(String windowname) {
		String x = loadedSettings.getProperty("gui.windows." + windowname + ".x");
		String y = loadedSettings.getProperty("gui.windows." + windowname + ".y");

		Point position = new Point();
		position.x = x != null ? Integer.parseInt(x) : 0;
		position.y = y != null ? Integer.parseInt(y) : 0;

		return position;
	}

	public Dimension readWindowSize(String windowname, int defaultWidth, int defaultHeight) {
		String w = loadedSettings.getProperty("gui.windows." + windowname + ".w");
		String h = loadedSettings.getProperty("gui.windows." + windowname + ".h");

		Dimension dimension = new Dimension();
		dimension.width = w != null ? Integer.parseInt(w) : defaultWidth;
		dimension.height = h != null ? Integer.parseInt(h) : defaultHeight;

		return dimension;
	}

	public void writeWindowPos(String windowname, Point pos) {
		loadedSettings.setProperty("gui.windows." + windowname + ".x", Integer.toString(pos.x));
		loadedSettings.setProperty("gui.windows." + windowname + ".y", Integer.toString(pos.y));
		writeSettings();
	}

	public void writeWindowSize(String windowname, Dimension dimension) {
		loadedSettings.setProperty("gui.windows." + windowname + ".w", Integer.toString(dimension.width));
		loadedSettings.setProperty("gui.windows." + windowname + ".h", Integer.toString(dimension.height));
		writeSettings();
	}

	public boolean readBool(String option) {
		String bool = loadedSettings.getProperty(option);
		if(bool == null) return false;

		return Integer.parseInt(bool) != 0;
	}

	public int readInt(String option) {
		String value = loadedSettings.getProperty(option);
		if (value == null) return 0;

		return Integer.parseInt(value);
	}

	public void writeBool(String option, boolean value) {
		String state = value ? "1" : "0";
		loadedSettings.setProperty(option, state);
		writeSettings();
	}

	public void writeInt(String option, int value) {
		String state = Integer.toString(value);
		loadedSettings.setProperty(option, state);
		writeSettings();
	}

	public String readString(String option) {
            return loadedSettings.getProperty(option, "");
	}

	public void writeString(String option, String value) {
		loadedSettings.setProperty(option, value);
		writeSettings();
	}

	public HashMap<Integer, keyCode> loadKeys() {
		HashMap<Integer, keyCode> m = new HashMap<Integer, keyCode>(22);

		m.put(readKey("up"), keyCode.UP);
		m.put(readKey("down"), keyCode.DOWN);
		m.put(readKey("left"), keyCode.LEFT);
		m.put(readKey("right"), keyCode.RIGHT);
		m.put(readKey("analogUp"), keyCode.ANUP);
		m.put(readKey("analogDown"), keyCode.ANDOWN);
		m.put(readKey("analogLeft"), keyCode.ANLEFT);
		m.put(readKey("analogRight"), keyCode.ANRIGHT);
		m.put(readKey("start"), keyCode.START);
		m.put(readKey("select"), keyCode.SELECT);
		m.put(readKey("triangle"), keyCode.TRIANGLE);
		m.put(readKey("square"), keyCode.SQUARE);
		m.put(readKey("circle"), keyCode.CIRCLE);
		m.put(readKey("cross"), keyCode.CROSS);
		m.put(readKey("lTrigger"), keyCode.L1);
		m.put(readKey("rTrigger"), keyCode.R1);
		m.put(readKey("home"), keyCode.HOME);
		m.put(readKey("hold"), keyCode.HOLD);
		m.put(readKey("volPlus"), keyCode.VOLPLUS);
		m.put(readKey("volMin"), keyCode.VOLMIN);
		m.put(readKey("screen"), keyCode.SCREEN);
		m.put(readKey("music"), keyCode.MUSIC);

		return m;
	}

	public void writeKeys(HashMap<Integer, keyCode> keys) {
		Iterator<Map.Entry<Integer, keyCode>> iter = keys.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Integer, keyCode> entry = iter.next();
			keyCode key = entry.getValue();
			int value = entry.getKey();

			switch (key) {
			case DOWN:
				writeKey("down", value);
				break;
			case UP:
				writeKey("up", value);
				break;
			case LEFT:
				writeKey("left", value);
				break;
			case RIGHT:
				writeKey("right", value);
				break;
			case ANDOWN:
				writeKey("analogDown", value);
				break;
			case ANUP:
				writeKey("analogUp", value);
				break;
			case ANLEFT:
				writeKey("analogLeft", value);
				break;
			case ANRIGHT:
				writeKey("analogRight", value);
				break;

			case TRIANGLE:
				writeKey("triangle", value);
				break;
			case SQUARE:
				writeKey("square", value);
				break;
			case CIRCLE:
				writeKey("circle", value);
				break;
			case CROSS:
				writeKey("cross", value);
				break;
			case L1:
				writeKey("lTrigger", value);
				break;
			case R1:
				writeKey("rTrigger", value);
				break;
			case START:
				writeKey("start", value);
				break;
			case SELECT:
				writeKey("select", value);
				break;

			case HOME:
				writeKey("home", value);
				break;
			case HOLD:
				writeKey("hold", value);
				break;
			case VOLMIN:
				writeKey("volMin", value);
				break;
			case VOLPLUS:
				writeKey("volPlus", value);
				break;
			case SCREEN:
				writeKey("screen", value);
				break;
			case MUSIC:
				writeKey("music", value);
				break;

			default:
				break;
			}
		}
		writeSettings();
	}

	private int readKey(String keyName) {
		String str = loadedSettings.getProperty("keys." + keyName);
		if(str == null)
			return KeyEvent.VK_UNDEFINED;
		return Integer.parseInt(str);
	}

	private void writeKey(String keyName, int key) {
		loadedSettings.setProperty("keys." + keyName, Integer.toString(key));
	}

	private static class SortedProperties extends Properties {

		private static final long serialVersionUID = -8127868945637348944L;

		public SortedProperties(Properties defaultSettings) {
			super(defaultSettings);
		}

		@Override
		@SuppressWarnings("unchecked")
		public synchronized Enumeration keys() {
			Enumeration keysEnum = super.keys();
			List keyList = Collections.list(keysEnum);
                        Collections.sort(keyList);
			return Collections.enumeration(keyList);
		}
	}

	public void readRecent(String cat, List<RecentElement> recent) {
		for(int i = 0;; ++i) {
    		String r = loadedSettings.getProperty("gui.recent." + cat + "." + i);
    		if(r == null) break;
    		String title = loadedSettings.getProperty("gui.recent." + cat + "." + i + ".title");
    		recent.add(new RecentElement(r, title));
    	}
	}

	@SuppressWarnings("unchecked")
	public void writeRecent(String cat, List<RecentElement> recent) {
		Enumeration<String> keys = loadedSettings.keys();
		while(keys.hasMoreElements()) {
			String key = keys.nextElement();
			if(key.startsWith("gui.recent." + cat))
                            loadedSettings.remove(key);
		}
                int index = 0;
		for(RecentElement elem : recent) {
			loadedSettings.setProperty("gui.recent." + cat + "." + index, elem.path);
			if(elem.title != null)
                            loadedSettings.setProperty("gui.recent." + cat + "." + index + ".title", elem.title);
                        index++;
		}
		writeSettings();
	}

    /**
     * Reads the following settings:
     * gui.memStickBrowser.font.name=SansSerif
     * gui.memStickBrowser.font.file=
     * gui.memStickBrowser.font.size=11
     * @return      Tries to return a font in this order:
     *              - Font from local file (somefont.ttf),
     *              - Font registered with the operating system,
     *              - SansSerif, Plain, 11.
     */
    private Font loadedFont = null;
    public Font getFont() {
        if (loadedFont != null) {
            return loadedFont;
        }

        Font font = new Font("SansSerif", Font.PLAIN, 1);
        int fontsize = 11;

        try {
            Font base = font; // Default font
            String fontname = readString("gui.font.name");
            String fontfilename = readString("gui.font.file");
            String fontsizestr = readString("gui.font.size");

            if (fontfilename.length() != 0) {
                // Load file font
                File fontfile = new File(fontfilename);
                if (fontfile.exists()) {
                    base = Font.createFont(Font.TRUETYPE_FONT, fontfile);
                } else {
                    System.err.println("gui.font.file '" + fontfilename + "' doesn't exist.");
                }
            } else if (fontname.length() != 0) {
                // Load system font
                base = new Font(fontname, Font.PLAIN, 1);
            }

            // Set font size
            if (fontsizestr.length() > 0) fontsize = Integer.parseInt(fontsizestr);
            else System.err.println("gui.font.size setting is missing.");

            font = base.deriveFont(Font.PLAIN, fontsize);

            // register font as a font family so we can use it in StyledDocument's
            java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(base);
        } catch(NumberFormatException e) {
            System.err.println("gui.font.size setting is invalid.");
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

        loadedFont = font;
        return font;
    }
}