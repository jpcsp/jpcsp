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
package jpcsp.format.rco.vsmx.objects;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import jpcsp.format.rco.vsmx.INativeFunction;
import jpcsp.format.rco.vsmx.VSMX;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXNativeObject;

public class NativeFunctionFactory {
	private static final Logger log = VSMX.log;
	private static NativeFunctionFactory singleton;

	private static class NativeFunction implements INativeFunction {
		private Method method;
		private Object object;
		private int args;

		public NativeFunction(Object object, Method method, int args) {
			this.object = object;
			this.method = method;
			this.args = args;
		}

		@Override
		public int getArgs() {
			return args;
		}

		@Override
		public VSMXBaseObject call(VSMXBaseObject[] arguments) {
			VSMXBaseObject returnValue = null;
			try {
				Object result = method.invoke(object, (Object[]) arguments);
				if (result instanceof VSMXBaseObject) {
					returnValue = (VSMXBaseObject) result;
				}
			} catch (IllegalArgumentException e) {
				log.error("call", e);
			} catch (IllegalAccessException e) {
				log.error("call", e);
			} catch (InvocationTargetException e) {
				log.error("call", e);
			}
			return returnValue;
		}
	}

	public static NativeFunctionFactory getInstance() {
		if (singleton == null) {
			singleton = new NativeFunctionFactory();
		}
		return singleton;
	}

	private NativeFunctionFactory() {
	}

	private INativeFunction getNativeFunctionInterface(Object object, String name, int numberOfArguments) {
		INativeFunction nativeFunction = null;

		// movieplayer.play has as much as 10 parameters
		for (int args = numberOfArguments + 1; args < 12; args++) {
			Class<?>[] arguments = new Class<?>[args];
			for (int i = 0; i < arguments.length; i++) {
				arguments[i] = VSMXBaseObject.class;
			}
			try {
				Method method = object.getClass().getMethod(name, arguments);
				nativeFunction = new NativeFunction(object, method, args - 1);
				break;
			} catch (SecurityException e) {
				log.error("getNativeFunction", e);
			} catch (NoSuchMethodException e) {
				// Ignore error
			}
		}

		if (nativeFunction == null && log.isDebugEnabled()) {
			log.debug(String.format("Not finding native function %s.%s(args=%d)", object, name, numberOfArguments + 1));
		}

		return nativeFunction;
	}

	public INativeFunction getNativeFunction(VSMXNativeObject object, String name, int numberOfArguments) {
		BaseNativeObject nativeObject = object.getObject();
		INativeFunction nativeFunction = getNativeFunctionInterface(nativeObject, name, numberOfArguments);

		return nativeFunction;
	}

	public INativeFunction getNativeFunction(VSMXBaseObject object, String name, int numberOfArguments) {
		INativeFunction nativeFunction = getNativeFunctionInterface(object, name, numberOfArguments);

		return nativeFunction;
	}
}
