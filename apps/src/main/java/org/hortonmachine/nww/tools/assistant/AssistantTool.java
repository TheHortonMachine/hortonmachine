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
package org.hortonmachine.nww.tools.assistant;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.hortonmachine.gears.assistant.IAssistantBackend;
import org.hortonmachine.gears.assistant.IScriptAssistant;
import org.hortonmachine.gears.assistant.ScriptContext;
import org.hortonmachine.gears.io.osm.OverpassClient;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.geoscript.console.assistant.ConsoleAssistantContext;
import org.hortonmachine.geoscript.console.assistant.IScriptAssistantHost;
import org.hortonmachine.geoscript.console.assistant.ScriptAssistantPanel;
import org.hortonmachine.gui.editor.CodeEditorFactory;
import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.nww.tools.INwwTool;
import org.hortonmachine.nww.tools.NwwContext;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.transform.ThreadInterrupt;

/**
 * The script assistant of the viewer: it writes scripts that fetch, make and show data, which
 * the user puts in the editor of the tab and runs. The scripts get the map as
 * <code>viewer</code> and the OpenStreetMap services as <code>osm</code>.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class AssistantTool implements INwwTool {

    private static final String PREF_OVERPASS_SERVER = "NWW_ASSISTANT_OVERPASS_SERVER";
    private static final String SCRIPT_NAME = "ViewerScript";
    /** The most frames of other classes shown per exception. */
    private static final int MAX_OTHER_FRAMES = 6;
    /** The longest result shown after a run. */
    private static final int MAX_RESULT_LENGTH = 500;

    /** The Overpass servers to choose from, by label. */
    private static final Map<String, String> OVERPASS_SERVERS = new LinkedHashMap<>();
    static {
        OVERPASS_SERVERS.put("overpass-api.de", OverpassClient.DEFAULT_URL);
        OVERPASS_SERVERS.put("kumi.systems (mirror)", "https://overpass.kumi.systems/api/interpreter");
        OVERPASS_SERVERS.put("private.coffee (mirror)", "https://overpass.private.coffee/api/interpreter");
    }

    private final IAssistantBackend backend;
    private final IScriptAssistant scriptAssistant;

    private ScriptAssistantPanel assistantPanel;
    private RSyntaxTextArea editor;
    private final JTextArea output = new JTextArea();
    private final JButton runButton = new JButton("Run");
    private final JButton stopButton = new JButton("Stop");
    private final JComboBox<String> serverCombo = new JComboBox<>(OVERPASS_SERVERS.keySet().toArray(new String[0]));

    private final Binding binding = new Binding();
    private GroovyShell shell;
    private int runCounter = 0;
    private volatile Thread runThread;
    /** The text of the last run, if it failed, and its error, for the fix of the assistant. */
    private volatile String lastFailedScript;
    private volatile String lastError;

    /**
     * @param backend the assistant backend, <code>null</code> if no assistant module is
     *          installed, which makes the tool not available.
     * @param scriptAssistant the script assistant of the backend, or <code>null</code>.
     */
    public AssistantTool( IAssistantBackend backend, IScriptAssistant scriptAssistant ) {
        this.backend = backend;
        this.scriptAssistant = scriptAssistant;
    }

    @Override
    public String getName() {
        return "Assistant";
    }

    @Override
    public ImageIcon getIcon() {
        return ImageCache.get(ImageCache.NOTE);
    }

    @Override
    public boolean isAvailable() {
        return backend != null && scriptAssistant != null;
    }

    @Override
    public String getUnavailableReason() {
        return isAvailable() ? null : "No assistant module (hm-llm) is installed";
    }

    @Override
    public JComponent createPanel( NwwContext context ) {
        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(new ASTTransformationCustomizer(ThreadInterrupt.class));
        shell = new GroovyShell(AssistantTool.class.getClassLoader(), binding, config);
        binding.setVariable(ViewerScripts.VIEWER_VARIABLE, new ViewerApi(context));
        binding.setVariable(ViewerScripts.OSM_VARIABLE, new OsmApi(this::overpassUrl));
        // println of the scripts
        binding.setVariable("out", new java.io.PrintWriter(new OutputWriter(), true));

        editor = CodeEditorFactory.createEditor(SyntaxConstants.SYNTAX_STYLE_GROOVY);
        editor.setRows(10);
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "hm-run");
        editor.getActionMap().put("hm-run", new AbstractAction(){
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed( ActionEvent e ) {
                run();
            }
        });

        assistantPanel = new ScriptAssistantPanel(backend, scriptAssistant, new Host());

        runButton.setIcon(ImageCache.get(ImageCache.RUN_SCRIPT));
        runButton.setToolTipText("Run the script of the editor (Ctrl+Enter): only you run scripts, never the assistant");
        runButton.addActionListener(e -> run());
        stopButton.setIcon(ImageCache.get(ImageCache.STOP));
        stopButton.setToolTipText("Stop the running script");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stop());
        JButton clearButton = new JButton(ImageCache.get(ImageCache.TRASH));
        clearButton.setToolTipText("Clear the output");
        clearButton.addActionListener(e -> output.setText(""));
        String lastServer = PreferencesHandler.getPreference(PREF_OVERPASS_SERVER, "");
        if (OVERPASS_SERVERS.containsKey(lastServer)) {
            serverCombo.setSelectedItem(lastServer);
        }
        serverCombo.setToolTipText("<html>The Overpass server of the OpenStreetMap queries:<br>"
                + "when one is busy, retry later or choose another</html>");
        serverCombo.addActionListener(e -> PreferencesHandler.setPreference(PREF_OVERPASS_SERVER, (String) serverCombo.getSelectedItem()));
        serverCombo.setMaximumSize(serverCombo.getPreferredSize());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(runButton);
        toolBar.add(stopButton);
        toolBar.add(clearButton);
        toolBar.addSeparator();
        toolBar.add(new JLabel("Overpass "));
        toolBar.add(serverCombo);

        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        output.setRows(6);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, editor.getFont().getSize()));
        JScrollPane outputScroll = new JScrollPane(output);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Output"));

        JSplitPane editorOutputSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, CodeEditorFactory.createScrollPane(editor),
                outputScroll);
        editorOutputSplit.setResizeWeight(0.6);
        editorOutputSplit.setBorder(null);
        JPanel runPanel = new JPanel(new BorderLayout());
        runPanel.add(toolBar, BorderLayout.NORTH);
        runPanel.add(editorOutputSplit, BorderLayout.CENTER);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, assistantPanel, runPanel);
        mainSplit.setResizeWeight(0.55);
        mainSplit.setContinuousLayout(true);
        mainSplit.setBorder(null);
        return mainSplit;
    }

    private String overpassUrl() {
        return OVERPASS_SERVERS.get((String) serverCombo.getSelectedItem());
    }

    @Override
    public void onClose() {
        if (assistantPanel != null) {
            assistantPanel.cancel();
        }
        stop();
    }

    private void run() {
        if (runThread != null) {
            appendOutput("A script is already running: wait or stop it.\n");
            return;
        }
        String script = editor.getText();
        if (script.isBlank()) {
            return;
        }
        lastFailedScript = null;
        lastError = null;
        String scriptName = SCRIPT_NAME + runCounter++ + ".groovy";
        appendOutput((output.getDocument().getLength() > 0 ? "\n" : "") + "> running\n");
        setRunning(true);
        long start = System.currentTimeMillis();
        Thread thread = new Thread(() -> {
            try {
                Object result = shell.evaluate(script, scriptName);
                long elapsed = System.currentTimeMillis() - start;
                String resultText = result == null ? "" : shorten(String.valueOf(result)) + "\n";
                appendOutput(resultText + "Done in " + elapsed / 1000.0 + " s.\n");
            } catch (Throwable t) {
                String error = errorText(t, scriptName);
                if (error == null) {
                    appendOutput("Script stopped.\n");
                } else {
                    lastFailedScript = script;
                    lastError = error;
                    appendOutput(error + "\n");
                }
            } finally {
                runThread = null;
                SwingUtilities.invokeLater(() -> setRunning(false));
            }
        }, "hm-viewer-script");
        thread.setDaemon(true);
        runThread = thread;
        thread.start();
    }

    private void stop() {
        Thread thread = runThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void setRunning( boolean running ) {
        runButton.setEnabled(!running);
        stopButton.setEnabled(running);
    }

    /**
     * @return the error of a run with the lines of the script and a few frames of the other
     *          classes, or <code>null</code> if the script was stopped.
     */
    private static String errorText( Throwable t, String scriptName ) {
        if (t instanceof MultipleCompilationErrorsException) {
            return t.getMessage().replace("startup failed:", "").replace(scriptName + ": ", "").trim();
        }
        for( Throwable cause = t; cause != null; cause = cause.getCause() != cause ? cause.getCause() : null ) {
            if (cause instanceof InterruptedException) {
                return null;
            }
        }
        Throwable current = StackTraceUtils.deepSanitize(t);
        StringBuilder error = new StringBuilder();
        for( int depth = 0; current != null && depth < 10; depth++ ) {
            error.append(depth == 0 ? "" : "Caused by: ").append(current).append("\n");
            int printed = 0;
            for( StackTraceElement element : current.getStackTrace() ) {
                if (element.getClassName().startsWith(AssistantTool.class.getName())) {
                    continue;
                }
                if (scriptName.equals(element.getFileName())) {
                    error.append("    at line ").append(element.getLineNumber()).append(" of the script\n");
                } else if (printed < MAX_OTHER_FRAMES) {
                    error.append("    at ").append(element).append("\n");
                    printed++;
                }
            }
            current = current.getCause() != current ? current.getCause() : null;
        }
        return error.toString().strip();
    }

    private static String shorten( String text ) {
        return text.length() > MAX_RESULT_LENGTH ? text.substring(0, MAX_RESULT_LENGTH) + "..." : text;
    }

    private void appendOutput( String text ) {
        SwingUtilities.invokeLater(() -> {
            output.append(text);
            output.setCaretPosition(output.getDocument().getLength());
        });
    }

    /**
     * Writes the println of the scripts to the output.
     */
    private class OutputWriter extends Writer {
        @Override
        public void write( char[] cbuf, int off, int len ) {
            appendOutput(new String(cbuf, off, len));
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    /**
     * The tab as the assistant panel knows it: its editor and the variables of its runs.
     */
    private class Host implements IScriptAssistantHost {
        @Override
        public ScriptContext getContext() {
            Map<Object, Object> variables = new LinkedHashMap<>();
            for( Object entry : binding.getVariables().entrySet() ) {
                Map.Entry< ? , ? > e = (Map.Entry< ? , ? >) entry;
                variables.put(e.getKey(), e.getValue());
            }
            variables.remove("out");
            return new ScriptContext(ViewerScripts.IMPORTS, ConsoleAssistantContext.helperReference(), ViewerScripts.examples(),
                    ConsoleAssistantContext.variables(variables), ConsoleAssistantContext.checker(shell.getClassLoader()), null,
                    ViewerScripts.HOST_REFERENCE);
        }

        @Override
        public String getEditorScript() {
            return editor.getText();
        }

        @Override
        public String getSelectedScript() {
            return editor.getSelectedText();
        }

        @Override
        public void addScriptToEditor( String script, boolean replace ) {
            if (replace) {
                // through the selection, so that the editor can undo it
                editor.selectAll();
            }
            editor.replaceSelection(script.endsWith("\n") ? script : script + "\n");
            editor.requestFocusInWindow();
        }

        @Override
        public String getLastError( String script ) {
            String failedScript = lastFailedScript;
            return failedScript != null && failedScript.strip().equals(script.strip()) ? lastError : null;
        }
    }
}
