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
package org.hortonmachine.gears.io.dxfdwg.libs.dwg.utils;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Clase que engloba mtodos para trabajar con bytes. 
 *
 * @author Vicente Caballero Navarro
 */
public class ByteUtils {
    public static final int SIZE_BOOL = 1;
    public static final int SIZE_SHORT = 2;
    public static final int SIZE_INT = 4;
    public static final int SIZE_LONG = 8;
    public static final int SIZE_DOUBLE = 8;

    /** A nibble->char mapping for printing out bytes. */
    public static final char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Return the <code>int</code> represented by the bytes in
     * <code>data</code> staring at offset <code>offset[0]</code>.
     *
     * @param data the array from which to read
     * @param offset A single element array whose first element is the index in
     * 		  data from which to begin reading on function entry, and which on
     * 		  function exit has been incremented by the number of bytes read.
     *
     * @return the value of the <code>int</code> decoded
     */
    public static final int bytesToInt( byte[] data, int[] offset ) {
        /**
         * TODO: We use network-order within OceanStore, but temporarily
         * supporting intel-order to work with some JNI code until JNI code is
         * set to interoperate with network-order.
         */
        int result = 0;

        for( int i = 0; i < SIZE_INT; ++i ) {
            result <<= 8;
            result |= byteToUnsignedInt(data[offset[0]++]);
        }

        return result;
    }

    /**
     * Write the bytes representing <code>i</code> into the byte array
     * <code>data</code>, starting at index <code>offset [0]</code>, and
     * increment <code>offset [0]</code> by the number of bytes written; if
     * <code>data == null</code>, increment <code>offset [0]</code> by the
     * number of bytes that would have been written otherwise.
     *
     * @param i the <code>int</code> to encode
     * @param data The byte array to store into, or <code>null</code>.
     * @param offset A single element array whose first element is the index in
     * 		  data to begin writing at on function entry, and which on
     * 		  function exit has been incremented by the number of bytes
     * 		  written.
     */
    public static final void intToBytes( int i, byte[] data, int[] offset ) {
        /**
         * TODO: We use network-order within OceanStore, but temporarily
         * supporting intel-order to work with some JNI code until JNI code is
         * set to interoperate with network-order.
         */
        if (data != null) {
            for( int j = (offset[0] + SIZE_INT) - 1; j >= offset[0]; --j ) {
                data[j] = (byte) i;
                i >>= 8;
            }
        }

        offset[0] += SIZE_INT;
    }

    /**
     * Return the <code>short</code> represented by the bytes in
     * <code>data</code> staring at offset <code>offset[0]</code>.
     *
     * @param data the array from which to read
     * @param offset A single element array whose first element is the index in
     * 		  data from which to begin reading on function entry, and which on
     * 		  function exit has been incremented by the number of bytes read.
     *
     * @return the value of the <code>short</code> decoded
     */
    public static final short bytesToShort( byte[] data, int[] offset ) {
        /**
         * TODO: We use network-order within OceanStore, but temporarily
         * supporting intel-order to work with some JNI code until JNI code is
         * set to interoperate with network-order.
         */
        short result = 0;

        for( int i = 0; i < SIZE_SHORT; ++i ) {
            result <<= 8;
            result |= (short) byteToUnsignedInt(data[offset[0]++]);
        }

        return result;
    }

    /**
     * Write the bytes representing <code>s</code> into the byte array
     * <code>data</code>, starting at index <code>offset [0]</code>, and
     * increment <code>offset [0]</code> by the number of bytes written; if
     * <code>data == null</code>, increment <code>offset [0]</code> by the
     * number of bytes that would have been written otherwise.
     *
     * @param s the <code>short</code> to encode
     * @param data The byte array to store into, or <code>null</code>.
     * @param offset A single element array whose first element is the index in
     * 		  data to begin writing at on function entry, and which on
     * 		  function exit has been incremented by the number of bytes
     * 		  written.
     */
    public static final void shortToBytes( short s, byte[] data, int[] offset ) {
        /**
         * TODO: We use network-order within OceanStore, but temporarily
         * supporting intel-order to work with some JNI code until JNI code is
         * set to interoperate with network-order.
         */
        if (data != null) {
            data[offset[0] + 1] = (byte) s;
            data[offset[0]] = (byte) (s >> 8);
        }

        offset[0] += SIZE_SHORT;
    }

