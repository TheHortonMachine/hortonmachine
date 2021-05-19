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

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
public class QuickSortAlgorithm {
    private double[] valuesToSortDouble;
    private float[] valuesToSortFloat;
    private int number;
    private double[] valuesToFollowDouble;
    private float[] valuesToFollowFloat;
    private int[] valuesToFollowInt;
    private IHMProgressMonitor monitor;

    public QuickSortAlgorithm( IHMProgressMonitor monitor ) {
        if (monitor != null)
            this.monitor = monitor;
    }

    /**
     * Sorts an array of double values and moves with the sort a second array.
     * 
     * @param values the array to sort.
     * @param valuesToFollow the array that should be sorted following the 
     *              indexes of the first array. Can be null.
     */
    public void sort( double[] values, double[] valuesToFollow ) {
        this.valuesToSortDouble = values;
        this.valuesToFollowDouble = valuesToFollow;

        number = values.length;

        if (monitor != null) {
            monitor.beginTask("Sorting...", -1);
            monitor.worked(1);
        }
        quicksort(0, number - 1);

        if (monitor != null)
            monitor.done();
    }

    /**
     * Sorts an array of float values and moves with the sort a second array.
     * 
     * @param values the array to sort.
     * @param valuesToFollow the array that should be sorted following the 
     *              indexes of the first array. Can be null.
     */
    public void sort( float[] values, float[] valuesToFollow ) {
        this.valuesToSortFloat = values;
        this.valuesToFollowFloat = valuesToFollow;

        number = values.length;

        if (monitor != null) {
            monitor.beginTask("Sorting...", -1);
            monitor.worked(1);
        }
        quicksortFloat(0, number - 1);
        if (monitor != null)
            monitor.done();
    }

    public void sort( double[] values, int[] valuesToFollow ) {
        this.valuesToSortDouble = values;
        this.valuesToFollowInt = valuesToFollow;

        number = values.length;

        if (monitor != null) {
            monitor.beginTask("Sorting...", -1);
            monitor.worked(1);
        }
        quicksortInt(0, number - 1);
        if (monitor != null)
            monitor.done();
    }

    private void quicksort( int low, int high ) {
        int i = low, j = high;
        // Get the pivot element from the middle of the list
        double pivot = valuesToSortDouble[(low + high) >>> 1];

        // Divide into two lists
        while( i <= j ) {
            // If the current value from the left list is smaller then the pivot
            // element then get the next element from the left list
            while( valuesToSortDouble[i] < pivot || (isNovalue(valuesToSortDouble[i]) && !isNovalue(pivot)) ) {

                i++;
            }
            // If the current value from the right list is larger then the pivot
            // element then get the next element from the right list
            while( valuesToSortDouble[j] > pivot || (!isNovalue(valuesToSortDouble[j]) && isNovalue(pivot)) ) {
                j--;
            }

            // If we have found a values in the left list which is larger then
            // the pivot element and if we have found a value in the right list
            // which is smaller then the pivot element then we exchange the
            // values.
            // As we are done we can increase i and j
            if (i <= j) {
                exchange(i, j);
                i++;
                j--;
            }
        }
        // Recursion
        if (low < j)
            quicksort(low, j);
        if (i < high)
            quicksort(i, high);
    }

    private void quicksortFloat( int low, int high ) {
        int i = low, j = high;
        // Get the pivot element from the middle of the list
        double pivot = valuesToSortFloat[(low + high) >>> 1];

        // Divide into two lists
        while( i <= j ) {
            // If the current value from the left list is smaller then the pivot
            // element then get the next element from the left list
            while( valuesToSortFloat[i] < pivot || (isNovalue(valuesToSortFloat[i]) && !isNovalue(pivot)) ) {

                i++;
            }
            // If the current value from the right list is larger then the pivot
            // element then get the next element from the right list
            while( valuesToSortFloat[j] > pivot || (!isNovalue(valuesToSortFloat[j]) && isNovalue(pivot)) ) {
                j--;
            }

            // If we have found a values in the left list which is larger then
            // the pivot element and if we have found a value in the right list
            // which is smaller then the pivot element then we exchange the
            // values.
            // As we are done we can increase i and j
            if (i <= j) {
                exchangeFloat(i, j);
                i++;
                j--;
            }
        }
        // Recursion
        if (low < j)
            quicksortFloat(low, j);
        if (i < high)
            quicksortFloat(i, high);
    }

    private void quicksortInt( int low, int high ) {
        int i = low, j = high;
        // Get the pivot element from the middle of the list
        double pivot = valuesToSortDouble[(low + high) >>> 1];

        // Divide into two lists
        while( i <= j ) {
            // If the current value from the left list is smaller then the pivot
            // element then get the next element from the left list
            while( valuesToSortDouble[i] < pivot || (isNovalue(valuesToSortDouble[i]) && !isNovalue(pivot)) ) {

                i++;
            }
            // If the current value from the right list is larger then the pivot
            // element then get the next element from the right list
            while( valuesToSortDouble[j] > pivot || (!isNovalue(valuesToSortDouble[j]) && isNovalue(pivot)) ) {
                j--;
            }

            // If we have found a values in the left list which is larger then
            // the pivot element and if we have found a value in the right list
            // which is smaller then the pivot element then we exchange the
            // values.
            // As we are done we can increase i and j
            if (i <= j) {
                exchangeDoubleInt(i, j);
                i++;
                j--;
            }
        }
        // Recursion
        if (low < j)
            quicksortInt(low, j);
        if (i < high)
            quicksortInt(i, high);
    }

    private void exchange( int i, int j ) {
        double temp = valuesToSortDouble[i];
        valuesToSortDouble[i] = valuesToSortDouble[j];
        valuesToSortDouble[j] = temp;
        if (valuesToFollowDouble != null) {
            double tempFollow = valuesToFollowDouble[i];
            valuesToFollowDouble[i] = valuesToFollowDouble[j];
            valuesToFollowDouble[j] = tempFollow;
        }
    }

    private void exchangeFloat( int i, int j ) {
        float temp = valuesToSortFloat[i];
        valuesToSortFloat[i] = valuesToSortFloat[j];
        valuesToSortFloat[j] = temp;
        if (valuesToFollowFloat != null) {
            float tempFollow = valuesToFollowFloat[i];
            valuesToFollowFloat[i] = valuesToFollowFloat[j];
            valuesToFollowFloat[j] = tempFollow;
        }
    }

    private void exchangeDoubleInt( int i, int j ) {
        double temp = valuesToSortDouble[i];
        valuesToSortDouble[i] = valuesToSortDouble[j];
        valuesToSortDouble[j] = temp;
        if (valuesToFollowInt != null) {
            int tempFollow = valuesToFollowInt[i];
            valuesToFollowInt[i] = valuesToFollowInt[j];
            valuesToFollowInt[j] = tempFollow;
        }
    }

}
