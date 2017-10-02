/*
 * Library name : dxf
 * (C) 2006 Micha�l Michaud
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * michael.michaud@free.fr
 *
 */

package org.hortonmachine.gears.io.dxfdwg.libs.dxf;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * DxfGroup is a group containing a dxf code and a dxf value.
 * The class contains several utils to read and write groups an to format data.
 * @author Micha�l Michaud
 * @version 0.5.0
 */
// History
public class DxfGroup {
    // Pour �crire le symbole d�cimal anglophone (.) plut�t que fran�ais (,)
    private static final DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
    private static final DecimalFormat[] decimalFormats = new DecimalFormat[]{new DecimalFormat("#0", dfs),
            new DecimalFormat("#0.0", dfs), new DecimalFormat("#0.00", dfs), new DecimalFormat("#0.000", dfs),
            new DecimalFormat("#0.0000", dfs), new DecimalFormat("#0.00000", dfs), new DecimalFormat("#0.000000", dfs),
            new DecimalFormat("#0.0000000", dfs), new DecimalFormat("#0.00000000", dfs), new DecimalFormat("#0.000000000", dfs),
            new DecimalFormat("#0.0000000000", dfs), new DecimalFormat("#0.00000000000", dfs),
            new DecimalFormat("#0.000000000000", dfs)};

    private int code;
    private String value;
    private long address;

    public DxfGroup( int code, String value ) {
        this.code = code;
        this.value = value;
    }

    public DxfGroup( String code, String value ) throws NumberFormatException {
        try {
            this.code = Integer.parseInt(code);
            this.value = value;
        } catch (NumberFormatException nfe) {
            throw nfe;
        }
    }

    public int getCode() {
        return code;
    }
    public void setCode( int code ) {
        this.code = code;
    }
    public String getValue() {
        return value;
    }
    public int getIntValue() {
        return Integer.parseInt(value.trim());
    }
    public float getFloatValue() {
        return Float.parseFloat(value.trim());
    }
    public double getDoubleValue() {
        return Double.parseDouble(value.trim());
    }
    // public void setValue() {this.value = value;}
    public long getAddress() {
        return address;
    }
    private void setAddress( long address ) {
        this.address = address;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (address ^ (address >>> 32));
        result = prime * result + code;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    public boolean equals( Object other ) {
        if (!(other instanceof DxfGroup)) {
            return false;
        }
        if (code == ((DxfGroup) other).getCode() && value.equals(((DxfGroup) other).getValue())) {
            return true;
        } else
            return false;
    }

    public String toString() {
        String codeString = "    " + Integer.toString(code);
        int stringLength = codeString.length();
        codeString = codeString.substring(stringLength - (code < 1000 ? 3 : 4), stringLength);
        return codeString + "\r\n" + value + "\r\n";
    }

    public static String int34car( int code ) {
        if (code < 10)
            return "  " + Integer.toString(code);
        else if (code < 100)
            return " " + Integer.toString(code);
        else
            return Integer.toString(code);
    }

    public static String int6car( int value ) {
        String s = "     " + Integer.toString(value);
        return s.substring(s.length() - 6, s.length());
    }

    public static String toString( int code, String value ) {
        return int34car(code) + "\r\n" + value + "\r\n";
    }

    public static String toString( int code, int value ) {
        return int34car(code) + "\r\n" + int6car(value) + "\r\n";
    }

    public static String toString( int code, float value, int decimalPartLength ) {
        return int34car(code) + "\r\n" + decimalFormats[decimalPartLength].format((double) value) + "\r\n";
    }

    public static String toString( int code, double value, int decimalPartLength ) {
        return int34car(code) + "\r\n" + decimalFormats[decimalPartLength].format(value) + "\r\n";
    }

    public static String toString( int code, Object value ) {
        if (value instanceof String) {
            return toString(code, (String) value);
        } else if (value instanceof Integer) {
            return toString(code, ((Integer) value).intValue());
        } else if (value instanceof Float) {
            return toString(code, ((Float) value).floatValue(), 3);
        } else if (value instanceof Double) {
            return toString(code, ((Double) value).doubleValue(), 6);
        } else
            return toString(code, value.toString());
    }

    public static DxfGroup readGroup( RandomAccessFile raf ) throws IOException {
        try {
            long pos = raf.getFilePointer();
            String line1 = raf.readLine();
            String line2 = raf.readLine();
            DxfGroup dxfGroup = new DxfGroup(Integer.parseInt(line1.trim()), line2);
            dxfGroup.setAddress(pos);
            return dxfGroup;
        } catch (IOException ioe) {
            raf.close();
            throw ioe;
        }
    }

}