    /**
     * Return the <code>long</code> represented by the bytes in
     * <code>data</code> staring at offset <code>offset[0]</code>.
     *
     * @param data the array from which to read
     * @param offset A single element array whose first element is the index in
     * 		  data from which to begin reading on  function entry, and which
     * 		  on function exit has been incremented by the number of bytes
     * 		  read.
     *
     * @return the value of the <code>long</code> decoded
     */
    public static final long bytesToLong( byte[] data, int[] offset ) {
        long result = 0;

        for( int i = 0; i < SIZE_LONG; ++i ) {
            result <<= 8;

            int res = byteToUnsignedInt(data[offset[0]++]);
            result = result | res;
        }

        return result;
    }

    /**
     * Write the bytes representing <code>l</code> into the byte array
     * <code>data</code>, starting at index <code>offset [0]</code>, and
     * increment <code>offset [0]</code> by the number of bytes written; if
     * <code>data == null</code>, increment <code>offset [0]</code> by the
     * number of bytes that would have been written otherwise.
     *
     * @param l the <code>long</code> to encode
     * @param data The byte array to store into, or <code>null</code>.
     * @param offset A single element array whose first element is the index in
     * 		  data to begin writing at on function entry, and which on
     * 		  function exit has been incremented by the number of bytes
     * 		  written.
     */
    public static final void longToBytes( long l, byte[] data, int[] offset ) {
        /**
         * TODO: We use network-order within OceanStore, but temporarily
         * supporting intel-order to work with some JNI code until JNI code is
         * set to interoperate with network-order.
         */
        if (data != null) {
            for( int j = (offset[0] + SIZE_LONG) - 1; j >= offset[0]; --j ) {
                data[j] = (byte) l;
                l >>= 8;
            }
        }

        offset[0] += SIZE_LONG;
    }

    /**
     * Return the <code>double</code> represented by the bytes in
     * <code>data</code> staring at offset <code>offset[0]</code>.
     *
     * @param data the array from which to read
     * @param offset A single element array whose first element is the index in
     * 		  data from which to begin reading on  function entry, and which
     * 		  on function exit has been incremented by the number of bytes
     * 		  read.
     *
     * @return the value of the <code>double</code> decoded
     */
    public static final double bytesToDouble( byte[] data, int[] offset ) {
        long bits = bytesToLong(data, offset);

        return Double.longBitsToDouble(bits);
    }

    /**
     * Write the bytes representing <code>d</code> into the byte array
     * <code>data</code>, starting at index <code>offset [0]</code>, and
     * increment <code>offset [0]</code> by the number of bytes written; if
     * <code>data == null</code>, increment <code>offset [0]</code> by the
     * number of bytes that would have been written otherwise.
     *
     * @param d the <code>double</code> to encode
     * @param data The byte array to store into, or <code>null</code>.
     * @param offset A single element array whose first element is the index in
     * 		  data to begin writing at on function entry, and which on
     * 		  function exit has been incremented by the number of bytes
     * 		  written.
     */
    public static final void doubleToBytes( double d, byte[] data, int[] offset ) {
        long bits = Double.doubleToLongBits(d);
        longToBytes(bits, data, offset);
    }

    /**
     * Return the <code>String</code> represented by the bytes in
     * <code>data</code> staring at offset <code>offset[0]</code>. This method
     * relies on the user using the corresponding <code>stringToBytes</code>
     * method to encode the <code>String</code>, so that it may properly
     * retrieve the <code>String</code> length.
     *
     * @param data the array from which to read
     * @param offset A single element array whose first element is the index in
     * 		  data from which to begin reading on function entry, and which on
     * 		  function exit has been incremented by the number of bytes read.
     *
     * @return the value of the <code>String</code> decoded
     */
    public static final String bytesToString( byte[] data, int[] offset ) {
        offset[0] = 0;

        int length = bytesToInt(data, offset);
        String st = null;

        if ((length < 0) || (length > data.length)) {
            st = new String(data);
        } else {
            st = new String(data, offset[0], length);
        }

        offset[0] += length;

        return st;
    }

    /**
     * Write the bytes representing <code>s</code> into the byte array
     * <code>data</code>, starting at index <code>offset [0]</code>, and
     * increment <code>offset [0]</code> by the number of bytes written; if
     * <code>data == null</code>, increment <code>offset [0]</code> by the
     * number of bytes that would have been written otherwise.
     *
     * @param s the <code>String</code> to encode
     * @param data The byte array to store into, or <code>null</code>.
     * @param offset A single element array whose first element is the index in
     * 		  data to begin writing at on function entry, and which on
     * 		  function exit has been incremented by the number of bytes
     * 		  written.
     */
    public static final void stringToBytes( String s, byte[] data, int[] offset ) {
        byte[] s_bytes = s.getBytes();

        if (data != null) {
            intToBytes(s_bytes.length, data, offset);
            memcpy(data, offset[0], s_bytes, 0, s_bytes.length);
        } else {
            offset[0] += SIZE_INT;
        }

        offset[0] += s_bytes.length;
    }

