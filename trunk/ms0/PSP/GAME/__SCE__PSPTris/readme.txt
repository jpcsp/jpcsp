PSPTris version 0.5 by Sandberg
-------------------------------


Post comments, ideas, bugs etc. to : sandbergpsp@gmail.com


Installation Guide
------------------
I only have a 1.5 FW, so this is for 1.5 FW, but it has been tested on 2.0 and 2.6 FW also.
Copy the folders "__SCE_PSPTris" and "%__SCE_PSPTris" to your PSP under "\PSP\GAME\".


Credits
-------
Coding by Sandberg
Graphics by Semtex199
Music by Prophet



Game types
----------

Classic - This is the classic tetris game that we all know and love :-)
Color   - In this game you'll have to match 2 or more balls in the same color.


Game instructions
-----------------
Use START to end the intro sequence.

In the menus, use UP/DOWN for navigating and CROSS for selecting a menu and exiting a submenu.
If you press SELECT on the menu it will show you the version of PSPTris.

Classic:
In the game use LEFT (or LEFT_TRIGGER), RIGHT (or RIGHT_TRIGGER) and DOWN for moving the bricks.
UP will drop the current brick.
CROSS will rotate the current brick.
START will pause the game.

Points :
dropping the brick gives 5 points
1 line = 10  * level
2 line = 40  * level
3 line = 80  * level
4 line = 120 * level


Color:
In the game use the analog stick for moving the selection. The game will mark equal matching colors
automatically.
Use CROSS for selecting a match. This will remove all the matching colors, shift down the rows and
add new bricks at the top.
Use TRIANGLE for automatically move to a legal match. NOTE, this will only be possible every 5 seconds
and it will cost you 500 point (below 500 point you will not be able to use this feature).

Points:
The points are not linear, i.e. removing 2 times 2 balls will give less points than removing 1 x 4 balls.

The formula for the points are : points(N) = N*10 + (N-1)*10 .. + 10

e.g. : 2 balls gives 2*10 + 1*10 = 20 points, and 4 balls give 4*10 + 3*10 + 2*10 + 1*10 = 100 points.


Entering names on the highscore screen
--------------------------------------
(taken from the Danzeff OSK readme.txt, modified for the usage in PSPTris)

The danzeff keyboard is an OSK, mainly controlled by the analog stick.

Use the analog stick to select which square to chose from, and then press the button (X O [] /\) in the direction of the letter you want to input.

To switch into numbers mode Tap the L Shoulder.
To get capital Letters hold the R shoulder while in letters input.
To get a complete set of extra characters, hold down R shoulder while in numbers mode.

In PSPTris only letters and number are accepted.

Other special keys:
Digital down  -> enter (store the highscore)
Digital left  -> select next character
Digital right -> select previous character


Greetings and thanks goes to (in random order)
----------------------------------------------
PSPDEV Team - Amazing work you've done.
Tyranid - for creating PSPLink. This saves me sooo much time.
Danzel - Thanks for letting me use your OSK. It works perfect.



Later, Sandberg.
