/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.HLE;

import jpcsp.HLE.kernel.managers.*;

/**
 *
 * @author hli
 */
public class Managers {

    public static UidManager uids;
    public static CallbackManager callbacks;
    public static SemaphoreManager sempahores;
    public static EventFlagManager eventsFlags;
    public static ThreadManager threads;
    //public static ModulesManager modules;

    static {
        uids = UidManager.singleton;
        callbacks = CallbackManager.singleton;
        sempahores = SemaphoreManager.singleton;
        eventsFlags = EventFlagManager.singleton;
        threads = ThreadManager.singleton;
        //modules = ModuleManager.singleton;
    }
}
