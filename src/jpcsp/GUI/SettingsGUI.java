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
package jpcsp.GUI;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.MutableComboBoxModel;

import jpcsp.Emulator;
import jpcsp.MainGUI;
import jpcsp.HLE.modules.sceUtility;
import jpcsp.settings.Settings;

import com.jidesoft.swing.FolderChooser;

/**
 *
 * @author shadow
 */
public class SettingsGUI extends javax.swing.JFrame {

    private static final long serialVersionUID = -732715495873159718L;
    private Settings settings;

    /**
     * Creates new form SettingsGUI
     */
    public SettingsGUI() {
        settings = Settings.getInstance();

        initComponents();

        setAllComponentsFromSettings();
    }

    private void setAllComponentsFromSettings() {
        setBoolFromSettings(pbpunpackcheck, "emu.pbpunpack");
        setBoolFromSettings(saveWindowPosCheck, "gui.saveWindowPos");
        setBoolFromSettings(fullscreenCheck, "gui.fullscreen");
        setBoolFromSettings(useCompiler, "emu.compiler");
        setBoolFromSettings(profilerCheck, "emu.profiler");
        setBoolFromSettings(shadersCheck, "emu.useshaders");
        setBoolFromSettings(geometryShaderCheck, "emu.useGeometryShader");
        setBoolFromSettings(filelogCheck, "emu.debug.enablefilelogger");
        setBoolFromSettings(loadAndRunCheck, "emu.loadAndRun");
        setIntFromSettings(languageBox, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_LANGUAGE);
        setIntFromSettings(buttonBox, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_BUTTON_PREFERENCE);
        setIntFromSettings(daylightBox, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_DAYLIGHT_SAVING_TIME);
        setIntFromSettings(timezoneSpinner, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_TIME_ZONE);
        setIntFromSettings(timeFormatBox, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_TIME_FORMAT);
        setIntFromSettings(dateFormatBox, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_DATE_FORMAT);
        setIntFromSettings(wlanPowerBox, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_WLAN_POWER_SAVE);
        setIntFromSettings(adhocChannelBox, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_ADHOC_CHANNEL);
        setStringFromSettings(nicknameTextField, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_NICKNAME);
        setBoolFromSettings(disableVBOCheck, "emu.disablevbo");
        setBoolFromSettings(disableUBOCheck, "emu.disableubo");
        setBoolFromSettings(enableVAOCheck, "emu.enablevao");
        setBoolFromSettings(enableGETextureCheck, "emu.enablegetexture");
        setBoolFromSettings(enableNativeCLUTCheck, "emu.enablenativeclut");
        setBoolFromSettings(enableDynamicShadersCheck, "emu.enabledynamicshaders");
        setBoolFromSettings(enableShaderStencilTestCheck, "emu.enableshaderstenciltest");
        setBoolFromSettings(enableShaderColorMaskCheck, "emu.enableshadercolormask");
        setBoolFromSettings(disableOptimizedVertexInfoReading, "emu.disableoptimizedvertexinforeading");
        setBoolFromSettings(saveStencilToMemory, "emu.saveStencilToMemory");
        setBoolFromSettings(useSoftwareRenderer, "emu.useSoftwareRenderer");
        setBoolFromSettings(onlyGEGraphicsCheck, "emu.onlyGEGraphics");
        setBoolFromSettings(useConnector, "emu.useConnector");
        setBoolFromSettings(useDebugFont, "emu.useDebugFont");
        setBoolFromSettings(useDebugMemory, "emu.useDebuggerMemory");
        setBoolFromSettings(useExternalDecoder, "emu.useExternalDecoder");
        setBoolFromSettings(useMediaEngine, "emu.useMediaEngine");
        setBoolFromSettings(useVertexCache, "emu.useVertexCache");
        setBoolFromSettings(invalidMemoryCheck, "emu.ignoreInvalidMemoryAccess");
        setBoolFromSettings(DisableSceAudioCheck, "emu.disablesceAudio");
        setBoolFromSettings(IgnoreAudioThreadsCheck, "emu.ignoreaudiothreads");
        setBoolFromSettings(disableBlockingAudioCheck, "emu.disableblockingaudio");
        setBoolFromSettings(ignoreUnmappedImports, "emu.ignoreUnmappedImports");
        setIntAsStringFromSettings(methodMaxInstructionsBox, "emu.compiler.methodMaxInstructions", 3000);
        setBoolFromSettings(extractEboot, "emu.extractEboot");
        setBoolFromSettings(cryptoSavedata, "emu.cryptoSavedata");
        setBoolFromSettings(extractPGD, "emu.extractPGD");
        setStringFromSettings(antiAliasingBox, "emu.graphics.antialias");
        setStringFromSettings(resolutionBox, "emu.graphics.resolution");
        setStringFromSettings(umdpath, "emu.umdpath");
        setStringFromSettings(tmppath, "emu.tmppath");
        setBoolFromSettings(umdBrowser, classicUmdDialog, "emu.umdbrowser");
    }

    private boolean isEnabledSettings(String settingsOption) {
        return !settings.isOptionFromPatch(settingsOption);
    }

    private void setBoolFromSettings(JRadioButton trueButton, JRadioButton falseButton, String settingsOption) {
        boolean value = settings.readBool(settingsOption);
        trueButton.setSelected(value);
        falseButton.setSelected(!value);
        trueButton.setEnabled(isEnabledSettings(settingsOption));
        falseButton.setEnabled(isEnabledSettings(settingsOption));
    }

    private void setBoolFromSettings(JCheckBox checkBox, String settingsOption) {
        checkBox.setSelected(settings.readBool(settingsOption));
        checkBox.setEnabled(isEnabledSettings(settingsOption));
    }

