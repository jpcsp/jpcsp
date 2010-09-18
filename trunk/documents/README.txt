           &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
     &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
   &&&&&&&&&*********&&&*********&&&********&&&&*******&&&*********&&&&&&&&&
 &&&&&&&&&&&&&&&&****&&&***&&&&**&&&***&&&&&&&&***&&&&&&&&***&&&&**&&&&&&&&&&&
&&&&&&&&&&&&&&&&&****&&&***&&&&**&&&***&&&&&&&&***&&&&&&&&***&&&&**&&&&&&&&&&&&
&&&&&&&&&&&&&&&&&****&&&*********&&&***&&&&&&&&&*****&&&&&*********&&&&&&&&&&&&
&&&&&&&&&&&&&&&&&****&&&***&&&&&&&&&***&&&&&&&&&&&&***&&&&***&&&&&&&&&&&&&&&&&&
 &&&&&&&&&&&&&&&&****&&&***&&&&&&&&&***&&&&&&&&&&&&***&&&&***&&&&&&&&&&&&&&&&
  &&&&&&&&&&&*******&&&&***&&&&&&&&&********&&&******&&&&&***&&&&&&&&&&&&&&&
    &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
           &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&

*******************************************************************************
[VERSION HISTORY]:
v0.6:
September 18, 2010 - Added new emulation changes and improved the FAQ.
v0.5:
March 25, 2010 - Merged all JPCSP's (v0.5) details into this README file.
*******************************************************************************

*******************************************************************************
[TABLE OF CONTENTS]:
- Introduction;
- What's new (changelog);
- JPCSP FAQ;
- The Team;
- Copyright;
- Contacts.
*******************************************************************************



			        [INTRODUCTION]
...............................................................................
...............................................................................
JPCSP is a Playstation Portable emulator written in Java for the PC.
The reasons for this choice rely on the portability and flexibility of this
programming language, which makes it much easier to test and apply changes 
whenever needed.
This emulator is an open source project. If you want to contribute, check the
links at the bottom of this FAQ.

In this FAQ we intend to explain the multiple functionalities and
architecture of this emulator, as well as list all the needed information to
clarify any possible doubts about it's usage.

Please keep in mind that JPCSP does not support or endorse piracy.
...............................................................................




			   [What's new (changelog)]
...............................................................................
...............................................................................
JPCSP v0.6 (September 18, 2010):

-> Included compilation of several "Allegrex" instructions in dynarec for a
   much better performance;

-> Performed major code cleanups and reorganization;

-> Fixed and improved module loading:
	- Corrected import and export's mapping;
	- Implemented newly discovered loading methods.

-> Updated all modules with the most recent findings;

-> Added all new save/load (savedata) modes;

-> Reviewed and improved all kernel object managers:
	- Implemented each kernel object manager's attributes;
	- Added full LwMutex support in a dedicated kernel object manager
	- Provided corrections for mutex, semaphore and event flag managers;
	- Properly implemented VPL and FPL handling.

-> Improved graphics' handling:
	- Splitted rendering into a new RE (rendering engine);
	- Cleaned up and optimized VideoEngine;
	- Fully implemented, corrected and organized main GE commands;
	- Improved shaders' usage and stability;
	- Introduced a geometry shader for increased speed in rendering;
	- Fixed display list processing;
	- Improved the performance of several GE commands.
	- Implemented the VideoEngine processing in a parallel thread,
	  to take advantage of dual-core processors and
	  match the PSP dual-core architecture.

-> Implemented a MediaEngine for video and audio playback (based on FFMPEG):
	- Added video playback support in sceMpeg and scePsmfPlayer;
	- Added ATRAC3 audio playback support in sceAtrac3plus;
	- Improved "UMD Browser" to display images, load videos and play sounds
	  from the UMD data.

-> Improved main GUI and debug tools:
	- Added a "Cheats" menu with CWCheat support;
	- Provided a cleaner organization and display of settings;
	- Removed "Emulation" menu;
	- Improved the "Logger" tool;
	- Added a new "Image Viewer" tool.




JPCSP v0.5 (March 09, 2010):

-> Lots of code cleanups;

-> Graphical improvements:
	- Shader improvements;
	- VideoEngine optimizations;
	- Textures handling fixes;

-> Implementation of interrupts' management:
	- Implemented Alarm, VTimer and VBlank interrupts;

-> Saving/Loading improvements:
	- Implemented LIST_LOAD and LIST_SAVE modes;
	- Improved mode 8 (MODE_TRY);
	
-> Improvements for faked MPEG functionalities:
	- Implemented partial YCbCr mode support;
	- Implemented partial PSMFPlayer faking;
	
-> Inclusion of multi-language packs:
	- Added English, French, German, Spanish, Catalan and Lithuanian packs;

-> Beggining of threaded IOAsync operations implementation;

-> General fixes for module loading;

-> Small improvements of HLE functions.
...............................................................................




			        [JPCSP FAQ]
...............................................................................
...............................................................................

1. Getting started:

Be sure to have JRE (Java Runtime Environement) installed in your computer
before attempting to run JPCSP.

NOTE: It is strongly advised that even on a 64-bit OS, you should install the
32-bit JRE release and use JPCSP's 32-bit version, for compatiblity reasons.

If you've downloaded the Windows version, use the batch (.bat) files located
inside JPCSP's main folder (start-windows-x86.bat or start-windows-amd64.bat).

If you've downloaded the Linux version, use the shell script (.sh) files 
located inside JPCSP's main folder (start-linux-x86.sh or 
start-linux-amd64.sh).

