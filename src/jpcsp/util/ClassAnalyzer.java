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
package jpcsp.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author gid15
 *
 */
public class ClassAnalyzer {
	private static Logger log = Logger.getLogger("classAnalyzer");

	public static class ParameterInfo {
		public final String name;
		public final Class<?> type;

		public ParameterInfo(String name, Class<?> type) {
			this.name = name;
			this.type = type;
		}
	}

	public ParameterInfo[] getParameters(String methodName, Class<?> c) {
		ParameterInfo[] parameters = null;

		try {
			ClassReader cr = new ClassReader(c.getName().replace('.', '/'));
			Method[] methods = c.getDeclaredMethods();
			if (methods != null) {
				AnalyzerClassVisitor cn = null;
				for (int i = 0; i < methods.length; i++) {
					if (methodName.equals(methods[i].getName())) {
						cn = new AnalyzerClassVisitor(methods[i]);
						break;
					}
				}

				if (cn != null) {
					cr.accept(cn, 0);
					parameters = cn.getParameters();
				}
			}
		} catch (IOException e) {
			log.error("Cannot read class", e);
		}

		return parameters;
	}

	private static class AnalyzerClassVisitor extends ClassNode {
		private ParameterInfo[] parameters = null;
		private String methodName;
		private Method method;

		public AnalyzerClassVisitor(Method method) {
			this.method = method;
			this.methodName = method.getName();
		}

		public ParameterInfo[] getParameters() {
			return parameters;
		}

		@Override
		public void visitEnd() {
			// Visit all the methods
			for (Iterator<?> it = methods.iterator(); it.hasNext(); ) {
				MethodNode method = (MethodNode) it.next();
				if (methodName.equals(method.name)) {
					visitMethod(method);
				}
			}
		}

		private void visitMethod(MethodNode methodNode) {
			// First parameter is "this" for non-static methods
			int firstIndex = Modifier.isStatic(method.getModifiers()) ? 0 : 1;
			Class<?>[] parameterTypes = method.getParameterTypes();
			int numberParameters = Math.min(parameterTypes.length, methodNode.localVariables.size() - firstIndex);
			parameters = new ParameterInfo[numberParameters];
			for (int i = 0; i < methodNode.localVariables.size(); i++) {
				LocalVariableNode localVariableNode = (LocalVariableNode) methodNode.localVariables.get(i);
				int parameterIndex = localVariableNode.index - firstIndex;
				if (parameterIndex >= 0 && parameterIndex < numberParameters) {
					parameters[parameterIndex] = new ParameterInfo(localVariableNode.name, parameterTypes[parameterIndex]);
				}
			}
		}
	}
}
