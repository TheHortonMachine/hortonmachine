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
package eu.hydrologis.jgrass.hortonmachine.utils;

import java.util.BitSet;

public class ByteUtilities {
    public static byte[] double2bytearray( double doubleValue ) {
        long l = Double.doubleToLongBits(doubleValue);
        byte[] b = new byte[8];
        int shift = 64 - 8;
        for( int k = 0; k < 8; k++, shift -= 8 ) {
            b[k] = (byte) (l >>> shift);
        }
        return b;
    }

    public static BitSet fromByteArray( byte[] bytes ) {
        BitSet bits = new BitSet();
        for( int i = 0; i < bytes.length * 8; i++ ) {
            if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }

    public static BitSet fromByte( byte thebyte ) {
        BitSet bits = new BitSet();
        for( int i = 0; i < 8; i++ ) {
            if ((thebyte & (1 << (i % 8))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }

    public static byte[] BitSet2ByteArray( BitSet bs ) {
        byte[] bytes = new byte[bs.size() / 8 + 1];

        for( int i = 0; i < bs.size(); i++ )
            if (bs.get(i))
                bytes[i / 8] |= 1 << (i % 8);

        return bytes;
    }

    public static byte[] toByteArray( BitSet bits ) {
        byte[] bytes = new byte[bits.length() / 8 + 1];
        for( int i = 0; i < bits.length(); i++ ) {
            if (bits.get(i)) {
                bytes[bytes.length - i / 8 - 1] |= 1 << (i % 8);
            }
        }
        return bytes;
    }

    public static float arr2float( byte[] arr, int start ) {
        int i = 0;
        int len = 4;
        int cnt = 0;
        byte[] tmp = new byte[len];
        for( i = start; i < (start + len); i++ ) {
            tmp[cnt] = arr[i];
            cnt++;
        }
        int accum = 0;
        i = 0;
        for( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
            accum |= ((long) (tmp[i] & 0xff)) << shiftBy;
            i++;
        }
        return Float.intBitsToFloat(accum);
    }

    public static int arr2int( byte[] arr, int start ) {
        int low = arr[start] & 0xff;
        int high = arr[start + 1] & 0xff;
        return (int) (high << 8 | low);
    }

    public static long arr2long( byte[] arr, int start ) {
        int i = 0;
        int len = 4;
        int cnt = 0;
        byte[] tmp = new byte[len];
        for( i = start; i < (start + len); i++ ) {
            tmp[cnt] = arr[i];
            cnt++;
        }
        long accum = 0;
        i = 0;
        for( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
            accum |= ((long) (tmp[i] & 0xff)) << shiftBy;
            i++;
        }
        return accum;
    }

    public static double arr2double( byte[] arr, int start ) {
        int i = 0;
        int len = 8;
        int cnt = 0;
        byte[] tmp = new byte[len];
        for( i = start; i < (start + len); i++ ) {
            tmp[cnt] = arr[i];
            cnt++;
        }
        long accum = 0;
        i = 0;
        for( int shiftBy = 0; shiftBy < 64; shiftBy += 8 ) {
            accum |= ((long) (tmp[i] & 0xff)) << shiftBy;
            i++;
        }
        return Double.longBitsToDouble(accum);
    }

}