    private void setIntFromSettings(JComboBox comboBox, String settingsOption) {
        comboBox.setSelectedIndex(settings.readInt(settingsOption));
        comboBox.setEnabled(isEnabledSettings(settingsOption));
    }

    private void setIntAsStringFromSettings(JComboBox comboBox, String settingsOption, int defaultValue) {
        comboBox.setSelectedItem(Integer.toString(settings.readInt(settingsOption, defaultValue)));
        comboBox.setEnabled(isEnabledSettings(settingsOption));
    }

    private void setIntFromSettings(JSpinner spinner, String settingsOption) {
        spinner.setValue(settings.readInt(settingsOption));
        spinner.setEnabled(isEnabledSettings(settingsOption));
    }

    private void setStringFromSettings(JComboBox comboBox, String settingsOption) {
        comboBox.setSelectedItem(settings.readString(settingsOption));
        comboBox.setEnabled(isEnabledSettings(settingsOption));
    }

    private void setStringFromSettings(JTextField textField, String settingsOption) {
        textField.setText(settings.readString(settingsOption));
        textField.setEnabled(isEnabledSettings(settingsOption));
    }

    private void setAllComponentsToSettings() {
        setBoolToSettings(pbpunpackcheck, "emu.pbpunpack");
        setBoolToSettings(saveWindowPosCheck, "gui.saveWindowPos");
        setBoolToSettings(fullscreenCheck, "gui.fullscreen");
        setBoolToSettings(useCompiler, "emu.compiler");
        setBoolToSettings(profilerCheck, "emu.profiler");
        setBoolToSettings(shadersCheck, "emu.useshaders");
        setBoolToSettings(geometryShaderCheck, "emu.useGeometryShader");
        setBoolToSettings(filelogCheck, "emu.debug.enablefilelogger");
        setBoolToSettings(loadAndRunCheck, "emu.loadAndRun");
        setIntToSettings(languageBox, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_LANGUAGE);
        setIntToSettings(buttonBox, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_BUTTON_PREFERENCE);
        setIntToSettings(daylightBox, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_DAYLIGHT_SAVING_TIME);
        setIntToSettings(timezoneSpinner, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_TIME_ZONE);
        setIntToSettings(timeFormatBox, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_TIME_FORMAT);
        setIntToSettings(dateFormatBox, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_DATE_FORMAT);
        setIntToSettings(wlanPowerBox, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_WLAN_POWER_SAVE);
        setIntToSettings(adhocChannelBox, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_ADHOC_CHANNEL);
        setStringToSettings(nicknameTextField, sceUtility.SYSTEMPARAM_SETTINGS_OPTION_NICKNAME);
        setBoolToSettings(disableVBOCheck, "emu.disablevbo");
        setBoolToSettings(disableUBOCheck, "emu.disableubo");
        setBoolToSettings(enableVAOCheck, "emu.enablevao");
        setBoolToSettings(enableGETextureCheck, "emu.enablegetexture");
        setBoolToSettings(enableNativeCLUTCheck, "emu.enablenativeclut");
        setBoolToSettings(enableDynamicShadersCheck, "emu.enabledynamicshaders");
        setBoolToSettings(enableShaderStencilTestCheck, "emu.enableshaderstenciltest");
        setBoolToSettings(enableShaderColorMaskCheck, "emu.enableshadercolormask");
        setBoolToSettings(disableOptimizedVertexInfoReading, "emu.disableoptimizedvertexinforeading");
        setBoolToSettings(saveStencilToMemory, "emu.saveStencilToMemory");
        setBoolToSettings(useSoftwareRenderer, "emu.useSoftwareRenderer");
        setBoolToSettings(onlyGEGraphicsCheck, "emu.onlyGEGraphics");
        setBoolToSettings(useConnector, "emu.useConnector");
        setBoolToSettings(useDebugFont, "emu.useDebugFont");
        setBoolToSettings(useDebugMemory, "emu.useDebuggerMemory");
        setBoolToSettings(useExternalDecoder, "emu.useExternalDecoder");
        setBoolToSettings(useMediaEngine, "emu.useMediaEngine");
        setBoolToSettings(useVertexCache, "emu.useVertexCache");
        setBoolToSettings(invalidMemoryCheck, "emu.ignoreInvalidMemoryAccess");
        setBoolToSettings(DisableSceAudioCheck, "emu.disablesceAudio");
        setBoolToSettings(IgnoreAudioThreadsCheck, "emu.ignoreaudiothreads");
        setBoolToSettings(disableBlockingAudioCheck, "emu.disableblockingaudio");
        setBoolToSettings(ignoreUnmappedImports, "emu.ignoreUnmappedImports");
        setIntAsStringToSettings(methodMaxInstructionsBox, "emu.compiler.methodMaxInstructions", 3000);
        setBoolToSettings(extractEboot, "emu.extractEboot");
        setBoolToSettings(cryptoSavedata, "emu.cryptoSavedata");
        setBoolToSettings(extractPGD, "emu.extractPGD");
        setStringToSettings(antiAliasingBox, "emu.graphics.antialias");
        setStringToSettings(resolutionBox, "emu.graphics.resolution");
        setStringToSettings(umdpath, "emu.umdpath");
        setStringToSettings(tmppath, "emu.tmppath");
        setBoolToSettings(umdBrowser, "emu.umdbrowser");
    }

    private void setBoolToSettings(JRadioButton radioButton, String settingsOption) {
        if (isEnabledSettings(settingsOption)) {
            settings.writeBool(settingsOption, radioButton.isSelected());
        }
    }

    private void setBoolToSettings(JCheckBox checkBox, String settingsOption) {
        if (isEnabledSettings(settingsOption)) {
            settings.writeBool(settingsOption, checkBox.isSelected());
        }
    }

    private void setIntToSettings(JComboBox comboBox, String settingsOption) {
        if (isEnabledSettings(settingsOption)) {
            settings.writeInt(settingsOption, comboBox.getSelectedIndex());
        }
    }

