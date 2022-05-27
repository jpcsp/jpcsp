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
package jpcsp.format.rco.vsmx.interpreter;

import java.util.Stack;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.format.rco.vsmx.VSMX;
import jpcsp.format.rco.vsmx.VSMXCode;
import jpcsp.format.rco.vsmx.VSMXGroup;
import jpcsp.format.rco.vsmx.VSMXMem;

public class VSMXInterpreter {
	private static final Logger log = VSMX.log;
	private VSMXMem mem;
	private int pc;
	private boolean exit;
	private Stack<VSMXBaseObject> stack;
	private Stack<VSMXCallState> callStates;
	private VSMXCallState callState;
	private VSMXObject globalVariables;
	private String prefix;
	private String name;

	private class InterpretFunctionAction implements IAction {
		private VSMXFunction function;
		private VSMXBaseObject object;
		private VSMXBaseObject[] arguments;

		public InterpretFunctionAction(VSMXFunction function, VSMXBaseObject object, VSMXBaseObject[] arguments) {
			this.function = function;
			this.object = object;
			this.arguments = arguments;
		}

		@Override
		public void execute() {
			interpretFunction(function, object, arguments);
		}
	}

	public VSMXInterpreter() {
	}

	public void setVSMX(VSMX vsmx) {
		mem = vsmx.getMem();
		name = vsmx.getName();
	}

	public VSMXObject getGlobalVariables() {
		return globalVariables;
	}

	private VSMXBaseObject[] popValues(int n) {
		VSMXBaseObject[] values = new VSMXBaseObject[n];
		for (int i = n - 1; i >= 0; i--) {
			values[i] = stack.pop().getValue();
		}

		return values;
	}

	private void pushCallState(VSMXBaseObject thisObject, int numberOfLocalVariables, boolean returnThis, boolean exitAfterCall) {
		if (callState != null) {
			callStates.push(callState);
		}
		callState = new VSMXCallState(thisObject, numberOfLocalVariables, pc, returnThis, exitAfterCall);
		stack = callState.getStack();
		prefix += "  ";
	}

	private void popCallState() {
		if (callStates.isEmpty()) {
			callState = null;
			stack = null;
			prefix = "";
		} else {
			callState = callStates.pop();
			stack = callState.getStack();
			prefix = prefix.substring(0, prefix.length() - 2);
		}
	}

