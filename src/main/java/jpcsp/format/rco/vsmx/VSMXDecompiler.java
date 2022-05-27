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
package jpcsp.format.rco.vsmx;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;

public class VSMXDecompiler {
	private static Logger log = VSMX.log;
	private VSMXMem mem;
	private String prefix;
	private Stack<Integer> blockEnd;
	private Stack<Integer> stack;
	private static final int SWITCH_STATE_NONE = 0;
	private static final int SWITCH_STATE_START = 1;
	private static final int SWITCH_STATE_VALUE = 2;
	private static final int SWITCH_STATE_CASE = 3;
	private static final int SWITCH_STATE_MULTI_VALUE = 4;
	private int switchState;
	private int switchBreakLine;
	private int ignoreFunctionSet;
	private int statementStartLine;
	private Set<Integer> needLineLabel;
	private StringBuilder booleanExpression;

	public VSMXDecompiler(VSMX vsmx) {
		mem = vsmx.getMem();
	}

	private void increaseIndent(int blockEndLine) {
		prefix += "  ";
		blockEnd.push(blockEndLine);
	}

	private void decrementIndent() {
		blockEnd.pop();
		prefix = prefix.substring(0, prefix.length() - 2);
	}

	private void operator2(StringBuilder s, String operator) {
		StringBuilder op1 = new StringBuilder();
		StringBuilder op2 = new StringBuilder();

		decompileOp(op1);
		decompileOp(op2);
		s.append(String.format("%s%s%s", op2, operator, op1));
	}

	private void operatorPre1(StringBuilder s, String operator) {
		StringBuilder op = new StringBuilder();

		decompileOp(op);
		s.append(String.format("%s%s", operator, op));
	}

	private void operatorPost1(StringBuilder s, String operator) {
		StringBuilder op = new StringBuilder();

		decompileOp(op);
		s.append(String.format("%s%s", op, operator));
	}

	private boolean isBooleanExpression(StringBuilder s) {
		if (stack.isEmpty()) {
			return false;
		}
		int i = stack.peek().intValue();
		if (!mem.codes[i].isOpcode(VSMXCode.VID_STACK_COPY)) {
			return false;
		}

		stack.pop();
		decompileOp(s);

		return true;
	}

	private void addToBooleanExpression(StringBuilder s) {
		if (booleanExpression == null) {
			booleanExpression = new StringBuilder();
		}
		booleanExpression.append(s.toString());
	}

	private void addToBooleanExpression(StringBuilder s, boolean isOr) {
		addToBooleanExpression(s);
		booleanExpression.append(isOr ? " || " : " && ");
	}

	private void decompileStmt(StringBuilder s) {
		if (stack.isEmpty()) {
			return;
		}
		int i = stack.pop();
		decompileStmt(s, i);
	}

	private boolean detectSwitch(StringBuilder s, int i) {
		if (mem.codes[i + 1].isOpcode(VSMXCode.VID_DEBUG_LINE)) {
			i++;
		}
		if (!mem.codes[i + 1].isOpcode(VSMXCode.VID_JUMP)) {
			return false;
		}

		if (blockEnd.size() > 0 && blockEnd.peek().intValue() == i) {
			return false;
		}

		return true;
	}

	private boolean isSwitch(int jumpLine) {
		if (switchState == SWITCH_STATE_NONE) {
			return false;
		}

		if (switchState == SWITCH_STATE_CASE && switchBreakLine >= 0 && jumpLine != switchBreakLine) {
			return false;
		}

		return true;
	}

	private int decompileSwitch(StringBuilder s, int i, int jumpLine) {
		StringBuilder op;
		switch (switchState) {
			case SWITCH_STATE_NONE:
				op = new StringBuilder();
				decompileOp(op);
				s.append(String.format("%sswitch (%s) {\n", prefix, op));
				switchState = SWITCH_STATE_START;
				switchBreakLine = -1;
				break;
			case SWITCH_STATE_START:
				if (switchBreakLine >= 0 && jumpLine == switchBreakLine) {
					switchState = SWITCH_STATE_NONE;
					i = switchBreakLine - 1;
				} else {
					switchState = SWITCH_STATE_VALUE;
				}
				break;
			case SWITCH_STATE_VALUE:
				op = new StringBuilder();
				decompileOp(op);
				if (mem.codes[i + 1].isOpcode(VSMXCode.VID_DEBUG_LINE)) {
					i++;
				}
				if (mem.codes[i + 1].isOpcode(VSMXCode.VID_JUMP)) {
					s.append(String.format("%scase %s:\n", prefix, op));
					switchState = SWITCH_STATE_MULTI_VALUE;
				} else {
					s.append(String.format("%scase %s: {\n", prefix, op));
					switchState = SWITCH_STATE_CASE;
					increaseIndent(0);
				}
				break;
			case SWITCH_STATE_MULTI_VALUE:
				switchState = SWITCH_STATE_VALUE;
				break;
			case SWITCH_STATE_CASE:
				s.append(String.format("%sbreak;\n", prefix));
				decrementIndent();
				s.append(String.format("%s}\n", prefix));
				switchBreakLine = jumpLine;
				switchState = SWITCH_STATE_START;
				break;
		}

		return i;
	}

