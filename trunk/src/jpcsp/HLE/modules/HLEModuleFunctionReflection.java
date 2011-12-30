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
package jpcsp.HLE.modules;

import java.lang.reflect.Method;
import java.util.HashMap;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import jpcsp.Processor;

public class HLEModuleFunctionReflection extends HLEModuleFunction {
	private Method     hleModuleMethod;
	private boolean    checkInsideInterrupt;
	private boolean    checkDispatchThreadEnabled;

	static public HashMap<String, HashMap<String, Method>> hleModuleModuleMethodsByName = new HashMap<String, HashMap<String, Method>>();

	public HLEModuleFunctionReflection(String moduleName, String functionName, HLEModule hleModule, Method hleModuleMethod, boolean checkInsideInterrupt, boolean checkDispatchThreadEnabled) {
		super(moduleName, functionName);

		this.checkInsideInterrupt = checkInsideInterrupt;
		this.checkDispatchThreadEnabled = checkDispatchThreadEnabled;
		this.hleModuleMethod = hleModuleMethod; 

		if (!hleModuleModuleMethodsByName.containsKey(moduleName)) {
			HashMap<String, Method> hleModuleMethodsByName = new HashMap<String, Method>();
			hleModuleModuleMethodsByName.put(moduleName, hleModuleMethodsByName);
			for (Method method : hleModule.getClass().getMethods()) {
				hleModuleMethodsByName.put(method.getName(), method);
			}
		}
	}
	
	public boolean checkDispatchThreadEnabled() {
		return checkDispatchThreadEnabled;
	}
	
	public boolean checkInsideInterrupt() {
		return checkInsideInterrupt;
	}

	public Method getHLEModuleMethod() {
		return hleModuleMethod;
	}

	@Override
	public void execute(Processor cpu) {
		throw(new NotImplementedException());
	}

	@Override
	public String compiledString() {
		throw(new NotImplementedException());
	}
}