	private void interpret(VSMXGroup code) {
		VSMXBaseObject o1, o2, o3, o, r;
		VSMXBaseObject arguments[];
		float f1, f2, f;
		String s1, s2, s;
		int i1, i2, i;
		boolean b;
		switch (code.getOpcode()) {
			case VSMXCode.VID_NOTHING:
				break;
			case VSMXCode.VID_OPERATOR_ASSIGN:
				o1 = stack.pop().getValue();
				o2 = stack.pop();
				if (o2 instanceof VSMXReference) {
					if (log.isTraceEnabled()) {
						log.trace(String.format("%s = %s", o2, o1));
					}
					((VSMXReference) o2).assign(o1);
					stack.push(o1);
				} else {
					log.warn(String.format("Line#%d non-ref assignment %s", pc - 1, code));
				}
				break;
			case VSMXCode.VID_OPERATOR_ADD:
				o1 = stack.pop().getValue();
				o2 = stack.pop().getValue();
				if (o1 instanceof VSMXString || o2 instanceof VSMXString) {
					s1 = o1.getStringValue();
					s2 = o2.getStringValue();
					s = s2 + s1;
					stack.push(new VSMXString(this, s));
				} else {
					f1 = o1.getFloatValue();
					f2 = o2.getFloatValue();
					f = f2 + f1;
					stack.push(new VSMXNumber(this, f));
				}
				break;
			case VSMXCode.VID_OPERATOR_SUBTRACT:
				f1 = stack.pop().getFloatValue();
				f2 = stack.pop().getFloatValue();
				f = f2 - f1;
				stack.push(new VSMXNumber(this, f));
				break;
			case VSMXCode.VID_OPERATOR_MULTIPLY:
				f1 = stack.pop().getFloatValue();
				f2 = stack.pop().getFloatValue();
				f = f2 * f1;
				stack.push(new VSMXNumber(this, f));
				break;
			case VSMXCode.VID_OPERATOR_DIVIDE:
				f1 = stack.pop().getFloatValue();
				f2 = stack.pop().getFloatValue();
				f = f2 / f1;
				stack.push(new VSMXNumber(this, f));
				break;
			case VSMXCode.VID_OPERATOR_MOD:
				f1 = stack.pop().getFloatValue();
				f2 = stack.pop().getFloatValue();
				f = f2 % f1;
				stack.push(new VSMXNumber(this, f));
				break;
			case VSMXCode.VID_OPERATOR_POSITIVE:
				f1 = stack.pop().getFloatValue();
				f = f1;
				stack.push(new VSMXNumber(this, f));
				break;
			case VSMXCode.VID_OPERATOR_NEGATE:
				f1 = stack.pop().getFloatValue();
				f = -f1;
				stack.push(new VSMXNumber(this, f));
				break;
			case VSMXCode.VID_OPERATOR_NOT:
				b = stack.pop().getBooleanValue();
				b = !b;
				stack.push(VSMXBoolean.getValue(b));
				break;
			case VSMXCode.VID_P_INCREMENT:
				o = stack.pop();
				f = o.getFloatValue();
				f += 1f;
				stack.push(new VSMXNumber(this, f));
				if (o instanceof VSMXReference) {
					((VSMXReference) o).assign(new VSMXNumber(this, f));
				} else {
					log.warn(String.format("Line#%d non-ref increment %s", pc - 1, code));
				}
				break;
			case VSMXCode.VID_P_DECREMENT:
				o = stack.pop();
				f = o.getFloatValue();
				f -= 1f;
				stack.push(new VSMXNumber(this, f));
				if (o instanceof VSMXReference) {
					((VSMXReference) o).assign(new VSMXNumber(this, f));
				} else {
					log.warn(String.format("Line#%d non-ref increment %s", pc - 1, code));
				}
				break;
			case VSMXCode.VID_INCREMENT:
				o = stack.pop();
				f = o.getFloatValue();
				stack.push(new VSMXNumber(this, f));
				if (o instanceof VSMXReference) {
					((VSMXReference) o).assign(new VSMXNumber(this, f + 1f));
				} else {
					log.warn(String.format("Line#%d non-ref increment %s", pc - 1, code));
				}
				break;
			case VSMXCode.VID_DECREMENT:
				o = stack.pop();
				f = o.getFloatValue();
				stack.push(new VSMXNumber(this, f));
				if (o instanceof VSMXReference) {
					((VSMXReference) o).assign(new VSMXNumber(this, f - 1f));
				} else {
					log.warn(String.format("Line#%d non-ref decrement %s", pc - 1, code));
				}
				break;
			case VSMXCode.VID_OPERATOR_EQUAL:
				o1 = stack.pop().getValue();
				o2 = stack.pop().getValue();
				b = o1.equals(o2);
				if (log.isTraceEnabled()) {
					log.trace(String.format("%s == %s: %b", o1, o2, b));
				}
				stack.push(VSMXBoolean.getValue(b));
				break;
			case VSMXCode.VID_OPERATOR_NOT_EQUAL:
				o1 = stack.pop().getValue();
				o2 = stack.pop().getValue();
				b = !o1.equals(o2);
				if (log.isTraceEnabled()) {
					log.trace(String.format("%s != %s: %b", o1, o2, b));
				}
				stack.push(VSMXBoolean.getValue(b));
				break;
			case VSMXCode.VID_OPERATOR_IDENTITY:
				o1 = stack.pop().getValue();
				o2 = stack.pop().getValue();
				b = o1.identity(o2);
				stack.push(VSMXBoolean.getValue(b));
				break;
			case VSMXCode.VID_OPERATOR_NON_IDENTITY:
				o1 = stack.pop().getValue();
				o2 = stack.pop().getValue();
				b = !o1.identity(o2);
				stack.push(VSMXBoolean.getValue(b));
				break;
			case VSMXCode.VID_OPERATOR_LT:
				f1 = stack.pop().getFloatValue();
				f2 = stack.pop().getFloatValue();
				b = f2 < f1;
				stack.push(VSMXBoolean.getValue(b));
				break;
			case VSMXCode.VID_OPERATOR_LTE:
				f1 = stack.pop().getFloatValue();
				f2 = stack.pop().getFloatValue();
				b = f2 <= f1;
				stack.push(VSMXBoolean.getValue(b));
				break;
			case VSMXCode.VID_OPERATOR_GTE:
				f1 = stack.pop().getFloatValue();
				f2 = stack.pop().getFloatValue();
				b = f2 >= f1;
				stack.push(VSMXBoolean.getValue(b));
				break;
			case VSMXCode.VID_OPERATOR_GT:
				f1 = stack.pop().getFloatValue();
				f2 = stack.pop().getFloatValue();
				b = f2 > f1;
				stack.push(VSMXBoolean.getValue(b));
				break;
			case VSMXCode.VID_OPERATOR_INSTANCEOF:
				log.warn(String.format("Line#%d unimplemented %s", pc - 1, code));
				break;
			case VSMXCode.VID_OPERATOR_IN:
				log.warn(String.format("Line#%d unimplemented %s", pc - 1, code));
				break;
			case VSMXCode.VID_OPERATOR_TYPEOF:
				o = stack.pop().getValue();
				String typeOf = o.typeOf();
				stack.push(new VSMXString(this, typeOf));
				break;
			case VSMXCode.VID_OPERATOR_B_AND:
				i1 = stack.pop().getIntValue();
				i2 = stack.pop().getIntValue();
				i = i1 & i2;
				stack.push(new VSMXNumber(this, i));
				break;
			case VSMXCode.VID_OPERATOR_B_XOR:
				i1 = stack.pop().getIntValue();
				i2 = stack.pop().getIntValue();
				i = i1 ^ i2;
				stack.push(new VSMXNumber(this, i));
				break;
			case VSMXCode.VID_OPERATOR_B_OR:
				i1 = stack.pop().getIntValue();
				i2 = stack.pop().getIntValue();
				i = i1 | i2;
				stack.push(new VSMXNumber(this, i));
				break;
			case VSMXCode.VID_OPERATOR_B_NOT:
				i1 = stack.pop().getIntValue();
				i = ~i1;
				stack.push(new VSMXNumber(this, i));
				break;
			case VSMXCode.VID_OPERATOR_LSHIFT:
				i1 = stack.pop().getIntValue();
				i2 = stack.pop().getIntValue();
				i = i2 << i1;
				stack.push(new VSMXNumber(this, i));
				break;
			case VSMXCode.VID_OPERATOR_RSHIFT:
				i1 = stack.pop().getIntValue();
				i2 = stack.pop().getIntValue();
				i = i2 >> i1;
				stack.push(new VSMXNumber(this, i));
				break;
			case VSMXCode.VID_OPERATOR_URSHIFT:
				i1 = stack.pop().getIntValue();
				i2 = stack.pop().getIntValue();
				i = i2 >>> i1;
				stack.push(new VSMXNumber(this, i));
				break;
			case VSMXCode.VID_STACK_COPY:
				o1 = stack.peek();
				stack.push(o1);
				break;
			case VSMXCode.VID_STACK_SWAP:
				o1 = stack.pop();
				o2 = stack.pop();
				stack.push(o1);
				stack.push(o2);
				break;
			case VSMXCode.VID_END_STMT:
				stack.clear();
				break;
			case VSMXCode.VID_CONST_NULL:
				stack.push(VSMXNull.singleton);
				break;
			case VSMXCode.VID_CONST_EMPTYARRAY:
				o = new VSMXArray(this);
				stack.push(o);
				break;
			case VSMXCode.VID_CONST_BOOL:
				stack.push(VSMXBoolean.getValue(code.value));
				break;
			case VSMXCode.VID_CONST_INT:
				stack.push(new VSMXNumber(this, code.value));
				break;
			case VSMXCode.VID_CONST_FLOAT:
				stack.push(new VSMXNumber(this, code.getFloatValue()));
				break;
			case VSMXCode.VID_CONST_STRING:
				stack.push(new VSMXString(this, mem.texts[code.value]));
				break;
			case VSMXCode.VID_CONST_OBJECT:
				break;
			case VSMXCode.VID_FUNCTION:
				stack.push(new VSMXFunction(this, (code.id >> 8) & 0xFF, (code.id >> 24) & 0xFF, code.value));
				break;
			case VSMXCode.VID_ARRAY:
				stack.push(new VSMXArray(this));
				break;
			case VSMXCode.VID_THIS:
				stack.push(callState.getThisObject());
				break;
			case VSMXCode.VID_UNNAMED_VAR:
				stack.push(new VSMXLocalVarReference(this, callState, code.value));
				break;
			case VSMXCode.VID_VARIABLE:
				stack.push(new VSMXReference(this, globalVariables, mem.names[code.value]));
				if (log.isTraceEnabled()) {
					log.trace(String.format("%s '%s'", VSMXCode.VsmxDecOps[code.getOpcode()], mem.names[code.value]));
				}
				break;
			case VSMXCode.VID_PROPERTY:
				o = stack.pop().getValue();
				if (o instanceof VSMXObject) {
					stack.push(new VSMXReference(this, (VSMXObject) o, mem.properties[code.value]));
					if (log.isTraceEnabled()) {
						log.trace(String.format("%s '%s': %s", VSMXCode.VsmxDecOps[code.getOpcode()], mem.properties[code.value], stack.peek()));
					}
				} else {
					stack.push(o.getPropertyValue(mem.properties[code.value]));
				}
				break;
			case VSMXCode.VID_METHOD:
				o = stack.pop().getValue();
				stack.push(new VSMXMethod(this, o, mem.properties[code.value]));
				if (log.isTraceEnabled()) {
					log.trace(String.format("%s '%s'", VSMXCode.VsmxDecOps[code.getOpcode()], mem.properties[code.value]));
				}
				break;
			case VSMXCode.VID_SET_ATTR:
				o1 = stack.pop().getValue();
				o2 = stack.pop();
				o2.setPropertyValue(mem.properties[code.value], o1);
				if (log.isTraceEnabled()) {
					log.trace(String.format("%s %s.%s = %s", VSMXCode.VsmxDecOps[code.getOpcode()], o2, mem.properties[code.value], o1));
				}
				break;
			case VSMXCode.VID_UNSET:
				o1 = stack.pop();
				o1.deletePropertyValue(mem.properties[code.value]);
				break;
			case VSMXCode.VID_OBJ_ADD_ATTR:
				log.warn(String.format("Line#%d unimplemented %s", pc - 1, code));
				break;
			case VSMXCode.VID_ARRAY_INDEX:
				o1 = stack.pop();
				o2 = stack.pop().getValue();
				if (o2 instanceof VSMXArray) {
					o = new VSMXReference(this, (VSMXObject) o2, o1.getIntValue());
					if (log.isTraceEnabled()) {
						log.trace(String.format("%s VSMXArray %s[%d] = %s", VSMXCode.VsmxDecOps[code.getOpcode()], o2, o1.getIntValue(), o));
					}
				} else if (o2 instanceof VSMXObject) {
					o = new VSMXReference(this, (VSMXObject) o2, o1.getStringValue());
					if (log.isTraceEnabled()) {
						log.trace(String.format("%s VSMXObject %s[%s] = %s", VSMXCode.VsmxDecOps[code.getOpcode()], o2, o1.getStringValue(), o));
					}
				} else {
					o = o2.getPropertyValue(o1.getStringValue());
					if (log.isTraceEnabled()) {
						log.trace(String.format("%s %s[%s] = %s", VSMXCode.VsmxDecOps[code.getOpcode()], o2, o1.getStringValue(), o));
					}
				}
				stack.push(o);
				break;
			case VSMXCode.VID_ARRAY_INDEX_KEEP_OBJ:
				o1 = stack.pop();
				o2 = stack.peek().getValue();
				if (o2 instanceof VSMXArray) {
					o = o2.getPropertyValue(o1.getIntValue());
				} else {
					o = o2.getPropertyValue(o1.getStringValue());
				}
				stack.push(o);
				break;
			case VSMXCode.VID_ARRAY_INDEX_ASSIGN:
				o1 = stack.pop().getValue();
				o2 = stack.pop();
				o3 = stack.pop().getValue();
				if (o3 instanceof VSMXArray) {
					o3.setPropertyValue(o2.getIntValue(), o1);
				} else {
					log.warn(String.format("Line#%d non-array index assignment %s", pc - 1, code));
				}
				break;
			case VSMXCode.VID_ARRAY_DELETE:
				o1 = stack.pop();
				o2 = stack.pop().getValue();
				if (o2 instanceof VSMXArray) {
					o2.deletePropertyValue(o1.getIntValue());
				} else {
					log.warn(String.format("Line#%d non-array delete %s", pc - 1, code));
				}
				break;
			case VSMXCode.VID_ARRAY_PUSH:
				o1 = stack.pop().getValue();
				o2 = stack.pop().getValue();
				if (o2 instanceof VSMXArray) {
					int length = ((VSMXArray) o2).getLength();
					o2.setPropertyValue(length, o1);
					stack.push(o2);
				} else {
					log.warn(String.format("Line#%d non-array push %s", pc - 1, code));
				}
				break;
			case VSMXCode.VID_JUMP:
				pc = code.value;
				break;
			case VSMXCode.VID_JUMP_TRUE:
				o1 = stack.pop();
				b = o1.getBooleanValue();
				if (b) {
					pc = code.value;
				}
				break;
			case VSMXCode.VID_JUMP_FALSE:
				o1 = stack.pop();
				b = !o1.getBooleanValue();
				if (b) {
					pc = code.value;
				}
				break;
			case VSMXCode.VID_CALL_FUNC:
				arguments = popValues(code.value);
				o = stack.pop().getValueWithArguments(code.value);
				if (o instanceof VSMXFunction) {
					VSMXFunction function = (VSMXFunction) o;

					callFunction(function, VSMXNull.singleton, arguments, code.value, false);
				} else {
					stack.push(VSMXNull.singleton);
					log.warn(String.format("Line#%d non-function call %s", pc - 1, code));
				}
				break;
			case VSMXCode.VID_CALL_METHOD:
				arguments = popValues(code.value);
				o = stack.pop().getValueWithArguments(code.value);
				if (o instanceof VSMXMethod) {
					VSMXMethod method = (VSMXMethod) o;
					VSMXFunction function = method.getFunction(code.value, arguments);

					if (function == null) {
						stack.push(VSMXNull.singleton);
						log.warn(String.format("Line#%d non existing method %s()", pc - 1, method.getName()));
					} else {
						callFunction(function, method.getThisObject(), method.getArguments(), method.getNumberOfArguments(), false);
					}
				} else if (o instanceof VSMXFunction) {
					VSMXFunction function = (VSMXFunction) o;
					o = stack.pop().getValue();
					callFunction(function, o, arguments, code.value, false);
				} else {
					stack.push(VSMXNull.singleton);
					log.warn(String.format("Line#%d non-method call %s", pc - 1, code));
				}
				break;
			case VSMXCode.VID_CALL_NEW:
				arguments = popValues(code.value);
				r = stack.pop();
				o = r.getValue();
				if (o instanceof VSMXArray) {
					if (code.value == 0) {
						// No arguments: create an empty array
						stack.push(new VSMXArray(this));
					} else if (code.value == 1) {
						// One argument: create an array of the given size
						stack.push(new VSMXArray(this, arguments[0].getIntValue()));
					} else {
						// More than 1 arguments: create an array containing the given values
						VSMXArray array = new VSMXArray(this, code.value);
						for (int j = 0; j < code.value; j++) {
							array.setPropertyValue(j, arguments[j]);
						}
						stack.push(array);
					}
				} else if (o instanceof VSMXFunction) {
					VSMXFunction function = (VSMXFunction) o;

					String className = null;
					if (r instanceof VSMXReference) {
						className = ((VSMXReference) r).getRefProperty();
					}
					VSMXObject thisObject = new VSMXObject(this, className);
					callFunction(function, thisObject, arguments, code.value, true);
				} else if (o instanceof VSMXObject) {
					if (code.value == 0) {
						stack.push(new VSMXObject(this, null));
					} else {
						log.warn(String.format("Line#%d wrong number of arguments for new Object %s", pc - 1, code));
					}
				} else {
					stack.push(new VSMXArray(this));
					log.warn(String.format("Line#%d unimplemented %s", pc - 1, code));
				}
				break;
			case VSMXCode.VID_RETURN:
				o = stack.pop().getValue();
				if (callState.getReturnThis()) {
					o = callState.getThisObject();
				}
				pc = callState.getReturnPc();
				if (callState.getExitAfterCall()) {
					exit = true;
				}
				popCallState();
				if (callState == null) {
					exit = true;
				} else {
					stack.push(o);
				}
				break;
			case VSMXCode.VID_THROW:
				log.warn(String.format("Line#%d unimplemented %s", pc - 1, code));
				break;
			case VSMXCode.VID_TRY_BLOCK_IN:
				log.warn(String.format("Line#%d unimplemented %s", pc - 1, code));
				break;
			case VSMXCode.VID_TRY_BLOCK_OUT:
				log.warn(String.format("Line#%d unimplemented %s", pc - 1, code));
				break;
			case VSMXCode.VID_CATCH_FINALLY_BLOCK_IN:
				log.warn(String.format("Line#%d unimplemented %s", pc - 1, code));
				break;
			case VSMXCode.VID_CATCH_FINALLY_BLOCK_OUT:
				log.warn(String.format("Line#%d unimplemented %s", pc - 1, code));
				break;
			case VSMXCode.VID_END:
				exit = true;
				break;
			case VSMXCode.VID_DEBUG_FILE:
				if (log.isDebugEnabled()) {
					log.debug(String.format("debug file '%s'", mem.texts[code.value]));
				}
				break;
			case VSMXCode.VID_DEBUG_LINE:
				if (log.isDebugEnabled()) {
					log.debug(String.format("debug line %d", code.value));
				}
				break;
			case VSMXCode.VID_MAKE_FLOAT_ARRAY:
				log.warn(String.format("Line#%d unimplemented %s", pc - 1, code));
				break;
			default:
				log.warn(String.format("Line#%d unimplemented %s", pc - 1, code));
				break;
		}
	}

