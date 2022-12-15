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

import java.util.TreeMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;

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

    public static final String OMSRASTERTRANSITIONCLASSES_DESCRIPTION = "Raster transition classes generator.";
    public static final String OMSRASTERTRANSITIONCLASSES_DOCUMENTATION = "";
    public static final String OMSRASTERTRANSITIONCLASSES_KEYWORDS = "Raster, Classes, Transition";
    public static final String OMSRASTERTRANSITIONCLASSES_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERTRANSITIONCLASSES_NAME = "rtransclasses";
    public static final int OMSRASTERTRANSITIONCLASSES_STATUS = 5;
    public static final String OMSRASTERTRANSITIONCLASSES_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERTRANSITIONCLASSES_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERTRANSITIONCLASSES_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERTRANSITIONCLASSES_IN_RASTER1_DESCRIPTION = "The previous classes raster.";
    public static final String OMSRASTERTRANSITIONCLASSES_IN_RASTER2_DESCRIPTION = "The next classes raster.";
    public static final String OMSRASTERTRANSITIONCLASSES_OUT_RASTER_DESCRIPTION = "The output transitions raster.";

    public static final String UNACCOUNTED = "unaccounted";

    public TreeMap<String, Integer> key2NewClassesMap = new TreeMap<>();
    public TreeMap<Integer, Integer> newClasses2CountMap = new TreeMap<>();

    @Execute
    public void process() throws Exception {
        checkNull(inPreviousRaster, inNextRaster);

        try (HMRaster r1 = HMRaster.fromGridCoverage(inPreviousRaster);
                HMRaster r2 = HMRaster.fromGridCoverage(inNextRaster);
                HMRaster transition = HMRaster.writableIntegerFromTemplate("transition", inPreviousRaster)) {

            double novalue = r1.getNovalue();

            int rows = r1.getRows();
            int cols = r1.getCols();
            pm.beginTask("Calculating transition...", rows);

            int runningClasses = 0;
            int runningEqualClasses = 10000;
            int runningNovalueClasses = 0;
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double v1 = r1.getValue(c, r);
                    double v2 = r2.getValue(c, r);
                    double outV;
                    boolean v1IsNovalue = r1.isNovalue(v1);
                    boolean v2IsNovalue = r2.isNovalue(v2);
                    if (v1IsNovalue && v2IsNovalue) {
                        outV = novalue;
                    } else {
                        String key;
                        Integer newClass;
                        if (v1IsNovalue) {
                            key = UNACCOUNTED + " " + v2;
                            newClass = key2NewClassesMap.get(key);
                            if (newClass == null) {
                                newClass = --runningNovalueClasses;
                                key2NewClassesMap.put(key, newClass);
                            }
                        } else if (v2IsNovalue) {
                            key = v1 + " " + UNACCOUNTED;
                            newClass = key2NewClassesMap.get(key);
                            if (newClass == null) {
                                newClass = --runningNovalueClasses;
                                key2NewClassesMap.put(key, newClass);
                            }
                        } else {
                            key = v1 + " " + v2;
                            newClass = key2NewClassesMap.get(key);
                            if (newClass == null) {
                                if (v1 == v2) {
                                    newClass = ++runningEqualClasses;
                                } else {
                                    newClass = ++runningClasses;
                                }
                                key2NewClassesMap.put(key, newClass);
                            }
                        }

                        Integer count = newClasses2CountMap.get(newClass);
                        if (count == null) {
                            count = 1;
                        } else {
                            count++;
                        }
                        newClasses2CountMap.put(newClass, count);
                        outV = newClass.doubleValue();
                    }
                    transition.setValue(c, r, (int) outV);
                }
                pm.worked(1);
            }
            pm.done();

            outRaster = transition.buildCoverage();
        }

    }

}
