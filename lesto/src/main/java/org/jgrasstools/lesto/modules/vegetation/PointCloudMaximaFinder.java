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
package org.jgrasstools.lesto.modules.vegetation;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
import static org.jgrasstools.lesto.modules.vegetation.OmsPointCloudMaximaFinder.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.jgrasstools.gears.io.las.ALasDataManager;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.utils.LasUtils;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.StringUtilities;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.lesto.modules.vegetation.OmsPointCloudMaximaFinder.DsmDtmDiffHelper;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

@Description(DESCR)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name(NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@SuppressWarnings("nls")
public class PointCloudMaximaFinder extends JGTModel {

    @Description(inLas_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inLas = null;

    @Description(inDtm_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inDtm;

    @Description(inRoi_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRoi;

    @Description(inDsmDtmDiff_DESCR)
    @In
    public GridCoverage2D inDsmDtmDiff;

    @Description(pMaxRadius_DESCR)
    @In
    public double pMaxRadius = -1.0;

    @Description(doDynamicRadius_DESCR)
    @In
    public boolean doDynamicRadius = true;

    @Description(pElevDiffThres_DESCR)
    @In
    public double pElevDiffThres = 3.5;

    @Description(pThreshold_DESCR)
    @In
    public double pThreshold = 0.0;

    @Description(pClass_DESCR)
    @In
    public String pClass = null;

    @Description(outTops_DESCR)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outTops = null;

    private AtomicInteger featureIndex = new AtomicInteger();

    @Execute
    public void process() throws Exception {
        checkNull(inLas);

        if (inRoi == null && inDtm == null) {
            throw new ModelsIllegalargumentException("At least one of raster or vector roi is necessary.", this);
        }
        GridCoverage2D inDtmGC = null;
        CoordinateReferenceSystem crs = null;
        List<Geometry> regionGeometries = new ArrayList<Geometry>();
        if (inRoi != null) {
            SimpleFeatureCollection inRoiFC = getVector(inRoi);
            regionGeometries = FeatureUtilities.featureCollectionToGeometriesList(inRoiFC, true, null);
            crs = inRoiFC.getBounds().getCoordinateReferenceSystem();
        } else {
            // use the dtm bounds
            inDtmGC = getRaster(inDtm);
            Polygon polygon = CoverageUtilities.getRegionPolygon(inDtmGC);
            regionGeometries.add(polygon);
            crs = inDtmGC.getCoordinateReferenceSystem();
        }

        DsmDtmDiffHelper helper = null;
        if (inDsmDtmDiff != null) {
            helper = new DsmDtmDiffHelper();
            helper.pElevDiffThres = pElevDiffThres;
            helper.gridGeometry = inDsmDtmDiff.getGridGeometry();
            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDsmDtmDiff);
            helper.cols = regionMap.getCols();
            helper.rows = regionMap.getRows();
            helper.xres = regionMap.getXres();
            helper.yres = regionMap.getYres();
            helper.dsmDtmDiffIter = CoverageUtilities.getRandomIterator(inDsmDtmDiff);
        }

        try (ALasDataManager lasData = ALasDataManager.getDataManager(new File(inLas), inDtmGC, pThreshold, crs)) {
            lasData.open();
            if (pClass != null) {
                double[] classes = StringUtilities.stringToDoubleArray(pClass, ",");
                lasData.setClassesConstraint(classes);
            }

            DefaultFeatureCollection outTopsFC = new DefaultFeatureCollection();
            SimpleFeatureBuilder lasBuilder = LasUtils.getLasFeatureBuilder(crs);

            final int roiNum = regionGeometries.size();
            int index = 1;
            for( final Geometry regionGeometry : regionGeometries ) {
                StringBuilder sb = new StringBuilder();
                sb.append("\nProcessing geometry N.");
                sb.append(index);
                sb.append(" of ");
                sb.append(roiNum);
                sb.append("\n");
                pm.message(sb.toString());
                // remove holes
                LineString exteriorRing = ((Polygon) regionGeometry).getExteriorRing();
                final Polygon regionPolygon = gf.createPolygon(gf.createLinearRing(exteriorRing.getCoordinates()));
                List<LasRecord> pointsInTile = lasData.getPointsInGeometry(regionPolygon, false);
                final int size = pointsInTile.size();
                if (size == 0) {
                    pm.errorMessage("No points processed in tile: " + regionPolygon);
                    continue;
                }
                try {
                    doProcess(pointsInTile, pMaxRadius, doDynamicRadius, helper, outTopsFC, lasBuilder, featureIndex, pm);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            dumpVector(outTopsFC, outTops);
        }
        if (helper != null)
            helper.dsmDtmDiffIter.done();
    }

}
