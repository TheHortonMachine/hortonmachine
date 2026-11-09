package org.hortonmachine.gears.io.las.utils;

import static org.hortonmachine.gears.utils.math.NumericsUtilities.dEq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.ui.progress.IProgressPrinter;
import org.hortonmachine.gears.ui.progress.ProgressUpdate;
import org.hortonmachine.gears.utils.colors.ColorInterpolator;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.locationtech.jts.geom.Envelope;

public class LasConstraints {
    private Integer sampling = null;

    private int[] impulsesConstrains = null;
    private int[] classesConstrains = null;

    private Double minIntensityConstrain = null;
    private Double maxIntensityConstrain = null;

    private Double westConstrain = null;
    private Double eastConstrain = null;
    private Double southConstrain = null;
    private Double northConstrain = null;
    private Double minZConstrain = null;
    private Double maxZConstrain = null;
    private Double lowerThresConstrain = null;
    private Double upperThresConstrain = null;

    private double minInt = Double.POSITIVE_INFINITY;
    private double maxInt = Double.NEGATIVE_INFINITY;
    private double minClass = Double.POSITIVE_INFINITY;
    private double maxClass = Double.NEGATIVE_INFINITY;
    private double minImpulse = Double.POSITIVE_INFINITY;
    private double maxImpulse = Double.NEGATIVE_INFINITY;
    private double minElevation = Double.POSITIVE_INFINITY;
    private double maxElevation = Double.NEGATIVE_INFINITY;
    private double minGroundHeight = Double.POSITIVE_INFINITY;
    private double maxGroundHeight = Double.NEGATIVE_INFINITY;

    private Envelope filteredEnvelope;

    private GridCoverage2D dtm;

    private double minIntensityConstrainD;

    private double maxIntensityConstrainD;

    private double westConstrainD;

    private double eastConstrainD;

    private double southConstrainD;

    private double northConstrainD;

    private double minZConstrainD;

    private double maxZConstrainD;

    private double lowerThresConstrainD;

    private double upperThresConstrainD;

    /*
     * The points read and the ones filtered from them, as columns to keep also tens of millions of
     * points in memory.
     */
    private LasPointColumns filteredColumns;
    private LasPointColumns lastReadColumns;
    /** The height over the DTM of the last point checked, NaN if not known. */
    private double checkedGroundHeight;

    /**
     * Get the points that survived the last {@link #applyConstraints(ALasReader, boolean, IProgressPrinter)} run.
     *
     * <p>The records are created at every call: with many points use {@link #getFilteredColumns()},
     * which needs less than half of the memory.</p>
     *
     * @return the points, or <code>null</code> if the constraints were never applied.
     */
    public List<LasRecord> getFilteredPoints() {
        LasPointColumns columns = filteredColumns;
        if (columns == null) {
            return null;
        }
        List<LasRecord> points = new ArrayList<>(columns.size());
        for( int i = 0; i < columns.size(); i++ ) {
            points.add(columns.get(i));
        }
        return points;
    }

    /**
     * Get the points that survived the last {@link #applyConstraints(ALasReader, boolean, IProgressPrinter)} run,
     * as columns. The height over the DTM is in {@link LasPointColumns#groundHeight}.
     *
     * @return the points, or <code>null</code> if the constraints were never applied. Not to be modified.
     */
    public LasPointColumns getFilteredColumns() {
        return filteredColumns;
    }

