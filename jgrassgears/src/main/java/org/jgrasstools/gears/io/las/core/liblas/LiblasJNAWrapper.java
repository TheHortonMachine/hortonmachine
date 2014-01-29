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
public interface LiblasJNAWrapper extends com.sun.jna.Library {
    String LAS_GetVersion();
    String LAS_GetFullVersion();
    int LAS_IsGDALEnabled();
    int LAS_IsLibGeoTIFFEnabled();

    /*
     * READER
     */
    long LASReader_Create( String filename );
    void LASReader_Destroy( long fileHandle );
    long LASReader_Seek( long fileHandle, long position );
    long LASReader_GetNextPoint( long fileHandle );
    long LASReader_GetPointAt( long fileHandle, long position );

    /*
     * WRITER
     */
    long LASWriter_Create( String filename, long headerHandle, int mode );
    long LASWriter_WritePoint( long fileHandle, long pointHandle );
    void LASWriter_Destroy( long fileHandle );

    /*
     * POINT READING
     */
    double LASPoint_GetX( long pointHandle );
    double LASPoint_GetY( long pointHandle );
    double LASPoint_GetZ( long pointHandle );
    short LASPoint_GetIntensity( long pointHandle );
    // LASPoint_GetFlightLineEdge( long pointHandle );
    // LASPoint_GetScanDirection( long pointHandle );
    /* the number of returns of given pulse */
    int LASPoint_GetNumberOfReturns( long pointHandle );
    int LASPoint_GetReturnNumber( long pointHandle );
    byte LASPoint_GetClassification( long pointHandle );
    /* the scan angle */
    // LASPoint_GetScanAngleRank( long pointHandle );
    // LASPoint_GetUserData( long pointHandle );
    double LASPoint_GetTime( long pointHandle );

    /*
     * POINT WRITING
     */
    long LASPoint_Create( long fileHandle );
    void LASPoint_Destroy( long pointHandle );
    void LASPoint_SetHeader( long pointHandle, long headerHandle );
    void LASPoint_SetX( long pointHandle, double x );
    void LASPoint_SetY( long pointHandle, double y );
    void LASPoint_SetZ( long pointHandle, double z );
    void LASPoint_SetTime( long pointHandle, double gpsTime );
    void LASPoint_SetIntensity( long pointHandle, short intensity );
    // void LASPoint_SetScanAngleRank( long pointHandle, short scanAngle );
    void LASPoint_SetNumberOfReturns( long pointHandle, int numberOfReturns );
    void LASPoint_SetReturnNumber( long pointHandle, int returnNumber );
    void LASPoint_SetClassification( long pointHandle, byte classification );
    // LASPoint_SetUserData(point, temp_i);
    // LASPoint_SetPointSourceId(point, temp_i);
    // LASPoint_SetFlightLineEdge(point, temp_i ? 1: 0);
    // LASPoint_SetScanDirection(point, temp_i ? 1: 0);
    void LASPoint_SetColor( long pointHandle, long colorHandle );

    /*
     * COLOR WRITING 
     */
    long LASColor_Create();
    void LASColor_SetRed( long colorHandle, short color );
    void LASColor_SetGreen( long colorHandle, short color );
    void LASColor_SetBlue( long colorHandle, short color );
    void LASColor_Destroy( long colorHandle );

    /*
     * COLOR READING 
     */
    long LASPoint_GetColor( long pointHandle );
    short LASColor_GetRed( long colorHandle );
    short LASColor_GetGreen( long colorHandle );
    short LASColor_GetBlue( long colorHandle );

    /*
     * HEADER READING
     */
    long LASReader_GetHeader( long fileHandle );
    void LASHeader_Destroy( long headerHandle );

    String LASHeader_GetFileSignature( long headerHandle );
    short LASHeader_GetFileSourceId( long headerHandle );
    long LASHeader_GetReserved( long headerHandle );
    String LASHeader_GetProjectId( long headerHandle );
    byte LASHeader_GetVersionMajor( long headerHandle );
    byte LASHeader_GetVersionMinor( long headerHandle );
    String LASHeader_GetSystemId( long headerHandle );
    String LASHeader_GetSoftwareId( long headerHandle );
    short LASHeader_GetCreationDOY( long headerHandle );
    short LASHeader_GetCreationYear( long headerHandle );
    short LASHeader_GetHeaderSize( long headerHandle );
    long LASHeader_GetDataOffset( long headerHandle );
    long LASHeader_GetRecordsCount( long headerHandle );
    byte LASHeader_GetDataFormatId( long headerHandle );
    short LASHeader_GetDataRecordLength( long headerHandle );
    long LASHeader_GetPointRecordsCount( long headerHandle );
    long LASHeader_GetPointRecordsByReturnCount( long headerHandle, int returnNum );
    double LASHeader_GetScaleX( long headerHandle );
    double LASHeader_GetScaleY( long headerHandle );
    double LASHeader_GetScaleZ( long headerHandle );
    double LASHeader_GetOffsetX( long headerHandle );
    double LASHeader_GetOffsetY( long headerHandle );
    double LASHeader_GetOffsetZ( long headerHandle );
    double LASHeader_GetMinX( long headerHandle );
    double LASHeader_GetMinY( long headerHandle );
    double LASHeader_GetMinZ( long headerHandle );
    double LASHeader_GetMaxX( long headerHandle );
    double LASHeader_GetMaxY( long headerHandle );
    double LASHeader_GetMaxZ( long headerHandle );

    long LASHeader_GetSRS( long headerHandle );
    String LASSRS_GetWKT( long srsHandle );

    /*
     * HEADER WRITING
     */
    long LASHeader_Create();
    void LASHeader_SetSystemId( long headerHandle, String system_identifier );
    void LASHeader_SetSoftwareId( long headerHandle, String generating_software );
    void LASHeader_SetCreationDOY( long headerHandle, short file_creation_day );
    void LASHeader_SetCreationYear( long headerHandle, short file_creation_year );
    void LASHeader_SetVersionMinor( long headerHandle, int version );
    void LASHeader_SetDataFormatId( long headerHandle, byte formatId );

    void LASHeader_SetPointRecordsCount( long headerHandle, long number_of_point_records );
    void LASHeader_SetScale( long headerHandle, double xScale, double yScale, double zScale );
    void LASHeader_SetOffset( long headerHandle, double xOffset, double yOffset, double zOffset );
    void LASHeader_SetMin( long headerHandle, double xMin, double yMin, double zMin );
    void LASHeader_SetMax( long headerHandle, double xMax, double yMax, double zMax );
}
