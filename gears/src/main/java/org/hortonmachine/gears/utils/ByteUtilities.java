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
package org.hortonmachine.gears.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

/**
 * Utilities to handle bytes and conversions.
 * 
 * <p>Note that Java uses Big Endian by default.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.0
 */
public class ByteUtilities {

    /**
     * Convert a short to a byte array (big endian).
     * 
     * @param data the short to convert.
     * @return the byte array.
     */
    public static byte[] shortToByteArrayBE( short data ) {
        return new byte[]{(byte) ((data >> 8) & 0xff), (byte) ((data >> 0) & 0xff),};
    }

    /**
     * Convert a char to a byte array (big endian).
     * 
     * @param data the char to convert.
     * @return the byte array.
     */
    public static byte[] charToByteArrayBE( char data ) {
        return new byte[]{(byte) ((data >> 8) & 0xff), (byte) ((data >> 0) & 0xff),};
    }

    /**
     * Convert an integer to a byte array (big endian).
     * 
     * @param data the int to convert.
     * @return the byte array.
     */
    public static byte[] intToByteArrayBE( int data ) {
        return new byte[]{(byte) ((data >> 24) & 0xff), (byte) ((data >> 16) & 0xff), (byte) ((data >> 8) & 0xff),
                (byte) ((data >> 0) & 0xff),};
    }

    /**
     * Convert an long to a byte array (big endian).
     * 
     * @param data the long to convert.
     * @return the byte array.
     */
    public static byte[] longToByteArrayBE( long data ) {
        return new byte[]{(byte) ((data >> 56) & 0xff), (byte) ((data >> 48) & 0xff), (byte) ((data >> 40) & 0xff),
                (byte) ((data >> 32) & 0xff), (byte) ((data >> 24) & 0xff), (byte) ((data >> 16) & 0xff),
                (byte) ((data >> 8) & 0xff), (byte) ((data >> 0) & 0xff),};
    }

    /**
     * Convert a float to a byte array (big endian).
     * 
     * @param data the float to convert.
     * @return the byte array.
     */
    public static byte[] floatToByteArrayBE( float data ) {
        return intToByteArrayBE(Float.floatToRawIntBits(data));
    }

    /**
     * Convert a double to a byte array (big endian).
     * 
     * @param data the double to convert.
     * @return the byte array.
     */
    public static byte[] doubleToByteArrayBE( double data ) {
        return longToByteArrayBE(Double.doubleToRawLongBits(data));
    }

    /**
     * Convert a boolean to a byte array (big endian).
     * 
     * @param data the boolean to convert.
     * @return the byte array.
     */
    public static byte[] booleanToByteArrayBE( boolean data ) {
        return new byte[]{(byte) (data ? 0x01 : 0x00)}; // bool -> {1 byte}
    }

    /**
     * Convert a byte array to a short (big endian).
     * 
     * @param data the byte array to convert.
     * @return the short.
     */
    public static short byteArrayToShortBE( byte[] data ) {
        if (data == null || data.length != 2)
            return 0x0;
        // ----------
        return (short) ((0xff & data[0]) << 8 | (0xff & data[1]) << 0);
    }

