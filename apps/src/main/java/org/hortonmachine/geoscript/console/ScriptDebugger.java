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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.groovy.runtime.FormatHelper;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;

import groovy.lang.Binding;

/**
 * The debugger of the console scripts.
 *
 * <p>
 * Scripts run in debug mode are compiled with a hook before each statement (see
 * {@link DebugCustomizer}), that calls {@link #shouldPause(int)} and, if needed,
 * {@link #pause(int, Map)} with the local variables visible at that point. The script thread then
 * waits until the gui asks to continue or to step.
 *
 * <p>
 * The depth used by step over and step out is the number of stack frames that belong to the
 * script source (the script, its methods, closures and classes), so that the frames of groovy and
 * of the called java code do not count.
 *
 * <p>Developed by Andrea Antonello with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class ScriptDebugger {

    private static final int SHORT_VALUE_SIZE = 200;
    private static final int LONG_VALUE_SIZE = 5000;

    /**
     * Receives the debugger events, in the script thread.
     */
    public interface Listener {
        /**
         * The script paused.
         *
         * @param line the line of the editor, 1 based.
         * @param variables the variables visible at the pause, locals first.
         */
        void paused( int line, List<Variable> variables );

        /**
         * The script resumed after a pause.
         */
        void resumed();
    }

    /**
     * A variable seen at a pause, described when it is captured, in the script thread.
     */
    public static class Variable {
        public final String name;
        public final boolean local;
        public final String type;
        public final String shortValue;
        public final String longValue;
        public final Object value;

        private Variable( String name, boolean local, Object value ) {
            this.name = name;
            this.local = local;
            this.value = value;
            this.type = value == null ? "" : typeName(value.getClass());
            this.longValue = describe(value, LONG_VALUE_SIZE);
            String oneLine = longValue.replaceAll("\\s*\\R\\s*", " ");
            this.shortValue = oneLine.length() > SHORT_VALUE_SIZE ? oneLine.substring(0, SHORT_VALUE_SIZE) + "..." : oneLine;
        }
    }

    private enum Mode {
        RUN, STEP_INTO, STEP_OVER, STEP_OUT
    }

    private static volatile ScriptDebugger active;

    private final Binding binding;
    private final Listener listener;
    private final Set<Integer> breakpoints = ConcurrentHashMap.newKeySet();
    private volatile Mode mode = Mode.RUN;
    private volatile String scriptFileName;
    private int pausedDepth;
    private boolean resumeRequested;

    /**
     * @param binding the binding of the scripts, whose variables are shown with the locals.
     * @param listener the listener of the pauses.
     */
    public ScriptDebugger( Binding binding, Listener listener ) {
        this.binding = binding;
        this.listener = listener;
    }

    /**
     * Called by the instrumented scripts before each statement.
     *
     * @param line the line of the editor, 1 based.
     * @return <code>true</code> if the script has to pause at this statement.
     */
    public static boolean shouldPause( int line ) {
        ScriptDebugger debugger = active;
        return debugger != null && debugger.isPauseNeeded(line);
    }

    /**
     * Called by the instrumented scripts when {@link #shouldPause(int)} said so.
     *
     * @param line the line of the editor, 1 based.
     * @param locals the local variables visible at the statement.
     * @throws InterruptedException if the script is interrupted while paused.
     */
    public static void pause( int line, Map<String, Object> locals ) throws InterruptedException {
        ScriptDebugger debugger = active;
        if (debugger != null) {
            debugger.doPause(line, locals);
        }
    }

    /**
     * Start debugging a script. Only one script at a time can be debugged.
     *
     * @param scriptFileName the file name the script is compiled with, to tell its stack frames.
     */
    public void start( String scriptFileName ) {
        this.scriptFileName = scriptFileName;
        mode = Mode.RUN;
        active = this;
    }

    /**
     * Stop debugging, once the script ended.
     */
    public void stop() {
        if (active == this) {
            active = null;
        }
    }

    /**
     * @param lines the lines with a breakpoint, 1 based.
     */
    public void setBreakpoints( Collection<Integer> lines ) {
        breakpoints.clear();
        breakpoints.addAll(lines);
    }

    /**
     * Run up to the next breakpoint.
     */
    public void resume() {
        release(Mode.RUN);
    }

    /**
     * Pause at the next statement, also inside the called methods and closures.
     */
    public void stepInto() {
        release(Mode.STEP_INTO);
    }

    /**
     * Pause at the next statement of the same method or closure, or of the callers.
     */
    public void stepOver() {
        release(Mode.STEP_OVER);
    }

    /**
     * Pause at the next statement of the callers.
     */
    public void stepOut() {
        release(Mode.STEP_OUT);
    }

    private synchronized void release( Mode newMode ) {
        mode = newMode;
        resumeRequested = true;
        notifyAll();
    }

    private boolean isPauseNeeded( int line ) {
        switch( mode ) {
        case STEP_INTO:
            return true;
        case STEP_OVER:
            return breakpoints.contains(line) || scriptDepth() <= pausedDepth;
        case STEP_OUT:
            return breakpoints.contains(line) || scriptDepth() < pausedDepth;
        default:
            return breakpoints.contains(line);
        }
    }

    private synchronized void doPause( int line, Map<String, Object> locals ) throws InterruptedException {
        pausedDepth = scriptDepth();
        resumeRequested = false;
        listener.paused(line, collectVariables(locals));
        try {
            while( !resumeRequested ) {
                wait();
            }
        } finally {
            listener.resumed();
        }
    }

    private List<Variable> collectVariables( Map<String, Object> locals ) {
        List<Variable> variables = new ArrayList<>();
        for( Map.Entry<String, Object> local : locals.entrySet() ) {
            variables.add(new Variable(local.getKey(), true, local.getValue()));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> bindingVariables = binding.getVariables();
        for( Map.Entry<String, Object> variable : bindingVariables.entrySet() ) {
            if (!locals.containsKey(variable.getKey())) {
                variables.add(new Variable(variable.getKey(), false, variable.getValue()));
            }
        }
        return variables;
    }

    private int scriptDepth() {
        String fileName = scriptFileName;
        long depth = StackWalker.getInstance().walk(frames -> frames.filter(f -> fileName.equals(f.getFileName())).count());
        return (int) depth;
    }

    private static String typeName( Class< ? > type ) {
        return type.isArray() ? typeName(type.getComponentType()) + "[]" : type.getSimpleName();
    }

    /**
     * Describe a value, without the huge output of some toString.
     *
     * @param value the value.
     * @param maxSize the size after which the description is cut.
     * @return the description.
     */
    static String describe( Object value, int maxSize ) {
        try {
            if (value instanceof GridCoverage2D) {
                GridCoverage2D coverage = (GridCoverage2D) value;
                CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
                return coverage.getRenderedImage().getWidth() + " x " + coverage.getRenderedImage().getHeight() + " cells, "
                        + (crs != null ? crs.getName().toString() : "no crs");
            }
            String description = FormatHelper.format(value, false, maxSize);
            return description.length() > maxSize ? description.substring(0, maxSize) + "..." : description;
        } catch (Throwable e) {
            return "<" + e.getClass().getSimpleName() + " while describing the value>";
        }
    }
}