    /**
     * Return the <code>boolean</code> represented by the bytes in
     * <code>data</code> staring at offset <code>offset[0]</code>.
     *
     * @param data the array from which to read
     * @param offset A single element array whose first element is the index in
     * 		  data from which to begin reading on  function entry, and which
     * 		  on function exit has been incremented by the number of bytes
     * 		  read.
     *
     * @return the value of the <code>boolean</code> decoded
     */
    public static final boolean bytesToBool( byte[] data, int[] offset ) {
        boolean result = true;

        if (data[offset[0]] == 0) {
            result = false;
        }

        offset[0] += SIZE_BOOL;

        return result;
    }

    /**
     * Write the bytes representing <code>b</code> into the byte array
     * <code>data</code>, starting at index <code>offset [0]</code>, and
     * increment <code>offset [0]</code> by the number of bytes written; if
     * <code>data == null</code>, increment <code>offset [0]</code> by the
     * number of bytes that would have been written otherwise.
     *
     * @param b the <code>boolean</code> to encode
     * @param data The byte array to store into, or <code>null</code>.
     * @param offset A single element array whose first element is the index in
     * 		  data to begin writing at on function entry, and which on
     * 		  function exit has been incremented by the number of bytes
     * 		  written.
     */
    public static final void boolToBytes( boolean b, byte[] data, int[] offset ) {
        if (data != null) {
            data[offset[0]] = (byte) (b ? 1 : 0);
        }

        offset[0] += SIZE_BOOL;
    }

    /**
     * Return the <code>BigInteger</code> represented by the bytes in
     * <code>data</code> staring at offset <code>offset[0]</code>.
     *
     * @param data the array from which to read
     * @param offset A single element array whose first element is the index in
     * 		  data from which to begin reading on  function entry, and which
     * 		  on function exit has been incremented by the number of bytes
     * 		  read.
     *
     * @return the <code>BigInteger</code> decoded
     */
    public static final BigInteger bytesToBigInteger( byte[] data, int[] offset ) {
        int length = bytesToInt(data, offset);
        byte[] bytes = new byte[length];
        offset[0] += memcpy(bytes, 0, data, offset[0], length);

        return new BigInteger(bytes);
    }

    /**
     * Write the bytes representing <code>n</code> into the byte array
     * <code>data</code>, starting at index <code>offset [0]</code>, and
     * increment <code>offset [0]</code> by the number of bytes written; if
     * <code>data == null</code>, increment <code>offset [0]</code> by the
     * number of bytes that would have been written otherwise.
     *
     * @param n the <code>BigInteger</code> to encode
     * @param data The byte array to store into, or <code>null</code>.
     * @param offset A single element array whose first element is the index in
     * 		  data to begin writing at on function entry, and which on
     * 		  function exit has been incremented by the number of bytes
     * 		  written.
     */
    public static final void bigIntegerToBytes( BigInteger n, byte[] data, int[] offset ) {
        byte[] bytes = n.toByteArray();
        intToBytes(bytes.length, data, offset);
        offset[0] += memcpy(data, offset[0], bytes, 0, bytes.length);
    }

    /**
     * Convert an array of <code>bytes</code>s into an array of
     * <code>ints</code>.
     *
     * @param dst the array to write
     * @param dst_offset the start offset in <code>dst</code>, times 4. This
     * 		  measures the offset as if <code>dst</code> were an array of
     * 		  <code>byte</code>s (rather than <code>int</code>s).
     * @param src the array to read
     * @param src_offset the start offset in <code>src</code>
     * @param length the number of <code>byte</code>s to copy.
     */
    public static final void bytesToInts( int[] dst, int dst_offset, byte[] src, int src_offset, int length ) {
        if ((src == null) || (dst == null) || ((src_offset + length) > src.length) || ((dst_offset + length) > (dst.length * 4))
                || ((dst_offset % 4) != 0) || ((length % 4) != 0)) {
            croak("bytesToInts parameters are invalid"
                    + " src=="
                    + Arrays.toString(src)
                    + " dst=="
                    + Arrays.toString(dst)
                    + (((src == null) || (dst == null)) ? " " : (" (src_offset+length)>src.length==" + (src_offset + length)
                            + ">" + src.length + " (dst_offset+length)>(dst.length*4)==" + (dst_offset + length) + ">"
                            + (dst.length * 4) + " (dst_offset%4)==" + (dst_offset % 4) + " (length%4)==" + (length % 4)
                            + " dest.length==" + dst.length + " length==" + length)));
        }

        // Convert parameters to normal format
        int[] offset = new int[1];
        offset[0] = src_offset;

        int int_dst_offset = dst_offset / 4;

        for( int i = 0; i < (length / 4); ++i ) {
            dst[int_dst_offset++] = bytesToInt(src, offset);
        }
    }

