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

import static org.objectweb.asm.tree.AbstractInsnNode.JUMP_INSN;
import static org.objectweb.asm.tree.AbstractInsnNode.TABLESWITCH_INSN;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * @author gid15
 *
 * Specialize a Java class by modifying its code to exclude part of it,
 * based on a list of field values dynamically computed at runtime.
 * The specialized class contains only the part of the code that will be
 * executed for the given field values, without the overhead for testing these values.
 * 
 * For example, given the following class:
 *    public class Test {
 *      public static int testValue;
 * 		int test(int parameter) {
 *        if (testValue == 0) {
 *          return 0;
 *        } else if (testValue < 0) {
 *          return -parameter;
 *        } else {
 *          return parameter;
 *        }
 *      }
 *    }
 *
 * A specialized class for the following field values:
 *    testValue = 123;
 * would be
 *    public class SpecialitedTest1 {
 *      int test(int parameter) {
 *        return parameter;
 *      }
 *    }
 *
 * and for
 *    testValue = -123;
 * it would be
 *    public class SpecialitedTest2 {
 *      int test(int parameter) {
 *        return -parameter;
 *      }
 *    }
 *
 * The following code statements can be evaluated by the specializer:
 * - if
 * - switch
 * - while
 * on field values of the following types:
 * - int
 * - byte
 * - short
 * - boolean
 * - float
 */
public class ClassSpecializer {
	private static Logger log = Logger.getLogger("classSpecializer");
	private static SpecializedClassLoader classLoader = new SpecializedClassLoader();

	public Class<?> specialize(String name, Class<?> c, HashMap<String, Object> variables) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		ClassVisitor cv = cw;

		StringWriter debugOutput = null;
		if (log.isTraceEnabled()) {
			log.trace(String.format("Specializing class %s", name));
			String[] variableNames = variables.keySet().toArray(new String[variables.size()]);
			Arrays.sort(variableNames);
			for (String variableName : variableNames) {
				log.trace(String.format("Variable %s=%s", variableName, variables.get(variableName)));
			}

			debugOutput = new StringWriter();
			PrintWriter debugPrintWriter = new PrintWriter(debugOutput);
			cv = new TraceClassVisitor(cv, debugPrintWriter);
			//cv = new TraceClassVisitor(debugPrintWriter);
		}

		try {
			ClassReader cr = new ClassReader(c.getName().replace('.', '/'));
			ClassNode cn = new SpecializedClassVisitor(name, variables);
			cr.accept(cn, 0);
			cn.accept(cv);
		} catch (IOException e) {
			log.error("Cannot read class", e);
		}

		if (debugOutput != null) {
			log.trace(debugOutput.toString());
		}

		Class<?> specializedClass = null;
		try {
			specializedClass = classLoader.defineClass(name, cw.toByteArray());
		} catch (ClassFormatError e) {
			log.error("Error while defining specialized class", e);
		}

