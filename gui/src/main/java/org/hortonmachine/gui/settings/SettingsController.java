package org.hortonmachine.gui.settings;

import java.awt.Component;
import static org.hortonmachine.gears.utils.PreferencesHandler.*;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.nio.charset.Charset;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.ssh.ProxyEnabler;
import org.hortonmachine.ssh.TunnelSingleton;

import com.jcraft.jsch.JSchException;

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

        String tunnelCheck = PreferencesHandler.getPreference(HM_PREF_TUNNELCHECK, "false");
        String tunnelHost = PreferencesHandler.getPreference(HM_PREF_TUNNELHOST, "");
        String tunnelUser = PreferencesHandler.getPreference(HM_PREF_TUNNELUSER, "");
        String tunnelPwd = PreferencesHandler.getPreference(HM_PREF_TUNNELPWD, "");
        String tunnelLocalPort = PreferencesHandler.getPreference(HM_PREF_TUNNELLOCALPORT, "");
        String tunnelRemotePort = PreferencesHandler.getPreference(HM_PREF_TUNNELREMOTEPORT, "");

        _sshTunnelCheckbox.setSelected(Boolean.parseBoolean(tunnelCheck));
        _tunnelHostField.setText(tunnelHost);
        _tunnelUserField.setText(tunnelUser);
        _tunnelPasswordField.setText(tunnelPwd);
        _tunnelLocalPortField.setText(tunnelLocalPort);
        _tunnelRemotePortField.setText(tunnelRemotePort);

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

    }

    private void applySettingsAndSavePreferences() throws Exception {
        String charset = _charsetTextField.getText();
        if (charset.trim().length() != 0) {
            if (!Charset.isSupported(charset)) {
                GuiUtilities.showWarningMessage(this, "Unsupported charset: " + charset);
            } else {
                PreferencesHandler.setPreference(HM_PREF_SHP_CHARSET, charset);
            }
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

        boolean tunnelSelected = _sshTunnelCheckbox.isSelected();
        if (tunnelSelected) {
            String host = _tunnelHostField.getText();
            String user = _tunnelUserField.getText();
            String pwd = _tunnelPasswordField.getText();
            String localPort = _tunnelLocalPortField.getText();
            String remotePort = _tunnelRemotePortField.getText();

            PreferencesHandler.setPreference(HM_PREF_TUNNELCHECK, "true");
            PreferencesHandler.setPreference(HM_PREF_TUNNELHOST, host);
            PreferencesHandler.setPreference(HM_PREF_TUNNELUSER, user);
            PreferencesHandler.setPreference(HM_PREF_TUNNELPWD, pwd);
            PreferencesHandler.setPreference(HM_PREF_TUNNELLOCALPORT, localPort);
            PreferencesHandler.setPreference(HM_PREF_TUNNELREMOTEPORT, remotePort);

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
            String doTunneling = PreferencesHandler.getPreference(HM_PREF_TUNNELCHECK, "false");
            if (Boolean.parseBoolean(doTunneling)) {
                String host = PreferencesHandler.getPreference(HM_PREF_TUNNELHOST, "");
                String user = PreferencesHandler.getPreference(HM_PREF_TUNNELUSER, "");
                String pwd = PreferencesHandler.getPreference(HM_PREF_TUNNELPWD, "");
                String localPort = PreferencesHandler.getPreference(HM_PREF_TUNNELLOCALPORT, "");
                String remotePort = PreferencesHandler.getPreference(HM_PREF_TUNNELREMOTEPORT, "");

                int remote = Integer.parseInt(remotePort);
                int local = Integer.parseInt(localPort);;
                try {
                    TunnelSingleton.INSTANCE.setTunnelObject(host, user, pwd, local, remote);
                } catch (JSchException e) {
                    Logger.INSTANCE.insertError("SettingsController", "An error occurred while opening tunnel", e);
                }
            }

            // charsets need to be set in shp read writer

            ComponentOrientation co = PreferencesHandler.getComponentOrientation();
            GuiUtilities.applyComponentOrientation(component, co);
        }
    }

    public static void onCloseHandleSettings() {
        TunnelSingleton.INSTANCE.disconnectTunnel();
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
