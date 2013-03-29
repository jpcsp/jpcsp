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

public class SceKernelErrors {
    /*
     * PSP Errors:
     * Represented by a 32-bit value with the following scheme:
     *
     *  31  30  29  28  27        16  15        0
     * | 1 | 0 | 0 | 0 | X | ... | X | E |... | E |
     *
     * Bits 31 and 30: Can only be 1 or 0.
     *      -> If both are 0, there's no error (0x0==SUCCESS).
     *      -> If 31 is 1 but 30 is 0, there's an error (0x80000000).
     *      -> If both bits are 1, a critical error stops the PSP (0xC0000000).
     *
     * Bits 29 and 28: Unknown. Never change.
     *
     * Bits 27 to 16 (X): Represent the system area associated with the error.
     *      -> 0x000 - Null (can be used anywhere).
     *      -> 0x001 - Errno (PSP's implementation of errno.h).
     *      -> 0x002 - Kernel.
     *      -> 0x011 - Utility.
     *      -> 0x021 - UMD.
     *      -> 0x022 - MemStick.
     *      -> 0x026 - Audio.
     *      -> 0x02b - Power.
     *      -> 0x041 - Wlan.
     *      -> 0x042 - SAS.
     *      -> 0x043 - HTTP(0x0431)/HTTPS/SSL(0x0435).
     *      -> 0x044 - WAVE.
     *      -> 0x046 - Font.
     *      -> 0x061 - MPEG(0x0618)/PSMF(0x0615)/PSMF Player(0x0616).
     *      -> 0x062 - AVC.
     *      -> 0x063 - ATRAC.
     *      -> 0x07f - Codec.
     *
     * Bits 15 to 0 (E): Represent the error code itself (different for each area).
     *      -> E.g.: 0x80110001 - Error -> Utility -> Some unknown error.
     */

    public static final int ERROR_ALREADY                                       = 0x80000020;
    public static final int ERROR_BUSY                                          = 0x80000021;
    public static final int ERROR_OUT_OF_MEMORY                                 = 0x80000022;

	public static final int ERROR_INVALID_ID                                    = 0x80000100;
    public static final int ERROR_INVALID_NAME                                  = 0x80000101;
    public static final int ERROR_INVALID_INDEX                                 = 0x80000102;
    public static final int ERROR_INVALID_POINTER                               = 0x80000103;
    public static final int ERROR_INVALID_SIZE                                  = 0x80000104;
    public static final int ERROR_INVALID_FLAG                                  = 0x80000105;
    public static final int ERROR_INVALID_COMMAND                               = 0x80000106;
    public static final int ERROR_INVALID_MODE                                  = 0x80000107;
    public static final int ERROR_INVALID_FORMAT                                = 0x80000108;
    public static final int ERROR_INVALID_VALUE                                 = 0x800001FE;
    public static final int ERROR_INVALID_ARGUMENT                              = 0x800001FF;

    public static final int ERROR_BAD_FILE                                      = 0x80000209;
    public static final int ERROR_ACCESS_ERROR                                  = 0x8000020D;

