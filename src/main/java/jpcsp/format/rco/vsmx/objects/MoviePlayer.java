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
package jpcsp.format.rco.vsmx.objects;

import org.apache.log4j.Logger;

import jpcsp.GUI.UmdVideoPlayer;
import jpcsp.format.rco.vsmx.VSMX;
import jpcsp.format.rco.vsmx.interpreter.VSMXArray;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXFunction;
import jpcsp.format.rco.vsmx.interpreter.VSMXInterpreter;
import jpcsp.format.rco.vsmx.interpreter.VSMXNativeObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXNumber;
import jpcsp.format.rco.vsmx.interpreter.VSMXObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXString;
import jpcsp.format.rco.vsmx.interpreter.VSMXUndefined;

public class MoviePlayer extends BaseNativeObject {
	// FWVGA display resolution as default (854x480)
	public static final int DEFAULT_WIDTH = 854;
	public static final int DEFAULT_HEIGHT = 480;
	private static final Logger log = VSMX.log;
	public static final String objectName = "movieplayer";
	private VSMXInterpreter interpreter;
	private UmdVideoPlayer umdVideoPlayer;
	private VSMXNativeObject controller;
	private boolean playing = false;
	private boolean menuMode;
	private int playListNumber;
	private int chapterNumber;
	private int videoNumber;
	private int audioNumber;
	private int audioFlag;
	private int subtitleNumber;
	private int subtitleFlag;
	private int width = DEFAULT_WIDTH;
	private int height = DEFAULT_HEIGHT;
	private int x;
	private int y;

	public static VSMXNativeObject create(VSMXInterpreter interpreter, UmdVideoPlayer umdVideoPlayer, VSMXNativeObject controller) {
		MoviePlayer moviePlayer = new MoviePlayer(interpreter, umdVideoPlayer, controller);
		VSMXNativeObject object = new VSMXNativeObject(interpreter, moviePlayer);
		moviePlayer.setObject(object);

		object.setPropertyValue("audioLanguageCode", new VSMXString(interpreter, "en"));
		object.setPropertyValue("subtitleLanguageCode", new VSMXString(interpreter, "en"));

		return object;
	}

	private MoviePlayer(VSMXInterpreter interpreter, UmdVideoPlayer umdVideoPlayer, VSMXNativeObject controller) {
		this.interpreter = interpreter;
		this.umdVideoPlayer = umdVideoPlayer;
		this.controller = controller;

		if (umdVideoPlayer != null) {
			umdVideoPlayer.setMoviePlayer(this);
		}
	}

