/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.io.grasslegacy.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

/**
 * <p>
 * A displayable object.
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @since 1.1.0
 */
public abstract class GDataObject extends Object {

    public static final int POINT_DATA = 1;

    public static final int VECTOR_DATA = 2;

    public static final int RASTER_DATA = 3;

    public static final Integer VECTOR_LINE_DATA = new Integer(1);

    public static final Integer VECTOR_AREA_DATA = new Integer(2);

    public static final int USE_BACKING_FILE = 1;

    public static final int USE_BACKING_MEMORY = 2;

    public static final int COLUMN_TYPE_DOUBLE = -1;

    public static final int COLUMN_TYPE_INT = -2;

    public static final int COLUMN_TYPE_FLOAT = -3;

    public static final int COLUMN_TYPE_STRING = 256;

    /*
     * Determines whether the data that this object refers to is volatile and thus needs to be
     * re-read from source everytime it is required.
     */
    protected boolean dataIsVolatile = false;

    /* Data store object */
    protected DataStore dstore = null;

    /* Column names */
    private ArrayList columnNames = null;

    /* vector of random access handels to temporary files */
    public static Vector rafHandles = new Vector(20);

    /**
     * 
     */
    public GDataObject() {
        dataIsVolatile = false;
        dstore = null;
        columnNames = new ArrayList();
    }

    /**
     * Constructor for Point data object with xyz + some other column data.
     */
    public GDataObject( int dtype, int[] xyz, int[] c ) {
        columnNames = new ArrayList();
        dataIsVolatile = false;
        dstore = new DataStore(dtype, xyz, c, 0, USE_BACKING_FILE);
        createColumnHeader(dstore.getColumnCount());
    }

    /**
     * Constructor for Vector data object.
     */
    public GDataObject( int dtype, int use ) {
        columnNames = null;
        dataIsVolatile = false;
        dstore = new DataStore(dtype, null, null, 0, use);
    }

    /**
     * Constructor for Raster data object.
     */
    public GDataObject( int dtype, ByteBuffer buf ) {
        columnNames = null;
        dataIsVolatile = false;
        dstore = new DataStore(dtype, buf);
    }

    /**
     * Constructor for Raster data object of len size.
     */
    public GDataObject( int dtype, int len, int use ) {
        columnNames = null;
        dataIsVolatile = false;
        dstore = new DataStore(dtype, null, null, len, use);
    }

    /**
     * 
     */
    private void createColumnHeader( int cols ) {
        /* Setup default column names */
        for( int i = 0; i < cols; i++ )
            columnNames.add(String.valueOf((char) ('A' + i)));
    }

    /**
     * 
     */
    public boolean hasData() {
        return dstore.rows != 0;
    }

    /**
     * 
     */
    public ByteBuffer getReadBuffer() {
        return dstore.readBuffer;
    }

    /**
     * 
     */
    public ByteBuffer setReadBuffer( ByteBuffer newBuffer ) {
        dstore.readBuffer = null;
        dstore.readBuffer = newBuffer;
        return newBuffer;
    }

    /**
     * 
     */
    public void setColumnNames( String[] names ) {
        for( int i = 0; i < names.length; i++ )
            columnNames.set(i, names[i]);
    }

    /**
     * 
     */
    public void setColumnName( int col, String name ) {
        columnNames.set(col, name);
    }

    /**
     * 
     */
    public String getColumnName( int col ) {
        return (String) columnNames.get(col);
    }

    /**
     * 
     */
    public int getColumnType( int col ) {
        return (dstore == null) ? 0 : dstore.getColumnType(col);
    }

    /**
     * Returns the data of this objects row and column cell.
     */
    public Object get( int row, int col ) {
        return (dstore == null) ? null : dstore.get(row, col);
    }

    /**
     * Extract the number of columns worth of data from the vector and write to the data store.
     */
    public void writeRow( Vector v ) {
        if (dstore != null)
            dstore.writeRow(v);
    }

    /**
     * Set the data at the cell given by the row and column specified to integer value.
     */
    public void setInt( int row, int col, int val ) {
    }

    /**
     * Set the data at the cell given by the row and column specified to float value.
     */
    public void setFloat( int row, int col, float val ) {
    }

    /**
     * Set the data at the cell given by the row and column specified to double value.
     */
    public void setDouble( int row, int col, double val ) {
    }

    /**
     * Set the data at the cell given by the row and column specified to String value.
     */
    public void setString( int row, int col, String val ) {
    }

    /**
     * Returns an enumeration of the data of this object.
     */
    public Enumeration enumerator() {
        return dstore.initEnumerator();
    }

