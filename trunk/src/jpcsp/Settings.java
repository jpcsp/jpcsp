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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import jpcsp.Controller.keyCode;

/**
*
* @author spip2001
*/
public class Settings {
   
   private final static String SETTINGS_FILE_NAME = "Settings.xml";

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
   
    /**
     * Reads a setting from the xml file and
     * returns it as a string.
     * @param path Path of the wanted value
     * @return
     */
    private String readXmlSetting(String path) {
       try {
          XPathFactory xpathFactory = XPathFactory.newInstance();
          XPath xpath = xpathFactory.newXPath();
          XPathExpression expr = xpath.compile(path);
          FileInputStream settingsIn = new FileInputStream(SETTINGS_FILE_NAME);
          InputSource source = new InputSource(settingsIn);
          String value = expr.evaluate(source);
          settingsIn.close();
          
          // System.out.println(path + " = " + value);
          
          return value;
       } catch (Exception e) {
          e.printStackTrace();
          return null;
       }
    }
   
    /**
     * Gets the xml document containing settings
     * @return
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    private Document getSettingsDocument() throws ParserConfigurationException, SAXException, IOException {
       DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
       DocumentBuilder builder= dbFactory.newDocumentBuilder();
       
        Document doc = builder.parse(new File(SETTINGS_FILE_NAME));
       
        return doc;
    }
   
    /**
     * Write settings in file
     * @param doc Settings as XML document
     */
    private void writeSettings(Document doc) {
       try {
            FileOutputStream fileOutputStream = new FileOutputStream(SETTINGS_FILE_NAME);
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(fileOutputStream));
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
         e.printStackTrace();
      } catch (TransformerException e) {
         e.printStackTrace();
      }
    }

    public int[] readWindowPos(String windowname) {
        int[] coord = new int[2];
        try {
           String x = readXmlSetting("//guisettings/windowspos/" + windowname + "/x");
           String y = readXmlSetting("//guisettings/windowspos/" + windowname + "/y");
            coord[0] = x != null ? Integer.parseInt(x) : 0 ;
            coord[1] = y != null ? Integer.parseInt(y) : 0 ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return coord;
    }
   
   public int[] readWindowSize(String windowname) {
        int[] dimension = new int[2];
        try {
           String x = readXmlSetting("//guisettings/windowsize/" + windowname + "/x");
           String y = readXmlSetting("//guisettings/windowsize/" + windowname + "/y");
           dimension[0] = x != null ? Integer.parseInt(x) : 0 ;
           dimension[1] = y != null ? Integer.parseInt(y) : 0 ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dimension;
    }

    public void writeWindowPos(String windowname, String[] pos) {
        try {
           Document doc = getSettingsDocument();
           
            XPathFactory xpathFactory = XPathFactory.newInstance();
          XPath xpath = xpathFactory.newXPath();
          
            Element posX = (Element) xpath.evaluate("//guisettings/windowspos/" + windowname + "/x", doc, XPathConstants.NODE);
            Element posY = (Element) xpath.evaluate("//guisettings/windowspos/" + windowname + "/y", doc, XPathConstants.NODE);
           
            posX.replaceChild(doc.createTextNode(pos[0]), posX.getFirstChild());
            posY.replaceChild(doc.createTextNode(pos[1]), posY.getFirstChild());
           
            writeSettings(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   
   public void writeWindowSize(String windowname, String[] dimension) {
      try {
         Document doc = getSettingsDocument();
           
            XPathFactory xpathFactory = XPathFactory.newInstance();
          XPath xpath = xpathFactory.newXPath();
          
            Element dimX = (Element) xpath.evaluate("//guisettings/windowsize/" + windowname + "/x", doc, XPathConstants.NODE);
            Element dimY = (Element) xpath.evaluate("//guisettings/windowsize/" + windowname + "/y", doc, XPathConstants.NODE);
           
            dimX.replaceChild(doc.createTextNode(dimension[0]), dimX.getFirstChild());
            dimY.replaceChild(doc.createTextNode(dimension[1]), dimY.getFirstChild());
           
            writeSettings(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   
    public boolean readBoolOptions(String option)
    {
        int value=0;
        try {
            String v = readXmlSetting("//" + option);
            value = v != null ? Integer.parseInt(v) : 0;
           
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(value==1) return true;
        else return false;
    }
   
    public void writeBoolOptions(String option,boolean value)
    {
          String state = value ? "1" : "0";
           
            try {
               Document doc = getSettingsDocument();
               
                XPathFactory xpathFactory = XPathFactory.newInstance();
              XPath xpath = xpathFactory.newXPath();
              
                Element emuOption = (Element) xpath.evaluate("//" + option, doc, XPathConstants.NODE);
               
                emuOption.replaceChild(doc.createTextNode(state), emuOption.getFirstChild());
               
                writeSettings(doc);
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
   
    @SuppressWarnings("unchecked")
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
            String rS = readXmlSetting("//emuoptions/keys/" + keyName);
            r = rS != null ? Integer.parseInt(rS) : null;
        } catch (Exception e) {
            e.printStackTrace();
        }
       
        return r;
    }
   
    public void writeKeyOption(String keyName, int key) {
        try {
           Document doc = getSettingsDocument();
           
            XPathFactory xpathFactory = XPathFactory.newInstance();
          XPath xpath = xpathFactory.newXPath();
          
            Element keyNameEl = (Element) xpath.evaluate("//emuoptions/keys/" + keyName, doc, XPathConstants.NODE);
           
            keyNameEl.replaceChild(doc.createTextNode(String.valueOf(key)), keyNameEl.getFirstChild());
           
            writeSettings(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   
}
