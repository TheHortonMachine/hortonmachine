/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.lesto.modules.vegetation;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.DESCR;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.KEYWORDS;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.LABEL;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.NAME;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.doDynamicRadius_DESCR;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.doProcess;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.inDsmDtmDiff_DESCR;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.inDtm_DESCR;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.inLas_DESCR;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.inRoi_DESCR;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.outTops_DESCR;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.pClass_DESCR;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.pElevDiffThres_DESCR;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.pMaxRadius_DESCR;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.pThreshold_DESCR;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hortonmachine.gears.io.las.ALasDataManager;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.StringUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.DsmDtmDiffHelper;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

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

@Description(DESCR)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name(NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@SuppressWarnings("nls")
public class PointCloudMaximaFinder extends HMModel {

    @Description(inLas_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inLas = null;

    @Description(inDtm_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDtm;

    @Description(inRoi_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inRoi;

    @Description(inDsmDtmDiff_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDsmDtmDiff;

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
    @UI(HMConstants.FILEOUT_UI_HINT)
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
            GridCoverage2D inDsmDtmDiffGC = getRaster(inDsmDtmDiff);
            helper = new DsmDtmDiffHelper();
            helper.pElevDiffThres = pElevDiffThres;
            helper.gridGeometry = inDsmDtmDiffGC.getGridGeometry();
            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDsmDtmDiffGC);
            helper.cols = regionMap.getCols();
            helper.rows = regionMap.getRows();
            helper.xres = regionMap.getXres();
            helper.yres = regionMap.getYres();
            helper.dsmDtmDiffIter = CoverageUtilities.getRandomIterator(inDsmDtmDiffGC);
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
