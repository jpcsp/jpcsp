/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.format;

public class FormatManager {
    private static PSPModuleInfo moduleinfo;
    
    public static PSPModuleInfo getInstancePspModuloInfo(){
        if (moduleinfo==null){
            moduleinfo = new PSPModuleInfo();
        }
        return moduleinfo;
    }
}
