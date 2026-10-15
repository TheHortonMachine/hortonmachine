/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears.modules.v.vectorcleaner;

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_KEYWORDS;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_LABEL;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_LICENSE;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_NAME;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_STATUS;
import static org.hortonmachine.gears.modules.v.vectorclipper.OmsVectorClipper.getDimension;
import static org.hortonmachine.gears.modules.v.vectorclipper.OmsVectorClipper.multiClass;
import static org.hortonmachine.gears.modules.v.vectorclipper.OmsVectorClipper.partsOfDimension;

import java.util.ArrayList;
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureGeometrySubstitutor;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EPrecision;
import org.hortonmachine.gears.utils.geometry.HMPrecision;
import org.locationtech.jts.algorithm.construct.MaximumInscribedCircle;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.geom.util.LinearComponentExtracter;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.overlayng.OverlayNG;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;

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

@Description(OMSVECTORCLEANER_DESCRIPTION)
@Documentation(OMSVECTORCLEANER_DOCUMENTATION)
@Author(name = OMSVECTORCLEANER_AUTHORNAMES, contact = OMSVECTORCLEANER_AUTHORCONTACTS)
@Keywords(OMSVECTORCLEANER_KEYWORDS)
@Label(OMSVECTORCLEANER_LABEL)
@Name(OMSVECTORCLEANER_NAME)
@Status(OMSVECTORCLEANER_STATUS)
@License(OMSVECTORCLEANER_LICENSE)
public class OmsVectorCleaner extends HMModel {

    @Description(OMSVECTORCLEANER_IN_MAP_DESCRIPTION)
    @In
    public SimpleFeatureCollection inMap = null;

    @Description(OMSVECTORCLEANER_P_MAX_HOLE_AREA_DESCRIPTION)
    @In
    public double pMaxHoleArea = 300.0;

    @Description(OMSVECTORCLEANER_P_MIN_WIDTH_DESCRIPTION)
    @In
    public double pMinWidth = 10.0;

    @Description(OMSVECTORCLEANER_P_MIN_SLIVER_LENGTH_DESCRIPTION)
    @In
    public double pMinSliverLength = 30.0;

    @Description(OMSVECTORCLEANER_DO_FILL_NOTCHES_DESCRIPTION)
    @In
    public boolean doFillNotches = true;

    @Description(OMSVECTORCLEANER_DO_FILL_GAPS_DESCRIPTION)
    @In
    public boolean doFillGaps = true;

    @Description(OMSVECTORCLEANER_P_PRECISION_DESCRIPTION)
    @UI("combo:" + PRECISION_MM + "," + PRECISION_CM + "," + PRECISION_DM + "," + PRECISION_M)
    @In
    public String pPrecision = PRECISION_MM;

    @Description(OMSVECTORCLEANER_P_OVERLAPS_DESCRIPTION)
    @UI("combo:" + OVERLAPS_NONE + "," + OVERLAPS_MARK + "," + OVERLAPS_LARGEST + "," + OVERLAPS_FIRST)
    @In
    public String pOverlaps = OVERLAPS_NONE;

