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
package org.jgrasstools.hortonmachine.externals.epanet;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Junctions;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Pipes;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

@Description("Synchronizes the features of the different epanet layers.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Epanet")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
@SuppressWarnings("nls")
public class EpanetFeaturesSynchronizer extends JGTModel {

    @Description("The junctions features.")
    @In
    @Out
    public SimpleFeatureCollection inJunctions = null;

    @Description("The junctions features.")
    @In
    @Out
    public SimpleFeatureCollection inTanks = null;

    @Description("The tanks features.")
    @In
    @Out
    public SimpleFeatureCollection inReservoirs = null;

    @Description("The pumps features.")
    @In
    @Out
    public SimpleFeatureCollection inPumps = null;

    @Description("The valves features.")
    @In
    @Out
    public SimpleFeatureCollection inValves = null;

    @Description("The pipes features.")
    @In
    @Out
    public SimpleFeatureCollection inPipes = null;

    @Description("The elevation model to extract the elevations.")
    @In
    public GridCoverage2D inDem = null;

    @Description("The tolerance in meters for putting a node on a coordinate.")
    @In
    public double pTol = 1;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Execute
    @SuppressWarnings("unchecked")
    public void process() throws Exception {
        checkNull(inJunctions, inTanks, inReservoirs, inPumps, inValves, inPipes);

        List<SimpleFeature> junctionsList = getFeatures(inJunctions);
        List<SimpleFeature> tanksList = getFeatures(inTanks);
        List<SimpleFeature> reservoirsList = getFeatures(inReservoirs);
        List<SimpleFeature> pipesList = getFeatures(inPipes);

        if (inDem != null) {
            pm.beginTask("Extracting elevations from dem...", junctionsList.size());
            for( SimpleFeature junction : junctionsList ) {
                Geometry geometry = (Geometry) junction.getDefaultGeometry();
                Coordinate coordinate = geometry.getCoordinate();
                double[] dest = inDem.evaluate(new Point2D.Double(coordinate.x, coordinate.y), (double[]) null);
                junction.setAttribute(Junctions.ELEVATION.getAttributeName(), dest[0]);
                pm.worked(1);
            }
            pm.done();
        }

        pm.beginTask("Extracting pipe-nodes links...", pipesList.size());
        for( SimpleFeature pipe : pipesList ) {
            Geometry geometry = (Geometry) pipe.getDefaultGeometry();
            Coordinate[] coordinates = geometry.getCoordinates();
            Coordinate first = coordinates[0];
            Coordinate last = coordinates[coordinates.length - 1];

            double length = geometry.getLength();
            pipe.setAttribute(Pipes.LENGTH.getAttributeName(), length);

            SimpleFeature nearestFirst = findWithinTolerance(first, junctionsList, tanksList, reservoirsList);
            if (nearestFirst != null) {
                Object attribute = nearestFirst.getAttribute(Junctions.ID.getAttributeName());
                pipe.setAttribute(Pipes.START_NODE.getAttributeName(), attribute);
            }
            SimpleFeature nearestLast = findWithinTolerance(last, junctionsList, tanksList, reservoirsList);
            if (nearestLast != null) {
                Object attribute = nearestLast.getAttribute(Junctions.ID.getAttributeName());
                pipe.setAttribute(Pipes.END_NODE.getAttributeName(), attribute);
            }
            pm.worked(1);
        }
        pm.done();

    }

    private SimpleFeature findWithinTolerance( Coordinate c, List<SimpleFeature>... nodesLists ) {
        for( List<SimpleFeature> nodeList : nodesLists ) {
            for( SimpleFeature node : nodeList ) {
                Geometry geometry = (Geometry) node.getDefaultGeometry();
                Coordinate coord = geometry.getCoordinate();
                if (coord.distance(c) <= pTol) {
                    return node;
                }
            }
        }
        return null;
    }

    public List<SimpleFeature> getFeatures( SimpleFeatureCollection collection ) {
        List<SimpleFeature> featuresList = new ArrayList<SimpleFeature>();
        SimpleFeatureIterator featureIterator = collection.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = featureIterator.next();
            featuresList.add(feature);
        }
        featureIterator.close();
        return featuresList;
    }

}
