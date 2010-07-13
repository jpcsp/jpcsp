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
     *      -> 0x000 - Main.
     *      -> 0x001 - File.
     *      -> 0x002 - Kernel.
     *      -> 0x011 - Utility.
     *      -> 0x026 - Audio.
     *
     * Bits 15 to 0 (E): Represent the error code itself (different for each area).
     *      -> E.g.: 0x80110001 - Error -> Utility -> Some unknown error.
     */

    public static final int ERROR_LIST_BUSY                                     = 0x80000021;
    public static final int ERROR_LIST_OUT_OF_MEMORY                            = 0x80000022;

	public static final int ERROR_INVALID_LIST_ID                               = 0x80000100;
    public static final int ERROR_INDEX                                         = 0x80000102;
    public static final int ERROR_POINTER                                       = 0x80000103;
    public static final int ERROR_SIZE                                  		= 0x80000104;
    public static final int ERROR_ARGUMENT                                      = 0x80000107;
    public static final int ERROR_FORMAT                                		= 0x80000108;

    public final static int ERROR_FILE_OPERATION_NOT_PERMITTED                  = 0x80010001;
    public final static int ERROR_FILE_NOT_FOUND                                = 0x80010002;
    public final static int ERROR_FILE_OPEN_ERROR                               = 0x80010003;
    public final static int ERROR_IO_ERROR                                      = 0x80010005;
    public final static int ERROR_ARG_LIST_TOO_LONG                             = 0x80010007;
    public final static int ERROR_INVALID_FILE_DESCRIPTOR                       = 0x80010009;
    public final static int ERROR_RESOURCE_UNAVAILABLE                          = 0x8001000B;
    public final static int ERROR_FILE_NO_MEMORY                                = 0x8001000C;
    public final static int ERROR_FILE_NO_PERM                                  = 0x8001000D;
    public final static int ERROR_FILE_INVALID_ADDR                             = 0x8001000E;
    public final static int ERROR_DEVICE_BUSY                                   = 0x80010010;
    public final static int ERROR_FILE_ALREADY_EXISTS                           = 0x80010011;
    public final static int ERROR_CROSS_DEV_LINK                                = 0x80010012;
    public final static int ERROR_DEVICE_NOT_FOUND                              = 0x80010013;
    public final static int ERROR_NOT_A_DIRECTORY                               = 0x80010014;
    public final static int ERROR_IS_DIRECTORY                                  = 0x80010015;
    public final static int ERROR_INVALID_ARGUMENT                              = 0x80010016;
    public final static int ERROR_TOO_MANY_OPEN_SYSTEM_FILES                    = 0x80010018;
    public final static int ERROR_FILE_IS_TOO_BIG                               = 0x8001001B;
    public final static int ERROR_DEVICE_NO_FREE_SPACE                          = 0x8001001C;
    public final static int ERROR_READ_ONLY                                     = 0x8001001E;
    public final static int ERROR_FILE_PATH_TOO_LONG                            = 0x80010024;
    public final static int ERROR_FILE_PROTOCOL                                 = 0x80010047;
    public final static int ERROR_DIRECTORY_IS_NOT_EMPTY                        = 0x8001005A;
    public final static int ERROR_TOO_MANY_SYMBOLIC_LINKS                       = 0x8001005C;
    public final static int ERROR_FILE_ADDR_IN_USE                              = 0x80010062;
    public final static int ERROR_CONNECTION_ABORTED                            = 0x80010067;
    public final static int ERROR_CONNECTION_RESET                              = 0x80010068;
    public final static int ERROR_NO_FREE_BUF_SPACE                             = 0x80010069;
    public final static int ERROR_FILE_TIMEOUT                                  = 0x8001006E;
    public final static int ERROR_NO_MEDIA                                      = 0x8001007B;
    public final static int ERROR_INVALID_MEDIUM                                = 0x8001007C;
    public final static int ERROR_FILE_QUOTA_EXCEEDED                           = 0x80010084;

    public final static int ERROR_FILE_READ_ERROR                               = 0x80020130;
    public final static int ERROR_TOO_MANY_OPEN_FILES                           = 0x80020320;
    public final static int ERROR_NO_SUCH_DEVICE                                = 0x80020321;
    public final static int ERROR_BAD_FILE_DESCRIPTOR                           = 0x80020323;
    public final static int ERROR_UNSUPPORTED_OPERATION                         = 0x80020325;
    public final static int ERROR_NOCWD                                         = 0x8002032c;
    public final static int ERROR_FILENAME_TOO_LONG                             = 0x8002032d;
    public final static int ERROR_ASYNC_BUSY                                    = 0x80020329;
    public final static int ERROR_NO_ASYNC_OP                                   = 0x8002032a;

    public static final int ERROR_SAVEDATA_LOAD_NO_MEMSTICK                     = 0x80110301;
    public static final int ERROR_SAVEDATA_LOAD_MEMSTICK_REMOVED                = 0x80110302;
    public static final int ERROR_SAVEDATA_LOAD_ACCESS_ERROR                    = 0x80110305;
    public static final int ERROR_SAVEDATA_LOAD_DATA_BROKEN                     = 0x80110306;
    public static final int ERROR_SAVEDATA_LOAD_NO_DATA                         = 0x80110307;
    public static final int ERROR_SAVEDATA_LOAD_BAD_PARAMS                      = 0x80110308;
    public static final int ERROR_SAVEDATA_LOAD_NO_UMD                          = 0x80110309;

    public static final int ERROR_SAVEDATA_RW_ACCESS_ERROR                      = 0x80110325;
    public static final int ERROR_SAVEDATA_RW_DATA_BROKEN                       = 0x80110326;
    public static final int ERROR_SAVEDATA_RW_NO_DATA                           = 0x80110327;

    public static final int ERROR_SAVEDATA_SAVE_NO_MEMSTICK                     = 0x80110381;
    public static final int ERROR_SAVEDATA_SAVE_MEMSTICK_REMOVED                = 0x80110382;
    public static final int ERROR_SAVEDATA_SAVE_NO_SPACE                        = 0x80110383;
    public static final int ERROR_SAVEDATA_SAVE_MEMSTICK_PROTECTED              = 0x80110384;
    public static final int ERROR_SAVEDATA_SAVE_ACCESS_ERROR                    = 0x80110385;
    public static final int ERROR_SAVEDATA_SAVE_BAD_PARAMS                      = 0x80110388;
    public static final int ERROR_SAVEDATA_SAVE_NO_UMD                          = 0x80110389;
    public static final int ERROR_SAVEDATA_SAVE_WRONG_UMD                       = 0x8011038a;

    public static final int ERROR_SAVEDATA_SIZES_NO_MEMSTICK                    = 0x801103c1;
    public static final int ERROR_SAVEDATA_SIZES_MEMSTICK_REMOVED               = 0x801103c2;
    public static final int ERROR_SAVEDATA_SIZES_ACCESS_ERROR                   = 0x801103c5;
    public static final int ERROR_SAVEDATA_SIZES_DATA_BROKEN                    = 0x801103c6;
    public static final int ERROR_SAVEDATA_SIZES_NO_DATA                        = 0x801103c7;
    public static final int ERROR_SAVEDATA_SIZES_BAD_PARAMS                     = 0x801103c8;

    public final static int ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT               = 0x80020064;
    public final static int ERROR_UNKNOWN_UID                                   = 0x800200cb;
    public final static int ERROR_UNMATCH_TYPE_UID                              = 0x800200cc;
    public final static int ERROR_NOT_EXIST_ID                                  = 0x800200cd;
    public final static int ERROR_NOT_FOUND_FUNCTION_UID                        = 0x800200ce;
    public final static int ERROR_ALREADY_HOLDER_UID                            = 0x800200cf;
    public final static int ERROR_NOT_HOLDER_UID                                = 0x800200d0;
    public final static int ERROR_ILLEGAL_PERMISSION                            = 0x800200d1;
    public final static int ERROR_ILLEGAL_ARGUMENT                              = 0x800200d2;
    public final static int ERROR_ILLEGAL_ADDR                                  = 0x800200d3;
    public final static int ERROR_MEMORY_AREA_OUT_OF_RANGE                      = 0x800200d4;
    public final static int ERROR_MEMORY_AREA_IS_OVERLAP                        = 0x800200d5;
    public final static int ERROR_ILLEGAL_PARTITION_ID                          = 0x800200d6;
    public final static int ERROR_PARTITION_IN_USE                              = 0x800200d7;
    public final static int ERROR_ILLEGAL_MEMBLOCK_ALLOC_TYPE                   = 0x800200d8;
    public final static int ERROR_FAILED_ALLOC_MEMBLOCK                         = 0x800200d9;
    public final static int ERROR_INHIBITED_RESIZE_MEMBLOCK                     = 0x800200da;
    public final static int ERROR_FAILED_RESIZE_MEMBLOCK                        = 0x800200db;
    public final static int ERROR_FAILED_ALLOC_HEAPBLOCK                        = 0x800200dc;
    public final static int ERROR_FAILED_ALLOC_HEAP                             = 0x800200dd;
    public final static int ERROR_ILLEGAL_CHUNK_ID                              = 0x800200de;
    public final static int ERROR_CANNOT_FIND_CHUNK_NAME                        = 0x800200df;
    public final static int ERROR_NO_FREE_CHUNK                                 = 0x800200e0;

    public final static int ERROR_MODULE_LINK_ERROR                             = 0x8002012c;
    public final static int ERROR_ILLEGAL_OBJECT_FORMAT                         = 0x8002012d;
    public final static int ERROR_UNKNOWN_MODULE                                = 0x8002012e;
    public final static int ERROR_UNKNOWN_MODULE_FILE                           = 0x8002012f;
    public final static int ERROR_MODULE_FILE_READ_ERROR                        = 0x80020130;
    public final static int ERROR_MEMORY_IN_USE                                 = 0x80020131;
    public final static int ERROR_PARTITION_MISMATCH                            = 0x80020132;
    public final static int ERROR_MODULE_ALREADY_STARTED                        = 0x80020133;
    public final static int ERROR_MODULE_NOT_STARTED                            = 0x80020134;
    public final static int ERROR_MODULE_ALREADY_STOPPED                        = 0x80020135;
    public final static int ERROR_MODULE_CANNOT_STOP                            = 0x80020136;
    public final static int ERROR_MODULE_NOT_STOPPED                            = 0x80020137;
    public final static int ERROR_MODULE_CANNOT_REMOVE                          = 0x80020138;
    public final static int ERROR_EXCLUSIVE_LOAD                                = 0x80020139;
    public final static int ERROR_LIBRARY_IS_NOT_LINKED                         = 0x8002013a;
    public final static int ERROR_LIBRARY_ALREADY_EXISTS                        = 0x8002013b;
    public final static int ERROR_LIBRARY_NOT_FOUND                             = 0x8002013c;
    public final static int ERROR_ILLEGAL_LIBRARY_HEADER                        = 0x8002013d;
    public final static int ERROR_LIBRARY_IN_USE                                = 0x8002013e;
    public final static int ERROR_MODULE_ALREADY_STOPPING                       = 0x8002013f;
    public final static int ERROR_ILLEGAL_OFFSET_VALUE                          = 0x80020140;
    public final static int ERROR_ILLEGAL_POSITION_CODE                         = 0x80020141;
    public final static int ERROR_ILLEGAL_ACCESS_CODE                           = 0x80020142;
    public final static int ERROR_MODULE_MANAGER_BUSY                           = 0x80020143;
    public final static int ERROR_ILLEGAL_FLAG                                  = 0x80020144;
    public final static int ERROR_CANNOT_GET_MODULE_LIST                        = 0x80020145;
    public final static int ERROR_PROHIBIT_LOADMODULE_DEVICE                    = 0x80020146;
    public final static int ERROR_PROHIBIT_LOADEXEC_DEVICE                      = 0x80020147;
    public final static int ERROR_UNSUPPORTED_PRX_TYPE                          = 0x80020148;
    public final static int ERROR_ILLEGAL_PERMISSION_CALL                       = 0x80020149;
    public final static int ERROR_CANNOT_GET_MODULE_INFO                        = 0x8002014a;
    public final static int ERROR_ILLEGAL_LOADEXEC_BUFFER                       = 0x8002014b;
    public final static int ERROR_ILLEGAL_LOADEXEC_FILENAME                     = 0x8002014c;
    public final static int ERROR_NO_EXIT_CALLBACK                              = 0x8002014d;

    public final static int ERROR_NO_MEMORY                                     = 0x80020190;
    public final static int ERROR_ILLEGAL_ATTR                                  = 0x80020191;
    public final static int ERROR_ILLEGAL_THREAD_ENTRY_ADDR                     = 0x80020192;
    public final static int ERROR_ILLEGAL_PRIORITY                              = 0x80020193;
    public final static int ERROR_ILLEGAL_STACK_SIZE                            = 0x80020194;
    public final static int ERROR_ILLEGAL_MODE                                  = 0x80020195;
    public final static int ERROR_ILLEGAL_MASK                                  = 0x80020196;
    public final static int ERROR_ILLEGAL_THREAD                                = 0x80020197;
    public final static int ERROR_NOT_FOUND_THREAD                              = 0x80020198;
    public final static int ERROR_NOT_FOUND_SEMAPHORE                           = 0x80020199;
    public final static int ERROR_NOT_FOUND_EVENT_FLAG                          = 0x8002019a;
    public final static int ERROR_NOT_FOUND_MESSAGE_BOX                         = 0x8002019b;
    public final static int ERROR_NOT_FOUND_VPOOL                               = 0x8002019c;
    public final static int ERROR_NOT_FOUND_FPOOL                               = 0x8002019d;
    public final static int ERROR_NOT_FOUND_MESSAGE_PIPE                        = 0x8002019e;
    public final static int ERROR_NOT_FOUND_ALARM                               = 0x8002019f;
    public final static int ERROR_NOT_FOUND_THREAD_EVENT_HANDLER                = 0x800201a0;
    public final static int ERROR_NOT_FOUND_CALLBACK                            = 0x800201a1;
    public final static int ERROR_THREAD_ALREADY_DORMANT                        = 0x800201a2;
    public final static int ERROR_THREAD_ALREADY_SUSPEND                        = 0x800201a3;
    public final static int ERROR_THREAD_IS_NOT_DORMANT                         = 0x800201a4;
    public final static int ERROR_THREAD_IS_NOT_SUSPEND                         = 0x800201a5;
    public final static int ERROR_THREAD_IS_NOT_WAIT                            = 0x800201a6;
    public final static int ERROR_NOW_DISPATCH_DISABLED                         = 0x800201a7;
    public final static int ERROR_WAIT_TIMEOUT                                  = 0x800201a8;
    public final static int ERROR_WAIT_CANCELLED                                = 0x800201a9;
    public final static int ERROR_WAIT_STATUS_RELEASED                          = 0x800201aa;
    public final static int ERROR_WAIT_STATUS_RELEASED_CALLBACK                 = 0x800201ab;
    public final static int ERROR_THREAD_IS_TERMINATED                          = 0x800201ac;
    public final static int ERROR_SEMA_ZERO                                     = 0x800201ad;
    public final static int ERROR_SEMA_OVERFLOW                                 = 0x800201ae;
    public final static int ERROR_EVENT_FLAG_POLL_FAILED                        = 0x800201af;
    public final static int ERROR_EVENT_FLAG_NO_MULTI_PERM                      = 0x800201b0;
    public final static int ERROR_EVENT_FLAG_ILLEGAL_WAIT_PATTERN               = 0x800201b1;
    public final static int ERROR_MESSAGEBOX_NO_MESSAGE                         = 0x800201b2;
    public final static int ERROR_MESSAGE_PIPE_FULL                             = 0x800201b3;
    public final static int ERROR_MESSAGE_PIPE_EMPTY                            = 0x800201b4;
    public final static int ERROR_WAIT_DELETE                                   = 0x800201b5;
    public final static int ERROR_ILLEGAL_MEMBLOCK                              = 0x800201b6;
    public final static int ERROR_ILLEGAL_MEMSIZE                               = 0x800201b7;
    public final static int ERROR_ILLEGAL_SCRATCHPAD_ADDR                       = 0x800201b8;
    public final static int ERROR_SCRATCHPAD_IN_USE                             = 0x800201b9;
    public final static int ERROR_SCRATCHPAD_NOT_IN_USE                         = 0x800201ba;
    public final static int ERROR_ILLEGAL_TYPE                                  = 0x800201bb;
    public final static int ERROR_ILLEGAL_SIZE                                  = 0x800201bc;
    public final static int ERROR_ILLEGAL_COUNT                                 = 0x800201bd;
    public final static int ERROR_NOT_FOUND_VTIMER                              = 0x800201be;
    public final static int ERROR_ILLEGAL_VTIMER                                = 0x800201bf;
    public final static int ERROR_ILLEGAL_KTLS                                  = 0x800201c0;
    public final static int ERROR_KTLS_IS_FULL                                  = 0x800201c1;
    public final static int ERROR_KTLS_IS_BUSY                                  = 0x800201c2;
    public final static int ERROR_NOT_FOUND_MUTEX                               = 0x800201c3;
    public final static int ERROR_MUTEX_LOCKED                                  = 0x800201c4;

    public final static int ERROR_MUTEX_OVERFLOW                                = 0x800201c8;

    public final static int ERROR_CANNOT_CANCEL                                 = 0x80020261;

    public final static int ERROR_DEVCTL_BAD_PARAMS                             = 0x80220081;

    public final static int ERROR_AUDIO_CHANNEL_NOT_INIT                        = 0x80260001;
    public final static int ERROR_AUDIO_CHANNEL_BUSY                            = 0x80260002;
    public final static int ERROR_AUDIO_INVALID_CHANNEL                         = 0x80260003;
    public final static int ERROR_AUDIO_PRIV_REQUIRED                           = 0x80260004;
    public final static int ERROR_AUDIO_NO_CHANNELS_AVAILABLE                   = 0x80260005;
    public final static int ERROR_AUDIO_OUTPUT_SAMPLE_DATA_SIZE_NOT_ALIGNED     = 0x80260006;
    public final static int ERROR_AUDIO_INVALID_FORMAT                          = 0x80260007;
    public final static int ERROR_AUDIO_CHANNEL_NOT_RESERVED                    = 0x80260008;
    public final static int ERROR_AUDIO_NOT_OUTPUT                              = 0x80260009;

    public final static int ERROR_PSMF_NOT_INITIALIZED                          = 0x80615001;
    public final static int ERROR_PSMF_BAD_VERSION                              = 0x80615002;
    public final static int ERROR_PSMF_NOT_FOUND                                = 0x80615025;
    public final static int ERROR_PSMF_INVALID_PSMF                             = 0x80615501;
    public final static int ERROR_PSMF_INVALID_VALUE                            = 0x806151FE;
}