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
package org.hortonmachine.gears.modules.v.grids;

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.DefaultGridFeatureBuilder;
import org.geotools.grid.GridFeatureBuilder;
import org.geotools.grid.Grids;
import org.geotools.grid.Lines;
import org.geotools.grid.oblong.Oblongs;
import org.geotools.grid.ortholine.LineOrientation;
import org.geotools.grid.ortholine.OrthoLineDef;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@Description(OmsGridsGenerator.OMSGRIDSGENERATOR_DESCRIPTION)
@Documentation(OmsGridsGenerator.OMSGRIDSGENERATOR_DOCUMENTATION)
@Author(name = OmsGridsGenerator.OMSGRIDSGENERATOR_AUTHORNAMES, contact = OmsGridsGenerator.OMSGRIDSGENERATOR_AUTHORCONTACTS)
@Keywords(OmsGridsGenerator.OMSGRIDSGENERATOR_KEYWORDS)
@Label(OmsGridsGenerator.OMSGRIDSGENERATOR_LABEL)
@Name(OmsGridsGenerator.OMSGRIDSGENERATOR_NAME)
@Status(OmsGridsGenerator.OMSGRIDSGENERATOR_STATUS)
@License(OmsGridsGenerator.OMSGRIDSGENERATOR_LICENSE)
public class OmsGridsGenerator extends HMModel {
    @Description(OMSGRIDSGENERATOR_inVector_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector = null;

    @Description(OMSGRIDSGENERATOR_inRaster_DESCRIPTION)
    @In
    public GridCoverage2D inRaster = null;

    @Description(OMSGRIDSGENERATOR_pLon_DESCRIPTION)
    @In
    public Double pLon;

    @Description(OMSGRIDSGENERATOR_pLat_DESCRIPTION)
    @In
    public Double pLat;

    @Description(OMSGRIDSGENERATOR_pWidth_DESCRIPTION)
    @In
    public Double pWidth;

    @Description(OMSGRIDSGENERATOR_pHeight_DESCRIPTION)
    @In
    public Double pHeight;

    @Description(OMSGRIDSGENERATOR_pRows_DESCRIPTION)
    @In
    public int pRows = 10;

    @Description(OMSGRIDSGENERATOR_pCols_DESCRIPTION)
    @In
    public int pCols = 10;

    @Description(OMSGRIDSGENERATOR_pSpacing_DESCRIPTION)
    @In
    public Double pSpacing = null;

    @Description(OMSGRIDSGENERATOR_pType_DESCRIPTION)
    @UI("combo: " + POLYGON + "," + LINE + "," + POINT)
    @In
    public String pType = POLYGON;

    @Description(OMSGRIDSGENERATOR_pCode_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSGRIDSGENERATOR_outMap_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outMap = null;

    public static final String POINT = "point";
    public static final String LINE = "line";
    public static final String POLYGON = "polygon";

    // VARS DOCS START
    public static final String OMSGRIDSGENERATOR_DESCRIPTION = "Generates grid of lines or polygons.";
    public static final String OMSGRIDSGENERATOR_DOCUMENTATION = "";
    public static final String OMSGRIDSGENERATOR_KEYWORDS = "Vector, Grid";
    public static final String OMSGRIDSGENERATOR_LABEL = VECTORPROCESSING;
    public static final String OMSGRIDSGENERATOR_NAME = "gridgenerator";
    public static final int OMSGRIDSGENERATOR_STATUS = 40;
    public static final String OMSGRIDSGENERATOR_LICENSE = "http://www.gnu.org/licenses/gpl-3.0.html";
    public static final String OMSGRIDSGENERATOR_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSGRIDSGENERATOR_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSGRIDSGENERATOR_inVector_DESCRIPTION = "Optional vector map from which to take the bounds (if supplied, all other bounds related parameter are ignored). This needs pRows and pCols to be defined.";
    public static final String OMSGRIDSGENERATOR_inRaster_DESCRIPTION = "Optional raster map from which to take the bounds (if supplied, all other bounds related parameter are ignored. This needs pRows and pCols to be defined.";
    public static final String OMSGRIDSGENERATOR_pLon_DESCRIPTION = "The lower left longitude (needed if no map is supplied).";
    public static final String OMSGRIDSGENERATOR_pLat_DESCRIPTION = "The lower left latitude (needed if no map is supplied).";
    public static final String OMSGRIDSGENERATOR_pWidth_DESCRIPTION = "The optional grid cell width.";
    public static final String OMSGRIDSGENERATOR_pHeight_DESCRIPTION = "The optional grid cell height.";
    public static final String OMSGRIDSGENERATOR_pRows_DESCRIPTION = "The number of rows.";
    public static final String OMSGRIDSGENERATOR_pCols_DESCRIPTION = "The number of cols.";
    public static final String OMSGRIDSGENERATOR_pSpacing_DESCRIPTION = "The vertex spacing to use.";
    public static final String OMSGRIDSGENERATOR_pType_DESCRIPTION = "The output type.";
    public static final String OMSGRIDSGENERATOR_pCode_DESCRIPTION = "The code defining the coordinate reference system, composed by authority and code number (ex. EPSG:4328). Applied in the case the file is missing.";
    public static final String OMSGRIDSGENERATOR_outMap_DESCRIPTION = "The grid.";
    // VARS DOC END

    @Execute
    public void process() throws Exception {

        boolean isSquare;
        double s;
        double n;
        double w;
        double e;
        CoordinateReferenceSystem crs;
        if (inVector != null) {
            ReferencedEnvelope bounds = inVector.getBounds();
            crs = inVector.getSchema().getCoordinateReferenceSystem();
            s = bounds.getMinY();
            n = bounds.getMaxY();
            w = bounds.getMinX();
            e = bounds.getMaxX();
            pWidth = bounds.getWidth() / pCols;
            pHeight = bounds.getHeight() / pRows;
        } else if (inRaster != null) {
            Envelope2D bounds = inRaster.getGridGeometry().getEnvelope2D();
            crs = inRaster.getCoordinateReferenceSystem();
            s = bounds.getMinY();
            w = bounds.getMinX();
            if (pWidth != null && pHeight != null) {
                n = s + pWidth * pCols;
                e = w + pHeight * pRows;
            } else {
                n = bounds.getMaxY();
                e = bounds.getMaxX();
                pWidth = bounds.getWidth() / pCols;
                pHeight = bounds.getHeight() / pRows;
            }
        } else {
            checkNull(pCode, pLat, pLon);
            s = pLat;
            n = pLat + pRows * pHeight;
            w = pLon;
            e = pLon + pCols * pWidth;
            crs = CrsUtilities.getCrsFromEpsg(pCode, null);
        }
        isSquare = NumericsUtilities.dEq(pWidth, pHeight) ? true : false;

        outMap = new DefaultFeatureCollection();

        double delta = 0.000001;
        ReferencedEnvelope env = new ReferencedEnvelope(w, e + delta, s, n + delta, crs);

        pm.beginTask("Generating grid...", IHMProgressMonitor.UNKNOWN);

        GridFeatureBuilder builder = new DefaultGridFeatureBuilder(crs);
        SimpleFeatureSource grid;
        switch( pType ) {
        case POLYGON:
            if (isSquare) {
                if (pSpacing != null) {
                    grid = Grids.createSquareGrid(env, pWidth, pSpacing);
                } else {
                    grid = Grids.createSquareGrid(env, pWidth);
                }
            } else {
                if (pSpacing != null) {
                    grid = Oblongs.createGrid(env, pWidth, pHeight, pSpacing, builder);
                } else {
                    grid = Oblongs.createGrid(env, pWidth, pHeight, builder);
                }
            }
            outMap = grid.getFeatures();
            List<Geometry> geomsList = FeatureUtilities.featureCollectionToGeometriesList(outMap, true, null);
            createPolygons(crs, gf, geomsList);
            break;
        case LINE:
        case POINT:
            List<OrthoLineDef> lineDefs = Arrays.asList(//
                    new OrthoLineDef(LineOrientation.VERTICAL, 1, pHeight), //
                    new OrthoLineDef(LineOrientation.HORIZONTAL, 1, pWidth) //
                    );
            if (pSpacing != null) {
                grid = Lines.createOrthoLines(env, lineDefs, pSpacing, builder);
            } else {
                grid = Lines.createOrthoLines(env, lineDefs);
            }
            outMap = grid.getFeatures();
            GeometryFactory gf = GeometryUtilities.gf();
            List<Geometry> geoms = FeatureUtilities.featureCollectionToGeometriesList(outMap, true, null);
            List<LineString> verticals = new ArrayList<LineString>();
            List<LineString> horizontals = new ArrayList<LineString>();
            for( Geometry geometry : geoms ) {
                Envelope envelope = geometry.getEnvelopeInternal();
                Coordinate first = new Coordinate(envelope.getMinX(), envelope.getMinY());
                Coordinate last = new Coordinate(envelope.getMaxX(), envelope.getMaxY());
                LineString lineString = gf.createLineString(new Coordinate[]{first, last});
                if (envelope.getWidth() > envelope.getHeight()) {
                    horizontals.add(lineString);
                } else {
                    verticals.add(lineString);
                }
            }

            if (pType.equals(LINE)) {
                createLines(crs, verticals, horizontals);
            } else {
                createPoints(crs, gf, verticals, horizontals);
            }

            break;
        default:
            throw new ModelsIllegalargumentException("The supplied pType is not supported.", this, pm);
        }

        pm.done();
    }

    private void createPolygons( CoordinateReferenceSystem crs, GeometryFactory gf, List<Geometry> polygons ) {
        outMap = new DefaultFeatureCollection();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName(POLYGON);
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("id", Long.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder fbuilder = new SimpleFeatureBuilder(type);

        long index = 0;
        int numGeometries = polygons.size();
        for( int i = 0; i < numGeometries; i++ ) {
            Geometry geometry = polygons.get(i);
            Object[] values = new Object[]{geometry, index++};
            fbuilder.addAll(values);
            SimpleFeature feature = fbuilder.buildFeature(null);
            ((DefaultFeatureCollection) outMap).add(feature);
        }
    }

    private void createPoints( CoordinateReferenceSystem crs, GeometryFactory gf, List<LineString> verticals,
            List<LineString> horizontals ) {
        outMap = new DefaultFeatureCollection();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName(POINT);
        b.setCRS(crs);
        b.add("the_geom", Point.class);
        b.add("id", Long.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder fbuilder = new SimpleFeatureBuilder(type);

        Geometry gVer = gf.createMultiLineString(verticals.toArray(new LineString[0]));
        Geometry gHor = gf.createMultiLineString(horizontals.toArray(new LineString[0]));

        Geometry intersection = gHor.intersection(gVer);

        long index = 0;
        int numGeometries = intersection.getNumGeometries();
        for( int i = 0; i < numGeometries; i++ ) {
            Geometry geometry = intersection.getGeometryN(i);
            Object[] values = new Object[]{geometry, index++};
            fbuilder.addAll(values);
            SimpleFeature feature = fbuilder.buildFeature(null);
            ((DefaultFeatureCollection) outMap).add(feature);
        }
    }

    private void createLines( CoordinateReferenceSystem crs, List<LineString> verticals, List<LineString> horizontals ) {
        outMap = new DefaultFeatureCollection();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName(LINE);
        b.setCRS(crs);
        b.add("the_geom", MultiLineString.class);
        b.add("id", Long.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder fbuilder = new SimpleFeatureBuilder(type);

        long index = 0;
        for( LineString lineString : horizontals ) {
            Object[] values = new Object[]{lineString, index++};
            fbuilder.addAll(values);
            SimpleFeature feature = fbuilder.buildFeature(null);
            ((DefaultFeatureCollection) outMap).add(feature);
        }
        for( LineString lineString : verticals ) {
            Object[] values = new Object[]{lineString, index++};
            fbuilder.addAll(values);
            SimpleFeature feature = fbuilder.buildFeature(null);
            ((DefaultFeatureCollection) outMap).add(feature);
        }
    }
}
