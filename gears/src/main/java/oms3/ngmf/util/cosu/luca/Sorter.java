/*
 * $Id: Sorter.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
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
