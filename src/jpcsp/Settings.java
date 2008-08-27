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

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import jpcsp.Controller.keyCode;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 *
 * @author shadow
 */
public class Settings {

    private static Settings instance = null;

    public static Settings get_instance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    public void NullSettings() {
        instance = null;
    }

    public int[] readWindowPos(String windowname) {
        int[] coord = new int[2];
        try {
            // Build the document with SAX and Xerces, no validation
            SAXBuilder builder = new SAXBuilder();
            // Create the document
            Document doc = builder.build(new File("Settings.xml"));
            Element webapp = doc.getRootElement();
            coord[0] = Integer.parseInt(webapp.getChild("guisettings").getChild("windowspos").getChild(windowname).getChild("x").getText());
            coord[1] = Integer.parseInt(webapp.getChild("guisettings").getChild("windowspos").getChild(windowname).getChild("y").getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return coord;
    }
	
	public int[] readWindowSize(String windowname) {
        int[] dimension = new int[2];
        try {
            // Build the document with SAX and Xerces, no validation
            SAXBuilder builder = new SAXBuilder();
            // Create the document
            Document doc = builder.build(new File("Settings.xml"));
            Element webapp = doc.getRootElement();
            dimension[0] = Integer.parseInt(webapp.getChild("guisettings").getChild("windowsize").getChild(windowname).getChild("x").getText());
            dimension[1] = Integer.parseInt(webapp.getChild("guisettings").getChild("windowsize").getChild(windowname).getChild("y").getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dimension;
    }

    public void writeWindowPos(String windowname, String[] pos) {
        try {

            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new File("Settings.xml"));
            Element webapp = doc.getRootElement();
            webapp.getChild("guisettings").getChild("windowspos").getChild(windowname).getChild("x").setText(pos[0]);
            webapp.getChild("guisettings").getChild("windowspos").getChild(windowname).getChild("y").setText(pos[1]);
            XMLOutputter xmloutputter = new XMLOutputter();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream("Settings.xml");
                xmloutputter.output(doc, fileOutputStream);
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	public void writeWindowSize(String windowname, String[] dimension) {
        try {

            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new File("Settings.xml"));
            Element webapp = doc.getRootElement();
            webapp.getChild("guisettings").getChild("windowsize").getChild(windowname).getChild("x").setText(dimension[0]);
            webapp.getChild("guisettings").getChild("windowsize").getChild(windowname).getChild("y").setText(dimension[1]);
            XMLOutputter xmloutputter = new XMLOutputter();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream("Settings.xml");
                xmloutputter.output(doc, fileOutputStream);
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
    public boolean readBoolEmuoptions(String option)
    {
        int value=0;
        try {
            // Build the document with SAX and Xerces, no validation
            SAXBuilder builder = new SAXBuilder();
            // Create the document
            Document doc = builder.build(new File("Settings.xml"));
            Element webapp = doc.getRootElement();
            value =Integer.parseInt(webapp.getChild("emuoptions").getChild(option).getText());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(value==1) return true;
        else return false;
    }
    public void writeBoolEmuoptions(String option,boolean value)
    {
            String state = "0";
            if(value) state = "1";
            try {

            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new File("Settings.xml"));
            Element webapp = doc.getRootElement();
            webapp.getChild("emuoptions").getChild(option).setText(state);  
            XMLOutputter xmloutputter = new XMLOutputter();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream("settings.xml");
                xmloutputter.output(doc, fileOutputStream);
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    public HashMap<Integer, keyCode> loadKeys() {
        HashMap<Integer, keyCode> m = new HashMap<Integer, keyCode>(22);
        
        m.put(readKeyOption("up"), keyCode.UP);
        m.put(readKeyOption("down"), keyCode.DOWN);
        m.put(readKeyOption("left"), keyCode.LEFT);
        m.put(readKeyOption("right"), keyCode.RIGHT);
        m.put(readKeyOption("analogUp"), keyCode.ANUP);
        m.put(readKeyOption("analogDown"), keyCode.ANDOWN);
        m.put(readKeyOption("analogLeft"), keyCode.ANLEFT);
        m.put(readKeyOption("analogRight"), keyCode.ANRIGHT);
        m.put(readKeyOption("start"), keyCode.START);
        m.put(readKeyOption("select"), keyCode.SELECT);
        m.put(readKeyOption("triangle"), keyCode.TRIANGLE);
        m.put(readKeyOption("square"), keyCode.SQUARE);
        m.put(readKeyOption("circle"), keyCode.CIRCLE);
        m.put(readKeyOption("cross"), keyCode.CROSS);
        m.put(readKeyOption("lTrigger"), keyCode.L1);
        m.put(readKeyOption("rTrigger"), keyCode.R1);
        m.put(readKeyOption("home"), keyCode.HOME);
        m.put(readKeyOption("hold"), keyCode.HOLD);
        m.put(readKeyOption("volPlus"), keyCode.VOLPLUS);
        m.put(readKeyOption("volMin"), keyCode.VOLMIN);
        m.put(readKeyOption("screen"), keyCode.SCREEN);
        m.put(readKeyOption("music"), keyCode.MUSIC);
        
        return m;
    }
    
    public void writeKeys(HashMap<Integer, keyCode> keys) {
        Iterator iter = keys.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry<Integer, keyCode> entry = (Map.Entry)iter.next();
            keyCode key = (keyCode)entry.getValue();
            int value = (Integer)entry.getKey();
            
            switch (key) {
                case DOWN:      writeKeyOption("down", value); break;
                case UP:        writeKeyOption("up", value); break;
                case LEFT:      writeKeyOption("left", value); break;
                case RIGHT:     writeKeyOption("right", value); break;
                case ANDOWN:    writeKeyOption("analogDown", value); break;
                case ANUP:      writeKeyOption("analogUp", value); break;
                case ANLEFT:    writeKeyOption("analogLeft", value); break;
                case ANRIGHT:   writeKeyOption("analogRight", value); break;
            
                case TRIANGLE:  writeKeyOption("triangle", value); break;
                case SQUARE:    writeKeyOption("square", value); break;
                case CIRCLE:    writeKeyOption("circle", value); break;
                case CROSS:     writeKeyOption("cross", value); break;
                case L1:        writeKeyOption("lTrigger", value); break;
                case R1:        writeKeyOption("rTrigger", value); break;
                case START:     writeKeyOption("start", value); break;
                case SELECT:    writeKeyOption("select", value); break;
                
                case HOME:      writeKeyOption("home", value); break;
                case HOLD:      writeKeyOption("hold", value); break;
                case VOLMIN:    writeKeyOption("volMin", value); break;
                case VOLPLUS:   writeKeyOption("volPlus", value); break;
                case SCREEN:    writeKeyOption("screen", value); break;
                case MUSIC:     writeKeyOption("music", value); break;
                        
                default: break;
            }
        }
    }
    
    private int readKeyOption(String keyName) {
        int r = KeyEvent.VK_UNDEFINED;
        
        try {
            // Build the document with SAX and Xerces, no validation
            SAXBuilder builder = new SAXBuilder();
            // Create the document
            Document doc = builder.build(new File("Settings.xml"));
            Element webapp = doc.getRootElement();
            r = Integer.parseInt(webapp.getChild("emuoptions").getChild("keys").getChild(keyName).getText());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return r;
    }
    
    public void writeKeyOption(String keyName, int key) {
        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new File("Settings.xml"));
            Element webapp = doc.getRootElement();
            webapp.getChild("emuoptions").getChild("keys").getChild(keyName).setText(Integer.toString(key));
            XMLOutputter xmloutputter = new XMLOutputter();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream("Settings.xml");
                xmloutputter.output(doc, fileOutputStream);
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