    public final static int ERROR_ERRNO_OPERATION_NOT_PERMITTED                 = 0x80010001;
    public final static int ERROR_ERRNO_FILE_NOT_FOUND                          = 0x80010002;
    public final static int ERROR_ERRNO_FILE_OPEN_ERROR                         = 0x80010003;
    public final static int ERROR_ERRNO_IO_ERROR                                = 0x80010005;
    public final static int ERROR_ERRNO_ARG_LIST_TOO_LONG                       = 0x80010007;
    public final static int ERROR_ERRNO_INVALID_FILE_DESCRIPTOR                 = 0x80010009;
    public final static int ERROR_ERRNO_RESOURCE_UNAVAILABLE                    = 0x8001000B;
    public final static int ERROR_ERRNO_NO_MEMORY                               = 0x8001000C;
    public final static int ERROR_ERRNO_NO_PERM                                 = 0x8001000D;
    public final static int ERROR_ERRNO_FILE_INVALID_ADDR                       = 0x8001000E;
    public final static int ERROR_ERRNO_DEVICE_BUSY                             = 0x80010010;
    public final static int ERROR_ERRNO_FILE_ALREADY_EXISTS                     = 0x80010011;
    public final static int ERROR_ERRNO_CROSS_DEV_LINK                          = 0x80010012;
    public final static int ERROR_ERRNO_DEVICE_NOT_FOUND                        = 0x80010013;
    public final static int ERROR_ERRNO_NOT_A_DIRECTORY                         = 0x80010014;
    public final static int ERROR_ERRNO_IS_DIRECTORY                            = 0x80010015;
    public final static int ERROR_ERRNO_INVALID_ARGUMENT                        = 0x80010016;
    public final static int ERROR_ERRNO_TOO_MANY_OPEN_SYSTEM_FILES              = 0x80010018;
    public final static int ERROR_ERRNO_FILE_IS_TOO_BIG                         = 0x8001001B;
    public final static int ERROR_ERRNO_DEVICE_NO_FREE_SPACE                    = 0x8001001C;
    public final static int ERROR_ERRNO_READ_ONLY                               = 0x8001001E;
    public final static int ERROR_ERRNO_CLOSED                                  = 0x80010020;
    public final static int ERROR_ERRNO_FILE_PATH_TOO_LONG                      = 0x80010024;
    public final static int ERROR_ERRNO_FILE_PROTOCOL                           = 0x80010047;
    public final static int ERROR_ERRNO_DIRECTORY_IS_NOT_EMPTY                  = 0x8001005A;
    public final static int ERROR_ERRNO_TOO_MANY_SYMBOLIC_LINKS                 = 0x8001005C;
    public final static int ERROR_ERRNO_FILE_ADDR_IN_USE                        = 0x80010062;
    public final static int ERROR_ERRNO_CONNECTION_ABORTED                      = 0x80010067;
    public final static int ERROR_ERRNO_CONNECTION_RESET                        = 0x80010068;
    public final static int ERROR_ERRNO_NO_FREE_BUF_SPACE                       = 0x80010069;
    public final static int ERROR_ERRNO_FILE_TIMEOUT                            = 0x8001006E;
    public final static int ERROR_ERRNO_IN_PROGRESS                             = 0x80010077;
    public final static int ERROR_ERRNO_ALREADY                                 = 0x80010078;
    public final static int ERROR_ERRNO_NO_MEDIA                                = 0x8001007B;
    public final static int ERROR_ERRNO_INVALID_MEDIUM                          = 0x8001007C;
    public final static int ERROR_ERRNO_ADDRESS_NOT_AVAILABLE                   = 0x8001007D;
    public final static int ERROR_ERRNO_IS_ALREADY_CONNECTED                    = 0x8001007F;    
    public final static int ERROR_ERRNO_NOT_CONNECTED                           = 0x80010080;
    public final static int ERROR_ERRNO_FILE_QUOTA_EXCEEDED                     = 0x80010084;
    public final static int ERROR_ERRNO_FUNCTION_NOT_SUPPORTED                  = 0x8001B000;
    public final static int ERROR_ERRNO_ADDR_OUT_OF_MAIN_MEM                    = 0x8001B001;
    public final static int ERROR_ERRNO_INVALID_UNIT_NUM                        = 0x8001B002;
    public final static int ERROR_ERRNO_INVALID_FILE_SIZE                       = 0x8001B003;
    public final static int ERROR_ERRNO_INVALID_FLAG                            = 0x8001B004;

    public final static int ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT        = 0x80020064;
    public final static int ERROR_KERNEL_INTERRUPTS_ALREADY_DISABLED            = 0x80020066;
    public final static int ERROR_KERNEL_UNKNOWN_UID                            = 0x800200cb;
    public final static int ERROR_KERNEL_UNMATCH_TYPE_UID                       = 0x800200cc;
    public final static int ERROR_KERNEL_NOT_EXIST_ID                           = 0x800200cd;
    public final static int ERROR_KERNEL_NOT_FOUND_FUNCTION_UID                 = 0x800200ce;
    public final static int ERROR_KERNEL_ALREADY_HOLDER_UID                     = 0x800200cf;
    public final static int ERROR_KERNEL_NOT_HOLDER_UID                         = 0x800200d0;
    public final static int ERROR_KERNEL_ILLEGAL_PERMISSION                     = 0x800200d1;
    public final static int ERROR_KERNEL_ILLEGAL_ARGUMENT                       = 0x800200d2;
    public final static int ERROR_KERNEL_ILLEGAL_ADDR                           = 0x800200d3;
    public final static int ERROR_KERNEL_OUT_OF_RANGE                           = 0x800200d4;
    public final static int ERROR_KERNEL_MEMORY_AREA_IS_OVERLAP                 = 0x800200d5;
    public final static int ERROR_KERNEL_ILLEGAL_PARTITION_ID                   = 0x800200d6;
    public final static int ERROR_KERNEL_PARTITION_IN_USE                       = 0x800200d7;
    public final static int ERROR_KERNEL_ILLEGAL_MEMBLOCK_ALLOC_TYPE            = 0x800200d8;
    public final static int ERROR_KERNEL_FAILED_ALLOC_MEMBLOCK                  = 0x800200d9;
    public final static int ERROR_KERNEL_INHIBITED_RESIZE_MEMBLOCK              = 0x800200da;
    public final static int ERROR_KERNEL_FAILED_RESIZE_MEMBLOCK                 = 0x800200db;
    public final static int ERROR_KERNEL_FAILED_ALLOC_HEAPBLOCK                 = 0x800200dc;
    public final static int ERROR_KERNEL_FAILED_ALLOC_HEAP                      = 0x800200dd;
    public final static int ERROR_KERNEL_ILLEGAL_CHUNK_ID                       = 0x800200de;
    public final static int ERROR_KERNEL_CANNOT_FIND_CHUNK_NAME                 = 0x800200df;
    public final static int ERROR_KERNEL_NO_FREE_CHUNK                          = 0x800200e0;
    public final static int ERROR_KERNEL_MEMBLOCK_FRAGMENTED                    = 0x800200e1;
    public final static int ERROR_KERNEL_MEMBLOCK_CANNOT_JOINT                  = 0x800200e2;
    public final static int ERROR_KERNEL_MEMBLOCK_CANNOT_SEPARATE               = 0x800200e3;
    public final static int ERROR_KERNEL_ILLEGAL_ALIGNMENT_SIZE                 = 0x800200e4;
    public final static int ERROR_KERNEL_ILLEGAL_DEVKIT_VER                     = 0x800200e5;

