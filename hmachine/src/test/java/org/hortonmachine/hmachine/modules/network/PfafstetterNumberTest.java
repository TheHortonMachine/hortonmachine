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

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hortonmachine.hmachine.modules.network.PfafstetterNumber;

public class PfafstetterNumberTest extends TestCase {

    public void testSortingAndConnections() throws Exception {
        PfafstetterNumber n1 = new PfafstetterNumber("2.5"); //$NON-NLS-1$
        PfafstetterNumber n2 = new PfafstetterNumber("2.6.4"); //$NON-NLS-1$
        PfafstetterNumber n3 = new PfafstetterNumber("2.4.3"); //$NON-NLS-1$
        PfafstetterNumber n4 = new PfafstetterNumber("2.7.1"); //$NON-NLS-1$
        PfafstetterNumber n5 = new PfafstetterNumber("2.4.16.45"); //$NON-NLS-1$
        PfafstetterNumber n6 = new PfafstetterNumber("2.6.2.1"); //$NON-NLS-1$
        PfafstetterNumber n7 = new PfafstetterNumber("2.7.6.5.2"); //$NON-NLS-1$
        PfafstetterNumber n8 = new PfafstetterNumber("2.7.6.2.1"); //$NON-NLS-1$
        PfafstetterNumber n9 = new PfafstetterNumber("2.6.2.7"); //$NON-NLS-1$
        List<PfafstetterNumber> list = new ArrayList<PfafstetterNumber>();
        list.add(n1);
        list.add(n2);
        list.add(n3);
        list.add(n4);
        list.add(n5);
        list.add(n6);
        list.add(n7);
        list.add(n8);

        assertEquals(true, n1.isDownStreamOf(n2));
        assertEquals(true, n1.isDownStreamOf(n4));
        assertEquals(false, n3.isDownStreamOf(n2));
        assertEquals(true, n3.isDownStreamOf(n5));
        assertEquals(false, n4.isDownStreamOf(n1));
        assertEquals(false, n6.isDownStreamOf(n7));
        assertEquals(false, n8.isDownStreamOf(n7));

        assertEquals(false,PfafstetterNumber.areConnectedUpstream(n1, n2));
        assertEquals(false,PfafstetterNumber.areConnectedUpstream(n1, n4));
        assertEquals(false,PfafstetterNumber.areConnectedUpstream(n3, n2));
        assertEquals(false,PfafstetterNumber.areConnectedUpstream(n3, n5));
        assertEquals(false,PfafstetterNumber.areConnectedUpstream(n4, n1));
        assertEquals(false,PfafstetterNumber.areConnectedUpstream(n4, n6));
        assertEquals(false,PfafstetterNumber.areConnectedUpstream(n8, n7));
        assertEquals(false,PfafstetterNumber.areConnectedUpstream(n6, n9));

        Collections.sort(list);
        PfafstetterNumber[] array = list.toArray(new PfafstetterNumber[list.size()]);

        assertEquals("2.7.6.5.2", array[0].toString());
        assertEquals("2.7.6.2.1", array[1].toString());
        assertEquals("2.7.1", array[2].toString());
        assertEquals("2.6.4", array[3].toString());
        assertEquals("2.6.2.1", array[4].toString());
        assertEquals("2.5", array[5].toString());
        assertEquals("2.4.16.45", array[6].toString());
        assertEquals("2.4.3", array[7].toString());
    }
}