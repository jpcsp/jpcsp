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
    public SceUtilityHtmlViewerFile fileDownload;
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
            url = readStringZ(urlAddr);
            titleAddr = read32();
            title = readStringZ(titleAddr);
            unk1 = read32();
            unk2 = read32();
		}

		@Override
		protected void write() {
            write32(urlAddr);
            writeStringZ(url, urlAddr);
            write32(titleAddr);
            writeStringZ(title, titleAddr);
            write32(unk1);
            write32(unk2);
		}

		@Override
		public int sizeof() {
			return 4 * 4;
		}

		@Override
		public String toString() {
			return String.format("SceUtilityHtmlViewerBookmark[url='%s', title='%s']", url, title);
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
            path = readStringZ(pathAddr);
            fileNameAddr = read32();
            fileName = readStringZ(fileNameAddr);
		}

		@Override
		protected void write() {
            write32(pathAddr);
            writeStringZ(path, pathAddr);
            write32(fileNameAddr);
            writeStringZ(fileName, fileNameAddr);
		}

		@Override
		public int sizeof() {
			return 2 * 4;
		}

		@Override
		public String toString() {
			return String.format("SceUtilityHtmlViewerFile[path='%s', fileName='%s']", path, fileName);
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
            homeUrl = readStringZ(homeUrlAddr);
		}

		@Override
		protected void write() {
            write32(cookiePolicyFlag);
            write32(cacheSize);
            write32(homeUrlAddr);
            writeStringZ(homeUrl, homeUrlAddr);
		}

		@Override
		public int sizeof() {
			return 3 * 4;
		}

		@Override
		public String toString() {
			return String.format("SceUtilityHtmlViewerConfig[cookiePolicy=0x%X, cacheSize=0x%X, homeUrl='%s']", cookiePolicyFlag, cacheSize, homeUrl);
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
        url = readStringZ(urlAddr);
        tabNum = read32();
        userInterfaceLevel = read32();
        initFlag = read32();
		fileDownload = new SceUtilityHtmlViewerFile();
		read(fileDownload);
		fileUpload = new SceUtilityHtmlViewerFile();
		read(fileUpload);
		fileConfig = new SceUtilityHtmlViewerConfig();
		read(fileConfig);
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
        write(fileDownload);
        write(fileUpload);
        write(fileConfig);
        write32(disconnectAutoFlag);
    }

    @Override
	public int sizeof() {
        return base.totalSizeof();
    }

    @Override
    public String toString() {
        return String.format("SceUtilityHtmlViewerParams[dataAddr=0x%08X, dataSize=0x%X, bookmarkNum=%d, url='%s', fileDownload=%s, fileUpload=%s, fileConfig=%s]", dataAddr, dataSize, bookmarkNum, url, fileDownload, fileUpload, fileConfig);
    }
}