	public void play(VSMXBaseObject object,
	                 VSMXBaseObject pauseMode,
	                 VSMXBaseObject menuMode,
	                 VSMXBaseObject playListNumber,
	                 VSMXBaseObject chapterNumber,
	                 VSMXBaseObject videoNumber,
	                 VSMXBaseObject audioNumber,
	                 VSMXBaseObject audioFlag,
	                 VSMXBaseObject subtitleNumber,
	                 VSMXBaseObject subtitleFlag,
	                 VSMXBaseObject unknownBool) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MoviePlayer.play pauseMode=%d, menuMode=%d, playListNumber=%d, chapterNumber=%d, videoNumber=0x%X, audioNumber=0x%X, audioFlag=0x%X, subtitleNumber=%d, subtitleFlag=0x%X, unknownBool=%b", pauseMode.getIntValue(), menuMode.getIntValue(), playListNumber.getIntValue(), chapterNumber.getIntValue(), videoNumber.getIntValue(), audioNumber.getIntValue(), audioFlag.getIntValue(), subtitleNumber.getIntValue(), subtitleFlag.getIntValue(), unknownBool.getBooleanValue()));
		}
		playing = true;
		boolean previousMenuMode = this.menuMode;
		this.menuMode = menuMode.getBooleanValue();
		this.playListNumber = playListNumber.getIntValue();
		this.chapterNumber = chapterNumber.getIntValue();
		this.videoNumber = videoNumber.getIntValue();
		this.audioNumber = audioNumber.getIntValue();
		this.audioFlag = audioFlag.getIntValue();
		this.subtitleNumber = subtitleNumber.getIntValue();
		this.subtitleFlag = subtitleFlag.getIntValue();

		if (umdVideoPlayer != null) {
			umdVideoPlayer.play(this.playListNumber, this.chapterNumber, this.videoNumber, this.audioNumber, this.audioFlag, this.subtitleNumber, this.subtitleFlag);
		}

		// Going to menu mode?
		if (!previousMenuMode && this.menuMode) {
			// Call the "controller.onMenu" callback
			VSMXBaseObject callback = controller.getPropertyValue("onMenu");
			if (callback instanceof VSMXFunction) {
				VSMXBaseObject arguments[] = new VSMXBaseObject[0];
				interpreter.interpretFunction((VSMXFunction) callback, null, arguments);
			}
		}
	}

	public void stop(VSMXBaseObject object, VSMXBaseObject unknownInt, VSMXBaseObject unknownBool) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MoviePlayer.stop unknownInt=%d, unknownBool=%b", unknownInt.getIntValue(), unknownBool.getBooleanValue()));
		}
		playing = false;
	}

	public void resume(VSMXBaseObject object) {
		playing = true;
	}

	public VSMXBaseObject getResumeInfo(VSMXBaseObject object) {
		VSMXBaseObject resumeInfo;
		if (playing) {
			resumeInfo = new VSMXObject(interpreter, "ResumeInfo");
			resumeInfo.setPropertyValue("playListNumber", new VSMXNumber(interpreter, playListNumber));
		} else {
			resumeInfo = VSMXUndefined.singleton;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("MoviePlayer.getResumeInfo() returning %s", resumeInfo));
		}

		return resumeInfo;
	}

	public void changeResumeInfo(VSMXBaseObject object, VSMXBaseObject videoNumber, VSMXBaseObject audioNumber, VSMXBaseObject audioFlag, VSMXBaseObject subtitleNumber, VSMXBaseObject subtitleFlag) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MoviePlayer.changeResumeInfo videoNumber=0x%X, audioNumber=0x%X, audioFlag=0x%X, subtitleNumber=%d, subtitleFlag=0x%X", videoNumber.getIntValue(), audioNumber.getIntValue(), audioFlag.getIntValue(), subtitleNumber.getIntValue(), subtitleFlag.getIntValue()));
		}
		this.videoNumber = videoNumber.getIntValue();
		this.audioNumber = audioNumber.getIntValue();
		this.audioFlag = audioFlag.getIntValue();
		this.subtitleNumber = subtitleNumber.getIntValue();
		this.subtitleFlag = subtitleFlag.getIntValue();
	}

	public VSMXBaseObject getPlayerStatus(VSMXBaseObject object) {
		VSMXBaseObject playerStatus;
		if (playing) {
			playerStatus = new VSMXObject(interpreter, "PlayerStatus");
			playerStatus.setPropertyValue("playListNumber", new VSMXNumber(interpreter, playListNumber));
			playerStatus.setPropertyValue("chapterNumber", new VSMXNumber(interpreter, chapterNumber));
			playerStatus.setPropertyValue("videoNumber", new VSMXNumber(interpreter, videoNumber));
			playerStatus.setPropertyValue("audioNumber", new VSMXNumber(interpreter, audioNumber));
			playerStatus.setPropertyValue("audioFlag", new VSMXNumber(interpreter, audioFlag));
			playerStatus.setPropertyValue("subtitleNumber", new VSMXNumber(interpreter, subtitleNumber));
			playerStatus.setPropertyValue("subtitleFlag", new VSMXNumber(interpreter, subtitleFlag));
		} else {
			playerStatus = VSMXUndefined.singleton;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("MoviePlayer.getPlayerStatus() returning %s", playerStatus));
		}

		return playerStatus;
	}

	public void onPlayListEnd(int playListNumber) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MoviePlayer.onPlayListEnd playListNumber=%d", playListNumber));
		}

		VSMXBaseObject callback = getObject().getPropertyValue("onPlayListEnd");
		if (callback instanceof VSMXFunction) {
			VSMXBaseObject argument = new VSMXObject(interpreter, null);
			argument.setPropertyValue("playListNumber", new VSMXNumber(interpreter, playListNumber));

			VSMXBaseObject arguments[] = new VSMXBaseObject[1];
			arguments[0] = argument;
			interpreter.interpretFunction((VSMXFunction) callback, null, arguments);
		}
	}

	public void onChapter(int chapterNumber) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MoviePlayer.onChapter chapterNumber=%d", chapterNumber));
		}

		VSMXBaseObject callback = getObject().getPropertyValue("onChapter");
		if (callback instanceof VSMXFunction) {
			VSMXBaseObject argument = new VSMXObject(interpreter, null);
			argument.setPropertyValue("chapterNumber", new VSMXNumber(interpreter, chapterNumber));

			VSMXBaseObject arguments[] = new VSMXBaseObject[1];
			arguments[0] = argument;
			interpreter.interpretFunction((VSMXFunction) callback, null, arguments);
		}
	}

	public VSMXBaseObject getSize(VSMXBaseObject object) {
		VSMXInterpreter interpreter = object.getInterpreter();
		VSMXArray size = new VSMXArray(interpreter, 2);
		size.setPropertyValue(0, new VSMXNumber(interpreter, width));
		size.setPropertyValue(1, new VSMXNumber(interpreter, height));

		return size;
	}

	public void setSize(VSMXBaseObject object, VSMXBaseObject width, VSMXBaseObject height) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MoviePlayer.setSize(%s, %s)", width, height));
		}

		this.width = width.getIntValue();
		this.height = height.getIntValue();
	}

	public VSMXBaseObject getPos(VSMXBaseObject object) {
		VSMXInterpreter interpreter = object.getInterpreter();
		VSMXArray pos = new VSMXArray(interpreter, 2);
		pos.setPropertyValue(0, new VSMXNumber(interpreter, x));
		pos.setPropertyValue(1, new VSMXNumber(interpreter, y));

		return pos;
	}

	public void setPos(VSMXBaseObject object, VSMXBaseObject x, VSMXBaseObject y) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MoviePlayer.setPos(%s, %s)", x, y));
		}

		this.x = x.getIntValue();
		this.y = y.getIntValue();
	}

	public void onUp() {
		((Controller) controller.getObject()).onUp();
	}

	public void onDown() {
		((Controller) controller.getObject()).onDown();
	}

	public void onLeft() {
		((Controller) controller.getObject()).onLeft();
	}

	public void onRight() {
		((Controller) controller.getObject()).onRight();
	}

	public void onPush() {
		((Controller) controller.getObject()).onPush();
	}
}
