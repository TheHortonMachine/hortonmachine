package org.hortonmachine.gears.io.geopaparazzi.styles;

/**
 * Created by hydrologis on 18/07/14.
 */
public interface ISpatialiteTableAndFieldsNames {
    /**
     * From https://www.gaia-gis.it/fossil/libspatialite/wiki?name=metadata-4.0
     */
    public static final String METADATA_VECTOR_LAYERS_TABLE_NAME = "vector_layers";
    /**
     * From not yet documented [2014-05-09]
     */
    public static final String METADATA_RASTERLITE2_RASTER_COVERAGES_TABLE_NAME = "raster_coverages";
    /**
     * Starting from spatialite 2.4 to 3.1.0
     */
    public static final String METADATA_LAYER_STATISTICS_TABLE_NAME = "layer_statistics";
    /**
     * Starting from spatialite 2.4 to present
     */
    public static final String METADATA_GEOMETRY_COLUMNS_TABLE_NAME = "geometry_columns";
    /**
     * Starting from spatialite 2.4 to present
     */
    public static final String METADATA_VIEWS_GEOMETRY_COLUMNS_TABLE_NAME = "views_geometry_columns";
    /**
     * From https://www.gaia-gis.it/fossil/libspatialite/wiki?name=metadata-4.0
     */
    public static final String METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME = "vector_layers_statistics";

    /**
     * From 12-128r9_OGC_GeoPackage_Encoding_Standard_accept_changes_.pdf
     */
    public static final String METADATA_GEOPACKAGE_TABLE_NAME = "gpkg_contents";
    /**
     * The metadata table.
     */
    public final static String TABLE_METADATA = "metadata";
    /**
     * The metadata column name.
     */
    public final static String COL_METADATA_NAME = "name";
    /**
     * The metadata column value.
     */
    public final static String COL_METADATA_VALUE = "value";
    /**
     * The properties table name.
     */
    public static final String PROPERTIESTABLE = "dataproperties";
   

}
