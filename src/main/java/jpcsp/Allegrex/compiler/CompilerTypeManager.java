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
package jpcsp.Allegrex.compiler;

import java.util.HashMap;

import org.objectweb.asm.Type;

/**
 * @author gid15
 *
 */
public class CompilerTypeManager {
	private HashMap<Class<?>, CompilerTypeInformation> compilerTypeInformations;
	private CompilerTypeInformation defaultCompilerTypeInformation;

	public CompilerTypeManager() {
		compilerTypeInformations = new HashMap<Class<?>, CompilerTypeInformation>();
		defaultCompilerTypeInformation = new CompilerTypeInformation(null, null, "%s");

		addCompilerTypeInformation(int.class, new CompilerTypeInformation(Type.getInternalName(Integer.class), "(I)V", "0x%X"));
		addCompilerTypeInformation(boolean.class, new CompilerTypeInformation(Type.getInternalName(Boolean.class), "(Z)V", "%b"));
		addCompilerTypeInformation(long.class, new CompilerTypeInformation(Type.getInternalName(Long.class), "(J)V", "0x%X"));
		addCompilerTypeInformation(short.class, new CompilerTypeInformation(Type.getInternalName(Short.class), "(S)V", "0x%X"));
		addCompilerTypeInformation(float.class, new CompilerTypeInformation(Type.getInternalName(Float.class), "(F)V", "%f"));
		addCompilerTypeInformation(String.class, new CompilerTypeInformation(null, null, "'%s'"));
	}

	private void addCompilerTypeInformation(Class<?> type, CompilerTypeInformation compilerTypeInformation) {
		compilerTypeInformations.put(type, compilerTypeInformation);
	}

	public CompilerTypeInformation getCompilerTypeInformation(Class<?> type) {
		CompilerTypeInformation compilerTypeInformation = compilerTypeInformations.get(type);
		if (compilerTypeInformation == null) {
			compilerTypeInformation = defaultCompilerTypeInformation;
		}

		return compilerTypeInformation;
	}
}
