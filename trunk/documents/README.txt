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
With this distribution you should find a batch (.bat) file. This is just a
shortcut for the command line operation that launches JPCSP's .jar file. You
can edit this file if you want to change the heap memory the emulator can use.
Eg.: java -Xmx512m -jar jpcsp.jar -> uses 512 MB of RAM.




2. Loading/Running applications:

To load an ISO/CSO image, you need to place it under the "umdimages" folder
(this folder can be changed under Settings > Compatibility).
For homebrew, place the application's main folder (which should contain the 
EBOOT file) under ms0 > PSP > GAME.

To know which games are currently compatible with the emulator, you can check
the Compatibility page of JPCSP´s homepage (link at the bottom of this FAQ), or
check this list (updated): http://jpcsp.org/forum/viewtopic.php?f=4&t=6368

The games tagged as "Encrypted" cannot be loaded by regular means. The only
legal option is to own a PSP and use it to decrypt your own games' boot file.




3. Usage:

The "File" menu allows you to load UMD images, homebrew from MemStick and any
other file such as demos.
The "Emulation" menu contains the run, pause and reset buttons for the
emulator.
The "Options" menu contains the "Screenshot" function, which allows you to take
a screenshot of the current screen. It is then saved as [DiscID]_Snap_XXX.png
in the current directory. The "Settings" allow you to change features such as
HLE specific options or keyboard mapping. The "Save/Load Snapshot" functions
capture and loads the current RAM memory and GPR registers' state to a file,
so it can be used as an additional save option. The "Rotate" option is able to
change the screen orientation accordingly to a chosen angle.
The "Language" menu allows you to change the language of the emulator.
The "Debug" menu contains all the advanced features of the emulator such as the
debugger and the memory viewer.
The "Help" menu contains the "About" window.

Below the main window you can find the logger. This window keeps track of the
instructions used by the running applications, so it contains useful debug
information. You can change the logging level to OFF, INFO, WARN, ERROR, FATAL,
DEBUG, TRACE or ALL. You can also export the listed information to a file.



3. Command-Line options:

Usage: java -Xmx512m -jar jpcsp.jar <OPTIONS>

  -d, --debugger             Open debugger at start.
  -f, --loadfile FILE        Load a file.
                             Example: ms0/PSP/GAME/pspsolitaire/EBOOT.PBP
  -u, --loadumd FILE         Load a UMD. Example: umdimages/cube.iso
  -r, --run                  Run loaded file or umd. Use with -f or -u option.




4. Requirements:

No accurate requirements are needed to use JPCSP (aside from OpenGL support), 
however, a good overall system is clearly a huge help.

Here are some recommended requirements:
- OS: Windows (XP/Vista/7) 32bit/64bit or 
      Linux (any up-to-date distribution) 32bit/64bit.
- CPU: 2.0 Ghz Intel Dual Core or 2.3 Ghz AMD Athlon 64 3200+.
- RAM: 1 Gb.
- Video Card: 256 Mb. 
              Requires full OpenGL support (version 2.0 is the recommended 
              minimum).

Please note that these settings are mostly a reflection of users' experience.




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
To avoid repeatedly editing the Compatibility settings each time you want to 
play a different game you can save them in a patch file that will get 
automatically loaded with the game.
Patch files go in the patches directory and are named after the game's Disc ID.

Please note when a game is using a patch file all compatibility settings in 
the user interface will be overridden regardless of their state.


- Debugger:
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


- Instruction Counter:
Lists all instructions and their frequency for the currently loaded program.


- File IO Logger:
Shows all IO activity. 
Useful for developers to make sure they are reading files once only and as 
quickly as possible. It can also tell you if you have forgotten to close a 
file handle.
This logger will need enabling from the Settings dialog.


- ELF Header Information:
Shows the ELF headers.


- Profiler:
The profiler is a method used by JPCSP to analyse repeated code sequences in
order to allow further optimization. If you wish, you can turn this feature on
in the "Settings". The data will be saved for a certain application after 
closing the emulator.


- ISO contents:
You can dump the current ISO/CSO image's contents into an illustrative .txt
file. In order to do this, go to "Debug">"Dump ISO to ISO-index.txt".

...............................................................................




			   	[The Team]
...............................................................................
...............................................................................
Developers:
- Owners:
	shadow,
	fiveofhearts,
	hlide,
	gid15,
	gigaherz,
	Orphis,
	mad,
	Nutzje,
	aisesal,
	shashClp.

- Committers:
	wrayal,
	dreampeppers99,
	spip2,
	mozvip,
	Drakon,
	Hykem,
	raziel1000,
	i30817,
	theball,
	J_BYYX.



Beta-testers:
	BlackDaemon,
	SilvX,
	s1n,
	Foxik.
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

JPCSP's Official Website and Forum:
- http://www.jpcsp.org
- http://jpcsp.org/forum

Emunewz:
- http://www.emunewz.net/forum/portal.php
...............................................................................