		return specializedClass;
	}

	private static class SpecializedClassVisitor extends ClassNode {
		private final String specializedClassName;
		private final HashMap<String, Object> variables;
		private Object value;
		private AbstractInsnNode deleteUpToInsn;
		private String className;
		private String superClassName;

		public SpecializedClassVisitor(String specializedClassName, HashMap<String, Object> variables) {
			this.specializedClassName = specializedClassName;
			this.variables = variables;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			className = name;
			superClassName = superName;
			// Define the specialized class as extending the original class
			super.visit(version, access, specializedClassName, signature, name, interfaces);
		}

		@Override
		public void visitEnd() {
			// Visit all the methods
			for (Iterator<?> it = methods.iterator(); it.hasNext(); ) {
				MethodNode method = (MethodNode) it.next();
				visitMethod(method);
			}

			// Delete all the fields used as specialization variables
			for (ListIterator<?> lit = fields.listIterator(); lit.hasNext(); ) {
				FieldNode field = (FieldNode) lit.next();
				if ((field.access & Opcodes.ACC_STATIC) != 0 && variables.containsKey(field.name)) {
					lit.remove();
				}
			}

			super.visitEnd();
		}

		private void visitMethod(MethodNode method) {
			final boolean isConstructor = "<init>".equals(method.name);

			deleteUpToInsn = null;
			for (ListIterator<?> lit = method.instructions.iterator(); lit.hasNext(); ) {
				AbstractInsnNode insn = (AbstractInsnNode) lit.next();

				if (deleteUpToInsn != null) {
					if (insn == deleteUpToInsn) {
						deleteUpToInsn = null;
					} else {
						// Do not delete labels, they could be used as a target from a previous jump.
						// Also keep line numbers for easier debugging.
						if (insn.getType() != AbstractInsnNode.LABEL && insn.getType() != AbstractInsnNode.LINE) {
							lit.remove();
						}
						continue;
					}
				}

				if (insn.getType() == AbstractInsnNode.FRAME) {
					// Remove all the FRAME information, they will be calculated
					// anew after the class specialization.
					lit.remove();
				} else if (insn.getOpcode() == Opcodes.GETSTATIC) {
					FieldInsnNode fieldInsn = (FieldInsnNode) insn;
					if (variables.containsKey(fieldInsn.name)) {
						boolean processed = false;
						value = variables.get(fieldInsn.name);
						AbstractInsnNode nextInsn = insn.getNext();
						if (analyseIfTestInt(insn)) {
							processed = true;
						} else if (nextInsn != null && nextInsn.getType() == TABLESWITCH_INSN) {
							TableSwitchInsnNode switchInsn = (TableSwitchInsnNode) nextInsn;
							LabelNode label = null;
							if (isIntValue(value)) {
								int n = getIntValue(value);
								if (n >= switchInsn.min && n <= switchInsn.max) {
									int i = n - switchInsn.min;
									if (i < switchInsn.labels.size()) {
										label = (LabelNode) switchInsn.labels.get(i);
									}
								}
							}
							if (label == null) {
								label = switchInsn.dflt;
							}
							if (label != null) {
								processed = true;
								// Delete up to the switch label.
								// The other switch cases will be eliminated
								// by the dead code removal.
								deleteUpToInsn = label;
							}
						} else if (nextInsn != null && nextInsn.getType() == AbstractInsnNode.INSN) {
							int opcode = nextInsn.getOpcode();
							int n = 0;
							float f = 0f;
							boolean isIntConstant = false;
							boolean isFloatConstant = false;
							switch (opcode) {
								case Opcodes.ICONST_M1: n = -1; isIntConstant = true; break;
								case Opcodes.ICONST_0: n = 0; isIntConstant = true; break;
								case Opcodes.ICONST_1: n = 1; isIntConstant = true; break;
								case Opcodes.ICONST_2: n = 2; isIntConstant = true; break;
								case Opcodes.ICONST_3: n = 3; isIntConstant = true; break;
								case Opcodes.ICONST_4: n = 4; isIntConstant = true; break;
								case Opcodes.ICONST_5: n = 5; isIntConstant = true; break;
								case Opcodes.FCONST_0: f = 0f; isFloatConstant = true; break;
								case Opcodes.FCONST_1: f = 1f; isFloatConstant = true; break;
								case Opcodes.FCONST_2: f = 2f; isFloatConstant = true; break;
							}
							if (isIntConstant) {
								if (analyseIfTestInt(nextInsn, n)) {
									processed = true;
								}
							} else if (isFloatConstant) {
								if (analyseIfTestFloat(nextInsn, f)) {
									processed = true;
								}
							}
						} else if (nextInsn != null && nextInsn.getType() == AbstractInsnNode.INT_INSN) {
							IntInsnNode intInsn = (IntInsnNode) nextInsn;
							if (analyseIfTestInt(nextInsn, intInsn.operand)) {
								processed = true;
							}
						} else if (nextInsn != null && nextInsn.getType() == AbstractInsnNode.LDC_INSN) {
							LdcInsnNode ldcInsn = (LdcInsnNode) nextInsn;
							if (isIntValue(ldcInsn.cst)) {
								if (analyseIfTestInt(nextInsn, getIntValue(ldcInsn.cst))) {
									processed = true;
								}
							} else if (isFloatValue(ldcInsn.cst)) {
								if (analyseIfTestFloat(nextInsn, getFloatValue(ldcInsn.cst))) {
									processed = true;
								}
							}
						}

						if (processed) {
							lit.remove();
						} else {
							// Replace the field access by its constant value
							AbstractInsnNode constantInsn = getConstantInsn(value);
							if (constantInsn != null) {
								method.instructions.set(insn, constantInsn);
							}
						}
					} else {
						if (fieldInsn.owner.equals(className)) {
							// Replace the class name by the specialized class name
							fieldInsn.owner = specializedClassName;
						}
					}
				} else if (insn.getOpcode() == Opcodes.PUTSTATIC) {
					FieldInsnNode fieldInsn = (FieldInsnNode) insn;
					if (!variables.containsKey(fieldInsn.name)) {
						if (fieldInsn.owner.equals(className)) {
							// Replace the class name by the specialized class name
							fieldInsn.owner = specializedClassName;
						}
					}
				} else if (insn.getType() == AbstractInsnNode.METHOD_INSN) {
					MethodInsnNode methodInsn = (MethodInsnNode) insn;
					if (methodInsn.owner.equals(className)) {
						// Replace the class name by the specialized class name
						methodInsn.owner = specializedClassName;
					} else if (isConstructor && methodInsn.owner.equals(superClassName)) {
						// Update the call to the constructor of the parent class
						methodInsn.owner = className;
					}
				}
			}

			// Delete all the information about local variables, they are no longer correct
			// (the class loader would complain).
			method.localVariables.clear();

			optimizeJumps(method);
			removeDeadCode(method);
			optimizeJumps(method);
		}

		/**
		 * Optimize the jumps from a method:
		 * - jumps to a "GOTO label" instruction
		 *   are replaced with a direct jump to "label";
		 * - a GOTO to the next instruction is deleted;
		 * - a GOTO to a RETURN or ATHROW instruction
		 *   is replaced with this RETURN or ATHROW instruction.
		 *
		 * @param method  the method to be optimized
		 */
		private void optimizeJumps(MethodNode method) {
			for (ListIterator<?> lit = method.instructions.iterator(); lit.hasNext(); ) {
				AbstractInsnNode insn = (AbstractInsnNode) lit.next();
				if (insn.getType() == AbstractInsnNode.JUMP_INSN) {
					JumpInsnNode jumpInsn = (JumpInsnNode) insn;
					LabelNode label = jumpInsn.label;
					AbstractInsnNode target;
					// while target == goto l, replace label with l
					while (true) {
						target = label;
						while (target != null && target.getOpcode() < 0) {
							target = target.getNext();
						}
						if (target != null && target.getOpcode() == Opcodes.GOTO) {
							label = ((JumpInsnNode) target).label;
						} else {
							break;
						}
					}

					// update target
					jumpInsn.label = label;

					boolean removeJump = false;
					if (jumpInsn.getOpcode() == Opcodes.GOTO) {
						// Delete a GOTO to the next instruction
						AbstractInsnNode next = jumpInsn.getNext();
						while (next != null) {
							if (next == label) {
								removeJump = true;
								break;
							} else if (next.getOpcode() >= 0) {
								break;
							}
							next = next.getNext();
						}
					}

					if (removeJump) {
						lit.remove();
					} else {
						// if possible, replace jump with target instruction
						if (jumpInsn.getOpcode() == Opcodes.GOTO && target != null) {
							switch (target.getOpcode()) {
								case Opcodes.IRETURN:
								case Opcodes.LRETURN:
								case Opcodes.FRETURN:
								case Opcodes.DRETURN:
								case Opcodes.ARETURN:
								case Opcodes.RETURN:
								case Opcodes.ATHROW:
									// replace instruction with clone of target
									method.instructions.set(insn, target.clone(null));
							}
						}
					}
				}
			}
		}

		/**
		 * Remove the dead code - or unreachable code - from a method.
		 * 
		 * @param method  the method to be updated
		 */
		private void removeDeadCode(MethodNode method) {
			try {
				// Analyze the method using the BasicInterpreter.
				// As a result, the computed frames are null for instructions
				// that cannot be reached.
				Analyzer analyzer = new Analyzer(new BasicInterpreter());
				analyzer.analyze(specializedClassName, method);
				Frame[] frames = analyzer.getFrames();
				AbstractInsnNode[] insns = method.instructions.toArray();
				for (int i = 0; i < frames.length; i++) {
					AbstractInsnNode insn = insns[i];
					if (frames[i] == null && insn.getType() != AbstractInsnNode.LABEL) {
						// This instruction was not reached by the analyzer
						method.instructions.remove(insn);
						insns[i] = null;
					}
				}
			} catch (AnalyzerException e) {
				// Ignore error
			}
		}

		private boolean analyseIfTestInt(AbstractInsnNode insn) {
			return analyseIfTestInt(insn, null);
		}

		private boolean analyseIfTestInt(AbstractInsnNode insn, Integer testValue) {
			boolean eliminateJump = false;

			AbstractInsnNode nextInsn = insn.getNext();
			if (nextInsn != null && nextInsn.getType() == JUMP_INSN) {
				JumpInsnNode jumpInsn = (JumpInsnNode) nextInsn;
				boolean doJump = false;
				switch (jumpInsn.getOpcode()) {
					case Opcodes.IFEQ:
						if (testValue == null && isIntValue(value)) {
							doJump = getIntValue(value) == 0;
							eliminateJump = true;
						}
						break;
					case Opcodes.IFNE:
						if (testValue == null && isIntValue(value)) {
							doJump = getIntValue(value) != 0;
							eliminateJump = true;
						}
						break;
					case Opcodes.IFLT:
						if (testValue == null && isIntValue(value)) {
							doJump = getIntValue(value) < 0;
							eliminateJump = true;
						}
						break;
					case Opcodes.IFGE:
						if (testValue == null && isIntValue(value)) {
							doJump = getIntValue(value) >= 0;
							eliminateJump = true;
						}
						break;
					case Opcodes.IFGT:
						if (testValue == null && isIntValue(value)) {
							doJump = getIntValue(value) > 0;
							eliminateJump = true;
						}
						break;
					case Opcodes.IFLE:
						if (testValue == null && isIntValue(value)) {
							doJump = getIntValue(value) <= 0;
							eliminateJump = true;
						}
						break;
					case Opcodes.IF_ICMPEQ:
						if (testValue != null && isIntValue(value)) {
							doJump = getIntValue(value) == testValue.intValue();
							eliminateJump = true;
						}
						break;
					case Opcodes.IF_ICMPNE:
						if (testValue != null && isIntValue(value)) {
							doJump = getIntValue(value) != testValue.intValue();
							eliminateJump = true;
						}
						break;
					case Opcodes.IF_ICMPLT:
						if (testValue != null && isIntValue(value)) {
							doJump = getIntValue(value) < testValue.intValue();
							eliminateJump = true;
						}
						break;
					case Opcodes.IF_ICMPGE:
						if (testValue != null && isIntValue(value)) {
							doJump = getIntValue(value) >= testValue.intValue();
							eliminateJump = true;
						}
						break;
					case Opcodes.IF_ICMPGT:
						if (testValue != null && isIntValue(value)) {
							doJump = getIntValue(value) > testValue.intValue();
							eliminateJump = true;
						}
						break;
					case Opcodes.IF_ICMPLE:
						if (testValue != null && isIntValue(value)) {
							doJump = getIntValue(value) <= testValue.intValue();
							eliminateJump = true;
						}
						break;
				}

				if (eliminateJump) {
					if (doJump) {
						deleteUpToInsn = jumpInsn.label;
					} else {
						deleteUpToInsn = jumpInsn.getNext();
					}
				}
			}

			return eliminateJump;
		}

		private boolean analyseIfTestFloat(AbstractInsnNode insn, float testValue) {
			boolean eliminateJump = false;

			AbstractInsnNode nextInsn = insn.getNext();
			if (nextInsn != null && (nextInsn.getOpcode() == Opcodes.FCMPL || nextInsn.getOpcode() == Opcodes.FCMPG)) {
				AbstractInsnNode nextNextInsn = nextInsn.getNext();
				if (nextNextInsn != null && nextNextInsn.getType() == JUMP_INSN) {
					JumpInsnNode jumpInsn = (JumpInsnNode) nextNextInsn;
					boolean doJump = false;
					switch (jumpInsn.getOpcode()) {
						case Opcodes.IFEQ:
							if (isFloatValue(value)) {
								doJump = getFloatValue(value) == testValue;
								eliminateJump = true;
							}
							break;
						case Opcodes.IFNE:
							if (isFloatValue(value)) {
								doJump = getFloatValue(value) != testValue;
								eliminateJump = true;
							}
							break;
						case Opcodes.IFLT:
							if (isFloatValue(value)) {
								doJump = getFloatValue(value) < testValue;
								eliminateJump = true;
							}
							break;
						case Opcodes.IFGE:
							if (isFloatValue(value)) {
								doJump = getFloatValue(value) >= testValue;
								eliminateJump = true;
							}
							break;
						case Opcodes.IFGT:
							if (isFloatValue(value)) {
								doJump = getFloatValue(value) > testValue;
								eliminateJump = true;
							}
							break;
						case Opcodes.IFLE:
							if (isFloatValue(value)) {
								doJump = getFloatValue(value) <= testValue;
								eliminateJump = true;
							}
							break;
					}

					if (eliminateJump) {
						if (doJump) {
							deleteUpToInsn = jumpInsn.label;
						} else {
							deleteUpToInsn = jumpInsn.getNext();
						}
					}
				}
			}

			return eliminateJump;
		}

		private boolean isIntValue(Object value) {
			return (value instanceof Integer) || (value instanceof Boolean);
		}

		private int getIntValue(Object value) {
			if (value instanceof Integer) {
				return ((Integer) value).intValue();
			}
			if (value instanceof Boolean) {
				return value == Boolean.FALSE ? 0 : 1;
			}
			return 0;
		}

		private boolean isFloatValue(Object value) {
			return (value instanceof Float);
		}

		private float getFloatValue(Object value) {
			if (value instanceof Float) {
				return ((Float) value).floatValue();
			}
			return 0f;
		}

		private AbstractInsnNode getConstantInsn(Object value) {
			AbstractInsnNode constantInsn = null;

			if (isIntValue(value)) {
				int n = getIntValue(value);
				// Find the optimum opcode to represent this integer value
				switch (n) {
					case -1:
						constantInsn = new InsnNode(Opcodes.ICONST_M1);
						break;
					case 0:
						constantInsn = new InsnNode(Opcodes.ICONST_0);
						break;
					case 1:
						constantInsn = new InsnNode(Opcodes.ICONST_1);
						break;
					case 2:
						constantInsn = new InsnNode(Opcodes.ICONST_2);
						break;
					case 3:
						constantInsn = new InsnNode(Opcodes.ICONST_3);
						break;
					case 4:
						constantInsn = new InsnNode(Opcodes.ICONST_4);
						break;
					case 5:
						constantInsn = new InsnNode(Opcodes.ICONST_5);
						break;
					default:
						if (Byte.MIN_VALUE <= n && n < Byte.MAX_VALUE) {
							constantInsn = new IntInsnNode(Opcodes.BIPUSH, n);
						} else if (Short.MIN_VALUE <= n && n < Short.MAX_VALUE) {
							constantInsn = new IntInsnNode(Opcodes.SIPUSH, n);
						} else {
							constantInsn = new LdcInsnNode(new Integer(n));
						}
						break;
				}
			}

			return constantInsn;
		}
	}

	private static class SpecializedClassLoader extends ClassLoader {
		public Class<?> defineClass(String name, byte[] b) {
			return defineClass(name, b, 0, b.length);
		}
	}
}
