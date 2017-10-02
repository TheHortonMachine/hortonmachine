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

import org.geotools.util.SimpleInternationalString;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;

/**
 * Wrapper for geotools' {@link ProgressListener}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeotoolsProgressMonitorAdapter implements IHMProgressMonitor {

    private final ProgressListener geotoolsMonitor;
    private int totalWork;
    protected int runningWork;
    protected float lastPercentage = -1;

    public GeotoolsProgressMonitorAdapter( ProgressListener geotoolsMonitor ) {
        this.geotoolsMonitor = geotoolsMonitor;
    }

    public void beginTask( String name, int totalWork ) {
        this.totalWork = totalWork;
        runningWork = 0;
        geotoolsMonitor.started();
        InternationalString iName = new SimpleInternationalString(name);
        geotoolsMonitor.setTask(iName);
    }

    public void beginTask( String name ) {
        InternationalString iName = new SimpleInternationalString(name);
        geotoolsMonitor.setTask(iName);
    }

    public void done() {
        geotoolsMonitor.complete();
    }

    public void internalWorked( double work ) {
    }

    public boolean isCanceled() {
        return geotoolsMonitor.isCanceled();
    }

    public void setCanceled( boolean value ) {
        geotoolsMonitor.setCanceled(value);
    }

    public void setTaskName( String name ) {
        InternationalString iName = new SimpleInternationalString(name);
        geotoolsMonitor.setTask(iName);
    }

    public void subTask( String name ) {
        InternationalString iName = new SimpleInternationalString(name);
        geotoolsMonitor.setTask(iName);
    }

    public void worked( int work ) {
        if (totalWork != -1) {
            runningWork = runningWork + work;
            // calculate %
            float percentage = 100 * runningWork / (float) totalWork;
            if (percentage % 10 == 0 && NumericsUtilities.fEq(percentage, lastPercentage)) {
                geotoolsMonitor.progress(percentage);
                lastPercentage = percentage;
            }
        }
    }

    public <T> T adapt( Class<T> adaptee ) {
        if (adaptee.isAssignableFrom(ProgressListener.class)) {
            return adaptee.cast(geotoolsMonitor);
        }
        return null;
    }

    public void errorMessage( String message ) {
    }

    public void message( String message ) {
    }

    public void exceptionThrown( String message ) {
    }

    public void onModuleExit() {
    }

}