    /**
     * Returns the number of columns in this data object.
     */
    public int getColumnCount() {
        return dstore.columns == null ? -1 : dstore.columns.length;
    }

    /**
     * Returns the number of rows in this data object.
     */
    public int getRowCount() {
        return dstore.rows;
    }

    /*
     * Point data classes that is used to hold points
     */
    public static class PointData extends GDataObject {

        /**
         * Default constructor for point data object.
         */
        public PointData( int[] c ) {
            super(POINT_DATA,
                    new int[]{COLUMN_TYPE_DOUBLE, COLUMN_TYPE_DOUBLE, COLUMN_TYPE_DOUBLE}, c);
        }

        /**
         * Returns a string representation of this object.
         */
        public String toString() {
            return "PointData object, " + dstore.toString();
        }
    }

    /**
     * 
     */
    public static class VectorData extends GDataObject {

        /**
         * Default constructor for Vector data object.
         */
        public VectorData() {
            super(VECTOR_DATA, USE_BACKING_FILE);
        }

        /**
         * To string method.
         */
        public String toString() {
            return "VectorData object, datastore=" + dstore.toString();
        }
    }

    /**
     * 
     */
    public static class GridData extends GDataObject {

        /**
         * Default constructor for Raster data object.
         */
        public GridData( int size, int backing ) {
            super(RASTER_DATA, size, backing);
        }

        /**
         * Default constructor for Raster data object.
         */
        public GridData( int size ) {
            super(RASTER_DATA, size, USE_BACKING_MEMORY);
        }

        /**
         * Default constructor for Raster data object.
         */
        public GridData( ByteBuffer buf ) {
            super(RASTER_DATA, buf);
        }

        /**
         * To string method.
         */
        public String toString() {
            return "RasterData object, datastore=" + dstore.toString();
        }
    }

    /**
     * 
     */
    private class DataStore implements Enumeration {

        /* Backing file */
        private File backingTempFile = null;

        private RandomAccessFile backingFile = null;

        /* Number of rows in data object */
        protected int rows = 0;

        /* Column types */
        protected int[] columns = null;

        /* Current cursor position for reading data */
        protected int offset = 0;

        protected int bytesToRead = 0;

        /* Width in bytes of column data */
        protected int columnwidth = 0;

        /* Type of backing store */
        private int backing = 0;

        /* Reading channel object */
        protected ByteBuffer readBuffer = null;

        /* */
        protected int dataType = 0;

        /**
         * 
         */
        private DataStore( int dtype, int[] xyz, int[] c, int len, int backingType ) {
            int i;
            dataType = dtype;
            backing = backingType;

            if (dtype == RASTER_DATA) {
                columns = null;
                columnwidth = 0;
                if (backingType == USE_BACKING_MEMORY || backingType == USE_BACKING_FILE) {
                    readBuffer = ByteBuffer.allocate(len);

                    // if (JGrassLibsPlugin.isReallyDebugging()) {
                    // Logger.trace(getClass(), "ALLOCATING RASTER BUFFER -
                    // LENGTH = " + len
                    // + " - BUFFER.CAPACITY=" + readBuffer, null);
                    // }
                } else if (backingType == USE_BACKING_FILE) {
                    try {
                        /* Create an empty file */
                        backingFile = createBackingFile(len);
                        readBuffer = null;
                        readBuffer = ((MappedByteBuffer) (backingFile.getChannel().map(
                                FileChannel.MapMode.READ_WRITE, 0, backingFile.length()))).force()
                                .load();
                        readBuffer.rewind();
                    } catch (Exception e) {
                        backingTempFile = null;
                        backingFile = null;
                    }
                }
            } else if (dtype == POINT_DATA) {
                columns = new int[3 + (c == null ? 0 : c.length)];
                // columns[0] = COLUMN_TYPE_FLOAT;
                // columns[1] = COLUMN_TYPE_FLOAT;
                // columns[2] = COLUMN_TYPE_FLOAT;
                for( i = 0; i < xyz.length; i++ ) {
                    columns[i] = xyz[i];
                    columnwidth += xyz[i] == COLUMN_TYPE_DOUBLE ? 8 : xyz[i] == COLUMN_TYPE_FLOAT
                            ? 4
                            : xyz[i] == COLUMN_TYPE_INT ? 4 : xyz[i];
                }
                if (c != null) {
                    for( i = 0; i < c.length; i++ ) {
                        columns[i + 3] = c[i];
                        columnwidth += c[i] == COLUMN_TYPE_DOUBLE ? 8 : c[i] == COLUMN_TYPE_FLOAT
                                ? 4
                                : c[i] == COLUMN_TYPE_INT ? 4 : c[i];
                    }
                }
                if (backingType == USE_BACKING_FILE) {
                    try {
                        /* Create an empty file */
                        backingFile = createBackingFile(0);
                    } catch (Exception e) {
                        backingTempFile = null;
                        backingFile = null;
                    }
                }
            } else if (dtype == VECTOR_DATA) {
                if (backingType == USE_BACKING_FILE) {
                    try {
                        /* Create an empty file */
                        backingFile = createBackingFile(0);
                    } catch (Exception e) {
                        backingTempFile = null;
                        backingFile = null;
                    }
                }
            }
        }

