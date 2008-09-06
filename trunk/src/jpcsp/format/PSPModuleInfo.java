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

import java.io.IOException;
import jpcsp.filesystems.*;
import static jpcsp.util.Utilities.*;

public class PSPModuleInfo {

    private int m_attr;
    private int m_version;
    private byte[] m_name = new byte[28];
    private long m_gp;
    private long m_exports;
    private long m_exp_end;
    private long m_imports;
    private long m_imp_end;
    private String m_namez = ""; // String version of m_name


    public void read(SeekableDataInput f) throws IOException {
        m_attr = readUHalf(f);
        m_version = readUHalf(f);
        f.readFully(getM_name());
        m_gp = readUWord(f);
        m_exports = readUWord(f); // .lib.ent

        m_exp_end = readUWord(f);
        m_imports = readUWord(f); // .lib.stub

        m_imp_end = readUWord(f);

        // Convert the array of bytes used for the module name to a Java String
        // If we don't do it this way then there will be too many nul-bytes on the end, and some shells print them all!
        int len;
        for (len = 0; len < 28 && getM_name()[len] != 0; len++); // Why this?
            m_namez = new String(getM_name(), 0, len);
    }

    public int getM_attr() {
        return m_attr;
    }

    public int getM_version() {
        return m_version;
    }

    public byte[] getM_name() {
        return m_name;
    }

    public long getM_gp() {
        return m_gp;
    }

    public long getM_exports() {
        return m_exports;
    }

    public long getM_exp_end() {
        return m_exp_end;
    }

    public long getM_imports() {
        return m_imports;
    }

    public long getM_imp_end() {
        return m_imp_end;
    }

    public String getM_namez() {
        return m_namez;
    }
}
