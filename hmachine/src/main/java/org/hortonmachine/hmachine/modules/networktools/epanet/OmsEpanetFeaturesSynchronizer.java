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
package org.hortonmachine.hmachine.modules.networktools.epanet;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_inJunctions_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_inPipes_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_inPumps_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_inReservoirs_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_inTanks_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_inValves_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_outWarning_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETFEATURESSYNCHRONIZER_pTol_DESCRIPTION;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

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
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetConstants;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Junctions;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Pipes;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Pumps;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Reservoirs;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Tanks;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Valves;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

@Description(OMSEPANETFEATURESSYNCHRONIZER_DESCRIPTION)
@Author(name = OMSEPANETFEATURESSYNCHRONIZER_AUTHORNAMES, contact = OMSEPANETFEATURESSYNCHRONIZER_AUTHORCONTACTS)
@Keywords(OMSEPANETFEATURESSYNCHRONIZER_KEYWORDS)
@Label(OMSEPANETFEATURESSYNCHRONIZER_LABEL)
@Name(OMSEPANETFEATURESSYNCHRONIZER_NAME)
@Status(OMSEPANETFEATURESSYNCHRONIZER_STATUS)
@License(OMSEPANETFEATURESSYNCHRONIZER_LICENSE)
public class OmsEpanetFeaturesSynchronizer extends HMModel {

    @Description(OMSEPANETFEATURESSYNCHRONIZER_inJunctions_DESCRIPTION)
    @In
    @Out
    public SimpleFeatureCollection inJunctions = null;

    @Description(OMSEPANETFEATURESSYNCHRONIZER_inTanks_DESCRIPTION)
    @In
    @Out
    public SimpleFeatureCollection inTanks = null;

    @Description(OMSEPANETFEATURESSYNCHRONIZER_inReservoirs_DESCRIPTION)
    @In
    @Out
    public SimpleFeatureCollection inReservoirs = null;

    @Description(OMSEPANETFEATURESSYNCHRONIZER_inPumps_DESCRIPTION)
    @In
    @Out
    public SimpleFeatureCollection inPumps = null;

    @Description(OMSEPANETFEATURESSYNCHRONIZER_inValves_DESCRIPTION)
    @In
    @Out
    public SimpleFeatureCollection inValves = null;

    @Description(OMSEPANETFEATURESSYNCHRONIZER_inPipes_DESCRIPTION)
    @In
    @Out
    public SimpleFeatureCollection inPipes = null;

    @Description(OMSEPANETFEATURESSYNCHRONIZER_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSEPANETFEATURESSYNCHRONIZER_pTol_DESCRIPTION)
    @In
    public double pTol = 0.0001;

    @Description(OMSEPANETFEATURESSYNCHRONIZER_outWarning_DESCRIPTION)
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
        List<SimpleFeature> pipesList = toList(inPipes);

        if (junctionsList.size() == 0 || pipesList.size() == 0) {
            throw new ModelsIllegalargumentException("For the module to run pipes and junctions have to be available.", this);
        }

        List<SimpleFeature> tanksList = new ArrayList<SimpleFeature>();
        if (inTanks != null)
            tanksList = toList(inTanks);
        if (tanksList.size() == 0) {
            inTanks = null;
        }
        List<SimpleFeature> reservoirsList = new ArrayList<SimpleFeature>();
        if (inReservoirs != null)
            reservoirsList = toList(inReservoirs);
        if (reservoirsList.size() == 0) {
            inReservoirs = null;
        }
        List<SimpleFeature> pumpsList = new ArrayList<SimpleFeature>();
        if (inPumps != null)
            pumpsList = toList(inPumps);
        if (pumpsList.size() == 0) {
            inPumps = null;
        }
        List<SimpleFeature> valvesList = new ArrayList<SimpleFeature>();
        if (inValves != null)
            valvesList = toList(inValves);
        if (valvesList.size() == 0) {
            inValves = null;
        }