    private void setIntAsStringToSettings(JComboBox comboBox, String settingsOption, int defaultValue) {
        if (isEnabledSettings(settingsOption)) {
            settings.writeInt(settingsOption, Integer.parseInt(comboBox.getSelectedItem().toString()));
        }
    }

    private void setIntToSettings(JSpinner spinner, String settingsOption) {
        if (isEnabledSettings(settingsOption)) {
            settings.writeInt(settingsOption, Integer.parseInt(spinner.getValue().toString()));
        }
    }

    private void setStringToSettings(JComboBox comboBox, String settingsOption) {
        if (isEnabledSettings(settingsOption)) {
            settings.writeString(settingsOption, comboBox.getSelectedItem().toString());
        }
    }

    private void setStringToSettings(JTextField textField, String settingsOption) {
        if (isEnabledSettings(settingsOption)) {
            settings.writeString(settingsOption, textField.getText());
        }
    }

    private ComboBoxModel makeComboBoxModel(String[] items) {
        MutableComboBoxModel comboBox = new DefaultComboBoxModel();
        for (String item : items) {
            comboBox.addElement(item);
        }
        return comboBox;
    }

    public static String[] getImposeLanguages() {
        return new String[]{
            "Japanese",
            "English",
            "French",
            "Spanish",
            "German",
            "Italian",
            "Dutch",
            "Portuguese",
            "Russian",
            "Korean",
            "Traditional Chinese",
            "Simplfied Chinese"
        };
    }

    public static String[] getImposeButtons() {
        return new String[]{
            "\"O\" for \"Enter\"",
            "\"X\" for \"Enter\""
        };
    }

    public static String[] getSysparamDaylightSavings() {
        return new String[]{
            "Off",
            "On"
        };
    }

    public static String[] getSysparamTimeFormats() {
        return new String[]{
            "24H",
            "12H"
        };
    }

    public static String[] getSysparamDateFormats() {
        return new String[]{
            "YYYY-MM-DD",
            "MM-DD-YYYY",
            "DD-MM-YYYY"
        };
    }

    public static String[] getSysparamWlanPowerSaves() {
        return new String[]{
            "Off",
            "On"
        };
    }

    public static String[] getSysparamAdhocChannels() {
        return new String[]{
            "Auto",
            "1",
            "6",
            "11"
        };
    }

