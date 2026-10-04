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
package org.hortonmachine.cli;

import java.io.PrintStream;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

/**
 * The progress monitor of the command line.
 *
 * <p>The output is meant to be read by people and by other applications, which can rely on these lines:
 * <ul>
 * <li><code>Task: &lt;name&gt;</code> when a task begins;</li>
 * <li><code>Progress: &lt;percentage&gt;%</code> every {@value #STEP}% of a task of known length;</li>
 * <li>any other line is a message of the module.</li>
 * </ul>
 * Errors are written to the error stream.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class CliProgressMonitor implements IHMProgressMonitor {
    public static final String TASK_PREFIX = "Task: ";
    public static final String PROGRESS_PREFIX = "Progress: ";

    /** Percentage between progress lines. */
    public static final int STEP = 5;

    private final PrintStream out;
    private final PrintStream err;
    private boolean canceled = false;
    private int totalWork = UNKNOWN;
    private double runningWork = 0;
    private int lastPercentage = -1;

    public CliProgressMonitor( PrintStream out, PrintStream err ) {
        this.out = out;
        this.err = err;
    }

    @Override
    public void beginTask( String name, int totalWork ) {
        this.totalWork = totalWork;
        runningWork = 0;
        lastPercentage = -1;
        out.println(TASK_PREFIX + name);
    }

    @Override
    public void worked( int work ) {
        internalWorked(work);
    }

    @Override
    public void internalWorked( double work ) {
        if (totalWork <= 0) {
            return;
        }
        runningWork += work;
        int percentage = (int) Math.min(100, 100 * runningWork / totalWork);
        percentage = percentage - percentage % STEP;
        if (percentage > lastPercentage) {
            lastPercentage = percentage;
            out.println(PROGRESS_PREFIX + percentage + "%");
        }
    }

    @Override
    public void done() {
        if (totalWork > 0 && lastPercentage < 100) {
            lastPercentage = 100;
            out.println(PROGRESS_PREFIX + "100%");
        }
        totalWork = UNKNOWN;
    }

    @Override
    public void message( String message ) {
        out.println(message);
    }

    @Override
    public void errorMessage( String message ) {
        err.println(message);
    }

    @Override
    public void exceptionThrown( String message ) {
        err.println(message);
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled( boolean value ) {
        canceled = value;
    }

    @Override
    public void setTaskName( String name ) {
        out.println(TASK_PREFIX + name);
    }

    @Override
    public void subTask( String name ) {
        out.println(name);
    }

    @Override
    public <T> T adapt( Class<T> adaptee ) {
        if (adaptee.isAssignableFrom(PrintStream.class)) {
            return adaptee.cast(out);
        }
        return null;
    }

    @Override
    public void onModuleExit() {
    }
}
