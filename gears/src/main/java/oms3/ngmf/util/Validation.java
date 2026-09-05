/*
 * $Id: Validation.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */
package oms3.ngmf.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * 
 * @author od
 */
public class Validation {

    /**
     * Creates a hex encoded sha-256 hash of all files.
     *
     * @param algorithm the algorithm to be used.
     * @param files the files to include for this hash.
     * @return the hexadecimal digest (length 64byte for sha-256).
     */
    public static String hexDigest(String algorithm, File[] files) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] buf = new byte[4096];
            for (File f : files) {
                FileInputStream in = new FileInputStream(f);
                int nread = in.read(buf);
                while (nread > 0) {
                    md.update(buf, 0, nread);
                    nread = in.read(buf);
                }
                in.close();
            }
            return toHex(md.digest(buf));
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return "<error>";
    }

    private static String toHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }
}
