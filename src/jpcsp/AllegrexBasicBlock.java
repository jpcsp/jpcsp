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
package jpcsp;

import java.lang.reflect.Method;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;

/**
 *
 * @author hli, guillaume.serre@gmail.com
 */
public class AllegrexBasicBlock {

    protected int entry;
    protected int size;
    protected StringBuffer buffer = new StringBuffer();
    protected boolean freezed = false;
    protected static ClassPool pool = ClassPool.getDefault();
    protected Method method = null;
    protected long creationTimestamp = 0;
    protected Processor processor = null;
    protected int executionCount = 1;
    protected Integer branchTrue = null;
    protected Integer branchFalse = null;

    public final int getEntry() {
        return entry;
    }
    
    public final int getSize() {
        return entry;
    }
    
    public AllegrexBasicBlock(Processor processor, int entry) {
        this.processor = processor;
        this.entry = entry;

        creationTimestamp = System.currentTimeMillis();

        //processor.tracked_gpr = new Processor.RegisterTracking[32];
    }

    public void emit(String javaCode) {
        if (!freezed) {
            if (javaCode.endsWith(";")) {
                buffer.append(javaCode + "\n");
            } else {
                buffer.append(javaCode);
            }
        }
    }

    public void freeze() {

        //processor.reset_register_tracking();
        
        Processor.log.debug("Freezing basic block : " + Integer.toHexString(entry));

        StringBuffer javaMethod = new StringBuffer("public static void execute");
        javaMethod.append("(jpcsp.Processor processor, Integer entry) {\n");
        javaMethod.append("jpcsp.Memory memory = jpcsp.Memory.get_instance();\n");
        javaMethod.append("while(processor.pc == entry.intValue()) {\n");
        javaMethod.append(buffer).append("}; };");

        Processor.log.debug(javaMethod.toString());

        CtClass dynaCtClass = pool.makeClass("BasicBlock" + Integer.toHexString(entry));
        try {
            dynaCtClass.addMethod(
                    CtNewMethod.make(javaMethod.toString(),
                    dynaCtClass));
            Class dynaClass = dynaCtClass.toClass();
            method = dynaClass.getDeclaredMethod("execute", Processor.class, Integer.class);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        freezed = true;
    }

    public void execute() {
        try {
            method.invoke(null, processor, entry);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
