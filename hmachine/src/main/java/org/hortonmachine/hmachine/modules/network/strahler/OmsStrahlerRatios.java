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
package org.hortonmachine.hmachine.modules.network.strahler;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_inNet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_inStrahler_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_outArea_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_outBisfurcation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_outLength_DESCRIPTION;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

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
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.modules.r.summary.OmsRasterSummary;
import org.hortonmachine.gears.utils.features.FeatureMate;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsWateroutlet;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

@Description(OMSSTRAHLERRATIOS_DESCRIPTION)
@Author(name = OMSSTRAHLERRATIOS_AUTHORNAMES, contact = OMSSTRAHLERRATIOS_AUTHORCONTACTS)
@Keywords(OMSSTRAHLERRATIOS_KEYWORDS)
@Label(OMSSTRAHLERRATIOS_LABEL)
@Name(OMSSTRAHLERRATIOS_NAME)
@Status(OMSSTRAHLERRATIOS_STATUS)
@License(OMSSTRAHLERRATIOS_LICENSE)
public class OmsStrahlerRatios extends HMModel {

    @Description(OMSSTRAHLERRATIOS_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSSTRAHLERRATIOS_inStrahler_DESCRIPTION)
    @In
    public GridCoverage2D inStrahler = null;

    @Description(OMSSTRAHLERRATIOS_inNet_DESCRIPTION)
    @In
    public SimpleFeatureCollection inNet = null;

    @Description(OMSSTRAHLERRATIOS_outBisfurcation_DESCRIPTION)
    @Out
    public double outBisfurcation;

    @Description(OMSSTRAHLERRATIOS_outArea_DESCRIPTION)
    @Out
    public double outArea;

    @Description(OMSSTRAHLERRATIOS_outLength_DESCRIPTION)
    @Out
    public double outLength;

    @Execute
    public void process() throws Exception {
        checkNull(inFlow, inNet, inStrahler);

        List<FeatureMate> netList = FeatureUtilities.featureCollectionToMatesList(inNet);

        OmsRasterSummary summary = new OmsRasterSummary();
        summary.inRaster = inStrahler;
        summary.pm = pm;
        summary.process();
        int maxStrahler = summary.outMax.intValue();

        LinkedHashMap<Integer, List<FeatureMate>> strahler2FeaturesMap = new LinkedHashMap<Integer, List<FeatureMate>>();
        for( int i = 1; i <= maxStrahler; i++ ) {
            strahler2FeaturesMap.put(i, new ArrayList<FeatureMate>());
        }

        final double[] value = new double[1];
        for( FeatureMate featureMate : netList ) {
            Geometry geometry = featureMate.getGeometry();
            Coordinate[] coordinates = geometry.getCoordinates();
            Coordinate coordinate = coordinates[0];

            inStrahler.evaluate(new Point2D.Double(coordinate.x, coordinate.y), value);

            if (HMConstants.isNovalue(value[0]) || value[0] < 1 || value[0] > maxStrahler) {
                throw new ModelsIllegalargumentException("An incorrect value of OmsStrahler was extracted from the map.", this, pm);
            }

            int strahler = (int) value[0];

            List<FeatureMate> matesList = strahler2FeaturesMap.get(strahler);
            matesList.add(featureMate);
        }

        Set<Integer> strahlerSet = strahler2FeaturesMap.keySet();
        Integer[] strahlerArray = strahlerSet.toArray(new Integer[0]);
        double ratioSum = 0;
        double ratioLengths = 0;
        double ratioAreas = 0;
        int num = strahlerArray.length - 1;
        pm.beginTask("Calculating...", num);
        for( int i = 0; i < strahlerArray.length - 1; i++ ) {
            Integer strahler1 = strahlerArray[i];
            Integer strahler2 = strahlerArray[i + 1];

            List<FeatureMate> mates1 = strahler2FeaturesMap.get(strahler1);
            List<FeatureMate> mates2 = strahler2FeaturesMap.get(strahler2);

            // bifurcation
            ratioSum = ratioSum + mates1.size() / (double) mates2.size();

            // lengths
            double lengthAvg1 = 0;
            for( FeatureMate featureMate : mates1 ) {
                lengthAvg1 = lengthAvg1 + featureMate.getGeometry().getLength();
            }
            lengthAvg1 = lengthAvg1 / mates1.size();
            double lengthAvg2 = 0;
            for( FeatureMate featureMate : mates2 ) {
                lengthAvg2 = lengthAvg2 + featureMate.getGeometry().getLength();
            }
            lengthAvg2 = lengthAvg2 / mates2.size();

            ratioLengths = ratioLengths + lengthAvg2 / lengthAvg1;

            // areas
            double areaAvg1 = 0;
            for( FeatureMate featureMate : mates1 ) {
                Coordinate[] coordinates = featureMate.getGeometry().getCoordinates();
                Coordinate c = coordinates[coordinates.length - 2];
                OmsWateroutlet wateroutlet = new OmsWateroutlet();
                wateroutlet.inFlow = inFlow;
                wateroutlet.pEast = c.x;
                wateroutlet.pNorth = c.y;
                wateroutlet.pm = new DummyProgressMonitor();
                wateroutlet.process();
                double outArea = wateroutlet.outArea;
                areaAvg1 = areaAvg1 + outArea;
            }
            areaAvg1 = areaAvg1 / mates1.size();
            double areaAvg2 = 0;
            for( FeatureMate featureMate : mates2 ) {
                Coordinate[] coordinates = featureMate.getGeometry().getCoordinates();
                Coordinate c = coordinates[coordinates.length - 2];
                OmsWateroutlet wateroutlet = new OmsWateroutlet();
                wateroutlet.inFlow = inFlow;
                wateroutlet.pEast = c.x;
                wateroutlet.pNorth = c.y;
                wateroutlet.pm = new DummyProgressMonitor();
                wateroutlet.process();
                double outArea = wateroutlet.outArea;
                areaAvg2 = areaAvg2 + outArea;
            }
            areaAvg2 = areaAvg2 / mates2.size();

            ratioAreas = ratioAreas + areaAvg2 / areaAvg1;
            pm.worked(1);
        }
        pm.done();

        outBisfurcation = ratioSum / num;
        outLength = ratioLengths / num;
        outArea = ratioAreas / num;

    }

    // public static void main( String[] args ) throws Exception {
    //
    // String flow = "";
    // String strahler = "";
    // // String strahler = "";
    // String net = "";
    //
    // OmsStrahlerRatios ratios = new OmsStrahlerRatios();
    // ratios.inFlow = OmsRasterReader.readRaster(flow);
    // ratios.inStrahler = OmsRasterReader.readRaster(strahler);
    // ratios.inNet = OmsVectorReader.readVector(net);
    // ratios.process();
    // System.out.println(ratios.outBisfurcation);
    // System.out.println(ratios.outLength);
    // System.out.println(ratios.outArea);
    // }

}
