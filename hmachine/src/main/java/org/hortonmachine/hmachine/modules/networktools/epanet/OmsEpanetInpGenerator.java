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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_inControl_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_inDemand_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_inExtras_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_inJunctions_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_inOptions_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_inPipes_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_inPumps_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_inReservoirs_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_inRules_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_inTanks_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_inTime_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_inValves_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETINPGENERATOR_outFile_DESCRIPTION;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
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

@Description(OMSEPANETINPGENERATOR_DESCRIPTION)
@Author(name = OMSEPANETINPGENERATOR_AUTHORNAMES, contact = OMSEPANETINPGENERATOR_AUTHORCONTACTS)
@Keywords(OMSEPANETINPGENERATOR_KEYWORDS)
@Label(OMSEPANETINPGENERATOR_LABEL)
@Name(OMSEPANETINPGENERATOR_NAME)
@Status(OMSEPANETINPGENERATOR_STATUS)
@License(OMSEPANETINPGENERATOR_LICENSE)
public class OmsEpanetInpGenerator extends HMModel {

    @Description(OMSEPANETINPGENERATOR_inOptions_DESCRIPTION)
    @In
    public OmsEpanetParametersOptions inOptions = null;

    @Description(OMSEPANETINPGENERATOR_inTime_DESCRIPTION)
    @In
    public OmsEpanetParametersTime inTime = null;

    @Description(OMSEPANETINPGENERATOR_inJunctions_DESCRIPTION)
    @In
    public SimpleFeatureCollection inJunctions = null;

    @Description(OMSEPANETINPGENERATOR_inTanks_DESCRIPTION)
    @In
    public SimpleFeatureCollection inTanks = null;

    @Description(OMSEPANETINPGENERATOR_inReservoirs_DESCRIPTION)
    @In
    public SimpleFeatureCollection inReservoirs = null;

    @Description(OMSEPANETINPGENERATOR_inPumps_DESCRIPTION)
    @In
    public SimpleFeatureCollection inPumps = null;

    @Description(OMSEPANETINPGENERATOR_inValves_DESCRIPTION)
    @In
    public SimpleFeatureCollection inValves = null;

    @Description(OMSEPANETINPGENERATOR_inPipes_DESCRIPTION)
    @In
    public SimpleFeatureCollection inPipes = null;

    @Description(OMSEPANETINPGENERATOR_inExtras_DESCRIPTION)
    @In
    public String inExtras = null;

    @Description(OMSEPANETINPGENERATOR_inDemand_DESCRIPTION)
    @In
    public String inDemand = null;

    @Description(OMSEPANETINPGENERATOR_inControl_DESCRIPTION)
    @In
    public String inControl = null;

    @Description(OMSEPANETINPGENERATOR_inRules_DESCRIPTION)
    @In
    public String inRules = null;

    @Description(OMSEPANETINPGENERATOR_outFile_DESCRIPTION)
    @In
    public String outFile = null;

    private static final String NL = "\n";
    private static final String SPACER = "\t\t";

    private HashMap<String, String> curveId2Path = new HashMap<String, String>();
    private HashMap<String, String> patternId2Path = new HashMap<String, String>();
    private HashMap<String, String> demandId2Path = new HashMap<String, String>();

    private List<String> curvesFilesList = new ArrayList<String>();
    private List<String> patternsFilesList = new ArrayList<String>();

    private BufferedWriter bwInp = null;
    private BufferedWriter bwEpanetInp = null;

