package org.jgrasstools.hortonmachine.models.hm;

import static org.jgrasstools.gears.libs.modules.JGTConstants.utcDateFormatterYYYYMMDDHHMM;

import java.util.HashMap;

import org.geotools.feature.FeatureCollection;
import org.jgrasstools.gears.io.adige.AdigeBoundaryCondition;
import org.jgrasstools.gears.io.adige.AdigeBoundaryConditionReader;
import org.jgrasstools.gears.io.adige.AdigeBoundaryConditionWriter;
import org.jgrasstools.gears.io.adige.VegetationLibraryReader;
import org.jgrasstools.gears.io.adige.VegetationLibraryRecord;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
import org.jgrasstools.gears.io.timedependent.TimeseriesByStepReaderId2Value;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.Adige;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.joda.time.format.DateTimeFormatter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Test Adige.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestAdige extends HMTestCase {

    @SuppressWarnings("nls")
    public void testAdige() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        String startDate = "2005-05-01 00:00";
        String endDate = "2005-05-01 03:00";
        int timeStepMinutes = 30;

        String shpFolder = "/home/moovida/data/newage/sampledata/shapefiles/";
        String dataFolder = "/home/moovida/data/newage/sampledata/";

        String hillslopePath = shpFolder + "basins_passirio_width0.shp";

        String rainDataPath = dataFolder + "kriging_interpolated.csv";
        // String netradiationDataPath = "";
        // String shortradiationDataPath = "";
        // String temperatureDataPath = "";
        // String humidityDataPath = "";
        // String windspeedDataPath = "";
        // String pressureDataPath = "";
        // String sweDataPath = "";

        String hydrometersPath = shpFolder + "hydrometers.shp";
        String hydrometersDataPath = dataFolder + "hydrometers_new.csv";
        String damsPath = shpFolder + "dams.shp";
        String damsDataPath = dataFolder + "dams_new.csv";
        String tributaryPath = shpFolder + "tributaries.shp";
        String tributaryDataPath = dataFolder + "tributaries_new.csv";
        // String offtakesPath = shpFolder + "offtakes.shp";
        // String offtakesDataPath = dataFolder + "offtakes_new.csv";

        String vegetationPath = dataFolder + "vegetation.csv";

        String networkPath = shpFolder + "net_passirio.shp";

        String inBoundaryConditionsPath = dataFolder + "boundary_conditions/in.txt";
        String outBoundaryConditionsPath = dataFolder + "boundary_conditions/out.txt";

        String fId = "ID";

        FeatureCollection<SimpleFeatureType, SimpleFeature> hillslopeFC = ShapefileFeatureReader
                .readShapefile(hillslopePath);

        // meteo
        TimeseriesByStepReaderId2Value rainReader = getTimeseriesReader(rainDataPath, fId,
                startDate, endDate, timeStepMinutes);
        // TimeseriesByStepReaderId2Value netradiationReader = getTimeseriesReader(
        // netradiationDataPath, fId, startDate, endDate, timeStepMinutes);
        // TimeseriesByStepReaderId2Value shortradiationReader = getTimeseriesReader(
        // shortradiationDataPath, fId, startDate, endDate, timeStepMinutes);
        // TimeseriesByStepReaderId2Value temperatureReader =
        // getTimeseriesReader(temperatureDataPath,
        // fId, startDate, endDate, timeStepMinutes);
        // TimeseriesByStepReaderId2Value humidityReader = getTimeseriesReader(humidityDataPath,
        // fId,
        // startDate, endDate, timeStepMinutes);
        // TimeseriesByStepReaderId2Value windspeedReader = getTimeseriesReader(windspeedDataPath,
        // fId, startDate, endDate, timeStepMinutes);
        // TimeseriesByStepReaderId2Value pressureReader = getTimeseriesReader(pressureDataPath,
        // fId,
        // startDate, endDate, timeStepMinutes);
        // TimeseriesByStepReaderId2Value sweReader = getTimeseriesReader(sweDataPath, fId,
        // startDate,
        // endDate, timeStepMinutes);

        TimeseriesByStepReaderId2Value hydrometersReader = getTimeseriesReader(hydrometersDataPath,
                fId, startDate, endDate, timeStepMinutes);
        TimeseriesByStepReaderId2Value damsReader = getTimeseriesReader(damsDataPath, fId,
                startDate, endDate, timeStepMinutes);
        TimeseriesByStepReaderId2Value tributaryReader = getTimeseriesReader(tributaryDataPath,
                fId, startDate, endDate, timeStepMinutes);
        // TimeseriesByStepReaderId2Value offtakesReader = getTimeseriesReader(offtakesDataPath,
        // fId,
        // startDate, endDate, timeStepMinutes);

        FeatureCollection<SimpleFeatureType, SimpleFeature> hydrometersFC = ShapefileFeatureReader
                .readShapefile(hydrometersPath);
        FeatureCollection<SimpleFeatureType, SimpleFeature> damsFC = ShapefileFeatureReader
                .readShapefile(damsPath);
        FeatureCollection<SimpleFeatureType, SimpleFeature> tributaryFC = ShapefileFeatureReader
                .readShapefile(tributaryPath);
        // FeatureCollection<SimpleFeatureType, SimpleFeature> offtakesFC = ShapefileFeatureReader
        // .readShapefile(offtakesPath);

        VegetationLibraryReader vegetationReader = new VegetationLibraryReader();
        vegetationReader.file = vegetationPath;
        vegetationReader.read();
        HashMap<Integer, VegetationLibraryRecord> vegetationData = vegetationReader.data;
        vegetationReader.close();

        FeatureCollection<SimpleFeatureType, SimpleFeature> networkFC = ShapefileFeatureReader
                .readShapefile(networkPath);

        Adige adige = new Adige();
        adige.pm = pm;
        adige.inHillslope = hillslopeFC;
        adige.fNetnum = "netnum";
        adige.fBaricenter = "avgz";
        adige.fVegetation = "uso_reclas";
        adige.fAvg_sub = "mean_sub";
        adige.fVar_sub = "sd_sub";
        adige.fAvg_sup_10 = "mean_10";
        adige.fVar_sup_10 = "sd_10";
        adige.fAvg_sup_30 = "mean_30";
        adige.fVar_sup_30 = "sd_30";
        adige.fAvg_sup_60 = "mean_60";
        adige.fVar_sup_60 = "sd_60";
        adige.pV_sup = 0.5;
        adige.pV_sub = 0.5;
        adige.inHydrometers = hydrometersFC;
        adige.inDams = damsFC;
        adige.inTributary = tributaryFC;
        // adige.inOfftakes = offtakesFC;
        adige.inVegetation = vegetationData;
        adige.pPfafids = "514.11,514.9";
        adige.fMonpointid = "id_punti_m";
        adige.inNetwork = networkFC;
        adige.fPfaff = "pfafstette";
        adige.fNetelevstart = "elevfirstp";
        adige.fNetelevend = "elevlastpo";

        adige.pKs = 3.0;
        adige.pMstexp = 11.0;
        adige.pDepthmnsat = 2.0;
        adige.pSpecyield = 0.01;
        adige.pPorosity = 0.41;
        adige.pEtrate = 0.001;
        adige.pSatconst = 0.3;

        adige.pRouting = 3;
        adige.pRainintensity = -1;
        adige.pRainduration = -1;
        adige.doLog = false;

        adige.tTimestep = timeStepMinutes;
        adige.tStart = startDate;
        adige.tEnd = endDate;

        adige.doBoundary = true;
        AdigeBoundaryConditionReader boundaryConditionReader = new AdigeBoundaryConditionReader();
        boundaryConditionReader.file = inBoundaryConditionsPath;
        boundaryConditionReader.read();
        HashMap<Integer, AdigeBoundaryCondition> inBoundaryConditions = boundaryConditionReader.data;
        boundaryConditionReader.close();
        adige.inInitialconditions = inBoundaryConditions;
        
        adige.pDischargePerUnitArea = 0.01;
        adige.pStartSuperficialDischargeFraction = 0.3;
        adige.pMaxSatVolumeS1 = 0.2;
        adige.pMaxSatVolumeS2 = 0.25;
        // adige.inInitialconditions = inBoundaryConditions;

        rainReader.initProcess();
        while( rainReader.doProcess ) {
            rainReader.nextRecord();
            adige.inRain = rainReader.data;

            // netradiationReader.nextRecord();
            // adige.inNetradiation = netradiationReader.data;
            //
            // shortradiationReader.nextRecord();
            // adige.inShortradiation = shortradiationReader.data;
            //
            // temperatureReader.nextRecord();
            // adige.inTemperature = temperatureReader.data;
            //
            // humidityReader.nextRecord();
            // adige.inHumidity = humidityReader.data;
            //
            // windspeedReader.nextRecord();
            // adige.inWindspeed = windspeedReader.data;
            //
            // pressureReader.nextRecord();
            // adige.inPressure = pressureReader.data;
            //
            // sweReader.nextRecord();
            // adige.inSwe = sweReader.data;

            hydrometersReader.nextRecord();
            adige.inHydrometerdata = hydrometersReader.data;

            damsReader.nextRecord();
            adige.inDamsdata = damsReader.data;

            tributaryReader.nextRecord();
            adige.inTributarydata = tributaryReader.data;

            // offtakesReader.nextRecord();
            // adige.inOfftakesdata = offtakesReader.data;

            adige.process();

            HashMap<Integer, double[]> outDischarge = adige.outDischarge;
            HashMap<Integer, double[]> outSubDischarge = adige.outSubdischarge;
            HashMap<Integer, double[]> outS1 = adige.outS1;
            HashMap<Integer, double[]> outS2 = adige.outS2;
        }

        rainReader.close();
        // netradiationReader.close();
        // shortradiationReader.close();
        // temperatureReader.close();
        // humidityReader.close();
        // windspeedReader.close();
        // pressureReader.close();
        // sweReader.close();

        hydrometersReader.close();
        damsReader.close();
        tributaryReader.close();
        // offtakesReader.close();

        AdigeBoundaryConditionWriter boundaryConditionWriter = new AdigeBoundaryConditionWriter();
        boundaryConditionWriter.file = outBoundaryConditionsPath;
        boundaryConditionWriter.data = adige.outFinalconditions;
        boundaryConditionWriter.write();
        boundaryConditionWriter.close();

    }

    private TimeseriesByStepReaderId2Value getTimeseriesReader( String path, String id,
            String startDate, String endDate, int timeStepMinutes ) {
        TimeseriesByStepReaderId2Value dataReader = new TimeseriesByStepReaderId2Value();
        dataReader.file = path;
        dataReader.fileNovalue = "-9999";
        dataReader.idfield = id;
        dataReader.tStart = startDate;
        dataReader.tTimestep = timeStepMinutes;
        dataReader.tEnd = endDate;
        return dataReader;
    }
}
