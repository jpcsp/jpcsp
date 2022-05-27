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
package jpcsp.HLE.kernel.types;

import jpcsp.HLE.ITPointerBase;
import jpcsp.HLE.Modules;

public class SceKernelCallbackInfo extends pspBaseCallback {
    private final String name;
    private final int threadId;
    private final int callbackArgument;
    private int notifyCount;
    private int notifyArg;

    private class SceKernelCallbackStatus extends pspAbstractMemoryMappedStructureVariableLength {
    	@Override
    	protected void write() {
    		super.write();
    		writeStringNZ(32, name);
    		write32(threadId);
    		write32(getCallbackFunction());
    		write32(callbackArgument);
    		write32(notifyCount);
    		write32(notifyArg);
    	}
    }

    public SceKernelCallbackInfo(String name, int threadId, int callback_addr, int callback_arg_addr) {
    	super(callback_addr, 3);
        this.name = name;
        this.threadId = threadId;
        this.callbackArgument = callback_arg_addr;
        notifyCount = 0;
        notifyArg = 0;
    }

    public void write(ITPointerBase statusAddr) {
    	SceKernelCallbackStatus status = new SceKernelCallbackStatus();
    	status.write(statusAddr);
    }

    /** Call this to switch in the callback, in a given thread context.
     */
    @Override
    public void call(SceKernelThreadInfo thread, IAction afterAction) {
    	setArgument(0, notifyCount);
    	setArgument(1, notifyArg);
    	setArgument(2, callbackArgument);

        // clear the counter and the arg
        notifyCount = 0;
        notifyArg = 0;

        super.call(thread, afterAction);
    }

    public int getThreadId() {
    	return threadId;
    }

    public int getNotifyCount() {
    	return notifyCount;
    }

    public int getNotifyArg() {
    	return notifyArg;
    }

    public void cancel() {
    	notifyArg = 0;
    	notifyCount = 0;
    }

    public int getCallbackArgument() {
    	return callbackArgument;
    }

    public void setNotifyArg(int notifyArg) {
    	notifyCount++; // keep increasing this until we actually enter the callback
    	this.notifyArg = notifyArg;
    }

    @Override
	public String toString() {
		return String.format("uid:0x%X, name:'%s', thread:'%s', PC:0x%08X, $a0:0x%08X, $a1:0x%08X, $a2:0x%08X", getUid(), name, Modules.ThreadManForUserModule.getThreadName(threadId), getCallbackFunction(), notifyCount, notifyArg, callbackArgument);
	}
}
