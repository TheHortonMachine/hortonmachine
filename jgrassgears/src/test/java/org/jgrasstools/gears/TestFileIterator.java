package org.jgrasstools.gears;

import java.io.File;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.modules.utils.fileiterator.FileIterator;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
/**
 * Test FileIterator.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestFileIterator extends HMTestCase {

    @SuppressWarnings("nls")
    public void testFileIterator() throws Exception {
        
//        FileIterator iter = new FileIterator();
//        iter.inFolder = "folder";
//        iter.process();
//        List<File> filesList = iter.filesList;
        
    }

}