    @Execute
    public void process() throws Exception {
        checkNull(inJunctions, inPipes, outFile);

        if (inReservoirs == null) {
            inReservoirs = new DefaultFeatureCollection();
        }
        if (inTanks == null) {
            inTanks = new DefaultFeatureCollection();
        }
        if (inPumps == null) {
            inPumps = new DefaultFeatureCollection();
        }
        if (inValves == null) {
            inValves = new DefaultFeatureCollection();
        }

        int resSize = inReservoirs.size();
        int tanksSize = inTanks.size();
        if (resSize + tanksSize < 1) {
            throw new ModelsIllegalargumentException("The model needs at least one tanks or reservoir to work.", this, pm);
        }

        if (inExtras != null) {
            handleCurves();
            handlePatterns();
            handleDemands();
        }

        List<SimpleFeature> junctionsList = FeatureUtilities.featureCollectionToList(inJunctions);
        List<SimpleFeature> tanksList = FeatureUtilities.featureCollectionToList(inTanks);
        List<SimpleFeature> reservoirsList = FeatureUtilities.featureCollectionToList(inReservoirs);
        List<SimpleFeature> pipesList = FeatureUtilities.featureCollectionToList(inPipes);
        List<SimpleFeature> pumpsList = FeatureUtilities.featureCollectionToList(inPumps);
        List<SimpleFeature> valvesList = FeatureUtilities.featureCollectionToList(inValves);

        File outputFile = new File(outFile);
        String name = outputFile.getName();
        if (name.indexOf('.') != -1) {
            name = FileUtilities.getNameWithoutExtention(outputFile);
        }
        File outputEpanetFile = new File(outputFile.getParentFile(), name + "_epanet.inp");

        try {
            pm.beginTask("Generating inp file...", 15);
            bwInp = new BufferedWriter(new FileWriter(outputFile));
            bwEpanetInp = new BufferedWriter(new FileWriter(outputEpanetFile));

            write("[TITLE]");
            pm.worked(1);
            if (junctionsList.size() > 0) {
                String junctionsText = handleJunctions(junctionsList);
                write(junctionsText);
            }
            pm.worked(1);
            if (reservoirsList.size() > 0) {
                String reservoirsText = handleReservoirs(reservoirsList);
                write(reservoirsText);
            }
            pm.worked(1);
            if (tanksList.size() > 0) {
                String tanksText = handleTanks(tanksList);
                write(tanksText);
            }
            pm.worked(1);
            if (pumpsList.size() > 0) {
                String pumpsText = handlePumps(pumpsList);
                write(pumpsText);
            }
            pm.worked(1);
            if (valvesList.size() > 0) {
                String valvesText = handleValves(valvesList);
                write(valvesText);
            }
            pm.worked(2);
            if (pipesList.size() > 0) {
                String pipesText = handlePipes(pipesList);
                write(pipesText);
                String pipeDemandsText = handlePipedemands(pipesList, valvesList, pumpsList);
                write(pipeDemandsText, true);
            }

            /*
             * the demands section
             */
            pm.worked(1);
            if (inDemand != null) {
                write("\n\n[DEMANDS]\n");
                String demandSection = FileUtilities.readFile(new File(inDemand));
                write(demandSection);
            }

            /*
             * the controls section
             */
            pm.worked(1);
            if (inControl != null) {
                write("\n\n[CONTROLS]\n");
                String demandSection = FileUtilities.readFile(new File(inControl));
                write(demandSection);
            }

            /*
             * the rules section
             */
            pm.worked(1);
            if (inRules != null) {
                write("\n\n[RULES]\n");
                String demandSection = FileUtilities.readFile(new File(inRules));
                write(demandSection);
            }

            /*
             * the patterns section
             */
            pm.worked(1);
            write("\n\n[PATTERNS]\n");
            for( String patternsFilePath : patternsFilesList ) {
                String patternString = FileUtilities.readFile(new File(patternsFilePath));
                write(patternString);
            }

            /*
             * the curves section
             */
            pm.worked(1);
            write("\n\n[CURVES]\n");
            for( String curveFilePath : curvesFilesList ) {
                String curveString = FileUtilities.readFile(new File(curveFilePath));
                write(curveString);
            }

            /*
             * the time section
             */
            pm.worked(1);
            write("\n\n" + OmsEpanetParametersTime.TIMESECTION + "\n");
            Properties timeParameters = inTime.outProperties;
            Set<Entry<Object, Object>> entrySet = timeParameters.entrySet();
            for( Entry<Object, Object> entry : entrySet ) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (value.toString().length() > 0) {
                    write(key + "\t" + value + "\n");
                }
            }

            /*
             * the options section
             */
            pm.worked(1);
            write("\n\n" + OmsEpanetParametersOptions.OPTIONSSECTION + "\n");
            Properties optionsParameters = inOptions.outProperties;
            Set<Entry<Object, Object>> optionsEntrySet = optionsParameters.entrySet();
            for( Entry<Object, Object> entry : optionsEntrySet ) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (value.toString().length() > 0) {
                    write(key + "\t" + value + "\n");
                }
            }

