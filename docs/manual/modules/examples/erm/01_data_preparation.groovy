import org.hortonmachine.hmachine.geoframe.ermworkflow.ErmDataPreparator;

// the folder containing the input data: change it to yours
var workspace = "/data/COURSE_WATER/"

var prep = new ErmDataPreparator()
prep.inDtm = workspace + "dem.tif"
prep.outGeopackageName = "geoframe_data.gpkg"

// this defines the network roughness
prep.pDrainThreshold = 2000

// this the size of the hydrological response units
prep.pDesiredAreaDelta = 20.0
prep.pDesiredArea = 1_000_000.0

// the basin's outlet
//176033.934,3926035.360
prep.pOutletEasting = 176033.934
prep.pOutletNorthing = 3926035.36

// observation points
prep.pStreamGaugeIDField = "stationid"
prep.inStreamGauge = workspace + "discharge/dams.shp"

prep.process();
