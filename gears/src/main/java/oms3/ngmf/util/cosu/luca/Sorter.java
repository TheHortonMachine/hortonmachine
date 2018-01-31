/*
 * Sorter.java
 *
 * Created on November 29, 2004, 4:45 PM
 */
package oms3.ngmf.util.cosu.luca;

import java.util.Comparator;

class Sorter implements Comparator<Integer> {

    public static final int ASCENDING = 1;
    public static final int DESCENDING = 2;
    private double[] dataArray;
    private int sortType;

    /* An object such as Vector will be sorted depending on the entries in
     *  dataArray using this Sorter. Select Sorter.ASCENDING or Sorter.DESCENDING
     *  for sortType. */
    Sorter(double[] dataArray, int ASCENDING_or_DESCENDING) {
        this.dataArray = dataArray;
        this.sortType = ASCENDING_or_DESCENDING;
    }

    /** For ascending,
     *  return 1 if dataArray[o1] is greater than dataArray[o2]
     *  return 0 if dataArray[o1] is equal to dataArray[o2]
     *  return -1 if dataArray[o1] is less than dataArray[o2]
     *
     *  For decending, do it in the opposize way.
     */
    @Override
    public int compare(Integer o1, Integer o2) {
        double diff = dataArray[o2] - dataArray[o1];
        if (diff == 0) {
            return 0;
        }
        if (sortType == ASCENDING) {
            return (diff > 0) ? -1 : 1;
        } else {
            return (diff > 0) ? 1 : -1;
        }
    }
}
