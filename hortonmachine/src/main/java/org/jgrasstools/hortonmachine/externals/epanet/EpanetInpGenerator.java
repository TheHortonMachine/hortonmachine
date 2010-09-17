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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Status;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.jgrasstools.gears.libs.exceptions.ModelsIOException;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Junctions;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Pipes;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Pumps;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Reservoirs;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Tanks;
import org.jgrasstools.hortonmachine.externals.epanet.core.EpanetFeatureTypes.Valves;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

@Description("Generates the inp file for an epanet run.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Epanet")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
@SuppressWarnings("nls")
public class EpanetInpGenerator extends JGTModel {

    @Description("The junctions features.")
    @In
    public SimpleFeatureCollection inJunctions = null;

    @Description("The junctions features.")
    @In
    public SimpleFeatureCollection inTanks = null;

    @Description("The tanks features.")
    @In
    public SimpleFeatureCollection inReservoirs = null;

    @Description("The pumps features.")
    @In
    public SimpleFeatureCollection inPumps = null;

    @Description("The valves features.")
    @In
    public SimpleFeatureCollection inValves = null;

    @Description("The pipes features.")
    @In
    public SimpleFeatureCollection inPipes = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The file into which to write the inp.")
    @In
    public String outFile = null;

    private static final String NL = "\n";
    private static final String SPACER = " ";

    @Execute
    public void process() throws Exception {
        checkNull(inJunctions, inTanks, inReservoirs, inPumps, inValves, inPipes, outFile);

        int resSize = inReservoirs.size();
        int tanksSize = inTanks.size();
        if (resSize + tanksSize < 1) {
            throw new ModelsIllegalargumentException("The model needs at least one tanks or reservoir to work.", this);
        }

        File outputFile = new File(outFile);

        BufferedWriter bw = null;
        try {
            pm.beginTask("Generating inp file...", 7);
            bw = new BufferedWriter(new FileWriter(outputFile));
            bw.write("[TITLE]");
            pm.worked(1);

            // TODO better check layer availability
            if (inPipes == null || inJunctions == null) {
                pm.worked(6);
                throw new ModelsIOException("Not all necessary layer are available.", this);
            }

            String junctionsText = handleJunctions(inJunctions);
            bw.write(junctionsText);
            pm.worked(1);
            String pipesText = handlePipes(inPipes);
            bw.write(pipesText);
            pm.worked(1);
            String tanksText = handleTanks(inTanks);
            bw.write(tanksText);
            pm.worked(1);
            String reservoirsText = handleReservoirs(inReservoirs);
            bw.write(reservoirsText);
            pm.worked(1);
            String pumpsText = handlePumps(inPumps);
            bw.write(pumpsText);
            pm.worked(1);
            String valvesText = handleValves(inValves);
            bw.write(valvesText);
        } finally {
            pm.done();
            bw.close();
        }
    }

    private String handleJunctions( SimpleFeatureCollection featureCollection ) throws IOException {
        StringBuilder sbJunctions = new StringBuilder();
        StringBuilder sbJunctionsCoords = new StringBuilder();
        sbJunctions.append("\n\n[JUNCTIONS]\n");
        sbJunctionsCoords.append("\n\n[COORDINATES]\n");
        SimpleFeatureIterator featureIterator = featureCollection.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = (SimpleFeature) featureIterator.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Coordinate coordinate = geometry.getCoordinate();

            // [JUNCTIONS]
            Object dc_id = getAttribute(feature, Junctions.ID.getAttributeName());
            sbJunctions.append(dc_id.toString());
            sbJunctions.append(SPACER);
            Object elevation = getAttribute(feature, Junctions.ELEVATION.getAttributeName());
            sbJunctions.append(elevation.toString());
            sbJunctions.append(SPACER);
            Object demand = getAttribute(feature, Junctions.DEMAND.getAttributeName());
            sbJunctions.append(demand.toString());
            sbJunctions.append(SPACER);
            Object pattern = getAttribute(feature, Junctions.PATTERN.getAttributeName());
            sbJunctions.append(pattern.toString());
            sbJunctions.append(NL);

            // [COORDINATES]
            sbJunctionsCoords.append(dc_id.toString());
            sbJunctionsCoords.append(SPACER);
            sbJunctionsCoords.append(coordinate.x);
            sbJunctionsCoords.append(SPACER);
            sbJunctionsCoords.append(coordinate.y);
            sbJunctionsCoords.append(NL);
        }
        featureIterator.close();

