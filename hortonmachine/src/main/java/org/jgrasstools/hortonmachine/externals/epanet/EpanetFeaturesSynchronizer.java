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

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

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
import org.geotools.feature.FeatureCollections;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetConstants;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Junctions;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Pipes;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Pumps;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Reservoirs;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Tanks;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Valves;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

@Description("Synchronizes the features of the different epanet layers.")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
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
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("Warning messages if something odd happened but is no error.")
    @Out
    public String outWarning = "";

    private StringBuilder warningBuilder = new StringBuilder();

    private String junctionElevatioAttributeName;
    private String tanksElevationAttributeName;
    private String reservoirHeadAttributeName;
    private String pipesStartNodeAttributeName;
    private String pipesEndNodeAttributeName;
    private String pipesIdAttributeName;
    private String lengthAttributeName;
    private String pumpsStartNodeAttributeName;
    private String pumpsEndNodeAttributeName;
    private String pumpsIdAttributeName;
    private String valvesStartNodeAttributeName;
    private String valvesEndNodeAttributeName;
    private String valvesIdAttributeName;

    @Execute
    @SuppressWarnings("unchecked")
    public void process() throws Exception {
        checkNull(inJunctions, inPipes);

        List<SimpleFeature> junctionsList = toList(inJunctions);
        List<SimpleFeature> tanksList = toList(inTanks);
        List<SimpleFeature> reservoirsList = toList(inReservoirs);
        List<SimpleFeature> pipesList = toList(inPipes);
        List<SimpleFeature> pumpsList = toList(inPumps);
        List<SimpleFeature> valvesList = toList(inValves);

        /*
         * check field names
         */
        junctionElevatioAttributeName = FeatureUtilities.findAttributeName(inJunctions.getSchema(),
                Junctions.ELEVATION.getAttributeName());
        tanksElevationAttributeName = FeatureUtilities.findAttributeName(inTanks.getSchema(),
                Tanks.BOTTOM_ELEVATION.getAttributeName());
        reservoirHeadAttributeName = FeatureUtilities.findAttributeName(inReservoirs.getSchema(),
                Reservoirs.HEAD.getAttributeName());
        pipesStartNodeAttributeName = FeatureUtilities
                .findAttributeName(inPipes.getSchema(), Pipes.START_NODE.getAttributeName());
        pipesEndNodeAttributeName = FeatureUtilities.findAttributeName(inPipes.getSchema(), Pipes.END_NODE.getAttributeName());
        pipesIdAttributeName = FeatureUtilities.findAttributeName(inPipes.getSchema(), Pipes.ID.getAttributeName());
        lengthAttributeName = FeatureUtilities.findAttributeName(inPipes.getSchema(), Pipes.LENGTH.getAttributeName());
        pumpsStartNodeAttributeName = FeatureUtilities
                .findAttributeName(inPumps.getSchema(), Pumps.START_NODE.getAttributeName());
        pumpsEndNodeAttributeName = FeatureUtilities.findAttributeName(inPumps.getSchema(), Pumps.END_NODE.getAttributeName());
        pumpsIdAttributeName = FeatureUtilities.findAttributeName(inPumps.getSchema(), Pumps.ID.getAttributeName());
        valvesStartNodeAttributeName = FeatureUtilities.findAttributeName(inValves.getSchema(),
                Valves.START_NODE.getAttributeName());
        valvesEndNodeAttributeName = FeatureUtilities.findAttributeName(inValves.getSchema(), Valves.END_NODE.getAttributeName());
        valvesIdAttributeName = FeatureUtilities.findAttributeName(inValves.getSchema(), Valves.ID.getAttributeName());

        /*
         * elevations for junctions and tanks on dem
         */
        if (inDem != null) {
            inJunctions = FeatureCollections.newCollection();
            pm.beginTask("Extracting elevations from dem...", junctionsList.size() + tanksList.size() + reservoirsList.size());

            for( SimpleFeature junction : junctionsList ) {
                Geometry geometry = (Geometry) junction.getDefaultGeometry();
                Coordinate coordinate = geometry.getCoordinate();
                double[] dest = new double[]{-9999.0};
                try {
                    inDem.evaluate(new Point2D.Double(coordinate.x, coordinate.y), dest);
                    junction.setAttribute(junctionElevatioAttributeName, dest[0]);
                } catch (Exception e) {
                    appendWarning("No elevation available for junction: ",
                            (String) junction.getAttribute(junctionElevatioAttributeName));
                }
                inJunctions.add(junction);
                pm.worked(1);
            }
            inTanks = FeatureCollections.newCollection();

            for( SimpleFeature tank : tanksList ) {
                Geometry geometry = (Geometry) tank.getDefaultGeometry();
                Coordinate coordinate = geometry.getCoordinate();
                double[] dest = new double[]{-9999.0};
                try {
                    inDem.evaluate(new Point2D.Double(coordinate.x, coordinate.y), dest);
                    tank.setAttribute(tanksElevationAttributeName, dest[0]);
                } catch (Exception e) {
                    appendWarning("No elevation available for tank: ", (String) tank.getAttribute(tanksElevationAttributeName));
                }
                inTanks.add(tank);
                pm.worked(1);
            }
            inReservoirs = FeatureCollections.newCollection();

            for( SimpleFeature reservoir : reservoirsList ) {
                Geometry geometry = (Geometry) reservoir.getDefaultGeometry();
                Coordinate coordinate = geometry.getCoordinate();
                double[] dest = new double[]{-9999.0};
                try {
                    inDem.evaluate(new Point2D.Double(coordinate.x, coordinate.y), dest);
                    reservoir.setAttribute(reservoirHeadAttributeName, dest[0]);
                } catch (Exception e) {
                    appendWarning("No elevation available for reservoir: ",
                            (String) reservoir.getAttribute(reservoirHeadAttributeName));
                }
                inReservoirs.add(reservoir);
                pm.worked(1);
            }
            pm.done();
        }

        /*
         * handle pipes and links to the junctions-tanks-reservoirs
         */
        pm.beginTask("Extracting pipe-nodes links...", pipesList.size());
        for( SimpleFeature pipe : pipesList ) {
            Geometry geometry = (Geometry) pipe.getDefaultGeometry();
            Coordinate[] coordinates = geometry.getCoordinates();
            Coordinate first = coordinates[0];
            Coordinate last = coordinates[coordinates.length - 1];

            SimpleFeature nearestFirst = findWithinTolerance(first, junctionsList, tanksList, reservoirsList);
            if (nearestFirst != null) {
                Object attribute = FeatureUtilities.getAttributeCaseChecked(nearestFirst, Junctions.ID.getAttributeName());
                pipe.setAttribute(pipesStartNodeAttributeName, attribute);
            } else {
                Object attribute = pipe.getAttribute(pipesIdAttributeName);
                appendWarning("No start node found for pipe: ", attribute.toString());
            }
            SimpleFeature nearestLast = findWithinTolerance(last, junctionsList, tanksList, reservoirsList);
            if (nearestLast != null) {
                Object attribute = FeatureUtilities.getAttributeCaseChecked(nearestLast, Junctions.ID.getAttributeName());
                pipe.setAttribute(pipesEndNodeAttributeName, attribute);
            } else {
                Object attribute = pipe.getAttribute(pipesIdAttributeName);
                appendWarning("No end node found for pipe: ", attribute.toString());
            }

            if (nearestFirst != null && nearestLast != null) {
                Object elev1Obj = getElevation(nearestFirst);
                Object elev2Obj = getElevation(nearestLast);
                double length = geometry.getLength();
                if (elev1Obj != null && elev2Obj != null) {
                    if (elev1Obj instanceof Double) {
                        double elev1 = (Double) elev1Obj;
                        double elev2 = (Double) elev2Obj;
                        double length3d = sqrt(pow(abs(elev2 - elev1), 2.0) + pow(length, 2.0));
                        pipe.setAttribute(lengthAttributeName, length3d);
                    }
                } else {
                    // 2D
                    pipe.setAttribute(lengthAttributeName, length);
                }
            }
            pm.worked(1);
        }
        pm.done();

        /*
         * handle pumps
         */
        pm.beginTask("Extracting pumps attributes...", pumpsList.size());
        inPumps = FeatureCollections.newCollection();
        for( SimpleFeature pump : pumpsList ) {
            Geometry geometry = (Geometry) pump.getDefaultGeometry();
            Geometry buffer = geometry.buffer(pTol);

            boolean gotIt = false;
            for( SimpleFeature pipe : pipesList ) {
                Geometry pipeGeom = (Geometry) pipe.getDefaultGeometry();
                if (pipeGeom.intersects(buffer)) {
                    // pump is on pipe
                    Object startNode = pipe.getAttribute(pipesStartNodeAttributeName);
                    pump.setAttribute(pumpsStartNodeAttributeName, startNode);
                    Object endNode = pipe.getAttribute(pipesEndNodeAttributeName);
                    pump.setAttribute(pumpsEndNodeAttributeName, endNode);

                    pipe.setAttribute(pipesIdAttributeName, EpanetConstants.DUMMYPIPE);
                    gotIt = true;
                }
            }
            if (!gotIt) {
                appendWarning("Pump ", (String) pump.getAttribute(pumpsIdAttributeName), " could not be placed on any pipe");
            }
            inPumps.add(pump);
            pm.worked(1);
        }
        pm.done();

        /*
         * handle valves
         */
        pm.beginTask("Extracting valves attributes...", valvesList.size());
        inValves = FeatureCollections.newCollection();
        for( SimpleFeature valve : valvesList ) {
            Geometry geometry = (Geometry) valve.getDefaultGeometry();
            Geometry buffer = geometry.buffer(pTol);

            boolean gotIt = false;
            for( SimpleFeature pipe : pipesList ) {
                Geometry pipeGeom = (Geometry) pipe.getDefaultGeometry();
                if (pipeGeom.intersects(buffer)) {
                    // pump is on pipe
                    Object startNode = pipe.getAttribute(pipesStartNodeAttributeName);
                    valve.setAttribute(valvesStartNodeAttributeName, startNode);
                    Object endNode = pipe.getAttribute(pipesEndNodeAttributeName);
                    valve.setAttribute(valvesEndNodeAttributeName, endNode);
                    // mark pipe as dummy
                    pipe.setAttribute(pipesIdAttributeName, EpanetConstants.DUMMYPIPE);
                    gotIt = true;
                }
            }
            if (!gotIt) {
                appendWarning("Valve ", (String) valve.getAttribute(valvesIdAttributeName), " could not be placed on any pipe");
            }
            inValves.add(valve);
            pm.worked(1);
        }
        pm.done();

        inPipes = FeatureCollections.newCollection();
        for( SimpleFeature pipe : pipesList ) {
            inPipes.add(pipe);
        }

        outWarning = warningBuilder.toString();
    }
    private Object getElevation( SimpleFeature nearestFirst ) {
        Object elevObj = nearestFirst.getAttribute(junctionElevatioAttributeName);
        if (elevObj == null) {
            // try tank
            elevObj = nearestFirst.getAttribute(tanksElevationAttributeName);
        }
        if (elevObj == null) {
            // try
            elevObj = nearestFirst.getAttribute(reservoirHeadAttributeName);
        }
        return elevObj;
    }

    private List<SimpleFeature> toList( SimpleFeatureCollection fc ) {
        List<SimpleFeature> list = new ArrayList<SimpleFeature>();
        if (fc != null)
            list = FeatureUtilities.featureCollectionToList(fc);
        return list;
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

    private void appendWarning( String... msgs ) {
        for( String msg : msgs ) {
            warningBuilder.append(msg);
        }
        warningBuilder.append("\n");
    }

}
