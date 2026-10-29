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
package org.hortonmachine.geoscript.console.assistant;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;

import org.hortonmachine.assistant.AssistantProgressMonitor;
import org.hortonmachine.assistant.AssistantSettingsPanel;
import org.hortonmachine.assistant.MarkdownHtml;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.assistant.AssistantConfig.ServerKind;
import org.hortonmachine.gears.assistant.AssistantTurn;
import org.hortonmachine.gears.assistant.IAssistantBackend;
import org.hortonmachine.gears.assistant.IScriptAssistant;
import org.hortonmachine.gears.assistant.ScriptAnswer;
import org.hortonmachine.gears.assistant.ScriptAssistOptions;
import org.hortonmachine.gears.assistant.ScriptContext;
import org.hortonmachine.gears.libs.exceptions.ModelsUserCancelException;
import org.hortonmachine.gui.editor.CodeEditorFactory;
import org.hortonmachine.gui.utils.GuiUtilities;

/**
 * The script assistant of the scripting console: answers requests (which module does something,
 * what a parameter means, or a script that does something), fixes the script of the editor with
 * the error of its last run, explains it.
 *
 * <p>The requests form a discussion: it is shown formatted (markdown of the answers, highlighted
 * scripts) and each request carries the earlier turns, so follow-ups are understood (how many is set
 * in the settings). "New conversation" starts over. With an empty request box, the last request is
 * used again.</p>
 *
 * <p>Fix and Explain work on the selection of the editor, if any, else on the whole script.</p>
 *
 * <p>Each script of the discussion has links to insert it at the caret of the editor, to replace
 * the script of the editor with it, or to copy it. Scripts are never run: the user runs them.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class ScriptAssistantPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final IAssistantBackend backend;
    private final IScriptAssistant scriptAssistant;
    private final IScriptAssistantHost host;

    private final JTextArea questionArea = new JTextArea(3, 50);
    private final JButton askButton = new JButton("Ask");
    private final JButton fixButton = new JButton("Fix editor script");
    private final JButton explainButton = new JButton("Explain editor script");
    private final JCheckBox editorScriptCheckbox = new JCheckBox("Include the editor script");
    private final JButton cancelButton = new JButton("Cancel");
    private final JButton newConversationButton = new JButton("New conversation");
    private final JButton settingsButton = new JButton("Settings");
    private final JLabel statusLabel = new JLabel(" ");
    private final JProgressBar progressBar = new JProgressBar();
    private final JEditorPane discussionPane = new JEditorPane(){
        private static final long serialVersionUID = 1L;

        // always as wide as the panel, so that the answers wrap instead of running out of it
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
    };

    /**
     * The turns of the current discussion, oldest first, as told to the model.
     */
    private final List<AssistantTurn> history = new ArrayList<>();
    /**
     * For each turn, the answer as shown, with the result of its checks: shown in the discussion
     * only, in the history the model would copy the marks of the checks into its next answers.
     */
    private final List<ScriptAnswer> turnAnswers = new ArrayList<>();

    /**
     * A code block of the discussion, which its links refer to by index.
     *
     * @param code the code.
     * @param checkError the problems the checks still find in it, or <code>null</code>.
     */
    private record CodeBlock( String code, String checkError ) {
    }

    /**
     * The code blocks of the discussion as last rendered, in order.
     */
    private final List<CodeBlock> codeBlocks = new ArrayList<>();
    /**
     * The links above the code blocks: <code>hm:action:index</code>.
     */
    private static final String CODE_LINK = "hm:";
    /**
     * The request waiting for its answer, shown after the turns.
     */
    private String pendingQuestion;
    private final StringBuilder pendingText = new StringBuilder();
    private boolean pendingIsScript;
    /**
     * Why the pending answer is a new attempt, if the checks rejected the earlier one.
     */
    private String pendingRoundNote;
    /**
     * Published among the tokens when a repair round starts, followed by its description: the
     * model never writes this character.
     */
    private static final String ROUND_MARKER = "\u0000round:";
    /**
     * A message shown after the discussion, e.g. why the last request failed.
     */
    private String notice;

    /**
     * The requests typed in this window, to use them again.
     */
    private final List<String> typedQuestions = new ArrayList<>();
    private int recallIndex = -1;

    private AssistantProgressMonitor monitor;
    private boolean running = false;
    /**
     * The warm-up of the server, while it runs.
     */
    private AssistantProgressMonitor prepareMonitor;

    /**
     * While an answer streams, the discussion is rendered at most this often: rendering it all
     * takes tens of milliseconds in long discussions, as long as the model takes for a token.
     */
    private static final int STREAM_RENDER_MILLIS = 150;
    private final Timer streamRenderTimer = new Timer(STREAM_RENDER_MILLIS, e -> renderDiscussion());
    {
        streamRenderTimer.setRepeats(false);
    }

    /**
     * @param backend the assistant backend.
     * @param scriptAssistant the script assistant of the backend.
     * @param host the scripting console.
     */
    public ScriptAssistantPanel( IAssistantBackend backend, IScriptAssistant scriptAssistant, IScriptAssistantHost host ) {
        this.backend = backend;
        this.scriptAssistant = scriptAssistant;
        this.host = host;

        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setToolTipText("<html>e.g.: extract the network from the dem in /data/dem.tif with threshold 100;<br>"
                + "or: which module computes the slope?<br>"
                + "Ctrl+Enter asks, Ctrl+W clears the box, Ctrl+Up/Down recall earlier requests.<br>"
                + "With the box empty, Ask uses the last request again.</html>");
        askButton.setToolTipText("<html>Ask a question or for a script. Scripts in the answer are compiled and checked, "
                + "not run. (Ctrl+Enter)</html>");
        fixButton.setToolTipText("Fix the script of the editor (or the selection) with the error of its last run.");
        explainButton.setToolTipText("Explain the script of the editor (or the selection).");
        newConversationButton.setToolTipText("Forget the discussion and start over. (Ctrl+N)");
        editorScriptCheckbox.setToolTipText("<html>Send the script of the editor (or the selection) with each request, so that "
                + "the answers go on with it<br>and use its variables. It holds your paths and values: on by default only "
                + "for a server on this machine.</html>");
        editorScriptCheckbox.setSelected(backend.getConfig().isLocal());

        discussionPane.setContentType("text/html");
        discussionPane.setEditable(false);
        discussionPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        discussionPane.setFont(UIManager.getFont("Label.font"));
        discussionPane.addHyperlinkListener(e -> {
            if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
                return;
            }
            if (e.getDescription() != null && e.getDescription().startsWith(CODE_LINK)) {
                codeAction(e.getDescription());
            } else if (e.getURL() != null) {
                try {
                    GuiUtilities.openUrl(e.getURL().toURI());
                } catch (Exception ex) {
                    Logger.INSTANCE.insertError("ScriptAssistantPanel", "Can't open " + e.getURL(), ex);
                }
            }
        });
        cancelButton.setEnabled(false);

        askButton.addActionListener(e -> ask());
        fixButton.addActionListener(e -> fix());
        explainButton.addActionListener(e -> explain());
        cancelButton.addActionListener(e -> cancel());
        newConversationButton.addActionListener(e -> newConversation());
        settingsButton.addActionListener(e -> {
            AssistantConfig before = backend.getConfig();
            AssistantSettingsPanel.showDialog(this, backend);
            updateStatus();
            // another server, model or context needs its own warm-up; nothing changed, none
            if (!backend.getConfig().equals(before)) {
                prepare();
            }
            // the script goes to an online server only if the user asks again
            if (before.isLocal() != backend.getConfig().isLocal()) {
                editorScriptCheckbox.setSelected(backend.getConfig().isLocal());
            }
        });
        addShortcuts();

        JPanel questionPanel = new JPanel(new BorderLayout(0, 4));
        questionPanel.setBorder(BorderFactory.createTitledBorder("Request"));
        questionPanel.add(new JScrollPane(questionArea), BorderLayout.CENTER);
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actionsPanel.add(askButton);
        actionsPanel.add(fixButton);
        actionsPanel.add(explainButton);
        // on the right, so that they stay visible in a narrow window
        JPanel conversationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        conversationPanel.add(newConversationButton);
        conversationPanel.add(settingsButton);
        JPanel buttonsPanel = new JPanel(new BorderLayout());
        buttonsPanel.add(actionsPanel, BorderLayout.CENTER);
        buttonsPanel.add(conversationPanel, BorderLayout.EAST);
        // on its own line, so that it isn't cut in a narrow window
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        optionsPanel.add(editorScriptCheckbox);
        buttonsPanel.add(optionsPanel, BorderLayout.SOUTH);
        questionPanel.add(buttonsPanel, BorderLayout.SOUTH);

        JPanel outputPanel = new JPanel(new BorderLayout(0, 4));
        outputPanel.setBorder(BorderFactory.createTitledBorder("Discussion"));
        JPanel progressPanel = new JPanel(new BorderLayout(4, 0));
        progressPanel.add(statusLabel, BorderLayout.CENTER);
        // next to the progress, where it is looked for while waiting
        JPanel cancelPanel = new JPanel(new BorderLayout(4, 0));
        cancelPanel.add(progressBar, BorderLayout.CENTER);
        cancelPanel.add(cancelButton, BorderLayout.EAST);
        progressPanel.add(cancelPanel, BorderLayout.EAST);
        outputPanel.add(progressPanel, BorderLayout.NORTH);
        JScrollPane discussionScroll = new JScrollPane(discussionPane);
        discussionScroll.setPreferredSize(new java.awt.Dimension(700, 480));
        outputPanel.add(discussionScroll, BorderLayout.CENTER);

        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(questionPanel, BorderLayout.NORTH);
        add(outputPanel, BorderLayout.CENTER);

        updateStatus();
        renderDiscussion();
        prepare();
    }

    /**
     * Get the server ready in the background, so that the first request doesn't wait for the model
     * to load and to read the reference: it takes a few seconds once, then stays ready while the
     * model is loaded. Requests can be sent meanwhile, the server takes them after it.
     *
     * <p>Hosted services are not prepared: their models are always loaded, they keep nothing for
     * the next request, and on free plans each warm-up counts as one of the few allowed requests.</p>
     */
    private void prepare() {
        if (prepareMonitor != null) {
            prepareMonitor.setCanceled(true);
            prepareMonitor = null;
        }
        AssistantConfig config = backend.getConfig();
        String model = config.model();
        if (model == null || model.isBlank()) {
            return;
        }
        if (config.serverKind() == ServerKind.OPENAI_COMPATIBLE && !config.isLocal()) {
            if (!running) {
                progressBar.setIndeterminate(false);
                updateStatus();
            }
            return;
        }
        AssistantProgressMonitor pm = new AssistantProgressMonitor(null, null);
        prepareMonitor = pm;
        ScriptContext context = host.getContext();
        if (!running) {
            statusLabel.setText("Preparing the assistant: loading " + model + " and reading the reference of the modules...");
            progressBar.setIndeterminate(true);
        }
        long start = System.currentTimeMillis();
        new SwingWorker<Void, Void>(){
            @Override
            protected Void doInBackground() throws Exception {
                scriptAssistant.prepare(context, pm);
                return null;
            }

            @Override
            protected void done() {
                if (pm != prepareMonitor) {
                    return; // replaced by a newer one
                }
                prepareMonitor = null;
                if (running) {
                    return; // a request owns the status now
                }
                progressBar.setIndeterminate(false);
                try {
                    get();
                    statusLabel.setText(String.format("Ready (prepared in %.1f s). ", (System.currentTimeMillis() - start) / 1000.0)
                            + backend.getName() + " at " + backend.getConfig().baseUrl() + ", " + model);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    if (cause instanceof ModelsUserCancelException) {
                        updateStatus();
                        return;
                    }
                    String reason = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
                    statusLabel.setText("Not ready: " + reason + " Check the server and the model in the settings.");
                }
            }
        }.execute();
    }

    /**
     * Ctrl+Enter asks (Ctrl+Shift+Enter too, which asked when writing scripts was apart), Ctrl+W
     * clears the request, Ctrl+N starts a new conversation: they work in the whole window.
     * Ctrl+Up/Down in the request box recall the earlier requests.
     */
    private void addShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "askQuestion");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                "askQuestion");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK), "clearQuestion");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "newConversation");
        actionMap.put("askQuestion", action(() -> clickIfEnabled(askButton)));
        actionMap.put("clearQuestion", action(() -> {
            questionArea.setText("");
            recallIndex = -1;
            questionArea.requestFocusInWindow();
        }));
        actionMap.put("newConversation", action(() -> clickIfEnabled(newConversationButton)));

        // in the box itself, so that they win over the caret movements of the text area
        InputMap questionInputMap = questionArea.getInputMap(JComponent.WHEN_FOCUSED);
        questionInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK), "recallOlder");
        questionInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), "recallNewer");
        questionArea.getActionMap().put("recallOlder", action(() -> recall(-1)));
        questionArea.getActionMap().put("recallNewer", action(() -> recall(1)));
    }

    private static AbstractAction action( Runnable runnable ) {
        return new AbstractAction(){
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed( ActionEvent e ) {
                runnable.run();
            }
        };
    }

    private static void clickIfEnabled( JButton button ) {
        if (button.isEnabled()) {
            button.doClick();
        }
    }

    /**
     * Put an earlier request in the box.
     *
     * @param direction -1 for an older one, 1 for a newer one (past the newest the box is emptied).
     */
    private void recall( int direction ) {
        if (typedQuestions.isEmpty()) {
            return;
        }
        if (direction < 0) {
            recallIndex = recallIndex < 0 ? typedQuestions.size() - 1 : Math.max(0, recallIndex - 1);
        } else {
            if (recallIndex < 0) {
                return;
            }
            recallIndex++;
            if (recallIndex >= typedQuestions.size()) {
                recallIndex = -1;
                questionArea.setText("");
                return;
            }
        }
        questionArea.setText(typedQuestions.get(recallIndex));
    }

    private void updateStatus() {
        AssistantConfig config = backend.getConfig();
        String model = config.model() == null ? "no model chosen" : config.model();
        statusLabel.setText(backend.getName() + " at " + config.baseUrl() + ", " + model);
    }

    private void newConversation() {
        history.clear();
        turnAnswers.clear();
        notice = null;
        updateStatus();
        renderDiscussion();
        questionArea.requestFocusInWindow();
    }

    /**
     * Show the discussion: each request in a tinted block, each answer with its markdown
     * formatted and its scripts highlighted, with their links, then the request waiting for its
     * answer and the notice.
     */
    private void renderDiscussion() {
        streamRenderTimer.stop();
        boolean dark = CodeEditorFactory.isDarkLaf();
        MarkdownHtml.Palette palette = dark ? MarkdownHtml.DARK : MarkdownHtml.LIGHT;
        String questionBackground = dark ? "#2a3a52" : "#e3ecf9";
        String youColor = dark ? "#7fb2ff" : "#0b4fa8";
        String assistantColor = dark ? "#8fd18f" : "#2e7d32";

        codeBlocks.clear();
        StringBuilder sb = new StringBuilder("<html><body>");
        if (history.isEmpty() && pendingQuestion == null && notice == null) {
            sb.append("<p><font color=\"#888888\">Ask a question or for a script. Follow-up requests continue the "
                    + "discussion; New conversation starts over. The links above each script put it into the "
                    + "editor.</font></p>");
        }
        for( int i = 0; i < history.size(); i++ ) {
            ScriptAnswer answer = turnAnswers.get(i);
            String note = checkNote(answer);
            appendQuestion(sb, history.get(i).question(), questionBackground, youColor, palette);
            appendAnswer(sb, MarkdownHtml.toHtml(answer.text(), palette, ( code, language ) -> codeLinks(answer, code, language))
                    + (note != null ? note : ""), assistantColor);
        }
        if (pendingQuestion != null) {
            appendQuestion(sb, pendingQuestion, questionBackground, youColor, palette);
            String answer = "";
            if (pendingRoundNote != null) {
                answer = "<p><font color=\"#b26a00\"><i>" + MarkdownHtml.escape(pendingRoundNote) + "</i></font></p>";
            }
            if (pendingText.length() == 0) {
                answer += "<p><i>waiting for the model...</i></p>";
            } else if (pendingIsScript) {
                // JSON while it arrives, unreadable: only how far it is, the script is shown when done
                answer += "<p><i>writing the script... (" + pendingText.length() + " characters so far)</i></p>";
            } else {
                answer += MarkdownHtml.toHtml(pendingText.toString(), palette);
            }
            appendAnswer(sb, answer, assistantColor);
        }
        if (notice != null) {
            sb.append("<p><font color=\"#c0392b\">").append(MarkdownHtml.escape(notice)).append("</font></p>");
        }
        sb.append("</body></html>");
        discussionPane.setText(sb.toString());
        discussionPane.setCaretPosition(discussionPane.getDocument().getLength());
    }

    /**
     * The links above a script of an answer, which refer to it by its index in {@link #codeBlocks}.
     *
     * @param answer the answer the script is in.
     * @param code the code of the block.
     * @param language the language of the block.
     * @return the links, or <code>null</code> for blocks that aren't scripts.
     */
    private String codeLinks( ScriptAnswer answer, String code, String language ) {
        if (!language.isEmpty() && !language.equals("groovy") && !language.equals("java")) {
            return null;
        }
        // the problems of the checks belong to the script that was checked
        String checkError = answer.checkError() != null && answer.script() != null && code.strip().equals(answer.script().strip())
                ? answer.checkError()
                : null;
        int index = codeBlocks.size();
        codeBlocks.add(new CodeBlock(code.strip(), checkError));
        String separator = " &nbsp;&middot;&nbsp; ";
        return "<p><font size=\"-1\"><a href=\"" + CODE_LINK + "insert:" + index + "\">Insert at caret</a>" + separator
                + "<a href=\"" + CODE_LINK + "replace:" + index + "\">Replace editor script</a>" + separator + "<a href=\""
                + CODE_LINK + "copy:" + index + "\">Copy</a></font></p>";
    }

    /**
     * Run the action of a link above a script.
     *
     * @param link the link, as <code>hm:action:index</code>.
     */
    private void codeAction( String link ) {
        String[] parts = link.substring(CODE_LINK.length()).split(":");
        if (parts.length != 2) {
            return;
        }
        int index;
        try {
            index = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }
        if (index < 0 || index >= codeBlocks.size()) {
            return;
        }
        CodeBlock block = codeBlocks.get(index);
        if (parts[0].equals("copy")) {
            GuiUtilities.copyToClipboard(block.code());
            statusLabel.setText("Script copied.");
            return;
        }
        if (block.checkError() != null) {
            boolean ok = GuiUtilities.showYesNoDialog(this,
                    "The checks still find problems in this script:\n" + block.checkError() + "\n\nAdd it to the editor anyway?");
            if (!ok) {
                return;
            }
        }
        host.addScriptToEditor(block.code(), parts[0].equals("replace"));
    }

    /**
     * Render the streaming answer soon, gathering the tokens that arrive meanwhile.
     */
    private void scheduleStreamRender() {
        if (!streamRenderTimer.isRunning()) {
            streamRenderTimer.start();
        }
    }

    private static void appendQuestion( StringBuilder sb, String question, String background, String color,
            MarkdownHtml.Palette palette ) {
        sb.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"6\"><tr><td bgcolor=\"").append(background)
                .append("\"><b><font color=\"").append(color).append("\">You</font></b>")
                .append(MarkdownHtml.toHtml(question, palette)).append("</td></tr></table>");
    }

    private static void appendAnswer( StringBuilder sb, String answerHtml, String color ) {
        sb.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"6\"><tr><td><b><font color=\"").append(color)
                .append("\">Assistant</font></b>").append(answerHtml).append("</td></tr></table><br>");
    }

    /**
     * @return <code>true</code> if a model is chosen, after asking for one in the settings if needed.
     */
    private boolean checkModel() {
        String model = backend.getConfig().model();
        if (model == null || model.isBlank()) {
            AssistantSettingsPanel.showDialog(this, backend);
            updateStatus();
            model = backend.getConfig().model();
        }
        return model != null && !model.isBlank();
    }

    /**
     * @return the request of the box or, if it is empty, the last one made; <code>null</code> if
     *          there is none.
     */
    private String readQuestion() {
        String question = questionArea.getText().trim();
        if (question.isEmpty()) {
            if (typedQuestions.isEmpty()) {
                GuiUtilities.showWarningMessage(this, "Write a request first.");
                return null;
            }
            return typedQuestions.get(typedQuestions.size() - 1);
        }
        if (typedQuestions.isEmpty() || !typedQuestions.get(typedQuestions.size() - 1).equals(question)) {
            typedQuestions.add(question);
        }
        recallIndex = -1;
        return question;
    }

    /**
     * @return the selection of the editor or, without one, its whole script; <code>null</code>
     *          after telling the user, if it is empty.
     */
    private String readEditorScript() {
        String selected = host.getSelectedScript();
        String script = selected != null && !selected.isBlank() ? selected : host.getEditorScript();
        if (script == null || script.isBlank()) {
            GuiUtilities.showWarningMessage(this, "The editor of the console is empty.");
            return null;
        }
        return script;
    }

    private void ask() {
        String question = readQuestion();
        if (question == null || !checkModel()) {
            return;
        }
        ScriptContext context = host.getContext();
        if (editorScriptCheckbox.isSelected()) {
            String selected = host.getSelectedScript();
            String script = selected != null && !selected.isBlank() ? selected : host.getEditorScript();
            if (script != null && !script.isBlank()) {
                context = context.withEditorScript(script);
            }
        }
        ScriptContext requestContext = context;
        List<AssistantTurn> turns = List.copyOf(history);
        runAnswerRequest(question, true, false,
                ( onToken, pm ) -> scriptAssistant.ask(requestContext, question, turns, options(), onToken, pm));
    }

    private void fix() {
        String script = readEditorScript();
        if (script == null) {
            return;
        }
        String error = host.getLastError(script);
        if (error == null) {
            GuiUtilities.showWarningMessage(this,
                    "Run the script of the editor (or the selection) first: the fix needs the error of its run.");
            return;
        }
        if (!checkModel()) {
            return;
        }
        ScriptContext context = host.getContext();
        List<AssistantTurn> turns = List.copyOf(history);
        String turnQuestion = "Fix this script:\n```groovy\n" + script.strip() + "\n```\nIt failed with: " + error;
        runAnswerRequest(turnQuestion, false, true,
                ( onToken, pm ) -> scriptAssistant.fix(context, script, error, turns, options(), onToken, pm));
    }

    private void explain() {
        String script = readEditorScript();
        if (script == null || !checkModel()) {
            return;
        }
        ScriptContext context = host.getContext();
        List<AssistantTurn> turns = List.copyOf(history);
        String turnQuestion = "Explain this script:\n```groovy\n" + script.strip() + "\n```";
        runTextRequest(turnQuestion, false,
                ( onToken, pm ) -> scriptAssistant.explain(context, script, turns, options(), onToken, pm));
    }

    private static ScriptAssistOptions options() {
        return ScriptAssistOptions.defaults();
    }

    @FunctionalInterface
    private interface Request<T> {
        T run( Consumer<String> onToken, AssistantProgressMonitor pm ) throws Exception;
    }

    /**
     * @param turnQuestion the request as recorded in the discussion.
     * @param fromQuestionBox <code>true</code> if the request comes from the request box, which is
     *          then cleared for the next request and given it back if the request fails.
     */
    private void runTextRequest( String turnQuestion, boolean fromQuestionBox, Request<String> request ) {
        startRequest(turnQuestion, false, fromQuestionBox);
        AssistantProgressMonitor pm = monitor;
        new SwingWorker<String, String>(){
            @Override
            protected String doInBackground() throws Exception {
                return request.run(this::publish, pm);
            }

            @Override
            protected void process( List<String> tokens ) {
                tokens.forEach(pendingText::append);
                scheduleStreamRender();
            }

            @Override
            protected void done() {
                try {
                    String answer = get();
                    history.add(new AssistantTurn(turnQuestion, answer, null));
                    turnAnswers.add(new ScriptAnswer(answer, null, false, null, List.of()));
                    endTurn();
                } catch (Exception e) {
                    endWithError(e, fromQuestionBox ? turnQuestion : null);
                }
            }
        }.execute();
    }

    /**
     * @param turnQuestion the request as recorded in the discussion.
     * @param fromQuestionBox <code>true</code> if the request comes from the request box.
     * @param json <code>true</code> if the answer streams as JSON, unreadable until done.
     * @param request the request, whose script, if any, is checked and maybe repaired.
     */
    private void runAnswerRequest( String turnQuestion, boolean fromQuestionBox, boolean json, Request<ScriptAnswer> request ) {
        startRequest(turnQuestion, json, fromQuestionBox);
        AssistantProgressMonitor pm = monitor;
        new SwingWorker<ScriptAnswer, String>(){
            @Override
            protected ScriptAnswer doInBackground() throws Exception {
                // through the same channel as the tokens, so that it comes after those of the rejected answer
                pm.setTaskListener(task -> publish(ROUND_MARKER + task));
                return request.run(this::publish, pm);
            }

            @Override
            protected void process( List<String> tokens ) {
                for( String token : tokens ) {
                    if (token.startsWith(ROUND_MARKER)) {
                        // a new attempt replaces the rejected answer
                        pendingText.setLength(0);
                        pendingRoundNote = token.substring(ROUND_MARKER.length());
                    } else {
                        pendingText.append(token);
                    }
                }
                scheduleStreamRender();
            }

            @Override
            protected void done() {
                try {
                    ScriptAnswer answer = get();
                    // a rejection is told to the model too, as a plain fact a follow-up can use; the
                    // marks of the check are only shown, or the model copies them into its answers
                    String answerText = answer.text();
                    history.add(new AssistantTurn(turnQuestion, answer.checkError() != null
                            ? answerText + "\n\n(The checks still found problems: "
                                    + answer.checkError().strip().lines().findFirst().orElse("") + ")"
                            : answerText, null));
                    turnAnswers.add(answer);
                    endTurn();
                } catch (Exception e) {
                    endWithError(e, fromQuestionBox ? turnQuestion : null);
                }
            }
        }.execute();
    }

    /**
     * @return the result of the checks of the script, as html to show under the answer, or
     *          <code>null</code> if it has no script.
     */
    private static String checkNote( ScriptAnswer answer ) {
        if (answer.script() == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (answer.checkError() != null) {
            sb.append("<p><font color=\"#c0392b\"><b>The checks still find problems:</b> ")
                    .append(MarkdownHtml.escape(answer.checkError())).append("</font></p>");
        } else if (answer.checked()) {
            sb.append("<p><font color=\"#2e7d32\"><i>Compiled, and its use of the modules checked. Not run.</i></font></p>");
        }
        if (answer.modules() != null && !answer.modules().isEmpty()) {
            sb.append("<p><font color=\"#888888\">Modules: ").append(MarkdownHtml.escape(String.join(", ", answer.modules())))
                    .append("</font></p>");
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private void startRequest( String turnQuestion, boolean isScript, boolean fromQuestionBox ) {
        if (fromQuestionBox) {
            questionArea.setText("");
        }
        notice = null;
        pendingQuestion = turnQuestion;
        pendingText.setLength(0);
        pendingRoundNote = null;
        pendingIsScript = isScript;
        monitor = new AssistantProgressMonitor(progressBar, null);
        statusLabel.setText("Waiting for the answer...");
        progressBar.setIndeterminate(true);
        setRunning(true);
        renderDiscussion();
    }

    private void endTurn() {
        pendingQuestion = null;
        renderDiscussion();
        endRequest("Done. " + history.size() + (history.size() == 1 ? " turn" : " turns") + " in this discussion, the last "
                + backend.getConfig().historyTurns() + " are remembered.");
    }

    private void endRequest( String message ) {
        progressBar.setIndeterminate(false);
        statusLabel.setText(message);
        setRunning(false);
    }

    /**
     * A failed or canceled request leaves no turn: the discussion goes back to how it was and a
     * typed request goes back to the request box, to try again.
     */
    private void endWithError( Exception e, String typedQuestion ) {
        pendingQuestion = null;
        if (typedQuestion != null && questionArea.getText().isBlank()) {
            questionArea.setText(typedQuestion);
        }
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        if (cause instanceof ModelsUserCancelException) {
            renderDiscussion();
            endRequest("Canceled.");
            return;
        }
        Logger.INSTANCE.insertError("ScriptAssistantPanel", "Assistant request failed", cause);
        String reason = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        notice = "The request failed: " + reason + ". Check the server and the model in the settings.";
        renderDiscussion();
        endRequest("Failed.");
    }

    private void setRunning( boolean running ) {
        this.running = running;
        askButton.setEnabled(!running);
        fixButton.setEnabled(!running);
        explainButton.setEnabled(!running);
        settingsButton.setEnabled(!running);
        newConversationButton.setEnabled(!running);
        cancelButton.setEnabled(running);
    }

    /**
     * Stop a running request and the warm-up, if any.
     */
    public void cancel() {
        if (monitor != null) {
            monitor.setCanceled(true);
        }
        if (prepareMonitor != null) {
            prepareMonitor.setCanceled(true);
        }
    }
}
