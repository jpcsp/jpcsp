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
package jpcsp.settings;

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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;

import jpcsp.Emulator;
import jpcsp.State;
import jpcsp.Controller.keyCode;
import jpcsp.GUI.RecentElement;
import jpcsp.util.Utilities;

/**
 *
 * @author spip2001, gid15
 */
public class Settings {
	private final static String SETTINGS_FILE_NAME = "Settings.properties";
	private final static String DEFAULT_SETTINGS_FILE_NAME = "/jpcsp/DefaultSettings.properties";
	private static Settings instance = null;
	private Properties defaultSettings;
	private SortedProperties loadedSettings;
	private Properties patchSettings;
	private HashMap<String, List<ISettingsListener>> listenersByKey;
	private List<SettingsListenerInfo> allListeners;

	public static Settings getInstance() {
		if (instance == null) {
			instance = new Settings();
		}
		return instance;
	}

	private Settings() {
		listenersByKey = new HashMap<String, List<ISettingsListener>>();
		allListeners = new LinkedList<SettingsListenerInfo>();
		defaultSettings = new Properties();
		patchSettings = new Properties();
        InputStream defaultSettingsStream = null;
        InputStream loadedSettingsStream = null;
		try {
			defaultSettingsStream = getClass().getResourceAsStream(DEFAULT_SETTINGS_FILE_NAME);
			defaultSettings.load(defaultSettingsStream);
			loadedSettings = new SortedProperties(defaultSettings);
			File settingsFile = new File(SETTINGS_FILE_NAME);
			settingsFile.createNewFile();
            loadedSettingsStream = new BufferedInputStream(new FileInputStream(settingsFile));
			loadedSettings.load(loadedSettingsStream);
		} catch (FileNotFoundException e) {
			Emulator.log.error("Settings file not found:", e);
		} catch (IOException e) {
			Emulator.log.error("Problem loading settings:", e);
		} finally{
			Utilities.close(defaultSettingsStream, loadedSettingsStream);
		}
	}

	public void loadPatchSettings() {
		Properties previousPatchSettings = new Properties(patchSettings);
		patchSettings.clear();

		String discId = State.discId;
		if (discId != State.DISCID_UNKNOWN_FILE && discId != State.DISCID_UNKNOWN_NOTHING_LOADED) {
			String patchFileName = String.format("patches/%s.properties", discId);
			File patchFile = new File(patchFileName);
			InputStream patchSettingsStream = null;
			try {
				patchSettingsStream = new BufferedInputStream(new FileInputStream(patchFile));
				patchSettings.load(patchSettingsStream);
				Emulator.log.info(String.format("Overwriting default settings with patch file '%s'", patchFileName));
			} catch (FileNotFoundException e) {
				Emulator.log.debug(String.format("Patch file not found: %s", e.toString()));
			} catch (IOException e) {
				Emulator.log.error("Problem loading patch:", e);
			} finally {
				Utilities.close(patchSettingsStream);
			}
		}

		// Trigger the settings listener for all values modified
		// by the new patch settings.
		for (Enumeration<Object> e = patchSettings.keys(); e.hasMoreElements(); ) {
			String key = e.nextElement().toString();
			previousPatchSettings.remove(key);
			String value = patchSettings.getProperty(key);
			if (!value.equals(loadedSettings.getProperty(key))) {
				triggerSettingsListener(key, value);
			}
		}

		// Trigger the settings listener for all values that disappeared from the
		// previous patch settings.
		for (Enumeration<Object> e = previousPatchSettings.keys(); e.hasMoreElements(); ) {
			String key = e.nextElement().toString();
			String oldValue = previousPatchSettings.getProperty(key);
			String newValue = getProperty(key);
			if (!oldValue.equals(newValue)) {
				triggerSettingsListener(key, newValue);
			}
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
			Emulator.log.error("Settings file not found:", e);
		} catch (IOException e) {
			Emulator.log.error("Problem saving settings:", e);
		} finally {
			Utilities.close(out);
        }
	}

	private String getProperty(String key) {
		String value = patchSettings.getProperty(key);
		if (value == null) {
			value = loadedSettings.getProperty(key);
		}

		return value;
	}

