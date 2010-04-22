package org.jgrasstools.hortonmachine.oms;

import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Role;
import oms3.control.Iteration;

import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
import org.jgrasstools.gears.io.timedependent.TimeseriesByStepReaderId2Value;
import org.jgrasstools.gears.io.timedependent.TimeseriesByStepWriterId2Value;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.hortonmachine.modules.statistics.kriging.Kriging;

/**
 * @author moovida
 */
public class KrigingIteration extends Iteration {

    // Model parameter
    @Role(Role.PARAMETER)
    @In
    public String rainFilePath;

    @Role(Role.PARAMETER)
    @In
    public String stationsPath;

    @Role(Role.PARAMETER)
    @In
    public String interpolatedPointsPath;

    @Role(Role.PARAMETER)
    @In
    public String interpolatedRainPath;

    @Description("The start date.")
    @In
    public String startDate = null;

    @Description("The end date.")
    @In
    public String endDate = null;

    @Description("The timestep in minutes.")
    @In
    public int timestep = -1;

    // components
    private TimeseriesByStepReaderId2Value rainReader = new TimeseriesByStepReaderId2Value();
    private TimeseriesByStepWriterId2Value interpolatedRainWriter = new TimeseriesByStepWriterId2Value();
    private ShapefileFeatureReader stationsReader = new ShapefileFeatureReader();
    private ShapefileFeatureReader interpolatedPointsReader = new ShapefileFeatureReader();
    private Kriging kriging = new Kriging();

    @Initialize
    public void init() {
        conditional(rainReader, "isTicking");

        // timer
        in2in("startDate", rainReader, "tStart");
        in2in("endDate", rainReader, "tEnd");
        in2in("timestep", rainReader, "tTimestep");

        // pms
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
        val2in(pm, kriging, "pm");

        // rain reader
         val2in("-9999", rainReader, "fileNovalue");
         val2in(JGTConstants.doubleNovalue, rainReader, "novalue");
         val2in("ID", rainReader, "idfield");
        in2in("rainFilePath", rainReader, "file");

        // shapefile readers
        in2in("stationsPath", stationsReader, "file");
        in2in("interpolatedPointsPath", interpolatedPointsReader, "file");

        // kriging extra
        val2in("ID_PUNTI_M", kriging, "fStationsid");
        val2in("netnum", kriging, "fInterpolateid");
        val2in(0, kriging, "pMode");
        val2in(new double[]{10000.0, 10000.0, 100.0}, kriging, "pIntegralscale");
        val2in(0.5, kriging, "pVariance");
        val2in(false, kriging, "doLogarithmic");
        // val2in(0, kriging, "variogramMode");
        // val2in(0, kriging, "a");
        // val2in(0, kriging, "s");
        // val2in(0, kriging, "nug");

        // writer
        val2in("kriging_output", interpolatedRainWriter, "tablename");

        // writer
        in2in("interpolatedRainPath", interpolatedRainWriter, "file");

        // rain reader to kriging
        out2in(rainReader, "data", kriging, "inData");
        // stations shape to kriging
        out2in(stationsReader, "geodata", kriging, "inStations");
        // interpolated points shape to kriging
        out2in(interpolatedPointsReader, "geodata", kriging, "inInterpolate");
        // kriging to interpolated rain writer
        out2in(kriging, "outData", interpolatedRainWriter, "data");
        // timer to writer
        out2in(rainReader, "tStart", interpolatedRainWriter);
        out2in(rainReader, "tTimestep", interpolatedRainWriter);

        initializeComponents();
    }

}
