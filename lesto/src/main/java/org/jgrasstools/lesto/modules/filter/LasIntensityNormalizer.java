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

import org.jgrasstools.gears.io.las.core.ALasWriter;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.core.v_1_0.LasReader;
import org.jgrasstools.gears.io.las.core.v_1_0.LasWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("A module that normalizes a value inside the las")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, normalize")
@Label(JGTConstants.VECTORPROCESSING)
@Name("lasnorm")
@Status(Status.EXPERIMENTAL)
@License(JGTConstants.GPL3_LICENSE)
public class LasIntensityNormalizer extends JGTModel {
    @Description("A las file to normalize.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inLas;

    @Description("Normalization factor.")
    @In
    public double pFactor = 1;

    @Description("Output file.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outLas;

    public void process() throws Exception {
        checkNull(inLas, outLas);

        CoordinateReferenceSystem crs = null;
        File lasFile = new File(inLas);
        ALasReader reader = ALasReader.getReader(lasFile, crs);
        reader.open();
        ILasHeader header = reader.getHeader();
        long recordsNum = header.getRecordsCount();

        File outFile = new File(outLas);
        ALasWriter writer = new LasWriter(outFile, crs);
        pm.beginTask("Normalizing...", (int) recordsNum);
        reader = ALasReader.getReader(lasFile, crs);
        reader.open();
        writer.open();
        while( reader.hasNextPoint() ) {
            LasRecord readNextLasDot = reader.getNextPoint();
            double newValue = readNextLasDot.intensity * pFactor;
            readNextLasDot.intensity = (short) newValue;
            writer.addPoint(readNextLasDot);
            pm.worked(1);
        }
        reader.close();
        writer.close();
        pm.done();
    }

    public static void main( String[] args ) throws Exception {
        LasIntensityNormalizer norm = new LasIntensityNormalizer();
        norm.inLas = "/home/moovida/geologico_2013/flightlines/twostripes/001059_3_4_thres112.las";
        norm.pFactor = 81.0 / 112.0;
        norm.outLas = "/home/moovida/geologico_2013/flightlines/twostripes/001059_3_4_thres112_norm.las";
        norm.process();
    }
}
