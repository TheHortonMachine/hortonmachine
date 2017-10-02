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

import java.util.BitSet;

/**
 * Number in hexadecimal format are used throughout Freenet.
 * 
 * <p>Unless otherwise stated, the conventions follow the rules outlined in the 
 * Java Language Specification.</p>
 * 
 * @author syoung
 */
public class HexUtil {
	private HexUtil() {		
	}	
	

	/**
	 * Converts a byte array into a string of upper case hex chars.
	 * 
	 * @param bs
	 *            A byte array
	 * @param off
	 *            The index of the first byte to read
	 * @param length
	 *            The number of bytes to read.
	 * @return the string of hex chars.
	 */
	public static final String bytesToHex(byte[] bs, int off, int length) {
		StringBuffer sb = new StringBuffer(length * 2);
		bytesToHexAppend(bs, off, length, sb);
		return sb.toString();
	}

	public static final void bytesToHexAppend(
		byte[] bs,
		int off,
		int length,
		StringBuffer sb) {
		sb.ensureCapacity(sb.length() + length * 2);
		for (int i = off; i < (off + length) && i < bs.length; i++) {
			sb.append(Character.forDigit((bs[i] >>> 4) & 0xf, 16));
			sb.append(Character.forDigit(bs[i] & 0xf, 16));
		}
	}

	public static final String bytesToHex(byte[] bs) {
		return bytesToHex(bs, 0, bs.length);
	}

	public static final byte[] hexToBytes(String s) {
		return hexToBytes(s, 0);
	}

	public static final byte[] hexToBytes(String s, int off) {
		byte[] bs = new byte[off + (1 + s.length()) / 2];
		hexToBytes(s, bs, off);
		return bs;
	}

	/**
	 * Converts a String of hex characters into an array of bytes.
	 * 
	 * @param s
	 *            A string of hex characters (upper case or lower) of even
	 *            length.
	 * @param out
	 *            A byte array of length at least s.length()/2 + off
	 * @param off
	 *            The first byte to write of the array
	 */
	public static final void hexToBytes(String s, byte[] out, int off)
		throws NumberFormatException, IndexOutOfBoundsException {
		int slen = s.length();
		if ((slen % 2) != 0) {
			s = '0' + s;
		}

		if (out.length < off + slen / 2) {
			throw new IndexOutOfBoundsException(
				"Output buffer too small for input ("
					+ out.length
					+ "<"
					+ off
					+ slen / 2
					+ ")");
		}

		// Safe to assume the string is even length
		byte b1, b2;
		for (int i = 0; i < slen; i += 2) {
			b1 = (byte) Character.digit(s.charAt(i), 16);
			b2 = (byte) Character.digit(s.charAt(i + 1), 16);
			if (b1 < 0 || b2 < 0) {
				throw new NumberFormatException();
			}
			out[off + i / 2] = (byte) (b1 << 4 | b2);
		}
	}

	/**
	 * Pack the bits in ba into a byte[].
	 */
	public final static byte[] bitsToBytes(BitSet ba, int size) {
		int bytesAlloc = countBytesForBits(size);
		byte[] b = new byte[bytesAlloc];
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<b.length;i++) {
			short s = 0;
			for(int j=0;j<8;j++) {
				int idx = i*8+j;
				boolean val = 
					idx > size ? false :
						ba.get(idx);
				s |= val ? (1<<j) : 0;
				sb.append(val ? '1' : '0');
			}
			if(s > 255) throw new IllegalStateException("WTF? s = "+s);
			b[i] = (byte)s;
		}
		return b;
	}

	/**
	 * Pack the bits in ba into a byte[] then convert that
	 * to a hex string and return it.
	 */
	public final static String bitsToHexString(BitSet ba, int size) {
		return bytesToHex(bitsToBytes(ba, size));
	}


	/**
	 * @return the number of bytes required to represent the
	 * bitset
	 */
	public static int countBytesForBits(int size) {
		// Brackets matter here! == takes precedence over the rest
		return (size/8) + ((size % 8) == 0 ? 0:1);
	}


	/**
	 * Read bits from a byte array into a bitset
	 * @param b the byte[] to read from
	 * @param ba the bitset to write to
	 */
	public static void bytesToBits(byte[] b, BitSet ba, int maxSize) {
		int x = 0;
		for(int i=0;i<b.length;i++) {
			for(int j=0;j<8;j++) {
				if(x > maxSize) break;
				int mask = 1 << j;
				boolean value = (mask & b[i]) != 0;
				ba.set(x, value);
				x++;
			}
		}
	}


	/**
	 * Read a hex string of bits and write it into a bitset
	 * @param s hex string of the stored bits
	 * @param ba the bitset to store the bits in
	 * @param length the maximum number of bits to store 
	 */
	public static void hexToBits(String s, BitSet ba, int length) {
		byte[] b = hexToBytes(s);
		bytesToBits(b, ba, length);
	}
}
