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
package eu.hydrologis.jgrass.hortonmachine.modules.network.netnumbering;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;
import oms3.annotations.Role;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;

import com.vividsolutions.jts.geom.Geometry;

import eu.hydrologis.jgrass.hortonmachine.libs.exceptions.ModelsIllegalargumentException;
import eu.hydrologis.jgrass.hortonmachine.libs.models.HMModel;
import eu.hydrologis.jgrass.hortonmachine.libs.models.ModelsEngine;
import eu.hydrologis.jgrass.hortonmachine.libs.monitor.DummyProgressMonitor;
import eu.hydrologis.jgrass.hortonmachine.libs.monitor.IHMProgressMonitor;
import eu.hydrologis.jgrass.hortonmachine.utils.coverage.CoverageUtilities;
/**
 * <p>
 * The openmi compliant representation of the netnumbering model. It assign
 * numbers to the network's links and can be used by hillslope2channelattribute
 * to label the hillslope flowing into the link with the same number.
 * </p>
 * <p>
 * <DT><STRONG>Inputs:</STRONG></DT>
 * <DD>
 * <OL>
 * <LI>the map containing the drainage directions (-flow);</LI>
 * <LI>the map containing the channel network (-net);
 * </OL>
 * <P></DD>
 * <DT><STRONG>Returns:</STRONG></DT>
 * <DD>
 * <OL>
 * <LI>the map containing the net with the streams numerated (-netnumber);</LI>
 * <LI>the map containing the sub-basins (-basin).</LI>
 * </OL>
 * <P></DD>
 * <p>
 * Usage mode 0: h.netnumbering --mode 0 --igrass-flow flow --igrass-net net
 * --ograss-netnumber netnumber --ograss-basin basin
 * </p>
 * <p>
 * Usage mode 1: h.netnumbering --mode 1 --thtca value --igrass-flow flow
 * --igrass-net net --igrass-tca tca --ograss-netnumber netnumber--ograss-basin
 * basin
 * </p>
 * <p>
 * Usage mode 2: h.netnumbering --mode 2 --igrass-flow flow --igrass-net net
 * --ishapefile-pointshape "filepath" --ograss-netnumber netnumber--ograss-basin
 * basin
 * </p>
 * <p>
 * Usage mode 3: h.netnumbering --mode 3 --thtca value --igrass-flow flow
 * --igrass-net net --igrass-tca tca --ishapefile-pointshape "filepath"
 * --ograss-netnumber netnumber--ograss-basin basin
 * </p>
 * <p>
 * With color map: h.netnumbering --igrass-flow flow --igrass-net net
 * --ograss-netnumber netnumberx --ograss-basin basinx --ocolor-colornumbers
 * netnumber --ocolor-colorbasins basin
 * <p>
 * <DT><STRONG>Notes:</STRONG><BR>
 * </DT>
 * <DD>The algorithm start from the channel heads which are numbered first.
 * Then, starting again from each source, the drainage direction are followed
 * till a junction is found. If the link downhill the junction was already
 * numbered, a new source is chosen. Otherwise the network is scanned downstream
 * ad a new number is attributed to the link's pixels. Was extensively used for
 * the calculations in [11] (See also: Tca) <BR>
 * </OL></DD>
 * </p>
 * 
 * @author Erica Ghesla - erica.ghesla@ing.unitn.it, Andrea Cozzini, Riccardo
 *         Rigon, (2004).
 */
public class NetNumbering extends HMModel {
    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The map of total contributing area.")
    @In
    public GridCoverage2D inTca = null;

    @Description("The map of the network.")
    @In
    public GridCoverage2D inNet = null;

