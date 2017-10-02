package org.hortonmachine.gears.io.grasslegacy;

import java.awt.image.WritableRaster;

/**
 * This iterator is part of the large GRASS raster support.
 * 
 * <p>For better understanding of resaons for this hack, please see
 * the {@link GrassLegacyGridCoverage2D} documentation.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @see GrassLegacyGridCoverage2D
 */
public class GrassLegacyWritableRaster extends WritableRaster {

    private final double[][] data;

    public GrassLegacyWritableRaster( double[][] data ) {
        super(null, null);
        this.data = data;
    }

    public double[][] getData() {
        return data;
    }

    public int[] getPixel( int x, int y, int[] iArray ) {
        if (iArray == null) {
            iArray = new int[0];
        }
        iArray[0] = (int) Math.round(data[y][x]);
        return iArray;
    }

    public float[] getPixel( int x, int y, float[] fArray ) {
        if (fArray == null) {
            fArray = new float[0];
        }
        fArray[0] = (float) data[y][x];
        return fArray;
    }

    public double[] getPixel( int x, int y, double[] dArray ) {
        if (dArray == null) {
            dArray = new double[0];
        }
        dArray[0] = data[y][x];
        return dArray;
    }

    public int getSample( int x, int y, int b ) {
        return (int) Math.round(data[y][x]);
    }

    public double getSampleDouble( int x, int y, int b ) {
        return data[y][x];
    }

    public float getSampleFloat( int x, int y, int b ) {
        return (float) data[y][x];
    }

    public void setPixel( int x, int y, int[] iArray ) {
        data[y][x] = iArray[0];
    }

    public void setPixel( int x, int y, float[] fArray ) {
        data[y][x] = fArray[0];
    }

    public void setPixel( int x, int y, double[] dArray ) {
        data[y][x] = dArray[0];
    }

    public void setSample( int x, int y, int b, int s ) {
        data[y][x] = s;
    }

    public void setSample( int x, int y, int b, float s ) {
        data[y][x] = s;
    }

    public void setSample( int x, int y, int b, double s ) {
        data[y][x] = s;
    }

}