    /**
     * Convert an array of <code>int</code>s into an array of
     * <code>bytes</code>.
     *
     * @param dst the array to write
     * @param dst_offset the start offset in <code>dst</code>
     * @param src the array to read
     * @param src_offset the start offset in <code>src</code>, times 4. This
     * 		  measures the offset as if <code>src</code> were an array of
     * 		  <code>byte</code>s (rather than <code>int</code>s).
     * @param length the number of <code>byte</code>s to copy.
     */
    public static final void intsToBytes( byte[] dst, int dst_offset, int[] src, int src_offset, int length ) {
        if ((src == null) || (dst == null) || ((dst_offset + length) > dst.length) || ((src_offset + length) > (src.length * 4))
                || ((src_offset % 4) != 0) || ((length % 4) != 0)) {
            croak("intsToBytes parameters are invalid:" + " src=" + Arrays.toString(src) + " dst=" + Arrays.toString(dst)
                    + " (dst_offset=" + dst_offset + " + length=" + length + ")=" + (dst_offset + length) + " > dst.length="
                    + ((dst == null) ? 0 : dst.length) + " (src_offset=" + src_offset + " + length=" + length + ")="
                    + (src_offset + length) + " > (src.length=" + ((src == null) ? 0 : src.length) + "*4)="
                    + ((src == null) ? 0 : (src.length * 4)) + " (src_offset=" + src_offset + " % 4)=" + (src_offset % 4)
                    + " != 0" + " (length=" + length + " % 4)=" + (length % 4) + " != 0");
        }

        // Convert parameters to normal format
        int[] offset = new int[1];
        offset[0] = dst_offset;

        int int_src_offset = src_offset / 4;

        for( int i = 0; i < (length / 4); ++i ) {
            intToBytes(src[int_src_offset++], dst, offset);
        }
    }

    /**
     * Convert a <code>byte</code> into an unsigned integer.
     *
     * @param b the <code>byte</code> to cast
     *
     * @return a postiive <code>int</code> whose lowest byte contains the bits
     * 		   of <code>b</code>.
     */
    public static final int byteToUnsignedInt( byte b ) {
        return ((int) b) & 0xff;
    }

    /**
     * Copy contents of one array of <code>bytes</code> into another. If either
     * array is <code>null</code>, simply return the <code>length</code>
     * parameter directly.
     *
     * @param dst the array to write, or <code>null</code>
     * @param dst_offset the start offset in <code>dst</code>
     * @param src the array to read, or <code>null</code>
     * @param src_offset the start offset in <code>src</code>
     * @param length the number of <code>byte</code>s to copy.
     *
     * @return DOCUMENT ME!
     */
    public static int memcpy( byte[] dst, int dst_offset, byte[] src, int src_offset, int length ) {
        if ((dst != null) && (src != null)) {
            if (dst.length < (dst_offset + length)) {
                croak("dst.length = " + dst.length + ", but " + "dst_offset = " + dst_offset + " and length = " + length + ".");
            }

            if (src.length < (src_offset + length)) {
                croak("src.length = " + src.length + ", but " + "src_offset = " + src_offset + " and length = " + length + ".");
            }

            for( int i = 0; i < length; ++i, ++dst_offset, ++src_offset )
                dst[dst_offset] = src[src_offset];
        }

        return length;
    }

    /**
     * Compare the contents of one array of <code>bytes</code> to another.
     *
     * @param a the first array
     * @param a_offset the start offset in <code>a</code>
     * @param b the second array
     * @param b_offset the start offset in <code>b</code>
     * @param length the number of <code>byte</code>s to compare.
     *
     * @return DOCUMENT ME!
     */
    public static boolean memcmp( byte[] a, int a_offset, byte[] b, int b_offset, int length ) {
        if ((a == null) && (b == null)) {
            return true;
        }

        if ((a == null) || (b == null)) {
            return false;
        }

        for( int i = 0; i < length; ++i, ++a_offset, ++b_offset )
            if (a[a_offset] != b[b_offset]) {
                return false;
            }

        return true;
    }

