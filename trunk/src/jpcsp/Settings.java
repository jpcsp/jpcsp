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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
    public int[] readWindowPos(String windowname)
    {
       int[] coord = new int[2];
       try {
            // Build the document with SAX and Xerces, no validation
            SAXBuilder builder = new SAXBuilder();
            // Create the document
            Document doc = builder.build(new File("settings.xml"));
            Element webapp = doc.getRootElement();
            coord[0] = Integer.parseInt(webapp.getChild("guisettings").getChild("windowspos").getChild(windowname).getChild("x").getText());
            coord[1] = Integer.parseInt(webapp.getChild("guisettings").getChild("windowspos").getChild(windowname).getChild("y").getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return coord;
         
    }
    public void writeWindowPos(String windowname,String[] pos)
    {
            try {
           
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new File("settings.xml"));
            Element webapp = doc.getRootElement();
            webapp.getChild("guisettings").getChild("windowspos").getChild(windowname).getChild("x").setText(pos[0]);
            webapp.getChild("guisettings").getChild("windowspos").getChild(windowname).getChild("y").setText(pos[1]);
            XMLOutputter xmloutputter = new XMLOutputter();
            try
            {
                FileOutputStream fileOutputStream = new FileOutputStream("settings.xml");
                xmloutputter.output(doc, fileOutputStream);
                fileOutputStream.close();
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
         } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
