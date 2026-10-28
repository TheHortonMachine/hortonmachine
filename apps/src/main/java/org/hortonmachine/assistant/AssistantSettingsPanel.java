/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.assistant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.assistant.AssistantConfig.ServerKind;
import org.hortonmachine.gears.assistant.BackendStatus;
import org.hortonmachine.gears.assistant.IAssistantBackend;
import org.hortonmachine.gears.assistant.ModelInfo;
import org.hortonmachine.gears.libs.exceptions.ModelsUserCancelException;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The settings of the assistant: server, model, connection test and model download.
 *
 * <p>It uses only the contracts of {@link IAssistantBackend}: the edited values are applied (and
 * saved) with {@link IAssistantBackend#setConfig(AssistantConfig)} before testing them.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class AssistantSettingsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final String[] KIND_NAMES = {"Ollama", "OpenAI compatible (LM Studio, vLLM, hosted)"};

    private final IAssistantBackend backend;

    private final JComboBox<String> kindCombo = new JComboBox<>(KIND_NAMES);
    private final JTextField urlField = new JTextField(30);
    private final JPasswordField apiKeyField = new JPasswordField(30);
    private final JTextField timeoutField = new JTextField(6);
    private final JTextField historyField = new JTextField(6);
    private final JComboBox<String> modelCombo = new JComboBox<>();
    private final JTextArea statusArea = new JTextArea(4, 40);
    private final JLabel remoteLabel = new JLabel();
    private final JTextField pullModelField = new JTextField(20);
    private final JButton pullButton = new JButton("Download");
    private final JButton cancelPullButton = new JButton("Cancel");
    private final JProgressBar pullProgress = new JProgressBar();
    private final JLabel pullLabel = new JLabel(" ");
    private AssistantProgressMonitor pullMonitor;

    /**
     * @param backend the backend to configure.
     */
    public AssistantSettingsPanel( IAssistantBackend backend ) {
        this.backend = backend;

        AssistantConfig config = backend.getConfig();
        kindCombo.setSelectedIndex(config.serverKind() == ServerKind.OLLAMA ? 0 : 1);
        urlField.setText(config.baseUrl());
        apiKeyField.setText(config.apiKey() == null ? "" : config.apiKey());
        apiKeyField.setToolTipText("Leave empty for a local Ollama. Only for servers that need one (behind a proxy, hosted); "
                + "it is saved in plain text in the user preferences.");
        timeoutField.setText(String.valueOf(config.timeoutSeconds()));
        historyField.setText(String.valueOf(config.historyTurns()));
        historyField.setToolTipText("Earlier questions and answers sent again with each request. 0 = no memory.");
        JLabel historyHelp = new JLabel("<html><body style='width: 380px'>How many earlier questions and answers of the discussion are sent again "
                + "with each request, so that follow-ups like \"now only the larger ones\" are understood. "
                + "More turns: better follow-ups, but longer and slower requests that use more of the model's "
                + "context (for small local models 4 to 8 is a good range). 0: every request stands alone.</html>");
        historyHelp.setForeground(new Color(110, 110, 110));
        modelCombo.setEditable(true);
        if (config.model() != null) {
            modelCombo.addItem(config.model());
            modelCombo.setSelectedItem(config.model());
        }
        pullModelField.setText(backend.getRecommendedModel());

        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        remoteLabel.setForeground(new Color(180, 90, 0));
        cancelPullButton.setEnabled(false);
        pullButton.setEnabled(false);
        // the download status is written in the bar too, so it is visible whatever the window size
        pullProgress.setStringPainted(true);
        pullProgress.setString("");

        JButton testButton = new JButton("Apply and test");
        testButton.addActionListener(e -> testConnection());
        JButton refreshModelsButton = new JButton("Refresh");
        refreshModelsButton.addActionListener(e -> testConnection());
        pullButton.addActionListener(e -> pullModel());
        cancelPullButton.addActionListener(e -> {
            if (pullMonitor != null) {
                pullMonitor.setCanceled(true);
            }
        });

        FormLayout layout = new FormLayout("RIGHT:DEFAULT:NONE,4DLU,FILL:DEFAULT:GROW,4DLU,FILL:DEFAULT:NONE",
                "P,3DLU,P,3DLU,P,3DLU,P,3DLU,P,1DLU,P,3DLU,P,6DLU,FILL:P:GROW,3DLU,P,9DLU,P,3DLU,P,3DLU,P");
        setLayout(layout);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        CellConstraints cc = new CellConstraints();
        add(new JLabel("Server"), cc.xy(1, 1));
        add(kindCombo, cc.xyw(3, 1, 3));
        add(new JLabel("URL"), cc.xy(1, 3));
        add(urlField, cc.xyw(3, 3, 3));
        add(new JLabel("API key"), cc.xy(1, 5));
        add(apiKeyField, cc.xyw(3, 5, 3));
        add(new JLabel("Timeout (s)"), cc.xy(1, 7));
        add(timeoutField, cc.xy(3, 7, CellConstraints.LEFT, CellConstraints.DEFAULT));
        add(new JLabel("Discussion memory"), cc.xy(1, 9));
        add(historyField, cc.xy(3, 9, CellConstraints.LEFT, CellConstraints.DEFAULT));
        add(historyHelp, cc.xyw(3, 11, 3));
        add(new JLabel("Model"), cc.xy(1, 13));
        add(modelCombo, cc.xy(3, 13));
        add(refreshModelsButton, cc.xy(5, 13));
        add(new JScrollPane(statusArea), cc.xyw(1, 15, 3));
        add(testButton, cc.xy(5, 15, CellConstraints.FILL, CellConstraints.TOP));
        add(remoteLabel, cc.xyw(1, 17, 5));
        add(new JLabel("Download model"), cc.xy(1, 19));
        add(pullModelField, cc.xy(3, 19));
        JPanel pullButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pullButtons.add(pullButton);
        pullButtons.add(cancelPullButton);
        add(pullButtons, cc.xy(5, 19));
        add(pullProgress, cc.xyw(3, 21, 3));
        add(pullLabel, cc.xyw(3, 23, 3));

        testConnection();
    }

    private AssistantConfig readConfig() {
        ServerKind kind = kindCombo.getSelectedIndex() == 0 ? ServerKind.OLLAMA : ServerKind.OPENAI_COMPATIBLE;
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            url = AssistantConfig.DEFAULT_OLLAMA_URL;
            urlField.setText(url);
        }
        Object selectedModel = modelCombo.getEditor().getItem();
        String model = selectedModel == null ? null : selectedModel.toString().trim();
        String apiKey = new String(apiKeyField.getPassword()).trim();
        int timeout = readInt(timeoutField, AssistantConfig.DEFAULT_TIMEOUT_SECONDS);
        int historyTurns = Math.max(0, readInt(historyField, AssistantConfig.DEFAULT_HISTORY_TURNS));
        return new AssistantConfig(kind, url, model == null || model.isEmpty() ? null : model,
                apiKey.isEmpty() ? null : apiKey, timeout, historyTurns);
    }

    /**
     * @return the number in the field, or the default, also written back, if it isn't one.
     */
    private static int readInt( JTextField field, int defaultValue ) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            field.setText(String.valueOf(defaultValue));
            return defaultValue;
        }
    }

    /**
     * Apply the edited values, then check the server and list its models.
     */
    private void testConnection() {
        backend.setConfig(readConfig());
        statusArea.setText("Checking the server...");
        pullButton.setEnabled(false);
        new SwingWorker<Void, Void>(){
            BackendStatus status;
            List<ModelInfo> models = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                status = backend.checkStatus();
                if (status.reachable()) {
                    try {
                        models = backend.listModels();
                    } catch (Exception e) {
                        Logger.INSTANCE.insertError("AssistantSettingsPanel", "Error listing the models", e);
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                StringBuilder sb = new StringBuilder();
                if (status.reachable()) {
                    sb.append("Connected to ").append(backend.getName());
                    if (status.version() != null) {
                        sb.append(" ").append(status.version());
                    }
                    sb.append(".\n");
                }
                sb.append(status.message());
                statusArea.setText(sb.toString());
                statusArea.setCaretPosition(0);

                if (status.local()) {
                    remoteLabel.setText(" ");
                } else {
                    remoteLabel.setText("Remote server: questions, database schema and any sample rows are sent to "
                            + backend.getConfig().baseUrl());
                }

                if (status.reachable()) {
                    String current = backend.getConfig().model();
                    modelCombo.removeAllItems();
                    for( ModelInfo model : models ) {
                        modelCombo.addItem(model.name());
                    }
                    if (current != null) {
                        modelCombo.setSelectedItem(current);
                    } else if (!models.isEmpty()) {
                        modelCombo.setSelectedIndex(0);
                        backend.setConfig(readConfig());
                    }
                }
                pullButton.setEnabled(status.reachable() && backend.canPullModels());
            }
        }.execute();
    }

    private void pullModel() {
        String model = pullModelField.getText().trim();
        if (model.isEmpty()) {
            return;
        }
        backend.setConfig(readConfig());
        pullMonitor = new AssistantProgressMonitor(pullProgress, pullLabel);
        AssistantProgressMonitor monitor = pullMonitor;
        pullButton.setEnabled(false);
        cancelPullButton.setEnabled(true);
        pullLabel.setText("Starting the download of " + model + "...");
        new SwingWorker<Void, Void>(){
            Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    backend.pullModel(model, monitor);
                } catch (Exception e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                cancelPullButton.setEnabled(false);
                pullButton.setEnabled(true);
                if (error == null) {
                    pullLabel.setText("Downloaded " + model + ".");
                    modelCombo.getEditor().setItem(model);
                    testConnection();
                } else if (error instanceof ModelsUserCancelException) {
                    pullLabel.setText("Download canceled.");
                } else {
                    Logger.INSTANCE.insertError("AssistantSettingsPanel", "Error downloading " + model, error);
                    pullLabel.setText("Download failed: " + error.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Stop a running download, if any.
     */
    public void cancelDownload() {
        if (pullMonitor != null) {
            pullMonitor.setCanceled(true);
        }
    }

    /**
     * Show the settings in a modal dialog.
     *
     * @param parent the component to show the dialog over.
     * @param backend the backend to configure.
     */
    public static void showDialog( Component parent, IAssistantBackend backend ) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Assistant settings", JDialog.ModalityType.APPLICATION_MODAL);
        AssistantSettingsPanel panel = new AssistantSettingsPanel(backend);
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
            backend.setConfig(panel.readConfig());
            panel.cancelDownload();
            dialog.dispose();
        });
        dialog.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing( WindowEvent e ) {
                backend.setConfig(panel.readConfig());
                panel.cancelDownload();
            }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(closeButton);
        dialog.getContentPane().add(panel, BorderLayout.CENTER);
        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        // some room for longer status messages; never smaller than the packed size
        Dimension packed = dialog.getSize();
        dialog.setMinimumSize(packed);
        dialog.setSize(packed.width, packed.height + 80);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
