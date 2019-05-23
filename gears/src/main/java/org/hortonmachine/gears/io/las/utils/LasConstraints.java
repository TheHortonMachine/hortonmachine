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

    private List<LasRecord> filteredPoints;
    private List<LasRecord> lastReadPoints;

    /**
     * @return the points that survived the last {@link #applyConstraints(ALasReader)} run.
     */
    public List<LasRecord> getFilteredPoints() {
        return filteredPoints;
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
        if (doReread || lastReadPoints == null) {
            lastReadPoints = new ArrayList<>(1000000);
            try {
                int progress = 0;
                while( lasReader.hasNextPoint() ) {
                    if (count % 1000 == 0 && progressPrinter != null) {
                        progressPrinter.publish(new ProgressUpdate("Reading dataset...", progress++));
                    }

                    count++;

                    LasRecord lasDot = lasReader.getNextPoint();
                    boolean takeIt = checkPoint(lasDot, doSampling, samp, count);
                    if (takeIt) {
                        lastReadPoints.add(lasDot);
                    }
                }
                filteredPoints = lastReadPoints;
                System.out.println(filteredPoints.size());
            } finally {
                try {
                    lasReader.rewind();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            List<LasRecord> newFilteredPoints = new ArrayList<>(1000000);
            lastReadPoints.forEach(lr -> {
                // do not resample
                boolean takeIt = checkPoint(lr, false, -1, -1);
                if (takeIt) {
                    newFilteredPoints.add(lr);
                }
            });
            filteredPoints = newFilteredPoints;
            System.out.println(filteredPoints.size());
        }
    }

    private boolean checkPoint( LasRecord lasDot, boolean doSampling, int samp, int count ) {
        boolean takeIt = true;
        final double x = lasDot.x;
        final double y = lasDot.y;
        double z = lasDot.z;
        final double intensity = lasDot.intensity;
        final int classification = lasDot.classification;
        final double impulse = lasDot.returnNumber;

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
                double value = CoverageUtilities.getValue(dtm, lasDot.x, lasDot.y);
                if (!HMConstants.isNovalue(value)) {
                    double groundHeight = z - value;
                    if (groundHeight < 0)
                        groundHeight = 0;
                    lasDot.groundElevation = groundHeight;
                    takeIt = true;
                } else {
                    takeIt = false;
                }

                if (lowerThresConstrain != null) {
                    takeIt = lasDot.groundElevation >= lowerThresConstrainD;
                }
                if (upperThresConstrain != null) {
                    takeIt = lasDot.groundElevation <= upperThresConstrainD;
                }
            } else {
                lasDot.groundElevation = Double.NaN;
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
            if (!Double.isNaN(lasDot.groundElevation)) {
                minGroundHeight = Math.min(minGroundHeight, lasDot.groundElevation);
                maxGroundHeight = Math.max(maxGroundHeight, lasDot.groundElevation);
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

    public void setLowerThres( Double lowerThres ) {
        this.lowerThresConstrain = lowerThres;

    }
    public void setUpperThres( Double upperThres ) {
        this.upperThresConstrain = upperThres;

    }

}