	private boolean isFunction(int i) {
		if (stack.isEmpty()) {
			return false;
		}

		int prev = stack.peek().intValue();
		VSMXGroup prevCode = mem.codes[prev];
		if (prevCode.isOpcode(VSMXCode.VID_OPERATOR_ASSIGN)) {
			prev--;
			prevCode = mem.codes[prev];
		}

		if (!prevCode.isOpcode(VSMXCode.VID_FUNCTION) || prevCode.value != i + 1) {
			return false;
		}

		VSMXGroup prePrevCode = mem.codes[prev - 1];
		if (prePrevCode.isOpcode(VSMXCode.VID_PROPERTY)) {
			prev--;
			prePrevCode = mem.codes[prev - 1];
		}

		if (!prePrevCode.isOpcode(VSMXCode.VID_VARIABLE)) {
			return false;
		}

		return true;
	}

	private void decompileFunction(StringBuilder s, int setLine) {
		StringBuilder function = new StringBuilder();
		decompileOp(function);
		StringBuilder name = new StringBuilder();
		decompileOp(name);
		stack.push(setLine);
		StringBuilder set = new StringBuilder();
		decompileOp(set);
		s.append(String.format("%s%s.%s = %s\n", prefix, name, set, function));

		increaseIndent(setLine);

		ignoreFunctionSet = setLine;
	}

