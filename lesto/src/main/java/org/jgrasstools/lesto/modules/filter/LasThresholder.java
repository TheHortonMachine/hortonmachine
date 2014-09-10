package org.jgrasstools.lesto.modules.filter;
import java.io.File;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.ALasWriter;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.core.v_1_0.LasWriter;
import org.jgrasstools.gears.io.las.utils.LasUtils;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("A module that applies threshold and filters on a value inside the las")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, threshold, filter")
@Label(JGTConstants.VECTORPROCESSING)
@Name("lasthreshold")
@Status(Status.EXPERIMENTAL)
@License(JGTConstants.GPL3_LICENSE)
public class LasThresholder extends JGTModel {
    @Description("A las file to filter.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inLas;

    @Description("Lower threshold.")
    @In
    public Double pLower;

    @Description("Upper threshold.")
    @In
    public Double pUpper;

    @Description("The value to analyze.")
    @UI("combo:" + LasUtils.INTENSITY + "," + LasUtils.ELEVATION)
    @In
    public String pType = LasUtils.INTENSITY;

    @Description("Output file.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outLas;

    public void process() throws Exception {
        checkNull(inLas, outLas);

        boolean doIntensity = false;
        if (pType.equals(LasUtils.INTENSITY)) {
            doIntensity = true;
        }

        CoordinateReferenceSystem crs = null;
        File lasFile = new File(inLas);
        ALasReader reader = ALasReader.getReader(lasFile, crs);
        reader.open();
        ILasHeader header = reader.getHeader();
        long recordsNum = header.getRecordsCount();

        File outFile = new File(outLas);
        ALasWriter writer = new LasWriter(outFile, crs);
        writer.setBounds(header);
        writer.open();

        pm.beginTask("Filtering", (int) recordsNum);
        double min = Double.NEGATIVE_INFINITY;
        if (pLower != null) {
            min = pLower;
        }
        double max = Double.POSITIVE_INFINITY;
        if (pUpper != null) {
            max = pUpper;
        }
        while( reader.hasNextPoint() ) {
            LasRecord readNextLasDot = reader.getNextPoint();
            double value = readNextLasDot.z;
            if (doIntensity) {
                value = readNextLasDot.intensity;
            }
            if (value < min) {
                pm.worked(1);
                continue;
            }
            if (value > max) {
                pm.worked(1);
                continue;
            }
            writer.addPoint(readNextLasDot);
            pm.worked(1);
        }
        reader.close();
        writer.close();
        pm.done();
    }

    public static void main( String[] args ) throws Exception {

         LasThresholder norm = new LasThresholder();
         norm.pLower = 70.0;
         norm.pType = LasUtils.INTENSITY;
         norm.inLas =
         "/home/moovida/geologico_2013/flightlines/001059_3_normfl_thres350_elev4.las";
         norm.outLas = "/home/moovida/geologico_2013/flightlines/001059_3_normfl_thres350_elev4_range70_350.las";
         norm.process();

//        int delta = 100;
//        for( int i = 80; i < 1000; i = i + delta ) {
//            int from = i;
//            int to = i + delta;
//
//            LasThresholder norm = new LasThresholder();
//            norm.pLower = (double) from;
//            norm.pUpper = (double) to;
//            norm.pType = LasUtils.INTENSITY;
//            norm.inLas = "/home/moovida/geologico_2013/flightlines/001059_3_normfl_thres350_elev4.las";
//            norm.outLas = "/home/moovida/geologico_2013/flightlines/001059_3_normfl_thres350_elev4_range" + from + "_" + to
//                    + ".las";
//            norm.process();
//        }

    }
}
