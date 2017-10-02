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
 * A progress monitor for printstream based applications, i.e. console or commandline.
 * 
 * <p>This implements both {@link IHMProgressMonitor} and 
 * {@link IProgressMonitor} in order to be used also in the part of
 * the code that needs to stay clean of rcp code.</p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PrintStreamProgressMonitor implements IHMProgressMonitor {

    protected boolean cancelled = false;
    protected final PrintStream outStream;
    protected final PrintStream errStream;
    protected String taskName;
    protected int totalWork;
    protected int runningWork;
    protected int lastPercentage = -1;
    private String prefix = null;

    public PrintStreamProgressMonitor() {
        this(System.out, System.err);
    }

    public PrintStreamProgressMonitor( PrintStream outStream, PrintStream errorStream ) {
        this.outStream = outStream;
        this.errStream = errorStream;
    }

    public PrintStreamProgressMonitor( String prefix, PrintStream outStream, PrintStream errorStream ) {
        this.prefix = prefix;
        this.outStream = outStream;
        this.errStream = errorStream;
    }

    public void beginTask( String name, int totalWork ) {
        this.taskName = name;
        this.totalWork = totalWork;
        runningWork = 0;
        if (prefix != null) {
            outStream.print(prefix);
        }
        outStream.println(taskName);
    }

    public void beginTask( String name ) {
        this.taskName = name;
        this.totalWork = -1;
        runningWork = 0;
        if (prefix != null) {
            outStream.print(prefix);
        }
        outStream.println(taskName);
    }

    public void done() {
        if (prefix != null) {
            outStream.print(prefix);
        }
        outStream.println("Finished.");
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

    public void worked( int work ) {
        if (totalWork == -1) {
            if (prefix != null) {
                outStream.print(prefix);
            }
            outStream.print("..."); //$NON-NLS-1$ 
        } else {
            runningWork = runningWork + work;
            // calculate %
            int percentage = 100 * runningWork / totalWork;
            if (percentage % 10 == 0 && percentage != lastPercentage) {
                if (prefix != null) {
                    outStream.print(prefix);
                }
                outStream.print(percentage + "%... "); //$NON-NLS-1$
                if (prefix != null) {
                    outStream.println();
                }
                lastPercentage = percentage;
            }
        }
    }

    public PrintStream getPrintStream() {
        return outStream;
    }

    public <T> T adapt( Class<T> adaptee ) {
        if (adaptee.isAssignableFrom(PrintStream.class)) {
            return adaptee.cast(outStream);
        }
        return null;
    }

    public void errorMessage( String message ) {
        errStream.println(message);
    }

    public void message( String message ) {
        outStream.println(message);
    }
    
    public void exceptionThrown(String message) {
    }
    
    public void onModuleExit() {
    }

}
