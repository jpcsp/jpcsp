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
package jpcsp.graphics.RE.externalge;

import static jpcsp.graphics.GeCommands.END;
import static jpcsp.graphics.GeCommands.FINISH;
import static jpcsp.graphics.RE.externalge.NativeUtils.INTR_STAT_END;
import static jpcsp.graphics.RE.externalge.NativeUtils.INTR_STAT_FINISH;
import static jpcsp.graphics.RE.externalge.NativeUtils.INTR_STAT_SIGNAL;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jpcsp.Memory;
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.HLE.modules.sceGe_user;
import jpcsp.graphics.GeCommands;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public class CoreThread extends Thread {
	protected static Logger log = ExternalGE.log;
	private static CoreThread instance;
	private volatile boolean exit;
	private Semaphore sync;
	private volatile boolean insideRendering;

	public static CoreThread getInstance() {
		if (instance == null) {
			instance = new CoreThread();
			instance.setDaemon(true);
			instance.setName("ExternalGE - Core Thread");
			instance.start();
		}

		return instance;
	}

	public static void exit() {
		if (instance != null) {
			instance.exit = true;
			instance = null;
		}
	}

	private CoreThread() {
		sync = new Semaphore(0);
	}

	@Override
	public void run() {
		boolean doCoreInterpret = false;

		while (!exit) {
			PspGeList list = ExternalGE.getCurrentList();

			if (list == null) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("CoreThread no current list available... waiting"));
				}

				waitForSync(100);
			} else if (doCoreInterpret || list.waitForSync(100)) {
				setInsideRendering(true);

				doCoreInterpret = false;
				NativeUtils.setCoreMadr(list.getPc());
				NativeUtils.updateMemoryUnsafeAddr();

				if (log.isDebugEnabled()) {
					log.debug(String.format("CoreThread processing %s", list));
				}

				while (NativeUtils.coreInterpret()) {
					NativeUtils.updateMemoryUnsafeAddr();

					if (log.isDebugEnabled()) {
						list.setPc(NativeUtils.getCoreMadr());
						log.debug(String.format("CoreThread looping %s", list));
					}

					if (ExternalGE.enableAsyncRendering && NativeUtils.getRendererIndexCount() > 0) {
						break;
					}
				}

				list.setPc(NativeUtils.getCoreMadr());

				int intrStat = NativeUtils.getCoreIntrStat(); 
				if ((intrStat & INTR_STAT_END) != 0) {
					if ((intrStat & INTR_STAT_SIGNAL) != 0) {
						executeCommandSIGNAL(list);
					}
					if ((intrStat & INTR_STAT_FINISH) != 0) {
						executeCommandFINISH(list);
					}
					intrStat &= ~(INTR_STAT_END | INTR_STAT_SIGNAL | INTR_STAT_FINISH);
					NativeUtils.setCoreIntrStat(intrStat);
				}

				if (ExternalGE.enableAsyncRendering && NativeUtils.getRendererIndexCount() > 0) {
					ExternalGE.render();
					doCoreInterpret = true;
				}

				setInsideRendering(false);
			}
		}

		log.info(String.format("CoreThread exited"));
	}

	public void sync() {
		if (sync != null) {
			sync.release();
		}
	}

	private boolean waitForSync(int millis) {
		while (true) {
	    	try {
	    		int availablePermits = sync.drainPermits();
	    		if (availablePermits > 0) {
	    			break;
	    		}

    			if (sync.tryAcquire(millis, TimeUnit.MILLISECONDS)) {
    				break;
    			}
				return false;
			} catch (InterruptedException e) {
				// Ignore exception and retry again
				log.debug(String.format("CoreThread waitForSync %s", e));
			}
		}

		return true;
	}

	private static int command(int instruction) {
		return instruction >>> 24;
	}

	private void executeCommandFINISH(PspGeList list) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("FINISH %s", list));
		}

		list.clearRestart();
		list.finishList();
		list.pushFinishCallback(list.id, NativeUtils.getCoreCmdArray(GeCommands.FINISH) & 0x00FFFFFF);
		list.endList();
		list.status = sceGe_user.PSP_GE_LIST_DONE;
		ExternalGE.finishList(list);
	}

	private void executeCommandSIGNAL(PspGeList list) {
		int args = NativeUtils.getCoreCmdArray(GeCommands.SIGNAL) & 0x00FFFFFF;
        int behavior = (args >> 16) & 0xFF;
        int signal = args & 0xFFFF;
        if (log.isDebugEnabled()) {
            log.debug(String.format("SIGNAL (behavior=%d, signal=0x%X)", behavior, signal));
        }

        switch (behavior) {
            case sceGe_user.PSP_GE_SIGNAL_SYNC: {
                // Skip FINISH / END
                Memory mem = Memory.getInstance();
                if (command(mem.read32(list.getPc())) == FINISH) {
                	list.readNextInstruction();
                    if (command(mem.read32(list.getPc())) == END) {
                    	list.readNextInstruction();
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug(String.format("PSP_GE_SIGNAL_SYNC ignored PC: 0x%08X", list.getPc()));
                }
                break;
            }
            case sceGe_user.PSP_GE_SIGNAL_CALL: {
                // Call list using absolute address from SIGNAL + END.
                int hi16 = signal & 0x0FFF;
                int lo16 = NativeUtils.getCoreCmdArray(GeCommands.END) & 0xFFFF;
                int addr = (hi16 << 16) | lo16;
                int oldPc = list.getPc();
                list.callAbsolute(addr);
                int newPc = list.getPc();
                if (log.isDebugEnabled()) {
                    log.debug(String.format("PSP_GE_SIGNAL_CALL old PC: 0x%08X, new PC: 0x%08X", oldPc, newPc));
                }
                break;
            }
            case sceGe_user.PSP_GE_SIGNAL_RETURN: {
                // Return from PSP_GE_SIGNAL_CALL.
                int oldPc = list.getPc();
                list.ret();
                int newPc = list.getPc();
                if (log.isDebugEnabled()) {
                    log.debug(String.format("PSP_GE_SIGNAL_RETURN old PC: 0x%08X, new PC: 0x%08X", oldPc, newPc));
                }
                break;
            }
            case sceGe_user.PSP_GE_SIGNAL_TBP0_REL:
            case sceGe_user.PSP_GE_SIGNAL_TBP1_REL:
            case sceGe_user.PSP_GE_SIGNAL_TBP2_REL:
            case sceGe_user.PSP_GE_SIGNAL_TBP3_REL:
            case sceGe_user.PSP_GE_SIGNAL_TBP4_REL:
            case sceGe_user.PSP_GE_SIGNAL_TBP5_REL:
            case sceGe_user.PSP_GE_SIGNAL_TBP6_REL:
            case sceGe_user.PSP_GE_SIGNAL_TBP7_REL: {
                // Overwrite TBPn and TBPw with SIGNAL + END (uses relative address only).
                int hi16 = signal & 0xFFFF;
                int end = NativeUtils.getCoreCmdArray(GeCommands.END);
                int lo16 = end & 0xFFFF;
                int width = (end >> 16) & 0xFF;
                int addr = list.getAddressRel((hi16 << 16) | lo16);
                int tbpValue = (behavior - sceGe_user.PSP_GE_SIGNAL_TBP0_REL + GeCommands.TBP0) << 24 |
                               (addr & 0x00FFFFFF);
                int tbwValue = (behavior - sceGe_user.PSP_GE_SIGNAL_TBP0_REL + GeCommands.TBW0) << 24 |
                               ((addr >> 8) & 0x00FF0000) | (width & 0xFFFF);
                NativeUtils.setCoreCmdArray(command(tbpValue),	tbpValue);
                NativeUtils.setCoreCmdArray(command(tbwValue),	tbwValue);
                break;
            }
            case sceGe_user.PSP_GE_SIGNAL_TBP0_REL_OFFSET:
            case sceGe_user.PSP_GE_SIGNAL_TBP1_REL_OFFSET:
            case sceGe_user.PSP_GE_SIGNAL_TBP2_REL_OFFSET:
            case sceGe_user.PSP_GE_SIGNAL_TBP3_REL_OFFSET:
            case sceGe_user.PSP_GE_SIGNAL_TBP4_REL_OFFSET:
            case sceGe_user.PSP_GE_SIGNAL_TBP5_REL_OFFSET:
            case sceGe_user.PSP_GE_SIGNAL_TBP6_REL_OFFSET:
            case sceGe_user.PSP_GE_SIGNAL_TBP7_REL_OFFSET: {
                // Overwrite TBPn and TBPw with SIGNAL + END (uses relative address with offset).
                int hi16 = signal & 0xFFFF;
                // Read & skip END
                int end = NativeUtils.getCoreCmdArray(GeCommands.END);
                int lo16 = end & 0xFFFF;
                int width = (end >> 16) & 0xFF;
                int addr = list.getAddressRelOffset((hi16 << 16) | lo16);
                int tbpValue = (behavior - sceGe_user.PSP_GE_SIGNAL_TBP0_REL + GeCommands.TBP0) << 24 |
                               (addr & 0x00FFFFFF);
                int tbwValue = (behavior - sceGe_user.PSP_GE_SIGNAL_TBP0_REL + GeCommands.TBW0) << 24 |
                               ((addr >> 8) & 0x00FF0000) | (width & 0xFFFF);
                NativeUtils.setCoreCmdArray(command(tbpValue),	tbpValue);
                NativeUtils.setCoreCmdArray(command(tbwValue),	tbwValue);
                break;
            }
            case sceGe_user.PSP_GE_SIGNAL_HANDLER_SUSPEND:
            case sceGe_user.PSP_GE_SIGNAL_HANDLER_CONTINUE:
            case sceGe_user.PSP_GE_SIGNAL_HANDLER_PAUSE: {
            	list.clearRestart();
            	list.pushSignalCallback(list.id, behavior, signal);
            	list.endList();
        		list.status = sceGe_user.PSP_GE_LIST_END_REACHED;
                break;
            }
            default: {
                if (log.isInfoEnabled()) {
                    log.warn(String.format("SIGNAL (behavior=%d, signal=0x%X) unknown behavior at 0x%08X", behavior, signal, list.getPc() - 4));
                }
            }
        }

        if (list.isDrawing()) {
        	list.sync();
        	NativeUtils.setCoreCtrlActive();
        }
	}

	public boolean isInsideRendering() {
		return insideRendering;
	}

	public void setInsideRendering(boolean insideRendering) {
		this.insideRendering = insideRendering;
	}
}