    public final static int ERROR_KERNEL_MODULE_LINK_ERROR                      = 0x8002012c;
    public final static int ERROR_KERNEL_ILLEGAL_OBJECT_FORMAT                  = 0x8002012d;
    public final static int ERROR_KERNEL_UNKNOWN_MODULE                         = 0x8002012e;
    public final static int ERROR_KERNEL_UNKNOWN_MODULE_FILE                    = 0x8002012f;
    public final static int ERROR_KERNEL_FILE_READ_ERROR                        = 0x80020130;
    public final static int ERROR_KERNEL_MEMORY_IN_USE                          = 0x80020131;
    public final static int ERROR_KERNEL_PARTITION_MISMATCH                     = 0x80020132;
    public final static int ERROR_KERNEL_MODULE_ALREADY_STARTED                 = 0x80020133;
    public final static int ERROR_KERNEL_MODULE_NOT_STARTED                     = 0x80020134;
    public final static int ERROR_KERNEL_MODULE_ALREADY_STOPPED                 = 0x80020135;
    public final static int ERROR_KERNEL_MODULE_CANNOT_STOP                     = 0x80020136;
    public final static int ERROR_KERNEL_MODULE_NOT_STOPPED                     = 0x80020137;
    public final static int ERROR_KERNEL_MODULE_CANNOT_REMOVE                   = 0x80020138;
    public final static int ERROR_KERNEL_EXCLUSIVE_LOAD                         = 0x80020139;
    public final static int ERROR_KERNEL_LIBRARY_IS_NOT_LINKED                  = 0x8002013a;
    public final static int ERROR_KERNEL_LIBRARY_ALREADY_EXISTS                 = 0x8002013b;
    public final static int ERROR_KERNEL_LIBRARY_NOT_FOUND                      = 0x8002013c;
    public final static int ERROR_KERNEL_ILLEGAL_LIBRARY_HEADER                 = 0x8002013d;
    public final static int ERROR_KERNEL_LIBRARY_IN_USE                         = 0x8002013e;
    public final static int ERROR_KERNEL_MODULE_ALREADY_STOPPING                = 0x8002013f;
    public final static int ERROR_KERNEL_ILLEGAL_OFFSET_VALUE                   = 0x80020140;
    public final static int ERROR_KERNEL_ILLEGAL_POSITION_CODE                  = 0x80020141;
    public final static int ERROR_KERNEL_ILLEGAL_ACCESS_CODE                    = 0x80020142;
    public final static int ERROR_KERNEL_MODULE_MANAGER_BUSY                    = 0x80020143;
    public final static int ERROR_KERNEL_ILLEGAL_FLAG                           = 0x80020144;
    public final static int ERROR_KERNEL_CANNOT_GET_MODULE_LIST                 = 0x80020145;
    public final static int ERROR_KERNEL_PROHIBIT_LOADMODULE_DEVICE             = 0x80020146;
    public final static int ERROR_KERNEL_PROHIBIT_LOADEXEC_DEVICE               = 0x80020147;
    public final static int ERROR_KERNEL_UNSUPPORTED_PRX_TYPE                   = 0x80020148;
    public final static int ERROR_KERNEL_ILLEGAL_PERMISSION_CALL                = 0x80020149;
    public final static int ERROR_KERNEL_CANNOT_GET_MODULE_INFO                 = 0x8002014a;
    public final static int ERROR_KERNEL_ILLEGAL_LOADEXEC_BUFFER                = 0x8002014b;
    public final static int ERROR_KERNEL_ILLEGAL_LOADEXEC_FILENAME              = 0x8002014c;
    public final static int ERROR_KERNEL_NO_EXIT_CALLBACK                       = 0x8002014d;
    public final static int ERROR_KERNEL_MEDIA_CHANGED                          = 0x8002014e;
    public final static int ERROR_KERNEL_CANNOT_USE_BETA_VER_MODULE             = 0x8002014f;

