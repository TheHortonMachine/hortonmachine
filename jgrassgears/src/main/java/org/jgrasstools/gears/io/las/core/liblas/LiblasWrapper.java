package org.jgrasstools.gears.io.las.core.liblas;

public interface LiblasWrapper extends com.sun.jna.Library {
    String LAS_GetVersion();
    String LAS_GetFullVersion();
    int LAS_IsGDALEnabled();
    int LAS_IsLibGeoTIFFEnabled();
    long LASReader_Create( String filename );
    void LASReader_Destroy( long fileHandle );
    long LASReader_Seek( long fileHandle, long position );

    long LASReader_GetNextPoint( long fileHandle );
    long LASReader_GetPointAt( long fileHandle, long position );

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

    long LASPoint_GetColor( long pointHandle );
    short LASPoint_GetRed( long colorHandle );
    short LASPoint_GetGreen( long colorHandle );
    short LASPoint_GetBlue( long colorHandle );

    /*
     * HEADER
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
}
