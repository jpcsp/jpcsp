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
package jpcsp.HLE.modules150;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_ADDR;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_ARGUMENT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_PRIORITY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_THREAD;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NOT_FOUND_ALARM;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NOT_FOUND_THREAD;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NOT_FOUND_THREAD_EVENT_HANDLER;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NOT_FOUND_VTIMER;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_THREAD_ALREADY_DORMANT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_THREAD_IS_NOT_DORMANT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_THREAD_IS_NOT_SUSPEND;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_THREAD_IS_TERMINATED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_STATUS_RELEASED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_ATTR_KERNEL;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_ATTR_USER;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_RUNNING;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_STOPPED;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_SUSPEND;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_WAITING;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_WAITING_SUSPEND;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_DELAY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_NONE;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_SLEEP;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_THREAD_END;
import static jpcsp.util.Utilities.readStringNZ;
import static jpcsp.util.Utilities.writeStringZ;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jpcsp.AllegrexOpcodes;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Debugger.DumpDebugState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.managers.SystemTimeManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelAlarmInfo;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelSystemStatus;
import jpcsp.HLE.kernel.types.SceKernelThreadEventHandlerInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceKernelVTimerInfo;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.scheduler.Scheduler;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

/*
 * Thread scheduling on PSP:
 * - when a thread having a higher priority than the current thread, switches to the
 *   READY state, the current thread is interrupted immediately and is loosing the
 *   RUNNING state. The new thread then moves to the RUNNING state.
 * - when a thread having the same or a lower priority than the current thread,
 *   switches to the READY state, the current thread is not interrupted and is keeping
 *   the RUNNING state.
 * - a RUNNING thread can only yield to a thread having the same priority by calling
 *   sceKernelRotateThreadReadyQueue(0).
 * - the clock precision when interrupting a RUNNING thread is about 200 microseconds.
 *   i.e., it can take up to 200us when a high priority thread moves to the READY
 *   state before it changes to the RUNNING state.
 *
 * Thread scheduling on Jpcsp:
 * - the rules for moving between states are implemented in hleChangeThreadState()
 * - the rules for choosing the thread in the RUNNING state are implemented in
 *   hleRescheduleCurrentThread()
 * - the clock precision for interrupting a RUNNING thread is about 1000 microseconds.
 *   This is due to a restriction of the Java timers used by the Thread.sleep() methods.
 *   Even the Thread.sleep(millis, nanos) seems to have the same restriction
 *   (at least on windows).
 * - preemptive scheduling is implemented in RuntimeContext by a separate
 *   Java thread (RuntimeSyncThread). This thread sets the flag RuntimeContext.wantSync
 *   when a scheduler action has to be executed. This flag is checked by the compiled
 *   code at each back branch (i.e. a branch to a lower address, usually a loop).
 *
 * Test application:
 * - taskScheduler.prx: testing the scheduler rules between threads having higher,
 *                      lower or the same priority.
 *                      The clock precision of 200us on the PSP can be observed here.
 */
public class ThreadManForUser implements HLEModule, HLEStartModule {

    protected static Logger log = Modules.getLogger("ThreadManForUser");

    @Override
    public String getName() {
        return "ThreadManForUser";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x6E9EA350, _sceKernelReturnFromCallbackFunction);
            mm.addFunction(0x0C106E53, sceKernelRegisterThreadEventHandlerFunction);
            mm.addFunction(0x72F3C145, sceKernelReleaseThreadEventHandlerFunction);
            mm.addFunction(0x369EEB6B, sceKernelReferThreadEventHandlerStatusFunction);
            mm.addFunction(0xE81CAF8F, sceKernelCreateCallbackFunction);
            mm.addFunction(0xEDBA5844, sceKernelDeleteCallbackFunction);
            mm.addFunction(0xC11BA8C4, sceKernelNotifyCallbackFunction);
            mm.addFunction(0xBA4051D6, sceKernelCancelCallbackFunction);
            mm.addFunction(0x2A3D44FF, sceKernelGetCallbackCountFunction);
            mm.addFunction(0x349D6D6C, sceKernelCheckCallbackFunction);
            mm.addFunction(0x730ED8BC, sceKernelReferCallbackStatusFunction);
            mm.addFunction(0x9ACE131E, sceKernelSleepThreadFunction);
            mm.addFunction(0x82826F70, sceKernelSleepThreadCBFunction);
            mm.addFunction(0xD59EAD2F, sceKernelWakeupThreadFunction);
            mm.addFunction(0xFCCFAD26, sceKernelCancelWakeupThreadFunction);
            mm.addFunction(0x9944F31F, sceKernelSuspendThreadFunction);
            mm.addFunction(0x75156E8F, sceKernelResumeThreadFunction);
            mm.addFunction(0x278C0DF5, sceKernelWaitThreadEndFunction);
            mm.addFunction(0x840E8133, sceKernelWaitThreadEndCBFunction);
            mm.addFunction(0xCEADEB47, sceKernelDelayThreadFunction);
            mm.addFunction(0x68DA9E36, sceKernelDelayThreadCBFunction);
            mm.addFunction(0xBD123D9E, sceKernelDelaySysClockThreadFunction);
            mm.addFunction(0x1181E963, sceKernelDelaySysClockThreadCBFunction);
            mm.addFunction(0xD6DA4BA1, sceKernelCreateSemaFunction);
            mm.addFunction(0x28B6489C, sceKernelDeleteSemaFunction);
            mm.addFunction(0x3F53E640, sceKernelSignalSemaFunction);
            mm.addFunction(0x4E3A1105, sceKernelWaitSemaFunction);
            mm.addFunction(0x6D212BAC, sceKernelWaitSemaCBFunction);
            mm.addFunction(0x58B1F937, sceKernelPollSemaFunction);
            mm.addFunction(0x8FFDF9A2, sceKernelCancelSemaFunction);
            mm.addFunction(0xBC6FEBC5, sceKernelReferSemaStatusFunction);
            mm.addFunction(0x55C20A00, sceKernelCreateEventFlagFunction);
            mm.addFunction(0xEF9E4C70, sceKernelDeleteEventFlagFunction);
            mm.addFunction(0x1FB15A32, sceKernelSetEventFlagFunction);
            mm.addFunction(0x812346E4, sceKernelClearEventFlagFunction);
            mm.addFunction(0x402FCF22, sceKernelWaitEventFlagFunction);
            mm.addFunction(0x328C546A, sceKernelWaitEventFlagCBFunction);
            mm.addFunction(0x30FD48F0, sceKernelPollEventFlagFunction);
            mm.addFunction(0xCD203292, sceKernelCancelEventFlagFunction);
            mm.addFunction(0xA66B0120, sceKernelReferEventFlagStatusFunction);
            mm.addFunction(0x8125221D, sceKernelCreateMbxFunction);
            mm.addFunction(0x86255ADA, sceKernelDeleteMbxFunction);
            mm.addFunction(0xE9B3061E, sceKernelSendMbxFunction);
            mm.addFunction(0x18260574, sceKernelReceiveMbxFunction);
            mm.addFunction(0xF3986382, sceKernelReceiveMbxCBFunction);
            mm.addFunction(0x0D81716A, sceKernelPollMbxFunction);
            mm.addFunction(0x87D4DD36, sceKernelCancelReceiveMbxFunction);
            mm.addFunction(0xA8E8C846, sceKernelReferMbxStatusFunction);
            mm.addFunction(0x7C0DC2A0, sceKernelCreateMsgPipeFunction);
            mm.addFunction(0xF0B7DA1C, sceKernelDeleteMsgPipeFunction);
            mm.addFunction(0x876DBFAD, sceKernelSendMsgPipeFunction);
            mm.addFunction(0x7C41F2C2, sceKernelSendMsgPipeCBFunction);
            mm.addFunction(0x884C9F90, sceKernelTrySendMsgPipeFunction);
            mm.addFunction(0x74829B76, sceKernelReceiveMsgPipeFunction);
            mm.addFunction(0xFBFA697D, sceKernelReceiveMsgPipeCBFunction);
            mm.addFunction(0xDF52098F, sceKernelTryReceiveMsgPipeFunction);
            mm.addFunction(0x349B864D, sceKernelCancelMsgPipeFunction);
            mm.addFunction(0x33BE4024, sceKernelReferMsgPipeStatusFunction);
            mm.addFunction(0x56C039B5, sceKernelCreateVplFunction);
            mm.addFunction(0x89B3D48C, sceKernelDeleteVplFunction);
            mm.addFunction(0xBED27435, sceKernelAllocateVplFunction);
            mm.addFunction(0xEC0A693F, sceKernelAllocateVplCBFunction);
            mm.addFunction(0xAF36D708, sceKernelTryAllocateVplFunction);
            mm.addFunction(0xB736E9FF, sceKernelFreeVplFunction);
            mm.addFunction(0x1D371B8A, sceKernelCancelVplFunction);
            mm.addFunction(0x39810265, sceKernelReferVplStatusFunction);
            mm.addFunction(0xC07BB470, sceKernelCreateFplFunction);
            mm.addFunction(0xED1410E0, sceKernelDeleteFplFunction);
            mm.addFunction(0xD979E9BF, sceKernelAllocateFplFunction);
            mm.addFunction(0xE7282CB6, sceKernelAllocateFplCBFunction);
            mm.addFunction(0x623AE665, sceKernelTryAllocateFplFunction);
            mm.addFunction(0xF6414A71, sceKernelFreeFplFunction);
            mm.addFunction(0xA8AA591F, sceKernelCancelFplFunction);
            mm.addFunction(0xD8199E4C, sceKernelReferFplStatusFunction);
            mm.addFunction(0x0E927AED, _sceKernelReturnFromTimerHandlerFunction);
            mm.addFunction(0x110DEC9A, sceKernelUSec2SysClockFunction);
            mm.addFunction(0xC8CD158C, sceKernelUSec2SysClockWideFunction);
            mm.addFunction(0xBA6B92E2, sceKernelSysClock2USecFunction);
            mm.addFunction(0xE1619D7C, sceKernelSysClock2USecWideFunction);
            mm.addFunction(0xDB738F35, sceKernelGetSystemTimeFunction);
            mm.addFunction(0x82BC5777, sceKernelGetSystemTimeWideFunction);
            mm.addFunction(0x369ED59D, sceKernelGetSystemTimeLowFunction);
            mm.addFunction(0x6652B8CA, sceKernelSetAlarmFunction);
            mm.addFunction(0xB2C25152, sceKernelSetSysClockAlarmFunction);
            mm.addFunction(0x7E65B999, sceKernelCancelAlarmFunction);
            mm.addFunction(0xDAA3F564, sceKernelReferAlarmStatusFunction);
            mm.addFunction(0x20FFF560, sceKernelCreateVTimerFunction);
            mm.addFunction(0x328F9E52, sceKernelDeleteVTimerFunction);
            mm.addFunction(0xB3A59970, sceKernelGetVTimerBaseFunction);
            mm.addFunction(0xB7C18B77, sceKernelGetVTimerBaseWideFunction);
            mm.addFunction(0x034A921F, sceKernelGetVTimerTimeFunction);
            mm.addFunction(0xC0B3FFD2, sceKernelGetVTimerTimeWideFunction);
            mm.addFunction(0x542AD630, sceKernelSetVTimerTimeFunction);
            mm.addFunction(0xFB6425C3, sceKernelSetVTimerTimeWideFunction);
            mm.addFunction(0xC68D9437, sceKernelStartVTimerFunction);
            mm.addFunction(0xD0AEEE87, sceKernelStopVTimerFunction);
            mm.addFunction(0xD8B299AE, sceKernelSetVTimerHandlerFunction);
            mm.addFunction(0x53B00E9A, sceKernelSetVTimerHandlerWideFunction);
            mm.addFunction(0xD2D615EF, sceKernelCancelVTimerHandlerFunction);
            mm.addFunction(0x5F32BEAA, sceKernelReferVTimerStatusFunction);
            mm.addFunction(0x446D8DE6, sceKernelCreateThreadFunction);
            mm.addFunction(0x9FA03CD3, sceKernelDeleteThreadFunction);
            mm.addFunction(0xF475845D, sceKernelStartThreadFunction);
            mm.addFunction(0x532A522E, _sceKernelExitThreadFunction);
            mm.addFunction(0xAA73C935, sceKernelExitThreadFunction);
            mm.addFunction(0x809CE29B, sceKernelExitDeleteThreadFunction);
            mm.addFunction(0x616403BA, sceKernelTerminateThreadFunction);
            mm.addFunction(0x383F7BCC, sceKernelTerminateDeleteThreadFunction);
            mm.addFunction(0x3AD58B8C, sceKernelSuspendDispatchThreadFunction);
            mm.addFunction(0x27E22EC2, sceKernelResumeDispatchThreadFunction);
            mm.addFunction(0xEA748E31, sceKernelChangeCurrentThreadAttrFunction);
            mm.addFunction(0x71BC9871, sceKernelChangeThreadPriorityFunction);
            mm.addFunction(0x912354A7, sceKernelRotateThreadReadyQueueFunction);
            mm.addFunction(0x2C34E053, sceKernelReleaseWaitThreadFunction);
            mm.addFunction(0x293B45B8, sceKernelGetThreadIdFunction);
            mm.addFunction(0x94AA61EE, sceKernelGetThreadCurrentPriorityFunction);
            mm.addFunction(0x3B183E26, sceKernelGetThreadExitStatusFunction);
            mm.addFunction(0xD13BDE95, sceKernelCheckThreadStackFunction);
            mm.addFunction(0x52089CA1, sceKernelGetThreadStackFreeSizeFunction);
            mm.addFunction(0x17C1684E, sceKernelReferThreadStatusFunction);
            mm.addFunction(0xFFC36A14, sceKernelReferThreadRunStatusFunction);
            mm.addFunction(0x627E6F3A, sceKernelReferSystemStatusFunction);
            mm.addFunction(0x94416130, sceKernelGetThreadmanIdListFunction);
            mm.addFunction(0x57CF62DD, sceKernelGetThreadmanIdTypeFunction);
            mm.addFunction(0x64D4540E, sceKernelReferThreadProfilerFunction);
            mm.addFunction(0x8218B4DD, sceKernelReferGlobalProfilerFunction);

            mm.addHLEFunction(hleKernelAsyncLoopFunction);
            mm.addHLEFunction(hleKernelExitCallbackFunction);
            mm.addHLEFunction(hleKernelExitThreadFunction);
        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(_sceKernelReturnFromCallbackFunction);
            mm.removeFunction(sceKernelRegisterThreadEventHandlerFunction);
            mm.removeFunction(sceKernelReleaseThreadEventHandlerFunction);
            mm.removeFunction(sceKernelReferThreadEventHandlerStatusFunction);
            mm.removeFunction(sceKernelCreateCallbackFunction);
            mm.removeFunction(sceKernelDeleteCallbackFunction);
            mm.removeFunction(sceKernelNotifyCallbackFunction);
            mm.removeFunction(sceKernelCancelCallbackFunction);
            mm.removeFunction(sceKernelGetCallbackCountFunction);
            mm.removeFunction(sceKernelCheckCallbackFunction);
            mm.removeFunction(sceKernelReferCallbackStatusFunction);
            mm.removeFunction(sceKernelSleepThreadFunction);
            mm.removeFunction(sceKernelSleepThreadCBFunction);
            mm.removeFunction(sceKernelWakeupThreadFunction);
            mm.removeFunction(sceKernelCancelWakeupThreadFunction);
            mm.removeFunction(sceKernelSuspendThreadFunction);
            mm.removeFunction(sceKernelResumeThreadFunction);
            mm.removeFunction(sceKernelWaitThreadEndFunction);
            mm.removeFunction(sceKernelWaitThreadEndCBFunction);
            mm.removeFunction(sceKernelDelayThreadFunction);
            mm.removeFunction(sceKernelDelayThreadCBFunction);
            mm.removeFunction(sceKernelDelaySysClockThreadFunction);
            mm.removeFunction(sceKernelDelaySysClockThreadCBFunction);
            mm.removeFunction(sceKernelCreateSemaFunction);
            mm.removeFunction(sceKernelDeleteSemaFunction);
            mm.removeFunction(sceKernelSignalSemaFunction);
            mm.removeFunction(sceKernelWaitSemaFunction);
            mm.removeFunction(sceKernelWaitSemaCBFunction);
            mm.removeFunction(sceKernelPollSemaFunction);
            mm.removeFunction(sceKernelCancelSemaFunction);
            mm.removeFunction(sceKernelReferSemaStatusFunction);
            mm.removeFunction(sceKernelCreateEventFlagFunction);
            mm.removeFunction(sceKernelDeleteEventFlagFunction);
            mm.removeFunction(sceKernelSetEventFlagFunction);
            mm.removeFunction(sceKernelClearEventFlagFunction);
            mm.removeFunction(sceKernelWaitEventFlagFunction);
            mm.removeFunction(sceKernelWaitEventFlagCBFunction);
            mm.removeFunction(sceKernelPollEventFlagFunction);
            mm.removeFunction(sceKernelCancelEventFlagFunction);
            mm.removeFunction(sceKernelReferEventFlagStatusFunction);
            mm.removeFunction(sceKernelCreateMbxFunction);
            mm.removeFunction(sceKernelDeleteMbxFunction);
            mm.removeFunction(sceKernelSendMbxFunction);
            mm.removeFunction(sceKernelReceiveMbxFunction);
            mm.removeFunction(sceKernelReceiveMbxCBFunction);
            mm.removeFunction(sceKernelPollMbxFunction);
            mm.removeFunction(sceKernelCancelReceiveMbxFunction);
            mm.removeFunction(sceKernelReferMbxStatusFunction);
            mm.removeFunction(sceKernelCreateMsgPipeFunction);
            mm.removeFunction(sceKernelDeleteMsgPipeFunction);
            mm.removeFunction(sceKernelSendMsgPipeFunction);
            mm.removeFunction(sceKernelSendMsgPipeCBFunction);
            mm.removeFunction(sceKernelTrySendMsgPipeFunction);
            mm.removeFunction(sceKernelReceiveMsgPipeFunction);
            mm.removeFunction(sceKernelReceiveMsgPipeCBFunction);
            mm.removeFunction(sceKernelTryReceiveMsgPipeFunction);
            mm.removeFunction(sceKernelCancelMsgPipeFunction);
            mm.removeFunction(sceKernelReferMsgPipeStatusFunction);
            mm.removeFunction(sceKernelCreateVplFunction);
            mm.removeFunction(sceKernelDeleteVplFunction);
            mm.removeFunction(sceKernelAllocateVplFunction);
            mm.removeFunction(sceKernelAllocateVplCBFunction);
            mm.removeFunction(sceKernelTryAllocateVplFunction);
            mm.removeFunction(sceKernelFreeVplFunction);
            mm.removeFunction(sceKernelCancelVplFunction);
            mm.removeFunction(sceKernelReferVplStatusFunction);
            mm.removeFunction(sceKernelCreateFplFunction);
            mm.removeFunction(sceKernelDeleteFplFunction);
            mm.removeFunction(sceKernelAllocateFplFunction);
            mm.removeFunction(sceKernelAllocateFplCBFunction);
            mm.removeFunction(sceKernelTryAllocateFplFunction);
            mm.removeFunction(sceKernelFreeFplFunction);
            mm.removeFunction(sceKernelCancelFplFunction);
            mm.removeFunction(sceKernelReferFplStatusFunction);
            mm.removeFunction(_sceKernelReturnFromTimerHandlerFunction);
            mm.removeFunction(sceKernelUSec2SysClockFunction);
            mm.removeFunction(sceKernelUSec2SysClockWideFunction);
            mm.removeFunction(sceKernelSysClock2USecFunction);
            mm.removeFunction(sceKernelSysClock2USecWideFunction);
            mm.removeFunction(sceKernelGetSystemTimeFunction);
            mm.removeFunction(sceKernelGetSystemTimeWideFunction);
            mm.removeFunction(sceKernelGetSystemTimeLowFunction);
            mm.removeFunction(sceKernelSetAlarmFunction);
            mm.removeFunction(sceKernelSetSysClockAlarmFunction);
            mm.removeFunction(sceKernelCancelAlarmFunction);
            mm.removeFunction(sceKernelReferAlarmStatusFunction);
            mm.removeFunction(sceKernelCreateVTimerFunction);
            mm.removeFunction(sceKernelDeleteVTimerFunction);
            mm.removeFunction(sceKernelGetVTimerBaseFunction);
            mm.removeFunction(sceKernelGetVTimerBaseWideFunction);
            mm.removeFunction(sceKernelGetVTimerTimeFunction);
            mm.removeFunction(sceKernelGetVTimerTimeWideFunction);
            mm.removeFunction(sceKernelSetVTimerTimeFunction);
            mm.removeFunction(sceKernelSetVTimerTimeWideFunction);
            mm.removeFunction(sceKernelStartVTimerFunction);
            mm.removeFunction(sceKernelStopVTimerFunction);
            mm.removeFunction(sceKernelSetVTimerHandlerFunction);
            mm.removeFunction(sceKernelSetVTimerHandlerWideFunction);
            mm.removeFunction(sceKernelCancelVTimerHandlerFunction);
            mm.removeFunction(sceKernelReferVTimerStatusFunction);
            mm.removeFunction(sceKernelCreateThreadFunction);
            mm.removeFunction(sceKernelDeleteThreadFunction);
            mm.removeFunction(sceKernelStartThreadFunction);
            mm.removeFunction(_sceKernelExitThreadFunction);
            mm.removeFunction(sceKernelExitThreadFunction);
            mm.removeFunction(sceKernelExitDeleteThreadFunction);
            mm.removeFunction(sceKernelTerminateThreadFunction);
            mm.removeFunction(sceKernelTerminateDeleteThreadFunction);
            mm.removeFunction(sceKernelSuspendDispatchThreadFunction);
            mm.removeFunction(sceKernelResumeDispatchThreadFunction);
            mm.removeFunction(sceKernelChangeCurrentThreadAttrFunction);
            mm.removeFunction(sceKernelChangeThreadPriorityFunction);
            mm.removeFunction(sceKernelRotateThreadReadyQueueFunction);
            mm.removeFunction(sceKernelReleaseWaitThreadFunction);
            mm.removeFunction(sceKernelGetThreadIdFunction);
            mm.removeFunction(sceKernelGetThreadCurrentPriorityFunction);
            mm.removeFunction(sceKernelGetThreadExitStatusFunction);
            mm.removeFunction(sceKernelCheckThreadStackFunction);
            mm.removeFunction(sceKernelGetThreadStackFreeSizeFunction);
            mm.removeFunction(sceKernelReferThreadStatusFunction);
            mm.removeFunction(sceKernelReferThreadRunStatusFunction);
            mm.removeFunction(sceKernelReferSystemStatusFunction);
            mm.removeFunction(sceKernelGetThreadmanIdListFunction);
            mm.removeFunction(sceKernelGetThreadmanIdTypeFunction);
            mm.removeFunction(sceKernelReferThreadProfilerFunction);
            mm.removeFunction(sceKernelReferGlobalProfilerFunction);

