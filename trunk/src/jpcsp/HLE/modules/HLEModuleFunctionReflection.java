package jpcsp.HLE.modules;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import jpcsp.Processor;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEUidClass;
import jpcsp.HLE.HLEUidObjectMapping;
import jpcsp.HLE.ITPointerBase;
import jpcsp.HLE.Modules;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TErrorPointer32;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointer64;
import jpcsp.HLE.TPointerBase;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;

public class HLEModuleFunctionReflection extends HLEModuleFunction {
	HLEModule  hleModule;
	Class<?>   hleModuleClass;
	String     hleModuleMethodName;
	Method     hleModuleMethod;
	Class<?>[] hleModuleMethodParametersTypes;
	Class<?>   hleModuleMethodReturnType;
	boolean    checkInsideInterrupt;
	boolean    fastOldInvoke;
	TErrorPointer32 errorHolder;
	LinkedList<Method> decodingRunListList;
	int        parameterCount;
	
	static HashMap<String, Method> methodsByName;

	static {
		methodsByName = new HashMap<String, Method>();
		for (Method method : HLEModuleFunctionReflection.class.getMethods()) {
			methodsByName.put(method.getName(), method);
			//System.err.println(method.getName());
		}
	}
	
	public HLEModuleFunctionReflection(String moduleName, String functionName, HLEModule hleModule, String hleModuleMethodName, Method hleModuleMethod, boolean checkInsideInterrupt) {
		super(moduleName, functionName);
		
		this.hleModule = hleModule;
		this.hleModuleClass = hleModule.getClass();
		this.hleModuleMethodName = hleModuleMethodName;
		this.checkInsideInterrupt = checkInsideInterrupt;
		this.hleModuleMethod = hleModuleMethod; 
		this.hleModuleMethodParametersTypes = this.hleModuleMethod.getParameterTypes();
		this.hleModuleMethodReturnType = this.hleModuleMethod.getReturnType();
		this.parameterCount = hleModuleMethodParametersTypes.length;
		
		try {
			if (
				this.hleModuleMethodReturnType == void.class &&
				this.hleModuleMethodParametersTypes.length == 1 &&
				this.hleModuleMethodParametersTypes[0] == Processor.class
			) {
				fastOldInvoke = true;
			} else {
				fastOldInvoke = false;
				prepareParameterDecodingRunList();
			}
		} catch (Throwable o) {
			System.err.println("OnMethod: " + hleModuleMethod);
			o.printStackTrace();
		}
	}
	
	class RunListParams {
		int paramIndex;
		Object[] params;
		Processor processor;
		
		void setParamNext(Object value) {
			params[paramIndex++] = value;
		}
	}
	
	protected Method getRunListMethod(String name) throws Throwable {
		Method method = methodsByName.get(name);
		if (method == null) throw(new Exception(String.format("Can't find method '%s'", name)));
		return method;
	}
	
	protected void prepareParameterDecodingRunList() throws Throwable {
		Annotation[][] paramsAnotations = this.hleModuleMethod.getParameterAnnotations();
		
		decodingRunListList = new LinkedList<Method>();
		canBeNullParams = new boolean[parameterCount];
		errorValuesOnNotFound = new Integer[parameterCount];
		methodsToCheck = new Method[parameterCount];
		
		int paramIndex = 0;
		for (Class<?> paramClass : hleModuleMethodParametersTypes) {
			Annotation[] paramAnnotations = paramsAnotations[paramIndex];
			String methodToCheckName = null;

			for (Annotation currentAnnotation : paramAnnotations) {
				if (currentAnnotation instanceof CanBeNull) {
					canBeNullParams[paramIndex] = true;
				}
				if (currentAnnotation instanceof CheckArgument) {
					methodToCheckName = ((CheckArgument)currentAnnotation).value();
				}
			}

			if (paramClass == Processor.class) {
				decodingRunListList.add(getRunListMethod("parameterAddProcessor"));
			} else if (paramClass == int.class) {
				decodingRunListList.add(getRunListMethod("parameterAddInteger"));
			} else if (paramClass == float.class) {
				decodingRunListList.add(getRunListMethod("parameterAddFloat"));
			} else if (paramClass == long.class) {
				decodingRunListList.add(getRunListMethod("parameterAddLong"));
			} else if (paramClass == boolean.class) {
				decodingRunListList.add(getRunListMethod("parameterAddBoolean"));
			} /*else if (paramClass.isEnum()) {
				params.add(paramClass.cast(processor.parameterReader.getNextInt()));
			}*/ else if (TPointer.class.isAssignableFrom(paramClass)) {
				decodingRunListList.add(getRunListMethod("parameterAddTPointer"));
			} else if (TPointerBase.class.isAssignableFrom(paramClass)) {
				if (TPointer64.class.isAssignableFrom(paramClass)) {
					decodingRunListList.add(getRunListMethod("parameterAddTPointer64"));
				} else if (TErrorPointer32.class.isAssignableFrom(paramClass)) {
					decodingRunListList.add(getRunListMethod("parameterAddTErrorPointer32"));
				} else if (TPointer32.class.isAssignableFrom(paramClass)) {
					decodingRunListList.add(getRunListMethod("parameterAddTPointer32"));
				} else {
					throw(new RuntimeException("Unknown TPointerBase parameter class '" + paramClass + "'"));
				}
			} else {
				HLEUidClass hleUidClass = paramClass.getAnnotation(HLEUidClass.class);
				if (hleUidClass != null) {
					errorValuesOnNotFound[paramIndex] = hleUidClass.errorValueOnNotFound();
					
					decodingRunListList.add(getRunListMethod("parameterAddUidObject"));
				} else {
					throw(new RuntimeException("Unknown parameter class '" + paramClass + "'"));
				}
			}
			
			if (methodToCheckName != null) {
				for (Method method : hleModuleClass.getMethods()) {
					if (method.getName().equals(methodToCheckName)) {
						methodsToCheck[paramIndex] = method;
						break;
					}
				}
				
				if (methodsToCheck[paramIndex] != null) {
					decodingRunListList.add(getRunListMethod("parameterCheck"));
				}
			}
			
			paramIndex++;
		}
		
		//decodingRunList = new Method[decodingRunListList.size()];

		//decodingRunListList.toArray(decodingRunList);
	}
	