    public final static int ERROR_KERNEL_NO_MEMORY                              = 0x80020190;
    public final static int ERROR_KERNEL_ILLEGAL_ATTR                           = 0x80020191;
    public final static int ERROR_KERNEL_ILLEGAL_THREAD_ENTRY_ADDR              = 0x80020192;
    public final static int ERROR_KERNEL_ILLEGAL_PRIORITY                       = 0x80020193;
    public final static int ERROR_KERNEL_ILLEGAL_STACK_SIZE                     = 0x80020194;
    public final static int ERROR_KERNEL_ILLEGAL_MODE                           = 0x80020195;
    public final static int ERROR_KERNEL_ILLEGAL_MASK                           = 0x80020196;
    public final static int ERROR_KERNEL_ILLEGAL_THREAD                         = 0x80020197;
    public final static int ERROR_KERNEL_NOT_FOUND_THREAD                       = 0x80020198;
    public final static int ERROR_KERNEL_NOT_FOUND_SEMAPHORE                    = 0x80020199;
    public final static int ERROR_KERNEL_NOT_FOUND_EVENT_FLAG                   = 0x8002019a;
    public final static int ERROR_KERNEL_NOT_FOUND_MESSAGE_BOX                  = 0x8002019b;
    public final static int ERROR_KERNEL_NOT_FOUND_VPOOL                        = 0x8002019c;
    public final static int ERROR_KERNEL_NOT_FOUND_FPOOL                        = 0x8002019d;
    public final static int ERROR_KERNEL_NOT_FOUND_MESSAGE_PIPE                 = 0x8002019e;
    public final static int ERROR_KERNEL_NOT_FOUND_ALARM                        = 0x8002019f;
    public final static int ERROR_KERNEL_NOT_FOUND_THREAD_EVENT_HANDLER         = 0x800201a0;
    public final static int ERROR_KERNEL_NOT_FOUND_CALLBACK                     = 0x800201a1;
    public final static int ERROR_KERNEL_THREAD_ALREADY_DORMANT                 = 0x800201a2;
    public final static int ERROR_KERNEL_THREAD_ALREADY_SUSPEND                 = 0x800201a3;
    public final static int ERROR_KERNEL_THREAD_IS_NOT_DORMANT                  = 0x800201a4;
    public final static int ERROR_KERNEL_THREAD_IS_NOT_SUSPEND                  = 0x800201a5;
    public final static int ERROR_KERNEL_THREAD_IS_NOT_WAIT                     = 0x800201a6;
    public final static int ERROR_KERNEL_WAIT_CAN_NOT_WAIT                      = 0x800201a7;
    public final static int ERROR_KERNEL_WAIT_TIMEOUT                           = 0x800201a8;
    public final static int ERROR_KERNEL_WAIT_CANCELLED                         = 0x800201a9;
    public final static int ERROR_KERNEL_WAIT_STATUS_RELEASED                   = 0x800201aa;
    public final static int ERROR_KERNEL_WAIT_STATUS_RELEASED_CALLBACK          = 0x800201ab;
    public final static int ERROR_KERNEL_THREAD_IS_TERMINATED                   = 0x800201ac;
    public final static int ERROR_KERNEL_SEMA_ZERO                              = 0x800201ad;
    public final static int ERROR_KERNEL_SEMA_OVERFLOW                          = 0x800201ae;
    public final static int ERROR_KERNEL_EVENT_FLAG_POLL_FAILED                 = 0x800201af;
    public final static int ERROR_KERNEL_EVENT_FLAG_NO_MULTI_PERM               = 0x800201b0;
    public final static int ERROR_KERNEL_EVENT_FLAG_ILLEGAL_WAIT_PATTERN        = 0x800201b1;
    public final static int ERROR_KERNEL_MESSAGEBOX_NO_MESSAGE                  = 0x800201b2;
    public final static int ERROR_KERNEL_MESSAGE_PIPE_FULL                      = 0x800201b3;
    public final static int ERROR_KERNEL_MESSAGE_PIPE_EMPTY                     = 0x800201b4;
    public final static int ERROR_KERNEL_WAIT_DELETE                            = 0x800201b5;
    public final static int ERROR_KERNEL_ILLEGAL_MEMBLOCK                       = 0x800201b6;
    public final static int ERROR_KERNEL_ILLEGAL_MEMSIZE                        = 0x800201b7;
    public final static int ERROR_KERNEL_ILLEGAL_SCRATCHPAD_ADDR                = 0x800201b8;
    public final static int ERROR_KERNEL_SCRATCHPAD_IN_USE                      = 0x800201b9;
    public final static int ERROR_KERNEL_SCRATCHPAD_NOT_IN_USE                  = 0x800201ba;
    public final static int ERROR_KERNEL_ILLEGAL_TYPE                           = 0x800201bb;
    public final static int ERROR_KERNEL_ILLEGAL_SIZE                           = 0x800201bc;
    public final static int ERROR_KERNEL_ILLEGAL_COUNT                          = 0x800201bd;
    public final static int ERROR_KERNEL_NOT_FOUND_VTIMER                       = 0x800201be;
    public final static int ERROR_KERNEL_ILLEGAL_VTIMER                         = 0x800201bf;
    public final static int ERROR_KERNEL_ILLEGAL_KTLS                           = 0x800201c0;
    public final static int ERROR_KERNEL_KTLS_IS_FULL                           = 0x800201c1;
    public final static int ERROR_KERNEL_KTLS_IS_BUSY                           = 0x800201c2;
    public final static int ERROR_KERNEL_MUTEX_NOT_FOUND                        = 0x800201c3;
    public final static int ERROR_KERNEL_MUTEX_LOCKED                           = 0x800201c4;
    public final static int ERROR_KERNEL_MUTEX_UNLOCKED                         = 0x800201c5;
    public final static int ERROR_KERNEL_MUTEX_LOCK_OVERFLOW                    = 0x800201c6;
    public final static int ERROR_KERNEL_MUTEX_UNLOCK_UNDERFLOW                 = 0x800201c7;
    public final static int ERROR_KERNEL_MUTEX_RECURSIVE_NOT_ALLOWED            = 0x800201c8;
    public final static int ERROR_KERNEL_MESSAGEBOX_DUPLICATE_MESSAGE           = 0x800201c9;
    public final static int ERROR_KERNEL_LWMUTEX_NOT_FOUND                      = 0x800201ca;
    public final static int ERROR_KERNEL_LWMUTEX_LOCKED                         = 0x800201cb;
    public final static int ERROR_KERNEL_LWMUTEX_UNLOCKED                       = 0x800201cc;
    public final static int ERROR_KERNEL_LWMUTEX_LOCK_OVERFLOW                  = 0x800201cd;
    public final static int ERROR_KERNEL_LWMUTEX_UNLOCK_UNDERFLOW               = 0x800201ce;
    public final static int ERROR_KERNEL_LWMUTEX_RECURSIVE_NOT_ALLOWED          = 0x800201cf;

