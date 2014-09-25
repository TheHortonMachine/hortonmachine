package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment;

import java.util.Comparator;

import org.opengis.feature.simple.SimpleFeature;

public class NetIndexComparator implements Comparator<SimpleFeature>, LWFields{

    //establish the position of each net point respect to the others
    
    @Override
    public int compare( SimpleFeature f1, SimpleFeature f2 ) {
        int linkid1 = (Integer) f1.getAttribute(LINKID);
        int linkid2 = (Integer) f2.getAttribute(LINKID);

        if (linkid1 < linkid2) {
            return -1;
        } else if (linkid1 > linkid2) {
            return 1;
        } else {
            return 0;
        }
    }

}
