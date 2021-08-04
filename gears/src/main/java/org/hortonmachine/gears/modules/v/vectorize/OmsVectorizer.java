/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.gears.modules.v.vectorize;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_DO_REGION_CHECK_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_DO_REMOVE_HOLES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_F_DEFAULT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_P_THRES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_P_VALUE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORIZER_STATUS;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

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
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.Envelope2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.modules.r.rangelookup.OmsRangeLookup;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.jaitools.media.jai.vectorize.VectorizeDescriptor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.AffineTransformation;

@Description(OMSVECTORIZER_DESCRIPTION)
@Documentation(OMSVECTORIZER_DOCUMENTATION)
@Author(name = OMSVECTORIZER_AUTHORNAMES, contact = OMSVECTORIZER_AUTHORCONTACTS)
@Keywords(OMSVECTORIZER_KEYWORDS)
@Label(OMSVECTORIZER_LABEL)
@Name(OMSVECTORIZER_NAME)
@Status(OMSVECTORIZER_STATUS)
@License(OMSVECTORIZER_LICENSE)
public class OmsVectorizer extends HMModel {

    @Description(OMSVECTORIZER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSVECTORIZER_P_VALUE_DESCRIPTION)
    @In
    public Double pValue = null;

    @Description(OMSVECTORIZER_F_DEFAULT_DESCRIPTION)
    @In
    public String fDefault = "value";

    @Description(OMSVECTORIZER_DO_REMOVE_HOLES_DESCRIPTION)
    @In
    public boolean doRemoveHoles = false;

    @Description(OMSVECTORIZER_P_THRES_DESCRIPTION)
    @In
    public double pThres = 0;

    @Description(OMSVECTORIZER_DO_REGION_CHECK_DESCRIPTION)
    @In
    public boolean doRegioncheck = false;

    @Description("Don't consider values, use value-nvalue mask.")
    @In
    public boolean doMask = false;

    @Description("A threshold to set on the values before masking (values below are nulled).")
    @In
    public double pMaskThreshold = Double.NaN;

    @Description(OMSVECTORIZER_OUT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector = null;

    public int featureIndex = 0;

    private CoordinateReferenceSystem crs;

    private double novalue;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }
        checkNull(inRaster);
        crs = inRaster.getCoordinateReferenceSystem();

        novalue = HMConstants.getNovalue(inRaster);

        doRegionCheck();

        String classes = null;
        StringBuilder sb = new StringBuilder();
        if (pValue != null) {
            sb.append("(null ");
            sb.append(pValue);
            sb.append("),[");
            sb.append(pValue);
            sb.append(" ");
            sb.append(pValue);
            sb.append("],(");
            sb.append(pValue);
            sb.append(" null)");
            classes = novalue + "," + pValue + "," + novalue;

            String ranges = sb.toString();

            pm.beginTask("Extract range: " + ranges, IHMProgressMonitor.UNKNOWN);

            // values are first classified, since the vectorializer works on same values
            OmsRangeLookup cont = new OmsRangeLookup();
            cont.inRaster = inRaster;
            cont.pRanges = ranges;
            cont.pClasses = classes;
            cont.pm = pm;
            cont.process();
            inRaster = cont.outRaster;

            pm.done();
        }

        if (doMask) {
            inRaster = maskRaster();
        }

        pm.beginTask("Vectorizing map...", IHMProgressMonitor.UNKNOWN);
        Map<String, Object> args = new HashMap<String, Object>();
        // args.put("outsideValues", Collections.singleton(0));
        Collection<Polygon> polygonsList = doVectorize(inRaster.getRenderedImage(), args);
        pm.done();

        HashMap<String, Double> regionParams = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        double xRes = regionParams.get(CoverageUtilities.XRES);
        double yRes = regionParams.get(CoverageUtilities.YRES);

        final AffineTransform mt2D = (AffineTransform) inRaster.getGridGeometry().getGridToCRS2D(PixelOrientation.CENTER);
        final AffineTransformation awt2WorldTransformation = new AffineTransformation(mt2D.getScaleX(), mt2D.getShearX(),
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

        outVector = new DefaultFeatureCollection();

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
            polygon.apply(awt2WorldTransformation);
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

            if (doRemoveHoles) {
                LineString exteriorRing = polygon.getExteriorRing();
                polygon = gf.createPolygon(exteriorRing.getCoordinates());
            }

            area = polygon.getArea();
            double perim = polygon.getLength();
            org.locationtech.jts.geom.Point centroid = polygon.getCentroid();
            Coordinate centroidCoord = centroid.getCoordinate();
            Object[] values = new Object[]{polygon, featureIndex, tmpValue, area, perim, centroidCoord.x, centroidCoord.y};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(type.getTypeName() + "." + featureIndex);
            featureIndex++;
            ((DefaultFeatureCollection) outVector).add(feature);
        }
    }

    private GridCoverage2D maskRaster() {
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        RandomIter rasterIter = CoverageUtilities.getRandomIterator(inRaster);
        WritableRaster[] holder = new WritableRaster[1];
        GridCoverage2D outGC = CoverageUtilities.createCoverageFromTemplate(inRaster, novalue, holder);
        WritableRandomIter outIter = RandomIterFactory.createWritable(holder[0], null);

        pm.beginTask("Masking map...", nRows);
        for( int r = 0; r < nRows; r++ ) {
            for( int c = 0; c < nCols; c++ ) {
                double value = rasterIter.getSampleDouble(c, r, 0);
                boolean doNull = false;
                if (!isNovalue(value, novalue)) {
                    if (!Double.isNaN(pMaskThreshold)) {
                        // check threshold
                        if (value < pMaskThreshold) {
                            doNull = true;
                        } else {
                            doNull = false;
                        }
                    }
                } else {
                    doNull = true;
                }
                if (!doNull)
                    outIter.setSample(c, r, 0, 1);
            }
            pm.worked(1);
        }
        pm.done();
        return outGC;
    }

    private void doRegionCheck() throws TransformException {
        if (doRegioncheck) {

            int left = Integer.MAX_VALUE;
            int right = -Integer.MAX_VALUE;
            int top = -Integer.MAX_VALUE;
            int bottom = Integer.MAX_VALUE;

            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
            int cols = regionMap.getCols();
            int rows = regionMap.getRows();

            pm.beginTask("Try to shrink the region over covered area...", rows);
            RandomIter rasterIter = CoverageUtilities.getRandomIterator(inRaster);
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double value = rasterIter.getSampleDouble(c, r, 0);
                    if (!isNovalue(value)) {
                        left = min(left, c);
                        right = max(right, c);
                        top = max(top, r);
                        bottom = min(bottom, r);
                    }
                }
                pm.worked(1);
            }
            pm.done();
            rasterIter.done();

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