    @Description(OMSVECTORCLEANER_OUT_MAP_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outMap = null;

    @Description(OMSVECTORCLEANER_OUT_CHANGES_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outChanges = null;

    public static final String PRECISION_MM = "mm";
    public static final String PRECISION_CM = "cm";
    public static final String PRECISION_DM = "dm";
    public static final String PRECISION_M = "m";

    /** The overlaps between features are not looked for. */
    public static final String OVERLAPS_NONE = "none";
    /** The overlaps are only added to the changes. */
    public static final String OVERLAPS_MARK = "mark";
    /** The overlap stays with the larger feature and is removed from the smaller. */
    public static final String OVERLAPS_LARGEST = "largest";
    /** The overlap stays with the feature that comes first and is removed from the later. */
    public static final String OVERLAPS_FIRST = "first";

    /** The field of the changes with the id of the feature they belong to. */
    public static final String FIELD_ORIGID = "origid";
    /** The field of the changes with the kind of change. */
    public static final String FIELD_TYPE = "type";
    /** The field of the changes with the area of the piece. */
    public static final String FIELD_AREA = "area";

    /** The outline of an invalid geometry, before it was fixed. */
    public static final String TYPE_INVALID = "invalid";
    /** A hole, or the part of a hole, that was filled. */
    public static final String TYPE_HOLE = "hole";
    /** A narrow spike of the outline that was cut. */
    public static final String TYPE_CUT = "cut";
    /** A narrow notch or gap of the outline that was filled. */
    public static final String TYPE_FILL = "fill";
    /** A narrow gap between two parts of a feature that was filled, joining them. */
    public static final String TYPE_GAP = "gap";
    /** The part shared by two features, with the ids of both. */
    public static final String TYPE_OVERLAP = "overlap";

    public static final String OMSVECTORCLEANER_DESCRIPTION = "Fixes invalid geometries, removes the empty ones and cleans polygons of small holes and narrow slivers.";
    public static final String OMSVECTORCLEANER_DOCUMENTATION = """
            Invalid geometries, as polygons that cross themselves, are fixed, keeping as much as \
            possible of their shape and only their parts of the same dimension, and the features \
            without a geometry or with an empty one are removed. This is done for points, lines and \
            polygons.

            Polygons can also be cleaned. Holes with an area up to the max hole area are filled. \
            Holes, or the parts of holes, narrower than the min width are filled too, as the gap \
            between two parcels digitized not exactly on top of each other. On the outline, the \
            narrow spikes are cut and the narrow notches and gaps between parts filled, if they are \
            longer than the min sliver length, so that the sharp corners stay. The width is the \
            diameter of the biggest circle that fits in the piece, the length half its perimeter, \
            which works also for bent slivers. A notch is in the outline of a single polygon, a gap \
            is between two or more parts of the feature, which filling it joins: the filling of each \
            can be switched off. Only the pieces to clean are touched, the rest of the \
            outline keeps its vertices. A feature that is itself a sliver is removed. Each feature is \
            cleaned on its own: in a vector of adjacent polygons, a filled hole or notch can overlap \
            a neighbor.

            The overlaps between polygons, also those made by the cleaning, can then be marked in \
            the changes, or resolved: the overlap is kept by the larger of the two features, or by \
            the one that comes first in the vector, as by priority, and removed from the other, which \
            is cleaned again of the slivers it is left with. A feature can be removed this way, if it \
            is all inside others.

            The overlays round the coordinates of the pieces they create to a grid of the precision, \
            which makes them robust against the floating point arithmetic. In degrees, as for \
            EPSG:4326, the precision is taken at the equator. Areas, widths and lengths are in the \
            units of the coordinate reference system: better use a projected one.

            The changes vector marks where the cleaning happened, with the outlines of the invalid \
            geometries before they were fixed, of the filled holes, of the cut spikes, of the \
            filled notches and gaps and of the overlaps, each with the id of its feature (of both, for an \
            overlap), the kind of change and its area.
            """;
    public static final String OMSVECTORCLEANER_KEYWORDS = "JTS, Clean, Invalid, Holes, Slivers, Vector";
    public static final String OMSVECTORCLEANER_LABEL = VECTORPROCESSING;
    public static final String OMSVECTORCLEANER_NAME = "vcleaner";
    public static final int OMSVECTORCLEANER_STATUS = 40;
    public static final String OMSVECTORCLEANER_LICENSE = "http://www.gnu.org/licenses/gpl-3.0.html";
    public static final String OMSVECTORCLEANER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSVECTORCLEANER_AUTHORCONTACTS = "https://g-ant.eu";
    public static final String OMSVECTORCLEANER_IN_MAP_DESCRIPTION = "The vector to clean: points, lines or polygons.";
    public static final String OMSVECTORCLEANER_P_MAX_HOLE_AREA_DESCRIPTION = "The max area of the holes to fill. 0 fills none.";
    public static final String OMSVECTORCLEANER_P_MIN_WIDTH_DESCRIPTION = "The min width of holes and of spikes and notches of the outline: narrower ones are cleaned. 0 cleans none.";
    public static final String OMSVECTORCLEANER_P_MIN_SLIVER_LENGTH_DESCRIPTION = "The min length of a narrow spike or notch of the outline to be cleaned, to keep the sharp corners.";
    public static final String OMSVECTORCLEANER_DO_FILL_NOTCHES_DESCRIPTION = "Fill the narrow notches of the outline.";
    public static final String OMSVECTORCLEANER_DO_FILL_GAPS_DESCRIPTION = "Fill the narrow gaps between the parts of a feature, joining them.";
    public static final String OMSVECTORCLEANER_P_PRECISION_DESCRIPTION ="The precision of the overlays: mm, cm, dm or m.";
    public static final String OMSVECTORCLEANER_P_OVERLAPS_DESCRIPTION = "What to do with the overlaps between polygons: none (not looked for), mark (only added to the changes), largest (kept by the larger feature) or first (kept by the feature that comes first).";
    public static final String OMSVECTORCLEANER_OUT_MAP_DESCRIPTION = "The cleaned features, with their attributes.";
    public static final String OMSVECTORCLEANER_OUT_CHANGES_DESCRIPTION = "The outlines of what was fixed or cleaned, as lines.";

    private final GeometryFactory gf = new GeometryFactory();
    private HMPrecision precision;
    private BufferParameters bufferParameters;
    private double noiseArea;

    private SimpleFeatureBuilder changesBuilder;
    private DefaultFeatureCollection changesFC;

    private int filledHoles = 0;
    private double filledHolesArea = 0;
    private double cutArea = 0;
    private double filledArea = 0;
    private double filledGapsArea = 0;
    private int overlapsCount = 0;
    private double overlapsArea = 0;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMap == null, doReset)) {
            return;
        }
        checkNull(inMap, pPrecision);
        if (pMaxHoleArea < 0 || pMinWidth < 0 || pMinSliverLength < 0) {
            throw new ModelsIllegalargumentException("The max hole area, the min width and the min sliver length can't be negative.", this);
        }
        EPrecision ePrecision;
        try {
            ePrecision = EPrecision.forName(pPrecision);
        } catch (IllegalArgumentException e) {
            throw new ModelsIllegalargumentException(e.getMessage(), this);
        }
        String overlapsMode = pOverlaps == null ? OVERLAPS_NONE : pOverlaps.trim().toLowerCase();
        if (!List.of(OVERLAPS_NONE, OVERLAPS_MARK, OVERLAPS_LARGEST, OVERLAPS_FIRST).contains(overlapsMode)) {
            throw new ModelsIllegalargumentException(
                    "Unknown overlaps handling: " + pOverlaps + ". Use one of none, mark, largest or first.", this);
        }