If you've downloaded the MacOSX version, just double click the application
bundle to start JPCSP.




2. Loading/Running applications:

To load an ISO/CSO image, you need to place it under the "umdimages" folder
(this folder can be changed under Options > Configuration > General).
For homebrew, place the application's main folder (which should contain the 
EBOOT file) under ms0 > PSP > GAME.

To know which games are currently compatible with the emulator, you can check
JPCSP's forum for the latest news and an updated compatibility list post.

The games tagged as "Encrypted" cannot be loaded by regular means. The only
legal option is to own a PSP and use it to decrypt your own games' boot file.




3. Usage:

The "File" menu allows you to load UMD images (Load UMD), homebrew applications
(Load MemStick), and any other file such as demos (Load File). It also allows 
to capture and load the current RAM memory and GPR registers' state to a file,
so it can be used as an additional save option (Save/Load Snapshot).

The "Options" menu contains dedicated settings for "Video" (Rotate and 
Screenshot), "Audio" (Mute) and "Controls" features, as well as the 
"Configuration" menu.

The "Debug" menu contains all the advanced features of the emulator such as the
logger, the debugger and the memory viewer (see section 5. Advanced features).

The "Cheats" menu allows you to apply cheats to the current application.

The "Language" menu allows you to change the language of the emulator.

The "Help" menu contains the "About" window.




3. Command-Line options:

Usage: java -Xmx512m -jar jpcsp.jar <OPTIONS>

  -d, --debugger             Open debugger at start.
  -f, --loadfile FILE        Load a file.
                             Example: ms0/PSP/GAME/pspsolitaire/EBOOT.PBP
  -u, --loadumd FILE         Load a UMD. Example: umdimages/cube.iso
  -r, --run                  Run loaded file or umd. Use with -f or -u option.




4. Requirements:

Minimum:
- OS: Windows 32bit or 64bit / Linux 32bit or 64bit / Mac OSX; 
- CPU: Pentium 4 and up;
- GPU: Any graphic card supporting OpenGL 2.0 and up; 
- Memory: 1GB RAM.

Recommended:
- OS: Windows Vista / Windows 7;
- CPU: Dual core @ 2.5 GHz;
- GPU: Updated graphic card supporting OpenGL;
- Memory: 2GB RAM or more. 




5. Advanced features:

- Font Override:
It is possible to override the font used in the log window by editing the 
settings.properties file.
You can specify a system font name, such as Arial or a TTF file name.
We suggest Mona for partial Japanese character support. 
Alternatively you can install extended language support in Windows from the 
Regional Settings in Windows Control Panel, you will need a Windows CD and 
around 300mb disk space.

Please note if a font does not work it is a limitation of Java, not JPCSP.


- Game Pads:
We don't support game pads in JPCSP yet, but you may use 3rd party software 
that maps your pad's buttons to the keyboard.


- Patch files:
To avoid repeatedly editing the settings each time you want to play a different
game you can save them in a patch file that will get automatically loaded with 
the game. Patch files go in the "patches" directory and are named after the 
game's Disc ID.

Please note when a game is using a patch file all compatibility settings in 
the user interface will be overridden regardless of their state.


