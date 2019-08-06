package org.hortonmachine.database;

import java.awt.Dimension;
import java.awt.Window;
import java.io.File;

import javax.swing.DefaultComboBoxModel;

import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gui.utils.GuiBridgeHandler;
import org.hortonmachine.gui.utils.GuiUtilities;

public class NewDbController extends NewDbView {
    private static final long serialVersionUID = 1L;
    private GuiBridgeHandler guiBridge;
    private boolean doOpen;
    private Window parentWindow;

    private boolean isOk = false;
    private String remoteJdbcUrl;
    private boolean isRemote = false;
    private String user;
    private String pwd;
    private String localPath;
    private boolean allowRemoteConnectionForLocal;

    public NewDbController( Window parent, GuiBridgeHandler guiBridge, boolean doOpen, String localPath, String remoteJdbcUrl,
            String user, String pwd, boolean allowRemoteConnectionForLocal ) {
        parentWindow = parent;
        this.guiBridge = guiBridge;
        this.doOpen = doOpen;
        this.localPath = localPath;
        this.remoteJdbcUrl = remoteJdbcUrl;
        this.allowRemoteConnectionForLocal = allowRemoteConnectionForLocal;

        if (user == null) {
            user = "sa";
        }
        if (pwd == null) {
            pwd = "";
        }
        this.user = user;
        this.pwd = pwd;

        _connectRemoteCheck.setVisible(false);

        if (remoteJdbcUrl != null) {
            isRemote = true;
        }
        setPreferredSize(new Dimension(800, 250));
        init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        _browseButton.addActionListener(e -> {
            File[] files;
            if (doOpen) {
                files = guiBridge.showOpenFileDialog("Select database to open", PreferencesHandler.getLastFile(),
                        HMConstants.dbFileFilter);
            } else {
                files = guiBridge.showSaveFileDialog("Select database to create", PreferencesHandler.getLastFile(),
                        HMConstants.dbFileFilter);
            }
            if (files != null && files.length > 0) {
                String absolutePath = files[0].getAbsolutePath();
                PreferencesHandler.setLastPath(absolutePath);
                _dbTextField.setText(absolutePath);

                checkDbType(absolutePath);

            }
        });

        _cancelButton.addActionListener(e -> {
            parentWindow.dispose();
        });

        _connectButton.addActionListener(e -> {
            isOk = true;

            parentWindow.dispose();
        });

        _userTextField.setText(user);
        _pwdTextField.setText(pwd);

        _dbTypeCombo.setModel(new DefaultComboBoxModel<>(EDb.getSpatialTypesDesktop()));
        _dbTypeCombo.addActionListener(e -> {
            EDb selectedItem = (EDb) _dbTypeCombo.getSelectedItem();
            _userTextField.setEnabled(selectedItem.supportsPwd());
            _pwdTextField.setEnabled(selectedItem.supportsPwd());
            _extTextField.setText(selectedItem.getExtension());
        });

        _userTextField.setEnabled(false);
        _pwdTextField.setEnabled(false);
        _extTextField.setEditable(false);
        _extTextField.setText(((EDb) _dbTypeCombo.getSelectedItem()).getExtension());

        if (remoteJdbcUrl != null && remoteJdbcUrl.trim().length() != 0) {
            _dbTextField.setText(remoteJdbcUrl);
        } else if (localPath != null) {
            _dbTextField.setText(localPath);
            checkDbType(localPath);
        }

        if (isRemote) {
            _dbTypeCombo.setSelectedItem(EDb.H2GIS);
            _dbLabel.setText("JDBC Url");
            _browseButton.setVisible(false);
            _dbTypeCombo.setVisible(false);
            _dbTypeLabel.setVisible(false);
            _extLabel.setVisible(false);
            _extTextField.setVisible(false);
        }
    }

    private void checkDbType( String absolutePath ) {
        for( EDb edb : EDb.getSpatialTypesDesktop() ) {
            if (absolutePath.endsWith(edb.getExtension())) {
                _dbTypeCombo.setSelectedItem(edb);

                if (edb.supportsServerMode() && allowRemoteConnectionForLocal) {
                    _connectRemoteCheck.setVisible(true);
                }
                break;
            }
        }
    }

    public boolean isOk() {
        return isOk;
    }

    public String getDbPath() {
        return _dbTextField.getText().trim();
    }

    public String getDbUser() {
        return _userTextField.getText().trim();
    }

    public String getDbPwd() {
        return new String(_pwdTextField.getPassword());
    }

    public EDb getDbType() {
        EDb edb = (EDb) _dbTypeCombo.getSelectedItem();
        return edb;
    }

    public boolean connectInRemote() {
        return _connectRemoteCheck.isSelected();
    }

}