        int dimension = getDimension(inMap, this);
        CoordinateReferenceSystem crs = inMap.getSchema().getCoordinateReferenceSystem();
        precision = new HMPrecision(ePrecision, crs);
        // mitre joins keep the corners of what is not cleaned sharp
        bufferParameters = new BufferParameters();
        bufferParameters.setJoinStyle(BufferParameters.JOIN_MITRE);
        bufferParameters.setMitreLimit(5.0);
        // no simplification of the input, which by default moves the outline by up to 1% of the
        // distance: the opening and closing would then give long thin strips along the borders
        // that are not there, cut or filled as slivers
        bufferParameters.setSimplifyFactor(0.0);
        // pieces below a cell of the grid are the noise of the rounding, while a hairline gap of a
        // mm along hundreds of meters is a real sliver
        noiseArea = Math.pow(precision.getGridSize(), 2);
        boolean doCleanPolygons = dimension == 2 && (pMaxHoleArea > 0 || pMinWidth > 0);

        FeatureGeometrySubstitutor substitutor = new FeatureGeometrySubstitutor(inMap.getSchema(), multiClass(dimension));
        DefaultFeatureCollection outFC = new DefaultFeatureCollection("cleaned", substitutor.getNewFeatureType());

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("changes");
        b.setCRS(crs);
        b.add("the_geom", MultiLineString.class);
        b.add(FIELD_ORIGID, String.class);
        b.add(FIELD_TYPE, String.class);
        b.add(FIELD_AREA, Double.class);
        SimpleFeatureType changesType = b.buildFeatureType();
        changesBuilder = new SimpleFeatureBuilder(changesType);
        changesFC = new DefaultFeatureCollection("changes", changesType);

