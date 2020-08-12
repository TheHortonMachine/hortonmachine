package org.hortonmachine.gui.utils;

import static org.hortonmachine.gears.utils.PreferencesHandler.HM_PREF_PROXYCHECK;
import static org.hortonmachine.gears.utils.PreferencesHandler.HM_PREF_PROXYHOST;
import static org.hortonmachine.gears.utils.PreferencesHandler.HM_PREF_PROXYPORT;
import static org.hortonmachine.gears.utils.PreferencesHandler.HM_PREF_PROXYUSER;

import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.ui.progress.ProgressUpdate;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.gui.utils.executor.ExecutorIndeterminateGui;
import org.hortonmachine.ssh.HMSshSession;
import org.hortonmachine.ssh.SshTunnelHandler;
import org.hortonmachine.ssh.SshUtilities;

public class SshTesterController extends SshTesterView implements IOnCloseListener {

    private SshTunnelHandler sshTunnel;

    public SshTesterController() {

        setPreferredSize(new Dimension(900, 800));

        String host = SshUtilities.getPreference(SshUtilities.HOST, "");
        String port = SshUtilities.getPreference(SshUtilities.PORT, "22");
        String user = SshUtilities.getPreference(SshUtilities.USER, "");
        String pwd = SshUtilities.getPreference(SshUtilities.PWD, "");

        _hostField.setText(host);
        _portField.setText(port);
        _userField.setText(user);
        _passwordField.setText(pwd);

        String thost = SshUtilities.getPreference(SshUtilities.TUNNELHOST, "");
        String tport_remote = SshUtilities.getPreference(SshUtilities.TUNNELPORT_REMOTE, "22");
        String tport_local = SshUtilities.getPreference(SshUtilities.TUNNELPORT_LOCAL, "22");
        String tuser = SshUtilities.getPreference(SshUtilities.TUNNELUSER, "");
        String tpwd = SshUtilities.getPreference(SshUtilities.TUNNELPWD, "");
        _remoteTunnelHostField.setText(thost);
        _tunnelPasswordField.setText(tpwd);
        _tunnelUserField.setText(tuser);
        _remoteTunnelPortField.setText(tport_remote);
        _localTunnelPortField.setText(tport_local);

        _toggleTunnelButton.addActionListener(( e ) -> {
            try {
                if (!_toggleTunnelButton.isSelected()) {
                    // disable tunnel
                    if (sshTunnel != null) {
                        sshTunnel.close();
                        sshTunnel = null;
                    }
                    _toggleTunnelButton.setText("create tunnel");
                } else {
                    // enable tunnel
                    String h = _remoteTunnelHostField.getText();
                    String u = _tunnelUserField.getText();
                    String p = _tunnelPasswordField.getText();
                    int localPort = Integer.parseInt(_localTunnelPortField.getText());
                    int remotePort = Integer.parseInt(_remoteTunnelPortField.getText());
                    sshTunnel = SshTunnelHandler.openTunnel(h, u, p, localPort, remotePort);
                    _toggleTunnelButton.setText("disconnect tunnel");
                }
            } catch (Exception e1) {
                GuiUtilities.showErrorMessage(_commandButton, e1.getMessage());
            }
        });

        String proxyCheck = PreferencesHandler.getPreference(HM_PREF_PROXYCHECK, "false");
        if (Boolean.parseBoolean(proxyCheck)) {
            String proxyHost = PreferencesHandler.getPreference(HM_PREF_PROXYHOST, "");
            String proxyPort = PreferencesHandler.getPreference(HM_PREF_PROXYPORT, "");
            String proxyUser = PreferencesHandler.getPreference(HM_PREF_PROXYUSER, "");

            _proxyLabel.setText("Using proxy: " + proxyUser + "@" + proxyHost + ":" + proxyPort);
        }
        String sshKeyCheck = SshUtilities.getPreference(SshUtilities.KEYPATH, "");
        if (sshKeyCheck.trim().length() > 0) {
            _proxyLabel.setText("Using key: " + sshKeyCheck);
        }

        _commandField.setText("ls -l");
        _commandButton.addActionListener(( e ) -> {
            final String ho = _hostField.getText().trim();
            final String po = _portField.getText().trim();
            final String u = _userField.getText().trim();
            final String p = _passwordField.getText().trim();
            final String c = _commandField.getText().trim();

            new ExecutorIndeterminateGui(){
                @Override
                public void backGroundWork() throws Exception {
                    publish(new ProgressUpdate("Executing command on remote host...", 0));
                    if (ho.length() > 0 && po.length() > 0) {
                        try (HMSshSession session = new HMSshSession(ho, Integer.parseInt(po), u, p)) {
                            String res = SshUtilities.runShellCommand(session.getSession(), c);
                            _outputArea.setText(res);
                        } catch (Exception e1) {
                            _outputArea.setText("ERROR: " + e1.getMessage());
                        }
                    }

                }
            }.execute();

        });
    }

    @Override
    public void onClose() {

        String h = _remoteTunnelHostField.getText();
        String u = _tunnelUserField.getText();
        String p = _tunnelPasswordField.getText();
        String localPort = _localTunnelPortField.getText();
        String remotePort = _remoteTunnelPortField.getText();
        SshUtilities.setPreference(SshUtilities.TUNNELHOST, h);
        SshUtilities.setPreference(SshUtilities.TUNNELPORT_REMOTE, String.valueOf(remotePort));
        SshUtilities.setPreference(SshUtilities.TUNNELPORT_LOCAL, String.valueOf(localPort));
        SshUtilities.setPreference(SshUtilities.TUNNELUSER, u);
        SshUtilities.setPreference(SshUtilities.TUNNELPWD, p);

        h = _hostField.getText().trim();
        String port = _portField.getText().trim();
        u = _userField.getText().trim();
        p = _passwordField.getText().trim();
        SshUtilities.setPreference(SshUtilities.HOST, h);
        SshUtilities.setPreference(SshUtilities.PORT, port);
        SshUtilities.setPreference(SshUtilities.USER, u);
        SshUtilities.setPreference(SshUtilities.PWD, p);

        if (sshTunnel != null) {
            try {
                sshTunnel.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            sshTunnel = null;
        }
    }

    public JComponent asJComponent() {
        return this;
    }

    @Override
    public boolean canCloseWithoutPrompt() {
        return true;
    }

    public static void main( String[] args ) {
        GuiUtilities.setDefaultLookAndFeel();

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();

        final SshTesterController controller = new SshTesterController();

        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine Ssh Utils");

        Class<SshTesterController> class1 = SshTesterController.class;
        ImageIcon icon = new ImageIcon(class1.getResource("/org/hortonmachine/images/hm150.png"));
        frame.setIconImage(icon.getImage());

        GuiUtilities.addClosingListener(frame, controller);

    }
}
