package org.jgrasstools.gears.modules.v.smoothing;

import org.opengis.feature.simple.SimpleFeature;

public class FeatureElevationComparer implements Comparable<FeatureElevationComparer> {

    private final SimpleFeature feature;
    private double elevation;

    public FeatureElevationComparer( SimpleFeature feature, String field ) {
        this.feature = feature;

        elevation = ((Number) feature.getAttribute(field)).doubleValue();
    }

    public SimpleFeature getFeature() {
        return feature;
    }

    public double getElevation() {
        return elevation;
    }

    public int compareTo( FeatureElevationComparer o ) {
        if (elevation < o.getElevation()) {
            return -1;
        } else if (elevation > o.getElevation()) {
            return 1;
        } else
            return 0;
    }

}