	private void decompileOp(StringBuilder s) {
		if (stack.isEmpty()) {
			return;
		}
		int i = stack.pop();
		VSMXGroup code = mem.codes[i];
		int opcode = code.getOpcode();
		int args;
		StringBuilder ops[];
		StringBuilder op, op1, op2, method;
		switch (opcode) {
			case VSMXCode.VID_VARIABLE:
				s.append(mem.names[code.value]);
				break;
			case VSMXCode.VID_UNNAMED_VAR:
				s.append(String.format("var%d", code.value));
				break;
			case VSMXCode.VID_CONST_BOOL:
				if (code.value == 1) {
					s.append("true");
				} else if (code.value == 0) {
					s.append("false");
				} else {
					s.append(String.format("0x%X", code.value));
				}
				break;
			case VSMXCode.VID_CONST_INT:
				s.append(String.format("%d", code.value));
				break;
			case VSMXCode.VID_CONST_FLOAT:
				s.append(String.format("%f", code.getFloatValue()));
				break;
			case VSMXCode.VID_CONST_STRING:
			case VSMXCode.VID_DEBUG_FILE:
				s.append(String.format("\"%s\"", mem.texts[code.value]));
				break;
			case VSMXCode.VID_PROPERTY:
				op = new StringBuilder();
				decompileOp(op);
				s.append(String.format("%s.%s", op, mem.properties[code.value]));
				break;
			case VSMXCode.VID_METHOD:
			case VSMXCode.VID_SET_ATTR:
			case VSMXCode.VID_UNSET:
			case VSMXCode.VID_OBJ_ADD_ATTR:
				s.append(mem.properties[code.value]);
				break;
			case VSMXCode.VID_FUNCTION:
				args = (code.id >> 8) & 0xFF;
				s.append("function(");
				for (int n = 0; n < args; n++) {
					if (n > 0) {
						s.append(", ");
					}
					s.append(String.format("var%d", n + 1));
				}
				s.append(String.format(") {"));
				break;
			case VSMXCode.VID_CONST_EMPTYARRAY:
				s.append("{}");
				break;
			case VSMXCode.VID_CONST_NULL:
				s.append("null");
				break;
			case VSMXCode.VID_THIS:
				s.append("this");
				break;
			case VSMXCode.VID_ARRAY_INDEX:
				op1 = new StringBuilder();
				decompileOp(op1);
				op2 = new StringBuilder();
				decompileOp(op2);
				s.append(String.format("%s[%s]", op2, op1));
				break;
			case VSMXCode.VID_ARRAY_INDEX_KEEP_OBJ:
				op1 = new StringBuilder();
				decompileOp(op1);
				i = stack.peek();
				op2 = new StringBuilder();
				decompileOp(op2);
				stack.push(i);
				s.append(String.format("%s[%s]", op2, op1));
				break;
			case VSMXCode.VID_CALL_NEW:
				args = code.value;
				ops = new StringBuilder[args];
				for (int n = args - 1; n >= 0; n--) {
					ops[n] = new StringBuilder();
					decompileOp(ops[n]);
				}
				op = new StringBuilder();
				decompileOp(op);
				s.append(String.format("new %s(", op));
				for (int n = 0; n < args; n++) {
					if (n > 0) {
						s.append(", ");
					}
					s.append(ops[n]);
				}
				s.append(")");
				break;
			case VSMXCode.VID_CALL_METHOD:
				args = code.value;
				ops = new StringBuilder[args];
				for (int n = args - 1; n >= 0; n--) {
					ops[n] = new StringBuilder();
					decompileOp(ops[n]);
				}
				method = new StringBuilder();
				decompileOp(method);
				op = new StringBuilder();
				decompileOp(op);
				s.append(String.format("%s.%s(", op, method));
				for (int n = 0; n < args; n++) {
					if (n > 0) {
						s.append(", ");
					}
					s.append(ops[n]);
				}
				s.append(")");
				break;
			case VSMXCode.VID_CALL_FUNC:
				args = code.value;
				ops = new StringBuilder[args];
				for (int n = args - 1; n >= 0; n--) {
					ops[n] = new StringBuilder();
					decompileOp(ops[n]);
				}
				method = new StringBuilder();
				decompileOp(method);
				s.append(String.format("%s(", method));
				for (int n = 0; n < args; n++) {
					if (n > 0) {
						s.append(", ");
					}
					s.append(ops[n]);
				}
				s.append(")");
				break;
			case VSMXCode.VID_OPERATOR_EQUAL:
				operator2(s, " == ");
				break;
			case VSMXCode.VID_OPERATOR_NOT_EQUAL:
				operator2(s, " != ");
				break;
			case VSMXCode.VID_OPERATOR_GT:
				operator2(s, " > ");
				break;
			case VSMXCode.VID_OPERATOR_GTE:
				operator2(s, " >= ");
				break;
			case VSMXCode.VID_OPERATOR_LT:
				operator2(s, " < ");
				break;
			case VSMXCode.VID_OPERATOR_LTE:
				operator2(s, " <= ");
				break;
			case VSMXCode.VID_OPERATOR_NOT:
				operatorPre1(s, "!");
				break;
			case VSMXCode.VID_OPERATOR_NEGATE:
				operatorPre1(s, "-");
				break;
			case VSMXCode.VID_OPERATOR_ADD:
				operator2(s, " + ");
				break;
			case VSMXCode.VID_OPERATOR_SUBTRACT:
				operator2(s, " - ");
				break;
			case VSMXCode.VID_OPERATOR_MULTIPLY:
				operator2(s, " * ");
				break;
			case VSMXCode.VID_OPERATOR_DIVIDE:
				operator2(s, " / ");
				break;
			case VSMXCode.VID_OPERATOR_MOD:
				operator2(s, " % ");
				break;
			case VSMXCode.VID_OPERATOR_B_AND:
				operator2(s, " & ");
				break;
			case VSMXCode.VID_OPERATOR_B_XOR:
				operator2(s, " ^ ");
				break;
			case VSMXCode.VID_OPERATOR_B_OR:
				operator2(s, " | ");
				break;
			case VSMXCode.VID_OPERATOR_B_NOT:
				operatorPre1(s, "~");
				break;
			case VSMXCode.VID_OPERATOR_LSHIFT:
				operator2(s, " << ");
				break;
			case VSMXCode.VID_OPERATOR_RSHIFT:
				operator2(s, " >> ");
				break;
			case VSMXCode.VID_OPERATOR_URSHIFT:
				operator2(s, " >>> ");
				break;
			case VSMXCode.VID_INCREMENT:
				operatorPost1(s, "++");
				break;
			case VSMXCode.VID_DECREMENT:
				operatorPost1(s, "--");
				break;
			case VSMXCode.VID_P_INCREMENT:
				operatorPre1(s, "++");
				break;
			case VSMXCode.VID_P_DECREMENT:
				operatorPre1(s, "--");
				break;
			case VSMXCode.VID_ARRAY_PUSH:
				op1 = new StringBuilder();
				decompileOp(op1);
				if (!stack.isEmpty() && mem.codes[stack.peek().intValue()].isOpcode(VSMXCode.VID_ARRAY_PUSH)) {
					// Display nicely an array initialization
					while (!stack.isEmpty() && mem.codes[stack.peek().intValue()].isOpcode(VSMXCode.VID_ARRAY_PUSH)) {
						stack.pop();
						op2 = new StringBuilder();
						decompileOp(op2);
						op1.insert(0, String.format(",\n%s  ", prefix));
						op1.insert(0, op2.toString());
					}
					op2 = new StringBuilder();
					decompileOp(op2);
					s.append(String.format("%s {\n%s  %s\n%s}", op2, prefix, op1, prefix));
				} else {
					op2 = new StringBuilder();
					decompileOp(op2);
					s.append(String.format("%s.push(%s)", op2, op1));
				}
				break;
			case VSMXCode.VID_ARRAY:
				s.append("new Array()");
				break;
			case VSMXCode.VID_OPERATOR_ASSIGN:
				op1 = new StringBuilder();
				decompileOp(op1);
				op2 = new StringBuilder();
				decompileOp(op2);
				s.append(String.format("%s = %s", op2, op1));
				break;
			case VSMXCode.VID_STACK_COPY:
				if (!stack.isEmpty()) {
					i = stack.pop();
					stack.push(i);
					stack.push(i);
					decompileOp(s);
				}
				break;
			case VSMXCode.VID_DEBUG_LINE:
				// Ignore debug line
				decompileOp(s);
				break;
			default:
				log.warn(String.format("Line #%d: decompileOp(%s) unimplemented", i, VSMXCode.VsmxDecOps[opcode]));
				break;
		}
	}