	protected Object[] executeParameterDecodingRunList(Processor processor) throws Throwable {
		processor.parameterReader.cpu = processor.cpu;
		processor.parameterReader.memory = Processor.memory;
		processor.parameterReader.resetReading();
		
		RunListParams runListParams = new RunListParams();
		runListParams.processor = processor;
		runListParams.params = new Object[parameterCount];
		runListParams.paramIndex = 0;

		for (Method decodingRunListMethod : decodingRunListList) {
			//System.err.println(runListParams.paramIndex);
			decodingRunListMethod.invoke(this, runListParams);
		}
		
		return runListParams.params;
	}
	
	protected boolean[] canBeNullParams;
	protected Integer[] errorValuesOnNotFound;
	protected Method[] methodsToCheck;
	
	protected void checkAddressIsGood(RunListParams runListParams) {
		if (!canBeNullParams[runListParams.paramIndex - 1]) {
			ITPointerBase pointer = (ITPointerBase)runListParams.params[runListParams.paramIndex - 1];
			if (!pointer.isAddressGood()) {
				throw(new SceKernelErrorException(SceKernelErrors.ERROR_INVALID_POINTER));
			}
		}
	}

	public void parameterAddProcessor(RunListParams runListParams) {
		runListParams.setParamNext(runListParams.processor);
	}
	
	public void parameterAddInteger(RunListParams runListParams) {
		runListParams.setParamNext(runListParams.processor.parameterReader.getNextInt());
	}

	public void parameterAddFloat(RunListParams runListParams) {
		runListParams.setParamNext(runListParams.processor.parameterReader.getNextFloat());
	}

	public void parameterAddLong(RunListParams runListParams) {
		runListParams.setParamNext(runListParams.processor.parameterReader.getNextLong());
	}

	public void parameterAddBoolean(RunListParams runListParams) {
		int value = runListParams.processor.parameterReader.getNextInt();
		if (value < 0 || value > 1) {
			Logger.getRootLogger().warn(
				String.format("Parameter exepcted to be bool but had value 0x%08X", value)
			);
		}

		runListParams.setParamNext((value != 0));
	}

	public void parameterAddTPointer(RunListParams runListParams) {
		runListParams.setParamNext(new TPointer(Processor.memory, runListParams.processor.parameterReader.getNextInt()));
		checkAddressIsGood(runListParams);
	}
	
	public void parameterAddTPointer64(RunListParams runListParams) {
		runListParams.setParamNext(new TPointer64(Processor.memory, runListParams.processor.parameterReader.getNextInt()));
		checkAddressIsGood(runListParams);
	}

	public void parameterAddTPointer32(RunListParams runListParams) {
		runListParams.setParamNext(new TPointer32(Processor.memory, runListParams.processor.parameterReader.getNextInt()));
		checkAddressIsGood(runListParams);
	}
	
	public void parameterAddTErrorPointer32(RunListParams runListParams) {
		runListParams.setParamNext(this.errorHolder = new TErrorPointer32(Processor.memory, runListParams.processor.parameterReader.getNextInt()));
		checkAddressIsGood(runListParams);
	}
	