	private void interpret() {
		exit = false;

		while (!exit) {
			VSMXGroup code = mem.codes[pc];
			if (log.isTraceEnabled()) {
				log.trace(String.format("%sInterpret Line#%d: %s", prefix, pc, code));
			}
			pc++;
			interpret(code);
		}

		exit = false;
	}

	public synchronized void run(VSMXObject globalVariables) {
		prefix = "";
		pc = 0;
		exit = false;
		callStates = new Stack<VSMXCallState>();
		pushCallState(VSMXNull.singleton, 0, false, true);
		this.globalVariables = globalVariables;

		VSMXBoolean.init(this);

		interpret();

		callStates.clear();
		callState = null;
		prefix = "";

		if (log.isTraceEnabled()) {
			log.trace(String.format("Global variables after run(): %s", globalVariables));
		}
	}

	private void callFunction(VSMXFunction function, VSMXBaseObject thisObject, VSMXBaseObject[] arguments, int numberArguments, boolean returnThis) {
		pushCallState(thisObject, function.getLocalVars() + function.getArgs(), returnThis, false);
		for (int i = 1; i <= function.getArgs() && i <= numberArguments; i++) {
			callState.setLocalVar(i, arguments[i - 1]);
		}

		function.call(callState);

		int startLine = function.getStartLine();
		if (startLine >= 0 && startLine < mem.codes.length) {
			pc = startLine;
		} else {
			popCallState();

			VSMXBaseObject returnValue = function.getReturnValue();
			if (returnThis) {
				stack.push(thisObject);
			} else if (returnValue != null) {
				stack.push(returnValue);
			}
		}
	}

