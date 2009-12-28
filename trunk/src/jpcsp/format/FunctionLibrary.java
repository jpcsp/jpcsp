/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.format;

import java.util.*;

/**
 *
 * @author hli
 */
class Module {

    private final String m_name;
    private Hashtable<String, String> m_functions;

    public Module(String name) {
        m_name = name;
        m_functions = new Hashtable<String, String>();
    }

    public String getName() {
        return m_name;
    }

    public String getFunctionFromModule(String nid) {
        try {
            return m_functions.get(nid);
        } catch (NullPointerException dummy) {
            return null;
        }
    }

    public int countFunctions() {
        return m_functions.size();
    }

    public void addFunction(String nid, String name) {
        m_functions.put(nid, name);
    }
}

public class FunctionLibrary {

    private int m_moduleCount = 0;
    private int m_functionCount = 0;
    private Hashtable<String, Module> m_moduleTable;
    private List<Module> m_moduleList;

    public String getFunctionFromLibrary(String library, String nid) {
        String ret;
        try {
            ret = (m_moduleTable.get(library)).getFunctionFromModule(nid);
            return ret;
        } catch (NullPointerException dummy1) {
            try {
                Module that = m_moduleTable.get(library);
                ret = library + "_Unknown_" + nid.substring(2);
                addFunctionToLibrary(library, nid, ret);
                return ret;
            } catch (NullPointerException dummy2) {
                ret = null;
                addModule(library);
                addFunctionToLibrary(library, nid, library + "_Unknown_" + nid.substring(2));
                return ret;
            }
        }
    }

    public void addModule(String module) {
        m_moduleTable.put(module, new Module(module));
        m_moduleCount++;
    }

    public void addFunctionToLibrary(String module, String nid, String name) {
        Module that = m_moduleTable.get(module);
        that.addFunction(nid, name);
        m_functionCount++;
    }

    public int getModuleCount() {
        return m_moduleCount;
    }

    public int getFunctionCount() {
        return m_functionCount;
    }

    FunctionLibrary(String fw) throws Exception {
        /*
        m_moduleTable = new Hashtable();
        
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new File(fw + "/psplibdoc.xml"));
        
        XPath modules = XPath.newInstance("/PSPLIBDOC/PRXFILES/PRXFILE/LIBRARIES/LIBRARY");
        
        List LibList = modules.selectNodes(doc);
        m_moduleList = modules.selectNodes(doc, "//NAME");
        
        Iterator i = LibList.iterator();
        
        int x = 0;
        
        while (i.hasNext()) {
        Element curEl = (Element) i.next();
        String modName = curEl.getChild("NAME").getText();
        Module newMod = new Module(modName);
        List FunctionList = curEl.getChild("FUNCTIONS").getChildren("FUNCTION");
        Iterator j = FunctionList.iterator();
        while (j.hasNext()) {
        Element funcEl = (Element) j.next();
        newMod.addFunction(funcEl.getChild("NID").getText(), funcEl.getChild("NAME").getText());
        m_functionCount++;
        }
        m_moduleCount++;
        m_moduleTable.put(modName, newMod);
        }
         */
    }
}

