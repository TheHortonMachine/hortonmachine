package org.hortonmachine.gui.console;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.PrintStream;

import javax.swing.JComponent;
import javax.swing.text.BadLocationException;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.PrintStreamProgressMonitor;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.ImageCache;
import org.joda.time.DateTime;

public class LogConsoleController extends LogConsoleView {

    private IHMProgressMonitor pm = null;
    private String processName;
    private PrintStream logAreaPrintStream;

    public LogConsoleController( final IHMProgressMonitor pm ) {
        if (pm != null)
            this.pm = pm;
        init();
    }

    private void init() {
        clearButton.addActionListener(new ActionListener(){
            public void actionPerformed( ActionEvent e ) {
                // clears the text area
                try {
                    logArea.getDocument().remove(0, logArea.getDocument().getLength());
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });
        clearButton.setIcon(ImageCache.getInstance().getImage(ImageCache.TRASH));

        copyButton.addActionListener(new ActionListener(){
            public void actionPerformed( ActionEvent e ) {
                GuiUtilities.copyToClipboard(logArea.getText());
            }
        });
        copyButton.setIcon(ImageCache.getInstance().getImage(ImageCache.COPY));

        stopButton.addActionListener(new ActionListener(){
            public void actionPerformed( ActionEvent e ) {
                if (pm != null)
                    pm.setCanceled(true);
                stopButton.setEnabled(false);
            }
        });
        stopButton.setIcon(ImageCache.getInstance().getImage(ImageCache.PROGRESS_STOP));

        logAreaPrintStream = new PrintStream(new CustomOutputStream(logArea));
        if (pm == null) {
            pm = new PrintStreamProgressMonitor(logAreaPrintStream, logAreaPrintStream);
        }

        // re-assigns standard output stream and error output stream
        System.setOut(logAreaPrintStream);
        System.setErr(logAreaPrintStream);

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

    public PrintStream getLogAreaPrintStream() {
        return logAreaPrintStream;
    }

    public void beginProcess( String name ) {
        processName = name;
        System.out.println("Process " + name + " started at: "
                + new DateTime().toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS) + "\n\n");
    }

    public void finishProcess() {
        System.out.println("\n\nProcess " + processName + " stopped at: "
                + new DateTime().toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS));
        stopButton.setEnabled(false);
    }

    public void stopLogging() {
        System.setOut(System.out);
        System.setErr(System.err);
    }

    public JComponent asJComponent() {
        return this;
    }

    public IHMProgressMonitor getProgressMonitor() {
        return pm;
    }
}