	private int decompileStmt(StringBuilder s, int i) {
		int initialLength = s.length();
		VSMXGroup code = mem.codes[i];
		int opcode = code.getOpcode();
		StringBuilder op1;
		StringBuilder op2;
		StringBuilder op3;
		switch (opcode) {
			case VSMXCode.VID_OPERATOR_ASSIGN:
				op1 = new StringBuilder();
				decompileOp(op1);
				op2 = new StringBuilder();
				decompileOp(op2);
				s.append(String.format("%s%s = %s", prefix, op2, op1));
				break;
			case VSMXCode.VID_ARRAY_INDEX_ASSIGN:
				op1 = new StringBuilder();
				decompileOp(op1);
				op2 = new StringBuilder();
				decompileOp(op2);
				op3 = new StringBuilder();
				decompileOp(op3);
				s.append(String.format("%s%s[%s] = %s", prefix, op3, op2, op1));
				break;
			case VSMXCode.VID_CALL_FUNC:
			case VSMXCode.VID_CALL_METHOD:
				stack.push(i);
				op1 = new StringBuilder();
				decompileOp(op1);
				s.append(String.format("%s%s", prefix, op1));
				break;
			case VSMXCode.VID_JUMP_TRUE:
				op1 = new StringBuilder();
				if (isBooleanExpression(op1)) {
					addToBooleanExpression(op1, true);
				} else if (booleanExpression != null) {
					decompileOp(op1);
					addToBooleanExpression(op1, true);
					s.append(String.format("%sif (%s) {", prefix, booleanExpression));
					increaseIndent(code.value);
					booleanExpression = null;
				} else {
					decompileOp(op1);
					// this is probably a "for" loop
					s.append(String.format("%d:\n", statementStartLine));
					if (mem.codes[i + 1].isOpcode(VSMXCode.VID_JUMP)) {
						int elseGoto = mem.codes[i + 1].value;
						s.append(String.format("%sif (%s) goto %d; else goto %d", prefix, op1, code.value, elseGoto));
						needLineLabel.add(elseGoto);
						needLineLabel.add(i + 2);
						i++;
					} else {
						s.append(String.format("%sif (%s) goto %d", prefix, op1, code.value));
					}
					needLineLabel.add(code.value);
				}
				break;
			case VSMXCode.VID_JUMP_FALSE:
				op1 = new StringBuilder();
				if (isBooleanExpression(op1)) {
					addToBooleanExpression(op1, false);
				} else if (booleanExpression != null) {
					decompileOp(op1);
					addToBooleanExpression(op1);
					s.append(String.format("%sif (%s) {", prefix, booleanExpression));
					increaseIndent(code.value);
					booleanExpression = null;
				} else {
					decompileOp(op1);
					s.append(String.format("%sif (%s) {", prefix, op1));
					increaseIndent(code.value);
				}
				break;
			case VSMXCode.VID_RETURN:
				op1 = new StringBuilder();
				decompileOp(op1);
				s.append(String.format("%sreturn %s", prefix, op1));
				break;
			case VSMXCode.VID_SET_ATTR:
				if (i == ignoreFunctionSet) {
					ignoreFunctionSet = -1;
				} else {
					op1 = new StringBuilder();
					decompileOp(op1);
					op2 = new StringBuilder();
					decompileOp(op2);
					s.append(String.format("%s%s.%s = %s", prefix, op2, mem.properties[code.value], op1));
				}
				break;
			case VSMXCode.VID_INCREMENT:
				s.append(prefix);
				operatorPost1(s, "++");
				break;
			case VSMXCode.VID_DECREMENT:
				s.append(prefix);
				operatorPost1(s, "--");
				break;
			case VSMXCode.VID_P_INCREMENT:
				s.append(prefix);
				operatorPre1(s, "++");
				break;
			case VSMXCode.VID_P_DECREMENT:
				s.append(prefix);
				operatorPre1(s, "--");
				break;
			case VSMXCode.VID_OPERATOR_EQUAL:
				operator2(s, " == ");
				break;
			case VSMXCode.VID_JUMP:
				if (code.value > i) {
					increaseIndent(code.value);
				} else {
					// Backward loop
					s.append(String.format("%sgoto %d", prefix, code.value));
				}
				break;
			case VSMXCode.VID_VARIABLE:
				s.append(prefix);
				s.append(mem.names[code.value]);
				break;
			case VSMXCode.VID_UNNAMED_VAR:
				s.append(prefix);
				s.append(String.format("var%d", code.value));
				break;
			case VSMXCode.VID_DEBUG_LINE:
				break;
			default:
				log.warn(String.format("Line #%d: decompileStmt(%s) unimplemented", i, VSMXCode.VsmxDecOps[opcode]));
				break;
		}

		if (s.length() != initialLength) {
			if (s.charAt(s.length() - 1) != '{') {
				s.append(";");
			}
			s.append(String.format(" // line %d", i));
			s.append("\n");
		}

		return i;
	}

