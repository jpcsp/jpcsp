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
package jpcsp.HLE;

import jpcsp.Debugger.DisassemblerModule.*;
import jpcsp.Emulator;
import jpcsp.GeneralJpcspException;

public class SyscallHandler {

    // Change this to return the number of cycles used?
    public static void syscall(int code) {
        int gpr[] = Emulator.getProcessor().gpr;

        // Some syscalls implementation throw GeneralJpcspException,
        // and Processor isn't setup to catch exceptions so we'll do it
        // here for now, or we could just stop throwing exceptions.
        // Also we need to decide whether to pass arguments to the functions,
        // or let them read the registers they want themselves.
        try {
            // Currently using FW1.50 codes
            switch(code) {
                case 0x200d:
                    ThreadMan.get_instance().ThreadMan_sceKernelCreateCallback(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x2015:
                    ThreadMan.get_instance().ThreadMan_sceKernelSleepThreadCB();
                    break;
                case 0x201c:
                    ThreadMan.get_instance().ThreadMan_sceKernelDelayThread(gpr[4]);
                    break;
                case 0x206d:
                    ThreadMan.get_instance().ThreadMan_sceKernelCreateThread(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8], gpr[9]);
                    break;
                case 0x206e:
                    ThreadMan.get_instance().ThreadMan_sceKernelDeleteThread(gpr[4]);
                    break;
                case 0x206f:
                    ThreadMan.get_instance().ThreadMan_sceKernelStartThread(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x2070:
                case 0x2071:
                    ThreadMan.get_instance().ThreadMan_sceKernelExitThread(gpr[4]);
                    break;
                case 0x2072:
                    ThreadMan.get_instance().ThreadMan_sceKernelExitDeleteThread(gpr[4]);
                    break;
                case 0x2073:
                    ThreadMan.get_instance().ThreadMan_sceKernelTerminateThread(gpr[4]);
                    break;

                case 0x20b2:
                    pspstdio.get_instance().sceKernelStdin();
                    break;
                case 0x20b3:
                    pspstdio.get_instance().sceKernelStdout();
                    break;
                case 0x20b4:
                    pspstdio.get_instance().sceKernelStderr();
                    break;

                case 0x20bf:
                    Utils.get_instance().Utils_sceKernelUtilsMt19937Init(gpr[4], gpr[5]);
                    break;
                case 0x20c0:
                    Utils.get_instance().Utils_sceKernelUtilsMt19937UInt(gpr[4]);
                    break;

                case 0x20eb:
                    LoadExec.get_instance().sceKernelExitGame();
                    break;
                case 0x20ec:
                    LoadExec.get_instance().sceKernelRegisterExitCallback(gpr[4]);
                    break;

                case 0x213a:
                    pspdisplay.get_instance().sceDisplaySetMode(gpr[4], gpr[5], gpr[6]);
                    break;
                case 0x213f:
                    pspdisplay.get_instance().sceDisplaySetFrameBuf(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
                case 0x2147:
                    pspdisplay.get_instance().sceDisplayWaitVblankStart();
                    break;

                case 0x214c:
                    pspctrl.get_instance().sceCtrlSetSamplingCycle(gpr[4]);
                    break;
                case 0x214d:
                    pspctrl.get_instance().sceCtrlGetSamplingCycle(gpr[4]);
                    break;
                case 0x214e:
                    pspctrl.get_instance().sceCtrlSetSamplingMode(gpr[4]);
                    break;
                case 0x214f:
                    pspctrl.get_instance().sceCtrlGetSamplingMode(gpr[4]);
                    break;
                case 0x2150:
                    pspctrl.get_instance().sceCtrlPeekBufferPositive(gpr[4], gpr[5]);
                    break;
                case 0x2151:
                    pspctrl.get_instance().sceCtrlPeekBufferNegative(gpr[4], gpr[5]);
                    break;
                case 0x2152:
                    pspctrl.get_instance().sceCtrlReadBufferPositive(gpr[4], gpr[5]);
                    break;
                case 0x2153:
                    pspctrl.get_instance().sceCtrlReadBufferNegative(gpr[4], gpr[5]);
                    break;
                case 0x2155:
                    pspctrl.get_instance().sceCtrlPeekLatch(gpr[4]);
                    break;

                case 0x20f0:
                    pspge.get_instance().sceGeEdramGetAddr();
                    break;
                case 0x20f6:
                    pspge.get_instance().sceGeListEnQueue(gpr[4], gpr[5], gpr[6], gpr[7]);
                    break;
                case 0x20f8:
                    pspge.get_instance().sceGeListDeQueue(gpr[4]);
                    break;
                case 0x20f9:
                    pspge.get_instance().sceGeListUpdateStallAddr(gpr[4], gpr[5]);
                    break;
                /*
                case 0x20fe:
                    pspge.get_instance().sceGeSetCallback(gpr[4]);
                    break;
                case 0x20ff:
                    pspge.get_instance().sceGeUnsetCallback(gpr[4]);
                    break;
                */

                default:
                {
                  for (syscalls.calls c : syscalls.calls.values()) {
                  if (c.getValue() == code) {
                      System.out.println("Unsupported syscall " + Integer.toHexString(code) + " " + c);
                      return;
                     }
                  }
                  System.out.println("Unsupported syscall " + Integer.toHexString(code));
                }
                break;
            }
        } catch(GeneralJpcspException e) {
            System.out.println(e.getMessage());
        }
    }
}
