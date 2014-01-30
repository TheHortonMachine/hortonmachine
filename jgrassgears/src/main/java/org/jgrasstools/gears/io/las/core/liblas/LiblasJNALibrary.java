/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.io.las.core.liblas;

/**
 * JNA wrapper for the laslib library.
 * 
 * @see LiblasReader
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface LiblasJNALibrary extends com.sun.jna.Library {
    abstract String LAS_GetVersion();
    abstract String LAS_GetFullVersion();
    abstract int LAS_IsGDALEnabled();
    abstract int LAS_IsLibGeoTIFFEnabled();

    /*
     * READER
     */
    abstract long LASReader_Create( String filename );
    abstract void LASReader_Destroy( long fileHandle );
    abstract long LASReader_Seek( long fileHandle, long position );
    abstract long LASReader_GetNextPoint( long fileHandle );
    abstract long LASReader_GetPointAt( long fileHandle, long position );

    /*
     * WRITER
     */
    abstract long LASWriter_Create( String filename, long headerHandle, byte mode );
    abstract long LASWriter_WritePoint( long fileHandle, long pointHandle );
    abstract void LASWriter_Destroy( long fileHandle );

    /*
     * POINT READING
     */
    abstract double LASPoint_GetX( long pointHandle );
    abstract double LASPoint_GetY( long pointHandle );
    abstract double LASPoint_GetZ( long pointHandle );
    abstract short LASPoint_GetIntensity( long pointHandle );
    // LASPoint_GetFlightLineEdge( long pointHandle );
    // LASPoint_GetScanDirection( long pointHandle );
    /* the number of returns of given pulse */
    abstract short LASPoint_GetNumberOfReturns( long pointHandle );
    abstract short LASPoint_GetReturnNumber( long pointHandle );
    abstract byte LASPoint_GetClassification( long pointHandle );
    /* the scan angle */
    // LASPoint_GetScanAngleRank( long pointHandle );
    // LASPoint_GetUserData( long pointHandle );
    abstract double LASPoint_GetTime( long pointHandle );

    /*
     * POINT WRITING
     */
    abstract long LASPoint_Create( long fileHandle );
    abstract void LASPoint_Destroy( long pointHandle );
    abstract void LASPoint_SetHeader( long pointHandle, long headerHandle );
    abstract void LASPoint_SetX( long pointHandle, double x );
    abstract void LASPoint_SetY( long pointHandle, double y );
    abstract void LASPoint_SetZ( long pointHandle, double z );
    abstract void LASPoint_SetTime( long pointHandle, double gpsTime );
    abstract void LASPoint_SetIntensity( long pointHandle, short intensity );
    // void LASPoint_SetScanAngleRank( long pointHandle, short scanAngle );
    abstract void LASPoint_SetNumberOfReturns( long pointHandle, short numberOfReturns );
    abstract void LASPoint_SetReturnNumber( long pointHandle, short returnNumber );
    abstract void LASPoint_SetClassification( long pointHandle, byte classification );
    // LASPoint_SetUserData(point, temp_i);
    // LASPoint_SetPointSourceId(point, temp_i);
    // LASPoint_SetFlightLineEdge(point, temp_i ? 1: 0);
    // LASPoint_SetScanDirection(point, temp_i ? 1: 0);
    abstract void LASPoint_SetColor( long pointHandle, long colorHandle );

    /*
     * COLOR WRITING 
     */
    abstract long LASColor_Create();
    abstract void LASColor_SetRed( long colorHandle, short color );
    abstract void LASColor_SetGreen( long colorHandle, short color );
    abstract void LASColor_SetBlue( long colorHandle, short color );
    abstract void LASColor_Destroy( long colorHandle );

    /*
     * COLOR READING 
     */
    abstract long LASPoint_GetColor( long pointHandle );
    abstract short LASColor_GetRed( long colorHandle );
    abstract short LASColor_GetGreen( long colorHandle );
    abstract short LASColor_GetBlue( long colorHandle );

    /*
     * HEADER READING
     */
    abstract long LASReader_GetHeader( long fileHandle );
    abstract void LASHeader_Destroy( long headerHandle );

    abstract String LASHeader_GetFileSignature( long headerHandle );
    abstract short LASHeader_GetFileSourceId( long headerHandle );
    abstract long LASHeader_GetReserved( long headerHandle );
    abstract String LASHeader_GetProjectId( long headerHandle );
    abstract byte LASHeader_GetVersionMajor( long headerHandle );
    abstract byte LASHeader_GetVersionMinor( long headerHandle );
    abstract String LASHeader_GetSystemId( long headerHandle );
    abstract String LASHeader_GetSoftwareId( long headerHandle );
    abstract short LASHeader_GetCreationDOY( long headerHandle );
    abstract short LASHeader_GetCreationYear( long headerHandle );
    abstract short LASHeader_GetHeaderSize( long headerHandle );
    abstract long LASHeader_GetDataOffset( long headerHandle );
    abstract long LASHeader_GetRecordsCount( long headerHandle );
    abstract byte LASHeader_GetDataFormatId( long headerHandle );
    abstract short LASHeader_GetDataRecordLength( long headerHandle );
    abstract long LASHeader_GetPointRecordsCount( long headerHandle );
    abstract long LASHeader_GetPointRecordsByReturnCount( long headerHandle, int returnNum );
    abstract double LASHeader_GetScaleX( long headerHandle );
    abstract double LASHeader_GetScaleY( long headerHandle );
    abstract double LASHeader_GetScaleZ( long headerHandle );
    abstract double LASHeader_GetOffsetX( long headerHandle );
    abstract double LASHeader_GetOffsetY( long headerHandle );
    abstract double LASHeader_GetOffsetZ( long headerHandle );
    abstract double LASHeader_GetMinX( long headerHandle );
    abstract double LASHeader_GetMinY( long headerHandle );
    abstract double LASHeader_GetMinZ( long headerHandle );
    abstract double LASHeader_GetMaxX( long headerHandle );
    abstract double LASHeader_GetMaxY( long headerHandle );
    abstract double LASHeader_GetMaxZ( long headerHandle );

    abstract long LASHeader_GetSRS( long headerHandle );
    abstract String LASSRS_GetWKT( long srsHandle );

    /*
     * HEADER WRITING
     */
    abstract long LASHeader_Create();
    abstract void LASHeader_SetSystemId( long headerHandle, String system_identifier );
    abstract void LASHeader_SetSoftwareId( long headerHandle, String generating_software );
    abstract void LASHeader_SetCreationDOY( long headerHandle, short file_creation_day );
    abstract void LASHeader_SetCreationYear( long headerHandle, short file_creation_year );
    abstract void LASHeader_SetVersionMinor( long headerHandle, int version );
    abstract void LASHeader_SetDataFormatId( long headerHandle, byte formatId );
    abstract void LASHeader_SetDataOffset( long headerHandle, int offset );
    abstract void LASHeader_SetDataRecordLength( long headerHandle, short recordLength );

    abstract void LASHeader_SetPointRecordsCount( long headerHandle, long number_of_point_records );
    abstract void LASHeader_SetScale( long headerHandle, double xScale, double yScale, double zScale );
    abstract void LASHeader_SetOffset( long headerHandle, double xOffset, double yOffset, double zOffset );
    abstract void LASHeader_SetMin( long headerHandle, double xMin, double yMin, double zMin );
    abstract void LASHeader_SetMax( long headerHandle, double xMax, double yMax, double zMax );
}
