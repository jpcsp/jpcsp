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
package jpcsp.HLE.VFS.compress;

import static jpcsp.HLE.modules.IoFileMgrForUser.PSP_O_RDONLY;
import static jpcsp.HLE.modules.IoFileMgrForUser.PSP_O_RDWR;
import static jpcsp.format.PSP.AES_KEY_SIZE;
import static jpcsp.format.PSP.CHECK_SIZE;
import static jpcsp.format.PSP.CMAC_DATA_HASH_SIZE;
import static jpcsp.format.PSP.CMAC_HEADER_HASH_SIZE;
import static jpcsp.format.PSP.CMAC_KEY_SIZE;
import static jpcsp.format.PSP.KEY_DATA_SIZE;
import static jpcsp.format.PSP.PSP_HEADER_SIZE;
import static jpcsp.format.PSP.PSP_MAGIC;
import static jpcsp.format.PSP.SCE_KERNEL_MAX_MODULE_SEGMENT;
import static jpcsp.format.PSP.SHA1_HASH_SIZE;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.readUnaligned16;
import static jpcsp.util.Utilities.readUnaligned32;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import jpcsp.HLE.VFS.AbstractProxyVirtualFileSystem;
import jpcsp.HLE.VFS.ByteArrayVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.kernel.types.SceIoDirent;
import jpcsp.format.Elf32Header;
import jpcsp.util.Utilities;

/**
 * Virtual file system showing all the PRX files as compressed PRX's.
 * 
 * @author gid15
 *
 */
public class CompressPrxVirtualFileSystem extends AbstractProxyVirtualFileSystem {
	private static Map<String, Integer> prxModAttribute;
	private static Map<String, Boolean> prxKle4Compression;

	public CompressPrxVirtualFileSystem(IVirtualFileSystem vfs) {
		super(vfs);

		if (prxModAttribute == null) {
			initPrxMappings();
		}
	}

	private void addModAttribute(String prefix, String prx, int modAttribute) {
		prxModAttribute.put(prx, modAttribute);
		if (prefix != null) {
			prxModAttribute.put(prefix + prx, modAttribute);
		}
	}

	private void addKdModAttribute(String prx, int modAttribute) {
		addModAttribute("kd/", prx, modAttribute);
	}

	private void addVshModAttribute(String prx, int modAttribute) {
		addModAttribute("vsh/module/", prx, modAttribute);
	}

