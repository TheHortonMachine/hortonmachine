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
package org.jgrasstools.gears.libs.modules;

import static org.jgrasstools.gears.libs.modules.Variables.PROGRESS_MONITOR_EN;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import oms3.Access;
import oms3.ComponentAccess;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.monitor.GeotoolsProgressMonitorAdapter;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.opengis.util.ProgressListener;

/**
 * Superclass for modules.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class JGTModel implements Process {

    @Description(//
    en = PROGRESS_MONITOR_EN,//
    it = PROGRESS_MONITOR_EN//
    )
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    /**
     * Variable that defines if time is still available or run out.
     * 
     * <p>
     * This variable should be set by modules that "lead" the time
     * chain:<br>
     * <ul>
     *      <li><b>true</b>, when the simulation starts</li>
     *      <li><b>false</b>, when the simulation should end, regardless for which 
     *          reason (data finished, end date reached...)
     *      </li>
     * </ul>
     * </p>
     */
    // TODO check this out???? @Out
    @UI(JGTConstants.ITERATOR_UI_HINT)
    public boolean doProcess = false;

    /**
     * A switch that can enable module resetting.
     * 
     * <p>
     * This variable might be usefull in the case in which 
     * NON-timedependent modules at a certain point should anyways
     * re-read or re-process the data. For example in the case in which
     * a map was already calculated but at a certain point should 
     * be recalculated.
     * </p>
     */
    public boolean doReset = false;

    /**
     * Check on the progress monitor to see if the process was stopped.
     * 
     * <p>Modules can use that internally to exit, if necessary.</p>
     * 
     * @param pm the {@link IJGTProgressMonitor progress monitor}.
     * @return true if the process was stopped.
     */
    protected boolean isCanceled( IJGTProgressMonitor pm ) {
        if (pm.isCanceled()) {
            pm.done();
            return true;
        }
        return false;
    }

    public Map<String, Object> execute( Map<String, Object> input, ProgressListener monitor ) throws ProcessException {
        // the geotools monitor is wrapped into the internal progress monitor
        GeotoolsProgressMonitorAdapter pm = new GeotoolsProgressMonitorAdapter(monitor);
        input.put("pm", pm); //$NON-NLS-1$
        // set the inputs to the model
        ComponentAccess.setInputData(input, this, null);

        // trigger execution of the module
        ComponentAccess.callAnnotated(this, Initialize.class, true);
        ComponentAccess.callAnnotated(this, Execute.class, false);
        ComponentAccess.callAnnotated(this, Finalize.class, true);

        // get the results
        ComponentAccess cA = new ComponentAccess(this);
        Collection<Access> outputs = cA.outputs();

        // and put them into the output map
        HashMap<String, Object> outputMap = new HashMap<String, Object>();
        for( Access access : outputs ) {
            try {
                String fieldName = access.getField().getName();
                Object fieldValue = access.getFieldValue();
                outputMap.put(fieldName, fieldValue);
            } catch (Exception e) {
                throw new ProcessException(e.getLocalizedMessage());
            }
        }
        return outputMap;
    }

    /**
     * Utility method to concatenate conditions with or.
     * 
     * <p>
     * This can be useful for readability (in case of negation).
     * </p>
     * 
     * @param statements a list of statements.
     * @return the final boolean from the or concatenation.
     */
    protected boolean concatOr( boolean... statements ) {
        boolean isTrue = statements[0];
        for( int i = 1; i < statements.length; i++ ) {
            isTrue = isTrue || statements[i];
        }
        return isTrue;
    }

    /**
     * Checks if the passed objects are all != null and if one is null, throws Exception.
     * 
     * @param objects the objects to check.
     */
    protected void checkNull( Object... objects ) {
        for( Object object : objects ) {
            if (object == null) {
                throw new ModelsIllegalargumentException("Mandatory input argument is missing. Check your syntax...", this
                        .getClass().getSimpleName());
            }
        }
    }

    /**
     * Fast default reading of raster from definition. 
     * 
     * <p>If the source format is not supported, and {@link Exception} is thrown.</p>
     * <p>If the source is <code>null</code>, null will be returned.</p>
     * 
     * @param source the definition for the raster source.
     * @return the read {@link GridCoverage2D}.
     * @throws Exception
     */
    protected GridCoverage2D getRaster( String source ) throws Exception {
        if (source == null)
            return null;
        return OmsRasterReader.readRaster(source);
    }

    /**
     * Fast default reading of vector from definition. 
     * 
     * <p>If the source format is not supported, and {@link Exception} is thrown.</p>
     * <p>If the source is <code>null</code>, null will be returned.</p>
     * 
     * @param source the definition to the vector source.
     * @return the read {@link GridCoverage2D}.
     * @throws Exception
     */
    protected SimpleFeatureCollection getVector( String source ) throws Exception {
        if (source == null)
            return null;
        return OmsVectorReader.readVector(source);
    }

    /**
     * Fast default writing of raster to source. 
     * 
     * @param raster the {@link GridCoverage2D} to write.
     * @param source the source to which to write to.
     * @throws Exception
     */
    protected void dumpRaster( GridCoverage2D raster, String source ) throws Exception {
        if (raster == null)
            return;
        OmsRasterWriter.writeRaster(source, raster);
    }

    /**
     * Fast default writing of vector to source. 
     * 
     * @param vector the {@link SimpleFeatureCollection} to write.
     * @param source the source to which to write to.
     * @throws Exception
     */
    protected void dumpVector( SimpleFeatureCollection vector, String source ) throws Exception {
        if (vector == null)
            return;
        OmsVectorWriter.writeVector(source, vector);
    }

    public void help() throws Exception {
        String help = ModelsSupporter.generateHelp(this);
        pm.message(help);
    }

    public void template() throws Exception {
        String help = ModelsSupporter.generateTemplate(this);
        pm.message(help);
    }

}
