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
package org.hortonmachine.gears.libs.modules;

/**
 * Variable names that also need translations and used in the modules.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface Variables {
    public String DEFAULT = "default";

    public String PROGRESS_MONITOR_EN = "The progress monitor.";

    public String TYPE_INT = "INTEGER";
    public String TYPE_FLOAT = "FLOAT";
    public String TYPE_DOUBLE = "DOUBLE";

    public String TPS = "TPS";
    public String IDW = "IDW";

    public String NEAREST_NEIGHTBOUR = "nearest neightbour";
    public String BILINEAR = "bilinear";
    public String BICUBIC = "bicubic";

    public String INTERSECTION = "intersection";
    public String UNION = "union";
    public String DIFFERENCE = "difference";
    public String SYMDIFFERENCE = "symdifference";

    public String TCA = "only tca";
    public String TCA_SLOPE = "tca and slope";
    public String TCA_CONVERGENT = "tca in convergent sites";

    public String FINITE_DIFFERENCES = "Finite Differences";
    public String HORN = "Horn";
    public String EVANS = "Evans";

    public String FIXED_NETWORK = "with fixed network";

    public String CAP_ROUND = "round";
    public String CAP_FLAT = "flat";
    public String CAP_SQUARE = "square";
    public String JOIN_ROUND = "round";
    public String JOIN_MITRE = "mitre";
    public String JOIN_BEVEL = "bevel";
    
    public String PROJECT = "Project";
    public String CALIBRATION = "Calibration";

    public String DILATE = "dilate";
    public String ERODE = "erode";
    public String SKELETONIZE1 = "skeletonize1";
    public String SKELETONIZE2 = "skeletonize2";
    public String SKELETONIZE2VAR = "skeletonize2var";
    public String SKELETONIZE3 = "skeletonize3";
    public String LINEENDINGS = "lineendings";
    public String LINEJUNCTIONS = "linejunctions";
    public String PRUNE = "prune";
    public String OPEN = "open";
    public String CLOSE = "close";

    public String OGC_TMS = "OGC TMS";
    public String GOOGLE = "Google";
    public String TMS = "TMS";
    public String WMS = "WMS";

    /*
     * zonalstats 
     */
    public String MIN = "min";
    public String MAX = "max";
    public String AVG = "avg";
    public String VAR = "var";
    public String SDEV = "sdev";
    public String SUM = "sum";
    public String AVGABSDEV = "avgabsdev";
    public String ACTCELLS = "actcells";
    public String INVCELLS = "invcells";

    /**
     * Custom keyword. 
     */
    public String CUSTOM = "custom";
    /**
     * Windows mode for deciduous. 
     */
    public String DECIDUOUS = "deciduous";
    /**
     * Windows mode for conifer. 
     */
    public String CONIFER = "conifer";
    /**
     * Windows mode for mixed pines and deciduous. 
     */
    public String MIXED_PINES_AND_DECIDUOUS = "mixed_pines_and_deciduous_trees";

    /**
     * Kernel type binary.
     */
    public String BINARY = "binary";
    /**
     * Kernel type cosine.
     */
    public String COSINE = "cosine";
    /**
     * Kernel type distance.
     */
    public String DISTANCE = "distance";
    /**
     * Kernel type epanechnikov.
     */
    public String EPANECHNIKOV = "epanechnikov";
    /**
     * Kernel type gaussian.
     */
    public String GAUSSIAN = "gaussian";
    /**
     * Kernel type inverse_distance.
     */
    public String INVERSE_DISTANCE = "inverse_distance";
    /**
     * Kernel type quartic.
     */
    public String QUARTIC = "quartic";
    /**
     * Kernel type triangular.
     */
    public String TRIANGULAR = "triangular";
    /**
     * Kernel type triweight.
     */
    public String TRIWEIGHT = "triweight";

    
    public String TOPOLOGYPRESERVINGSIMPLIFIER = "TopologyPreservingSimplifier";
    public String DOUGLAS_PEUCKER = "Douglas Peucker";
    public String PRECISION_REDUCER = "Reduce coordinate precision";
    
    public String KRIGING_EXPERIMENTAL_VARIOGRAM = "With Experimental Variogram";
    public String KRIGING_DEFAULT_VARIOGRAM = "With Default Variogram";
}
