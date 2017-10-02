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
package org.hortonmachine.gears.io.grasslegacy.io;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.zip.Deflater;

import org.hortonmachine.gears.io.grasslegacy.utils.Window;

/**
 * <p>
 * Write compressed JGrass rasters to disk
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @since 1.1.0
 */
public class CompressesRasterWriter {

    private static final String ERROR_IN_WRITING_RASTER = "Error in writing raster: ";

    private int outputToDiskType = 0;


    private double[] range = null;

    private long pointerInFilePosition = 0;

    private long[] rowaddresses = null;

    private Window dataWindow = null;

    /**
     * Preparing the environment for compressing and writing the map to disk
     * 
     * @param _outputToDiskType
     * @param _novalue
     * @param _jump
     * @param _range
     * @param _pointerInFilePosition
     * @param _rowaddresses
     * @param _dataWindow
     */
    public CompressesRasterWriter( int _outputToDiskType,  double[] _range,
            long _pointerInFilePosition, long[] _rowaddresses, Window _dataWindow ) {
        outputToDiskType = _outputToDiskType;
        range = _range;
        range[0] = Double.POSITIVE_INFINITY; 
        range[1] = Double.NEGATIVE_INFINITY; 
        pointerInFilePosition = _pointerInFilePosition;
        rowaddresses = _rowaddresses;
        dataWindow = _dataWindow;
    }

    /**
     * Passing the object after defining the type of data that will be written
     * 
     * @param theCreatedFile
     * @param theCreatedNullFile
     * @param dataObject
     * @return
     * @throws RasterWritingFailureException
     */
    public boolean compressAndWriteObj( RandomAccessFile theCreatedFile, RandomAccessFile theCreatedNullFile, Object dataObject )
            throws RasterWritingFailureException {
        if (dataObject instanceof double[][]) {
            compressAndWrite(theCreatedFile, theCreatedNullFile, (double[][]) dataObject);
        } else {
            throw new RasterWritingFailureException("Raster type not supported.");
        }
        return true;
    }

