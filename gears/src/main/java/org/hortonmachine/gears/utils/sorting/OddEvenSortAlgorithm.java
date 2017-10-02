/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.gears.utils.sorting;

import java.util.ArrayList;
import java.util.List;

public class OddEvenSortAlgorithm {

    /**
     * Sorts two arrays regarding to the sort of the first
     * 
     * @param arrayToBeSorted the array on wich the sort is performed
     * @param arrayThatFollowsTheSort the array that is sorted following the others array sort. Can
     *        be null
     */
    public static void oddEvenSort( double arrayToBeSorted[], double[] arrayThatFollowsTheSort ) {
        for( int i = 0; i < arrayToBeSorted.length / 2; i++ ) {
            for( int j = 0; j + 1 < arrayToBeSorted.length; j += 2 )
                if (arrayToBeSorted[j] > arrayToBeSorted[j + 1]) {
                    double tmpa = arrayToBeSorted[j];
                    arrayToBeSorted[j] = arrayToBeSorted[j + 1];
                    arrayToBeSorted[j + 1] = tmpa;
                    if (arrayThatFollowsTheSort != null) {
                        double tmpb = arrayThatFollowsTheSort[j];
                        arrayThatFollowsTheSort[j] = arrayThatFollowsTheSort[j + 1];
                        arrayThatFollowsTheSort[j + 1] = tmpb;
                    }
                }
            for( int j = 1; j + 1 < arrayToBeSorted.length; j += 2 )
                if (arrayToBeSorted[j] > arrayToBeSorted[j + 1]) {
                    double tmpa = arrayToBeSorted[j];
                    arrayToBeSorted[j] = arrayToBeSorted[j + 1];
                    arrayToBeSorted[j + 1] = tmpa;
                    if (arrayThatFollowsTheSort != null) {
                        double tmpb = arrayThatFollowsTheSort[j];
                        arrayThatFollowsTheSort[j] = arrayThatFollowsTheSort[j + 1];
                        arrayThatFollowsTheSort[j + 1] = tmpb;
                    }
                }
        }
    }

    /**
     * Sorts two lists regarding to the sort of the first
     * 
     * @param listToBeSorted the array on wich the sort is performed
     * @param listThatFollowsTheSort the array that is sorted following the others list sort. Can be
     *        null, in which case it acts like a normal sorting algorithm
     */
    public static void oddEvenSort( List<Double> listToBeSorted, List<Double> listThatFollowsTheSort ) {
        for( int i = 0; i < listToBeSorted.size() / 2; i++ ) {
            for( int j = 0; j + 1 < listToBeSorted.size(); j += 2 )
                if (listToBeSorted.get(j) > listToBeSorted.get(j + 1)) {
                    double tmpa = listToBeSorted.get(j);
                    listToBeSorted.set(j, listToBeSorted.get(j + 1));
                    listToBeSorted.set(j + 1, tmpa);
                    if (listThatFollowsTheSort != null) {
                        double tmpb = listThatFollowsTheSort.get(j);
                        listThatFollowsTheSort.set(j, listThatFollowsTheSort.get(j + 1));
                        listThatFollowsTheSort.set(j + 1, tmpb);
                    }
                }
            for( int j = 1; j + 1 < listToBeSorted.size(); j += 2 )
                if (listToBeSorted.get(j) > listToBeSorted.get(j + 1)) {
                    double tmpa = listToBeSorted.get(j);
                    listToBeSorted.set(j, listToBeSorted.get(j + 1));
                    listToBeSorted.set(j + 1, tmpa);
                    if (listThatFollowsTheSort != null) {
                        double tmpb = listThatFollowsTheSort.get(j);
                        listThatFollowsTheSort.set(j, listThatFollowsTheSort.get(j + 1));
                        listThatFollowsTheSort.set(j + 1, tmpb);
                    }
                }
        }
    }

    // public static void main( String[] args ) {
    // OddEvenSortAlgorithm sort = new OddEvenSortAlgorithm();
    //
    // double[] tmp1 = {517.0, 515.0, 518.0, 509.0, 522.0, 505.0, 506.0, 507.0, 508.0, 510.0,
    // 511.0, 512.0, 513.0, 514.0, 516.0, 519.0, 520.0, 521.0, 523.0, 524.0, 525.0};
    //
    // double[] tmp2 = {175.4196, 175.0147, 175.6273, 174.246, 176.4338, 173.6218, 173.8197,
    // 174.0276, 174.2274, 174.2604, 174.3958, 174.412, 174.6161, 174.8132, 175.2264,
    // 175.8193, 176.0247, 176.222, 176.6479, 176.8464, 177.0386};
    //
    // try {
    // sort.oddEvenSort(tmp2, tmp1);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    //
    // for( int i = 0; i < tmp2.length; i++ ) {
    // System.out.println(tmp2[i] + " " + tmp1[i]); //$NON-NLS-1$
    // }
    // }

    public static void main( String[] args ) {

        List<Double> a = new ArrayList<Double>();
        List<Double> b = new ArrayList<Double>();
        a.add(517.0);
        a.add(515.0);
        a.add(518.0);
        a.add(509.0);
        a.add(522.0);
        a.add(505.0);
        a.add(506.0);
        a.add(507.0);
        a.add(508.0);
        a.add(510.0);
        a.add(511.0);
        a.add(512.0);
        a.add(513.0);
        a.add(514.0);
        a.add(516.0);
        a.add(519.0);
        a.add(520.0);
        a.add(521.0);
        a.add(523.0);
        a.add(524.0);
        a.add(525.0);

        b.add(175.4196);
        b.add(175.0147);
        b.add(175.6273);
        b.add(174.246);
        b.add(176.4338);
        b.add(173.6218);
        b.add(173.8197);
        b.add(174.0276);
        b.add(174.2274);
        b.add(174.2604);
        b.add(174.3958);
        b.add(174.412);
        b.add(174.6161);
        b.add(174.8132);
        b.add(175.2264);
        b.add(175.8193);
        b.add(176.0247);
        b.add(176.222);
        b.add(176.6479);
        b.add(176.8464);
        b.add(177.0386);

        try {
            oddEvenSort(b, a);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for( int i = 0; i < a.size(); i++ ) {
            System.out.println(b.get(i) + " " + a.get(i)); //$NON-NLS-1$
        }
    }
}
