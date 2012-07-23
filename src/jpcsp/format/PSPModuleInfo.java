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

import static jpcsp.util.Utilities.readUHalf;
import static jpcsp.util.Utilities.readUWord;

import java.io.IOException;
import java.nio.ByteBuffer;

import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure;

public class PSPModuleInfo extends pspAbstractMemoryMappedStructure {
	private static final int NAME_LENGTH = 28;
    private int m_attr;
    private int m_version;
    private long m_gp;
    private long m_exports;
    private long m_exp_end;
    private long m_imports;
    private long m_imp_end;
    private String m_namez = ""; // String version of m_name

    public void read(ByteBuffer f) throws IOException {
        m_attr = readUHalf(f);
        m_version = readUHalf(f);
        byte[] m_name = new byte[NAME_LENGTH];
        f.get(m_name);
        m_gp = readUWord(f);
        m_exports = readUWord(f); // .lib.ent
        m_exp_end = readUWord(f);
        m_imports = readUWord(f); // .lib.stub
        m_imp_end = readUWord(f);

        // Convert the array of bytes used for the module name to a Java String
        // Calculate the length of the printable portion of the string, otherwise
        // any extra trailing characters may be printed as garbage.
        int len = 0;
        while (len < 28 && m_name[len] != 0)
            len++;
        m_namez = new String(m_name, 0, len);
    }

	@Override
	protected void read() {
		m_attr = read16();
		m_version = read16();
		m_namez = readStringNZ(NAME_LENGTH);
		m_gp = read32();
		m_exports = read32();
		m_exp_end = read32();
		m_imports = read32();
		m_imp_end = read32();
	}

	@Override
	protected void write() {
		write16((short) m_attr);
		write16((short) m_version);
		writeStringNZ(NAME_LENGTH, m_namez);
		write32((int) m_gp);
		write32((int) m_exports);
		write32((int) m_exp_end);
		write32((int) m_imports);
		write32((int) m_imp_end);
	}

	public int getM_attr() {
        return m_attr;
    }

    public int getM_version() {
        return m_version;
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

	@Override
	public int sizeof() {
		return 52;
	}
}