    public final static int ERROR_KERNEL_POWER_CANNOT_CANCEL                    = 0x80020261;

    public final static int ERROR_KERNEL_TOO_MANY_OPEN_FILES                    = 0x80020320;
    public final static int ERROR_KERNEL_NO_SUCH_DEVICE                         = 0x80020321;
    public final static int ERROR_KERNEL_BAD_FILE_DESCRIPTOR                    = 0x80020323;
    public final static int ERROR_KERNEL_UNSUPPORTED_OPERATION                  = 0x80020325;
    public final static int ERROR_KERNEL_NOCWD                                  = 0x8002032c;
    public final static int ERROR_KERNEL_FILENAME_TOO_LONG                      = 0x8002032d;
    public final static int ERROR_KERNEL_ASYNC_BUSY                             = 0x80020329;
    public final static int ERROR_KERNEL_NO_ASYNC_OP                            = 0x8002032a;

    public final static int ERROR_KERNEL_NOT_CACHE_ALIGNED                      = 0x8002044c;
    public final static int ERROR_KERNEL_MAX_ERROR                              = 0x8002044d;

    public static final int ERROR_UTILITY_INVALID_STATUS                        = 0x80110001;
    public static final int ERROR_UTILITY_INVALID_PARAM_ADDR                    = 0x80110002;
    public static final int ERROR_UTILITY_IS_UNKNOWN                            = 0x80110003;
    public static final int ERROR_UTILITY_INVALID_PARAM_SIZE                    = 0x80110004;
    public static final int ERROR_UTILITY_WRONG_TYPE                            = 0x80110005;
    public static final int ERROR_UTILITY_MODULE_NOT_FOUND                      = 0x80110006;

    public static final int ERROR_UTILITY_INVALID_SYSTEM_PARAM_ID               = 0x80110103;
    public static final int ERROR_UTILITY_INVALID_ADHOC_CHANNEL                 = 0x80110104;

    public static final int ERROR_SAVEDATA_LOAD_NO_MEMSTICK                     = 0x80110301;
    public static final int ERROR_SAVEDATA_LOAD_MEMSTICK_REMOVED                = 0x80110302;
    public static final int ERROR_SAVEDATA_LOAD_ACCESS_ERROR                    = 0x80110305;
    public static final int ERROR_SAVEDATA_LOAD_DATA_BROKEN                     = 0x80110306;
    public static final int ERROR_SAVEDATA_LOAD_NO_DATA                         = 0x80110307;
    public static final int ERROR_SAVEDATA_LOAD_BAD_PARAMS                      = 0x80110308;
    public static final int ERROR_SAVEDATA_LOAD_NO_UMD                          = 0x80110309;
    public static final int ERROR_SAVEDATA_LOAD_INTERNAL_ERROR                  = 0x80110309;

    public static final int ERROR_SAVEDATA_RW_NO_MEMSTICK                       = 0x80110321;
    public static final int ERROR_SAVEDATA_RW_MEMSTICK_REMOVED                  = 0x80110322;
    public static final int ERROR_SAVEDATA_RW_MEMSTICK_FULL                     = 0x80110323;
    public static final int ERROR_SAVEDATA_RW_MEMSTICK_PROTECTED                = 0x80110324;
    public static final int ERROR_SAVEDATA_RW_ACCESS_ERROR                      = 0x80110325;
    public static final int ERROR_SAVEDATA_RW_DATA_BROKEN                       = 0x80110326;
    public static final int ERROR_SAVEDATA_RW_NO_DATA                           = 0x80110327;
    public static final int ERROR_SAVEDATA_RW_BAD_PARAMS                        = 0x80110328;
    public static final int ERROR_SAVEDATA_RW_FILE_NOT_FOUND                    = 0x80110329;
    public static final int ERROR_SAVEDATA_RW_CAN_NOT_SUSPEND                   = 0x8011032a;
    public static final int ERROR_SAVEDATA_RW_INTERNAL_ERROR                    = 0x8011032b;
    public static final int ERROR_SAVEDATA_RW_BAD_STATUS                        = 0x8011032c;
    public static final int ERROR_SAVEDATA_RW_SECURE_FILE_FULL                  = 0x8011032d;

