package org.hortonmachine.gui.utils;

import static org.hortonmachine.gears.utils.PreferencesHandler.HM_PREF_PROXYCHECK;
import static org.hortonmachine.gears.utils.PreferencesHandler.HM_PREF_PROXYHOST;

import java.awt.Dimension;

import static org.hortonmachine.gears.utils.PreferencesHandler.*;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.hortonmachine.gears.ui.progress.ProgressUpdate;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.gui.utils.executor.ExecutorIndeterminateGui;
import org.hortonmachine.ssh.HMSshSession;
import org.hortonmachine.ssh.SshUtilities;

public class SshTesterController extends SshTesterView implements IOnCloseListener {

    public SshTesterController() {

        setPreferredSize(new Dimension(900, 800));

        String host = SshUtilities.getPreference(SshUtilities.HOST, "");
        String user = SshUtilities.getPreference(SshUtilities.USER, "");
        String pwd = SshUtilities.getPreference(SshUtilities.PWD, "");

        _hostField.setText(host);
        _userField.setText(user);
        _passwordField.setText(pwd);
        
        String proxyCheck = PreferencesHandler.getPreference(HM_PREF_PROXYCHECK, "false");
        if (Boolean.parseBoolean(proxyCheck)) {
            String proxyHost = PreferencesHandler.getPreference(HM_PREF_PROXYHOST, "");
            String proxyPort = PreferencesHandler.getPreference(HM_PREF_PROXYPORT, "");
            String proxyUser = PreferencesHandler.getPreference(HM_PREF_PROXYUSER, "");

            _proxyLabel.setText("Using proxy: " + proxyUser + "@" + proxyHost + ":" + proxyPort);
        }

        _commandField.setText("ls -l");
        _commandButton.addActionListener(( e ) -> {
            final String ho = _hostField.getText().trim();
            final String u = _userField.getText().trim();
            final String p = _passwordField.getText().trim();
            final String c = _commandField.getText().trim();

            new ExecutorIndeterminateGui(){
                @Override
                public void backGroundWork() throws Exception {
                    publish(new ProgressUpdate("Executing command on remote host...", 0));
                    if (ho.length() > 0 && u.length() > 0 && p.length() > 0) {
                        try (HMSshSession session = new HMSshSession(ho, u, p)) {
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

        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine Ssh Tester");

        Class<SshTesterController> class1 = SshTesterController.class;
        ImageIcon icon = new ImageIcon(class1.getResource("/org/hortonmachine/images/hm150.png"));
        frame.setIconImage(icon.getImage());

        GuiUtilities.addClosingListener(frame, controller);

    }
}