	public synchronized void interpretFunction(VSMXFunction function, VSMXBaseObject object, VSMXBaseObject[] arguments) {
		pushCallState(object, function.getLocalVars(), false, true);
		for (int i = 1; i <= function.getArgs(); i++) {
			if (arguments == null || i > arguments.length) {
				callState.setLocalVar(i, VSMXNull.singleton);
			} else {
				callState.setLocalVar(i, arguments[i - 1]);
			}
		}
		pc = function.getStartLine();

		interpret();
	}

	public synchronized void delayInterpretFunction(VSMXFunction function, VSMXBaseObject object, VSMXBaseObject[] arguments) {
		IAction action = new InterpretFunctionAction(function, object, arguments);
		Emulator.getScheduler().addAction(action);
	}

	public synchronized void interpretScript(VSMXBaseObject object, String script) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("interpretScript %s on %s", script, object));
		}

		if (script == null) {
			return;
		}

		if (object == null) {
			object = VSMXNull.singleton;
		}

		String scriptPrefix = String.format("script:/%s/", name);
		if (script.startsWith(scriptPrefix)) {
			String functionName = script.substring(scriptPrefix.length());
			VSMXBaseObject functionObject = globalVariables.getPropertyValue(functionName);
			if (functionObject instanceof VSMXFunction) {
				VSMXFunction function = (VSMXFunction) functionObject;
				if (log.isDebugEnabled()) {
					log.debug(String.format("interpretScript function=%s", function));
				}
				interpretFunction(function, object, null);
			}
		} else {
			log.warn(String.format("interpretScript unknown script syntax '%s'", script));
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();

		s.append(String.format("pc=%d", pc));
		s.append(String.format(", %s", callState));

		return s.toString();
	}
}
