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
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

    private static final String[] KIND_NAMES = {"Ollama", "OpenAI compatible (OpenRouter, LM Studio, vLLM, hosted)"};

    /**
     * The urls proposed for each kind: OpenRouter, LM Studio and vLLM on their default ports.
     */
    private static final List<String> OLLAMA_URLS = List.of(AssistantConfig.DEFAULT_OLLAMA_URL);
    private static final List<String> OPENAI_URLS = List.of("https://openrouter.ai/api/v1", "http://localhost:1234/v1",
            "http://localhost:8000/v1");

    private final IAssistantBackend backend;

    private final JComboBox<String> kindCombo = new JComboBox<>(KIND_NAMES);
    private final JComboBox<String> urlCombo = new JComboBox<>();
    private final JPasswordField apiKeyField = new JPasswordField(30);
    private final JTextField timeoutField = new JTextField(6);
    private final JTextField historyField = new JTextField(6);
    private final JTextField keepAliveField = new JTextField(6);
    private final JTextField contextField = new JTextField(6);
    private final JComboBox<String> modelCombo = new JComboBox<>();
    private final JCheckBox freeOnlyCheck = new JCheckBox("Only free");
    /**
     * All the models of the server, sorted; the list shows them or only the free ones.
     */
    private final List<String> modelNames = new ArrayList<>();
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
        urlCombo.setEditable(true);
        setUrlChoices(config.baseUrl());
        urlCombo.setToolTipText("<html>The address of the server. The list proposes the usual ones for the chosen server kind;<br>"
                + "switching the kind replaces a proposed url, a url typed by hand is kept.</html>");
        apiKeyField.setText(config.apiKey() == null ? "" : config.apiKey());
        apiKeyField.setToolTipText("Leave empty for a local Ollama. Only for servers that need one (behind a proxy, hosted); "
                + "it is saved in plain text in the user preferences.");
        timeoutField.setText(String.valueOf(config.timeoutSeconds()));
        historyField.setText(String.valueOf(config.historyTurns()));
        keepAliveField.setText(String.valueOf(config.keepAliveMinutes()));
        keepAliveField.setToolTipText("<html>Ollama only. How long the model stays loaded after the last request. While it is loaded,<br>"
                + "Ollama also remembers what it read, and the next request is much faster.</html>");
        contextField.setText(String.valueOf(config.contextLength()));
        contextField.setToolTipText("<html>Ollama only. The context of the model, in tokens: request and answer must fit in it,<br>"
                + "or the request is cut. The script assistant needs about 12000; more uses more memory.</html>");
        kindCombo.addActionListener(e -> {
            String url = getUrl();
            boolean proposed = url.isEmpty() || OLLAMA_URLS.contains(url) || OPENAI_URLS.contains(url);
            setUrlChoices(proposed ? urlChoices().get(0) : url);
            updateOllamaFields();
        });
        updateOllamaFields();
        historyField.setToolTipText("Earlier questions and answers sent again with each request. 0 = no memory.");
        JLabel historyHelp = new JLabel("<html><body style='width: 380px'>How many earlier questions and answers of the discussion are sent again "
                + "with each request, so that follow-ups like \"now only the larger ones\" are understood. "
                + "More turns: better follow-ups, but longer and slower requests that use more of the model's "
                + "context (for small local models 4 to 8 is a good range). 0: every request stands alone.</html>");
        historyHelp.setForeground(new Color(110, 110, 110));
        modelCombo.setEditable(true);
        modelCombo.setMaximumRowCount(25);
        // the width of a sample name, not of the longest model: hosted services have very long ones
        modelCombo.setPrototypeDisplayValue("provider/a-model-name-of-usual-length:free");
        freeOnlyCheck.setEnabled(false);
        freeOnlyCheck.setToolTipText("<html>List only the models the server offers for free (OpenRouter: names ending in \":free\").<br>"
                + "They have low daily limits and may be slow or busy.</html>");
        freeOnlyCheck.addActionListener(e -> fillModels());
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
                "P,3DLU,P,3DLU,P,3DLU,P,3DLU,P,3DLU,P,3DLU,P,1DLU,P,3DLU,P,6DLU,FILL:P:GROW,3DLU,P,9DLU,P,3DLU,P,3DLU,P");
        setLayout(layout);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        CellConstraints cc = new CellConstraints();
        add(new JLabel("Server"), cc.xy(1, 1));
        add(kindCombo, cc.xyw(3, 1, 3));
        add(new JLabel("URL"), cc.xy(1, 3));
        add(urlCombo, cc.xyw(3, 3, 3));
        add(new JLabel("API key"), cc.xy(1, 5));
        add(apiKeyField, cc.xyw(3, 5, 3));
        add(new JLabel("Timeout (s)"), cc.xy(1, 7));
        add(timeoutField, cc.xy(3, 7, CellConstraints.LEFT, CellConstraints.DEFAULT));
        add(new JLabel("Keep model loaded (min)"), cc.xy(1, 9));
        add(keepAliveField, cc.xy(3, 9, CellConstraints.LEFT, CellConstraints.DEFAULT));
        add(new JLabel("Context (tokens)"), cc.xy(1, 11));
        add(contextField, cc.xy(3, 11, CellConstraints.LEFT, CellConstraints.DEFAULT));
        add(new JLabel("Discussion memory"), cc.xy(1, 13));
        add(historyField, cc.xy(3, 13, CellConstraints.LEFT, CellConstraints.DEFAULT));
        add(historyHelp, cc.xyw(3, 15, 3));
        add(new JLabel("Model"), cc.xy(1, 17));
        JPanel modelPanel = new JPanel(new BorderLayout(6, 0));
        modelPanel.add(modelCombo, BorderLayout.CENTER);
        modelPanel.add(freeOnlyCheck, BorderLayout.EAST);
        add(modelPanel, cc.xy(3, 17));
        add(refreshModelsButton, cc.xy(5, 17));
        add(new JScrollPane(statusArea), cc.xyw(1, 19, 3));
        add(testButton, cc.xy(5, 19, CellConstraints.FILL, CellConstraints.TOP));
        add(remoteLabel, cc.xyw(1, 21, 5));
        add(new JLabel("Download model"), cc.xy(1, 23));
        add(pullModelField, cc.xy(3, 23));
        JPanel pullButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pullButtons.add(pullButton);
        pullButtons.add(cancelPullButton);
        add(pullButtons, cc.xy(5, 23));
        add(pullProgress, cc.xyw(3, 25, 3));
        add(pullLabel, cc.xyw(3, 27, 3));

        testConnection();
    }

    private AssistantConfig readConfig() {
        ServerKind kind = kindCombo.getSelectedIndex() == 0 ? ServerKind.OLLAMA : ServerKind.OPENAI_COMPATIBLE;
        String url = getUrl();
        if (url.isEmpty()) {
            url = urlChoices().get(0);
            urlCombo.getEditor().setItem(url);
        }
        Object selectedModel = modelCombo.getEditor().getItem();
        String model = selectedModel == null ? null : selectedModel.toString().trim();
        String apiKey = new String(apiKeyField.getPassword()).trim();
        int timeout = readInt(timeoutField, AssistantConfig.DEFAULT_TIMEOUT_SECONDS);
        int historyTurns = Math.max(0, readInt(historyField, AssistantConfig.DEFAULT_HISTORY_TURNS));
        int keepAlive = Math.max(0, readInt(keepAliveField, AssistantConfig.DEFAULT_KEEP_ALIVE_MINUTES));
        int contextLength = readInt(contextField, AssistantConfig.DEFAULT_CONTEXT_LENGTH);
        return new AssistantConfig(kind, url, model == null || model.isEmpty() ? null : model,
                apiKey.isEmpty() ? null : apiKey, timeout, historyTurns, keepAlive, contextLength);
    }

    /**
     * OpenRouter offers some models for free (with low daily limits) under a name ending in ":free".
     */
    private static boolean isFree( String modelName ) {
        return modelName.endsWith(":free");
    }

    /**
     * Fill the model list with the models of the server, only the free ones if asked, keeping
     * the model in the editor.
     */
    private void fillModels() {
        Object current = modelCombo.getEditor().getItem();
        boolean freeOnly = freeOnlyCheck.isEnabled() && freeOnlyCheck.isSelected();
        modelCombo.removeAllItems();
        for( String name : modelNames ) {
            if (!freeOnly || isFree(name)) {
                modelCombo.addItem(name);
            }
        }
        modelCombo.getEditor().setItem(current);
    }

    /**
     * @return the urls proposed for the selected server kind.
     */
    private List<String> urlChoices() {
        return kindCombo.getSelectedIndex() == 0 ? OLLAMA_URLS : OPENAI_URLS;
    }

    /**
     * Propose the urls of the selected server kind and show the given url.
     */
    private void setUrlChoices( String url ) {
        urlCombo.removeAllItems();
        for( String choice : urlChoices() ) {
            urlCombo.addItem(choice);
        }
        urlCombo.getEditor().setItem(url == null ? "" : url);
    }

    /**
     * @return the url in the editor of the combo, also when typed and not yet confirmed.
     */
    private String getUrl() {
        Object item = urlCombo.getEditor().getItem();
        return item == null ? "" : item.toString().trim();
    }

    /**
     * Keep alive and context are sent only to Ollama servers.
     */
    private void updateOllamaFields() {
        boolean ollama = kindCombo.getSelectedIndex() == 0;
        keepAliveField.setEnabled(ollama);
        contextField.setEnabled(ollama);
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
                        models = new ArrayList<>(backend.listModels());
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
                    // wrapped, since a long line would widen the panel and push the buttons out of it
                    remoteLabel.setText("<html><body style='width: 480px'>Remote server: questions, scripts, variable names, "
                            + "database schema and any sample rows are sent to " + backend.getConfig().baseUrl() + "</body></html>");
                }

                if (status.reachable()) {
                    String current = backend.getConfig().model();
                    // hosted services list hundreds of models, in no useful order
                    models.sort(Comparator.comparing(ModelInfo::name, String.CASE_INSENSITIVE_ORDER));
                    modelNames.clear();
                    for( ModelInfo model : models ) {
                        modelNames.add(model.name());
                    }
                    freeOnlyCheck.setEnabled(modelNames.stream().anyMatch(AssistantSettingsPanel::isFree));
                    fillModels();
                    if (current != null) {
                        modelCombo.setSelectedItem(current);
                    } else if (modelCombo.getItemCount() > 0) {
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
