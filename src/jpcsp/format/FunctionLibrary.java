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
package jpcsp.format;

import java.util.Hashtable;
import java.util.List;

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
    }
}