package org.hortonmachine.gears.utils.clustering;

import java.util.Arrays;

public class GvmVectorSpace extends GvmSpace {

    // statics

    private static double[] coords( Object obj ) {
        return (double[]) obj;
    }

    // fields

    private final int dimensions;

    // constructors

    public GvmVectorSpace( int dimensions ) {
        if (dimensions < 1)
            throw new IllegalArgumentException("non-positive dimensions");
        this.dimensions = dimensions;
    }

    // accessors

    public int getDimensions() {
        return dimensions;
    }

    // space factory methods

    @Override
    public double[] newOrigin() {
        return new double[dimensions];
    }

    @Override
    public double[] newCopy( Object pt ) {
        return coords(pt).clone();
    }

    // space point operations

    @Override
    public double magnitudeSqr( Object pt ) {
        double sum = 0.0;
        double[] coords = coords(pt);
        for( int i = 0; i < dimensions; i++ ) {
            double c = coords[i];
            sum += c * c;
        }
        return sum;
    }

    @Override
    public double sum( Object pt ) {
        double sum = 0.0;
        for( double coord : coords(pt) ) {
            sum += coord;
        }
        return sum;
    }

    @Override
    public void setToOrigin( Object pt ) {
        Arrays.fill(coords(pt), 0.0);
    }

    @Override
    public void setTo( Object dstPt, Object srcPt ) {
        System.arraycopy(coords(srcPt), 0, coords(dstPt), 0, dimensions);
    }

    @Override
    public void setToScaled( Object dstPt, double m, Object srcPt ) {
        double[] dstCoords = coords(dstPt);
        double[] srcCoords = coords(srcPt);
        for( int i = 0; i < dimensions; i++ ) {
            dstCoords[i] = m * srcCoords[i];
        }
    }

    @Override
    public void setToScaledSqr( Object dstPt, double m, Object srcPt ) {
        double[] dstCoords = coords(dstPt);
        double[] srcCoords = coords(srcPt);
        for( int i = 0; i < dimensions; i++ ) {
            double c = srcCoords[i];
            dstCoords[i] = m * c * c;
        }
    }

    @Override
    public void add( Object dstPt, Object srcPt ) {
        double[] dstCoords = coords(dstPt);
        double[] srcCoords = coords(srcPt);
        for( int i = 0; i < dimensions; i++ ) {
            dstCoords[i] += srcCoords[i];
        }
    }

    @Override
    public void addScaled( Object dstPt, double m, Object srcPt ) {
        double[] dstCoords = coords(dstPt);
        double[] srcCoords = coords(srcPt);
        for( int i = 0; i < dimensions; i++ ) {
            dstCoords[i] += m * srcCoords[i];
        }
    }

    @Override
    public void addScaledSqr( Object dstPt, double m, Object srcPt ) {
        double[] dstCoords = coords(dstPt);
        double[] srcCoords = coords(srcPt);
        for( int i = 0; i < dimensions; i++ ) {
            double c = srcCoords[i];
            dstCoords[i] += m * c * c;
        }
    }

    @Override
    public void subtract( Object dstPt, Object srcPt ) {
        double[] dstCoords = coords(dstPt);
        double[] srcCoords = coords(srcPt);
        for( int i = 0; i < dimensions; i++ ) {
            dstCoords[i] -= srcCoords[i];
        }
    }

    @Override
    public void subtractScaled( Object dstPt, double m, Object srcPt ) {
        double[] dstCoords = coords(dstPt);
        double[] srcCoords = coords(srcPt);
        for( int i = 0; i < dimensions; i++ ) {
            dstCoords[i] -= m * srcCoords[i];
        }
    }

    @Override
    public void subtractScaledSqr( Object dstPt, double m, Object srcPt ) {
        double[] dstCoords = coords(dstPt);
        double[] srcCoords = coords(srcPt);
        for( int i = 0; i < dimensions; i++ ) {
            double c = srcCoords[i];
            dstCoords[i] -= m * c * c;
        }
    }

    @Override
    public void scale( Object pt, double m ) {
        double[] coords = coords(pt);
        for( int i = 0; i < dimensions; i++ ) {
            coords[i] *= m;
        }
    }

    @Override
    public void square( Object pt ) {
        double[] coords = coords(pt);
        for( int i = 0; i < dimensions; i++ ) {
            coords[i] *= coords[i];
        }
    }

    // optimizations

    @Override
    public double distance( Object pt1, Object pt2 ) {
        double[] coords1 = coords(pt1);
        double[] coords2 = coords(pt2);
        double sum = 0.0;
        for( int i = 0; i < dimensions; i++ ) {
            double d = coords1[i] - coords2[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    @Override
    public double variance( double m, Object pt, Object ptSqr ) {
        double[] coords = coords(pt);
        double[] sqrCoords = coords(ptSqr);
        double sum = 0.0;
        for( int i = 0; i < dimensions; i++ ) {
            double c = coords[i];
            sum += sqrCoords[i] - c * c / m;
        }
        return sum;
    }

    @Override
    public double variance( double m1, Object pt1, Object ptSqr1, double m2, Object pt2 ) {
        double[] coords1 = coords(pt1);
        double[] sqrCoords1 = coords(ptSqr1);
        double[] coords2 = coords(pt2);

        double m0 = m1 + m2;
        double sum = 0.0;
        for( int i = 0; i < dimensions; i++ ) {
            double c2 = coords2[i];
            double c = coords1[i] + m2 * c2;
            double cSqr = sqrCoords1[i] + m2 * c2 * c2;
            sum += cSqr - c * c / m0;
        }
        return sum;
    }

    @Override
    public double variance( double m1, Object pt1, Object ptSqr1, double m2, Object pt2, Object ptSqr2 ) {
        double[] coords1 = coords(pt1);
        double[] sqrCoords1 = coords(ptSqr1);
        double[] coords2 = coords(pt2);
        double[] sqrCoords2 = coords(ptSqr2);

        double m0 = m1 + m2;
        double sum = 0.0;
        for( int i = 0; i < dimensions; i++ ) {
            double c = coords1[i] + coords2[i];
            double cSqr = sqrCoords1[i] + sqrCoords2[i];
            sum += cSqr - c * c / m0;
        }

        return sum;
    }

    @Override
    public String toString( Object pt ) {
        return Arrays.toString(coords(pt));
    }

}
