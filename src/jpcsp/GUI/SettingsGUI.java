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
import jpcsp.Resource;
import jpcsp.HLE.modules.sceUtility;
import jpcsp.settings.Settings;

import com.jidesoft.swing.FolderChooser;

/**
 *
 * @author  shadow
 */
public class SettingsGUI extends javax.swing.JFrame {
	private static final long serialVersionUID = -732715495873159718L;
	private Settings settings;
    
    /** Creates new form SettingsGUI */
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
        setBoolFromSettings(useSoftwareRenderer, "emu.useSoftwareRenderer");
        setBoolFromSettings(onlyGEGraphicsCheck, "emu.onlyGEGraphics");
        setBoolFromSettings(useConnector, "emu.useConnector");
        setBoolFromSettings(useDebugFont, "emu.useDebugFont");
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
        setBoolFromSettings(umdBrowser, ClassicOpenDialogumd, "emu.umdbrowser");
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
        setBoolToSettings(useSoftwareRenderer, "emu.useSoftwareRenderer");
        setBoolToSettings(onlyGEGraphicsCheck, "emu.onlyGEGraphics");
        setBoolToSettings(useConnector, "emu.useConnector");
        setBoolToSettings(useDebugFont, "emu.useDebugFont");
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

    private ComboBoxModel makeLanguageComboBoxModel() {
        MutableComboBoxModel comboBox = new DefaultComboBoxModel();
        for (String language : getImposeLanguages()) {
        	comboBox.addElement(language);
        }

        return comboBox;
    }

    public static String[] getImposeLanguages() {
    	return new String[] { Resource.getEnglish("japanese"),
    	                      Resource.getEnglish("english"),
    	                      Resource.getEnglish("french"),
    	                      Resource.getEnglish("spanish"),
    	                      Resource.getEnglish("german"),
    	                      Resource.getEnglish("italian"),
    	                      Resource.getEnglish("dutch"),
    	                      Resource.getEnglish("portuguese"),
    	                      Resource.getEnglish("russian"),
    	                      Resource.getEnglish("korean"),
    	                      Resource.getEnglish("traditionalChinese"),
    	                      Resource.getEnglish("simplifiedChinese") };
    }

    public static String[] getImposeButtons() {
    	return new String[] { "\"O\" for \"Enter\"", "\"X\" for \"Enter\"" };
    }

    public static String[] getSysparamDaylightSavings() {
    	return new String[] { "Off", "On" };
    }

    public static String[] getSysparamTimeFormats() {
    	return new String[] { "24H", "12H" };
    }

    public static String[] getSysparamDateFormats() {
    	return new String[] { "YYYY-MM-DD", "MM-DD-YYYY", "DD-MM-YYYY" };
    }

    public static String[] getSysparamWlanPowerSaves() {
    	return new String[] { "Off", "On" };
    }

    public static String[] getSysparamAdhocChannels() {
    	return new String[] { "Auto", "1", "6", "11" };
    }

