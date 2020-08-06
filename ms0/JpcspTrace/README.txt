====================================================================
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
====================================================================

This application is a plugin for a real PSP.
It allows the logging of syscall's performed by an application when running on a PSP.

Installation
=============
The plugin can only be installed on a real PSP running a custom firmware.
The plugin has been tested with procfw 6.60 PRO-C2.

Copy the file JpcspTrace.prx to your PSP
	ms0:/seplugins/JpcspTrace.prx

Copy the file JpcspTraceUser.prx to your PSP
	ms0:/seplugins/JpcspTraceUser.prx

Copy the file JpcspTrace.config to your PSP
	ms0:/seplugins/JpcspTrace.config

Add the following line to the file ms0:/seplugins/game.txt on your PSP
	ms0:/seplugins/JpcspTrace.prx 1

Edit the file ms0:/seplugins/JpcspTrace.config to your needs.


Usage
======
After installation, the plugin is activated when starting a PSP application.

The format of the file JpcspTrace.config is the following:
- the file is processed line by line
- leading spaces or tabs in a line are ignored
- an empty line or a line starting with a "#" is a comment
- the log buffer length can be set using a line starting with
    LogBufferLength 0xNNNN
- the log buffer is written to the log.txt file at each output line
  unless the following line is present:
    BufferLogWrites
  In this case, the log buffer is written to the log.txt file only when
  the buffer is full. This method has a better performance but some log data
  could be lost in case JpcspTrace is crashing.
- the log buffer can be forced to be written to the log.txt through the command:
    FlushLogBuffer
- a memory range can be dumped to a file using the following command:
    DumpMemory 0xNNNNNNNN 0xNNN ms0:/filename
  where the first parameter is the address start, the second parameter
  is the length in bytes to be dumped and the third parameter is
  the file name where the memory will be dumped.
- the complete NAND (flash0, flash1, flash2) can be dumped using the following command:
    DumpNand
  The following files will then be created:
  - ms0:/nand.fuseid: will contain the 8-bytes fuseId of the PSP
  - ms0:/nand.block: will contain the 32MB user data of the whole NAND
  - ms0:/nand.spare: will contain the 1MB raw spare data of the whole NAND
  - ms0:/nand.result: will contain the 2K 4-bytes result of each sceNandReadPages() call
- a KIRK command can be executed and its output saved to a file by using the command:
    ExecuteKirkCommand 0xN ms0:/output 0xNN ms0:/input 0xNN
  where the first parameter is the command code, the second parameter is the file name
  where the output will be written to, the third parameter is the size in bytes of the
  output, the fourth parameter is the file name for reading the input and the
  fifth parameter is the size in bytes of the input.
  If the input file name starts with "0x", it will be interpreted as a memory address
  and the values stored at that address will be used as input to the KIRK command
  instead of reading the input file.
  If the output file name starts with "0x", it will be interpreted as a memory address
  and the result of the KIRK command will be stored at that address
  instead of being written to the output file.
  If the output file name starts with ">", the output will be appended to the file name.
  If the output file name ends with ".xml", the output will be written into the same
  format as Jpcsp file PreDecrypt.xml, so that the result can easily be added to it.
- a SYSCON command can be executed and its output saved to a file by using the command:
    ExecuteSysconCommand 0xN ms0:/output 0xNN ms0:/input 0xNN
  where the first parameter is the command code, the second parameter is the file name
  where the output will be written to, the third parameter is the size in bytes of the
  output, the fourth parameter is the file name for reading the input and the
  fifth parameter is the size in bytes of the input.
  If the input file name starts with "0x", it will be interpreted as a memory address
  and the values stored at that address will be used as input to the SYSCON command
  instead of reading the input file.
  If the output file name starts with "0x", it will be interpreted as a memory address
  and the result of the SYSCON command will be stored at that address
  instead of being written to the output file.
- a page of the ID Storage can be read by using the command:
    IdStorageRead 0xNNN ms0:/output
  where the first parameter is the key number and the second parameter is the file
  name where the output will be written to.
- the file flash0:/kd/resource/meimg.img need to be decrypted on a real PSP
  before it can be used in Jpcsp for the "--reboot" function.
  The following command is decrypting this file:
    DecryptMeimg flash0:/kd/resource/meimg.img ms0:/meimg.img
  The result file ms0:/meimg.img need then to be copied to Jpcsp
  flash0/kd/resource/meimg.img
