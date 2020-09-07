package org.hortonmachine.gui.settings;

import static org.hortonmachine.gears.utils.PreferencesHandler.*;
import static org.hortonmachine.gears.utils.PreferencesHandler.HM_PREF_PROXYHOST;
import static org.hortonmachine.gears.utils.PreferencesHandler.HM_PREF_PROXYPORT;
import static org.hortonmachine.gears.utils.PreferencesHandler.HM_PREF_PROXYPWD;
import static org.hortonmachine.gears.utils.PreferencesHandler.HM_PREF_PROXYUSER;
import static org.hortonmachine.gears.utils.PreferencesHandler.HM_PREF_SHP_CHARSET;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.io.File;
import java.nio.charset.Charset;
import java.util.prefs.Preferences;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.log.PreferencesDb;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.ssh.ProxyEnabler;
import org.hortonmachine.ssh.SshUtilities;

public class SettingsController extends SettingsView implements IOnCloseListener {

    public SettingsController() {
        setPreferredSize(new Dimension(900, 400));

        fillFromPreferences();
    }

    private void fillFromPreferences() {
        String proxyCheck = PreferencesHandler.getPreference(HM_PREF_PROXYCHECK, "false");
        String proxyHost = PreferencesHandler.getPreference(HM_PREF_PROXYHOST, "");
        String proxyPort = PreferencesHandler.getPreference(HM_PREF_PROXYPORT, "");
        String proxyUser = PreferencesHandler.getPreference(HM_PREF_PROXYUSER, "");
        String proxyPwd = PreferencesHandler.getPreference(HM_PREF_PROXYPWD, "");

        _proxyCheckbox.setSelected(Boolean.parseBoolean(proxyCheck));
        _proxyHostField.setText(proxyHost);
        _proxyPortField.setText(proxyPort);
        _proxyUserField.setText(proxyUser);
        _proxyPasswordField.setText(proxyPwd);

        String shpCharset = PreferencesHandler.getPreference(HM_PREF_SHP_CHARSET, "");
        _charsetTextField.setText(shpCharset);

        ComponentOrientation co = PreferencesHandler.getComponentOrientation();
        _orientationCombo.setModel(new DefaultComboBoxModel<String>(
                new String[]{PreferencesHandler.LEFT_TO_RIGHT, PreferencesHandler.RIGHT_TO_LEFT}));
        _orientationCombo
                .setSelectedItem(co.isLeftToRight() ? PreferencesHandler.LEFT_TO_RIGHT : PreferencesHandler.RIGHT_TO_LEFT);
        _orientationCombo.addActionListener(( e ) -> {
            String selection = (String) _orientationCombo.getSelectedItem();
            if (selection != null) {
                PreferencesHandler.saveComponentOrientation(selection);
            }
        });

        GuiUtilities.setFolderBrowsingOnWidgets(_preferencesDbPAth, _preferencesDbButton, null);

        Preferences preferences = Preferences.userRoot().node(PreferencesDb.PREFS_NODE_NAME);
        File baseFolder = PreferencesDb.getBaseFolder();
        String folderPath = preferences.get(PreferencesDb.HM_PREF_PREFFOLDER, baseFolder.getAbsolutePath());
        _preferencesDbPAth.setText(folderPath);

        String sshKeyPath = SshUtilities.getPreference(SshUtilities.KEYPATH, "");
        _sshKeyPathField.setText(sshKeyPath);
        GuiUtilities.setFileBrowsingOnWidgets(_sshKeyPathField, _sshKeyButton, null, null);
        String sshKeyPassphrase = SshUtilities.getPreference(SshUtilities.KEYPASSPHRASE, "");
        _sshKeyPassphraseField.setText(sshKeyPassphrase);

        String spatialiteLibsFolder = DbsUtilities.getPreference(DbsUtilities.SPATIALITE_DYLIB_FOLDER, "");
        _spatialiteModPathField.setText(spatialiteLibsFolder);
        GuiUtilities.setFolderBrowsingOnWidgets(_spatialiteModPathField, _spatialiteModButton, null);
    }

