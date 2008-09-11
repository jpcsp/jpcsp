/*
Function:
- http://psp.jim.sh/pspsdk-doc/group__SysMem.html


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
package jpcsp.HLE;

/**
 *
 * @author shadow
 */
public class pspSysMem {
    private static pspSysMem instance;

    public static pspSysMem get_instance() {
        if (instance == null) {
            instance = new pspSysMem();
        }
        return instance;
    }
    public void sceKernelMaxFreeMemSize()
    {
        System.out.println("Unimplement:sceKernelMaxFreeMemSize");
    }
    public void sceKernelTotalFreeMemSize()
    {
        System.out.println("Unimplement:sceKernelTotalFreeMemSize");
    }
    public void sceKernelAllocPartitionMemory(int a0, int a1, int a2, int a3)
    {
        System.out.println("Unimplement:sceKernelAllocPartitionMemory");
    }
    public void sceKernelFreePartitionMemory(int a0)
    {
        System.out.println("Unimplement:sceKernelFreePartitionMemory");
    }
    public void sceKernelGetBlockHeadAddr(int a0)
    {
        System.out.println("Unimplement:sceKernelGetBlockHeadAddr");
    }

}