            /*
             * coordinates and vertices
             */
            pm.worked(1);
            String coordsText = handleCoordinates(junctionsList, reservoirsList, tanksList);
            write(coordsText);
            pm.worked(1);
            String vertexText = handleVertices(pipesList);
            write(vertexText);

        } finally {
            pm.done();
            if (bwInp != null)
                bwInp.close();
            if (bwEpanetInp != null)
                bwEpanetInp.close();
        }
    }

    private void write( String string, boolean... onlyCustom ) throws IOException {
        bwInp.write(string);
        if (onlyCustom.length == 0 || !onlyCustom[0])
            bwEpanetInp.write(string);
    }

    private void handleCurves() {
        final String prefix = EpanetConstants.CURVES_FILE_PREFIX.toString();
        File folder = new File(inExtras);
        File[] curvefiles = folder.listFiles(new FilenameFilter(){
            public boolean accept( File dir, String name ) {
                if (name.toLowerCase().startsWith(prefix)) {
                    return true;
                }
                return false;
            }
        });

        for( File curveFile : curvefiles ) {
            String name = curveFile.getName();
            String id = name.replaceFirst(prefix + "_", "");
            curveId2Path.put(id, curveFile.getAbsolutePath());
        }
    }

    private void handlePatterns() {
        final String prefix = EpanetConstants.PATTERNS_FILE_PREFIX.toString();
        File folder = new File(inExtras);
        File[] patternFiles = folder.listFiles(new FilenameFilter(){
            public boolean accept( File dir, String name ) {
                if (name.toLowerCase().startsWith(prefix)) {
                    return true;
                }
                return false;
            }
        });

        for( File patternFile : patternFiles ) {
            String name = patternFile.getName();
            String id = name.replaceFirst(prefix + "_", "");
            patternId2Path.put(id, patternFile.getAbsolutePath());
        }
    }

    private void handleDemands() {
        final String prefix = EpanetConstants.DEMANDS_FILE_PREFIX.toString();
        File folder = new File(inExtras);
        File[] demandsFiles = folder.listFiles(new FilenameFilter(){
            public boolean accept( File dir, String name ) {
                if (name.toLowerCase().startsWith(prefix)) {
                    return true;
                }
                return false;
            }
        });

        for( File demandsFile : demandsFiles ) {
            String name = demandsFile.getName();
            String id = name.replaceFirst(prefix + "_", "");
            demandId2Path.put(id, demandsFile.getAbsolutePath());
        }
    }

    private String handleJunctions( List<SimpleFeature> junctionsList ) throws IOException {
        StringBuilder sbJunctions = new StringBuilder();
        sbJunctions.append("\n\n[JUNCTIONS]\n");
        sbJunctions.append(";ID").append(SPACER);
        sbJunctions.append("ELEV").append(SPACER);
        sbJunctions.append("DEMAND").append(SPACER);
        sbJunctions.append("PATTERN").append(NL);

        StringBuilder sbEmitters = new StringBuilder();
        sbEmitters.append("\n\n[EMITTERS]\n");
        sbEmitters.append(";JUNCTION").append(SPACER);
        sbEmitters.append("COEFFICIENT").append(NL);

        for( SimpleFeature junction : junctionsList ) {
            // [JUNCTIONS]
            Object dc_id = getAttribute(junction, Junctions.ID.getAttributeName());
            if (dc_id == null)
                throw new IOException("Found a junction without ID. Please check your data!");
            sbJunctions.append(dc_id.toString());
            sbJunctions.append(SPACER);

            Object elevation = getAttribute(junction, Junctions.ELEVATION.getAttributeName());
            if (elevation == null) {
                elevation = new Double(0);
            }
            Object depth = getAttribute(junction, Junctions.DEPTH.getAttributeName());
            if (depth == null) {
                depth = new Double(0);
            }
            double elev = ((Double) elevation) - ((Double) depth);
            sbJunctions.append(elev);
            sbJunctions.append(SPACER);
            Object demand = getAttribute(junction, Junctions.DEMAND.getAttributeName());
            if (demand == null) {
                demand = new Double(0);
            }
            sbJunctions.append(demand.toString());
            sbJunctions.append(SPACER);
            Object pattern = getAttribute(junction, Junctions.PATTERN.getAttributeName());
            if (pattern == null) {
                pattern = new Double(0);
            }
            String patternId = pattern.toString();
            sbJunctions.append(patternId);
            sbJunctions.append(NL);

            String path = patternId2Path.get(patternId);
            if (path != null) {
                if (!patternsFilesList.contains(path))
                    patternsFilesList.add(path);
            }

            // emitters
            Object emitterCoeff = getAttribute(junction, Junctions.EMITTER_COEFFICIENT.getAttributeName());
            if (emitterCoeff instanceof Double) {
                double coeff = (Double) emitterCoeff;
                sbEmitters.append(dc_id.toString());
                sbEmitters.append(SPACER);
                sbEmitters.append(coeff);
                sbEmitters.append(NL);
            }

        }

        sbJunctions.append("\n\n");
        sbJunctions.append(sbEmitters.toString());
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
            Object id = getAttribute(pipe, Pipes.ID.getAttributeName());
            if (id == null)
                throw new IOException("Found a pipe without ID. Please check your data!");
            String idString = id.toString();
            if (idString.toUpperCase().startsWith(EpanetConstants.DUMMYPIPE.toString())) {
                continue;
            }
            sbPipes.append(idString);
            sbPipes.append(SPACER);

            Object node1 = getAttribute(pipe, Pipes.START_NODE.getAttributeName());
            if (node1 == null) {
                throwError(idString, "pipe", "startnode");
            }
            sbPipes.append(node1.toString());
            sbPipes.append(SPACER);
            Object node2 = getAttribute(pipe, Pipes.END_NODE.getAttributeName());
            if (node2 == null)
                throwError(idString, "pipe", "endnode");
            sbPipes.append(node2.toString());
            sbPipes.append(SPACER);
            Object length = getAttribute(pipe, Pipes.LENGTH.getAttributeName());
            if (length == null)
                throwError(idString, "pipe", "length");
            sbPipes.append(length.toString());
            sbPipes.append(SPACER);
            Object diameter = getAttribute(pipe, Pipes.DIAMETER.getAttributeName());
            if (diameter == null)
                throwError(idString, "pipe", "diameter");
            sbPipes.append(diameter.toString());
            sbPipes.append(SPACER);
            Object roughness = getAttribute(pipe, Pipes.ROUGHNESS.getAttributeName());
            if (roughness == null)
                throwError(idString, "pipe", "roughness");
            sbPipes.append(roughness.toString());
            sbPipes.append(SPACER);
            Object minorloss = getAttribute(pipe, Pipes.MINORLOSS.getAttributeName());
            if (minorloss == null) {
                sbPipes.append("0");
            } else {
                sbPipes.append(minorloss.toString());
            }
            sbPipes.append(SPACER);
            Object status = getAttribute(pipe, Pipes.STATUS.getAttributeName());
            if (status == null) {
                sbPipes.append("open");
            } else {
                sbPipes.append(status.toString());
            }
            sbPipes.append(NL);
        }

        sbPipes.append("\n\n");
        return sbPipes.toString();
    }

    private String handlePipedemands( List<SimpleFeature> pipesList, List<SimpleFeature> valvesList, List<SimpleFeature> pumpsList )
            throws IOException {
        StringBuilder sbPipesdemand = new StringBuilder();
        sbPipesdemand.append("\n\n[PDEMAND]\n");
        sbPipesdemand.append(";ID").append(SPACER);
        sbPipesdemand.append("PDEMAND").append(SPACER);
        sbPipesdemand.append("LEAKCOEFF").append(SPACER);
        sbPipesdemand.append("PATTERN").append(NL);

        // valves and pumps (virtual pipes for epanet) need a placeholder
        int dummyNum = valvesList.size() + pumpsList.size();
        for( int i = 0; i < dummyNum; i++ ) {
            sbPipesdemand.append("DUMMY").append(i).append(SPACER);
            sbPipesdemand.append("0");
            sbPipesdemand.append(SPACER);
            sbPipesdemand.append("0");
            sbPipesdemand.append(SPACER);
            sbPipesdemand.append("\t");
            sbPipesdemand.append(NL);
        }

        // normal pipes now
        for( SimpleFeature pipe : pipesList ) {
            // [PIPES]
            Object id = getAttribute(pipe, Pipes.ID.getAttributeName());
            if (id == null)
                throw new IOException("Found a pipe without ID. Please check your data!");
            String idString = id.toString();
            if (idString.toUpperCase().startsWith(EpanetConstants.DUMMYPIPE.toString())) {
                continue;
            }
            sbPipesdemand.append(idString);
            sbPipesdemand.append(SPACER);

            Object demand = getAttribute(pipe, Pipes.DEMAND.getAttributeName());
            if (demand == null) {
                sbPipesdemand.append("0");
            } else {
                sbPipesdemand.append(demand.toString());
            }
            sbPipesdemand.append(SPACER);

            Object leakCoeff = getAttribute(pipe, Pipes.LEAKCOEFF.getAttributeName());
            if (leakCoeff == null) {
                sbPipesdemand.append("0");
            } else {
                sbPipesdemand.append(leakCoeff.toString());
            }
            sbPipesdemand.append(SPACER);

            Object pattern = getAttribute(pipe, Pipes.PATTERN.getAttributeName());
            if (pattern == null) {
                sbPipesdemand.append("\t");
            } else {
                String patternId = pattern.toString();
                sbPipesdemand.append(patternId);
                String path = patternId2Path.get(patternId);
                if (path != null) {
                    if (!patternsFilesList.contains(path))
                        patternsFilesList.add(path);
                }
            }
            sbPipesdemand.append(NL);
        }
        sbPipesdemand.append("\n\n");
        return sbPipesdemand.toString();
    }

    private void throwError( String idString, String who, String what ) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("The ");
        sb.append(who);
        sb.append(" ");
        sb.append(idString);
        sb.append(" has no ");
        sb.append(what);
        sb.append(" defined. Please check your data.");
        throw new IOException(sb.toString());
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
            if (dc_id == null)
                throw new IOException("Found a reservoir without ID. Please check your data!");
            String idString = dc_id.toString();
            sbReservoirs.append(idString);
            sbReservoirs.append(SPACER);
            Object head = getAttribute(reservoir, Reservoirs.HEAD.getAttributeName());
            if (head == null)
                throwError(idString, "reservoir", "head");
            sbReservoirs.append(head.toString());
            sbReservoirs.append(SPACER);
            Object pattern = getAttribute(reservoir, Reservoirs.HEAD_PATTERN.getAttributeName());
            String patternId = "0";
            if (pattern != null) {
                patternId = pattern.toString();
                sbReservoirs.append(patternId);
            }
            sbReservoirs.append(NL);

            String path = patternId2Path.get(patternId);
            if (path != null) {
                if (!patternsFilesList.contains(path))
                    patternsFilesList.add(path);
            }
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
            if (dc_id == null)
                throw new IOException("Found a tank without ID. Please check your data!");
            sbTanks.append(dc_id.toString());
            sbTanks.append(SPACER);
            Object elevation = getAttribute(tank, Tanks.BOTTOM_ELEVATION.getAttributeName());
            if (elevation != null) {
                sbTanks.append(elevation.toString());
            } else {
                sbTanks.append("0");
            }
            sbTanks.append(SPACER);
            Object initLevel = getAttribute(tank, Tanks.INITIAL_WATER_LEVEL.getAttributeName());
            if (initLevel != null) {
                sbTanks.append(initLevel.toString());
            } else {
                sbTanks.append("0");
            }
            sbTanks.append(SPACER);
            Object minLevel = getAttribute(tank, Tanks.MINIMUM_WATER_LEVEL.getAttributeName());
            if (minLevel != null) {
                sbTanks.append(minLevel.toString());
            } else {
                sbTanks.append("0");
            }
            sbTanks.append(SPACER);
            Object maxLevel = getAttribute(tank, Tanks.MAXIMUM_WATER_LEVEL.getAttributeName());
            if (maxLevel != null) {
                sbTanks.append(maxLevel.toString());
            } else {
                sbTanks.append("0");
            }
            sbTanks.append(SPACER);
            Object diameter = getAttribute(tank, Tanks.DIAMETER.getAttributeName());
            if (diameter != null) {
                sbTanks.append(diameter.toString());
            } else {
                sbTanks.append("0");
            }
            sbTanks.append(SPACER);
            Object minVol = getAttribute(tank, Tanks.MINIMUM_VOLUME.getAttributeName());
            if (minVol != null) {
                sbTanks.append(minVol.toString());
            } else {
                sbTanks.append("0");
            }
            sbTanks.append(SPACER);
            Object volCurve = getAttribute(tank, Tanks.VOLUME_CURVE_ID.getAttributeName());
            String volCurveId;
            if (volCurve != null) {
                volCurveId = volCurve.toString();
            } else {
                volCurveId = "0";
            }
            sbTanks.append(volCurveId);
            sbTanks.append(NL);

            String path = curveId2Path.get(volCurveId);
            if (path != null) {
                if (!curvesFilesList.contains(path))
                    curvesFilesList.add(path);
            }
        }

        return sbTanks.toString();
    }

    private String handlePumps( List<SimpleFeature> pumpsList ) throws IOException {
        StringBuilder sbPumps = new StringBuilder();
        StringBuilder sbEnergy = new StringBuilder();
        sbPumps.append("\n\n[PUMPS]\n");
        sbPumps.append(";ID").append(SPACER);
        sbPumps.append("NODE1").append(SPACER);
        sbPumps.append("NODE2").append(SPACER);
        sbPumps.append("PARAMETERS").append(NL);

        sbEnergy.append("\n\n[ENERGY]\n");

        for( SimpleFeature pump : pumpsList ) {
            // [PUMPS]
            // ;ID Node1 Node2 Parameters(key1 value1 key2 value2...)
            Object dc_id = getAttribute(pump, Pumps.ID.getAttributeName());
            if (dc_id == null)
                throw new IOException("Found a pump without ID. Please check your data!");
            String pumpId = dc_id.toString();
            sbPumps.append(pumpId);
            sbPumps.append(SPACER);
            Object node1 = getAttribute(pump, Pumps.START_NODE.getAttributeName());
            if (node1 == null) {
                throwError(pumpId, "pump", "startnode");
            }
            sbPumps.append(node1.toString());
            sbPumps.append(SPACER);
            Object node2 = getAttribute(pump, Pumps.END_NODE.getAttributeName());
            if (node2 == null) {
                throwError(pumpId, "pump", "endnode");
            }
            sbPumps.append(node2.toString());
            sbPumps.append(SPACER);

            Object power = getAttribute(pump, Pumps.POWER.getAttributeName());
            if (power != null && !power.toString().equals("")) {
                sbPumps.append("POWER " + power.toString());
                sbPumps.append(SPACER);
            }
            Object head = getAttribute(pump, Pumps.HEAD_ID.getAttributeName());
            if (head != null && !head.toString().equals("")) {
                String headId = head.toString();
                sbPumps.append("HEAD " + headId);
                sbPumps.append(SPACER);

                String path = curveId2Path.get(headId);
                if (path != null) {
                    if (!curvesFilesList.contains(path))
                        curvesFilesList.add(path);
                }
            }
            Object speed = getAttribute(pump, Pumps.SPEED.getAttributeName());
            if (speed != null && !speed.toString().equals("")) {
                sbPumps.append("SPEED " + speed.toString());
                sbPumps.append(SPACER);
            }
            Object speedPattern = getAttribute(pump, Pumps.SPEED_PATTERN.getAttributeName());
            if (speedPattern != null && !speedPattern.toString().equals("")) {
                String patternId = speedPattern.toString();
                sbPumps.append("PATTERN " + patternId);
                sbPumps.append(SPACER);

                String path = patternId2Path.get(patternId);
                if (path != null) {
                    if (!patternsFilesList.contains(path))
                        patternsFilesList.add(path);
                }
            }
            sbPumps.append(NL);

            /*
             * energy part
             */
            Object price = getAttribute(pump, Pumps.PRICE.getAttributeName());
            if (price != null && !price.toString().equals("")) {
                String priceStr = price.toString();
                sbEnergy.append("PUMP " + pumpId);
                sbEnergy.append(SPACER);
                sbEnergy.append("PRICE " + priceStr);
                sbEnergy.append(NL);
            }
            Object pricePattern = getAttribute(pump, Pumps.PRICE_PATTERN.getAttributeName());
            if (pricePattern != null && !pricePattern.toString().equals("")) {
                String pricePatternStr = pricePattern.toString();
                sbEnergy.append("PUMP " + pumpId);
                sbEnergy.append(SPACER);
                sbEnergy.append("PATTERN " + pricePatternStr);
                sbEnergy.append(NL);
            }
            Object effic = getAttribute(pump, Pumps.EFFICIENCY.getAttributeName());
            if (effic != null && !effic.toString().equals("")) {
                String effStr = effic.toString();
                sbEnergy.append("PUMP " + pumpId);
                sbEnergy.append(SPACER);
                sbEnergy.append("EFFIC " + effStr);
                sbEnergy.append(NL);

                String path = curveId2Path.get(effStr);
                if (path != null) {
                    if (!curvesFilesList.contains(path))
                        curvesFilesList.add(path);
                }
            }
        }

        sbPumps.append(NL).append(NL);
        sbPumps.append(sbEnergy);
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

        StringBuilder sbValvesStatus = new StringBuilder();
        sbValvesStatus.append("\n\n[STATUS]\n");
        sbValvesStatus.append(";ID").append(SPACER);
        sbValvesStatus.append("Status/Setting").append(NL);
        boolean hasStatus = false;

        for( SimpleFeature valve : valvesList ) {
            // [VALVES]
            // ;ID Node1 Node2 Diameter Type Setting MinorLoss
            Object dc_id = getAttribute(valve, Valves.ID.getAttributeName());
            if (dc_id == null)
                throw new IOException("Found a valve without ID. Please check your data!");
            String idString = dc_id.toString();
            sbValves.append(idString);
            sbValves.append(SPACER);

            Object node1 = getAttribute(valve, Valves.START_NODE.getAttributeName());
            if (node1 == null) {
                throwError(idString, "valve", "startnode");
            }
            sbValves.append(node1.toString());
            sbValves.append(SPACER);
            Object node2 = getAttribute(valve, Valves.END_NODE.getAttributeName());
            if (node2 == null) {
                throwError(idString, "valve", "endnode");
            }
            sbValves.append(node2.toString());
            sbValves.append(SPACER);
            Object diameter = getAttribute(valve, Valves.DIAMETER.getAttributeName());
            if (diameter == null) {
                throwError(idString, "valve", "diameter");
            }
            sbValves.append(diameter.toString());
            sbValves.append(SPACER);
            Object type = getAttribute(valve, Valves.TYPE.getAttributeName());
            if (type == null) {
                throwError(idString, "valve", "type");
            }
            sbValves.append(type.toString());
            sbValves.append(SPACER);
            Object setting = getAttribute(valve, Valves.SETTING.getAttributeName());
            if (setting != null) {
                sbValves.append(setting.toString());
            } else {
                sbValves.append("0");
            }
            sbValves.append(SPACER);
            Object minorLoss = getAttribute(valve, Valves.MINORLOSS.getAttributeName());
            if (setting != null) {
                sbValves.append(minorLoss.toString());
            } else {
                sbValves.append("0");
            }
            sbValves.append(NL);

            // STATUS PART
            Object status = getAttribute(valve, Valves.STATUS.getAttributeName());
            if (status != null && status.toString().trim().length() != 0) {
                sbValvesStatus.append(idString);
                sbValvesStatus.append(SPACER);
                sbValvesStatus.append(status.toString());
                sbValvesStatus.append(NL);
                hasStatus = true;
            }
        }

        if (hasStatus)
            sbValves.append(sbValvesStatus.toString());
        sbValves.append("\n\n");
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
            Object id = getAttribute(pipe, Pipes.ID.getAttributeName());
            String idString = id.toString();
            if (idString.toUpperCase().startsWith(EpanetConstants.DUMMYPIPE.toString())) {
                continue;
            }

            // [VERTICES]
            for( Coordinate coordinate : coordinates ) {
                sbPipesVertices.append(idString);
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