    public static final int ERROR_SAVEDATA_DELETE_NO_MEMSTICK                   = 0x80110341;
    public static final int ERROR_SAVEDATA_DELETE_MEMSTICK_REMOVED              = 0x80110342;
    public static final int ERROR_SAVEDATA_DELETE_MEMSTICK_PROTECTED            = 0x80110344;
    public static final int ERROR_SAVEDATA_DELETE_ACCESS_ERROR                  = 0x80110345;
    public static final int ERROR_SAVEDATA_DELETE_DATA_BROKEN                   = 0x80110346;
    public static final int ERROR_SAVEDATA_DELETE_NO_DATA                       = 0x80110347;
    public static final int ERROR_SAVEDATA_DELETE_BAD_PARAMS                    = 0x80110348;
    public static final int ERROR_SAVEDATA_DELETE_INTERNAL_ERROR                = 0x8011034b;

    public static final int ERROR_SAVEDATA_SAVE_NO_MEMSTICK                     = 0x80110381;
    public static final int ERROR_SAVEDATA_SAVE_MEMSTICK_REMOVED                = 0x80110382;
    public static final int ERROR_SAVEDATA_SAVE_NO_SPACE                        = 0x80110383;
    public static final int ERROR_SAVEDATA_SAVE_MEMSTICK_PROTECTED              = 0x80110384;
    public static final int ERROR_SAVEDATA_SAVE_ACCESS_ERROR                    = 0x80110385;
    public static final int ERROR_SAVEDATA_SAVE_DATA_BROKEN                     = 0x80110386;
    public static final int ERROR_SAVEDATA_SAVE_BAD_PARAMS                      = 0x80110388;
    public static final int ERROR_SAVEDATA_SAVE_NO_UMD                          = 0x80110389;
    public static final int ERROR_SAVEDATA_SAVE_WRONG_UMD                       = 0x8011038a;
    public static final int ERROR_SAVEDATA_SAVE_INTERNAL_ERROR                  = 0x8011038b;

    public static final int ERROR_SAVEDATA_SIZES_NO_MEMSTICK                    = 0x801103c1;
    public static final int ERROR_SAVEDATA_SIZES_MEMSTICK_REMOVED               = 0x801103c2;
    public static final int ERROR_SAVEDATA_SIZES_ACCESS_ERROR                   = 0x801103c5;
    public static final int ERROR_SAVEDATA_SIZES_DATA_BROKEN                    = 0x801103c6;
    public static final int ERROR_SAVEDATA_SIZES_NO_DATA                        = 0x801103c7;
    public static final int ERROR_SAVEDATA_SIZES_BAD_PARAMS                     = 0x801103c8;
    public static final int ERROR_SAVEDATA_SIZES_INTERNAL_ERROR                 = 0x801103cb;

    public static final int ERROR_NETPARAM_BAD_NETCONF                          = 0x80110601;
    public static final int ERROR_NETPARAM_BAD_PARAM                            = 0x80110604;

    public static final int ERROR_NET_MODULE_BAD_ID                             = 0x80110801;
    public static final int ERROR_NET_MODULE_ALREADY_LOADED                     = 0x80110802;
    public static final int ERROR_NET_MODULE_NOT_LOADED                         = 0x80110803;

    public static final int ERROR_AV_MODULE_BAD_ID                              = 0x80110F01;
    public static final int ERROR_AV_MODULE_ALREADY_LOADED                      = 0x80110F02;
    public static final int ERROR_AV_MODULE_NOT_LOADED                          = 0x80110F03;

    public static final int ERROR_MODULE_BAD_ID                                 = 0x80111101;
    public static final int ERROR_MODULE_ALREADY_LOADED                         = 0x80111102;
    public static final int ERROR_MODULE_NOT_LOADED                             = 0x80111103;

    public static final int ERROR_SCREENSHOT_CONT_MODE_NOT_INIT                 = 0x80111229;

    public final static int ERROR_UMD_NOT_READY                                 = 0x80210001;
    public final static int ERROR_UMD_LBA_OUT_OF_BOUNDS                         = 0x80210002;
    public final static int ERROR_UMD_NO_DISC                                   = 0x80210003;

    public final static int ERROR_MEMSTICK_DEVCTL_BAD_PARAMS                    = 0x80220081;
    public final static int ERROR_MEMSTICK_DEVCTL_TOO_MANY_CALLBACKS            = 0x80220082;