    private ComboBoxModel makeResolutions() {
        MutableComboBoxModel comboBox = new DefaultComboBoxModel();
        comboBox.addElement("Native");

        Set<String> resolutions = new HashSet<String>();
        GraphicsDevice localDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode[] displayModes = localDevice.getDisplayModes();
        for (int i = 0; displayModes != null && i < displayModes.length; i++) {
            DisplayMode displayMode = displayModes[i];
            if (displayMode.getBitDepth() == MainGUI.displayModeBitDepth) {
                String resolution = String.format("%dx%d", displayMode.getWidth(), displayMode.getHeight());
                if (!resolutions.contains(resolution)) {
                    comboBox.addElement(resolution);
                    resolutions.add(resolution);
                }
            }
        }

        return comboBox;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jButtonOK = new javax.swing.JButton();
        jButtonApply = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        GeneralPanel = new javax.swing.JPanel();
        pbpunpackcheck = new javax.swing.JCheckBox();
        saveWindowPosCheck = new javax.swing.JCheckBox();
        filelogCheck = new javax.swing.JCheckBox();
        loadAndRunCheck = new javax.swing.JCheckBox();
        umdBrowser = new javax.swing.JRadioButton();
        classicUmdDialog = new javax.swing.JRadioButton();
        umdPathLabel = new javax.swing.JLabel();
        umdpath = new javax.swing.JTextField();
        umdPathBrowseButton = new javax.swing.JButton();
        tmpPathLabel = new javax.swing.JLabel();
        tmppath = new javax.swing.JTextField();
        tmpPathBrowseButton = new javax.swing.JButton();
        RegionPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        imposeLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        languageLabel = new javax.swing.JLabel();
        languageBox = new javax.swing.JComboBox();
        buttonLabel = new javax.swing.JLabel();
        buttonBox = new javax.swing.JComboBox();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        sysParmLabel = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        daylightLabel = new javax.swing.JLabel();
        daylightBox = new javax.swing.JComboBox();
        timeFormatLabel = new javax.swing.JLabel();
        timeFormatBox = new javax.swing.JComboBox();
        dateFormatLabel = new javax.swing.JLabel();
        dateFormatBox = new javax.swing.JComboBox();
        wlanPowerLabel = new javax.swing.JLabel();
        wlanPowerBox = new javax.swing.JComboBox();
        adhocChannelLabel = new javax.swing.JLabel();
        adhocChannelBox = new javax.swing.JComboBox();
        timezoneLabel = new javax.swing.JLabel();
        timezoneSpinner = new javax.swing.JSpinner();
        nicknamelLabel = new javax.swing.JLabel();
        nicknameTextField = new javax.swing.JTextField();
        VideoPanel = new javax.swing.JPanel();
        disableVBOCheck = new javax.swing.JCheckBox();
        onlyGEGraphicsCheck = new javax.swing.JCheckBox();
        useVertexCache = new javax.swing.JCheckBox();
        shadersCheck = new javax.swing.JCheckBox();
        geometryShaderCheck = new javax.swing.JCheckBox();
        disableUBOCheck = new javax.swing.JCheckBox();
        enableVAOCheck = new javax.swing.JCheckBox();
        enableGETextureCheck = new javax.swing.JCheckBox();
        enableNativeCLUTCheck = new javax.swing.JCheckBox();
        enableDynamicShadersCheck = new javax.swing.JCheckBox();
        enableShaderStencilTestCheck = new javax.swing.JCheckBox();
        enableShaderColorMaskCheck = new javax.swing.JCheckBox();
        disableOptimizedVertexInfoReading = new javax.swing.JCheckBox();
        saveStencilToMemory = new javax.swing.JCheckBox();
        useSoftwareRenderer = new javax.swing.JCheckBox();
        AudioPanel = new javax.swing.JPanel();
        IgnoreAudioThreadsCheck = new javax.swing.JCheckBox();
        disableBlockingAudioCheck = new javax.swing.JCheckBox();
        DisableSceAudioCheck = new javax.swing.JCheckBox();
        MemoryPanel = new javax.swing.JPanel();
        invalidMemoryCheck = new javax.swing.JCheckBox();
        ignoreUnmappedImports = new javax.swing.JCheckBox();
        MiscPanel = new javax.swing.JPanel();
        useMediaEngine = new javax.swing.JCheckBox();
        useConnector = new javax.swing.JCheckBox();
        useExternalDecoder = new javax.swing.JCheckBox();
        useDebugFont = new javax.swing.JCheckBox();
        useDebugMemory = new javax.swing.JCheckBox();
        CompilerPanel = new javax.swing.JPanel();
        useCompiler = new javax.swing.JCheckBox();
        methodMaxInstructionsBox = new javax.swing.JComboBox();
        profilerCheck = new javax.swing.JCheckBox();
        methodMaxInstructionsLabel = new javax.swing.JLabel();
        CryptoPanel = new javax.swing.JPanel();
        extractEboot = new javax.swing.JCheckBox();
        cryptoSavedata = new javax.swing.JCheckBox();
        extractPGD = new javax.swing.JCheckBox();
        DisplayPanel = new javax.swing.JPanel();
        antiAliasLabel = new javax.swing.JLabel();
        antiAliasingBox = new javax.swing.JComboBox();
        resolutionLabel = new javax.swing.JLabel();
        resolutionBox = new javax.swing.JComboBox();
        fullscreenCheck = new javax.swing.JCheckBox();
        cancelButton = new jpcsp.GUI.CancelButton();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
        setTitle(bundle.getString("SettingsGUI.title")); // NOI18N

        jButtonOK.setText(bundle.getString("OkButton.text")); // NOI18N
        jButtonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOKActionPerformed(evt);
            }
        });

        jButtonApply.setText(bundle.getString("SettingsGUI.jButtonApply.text")); // NOI18N
        jButtonApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonApplyActionPerformed(evt);
            }
        });

        pbpunpackcheck.setText(bundle.getString("SettingsGUI.pbpunpackcheck.text")); // NOI18N

        saveWindowPosCheck.setText(bundle.getString("SettingsGUI.saveWindowPosCheck.text")); // NOI18N

        filelogCheck.setText(bundle.getString("SettingsGUI.filelogCheck.text")); // NOI18N

        loadAndRunCheck.setText(bundle.getString("SettingsGUI.loadAndRunCheck.text")); // NOI18N

        buttonGroup1.add(umdBrowser);
        umdBrowser.setText(bundle.getString("SettingsGUI.umdBrowser.text")); // NOI18N

        buttonGroup1.add(classicUmdDialog);
        classicUmdDialog.setText(bundle.getString("SettingsGUI.classicUmdDialog.text")); // NOI18N

        umdPathLabel.setText(bundle.getString("SettingsGUI.umdPathLabel.text")); // NOI18N

        umdpath.setEditable(false);
        umdpath.setText("umdimages"); // NOI18N

        umdPathBrowseButton.setText("..."); // NOI18N
        umdPathBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                umdPathBrowseButtonActionPerformed(evt);
            }
        });

        tmpPathLabel.setText(bundle.getString("SettingsGUI.tmpPathLabel.text")); // NOI18N

        tmppath.setEditable(false);
        tmppath.setText("tmp"); // NOI18N

        tmpPathBrowseButton.setText("..."); // NOI18N
        tmpPathBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tmpPathBrowseButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout GeneralPanelLayout = new javax.swing.GroupLayout(GeneralPanel);
        GeneralPanel.setLayout(GeneralPanelLayout);
        GeneralPanelLayout.setHorizontalGroup(
            GeneralPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(GeneralPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(GeneralPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(GeneralPanelLayout.createSequentialGroup()
                        .addGroup(GeneralPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(umdPathLabel)
                            .addComponent(tmpPathLabel))
                        .addGap(21, 21, 21)
                        .addGroup(GeneralPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(umdpath, javax.swing.GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
                            .addComponent(tmppath))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(GeneralPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(umdPathBrowseButton)
                            .addComponent(tmpPathBrowseButton)))
                    .addGroup(GeneralPanelLayout.createSequentialGroup()
                        .addComponent(pbpunpackcheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(138, 138, 138))
                    .addGroup(GeneralPanelLayout.createSequentialGroup()
                        .addComponent(saveWindowPosCheck, javax.swing.GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE)
                        .addGap(200, 200, 200))
                    .addGroup(GeneralPanelLayout.createSequentialGroup()
                        .addComponent(filelogCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(261, 261, 261))
                    .addGroup(GeneralPanelLayout.createSequentialGroup()
                        .addComponent(loadAndRunCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(196, 196, 196))
                    .addGroup(GeneralPanelLayout.createSequentialGroup()
                        .addComponent(umdBrowser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(289, 289, 289))
                    .addGroup(GeneralPanelLayout.createSequentialGroup()
                        .addComponent(classicUmdDialog, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(189, 189, 189)))
                .addGap(293, 293, 293))
        );
        GeneralPanelLayout.setVerticalGroup(
            GeneralPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(GeneralPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pbpunpackcheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saveWindowPosCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filelogCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(loadAndRunCheck)
                .addGap(18, 18, 18)
                .addComponent(umdBrowser)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(classicUmdDialog)
                .addGap(18, 18, 18)
                .addGroup(GeneralPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(umdPathLabel)
                    .addComponent(umdpath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(umdPathBrowseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(GeneralPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tmpPathLabel)
                    .addComponent(tmppath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tmpPathBrowseButton))
                .addContainerGap(254, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(bundle.getString("SettingsGUI.GeneralPanel.title"), GeneralPanel); // NOI18N

        jPanel1.setLayout(new java.awt.GridLayout(20, 2, 10, 0));

        imposeLabel.setText(bundle.getString("SettingsGUI.imposeLabel.text")); // NOI18N
        jPanel1.add(imposeLabel);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 364, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );

        jPanel1.add(jPanel2);

        languageLabel.setText(bundle.getString("SettingsGUI.languageLabel.text")); // NOI18N
        jPanel1.add(languageLabel);

        languageBox.setModel(makeComboBoxModel(getImposeLanguages()));
        jPanel1.add(languageBox);

        buttonLabel.setText(bundle.getString("SettingsGUI.buttonLabel.text")); // NOI18N
        jPanel1.add(buttonLabel);

        buttonBox.setModel(makeComboBoxModel(getImposeButtons()));
        jPanel1.add(buttonBox);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 364, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );

        jPanel1.add(jPanel4);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 364, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );

        jPanel1.add(jPanel5);

        sysParmLabel.setText(bundle.getString("SettingsGUI.sysParmLabel.text")); // NOI18N
        jPanel1.add(sysParmLabel);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 364, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );

        jPanel1.add(jPanel3);

        daylightLabel.setText(bundle.getString("SettingsGUI.daylightLabel.text")); // NOI18N
        jPanel1.add(daylightLabel);

        daylightBox.setModel(makeComboBoxModel(getSysparamDaylightSavings()));
        jPanel1.add(daylightBox);

        timeFormatLabel.setText(bundle.getString("SettingsGUI.timeFormatLabel.text")); // NOI18N
        jPanel1.add(timeFormatLabel);

        timeFormatBox.setModel(makeComboBoxModel(getSysparamTimeFormats()));
        jPanel1.add(timeFormatBox);

        dateFormatLabel.setText(bundle.getString("SettingsGUI.dateFormatLabel.text")); // NOI18N
        jPanel1.add(dateFormatLabel);

        dateFormatBox.setModel(makeComboBoxModel(getSysparamDateFormats()));
        jPanel1.add(dateFormatBox);

        wlanPowerLabel.setText(bundle.getString("SettingsGUI.wlanPowerLabel.text")); // NOI18N
        jPanel1.add(wlanPowerLabel);

        wlanPowerBox.setModel(makeComboBoxModel(getSysparamWlanPowerSaves()));
        jPanel1.add(wlanPowerBox);

        adhocChannelLabel.setText(bundle.getString("SettingsGUI.adhocChannel.text")); // NOI18N
        jPanel1.add(adhocChannelLabel);

        adhocChannelBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Auto", "1", "6", "11" }));
        jPanel1.add(adhocChannelBox);

        timezoneLabel.setText(bundle.getString("SettingsGUI.timezoneLabel.text")); // NOI18N
        jPanel1.add(timezoneLabel);

        timezoneSpinner.setModel(new javax.swing.SpinnerNumberModel(0, -720, 720, 1));
        jPanel1.add(timezoneSpinner);

        nicknamelLabel.setText(bundle.getString("SettingsGUI.nicknameLabel.text")); // NOI18N
        jPanel1.add(nicknamelLabel);

        nicknameTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        nicknameTextField.setText("JPCSP"); // NOI18N
        jPanel1.add(nicknameTextField);

        javax.swing.GroupLayout RegionPanelLayout = new javax.swing.GroupLayout(RegionPanel);
        RegionPanel.setLayout(RegionPanelLayout);
        RegionPanelLayout.setHorizontalGroup(
            RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RegionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 739, Short.MAX_VALUE)
                .addContainerGap())
        );
        RegionPanelLayout.setVerticalGroup(
            RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RegionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 468, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab(bundle.getString("SettingsGUI.RegionPanel.title"), RegionPanel); // NOI18N

        disableVBOCheck.setText(bundle.getString("SettingsGUI.disableVBOCheck.text")); // NOI18N

        onlyGEGraphicsCheck.setText(bundle.getString("SettingsGUI.onlyGEGraphicsCheck.text")); // NOI18N

        useVertexCache.setText(bundle.getString("SettingsGUI.useVertexCache.text")); // NOI18N

        shadersCheck.setText(bundle.getString("SettingsGUI.shaderCheck.text")); // NOI18N

        geometryShaderCheck.setText(bundle.getString("SettingsGUI.geometryShaderCheck.text")); // NOI18N

        disableUBOCheck.setText(bundle.getString("SettingsGUI.disableUBOCheck.text")); // NOI18N

        enableVAOCheck.setText(bundle.getString("SettingsGUI.enableVAOCheck.text")); // NOI18N

        enableGETextureCheck.setText(bundle.getString("SettingsGUI.enableGETextureCheck.text")); // NOI18N

        enableNativeCLUTCheck.setText(bundle.getString("SettingsGUI.enableNativeCLUTCheck.text")); // NOI18N

        enableDynamicShadersCheck.setText(bundle.getString("SettingsGUI.enableDynamicShadersCheck.text")); // NOI18N

        enableShaderStencilTestCheck.setText(bundle.getString("SettingsGUI.enableShaderStencilTestCheck.text")); // NOI18N

        enableShaderColorMaskCheck.setText(bundle.getString("SettingsGUI.enableShaderColorMaskCheck.text")); // NOI18N

        disableOptimizedVertexInfoReading.setText(bundle.getString("SettingsGUI.disableOptimizedVertexInfoReading.text")); // NOI18N

        saveStencilToMemory.setText(bundle.getString("SettingsGUI.saveStencilToMemory.text")); // NOI18N

        useSoftwareRenderer.setText(bundle.getString("SettingsGUI.useSoftwareRenderer.text")); // NOI18N

        javax.swing.GroupLayout VideoPanelLayout = new javax.swing.GroupLayout(VideoPanel);
        VideoPanel.setLayout(VideoPanelLayout);
        VideoPanelLayout.setHorizontalGroup(
            VideoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(VideoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(VideoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(VideoPanelLayout.createSequentialGroup()
                        .addComponent(disableVBOCheck, javax.swing.GroupLayout.DEFAULT_SIZE, 423, Short.MAX_VALUE)
                        .addGap(315, 315, 315))
                    .addGroup(VideoPanelLayout.createSequentialGroup()
                        .addComponent(onlyGEGraphicsCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(331, 331, 331))
                    .addGroup(VideoPanelLayout.createSequentialGroup()
                        .addComponent(useVertexCache, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(575, 575, 575))
                    .addGroup(VideoPanelLayout.createSequentialGroup()
                        .addComponent(shadersCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(606, 606, 606))
                    .addComponent(geometryShaderCheck, javax.swing.GroupLayout.PREFERRED_SIZE, 720, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(VideoPanelLayout.createSequentialGroup()
                        .addComponent(disableUBOCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(64, 64, 64))
                    .addGroup(VideoPanelLayout.createSequentialGroup()
                        .addComponent(enableVAOCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(235, 235, 235))
                    .addGroup(VideoPanelLayout.createSequentialGroup()
                        .addComponent(enableGETextureCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(304, 304, 304))
                    .addGroup(VideoPanelLayout.createSequentialGroup()
                        .addComponent(enableNativeCLUTCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18))
                    .addComponent(enableDynamicShadersCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(VideoPanelLayout.createSequentialGroup()
                        .addComponent(enableShaderStencilTestCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(103, 103, 103))
                    .addGroup(VideoPanelLayout.createSequentialGroup()
                        .addComponent(enableShaderColorMaskCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(108, 108, 108))
                    .addGroup(VideoPanelLayout.createSequentialGroup()
                        .addComponent(disableOptimizedVertexInfoReading, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(234, 234, 234))
                    .addGroup(VideoPanelLayout.createSequentialGroup()
                        .addComponent(saveStencilToMemory, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(334, 334, 334))
                    .addGroup(VideoPanelLayout.createSequentialGroup()
                        .addComponent(useSoftwareRenderer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(210, 210, 210)))
                .addGap(17, 17, 17))
        );
        VideoPanelLayout.setVerticalGroup(
            VideoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(VideoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(disableVBOCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(onlyGEGraphicsCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(useVertexCache)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(shadersCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(geometryShaderCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(disableUBOCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(enableVAOCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(enableGETextureCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(enableNativeCLUTCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(enableDynamicShadersCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(enableShaderStencilTestCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(enableShaderColorMaskCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(disableOptimizedVertexInfoReading)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saveStencilToMemory)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(useSoftwareRenderer)
                .addContainerGap(139, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(bundle.getString("SettingsGUI.VideoPanel.title"), VideoPanel); // NOI18N

        IgnoreAudioThreadsCheck.setText(bundle.getString("SettingsGUI.IgnoreAudioThreadsCheck.text")); // NOI18N

        disableBlockingAudioCheck.setText(bundle.getString("SettingsGUI.disableBlockingAudioCheck.text")); // NOI18N

        DisableSceAudioCheck.setText(bundle.getString("SettingsGUI.DisableSceAudioCheck.text")); // NOI18N

        javax.swing.GroupLayout AudioPanelLayout = new javax.swing.GroupLayout(AudioPanel);
        AudioPanel.setLayout(AudioPanelLayout);
        AudioPanelLayout.setHorizontalGroup(
            AudioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AudioPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(AudioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(AudioPanelLayout.createSequentialGroup()
                        .addComponent(IgnoreAudioThreadsCheck, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                        .addGap(150, 150, 150))
                    .addComponent(DisableSceAudioCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(AudioPanelLayout.createSequentialGroup()
                        .addComponent(disableBlockingAudioCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(147, 147, 147)))
                .addGap(405, 405, 405))
        );
        AudioPanelLayout.setVerticalGroup(
            AudioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AudioPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(IgnoreAudioThreadsCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(DisableSceAudioCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(disableBlockingAudioCheck)
                .addContainerGap(415, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(bundle.getString("SettingsGUI.AudioPanel.title"), AudioPanel); // NOI18N

        invalidMemoryCheck.setText(bundle.getString("SettingsGUI.invalidMemoryCheck.text")); // NOI18N

        ignoreUnmappedImports.setText(bundle.getString("SettingsGUI.ignoreUnmappedImports.text")); // NOI18N

        javax.swing.GroupLayout MemoryPanelLayout = new javax.swing.GroupLayout(MemoryPanel);
        MemoryPanel.setLayout(MemoryPanelLayout);
        MemoryPanelLayout.setHorizontalGroup(
            MemoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MemoryPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(MemoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(invalidMemoryCheck, javax.swing.GroupLayout.DEFAULT_SIZE, 747, Short.MAX_VALUE)
                    .addComponent(ignoreUnmappedImports, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        MemoryPanelLayout.setVerticalGroup(
            MemoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MemoryPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(invalidMemoryCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ignoreUnmappedImports)
                .addContainerGap(438, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(bundle.getString("SettingsGUI.MemoryPanel.title"), MemoryPanel); // NOI18N

        useMediaEngine.setText(bundle.getString("SettingsGUI.useMediaEngine.text")); // NOI18N

        useConnector.setText(bundle.getString("SettingsGUI.useConnector.text")); // NOI18N

        useExternalDecoder.setText(bundle.getString("SettingsGUI.useExternalDecoder.text")); // NOI18N

        useDebugFont.setText(bundle.getString("SettingsGUI.useDebugFont.text")); // NOI18N

        useDebugMemory.setText(bundle.getString("SettingsGUI.useDebugMemory.text")); // NOI18N

        javax.swing.GroupLayout MiscPanelLayout = new javax.swing.GroupLayout(MiscPanel);
        MiscPanel.setLayout(MiscPanelLayout);
        MiscPanelLayout.setHorizontalGroup(
            MiscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MiscPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(MiscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(useExternalDecoder, javax.swing.GroupLayout.DEFAULT_SIZE, 747, Short.MAX_VALUE)
                    .addComponent(useConnector, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(useMediaEngine, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(useDebugFont, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(useDebugMemory, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        MiscPanelLayout.setVerticalGroup(
            MiscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MiscPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(useMediaEngine)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(useConnector)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(useExternalDecoder)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(useDebugFont)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(useDebugMemory)
                .addContainerGap(369, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(bundle.getString("SettingsGUI.MiscPanel.title"), MiscPanel); // NOI18N

        useCompiler.setText(bundle.getString("SettingsGUI.useCompiler.text")); // NOI18N

        methodMaxInstructionsBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "50", "100", "500", "1000", "3000" }));

        profilerCheck.setText(bundle.getString("SettingsGUI.profileCheck.text")); // NOI18N

        methodMaxInstructionsLabel.setText(bundle.getString("SettingsGUI.methodMaxInstructionsLabel.text")); // NOI18N

        javax.swing.GroupLayout CompilerPanelLayout = new javax.swing.GroupLayout(CompilerPanel);
        CompilerPanel.setLayout(CompilerPanelLayout);
        CompilerPanelLayout.setHorizontalGroup(
            CompilerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CompilerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(CompilerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(useCompiler, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(profilerCheck, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, CompilerPanelLayout.createSequentialGroup()
                        .addComponent(methodMaxInstructionsBox, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(methodMaxInstructionsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(358, Short.MAX_VALUE))
        );
        CompilerPanelLayout.setVerticalGroup(
            CompilerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CompilerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(useCompiler)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(profilerCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(CompilerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(methodMaxInstructionsBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(methodMaxInstructionsLabel))
                .addContainerGap(412, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(bundle.getString("SettingsGUI.CompilerPanel.title"), CompilerPanel); // NOI18N

        extractEboot.setText(bundle.getString("SettingsGUI.extractEboot.text")); // NOI18N

        cryptoSavedata.setText(bundle.getString("SettingsGUI.cryptoSavedata.text")); // NOI18N

        extractPGD.setText(bundle.getString("SettingsGUI.extractPGD.text")); // NOI18N

        javax.swing.GroupLayout CryptoPanelLayout = new javax.swing.GroupLayout(CryptoPanel);
        CryptoPanel.setLayout(CryptoPanelLayout);
        CryptoPanelLayout.setHorizontalGroup(
            CryptoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CryptoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(CryptoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(extractEboot, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cryptoSavedata, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(extractPGD, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap(394, Short.MAX_VALUE))
        );
        CryptoPanelLayout.setVerticalGroup(
            CryptoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CryptoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(extractEboot)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cryptoSavedata)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(extractPGD)
                .addContainerGap(415, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(bundle.getString("SettingsGUI.CryptoPanel.title"), CryptoPanel); // NOI18N

        antiAliasLabel.setText(bundle.getString("SettingsGUI.antiAliasLabel.text")); // NOI18N

        antiAliasingBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "OFF", "x4", "x8", "x16" }));

        resolutionLabel.setText(bundle.getString("SettingsGUI.resolutionLabel.text")); // NOI18N

        resolutionBox.setModel(makeResolutions());

        fullscreenCheck.setText(bundle.getString("SettingsGUI.fullscreenCheck.text")); // NOI18N

        javax.swing.GroupLayout DisplayPanelLayout = new javax.swing.GroupLayout(DisplayPanel);
        DisplayPanel.setLayout(DisplayPanelLayout);
        DisplayPanelLayout.setHorizontalGroup(
            DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DisplayPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fullscreenCheck)
                    .addGroup(DisplayPanelLayout.createSequentialGroup()
                        .addGroup(DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(DisplayPanelLayout.createSequentialGroup()
                                .addComponent(antiAliasLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                                .addGap(28, 28, 28))
                            .addComponent(resolutionLabel))
                        .addGroup(DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(antiAliasingBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(resolutionBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addGap(1318, 1318, 1318))
        );
        DisplayPanelLayout.setVerticalGroup(
            DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DisplayPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(antiAliasLabel)
                    .addComponent(antiAliasingBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(resolutionBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(resolutionLabel))
                .addGap(18, 18, 18)
                .addComponent(fullscreenCheck)
                .addContainerGap(385, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(bundle.getString("SettingsGUI.DisplayPanel.title"), DisplayPanel); // NOI18N

        cancelButton.setText(bundle.getString("CancelButton.text")); // NOI18N
        cancelButton.setParent(this);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap(257, Short.MAX_VALUE)
                        .addComponent(jButtonOK, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonApply, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, 178, Short.MAX_VALUE))
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButtonApply)
                        .addComponent(jButtonOK)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public void RefreshWindow() {
        setAllComponentsFromSettings();
    }

    private void jButtonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOKActionPerformed
        setAllComponentsToSettings();
        dispose();
	}//GEN-LAST:event_jButtonOKActionPerformed

    private void jButtonApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonApplyActionPerformed
        setAllComponentsToSettings();
	}//GEN-LAST:event_jButtonApplyActionPerformed

    private void umdPathBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_umdPathBrowseButtonActionPerformed
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
        FolderChooser folderChooser = new FolderChooser(bundle.getString("SettingsGUI.strSelectUMDPath.text"));
        int result = folderChooser.showSaveDialog(umdPathBrowseButton.getTopLevelAncestor());
        if (result == FolderChooser.APPROVE_OPTION) {
            umdpath.setText(folderChooser.getSelectedFile().getPath());
        }
    }//GEN-LAST:event_umdPathBrowseButtonActionPerformed

    private void tmpPathBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tmpPathBrowseButtonActionPerformed
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jpcsp/languages/jpcsp"); // NOI18N
        FolderChooser folderChooser = new FolderChooser(bundle.getString("SettingsGUI.strSelectTMPPath.text"));
        int result = folderChooser.showSaveDialog(tmpPathBrowseButton.getTopLevelAncestor());
        if (result == FolderChooser.APPROVE_OPTION) {
            tmppath.setText(folderChooser.getSelectedFile().getPath());
        }
    }//GEN-LAST:event_tmpPathBrowseButtonActionPerformed

    @Override
    public void dispose() {
        Emulator.getMainGUI().endWindowDialog();
        super.dispose();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel AudioPanel;
    private javax.swing.JPanel CompilerPanel;
    private javax.swing.JPanel CryptoPanel;
    private javax.swing.JCheckBox DisableSceAudioCheck;
    private javax.swing.JPanel DisplayPanel;
    private javax.swing.JPanel GeneralPanel;
    private javax.swing.JCheckBox IgnoreAudioThreadsCheck;
    private javax.swing.JPanel MemoryPanel;
    private javax.swing.JPanel MiscPanel;
    private javax.swing.JPanel RegionPanel;
    private javax.swing.JPanel VideoPanel;
    private javax.swing.JComboBox adhocChannelBox;
    private javax.swing.JLabel adhocChannelLabel;
    private javax.swing.JLabel antiAliasLabel;
    private javax.swing.JComboBox antiAliasingBox;
    private javax.swing.JComboBox buttonBox;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel buttonLabel;
    private jpcsp.GUI.CancelButton cancelButton;
    private javax.swing.JRadioButton classicUmdDialog;
    private javax.swing.JCheckBox cryptoSavedata;
    private javax.swing.JComboBox dateFormatBox;
    private javax.swing.JLabel dateFormatLabel;
    private javax.swing.JComboBox daylightBox;
    private javax.swing.JLabel daylightLabel;
    private javax.swing.JCheckBox disableBlockingAudioCheck;
    private javax.swing.JCheckBox disableOptimizedVertexInfoReading;
    private javax.swing.JCheckBox disableUBOCheck;
    private javax.swing.JCheckBox disableVBOCheck;
    private javax.swing.JCheckBox enableDynamicShadersCheck;
    private javax.swing.JCheckBox enableGETextureCheck;
    private javax.swing.JCheckBox enableNativeCLUTCheck;
    private javax.swing.JCheckBox enableShaderColorMaskCheck;
    private javax.swing.JCheckBox enableShaderStencilTestCheck;
    private javax.swing.JCheckBox enableVAOCheck;
    private javax.swing.JCheckBox extractEboot;
    private javax.swing.JCheckBox extractPGD;
    private javax.swing.JCheckBox filelogCheck;
    private javax.swing.JCheckBox fullscreenCheck;
    private javax.swing.JCheckBox geometryShaderCheck;
    private javax.swing.JCheckBox ignoreUnmappedImports;
    private javax.swing.JLabel imposeLabel;
    private javax.swing.JCheckBox invalidMemoryCheck;
    private javax.swing.JButton jButtonApply;
    private javax.swing.JButton jButtonOK;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JComboBox languageBox;
    private javax.swing.JLabel languageLabel;
    private javax.swing.JCheckBox loadAndRunCheck;
    private javax.swing.JComboBox methodMaxInstructionsBox;
    private javax.swing.JLabel methodMaxInstructionsLabel;
    private javax.swing.JTextField nicknameTextField;
    private javax.swing.JLabel nicknamelLabel;
    private javax.swing.JCheckBox onlyGEGraphicsCheck;
    private javax.swing.JCheckBox pbpunpackcheck;
    private javax.swing.JCheckBox profilerCheck;
    private javax.swing.JComboBox resolutionBox;
    private javax.swing.JLabel resolutionLabel;
    private javax.swing.JCheckBox saveStencilToMemory;
    private javax.swing.JCheckBox saveWindowPosCheck;
    private javax.swing.JCheckBox shadersCheck;
    private javax.swing.JLabel sysParmLabel;
    private javax.swing.JComboBox timeFormatBox;
    private javax.swing.JLabel timeFormatLabel;
    private javax.swing.JLabel timezoneLabel;
    private javax.swing.JSpinner timezoneSpinner;
    private javax.swing.JButton tmpPathBrowseButton;
    private javax.swing.JLabel tmpPathLabel;
    private javax.swing.JTextField tmppath;
    private javax.swing.JRadioButton umdBrowser;
    private javax.swing.JButton umdPathBrowseButton;
    private javax.swing.JLabel umdPathLabel;
    private javax.swing.JTextField umdpath;
    private javax.swing.JCheckBox useCompiler;
    private javax.swing.JCheckBox useConnector;
    private javax.swing.JCheckBox useDebugFont;
    private javax.swing.JCheckBox useDebugMemory;
    private javax.swing.JCheckBox useExternalDecoder;
    private javax.swing.JCheckBox useMediaEngine;
    private javax.swing.JCheckBox useSoftwareRenderer;
    private javax.swing.JCheckBox useVertexCache;
    private javax.swing.JComboBox wlanPowerBox;
    private javax.swing.JLabel wlanPowerLabel;
    // End of variables declaration//GEN-END:variables
}