    /**
     * Fill the given array with zeros.
     *
     * @param array the array to clear
     * @param offset the start offset
     * @param length the number of <code>byte</code>s to clear.
     */
    public static void memclr( byte[] array, int offset, int length ) {
        for( int i = 0; i < length; ++i, ++offset )
            array[offset] = 0;
    }

    /**
     * Round a number up to a given multiple.
     *
     * @param value the number to be rounded
     * @param multiple the number to which to be rounded
     *
     * @return the smallest <code>int</code> greater than or equal to
     * 		   <code>value</code> which divides <code>multiple</code> exactly.
     */
    public static int round_up( int value, int multiple ) {
        return (((value - 1) / multiple) + 1) * multiple;
    }

    /**
     * Return a new array equal to original except zero-padded to an integral
     * mulitple of blocks.  If the original is already an integral multiple of
     * blocks, just return it.
     *
     * @param original the array of <code>byte</code>s to be padded
     * @param block_size the size of the blocks
     *
     * @return an array whose size divides <code>block_size</code> exactly. The
     * 		   array is either <code>original</code> itself, or a copy whose
     * 		   first <code>original.length</code> bytes are equal to
     * 		   <code>original</code>.
     */
    public static byte[] zero_pad( byte[] original, int block_size ) {
        if ((original.length % block_size) == 0) {
            return original;
        }

        byte[] result = new byte[round_up(original.length, block_size)];
        memcpy(result, 0, original, 0, original.length);

        // Unnecessary - jvm sets bytes to 0.
        // memclr (result, original.length, result.length - original.length);
        return result;
    }

    /**
     * Determines whether two arrays of <code>byte</code>s contain the same
     * contents.
     *
     * @param b1 The first array
     * @param b2 The second array
     *
     * @return <code>true</code> if both arrays are <code>null</code>, both
     * 		   empty, or both of the same length with equal contents.
     */
    public static boolean equals( byte[] b1, byte[] b2 ) {
        if (b1 == b2) {
            return true;
        }

        if ((b1 == null) || (b2 == null)) { // only one is null

            return false;
        }

        if (b1.length != b2.length) {
            return false;
        }

        for( int i = 0; i < b1.length; ++i )
            if (b1[i] != b2[i]) {
                return false;
            }

        return true;
    }

    /**
     * Produce a <code>String</code> representation for the specified array of
     * <code>byte</code>s.  Print each <code>byte</code> as two hexadecimal
     * digits.
     *
     * @param data The array to print
     * @param offset the start offset in <code>data</code>
     * @param length the number of <code>byte</code>s to print
     *
     * @return DOCUMENT ME!
     */
    public static String print_bytes( byte[] data, int offset, int length ) {
        int size = 2 * length;
        size += ((size / 8) + (size / 64));

        char[] buf = new char[size];
        int low_mask = 0x0f;
        int high_mask = 0xf0;
        int buf_pos = 0;
        byte b;

        int j = 0;

        for( int i = offset; i < (offset + length); ++i ) {
            b = data[i];
            buf[buf_pos++] = digits[(high_mask & b) >> 4];
            buf[buf_pos++] = digits[(low_mask & b)];

            if ((j % 4) == 3) {
                buf[buf_pos++] = ' ';
            }

            if ((j % 32) == 31) {
                buf[buf_pos++] = '\n';
            }

            ++j;
        }

        return new String(buf);
    }

    /**
     * DOCUMENT ME!
     *
     * @param data DOCUMENT ME!
     * @param offset DOCUMENT ME!
     * @param length DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static String print_bytes_exact( byte[] data, int offset, int length ) {
        int size = 2 * length;
        char[] buf = new char[size];
        int low_mask = 0x0f;
        int high_mask = 0xf0;
        int buf_pos = 0;
        byte b;

        int j = 0;

        for( int i = offset; i < (offset + length); ++i ) {
            b = data[i];
            buf[buf_pos++] = digits[(high_mask & b) >> 4];
            buf[buf_pos++] = digits[(low_mask & b)];
            ++j;
        }

        return new String(buf);
    }

    /**
     * DOCUMENT ME!
     *
     * @param data DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static String print_bytes( byte[] data ) {
        return print_bytes(data, 0, data.length);
    }

    /**
     * DOCUMENT ME!
     *
     * @param msg DOCUMENT ME!
     */
    private static void croak( String msg ) {
        // throw new java.AssertionViolatedException(msg);
    }

    /**
     * DOCUMENT ME!
     *
     * @param b DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static int getUnsigned( byte b ) {
        return ((b & 0xff)); // >> 8);
    }
}