        /**
         * 
         */
        private DataStore( int dtype, ByteBuffer buf ) {
            if (dtype == RASTER_DATA) {
                columns = null;
                columnwidth = 0;
                backing = USE_BACKING_MEMORY;
                readBuffer = buf;
            }
        }

        /**
         * 
         */
        public int getRowCount() {
            return rows;
        }

        /**
         * 
         */
        public int getColumnCount() {
            return columns == null ? -1 : columns.length;
        }

        /**
         * 
         */
        public int getColumnType( int col ) {
            return columns[col];
        }

        /**
         * Extract the number of columns worth of data from the vector and write to the backing
         * store.
         */
        public void writeRow( Vector v ) {
            if (dataType == POINT_DATA) {

                if (columns != null) {
                    int[] c = columns;
                    for( int i = 0; i < c.length; i++ ) {
                        if (i < v.size()) {
                            // System.out.println("GDATAOBJECT.writeRow -
                            // column="+i+", column_width="+columnwidth+
                            // ", columnformat="+columns[i]+",
                            // data="+v.elementAt(i).toString());
                            if (c[i] == COLUMN_TYPE_FLOAT) {
                                if (i < 3)
                                    writeFloat(((Double) v.elementAt(i)).floatValue());
                                else
                                    writeFloat(((Float) v.elementAt(i)).floatValue());
                            } else if (c[i] == COLUMN_TYPE_DOUBLE) {
                                writeDouble(((Double) v.elementAt(i)).doubleValue());
                            } else if (c[i] == COLUMN_TYPE_INT) {
                                if (i < 3)
                                    writeInt(((Double) v.elementAt(i)).intValue());
                                else
                                    writeInt(((Integer) v.elementAt(i)).intValue());
                            } else if (c[i] == COLUMN_TYPE_STRING) {
                                writeString((String) v.elementAt(i));
                            }
                        }
                    }
                    rows++;
                }
            } else if (dataType == VECTOR_DATA) {
                /*
                 * The first element is the type of vector, LINE or AREA. Pass this on so the
                 * display object will know what type of vector to render.
                 */
                writeInt(((Integer) v.elementAt(0)).intValue());
                /* The next element is the number of parts to this vector. */
                int numParts = ((Integer) v.elementAt(1)).intValue();
                writeInt(numParts);
                /* The next element is the total number of point to this vector. */
                int numPoints = ((Integer) v.elementAt(2)).intValue();
                writeInt(numPoints);
                /* The next elements are the part indexes. */
                for( int i = 0; i < numParts; i++ )
                    writeInt(((Integer) v.elementAt(3 + i)).intValue());
                /* Now comes the vectex data. */
                for( int i = 0; i < numPoints * 2; i++ ) {
                    writeFloat(((Float) v.elementAt(3 + numParts + i)).floatValue());
                }
                rows += numPoints;
            }
        }

        /**
         * Write a float to the backing store.
         * 
         * @param float data to be written to the backing store
         */
        private void writeFloat( float f ) {
            try {
                if (backing == USE_BACKING_FILE)
                    backingFile.writeFloat(f);
                else if (backing == USE_BACKING_MEMORY)
                    ;
            } catch (IOException ioe) {
            }
        }

        /**
         * Write a double to the backing store.
         * 
         * @param double data to be written to the backing store
         */
        private void writeDouble( double d ) {
            try {
                if (backing == USE_BACKING_FILE)
                    backingFile.writeDouble(d);
                else if (backing == USE_BACKING_MEMORY)
                    ;
            } catch (IOException ioe) {
            }
        }

        /**
         * Write a integer to the backing store. Integers are stored as long integers 4 bytes wide.
         * 
         * @param int data to be written to the backing store
         */
        private void writeInt( int i ) {
            try {
                if (backing == USE_BACKING_FILE)
                    backingFile.writeInt(i);
                else if (backing == USE_BACKING_MEMORY)
                    ;
            } catch (IOException ioe) {
            }
        }

