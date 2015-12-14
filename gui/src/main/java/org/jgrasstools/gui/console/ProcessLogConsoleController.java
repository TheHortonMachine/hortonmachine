package org.jgrasstools.gui.console;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import org.jgrasstools.gui.utils.GuiUtilities;

public class ProcessLogConsoleController extends ProcessLogConsoleView implements IProcessListener {

    private String processName;
    private Process process;
    private Runnable finishRunnable;
    private StyledDocument doc;
    private JTextPane logPane;
    private JScrollPane scrollPane;

    public ProcessLogConsoleController() {
        init();
    }

    private void init() {
        logPane = new JTextPane();
        scrollPane = new JScrollPane(logPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        doc = logPane.getStyledDocument();
        logPane.setEditable(false);

        clearButton.addActionListener(new ActionListener(){
            public void actionPerformed( ActionEvent e ) {
                // clears the text area
                try {
                    logPane.getDocument().remove(0, logPane.getDocument().getLength());
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });
        clearButton.setIcon(new ImageIcon("trash.gif"));

        copyButton.addActionListener(new ActionListener(){
            public void actionPerformed( ActionEvent e ) {
                GuiUtilities.copyToClipboard(logPane.getText());
            }
        });
        copyButton.setIcon(new ImageIcon("copy_edit.gif"));

        stopButton.addActionListener(new ActionListener(){
            public void actionPerformed( ActionEvent e ) {
                killProcess();
            }
        });
        stopButton.setIcon(new ImageIcon("progress_stop"));

        setPreferredSize(new Dimension(480, 320));

        addComponentListener(new ComponentListener(){

            public void componentShown( ComponentEvent e ) {
            }

            public void componentResized( ComponentEvent e ) {
            }

            public void componentMoved( ComponentEvent e ) {
            }

            public void componentHidden( ComponentEvent e ) {
                stopLogging();
            }

        });

    }

    protected void killProcess() {
        if (process != null) {
            process.destroy();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void beginProcess( Process process, String name ) {
        this.process = process;
        processName = name;
        // System.out.println("Process " + name + " started at: "
        // + new DateTime().toString(JGTConstants.dateTimeFormatterYYYYMMDDHHMMSS) + "\n\n");
    }

    public void finishProcess() {
        // System.out.println("\n\nProcess " + processName + " stopped at: "
        // + new DateTime().toString(JGTConstants.dateTimeFormatterYYYYMMDDHHMMSS));
        stopButton.setEnabled(false);

        if (finishRunnable != null) {
            finishRunnable.run();
        }
    }

    public void stopLogging() {
    }

    public JComponent asJComponent() {
        return this;
    }

    public void onMessage( String message, LogStyle style ) {
        try {
            // logPane.setText(message);
            if (doc != null) {
                doc.insertString(doc.getLength(), message + "\n", style.getAttributeSet());
                // logPane.setCaretPosition(doc.getLength());
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());

    }

    public void onProcessStopped() {
        finishProcess();
    }

    public void addFinishRunnable( Runnable finishRunnable ) {
        this.finishRunnable = finishRunnable;
    }
}