	private void initPrxMappings() {
		prxModAttribute = new HashMap<String, Integer>();

		addKdModAttribute("_galaxy.prx", 0x1006);
		addKdModAttribute("_inferno.prx", 0x1000);
		addKdModAttribute("_march33.prx", 0x0101);
		addKdModAttribute("_popcorn.prx", 0x1007);
		addKdModAttribute("_stargate.prx", 0x1007);
		addKdModAttribute("_systemctrl.prx", 0x3007);
		addKdModAttribute("_usbdevice.prx", 0x1006);
		addKdModAttribute("_vshctrl.prx", 0x1007);
		addKdModAttribute("amctrl.prx", 0x5007);
		addKdModAttribute("ata.prx", 0x1007);
		addKdModAttribute("audio.prx", 0x1007);
		addKdModAttribute("audiocodec_260.prx", 0x1006);
		addKdModAttribute("avcodec.prx", 0x1006);
		addKdModAttribute("cert_loader.prx", 0x1006);
		addKdModAttribute("chkreg.prx", 0x5006);
		addKdModAttribute("chnnlsv.prx", 0x5006);
		addKdModAttribute("clockgen.prx", 0x1007);
		addKdModAttribute("codec_01g.prx", 0x1007);
		addKdModAttribute("codepage.prx", 0x1007);
		addKdModAttribute("ctrl.prx", 0x1007);
		addKdModAttribute("display_01g.prx", 0x1007);
		addKdModAttribute("dmacman.prx", 0x1007);
		addKdModAttribute("exceptionman.prx", 0x1007);
		addKdModAttribute("fatms.prx", 0x1007);
		addKdModAttribute("g729.prx", 0x1006);
		addKdModAttribute("ge.prx", 0x1007);
		addKdModAttribute("hpremote_01g.prx", 0x1007);
		addKdModAttribute("http_storage.prx", 0x1006);
		addKdModAttribute("idstorage.prx", 0x1007);
		addKdModAttribute("ifhandle.prx", 0x1006);
		addKdModAttribute("impose_01g.prx", 0x1007);
		addKdModAttribute("init.prx", 0x1006);
		addKdModAttribute("interruptman.prx", 0x1007);
		addKdModAttribute("iofilemgr.prx", 0x1007);
		addKdModAttribute("iofilemgr_dnas.prx", 0x1007);
		addKdModAttribute("irda.prx", 0x1006);
		addKdModAttribute("isofs.prx", 0x1007);
		addKdModAttribute("led.prx", 0x1007);
		addKdModAttribute("lfatfs.prx", 0x1007);
		addKdModAttribute("lflash_fatfmt.prx", 0x1006);
		addKdModAttribute("libaac.prx", 0x0006);
		addKdModAttribute("libasfparser.prx", 0x0002);
		addKdModAttribute("libatrac3plus.prx", 0x0006);
		addKdModAttribute("libaudiocodec2.prx", 0x0006);
		addKdModAttribute("libdnas.prx", 0x0006);
		addKdModAttribute("libdnas_core.prx", 0x1006);
		addKdModAttribute("libgameupdate.prx", 0x0000);
		addKdModAttribute("libhttp.prx", 0x0006);
		addKdModAttribute("libmp3.prx", 0x0006);
		addKdModAttribute("libmp4.prx", 0x0006);
		addKdModAttribute("libparse_http.prx", 0x0006);
		addKdModAttribute("libparse_uri.prx", 0x0006);
		addKdModAttribute("libssl.prx", 0x0006);
		addKdModAttribute("libupdown.prx", 0x0006);
		addKdModAttribute("loadcore.prx", 0x3007);
		addKdModAttribute("loadexec_01g.prx", 0x3007);
		addKdModAttribute("lowio.prx", 0x1007);
		addKdModAttribute("mcctrl.prx", 0x5006);
		addKdModAttribute("me_wrapper.prx", 0x1007);
		addKdModAttribute("mediaman.prx", 0x1007);
		addKdModAttribute("mediasync.prx", 0x1007);
		addKdModAttribute("memab.prx", 0x1006);
		addKdModAttribute("memlmd_01g.prx", 0x7007);
		addKdModAttribute("mesg_led_01g.prx", 0x7007);
		addKdModAttribute("mgr.prx", 0x1006);
		addKdModAttribute("mgvideo.prx", 0x1006);
		addKdModAttribute("mlnbridge.prx", 0x1006);
		addKdModAttribute("mlnbridge_msapp.prx", 0x1006);
		addKdModAttribute("modulemgr.prx", 0x3007);
		addKdModAttribute("mp4msv.prx", 0x0000);
		addKdModAttribute("mpeg.prx", 0x0006);
		addKdModAttribute("mpeg_vsh.prx", 0x0006);
		addKdModAttribute("mpegbase_260.prx", 0x1006);
		addKdModAttribute("msaudio.prx", 0x1006);
		addKdModAttribute("msstor.prx", 0x1007);
		addKdModAttribute("np.prx", 0x0006);
		addKdModAttribute("np_auth.prx", 0x0006);
		addKdModAttribute("np_campaign.prx", 0x0006);
		addKdModAttribute("np_commerce2.prx", 0x0006);
		addKdModAttribute("np_commerce2_regcam.prx", 0x0006);
		addKdModAttribute("np_commerce2_store.prx", 0x0006);
		addKdModAttribute("np_core.prx", 0x1006);
		addKdModAttribute("np_inst.prx", 0x5006);
		addKdModAttribute("np_matching2.prx", 0x0006);
		addKdModAttribute("np_service.prx", 0x0006);
		addKdModAttribute("np9660.prx", 0x1007);
		addKdModAttribute("npdrm.prx", 0x5006);
		addKdModAttribute("openpsid.prx", 0x5007);
		addKdModAttribute("pops_01g.prx", 0x0000);
		addKdModAttribute("popsman.prx", 0x1007);
		addKdModAttribute("power_01g.prx", 0x1007);
		addKdModAttribute("psheet.prx", 0x1006);
		addKdModAttribute("pspnet.prx", 0x0006);
		addKdModAttribute("pspnet_adhoc.prx", 0x0006);
		addKdModAttribute("pspnet_adhoc_auth.prx", 0x1006);
		addKdModAttribute("pspnet_adhoc_discover.prx", 0x0006);
		addKdModAttribute("pspnet_adhoc_download.prx", 0x0006);
		addKdModAttribute("pspnet_adhoc_matching.prx", 0x0006);
		addKdModAttribute("pspnet_adhoc_transfer_int.prx", 0x0006);
		addKdModAttribute("pspnet_adhocctl.prx", 0x0006);
		addKdModAttribute("pspnet_apctl.prx", 0x0006);
		addKdModAttribute("pspnet_inet.prx", 0x0006);
		addKdModAttribute("pspnet_resolver.prx", 0x0006);
		addKdModAttribute("pspnet_upnp.prx", 0x0006);
		addKdModAttribute("pspnet_wispr.prx", 0x0006);
		addKdModAttribute("registry.prx", 0x1007);
		addKdModAttribute("rtc.prx", 0x1007);
		addKdModAttribute("sc_sascore.prx", 0x1006);
		addKdModAttribute("semawm.prx", 0x5006);
		addKdModAttribute("sircs.prx", 0x1006);
		addKdModAttribute("syscon.prx", 0x1007);
		addKdModAttribute("sysmem.prx", 0x1007);
		addKdModAttribute("systimer.prx", 0x1007);
		addKdModAttribute("threadman.prx", 0x1007);
		addKdModAttribute("umd9660.prx", 0x1007);
		addKdModAttribute("umdman.prx", 0x1007);
		addKdModAttribute("usb.prx", 0x1007);
		addKdModAttribute("usbacc.prx", 0x1006);
		addKdModAttribute("usbcam.prx", 0x1006);
		addKdModAttribute("usbgps.prx", 0x1006);
		addKdModAttribute("usbmic.prx", 0x1006);
		addKdModAttribute("usbpspcm.prx", 0x1006);
		addKdModAttribute("usbstor.prx", 0x1006);
		addKdModAttribute("usbstorboot.prx", 0x5006);
		addKdModAttribute("usbstormgr.prx", 0x1006);
		addKdModAttribute("usbstorms.prx", 0x1006);
		addKdModAttribute("usersystemlib.prx", 0x0007);
		addKdModAttribute("utility.prx", 0x1007);
		addKdModAttribute("vaudio.prx", 0x1006);
		addKdModAttribute("videocodec_260.prx", 0x1006);
		addKdModAttribute("vshbridge.prx", 0x1007);
		addKdModAttribute("vshbridge_msapp.prx", 0x1007);
		addKdModAttribute("wlan.prx", 0x1007);
		addKdModAttribute("wlanfirm_01g.prx", 0x1007);

		addVshModAttribute("_recovery.prx", 0x0101);
		addVshModAttribute("_satelite.prx", 0x0101);
		addVshModAttribute("adhoc_transfer.prx", 0x0000);
		addVshModAttribute("auth_plugin.prx", 0x0000);
		addVshModAttribute("auto_connect.prx", 0x0000);
		addVshModAttribute("camera_plugin.prx", 0x0000);
		addVshModAttribute("common_gui.prx", 0x0000);
		addVshModAttribute("common_util.prx", 0x0000);
		addVshModAttribute("content_browser.prx", 0x0000);
		addVshModAttribute("dd_helper.prx", 0x0000);
		addVshModAttribute("dd_helper_utility.prx", 0x0000);
		addVshModAttribute("dialogmain.prx", 0x0000);
		addVshModAttribute("dnas_plugin.prx", 0x0000);
		addVshModAttribute("file_parser_base.prx", 0x0000);
		addVshModAttribute("game_install_plugin.prx", 0x0000);
		addVshModAttribute("game_plugin.prx", 0x0000);
		addVshModAttribute("htmlviewer_plugin.prx", 0x0000);
		addVshModAttribute("htmlviewer_ui.prx", 0x0002);
		addVshModAttribute("htmlviewer_utility.prx", 0x0000);
		addVshModAttribute("hvauth_r.prx", 0x0000);
		addVshModAttribute("impose_plugin.prx", 0x0000);
		addVshModAttribute("launcher_plugin.prx", 0x0000);
		addVshModAttribute("lftv_main_plugin.prx", 0x0000);
		addVshModAttribute("lftv_middleware.prx", 0x0000);
		addVshModAttribute("lftv_plugin.prx", 0x0000);
		addVshModAttribute("libfont_arib.prx", 0x0006);
		addVshModAttribute("libfont_hv.prx", 0x0006);
		addVshModAttribute("libpspvmc.prx", 0x0000);
		addVshModAttribute("libslim.prx", 0x0002);
		addVshModAttribute("libwww.prx", 0x0002);
		addVshModAttribute("marlindownloader.prx", 0x0000);
		addVshModAttribute("mcore.prx", 0x0000);
		addVshModAttribute("mlnapp_proxy.prx", 0x0000);
		addVshModAttribute("mlnbb.prx", 0x0000);
		addVshModAttribute("mlncmn.prx", 0x0000);
		addVshModAttribute("mlnusb.prx", 0x0000);
		addVshModAttribute("mm_flash.prx", 0x0002);
		addVshModAttribute("msgdialog_plugin.prx", 0x0000);
		addVshModAttribute("msvideo_main_plugin.prx", 0x0000);
		addVshModAttribute("msvideo_plugin.prx", 0x0000);
		addVshModAttribute("music_browser.prx", 0x0000);
		addVshModAttribute("music_main_plugin.prx", 0x0000);
		addVshModAttribute("music_parser.prx", 0x0000);
		addVshModAttribute("music_player.prx", 0x0000);
		addVshModAttribute("netconf_plugin.prx", 0x0000);
		addVshModAttribute("netconf_plugin_auto_bfl.prx", 0x0000);
		addVshModAttribute("netconf_plugin_auto_nec.prx", 0x0000);
		addVshModAttribute("netfront.prx", 0x0002);
		addVshModAttribute("netplay_client_plugin.prx", 0x0000);
		addVshModAttribute("netplay_server_plus_utility.prx", 0x0000);
		addVshModAttribute("netplay_server_utility.prx", 0x0000);
		addVshModAttribute("netplay_server2_utility.prx", 0x0000);
		addVshModAttribute("npadmin_plugin.prx", 0x0000);
		addVshModAttribute("npinstaller_plugin.prx", 0x0000);
		addVshModAttribute("npsignin_plugin.prx", 0x0000);
		addVshModAttribute("npsignup_plugin.prx", 0x0000);
		addVshModAttribute("opening_plugin.prx", 0x0000);
		addVshModAttribute("osk_plugin.prx", 0x0000);
		addVshModAttribute("paf.prx", 0x0000);
		addVshModAttribute("pafmini.prx", 0x0000);
		addVshModAttribute("photo_browser.prx", 0x0000);
		addVshModAttribute("photo_main_plugin.prx", 0x0000);
		addVshModAttribute("photo_player.prx", 0x0000);
		addVshModAttribute("premo_plugin.prx", 0x0000);
		addVshModAttribute("ps3scan_plugin.prx", 0x0000);
		addVshModAttribute("psn_plugin.prx", 0x0000);
		addVshModAttribute("psn_utility.prx", 0x0000);
		addVshModAttribute("radioshack_plugin.prx", 0x0000);
		addVshModAttribute("recommend_browser.prx", 0x0000);
		addVshModAttribute("recommend_launcher_plugin.prx", 0x0000);
		addVshModAttribute("recommend_main.prx", 0x0000);
		addVshModAttribute("rss_browser.prx", 0x0000);
		addVshModAttribute("rss_common.prx", 0x0000);
		addVshModAttribute("rss_downloader.prx", 0x0000);
		addVshModAttribute("rss_main_plugin.prx", 0x0000);
		addVshModAttribute("rss_reader.prx", 0x0000);
		addVshModAttribute("rss_subscriber.prx", 0x0000);
		addVshModAttribute("savedata_auto_dialog.prx", 0x0000);
		addVshModAttribute("savedata_plugin.prx", 0x0000);
		addVshModAttribute("savedata_utility.prx", 0x0000);
		addVshModAttribute("screenshot_plugin.prx", 0x0000);
		addVshModAttribute("store_browser_plugin.prx", 0x0000);
		addVshModAttribute("store_checkout_plugin.prx", 0x0000);
		addVshModAttribute("store_checkout_utility.prx", 0x0000);
		addVshModAttribute("subs_plugin.prx", 0x0000);
		addVshModAttribute("sysconf_plugin.prx", 0x0000);
		addVshModAttribute("update_plugin.prx", 0x0000);
		addVshModAttribute("video_main_plugin.prx", 0x0000);
		addVshModAttribute("video_plugin.prx", 0x0000);
		addVshModAttribute("visualizer_plugin.prx", 0x0000);
		addVshModAttribute("vshmain.prx", 0x0800);

		prxKle4Compression = new HashMap<String, Boolean>();
		prxKle4Compression.put("loadcore.prx", Boolean.TRUE);
		prxKle4Compression.put("kd/loadcore.prx", Boolean.TRUE);
		prxKle4Compression.put("sysmem.prx", Boolean.TRUE);
		prxKle4Compression.put("kd/sysmem.prx", Boolean.TRUE);
	}