        /*
         * check field names
         */
        junctionElevatioAttributeName = FeatureUtilities.findAttributeName(inJunctions.getSchema(),
                Junctions.ELEVATION.getAttributeName());
        if (junctionElevatioAttributeName == null) {
            throw new ModelsIllegalargumentException(
                    "The junctions layer is missing the " + Junctions.ELEVATION.getAttributeName() + " attribute.", this);
        }
        String junctionIDAttributeName = FeatureUtilities.findAttributeName(inJunctions.getSchema(),
                Junctions.ID.getAttributeName());
        pipesStartNodeAttributeName = FeatureUtilities.findAttributeName(inPipes.getSchema(),
                Pipes.START_NODE.getAttributeName());
        pipesEndNodeAttributeName = FeatureUtilities.findAttributeName(inPipes.getSchema(), Pipes.END_NODE.getAttributeName());
        pipesIdAttributeName = FeatureUtilities.findAttributeName(inPipes.getSchema(), Pipes.ID.getAttributeName());
        lengthAttributeName = FeatureUtilities.findAttributeName(inPipes.getSchema(), Pipes.LENGTH.getAttributeName());
        if (lengthAttributeName == null) {
            throw new ModelsIllegalargumentException(
                    "The pipes layer is missing the " + Pipes.LENGTH.getAttributeName() + " attribute.", this);
        }

        String tanksIDAttributeName = null;
        if (inTanks != null) {
            tanksElevationAttributeName = FeatureUtilities.findAttributeName(inTanks.getSchema(),
                    Tanks.BOTTOM_ELEVATION.getAttributeName());
            tanksIDAttributeName = FeatureUtilities.findAttributeName(inTanks.getSchema(), Tanks.ID.getAttributeName());
        }
        String reservoirIDAttributeName = null;
        if (inReservoirs != null) {
            reservoirHeadAttributeName = FeatureUtilities.findAttributeName(inReservoirs.getSchema(),
                    Reservoirs.HEAD.getAttributeName());
            reservoirIDAttributeName = FeatureUtilities.findAttributeName(inReservoirs.getSchema(),
                    Reservoirs.ID.getAttributeName());
        }
        if (inPumps != null) {
            pumpsStartNodeAttributeName = FeatureUtilities.findAttributeName(inPumps.getSchema(),
                    Pumps.START_NODE.getAttributeName());
            pumpsEndNodeAttributeName = FeatureUtilities.findAttributeName(inPumps.getSchema(),
                    Pumps.END_NODE.getAttributeName());
            pumpsIdAttributeName = FeatureUtilities.findAttributeName(inPumps.getSchema(), Pumps.ID.getAttributeName());
        }
        if (inValves != null) {
            valvesStartNodeAttributeName = FeatureUtilities.findAttributeName(inValves.getSchema(),
                    Valves.START_NODE.getAttributeName());
            valvesEndNodeAttributeName = FeatureUtilities.findAttributeName(inValves.getSchema(),
                    Valves.END_NODE.getAttributeName());
            valvesIdAttributeName = FeatureUtilities.findAttributeName(inValves.getSchema(), Valves.ID.getAttributeName());
        }

        /*
         * check that no ids are double
         */
        checkIds(junctionsList, junctionIDAttributeName, "Found two junctions with the same ID. Check your data.");
        checkIds(pipesList, pipesIdAttributeName, "Found two pipes with the same ID. Check your data.");
        if (inPumps != null)
            checkIds(pumpsList, pumpsIdAttributeName, "Found two pumpes with the same ID. Check your data.");
        if (inTanks != null)
            checkIds(tanksList, tanksIDAttributeName, "Found two tanks with the same ID. Check your data.");
        if (inValves != null)
            checkIds(valvesList, valvesIdAttributeName, "Found two valves with the same ID. Check your data.");
        if (inReservoirs != null)
            checkIds(reservoirsList, reservoirIDAttributeName, "Found two reservoirs with the same ID. Check your data.");