        /**
         * Write a string to the backing store. Strings are stored as a fixed length sequence of
         * bytes of length set by the value of GDataObject.COLUMN_TYPE_STRING.
         * 
         * @param String data to be written to the backing store
         */
        private void writeString( String s ) {
            try {
                if (backing == USE_BACKING_FILE) {
                    StringBuffer buf = new StringBuffer(GDataObject.COLUMN_TYPE_STRING);
                    buf.insert(0, s);
                    buf.setLength(GDataObject.COLUMN_TYPE_STRING);
                    backingFile.writeBytes(buf.toString());
                } else if (backing == USE_BACKING_MEMORY)
                    ;
            } catch (IOException ioe) {
            }
        }

        /**
         * 
         */
        public DataStore initEnumerator() {
            try {
                if (dataType == POINT_DATA) {
                    if (backing == USE_BACKING_FILE) {
                        readBuffer = null;
                        readBuffer = ((MappedByteBuffer) (backingFile.getChannel().map(
                                FileChannel.MapMode.READ_ONLY, 0, backingFile.length()))).force()
                                .load();
                    } else if (backing == USE_BACKING_MEMORY)
                        readBuffer.rewind();
                    offset = 0;
                    bytesToRead = columnwidth;
                    return this;
                } else if (dataType == VECTOR_DATA) {
                    if (backing == USE_BACKING_FILE) {
                        readBuffer = null;
                        readBuffer = ((MappedByteBuffer) (backingFile.getChannel().map(
                                FileChannel.MapMode.READ_ONLY, 0, backingFile.length()))).force()
                                .load();
                    } else if (backing == USE_BACKING_MEMORY)
                        readBuffer.rewind();
                    offset = 0;
                    bytesToRead = (readBuffer.getInt(0) * 8) + 4;
                    return this;
                } else if (dataType == RASTER_DATA) {
                    if (backing == USE_BACKING_MEMORY)
                        readBuffer.rewind();
                    return this;
                }
            } catch (IOException ioe) {
            }
            return null;
        }

        /**
         * 
         */
        public boolean hasMoreElements() {
            return (bytesToRead <= (readBuffer.limit() - offset));
        }

        /**
         * 
         */
        public Object nextElement() {
            if (dataType == VECTOR_DATA) {
                int numParts = readBuffer.getInt(offset + 4);
                int numPoints = readBuffer.getInt(offset + 8);
                bytesToRead = numParts * 4 + numPoints * 8 + 12;
            }

            readBuffer.position(offset);
            offset += bytesToRead;
            return readBuffer;
        }

        /**
         * Returns the data from the row and column specified. The row and column are 1 indexed.
         */
        public Object get( int row, int col ) {
            if (col < 0 || col >= columns.length) {
                return "";
            }
            if (row < 0 || row >= rows) {
                return "";
            }

            int offset = 0;
            for( int i = 0; i < col; i++ )
                offset += (columns[i] == COLUMN_TYPE_DOUBLE ? 8 : columns[i] == COLUMN_TYPE_FLOAT
                        ? 4
                        : columns[i] == COLUMN_TYPE_INT ? 4 : columns[i]);
            offset += (row * columnwidth);
            // System.out.println("POINTDATAOBJECT.GET, row="+row+",
            // col="+col+", offset="+offset+", numcolumn="+columns.length);
            if (columns[col] == COLUMN_TYPE_FLOAT)
                return new Float(readBuffer.getFloat(offset));
            else if (columns[col] == COLUMN_TYPE_DOUBLE)
                return new Double(readBuffer.getDouble(offset));
            else if (columns[col] == COLUMN_TYPE_INT)
                return new Integer(readBuffer.getInt(offset));
            else {
                // System.out.println("reading string of length="+columns[col]);
                byte[] s = new byte[columns[col]];
                readBuffer.position(offset);
                readBuffer.get(s, 0, columns[col]);
                return new String(s, 0, columns[col]).trim();
            }
        }

        /**
         * 
         */
        private RandomAccessFile createBackingFile( int len ) throws IOException {
            backingTempFile = File.createTempFile("jgrass", "dat");
            backingTempFile.deleteOnExit();
            RandomAccessFile raf = new RandomAccessFile(backingTempFile, "rw");
            if (len > 0)
                raf.setLength(len);
            rafHandles.add(raf);
            return raf;
        }

        /**
         * 
         */
        public String toString() {
            return "datatype="
                    + ((dataType == RASTER_DATA) ? "RASTER" : (dataType == POINT_DATA)
                            ? "POINT"
                            : "VECTOR") + ", rows=" + rows + ", columns="
                    + (columns == null ? "" : "" + columns.length) + ", backing="
                    + (backing == USE_BACKING_FILE ? backingTempFile.getName() : "MEMORY")
                    + ", limit=" + (readBuffer == null ? "" : "" + readBuffer.limit());
        }
    }
}