- a 32-bit value can be read/written to memory using the commands
    write32 0xNNNNNNNN 0xNNN
    read32 0xNNNNNNNN
  where the first parameter is the address and the second parameter
  for write32 is the value to be written.
  Similar commands are also available for 16 and 8-bit values:
    write16 0xNNNNNNNN 0xNNNN
    read16 0xNNNNNNNN
    write8 0xNNNNNNNN 0xNN
    read8 0xNNNNNNNN
  The commands can, for example, be used to test simple MMIO cases.
- a memory range can be copied to another memory address:
    memcpy 0xNNNNNNNN 0xNNNNNNNN 0xNNN
  where the first parameter is the destination address,
  the second parameter is the source address and
  the third parameter is the length in bytes to be copied.
- a memory range can be reset to a fixed value:
    memset 0xNNNNNNNN 0xNN 0xNNN
  where the first parameter is the start address,
  the second parameter is the 8-bit value to be set and
  the third parameter is the length in bytes to be set.
- a memory range can be xor-ed with another memory range:
    xor 0xNNNNNNNN 0xNNNNNNNN 0xNNNNNNNN 0xNNN
  where the first parameter is the destination address,
  the second parameter is the first source address,
  the third parameter is the second source address and
  the fourth parameter is the length in bytes to be xor-ed.
- the interrupts can be disabled/enabled:
    DisableInterrupts
	EnableInterrupts
- the logging of the processed commands can be disabled:
    LogCommands 0
  will stop logging the processed commands and
    LogCommands 1
  will log again the processed commands.
- a text can be logged, even if the logging of commands is disabled:
    Echo this is any text
  will log "this is any text"
- a delay can be introduced in the processing of the commands:
    Delay 0xNNN
  will perform a delay of 0xNNN microseconds before continuing
  execution.
- the processing can wait for a fixed 32-bit value to be present
  at some memory address:
    WaitFor32 0xNNNNNNNN 0xNNN
  where the first parameter is the address and
  the second parameter is the 32-bit value that need to be found
  at the given address before continuing execution.
- the content of MIPS registered can be watched in almost any
  kernel module:
	Watch <module-name> <name> 0xNNNN <register-list>

  The code at the offsets 0xNNNN and 0xNNNN+4 of the module <module-name>
  will be patched so that the content of any registers can be logged
  each time this code is executed.
  As the watched code can be into the code executed during an interrupt,
  the watched values are first logged into a "Watch" buffer and
  only be written to the log file at the next logging of a syscall.
  The <name> can be any character sequence without spaces which will only
  be used during logging to help identifying which Watch point is being logged.

  The list of registers to be logged is defined in <register-list> which can
  be a white-spare or comma-separated list of registers.
  Examples:
	- $a0 $s0 $t0
	  will log the values of the 3 registers $a0, $s0 and $t0 at the time
	  after the instruction at offset 0xNNNN and before the instruction at offset 0xNNNN+4
	- $a0, $s0, $t0
	  the registers can be comma-separated
	- a0 s0 t0
	  the '$' sign before the register name is optional
	- < $a0 $s0 $t0
	  will log the values of the 3 registers $a0, $s0 and $t0 at the time
	  before the instruction at offset 0xNNNN
	- > $a0 $s0 $t0
	  will log the values of the 3 registers $a0, $s0 and $t0 at the time
	  after the instruction at offset 0xNNNN+4
	- *$a0 0xN
	  will log the content of the memory at the address stored in the register $a0.
	  0xN bytes will be logged.
	- $a0 *$a0 0xN
	  will log the value of the register $a0 and the content of the memory
	  at the address stored in the register $a0. 0xN bytes will be logged.

  Complete examples:
	- Watch sceUSB_Driver Interrupt_0x1A 0x00006D10 > $s2
	  will log something like:
		00:20.886 sceUSB_Driver.Interrupt_0x1A: $s2=0x00000008
	- Watch sceUSB_Driver Interrupt_0x1A_0x000066C0 0x000066C0 > $t1 $t3 *$t3 0xC
	  will log something like (the log line is followed by the 12 bytes at the memory pointed by $t3):
		00:21.009 sceUSB_Driver.Interrupt_0x1A_0x000066C0: $t1=0xBD800014, $t3=0xA818A4F0
		01 00 00 08 00 00 00 00 00 F2 07 08

  Restriction: multiple register values can be logged but only maximum one memory content
               (i.e. "*$reg") can be logged into one Watch command.
  Restriction: the code at the offsets 0xNNNN and 0xNNNN+4 may not
               contain any MIPS instructions having a delay slot (e.g. branching instructions).
