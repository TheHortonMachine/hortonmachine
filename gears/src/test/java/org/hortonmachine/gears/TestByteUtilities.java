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
package org.hortonmachine.gears;

import static org.hortonmachine.gears.utils.ByteUtilities.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.hortonmachine.gears.utils.HMTestCase;
/**
 * Test FeatureUtils.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestByteUtilities extends HMTestCase {

    public void testByteUtilities() throws Exception {

        short[] shorts = {0, 1, 2, 3, -1, -2, -3, 10000, -10000};
        for( short i : shorts ) {
            byte[] shortToByteArray = shortToByteArrayBE(i);
            short byteArrayToShort = byteArrayToShortBE(shortToByteArray);
            assertEquals(i, byteArrayToShort);
        }

        int[] ints = {0, 1, 2, 3, -1, -2, -3, 100000, -100000};
        for( int i : ints ) {
            // big endian
            byte[] intToByteArray = intToByteArrayBE(i);
            int byteArrayToIntBE = byteArrayToIntBE(intToByteArray);

            // little endian
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putInt(i);
            byte[] array = bb.array();
            long byteArrayToIntLE = byteArrayToIntLE(array);

            assertEquals(i, byteArrayToIntBE);
            assertEquals(i, byteArrayToIntLE);
        }

        long[] longs = {0l, 1l, 2l, 3l, -1l, -2l, -3l, 100000l, -100000l};
        for( long l : longs ) {
            // big endian
            byte[] intToByteArray = longToByteArrayBE(l);
            long byteArrayToLongBE = byteArrayToLongBE(intToByteArray);

            // little endian
            ByteBuffer bb = ByteBuffer.allocate(8);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putLong(l);
            byte[] array = bb.array();
            long byteArrayToLongLE = byteArrayToLongLE(array);

            assertEquals(l, byteArrayToLongBE);
            assertEquals(l, byteArrayToLongLE);
        }

        float[] floats = {0.5f, 1.3f, 2.7f, 3.1f, -1.0f, -2.777f, -3.888888f, 100000.123456f, -100000.98765431f};
        for( float f : floats ) {
            // big endian
            byte[] floatToByteArray = floatToByteArrayBE(f);
            float byteArrayToFloatBE = byteArrayToFloatBE(floatToByteArray);

            // little endian
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putFloat(f);
            byte[] array = bb.array();
            float byteArrayToFloatLE = byteArrayToFloatLE(array);

            assertEquals(f, byteArrayToFloatBE);
            assertEquals(f, byteArrayToFloatLE);
        }

        double[] doubles = {0.5, 1.3, 2.7, 3.1, -1.0, -2.777, -3.888888, 100000.123456, -100000.98765431};
        for( double d : doubles ) {
            // big endian
            byte[] doubleToByteArray = doubleToByteArrayBE(d);
            double byteArrayToDoubleBE = byteArrayToDoubleBE(doubleToByteArray);
            
            // little endian
            ByteBuffer bb = ByteBuffer.allocate(8);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putDouble(d);
            byte[] array = bb.array();
            double byteArrayToDoubleLE = byteArrayToDoubleLE(array);
            
            assertEquals(d, byteArrayToDoubleBE);
            assertEquals(d, byteArrayToDoubleLE);
        }

    }

}
