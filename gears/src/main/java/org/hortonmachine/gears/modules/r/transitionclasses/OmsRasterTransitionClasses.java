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
package org.hortonmachine.gears.modules.r.transitionclasses;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_KEYWORDS;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_LABEL;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_LICENSE;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_NAME;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_STATUS;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.HMRasterWritableBuilder;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description(OMSRASTERTRANSITIONCLASSES_DESCRIPTION)
@Documentation(OMSRASTERTRANSITIONCLASSES_DOCUMENTATION)
@Author(name = OMSRASTERTRANSITIONCLASSES_AUTHORNAMES, contact = OMSRASTERTRANSITIONCLASSES_AUTHORCONTACTS)
@Keywords(OMSRASTERTRANSITIONCLASSES_KEYWORDS)
@Label(OMSRASTERTRANSITIONCLASSES_LABEL)
@Name(OMSRASTERTRANSITIONCLASSES_NAME)
@Status(OMSRASTERTRANSITIONCLASSES_STATUS)
@License(OMSRASTERTRANSITIONCLASSES_LICENSE)
public class OmsRasterTransitionClasses extends HMModel {

    @Description(OMSRASTERTRANSITIONCLASSES_IN_RASTER1_DESCRIPTION)
    @In
    public GridCoverage2D inPreviousRaster;

    @Description(OMSRASTERTRANSITIONCLASSES_IN_RASTER2_DESCRIPTION)
    @In
    public GridCoverage2D inNextRaster;

    @Description(OMSRASTERTRANSITIONCLASSES_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OMSRASTERTRANSITIONCLASSES_DESCRIPTION = "Compares two rasters of classes of different times, as two land covers, and gives each change from a class to another its own code.";
    public static final String OMSRASTERTRANSITIONCLASSES_DOCUMENTATION = """
            Each pair of a previous and a next class found in the same cell is a transition, and \
            gets a code in the order in which it is first met, reading the rasters from the \
            north-west corner by rows: the transitions between different classes get the codes \
            from 1, the cells whose class didn't change the codes from 10001, and the cells \
            that are novalue only in one of the rasters, the classes that appear or disappear, \
            the negative codes from -1. The cells novalue in both rasters stay novalue.

            The codes of a run depend on the data, so the table of the transitions, written by \
            the toolbox module, tells for each code its previous and next class and its count of \
            cells: the counts times the cell area give the area of each change, as for a \
            transition matrix of the land cover. The two rasters need the same grid.
            """;
    public static final String OMSRASTERTRANSITIONCLASSES_KEYWORDS = "Raster, Classes, Transition, Change, Land cover";
    public static final String OMSRASTERTRANSITIONCLASSES_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERTRANSITIONCLASSES_NAME = "rtransclasses";
    public static final int OMSRASTERTRANSITIONCLASSES_STATUS = 40;
    public static final String OMSRASTERTRANSITIONCLASSES_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERTRANSITIONCLASSES_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERTRANSITIONCLASSES_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERTRANSITIONCLASSES_IN_RASTER1_DESCRIPTION = "The raster of the classes of the previous time.";
    public static final String OMSRASTERTRANSITIONCLASSES_IN_RASTER2_DESCRIPTION = "The raster of the classes of the next time, on the same grid.";
    public static final String OMSRASTERTRANSITIONCLASSES_OUT_RASTER_DESCRIPTION = "The raster of the codes of the transitions.";

    public static final String UNACCOUNTED = "unaccounted";

    /**
     * The code after which the codes of the unchanged classes start.
     */
    public static final int EQUAL_CLASSES_START = 10000;

    /**
     * The transitions, as "previous next", with their code.
     */
    public TreeMap<String, Integer> key2NewClassesMap = new TreeMap<>();
    /**
     * The count of cells of each code.
     */
    public TreeMap<Integer, Integer> newClasses2CountMap = new TreeMap<>();

    @Execute
    public void process() throws Exception {
        checkNull(inPreviousRaster, inNextRaster);

        try (HMRaster r1 = HMRaster.fromGridCoverage(inPreviousRaster);
                HMRaster r2 = HMRaster.fromGridCoverage(inNextRaster);
                HMRaster transition = new HMRasterWritableBuilder().setName("transition").setDoInteger(true)
                        .setTemplate(inPreviousRaster).setNoValue(HMConstants.intNovalue).build()) {

            int rows = r1.getRows();
            int cols = r1.getCols();
            if (r2.getRows() != rows || r2.getCols() != cols) {
                throw new ModelsIllegalargumentException("The two rasters need to have the same grid.", this, pm);
            }
            pm.beginTask("Calculating transition...", rows);

            int runningClasses = 0;
            int runningEqualClasses = EQUAL_CLASSES_START;
            int runningNovalueClasses = 0;
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double v1 = r1.getValue(c, r);
                    double v2 = r2.getValue(c, r);
                    boolean v1IsNovalue = r1.isNovalue(v1);
                    boolean v2IsNovalue = r2.isNovalue(v2);
                    if (v1IsNovalue && v2IsNovalue) {
                        continue;
                    }
                    String key = (v1IsNovalue ? UNACCOUNTED : formatClass(v1)) + " "
                            + (v2IsNovalue ? UNACCOUNTED : formatClass(v2));
                    Integer newClass = key2NewClassesMap.get(key);
                    if (newClass == null) {
                        if (v1IsNovalue || v2IsNovalue) {
                            newClass = --runningNovalueClasses;
                        } else if (v1 == v2) {
                            newClass = ++runningEqualClasses;
                        } else {
                            newClass = ++runningClasses;
                        }
                        key2NewClassesMap.put(key, newClass);
                    }
                    newClasses2CountMap.merge(newClass, 1, Integer::sum);
                    transition.setValue(c, r, newClass.intValue());
                }
                pm.worked(1);
            }
            pm.done();

            outRaster = transition.buildCoverage();
        }
    }

    /**
     * @return the table of the transitions, as csv with header: code;previous;next;count.
     */
    public String getTransitionsTable() {
        TreeMap<Integer, String> newClasses2KeyMap = new TreeMap<>();
        for( Entry<String, Integer> entry : key2NewClassesMap.entrySet() ) {
            newClasses2KeyMap.put(entry.getValue(), entry.getKey());
        }
        StringBuilder sb = new StringBuilder("code;previous;next;count\n");
        for( Entry<Integer, Integer> entry : newClasses2CountMap.entrySet() ) {
            String[] split = newClasses2KeyMap.get(entry.getKey()).split(" ");
            sb.append(entry.getKey()).append(";").append(split[0]).append(";").append(split[1]).append(";")
                    .append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    /**
     * The classes are usually integers: they are written without the decimals.
     */
    private static String formatClass( double value ) {
        if (value == Math.rint(value) && Math.abs(value) < 1E15) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

}