    /**
     * Get the data filtered by the current constraints.
     * 
     * @param lasReader the reader.
     * @param doReread if true, the data are read again, else only the existing are filtered.
     * @param progressPrinter the monitor. This needs to consider that every 1000 a worked(1) is called.
     * @return the list of points to keep.
     * @throws Exception
     */
    public void applyConstraints( ALasReader lasReader, boolean doReread, IProgressPrinter progressPrinter ) throws Exception {
        boolean doSampling = false;
        int samp = -1;
        if (sampling != null) {
            doSampling = true;
            samp = sampling;
        }
        int count = 0;

        minInt = Double.POSITIVE_INFINITY;
        maxInt = Double.NEGATIVE_INFINITY;
        minClass = Double.POSITIVE_INFINITY;
        maxClass = Double.NEGATIVE_INFINITY;
        minImpulse = Double.POSITIVE_INFINITY;
        maxImpulse = Double.NEGATIVE_INFINITY;
        minElevation = Double.POSITIVE_INFINITY;
        maxElevation = Double.NEGATIVE_INFINITY;
        maxGroundHeight = Double.NEGATIVE_INFINITY;
        minGroundHeight = Double.POSITIVE_INFINITY;

        minIntensityConstrainD = minIntensityConstrain != null ? minIntensityConstrain : 0;
        maxIntensityConstrainD = maxIntensityConstrain != null ? maxIntensityConstrain : 0;
        westConstrainD = westConstrain != null ? westConstrain : 0;
        eastConstrainD = eastConstrain != null ? eastConstrain : 0;
        southConstrainD = southConstrain != null ? southConstrain : 0;
        northConstrainD = northConstrain != null ? northConstrain : 0;
        minZConstrainD = minZConstrain != null ? minZConstrain : 0;
        maxZConstrainD = maxZConstrain != null ? maxZConstrain : 0;
        lowerThresConstrainD = lowerThresConstrain != null ? lowerThresConstrain : 0;
        upperThresConstrainD = upperThresConstrain != null ? upperThresConstrain : 0;

        filteredEnvelope = new Envelope();
        if (doReread || lastReadColumns == null) {
            // release the old points before reading the new ones
            lastReadColumns = null;
            filteredColumns = null;
            long recordsCount = lasReader.getHeader().getRecordsCount();
            long expected = doSampling && samp > 0 ? recordsCount / samp + 1 : recordsCount;
            LasPointColumns readColumns = new LasPointColumns((int) Math.min(expected, Integer.MAX_VALUE - 8));
            try {
                int progress = 0;
                while( lasReader.hasNextPoint() ) {
                    if (count % 1000 == 0 && progressPrinter != null) {
                        progressPrinter.publish(new ProgressUpdate("Reading dataset...", progress++));
                    }

                    count++;

                    LasRecord lasDot = lasReader.getNextPoint();
                    boolean takeIt = checkPoint(lasDot.x, lasDot.y, lasDot.z, lasDot.intensity, lasDot.classification,
                            lasDot.returnNumber, doSampling, samp, count);
                    if (takeIt) {
                        lasDot.groundElevation = checkedGroundHeight;
                        readColumns.add(lasDot);
                    }
                }
                readColumns.trimToSize();
                lastReadColumns = readColumns;
                filteredColumns = readColumns;
            } finally {
                try {
                    lasReader.rewind();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            LasPointColumns read = lastReadColumns;
            LasPointColumns newFiltered = new LasPointColumns(read.size());
            for( int i = 0; i < read.size(); i++ ) {
                // do not resample
                boolean takeIt = checkPoint(read.x[i], read.y[i], read.z[i], read.intensity[i], read.classification[i],
                        read.returnNumber[i], false, -1, -1);
                if (takeIt) {
                    newFiltered.add(read, i);
                    newFiltered.groundHeight[newFiltered.size() - 1] = (float) checkedGroundHeight;
                }
            }
            newFiltered.trimToSize();
            filteredColumns = newFiltered;
        }
    }

    /**
     * Check if a point passes the constraints, leaving its height over the DTM in
     * {@link #checkedGroundHeight}, and update the statistics of the filtered points.
     */
    private boolean checkPoint( double x, double y, double z, double intensity, int classification, double impulse,
            boolean doSampling, int samp, int count ) {
        boolean takeIt = true;
        checkedGroundHeight = Double.NaN;

        if (doSampling && (count % samp != 0)) {
            takeIt = false;
        }
        if (takeIt && minIntensityConstrain != null) {
            takeIt = false;
            if (intensity >= minIntensityConstrainD && intensity <= maxIntensityConstrainD) {
                takeIt = true;
            }
        }
        if (takeIt && impulsesConstrains != null) {
            takeIt = false;
            for( final double imp : impulsesConstrains ) {
                if (dEq(impulse, imp)) {
                    takeIt = true;
                    break;
                }
            }
        }
        if (takeIt && classesConstrains != null) {
            takeIt = false;
            for( final double classs : classesConstrains ) {
                if (classification == (int) classs) {
                    takeIt = true;
                    break;
                }
            }
        }
        if (takeIt && (westConstrain != null && eastConstrain != null && southConstrain != null && northConstrain != null)) {
            takeIt = false;
            if (x >= westConstrainD && x <= eastConstrainD && y >= southConstrainD && y <= northConstrainD) {
                takeIt = true;
            }
        }
        if (takeIt && (minZConstrain != null && maxZConstrain != null)) {
            takeIt = false;
            if (z >= minZConstrainD && z <= maxZConstrainD) {
                takeIt = true;
            }
        }

        if (takeIt) {
            if (dtm != null) {
                double value = CoverageUtilities.getValue(dtm, x, y);
                if (!HMConstants.isNovalue(value)) {
                    // negative heights are kept: they show where the points are under the DTM
                    checkedGroundHeight = z - value;
                    takeIt = true;
                } else {
                    takeIt = false;
                }

                if (takeIt && lowerThresConstrain != null) {
                    takeIt = checkedGroundHeight >= lowerThresConstrainD;
                }
                if (takeIt && upperThresConstrain != null) {
                    takeIt = checkedGroundHeight <= upperThresConstrainD;
                }
            }
        }

        if (takeIt) {
            minImpulse = Math.min(minImpulse, impulse);
            maxImpulse = Math.max(maxImpulse, impulse);
            minInt = Math.min(minInt, intensity);
            maxInt = Math.max(maxInt, intensity);
            minClass = Math.min(minClass, classification);
            maxClass = Math.max(maxClass, classification);
            minElevation = Math.min(minElevation, z);
            maxElevation = Math.max(maxElevation, z);
            if (!Double.isNaN(checkedGroundHeight)) {
                minGroundHeight = Math.min(minGroundHeight, checkedGroundHeight);
                maxGroundHeight = Math.max(maxGroundHeight, checkedGroundHeight);
            }

            filteredEnvelope.expandToInclude(x, y);
        }
        return takeIt;
    }

    public ColorInterpolator getElevationColorInterpolator() {
        if (Double.isInfinite(maxGroundHeight)) {
            return new ColorInterpolator(EColorTables.elev.name(), minElevation, maxElevation, null);
        } else {
            return new ColorInterpolator(EColorTables.elev.name(), minGroundHeight, maxGroundHeight, null);
        }
    }

    public ColorInterpolator getIntensityColorInterpolator() {
        return new ColorInterpolator(EColorTables.rainbow.name(), minInt, maxInt, null);
    }

    public ColorInterpolator getImpulseColorInterpolator() {
        return new ColorInterpolator(EColorTables.rainbow.name(), minImpulse, maxImpulse, null);
    }

    public ColorInterpolator getClassificationColorInterpolator() {
        return new ColorInterpolator(EColorTables.rainbow.name(), minClass, maxClass, null);
    }

    public double[] getStats() {
        return new double[]{minElevation, maxElevation, minInt, maxInt, minClass, maxClass, minImpulse, maxImpulse,
                minGroundHeight, maxGroundHeight};
    }

    public Envelope getFilteredEnvelope() {
        return filteredEnvelope;
    }

    public void setSampling( Integer sampling ) {
        this.sampling = sampling;

    }

    public void setClassifications( int[] classes ) {
        this.classesConstrains = classes;

    }

    public void setImpulses( int[] impulses ) {
        this.impulsesConstrains = impulses;

    }

    public void setMaxIntensity( Double maxInt ) {
        this.maxIntensityConstrain = maxInt;

    }
    public void setMinIntensity( Double minInt ) {
        this.minIntensityConstrain = minInt;

    }

    public void setWest( Double west ) {
        this.westConstrain = west;
        checkBounds();
    }

    public void setEast( Double east ) {
        this.eastConstrain = east;
        checkBounds();
    }
    public void setSouth( Double south ) {
        this.southConstrain = south;
        checkBounds();
    }
    public void setNorth( Double north ) {
        this.northConstrain = north;
        checkBounds();
    }
    public void setMinZ( Double minZ ) {
        this.minZConstrain = minZ;
        checkBounds();
    }
    public void setMaxZ( Double maxZ ) {
        this.maxZConstrain = maxZ;
        checkBounds();
    }

    public Double[] checkBounds() {
        if (westConstrain != null && eastConstrain != null) {
            double westConstrainTmp = Math.min(westConstrain, eastConstrain);
            eastConstrain = Math.max(westConstrain, eastConstrain);
            westConstrain = westConstrainTmp;
        }
        if (southConstrain != null && northConstrain != null) {
            double southConstrainTmp = Math.min(southConstrain, northConstrain);
            northConstrain = Math.max(southConstrain, northConstrain);
            southConstrain = southConstrainTmp;
        }
        if (minZConstrain != null && maxZConstrain != null) {
            double minZConstrainTmp = Math.min(minZConstrain, maxZConstrain);
            maxZConstrain = Math.max(minZConstrain, maxZConstrain);
            minZConstrain = minZConstrainTmp;
        }
        return new Double[]{westConstrain, eastConstrain, southConstrain, northConstrain, minZConstrain, maxZConstrain};
    }

    public void setDtm( GridCoverage2D dtm ) {
        this.dtm = dtm;
    }
    
    public boolean hasDtm() {
		return this.dtm != null;
	}

    public void setLowerThres( Double lowerThres ) {
        this.lowerThresConstrain = lowerThres;

    }
    public void setUpperThres( Double upperThres ) {
        this.upperThresConstrain = upperThres;

    }

}
