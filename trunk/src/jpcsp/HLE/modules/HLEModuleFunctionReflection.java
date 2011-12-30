package jpcsp.HLE.modules;

import java.lang.reflect.Method;
import java.util.HashMap;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import jpcsp.Processor;

public class HLEModuleFunctionReflection extends HLEModuleFunction {
	class RunListParams {
		int paramIndex;
		Object[] params;
		Processor processor;
		Object returnObject;

		void setParamNext(Object value) {
			params[paramIndex++] = value;
		}
	}

	HLEModule  hleModule;
	Class<?>   hleModuleClass;
	String     hleModuleMethodName;
	Method     hleModuleMethod;
	Class<?>[] hleModuleMethodParametersTypes;
	boolean    checkInsideInterrupt;
	boolean    checkDispatchThreadEnabled;
	Class<?>   hleModuleMethodReturnType;
	int        parameterCount;
	RunListParams runListParams = new RunListParams();
	
	// hleModuleMethodsByName.get(methodToCheckName)

	static public HashMap<String, Method> methodsByName;
	static public HashMap<String, HashMap<String, Method>> hleModuleModuleMethodsByName = new HashMap<String, HashMap<String, Method>>();
	static public HashMap<String, Method> hleModuleMethodsByName;

	static {
		methodsByName = new HashMap<String, Method>();
		for (Method method : HLEModuleFunctionReflection.class.getMethods()) {
			methodsByName.put(method.getName(), method);
		}
	}

	public HLEModuleFunctionReflection(String moduleName, String functionName, HLEModule hleModule, String hleModuleMethodName, Method hleModuleMethod, boolean checkInsideInterrupt, boolean checkDispatchThreadEnabled) {
		super(moduleName, functionName);
		
		this.hleModule = hleModule;
		this.hleModuleClass = hleModule.getClass();
		this.hleModuleMethodName = hleModuleMethodName;
		this.checkInsideInterrupt = checkInsideInterrupt;
		this.checkDispatchThreadEnabled = checkDispatchThreadEnabled;
		this.hleModuleMethod = hleModuleMethod; 
		this.hleModuleMethodParametersTypes = this.hleModuleMethod.getParameterTypes();
		this.hleModuleMethodReturnType = this.hleModuleMethod.getReturnType();
		this.parameterCount = hleModuleMethodParametersTypes.length;
		this.runListParams.params = new Object[parameterCount];
		
		if (!hleModuleModuleMethodsByName.containsKey(moduleName)) {
			hleModuleModuleMethodsByName.put(moduleName, new HashMap<String, Method>());
			hleModuleMethodsByName = hleModuleModuleMethodsByName.get(moduleName);
			for (Method method : hleModuleClass.getMethods()) {
				hleModuleMethodsByName.put(method.getName(), method);
			}
		} else {
			hleModuleMethodsByName = hleModuleModuleMethodsByName.get(moduleName);
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
