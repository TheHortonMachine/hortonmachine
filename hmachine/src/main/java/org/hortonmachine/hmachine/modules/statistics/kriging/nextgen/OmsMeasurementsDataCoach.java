/* This file is part of JGrasstools (http://www.jgrasstools.org)
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
package org.hortonmachine.hmachine.modules.statistics.kriging.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description("Measurements data coach.")
//@Documentation("Kriging.html")
@Author(name = "Silvia Franceschi, Andrea Antonello")
@Keywords("Kriging, Variogram, Interpolation")
@Label("")
@Name("stationdatacoach")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class OmsMeasurementsDataCoach extends HMModel {

    @Description(inStations_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inStations = null;

    @Description(inData_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inMeasurements = null;

    @Description(inInterpolate_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inInterpolate = null;

    @Description(fStationsid_DESCRIPTION)
    @In
    public String fStationsid = null;

    @Description(fStationsZ_DESCRIPTION)
    @In
    public String fStationsZ = null;

    @Description(fInterpolateid_DESCRIPTION)
    @In
    public String fInterpolateid = null;

    @Description(fPointZ_DESCRIPTION)
    @In
    public String fInterpolatedZ = null;

    @Description(maxdist_Description)
    @In
    public Double pMaxDist = null;

    @Description(maxCount_description)
    @In
    public Integer pMaxClosestStationsNum = null;

    @Description("Static target points ids to coordinates map for current timestep.")
    @Out
    public HashMap<Integer, Coordinate> outTargetPointsIds2CoordinateMap;

    @Description("Valid station ids to coordinates map for current timestep.")
    @Out
    public HashMap<Integer, Coordinate> outStationIds2CoordinateMap;

    @Description("Measurement data for current timestep and station ids.")
    @Out
    public HashMap<Integer, double[]> outStationIds2ValueMap;

    @Description("Association of target points with their valid stations and distance.")
    @Out
    public HashMap<Integer, TargetPointAssociation> outTargetPointId2AssociationMap;

    public static final String inStations_DESCRIPTION = "The vector of the measurement point, containing the position of the stations.";
    public static final String fStationsid_DESCRIPTION = "The field of the vector of stations, defining the id.";
    public static final String fStationsZ_DESCRIPTION = "The field of the vector of stations, defining the elevation.";
    public static final String inData_DESCRIPTION = "The file with the measured data, to be interpolated.";
    public static final String inInterpolate_DESCRIPTION = "The vector of the points in which the data have to be interpolated.";
    public static final String fInterpolateid_DESCRIPTION = "The field of the interpolated vector points, defining the id.";
    public static final String fPointZ_DESCRIPTION = "The field of the interpolated vector points, defining the elevation.";
    public static final String maxCount_description = "In the case of kriging with neighbor, inNumCloserStations is the number of stations the algorithm has to consider";
    public static final String maxdist_Description = "In the case of kriging with neighbor, maxdist is the maximum distance within the algorithm has to consider the stations";

    private OmsTimeSeriesIteratorReader inputReader;

    private HashMap<Integer, Coordinate> allStationsMap;

    private HashMap<Integer, Coordinate> targetPointsMap;

    private double nv = -9999.0;

    @Initialize
    public void initProcess() {
        doProcess = true;
    }
    
    private void ensureOpen() throws Exception {
        if (allStationsMap != null) {
            return;
        }
        checkNull(inStations, inMeasurements, inInterpolate, fStationsid, fStationsZ, fInterpolateid, fInterpolatedZ);

        SimpleFeatureCollection stationsFC = getVector(inStations);
        SimpleFeatureCollection interpolatedFC = getVector(inInterpolate);

        List<SimpleFeature> stationsList = FeatureUtilities.featureCollectionToList(stationsFC);
        allStationsMap = new HashMap<>();
        for( SimpleFeature stationFeature : stationsList ) {
            Geometry geom = (Geometry) stationFeature.getDefaultGeometry();
            Coordinate coordinate = geom.getCoordinate();
            Object idObj = stationFeature.getAttribute(fStationsid);
            int id = 0;
            if (idObj instanceof Number) {
                Number idNum = (Number) idObj;
                id = idNum.intValue();
            }
            Object zObj = stationFeature.getAttribute(fStationsZ);
            double z = 0;
            if (zObj instanceof Number) {
                Number zNum = (Number) zObj;
                z = zNum.doubleValue();
            }
            coordinate.z = z;
            allStationsMap.put(id, coordinate);
        }

        List<SimpleFeature> interpolatedList = FeatureUtilities.featureCollectionToList(interpolatedFC);
        targetPointsMap = new HashMap<>();
        for( SimpleFeature interpFeature : interpolatedList ) {
            Geometry geom = (Geometry) interpFeature.getDefaultGeometry();
            Coordinate coordinate = geom.getCoordinate();
            Object idObj = interpFeature.getAttribute(fInterpolateid);
            int id = 0;
            if (idObj instanceof Number) {
                Number idNum = (Number) idObj;
                id = idNum.intValue();
            }
            Object zObj = interpFeature.getAttribute(fInterpolatedZ);
            double z = 0;
            if (zObj instanceof Number) {
                Number zNum = (Number) zObj;
                z = zNum.doubleValue();
            }
            coordinate.z = z;
            targetPointsMap.put(id, coordinate);
        }

        inputReader = new OmsTimeSeriesIteratorReader();
        inputReader.file = inMeasurements;
        inputReader.idfield = "ID";
        // READ ALL DATA
        // inputReader.tStart = startDate;
        // inputReader.tEnd = endDate;
        // inputReader.tTimestep = timestep;
        inputReader.fileNovalue = "" + nv;
        inputReader.initProcess();
    }

    @Execute
    public void process() throws Exception {
        ensureOpen();

        inputReader.nextRecord();
        outStationIds2ValueMap = inputReader.outData;

        // TODO do checks
        List<Integer> stationsToKeep = new ArrayList<>();
        for( Entry<Integer, double[]> id2Value : outStationIds2ValueMap.entrySet() ) {
            Integer id = id2Value.getKey();
            double value = id2Value.getValue()[0];
            if (!HMConstants.isNovalue(value, -9999.0)) {
                stationsToKeep.add(id);
            }
        }

        HashMap<Integer, Coordinate> tmpMap = new HashMap<>();
        for( Integer id : stationsToKeep ) {
            tmpMap.put(id, allStationsMap.get(id));
        }
        outStationIds2CoordinateMap = tmpMap;

        outTargetPointId2AssociationMap = new HashMap<>();
        if (pMaxDist != null && pMaxClosestStationsNum != null) {
            for( Entry<Integer, Coordinate> interpEntry : targetPointsMap.entrySet() ) {
                Integer interpId = interpEntry.getKey();
                Coordinate xyz = interpEntry.getValue();

                TreeMap<Double, Integer> distance2StationIdMap = new TreeMap<>();
                // check for stations in the max distance and sort them
                for( Entry<Integer, Coordinate> statEntry : outStationIds2CoordinateMap.entrySet() ) {
                    Integer stationId = statEntry.getKey();
                    Coordinate statXyz = statEntry.getValue();

                    double distance = statXyz.distance(xyz);
                    if (distance < pMaxDist) {
                        distance2StationIdMap.put(distance, stationId);
                    }
                }

                TargetPointAssociation tpa = new TargetPointAssociation();
                int count = 0;
                double previous = 0;
                boolean allSame = true;
                int nonZeroCount = 0;
                for( Entry<Double, Integer> entry : distance2StationIdMap.entrySet() ) {
                    if (count < pMaxClosestStationsNum) {

                        tpa.stationDistances.add(entry.getKey());
                        tpa.stationIds.add(entry.getValue());

                        double value = outStationIds2ValueMap.get(entry.getValue())[0];
                        if (value != 0.0) {
                            nonZeroCount++;
                        }
                        if (count != 0 && allSame) {
                            if (previous != value) {
                                allSame = false;
                            }
                        }
                        previous = value;
                    }
                    count++;
                }
                if (tpa.stationIds.isEmpty()) {
                    tpa.interpolationType = InterpolationType.NODATA;
                } else if (allSame) {
                    tpa.interpolationType = InterpolationType.NOINTERPOLATION_USE_RAW_DATA;
                } else {
                    if (tpa.stationIds.size() == 1) {
                        tpa.interpolationType = InterpolationType.NOINTERPOLATION_USE_RAW_DATA;
                    } else if (tpa.stationIds.size() == 2 || nonZeroCount < 3) {
                        tpa.interpolationType = InterpolationType.INTERPOLATION_IDW;
                    } else {
                        tpa.interpolationType = InterpolationType.INTERPOLATION_KRIGING;
                    }
                }
                outTargetPointId2AssociationMap.put(interpId, tpa);
            }
        } else {
            for( Entry<Integer, Coordinate> interpEntry : targetPointsMap.entrySet() ) {
                Integer interpId = interpEntry.getKey();
                Coordinate xyz = interpEntry.getValue();

                TargetPointAssociation tpa = new TargetPointAssociation();
                int count = 0;
                double previous = 0;
                boolean allSame = true;
                int nonZeroCount = 0;

                for( Entry<Integer, Coordinate> statEntry : outStationIds2CoordinateMap.entrySet() ) {
                    Integer stationId = statEntry.getKey();
                    Coordinate statXyz = statEntry.getValue();

                    double distance = statXyz.distance(xyz);

                    tpa.stationDistances.add(distance);
                    tpa.stationIds.add(stationId);

                    double value = outStationIds2ValueMap.get(stationId)[0];
                    if (value != 0.0) {
                        nonZeroCount++;
                    }
                    if (count != 0 && allSame) {
                        if (previous != value) {
                            allSame = false;
                        }
                    }
                    previous = value;
                }
                if (tpa.stationIds.isEmpty()) {
                    tpa.interpolationType = InterpolationType.NODATA;
                } else if (allSame) {
                    tpa.interpolationType = InterpolationType.NOINTERPOLATION_USE_RAW_DATA;
                } else {
                    if (tpa.stationIds.size() == 1) {
                        tpa.interpolationType = InterpolationType.NOINTERPOLATION_USE_RAW_DATA;
                    } else if (tpa.stationIds.size() == 2 || nonZeroCount < 3) {
                        tpa.interpolationType = InterpolationType.INTERPOLATION_IDW;
                    } else {
                        tpa.interpolationType = InterpolationType.INTERPOLATION_KRIGING;
                    }
                }
                outTargetPointId2AssociationMap.put(interpId, tpa);
            }
        }
        
        doProcess = inputReader.doProcess;
    }
}
