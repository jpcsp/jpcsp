package jpcsp.HLE.modules;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jpcsp.Processor;

public class HLEModuleFunctionReflection extends HLEModuleFunction {
	HLEModule hleModule;
	Class<?>  hleModuleClass;
	String    hleModuleMethodName;
	Method    hleModuleMethod;
	
	public HLEModuleFunctionReflection(String moduleName, String functionName, HLEModule hleModule, String hleModuleMethodName) {
		super(moduleName, functionName);
		
		this.hleModule = hleModule;
		this.hleModuleClass = hleModule.getClass();
		this.hleModuleMethodName = hleModuleMethodName;
		try {
			this.hleModuleMethod = hleModuleClass.getMethod(this.hleModuleMethodName, new Class[] { Processor.class });
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void execute(Processor cpu) {
		try {
			this.hleModuleMethod.invoke(hleModule, cpu);
		} catch (InvocationTargetException e) {
			System.err.println(
				"Error calling "
				+ ":: hleModule='" + hleModule + "'"
				+ ":: hleModuleClass='" + hleModuleClass + "'"
				+ ":: hleModuleMethodName='" + hleModuleMethodName + "'"
				+ ":: hleModuleMethod='" + hleModuleMethod + "'"
			);
			try {
				throw(e.getCause());
			} catch (Throwable e1) {
				e1.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String compiledString() {
		return this.hleModuleClass.getName() + "." + this.hleModuleMethodName + "(processor);";
	}

}
