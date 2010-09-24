package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.adige.AdigeBoundaryCondition;
import org.jgrasstools.gears.io.adige.AdigeBoundaryConditionReader;
import org.jgrasstools.gears.io.adige.AdigeBoundaryConditionWriter;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
import org.jgrasstools.gears.io.timedependent.TimeseriesByStepReaderId2Value;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.Adige;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.duffy.DuffyInputs;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.adige.hymod.HymodInputs;
import org.jgrasstools.hortonmachine.utils.HMTestCase;

/**
 * Test Adige.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestAdige extends HMTestCase {

    @SuppressWarnings("nls")
    public void testAdigeDuffy() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        String startDate = "2005-05-01 00:00";
        String endDate = "2005-05-01 03:00";
        int timeStepMinutes = 30;

        String shpFolder = "/Users/silli/lavori_tmp/newage/newage_oms/sampledata/shapefiles/";
        String dataFolder = "/Users/silli/lavori_tmp/newage/newage_oms/sampledata/";

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

        SimpleFeatureCollection hillslopeFC = ShapefileFeatureReader.readShapefile(hillslopePath);

        // meteo
        TimeseriesByStepReaderId2Value rainReader = getTimeseriesReader(rainDataPath, fId, startDate, endDate, timeStepMinutes);
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

        TimeseriesByStepReaderId2Value hydrometersReader = getTimeseriesReader(hydrometersDataPath, fId, startDate, endDate,
                timeStepMinutes);
        TimeseriesByStepReaderId2Value damsReader = getTimeseriesReader(damsDataPath, fId, startDate, endDate, timeStepMinutes);
        TimeseriesByStepReaderId2Value tributaryReader = getTimeseriesReader(tributaryDataPath, fId, startDate, endDate,
                timeStepMinutes);
        // TimeseriesByStepReaderId2Value offtakesReader = getTimeseriesReader(offtakesDataPath,
        // fId,
        // startDate, endDate, timeStepMinutes);

        SimpleFeatureCollection hydrometersFC = ShapefileFeatureReader.readShapefile(hydrometersPath);
        SimpleFeatureCollection damsFC = ShapefileFeatureReader.readShapefile(damsPath);
        SimpleFeatureCollection tributaryFC = ShapefileFeatureReader.readShapefile(tributaryPath);
        // SimpleFeatureCollection offtakesFC = ShapefileFeatureReader
        // .readShapefile(offtakesPath);

        SimpleFeatureCollection networkFC = ShapefileFeatureReader.readShapefile(networkPath);

        DuffyInputs duffyInputs = new DuffyInputs();
        duffyInputs.fAvg_sub = "mean_sub";
        duffyInputs.fVar_sub = "sd_sub";
        duffyInputs.fAvg_sup_10 = "mean_10";
        duffyInputs.fVar_sup_10 = "sd_10";
        duffyInputs.fAvg_sup_30 = "mean_30";
        duffyInputs.fVar_sup_30 = "sd_30";
        duffyInputs.fAvg_sup_60 = "mean_60";
        duffyInputs.fVar_sup_60 = "sd_60";
        duffyInputs.pV_sup = 0.5;
        duffyInputs.pV_sub = 0.5;
        duffyInputs.pKs = 3.0;
        duffyInputs.pMstexp = 11.0;
        duffyInputs.pDepthmnsat = 2.0;
        duffyInputs.pSpecyield = 0.01;
        duffyInputs.pPorosity = 0.41;
        duffyInputs.pEtrate = 0.001;
        duffyInputs.pSatconst = 0.3;
        duffyInputs.pRouting = 3;
        duffyInputs.doBoundary = true;
        AdigeBoundaryConditionReader boundaryConditionReader = new AdigeBoundaryConditionReader();
        boundaryConditionReader.file = inBoundaryConditionsPath;
        boundaryConditionReader.read();
        HashMap<Integer, AdigeBoundaryCondition> inBoundaryConditions = boundaryConditionReader.data;
        boundaryConditionReader.close();
        duffyInputs.inInitialconditions = inBoundaryConditions;
        duffyInputs.pDischargePerUnitArea = 0.01;
        duffyInputs.pStartSuperficialDischargeFraction = 0.3;
        duffyInputs.pMaxSatVolumeS1 = 0.2;
        duffyInputs.pMaxSatVolumeS2 = 0.25;
        // adige.inInitialconditions = inBoundaryConditions;

        Adige adige = new Adige();
        adige.pm = pm;
        adige.inDuffyInput = duffyInputs;
        adige.inHillslope = hillslopeFC;
        adige.fNetnum = "netnum";
        adige.fBaricenter = "avgz";
        // adige.fVegetation = "uso_reclas";
        adige.inHydrometers = hydrometersFC;
        adige.inDams = damsFC;
        adige.inTributary = tributaryFC;
        // adige.inOfftakes = offtakesFC;
        // adige.inVegetation = vegetationData;
        adige.pPfafids = "514.11,514.9";
        adige.fMonpointid = "id_punti_m";
        adige.inNetwork = networkFC;
        adige.fPfaff = "pfafstette";
        adige.fNetelevstart = "elevfirstp";
        adige.fNetelevend = "elevlastpo";

        adige.pRainintensity = -1;
        adige.pRainduration = -1;
        adige.doLog = false;

        adige.tTimestep = timeStepMinutes;
        adige.tStart = startDate;
        adige.tEnd = endDate;

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
            HashMap<Integer, double[]> outS1 = duffyInputs.outS1;
            HashMap<Integer, double[]> outS2 = duffyInputs.outS2;
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
        boundaryConditionWriter.data = duffyInputs.outFinalconditions;
        boundaryConditionWriter.write();
        boundaryConditionWriter.close();

    }

    @SuppressWarnings("nls")
    public void testAdigeHymod() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        String startDate = "2005-05-01 00:00";
        String endDate = "2005-05-01 03:00";
        int timeStepMinutes = 60;

        String shpFolder = "/Users/silli/lavori_tmp/newage/newage_oms/jami_input/";
        String dataFolder = "/Users/silli/lavori_tmp/newage/newage_oms/jami_input/";

        String hillslopePath = shpFolder + "basins_passirio_width0.shp";

        String rainDataPath = dataFolder + "energy_out_pnet_hour.csv";
        String etpDataPath = dataFolder + "etpfao_out_hour.csv";
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

        String networkPath = shpFolder + "net_passirio.shp";

        String fId = "ID";

        SimpleFeatureCollection hillslopeFC = ShapefileFeatureReader.readShapefile(hillslopePath);

        // meteo
        TimeseriesByStepReaderId2Value rainReader = getTimeseriesReader(rainDataPath, fId, startDate, endDate, timeStepMinutes);
        TimeseriesByStepReaderId2Value hydrometersReader = getTimeseriesReader(hydrometersDataPath, fId, startDate, endDate,
                timeStepMinutes);
        TimeseriesByStepReaderId2Value damsReader = getTimeseriesReader(damsDataPath, fId, startDate, endDate, timeStepMinutes);
        TimeseriesByStepReaderId2Value tributaryReader = getTimeseriesReader(tributaryDataPath, fId, startDate, endDate,
                timeStepMinutes);

        // etp
        TimeseriesByStepReaderId2Value etpReader = getTimeseriesReader(etpDataPath, fId, startDate, endDate, timeStepMinutes);

        SimpleFeatureCollection hydrometersFC = ShapefileFeatureReader.readShapefile(hydrometersPath);
        SimpleFeatureCollection damsFC = ShapefileFeatureReader.readShapefile(damsPath);
        SimpleFeatureCollection tributaryFC = ShapefileFeatureReader.readShapefile(tributaryPath);

        SimpleFeatureCollection networkFC = ShapefileFeatureReader.readShapefile(networkPath);

        HymodInputs hymodInputs = new HymodInputs();
        hymodInputs.pCmax = 72.45;
        hymodInputs.pB = 4.9;
        hymodInputs.pAlpha = 0.76;
        hymodInputs.pRs = 0.00014;
        hymodInputs.pRq = 0.12;
        hymodInputs.pQ0 = 10.0;

        Adige adige = new Adige();
        adige.pm = pm;
        adige.inHymodInput = hymodInputs;
        adige.inHillslope = hillslopeFC;
        adige.fNetnum = "netnum";
        adige.fBaricenter = "avgz";
        // adige.fVegetation = "uso_reclas";
        adige.inHydrometers = hydrometersFC;
        adige.inDams = damsFC;
        adige.inTributary = tributaryFC;
        // adige.inOfftakes = offtakesFC;
        // adige.inVegetation = vegetationData;
        adige.pPfafids = "514.11,514.9";
        adige.fMonpointid = "id_punti_m";
        adige.inNetwork = networkFC;
        adige.fPfaff = "pfafstette";
        adige.fNetelevstart = "elevfirstp";
        adige.fNetelevend = "elevlastpo";

        adige.pRainintensity = -1;
        adige.pRainduration = -1;
        adige.doLog = false;

        adige.tTimestep = timeStepMinutes;
        adige.tStart = startDate;
        adige.tEnd = endDate;

        rainReader.initProcess();
        while( rainReader.doProcess ) {
            rainReader.nextRecord();
            adige.inRain = rainReader.data;

            etpReader.nextRecord();
            adige.inEtp = etpReader.data;

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

    }

    private TimeseriesByStepReaderId2Value getTimeseriesReader( String path, String id, String startDate, String endDate,
            int timeStepMinutes ) {
        TimeseriesByStepReaderId2Value dataReader = new TimeseriesByStepReaderId2Value();
        dataReader.file = path;
        dataReader.fileNovalue = "-9999";
        dataReader.idfield = id;
        dataReader.tStart = startDate;
        dataReader.tTimestep = timeStepMinutes;
        dataReader.tEnd = endDate;
        return dataReader;
    }

    public static void main( String[] args ) throws Exception {
        new TestAdige().testAdigeHymod();
    }
}