	private String getProperty(String key, String defaultValue) {
		String value = patchSettings.getProperty(key);
		if (value == null) {
			value = loadedSettings.getProperty(key, defaultValue);
		}

		return value;
	}

	private void setProperty(String key, String value) {
		String previousValue = getProperty(key);

		// Store the value in the loadedSettings,
		// the patchSettings staying unchanged.
		loadedSettings.setProperty(key, value);

		// Retrieve the new value (might be different from the value
		// just set in the loadedSettings as it might be overwritten by
		// a patchSettings).
		String newValue = getProperty(key);

		// Trigger the settings listener if this resulted in a changed value
		if (previousValue == null || !previousValue.equals(newValue)) {
			triggerSettingsListener(key, newValue);
		}
	}

	public Point readWindowPos(String windowname) {
		String x = getProperty("gui.windows." + windowname + ".x");
		String y = getProperty("gui.windows." + windowname + ".y");

		Point position = new Point();
		position.x = x != null ? Integer.parseInt(x) : 0;
		position.y = y != null ? Integer.parseInt(y) : 0;

		return position;
	}

	public Dimension readWindowSize(String windowname, int defaultWidth, int defaultHeight) {
		String w = getProperty("gui.windows." + windowname + ".w");
		String h = getProperty("gui.windows." + windowname + ".h");

		Dimension dimension = new Dimension();
		dimension.width = w != null ? Integer.parseInt(w) : defaultWidth;
		dimension.height = h != null ? Integer.parseInt(h) : defaultHeight;

		return dimension;
	}

	public void writeWindowPos(String windowname, Point pos) {
		setProperty("gui.windows." + windowname + ".x", Integer.toString(pos.x));
		setProperty("gui.windows." + windowname + ".y", Integer.toString(pos.y));
		writeSettings();
	}

	public void writeWindowSize(String windowname, Dimension dimension) {
		setProperty("gui.windows." + windowname + ".w", Integer.toString(dimension.width));
		setProperty("gui.windows." + windowname + ".h", Integer.toString(dimension.height));
		writeSettings();
	}

	public static boolean parseBool(String value) {
		return Integer.parseInt(value) != 0;
	}

	public static int parseInt(String value) {
		return Integer.parseInt(value);
	}

	public static float parseFloat(String value) {
		return Float.parseFloat(value);
	}

	public boolean readBool(String option) {
		String bool = getProperty(option);
		if (bool == null) {
			return false;
		}

		return parseBool(bool);
	}

	public int readInt(String option) {
		return readInt(option, 0);
	}

	public int readInt(String option, int defaultValue) {
		String value = getProperty(option);
		if (value == null) {
			return defaultValue;
		}

		return parseInt(value);
	}

	public void writeBool(String option, boolean value) {
		String state = value ? "1" : "0";
		setProperty(option, state);
		writeSettings();
	}

	public void writeInt(String option, int value) {
		String state = Integer.toString(value);
		setProperty(option, state);
		writeSettings();
	}

	public String readString(String option) {
		return readString(option, "");
	}

	public String readString(String option, String defaultValue) {
		return getProperty(option, defaultValue);
	}

	public boolean isOptionFromPatch(String option) {
		return patchSettings.containsKey(option);
	}

	public void writeString(String option, String value) {
		setProperty(option, value);
		writeSettings();
	}

	public void writeFloat(String option, float value) {
		String state = Float.toString(value);
		setProperty(option, state);
		writeSettings();
	}

