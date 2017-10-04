package org.hortonmachine.database;

import java.awt.Dimension;
import java.awt.Window;
import java.io.File;

import javax.swing.DefaultComboBoxModel;

import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.libs.modules.HMConstants;
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

    public NewDbController( Window parent, GuiBridgeHandler guiBridge, boolean doOpen, String remoteJdbcUrl ) {
        parentWindow = parent;
        this.guiBridge = guiBridge;
        this.doOpen = doOpen;
        this.remoteJdbcUrl = remoteJdbcUrl;
        if (remoteJdbcUrl != null) {
            isRemote = true;
        }
        setPreferredSize(new Dimension(800, 200));
        init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        _browseButton.addActionListener(e -> {
            File[] files;
            if (doOpen) {
                files = guiBridge.showOpenFileDialog("Select database to open", GuiUtilities.getLastFile(),
                        HMConstants.dbFileFilter);
            } else {
                files = guiBridge.showSaveFileDialog("Select database to create", GuiUtilities.getLastFile(),
                        HMConstants.dbFileFilter);
            }
            if (files != null && files.length > 0) {
                String absolutePath = files[0].getAbsolutePath();
                GuiUtilities.setLastPath(absolutePath);
                _dbTextField.setText(absolutePath);

                for( EDb edb : EDb.getSpatialTypesDesktop() ) {
                    if (absolutePath.endsWith(edb.getExtension())) {
                        _dbTypeCombo.setSelectedItem(edb);
                        break;
                    }
                }

            }
        });

        _cancelButton.addActionListener(e -> {
            parentWindow.dispose();
        });

        _connectButton.addActionListener(e -> {
            isOk = true;

            parentWindow.dispose();
        });

        _userTextField.setText("sa");
        _pwdTextField.setText("");

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

        if (remoteJdbcUrl != null) {
            _dbTextField.setText(remoteJdbcUrl);
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

}
