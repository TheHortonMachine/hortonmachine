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
package org.hortonmachine.database.assistant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
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
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.hortonmachine.assistant.AssistantProgressMonitor;
import org.hortonmachine.assistant.AssistantSettingsPanel;
import org.hortonmachine.assistant.MarkdownHtml;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.utils.SqlFormatter;
import org.hortonmachine.gears.assistant.AssistantConfig;
import org.hortonmachine.gears.assistant.AssistantTurn;
import org.hortonmachine.gears.assistant.IAssistantBackend;
import org.hortonmachine.gears.assistant.ISqlAssistant;
import org.hortonmachine.gears.assistant.SqlAnswer;
import org.hortonmachine.gears.assistant.SqlAssistOptions;
import org.hortonmachine.gears.libs.exceptions.ModelsUserCancelException;
import org.hortonmachine.gui.editor.CodeEditorFactory;
import org.hortonmachine.gui.utils.GuiUtilities;

/**
 * The SQL assistant of the database viewer: answers free questions (also how to connect to a
 * database, so it works without a connection), writes SQL from a question, fixes the SQL of the
 * editor with the error of the database, explains it.
 *
 * <p>The requests form a discussion: it is shown formatted (markdown of the answers, highlighted
 * SQL) and each request carries the earlier turns, so follow-ups are understood (how many is set in
 * the settings). "New conversation" starts over. With an empty question box, the last question is
 * used again, e.g. to write the SQL of something just asked.</p>
 *
 * <p>Generated SQL is never run: it goes into the editor only when the user asks, after a
 * confirmation if it changes the database. No row limit is added, since the viewer already
 * limits the rows it shows.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class SqlAssistantPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final IAssistantBackend backend;
    private final ISqlAssistant sqlAssistant;
    private final ISqlAssistantHost host;

    private final JTextArea questionArea = new JTextArea(3, 50);
    private final JLabel databaseLabel = new JLabel(" ");
    private final JCheckBox sampleRowsCheckbox = new JCheckBox("Include sample rows");
    private final JButton askButton = new JButton("Ask");
    private final JButton generateButton = new JButton("Write SQL");
    private final JButton fixButton = new JButton("Fix editor SQL");
    private final JButton explainButton = new JButton("Explain editor SQL");
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
    private final RSyntaxTextArea sqlArea = CodeEditorFactory.createEditor(SyntaxConstants.SYNTAX_STYLE_SQL);
    private final JLabel writeWarningLabel = new JLabel(" ");
    private final JButton insertButton = new JButton("Add to editor");

    /**
     * The turns of the current discussion, oldest first.
     */
    private final List<AssistantTurn> history = new ArrayList<>();
    /**
     * For each turn, the result of the database check, shown in the discussion only: in the
     * history the model would copy it into its next answers.
     */
    private final List<String> turnNotes = new ArrayList<>();
    /**
     * The request waiting for its answer, shown after the turns.
     */
    private String pendingQuestion;
    private final StringBuilder pendingText = new StringBuilder();
    private boolean pendingIsSql;
    /**
     * A message shown after the discussion, e.g. why the last request failed.
     */
    private String notice;

    /**
     * The questions typed in this window, to use them again.
     */
    private final List<String> typedQuestions = new ArrayList<>();
    private int recallIndex = -1;

    private AssistantProgressMonitor monitor;
    private SqlAnswer lastAnswer;
    private boolean running = false;

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
     * @param sqlAssistant the SQL assistant of the backend.
     * @param host the database viewer.
     */
    public SqlAssistantPanel( IAssistantBackend backend, ISqlAssistant sqlAssistant, ISqlAssistantHost host ) {
        this.backend = backend;
        this.sqlAssistant = sqlAssistant;
        this.host = host;

        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setToolTipText("<html>e.g.: the 10 longest roads with their municipality; or: how do I open our PostGIS?<br>"
                + "Ctrl+Enter runs it, Ctrl+W clears it, Ctrl+Up/Down recall earlier questions.<br>"
                + "With the box empty, Ask and Write SQL use the last question again.</html>");
        askButton.setToolTipText("Ask anything about databases, also how to connect to one. The answer is only text. "
                + "(Ctrl+Shift+Enter, or Ctrl+Enter without a database)");
        generateButton.setToolTipText("Write the SQL that answers the question, for the connected database. (Ctrl+Enter)");
        newConversationButton.setToolTipText("Forget the discussion and start over. (Ctrl+N)");
        sampleRowsCheckbox.setToolTipText(
                "Give the model the first rows of each table. Helps with codes and formats, but sends data to the server.");
        databaseLabel.setForeground(new Color(110, 110, 110));

        discussionPane.setContentType("text/html");
        discussionPane.setEditable(false);
        discussionPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        discussionPane.setFont(UIManager.getFont("Label.font"));
        discussionPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getURL() != null) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
                    Logger.INSTANCE.insertError("SqlAssistantPanel", "Can't open " + e.getURL(), ex);
                }
            }
        });
        sqlArea.setRows(6);
        sqlArea.setLineWrap(true);
        sqlArea.setWrapStyleWord(true);
        writeWarningLabel.setForeground(new Color(180, 40, 0));
        insertButton.setEnabled(false);
        cancelButton.setEnabled(false);

        askButton.addActionListener(e -> ask());
        generateButton.addActionListener(e -> generate());
        fixButton.addActionListener(e -> fix());
        explainButton.addActionListener(e -> explain());
        cancelButton.addActionListener(e -> {
            if (monitor != null) {
                monitor.setCanceled(true);
            }
        });
        newConversationButton.addActionListener(e -> newConversation());
        settingsButton.addActionListener(e -> {
            AssistantSettingsPanel.showDialog(this, backend);
            updateStatus();
        });
        insertButton.addActionListener(e -> insertIntoEditor());
        addShortcuts();

        JPanel questionPanel = new JPanel(new BorderLayout(0, 4));
        questionPanel.setBorder(BorderFactory.createTitledBorder("Question"));
        questionPanel.add(new JScrollPane(questionArea), BorderLayout.CENTER);
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actionsPanel.add(askButton);
        actionsPanel.add(generateButton);
        actionsPanel.add(fixButton);
        actionsPanel.add(explainButton);
        actionsPanel.add(cancelButton);
        actionsPanel.add(sampleRowsCheckbox);
        actionsPanel.add(newConversationButton);
        actionsPanel.add(settingsButton);
        JPanel southPanel = new JPanel(new BorderLayout(0, 2));
        southPanel.add(actionsPanel, BorderLayout.CENTER);
        southPanel.add(databaseLabel, BorderLayout.SOUTH);
        questionPanel.add(southPanel, BorderLayout.SOUTH);

        JPanel outputPanel = new JPanel(new BorderLayout(0, 4));
        outputPanel.setBorder(BorderFactory.createTitledBorder("Discussion"));
        JPanel progressPanel = new JPanel(new BorderLayout(4, 0));
        progressPanel.add(statusLabel, BorderLayout.CENTER);
        progressPanel.add(progressBar, BorderLayout.EAST);
        outputPanel.add(progressPanel, BorderLayout.NORTH);
        JScrollPane discussionScroll = new JScrollPane(discussionPane);
        discussionScroll.setPreferredSize(new java.awt.Dimension(700, 320));
        outputPanel.add(discussionScroll, BorderLayout.CENTER);

        JPanel sqlPanel = new JPanel(new BorderLayout(0, 4));
        sqlPanel.setBorder(BorderFactory.createTitledBorder("Latest SQL"));
        sqlPanel.add(CodeEditorFactory.createScrollPane(sqlArea), BorderLayout.CENTER);
        JPanel insertPanel = new JPanel(new BorderLayout(4, 0));
        insertPanel.add(writeWarningLabel, BorderLayout.CENTER);
        insertPanel.add(insertButton, BorderLayout.EAST);
        sqlPanel.add(insertPanel, BorderLayout.SOUTH);

        JSplitPane answerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outputPanel, sqlPanel);
        answerSplit.setResizeWeight(0.7);

        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(questionPanel, BorderLayout.NORTH);
        add(answerSplit, BorderLayout.CENTER);

        updateStatus();
        updateDatabaseState();
        renderDiscussion();
    }

    /**
     * Ctrl+Enter runs the question (Write SQL with a database, Ask without), Ctrl+Shift+Enter
     * always asks, Ctrl+W clears the question, Ctrl+N starts a new conversation: they work in the
     * whole window. Ctrl+Up/Down in the question box recall the earlier questions.
     */
    private void addShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "runQuestion");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                "askQuestion");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK), "clearQuestion");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "newConversation");
        actionMap.put("runQuestion", action(() -> clickIfEnabled(host.getDatabase() != null ? generateButton : askButton)));
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
     * Put an earlier question in the box.
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

    /**
     * Enable the actions that need a database according to the connection of the viewer. Call
     * it when the connection may have changed.
     */
    public void updateDatabaseState() {
        boolean hasDb = host.getDatabase() != null;
        generateButton.setEnabled(hasDb && !running);
        fixButton.setEnabled(hasDb && !running);
        explainButton.setEnabled(hasDb && !running);
        databaseLabel.setText(hasDb
                ? "Connected to " + host.getDatabase().getDatabasePath()
                : "No database connected: writing SQL needs one, but you can Ask, e.g. how to connect.");
    }

    private void newConversation() {
        history.clear();
        turnNotes.clear();
        notice = null;
        lastAnswer = null;
        sqlArea.setText("");
        writeWarningLabel.setText(" ");
        insertButton.setEnabled(false);
        updateStatus();
        renderDiscussion();
        questionArea.requestFocusInWindow();
    }

    /**
     * Show the discussion: each question in a tinted block, each answer with its markdown
     * formatted and its SQL highlighted, then the request waiting for its answer and the notice.
     */
    private void renderDiscussion() {
        streamRenderTimer.stop();
        boolean dark = CodeEditorFactory.isDarkLaf();
        MarkdownHtml.Palette palette = dark ? MarkdownHtml.DARK : MarkdownHtml.LIGHT;
        String questionBackground = dark ? "#2a3a52" : "#e3ecf9";
        String youColor = dark ? "#7fb2ff" : "#0b4fa8";
        String assistantColor = dark ? "#8fd18f" : "#2e7d32";

        StringBuilder sb = new StringBuilder("<html><body>");
        if (history.isEmpty() && pendingQuestion == null && notice == null) {
            sb.append("<p><font color=\"#888888\">Ask a question or let the assistant write SQL. Follow-up questions "
                    + "continue the discussion; New conversation starts over.</font></p>");
        }
        for( int i = 0; i < history.size(); i++ ) {
            AssistantTurn turn = history.get(i);
            String note = turnNotes.get(i);
            appendQuestion(sb, turn.question(), questionBackground, youColor, palette);
            appendAnswer(sb, MarkdownHtml.toHtml(turn.answer() == null ? "" : turn.answer(), palette)
                    + (turn.sql() != null ? MarkdownHtml.codeBlock(SqlFormatter.format(turn.sql()), "sql", palette) : "")
                    + (note != null ? note : ""), assistantColor);
        }
        if (pendingQuestion != null) {
            appendQuestion(sb, pendingQuestion, questionBackground, youColor, palette);
            String answer;
            if (pendingText.length() == 0) {
                answer = "<p><i>waiting for the model...</i></p>";
            } else if (pendingIsSql) {
                // JSON while it arrives: shown raw
                answer = "<p><i>writing the SQL...</i></p>" + MarkdownHtml.codeBlock(pendingText.toString(), "json", palette);
            } else {
                answer = MarkdownHtml.toHtml(pendingText.toString(), palette);
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

    private SqlAssistOptions options() {
        return new SqlAssistOptions(sampleRowsCheckbox.isSelected(), 0);
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
     * @return the database, or <code>null</code> after telling the user what is missing.
     */
    private ADb checkDatabase() {
        ADb db = host.getDatabase();
        if (db == null) {
            updateDatabaseState();
            GuiUtilities.showWarningMessage(this, "Connect to a database first.");
            return null;
        }
        return checkModel() ? db : null;
    }

    /**
     * @return the question of the box or, if it is empty, the last one asked; <code>null</code> if
     *          there is none.
     */
    private String readQuestion() {
        String question = questionArea.getText().trim();
        if (question.isEmpty()) {
            if (typedQuestions.isEmpty()) {
                GuiUtilities.showWarningMessage(this, "Write a question first.");
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

    private void ask() {
        String question = readQuestion();
        if (question == null || !checkModel()) {
            return;
        }
        // the connection is optional here: without one the model helps to connect
        ADb db = host.getDatabase();
        List<AssistantTurn> turns = List.copyOf(history);
        runTextRequest(question, true, ( onToken, pm ) -> sqlAssistant.help(db, question, turns, options(), onToken, pm));
    }

    private void generate() {
        String question = readQuestion();
        if (question == null) {
            return;
        }
        ADb db = checkDatabase();
        if (db == null) {
            return;
        }
        List<AssistantTurn> turns = List.copyOf(history);
        runSqlRequest(question, true, ( onToken, pm ) -> sqlAssistant.generate(db, question, turns, options(), onToken, pm));
    }

    private void fix() {
        String sql = host.getEditorSql().trim();
        if (sql.isEmpty()) {
            GuiUtilities.showWarningMessage(this, "The SQL editor is empty.");
            return;
        }
        String error = host.getLastError(sql);
        if (error == null) {
            GuiUtilities.showWarningMessage(this,
                    "Run the SQL of the editor first: the fix needs the error the database gives for it.");
            return;
        }
        ADb db = checkDatabase();
        if (db == null) {
            return;
        }
        List<AssistantTurn> turns = List.copyOf(history);
        String turnQuestion = "Fix this SQL:\n```sql\n" + sql + "\n```\nThe database gave this error: " + error;
        runSqlRequest(turnQuestion, false,
                ( onToken, pm ) -> sqlAssistant.fix(db, sql, error, turns, options(), onToken, pm));
    }

    private void explain() {
        String sql = host.getEditorSql().trim();
        if (sql.isEmpty()) {
            GuiUtilities.showWarningMessage(this, "The SQL editor is empty.");
            return;
        }
        ADb db = checkDatabase();
        if (db == null) {
            return;
        }
        List<AssistantTurn> turns = List.copyOf(history);
        String turnQuestion = "Explain this SQL:\n```sql\n" + sql + "\n```";
        runTextRequest(turnQuestion, false, ( onToken, pm ) -> sqlAssistant.explain(db, sql, turns, options(), onToken, pm));
    }

    @FunctionalInterface
    private interface Request<T> {
        T run( Consumer<String> onToken, AssistantProgressMonitor pm ) throws Exception;
    }

    /**
     * @param turnQuestion the question as recorded in the discussion.
     * @param fromQuestionBox <code>true</code> if the request comes from the question box, which is
     *          then cleared for the next question and given it back if the request fails.
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
                    turnNotes.add(null);
                    endTurn();
                } catch (Exception e) {
                    endWithError(e, fromQuestionBox ? turnQuestion : null);
                }
            }
        }.execute();
    }

    private void runSqlRequest( String turnQuestion, boolean fromQuestionBox, Request<SqlAnswer> request ) {
        lastAnswer = null;
        sqlArea.setText("");
        writeWarningLabel.setText(" ");
        startRequest(turnQuestion, true, fromQuestionBox);
        AssistantProgressMonitor pm = monitor;
        new SwingWorker<SqlAnswer, String>(){
            @Override
            protected SqlAnswer doInBackground() throws Exception {
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
                    SqlAnswer answer = get();
                    lastAnswer = answer;
                    // a rejection is told to the model too, as a plain fact a follow-up can use; the
                    // marks of the check are only shown, or the model copies them into its answers
                    String answerText = answer.explanation();
                    String note = null;
                    if (answer.checkError() != null) {
                        answerText += "\n\n(The database rejected this SQL: " + answer.checkError().strip().lines().findFirst()
                                .orElse("") + ")";
                        note = "<p><font color=\"#c0392b\"><b>The database still rejects this SQL:</b> "
                                + MarkdownHtml.escape(answer.checkError()) + "</font></p>";
                    } else if (answer.checked()) {
                        note = "<p><font color=\"#2e7d32\"><i>Checked by the database.</i></font></p>";
                    }
                    history.add(new AssistantTurn(turnQuestion, answerText, answer.sql()));
                    turnNotes.add(note);
                    // models write SQL on one line: laid out for reading, and so it goes to the editor
                    sqlArea.setText(SqlFormatter.format(answer.sql()));
                    sqlArea.setCaretPosition(0);
                    if (answer.checkError() != null) {
                        writeWarningLabel.setText("The database rejects this SQL: " + answer.checkError());
                    } else if (!answer.readOnly()) {
                        writeWarningLabel.setText("This SQL changes the database, or could not be recognized as a query.");
                    }
                    endTurn();
                } catch (Exception e) {
                    endWithError(e, fromQuestionBox ? turnQuestion : null);
                }
            }
        }.execute();
    }

    private void startRequest( String turnQuestion, boolean isSql, boolean fromQuestionBox ) {
        if (fromQuestionBox) {
            questionArea.setText("");
        }
        notice = null;
        pendingQuestion = turnQuestion;
        pendingText.setLength(0);
        pendingIsSql = isSql;
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
     * typed question goes back to the question box, to try again.
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
        Logger.INSTANCE.insertError("SqlAssistantPanel", "Assistant request failed", cause);
        String reason = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        notice = "The request failed: " + reason + ". Check the server and the model in the settings.";
        renderDiscussion();
        endRequest("Failed.");
    }

    private void setRunning( boolean running ) {
        this.running = running;
        askButton.setEnabled(!running);
        settingsButton.setEnabled(!running);
        newConversationButton.setEnabled(!running);
        cancelButton.setEnabled(running);
        insertButton.setEnabled(!running && lastAnswer != null);
        updateDatabaseState();
    }

    private void insertIntoEditor() {
        String sql = sqlArea.getText().trim();
        if (sql.isEmpty() || lastAnswer == null) {
            return;
        }
        if (!lastAnswer.readOnly()) {
            boolean ok = GuiUtilities.showYesNoDialog(this, "This SQL changes the database, or could not be recognized as a query.\n"
                    + "It is only added to the editor, not run. Add it?");
            if (!ok) {
                return;
            }
        } else if (lastAnswer.checkError() != null) {
            boolean ok = GuiUtilities.showYesNoDialog(this,
                    "The database rejects this SQL:\n" + lastAnswer.checkError() + "\n\nAdd it to the editor anyway?");
            if (!ok) {
                return;
            }
        }
        host.addSqlToEditor(sql);
    }

    /**
     * Stop a running request, if any.
     */
    public void cancel() {
        if (monitor != null) {
            monitor.setCanceled(true);
        }
    }
}