        int emptyCount = 0;
        int invalidCount = 0;
        int removedCount = 0;
        double areaBefore = 0;
        double areaAfter = 0;
        List<SimpleFeature> features = FeatureUtilities.featureCollectionToList(inMap);
        List<SimpleFeature> keptFeatures = new ArrayList<>();
        List<Geometry> keptGeometries = new ArrayList<>();
        pm.beginTask("Cleaning...", features.size());
        for( SimpleFeature feature : features ) {
            pm.worked(1);
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            if (geometry == null || geometry.isEmpty()) {
                emptyCount++;
                continue;
            }
            String id = feature.getID();
            if (!geometry.isValid()) {
                invalidCount++;
                addChange(id, TYPE_INVALID, geometry);
                geometry = GeometryFixer.fix(geometry);
            }
            // after the fix, as the area of a geometry that crosses itself is meaningless
            areaBefore += geometry.getArea();
            Geometry cleaned = partsOfDimension(geometry, dimension);
            if (cleaned != null && doCleanPolygons) {
                cleaned = cleanPolygons(cleaned, id);
            }
            if (cleaned == null) {
                removedCount++;
                continue;
            }
            keptFeatures.add(feature);
            keptGeometries.add(cleaned);
        }
        pm.done();

        if (dimension == 2 && !overlapsMode.equals(OVERLAPS_NONE)) {
            handleOverlaps(keptFeatures, keptGeometries, overlapsMode);
        }

        for( int i = 0; i < keptFeatures.size(); i++ ) {
            Geometry geometry = keptGeometries.get(i);
            if (geometry == null) {
                // all of it was given to the overlapping features
                removedCount++;
                continue;
            }
            areaAfter += geometry.getArea();
            outFC.add(substitutor.substituteGeometry(keptFeatures.get(i), geometry));
        }
        outMap = outFC;
        outChanges = changesFC;