	private boolean isPrx(String fileName) {
		return fileName.toLowerCase().endsWith(".prx");
	}

	private boolean isKl4eCompression(String fileName) {
		Boolean kle4Compression = prxKle4Compression.get(fileName.toLowerCase());
		if (kle4Compression != null) {
			return kle4Compression.booleanValue();
		}
		return false;
	}

	private int getModAttribute(String fileName) {
		Integer modAttribute = prxModAttribute.get(fileName.toLowerCase());
		if (modAttribute != null) {
			return modAttribute.intValue();
		}
		return 0x1007; // SCE_MODULE_KERNEL
	}

	private static void write8(OutputStream os, int value) throws IOException {
		os.write(value & 0xFF);
	}

	private static void write16(OutputStream os, int value) throws IOException {
		write8(os, value);
		write8(os, value >> 8);
	}

	private static void write32(OutputStream os, int value) throws IOException {
		write8(os, value);
		write8(os, value >> 8);
		write8(os, value >> 16);
		write8(os, value >> 24);
	}

	private static void writeString(OutputStream os, String value, int length) throws IOException {
		if (value != null) {
			int n = Math.min(value.length(), length);
			for (int i = 0; i < n; i++) {
				char c = value.charAt(i);
				write8(os, c);
			}
			length -= n;
		}

		while (length > 0) {
			length--;
			write8(os, 0);
		}
	}