            mm.removeFunction(hleKernelAsyncLoopFunction);
            mm.removeFunction(hleKernelExitCallbackFunction);
            mm.removeFunction(hleKernelExitThreadFunction);
        }
    }
    private HashMap<Integer, SceKernelThreadInfo> threadMap;
    private HashMap<Integer, SceKernelThreadEventHandlerInfo> threadEventHandlerMap;
    private HashMap<Integer, Integer> threadEventMap;
    private LinkedList<SceKernelThreadInfo> readyThreads;
    private SceKernelThreadInfo currentThread;
    private SceKernelThreadInfo idle0,  idle1;
    public Statistics statistics;
    private boolean dispatchThreadEnabled;
    private static final int SCE_KERNEL_DISPATCHTHREAD_STATE_DISABLED = 0;
    private static final int SCE_KERNEL_DISPATCHTHREAD_STATE_ENABLED = 1;

    // The PSP seems to have a resolution of 200us
    protected static final int THREAD_DELAY_MINIMUM_MICROS = 200;

    protected static final int CALLBACKID_REGISTER = 16; // $s0
    protected CallbackManager callbackManager = new CallbackManager();
    protected static final int IDLE_THREAD_ADDRESS = MemoryMap.START_RAM;
    public static final int THREAD_EXIT_HANDLER_ADDRESS = MemoryMap.START_RAM + 0x20;
    protected static final int CALLBACK_EXIT_HANDLER_ADDRESS = MemoryMap.START_RAM + 0x30;
    public static final int ASYNC_LOOP_ADDRESS = MemoryMap.START_RAM + 0x40;
    private HashMap<Integer, SceKernelCallbackInfo> callbackMap;
    private boolean USE_THREAD_BANLIST = false;
    private static final boolean LOG_CONTEXT_SWITCHING = true;
    private static final boolean LOG_INSTRUCTIONS = false;
    public boolean exitCalled = false;

    // see sceKernelGetThreadmanIdList
    public final static int SCE_KERNEL_TMID_Thread = 1;
    public final static int SCE_KERNEL_TMID_Semaphore = 2;
    public final static int SCE_KERNEL_TMID_EventFlag = 3;
    public final static int SCE_KERNEL_TMID_Mbox = 4;
    public final static int SCE_KERNEL_TMID_Vpl = 5;
    public final static int SCE_KERNEL_TMID_Fpl = 6;
    public final static int SCE_KERNEL_TMID_Mpipe = 7;
    public final static int SCE_KERNEL_TMID_Callback = 8;
    public final static int SCE_KERNEL_TMID_ThreadEventHandler = 9;
    public final static int SCE_KERNEL_TMID_Alarm = 10;
    public final static int SCE_KERNEL_TMID_VTimer = 11;
    public final static int SCE_KERNEL_TMID_Mutex = 12;
    public final static int SCE_KERNEL_TMID_LwMutex = 13;
    public final static int SCE_KERNEL_TMID_SleepThread = 64;
    public final static int SCE_KERNEL_TMID_DelayThread = 65;
    public final static int SCE_KERNEL_TMID_SuspendThread = 66;
    public final static int SCE_KERNEL_TMID_DormantThread = 67;
    protected static final int INTR_NUMBER = IntrManager.PSP_SYSTIMER0_INTR;
    protected Map<Integer, SceKernelAlarmInfo> alarms;
    protected Map<Integer, SceKernelVTimerInfo> vtimers;
    protected boolean needThreadReschedule;

    public ThreadManForUser() {
    }

    @Override
    public void start() {
        threadMap = new HashMap<Integer, SceKernelThreadInfo>();
        threadEventMap = new HashMap<Integer, Integer>();
        threadEventHandlerMap = new HashMap<Integer, SceKernelThreadEventHandlerInfo>();
        readyThreads = new LinkedList<SceKernelThreadInfo>();
        statistics = new Statistics();

        callbackMap = new HashMap<Integer, SceKernelCallbackInfo>();
        callbackManager.Initialize();

        install_idle_threads();
        install_thread_exit_handler();
        install_callback_exit_handler();
        install_async_loop_handler();

        alarms = new HashMap<Integer, SceKernelAlarmInfo>();
        vtimers = new HashMap<Integer, SceKernelVTimerInfo>();

        dispatchThreadEnabled = true;
        needThreadReschedule = true;

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		if (threadMXBean.isThreadCpuTimeSupported()) {
			threadMXBean.setThreadCpuTimeEnabled(true);
		}
    }

    @Override
    public void stop() {
        alarms = null;
        vtimers = null;
        for(SceKernelThreadInfo thread : threadMap.values()) {
            terminateThread(thread);
        }
    }

    public Iterator<SceKernelThreadInfo> iterator() {
        return threadMap.values().iterator();
    }

    public Iterator<SceKernelThreadInfo> iteratorByPriority() {
        Collection<SceKernelThreadInfo> c = threadMap.values();
        List<SceKernelThreadInfo> list = new LinkedList<SceKernelThreadInfo>(c);
        Collections.sort(list, idle0); // We need an instance of SceKernelThreadInfo for the comparator, so we use idle0
        return list.iterator();
    }

    /** call this when resetting the emulator
     * @param entry_addr entry from ELF header
     * @param attr from sceModuleInfo ELF section header */
    public void Initialise(SceModule module, int entry_addr, int attr, String pspfilename, int moduleid, boolean fromSyscall) {
        // Create a thread the program will run inside

        // The stack size seems to be 0x40000 when starting the application from the VSH
        // and smaller when starting the application with sceKernelLoadExec() - guess: 0x4000.
        // This could not be reproduced on a PSP.
        int rootStackSize = (fromSyscall ? 0x4000 : 0x40000);
        // Use the module_start_thread_stacksize when this information was present in the ELF file
        if (module != null && module.module_start_thread_stacksize > 0) {
            rootStackSize = module.module_start_thread_stacksize;
        }

        int rootInitPriority = 0x20;
        // Use the module_start_thread_priority when this information was present in the ELF file
        if (module != null && module.module_start_thread_priority > 0) {
            rootInitPriority = module.module_start_thread_priority;
        }
        currentThread = new SceKernelThreadInfo("root", entry_addr, rootInitPriority, rootStackSize, attr);
        currentThread.moduleid = moduleid;
        threadMap.put(currentThread.uid, currentThread);

        // Set user mode bit if kernel mode bit is not present
        if ((currentThread.attr & PSP_THREAD_ATTR_KERNEL) != PSP_THREAD_ATTR_KERNEL) {
            currentThread.attr |= PSP_THREAD_ATTR_USER;
        }

        // Setup args by copying them onto the stack
        //Modules.log.debug("pspfilename - '" + pspfilename + "'");
        int len = pspfilename.length();
        int address = currentThread.cpuContext.gpr[29];
        writeStringZ(Memory.getInstance(), address, pspfilename);
        currentThread.cpuContext.gpr[4] = len + 1; // a0 = len + string terminator
        currentThread.cpuContext.gpr[5] = address; // a1 = pointer to arg data in stack

        currentThread.status = PSP_THREAD_READY;

        // Switch in the thread
        currentThread.status = PSP_THREAD_RUNNING;
        currentThread.restoreContext();
    }

    private void install_idle_threads() {
        Memory mem = Memory.getInstance();

        // Generate 2 idle threads which can toggle between each other when there are no ready threads
        int instruction_addiu = // addiu a0, zr, 0
                ((AllegrexOpcodes.ADDIU & 0x3f) << 26) | ((0 & 0x1f) << 21) | ((4 & 0x1f) << 16);
        int instruction_lui = // lui ra, 0x08000000
                ((AllegrexOpcodes.LUI & 0x3f) << 26) | ((31 & 0x1f) << 16) | (0x0800 & 0x0000ffff);
        int instruction_jr = // jr ra
                ((AllegrexOpcodes.SPECIAL & 0x3f) << 26) | (AllegrexOpcodes.JR & 0x3f) | ((31 & 0x1f) << 21);
        int instruction_syscall = // syscall 0x0201c [sceKernelDelayThread]
                ((AllegrexOpcodes.SPECIAL & 0x3f) << 26) | (AllegrexOpcodes.SYSCALL & 0x3f) | ((sceKernelDelayThreadFunction.getSyscallCode() & 0x000fffff) << 6);

        // This memory is always reserved on a real PSP
        SysMemInfo info = Modules.SysMemUserForUserModule.malloc(1, "ThreadMan-RootMem", SysMemUserForUser.PSP_SMEM_Addr, 0x4000, MemoryMap.START_USERSPACE);
        int reservedMem = info.addr;

        mem.write32(IDLE_THREAD_ADDRESS + 0, instruction_addiu);
        mem.write32(IDLE_THREAD_ADDRESS + 4, instruction_lui);
        mem.write32(IDLE_THREAD_ADDRESS + 8, instruction_jr);
        mem.write32(IDLE_THREAD_ADDRESS + 12, instruction_syscall);

        // lowest allowed priority is 0x77, so we are ok at 0x7f
        // Allocate a stack because interrupts can be processed by the
        // idle thread, using its stack.
        // The stack is allocated into the reservedMem area.
        idle0 = new SceKernelThreadInfo("idle0", IDLE_THREAD_ADDRESS | 0x80000000, 0x7f, 0, PSP_THREAD_ATTR_KERNEL);
        idle0.freeStack();
        idle0.stackSize = 0x2000;
        idle0.stack_addr = reservedMem;
        idle0.reset();
        idle0.exitStatus = ERROR_KERNEL_THREAD_IS_NOT_DORMANT;
        threadMap.put(idle0.uid, idle0);
        hleChangeThreadState(idle0, PSP_THREAD_READY);

        idle1 = new SceKernelThreadInfo("idle1", IDLE_THREAD_ADDRESS | 0x80000000, 0x7f, 0, PSP_THREAD_ATTR_KERNEL);
        idle1.freeStack();
        idle1.stackSize = 0x2000;
        idle1.stack_addr = reservedMem + 0x2000;
        idle1.reset();
        idle1.exitStatus = ERROR_KERNEL_THREAD_IS_NOT_DORMANT;
        threadMap.put(idle1.uid, idle1);
        hleChangeThreadState(idle1, PSP_THREAD_READY);
    }

    private void install_thread_exit_handler() {
        Memory mem = Memory.getInstance();

        int instruction_syscall = // syscall 0x6f000 [hleKernelExitThread]
                ((AllegrexOpcodes.SPECIAL & 0x3f) << 26) | (AllegrexOpcodes.SYSCALL & 0x3f) | ((hleKernelExitThreadFunction.getSyscallCode() & 0x000fffff) << 6);

        // Add a "jr $ra" instruction to indicate the end of the CodeBlock to the compiler
        int instruction_jr = AllegrexOpcodes.JR | (31 << 21);

        mem.write32(THREAD_EXIT_HANDLER_ADDRESS + 0, instruction_syscall);
        mem.write32(THREAD_EXIT_HANDLER_ADDRESS + 4, instruction_jr);
    }

    private void install_callback_exit_handler() {
        Memory mem = Memory.getInstance();

        int instruction_syscall = // syscall 0x6f001 [hleKernelExitCallback]
                ((AllegrexOpcodes.SPECIAL & 0x3f) << 26) | (AllegrexOpcodes.SYSCALL & 0x3f) | ((hleKernelExitCallbackFunction.getSyscallCode() & 0x000fffff) << 6);

        // Add a "jr $ra" instruction to indicate the end of the CodeBlock to the compiler
        int instruction_jr = AllegrexOpcodes.JR | (31 << 21);

        mem.write32(CALLBACK_EXIT_HANDLER_ADDRESS + 0, instruction_syscall);
        mem.write32(CALLBACK_EXIT_HANDLER_ADDRESS + 4, instruction_jr);
    }

    private void install_async_loop_handler() {
        Memory mem = Memory.getInstance();

        int instruction_syscall = // syscall 0x6f002 [hleKernelAsyncLoop]
                ((AllegrexOpcodes.SPECIAL & 0x3f) << 26) | (AllegrexOpcodes.SYSCALL & 0x3f) | ((hleKernelAsyncLoopFunction.getSyscallCode() & 0x000fffff) << 6);

        int instruction_b = (AllegrexOpcodes.BEQ << 26) | 0xFFFE; // branch back to syscall
        int instruction_nop = (AllegrexOpcodes.SLL << 26); // nop

        // Add a "jr $ra" instruction to indicate the end of the CodeBlock to the compiler
        int instruction_jr = AllegrexOpcodes.JR | (31 << 21);

        mem.write32(ASYNC_LOOP_ADDRESS + 0, instruction_syscall);
        mem.write32(ASYNC_LOOP_ADDRESS + 4, instruction_b);
        mem.write32(ASYNC_LOOP_ADDRESS + 8, instruction_nop);
        mem.write32(ASYNC_LOOP_ADDRESS + 12, instruction_jr);
    }

    /** to be called when exiting the emulation */
    public void exit() {
        exitCalled = true;

        if (threadMap != null) {
            // Delete all the threads to collect statistics
            deleteAllThreads();

            log.info("----------------------------- ThreadMan exit -----------------------------");

            if (DurationStatistics.collectStatistics) {
            	statistics.exit();
	            log.info(String.format("ThreadMan Statistics (%,d cycles in %.3fs):", statistics.allCycles, statistics.getDurationMillis() / 1000.0));
	            Collections.sort(statistics.threads);
	            for (Statistics.ThreadStatistics threadStatistics : statistics.threads) {
	                double percentage = 0;
	                if (statistics.allCycles != 0) {
	                    percentage = (threadStatistics.runClocks / (double) statistics.allCycles) * 100;
	                }
	                log.info(String.format("    Thread %-30s %,12d cycles (%5.2f%%)", threadStatistics.getQuotedName(), threadStatistics.runClocks, percentage));
	            }
            }
        }
    }

    /** To be called from the main emulation loop
     *  This is only used when running in interpreter mode,
     *  i.e. it is no longer used when the Compiler is enabled.
     */
    public void step() {
        if (LOG_INSTRUCTIONS) {
            if (log.isTraceEnabled()) {
                CpuState cpu = Emulator.getProcessor().cpu;

                if (!isIdleThread(currentThread) && cpu.pc != 0) {
                    int address = cpu.pc - 4;
                    int opcode = Memory.getInstance().read32(address);
                    log.trace(String.format("Executing %08X %s", address, Decoder.instruction(opcode).disasm(address, opcode)));
                }
            }
        }

        if (currentThread != null) {
            currentThread.runClocks++;
        } else if (!exitCalled) {
            // We always need to be in a thread! we shouldn't get here.
            log.error("No ready threads!");
        }
    }

    private void internalContextSwitch(SceKernelThreadInfo newThread) {
        if (currentThread != null) {
            // Switch out old thread
            if (currentThread.status == PSP_THREAD_RUNNING) {
                hleChangeThreadState(currentThread, PSP_THREAD_READY);
            }

            // save registers
            currentThread.saveContext();
        }

        if (newThread != null) {
            // Switch in new thread
            hleChangeThreadState(newThread, PSP_THREAD_RUNNING);
            // restore registers
            newThread.restoreContext();

            if (LOG_CONTEXT_SWITCHING && Modules.log.isDebugEnabled() && !isIdleThread(newThread)) {
                log.debug("---------------------------------------- SceUID=" + Integer.toHexString(newThread.uid) + " name:'" + newThread.name + "'");
            }
        } else {
            // When running under compiler mode this gets triggered by exit()
            if (!exitCalled) {
                DumpDebugState.dumpDebugState();

                log.info("No ready threads - pausing emulator. caller:" + getCallingFunction());
                Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNKNOWN);
            }
        }

        currentThread = newThread;

        RuntimeContext.update();
    }

    /** @param newThread The thread to switch in. */
    private boolean contextSwitch(SceKernelThreadInfo newThread) {
        if (IntrManager.getInstance().isInsideInterrupt()) {
            // No context switching inside an interrupt
            if (log.isDebugEnabled()) {
                log.debug("Inside an interrupt, not context switching to " + newThread);
            }
            return false;
        }

        if (!dispatchThreadEnabled) {
            log.info("DispatchThread disabled, not context switching to " + newThread);
            return false;
        }

        internalContextSwitch(newThread);

        checkThreadCallbacks(currentThread);

        return true;
    }

    /** This function must have the property of never returning currentThread,
     * unless currentThread is already null.
     * @return The next thread to schedule (based on thread priorities). */
    private SceKernelThreadInfo nextThread() {
        // Find the thread with status PSP_THREAD_READY and the highest priority.
        // In this implementation low priority threads can get starved.
        // Remark: the currentThread is not present in the readyThreads List.
        SceKernelThreadInfo found = null;
        synchronized (readyThreads) {
            for (SceKernelThreadInfo thread : readyThreads) {
                if (found == null || thread.currentPriority < found.currentPriority) {
                    found = thread;
                }
            }
        }
        return found;
    }

    /**
     * Switch to the thread with status PSP_THREAD_READY and having the highest priority.
     * If the current thread is in status PSP_THREAD_READY and
     * still having the highest priority, nothing is changed.
     * If the current thread is having the same priority as the highest priority,
     * nothing is changed (no yielding to threads having the same priority).
     */
    public void hleRescheduleCurrentThread() {
    	if (needThreadReschedule) {
	        SceKernelThreadInfo newThread = nextThread();
	        if (newThread != null &&
	                (currentThread == null ||
	                currentThread.status != PSP_THREAD_RUNNING ||
	                currentThread.currentPriority > newThread.currentPriority)) {
	            if (LOG_CONTEXT_SWITCHING && Modules.log.isDebugEnabled()) {
	                log.debug("Context switching to '" + newThread + "' after reschedule");
	            }

	            if (contextSwitch(newThread)) {
	            	needThreadReschedule = false;
	            }
	        } else {
	        	needThreadReschedule = false;
	        }
    	}
    }

    /**
     * Same behavior as hleRescheduleCurrentThread()
     * excepted that it executes callbacks when doCallbacks == true
     */
    public void hleRescheduleCurrentThread(boolean doCallbacks) {
        if (doCallbacks) {
            if (currentThread != null) {
                currentThread.doCallbacks = doCallbacks;
            }
            checkCallbacks();
        }

        hleRescheduleCurrentThread();
    }

    public int getCurrentThreadID() {
        if (currentThread == null) {
            return -1;
        }

        return currentThread.uid;
    }

    public SceKernelThreadInfo getCurrentThread() {
        return currentThread;
    }

    public boolean isIdleThread(SceKernelThreadInfo thread) {
        return (thread == idle0 || thread == idle1);
    }

    public boolean isKernelMode() {
        return ((currentThread.attr & PSP_THREAD_ATTR_KERNEL) == PSP_THREAD_ATTR_KERNEL);
    }

    public String getThreadName(int uid) {
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            return "NOT A THREAD";
        }
        return thread.name;
    }

    public boolean isThreadBlocked(SceKernelThreadInfo thread) {
    	return (thread.status & PSP_THREAD_WAITING) != 0 && thread.wait.waitingBlocked;
    }

    public void hleBlockCurrentThread() {
        hleBlockCurrentThread(null);
    }

    private void hleBlockThread(SceKernelThreadInfo thread, boolean doCallbacks, IAction onUnblockAction, IWaitStateChecker waitStateChecker) {
    	// A blocked thread (e.g. a thread blocked due to audio output or
    	// wait for vblank or sceCtrl sample reading) is implemented like
    	// a "wait for Event Flag". This is the closest implementation to a real PSP,
    	// as event flags are usually used by a PSP to implement these wait
    	// functions.
        if (thread.status != PSP_THREAD_WAITING) {
	    	thread.doCallbacks = doCallbacks;
	    	thread.wait.waitingBlocked = true;
	    	thread.wait.onUnblockAction = onUnblockAction;
	    	thread.waitType = SceKernelThreadInfo.PSP_WAIT_EVENTFLAG;
	    	thread.waitId = 0;
	    	thread.wait.waitStateChecker = waitStateChecker;
	        hleChangeThreadState(thread, PSP_THREAD_WAITING);
        }
    }

    public void hleBlockCurrentThread(IAction onUnblockAction) {
        if (LOG_CONTEXT_SWITCHING && Modules.log.isDebugEnabled()) {
            log.debug("-------------------- block SceUID=" + Integer.toHexString(currentThread.uid) + " name:'" + currentThread.name + "' caller:" + getCallingFunction());
        }

    	hleBlockThread(currentThread, false, onUnblockAction, null);
        hleRescheduleCurrentThread();
    }

    public void hleBlockCurrentThreadCB(IAction onUnblockAction, IWaitStateChecker waitStateChecker) {
        if (LOG_CONTEXT_SWITCHING && Modules.log.isDebugEnabled()) {
            log.debug("-------------------- block SceUID=" + Integer.toHexString(currentThread.uid) + " name:'" + currentThread.name + "' caller:" + getCallingFunction());
        }

    	hleBlockThread(currentThread, true, onUnblockAction, waitStateChecker);
        hleRescheduleCurrentThread(true);
    }

    public SceKernelThreadInfo getThreadById(int uid) {
        return threadMap.get(uid);
    }

    public SceKernelThreadInfo getThreadByName(String name) {
        for (SceKernelThreadInfo thread : threadMap.values()) {
            if (name.equals(thread.name)) {
            	return thread;
            }
        }

        return null;
    }

    public void hleUnblockThread(int uid) {
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", false)) {
            SceKernelThreadInfo thread = threadMap.get(uid);
            hleChangeThreadState(thread, PSP_THREAD_READY);

            if (LOG_CONTEXT_SWITCHING && thread != null && Modules.log.isDebugEnabled()) {
                log.debug("-------------------- unblock SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' caller:" + getCallingFunction());
            }
        }
    }

    private String getCallingFunction() {
        String msg = "";
        StackTraceElement[] lines = new Exception().getStackTrace();
        if (lines.length >= 3) {
            msg = lines[2].toString();
            msg = msg.substring(0, msg.indexOf("("));
            //msg = "'" + msg.substring(msg.lastIndexOf(".") + 1, msg.length()) + "'";
            String[] parts = msg.split("\\.");
            msg = "'" + parts[parts.length - 2] + "." + parts[parts.length - 1] + "'";
        } else {
            for (StackTraceElement e : lines) {
                String line = e.toString();
                if (line.startsWith("jpcsp.Allegrex") || line.startsWith("jpcsp.Processor")) {
                    break;
                }
                msg += "\n" + line;
            }
        }
        return msg;
    }

    public void hleThreadWaitTimeout(SceKernelThreadInfo thread) {
        if (thread.waitType == PSP_WAIT_NONE) {
            // The thread is no longer waiting...
        } else {
            onWaitTimeout(thread);
            hleChangeThreadState(thread, PSP_THREAD_READY);
        }
    }

    /** Call this when a thread's wait timeout has expired.
     * You can assume the calling function will set thread.status = ready. */
    private void onWaitTimeout(SceKernelThreadInfo thread) {
        // ThreadEnd
        if (thread.wait.waitingOnThreadEnd) {
            // Untrack
            thread.wait.waitingOnThreadEnd = false;

            // Return WAIT_TIMEOUT
            thread.cpuContext.gpr[2] = ERROR_KERNEL_WAIT_TIMEOUT;
        } // EventFlag
        else if (thread.wait.waitingOnEventFlag) {
            Managers.eventFlags.onThreadWaitTimeout(thread);
        } // Sema
        else if (thread.wait.waitingOnSemaphore) {
            Managers.semas.onThreadWaitTimeout(thread);
        } // UMD stat
        else if (thread.wait.waitingOnUmd) {
            Modules.sceUmdUserModule.onThreadWaitTimeout(thread);
        } // Mutex
        else if (thread.wait.waitingOnMutex) {
            Managers.mutex.onThreadWaitTimeout(thread);
        } // LwMutex
        else if (thread.wait.waitingOnLwMutex) {
            Managers.lwmutex.onThreadWaitTimeout(thread);
        } // MsgPipe
        else if (thread.wait.waitingOnMsgPipeSend || thread.wait.waitingOnMsgPipeReceive) {
            Managers.msgPipes.onThreadWaitTimeout(thread);
        } //Mbx
        else if (thread.wait.waitingOnMbxReceive) {
            Managers.mbx.onThreadWaitTimeout(thread);
        } //Fpl
        else if (thread.wait.waitingOnFpl) {
            Managers.fpl.onThreadWaitTimeout(thread);
        } //Vpl
        else if (thread.wait.waitingOnVpl) {
            Managers.vpl.onThreadWaitTimeout(thread);
        }

    // IO has no timeout, it's always forever
    }

    public void hleThreadWaitRelease(SceKernelThreadInfo thread) {
        if (thread.waitType != PSP_WAIT_NONE) {
            onWaitReleased(thread);
            hleChangeThreadState(thread, PSP_THREAD_READY);
        }
    }

    /** Call this when a thread's wait has been released. */
    private void onWaitReleased(SceKernelThreadInfo thread) {
        // ThreadEnd
        if (thread.wait.waitingOnThreadEnd) {
            // Untrack
            thread.wait.waitingOnThreadEnd = false;
            // Return ERROR_WAIT_STATUS_RELEASED
            thread.cpuContext.gpr[2] = ERROR_KERNEL_WAIT_STATUS_RELEASED;
        } // EventFlag
        else if (thread.wait.waitingOnEventFlag) {
            Managers.eventFlags.onThreadWaitReleased(thread);
        } // Sema
        else if (thread.wait.waitingOnSemaphore) {
            Managers.semas.onThreadWaitReleased(thread);
        } // UMD stat
        else if (thread.wait.waitingOnUmd) {
            Modules.sceUmdUserModule.onThreadWaitReleased(thread);
        } // Mutex
        else if (thread.wait.waitingOnMutex) {
            Managers.mutex.onThreadWaitReleased(thread);
        } // LwMutex
        else if (thread.wait.waitingOnLwMutex) {
            Managers.lwmutex.onThreadWaitReleased(thread);
        } // MsgPipe
        else if (thread.wait.waitingOnMsgPipeSend || thread.wait.waitingOnMsgPipeReceive) {
            Managers.msgPipes.onThreadWaitReleased(thread);
        } //Mbx
        else if (thread.wait.waitingOnMbxReceive) {
            Managers.mbx.onThreadWaitReleased(thread);
        } //Fpl
        else if (thread.wait.waitingOnFpl) {
            Managers.fpl.onThreadWaitReleased(thread);
        } //Vpl
        else if (thread.wait.waitingOnVpl) {
            Managers.vpl.onThreadWaitReleased(thread);
        } // Block
        else if (thread.wait.waitingBlocked) {
        	thread.cpuContext.gpr[2] = ERROR_KERNEL_WAIT_STATUS_RELEASED;
        }
        // IO has no timeout, it's always forever.
    }

    private void deleteAllThreads() {
    	// Copy the list of threads into a new list to avoid ConcurrentListModificationException
    	List<SceKernelThreadInfo> threadsToBeDeleted = new LinkedList<SceKernelThreadInfo>(threadMap.values());

    	for (SceKernelThreadInfo thread : threadsToBeDeleted) {
    		hleDeleteThread(thread);
    	}
    }

    public void hleDeleteThread(SceKernelThreadInfo thread) {
        if (!threadMap.containsKey(thread.uid)) {
            log.debug(String.format("Thread %s already deleted", thread.toString()));
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("really deleting thread:'" + thread.name + "'");
        }

        // cleanup thread - free the stack
        if (thread.stack_addr != 0) {
            if (log.isDebugEnabled()) {
                log.debug("thread:'" + thread.name + "' freeing stack " + String.format("0x%08X", thread.stack_addr));
            }
            thread.freeStack();
        }

        cancelThreadWait(thread);
        threadMap.remove(thread.uid);
        SceUidManager.releaseUid(thread.uid, "ThreadMan-thread");

        Managers.eventFlags.onThreadDeleted(thread);
        Managers.semas.onThreadDeleted(thread);
        Managers.mutex.onThreadDeleted(thread);
        Managers.lwmutex.onThreadDeleted(thread);
        Managers.msgPipes.onThreadDeleted(thread);
        Managers.mbx.onThreadDeleted(thread);
        Managers.fpl.onThreadDeleted(thread);
        Managers.vpl.onThreadDeleted(thread);
        Modules.sceUmdUserModule.onThreadDeleted(thread);
        RuntimeContext.onThreadDeleted(thread);
        // TODO blocking audio?
        // TODO async io?

        statistics.addThreadStatistics(thread);
    }

    private void removeFromReadyThreads(SceKernelThreadInfo thread) {
        synchronized (readyThreads) {
            readyThreads.remove(thread);
        	needThreadReschedule = true;
        }
    }

    private void addToReadyThreads(SceKernelThreadInfo thread, boolean addFirst) {
        synchronized (readyThreads) {
        	if (addFirst) {
        		readyThreads.addFirst(thread);
        	} else {
        		readyThreads.addLast(thread);
        	}
        	needThreadReschedule = true;
        }
    }

    private void setToBeDeletedThread(SceKernelThreadInfo thread) {
        thread.doDelete = true;

        if (thread.status == PSP_THREAD_STOPPED) {
            // It's possible for a game to request the same thread to be deleted multiple times.
            // We only mark for deferred deletion.
            // Example:
            // - main thread calls sceKernelDeleteThread on child thread
            // - child thread calls sceKernelExitDeleteThread
            if (thread.doDeleteAction == null) {
                thread.doDeleteAction = new DeleteThreadAction(thread);
                Scheduler.getInstance().addAction(thread.doDeleteAction);
            }
        }
    }

    public void hleKernelChangeThreadPriority(SceKernelThreadInfo thread, int newPriority) {
    	if (thread == null) {
    		return;
    	}

    	int oldPriority = thread.currentPriority;
        thread.currentPriority = newPriority;
        // switch in the target thread if it's now higher priority
        if ((thread.status & PSP_THREAD_READY) == PSP_THREAD_READY &&
        		newPriority < currentThread.currentPriority) {
            if (log.isDebugEnabled()) {
                log.debug("hleKernelChangeThreadPriority yielding to thread with higher priority");
            }
            needThreadReschedule = true;
            hleRescheduleCurrentThread();
        } else if ((thread.status & PSP_THREAD_READY) == PSP_THREAD_READY &&
            		newPriority == oldPriority) {
            if (log.isDebugEnabled()) {
                log.debug("hleKernelChangeThreadPriority yielding to thread with same priority");
            }
            // Move the thread to the end of the list
        	removeFromReadyThreads(thread);
            addToReadyThreads(thread, false);
            needThreadReschedule = true;
            hleRescheduleCurrentThread();
        } else if (thread.uid == currentThread.uid && newPriority > oldPriority) {
            if (log.isDebugEnabled()) {
                log.debug("hleKernelChangeThreadPriority rescheduling");
            }
            // yield if we moved ourself to lower priority
            needThreadReschedule = true;
            hleRescheduleCurrentThread();
        }
    }

    /**
     * Change to state of a thread.
     * This function must be used when changing the state of a thread as
     * it updates the ThreadMan internal data structures and implements
     * the PSP behavior on status change.
     *
     * @param thread    the thread to be updated
     * @param newStatus the new thread status
     */
    public void hleChangeThreadState(SceKernelThreadInfo thread, int newStatus) {
        if (thread == null) {
            return;
        }

        if (thread.status == newStatus) {
            // Thread status not changed, nothing to do
            return;
        }

        if (!dispatchThreadEnabled && thread == currentThread && newStatus != PSP_THREAD_RUNNING) {
            log.info("DispatchThread disabled, not changing thread state of " + thread + " to " + newStatus);
            return;
        }

        boolean addReadyThreadsFirst = false;

        // Moving out of the following states...
        if (thread.status == PSP_THREAD_WAITING && newStatus != PSP_THREAD_WAITING_SUSPEND) {
            if (thread.wait.waitTimeoutAction != null) {
                Scheduler.getInstance().removeAction(thread.wait.microTimeTimeout, thread.wait.waitTimeoutAction);
                thread.wait.waitTimeoutAction = null;
            }
            if (thread.wait.waitingBlocked) {
            	thread.wait.waitingBlocked = false;
            	if (thread.wait.onUnblockAction != null) {
            		thread.wait.onUnblockAction.execute();
            		thread.wait.onUnblockAction = null;
            	}
            }
            thread.doCallbacks = false;
        } else if (thread.status == PSP_THREAD_STOPPED) {
            if (thread.doDeleteAction != null) {
                Scheduler.getInstance().removeAction(0, thread.doDeleteAction);
                thread.doDeleteAction = null;
            }
        } else if (thread.status == PSP_THREAD_READY) {
            removeFromReadyThreads(thread);
        } else if (thread.status == PSP_THREAD_SUSPEND) {
            thread.doCallbacks = false;
        } else if (thread.status == PSP_THREAD_RUNNING) {
        	needThreadReschedule = true;
        	// When a running thread has to yield to a thread having a higher
        	// priority, the thread stays in front of the ready threads having
        	// the same priority (no yielding to threads having the same priority).
        	addReadyThreadsFirst = true;
        }

        thread.status = newStatus;

        // Moving to the following states...
        if (thread.status == PSP_THREAD_WAITING) {
            if (thread.wait.waitTimeoutAction != null) {
                Scheduler.getInstance().addAction(thread.wait.microTimeTimeout, thread.wait.waitTimeoutAction);
            }

            // debug
            if (thread.waitType == PSP_WAIT_NONE) {
                log.warn("changeThreadState thread '" + thread.name + "' => PSP_THREAD_WAITING. waitType should NOT be PSP_WAIT_NONE. caller:" + getCallingFunction());
            }
        } else if (thread.status == PSP_THREAD_STOPPED) {
            // TODO check if stopped threads eventually get automatically deleted on a real psp
            // HACK auto delete module mgr threads
            if (thread.name.equals("root") || // should probably find the real name and change it
                    thread.name.equals("SceModmgrStart") ||
                    thread.name.equals("SceKernelModmgrStop")) {
                thread.doDelete = true;
            }

            if (thread.doDelete) {
                if (thread.doDeleteAction == null) {
                    thread.doDeleteAction = new DeleteThreadAction(thread);
                    Scheduler.getInstance().addAction(0, thread.doDeleteAction);
                }
            }
            onThreadStopped(thread);
        } else if (thread.status == PSP_THREAD_READY) {
            addToReadyThreads(thread, addReadyThreadsFirst);
            thread.waitType = PSP_WAIT_NONE;
            thread.wait.waitTimeoutAction = null;
            thread.wait.waitStateChecker = null;
        } else if (thread.status == PSP_THREAD_RUNNING) {
            // debug
            if (thread.waitType != PSP_WAIT_NONE) {
                log.error("changeThreadState thread '" + thread.name + "' => PSP_THREAD_RUNNING. waitType should be PSP_WAIT_NONE. caller:" + getCallingFunction());
            }
        }
    }

    private void cancelThreadWait(SceKernelThreadInfo thread) {
        // Cancel all waiting actions
        thread.wait.waitingOnEventFlag = false;
        thread.wait.waitingOnIo = false;
        thread.wait.waitingOnMbxReceive = false;
        thread.wait.waitingOnMsgPipeReceive = false;
        thread.wait.waitingOnMsgPipeSend = false;
        thread.wait.waitingOnMutex = false;
        thread.wait.waitingOnLwMutex = false;
        thread.wait.waitingOnSemaphore = false;
        thread.wait.waitingOnThreadEnd = false;
        thread.wait.waitingOnUmd = false;
        thread.wait.waitingOnFpl = false;
        thread.wait.waitingOnVpl = false;
        thread.wait.waitingBlocked = false;
        thread.wait.onUnblockAction = null;
        thread.wait.waitStateChecker = null;
        thread.waitType = PSP_WAIT_NONE;
        if (thread.wait.waitTimeoutAction != null) {
            Scheduler.getInstance().removeAction(thread.wait.microTimeTimeout, thread.wait.waitTimeoutAction);
            thread.wait.waitTimeoutAction = null;
        }
    }

    private void terminateThread(SceKernelThreadInfo thread) {
        hleChangeThreadState(thread, PSP_THREAD_STOPPED);  // PSP_THREAD_STOPPED (checked)
        cancelThreadWait(thread);
        RuntimeContext.onThreadExit(thread);
    }

    private void onThreadStopped(SceKernelThreadInfo stoppedThread) {
        for (SceKernelThreadInfo thread : threadMap.values()) {
            // Wakeup threads that are in sceKernelWaitThreadEnd
            // We're assuming if waitingOnThreadEnd is set then thread.status = waiting
            if (thread.wait.waitingOnThreadEnd &&
                    thread.wait.ThreadEnd_id == stoppedThread.uid) {
                // Untrack
                thread.wait.waitingOnThreadEnd = false;
                // Return exit status of stopped thread
                thread.cpuContext.gpr[2] = stoppedThread.exitStatus;
                // Wakeup
                hleChangeThreadState(thread, PSP_THREAD_READY);
            }
        }
    }

    public void hleKernelExitCallback() {
        hleKernelExitCallback(Emulator.getProcessor());
    }

    private void hleKernelExitCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int callbackId = cpu.gpr[CALLBACKID_REGISTER];
        Callback callback = callbackManager.remove(callbackId);
        if (callback != null) {
            if (log.isTraceEnabled()) {
                log.trace("End of callback " + callback);
            }
            cpu.gpr[CALLBACKID_REGISTER] = callback.getSavedIdRegister();
            cpu.gpr[31] = callback.getSavedRa();
            cpu.pc = callback.getSavedPc();
            IAction afterAction = callback.getAfterAction();
            if (afterAction != null) {
                afterAction.execute();
            }

            // Do we need to restore $v0/$v1?
            if (callback.isReturnVoid()) {
            	cpu.gpr[2] = callback.getSavedV0();
            	cpu.gpr[3] = callback.getSavedV1();
            }
        }
    }

    public final HLEModuleFunction hleKernelExitCallbackFunction = new HLEModuleFunction("ThreadManForUser", "hleKernelExitCallback") {

        @Override
        public final void execute(Processor processor) {
            hleKernelExitCallback(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.hleKernelExitCallback(processor);";
        }
    };

    /**
     * Execute the code at the given address.
     * The code is executed in the context of the currentThread.
     * Parameters ($a0, $a1, ...) may have been copied to the current CpuState
     * before calling this method.
     * This call can return before the completion of the code. Use the
     * "afterAction" parameter to trigger some actions that need to be executed
     * after the code (e.g. to evaluate a return value in $v0).
     *
     * @param address     the address to be called
     * @param afterAction the action to be executed after the completion of the code
     * @param returnVoid  the code has a void return value, i.e. $v0/$v1 have to be restored
     */
    public void callAddress(int address, IAction afterAction, boolean returnVoid) {
        callAddress(null, address, afterAction, returnVoid, null);
    }

    private void callAddress(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid, int[] parameters) {
        if (thread != null) {
            // Save the wait state of the thread to restore it after the call
            int status = thread.status;
            int waitType = thread.waitType;
            int waitId = thread.waitId;
            ThreadWaitInfo threadWaitInfo = new ThreadWaitInfo();
            threadWaitInfo.copy(thread.wait);
            boolean doCallbacks = thread.doCallbacks;

            // Context switch to the requested thread
            thread.waitType = PSP_WAIT_NONE;
            internalContextSwitch(thread);

            afterAction = new AfterCallAction(thread, status, waitType, waitId, threadWaitInfo, doCallbacks, afterAction);
        }

        CpuState cpu = Emulator.getProcessor().cpu;

        // Copy parameters ($a0, $a1, ...) to the cpu
        if (parameters != null) {
            System.arraycopy(parameters, 0, cpu.gpr, 4, parameters.length);
        }

        int callbackId = callbackManager.getNewCallbackId();
        Callback callback = new Callback(callbackId, cpu.gpr[CALLBACKID_REGISTER], cpu.gpr[31], cpu.pc, cpu.gpr[2], cpu.gpr[3], afterAction, returnVoid);
        cpu.gpr[CALLBACKID_REGISTER] = callbackId;
        cpu.gpr[31] = CALLBACK_EXIT_HANDLER_ADDRESS;
        cpu.pc = address;

        callbackManager.addCallback(callback);
        if (thread == null) {
            RuntimeContext.executeCallback();
        } else {
            RuntimeContext.executeCallback(thread);
        }

        // When the compiler is enabled, the callback is already executed
        // when returning from RuntimeContext.executeCallback()
        if (cpu.pc == CALLBACK_EXIT_HANDLER_ADDRESS) {
            hleKernelExitCallback(Emulator.getProcessor());
        }
    }

    /**
     * Trigger a call to a callback in the context of a thread.
     * This call can return before the completion of the callback. Use the
     * "afterAction" parameter to trigger some actions that need to be executed
     * after the callback (e.g. to evaluate a return value in $v0).
     *
     * @param thread      the callback has to be executed by this thread (null means the currentThread)
     * @param address     address of the callback
     * @param afterAction action to be executed after the completion of the callback
     * @param returnVoid  the callback has a void return value, i.e. $v0/$v1 have to be restored
     */
    public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Execute callback 0x%08X, afterAction=%s, returnVoid=%b", address, afterAction, returnVoid));
        }

        callAddress(thread, address, afterAction, returnVoid, null);
    }

    /**
     * Trigger a call to a callback in the context of a thread.
     * This call can return before the completion of the callback. Use the
     * "afterAction" parameter to trigger some actions that need to be executed
     * after the callback (e.g. to evaluate a return value in cpu.gpr[2]).
     *
     * @param thread      the callback has to be executed by this thread (null means the currentThread)
     * @param address     address of the callback
     * @param afterAction action to be executed after the completion of the callback
     * @param returnVoid  the callback has a void return value, i.e. $v0/$v1 have to be restored
     * @param registerA0  first parameter of the callback ($a0)
     */
    public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid, int registerA0) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Execute callback 0x%08X($a0=0x%08X), afterAction=%s, returnVoid=%b", address, registerA0, afterAction, returnVoid));
        }

        callAddress(thread, address, afterAction, returnVoid, new int[]{registerA0});
    }

    /**
     * Trigger a call to a callback in the context of a thread.
     * This call can return before the completion of the callback. Use the
     * "afterAction" parameter to trigger some actions that need to be executed
     * after the callback (e.g. to evaluate a return value in cpu.gpr[2]).
     *
     * @param thread      the callback has to be executed by this thread (null means the currentThread)
     * @param address     address of the callback
     * @param afterAction action to be executed after the completion of the callback
     * @param returnVoid  the callback has a void return value, i.e. $v0/$v1 have to be restored
     * @param registerA0  first parameter of the callback ($a0)
     * @param registerA1  second parameter of the callback ($a1)
     */
    public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid, int registerA0, int registerA1) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Execute callback 0x%08X($a0=0x%08X, $a1=0x%08X), afterAction=%s, returnVoid=%b", address, registerA0, registerA1, afterAction, returnVoid));
        }

        callAddress(thread, address, afterAction, returnVoid, new int[]{registerA0, registerA1});
    }

    /**
     * Trigger a call to a callback in the context of a thread.
     * This call can return before the completion of the callback. Use the
     * "afterAction" parameter to trigger some actions that need to be executed
     * after the callback (e.g. to evaluate a return value in cpu.gpr[2]).
     *
     * @param thread      the callback has to be executed by this thread (null means the currentThread)
     * @param address     address of the callback
     * @param afterAction action to be executed after the completion of the callback
     * @param returnVoid  the callback has a void return value, i.e. $v0/$v1 have to be restored
     * @param registerA0  first parameter of the callback ($a0)
     * @param registerA1  second parameter of the callback ($a1)
     * @param registerA2  third parameter of the callback ($a2)
     */
    public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid, int registerA0, int registerA1, int registerA2) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Execute callback 0x%08X($a0=0x%08X, $a1=0x%08X, $a2=0x%08X), afterAction=%s, returnVoid=%b", address, registerA0, registerA1, registerA2, afterAction, returnVoid));
        }

        callAddress(thread, address, afterAction, returnVoid, new int[]{registerA0, registerA1, registerA2});
    }

    /**
     * Trigger a call to a callback in the context of a thread.
     * This call can return before the completion of the callback. Use the
     * "afterAction" parameter to trigger some actions that need to be executed
     * after the callback (e.g. to evaluate a return value in cpu.gpr[2]).
     *
     * @param thread      the callback has to be executed by this thread (null means the currentThread)
     * @param address     address of the callback
     * @param afterAction action to be executed after the completion of the callback
     * @param returnVoid  the callback has a void return value, i.e. $v0/$v1 have to be restored
     * @param registerA0  first parameter of the callback ($a0)
     * @param registerA1  second parameter of the callback ($a1)
     * @param registerA2  third parameter of the callback ($a2)
     * @param registerA3  fourth parameter of the callback ($a3)
     */
    public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid, int registerA0, int registerA1, int registerA2, int registerA3) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Execute callback 0x%08X($a0=0x%08X, $a1=0x%08X, $a2=0x%08X, $a3=0x%08X), afterAction=%s, returnVoid=%b", address, registerA0, registerA1, registerA2, registerA3, afterAction, returnVoid));
        }

        callAddress(thread, address, afterAction, returnVoid, new int[]{registerA0, registerA1, registerA2, registerA3});
    }

    /**
     * Trigger a call to a callback in the context of a thread.
     * This call can return before the completion of the callback. Use the
     * "afterAction" parameter to trigger some actions that need to be executed
     * after the callback (e.g. to evaluate a return value in cpu.gpr[2]).
     *
     * @param thread      the callback has to be executed by this thread (null means the currentThread)
     * @param address     address of the callback
     * @param afterAction action to be executed after the completion of the callback
     * @param returnVoid  the callback has a void return value, i.e. $v0/$v1 have to be restored
     * @param registerA0  first parameter of the callback ($a0)
     * @param registerA1  second parameter of the callback ($a1)
     * @param registerA2  third parameter of the callback ($a2)
     * @param registerT0  fifth parameter of the callback ($t0)
     */
    public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid, int registerA0, int registerA1, int registerA2, int registerA3, int registerT0) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Execute callback 0x%08X($a0=0x%08X, $a1=0x%08X, $a2=0x%08X, $a3=0x%08X, $t0=0x%08X), afterAction=%s, returnVoid=%b", address, registerA0, registerA1, registerA2, registerA3, registerT0, afterAction, returnVoid));
        }

        callAddress(thread, address, afterAction, returnVoid, new int[]{registerA0, registerA1, registerA2, registerA3, registerT0});
    }

    public void hleKernelExitThread(Processor processor) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Thread exit detected SceUID=%x name='%s' return:0x%08X", currentThread.uid, currentThread.name, processor.cpu.gpr[2]));
        }

        sceKernelExitThread(processor);
    }

    public final HLEModuleFunction hleKernelExitThreadFunction = new HLEModuleFunction("ThreadManForUser", "hleKernelExitThread") {

        @Override
        public final void execute(Processor processor) {
            hleKernelExitThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.hleKernelExitThread(processor);";
        }
    };

    public void hleKernelExitDeleteThread() {
        if (log.isDebugEnabled()) {
            log.debug(String.format("hleKernelExitDeleteThread SceUID=%x name='%s' return:0x%08X", currentThread.uid, currentThread.name, Emulator.getProcessor().cpu.gpr[2]));
        }

        sceKernelExitDeleteThread(Emulator.getProcessor());
    }

    public void hleKernelAsyncLoop(Processor processor) {
        Modules.IoFileMgrForUserModule.hleAsyncThread(processor);
    }
    public final HLEModuleFunction hleKernelAsyncLoopFunction = new HLEModuleFunction("ThreadManForUser", "hleKernelAsyncLoop") {

        @Override
        public final void execute(Processor processor) {
            hleKernelAsyncLoop(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.hleKernelAsyncLoop(processor);";
        }
    };

    /** Note: Some functions allow uid = 0 = current thread, others don't.
     * if uid = 0 then $v0 is set to ERROR_ILLEGAL_THREAD and false is returned
     * if uid < 0 then $v0 is set to ERROR_NOT_FOUND_THREAD and false is returned */
    private boolean checkThreadID(int uid) {
        if (uid == 0) {
            log.warn("checkThreadID illegal thread (uid=0) caller:" + getCallingFunction());
            Emulator.getProcessor().cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_THREAD;
            return false;
        } else if (uid < 0) {
            log.warn("checkThreadID not found thread " + Integer.toHexString(uid) + " (uid<0) caller:" + getCallingFunction());
            Emulator.getProcessor().cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
            return false;
        } else {
            SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
            return true;
        }
    }

    public SceKernelThreadInfo hleKernelCreateThread(String name, int entry_addr,
            int initPriority, int stackSize, int attr, int option_addr) {

        if (option_addr != 0) {
            Modules.log.warn("hleKernelCreateThread unhandled SceKernelThreadOptParam: " +
                    "option_addr=0x" + Integer.toHexString(option_addr));
        }

        SceKernelThreadInfo thread = new SceKernelThreadInfo(name, entry_addr, initPriority, stackSize, attr);
        threadMap.put(thread.uid, thread);

        // inherit module id
        if (currentThread != null) {
            thread.moduleid = currentThread.moduleid;
        }

        if (log.isDebugEnabled()) {
            log.debug("hleKernelCreateThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' PC=" + Integer.toHexString(thread.cpuContext.pc) + " attr:0x" + Integer.toHexString(attr) + " pri:0x" + Integer.toHexString(initPriority) + " stackSize:0x" + Integer.toHexString(stackSize));
        }

        return thread;
    }

    public void setThreadBanningEnabled(boolean enabled) {
        USE_THREAD_BANLIST = enabled;
        log.info("Audio threads disabled: " + USE_THREAD_BANLIST);
    }
    /** use lower case in this list */
    private final String[] threadNameBanList = new String[]{
        "bgm thread", "sgx-psp-freq-thr", "sgx-psp-pcm-th", "ss playthread",
        "spcbgm", "scemainsamplebgmmp3", "se thread"
    };
    /* suspected sound thread names:
     * SndMain, SoundThread, At3Main, Atrac3PlayThread,
     * bgm thread, SceWaveMain, SasCore thread, soundThread,
     * ATRAC3 play thread, SAS Thread, XomAudio, sgx-psp-freq-thr,
     * sgx-psp-at3-th, sgx-psp-pcm-th, sgx-psp-sas-th, snd_tick_timer_thread,
     * snd_stream_service_thread_1, SAS / Main Audio, AudioMixThread,
     * snd_stream_service_thread_0, sound_poll_thread, stream_sound_poll_thread,
     * sndp thread, SndpThread, Ss PlayThread, SndSsThread, SPCBGM,
     * SE Thread, FMOD Software Mixer thread
     *
     * keywords:
     * snd, sound, at3, atrac3, sas, wave, pcm, audio, mpeg, fmod
     *
     * false positives:
     * pcm: SPCMain (Skate Park City Main)
     *
     * ambiguous keywords:
     * bgm, freq, sgx
     */

    private boolean isBannedThread(SceKernelThreadInfo thread) {
        if (USE_THREAD_BANLIST) {
            String name = thread.name.toLowerCase();
            if (name.contains("snd") || name.contains("sound") ||
                    name.contains("at3") || name.contains("atrac") ||
                    name.contains("sas") || name.contains("wave") ||
                    name.contains("audio") || name.contains("mpeg") ||
                    name.contains("fmod") || name.contains("mp3")) {
                return true;
            }

            for (String threadName : threadNameBanList) {
                if (name.equals(threadName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void hleKernelStartThread(SceKernelThreadInfo thread, int userDataLength, int userDataAddr, int gp) {
        if (log.isDebugEnabled()) {
            log.debug("hleKernelStartThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' dataLen=0x" + Integer.toHexString(userDataLength) + " data=0x" + Integer.toHexString(userDataAddr) + " gp=0x" + Integer.toHexString(gp));
        }
        // Reset all thread parameters: a thread can be restarted when it has exited.
        thread.reset();
        // Setup args by copying them onto the stack
        //int address = thread.cpuContext.gpr[29];
        // 256 bytes padding between user data top and real stack top
        int address = (thread.stack_addr + thread.stackSize - 0x100) - ((userDataLength + 0xF) & ~0xF);
        if (userDataAddr == 0) {
            // Set the pointer to NULL when none is provided
            thread.cpuContext.gpr[4] = 0; // a0 = user data len
            thread.cpuContext.gpr[5] = 0; // a1 = pointer to arg data in stack
        } else {
            Memory.getInstance().memcpy(address, userDataAddr, userDataLength);
            thread.cpuContext.gpr[4] = userDataLength; // a0 = user data len
            thread.cpuContext.gpr[5] = address; // a1 = pointer to arg data in stack
        }
        // 64 bytes padding between program stack top and user data
        thread.cpuContext.gpr[29] = address - 0x40;
        // testing
        if (log.isDebugEnabled() && thread.cpuContext.gpr[28] != gp) {
            // from sceKernelStartModule is ok, anywhere else might be an error
            log.debug("hleKernelStartThread oldGP=0x" + Integer.toHexString(thread.cpuContext.gpr[28]) + " newGP=0x" + Integer.toHexString(gp));
        }
        thread.cpuContext.gpr[28] = gp;
        // switch in the target thread if it's higher priority
        hleChangeThreadState(thread, PSP_THREAD_READY);
        if (thread.currentPriority < currentThread.currentPriority) {
            if (log.isDebugEnabled()) {
                log.debug("hleKernelStartThread switching in thread immediately");
            }
            hleRescheduleCurrentThread();
        }
    }

    public void hleKernelSleepThread(boolean doCallbacks) {
        if (currentThread.wakeupCount > 0) {
            // sceKernelWakeupThread() has been called before, do not sleep
            currentThread.wakeupCount--;
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            // Go to wait state
            // wait type
            currentThread.waitType = PSP_WAIT_SLEEP;
            // Wait forever (another thread will call sceKernelWakeupThread)
            hleKernelThreadWait(currentThread, 0, true);
            hleChangeThreadState(currentThread, PSP_THREAD_WAITING);
            Emulator.getProcessor().cpu.gpr[2] = 0;
            hleRescheduleCurrentThread(doCallbacks);
        }
    }

    public void hleKernelWakeupThread(SceKernelThreadInfo thread) {
        if (thread.status != PSP_THREAD_WAITING || thread.waitType != PSP_WAIT_SLEEP) {
            thread.wakeupCount++;
            if (log.isDebugEnabled()) {
            	log.debug("sceKernelWakeupThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' not sleeping/waiting (status=0x" + Integer.toHexString(thread.status) + "), incrementing wakeupCount to " + thread.wakeupCount);
            }
        } else if (isBannedThread(thread)) {
            log.warn("sceKernelWakeupThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' banned, not waking up");
        } else {
            if (log.isDebugEnabled()) {
                log.debug("sceKernelWakeupThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");
            }
            hleChangeThreadState(thread, PSP_THREAD_READY);
            // switch in the target thread if it's now higher priority
            if (thread.currentPriority < currentThread.currentPriority) {
                if (log.isDebugEnabled()) {
                    log.debug("sceKernelWakeupThread yielding to thread with higher priority");
                }
                hleRescheduleCurrentThread();
            }
        }
    }

    private void hleKernelWaitThreadEnd(int uid, int micros, boolean forever, boolean callbacks) {
        if (!checkThreadID(uid)) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("hleKernelWaitThreadEnd SceUID=" + Integer.toHexString(uid) + " micros=" + micros + " forever=" + forever + " callbacks=" + callbacks);
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn("hleKernelWaitThreadEnd unknown thread 0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else if (isBannedThread(thread)) {
            log.warn("hleKernelWaitThreadEnd SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "' banned, not waiting");
            Emulator.getProcessor().cpu.gpr[2] = 0;
            hleRescheduleCurrentThread();
        } else if (thread.status == PSP_THREAD_STOPPED) {
            log.debug("hleKernelWaitThreadEnd SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "' thread already stopped, not waiting");
            Emulator.getProcessor().cpu.gpr[2] = 0;
            hleRescheduleCurrentThread();
        } else {
            // wait type
            currentThread.waitType = PSP_WAIT_THREAD_END;
            // Go to wait state
            hleKernelThreadWait(currentThread, micros, forever);
            // Wait on a specific thread end
            currentThread.wait.waitingOnThreadEnd = true;
            currentThread.wait.ThreadEnd_id = uid;
            hleChangeThreadState(currentThread, PSP_THREAD_WAITING);
            hleRescheduleCurrentThread(callbacks);
        }
    }

    /**
     * Set the wait timeout for a thread. The state of the thread is not changed.
     *
     * @param thread  the thread
     * @param wait    the same as thread.wait
     * @param micros  the timeout in microseconds (this is an unsigned value: SceUInt32)
     * @param forever true if the thread has to wait forever (micros in then ignored)
     */
    public void hleKernelThreadWait(SceKernelThreadInfo thread, int micros, boolean forever) {
        thread.wait.forever = forever;
        thread.wait.micros = micros; // for debugging
        if (forever) {
            thread.wait.microTimeTimeout = 0;
            thread.wait.waitTimeoutAction = null;
        } else {
            long longMicros = ((long) micros) & 0xFFFFFFFFL;
            thread.wait.microTimeTimeout = Emulator.getClock().microTime() + longMicros;
            thread.wait.waitTimeoutAction = new TimeoutThreadAction(thread);
        }

        if (LOG_CONTEXT_SWITCHING && Modules.log.isDebugEnabled() && !isIdleThread(thread)) {
            log.debug("-------------------- hleKernelThreadWait micros=" + micros + " forever:" + forever + " thread:'" + thread.name + "' caller:" + getCallingFunction());
        }
    }

    public void hleKernelDelayThread(int micros, boolean doCallbacks) {
        // wait type
        currentThread.waitType = PSP_WAIT_DELAY;

        if (micros < THREAD_DELAY_MINIMUM_MICROS) {
        	micros = THREAD_DELAY_MINIMUM_MICROS;
        }

        if (log.isDebugEnabled()) {
            log.debug("hleKernelDelayThread micros=" + micros + ", callbacks=" + doCallbacks);
        }

        // Wait on a timeout only
        hleKernelThreadWait(currentThread, micros, false);
        hleChangeThreadState(currentThread, PSP_THREAD_WAITING);
        hleRescheduleCurrentThread(doCallbacks);
    }

    public SceKernelCallbackInfo hleKernelCreateCallback(String name, int func_addr, int user_arg_addr) {
        SceKernelCallbackInfo callback = new SceKernelCallbackInfo(name, currentThread.uid, func_addr, user_arg_addr);
        callbackMap.put(callback.uid, callback);
        log.debug("hleKernelCreateCallback SceUID=" + Integer.toHexString(callback.uid) + " name:'" + name + "' PC=" + Integer.toHexString(func_addr) + " arg=" + Integer.toHexString(user_arg_addr) + " thread:'" + currentThread.name + "'");
        return callback;
    }

    /** @return true if successful. */
    public boolean hleKernelDeleteCallback(int uid) {
        SceKernelCallbackInfo info = callbackMap.remove(uid);
        boolean removed = info != null;
        if (removed) {
            if (log.isDebugEnabled()) {
                log.debug("hleKernelDeleteCallback SceUID=" + Integer.toHexString(uid) + " name:'" + info.name + "'");
            }
        } else {
            log.warn("hleKernelDeleteCallback not a callback uid 0x" + Integer.toHexString(uid));
        }

        return removed;
    }

    protected int getThreadCurrentStackSize() {
        int size = currentThread.stackSize - (Emulator.getProcessor().cpu.gpr[29] - currentThread.stack_addr) - 0x130;
        if (size < 0) {
            size = 0;
        }

        return size;
    }

    protected int setThreadCurrentStackSize(int size) {
        if(size > 0) {
            currentThread.stackSize += size;
        }
        return getThreadCurrentStackSize();
    }

    private boolean threadCanNotCallback(SceKernelThreadInfo thread, int callbackType, int cbid, SceKernelCallbackInfo cb) {
        return !thread.callbackRegistered[callbackType] || (cbid != -1 && cb.uid != cbid);
    }

    private boolean userCurrentThreadTryingToSwitchToKernelMode(int newAttr) {
        return (currentThread.attr & PSP_THREAD_ATTR_USER) == PSP_THREAD_ATTR_USER && (newAttr & PSP_THREAD_ATTR_USER) != PSP_THREAD_ATTR_USER;
    }

    private boolean userThreadCalledKernelCurrentThread(SceKernelThreadInfo thread) {
        return !isIdleThread(thread) &&
                ((thread.attr & PSP_THREAD_ATTR_KERNEL) != PSP_THREAD_ATTR_KERNEL ||
                (currentThread.attr & PSP_THREAD_ATTR_KERNEL) == PSP_THREAD_ATTR_KERNEL);
    }

    private int getDispatchThreadState() {
        return dispatchThreadEnabled ? SCE_KERNEL_DISPATCHTHREAD_STATE_ENABLED : SCE_KERNEL_DISPATCHTHREAD_STATE_DISABLED;
    }

    /** Registers a callback on the thread that created the callback.
     * @return true on success (the cbid was a valid callback uid) */
    public boolean hleKernelRegisterCallback(int callbackType, int cbid) {
        SceKernelCallbackInfo callback = callbackMap.get(cbid);
        if (callback == null) {
            log.warn("hleKernelRegisterCallback(type=" + callbackType + ") unknown uid " + Integer.toHexString(cbid));
            return false;
        }

        SceKernelThreadInfo thread = getThreadById(callback.threadId);
        // Consistency check
        if (thread.callbackReady[callbackType]) {
            log.warn("hleKernelRegisterCallback(type=" + callbackType + ") ready=true");
        }

        thread.callbackRegistered[callbackType] = true;
        thread.callbackInfo[callbackType] = callback;
        return true;
    }

    /** Unregisters a callback by type and cbid. May not be on the current thread.
     * @param callbackType See SceKernelThreadInfo.
     * @param cbid The UID of the callback to unregister.
     * @return SceKernelCallbackInfo of the removed callback, or null if it
     * couldn't be found. */
    public SceKernelCallbackInfo hleKernelUnRegisterCallback(int callbackType, int cbid) {
        SceKernelCallbackInfo oldInfo = null;

        for (SceKernelThreadInfo thread : threadMap.values()) {
            if (thread.callbackRegistered[callbackType] &&
                    thread.callbackInfo[callbackType].uid == cbid) {
                // Warn if we are removing a pending callback, this a callback
                // that has been pushed but not yet executed.
                if (thread.callbackReady[callbackType]) {
                    log.warn("hleKernelUnRegisterCallback(type=" + callbackType + ") removing pending callback");
                }

                oldInfo = thread.callbackInfo[callbackType];
                thread.callbackRegistered[callbackType] = false;
                thread.callbackReady[callbackType] = false;
                thread.callbackInfo[callbackType] = null;
                break;
            }
        }

        if (oldInfo == null) {
            log.warn("hleKernelUnRegisterCallback(type=" + callbackType + ") cbid=" + Integer.toHexString(cbid) + " no matching callbacks found");
        }

        return oldInfo;
    }

    /** push callback to all threads */
    public void hleKernelNotifyCallback(int callbackType, int notifyArg) {
        hleKernelNotifyCallback(callbackType, -1, notifyArg);
    }

    /** @param cbid If cbid is -1, then push callback to all threads
     * if cbid is not -1 then only trigger that specific cbid provided it is
     * also of type callbackType. */
    public void hleKernelNotifyCallback(int callbackType, int cbid, int notifyArg) {
        boolean pushed = false;

        for (SceKernelThreadInfo thread : threadMap.values()) {
            SceKernelCallbackInfo cb = thread.callbackInfo[callbackType];

            if (threadCanNotCallback(thread, callbackType, cbid, cb)) {
                continue;
            }

            if (cb.notifyCount != 0) {
                log.warn("hleKernelNotifyCallback(type=" + callbackType + ") thread:'" + thread.name + "' overwriting previous notifyArg 0x" + Integer.toHexString(cb.notifyArg) + " -> 0x" + Integer.toHexString(notifyArg) + ", newCount=" + (cb.notifyCount + 1));
            }

            cb.notifyCount++; // keep increasing this until we actually enter the callback
            cb.notifyArg = notifyArg;
            thread.callbackReady[callbackType] = true;
            pushed = true;
        }

        if (pushed) {
            // Enter callbacks immediately,
            // except those registered to the current thread. The app must explictly
            // call sceKernelCheckCallback or a waitCB function to do that.
            if (log.isDebugEnabled()) {
                log.debug("hleKernelNotifyCallback(type=" + callbackType + ") calling checkCallbacks");
            }
            checkCallbacks();
        } else {
            log.warn("hleKernelNotifyCallback(type=" + callbackType + ") no registered callbacks to push");
        }
    }

    /** runs callbacks. Check the thread doCallbacks flag.
     * @return true if we switched into a callback. */
    private boolean checkThreadCallbacks(SceKernelThreadInfo thread) {
        boolean handled = false;

        if (thread == null || !thread.doCallbacks) {
            return handled;
        }

        for (int i = 0; i < SceKernelThreadInfo.THREAD_CALLBACK_SIZE; i++) {
            if (thread.callbackReady[i] && thread.callbackRegistered[i]) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Entering callback type %d %s for thread %s (current thread is %s)", i, thread.callbackInfo[i].toString(), thread.toString(), currentThread.toString()));
                }

                thread.callbackReady[i] = false;
                thread.callbackInfo[i].startContext(thread, null);
                handled = true;
                break;
            }
        }

        return handled;
    }

    public void cancelAlarm(SceKernelAlarmInfo sceKernelAlarmInfo) {
        Scheduler.getInstance().removeAction(sceKernelAlarmInfo.schedule, sceKernelAlarmInfo.alarmInterruptAction);
        sceKernelAlarmInfo.schedule = 0;
    }

    public void rescheduleAlarm(SceKernelAlarmInfo sceKernelAlarmInfo, int delay) {
        if (delay < 0) {
            delay = 100;
        }

        sceKernelAlarmInfo.schedule += delay;
        scheduleAlarm(sceKernelAlarmInfo);

        if (log.isDebugEnabled()) {
            log.debug(String.format("New Schedule for Alarm uid=%x: %d", sceKernelAlarmInfo.uid, sceKernelAlarmInfo.schedule));
        }
    }

    private void scheduleAlarm(SceKernelAlarmInfo sceKernelAlarmInfo) {
        Scheduler.getInstance().addAction(sceKernelAlarmInfo.schedule, sceKernelAlarmInfo.alarmInterruptAction);
    }

    protected void hleKernelSetAlarm(Processor processor, long delayUsec, int handlerAddress, int handlerArgument) {
        CpuState cpu = processor.cpu;

        long now = Scheduler.getNow();
        long schedule = now + delayUsec;
        SceKernelAlarmInfo sceKernelAlarmInfo = new SceKernelAlarmInfo(schedule, handlerAddress, handlerArgument);
        alarms.put(sceKernelAlarmInfo.uid, sceKernelAlarmInfo);

        scheduleAlarm(sceKernelAlarmInfo);

        cpu.gpr[2] = sceKernelAlarmInfo.uid;
    }

    protected long getSystemTime() {
        return SystemTimeManager.getSystemTime();
    }

    public long getVTimerTime(SceKernelVTimerInfo sceKernelVTimerInfo) {
        long time = sceKernelVTimerInfo.current;

        if (sceKernelVTimerInfo.active == SceKernelVTimerInfo.ACTIVE_RUNNING) {
            time += getSystemTime() - sceKernelVTimerInfo.base;
        }

        return time;
    }

    protected long getVTimerScheduleForScheduler(SceKernelVTimerInfo sceKernelVTimerInfo) {
        return sceKernelVTimerInfo.base + sceKernelVTimerInfo.schedule;
    }

    protected void setVTimer(SceKernelVTimerInfo sceKernelVTimerInfo, long time) {
        sceKernelVTimerInfo.current = time;
    }

    protected void startVTimer(SceKernelVTimerInfo sceKernelVTimerInfo) {
        sceKernelVTimerInfo.active = SceKernelVTimerInfo.ACTIVE_RUNNING;
        sceKernelVTimerInfo.base = getSystemTime();

        if (sceKernelVTimerInfo.schedule != 0 && sceKernelVTimerInfo.handlerAddress != 0) {
            scheduleVTimer(sceKernelVTimerInfo, sceKernelVTimerInfo.schedule);
        }
    }

    protected void stopVTimer(SceKernelVTimerInfo sceKernelVTimerInfo) {
        sceKernelVTimerInfo.active = SceKernelVTimerInfo.ACTIVE_STOPPED;
        // Sum the elapsed time (multiple Start/Stop sequences are added)
        sceKernelVTimerInfo.current += getSystemTime() - sceKernelVTimerInfo.base;
    }

    protected void scheduleVTimer(SceKernelVTimerInfo sceKernelVTimerInfo, long schedule) {
        sceKernelVTimerInfo.schedule = schedule;

        if (sceKernelVTimerInfo.active == SceKernelVTimerInfo.ACTIVE_RUNNING) {
            Scheduler scheduler = Scheduler.getInstance();
            scheduler.addAction(getVTimerScheduleForScheduler(sceKernelVTimerInfo), sceKernelVTimerInfo.vtimerInterruptAction);
        }
    }

    public void cancelVTimer(SceKernelVTimerInfo sceKernelVTimerInfo) {
        Scheduler.getInstance().removeAction(getVTimerScheduleForScheduler(sceKernelVTimerInfo), sceKernelVTimerInfo.vtimerInterruptAction);
        sceKernelVTimerInfo.schedule = 0;
        sceKernelVTimerInfo.handlerAddress = 0;
        sceKernelVTimerInfo.handlerArgument = 0;
    }

    public void rescheduleVTimer(SceKernelVTimerInfo sceKernelVTimerInfo, int delay) {
        if (delay < 0) {
            delay = 100;
        }

        sceKernelVTimerInfo.schedule += delay;

        scheduleVTimer(sceKernelVTimerInfo, sceKernelVTimerInfo.schedule);

        if (log.isDebugEnabled()) {
            log.debug(String.format("New Schedule for VTimer uid=%x: %d", sceKernelVTimerInfo.uid, sceKernelVTimerInfo.schedule));
        }
    }

    /**
     * Iterates waiting threads, making sure doCallbacks is set before
     * checking for pending callbacks.
     * Handles sceKernelCheckCallback when doCallbacks is set on currentThread.
     * Handles redirects to yieldCB (from fake waitCB) on the thread that called waitCB.
     *
     * We currently call checkCallbacks() at the end of each waitCB function
     * since this has less overhead than checking on every step.
     *
     * Some trickery is used in yieldCurrentThreadCB(). By the time we get
     * inside the checkCallbacks() function the thread that called yieldCB is
     * no longer the current thread. Also the thread that called yieldCB is
     * not in the wait state (it's in the ready state). so what we do is check
     * every thread, not just the waiting threads for the doCallbacks flag.
     * Also the waitingThreads list only contains waiting threads that have a
     * finite wait period, so we have to iterate on all threads anyway.
     *
     * It is probably unsafe to call contextSwitch() when insideCallback is true.
     * insideCallback may become true after a call to checkCallbacks().
     */
    private void checkCallbacks() {
        if (log.isTraceEnabled()) {
            log.trace("checkCallbacks current thread is '" + currentThread.name + "' doCallbacks:" + currentThread.doCallbacks + " caller:" + getCallingFunction());
        }

        boolean handled;
        do {
            handled = false;
            for (SceKernelThreadInfo thread : threadMap.values()) {
                if (thread.doCallbacks && checkThreadCallbacks(thread)) {
                    handled = true;
                    break;
                }
            }
        } while (handled);
    }

    public void _sceKernelReturnFromCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented _sceKernelReturnFromCallback");

        cpu.gpr[2] = 0;
    }

    public void sceKernelRegisterThreadEventHandler(Processor processor) {
        CpuState cpu = processor.cpu;

        int name_addr = cpu.gpr[4];
        int thid = cpu.gpr[5];
        int mask = cpu.gpr[6];
        int handler_func = cpu.gpr[7];
        int common_addr = cpu.gpr[8];

        String name = readStringNZ(name_addr, 32);

        if (log.isDebugEnabled()) {
            log.debug("sceKernelRegisterThreadEventHandler name=" + name + ", thid=0x" + Integer.toHexString(thid) + ", mask=0x" + Integer.toHexString(mask) + ", handler_func=0x" + Integer.toHexString(handler_func) + ", common_addr=0x" + Integer.toHexString(common_addr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (threadMap.containsKey(thid)) {
            SceKernelThreadEventHandlerInfo handler = new SceKernelThreadEventHandlerInfo(name, thid, mask, handler_func, common_addr);
            threadEventHandlerMap.put(handler.uid, handler);
            threadEventMap.put(thid, handler.uid);
            cpu.gpr[2] = handler.uid;
        } else {
            SceKernelThreadEventHandlerInfo handler = new SceKernelThreadEventHandlerInfo(name, thid, mask, handler_func, common_addr);
            if (thid == SceKernelThreadEventHandlerInfo.THREAD_EVENT_ID_CURRENT) {
                threadEventHandlerMap.put(handler.uid, handler);
                threadEventMap.put(getCurrentThread().uid, handler.uid);
                cpu.gpr[2] = handler.uid;
            } else if (thid == SceKernelThreadEventHandlerInfo.THREAD_EVENT_ID_USER) {
                threadEventHandlerMap.put(handler.uid, handler);
                for (SceKernelThreadInfo thread : threadMap.values()) {
                    if ((thread.attr & PSP_THREAD_ATTR_USER) == PSP_THREAD_ATTR_USER) {
                        threadEventMap.put(thread.uid, handler.uid);
                    }
                }
                cpu.gpr[2] = handler.uid;
            } else if (thid == SceKernelThreadEventHandlerInfo.THREAD_EVENT_ID_KERN && isKernelMode()) {
                threadEventHandlerMap.put(handler.uid, handler);
                for (SceKernelThreadInfo thread : threadMap.values()) {
                    if ((thread.attr & PSP_THREAD_ATTR_KERNEL) == PSP_THREAD_ATTR_KERNEL) {
                        threadEventMap.put(thread.uid, handler.uid);
                    }
                }
                cpu.gpr[2] = handler.uid;
            } else if (thid == SceKernelThreadEventHandlerInfo.THREAD_EVENT_ID_ALL && isKernelMode()) {
                threadEventHandlerMap.put(handler.uid, handler);
                for (SceKernelThreadInfo thread : threadMap.values()) {
                    threadEventMap.put(thread.uid, handler.uid);
                }
                cpu.gpr[2] = handler.uid;
            } else {
                cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
            }
        }
    }

    public void sceKernelReleaseThreadEventHandler(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelReleaseThreadEventHandler uid=0x" + Integer.toHexString(uid));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (threadEventHandlerMap.containsKey(uid)) {
            threadEventHandlerMap.remove(uid);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD_EVENT_HANDLER;
        }
    }

    public void sceKernelReferThreadEventHandlerStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int uid = cpu.gpr[4];
        int status_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferThreadEventHandlerStatus uid=0x" + Integer.toHexString(uid) + ", status_addr=0x" + Integer.toHexString(status_addr));
        }

        if (threadEventHandlerMap.containsKey(uid)) {
            threadEventHandlerMap.get(uid).write(mem, status_addr);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD_EVENT_HANDLER;
        }
    }

    public void sceKernelCreateCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int name_addr = cpu.gpr[4];
        int func_addr = cpu.gpr[5];
        int user_arg_addr = cpu.gpr[6];
        String name = readStringNZ(name_addr, 32);

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceKernelCallbackInfo callback = hleKernelCreateCallback(name, func_addr, user_arg_addr);
        cpu.gpr[2] = callback.uid;
    }

    public void sceKernelDeleteCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (hleKernelDeleteCallback(uid)) {
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_NOT_FOUND_CALLBACK;
        }
    }

    /**
     * Manually notifies a callback. Mostly used for exit callbacks,
     * and shouldn't be used at all (only some old homebrews use this, anyway).
     */
    public void sceKernelNotifyCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        int arg = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelNotifyCallback uid=0x" + Integer.toHexString(uid) + ", arg=0x" + Integer.toHexString(arg));
        }
        SceKernelCallbackInfo callback = callbackMap.get(uid);
        if (callback != null) {
        	boolean foundCallback = false;
            for(int i = 0; i < SceKernelThreadInfo.THREAD_CALLBACK_SIZE; i++) {
                if(getCurrentThread().callbackInfo[i] != null) {
                    if(getCurrentThread().callbackInfo[i].uid == callback.uid) {
                        hleKernelNotifyCallback(i, uid, arg);
                        foundCallback = true;
                        break;
                    }
                }
            }
            if (!foundCallback) {
            	// Register the callback as a temporary THREAD_CALLBACK_USER_DEFINED
            	if (hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_USER_DEFINED, uid)) {
            		hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_USER_DEFINED, uid, arg);
            	}
            }
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_NOT_FOUND_CALLBACK;
        }
    }

    public void sceKernelCancelCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelCancelCallback uid=0x" + Integer.toHexString(uid));
        }

        SceKernelCallbackInfo callback = callbackMap.get(uid);
        if (callback != null) {
            callback.notifyArg = 0;
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_NOT_FOUND_CALLBACK;
        }

    }

    /** Return the current notifyCount for a specific callback */
    public void sceKernelGetCallbackCount(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelGetCallbackCount uid=0x" + Integer.toHexString(uid));
        }
        SceKernelCallbackInfo callback = callbackMap.get(uid);
        if (callback != null) {
            cpu.gpr[2] = callback.notifyCount;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_NOT_FOUND_CALLBACK;
        }
    }

    /** Check callbacks, only on the current thread. */
    public void sceKernelCheckCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelCheckCallback(void)");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // 0 - The calling thread has no reported callbacks.
        // 1 - The calling thread has reported callbacks which were executed sucessfully.
        currentThread.doCallbacks = true;  // Callbacks are always allowed here.
        cpu.gpr[2] = checkThreadCallbacks(currentThread) ? 1 : 0;
        currentThread.doCallbacks = false; // Callbacks may not be allowed after this.
    }

    public void sceKernelReferCallbackStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int uid = cpu.gpr[4];
        int info_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferCallbackStatus SceUID=" + Integer.toHexString(uid) + " info=" + Integer.toHexString(info_addr));
        }

        SceKernelCallbackInfo info = callbackMap.get(uid);
        if (info == null) {
            log.warn("sceKernelReferCallbackStatus unknown uid 0x" + Integer.toHexString(uid));
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_NOT_FOUND_CALLBACK;
        } else if (!Memory.isAddressGood(info_addr)) {
            log.warn("sceKernelReferCallbackStatus bad info address 0x" + Integer.toHexString(info_addr));
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_ILLEGAL_ADDR;
        } else {
            int size = mem.read32(info_addr);
            if (size == SceKernelCallbackInfo.size) {
                info.write(mem, info_addr);
                cpu.gpr[2] = 0;
            } else {
                log.warn("sceKernelReferCallbackStatus bad info size got " + size + " want " + SceKernelCallbackInfo.size);
                cpu.gpr[2] = -1;
            }
        }
    }

    /** sleep the current thread (using wait) */
    public void sceKernelSleepThread(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelSleepThread SceUID=" + Integer.toHexString(currentThread.uid) + " name:'" + currentThread.name + "'");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        hleKernelSleepThread(false);
    }

    /** sleep the current thread and handle callbacks (using wait)
     * in our implementation we have to use wait, not suspend otherwise we don't handle callbacks. */
    public void sceKernelSleepThreadCB(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelSleepThreadCB SceUID=" + Integer.toHexString(currentThread.uid) + " name:'" + currentThread.name + "'");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        hleKernelSleepThread(true);
        checkCallbacks();
    }

    public void sceKernelWakeupThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (!checkThreadID(uid)) {
            return;
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " unknown thread");
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else {
            cpu.gpr[2] = 0;
            hleKernelWakeupThread(thread);
        }
    }

    public void sceKernelCancelWakeupThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (uid == 0) {
            uid = currentThread.uid;
        }
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn("sceKernelCancelWakeupThread SceUID=" + Integer.toHexString(uid) + ") unknown thread");
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("sceKernelCancelWakeupThread SceUID=" + Integer.toHexString(uid) + ") wakeupCount=" + thread.wakeupCount);
            }
            cpu.gpr[2] = thread.wakeupCount;
            thread.wakeupCount = 0;
        }
    }

    public void sceKernelSuspendThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (!checkThreadID(uid)) {
            return;
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn("sceKernelSuspendThread SceUID=" + Integer.toHexString(uid) + " unknown thread");
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else if (uid == currentThread.uid) {
            log.warn("sceKernelSuspendThread on self is not allowed");
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_THREAD;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("sceKernelSuspendThread SceUID=" + Integer.toHexString(uid));
            }
            if (thread.status == PSP_THREAD_WAITING) {
            	hleChangeThreadState(thread, PSP_THREAD_WAITING_SUSPEND);
            } else {
            	hleChangeThreadState(thread, PSP_THREAD_SUSPEND);
            }
            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelResumeThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (!checkThreadID(uid)) {
            return;
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn("sceKernelResumeThread SceUID=" + Integer.toHexString(uid) + " unknown thread");
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else if (thread.status != PSP_THREAD_SUSPEND && thread.status != PSP_THREAD_WAITING_SUSPEND) {
            log.warn("sceKernelResumeThread SceUID=" + Integer.toHexString(uid) + " not suspended (status=" + thread.status + ")");
            cpu.gpr[2] = ERROR_KERNEL_THREAD_IS_NOT_SUSPEND;
        } else if (isBannedThread(thread)) {
            log.warn("sceKernelResumeThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "' banned, not resuming");
            cpu.gpr[2] = 0;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("sceKernelResumeThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "'");
            }
            if (thread.status == PSP_THREAD_WAITING_SUSPEND) {
                hleChangeThreadState(thread, PSP_THREAD_WAITING);
            } else {
            	hleChangeThreadState(thread, PSP_THREAD_READY);
            }
            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelWaitThreadEnd(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        int timeout_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelWaitThreadEnd redirecting to hleKernelWaitThreadEnd(callbacks=false)");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int micros = 0;
        boolean forever = true;

        if (timeout_addr != 0) {
            micros = Memory.getInstance().read32(timeout_addr);
            forever = false;
        }
        hleKernelWaitThreadEnd(uid, micros, forever, false);
    }

    public void sceKernelWaitThreadEndCB(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        int timeout_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelWaitThreadEndCB redirecting to hleKernelWaitThreadEnd(callbacks=true)");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int micros = 0;
        boolean forever = true;

        if (timeout_addr != 0) {
            micros = Memory.getInstance().read32(timeout_addr);
            forever = false;
        }
        hleKernelWaitThreadEnd(uid, micros, forever, true);
        checkCallbacks();
    }

    /** wait the current thread for a certain number of microseconds */
    public void sceKernelDelayThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int micros = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        hleKernelDelayThread(micros, false);
    }

    /** wait the current thread for a certain number of microseconds */
    public void sceKernelDelayThreadCB(Processor processor) {
        CpuState cpu = processor.cpu;

        int micros = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        hleKernelDelayThread(micros, true);
    }

    /**
     * Delay the current thread by a specified number of sysclocks
     *
     * @param sysclocks_addr - Address of delay in sysclocks
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelDelaySysClockThread(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int sysclocks_addr = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (Memory.isAddressGood(sysclocks_addr)) {
            long sysclocks = mem.read64(sysclocks_addr);
            int micros = SystemTimeManager.hleSysClock2USec32(sysclocks);
            hleKernelDelayThread(micros, false);
        } else {
            log.warn("sceKernelDelaySysClockThread invalid sysclocks address 0x" + Integer.toHexString(sysclocks_addr));
            cpu.gpr[2] = -1;
        }
    }

    /**
     * Delay the current thread by a specified number of sysclocks handling callbacks
     *
     * @param sysclocks_addr - Address of delay in sysclocks
     *
     * @return 0 on success, < 0 on error
     *
     */
    public void sceKernelDelaySysClockThreadCB(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int sysclocks_addr = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (Memory.isAddressGood(sysclocks_addr)) {
            long sysclocks = mem.read64(sysclocks_addr);
            int micros = SystemTimeManager.hleSysClock2USec32(sysclocks);
            hleKernelDelayThread(micros, true);
        } else {
            log.warn("sceKernelDelaySysClockThreadCB invalid sysclocks address 0x" + Integer.toHexString(sysclocks_addr));
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }
    }

    public void sceKernelCreateSema(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.semas.sceKernelCreateSema(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]);
    }

    public void sceKernelDeleteSema(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.semas.sceKernelDeleteSema(cpu.gpr[4]);
    }

    public void sceKernelSignalSema(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.semas.sceKernelSignalSema(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelWaitSema(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.semas.sceKernelWaitSema(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    public void sceKernelWaitSemaCB(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.semas.sceKernelWaitSemaCB(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    public void sceKernelPollSema(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.semas.sceKernelPollSema(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelCancelSema(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.semas.sceKernelCancelSema(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    public void sceKernelReferSemaStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.semas.sceKernelReferSemaStatus(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelCreateEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.eventFlags.sceKernelCreateEventFlag(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7]);
    }

    public void sceKernelDeleteEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.eventFlags.sceKernelDeleteEventFlag(cpu.gpr[4]);
    }

    public void sceKernelSetEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.eventFlags.sceKernelSetEventFlag(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelClearEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.eventFlags.sceKernelClearEventFlag(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelWaitEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.eventFlags.sceKernelWaitEventFlag(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]);
    }

    public void sceKernelWaitEventFlagCB(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.eventFlags.sceKernelWaitEventFlagCB(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]);
    }

    public void sceKernelPollEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.eventFlags.sceKernelPollEventFlag(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7]);
    }

    public void sceKernelCancelEventFlag(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.eventFlags.sceKernelCancelEventFlag(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    public void sceKernelReferEventFlagStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.eventFlags.sceKernelReferEventFlagStatus(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelCreateMbx(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.mbx.sceKernelCreateMbx(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    public void sceKernelDeleteMbx(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.mbx.sceKernelDeleteMbx(cpu.gpr[4]);
    }

    public void sceKernelSendMbx(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.mbx.sceKernelSendMbx(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelReceiveMbx(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.mbx.sceKernelReceiveMbx(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    public void sceKernelReceiveMbxCB(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.mbx.sceKernelReceiveMbxCB(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    public void sceKernelPollMbx(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.mbx.sceKernelPollMbx(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelCancelReceiveMbx(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.mbx.sceKernelCancelReceiveMbx(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelReferMbxStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.mbx.sceKernelReferMbxStatus(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelCreateMsgPipe(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.msgPipes.sceKernelCreateMsgPipe(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]);
    }

    public void sceKernelDeleteMsgPipe(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.msgPipes.sceKernelDeleteMsgPipe(cpu.gpr[4]);
    }

    public void sceKernelSendMsgPipe(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.msgPipes.sceKernelSendMsgPipe(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]);
    }

    public void sceKernelSendMsgPipeCB(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.msgPipes.sceKernelSendMsgPipeCB(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]);
    }

    public void sceKernelTrySendMsgPipe(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.msgPipes.sceKernelTrySendMsgPipe(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]);
    }

    public void sceKernelReceiveMsgPipe(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.msgPipes.sceKernelReceiveMsgPipe(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]);
    }

    public void sceKernelReceiveMsgPipeCB(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.msgPipes.sceKernelReceiveMsgPipeCB(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]);
    }

    public void sceKernelTryReceiveMsgPipe(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.msgPipes.sceKernelTryReceiveMsgPipe(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]);
    }

    public void sceKernelCancelMsgPipe(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.msgPipes.sceKernelCancelMsgPipe(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    public void sceKernelReferMsgPipeStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.msgPipes.sceKernelReferMsgPipeStatus(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelCreateVpl(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.vpl.sceKernelCreateVpl(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]);
    }

    public void sceKernelDeleteVpl(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.vpl.sceKernelDeleteVpl(cpu.gpr[4]);
    }

    public void sceKernelAllocateVpl(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.vpl.sceKernelAllocateVpl(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7]);
    }

    public void sceKernelAllocateVplCB(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.vpl.sceKernelAllocateVplCB(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7]);
    }

    public void sceKernelTryAllocateVpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.vpl.sceKernelTryAllocateVpl(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    public void sceKernelFreeVpl(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.vpl.sceKernelFreeVpl(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelCancelVpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.vpl.sceKernelCancelVpl(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelReferVplStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.vpl.sceKernelReferVplStatus(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelCreateFpl(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.fpl.sceKernelCreateFpl(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]);
    }

    public void sceKernelDeleteFpl(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.fpl.sceKernelDeleteFpl(cpu.gpr[4]);
    }

    public void sceKernelAllocateFpl(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.fpl.sceKernelAllocateFpl(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    public void sceKernelAllocateFplCB(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.fpl.sceKernelAllocateFplCB(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    public void sceKernelTryAllocateFpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.fpl.sceKernelTryAllocateFpl(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelFreeFpl(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Managers.fpl.sceKernelFreeFpl(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelCancelFpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.fpl.sceKernelCancelFpl(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelReferFplStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.fpl.sceKernelReferFplStatus(cpu.gpr[4], cpu.gpr[5]);
    }

    public void _sceKernelReturnFromTimerHandler(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented _sceKernelReturnFromTimerHandler");

        cpu.gpr[2] = 0;
    }

    public void sceKernelUSec2SysClock(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.systime.sceKernelUSec2SysClock(cpu.gpr[4], cpu.gpr[5]);
    }

    public void sceKernelUSec2SysClockWide(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.systime.sceKernelUSec2SysClockWide(cpu.gpr[4]);
    }

    public void sceKernelSysClock2USec(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.systime.sceKernelSysClock2USec(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    public void sceKernelSysClock2USecWide(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.systime.sceKernelSysClock2USecWide(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7]);
    }

    public void sceKernelGetSystemTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.systime.sceKernelGetSystemTime(cpu.gpr[4]);
    }

    public void sceKernelGetSystemTimeWide(Processor processor) {
        Managers.systime.sceKernelGetSystemTimeWide();
    }

    public void sceKernelGetSystemTimeLow(Processor processor) {
        Managers.systime.sceKernelGetSystemTimeLow();
    }

    /**
     * Set an alarm.
     * @param delayUsec - The number of micro seconds till the alarm occurs.
     * @param handlerAddress - Pointer to a ::SceKernelAlarmHandler
     * @param handlerArgument - Common pointer for the alarm handler
     *
     * @return A UID representing the created alarm, < 0 on error.
     */
    public void sceKernelSetAlarm(Processor processor) {
        CpuState cpu = processor.cpu;

        int delayUsec = cpu.gpr[4];
        int handlerAddress = cpu.gpr[5];
        int handlerArgument = cpu.gpr[6];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelSetAlarm(%d,0x%08X,0x%08X)", delayUsec, handlerAddress, handlerArgument));
        }

        hleKernelSetAlarm(processor, delayUsec, handlerAddress, handlerArgument);
    }

    /**
     * Set an alarm using a ::SceKernelSysClock structure for the time
     *
     * @param delaySysclockAddr - Pointer to a ::SceKernelSysClock structure
     * @param handlerAddress - Pointer to a ::SceKernelAlarmHandler
     * @param handlerArgument - Common pointer for the alarm handler.
     *
     * @return A UID representing the created alarm, < 0 on error.
     */
    public void sceKernelSetSysClockAlarm(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int delaySysclockAddr = cpu.gpr[4];
        int handlerAddress = cpu.gpr[5];
        int handlerArgument = cpu.gpr[6];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelSetSysClockAlarm(0x%08X,0x%08X,0x%08X)", delaySysclockAddr, handlerAddress, handlerArgument));
        }

        if (Memory.isAddressGood(delaySysclockAddr)) {
            long delaySysclock = mem.read64(delaySysclockAddr);
            long delayUsec = SystemTimeManager.hleSysClock2USec(delaySysclock);

            hleKernelSetAlarm(processor, delayUsec, handlerAddress, handlerArgument);
        } else {
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_ADDR;
        }
    }

    /**
     * Cancel a pending alarm.
     *
     * @param alarmUid - UID of the alarm to cancel.
     *
     * @return 0 on success, < 0 on error.
     */
    public void sceKernelCancelAlarm(Processor processor) {
        CpuState cpu = processor.cpu;

        int alarmUid = cpu.gpr[4];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelCancelAlarm(uid=0x%x)", alarmUid));
        }

        SceKernelAlarmInfo sceKernelAlarmInfo = alarms.get(alarmUid);
        if (sceKernelAlarmInfo == null) {
            log.warn(String.format("sceKernelCancelAlarm unknown uid=0x%x", alarmUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_ALARM;
        } else {
            cancelAlarm(sceKernelAlarmInfo);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Refer the status of a created alarm.
     *
     * @param alarmUid - UID of the alarm to get the info of
     * @param infoAddr - Pointer to a ::SceKernelAlarmInfo structure
     *
     * @return 0 on success, < 0 on error.
     */
    public void sceKernelReferAlarmStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int alarmUid = cpu.gpr[4];
        int infoAddr = cpu.gpr[5];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelReferAlarmStatus(uid=0x%x, infoAddr=0x%08X)", alarmUid, infoAddr));
        }

        SceKernelAlarmInfo sceKernelAlarmInfo = alarms.get(alarmUid);
        if (sceKernelAlarmInfo == null) {
            log.warn(String.format("sceKernelReferAlarmStatus unknown uid=0x%x", alarmUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_ALARM;
        } else if (!Memory.isAddressGood(infoAddr)) {
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_ADDR;
        } else {
            int size = mem.read32(infoAddr);
            sceKernelAlarmInfo.size = size;
            sceKernelAlarmInfo.write(mem, infoAddr);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Create a virtual timer
     *
     * @param nameAddr - Name for the timer.
     * @param optAddr  - Pointer to an ::SceKernelVTimerOptParam (pass NULL)
     *
     * @return The VTimer's UID or < 0 on error.
     */
    public void sceKernelCreateVTimer(Processor processor) {
        CpuState cpu = processor.cpu;

        int nameAddr = cpu.gpr[4];
        int optAddr = cpu.gpr[5];
        String name = Utilities.readStringZ(nameAddr);
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelCreateVTimer(name=%s(0x%08X), optAddr=0x%08X)", name, nameAddr, optAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceKernelVTimerInfo sceKernelVTimerInfo = new SceKernelVTimerInfo(name);
        vtimers.put(sceKernelVTimerInfo.uid, sceKernelVTimerInfo);
        cpu.gpr[2] = sceKernelVTimerInfo.uid;
    }

    /**
     * Delete a virtual timer
     *
     * @param vtimerUid - The UID of the timer
     *
     * @return < 0 on error.
     */
    public void sceKernelDeleteVTimer(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelDeleteVTimer(uid=0x%x)", vtimerUid));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.remove(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelDeleteVTimer unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            sceKernelVTimerInfo.delete();
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Get the timer base
     *
     * @param vtimerUid - UID of the vtimer
     * @param baseAddr - Pointer to a ::SceKernelSysClock structure
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelGetVTimerBase(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int vtimerUid = cpu.gpr[4];
        int baseAddr = cpu.gpr[5];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetVTimerBase(uid=0x%x,baseAddr=0x%08X)", vtimerUid, baseAddr));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelGetVTimerBase unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else if (!Memory.isAddressGood(baseAddr)) {
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_ADDR;
        } else {
            mem.write64(baseAddr, sceKernelVTimerInfo.base);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Get the timer base (wide format)
     *
     * @param vtimerUid - UID of the vtimer
     *
     * @return The 64bit timer base
     */
    public void sceKernelGetVTimerBaseWide(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetVTimerBaseWide(uid=0x%x)", vtimerUid));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelGetVTimerBaseWide unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            Utilities.returnRegister64(cpu, sceKernelVTimerInfo.base);
        }
    }

    /**
     * Get the timer time
     *
     * @param vtimerUid - UID of the vtimer
     * @param timeAddr - Pointer to a ::SceKernelSysClock structure
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelGetVTimerTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int vtimerUid = cpu.gpr[4];
        int timeAddr = cpu.gpr[5];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetVTimerTime(uid=0x%x,timeAddr=0x%08X)", vtimerUid, timeAddr));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelGetVTimerTime unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else if (!Memory.isAddressGood(timeAddr)) {
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_ADDR;
        } else {
            long time = getVTimerTime(sceKernelVTimerInfo);
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceKernelGetVTimerTime returning %d", time));
            }
            mem.write64(timeAddr, time);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Get the timer time (wide format)
     *
     * @param vtimerUid - UID of the vtimer
     *
     * @return The 64bit timer time
     */
    public void sceKernelGetVTimerTimeWide(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetVTimerTimeWide(uid=0x%x)", vtimerUid));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelGetVTimerTimeWide unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            long time = getVTimerTime(sceKernelVTimerInfo);
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceKernelGetVTimerTimeWide returning %d", time));
            }
            Utilities.returnRegister64(cpu, time);
        }
    }

    /**
     * Set the timer time
     *
     * @param vtimerUid - UID of the vtimer
     * @param timeAddr - Pointer to a ::SceKernelSysClock structure
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelSetVTimerTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int vtimerUid = cpu.gpr[4];
        int timeAddr = cpu.gpr[5];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelSetVTimerTime(uid=0x%x,timeAddr=0x%08X)", vtimerUid, timeAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelSetVTimerTime unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else if (!Memory.isAddressGood(timeAddr)) {
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_ADDR;
        } else {
            long time = mem.read64(timeAddr);
            setVTimer(sceKernelVTimerInfo, time);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Set the timer time (wide format)
     *
     * @param vtimerUid - UID of the vtimer
     * @param time - a ::SceKernelSysClock structure
     *
     * @return Possibly the last time
     */
    public void sceKernelSetVTimerTimeWide(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        // cpu.gpr[5] not used!
        long time = Utilities.getRegister64(cpu, 6);
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelSetVTimerTime(uid=0x%x,time=0x%016X)", vtimerUid, time));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelSetVTimerTime unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            setVTimer(sceKernelVTimerInfo, time);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Start a virtual timer
     *
     * @param vtimerUid - The UID of the timer
     *
     * @return < 0 on error
     */
    public void sceKernelStartVTimer(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelStartVTimer(uid=0x%x)", vtimerUid));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelStartVTimer unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            if (sceKernelVTimerInfo.active == SceKernelVTimerInfo.ACTIVE_RUNNING) {
                cpu.gpr[2] = 1; // already started
            } else {
                startVTimer(sceKernelVTimerInfo);
                cpu.gpr[2] = 0;
            }
        }
    }

    /**
     * Stop a virtual timer
     *
     * @param vtimerUid - The UID of the timer
     *
     * @return < 0 on error
     */
    public void sceKernelStopVTimer(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelStopVTimer(uid=0x%x)", vtimerUid));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelStopVTimer unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            if (sceKernelVTimerInfo.active == SceKernelVTimerInfo.ACTIVE_STOPPED) {
                cpu.gpr[2] = 0; // already stopped
            } else {
                stopVTimer(sceKernelVTimerInfo);
                cpu.gpr[2] = 1;
            }
        }
    }

    /**
     * Set the timer handler
     *
     * @param vtimerUid - UID of the vtimer
     * @param scheduleAddr - Time to call the handler
     * @param handlerAddress - The timer handler
     * @param handlerArgument  - Common pointer
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelSetVTimerHandler(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int vtimerUid = cpu.gpr[4];
        int scheduleAddr = cpu.gpr[5];
        int handlerAddress = cpu.gpr[6];
        int handlerArgument = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.warn(String.format("sceKernelSetVTimerHandler(uid=0x%x,scheduleAddr=0x%08X,handlerAddress=0x%08X,handlerArgument=0x%08X)", vtimerUid, scheduleAddr, handlerAddress, handlerArgument));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelSetVTimerHandler unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else if (!Memory.isAddressGood(scheduleAddr)) {
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_ADDR;
        } else {
            long schedule = mem.read64(scheduleAddr);
            sceKernelVTimerInfo.handlerAddress = handlerAddress;
            sceKernelVTimerInfo.handlerArgument = handlerArgument;
            if (handlerAddress != 0) {
                scheduleVTimer(sceKernelVTimerInfo, schedule);
            }
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Set the timer handler (wide mode)
     *
     * @param vtimerUid - UID of the vtimer
     * @param schedule - Time to call the handler
     * @param handlerAddress - The timer handler
     * @param handlerArgument  - Common pointer
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelSetVTimerHandlerWide(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        // cpu.gpr[5] not used!
        long schedule = Utilities.getRegister64(cpu, 6);
        int handlerAddress = cpu.gpr[8];
        int handlerArgument = cpu.gpr[9];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelSetVTimerHandlerWide(uid=0x%x,schedule=0x%016X,handlerAddress=0x%08X,handlerArgument=0x%08X)", vtimerUid, schedule, handlerAddress, handlerArgument));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelSetVTimerHandler unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            sceKernelVTimerInfo.handlerAddress = handlerAddress;
            sceKernelVTimerInfo.handlerArgument = handlerArgument;
            if (handlerAddress != 0) {
                scheduleVTimer(sceKernelVTimerInfo, schedule);
            }
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Cancel the timer handler
     *
     * @param vtimerUid - The UID of the vtimer
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelCancelVTimerHandler(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelCancelVTimerHandler(uid=0x%x)", vtimerUid));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelCancelVTimerHandler unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            cancelVTimer(sceKernelVTimerInfo);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Get the status of a VTimer
     *
     * @param vtimerUid - The uid of the VTimer
     * @param infoAddr - Pointer to a ::SceKernelVTimerInfo structure
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelReferVTimerStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int vtimerUid = cpu.gpr[4];
        int infoAddr = cpu.gpr[5];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelReferVTimerStatus(uid=0x%x,infoAddr=0x%08X)", vtimerUid, infoAddr));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelReferVTimerStatus unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else if (!Memory.isAddressGood(infoAddr)) {
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_ADDR;
        } else {
            int size = mem.read32(infoAddr);
            sceKernelVTimerInfo.size = size;
            sceKernelVTimerInfo.write(mem, infoAddr);
            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelCreateThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int name_addr = cpu.gpr[4];
        int entry_addr = cpu.gpr[5];
        int initPriority = cpu.gpr[6];
        int stackSize = cpu.gpr[7];
        int attr = cpu.gpr[8];
        int option_addr = cpu.gpr[9];
        String name = readStringNZ(name_addr, 32);

        if (log.isDebugEnabled()) {
            log.debug("sceKernelCreateThread redirecting to hleKernelCreateThread");
        }
        SceKernelThreadInfo thread = hleKernelCreateThread(name, entry_addr, initPriority, stackSize, attr, option_addr);

        if (thread.stackSize > 0 && thread.stack_addr == 0) {
            log.warn("sceKernelCreateThread not enough memory to create the stack");
            hleDeleteThread(thread);
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_NO_MEMORY;
        } else {
            // Check if there's a registered event handler for this thread.
            if (threadEventMap.containsKey(thread.uid)) {
                int handlerUid = threadEventMap.get(thread.uid);
                SceKernelThreadEventHandlerInfo handler = threadEventHandlerMap.get(handlerUid);

                // Check if this handler's mask matches the function.
                if (handler.checkCreateMask()) {
                    handler.triggerThreadEventHandler(SceKernelThreadEventHandlerInfo.THREAD_EVENT_CREATE);
                    cpu.gpr[2] = handler.result;
                }
            } else {
                // Inherit kernel mode if user mode bit is not set
                if ((currentThread.attr & PSP_THREAD_ATTR_KERNEL) == PSP_THREAD_ATTR_KERNEL &&
                        (attr & PSP_THREAD_ATTR_USER) != PSP_THREAD_ATTR_USER) {
                    log.debug("sceKernelCreateThread inheriting kernel mode");
                    thread.attr |= PSP_THREAD_ATTR_KERNEL;
                }
                // Inherit user mode
                if ((currentThread.attr & PSP_THREAD_ATTR_USER) == PSP_THREAD_ATTR_USER) {
                    if ((thread.attr & PSP_THREAD_ATTR_USER) != PSP_THREAD_ATTR_USER) {
                        log.debug("sceKernelCreateThread inheriting user mode");
                    }
                    thread.attr |= PSP_THREAD_ATTR_USER;
                    // Always remove kernel mode bit
                    thread.attr &= ~PSP_THREAD_ATTR_KERNEL;
                }
                cpu.gpr[2] = thread.uid;
            }
        }
    }

    /** mark a thread for deletion. */
    public void sceKernelDeleteThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (uid == 0) {
            uid = currentThread.uid;
        }
        if (!checkThreadID(uid)) {
            return;
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else {
            log.debug("sceKernelDeleteThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");
            // Check if there's a registered event handler for this thread.
            if (threadEventMap.containsKey(thread.uid)) {
                int handlerUid = threadEventMap.get(thread.uid);
                SceKernelThreadEventHandlerInfo handler = threadEventHandlerMap.get(handlerUid);

                // Check if this handler's mask matches the function.
                if (handler.checkDeleteMask()) {
                    handler.triggerThreadEventHandler(SceKernelThreadEventHandlerInfo.THREAD_EVENT_DELETE);
                    cpu.gpr[2] = handler.result;
                }
            } else {
                // Mark thread for deletion
                setToBeDeletedThread(thread);
                if (thread.status != PSP_THREAD_STOPPED) {
                    cpu.gpr[2] = ERROR_KERNEL_THREAD_IS_NOT_DORMANT;
                } else {
                    cpu.gpr[2] = 0;
                }
            }
        }
    }

    public void sceKernelStartThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        int len = cpu.gpr[5];
        int data_addr = cpu.gpr[6];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!checkThreadID(uid)) {
            return;
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else if (isBannedThread(thread)) {
            log.warn("sceKernelStartThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' banned, not starting");
            // Banned, fake start.
            cpu.gpr[2] = 0;
            hleRescheduleCurrentThread();
        } else if (thread.status != PSP_THREAD_STOPPED) {
            cpu.gpr[2] = ERROR_KERNEL_THREAD_IS_NOT_DORMANT;
        } else {
            // Check if there's a registered event handler for this thread.
            if (threadEventMap.containsKey(thread.uid)) {
                int handlerUid = threadEventMap.get(thread.uid);
                SceKernelThreadEventHandlerInfo handler = threadEventHandlerMap.get(handlerUid);
                // Check if this handler's mask matches the function.
                if (handler.checkStartMask()) {
                    handler.triggerThreadEventHandler(SceKernelThreadEventHandlerInfo.THREAD_EVENT_START);
                    cpu.gpr[2] = handler.result;
                }
                thread.exitStatus = ERROR_KERNEL_THREAD_IS_NOT_DORMANT; // Update the exit status.
            } else {
                log.debug("sceKernelStartThread redirecting to hleKernelStartThread");
                cpu.gpr[2] = 0;
                hleKernelStartThread(thread, len, data_addr, thread.gpReg_addr);
                thread.exitStatus = ERROR_KERNEL_THREAD_IS_NOT_DORMANT; // Update the exit status.
            }
        }
    }

    public void _sceKernelExitThread(Processor processor) {
        // _sceKernelExitThread is equivalent to sceKernelExitThread
        sceKernelExitThread(processor);
    }

    /** exit the current thread */
    public void sceKernelExitThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int exitStatus = cpu.gpr[4];

        SceKernelThreadInfo thread = currentThread;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelExitThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' exitStatus:0x" + Integer.toHexString(exitStatus));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if(exitStatus < 0) {
            thread.exitStatus = ERROR_KERNEL_ILLEGAL_ARGUMENT;
        } else {
            thread.exitStatus = exitStatus;
        }
        // Check if there's a registered event handler for this thread.
        if (threadEventMap.containsKey(thread.uid)) {
            int handlerUid = threadEventMap.get(thread.uid);
            SceKernelThreadEventHandlerInfo handler = threadEventHandlerMap.get(handlerUid);
            // Check if this handler's mask matches the function.
            if (handler.checkExitMask()) {
                handler.triggerThreadEventHandler(SceKernelThreadEventHandlerInfo.THREAD_EVENT_EXIT);
                cpu.gpr[2] = handler.result;
            }
        } else {
            cpu.gpr[2] = 0;
            hleChangeThreadState(thread, PSP_THREAD_STOPPED);
            RuntimeContext.onThreadExit(thread);
            hleRescheduleCurrentThread();
        }
    }

    /** exit the current thread, then delete it */
    public void sceKernelExitDeleteThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int exitStatus = cpu.gpr[4];

        SceKernelThreadInfo thread = currentThread;
        if (log.isDebugEnabled()) {
            log.debug("sceKernelExitDeleteThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' exitStatus:0x" + Integer.toHexString(exitStatus));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        thread.exitStatus = exitStatus;
        cpu.gpr[2] = 0;
        hleChangeThreadState(thread, PSP_THREAD_STOPPED);
        RuntimeContext.onThreadExit(thread);
        setToBeDeletedThread(thread);
        hleRescheduleCurrentThread();
    }

    /** terminate thread */
    public void sceKernelTerminateThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (!checkThreadID(uid)) {
            return;
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else {
            log.debug("sceKernelTerminateThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");
            cpu.gpr[2] = 0;
            terminateThread(thread);
            thread.exitStatus = ERROR_KERNEL_THREAD_IS_TERMINATED; // Update the exit status.
        }
    }

    /** terminate thread, then mark it for deletion */
    public void sceKernelTerminateDeleteThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!checkThreadID(uid)) {
            return;
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else {
            log.debug("sceKernelTerminateDeleteThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");
            terminateThread(thread);
            setToBeDeletedThread(thread);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Suspend the dispatch thread
     *
     * @return The current state of the dispatch thread, < 0 on error
     */
    public void sceKernelSuspendDispatchThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int state = getDispatchThreadState();
        if (log.isDebugEnabled()) {
            log.debug("sceKernelSuspendDispatchThread() state=" + state);
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        dispatchThreadEnabled = false;
        cpu.gpr[2] = state;
    }

    /**
     * Resume the dispatch thread
     *
     * @param state - The state of the dispatch thread
     *                (from sceKernelSuspendDispatchThread)
     * @return 0 on success, < 0 on error
     */
    public void sceKernelResumeDispatchThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int state = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelResumeDispatchThread(state=" + state + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (state == SCE_KERNEL_DISPATCHTHREAD_STATE_ENABLED) {
            dispatchThreadEnabled = true;
        }
        cpu.gpr[2] = 0;
    }

    public void sceKernelChangeCurrentThreadAttr(Processor processor) {
        CpuState cpu = processor.cpu;

        int removeAttr = cpu.gpr[4];
        int addAttr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelChangeCurrentThreadAttr" + " removeAttr:0x" + Integer.toHexString(removeAttr) + " addAttr:0x" + Integer.toHexString(addAttr) + " oldAttr:0x" + Integer.toHexString(currentThread.attr));
        }

        // Probably meant to be sceKernelChangeThreadAttr unknown=uid
        if (removeAttr != 0) {
            log.warn("sceKernelChangeCurrentThreadAttr removeAttr:0x" + Integer.toHexString(removeAttr) + " non-zero");
        }

        int newAttr = (currentThread.attr & ~removeAttr) | addAttr;
        // Don't allow switching into kernel mode!
        if (userCurrentThreadTryingToSwitchToKernelMode(newAttr)) {
            log.debug("sceKernelChangeCurrentThreadAttr forcing user mode");
            newAttr |= PSP_THREAD_ATTR_USER;
        }
        currentThread.attr = newAttr;
        cpu.gpr[2] = 0;
    }

    public void sceKernelChangeThreadPriority(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        int priority = cpu.gpr[5];

        if (uid == 0) {
            uid = currentThread.uid;
        }

        // Priority 0 means priority of the calling thread
        if (priority == 0) {
            priority = currentThread.currentPriority;
        }

        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (uid < 0) {
            log.warn("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(uid) + " is invalid");
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_THREAD;
        } else if (thread == null) {
            log.warn("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(uid) + " newPriority:0x" + Integer.toHexString(priority) + ") unknown thread");
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else if ((thread.status & PSP_THREAD_STOPPED) == PSP_THREAD_STOPPED) {
            log.warn("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(uid) + " newPriority:0x" + Integer.toHexString(priority) + " oldPriority:0x" + Integer.toHexString(thread.currentPriority) + " thread is stopped, ignoring");
            // Tested on PSP:
            // If the thread is stopped, it's current priority is replaced by it's initial priority.
            thread.currentPriority = thread.initPriority;
            cpu.gpr[2] = ERROR_KERNEL_THREAD_ALREADY_DORMANT;
        } else if ((priority < 0x10 || priority > 0x6F) && priority != 0x7F) {
            // Only affects user and idle threads (range from 0x10 to 0x6F and 0x7F).
            log.warn("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(uid) + " newPriority:0x" + Integer.toHexString(priority) + " oldPriority:0x" + Integer.toHexString(thread.currentPriority) + " newPriority is outside of valid range");
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_PRIORITY;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(uid) + " newPriority:0x" + Integer.toHexString(priority) + " oldPriority:0x" + Integer.toHexString(thread.currentPriority));
            }
            cpu.gpr[2] = 0;
            hleKernelChangeThreadPriority(thread, priority);
        }
    }

    /**
     * Rotate thread ready queue at a set priority
     *
     * @param priority - The priority of the queue
     *
     * @return 0 on success, < 0 on error.
     */
    public void sceKernelRotateThreadReadyQueue(Processor processor) {
        CpuState cpu = processor.cpu;

        int priority = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelRotateThreadReadyQueue priority=" + priority);
        }

        if (priority == 0) {
            priority = currentThread.currentPriority;
        }

        if ((priority < 0x10 || priority > 0x6F) && priority != 0x7F) {
            // Only affects user and idle threads (range from 0x10 to 0x6F and 0x7F).
            log.warn("sceKernelRotateThreadReadyQueue priority:0x" + Integer.toHexString(priority) + " is outside of valid range");
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_PRIORITY;
        } else {
            synchronized (readyThreads) {
                for (SceKernelThreadInfo thread : readyThreads) {
                    if (thread.currentPriority == priority) {
                        // Move the thread to the end of the list
                    	removeFromReadyThreads(thread);
                        addToReadyThreads(thread, false);
                        hleRescheduleCurrentThread();
                        break;
                    }
                }
            }
        }
    }

    public void sceKernelReleaseWaitThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (uid == 0) {
            log.warn("sceKernelReleaseWaitThread SceUID=" + Integer.toHexString(uid) + ") can't release itself!");
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_ILLEGAL_THREAD;
        } else if (thread == null) {
            log.warn("sceKernelReleaseWaitThread SceUID=" + Integer.toHexString(uid) + ") unknown thread");
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_NOT_FOUND_THREAD;
        } else if (thread.status != PSP_THREAD_WAITING) {
            if (log.isDebugEnabled()) {
                log.debug("sceKernelReleaseWaitThread SceUID=" + Integer.toHexString(uid) + ") Thread not waiting: status=" + thread.status);
            }
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_THREAD_IS_NOT_WAIT;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("sceKernelReleaseWaitThread SceUID=" + Integer.toHexString(uid) + ") releasing waiting thread: " + thread.toString());
            }
            cpu.gpr[2] = 0;
            hleThreadWaitRelease(thread);
            // Switch to the released thread if it has a higher priority.
            if (thread.currentPriority < currentThread.currentPriority) {
                hleRescheduleCurrentThread();
            }
        }
    }

    /** Get the current thread Id */
    public void sceKernelGetThreadId(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelGetThreadId returning uid=0x" + Integer.toHexString(currentThread.uid));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = currentThread.uid;
    }

    public void sceKernelGetThreadCurrentPriority(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelGetThreadCurrentPriority returning currentPriority=" + currentThread.currentPriority);
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = currentThread.currentPriority;
    }

    /** @return ERROR_NOT_FOUND_THREAD on uid < 0, uid == 0 and thread not found */
    public void sceKernelGetThreadExitStatus(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn("sceKernelGetThreadExitStatus unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("sceKernelGetThreadExitStatus uid=0x" + Integer.toHexString(uid) + " exitStatus=0x" + Integer.toHexString(thread.exitStatus));
            }
            cpu.gpr[2] = thread.exitStatus;
        }
    }

    /** @return amount of free stack space.*/
    public void sceKernelCheckThreadStack(Processor processor) {
        CpuState cpu = processor.cpu;

        int size = getThreadCurrentStackSize();
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelCheckThreadStack returning size=0x%X", size));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = size;
    }

    /** @return amount of unused stack space of a thread.*/
    public void sceKernelGetThreadStackFreeSize(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (uid == 0) {
            uid = currentThread.uid;
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn("sceKernelGetThreadStackFreeSize unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else {
            // This function compares the stack of the specified thread from when it started with
            // it's present state. The returned value may not be always the remaining free stack size, because
            // only the space that has never been used (consumed and/or released afterwards, for instance) counts.
            int size = getThreadCurrentStackSize() - 0xfb0;
            if (size < 0) {
                size = 0;
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceKernelGetThreadStackFreeSize returning size=0x%X", size));
            }
            cpu.gpr[2] = size;
        }
    }

    /** Get the status information for the specified thread
     **/
    public void sceKernelReferThreadStatus(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        int addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferThreadStatus uid=0x" + Integer.toHexString(uid) + " addr=0x" + Integer.toHexString(addr));
        }

        if (uid == 0) {
            uid = currentThread.uid;
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn("sceKernelReferThreadStatus unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else {
            thread.write(Memory.getInstance(), addr);
            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelReferThreadRunStatus(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        int addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferThreadRunStatus uid=0x" + Integer.toHexString(uid) + " addr=0x" + Integer.toHexString(addr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (uid == 0) {
            uid = currentThread.uid;
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn("sceKernelReferThreadRunStatus unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else {
            thread.writeRunStatus(Memory.getInstance(), addr);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Get the current system status.
     *
     * @param status - Pointer to a ::SceKernelSystemStatus structure.
     *
     * @return < 0 on error.
     */
    public void sceKernelReferSystemStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int statusAddr = cpu.gpr[4];

        if (Memory.isAddressGood(statusAddr)) {
            SceKernelSystemStatus status = new SceKernelSystemStatus();
            status.read(mem, statusAddr);
            status.status = 0;
            status.write(mem, statusAddr);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = -1;
        }
    }

    /** Write uid's to buffer
     * return written count
     * save full count to idcount_addr */
    public void sceKernelGetThreadmanIdList(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int type = cpu.gpr[4];
        int readbuf_addr = cpu.gpr[5];
        int readbufsize = cpu.gpr[6];
        int idcount_addr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelGetThreadmanIdList type=" + type + " readbuf:0x" + Integer.toHexString(readbuf_addr) + " readbufsize:" + readbufsize + " idcount:0x" + Integer.toHexString(idcount_addr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int saveCount = 0;
        int fullCount = 0;

        if (type == SCE_KERNEL_TMID_Thread) {
            for (SceKernelThreadInfo thread : threadMap.values()) {
                // Hide kernel mode threads when called from a user mode thread
                if (userThreadCalledKernelCurrentThread(thread)) {
                    if (saveCount < readbufsize) {
                        if (log.isDebugEnabled()) {
                            log.debug("sceKernelGetThreadmanIdList adding thread '" + thread.name + "'");
                        }
                        mem.write32(readbuf_addr + saveCount * 4, thread.uid);
                        saveCount++;
                    } else {
                        log.warn("sceKernelGetThreadmanIdList NOT adding thread '" + thread.name + "' (no more space)");
                    }
                    fullCount++;
                }
            }
        } else {
            log.warn("UNIMPLEMENTED:sceKernelGetThreadmanIdList type=" + type);
        }

        if (Memory.isAddressGood(idcount_addr)) {
            mem.write32(idcount_addr, fullCount);
        }

        cpu.gpr[2] = saveCount;
    }

    public void sceKernelGetThreadmanIdType(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelGetThreadmanIdType uid=0x" + uid);
        }

        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", false)) {
            cpu.gpr[2] = SCE_KERNEL_TMID_Thread;
        } else if (SceUidManager.checkUidPurpose(uid, "ThreadMan-sema", false)) {
            cpu.gpr[2] = SCE_KERNEL_TMID_Semaphore;
        } else if (SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", false)) {
            cpu.gpr[2] = SCE_KERNEL_TMID_EventFlag;
        } else if (SceUidManager.checkUidPurpose(uid, "ThreadMan-Mbx", false)) {
            cpu.gpr[2] = SCE_KERNEL_TMID_Mbox;
        } else if (SceUidManager.checkUidPurpose(uid, "ThreadMan-Vpl", false)) {
            cpu.gpr[2] = SCE_KERNEL_TMID_Vpl;
        } else if (SceUidManager.checkUidPurpose(uid, "ThreadMan-Fpl", false)) {
            cpu.gpr[2] = SCE_KERNEL_TMID_Fpl;
        } else if (SceUidManager.checkUidPurpose(uid, "ThreadMan-MsgPipe", false)) {
            cpu.gpr[2] = SCE_KERNEL_TMID_Mpipe;
        } else if (SceUidManager.checkUidPurpose(uid, "ThreadMan-callback", false)) {
            cpu.gpr[2] = SCE_KERNEL_TMID_Callback;
        } else if (SceUidManager.checkUidPurpose(uid, "ThreadMan-ThreadEventHandler", false)) {
            cpu.gpr[2] = SCE_KERNEL_TMID_ThreadEventHandler;
        } else if (SceUidManager.checkUidPurpose(uid, "ThreadMan-Alarm", false)) {
            cpu.gpr[2] = SCE_KERNEL_TMID_Alarm;
        } else if (SceUidManager.checkUidPurpose(uid, "ThreadMan-VTimer", false)) {
            cpu.gpr[2] = SCE_KERNEL_TMID_VTimer;
        } else if (SceUidManager.checkUidPurpose(uid, "ThreadMan-Mutex", false)) {
            cpu.gpr[2] = SCE_KERNEL_TMID_Mutex;
        } else if (SceUidManager.checkUidPurpose(uid, "ThreadMan-LwMutex", false)) {
            cpu.gpr[2] = SCE_KERNEL_TMID_LwMutex;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_ILLEGAL_ARGUMENT;
        }
    }

    public void sceKernelReferThreadProfiler(Processor processor) {
        CpuState cpu = processor.cpu;

        // Can be safely ignored. Only valid in debug mode on a real PSP.
        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferThreadProfiler");
        }

        cpu.gpr[2] = 0;
    }

    public void sceKernelReferGlobalProfiler(Processor processor) {
        CpuState cpu = processor.cpu;

        // Can be safely ignored. Only valid in debug mode on a real PSP.
        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferGlobalProfiler");
        }

        cpu.gpr[2] = 0;
    }

    public static class Statistics {

        private ArrayList<ThreadStatistics> threads = new ArrayList<ThreadStatistics>();
        public long allCycles = 0;
        public long startTimeMillis;
        public long endTimeMillis;
        public long allCpuMillis = 0;

        public Statistics() {
            startTimeMillis = System.currentTimeMillis();
        }

        public void exit() {
            endTimeMillis = System.currentTimeMillis();
        }

        public long getDurationMillis() {
            return endTimeMillis - startTimeMillis;
        }

        private void addThreadStatistics(SceKernelThreadInfo thread) {
        	if (!DurationStatistics.collectStatistics) {
        		return;
        	}

        	ThreadStatistics threadStatistics = new ThreadStatistics();
            threadStatistics.name = thread.name;
            threadStatistics.runClocks = thread.runClocks;
            threads.add(threadStatistics);

            allCycles += thread.runClocks;

            if (thread.javaThreadId > 0) {
	            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	            if (threadMXBean.isThreadCpuTimeEnabled()) {
	            	long threadCpuTimeNanos = thread.javaThreadCpuTimeNanos;
	            	if (threadCpuTimeNanos < 0) {
	            		threadCpuTimeNanos = threadMXBean.getThreadCpuTime(thread.javaThreadId);
	            	}
	            	if (threadCpuTimeNanos > 0) {
	            		allCpuMillis += threadCpuTimeNanos / 1000000L;
	            	}
	            }
            }
        }

        private static class ThreadStatistics implements Comparable<ThreadStatistics> {
            public String name;
            public long runClocks;

            @Override
			public int compareTo(ThreadStatistics o) {
				return -(new Long(runClocks).compareTo(o.runClocks));
			}

            public String getQuotedName() {
            	return "'" + name + "'";
            }
        }
    }

    public static class CallbackManager {

        private Map<Integer, Callback> callbacks;
        private int currentCallbackId;

        public void Initialize() {
            callbacks = new HashMap<Integer, Callback>();
            currentCallbackId = 1;
        }

        public void addCallback(Callback callback) {
            callbacks.put(callback.getId(), callback);
        }

        public Callback remove(int id) {
            Callback callback = callbacks.remove(id);

            return callback;
        }

        public int getNewCallbackId() {
            return currentCallbackId++;
        }
    }

    public static class Callback {

        private int id;
        private int savedIdRegister;
        private int savedRa;
        private int savedPc;
        private int savedV0;
        private int savedV1;
        private IAction afterAction;
        private boolean returnVoid;

        public Callback(int id, int savedIdRegister, int savedRa, int savedPc, int savedV0, int savedV1, IAction afterAction, boolean returnVoid) {
            this.id = id;
            this.savedIdRegister = savedIdRegister;
            this.savedRa = savedRa;
            this.savedPc = savedPc;
            this.savedV0 = savedV0;
            this.savedV1 = savedV1;
            this.afterAction = afterAction;
            this.returnVoid = returnVoid;
        }

        public IAction getAfterAction() {
            return afterAction;
        }

        public int getId() {
            return id;
        }

        public int getSavedIdRegister() {
            return savedIdRegister;
        }

        public int getSavedRa() {
            return savedRa;
        }

        public int getSavedPc() {
            return savedPc;
        }

		public int getSavedV0() {
			return savedV0;
		}

		public int getSavedV1() {
			return savedV1;
		}

		public boolean isReturnVoid() {
			return returnVoid;
		}

        @Override
        public String toString() {
            return String.format("Callback id=%d,savedIdReg=0x%08X,savedPc=0x%08X,savedV0=0x%08X,savedV1=0x%08X", getId(), getSavedIdRegister(), getSavedPc(), getSavedV0(), getSavedV1());
        }
    }

    private class AfterCallAction implements IAction {

        SceKernelThreadInfo thread;
        int status;
        int waitType;
        int waitId;
        ThreadWaitInfo threadWaitInfo;
        boolean doCallback;
        IAction afterAction;

        public AfterCallAction(SceKernelThreadInfo thread, int status, int waitType, int waitId, ThreadWaitInfo threadWaitInfo, boolean doCallback, IAction afterAction) {
            this.thread = thread;
            this.status = status;
            this.waitType = waitType;
            this.waitId = waitId;
            this.threadWaitInfo = threadWaitInfo;
            this.doCallback = doCallback;
            this.afterAction = afterAction;
        }

        @Override
        public void execute() {
            boolean restoreWaitState = true;

            // After calling a callback, check if the waiting state of the thread
            // is still valid, i.e. if the thread must continue to wait or if the
            // wait condition has been reached.
            if (threadWaitInfo.waitStateChecker != null) {
                if (!threadWaitInfo.waitStateChecker.continueWaitState(thread, threadWaitInfo)) {
                    restoreWaitState = false;
                }
            }

            if (restoreWaitState) {
                if (log.isDebugEnabled()) {
                    log.debug("AfterCallAction: restoring wait state for thread: " + thread.toString());
                }

                // Restore the wait state of the thread
                thread.waitType = waitType;
                thread.waitId = waitId;
                thread.wait.copy(threadWaitInfo);

                hleChangeThreadState(thread, status);
            } else if (thread.status != PSP_THREAD_READY) {
                if (log.isDebugEnabled()) {
                    log.debug("AfterCallAction: set thread to READY state: " + thread.toString());
                }

                hleChangeThreadState(thread, PSP_THREAD_READY);
                doCallback = false;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("AfterCallAction: leaving thread in READY state: " + thread.toString());
                }
                doCallback = false;
            }

            hleRescheduleCurrentThread(doCallback);

            if (afterAction != null) {
                afterAction.execute();
            }
        }
    }

    public class TimeoutThreadAction implements IAction {

        private SceKernelThreadInfo thread;

        public TimeoutThreadAction(SceKernelThreadInfo thread) {
            this.thread = thread;
        }

        @Override
        public void execute() {
            hleThreadWaitTimeout(thread);
        }
    }

    public class DeleteThreadAction implements IAction {

        private SceKernelThreadInfo thread;

        public DeleteThreadAction(SceKernelThreadInfo thread) {
            this.thread = thread;
        }

        @Override
        public void execute() {
            hleDeleteThread(thread);
        }
    }
    public final HLEModuleFunction _sceKernelReturnFromCallbackFunction = new HLEModuleFunction("ThreadManForUser", "_sceKernelReturnFromCallback") {

        @Override
        public final void execute(Processor processor) {
            _sceKernelReturnFromCallback(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule._sceKernelReturnFromCallback(processor);";
        }
    };
    public final HLEModuleFunction sceKernelRegisterThreadEventHandlerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelRegisterThreadEventHandler") {

        @Override
        public final void execute(Processor processor) {
            sceKernelRegisterThreadEventHandler(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelRegisterThreadEventHandler(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReleaseThreadEventHandlerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReleaseThreadEventHandler") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReleaseThreadEventHandler(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReleaseThreadEventHandler(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferThreadEventHandlerStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferThreadEventHandlerStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferThreadEventHandlerStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferThreadEventHandlerStatus(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCreateCallbackFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCreateCallback") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCreateCallback(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCreateCallback(processor);";
        }
    };
    public final HLEModuleFunction sceKernelDeleteCallbackFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDeleteCallback") {

        @Override
        public final void execute(Processor processor) {
            sceKernelDeleteCallback(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDeleteCallback(processor);";
        }
    };
    public final HLEModuleFunction sceKernelNotifyCallbackFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelNotifyCallback") {

        @Override
        public final void execute(Processor processor) {
            sceKernelNotifyCallback(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelNotifyCallback(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCancelCallbackFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCancelCallback") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCancelCallback(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCancelCallback(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetCallbackCountFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetCallbackCount") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetCallbackCount(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelGetCallbackCount(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCheckCallbackFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCheckCallback") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCheckCallback(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCheckCallback(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferCallbackStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferCallbackStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferCallbackStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferCallbackStatus(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSleepThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSleepThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSleepThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSleepThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSleepThreadCBFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSleepThreadCB") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSleepThreadCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSleepThreadCB(processor);";
        }
    };
    public final HLEModuleFunction sceKernelWakeupThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelWakeupThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelWakeupThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelWakeupThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCancelWakeupThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCancelWakeupThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCancelWakeupThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCancelWakeupThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSuspendThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSuspendThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSuspendThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSuspendThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelResumeThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelResumeThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelResumeThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelResumeThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelWaitThreadEndFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelWaitThreadEnd") {

        @Override
        public final void execute(Processor processor) {
            sceKernelWaitThreadEnd(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelWaitThreadEnd(processor);";
        }
    };
    public final HLEModuleFunction sceKernelWaitThreadEndCBFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelWaitThreadEndCB") {

        @Override
        public final void execute(Processor processor) {
            sceKernelWaitThreadEndCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelWaitThreadEndCB(processor);";
        }
    };
    public final HLEModuleFunction sceKernelDelayThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDelayThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelDelayThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDelayThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelDelayThreadCBFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDelayThreadCB") {

        @Override
        public final void execute(Processor processor) {
            sceKernelDelayThreadCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDelayThreadCB(processor);";
        }
    };
    public final HLEModuleFunction sceKernelDelaySysClockThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDelaySysClockThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelDelaySysClockThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDelaySysClockThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelDelaySysClockThreadCBFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDelaySysClockThreadCB") {

        @Override
        public final void execute(Processor processor) {
            sceKernelDelaySysClockThreadCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDelaySysClockThreadCB(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCreateSemaFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCreateSema") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCreateSema(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCreateSema(processor);";
        }
    };
    public final HLEModuleFunction sceKernelDeleteSemaFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDeleteSema") {

        @Override
        public final void execute(Processor processor) {
            sceKernelDeleteSema(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDeleteSema(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSignalSemaFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSignalSema") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSignalSema(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSignalSema(processor);";
        }
    };
    public final HLEModuleFunction sceKernelWaitSemaFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelWaitSema") {

        @Override
        public final void execute(Processor processor) {
            sceKernelWaitSema(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelWaitSema(processor);";
        }
    };
    public final HLEModuleFunction sceKernelWaitSemaCBFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelWaitSemaCB") {

        @Override
        public final void execute(Processor processor) {
            sceKernelWaitSemaCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelWaitSemaCB(processor);";
        }
    };
    public final HLEModuleFunction sceKernelPollSemaFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelPollSema") {

        @Override
        public final void execute(Processor processor) {
            sceKernelPollSema(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelPollSema(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCancelSemaFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCancelSema") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCancelSema(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCancelSema(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferSemaStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferSemaStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferSemaStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferSemaStatus(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCreateEventFlagFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCreateEventFlag") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCreateEventFlag(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCreateEventFlag(processor);";
        }
    };
    public final HLEModuleFunction sceKernelDeleteEventFlagFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDeleteEventFlag") {

        @Override
        public final void execute(Processor processor) {
            sceKernelDeleteEventFlag(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDeleteEventFlag(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSetEventFlagFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSetEventFlag") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSetEventFlag(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSetEventFlag(processor);";
        }
    };
    public final HLEModuleFunction sceKernelClearEventFlagFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelClearEventFlag") {

        @Override
        public final void execute(Processor processor) {
            sceKernelClearEventFlag(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelClearEventFlag(processor);";
        }
    };
    public final HLEModuleFunction sceKernelWaitEventFlagFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelWaitEventFlag") {

        @Override
        public final void execute(Processor processor) {
            sceKernelWaitEventFlag(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelWaitEventFlag(processor);";
        }
    };
    public final HLEModuleFunction sceKernelWaitEventFlagCBFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelWaitEventFlagCB") {

        @Override
        public final void execute(Processor processor) {
            sceKernelWaitEventFlagCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelWaitEventFlagCB(processor);";
        }
    };
    public final HLEModuleFunction sceKernelPollEventFlagFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelPollEventFlag") {

        @Override
        public final void execute(Processor processor) {
            sceKernelPollEventFlag(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelPollEventFlag(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCancelEventFlagFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCancelEventFlag") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCancelEventFlag(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCancelEventFlag(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferEventFlagStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferEventFlagStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferEventFlagStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferEventFlagStatus(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCreateMbxFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCreateMbx") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCreateMbx(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCreateMbx(processor);";
        }
    };
    public final HLEModuleFunction sceKernelDeleteMbxFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDeleteMbx") {

        @Override
        public final void execute(Processor processor) {
            sceKernelDeleteMbx(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDeleteMbx(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSendMbxFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSendMbx") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSendMbx(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSendMbx(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReceiveMbxFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReceiveMbx") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReceiveMbx(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReceiveMbx(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReceiveMbxCBFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReceiveMbxCB") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReceiveMbxCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReceiveMbxCB(processor);";
        }
    };
    public final HLEModuleFunction sceKernelPollMbxFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelPollMbx") {

        @Override
        public final void execute(Processor processor) {
            sceKernelPollMbx(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelPollMbx(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCancelReceiveMbxFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCancelReceiveMbx") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCancelReceiveMbx(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCancelReceiveMbx(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferMbxStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferMbxStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferMbxStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferMbxStatus(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCreateMsgPipeFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCreateMsgPipe") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCreateMsgPipe(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCreateMsgPipe(processor);";
        }
    };
    public final HLEModuleFunction sceKernelDeleteMsgPipeFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDeleteMsgPipe") {

        @Override
        public final void execute(Processor processor) {
            sceKernelDeleteMsgPipe(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDeleteMsgPipe(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSendMsgPipeFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSendMsgPipe") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSendMsgPipe(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSendMsgPipe(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSendMsgPipeCBFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSendMsgPipeCB") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSendMsgPipeCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSendMsgPipeCB(processor);";
        }
    };
    public final HLEModuleFunction sceKernelTrySendMsgPipeFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelTrySendMsgPipe") {

        @Override
        public final void execute(Processor processor) {
            sceKernelTrySendMsgPipe(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelTrySendMsgPipe(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReceiveMsgPipeFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReceiveMsgPipe") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReceiveMsgPipe(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReceiveMsgPipe(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReceiveMsgPipeCBFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReceiveMsgPipeCB") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReceiveMsgPipeCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReceiveMsgPipeCB(processor);";
        }
    };
    public final HLEModuleFunction sceKernelTryReceiveMsgPipeFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelTryReceiveMsgPipe") {

        @Override
        public final void execute(Processor processor) {
            sceKernelTryReceiveMsgPipe(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelTryReceiveMsgPipe(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCancelMsgPipeFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCancelMsgPipe") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCancelMsgPipe(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCancelMsgPipe(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferMsgPipeStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferMsgPipeStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferMsgPipeStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferMsgPipeStatus(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCreateVplFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCreateVpl") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCreateVpl(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCreateVpl(processor);";
        }
    };
    public final HLEModuleFunction sceKernelDeleteVplFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDeleteVpl") {

        @Override
        public final void execute(Processor processor) {
            sceKernelDeleteVpl(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDeleteVpl(processor);";
        }
    };
    public final HLEModuleFunction sceKernelAllocateVplFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelAllocateVpl") {

        @Override
        public final void execute(Processor processor) {
            sceKernelAllocateVpl(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelAllocateVpl(processor);";
        }
    };
    public final HLEModuleFunction sceKernelAllocateVplCBFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelAllocateVplCB") {

        @Override
        public final void execute(Processor processor) {
            sceKernelAllocateVplCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelAllocateVplCB(processor);";
        }
    };
    public final HLEModuleFunction sceKernelTryAllocateVplFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelTryAllocateVpl") {

        @Override
        public final void execute(Processor processor) {
            sceKernelTryAllocateVpl(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelTryAllocateVpl(processor);";
        }
    };
    public final HLEModuleFunction sceKernelFreeVplFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelFreeVpl") {

        @Override
        public final void execute(Processor processor) {
            sceKernelFreeVpl(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelFreeVpl(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCancelVplFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCancelVpl") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCancelVpl(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCancelVpl(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferVplStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferVplStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferVplStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferVplStatus(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCreateFplFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCreateFpl") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCreateFpl(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCreateFpl(processor);";
        }
    };
    public final HLEModuleFunction sceKernelDeleteFplFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDeleteFpl") {

        @Override
        public final void execute(Processor processor) {
            sceKernelDeleteFpl(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDeleteFpl(processor);";
        }
    };
    public final HLEModuleFunction sceKernelAllocateFplFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelAllocateFpl") {

        @Override
        public final void execute(Processor processor) {
            sceKernelAllocateFpl(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelAllocateFpl(processor);";
        }
    };
    public final HLEModuleFunction sceKernelAllocateFplCBFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelAllocateFplCB") {

        @Override
        public final void execute(Processor processor) {
            sceKernelAllocateFplCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelAllocateFplCB(processor);";
        }
    };
    public final HLEModuleFunction sceKernelTryAllocateFplFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelTryAllocateFpl") {

        @Override
        public final void execute(Processor processor) {
            sceKernelTryAllocateFpl(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelTryAllocateFpl(processor);";
        }
    };
    public final HLEModuleFunction sceKernelFreeFplFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelFreeFpl") {

        @Override
        public final void execute(Processor processor) {
            sceKernelFreeFpl(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelFreeFpl(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCancelFplFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCancelFpl") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCancelFpl(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCancelFpl(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferFplStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferFplStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferFplStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferFplStatus(processor);";
        }
    };
    public final HLEModuleFunction _sceKernelReturnFromTimerHandlerFunction = new HLEModuleFunction("ThreadManForUser", "_sceKernelReturnFromTimerHandler") {

        @Override
        public final void execute(Processor processor) {
            _sceKernelReturnFromTimerHandler(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule._sceKernelReturnFromTimerHandler(processor);";
        }
    };
    public final HLEModuleFunction sceKernelUSec2SysClockFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelUSec2SysClock") {

        @Override
        public final void execute(Processor processor) {
            sceKernelUSec2SysClock(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelUSec2SysClock(processor);";
        }
    };
    public final HLEModuleFunction sceKernelUSec2SysClockWideFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelUSec2SysClockWide") {

        @Override
        public final void execute(Processor processor) {
            sceKernelUSec2SysClockWide(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelUSec2SysClockWide(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSysClock2USecFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSysClock2USec") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSysClock2USec(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSysClock2USec(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSysClock2USecWideFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSysClock2USecWide") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSysClock2USecWide(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSysClock2USecWide(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetSystemTimeFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetSystemTime") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetSystemTime(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelGetSystemTime(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetSystemTimeWideFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetSystemTimeWide") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetSystemTimeWide(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelGetSystemTimeWide(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetSystemTimeLowFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetSystemTimeLow") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetSystemTimeLow(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelGetSystemTimeLow(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSetAlarmFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSetAlarm") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSetAlarm(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSetAlarm(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSetSysClockAlarmFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSetSysClockAlarm") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSetSysClockAlarm(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSetSysClockAlarm(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCancelAlarmFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCancelAlarm") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCancelAlarm(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCancelAlarm(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferAlarmStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferAlarmStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferAlarmStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferAlarmStatus(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCreateVTimerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCreateVTimer") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCreateVTimer(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCreateVTimer(processor);";
        }
    };
    public final HLEModuleFunction sceKernelDeleteVTimerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDeleteVTimer") {

        @Override
        public final void execute(Processor processor) {
            sceKernelDeleteVTimer(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDeleteVTimer(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetVTimerBaseFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetVTimerBase") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetVTimerBase(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelGetVTimerBase(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetVTimerBaseWideFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetVTimerBaseWide") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetVTimerBaseWide(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelGetVTimerBaseWide(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetVTimerTimeFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetVTimerTime") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetVTimerTime(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelGetVTimerTime(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetVTimerTimeWideFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetVTimerTimeWide") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetVTimerTimeWide(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelGetVTimerTimeWide(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSetVTimerTimeFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSetVTimerTime") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSetVTimerTime(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSetVTimerTime(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSetVTimerTimeWideFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSetVTimerTimeWide") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSetVTimerTimeWide(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSetVTimerTimeWide(processor);";
        }
    };
    public final HLEModuleFunction sceKernelStartVTimerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelStartVTimer") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStartVTimer(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelStartVTimer(processor);";
        }
    };
    public final HLEModuleFunction sceKernelStopVTimerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelStopVTimer") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStopVTimer(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelStopVTimer(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSetVTimerHandlerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSetVTimerHandler") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSetVTimerHandler(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSetVTimerHandler(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSetVTimerHandlerWideFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSetVTimerHandlerWide") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSetVTimerHandlerWide(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSetVTimerHandlerWide(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCancelVTimerHandlerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCancelVTimerHandler") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCancelVTimerHandler(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCancelVTimerHandler(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferVTimerStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferVTimerStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferVTimerStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferVTimerStatus(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCreateThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCreateThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCreateThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCreateThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelDeleteThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDeleteThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelDeleteThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDeleteThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelStartThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelStartThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStartThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelStartThread(processor);";
        }
    };
    public final HLEModuleFunction _sceKernelExitThreadFunction = new HLEModuleFunction("ThreadManForUser", "_sceKernelExitThread") {

        @Override
        public final void execute(Processor processor) {
            _sceKernelExitThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule._sceKernelExitThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelExitThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelExitThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelExitThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelExitThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelExitDeleteThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelExitDeleteThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelExitDeleteThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelExitDeleteThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelTerminateThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelTerminateThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelTerminateThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelTerminateThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelTerminateDeleteThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelTerminateDeleteThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelTerminateDeleteThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelTerminateDeleteThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSuspendDispatchThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSuspendDispatchThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSuspendDispatchThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelSuspendDispatchThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelResumeDispatchThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelResumeDispatchThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelResumeDispatchThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelResumeDispatchThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelChangeCurrentThreadAttrFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelChangeCurrentThreadAttr") {

        @Override
        public final void execute(Processor processor) {
            sceKernelChangeCurrentThreadAttr(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelChangeCurrentThreadAttr(processor);";
        }
    };
    public final HLEModuleFunction sceKernelChangeThreadPriorityFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelChangeThreadPriority") {

        @Override
        public final void execute(Processor processor) {
            sceKernelChangeThreadPriority(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelChangeThreadPriority(processor);";
        }
    };
    public final HLEModuleFunction sceKernelRotateThreadReadyQueueFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelRotateThreadReadyQueue") {

        @Override
        public final void execute(Processor processor) {
            sceKernelRotateThreadReadyQueue(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelRotateThreadReadyQueue(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReleaseWaitThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReleaseWaitThread") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReleaseWaitThread(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReleaseWaitThread(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetThreadIdFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetThreadId") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetThreadId(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelGetThreadId(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetThreadCurrentPriorityFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetThreadCurrentPriority") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetThreadCurrentPriority(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelGetThreadCurrentPriority(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetThreadExitStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetThreadExitStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetThreadExitStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelGetThreadExitStatus(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCheckThreadStackFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCheckThreadStack") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCheckThreadStack(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCheckThreadStack(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetThreadStackFreeSizeFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetThreadStackFreeSize") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetThreadStackFreeSize(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelGetThreadStackFreeSize(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferThreadStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferThreadStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferThreadStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferThreadStatus(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferThreadRunStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferThreadRunStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferThreadRunStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferThreadRunStatus(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferSystemStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferSystemStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferSystemStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferSystemStatus(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetThreadmanIdListFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetThreadmanIdList") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetThreadmanIdList(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelGetThreadmanIdList(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetThreadmanIdTypeFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetThreadmanIdType") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetThreadmanIdType(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelGetThreadmanIdType(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferThreadProfilerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferThreadProfiler") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferThreadProfiler(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferThreadProfiler(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferGlobalProfilerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferGlobalProfiler") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferGlobalProfiler(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferGlobalProfiler(processor);";
        }
    };
}