	public float readFloat(String option, float defaultValue) {
		String value = getProperty(option);
		if (value == null) {
			return defaultValue;
		}

		return parseFloat(value);
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

	public HashMap<keyCode, String> loadController() {
		HashMap<keyCode, String> m = new HashMap<keyCode, String>(22);

		m.put(keyCode.UP, readController("up"));
		m.put(keyCode.DOWN, readController("down"));
		m.put(keyCode.LEFT, readController("left"));
		m.put(keyCode.RIGHT, readController("right"));
		m.put(keyCode.ANUP, readController("analogUp"));
		m.put(keyCode.ANDOWN, readController("analogDown"));
		m.put(keyCode.ANLEFT, readController("analogLeft"));
		m.put(keyCode.ANRIGHT, readController("analogRight"));
		m.put(keyCode.START, readController("start"));
		m.put(keyCode.SELECT, readController("select"));
		m.put(keyCode.TRIANGLE, readController("triangle"));
		m.put(keyCode.SQUARE, readController("square"));
		m.put(keyCode.CIRCLE, readController("circle"));
		m.put(keyCode.CROSS, readController("cross"));
		m.put(keyCode.L1, readController("lTrigger"));
		m.put(keyCode.R1, readController("rTrigger"));
		m.put(keyCode.HOME, readController("home"));
		m.put(keyCode.HOLD, readController("hold"));
		m.put(keyCode.VOLPLUS, readController("volPlus"));
		m.put(keyCode.VOLMIN, readController("volMin"));
		m.put(keyCode.SCREEN, readController("screen"));
		m.put(keyCode.MUSIC, readController("music"));

		// Removed unset entries
		for (keyCode key : keyCode.values()) {
			if (m.get(key) == null) {
				m.remove(key);
			}
		}

		return m;
	}

	public void writeKeys(HashMap<Integer, keyCode> keys) {
		for (Map.Entry<Integer, keyCode> entry : keys.entrySet()) {
			keyCode key = entry.getValue();
			int value = entry.getKey();

			switch (key) {
				case DOWN:	   writeKey("down", value); break;
				case UP:	   writeKey("up", value); break;
				case LEFT:	   writeKey("left", value); break;
				case RIGHT:	   writeKey("right", value); break;
				case ANDOWN:   writeKey("analogDown", value); break;
				case ANUP:     writeKey("analogUp", value); break;
				case ANLEFT:   writeKey("analogLeft", value); break;
				case ANRIGHT:  writeKey("analogRight", value); break;
				case TRIANGLE: writeKey("triangle", value); break;
				case SQUARE:   writeKey("square", value); break;
				case CIRCLE:   writeKey("circle", value); break;
				case CROSS:    writeKey("cross", value); break;
				case L1:       writeKey("lTrigger", value); break;
				case R1:       writeKey("rTrigger", value); break;
				case START:    writeKey("start", value); break;
				case SELECT:   writeKey("select", value); break;
				case HOME:     writeKey("home", value); break;
				case HOLD:     writeKey("hold", value); break;
				case VOLMIN:   writeKey("volMin", value); break;
				case VOLPLUS:  writeKey("volPlus", value); break;
				case SCREEN:   writeKey("screen", value); break;
				case MUSIC:    writeKey("music", value); break;
				case RELEASED: break;
			}
		}
		writeSettings();
	}

	public void writeController(HashMap<keyCode, String> keys) {
		for (Map.Entry<keyCode, String> entry : keys.entrySet()) {
			keyCode key = entry.getKey();
			String value = entry.getValue();

			switch (key) {
				case DOWN:	   writeController("down", value); break;
				case UP:	   writeController("up", value); break;
				case LEFT:	   writeController("left", value); break;
				case RIGHT:	   writeController("right", value); break;
				case ANDOWN:   writeController("analogDown", value); break;
				case ANUP:     writeController("analogUp", value); break;
				case ANLEFT:   writeController("analogLeft", value); break;
				case ANRIGHT:  writeController("analogRight", value); break;
				case TRIANGLE: writeController("triangle", value); break;
				case SQUARE:   writeController("square", value); break;
				case CIRCLE:   writeController("circle", value); break;
				case CROSS:    writeController("cross", value); break;
				case L1:       writeController("lTrigger", value); break;
				case R1:       writeController("rTrigger", value); break;
				case START:    writeController("start", value); break;
				case SELECT:   writeController("select", value); break;
				case HOME:     writeController("home", value); break;
				case HOLD:     writeController("hold", value); break;
				case VOLMIN:   writeController("volMin", value); break;
				case VOLPLUS:  writeController("volPlus", value); break;
				case SCREEN:   writeController("screen", value); break;
				case MUSIC:    writeController("music", value); break;
				case RELEASED: break;
			}
		}
		writeSettings();
	}

	private int readKey(String keyName) {
		String str = getProperty("keys." + keyName);
		if (str == null) {
			return KeyEvent.VK_UNDEFINED;
		}
		return Integer.parseInt(str);
	}

	private void writeKey(String keyName, int key) {
		setProperty("keys." + keyName, Integer.toString(key));
	}

	private String readController(String name) {
		return getProperty("controller." + name);
	}

	private void writeController(String name, String value) {
		setProperty("controller." + name, value);
	}

	private static class SortedProperties extends Properties {
		private static final long serialVersionUID = -8127868945637348944L;

		public SortedProperties(Properties defaultSettings) {
			super(defaultSettings);
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public synchronized Enumeration keys() {
			Enumeration keysEnum = super.keys();
			List keyList = Collections.list(keysEnum);
			Collections.sort(keyList);

			return Collections.enumeration(keyList);
		}
	}

	public void readRecent(String cat, List<RecentElement> recent) {
		for(int i = 0;; ++i) {
    		String r = getProperty("gui.recent." + cat + "." + i);
    		if (r == null) {
    			break;
    		}
    		String title = getProperty("gui.recent." + cat + "." + i + ".title");
    		recent.add(new RecentElement(r, title));
    	}
	}

	@SuppressWarnings("unchecked")
	public void writeRecent(String cat, List<RecentElement> recent) {
		Enumeration<String> keys = loadedSettings.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			if (key.startsWith("gui.recent." + cat)) {
                loadedSettings.remove(key);
			}
		}
		int index = 0;
		for (RecentElement elem : recent) {
			setProperty("gui.recent." + cat + "." + index, elem.path);
			if (elem.title != null) {
                setProperty("gui.recent." + cat + "." + index + ".title", elem.title);
			}
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

    /**
     * Register a settings listener for a specific option.
     * The settings listener will be called as soon as the option value changes,
     * e.g. when modifying the configuration through the GUI, or when loading
     * a game having a patch file defined.
     * The settings listener is also called immediately by this method while
     * registering.
     *
     * Only one settings listener can be defined for each name/option combination.
     * This allows to call this method for the same listener multiple times and
     * have it registered only once.
     *
     * @param name      the name of the settings listener
     * @param option    the settings option
     * @param listener  the listener to be called when the settings option value changes
     */
    public void registerSettingsListener(String name, String option, ISettingsListener listener) {
    	removeSettingsListener(name, option);

    	SettingsListenerInfo info = new SettingsListenerInfo(name, option, listener);
    	allListeners.add(info);
    	List<ISettingsListener> listenersForKey = listenersByKey.get(option);
    	if (listenersForKey == null) {
    		listenersForKey = new LinkedList<ISettingsListener>();
    		listenersByKey.put(option, listenersForKey);
    	}
    	listenersForKey.add(listener);

    	// Trigger the settings listener immediately if a value is defined
    	String value = getProperty(option);
    	if (value != null) {
    		listener.settingsValueChanged(option, value);
    	}
    }

    /**
     * Remove the settings listeners matching the name and option parameters.
     * 
     * @param name     the name of the settings listener, or null to match any name
     * @param option   the settings open, or null to match any settings option
     */
    public void removeSettingsListener(String name, String option) {
    	for (ListIterator<SettingsListenerInfo> lit = allListeners.listIterator(); lit.hasNext(); ) {
    		SettingsListenerInfo info = lit.next();
    		if (info.equals(name, option)) {
    			lit.remove();
    			String key = info.getKey();
    			List<ISettingsListener> listenersForKey = listenersByKey.get(key);
    			listenersForKey.remove(info.getListener());
    			if (listenersForKey.isEmpty()) {
    				listenersByKey.remove(key);
    			}
    		}
    	}
    }

    /**
     * Remove all the settings listeners matching the name parameter.
     * 
     * @param name     the name of the settings listener, or null to match any name
     *                 (in which case all the settings listeners will be removed).
     */
    public void removeSettingsListener(String name) {
    	removeSettingsListener(name, null);
    }

    /**
     * Trigger the settings listener for a given settings key.
     * This method has to be called when the value of a settings key changes.
     * 
     * @param key     the key
     * @param value   the settings value
     */
    private void triggerSettingsListener(String key, String value) {
    	List<ISettingsListener> listenersForKey = listenersByKey.get(key);
    	if (listenersForKey != null) {
    		for (ISettingsListener listener : listenersForKey) {
    			listener.settingsValueChanged(key, value);
    		}
    	}
    }
}