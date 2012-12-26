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
v.07:
release date open
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
Jpcsp is the most advanced PlayStation Portable emulator,
allowing you to play your PSP games on a PC.
Jpcsp has been started in July 2008 and is developed by a small but active team.

Even though Jpcsp is written in Java, it can already reach 100% PSP speed on
a lot of commercial games... and the emulator performance is constantly increasing.

Jpcsp takes full advantage of dual-core processors, matching the PSP dual-core
architecture. Even a quad-core can give a small performance improvement by leaving
free CPU cores for the Java JIT Compiler and the graphics cache.

NVIDIA graphic cards are fully supported and Jpcsp can take advantage of powerful GPUs.
Problems have been reported with ATI/AMD graphic cards: keep you Catalyst driver
up-to-date as new releases might fix issues.

The project is still a beta release, but currently more than 300 games are already
playable. See the list on the official Forum.

Video and Atrac3 sound are supported, but only on Windows 32bit systems.
Atrac3+ sound is not directly supported as there is currently no free codec available
for this format. However, Atrac3+ can be decoded automatically on Windows 32bit
when installing SonicStage.

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

JPCSP v0.7 (????????):

-> Added support for rendering in software mode, not using the GPU;

-> Added Game Pad support;

-> Major performance improvements in the graphic processing (see new Video options);

-> Automatic decoding of stereo ATRAC3+ audio using SonicStage;

-> Rendering of fonts using "sceFont". OpenSource fonts are provided as a replacement
   for the PSP fonts. But the PSP original fonts can be used for maximal compatibility;

-> Automatic decryption of encrypted EBOOT.BIN and PGD files.
   Savedata files can also be loaded/saved in an encrypted form, like on a real PSP;

-> Added the configuration of regional settings
   under "Options" > "Configuration" > "Region";

-> Configuration settings can be changed "on the fly",
   i.e. the changes are effective immediately.

-> Draft implementation for the support of Video UMDs;

-> Basic network support:
   Infrastructure network is almost complete.
   Adhoc network is only working between two Jpcsp, not with a real PSP.
   Port shifting is available to support running two Jpcsp instances on the same computer.
   Signin to the PlayStation Network is not supported at all.

-> A lot of compatibility improvements in almost all of the PSP modules.

-> Major code cleanup in the module architecture (now using Java reflection).

-> New method used for computing FPS. The FPS value is now more reliable but usually lower
   than on previous releases. It doesn't mean that the Jpcsp performance is now worse,
   but just that the previous FPS counter was over-optimistic.

-> Added Frame skipping option;

-> Added cheat import from cheat.db

-> Added export function for the current 3D scene in Wavefront .obj/.mtl format,
   including the textures

-> Added texture modding

-> Added functions to clear the texture and vertex caches

-> Added option to change the PSP clock speed




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

-> Beginning of threaded IOAsync operations implementation;

-> General fixes for module loading;

-> Small improvements of HLE functions.
...............................................................................




			        [JPCSP FAQ]
...............................................................................
...............................................................................

1. Getting started:

Be sure to have JRE (Java Runtime Environment) installed in your computer
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




4. Command-Line options:

Usage: java -Xmx1024m -Xss2m -XX:MaxPermSize=128m -XX:ReservedCodeCacheSize=64m -Djava.library.path=lib/windows-x86 -jar bin/jpcsp.jar <OPTIONS>

Available <OPTIONS>:

  -d, --debugger             Open debugger at start.
  -f, --loadfile FILE        Load a file.
                             Example: ms0/PSP/GAME/pspsolitaire/EBOOT.PBP
  -u, --loadumd FILE         Load a UMD. Example: umdimages/cube.iso
  -r, --run                  Run loaded file or umd. Use with -f or -u option.
  -t, --tests                Run the automated tests.
  --netClientPortShift N     Increase Network client ports by N (e.g. N = 100).
                             Only required when running 2 Jpcsp instances on the same computer.
  --netServerPortShift N     Increase Network server ports by N (e.g. N = 100).
                             Only required when running 2 Jpcsp instances on the same computer.




5. Requirements:

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




