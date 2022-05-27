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

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public class CompilerClassLoader extends ClassLoader {
    public static Logger log = Logger.getLogger("loader");
    private ICompiler compiler;

    public CompilerClassLoader(ICompiler compiler) {
        this.compiler = compiler;
    }

    public Class<?> defineClass(String name, byte[] b) {
        return defineClass(name, b, 0, b.length);
    }

    public Class<?> defineClass(byte[] b) {
        return defineClass(null, b, 0, b.length);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Check if the class has already been loaded
        Class<?> loadedClass = findLoadedClass(name);

        if (loadedClass == null && compiler != null) {
        	if (log.isTraceEnabled()) {
        		log.trace("ClassLoader creating class " + name);
        	}
            IExecutable executable = compiler.compile(name);
            if (executable != null) {
                loadedClass = executable.getClass();
            }
        }

        if (loadedClass == null) {
            loadedClass = super.findClass(name);
        }

        return loadedClass;
    }
}
