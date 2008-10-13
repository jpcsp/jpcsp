/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.HLE.kernel.managers;

import java.util.HashMap;

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import static jpcsp.util.Utilities.*;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.*;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;

/**
 *
 * @author hli
 */
public class CallbackManager {

    private static HashMap<Integer, SceKernelCallbackInfo> callbackMap;

    public boolean isUidValid(int uid) {
        return callbackMap.containsKey(uid);
    }

    public void sceKernelCreateCallback(Processor processor) {
        int[] gpr = processor.cpu.gpr;
        Memory mem = Processor.memory;

        int name_addr = gpr[4];
        int callback_addr = gpr[5];
        int callback_arg_addr = gpr[6];

        String name = readStringZ(mem.mainmemory,
                (name_addr & 0x3fffffff) - MemoryMap.START_RAM);

        // initPattern and currentPattern should be the same at init
        SceKernelCallbackInfo callback = new SceKernelCallbackInfo(name, callback_addr, callback_arg_addr);

        int uid = callback.getUid();

        gpr[2] = uid;

        if (0 < uid) {
            callbackMap.put(uid, callback);
        }
    }

    public void sceKernelDeleteCallback(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];

        SceKernelCallbackInfo callback = callbackMap.get(uid);

        if (callback != null) {
            Modules.log.debug("sceKernelDeleteCallback id=" + uid);
            callback.release();
            gpr[2] = 0;
        } else {
            Modules.log.warn("sceKernelDeleteCallback - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_CALLBACK;
        }
    }

    public void sceKernelNotifyCallback(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];

        SceKernelCallbackInfo callback = callbackMap.get(uid);
        if (callback != null) {
            //this.thread.callbackNotify = true;
            callback.notifyArg = gpr[5];
            callback.notifyCount++;

            Modules.log.debug(String.format("sceKernelNotifyCallback PARTIALLY implemented id=%s arg=0x%08x", gpr[4], callback.notifyArg));

            //if (this.thread.isCallback)
            //{
            //  if (this.thread.status == THREAD_WAITING || this.thread.status == (THREAD_WAITING | THREAD_SUSPEND))
            //  {
            //    int s0 = sub_0000022C(this.thread);
            //
            //    this.thread.callbackStatus = KERNEL_ERROR_NOTIFY_CALLBACK;
            //    if (this.thread.waitType != 0)
            //    {
            //      s0 += sub_000005F4(thread.thread.waitType);
            //    }
            //    if (s0 != 0)
            //    {
            //      gInfo.nextThread = 0;
            //      _ReleaseWaitThread(0);
            //    }
            //  }
            //}
            //return KERNEL_ERROR_OK;

            processor.cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceKernelNotifyCallback - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_CALLBACK;
        }

    }

    public void sceKernelCancelCallback(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];

        SceKernelCallbackInfo callback = callbackMap.get(uid);

        if (callback != null) {
            Modules.log.debug("sceKernelCancelCallback id=" + uid);
            callback.notifyArg = 0;
            callback.callback_arg_addr = 0;
            gpr[2] = 0;
        } else {
            Modules.log.warn("sceKernelCancelCallback invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_CALLBACK;
        }
    }

    public void sceKernelGetCallbackCount(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        int uid = gpr[4];

        SceKernelCallbackInfo callback = callbackMap.get(uid);

        if (callback != null) {
            Modules.log.debug("sceKernelGetCallbackCount id=" + uid);
            gpr[2] = callback.notifyCount;
        } else {
            Modules.log.warn("sceKernelGetCallbackCount - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_CALLBACK;
        }
    }

    public void sceKernelCheckCallback(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        Modules.log.debug("sceKernelCheckCallback PARTIALLY implemented");

        int ret = 0;

        //if (Managers.threads.currentThread.callbackNotify != 0)
        //{
        //    ret = (dispatchCallbacks() > 0) ? 1 : 0;
        //}

        gpr[2] = ret;
    }

    public void sceKernelReferCallbackStatus(Processor processor) {
        int[] gpr = processor.cpu.gpr;
        Memory mem = Processor.memory;

        int uid = gpr[4];

        SceKernelCallbackInfo callback = callbackMap.get(uid);

        if (callback != null) {
            int addr = gpr[5];

            Modules.log.debug("sceKernelReferCallbackStatus id=" + uid);
            
            int i, len = Math.min(callback.name.length(), 31);

            mem.write32(addr, 1 * 4 + 32 * 1 + 5 * 4); //struct size
            for (addr += 4    , i = 0; i < len; i++) {
                mem.write8(addr++, (byte) callback.name.charAt(i));
            }
            for (; i < 32; i++) {
                mem.write8(addr++, (byte) 0);
            }
            mem.write32(addr + 0 + 0 + 0 + 0, callback.thread.uid);
            mem.write32(addr + 4 + 0 + 0 + 0, callback.callback_addr);
            mem.write32(addr + 4 + 4 + 0 + 0, callback.callback_arg_addr);
            mem.write32(addr + 4 + 4 + 4 + 0, callback.notifyCount);
            mem.write32(addr + 4 + 4 + 4 + 4, callback.notifyArg);

            gpr[2] = 0;
        } else {
            Modules.log.warn("sceKernelReferCallbackStatus - invalid id=" + uid);
            gpr[2] = ERROR_NOT_FOUND_CALLBACK;
        }
    }

    public boolean releaseObject(SceKernelUid object) {
        if (Managers.uids.removeObject(object)) {
            callbackMap.remove(object.getUid());
            return true;
        }
        return false;
    }

    public void reset() {
        callbackMap = new HashMap<Integer, SceKernelCallbackInfo>();
    }
    public static final CallbackManager singleton;

    private CallbackManager() {
    }
    

    static {
        singleton = new CallbackManager();
        singleton.reset();
    }
}
