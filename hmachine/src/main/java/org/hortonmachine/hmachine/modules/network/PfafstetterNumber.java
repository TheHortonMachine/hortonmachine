/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.hmachine.modules.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.hortonmachine.gears.utils.StringUtilities;

/**
 * The object for OmsPfafstetter numbers, supplying several methods to compare and analyse a
 * hierarchial network numbered following a OmsPfafstetter modified network.
 *
 * @author Andrea Antonello - www.hydrologis.com
 */
public class PfafstetterNumber implements Comparable<PfafstetterNumber> {

    final private String pfafstetterNumberString;
    private String pfafstetterUpToLastLevel = null;
    private int order = -1;
    private List<Integer> ordersList = null;

    public PfafstetterNumber(String pfafstetterNumberString) {
        this.pfafstetterNumberString = pfafstetterNumberString;

        ordersList = new ArrayList<Integer>();
        int lastDot = pfafstetterNumberString.lastIndexOf('.');
        if (lastDot == -1) {
            this.order = 1;
            ordersList.add(Integer.parseInt(pfafstetterNumberString));
            pfafstetterUpToLastLevel = ""; //$NON-NLS-1$
        } else {
            String[] order = StringUtilities.REGEX_PATTER_DOT.split(pfafstetterNumberString);
            this.order = order.length;
            for (String string : order) {
                ordersList.add(Integer.parseInt(string));
            }
            pfafstetterUpToLastLevel = pfafstetterNumberString.substring(0, lastDot + 1);
        }

    }

    /**
     * @return the hierarchic order of the channel defined by this pfafstetter number
     */
    public int getOrder() {
        return order;
    }

    /**
     * @return the list of all the numbers composing this pfafstetter number
     */
    public List<Integer> getOrdersList() {
        return ordersList;
    }

    /**
     * The pfafstetter string without the last level, useful for comparison. The dot is added at the
     * end in order have defined levels. *
     *
     * @return the pfafstetter string without the last level.
     */
    public String toStringUpToLastLevel() {
        return pfafstetterUpToLastLevel;
    }

    /**
     * Check if the number is of a certain order or minor to that order
     *
     * @return <code>true</code> if the supplied order is of same or minor order of the current.
     */
    public boolean isOfOrderOrMinor(int order) {
        return this.order >= order;
    }

    /**
     * Checks if the actual pfafstetter object is downstream or not of the passed argument
     *
     * @param pfafstetterNumber the pfafstetterNumber to check against.
     * @return true if the actual obj is downstream of the passed one.
     */
    public boolean isDownStreamOf(PfafstetterNumber pfafstetterNumber) {
        /*
         * all the upstreams will have same numbers until the last dot
         */
        int lastDot = pfafstetterNumberString.lastIndexOf('.');
        String pre = pfafstetterNumberString.substring(0, lastDot + 1);
        String lastNum = pfafstetterNumberString.substring(lastDot + 1, pfafstetterNumberString
                .length());
        int lastNumInt = Integer.parseInt(lastNum);

        if (lastNumInt % 2 == 0) {
            // it has to be the last piece of a river, therefore no piece contained
            return false;
        } else {
            /*
             * check if the passed one is upstream
             */
            String pfaff = pfafstetterNumber.toString();
            if (pfaff.startsWith(pre)) {
                // search for all those with a higher next number
                String lastPart = pfaff.substring(lastDot + 1, pfaff.length());
                String lastPartParent = StringUtilities.REGEX_PATTER_DOT.split(lastPart)[0];
                if (Integer.parseInt(lastPartParent) >= lastNumInt) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return true if the queried piece is end piece of a reach, i.e. pfafstetter is even
     */
    public boolean isEndPiece() {
        return ordersList.get(ordersList.size() - 1) % 2 == 0;
    }

    @Override
    public int compareTo(PfafstetterNumber o) {
        List<Integer> p1OrdersList = getOrdersList();
        List<Integer> p2OrdersList = o.getOrdersList();

        int levels = p1OrdersList.size();
        if (p2OrdersList.size() < levels) {
            levels = p2OrdersList.size();
        }

        /*
         * check the numbers to the minor level of the two
         */
        for (int i = 0; i < levels; i++) {
            int thisone = p1OrdersList.get(i);
            int otherone = p2OrdersList.get(i);
            if (thisone > otherone) {
                /*
                 * if this has major number of the other, then this has to be sorted as minor of the
                 * other, following the pfafstetter logic that has major numbers towards valley
                 */
                return -1;
            } else if (thisone < otherone) {
                return 1;
            }
            // if they are equal, go on to the next level
        }

        return 0;
    }

    /**
     * Checks if two pfafstetter are connected upstream, i.e. p1 is more downstream than p2
     *
     * @param p1 the first pfafstetter.
     * @param p2 the second pfafstetter.
     * @return <code>true</code> if the first is more upstream than the second.
     */
    public synchronized static boolean areConnectedUpstream(PfafstetterNumber p1,
                                                            PfafstetterNumber p2) {

        List<Integer> p1OrdersList = p1.getOrdersList();
        List<Integer> p2OrdersList = p2.getOrdersList();

        int levelDiff = p1OrdersList.size() - p2OrdersList.size();
        if (levelDiff == 0) {
            if (p1.toStringUpToLastLevel().equals(p2.toStringUpToLastLevel())) {
                int p1Last = p1OrdersList.get(p1OrdersList.size() - 1);
                int p2Last = p2OrdersList.get(p2OrdersList.size() - 1);
                if (p2Last == p1Last + 1 || p2Last == p1Last + 2) {
                    return p1Last % 2 != 0;
                }
            }
        } else if (levelDiff == -1) {
            if (p2.toString().startsWith(p1.toStringUpToLastLevel())) {
                int p2Last = p2OrdersList.get(p2OrdersList.size() - 1);
                if (p2Last != 1) {
                    return false;
                }
                int p1Last = p1OrdersList.get(p1OrdersList.size() - 1);
                int p2LastMinus1 = p2OrdersList.get(p2OrdersList.size() - 2);
                if (p2LastMinus1 == p1Last + 1 || p2Last == p1Last + 2) {
                    return p1Last % 2 != 0;
                }
            }
        }
        return false;
    }

    /**
     * Inverse of {@link #areConnectedUpstream(org.hortonmachine.hmachine.modules.network.PfafstetterNumber, org.hortonmachine.hmachine.modules.network.PfafstetterNumber)} .
     *
     * @param p1 the first pfafstetter.
     * @param p2 the second pfafstetter.
     * @return <code>true</code> if the first is more downstream than the second.
     */
    public synchronized static boolean areConnectedDownstream(PfafstetterNumber p1,
                                                              PfafstetterNumber p2) {
        return areConnectedUpstream(p2, p1);
    }


    public String toString() {
        return pfafstetterNumberString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PfafstetterNumber that = (PfafstetterNumber) o;

        if (!pfafstetterNumberString.equals(that.pfafstetterNumberString)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return pfafstetterNumberString.hashCode();
    }
}