- the size (in bytes) of the internal Watch buffer can be defined with the command
	WatchBufferLength 0xNNNN
- one syscall to be traced is described in a single line:
	<syscall-name> <nid> <number-of-parameters> <parameter-types>

  The <syscall-name> is just used for easier reading in the log file,
  it has no other function.
  The <nid> is the syscall NID in hexadecimal with an optional "0x" prefix,
  for example 0x1234ABCD or 1234AbCd. The NID is uniquely identifying the
  syscall to be traced. The pluging automatically scans all the PSP modules
  and libraries to find the given NID address. The first occurence found
  is then used.
  The <number-of-parameters> is the number of parameters that have to be
  logged for the syscall. This number is optional and will default to 8.
  Valid values for <number-of-parameters> are between 0 and 8.
  The <parameter-types> gives the type of each syscall parameters, with
  one letter per parameter. The <parameter-types> is optional and will
  then default to "xxxxxxxx", i.e. will log all the parameters in hexadecimal format.
  The following parameter types are available:
  - x: log the parameter value as an hexadecimal number without leading zero's, e.g. 0x12345
  - d: log the parameter value as a decimal number without leading zero's, e.g. 12345
  - s: log the parameter value as a pointer to a zero-terminated string, e.g.
       0x08812345('This is a string')
  - p: log the parameter value as a pointer to a 32-bit value, e.g.
       0x08812340(0x00012345)    (when the 32-bit value at address 0x08812340 is 0x00012345)
  - P: log the parameter value as a pointer to a 64-bit value, e.g.
       0x08812340(0x00012345 0x6789ABCD)    (when the 64-bit value at address 0x08812340 is 0x6789ABCD00012345)
  - v: log the parameter value as a pointer to a variable-length structure.
       The first 32-bit value is the total length of the structure. E.g.:
       0x08812340(0x00000008 0x00012345)
  - F: log the parameter value as a FontInfo structure (see sceFontGetFontInfo)
  - f: log the parameter value as a pspCharInfo structure (see sceFontGetCharInfo)
  - e: log the parameter value as a Mpeg EP structure (16 bytes long)
  - a: log the parameter value as a SceMpegAu structure (24 bytes long, see sceMpeg)
  - t: log the parameter value as a SceMp4TrackSampleBuf structure (240 bytes long, see sceMp4)
  - I: log the parameter value as a pspNetSockAddrInternet structure (8 bytes long, see sceNetInet)
  - B: log the parameter value as a pointer to a memory buffer having its length
       stored into the next parameter value
  - V: log the parameter value as a video codec structure as used in sceVideocodec
  - !: this flag is not a parameter type but indicates that the syscall parameters have
       to be logged before and after the syscall (i.e. twice). By default, the parameters
       are only logged after the syscall.
  - $: this flag is not a parameter type but indicates that the total free memory
       and maximum free memory have to be logged with the syscall. In combination with
       the '!' flag, the free memory before and after the syscall can be logged.
  - >: this flag is not a parameter type but indicates that the stack usage
       of the function has to logged.
  - %: flush the log file after executing the syscall
  All the parameter types are concatenated into one string, starting with
  the type of the first parameter ($a0). Unspecified parameter types default to "x".

The syscall's listed in JpcspTrace.config will be logged into the file
	ms0:/log.txt

The logging information is always appended at the end of the file.

The format of the file log.txt is as follows:
- a new session starts with the line
	JpcspTrace - module_start
- a few lines are then listing the config file as it is being processed
- after initialization, one line is output for each call of a syscall
  configured in JpcspTrace.config
- the line format is
	HH:MM:SS <thread-name> - <syscall-name> <parameter-a0>, <parameter-a1>, ... = <result-v0>

When logging syscall's that are often called by an application might
slowdown dramatically the application. Be careful to only log the needed syscall's.


Deactivation
=============
Edit the following line in the file ms0:/seplugins/game.txt on your PSP
	ms0:/seplugins/JpcspTrace.prx 0
(i.e. replace "1" by "0")


Deinstallation
===============
Remove the line from the file ms0:/seplugins/game.txt on your PSP.
Delete the following files on your PSP:
	ms0:/seplugins/JpcspTrace.prx
	ms0:/seplugins/JpcspTrace.config
	ms0:/log.txt
