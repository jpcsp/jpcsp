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

import jpcsp.format.rco.vsmx.VSMX;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXNativeObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXNumber;
import jpcsp.format.rco.vsmx.interpreter.VSMXObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXString;
import jpcsp.format.rco.vsmx.interpreter.VSMXUndefined;

public class MoviePlayer extends BaseNativeObject {
	private static final Logger log = VSMX.log;
	public static final String objectName = "movieplayer";
	private boolean playing = false;
	private int playListNumber;
	private int chapterNumber;
	private int videoNumber;
	private int audioNumber;
	private int audioFlag;
	private int subtitleNumber;
	private int subtitleFlag;

	public static VSMXNativeObject create() {
		MoviePlayer moviePlayer = new MoviePlayer();
		VSMXNativeObject object = new VSMXNativeObject(moviePlayer);
		moviePlayer.setObject(object);

		object.setPropertyValue("audioLanguageCode", new VSMXString("en"));
		object.setPropertyValue("subtitleLanguageCode", new VSMXString("en"));

		return object;
	}

	public void play(VSMXBaseObject object,
	                 VSMXBaseObject unknownInt1,
	                 VSMXBaseObject unknownInt2,
	                 VSMXBaseObject playListNumber,
	                 VSMXBaseObject chapterNumber,
	                 VSMXBaseObject videoNumber,
	                 VSMXBaseObject audioNumber,
	                 VSMXBaseObject audioFlag,
	                 VSMXBaseObject subtitleNumber,
	                 VSMXBaseObject subtitleFlag,
	                 VSMXBaseObject unknownBool) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MoviePlayer.play unknownInt1=%d, unknownInt2=%d, playListNumber=%d, chapterNumber=%d, videoNumber=0x%X, audioNumber=0x%X, audioFlag=0x%X, subtitleNumber=%d, subtitleFlag=0x%X, unknownBool=%b", unknownInt1.getIntValue(), unknownInt2.getIntValue(), playListNumber.getIntValue(), chapterNumber.getIntValue(), videoNumber.getIntValue(), audioNumber.getIntValue(), audioFlag.getIntValue(), subtitleNumber.getIntValue(), subtitleFlag.getIntValue(), unknownBool.getBooleanValue()));
		}
		playing = true;
		this.playListNumber = playListNumber.getIntValue();
		this.chapterNumber = chapterNumber.getIntValue();
		this.videoNumber = videoNumber.getIntValue();
		this.audioNumber = audioNumber.getIntValue();
		this.audioFlag = audioFlag.getIntValue();
		this.subtitleNumber = subtitleNumber.getIntValue();
		this.subtitleFlag = subtitleFlag.getIntValue();
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
			resumeInfo = new VSMXObject();
			resumeInfo.setPropertyValue("playListNumber", new VSMXNumber(playListNumber));
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
			playerStatus = new VSMXObject();
			playerStatus.setPropertyValue("playListNumber", new VSMXNumber(playListNumber));
			playerStatus.setPropertyValue("chapterNumber", new VSMXNumber(chapterNumber));
			playerStatus.setPropertyValue("videoNumber", new VSMXNumber(videoNumber));
			playerStatus.setPropertyValue("audioNumber", new VSMXNumber(audioNumber));
			playerStatus.setPropertyValue("audioFlag", new VSMXNumber(audioFlag));
			playerStatus.setPropertyValue("subtitleNumber", new VSMXNumber(subtitleNumber));
			playerStatus.setPropertyValue("subtitleFlag", new VSMXNumber(subtitleFlag));
		} else {
			playerStatus = VSMXUndefined.singleton;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("MoviePlayer.getPlayerStatus() returning %s", playerStatus));
		}

		return playerStatus;
	}
}
