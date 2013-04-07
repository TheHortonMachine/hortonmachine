package org.jgrasstools.gears.io.las.core;

import java.util.Comparator;

public class LasRecordElevationComparator implements Comparator<LasRecord> {
    @Override
    public int compare( LasRecord o1, LasRecord o2 ) {
        if (o1.z < o2.z) {
            return -1;
        } else if (o1.z > o2.z) {
            return 1;
        } else {
            return 0;
        }
    }
}
