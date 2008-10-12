/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.HLE.kernel.types;

/**
 *
 * @author hli
 */
public class SceKernelErrors {

    public final static int ERROR_NOT_FOUND_THREAD = 0x80020198;
    public final static int ERROR_NOT_FOUND_SEMAPHORE = 0x80020199;
    public final static int ERROR_NOT_FOUND_EVENT_FLAG = 0x8002019a;
    public final static int ERROR_NOT_FOUND_MESSAGE_BOX = 0x8002019b;
    public final static int ERROR_NOT_FOUND_VPOOL = 0x8002019c;
    public final static int ERROR_NOT_FOUND_FPOOL = 0x8002019d;
    public final static int ERROR_NOT_FOUND_MESSAGE_PIPE = 0x8002019e;
    public final static int ERROR_NOT_FOUND_ALARM = 0x8002019f;
    public final static int ERROR_NOT_FOUND_THREAD_EVENT_HANDLER = 0x800201a0;
    public final static int ERROR_NOT_FOUND_CALLBACK = 0x800201a1;
    public final static int ERROR_THREAD_ALREADY_DORMANT = 0x800201a2;
    public final static int ERROR_THREAD_ALREADY_SUSPEND = 0x800201a3;
    public final static int ERROR_THREAD_IS_NOT_DORMANT = 0x800201a4;
    public final static int ERROR_THREAD_IS_NOT_SUSPEND = 0x800201a5;
    public final static int ERROR_THREAD_IS_NOT_WAIT = 0x800201a6;
}
