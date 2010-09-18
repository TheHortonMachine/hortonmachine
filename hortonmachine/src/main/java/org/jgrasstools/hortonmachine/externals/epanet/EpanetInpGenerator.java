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
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Status;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.libs.exceptions.ModelsIOException;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
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

    @Description("The options parameters.")
    @In
    public EpanetParametersOptions inOptions = null;

    @Description("The time parameters.")
    @In
    public EpanetParametersTime inTime = null;

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
    private static final String SPACER = "\t\t";

    @Execute
    public void process() throws Exception {
        checkNull(inJunctions, inTanks, inReservoirs, inPumps, inValves, inPipes, outFile);

        int resSize = inReservoirs.size();
        int tanksSize = inTanks.size();
        if (resSize + tanksSize < 1) {
            throw new ModelsIllegalargumentException("The model needs at least one tanks or reservoir to work.", this);
        }

        List<SimpleFeature> junctionsList = FeatureUtilities.featureCollectionToList(inJunctions);
        List<SimpleFeature> tanksList = FeatureUtilities.featureCollectionToList(inTanks);
        List<SimpleFeature> reservoirsList = FeatureUtilities.featureCollectionToList(inReservoirs);
        List<SimpleFeature> pipesList = FeatureUtilities.featureCollectionToList(inPipes);
        List<SimpleFeature> pumpsList = FeatureUtilities.featureCollectionToList(inPumps);
        List<SimpleFeature> valvesList = FeatureUtilities.featureCollectionToList(inValves);

        File outputFile = new File(outFile);

        BufferedWriter bw = null;
        try {
            pm.beginTask("Generating inp file...", 10);
            bw = new BufferedWriter(new FileWriter(outputFile));
            bw.write("[TITLE]");
            pm.worked(1);

            // TODO better check layer availability
            if (inPipes == null || inJunctions == null) {
                pm.worked(8);
                throw new ModelsIOException("Not all necessary layer are available.", this);
            }

            String junctionsText = handleJunctions(junctionsList);
            bw.write(junctionsText);
            pm.worked(1);
            String pipesText = handlePipes(pipesList);
            bw.write(pipesText);
            pm.worked(1);
            String tanksText = handleTanks(tanksList);
            bw.write(tanksText);
            pm.worked(1);
            String reservoirsText = handleReservoirs(reservoirsList);
            bw.write(reservoirsText);
            pm.worked(1);
            String pumpsText = handlePumps(pumpsList);
            bw.write(pumpsText);
            pm.worked(1);
            String valvesText = handleValves(valvesList);
            bw.write(valvesText);

            /*
             * the time section
             */
            pm.worked(1);
            bw.write("\n\n" + EpanetParametersTime.TIMESECTION + "\n");
            Properties timeParameters = inTime.outProperties;
            Set<Entry<Object, Object>> entrySet = timeParameters.entrySet();
            for( Entry<Object, Object> entry : entrySet ) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                bw.write(key + "\t" + value + "\n");
            }
            
            /*
             * the options section
             */
            pm.worked(1);
            bw.write("\n\n" + EpanetParametersOptions.OPTIONSSECTION + "\n");
            Properties optionsParameters = inOptions.outProperties;
            Set<Entry<Object, Object>> optionsEntrySet = optionsParameters.entrySet();
            for( Entry<Object, Object> entry : optionsEntrySet ) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                bw.write(key + "\t" + value + "\n");
            }

            /*
             * coordinates and vertices
             */
            pm.worked(1);
            String coordsText = handleCoordinates(junctionsList, reservoirsList, tanksList);
            bw.write(coordsText);
            pm.worked(1);
            String vertexText = handleVertices(pipesList);
            bw.write(vertexText);

        } finally {
            pm.done();
            bw.close();
        }
    }

    private String handleJunctions( List<SimpleFeature> junctionsList ) throws IOException {
        StringBuilder sbJunctions = new StringBuilder();
        sbJunctions.append("\n\n[JUNCTIONS]\n");
        sbJunctions.append(";ID").append(SPACER);
        sbJunctions.append("ELEV").append(SPACER);
        sbJunctions.append("DEMAND").append(SPACER);
        sbJunctions.append("PATTERN").append(NL);

        for( SimpleFeature junction : junctionsList ) {
            // [JUNCTIONS]
            Object dc_id = getAttribute(junction, Junctions.ID.getAttributeName());
            sbJunctions.append(dc_id.toString());
            sbJunctions.append(SPACER);
            Object elevation = getAttribute(junction, Junctions.ELEVATION.getAttributeName());
            sbJunctions.append(elevation.toString());
            sbJunctions.append(SPACER);
            Object demand = getAttribute(junction, Junctions.DEMAND.getAttributeName());
            sbJunctions.append(demand.toString());
            sbJunctions.append(SPACER);
            Object pattern = getAttribute(junction, Junctions.PATTERN.getAttributeName());
            sbJunctions.append(pattern.toString());
            sbJunctions.append(NL);
        }

        sbJunctions.append("\n\n");
        return sbJunctions.toString();
    }

    private String handlePipes( List<SimpleFeature> pipesList ) throws IOException {
        StringBuilder sbPipes = new StringBuilder();
        sbPipes.append("\n\n[PIPES]\n");
        sbPipes.append(";ID").append(SPACER);
        sbPipes.append("NODE1").append(SPACER);
        sbPipes.append("NODE2").append(SPACER);
        sbPipes.append("LENGTH").append(SPACER);
        sbPipes.append("DIAMETER").append(SPACER);
        sbPipes.append("ROUGHNESS").append(SPACER);
        sbPipes.append("MINORLOSS").append(SPACER);
        sbPipes.append("STATUS").append(NL);

        for( SimpleFeature pipe : pipesList ) {
            // [PIPES]
            Object dc_id = getAttribute(pipe, Pipes.ID.getAttributeName());
            sbPipes.append(dc_id.toString());
            sbPipes.append(SPACER);
            Object node1 = getAttribute(pipe, Pipes.START_NODE.getAttributeName());
            sbPipes.append(node1.toString());
            sbPipes.append(SPACER);
            Object node2 = getAttribute(pipe, Pipes.END_NODE.getAttributeName());
            sbPipes.append(node2.toString());
            sbPipes.append(SPACER);
            Object length = getAttribute(pipe, Pipes.LENGTH.getAttributeName());
            sbPipes.append(length.toString());
            sbPipes.append(SPACER);
            Object diameter = getAttribute(pipe, Pipes.DIAMETER.getAttributeName());
            sbPipes.append(diameter.toString());
            sbPipes.append(SPACER);
            Object roughness = getAttribute(pipe, Pipes.ROUGHNESS.getAttributeName());
            sbPipes.append(roughness.toString());
            sbPipes.append(SPACER);
            Object minorloss = getAttribute(pipe, Pipes.MINORLOSS.getAttributeName());
            sbPipes.append(minorloss.toString());
            sbPipes.append(SPACER);
            Object status = getAttribute(pipe, Pipes.STATUS.getAttributeName());
            sbPipes.append(status.toString());
            sbPipes.append(NL);
        }

        sbPipes.append("\n\n");
        return sbPipes.toString();
    }

    private String handleReservoirs( List<SimpleFeature> reservoirsList ) throws IOException {
        StringBuilder sbReservoirs = new StringBuilder();
        sbReservoirs.append("\n\n[RESERVOIRS]\n");
        sbReservoirs.append(";ID").append(SPACER);
        sbReservoirs.append("HEAD").append(SPACER);
        sbReservoirs.append("PATTERN").append(NL);
        for( SimpleFeature reservoir : reservoirsList ) {
            // [RESERVOIRS]
            Object dc_id = getAttribute(reservoir, Reservoirs.ID.getAttributeName());
            sbReservoirs.append(dc_id.toString());
            sbReservoirs.append(SPACER);
            Object head = getAttribute(reservoir, Reservoirs.HEAD.getAttributeName());
            sbReservoirs.append(head.toString());
            sbReservoirs.append(SPACER);
            Object pattern = getAttribute(reservoir, Reservoirs.HEAD_PATTERN.getAttributeName());
            sbReservoirs.append(pattern.toString());
            sbReservoirs.append(NL);
        }

        return sbReservoirs.toString();
    }

    private String handleTanks( List<SimpleFeature> tanksList ) throws IOException {
        StringBuilder sbTanks = new StringBuilder();
        sbTanks.append("\n\n[TANKS]\n");
        sbTanks.append(";ID").append(SPACER);
        sbTanks.append("ELEV").append(SPACER);
        sbTanks.append("INITLEVEL").append(SPACER);
        sbTanks.append("MINLEVEL").append(SPACER);
        sbTanks.append("MAXLEVEL").append(SPACER);
        sbTanks.append("DIAMETER").append(SPACER);
        sbTanks.append("MINVOL").append(SPACER);
        sbTanks.append("VOLCURVE").append(NL);

        for( SimpleFeature tank : tanksList ) {
            // [TANKS]
            // ;ID Elevation InitLevel MinLevel MaxLevel Diameter MinVol VolCurve
            Object dc_id = getAttribute(tank, Tanks.ID.getAttributeName());
            sbTanks.append(dc_id.toString());
            sbTanks.append(SPACER);
            Object elevation = getAttribute(tank, Tanks.BOTTOM_ELEVATION.getAttributeName());
            sbTanks.append(elevation.toString());
            sbTanks.append(SPACER);
            Object initLevel = getAttribute(tank, Tanks.INITIAL_WATER_LEVEL.getAttributeName());
            sbTanks.append(initLevel.toString());
            sbTanks.append(SPACER);
            Object minLevel = getAttribute(tank, Tanks.MINIMUM_WATER_LEVEL.getAttributeName());
            sbTanks.append(minLevel.toString());
            sbTanks.append(SPACER);
            Object maxLevel = getAttribute(tank, Tanks.MAXIMUM_WATER_LEVEL.getAttributeName());
            sbTanks.append(maxLevel.toString());
            sbTanks.append(SPACER);
            Object diameter = getAttribute(tank, Tanks.DIAMETER.getAttributeName());
            sbTanks.append(diameter.toString());
            sbTanks.append(SPACER);
            Object minVol = getAttribute(tank, Tanks.MINIMUM_VOLUME.getAttributeName());
            sbTanks.append(minVol.toString());
            sbTanks.append(SPACER);
            Object volCurve = getAttribute(tank, Tanks.VOLUME_CURVE_ID.getAttributeName());
            sbTanks.append(volCurve.toString());
            sbTanks.append(NL);
        }

        return sbTanks.toString();
    }

    private String handlePumps( List<SimpleFeature> pumpsList ) throws IOException {
        StringBuilder sbPumps = new StringBuilder();
        sbPumps.append("\n\n[PUMPS]\n");
        sbPumps.append(";ID").append(SPACER);
        sbPumps.append("NODE1").append(SPACER);
        sbPumps.append("NODE2").append(SPACER);
        sbPumps.append("PARAMETERS").append(NL);

        for( SimpleFeature pump : pumpsList ) {
            // [PUMPS]
            // ;ID Node1 Node2 Parameters(key1 value1 key2 value2...)
            Object dc_id = getAttribute(pump, Pumps.ID.getAttributeName());
            sbPumps.append(dc_id.toString());
            sbPumps.append(SPACER);
            Object node1 = getAttribute(pump, Pumps.START_NODE.getAttributeName());
            sbPumps.append(node1.toString());
            sbPumps.append(SPACER);
            Object node2 = getAttribute(pump, Pumps.END_NODE.getAttributeName());
            sbPumps.append(node2.toString());
            sbPumps.append(SPACER);

            Object power = getAttribute(pump, Pumps.POWER.getAttributeName());
            if (power != null) {
                sbPumps.append("POWER " + power.toString());
                sbPumps.append(SPACER);
            }
            Object head = getAttribute(pump, Pumps.HEAD_ID.getAttributeName());
            if (head != null) {
                sbPumps.append("HEAD " + head.toString());
                sbPumps.append(SPACER);
            }
            Object speed = getAttribute(pump, Pumps.SPEED.getAttributeName());
            if (speed != null) {
                sbPumps.append("SPEED " + speed.toString());
                sbPumps.append(SPACER);
            }
            Object speedPattern = getAttribute(pump, Pumps.SPEED_PATTERN.getAttributeName());
            if (speedPattern != null) {
                sbPumps.append("PATTERN " + speedPattern.toString());
                sbPumps.append(SPACER);
            }
            sbPumps.append(NL);
        }

        return sbPumps.toString();
    }

    private String handleValves( List<SimpleFeature> valvesList ) throws IOException {
        StringBuilder sbValves = new StringBuilder();
        sbValves.append("\n\n[VALVES]\n");
        sbValves.append(";ID").append(SPACER);
        sbValves.append("NODE1").append(SPACER);
        sbValves.append("NODE2").append(SPACER);
        sbValves.append("DIAMETER").append(SPACER);
        sbValves.append("TYPE").append(SPACER);
        sbValves.append("SETTING").append(SPACER);
        sbValves.append("MINORLOSS").append(NL);

        for( SimpleFeature valve : valvesList ) {
            // [VALVES]
            // ;ID Node1 Node2 Diameter Type Setting MinorLoss
            Object dc_id = getAttribute(valve, Valves.ID.getAttributeName());
            sbValves.append(dc_id.toString());
            sbValves.append(SPACER);
            Object node1 = getAttribute(valve, Valves.START_NODE.getAttributeName());
            sbValves.append(node1.toString());
            sbValves.append(SPACER);
            Object node2 = getAttribute(valve, Valves.END_NODE.getAttributeName());
            sbValves.append(node2.toString());
            sbValves.append(SPACER);
            Object diameter = getAttribute(valve, Valves.DIAMETER.getAttributeName());
            sbValves.append(diameter.toString());
            sbValves.append(SPACER);
            Object type = getAttribute(valve, Valves.TYPE.getAttributeName());
            sbValves.append(type.toString());
            sbValves.append(SPACER);
            Object setting = getAttribute(valve, Valves.SETTING.getAttributeName());
            sbValves.append(setting.toString());
            sbValves.append(SPACER);
            Object minorLoss = getAttribute(valve, Valves.MINORLOSS.getAttributeName());
            sbValves.append(minorLoss.toString());
            sbValves.append(NL);
        }

        return sbValves.toString();
    }

    private String handleCoordinates( List<SimpleFeature> junctionsList, List<SimpleFeature> reservoirsList,
            List<SimpleFeature> tanksList ) throws IOException {
        ArrayList<SimpleFeature> nodesList = new ArrayList<SimpleFeature>();
        nodesList.addAll(junctionsList);
        nodesList.addAll(reservoirsList);
        nodesList.addAll(tanksList);

        StringBuilder sbJunctionsCoords = new StringBuilder();
        sbJunctionsCoords.append("\n\n[COORDINATES]\n");
        sbJunctionsCoords.append(";NODE").append(SPACER);
        sbJunctionsCoords.append("XCOORD").append(SPACER);
        sbJunctionsCoords.append("YCOORD").append(NL);

        for( SimpleFeature node : nodesList ) {
            Geometry geometry = (Geometry) node.getDefaultGeometry();
            Coordinate coordinate = geometry.getCoordinate();

            // [COORDINATES]
            Object attribute = getAttribute(node, Junctions.ID.getAttributeName());
            sbJunctionsCoords.append(attribute.toString());
            sbJunctionsCoords.append(SPACER);
            sbJunctionsCoords.append(coordinate.x);
            sbJunctionsCoords.append(SPACER);
            sbJunctionsCoords.append(coordinate.y);
            sbJunctionsCoords.append(NL);
        }

        sbJunctionsCoords.append("\n\n");
        return sbJunctionsCoords.toString();
    }

    private String handleVertices( List<SimpleFeature> pipesList ) throws IOException {
        StringBuilder sbPipesVertices = new StringBuilder();
        sbPipesVertices.append("\n\n[VERTICES]\n");
        sbPipesVertices.append(";NODE").append(SPACER);
        sbPipesVertices.append("XCOORD").append(SPACER);
        sbPipesVertices.append("YCOORD").append(NL);

        for( SimpleFeature pipe : pipesList ) {
            Geometry geometry = (Geometry) pipe.getDefaultGeometry();
            Coordinate[] coordinates = geometry.getCoordinates();

            // [PIPES]
            Object dc_id = getAttribute(pipe, Pipes.ID.getAttributeName());

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

        sbPipesVertices.append("\n\n");
        return sbPipesVertices.toString();
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
