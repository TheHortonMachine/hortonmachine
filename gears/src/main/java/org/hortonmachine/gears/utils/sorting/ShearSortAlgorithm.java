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

public class ShearSortAlgorithm {
    private int Log, Rows, Cols;

    /**
     * Sorts two arrays regarding to the sort of the first
     * 
     * @param arrayToBeSorted
     *            the array on wich the sort is performed
     * @param arrayThatFollowsTheSort
     *            the array that is sorted following the others array sort. Can
     *            be null
     * @throws Exception
     */
    public void parallelSort(double[] arrayToBeSorted,
            double[] arrayThatFollowsTheSort) throws Exception {
        int pow = 1, div = 1;
        int h[];

        for (int i = 1; i * i <= arrayToBeSorted.length; i++)
            if (arrayToBeSorted.length % i == 0)
                div = i;
        Rows = div;
        Cols = arrayToBeSorted.length / div;
        for (Log = 0; pow <= Rows; Log++)
            pow = pow * 2;

        h = new int[Rows];
        for (int i = 0; i < Rows; i++)
            h[i] = i * Cols;

        for (int k = 0; k < Log; k++) {
            for (int j = 0; j < Cols / 2; j++) {
                for (int i = 0; i < Rows; i++)
                    sortPart1(arrayToBeSorted, arrayThatFollowsTheSort, i
                            * Cols, (i + 1) * Cols, 1, (i % 2 == 0 ? true
                            : false));
                apause(h);
                for (int i = 0; i < Rows; i++)
                    sortPart2(arrayToBeSorted, arrayThatFollowsTheSort, i
                            * Cols, (i + 1) * Cols, 1, (i % 2 == 0 ? true
                            : false));
                apause(h);
            }
            for (int j = 0; j < Rows / 2; j++) {
                for (int i = 0; i < Cols; i++)
                    sortPart1(arrayToBeSorted, arrayThatFollowsTheSort, i, Rows
                            * Cols + i, Cols, true);
                apause(h);
                for (int i = 0; i < Cols; i++)
                    sortPart2(arrayToBeSorted, arrayThatFollowsTheSort, i, Rows
                            * Cols + i, Cols, true);
                apause(h);
            }
        }
        for (int j = 0; j < Cols / 2; j++) {
            for (int i = 0; i < Rows; i++)
                sortPart1(arrayToBeSorted, arrayThatFollowsTheSort, i * Cols,
                        (i + 1) * Cols, 1, true);
            apause(h);
            for (int i = 0; i < Rows; i++)
                sortPart2(arrayToBeSorted, arrayThatFollowsTheSort, i * Cols,
                        (i + 1) * Cols, 1, true);
            apause(h);
        }
        for (int i = 0; i < Rows; i++)
            h[i] = -1;
        apause(h);
    }

    private void sortPart1(double[] arrayToSort, double[] arrayToFollowSort,
            int Lo, int Hi, int Nx, boolean Up) throws Exception {
        for (int j = Lo; j + Nx < Hi; j += 2 * Nx)
            if ((Up && arrayToSort[j] > arrayToSort[j + Nx]) || !Up
                    && arrayToSort[j] < arrayToSort[j + Nx]) {
                double T = arrayToSort[j];
                arrayToSort[j] = arrayToSort[j + Nx];
                arrayToSort[j + Nx] = T;

                /*
                 * the compagnon
                 */
                if (arrayToFollowSort != null) {
                    double Tfollow = arrayToFollowSort[j];
                    arrayToFollowSort[j] = arrayToFollowSort[j + Nx];
                    arrayToFollowSort[j + Nx] = Tfollow;
                }
            }
    }

    private void sortPart2(double[] arrayToSort, double[] arrayToFollowSort,
            int Lo, int Hi, int Nx, boolean Up) throws Exception {
        for (int j = Lo + Nx; j + Nx < Hi; j += 2 * Nx)
            if ((Up && arrayToSort[j] > arrayToSort[j + Nx]) || !Up
                    && arrayToSort[j] < arrayToSort[j + Nx]) {
                double T = arrayToSort[j];
                arrayToSort[j] = arrayToSort[j + Nx];
                arrayToSort[j + Nx] = T;
                /*
                 * the compagnon
                 */
                if (arrayToFollowSort != null) {
                    double Tfollow = arrayToFollowSort[j];
                    arrayToFollowSort[j] = arrayToFollowSort[j + Nx];
                    arrayToFollowSort[j + Nx] = Tfollow;
                }
            }
    }

    private void apause(int H[]) {
        try {
            Thread.sleep(40);
        } catch (InterruptedException e) {
        }
    }

    public static void main(String[] args) {
        ShearSortAlgorithm t = new ShearSortAlgorithm();
        double[] tmp1 = new double[] { 2.5, 4.7, 1.3, 10.123, -0.3, -14.0,
                123.6 };
        double[] tmp2 = new double[] { 2.0, 3.0, 1.0, 4.0, -0.3, -1.0, 5.0 };

        try {
            t.parallelSort(tmp1, tmp2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < tmp2.length; i++) {
            System.out.println(tmp1[i] + " " + tmp2[i]);
        }
    }
}