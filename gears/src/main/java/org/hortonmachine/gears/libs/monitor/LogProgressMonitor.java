/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.gears.libs.monitor;

import java.io.PrintStream;

/**
 * A progress monitor that sends progress to log.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LogProgressMonitor implements IHMProgressMonitor {

    private static final String DOTS = "...";
    private static final String PERC = "%...";
    protected boolean cancelled = false;
    protected String taskName;
    protected int totalWork;
    protected int runningWork;
    protected volatile int lastPercentage = -1;
    private String prefix = null;
    private PrintStream outStream = System.out;
    private PrintStream errorStream = System.err;

    public LogProgressMonitor() {
        this(null, null, "");
    }

    public LogProgressMonitor( PrintStream outStream, PrintStream errorStream ) {
        this(outStream, errorStream, "");
    }

    public LogProgressMonitor( String prefix ) {
        this(null, null, prefix);
    }

    public LogProgressMonitor( PrintStream outStream, PrintStream errorStream, String prefix ) {
        if (outStream != null)
            this.outStream = outStream;
        if (errorStream != null)
            this.errorStream = errorStream;
        this.prefix = prefix;
    }

    public void beginTask( String name, int totalWork ) {
        this.taskName = name;
        this.totalWork = totalWork;
        runningWork = 0;
        if (prefix != null) {
            taskName = prefix + taskName;
        }
        outStream.println(taskName);
    }

    public void beginTask( String name ) {
        this.taskName = name;
        this.totalWork = -1;
        runningWork = 0;
        if (prefix != null) {
            taskName = prefix + taskName;
        }
        outStream.println(taskName);
    }

    public void done() {
        String msg = "Finished.";
        if (prefix != null) {
            msg = prefix + msg;
        }
        outStream.println(msg);
    }

    public void internalWorked( double work ) {
    }

    public boolean isCanceled() {
        return cancelled;
    }

    public void setCanceled( boolean cancelled ) {
        this.cancelled = cancelled;
    }

    public void setTaskName( String name ) {
        taskName = name;
    }

    public void subTask( String name ) {
    }

    public void worked( int workDone ) {
        if (totalWork == -1) {
            String msg = DOTS;
            if (prefix != null) {
                msg = prefix + msg;
            }
            outStream.println(msg);
        } else {
            runningWork = runningWork + workDone;
            // calculate %
            int percentage = (int) (100 * (runningWork / (float) totalWork));
            if (percentage % 10 == 0) {
                String msg = percentage + PERC;
                if (prefix != null) {
                    msg = prefix + msg;
                }
                if (percentage != lastPercentage) {
                    outStream.println(msg);
                    lastPercentage = percentage;
                }
            }
        }
    }

    public <T> T adapt( Class<T> adaptee ) {
        return null;
    }

    public void errorMessage( String message ) {
        if (prefix != null) {
            errorStream.println(prefix + message);
        } else {
            errorStream.println(message);
        }
    }

    public void message( String message ) {
        if (prefix != null) {
            outStream.println(prefix + message);
        } else {
            outStream.println(message);
        }
    }

    public void exceptionThrown( String message ) {
    }

    public void onModuleExit() {
    }

}
