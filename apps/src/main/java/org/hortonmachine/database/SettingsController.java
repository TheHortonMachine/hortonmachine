package org.hortonmachine.database;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.ssh.ProxyEnabler;

public class SettingsController extends SettingsView {
    private static final String HM_PREF_PROXYPWD = "hm_pref_proxypwd";
    private static final String HM_PREF_PROXYUSER = "hm_pref_proxyuser";
    private static final String HM_PREF_PROXYPORT = "hm_pref_proxyport";
    private static final String HM_PREF_PROXYHOST = "hm_pref_proxyhost";
    private static final String HM_PREF_PROXYCHECK = "hm_pref_proxycheck";

    public static void openSettings() {
        SettingsController settingsController = new SettingsController();
        settingsController.fillFromPreferences();

//        GuiUtilities.openDialogWithPanel(settingsController, "Settings", new Dimension(600, 700), true);
        boolean result = GuiUtilities.openConfirmDialogWithPanel(null, settingsController, "Settings");
        if (result) {

            try {
                settingsController.applySettingsAndSavePreferences();
            } catch (Exception e) {
                if (e.getMessage().equalsIgnoreCase("numberformat")) {
                    GuiUtilities.showErrorMessage(settingsController,
                            "It seems that the port was not set properly, check your settings.  ");
                } else {
                    GuiUtilities.showErrorMessage(settingsController, "An error occurred: \n" + ExceptionUtils.getStackTrace(e));
                }
            }
        }
    }

    private void fillFromPreferences() {
        String proxyCheck = GuiUtilities.getPreference(HM_PREF_PROXYCHECK, "false");
        String proxyHost = GuiUtilities.getPreference(HM_PREF_PROXYHOST, "");
        String proxyPort = GuiUtilities.getPreference(HM_PREF_PROXYPORT, "");
        String proxyUser = GuiUtilities.getPreference(HM_PREF_PROXYUSER, "");
        String proxyPwd = GuiUtilities.getPreference(HM_PREF_PROXYPWD, "");

        _proxyCheckbox.setSelected(Boolean.parseBoolean(proxyCheck));
        _proxyHostField.setText(proxyHost);
        _proxyPortField.setText(proxyPort);
        _proxyUserField.setText(proxyUser);
        _proxyPasswordField.setText(proxyPwd);

        String tunnelCheck = GuiUtilities.getPreference("hm_pref_tunnelcheck", "false");
        String tunnelHost = GuiUtilities.getPreference("hm_pref_tunnelhost", "");
        String tunnelUser = GuiUtilities.getPreference("hm_pref_tunneluser", "");
        String tunnelPwd = GuiUtilities.getPreference("hm_pref_tunnelpwd", "");
        String tunnelLocalPort = GuiUtilities.getPreference("hm_pref_tunnellocalport", "");
        String tunnelRemotePort = GuiUtilities.getPreference("hm_pref_tunnelremoteport", "");

        _sshTunnelCheckbox.setSelected(Boolean.parseBoolean(tunnelCheck));
        _tunnelHostField.setText(tunnelHost);
        _tunnelUserField.setText(tunnelUser);
        _tunnelPasswordField.setText(tunnelPwd);
        _tunnelLocalPortField.setText(tunnelLocalPort);
        _tunnelRemotePortField.setText(tunnelRemotePort);

    }

    private void applySettingsAndSavePreferences() throws Exception {
        boolean proxySelected = _proxyCheckbox.isSelected();
        if (proxySelected) {
            String host = _proxyHostField.getText();
            String port = _proxyPortField.getText();
            String user = _proxyUserField.getText();
            String pwd = _proxyPasswordField.getText();

            GuiUtilities.setPreference(HM_PREF_PROXYCHECK, "true");
            GuiUtilities.setPreference(HM_PREF_PROXYHOST, host);
            GuiUtilities.setPreference(HM_PREF_PROXYPORT, port);
            GuiUtilities.setPreference(HM_PREF_PROXYUSER, user);
            GuiUtilities.setPreference(HM_PREF_PROXYPWD, pwd);

            ProxyEnabler.enableProxy(host, port, user, pwd, "");
        } else {
            GuiUtilities.setPreference(HM_PREF_PROXYCHECK, "false");
            ProxyEnabler.disableProxy();
        }

        boolean tunnelSelected = _sshTunnelCheckbox.isSelected();
        if (tunnelSelected) {
            String host = _tunnelHostField.getText();
            String user = _tunnelUserField.getText();
            String pwd = _tunnelPasswordField.getText();
            String localPort = _tunnelLocalPortField.getText();
            String remotePort = _tunnelRemotePortField.getText();

            GuiUtilities.setPreference("hm_pref_tunnelcheck", "true");
            GuiUtilities.setPreference("hm_pref_tunnelhost", host);
            GuiUtilities.setPreference("hm_pref_tunneluser", user);
            GuiUtilities.setPreference("hm_pref_tunnelpwd", pwd);
            GuiUtilities.setPreference("hm_pref_tunnellocalport", localPort);
            GuiUtilities.setPreference("hm_pref_tunnelremoteport", remotePort);

            int remote = Integer.parseInt(remotePort);
            int local = Integer.parseInt(localPort);;
            TunnelSingleton.INSTANCE.setTunnelObject(host, user, pwd, local, remote);
        } else {
            GuiUtilities.setPreference("hm_pref_tunnelcheck", "false");
            TunnelSingleton.INSTANCE.disconnectTunnel();
        }
    }

}
