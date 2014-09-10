package org.jgrasstools.lesto.modules.filter;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

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

import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.jgrasstools.gears.io.las.core.ALasWriter;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.core.v_1_0.LasReader;
import org.jgrasstools.gears.io.las.core.v_1_0.LasWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("A module that merges las files to a single one.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, merge")
@Label(JGTConstants.VECTORPROCESSING)
@Name("lasmerge")
@Status(Status.EXPERIMENTAL)
@License(JGTConstants.GPL3_LICENSE)
public class LasMerger extends JGTModel {
    @Description("A folder of las files to merge.")
    @UI(JGTConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder;

    @Description("The merged las output file.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outLas;

    public void process() throws Exception {
        checkNull(inFolder, outLas);
        CoordinateReferenceSystem crs = null;

        File inFolderFile = new File(inFolder);
        File[] lasList = inFolderFile.listFiles(new FilenameFilter(){
            public boolean accept( File arg0, String arg1 ) {
                return arg1.toLowerCase().endsWith(".las");
            }
        });

        StringBuilder sb = new StringBuilder("Merging files:");
        for( File file : lasList ) {
            sb.append("\n").append(file.getAbsolutePath());
        }
        pm.message(sb.toString());

        // create readers and calculate bounds
        List<ALasReader> readers = new ArrayList<ALasReader>();
        double xMin = Double.POSITIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY;
        double zMin = Double.POSITIVE_INFINITY;
        double xMax = Double.NEGATIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;
        double zMax = Double.NEGATIVE_INFINITY;
        int count = 0;
        for( File lasFile : lasList ) {
            ALasReader reader = ALasReader.getReader(lasFile, crs);
            reader.open();
            ILasHeader header = reader.getHeader();
            long recordsNum = header.getRecordsCount();
            count = (int) (count + recordsNum);
            ReferencedEnvelope3D envelope = header.getDataEnvelope();
            xMin = min(xMin, envelope.getMinX());
            yMin = min(yMin, envelope.getMinY());
            zMin = min(zMin, envelope.getMinZ());
            xMax = max(xMax, envelope.getMaxX());
            yMax = max(yMax, envelope.getMaxY());
            zMax = max(zMax, envelope.getMaxZ());
            readers.add(reader);
        }

        File outFile = new File(outLas);
        ALasWriter writer = new LasWriter(outFile, crs);
        writer.setBounds(xMin, xMax, yMin, yMax, zMin, zMax);
        writer.open();

        pm.beginTask("Merging...", count);
        for( ALasReader reader : readers ) {
            while( reader.hasNextPoint() ) {
                LasRecord readNextLasDot = reader.getNextPoint();
                writer.addPoint(readNextLasDot);
                pm.worked(1);
            }
            reader.close();
        }
        writer.close();
        pm.done();
    }

    public static void main( String[] args ) throws Exception {
        LasMerger norm = new LasMerger();
        norm.inFolder = "/home/moovida/geologico_2013/flightlines/norm/";
        norm.outLas = "/home/moovida/geologico_2013/flightlines/norm/merged.las";
        norm.process();
    }
}
