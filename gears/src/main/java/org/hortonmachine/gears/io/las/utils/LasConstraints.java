package org.hortonmachine.gears.io.las.utils;

import static org.hortonmachine.gears.utils.math.NumericsUtilities.dEq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
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

    private List<LasRecord> filteredPoints;

    private double minInt = Double.POSITIVE_INFINITY;
    private double maxInt = Double.NEGATIVE_INFINITY;
    private double minClass = Double.POSITIVE_INFINITY;
    private double maxClass = Double.NEGATIVE_INFINITY;
    private double minImpulse = Double.POSITIVE_INFINITY;
    private double maxImpulse = Double.NEGATIVE_INFINITY;
    private double minElevation = Double.POSITIVE_INFINITY;
    private double maxElevation = Double.NEGATIVE_INFINITY;

    private Envelope filteredEnvelope;

    private GridCoverage2D dtm;

    private boolean isDirty = false;

    /**
     * @return the points that survived the last {@link #applyConstraints(ALasReader)} run.
     */
    public List<LasRecord> getFilteredPoints() {
        return filteredPoints;
    }

    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Get the data filtered by the current constraints.
     * 
     * @param lasReader the reader.
     * @param monitor the monitor. This needs to consider that every 1000 a worked(1) is called.
     * @return the list of points to keep.
     * @throws Exception
     */
    public void applyConstraints( ALasReader lasReader, IHMProgressMonitor monitor ) throws Exception {
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

        double minIntensityConstrainD = minIntensityConstrain != null ? minIntensityConstrain : 0;
        double maxIntensityConstrainD = maxIntensityConstrain != null ? maxIntensityConstrain : 0;
        double westConstrainD = westConstrain != null ? westConstrain : 0;
        double eastConstrainD = eastConstrain != null ? eastConstrain : 0;
        double southConstrainD = southConstrain != null ? southConstrain : 0;
        double northConstrainD = northConstrain != null ? northConstrain : 0;
        double minZConstrainD = minZConstrain != null ? minZConstrain : 0;
        double maxZConstrainD = maxZConstrain != null ? maxZConstrain : 0;
        double lowerThresConstrainD = lowerThresConstrain != null ? lowerThresConstrain : 0;
        double upperThresConstrainD = upperThresConstrain != null ? upperThresConstrain : 0;

        filteredEnvelope = new Envelope();
        filteredPoints = new ArrayList<>(1000000);
        try {
            while( lasReader.hasNextPoint() ) {
                LasRecord lasDot = lasReader.getNextPoint();

                if (count % 1000 == 0)
                    monitor.worked(1);

                count++;
                final double x = lasDot.x;
                final double y = lasDot.y;
                double z = lasDot.z;
                final double intensity = lasDot.intensity;
                final int classification = lasDot.classification;
                final double impulse = lasDot.returnNumber;

                boolean takeIt = true;
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
                if (takeIt
                        && (westConstrain != null && eastConstrain != null && southConstrain != null && northConstrain != null)) {
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
                            z = z - value;
                            if (z < 0)
                                z = 0;
                            lasDot.z = z;
                        } else {
                            continue;
                        }

                        if (lowerThresConstrain != null) {
                            if (z < lowerThresConstrainD) {
                                continue;
                            }
                        }
                        if (upperThresConstrain != null) {
                            if (z > upperThresConstrainD) {
                                continue;
                            }
                        }
                    }

                    minImpulse = Math.min(minImpulse, impulse);
                    maxImpulse = Math.max(maxImpulse, impulse);
                    minInt = Math.min(minInt, intensity);
                    maxInt = Math.max(maxInt, intensity);
                    minClass = Math.min(minClass, classification);
                    maxClass = Math.max(maxClass, classification);
                    minElevation = Math.min(minElevation, z);
                    maxElevation = Math.max(maxElevation, z);

                    filteredEnvelope.expandToInclude(x, y);

                    filteredPoints.add(lasDot);
                }
            }
            isDirty = false;
        } finally {
            try {
                lasReader.rewind();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ColorInterpolator getElevationColorInterpolator() {
        return new ColorInterpolator(EColorTables.elev.name(), minElevation, maxElevation, null);
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
        return new double[]{minElevation, maxElevation, minInt, maxInt, minClass, maxClass, minImpulse, maxImpulse};
    }

    public Envelope getFilteredEnvelope() {
        return filteredEnvelope;
    }

    public void setSampling( Integer sampling ) {
        this.sampling = sampling;
        isDirty = true;
    }

    public void setClassifications( int[] classes ) {
        this.classesConstrains = classes;
        isDirty = true;
    }

    public void setImpulses( int[] impulses ) {
        this.impulsesConstrains = impulses;
        isDirty = true;
    }

    public void setMaxIntensity( Double maxInt ) {
        this.maxIntensityConstrain = maxInt;
        isDirty = true;
    }
    public void setMinIntensity( Double minInt ) {
        this.minIntensityConstrain = minInt;
        isDirty = true;
    }

    public void setWest( Double west ) {
        this.westConstrain = west;
        isDirty = true;
    }
    public void setEast( Double east ) {
        this.eastConstrain = east;
        isDirty = true;
    }
    public void setSouth( Double south ) {
        this.southConstrain = south;
        isDirty = true;
    }
    public void setNorth( Double north ) {
        this.northConstrain = north;
        isDirty = true;
    }
    public void setMinZ( Double minZ ) {
        this.minZConstrain = minZ;
        isDirty = true;
    }
    public void setMaxZ( Double maxZ ) {
        this.maxZConstrain = maxZ;
        isDirty = true;
    }

    public void setDtm( GridCoverage2D dtm ) {
        this.dtm = dtm;
        isDirty = true;
    }

    public void setLowerThres( Double lowerThres ) {
        this.lowerThresConstrain = lowerThres;
        isDirty = true;
    }
    public void setUpperThres( Double upperThres ) {
        this.upperThresConstrain = upperThres;
        isDirty = true;
    }

}
