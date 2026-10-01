import org.hortonmachine.hmachine.geoframe.ermworkflow.ErmSimulation;

// the folder containing the input data: change it to yours
var workspace = "/data/COURSE_WATER/"

var sim = new ErmSimulation();
sim.inGeopackagePath = workspace + "outputs/geoframe_data.gpkg"
sim.inFromTimestamp = "2020-01-01 01:00:00"
sim.inToTimestamp = "2023-12-31 01:00:00"
sim.pTimeStepMinutes = 60 * 24
sim.pSpinUpDays = 365

// from calibration
sim.inParams = new double[]{
  1.1614983901587717, 1.4969306351817795, 2.9830333245670695, 1.808081781219844, 2.3745699575399704E-4, 0.49980203055182526, 0.14335399499928275, 0.9799932714777185, 40.000572352728085, 2.9999425942946267, 0.8000772403354526, 2.9995843441109766, 99.99909451256433, 4.999999347303716, 0.9999592410245134, 100.12398303724608, 0.01159196273045821, 0.9999999727806498
}
sim.process();
