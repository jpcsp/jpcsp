package jpcsp.javassist;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;

/**
 * @author guillaume.serre@gmail.com
 *
 */
public class DynaCode {
	
	protected int entry;
	protected StringBuffer javaBuffer = new StringBuffer();
	protected boolean freezed = false;
	
	protected int pc, npc;
	
	protected static ClassPool pool = ClassPool.getDefault();
	
	protected Method dynaMethod = null;
	
	protected long creationTimeStamp = 0;

    private Integer fixed_gpr_values[] = new Integer[32];	
    private String fixed_gpr_vars[] = new String[32];	
    private Float fixed_fpr_values[] = new Float[32];	
    private String fixed_fpr_vars[] = new String[32];	
    
    protected Set<String> locals = new HashSet<String>();
    
    public void addLocal(String local) {
    	if (!local.endsWith(";")) local += ";";
    	locals.add( local );
    }
    
    public int getEntry() {
		return entry;
	}
    
    protected List<DynaInstr> instructions = new ArrayList<DynaInstr>();
	
	public DynaCode(int entry) {
		this.entry = entry;		
		this.creationTimeStamp = System.currentTimeMillis();
		fixed_gpr_values[0] = 0;
	}

	public void addJavaInstruction(String javaCode) {
		if (! freezed ) {
			if (javaCode.endsWith(";") || javaCode.endsWith("}")) {
				javaBuffer.append( javaCode + "\n");				
			} else {
				javaBuffer.append( javaCode);
			}
		}
	}
	    
    public void fixGPRValue(int register, int value) {
    	if (register == 0) return;
    	fixed_gpr_values[register] = value;
    	fixed_gpr_vars[register] = null;
    }
    
    public void fixGPRVar(int register, String var) {   	
    	for (int i = 0; i < fixed_gpr_vars.length; i++) {	// we make sure that no other register is already using that var
			String varI = fixed_gpr_vars[i];
			if (varI != null && varI.equals(var) && i != register) {
				fixed_gpr_vars[i] = null;
			}
		}
    	if (register == 0) return;    	
    	fixed_gpr_vars[register] = var;
    	fixed_gpr_values[register] = null;
    }
    
    public void fixFPRValue(int register, float value) {
    	fixed_fpr_values[register] = value;
    	fixed_fpr_vars[register] = null;
    }
    
    public void fixFPRVar(int register, String var) {   	
    	for (int i = 0; i < fixed_fpr_vars.length; i++) {	// we make sure that no other register is already using that var
			String varI = fixed_fpr_vars[i];
			if (varI != null && varI.equals(var) && i != register) {
				fixed_fpr_vars[i] = null;
			}
		}
    	if (register == 0) return;    	
    	fixed_fpr_vars[register] = var;
    	fixed_fpr_values[register] = null;
    }

    public void unfixAll() {
    	for (int i=1; i<32; i++) {
    		unfixGPR(i);
    		unfixFPR(i);
    	}
    }
    
    public String getGPRCodeRepr(int register) {
    	Integer value = fixed_gpr_values[register];
    	if (value == null) {
    		String var = fixed_gpr_vars[register];
    		if (var != null) {
    			return var;
    		}
    		return "gpr[" + register + "]";
    	} else {
    		return "0x" + Integer.toHexString(value);
    	}
    }  
    
    public String getFPRCodeRepr(int register) {
    	Float value = fixed_fpr_values[register];
    	if (value == null) {
    		String var = fixed_fpr_vars[register];
    		if (var != null) {
    			return var;
    		}
    		return "fpr[" + register + "]";
    	} else {
    		return Float.toString( value );
    	}
    }    
    
    public void unfixGPR(int register) {
    	if (register == 0) return;
    	fixed_gpr_values[register] = null;
    	fixed_gpr_vars[register] = null;
    }

    public void unfixFPR(int register) {
    	fixed_fpr_values[register] = null;
    	fixed_fpr_vars[register] = null;
    }

    public Integer getFixedValue(int register) {
    	return fixed_gpr_values[register];
    }
    
    protected StringBuffer javaMethod = null;
	
	public void freeze() {
		
		//System.out.println("Freezing DynaCode block : " + Integer.toHexString( entry ));
		
		javaMethod = new StringBuffer(
				"public static void execute(java.lang.Integer entry) {jpcsp.Processor processor = jpcsp.HLE.PSPThread.currentPSPThread().processor; jpcsp.Memory memory = jpcsp.Memory.get_instance(); int[] gpr = processor.gpr; float[] fpr = processor.fpr; int word; "				
		).append("long x, y; int lo, hi;\n");
		
		// Add local variables
		for (Iterator<String> iterator = locals.iterator(); iterator.hasNext();) {
			String local = iterator.next();
			javaMethod.append(local).append(";\n");
		}
		
		javaMethod.append("while(processor.npc == 0x" + Integer.toHexString(entry) + ") {\n");
		for (Iterator<DynaInstr> iterator = instructions.iterator(); iterator.hasNext();) {
			DynaInstr instruction = iterator.next();
			javaMethod.append( "// PC=0x" + Integer.toHexString(instruction.pc) + "\n" );
			javaMethod.append( instruction.javaCode );
		}
		javaMethod.append("};");
		javaMethod.append("processor.pc = processor.npc; };" );

		// System.out.println(javaMethod.toString());
		
		String className = "jpcsp.javassist.DynaCode" + Integer.toHexString(entry);
		
			CtClass dynaCtClass;
			try {
				dynaCtClass = pool.makeClass(className);
			} catch(RuntimeException e) {
				Class dynaClass;
				try {
					dynaClass = Class.forName( className );
					dynaMethod = dynaClass.getDeclaredMethod("execute", Integer.class );
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				freezed = true;
				return;
			}
				try {
					dynaCtClass.addMethod(
					        CtNewMethod.make(javaMethod.toString(),
					            dynaCtClass));
					Class dynaClass = dynaCtClass.toClass();
					dynaMethod = dynaClass.getDeclaredMethod("execute", Integer.class );
				}catch (Exception e) {
					e.printStackTrace();			
				}			

		freezed = true;
	}
	
	public void execute() {
		try {
			dynaMethod.invoke(null, entry);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void gprAssignExpression(int register, String expression) {
		if (register == 0) return;
		javaBuffer.append("gpr[" + register + "] = (" + expression + ");\n");
		unfixGPR(register);
	}

	public void gprAssignLocal(int register, String var) {
		if (register == 0) return;
		javaBuffer.append("gpr[" + register + "] = " + var + ";\n");
		fixGPRVar(register, var);
	}

	public void gprAssignValue(int register, int value) {
		if (register == 0) return;
		javaBuffer.append("gpr[" + register + "] = 0x" + Integer.toHexString(value) + ";\n");
		fixGPRValue(register, value);
	}

	public void fprAssignExpression(int register, String expression) {
		javaBuffer.append("fpr[" + register + "] = (" + expression + ");\n");
		unfixFPR(register);
	}

	public void fprAssignLocal(int register, String var) {
		javaBuffer.append("fpr[" + register + "] = " + var + ";\n");
		fixFPRVar(register, var);
	}

	public void fprAssignValue(int register, float value) {
		javaBuffer.append("fpr[" + register + "] = " + value + ";\n");
		fixFPRValue(register, value);
	}

	public void setPc(int pc) {
		DynaInstr currentInstr = new DynaInstr(pc);
		instructions.add(currentInstr);
		javaBuffer = currentInstr.javaCode;
		this.pc = pc;
	}

	public void setNpc(int npc) {
		this.npc = npc;
	}	

}
