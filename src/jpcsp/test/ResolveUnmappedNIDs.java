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
package jpcsp.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;


class NIDInfo {
    public String prx;
    public String prxName;
    public String libraryName;
    public String functionName;
    public int functionNID;

    public boolean resolved;
    public String firmwareVersion;

    public NIDInfo(int nid) {
        functionNID = nid;
        resolved = false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("prx=" + prx + " ");
        sb.append("prxName=" + prxName + " ");
        sb.append("libraryName=" + libraryName + " ");
        sb.append("functionName=" + functionName + " ");
        sb.append("functionNID=" + String.format("0x%08X", functionNID));
        return sb.toString();
    }
}

class Module {

    private final String m_name;
    private Hashtable<String, String> m_functions;

    public Module(String name) {
        m_name = new String(name);
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
        //System.out.println(nid + "/" + name);
        m_functions.put(nid, name);
    }
}

class Firmware {
    private String m_version;
    private int m_moduleCount;
    private int m_functionCount;
    private Hashtable<String, Module> m_moduleTable;

    // psplibdoc parser based on hlide's FunctionLibrary.java
    public Firmware(String version, String psplibdoc_filename) throws Exception {
        m_version = version;
        m_moduleCount = 0;
        m_functionCount = 0;
        m_moduleTable = new Hashtable<String, Module>();

        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new File(psplibdoc_filename));

        XPath modules = XPath.newInstance("/PSPLIBDOC/PRXFILES/PRXFILE/LIBRARIES/LIBRARY");

        List<?> LibList = modules.selectNodes(doc);
        //m_moduleList = modules.selectNodes(doc, "//NAME");

        Iterator<?> i = LibList.iterator();

        while (i.hasNext()) {
            Element curEl = (Element) i.next();
            String modName = curEl.getChild("NAME").getText();
            Module newMod = new Module(modName);
            List<?> FunctionList = curEl.getChild("FUNCTIONS").getChildren("FUNCTION");
            Iterator<?> j = FunctionList.iterator();
            while (j.hasNext()) {
                Element funcEl = (Element) j.next();
                newMod.addFunction(funcEl.getChild("NID").getText(), funcEl.getChild("NAME").getText());
                m_functionCount++;
            }
            m_moduleCount++;
            m_moduleTable.put(modName, newMod);
        }

        System.out.println("filename: " + psplibdoc_filename + " modules: " + m_moduleCount + " functions: " + m_functionCount);
    }

    public String getVersion() {
        return m_version;
    }

    public int getModuleCount() {
        return m_moduleCount;
    }

    public int getFunctionCount() {
        return m_functionCount;
    }

    /** @return true if resolved */
    public boolean resolveNID(NIDInfo info) {
        for (Module module : m_moduleTable.values()) {
            String functionName = module.getFunctionFromModule(String.format("0x%08X", info.functionNID));
            if (functionName != null) {
                info.functionName = functionName;
                info.libraryName = module.getName();
                if (!info.resolved) {
                    info.firmwareVersion = m_version;
                }
                info.resolved = true;
                return true;
            }
        }
        return false;
    }
}

public class ResolveUnmappedNIDs {

    public static HashMap<Integer, NIDInfo> parseLog(String filename) {
        LinkedHashMap<Integer, NIDInfo> nids = new LinkedHashMap<Integer, NIDInfo>();

        try {
            FileReader fr = new FileReader(filename);
            BufferedReader br = new BufferedReader(fr);
            String line;

            while((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                for (int i = 10; i < parts.length; i++) {
                    if (parts[i].startsWith("[0x")) {
                        try {
                            int nid = (int)Long.parseLong(parts[i].substring(3, 11), 16);
                            NIDInfo info = new NIDInfo(nid);
                            nids.put(nid, info);
                        } catch(Exception e) {
                            //System.err.println(e.getMessage());
                            //System.err.println("error near '" + parts[i] + "'");
                        }
                        break;
                    }
                }
            }

            br.close();
            fr.close();
        } catch(FileNotFoundException e) {
            System.err.println("parseLog: File not found: " + filename);
        } catch(Exception e) {
            System.err.println("parseLog: " + filename);
            e.printStackTrace();
        }

        return nids;
    }

    public static void processNIDs(LinkedList<Firmware> firmware, HashMap<Integer, NIDInfo> nids) {
        for (NIDInfo info : nids.values()) {
            for (Firmware fw : firmware) {
                fw.resolveNID(info);
                // keep resolving in newer fw, since later libdoc have nids decoded to function names
                //if (found)
                //    break;
            }
        }
    }

    public static void printResults(HashMap<Integer, NIDInfo> nids, int syscallCode) {
        for (NIDInfo info : nids.values()) {
            //System.out.println(info);

            // sample:
            // sceUmdReplaceProhibit(0x3007, 0x87533940), // 2.00+
            String functionName = (info.functionName == null) ? String.format("unknown_%08X", info.functionNID) : info.functionName;
            String firmwareVersion = (info.firmwareVersion == null) ? "" : " // " + info.firmwareVersion + "+";
            String msg = String.format("%s(0x%04x, 0x%08X),%s",
                functionName, syscallCode++, info.functionNID, firmwareVersion);
            System.out.println(msg);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("parameters: <logfile> [syscallCodeStart]");
            System.err.println("example: fragment.txt 0x3000");
            System.exit(1);
        }

        // Load psplibdocs
        String prefixPath = "psplibdoc/";
        LinkedList<Firmware> firmware = new LinkedList<Firmware>();

        try {
            firmware.add(new Firmware("1.00", prefixPath + "100_psplibdoc_230808.xml"));
            firmware.add(new Firmware("1.50", prefixPath + "150_psplibdoc_190808.xml"));
            firmware.add(new Firmware("2.00", prefixPath + "200_psplibdoc_260808.xml"));
            firmware.add(new Firmware("2.50", prefixPath + "250_psplibdoc_270808.xml"));
            firmware.add(new Firmware("2.71", prefixPath + "271_psplibdoc_280808.xml"));
            firmware.add(new Firmware("3.52", prefixPath + "352_psplibdoc_190808.xml"));
            firmware.add(new Firmware("3.95", prefixPath + "395_psplibdoc_020508.xml"));
            firmware.add(new Firmware("4.05", prefixPath + "405_psplibdoc_190808.xml"));
            firmware.add(new Firmware("5.00", prefixPath + "500_psplibdoc_191008.xml"));
        } catch(Exception e) {
            e.printStackTrace();
        }

        // Load log file fragment
        HashMap<Integer, NIDInfo> nids = parseLog(args[0]);

        // Resolve nids
        processNIDs(firmware, nids);

        // Output
        int syscallCode = 0x3000;
        if (args.length >= 2) {
            try {
                syscallCode = Integer.parseInt(args[1], 16);
            } catch(Exception e) {
                System.err.println("bad syscall parameter, defaulting to 0x3000");
            }
        }
        printResults(nids, syscallCode);
    }
}
