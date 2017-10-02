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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.Date;

import org.hortonmachine.gears.io.grasslegacy.utils.FileUtilities;
import org.hortonmachine.gears.io.grasslegacy.utils.GrassLegacyConstans;
import org.hortonmachine.gears.io.grasslegacy.utils.Window;

/**
 * <p>
 * This writes native JGrass Raster maps to disk in double format.
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @since 1.1.0
 */
public class GrassRasterWriter extends MapWriter {

    private String name = null;

    private String mapsetPath = null;

    private String locationPath = null;

    private String fcellFilePath = null;

    private long[] rowaddresses;

    /** the position in the file at which the next writing occurs */
    private long pointerInFilePosition;

    private double range[] = new double[2];

    private static final String ERROR_IN_WRITING_RASTER = "Error in writing raster: ";

    /**
     * this is 1 for float and 2 for double (jgrass only supports those two)
     */
    private int outputToDiskType = 2;


    /**
     * 
     */
    public GrassRasterWriter() {
        super(MapWriter.RASTER_WRITER);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jgrass.io.MapWriter#open(java.lang.String) this method tests for the existence of the
     *      needed folder structure and creates missing parts. The dataOutputType for the map to be
     *      created is checked
     */
    public boolean open( String fileName, String locationPath, String mapsetName ) {

        if (dataWindow != null) {
            name = fileName;
            this.locationPath = locationPath;
            this.mapsetPath = locationPath + File.separator + mapsetName;
            fcellFilePath = mapsetPath + File.separator + GrassLegacyConstans.FCELL + File.separator + name;

            if (!checkStructure())
                return false;
            if (!createEmptyHeader(fcellFilePath, dataWindow.getRows()))
                return false;
        } else {
            return false;
        }

        return true;
    }

    /**
     * this method writes the new map using the geographic region and settings of the active region
     * (dataWindow). Parameter is the dataobject holding the data
     */
    /*
     * (non-Javadoc)
     * 
     * @see jgrass.io.MapWriter#write(java.lang.Object)
     */
    public boolean write( Object dataObject ) throws Exception {
        /*
         * open the streams: the file for the map to create but also the needed null-file inside of
         * the cell_misc folder
         */
        File ds1 = new File(fcellFilePath);
        RandomAccessFile theCreatedFile = new RandomAccessFile(ds1, "rw"); //$NON-NLS-1$
        File ds2 = new File(mapsetPath + File.separator + GrassLegacyConstans.CELL_MISC + File.separator + name + File.separator
                + GrassLegacyConstans.CELLMISC_NULL);
        RandomAccessFile theCreatedNullFile = new RandomAccessFile(ds2, "rw"); //$NON-NLS-1$

        /*
         * finally writing to disk
         */
        CompressesRasterWriter crwriter = new CompressesRasterWriter(outputToDiskType, range, pointerInFilePosition,
                rowaddresses, dataWindow);
        crwriter.compressAndWriteObj(theCreatedFile, theCreatedNullFile, dataObject);
        // not sure I have to do this, have to check sooner or later
        outputToDiskType = crwriter.getOutputToDiskType();
        range = crwriter.getRange();
        pointerInFilePosition = crwriter.getPointerInFilePosition();
        rowaddresses = crwriter.getRowaddresses();
        dataWindow = crwriter.getDataWindow();

        theCreatedFile.close();
        theCreatedNullFile.close();
        createUtilityFiles();
        return true;
    }

    /**
     * check if the needed folders are there (they could be missing if the mapset has just been
     * created and this is the first file that gets into it
     * 
     * @return
     */
    private boolean checkStructure() {
        File ds;

        ds = new File(mapsetPath + File.separator + GrassLegacyConstans.CATS + File.separator);
        if (!ds.exists())
            if (!ds.mkdir())
                return false;
        ds = new File(mapsetPath + File.separator + GrassLegacyConstans.CELL + File.separator);
        if (!ds.exists())
            if (!ds.mkdir())
                return false;
        ds = new File(mapsetPath + File.separator + GrassLegacyConstans.CELL_MISC + File.separator);
        if (!ds.exists())
            if (!ds.mkdir())
                return false;
        ds = new File(mapsetPath + File.separator + GrassLegacyConstans.CELL_MISC + File.separator + name);
        if (!ds.exists())
            if (!ds.mkdir())
                return false;
        ds = new File(mapsetPath + File.separator + GrassLegacyConstans.FCELL + File.separator);
        if (!ds.exists())
            if (!ds.mkdir())
                return false;
        ds = new File(mapsetPath + File.separator + GrassLegacyConstans.CELLHD + File.separator);
        if (!ds.exists())
            if (!ds.mkdir())
                return false;
        ds = new File(mapsetPath + File.separator + GrassLegacyConstans.COLR + File.separator);
        if (!ds.exists())
            if (!ds.mkdir())
                return false;
        ds = new File(mapsetPath + File.separator + GrassLegacyConstans.HIST + File.separator);
        if (!ds.exists())
            if (!ds.mkdir())
                return false;
        return true;
    }

    /**
     * creates the space for the header of the rasterfile, filling the spaces with zeroes. After the
     * compression the values will be rewritten
     * 
     * @param filePath
     * @param rows
     * @return
     */
    private boolean createEmptyHeader( String filePath, int rows ) {

        try {
            RandomAccessFile theCreatedFile = new RandomAccessFile(filePath, "rw");

            rowaddresses = new long[rows + 1];
            // the size of a long
            theCreatedFile.write(4);
            // write the addresses of the row begins. Since we don't know how
            // much
            // they will be compressed, they will be filled after the
            // compression
            for( int i = 0; i < rows + 1; i++ ) {
                theCreatedFile.writeInt(0);
            }
            pointerInFilePosition = theCreatedFile.getFilePointer();
            rowaddresses[0] = pointerInFilePosition;

            theCreatedFile.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    // here the missing goes

    /**
     * this method creates all the support files needed in the grass filesystem for a raster map and
     * registers the rastermap to the parent-mapset
     */
    private void createUtilityFiles() throws Exception {
        // create the right files in the right places
        // cats/<name>
        File ds = new File(mapsetPath + File.separator + GrassLegacyConstans.CATS + File.separator + name);
        OutputStreamWriter catsWriter = new OutputStreamWriter(new FileOutputStream(ds));
        catsWriter.write("# xyz categories\n#\n#\n 0.00 0.00 0.00 0.00");
        catsWriter.close();

        // cell/<name>
        ds = new File(mapsetPath + File.separator + GrassLegacyConstans.CELL + File.separator + name);
        OutputStreamWriter cellWriter = new OutputStreamWriter(new FileOutputStream(ds));
        cellWriter.write("");
        cellWriter.close();

        // cell_misc/<name>/<files>
        // the directory <name> in cell_misc has already been created in
        // writeMapInActiveRegion (or extended) of the Class Mapset (or
        // extended)

        // f_format
        ds = new File(mapsetPath + File.separator + GrassLegacyConstans.CELL_MISC + File.separator + name + File.separator
                + GrassLegacyConstans.CELLMISC_FORMAT);
        OutputStreamWriter cell_miscFormatWriter = new OutputStreamWriter(new FileOutputStream(ds));
        if (outputToDiskType * 4 == 8) {
            cell_miscFormatWriter.write("type: double\nbyte_order: xdr\nlzw_compression_bits: -1");
        } else {
            cell_miscFormatWriter.write("type: float\nbyte_order: xdr\nlzw_compression_bits: -1");
        }

        cell_miscFormatWriter.close();

        // f_quant
        ds = new File(mapsetPath + File.separator + GrassLegacyConstans.CELL_MISC + File.separator + name + File.separator
                + GrassLegacyConstans.CELLMISC_QUANT);
        OutputStreamWriter cell_miscQantWriter = new OutputStreamWriter(new FileOutputStream(ds));
        cell_miscQantWriter.write("round");
        cell_miscQantWriter.close();

        // f_range
        ds = new File(mapsetPath + File.separator + GrassLegacyConstans.CELL_MISC + File.separator + name + File.separator
                + GrassLegacyConstans.CELLMISC_RANGE);
        OutputStream cell_miscRangeStream = new FileOutputStream(ds);
        // if (logger.isDebugEnabled())
        // logger.debug("RRRRRRRRRRRRRANGES: " + range[0] + "/" + range[1]);
        cell_miscRangeStream.write(FileUtilities.double2bytearray(range[0]));
        cell_miscRangeStream.write(FileUtilities.double2bytearray(range[1]));
        cell_miscRangeStream.close();

        // write the file cellhd
        /*
         * this won't work since the default window lost the proj information somewhere. When it is
         * set at the begin, it is there. This is a problem, since grass 6.0 complains about wrong
         * proj!!! It doesn't show rasters. createCellhd(GrassEnvironmentManager.getInstance()
         * .getCurrentSessionGrassEnvironment().getLocation().getDefaultWindow() .getProj(),
         * GrassEnvironmentManager.getInstance()
         * .getCurrentSessionGrassEnvironment().getLocation().getDefaultWindow() .getZone(),
         * dataWindow.getNorth(), dataWindow.getSouth(), dataWindow .getEast(),
         * dataWindow.getWest(), dataWindow.getCols(), dataWindow .getRows(),
         * dataWindow.getNSResolution(), dataWindow.getWEResolution(), -1, 1);
         */

        /*
         * need to reread the wind file to get the proj and zone (GRASS will not work if the cellhd
         * is not equal to the WIND proj)
         */
        Window tmp = new Window(locationPath + File.separator + GrassLegacyConstans.PERMANENT_MAPSET + File.separator
                + GrassLegacyConstans.WIND);
        createCellhd(tmp.getProj(), tmp.getZone(), dataWindow.getNorth(), dataWindow.getSouth(), dataWindow.getEast(),
                dataWindow.getWest(), dataWindow.getCols(), dataWindow.getRows(), dataWindow.getNSResolution(),
                dataWindow.getWEResolution(), -1, 1);

        // hist/<name>
        ds = new File(mapsetPath + File.separator + GrassLegacyConstans.HIST + File.separator + name);
        OutputStreamWriter windFile = new OutputStreamWriter(new FileOutputStream(ds));
        Date date = new Date();
        windFile.write(date + "\n");
        windFile.write(name + "\n");
        windFile.write(mapsetPath + "\n");
        windFile.write("generic user\n");
        windFile.write("DCELL\n");
        windFile.write("\n\nCreated by JGrass\n");
        windFile.write(historyComment + "\n");
        windFile.close();
        // now all the files have been created
    }

    /**
     * changes the cellhd file inserting the new values obtained from the environment
     */
    private void createCellhd( int chproj, int chzone, double chn, double chs, double che, double chw, int chcols, int chrows,
            double chnsres, double chewres, int chformat, int chcompressed ) throws Exception {
        StringBuffer data = new StringBuffer(512);
        data.append("proj:   " + chproj + "\n").append("zone:   " + chzone + "\n").append("north:   " + chn + "\n")
                .append("south:   " + chs + "\n").append("east:   " + che + "\n").append("west:   " + chw + "\n")
                .append("cols:   " + chcols + "\n").append("rows:   " + chrows + "\n").append("n-s resol:   " + chnsres + "\n")
                .append("e-w resol:   " + chewres + "\n").append("format:   " + chformat + "\n")
                .append("compressed:   " + chcompressed);
        File ds = new File(mapsetPath + File.separator + GrassLegacyConstans.CELLHD + File.separator + name);
        OutputStreamWriter windFile = new OutputStreamWriter(new FileOutputStream(ds));
        windFile.write(data.toString());
        windFile.close();
    }

    public void setOutputDataObject( Object _dataObject ) {
        dataObject = _dataObject;
        if (_dataObject instanceof Double) {
            outputToDiskType = 2;
        } else if (_dataObject instanceof Float) {
            outputToDiskType = 1;
        } else {
            // throw something
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jgrass.io.MapWriter#setParameter(java.lang.String, java.lang.Object)
     */
    public void setParameter( String key, Object obj ) {
    }

    public void setDataWindow( Window window ) {
        dataWindow = window;
    }

    public boolean open( String mapPath ) {
        File mapFile = new File(mapPath);

        String fileName = mapFile.getName();

        File mapsetFile = mapFile.getParentFile().getParentFile();
        String mapsetName = mapsetFile.getName();
        String locationPath = mapsetFile.getParent();

        return open(fileName, locationPath, mapsetName);
    }

}
