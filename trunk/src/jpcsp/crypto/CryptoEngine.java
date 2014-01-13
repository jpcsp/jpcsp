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
package jpcsp.crypto;

import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.Settings;

@SuppressWarnings("unused")
public class CryptoEngine {

    private static final String name = "CryptEngine";
    private static boolean isCryptoEngineInit;
    private static boolean cryptoSavedata;
    private static boolean extractEboot;
    private static boolean extractSavedataKey;
    private static KIRK kirk;
    private static PRX prx;
    private static SAVEDATA sd;
    private static AMCTRL amctrl;
    private static PGD pgd;
    private static DRM drm;
    private static CryptSavedataSettingsListerner cryptSavedataSettingsListerner;
    private static ExtractEbootSettingsListerner extractEbootSettingsListerner;
    private static ExtractSavedataKeySettingsListerner extractSavedataKeySettingsListerner;
    
    private static class CryptSavedataSettingsListerner extends AbstractBoolSettingsListener {

        @Override
        protected void settingsValueChanged(boolean value) {
            setSavedataCryptoStatus(!value);
        }
    }
    
    private static class ExtractEbootSettingsListerner extends AbstractBoolSettingsListener {

        @Override
        protected void settingsValueChanged(boolean value) {
            setExtractEbootStatus(value);
        }
    }
    
    private static class ExtractSavedataKeySettingsListerner extends AbstractBoolSettingsListener {

        @Override
        protected void settingsValueChanged(boolean value) {
            setExtractSavedataKeyStatus(value);
        }
    }

    public CryptoEngine() {
        installSettingsListeners();
        setCryptoEngineStatus(true);
        kirk = new KIRK();
        prx = new PRX();
        sd = new SAVEDATA();
        amctrl = new AMCTRL();
        pgd = new PGD();
        drm = new DRM();
    }
    
    public KIRK getKIRKEngine() {
        return kirk;
    }
    
    public PRX getPRXEngine() {
        return prx;
    }
    
    public SAVEDATA getSAVEDATAEngine() {
        return sd;
    }
    
    public AMCTRL getAMCTRLEngine() {
        return amctrl;
    }
    
    public PGD getPGDEngine() {
        return pgd;
    }
    
    public DRM getDRMEngine() {
        return drm;
    }

    private static void installSettingsListeners() {
        if (cryptSavedataSettingsListerner == null) {
            cryptSavedataSettingsListerner = new CryptSavedataSettingsListerner();
            Settings.getInstance().registerSettingsListener(name, "emu.cryptoSavedata", cryptSavedataSettingsListerner);
        }
        if (extractEbootSettingsListerner == null) {
            extractEbootSettingsListerner = new ExtractEbootSettingsListerner();
            Settings.getInstance().registerSettingsListener(name, "emu.extractEboot", extractEbootSettingsListerner);
        }
        if (extractSavedataKeySettingsListerner == null) {
            extractSavedataKeySettingsListerner = new ExtractSavedataKeySettingsListerner();
            Settings.getInstance().registerSettingsListener(name, "emu.extractSavedataKey", extractSavedataKeySettingsListerner);
        }
    }

    /*
     * Helper functions: used for status checking and parameter sorting.
     */
    public static boolean getCryptoEngineStatus() {
        return isCryptoEngineInit;
    }

    private static void setCryptoEngineStatus(boolean status) {
        isCryptoEngineInit = status;
    }
    
    public static boolean getExtractEbootStatus() {
        installSettingsListeners();
        return extractEboot;
    }

    private static void setExtractEbootStatus(boolean status) {
        extractEboot = status;
    }
    
    public static boolean getExtractSavedataKeyStatus() {
        installSettingsListeners();
        return extractSavedataKey;
    }

    private static void setExtractSavedataKeyStatus(boolean status) {
        extractSavedataKey = status;
    }

    public static boolean getSavedataCryptoStatus() {
        installSettingsListeners();
        return cryptoSavedata;
    }

    private static void setSavedataCryptoStatus(boolean status) {
        cryptoSavedata = status;
    }
}