    private void applySettingsAndSavePreferences() throws Exception {
        String charset = _charsetTextField.getText();
        if (charset.trim().length() != 0) {
            if (!Charset.isSupported(charset)) {
                GuiUtilities.showWarningMessage(this, "Unsupported charset: " + charset);
            } else {
                PreferencesHandler.setPreference(HM_PREF_SHP_CHARSET, charset);
            }
        } else {
            PreferencesHandler.setPreference(HM_PREF_SHP_CHARSET, "");
        }

        boolean proxySelected = _proxyCheckbox.isSelected();
        PreferencesHandler.setPreference(HM_PREF_PROXYCHECK, proxySelected ? "true" : "false");
        if (proxySelected) {
            String host = _proxyHostField.getText();
            String port = _proxyPortField.getText();
            String user = _proxyUserField.getText();
            String pwd = _proxyPasswordField.getText();

            PreferencesHandler.setPreference(HM_PREF_PROXYHOST, host);
            PreferencesHandler.setPreference(HM_PREF_PROXYPORT, port);
            PreferencesHandler.setPreference(HM_PREF_PROXYUSER, user);
            PreferencesHandler.setPreference(HM_PREF_PROXYPWD, pwd);
        }

        String folderPath = _preferencesDbPAth.getText();
        if (new File(folderPath).exists()) {
            Preferences preferences = Preferences.userRoot().node(PreferencesDb.PREFS_NODE_NAME);
            preferences.put(PreferencesDb.HM_PREF_PREFFOLDER, folderPath);
        }

        String sshPath = _sshKeyPathField.getText().trim();
        File sshFile = new File(sshPath);
        if (sshFile.exists() && sshFile.isFile()) {
            SshUtilities.setPreference(SshUtilities.KEYPATH, sshPath);
        }
        String sshKeyPassphrase = _sshKeyPassphraseField.getText().trim();
        SshUtilities.setPreference(SshUtilities.KEYPASSPHRASE, sshKeyPassphrase);

        String spatialiteModField = _spatialiteModPathField.getText();
        if (new File(spatialiteModField).exists() && new File(spatialiteModField).isDirectory()) {
            DbsUtilities.setPreference(DbsUtilities.SPATIALITE_DYLIB_FOLDER, spatialiteModField);
        }
    }

    public JComponent asJComponent() {
        return this;
    }

    @Override
    public boolean canCloseWithoutPrompt() {
        return true;
    }

    public void onClose() {
        try {
            applySettingsAndSavePreferences();
        } catch (Exception e) {
            Logger.INSTANCE.insertError("SettingsController", "An error occurred while saving the settings", e);
        }
    }

    /**
     * Applies all settings to the current component or module.
     *  
     * @param component
     */
    public static void applySettings( Component component ) {
        String doProxy = PreferencesHandler.getPreference(HM_PREF_PROXYCHECK, "false");
        if (Boolean.parseBoolean(doProxy)) {
            String host = PreferencesHandler.getPreference(HM_PREF_PROXYHOST, "");
            String port = PreferencesHandler.getPreference(HM_PREF_PROXYPORT, "");
            String user = PreferencesHandler.getPreference(HM_PREF_PROXYUSER, "");
            String pwd = PreferencesHandler.getPreference(HM_PREF_PROXYPWD, "");

            ProxyEnabler.enableProxy(host, port, user, pwd, "");
        }

        if (component != null) {
            // charsets need to be set in shp read writer
            ComponentOrientation co = PreferencesHandler.getComponentOrientation();
            GuiUtilities.applyComponentOrientation(component, co);
        }
    }

    public static void onCloseHandleSettings() {
    }

    public static void main( String[] args ) throws Exception {
        GuiUtilities.setDefaultLookAndFeel();

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
        final SettingsController controller = new SettingsController();
        applySettings(controller);
        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine Settings");

        frame.setIconImage(ImageCache.getBuffered(ImageCache.HORTONMACHINE_FRAME_ICON));

        GuiUtilities.addClosingListener(frame, controller);

    }

}
