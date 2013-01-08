/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.network.netnumbering;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;

import com.vividsolutions.jts.geom.Geometry;

@Description("Assigns the numbers to the network's links.")
@Documentation("OmsNetNumbering.html")
@Author(name = "Daniele Andreis, Antonello Andrea, Erica Ghesla, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Network, SplitSubbasins")
@Label(JGTConstants.NETWORK)
@Name("netnum")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class OmsNetNumbering extends JGTModel {
    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The map of total contributing area.")
    @In
    public GridCoverage2D inTca = null;

    @Description("The map of the network.")
    @In
    public GridCoverage2D inNet = null;

    @Description("The monitoringpoints vector map.")
    @In
    public SimpleFeatureCollection inPoints = null;

    @Description("The running mode: 0: standard way (default); 1: with a threshold on TCA; 2: in a set of defined monitoring points")
    @In
    public int pMode = 0;

    @Description("Threshold value on tca map.")
    @In
    public double pThres = 0;
    
    @Description("The name of the node id field in mode 2.")
    @In
    public String fPointId = null;
    
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
        checkNull(inFlow, inNet);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

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
            featureIterator.close();
        }

        ArrayList<Integer> nstream = new ArrayList<Integer>();

        WritableRaster netNumWR = null;
        if (pMode == 0) {
            netNumWR = ModelsEngine.netNumbering(nstream, flowIter, netIter, nCols, nRows, pm);
        } else if (pMode == 1) {
            if (tcaIter == null) {
                throw new ModelsIllegalargumentException("This method needs the map of tca.", this);
            }
            netNumWR = ModelsEngine.netNumberingWithTca(nstream, flowIter, netIter, tcaIter, nCols, nRows, pThres, pm);
        } else if (pMode == 2) {
            if (attributeVect == null || geomVect == null) {
                throw new ModelsIllegalargumentException("This processing mode needs a point featurecollection.", this);
            }
            if(fPointId==null){
            	throw new ModelsIllegalargumentException("This processing mode needs the field of the point ID .", this);
            }
            netNumWR = ModelsEngine.netNumberingWithPoints(nstream, flowIter, netIter, nRows, nCols, attributeVect, geomVect,
                    inFlow.getGridGeometry(),fPointId, pm);
        } else {
            // if (attributeVect == null || geomVect == null || tcaIter == null) {
            // throw new ModelsIllegalargumentException(
            // "This processing mode needs a point featurecollection and the map of tca.", this);
            // }
            // netNumWR = ModelsEngine.netNumberingWithPointsAndTca(nstream, flowIter, netIter,
            // tcaIter, pThres, nRows, nCols,
            // attributeVect, geomVect, inFlow.getGridGeometry(), pm);
            throw new ModelsIllegalargumentException("Only pMode 0, 1 and 2 are supported.", this);
        }

        WritableRandomIter netNumIter = RandomIterFactory.createWritable(netNumWR, null);
        WritableRaster basinWR = ModelsEngine.extractSubbasins(flowIter, netIter, netNumIter, nRows, nCols, pm);

        outNetnum = CoverageUtilities.buildCoverage("netnum", netNumWR, regionMap, inFlow.getCoordinateReferenceSystem());
        outBasins = CoverageUtilities.buildCoverage("subbasins", basinWR, regionMap, inFlow.getCoordinateReferenceSystem());
    }
}