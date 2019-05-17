package org.hortonmachine.gears.io.las.utils;

import static org.hortonmachine.gears.utils.math.NumericsUtilities.dEq;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.colors.ColorInterpolator;
import org.hortonmachine.gears.utils.colors.EColorTables;
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

        filteredEnvelope = new Envelope();
        filteredPoints = new ArrayList<>(1000000);
        while( lasReader.hasNextPoint() ) {
            LasRecord lasDot = lasReader.getNextPoint();

            if (count % 1000 == 0)
                monitor.worked(1);

            count++;
            final double x = lasDot.x;
            final double y = lasDot.y;
            final double z = lasDot.z;
            final double intensity = lasDot.intensity;
            final int classification = lasDot.classification;
            final double impulse = lasDot.returnNumber;

            boolean takeIt = true;
            if (doSampling && (count % samp != 0)) {
                takeIt = false;
            }
            if (takeIt && minIntensityConstrain != null) {
                takeIt = false;
                if (intensity >= minIntensityConstrain && intensity <= maxIntensityConstrain) {
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
                if (x >= westConstrain && x <= eastConstrain && y >= southConstrain && y <= northConstrain) {
                    takeIt = true;
                }
            }
            if (takeIt && (minZConstrain != null && maxZConstrain != null)) {
                takeIt = false;
                if (z >= minZConstrain && z <= maxZConstrain) {
                    takeIt = true;
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

                filteredEnvelope.expandToInclude(x, y);

                filteredPoints.add(lasDot);
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
    }
    public void setEast( Double east ) {
        this.eastConstrain = east;
    }
    public void setSouth( Double south ) {
        this.southConstrain = south;
    }
    public void setNorth( Double north ) {
        this.northConstrain = north;
    }
    public void setMinZ( Double minZ ) {
        this.minZConstrain = minZ;
    }
    public void setMaxZ( Double maxZ ) {
        this.maxZConstrain = maxZ;
    }

}
