/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrasstools.gears.modules.v.vectorize;

import jaitools.media.jai.vectorize.VectorizeDescriptor;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.modules.r.rangelookup.RangeLookup;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.AffineTransformation;

@Description("Module for raster to vector conversion.")
@Documentation("Vectorizer.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Raster, Vector, ScanLineRasterizer")
@Status(Status.CERTIFIED)
@Label(JGTConstants.VECTORPROCESSING)
@Name("vectorizer")
@License("General Public License Version 3 (GPLv3)")
@SuppressWarnings("nls")
public class Vectorizer extends JGTModel {

    @Description("The raster that has to be converted.")
    @In
    public GridCoverage2D inGeodata;

    @Description("The value to use to trace the polygons. If it is null then all the value of the raster are used.")
    @In
    public Double pValue = null;

    @Description("The field name to use as a name for the raster value in the vector.")
    @In
    public String fDefault = "value";

    @Description("A threshold on cell number to filter away polygons with cells less than that.")
    @In
    public double pThres = 0;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The extracted vector.")
    @Out
    public SimpleFeatureCollection outGeodata = null;

    private CoordinateReferenceSystem crs;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outGeodata == null, doReset)) {
            return;
        }
        checkNull(inGeodata);
        crs = inGeodata.getCoordinateReferenceSystem();

        String classes = null;
        StringBuilder sb = new StringBuilder();
        if (pValue == null) {
            sb.append("(null null)");
            classes = "1";
        } else {
            sb.append("(null ");
            sb.append(pValue);
            sb.append("),[");
            sb.append(pValue);
            sb.append(" ");
            sb.append(pValue);
            sb.append("],(");
            sb.append(pValue);
            sb.append(" null)");
            classes = "NaN,1,NaN";
        }
        String ranges = sb.toString();

        // values are first classified, since the vectorializer works on same values
        RangeLookup cont = new RangeLookup();
        cont.inGeodata = inGeodata;
        cont.pRanges = ranges;
        cont.pClasses = classes;
        cont.pm = pm;
        cont.process();
        GridCoverage2D outCov = cont.outGeodata;

        Map<String, Object> args = new HashMap<String, Object>();
        // args.put("outsideValues", Collections.singleton(0));
        Collection<Polygon> polygonsList = doVectorize(outCov.getRenderedImage(), args);

        HashMap<String, Double> regionParams = CoverageUtilities.getRegionParamsFromGridCoverage(inGeodata);
        double xRes = regionParams.get(CoverageUtilities.XRES);
        double yRes = regionParams.get(CoverageUtilities.YRES);

        final AffineTransform mt2D = (AffineTransform) inGeodata.getGridGeometry().getGridToCRS2D(PixelOrientation.CENTER);
        final AffineTransformation jtsTransformation = new AffineTransformation(mt2D.getScaleX(), mt2D.getShearX(),
                mt2D.getTranslateX() - xRes / 2.0, mt2D.getShearY(), mt2D.getScaleY(), mt2D.getTranslateY() + yRes / 2.0);

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("raster2vector");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("cat", Integer.class);
        b.add(fDefault, Double.class);
        SimpleFeatureType type = b.buildFeatureType();

        outGeodata = FeatureCollections.newCollection();
        int index = 0;
        for( Polygon polygon : polygonsList ) {
            double area = polygon.getArea();
            if (area <= pThres) {
                continue;
            }

            Double tmpValue = -1.0;
            Object userData = polygon.getUserData();
            if (userData instanceof Double) {
                tmpValue = (Double) userData;
            }
            polygon.apply(jtsTransformation);
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            Object[] values = new Object[]{polygon, index, tmpValue};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(type.getTypeName() + "." + index);
            index++;
            outGeodata.add(feature);
        }
    }

    /**
     * Helper function to run the Vectorize operation with given parameters and
     * retrieve the vectors.
     * 
     * @param src the source image
     * @param args a {@code Map} of parameter names and values
     * 
     * @return the generated vectors as JTS Polygons
     */
    @SuppressWarnings("unchecked")
    private Collection<Polygon> doVectorize( RenderedImage src, Map<String, Object> args ) {
        ParameterBlockJAI pb = new ParameterBlockJAI("Vectorize");
        pb.setSource("source0", src);

        // Set any parameters that were passed in
        for( Entry<String, Object> e : args.entrySet() ) {
            pb.setParameter(e.getKey(), e.getValue());
        }

        // Get the desintation image: this is the unmodified source image data
        // plus a property for the generated vectors
        RenderedOp dest = JAI.create("Vectorize", pb);

        // Get the vectors
        Object property = dest.getProperty(VectorizeDescriptor.VECTOR_PROPERTY_NAME);
        return (Collection<Polygon>) property;
    }

}