    /**
     * compress and write double[][] matrix this method converts every single row of the matrix of
     * values to bytes, as needed by the deflater. Then the byterows are compressed and then written
     * to file. Every rows first byte carries the information about compression (0 = not compressed,
     * 1 = compressed). At the begin the place for the header is written to file, in the end the
     * header is re-written with the right rowaddresses (at the begin we do not know how much
     * compression will influence).
     * 
     * @param theCreatedFile - handler for the main map file
     * @param theCreatedNullFile - handler for the file of the null map (in cell_misc)
     * @param rastermatrix - the data matrix
     * @return successfull or not
     * @throws RasterWritingFailureException
     */
    private boolean compressAndWrite( RandomAccessFile theCreatedFile, RandomAccessFile theCreatedNullFile,
            double[][] rastermatrix ) throws RasterWritingFailureException {
        try {
            // set the number of bytes needed for the values to write to disk
            int numberofbytes = outputToDiskType * 4;

            /*
             * the underlying byte array is needed as input to the deflater create it with the size
             * of the column * numberofbytes (8 for double, 4 for float), which is made to define
             * how we write to disk
             */
            byte[] rowAsBytes = new byte[rastermatrix[0].length * numberofbytes];
            ByteBuffer rowAsByteBuffer = ByteBuffer.wrap(rowAsBytes);

            /*
             * let's get started with the serious stuff. The null file is a bitmap representing with
             * 0's the nulls and with 1's existing values. Important is that for example 12 numbers
             * in a row will use 2 bytes in the nulls file, but fill only 12 n=bits. Therefore we
             * need to padd it.
             */
            int numberOfValuesPerRow = rastermatrix[0].length;
            int rest = numberOfValuesPerRow % 8;
            int paddings = 0;
            if (rest != 0) {
                paddings = 8 - rest;
            }
            BitSet nullbits = new BitSet(numberOfValuesPerRow + paddings);

            // iterate over the number of rows to compress every row and
            // write the result to disk
            // if (logger.isDebugEnabled())
            // logger.debug("rows x cols = " + rastermatrix.length + " x "
            // + rastermatrix[0].length);
            int k = 0;

            for( int i = 0; i < rastermatrix.length; i++ ) {
                for( int j = 0; j < rastermatrix[0].length; j++ ) {
                    // if it is NOT an NAN or if it is NOT a Novalue, then write the
                    // value
                    if (!Double.isNaN(rastermatrix[i][j])) {
                        // since we have to reread all the values, let's get the
                        // range
                        if (rastermatrix[i][j] < range[0])
                            range[0] = rastermatrix[i][j];
                        if (rastermatrix[i][j] > range[1])
                            range[1] = rastermatrix[i][j];

                        // convert the double row in a sequence of byte as needed by
                        // the
                        // deflater
                        if (numberofbytes == 8) {
                            rowAsByteBuffer.putDouble(rastermatrix[i][j]);
                        } else {
                            // the check on other formats is no longer needed, since
                            // it
                            // would jump out at the begin of this method
                            rowAsByteBuffer.putFloat((float) rastermatrix[i][j]);
                        }

                        /*
                         * ...and create the bitarray for the nullmap (in this case 0 is ok, so we
                         * just increment the counter k
                         */
                        k++;
                    }
                    // if it is a novalue, set the value and add the set the bit in
                    // the null-bitmap to true
                    else {
                        // put in the map the placeholder = 0.0 ...
                        if (numberofbytes == 8) {
                            rowAsByteBuffer.putDouble(0.0);
                        } else {
                            // the check on other formats is no longer needed, since
                            // it
                            // would jump out at the begin of this method
                            rowAsByteBuffer.putFloat(0f);
                        }
                        // ...and set the bit for the nullmap
                        nullbits.set(k);
                        k++;
                    }

                }

                /*
                 * now the bitset is complete... just need to write it to disk to create in one time
                 * the row (in cell_misc)
                 */
                int l = 0;
                byte[] bytearray = new byte[(numberOfValuesPerRow + paddings) / 8];

                for( int e = 0; e < (numberOfValuesPerRow + paddings) / 8; e++ ) {
                    bytearray[e] = (byte) 0;
                    for( int f = 0; f < 8; f++ ) {
                        if (nullbits.get(l)) {
                            bytearray[e] += (byte) Math.pow(2.0, (double) (7 - f));
                        }
                        l++;
                    }
                }

                theCreatedNullFile.write(bytearray);
                nullbits.clear();
                k = 0;

                /*
                 * now the row is converted to an array of bytes (ByteBuffer's method wrap applies
                 * changes on the ByteBuffer to the bytearray and vice versa. We can start with the
                 * deflater.
                 */
                byte[] output = new byte[rowAsBytes.length * 2];
                /* lenght *2 since not always compressing gives the needed result :) */
                Deflater compresser = new Deflater();
                compresser.setInput(rowAsBytes);
                compresser.finish();
                int compressedDataLength = compresser.deflate(output);

                /*
                 * now write to file the compressed row and set the right rowaddress.
                 */
                theCreatedFile.seek(pointerInFilePosition);
                /*
                 * jgrass always uses compression, so the first byte of the row will always be 49,
                 * i.e. 1 which means that the row is compressed
                 */
                theCreatedFile.write(49);
                theCreatedFile.write(output, 0, compressedDataLength);

                rowaddresses[i + 1] = pointerInFilePosition = theCreatedFile.getFilePointer();

                rowAsByteBuffer.clear();
            }

            /*
             * now that all the compressed rows are written to file, we have to write their
             * addresses in the header
             */
            theCreatedFile.seek(1);
            for( int i = 0; i < rowaddresses.length; i++ ) {
                theCreatedFile.writeInt((int) rowaddresses[i]);
            }
        } catch (Exception e) {
            throw new RasterWritingFailureException(ERROR_IN_WRITING_RASTER + e.getLocalizedMessage());
        }
        return true;
    }


    public Window getDataWindow() {
        return dataWindow;
    }

    public int getOutputToDiskType() {
        return outputToDiskType;
    }

    public long getPointerInFilePosition() {
        return pointerInFilePosition;
    }

    public double[] getRange() {
        return range;
    }

    public long[] getRowaddresses() {
        return rowaddresses;
    }
}