	private static void writeBytes(OutputStream os, byte[] bytes, int length) throws IOException {
		if (bytes != null) {
			int n = Math.min(bytes.length, length);
			for (int i = 0; i < n; i++) {
				write8(os, bytes[i]);
			}
			length -= n;
		}

		while (length > 0) {
			length--;
			write8(os, 0);
		}
	}

	private void writePspHeader(OutputStream os, byte[] elfFile, String fileName) throws IOException {
		int bssSize = 0;
		int modInfoOffset = -1;
		int nSegments = 0;
		int type = readUnaligned16(elfFile, 16);
		int bootEntry = readUnaligned32(elfFile, 24);
		int phOffset = readUnaligned32(elfFile, 28);
		int phEntSize = readUnaligned16(elfFile, 42);
		int phNum = readUnaligned16(elfFile, 44);
		int[] segAlign = new int[SCE_KERNEL_MAX_MODULE_SEGMENT];
		int[] segAddress = new int[SCE_KERNEL_MAX_MODULE_SEGMENT];
		int[] segSize = new int[SCE_KERNEL_MAX_MODULE_SEGMENT];

		// Scan all the ELF program headers
		for (int i = 0; i < phNum; i++) {
			int offset = phOffset + i * phEntSize;
			int phType = readUnaligned32(elfFile, offset + 0);

			// Compute the BSS size
			int phEntFileSize = readUnaligned32(elfFile, offset + 16);
			int phEntMemSize = readUnaligned32(elfFile, offset + 20);
			if (phEntMemSize > phEntFileSize) {
				bssSize += phEntMemSize - phEntFileSize;
			}

			if (phType == 1 && nSegments < SCE_KERNEL_MAX_MODULE_SEGMENT) {
				segAlign[nSegments] = readUnaligned32(elfFile, offset + 28);
				segAddress[nSegments] = readUnaligned32(elfFile, offset + 8);
				segSize[nSegments] = phEntMemSize;

				if (type == Elf32Header.ET_SCE_PRX && nSegments == 0) {
					modInfoOffset = readUnaligned32(elfFile, offset + 12);
				}

				nSegments++;
			}
		}

		int compAttribute = 0x001; // SCE_EXEC_FILE_COMPRESSED
		boolean isKl4eCompressed = isKl4eCompression(fileName);
		if (isKl4eCompressed) {
			compAttribute |= 0x200; // SCE_EXEC_FILE_KL4E_COMPRESSED
		}

		write32(os, PSP_MAGIC); // Offset 0
		write16(os, getModAttribute(fileName)); // Offset 4, modAttribute
		write16(os, compAttribute); // Offset 6, compAttribute
		write8(os, 0); // Offset 8, moduleVerLo
		write8(os, 0); // Offset 9, moduleVerHi
		writeString(os, fileName, 28); // Offset 10, modName
		write8(os, 0); // Offset 38, modVersion
		write8(os, nSegments); // Offset 39, nSegments
		write32(os, elfFile.length); // Offset 40, elfSize
		write32(os, 0); // Offset 44, pspSize
		write32(os, bootEntry); // Offset 48, bootEntry
		write32(os, modInfoOffset); // Offset 52, modInfoOffset (must be < 0)
		write32(os, bssSize); // Offset 56, bssSize
		for (int i = 0; i < SCE_KERNEL_MAX_MODULE_SEGMENT; i++) {
			write16(os, segAlign[i]); // Offset 60-66, segAlign
		}
		for (int i = 0; i < SCE_KERNEL_MAX_MODULE_SEGMENT; i++) {
			write32(os, segAddress[i]); // Offset 68-80, segAddress
		}
		for (int i = 0; i < SCE_KERNEL_MAX_MODULE_SEGMENT; i++) {
			write32(os, segSize[i]); // Offset 84-96, segSize
		}
		for (int i = 0; i < 5; i++) {
			write32(os, 0); // Offset 100-116, reserved
		}
		write32(os, 0); // Offset 120, devkitVersion
		write8(os, 0); // Offset 124, decryptMode (DECRYPT_MODE_KERNEL_MODULE)
		write8(os, 0); // Offset 125, padding
		write16(os, 0); // Offset 126, overlapSize

		// Non-encrypted but compressed files having a short 128 bytes PSP header.
		// Excepted for sysmem.prx and loadcore.prx which are assumed to always be
		// KL4E compressed and encrypted. In those cases, the full 336 bytes
		// PSP header is present.
		if (isKl4eCompressed) {
			writeBytes(os, null, AES_KEY_SIZE); // Offset 128-143, aeskey
			writeBytes(os, null, CMAC_KEY_SIZE); // Offset 144-159, cmacKey
			writeBytes(os, null, CMAC_HEADER_HASH_SIZE); // Offset 160-175, cmacHeaderHash
			write32(os, 0); // Offset 176, compSize
			write32(os, 0); // Offset 180, unk180
			write32(os, 0); // Offset 184, unk184
			write32(os, 0); // Offset 188, unk188
			writeBytes(os, null, CMAC_DATA_HASH_SIZE); // Offset 192-207, cmacDataHash
			write32(os, 0); // Offset 208, tag
			writeBytes(os, null, CHECK_SIZE); // Offset 212-299, sCheck
			writeBytes(os, null, SHA1_HASH_SIZE); // Offset 300-319, sha1Hash
			writeBytes(os, null, KEY_DATA_SIZE); // Offset 320-335, keyData
		}
	}

