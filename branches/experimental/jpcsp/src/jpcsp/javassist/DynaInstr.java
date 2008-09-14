package jpcsp.javassist;

/**
 * @author guillaume.serre@gmail.com
 *
 */
public class DynaInstr {
	
	protected int pc;
	public StringBuffer javaCode;
	
	public DynaInstr(int pc) {
		this.pc = pc;
		javaCode = new StringBuffer();
	}

}
