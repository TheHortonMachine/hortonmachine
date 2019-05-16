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
package org.hortonmachine.gears.io.las;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.index.LasIndexer;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Abstract las data manager class.
 * 
 * <p>This is used to create the data manager, being it 
 * from single las file or index folder.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class ALasDataManager implements AutoCloseable {
    protected GeometryFactory gf = GeometryUtilities.gf();
    protected double[] intensityRange;
    protected double[] impulses;
    protected int impulsesNum = -1;
    protected double[] classes;
    protected boolean hasConstraint = false;

    protected CoordinateReferenceSystem crs;

    /**
     * Factory method to create {@link ALasDataManager}.
     * 
     * @param lasFile the las file or las folder index file.
     * @param inDem a dem to normalize the elevation. If <code>!null</code>, the height over the dtm is added as {@link LasRecord#groundElevation}.
     * @param elevThreshold a threshold to use for the elevation normalization.
     * @param inCrs the data {@link org.opengis.referencing.crs.CoordinateReferenceSystem}. if null, the one of the dem is read, if available.
     */
    public static ALasDataManager getDataManager( File dataFile, GridCoverage2D inDem, double elevThreshold,
            CoordinateReferenceSystem inCrs ) {
        String lcName = dataFile.getName().toLowerCase();
        if (lcName.endsWith(".las") || lcName.endsWith(".laz")) {
            return new LasFileDataManager(dataFile, inDem, elevThreshold, inCrs);
        } else if (lcName.equals(LasIndexer.INDEX_LASFOLDER)) {
            return new LasFolderIndexDataManager(dataFile, inDem, elevThreshold, inCrs);
        } else {
            try {
                EDb edb = EDb.fromFileDesktop(dataFile);
                if (edb != null && edb.isSpatial()) {
                    return new DatabaseLasDataManager(dataFile, inDem, elevThreshold, inCrs);
                }
            } catch (Exception e) {
                // ignore, will be handled
            }

            throw new IllegalArgumentException("Can only read .las and " + LasIndexer.INDEX_LASFOLDER + " files.");
        }
    }

    /**
     * @return the file representing the dataset.
     */
    public abstract File getFile();

    /**
     * Open the main folder file and read the main index.
     *
     * @throws Exception
     */
    public abstract void open() throws Exception;

    public void setIntensityConstraint( double[] minMax ) {
        if (minMax == null)
            return;
        intensityRange = minMax;
        hasConstraint = true;
    }

    public void setImpulsesConstraint( double[] impulsesToKeep ) {
        if (impulsesToKeep == null)
            return;
        impulses = impulsesToKeep;
        hasConstraint = true;
    }

    public void setImpulsesNumConstraint( int impulsesNumToKeep ) {
        impulsesNum = impulsesNumToKeep;
        hasConstraint = true;
    }

    public void setClassesConstraint( double[] classesToKeep ) {
        if (classesToKeep == null)
            return;
        classes = classesToKeep;
        hasConstraint = true;
    }

    /**
     * Get points inside a given geometry boundary.
     *
     * @param checkGeom the {@link org.locationtech.jts.geom.Geometry} to use to check.
     * @param doOnlyEnvelope check for the geom envelope instead of a intersection with it.
     * @return the list of points contained in the supplied geometry.
     * @throws Exception
     */
    public abstract List<LasRecord> getPointsInGeometry( Geometry checkGeom, boolean doOnlyEnvelope ) throws Exception;

    /**
     * Retrieve all the trees envelopes that intersect the geometry.
     *
     * @param checkGeom the {@link org.locationtech.jts.geom.Geometry} to use to check.
     * @param doOnlyEnvelope check for the geom envelope instead of a intersection with it.
     * @param minMaxZ an array to be filled with the min and max z to be used as style.
     * @return the list of envelopes contained in the supplied geometry.
     * @throws Exception
     */
    public abstract List<Geometry> getEnvelopesInGeometry( Geometry checkGeom, boolean doOnlyEnvelope, double[] minMaxZ )
            throws Exception;

    /**
     * Get the overall envelope of the las folder.
     *
     * <p>This reads the data from the index.</p>
     *
     * @return the {@link org.geotools.geometry.jts.ReferencedEnvelope} of the data.
     * @throws Exception
     */
    public abstract ReferencedEnvelope getOverallEnvelope() throws Exception;

    /**
     * Getter for the list of envelopes of all las files.
     *
     * @return the list of {@link org.geotools.geometry.jts.ReferencedEnvelope}s.
     * @throws Exception
     */
    public abstract List<ReferencedEnvelope> getEnvelopeList() throws Exception;

    /**
     * Get the overall envelope 3d of the las folder.
     *
     * <p>Warning: this needs to open all involved readers.
     *
     * @return the {@link org.geotools.geometry.jts.ReferencedEnvelope3D} of the data.
     * @throws Exception
     */
    public abstract ReferencedEnvelope3D getEnvelope3D() throws Exception;

    /**
     * Creates a polygon {@link org.geotools.data.simple.SimpleFeatureCollection} for all las files.
     *
     * @return the features of teh las file bounds.
     * @throws Exception
     */
    public abstract SimpleFeatureCollection getOverviewFeatures() throws Exception;

    /**
     * Retrieve all the envelope features that intersect the geometry.
     *
     * <p>an elev attribute is added with the max elev contained in the envelope.
     *
     * @param checkGeom the {@link org.locationtech.jts.geom.Geometry} to use to check.
     * @param doOnlyEnvelope check for the geom envelope instead of a intersection with it.
     * @param minMaxZI an array to be filled with the [minz,maxz, minintensity, maxintensity] to be used as style.
     * @param doPoints if <code>true</code>, create points instead of polygons.
     * @return the features of the envelopes contained in the supplied geometry.
     * @throws Exception
     */
    public synchronized SimpleFeatureCollection getEnvelopeFeaturesInGeometry( Geometry checkGeom, boolean doOnlyEnvelope,
            double[] minMaxZI, boolean doPoints ) throws Exception {
        List<Geometry> envelopesInGeometry = getEnvelopesInGeometry(checkGeom, doOnlyEnvelope, null);

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("overview");
        b.setCRS(crs);
        if (!doPoints) {
            b.add("the_geom", Polygon.class);
        } else {
            b.add("the_geom", Point.class);
        }
        b.add("elev", Double.class);
        b.add("intensity", Double.class);

        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        double minI = Double.POSITIVE_INFINITY;
        double maxI = Double.NEGATIVE_INFINITY;

        DefaultFeatureCollection newFeatures = new DefaultFeatureCollection();
        for( int i = 0; i < envelopesInGeometry.size(); i++ ) {
            Geometry geom = envelopesInGeometry.get(i);
            if (doPoints) {
                Envelope envelope = geom.getEnvelopeInternal();
                Coordinate centre = envelope.centre();
                geom = gf.createPoint(centre);
            }

            double elev = -9999.0;
            double intens = -9999.0;
            Object userData = geom.getUserData();
            if (userData instanceof double[]) {
                double[] data = (double[]) userData;
                elev = data[0];
                intens = data[1];
            }

            if (minMaxZI != null) {
                minZ = Math.min(minZ, elev);
                maxZ = Math.max(maxZ, elev);
                minI = Math.min(minI, intens);
                maxI = Math.max(maxI, intens);
            }

            Object[] objs = new Object[]{geom, elev, intens};
            builder.addAll(objs);
            SimpleFeature feature = builder.buildFeature(null);
            newFeatures.add(feature);
        }

        if (minMaxZI != null) {
            minMaxZI[0] = minZ;
            minMaxZI[1] = maxZ;
            minMaxZI[2] = minI;
            minMaxZI[3] = maxI;
        }
        return newFeatures;
    }

    /**
     * Check the point for constraints.
     *
     * @param lasDot the point to check.
     * @return <code>true</code> if the point is accepted.
     */
    protected boolean doAccept( LasRecord lasDot ) {
        if (!hasConstraint) {
            return true;
        }
        boolean takeIt = true;
        if (intensityRange != null) {
            short intensity = lasDot.intensity;
            if (intensity >= intensityRange[0] && intensity <= intensityRange[1]) {
                takeIt = true;
            } else {
                return false;
            }
        }
        if (impulses != null) {
            int impulse = lasDot.returnNumber;
            takeIt = false;
            for( final double imp : impulses ) {
                if (impulse == (int) imp) {
                    takeIt = true;
                    break;
                }
            }
            if (!takeIt)
                return false;
        }
        if (impulsesNum != -1) {
            int numOfReturns = lasDot.numberOfReturns;
            if (numOfReturns != (int) impulsesNum) {
                return false;
            }
        }
        if (classes != null) {
            int classification = lasDot.classification;
            takeIt = false;
            for( final double classs : classes ) {
                if (classification == (int) classs) {
                    // since it is the last checked, if it is true, accept it
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Extracts the points contained inside a vertical range from the supplied list of points.
     *
     * @param pointsList the list os {@link org.hortonmachine.gears.io.las.core.LasRecord points}.
     * @param min the min value of the range.
     * @param max the max value of the range.
     * @param isGroundElev if <code>true</code>, ground elevation is used instead of z.
     * @return the points contained in the range.
     */
    public static List<LasRecord> getPointsInVerticalRange( List<LasRecord> pointsList, double min, double max,
            boolean isGroundElev ) {
        ArrayList<LasRecord> pointsListInVertical = new ArrayList<LasRecord>();
        if (!isGroundElev) {
            for( LasRecord lasRecord : pointsList ) {
                if (NumericsUtilities.isBetween(lasRecord.z, min, max)) {
                    pointsListInVertical.add(lasRecord);
                }
            }
        } else {
            for( LasRecord lasRecord : pointsList ) {
                if (NumericsUtilities.isBetween(lasRecord.groundElevation, min, max)) {
                    pointsListInVertical.add(lasRecord);
                }
            }
        }
        return pointsListInVertical;
    }

    /**
     * Extracts the points contained inside a height from ground range from the supplied list of points.
     *
     * <p>No check is done on the existence of the ground height value.
     *
     * @param pointsList the list os {@link org.hortonmachine.gears.io.las.core.LasRecord points}.
     * @param min the min value of the range.
     * @param max the max value of the range.
     * @return the points contained in the range.
     */
    public static List<LasRecord> getPointsInHeightRange( List<LasRecord> pointsList, double min, double max ) {
        ArrayList<LasRecord> pointsListInVertical = new ArrayList<LasRecord>();
        for( LasRecord lasRecord : pointsList ) {
            if (NumericsUtilities.isBetween(lasRecord.groundElevation, min, max)) {
                pointsListInVertical.add(lasRecord);
            }
        }
        return pointsListInVertical;
    }
}