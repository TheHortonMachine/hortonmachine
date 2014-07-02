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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_fPointId_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_inNet_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_inPoints_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_inTca_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_outBasins_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_outNetnum_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_pMode_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETNUMBERING_pThres_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.JGTConstants.*;

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
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureIterator;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;

import com.vividsolutions.jts.geom.Geometry;

@Description(OMSNETNUMBERING_DESCRIPTION)
@Author(name = OMSNETNUMBERING_AUTHORNAMES, contact = OMSNETNUMBERING_AUTHORCONTACTS)
@Keywords(OMSNETNUMBERING_KEYWORDS)
@Label(OMSNETNUMBERING_LABEL)
@Name(OMSNETNUMBERING_NAME)
@Status(OMSNETNUMBERING_STATUS)
@License(OMSNETNUMBERING_LICENSE)
public class OmsNetNumbering extends JGTModel {

    @Description(OMSNETNUMBERING_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSNETNUMBERING_inTca_DESCRIPTION)
    @In
    public GridCoverage2D inTca = null;

    @Description(OMSNETNUMBERING_inNet_DESCRIPTION)
    @In
    public GridCoverage2D inNet = null;

    @Description(OMSNETNUMBERING_inPoints_DESCRIPTION)
    @In
    public SimpleFeatureCollection inPoints = null;

    @Description(OMSNETNUMBERING_pMode_DESCRIPTION)
    @In
    public int pMode = 0;

    @Description(OMSNETNUMBERING_pThres_DESCRIPTION)
    @In
    public double pThres = 0;

    @Description(OMSNETNUMBERING_fPointId_DESCRIPTION)
    @In
    public String fPointId = null;

    @Description(OMSNETNUMBERING_outNetnum_DESCRIPTION)
    @Out
    public GridCoverage2D outNetnum = null;

    @Description(OMSNETNUMBERING_outBasins_DESCRIPTION)
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

        RandomIter netIter = CoverageUtilities.getRandomIterator(inNet);

        RandomIter tcaIter = null;
        if (pMode == 1) {
            tcaIter = CoverageUtilities.getRandomIterator(inTca);
        }

        WritableRaster netNumWR = null;
        if (pMode == 0) {
            netNumWR = ModelsEngine.netNumbering(flowIter, netIter, nCols, nRows, pm);
        } else if (pMode == 1) {
            if (tcaIter == null) {
                throw new ModelsIllegalargumentException("This method needs the map of tca.", this, pm);
            }
            netNumWR = ModelsEngine.netNumberingWithTca(flowIter, netIter, tcaIter, nCols, nRows, pThres, pm);
        } else if (pMode == 2) {
            if (inPoints == null) {
                throw new ModelsIllegalargumentException("This processing mode needs a point featurecollection.", this, pm);
            }
            if (fPointId == null) {
                throw new ModelsIllegalargumentException("This processing mode needs the field of the point ID .", this, pm);
            }
            netNumWR = ModelsEngine.netNumberingWithPoints(flowIter, netIter, nRows, nCols, inPoints, fPointId,
                    inFlow.getGridGeometry(), pm);
        } else {
            // if (attributeVect == null || geomVect == null || tcaIter == null) {
            // throw new ModelsIllegalargumentException(
            // "This processing mode needs a point featurecollection and the map of tca.", this);
            // }
            // netNumWR = ModelsEngine.netNumberingWithPointsAndTca(nstream, flowIter, netIter,
            // tcaIter, pThres, nRows, nCols,
            // attributeVect, geomVect, inFlow.getGridGeometry(), pm);
            throw new ModelsIllegalargumentException("Only pMode 0, 1 and 2 are supported.", this, pm);
        }

        WritableRandomIter netNumIter = RandomIterFactory.createWritable(netNumWR, null);
        WritableRaster basinWR = ModelsEngine.extractSubbasins(flowIter, netIter, netNumIter, nRows, nCols, pm);

        outNetnum = CoverageUtilities.buildCoverage("netnum", netNumWR, regionMap, inFlow.getCoordinateReferenceSystem());
        outBasins = CoverageUtilities.buildCoverage("subbasins", basinWR, regionMap, inFlow.getCoordinateReferenceSystem());
    }
}