        /*
         * elevations for junctions and tanks on dem
         */
        if (inElev != null) {
            pm.beginTask("Extracting elevations from dem...", junctionsList.size() + tanksList.size() + reservoirsList.size());

            inJunctions = new DefaultFeatureCollection();
            for( SimpleFeature junction : junctionsList ) {
                Geometry geometry = (Geometry) junction.getDefaultGeometry();
                Coordinate coordinate = geometry.getCoordinate();
                double[] dest = new double[]{-9999.0};
                try {
                    inElev.evaluate(new Point2D.Double(coordinate.x, coordinate.y), dest);
                    junction.setAttribute(junctionElevatioAttributeName, dest[0]);
                } catch (Exception e) {
                    appendWarning("No elevation available for junction: ",
                            (String) junction.getAttribute(junctionElevatioAttributeName));
                }
                ((DefaultFeatureCollection) inJunctions).add(junction);
                pm.worked(1);
            }

            inTanks = new DefaultFeatureCollection();
            for( SimpleFeature tank : tanksList ) {
                Geometry geometry = (Geometry) tank.getDefaultGeometry();
                Coordinate coordinate = geometry.getCoordinate();
                double[] dest = new double[]{-9999.0};
                try {
                    inElev.evaluate(new Point2D.Double(coordinate.x, coordinate.y), dest);
                    tank.setAttribute(tanksElevationAttributeName, dest[0]);
                } catch (Exception e) {
                    appendWarning("No elevation available for tank: ", (String) tank.getAttribute(tanksElevationAttributeName));
                }
                ((DefaultFeatureCollection) inTanks).add(tank);
                pm.worked(1);
            }

            inReservoirs = new DefaultFeatureCollection();
            for( SimpleFeature reservoir : reservoirsList ) {
                Geometry geometry = (Geometry) reservoir.getDefaultGeometry();
                Coordinate coordinate = geometry.getCoordinate();
                double[] dest = new double[]{-9999.0};
                try {
                    inElev.evaluate(new Point2D.Double(coordinate.x, coordinate.y), dest);
                    reservoir.setAttribute(reservoirHeadAttributeName, dest[0]);
                } catch (Exception e) {
                    appendWarning("No elevation available for reservoir: ",
                            (String) reservoir.getAttribute(reservoirHeadAttributeName));
                }
                ((DefaultFeatureCollection) inReservoirs).add(reservoir);
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
            if (coordinates.length < 2) {
                Object attribute = FeatureUtilities.getAttributeCaseChecked(pipe, Pipes.ID.getAttributeName());
                appendWarning("Found pipe with less than 2 coordinates: ", attribute.toString());
                continue;
            }
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

        int dummyIndex = 0;

        /*
         * handle pumps
         */
        pm.beginTask("Extracting pumps attributes...", pumpsList.size());
        inPumps = new DefaultFeatureCollection();
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

                    String dummy = EpanetConstants.DUMMYPIPE.toString() + dummyIndex++;
                    pipe.setAttribute(pipesIdAttributeName, dummy);
                    gotIt = true;
                }
            }
            if (!gotIt) {
                appendWarning("Pump ", (String) pump.getAttribute(pumpsIdAttributeName), " could not be placed on any pipe");
            }
            ((DefaultFeatureCollection) inPumps).add(pump);
            pm.worked(1);
        }
        pm.done();

        /*
         * handle valves
         */
        pm.beginTask("Extracting valves attributes...", valvesList.size());
        inValves = new DefaultFeatureCollection();
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
                    String dummy = EpanetConstants.DUMMYPIPE.toString() + dummyIndex++;
                    pipe.setAttribute(pipesIdAttributeName, dummy);
                    gotIt = true;
                }
            }
            if (!gotIt) {
                appendWarning("Valve ", (String) valve.getAttribute(valvesIdAttributeName), " could not be placed on any pipe");
            }
            ((DefaultFeatureCollection) inValves).add(valve);
            pm.worked(1);
        }
        pm.done();

        inPipes = new DefaultFeatureCollection();
        for( SimpleFeature pipe : pipesList ) {
            ((DefaultFeatureCollection) inPipes).add(pipe);
        }

        outWarning = warningBuilder.toString();
    }

    private void checkIds( List<SimpleFeature> featureList, String attributesName, String msg ) {
        TreeSet<Object> checkTree = new TreeSet<Object>();
        for( SimpleFeature sF : featureList ) {
            Object id = sF.getAttribute(attributesName);
            if (!checkTree.add(id)) {
                throw new ModelsIllegalargumentException(msg + "(" + id + ")", this, pm);
            }
        }
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