    /**
     * Convert a byte array to an short (little endian).
     * 
     * @param data the byte array to convert.
     * @return the integer.
     */
    public static short byteArrayToShortLE( byte[] data ) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getShort();
    }

    /**
     * Convert a byte array to a char (big endian).
     * 
     * @param data the byte array to convert.
     * @return the char.
     */
    public static char byteArrayToCharBE( byte[] data ) {
        if (data == null || data.length != 2)
            return 0x0;
        // ----------
        return (char) ((0xff & data[0]) << 8 | (0xff & data[1]) << 0);
    }

    /**
     * Convert a byte array to an integer (big endian).
     * 
     * @param data the byte array to convert.
     * @return the integer.
     */
    public static int byteArrayToIntBE( byte[] data ) {
        if (data == null || data.length != 4)
            return 0x0;
        // ----------
        return (int) ( // NOTE: type cast not necessary for int
        (0xff & data[0]) << 24 | (0xff & data[1]) << 16 | (0xff & data[2]) << 8 | (0xff & data[3]) << 0);
    }

    /**
     * Convert a byte array to an integer (little endian).
     * 
     * @param data the byte array to convert.
     * @return the integer.
     */
    public static int byteArrayToIntLE( byte[] data ) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getInt();
    }

    /**
     * Convert a byte array to an integer (big endian).
     * 
     * @param data the byte array to convert.
     * @return the integer.
     */
    public static long byteArrayToLongBE( byte[] data ) {
        if (data == null)
            return 0x0;
        long accum = 0;
        int shiftBy = 8 * data.length - 8;
        for( byte b : data ) {
            accum |= (long) (0xff & b) << shiftBy;
            shiftBy -= 8;
        }
        return accum;
    }

    /**
     * Convert a byte array to an long (little endian).
     * 
     * @param data the byte array to convert.
     * @return the long.
     */
    public static long byteArrayToLongLE( byte[] data ) {
        if (data == null)
            return 0x0;
        long accum = 0;
        int shiftBy = 0;
        for( byte b : data ) {
            accum |= (long) (b & 0xff) << shiftBy;
            shiftBy += 8;
        }
        return accum;
    }

    public static float byteArrayToFloatBE( byte[] data ) {
        if (data == null || data.length != 4)
            return 0x0;
        // ---------- simple:
        return Float.intBitsToFloat(byteArrayToIntBE(data));
    }

    /**
     * Convert a byte array to an float (little endian).
     * 
     * @param data the byte array to convert.
     * @return the float.
     */
    public static float byteArrayToFloatLE( byte[] data ) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getFloat();
    }

    public static double byteArrayToDoubleBE( byte[] data ) {
        if (data == null || data.length != 8)
            return 0x0;
        // ---------- simple:
        return Double.longBitsToDouble(byteArrayToLongBE(data));
    }

    /**
     * Convert a byte array to an double (little endian).
     * 
     * @param data the byte array to convert.
     * @return the double.
     */
    public static double byteArrayToDoubleLE( byte[] data ) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getDouble();
    }

    public static boolean byteArrayToBoolean( byte[] data ) {
        return (data == null || data.length == 0) ? false : data[0] != 0x00;
    }

    public static String byteArrayToString( byte[] data ) {
        return (data == null) ? null : new String(data);
    }

    public static BitSet bitsetFromByteArray( byte[] bytes ) {
        BitSet bits = new BitSet();
        for( int i = 0; i < bytes.length * 8; i++ ) {
            if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }

    public static BitSet bitsetFromByte( byte thebyte ) {
        BitSet bits = new BitSet();
        for( int i = 0; i < 8; i++ ) {
            if ((thebyte & (1 << (i % 8))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }

    public static byte[] bitSetToByteArray( BitSet bs ) {
        byte[] bytes = new byte[bs.size() / 8 + 1];

        for( int i = 0; i < bs.size(); i++ )
            if (bs.get(i))
                bytes[i / 8] |= 1 << (i % 8);

        return bytes;
    }

    // public static byte[] toByteArray( BitSet bits ) {
    // byte[] bytes = new byte[bits.length() / 8 + 1];
    // for( int i = 0; i < bits.length(); i++ ) {
    // if (bits.get(i)) {
    // bytes[bytes.length - i / 8 - 1] |= 1 << (i % 8);
    // }
    // }
    // return bytes;
    // }

    // /////////////////////////////////////////////
    // /////////////////////////////////////////////
    // /////////////////////////////////////////////
    // /////////////////////////////////////////////
    // /////////////////////////////////////////////

    /**
     * @param doubleValue
     * @return
     * @deprecated use {@link #doubleToByteArrayBE(double)}.
     */
    public static byte[] double2bytearray( double doubleValue ) {
        long l = Double.doubleToLongBits(doubleValue);
        byte[] b = new byte[8];
        int shift = 64 - 8;
        for( int k = 0; k < 8; k++, shift -= 8 ) {
            b[k] = (byte) (l >>> shift);
        }
        return b;
    }

    /**
     * @param arr
     * @param start
     * @return
     * @deprecated
     */
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

    /**
     * @param arr
     * @param start
     * @return
     * @deprecated use {@link #byteArrayToLongBE(byte[])}
     */
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

    /**
     * @param arr
     * @param start
     * @return
     * @deprecated
     */
    public static int arr2int( byte[] arr, int start ) {
        int low = arr[start] & 0xff;
        int high = arr[start + 1] & 0xff;
        return (int) (high << 8 | low);
    }

    /**
     * @param arr
     * @param start
     * @return
     * @deprecated
     */
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

    /**
     * @param arr
     * @return
     * @deprecated
     */
    public static short arr2short( byte[] arr ) {
        int i = 0;
        i |= arr[0] & 0xFF;
        i <<= 8;
        i |= arr[1] & 0xFF;
        return (short) i;
    }
}
