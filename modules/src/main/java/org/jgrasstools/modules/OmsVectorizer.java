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
package org.jgrasstools.modules;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_doRegioncheck_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_fDefault_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_outVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_pThres_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_pValue_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

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
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.Envelope2D;
import org.jaitools.media.jai.vectorize.VectorizeDescriptor;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.r.rangelookup.OmsRangeLookup;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.AffineTransformation;

@Description(OMSVECTORIZER_DESCRIPTION)
@Documentation(OMSVECTORIZER_DOCUMENTATION)
@Author(name = OMSVECTORIZER_AUTHORNAMES, contact = OMSVECTORIZER_AUTHORCONTACTS)
@Keywords(OMSVECTORIZER_KEYWORDS)
@Label(OMSVECTORIZER_LABEL)
@Name("_" + OMSVECTORIZER_NAME)
@Status(OMSVECTORIZER_STATUS)
@License(OMSVECTORIZER_LICENSE)
public class OmsVectorizer extends JGTModel {

    @Description(OMSVECTORIZER_inRaster_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSVECTORIZER_pValue_DESCRIPTION)
    @In
    public Double pValue = null;

    @Description(OMSVECTORIZER_fDefault_DESCRIPTION)
    @In
    public String fDefault = "value";

    @Description(OMSVECTORIZER_pThres_DESCRIPTION)
    @In
    public double pThres = 0;

    @Description(OMSVECTORIZER_doRegioncheck_DESCRIPTION)
    @In
    public boolean doRegioncheck = false;

    @Description(OMSVECTORIZER_outVector_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector = null;

    private CoordinateReferenceSystem crs;

    private int cols;

    private int rows;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }
        checkNull(inRaster);
        crs = inRaster.getCoordinateReferenceSystem();
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        cols = regionMap.getCols();
        rows = regionMap.getRows();

        doRegionCheck();

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
        OmsRangeLookup cont = new OmsRangeLookup();
        cont.inRaster = inRaster;
        cont.pRanges = ranges;
        cont.pClasses = classes;
        cont.pm = pm;
        cont.process();
        GridCoverage2D outCov = cont.outRaster;

        Map<String, Object> args = new HashMap<String, Object>();
        // args.put("outsideValues", Collections.singleton(0));
        Collection<Polygon> polygonsList = doVectorize(outCov.getRenderedImage(), args);

        HashMap<String, Double> regionParams = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        double xRes = regionParams.get(CoverageUtilities.XRES);
        double yRes = regionParams.get(CoverageUtilities.YRES);

        final AffineTransform mt2D = (AffineTransform) inRaster.getGridGeometry().getGridToCRS2D(PixelOrientation.CENTER);
        final AffineTransformation jtsTransformation = new AffineTransformation(mt2D.getScaleX(), mt2D.getShearX(),
                mt2D.getTranslateX() - xRes / 2.0, mt2D.getShearY(), mt2D.getScaleY(), mt2D.getTranslateY() + yRes / 2.0);

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("raster2vector");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("cat", Integer.class);
        b.add(fDefault, Double.class);
        b.add("area", Double.class);
        b.add("perimeter", Double.class);
        b.add("xcentroid", Double.class);
        b.add("ycentroid", Double.class);
        SimpleFeatureType type = b.buildFeatureType();

        outVector = FeatureCollections.newCollection();
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

            area = polygon.getArea();
            double perim = polygon.getLength();
            com.vividsolutions.jts.geom.Point centroid = polygon.getCentroid();
            Coordinate centroidCoord = centroid.getCoordinate();
            Object[] values = new Object[]{polygon, index, tmpValue, area, perim, centroidCoord.x, centroidCoord.y};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(type.getTypeName() + "." + index);
            index++;
            outVector.add(feature);
        }
    }

    private void doRegionCheck() throws TransformException {
        if (doRegioncheck) {
            int left = Integer.MAX_VALUE;
            int right = -Integer.MAX_VALUE;
            int top = -Integer.MAX_VALUE;
            int bottom = Integer.MAX_VALUE;

            RenderedImage rasterRI = inRaster.getRenderedImage();
            RandomIter rasterIter = RandomIterFactory.create(rasterRI, null);
            for( int c = 0; c < cols; c++ ) {
                for( int r = 0; r < rows; r++ ) {
                    double value = rasterIter.getSampleDouble(c, r, 0);
                    if (!isNovalue(value)) {
                        left = min(left, c);
                        right = max(right, c);
                        top = max(top, r);
                        bottom = min(bottom, r);
                    }
                }
            }

            GridGeometry2D gridGeometry = inRaster.getGridGeometry();
            GridEnvelope2D gEnv = new GridEnvelope2D();
            gEnv.setLocation(new Point(left, top));
            gEnv.add(new Point(right, bottom));
            Envelope2D envelope2d = gridGeometry.gridToWorld(gEnv);
            inRaster = (GridCoverage2D) Operations.DEFAULT.crop(inRaster, envelope2d);
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