    @Description("The monitoringpoints features map.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inPoints = null;

    @Role(Role.PARAMETER)
    @Description("The running mode.")
    @In
    public int pMode = 0;

    @Role(Role.PARAMETER)
    @Description("Threshold value on tca map.")
    @In
    public double pThres = 0;

    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new DummyProgressMonitor();

    @Description("The map of netnumbering")
    @Out
    public GridCoverage2D outNetnum = null;

    @Description("The map of subbasins")
    @Out
    public GridCoverage2D outBasins = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outNetnum == null, doReset)) {
            return;
        }
        
        ModelsEngine modelsEngine = new ModelsEngine();
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        int nRows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);
        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);

        RenderedImage netRI = inNet.getRenderedImage();
        RandomIter netIter = RandomIterFactory.create(netRI, null);

        RandomIter tcaIter = null;
        if (pMode == 1) {
            RenderedImage tcaRI = inTca.getRenderedImage();
            tcaIter = RandomIterFactory.create(tcaRI, null);
        }
        ArrayList<Geometry> geomVect = null;
        ArrayList<HashMap<String, ? >> attributeVect = null;
        if (inPoints != null) {
            HashMap<String, Object> geomMap = null;

            List<String> key = new ArrayList<String>();
            SimpleFeatureType ft = inPoints.getSchema();
            for( int i = 0; i < ft.getAttributeCount(); i++ ) {
                AttributeType at = ft.getType(i);
                key.add(at.getName().toString());
            }
            geomVect = new ArrayList<Geometry>();
            attributeVect = new ArrayList<HashMap<String, ? >>();
            FeatureIterator<SimpleFeature> featureIterator = inPoints.features();
            while( featureIterator.hasNext() ) {
                SimpleFeature feature = featureIterator.next();
                geomMap = new HashMap<String, Object>();
                for( int i = 0; i < feature.getAttributeCount(); i++ ) {
                    Object attribute = feature.getAttribute(i);
                    if (attribute != null) {
                        feature.getAttribute(i).getClass();
                        if (!(attribute instanceof Geometry))
                            geomMap.put(key.get(i), attribute);
                    } else {
                        geomMap.put(key.get(i), "null");
                    }
                }
                geomMap.put("id", feature.getID());
                geomVect.add((Geometry) feature.getDefaultGeometry());
                attributeVect.add(geomMap);
                geomMap = null;
            }
            inPoints.close(featureIterator);
        }

        ArrayList<Integer> nstream = new ArrayList<Integer>();

        WritableRaster netNumWR = null;
        if (pMode == 0) {
            netNumWR = modelsEngine.netNumbering(nstream, flowIter, netIter, nCols, nRows, pm);
        } else if (pMode == 1) {
            if (tcaIter == null) {
                throw new ModelsIllegalargumentException("This method needs the map of tca.", this.getClass().getSimpleName());
            }
            netNumWR = modelsEngine.netNumberingWithTca(nstream, flowIter, netIter, tcaIter, nCols, nRows, pThres, pm);
        } else if (pMode == 2) {
            if (attributeVect == null || geomVect == null) {
                throw new ModelsIllegalargumentException("This processing mode needs a point featurecollection.", this.getClass().getSimpleName());
            }
            netNumWR = modelsEngine.netNumberingWithPoints(nstream, flowIter, netIter, nRows, nCols, attributeVect, geomVect, inFlow.getGridGeometry(), pm);
        } else {
            if (attributeVect == null || geomVect == null || tcaIter == null) {
                throw new ModelsIllegalargumentException("This processing mode needs a point featurecollection and the map of tca.", this.getClass().getSimpleName());
            }
            netNumWR = modelsEngine.netNumberingWithPointsAndTca(nstream, flowIter, netIter, tcaIter, pThres, nRows, nCols, attributeVect, geomVect, inFlow.getGridGeometry(), pm);
        }

        WritableRandomIter netNumIter = RandomIterFactory.createWritable(netNumWR, null);
        WritableRaster basinWR = modelsEngine.extractSubbasins(flowIter, netIter, netNumIter, nRows, nCols, pm);

        outNetnum = CoverageUtilities.buildCoverage("netnum", netNumWR, regionMap, inFlow.getCoordinateReferenceSystem());
        outBasins = CoverageUtilities.buildCoverage("subbasins", basinWR, regionMap, inFlow.getCoordinateReferenceSystem());
    }
}
