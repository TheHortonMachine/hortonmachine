package org.jgrasstools.gears.modules.io;

import java.io.File;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.dxfdwg.DxfFeatureReader;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureWriter;
import org.jgrasstools.gears.modules.utils.fileiterator.FileIterator;
import org.jgrasstools.gears.utils.files.FileUtilities;
/**
 * Test dxf reader.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestDxfIO {

    public void testDxfIO() throws Exception {

        FileIterator fileIterator = new FileIterator();
        fileIterator.inFolder = "/home/moovida/data/008dxf/";
        fileIterator.pCode = "EPSG:32632";
        fileIterator.pRegex = ".dxf";
        fileIterator.process();

        List<File> filesList = fileIterator.filesList;

        for( File dxfFile : filesList ) {

            String name = FileUtilities.getNameWithoutExtention(dxfFile);

            DxfFeatureReader reader = new DxfFeatureReader();
            reader.file = dxfFile.getAbsolutePath();
            reader.readFeatureCollection();

            SimpleFeatureCollection pointsFC = reader.pointsFC;
            SimpleFeatureCollection linesFC = reader.lineFC;

            String parent = dxfFile.getParent();
            if (linesFC.size() > 0)
                ShapefileFeatureWriter.writeShapefile(parent + File.separator + name + "_lines.shp", linesFC);
            if (pointsFC.size() > 0)
                ShapefileFeatureWriter.writeShapefile(parent + File.separator + name + "_points.shp", pointsFC);

            
        }

        // System.out.println(pointsFC.size() + " / " + linesFC.size());
        //
        // MapsViewer viewer = new MapsViewer();
        // viewer.featureCollections = new SimpleFeatureCollection[]{linesFC};
        // viewer.displayMaps();

    }

    public static void main( String[] args ) throws Exception {
        new TestDxfIO().testDxfIO();
    }
}