6. Advanced features:

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
Game Pads are supported by Jpcsp. They can be configured under "Options -> Controls".
The keyboard support is always active, even when using a Game Pad. I.e. you can use
both the game pad and keyboard controls.
The list of save games can also be controlled using the Game Pad: Up/Down to move
the selection, [X] for select and [O] for Cancel (or the opposite depending on the
region button preference).


- Patch files:
To avoid repeatedly editing the settings each time you want to play a different
game you can save them in a patch file that will get automatically loaded with 
the game. Patch files go in the "patches" directory and are named after the 
game's Disc ID.

Please note when a game is using a patch file all compatibility settings in 
the user interface will be overridden regardless of their state.


- Media Engine:
The "Media Engine" can be enabled under "Options" > "Configuration" > "Media".
This allows JPCSP to use the FFMPEG's wrapper Xuggler to decode and playback
ingame videos (instead of faked MPEG data) and audio (ATRAC3 only).
The playback of ATRAC3+ audio is only available when the configuration option
"Decode audio files with SonicStage" is enabled
(under "Options" > "Configuration" > "Media") and when SonicStage is installed
on your computer. SonicStage (http://en.wikipedia.org/wiki/Sonicstage)
is not provided by Jpcsp and must be installed separately. The playback of monaural
ATRAC3+ audio has been reported to not work. Mono ATRAC3+ cannot be decoded by
SonicStage, this is a restriction of this product.


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
Contains internal information on VFPU operations.


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
The profiler is a method used by JPCSP to analyze repeated code sequences in
order to allow further optimization. If you wish, you can turn this feature on
under "Options" > "Configuration" > "General". The data will be saved in the file
"profiler.txt" after closing the emulator.
Use "Reset Profiler Information" under "Debug" to clear the current profiler data
and re-start collecting profiler information from now on.

For additional information, see the detailed profiler information below.

- ISO contents:
You can dump the current ISO/CSO image's contents into an illustrative .txt
file named iso-index.txt.
In order to do this, go to "Debug" > "Dump ISO to ISO-index.txt".


- Memory breakpoints:
You can set memory read or write breakpoints: create a file named "Memory.mbrk"
in the main directory. When this file is present, the DebuggerMemory is automatically
activated when Jpcsp is started. Expect a small performance drop.
The format of the file is quite simple:

   R 0xXXXXXXXX
   W 0xXXXXXXXX
   RW 0xXXXXXXXX
   R 0xXXXXXXXX - 0xYYYYYYYY
   W 0xXXXXXXXX - 0xYYYYYYYY
   RW 0xXXXXXXXX - 0xYYYYYYYY
   read|write|read8|write8|read16|write16|read32|write32|pause

to set read (R), write (W) and read-write (RW) breakpoints on a single address
or an address range.
The last line is to enable traces of the corresponding reads and writes. One or
multiple of these options can be specified (e.g., only "write" or "read32|write32").
When "pause" is specified, the emulator is pausing when reading/writing the selected
addresses. Otherwise, the emulator is just logging at INFO level the memory access.


- Export 3D scene:
You can export the current 3D scene of an application by selecting the corresponding
file menu option. This will export the current 3D scene in Wavefront format (.obj/.mtl),
including the textures used in the scene. The textures are stored in 32-bit .png format.
You can then import the .obj file in a 3D modeling application (e.g. http://www.blender.org/).
Lamps cannot be exported in Wavefront format, so you will probably need to manually add
lighting to the imported scene.
You can export the whole 3D scene or only the visible objects (to reduce the size of the
exported file). The scenes are exported under:
    <Jpcsp main directory>/export/Export-1/...
    <Jpcsp main directory>/export/Export-2/...


- Texture modding:
You can try to modify one or multiple of the textures to perform modding of your application
without changing its code.
Following these steps:
  a. export the 3D scene. The exported files will be located under the "export" directory.
  b. change one or multiple texture files (.png). Keep the name of these files unchanged.
  c. move the modified texture files to the following directory:
        tmp/<DiscID>/Textures/Image0XXXXXXX.png (or Image0XXXXXXX_0XXXXXXX.png)
     The DiscID has to be replaced by the 9-character Disc-ID of the application.
     The DiscID is displayed, for example, by the UMD Browser.
     Only move the textures you have really modified. The loading of texture files
     is slower.
  d. Restart your application. The modded texture files should be loaded instead of the
     application native ones.



7. Explanation of the advanced Video options:

I must also admit that the Video configuration options
(under "Options" > "Configuration" > "Video") are not always self-explanatory.
This is because they are related on how to map the low-level PSP graphic functions
to the OpenGL functions.
OpenGL provides some optimization techniques for application developers when they
follow some basic principles (e.g. grouping the display of similar graphics together,
reusing the same data at each frame...). The PSP does not use the same optimization
techniques, e.g. there is no real advantage in grouping similar graphics together,
or reusing the same data at each frame. So, the PSP programmers are optimizing for
the PSP, which is not the same as optimizing for OpenGL.
The Video configuration options allow the activation/deactivation of OpenGL techniques:
they might improve some games if their programmers by chance more or less followed
the OpenGL principles, but they also might decrease the performance if the game
programmers did something completely different (which is also legitimate on a PSP).

So, to the different options:
- Disable VBO:
    using OpenGL VBO (http://www.opengl.org/wiki/Vertex_Buffer_Object) should always
    bring a win. This option is probably useless.
- Only GE graphics:
    the PSP allows drawing using GE commands or by writing directly to the PSP
    framebuffer memory. With OpenGL, supporting both methods is cost expensive.
    When this option is activated, only the drawing using GE commands is supported.
    If the application writes directly to the PSP framebuffer memory, this is ignored.
    As a side effect, a more accurate FPS is displayed when enabling this option.
    When disabled, the displayed FPS is over-optimistic. This is why a lower FPS but
    a smoother/faster play is often reported. Trust the faster play, not the FPS ;-).
- Use Vertex Cache:
    when enabled, parts of the graphics (positions, colors, bones...) are loaded on
    the graphic card and reused from frame to frame when their data is not changing.
    This has however a negative impact if the game programmers are changing their data
    (e.g. the positions) very often.
    By the way, a texture cache is always used and cannot be disabled.
- Use shaders:
    use vertex and fragment shaders to implement most of the PSP functions. Some of
    the PSP functions cannot be implemented without the use of shaders, so this option
    should provide the most accurate rendering. Unfortunately, some shader
    implementations are somewhat buggy, depending on the graphic card used
    (e.g. AMD/ATI or Intel).
- Use a Geometry shader for 2D rendering:
    when using shaders, this option might bring a slight performance improvement for 2D
    applications.
- Disable UBO:
    when using shaders, OpenGL UBO's (http://www.opengl.org/wiki/Uniform_Buffer_Object)
    should bring a better performance. But again, some graphic card drivers have
    sometimes buggy implementations. This is why this option is enabled by default,
    and only a "Disable" option is available.
- Enable VAO:
    an OpenGL optimization (http://www.opengl.org/wiki/Vertex_Array_Object) when
    similar graphics are grouped together. This is just available as an option as most
    PSP programmers do not following this approach.
- Enable saving GE screen to Textures:
    the content of the PSP framebuffer is kept in an OpenGL texture instead of the PSP
    memory: This allows faster load/save from OpenGL, but breaks compatibility if the
    application is manipulating directly the framebuffer memory.
- Enable decoding of indexed textures (using CLUT) in shader:
    this option brings a performance boost when combined with
    "Enable saving GE screen to Textures" and when the application is doing
    manipulations on the Red/Green/Blue color channels of the framebuffer
    (e.g. to implement some graphic effects, blurs or shadows). Available only as an
    option as it might break the compatibility for other applications...
- Enable dynamic shader generation:
    we have a single shader implementing all the PSP functions. This shader contains
    of lot of condition tests ("if (mode==0) then xxx", "if (mode==1) then yyy") to
    support all the combinations. This option enables the generation of a separate
    shader for each combination. E.g., when mode==0, we create one shader containing
    only "xxx" and when mode==1, we create another shader containing only "yyy".
    Each of these shaders will then execute faster because it can avoid the condition
    test. Due to the large number of possible combinations, this could result in the
    generation of several hundred different shaders. As this might overload the graphic
    card driver, this feature is only available as an option. As a side effect, some
    graphic card drivers (e.g. AMD/ATI) are reported to be less buggy when using this
    option.
- Enable the shader implementation for the "Stencil Test":
    the PSP supports a "stencil" function used to implement some graphical effects.
    This function cannot be implemented correctly using the standard OpenGL functions.
    When enabling this option, an implementation matching the PSP features is
    activated through the shaders. The option is only relevant when using the shaders
    and is only available as an option because it has a negative impact on the
    performance (lower FPS).
    If your application is not displaying correctly or is logging the warning
        "Both different SFIX (NNNNNNNN) and DFIX (NNNNNNNN) are not supported"
    you might try this option.
- Enable the shader implementation for the "Color Mask":
    if your application is logging the warning
        "Unimplemented Red/Green/Blue mask 0xNN"
    you might try this option. It might increase the quality of the rendered graphics,
    but is only relevant when using shaders.
    It is only available as an option because it has a negative impact on the
    performance (lower FPS).
- Disable optimized VertexInfo reading:
    you might try this option if graphics are sometimes corrupted.
    This option has a negative impact on the performance (lower FPS), but
    provides higher compatibility.
- Use Software Rendering:
    this option enables the emulation of all the PSP graphics in software by the emulator.
    The hardware of your graphics card is not used (well, it will just be used
    at the very end of the rendering to show the rendered image).
    The software rendering is much slower than the hardware-based rendering using
    a modern graphics card (GPU). But it allows a much higher degree of compatibility
    and avoids rendering problems related to buggy OpenGL drivers.


8. PSP fonts

When enabling the option "Use non-native fonts from flash0 folder"
(under "Options" > "Configuration" > "Media"), OpenSource fonts are used as a
replacement for the PSP native fonts. The fonts do not match 100% the PSP but
provide a quite good approximation. For 100% compatibility, the original fonts
from your PSP can be used. Copy the files under "flash0:/fonts" on your PSP to
Jpcsp "flash0/fonts" directory.


9. FPS and Frame Skipping

Starting with r2471, a new FPS counter has been introduced. It produces more reliable
FPS values reflecting the real number of different frames displayed by the PSP during
the last second.
The FPS counter used in previous releases was over-optimistic and was reporting
too high FPS values. Don't worry if you see reports or videos from previous releases
showing a higher FPS number. It doesn't mean that Jpcsp's performance is getting
worse: the frames are displayed at the same speed as before (assuming same hardware
and same compatibility options), the FPS counter is just showing a more accurate value.

In the Options menu, under Video, a frame skipping feature is available. It allows to define
a desired FPS value. Jpcsp will then skip as many frames as required to try to reach
this FPS value. The desired FPS value can only be reached if the application is able
to produce frames at this rate. Some applications can run at 60 FPS, others only
at 30 FPS and a few run at full speed with even a lower FPS rate. The PSP does not
require a fixed FPS rate for an application to run correctly, there is no general rule.
Also, Jpcsp will only skip a maximum of 75% of the frames, a minimum of 25% will always
be displayed. Skipping more than 75% of the frames would make the application run very jerky.


10. Profiler

10.1 What is profiling and how to use it?

Profiling in Jpcsp is a dynamic analysis of the PSP application being run
that measures the time spent by the PSP application in different parts
of its code and that collects different statistics in order to help
the development of further Jpcsp optimizations.

The Jpcsp profiler is collecting information about
- the PSP CPU: i.e. the frequency and duration of all the application
  MIPS/Allegrex function calls;
- the PSP GPU: i.e. the frequency and duration of the critical graphic
  commands used to render the application display.

For those reading the Wikipedia article on profiling
    http://en.wikipedia.org/wiki/Profiling_%28computer_programming%29
the Jpcsp profiler is a "Flat" and "Instrumenting" profiler. 

The profiler has to be enabled in the compiler configuration options:
Options -> Configuration -> Compiler tab -> select "Output profiler to profiler.txt".
It has to be enabled before running the application i.e., it cannot be enabled "on-the-fly".
The profiler information is then collected when starting the application
during the whole application run until leaving Jpcsp.
When Jpcsp is closed, the complete profiler information is written into a file "profiler.txt".

Also, when enabling the profiler, keep the other compiler option
"Maximum method size" to its default value of 3000.


10.2 How to make the best use of profiling

An application run is typically having the following phases:
- Intro (developer Logos)
- Loading
- Menu
- Video
- In-game play
Some of them might be mixed (e.g. further loading or video's can happen
during the in-game play), but the approach is usually very similar.
When profiling, it is important to understand that these phases usually
involve different part of the application code or are using different graphics
rendering.
For example, a loading phase is typically involving the application code
doing the reading of files, the parsing or decoding of data and preparing the data
structures in PSP memory. The graphics during a Loading phase are very simple,
usually only a "Loading..." graphic.
The display of a video is involving a completely different part of the application
code i.e., the part calling the PSP functions to decode and display Mpeg videos,
including the Mpeg audio.
The in-game play is usually the most CPU and GPU intensive part of the application,
where game logic has to be applied, complex graphics have to be rendered and
background music or sound effects have to be output.
As a rough estimation, the following overview usually applies:
- Intro: low PSP CPU usage, simple graphics, no or simple audio (BGM)
- Loading: high PSP CPU usage, simple graphics, no audio
- Menu: low PSP CPU usage, simple graphics, simple audio (BGM)
- Video: low PSP CPU usage (but high HLE emulation usage to decode the Mpeg),
         simple graphics (Video image), simple audio (Video audio)
- In-game play: high PSP CPU usage, complex graphics,
                complex audio (mix of BGM and sound effects)

When profiling the whole application from the beginning, all the different
phases are mixed together in the profiler data and it is then not always
obvious which part of Jpcsp has to be optimized in order to bring the most
benefit. As the in-game play is usually the phase having the lowest performance
(i.e. the lowest FPS), it makes sense to collect only profiling information
for that phase so that it is not mixed with other information which could
bias the observations. For this, Jpcsp has the menu option
"Debug -> Reset Profiler Information": when reaching the part of the application
having the low performance that you would like to profile (it could be only
some particular scenes from the in-game play that have a poor performance),
select the menu option to reset the profiler information, let Jpcsp run in that
part for at least 10 seconds (so that enough statistical information can be
collected) and then close Jpcsp. The generated profiler file will then only
contain information since the last reset of the profiler.
The profiler file and the associated log file (at INFO level) have then to be
posted on the official forum under the related game thread. Also explain for
which part you collected the profiler information (e.g. for the whole run
or if you have reset the profiler at some point).
This profile file is then analysed by the Jpcsp development team and
the collection information might help improving the Jpcsp performance.
Note that it doesn't necessarily mean that the performance will be
improved, but at least it gives some chances to have it improved...


10.3 Analyzing the output of the profiler

This chapter is just to give an insight on how the development team is analyzing the
information collected by the profiler.

The profiler.txt is organized in 2 sections:
- first, the CodeBlocks profiling information, describing the most intensively
  used code blocks (one code block is usually corresponding to one C function
  in the source code of the application): the code blocks are sorted based on
  the number of instructions dynamically executed within the code block.
- second, the Graphical Engine (GE) profiler is displaying statistics on
  selected GE commands (PRIM, BEZIER, SPLINE, TRXKICK, BBOX) and the most
  used VTYPE parameters.

Each CodeBlock starts with a line like:
	_S1_2_8900B88 602.910 instructions (52,461%), 630 calls (08900B88 - 08900C38, length 45)
The number of instructions dynamically executed in this CodeBlock is listed, with
a percentage compared to the total number of instructions executed in all the CodeBlocks.
When this percentage is high (usually above 10%), this CodeBlock is worth looking closer at it.
We want to optimize only those parts that have the most impact!
The number of times this CodeBlock was called is also given: a high number of instructions and
a low number of calls means this is a CodeBlock performing a lot of loops internally.
A lot of loops is good for optimizations: this is usually where the most benefit can be gained.
When the number of calls is very high, it means that the CodeBlock is executing "straight"
without loops and an optimization will probably not be very effective. It is probably
better to look at the caller: why is this CodeBlock called so often?
The position in memory of the CodeBlock (lowest address - highest address) and the static number of
MIPS instructions is also listed.

After this first line, the complete disassembled code of the CodeBlock is listed, but only
if it is not too large (the limit is based on the number of MIPS instructions and can be configured
at jpcsp.Allegrex.compiler.Profiler.codeLogMaxLength).

After complete disassembled code, the back branches executed the most are separately listed.
For example:
	Back Branch 08900C2C 2.520 times (length 11)
	    08900BF4:[1568FFE8]: bne        $t3, $t0, 0x08900B98
	    08900BF8:[000B1100]: sll        $v0, $t3, 0x0004
	    08900BFC:[00803021]: addu       $a2, $a0, $zr <=> move $a2, $a0
	    08900C00:[03A03821]: addu       $a3, $sp, $zr <=> move $a3, $sp
	    08900C04:[27A80040]: addiu      $t0, $sp, 64
	--> 08900C08:[8CE20000]: lw         $v0, 0($a3)
	    08900C0C:[8CE30004]: lw         $v1, 4($a3)
	    08900C10:[8CE40008]: lw         $a0, 8($a3)
	    08900C14:[8CE5000C]: lw         $a1, 12($a3)
	    08900C18:[24E70010]: addiu      $a3, $a3, 16
	    08900C1C:[ACC20000]: sw         $v0, 0($a2)
	    08900C20:[ACC30004]: sw         $v1, 4($a2)
	    08900C24:[ACC40008]: sw         $a0, 8($a2)
	    08900C28:[ACC5000C]: sw         $a1, 12($a2)
	--> 08900C2C:[14E8FFF6]: bne        $a3, $t0, 0x08900C08
	    08900C30:[24C60010]: addiu      $a2, $a2, 16
	    08900C34:[03E00008]: jr         $ra
	    08900C38:[27BD0040]: addiu      $sp, $sp, 64
A back branch is usually (but not necessarily) indicating a loop.
A few context instructions are also listed before and after the back branch so that
it can be read more easily. The branching and the target instructions are marked
with "-->".
In the above example, a typical memory copy can be recognized (copying 16 bytes
at each loop).

The best optimization result is when the functionality of the CodeBlock
can be recognized as a whole, for example, if the CodeBlock is implementing
a typical libc function like memcpy or strlen.
In that case, the CodeBlock can be added to the Compiler.xml file and a native
Java implementation (if not yet available) can be added.
The effort is worth optimizing such cases as such functions are usually
reused in several applications and are coming from common base libraries.

Other optimization possibilities are in the loops (based on the Back Branch
analysis): only the loops having the most impact (i.e. number of times the loop
is executed times the loop length) should be analysed. Here, the potential of
re-usability in other applications is quite low as there are good chances that
the C compiler will compile part of the source code with a different pattern
each time (different registers used or different instructions sequence).
Most of these optimizations concern the recognition of partial memcpy
operations (those that were inlined by the C compiler as an optimization).

The section of the GE profiler lists different statistics converning the
graphics display:
- number of GE lists (number of sceGeListEnqueue...)
- number of texture loads (texture found in the cache are not counted).
  This is to check that the texture cache is doing a good job.
- Copy GE to memory: number of times the GE is copied back to memory
- Copy Stencil to memory: number of times the Stencil information is copied back to memory
- PRIM, BEZIER, SPLINE, TRXKICK, BBOX: number of times these commands were called
- different VTYPE combinations, sorted, most used first.
Each statistic is giving the total number of calls and the average
number of calls per GE list (total number of calls divided by the number of GE lists).
The average per GE list is probably the most relevant information.

...............................................................................




			   	[The Team]
...............................................................................
...............................................................................
JPCSP Team (active):
- gid15
- Hykem
- soywiz
- Orphis
- shadow

Past members and contributors (inactive):
- hlide
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
- Darth1701
- Hyakki
- Kai3213
- MaXiMu
- dista_bagus
- beanclr
- Vegerunks
- pmk
- serio
- shakirmole
- otenki
- mordaki987
- legend80
- Itaru
- theball
- virgil94
- Pinguito
- andutrache
- hyperspeedgx
- l2sp
- ionelush2001
- rcoltrane
- montcer9012
- nash67
- sum2012
...and a lot more

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




			   	[Links]
...............................................................................
...............................................................................
JPCSP's Google Code repository:
- http://code.google.com/p/jpcsp

JPCSP's Official Website:
- http://www.jpcsp.org

JPCSP's Official Forum (hosted at Emunewz.net):
- http://www.emunewz.net/forum/forumdisplay.php?fid=51

Official recent SVN builds can be found at:
- http://buildbot.orphis.net/jpcsp/
...............................................................................