    public final static int ERROR_USBMIC_INVALID_MAX_SAMPLES                    = 0x80243806;
    public final static int ERROR_USBMIC_INVALID_FREQUENCY                      = 0x8024380A;

    public final static int ERROR_AUDIO_CHANNEL_NOT_INIT                        = 0x80260001;
    public final static int ERROR_AUDIO_CHANNEL_BUSY                            = 0x80260002;
    public final static int ERROR_AUDIO_INVALID_CHANNEL                         = 0x80260003;
    public final static int ERROR_AUDIO_PRIV_REQUIRED                           = 0x80260004;
    public final static int ERROR_AUDIO_NO_CHANNELS_AVAILABLE                   = 0x80260005;
    public final static int ERROR_AUDIO_OUTPUT_SAMPLE_DATA_SIZE_NOT_ALIGNED     = 0x80260006;
    public final static int ERROR_AUDIO_INVALID_FORMAT                          = 0x80260007;
    public final static int ERROR_AUDIO_CHANNEL_NOT_RESERVED                    = 0x80260008;
    public final static int ERROR_AUDIO_NOT_OUTPUT                              = 0x80260009;
    public final static int ERROR_AUDIO_INVALID_FREQUENCY                       = 0x8026000A;
    public final static int ERROR_AUDIO_CHANNEL_ALREADY_RESERVED                = 0x80268002;

    public final static int ERROR_POWER_VMEM_IN_USE                             = 0x802b0200;

    public final static int ERROR_NET_BUFFER_TOO_SMALL                          = 0x80400706;

    public final static int ERROR_NET_RESOLVER_BAD_ID                           = 0x80410408;
    public final static int ERROR_NET_RESOLVER_ALREADY_STOPPED                  = 0x8041040a;
    public final static int ERROR_NET_RESOLVER_INVALID_HOST                     = 0x80410414;

    public final static int ERROR_NET_ADHOC_INVALID_SOCKET_ID                   = 0x80410701;
    public final static int ERROR_NET_ADHOC_INVALID_ADDR                        = 0x80410702;
    public final static int ERROR_NET_ADHOC_NO_DATA_AVAILABLE                   = 0x80410709;
    public final static int ERROR_NET_ADHOC_PORT_IN_USE                         = 0x8041070a;
    public final static int ERROR_NET_ADHOC_NOT_INITIALIZED                     = 0x80410712;
    public final static int ERROR_NET_ADHOC_ALREADY_INITIALIZED                 = 0x80410713;
    public final static int ERROR_NET_ADHOC_DISCONNECTED                        = 0x8041070c;
    public final static int ERROR_NET_ADHOC_TIMEOUT                             = 0x80410715;
    public final static int ERROR_NET_ADHOC_NO_ENTRY                            = 0x80410716;
    public final static int ERROR_NET_ADHOC_CONNECTION_REFUSED                  = 0x80410718;

    public final static int ERROR_NET_ADHOC_INVALID_MATCHING_ID                 = 0x80410807;
    public final static int ERROR_NET_ADHOC_MATCHING_ALREADY_INITIALIZED        = 0x80410812;
    public final static int ERROR_NET_ADHOC_MATCHING_NOT_INITIALIZED            = 0x80410813;

    public final static int ERROR_NET_ADHOCCTL_ALREADY_INITIALIZED              = 0x80410b07;
    public final static int ERROR_NET_ADHOCCTL_NOT_INITIALIZED                  = 0x80410b08;
    public final static int ERROR_NET_ADHOCCTL_TOO_MANY_HANDLERS                = 0x80410b12;

    public final static int ERROR_WLAN_BAD_PARAMS                               = 0x80410d13;

    public final static int ERROR_SAS_INVALID_VOICE                             = 0x80420010;
    public final static int ERROR_SAS_INVALID_ADSR_CURVE_MODE                   = 0x80420013;
    public final static int ERROR_SAS_INVALID_PARAMETER                         = 0x80420014;
    public final static int ERROR_SAS_VOICE_PAUSED                              = 0x80420016;
    public final static int ERROR_SAS_INVALID_SIZE                              = 0x8042001A;
    public final static int ERROR_SAS_BUSY                                      = 0x80420030;
    public final static int ERROR_SAS_NOT_INIT                                  = 0x80420100;

    public final static int ERROR_HTTP_NOT_INIT                                 = 0x80431001;
    public final static int ERROR_HTTP_ALREADY_INIT                             = 0x80431020;
    public final static int ERROR_HTTP_NO_MEMORY                                = 0x80431077;
    public final static int ERROR_HTTP_SYSTEM_COOKIE_NOT_LOADED                 = 0x80431078;
    public final static int ERROR_HTTP_INVALID_PARAMETER                        = 0x804311FE;

