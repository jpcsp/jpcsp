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


public class SceUtilityHtmlViewerParams extends pspAbstractMemoryMappedStructure {

    public pspUtilityDialogCommon base;
    public int dataAddr;
    public int dataSize;
    public int bookmarkNum;
    public int bookmarkAddr;
    public SceUtilityHtmlViewerBookmark bookmark;
    public int urlAddr;
    public String url;
    public int tabNum;
    public int userInterfaceLevel;
        public final static int PSP_UTILITY_HTMLVIEWER_USER_INTERFACE_FULL = 0;
        public final static int PSP_UTILITY_HTMLVIEWER_USER_INTERFACE_PARTIAL = 1;
        public final static int PSP_UTILITY_HTMLVIEWER_USER_INTERFACE_NONE= 2;
    public int initFlag;
        public final static int PSP_UTILITY_HTMLVIEWER_USE_START_SCREEN = 0x1;
        public final static int PSP_UTILITY_HTMLVIEWER_DISABLE_RESTRICTIONS = 0x2;
    public int fileDownloadAddr;
    public SceUtilityHtmlViewerFile fileDownload;
    public int fileUploadAddr;
    public SceUtilityHtmlViewerFile fileUpload;
    public int fileConfigAddr;
    public SceUtilityHtmlViewerConfig fileConfig;
    public int disconnectAutoFlag;
        public final static int PSP_UTILITY_HTMLVIEWER_DISCONNECT_AUTO_ON = 0;
        public final static int PSP_UTILITY_HTMLVIEWER_DISCONNECT_AUTO_OFF = 1;
        public final static int PSP_UTILITY_HTMLVIEWER_DISCONNECT_AUTO_ASK = 2;

    public static class SceUtilityHtmlViewerBookmark extends pspAbstractMemoryMappedStructure {
        public int urlAddr;
        public String url;
        public int titleAddr;
        public String title;
        public int unk1;
        public int unk2;

		@Override
		protected void read() {
            urlAddr = read32();
            url = readStringUTF16Z(urlAddr);
            titleAddr = read32();
            title = readStringUTF16Z(titleAddr);
            unk1 = read32();
            unk2 = read32();
		}

		@Override
		protected void write() {
            write32(urlAddr);
            writeStringUTF16Z(urlAddr, url);
            write32(titleAddr);
            writeStringUTF16Z(titleAddr, title);
            write32(unk1);
            write32(unk2);
		}

		@Override
		public int sizeof() {
			return 4 * 4;
		}
	}

    public static class SceUtilityHtmlViewerFile extends pspAbstractMemoryMappedStructure {
        public int pathAddr;
        public String path;
        public int fileNameAddr;
        public String fileName;

		@Override
		protected void read() {
            pathAddr = read32();
            path = readStringUTF16Z(pathAddr);
            fileNameAddr = read32();
            fileName = readStringUTF16Z(fileNameAddr);
		}

		@Override
		protected void write() {
            write32(pathAddr);
            writeStringUTF16Z(pathAddr, path);
            write32(fileNameAddr);
            writeStringUTF16Z(fileNameAddr, fileName);
		}

		@Override
		public int sizeof() {
			return 2 * 4;
		}
	}

    public static class SceUtilityHtmlViewerConfig extends pspAbstractMemoryMappedStructure {
        public int cookiePolicyFlag;
            public final static int PSP_UTILITY_HTMLVIEWER_COOKIE_POLICY_REJECT = 0;
            public final static int PSP_UTILITY_HTMLVIEWER_COOKIE_POLICY_ACCEPT = 1;
            public final static int PSP_UTILITY_HTMLVIEWER_COOKIE_POLICY_ASK = 2;
            public final static int PSP_UTILITY_HTMLVIEWER_COOKIE_POLICY_DEFAULT = 2;
        public int cacheSize;
        public int homeUrlAddr;
        public String homeUrl;

		@Override
		protected void read() {
            cookiePolicyFlag = read32();
            cacheSize = read32();
            homeUrlAddr = read32();
            homeUrl = readStringUTF16Z(homeUrlAddr);
		}

		@Override
		protected void write() {
            write32(cookiePolicyFlag);
            write32(cacheSize);
            write32(homeUrlAddr);
            writeStringUTF16Z(homeUrlAddr, homeUrl);
		}

		@Override
		public int sizeof() {
			return 3 * 4;
		}
	}

    @Override
	protected void read() {
        base = new pspUtilityDialogCommon();
        read(base);
        setMaxSize(base.totalSizeof());

        dataAddr = read32();
        dataSize = read32();
        bookmarkNum = read32();
        bookmarkAddr = read32();
        if (bookmarkAddr != 0) {
			bookmark = new SceUtilityHtmlViewerBookmark();
			bookmark.read(mem, bookmarkAddr);
		} else {
			bookmark = null;
		}
        urlAddr = read32();
        url = readStringUTF16Z(urlAddr);
        tabNum = read32();
        userInterfaceLevel = read32();
        initFlag = read32();
        fileDownloadAddr = read32();
        if (fileDownloadAddr != 0) {
			fileDownload = new SceUtilityHtmlViewerFile();
			fileDownload.read(mem, fileDownloadAddr);
		} else {
			fileDownload = null;
		}
        fileUploadAddr = read32();
        if (fileUploadAddr != 0) {
			fileUpload = new SceUtilityHtmlViewerFile();
			fileUpload.read(mem, fileUploadAddr);
		} else {
			fileUpload = null;
		}
        fileConfigAddr = read32();
        if (fileConfigAddr != 0) {
			fileConfig = new SceUtilityHtmlViewerConfig();
			fileDownload.read(mem, fileConfigAddr);
		} else {
			fileConfig = null;
		}
        disconnectAutoFlag = read32();
    }

    @Override
	protected void write() {
        write(base);
        setMaxSize(base.totalSizeof());

        write32(dataAddr);
        write32(dataSize);
        write32(bookmarkNum);
        write32(bookmarkAddr);
        if (bookmark != null && bookmarkAddr != 0) {
			bookmark.write(mem, bookmarkAddr);
		}
        write32(urlAddr);
        writeStringUTF16Z(urlAddr, url);
        write32(tabNum);
        write32(userInterfaceLevel);
        write32(initFlag);
        write32(fileDownloadAddr);
        if (fileDownload != null && fileDownloadAddr != 0) {
			fileDownload.write(mem, fileDownloadAddr);
		}
        write32(fileUploadAddr);
        if (fileUpload != null && fileUploadAddr != 0) {
			fileUpload.write(mem, fileUploadAddr);
		}
        write32(fileConfigAddr);
        if (fileConfig != null && fileConfigAddr != 0) {
			fileConfig.write(mem, fileConfigAddr);
		}
        write32(disconnectAutoFlag);
    }

    @Override
	public int sizeof() {
        return base.totalSizeof();
    }

    @Override
    public String toString() {
        // TODO
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }
}