    private ComboBoxModel makeMethodMaxInstructions() {
        MutableComboBoxModel comboBox = new DefaultComboBoxModel();
        comboBox.addElement("50");
        comboBox.addElement("100");
        comboBox.addElement("500");
        comboBox.addElement("1000");
        comboBox.addElement("3000");

        return comboBox;
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

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jButtonOK = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();
        jButtonApply = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        generalPanel = new javax.swing.JPanel();
        pbpunpackcheck = new javax.swing.JCheckBox();
        saveWindowPosCheck = new javax.swing.JCheckBox();
        umdBrowser = new javax.swing.JRadioButton();
        ClassicOpenDialogumd = new javax.swing.JRadioButton();
        umdpath = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        filelogCheck = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        tmppath = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        RegionPanel = new javax.swing.JPanel();
        languageLabel = new javax.swing.JLabel();
        languageBox = new javax.swing.JComboBox();
        buttonLabel = new javax.swing.JLabel();
        buttonBox = new javax.swing.JComboBox();
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
        imposeLabel = new javax.swing.JLabel();
        imposeLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        VideoPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
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
        useSoftwareRenderer = new javax.swing.JCheckBox();
        AudioPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        IgnoreAudioThreadsCheck = new javax.swing.JCheckBox();
        disableBlockingAudioCheck = new javax.swing.JCheckBox();
        DisableSceAudioCheck = new javax.swing.JCheckBox();
        MemoryPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        invalidMemoryCheck = new javax.swing.JCheckBox();
        ignoreUnmappedImports = new javax.swing.JCheckBox();
        MiscPanel = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        useMediaEngine = new javax.swing.JCheckBox();
        useConnector = new javax.swing.JCheckBox();
        useExternalDecoder = new javax.swing.JCheckBox();
        useDebugFont = new javax.swing.JCheckBox();
        CompilerPanel = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        useCompiler = new javax.swing.JCheckBox();
        methodMaxInstructionsBox = new javax.swing.JComboBox();
        profilerCheck = new javax.swing.JCheckBox();
        methodMaxInstructionsLabel = new javax.swing.JLabel();
        CryptoPanel = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        extractEboot = new javax.swing.JCheckBox();
        cryptoSavedata = new javax.swing.JCheckBox();
        extractPGD = new javax.swing.JCheckBox();
        DisplayPanel = new javax.swing.JPanel();
        fullscreenCheck = new javax.swing.JCheckBox();
        antiAliasLabel = new javax.swing.JLabel();
        antiAliasingBox = new javax.swing.JComboBox();
        resolutionLabel = new javax.swing.JLabel();
        resolutionBox = new javax.swing.JComboBox();

        setTitle("Configuration");
        setResizable(false);

        jButtonOK.setText(Resource.get("ok"));
        jButtonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOKActionPerformed(evt);
            }
        });

        jButtonCancel.setText(Resource.get("cancel"));
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        jButtonApply.setText(Resource.get("apply"));
        jButtonApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonApplyActionPerformed(evt);
            }
        });

        pbpunpackcheck.setText(Resource.get("unpackpbp"));

        saveWindowPosCheck.setText(Resource.get("saveposition"));

        buttonGroup1.add(umdBrowser);
        umdBrowser.setText(Resource.get("useUMDBrowser"));

        buttonGroup1.add(ClassicOpenDialogumd);
        ClassicOpenDialogumd.setText(Resource.get("useclassicUMD"));

        umdpath.setEditable(false);

        jLabel1.setText(Resource.get("UMDpath"));

        jButton1.setText("...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        filelogCheck.setText(Resource.get("enablefileIO"));

        jLabel2.setText(Resource.get("TMPpath"));

        tmppath.setEditable(false);

        jButton2.setText("...");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout generalPanelLayout = new javax.swing.GroupLayout(generalPanel);
        generalPanel.setLayout(generalPanelLayout);
        generalPanelLayout.setHorizontalGroup(
            generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generalPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(generalPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(umdpath, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(generalPanelLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tmppath, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(umdBrowser, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(ClassicOpenDialogumd, javax.swing.GroupLayout.Alignment.LEADING))
                    .addComponent(filelogCheck)
                    .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(pbpunpackcheck, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(saveWindowPosCheck, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(246, Short.MAX_VALUE))
        );
        generalPanelLayout.setVerticalGroup(
            generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generalPanelLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(pbpunpackcheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saveWindowPosCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filelogCheck)
                .addGap(41, 41, 41)
                .addComponent(umdBrowser)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(ClassicOpenDialogumd)
                .addGap(18, 18, 18)
                .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(umdpath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addGap(18, 18, 18)
                .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(tmppath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2))
                .addContainerGap(74, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(Resource.get("general"), generalPanel);

        languageLabel.setText(Resource.get("language"));

        languageBox.setModel(makeLanguageComboBoxModel());

        buttonLabel.setText(Resource.get("buttonpref"));

        buttonBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "\"O\" for \"Enter\"", "\"X\" for \"Enter\"" }));

        daylightLabel.setText(Resource.get("daylightSavings"));

        daylightBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Off", "On" }));

        timeFormatLabel.setText(Resource.get("timeformat"));

        timeFormatBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "24H", "12H" }));

        dateFormatLabel.setText(Resource.get("dateformat"));

        dateFormatBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "YYYY-MM-DD", "MM-DD-YYYY", "DD-MM-YYYY" }));

        wlanPowerLabel.setText(Resource.get("wlanpowersaving"));

        wlanPowerBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Off", "On" }));

        adhocChannelLabel.setText(Resource.get("adhocChannel"));

        adhocChannelBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Auto", "1", "2", "3", "4" }));

        timezoneLabel.setText(Resource.get("timezone"));

        timezoneSpinner.setModel(new javax.swing.SpinnerNumberModel(0, -720, 720, 1));

        nicknamelLabel.setText(Resource.get("nickname"));

        nicknameTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        nicknameTextField.setText("JPCSP");

        imposeLabel.setText("Impose:");

        imposeLabel1.setText("System param:");

        javax.swing.GroupLayout RegionPanelLayout = new javax.swing.GroupLayout(RegionPanel);
        RegionPanel.setLayout(RegionPanelLayout);
        RegionPanelLayout.setHorizontalGroup(
            RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RegionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(imposeLabel)
                    .addGroup(RegionPanelLayout.createSequentialGroup()
                        .addGroup(RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(imposeLabel1)
                            .addComponent(daylightLabel)
                            .addComponent(timezoneLabel)
                            .addComponent(timeFormatLabel)
                            .addComponent(dateFormatLabel)
                            .addComponent(wlanPowerLabel)
                            .addComponent(adhocChannelLabel)
                            .addComponent(nicknamelLabel))
                        .addGap(29, 29, 29)
                        .addGroup(RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(timezoneSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(daylightBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(timeFormatBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dateFormatBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(wlanPowerBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(adhocChannelBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(nicknameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(RegionPanelLayout.createSequentialGroup()
                        .addGroup(RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(languageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(buttonLabel))
                        .addGap(117, 117, 117)
                        .addGroup(RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(languageBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 819, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        RegionPanelLayout.setVerticalGroup(
            RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RegionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(imposeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(languageLabel)
                    .addComponent(languageBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonLabel)
                    .addComponent(buttonBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(24, 24, 24)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(imposeLabel1)
                .addGap(18, 18, 18)
                .addGroup(RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(daylightLabel)
                    .addComponent(daylightBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(timezoneLabel)
                    .addComponent(timezoneSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(timeFormatLabel)
                    .addComponent(timeFormatBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dateFormatLabel)
                    .addComponent(dateFormatBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(wlanPowerLabel)
                    .addComponent(wlanPowerBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(adhocChannelLabel)
                    .addComponent(adhocChannelBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RegionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nicknamelLabel)
                    .addComponent(nicknameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(31, 31, 31))
        );

        jTabbedPane1.addTab(Resource.get("region"), RegionPanel);

        disableVBOCheck.setText(Resource.get("disablevbo"));

        onlyGEGraphicsCheck.setText(Resource.get("onlyGeGraphics"));

        useVertexCache.setText(Resource.get("usevertex"));

        shadersCheck.setText(Resource.get("useshader"));

        geometryShaderCheck.setText(Resource.get("useGeometryShader"));

        disableUBOCheck.setText(Resource.get("disableubo"));

        enableVAOCheck.setText(Resource.get("enablevao"));

        enableGETextureCheck.setText(Resource.get("enablegetexture"));

        enableNativeCLUTCheck.setText(Resource.get("enablenativeclut"));

        enableDynamicShadersCheck.setText(Resource.get("enabledynamicshaders"));

        enableShaderStencilTestCheck.setText(Resource.get("enableshaderstenciltest"));

        enableShaderColorMaskCheck.setText(Resource.get("enableshadercolormask"));

        disableOptimizedVertexInfoReading.setText(Resource.get("disableoptimizedvertexinforeading"));

        useSoftwareRenderer.setText(Resource.get("useSoftwareRenderer"));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(disableVBOCheck)
                    .addComponent(onlyGEGraphicsCheck)
                    .addComponent(useVertexCache)
                    .addComponent(shadersCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(geometryShaderCheck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(disableUBOCheck)
                    .addComponent(enableVAOCheck)
                    .addComponent(enableGETextureCheck)
                    .addComponent(enableNativeCLUTCheck)
                    .addComponent(enableDynamicShadersCheck)
                    .addComponent(enableShaderStencilTestCheck)
                    .addComponent(enableShaderColorMaskCheck)
                    .addComponent(disableOptimizedVertexInfoReading)
                    .addComponent(useSoftwareRenderer))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
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
                .addComponent(useSoftwareRenderer)
                .addContainerGap(30, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout VideoPanelLayout = new javax.swing.GroupLayout(VideoPanel);
        VideoPanel.setLayout(VideoPanelLayout);
        VideoPanelLayout.setHorizontalGroup(
            VideoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(VideoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(423, Short.MAX_VALUE))
        );
        VideoPanelLayout.setVerticalGroup(
            VideoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(VideoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(Resource.get("video"), VideoPanel);

        IgnoreAudioThreadsCheck.setText(Resource.get("disableaudiothreads"));

        disableBlockingAudioCheck.setText(Resource.get("disableaudiotblocking"));

        DisableSceAudioCheck.setText(Resource.get("disableaudiotchannels"));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(IgnoreAudioThreadsCheck)
            .addComponent(DisableSceAudioCheck)
            .addComponent(disableBlockingAudioCheck)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(IgnoreAudioThreadsCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(DisableSceAudioCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(disableBlockingAudioCheck))
        );

        javax.swing.GroupLayout AudioPanelLayout = new javax.swing.GroupLayout(AudioPanel);
        AudioPanel.setLayout(AudioPanelLayout);
        AudioPanelLayout.setHorizontalGroup(
            AudioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AudioPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(429, Short.MAX_VALUE))
        );
        AudioPanelLayout.setVerticalGroup(
            AudioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AudioPanelLayout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(257, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(Resource.get("audio"), AudioPanel);

        invalidMemoryCheck.setText(Resource.get("ignoreinvalidmemory"));

        ignoreUnmappedImports.setText(Resource.get("ignoreUnmaped"));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ignoreUnmappedImports)
                    .addComponent(invalidMemoryCheck))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(invalidMemoryCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ignoreUnmappedImports)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout MemoryPanelLayout = new javax.swing.GroupLayout(MemoryPanel);
        MemoryPanel.setLayout(MemoryPanelLayout);
        MemoryPanelLayout.setHorizontalGroup(
            MemoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MemoryPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(423, Short.MAX_VALUE))
        );
        MemoryPanelLayout.setVerticalGroup(
            MemoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MemoryPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(269, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(Resource.get("memory"), MemoryPanel);

        useMediaEngine.setText(Resource.get("useMediaEngine"));

        useConnector.setText(Resource.get("useConnector"));

        useExternalDecoder.setText(Resource.get("useExternalDecoder"));

        useDebugFont.setText(Resource.get("useDebugFont"));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(useMediaEngine, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(useConnector, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(useExternalDecoder, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(useDebugFont, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(useMediaEngine)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(useConnector)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(useExternalDecoder)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(useDebugFont)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout MiscPanelLayout = new javax.swing.GroupLayout(MiscPanel);
        MiscPanel.setLayout(MiscPanelLayout);
        MiscPanelLayout.setHorizontalGroup(
            MiscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MiscPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(423, Short.MAX_VALUE))
        );
        MiscPanelLayout.setVerticalGroup(
            MiscPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MiscPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(223, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(Resource.get("misc"), MiscPanel);

        useCompiler.setText(Resource.get("useCompiler"));

        methodMaxInstructionsBox.setModel(makeMethodMaxInstructions());

        profilerCheck.setText(Resource.get("outputprofiler"));

        methodMaxInstructionsLabel.setText(Resource.get("methodMaxInstructions"));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(useCompiler, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(profilerCheck, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel5Layout.createSequentialGroup()
                        .addComponent(methodMaxInstructionsBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, javax.swing.GroupLayout.DEFAULT_SIZE)
                        .addComponent(methodMaxInstructionsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(useCompiler)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(profilerCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(methodMaxInstructionsBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(methodMaxInstructionsLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout CompilerPanelLayout = new javax.swing.GroupLayout(CompilerPanel);
        CompilerPanel.setLayout(CompilerPanelLayout);
        CompilerPanelLayout.setHorizontalGroup(
            CompilerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CompilerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(406, Short.MAX_VALUE))
        );
        CompilerPanelLayout.setVerticalGroup(
            CompilerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CompilerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(243, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(Resource.get("compiler"), CompilerPanel);

        extractEboot.setText(Resource.get("extractEboot"));

        cryptoSavedata.setText(Resource.get("cryptoSavedata"));

        extractPGD.setText(Resource.get("extractPGD"));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(extractEboot, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cryptoSavedata, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(extractPGD, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(extractEboot)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cryptoSavedata)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(extractPGD)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout CryptoPanelLayout = new javax.swing.GroupLayout(CryptoPanel);
        CryptoPanel.setLayout(CryptoPanelLayout);
        CryptoPanelLayout.setHorizontalGroup(
            CryptoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CryptoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(423, Short.MAX_VALUE))
        );
        CryptoPanelLayout.setVerticalGroup(
            CryptoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CryptoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(246, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(Resource.get("crypto"), CryptoPanel);

        fullscreenCheck.setText(Resource.get("fullscreenMode"));

        antiAliasLabel.setText(Resource.get("antiAliasing"));

        antiAliasingBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "OFF", "x4", "x8", "x16" }));

        resolutionLabel.setText(Resource.get("resolution"));

        resolutionBox.setModel(makeResolutions());

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
                            .addComponent(antiAliasLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(resolutionLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(resolutionBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(antiAliasingBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(405, Short.MAX_VALUE))
        );
        DisplayPanelLayout.setVerticalGroup(
            DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DisplayPanelLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(antiAliasLabel)
                    .addComponent(antiAliasingBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addGroup(DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(resolutionLabel)
                    .addComponent(resolutionBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(fullscreenCheck)
                .addContainerGap(228, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(Resource.get("display"), DisplayPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jButtonOK, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonApply, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 535, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 377, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonApply)
                    .addComponent(jButtonCancel)
                    .addComponent(jButtonOK))
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

	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
		FolderChooser folderChooser = new FolderChooser("Select UMD folder");
		int result = folderChooser.showSaveDialog(jButton1.getTopLevelAncestor());
		if (result == FolderChooser.APPROVE_OPTION) {
			umdpath.setText(folderChooser.getSelectedFile().getPath());
		}
	}//GEN-LAST:event_jButton1ActionPerformed

	private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
		RefreshWindow();
		dispose();
	}//GEN-LAST:event_jButtonCancelActionPerformed

	private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
		FolderChooser folderChooser = new FolderChooser("Select TMP folder");
		int result = folderChooser.showSaveDialog(jButton2.getTopLevelAncestor());
		if (result == FolderChooser.APPROVE_OPTION) {
			tmppath.setText(folderChooser.getSelectedFile().getPath());
		}
	}//GEN-LAST:event_jButton2ActionPerformed


	@Override
	public void dispose() {
		Emulator.getMainGUI().endWindowDialog();
		super.dispose();
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel AudioPanel;
    private javax.swing.JRadioButton ClassicOpenDialogumd;
    private javax.swing.JPanel CompilerPanel;
    private javax.swing.JPanel CryptoPanel;
    private javax.swing.JCheckBox DisableSceAudioCheck;
    private javax.swing.JPanel DisplayPanel;
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
    private javax.swing.JCheckBox cryptoSavedata;
    private javax.swing.JComboBox dateFormatBox;
    private javax.swing.JLabel dateFormatLabel;
    private javax.swing.JComboBox daylightBox;
    private javax.swing.JLabel daylightLabel;
    private javax.swing.JCheckBox disableBlockingAudioCheck;
    private javax.swing.JCheckBox disableOptimizedVertexInfoReading;
    private javax.swing.JCheckBox useSoftwareRenderer;
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
    private javax.swing.JPanel generalPanel;
    private javax.swing.JCheckBox geometryShaderCheck;
    private javax.swing.JCheckBox ignoreUnmappedImports;
    private javax.swing.JLabel imposeLabel;
    private javax.swing.JLabel imposeLabel1;
    private javax.swing.JCheckBox invalidMemoryCheck;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButtonApply;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonOK;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JComboBox languageBox;
    private javax.swing.JLabel languageLabel;
    private javax.swing.JComboBox methodMaxInstructionsBox;
    private javax.swing.JLabel methodMaxInstructionsLabel;
    private javax.swing.JTextField nicknameTextField;
    private javax.swing.JLabel nicknamelLabel;
    private javax.swing.JCheckBox onlyGEGraphicsCheck;
    private javax.swing.JCheckBox pbpunpackcheck;
    private javax.swing.JCheckBox profilerCheck;
    private javax.swing.JComboBox resolutionBox;
    private javax.swing.JLabel resolutionLabel;
    private javax.swing.JCheckBox saveWindowPosCheck;
    private javax.swing.JCheckBox shadersCheck;
    private javax.swing.JComboBox timeFormatBox;
    private javax.swing.JLabel timeFormatLabel;
    private javax.swing.JLabel timezoneLabel;
    private javax.swing.JSpinner timezoneSpinner;
    private javax.swing.JTextField tmppath;
    private javax.swing.JRadioButton umdBrowser;
    private javax.swing.JTextField umdpath;
    private javax.swing.JCheckBox useCompiler;
    private javax.swing.JCheckBox useConnector;
    private javax.swing.JCheckBox useDebugFont;
    private javax.swing.JCheckBox useExternalDecoder;
    private javax.swing.JCheckBox useMediaEngine;
    private javax.swing.JCheckBox useVertexCache;
    private javax.swing.JComboBox wlanPowerBox;
    private javax.swing.JLabel wlanPowerLabel;
    // End of variables declaration//GEN-END:variables
}