	private String decompile() {
		StringBuilder s = new StringBuilder();

		prefix = "";
		stack = new Stack<Integer>();
		blockEnd = new Stack<Integer>();
		switchState = SWITCH_STATE_NONE;
		ignoreFunctionSet = -1;
		statementStartLine = 0;
		needLineLabel = new HashSet<Integer>();

		for (int i = 0; i < mem.codes.length; i++) {
			VSMXGroup code = mem.codes[i];
			int opcode = code.getOpcode();
			while (!blockEnd.isEmpty() && blockEnd.peek().intValue() == i) {
				decrementIndent();
				s.append(String.format("%s}\n", prefix));
			}
			if (needLineLabel.remove(i)) {
				s.append(String.format("%d:\n", i));
			}
			switch (opcode) {
				case VSMXCode.VID_END_STMT:
					decompileStmt(s);
					statementStartLine = i + 1;
					break;
				case VSMXCode.VID_RETURN:
				case VSMXCode.VID_JUMP_FALSE:
				case VSMXCode.VID_JUMP_TRUE:
					i = decompileStmt(s, i);
					break;
				case VSMXCode.VID_JUMP:
					if (isSwitch(code.value) || detectSwitch(s, i)) {
						i = decompileSwitch(s, i, code.value);
					} else if (isFunction(i)) {
						decompileFunction(s, code.value);
					} else {
						if (!blockEnd.isEmpty() && blockEnd.peek().intValue() == i + 1) {
							decrementIndent();
							s.append(String.format("%s} else {\n", prefix));
						}
						i = decompileStmt(s, i);
					}
					break;
				default:
					stack.push(i);
					break;
			}
		}

		return s.toString();
	}

	@Override
	public String toString() {
		return decompile();
	}
}
