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
package org.jgrasstools.gears.libs.monitor;

import java.util.logging.Logger;

/**
 * A progress monitor that sends progress to log.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LogProgressMonitor implements IJGTProgressMonitor {
    private static final Logger log = Logger.getLogger("org.jgrasstools");

    protected boolean cancelled = false;
    protected String taskName;
    protected int totalWork;
    protected int runningWork;
    protected int lastPercentage = -1;
    private String prefix = null;

    public LogProgressMonitor() {
        this.prefix = "";
    }
    public LogProgressMonitor( String prefix ) {
        this.prefix = prefix;
    }

    public void beginTask( String name, int totalWork ) {
        this.taskName = name;
        this.totalWork = totalWork;
        runningWork = 0;
        if (prefix != null) {
            taskName = prefix + taskName;
        }
        System.out.println(taskName);
        // log.info(taskName);
    }

    public void beginTask( String name ) {
        this.taskName = name;
        this.totalWork = -1;
        runningWork = 0;
        if (prefix != null) {
            taskName = prefix + taskName;
        }
        System.out.println(taskName);
        // log.info(taskName);
    }

    public void done() {
        String msg = "Finished.";
        if (prefix != null) {
            msg = prefix + msg;
        }

        System.out.println(msg);
        // log.info(msg);
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
            String msg = "...";
            if (prefix != null) {
                msg = prefix + msg;
            }
            System.out.println(msg);
            // log.info(msg);
        } else {
            runningWork = runningWork + work;
            // calculate %
            int percentage = 100 * runningWork / totalWork;
            if (percentage % 10 == 0 && percentage != lastPercentage) {
                String msg = percentage + "%...";
                if (prefix != null) {
                    msg = prefix + msg;
                }
                System.out.println(msg);
                // log.info(msg);
                lastPercentage = percentage;
            }
        }
    }

    public <T> T adapt( Class<T> adaptee ) {
        return null;
    }

    public void errorMessage( String message ) {
        System.err.println(message);
        // log.severe(message);
    }

    public void message( String message ) {
        System.out.println(message);
        // log.info(message);
    }
}
