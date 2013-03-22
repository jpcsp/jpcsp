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
- one syscall to be traced is described in a single line:
	<syscall-name> <nid> <number-of-parameters>

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