        sbJunctions.append("\n\n");
        sbJunctions.append(sbJunctionsCoords.toString());
        return sbJunctions.toString();
    }

    private String handlePipes( SimpleFeatureCollection featureCollection ) throws IOException {
        StringBuilder sbPipes = new StringBuilder();
        StringBuilder sbPipesVertices = new StringBuilder();
        sbPipes.append("\n\n[PIPES]\n");
        sbPipesVertices.append("\n\n[VERTICES]\n");
        SimpleFeatureIterator featureIterator = featureCollection.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = (SimpleFeature) featureIterator.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Coordinate[] coordinates = geometry.getCoordinates();

            // [PIPES]
            Object dc_id = getAttribute(feature, Pipes.ID.getAttributeName());
            sbPipes.append(dc_id.toString());
            sbPipes.append(SPACER);
            Object node1 = getAttribute(feature, Pipes.NODE1.getAttributeName());
            sbPipes.append(node1.toString());
            sbPipes.append(SPACER);
            Object node2 = getAttribute(feature, Pipes.NODE2.getAttributeName());
            sbPipes.append(node2.toString());
            sbPipes.append(SPACER);
            Object length = getAttribute(feature, Pipes.LENGTH.getAttributeName());
            sbPipes.append(length.toString());
            sbPipes.append(SPACER);
            Object diameter = getAttribute(feature, Pipes.DIAMETER.getAttributeName());
            sbPipes.append(diameter.toString());
            sbPipes.append(SPACER);
            Object roughness = getAttribute(feature, Pipes.ROUGHNESS.getAttributeName());
            sbPipes.append(roughness.toString());
            sbPipes.append(SPACER);
            Object minorloss = getAttribute(feature, Pipes.MINORLOSS.getAttributeName());
            sbPipes.append(minorloss.toString());
            sbPipes.append(SPACER);
            Object status = getAttribute(feature, Pipes.STATUS.getAttributeName());
            sbPipes.append(status.toString());
            sbPipes.append(NL);

            // [VERTICES]
            for( Coordinate coordinate : coordinates ) {
                sbPipesVertices.append(dc_id.toString());
                sbPipesVertices.append(SPACER);
                sbPipesVertices.append(coordinate.x);
                sbPipesVertices.append(SPACER);
                sbPipesVertices.append(coordinate.y);
                sbPipesVertices.append(NL);
            }
        }
        featureIterator.close();

        sbPipes.append("\n\n");
        sbPipes.append(sbPipesVertices.toString());
        return sbPipes.toString();
    }

    private String handleReservoirs( SimpleFeatureCollection featureCollection ) throws IOException {
        StringBuilder sbReservoirs = new StringBuilder();
        sbReservoirs.append("\n\n[RESERVOIRS]\n");
        SimpleFeatureIterator featureIterator = featureCollection.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = (SimpleFeature) featureIterator.next();

            // [RESERVOIRS]
            Object dc_id = getAttribute(feature, Reservoirs.ID.getAttributeName());
            sbReservoirs.append(dc_id.toString());
            sbReservoirs.append(SPACER);
            Object head = getAttribute(feature, Reservoirs.HEAD.getAttributeName());
            sbReservoirs.append(head.toString());
            sbReservoirs.append(SPACER);
            Object pattern = getAttribute(feature, Reservoirs.HEAD_PATTERN.getAttributeName());
            sbReservoirs.append(pattern.toString());
            sbReservoirs.append(NL);
        }
        featureIterator.close();

        return sbReservoirs.toString();
    }

    private String handleTanks( SimpleFeatureCollection featureCollection ) throws IOException {
        StringBuilder sbTanks = new StringBuilder();
        sbTanks.append("\n\n[TANKS]\n");
        SimpleFeatureIterator featureIterator = featureCollection.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = (SimpleFeature) featureIterator.next();

            // [TANKS]
            // ;ID Elevation InitLevel MinLevel MaxLevel Diameter MinVol VolCurve
            Object dc_id = getAttribute(feature, Tanks.ID.getAttributeName());
            sbTanks.append(dc_id.toString());
            sbTanks.append(SPACER);
            Object elevation = getAttribute(feature, Tanks.BOTTOM_ELEVATION.getAttributeName());
            sbTanks.append(elevation.toString());
            sbTanks.append(SPACER);
            Object initLevel = getAttribute(feature, Tanks.INITIAL_WATER_LEVEL.getAttributeName());
            sbTanks.append(initLevel.toString());
            sbTanks.append(SPACER);
            Object minLevel = getAttribute(feature, Tanks.MINIMUM_WATER_LEVEL.getAttributeName());
            sbTanks.append(minLevel.toString());
            sbTanks.append(SPACER);
            Object maxLevel = getAttribute(feature, Tanks.MAXIMUM_WATER_LEVEL.getAttributeName());
            sbTanks.append(maxLevel.toString());
            sbTanks.append(SPACER);
            Object diameter = getAttribute(feature, Tanks.DIAMETER.getAttributeName());
            sbTanks.append(diameter.toString());
            sbTanks.append(SPACER);
            Object minVol = getAttribute(feature, Tanks.MINIMUM_VOLUME.getAttributeName());
            sbTanks.append(minVol.toString());
            sbTanks.append(SPACER);
            Object volCurve = getAttribute(feature, Tanks.VOLUME_CURVE_ID.getAttributeName());
            sbTanks.append(volCurve.toString());
            sbTanks.append(NL);
        }
        featureIterator.close();

        return sbTanks.toString();
    }

    private String handlePumps( SimpleFeatureCollection featureCollection ) throws IOException {
        StringBuilder sbPumps = new StringBuilder();
        sbPumps.append("\n\n[PUMPS]\n");
        SimpleFeatureIterator featureIterator = featureCollection.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = (SimpleFeature) featureIterator.next();

            // [PUMPS]
            // ;ID Node1 Node2 Parameters(key1 value1 key2 value2...)
            Object dc_id = getAttribute(feature, Pumps.ID.getAttributeName());
            sbPumps.append(dc_id.toString());
            sbPumps.append(SPACER);
            Object node1 = getAttribute(feature, Pumps.NODE1.getAttributeName());
            sbPumps.append(node1.toString());
            sbPumps.append(SPACER);
            Object node2 = getAttribute(feature, Pumps.NODE2.getAttributeName());
            sbPumps.append(node2.toString());
            sbPumps.append(SPACER);

            Object power = getAttribute(feature, Pumps.POWER_KW.getAttributeName());
            if (power != null) {
                sbPumps.append("POWER " + power.toString());
                sbPumps.append(SPACER);
            }
            Object head = getAttribute(feature, Pumps.HEAD_ID.getAttributeName());
            if (head != null) {
                sbPumps.append("HEAD " + head.toString());
                sbPumps.append(SPACER);
            }
            Object speed = getAttribute(feature, Pumps.SPEED.getAttributeName());
            if (speed != null) {
                sbPumps.append("SPEED " + speed.toString());
                sbPumps.append(SPACER);
            }
            Object speedPattern = getAttribute(feature, Pumps.SPEED_PATTERN.getAttributeName());
            if (speedPattern != null) {
                sbPumps.append("PATTERN " + speedPattern.toString());
                sbPumps.append(SPACER);
            }
            sbPumps.append(NL);
        }
        featureIterator.close();

        return sbPumps.toString();
    }

    private String handleValves( SimpleFeatureCollection featureCollection ) throws IOException {
        StringBuilder sbValves = new StringBuilder();
        sbValves.append("\n\n[VALVES]\n");
        SimpleFeatureIterator featureIterator = featureCollection.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = (SimpleFeature) featureIterator.next();

            // [VALVES]
            // ;ID Node1 Node2 Diameter Type Setting MinorLoss
            Object dc_id = getAttribute(feature, Valves.ID.getAttributeName());
            sbValves.append(dc_id.toString());
            sbValves.append(SPACER);
            Object node1 = getAttribute(feature, Valves.NODE1.getAttributeName());
            sbValves.append(node1.toString());
            sbValves.append(SPACER);
            Object node2 = getAttribute(feature, Valves.NODE2.getAttributeName());
            sbValves.append(node2.toString());
            sbValves.append(SPACER);
            Object diameter = getAttribute(feature, Valves.DIAMETER.getAttributeName());
            sbValves.append(diameter.toString());
            sbValves.append(SPACER);
            Object type = getAttribute(feature, Valves.TYPE.getAttributeName());
            sbValves.append(type.toString());
            sbValves.append(SPACER);
            Object setting = getAttribute(feature, Valves.SETTING.getAttributeName());
            sbValves.append(setting.toString());
            sbValves.append(SPACER);
            Object minorLoss = getAttribute(feature, Valves.MINORLOSS.getAttributeName());
            sbValves.append(minorLoss.toString());
            sbValves.append(NL);
        }
        featureIterator.close();

        return sbValves.toString();
    }

    private Object getAttribute( SimpleFeature feature, String attributeName ) {
        Object attribute = feature.getAttribute(attributeName);
        if (attribute == null) {
            attribute = feature.getAttribute(attributeName.toUpperCase());
        }
        if (attribute == null) {
            attribute = feature.getAttribute(attributeName.toLowerCase());
        }
        return attribute;
    }

}