    public final static int ERROR_SSL_NOT_INIT                                  = 0x80435001;
    public final static int ERROR_SSL_ALREADY_INIT                              = 0x80435020;
    public final static int ERROR_SSL_OUT_OF_MEMORY                             = 0x80435022;
    public final static int ERROR_HTTPS_CERT_ERROR                              = 0x80435060;
    public final static int ERROR_HTTPS_HANDSHAKE_ERROR                         = 0x80435061;
    public final static int ERROR_HTTPS_IO_ERROR                                = 0x80435062;
    public final static int ERROR_HTTPS_INTERNAL_ERROR                          = 0x80435063;
    public final static int ERROR_HTTPS_PROXY_ERROR                             = 0x80435064;
    public final static int ERROR_SSL_INVALID_PARAMETER                         = 0x804351FE;

    public final static int ERROR_WAVE_NOT_INIT                                 = 0x80440001;
    public final static int ERROR_WAVE_FAILED_EXIT                              = 0x80440002;
    public final static int ERROR_WAVE_BAD_VOL                                  = 0x8044000a;
    public final static int ERROR_WAVE_INVALID_CHANNEL                          = 0x80440010;
    public final static int ERROR_WAVE_INVALID_SAMPLE_COUNT                     = 0x80440011;

    public final static int ERROR_FONT_INVALID_LIBID                            = 0x80460002;
    public final static int ERROR_FONT_INVALID_PARAMETER                        = 0x80460003;
    public final static int ERROR_FONT_TOO_MANY_OPEN_FONTS                      = 0x80460009;

    public final static int ERROR_PGD_INVALID_HEADER                            = 0x80510204;

    public final static int ERROR_NPAUTH_NOT_INIT                               = 0x80550302;

    public final static int ERROR_NPSERVICE_NOT_INIT                            = 0x80550502;

    public final static int ERROR_MPEG_BAD_VERSION                              = 0x80610002;
    public final static int ERROR_MPEG_NO_MEMORY                                = 0x80610022;
    public final static int ERROR_MPEG_INVALID_ADDR                             = 0x80610103;
    public final static int ERROR_MPEG_INVALID_VALUE                            = 0x806101fe;

    public final static int ERROR_PSMF_NOT_INITIALIZED                          = 0x80615001;
    public final static int ERROR_PSMF_BAD_VERSION                              = 0x80615002;
    public final static int ERROR_PSMF_NOT_FOUND                                = 0x80615025;
    public final static int ERROR_PSMF_INVALID_ID                               = 0x80615100;
    public final static int ERROR_PSMF_INVALID_VALUE                            = 0x806151fe;
    public final static int ERROR_PSMF_INVALID_TIMESTAMP                        = 0x80615500;
    public final static int ERROR_PSMF_INVALID_PSMF                             = 0x80615501;

    public final static int ERROR_PSMFPLAYER_NOT_INITIALIZED                    = 0x80616001;
    public final static int ERROR_PSMFPLAYER_INVALID_CONFIG_MODE                = 0x80616006;
    public final static int ERROR_PSMFPLAYER_INVALID_CONFIG_VALUE               = 0x80616008;
    public final static int ERROR_PSMFPLAYER_NO_MORE_DATA                       = 0x8061600c;

    public final static int ERROR_MP4_NO_MORE_DATA                              = 0x8061700a;

    public final static int ERROR_MPEG_NO_DATA                                  = 0x80618001;

    public final static int ERROR_AVC_VIDEO_FATAL                               = 0x80628002;

    public final static int ERROR_ATRAC_NO_ID                                   = 0x80630003;
    public final static int ERROR_ATRAC_INVALID_CODEC                           = 0x80630004;
    public final static int ERROR_ATRAC_BAD_ID                                  = 0x80630005;
    public final static int ERROR_ATRAC_ALL_DATA_LOADED                         = 0x80630009;
    public final static int ERROR_ATRAC_NO_DATA                                 = 0x80630010;
    public final static int ERROR_ATRAC_SECOND_BUFFER_NEEDED                    = 0x80630012;
    public final static int ERROR_ATRAC_INCORRECT_READ_SIZE                     = 0x80630013;
    public final static int ERROR_ATRAC_SECOND_BUFFER_NOT_NEEDED                = 0x80630022;
    public final static int ERROR_ATRAC_BUFFER_IS_EMPTY                         = 0x80630023;
    public final static int ERROR_ATRAC_ALL_DATA_DECODED                        = 0x80630024;

    public final static int ERROR_AAC_INVALID_ID                                = 0x80691001;
    public final static int ERROR_AAC_INVALID_ADDRESS                           = 0x80691002;
    public final static int ERROR_AAC_INVALID_PARAMETER                         = 0x80691003;
    public final static int ERROR_AAC_ID_NOT_INITIALIZED                        = 0x80691103;
    public final static int ERROR_AAC_NO_MORE_FREE_ID                           = 0x80691201;
    public final static int ERROR_AAC_NOT_ENOUGH_MEMORY                         = 0x80691501;
    public final static int ERROR_AAC_RESOURCE_NOT_INITIALIZED                  = 0x80691503;

    public final static int ERROR_CODEC_AUDIO_FATAL                             = 0x807f00fc;
    
    public final static int FATAL_UMD_UNKNOWN_MEDIUM                            = 0xC0210004;
    public final static int FATAL_UMD_HARDWARE_FAILURE                          = 0xC0210005;
}