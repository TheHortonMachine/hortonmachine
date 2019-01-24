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

/**
 * The Main Progress Monitor.
 * 
 * <p>
 * This is done in order to be able to use the monitor outside of the
 * eclipse environment.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface IHMProgressMonitor {
    /**
     * Constant indicating an unknown amount of work.
     */
    public final static int UNKNOWN = -1;

    /**
     * Notifies that the main task is beginning.
     * 
     * @param name
     * @param totalWork
     */
    public void beginTask( String name, int totalWork );

    /**
     * Sends out a message.
     * 
     * @param message the message to send out.
     */
    public void message( String message );

    /**
     * Sends out an error message.
     * 
     * @param message the error message to send out.
     */
    public void errorMessage( String message );

    /**
     * Method to be called in case of exception.
     * 
     * @param message the error message to send out.
     */
    public void exceptionThrown(String message);

    /**
     * Notifies that the work is done; that is, either the main task is completed or the user canceled it.
     */
    public void done();

    /**
     * Internal method to handle scaling correctly.
     * 
     * @param work
     */
    public void internalWorked( double work );

    /**
     * Returns whether cancellation of current operation has been requested.
     * 
     * @return
     */
    public boolean isCanceled();

    /**
     * Sets the cancel state to the given value.
     * 
     * @param value
     */
    public void setCanceled( boolean value );

    /**
     * Sets the task name to the given value.
     * 
     * @param name
     */
    public void setTaskName( String name );

    /**
     * Notifies that a subtask of the main task is beginning.
     * 
     * @param name
     */
    public void subTask( String name );

    /**
     * Notifies that a given number of work unit of the main task has been completed.
     * 
     * @param work
     */
    public void worked( int work );

    /**
     * Adapts the monitor to a given class.
     * 
     * @param adaptee the class to which to adapt to.
     * @return the adapted object or null, if it is not assignable.
     */
    public <T> T adapt( Class<T> adaptee );

    /**
     * Method to call on module finalization.
     */
    public void onModuleExit();
}