	private void fixPspSizeInHeader(byte[] bytes) {
		int pspSize = bytes.length;
		Utilities.writeUnaligned32(bytes, 44, pspSize);
	}

	private byte[] getCompressedPrxFile(String dirName, String fileName) {
		String proxyFileName;
		if (dirName == null || dirName.length() == 0) {
			proxyFileName = fileName;
		} else {
			proxyFileName = dirName + "/" + fileName;
		}

		IVirtualFile vFileUncompressed = super.ioOpen(proxyFileName, PSP_O_RDONLY, 0);
		if (vFileUncompressed == null) {
			return null;
		}
		byte[] bufferUncompressed = Utilities.readCompleteFile(vFileUncompressed);
		vFileUncompressed.ioClose();
		if (bufferUncompressed == null) {
			return null;
		}

		int headerMagic = Utilities.readUnaligned32(bufferUncompressed, 0);
		if (headerMagic != Elf32Header.ELF_MAGIC) {
			return bufferUncompressed;
		}

		int lengthUncompressed = bufferUncompressed.length;

		ByteArrayOutputStream osCompressed = new ByteArrayOutputStream(PSP_HEADER_SIZE + 9 + lengthUncompressed);
		try {
			writePspHeader(osCompressed, bufferUncompressed, fileName);
			// loadcore.prx and sysmem.prx need to be compressed using KL4E.
			// KL4E supports a version where the data is not compressed.
			// Use this simple version as we have no real KL4E compressor.
			if (isKl4eCompression(fileName)) {
				writeString(osCompressed, "KL4E", 4);
				write8(osCompressed, 0x80); // Flag indicating that the rest of the data is uncompressed
				write32(osCompressed, endianSwap32(lengthUncompressed));
				writeBytes(osCompressed, bufferUncompressed, lengthUncompressed);
			} else {
				GZIPOutputStream os = new GZIPOutputStream(osCompressed);
				os.write(bufferUncompressed, 0, lengthUncompressed);
				os.close();
			}
		} catch (IOException e) {
		}

		byte[] bytes = osCompressed.toByteArray();
		fixPspSizeInHeader(bytes);

		return bytes;
	}

	private long getCompressedPrxSize(String dirName, String fileName) {
		byte[] compressedFile = getCompressedPrxFile(dirName, fileName);
		if (compressedFile == null) {
			return 0L;
		}
		return (long) compressedFile.length;
	}

	@Override
	public int ioDread(String dirName, SceIoDirent dir) {
		int result = super.ioDread(dirName, dir);
		if (isPrx(dir.filename)) {
			dir.stat.size = getCompressedPrxSize(dirName, dir.filename);
		}

		return result;
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode) {
		if (isPrx(fileName) && (flags & PSP_O_RDWR) == PSP_O_RDONLY) {
			return new ByteArrayVirtualFile(getCompressedPrxFile(null, fileName));
		}

		return super.ioOpen(fileName, flags, mode);
	}
}
