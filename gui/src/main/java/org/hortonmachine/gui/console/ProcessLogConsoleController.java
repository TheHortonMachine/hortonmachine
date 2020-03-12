/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.gui.console;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import org.hortonmachine.gears.utils.processes.ELogStyle;
import org.hortonmachine.gears.utils.processes.IProcessListener;
import org.hortonmachine.gui.utils.GuiUtilities;

/**
 * A console for logging processes.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class ProcessLogConsoleController extends ProcessLogConsoleView implements IProcessListener {
    private static final long serialVersionUID = 1L;

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
        Class<ProcessLogConsoleController> class1 = ProcessLogConsoleController.class;
        ImageIcon trashIcon = new ImageIcon(class1.getResource("/org/hortonmachine/images/trash.gif"));
        ImageIcon copyIcon = new ImageIcon(class1.getResource("/org/hortonmachine/images/copy_edit.gif"));
        ImageIcon stopIcon = new ImageIcon(class1.getResource("/org/hortonmachine/images/progress_stop.gif"));

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
        clearButton.setIcon(trashIcon);

        copyButton.addActionListener(new ActionListener(){
            public void actionPerformed( ActionEvent e ) {
                GuiUtilities.copyToClipboard(logPane.getText());
            }
        });
        copyButton.setIcon(copyIcon);

        stopButton.addActionListener(new ActionListener(){
            public void actionPerformed( ActionEvent e ) {
                stopButton.setEnabled(false);
                killProcess();
            }
        });
        stopButton.setIcon(stopIcon);

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
        // + new DateTime().toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS) + "\n\n");
    }

    public void finishProcess() {
        // System.out.println("\n\nProcess " + processName + " stopped at: "
        // + new DateTime().toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS));
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

    public void onMessage( String message, ELogStyle style ) {
        try {
            // logPane.setText(message);
            if (doc != null) {
                if (!ConsoleMessageFilter.doRemove(message))
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