        pm.message("Invalid geometries fixed: " + invalidCount);
        pm.message("Features without geometry removed: " + emptyCount);
        pm.message("Features left empty after the cleaning removed: " + removedCount);
        if (dimension == 2) {
            pm.message(String.format("Holes filled: %d, with an area of %,.2f", filledHoles, filledHolesArea));
            pm.message(String.format("Area of the spikes cut: %,.2f", cutArea));
            pm.message(String.format("Area of the notches filled: %,.2f", filledArea));
            pm.message(String.format("Area of the gaps between parts filled: %,.2f", filledGapsArea));
            if (!overlapsMode.equals(OVERLAPS_NONE)) {
                pm.message(String.format("Overlaps found: %d, with an area of %,.2f", overlapsCount, overlapsArea));
            }
            double diff = areaAfter - areaBefore;
            double percentage = areaBefore > 0 ? 100.0 * diff / areaBefore : 0;
            pm.message(String.format("Total area before the cleaning: %,.2f, after: %,.2f, difference: %+,.2f (%+.4f %%)", areaBefore,
                    areaAfter, diff, percentage));
        }
    }

    /**
     * Removes the small and narrow holes, then the slivers of the outline.
     *
     * @param geometry the valid polygons of a feature.
     * @param id the id of the feature, for the changes.
     * @return the cleaned polygons or <code>null</code>, if nothing is left.
     */
    private Geometry cleanPolygons( Geometry geometry, String id ) {
        List<Polygon> polygons = new ArrayList<>();
        for( Geometry part : pieces(geometry) ) {
            Geometry cleaned = cleanHoles((Polygon) part, id);
            if (cleaned != null) {
                for( Geometry p : pieces(cleaned) ) {
                    polygons.add((Polygon) p);
                }
            }
        }
        if (polygons.isEmpty()) {
            return null;
        }
        Geometry result = gf.createMultiPolygon(GeometryFactory.toPolygonArray(polygons));
        if (pMinWidth > 0) {
            result = removeOuterSlivers(result, id);
        }
        return result;
    }

    /**
     * Fills the holes with an area up to the max hole area, and those or their parts narrower than
     * the min width.
     *
     * <p>The kept holes are not touched, the overlay is done only if parts of a hole are filled.</p>
     */
    private Geometry cleanHoles( Polygon polygon, String id ) {
        List<LinearRing> keptRings = new ArrayList<>();
        List<Geometry> keptParts = new ArrayList<>();
        for( int i = 0; i < polygon.getNumInteriorRing(); i++ ) {
            LinearRing ring = polygon.getInteriorRingN(i);
            Polygon hole = gf.createPolygon(ring);
            double holeArea = hole.getArea();
            if (holeArea <= pMaxHoleArea) {
                fillHole(id, hole, holeArea);
                continue;
            }
            if (pMinWidth <= 0) {
                keptRings.add(ring);
                continue;
            }
            // drop the narrow holes and the narrow tails of the bigger ones
            List<Geometry> parts = new ArrayList<>();
            double partsArea = 0;
            for( Geometry part : pieces(opening(hole, true)) ) {
                double partArea = part.getArea();
                if (partArea > pMaxHoleArea && partArea > noiseArea) {
                    parts.add(part);
                    partsArea += partArea;
                }
            }
            if (parts.isEmpty()) {
                fillHole(id, hole, holeArea);
            } else if (parts.size() == 1 && Math.abs(holeArea - partsArea) <= noiseArea) {
                // nothing narrow in it
                keptRings.add(ring);
            } else {
                keptParts.addAll(parts);
                filledHoles++;
                filledHolesArea += holeArea - partsArea;
                // with the floating point overlay, as the hairlines of the hole, that the rounding
                // collapses, are filled too and must be traced
                Geometry filled = polygonal(
                        OverlayNGRobust.overlay(hole, OverlayNGRobust.union(gf.buildGeometry(parts)), OverlayNG.DIFFERENCE));
                for( Geometry piece : pieces(filled) ) {
                    if (piece.getArea() > noiseArea) {
                        addChange(id, TYPE_HOLE, piece);
                    }
                }
            }
        }
        Polygon withKeptRings = gf.createPolygon(polygon.getExteriorRing(), GeometryFactory.toLinearRingArray(keptRings));
        if (keptParts.isEmpty()) {
            return withKeptRings;
        }
        Geometry cleaned = polygonal(precision.difference(withKeptRings, precision.union(keptParts)));
        return cleaned.isEmpty() ? null : cleaned;
    }

    private void fillHole( String id, Polygon hole, double holeArea ) {
        filledHoles++;
        filledHolesArea += holeArea;
        addChange(id, TYPE_HOLE, hole);
    }

    /**
     * Cuts the narrow spikes and fills the narrow notches and gaps of the outline, touching only
     * the slivers and leaving the rest of the outline as it is.
     */
    private Geometry removeOuterSlivers( Geometry geometry, String id ) {
        Geometry result = cutSlivers(geometry, id);
        if (result == null) {
            return null;
        }
        if (!doFillNotches && !doFillGaps) {
            return result;
        }
        // narrow notches and gaps between parts -> fill
        List<Geometry> toFill = new ArrayList<>();
        for( Geometry piece : slivers(precision.difference(closing(result), result)) ) {
            boolean isGap = isGap(piece, result);
            if (isGap ? doFillGaps : doFillNotches) {
                toFill.add(piece);
                if (isGap) {
                    filledGapsArea += piece.getArea();
                } else {
                    filledArea += piece.getArea();
                }
                addChange(id, isGap ? TYPE_GAP : TYPE_FILL, piece);
            }
        }
        if (!toFill.isEmpty()) {
            result = polygonal(precision.union(result, precision.union(toFill)));
        }
        return result;
    }

    /**
     * @return <code>true</code> if the piece touches two or more separate polygons of the
     *         geometry, as a gap between them, <code>false</code> if only one, as a notch.
     */
    private boolean isGap( Geometry piece, Geometry geometry ) {
        int touched = 0;
        for( Geometry part : pieces(geometry) ) {
            if (piece.distance(part) <= precision.getGridSize()) {
                touched++;
                if (touched > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Cuts the narrow spikes of the outline.
     *
     * @return the geometry without the spikes or <code>null</code>, if nothing is left.
     */
    private Geometry cutSlivers( Geometry geometry, String id ) {
        List<Geometry> toCut = slivers(precision.difference(geometry, opening(geometry)));
        if (toCut.isEmpty()) {
            return geometry;
        }
        Geometry cut = polygonal(precision.union(toCut));
        cutArea += cut.getArea();
        for( Geometry piece : toCut ) {
            addChange(id, TYPE_CUT, piece);
        }
        Geometry result = polygonal(precision.difference(geometry, cut));
        return result.isEmpty() ? null : result;
    }

    /**
     * Finds the overlaps between the polygons and, if asked, removes each from one of the two.
     *
     * <p>The pairs are taken in the order of the features, each with the geometries as they are
     * after the overlaps before it have been resolved.</p>
     *
     * @param features the features.
     * @param geometries their cleaned geometries, changed in place: <code>null</code> for one
     *        that is left empty.
     * @param mode {@link #OVERLAPS_MARK}, {@link #OVERLAPS_LARGEST} or {@link #OVERLAPS_FIRST}.
     */
    private void handleOverlaps( List<SimpleFeature> features, List<Geometry> geometries, String mode ) {
        int count = geometries.size();
        // the size before anything is removed, so that the result doesn't depend on the order
        double[] areas = new double[count];
        STRtree tree = new STRtree();
        for( int i = 0; i < count; i++ ) {
            Geometry geometry = geometries.get(i);
            areas[i] = geometry.getArea();
            tree.insert(geometry.getEnvelopeInternal(), i);
        }
        pm.beginTask("Looking for overlaps...", count);
        for( int i = 0; i < count; i++ ) {
            pm.worked(1);
            Geometry geometry = geometries.get(i);
            if (geometry == null) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Integer> candidates = tree.query(geometry.getEnvelopeInternal());
            candidates.sort(null);
            for( int j : candidates ) {
                Geometry other = geometries.get(j);
                if (j <= i || other == null || !geometry.intersects(other)) {
                    continue;
                }
                Geometry overlap = polygonal(precision.intersection(geometry, other));
                double overlapArea = overlap.getArea();
                if (overlapArea <= noiseArea || overlap.isEmpty()) {
                    // they only touch, or the noise of the rounding
                    continue;
                }
                overlapsCount++;
                overlapsArea += overlapArea;
                addChange(features.get(i).getID() + ";" + features.get(j).getID(), TYPE_OVERLAP, overlap);
                if (mode.equals(OVERLAPS_MARK)) {
                    continue;
                }
                // the one that gives the overlap away: the smaller, or the later
                boolean giveFromFirst = mode.equals(OVERLAPS_LARGEST) && areas[i] < areas[j];
                int loser = giveFromFirst ? i : j;
                Geometry reduced = polygonal(precision.difference(geometries.get(loser), overlap));
                if (!reduced.isEmpty() && pMinWidth > 0) {
                    reduced = cutSlivers(reduced, features.get(loser).getID());
                }
                geometries.set(loser, reduced == null || reduced.isEmpty() ? null : reduced);
                if (giveFromFirst) {
                    geometry = geometries.get(i);
                    if (geometry == null) {
                        break;
                    }
                }
            }
        }
        pm.done();
    }

    /**
     * @return the polygons of the geometry that are narrower than the min width and longer than
     *         the min sliver length.
     */
    private List<Geometry> slivers( Geometry geometry ) {
        List<Geometry> slivers = new ArrayList<>();
        for( Geometry piece : pieces(polygonal(geometry)) ) {
            if (piece.getArea() < noiseArea) {
                continue;
            }
            double width = 2 * new MaximumInscribedCircle(piece, pMinWidth / 20.0).getRadiusLine().getLength();
            // half the perimeter works also for bent slivers
            double length = piece.getLength() / 2.0;
            if (width < pMinWidth && length > pMinSliverLength) {
                slivers.add(piece);
            }
        }
        return slivers;
    }

    /**
     * Morphological opening: removes every part of the geometry narrower than the min width.
     */
    private Geometry opening( Geometry geometry ) {
        return opening(geometry, false);
    }

    /**
     * @param floating if <code>true</code>, the result is cut with the floating point overlay,
     *        so that its edges along the geometry are exactly those of the geometry, as needed to
     *        compute what the opening removed, also the hairlines narrower than the precision.
     */
    private Geometry opening( Geometry geometry, boolean floating ) {
        double half = pMinWidth / 2.0;
        Geometry shrunk = BufferOp.bufferOp(geometry, -half, bufferParameters);
        if (shrunk.isEmpty()) {
            return gf.createMultiPolygon();
        }
        Geometry grown = BufferOp.bufferOp(shrunk, half, bufferParameters);
        if (floating) {
            return polygonal(OverlayNGRobust.overlay(grown, geometry, OverlayNG.INTERSECTION));
        }
        return polygonal(precision.intersection(grown, geometry));
    }

    /**
     * Morphological closing: fills every gap or notch of the geometry narrower than the min width.
     */
    private Geometry closing( Geometry geometry ) {
        double half = pMinWidth / 2.0;
        Geometry grown = BufferOp.bufferOp(geometry, half, bufferParameters);
        return polygonal(precision.union(BufferOp.bufferOp(grown, -half, bufferParameters), geometry));
    }

    /**
     * @return the polygons of an overlay, without the lines and points it can give where the
     *         geometries touch or the rounding collapsed a piece, as a multipolygon, empty if none.
     */
    private Geometry polygonal( Geometry geometry ) {
        Geometry polygons = partsOfDimension(geometry, 2);
        return polygons == null ? gf.createMultiPolygon() : polygons;
    }

    private static List<Geometry> pieces( Geometry geometry ) {
        List<Geometry> pieces = new ArrayList<>();
        for( int i = 0; i < geometry.getNumGeometries(); i++ ) {
            Geometry piece = geometry.getGeometryN(i);
            if (!piece.isEmpty()) {
                pieces.add(piece);
            }
        }
        return pieces;
    }

    /**
     * Adds the outline of a piece to the changes: the rings of polygons, lines as they are.
     * Points have no outline and are left out.
     */
    @SuppressWarnings("unchecked")
    private void addChange( String id, String type, Geometry piece ) {
        // also for collections, that have no boundary
        List<LineString> lines = new ArrayList<>(LinearComponentExtracter.getLines(piece, true));
        lines.removeIf(Geometry::isEmpty);
        if (lines.isEmpty()) {
            return;
        }
        changesBuilder.add(gf.createMultiLineString(GeometryFactory.toLineStringArray(lines)));
        changesBuilder.add(id);
        changesBuilder.add(type);
        changesBuilder.add(piece.getArea());
        changesFC.add(changesBuilder.buildFeature(null));
    }

}