	public void parameterAddUidObject(RunListParams runListParams) {
		int uid = runListParams.processor.parameterReader.getNextInt();
		
		Object object = HLEUidObjectMapping.getObject(hleModuleMethodParametersTypes[runListParams.paramIndex], uid);
		if (object == null) {
			throw(new SceKernelErrorException(errorValuesOnNotFound[runListParams.paramIndex]));
		}
		runListParams.setParamNext(object);
	}

	public void parameterCheck(RunListParams runListParams) throws Throwable {
	//void parameterCheck(Integer paramIndex, Object[] params, Processor processor) throws Throwable {
		//System.err.println("ParamIndex: " + runListParams.paramIndex - 1);
		//System.err.println(methodsToCheck[runListParams.paramIndex - 1]);
		runListParams.params[runListParams.paramIndex - 1] =
			methodsToCheck[runListParams.paramIndex - 1].invoke(hleModule, runListParams.params[runListParams.paramIndex - 1])
		;
	}

	private void setReturnValue(Processor processor, Object returnObject) {
		try {
			if (hleModuleMethodReturnType == void.class) {
				// Do nothing
			} else if (hleModuleMethodReturnType == int.class) {
				processor.parameterReader.setReturnValueInt((Integer)returnObject);
			} else if (hleModuleMethodReturnType == boolean.class) {
				processor.parameterReader.setReturnValueInt(((Boolean)returnObject) ? 1 : 0);
			} else if (hleModuleMethodReturnType == long.class) {
				processor.parameterReader.setReturnValueLong((Integer)returnObject);
			} else if (hleModuleMethodReturnType == float.class) {
				processor.parameterReader.setReturnValueFloat((Float)returnObject);
			} else {
				
				HLEUidClass hleUidClass = hleModuleMethodReturnType.getAnnotation(HLEUidClass.class);
				
				if (hleUidClass != null) {
					if (hleUidClass.moduleMethodUidGenerator().length() == 0) {
						processor.parameterReader.setReturnValueInt(
							HLEUidObjectMapping.createUidForObject(hleModuleMethodReturnType, returnObject)
						);
					} else {
						Method moduleMethodUidGenerator = hleModule.getClass().getMethod(hleUidClass.moduleMethodUidGenerator(), new Class[] { });
						int uid = (Integer)moduleMethodUidGenerator.invoke(hleModule);
						processor.parameterReader.setReturnValueInt(
							HLEUidObjectMapping.addObjectMap(hleModuleMethodReturnType, uid, returnObject)
						);
					}
				} else {
					throw(new RuntimeException("Can't handle return type '" + hleModuleMethodReturnType + "'"));
				}
			}
		} catch (Throwable o) {
			throw(new RuntimeException(o.getCause()));
		}
	}

	@Override
	public void execute(Processor processor) {
		try {
			if (checkInsideInterrupt) {
		        if (IntrManager.getInstance().isInsideInterrupt()) {
		        	throw(new SceKernelErrorException(SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT));
		        	//setReturnValue(processor, SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT); return;
		        }
			}
			
			if (getUnimplemented()) {
				Modules.getLogger(this.getModuleName()).warn(
					String.format(
						"Unimplemented NID function %s.%s [0x%08X]",
						this.getModuleName(),
						this.getFunctionName(),
						this.getNid()
					)
				);
			}
			
			try {
				if (fastOldInvoke) {
					this.hleModuleMethod.invoke(hleModule, processor);
				} else {
					//this.hleModuleMethod.invoke(hleModule, cpu);
					Object returnObject = this.hleModuleMethod.invoke(
						hleModule,
						executeParameterDecodingRunList(processor)
					);

					if (errorHolder != null) {
						errorHolder.setValue(0);
					}

					setReturnValue(processor, returnObject);
				}
			} catch (InvocationTargetException e) {
				System.err.println(
					"Error calling "
					+ ":: hleModule='" + hleModule + "'"
					+ ":: hleModuleClass='" + hleModuleClass + "'"
					+ ":: hleModuleMethodName='" + hleModuleMethodName + "'"
					+ ":: hleModuleMethod='" + hleModuleMethod + "'"
				);
				throw(e.getCause());
				
				
			}
		} catch (SceKernelErrorException kernelError) {
			if (errorHolder != null) {
				errorHolder.setValue(kernelError.errorCode);
				setReturnValue(processor, 0);
			} else {
				setReturnValue(processor, kernelError.errorCode);
			}
		} catch (Throwable e1) {
			System.err.println("OnMethod: " + hleModuleMethod);
			e1.printStackTrace();
		}
	}

	@Override
	public String compiledString() {
		//return "processor.parameterReader.resetReading(); " + this.hleModuleClass.getName() + "." + this.hleModuleMethodName + "(processor);";
		return "";
	}

}