- Media Engine:
NOTE: Currently, only supported in 32-bit Windows.
The "Media Engine" can be enabled under "Options" > "Configuration" > "Misc".
You also need to unpack a folder called ffmpeg-natives.7z located under lib >
windows-x86.
This allows JPCSP to use the FFMPEG's wrapper Xuggler to decode and playback
ingame videos (instead of faked MPEG data) and audio (ATRAC3 only, ATRAC3+ is
not supported yet).


- Debug Tools (under "Debug" > "Tools"):
* Logger:
The logger is a tool that can be enabled under "Debug" > "Tools" > "Logger" >
"Show Logger". This opens an additional window that keeps track of the internal
functioning of the current application in the log4j style.
With this tool you can check which syscalls are called by the application, 
which code blocks are being compiled by the compiler and so on. 

You can also customize it's usage by going to "Debug" > "Tools" > "Logger" > 
"Customize...".
There you can specify general settings like opening this window at startup,
preview the current log4j settings' file (LogSettings.xml), output the logging
information into one file (formatted HTML or plain TXT) or several splitted 
files and even change the logging method in use (GPU log only, for example).

Note: After these modifications are applied, you must press the "Generate new
settings file" button to update the external log settings' file. The changes in
"Settings" and "Advanced" tabs are not saved by the emulator so the user can
easily regenerate the original file.


* Debugger:
The debugger allows you to view the disassembly, place, export and import 
breakpoints, step through instructions and override the program counter.
The contents of the GPR and FPR are present in the side bar.
Raw memory and VFPU registers are available in separate windows.

There are some other miscellaneous features in the side bar:
-> GPI switches and GPO indicators;
-> GE Capture:
	The GE is the graphics unit. We allow you to capture and replay 1 
	display list. All textures used in the display list will be saved to 
	the tmp directory.

	For best results run replays in the same game the capture was taken in.


* Memory Viewer:
Displays the current's application raw RAM memory.


* Image Viewer:
Displays images directly loaded from the VRAM.


* VFPU Registers:
Contains internal information on VFPU oprations.


* Instruction Counter:
Lists all instructions and their frequency for the currently loaded program.


* File IO Logger:
Shows all IO activity. 
Useful for developers to make sure they are reading files once only and as 
quickly as possible. It can also tell you if you have forgotten to close a 
file handle.


* ELF Header Information:
Shows ELF section and program headers parsed from the application's main boot 
file.


- Profiler:
The profiler is a method used by JPCSP to analyse repeated code sequences in
order to allow further optimization. If you wish, you can turn this feature on
under "Options" > "Configuration" > "General". The data will be saved for a 
certain application after closing the emulator.
Use "Reset Profiler Information" under "Debug" to clear the saved data.


- ISO contents:
You can dump the current ISO/CSO image's contents into an illustrative .txt
file. In order to do this, go to "Debug">"Dump ISO to ISO-index.txt".

...............................................................................




			   	[The Team]
...............................................................................
...............................................................................
JPCSP Team (active):
- gid15
- hlide
- Hykem
- Orphis
- shadow

Past members and contributors (inactive):
- fiveofhearts
- gigaherz
- mad
- Nutzje
- aisesal
- shashClp
- wrayal
- dreampeppers99
- spip2
- mozvip
- Drakon
- raziel1000
- i30817
- theball
- J_BYYX

Beta-testers:
- BlackDaemon
- SilvX
- s1n
- Foxik
- Darth1701
...............................................................................




			   	[Copyright]
...............................................................................
...............................................................................
Jpcsp is free software: you can redistribute it and/or modify 
it under the terms of the GNU General Public License as published by 
the Free Software Foundation, either version 3 of the License, or 
(at your option) any later version. 
  
Jpcsp is distributed in the hope that it will be useful, 
but WITHOUT ANY WARRANTY; without even the implied warranty of 
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
GNU General Public License for more details. 
  
You should have received a copy of the GNU General Public License 
along with Jpcsp.  If not, see <http://www.gnu.org/licenses>.
...............................................................................




			   	[Contacts]
...............................................................................
...............................................................................
JPCSP's Google Code repository:
- http://code.google.com/p/jpcsp

JPCSP's Official Website:
- http://www.jpcsp.org

JPCSP's Official Forum (hosted at Emunewz.net):
- http://www.emunewz.net/forum/forumdisplay.php?fid=51
...............................................................................