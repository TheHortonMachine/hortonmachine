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
package org.hortonmachine.geoscript.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DescWindowVisibility;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;
import org.hortonmachine.HM;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.libs.exceptions.ModelsUserCancelException;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gui.editor.CodeEditorFactory;
import org.hortonmachine.gui.settings.SettingsController;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.ImageCache;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.transform.ThreadInterrupt;

/**
 * The HortonMachine geoscript console: a groovy editor with syntax highlighting, syntax checking
 * and code completion, that runs scripts with geoscript and the HortonMachine on the classpath.
 *
 * <p>
 * Variables assigned without <code>def</code> live in the binding and survive between runs, so
 * that a script can be run piece by piece (run selection) and the completion knows the type of
 * their values.
 *
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class ScriptConsole {

    private static final String PREF_FONT_SIZE = "hm_console_font_size";
    private static final String PREF_LAST_SCRIPT = "hm_console_last_script";
    private static final String PREF_AUTOCLEAR_OUTPUT = "hm_console_autoclear_output";
    private static final String PREF_OUTPUT_RIGHT = "hm_console_output_right";
    private static final String SCRIPT_NAME = "ConsoleScript";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Pattern IMPORT_LINE = Pattern.compile("^\\s*(import|package)\\s.*$", Pattern.MULTILINE);

    /**
     * Output lines that are not shown in the console (they still reach the terminal), since they
     * would only hide the output of the scripts. Each pattern must match the whole line, without
     * the line ending. Add here what else turns out to be noise.
     */
    private static final List<Pattern> HIDDEN_OUTPUT_LINES = List.of(//
            // printed by the gdal java bindings when the native gdal is missing
            Pattern.compile("Native library load failed\\."), //
            Pattern.compile(".*UnsatisfiedLinkError.*(gdal|kdu_jni).*"), //
            // logged by imageio-ext when the native gdal and kakadu are missing (log header and message)
            Pattern.compile(".*(GDALUtilities loadGDAL|KakaduUtilities loadKakadu)"), //
            Pattern.compile(".*Failed to load the (GDAL|Kakadu) native libs.*") //
    );

    private final boolean exitOnClose;
    private final JFrame frame;
    private final RSyntaxTextArea editor;
    private final ConsoleOutputPane output;
    private final FindBar findBar;
    private JSplitPane splitPane;
    private boolean autoClearOutput = Boolean.parseBoolean(PreferencesHandler.getPreference(PREF_AUTOCLEAR_OUTPUT, "false"));
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel caretLabel = new JLabel(" ");

    private final Binding binding = new Binding();
    private final GroovyShell shell;
    private final ClassIndex classIndex = new ClassIndex();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private File currentFile;
    private boolean dirty = false;
    private int scriptCounter = 1;
    private volatile Thread runThread;
    private boolean errorHighlighted = false;

    private Action runAction;
    private Action runSelectionAction;
    private Action interruptAction;

    /**
     * @param exitOnClose if <code>true</code> the jvm exits when the console is closed.
     */
    public ScriptConsole( boolean exitOnClose ) {
        this.exitOnClose = exitOnClose;

        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(new ASTTransformationCustomizer(ThreadInterrupt.class));
        shell = new GroovyShell(ScriptConsole.class.getClassLoader(), binding, config);

        editor = CodeEditorFactory.createEditor(SyntaxConstants.SYNTAX_STYLE_GROOVY);
        output = new ConsoleOutputPane();
        findBar = new FindBar();

        GroovySyntaxParser syntaxParser = new GroovySyntaxParser();
        syntaxParser.warmUp();
        editor.addParser(syntaxParser);
        editor.setParserDelay(700);

        classIndex.buildAsync();
        installCompletion();
        createActions();

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing( WindowEvent e ) {
                close();
            }
        });
        frame.setJMenuBar(createMenuBar());
        frame.getContentPane().add(createToolBar(), BorderLayout.NORTH);
        frame.getContentPane().add(createMainPanel(), BorderLayout.CENTER);
        frame.getContentPane().add(createStatusBar(), BorderLayout.SOUTH);
        GuiUtilities.setDefaultFrameIcon(frame);

        setFontSize(PreferencesHandler.getPreference(PREF_FONT_SIZE, editor.getFont().getSize()));
        editor.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate( DocumentEvent e ) {
                changed();
            }
            public void removeUpdate( DocumentEvent e ) {
                changed();
            }
            public void changedUpdate( DocumentEvent e ) {
            }
        });
        editor.addCaretListener(e -> updateCaretLabel());

        System.setOut(output.createStream(output.normalStyle, originalOut, ScriptConsole::isNotNoise));
        System.setErr(output.createStream(output.errorStyle, originalErr, ScriptConsole::isNotNoise));

        updateTitle();
        updateCaretLabel();
        output.appendLine("HortonMachine " + org.hortonmachine.Version.getVersion() + " - Groovy "
                + GroovyShell.class.getPackage().getImplementationVersion() + " - Java " + System.getProperty("java.version"),
                output.promptStyle);
        output.appendLine("Ctrl+Space completes, Ctrl+Enter runs the script (or the selection).", output.normalStyle);
    }

    /**
     * Show the console.
     */
    public void show() {
        frame.setSize(1100, 850);
        GuiUtilities.centerOnScreen(frame);
        frame.setVisible(true);
        editor.requestFocusInWindow();
    }

    /**
     * @return the editor, for the ones that want to add to the console.
     */
    public RSyntaxTextArea getEditor() {
        return editor;
    }

    private void installCompletion() {
        GroovyTypeResolver resolver = new GroovyTypeResolver(classIndex, shell::getClassLoader, binding::getVariables);
        GroovyCompletionProvider provider = new GroovyCompletionProvider(resolver);
        AutoCompletion autoCompletion = new AutoCompletion(provider){
            @Override
            protected void insertCompletion( Completion c, boolean typedParamListStartChar ) {
                super.insertCompletion(c, typedParamListStartChar);
                if (c instanceof GroovyCompletionProvider.ClassCompletion) {
                    GroovyCompletionProvider.ClassCompletion classCompletion = (GroovyCompletionProvider.ClassCompletion) c;
                    if (classCompletion.needsImport()) {
                        addImport(classCompletion.getFqn());
                    }
                }
            }
        };
        autoCompletion.setAutoActivationEnabled(true);
        autoCompletion.setAutoActivationDelay(300);
        autoCompletion.setAutoCompleteSingleChoices(false);
        autoCompletion.setParameterAssistanceEnabled(true);
        autoCompletion.setDescWindowVisibility(DescWindowVisibility.ALWAYS);
        autoCompletion.setChoicesWindowSize(450, 300);
        autoCompletion.setDescriptionWindowSize(450, 300);
        autoCompletion.install(editor);
    }

    /**
     * Add an import after the existing ones.
     *
     * @param fqn the fully qualified name of the class.
     */
    private void addImport( String fqn ) {
        String text = editor.getText();
        int insertAt = 0;
        Matcher m = IMPORT_LINE.matcher(text);
        while( m.find() ) {
            insertAt = Math.min(text.length(), m.end() + 1);
        }
        try {
            String importLine = "import " + fqn + "\n";
            if (insertAt == text.length() && insertAt > 0 && text.charAt(insertAt - 1) != '\n') {
                importLine = "\n" + importLine;
            }
            editor.getDocument().insertString(insertAt, importLine, null);
        } catch (BadLocationException e) {
            Logger.INSTANCE.insertError("", "Unable to add the import for " + fqn, e);
        }
    }

    private void createActions() {
        runAction = new AbstractAction("Run", ImageCache.get(ImageCache.RUN)){
            public void actionPerformed( ActionEvent e ) {
                String selected = editor.getSelectedText();
                runScript(selected != null && !selected.isBlank());
            }
        };
        runAction.putValue(Action.SHORT_DESCRIPTION, "Run the script, or the selection if any (Ctrl+Enter)");
        runSelectionAction = new AbstractAction("Run selection", ImageCache.get(ImageCache.RUN_TO_FILE)){
            public void actionPerformed( ActionEvent e ) {
                runScript(true);
            }
        };
        runSelectionAction.putValue(Action.SHORT_DESCRIPTION, "Run the selected lines (Ctrl+Shift+Enter)");
        interruptAction = new AbstractAction("Interrupt", ImageCache.get(ImageCache.STOP)){
            public void actionPerformed( ActionEvent e ) {
                Thread thread = runThread;
                if (thread != null) {
                    thread.interrupt();
                    setStatus("Interrupt requested: modules stop at their next progress step...");
                }
            }
        };
        interruptAction.putValue(Action.SHORT_DESCRIPTION, "Interrupt the running script");
        interruptAction.setEnabled(false);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(item("New", ImageCache.get(ImageCache.NEW), keyCtrl(KeyEvent.VK_N), e -> newScript()));
        fileMenu.add(item("Open...", ImageCache.get(ImageCache.OPEN), keyCtrl(KeyEvent.VK_O), e -> openScript()));
        fileMenu.add(item("Save", ImageCache.get(ImageCache.SAVE), keyCtrl(KeyEvent.VK_S), e -> saveScript(false)));
        fileMenu.add(item("Save as...", null, keyCtrlShift(KeyEvent.VK_S), e -> saveScript(true)));
        fileMenu.addSeparator();
        fileMenu.add(item("Exit", null, null, e -> close()));
        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        editMenu.add(new JMenuItem(RTextArea.getAction(RTextArea.UNDO_ACTION)));
        editMenu.add(new JMenuItem(RTextArea.getAction(RTextArea.REDO_ACTION)));
        editMenu.addSeparator();
        editMenu.add(new JMenuItem(RTextArea.getAction(RTextArea.CUT_ACTION)));
        editMenu.add(new JMenuItem(RTextArea.getAction(RTextArea.COPY_ACTION)));
        editMenu.add(new JMenuItem(RTextArea.getAction(RTextArea.PASTE_ACTION)));
        editMenu.add(new JMenuItem(RTextArea.getAction(RTextArea.SELECT_ALL_ACTION)));
        editMenu.addSeparator();
        editMenu.add(item("Find/Replace...", null, keyCtrl(KeyEvent.VK_F), e -> findBar.open()));
        menuBar.add(editMenu);

        JMenu viewMenu = new JMenu("View");
        viewMenu.add(item("Bigger font", null, keyCtrl(KeyEvent.VK_EQUALS), e -> setFontSize(editor.getFont().getSize() + 1)));
        viewMenu.add(item("Smaller font", null, keyCtrl(KeyEvent.VK_MINUS), e -> setFontSize(editor.getFont().getSize() - 1)));
        viewMenu.addSeparator();
        boolean outputRight = Boolean.parseBoolean(PreferencesHandler.getPreference(PREF_OUTPUT_RIGHT, "false"));
        JCheckBoxMenuItem outputRightItem = new JCheckBoxMenuItem("Output on the right", outputRight);
        outputRightItem.addActionListener(e -> setOutputOnTheRight(outputRightItem.isSelected()));
        viewMenu.add(outputRightItem);
        menuBar.add(viewMenu);

        JMenu scriptMenu = new JMenu("Script");
        scriptMenu.add(item(runAction, keyCtrl(KeyEvent.VK_ENTER)));
        scriptMenu.add(item(runSelectionAction, keyCtrlShift(KeyEvent.VK_ENTER)));
        scriptMenu.add(new JMenuItem(interruptAction));
        scriptMenu.addSeparator();
        scriptMenu.add(item("Clear output", ImageCache.get(ImageCache.TRASH), keyCtrl(KeyEvent.VK_W), e -> output.clear()));
        JCheckBoxMenuItem autoClearItem = new JCheckBoxMenuItem("Clear output before running", autoClearOutput);
        autoClearItem.addActionListener(e -> {
            autoClearOutput = autoClearItem.isSelected();
            PreferencesHandler.setPreference(PREF_AUTOCLEAR_OUTPUT, String.valueOf(autoClearOutput));
        });
        scriptMenu.add(autoClearItem);
        scriptMenu.add(item("Clear variables", null, null, e -> {
            binding.getVariables().clear();
            setStatus("Variables cleared.");
        }));
        menuBar.add(scriptMenu);

        JMenu hmMenu = new JMenu("HM");
        hmMenu.add(item("Add HM main imports", null, null, e -> insertAtStart(ConsoleExamples.HM_IMPORTS)));
        hmMenu.add(item("Add Geoscript main imports", null, null, e -> insertAtStart(ConsoleExamples.GEOSCRIPT_IMPORTS)));
        hmMenu.add(item("Show HM class helper methods", null, null, e -> {
            output.appendLine("HM helper methods:", output.promptStyle);
            output.appendLine(HM.methods(), output.normalStyle);
        }));
        hmMenu.addSeparator();
        JMenu examplesMenu = new JMenu("Examples");
        for( Map.Entry<String, String> example : ConsoleExamples.getExamples().entrySet() ) {
            examplesMenu.add(item(example.getKey(), null, null, e -> editor.replaceSelection(example.getValue())));
        }
        hmMenu.add(examplesMenu);
        menuBar.add(hmMenu);

        return menuBar;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(toolButton("New script", ImageCache.get(ImageCache.NEW), e -> newScript()));
        toolBar.add(toolButton("Open script", ImageCache.get(ImageCache.OPEN), e -> openScript()));
        toolBar.add(toolButton("Save script", ImageCache.get(ImageCache.SAVE), e -> saveScript(false)));
        toolBar.addSeparator();
        toolBar.add(runAction);
        toolBar.add(runSelectionAction);
        toolBar.add(interruptAction);
        toolBar.addSeparator();
        toolBar.add(toolButton("Clear output", ImageCache.get(ImageCache.TRASH), e -> output.clear()));
        return toolBar;
    }

    private JComponent createMainPanel() {
        RTextScrollPane editorScroll = CodeEditorFactory.createScrollPane(editor);
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.add(editorScroll, BorderLayout.CENTER);
        editorPanel.add(new ErrorStrip(editor), BorderLayout.LINE_END);
        editorPanel.add(findBar, BorderLayout.SOUTH);

        JScrollPane outputScroll = new JScrollPane(output);
        outputScroll.setMinimumSize(new Dimension(100, 80));

        boolean outputRight = Boolean.parseBoolean(PreferencesHandler.getPreference(PREF_OUTPUT_RIGHT, "false"));
        splitPane = new JSplitPane(outputRight ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT, editorPanel,
                outputScroll);
        splitPane.setResizeWeight(outputRight ? 0.55 : 0.65);
        splitPane.setDividerLocation(outputRight ? 600 : 500);
        return splitPane;
    }

    /**
     * Place the output below or on the right of the editor.
     *
     * @param right if <code>true</code>, the output goes on the right.
     */
    private void setOutputOnTheRight( boolean right ) {
        splitPane.setOrientation(right ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT);
        double weight = right ? 0.55 : 0.65;
        splitPane.setResizeWeight(weight);
        splitPane.setDividerLocation(weight);
        PreferencesHandler.setPreference(PREF_OUTPUT_RIGHT, String.valueOf(right));
    }

    private JComponent createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(caretLabel, BorderLayout.EAST);
        return statusBar;
    }

    private JMenuItem item( String text, Icon icon, KeyStroke key, java.util.function.Consumer<ActionEvent> consumer ) {
        Action action = new AbstractAction(text, icon){
            public void actionPerformed( ActionEvent e ) {
                consumer.accept(e);
            }
        };
        return item(action, key);
    }

    /**
     * Create a menu item and bind its key also in the editor, which would otherwise consume it.
     */
    private JMenuItem item( Action action, KeyStroke key ) {
        JMenuItem item = new JMenuItem(action);
        if (key != null) {
            item.setAccelerator(key);
            String name = "hm-console-" + action.getValue(Action.NAME);
            editor.getInputMap().put(key, name);
            editor.getActionMap().put(name, action);
        }
        return item;
    }

    private JButton toolButton( String tooltip, Icon icon, java.util.function.Consumer<ActionEvent> consumer ) {
        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.addActionListener(consumer::accept);
        return button;
    }

    private static KeyStroke keyCtrl( int keyCode ) {
        return KeyStroke.getKeyStroke(keyCode, InputEvent.CTRL_DOWN_MASK);
    }

    private static KeyStroke keyCtrlShift( int keyCode ) {
        return KeyStroke.getKeyStroke(keyCode, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
    }

    private void runScript( boolean selectionOnly ) {
        if (runThread != null) {
            setStatus("A script is already running: wait or interrupt it.");
            return;
        }
        String text;
        int firstLine = 0;
        if (selectionOnly) {
            // run whole lines, so that a partial selection still makes sense
            try {
                int startLine = editor.getLineOfOffset(editor.getSelectionStart());
                int endLine = editor.getLineOfOffset(Math.max(editor.getSelectionStart(), editor.getSelectionEnd() - 1));
                int start = editor.getLineStartOffset(startLine);
                int end = editor.getLineEndOffset(endLine);
                text = editor.getText(start, end - start);
                firstLine = startLine;
            } catch (BadLocationException e) {
                return;
            }
        } else {
            text = editor.getText();
        }
        if (text.isBlank()) {
            return;
        }
        clearErrorHighlight();
        if (autoClearOutput) {
            output.clear();
        }

        String scriptName = SCRIPT_NAME + scriptCounter++ + ".groovy";
        String what = selectionOnly ? "selection" : (currentFile != null ? currentFile.getName() : "script");
        String separator = output.getDocument().getLength() > 0 ? "\n" : "";
        output.appendLine(separator + "> " + LocalTime.now().format(TIME_FORMAT) + " running " + what, output.promptStyle);
        setRunning(true);
        setStatus("Running " + what + "...");

        int lineOffset = firstLine;
        long start = System.currentTimeMillis();
        runThread = new Thread(() -> {
            try {
                Object result = shell.evaluate(text, scriptName);
                long elapsed = System.currentTimeMillis() - start;
                System.out.flush();
                SwingUtilities.invokeLater(() -> {
                    if (result != null) {
                        output.appendResult(result);
                    }
                    setStatus("Execution complete in " + formatElapsed(elapsed) + (result == null ? ", no result." : "."));
                });
            } catch (Throwable t) {
                System.out.flush();
                SwingUtilities.invokeLater(() -> showError(t, scriptName, lineOffset));
            } finally {
                runThread = null;
                SwingUtilities.invokeLater(() -> setRunning(false));
            }
        }, "hm-console-script");
        runThread.setDaemon(true);
        runThread.start();
    }

    private void showError( Throwable t, String scriptName, int lineOffset ) {
        if (t instanceof MultipleCompilationErrorsException) {
            MultipleCompilationErrorsException ce = (MultipleCompilationErrorsException) t;
            output.appendLine(ce.getMessage().replace(scriptName + ": ", "").trim(), output.errorStyle);
            for( Message message : ce.getErrorCollector().getErrors() ) {
                if (message instanceof SyntaxErrorMessage) {
                    highlightErrorLine(((SyntaxErrorMessage) message).getCause().getStartLine() - 1 + lineOffset);
                    break;
                }
            }
            setStatus("Compilation failed.");
            return;
        }
        if (isInterruption(t)) {
            output.appendLine("Script interrupted.", output.errorStyle);
            setStatus("Script interrupted.");
            return;
        }
        // the causes matter: exceptions from other threads (ex. parallel streams) are rethrown
        // wrapped, with the original, the one with the message, as cause
        Throwable current = StackTraceUtils.deepSanitize(t);
        boolean highlighted = false;
        for( int depth = 0; current != null && depth < 10; depth++ ) {
            output.appendLine((depth == 0 ? "" : "Caused by: ") + current, output.errorStyle);
            int printed = 0;
            for( StackTraceElement element : current.getStackTrace() ) {
                if (element.getClassName().startsWith(ScriptConsole.class.getName())) {
                    continue; // the console running the script
                }
                if (scriptName.equals(element.getFileName())) {
                    int line = element.getLineNumber() + lineOffset;
                    output.appendLine("    at line " + line + " of the script", output.errorStyle);
                    if (!highlighted) {
                        highlightErrorLine(line - 1);
                        highlighted = true;
                    }
                } else if (printed < 8) {
                    output.appendLine("    at " + element, output.errorStyle);
                    printed++;
                }
            }
            current = current.getCause() != current ? current.getCause() : null;
        }
        setStatus("Execution failed: " + t.getClass().getSimpleName());
    }

    /**
     * @return <code>true</code> if the script stopped because it was interrupted: in the script
     *          itself or in a module, through its progress monitor.
     */
    private static boolean isInterruption( Throwable t ) {
        for( int depth = 0; t != null && depth < 10; depth++ ) {
            if (t instanceof InterruptedException || t instanceof ModelsUserCancelException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private void highlightErrorLine( int line ) {
        try {
            if (line >= 0 && line < editor.getLineCount()) {
                editor.addLineHighlight(line, CodeEditorFactory.isDarkLaf() ? new Color(0x5c2b2b) : new Color(0xfde2e2));
                editor.setCaretPosition(editor.getLineStartOffset(line));
                errorHighlighted = true;
            }
        } catch (BadLocationException e) {
            // ignore
        }
    }

    private void clearErrorHighlight() {
        if (errorHighlighted) {
            editor.removeAllLineHighlights();
            errorHighlighted = false;
        }
    }

    private void setRunning( boolean running ) {
        runAction.setEnabled(!running);
        runSelectionAction.setEnabled(!running);
        interruptAction.setEnabled(running);
    }

    private static boolean isNotNoise( String line ) {
        for( Pattern pattern : HIDDEN_OUTPUT_LINES ) {
            if (pattern.matcher(line).matches()) {
                return false;
            }
        }
        return true;
    }

    private static String formatElapsed( long millis ) {
        return millis < 1000 ? millis + " ms" : String.format("%.1f s", millis / 1000.0);
    }

    private void insertAtStart( String text ) {
        try {
            editor.getDocument().insertString(0, text, null);
        } catch (BadLocationException e) {
            // ignore
        }
    }

    private void changed() {
        if (!dirty) {
            dirty = true;
            updateTitle();
        }
        SwingUtilities.invokeLater(this::clearErrorHighlight);
    }

    private void newScript() {
        if (!checkSaved()) {
            return;
        }
        editor.setText("");
        editor.discardAllEdits();
        currentFile = null;
        dirty = false;
        updateTitle();
    }

    private void openScript() {
        if (!checkSaved()) {
            return;
        }
        File[] files = GuiUtilities.showOpenFilesDialog(frame, "Open script", false, PreferencesHandler.getLastFile(),
                new FileNameExtensionFilter("Groovy scripts", "groovy", "gvy", "gy", "gsh"));
        if (files != null && files.length > 0 && files[0] != null) {
            openFile(files[0]);
        }
    }

    /**
     * Load a script in the editor.
     *
     * @param file the script file.
     */
    public void openFile( File file ) {
        try {
            editor.setText(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            editor.setCaretPosition(0);
            editor.discardAllEdits();
            currentFile = file;
            dirty = false;
            PreferencesHandler.setPreference(PREF_LAST_SCRIPT, file.getAbsolutePath());
            updateTitle();
            setStatus("Opened " + file.getAbsolutePath());
        } catch (Exception e) {
            GuiUtilities.showErrorMessage(frame, "Unable to open " + file + ": " + e.getMessage());
        }
    }

    private boolean saveScript( boolean askName ) {
        File file = currentFile;
        if (file == null || askName) {
            file = GuiUtilities.showSaveFileDialog(frame, "Save script", PreferencesHandler.getLastFile());
            if (file == null) {
                return false;
            }
            if (!file.getName().contains(".")) {
                file = new File(file.getParentFile(), file.getName() + ".groovy");
            }
            if (file.exists() && !GuiUtilities.showYesNoDialog(frame, "The file " + file.getName() + " exists. Overwrite it?")) {
                return false;
            }
        }
        try {
            Files.writeString(file.toPath(), editor.getText(), StandardCharsets.UTF_8);
            currentFile = file;
            dirty = false;
            PreferencesHandler.setPreference(PREF_LAST_SCRIPT, file.getAbsolutePath());
            updateTitle();
            setStatus("Saved " + file.getAbsolutePath());
            return true;
        } catch (Exception e) {
            GuiUtilities.showErrorMessage(frame, "Unable to save " + file + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * @return <code>true</code> if it is ok to discard the editor content.
     */
    private boolean checkSaved() {
        if (!dirty || editor.getText().isBlank()) {
            return true;
        }
        int answer = JOptionPane.showConfirmDialog(frame, "The script has unsaved changes. Save them?", "Unsaved changes",
                JOptionPane.YES_NO_CANCEL_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            return saveScript(false);
        }
        return answer == JOptionPane.NO_OPTION;
    }

    private void close() {
        if (!checkSaved()) {
            return;
        }
        Thread thread = runThread;
        if (thread != null) {
            thread.interrupt();
        }
        System.setOut(originalOut);
        System.setErr(originalErr);
        frame.dispose();
        if (exitOnClose) {
            System.exit(0);
        }
    }

    private void setFontSize( int size ) {
        int fontSize = Math.max(8, Math.min(40, size));
        editor.setFont(editor.getFont().deriveFont((float) fontSize));
        output.setFont(output.getFont().deriveFont((float) fontSize));
        PreferencesHandler.setPreference(PREF_FONT_SIZE, String.valueOf(fontSize));
    }

    private void updateTitle() {
        String name = currentFile != null ? currentFile.getName() : "Untitled";
        frame.setTitle((dirty ? "*" : "") + name + " - HortonMachine Geoscript Console");
    }

    private void updateCaretLabel() {
        int caret = editor.getCaretPosition();
        try {
            int line = editor.getLineOfOffset(caret);
            int column = caret - editor.getLineStartOffset(line);
            caretLabel.setText("Ln " + (line + 1) + ", Col " + (column + 1));
        } catch (BadLocationException e) {
            caretLabel.setText(" ");
        }
    }

    private void setStatus( String status ) {
        statusLabel.setText(status);
    }

    /**
     * A bar to find and replace text in the editor.
     */
    private class FindBar extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JTextField findField = new JTextField(20);
        private final JTextField replaceField = new JTextField(15);
        private final JCheckBox matchCaseBox = new JCheckBox("Match case");
        private final JCheckBox regexBox = new JCheckBox("Regex");
        private final JLabel resultLabel = new JLabel();

        FindBar() {
            super(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

            // one row for find and one for replace, so that nothing wraps out of sight
            JLabel findLabel = new JLabel("Find:");
            JLabel replaceLabel = new JLabel("Replace:");
            findLabel.setPreferredSize(replaceLabel.getPreferredSize());
            JPanel findRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 2));
            findRow.add(findLabel);
            findRow.add(findField);
            findRow.add(button("Next", e -> find(true)));
            findRow.add(button("Previous", e -> find(false)));
            findRow.add(matchCaseBox);
            findRow.add(regexBox);
            findRow.add(resultLabel);
            JPanel replaceRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 2));
            replaceRow.add(replaceLabel);
            replaceRow.add(replaceField);
            replaceRow.add(button("Replace", e -> replace(false)));
            replaceRow.add(button("Replace all", e -> replace(true)));
            JPanel rows = new JPanel(new GridLayout(2, 1));
            rows.add(findRow);
            rows.add(replaceRow);
            add(rows, BorderLayout.CENTER);

            JButton closeButton = button("✕", e -> close());
            closeButton.setToolTipText("Close the find bar (Esc)");
            JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 6, 2));
            closePanel.add(closeButton);
            add(closePanel, BorderLayout.EAST);
            setVisible(false);

            findField.setColumns(20);
            replaceField.setColumns(20);
            findField.addActionListener(e -> find(true));
            replaceField.addActionListener(e -> replace(false));

            // escape closes the bar both from its fields and from the editor
            Action closeAction = new AbstractAction(){
                public void actionPerformed( ActionEvent e ) {
                    if (isVisible()) {
                        close();
                    }
                }
            };
            KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escape, "close-find");
            getActionMap().put("close-find", closeAction);
            editor.getInputMap().put(escape, "close-find");
            editor.getActionMap().put("close-find", closeAction);
        }

        private JButton button( String text, java.util.function.Consumer<ActionEvent> consumer ) {
            JButton button = new JButton(text);
            button.setFont(button.getFont().deriveFont(Font.PLAIN));
            button.addActionListener(consumer::accept);
            return button;
        }

        void open() {
            String selected = editor.getSelectedText();
            if (selected != null && !selected.contains("\n")) {
                findField.setText(selected);
            }
            setVisible(true);
            getParent().revalidate();
            findField.selectAll();
            findField.requestFocusInWindow();
        }

        void close() {
            setVisible(false);
            SearchEngine.markAll(editor, new SearchContext(""));
            getParent().revalidate();
            editor.requestFocusInWindow();
        }

        private SearchContext context( boolean forward ) {
            SearchContext context = new SearchContext(findField.getText(), matchCaseBox.isSelected());
            context.setRegularExpression(regexBox.isSelected());
            context.setSearchForward(forward);
            context.setSearchWrap(true);
            context.setMarkAll(true);
            context.setReplaceWith(replaceField.getText());
            return context;
        }

        private void find( boolean forward ) {
            if (findField.getText().isEmpty()) {
                return;
            }
            SearchResult result = SearchEngine.find(editor, context(forward));
            resultLabel.setText(result.wasFound() ? result.getMarkedCount() + " matches" : "Not found");
        }

        private void replace( boolean all ) {
            if (findField.getText().isEmpty()) {
                return;
            }
            SearchResult result = all ? SearchEngine.replaceAll(editor, context(true))
                    : SearchEngine.replace(editor, context(true));
            resultLabel.setText(all ? result.getCount() + " replaced" : result.wasFound() ? "Replaced" : "Not found");
        }
    }

    public static void main( String[] args ) {
        try {
            if (args.length == 1 && !args[0].trim().isEmpty() && new File(args[0]).isFile()) {
                // run a script headless
                new GroovyShell().run(new File(args[0]), Collections.emptyList());
                return;
            }
            Logger.INSTANCE.init();
            SettingsController.applySettings(null);
            GuiUtilities.setDefaultLookAndFeel();
            SwingUtilities.invokeLater(() -> {
                ScriptConsole console = new ScriptConsole(true);
                String lastScript = PreferencesHandler.getPreference(PREF_LAST_SCRIPT, "");
                if (!lastScript.isBlank() && new File(lastScript).isFile()) {
                    console.openFile(new File(lastScript));
                }
                console.show();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
