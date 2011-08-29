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
package org.jgrasstools.hortonmachine.modules.network.strahler;

import static java.lang.Math.max;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

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
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.modules.ModelsSupporter;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.modules.r.summary.RasterSummary;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureMate;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.hortonmachine.modules.demmanipulation.wateroutlet.Wateroutlet;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

@Description("Calculates the Strahler order on a basin.")
@Author(name = "Erica Ghesla, Antonello Andrea, Rigon Riccardo", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Network, Strahler")
@Label(JGTConstants.NETWORK)
@Name("strahler")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class StrahlerRatios extends JGTModel {

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The map of Strahler.")
    @In
    public GridCoverage2D inStrahler = null;

    @Description("The map of the network.")
    @In
    public SimpleFeatureCollection inNet = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The bifurcation ratio.")
    @Out
    public double outBisfurcation;

    @Description("The area ratio.")
    @Out
    public double outArea;

    @Description("The length ratio.")
    @Out
    public double outLength;

    @Execute
    public void process() throws Exception {
        checkNull(inFlow, inNet, inStrahler);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        RenderedImage flowRI = inFlow.getRenderedImage();
        List<FeatureMate> netList = FeatureUtilities.featureCollectionToMatesList(inNet);

        RasterSummary summary = new RasterSummary();
        summary.inRaster = inStrahler;
        summary.pm = pm;
        summary.process();
        int maxStrahler = summary.outMax.intValue();

        LinkedHashMap<Integer, List<FeatureMate>> strahler2FeaturesMap = new LinkedHashMap<Integer, List<FeatureMate>>();
        for( int i = 1; i < maxStrahler; i++ ) {
            strahler2FeaturesMap.put(i, new ArrayList<FeatureMate>());
        }

        GeometryFactory gf = GeometryUtilities.gf();
        final double[] value = new double[1];
        for( FeatureMate featureMate : netList ) {
            Geometry geometry = featureMate.getGeometry();
            Coordinate[] coordinates = geometry.getCoordinates();
            Coordinate coordinate;
            if (coordinates.length > 2) {
                coordinate = coordinates[coordinates.length / 2];
            } else {
                coordinate = coordinates[coordinates.length - 1];
            }

            inStrahler.evaluate(new Point2D.Double(coordinate.x, coordinate.y), value);

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
                Wateroutlet wateroutlet = new Wateroutlet();
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
                Wateroutlet wateroutlet = new Wateroutlet();
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

        }

        ratioSum = ratioSum / num;
        ratioLengths = ratioLengths / num;
        ratioAreas = ratioAreas / num;

    }

}
