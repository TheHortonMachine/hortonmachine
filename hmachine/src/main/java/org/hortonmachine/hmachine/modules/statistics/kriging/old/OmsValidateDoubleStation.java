package org.hortonmachine.hmachine.modules.statistics.kriging.old;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVALIDATEDOUBLESTATION_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVALIDATEDOUBLESTATION_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVALIDATEDOUBLESTATION_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVALIDATEDOUBLESTATION_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVALIDATEDOUBLESTATION_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVALIDATEDOUBLESTATION_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVALIDATEDOUBLESTATION_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVALIDATEDOUBLESTATION_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVALIDATEDOUBLESTATION_doMean_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVALIDATEDOUBLESTATION_fStationsid_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVALIDATEDOUBLESTATION_inData_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVALIDATEDOUBLESTATION_inStations_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVALIDATEDOUBLESTATION_outData_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVALIDATEDOUBLESTATION_outStations_DESCRIPTION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import org.geotools.feature.FeatureIterator;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

@Description(OMSVALIDATEDOUBLESTATION_DESCRIPTION)
@Author(name = OMSVALIDATEDOUBLESTATION_AUTHORNAMES, contact = OMSVALIDATEDOUBLESTATION_AUTHORCONTACTS)
@Keywords(OMSVALIDATEDOUBLESTATION_KEYWORDS)
@Label(OMSVALIDATEDOUBLESTATION_LABEL)
@Name(OMSVALIDATEDOUBLESTATION_NAME)
@Status(OMSVALIDATEDOUBLESTATION_STATUS)
@License(OMSVALIDATEDOUBLESTATION_LICENSE)
public class OmsValidateDoubleStation extends HMModel {

    @Description(OMSVALIDATEDOUBLESTATION_inStations_DESCRIPTION)
    @In
    public SimpleFeatureCollection inStations = null;

    @Description(OMSVALIDATEDOUBLESTATION_fStationsid_DESCRIPTION)
    @In
    public String fStationsid = null;

    @Description(OMSVALIDATEDOUBLESTATION_inData_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inData = null;

    @Description(OMSVALIDATEDOUBLESTATION_outData_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> outData = null;

    @Description(OMSVALIDATEDOUBLESTATION_outStations_DESCRIPTION)
    @In
    public SimpleFeatureCollection outStations = null;

    @Description(OMSVALIDATEDOUBLESTATION_doMean_DESCRIPTION)
    @In
    public boolean doMean = false;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void verifyDoubleStation() throws Exception {
        if (!concatOr((outStations == null || outData == null), doReset)) {
            return;
        }

        if (inData == null || inStations == null) {
            throw new NullPointerException(msg.message("kriging.stationproblem"));
        }

        List<Double> xStationList = new ArrayList<Double>();
        List<Double> yStationList = new ArrayList<Double>();
        List<Double> zStationList = new ArrayList<Double>();
        List<Double> hStationList = new ArrayList<Double>();
        List<Integer> idStationList = new ArrayList<Integer>();

        /*
         * Store the station coordinates and measured data in the array.
         */
        FeatureIterator<SimpleFeature> stationsIter = inStations.features();
        try {
            while( stationsIter.hasNext() ) {
                SimpleFeature feature = stationsIter.next();
                int id = ((Number) feature.getAttribute(fStationsid)).intValue();
                Coordinate coordinate = ((Geometry) feature.getDefaultGeometry()).getCentroid().getCoordinate();
                double[] h = inData.get(id);
                if (h == null) {
                    /*
                     * skip data for non existing stations, they are allowed.
                     * Also skip novalues.
                     */
                    // throw new NullPointerException("thereisn't data");
                    continue;
                }
                idStationList.add(id);
                xStationList.add(coordinate.x);
                yStationList.add(coordinate.y);
                zStationList.add(coordinate.z);
                hStationList.add(h[0]);

            }
        } finally {
            stationsIter.close();
        }

        int nStaz = xStationList.size();
        /*
         * The coordinates of the station points plus in last position a place
         * for the coordinate of the point to interpolate.
         */
        int[] idStation = new int[nStaz];
        double[] xStation = new double[nStaz];
        double[] yStation = new double[nStaz];
        double[] zStation = new double[nStaz];
        double[] hStation = new double[nStaz];
        idStation[0] = idStationList.get(0);
        xStation[0] = xStationList.get(0);
        yStation[0] = yStationList.get(0);
        zStation[0] = zStationList.get(0);
        hStation[0] = hStationList.get(0);
        int k = 0;
        int j = 0;
        hStation[k] = hStationList.get(0);
        idStation[k] = idStationList.get(0);

        List<Integer> idStationtoDelete = new ArrayList<Integer>();
        outData = new HashMap<Integer, double[]>();
        for( int i = 1; i < xStation.length; i++ ) {
            int id = idStationList.get(i);
            double xTmp = xStationList.get(i);
            double yTmp = yStationList.get(i);
            double zTmp = zStationList.get(i);
            double hTmp = hStationList.get(i);
            boolean doubleStation = ModelsEngine.verifyDoubleStation(xStation, yStation, zStation, hStation, xTmp, yTmp, zTmp,
                    hTmp, i, doMean);

            if (!doubleStation) {
                xStation[k] = xStationList.get(i);
                yStation[k] = yStationList.get(i);
                zStation[k] = zStationList.get(i);
                hStation[k] = hStationList.get(i);
                idStation[k] = id;
                k++;

            } else {
                idStationtoDelete.add(id);
                j++;
            }
        }

        for( int i = 0; i < k; i++ ) {
            outData.put(idStation[i], new double[]{hStation[i]});
        }

        stationsIter = inStations.features();
        outStations = new DefaultFeatureCollection();

        try {
            while( stationsIter.hasNext() ) {
                SimpleFeature feature = stationsIter.next();
                int id = ((Number) feature.getAttribute(fStationsid)).intValue();

                for( int q = 0; q < j; q++ ) {
                    if (idStationtoDelete.get(q) == id) {

                        continue;
                    }
                }

                ((DefaultFeatureCollection) outStations).add(feature);

            }

        } finally {
            stationsIter.close();
        }

    }

}
